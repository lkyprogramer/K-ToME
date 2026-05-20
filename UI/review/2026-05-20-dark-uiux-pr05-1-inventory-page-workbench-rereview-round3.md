# Dark UI/UX PR05-1 Inventory Page Workbench Round 3 Review

审查日期: 2026-05-20

结论: **PR05-1 仍不可按完成态关闭**。本轮修改已经把 PR05-1 接入 README、screen matrix、acceptance lint，并把参考图中的负重、筛选、经济栏、额外装备槽列成排除项；但当前 runtime 仍是旧 4x2/8-slot right-panel inventory 路径，缺少独立 workbench presentation、6x4 grid、stack badge、typed detail/action rows、2D input、PR05-1 golden 和最终 manual evidence。

本报告只列必须整改项。每条整改完成前，PR05-1 不能合并为“已完成背包 workbench”。

## 审查范围

- PR 文档: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`
- 路由文档: `UI/pr/README.md`, `UI/pr/screen-coverage-matrix.md`
- manual record: `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md`
- 验收 lint: `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt`
- 当前实现对照: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`, `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt`, `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`, `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
- 当前测试对照: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`, `client/src/test/kotlin/com/ktome/client/render/EquipmentInventoryPresenterTest.kt`, `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`, `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`

已运行验证:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint
```

结果: `BUILD SUCCESSFUL`。该结果只证明 PR05-1 文档路由、reference PNG 尺寸/hash 和 manual template 路径被 acceptance lint 覆盖；它不证明 runtime workbench 已完成。

## 已修复项

1. `UI/pr/README.md` 已加入 PR05-1 条目，并把串行顺序改成 `PR-05 -> PR-05-1 -> PR-06`。
2. `UI/pr/screen-coverage-matrix.md` 已加入 PR05-1 的 equipment panel、backpack grid 和四个 golden label。
3. `Phase4V4AcceptanceContractLintTest` 已纳管 `UI05-1`，并检查 reference PNG 存在、`1672x941` 尺寸、sha256、prompt 和 manual record 的机器绝对路径。
4. `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 已明确排除负重、承重、筛选 tab、search/sort、顶部经济栏和额外真实装备槽。

## P1 阻断问题

### P1-1. 独立 workbench presentation owner 仍不存在

证据:

- 文档把 `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt` 和 `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt` 写成 PR05-1 canonical artifact: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:31`, `:62-63`, `:87`。
- 当前文件枚举没有 `InventoryWorkbenchPresentation.kt` 和 `InventoryWorkbenchPresenterTest.kt`。
- 当前 render path 仍在 `TileRenderModel` 中用 `EquipmentInventoryPresenter.present(...)` 生成 demo shell right-panel inventory: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:980-1017`。
- 当前 presenter 类型仍是 `EquipmentInventoryPresentation`，默认 `inventoryColumns=4`, `inventoryVisibleRows=2`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:49-83`。

影响:

- PR05-1 的全屏背包 workbench 仍没有单一模型 owner。
- renderer 仍可同时消费旧 right-panel grid、modal detail route 和新文档字段，形成双路径。
- 后续 golden 即使出现，也无法证明 workbench 合同，而只能证明旧 shell right-panel 继续可画。

必须整改:

1. 新增 `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt`，定义 PR05-1 唯一 presentation root。
2. 新增 `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt`。
3. `UiMode.INVENTORY` 的 render model 必须消费 `InventoryWorkbenchPresentation`，不能把 `TileDemoShellModel.backpackSlots` 当作全屏 inventory page 数据源。
4. 旧 right-panel inventory scaffold 只能保留为 map shell companion；`UiMode.INVENTORY` 不能把它作为主背包页。
5. `EquipmentInventoryPresenter` 可继续服务 map shell 装备/背包小面板，但不能作为 PR05-1 workbench owner。

验收条件:

- `rg --files | rg 'InventoryWorkbenchPresentation|InventoryWorkbenchPresenterTest'` 必须返回上述两个文件。
- `InventoryWorkbenchPresenterTest` 必须断言 root model 含 `equipmentSlots`, `inventoryGrid`, `detailRows`, `compareRows`, `actionRows`, `footerHints`, `selectedEntryId`, `hoveredCell`, `focusedCell`, `pageIndex`, `pageCount`, `capacityText`。
- `TileRenderModel` 中 `UiMode.INVENTORY` 必须有明确 workbench root 分支；旧 modal/right-panel route 不得作为主路径。

### P1-2. 6x4 grid 合同未实现，代码和测试仍锁定 8-slot / 4x2 行为

证据:

- 文档要求每页 `columns * rows = 24`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:257`, `:316-320`, `:371`。
- `InputHandler` 仍固定 `inventoryPageSize = 8`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:122`。
- `EquipmentInventoryPresenterRequest` 仍默认 `inventoryColumns=4`, `inventoryVisibleRows=2`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:72-83`。
- 当前 page label 仍硬编码拼接 `"PgUp/PgDn"`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:178-183`。
- 当前 presenter test 仍断言 `2/2  PgUp/PgDn` 且 page 2 从 index `8` 开始: `client/src/test/kotlin/com/ktome/client/render/EquipmentInventoryPresenterTest.kt:93-106`。
- 当前 input test 仍断言 PageDown 进入 index `8`、`16`: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1444-1499`。

