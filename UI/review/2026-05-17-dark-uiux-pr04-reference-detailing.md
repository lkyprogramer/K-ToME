# Dark UI/UX PR04 Reference Detailing Pilot

## 1. Status

This document is a design-detailing pilot for `UI/pr/dark-uiux-pr04-profession-tree-ui.md`.

It is not a new authority. It refines the existing canonical reference image into renderer-facing, golden-facing, and review-facing rules. If useful, selected rules can be absorbed into the PR04 document or converted into `TileRendererCanvasTest` / `TalentSidebarPresenterTest` assertions.

Source image:

- `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png`
- `1280x840`
- Existing PR04 role: unique visual development reference for Talent Assign.

## 2. Verdict

The reference image is already suitable as the PR04 canonical direction, but it is not enough by itself. For K-ToME PR04, the implementation contract must be anchored to the current branch's typed talent assign surface:

- `RenderUiStateSnapshot.talentTrees` is the data source for trees, rows, prerequisites, rank, point banks, lock reasons, and detail-preview fields.
- `TalentSidebarPresenter.presentPanel(...)` is the presentation authority for header text, section rows, markers, connector prefix, detail blocks, legend, footer help, and active-slot modal rows.
- `TileRenderModel` / `TileRenderer` may choose bounds, clipping, reference-crop assets, and truncation, but must not derive gameplay state from marker text, icon keys, localized strings, colors, or row index.
- `InputHandler` / `ModalStack` own the talent modal state machine and selection identity. The image cannot redefine command semantics.
- PR04 resource fidelity is limited to scoped reference-crop skill icons and modal chrome; full icon/style rebaseline remains PR06-owned.

The most useful refinement is not another full redesign. The useful refinement is to lock down:

1. stable panel proportions,
2. row grammar and state priority,
3. detail-pane block hierarchy,
4. overflow behavior,
5. the project-owned data/model/input boundaries that keep the image from becoming a second rules source,
6. visual tokens that the renderer can approximate without pixel-perfect image matching.

The current image should stay the visual target. PR04 should avoid generating a competing second reference unless a specific readability problem is found in the libGDX render.

## 3. Project Truth Anchors

This pilot now treats the reference image as a visual target and the current project surface as the implementation truth.

| Layer | Current source | Design consequence | Status |
| --- | --- | --- | --- |
| Snapshot contract | `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt` exposes `TalentTreeSnapshot`, `TalentTreeNodeSnapshot`, `TalentNodePrerequisiteSnapshot`, `descriptionModel`, `nextRankDescriptionModel`, lock reasons, point banks, state, category, rank, and committed rank | PR04 must render actual snapshot data. Effect numbers, cost, cooldown, range, lock reason, and next preview cannot be copied from the mockup | consistent |
| Snapshot builder | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` builds talent trees from profession + race tree ids, applies presentation order from typed prerequisite data, and records `talentPrereqs` into node snapshots | Connector depth and edge tone are allowed only from typed prerequisites. If the snapshot lacks data, renderer cannot fake a tree from row order or localized lock text | consistent |
| Presentation model | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentAssignPanelModel.kt` defines `TalentAssignPanelModel`, section rows, detail blocks, legend, and active-slot modal item types | Renderer-facing design must map to these typed fields, not to one-off mock coordinates or screenshot labels | consistent |
| Presenter authority | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt` builds rows from `uiState.talentTrees`, restores selected node by `TalentTreeSelectionIdentity`, maps four `TalentNodeStateSnapshot` states to `[x]/[+]/[r]/[*]`, and builds ordered detail blocks | Row grammar, detail order, state markers, footer help, legend, and modal rows are presenter-owned. The renderer should only draw the model | consistent |
| Input state | `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` handles `TALENT_ASSIGN` navigation, `Left/Right` tree switching, `Enter/Space` learn/confirm, `R` respec or reserve, and `ACTIVE_TALENT_SLOT_CHOICE` `1..4/R/Esc` commands | Footer and action text must mirror actual input truth. The image cannot introduce commands that are not handled in `InputHandler` | consistent |
| Modal state | `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt` persists `talentTreeSelectionIdentity` in modal local state | Focus is a durable identity over `talentId + treeId + ownerType + treeOwnerId`, not a visual row index | consistent |
| Render model | `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` creates `TileTalentAssignPanelRenderModel` and resolves row/detail/chrome assets through `VisualManifestResolver` | Icon/chrome fidelity belongs in manifest keys and reference-crop tags. Production code should not use raw file paths as fallback truth | consistent |
| Renderer | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` draws a full-screen modal, `55%` left list, right detail pane, reference-crop chrome when present, centered list viewport, full-row cyan focus, footer help, and legend | Geometry guidance can be made concrete, but pixel-perfect screenshot coordinates should not override the current scalable layout | consistent |
| Locale contract | `game/src/main/resources/i18n/zh-CN.json` and `en-US.json` contain `ui.talent.assign.*` strings including footer help, state labels, detail labels, action labels, and legend labels | All displayed text comes from localization. The doc can show Chinese examples, but tests and renderer cannot hard-code them | consistent |
| Resource contract | `assets-src/image/specs/phase4-uiux-pr04-reference-crop-plan.yaml` and both visual manifests define `dark.uiux.pr04.talent.vanguard.*` and `dark.uiux.pr04.talent_assign.chrome.*` reference-crop keys | PR04 can use scoped reference crops for Vanguard evidence. Non-canonical profession icons remain fallback/PR06 risk, not PR04 gameplay failure | consistent with PR04 scope |
| Validation surface | `TalentSidebarPresenterTest`, `InputHandlerTest`, `TileRendererCanvasTest`, and `GoldenScreenshotHarnessTest` already cover model fields, identity restore, input isolation, selected row/icon/rank, detail ordering, overflow, right companion coexistence, and canonical golden artifact labels | The detailing work should strengthen these tests/golden expectations instead of inventing separate visual-only acceptance rules | partially complete; still needs live gate run before claim of pass |

