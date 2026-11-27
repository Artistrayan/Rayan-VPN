# Android WireGuard integration

- Official \`com.wireguard.android:tunnel:1.0.20260102\` AAR from Maven Central.
- \`GoBackend\` userspace engine supports non-root Android devices.
- \`GoBackend.VpnService\` owns the Android VPN interface; the app service only manages foreground lifecycle.
- WireGuard \`Config.parse\` validates profiles.
- Profiles are encrypted at rest using AES-GCM with an Android Keystore key.
- Traffic counters come from WireGuard peer statistics, not generated values.

## Provisioning gate

The control plane does not yet have a live node inventory or a configuration-issuance endpoint. Fresh installs therefore fail closed because no profile is present. No placeholder endpoint is created and no successful connection is reported.

The future authenticated TLS enrollment response must provide a device-specific profile and pass it to \`WireGuardConfigStore.save(serverId, profile)\`. The client private key must be generated and retained on-device; the backend stores only the device public key and server-side peer metadata.

## Connection truth conditions

1. A profile exists and parses using WireGuard's parser.
2. \`GoBackend.setState(..., UP, config)\` succeeds.
3. \`GoBackend.getState(tunnel)\` confirms UP.
4. A peer handshake is observed before CONNECTED is reported.
5. Traffic rates and totals come only from WireGuard statistics.

Unknown exit IP, ping and jitter remain unknown. Real nodes, peer provisioning, firewall/NAT, DNS/IPv6 policy and leak tests remain required before production.
