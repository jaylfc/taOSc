# taOSc

The taOS companion app for iOS/watchOS and Android/Wear OS.

taOSc puts a running [taOS](https://github.com/jaylfc/taOS) instance in your
pocket. It does exactly three things:

1. **Canvas** - the agent-driven taOS UI on your phone, and a native
   projection of it on your wrist.
2. **Actionable notifications** - agent Decisions you can answer (approve,
   deny, pick an option, quick-reply) from the phone and the watch, straight
   from the lock screen.
3. **Share to taOS** - the OS share sheet sends anything into taOS: the
   Library, a project's files, or an agent chat.

It is not a second taOS. It is the pocket surface onto the taOS you already
run.

## Architecture in one paragraph

The phone apps are thin native shells hosting the existing taOS web UI in a
system WebView (WKWebView on iOS, WebView on Android), dropping to native
only where the OS demands it: push, share, pairing, secure storage. The
watch apps are fully native (SwiftUI / Compose for Wear) because neither
watch OS has a usable WebView; they render notifications, Decisions, and a
constrained projection of canvas state. One web codebase, four thin shells.

## Connecting

Two login methods:

- **taOSgo** (paid cloud subscription): sign in and your instance is
  discovered and reachable off-LAN automatically.
- **LAN**: enter your instance URL directly. No cloud dependency.

Either way, first connect is an explicit grant: your taOS instance shows an
allow/deny prompt with a verification code before the device is paired.
Devices get scoped tokens, never full sessions, and can be revoked from taOS
at any time.

## Repository layout

- `docs/` - design documents and the slice-by-slice build plan
- `apple/` - iOS and watchOS apps (Swift, coming with slice S4)
- `android/` - Android and Wear OS apps (Kotlin, coming with slice S3)

Server-side surfaces taOSc depends on (device registry, push dispatch,
share destinations, canvas projection) live in the main
[taOS repository](https://github.com/jaylfc/taOS).

## Status

Early. The server foundation (device identity, scoped tokens, APNs) is
merged in taOS core; the share/push backend slices are in progress; the
client shells are next. See `docs/EPIC-taosc.md` for the live slice status.

## Building

- iOS/watchOS: Xcode with the iOS and watchOS SDKs; simulator builds need no
  signing identity.
- Android/Wear OS: builds on Linux with the standard Android toolchain.

## License

To be determined; see the taOS core repository for the project's licensing
approach.
