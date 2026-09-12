### Fixed
- Fixed 19 test compilation errors in `PairingServiceTest.kt` and `DecisionActionMapperTest.kt` so unit tests execute again
- Fixed wire-contract defect: `multi_select` now sends the tapped value as a one-element JSON array instead of all offered options or an empty array
- Fixed wire-contract defect: `add_note` now sends both `value` and `note` fields to prevent 422 validation errors
- Fixed `PairingService.updatePushToken` to use the injected `HttpClient` instead of a private method, making PATCH behavior testable on the JVM
