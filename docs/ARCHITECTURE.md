# Rayan VPN architecture

## 1. System boundaries

Rayan VPN has two independent planes:

- **Control plane:** app configuration, anonymous device bootstrap, optional account identity, public-key registration, server discovery, policy/entitlement decisions, subscription verification, and aggregate usage metadata.
- **Data plane:** encrypted WireGuard packets between the Android device and a regional gateway. User traffic must not pass through the API service.

The API must never proxy ordinary user internet traffic.

## 2. Android client

- Kotlin + Jetpack Compose for UI.
- Android `VpnService` for OS VPN lifecycle, consent, foreground execution, and TUN ownership.
- Official WireGuard Android tunnel implementation for cryptography, peer configuration, and packet transport.
- Locally generated WireGuard private key; private key remains on device and is stored using Android Keystore-backed protection where supported.
- Backend receives only the device public key and a device-scoped identifier.
- Explicit connection states: idle, preparing, connecting, connected (only after handshake), reconnecting, stopping, failed.
- No synthetic ping, throughput, byte counters, exit IP, or fake connected state.

## 3. Anonymous-first identity

Initial connection is available without an email/password account.

1. App generates a random installation/device identifier and WireGuard key pair.
2. App requests an anonymous device registration challenge from the API.
3. API applies abuse controls and issues a short-lived, device-bound registration grant.
4. App registers its public key and receives a signed/short-lived peer configuration.
5. The gateway is configured with the public key and assigned tunnel address.
6. App starts WireGuard and reports connection lifecycle metadata, not payload data.

Account creation is optional. Linking an account associates the anonymous device with an account after proof of device possession. Account linking must not silently rotate or expose private keys.

## 4. Global gateway topology

Use multiple infrastructure providers and regions to reduce provider concentration risk. A region is not considered available until it has:
- a hardened Linux host;
- WireGuard installed and configured;
- a restrictive ingress/egress firewall;
- a tested routing/NAT policy;
- a health agent with authenticated control-plane reporting;
- capacity and bandwidth monitoring;
- a documented abuse response and provider-compliance process.

The control plane maintains a registry of gateways and their health/capacity. Server selection considers measured latency, current load, region availability, plan eligibility, and abuse/risk policy. Never return an unprovisioned or unhealthy node as selectable.

## 5. Backend services

Initial modular monolith:
- Fastify HTTP API
- PostgreSQL for durable users/devices/gateways/keys/sessions/entitlements/usage/purchases
- Redis for short-lived challenges, distributed rate limits, and coordination when required
- background worker for gateway reconciliation, usage aggregation, and purchase notifications
- provider-specific infrastructure adapters behind a stable gateway interface

Avoid microservices until measured scale or independent operational requirements justify them.

## 6. Revenue and entitlement

- Free tier: real usable access, subject to a clearly documented fair-use capacity policy.
- Advertising: shown only in the app UI. Rewarded ads are optional; a reward is granted only after server-verifiable ad completion validation. Ads never modify or inspect VPN payload traffic.
- Premium: Google Play Billing for Play-distributed builds. Backend verifies purchase tokens with Google Play Developer API before granting entitlements.
- Never trust client-side VIP flags, ad callbacks, local timers, or purchase success screens as proof of entitlement.

## 7. Privacy and telemetry

Do not collect browsing history, DNS query history, destination IPs, packet contents, or full traffic logs. Operational telemetry should be minimized to what is needed for security, capacity, billing/fair-use, and reliability. Publish retention periods and deletion procedures. Separate abuse/security events from product analytics and restrict access.

## 8. Availability and failure behavior

- No healthy gateway: fail closed and show a clear unavailable state.
- Tunnel handshake absent: never display connected.
- Gateway unhealthy during session: stop or reconnect according to a documented policy; prevent unintended direct traffic when the user has enabled always-on/lockdown behavior.
- API outage after a valid tunnel is established: define a bounded behavior for the existing tunnel; do not silently weaken encryption or route around the VPN.
- Control-plane and gateway credentials must be separate and least-privileged.
