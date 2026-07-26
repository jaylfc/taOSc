# taOSc pairing and login

Date: 2026-07-26. Status: taOSgo half REVIEWED and endorsed (bus 1360;
website-side endpoint carded as tsk-5rukhy on prj-utbsh7). Core half (S4e
pair-requests + grant Decision) still awaiting @taOS-dev review before that
card goes claimable.

Jay's requirements (2026-07-26, verbatim intent):
1. **Two login methods** in the apps: **taOSgo** (paid cloud subscription) or
   **LAN** (manual IP/URL entry).
2. **First connect is an explicit grant**: when an app first tries to
   connect, the user gets a notification in taOS to allow/deny taOSc
   connecting. Pairing is consent, not a side effect of being logged in.

This supersedes the S1 assumption that pairing rides an existing web session
(`POST /api/devices/register` returns the token immediately). That route can
stay for session-context registration, but the app flow becomes
request → grant → token.

## The grant flow (both login methods converge here)

Deliberately the same shape as the proven agent auth-request flow, surfaced
through the existing Decisions system:

1. App sends an **unauthenticated pairing request** to the instance:
   `POST /api/devices/pair-requests` `{platform, display_name}` →
   `{pair_request_id, verify_code}`.
2. The server raises a **Decision** to the instance's user - "taOSc on
   'Jay's iPhone' wants to connect. Code 481-905. Allow?" - surfacing in the
   bell/Decisions app like any other approve/deny (and, once phones exist,
   pushed to already-paired devices).
3. The app displays the same `verify_code`; the user approves only if the
   codes match (kills a racing attacker's parallel request).
4. App polls `GET /api/devices/pair-requests/{id}` → on approval:
   device row + `scoped_token` (shown once → Keychain). On deny/expiry:
   terminal status.

Abuse controls: requests expire (~10 min), unauthenticated POST is
rate-limited per IP, the Decision shows requester IP + platform, and
`DELETE /api/devices/{id}` (already merged) revokes any grant later.

## Login method A: LAN

First-run screen (S4a already builds the URL entry): user types the
instance URL/IP, app runs the grant flow against it directly. Done - no
cloud dependency, works fully offline from taos.my.

## Login method B: taOSgo (paid cloud)

taOSgo is **discovery + reachability**, not a different trust path:

1. The app calls `POST /api/taosgo/app-join` (website-side, carded as
   tsk-5rukhy): credentials-in-body, one shot -
   `{email, password, device_name?}` ->
   `{join_key, login_server, hosts: [{handle, addr}]}`. **No taos.my session
   or token is created or stored on the phone.** Its durable credentials are
   the tailnet node key plus the instance device token from the grant flow,
   which makes control-plane-only structural rather than promised.
   Gate order: transport 503, per-IP limit 429 (pre-auth), timing-equalised
   auth 401, email_verified 403, taosgo_status in trialing/active 403.
   `hosts: []` with 200 when the account has no instance yet - the app shows
   guidance, not an error.
2. Phone joins the user's own tailnet with the key, then runs the **same
   grant flow** against the instance URL.

Settled positions binding the pairing cards (1314 as amended by 1360):
deny-by-default ACL review (`deploy/headscale/policy.hujson`,
autogroup:self) is website-side work and gates the first real phone JOIN,
not the client build; phone nodes are **non-ephemeral** (1360 divergence
from 1314: join keys are single-use with a TTL, so an auto-removed
ephemeral node would force password re-entry whenever the phone sleeps too
long - node hygiene comes from revocation instead; **flagged for Jay's veto
pre-merge**).

**Tailnet join, client half (my call per 1360):** iOS v1 uses the official
Tailscale app pointed at the custom `login_server` - an embedded
NetworkExtension via libtailscale is entitlement- and effort-heavy and is
deferred. Android decides at S3 pairing-card time (either route works;
nothing website-side changes with the choice).

## Card impact

- **S4a (tsk-gghqn3, claimable):** unchanged - the shell + URL entry IS the
  LAN entry point. Builder note added: first-run UI should not assume a
  single login method exists forever.
- **NEW S4e (backend, core, held for review):** pair-request endpoints + the
  grant Decision. Prerequisite for S4b.
- **S4b (client pairing, held):** two-method chooser; LAN + grant flow in
  v1; taOSgo path carded separately once website-dev settles the API.
- **S4d (tsk-wtkizs, claimable):** unaffected - device-bearer self-service
  is needed regardless of how the token was granted.
- **S4c (push + actionable Decisions, held):** unchanged in content; blocked
  on S4b + S4d.
