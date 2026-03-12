#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

"$ROOT_DIR/scripts/bootstrap-deps.sh" "$@"
"$ROOT_DIR/gradlew" --offline test jacocoTestReport :core:jacocoTestCoverageVerification
