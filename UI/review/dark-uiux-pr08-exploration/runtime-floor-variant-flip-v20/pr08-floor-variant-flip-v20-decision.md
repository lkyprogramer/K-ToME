# PR-08 Floor Variant Flip V20 Decision

Date: 2026-05-28

## Verdict

`accepted-forward-only`

V20 keeps the existing PR-08 floor variant resource family and adds
deterministic `flipX` / `flipY` placement for tile-ground variant families.
This is a resource-use improvement rather than another overlay or alpha pass:
the runtime now uses the same authored floor cells in more orientations, so
repeated 32px stone motifs do not keep the same directional read across the
room.

It is not director-grade closure. The map-stage crop still reads too grid-first
compared with `UI/UI-demo-new.png`, and the floor/wall silhouette still needs a
stronger structural map pass or a final floor/wall resource sprint.

## Scope

- Client render-model placement only.
- No resource, manifest, schema, save, replay, profile, content-pack or
  gameplay-rule change.
- No golden baseline update.

## Evidence

- Full screen:
  `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-ui-demo-new-parity-1672x941.png`
- Map-stage crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-ui-demo-new-map-stage-crop.png`
- Right panel crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-ui-demo-new-right-panel-grid.png`
- Bottom deck crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-ui-demo-new-bottom-deck-no-command-hints.png`
- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-runtime-metrics.json`

## Metrics

- mean absolute RGB diff versus V19 map crop: `0.2950285588331129`
- max RGB diff versus V19 map crop: `47`
- changed pixels versus V19 map crop: `241670 / 2086920`
- changed ratio versus V19 map crop: `0.11580223487244361`

## Artifact Hashes

- comparison board:
  `1dedb883beb210c3bd4e928a8b1bc0ff8def980548d04c882d5ebb061ae05df7`
- runtime metrics:
  `6a9deea4319586b875368e59ac54888fe66a545fc5bcef79f5dcc89c3a8bfb7d`
- full screen:
  `b6aa5e63e7b83d760580c4a7fbf3e1e9d1747124fa7f56e5b9ae14fa28114b9f`
- map-stage crop:
  `0baad92e11a458e9115cdb0020aed189d17f4e659451232648e2c549065fd155`
- right panel crop:
  `2e57d934014ca9a07a31e6b9c438c1b8b5b4c4e96bf0cffd8b08399944f3fa5a`
- bottom deck crop:
  `87fc0c5f1a89615823ab088d266d510d9c777b432df95c615ec80c0575f9477b`

## Next Direction

Do not treat floor flipping as sufficient. The next map pass should either
change the room-scale hidden-stage / aperture structure more aggressively, or
return to a proper floor/wall resource sprint with new candidate evidence if
the blocker is reclassified as resource-owned.
