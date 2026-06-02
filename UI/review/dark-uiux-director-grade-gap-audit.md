# Dark UI/UX PR-08 Director-Grade Gap Audit

> Date: 2026-05-26
> Scope: PR-08 exploratory lines from `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md`
> Verdict: `resource-gap` is the primary root cause; `lighting-gap` and overlay budget are secondary blockers.

## Direct Conclusion

PR-08 should not continue by adding more renderer micro-rectangles. The subtractive spike shows that disabling the decorative overlay stack makes the map clearer, but it does **not** make the first screen approach `UI/UI-demo-new.png`.

The remaining problem is structural: the runtime first screen is still built from one repeated ruins floor tile, one repeated ruins wall tile, a hard square grid, weak non-rectangular darkness, and UI chrome that reads as separate panels. The next large change should be an asset/compositor reset: target comp first, sliceability proof second, then PR-08 owner-routed asset migration and a smaller renderer compositor.

The final PR-08 evidence chain and locked implementation direction are consolidated in `UI/review/dark-uiux-pr08-evidence-and-direction.md`.

## Evidence Set

| Evidence | Path | Status |
| --- | --- | --- |
| Quality target | `UI/UI-demo-new.png` | Reference only, not mapping truth |
| Current runtime full screen | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | Current state |
| Current runtime map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | Current state |
| Subtractive full screen | `UI/review/dark-uiux-pr08-exploration/subtractive-ui-demo-new-parity-1672x941.png` | Exploratory, not golden |
| Subtractive map crop | `UI/review/dark-uiux-pr08-exploration/subtractive-ui-demo-new-map-stage-crop.png` | Exploratory, not golden |
| Ruins first-screen contact sheet | `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png` | PR-02-2 upstream resource evidence |

## Subtractive Spike

Temporary local patch only; it was removed after capture. The spike disabled:

1. `drawVisibleRoomFoundationGlaze`
2. `drawCellMaterial`
3. `renderWarmMapOverlay`
4. `drawVisibleRoomGridDissolve` indirectly because it is called from `drawVisibleRoomAtmosphere`

It kept bitmap terrain placement, props, actors, loot markers, telegraph/selection, fog veils, shell chrome, right panel and bottom HUD.

| Label | Current hash | Subtractive hash | Interpretation |
| --- | --- | --- | --- |
| `ui-demo-new-parity-1672x941` | `f6f94cbb3a90df90e309d6e8cbefecf02f0d43c990e323ff5a1537188911aa78` | `933e0b0b080c26f2b3fe6697dfaeac180585df55d612c57ceac4be074515f561` | Full-screen visual changed only in map-stage path |
| `ui-demo-new-map-stage-crop` | `070f834b41a5db14c103a46d23d47da2662afc33400c0fda75c761ef5a9fa84d` | `0f610cf2f0a51dc554672bde261cb50ba824b7fc85aee4b5a7d88c3089ed67fc` | Map crop exposes raw tile repetition and hard grid |
| `ui-demo-new-right-panel-grid` | `c8bcf38d64bfeb2648b5d26535dea2f7ee4220e024860846fb0159d3f544b5d7` | unchanged | Right panel gap is independent from map overlay |
| `ui-demo-new-bottom-deck-no-command-hints` | `fbb282fdd9e32821e3bd7b4d08e9df76b70f32d59e8c5bd625171ed72d646b4f` | unchanged | Bottom deck gap is independent from map overlay |

Root-cause verdict: `resource-gap`.

Reason: overlay removal improves clarity but leaves the map far below reference quality. The floor/wall read as repeated 32px squares, the room silhouette remains a rectilinear grid, and lighting loses atmosphere. So the issue is not "good bitmap buried by overlay"; it is "insufficient resource/composition foundation plus overgrown overlay trying to compensate."

## Overlay Baseline And Budget

Dynamic diagnostic sample: 12x8 visible map, 96 visible cells, cell size 32px. This diagnostic was run through temporary test instrumentation and then removed.

