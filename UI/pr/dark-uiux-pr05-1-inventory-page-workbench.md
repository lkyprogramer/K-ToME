# Dark UI/UX PR-05-1 Inventory Page Workbench

**阶段**: `dark-uiux-pr05-1-inventory-page-workbench`
**优先级**: `P1`
**工作量**: `L`
**前置条件**: PR-03 equipment / inventory / item icon 基础能力已进入可用状态；PR-05 map / actor / portrait replacement 保留暗黑游戏 shell、左 rail、底部 HUD 与地图上下文；`UI/dark-uiux-pr03-inventory-page-reference.png` 已作为 one-off development reference 生成并完成尺寸验收。
**资源生成结论**: 本 PR 不生成正式 runtime asset，不新增 sprite sheet，不进入 manifest，不修改 `sheet-plan.yaml` / `key-registry.yaml` / canonical atlas。参考图只作为 UI 合同图；如果后续需要把其中某些 item / frame / panel 视觉升级为正式资源，必须另走 sheet plan -> raw sheet -> slice/contact sheet -> canonical manifest -> `resourcePipelineLint`。

> 本 PR 的目标是把当前不可用的背包大弹窗改造成 K-ToME 主流程可长期使用的全屏 workbench。它不是 PR-03 资源补丁，也不是 PR-05 地图资源范围的延伸。

## Open Design 辅助参考

开发 PR05-1 时可在完成本 PR 预检后读取以下辅助设计输入：

1. [K-ToME Dark UI Design Reference For Open Design](../review/open-design/ktome-dark-ui-design.md)：统一 color roles、spacing、component states 与 anti-pattern 语言。
2. [Dark UI/UX PR05-1 Inventory Page Workbench Design Notes](../review/open-design/dark-uiux-pr05-1-inventory-page-workbench-design.md)：辅助 review 背包 workbench 的信息层级、6x4 grid 可读性、selection / hover / focus / disabled state 区分、typed detail / compare 诚实性、reference image 排除项和 golden/manual evidence 质量。

这些文档只用于设计理解、review 和证据质检，不能覆盖本 PR 文档、gate、manifest、schema、resource pipeline、golden/manual evidence、whitebox scenario 或 implementation contract。

## 0. 开发治理与验收矩阵