影响:

- PR05-1 的核心空间预算没有落地。
- 24-cell 容量、page count、空格稳定性、最小窗口布局均无法验证。
- 当前测试会阻止实现改成 6x4，因为它们仍把 8-slot 作为正确结果。

必须整改:

1. 删除 PR05-1 路径中的固定 `inventoryPageSize = 8`。
2. PR05-1 page size 必须从 `InventoryWorkbenchPresentation.inventoryGrid.columns * rows` 得出，且固定为 `6 * 4 = 24`。
3. `InventoryWorkbenchPresenterTest` 必须覆盖 `columns == 6`, `rows == 4`, `cells.size == 24`, `pageSize == 24`。
4. 现有断言 index `8` / `16` 的 input tests 必须改成 PR05-1 语义: PageDown 从当前 visual cell 坐标翻到下一页同坐标；第一页到第二页的 base index 是 `24`。
5. `pageLabel` 必须从 semantic model 或 locale token 组合，不得在 presenter 里硬编码最终显示字符串。

验收条件:

- `rg -n "inventoryPageSize = 8|2/2  PgUp/PgDn|assertEquals\\(8, handler.overlayState\\(\\)\\.inventorySelection\\)" client/src` 不得命中 PR05-1 正式路径或 PR05-1 tests。
- `InventoryWorkbenchPresenterTest` 必须有 25+ entry case，断言第二页 pageIndex/pageCount/focus identity。

### P1-3. 输入状态机违反 PR05-1 合同

证据:

- 文档要求 PageUp/PageDown 只在 `UiMode.INVENTORY` 生效: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:377`。
- `pollMapCommand` 在 map mode 中调用 `pollBackpackPaging(snapshot)`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:443-445`。
- `pollBackpackPaging` 会在 map mode 修改 `inventorySelection`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:509-525`。
- 当前测试明确把 map mode PageUp/PageDown 修改背包 selection 当作正确行为: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1305-1328`, `:1491-1499`。
- 文档要求 arrow left/right/up/down 二维移动: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:369-371`, `:381-388`。
- 当前 `pollInventoryCommand` 只处理 up/down，未处理 left/right: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:870-891`。
- 文档要求 hover 只写 `hoveredCell`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:389-391`。
- 当前 hover 在 inventory mode 直接写 committed `inventorySelection`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1710-1715`。
- 当前测试也断言 hover 会同步改 `inventorySelection`: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1389-1394`。

影响:

- 玩家在 map mode 按 PageUp/PageDown 会静默改变隐藏背包 selection，没有可见反馈。
- Inventory mode 不是二维 grid focus，而是旧线性列表。
- 鼠标 hover 会污染键盘 selection，右侧 compare/detail 不能稳定表达 committed selection。

必须整改:

1. `pollMapCommand` 不得调用背包分页逻辑。
2. `pollBackpackPaging` 必须只从 `pollInventoryCommand` 或 PR05-1 inventory focus handler 调用。
3. Inventory focus 必须使用 `focusedCell(row, column, pageIndex)`，left/right 在同一行移动，up/down 在同一列移动，默认不 wrap。
4. PageUp/PageDown 必须保持同一 visual cell 坐标；目标页无 entry 时 focus 落到空 cell，selected state 变为空槽。
5. Hover 必须只更新 `hoveredCell` / `hoveredInventoryIndex`，不得写 `inventorySelection` / `selectedEntryId`。
6. 只有 click、Enter 或显式 selection command 才能提交 selected entry。
7. `D` 必须保持 drop action，不参与右移。

验收条件:

- `InputHandlerTest` 必须新增或替换为以下 cases: map mode PageUp/PageDown 不改 inventory focus；inventory left/right/up/down 二维移动；PageDown 保留 visual cell 坐标；hover 不改 selected entry；Enter 才提交/inspect；D 仍 drop。
- `rg -n "pollBackpackPaging\\(|inventorySelection = hoveredInventory" client/src/main/kotlin/com/ktome/client/input` 不得命中违反上述规则的 PR05-1 路径。

### P1-4. Stack grouping 和 quantity badge 仍未实现，现有测试还锁死 `quantityText = null`

证据:

- 文档把 stack grouping 和 quantity badge 写成 blocking 行为: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:326-337`。
- 当前 presenter 对每个 inventory entry 逐条映射 cell，没有 stack grouping: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:133-158`。
- 当前所有 item cell 的 `quantityText` 都是 `null`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:149-164`。
- 当前测试断言所有 `quantityText == null`: `client/src/test/kotlin/com/ktome/client/render/EquipmentInventoryPresenterTest.kt:63-90`。
- renderer 只有绘制 quantityText 的能力，不提供 stack identity、entry ids、quantity 或 action target: `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt:368-377`。

