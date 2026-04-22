#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/capture-macos-app-window.sh --out <png> [--bundle-id <id>] [--app-name <name>] [--title <text>] [--pid <pid>]
  scripts/capture-macos-app-window.sh --list

Captures a single macOS application window by resolving a CoreGraphics window id
first, then calling screencapture with -l <window-id>. This is intentionally not
a desktop screenshot helper. By default, the captured PNG is downsampled to an
evidence-friendly size to keep white-box artifacts readable without storing the
full Retina backing buffer.

Options:
  --out <png>          Output PNG path.
  --metadata <path>   Metadata sidecar path. Defaults to <png>.metadata.txt.
  --bundle-id <id>    Bundle id used to resolve the target process pid.
  --app-name <name>   Owner or window-name substring fallback, e.g. K-ToME.
  --title <text>      Window title substring.
  --pid <pid>         Explicit process id. Overrides bundle-id resolution.
  --raw               Preserve the raw screencapture PNG without resizing.
  --truecolor         Keep full-color PNG when processing. Default uses PNG8.
  --max-width <px>    Max output width for processed captures. Default: 1600.
  --max-height <px>   Max output height for processed captures. Default: 1200.
  --list              List visible windows and exit.
  -h, --help          Show this help.

Recommended K-ToME usage:
  scripts/capture-macos-app-window.sh \
    --bundle-id com.ktome.client \
    --app-name K-ToME \
    --out build/whitebox/<task>/evidence/<step>.png
USAGE
}

APP_NAME=""
BUNDLE_ID=""
TITLE_CONTAINS=""
TARGET_PID=""
OUT=""
METADATA=""
LIST_ONLY=0
RAW_OUTPUT=0
TRUECOLOR_OUTPUT=0
MAX_WIDTH=1600
MAX_HEIGHT=1200

while [[ $# -gt 0 ]]; do
  case "$1" in
    --app-name)
      APP_NAME="${2:-}"
      shift 2
      ;;
    --bundle-id)
      BUNDLE_ID="${2:-}"
      shift 2
      ;;
    --title)
      TITLE_CONTAINS="${2:-}"
      shift 2
      ;;
    --pid)
      TARGET_PID="${2:-}"
      shift 2
      ;;
    --out)
      OUT="${2:-}"
      shift 2
      ;;
    --metadata)
      METADATA="${2:-}"
      shift 2
      ;;
    --raw)
      RAW_OUTPUT=1
      shift
      ;;
    --truecolor)
      TRUECOLOR_OUTPUT=1
      shift
      ;;
    --max-width)
      MAX_WIDTH="${2:-}"
      shift 2
      ;;
    --max-height)
      MAX_HEIGHT="${2:-}"
      shift 2
      ;;
    --list)
      LIST_ONLY=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ "$LIST_ONLY" -eq 0 && -z "$OUT" ]]; then
  echo "--out is required unless --list is used" >&2
  usage >&2
  exit 2
fi

if [[ "$LIST_ONLY" -eq 0 && -z "$TARGET_PID" && -z "$BUNDLE_ID" && -z "$APP_NAME" && -z "$TITLE_CONTAINS" ]]; then
  echo "At least one of --pid, --bundle-id, --app-name, or --title is required" >&2
  usage >&2
  exit 2
fi

if [[ -n "$TARGET_PID" && ! "$TARGET_PID" =~ ^[0-9]+$ ]]; then
  echo "--pid must be numeric: $TARGET_PID" >&2
  exit 2
fi

if [[ ! "$MAX_WIDTH" =~ ^[1-9][0-9]*$ ]]; then
  echo "--max-width must be a positive integer: $MAX_WIDTH" >&2
  exit 2
fi

if [[ ! "$MAX_HEIGHT" =~ ^[1-9][0-9]*$ ]]; then
  echo "--max-height must be a positive integer: $MAX_HEIGHT" >&2
  exit 2
fi

HELPER_ROOT="${KTOME_WINDOW_CAPTURE_HELPER_DIR:-${TMPDIR:-/tmp}/ktome-window-capture}"
HELPER_SRC="$HELPER_ROOT/list-macos-windows.m"
HELPER_BIN="$HELPER_ROOT/list-macos-windows"

