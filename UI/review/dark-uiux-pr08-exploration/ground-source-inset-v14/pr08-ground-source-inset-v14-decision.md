# PR-08 Ground Source Inset V14 Decision

Verdict: rejected.

## What Changed

- Tested a renderer-local source crop/inset path for `tile_ground` draws.
- Intended effect: remove dark edge pixels from the repeated ground source art before those pixels could become the room-scale lattice authority.
- The code and focused test were reverted after runtime evidence review.

## Evidence

- Runtime archive:
  `UI/review/dark-uiux-pr08-exploration/ground-source-inset-v14/runtime-v14-ground-source-inset/`
- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/ground-source-inset-v14/pr08-ground-source-inset-v14-runtime-comparison-board.png`
- Metrics:
  `UI/review/dark-uiux-pr08-exploration/ground-source-inset-v14/pr08-ground-source-inset-v14-runtime-metrics.json`

## Result

- V14 increased the diagnostic seam contrast instead of demoting it:
  - vertical contrast delta: `+0.3852503034532999`
  - horizontal contrast delta: `+0.17582894539610017`
- The pixel difference was real, but the first read still stayed grid-first and the crop made the tile body feel more mechanically sampled.

## Decision

Do not keep source-crop support for this PR-08 map blocker. The failure shows the visible lattice is not primarily a removable source-edge artifact; it is produced by repeated tile sampling/overdraw plus insufficient room-scale material authority.
