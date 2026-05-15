# Dark UI/UX PR-02-1 Demo Shell Foundation Manual Record

manualReviewers:
  - id: `codex-whitebox-runner`
    role: `agent-whitebox-runner`
    verdict: `pass_for_ui_demo_new_visual_parity_golden`
reviewedAt: `2026-05-15T00:04:43+0800`
demoReference: `UI/UI-demo-new.png`
demoParityVerdict: `pass_for_ui_demo_new_first_screen_visual_parity`
whiteboxStatus: `golden_side_by_side_pass_packaged_capture_not_refreshed_this_turn`
blockingFindings: []
nonBlockingFindings:
  - `Packaged app inventory-open capture was not refreshed in this pass; inventory pagination behavior is covered by focused input tests and golden page evidence.`
  - `Broader non-demo-map terrain, actor, prop, VFX, and lighting catalog quality remains PR-05/PR-06 scope; UI-demo-new first-screen terrain, actor, stairs, and backdrop are not deferred.`
ownerAndNonOwnerCloseoutSignoff: `golden_owner_scope_recorded`
sameFrameReason: `ui-demo-new-parity-shell-includes-right-panel-bottom-and-map-stage`

## Scope

- scenarioId: `dark-uiux-pr02-1-demo-shell-foundation`
- prId: `PR-02-1`
- manualRecordPath: `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`
- goldenRoot: `client/build/reports/golden/dark-uiux-pr02-1`
- visualReviewArtifact: `build/reports/verification/dark-uiux/ui-demo-new-side-by-side-current.png`
- demoReference: `UI/UI-demo-new.png`
- locale: `zh-CN`
- seed: `2026051102`

## Screenshot Label Coverage