影响:

- 消耗品、材料、钥匙类物品会继续占用多个 cell。
- 6x4 容量、分页和 selection identity 会按 entry count 而不是 visible stack cells 计算。
- Use/drop 对 stack cell 的语义无法表达，renderer 会被迫猜测单个 entry 或整组。

必须整改:

1. `InventoryWorkbenchPresentation` 必须定义 stack cell 元数据: `stackId`, `entryIds`, `representativeItem`, `quantity`, `quantityText`, `actionTarget`。
2. Stack identity 必须来自 typed stack key 或 typed item identity；不得由 localized name、display text、icon key、asset path、排序位置推导。
3. 装备、unique、artifact、不同 affix/stat/special identity 的 item 默认不合并。
4. quantity badge 格式必须固定: quantity `1` 不显示 badge；`2..99` 显示 `xN`；`>=100` 显示 `x99+`。
5. PR05-1 的 page count、capacity 和 empty cells 必须按 visible stack cells 计算。
6. 删除或改写当前 `quantityText == null` 的测试断言。

验收条件:

- `InventoryWorkbenchPresenterTest` 必须覆盖: 两个相同 stackable consumable 合并成一个 cell；10 个相同物品显示 `x10`；100 个相同物品显示 `x99+`；两个同 base id 但不同 affix/stat 的装备不合并；合并后的 stack across pagination selection identity 稳定。
- `TileRendererCanvasTest` 必须断言 quantity badge 与 quality marker / focus ring 不重叠。

### P1-5. Detail / compare / action rows 仍不是 PR05-1 typed workbench rows

证据:

- 文档要求 `InventoryDetailRow`, `InventoryCompareRow`, `InventoryActionRow` 由 typed display rows 驱动: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:393-412`。
- 当前代码没有 `InventoryActionRow`, `InventoryDetailRow`, `InventoryCompareRow` 类型。
- 当前 selected item tooltip 从 renderer/model helper 中拼接 `TileTextLine`: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:2499-2670`。
- 当前 compare rows 在 `equipmentComparisonLines` / `statDeltaLines` 中直接产出 localized text line: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:2673-2735`。

影响:

- 右侧 detail/compare/action pane 没有稳定数据合同。
- 禁用原因、快捷键、可用性、source field id 无法测试。
- Renderer 仍参与业务语义组装，PR05-1 无法证明“只展示 typed source 中真实可得的差异”。

必须整改:

1. 在 `InventoryWorkbenchPresentation.kt` 中定义 typed rows。
2. `InventoryDetailRow` 必须包含 `token/key`, `tone`, `optionalValue`, `sourceFieldId`。
3. `InventoryCompareRow` 必须包含 `statOrEffectId`, `currentValue`, `candidateValue`, `delta`, `tone`, `sourceFieldId`。
4. `InventoryActionRow` 必须包含 `actionId`, `labelToken`, `shortcutToken`, `availability`, `disabledReasonToken`。
5. Renderer 只能绘制这些 rows，不能解析 localized text、icon key、asset path 或 filename。
6. `DescriptionPresenter` 只能作为 semantic row source；最终 localized string 不能反向被 renderer 解析成规则。

验收条件:

- `InventoryWorkbenchPresenterTest` 必须覆盖 equippable、non-equippable、empty selection、no current equipped target、disabled action reason。
- `rg -n "InventoryActionRow|InventoryDetailRow|InventoryCompareRow" client/src/main/kotlin client/src/test/kotlin` 必须命中 production model 和 focused tests。

### P1-6. PR05-1 golden labels 和最终 manual evidence 仍未落地

证据:

- 文档要求四个 PR05-1 golden labels: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:64`, `:446-453`。
- `GoldenScreenshotHarnessTest` 只注册到 PR05 的 `dark-uiux-pr05-map-layer-stack`, `dark-uiux-pr05-actor-boss-telegraph`: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:156-160`, `:243-251`。
- 对 `GoldenScreenshotHarnessTest`, `ClientSmokeHarnessTest`, `ValidationScenarioRegistry`, `ValidationScenarioPresentationCatalog`, `TileRendererCanvasTest` 的 PR05-1 label 搜索没有命中。
- manual record 仍是 `PENDING_IMPLEMENTATION` / `not-run`: `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md:12-20`, `:26-29`, `:42-45`。

影响:

- 当前没有 screenshot 或 packaged/manual 证据证明 1672x941、1280x800、1024x768、pagination、compare、stack badge、long text no-overlap。
- PR05-1 close checklist 中的 evidence 条件未满足。

必须整改:

1. 在 golden harness 中注册四个 labels: `dark-uiux-pr05-1-inventory-workbench`, `dark-uiux-pr05-1-inventory-compare`, `dark-uiux-pr05-1-inventory-pagination`, `dark-uiux-pr05-1-inventory-min-window`。
2. 新增 deterministic scenario `dark-uiux-pr05-1-inventory-page-workbench`，包含至少 25 个 visible entries、一个 equippable item、一个 stackable group、一个 consumable、一个 material-like item、一个 empty slot、长 zh-CN 名称、长 en-US 名称。
3. 运行 `:client:goldenScreenshot` 后，把四个 labels 的结果写入 canonical golden index。
4. 将 manual record 中所有 `PENDING_IMPLEMENTATION` 和 `not-run` 替换为真实 repo-relative artifact path、hash、gate result 和残余风险。
5. 四个 required labels 不允许用 skipped-with-reason 代替；跳过只允许用于非 blocking 附加步骤。

验收条件:

- `rg -n "dark-uiux-pr05-1-inventory-(workbench|compare|pagination|min-window)" client/src/test/kotlin game/src/main client/src/main UI/manual-records` 必须命中 golden registration、scenario/presentation setup 和 manual record。
- `rg -n "PENDING_IMPLEMENTATION|not-run" UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md` 必须无命中。

### P1-7. 当前工作区仍混入 PR05 正式资源/manifest 变更，PR05-1 边界不可合并

证据:

- PR05-1 文档声明不生成正式 runtime asset、不修改 `sheet-plan.yaml` / `key-registry.yaml` / canonical atlas: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:7`, `:51`, `:84`, `:153-155`。
- 当前工作区包含大量 formal resource / manifest / sheet plan 改动: `UI/sprite-sheets/key-registry.yaml`, `UI/sprite-sheets/sheet-plan.yaml`, `assets-src/image/manifests/phase2-visual-manifest.json`, `client/src/main/resources/manifests/visual-manifest.json`, `client/src/main/resources/dark-v1/...`, `assets-src/image/raw/sheets/dark-v1/...`, `assets-src/image/contact-sheets/dark-v1/...`。
- `git diff --stat` 显示 34 个 tracked files、7246 insertions、1515 deletions，其中包含 PR05 resource pipeline surface。

影响:

- Reviewer 无法判断当前提交是 PR05-1 workbench 还是 PR05 formal resource replacement。
- 如果同一 PR 合并，PR05-1 的“不生成资源”合同失效。
- 如果不补 resource gates，正式资源改动会绕过 owner-scope / manifest coverage。

