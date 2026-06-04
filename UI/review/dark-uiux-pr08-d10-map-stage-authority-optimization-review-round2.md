# Dark UI/UX PR-08 D10 Map-Stage Authority Optimization Review Round 2

**Review target**: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` plus current PR-08 D10 / D9 worktree changes
**Local date**: `2026-06-04`
**Verdict**: `request_changes` for the current combined worktree; D10-P0 docs/lint fixes are acceptable if split as a docs-only packet

## Findings

### P1-1. Current worktree still violates the D10-P0 split rule it now documents

The previous D10 doc/lint findings were mostly fixed: D10 is now registered in `Phase4V4AcceptanceContractLintTest`, D10 bash placeholders are rejected, numeric `D10-S*` labels are rejected, and the manual record has actual validation results. The remaining blocker is that the actual worktree still mixes the docs-only D10-P0 authority promotion with runtime renderer/resource/golden changes.

Evidence:

1. D10 now explicitly says `D10-P0` can only close as docs-only and that renderer/resource/manifest/golden/package changes must be split or reclassified: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:86-92`.
2. D10-P0 whitebox section repeats that runtime renderer/resource/manifest/golden changes are not valid P0 closure evidence: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:544-548`.
3. D10-P0 "Do not enter P1" also blocks closure if the PR/worktree still mixes docs-only evidence with runtime implementation changes: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:550-555`.
4. The current status still contains tracked runtime changes in `RoomArtPlatePresentation.kt`, `TileRenderer.kt`, `GoldenScreenshotHarnessTest.kt`, manifests and resource tests, plus many untracked PNG resources and `Pr08RoomArtPlateD9RealProcgenProbeTest.kt`.
5. The renderer path is not just incidental: `RoomArtPlatePresentation` adds new topology-risk hybrid rendering passes such as band mantle, aperture pressure, boundary wall mass slabs and wall run veils: `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt:279-292`.
6. The golden path adds runtime topology crop probes and a single-shot LWJGL capture helper: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:821-831`, `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:3906-3976`.

Impact:

This is now an internal contract violation, not just a review preference. A reviewer cannot tell whether the submission is D10-P0 docs-only, a PR-08 map-stage runtime packet, or a later retained UI implementation packet. If merged as-is, the D10 manual record can say "runtime migration has not started" while the same PR changes runtime renderer/golden/resource behavior.

Fix direction:

1. Split the commit/worktree into at least two packets:
   - D10-P0: `UI/pr`, `UI/PLAN.md`, screen matrix, manual record, acceptance lint only.
   - PR-08 map-stage runtime/evidence: renderer, manifests, PNGs, golden probes, focused renderer/resource tests.
2. If the runtime packet is intentionally part of D10, reclassify it as a later implementation packet and add the matching D10-P1+ Scene2D owner surface. Current code still has no `KtomeUiStage`, `KtomeRootTable`, `KtomeSceneSkin` or `MapStageActor`.
3. Keep the D10-P0 manual record out of any PR that contains renderer/resource/golden runtime changes.

### P2-1. D9 real-procgen probe is untagged, so default `:client:test` runs evidence generation

Evidence:

1. `Pr08RoomArtPlateD9RealProcgenProbeTest` has a plain `@Test` and no class/method tag: `client/src/test/kotlin/com/ktome/client/golden/Pr08RoomArtPlateD9RealProcgenProbeTest.kt:17-23`.
2. That test writes `evidence-index.tsv` and `summary.md` under `client/build/reports/golden/dark-uiux-pr08-d9-real-procgen`: `client/src/test/kotlin/com/ktome/client/golden/Pr08RoomArtPlateD9RealProcgenProbeTest.kt:94-98`, `client/src/test/kotlin/com/ktome/client/golden/Pr08RoomArtPlateD9RealProcgenProbeTest.kt:162-166`.
3. The root default `test` task only excludes tests with tags from `verificationOnlyTestTags`: `build.gradle.kts:36-69`, `build.gradle.kts:309-316`.
4. Since this probe has no tag, it is part of ordinary `:client:test`, even though its name, output location and review docs treat it as D9 evidence/probe.

Impact:

This pollutes the small default client test lane with an evidence-producing probe. It also makes `:client:test` write golden/report artifacts that are not needed for ordinary unit-test verification. That is a `test-gap` / verification-boundary issue: future developers can trigger D9 evidence generation by running a normal test suite, and timing/report side effects become part of the small-test lane.

Fix direction:

1. Add a dedicated tag, for example `@Tag("pr08RealProcgenProbe")`.
2. Add that tag to `verificationOnlyTestTags`, or create a dedicated Gradle task such as `:client:pr08RealProcgenProbe`.
3. If the test must stay in default `:client:test`, stop writing repo build evidence from it and write only to `@TempDir`; keep report materialization in the dedicated evidence task.

### P2-2. Runtime topology probes still run inside full `:client:goldenScreenshot` while bypassing LWJGL cleanup

Evidence:

1. `GoldenScreenshotHarnessTest` is class-tagged with `@Tag("goldenScreenshot")`: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:97-98`.
2. The two runtime topology probe methods add `@Tag("pr08RuntimeTopologyProbe")`, but they still inherit the class-level `goldenScreenshot` tag and therefore run in full `:client:goldenScreenshot`: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:821-831`.
3. These probes call `withSingleShotLwjgl3Context`: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:1356-1359`.
4. The helper reflects into `Lwjgl3Application.windows`, clears the internal window array, and overrides both `cleanupWindows()` and `cleanup()` as no-ops: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:3906-3976`.

Impact:

The added comment correctly says this helper is not general golden infrastructure, but the tag setup still makes full `:client:goldenScreenshot` execute it. That means the normal golden gate now contains a path that intentionally skips native teardown. Even if the focused run passes, this can leave GLFW/window lifecycle behavior order-dependent in the full golden suite.

Fix direction:

1. Move these probes to a separate class not annotated with `@Tag("goldenScreenshot")`, or explicitly exclude `pr08RuntimeTopologyProbe` from full golden and add a dedicated task for it.
2. If they must stay in `goldenScreenshot`, do not skip `cleanupWindows()` / `cleanup()`; fix the macOS hidden-window cleanup hang at the harness level.
3. Document the dedicated probe command in the PR-08 evidence record and keep it separate from general golden closure.

### P2-3. D10 phase gates are executable now, but still not phase-specific enough to prove the declared evidence labels

Evidence:

1. D10-P2 through P7 phase gates now use full `./gradlew :client:goldenScreenshot --no-configuration-cache`: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:623-631`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:667-675`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:709-718`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:761-769`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:814-822`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:860-868`.
2. The same phases declare concrete D10 evidence labels such as `dark-uiux-pr08-d10-main-menu-retained`, `dark-uiux-pr08-d10-retained-shell`, `dark-uiux-pr08-d10-inventory-workbench-retained`, `dark-uiux-pr08-d10-talent-tree-retained`: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:634-639`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:720-726`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:772-778`, `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:825-831`.
3. There is currently no D10 label-registry test or lint asserting those label artifacts exist when a phase is closed.

