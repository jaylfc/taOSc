# Build-test gate preconditions

## Shared Xcode scheme

Any target that CI builds needs its scheme committed under
`apple/taOSc.xcodeproj/xcshareddata/xcschemes/`. Xcode does not share schemes
by default and does not synthesise one for a fresh clone with no `xcuserdata`;
`xcodebuild -scheme` then fails with `does not contain a scheme named`. The
existing `taOSc.xcscheme` in that directory is the worked example.

## PR gates

A PR must satisfy:

- Secret scan and lint (`tsk-5w6eaf`)
- Swift build and test (`tsk-pispe7`)

Green status from CodeRabbit, Gitar, and Kilo Code Review on a PR is diff
review only. It is not compile verification.
