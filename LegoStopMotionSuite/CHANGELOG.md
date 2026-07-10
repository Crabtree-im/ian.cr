# Changelog

All notable project updates are tracked here.

## 2026-05-23 - Lego stop motion building (`29cad57`)

### Added
- iOS capture flow: `New Take` reset action.
- iOS upload gating and clearer upload/retry status behavior.
- macOS receiver lifecycle controls (start/stop receiver).
- macOS playback safety guard (disable play when no frames are loaded).
- `EditorAppTests` Info.plist at `XcodeGen/Plists/EditorAppTests-Info.plist`.
- ZIPFoundation dependency in shared package for cross-platform zip/unzip support.

### Changed
- Shared archive utility switched to ZIPFoundation-backed zip/unzip implementation for iOS/macOS compatibility.
- iOS transfer service updated for iOS-safe peer naming and optional transfer progress observation.
- README updated with current status and repeatable progress-inspection commands.

### Fixed
- XcodeGen test target configuration so `EditorAppTests` builds and runs.
- CaptureApp iOS compile blockers in shared archive and transfer service code paths.
- iOS simulator destination availability after simulator runtime installation.

### Verified
- `xcodebuild build -project XcodeGen/LegoStopMotionSuite.xcodeproj -scheme CaptureApp -destination 'generic/platform=iOS Simulator'` exits `0`.
- `xcodebuild test -project XcodeGen/LegoStopMotionSuite.xcodeproj -scheme EditorApp -destination 'platform=macOS,arch=arm64'` exits `0`.

### Remaining
- On-device signing is still pending local Apple code-signing identities/team setup.

## 2026-05-17 - lego stop motion building plan (`d25be88`)

### Added
- Initial production-oriented scaffold for:
  - `iOSCaptureApp`
  - `macOSEditorApp`
  - `Shared`
- Architecture and workflow documentation for local transfer, import, timeline, and export.
