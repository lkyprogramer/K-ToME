# Dark UI/UX PR-05-1《Inventory Page Workbench》深度审查

- 审查日期: 2026-05-20
- 审查对象: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`
- 参考图: `UI/dark-uiux-pr03-inventory-page-reference.png`
- 上游合同: `AGENTS.md`, `docs/INDEX.md`, `docs/review/rule/pr-level-review-standard.md`, `UI/PLAN.md`, `UI/ART_STYLE_BIBLE.md`, `UI/pr/README.md`, `UI/pr/development-governance.md`, `UI/pr/screen-coverage-matrix.md`, `UI/pr/dark-uiux-pr03-equipment-inventory-items.md`, `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`
- 当前实现抽查: `EquipmentInventoryPresenter`, `InputHandler`, `DemoShellLayout`, `TileRenderModel`, `Phase4V4AcceptanceContractLintTest`
- 审查结论: **request_changes**。方向正确，但当前文档还不能直接进入实现。必须先补齐 PR 路由、lint 纳管、可执行验收矩阵、6x4 workbench 状态模型、输入/hover 语义和白盒证据合同。

## Findings

### P0

- **F-P0-1: PR05-1 还没有接入 dark UI/UX 执行真源与 `acceptanceContractLint`，当前文档会成为孤立合同。**
  - 证据:
    - `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:24-35` 使用 `ID / Owner / Contract / Fast Gate / Release Gate` 五列表，而 `UI/pr/development-governance.md` 要求 `requirementId / source / owner / fastCheck / ownerGate / artifact / whitebox`。
    - 目标文档没有出现 `development-governance.md`, `docs/verification/README.md`, `docs/rule/ai-change-governance.md`, `### Acceptance Matrix`, `### Gate Budget`, `### Canonical Artifact`, `### Failure Rule`, `build/verification/verify-changed/full-task-duration-summary.{json,md}` 这些当前 lint 检查项。
    - `UI/pr/README.md:17-19` 执行顺序仍是 `PR-05 -> PR-06 -> PR-07`，没有 PR05-1；`UI/pr/README.md:23` 串行推进规则也没有插入 PR05-1。
    - `UI/pr/screen-coverage-matrix.md:38-39` 装备面板和背包 grid owner 仍是 `PR-03 + PR-07`，没有 PR05-1。
    - `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt:153-205` 的 `uiPrDocs` 没有 `UI05-1` 条目。
  - 影响: 文档即使写得再详细，当前 gate 也不会强制检查它；PR05-1 的 artifact、whitebox、owner、screen coverage 和 close gate 都可能被开发者或 CI 漏掉。这不是格式问题，而是开发合同不被仓库治理系统消费。
  - 修复方向:
    1. 在 `UI/pr/README.md` 执行顺序、串行推进规则、Evidence Matrix 中插入 PR05-1。
    2. 在 `UI/pr/screen-coverage-matrix.md` 的装备面板、背包 grid、golden/manual label inventory 中加入 PR05-1 owner 与 label。
    3. 在 `Phase4V4AcceptanceContractLintTest` 增加 `PrDoc(requirementPrefix = "UI05-1", path = "UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md", minimumRows = 8)` 或等价纳管。
    4. 把目标文档第 0 节改成 governance 固定结构和字段，不要保留并行矩阵口径。

### P1

- **F-P1-1: 6x4 workbench 的 presentation model 没有冻结，开发者无法从文档唯一推导实现边界。**
  - 证据:
    - 文档要求三栏 workbench、6x4 grid、detail/compare/action footer: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:89-90`, `:213-219`, `:221-239`, `:288-290`。
    - 当前 `EquipmentInventoryPresenter` 默认仍是 `4 x 2`、page size 由 request 计算，模型只包含 equipment slots 和 grid，没有 `pageIndex/pageCount/capacity/focusedCell/detailRows/compareRows/actionRows/disabledReasons`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:38-47`, `:72-83`, `:139-184`。
    - 当前 `DemoShellLayout` 的 right panel backpack 仍固定 `rows = 2`, `columns = 4`: `client/src/main/kotlin/com/ktome/client/render/layout/DemoShellLayout.kt:254-295`。
  - 影响: 文档说的是新的全屏背包工作台，但没有把旧 right-panel scaffold、modal detail、tooltip route 如何被新 presentation contract 替换写成可执行接口。实现者可能只把 4x2 右栏放大，或者继续在 renderer 中拼 detail/compare 文本，达不到“可长期使用的 workbench”目标。
  - 修复方向:
    - 明确新增或扩展一个单一 client presentation owner，例如 `InventoryWorkbenchPresentation`，字段至少包括 `equipmentSlots`, `inventoryGrid(columns=6, rows=4)`, `selectedEntryId`, `focusedCell`, `pageIndex`, `pageCount`, `capacityText`, `detailRows`, `compareRows`, `actionRows`, `footerHints`, `emptySelectionState`, `equipmentTargetSlotId`。
    - 在文档中说明旧 `EquipmentInventoryPresentation` 是复用、扩展还是被 workbench adapter 包装；避免 renderer 同时消费旧 right-panel grid 和新 full-screen model。
    - Required Tests 中给出具体断言: `columns == 6`, `visibleRows == 4`, `cells.size == 24`, page label 与 capacity 由 model 输出，不从 renderer 拼接。

