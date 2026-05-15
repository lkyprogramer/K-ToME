# Dark UI/UX PR-03 Deep Review — Equipment / Inventory / Items / Shop

- 评审日期: 2026-05-15
- 分支: `codex/dark-uiux-pr03-equipment-inventory-items`
- 规范源: `UI/pr/dark-uiux-pr03-equipment-inventory-items.md`
- 评审角色: Roguelike / ToME 设计总监 + 系统策划总监 + 玩法体验审查负责人
- 评审范围: Round 7 资源合同、key registry / manifest、`EquipmentInventoryPresenter` / `TileRenderer*`、Modal card / Description presenter / InputHandler、validation scenario、tests、manual record、lint scripts、tools gradle hookup
- 严重度: BLOCKER / HIGH / MEDIUM / LOW / NIT（沿用 `code-review-high` rubric）

---

## Summary

- 整体结论：**request_changes**。装备 / 背包 / 商店三个主向都已经接上真实 snapshot，资源管道、key registry、manifest 同步、focused test 与 golden 五个标签都在；但 §8 强约束的人工白盒证据两份都被自填为 *skipped by request*，这一项是 `UI03-M08` 的硬要求，必须先做掉才能 close PR。
- 顶层风险:
  1. **HIGH**: `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md`、`UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` 全部 `scenarioId/screenshotPath/logPath` 写成 `N/A - manual ... skipped`，直接违反 §8 "Only skipped on headless CI" 与 §0 Failure Rule "不得只靠人工白盒证明 grid / tooltip / shop replacement 行为" 的反向约束（白盒不是替代，是必须项）。
  2. **MEDIUM**: `ModalCardModel.shopTypeIconKey` 用裸字符串 `"ui.shop.tag.inscription"` 在 client 端识别 inscription 商品类型（`client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt:194-196`），等价于 client 用 locale tag 反推规则，触碰 §5 第 7 条与 §6.3 "snapshot/presenter/locale token only, no business rule" 的语义边界。
  3. **MEDIUM**: `demoOperationRows` 把 inscription replacement 候选槽位 `slotRows` 与 supporting hint 都硬截到 `take(2)`（`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:926`）。在 4 个候选槽全满的核心 5–8 路径下，最多可隐藏两个 hotkey 行，与 §6.4 "renderer 只展示 snapshot hotkey，不重新编号" 的精神不符。
- Approval: **request_changes**

---

## Affected files

共 32 个跟 main 的 diff 文件，分 5 类（与 spec §2 影响范围对照）：

| Category | 文件 | 状态 |
| --- | --- | --- |
| 资源合同 | `UI/sprite-sheets/sheet-plan.yaml`, `UI/sprite-sheets/key-registry.yaml`, `UI/sprite-sheets/prompts/dark-v1/prompt-index.json` | modified |
| Manifest | `assets-src/image/manifests/phase2-visual-manifest.json`, `client/src/main/resources/manifests/visual-manifest.json`, `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`, `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`（new） | modified / added |
| Client presenter / renderer | `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt`（new）, `TileRenderModel.kt`, `TileRenderer.kt`, `DemoShellRenderer.kt`, `RewardPresentationText.kt`, `client/src/main/kotlin/com/ktome/client/assets/DarkUiEquipmentInventoryVisualKeys.kt`（new）, `client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt` | added / modified |
| Game data + 规则 | `game/src/main/resources/data/items/index.yaml`, `game/src/main/resources/data/visuals/index.yaml`, `game/src/main/resources/i18n/en-US.json`, `game/src/main/resources/i18n/zh-CN.json`, `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`, `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml` | modified |
| Tests / Scripts / Docs | 5 个 client test + 1 个 game test + `FoundationGameSessionTest`、`ValidationScenarioRegistryTest`，4 个 verify 脚本，`tools/build.gradle.kts`，`UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md`（new）, `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md`（new）, `GoldenScreenshotHarnessTest.kt` | added / modified |

CI / runtime impact 全部走 owner-scope coverage + golden + clientSmoke，无新 build 入口。

---

## Root cause & assumptions

