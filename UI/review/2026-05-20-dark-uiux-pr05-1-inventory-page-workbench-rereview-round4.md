# Dark UI/UX PR05-1 Inventory Page Workbench Round 4 Review

审查日期: 2026-05-20

结论: **PR05-1 仍不能按完成态关闭**。本轮已经修复上一轮报告中的多项文档精确性问题，并新增了 `Pr05InventoryReferenceExclusionLintTest`，`acceptanceContractLint` 与定向 `contractLint` 均通过；但 runtime workbench 仍未实现，当前玩家路径仍由旧 4x2 / 8-slot right-panel inventory、旧 modal detail 和旧 input selection 逻辑驱动。

本报告只保留必须整改项。没有可选优化项。

## Findings

### P0

- 本轮未发现 save、replay、profile schema、manifest version 或 core/game 规则合同的不可逆破坏。PR05-1 当前主要风险仍是 required client behavior 未落地、证据缺失和 PR 边界混杂。

### P1

#### P1-1. `UiMode.INVENTORY` 仍没有 PR05-1 workbench owner，运行时仍走旧 right-panel inventory presentation

证据:

- PR05-1 文档要求新增唯一 owner `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:31`, `:63`, `:291`。
- 当前仓库没有 `InventoryWorkbenchPresentation.kt` 和 `InventoryWorkbenchPresenterTest.kt`。
- 当前 `TileRenderModel` 仍用 `EquipmentInventoryPresenter.present(...)` 生成 `TileDemoShellModel.backpackSlots` 和 `equipmentInventory`: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:980-1017`。
- 当前 `EquipmentInventoryPresentation.empty()` 和 request 默认值仍是 `columns=4`, `visibleRows=2`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:49-83`。

必须整改:

1. 新增 `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt`。
2. 新增 `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt`。
3. `UiMode.INVENTORY` 必须由 `InventoryWorkbenchPresentation` 驱动 full-page workbench；`TileDemoShellModel.backpackSlots` 只能服务 map shell companion。
4. `EquipmentInventoryPresenter` 不能继续作为 PR05-1 full-page owner。
5. `InventoryWorkbenchPresenterTest` 必须断言 root model 至少包含 `equipmentSlots`, `inventoryGrid`, `stackGroups`, `selectedEntryId`, `hoveredCell`, `focusedCell`, `pageIndex`, `pageCount`, `capacityText`, `detailRows`, `compareRows`, `actionRows`, `footerHints`, `emptySelectionState`, `equipmentTargetSlotId`。

验收条件:

- `rg --files | rg 'InventoryWorkbenchPresentation|InventoryWorkbenchPresenterTest'` 必须返回两个 PR05-1 文件。
- `TileRenderModel` 中 `UiMode.INVENTORY` 必须有明确 full-page workbench 分支。
- `rg -n "TileDemoShellModel.backpackSlots|EquipmentInventoryPresenter.present" client/src/main/kotlin/com/ktome/client/render` 不得命中 PR05-1 full-page path。

#### P1-2. 输入合同仍和 PR05-1 文档直接冲突

证据:

- PR05-1 要求 `PageUp/PageDown` 只在 `UiMode.INVENTORY` 生效: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:371`, `:377`。
- `pollMapCommand` 仍调用 `pollBackpackPaging(snapshot)`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:443-445`。
- `pollBackpackPaging` 在 map mode 下修改 `inventorySelection`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:509-525`。
- PR05-1 要求 arrow left/right/up/down 二维移动: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:369-371`, `:381-387`。
- `pollInventoryCommand` 只处理 up/down，没有 left/right: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:870-891`。
- PR05-1 要求 hover 只写 `hoveredCell`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:389-391`。
- 当前 hover 在 inventory mode 直接写 committed `inventorySelection`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1710-1715`。
- 测试仍把 map mode 翻页和 hover 提交 selection 当正确行为: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1305-1328`, `:1389-1394`, `:1444-1499`。

必须整改:

