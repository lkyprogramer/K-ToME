# Dark UI/UX PR07 Packaged App Manual Record

## Summary

| Field | Value |
| --- | --- |
| result | `FAIL_UI_DIRECTOR_REVIEW_2026_05_24` |
| date | 2026-05-24 |
| source doc | `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md` |
| flow doc | `docs/computer-use-whitebox-flow.md` |
| scenario | `dark-uiux-pr07-final-ui` |
| manual record | `UI/manual-records/dark-uiux-pr07-packaged-app.md` |
| final screen index | `UI/manual-records/dark-uiux-pr07-final-all-screens.md` |
| review standard | Packaged app + Computer Use whitebox, judged against `UI/UI-demo-new.png` as UI/art director quality bar. |

This is a real packaged app whitebox run. The app was rebuilt/materialized again on 2026-05-24, launched from the packaged runtime-app bundle, and recaptured with target-window screenshots plus metadata/sha256 sidecars. Computer Use was attempted for the current packaged libGDX window, but `get_app_state` returned `cgWindowNotFound` / timeout and a direct click call timed out; persisted evidence therefore uses the documented `capture_mode=macos-window-id` fallback from `docs/computer-use-whitebox-flow.md`. The result is still a failure: the current packaged UI remains below the `UI/UI-demo-new.png` visual quality bar. The latest renderer/staging polish pass improved equipment socket scale, real equipped item icon prominence, prop staging, ritual/bonfire atmosphere, cooler floor material, collapsed-room corner masking, pre-actor terrain glaze, terrain tile bleed, and validation-only non-rectangular stage-room preference. The latest packaged shell evidence is visibly better than the original PR07 shell, but still reads below director-level final art quality. The earlier `F9` / `V` blocker was rechecked in a prior packaged pass after compact validation-panel polish: the PR07 fast action path was reachable and the selected action plus evidence summary path were visible, but the current CUA session could not re-drive that path because the plugin could not bind or click the window. The remaining PR07 blocker is the overall art-direction / UI-quality gap, with CUA operability recorded as a verification limitation rather than a pass.

## Execution

| Item | Result |
| --- | --- |
| package command | `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr07-final-ui` |
| package result | `PASS` |
| scenario launch script | `build/whitebox/dark-uiux-pr07-final-ui/launch-packaged-app.sh` |
| scenario pid | `64638`, `66362`, `66741`, `67261`, `68716`, `69373`, `80782`, `81147`, `88046`, `88880`, `90145`, `41060`, `52817`, `54924`, `56323`, `57225`, `59611`, `60101`, `61858` across fresh packaged sessions |
| direct packaged menu pid | `53815` |
| controlled UiErrorScreen pid | `55187` |
| isolated scenario runtime home | `build/whitebox/dark-uiux-pr07-final-ui/runtime-home` |
| direct menu runtime home | `build/whitebox/dark-uiux-pr07-final-ui/runtime-home-menu-20260524` |
| controlled UiErrorScreen runtime home | `build/whitebox/dark-uiux-pr07-final-ui/runtime-home-ui-error-20260524` |
| app executable sha256 | `939fc458beb001ffc6ce1b053272b545cbcb001d858285f46574665579d9fe85` |
| Computer Use target | `build/whitebox/dark-uiux-pr07-final-ui/runtime-app/K-ToME.app`, `client/build/release/K-ToME.app`, and `build/whitebox/dark-uiux-pr07-final-ui/runtime-app-ui-error/K-ToME.app`; current `get_app_state` listed the running app but returned `cgWindowNotFound` / timeout for the libGDX window, and `click` timed out, so final persisted evidence uses the documented target-window capture fallback. |
| contact sheet | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-20260524-contact-sheet.png` |
| latest shell screenshot | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-demo-new-shell-polish7-truecolor.png`, metadata target pid `61858`, sha256 `e4bded8b9c6cb3346a7395ad43ad3a62ddb94d0577a675661cbde11d36744a4e` |

## Scenario Contract