必须整改:

1. PR05-1 提交必须只包含 PR05-1 docs、client workbench implementation、focused tests、golden/manual evidence、acceptance/contract lint。
2. PR05 formal resource / manifest / sheet plan / runtime PNG 改动必须拆到独立 PR 或独立提交，并按 PR05 资源合同执行 resource pipeline gates。
3. 若坚持同一 PR 承载两类改动，PR 描述和 gate list 必须把 `resourcePipelineLint`, manifest coverage, owner-scope evidence 升级为 blocking gate，并明确 PR05 resource owner scope；否则不能按 PR05-1 合同关闭。

验收条件:

- PR05-1 最终 diff 中不得出现 `UI/sprite-sheets/key-registry.yaml`, `UI/sprite-sheets/sheet-plan.yaml`, `assets-src/image/raw/sheets/dark-v1`, `assets-src/image/contact-sheets/dark-v1`, `client/src/main/resources/dark-v1`，除非该 PR 同时声明为 resource PR 并提供对应 gates。
- PR 描述必须逐项列出 PR05-1 文件边界和 PR05 resource 文件边界，不能混写为“UI 优化”。

### P1-8. `UI05-1-REF-EXCLUSION-01` 写了 `contractLint`，但当前没有对应实现

证据:

- Acceptance Matrix 写明 `UI05-1-REF-EXCLUSION-01` 的 ownerGate 是 `contractLint`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:30`。
- 当前 `contractLint` 相关实现没有 PR05-1 denylist scan；关键词 `ui.inventory.filter`, `ui.inventory.category_tab`, `ui.inventory.weight`, `burden`, `encumbrance`, `carryWeight`, `weightCapacity`, `负重`, `承重` 只在文档、参考 prompt 和 review 报告中出现，未在 `tools` contract lint 规则中出现。
- `acceptanceContractLint` 只校验 reference artifact 和文本机器路径，不校验生产代码 denylist: `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt:122-158`。

影响:

- 参考图排除项目前只靠人工 review。
- 后续实现可以把负重、筛选、经济栏词条或 locale key 引入 production surface，而 `contractLint` 不会失败。

必须整改:

1. 新增 `@Tag("contractLint")` 测试，名称固定为 `Pr05InventoryReferenceExclusionLintTest` 或放入现有 `ContractLintTest` 的独立 test case。
2. 扫描范围必须是 production surface: `client/src/main/kotlin`, `client/src/main/resources/manifests`, `assets-src/image/manifests`, `game/src/main/resources/i18n`。
3. 扫描词必须是精确 denylist: `ui.inventory.filter`, `ui.inventory.category_tab`, `ui.inventory.weight`, `burden`, `encumbrance`, `carryWeight`, `weightCapacity`, `负重`, `承重`。
4. 扫描必须排除 PR 文档、manual record、review 报告、reference prompt 和 historical docs。
5. 如果未来有 typed upstream PR 正式引入 item weight 或 filtering，该 lint 必须在同一 PR 改为新合同，并同步 PR05-1 文档说明 owner 迁移。

验收条件:

- `./gradlew contractLint` 必须覆盖该 denylist case。
- 在 production fixture 中临时加入 `ui.inventory.weight` 或 `carryWeight` 时，该 test 必须失败。

## P2 文档精确性问题

### P2-1. §4.1 仍使用非强制比例口径

证据:

- `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:222-231` 使用非强制标题、浮动百分比区间和未定义的 shell layout 接管口径。

影响:

- 开发者无法知道在 1672x941、1280x800、1024x768 下每栏最小宽度、gutter、footer 高度、grid cell 尺寸的硬边界。
- Golden 失败时无法判定是布局错误还是比例浮动允许。

必须整改:

1. 把 §4.1 改成 `Layout Profiles` 表。
2. 表必须逐行写明 `viewport`, `equipmentColumnMinWidth`, `gridColumnMinWidth`, `detailColumnMinWidth`, `gutter`, `footerHeight`, `gridCellSizeRange`, `overflowPolicy`。
3. `1024x768` 行必须写出 exact no-overlap 断言: footer visible、24 cells visible、detail pane maxLines、action footer 不遮挡 grid。
4. 删除非强制标题、approximate width 标题和未定义 shell layout 接管口径。

验收条件:

- §4.1 只保留可测试 layout profile 表，不保留浮动百分比区间或未定义 layout 接管句。
- `TileRendererCanvasTest` 或 layout test 覆盖 `1672x941`, `1280x800`, `1024x768` 三个 profile。

### P2-2. Visual-only socket token 仍是占位短语，不是可实现 token

证据:

- 表格中 rows 5-9 的 `labelToken` 是 `inventory-scoped locked visual token`，`tooltipToken` 是 `inventory-scoped disabled visual-only tooltip`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:243-247`。
- 当前实现 visual-only slots 的 `labelKey=null`, `label=""`, `tooltipAnchorId="visual-socket-$index"`，但 hover 只映射 typed equipment slots: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:115-129`, `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1698-1702`。

影响:

- 文档没有给出 runtime 可查的 locale token。
- 当前 visual-only socket 没有 disabled reason，也没有可验证 tooltip。

必须整改:

1. 文档 rows 5-9 必须改成具体 token，或明确 `labelToken=null`, `tooltipToken=null`, `disabledReasonToken=ui.inventory.slot.visual_only_unavailable`。
2. 若使用 token，必须在 `en-US` 和 `zh-CN` locale 中新增同名 key，并由 `localeLint` 覆盖。
3. Presenter 必须输出 `visualOnly=true`, `enabled=false`, `disabledReasonToken`。
4. Hover/layout test 必须证明 visual-only socket 不参与 equipment identity、action、save、compare，同时可显示 disabled affordance。

验收条件:

- `rg -n "inventory-scoped locked visual token|inventory-scoped disabled visual-only tooltip" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。
- `InventoryWorkbenchPresenterTest` 覆盖 visual-only socket disabled reason。

