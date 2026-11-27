# Existing repository audit

Audit baseline: `main` at the time this document was added.

## What already exists

- Kotlin Android application using Jetpack Compose.
- Main, server-list, settings, and subscription screens.
- Android `VpnService` declaration and foreground-service notification.
- A service class named `AriaVpnService`.
- UI models for servers, session state, and traffic.
- Existing ad/reward and subscription UI components.
- Gradle project and basic unit/instrumentation test structure.

## Critical correctness findings

### 1. No real VPN data plane

`app/src/main/java/com/example/vpn/AriaVpnService.kt` calls Android `VpnService.Builder.establish()`, but it does not pass the resulting TUN file descriptor to a WireGuard backend or a packet-forwarding engine. Establishing the TUN interface alone does not create an operational VPN tunnel. There is no verified remote peer handshake or remote gateway routing in this implementation.

### 2. Simulated telemetry

The service currently:
- adds synthetic bytes to traffic totals rather than measuring actual tunnel byte counters;
- generates jitter and ping using random values;
- falls back to a fabricated bandwidth value;
- assigns placeholder IP addresses based on country selection.

These values must be removed from production UI and backend accounting. Metrics must come from the actual WireGuard tunnel and trusted server-side counters.

### 3. Client-side entitlements are not authoritative

Any free-session timer, VIP state, or rewarded-ad credit enforced only inside the Android process can be modified by a user. Production entitlements, reward grants, quotas, and subscription verification must be validated by the backend and/or Google Play purchase verification.

### 4. Prototype identity and configuration

The Android namespace and application ID currently use example/Aria identifiers. Before release, migrate to a stable Rayan VPN package namespace and configure production signing, privacy disclosures, app links, and environment-specific API endpoints. Do not change signing configuration without preserving the existing build environment's requirements.

### 5. No control-plane service was present in the inspected tree

The repository contained Android code and configuration but no backend API, database migrations, or automated infrastructure provisioning. The initial backend workspace is being introduced separately.

## Release blockers

- Official WireGuard tunnel integration and handshake verification.
- Secure key lifecycle and device registration.
- Real remote VPN gateways in multiple regions.
- IPv4/IPv6 routing and DNS leak prevention.
- Fail-closed behavior and verified kill-switch semantics.
- Server-side usage accounting and entitlement enforcement.
- Subscription receipt verification and server-side rewarded-ad validation.
- Privacy policy, Data safety declarations, VpnService disclosures, and policy review.
- Automated Android build, backend tests, dependency/security scans, and end-to-end tests.

## Release rule

Until every critical data-plane and privacy release blocker is closed, the app is a development prototype and must not advertise itself as a working privacy VPN.
