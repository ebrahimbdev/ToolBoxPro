import type { Env, UserRow, SubscriptionRow, RemoteConfigRow } from "../env";
import { json, errorJson } from "../cors";
import { requireAppAuth } from "../auth";
import { now, uuid, clampInt } from "../helpers";

async function findUser(db: D1Database, deviceId: string): Promise<UserRow | null> {
  const row = await db.prepare("SELECT * FROM users WHERE device_id = ?").bind(deviceId).first<UserRow>();
  return row ?? null;
}

async function activeSub(db: D1Database, userId: string): Promise<SubscriptionRow | null> {
  const row = await db
    .prepare(
      "SELECT * FROM subscriptions WHERE user_id = ? AND status = 'active' AND expires_at > ? ORDER BY expires_at DESC LIMIT 1"
    )
    .bind(userId, now())
    .first<SubscriptionRow>();
  return row ?? null;
}

async function getConfig(db: D1Database): Promise<RemoteConfigRow> {
  const row = await db.prepare("SELECT * FROM remote_config WHERE key = 'app'").first<RemoteConfigRow>();
  if (row) return row;
  await db
    .prepare("INSERT OR IGNORE INTO remote_config (key, updated_at) VALUES ('app', ?)")
    .bind(now())
    .run();
  const created = await db.prepare("SELECT * FROM remote_config WHERE key = 'app'").first<RemoteConfigRow>();
  if (!created) throw new Error("remote_config init failed");
  return created;
}

function publicUser(u: UserRow, sub: SubscriptionRow | null, cfg: RemoteConfigRow) {
  const premium = !!sub;
  return {
    device_id: u.device_id,
    username: u.username ?? "",
    brand: u.brand ?? "",
    model: u.model ?? "",
    os_version: u.os_version ?? "",
    app_version: u.app_version ?? "",
    created_at: u.created_at,
    last_seen: u.last_seen,
    ad_views: u.ad_views,
    tool_opens: u.tool_opens,
    premium,
    subscription: sub
      ? {
          plan: sub.plan,
          price: sub.price,
          duration_days: sub.duration_days,
          starts_at: sub.starts_at,
          expires_at: sub.expires_at,
          status: sub.status,
        }
      : null,
    config: {
      ad_interval_seconds: cfg.ad_interval_seconds,
      banner_enabled: cfg.banner_enabled === 1,
      interstitial_enabled: cfg.interstitial_enabled === 1,
      free_ad_multiplier: cfg.free_ad_multiplier,
    },
  };
}

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

