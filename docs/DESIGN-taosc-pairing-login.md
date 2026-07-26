# taOSc pairing and login

Date: 2026-07-26. Status: proposed - needs @taOS-dev
(core: Decisions + devices) and @taOS-website-dev (taOSgo/taos.my side)
review before the server card goes claimable.

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

1. User signs into their taOSgo account in the app.
2. taos.my (control plane only - never in the data path, per bus 1287/1314)
   returns the user's instance identity and mints a **tailnet preauth join
   key** (the existing P3 host-pairing / P5 cluster-join primitive) so the
   phone can reach the instance off-LAN.
3. Phone joins the user's own tailnet, then runs the **same grant flow**
   against the instance URL.

Per 1314's standing caveats, these bind the pairing cards: deny-by-default
ACL review (`deploy/headscale/policy.hujson`, autogroup:self) before the
first phone joins; phone nodes ephemeral as a deliberate choice; any
taos.my API use via a scoped controller token (`require_account_scope`),
never a browser session.

**Open question for @taOS-website-dev:** the taOSgo account-login + join-key
mint API from a native app, and the practical shape of tailnet join on
iOS/Android (Tailscale SDK vs companion VPN app) - this decides how much of
method B is v1.

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
