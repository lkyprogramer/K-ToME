# PR-08 Hidden Aperture Masonry V21 Decision

Date: 2026-05-28

## Verdict

`accepted-forward-only`

V21 moves the hidden-stage aperture structure into the fog-veil phase, after
hidden fog has been drawn and before targeting/cursor overlays. The new
room-scale masonry shoulders and shelves are clipped outside the visible room,
so the playable center stays readable while the surrounding darkness reads more
like an authored carved aperture than a flat black stage.

It is not director-grade closure. The full map crop is less empty than V20, but
the screen still needs stronger material/chrome polish before it can match the
first-read quality of `UI/UI-demo-new.png`.

## Scope

- Client renderer compositor only.
- No resource, manifest, schema, save, replay, profile, content-pack or
  gameplay-rule change.
- No golden baseline update.

## Evidence

- Full screen:
  `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-ui-demo-new-parity-1672x941.png`
- Map-stage crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-ui-demo-new-map-stage-crop.png`
- Right panel crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-ui-demo-new-right-panel-grid.png`
- Bottom deck crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-ui-demo-new-bottom-deck-no-command-hints.png`
- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-runtime-metrics.json`

## Metrics

- mean absolute RGB diff versus V20 map crop: `0.24766130629508237`
- max RGB diff versus V20 map crop: `14`
- changed pixels versus V20 map crop: `199364 / 2086920`
- changed ratio versus V20 map crop: `0.09553025511279781`

## Artifact Hashes

- comparison board:
  `151f83c60d110d221c8a0a991dbc910706a23c91323582b5807a0cd842760811`
- runtime metrics:
  `26408bb4b1fa64fbcdcbc86e68d9bd04e8d2af5e42132d7a45051de836cc3248`
- full screen:
  `16798e19101b3b7c850dcec5f84ac970c31076d3dd0c531c033e8d6aa277b007`
- map-stage crop:
  `0cfaf9942278db1fe921e2fce933dd3e7a5a476845d2bb687c907d7b2cf2f433`
- right panel crop:
  `0147692daa8cda571cee24c4e4c168d6d53e82cd9a073cad4eb67ec4a60cdb01`
- bottom deck crop:
  `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`

## Golden Actual Hashes

From the retained expected PR-02-1 golden failure:

- `ui-demo-new-parity-1672x941=c8d29456c5db97976bf73e034daa0928ea926c977ec4538b1a9fc92439606108`
- `ui-demo-new-parity-1280x800=015b589f3f7b58333a3dce3d90167420a5fa9cafdf6775fb48dba3d096342b5a`
- `ui-demo-new-right-panel-grid=9d29e9f6aa2cb7d293df853397177e9d2d2a9a2a8613b2a615ad9de7c2c6ff23`
- `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
- `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
- `ui-demo-new-map-stage-crop=67daccb1e4b2122ed36d047c3b7d258d3fe869b62c377296439cf26ae7dac7b3`
- `ui-demo-new-inventory-page-1=56eda712404c7fe31bb0eee2b1865cb4d38bd56efbe1734171acee158b325158`
- `ui-demo-new-inventory-page-2=caa8797fc7ee7f553d4de372d5cdb7261ac9ade6ee9b83dcdf297cbcda4d0aac`

## Next Direction

Keep V21 because it fixes a real compositor-layering problem and gives the map
a stronger dark-stage silhouette. Do not continue with hidden-stage alpha-only
tuning. The next high-leverage pass should either move to right-panel/bottom
chrome density, or make a larger map readability change with new authored
resources if the map remains the dominant blocker after review.
