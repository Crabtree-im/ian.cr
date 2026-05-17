# Lego Stop Motion Suite

This folder contains a production-oriented scaffold for a two-part stop-motion system:

1. `iOSCaptureApp` - iPhone frame capture app
2. `macOSEditorApp` - macOS timeline editor and exporter
3. `Shared` - shared Swift package used by both apps

## Transfer Recommendation

Primary transfer method: **MultipeerConnectivity**.

Why this is the best fit for this project:

- Offline local transfer over Wi-Fi/Bluetooth.
- Predictable app-to-app flow for repeated capture sessions.
- No cloud account or external infrastructure required.
- Better automation and less user friction than AirDrop-only workflows.

Fallback options:

- AirDrop: easy backup path, but less automatable.
- iCloud container: useful for remote/asynchronous workflows, but slower and less deterministic on set.

## Integration Notes

Use the included XcodeGen configuration to generate a runnable multi-target project:

1. Install XcodeGen: `brew install xcodegen`
2. From this folder run: `./bootstrap_xcode.sh`
3. Open `XcodeGen/LegoStopMotionSuite.xcodeproj`
4. Set your development team for both targets.

Included permissions and capabilities are configured in:

- `XcodeGen/Plists/CaptureApp-Info.plist`
- `XcodeGen/Plists/EditorApp-Info.plist`
- `XcodeGen/Entitlements/EditorApp.entitlements`

## Additional Implemented Features

- iOS onion-skin overlay with adjustable opacity during capture.
- iOS AirDrop fallback via `ShareLink` for any prepared batch zip.
- macOS manual zip import (`Import Zip...`) for AirDrop-received files.

## Data Flow

1. iOS captures JPEG frames into app sandbox.
2. iOS writes `manifest.json` and zips batch folder.
3. iOS sends zip via MultipeerConnectivity to macOS app.
4. macOS imports zip into project folder.
5. macOS builds timeline at 12-15 FPS with equal frame durations.
6. macOS exports animation to `.mov`.