1. 删除 map mode 对 `pollBackpackPaging` 的调用。
2. PR05-1 inventory focus 必须建模为 `focusedCell(row, column, pageIndex)`。
3. left/right 必须在同一行移动，up/down 必须在同一列移动，默认不 wrap。
4. PageUp/PageDown 必须按 `columns * rows = 24` 翻页，并保持同一 visual cell 坐标。
5. Hover 只能更新 preview state，不能写 `inventorySelection` / `selectedEntryId`。
6. 只有 click、Enter 或显式 selection command 能提交 selection。
7. `D` 必须继续 drop，不得参与右移。

验收条件:

- `InputHandlerTest` 必须有以下 case: map mode PageUp/PageDown 不改 inventory selection；inventory left/right/up/down 二维移动；PageDown 保持 visual cell 坐标；hover 不改 selected entry；Enter 提交/inspect；D drop。
- `rg -n "pollBackpackPaging\\(|inventorySelection = hoveredInventory" client/src/main/kotlin/com/ktome/client/input` 不得命中违反 PR05-1 的路径。

#### P1-3. 6x4 grid、stack grouping、quantity badge 仍未实现，旧测试还锁定 8-slot / `quantityText=null`

证据:

- 文档要求 `columns=6`, `rows=4`, `cells.size=24`, `pageSize=24`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:257`, `:316-320`。
- `InputHandler` 仍固定 `inventoryPageSize = 8`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:122`。
- `EquipmentInventoryPresenter` 默认 4x2，并按 entry 逐个生成 cell: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:72-83`, `:133-184`。
- 所有 inventory cell 的 `quantityText` 仍是 `null`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:149-164`。
- `EquipmentInventoryPresenterTest` 断言 `quantityText == null`，并断言 page 2 从 index `8` 开始: `client/src/test/kotlin/com/ktome/client/render/EquipmentInventoryPresenterTest.kt:63-107`。

必须整改:

1. PR05-1 grid page size 必须从 workbench model 的 `columns * rows` 得出，且固定为 `24`。
2. PR05-1 stack cell 必须包含 `stackId`, `entryIds`, `representativeItem`, `quantity`, `quantityText`, `actionTarget`。
3. quantity badge 固定规则必须落实到实现: `1` 不显示，`2..99` 显示 `xN`，`>=100` 显示 `x99+`。
4. 装备、unique、artifact、不同 affix/stat/special identity 的 item 默认不得合并。
5. 6x4 capacity、pageCount、empty cells 必须按 visible stack cells 计算。
6. 删除或改写当前 `quantityText == null` 与 page index `8` / `16` 的 PR05-1 路径断言。

验收条件:

- `InventoryWorkbenchPresenterTest` 必须覆盖 `x2`, `x10`, `x99`, `x99+`。
- `InventoryWorkbenchPresenterTest` 必须覆盖同 base id 不同 affix/stat 的装备不合并。
- `TileRendererCanvasTest` 必须断言 quantity badge 不改变 cell 尺寸，且不覆盖 quality marker / focus ring。

#### P1-4. Typed detail / compare / action rows 仍不存在，renderer 仍在拼装 tooltip 和 compare text

证据:

- 文档要求 `InventoryDetailRow`, `InventoryCompareRow`, `InventoryActionRow`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:393-412`。
- 当前 production/test 没有 `InventoryActionRow`, `InventoryDetailRow`, `InventoryCompareRow`。
- `TileRenderModel.itemTooltip` 仍拼装 `TileTextLine` body lines: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:2640-2670`。
- `equipmentComparisonLines` / `statDeltaLines` 仍直接本地化并拼接 delta text: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:2673-2735`。

必须整改:

1. `InventoryWorkbenchPresentation.kt` 必须定义 typed row types。
2. `InventoryDetailRow` 必须包含 `token/key`, `tone`, `optionalValue`, `sourceFieldId`。
3. `InventoryCompareRow` 必须包含 `statOrEffectId`, `currentValue`, `candidateValue`, `delta`, `tone`, `sourceFieldId`。
4. `InventoryActionRow` 必须包含 `actionId`, `labelToken`, `shortcutToken`, `availability`, `disabledReasonToken`。
5. Renderer 只能绘制 typed rows，不能解析 localized text、icon key、asset path 或 filename。

验收条件:

- `rg -n "InventoryActionRow|InventoryDetailRow|InventoryCompareRow" client/src/main/kotlin client/src/test/kotlin` 必须命中 production model 和 focused tests。
- `InventoryWorkbenchPresenterTest` 必须覆盖 equippable、non-equippable、empty selection、no current equipped target、disabled action reason。

#### P1-5. Golden/manual evidence 仍是模板状态，PR05-1 玩家路径没有可验收证据

证据:

- 文档要求四个 PR05-1 golden labels: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:65`, `:450-453`。
- `GoldenScreenshotHarnessTest` 只注册到 PR05 的 `dark-uiux-pr05-map-layer-stack` 和 `dark-uiux-pr05-actor-boss-telegraph`: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:156-160`, `:243-251`。
- production/test 路径中没有 PR05-1 golden labels。
- manual record 仍包含 `PENDING_IMPLEMENTATION` 和 `not-run`: `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md:12-20`, `:26-29`, `:42-45`。

必须整改:

1. 注册四个 golden labels: `dark-uiux-pr05-1-inventory-workbench`, `dark-uiux-pr05-1-inventory-compare`, `dark-uiux-pr05-1-inventory-pagination`, `dark-uiux-pr05-1-inventory-min-window`。
2. 新增 deterministic scenario `dark-uiux-pr05-1-inventory-page-workbench`。
3. Scenario 必须包含至少 25 个 visible entries、一个 equippable item、一个 stackable group、一个 consumable、一个 material-like item、empty cells、长 zh-CN 名称、长 en-US 名称。
4. manual record 必须替换所有 `PENDING_IMPLEMENTATION` / `not-run` 为真实 repo-relative artifact path、hash、gate result 和残余风险。
5. 四个 required golden/manual evidence 不允许 skip。

验收条件:

- `rg -n "dark-uiux-pr05-1-inventory-(workbench|compare|pagination|min-window)" client/src/test/kotlin game/src/main client/src/main UI/manual-records` 必须命中 golden registration、scenario setup 和 manual evidence。
- `rg -n "PENDING_IMPLEMENTATION|not-run" UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md` 必须无命中。

#### P1-6. PR05-1 仍和 PR05 resource/manifest 改动混在同一工作区，违反当前文档边界

证据:

- PR05-1 文档声明不生成正式 runtime asset，不新增 sprite sheet，不进入 manifest，不修改 `sheet-plan.yaml` / `key-registry.yaml` / canonical atlas: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:7`, `:51`, `:147-155`, `:612`。
- 当前工作区仍包含 `UI/sprite-sheets/key-registry.yaml`, `UI/sprite-sheets/sheet-plan.yaml`, `assets-src/image/manifests/phase2-visual-manifest.json`, `client/src/main/resources/manifests/visual-manifest.json`, `client/src/main/resources/dark-v1/...`, `assets-src/image/raw/sheets/dark-v1/...`, `assets-src/image/contact-sheets/dark-v1/...`。
- 当前 `git diff --stat` 仍显示 34 个 tracked files、7246 insertions、1515 deletions，并伴随大量 untracked runtime PNG / raw sheets / contact sheets。

必须整改:

1. PR05-1 提交必须只包含 PR05-1 docs、client workbench implementation、focused tests、golden/manual evidence、acceptance/contract lint。
2. PR05 formal resource / manifest / sheet plan / runtime PNG 必须拆到独立 PR 或独立提交。
3. 若同一 PR 强行承载两类改动，必须把 `resourcePipelineLint`, manifest coverage, owner-scope evidence 升级为 blocking gate，并在 PR 描述中写清 PR05 resource owner scope。

验收条件:

- PR05-1 final diff 不得包含 `UI/sprite-sheets/key-registry.yaml`, `UI/sprite-sheets/sheet-plan.yaml`, `assets-src/image/raw/sheets/dark-v1`, `assets-src/image/contact-sheets/dark-v1`, `client/src/main/resources/dark-v1`，除非该 PR 同时声明为 resource PR 并提供 resource gates。

