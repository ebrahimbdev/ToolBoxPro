import type { Env, PaymentRow, UserRow } from "../env";
import { json, errorJson } from "../cors";
import { requireAdmin } from "../auth";
import { now, uuid, clampInt, bool01 } from "../helpers";

async function parseBody(request: Request): Promise<Record<string, unknown>> {
  const text = await request.text();
  if (!text) return {};
  try {
    const parsed: unknown = JSON.parse(text);
    if (parsed && typeof parsed === "object") return parsed as Record<string, unknown>;
  } catch {
    /* fallthrough */
  }
  return {};
}

export async function handleAdminRoute(request: Request, env: Env, path: string): Promise<Response> {
  const authErr = requireAdmin(request, env);
  if (authErr) return authErr;

  if (path === "/admin/v1/login" && request.method === "POST") {
    return json({ ok: true });
  }

  if (path === "/admin/v1/stats" && request.method === "GET") {
    const totalUsers = await env.DB.prepare("SELECT COUNT(*) AS c FROM users").first<{ c: number }>();
    const activeUsers = await env.DB
      .prepare("SELECT COUNT(*) AS c FROM users WHERE last_seen > ?")
      .bind(now() - 7 * 24 * 60 * 60 * 1000)
      .first<{ c: number }>();
    const premiumUsers = await env.DB
      .prepare("SELECT COUNT(DISTINCT user_id) AS c FROM subscriptions WHERE status='active' AND expires_at > ?")
      .bind(now())
      .first<{ c: number }>();
    const pendingPayments = await env.DB
      .prepare("SELECT COUNT(*) AS c FROM payments WHERE status IN ('created','pending')")
      .first<{ c: number }>();
    const totalAdViews = await env.DB.prepare("SELECT COALESCE(SUM(ad_views),0) AS c FROM users").first<{ c: number }>();
    const totalToolOpens = await env.DB
      .prepare("SELECT COALESCE(SUM(tool_opens),0) AS c FROM users")
      .first<{ c: number }>();
    const confirmedRevenue = await env.DB
      .prepare("SELECT COALESCE(SUM(amount),0) AS c FROM payments WHERE status='confirmed'")
      .first<{ c: number }>();

    return json({
      total_users: totalUsers?.c ?? 0,
      active_users_7d: activeUsers?.c ?? 0,
      premium_users: premiumUsers?.c ?? 0,
      pending_payments: pendingPayments?.c ?? 0,
      total_ad_views: totalAdViews?.c ?? 0,
      total_tool_opens: totalToolOpens?.c ?? 0,
      confirmed_revenue: confirmedRevenue?.c ?? 0,
    });
  }

  if (path === "/admin/v1/users" && request.method === "GET") {
    const url = new URL(request.url);
    const q = url.searchParams.get("q")?.slice(0, 64) ?? "";
    const limit = clampInt(url.searchParams.get("limit"), 1, 200, 50);
    const offset = clampInt(url.searchParams.get("offset"), 0, 100_000, 0);

    const rows = q
      ? await env.DB
          .prepare(
            `SELECT * FROM users WHERE username LIKE ? OR device_id LIKE ? OR model LIKE ?
             ORDER BY last_seen DESC LIMIT ? OFFSET ?`
          )
          .bind(`%${q}%`, `%${q}%`, `%${q}%`, limit, offset)
          .all<UserRow>()
      : await env.DB
          .prepare("SELECT * FROM users ORDER BY last_seen DESC LIMIT ? OFFSET ?")
          .bind(limit, offset)
          .all<UserRow>();

    const count = await env.DB.prepare("SELECT COUNT(*) AS c FROM users").first<{ c: number }>();
    return json({ users: rows.results ?? [], total: count?.c ?? 0 });
  }

  if (path.startsWith("/admin/v1/users/") && path.endsWith("/username") && request.method === "PATCH") {
    const deviceId = decodeURIComponent(path.split("/")[4] ?? "");
    if (!deviceId) return errorJson("device id required", 400);
    const body = await parseBody(request);
    const username = String(body.username ?? "").trim().slice(0, 64);
    await env.DB
      .prepare("UPDATE users SET username = ? WHERE device_id = ?")
      .bind(username || null, deviceId)
      .run();
    const user = await env.DB.prepare("SELECT * FROM users WHERE device_id = ?").bind(deviceId).first();
    if (!user) return errorJson("user not found", 404);
    return json({ user });
  }

  if (path.startsWith("/admin/v1/users/") && path.endsWith("/block") && request.method === "PATCH") {
    const deviceId = decodeURIComponent(path.split("/")[4] ?? "");
    if (!deviceId) return errorJson("device id required", 400);
    const body = await parseBody(request);
    const blocked = bool01(body.blocked, 1);
    await env.DB.prepare("UPDATE users SET is_blocked = ? WHERE device_id = ?").bind(blocked, deviceId).run();
    return json({ ok: true, blocked: blocked === 1 });
  }

  if (path.startsWith("/admin/v1/users/") && path.endsWith("/grant") && request.method === "POST") {
    const deviceId = decodeURIComponent(path.split("/")[4] ?? "");
    if (!deviceId) return errorJson("device id required", 400);
    const user = await env.DB.prepare("SELECT * FROM users WHERE device_id = ?").bind(deviceId).first<UserRow>();
    if (!user) return errorJson("user not found", 404);

    const body = await parseBody(request);
    const durationDays = clampInt(body.duration_days, 1, 3650, 30);
    const price = clampInt(body.price, 0, 100_000_000, 0);
    const plan = String(body.plan ?? "monthly").slice(0, 32);
    const ts = now();
    const id = uuid();

    await env.DB
      .prepare(
        `UPDATE subscriptions SET status='superseded'
         WHERE user_id = ? AND status='active' AND expires_at > ?`
      )
      .bind(user.id, ts)
      .run();

    await env.DB
      .prepare(
        `INSERT INTO subscriptions (id, user_id, plan, price, duration_days, starts_at, expires_at, status, created_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?)`
      )
      .bind(id, user.id, plan, price, durationDays, ts, ts + durationDays * 24 * 60 * 60 * 1000, ts)
      .run();

    return json({ ok: true, expires_at: ts + durationDays * 24 * 60 * 60 * 1000 });
  }

  if (path === "/admin/v1/payments" && request.method === "GET") {
    const url = new URL(request.url);
    const status = url.searchParams.get("status");
    const limit = clampInt(url.searchParams.get("limit"), 1, 200, 50);

    const rows = status
      ? await env.DB
          .prepare(
            `SELECT p.*, u.username, u.device_id, u.model
             FROM payments p LEFT JOIN users u ON u.id = p.user_id
             WHERE p.status = ? ORDER BY p.created_at DESC LIMIT ?`
          )
          .bind(status, limit)
          .all<PaymentRow & { username: string | null; device_id: string | null; model: string | null }>()
      : await env.DB
          .prepare(
            `SELECT p.*, u.username, u.device_id, u.model
             FROM payments p LEFT JOIN users u ON u.id = p.user_id
             ORDER BY p.created_at DESC LIMIT ?`
          )
          .bind(limit)
          .all<PaymentRow & { username: string | null; device_id: string | null; model: string | null }>();

    return json({ payments: rows.results ?? [] });
  }

  if (path.match(/^\/admin\/v1\/payments\/[^/]+\/(confirm|reject)$/) && request.method === "PATCH") {
    const parts = path.split("/");
    const paymentId = decodeURIComponent(parts[4] ?? "");
    const action = parts[5];
    if (!paymentId || !action) return errorJson("bad path", 400);

    const payment = await env.DB.prepare("SELECT * FROM payments WHERE id = ?").bind(paymentId).first<PaymentRow>();
    if (!payment) return errorJson("payment not found", 404);

    const ts = now();
    if (action === "confirm") {
      await env.DB
        .prepare("UPDATE payments SET status='confirmed', confirmed_at=?, confirmed_by=? WHERE id=?")
        .bind(ts, "admin", paymentId)
        .run();

      await env.DB
        .prepare(
          `UPDATE subscriptions SET status='superseded'
           WHERE user_id = ? AND status='active' AND expires_at > ?`
        )
        .bind(payment.user_id, ts)
        .run();

      const cfg = await env.DB.prepare("SELECT * FROM remote_config WHERE key='app'").first<{
        subscription_duration_days: number;
      }>();
      const durationDays = clampInt(cfg?.subscription_duration_days, 1, 3650, 30);
      await env.DB
        .prepare(
          `INSERT INTO subscriptions (id, user_id, plan, price, duration_days, starts_at, expires_at, status, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?)`
        )
        .bind(
          uuid(),
          payment.user_id,
          payment.plan,
          payment.amount,
          durationDays,
          ts,
          ts + durationDays * 24 * 60 * 60 * 1000,
          ts
        )
        .run();
    } else {
      await env.DB
        .prepare("UPDATE payments SET status='rejected', confirmed_at=?, confirmed_by=? WHERE id=?")
        .bind(ts, "admin", paymentId)
        .run();
    }

    const fresh = await env.DB.prepare("SELECT * FROM payments WHERE id = ?").bind(paymentId).first();
    return json({ payment: fresh });
  }

  if (path === "/admin/v1/config" && request.method === "GET") {
    const row = await env.DB.prepare("SELECT * FROM remote_config WHERE key='app'").first();
    return json({ config: row });
  }

  if (path === "/admin/v1/config" && request.method === "PATCH") {
    const body = await parseBody(request);
    const current = await env.DB.prepare("SELECT * FROM remote_config WHERE key='app'").first<Record<string, unknown>>();
    if (!current) return errorJson("config not found", 404);

    const next = {
      ad_interval_seconds: clampInt(body.ad_interval_seconds, 5, 600, Number(current.ad_interval_seconds)),
      banner_enabled: bool01(body.banner_enabled, Number(current.banner_enabled)),
      interstitial_enabled: bool01(body.interstitial_enabled, Number(current.interstitial_enabled)),
      free_ad_multiplier: clampInt(body.free_ad_multiplier, 1, 10, Number(current.free_ad_multiplier)),
      subscription_price_toman: clampInt(
        body.subscription_price_toman,
        0,
        100_000_000,
        Number(current.subscription_price_toman)
      ),
      subscription_duration_days: clampInt(
        body.subscription_duration_days,
        1,
        3650,
        Number(current.subscription_duration_days)
      ),
      card_number: body.card_number !== undefined ? String(body.card_number).slice(0, 64) : String(current.card_number ?? ""),
      card_holder: body.card_holder !== undefined ? String(body.card_holder).slice(0, 64) : String(current.card_holder ?? ""),
      crypto_wallet:
        body.crypto_wallet !== undefined ? String(body.crypto_wallet).slice(0, 256) : String(current.crypto_wallet ?? ""),
      crypto_network:
        body.crypto_network !== undefined ? String(body.crypto_network).slice(0, 32) : String(current.crypto_network ?? ""),
      gateway_enabled: bool01(body.gateway_enabled, Number(current.gateway_enabled)),
      gateway_status_note:
        body.gateway_status_note !== undefined
          ? String(body.gateway_status_note).slice(0, 128)
          : String(current.gateway_status_note ?? "coming soon"),
      updated_at: now(),
    };

    await env.DB
      .prepare(
        `UPDATE remote_config SET
           ad_interval_seconds=?, banner_enabled=?, interstitial_enabled=?, free_ad_multiplier=?,
           subscription_price_toman=?, subscription_duration_days=?, card_number=?, card_holder=?,
           crypto_wallet=?, crypto_network=?, gateway_enabled=?, gateway_status_note=?, updated_at=?
         WHERE key='app'`
      )
      .bind(
        next.ad_interval_seconds,
        next.banner_enabled,
        next.interstitial_enabled,
        next.free_ad_multiplier,
        next.subscription_price_toman,
        next.subscription_duration_days,
        next.card_number,
        next.card_holder,
        next.crypto_wallet,
        next.crypto_network,
        next.gateway_enabled,
        next.gateway_status_note,
        next.updated_at
      )
      .run();

    const row = await env.DB.prepare("SELECT * FROM remote_config WHERE key='app'").first();
    return json({ config: row });
  }

  return errorJson("not found", 404);
}