- **F-P1-2: 输入合同与当前实现冲突，但文档没有冻结新的二维 focus 语义。**
  - 证据:
    - 文档要求方向键或 WASD 移动 focus、`PgUp/PgDn` pagination、`Enter/E` equip/use、hover 不覆盖 keyboard selection: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:32`, `:236-239`, `:302-307`, `:333-339`。
    - 当前 `InputHandler` page size 是 `8`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:121-123`。
    - 当前 inventory 只处理 `UP/W` 和 `DOWN/S` 的线性移动，没有 left/right / A/D 网格移动；`D` 已经被 drop 使用: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:870-907`。
    - 当前 hover 会直接写入 `inventorySelection`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1710-1715`，现有测试也把 hover 改 selection 当作正确行为: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1389-1393`。
    - 现有 page test 锁定 `PageDown` 从 3 跳到 8、再到 16: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1443-1498`。
  - 影响: 6x4 grid 下，二维移动、page step、hover preview 和 keyboard selection 是背包高频体验的核心。如果不冻结，`WASD` 会和 `D` drop 冲突，hover 仍可能抢走选中项，玩家在比较装备时会出现“鼠标路过就换选中”的误操作。
  - 修复方向:
    - 明确 `A/D` 是否用于左右移动；若 `D` 保留 drop，则不要写 `WASD`，改成 `Arrow keys` 或新增替代键。
    - 冻结二维移动规则: left/right/up/down 如何跨行、是否 wrap、空格如何跳过、页边界如何处理。
    - 分离 `hoveredCell` 与 `selectedCell`: hover 只能影响 tooltip/preview，不写入 committed selection；点击或 Enter 才提交选择。
    - 把 `inventoryPageSize` 改为来自 grid model 的 `columns * rows = 24`，并新增 tests 替换现有 8-slot page 断言。

- **F-P1-3: Detail + Compare 的 typed row 合同不足，容易回退到 sidebar 文本拼接或 localized text 反推。**
  - 证据:
    - 文档要求 detail/compare 只展示 typed source，可用/不可用 action 需要禁用原因: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:223-239`, `:245-258`, `:321-329`。
    - 当前 detail 和 compare 仍在 `TileRenderModel` 的 sidebar/modal 分支中拼 row，selected item 后又遍历整个 inventory list: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1517-1572`。
    - 当前 tooltip compare 由 `equipmentComparisonLines` 动态生成，typed delta 只覆盖 `ItemStatModifierSnapshot` 的 stat 差异，action availability、use/equip/drop disabled reason 没有统一 display row owner: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:2536-2705`。
  - 影响: 文档已经意识到不能解析 localized text，但没有把“可显示字段、隐藏字段、禁用动作、无 delta、无装备、非装备 item”固化成 typed display rows。实现时很容易继续在 renderer/sidebar 中拼中文、截断行和 compare 逻辑，形成第二套 detail authority。
  - 修复方向:
    - 在文档中定义 `InventoryDetailRow`, `InventoryCompareRow`, `InventoryActionRow` 的最小字段: token/key、tone、numeric delta、availability、disabledReasonToken、source field。
    - 明确哪些 stat/effect 已有 typed source；没有 typed delta 的字段必须隐藏，并用 focused test 断言不会显示示例数值。
    - 把 `DescriptionPresenter` 的职责写清楚: 它可以产出 semantic display rows，但 renderer 不能重新解析它的文本。

- **F-P1-4: 白盒与 golden 证据不可执行，仍停留在“建议 label / 建议路径”。**
  - 证据:
    - Golden label 写成“建议”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:352-358`。
    - Manual record 也是“建议路径”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:360-366`。
    - Manual checklist 没有 scenarioId、seed、inputSequence、screenshotPath、logPath、cleanupStatus、golden index entry、最小窗口尺寸这些 PR03 已经使用过的证据字段: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:368-376`。
  - 影响: 背包 workbench 是玩家可见、高频、布局敏感界面，不能只靠开发者自由截图。没有 deterministic scenario 和 artifact 字段，review 无法判断“full inventory / empty / long item / consumable / material / compare / pagination / min window”是否被同一输入稳定复现。
  - 修复方向:
    - 把三个 golden label 改为必填，并写入 `UI/pr/screen-coverage-matrix.md` 和 `GoldenScreenshotHarnessTest` 预期。
    - 新增固定 scenario，例如 `dark-uiux-pr05-1-inventory-page-workbench`，声明 seed、职业、装备、24+ 背包物品、空槽、长名称、可装备、不可装备、消耗品、材料。
    - Manual record 必须是表格: `evidenceLabel / scenarioId / inputSequence / viewport / screenshotPath / logPath / expectedObservation / cleanupStatus / residualRisk`。

