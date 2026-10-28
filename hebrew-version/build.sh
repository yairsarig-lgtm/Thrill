#!/usr/bin/env bash
# Builds the Hebrew app with the main build script: ThrillHebrew.apk (sideload) and ThrillHebrew.aab (Play).
# Run ./translate.py first whenever the main app has changed.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
exec "$HERE/../build.sh" "$HERE" ThrillHebrew
