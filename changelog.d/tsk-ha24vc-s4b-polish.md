### Fixed

- Expanded pairing and taOSgo join endpoints to accept the full 2xx response range instead of only HTTP 200
- Fixed PairingGrantView to distinguish cancellation from unreachable errors, prevent duplicate onComplete calls, and properly propagate task cancellation in polling
- Fixed TaOSgoEntryView to show a loading state during tailnet join transition, cancel in-flight join tasks on retry, and reuse a single view model across taps
- Fixed TaOSgoJoinViewModel to isolate phase mutations to the main actor, add a reentrancy guard to join(), validate API response fields, and preserve the underlying error type
- Fixed derivedDataPath quoting in swift-build workflow to handle runner paths containing whitespace
