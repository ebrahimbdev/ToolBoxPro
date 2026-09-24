# ToolBoxPro API (Cloudflare Worker)

## Local

```bash
cd worker
npm install
npx wrangler d1 migrations apply DB --local
npx wrangler dev
```

## One-time Cloudflare setup

1. Free account: https://dash.cloudflare.com
2. `npx wrangler login`
3. Create D1 DB:
   ```bash
   npx wrangler d1 create toolboxpro
   ```
   Copy `database_id` into `wrangler.jsonc`.
4. Apply remote migrations:
   ```bash
   npx wrangler d1 migrations apply DB --remote
   ```
5. Set secrets:
   ```bash
   npx wrangler secret put ADMIN_TOKEN
   npx wrangler secret put APP_HMAC_SECRET
   npx wrangler secret put APP_KEY
   ```
   Use long random values. For app builds set the same `APP_KEY` / `APP_HMAC_SECRET` in Android `BuildConfig` or `local.properties`.
6. Deploy:
   ```bash
   npx wrangler deploy
   ```

## GitHub Actions (recommended)

Secrets in repo `ebrahimbdev/ToolBoxPro`:

- `CLOUDFLARE_API_TOKEN` — token with Edit Cloudflare Workers + Edit D1
- `CLOUDFLARE_ACCOUNT_ID`
- `ADMIN_TOKEN` / `APP_HMAC_SECRET` / `APP_KEY` — set as wrangler secrets once (manually) or via workflow

Workflow: `.github/workflows/deploy-worker.yml` (push to `main` on `worker/**` or manual dispatch).

## Routes

### App (`/api/v1/*`) — headers: `X-Device-Id`, `X-App-Key`, `X-Timestamp`, `X-Signature`

- `POST /register` — body: `{username, brand, model, os_version, app_version}`
- `POST /heartbeat` — body: `{ad_views, tool_opens, username, app_version}`
- `GET /config` — remote config (ads, prices, payments)
- `GET /profile`
- `GET /subscription`
- `POST /payments/create` — `{method: crypto|card|gateway, amount, plan, tx_hash, note}`
- `GET /payments/mine`

### Admin (`/admin/v1/*`) — header: `Authorization: Bearer <ADMIN_TOKEN>`

- `POST /login`
- `GET /stats`
- `GET /users?q=&limit=&offset=`
- `PATCH /users/{deviceId}/username` `{username}`
- `PATCH /users/{deviceId}/block` `{blocked: 1|0}`
- `POST /users/{deviceId}/grant` `{duration_days, price, plan}`
- `GET /payments?status=`
- `PATCH /payments/{id}/confirm|reject`
- `GET|PATCH /config`

## Notes

- Iranian gateway: `gateway_enabled=0`, note `coming soon` (until merchant ID ready).
- Crypto: set wallet address in admin config (`crypto_wallet` + `crypto_network`); auto-verify later.
- Card-to-card: admin confirms manually after seeing payment request + note/screenshot.