### P2-3. Model owner 仍写“新增或扩展”和“例如”，与 canonical artifact 冲突

证据:

- canonical artifact 写了 expected owner path: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:62`。
- Failure Rule 写缺少 `InventoryWorkbenchPresentation.kt` 即未完成: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:87`。
- §5.1 又写“新增或扩展一个单一 client presentation owner，例如 `InventoryWorkbenchPresentation`”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:291`。

影响:

- 实现者可以继续扩展 `EquipmentInventoryPresenter`，声称满足“扩展一个 owner”，但 failure rule 又说必须有 `InventoryWorkbenchPresentation.kt`。

必须整改:

1. §5.1 必须改成: `PR05-1 必须新增 client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt 作为唯一 workbench presentation owner`。
2. 若保留 `EquipmentInventoryPresenter`，文档必须写明它仅服务 map shell companion，不服务 `UiMode.INVENTORY` full-page route。
3. 删除 “例如” 和 “或扩展”。

验收条件:

- `rg -n "例如 `InventoryWorkbenchPresentation`|新增或扩展一个单一" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。

### P2-4. Quantity badge 格式仍是可选口径，无法测试

证据:

- 文档在 `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:334` 对 quantity badge 使用可选格式口径。

影响:

- `x100`, `99+`, `x99+`, `100` 都可能被实现为“compact”，测试无法判断。

必须整改:

1. 文档必须固定 quantity badge rule: quantity `1` 不显示 badge；`2..99` 显示 `xN`；`>=100` 显示 `x99+`。
2. focused tests 必须覆盖 `2`, `10`, `99`, `100`。
3. Renderer test 必须覆盖 `x99+` 不超出 cell。

验收条件:

- `rg -n "capped compact form|等价 compact" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。

### P2-5. right-panel inventory scaffold 的保留条件仍含糊

证据:

- Removal table 在 `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:438` 对 right-panel scaffold 保留条件使用开放式口径。

影响:

- 旧 right-panel grid 可以继续承载 full inventory page，只要实现者声称“still needed”。

必须整改:

1. Removal table 必须改成: `right-panel inventory scaffold remains map shell companion only; UiMode.INVENTORY must not render or source its primary grid from TileDemoShellModel.backpackSlots`。
2. 加 focused regression: 打开 inventory 后，主 workbench root 存在；right-panel shell grid 不作为 selection/page source。

验收条件:

- right-panel scaffold 行必须明确限定为 map shell companion，不得留下开放式保留条件。

### P2-6. min-window 仍写 `1024x768 or canonical min-window token`

证据:

- Golden label 表和 renderer/golden tests 写 `1024x768 or canonical min-window token`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:453`, `:579`。
- `UI/pr/screen-coverage-matrix.md:80` 也写 `1024x768 或 canonical min-window`。
- 当前可查文档已有 `1024x768` 作为 shell 最小窗口证据；本 PR05-1 文档没有定义新的 canonical token。

