# PR-08 Ground No-Bleed Room Material V17 Decision

Verdict: accepted-forward only.

## What Changed

- Removed the special `tile_ground` draw bleed so generated ground tiles render at their semantic `32x32` footprint instead of being overdrawn across neighboring cells.
- Raised the single room-scale `tileset.ruins.room_breakup_01` compositor asset alpha from `0.64` to `0.78`.
- Kept the change inside `client` rendering and focused tests.
- Added no resource key, manifest row, schema/version field, content-pack contract, save/replay/profile field, or gameplay rule.

## Evidence

- Runtime archive:
  `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/runtime-v17-ground-bleed-0px-room-alpha-078/`
- Runtime comparison board:
  `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/pr08-ground-no-bleed-room-material-v17-runtime-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/pr08-ground-no-bleed-room-material-v17-runtime-metrics.json`

## Result

- V17 reduced the diagnostic seam contrast relative to V13:
  - vertical mean: `7.7746290403994065 -> 7.276342483666915`
  - horizontal mean: `10.21626493577502 -> 9.8988323008451`
- The runtime crop reads less like overdrawn tile noise and more like a stable dark-room floor field.
- It is still not director-grade closure: the map remains too grid-legible at first glance, especially in the central/right room.

## Decision

Keep V17 as the next accepted-forward baseline. The next iteration should not reintroduce ground bleed or source cropping for this blocker. Further map work should either reduce the remaining grid by changing the room-scale material/compositor authority, or move to right-panel/bottom-deck chrome once the map is no longer the highest-leverage first-screen issue.
