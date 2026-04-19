#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PLAN_PATH="$ROOT_DIR/assets-src/image/specs/macos-app-icon-plan.yaml"
RAW_DIR="$ROOT_DIR/assets-src/image/raw/generated"
PROCESSED_DIR="$ROOT_DIR/assets-src/image/processed"
RUNTIME_DIR="$ROOT_DIR/client/src"
REPORT_PATH="$ROOT_DIR/assets-src/image/manifests/macos-app-icon-generation-report.jsonl"
PROCESS_REPORT_PATH="$ROOT_DIR/assets-src/image/manifests/macos-app-icon-processing-report.jsonl"

PROCESS_REPORT_PATH="$PROCESS_REPORT_PATH" \
PROCESSED_ASSET_DIR="$PROCESSED_DIR" \
RUNTIME_ASSET_DIR="$RUNTIME_DIR" \
"$ROOT_DIR/scripts/generate_assets.sh" "$PLAN_PATH" "$RAW_DIR" "$REPORT_PATH"