Impact:

Replacing placeholder method names with full golden commands made the commands executable, but it weakened the test boundary. Full golden passing does not prove the phase-specific D10 labels were created, nor does it prove the declared actor-tree / whitebox evidence exists. This is acceptable as a broad regression gate, but not sufficient as the phase closure gate.

Fix direction:

1. Keep full `:client:goldenScreenshot` as a broad regression gate if desired.
2. Add a focused D10 label registry or actor-tree evidence test per phase before that phase can close.
3. Update the phase gates to name either the exact test methods or a dedicated tag such as `d10RetainedShell`, `d10StandaloneScreens`, `d10InventoryEquipment`, etc.

## Requirement Alignment

| Requirement | Current state | Conclusion |
| --- | --- | --- |
| D10 document is lint-protected | `UI08-D10` is now in `uiPrDocs`, and `assertUiD10RetainedUiContract` checks phase ids, manual record, transition fields, placeholder-free bash blocks and no numeric `D10-S*` labels | 一致 |
| Bash command placeholders removed | D10 bash gates no longer contain `GoldenScreenshotHarnessTest.<...>` / `<d10...>` placeholders, and lint rejects bash angle placeholders | 一致 |
| Manual record has actual validation results | `Validation Results` records docs scans, `acceptanceContractLint` and `maintainabilityLint` as passed | 一致 |
| D10-P0 docs-only split | D10 doc/manual record now state the split rule, but the current worktree still mixes runtime renderer/resource/golden changes | 不一致 for current combined worktree |
| Runtime map-stage packet verification | Focused D9 probe passes in this review, but full renderer/resource/golden owner ladder was not rerun in this review | 部分一致 |
| Probe/golden verification boundary | Runtime topology probe and D9 real-procgen probe exist, but task/tag boundaries are not clean | 部分一致 |

