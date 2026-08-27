### Fixed

- Fixed force-unwrap crashes in FirstRunView by using safe unwraps and error presentation when hosts are empty or host addresses are invalid
- Fixed KeychainStore error swallowing in PairingGrantView by properly handling token save errors and transitioning to error state instead of silently advancing
- Fixed SettingsStore logout to clear serverURL even when keychain delete fails, ensuring the app doesn't remain half-logged in