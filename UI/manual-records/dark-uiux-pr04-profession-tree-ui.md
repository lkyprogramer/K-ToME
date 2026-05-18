# Dark UI/UX PR-04 Profession Tree UI Manual Record

## Scope

| Field | Value |
| --- | --- |
| PR | `dark-uiux-pr04-profession-tree-ui` |
| canonical reference image | `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png` |
| canonical reference prompt | `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.prompt.txt` |
| canonical reference sha256 | `f586d29586cd8b7b60c3eadc5f1a06dfac9ae28e2b5bd2b88a21fa64c78a85b6` |
| generated runbook | `build/whitebox/dark-uiux-pr04-profession-tree-ui/cua-runbook.md` |
| generated runbook sha256 | `c29c53947412bb66958854fc974a9e43541131fd81282b0a777afa061f617ff6` |
| generated expected evidence | `build/whitebox/dark-uiux-pr04-profession-tree-ui/expected-evidence.json` |
| generated expected evidence sha256 | `6f0fad6f49173ece86e89fec9c990480cd14878459d8ff3f0582b78a5042df72` |
| runtime home | `build/whitebox/dark-uiux-pr04-profession-tree-ui/runtime-home` |
| evidence dir | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence` |
| app executable | `client/build/release/K-ToME.app/Contents/MacOS/K-ToME` |
| app executable sha256 | `e3d739c88bee51e9bdbbcd20df2a219824d853506daaaae5cde105caa27bb4a9` |

## Whitebox Status

| Item | Status |
| --- | --- |
| automated PR04 golden whitebox | `RUN` via `:client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr04 talent assign golden evidence writes canonical artifacts"` |
| packaged whitebox materialization | `RUN` via `:client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-profession-tree-ui` |
| packaged interactive CUA screenshots | `RUN` for Talent Assign panel, compact viewport, and post-close right companion restoration; active-slot packaged keyboard capture is not used because scripted macOS key routing did not reliably invoke the secondary validation action |
| PR04 contract verdict | `PASS` for PR04 panel structure, upstream active-slot snapshot ownership, active-slot modal model, compact viewport readability, and post-close right companion restoration |
| strict pixel identity | `NOT REQUIRED FOR PR04`; remaining font/frame/icon exactness stays under PR06 resource rebaseline and PR07 final all-screen audit |
| residual risk | packaged active-slot CUA capture needs a more reliable automation hook or manual operator pass if a future gate requires packaged-only active-slot screenshots |
| follow-up owner | PR06 owns formal skill/tree icon and frame rebaseline; PR07 owns final all-screens audit |

## Automated PR04 Golden Evidence

| PR04 label | Generated screenshot path | Golden index hash | Screenshot sha256 |
| --- | --- | --- | --- |
| `dark-uiux-pr04-talent-assign-panel-start` | `client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-talent-assign-panel-start.png` | `e060404ad32426013ead37adecd69c762dcaa8a03c02ec24b155c6bc52748da4` | `671caa5fa8de6f72550488915471ab5a3606ddfbad80d1c90ddb871ee6ab4fb2` |
| `dark-uiux-pr04-active-slot-choice` | `client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-active-slot-choice.png` | `fc4b5895a5a17a7a093bcc83be2da2eb5c747e3e47da639ed4df1eca6a979c4f` | `f039f27999e4fe1d70a516143f9d727d97af294d9ec93dffeb284a09ead8486d` |

## Packaged CUA Evidence