- **F-P1-5: 参考图中存在但当前游戏没有或不需要的系统没有被列成硬性排除清单，尤其是负重/承重。**
  - 证据:
    - 参考图显示了 `负重 18/40`、顶部金币栏、分类 tab、9 个具体装备部位等完整 ARPG 背包视觉，但目标文档的 `Anti-Reference Drift` 只排除了示例数值、像素、概率和乱码: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:171-175`。
    - 文档的数据源只列出 inventory/equipment snapshot、PR03 icons、typed item presentation 等现有来源，没有 item weight、encumbrance、carry capacity 或负重规则来源: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:245-251`。
    - 文档已把 sorting/filtering/search 写为非目标: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:382-389`，但没有明确说参考图里的 `全部/武器/护甲/消耗品/材料` 分类 tab 当前不得实现或显示。
    - 当前实现能看到的是 item count capacity / page 语义和已有 typed equipment slot；没有发现正式 item weight/encumbrance display source。`EquipmentInventoryPresenter` 当前只输出 equipment slots 与 grid: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:38-47`, `:85-184`。
  - 影响: 这是最容易被参考图误导的部分。若开发者为了“贴图还原”添加负重、承重、分类筛选、顶部金币 workbench 栏或真实头部/披风/手套/鞋子/戒指槽，就会把一个 UI PR 扩成规则/schema/client state 变更，并制造第二权威；若完全不说明，又会让 review 误以为这些缺失是实现遗漏。
  - 修复方向:
    - 在目标文档增加 `Reference Exclusion Table`，逐项列出“图上有，但当前阶段不得实现/不得显示”的元素。
    - 最低要求包含: `负重/承重/物品重量`、`分类筛选 tab`、`search/sort`、`顶部金币/经济栏`、`未接入规则的额外装备槽`、`参考图示例数值与物品名`。
    - 对每项写清楚 `current source exists?`, `show in PR05-1?`, `future prerequisite`, `verification`。例如负重应写为: 当前无 typed item weight / encumbrance source，PR05-1 不显示、不新增规则、不新增 locale key；未来只有在 core/game 冻结 item weight 合同后才能做。
    - 在 Acceptance Matrix 增加 `UI05-1-REF-EXCLUSION-01`，fast check 至少扫描 `负重|承重|encumbrance|carryWeight|weightCapacity|categoryTabs` 不得进入 production Kotlin、manifest 或 locale，除非同 PR 同步引入上游规则合同。

### P2

- **F-P2-1: 9-slot 人形装备区的“视觉槽 vs 规则槽”映射还不够硬。**
  - 证据:
    - 文档要求 9-slot 人形布局，但规则只显示当前项目已有装备槽: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:199-207`。
    - 当前实现是 4 个 typed slot 加 5 个 visual-only socket: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:85-130`。
  - 风险: 这是类 ToME 心智的关键画面。若不冻结每个视觉位置的 slotId/label/disabled tooltip/equipment target cue，开发者可能让视觉槽看起来像真实规则槽，误导玩家以为可装备头盔、披风、戒指等当前不存在的规则。
  - 修复方向: 在文档中增加一张 `visualSocketIndex -> position -> typedSlotId? -> labelToken -> enabled/disabled -> tooltipToken -> compareTargetCue` 表。明确 visual-only socket 只能是 disabled/empty affordance，不参与 identity、action、save、compare。