影响:

- Golden label 可以用一个未定义 token 替代 1024x768，导致最小窗口证据缺失。

必须整改:

1. PR05-1 golden/min-window 合同必须固定为 `1024x768`。
2. 若要引入 named token，必须在同一文档定义 token 名、数值、owner test 和引用位置；定义前不得用 token 替代数值。
3. `screen-coverage-matrix.md` 同步改成 `1024x768`。

验收条件:

- `rg -n "or canonical min-window token|或 canonical min-window" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md UI/pr/screen-coverage-matrix.md` 无命中。

### P2-7. Manual record 允许 required evidence skip，关闭口径过宽

证据:

- 文档允许 pending 值替换为 `explicit skipped-with-reason statement approved by the PR contract`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:477`。
- Acceptance Matrix 把 golden/manual whitebox 写成 required: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:32-38`。

影响:

- 四个 required labels 和 manual screenshot 可能被“skipped-with-reason”绕过。

必须整改:

1. §10.3 必须写明四个 required golden labels 和 manual screenshots 不允许 skip。
2. `skipped-with-reason` 只能用于非 blocking 附加记录，并且不能替代 `seed`, `screenshotPath`, `logPath`, `cleanupStatus`, `result`。
3. manual record finalization rule 必须写成: no `PENDING_IMPLEMENTATION`, no `not-run`, no skipped required label。

验收条件:

- manual record close scan 必须检查四个 required labels 都有 real artifact path 和 PASS/FAIL result。

## P3 细节问题

### P3-1. screen matrix 的“选中即详情”与 hover/selection 合同容易混读

证据:

- `UI/pr/screen-coverage-matrix.md:39` 写背包 grid “选中即详情”。
- PR05-1 文档要求 hover 不提交 selection，right detail 默认以 committed selection 为准: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:389-391`。

影响:

- 读 screen matrix 的实现者可能把 hover 或 focus 变化直接解释成 committed selection detail 更新。

必须整改:

1. `UI/pr/screen-coverage-matrix.md:39` 必须改成 `committed selected item drives detail pane; hover only previews tooltip and does not commit selection` 的中文等价表述。
2. 同一行必须保留 `Enter/click commits selection`，不得写成 hover/focus 自动提交。

验收条件:

- screen matrix 中背包 grid 行必须同时出现 committed selection 和 hover preview 区分。

### P3-2. §2.2 `May Touch` 仍使用许可口吻，未把触碰条件写成封闭边界

证据:

- `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:136-143` 标题是 `May Touch`，正文写“如果现有字段不足，可以触碰”。

影响:

- 允许触碰的边界不够封闭，容易被解释为“这些都可以顺手改”。

必须整改:

1. 标题改为 `Allowed Touch Boundary`。
2. 正文改为: `Only the following files/modules may be touched for PR05-1, and each touch must map to a listed acceptance row`。
3. 对 locale、fixtures、golden scenario builder 分别标注触发条件: footer/action token、typed row test、required golden label。

验收条件:

- `rg -n "May Touch|可以触碰" UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` 无命中。

## 开发闭环要求

PR05-1 下一轮提交必须满足以下顺序:

1. 先拆清 PR05 resource/manifest diff 和 PR05-1 workbench diff。
2. 再落 `InventoryWorkbenchPresentation.kt` 和 `InventoryWorkbenchPresenterTest.kt`。
3. 再改 input 2D focus、PageUp/PageDown inventory-only、hover preview not committed。
4. 再落 renderer workbench layout、typed detail/compare/action rows、stack badge。
5. 再注册四个 golden labels 和 deterministic scenario。
6. 最后更新 manual record，清空 `PENDING_IMPLEMENTATION` / `not-run`。

必须运行并记录:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.render.InventoryWorkbenchPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew localeLint contractLint maintainabilityLint verifyChanged
```

若最终 PR 仍包含正式资源/manifest/sheet 变更，还必须补充:

```bash
./gradlew resourcePipelineLint
```

并提供 PR05 owner-scope manifest coverage evidence。
