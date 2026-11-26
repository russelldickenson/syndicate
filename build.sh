#!/bin/zsh
# Builds a signed release APK of Syndicate.
#
# Signing settings use the same ANDROID_* names as the GitHub Actions workflows:
#   ANDROID_KEYSTORE_PATH     path to the shared PKCS12 keystore
#   ANDROID_KEYSTORE_PASSWORD passphrase for that keystore
#   ANDROID_KEY_ALIAS         optional: this repo's default is in gradle.properties
#
# Export them from your password manager before running, e.g.:
#   export ANDROID_KEYSTORE_PATH="$HOME/.android-signing/android-shared.p12"
#   export ANDROID_KEYSTORE_PASSWORD="$( ... )"
#   ./build.sh
#
# The output filename comes from apk.outputFileName in gradle.properties.

set -euo pipefail

ROOT="${0:A:h}"
cd "$ROOT"

: "${ANDROID_KEYSTORE_PATH:=$HOME/.android-signing/android-shared.p12}"
export ANDROID_KEYSTORE_PATH

if [[ -z "${ANDROID_KEYSTORE_PASSWORD:-}" ]]; then
  print -u2 "error: set ANDROID_KEYSTORE_PASSWORD (from your password manager)."
  exit 1
fi
export ANDROID_KEYSTORE_PASSWORD

# ANDROID_KEY_ALIAS is optional; gradle.properties supplies this app's alias.
if [[ -n "${ANDROID_KEY_ALIAS:-}" ]]; then
  export ANDROID_KEY_ALIAS
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
[[ -d "$ANDROID_HOME" ]] || { print -u2 "error: Android SDK not found at $ANDROID_HOME (set ANDROID_HOME)."; exit 1; }
[[ -s "$ANDROID_KEYSTORE_PATH" ]] || { print -u2 "error: keystore missing or empty at $ANDROID_KEYSTORE_PATH."; exit 1; }

print "Building release APK..."
./gradlew --console=plain -q assembleRelease

APK_NAME=$(sed -n 's/^apk\.outputFileName=//p' "$ROOT/gradle.properties" | head -1)
[[ -n "$APK_NAME" ]] || { print -u2 "error: apk.outputFileName is not set in $ROOT/gradle.properties."; exit 1; }
APK="$ROOT/app/build/outputs/apk/release/$APK_NAME"
[[ -s "$APK" ]] || { print -u2 "error: build finished but $APK was not produced."; exit 1; }

# Check the signature if the SDK has apksigner (newest build-tools first).
APKSIGNER=("$ANDROID_HOME"/build-tools/*/apksigner(N.On[1]))
if [[ -n "${APKSIGNER[1]:-}" ]]; then
  "${APKSIGNER[1]}" verify --verbose "$APK" 2>&1 | head -3
fi

print "\nSigned APK: $APK"

cp "$APK" "$ROOT/$APK_NAME"