| PR04 label | Screenshot path | Status | Screenshot sha256 | Window evidence |
| --- | --- | --- | --- | --- |
| `dark-uiux-pr04-talent-assign-panel-start` | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr04-talent-assign-panel-start.png` | `RUN` | `eeef76c72ca348f287b7623f7aee53745247f4a41c525ad6b1331bc8ebda0fe1` | `window_pid=85845`, `window_bounds=80,38,1280,828` |
| `dark-uiux-pr04-active-slot-choice` | `client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-active-slot-choice.png` | `RUN via automated golden`; packaged scripted-key capture excluded | `f039f27999e4fe1d70a516143f9d727d97af294d9ec93dffeb284a09ead8486d` | upstream snapshot requirement asserted by `GoldenScreenshotHarnessTest` and `ValidationScenarioRegistryTest` |
| `dark-uiux-pr04-talent-assign-min-window-log-visible` | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr04-talent-assign-min-window-log-visible.png` | `RUN` | `497ecd45e7b674d2e2c167db3aff0fcd08998b53a3c13c0c16275e19f1cd9e44` | `window_pid=85845`, `window_bounds=80,38,1000,700` |
| `dark-uiux-pr04-right-companion-coexistence` | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr04-right-companion-coexistence.png` | `RUN` | `01a71d139ca7d0b3ca8360e9a18ab017ee492545451ff21d2a284425ef211816` | `window_pid=85845`, `window_bounds=80,38,1280,840` |

## Validation Run

| Gate | Command | Result |
| --- | --- | --- |
| PR04 golden active-slot regression | `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr04 talent assign golden evidence writes canonical artifacts"` | `PASS`; active-slot golden differs from panel-start and renders the modal |
| PR04 packaged materialization | `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-profession-tree-ui` | `PASS`; generated runbook uses PR04 dark labels and post-close right companion wording |
| packaged CUA capture | `build/whitebox/dark-uiux-pr04-profession-tree-ui/launch-packaged-app.sh` + `scripts/capture-macos-app-window.sh` | `RUN`; primary/min/right companion screenshots captured from packaged app, active-slot captured by automated golden because scripted secondary action was not stable through macOS key routing |
| remaining focused/client/owner gates | see final PR validation summary | `PENDING` until final rerun |

All Gradle commands in this record were run after `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env` and were run serially.

## Self-Audit

| Item | Answer |
| --- | --- |
| base ref / target branch | based on local `main`, branch `codex/dark-uiux-pr04-profession-tree-ui` |
| reference fidelity | `PASS FOR PR04 CONTRACT`; exact pixel/font/resource parity remains scoped to PR06/PR07 |
| presentation authority | `TalentSidebarPresenter` owns `TalentAssignPanelModel`; renderer consumes typed model fields and does not parse marker text |
| active slot trigger authority | `RenderUiStateSnapshot.activeTalentSlotChoiceRequirement` is produced by `game`; `InputHandler` no longer infers slot-choice legality from client-side category/slot fullness |
| tree visual model | list tree only; no grid, tier band, column node graph or card layout |
| state marker mode | marker-text-only; `stateIconKey=null` on PR04 rows |
| prerequisiteConnectorMode | `implemented` from typed prerequisite projection |
| modal state machine | implemented as `1-4/R/Esc`; number keys outside active-slot modal remain isolated |
| detail pane hierarchy | hero icon, header, rank/cost, prerequisite, current detail, next preview, actions |
| footer vs actions | footer remains panel-level navigation; talent-specific commands stay in `ACTIONS` block |
| race tree behavior | runtime tree list is rendered without client filtering |
| right companion coexistence | Talent Assign is full-screen; right companion and bottom log are verified after leaving Talent Assign |
| mousePointerMode | `deferred`; keyboard-only contract is covered by focused input test |
| PR06 rebaseline | PR04 reference-crop Vanguard evidence icons are allowed only for canonical Talent Assign fidelity; full skill/tree icon and frame resources remain PR06 rebaseline owner |
| PR07 final audit | PR04 dark labels remain listed for final all-screens audit |

## Removal / Iteration Plan

| Item | Current decision | Owner | Exit condition |
| --- | --- | --- | --- |
| PR04 reference-crop Vanguard icons | kept only for PR04 Talent Assign fidelity evidence | PR06 | formal skill/tree icon rebaseline replaces temporary reference crops |
| legacy `talent-sidebar-*` labels | alias only; canonical labels are `dark-uiux-pr04-talent-assign-*` | PR07 | final screen coverage audit no longer references aliases as primary evidence |
| full reserve skill list modal | not added; active-slot modal exposes the `R` reserve action row only | future UI owner | reserve management gets a dedicated PR/spec |
| packaged active-slot CUA automation | not treated as PR04 blocker because automated golden and session tests cover the modal contract | PR04 follow-up only if required by gate | a reliable startup action or operator-driven CUA pass captures packaged active-slot modal |

## Current Visual Gaps Against Reference

| Gap | Status | Repair Direction |
| --- | --- | --- |
| exact skill icons / hero icon | matched for canonical Vanguard rows through PR04 reference-crop evidence keys | PR06 formal full icon rebaseline |
| exact frame/corner ornament density | close enough for PR04 contract, not strict-pixel exact | PR06/PR07 dark UI frame polish |
| exact font rasterization and weight | not exact | PR07 final all-screens visual polish |
| breakpoint preview copy | typed gameplay truth uses `击退 +1`; it does not fake the mock reference's radius copy | keep gameplay authority unless schema changes |