- **F-P2-2: Removal/Iteration Plan 不足，旧 modal/sidebar 路径是否删除、保留或迁移没有 owner。**
  - 证据:
    - 文档只说“不再用大 modal”与“回滚 client layout/presenter/input”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:74-75`, `:393-402`。
    - 当前仍有 `ModalFrameKind.INVENTORY`, `ITEM_DETAIL`, `ITEM_COMPARE` 分支和 sidebar rows: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:845-910`, `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1517-1572`。
  - 风险: 如果 workbench 只是新增一条路径，旧 modal 仍可通过 `X/BACKSPACE/F` 或 nav rail 残留入口触发，玩家会在两套背包 UI 之间漂移，测试也会继续保护旧行为。
  - 修复方向: 增加 removal table，列出 `ModalFrameKind.INVENTORY/ITEM_DETAIL/ITEM_COMPARE`、old sidebar inventory rows、old `dark-uiux-pr02-1-demo-shell-inventory-open` label、8-slot page tests 的处理方式: remove / rewrite / keep with reason / delete in PR07。

- **F-P2-3: Canonical Artifact 中混入“后续实现 PR”占位，不满足可开发文档的 artifact 冻结要求。**
  - 证据:
    - `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:52-61` 把 “后续实现 PR 中的 client presenter / renderer / input tests”、“后续实现 PR 中的 golden screenshot evidence”、“后续实现 PR 中的 manual white-box record”列为 canonical artifact。
  - 风险: canonical artifact 应该是可引用、可检查、repo-relative 的具体路径或 label；“后续实现 PR 中”会让 reviewer 无法判断 close gate 是否缺项。
  - 修复方向: 改成具体路径模板和 label: `client/src/test/kotlin/.../InventoryWorkbenchPresenterTest.kt`, `client/build/reports/golden/index.json` label `dark-uiux-pr05-1-*`, `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md`。

- **F-P2-4: Reference image 的治理边界还需要更明确。**
  - 证据:
    - 文档把 reference PNG/prompt 列为 canonical artifact: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:56-58`。
    - Rollback 又说如果本 PR 关闭建议删除 reference，避免形成未使用参考图: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:397-400`。
  - 风险: 同一个文件既是 canonical artifact 又建议 PR close 后删除，语义冲突。后续如果实现者删除参考图，review 失去视觉合同；如果保留，又缺少 hash/size/style 证明和引用规则。
  - 修复方向: 二选一冻结: 要么它是 PR05-1 的长期 design reference，则保留、在 README/screen matrix 中引用、记录尺寸/hash；要么它只是草稿输入，则不要列为 canonical artifact，只列为 non-canonical design input。

### P3

- **F-P3-1: Gate Budget 缺少最近耗时读取方式和失败复盘入口。**
  - 证据: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:37-50` 只有命令清单，没有 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 和 90 分钟/2 次失败复盘回写位置。
  - 修复方向: 按 `UI/pr/development-governance.md` 增加 Gate Budget 字段: heavy task、触发原因、freshness、duration source、failure review path。

- **F-P3-2: 最小窗口合同没有具体断点。**
  - 证据: 文档要求 `1672x941` 和最小支持窗口两个断点: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:197`, `:345-350`，但未写最小窗口尺寸或对应 layout profile。
  - 修复方向: 明确至少覆盖 `1672x941`、`1280x800`、当前 minimum supported window 的像素值，并写入 golden/manual 表。

## Requirement Alignment

- Requirement: PR05-1 作为 inventory page workbench，不改变规则、资源 manifest 或 PR-05 owner key。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:6-9`, `:124-134`, `:378-391`。
  - Conclusion: **部分一致**。非目标写得清楚，但文档路由和 acceptance lint 未接入，无法保证实现期不会漂移。

- Requirement: 全屏 workbench 三栏结构、6x4 grid、detail + compare、footer。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:181-197`, `:209-239`；当前实现 `EquipmentInventoryPresenter.kt:72-83`, `DemoShellLayout.kt:254-295`。
  - Conclusion: **部分一致**。设计目标清楚，当前实现差距明确，但文档没有冻结新 presentation model 和旧路径迁移。