export async function handleAppRoute(request: Request, env: Env, path: string): Promise<Response> {
  const authErr = await requireAppAuth(request, env);
  if (authErr) return authErr;

  const deviceIdHeader = request.headers.get("X-Device-Id");
  if (!deviceIdHeader) return errorJson("device id required", 400);
  const deviceId = deviceIdHeader.slice(0, 128);

  if (path === "/api/v1/register" && request.method === "POST") {
    const body = await parseBody(request);
    const username = String(body.username ?? "").trim().slice(0, 64);
    const brand = String(body.brand ?? "").slice(0, 64);
    const model = String(body.model ?? "").slice(0, 64);
    const osVersion = String(body.os_version ?? "").slice(0, 32);
    const appVersion = String(body.app_version ?? "").slice(0, 32);

    let user = await findUser(env.DB, deviceId);
    if (!user) {
      const id = uuid();
      const ts = now();
      await env.DB
        .prepare(
          `INSERT INTO users (id, device_id, username, brand, model, os_version, app_version, created_at, last_seen)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
        )
        .bind(id, deviceId, username || null, brand || null, model || null, osVersion || null, appVersion || null, ts, ts)
        .run();
      user = await findUser(env.DB, deviceId);
      if (!user) return errorJson("register failed", 500);
    } else if (username || brand || model || osVersion || appVersion) {
      await env.DB
        .prepare(
          `UPDATE users SET
             username = COALESCE(NULLIF(?, ''), username),
             brand = COALESCE(NULLIF(?, ''), brand),
             model = COALESCE(NULLIF(?, ''), model),
             os_version = COALESCE(NULLIF(?, ''), os_version),
             app_version = COALESCE(NULLIF(?, ''), app_version),
             last_seen = ?
           WHERE device_id = ?`
        )
        .bind(username, brand, model, osVersion, appVersion, now(), deviceId)
        .run();
      user = await findUser(env.DB, deviceId);
      if (!user) return errorJson("register failed", 500);
    }

    if (user.is_blocked === 1) return errorJson("account blocked", 403);
    const cfg = await getConfig(env.DB);
    const sub = await activeSub(env.DB, user.id);
    return json(publicUser(user, sub, cfg));
  }

  if (path === "/api/v1/heartbeat" && request.method === "POST") {
    const user = await findUser(env.DB, deviceId);
    if (!user) return errorJson("register first", 404);
    if (user.is_blocked === 1) return errorJson("account blocked", 403);

    const body = await parseBody(request);
    const adViews = clampInt(body.ad_views, 0, 10_000, 0);
    const toolOpens = clampInt(body.tool_opens, 0, 10_000, 0);
    const username = String(body.username ?? "").trim().slice(0, 64);
    const appVersion = String(body.app_version ?? "").slice(0, 32);

    await env.DB
      .prepare(
        `UPDATE users SET
           last_seen = ?,
           ad_views = ad_views + ?,
           tool_opens = tool_opens + ?,
           username = COALESCE(NULLIF(?, ''), username),
           app_version = COALESCE(NULLIF(?, ''), app_version)
         WHERE device_id = ?`
      )
      .bind(now(), adViews, toolOpens, username, appVersion, deviceId)
      .run();

    const fresh = await findUser(env.DB, deviceId);
    if (!fresh) return errorJson("update failed", 500);
    const cfg = await getConfig(env.DB);
    const sub = await activeSub(env.DB, fresh.id);
    return json(publicUser(fresh, sub, cfg));
  }

  if (path === "/api/v1/config" && request.method === "GET") {
    const cfg = await getConfig(env.DB);
    return json({
      ad_interval_seconds: cfg.ad_interval_seconds,
      banner_enabled: cfg.banner_enabled === 1,
      interstitial_enabled: cfg.interstitial_enabled === 1,
      free_ad_multiplier: cfg.free_ad_multiplier,
      subscription_price_toman: cfg.subscription_price_toman,
      subscription_duration_days: cfg.subscription_duration_days,
      card_number: cfg.card_number,
      card_holder: cfg.card_holder,
      crypto_wallet: cfg.crypto_wallet,
      crypto_network: cfg.crypto_network,
      gateway_enabled: cfg.gateway_enabled === 1,
      gateway_status_note: cfg.gateway_status_note,
    });
  }

  if (path === "/api/v1/payments/create" && request.method === "POST") {
    const user = await findUser(env.DB, deviceId);
    if (!user) return errorJson("register first", 404);
    if (user.is_blocked === 1) return errorJson("account blocked", 403);

    const body = await parseBody(request);
    const method = String(body.method ?? "").toLowerCase();
    if (!["crypto", "card", "gateway"].includes(method)) {
      return errorJson("invalid method", 400);
    }
    const amount = clampInt(body.amount, 1000, 100_000_000, 0);
    if (amount <= 0) return errorJson("invalid amount", 400);
    const plan = String(body.plan ?? "monthly").slice(0, 32);
    const txHash = String(body.tx_hash ?? "").slice(0, 128);
    const note = String(body.note ?? "").slice(0, 256);

    const id = uuid();
    const status = method === "gateway" && (await getConfig(env.DB)).gateway_enabled === 1 ? "pending" : "created";
    await env.DB
      .prepare(
        `INSERT INTO payments (id, user_id, amount, method, status, plan, tx_hash, note, created_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
      )
      .bind(id, user.id, amount, method, status, plan, txHash || null, note || null, now())
      .run();

    const row = await env.DB.prepare("SELECT * FROM payments WHERE id = ?").bind(id).first();
    return json({ payment: row, status });
  }

  if (path === "/api/v1/payments/mine" && request.method === "GET") {
    const user = await findUser(env.DB, deviceId);
    if (!user) return errorJson("register first", 404);
    const rows = await env.DB
      .prepare("SELECT * FROM payments WHERE user_id = ? ORDER BY created_at DESC LIMIT 50")
      .bind(user.id)
      .all<Record<string, unknown>>();
    return json({ payments: rows.results ?? [] });
  }

  if (path === "/api/v1/subscription" && request.method === "GET") {
    const user = await findUser(env.DB, deviceId);
    if (!user) return errorJson("register first", 404);
    const sub = await activeSub(env.DB, user.id);
    return json({ subscription: sub, premium: !!sub });
  }

  if (path === "/api/v1/profile" && request.method === "GET") {
    const user = await findUser(env.DB, deviceId);
    if (!user) return errorJson("register first", 404);
    const cfg = await getConfig(env.DB);
    const sub = await activeSub(env.DB, user.id);
    return json(publicUser(user, sub, cfg));
  }

  return errorJson("not found", 404);
}
