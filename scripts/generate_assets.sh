#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PLAN_PATH="${1:-$ROOT_DIR/assets-src/image/specs/phase2-asset-plan.yaml}"
OUT_DIR="${2:-$ROOT_DIR/assets-src/image/raw/generated}"
REPORT_PATH="${3:-$ROOT_DIR/assets-src/image/manifests/phase2-generation-report.jsonl}"
PROCESS_REPORT_PATH="${PROCESS_REPORT_PATH:-}"
MODEL="${IMAGE_MODEL:-gemini-3.1-flash-image-preview}"
SKIP_EXISTING="${GEMINI_SKIP_EXISTING:-0}"
CONCURRENCY="${GEMINI_CONCURRENCY:-1}"
PROCESS_AFTER_GENERATE="${PROCESS_ASSETS_AFTER_GENERATE:-1}"
PROCESSED_DIR="${PROCESSED_ASSET_DIR:-$ROOT_DIR/assets-src/image/processed}"
RUNTIME_DIR="${RUNTIME_ASSET_DIR:-$ROOT_DIR/client/src/main/resources}"

if [[ -z "$PROCESS_REPORT_PATH" ]]; then
  if [[ "$REPORT_PATH" == *generation-report.jsonl ]]; then
    PROCESS_REPORT_PATH="${REPORT_PATH%generation-report.jsonl}processing-report.jsonl"
  else
    PROCESS_REPORT_PATH="$ROOT_DIR/assets-src/image/manifests/phase2-processing-report.jsonl"
  fi
fi

if [[ -z "${GEMINI_API_KEY:-}" ]]; then
  echo "GEMINI_API_KEY is required. Export GEMINI_API_KEY before running Gemini image generation." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
mkdir -p "$(dirname "$REPORT_PATH")"
mkdir -p "$(dirname "$PROCESS_REPORT_PATH")"

cmd=(
  python3 "$ROOT_DIR/scripts/generate_assets_gemini.py"
  --plan "$PLAN_PATH"
  --out-dir "$OUT_DIR"
  --report "$REPORT_PATH"
  --model "$MODEL"
  --gemini-api-key "$GEMINI_API_KEY"
  --concurrency "$CONCURRENCY"
)

if [[ "$SKIP_EXISTING" == "1" ]]; then
  cmd+=(--skip-existing)
fi

"${cmd[@]}"

if [[ "$PROCESS_AFTER_GENERATE" == "1" ]]; then
  process_cmd=(
    python3 "$ROOT_DIR/scripts/process_assets.py"
    --plan "$PLAN_PATH"
    --raw-dir "$OUT_DIR"
    --processed-dir "$PROCESSED_DIR"
    --runtime-dir "$RUNTIME_DIR"
    --report "$PROCESS_REPORT_PATH"
  )

  if [[ "$SKIP_EXISTING" == "1" ]]; then
    process_cmd+=(--skip-existing)
  fi

  "${process_cmd[@]}"
fi

echo "Gemini image generation completed: $OUT_DIR"
