### Fixed

- Fixed android-build CI gate by overriding the delisted `tools` package in `setup-android@v3` with `packages: platform-tools`, restoring Gradle execution repo-wide
