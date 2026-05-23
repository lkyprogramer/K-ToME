# Dark UI/UX PR06 Overview Screenshot Manual Record

## Summary

| Field | Value |
| --- | --- |
| result | `PASS` |
| date | 2026-05-23 |
| runner | Codex Computer Use packaged-app whitebox |
| source doc | `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md` |
| flow doc | `docs/computer-use-whitebox-flow.md` |
| app | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/runtime-app/K-ToME.app` |
| app pid | `89956` |
| runtime home | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/runtime-home` |
| scenario | `dark-uiux-pr06-status-quest-skill-overview` |
| launch command | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/launch-packaged-app.sh` |
| runbook | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/cua-runbook.md` |

## Evidence

| Check | Evidence | SHA-256 | Result |
| --- | --- | --- | --- |
| skill / quest / shell overview | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-status-quest-skill-overview-live.png` | `be0cba8e780b1881933b1c61ba428540bb3e708b6467e445b4f9485616176881` | `PASS`: hotbar skill icons, quest log text, inventory/equipment shell, and status HUD row are visible. |
| status overflow fold | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-status-overflow-fold.png` | `be0cba8e780b1881933b1c61ba428540bb3e708b6467e445b4f9485616176881` | `PASS`: status row is capped and shows a `+10` fold badge inside the hero HUD. |
| quest marker row | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-quest-marker-row.png` | `be0cba8e780b1881933b1c61ba428540bb3e708b6467e445b4f9485616176881` | `PASS`: objective/progress text remains readable in the quest/log overview panel. |
| validation overlay compact action | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-validation-overlay-compact.png` | `2ecac60e58014f85cd68a7f9ca97cd7d5d65228847e20309296112c0a7e87c1c` | `PASS`: Computer Use key input triggers the PR06 validation action and the compact log line stays inside the overview panel. |

Every target-window screenshot above has a `.metadata.txt` sidecar with `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_pid=89956`, and `window_bounds=80,38,1280,828`.

## Focus Crops

| Surface | Crop | Result |
| --- | --- | --- |
| hotbar | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/final_skill_hotbar_crop.png` | `PASS`: the visible skills use accepted dark-fantasy weapon/shield slices instead of the rejected generic orange badge set. |
| inscriptions | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/final_inscription_crop.png` | `PASS`: `治疗之印` and `铁壁之印` use accepted Templar skill slices. |
| status HUD | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/final_status_hud_crop.png` | `PASS`: five visible status icons and `+10` fold badge are readable without resizing the HUD shell. |
| quest log | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/final_quest_log_crop.png` | `PASS`: quest objective/progress text is visible and does not overlap adjacent panels. |

## Gate Result

| Command | Result | Notes |
| --- | --- | --- |
| `python3 scripts/generate_dark_final_full_inventory.py --out UI/sprite-sheets/dark-v1-final-full-inventory.json` | `PASS` | Inventory contains 487 expected keys. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr06-status-quest-skill-overview` | `PASS` | Packaged app and PR06 whitebox materialization succeeded. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkArtRandomQa resourcePipelineLint` | `PASS` | Static resource, prompt subject, sprite-map, random QA, and resource authority gates pass. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json -Pktome.darkUiux.artRandomQaRecord=UI/manual-records/dark-uiux-pr06-art-random-qa.json -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md` | `PASS` | Final-full coverage is closed: expected=487, covered=487, missing=0, oldStyle=0, pending/rejected=0. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.ui.status.StatusIconResolverTest --tests com.ktome.client.render.TileRenderModelTest --tests com.ktome.client.render.TileRendererCanvasTest` | `PASS` | Focused client asset/status/render tests pass. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest` | `PASS` | Resource pipeline script tests pass, including art acceptance promotion and random QA decision preservation. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot` | `PASS` | Golden screenshots were rebaselined to the accepted PR06 resources. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged` | `PASS` | Changed preflight/full owner route succeeded, including client smoke, long-run lab, contract/resource/maintainability gates, and dark UI owner-scope coverage tasks. |

## Manual Findings

1. `PASS`: current branch satisfies the script/Gradle final-full resource gate for PR06 after Round 8/9 art regeneration and acceptance promotion.
2. `PASS`: runtime slices used by hotbar and inscription surfaces now come from accepted sheet output, not direct one-off PNG replacement.
3. `PASS`: the status HUD renderer now calls the status icon path and caps the row to five visible statuses plus a fold badge.
4. `PASS`: packaged PR06 scenario covers the overview shell, status fold, quest log, visible skill icons, inscription icons, and compact validation action.
5. `PASS_WITH_CANVAS_LIMIT`: Computer Use can attach to the app/window and send keys, but the game canvas does not expose in-game widget nodes; visual checks therefore use target-window screenshots as required by the whitebox flow.

## R07 Item Art Recheck

This follow-up run fixes the backpack/equipment gap: R07 item, material, affix, and shop-marker sheets were regenerated and promoted after being added to the PR06 art QA gate.

| Field | Value |
| --- | --- |
| app pid | `11318` |
| capture time | `2026-05-23T10:10:16Z` |
| app | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/runtime-app/K-ToME.app` |
| runtime home | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/runtime-home` |

| Check | Evidence | SHA-256 | Result |
| --- | --- | --- | --- |
| PR06 overview after R07 replacement | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-status-quest-skill-overview-live.png` | `a95c74a33104372d09358cfdc76c66c4df34e2b3464650c1a012b16799e80e2a` | `PASS`: right-panel backpack and equipment slots load the new item slices. |
| backpack item crop | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-r07-backpack-live-crop.png` | `3c258a7c37407ac57c47f58ba1c8cecccf664e73b061b56570b638bdd93af46b` | `PASS`: backpack slots show distinct sword, shield/armor, and potion silhouettes instead of old generic badge art. |
| equipment item crop | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-r07-equipment-live-crop.png` | `f4ccbbe76bb8d49574f9742dd6852314be2dd0f3a4da3266feb6edc630cb7d4b` | `PASS`: equipped weapon, shield, and armor slots use accepted R07 item runtime PNGs. |
| hotbar regression crop | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-r07-hotbar-live-crop.png` | `a88e72c802df260f11a573b2ec469c002339c2386abd5eda65230acf953cd54f` | `PASS`: skill hotbar remains on accepted R08 skill slices after the R07 item replacement. |

## Not Covered

This record does not claim a long-duration manual soak or every PR06 menu variant. It covers the PR06-owned packaged overview path and the art/resource contract gates needed before continuing broader interaction testing.
