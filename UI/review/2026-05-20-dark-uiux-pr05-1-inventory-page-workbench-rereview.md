# Dark UI/UX PR-05-1《Inventory Page Workbench》再审报告

- 审查日期: 2026-05-20
- 审查对象: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`
- 审查范围: PR05-1 文档、`UI/pr/README.md`、`UI/pr/screen-coverage-matrix.md`、`acceptanceContractLint` 纳管、当前 inventory / equipment / input / renderer / golden 相关实现表面
- 审查结论: **request_changes**。上一轮 P0 文档治理问题大多已补齐，`acceptanceContractLint` 也已通过；但当前实现仍基本停在 PR02-1/PR03 right-panel/modal 背包路径，没有真正落到 PR05-1 的全屏 6x4 workbench 合同。若这是 PR05-1 实现 PR，仍不能合并。

## Findings

### P0

- **未发现新的 P0。**
  - `acceptanceContractLint` 已纳管 `UI05-1` 并通过。
  - 本轮没有看到 save / replay / profile schema 的不可逆破坏证据。

### P1

- **F-P1-1: PR05-1 的核心 workbench presentation owner 仍不存在，当前 UI 仍是旧 right-panel / modal 路径。**
  - 证据:
    - 文档要求单一 `InventoryWorkbenchPresentation` owner，包含 9-slot、6x4 grid、selected/hover/focused、pagination、capacity、detail/compare/action/footer: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:282-317`。
    - `rg --files | rg 'InventoryWorkbench|dark-uiux-pr05-1'` 只找到文档和 review，没有 `client/src/main/kotlin/.../InventoryWorkbenchPresentation.kt` 或 `InventoryWorkbenchPresenterTest.kt`。
    - 当前 `EquipmentInventoryPresenter` 仍是 `InventoryGridModel(columns, visibleRows)`，默认 `inventoryColumns = 4`, `inventoryVisibleRows = 2`，且 `empty()` 也是 4x2: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:38-47`, `:49-68`, `:72-83`。
    - 当前 shell 仍在 `TileRenderModel.buildDemoShellModel` 中把 `EquipmentInventoryPresenter` 输出塞回右侧 demo shell 的 equipment / backpack slots: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:971-1014`。
  - 影响: 文档已经从“修文档”升级到“必须实现全屏 workbench”，但当前代码没有新 owner、没有 6x4 page model、没有右侧 detail/compare/action pane owner。玩家仍看不到 PR05-1 承诺的背包工作台。
  - 修复方向: 新增或扩展一个真正的 workbench model，并让 `UiMode.INVENTORY` 渲染全屏 workbench root frame；旧 right-panel grid 只能作为 shell companion，不得作为 PR05-1 主实现。

- **F-P1-2: 输入与 hover 状态机仍违背文档，测试还在保护旧行为。**
  - 证据:
    - 文档要求 arrow 二维 focus、`PgUp/PgDn` step 来自 `columns * rows = 24`，hover 只写 `hoveredCell`，不能覆盖 committed selection: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:359-383`。
    - 当前 `InputHandler` 仍固定 `inventoryPageSize = 8`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:121-123`。
    - inventory 打开态只处理 `UP/W` 和 `DOWN/S` 的线性移动，没有 left/right grid movement: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:845-910`。
    - hover 命中背包 cell 后仍直接写 `inventorySelection`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1687-1715`。
    - renderer 又把 `hoveredInventoryIndex ?: inventorySelection` 当作 effective selection: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:980`, `:2499-2504`。
    - 测试明确断言 hover 后 `inventorySelection == 7`，且 page down 仍从 3 跳到 8、16: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1389-1394`, `:1476-1488`。
  - 影响: 这是玩家误操作风险，不是实现细节。鼠标路过会改变 keyboard selection，比较面板会被 hover 抢走；8-slot page step 也直接违背 6x4 workbench。
  - 修复方向: 拆分 `selectedEntryId` 与 `hoveredCell`；把 page size 改为 workbench model 的 `6 * 4`；新增 left/right/up/down 二维 focus tests；删除或改写当前保护旧行为的 hover/page tests。

