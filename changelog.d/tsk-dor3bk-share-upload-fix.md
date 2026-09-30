### Added

- Implemented ShareRetryPolicy.kt with retry decision logic for background share uploads
- Created ShareUploadWorker.kt with WorkManager integration for offline-queued share uploads
- Modified ShareActivity.kt to enqueue WorkManager requests for each share item
- Added work-runtime-ktx 2.9.0 dependency for WorkManager support
- Created ShareRetryPolicyTest.kt with 7 comprehensive unit tests

The implementation provides offline-queued background share uploads with:
- Runtime token and URL reading (not stored in WorkManager input data)
- Retry logic with configurable attempt counts
- Proper file caching and cleanup
- Individual WorkManager request per share item
- Notification support for upload progress