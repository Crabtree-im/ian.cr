# Xcode Project Generation

This directory contains an XcodeGen specification for a two-target workspace:

- `CaptureApp` (iOS)
- `EditorApp` (macOS)

## Generate Project

From `LegoStopMotionSuite` run:

```bash
./bootstrap_xcode.sh
```

Generated output:

- `XcodeGen/LegoStopMotionSuite.xcodeproj`

## Required Signing Setup

1. Open the generated project in Xcode.
2. Set your team for both targets.
3. If bundle identifiers conflict, update:
   - `dev.icrabtree.stopmotion.capture`
   - `dev.icrabtree.stopmotion.editor`

## Capabilities

`CaptureApp`:

- Camera permission via Info.plist
- Local Network permission via Info.plist + Bonjour service entry
- AirDrop fallback supported through iOS share sheet in the app UI

`EditorApp`:

- App Sandbox enabled
- User-selected file read/write
- Network client + server
- Local Network permission via Info.plist + Bonjour service entry
- Manual zip import path for AirDrop-received batches