本节必须遵守 `UI/pr/development-governance.md`，验证入口必须接入 `docs/verification/README.md`，AI / resource / anti-bloat 边界必须遵守 `docs/rule/ai-change-governance.md`。PR05-1 是 `client/docs` UI workbench PR，不声明新的 Dark UI sprite `ownerPr`。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI05-1-ROUTE-01` | §0 governance / `UI/pr/README.md` / `UI/pr/screen-coverage-matrix.md` | docs/tools | `acceptanceContractLint` | `verifyChanged` | `UI/pr/README.md`, `UI/pr/screen-coverage-matrix.md`, `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt` | `N/A` |
| `UI05-1-REF-01` | §3 reference image contract | docs/assets | `acceptanceContractLint` checks reference PNG existence, `1672x941` dimensions, sha256, prompt path and repo-relative text paths | `N/A` | `UI/dark-uiux-pr03-inventory-page-reference.png`, `UI/dark-uiux-pr03-inventory-page-reference.prompt.txt` | `skipped: design reference only` |
| `UI05-1-REF-EXCLUSION-01` | §3.4 reference exclusion table | client/docs | exact denylist scan for `ui.inventory.filter`, `ui.inventory.category_tab`, `ui.inventory.weight`, `burden`, `encumbrance`, `carryWeight`, `weightCapacity`, `负重`, `承重` in production surfaces | `contractLint` | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`, `UI/pr/screen-coverage-matrix.md`, `tools/src/test/kotlin/com/ktome/tools/lint/Pr05InventoryWorkbenchReferenceExclusionLintTest.kt` | `N/A` |
| `UI05-1-PRESENTATION-01` | §5 inventory workbench presentation model | client | `InventoryWorkbenchPresenterTest` | `:client:clientSmoke` | `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt`, `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt` | `N/A` |
| `UI05-1-GRID-01` | §5.2 6x4 grid contract | client | presenter test asserts `columns == 6`, `rows == 4`, `cells.size == 24`, page/capacity from model | `:client:goldenScreenshot` | `client/build/reports/golden/index.json` labels `dark-uiux-pr05-1-inventory-*` | `required` |
| `UI05-1-STACK-01` | §5.3 stack grouping and quantity badge contract | game/client | `InventoryWorkbenchPresenterTest` stack grouping cases | `contractLint`, `:client:goldenScreenshot` | stack-capable inventory presentation model and `dark-uiux-pr05-1-inventory-workbench` evidence | `required` |
| `UI05-1-INPUT-01` | §6 input and hover state machine | client | `InputHandlerTest` inventory focus / page / hover-preview cases | `:client:clientSmoke` | `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | `N/A` |
| `UI05-1-DETAIL-01` | §7 typed detail / compare / action rows | client | `InventoryWorkbenchPresenterTest` row ownership cases plus `client/src/test/kotlin/com/ktome/client/ui/talent/DescriptionPresenterTest.kt` only for shared description behavior | `localeLint`, `contractLint` | presenter tests plus locale bundles for action hint tokens | `N/A` |
| `UI05-1-SOCKET-01` | §4.2 visual socket mapping | client | presenter/layout test asserts visual-only sockets are disabled and not rule slots | `:client:goldenScreenshot` | `InventoryWorkbenchPresenterTest`, `TileRendererCanvasTest` | `required` |
| `UI05-1-REMOVAL-01` | §9 removal / migration table | client | static scan and focused regression replacing old 8-slot / modal expectations | `maintainabilityLint` | updated input/render/golden tests listed in §9 | `N/A` |
| `UI05-1-EVIDENCE-01` | §10 golden and manual evidence | client/docs | `acceptanceContractLint` checks manual record template path; implementation close must additionally register PR05-1 labels in `GoldenScreenshotHarnessTest` and replace pending manual status with real evidence | `:client:goldenScreenshot` | `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md`, `client/build/reports/golden/index.json` | `required` |
| `UI05-1-NO-RULE-01` | §2 / §8 non-goals | core/game/client | static diff review: no core/game inventory rule, save/replay/profile/schema, manifest or sprite owner change | `verifyChanged` | PR diff and doc-vs-implementation self-audit | `N/A` |

### Gate Budget

| Gate | Trigger | Freshness / Duration Source | Stop Rule |
| --- | --- | --- | --- |
| `acceptanceContractLint` | PR05-1 doc, README, screen matrix, reference artifact or lint routing changes | always fresh; duration source is `N/A before first verifyChanged` until the first `verifyChanged` run | fail fast on missing Acceptance Matrix / Gate Budget / artifact / failure rule / reference PNG dimension or hash drift / manual record template path |
| focused client tests | presentation model, input, detail rows, renderer layout | focused test runtime recorded in PR description | do not debug via golden until focused failure is closed |
| `:client:clientSmoke` | player-visible inventory route and input mode | latest run output plus `build/verification/verify-changed/full-task-duration-summary.{json,md}` when available | if it fails twice, add or tighten focused test before rerun |
| `:client:goldenScreenshot` | full-screen workbench, pagination, min-window and compare labels | golden report plus screenshot labels in §10 | if golden fails twice, inspect concrete diff and update deterministic scenario first |
| `localeLint contractLint maintainabilityLint verifyChanged` | localized action hints, typed presentation model, renderer/input structure | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | if a single round exceeds 90 minutes, write review note before rerun |

`resourcePipelineLint` is not part of the default PR05-1 gate because this PR does not create formal runtime resources. If any formal image / manifest / key registry / sheet plan change is added, resource gates become blocking in the same change.

### Canonical Artifact

PR05-1 canonical artifact:

1. This document: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`.
2. PR route and coverage entries: `UI/pr/README.md`, `UI/pr/screen-coverage-matrix.md`.
3. Acceptance lint registration: `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt`.
4. Design reference: `UI/dark-uiux-pr03-inventory-page-reference.png`, `1672x941`, sha256 `af7ce7992be1838ee75116d89d2284f8c81f48345a46cbe53bb7f50f64e71837`.
5. Design reference prompt: `UI/dark-uiux-pr03-inventory-page-reference.prompt.txt`.
6. Reference exclusion lint: `tools/src/test/kotlin/com/ktome/tools/lint/Pr05InventoryWorkbenchReferenceExclusionLintTest.kt`.
7. Expected implementation owner path: `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt`.
8. Expected focused tests: `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt`, `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`, `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`.
9. Golden labels: `dark-uiux-pr05-1-inventory-workbench`, `dark-uiux-pr05-1-inventory-compare`, `dark-uiux-pr05-1-inventory-pagination`, `dark-uiux-pr05-1-inventory-min-window`.
10. Manual record target: `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md`. Before runtime implementation lands this file is only a `PENDING_IMPLEMENTATION` template; final PR05-1 evidence must replace pending fields with real screenshots, logs and gate results.

Non-canonical artifact:

1. `build/reports/codex-image/dark-uiux-pr03-inventory-page-reference.json`.
2. Codex transient generated image source directories.
3. prompt 中的一次性布局草案文字。
4. open-design review 文档中的非 PR 范围建议。

### Failure Rule

出现以下任一情况，本 PR 视为未完成：

1. PR05-1 没有进入 `UI/pr/README.md`、`UI/pr/screen-coverage-matrix.md` 或 `Phase4V4AcceptanceContractLintTest`。
2. 背包仍以大弹窗覆盖地图、右栏与底部 HUD，导致当前 shell 信息结构混乱。
3. workbench 只做静态图片复刻，无法通过真实 snapshot 驱动 selected item、equipment slot、pagination、empty slot 与 tooltip。
4. item icon 内烘焙数量、热键、页码、中文 tooltip 或装备状态。
5. 通过 localized text、asset path、icon key 字符串拆解来推导 item 规则或装备对比。
6. 可堆叠的同类物品仍占多个 grid cell，或数量 badge 不是由 typed stack/quantity presentation 驱动。
7. 新增正式 PNG / atlas / manifest key 但没有走 resource pipeline gate。
8. 显示参考图中当前没有 typed source 的负重、承重、分类筛选、顶部经济栏或额外真实装备槽。
9. 为了显示示例数据修改核心掉落、装备、存档、profile、shop 或 replay 合同。
10. PR05-1 implementation diff 缺少 `InventoryWorkbenchPresentation.kt` / `InventoryWorkbenchPresenterTest.kt`、PR05-1 golden label registration 或最终 manual record，却把文档合同当作已完成证据。
11. 与 PR05 formal resource replacement 混在同一 PR/提交中，但既没有拆分边界，也没有把 `resourcePipelineLint`、manifest coverage 和 owner-scope evidence 升级为 blocking gate。
12. map mode 下 `PageUp/PageDown` 静默修改隐藏背包 selection，却没有 visible UI feedback 或正式输入合同。

### Preflight Checklist

