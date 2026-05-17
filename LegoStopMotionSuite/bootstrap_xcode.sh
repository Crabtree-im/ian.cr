#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
XCODEGEN_DIR="$ROOT_DIR/XcodeGen"

if ! command -v xcodegen >/dev/null 2>&1; then
  echo "xcodegen is required. Install with: brew install xcodegen"
  exit 1
fi

pushd "$XCODEGEN_DIR" >/dev/null
xcodegen generate
popd >/dev/null

echo "Generated project: $XCODEGEN_DIR/LegoStopMotionSuite.xcodeproj"