- **F-P1-3: stack grouping 被文档写成 blocking 行为，但实现仍然没有 quantity / stack identity。**
  - 证据:
    - 文档要求同类可堆叠物品合并成一个 cell，`quantity badge` 由 typed overlay 驱动，并明确“不能继续把 `quantityText = null` 当作最终状态”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:319-340`。
    - screen matrix 也把 PR05-1 背包 grid 的 stack badge 列入必填覆盖: `UI/pr/screen-coverage-matrix.md:38-40`, `:77-80`。
    - 当前 `EquipmentInventoryPresenter` 对每个 `InventoryEntrySnapshot` 逐项生成 cell，`quantityText = null`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:133-185`。
  - 影响: 玩家无法区分“10 瓶药水”与“10 个独立格子”，背包容量、分页和奖励扫描都会失真。更重要的是，文档已经把这项变成 PR05-1 blocking 行为，当前实现完全没有对应 contract。
  - 修复方向: 在 presentation 层引入 typed `stackKey` / `quantity` / representative entry ids；没有上游同质性字段时，只能对已有 typed stackable source 做合并，不能用 localized name 或 iconKey 推断。

- **F-P1-4: PR05-1 golden / manual evidence 仍没有落地，文档证据目前只是索引占位。**
  - 证据:
    - 文档把四个 label 和 manual record 列成 canonical artifact / required evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:53-65`, `:434-489`。
    - screen matrix 也列出四个 PR05-1 required labels: `UI/pr/screen-coverage-matrix.md:77-80`。
    - 当前 `GoldenScreenshotHarnessTest` 只注册了 PR05 的 `dark-uiux-pr05-map-layer-stack` / `dark-uiux-pr05-actor-boss-telegraph`，没有 PR05-1 labels: `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:156-160`, `:243-250`。
    - `rg --files UI/manual-records UI/review | rg 'pr05-1|inventory-page-workbench'` 没有找到 `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md`。
  - 影响: 当前 reviewer 只能读文档，不能验证 1672x941、1280x800、1024x768、pagination、compare、long name、stack badge 或 no-overlap。PR07 不能替 PR05-1 证明当前 workbench 最小闭环。
  - 修复方向: 新增 deterministic scenario、注册四个 golden labels、补 manual record 模板和实际记录；并让 golden harness / manual checklist 能 fail fast 发现 label 缺失。

- **F-P1-5: 当前工作区混入大量 formal resource / manifest 变更，和 PR05-1 的 non-resource 边界冲突。**
  - 证据:
    - PR05-1 文档明确“不生成正式 runtime asset，不新增 sprite sheet，不进入 manifest，不修改 `sheet-plan.yaml` / `key-registry.yaml` / canonical atlas”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:6-8`。
    - Must Not Touch 也禁止 runtime manifest、sprite sheet manifest、atlas key registry 和 PR-05 resource owner keys: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:142-152`。
    - Implementation Order 第一步写明“不修改 sprite sheet plan 或 resource key registry”: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:492-500`。
    - 当前 `git diff --stat` 包含 `UI/sprite-sheets/key-registry.yaml`, `UI/sprite-sheets/sheet-plan.yaml`, `assets-src/image/manifests/phase2-visual-manifest.json`, `client/src/main/resources/manifests/visual-manifest.json`, prompt index 和资源脚本的大量改动。
  - 影响: 如果这些改动属于同一个 PR05-1 变更集，PR05-1 的 `UI05-1-NO-RULE-01` 和 `resourcePipelineLint is not default` 口径就不成立；如果它们属于 PR05，则当前分支/提交需要拆分，否则审查无法判断哪些 artifact 是 PR05-1 的完成证据。
  - 修复方向: 把 PR05 resource replacement 与 PR05-1 inventory workbench 分成独立提交/PR；若必须同 PR 提交，则 PR05-1 文档必须改写 Gate Budget，把 resourcePipelineLint / manifest coverage / owner-scope evidence 纳入 blocking gate。

