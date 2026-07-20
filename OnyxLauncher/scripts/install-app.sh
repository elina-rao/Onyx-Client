#!/usr/bin/env bash
# Install the packed Onyx Launcher into /Applications (full replace).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC=""
for CANDIDATE in \
  "$ROOT/dist/mac-arm64/Onyx Launcher.app" \
  "$ROOT/dist/mac-arm64/OnyxLauncher.app" \
  "$ROOT/dist/mac/Onyx Launcher.app" \
  "$ROOT/dist/mac/OnyxLauncher.app" \
  "$ROOT/dist/mac-x64/Onyx Launcher.app" \
  "$ROOT/dist/mac-x64/OnyxLauncher.app"
do
  if [ -d "$CANDIDATE" ]; then
    SRC="$CANDIDATE"
    break
  fi
done

if [ -z "$SRC" ]; then
  echo "No packed app found. Run: npm run pack" >&2
  exit 1
fi

DEST="/Applications/Onyx Launcher.app"
echo "Installing: $SRC → $DEST"
rm -rf "$DEST"
cp -R "$SRC" "$DEST"
xattr -cr "$DEST" 2>/dev/null || true
find "$DEST" -name '._*' -delete 2>/dev/null || true
codesign --force --deep --sign - "$DEST" 2>/dev/null || true
echo "Done. Open with: open -a \"Onyx Launcher\""
