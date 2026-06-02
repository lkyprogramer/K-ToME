# PR-08 Right Equipment Workbench V19 Decision

Date: 2026-05-28

## Verdict

`accepted-forward-only`

V19 improves the right panel equipment section by widening the slot stance and
strengthening the paper-doll rig backdrop. The top-right equipment area now
reads less like compact floating icons on a dark panel, and more like an
intentional forged workbench / armor scaffold.

It is not director-grade closure. The improvement is narrow, the right panel
still lacks the icon density and material richness of `UI/UI-demo-new.png`, and
the map-stage grid remains the dominant full-screen blocker.

## Scope

- Runtime/layout only.
- No resource, manifest, schema, save, replay, profile, content-pack or
  gameplay-rule change.
- No golden baseline update.

## Evidence

- Full screen:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-ui-demo-new-parity-1672x941.png`
- Right panel crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-ui-demo-new-right-panel-grid.png`
- Bottom deck crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-ui-demo-new-bottom-deck-no-command-hints.png`
- Map-stage crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-ui-demo-new-map-stage-crop.png`
- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-comparison-board.png`

## Artifact Hashes

- comparison board:
  `65d318dd51c23bb421f4bc1e8ce74e18222e1087e7697154e113617ea9415f4d`
- right panel crop:
  `0147692daa8cda571cee24c4e4c168d6d53e82cd9a073cad4eb67ec4a60cdb01`
- full screen:
  `75ee3ca04729310f5e8f84ec3d687bf320db49e4373766d96250e762dda5064c`
- bottom deck crop:
  `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
- map-stage crop:
  `ebb82b2e1dbc1017b622e7feda6f8885708e33356ce96a6f52ba7349911cd433`

## Next Direction

Continue right-panel work only if the next cut targets a clearly visible
inscription/backpack density or icon-quality gap. Do not continue subtle
right-panel alpha tuning.

For the overall director-grade target, the higher-leverage path is still a
structural map hidden-stage / room-scale compositor change, unless a new
full-screen comparison proves the right panel or bottom deck has become the
dominant first-read gap.