build_helper() {
  mkdir -p "$HELPER_ROOT"
  cat > "$HELPER_SRC" <<'OBJC'
#import <Foundation/Foundation.h>
#import <CoreGraphics/CoreGraphics.h>

static NSString *sanitize(NSString *value) {
    if (value == nil) {
        return @"";
    }

    NSMutableString *copy = [value mutableCopy];
    [copy replaceOccurrencesOfString:@"\t" withString:@" " options:0 range:NSMakeRange(0, copy.length)];
    [copy replaceOccurrencesOfString:@"\n" withString:@" " options:0 range:NSMakeRange(0, copy.length)];
    [copy replaceOccurrencesOfString:@"\r" withString:@" " options:0 range:NSMakeRange(0, copy.length)];
    return copy;
}

int main(void) {
    @autoreleasepool {
        CFArrayRef windowInfoRef = CGWindowListCopyWindowInfo(
            kCGWindowListOptionOnScreenOnly | kCGWindowListExcludeDesktopElements,
            kCGNullWindowID
        );

        if (windowInfoRef == NULL) {
            return 1;
        }

        NSArray *windows = CFBridgingRelease(windowInfoRef);

        for (NSDictionary *info in windows) {
            NSNumber *windowId = [info objectForKey:(id)kCGWindowNumber];
            NSNumber *pid = [info objectForKey:(id)kCGWindowOwnerPID];
            NSString *owner = sanitize([info objectForKey:(id)kCGWindowOwnerName]);
            NSString *name = sanitize([info objectForKey:(id)kCGWindowName]);
            NSNumber *layer = [info objectForKey:(id)kCGWindowLayer];
            NSNumber *alpha = [info objectForKey:(id)kCGWindowAlpha];
            NSDictionary *boundsDictionary = [info objectForKey:(id)kCGWindowBounds];

            CGRect bounds = CGRectZero;
            if (boundsDictionary != nil) {
                CGRectMakeWithDictionaryRepresentation((__bridge CFDictionaryRef)boundsDictionary, &bounds);
            }

            printf(
                "%llu\t%d\t%s\t%s\t%.0f\t%.0f\t%.0f\t%.0f\t%d\t%.3f\n",
                [windowId unsignedLongLongValue],
                [pid intValue],
                [owner UTF8String],
                [name UTF8String],
                bounds.origin.x,
                bounds.origin.y,
                bounds.size.width,
                bounds.size.height,
                [layer intValue],
                [alpha doubleValue]
            );
        }
    }

    return 0;
}
OBJC

  /usr/bin/clang -fobjc-arc -framework Foundation -framework CoreGraphics "$HELPER_SRC" -o "$HELPER_BIN"
}

if [[ ! -x "$HELPER_BIN" || "$0" -nt "$HELPER_BIN" ]]; then
  build_helper
fi

resolve_bundle_pid() {
  local bundle_id="$1"

  /usr/bin/osascript - "$bundle_id" <<'APPLESCRIPT' 2>/dev/null || true
on run argv
  set targetBundleId to item 1 of argv
  tell application "System Events"
    set matches to every process whose bundle identifier is targetBundleId
    if (count of matches) is 0 then
      return ""
    end if
    return unix id of item 1 of matches
  end tell
end run
APPLESCRIPT
}

WINDOWS="$("$HELPER_BIN")"

if [[ "$LIST_ONLY" -eq 1 ]]; then
  printf 'window_id\tpid\towner\tname\tx\ty\twidth\theight\tlayer\talpha\n'
  printf '%s\n' "$WINDOWS"
  exit 0
fi

if [[ -z "$TARGET_PID" && -n "$BUNDLE_ID" ]]; then
  TARGET_PID="$(resolve_bundle_pid "$BUNDLE_ID" | tr -d '[:space:]')"
fi

MATCH="$(
  printf '%s\n' "$WINDOWS" | awk -F '\t' \
    -v target_pid="$TARGET_PID" \
    -v app_name="$APP_NAME" \
    -v title_contains="$TITLE_CONTAINS" '
function lower(value) {
  return tolower(value)
}

BEGIN {
  best_area = -1
  best = ""
  app_lower = lower(app_name)
  title_lower = lower(title_contains)
}

NF >= 10 {
  window_id = $1
  pid = $2
  owner = $3
  name = $4
  width = $7 + 0
  height = $8 + 0
  layer = $9 + 0
  alpha = $10 + 0

  if (width <= 0 || height <= 0 || layer != 0 || alpha <= 0) {
    next
  }

  if (target_pid != "" && pid != target_pid) {
    next
  }

  owner_lower = lower(owner)
  name_lower = lower(name)

  if (target_pid == "" && app_lower != "" && index(owner_lower, app_lower) == 0 && index(name_lower, app_lower) == 0) {
    next
  }

  if (title_lower != "" && index(name_lower, title_lower) == 0) {
    next
  }

  area = width * height
  if (area > best_area) {
    best_area = area
    best = $0
  }
}

END {
  if (best != "") {
    print best
  }
}
'
)"

if [[ -z "$MATCH" ]]; then
  echo "No matching visible window found." >&2
  echo "bundle_id=${BUNDLE_ID:-<unset>} app_name=${APP_NAME:-<unset>} title=${TITLE_CONTAINS:-<unset>} pid=${TARGET_PID:-<unset>}" >&2
  echo >&2
  echo "Visible windows:" >&2
  printf 'window_id\tpid\towner\tname\tx\ty\twidth\theight\tlayer\talpha\n' >&2
  printf '%s\n' "$WINDOWS" >&2
  exit 1
fi

IFS=$'\t' read -r WINDOW_ID WINDOW_PID WINDOW_OWNER WINDOW_NAME WINDOW_X WINDOW_Y WINDOW_WIDTH WINDOW_HEIGHT WINDOW_LAYER WINDOW_ALPHA <<< "$MATCH"

mkdir -p "$(dirname "$OUT")"