执行实现前必须确认：

1. 已阅读 `UI/pr/dark-uiux-pr03-equipment-inventory-items.md`，确认 PR-03 已拥有装备、背包、item icon 与 shop/resource 基础合同。
2. 已阅读 `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`，确认 PR05-1 只复用 shell / map / HUD 存在感，不扩张 PR-05 的 map actor replacement 范围。
3. 已阅读 `UI/review/open-design/ktome-dark-ui-design.md`，只吸收暗黑 UI 原则，不把 open-design 当作 runtime truth。
4. 已阅读 `UI/ART_STYLE_BIBLE.md`，确认所有新增 UI 表现符合 `ktome-dark-fantasy-sprite-ui-v1`，避免 sci-fi HUD、neon、glassmorphism、高饱和大描边。
5. 已确认 reference PNG / prompt 不在 Kotlin、manifest、resource registry 或 runtime loader 中引用。
6. 已检查当前实现所需字段是否已经存在于 snapshot / presenter；缺字段时优先做 typed client presentation contract，不从 localized string、tooltip 文本或 icon 文件名反推规则。

## 1. 阶段目标

PR05-1 要解决的问题是：当前背包界面在 K-ToME 暗黑 tile shell 中不可用，信息层级混乱，modal 过大，地图、右栏、tooltip、底部 HUD 与背包内容互相压叠；同时缺少清晰的 grid focus、装备区、详情区、对比区和键盘操作反馈。

本 PR 的目标：

1. 将背包打开态改成一个可玩的全屏 inventory workbench，而不是覆盖式 debug modal。
2. 保留暗黑游戏窗口、左 rail、地图底层氛围、底部 HUD 的低亮存在感，让玩家仍知道自己在 run 内。
3. 建立稳定三栏结构：左侧装备 9-slot 人形视觉区，中间 6x4 backpack grid，右侧 selected item detail + current equipment compare。
4. 让 focus、selection、pagination、empty slot、quality、quantity、hotkey、tooltip 都有明确视觉表达。
5. 保持键盘优先，鼠标 hover 是增强路径，不是 committed selection 的唯一来源。
6. 不改变规则权威，不引入第二套 inventory / equipment state，不把参考图变成 runtime asset。

完成后，玩家在背包页应能一眼回答：

1. 当前选中哪个物品。
2. 它是什么、能否装备或使用、会替换什么。
3. 背包还有多少空位、当前在哪一页。
4. 常用动作对应哪个键。
5. 退出后会回到当前 run，而不是进入另一个 debug 工具。

## 2. 影响范围

### 2.1 Must Touch

PR05-1 runtime implementation must touch exactly these ownership surfaces unless a new acceptance row is added first:

1. `UI05-1-PRESENTATION-01` / `UI05-1-GRID-01` / `UI05-1-STACK-01` / `UI05-1-DETAIL-01`: `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt` and `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt`。
2. `UI05-1-GRID-01` / `UI05-1-DETAIL-01` / `UI05-1-SOCKET-01` / `UI05-1-REMOVAL-01`: client renderer / shell layout branch that draws `UiMode.INVENTORY` from the workbench model。
3. `UI05-1-INPUT-01`: client input focus, pagination, hover-preview and explicit selection handling。
4. `UI05-1-DETAIL-01`: typed detail / compare / action row presentation and locale keys only when production code references them。
5. `UI05-1-EVIDENCE-01`: deterministic golden scenario, four PR05-1 golden labels and final manual record evidence。
6. `UI05-1-ROUTE-01`: `UI/pr/README.md`, `UI/pr/screen-coverage-matrix.md` and acceptance lint routing。

### 2.2 Allowed Touch Boundary

Only the following files/modules are allowed for PR05-1, and each touch must map to a listed acceptance row:

1. client-only `InventoryWorkbenchPresentation` / `InventoryWorkbenchSelectionState` for `UI05-1-PRESENTATION-01`, `UI05-1-GRID-01`, `UI05-1-STACK-01`, `UI05-1-DETAIL-01`。
2. item presentation adapter，用于把已有 typed snapshot 转成显示行；只在 `InventoryWorkbenchPresenterTest` 证明现有 snapshot 字段不足时触碰。
3. localized key 表，只用于 footer/action token、empty state、disabled reason 和 visual-only socket reason；必须同步 `localeLint`。
4. tests / fixtures / golden scenario builder，只用于 required focused tests 和四个 PR05-1 golden labels。

### 2.3 Must Not Touch

本 PR 不允许触碰：

1. core inventory / equipment / combat 规则。
2. loot budget、drop table、shop offer legality。
3. save / replay / profile schema。
4. official content pack schema。
5. runtime manifest、sprite sheet manifest、atlas key registry。
6. PR-05 map tile、actor、boss telegraph、portrait resource owner keys。
7. item icon PNG 内容，除非另开正式资源化子任务并走 resource pipeline。

## 3. Reference Image Contract

### 3.1 Reference Source

PR05-1 的参考图：

```text
UI/dark-uiux-pr03-inventory-page-reference.png
UI/dark-uiux-pr03-inventory-page-reference.prompt.txt
```

参考图固定含义：

1. 这是 one-off development reference image。
2. 画布尺寸是 `1672x941`，用于对齐当前 shell/golden 密度。
3. style tag 是 `ktome-dark-fantasy-sprite-ui-v1`。
4. 它表达 layout、层级、密度、材质、focus ring 与信息组织。
5. 它不表达 runtime asset key、manifest key、真实 item rule、真实装备 stat 或最终本地化文案。

### 3.2 Art Direction

