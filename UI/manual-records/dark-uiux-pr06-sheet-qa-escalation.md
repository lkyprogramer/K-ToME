# Dark UI/UX PR06 Sheet QA Escalation Record

## Summary

| Field | Value |
| --- | --- |
| result | `PASS_R07_R08_R09_REGENERATED_ACCEPTED_RUNTIME_PROMOTED` |
| date | 2026-05-23 |
| ownerPr | `PR-06` |
| source doc | `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md` |
| art bible | `UI/ART_STYLE_BIBLE.md` |
| prompt source | `UI/sprite-sheets/sheet-plan.yaml` |
| generated prompt index | `UI/sprite-sheets/prompts/dark-v1/prompt-index.json` |
| random QA record | `UI/manual-records/dark-uiux-pr06-art-random-qa.json` |

## Root Cause

The rejected PR06 skill/talent/status/quest resources were not a runtime mapping issue. The prompt source used generic per-cell subjects such as `active skill icon`, `talent node icon`, `readable silhouette`, `elemental shape language`, and `landmark silhouette`. `scripts/generate_sheet_prompt.py` correctly copied those subjects into prompt files, so image generation received weak visual instructions and produced generic triangle/circle/rune-like cells.

The backpack/equipment regression was a second process gap: `r07-items-base`, `r07-items-unique-artifact`, and `r07-items-affix-material` were still outside the PR06 art acceptance and Gradle random-QA default. Their runtime PNGs were sliced from old pre-PR06 raw sheets, so coverage could be green while backpack-visible art had never passed the PR06 Art Bible gate.

The fix keeps prompt generation as a deterministic transcription step and moves quality enforcement into the sheet-plan lint contract.

## Rejected Artifacts

These current raw/contact sheets are rejected as final art evidence until regenerated from the rewritten prompts and manually passed:

| Sheet | Disposition | Reason |
| --- | --- | --- |
| `r07-items-base` | `REJECTED_REGENERATED` | Backpack/equipment item icons were outside the PR06 art gate and used old pre-PR06 raw sheet art. |
| `r07-items-unique-artifact` | `REJECTED_REGENERATED` | Unique/artifact icons were not sampled under the PR06 random QA gate. |
| `r07-items-affix-material` | `REJECTED_REGENERATED` | Material, affix, and shop markers were not covered by PR06 subject-quality lint or random QA. |
| `r08-skills-vanguard-berserker` | `REJECTED` | Skill/talent cells collapse into generic emblem/rune shapes; not acceptable as action-command/progression-emblem art. |
| `r08-skills-templar-rogue` | `REJECTED` | Same prompt-template failure; hotbar-sized skills are not visually distinct enough. |
| `r08-skills-arcanist-spellblade` | `REJECTED` | Same prompt-template failure; magic/weapon actions do not carry concrete subject language. |
| `r09-status-damage` | `REJECTED_PENDING_REGEN` | Status, mutation, and damage grammar was too similar and underspecified. |
| `r09-quest-zone-profession` | `REJECTED_PENDING_REGEN` | Quest, tree, profession, and zone subjects lacked concrete material/landmark/class identity instructions. |
| `r09-fallback-debug` | `REJECTED_PENDING_REGEN` | Missing/hidden/debug sentinels must avoid text-marked assets and still follow dark UI constraints. |
| `r09-rejected-polish` | `RESERVED_QA_REQUIRED` | Reserved polish sheet now has a deterministic reserved-slot sample in the random QA record. |

Rejected raw sheet hashes recorded before PR06 regeneration:

| Sheet | Rejected raw sheet hash |
| --- | --- |
| `r08-skills-vanguard-berserker` | `004c72185ff7c61063e32f778caacf7bb516ba09` |
| `r08-skills-templar-rogue` | `1d091dda11aff0da575a6cad8959b996d4858292` |
| `r08-skills-arcanist-spellblade` | `4bef671b97b284b0048db5d70cf54a6448989843` |
| `r09-status-damage` | `e11f0334b878f2623bbcad1436828c91c5d1b66e` |
| `r09-quest-zone-profession` | `1fec4c0a9b681d0b508009664daa52c31f58631d` |
| `r09-fallback-debug` | `dc793e96f6b347fae4b9dec6e3df5982830f6a7f` |
| `r09-rejected-polish` | `1e4798300e2555ddbc123ace1871f8a0d9f963bc` |

Rejected R07 raw sheet hashes before this refresh:

| Sheet | Rejected raw sheet hash |
| --- | --- |
| `r07-items-base` | `6c05e4060e0df3fcd9349bba0d06ddd8f22fd3e3` |
| `r07-items-unique-artifact` | `ccea1ef21b180a573f08859dc75a7ff9887e5fef` |
| `r07-items-affix-material` | `c3aaf15b82f94a57084e3d4fc8244107ca9ccae0` |

## Runtime Replacement

The earlier runtime stopgap has been superseded. PR06 now uses the accepted Round 7/8/9 sheet pipeline:

`sheet-plan.yaml -> generated prompt -> raw sheet -> repacked sheet -> contact sheet -> deterministic random QA -> acceptance record -> candidate map -> accepted sprite map -> runtime slice -> manifest`

The live hotbar and inscription surfaces were verified from the packaged PR06 scenario after runtime slicing. The visible player keys are:

| Runtime key | Runtime PNG | Visible surface |
| --- | --- | --- |
| `icon.skill.vanguard.power_strike` | `client/src/main/resources/dark-v1/icons/icon_skill_vanguard_power_strike.png` | Hotbar slot 1, `猛击` |
| `icon.skill.vanguard.shield_bash` | `client/src/main/resources/dark-v1/icons/icon_skill_vanguard_shield_bash.png` | Hotbar slot 2, `盾击` |
| `icon.skill.vanguard.guard_stance` | `client/src/main/resources/dark-v1/icons/icon_skill_vanguard_guard_stance.png` | Hotbar slot 3, `格挡` |
| `icon.skill.templar.holy_light` | `client/src/main/resources/dark-v1/icons/icon_skill_templar_holy_light.png` | `healing_light` inscription, `治疗之印` |
| `icon.skill.templar.holy_shield` | `client/src/main/resources/dark-v1/icons/icon_skill_templar_holy_shield.png` | `iron_shield` inscription, `铁壁之印` |

The backpack/equipment recheck also verified these R07 player-visible item keys from the packaged PR06 scenario:

| Runtime key | Runtime PNG | Visible surface |
| --- | --- | --- |
| `item.long_sword.icon` | `client/src/main/resources/dark-v1/items/item_long_sword_icon.png` | Equipment weapon slot and backpack slot 1. |
| `item.basic_shield.icon` | `client/src/main/resources/dark-v1/items/item_basic_shield_icon.png` | Equipment shield slot and backpack slot 2. |
| `item.chain_mail.icon` | `client/src/main/resources/dark-v1/items/item_chain_mail_icon.png` | Equipment armor slot and backpack slot 3. |
| `item.healing_potion.icon` | `client/src/main/resources/dark-v1/items/item_healing_potion_icon.png` | Backpack slot 4. |

Packaged evidence:

