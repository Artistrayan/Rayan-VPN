# Implementation roadmap

## Phase 0 — Baseline and correctness
- [x] Confirm product decisions: Android, hybrid revenue, global target, multi-provider hosting, optional accounts.
- [x] Audit existing Android prototype.
- [x] Document simulated VPN behavior and release blockers.
- [ ] Establish CI and reproducible Android/backend builds.
- [ ] Replace Aria/example package identity with approved Rayan identity without breaking signing/build workflow.

## Phase 1 — Control-plane foundation
- [x] Create backend workspace and health/readiness API.
- [ ] Add PostgreSQL migrations and repository layer.
- [ ] Add anonymous device bootstrap and short-lived registration grants.
- [ ] Add device public-key registration and revocation.
- [ ] Add authenticated gateway registry and health reporting.
- [ ] Add API rate limits and audit events without sensitive payload logging.

## Phase 2 — Real WireGuard data plane
- [ ] Integrate the official WireGuard Android tunnel implementation.
- [ ] Generate device keys locally and persist them securely.
- [ ] Provision the first real gateway using infrastructure-as-code/Ansible.
- [ ] Implement peer provisioning and revocation.
- [ ] Confirm handshake before showing CONNECTED.
- [ ] Measure actual tunnel transfer counters and actual endpoint latency.
- [ ] Test IPv4, IPv6, DNS, MTU, reconnect, sleep/resume, and network handover.

## Phase 3 — Security and reliability
- [ ] Fail-closed behavior and lockdown/kill-switch validation.
- [ ] Gateway hardening, firewall, NAT, sysctl, patching, and SSH policy.
- [ ] Automated secret scanning, dependency scanning, and SBOM.
- [ ] End-to-end leak tests on physical Android devices.
- [ ] Incident response, abuse handling, and privacy retention controls.

## Phase 4 — Global operations
- [ ] Add a second provider and at least three production regions.
- [ ] Gateway health/capacity controller and safe server selection.
- [ ] Usage accounting from gateway-side counters.
- [ ] Capacity planning and per-provider bandwidth cost reporting.

## Phase 5 — Monetization
- [ ] Google Play Billing integration and backend purchase-token verification.
- [ ] AdMob integration after policy and consent review.
- [ ] Server-side rewarded-ad SSV verification.
- [ ] Free-tier fair-use policy and premium entitlements.
- [ ] Refund, cancellation, grace-period, and restore-purchase flows.

## Phase 6 — Release readiness
- [ ] Privacy policy and data-retention disclosures.
- [ ] Google Play VpnService declaration and review.
- [ ] Data safety and ads declarations.
- [ ] Signed AAB, internal testing, staged rollout, and monitoring.
- [ ] Public release only after all data-plane release blockers are closed.

## Acceptance criteria for a real connection

A connection is accepted as operational only if all of the following are true:
1. Android has obtained user consent for the VPN service.
2. The official WireGuard tunnel backend has successfully started.
3. The remote peer handshake is recent and verified.
4. A public-IP check reports the expected gateway egress address.
5. DNS and IPv6 leak tests pass for the supported device/network matrix.
6. Actual byte counters increase when traffic is transferred.
7. Disconnect/reconnect and network handover behave as specified.

## M1 implementation notes — Android WireGuard engine

- The official WireGuard tunnel AAR is integrated through Maven Central.
- The app delegates VPN interface ownership to `GoBackend.VpnService`; the app's own service only orchestrates foreground lifecycle and telemetry.
- Tunnel profiles are parsed by WireGuard's own `Config` parser and stored encrypted with an Android Keystore AES-GCM key.
- The connection service refuses to report CONNECTED unless a profile exists, the GoBackend tunnel is UP, and a peer handshake has been observed.
- Traffic totals and rates come only from WireGuard peer statistics. Unknown exit IP, ping and jitter remain unknown.
- Region cards are non-operational until the control plane provisions and health-checks real nodes.
- Rewarded-ad quota grants are disabled until server-side validation is implemented.
- Remaining gate: provision a real node and connect the control plane to deliver a per-device configuration.