## 4. Project-Grounded Definition Matrix

| Reference intent | Current implementation evidence | PR04 definition |
| --- | --- | --- |
| Full-screen ToME-like talent menu | `TileRenderer.drawTalentAssignModal` uses modal visual bounds, content scrim, reference chrome, header, body split, footer, and optional slot-choice modal | Keep the single modal surface. Do not convert it into cards, graph nodes, or a web dashboard |
| Left tree list with three Vanguard groups in canonical evidence | PR04 doc and validation text require three expanded Vanguard sections for manual/golden evidence; runtime `TalentSidebarPresenter` maps every `uiState.talentTrees` section, including race trees | Canonical evidence should use Vanguard `6/5/5`; runtime design is generic over snapshot trees and must not filter race trees |
| Row marker + icon + name + rank | `TalentAssignTreeRowModel` already contains `stateMarkerText`, `skillIconKey`, `displayName`, `rankText`, `toneToken`, and `focused` | This row grammar is required. Renderer should not append rank or infer state from marker text |
| Tree connectors | `TalentSidebarPresenter.projectTreeRows(...)` projects connector prefix and edge state from typed same-tree prerequisites | Connectors are allowed because the branch has typed prerequisite snapshots. If this source regresses, connector drawing must stop rather than become heuristic |
| Selected row highlight | `TileRenderer` draws full-row cyan background and left cyan strip when `row.focused` is true | Focus and state remain independent: selected `[+]` still stays `[+]`, selected `[*]` still stays `[*]` |
| Current detail before next preview | `TalentSidebarPresenter.detailPane(...)` emits `CURRENT_RANK_DETAIL` before `NEXT_RANK_PREVIEW`; tests assert the order | The reference's right pane hierarchy is valid because it matches current model order |
| Preview numbers | `currentRankDetailBlock`, `DescriptionPresenter`, and `nextRankUpgradeLines` derive text from typed description models and placeholders | The mockup's damage/cost/cooldown numbers are examples only. Runtime numbers always come from snapshot/schema-derived detail fields |
| Action keycaps | `TalentSidebarPresenter.actionsBlock` emits `Enter`, `R`, `Esc`; `InputHandler` handles learn/confirm, respec/reserve, and close/rollback behavior depending on modal state | Keep keycap visual rhythm, but distinguish root `TALENT_ASSIGN` `R` respec from `ACTIVE_TALENT_SLOT_CHOICE` `R` reserve in doc/tests |
| Active slot choice | `ActiveSlotChoiceModalModel` has four slot items plus reserve row; `InputHandler.pollActiveTalentSlotChoiceCommand` consumes only `1..4/R/Esc` | The slot-choice modal is a typed list, not a footer-only hint. Number keys outside this modal must remain isolated |
| Footer help and legend | Locale key `ui.talent.assign.footer_help` currently states `Up/Down`, `Left/Right`, `Enter`, `R`, `Esc`; presenter legend has four state markers plus focus and optional pending | Footer text is implementation truth and should remain localizer-owned. Legend markers are visible state aids, not parser inputs |
| Reference-crop icons/chrome | `pr04TalentAssignIconKey` maps canonical Vanguard talent ids to `dark.uiux.pr04.talent.vanguard.*`; renderer recognizes `reference-crop` tags; chrome assets resolve through `TileTalentAssignReferenceChromeSlot` | PR04 can deliberately mix reference crops for canonical Vanguard evidence with manifest fallback elsewhere, but fallback must be called out as PR06 risk |

