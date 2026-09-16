### Added

- iOS APNs registration with per-type decision notification categories
  - `DECISION_APPROVE_DENY` category with `approve`, `reject`, `add_note` actions
  - `DECISION_FREE_TEXT` category with `quick_reply` text-input action
  - `DECISION_OPTIONS` category registered with no custom actions (tap-through to app)
- Push token hex encoding and PATCH to `/api/devices/{device_id}/push-token`
- Decision notification action handling with server answer POST
- URL validation rejecting foreign hosts
- App navigation to decisions list when `url` payload key is absent
- `aps-environment` entitlement and `NSUserNotificationsUsageDescription`
- Background Modes remote-notification capability