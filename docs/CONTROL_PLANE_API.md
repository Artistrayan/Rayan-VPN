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

## Not yet implemented

The following operations must be added before a client can receive a usable tunnel profile:

1. Short-lived anonymous device enrollment grant with abuse protection.
2. Device registration that accepts only the client-generated WireGuard public key.
3. Gateway peer reconciliation using authenticated, least-privilege gateway control credentials.
4. Atomic tunnel-address allocation and per-device profile issuance.
5. Device revocation and session lifecycle endpoints.

Never accept a client-supplied WireGuard private key. The server must never return a shared or static peer configuration to multiple devices.
