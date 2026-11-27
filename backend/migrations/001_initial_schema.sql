BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT users_email_normalized CHECK (email IS NULL OR email = lower(email))
);

CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    installation_id_hash BYTEA NOT NULL UNIQUE,
    wireguard_public_key TEXT NOT NULL UNIQUE,
    platform TEXT NOT NULL DEFAULT 'android' CHECK (platform = 'android'),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'revoked')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS devices_user_id_idx ON devices(user_id);
CREATE INDEX IF NOT EXISTS devices_status_last_seen_idx ON devices(status, last_seen_at DESC);

CREATE TABLE IF NOT EXISTS gateways (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider TEXT NOT NULL,
    region_code TEXT NOT NULL,
    country_code CHAR(2) NOT NULL,
    city TEXT NOT NULL,
    endpoint_host TEXT NOT NULL,
    endpoint_port INTEGER NOT NULL CHECK (endpoint_port BETWEEN 1 AND 65535),
    wireguard_public_key TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'provisioning'
        CHECK (status IN ('provisioning', 'healthy', 'degraded', 'maintenance', 'offline')),
    capacity_mbps INTEGER NOT NULL DEFAULT 0 CHECK (capacity_mbps >= 0),
    current_load_percent NUMERIC(5,2) NOT NULL DEFAULT 0
        CHECK (current_load_percent BETWEEN 0 AND 100),
    last_health_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(provider, endpoint_host, endpoint_port)
);

CREATE INDEX IF NOT EXISTS gateways_selection_idx
    ON gateways(status, region_code, current_load_percent)
    WHERE status IN ('healthy', 'degraded');

CREATE TABLE IF NOT EXISTS vpn_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    gateway_id UUID NOT NULL REFERENCES gateways(id) ON DELETE RESTRICT,
    state TEXT NOT NULL DEFAULT 'provisioning'
        CHECK (state IN ('provisioning', 'active', 'closed', 'failed', 'revoked')),
    assigned_ipv4 INET,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_handshake_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    close_reason TEXT
);

CREATE INDEX IF NOT EXISTS vpn_sessions_device_active_idx
    ON vpn_sessions(device_id, started_at DESC)
    WHERE state IN ('provisioning', 'active');

CREATE INDEX IF NOT EXISTS vpn_sessions_gateway_active_idx
    ON vpn_sessions(gateway_id, started_at DESC)
    WHERE state = 'active';

CREATE TABLE IF NOT EXISTS usage_aggregates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    gateway_id UUID NOT NULL REFERENCES gateways(id) ON DELETE RESTRICT,
    bucket_start TIMESTAMPTZ NOT NULL,
    bytes_rx BIGINT NOT NULL DEFAULT 0 CHECK (bytes_rx >= 0),
    bytes_tx BIGINT NOT NULL DEFAULT 0 CHECK (bytes_tx >= 0),
    source TEXT NOT NULL CHECK (source IN ('gateway_counter', 'provider_meter')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(device_id, gateway_id, bucket_start, source)
);

CREATE INDEX IF NOT EXISTS usage_aggregates_bucket_idx
    ON usage_aggregates(bucket_start DESC);

CREATE TABLE IF NOT EXISTS entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    tier TEXT NOT NULL CHECK (tier IN ('free', 'premium')),
    source TEXT NOT NULL CHECK (source IN ('default', 'google_play', 'promotion', 'support')),
    starts_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CHECK (user_id IS NOT NULL OR device_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS entitlements_user_active_idx
    ON entitlements(user_id, expires_at DESC)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS entitlements_device_active_idx
    ON entitlements(device_id, expires_at DESC)
    WHERE revoked_at IS NULL;

CREATE TABLE IF NOT EXISTS purchase_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform TEXT NOT NULL CHECK (platform = 'google_play'),
    product_id TEXT NOT NULL,
    purchase_token_hash BYTEA NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('pending', 'active', 'grace_period', 'cancelled', 'expired', 'refunded', 'revoked')),
    purchased_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS security_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    event_type TEXT NOT NULL,
    severity TEXT NOT NULL CHECK (severity IN ('info', 'warning', 'critical')),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS security_events_created_at_idx
    ON security_events(created_at DESC);

COMMIT;
