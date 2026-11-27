# Control-plane API

Base path: `/api/v1`

All endpoints use HTTPS in deployed environments. The control plane carries metadata only; it must never proxy VPN user traffic.

## Gateway discovery

`GET /api/v1/servers`

Optional query parameter:

- `region`: region code filter (2–32 ASCII letters, digits, or hyphens)

Example response:

```json
{
  "servers": [
    {
      "id": "gateway-uuid",
      "region": "de-fra-1",
      "countryCode": "DE",
      "city": "Frankfurt",
      "endpoint": {
        "host": "vpn.example.net",
        "port": 51820
      },
      "publicKey": "base64-wireguard-server-public-key",
      "capacityMbps": 1000,
      "loadPercent": 12.5,
      "lastHealthAt": "2026-11-27T12:00:00.000Z"
    }
  ]
}
```

The example is a response-shape illustration, not a provisioned gateway. Production responses are read from PostgreSQL and include only gateways whose control-plane health state is `healthy`. The API does not synthesize server entries or health values. Responses use `Cache-Control: no-store`.

Errors:

- `400 invalid_query`: malformed region filter
- `503 server_registry_unavailable`: database registry could not be queried

## Anonymous Android device enrollment

`POST /api/v1/devices/enroll`

Rate limit: 5 requests per source IP per hour, in addition to the API-wide limit.

Request body:

```json
{
  "installationId": "43-character-base64url-installation-id",
  "platform": "android",
  "wireGuardPublicKey": "base64-encoded-32-byte-public-key"
}
```

The client generates the WireGuard key pair locally. The private key is never sent. The API validates the public key, stores a SHA-256 hash of the installation identifier, stores the public key, and returns a random device credential once. Only a SHA-256 hash of that credential is stored in PostgreSQL. The Android client persists its credential and private key in an AES-GCM encrypted record protected by Android Keystore.

Successful response (`201`):

```json
{
  "deviceId": "device-uuid",
  "deviceToken": "opaque-one-time-returned-credential",
  "createdAt": "2026-11-27T12:00:00.000Z"
}
```

Responses include `Cache-Control: no-store`. Duplicate installation identifiers or public keys return `409 device_already_enrolled`. Invalid payloads or WireGuard keys return `400`.

**Deployment requirement:** apply `backend/migrations/001_initial_schema.sql` and then `backend/migrations/002_device_enrollment.sql` before enabling this route in a deployed control plane.

The credential is accepted by the following device endpoints:

- `GET /api/v1/devices/me`: validates the bearer credential and updates `last_seen_at`.
- `DELETE /api/v1/devices/me`: revokes the current device credential and device record; returns `204` on success.

Both endpoints return `401` for missing, malformed, revoked, or unknown credentials. The credential is never returned again after enrollment. A client that loses its local credential cannot recover it from the server; a dedicated secure recovery/re-enrollment flow is required.

## Not yet implemented

The following operations are still required before the client can receive a usable tunnel profile:

The following operations must be added before a client can receive a usable tunnel profile:

1. Gateway peer reconciliation using authenticated, least-privilege gateway control credentials.
2. Atomic tunnel-address allocation and per-device profile issuance.
3. Secure credential recovery/re-enrollment after device migration or reinstall.

Never accept a client-supplied WireGuard private key. The server must never return a shared or static peer configuration to multiple devices.
