# PR-08 Direction A Room Art Plate V2 Runtime Decision

> Date: 2026-05-31
> Status: `candidate-f-promoted-to-current-prototype`
> Scope: PR-08 map-stage room art plate resource replacement, still before
> D3 map-stage closure and before golden rebaseline

## Direct Decision

Use V2 Candidate F as the current `ui.map_stage.ruins.room_plate.pr08_demo`
prototype resource.

Reason:

1. Candidate E improves Candidate C but still carries visible large rectangular
   floor masses.
2. Candidate F gives the best runtime balance: less baked square slab rhythm,
   calmer central playable field and enough authored wall/light context.
3. Candidate G removes square rhythm most aggressively, but its runtime center
   is busier and risks competing with actors, loot and telegraphs.

This does not close D3 by itself. It replaces the underlying room art plate so
the next D3 decision is based on a stronger art-resource foundation rather than
more renderer micro-tuning.

## Runtime Evidence

| Evidence | Path |
| --- | --- |
| Prompt set | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/prompts/pr08-room-art-plate-v2-prompt-set.md` |
| Runtime comparison board | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/pr08-room-art-plate-v2-runtime-comparison-board.png` |
| Candidate E runtime | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/runtime-e/ui-demo-new-map-stage-crop.png` |
| Candidate F runtime | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/runtime-f/ui-demo-new-map-stage-crop.png` |
| Candidate G runtime | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/runtime-g/ui-demo-new-map-stage-crop.png` |
| Current prototype resource | `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png` |
| Source authority spec | `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml` |

## Hashes

| Artifact | SHA-256 |
| --- | --- |
| Candidate E source | `3c20ec159165f2ccb40f6741dc950027a69271bf9dc9c0cb1e7cb9b2079736f2` |
| Candidate F source | `21e520fe073a80e6f34f32badd86ec3a5ed181c10eddcbe42a0cb11b497579af` |
| Candidate G source | `d5702cc8a9d1e89e51081a18526a166af4b2e5c7b10b4575c115709e9bd9eca1` |
| Selected runtime resource | `21e520fe073a80e6f34f32badd86ec3a5ed181c10eddcbe42a0cb11b497579af` |
| Candidate E map crop | `5b79f4c0e271d7a93c4dd6b3b2cfc343537b52a394dee59c44abbc4b7e784810` |
| Candidate F map crop | `f43c7bc1085b328c0a85a10ff685e658e5ce6049b9a289f4966c323361826140` |
| Candidate G map crop | `a1906a8938e0f135f6c6cbc6bda77e4a729e0e6d7095330138ad3df7df480abf` |

## Validation Notes

The focused golden command was run for E, F and G:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Result: expected failure for all three candidates, because PR-08 exploration
changes map-stage hashes without golden rebaseline. The command still wrote the
runtime evidence archived above.

## Next Stop Condition

Prepare one final D3 runtime packet for Candidate F, then lky decides whether:

1. accept D3 map-stage closure for the ruins proof slice; or
2. request another art-plate generation/edit pass with a sharper prompt.

Do not resume open-ended renderer alpha/seam tuning as the default next move.
