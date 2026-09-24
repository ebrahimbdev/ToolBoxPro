PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY NOT NULL,
  device_id TEXT NOT NULL UNIQUE,
  username TEXT,
  brand TEXT,
  model TEXT,
  os_version TEXT,
  app_version TEXT,
  created_at INTEGER NOT NULL,
  last_seen INTEGER NOT NULL,
  ad_views INTEGER NOT NULL DEFAULT 0,
  tool_opens INTEGER NOT NULL DEFAULT 0,
  is_blocked INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_users_last_seen ON users(last_seen);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

CREATE TABLE IF NOT EXISTS subscriptions (
  id TEXT PRIMARY KEY NOT NULL,
  user_id TEXT NOT NULL,
  plan TEXT NOT NULL DEFAULT 'free',
  price INTEGER NOT NULL DEFAULT 0,
  duration_days INTEGER NOT NULL DEFAULT 0,
  starts_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'active',
  created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_subs_user ON subscriptions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_subs_expires ON subscriptions(expires_at);

CREATE TABLE IF NOT EXISTS payments (
  id TEXT PRIMARY KEY NOT NULL,
  user_id TEXT NOT NULL,
  amount INTEGER NOT NULL,
  method TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'created',
  plan TEXT NOT NULL DEFAULT 'monthly',
  gateway_ref TEXT,
  tx_hash TEXT,
  note TEXT,
  created_at INTEGER NOT NULL,
  confirmed_at INTEGER,
  confirmed_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_payments_user ON payments(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

CREATE TABLE IF NOT EXISTS remote_config (
  key TEXT PRIMARY KEY NOT NULL DEFAULT 'app',
  ad_interval_seconds INTEGER NOT NULL DEFAULT 45,
  banner_enabled INTEGER NOT NULL DEFAULT 1,
  interstitial_enabled INTEGER NOT NULL DEFAULT 1,
  subscription_price_toman INTEGER NOT NULL DEFAULT 150000,
  subscription_duration_days INTEGER NOT NULL DEFAULT 30,
  free_ad_multiplier INTEGER NOT NULL DEFAULT 2,
  card_number TEXT DEFAULT '',
  card_holder TEXT DEFAULT '',
  crypto_wallet TEXT DEFAULT '',
  crypto_network TEXT DEFAULT 'TRC20',
  gateway_enabled INTEGER NOT NULL DEFAULT 0,
  gateway_status_note TEXT NOT NULL DEFAULT 'coming soon',
  updated_at INTEGER NOT NULL
);

INSERT OR IGNORE INTO remote_config (key, updated_at) VALUES ('app', 0);

CREATE TABLE IF NOT EXISTS admin_tokens (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  token_hash TEXT NOT NULL UNIQUE,
  created_at INTEGER NOT NULL,
  revoked_at INTEGER
);
