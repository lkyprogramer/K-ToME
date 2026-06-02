# PR-08 Candidate F Structural Room Plate Art Edit Decision

**Date**: 2026-06-01
**Packet**: `structural-room-plate-art-edit-2026-06-01`
**Verdict**: Candidate K accepted forward as the current same-key room art
plate resource; D3 map-stage closure is still rejected

## Root Cause

The topology grammar pass removed a real targeting-mode grid source, but the
fixed default crop did not change. That proved the remaining first-read problem
is not another renderer overlay branch. The center-right combat field in the
room plate itself still has an even square-board rhythm that reinforces the
runtime visibility and marker surfaces.

## Route

Generated four review candidates from the current room plate:

1. `h_fracture_spine`: a lighter diagonal fracture pass.
2. `i_right_shadow_shoulder`: stronger right-side dark mass.
3. `j_light_breakup_clumps`: safer light and material breakup.
4. `k_combat_field_breakup`: stronger center-right diagonal fracture, rubble
   clumps, soft dark mass and warm lower transition.

Candidates I, J and K were checked in the actual runtime crop. K is selected
because it reduces the right combat field's square-board read more clearly than
J while avoiding I's highest darkness/readability risk.

## Why Not Micro-Tuning

This packet does not change marker alpha, cursor grammar, fog alpha, seam
weight or renderer topology. The previous runtime pass already exhausted that
path for this blocker. This edit changes the underlying same-key room art plate
surface so runtime overlays sit on a less grid-shaped environment.

## Rejected Alternatives

1. Accept the topology packet as D3 closure: rejected because the fixed default
   crop hash was unchanged.
2. Candidate H: rejected as too weak to affect first-read hierarchy.
3. Candidate I: rejected as the strongest grid suppression but with too much
   darkness risk around active gameplay markers.
4. Candidate J: rejected as safer but still too even in the problem field.
5. Golden rebaseline: rejected because the map slice is still not
   director-grade closure.

## Selected Resource

Production prototype resource now uses Candidate K under the existing key:

```text
ui.map_stage.ruins.room_plate.pr08_demo -> dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png
```

Resource hash:

```text
0e996f13c2bef96e19ab387bef8a805d7a4d9276499f286c4ca7af490d730472
```

Source authority path:

```text
UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/candidates/pr08-room-art-plate-structural-edit-k_combat-field-breakup.png
```

The resource keeps the existing manifest key and raw output path. No manifest
schema, atlas schema, content-pack contract, gameplay rule or localization
contract changed.

## Evidence

Artifacts:

1. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/structural-room-plate-art-edit-candidate-board.png`
2. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/topology-vs-structural-art-runtime-board.png`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/runtime-k-combat-field-breakup/ui-demo-new-map-stage-crop.png`
4. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/runtime-k-combat-field-breakup/evidence-index.tsv`
5. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/candidate-hashes.tsv`
6. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/golden-status.txt`

Actual selected map-stage hash:

```text
ui-demo-new-map-stage-crop=aa39bb4220f4e9e66da5574de02d7b7dcaae982f49375725bad82b2761c018be
```

## Validation

Expected fail, evidence written:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures" --no-configuration-cache
```

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew resourcePipelineLint --no-configuration-cache
```

## Rollback

Restore:

```text
UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/current-production-before-structural-edit.png
```

to:

```text
client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png
```

and revert the `sourceCandidatePath` / `subject` / `materialTags` changes in
`assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`.

## Director Verdict

Accepted forward:

1. Candidate K is the current best room plate foundation.
2. The center-right combat field reads less like a uniform tactical board.
3. Actor, loot and target markers remain readable in the runtime crop.
4. Resource authority still routes through the existing same-key manifest path.

Not accepted:

1. D3 map-stage closure.
2. Golden rebaseline.
3. All-map closure.

Next action:

The current evidence suggests one more structural art-resource pass should focus
on the same center-right field, but with a stronger generated or paintover
source rather than incremental procedural darkening. The target is to make the
room shape and material hierarchy strong enough that runtime visibility bands
become secondary without hiding gameplay markers.