实现应保留以下视觉原则：

1. 背景是低亮 dungeon shell，workbench 是主焦点。
2. 主面板使用 dark iron / worn stone / smoky brown-black 材质，边缘细碎但不花。
3. selected focus 使用 restrained cyan，不使用大面积 neon。
4. 可装备、稀有、可交互状态使用小面积 ember-gold。
5. item icon 使用 PR-03 resource 风格，数量、热键、页码、tooltip 由 UI 层绘制。
6. 文本必须清晰、短行、无压叠；内部标题不使用 hero-scale 字体。
7. panel 之间有足够 gutter，禁止 card 套 card。

### 3.3 Anti-Reference Drift

以下不是参考图合同，不得照抄：

1. 示例 `长剑` 的具体数值。
2. 示例 tooltip 的排版像素。
3. 示例 item 清单的具体掉落概率或经济含义。
4. 任何图片里不清晰或乱码的文本。
5. 任何当前游戏没有 typed source 的规则系统。

### 3.4 Reference Exclusion Table

| Reference element | Current typed source exists? | Show in PR05-1? | Future prerequisite | Verification |
| --- | --- | --- | --- | --- |
| 负重 / 承重 / item weight | no | no | core/game 冻结 item weight 与 carry capacity 合同 | exact denylist scan for `ui.inventory.weight`, `burden`, `encumbrance`, `carryWeight`, `weightCapacity`, `负重`, `承重` in PR05-1 production diff |
| 分类筛选 tab | no stable filter identity | no | inventory sorting/filtering PR，且 grid identity 不依赖排序位置 | exact denylist scan for inventory-owned tab/filter keys such as `ui.inventory.filter` and `ui.inventory.category_tab`; do not scan generic `category` / `filter` terms |
| search / sort | no | no | 独立 UX + input contract | no search/sort command in footer |
| 顶部金币 / 经济栏 | shop/economy not owned by inventory workbench | no | shop/economy surface PR 明确纳管 | no top economy strip in PR05-1 workbench |
| 额外真实装备槽 | only existing typed slots are real | visual-only disabled affordance only | core/game 增加装备槽合同 | visual socket mapping test |
| 参考图示例物品名和数值 | no | no | deterministic fixture from existing item snapshot | test asserts no hardcoded sample stat text |

容量可以显示 item count / page capacity，例如 `items/capacity` 或 `Page 1/2`。不得把容量写成重量、负重、承重或任何暗示移动/拾取规则的词。

## 4. UX Contract

### 4.1 Layout Profiles

背包 page 打开后使用四层结构：

1. **Shell Layer**: 左 rail、地图暗底、底部 HUD 低亮存在，证明仍在 run 内。
2. **Workbench Frame**: 主容器占据中心大区域，不遮挡窗口 chrome，不贴边。
3. **Inventory Content**: 左 equipment、中 backpack grid、右 detail / compare。
4. **Action Footer**: 固定底部键盘帮助、页码、容量、当前 mode。

Implementation must satisfy these layout profiles:

| viewport | equipmentColumnMinWidth | gridColumnMinWidth | detailColumnMinWidth | gutter | footerHeight | gridCellSizeRange | iconInnerPadding | detailMaxLines | overflowPolicy |
| --- | ---: | ---: | ---: | ---: | ---: | --- | --- | ---: | --- |
| `1672x941` | `300px` | `520px` | `400px` | `16px` | `64px` | `64px..72px` | `8px..10px` | `7` | all 24 cells visible; footer never overlaps grid |
| `1280x800` | `240px` | `440px` | `320px` | `14px` | `60px` | `58px..64px` | `6px..8px` | `6` | all 24 cells visible; compare/action rows stay inside detail pane |
| `1024x768` | `190px` | `384px` | `280px` | `10px` | `56px` | `52px..56px` | `4px..6px` | `5` | all 24 cells visible; footer visible; action footer does not overlap grid |

At `1024x768`, the workbench uses `4px..6px` icon inner padding and clamps detail content to max `5` lines. At every listed viewport it must keep all three columns visible, preserve the 6x4 grid, and keep footer hints out of item cells.

### 4.2 Visual Socket Mapping

左侧装备区视觉上使用 9-slot 人形布局，但规则上只能显示当前项目已有 typed equipment slot。visual-only socket 只能作为 disabled/empty affordance，不参与 identity、action、save、compare。

| visualSocketIndex | position | typedSlotId? | labelToken | enabled | tooltipToken | disabledReasonToken | compareTargetCue |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | weapon hand | existing weapon slot | `ui.sidebar.weapon` | yes when slot exists | existing slot tooltip | `null` | yes for weapon candidate |
| 2 | off hand | existing off-hand slot if available | `ui.sidebar.off_hand` | yes when slot exists | existing slot tooltip | `null` | yes for off-hand candidate |
| 3 | armor body | existing armor slot | `ui.sidebar.armor` | yes when slot exists | existing slot tooltip | `null` | yes for armor candidate |
| 4 | accessory / inscription anchor | existing accessory-like slot if available | `ui.reward.slot.accessory` | yes when slot exists | existing slot tooltip | `null` | yes when matching typed slot exists |
| 5 | head visual socket | none | `null` | no | `null` | `ui.inventory.slot.visual_only_unavailable` | no |
| 6 | cloak visual socket | none | `null` | no | `null` | `ui.inventory.slot.visual_only_unavailable` | no |
| 7 | gloves visual socket | none | `null` | no | `null` | `ui.inventory.slot.visual_only_unavailable` | no |
| 8 | boots visual socket | none | `null` | no | `null` | `ui.inventory.slot.visual_only_unavailable` | no |
| 9 | ring visual socket | none | `null` | no | `null` | `ui.inventory.slot.visual_only_unavailable` | no |

