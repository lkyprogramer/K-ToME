# PR-08 Structural Lattice Suppression V09 Decision

Date: 2026-05-28

## Objective

Try the V08 `dark-lattice-authority` finding as a narrow production candidate
without adding a new manifest key, atlas schema or production resource. The
candidate used renderer-side material-colored bridges over internal floor/floor
joints so actors, loot, cursor, targeting, hazards and telegraphs would remain
above the room compositor.

## Evidence

Runtime evidence root:
`UI/review/dark-uiux-pr08-exploration/structural-lattice-suppression-v09/`

Generated runtime artifacts:

1. `runtime-slice-a-lattice-suppression/ui-demo-new-map-stage-crop.png`
2. `runtime-slice-a-lattice-suppression/ui-demo-new-parity-1672x941.png`
3. `runtime-slice-a-lattice-suppression/ui-demo-new-parity-1280x800.png`
4. `pr08-structural-lattice-suppression-v09-runtime-comparison-board.png`
5. `pr08-structural-lattice-suppression-v09-metrics.json`

Golden actual hashes from the rejected candidate run:

1. `ui-demo-new-parity-1672x941=932feb81c4b57e56419ffb488da690279945f1c43a8d240006c771eaba116957`
2. `ui-demo-new-parity-1280x800=0d5f9c6469417b8da7151bfcfb03baca530c8289b6ef0dc7a05647a92c170fa1`
3. `ui-demo-new-map-stage-crop=94895ffde57a43697448b39c2e297dc8e5a860caacc5a3e38a46aeef9cec6bd6`

The V09 local edge proxy is same-script only and is not a replacement for the
V08 metric run. It shows almost no movement against W05i:

| Candidate | Mean edge | P90 edge |
| --- | ---: | ---: |
| W05i baseline | 1.873 | 5 |
| V09 runtime A | 1.866 | 5 |

## Decision

Reject V09 runtime A and revert the candidate Kotlin/test changes.

The candidate compiled and passed focused renderer checks, but the runtime crop
still reads grid-first. The material-colored seam bridges turn the dark lattice
into a colder visible lattice rather than demoting it. This fails the director
bar and does not match the V08 diagnostic target.

Retain only the evidence and decision record. Do not update golden baselines.

## Commands Run

1. `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas lowers dark lattice authority without adding room fog wash'`
   - PASS for the transient candidate.
2. `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest maintainabilityLint`
   - PASS for the transient candidate.
3. `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
   - EXPECTED FAIL because runtime hashes changed and golden baselines were not updated.

## Current State

No V09 production Kotlin/test change is retained. The retained output is only
the evidence folder plus this rejected decision.

## Next Direction

Do not continue renderer-side line bridges as the main solution. The next cut
must remove or demote the lattice at a stronger authority boundary:

1. source resource or resource-family edit that removes the repeated dark line
   authority before runtime composition, or
2. explicit room-scale material-breakup resource contract that is not aligned to
   every tile joint and does not become a fog overlay.

If a new resource key or owner row is required, route it as a PR-08 contract
change before mutating production manifests.
