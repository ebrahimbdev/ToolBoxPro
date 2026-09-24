import type { Env } from "./env";
import { withCors, json, errorJson } from "./cors";
import { handleAppRoute } from "./routes/app";
import { handleAdminRoute } from "./routes/admin";

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, PATCH, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Device-Id, X-App-Key, X-Timestamp, X-Signature, X-Admin-Token",
        "Access-Control-Max-Age": "86400",
      }});
    }

    try {
      const path = url.pathname.replace(/\/+$/, "") || "/";

      if (path === "/" || path === "/health") {
        return withCors(json({ ok: true, service: "toolboxpro-api", time: Date.now() }));
      }

      if (path.startsWith("/api/v1/")) {
        return withCors(await handleAppRoute(request, env, path));
      }

      if (path.startsWith("/admin/v1/")) {
        return withCors(await handleAdminRoute(request, env, path));
      }

      return withCors(errorJson("not found", 404));
    } catch (err) {
      console.error("unhandled", err);
      return withCors(errorJson("internal error", 500));
    }
  },
} satisfies ExportedHandler<Env>;