| Metric | Current measured value | PR-08 target budget | Result |
| --- | ---: | ---: | --- |
| `overlayFunctionCount` | 22 major decorative/material functions | <= 8 preferred before close | fail |
| `warmOverlaySubpassCount` | 20 calls inside `renderWarmMapOverlay` | <= 4 | fail |
| `materialRectCountPerVisibleCell` | 16.375 | <= 2 | fail |
| `gridDissolveEnabled` | `true` | `false` on accepted runtime path | fail |
| `totalRectDraws` | 3784 | no fixed target, should shrink after resource migration | high |
| `assetDraws` | 212 | N/A | evidence only |

Rect draw count by layer from the same diagnostic:

| Layer | Rect draws |
| --- | ---: |
| `MAP_CELL_MATERIAL` | 1572 |
| `MAP_WARM_OVERLAY` | 1247 |
| `SHELL_RIGHT_PANEL` | 637 |
| `MAP_STAGE_FRAME` | 136 |
| `SHELL_NAV_RAIL` | 70 |
| `SHELL_BOTTOM_ACTION_DECK` | 39 |
| `SHELL_BOTTOM_HERO` | 38 |
| `SHELL_BOTTOM_LOG_DECK` | 23 |
| `MAP_PLAYER_INDICATOR` | 13 |
| `SHELL_OUTER_FRAME` | 6 |
| `MAP_ACTORS` | 2 |
| `BACKGROUND` | 1 |

## Gap Matrix

| Area | Verdict | Evidence | Blocking problem | Direction |
| --- | --- | --- | --- | --- |
| `map-stage` | `blocking gap` | Current and subtractive map crops | Map reads as tiled grid inside a framed viewport, not as an authored dungeon stage | Build target comp and reduce renderer to compositor |
| `tiles/walls` | `blocking gap` | `r02-ui-demo-ruins-tiles-contact.png`, subtractive crop | One floor and one wall key cannot carry director-grade map material at runtime scale | PR-08 must either reroll PR-02-2 cells under supersession or add approved PR-08 variants without schema drift |
| `lighting/dark field` | `blocking gap` | Current crop muddies; subtractive crop loses atmosphere | Overlay adds atmosphere but also haze; removing it exposes flat black surroundings | Need explicit light/dark layer contract after target comp; do not restore broad amber haze |
| `right panel` | `blocking gap` | Current full screen and unchanged subtractive hashes | Equipment, inscription and backpack areas are data-correct but still read as stacked UI regions rather than one character workbench | Probe existing PR-02/03 chrome first; generate chrome only if sliceability says current resources cannot carry it |
| `bottom deck` | `blocking gap` | Current full screen and unchanged subtractive hashes | Hero/action/log deck still feels like multiple panels; text density and panel boundaries compete | Treat as bottom console resource/compositor family, not map overlay work |
| `text/readability` | `partial` | Current full screen | Chinese text remains readable in places, but material/transparency and dense panel borders reduce hierarchy | Target comp review must include Chinese long-line and hotkey stress cases |
| `resource ownership` | `blocking before mutation` | `UI/sprite-sheets/key-registry.yaml`, `owner-contracts/pr02-2-owner-keys.yaml` | `tileset.ruins.ground_01`, `tileset.ruins.wall_01`, `actor.vanguard`, `prop.stairs.down` are PR-02-2 owner keys today | PR-08 must record supersession and rollback before touching these keys |

## Resource Ownership Finding

The four first-screen resources named by PR-08 are currently PR-02-2 owner scope:

| Target key | Current owner | Current sheet |
| --- | --- | --- |
| `tileset.ruins.ground_01` | `PR-02-2` | `r02-ui-demo-ruins-tiles` |
| `tileset.ruins.wall_01` | `PR-02-2` | `r02-ui-demo-ruins-tiles` |
| `actor.vanguard` | `PR-02-2` | `r03-ui-demo-actor-props` |
| `prop.stairs.down` | `PR-02-2` | `r03-ui-demo-actor-props` |