- 改动目的: 落地 PR-03 §1 五条阶段目标（装备/背包/资源 grid、icon 化、空态/不可用/选中、shop 暗黑化、Round 7 sheet 分批），且不触碰规则、经济、数值与 snapshot schema。
- 实际路径: 资源 → key registry → canonical / runtime manifest → `EquipmentInventoryPresenter` → `TileRenderer*` + `ModalCardModel` → `InputHandler` → game data / i18n / validation registry → focused tests + golden + manual。整体顺序正确（与 §7 implementation order 一致）。
- 假设:
  - `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:536` 已经存在 `tagLabelKeys: List<String>`，这是 PR-02 之前就 ship 的字段，PR-03 直接消费，不算扩展 schema。
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:6006` 写入 `"ui.shop.tag.inscription"` 是 game 层既有行为（本 PR 不改动该 file），因此 client 端字符串匹配视作 token 边界。
- 未运行的 gate（review 期间没有真实执行）:
  - `./gradlew :client:goldenScreenshot`、`darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03 ...`、`spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=...`、`verifyChanged`：建议在 close 前由实现人在本地或 CI 跑一遍并把 duration summary 贴回 PR 说明。

---

## Findings

按严重度排序，每条都给出文件与行号、影响、最小修复路径与测试建议。code-review-high 约束下不在此报告中写入完整实现代码。

### [HIGH] [Docs / Acceptance] 手动白盒主记录 + fallback 注入记录被自填为 *skipped by request*，破坏 `UI03-M08`

- Where:
  - `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md:5-15`
  - `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md:5-17`
- Evidence:
  - 主记录所有 5 个 `evidenceLabel` 对应的 `screenshotPath` / `logPath` / `cleanupStatus` 均为 `N/A - manual ... skipped`。
  - 注入记录 `injectedMissingKey=item.missing.debug.icon` 没有任何 `screenshotPath`，也未走 `client/src/test/resources/dark-uiux/pr03-missing-key-fixture/` 这个 spec §8 表中明确要求的 fixture 路径（fixture 目录在仓库不存在）。
- Impact:
  - 直接违反 §8 表中 5 行 skip rule —— 每行都是 "Only skipped on headless CI" 或 "Only skipped if scenario unavailable"。本次 review 既不在 headless CI 上跑，也未声明 scenario 缺失，缺乏跳过依据。
  - 同时违反 §0 Failure Rule："inventory/shop golden 或 coverage 失败时先修 presenter / resource key / manifest mapping；**不得只靠人工白盒证明 grid / tooltip / shop replacement 行为**" —— 反向论证白盒不可被自动测试替代，反过来也成立：手动白盒是 §0/§8 强制 artifact，不能用 focused test 顶替。
  - 缺 fallback fixture 同时违反 §8 "primary path is resolver fixture copy under `client/src/test/resources/dark-uiux/pr03-missing-key-fixture/`; do not edit official runtime manifest"。
- Standards: 内部 governance（development-governance / acceptance contract），未涉及外部标准。
- Repro:
  - `rg -n "screenshotPath: \`N/A" UI/manual-records/dark-uiux-pr03-*.md` → 两份记录共 6 处 N/A。
- Recommendation:
  1. 由实现人在本地启动 `dark-uiux-pr03-equipment-inventory-items` 场景，按 §8 表中 5 个 label 逐一截图，写入 `client/build/reports/golden/` 与 `screenshotPath: UI/manual-records/screenshots/...`（或对应 golden index entry）；填好 `cleanupStatus`、`inputSequence`、`logPath`。
  2. 在 `client/src/test/resources/dark-uiux/pr03-missing-key-fixture/` 下放一份 manifest fixture（仅含本次注入的 missing 与 fallback 条目），通过 `ManifestResolveTest` 的 fixture 入口加载，再补一张 `dark-uiux-pr03-inventory-empty` 视觉截图作为 fallback 证据。
  3. 不接受继续把整页填 `N/A - manual ... skipped`，否则 `UI03-M08` 永远过不了。
- Tests:
  - 不需要新增自动化用例；已有 focused test 已经覆盖逻辑，**缺的是视觉证据**。建议把截图 hash 加进 `GoldenScreenshotHarnessTest.kt` 的 stable hash 表，保证下次回归能机控比对。

### [MEDIUM] [Correctness / Coupling] `ModalCardModel.shopTypeIconKey` 用 locale token 字符串识别 inscription 类型

- Where: `client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt:194-196`
  ```
  private fun shopTypeIconKey(offer: ShopOfferSnapshot): String? =
      DarkUiEquipmentInventoryVisualKeys.SHOP_INSCRIPTION_MARKER
          .takeIf { offer.tagLabelKeys.any { labelKey -> labelKey == "ui.shop.tag.inscription" } }
  ```
- Evidence:
  - 该 token 由 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:6006` 写入 `tagLabelKeys`，同时也作为 i18n 显示串（`game/src/main/resources/i18n/{en-US,zh-CN}.json:195`）使用。
  - 一旦本地化或重命名（例如 `ui.shop.tag.inscription_offer`、或 game 端将 token 拆为 display key + role key），client 端会静默丢掉 inscription marker。