### P2

#### P2-1. 新增 reference exclusion lint 已接入，但 fixture 覆盖不完整

证据:

- `Pr05InventoryReferenceExclusionLintTest` 的 denylist 有 9 个 token: `tools/src/test/kotlin/com/ktome/tools/lint/Pr05InventoryReferenceExclusionLintTest.kt:92-103`。
- 负向 fixture 只断言 `ui.inventory.weight` 和 `carryWeight`: `tools/src/test/kotlin/com/ktome/tools/lint/Pr05InventoryReferenceExclusionLintTest.kt:22-37`。
- 定向执行 `./gradlew :tools:contractLint --tests com.ktome.tools.lint.Pr05InventoryReferenceExclusionLintTest --rerun-tasks` 已通过。

必须整改:

1. `reference exclusion denylist reports exact forbidden tokens` 必须覆盖全部 9 个 denylist token。
2. 测试必须断言每个 token 都能产生 `ReferenceExclusionFinding`。
3. 测试必须覆盖至少两个生产根: `client/src/main/kotlin` 和 `game/src/main/resources/i18n`。
4. 扫描范围排除文档/review/prompt 的行为必须用测试固定，不能只靠 production root 常量间接表达。

验收条件:

- 在 fixture 文本中放入全部 denylist token 时，expected findings 数量必须等于 `FORBIDDEN_TOKENS.size`。
- 测试必须包含一个 doc/review/prompt path fixture，断言不进入 production scan。

#### P2-2. reference exclusion lint owner 命名缺少 `PR05-1`，容易和 PR05 resource owner 混淆

证据:

- 新增类名是 `Pr05InventoryReferenceExclusionLintTest`: `tools/src/test/kotlin/com/ktome/tools/lint/Pr05InventoryReferenceExclusionLintTest.kt:11`。
- 该 lint 的文档 owner 是 `UI05-1-REF-EXCLUSION-01`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:30`。
- PR05 已是 map actor portrait replacement，且当前工作区存在大量 PR05 resource 改动。

必须整改:

1. 类名必须改成 `Pr05_1InventoryReferenceExclusionLintTest` 或 `Pr05InventoryWorkbenchReferenceExclusionLintTest`。
2. 文档 artifact 行必须同步新类名。
3. 定向验证命令必须使用新类名。

验收条件:

- `rg -n "Pr05InventoryReferenceExclusionLintTest" UI tools` 无命中。
- `./gradlew :tools:contractLint --tests <new-class-name> --rerun-tasks` 必须通过。

#### P2-3. §11.2 Implementation Order 仍允许“新增或扩展 display model”，和 §5.1 唯一 owner 冲突

证据:

- §5.1 已写必须新增 `InventoryWorkbenchPresentation.kt`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:291`。
- §11.2 仍写“新增或扩展 inventory page display model”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:516`。

必须整改:

1. §11.2 第 1 条必须改成: `新增 client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt 和 InventoryWorkbenchPresenterTest.kt`。
2. 删除“或扩展”。
3. §11.2 必须显式写明 `EquipmentInventoryPresenter` 仅保留 map shell companion，不得承载 PR05-1 full-page route。

验收条件:

- `rg -n "新增或扩展 inventory page display model" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。

#### P2-4. Visual-only socket disabled reason 已写入文档，但当前 model/locale 没有对应落点

证据:

- 文档 rows 5-9 要求 `disabledReasonToken=ui.inventory.slot.visual_only_unavailable`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:243-251`, `:560`。
- 当前 `EquipmentSlotCellModel` 只有 `visualOnly`，没有 `enabled` / `disabledReasonToken`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:16-26`。
- 当前 locale 中没有 `ui.inventory.slot.visual_only_unavailable`。

必须整改:

1. `InventoryWorkbenchEquipmentSlot` 必须包含 `visualOnly`, `enabled`, `disabledReasonToken`。
2. `ui.inventory.slot.visual_only_unavailable` 必须进入 `en-US` 和 `zh-CN` locale。
3. `InventoryWorkbenchPresenterTest` 必须断言 visual-only sockets `enabled=false`, `disabledReasonToken=ui.inventory.slot.visual_only_unavailable`。
4. Visual-only sockets 不得作为 action/save/compare target。

