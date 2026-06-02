# Dark UI/UX PR-08 Director-Grade Target Comp Record

> Date: 2026-05-26
> Updated: 2026-05-27
> Scope: `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md` §4
> Status: `family-pack-accepted`

## Direct Conclusion

Target comp attempts 1, 2 and 3 are useful references, but none is accepted as a standalone resource authority. Attempt 3 is accepted only as mood/layout reference. The resource-family target pack is now accepted as the PR-08 direction evidence.

The exploration result in `UI/review/dark-uiux-director-grade-gap-audit.md` selects the PR-08 direction: asset/compositor reset with formal PR-08 supersession for first-screen ruins resources if those resources are rerolled or migrated.

Attempt 3 resolves the baked-text and broad composition problems enough to act as the current mood/layout reference, but it still fails as resource authority. Sampling 32px cells from the monolithic comp mixes semantic tile material with light, poison, props, wall edges and black-field compositor content.

The accepted direction is resource-family-first: floor, wall, compositor and UI chrome must be separated before runtime integration.

The complete evidence index, rejected directions, accepted direction matrix and implementation order are consolidated in `UI/review/dark-uiux-pr08-evidence-and-direction.md`.

## Required Artifacts

| Artifact | Path | Status |
| --- | --- | --- |
| First-screen target comp | `UI/targets/dark-uiux-director-grade-target-1672x941.png` | attempt 1 generated; needs reroll |
| 32px tile-truth inset | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth.png` | attempt 1 generated; failed sliceability |
| Attempt 1 prompt | `UI/targets/dark-uiux-director-grade-target-1672x941.prompt.txt` | available |
| Attempt 2 target comp | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt2.png` | rejected; baked text |
| Attempt 2 tile-truth inset | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth-attempt2.png` | rejected; repeated sampled content |
| Attempt 2 prompt | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt2.prompt.txt` | available |
| Attempt 3 target comp | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt3.png` | directional reference; not accepted |
| Attempt 3 tile-truth inset | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth-attempt3.png` | rejected; sampled crop is not clean resource truth |
| Attempt 3 prompt | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt3.prompt.txt` | available |
| Resource-family target pack | `UI/targets/dark-uiux-director-grade-target-family-pack.md` | accepted for direction |
| Family floor sheet | `UI/targets/dark-uiux-director-grade-target-family-floor-32px.png` | accepted for direction |
| Family wall sheet | `UI/targets/dark-uiux-director-grade-target-family-wall-32px.png` | accepted for direction |
| Family repeat sheet | `UI/targets/dark-uiux-director-grade-target-family-floor-wall-repeat.png` | accepted for direction |
| Family map compositor | `UI/targets/dark-uiux-director-grade-target-family-map-compositor.png` | accepted for direction |
| Family UI chrome | `UI/targets/dark-uiux-director-grade-target-family-ui-chrome.png` | accepted for direction |
| Family prompt set | `UI/targets/dark-uiux-director-grade-target-family-prompt.txt` | recorded |
| Gap audit | `UI/review/dark-uiux-director-grade-gap-audit.md` | available |
| Exploration screenshots | `UI/review/dark-uiux-pr08-exploration/` | available |

## Stop Rule

This record closes the exploration direction, not runtime integration permission.

Do not touch `TileRenderer.kt`, `DemoShellRenderer.kt`, `sheet-plan.yaml`, key registry, manifests, golden hashes, or packaged whitebox for PR-08 runtime integration until all of the following are true:

1. The family pack status remains `accepted-for-direction`.
2. Every resource-family row below is `accepted for direction` or explicitly `deferred`.
3. PR-08 owner coverage and rollback rows are added before any superseded key migration.
4. Resource readiness and focused renderer tests are wired before any golden update.
5. No accepted row requires baked text, full-screen paintover, atlas/region schema, or a new manifest schema.

## Director Verdict

| Field | Value |
| --- | --- |
| Verdict | `accepted-for-resource-family-direction` |
| Reason | Monolithic target comps remain evidence-only, but the family pack proves PR-08 can proceed as a resource-family-first reset: clean floor/wall 32px sheets, repeat contact sheet, compositor reference and reusable UI chrome are available without baked text, full-screen paintover, atlas/region schema or new manifest schema. |
| Reference target | `UI/UI-demo-new.png` |
| Current runtime evidence | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` |
| Current map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` |

## Attempt 1 Result

| Field | Value |
| --- | --- |
| Target comp path | `UI/targets/dark-uiux-director-grade-target-1672x941.png` |
| Target comp sha256 | `b5273da84dfbd0bc6f31ef923bb878924d407d122d430887b8ed7a98e4f80dcd` |
| Tile-truth inset path | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth.png` |
| Tile-truth inset sha256 | `addffb395be0568c4256dc968019602086c0a58ca7629cb974d570043115dd6c` |
| Prompt path | `UI/targets/dark-uiux-director-grade-target-1672x941.prompt.txt` |
| Prompt sha256 | `098d29e217cca4a4d958329313831c30c63d8c82dcfb87376849556c729158d6` |