- Impact:
  - 表现规则上等价于 "client 用 locale token 推断 offer 类型"，部分触碰 §5 第 7 条 "不在 client 用 locale text、price label、offer label 或 visual key 反推购买规则" 的意图。inscription marker 严格意义上是 *表现* 不是 *规则*，所以不是 BLOCKER，但若以后 game 端调整 tag 文本，会导致 UI 静默回退，属于 footgun。
  - 也使得 `ModalCardModelTest` 必须固化字符串常量，进一步把 client 测试耦合到 game-side i18n key。
- Standards: 内部 §5 / §6.2 / §6.3 边界。
- Repro:
  - `rg -n "ui.shop.tag.inscription" client/` 仅出现在 `ModalCardModel.kt` 与 `ModalCardModelTest.kt`。
- Recommendation（最小修复方案）:
  - **方案 A（推荐）**: 在 client 端 `DarkUiEquipmentInventoryVisualKeys` 旁新增 `object ShopOfferTagTokens { const val INSCRIPTION = "ui.shop.tag.inscription" }`，集中常量；保留行为不变但把字符串收口。改动 ≤ 5 行。
  - **方案 B**: 由 game 在 `ShopOfferSnapshot` 增 `offerType: ShopOfferType` 字段（typed enum），client 改为 `offer.offerType == INSCRIPTION`。这条会动 snapshot schema，**当前 PR 明确 §1 不扩展 snapshot**，所以放到后续 PR；但需要在 §6.3 边界表加一行 deferred 记录。
- Tests:
  - 现有 `ModalCardModelTest` 已经覆盖；切到方案 A 后改成断言常量来源即可。

### [MEDIUM] [UX / Renderer truncation] `demoOperationRows` 把 4 槽全满的 replacement candidate 行截断到 2

- Where: `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:902-926`
- Evidence:
  ```
  val slotRows = promptRows.filter { row -> row.icon?.resolvedKey == DarkUiEquipmentInventoryVisualKeys.SHOP_REPLACEMENT_SLOT_MARKER }
  val supportRows = promptRows.drop(1).filter { row -> row.icon == null }
  return listOfNotNull(title) + slotRows.take(2) + supportRows.take(2)
  ```
- Impact:
  - §6.4 第 1–2 条说候选槽位来自 `InscriptionReplacementPromptSnapshot.currentSlots`，hotkey 固定 `5–8`。当满 4 槽时（这是 §8 `dark-uiux-pr03-shop-full-slot-replace` 的核心场景），`slotRows.take(2)` 会丢掉 hotkey `7` / `8`，玩家在 operation hints 区只能看到 5 / 6 两行。
  - 实际玩法没坏（input handler 仍接受 5–8），但 hints 与可按按键不一致，会让玩家以为只剩 2 个槽可换。这同时会让 `dark-uiux-pr03-shop-full-slot-replace` golden 截图无法反映完整 4 行。
- Standards: 内部 §6.4 / §8 manual whitebox label。
- Repro:
  - 构造 4 个 `currentSlots`，hotkey 5,6,7,8，调用 `demoOperationRows`，断言返回 `1 (title) + 4 (slots) + supportRows`。
- Recommendation:
  - 改成 `slotRows.take(4)` 与 `supportRows.take(2)`（或干脆把 slot 行全部保留，因为最多 4 行）。如果 hints 区高度不足，应该在 layout 层加滚动 / paging，而不是在 presenter 层悄悄丢行。修复行号: `TileRenderModel.kt:926`。
- Tests:
  - 在 `TileRendererCanvasTest.kt` 已有的 "replacement slot marker drawn" case 上扩展：把 promptSnapshot 加到 4 个候选槽，断言 4 行 marker 全部被 drawSlot 调用。

### [LOW] [Implementation hygiene] `EquipmentInventoryPresenter.VISUAL_SOCKET_COUNT = 9` 硬编码

