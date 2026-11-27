# Rayan VPN

Rayan VPN is an Android-first VPN product targeting a global audience. The product model is hybrid: a usable free tier supported by clearly disclosed in-app advertising, plus an optional premium subscription. Initial access must be possible without creating an account; accounts are optional and used for cross-device entitlement, purchases, and support features.

## Product decisions

- Product: **Rayan VPN**
- Platform: Android first
- VPN data plane: WireGuard
- Android integration: Android `VpnService` with the official WireGuard tunnel implementation
- Hosting: multiple independent infrastructure providers across regions
- Account model: optional for initial connection
- Revenue: free tier + optional advertising/rewarded ads + premium subscription
- Backend: TypeScript, Fastify, PostgreSQL, Redis (Redis is introduced when rate limits/session coordination require it)

## Repository layout

- `app/`: Android application (Kotlin, Jetpack Compose)
- `backend/`: control-plane API (Fastify, TypeScript)
- `infra/`: server provisioning and hardening (to be added)
- `docs/`: architecture, security, and delivery decisions

## Important current-state warning

The existing Android UI and service are a prototype, not a production VPN. The current `AriaVpnService` establishes an Android TUN interface but does not attach a WireGuard userspace tunnel or forward packets. It also contains simulated traffic counters, randomized latency, and placeholder exit IP addresses. Therefore the current app must **not** be described or released as providing a working VPN until the real WireGuard data plane and end-to-end leak tests are implemented.

See [Repository audit](docs/REPOSITORY_AUDIT.md), [Architecture](docs/ARCHITECTURE.md), and [Roadmap](docs/ROADMAP.md).

## Development

### Backend

Requirements: Node.js 22+ and PostgreSQL 16+.

```bash
cd backend
cp .env.example .env
npm install
npm run dev
```

The API exposes liveness and readiness endpoints. Readiness requires a working PostgreSQL connection.

### Android

Open the repository in the current stable Android Studio and use the Gradle wrapper. Do not configure production endpoints or signing credentials in source control. The real WireGuard integration is a release blocker and is tracked in the roadmap.

## Security principles

- Never log VPN payloads, private keys, authentication tokens, or full tunnel configurations.
- Generate device WireGuard private keys locally; only public keys are registered with the control plane.
- Do not claim a connection is secure until the WireGuard tunnel handshake and routing are verified.
- Keep the control plane separate from the VPN packet data plane.
- Use least-privilege server credentials, encrypted transport, strict firewall rules, and auditable infrastructure changes.
- Collect the minimum operational metadata needed for abuse prevention and service reliability.
