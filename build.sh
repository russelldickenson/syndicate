#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# Name of the signed APK this script produces. Change this to rename the output.
APK_NAME="syndicate.apk"

# Placeholder defaults for local builds. CI can supply the same values as
# environment variables, which take precedence over everything below.
: "${SYNDICATE_KEYSTORE_FILE:=$PWD/release.keystore}"   # path to your .jks or .keystore
: "${SYNDICATE_STORE_PASSWORD:=CHANGE_ME}"
: "${SYNDICATE_KEY_ALIAS:=CHANGE_ME}"
: "${SYNDICATE_KEY_PASSWORD:=CHANGE_ME}"

export SYNDICATE_KEYSTORE_FILE SYNDICATE_STORE_PASSWORD SYNDICATE_KEY_ALIAS SYNDICATE_KEY_PASSWORD

if [[ ! -f "$SYNDICATE_KEYSTORE_FILE" ]]; then
  echo "error: keystore not found at $SYNDICATE_KEYSTORE_FILE" >&2
  echo "Set SYNDICATE_KEYSTORE_FILE or replace the placeholder in build.sh." >&2
  exit 1
fi

./gradlew :app:assembleRelease

GRADLE_APK="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$GRADLE_APK" ]]; then
  echo "error: expected APK not found at $GRADLE_APK" >&2
  exit 1
fi

mkdir -p "$(dirname "$APK_NAME")"
cp "$GRADLE_APK" "$APK_NAME"

echo "Signed APK: $PWD/$APK_NAME"