验收条件:

- `rg -n "ui.inventory.slot.visual_only_unavailable" game/src/main/resources/i18n client/src/main/kotlin client/src/test/kotlin` 必须命中 locale、production model/test。

#### P2-5. §2.1 Must Touch 仍使用预计口径，实际可改边界必须直接绑定 acceptance row

证据:

- §2.1 写“后续实现预计会触碰”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:126-135`。
- §2.2 已经改成 allowed touch boundary: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:137-143`。

必须整改:

1. §2.1 标题保持 `Must Touch`，正文必须改成 `PR05-1 runtime implementation must touch exactly these ownership surfaces`。
2. 每一项必须加 requirementId: presentation、renderer、input、description rows、golden scenario、docs route。
3. 不在 §2.1 列出的 production surface，必须走 §2.2 触发条件或 PR boundary note。

验收条件:

- `rg -n "预计会触碰" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。

### P3

#### P3-1. `May` 英文口径仍出现在 layout 降级规则中

证据:

- §4.1 写 `The workbench may reduce icon inner padding and detail line count...`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:231`。

必须整改:

1. 改成强制条件: `At 1024x768 the workbench reduces icon inner padding to <exact px/range> and detail lines to max 5`。
2. 对 1672x941 / 1280x800 / 1024x768 三行分别写明 detail maxLines 和 icon padding range。

验收条件:

- `rg -n "\\bmay\\b" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。

#### P3-2. `future typed upstream PR` 出现在 removal table，缺少具体迁移触发条件

证据:

- Removal table 的 reference-only row 写 `future typed upstream PR exists`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:440`。

必须整改:

1. 改成具体触发条件: `core/game item weight or inventory filtering contract is approved in its own PR and exposes typed snapshot fields`。
2. 同一行必须写明 PR05-1 当前处理: no UI, no locale key, no placeholder state。

验收条件:

- `rg -n "future typed upstream PR exists" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。

## Requirement Alignment

- Requirement: PR05-1 路由、reference artifact、manual template、reference exclusion lint 必须接入文档治理。
  Evidence: `UI/pr/README.md` 已加 PR05-1；`UI/pr/screen-coverage-matrix.md:38-39`, `:77-80` 已加 label；`Phase4V4AcceptanceContractLintTest` 已纳管 `UI05-1`；新增 `Pr05InventoryReferenceExclusionLintTest` 且定向 contractLint 通过。
  Conclusion: **部分一致**。文档/lint 层已基本闭合；新增 lint fixture 覆盖仍不足。

- Requirement: PR05-1 必须新增唯一 workbench presentation owner。
  Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:291`；当前无 `InventoryWorkbenchPresentation.kt` / `InventoryWorkbenchPresenterTest.kt`。
  Conclusion: **不一致**。

- Requirement: 背包 grid 必须 6x4、page size 24、stack-aware。
  Evidence: `InputHandler.kt:122` 固定 8；`EquipmentInventoryPresenter.kt:79-80` 默认 4x2；`EquipmentInventoryPresenter.kt:153`, `:164` 固定 `quantityText=null`。
  Conclusion: **不一致**。

- Requirement: Hover preview 不得覆盖 committed selection，PageUp/PageDown 只在 inventory mode 生效。
  Evidence: `InputHandler.kt:443-445`, `:509-525`, `:1710-1715`；`InputHandlerTest.kt:1305-1328`, `:1389-1394`, `:1444-1499` 仍锁定旧行为。
  Conclusion: **不一致**。

- Requirement: Golden/manual evidence 必须证明 workbench、compare、pagination、min-window。
  Evidence: `GoldenScreenshotHarnessTest.kt:156-160`, `:243-251` 没有 PR05-1 labels；manual record 仍是 pending template。
  Conclusion: **不一致**。

