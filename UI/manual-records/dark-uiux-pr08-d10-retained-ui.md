# Dark UI/UX PR-08 D10 Retained UI Planning Record

**Date**: `2026-06-03`
**Status**: `docs-only-authority-promotion`
**Owner**: `PR-08 D10`
**Contract**: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md`

## Runtime Boundary

This record is for `D10-P0` docs-only authority freeze. The closure marker is `runtime migration has not started`.

Renderer, resource, manifest, golden harness and packaged crop changes are not D10-P0 closure evidence. If such changes exist in the worktree, they must be split into an independent PR-08 map-stage evidence packet or reclassified under a later `D10-P1+` retained UI implementation phase with its own tests and manual record.

Round 2 review note: the current combined worktree still contains PR-08 runtime renderer/resource/golden changes. This manual record is valid only for a docs/lint-only D10-P0 staging packet; it must not be used as D10-P0 closure evidence if staged with runtime implementation files.

## Decision

D10 promotes the next PR-08 UI/UX route from hand-written `TileCanvas` UI composition to retained UI / theme authority:

1. Adopt libGDX official Scene2D `Stage`, Scene2D UI `Skin` and `Table`.
2. Keep `TileRenderer` as map rendering authority and embed it through `MapStageActor`.
3. Defer KTX until a compatibility spike proves it matches repo-pinned libGDX and Kotlin versions.
4. Use Skin Composer and TexturePacker only as authoring tools.
5. Do not adopt VisUI or gdx-skins as PR-08 D10 runtime dependencies.
6. Add retained UI adaptation plans for main menu, in-game shell, equipment/inventory detail and talent tree configuration.
7. Execute D10 as a phase-gated hard refactor, not a compatibility layer. Migrated surfaces must not keep old immediate UI as a long-term production route.

## Phase Plan

Implementation phases:

| Phase | Scope | Required closure |
| --- | --- | --- |
| `D10-P0` | authority freeze and cutover inventory | docs/index/manual record updated; no runtime claim |
| `D10-P1` | Scene2D kernel, Skin authority and input boundary | focused Scene2D/Skin/input tests |
| `D10-P2` | standalone screens hard cut | main menu, validation, outcome/error retained evidence |
| `D10-P3` | focus, modal, tooltip and hit-test model | keyboard, modal blocking and tooltip whitebox |
| `D10-P4` | in-run shell and `MapStageActor` | retained shell golden and map actor bounds evidence |
| `D10-P5` | equipment, inventory, shop and detail workbench | inventory/equipment/shop retained evidence |
| `D10-P6` | talent tree and active slot modal | talent tree and active slot modal retained evidence |
| `D10-P7` | combat, frontstage, log, nav and overlay closure | combat/frontstage/bottom/nav retained evidence |
| `D10-P8` | dead route removal and final gates | old route removal record, full client/golden/whitebox/verify gates |

Phase transition rule:

1. Each phase must record actual commands and artifacts before the next phase starts.
2. Each migrated surface must have one production route.
3. Temporary adapters need a removal phase.
4. Packaged whitebox is required whenever player-visible runtime shell or screen behavior changes.

## Evidence Status

This record contains planning evidence only. It does not claim runtime visual closure, packaged parity, golden rebaseline or all-map closure.

Required future evidence:

1. `dark-uiux-pr08-d10-retained-shell`
2. `dark-uiux-pr08-d10-main-menu-retained`
3. `dark-uiux-pr08-d10-inventory-workbench-retained`
4. `dark-uiux-pr08-d10-right-panel-inventory-retained`
5. `dark-uiux-pr08-d10-talent-tree-retained`
6. `dark-uiux-pr08-d10-active-slot-modal-retained`
7. `dark-uiux-pr08-d10-shop-retained`
8. `dark-uiux-pr08-d10-combat-decision-retained`
9. `dark-uiux-pr08-d10-frontstage-retained`
10. `dark-uiux-pr08-d10-bottom-deck-retained`
11. `dark-uiux-pr08-d10-nav-rail-retained`
12. packaged whitebox retained UI scenarios after runtime migration

## Validation Results

Initial D10-P0 doc validation for this record:

| Command | Result | Notes |
| --- | --- | --- |
| `rg -n '^#{1,3} ' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` | passed | heading inventory printed D10 doc and manual record headings |
| `rg -n 'T[O]DO|T[B]D|F[I]XME' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` | passed | no matches |
| `rg -n 'D10-S[0-9]' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | passed | no matches |
| `rg -n 'GoldenScreenshotHarnessTest\\.<|<d10|<targeted' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | passed | no matches |
| `git diff --check -- UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt` | passed | whitespace / patch hygiene passed |
| `./gradlew acceptanceContractLint --no-configuration-cache` | passed | first run exposed a case-sensitive D10 closure marker mismatch; record text was corrected and rerun passed |
| `./gradlew maintainabilityLint --no-configuration-cache` | passed | required because D10 lint is a non-trivial tools Kotlin governance change |

Command block used for the docs-only checks:

```bash
rg -n '^#{1,3} ' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md
rg -n 'T[O]DO|T[B]D|F[I]XME' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md
rg -n 'D10-S[0-9]' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
rg -n 'GoldenScreenshotHarnessTest\\.<|<d10|<targeted' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md
rg -n '[ \t]+$' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md
git diff --check -- UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt
```

## Round 2 Validation Results

Round 2 validation after probe routing cleanup:

| Command | Result | Notes |
| --- | --- | --- |
| `./scripts/verify-bootstrap.sh` | failed / interrupted after failure evidence was captured | failed in `:client:test` at `TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback`; this is part of the mixed PR-08 runtime renderer packet, not D10-P0 docs-only closure |
| `./gradlew acceptanceContractLint maintainabilityLint :client:compileTestKotlin :client:pr08RealProcgenProbe --no-configuration-cache` | passed | validates D10 lint, governance lint, client test compilation and the dedicated D9 real-procgen probe route |
| `./gradlew :client:pr08RuntimeTopologyProbe --no-configuration-cache` | passed | validates runtime topology probes as a dedicated owner command outside full golden |

The two PR-08 probe commands above are not D10-P0 closure evidence. They only verify the round2 probe-routing cleanup.
