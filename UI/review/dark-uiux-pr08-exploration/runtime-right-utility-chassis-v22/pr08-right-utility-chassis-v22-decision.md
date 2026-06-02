# PR-08 Right Utility Chassis V22 Decision

Date: 2026-05-28

## Verdict

`accepted-forward-only`

V22 widens the equipment paper-doll stance and adds one shared forged chassis
behind the inscription, backpack and operation-hint stack. The right column now
reads less like isolated grids floating on black and more like one authored
utility workbench.

It is not director-grade closure. The right panel crop is materially denser and
more intentional than V21, but the full screen still needs stronger map,
icon/detail and bottom-deck polish before it can match the first-read quality
of `UI/UI-demo-new.png`.

## Scope

- Client demo-shell layout and renderer chrome only.
- Focused client renderer/layout tests only.
- No resource, manifest, schema, save, replay, profile, content-pack or
  gameplay-rule change.
- No golden baseline update.

## Evidence

- Full screen:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-ui-demo-new-parity-1672x941.png`
- Map-stage crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-ui-demo-new-map-stage-crop.png`
- Right panel crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-ui-demo-new-right-panel-grid.png`
- Bottom deck crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-ui-demo-new-bottom-deck-no-command-hints.png`
- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-runtime-metrics.json`

## Metrics

- mean absolute RGB diff versus V21 right-panel crop: `1.5578277545327754`
- max RGB diff versus V21 right-panel crop: `212`
- changed pixels versus V21 right-panel crop: `417041 / 1147200`
- changed ratio versus V21 right-panel crop: `0.3635294630404463`

## Artifact Hashes

- comparison board:
  `1995eb6995b0af0d7ec5c6df4b3bae3cc6c7cfabb68b73d91ee7556ea28a1119`
- runtime metrics:
  `bbeea82502e3038c25a24c3ee45aa594f02f62a86c991b8f0e4d10a9d6f68a26`
- full screen:
  `245e88f1fd5e6be5c6946fdf023b41078cedb960a348b29b6df34c769365cce1`
- map-stage crop:
  `0cfaf9942278db1fe921e2fce933dd3e7a5a476845d2bb687c907d7b2cf2f433`
- right panel crop:
  `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
- bottom deck crop:
  `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
- inventory page 1:
  `2dc2ac8cf03dba3693f5d5d6e12a68225a0debeac6e9cc6321f823b1417ddd63`
- inventory page 2:
  `230165e27de1426439671000c82b215cdf15ded3c30d6bd9db1ee32ae77a5b23`

## Golden Actual Hashes

From the retained expected PR-02-1 golden failure:

- `ui-demo-new-parity-1672x941=d371f83aa0e30a0520ee8aaf216318682cf64aa13c8e213cc71c4a989d6a8216`
- `ui-demo-new-parity-1280x800=548b5437a127ff90cb66c4d9edc6911d70a5f4de38344f5283f4ee0fcd77771e`
- `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
- `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
- `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
- `ui-demo-new-map-stage-crop=67daccb1e4b2122ed36d047c3b7d258d3fe869b62c377296439cf26ae7dac7b3`
- `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
- `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`

## Next Direction

Keep V22 because it improves right-column utility density with a narrow
client-only runtime change and test coverage. Do not keep iterating on subtle
right-panel rail opacity. The next high-leverage pass should either improve
bottom-deck chrome cohesion and icon/detail density, or return to the map with
a larger resource-backed readability change if the map remains the dominant
full-screen blocker.
