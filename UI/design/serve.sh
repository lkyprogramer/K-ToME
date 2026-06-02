#!/usr/bin/env bash
# Local static server for the design HTML previews.
# Reason: pages load *.jsx via Babel fetch(), which file:// blocks as CORS.
# Serve over http:// so same-origin fetch works.
set -euo pipefail

PORT="${1:-8000}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Open the directory listing so every *.html is reachable by filename.
URL="http://localhost:${PORT}/"

cd "$DIR"
echo "Serving $DIR at $URL"
echo "Available pages:"
for f in *.html; do
  [ -e "$f" ] || continue
  # URL-encode spaces so the printed links are clickable.
  echo "  ${URL}${f// /%20}"
done
# Best-effort auto-open on macOS.
( sleep 1; command -v open >/dev/null && open "$URL" ) >/dev/null 2>&1 &
exec python3 -m http.server "$PORT"