## 5. Reference Geometry

These numbers are implementation guidance, not pixel-perfect assertions. The renderer should preserve ratios and hierarchy, then adapt to runtime font metrics.

| Region | Reference observation | Renderer guidance |
| --- | --- | --- |
| Outer modal | fills nearly the whole `1280x840` canvas with a thin framed border and small corner ornaments | modal should feel like a single full-screen RPG menu, not a floating card |
| Header band | title and points occupy the top `~90px` | title, profession points, and race points must remain outside scrollable content |
| Body split | left list is slightly wider than right detail pane | use `55-58%` left, `40-43%` right, with a fixed divider gap and no overlap |
| Left list | starts just below the points line and ends above footer | list may scroll internally, but section headers and rows remain in one list plane |
| Right detail | independent bordered pane aligned to left list top/bottom | selected talent detail never scrolls the left list and never shares row hit regions |
| Footer | one persistent strip at the bottom | keyboard help and state legend stay visible at canonical and min-window evidence sizes |

Suggested canonical layout model:

```text
root
  outerFrame
  header: title + points
  content
    leftTreePanel
    rightDetailPanel
  footerHelpAndLegend
```

Do not render this as nested cards inside cards. The reference reads as one modal with two internal panes.

## 6. Visual Token Refinement

The generated image has anti-aliased colors, so these are semantic target tokens rather than exact sampled values.

| Token | Intended role | Suggested renderer treatment |
| --- | --- | --- |
| `pr04.panel.bg` | near-black brown modal body | very dark brown-black fill, low saturation |
| `pr04.panel.border` | iron frame and pane separators | thin 1-2px border, not bright gold |
| `pr04.title.ember` | title and section labels | ember-gold, highest warmth in the UI |
| `pr04.text.primary` | readable body text | warm off-white, no pure white except keycaps if needed |
| `pr04.text.secondary` | metadata and inactive detail rows | muted warm gray |
| `pr04.row.selected.bg` | selected row fill | dark cyan fill, full-row rectangle |
| `pr04.row.selected.border` | selected row edge | restrained cyan line, not glowing neon |
| `pr04.state.learnable` | `[+]` and learnable chip | cyan |
| `pr04.state.locked` | `[x]` and locked rows | muted gray |
| `pr04.state.active` | `[*]` learned active | green |
| `pr04.state.reserve` | `[r]` reserve | ember-gold |
| `pr04.rule.line` | detail pane separators | subdued gold/brown horizontal rule |

Refinement rule: selection, state, and availability must not be expressed by color alone. The marker text `[+] / [x] / [*] / [r]`, rank text, and state chip remain required.

## 7. Left Tree Panel Detail

The left side should be treated as a dense operational menu, not as an illustration.

### 7.1 Row Grammar

Fixed row order:

```text
indent + connector + state marker + skill icon + talent name + rank
```

Renderer constraints:

1. row height is stable across all states;
2. icon size is fixed, visually around `24px`;
3. marker column width is stable, so `[+]` and `[*]` do not shift names;
4. rank is right-aligned in the row;
5. selected row spans the full list row, not just the text;
6. section headers are not selectable rows.
7. row `rankText` currently represents committed rank in list rows, while the detail block shows committed-to-preview transition; if pending allocation should be visible in rows, that must be added in `TalentSidebarPresenter`, not in the renderer.

### 7.2 Section Header

The section header is both a group label and a node-count summary.

```text
武器系                                      6/6
```

Rules to preserve:

1. section header has a darker band and ember text;
2. `6/6` means visible nodes / total nodes, not learned rank;
3. section header spacing must visually separate tree families;
4. header height may be slightly smaller than a talent row, but must remain readable.
5. current presenter emits `${tree.nodes.size}/${tree.nodes.size}` because there is no filtering; any future filtering must change presenter semantics and tests together.

### 7.3 Connector Detail

The reference uses lightweight connector glyphs to suggest prerequisites without becoming a graph.

Renderer guidance:

1. connector glyphs are secondary visual aids;
2. connector tone follows edge state, but row state remains primary;
3. connector depth is currently available through typed `TalentNodePrerequisiteSnapshot` projection; if that source is unavailable, do not fake connector depth from row order;
4. long connector chains must not push icons out of the stable icon column.