| Surface | Evidence | Result |
| --- | --- | --- |
| hotbar skill icons | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/final_skill_hotbar_crop.png` | `PASS` |
| inscription skill icons | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/final_inscription_crop.png` | `PASS` |
| R07 backpack item icons | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-r07-backpack-live-crop.png` | `PASS` |
| R07 equipment item icons | `build/whitebox/dark-uiux-pr06-status-quest-skill-overview/evidence/dark-uiux-pr06-r07-equipment-live-crop.png` | `PASS` |

## Accepted Replacement Sheets

| Sheet | Accepted git hash | Runtime disposition |
| --- | --- | --- |
| `r07-items-base` | `ead242c2ea8b9a1d0f41887da12b52115cc3bd02` | `PROMOTED` |
| `r07-items-unique-artifact` | `74250eba55348d1d2be5711f5b025d23c6fbc5e4` | `PROMOTED` |
| `r07-items-affix-material` | `d14069f4ea739f1c1caf7dd20cd59053d0450040` | `PROMOTED` |
| `r08-skills-vanguard-berserker` | `30752c26f701563cee40f48b2e325166bdc292ba` | `PROMOTED` |
| `r08-skills-templar-rogue` | `41cf61a4c9a8bda70d97281e91641751b78496d7` | `PROMOTED` |
| `r08-skills-arcanist-spellblade` | `15189fbc5b6b4a7aa0ca7a9baf9fa0b7c71ce053` | `PROMOTED` |
| `r09-status-damage` | `0c0f0a1b5b443f70387543995713d03a072ddf46` | `PROMOTED` |
| `r09-quest-zone-profession` | `442c2cea26d6672830a1f49d3710d656f276f7d4` | `PROMOTED` |
| `r09-fallback-debug` | `0be017e09d378c3072029b6b92a94fb3732e3bdc` | `PROMOTED` |
| `r09-rejected-polish` | `1e4798300e2555ddbc123ace1871f8a0d9f963bc` | `ACCEPTED_RESERVED_NOOP` |

The accepted sprite map is `assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl` with 263 accepted runtime slice records. `r09-rejected-polish` remains a blank reserved no-op sheet with no player-visible cells.

## Prompt Rework

`UI/sprite-sheets/sheet-plan.yaml` now separates visual grammar by family:

| Family | Required grammar |
| --- | --- |
| `icon.skill.*` | Action-command icon: concrete weapon/spell/object plus active impact or motion. |
| `talent.*` | Progression-emblem icon/visual: node badge, crest, or unlock emblem, not a generic rune pack. |
| `icon.status.*` | Compact actor status badge with condition-specific object/material cue. |
| `icon.damage_type.*` | Damage-channel glyph with elemental material, distinct from status badges. |
| `icon.mutation.*` | Elite mutation scar/mark with warped material grammar, distinct from status. |
| `icon.quest.*` | Objective item marker with concrete key/map/seal object language. |
| `icon.profession.*` | Class identity crest readable at profession-scale and small icon sizes. |
| `icon.tree.*` / `zone.*.icon` | Tree branch crest or route landmark, readable in overview contexts. |
| `item.*` | Inventory still-life object with concrete equipment/consumable/relic silhouette and material. |
| `material.*` / `affix.*` | Compact crafting/affix token with distinct physical object grammar, not generic rune marks. |
| `ui.shop.*` | Shop utility symbol with material object language and no text/digits inside the asset. |
| `missing_visual` / `tile.hidden` | Debug/fallback sentinels with no text inside the asset. |

Each player-visible subject must include concrete subject/action/state, material or era detail, small-size outline/readability, and `no text, no people`.

## Deterministic Random QA

Random QA is fixed to seed `dark-uiux-pr06-art-random-qa-v1`.

Sampling rule:

1. Sort by `sheetId + targetKey`.
2. Per sheet sample `max(4, ceil(playerVisibleCells * 15%))`, capped at 12.
3. If the sheet has fewer than 4 player-visible cells, inspect all.
4. Mandatory samples are added on top of the random set.
5. `r09-rejected-polish` has one deterministic reserved-slot sample because it has no player-visible cells.

Generated outputs:

| Artifact | Path |
| --- | --- |
| QA record | `UI/manual-records/dark-uiux-pr06-art-random-qa.json` |
| Multi-size sample sheets | `build/reports/verification/dark-uiux/random-qa/*-random-qa.png` |

Required review sizes:

| Family | Sizes |
| --- | --- |
| skills/talents | `16/24/32/48px` |
| status/damage/mutation | `16/24/32px` |
| quest/zone/tree/difficulty | `12/16/24/32px` |
| profession | `128/48/24px` |
| item icons | `24/32/48/64px` |
| materials/affixes | `16/24/32/48px` |
| shop markers | `16/24/32px` |
| fallback/debug | `16/24/32/48px` |
| rejected polish reserved sample | `32/48/128px` |

Failure criteria:

1. Subject unreadable at required size.
2. Same-family icons collapse into a shared generic shape.
3. Clean vector sticker look.
4. Overbright neon or glassmorphism.
5. Cross-cell bleed.
6. Text, watermark, or label inside the asset.
7. Outline collapses below 32px.

## Current Gate State

| Gate | State |
| --- | --- |
| `darkSpriteSheetLint` subject quality | Implemented; generic player-visible subjects now fail. |
| prompt regeneration | Completed from rewritten `sheet-plan.yaml`. |
| random QA record generation | Completed; 81 deterministic samples across R07/R08/R09 are `PASS`. |
| accepted art promotion | Completed through `scripts/apply_dark_sheet_art_acceptance.py`; raw sheet git hashes are checked before promotion. |
| `spriteSheetMapLint` final slice acceptance | `PASS` with the accepted sprite map and reviewed QA gate. |
| runtime slices from new R07/R08/R09 sheets | Promoted to `client/src/main/resources` through `scripts/slice_spritesheet.py`. |
| final-full art acceptance | `PASS`; rejected/pending player-visible art is zero. |

## Verified Commands

| Command | Result |
| --- | --- |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest` | `PASS`, 59 script/pipeline tests. |
| `python3 scripts/verify_dark_key_registry.py --report build/reports/verification/dark-uiux/dark-key-registry-lint.json` | `PASS`, registry entries = 487. |
| `python3 scripts/verify_sprite_sheet_map.py --check sheet-plan --plan UI/sprite-sheets/sheet-plan.yaml` | `PASS`, subject-quality lint accepted rewritten source subjects. |
| `python3 scripts/generate_sheet_prompt.py --plan UI/sprite-sheets/sheet-plan.yaml --output-dir UI/sprite-sheets/prompts/dark-v1` | `PASS`, prompt index regenerated. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkArtRandomQa` | `PASS`, Gradle default writes 10 R07/R08/R09 sheets and preserves reviewed decisions. |
| `python3 scripts/apply_dark_sheet_art_acceptance.py --input-report build/reports/verification/dark-uiux/pr06-art-candidate-map.jsonl --acceptance UI/manual-records/dark-uiux-pr06-sheet-art-acceptance.json --out assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl` | `PASS`, 10 sheets accepted, 263 records promoted. |
| `python3 scripts/verify_sprite_sheet_map.py --check map --plan UI/sprite-sheets/sheet-plan.yaml --runtime-root client/src/main/resources --report assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl --require-reviewed-qa --report-sheet-ids r07-items-base,r07-items-unique-artifact,r07-items-affix-material,r08-skills-vanguard-berserker,r08-skills-templar-rogue,r08-skills-arcanist-spellblade,r09-status-damage,r09-quest-zone-profession,r09-fallback-debug,r09-rejected-polish` | `PASS`, accepted runtime slices match reviewed QA. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkArtRandomQa resourcePipelineLint` | `PASS`, static resource gates pass. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json -Pktome.darkUiux.artRandomQaRecord=UI/manual-records/dark-uiux-pr06-art-random-qa.json -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md` | `PASS`, final-full coverage is closed with 487 expected keys. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr06-status-quest-skill-overview` | `PASS`, packaged PR06 whitebox app materialized. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot` | `PASS`, golden hashes rebaselined to accepted R07/R08/R09 resources after visual inspection. |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged` | `PASS`, changed preflight/full owner route succeeded. |
| Computer Use + `scripts/capture-macos-app-window.sh` | `PASS`, packaged screenshots captured for hotbar, status fold, quest log, inscription, and validation compact action. |
