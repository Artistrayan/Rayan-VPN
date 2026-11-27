BEGIN;

ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS device_token_hash BYTEA;

CREATE UNIQUE INDEX IF NOT EXISTS devices_device_token_hash_idx
    ON devices(device_token_hash)
    WHERE device_token_hash IS NOT NULL;

COMMIT;
