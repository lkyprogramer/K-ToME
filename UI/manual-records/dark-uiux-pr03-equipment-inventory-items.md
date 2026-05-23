# Dark UI/UX PR03 Equipment Inventory Items Manual Record

## Status

- result: `PASS`
- last-updated: `2026-05-15`
- manual white-box execution: packaged app session executed against `com.ktome.client`.
- capture method: target app window capture via `scripts/capture-macos-app-window.sh` per `docs/computer-use-whitebox-flow.md` section 7.
- appExecutable: `client/build/release/K-ToME.app/Contents/MacOS/K-ToME`
- appExecutableSha256: `ab44f6b6b39ac0191ea96427d50b66011f85cbb67d6e7db0046356a46e8d43d8`
- manualRecordTemplate: `build/whitebox/dark-uiux-pr03-equipment-inventory-items/manual-record-template.md`
- primaryRuntimeHome: `build/whitebox/dark-uiux-pr03-equipment-inventory-items/runtime-home`
- primaryEvidenceDir: `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence`
- shopScenarioRuntimeHome: `build/whitebox/phase4-v4-pr02/runtime-home`
- locale: `zh-CN`
- cleanupStatus: no `K-ToME.app/Contents/MacOS/K-ToME` packaged app process remained after cleanup check.

## Scenario Sessions

| scenarioId | seed | pid evidence | purpose |
| --- | ---: | --- | --- |
| `dark-uiux-pr03-equipment-inventory-items` | `2026050903` | `94339` in screenshot metadata | equipment panel, empty inventory, repeated inventory entries |
| `phase4-v4-pr02` | `2026042432` | `95096` / `97239` in screenshot metadata | PR03 shop and replacement states reuse the PR02 shop setup required by the PR03 doc |

## Evidence

| evidenceLabel | inputSequence | observed result | screenshotPath | metadataPath | sha256Path | result |
| --- | --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr03-equipment-slots` | launch `dark-uiux-pr03-equipment-inventory-items`; capture right equipment panel | Equipment, inscription slots, backpack grid, equipped items, empty visual sockets, and no right-panel ground-loot section are visible. The on-screen log shows `Validation PR-03 item showcase`. | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-equipment-slots.png` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-equipment-slots.png.metadata.txt` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-equipment-slots.png.sha256` | `PASS` |
| `dark-uiux-pr03-inventory-empty` | `F9, Enter, Esc, i` | Inventory modal shows the dark empty state, no placeholder item, and no HUD/log overlap. | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inventory-empty.png` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inventory-empty.png.metadata.txt` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inventory-empty.png.sha256` | `PASS` |
| `dark-uiux-pr03-inventory-stacked` | close inventory, `F9, Right, Enter, Esc, i, Down` | Repeated `healing_potion` entries remain separate stable cells; the rare charm cell is visible with its own frame, and the selection movement does not shift the grid. | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inventory-stacked.png` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inventory-stacked.png.metadata.txt` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inventory-stacked.png.sha256` | `PASS` |
| `dark-uiux-pr03-inscription-shop` | launch `phase4-v4-pr02`; `F9, Enter, Esc`; capture shop surface | Buy/sell shop surface is visible with dark offer cards, price/affordability markers, inscription marker, empty sell state, and no visual rule encoding in the icon assets. | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inscription-shop.png` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inscription-shop.png.metadata.txt` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-inscription-shop.png.sha256` | `PASS` |
| `dark-uiux-pr03-shop-full-slot-replace` | relaunch `phase4-v4-pr02`; `F9, Right, Enter, Down, Down, Enter, 1, 2, 3, 4, 5`; capture prompt; `Esc` to cancel | Full-slot replacement prompt shows four current slots with hotkeys `5-8`, candidate/current slot context, category rows, and selected hotkey `5`; numeric `1-4` did not leave the prompt before selecting `5`; `Esc` returned out of the prompt without confirming replacement. | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-shop-full-slot-replace.png` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-shop-full-slot-replace.png.metadata.txt` | `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-shop-full-slot-replace.png.sha256` | `PASS` |

Screenshot sidecar audit:

- `dark-uiux-pr03-equipment-slots.png`: `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_pid=94339`, SHA-256 `05e195d6033a01f3f15292ff950fa2040157a1ef2ecab9242546fa05f8b1ac5a`.
- `dark-uiux-pr03-inventory-empty.png`: `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_pid=94339`, SHA-256 `e78a11d440118a848f09c99200ea62752c0716d55f6b5abf354b848b95d3bc80`.
- `dark-uiux-pr03-inventory-stacked.png`: `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_pid=94339`, SHA-256 `b30b83864d4f990d3a11ac7408a4d38bc67e08c79e5e1af3277f89f7afb8acb7`.
- `dark-uiux-pr03-inscription-shop.png`: `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_pid=95096`, SHA-256 `62eddf42f8b58c5b39a45b19cf5a34e94d825881f4d52d58d169c34293220612`.
- `dark-uiux-pr03-shop-full-slot-replace.png`: `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_pid=97239`, SHA-256 `53ed1bdff2a316d3eeb5c393f35e43466ba91c043ee8290d81b9ddff805d7d0d`.

## Supporting Logs

- primary launcher log: `build/whitebox/dark-uiux-pr03-equipment-inventory-items/evidence/dark-uiux-pr03-app.log`
- shop launcher log: `build/whitebox/phase4-v4-pr02/evidence/app.log`
- required PR03 log event `log.validation.item.pr03_showcase` is visible in the captured in-game log panel for `dark-uiux-pr03-equipment-slots`.

## Validation Commands

- `./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest` -> `PASS`
- `./gradlew :client:test --tests com.ktome.client.input.ValidationCommandSourceTest` -> `PASS`
- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:test --tests com.ktome.client.input.ValidationCommandSourceTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest` -> `PASS`
- `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr03-equipment-inventory-items` -> `PASS`
- `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr02` -> `PASS`
- `./gradlew localeLint contractLint maintainabilityLint` -> `PASS`
- `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.EquipmentInventoryPresenterTest darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03 -Pktome.darkUiux.requiredOwnerSheetIds=r07-items-base,r07-items-unique-artifact,r07-items-affix-material` -> `PASS`