Therefore PR-08 has only two safe routes:

1. `PR-08 supersession`: record why PR-02-2 baseline is superseded, define rollback, add PR-08 owner coverage and reroll/migrate resources.
2. `PR-02-2 reroll only`: keep owner, sheetId, targetKey and output path stable; do not claim PR-08 close evidence.

For the planned director-grade reset, route 1 is the coherent direction. Mixing PR-08 close evidence with silent PR-02-2 reroll would create owner drift.

## Director Rubric For Next Step

These are derived from `UI/ART_STYLE_BIBLE.md` plus gameplay readability constraints. A target comp or runtime crop is accepted only if all blocking rows pass.

| Rubric | Source | Pass condition |
| --- | --- | --- |
| Map dominance | `UI/PLAN.md`, PR-08 §1 | Center map is the first visual read, not right panel or bottom deck |
| Worn stone material | `UI/ART_STYLE_BIBLE.md` §2/§4/§7.2 | Floor/wall look authored at 32px, not like repeated debug tiles |
| Non-rectangular dark field | PR-08 §6 | Darkness frames the room without becoming a second visibility authority |
| Actor/loot/telegraph priority | PR-08 §6 | Gameplay markers remain clearer than material and fog |
| Unified UI chrome | `UI/ART_STYLE_BIBLE.md` §4/§7.1 | Right panel and bottom deck share forged iron / worn stone language |
| Text safety | `UI/ART_STYLE_BIBLE.md` §3/§4 | Chinese labels, numbers and hotkeys are not buried by texture |
| No baked text | `UI/ART_STYLE_BIBLE.md` §5 | Images contain no Chinese, English, digits or shortcut labels |
| Sliceability | PR-08 §4 | Every visual family maps to sheet cell, key, consumer, test and fallback without new schema |
| Owner clarity | PR-08 §5 | PR-02-2 resources are either untouched or explicitly superseded by PR-08 |
| Package parity | `UI/pr/development-governance.md` §3 | Final close includes packaged whitebox, not debug golden only |

## Decision For The Large Refactor

Recommended direction:

1. Do **not** do a pure overlay cleanup PR. It will make the map clearer but visually flatter and still below director grade.
2. Do **not** continue adding renderer material rectangles. Current budget is already far past PR-08 target and makes feedback slow.
3. Do PR-08 as an asset/compositor reset:
   - accepted target comp with 32px tile-truth inset;
   - PR-08 supersession for the PR-02-2 first-screen ruins cells;
   - resource readiness report for each resource family;
   - renderer layer order reduced to `base stage art -> semantic tile/floor/wall -> deterministic visibility fog -> gameplay markers -> focus/selection -> UI chrome`;
   - focused tests only for layer order and marker readability, with golden/packaged whitebox after visual acceptance.

## Validation Actually Run

Commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
KTOME_DARK_UIUX_PR08_SUBTRACTIVE_SPIKE=1 ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

Result: expected failure after writing exploratory PNGs, because the subtractive spike changed `ui-demo-new-parity-1672x941`, `ui-demo-new-parity-1280x800` and `ui-demo-new-map-stage-crop` hashes. The screenshots were copied to `UI/review/dark-uiux-pr08-exploration/`; no golden baseline was accepted from this run.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

Result: passed, restoring canonical PR-02-1 build evidence after the exploratory run.

Temporary diagnostic commands also ran and intentionally failed to print overlay counts. The diagnostic code was removed after collecting the counts. No production or test code changes remain from the diagnostics.

## Known Limits

1. This audit only establishes root cause. Later target comp and family-pack evidence are indexed by `UI/review/dark-uiux-pr08-evidence-and-direction.md`.
2. This audit does not update PR-08 owner coverage wiring; that belongs to the later resource migration slice.
3. This audit does not prove packaged app parity. It only determines the refactor direction before the heavy PR-08 work starts.