- Requirement: PR05-1 不生成正式资源、不改 manifest/sheet plan。
  Evidence: 当前工作区仍混入 `sheet-plan`, `key-registry`, manifests, runtime PNG, raw sheets, contact sheets。
  Conclusion: **不一致**。

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| 文档路由 | PR05-1 进入 README、screen matrix、acceptance lint | 部分实现 | `UI/pr/README.md`, `UI/pr/screen-coverage-matrix.md:38-39`, `Phase4V4AcceptanceContractLintTest` | 路由已补；runtime evidence 未补 | Medium |
| Reference exclusion lint | 禁止负重/筛选/承重进入 production surfaces | 部分实现 | `Pr05InventoryReferenceExclusionLintTest.kt:10-104` | contractLint 已跑通；fixture 只覆盖 2/9 tokens | Medium |
| Workbench presentation | 唯一 `InventoryWorkbenchPresentation` owner | 未实现 | `UI/pr/...:291`; `rg` 无文件 | 仍是 `EquipmentInventoryPresenter` right-panel path | High |
| 6x4 grid | 24 cells, page size 24, capacity/page from model | 偏离实现 | `InputHandler.kt:122`; `EquipmentInventoryPresenter.kt:79-80` | 仍是 8-slot / 4x2 | High |
| Stack badge | typed stack grouping + `xN` / `x99+` | 未实现 | `EquipmentInventoryPresenter.kt:153`, `:164`; `EquipmentInventoryPresenterTest.kt:88` | 实现和测试都锁 `quantityText=null` | High |
| Input state machine | 2D focus、inventory-only pagination、hover preview only | 偏离实现 | `InputHandler.kt:443-445`, `:870-891`, `:1710-1715` | map mode 改隐藏 selection；hover 提交 selection | High |
| Detail/compare/action rows | typed rows, renderer only paints | 未实现 | `TileRenderModel.kt:2640-2735` | renderer 拼装 tooltip/compare text | High |
| Golden/manual evidence | 四个 PR05-1 labels + final manual | 未实现 | `GoldenScreenshotHarnessTest.kt:156-160`; manual record | 无 PR05-1 runtime screenshot evidence | High |
| PR boundary | PR05-1 不混正式资源 | 偏离实现 | `git status --short`, `git diff --stat` | PR05 resource/manifest/sheet 改动仍混在工作区 | High |

## 玩法与体验审查

### 核心循环

当前玩家打开背包后仍进入旧 modal/right-panel 逻辑，无法形成文档要求的“run 内全屏 workbench”。这会让地图、HUD、背包选择、详情和底部动作提示继续处在旧壳层混合状态，玩家不能稳定判断当前物品、目标装备槽、可执行动作和退出路径。

### 成长与构筑驱动

装备候选对比仍不是 typed workbench compare rows。玩家无法从稳定右侧 detail/compare pane 中看到装备替换收益，也无法确认 disabled action reason。对类 ToME 的装备构筑判断，这是当前阶段必须修复的基础体验。

### 奖励驱动与掉落体验

stack grouping 未实现，消耗品/材料仍按单 entry 占格。背包容量、分页和奖励回收体验会被重复物品噪音放大；一旦掉落量增加，玩家会把 UI 噪音理解为背包容量紧张或系统不整理。

### 新手体验与信息反馈

map mode PageUp/PageDown 仍会静默改隐藏 selection；hover 仍会提交 selection。这两个行为都会破坏“键盘 selection 是玩家确认状态”的学习模型，新玩家会在 item detail、drop/use、tooltip 之间看到不稳定反馈。

### 系统耦合与体验断层

PR05-1 当前仍和 PR05 resource replacement 混在同一工作区。玩家体验层面的 workbench 问题和资源替换问题被同一 diff 承载，会让 reviewer 无法判断截图变化来自 UI 结构还是资源替换。

## 当前阶段必须解决的问题

1. **必须先落 `InventoryWorkbenchPresentation.kt`。**
   - 当前 Phase 必须修，因为所有 grid、stack、detail、action、golden 和 input contract 都依赖这个 owner。
   - 不能推迟，因为继续用 `EquipmentInventoryPresenter` 会把 full-page workbench 建在 right-panel companion 上。
   - 修复方向: 新建 model + presenter test，`UiMode.INVENTORY` 切到 workbench root。