Positive findings:

1. The overall material direction is close to the desired forged iron / worn stone / ember-lit mood.
2. No obvious baked Chinese text, English text, hotkey labels or manifest keys are visible.
3. The right panel and bottom HUD read as a more coherent dark fantasy workbench than the current runtime.

Blocking findings:

1. The center map is not a K-ToME runtime-sliceable 32px orthographic tile plan. It reads as a hand-painted ruin board with perspective depth and large continuous scene structure.
2. The tile-truth inset repeats one sampled floor and one sampled wall source into obvious patterned strips, so it does not prove `tileset.ruins.ground_01` / `tileset.ruins.wall_01` can carry the target look.
3. The map geometry departs from the current shell's tile-grid affordance enough that using it directly would push PR-08 toward full-screen paintover or a new rendering contract.
4. The comp should not drive PR-08 resource migration except as mood reference.

Reroll requirement:

1. Keep the same overall material quality.
2. Make the map orthographic/top-down and visibly decomposable into repeatable `tile_ground`, `tile_wall`, prop, actor and light families.
3. Include a target floor and wall treatment that survives 32px 4x4 or 8x4 repetition without checkerboard seams.
4. Keep non-rectangular darkness as a compositor/backdrop strategy, not as a baked full-screen map image.

## Attempt 2 Result

| Field | Value |
| --- | --- |
| Target comp path | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt2.png` |
| Target comp sha256 | `0e8290d4185362e35eeb7ad06905e8e710bf4597d61323639154d9213326e6a8` |
| Tile-truth inset path | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth-attempt2.png` |
| Tile-truth inset sha256 | `883a3c8fbc8b423b88989ff54eaa65b413ce3d63a0a9a68a8159cae93c2fd44d` |
| Prompt path | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt2.prompt.txt` |
| Prompt sha256 | `d62a4ff31dc083e45a4ad483481de5c7ed9b155160a3cf91487584b5bba902c5` |

Positive findings:

1. The center map is much closer to K-ToME's runtime contract: orthographic, tile-map-like, with separable actors, props, markers and fog.
2. Non-rectangular darkness reads as a compositor/backdrop idea rather than a pure black rectangle.
3. Right panel equipment and backpack areas are closer to the intended character workbench.

Blocking findings:

1. The bottom-right log contains baked English sentences. This directly violates the no baked text rule and would be invalid as target comp acceptance.
2. The tile-truth inset still repeats sampled content too visibly; the floor/wall strategy needs a dedicated tile-family target, not a crop from a full-screen mock.
3. The right panel uses a detailed character figure and slot art that may be useful as mood reference, but it is not yet mapped to existing `ui_frame` / `icon_item` / actor resources.

Absorbed direction for attempt 3:

1. Keep attempt 2's orthographic map-stage direction.
2. Remove all log text, labels and number-like UI marks.
3. Produce floor/wall tile family targets separately from the full-screen comp, then assemble the comp from those families.
4. Prefer a resource-family target pack over another monolithic full-screen generation if the generator keeps leaking text.

## Attempt 3 Result

| Field | Value |
| --- | --- |
| Target comp path | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt3.png` |
| Target comp sha256 | `da5b3a0c61d0ca530d50dcb739a3c5eb9d27e7f967cc505f3bc4343132806e24` |
| Tile-truth inset path | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth-attempt3.png` |
| Tile-truth inset sha256 | `c79d1157ab5feeae52741350932b4f954bd2a224b9596ef8c93607aad8bf1430` |
| Prompt path | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt3.prompt.txt` |
| Prompt sha256 | `209a4c15556b890be4efba6dd1699db6b2e7f00bd496fb10601184a5cfb94dff` |