| label | source | sha256 | status | note |
| --- | --- | --- | --- | --- |
| `ui-demo-new-parity-1672x941` | golden | `0290f40cad40bc45683a2800833153fa0bcb197e288e97a3af7d61ffafb0e588` | pass | Full demo-aspect evidence: left rail, dominant map stage, right equipment/inscription/backpack/operation regions, bottom hero/action/log deck. |
| `ui-demo-new-parity-1280x800` | golden | `dd1e4ce70310bc56ae0ca859db51b1d17b1679b4e27dbb3f38ea6e956bbf4a92` | pass | Standard viewport evidence uses the same shell hierarchy and no right-panel ground loot. |
| `ui-demo-new-right-panel-grid` | golden | `90b5f1c8513bcc3ac5e08fbea9017ed28a807eee17bd1b0a7f86a9befbfd4286` | pass | Equipment sockets, inscriptions 5-8 real rows, inscriptions 9-12 empty rows, backpack 4x2, and operation hints are bounded. |
| `ui-demo-new-bottom-deck-no-command-hints` | golden | `ebb829c62a5830ec2d53d9f348b8d60f8edd57df9b6ca189aacc098f2e03ad78` | pass | Bottom deck contains hero/action/log only; visible shortcut help is confined to right operation hints. |
| `ui-demo-new-inventory-page-1` | golden | `5e7422f278285fd3eca80aa4dbc8273307fc61dfc70dd503c243e0f3d139cf8b` | pass | Inventory pagination fixture first page. |
| `ui-demo-new-inventory-page-2` | golden | `a741a91f4a3d2a1630ed979c57c15f306d157a19ac277291fac1790358656997` | pass | Inventory pagination fixture second page. |
| `ui-demo-new-nav-rail-crop` | golden | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` | pass | Dedicated crop evidence contains the nav rail at inspection quality. |
| `ui-demo-new-map-stage-crop` | golden | `551b3baf3d5751d5fa311164254deb81b2e5c0b7005cdbbaa6cb55cb98ade212` | pass | Dedicated crop evidence contains the map stage at inspection quality. |
| `ui-demo-new-side-by-side-current` | visual review artifact | `N/A` | pass | Side-by-side comparison against `UI/UI-demo-new.png`; used for director-level manual review. |

## Demo Delta Checklist

| demo element | status | evidence |
| --- | --- | --- |
| left icon rail is bounded to map-stage height and does not enter bottom deck | pass | `ui-demo-new-parity-1672x941` |
| dominant map stage uses `ui.shell.map_stage.backdrop` before terrain and no longer shows a programmatic black grid | pass | `ui-demo-new-map-stage-crop` |
| UI-demo-new first-screen ruins ground/wall resources are readable and stylistically closer to the DEMO | pass | regenerated `r02-ui-demo-ruins-tiles` contact sheet and golden screenshot |
| UI-demo-new first-screen vanguard actor and stairs prop are readable at gameplay scale | pass | regenerated `r03-ui-demo-actor-props` contact sheet and golden screenshot |
| right panel order is equipment, inscriptions, backpack, operation hints | pass | `ui-demo-new-right-panel-grid` |
| no right-panel ground-loot section is visible | pass | `ui-demo-new-right-panel-grid` |
| equipment is icon-first socket layout with display-only extra sockets | pass | `ui-demo-new-right-panel-grid` |
| inscriptions 5-12 are dual-column framed rows; 5-8 carry real inscription content and 9-12 stay empty | pass | `ui-demo-new-right-panel-grid` |
| backpack is 4x2 current page and supports pagination beyond 8 items | pass | `ui-demo-new-inventory-page-1`, `ui-demo-new-inventory-page-2`, `InputHandlerTest` |
| bottom hero card contains floor, HP/resource, attack and defense summary | pass | `ui-demo-new-bottom-deck-no-command-hints` |
| action deck supports `PLAYER_ACTIVE_TALENT_SLOT_COUNT = 4` with the fourth slot reserved/empty when needed | pass | `ui-demo-new-bottom-deck-no-command-hints` |
| bottom deck has no duplicate shortcut/help block; operation hints are only in the right panel | pass | `ui-demo-new-bottom-deck-no-command-hints` |

## Commands Run / Evidence Commands

| command / action | result | note |
| --- | --- | --- |
| `python3 scripts/generate_sheet_prompt.py` | PASS | Regenerated prompt files after tightening map-stage, ruins tile, actor, and prop subjects. |
| `python3 scripts/codex-generate-image.py ... --out assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png --overwrite` | PASS | Regenerated PR-02-2 ruins tile sheet for UI-demo-new first-screen readability. |
| `python3 scripts/repack_generated_sheet.py --sheet-id r02-ui-demo-ruins-tiles --overwrite` | PASS | Repacked generated r02 sheet. |
| `python3 scripts/slice_spritesheet.py --overwrite` | PASS | Runtime sprite slices regenerated. |
| `python3 scripts/render_contact_sheet.py --overwrite` | PASS | Contact sheets regenerated for visual QA. |
| `python3 scripts/verify_sprite_sheet_map.py --check sheet-plan --owner-contract UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml` | PASS | PR-02-1 owner sheet-plan coverage. |
| `python3 scripts/verify_sprite_sheet_map.py --check sheet-plan --owner-contract UI/sprite-sheets/owner-contracts/pr02-2-owner-keys.yaml` | PASS | PR-02-2 owner sheet-plan coverage. |
| `python3 scripts/verify_sprite_sheet_map.py --check map --report assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl` | PASS | PR-02-1 runtime sprite map coverage. |
| `python3 scripts/verify_sprite_sheet_map.py --check map --report assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl` | PASS | PR-02-2 runtime sprite map coverage. |
| `python3 scripts/verify_dark_manifest_coverage.py --coverage-mode owner-scope --owner-pr PR-02-1 --owner-contract UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml --report build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-1-owner-scope.json` | PASS | PR-02-1 owner-scope manifest coverage. |
| `python3 scripts/verify_dark_manifest_coverage.py --coverage-mode owner-scope --owner-pr PR-02-2 --owner-contract UI/sprite-sheets/owner-contracts/pr02-2-owner-keys.yaml --report build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-2-owner-scope.json` | PASS | PR-02-2 owner-scope manifest coverage. |
| `python3 scripts/verify_dark_key_registry.py` | PASS | Dark key registry consistency. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests '*DemoShellLayoutTest' --tests '*DemoShellRendererTest' --tests '*TileRendererCanvasTest' --tests '*InputHandlerTest' --tests '*ValidationCommandSourceTest' :game:test --tests '*ValidationScenarioRegistryTest' :tools:test --tests '*Phase4V4WhiteboxScenarioCliTest' --tests '*DarkSpriteSheetPipelineScriptTest' --tests '*VerificationImpactAnalyzerTest' --tests '*VerifyChangedBuildContractTest'` | PASS | Focused UI layout/render/input/scenario/tools routing checks. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --rerun-tasks` | PASS | Golden evidence labels and hashes above were refreshed and accepted. |
| `magick UI/UI-demo-new.png + current golden -> build/reports/verification/dark-uiux/ui-demo-new-side-by-side-current.png` | PASS | Regenerated the current side-by-side review artifact from `UI/UI-demo-new.png` and `ui-demo-new-parity-1672x941.png`. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke :client:goldenScreenshot` | PASS | Client smoke task passed; goldenScreenshot was up-to-date after the rerun. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :tools:darkManifestCoveragePr02_1OwnerScope :tools:darkManifestCoveragePr02_2OwnerScope` | PASS | Resource and manifest owner gates passed; PR-02-1 owner-scope expectedKeys=16, PR-02-2 expectedKeys=4. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint` | PASS | Anti-bloat / maintainability gate. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged` | PASS | Changed-file routing completed; missing probe visual/audio messages were fallback warnings inside successful contract preflight. |
| `git diff --check` | PASS | No whitespace errors. |

## Known Limits

- Packaged-app Computer Use capture was not refreshed in this pass; current visual pass is based on deterministic golden screenshots and side-by-side manual review.
- Complete non-demo terrain, actor, prop, VFX, and dynamic lighting art remains PR-05/PR-06 scope. The UI-demo-new first-screen map backdrop, ruins ground/wall, vanguard actor, and stairs prop are covered here and are not deferred.
