# PR-08 Room Art Plate Source-Cohesion S Decision

## Scope

Same-key `ui.map_stage.ruins.room_plate.pr08_demo` resource update plus a
client-only explored-fog presentation fix.

## Evidence

- `ui_map_stage_ruins_room_plate_pr08_demo.png`
- `ui-demo-new-map-stage-crop.png`
- `evidence-index.tsv`
- `../runtime-q-vs-s-surface-cohesion-board.png`

## Decision

Candidate S is accepted forward as the current same-key room plate resource.
It demotes Q's right-combat baked grid with stronger non-cell-aligned material
smoke, while preserving room topology and gameplay marker readability.

The runtime connected explored-fog blanket is also accepted forward. It removes
row/column tactical-run splitting for irregular explored pockets without
changing visibility authority.

## Hashes

```text
ui_map_stage_ruins_room_plate_pr08_demo.png=d0c3c2ea28c9493be4ab0d0acb441c92a5ce1564a8e081153ecf21697dca148f
ui-demo-new-map-stage-crop=48bc2d9b9c8f7e001c52d7f402b094efde495d6d13dbc76b138e94e9d6b745ed
```

## Verdict

D3 map-stage closure is still rejected. S improves Q, but the crop still has
visible tactical read-grid weight and should not be claimed as final
`UI/UI-demo-new.png` quality.