Positive findings:

1. The comp has no obvious readable UI text, labels, numbers, hotkeys, manifest keys or log sentences.
2. The map reads as orthographic top-down and much closer to K-ToME's current shell than attempt 1.
3. Actor, prop, telegraph, equipment, backpack and bottom HUD ideas are visually separable enough to guide resource-family planning.
4. Right panel and bottom deck material direction is stronger than the current runtime and can guide UI chrome reuse or reroll decisions.

Blocking findings:

1. The 32px tile-truth inset still fails: crops from the full-screen comp include poison/light overlays, props, wall-edge darkness and local scene context.
2. The floor/wall look is strong as a painted composition, but the comp does not prove that `tileset.ruins.ground_01` and `tileset.ruins.wall_01` can be rebuilt as repeatable 32px cells.
3. Accepting attempt 3 directly would risk turning PR-08 into full-screen paintover or renderer-specific crop fitting, both outside the PR contract.

Absorbed direction for the next artifact:

1. Treat attempt 3 as the current mood/layout reference.
2. Produce a resource-family target pack before runtime work: floor cells, wall cells, map-stage backdrop, panel body/dividers, slot chrome, bottom deck chrome and compositor examples.
3. Validate floor/wall with a true 32px repeated contact sheet created from the family pack, not from full-screen crop sampling.
4. Only after that target pack passes should PR-08 activate owner supersession or resource migration.

## Family Pack Result

| Field | Value |
| --- | --- |
| Family pack contract | `UI/targets/dark-uiux-director-grade-target-family-pack.md` |
| Floor target path | `UI/targets/dark-uiux-director-grade-target-family-floor-32px.png` |
| Floor target sha256 | `a9b6a32717bdbfc3fb94ebb64db8bffd2a689e405c7039b2f6ebf7d25dbe8619` |
| Wall target path | `UI/targets/dark-uiux-director-grade-target-family-wall-32px.png` |
| Wall target sha256 | `fb7688695a7f0f79ccf27db60be3174b3a2ff323844e39372e0c1917ec3c1e26` |
| Repeat sheet path | `UI/targets/dark-uiux-director-grade-target-family-floor-wall-repeat.png` |
| Repeat sheet sha256 | `80aa56bded38e2358f978bf8f90abe24353629fe6012d72097cd4453617ff73d` |
| Map compositor path | `UI/targets/dark-uiux-director-grade-target-family-map-compositor.png` |
| Map compositor sha256 | `d5497eea250474a5a08f4aa07f45e9c6d2d260499abfc110d6505d727de3116e` |
| UI chrome path | `UI/targets/dark-uiux-director-grade-target-family-ui-chrome.png` |
| UI chrome sha256 | `f47f8f00fa76bc5b905b5307a81947627fb8ed8c75808a4143ddec01b2494d63` |
| Prompt path | `UI/targets/dark-uiux-director-grade-target-family-prompt.txt` |
| Prompt sha256 | `92267b29733ee952e1d13718940becac3a87c4849c6a6fd1c4a8e52a7e12d7a9` |

Positive findings:

