# PR-08 Bottom Console Cap Rails V23 Decision

Date: 2026-05-28

## Verdict

`accepted-forward-only`

V23 adds an overlaid top cap rail, lower cap rail and small seam clamps across
the hero, action and log panels after all three bottom deck panels have drawn.
The bottom HUD now reads more like one forged console assembly instead of three
adjacent card bottoms.

It is not director-grade closure. The bottom crop has a clearer shared chassis,
but the full-screen gap still includes map readability, icon/detail density and
overall material richness compared with `UI/UI-demo-new.png`.

## Scope

- Client demo-shell renderer chrome only.
- Focused client renderer canvas test only.
- No layout, resource, manifest, schema, save, replay, profile, content-pack or
  gameplay-rule change.
- No golden baseline update.

## Evidence

- Full screen:
  `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-ui-demo-new-parity-1672x941.png`
- Map-stage crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-ui-demo-new-map-stage-crop.png`
- Right panel crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-ui-demo-new-right-panel-grid.png`
- Bottom deck crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-ui-demo-new-bottom-deck-no-command-hints.png`
- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-runtime-metrics.json`

## Metrics

- mean absolute RGB diff versus V22 bottom-deck crop: `0.1832368722493519`
- max RGB diff versus V22 bottom-deck crop: `37`
- changed pixels versus V22 bottom-deck crop: `32176 / 663480`
- changed ratio versus V22 bottom-deck crop: `0.048495809971664555`

## Artifact Hashes

- comparison board:
  `8c2d7fd40a62ed4ab0960f8de6fec22f06d05c7eb2f5acd5bc2402a64d3bb757`
- runtime metrics:
  `73ccd28c362d63eb48f3457ff48a5639d51f25471d0d245eb5d08a6bcd7e1fe9`
- full screen:
  `702b31f53b862f25a733ac6c49d43d2224a8a7dc1280d9397a519cf2cbf24b91`
- map-stage crop:
  `0cfaf9942278db1fe921e2fce933dd3e7a5a476845d2bb687c907d7b2cf2f433`
- right panel crop:
  `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
- bottom deck crop:
  `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- inventory page 1:
  `2dc2ac8cf03dba3693f5d5d6e12a68225a0debeac6e9cc6321f823b1417ddd63`
- inventory page 2:
  `230165e27de1426439671000c82b215cdf15ded3c30d6bd9db1ee32ae77a5b23`

## Golden Actual Hashes

From the retained expected PR-02-1 golden failure:

- `ui-demo-new-parity-1672x941=e8a037f72d8d6fbe3a9df489af4d013619ea76ab73a3ae8d4ae3e5a11aae455a`
- `ui-demo-new-parity-1280x800=f38c99539fdc85f4f604205ca580aed7ab2c07b193f84185a85976955093336b`
- `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
- `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
- `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
- `ui-demo-new-map-stage-crop=67daccb1e4b2122ed36d047c3b7d258d3fe869b62c377296439cf26ae7dac7b3`
- `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
- `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`

## Next Direction

Keep V23 because it improves bottom-deck assembly cohesion with a narrow
client-only runtime change and test coverage. Do not keep layering subtle
bottom rail opacity tweaks. The next high-leverage pass should target visible
icon/detail richness in the bottom/action or right-column controls, or return
to a larger resource-backed map readability change if the map remains the
dominant full-screen blocker.