- **F-P1-6: detail / compare / action rows 仍由 renderer 拼接，未形成文档要求的 typed row owner。**
  - 证据:
    - 文档要求 `InventoryDetailRow`, `InventoryCompareRow`, `InventoryActionRow`，renderer 不得解析 localized text、icon key、asset path 或 filename 来生成规则语义: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:385-405`。
    - 当前 `TileRenderModel` 在 sidebar 分支中直接拼 selected item header、description lines、item detail lines 和最多 3 行 compare: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1515-1572`。
    - `equipmentComparisonLines` / `statDeltaLines` 仍返回 `TileTextLine`，并在 renderer 里组合本地化 label 与 signed number: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:2673-2718`。
    - 没看到 `InventoryActionRow` 或 disabled reason model；`E` 和 `D` 仍直接发命令: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:902-908`。
  - 影响: 这会继续把 renderer 变成背包详情 authority。后续要加禁用原因、隐藏无 typed delta 字段、非装备 item action、compare target cue 时，会在 renderer/input 之间扩散第二套逻辑。
  - 修复方向: 让 presenter 输出 typed semantic rows；renderer 只画 row，不生成规则含义；action availability 和 disabled reason 也必须来自同一 presentation owner。

### P2

- **F-P2-1: Reference Exclusion 的 static scan 描述过宽，会产生大量误报，不能作为真实 fast check。**
  - 证据:
    - Acceptance Matrix 写的是扫描 `burden/filter/economy/category terms in production Kotlin, manifest and locale`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:30`。
    - 当前正式代码和 locale 已经合法存在 `category` 概念，例如 inscription category: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:2610-2615`, `game/src/main/resources/i18n/en-US.json:1026-1028`, `game/src/main/resources/i18n/zh-CN.json:1026-1028`。
  - 影响: 直接扫描 `category` / `filter` 会把 inscriptions、search、collection filtering 等正常代码打成违规，导致 fast check 不可执行。反过来，如果实现者因为误报把扫描放宽，又会失去排除参考图分类 tab 的意义。
  - 修复方向: 改成精确 denylist，例如 `ui.inventory.filter`, `ui.inventory.category_tab`, `ui.inventory.weight`, `burden`, `encumbrance`, `carryWeight`, `weightCapacity`；不要扫通用词 `filter` / `category`。

