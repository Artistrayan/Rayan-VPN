# Rayan VPN gateway provisioning

This Ansible playbook provisions the first real WireGuard gateway on Ubuntu 22.04+
with a public IPv4 address. It installs the official WireGuard tools, creates the
gateway private key **on the gateway itself**, enables IPv4 forwarding, configures
NAT, and enables `wg-quick@wg0`.

It does not create fake endpoints, add client peers, register a gateway in the
control-plane database, or issue client profiles. Those operations remain blocked
until the authenticated gateway reconciliation service is implemented.

## Requirements

- A real Ubuntu 22.04/24.04 VPS with a public IPv4 address and outbound internet.
- A DNS A record (recommended) pointing to that VPS.
- SSH access with a dedicated key and a sudo-capable account.
- Ansible Core 2.15+ on the operator workstation.
- In the provider firewall/security group, allow UDP 51820 inbound. Keep SSH
  restricted to the operator's trusted IP range. Do not expose PostgreSQL.
- Confirm that the provider permits VPN traffic and IP forwarding.

## Prepare inventory

Copy `inventory/hosts.yml.example` to `inventory/hosts.yml`, then replace
the documentation IP, DNS name, SSH user, key path and gateway metadata with
real values. Copy `group_vars/gateways.yml.example` to
`group_vars/gateways.yml` if you need to override defaults.

The example uses the reserved TEST-NET address 203.0.113.10 and is intentionally
not deployable as-is.

## Validate and deploy

From this directory:

```bash
ansible --version
ansible-inventory --list
ansible-playbook -i inventory/hosts.yml.example gateway.yml --syntax-check
ansible-playbook -i inventory/hosts.yml gateway.yml --check --diff
ansible-playbook -i inventory/hosts.yml gateway.yml
```

The check-mode command must use the real inventory file after you create it.
The private key is generated on the remote gateway and never exported to the
Ansible controller. The final WireGuard config is assembled on the gateway with
root-only permissions; its private-key field is therefore also protected by
mode 0600. The playbook suppresses output from key handling.

## Verify on the gateway

```bash
sudo systemctl status wg-quick@wg0 --no-pager
sudo wg show wg0
sudo sysctl net.ipv4.ip_forward
sudo ss -lunp | grep 51820
sudo cat /etc/wireguard/private.key | wg pubkey
```

The last command prints only the public key. Never copy or publish
`/etc/wireguard/private.key`.

## Control-plane registration

After provisioning, an operator must register the gateway in the existing
`gateways` table with its actual provider, region, endpoint, UDP port, and
public key. Set its status to `provisioning` until the control plane can
authenticate the gateway and verify a successful health check. Do not mark it
`healthy` manually merely to make it appear in the Android server list.

The playbook currently configures IPv4 NAT only. IPv6 is intentionally not
advertised to clients until routed IPv6, firewall policy, and leak tests are
implemented. The first release must use a client profile with an explicit
IPv4-only AllowedIPs policy until IPv6 support is verified.