- Requirement: 键盘优先，hover 是增强路径，不覆盖 keyboard selection。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:91`, `:300-307`, `:333-339`；当前实现 `InputHandler.kt:1710-1715` 和测试 `InputHandlerTest.kt:1389-1393`。
  - Conclusion: **不一致**。文档目标正确，但未给出替换当前行为的二维 focus / hover preview 状态机。

- Requirement: 对比只来自 typed source，不硬编码示例数值。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:34`, `:227-229`, `:245-258`；当前 `TileRenderModel.kt:2673-2705`。
  - Conclusion: **部分一致**。边界意识正确，但 detail/compare rows 的 owner 与字段未冻结。

- Requirement: 白盒、golden、manual evidence 可执行。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:341-376`。
  - Conclusion: **部分一致**。覆盖面方向正确，但 label/path/scenario 都是建议态，不足以 close PR。

- Requirement: 参考图仅提供视觉方向，图中不存在于当前游戏的系统必须显式排除。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:171-175`, `:245-251`, `:382-389`；当前 `EquipmentInventoryPresenter.kt:38-47`, `:85-184`。
  - Conclusion: **不一致**。文档没有把负重/承重、分类 tab、顶部经济栏和未接入规则的装备部位列为硬性非目标，无法防止实现期照图扩规则。

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| PR 路由 / governance | PR05-1 是 PR05 和 PR06 之间的独立 workbench PR | 未实现 | `UI/pr/README.md:17-23`, `Phase4V4AcceptanceContractLintTest.kt:153-205` | 未进入 README、screen matrix、acceptanceContractLint | P0 |
| Acceptance Matrix | 每条需求应可追到 owner/gate/artifact/whitebox | 偏离实现 | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:24-35` | 列头与 dark UI governance 不一致，缺 source/artifact/whitebox | P0 |
| Inventory presentation | 6x4 workbench grid、detail/compare/footer | 部分实现 | `EquipmentInventoryPresenter.kt:72-83`, `:139-184` | 当前 4x2 grid，缺 workbench detail/compare/action rows | P1 |
| Layout / renderer | 左 equipment、中 grid、右 detail compare | 未实现 | `DemoShellLayout.kt:254-295`, `TileRenderModel.kt:1517-1572` | 当前是 right panel + sidebar/modal 分支，不是 full-screen workbench | P1 |
| Input/focus | 二维 focus、page、equip/use/drop、hover 不抢 selection | 偏离实现 | `InputHandler.kt:121-123`, `:870-907`, `:1710-1715` | 当前 8-slot page、线性上下、hover 改 selection | P1 |
| Detail/compare typed contract | 只展示 typed delta，不解析 locale/icon/path | 部分实现 | `TileRenderModel.kt:2536-2705` | 有 stat delta helper，但没有 workbench typed row owner 和 action availability | P1 |
| Manual/golden evidence | desktop/min-window/pagination/full-empty/tooltip/no-overlap | 部分实现 | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:341-376` | 只有建议 label/路径，无 deterministic scenario 和 artifact 字段 | P1 |
| Reference-only unsupported systems | 参考图中无规则来源的系统必须显式排除 | 未实现 | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:171-175`, `:245-251`, `:382-389` | 负重/承重、分类 tab、顶部经济栏、额外装备槽没有硬性排除表 | P1 |
| Resource boundary | 不新增正式 runtime asset，不改 manifest | 一致 | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:7`, `:124-134` | 方向正确；reference artifact 语义需二选一 | P2 |

## 玩法与体验审查

### 核心循环

背包 workbench 是 run 内高频中断点。全屏工作台方向正确，因为它能把“获得物品 -> 比较当前装备 -> 决定装备/使用/丢弃 -> 回到地图”的循环变成一次清晰的战术决策，而不是 debug modal。当前文档最大风险是只描述目标画面，没有把 selection、hover preview、action availability 与 compare result 变成稳定 state machine；这会让核心循环仍然靠 UI 偶然行为维持。

### 战斗体验

背包页不能让地图输入穿透，这点文档写对了。但战斗中打开背包时，`E/Enter/D/Esc` 的含义必须非常稳定，尤其是 `D` 同时被写成 WASD 方向的一部分和 drop。若不澄清，玩家会在战斗压力下误丢或误用物品。

