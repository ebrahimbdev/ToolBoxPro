-- Usage gate: number of free tool entries the user gets before an ad is required.
-- 0 = gate disabled (unlimited free use).
ALTER TABLE remote_config ADD COLUMN free_uses_per_ad INTEGER NOT NULL DEFAULT 0;