RAW_CAPTURE="$OUT"
if [[ "$RAW_OUTPUT" -eq 0 ]]; then
  RAW_CAPTURE="$(/usr/bin/mktemp "${TMPDIR:-/tmp}/ktome-window-capture.XXXXXX.png")"
fi

/usr/sbin/screencapture -x -l "$WINDOW_ID" "$RAW_CAPTURE"

if [[ ! -s "$RAW_CAPTURE" ]]; then
  echo "Window capture produced an empty file: $RAW_CAPTURE" >&2
  exit 1
fi

if ! /usr/bin/file "$RAW_CAPTURE" | grep -q 'PNG image data'; then
  echo "Window capture did not produce a PNG image: $RAW_CAPTURE" >&2
  /usr/bin/file "$RAW_CAPTURE" >&2 || true
  exit 1
fi

process_capture() {
  local raw_file="$1"
  local output_file="$2"

  if [[ "$RAW_OUTPUT" -eq 1 ]]; then
    return
  fi

  if command -v magick >/dev/null 2>&1; then
    if [[ "$TRUECOLOR_OUTPUT" -eq 1 ]]; then
      magick "$raw_file" \
        -auto-orient \
        -resize "${MAX_WIDTH}x${MAX_HEIGHT}>" \
        -strip \
        -define png:compression-level=9 \
        -define png:compression-strategy=1 \
        "$output_file"
    else
      magick "$raw_file" \
        -auto-orient \
        -resize "${MAX_WIDTH}x${MAX_HEIGHT}>" \
        -strip \
        -define png:compression-level=9 \
        -define png:compression-strategy=1 \
        PNG8:"$output_file"
    fi
    return
  fi

  if command -v /usr/bin/sips >/dev/null 2>&1; then
    local max_dimension="$MAX_WIDTH"
    if [[ "$MAX_HEIGHT" -gt "$MAX_WIDTH" ]]; then
      max_dimension="$MAX_HEIGHT"
    fi
    /usr/bin/sips -Z "$max_dimension" "$raw_file" --out "$output_file" >/dev/null
    return
  fi

  cp "$raw_file" "$output_file"
}

process_capture "$RAW_CAPTURE" "$OUT"

if [[ "$RAW_OUTPUT" -eq 0 ]]; then
  rm -f "$RAW_CAPTURE"
fi

if [[ ! -s "$OUT" ]]; then
  echo "Processed window capture produced an empty file: $OUT" >&2
  exit 1
fi

if ! /usr/bin/file "$OUT" | grep -q 'PNG image data'; then
  echo "Processed window capture did not produce a PNG image: $OUT" >&2
  /usr/bin/file "$OUT" >&2 || true
  exit 1
fi

METADATA="${METADATA:-$OUT.metadata.txt}"
SHA_FILE="$OUT.sha256"
SHA256="$(/usr/bin/shasum -a 256 "$OUT" | awk '{print $1}')"
FILE_INFO="$(/usr/bin/file "$OUT")"
OUTPUT_BYTES="$(wc -c < "$OUT" | tr -d '[:space:]')"

PIXEL_WIDTH=""
PIXEL_HEIGHT=""
if command -v /usr/bin/sips >/dev/null 2>&1; then
  PIXEL_WIDTH="$(/usr/bin/sips -g pixelWidth "$OUT" 2>/dev/null | awk '/pixelWidth:/ {print $2}' || true)"
  PIXEL_HEIGHT="$(/usr/bin/sips -g pixelHeight "$OUT" 2>/dev/null | awk '/pixelHeight:/ {print $2}' || true)"
fi

cat > "$METADATA" <<EOF
captured_at=$(/bin/date -u +"%Y-%m-%dT%H:%M:%SZ")
capture_mode=macos-window-id
capture_processing=$(if [[ "$RAW_OUTPUT" -eq 1 ]]; then printf 'raw'; elif [[ "$TRUECOLOR_OUTPUT" -eq 1 ]]; then printf 'resized-truecolor-png'; else printf 'resized-png8'; fi)
max_width=${MAX_WIDTH}
max_height=${MAX_HEIGHT}
bundle_id=${BUNDLE_ID}
app_name=${APP_NAME}
title_contains=${TITLE_CONTAINS}
target_pid=${TARGET_PID}
window_id=${WINDOW_ID}
window_pid=${WINDOW_PID}
window_owner=${WINDOW_OWNER}
window_name=${WINDOW_NAME}
window_bounds=${WINDOW_X},${WINDOW_Y},${WINDOW_WIDTH},${WINDOW_HEIGHT}
window_layer=${WINDOW_LAYER}
window_alpha=${WINDOW_ALPHA}
output=${OUT}
metadata=${METADATA}
sha256=${SHA256}
output_bytes=${OUTPUT_BYTES}
pixel_width=${PIXEL_WIDTH}
pixel_height=${PIXEL_HEIGHT}
file_info=${FILE_INFO}
EOF

printf '%s  %s\n' "$SHA256" "$(basename "$OUT")" > "$SHA_FILE"

printf 'Captured window %s (%s, pid %s) -> %s\n' "$WINDOW_ID" "$WINDOW_OWNER" "$WINDOW_PID" "$OUT"
printf 'Metadata: %s\n' "$METADATA"