## Fallback Injection Link

- fallback record: `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md`
- fallback screenshot: not required in this run; resolver/presenter/coverage evidence was used instead of mutating the official runtime manifest.

## Residual Risk

- Manual evidence uses one `zh-CN` window size and one selected replacement hotkey. Focused tests cover the remaining replacement hotkey range and fallback behavior.
- `phase4-v4-pr02` is intentionally reused for shop and full-slot replacement states, matching the PR03 manual white-box table.

## PR06 R07 Art Refresh Addendum

On 2026-05-23, the R07 item/material/affix/shop-marker sheets were regenerated and promoted as part of the PR06 full-manifest art QA fix. This supersedes the older PR03 visual evidence for icon art style while preserving the PR03 layout/interaction contract.

| Check | Evidence | SHA-256 | Result |
| --- | --- | --- | --- |
| PR06 packaged backpack crop | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-r07-backpack-live-crop.png` | `3c258a7c37407ac57c47f58ba1c8cecccf664e73b061b56570b638bdd93af46b` | `PASS`: backpack item icons are regenerated dark-fantasy still-life silhouettes, not the old circular/vector badge set. |
| PR06 packaged equipment crop | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-r07-equipment-live-crop.png` | `f4ccbbe76bb8d49574f9742dd6852314be2dd0f3a4da3266feb6edc630cb7d4b` | `PASS`: equipped weapon, shield, and armor slots use accepted R07 runtime PNGs. |

Supporting resource gates:

- `darkArtRandomQa`: `PASS`, default sheet set now includes `r07-items-base`, `r07-items-unique-artifact`, and `r07-items-affix-material`.
- `darkManifestCoverageLint final-full`: `PASS`, expected keys = 487.
- `spriteSheetMapLint --require-reviewed-qa`: `PASS` for R07/R08/R09 accepted sheet set.