| Item | Repo-relative path or value |
| --- | --- |
| scenario id | `dark-uiux-pr07-final-ui` |
| expected command | `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr07-final-ui` |
| expected launch script | `build/whitebox/dark-uiux-pr07-final-ui/launch-packaged-app.sh` |
| expected runbook | `build/whitebox/dark-uiux-pr07-final-ui/cua-runbook.md` |
| expected evidence plan | `build/whitebox/dark-uiux-pr07-final-ui/expected-evidence.json` |
| expected runtime home | `build/whitebox/dark-uiux-pr07-final-ui/runtime-home` |
| expected evidence dir | `build/whitebox/dark-uiux-pr07-final-ui/evidence` |
| expected app log | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-final-ui-app.log` |

## Required Evidence Slots

| Surface group | Expected evidence path | Status |
| --- | --- | --- |
| main menu | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-main-menu.png` | `CAPTURED_FAIL_POLISH` |
| character creation | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-character-creation.png` | `CAPTURED_MAIN_MENU_CREATION_STATE` |
| continue unavailable | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-continue-unavailable.png` | `CAPTURED_FAIL_PROMINENT_ERROR_BAR` |
| validation setup | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-validation-setup.png` | `CAPTURED_PARTIAL_NOT_PR07_ROW` |
| validation overlay | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-validation-overlay.png` | `CAPTURED_ACTION_AND_EVIDENCE_VISIBLE_IN_PRIOR_PACKAGED_PASS_CURRENT_CUA_DRIVE_UNAVAILABLE` |
| ui-demo-new shell | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-demo-new-shell-polish7-truecolor.png` | `RECAPTURED_61858_FAIL_FINAL_ART_DIRECTION_AFTER_TERRAIN_BLEED_STAGE_GLAZE_POLISH` |
| inventory workbench | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inventory-workbench.png` | `CAPTURED_READABLE_FAIL_FINAL_POLISH` |
| inscription shop and replacement modal | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inscription-shop.png` + `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-shop-replacement-modal.png` | `CAPTURED_FAST_ACTION_REACHABLE` |
| talent assign and active slot modal | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-talent-assign.png` + `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-active-slot-modal.png` | `CAPTURED_FAST_ACTION_REACHABLE` |
| status / quest / skill | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-status-quest-skill-overview.png` | `CAPTURED_PARTIAL` |
| combat / inspect / route / stat / reward | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-combat-decision.png` + `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-look-inspect.png` + `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-world-route-selection.png` + `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-stat-assign.png` + `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-reward-frontstage.png` | `CAPTURED_WITH_FAST_ACTION_ROUTE_STAT_REWARD_RECHECKED` |
| map / telegraph | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-map-layer-telegraph.png` | `CAPTURED_MAP_LAYER_ONLY_TELEGRAPH_NOT_PROVEN` |
| victory / defeat | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-outcome-victory.png` + `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-outcome-defeat.png` | `VICTORY_HISTORICAL_CAPTURE_DEFEAT_NOT_RECAPTURED_IN_THIS_PASS` |
| loading / runtime error / UiErrorScreen | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-runtime-loading.png` + `dark-uiux-pr07-runtime-error.png` + `dark-uiux-pr07-ui-error-screen.png` | `UI_ERROR_CAPTURED_RUNTIME_LOADING_ERROR_NOT_REACHED` |
| accessibility / settings | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-accessibility-settings.png` | `NOT_REACHED` |

Each captured screenshot has `.metadata.txt` and `.sha256` sidecars. The captured metadata uses `capture_mode=macos-window-id` and binds the evidence to the packaged app window pid.

## UI / Art Director Findings

