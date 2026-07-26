Jay, 2026-07-26: a companion app for Android/Wear and iOS/watchOS. Its purpose is a **canvas** and a **notification system** on mobile, plus **share to taOS** on both platforms.

Full design: taOS project Files, `Design docs/DESIGN-taosc-companion-app.md`.

## Not greenfield: what already exists

Slice 1 of the Apple client is **merged** (#1671), and crucially it is not Apple-specific:

- `device_store.py` has a generic `platform TEXT` column, so **Android devices need no schema migration**
- `/api/devices/register`, list, `PATCH .../push-token`, `DELETE` are all platform-agnostic
- `device_auth.py` already models per-device scoped tokens
- `push/apns.py` exists; **there is no FCM**
- `clients/` does not exist yet, so no shell on either platform

The share destinations also already exist server-side: `POST /api/library/ingest` (file or url), `POST /api/projects/{slug}/files/upload`, and the chat message path. So share is mostly client work plus one small endpoint.

The expensive, easy-to-get-wrong part (device identity and scoped tokens) generalises for free. That is why adding a second platform is affordable.

## Architecture, inherited deliberately

The Apple design's central call was **native shell + system WebView hosting the existing taOS web UI**, dropping to native only for push, share, sensors and secure storage. That generalises to Android unchanged (`WKWebView` / `WebView`), so **the canvas is not written twice**. taOSc is roughly one UI codebase plus four thin native shells, not four apps.

Watch is fully native on both (no usable WebView) and scoped narrow on purpose: notifications and Decisions only, never the canvas.

## Decisions taken (Jay, 2026-07-26)

1. **Mac access exists** via borrow or CI, so iOS and Android proceed in parallel.
   *Update 2026-07-26:* Xcode is installed on the Mac mini (iOS 26.5 + watchOS
   26.5 SDKs verified, reachable over SSH); simulator builds are unblocked.
   Only Apple Developer signing remains for device/TestFlight. Worth recording as a hard dependency though: iOS and watchOS cannot be built or signed without a Mac and an Apple Developer account. Android builds on Linux. The old design assumed "builds run locally on the developer's Mac", which briefly stopped being true when the lead machine moved to Linux.
2. **Share asks each time** rather than assuming a destination: Library, a project's Files, or an agent chat.
3. **Watch is core v1**, not a follow-up. This changes the Apple design's sequencing, which had watchOS last.
4. **Two login methods + consent pairing** (Jay, 2026-07-26): apps sign in
   via taOSgo (paid cloud) or LAN manual URL; first connect raises a
   grant/allow notification in taOS. Pairing becomes request→grant→token
   (`DESIGN-taosc-pairing-login.md`), superseding session-side-effect
   registration for the app flow.
5. **Canvas on the watches too** (Jay, 2026-07-26, later the same day). Neither
   watch OS has a usable WebView, so this cannot reuse the web canvas: it is a
   native renderer over a constrained projection of canvas state. Tracked as
   S7 (`DESIGN-S7-watch-canvas.md`); S6 stays notifications-only and ships first.

## Slices (each independently shippable)

- [x] **S1. Server foundation** (#1671, merged)
- [ ] **S2. FCM push + `GET /api/share/destinations`** - pure backend, no Mac needed, unblocks Android *and* the iOS share extension
- [ ] **S3. Android phone shell** - WebView canvas, pairing, FCM, actionable notifications
- [ ] **S4. iOS phone shell** - the Apple design's Slice 2, unchanged
- [ ] **S5. Share targets** - Android intent filters + iOS Share Extension onto S2's picker
- [ ] **S6. Wear OS + watchOS** - notifications and Decisions on both wrists
- [ ] **S7. Watch canvas** - native projection renderer on both wrists (design approved, Jay via bus 1322; S7a server projection carded, S7b/c gated on S6 shells)

S3 and S4 are genuinely parallel work: different toolchains, one shared web canvas. **S2 is the critical path** and should go first because it unblocks both platforms and needs no Mac.

## The one piece of new server surface

`GET /api/share/destinations`. The client must not hardcode the destination list, because what a user can share into depends on their projects, their agents, and their grants. It returns what *this device's token* may write to. Without it the picker goes stale the moment a project is renamed or an agent is archived, and it would happily offer destinations the user cannot actually write to.

## Design rules that will otherwise bite us

- **Share must survive a cold start.** The share sheet is often the first thing touched after a reboot; the extension cannot assume a warm session.
- **Large files upload in the background** with a notification, not a spinner. iOS share extensions are memory-capped and killed aggressively.
- **Offline shares queue and retry.** A phone on a train is the normal case, not the edge case.
- **Notifications must be actionable on both platforms.** A Decision the user cannot answer from the lock screen defeats the point of putting it on the phone.
- **A lost phone must be revocable.** `DELETE /api/devices/{id}` exists; the UI needs to surface it.

## Open questions

1. **Distribution.** A self-hosted OS whose companion app lives in Apple's store is a real philosophical tension. Android can side-load an APK; iOS effectively cannot. Worth deciding deliberately rather than by default.
2. **Off-LAN.** The Apple design settled on one shared Headscale tailnet with per-account users and a deny-by-default ACL. Confirm it holds for Android's VPN APIs.
3. **Naming.** "taOSc" is the working name; store listings need a human one.
