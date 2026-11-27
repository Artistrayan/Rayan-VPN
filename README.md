# Rayan VPN

Rayan VPN is an Android-first VPN product targeting a global audience.

## Product decisions

- Product: **Rayan VPN**
- Platform: Android first
- VPN data plane: official WireGuard Android tunnel implementation
- Hosting: multiple independent infrastructure providers across regions
- Account model: optional; initial access is designed to work without an account
- Revenue: free tier supported by clearly disclosed advertising, plus optional premium subscription
- Control plane: TypeScript, Fastify, PostgreSQL; Redis is introduced when distributed rate limits and coordination require it

## Repository layout

- `app/`: Android application (Kotlin, Jetpack Compose)
- `backend/`: control-plane API (Fastify, TypeScript)
- `infra/`: gateway provisioning and hardening (not yet implemented)
- `docs/`: architecture, security, and delivery decisions

## Current implementation status

### Android

- The app integrates the official WireGuard Android userspace backend.
- WireGuard profiles are parsed by the official configuration parser and encrypted at rest using Android Keystore AES-GCM.
- Traffic counters come from the WireGuard backend.
- The app reports CONNECTED only after the backend is up and a peer handshake is observed.
- No production gateway profile is currently provisioned. A fresh installation therefore fails closed instead of connecting to a placeholder server.

### Control plane

- Fastify service, PostgreSQL connection pool, liveness and readiness endpoints are present.
- `GET /api/v1/servers` lists only database gateways whose health status is `healthy`. It never invents regions, endpoints, latency, or capacity.
- Anonymous device enrollment, key registration, gateway peer reconciliation, and device-specific profile issuance are not yet implemented.

**This repository is not yet a usable production VPN service.** A real gateway fleet, authenticated device enrollment, secure peer provisioning, routing/NAT, DNS and IPv6 policy, abuse controls, and end-to-end leak testing are required before public release.

## Development

### Backend

Requirements: Node.js 22+ and PostgreSQL 16+.

```bash
cd backend
cp .env.example .env
npm install
npm run dev
```

The API exposes liveness, readiness, and healthy-gateway discovery endpoints. Readiness requires a working PostgreSQL connection.

### Android

Open the repository in a current stable Android Studio installation. Use the Gradle wrapper if one is added to the repository, or the configured Gradle installation in CI. Do not commit production endpoints, signing credentials, gateway private keys, or device private keys.

## Security principles

- Generate WireGuard private keys locally; only public keys are registered with the control plane.
- Never log VPN payloads, private keys, authentication tokens, or full tunnel configurations.
- Do not claim a connection is secure until the WireGuard handshake and routing are verified.
- Keep the control plane separate from the VPN packet data plane.
- Use least-privilege gateway credentials, encrypted transport, strict firewall rules, and auditable infrastructure changes.
- Collect the minimum operational metadata needed for abuse prevention and service reliability.