如果当前 implementation 的 real slot 数不是 4，表中 `typedSlotId?` 必须按现有 typed source 调整；不得为了凑 9-slot 视觉增加 core/game rule slot。

当前真实 slot label helper 是 `client/src/main/kotlin/com/ktome/client/render/EquipmentSlotLabels.kt`，已存在 token 为 `ui.sidebar.weapon`、`ui.sidebar.off_hand`、`ui.sidebar.armor`、`ui.reward.slot.accessory`。Visual-only sockets must be emitted as `visualOnly=true`, `enabled=false`, `labelToken=null`, `tooltipToken=null`, and `disabledReasonToken=ui.inventory.slot.visual_only_unavailable`; they must not participate in equipment identity, action, save or compare.

### 4.3 Backpack Grid

中间 grid：

1. 默认 6x4，每页 24 个 slot。
2. 每个 cell 有稳定尺寸，hover、focus、quantity badge 不改变 cell 大小。
3. selected cell 使用 restrained cyan focus ring。
4. item quality 使用小面积 corner pip / trim，不用全格高饱和填充。
5. 同类可堆叠物品必须合并成一个 cell，并以固定 quantity badge 规则展示：quantity `1` 不显示 badge；`2..99` 显示 `xN`；`>=100` 显示 `x99+`。
6. stack count、hotkey、new marker、locked/disabled marker 都在 cell overlay 层，不烘焙进 icon。
7. empty slot 保持低亮金属边框，便于判断容量。
8. pagination 使用 footer 或 grid header 表达，不能塞进 item icon。

### 4.4 Detail + Compare Pane

右侧详情区：

1. 顶部显示 selected item name、quality、slot 或 item type。
2. 中部显示 effect / stat line / requirement / tag，最多保留 5-7 行关键内容。
3. 装备类 item 显示 current equipped compare；非装备 item 显示 use / drop / inspect 行。
4. 对比只展示 typed source 中真实可得的差异。
5. 正向差异用 restrained ember-gold 或 muted green；负向差异用 muted red；不使用高饱和霓虹。
6. 如果没有选中 item，显示 empty selection state，不显示假 tooltip。

### 4.5 Footer and Help

底部 help：

1. 固定显示 arrow keys move、`Enter` inspect/select、`E` equip/use、`D` drop、`PgUp/PgDn` page、`Esc` return。
2. `D` 保留为 drop，不作为右移键；本 PR 不声明 `WASD` grid movement。
3. 如果动作不可用，保留 key label 但置灰，并显示短原因或禁用状态。
4. 容量和页码必须可见，例如 item count capacity 和 `Page 1/2`。
5. Footer 不随 hover tooltip 高度变化，不挤压 grid。

## 5. Inventory Workbench Presentation Model

### 5.1 Model Owner

PR05-1 必须新增 `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt` 作为唯一 workbench presentation owner。Renderer 只能消费这个 model 和既有 shell layout，不得同时消费旧 right-panel grid、modal detail rows 和新的 workbench rows 形成双路径。`EquipmentInventoryPresenter` 可以继续服务 map shell companion，但不得服务 `UiMode.INVENTORY` full-page route。

最小字段：

| Field | Contract |
| --- | --- |
| `equipmentSlots` | 9 visual sockets with typed slot identity only where source exists; visual-only sockets expose `visualOnly=true`, `enabled=false`, `labelToken=null`, `tooltipToken=null`, `disabledReasonToken` |
| `inventoryGrid` | `columns=6`, `rows=4`, `cells.size=24`, stable cell bounds and empty cells |
| `stackGroups` / cell stack metadata | typed stack identity, representative entry ids, quantity text and stack action target |
| `selectedEntryId` | committed keyboard/click selection, not hover preview |
| `hoveredCell` | pointer preview only, never overwrites committed selection |
| `focusedCell` | keyboard focus coordinates and entry id |
| `pageIndex` / `pageCount` | model-owned pagination |
| `capacityText` | item count capacity, not weight or burden |
| `detailRows` | typed semantic rows for selected item |
| `compareRows` | typed semantic compare rows for equippable item |
| `actionRows` | equip/use/drop/return availability and disabled reasons |
| `footerHints` | localized action hint tokens |
| `emptySelectionState` | typed empty state, not fake tooltip text |
| `equipmentTargetSlotId` | target cue only when existing typed slot matches |

### 5.2 6x4 Grid Assertions

Focused tests must assert:

1. `columns == 6`。
2. `rows == 4`。
3. `cells.size == 24`。
4. `pageSize == columns * rows`。
5. page label and capacity come from the model, not renderer string concatenation。
6. empty cells remain present and have stable ids。
7. page 2 selection keeps entry identity stable when the visible index changes。

### 5.3 Stack Grouping And Quantity Badge Contract

PR05-1 必须把背包内“多个相同物品合并成一格并显示数量”作为 blocking 行为交付。

Stack grouping rules:

1. Stack identity 必须来自 typed stack key 或 typed item identity，不得由 renderer 比较 localized name、display text、iconKey、asset path 或排序位置推导。
2. 默认只合并明确可堆叠且完全同质的物品，例如同种 consumable、material、currency-like item 或未来 typed stackable item。
3. 装备、unique、artifact、带不同 material/affix/special template/stat roll 的 item 默认不自动合并；除非 game/core 先输出明确 stackable identity。
4. 合并后的 cell 必须保留 representative item icon、quality marker、tooltip anchor、entry ids 和 quantity。
5. quantity badge 由 UI overlay 绘制，固定格式为 quantity `1` 不显示 badge；`2..99` 显示 `xN`；`>=100` 显示 `x99+`；不得烘焙进 item icon。
6. selection/action 必须指向 stack cell 的 typed action target；use/drop 单个还是整组由 action row 明确表达，不能让 renderer 猜。
7. 如果当前 `InventoryEntrySnapshot` 不足以表达 stack quantity，PR05-1 必须新增 typed presentation field 或上游 snapshot 字段，并同步 focused tests；不能继续把 `quantityText = null` 当作最终状态。
8. 6x4 容量、page count 和 empty cell 以合并后的 visible stack cells 计算；manual/golden 必须覆盖合并前物品数大于 visible cell 数但合并后仍可读的情况。

Focused tests must cover:

1. two identical stackable consumables collapse into one cell with quantity badge `x2`。
2. ten identical stackable items expose `x10`; ninety-nine expose `x99`; one hundred expose `x99+`。
3. two equipment items with same base id but different affix/stat/special identity do not merge。
4. merged stack selection keeps stable stack identity across pagination。
5. quantity badge does not resize the cell or overlap quality marker / focus ring。

### 5.4 Snapshot Field Policy

如果实现发现现有 snapshot 不足：

1. 首先确认 PR-03 是否已经有对应 presenter 字段。
2. 若只是 UI 派生信息，优先在 client presenter 层新增 typed display model。
3. 若涉及规则真实字段，必须回到当前 phase / core owner 文档评估，不能在 PR05-1 里顺手改 core。
4. 任何跨 module public contract 变化都要更新 contract test 和文档。

### 5.5 Locale Policy

UI 文案必须通过 locale / token 体系：

1. key hints、section title、empty state、disabled reason 必须可本地化。
2. 测试断言优先检查 token / semantic line，不直接依赖整段中文排版。
3. screenshot 可显示中文，但不能出现乱码、裁切、重叠。
4. page label、capacity text 和 footer hints 必须来自 semantic model / token；不得在 presenter 或 renderer 中硬编码拼接 `"PgUp/PgDn"` 这类最终显示字符串作为长期合同。

## 6. Input And Hover State Machine

### 6.1 Required Input

1. 打开背包时进入 inventory focus mode。
2. Arrow left/right/up/down 移动 `focusedCell`。
3. `PgUp/PgDn` 翻页，page step 来自 `columns * rows = 24`。
4. `Enter` inspects or commits the focused cell as selected without using/dropping it。
5. `E` equips or uses selected item when action availability allows。
6. `D` drop selected item。
7. `Esc` return to previous gameplay mode。
8. 背包打开态 map movement 不执行。
9. `PgUp/PgDn` pagination 只在 `UiMode.INVENTORY` 生效；map mode 不得静默修改隐藏背包 selection。若未来保留 map mode 背包翻页快捷键，必须另写 visible UI feedback 和正式输入合同。

### 6.2 Two-Dimensional Focus

二维移动规则：

1. left/right 在同一行内移动；默认不 wrap。
2. up/down 在同一列移动；遇到页边界不跨页，必须用 `PgUp/PgDn`。
3. 空格可以获得 focus；空格 selected 后显示 empty selection state。
4. disabled item 可以获得 focus，但 action row 必须置灰并提供 disabled reason token。
5. page change 后 focus 保持在同一 visual cell 坐标；若目标页 cell 不存在 entry，则 selected state 变为空槽。

### 6.3 Hover Preview

Pointer hover 只能写 `hoveredCell`，不能写 committed `selectedEntryId`。只有 click、`Enter` 或显式 selection command 才能提交 selection。Hover tooltip 可以预览 item detail，但右侧 compare pane 默认以 committed selection 为准；如果未来要让 hover preview 临时刷新 compare pane，必须以单独 visual state 表达，不得覆盖 keyboard selection。

## 7. Typed Detail, Compare And Action Rows

Renderer 不得解析 localized text、icon key、asset path 或 filename 来生成规则语义。Detail / compare / action 必须由 typed display rows 驱动。

最小 row 形态：

| Row | Required fields | Source rule |
| --- | --- | --- |
| `InventoryDetailRow` | token/key, tone, optional value, source field id | only existing item presentation fields |
| `InventoryCompareRow` | stat/effect id, current value, candidate value, delta, tone, source field id | typed stat / effect source only |
| `InventoryActionRow` | action id, label token, shortcut token, availability, disabledReasonToken | input/action availability source |

Rules:

1. 没有 typed delta 的字段必须隐藏。
2. 没有当前装备时，compare pane 显示 typed empty compare state。
3. 非装备 item 不显示 equip action。
4. consumable / material / key / gold / gem 等只显示已有 typed item presentation 能表达的内容。
5. `DescriptionPresenter` 可以产出 semantic display rows；renderer 不能重新解析它最终输出的本地化文本。
6. Focused tests must assert that sample values from the reference prompt do not appear unless produced by test fixture data。

## 8. Non-Goals And Rule Boundaries

PR05-1 不做：

1. 新装备槽规则。
2. item weight、burden、carry capacity 或任何负重系统。
3. inventory sorting / filtering / search。
4. drag-and-drop。
5. stash、shop buy/sell、crafting 或 salvage。
6. item stat rebalance。
7. drop table 或 loot budget 调整。
8. save/replay/profile schema 修改。
9. 新正式 item icon、panel frame、atlas 或 manifest key。
10. PR05 map actor portrait resource replacement 的 owner key 改造。
11. PR06/PR07 的全局 polish 或 accessibility overhaul。

