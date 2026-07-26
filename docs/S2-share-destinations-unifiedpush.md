Slice 2 of #2128. Pure backend, no Mac required, and it is the critical path: it unblocks the Android shell **and** the iOS share extension.

Two independent pieces, listed in the order they should land.

## A. GET /api/share/destinations

The share picker must not hardcode its list. What a user can share into depends on their projects, their agents, and their grants, so the client asks and renders the answer.

Returns the destinations **this device's token may write to**:
- the Library (always available to a paired device)
- each project the user can write files to
- each agent the user can message

Shape roughly:
```json
{"destinations": [
  {"kind": "library", "id": "library", "label": "Library"},
  {"kind": "project_files", "id": "taos", "label": "taOS"},
  {"kind": "agent_chat", "id": "mary", "label": "Mary"}
]}
```

Each entry carries what the client needs to POST to the **existing** endpoint for that kind. No new ingest paths:
- `library` -> `POST /api/library/ingest` (multipart `file`, or form `url`)
- `project_files` -> `POST /api/projects/{slug}/files/upload`
- `agent_chat` -> the existing chat message path

**Authorization is the point of this endpoint.** It must filter by what the caller may actually write, not list everything and let the POST fail. A picker that offers a project the user cannot write to is a bug that surfaces as a confusing failure *after* the user has already chosen.

Device tokens are scoped (`device_auth.py`), so this respects device scope, not just user identity.

## B. Push for Android: UnifiedPush, not Firebase

**Decision (Jay, 2026-07-26): self-hosted only. No Firebase.**

Context for anyone picking this up, because the two platforms are not symmetric:

- **iOS: APNs is mandatory.** Apple provides no alternative for background push. A notification cannot reach an iPhone without transiting Apple's servers. This is a hard platform constraint, not a preference. `push/apns.py` already exists.
- **Android: FCM is optional.** Android permits self-hosted push, so we take that route.

**Approach: UnifiedPush**, the open standard for exactly this. The app registers with a *distributor* of the user's choosing and hands the resulting endpoint to taOS; the server then pushes to that endpoint. Default distributor: **ntfy**, self-hosted.

Why this fits taOS specifically:
- ntfy is small, HTTP-based and ARM-friendly, so it can run as a managed backend on the Pi alongside the others
- no Google account, no service-account JSON, no third party in the notification path
- users who genuinely want FCM can still choose an FCM-backed distributor without the app changing

Implementation notes:
- `push/unifiedpush.py` as a sibling to `push/apns.py`, behind one shared interface
- The device row already carries `platform`, so dispatch should be a **lookup, not a branch scattered through the notification code**. Introduce the small dispatch seam rather than `if platform == "ios"` at every call site.
- For UnifiedPush the stored push token is an **endpoint URL**, not an opaque device token. Check `device_store`'s `push_token` column is happy holding a URL, and that `PATCH /api/devices/{id}/push-token` accepts one.
- Both platforms must support **actionable** notifications: a Decision maps to approve / deny / pick-option / quick-reply. A Decision the user cannot answer from the lock screen defeats the point of putting it on the phone.

**Reachability caveat:** a self-hosted distributor must be reachable from the phone when off-LAN. That routes through the same Headscale relay the client design already assumes, so it composes with the existing plan rather than adding a new problem. Worth verifying early rather than at the end.

## Acceptance

- `GET /api/share/destinations` returns only destinations the calling device may write to, verified with a device token that lacks access to a given project.
- Posting a shared item to each returned destination succeeds against the existing endpoints.
- A UnifiedPush notification reaches an Android device via a self-hosted ntfy distributor, and is actionable.
- Dispatch chooses APNs or UnifiedPush from the stored `platform` with no platform branching at the call site.
- No Firebase dependency anywhere in the tree.

## Notes

- `device_store` already has the generic `platform` column, so no migration is needed for Android.
- Do not add new ingest endpoints. The three destinations already exist server-side; this slice is about *discovery and authorization*, not new write paths.

---
**Amendment 2026-07-26 (@taOS-dev):** the reachability sentence above ("routes
through the same Headscale relay") is superseded by the settled position in A2A
bus msg 1314/1287 (@taOS-website-dev, grounded in the relay code): the phone
joins the user's OWN tailnet via the existing preauth-key flow; the ntfy
distributor runs on the user's own instance; taos.my is control plane only and
never in the notification data path; the Caddy relay is per-request forward-auth
and cannot hold a distributor subscription. Card acceptance criteria must
include: ACL review (deploy/headscale/policy.hujson, autogroup:self) before the
first phone joins; ephemeral flag for phone nodes as a deliberate choice; scoped
controller token (require_account_scope) for any taos.my API use. A hosted
distributor as a Pro fallback is an OPEN Jay decision - do not card it.