- Where: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:96-97`
- Evidence: `9 = 4 typed + 5 visual-only`，与 PR-02-1 right panel 的 sockets 个数耦合。
- Impact: 后续若 PR-02-1 layout 调整 visual-only socket 数量，这里需要手动追改；目前未在 PR-02-1 contract / `EquipmentSlotLabels` 中暴露。
- Recommendation: 把 5 个 visual-only 数量来源放到 `EquipmentSlotLabels` 或 PR-02-1 layout contract，让 presenter 直接读取；如果暂不动，至少加 1 行注释说明对齐到哪个 layout 文档。
- Tests: 现有 presenter test 用 `typed.take(4)` / `displayOnly.size == 5` 断言常量值；改成读取常量后该断言保留语义即可。

### [LOW] [Docs vs Implementation] `DescriptionPresenter.kt` 与 spec §2 不一致（spec 列为 Modified，实际未改）

- Where: spec `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:75` 把 `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt` 列为 `Modified`，但 `git diff main --stat` 显示该文件未变；仅 test 被改。
- Impact: 文档与实现轻微 drift。功能上没问题（shop tooltip / description 复用现有 line 行为已经成立），但 spec self-audit 应当反映现实。
- Recommendation: 在 spec §2 把这一行的 Action 改为 `Touched (tests only)` 或在 PR 说明中加一条 "DescriptionPresenter.kt 已具备复用能力，本次只补 owner test"。
- Tests: 已有 `DescriptionPresenterTest` 增量用例覆盖。

### [LOW] [Data hygiene] `energy_tonic` / `consecrated_oil` 旧 `*.visual` manifest 入口可能 orphan

- Where: 检查点 —— `game/src/main/resources/data/items/index.yaml` 已经把这两件 base item 迁到 `item.energy_tonic.icon` / `item.consecrated_oil.icon`，但仍需确认 `assets-src/image/manifests/phase2-visual-manifest.json` 是否仍残留旧 `item.mana_potion.visual` / `item.healing_potion.visual` 条目仅供这两件 item 复用。
- Impact: 若 manifest 仍保留对应旧 entry 但无任何 consumer，会让 owner-scope coverage report 误把它们算作活键。规则 §4 第 4 条要求 fallback 只走 dark style；orphan 旧 entry 会绕过 coverage。
- Recommendation:
  - 跑 `rg -n "item\.mana_potion\.visual|item\.healing_potion\.visual" client/ game/ tools/` 确认是否仍被消费。如果只在 manifest 出现，需要在 key registry 标 `aliasOf` 或 `remove`，与 §4 表的 *Existing key migration* 行为对齐。
- Tests: `ManifestResolveTest` 增量用例已经断言新 key，旧 key 残留只能靠 coverage report 暴露。

### [LOW] [QA Provenance] PR-03 sprite map record 中 `CODEX_VISUAL_CHECKED` 与 `DRY_RUN` 混用

- Where: `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`（62 DRY_RUN + 42 CODEX_VISUAL_CHECKED）
- Evidence: 62 条 DRY_RUN 是 PR-00 / 01 / 02 的历史 record 拷贝到 PR-03 报告路径中。
- Impact: §0 acceptance matrix 中 `UI03-M01` 的 `requiredOwnerSheetIds=r07-items-base,r07-items-unique-artifact,r07-items-affix-material` 要求 owner-scope 只检查 R07；DRY_RUN 行的存在不会让 owner-scope 失败，但会让 `--require-reviewed-qa` 把 PR-03 sheet 之外的所有历史 cell 视作 reviewed gap，间接增加 lint 噪声。
- Recommendation:
  - 把 PR-03 报告改为只保留 PR-03 owner 的 cell 记录（即 42 条 R07 CODEX_VISUAL_CHECKED），其他历史 cell 留在 PR-00/01/02 自己的 sprite-map report 中。该 jsonl 与 owner-scope coverage 对齐，避免跨 PR 数据污染。
- Tests: 在 `scripts/verify_sprite_sheet_map.py` 既有的 sheet id filter 上加一条 owner_pr filter，保证 `--require-reviewed-qa` 与 owner_scope 对齐。

### [LOW] [Renderer] Quality marker 走 `drawRect` 而非 sprite

- Where: `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`（drawQualityMarker：3px 颜色条 `RARE=#D8A73F`、`MAGIC=#446ED8`、其余=`#C5C9C4`）
- Evidence: spec §4 第 3 条指出 quality frame 若需独立边框资源必须归 `ui_frame` 并回到 PR-02 / PR-00 registry，"PR-03 的 `r07-items-affix-material` 只生成 item / affix / material 主体或小 marker"。本次实现选了 *no sprite, drawRect color* 方案。
- Impact: 行为正确且不超范围。但 dark UI 视觉风格上 3px 纯色条与整体 sprite 化的格调不一致，长期看建议改为 small marker sprite（已在 `r07-items-affix-material` 范围内允许）。属于风格债，非合规债。
- Recommendation: 短期保留；后续 PR（PR-04/05 或视觉 polish PR）替换为 marker sprite。