## 9. Removal / Migration Plan

| Item | PR05-1 handling | Owner | Removal / rewrite condition | Regression scan |
| --- | --- | --- | --- | --- |
| old inventory modal body | rewrite to workbench route or delete old centered modal body | client | `UiMode.INVENTORY` renders workbench root frame | scan modal frame references in client input/render/tests |
| old item detail / compare modal rows | migrate into typed workbench detail / compare rows | client | right detail pane owns selected item rows | scan item detail / compare modal text route |
| 8-slot page size | replace with `columns * rows = 24` from workbench model | client | input, presenter and golden all use 6x4 | scan old fixed page-size and 4x2 right-panel constants |
| hover writes committed selection | split into `hoveredCell` and committed `selectedEntryId` | client | hover preview does not change keyboard selection | focused hover test |
| right-panel inventory scaffold | remains map shell companion only; `UiMode.INVENTORY` must not render or source its primary grid from `TileDemoShellModel.backpackSlots` | client | workbench owns full-screen inventory route | focused regression and golden/manual diff |
| old PR02-1 inventory-open label | keep as shell regression only, not PR05-1 evidence | docs/client | PR05-1 labels cover new workbench | screen matrix label inventory |
| reference-only burden/filter/economy UI | explicitly excluded: no UI, no locale key, no placeholder state | docs/client | core/game item weight or inventory filtering contract is approved in its own PR and exposes typed snapshot fields | exact denylist scan described in §3.4, not broad `category` / `filter` grep |
| map-mode backpack pagination side effect | remove from PR05-1 input path or make it a separate visible contract | client | `PageUp/PageDown` only mutate inventory focus/page while `UiMode.INVENTORY` is active | `rg -n "pollBackpackPaging|inventoryPageSize = 8" client/src/main/kotlin/com/ktome/client/input` |
| hardcoded page/footer display strings | migrate to semantic page/footer model and locale tokens | client | presenter exposes page/capacity/action hint tokens; renderer localizes only | `rg -n "PgUp/PgDn|Page " client/src/main/kotlin/com/ktome/client/render client/src/main/kotlin/com/ktome/client/input` |

## 10. Golden And Manual Evidence

### 10.1 Required Golden Labels

| Label | Scenario | Viewport | Expected observation |
| --- | --- | --- | --- |
| `dark-uiux-pr05-1-inventory-workbench` | full inventory workbench with equipment, consumable, material and empty slots | `1672x941` and `1280x800` | three-column workbench, 6x4 grid, footer, shell low-light context |
| `dark-uiux-pr05-1-inventory-compare` | selected equippable item with current equipped target | `1280x800` | selected focus, equipment target cue, typed compare rows, no fake sample stats |
| `dark-uiux-pr05-1-inventory-pagination` | inventory with more than 24 entries | `1280x800` | page count, page change, stable focus and capacity text |
| `dark-uiux-pr05-1-inventory-min-window` | compact workbench | `1024x768` | no text overlap, footer visible, selected row/detail readable |

### 10.2 Deterministic Scenario Requirements

The golden scenario must declare:

1. `scenarioId=dark-uiux-pr05-1-inventory-page-workbench`。
2. fixed seed。
3. release profession and locale。
4. at least 25 inventory entries to force pagination。
5. at least one equippable item matching an existing typed slot。
6. at least one stackable consumable or material group with quantity `>= 2`。
7. one consumable, one material-like item, one key-like item and several empty cells。
8. one long zh-CN item name and one long en-US item name for no-overlap coverage。
9. no item weight, category filter, top economy strip or unsupported equipment slots。

### 10.3 Manual Record

Manual record path:

```text
UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md
```

This file starts as a pending manual record template during contract wiring. Before PR05-1 can close, every required evidence row must have real gate output, screenshot/log paths, cleanup status and result. The four required golden labels and manual screenshots cannot be skipped; skipped-with-reason is allowed only for non-blocking supplementary records and cannot replace `seed`, `screenshotPath`, `logPath`, `cleanupStatus` or `result`.

Manual record table fields:

| Field | Required |
| --- | --- |
| `evidenceLabel` | yes |
| `scenarioId` | yes |
| `seed` | yes |
| `inputSequence` | yes |
| `viewport` | yes |
| `screenshotPath` | yes |
| `logPath` | yes |
| `expectedObservation` | yes |
| `cleanupStatus` | yes |
| `residualRisk` | yes |

人工白盒必须记录：

1. 打开背包后的全屏 workbench 截图。
2. 选中装备 item 的 detail + compare 截图。
3. 选中 consumable / material / empty slot 的状态截图。
4. 翻页前后截图。
5. 最小窗口或窄窗口无重叠截图。
6. 键盘操作路径：open -> move focus -> enter inspect/select -> equip/use attempt -> drop disabled/enabled -> page -> escape。
7. 视觉反例检查：无 sci-fi HUD、无 neon、无 glassmorphism、无中文乱码、无过亮边框、无 text overlap。

## 11. Implementation Order

### 11.1 Step 1 - Design Contract Wiring

1. 在 `UI/pr/README.md` 中把 PR05-1 插入 PR05 和 PR06 之间。
2. 在 `UI/pr/screen-coverage-matrix.md` 中把 backpack grid / equipment panel 的 owner 扩展为 `PR-03 + PR-05-1 + PR-07`。
3. 在 `Phase4V4AcceptanceContractLintTest` 中纳管 `UI05-1` 文档。
4. 添加 PR05-1 manual white-box record 模板。
5. 不修改 sprite sheet plan 或 resource key registry。