1. Floor and wall now have clean 8x4 32px family targets generated independently from any full-screen comp crop.
2. The repeat sheet is built from 32px cells from those family targets, so it validates the right failure mode for PR-08 resource planning.
3. The map compositor reference keeps black field, warm light, selection, hazard and telegraph as layers over visible resources.
4. The UI chrome reference separates reusable panel, divider, slot, deck and blank log surface treatments without baked text or item icons.

Remaining caveats:

1. The generated floor sheet has visible cell-border rhythm. It is acceptable as direction evidence, but final runtime resources need polish and owner-gated review before migration.
2. The wall sheet reads correctly as blocking mass, but final runtime assets should avoid over-busy rubble that can hurt tile readability.
3. The family pack does not approve runtime code changes by itself; PR-08 still needs resource readiness, owner coverage and focused renderer tests before integration.

## Sliceability Table

This table records the intended PR-08 integration strategy before any runtime/resource mutation. Rows with `pending target comp` are not implementation approval.

| Resource family | ownerPr | sheetId | targetKey | category | displaySize | tiling/stretch strategy | consumer | consumerTest | fallbackKey | requiresNewSchema | packagedRisk | Current decision |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Ruins floor | `PR-08` supersedes `PR-02-2` | `r02-ui-demo-ruins-tiles` | `tileset.ruins.ground_01` | `tile_ground` | `32px tile cell` | Repeatable tile; family pack repeat sheet must stay usable without checkerboard seams | `TileRenderer` terrain base | `ManifestResolveTest.darkUiuxPr02_2OwnerKeysResolveThroughExactEntries`; new PR-08 owner test required before migration | `missing_visual` | no | high | accepted for direction; activate supersession only with owner coverage |
| Ruins wall | `PR-08` supersedes `PR-02-2` | `r02-ui-demo-ruins-tiles` | `tileset.ruins.wall_01` | `tile_wall` | `32px tile cell` | Repeatable blocking wall tile; family pack must keep wall mass thicker than grid edge | `TileRenderer` terrain base | `ManifestResolveTest.darkUiuxPr02_2OwnerKeysResolveThroughExactEntries`; new PR-08 owner test required before migration | `missing_visual` | no | high | accepted for direction; activate supersession only with owner coverage |
| Vanguard actor | `PR-08` supersedes `PR-02-2` only if actor reroll is needed | `r03-ui-demo-actor-props` | `actor.vanguard` | `actor_sprite` | `32px tile footprint` | Centered actor sprite with transparent padding; no baked shadow as authority | `TileRenderer` actor layer | `ValidationScenarioRegistryTest.darkUiuxPr021ScenarioUsesVanguardOutpost`; new PR-08 marker-readability test required if rerolled | `missing_visual` | no | medium | defer unless target comp shows actor readability gap |
| Down stairs prop | `PR-08` supersedes `PR-02-2` only if prop reroll is needed | `r03-ui-demo-actor-props` | `prop.stairs.down` | `prop_interactable` | `32px tile footprint` | Centered prop sprite with transparent padding; no text or direction marker baked in | `TileRenderer` prop layer | `ValidationScenarioRegistryTest.darkUiuxPr021ScenarioUsesVanguardOutpost`; new PR-08 prop-readability test required if rerolled | `missing_visual` | no | medium | defer unless target comp shows prop readability gap |
| Map-stage backdrop | `PR-02-1` upstream or `PR-08` supersession if rerolled | `r01b-ui-shell-chrome` | `ui.shell.map_stage.backdrop` | `ui_frame` | stage fill | Stretch existing single PNG only if it remains non-authoritative visual atmosphere | `DemoShellRenderer.renderMapStageFrame` | `DemoShellRendererTest.shellKeysBindToConsumerRegionOnly`; PR-08 crop review required | `ui.frame.panel.body` | no | medium | compositor direction accepted; probe existing resource first |
| Right panel body/dividers | `PR-02` / `PR-02-1` upstream; PR-08 only if target comp proves gap | `r01-ui-chrome`, `r01b-ui-shell-chrome` | `ui.frame.panel.body`, `ui.shell.right_section.divider` | `ui_frame` | panel fill / divider strips | Stretch existing UI frame pieces; no baked labels | `DemoShellRenderer.renderShell` | `DemoShellRendererTest.shellKeysBindToConsumerRegionOnly`; `TileRendererCanvasTest` right-panel crop assertions | `ui.frame.panel.body` | no | medium | UI chrome direction accepted; probe existing resource first |
| Slot chrome | `PR-02` upstream; PR-08 only if target comp proves gap | `r01-ui-chrome` | `ui.frame.slot.empty`, `ui.frame.slot.equipped`, `ui.frame.slot.selected` | `ui_frame` | equipment/backpack slot sizes | Stretch or draw existing slot frame by state; icons remain separate resources | `DemoShellRenderer` equipment/backpack slots | `TileRendererCanvasTest` slot state assertions | `ui.frame.panel.body` | no | medium | UI chrome direction accepted; no schema change allowed |
| Bottom deck chrome | `PR-02-1` upstream; PR-08 only if target comp proves gap | `r01b-ui-shell-chrome` | `ui.shell.hero_card.frame`, `ui.shell.action_deck.frame`, `ui.shell.log_deck.frame` | `ui_frame` | bottom HUD panels | Stretch existing deck frames into one console family; text remains runtime drawn | `DemoShellRenderer` bottom HUD | `TileRendererCanvasTest.bottom hud panels sit on one forged foundation rail`; PR-08 bottom crop required | `ui.frame.panel.body` | no | medium | UI chrome direction accepted; validate runtime crop after integration |
| Light/dark field | `client` compositor first; resource only if target comp proves need | N/A | N/A | N/A | stage/cell overlay | Deterministic compositor layer; decorative darkness must not become visibility authority | `TileRenderer` after semantic terrain and before gameplay markers | new PR-08 layer-order and marker-readability tests required | N/A | no | high | compositor direction accepted; no resource key approved |

