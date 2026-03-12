#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BOOTSTRAP_ROOT="$ROOT_DIR/.bootstrap"
BOOTSTRAP_GRADLE_BIN_FILE="$BOOTSTRAP_ROOT/gradle-bin.path"
BOOTSTRAP_GRADLE_USER_HOME="$BOOTSTRAP_ROOT/gradle-user-home"

"$ROOT_DIR/scripts/bootstrap-deps.sh" "$@"

if [[ ! -f "$BOOTSTRAP_GRADLE_BIN_FILE" ]]; then
  echo "Bootstrapped Gradle metadata not found: $BOOTSTRAP_GRADLE_BIN_FILE" >&2
  exit 1
fi

BOOTSTRAP_GRADLE_BIN="$(<"$BOOTSTRAP_GRADLE_BIN_FILE")"

if [[ ! -x "$BOOTSTRAP_GRADLE_BIN" ]]; then
  echo "Bootstrapped Gradle binary is not executable: $BOOTSTRAP_GRADLE_BIN" >&2
  exit 1
fi

GRADLE_USER_HOME="$BOOTSTRAP_GRADLE_USER_HOME" \
  "$BOOTSTRAP_GRADLE_BIN" \
  --offline \
  --no-daemon \
  test \
  jacocoTestReport \
  :core:jacocoTestCoverageVerification