- **F-P2-2: Visual Socket Mapping 中的 label token 与当前 locale / helper 不一致。**
  - 证据:
    - 文档表格使用 `ui.inventory.slot.weapon`, `ui.inventory.slot.offhand`, `ui.inventory.slot.armor`, `ui.inventory.slot.locked_visual`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:230-246`。
    - 当前 slot label helper 使用 `ui.sidebar.weapon`, `ui.sidebar.off_hand`, `ui.sidebar.armor`, `ui.reward.slot.accessory`: `client/src/main/kotlin/com/ktome/client/render/EquipmentSlotLabels.kt:14-20`。
    - `rg` 没找到 `ui.inventory.slot.locked_visual` 或 `ui.inventory.slot.weapon` 的 locale 定义。
  - 影响: 文档看起来冻结了 9-slot token 合同，但实际 locale 不存在。实现者要么运行时显示 raw key，要么临时复用旧 key，导致视觉槽文案和 tooltip 口径再次漂移。
  - 修复方向: 二选一：文档改成当前真实 token；或同 PR 新增 `ui.inventory.slot.*` locale，并补 `localeLint` / focused test。

- **F-P2-3: `acceptanceContractLint` 已通过，但它没有检查 PR05-1 artifact 是否真实存在。**
  - 证据:
    - 本轮实际运行 `./gradlew acceptanceContractLint`，结果 `BUILD SUCCESSFUL`。
    - lint 当前只检查文档 token、Acceptance Matrix 和行数，不检查 artifact 文件或 golden label 是否存在: `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt:89-109`。
    - 同时，`InventoryWorkbenchPresentation.kt`, `InventoryWorkbenchPresenterTest.kt`, PR05-1 golden labels 和 manual record 都未落地。
  - 影响: 治理快路径已经能防孤立文档，但还不能防“文档写了 artifact，仓库没有 artifact”。这会给 PR close checklist 造成假阳性。
  - 修复方向: 对 PR05-1 增加轻量 artifact existence check，至少覆盖 PR05-1 manual path、GoldenScreenshotHarnessTest label registration 和 expected presenter test path。

- **F-P2-4: `Enter/E equip/use` 文档与当前输入行为不一致。**
  - 证据:
    - 文档要求 `Enter/E` equip/use selected item: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:272-280`, `:361-369`。
    - 当前 `Enter` / `Space` 是 `pushItemDetailFrame()`，只有 `E` activate item: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:894-904`。
  - 影响: footer 一旦显示 `Enter/E equip/use`，玩家按 Enter 却进入详情，会形成明显操作反馈错误。
  - 修复方向: 要么改文档为 `Enter inspect/select, E equip/use`；要么改实现让 Enter 按文档执行，并把 inspect 放到独立键位。

- **F-P2-5: map mode 下 `PageUp/PageDown` 会修改隐藏背包 selection，超出 PR05-1 输入合同。**
  - 证据:
    - 文档只要求背包打开态 `PgUp/PgDn` pagination 和 map movement 不穿透: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:359-369`。
    - 当前 map command path 在 movement 前调用 `pollBackpackPaging(snapshot)`，即使不在 `UiMode.INVENTORY` 也会改 `inventorySelection`: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:420-445`, `:509-524`。
    - 测试名也锁定了“map page keys page backpack instead of moving the actor”: `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1305-1328`。
  - 影响: 玩家在地图页按 PageUp/PageDown 会静默改变下次打开背包的选中项。这个行为不是文档要求，也不是可见反馈，属于隐藏状态副作用。
  - 修复方向: 将 backpack pagination 限定在 `UiMode.INVENTORY`；如果确实要保留 map mode PageUp/PageDown，则必须写入非目标以外的正式输入合同并提供 UI feedback。

- **F-P2-6: page label / footer hint 仍是 presenter 拼接英文字符串，不符合 locale/token policy。**
  - 证据:
    - 文档要求 footer hints 走 localized action hint tokens，测试优先检查 token / semantic line: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:290-305`, `:351-357`。
    - 当前 `EquipmentInventoryPresenter` 直接输出 `"${pageIndex + 1}/$pageCount  PgUp/PgDn"`: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:178-183`。
  - 影响: PR05-1 会新增中文/英文 UI 面，硬编码 `PgUp/PgDn` 会绕过 localeLint，也会让 footer 责任分散在 presenter 和 renderer 中。
  - 修复方向: 输出 semantic page model 或 `footerHints` token，由 renderer/localizer 渲染。

- **F-P2-7: Reference image hash / dimension 已写入文档，但没有自动化检查承接。**
  - 证据:
    - 文档写明 reference PNG 尺寸和 sha256: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:53-61`。
    - 本轮手动校验结果为 `1672x941` 且 sha256 匹配文档。
    - 但 Acceptance Matrix 的 `sips dimension check` 不是仓库内 task，`acceptanceContractLint` 通过不代表 reference image 被校验。
  - 影响: 图片被替换、压缩或改尺寸时，文档不会自动失败；PR05-1 的视觉密度参考可能静默漂移。
  - 修复方向: 增加轻量脚本或 tools test，检查 reference image path、尺寸、hash；如果只保留人工检查，则把 `fastCheck` 从 `sips dimension check` 改成明确的 manual verification。

### P3

- **F-P3-1: Acceptance Matrix 的 artifact 单元格出现 `this document`，不是可扫描的 repo-relative path。**
  - 证据: `UI05-1-REF-EXCLUSION-01` artifact 写为 `this document and UI/pr/screen-coverage-matrix.md`: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:30`。
  - 影响: 这不会立刻破坏实现，但会降低 lint 和 reviewer 的路径一致性。
  - 修复方向: 改成 `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`。

- **F-P3-2: `DescriptionPresenterTest` 在文档中没有写具体路径，且现有测试包名容易让 item presenter 责任混淆。**
  - 证据:
    - 文档 row 写 `DescriptionPresenterTest`，但未给路径: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:35`。
    - 当前实际文件是 `client/src/test/kotlin/com/ktome/client/ui/talent/DescriptionPresenterTest.kt`，不是 inventory/render 专属 test。
  - 影响: 小问题，但会让实现者以为现有 talent description test 已经覆盖 inventory detail rows。
  - 修复方向: 文档写全路径；如果 PR05-1 要扩 inventory detail row，建议新增 `InventoryWorkbenchPresenterTest` 或 `InventoryDetailRowsTest`。

