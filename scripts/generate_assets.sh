#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PLAN_PATH="${1:-$ROOT_DIR/assets-src/image/specs/phase2-asset-plan.yaml}"
OUT_DIR="${2:-$ROOT_DIR/assets-src/image/raw/generated}"
REPORT_PATH="${3:-$ROOT_DIR/assets-src/image/manifests/phase2-generation-report.jsonl}"
MODEL="${IMAGE_MODEL:-gemini-3.1-flash-image-preview}"

if [[ -z "${GEMINI_API_KEY:-}" ]]; then
  echo "GEMINI_API_KEY is required. Export GEMINI_API_KEY before running Gemini image generation." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
mkdir -p "$(dirname "$REPORT_PATH")"

python3 "$ROOT_DIR/scripts/generate_assets_gemini.py" \
  --plan "$PLAN_PATH" \
  --out-dir "$OUT_DIR" \
  --report "$REPORT_PATH" \
  --model "$MODEL" \
  --gemini-api-key "$GEMINI_API_KEY"

echo "Gemini image generation completed: $OUT_DIR"
