# PR-08 Map Stage Masonry V18 Decision

Verdict: rejected.

## What Changed

- Tested stronger `drawMapStageStoneTexture` alpha for hidden-stage masonry texture.
- Intended effect: make the large black map-stage field read as dark dungeon material rather than an empty rectangle.
- The code and focused test were reverted after runtime evidence review.

## Evidence

- Runtime archive:
  `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/runtime-v18-stage-masonry-texture/`
- Runtime comparison board:
  `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/pr08-map-stage-masonry-v18-runtime-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/pr08-map-stage-masonry-v18-runtime-metrics.json`

## Result

- Runtime diff was too small to justify a new renderer/test contract:
  - mean absolute RGB diff: `0.01954902653697329`
  - max RGB diff: `4`
  - dark-stage mean luma delta: `0.00482802381026584`
- Full screenshot review showed no meaningful improvement toward the reference image's hidden-room material depth.

## Decision

Do not keep stage texture alpha tuning as the next PR-08 cut. The hidden-stage gap needs a stronger structural technique than subtly raising existing masonry texture opacity.
