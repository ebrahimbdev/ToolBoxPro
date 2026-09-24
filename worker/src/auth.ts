import type { Env } from "./env";
import { errorJson } from "./cors";

function toHex(buf: ArrayBuffer): string {
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

async function hmacHex(secret: string, message: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(message));
  return toHex(sig);
}

export async function requireAppAuth(request: Request, env: Env): Promise<Response | null> {
  const deviceId = request.headers.get("X-Device-Id");
  const appKey = request.headers.get("X-App-Key");
  const timestamp = request.headers.get("X-Timestamp");
  const signature = request.headers.get("X-Signature");

  if (!deviceId || !appKey || !timestamp || !signature) {
    return errorJson("missing auth headers", 401);
  }
  if (appKey !== env.APP_KEY) {
    return errorJson("invalid app key", 401);
  }

  const ts = Number(timestamp);
  if (!Number.isFinite(ts) || Math.abs(Date.now() - ts) > 1000 * 60 * 10) {
    return errorJson("timestamp out of range", 401);
  }

  const secret = env.APP_HMAC_SECRET;
  if (!secret) {
    if (signature === "dev") return null;
    return errorJson("server misconfigured", 500);
  }

  const url = new URL(request.url);
  const bodyText = request.method === "GET" || request.method === "HEAD" ? "" : await request.clone().text();
  const bodyHash = toHex(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(bodyText)));
  const message = `${request.method}\n${url.pathname}\n${timestamp}\n${deviceId}\n${bodyHash}`;
  const expected = await hmacHex(secret, message);

  const a = new TextEncoder().encode(expected);
  const b = new TextEncoder().encode(signature.toLowerCase());
  if (a.length !== b.length || !crypto.subtle.timingSafeEqual(a, b)) {
    return errorJson("invalid signature", 401);
  }
  return null;
}

export function requireAdmin(request: Request, env: Env): Response | null {
  const token = request.headers.get("Authorization")?.replace(/^Bearer\s+/i, "")
    || request.headers.get("X-Admin-Token");
  if (!token) return errorJson("admin token required", 401);

  const expected = env.ADMIN_TOKEN;
  if (!expected) return errorJson("admin auth not configured", 500);
  if (token !== expected) return errorJson("invalid admin token", 403);
  return null;
}
