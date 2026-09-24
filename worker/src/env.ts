export interface Env {
  DB: D1Database;
  APP_KEY: string;
  APP_HMAC_SECRET?: string;
  ADMIN_TOKEN?: string;
}

export interface UserRow {
  id: string;
  device_id: string;
  username: string | null;
  brand: string | null;
  model: string | null;
  os_version: string | null;
  app_version: string | null;
  created_at: number;
  last_seen: number;
  ad_views: number;
  tool_opens: number;
  is_blocked: number;
}

export interface SubscriptionRow {
  id: string;
  user_id: string;
  plan: string;
  price: number;
  duration_days: number;
  starts_at: number;
  expires_at: number;
  status: string;
  created_at: number;
}

export interface PaymentRow {
  id: string;
  user_id: string;
  amount: number;
  method: string;
  status: string;
  plan: string;
  gateway_ref: string | null;
  tx_hash: string | null;
  note: string | null;
  created_at: number;
  confirmed_at: number | null;
  confirmed_by: string | null;
}

export interface RemoteConfigRow {
  key: string;
  ad_interval_seconds: number;
  banner_enabled: number;
  interstitial_enabled: number;
  subscription_price_toman: number;
  subscription_duration_days: number;
  free_ad_multiplier: number;
  card_number: string;
  card_holder: string;
  crypto_wallet: string;
  crypto_network: string;
  gateway_enabled: number;
  gateway_status_note: string;
  updated_at: number;
}