### [NIT] [Validation Scenario] `seed=2026050903`、`LOOT_LAB` 选择无校验

- Where: `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt:334-400`
- Recommendation: 在 `ValidationScenarioRegistryTest` 已经覆盖该 scenarioId 存在；可以补一条断言 `LOOT_LAB` 在 PR-03 之前的 `phase4-v4-pr02` shop scenario 列表中能复用（已经在 `phase4-v4-scenarios.yaml` 中），把跨文件依赖固化。

### [NIT] [Style] manual record 文件无 `last-updated` 字段

- Where: 两份 manual record markdown。
- Recommendation: 在 doc 顶部加 ISO 日期，方便回归比对。governance 未强制，留作 nit。

---

## Performance

- Hotspots:
  - `EquipmentInventoryPresenter.present` 在 paged overflow 路径下每帧重建 `cells: List<InventoryGridCellModel>`，但 list size 是 `columns * visibleRows`（上限通常 ≤ 24），cost 可忽略。
  - `DemoShellRenderer.drawOperationHintsSection` 在 inscription replacement prompt 上每行最多 3 个 13px icon、frame alpha 0.72；都是常量数量，无 N+1。
- Complexity notes:
  - `demoOperationRows` 的 `promptRows.filter { row -> row.icon?.resolvedKey == ... }` 两次扫描 + 1 次 `drop(1).filter`，list ≤ 8，问题不大。
- Bench / Monitoring plan:
  - 不需要新 bench；建议在 `verifyChanged` summary 里追踪 `:client:goldenScreenshot` duration，PR-03 加 5 张 golden 后应当与 PR-02 baseline 差距 < 5s。

---

## Integration

- API / contracts:
  - `ItemRenderSnapshot.iconKey` / `visualKey` / `qualityTierId` / `slotId` / `tagLabelKeys`：全部使用既有 snapshot 字段，未扩展 schema（满足 §1 / §6.2 deferred 规则）。
  - `ShopOfferSnapshot.price` / `RenderUiStateSnapshot.shardBalance` 用于 affordability 计算，仅作为 marker，未介入购买规则（满足 §6.2 第 5 行）。
- Manifest 兼容性:
  - canonical / runtime manifest 同步 603 条 entries；`ui.shop.offer.frame` 作为 alias 共享 `dark-v1/ui/ui_frame_panel_body.png`，不产生新 PNG（满足 §4 第 7 条）。
- 回滚计划:
  - 单 PR 全部改动属于内容 + UI 层；rollback 直接 `git revert <merge-commit>` 即可。manifest 同步通过 `syncPhase2Manifests` 重生成，无 DB migration。
  - 关键风险：item iconKey 改动会让现有 save 文件中持久化的 `iconKey` 失效？读 §5 / §6.2 可知 client 只消费 snapshot.iconKey，save 不存 iconKey，因此回滚安全。
- Rollout:
  - 无 feature flag，无渐进开关；UI 视觉变更属于 in-place。需要在 release note 里贴 5 张 golden 与 fallback 注入证据。

---

## Testing

- Coverage:
  - **Presenter**: `EquipmentInventoryPresenterTest`（3 用例）覆盖 typed 4 slot + 5 visual-only、inventory identity + dark fallback + manifest log、paged overflow stable empty cell。
  - **Renderer**: `TileRendererCanvasTest` 增量 3 用例覆盖 typed equipment grid、shop offer card markers、replacement slot marker。
  - **Modal**: `ModalCardModelTest` 增量覆盖 affordable/unaffordable/inscription marker、UNAFFORDABLE 不会注入 disabledReason。
  - **Input**: `InputHandlerTest` 增量覆盖 `NUM_1` / `NUM_4` 在 inscription replacement prompt 中不消费、ESC / BACKSPACE 触发 cancel。
  - **Description**: `DescriptionPresenterTest` 增量覆盖 shop offer 文案 token source。
  - **Manifest resolve**: `ManifestResolveTest` 增量覆盖 PR-03 owner keys（含 `DarkUiEquipmentInventoryVisualKeys.ownerKeys` 全集 + 关键 `ui.empty.inventory.icon` fallback）。
  - **Golden**: `GoldenScreenshotHarnessTest.kt:142-219, 425-439, 699-799` 注册 5 个 PR-03 label，stable hash 已落地。
  - **Game data contract**: `ItemVisualKeyContractTest` 覆盖 9 个迁移 item 的 iconKey / visualKey 与 30 个 visual key family。