## Removal/Iteration Plan

### Defer Removal / Split Required: D10-P0 vs PR-08 map-stage runtime packet

| Field | Details |
| --- | --- |
| Location | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:86-92`, `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt:279-292` |
| Phase / Work Package | D10-P0 docs-only and separate PR-08 map-stage evidence packet |
| Touched contract | UI PR governance, golden evidence, visual manifest/resource owner gates |
| Evidence | D10 says runtime changes are invalid P0 closure, but current worktree includes renderer, manifests, PNG resources and golden harness changes |
| Preconditions | Decide whether the runtime changes are a PR-08 map-stage packet or a later D10-P1+ implementation packet |
| Steps | 1. Commit D10-P0 docs/lint separately. 2. Move map-stage renderer/resource/golden changes into their own packet. 3. Attach focused renderer/resource/golden owner gates to that packet. |
| Verification | `acceptanceContractLint maintainabilityLint` for D10-P0; renderer packet should run focused `ManifestResolveTest`, `TileRendererCanvasTest`, resource pipeline lint and the relevant PR-08 golden/probe tasks |
| Rollback | Revert either packet independently; do not leave D10-P0 manual record coupled to runtime changes |

### Defer Cleanup: PR-08 probe task boundaries

| Field | Details |
| --- | --- |
| Location | `client/src/test/kotlin/com/ktome/client/golden/Pr08RoomArtPlateD9RealProcgenProbeTest.kt:17-23`, `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:821-831` |
| Phase / Work Package | PR-08 evidence / D9 probe |
| Touched contract | Verification routing and golden evidence materialization |
| Evidence | One probe is untagged and runs in default `:client:test`; another inherits full `goldenScreenshot` while using a no-cleanup native helper |
| Preconditions | Decide which probes are ordinary assertions versus evidence-generating verification tasks |
| Steps | 1. Add explicit tags. 2. Add dedicated Gradle tasks or exclusions. 3. Move report writing out of default test lanes. 4. Record exact owner commands in PR-08 manual/evidence docs. |
| Verification | Run `:client:test`, the dedicated probe task, and full `:client:goldenScreenshot` separately to prove routing is clean |
| Rollback | Restore probes to manual-only evidence if the dedicated task proves unstable |

## Additional Suggestions

1. Add a small D10 lint assertion that every evidence label listed in the D10 phase plan also appears in `UI/pr/screen-coverage-matrix.md`. This would prevent a future doc-only phase from drifting away from screen coverage.
2. In `assertUiD10RetainedUiContract`, consider checking the manual record has an `acceptanceContractLint` result row, not just `Validation Results`. The current check proves the section exists, but not that the documented P0 gate was recorded.
3. If the map-stage runtime packet remains in this branch, add a short packet-level README or manual record naming it separately from D10-P0, so review output and future commit messages do not collapse the two concerns again.

## Suggested Verification

Already run in this review:

```bash
git diff --check
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.golden.Pr08RoomArtPlateD9RealProcgenProbeTest.dark uiux pr08 d9 real procgen roi probe writes distribution artifacts" --no-configuration-cache
```

All four passed in this review.

Recommended before closing the runtime map-stage packet:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test \
  --tests com.ktome.client.assets.ManifestResolveTest \
  --tests com.ktome.client.render.TileRendererCanvasTest \
  --no-configuration-cache
./gradlew assetLint styleLint manifestLint resourcePipelineLint --no-configuration-cache
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache
```

Recommended after probe routing cleanup:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
```

## Summary

Round 2 fixed the main D10 document/lint defects from the first review. The remaining blockers are about submission boundary and verification ownership: the current worktree still combines D10-P0 with runtime renderer/resource/golden work, the D9 real-procgen probe is accidentally in the default small test lane, and the runtime topology probes still run inside full golden while bypassing LWJGL cleanup.

I would merge the D10-P0 docs/lint packet after splitting it. I would not merge the current combined worktree as one PR.
