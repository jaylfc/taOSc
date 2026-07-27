# taOSc pairing and login

Date: 2026-07-26 (revised 2026-07-27). Status: BOTH HALVES REVIEWED and
cleared to card. taOSgo half endorsed by @taOS-website-dev (bus 1360;
endpoint carded as tsk-5rukhy on prj-utbsh7). Core half reviewed by
@taOS-dev (bus 1490): sound to card with F1 and F2 written in, which this
revision does.

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

### Identity binding (core review, bus 1490 - both are must-specify)

**F1. The device binds to the APPROVING human's user_id.** The pair-request
is unauthenticated, so it has no session owner. The minted device binds to
the user_id of the session that ANSWERS the grant Decision
(`answer_decision`'s `user.user_id` in `routes/decisions.py`) - never a
fixed or admin default. Anything else is a cross-user device grant under
multi-user.

**F2. Approval binds to the specific pair_request_id, not "the pending
request".** Follow the existing metadata side-effect pattern: the Decision
carries `metadata={kind: "device_pairing", pair_request_id}`, and a
`_apply_device_pairing_grant` handler (sibling to `_apply_execution_grant` /
`_apply_app_grant`) transitions THAT request with an atomic
`UPDATE ... WHERE id=? AND status='pending'`, exactly as
`auth_requests_store.set_decision` does. A generic "allow taOSc?" grant
would let a racing attacker's request ride the victim's approval - the
classic confused deputy.

### Hardening by reuse (bus 1490; reuse, do not re-implement)

- **F3.** `verify_code` is a human comparison nonce only: never sent back to
  the server, never server-checked. Secrets module, >= 6 digits. Do NOT add
  a server-side code check.
- **F4.** Cap TOTAL pending pair-requests (mirror the auth-requests
  `_PENDING_CAP`) plus ~10 min expiry - not per-IP only, which a CGNAT or
  distributed flood defeats while raising code-collision odds.
- **F5.** Mint via the existing `DeviceStore.register` (gets the `taosdev_`
  token, per-user cap of 50, touch/last_seen, revoke and `require_device`
  for free). Apply an ios/watchos/android platform whitelist - the store
  does not validate. Wire `POST /api/devices/pair-requests` and
  `GET .../{id}` into `auth_middleware._is_exempt` using the exact
  method-sensitive single-segment pattern used for
  `/api/agents/auth-requests/{id}`. Add a per-request approve lock (TOCTOU
  double-mint).
- **F6.** Enforce expiry at APPROVE time (pending AND not-expired ->
  accepted), not merely hidden from the poll.
- **F7.** taOSgo adds NO new trust path: the phone still runs the same local
  grant Decision. Do not add a taOSgo-specific shortcut.

Model to copy verbatim: `routes/agent_auth_requests.py`,
`auth_requests_store.py` (atomic `set_decision`), and the `_apply_*_grant`
handlers in `decisions.py`.

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