### 11.2 Step 2 - Presentation Model

1. 新增 `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt` 和 `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt`。
2. 明确 `selectedEntryId`、`pageIndex`、`pageCount`、`capacityText`、`focusedCell`。
3. 将 item detail 行和 compare 行作为 typed display rows。
4. `EquipmentInventoryPresenter` 仅保留 map shell companion，不得承载 PR05-1 full-page route。
5. 用 focused tests 固定 empty/full/selected/equipped/disabled/page state。

### 11.3 Step 3 - Renderer Layout

1. 绘制 workbench container、equipment column、grid、detail pane、footer。
2. 使用现有 dark UI frame helper 和 PR05 shell 视觉，不新增 image asset。
3. 固定 grid cell 尺寸和 responsive constraints。
4. 增加 golden screenshot scenario。

### 11.4 Step 4 - Input and Interaction

1. 打开背包时进入 inventory focus mode。
2. arrow keys 移动 focus。
3. `PgUp/PgDn` 翻页。
4. `Enter` inspect/select，`E` equip/use，`D` drop，`Esc` return。
5. 输入不可穿透到 map movement。
6. 鼠标 hover 同步 `hoveredCell`，但不覆盖 keyboard selection。

### 11.5 Step 5 - Verification Close

1. 跑 focused presenter / renderer / input tests。
2. 跑 `:client:clientSmoke :client:goldenScreenshot`。
3. 补 manual white-box screenshots / notes。
4. 跑 `localeLint contractLint maintainabilityLint verifyChanged`。
5. 做 doc-vs-implementation self-audit，确认没有 reference image runtime leak。

## 12. Required Tests

### 12.1 Unit / Presenter Tests

需要覆盖：

1. empty inventory 显示 6x4 empty grid 和 empty detail。
2. 选中 equippable item 时，detail pane 有 name / quality / slot / action rows。
3. 当前装备存在时，compare pane 只显示 typed delta。
4. non-equippable item 不显示 equip action。
5. stackable identical items collapse into one grid cell and expose quantity in typed overlay display model。
6. stack quantity 不写入 iconKey、asset path 或 item icon bitmap。
7. non-stackable equipment / unique / artifact items with same base id do not merge unless typed upstream source marks them stackable。
8. page 2 selection 不改变 entry identity 或 stack identity。
9. disabled action 有 disabled state，不吞掉 key hint。
10. visual-only equipment sockets expose `disabledReasonToken=ui.inventory.slot.visual_only_unavailable` and do not become action/save/compare targets。
11. reference prompt 示例数值不会硬编码出现。

### 12.2 Input Tests

需要覆盖：

1. 背包打开态 arrow keys 移动 focus。
2. `PgUp/PgDn` 翻页并保留合法 focus。
3. `Esc` 返回原 gameplay mode。
4. 背包打开态 map movement 不执行。
5. hover 只改变 hover state，不覆盖 keyboard selected item。
6. `D` 执行 drop，不参与 grid movement。

### 12.3 Renderer / Golden Tests

需要覆盖：

1. desktop 1672x941 workbench full inventory matching §4.1 layout profile。
2. 1280x800 standard workbench matching §4.1 layout profile。
3. 1024x768 no-overlap matching §4.1 layout profile。
4. selected cell cyan focus ring 可见。
5. equipment target cue 可见。
6. footer 不遮挡 grid。
7. right detail pane 文本不裁切。
8. 长 item name、中文宽字、英文长词、quality marker、stack count badge 不互相重叠。
9. quantity badge 在 selected / hover / disabled / empty-neighbor 状态下不改变 grid cell 尺寸。

## 13. Rollback Boundary

如果实现引入 regression，回滚方式：

1. 回滚 client inventory page layout / presenter / input 改动。
2. 回滚 PR05-1 golden screenshot 和 manual record。
3. Reference PNG / prompt 是 PR05-1 design reference；若删除它，必须同步更新本文件、README 和 screen matrix，不能留下断链引用。
4. 不需要迁移存档或修复 manifest，因为本 PR 不应触碰 runtime schema 或 formal resource manifest。

如果只是不满意视觉细节，不应回滚规则或 input 层；优先调整 renderer spacing、contrast、focus ring、text wrapping 和 golden baseline。

## 14. PR Close Checklist

合并前必须确认：

1. PR05-1 已在 `UI/pr/README.md` 中有独立条目。
2. `UI/pr/screen-coverage-matrix.md` 已标记 inventory page workbench owner。
3. `acceptanceContractLint` 已纳管 `UI05-1`。
4. Reference image 没有被 Kotlin、manifest、key registry 或 runtime loader 引用。
5. build report 等 ignored 产物未提交。
6. 所有提交文件中无机器绝对路径、本机临时目录路径或平台私有路径。
7. focused tests、client smoke、golden、locale / contract / maintainability / verifyChanged 结果已记录。
8. manual white-box record 包含截图路径、操作路径、失败项与剩余风险；不能仍停留在 `PENDING_IMPLEMENTATION` 模板状态。
9. doc-vs-implementation self-audit 明确说明本 PR 没有改变背包规则、装备规则、资源 manifest 或存档合同。
10. 若同一工作区存在 PR05 resource / manifest / sheet 改动，PR05-1 提交或 PR 描述必须证明这些文件已拆分到其他提交/PR；否则按资源 PR 重新纳入 `resourcePipelineLint` 与 owner-scope evidence。