## 8. Selected Row Detail

The selected row is `横扫 0/5`.

The visual priority should be:

1. cyan selected strip,
2. `[+]` learnable marker,
3. skill icon,
4. talent name,
5. rank.

The row must still read as part of the tree, not as a detached detail card. This matters because PR04 should preserve keyboard list navigation and avoid introducing a hidden state owner in the renderer.

Suggested canvas assertions:

| Assertion | Purpose |
| --- | --- |
| selected row has full-row cyan background | prevents text-only focus regression |
| selected row keeps skill icon visible | prevents selection fill from covering icon |
| selected row rank remains right aligned | prevents long Chinese text from shifting rank |
| selected row marker remains `[+]` | prevents selected state from replacing semantic state |

## 9. Right Detail Pane Detail

The right pane has a clear reading stack. PR04 should preserve this order.

### 9.1 Header Block

Structure:

```text
large skill icon
talent name
state chip
rank transition
point cost
prerequisite
level requirement
```

Refinement:

1. the hero icon box should be square and aligned to the talent title top;
2. state chip should be small and adjacent to the talent name, not a full button;
3. rank transition and cost belong above the first separator;
4. prerequisite and level requirement are metadata, not error banners.
5. hero/detail icon keys come from the presenter and manifest resolver. For the current branch, `pr04TalentAssignHeroIconKey` prefers the selected talent's existing icon key and only falls back to PR04 reference-crop mapping when needed.

### 9.2 Current Rank Detail

This is the highest-value detail in the reference and must remain mandatory.

```text
当前等级详情（预览 1级）
类型: 主动
范围: 邻近
半径: 1
消耗: 10 耐力
冷却: 4 回合
效果: 对前方扇形敌人造成 120% 武器伤害
说明: 可加入快捷槽
```

Refinement:

1. label/value rows should align on a stable label column;
2. body text wraps inside the right pane, not over the separator;
3. unavailable fields are omitted, not rendered as blank labels;
4. this block is present for unlearned learnable talents as a rank-1 preview;
5. this block appears before `NEXT_RANK_PREVIEW` in tests and golden.
6. field content comes from `DescriptionPresenter` and `DescriptionModelSnapshot`; the reference mock's example numbers are not gameplay truth.

### 9.3 Next Rank Preview

The next-rank block should remain visibly secondary.

Rules:

1. title uses ember-gold but lower prominence than the selected talent name;
2. body lines are delta-oriented;
3. max-rank state must replace fake next-level lines;
4. next-rank text must not duplicate current-rank effect lines.
5. breakpoint/milestone text uses actual gameplay preview data, such as the existing knockback breakpoint assertion; it should not inherit unrelated mock wording like fake radius upgrades.

### 9.4 Action Block

The action block reads like keycaps, not like navigation pills.

Canonical actions:

```text
Enter 学习
R 预留
Esc 返回
```

Rules:

1. actions are detail-pane local commands;
2. footer help remains panel-level navigation;
3. disabled action variants must preserve the same horizontal rhythm;
4. `Esc 返回` in detail pane and `Esc 关闭` in footer can coexist only if their semantic scopes are clear.
5. root `TALENT_ASSIGN` `R` is currently respec-owner behavior, while `ACTIVE_TALENT_SLOT_CHOICE` `R` confirms reserve. The document must name that modal-state split instead of treating `R` as one global command.

## 10. Footer And Legend

The footer has two distinct roles:

1. left side: keyboard navigation help,
2. right side: state legend.

Refinement:

| Footer item | Detail |
| --- | --- |
| `↑↓ 选择` | list navigation |
| `←→ 切换系` | only if current input implementation supports tree/system switching |
| `Enter 学习` | selected talent action shortcut |
| `R 预留` | selected talent action shortcut |
| `Esc 关闭` | modal-level close |
| `[+] 可学习` | state legend |
| `[x] 锁定` | state legend |
| `[*] 已激活` | state legend |
| `[r] 预留` | state legend |

If runtime input uses `Tab` rather than `←→` to switch systems, the footer must follow implementation truth. The reference image is a visual guide, not an input-contract override.

For the current branch, `Left/Right` switching is implemented in `InputHandler.moveTalentTreeColumn(...)`, and the zh-CN locale footer already says `←→ 切换系`. Keep that as long as the input test surface stays aligned.

## 11. What To Absorb Into PR04

High-value additions worth absorbing into PR04 implementation or tests:

1. `selected row marker remains semantic state`: selected row cannot replace `[+]` with a focus-only marker.
2. `label/value column in current detail`: prevents detail pane becoming unstructured prose.
3. `footer scope separation`: panel navigation help and selected talent actions are distinct.
4. `full-row selection`: golden should fail if only text changes color.
5. `section count meaning`: section `6/6` must not be confused with learned ranks.
6. `right pane secondary hierarchy`: next preview must not visually dominate current detail.
7. `keycap rhythm`: action keycaps use stable dimensions so Chinese labels do not resize the row.
8. `project truth matrix`: PR04 review should verify presenter/model/input/resource evidence, not just screenshot resemblance.
9. `race tree no filter`: canonical Vanguard evidence uses three profession trees, but runtime rendering must consume all snapshot trees.
10. `slot-choice command split`: root talent assign and active-slot choice modal have different `R` semantics and must remain test-visible.

## 12. What Not To Absorb

Do not absorb these as implementation requirements:

1. exact generated image colors or sampled pixels;
2. exact effect numbers from the mockup;
3. exact icon artwork before PR06 rebaseline;
4. ornate corner decoration as a blocking renderer feature;
5. pixel-perfect text positions;
6. ToME-specific behavior not already present in K-ToME typed snapshots;
7. mouse hover behavior, unless PR04 explicitly implements hit regions.
8. hard-coded Vanguard strings in production code;
9. renderer-side owner filtering, learnability recomputation, prerequisite inference, or text parsing;
10. a new resource inventory outside the manifest / reference-crop pipeline.

## 13. Suggested Focused Assertions

Candidate tests, mapped from the reference image:

| Candidate assertion | Likely test surface |
| --- | --- |
| `TalentAssignPanelModel` exposes header, sections, detail, legend, footer as separate fields | `TalentSidebarPresenterTest` |
| section headers are not selectable rows | `TalentSidebarPresenterTest` |
| row state marker and selected focus are independent | `TalentSidebarPresenterTest`, `TileRendererCanvasTest` |
| selected row draws icon, name, rank, and full-width highlight | `TileRendererCanvasTest` |
| current detail appears before next preview | `TalentSidebarPresenterTest`, `TileRendererCanvasTest` |
| current detail uses label/value rows when fields exist | `TalentSidebarPresenterTest` |
| footer help and state legend both visible at canonical viewport | `TileRendererCanvasTest`, `goldenScreenshot` |
| long Chinese talent name truncates without shifting rank | `TileRendererCanvasTest` |
| action keycaps keep stable width | `TileRendererCanvasTest` |
| runtime race tree is rendered with the same row/detail contract | `TalentSidebarPresenterTest` |
| active slot choice consumes `1..4/R/Esc` only inside `ACTIVE_TALENT_SLOT_CHOICE` | `InputHandlerTest` |
| reference-crop assets resolve through manifest tags and fallback is recorded as residual risk | `TileRendererCanvasTest`, `manifestLint`, manual record |

## 14. Open Risks

| Risk | Impact | Recommendation |
| --- | --- | --- |
| Chinese font metrics differ from generated mockup | text can overflow right pane or footer | use renderer text measurement and truncation tests |
| existing skill icons are not dark-v1 | PR04 screenshot may mix visual eras | allow PR04 fallback only with manual residual risk; PR06 owns full rebaseline |
| footer/action command scope is ambiguous | `R` could be read as reserve globally even though root modal uses respec-owner behavior | document and test modal-state-specific command semantics |
| detail pane may be too dense at min-window | current detail can hide next preview or actions | prioritize current detail, then actions, then next preview under compact rules |
| connector projection regresses or is absent in future snapshots | fake prerequisite tree would become second authority | defer connectors or restore typed prerequisite projection before drawing |
| row rank and detail rank transition drift | players may not see pending allocation consistently | keep row/detail rank formatting presenter-owned and add pending-row tests if pending row rank is required |
| canonical Vanguard evidence is mistaken for runtime-only scope | race/dev/frozen tree behavior may regress | separate canonical screenshot contract from generic snapshot rendering contract |

## 15. Recommended Next Step

For the PR04 trial, absorb only the high-value renderer/test refinements from §11 and §13. Do not regenerate the image yet.

If the first libGDX golden render diverges visibly from the reference, use this document as the review checklist:

1. compare panel proportions,
2. compare row grammar,
3. compare selected-row state independence,
4. compare current-detail ordering,
5. compare footer scope,
6. compare actual project boundaries from §3 and §4,
7. only then decide whether a second reference image is needed.