## Requirement Alignment

- Requirement: PR05-1 进入 README、screen matrix 和 `acceptanceContractLint`。
  - Evidence: `UI/pr/README.md:17-24`, `UI/pr/screen-coverage-matrix.md:38-40`, `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt:153-210`；本轮 `./gradlew acceptanceContractLint` 通过。
  - Conclusion: **一致**。上一轮 P0 路由/lint 缺口已关闭。

- Requirement: PR05-1 不生成正式 runtime asset、不修改 manifest/key registry/sheet plan。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:6-8`, `:142-152`, `:492-500`；当前 `git diff --stat` 包含 manifest、sheet plan、key registry 和资源脚本变更。
  - Conclusion: **不一致**。如果这些变更属于 PR05 而不是 PR05-1，需要拆分工作区或提交边界；否则 PR05-1 non-resource 合同被破坏。

- Requirement: 全屏 inventory workbench，单一 presentation owner，6x4 grid。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:282-317`；当前 `EquipmentInventoryPresenter.kt:72-83`, `TileRenderModel.kt:971-1014`。
  - Conclusion: **不一致**。文档已清楚，但实现仍是旧 right-panel 4x2 grid。

- Requirement: hover 不覆盖 committed selection，pagination 使用 24-slot page。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:359-383`；当前 `InputHandler.kt:121-123`, `:1687-1715`; `InputHandlerTest.kt:1389-1394`, `:1476-1488`。
  - Conclusion: **不一致**。当前实现和测试都保护旧行为。

- Requirement: 同类可堆叠物品合并并显示 quantity badge。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:319-340`; 当前 `EquipmentInventoryPresenter.kt:145-165`。
  - Conclusion: **不一致**。当前 presentation 仍逐 entry 渲染，quantity 为 null。