## Supersession Decision

Recommended route: `PR-08 supersession`.

Rationale:

1. The gap audit selected `resource-gap` as the primary root cause.
2. The first-screen ruins resources are currently `PR-02-2` owner keys.
3. PR-08 close evidence uses `dark-uiux-pr08-director-*`, so silent PR-02-2 reroll would make owner evidence drift.

Rollback path for superseded keys:

1. Preserve current `PR-02-2` owner contract and contact sheet paths in review evidence.
2. Keep previous raw/runtime PNG hashes in the PR-08 readiness report.
3. If PR-08 reroll fails director review, revert only the PR-08 owner migration and generated resources; keep PR-07 and PR-02-2 evidence untouched.

## Target Comp Prompt Direction

Use this prompt direction only to produce the pending target comp. It is not a manifest or mapping authority.

```text
Create a 1672x941 first-screen dark fantasy roguelike UI target comp for K-ToME.
Layout must match the existing game shell: narrow left navigation rail, dominant center dungeon map stage, right equipment/inscription/backpack panel, and bottom hero/action/log HUD.
Style tag: ktome-dark-fantasy-sprite-ui-v1.
The center map is a hand-authored ruined dungeon room with worn stone floor slabs, thick chipped walls, non-rectangular surrounding darkness, restrained torch ember light, and readable actor/loot/telegraph markers.
The UI chrome is forged iron, worn stone, old leather, weathered brass, charcoal black panels, small ember accents, and restrained cyan edge highlights.
No baked Chinese text, English text, numbers, hotkeys, labels, watermarks, logos, or manifest keys inside the image.
Do not change gameplay layout. Do not create a full-screen paintover that cannot be sliced into tile, panel, slot, deck, backdrop, actor, prop, and light families.
```

## Next Required Action

Next PR-08 implementation pass should start with resource readiness and owner wiring, not renderer micro-rectangle expansion:

1. add explicit PR-08 owner coverage for superseded floor/wall keys before migration
2. generate or polish final runtime resource candidates from the accepted family direction
3. run resource pipeline lint and focused renderer tests before golden updates
4. keep actor/prop reroll deferred unless marker readability fails with the new floor/wall/compositor path