### 成长与构筑驱动

装备 compare 是构筑反馈的核心。文档要求 current equipped compare，但没有冻结可展示 stat/effect 的 typed source。若只显示 3 行 stat delta，玩家无法判断构筑收益；若 renderer 拼 tooltip 文本，又会违背单一权威。当前 PR 必须先建立 typed detail/compare rows，哪怕首版只覆盖已有 stat delta，也要明确哪些字段隐藏。

### 奖励驱动与掉落体验

PR03 已解决 item icon 和 inventory 基础表达，PR05-1 应该让掉落奖励进入“可扫描、可比较、可行动”的决策面。6x4 容量、quality pip、empty slot、page/capacity 是奖励节奏的一部分，不能只在视觉稿里存在。

这里必须区分 `物品数量容量` 与 `负重/承重`。前者是当前背包 page/capacity 展示可以依赖的 UI 数据；后者是参考图里的独立规则系统，当前没有 item weight 与 encumbrance source。PR05-1 如果照图显示 `负重 18/40`，玩家会自然认为重量影响拾取、移动或行动成本，这是规则承诺，不是装饰文本。

### 探索与新鲜感

保留地图暗底和 HUD 低亮存在感是正确方向，可以减少“进入另一个 debug 工具”的断层。但 full-screen workbench 需要明确 modal stack 移除或兼容，否则玩家可能在不同入口看到不同背包页，破坏探索连续性。

### 新手体验与信息反馈

新手最需要看到“我现在选中了什么、能做什么、为什么不能做”。文档已经列出这些问题，但 disabled action 的 typed source、短原因 token、empty selection state 和 footer 灰显规则还没有可执行字段。

### 系统耦合与体验断层

PR05-1 夹在 PR05 resource replacement 和 PR06 full manifest 之间是可以接受的，但必须在 README 和 screen matrix 中明确它是 `client/docs` UI workbench PR，不继承 PR05 的 resource owner scope，不反向污染 PR05 的 map/actor evidence。

## 当前阶段必须解决的问题

- **必须先解决 F-P0-1。**
  - 为什么当前 Phase 必须处理: 没有路由和 lint 纳管，后续实现没有仓库级完成定义。
  - 为什么不能推迟: 一旦实现开始，开发者会按孤立文档做，CI 不会发现 acceptance matrix 和 artifact 缺口。
  - 修复方向: README、screen matrix、acceptanceContractLint、目标文档第 0 节同步收口。
  - 优先级: P0。

- **必须冻结 workbench presentation model。**
  - 为什么当前 Phase 必须处理: 这是从 4x2 right panel 到 6x4 full-screen workbench 的核心边界。
  - 为什么不能推迟: 不冻结 model 会导致 renderer/input/test 各自生成一套状态。
  - 修复方向: 新增/扩展 typed presentation model，包含 grid/page/capacity/detail/compare/action/footer。
  - 优先级: P1。

- **必须重写 input / hover 语义。**
  - 为什么当前 Phase 必须处理: 背包高频操作的误用风险直接影响可玩性。
  - 为什么不能推迟: 当前测试已经锁定 hover 覆盖 selection 和 8-slot page；不先改测试，旧行为会继续被保护。
  - 修复方向: 二维 focus、page size 24、hover preview 与 selected 分离、`D` 与 WASD 冲突决策。
  - 优先级: P1。

- **必须让 golden/manual 证据可执行。**
  - 为什么当前 Phase 必须处理: full-screen UI 的主要风险是布局重叠、文字裁切、焦点不清和真实操作路径断裂。
  - 为什么不能推迟: PR07 final polish 不能替 PR05-1 证明当前 workbench 最小闭环。
  - 修复方向: 固定 scenario、seed、labels、manual record 表格、artifact path。
  - 优先级: P1。

- **必须补 Reference Exclusion Table，尤其是负重/承重。**
  - 为什么当前 Phase 必须处理: 当前 PR 的输入是一张强视觉参考图，图里混有 K-ToME 当前不存在的规则系统。
  - 为什么不能推迟: 一旦实现者把 `负重 18/40`、分类 tab 或顶部金币栏做进 UI，就会从纯 client workbench 变成规则/经济/筛选系统扩展，违反 PR05-1 非目标。
  - 修复方向: 在目标文档中逐项标注 `show / do not show / future prerequisite / verification`，并把负重、分类 tab、顶部金币栏、额外装备槽列为默认不显示。
  - 优先级: P1。