- Requirement: reference-only 不存在系统必须显式排除，尤其负重/承重。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:185-206`, `:406-420`；本轮 `rg` 未在 production Kotlin / manifest / locale 找到 `负重|承重|encumbrance|carryWeight|weightCapacity`。
  - Conclusion: **部分一致**。文档排除项补得正确；但 fast check 描述包含 `category/filter` 这类过宽词，无法直接自动化。

- Requirement: golden/manual evidence 可执行。
  - Evidence: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:434-489`; 当前 `GoldenScreenshotHarnessTest.kt:156-160`, `:243-250`; `rg --files UI/manual-records ...` 无 PR05-1 manual。
  - Conclusion: **不一致**。证据合同已写，但没有 artifact。

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| PR route / lint | PR05-1 独立插入 PR05 与 PR06 之间，并纳入 lint | 已实现 | `UI/pr/README.md:18`, `Phase4V4AcceptanceContractLintTest.kt:195-199` | `acceptanceContractLint` 通过 | P0 closed |
| Resource boundary | PR05-1 不改 manifest / sheet / key registry | 偏离实现 | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:6-8`; `git diff --stat` | 当前工作区混有大量 resource pipeline 变更 | P1 |
| Workbench model | 单一 `InventoryWorkbenchPresentation`，6x4 grid，detail/compare/action/footer | 未实现 | `EquipmentInventoryPresenter.kt:72-83`, `TileRenderModel.kt:971-1014` | 仍是 4x2 right-panel presenter | P1 |
| Input / focus | 二维 focus、24 page step、hover preview 不提交 selection | 偏离实现 | `InputHandler.kt:121-123`, `:845-910`, `:1687-1715` | 8-slot page、线性上下、hover 改 selection | P1 |
| Stack grouping | typed stack identity + quantity badge | 未实现 | `EquipmentInventoryPresenter.kt:145-165` | `quantityText = null`，没有 grouping | P1 |
| Detail/compare/action rows | typed semantic rows，renderer 只消费 | 部分实现 | `TileRenderModel.kt:1515-1572`, `:2673-2718` | renderer 仍拼 text rows，无 action row / disabled reason owner | P1 |
| Golden/manual evidence | 4 个 PR05-1 label + manual record | 未实现 | `GoldenScreenshotHarnessTest.kt:156-160`, `:243-250`; `rg --files` | 只有文档索引，未注册/未产物化 | P1 |
| Reference exclusion | 排除负重/筛选/金币栏/额外装备槽 | 部分实现 | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:195-206` | 文档正确，但 scan 规则过宽 | P2 |
| Locale/socket tokens | 9-slot visual socket label/tooltip token | 部分实现 | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:234-244`, `EquipmentSlotLabels.kt:14-20` | 文档 token 和当前 locale/helper 不一致 | P2 |

## 玩法与体验审查

### 核心循环

PR05-1 的文档现在比上一轮清楚很多：它知道背包是 run 内“拾取 -> 比较 -> 装备/使用/丢弃 -> 返回地图”的核心循环。但实现仍没有把这个循环变成一个稳定 workbench。当前路径继续依赖右栏 grid、sidebar rows 和 item detail modal，玩家仍会在多个局部 UI 之间跳转。

### 战斗体验

输入合同仍是最大风险。`D` drop 保留是对的，但 `Enter` 文档说 equip/use、实现却打开详情；hover 又会提交 selection。战斗压力下这会导致误用、误丢或比较对象错乱。

### 成长与构筑驱动

装备 compare 是构筑反馈核心。当前 compare 只是在 renderer 里拼少量 stat delta，没有 typed compare/action owner，也没有 disabled reason。首版可以只覆盖已有 stat，但必须把“哪些字段能显示、哪些字段隐藏、为什么禁用”交给 presentation，而不是 renderer 猜。

### 奖励驱动与掉落体验

stack grouping 现在是 blocking 要求，但实现没有 quantity。药水、材料、钥匙类物品如果仍占多个格子，玩家对容量、奖励密度和分页的判断都会偏离真实玩法。

### 探索与新鲜感

文档要求保留地图暗底和 HUD 低亮存在感是正确的；但 current implementation 仍没有全屏 workbench frame，只是在 demo shell right panel 和 sidebar 上追加 tooltip/compare，无法形成“仍在 run 内但进入背包工作台”的体验。

### 新手体验与信息反馈

新手最需要稳定知道“我选中了什么、按哪个键会发生什么”。当前 hover 抢 selection、Enter/E 语义不一致、action disabled reason 不存在，都会削弱可理解性。

### 系统耦合与体验断层

当前工作区同时包含 PR05 资源替换和 PR05-1 背包文档，这会让 evidence、manifest gate、manual record 的 owner 混在一起。对 UI/UX 主线来说，这种混合会让每个 PR 的退出条件不再可审计。

## 当前阶段必须解决的问题

- **必须先切清 PR05 resource 变更与 PR05-1 workbench 变更边界。**
  - 为什么当前 Phase 必须处理: PR05-1 文档明确不改正式资源和 manifest。
  - 为什么不能推迟: 一旦合并到同一 PR，resource gate 结果和 inventory workbench 证据会互相污染。
  - 修复方向: 拆分提交/PR；或改写 PR05-1 文档与 Gate Budget，把 resource gates 纳入 blocking。
  - 优先级: P1。

- **必须实现真正的 workbench presentation owner。**
  - 为什么当前 Phase 必须处理: 6x4 full-screen workbench 是本 PR 的核心，不是 polish。
  - 为什么不能推迟: 没有单一 model，input、renderer、golden 和 manual 都只能继续围绕旧 right-panel 补丁。
  - 修复方向: 新增 `InventoryWorkbenchPresentation` / tests，`UiMode.INVENTORY` 渲染全屏 workbench route。
  - 优先级: P1。

- **必须重写 input/hover/page tests。**
  - 为什么当前 Phase 必须处理: 当前 tests 正在锁定错误行为。
  - 为什么不能推迟: 测试会继续阻止 24-slot page、hover preview 和二维 focus 的正确实现。
  - 修复方向: 删除 hover 改 selection 断言；新增 6x4 page step、left/right/up/down、Enter/E 实际语义测试。
  - 优先级: P1。

- **必须补 PR05-1 golden/manual evidence。**
  - 为什么当前 Phase 必须处理: 背包是布局敏感的玩家主路径界面。
  - 为什么不能推迟: 没有截图/手工记录，无法证明 1024x768、长文本、stack badge、compare/detail 不重叠。
  - 修复方向: 注册四个 labels，新增 scenario 和 manual record。
  - 优先级: P1。

## Removal/Iteration Plan

| Item | 当前建议 | Owner | 删除/迁移条件 | 回归扫描 |
| --- | --- | --- | --- | --- |
| old 4x2 right-panel inventory as PR05-1 main path | PR05-1 中不能作为主背包页保留 | client | full-screen workbench route 接管 `UiMode.INVENTORY` | `rg -n "inventoryColumns: Int = 4|inventoryVisibleRows: Int = 2|rightBackpackTitle" client/src` |
| fixed 8-slot page size | replace with workbench grid `columns * rows = 24` | client | input tests 改为 24-slot page | `rg -n "inventoryPageSize = 8|assertEquals\\(8, handler.overlayState\\(\\)\\.inventorySelection" client/src` |
| hover commits selection | split `hoveredCell` and committed selection | client | hover test 只断言 hover preview，不改 selection | `rg -n "inventorySelection = hoveredInventory|hoveredInventoryIndex \\?: overlayState.inventorySelection" client/src` |
| renderer-owned compare/detail rows | migrate into typed presentation rows | client | `InventoryDetailRow` / `InventoryCompareRow` / `InventoryActionRow` tests passing | `rg -n "equipmentComparisonLines|statDeltaLines|DescriptionPresenter.presentInventoryItemLines" client/src/main/kotlin/com/ktome/client/render` |
| PR05 resource changes in PR05-1 surface | split or reclassify | docs/assets | PR05-1 diff 不含 sheet/manifest/key registry | `git diff --name-only | rg "sheet-plan|key-registry|visual-manifest|assets-src/image/raw|client/src/main/resources/dark-v1"` |

## Additional Suggestions

- 把 `UI05-1-REF-EXCLUSION-01` 的 fastCheck 改成精确 denylist，避免 `category/filter` 误报。
- 把 `this document` 改成明确路径 `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`。
- 明确 `Enter` 是 inspect/select 还是 equip/use；现在文档和实现不一致。
- 给 reference PNG 的 size/hash 增加 tools test，或把它降级为明确人工检查。
- 把 visual-only locked socket 的 locale token 写实；不要在表里放尚不存在的 key。

## Suggested Verification

本轮实际运行:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
shasum -a 256 UI/dark-uiux-pr03-inventory-page-reference.png
sips -g pixelWidth -g pixelHeight UI/dark-uiux-pr03-inventory-page-reference.png
rg -n '<machine-absolute-path-pattern>' UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md UI/pr/README.md UI/pr/screen-coverage-matrix.md UI/review/open-design/dark-uiux-pr05-1-inventory-page-workbench-design.md UI/dark-uiux-pr03-inventory-page-reference.prompt.txt
```

结果:

- `acceptanceContractLint` 通过。
- reference PNG sha256 与文档一致，尺寸为 `1672x941`。
- 上述 PR05-1 文档/参考 prompt 路径扫描未发现机器绝对路径。

本轮未运行:

- 未运行 `:client:test`、`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。
- 原因: 当前 review 已在静态证据层发现 PR05-1 核心实现缺失和 evidence 缺失；继续跑 golden 不能补足缺失的 PR05-1 labels。

修复后建议执行:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.render.InventoryWorkbenchPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew localeLint contractLint maintainabilityLint verifyChanged
git diff --check -- UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md UI/pr/README.md UI/pr/screen-coverage-matrix.md UI/review
```

## Summary

这次修改把上一轮最关键的文档治理问题补上了：PR05-1 已进入 README、screen matrix、acceptance lint，reference exclusion table 也明确排除了负重/承重等图上但当前不存在的系统。问题在于实现没有同步跟上：6x4 workbench、typed rows、hover/selection、stack badge、golden/manual evidence 都还没有闭环。下一步不应继续加文档描述，而应先切清 PR05 与 PR05-1 的改动边界，然后实现单一 workbench presentation owner 并用 focused tests + golden/manual 把玩家路径锁住。
