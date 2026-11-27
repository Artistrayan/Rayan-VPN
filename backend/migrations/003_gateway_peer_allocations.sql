BEGIN;

CREATE TABLE IF NOT EXISTS gateway_peer_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gateway_id UUID NOT NULL REFERENCES gateways(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    wireguard_public_key TEXT NOT NULL,
    assigned_ipv4 INET NOT NULL,
    state TEXT NOT NULL DEFAULT 'pending'
        CHECK (state IN ('pending', 'applied', 'removal_pending', 'failed')),
    last_error_code TEXT,
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT gateway_peer_ipv4_host CHECK (
        family(assigned_ipv4) = 4 AND masklen(assigned_ipv4) = 32
    ),
    CONSTRAINT gateway_peer_unique_device UNIQUE (gateway_id, device_id),
    CONSTRAINT gateway_peer_unique_address UNIQUE (gateway_id, assigned_ipv4)
);

CREATE INDEX IF NOT EXISTS gateway_peer_reconciliation_idx
    ON gateway_peer_allocations(gateway_id, state, updated_at)
    WHERE state IN ('pending', 'removal_pending', 'failed');

CREATE INDEX IF NOT EXISTS gateway_peer_device_idx
    ON gateway_peer_allocations(device_id, updated_at DESC);

COMMIT;