| Area | Verdict | Finding |
| --- | --- | --- |
| Overall parity with `UI/UI-demo-new.png` | `FAIL` | The packaged shell is improved but still below the reference. The reference has a richer authored dungeon stage, stronger torch-lit wall depth, more organic fog-of-war composition, denser right companion content, and a more coherent bronze/iron material hierarchy. The packaged shell now has stronger equipment slots, visible props, ritual/bonfire atmosphere, map-stage masking, pre-actor terrain glaze, and terrain bleed, but the room still reads too orthogonal/repetitive and the right companion panel still reads less finished. |
| Background atmosphere | `FAIL` | The latest renderer pass reduced the pure-black empty-stage problem and exposed more stone texture, but the background still lacks the reference's authored rubble, directional light, organic darkness, and room storytelling. |
| UI background / panel treatment | `FAIL` | Menu, validation setup, inventory, inspect, and error surfaces share dark chrome, and empty sockets now have more intentional interior treatment. Compared with the reference, several panel bodies still rely on thin translucent fills and low-contrast silhouettes. |
| Overall UI quality | `FAIL` | Inventory and talent screens are operable and mostly readable, but the total product still feels short of final art direction: repeated slot frames, sparse shell composition, and underpowered room detail keep it below the requested UI/UX acceptance bar. |
| Validation fast overlay | `PARTIAL_PASS_WITH_CURRENT_CUA_LIMITATION` | In the prior packaged scenario pass, `F9` / `V` entered the `PHASE4_V4_FAST` action path and `Right` / `Return` executed PR07 actions. The packaged screenshot showed the validation panel title, selected action, and evidence summary root path, so the action was no longer only inferable from logs. In the current pid `61858` pass, Computer Use could not bind or click the libGDX window, so this path was not re-driven; this does not change the overall UI/art-direction failure. |
| Layout overlap | `PARTIAL_PASS` | Captured menu, setup, shell, inventory, talent, inspect, combat, reward, and UiErrorScreen screens did not show catastrophic text overlap at 1280x800. Several surfaces remain low contrast or crowded, but the primary failure is quality and coverage, not a single overlap bug. |
| Old-style / temporary residue | `FAIL_RISK` | No ASCII renderer appeared in captured surfaces, and the controlled UiErrorScreen uses dark chrome. However, the sparse grid room, repeated placeholder-like slots, and low-richness panels still read as pre-final residue next to the reference. |

## Captured Evidence

| Evidence | Notes |
| --- | --- |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-main-menu.png` | Direct packaged menu capture. Dark chrome exists, but the screen is sparse and has a prominent red continue-load failure bar. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-character-creation.png` | Main-menu profession/race creation state capture. Text remains readable, but the surface lacks the reference's rich art direction. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-validation-setup.png` | Validation setup captured, but not on the `dark-uiux-pr07-final-ui` row. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-validation-overlay.png` | Fast action path reached the PR07 evidence summary; the right operation panel now shows the selected evidence action and repo-redacted evidence path in the packaged screenshot. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-demo-new-shell-polish7-truecolor.png` | Main blocker evidence, recaptured from pid `61858`: the packaged shell is improved but still does not meet `UI/UI-demo-new.png` visual quality. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inventory-workbench.png` | Inventory is readable and structured, but material depth and slot polish remain below final bar. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-talent-assign.png` | Talent assignment is readable and functionally structured; not enough to offset shell/background failure. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-active-slot-modal.png` | Active slot modal is reachable through PR07 fast action plus talent panel entry. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-look-inspect.png` | Inspect modal is bounded and readable, but panel finish remains weak. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-combat-decision.png` | Captures target/action context without a center-blocking modal; still below final polish. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-world-route-selection.png` | Route selection is reachable through PR07 fast action and shows readable cards, but remains low-polish against the reference bar. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-stat-assign.png` | Stat assign is reachable through PR07 fast action and shows bounded cards with no catastrophic overlap. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-reward-frontstage.png` | Reward/frontstage is reachable through PR07 fast action; the capture remains visually sparse and below final polish. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-error-screen.png` | Controlled invalid scenario startup uses dark error chrome and does not show old red/green placeholder styling. |
| `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-20260524-contact-sheet.png` | Review contact sheet comparing `UI/UI-demo-new.png` with current packaged captures. |

## Residual Risk

1. PR07 should remain blocked until the packaged shell/background/panel quality is raised to the `UI/UI-demo-new.png` bar and rerun.
2. The validation fast overlay path is minimally self-describing in prior packaged evidence for the selected action and evidence summary, but a later final pass should still verify the full action list across PR07 preparation actions after the visual-quality work is complete and after Computer Use can bind the packaged libGDX window, or with an explicitly accepted target-window fallback procedure.
3. The direct menu run displayed a continue-load failure state despite the isolated `user.home`; that is acceptable evidence for continue-unavailable styling but should not be treated as a clean first-run menu pass.
4. Victory/defeat, runtime loading/error, settings/accessibility, and the 60-minute fatigue audit remain lower priority than the blocking visual-quality failure.