## Removal/Iteration Plan

| Item | 当前建议 | Owner | 删除/迁移条件 | 回归扫描 |
| --- | --- | --- | --- | --- |
| `ModalFrameKind.INVENTORY` 旧大弹窗语义 | rewrite 为 workbench root frame，或删除旧 modal body 绘制 | PR05-1 | full-screen workbench route 进入 `UiMode.INVENTORY` 后不再渲染旧居中 modal | `rg -n "ModalFrameKind.INVENTORY|drawModal\\(|ITEM_DETAIL|ITEM_COMPARE" client/src/main/kotlin client/src/test/kotlin` |
| 8-slot inventory page | replace with `columns * rows = 24` from workbench grid model | PR05-1 | presenter/input/golden 全部使用 6x4 | `rg -n "inventoryPageSize = 8|visibleRows: Int = 2|inventoryColumns: Int = 4" client/src` |
| hover writes committed selection | split into `hoveredCell` and committed `selectedCell` | PR05-1 | hover tooltip works without changing keyboard selected item | `rg -n "hoveredInventoryIndex|inventorySelection = hovered" client/src/main/kotlin client/src/test/kotlin` |
| old inventory sidebar rows | remove or downgrade to debug-only after workbench detail pane exists | PR05-1 | detail/compare rows owned by workbench presentation | `rg -n "ui.controls.inventory.close_hint|ui.sidebar.item_detail|ui.modal.item_compare.stub" client/src/main/kotlin` |
| reference-only burden/filter/economy UI | explicitly exclude unless typed upstream source exists | PR05-1 docs | `Reference Exclusion Table` lists `负重/承重`、分类 tab、顶部金币栏、额外装备槽 as non-goals | `rg -n "负重|承重|encumbrance|carryWeight|weightCapacity|categoryTabs" client/src game/src core/src UI/pr` |
| reference PNG/prompt | choose canonical reference or non-canonical scratch | PR05-1 docs | README/screen matrix either reference it permanently or document it as disposable | `rg -n "dark-uiux-pr03-inventory-page-reference" .` |

## Additional Suggestions

- 把 PR05-1 的编号与 `ownerPr` 策略写清楚。它不生成资源，所以不需要 `ownerPr=PR-05-1` 的 sprite owner；但 golden/manual labels 应统一 `dark-uiux-pr05-1-*`。
- 在文档里补一个“首版不做且图上 tab 也不照搬”的排序/filter/search说明: 当前 non-goal 方向正确，但必须明确参考图里的分类 tab 当前不显示；未来若做 sorting/filter/search，也要声明 grid identity 不因排序改变，避免后续把 index 当长期排序真相。
- 对长 item name、中文宽字、英文长词、quality glyph、stack count badge 增加一条 no-overlap focused assertion，而不只依赖 golden。
- 如果文档仍想展示“容量”，只允许使用现有 item count capacity，例如 `items/capacity` 或 page count；不要写成重量、负重、背包承重或任何暗示移动/拾取规则的词。

## Suggested Verification

本次审查没有运行 Gradle 或截图验证；以下是修订文档和后续实现后建议执行的命令。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.render.InventoryWorkbenchPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew localeLint contractLint maintainabilityLint verifyChanged
git diff --check -- UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md UI/pr/README.md UI/pr/screen-coverage-matrix.md
```

如果 reference PNG/prompt 保留为 canonical artifact，建议补一条轻量检查:

```bash
rg -n "generated-images|dark-uiux-pr03-inventory-page-reference" UI/pr UI/review UI/manual-records client/src assets-src UI/sprite-sheets
```

其中 `dark-uiux-pr03-inventory-page-reference` 只允许出现在 PR05-1 文档、README/screen matrix 的 design reference、review/manual record 中，不能出现在 Kotlin、manifest、key registry、runtime loader。

## Summary

PR05-1 的产品方向是正确的: 把背包从小右栏/大 modal 升级成 run 内全屏 workbench，是类 ToME 玩法体验必须补的中枢界面。但当前文档还停在“方向稿 + 部分约束”状态，不是可直接实现的 K-ToME PR 合同。合并或开始实现前，先修 P0 路由/lint 纳管，再把 6x4 model、input/hover、detail/compare rows、whitebox/golden evidence 冻结到可测试粒度。
