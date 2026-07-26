# taOSc: the taOS companion app (iOS + watchOS, Android + Wear OS)

Date: 2026-07-26. Status: proposed.

Supersedes the platform scope of `docs/superpowers/specs/2026-07-06-taos-apple-client-design.md` while **keeping its architecture**. That doc's core decisions were right and are reused verbatim; this one generalises them to a second platform and adds a capability it never covered.

## 1. What taOSc is

One companion product, two platform implementations. Its job is exactly three things:

1. **A canvas** - the agent-driven taOS UI on the phone.
2. **A notification and Decisions target** - agent notifications that can be *acted on* (approve, deny, pick an option, quick-reply) from phone **and wrist**.
3. **Share to taOS** - the OS share sheet sends anything into taOS, with a destination picker.

Not a second taOS. It is the pocket surface onto the taOS you already run.

## 2. What already exists (verified 2026-07-26, do not rebuild)

Slice 1 of the Apple client is **merged** (#1671) and, importantly, it is not Apple-specific:

| Piece | State | Note |
|---|---|---|
| `device_store.py` | built | Schema has a generic `platform TEXT` column, so **Android devices need no migration** |
| `/api/devices/register`, list, `PATCH .../push-token`, delete | built | Platform-agnostic device lifecycle |
| `device_auth.py` scoped device tokens | built | Per-device auth already modelled |
| `push/apns.py` | built | **Apple only.** No FCM. |
| `clients/` directory | **does not exist** | No shell on either platform yet |

So the expensive, easy-to-get-wrong part (device identity + scoped tokens) generalises for free. The gaps are push for Android, the two shells, and share.

Share destinations also already exist server-side:
- Library: `POST /api/library/ingest` (multipart `file`, or form `url`)
- Project files: `POST /api/projects/{slug}/files/upload` (multipart)
- Agent chat: the existing chat message path

## 3. Architecture (inherited, and it generalises)

**Phone = native shell + web canvas.** A thin native app hosts the existing taOS web UI in a system WebView, dropping to native only where the OS demands it (push, share, sensors, secure storage). This was the Apple design's central call and it is the reason a second platform is affordable at all: `WKWebView` on iOS and `WebView` on Android host the *same* web UI, so the canvas is not written twice.

**Watch = fully native.** Neither watchOS nor Wear OS has a usable WebView for this, so both watch apps are native (SwiftUI / Compose for Wear) and scoped deliberately narrow: notifications and Decisions only, never the canvas.

The practical consequence: taOSc is roughly *one* UI codebase (the existing web app) plus four thin native shells, not four apps.

## 4. Share to taOS (net-new, the interesting part)

Jay's call: **ask each time.** The share sheet offers a destination rather than assuming one.

**Entry points**
- Android: `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intent filters (text, url, image, file) into a picker activity.
- iOS: a Share Extension with the same picker UI.

**Destinations (v1):** the Library, a project's Files, or an agent chat.

**The one piece of new server surface: `GET /api/share/destinations`.** The client must not hardcode a destination list, because what a user can share into depends on their projects, their agents, and their grants. The endpoint returns what *this* device's token may write to, and the picker renders it. Without this the picker goes stale the moment a project is renamed or an agent is archived.

**Send:** the picker POSTs to the destination's existing endpoint. No new ingest paths.

**Design rules**
- **Share must survive a cold start.** The share sheet is often the first thing a user touches after a reboot; the extension cannot assume a warm session.
- **Large files upload in the background**, with a visible notification, not a spinner the user must watch. iOS share extensions in particular are memory-capped and killed aggressively.
- **Offline shares queue** and retry rather than failing. A phone on a train is the normal case, not the edge case.

## 5. Push

APNs is built. **FCM is the gap** for Android and Wear. It should be a sibling module to `push/apns.py` behind one interface, so the notification layer does not learn about platforms: the device row already carries `platform`, so dispatch is a lookup, not a branch scattered through the code.

Both platforms must render *actionable* notifications, since a Decision the user cannot answer from the lock screen defeats the point.

## 6. Watch is core scope, not a follow-up

Jay's call, and it changes sequencing from the Apple design (which had watchOS last).

Both watch apps do the same job: receive an agent notification, show enough context to judge, and answer it - approve, deny, pick an option, quick-reply. The wrist is where the human-in-the-loop latency actually gets paid down.

Practical constraint that shapes the slices: a watch app **requires its phone app to exist first** on both platforms. So watch cannot be first, but it is v1, not v2.

## 7. Build sequence

Each slice is independently shippable. Server slices are platform-neutral and unblock both.

- **S1. Server foundation** - DONE (#1671).
- **S2. FCM push + share destinations** - `push/fcm.py` behind the existing push interface, and `GET /api/share/destinations`. Pure backend, no Mac needed, unblocks Android *and* the iOS share extension.
- **S3. Android phone shell** - WebView canvas, pairing, FCM, actionable notifications.
- **S4. iOS phone shell** - the Apple design's Slice 2, unchanged.
- **S5. Share targets** - Android intent filters + iOS Share Extension, both onto S2's picker.
- **S6. Wear OS + watchOS** - notifications and Decisions on both wrists.

S3 and S4 are genuinely parallel: different toolchains, different people, one shared web canvas.

## 8. Repository layout

`clients/apple/` (per the existing design) and `clients/android/`, in the main repo. Same reasoning as before: one source of truth, and the clients track the API they depend on.

Note the existing design assumed *"builds run locally on the developer's Mac"*. That assumption broke when the lead machine moved to Linux. Jay confirms Mac access via borrow or CI, so it holds again, but it should be written down as a real dependency rather than an aside: **iOS and watchOS cannot be built or signed without a Mac and an Apple Developer account.** Android builds on Linux.

## 9. Security and consent

- Device tokens are **scoped**, already modelled in `device_auth.py`. A phone is not an admin session.
- Share respects existing grants. `GET /api/share/destinations` returns only what the device may write, so the picker cannot offer a project the user has no access to.
- A lost phone must be revocable from taOS: `DELETE /api/devices/{id}` exists; the UI needs to surface it.
- Sensor capabilities stay as the Apple design specified (pull-based, consent-gated). Not expanded here.

## 10. Open questions

1. **Distribution.** TestFlight and Play internal testing are fine for Jay, but a self-hosted OS whose companion app lives in Apple's store is a philosophical tension worth deciding deliberately. Android can side-load an APK; iOS effectively cannot.
2. **Off-LAN.** The Apple design settled on one shared Headscale tailnet with per-account users and a deny-by-default ACL. taOSc inherits it; worth confirming it holds for Android's VPN APIs.
3. **Naming.** "taOSc" is the working name. Store listings need a human name.