2. **必须替换 8-slot / 4x2 / line selection 输入路径。**
   - 当前 Phase 必须修，因为 PR05-1 的核心体验是 6x4 workbench。
   - 不能推迟，因为旧 input tests 正在把错误行为锁成回归基线。
   - 修复方向: `focusedCell(row,column,pageIndex)` + page size 24 + hover preview split。

3. **必须把 stack/detail/action 做成 typed rows。**
   - 当前 Phase 必须修，因为背包页不是纯视觉页面，玩家要基于它做装备/use/drop 决策。
   - 不能推迟，因为 renderer 继续拼 text 会制造规则语义第二路径。
   - 修复方向: typed stack cell + `InventoryDetailRow` / `InventoryCompareRow` / `InventoryActionRow`。

4. **必须拆清 PR05 resource 改动和 PR05-1 workbench 改动。**
   - 当前 Phase 必须修，因为 PR05-1 文档明确不生成正式资源。
   - 不能推迟，因为混合 diff 会导致 resource gates 与 client workbench gates 双方都无法单独验收。
   - 修复方向: PR05-1 final diff 不包含 formal resource/sheet/manifest/runtime PNG，或显式升级为 resource PR 并补 resource gates。

5. **必须用 golden/manual evidence 证明玩家路径。**
   - 当前 Phase 必须修，因为这是 player-facing UI。
   - 不能推迟，因为 `acceptanceContractLint` 和 `contractLint` 只证明文档/lint，不证明 1024x768、长文本、stack badge、compare pane 可用。
   - 修复方向: 四个 labels + deterministic scenario + final manual record。

## Removal/Iteration Plan

### Defer Removal: old right-panel inventory scaffold

| Field | Details |
| --- | --- |
| Location | `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt`, `TileRenderModel.kt`, `InputHandler.kt` |
| Phase/Work Package | PR05-1 |
| Touched contract | client presentation/input |
| Evidence | 当前 full-page inventory 仍依赖 old companion path |
| Preconditions | `InventoryWorkbenchPresentation` 成为 `UiMode.INVENTORY` 唯一 owner；map shell companion 调用点保留独立语义 |
| Deletion or iteration steps | 1. 新增 workbench owner 2. `UiMode.INVENTORY` 切换到 workbench 3. 旧 right-panel scaffold 限定为 map shell companion 4. 删除旧 modal detail/compare 主路径依赖 |
| Affected harness/gates | `InventoryWorkbenchPresenterTest`, `InputHandlerTest`, `TileRendererCanvasTest`, `:client:clientSmoke`, `:client:goldenScreenshot`, `maintainabilityLint` |
| White-box check | 打开背包、移动 focus、hover、翻页、compare、drop/use、Esc 返回 |
| Rollback or fallback | 仅回退 workbench route；不得恢复 map mode hidden pagination |

## Optional Items

无可选改进项。本轮列出的全部条目都是必须整改或必须验证的内容。

## Suggested Verification

已运行并通过:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint contractLint
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:contractLint --tests com.ktome.tools.lint.Pr05InventoryReferenceExclusionLintTest --rerun-tasks
```

下一轮实现完成后必须运行:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.InventoryWorkbenchPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew localeLint contractLint maintainabilityLint verifyChanged
```

如果最终 diff 仍包含正式资源、manifest、sheet plan 或 runtime PNG，还必须运行并记录:

```bash
./gradlew resourcePipelineLint
```

并附 PR05 owner-scope manifest coverage evidence。

## Summary

本轮修改把文档/lint 层推进了一步：reference exclusion 已经有可执行 contractLint，layout、visual-only socket、quantity badge、manual skip 口径也更明确。阻断点没有转移：PR05-1 的 runtime workbench 仍未实现，旧 input/render/presenter/test 仍与文档直接冲突，golden/manual evidence 仍为空，工作区仍混有 PR05 resource surface。下一轮必须进入实现收口，不应继续只追加文档措辞。
