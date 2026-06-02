# PR-08 Authored Room-Scale Decal V08 Decision

Date: 2026-05-28

## Objective

Test whether the post-W05i map-stage blocker can be solved by an authored
room-scale decal/AO layer, or whether the blocker should be reclassified before
another production mutation.

Source runtime crop:
`UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/runtime-w05i-wall-edge-unify/ui-demo-new-map-stage-crop.png`

Reference crop:
`UI/review/dark-uiux-pr08-exploration/authored-room-scale-decal-v08/pr08-room-scale-decal-v08-reference-map-crop-from-ui-demo-new.png`

## Evidence

Generated exploration evidence:

1. `pr08-room-scale-decal-v08-comparison-board.png`
2. `pr08-room-scale-decal-v08-degrid-comparison-board.png`
3. `pr08-room-scale-decal-v08-dark-lattice-mask.png`
4. `pr08-room-scale-decal-v08-metrics.json`
5. `pr08-room-scale-decal-v08-degrid-metrics.json`

Measured edge scores:

| Candidate | Mean edge | P90 edge | Verdict |
| --- | ---: | ---: | --- |
| W05i baseline | 6.992 | 23 | current source evidence |
| A AO collar | 5.274 | 16 | rejected |
| B broken slab | 5.615 | 17 | rejected |
| C enclosure light | 4.741 | 14 | rejected |
| D degrid only | 6.238 | 20 | accepted as diagnostic only |
| E degrid + room AO | 5.456 | 17 | rejected |
| F contact enclosure | 5.588 | 17 | rejected |

The metric is useful only as a lattice-read proxy. It is not a director-grade
acceptance score.

## Decision

Reject the A/B/C room-scale paint candidates. They lower edge scores, but they
read as flat overlay or fog rather than authored dungeon material. The best
quantitative candidate, C, is visually worse than the target because it washes
the room into a broad amber field and does not create believable floor/wall
craft.

Reject E/F as production candidates. They prove that suppressing the lattice can
move the crop, but the AO/contact shapes remain too graphic and too uniform for
the director-grade bar.

Accept D `degrid_only` as a diagnostic target, not as runtime art. It preserves
markers and demonstrates that the active blocker is the dark repeated lattice
itself. This reclassifies the next production problem as
`dark-lattice-authority`, not as missing haze, missing warmth, or insufficient
wall-edge polish.

## Production Direction

Do not integrate any V08 generated overlay as a production asset.

The next production cut must choose one explicit contract before mutation:

1. a narrow room-scale lattice-mask / material-breakup resource contract with
   manifest ownership, preload path, renderer layer placement and focused tests;
2. a structural wall/floor interaction model that removes the dark lattice at
   source or at the wall/floor authority boundary without introducing a second
   visibility, terrain or resource truth.

Both paths must keep actors, loot, cursor, targeting, hazard and telegraph
readable. Any new manifest key, owner row or sheet slot is a PR-08 contract
change and must be routed explicitly before production mutation.

## Non-Goals

1. no golden baseline update
2. no new manifest schema or atlas schema
3. no `core` / `game` terrain snapshot change
4. no baked text, labels, hotkeys or digits in generated art
5. no second visibility or terrain authority
6. no further floor-edge-only or wall-edge-only polish for this blocker

## Next Stop Condition

The next runtime crop must prove a first-read improvement against W05i by both:

1. visual review against `UI/UI-demo-new.png`
2. a reduced lattice proxy on the map-stage crop without marker-readability loss

If the next candidate only lowers the metric by flattening the room into a fog
field, reject it even if the edge score improves.