- Gaps:
  - `demoOperationRows` 4 槽 replacement candidate 完整渲染未被断言（见 MEDIUM 第 3 条）。
  - manifest fixture for fallback injection 未落地（见 HIGH 第 1 条）。
- Flakiness risks:
  - `GoldenScreenshotHarnessTest` 依赖确定性 seed `2026050903`；只要不动 game RNG 与 manifest，hash 应稳定。
- Targeted test plan（建议添加）:
  - Given 4 个 inscription replacement 候选槽 hotkey 5–8，When `demoOperationRows`，Then 返回 1 title + 4 slot rows + ≤2 support rows。
  - Given `tagLabelKeys` 为空但 `offerType` 字段缺失（mock 场景），When `ModalCardModel.shopOffer`，Then `typeIconKey == null`。
  - Given fallback fixture 注入 `item.missing.debug.icon`，When `EquipmentInventoryPresenter`，Then 走 `ui.empty.inventory.icon` 并在 `ManifestLogSink` 输出 missing key 记录 —— 已在 presenter test 覆盖，但视觉证据缺失。

---

## Docs & Observability

- Docs to update / create:
  - **必改**: 两份 manual record 全部重填（HIGH 第 1 条）。
  - **建议**: spec §2 列 `DescriptionPresenter.kt` 改为 `Touched (tests only)`。
  - **建议**: 在 `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl` 文件顶部加一段 README / json header 注释（或独立 README）说明 owner_pr filter 与 DRY_RUN 历史 record 的关系。
- Logs / Metrics / Traces / Alerts:
  - `ManifestLogSink` 已捕获 missing key；建议把 `client/build/reports/golden/` 输出路径加进 release runbook。
  - 没有新增 runtime log；无需 PII 审查。
- Runbook:
  - 后续 owner（PR-04 / PR-05）若需要在 inscription marker 上加 typed `offerType`，参见 MEDIUM 第 2 条 deferred 方案。

---

## Open questions

1. `ShopOfferSnapshot.tagLabelKeys` 是否计划在后续 PR 替换为 typed `offerType: ShopOfferType`？如果是，本 PR 应在 spec §6.2 deferred 表登记，避免再次出现 client 反推。
2. PR-02-1 layout contract 是否暴露 `visualSocketCount` 常量？若否，建议在 PR-02-X follow-up 暴露，删除 `VISUAL_SOCKET_COUNT = 9` 硬编码。
3. `dark-uiux-pr03-fallback-key-injection` 走 fixture 路径的 `client/src/test/resources/dark-uiux/pr03-missing-key-fixture/` 是否已经预留？目前仓库不存在该目录，需要补一份 fixture manifest（spec §8 表硬要求）。
4. `tools/build.gradle.kts` 中新增的 `ktome.darkUiux.requireReviewedQa` 是否已经在某条 CI gate 中默认开启？若 PR-03 close 后必须保持 reviewed-QA 强制，建议在 `verifyChanged` 调用链中默认设置。

---

## Final recommendation

- Decision: **request_changes**
- Must-fix before merge:
  1. 补齐 `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md` 五个 label 的真实 screenshot / log / cleanupStatus 与 `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` 的 fixture 路径 + screenshot（HIGH）。
  2. `demoOperationRows` 的 `slotRows.take(2)` 改成 `take(4)`，并补 4 槽 hotkey 测试用例（MEDIUM）。
  3. 把 `"ui.shop.tag.inscription"` 字符串收口到 client 常量（方案 A，5 行内），并在 spec §6.2 deferred 表登记后续 typed `offerType` 路径（MEDIUM）。
- Nice-to-have post-merge:
  - 暴露 `visualSocketCount` 给 `EquipmentInventoryPresenter`，删硬编码（LOW）。
  - PR-03 sprite-map report 仅保留 R07 owner cell（LOW）。
  - quality marker 替换为 sprite 资源（LOW，可延后到 visual polish PR）。
  - spec §2 `DescriptionPresenter.kt` 行 Action 修正（LOW）。
- Confidence: **medium-high**。所有自动化测试设计与 manifest 同步均已就绪，剩余 blockers 集中在文档治理与一处 renderer truncation，整改成本小（< 0.5 PR）。
