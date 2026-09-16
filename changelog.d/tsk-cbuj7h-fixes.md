### Fixed
- Fixed compile error in `MainActivity.kt` where `Arrangement.Center()` was called as a function instead of used as a property
- Fixed compile error in `UnifiedPushRegistrar.kt` where `Messenger.binder` was used as an extra parameter to `Intent.putExtra`. Changed to use the `Messenger` object directly as it's `Parcelable`
- Fixed wire-contract defect: `updatePushToken` now correctly uses PATCH HTTP method to match server API spec
- Fixed wire-contract defect: `multi_select` decisions now serialize selected options as a JSON array instead of a single string
- Fixed wire-contract defect: `DecisionPayloadParser` now reads `actions` from the root level of payload and `image` from the root level where server sets them
- Enhanced `FakeHttpClient` in tests to be method-aware for proper HTTP method testing
- Added comprehensive tests for the above fixes including multi-select serialization, explicit actions handling, root-level image, and PATCH method verification