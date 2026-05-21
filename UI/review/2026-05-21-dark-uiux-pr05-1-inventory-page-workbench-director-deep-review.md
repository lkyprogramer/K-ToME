# Dark UI/UX PR05-1 Inventory Page Workbench 深度审阅

- **审阅日期**: 2026-05-21
- **审阅范围**: `codex/dark-uiux-pr05-1-inventory-workbench` 分支当前 working tree
- **合同基准**: `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`（613 行,§0–§13）
- **手工证据**: `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md`（PASS,PID 7464,4 golden+7 CUA 截图）
- **角色**: Roguelike / ToME 类游戏开发设计总监 + 系统策划 + 玩法体验审查负责人
- **审阅模式**: 深度合同核对(spec ↔ implementation 1:1 + 体验/性能/可维护性)

---

## Summary

### What changed
PR05-1 把背包从覆盖式 debug modal 改造为一个全屏三栏 workbench:左侧 9-slot 装备视觉(4 typed + 5 visual-only)、中间 6x4 backpack grid(24 cells/page,支持 stack 合并)、右侧 typed detail / compare / action,底部固定 footer。新增 `InventoryWorkbenchPresentation.kt`(877 行)作为 presentation owner,新增 `InventoryWorkbenchLayout.kt` 提供三档 viewport profile,renderer/input/i18n/golden 配套落地。

### Top risks
1. **`detailRows`/`compareRows` 在 renderer 端写死 `take(7)` / `take(5)`,未按 §4.1 `detailMaxLines = 7/6/5` 随 viewport profile 收缩;1024x768 实际允许的最大详情行数与 spec 不一致**(MEDIUM)。
2. **Stack identity 的 stackable type 白名单(`{CONSUMABLE, MATERIAL, CURRENCY}`)与典型字段拼接逻辑住在 client 端 renderer 模块,既没按 §5.3.7 引入 typed `stackKey` 或 `materialId` 上游字段,也未覆盖 `materialNameKey` 差异,潜在 false-merge 风险**(MEDIUM)。
3. **Detail 列 actions / footer hints 在窄屏使用 `if overflow return@forEach` 静默裁剪,违背 §4.5.3 "保留 key label 但置灰" 与 §4.5.4 "容量和页码必须可见" 的诚实可见原则**(MEDIUM)。

### Approval
**comment**(可以合入,但需要在合并前修掉 3 条 MEDIUM,LOW 项可入下一轮清理 PR)。
- 合同主干、focused tests、golden labels、locale tokens、whitebox scenario、manual evidence 都已对齐。
- 残留 3 条 MEDIUM 是 renderer 局部 / 上游字段层面的真实偏差,不影响 fast-check / contract-lint / golden,但会在窄屏与多 material 物品场景把规则压力推回到下一个 PR。

---

## Affected files

| 路径 | 改动 | 说明 |
| --- | --- | --- |
| `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt` | added(877 行) | PR05-1 owner;equipment sockets / 6x4 grid / stack / detail / compare / action / footer 模型 |
| `client/src/main/kotlin/com/ktome/client/render/layout/InventoryWorkbenchLayout.kt` | added(172 行) | 三档 viewport layout profile(1672/1280/1024 三档),`InventoryWorkbenchLayoutProfile.forViewport()` |
| `client/src/test/kotlin/com/ktome/client/render/InventoryWorkbenchPresenterTest.kt` | added(348 行) | 9 个 focused 测试覆盖 9-socket、6x4、stack、empty、compare、layout profile |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | modified | `drawInventoryWorkbenchModal`、`drawInventoryWorkbenchGrid`、`drawInventoryWorkbenchDetail`、`drawInventoryWorkbenchFooter` 等绘制路径 |
| `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | modified | `pollInventoryCommand`、`enterInventory`、`updateInventoryWorkbenchHover`、`reconcileInventoryCursor`、`pollMapBackpackPagingNoop` 等;新增 `inventoryFocusedCell`、`inventoryPageIndex`、`hoveredInventoryCell` 状态 |
| `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt` | modified | 仅服务 map shell companion(§5.1 允许保留);默认 columns=4 rows=2 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` / `TileOverlayModels.kt` | modified | 传递 workbench presentation 到渲染层 |
| `client/src/main/kotlin/com/ktome/client/audio/AudioRouter.kt` | modified | inventory open/close 音效路由 |
| `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt` | modified | PR05-1 typed scenario 注册 |
| `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` | modified | 4 个 golden labels + workbench/standard/min-window 三个 capture 流程 |
| `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | modified | workbench renderer 集成断言 |
| `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | modified | 焦点 ↔ 选中分离、page reset、focus reset、PgUp/PgDn 测试 |
| `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | modified | PR05-1 validation snapshot 数据生成 |
| `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt` | modified | 新增 `dark-uiux-pr05-1-inventory-page-workbench` scenario |
| `game/src/main/resources/i18n/{en-US,zh-CN}.json` | modified | 新增 `ui.key.*`、`ui.inventory.workbench.*`、`ui.inventory.footer.*`、`ui.inventory.action.*`、`ui.inventory.{page,capacity,empty_selection,empty_compare,slot.visual_only_unavailable,companion.page_hint}` tokens |
| `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt` 等 | modified | PR05-1 whitebox scenario CLI、materialization catalog、YAML、CLI 测试 |
| `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md` | modified | 最终 PASS 手工证据 |

---

## Root cause & assumptions

### 设计意图
PR05-1 是把"覆盖式背包大弹窗"升级为"主流程长期可用全屏 workbench"的合同 PR,**禁止**改动 core/game/inventory 规则、save、replay、profile、shop 与资源管线(§2 / §8 / §13)。所以本 PR 的全部增量都集中在:
1. **客户端 presentation 层**: `InventoryWorkbenchPresentation.kt` 作为唯一 owner(§5.1)。
2. **客户端 layout 层**: 新增 `InventoryWorkbenchLayout.kt` 三档 profile(§4.1)。
3. **客户端 input 层**: 全屏键盘+鼠标输入模式(§6 / §9 移除老 modal 与 hover-overwrites-selection)。
4. **客户端 renderer 层**: TileRenderer 增加 workbench 绘制路径。
5. **i18n / golden / whitebox / manual evidence** 同步落地(§10 / §11 / §12)。

### 关键假设(若与实际不符,findings 重点会变)
1. `EquipmentInventoryPresenter` 在 `UiMode.INVENTORY` 全屏路径**没有**被消费,仅服务 map shell companion(§5.1 默认允许)。证据:`TileRenderer` workbench 绘制路径只读 `InventoryWorkbenchPresentation`;`InputHandler` `pollInventoryCommand` 路径不再触发 8-slot 老分页(确认见 `rightPanelBackpackPageSize` 现仅在 `resolveHoveredInventoryIndex` 的 map shell hover 路径出现)。
2. PR05-1 validation scenario 在 `FoundationGameSession.kt` 中以 typed snapshot 产出可堆叠 consumable + 不同 affix 的装备 + 不同 special 的物品,这是 stack identity 区分能力的真源。
3. `materialNameKey` 在当前 PR05-1 demo 数据中对 consumable 类基本为 `null` 或同一值,因此 stack identity 缺 `materialId` 字段不会触发可视回归;但合同上不达标。

---

## Findings

### F1 [MEDIUM][Correctness / Spec drift] `detailRows.take(7)` 在所有 viewport 都吃 7 行,违反 §4.1 `detailMaxLines = 7/6/5`

- **Where**: `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:1062`、对照 `client/src/main/kotlin/com/ktome/client/render/layout/InventoryWorkbenchLayout.kt:118-170`、对照 spec §4.1(`UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md:225-231`)。
- **Evidence**:
  - Spec §4.1 明确 `detailMaxLines`: `1672x941 → 7`、`1280x800 → 6`、`1024x768 → 5`,并补充 "At `1024x768`, the workbench uses `4px..6px` icon inner padding and clamps detail content to max `5` lines"。
  - 实现:`workbench.detailRows.take(7).forEach { ... }` 在 `drawInventoryWorkbenchDetail` 中固定吃 7 行,不读 `InventoryWorkbenchLayoutProfile`;`InventoryWorkbenchLayoutProfile` 数据类完全没有 `detailMaxLines` 字段。
  - 即:1280x800 实际允许 7 行(spec 限 6),1024x768 实际允许 7 行(spec 限 5)。
- **Impact**: 在窄屏多 stat / 装备物品(典型场景:大量被动 description + statModifier)上,详情区会突破合同行数,可能挤压 compare 块、action 块或与 footer 重叠。Golden `dark-uiux-pr05-1-inventory-min-window` 在当前 fixture(consumable / 简单装备)下视觉看不出差异,所以 golden 没 catch;一旦未来 fixture 多 stat,合同立刻被打破。
- **Standards**: PR05-1 spec §4.1;CWE-1041(Use of Redundant Code) 不适用,但属于 "constants leaked across layers" 反模式。
- **Repro**:
  1. 构造一个 stackable consumable + 8 行 description 的 fixture(`statDefinitions` 全开)。
  2. 在 1024x768 触发该物品 selected。
  3. 观察 `detailRows.size == 8`,renderer 仍然渲染 7 行;spec 要求 5 行 + 截断 hint。
- **Recommendation**(最小修复):
  1. 在 `InventoryWorkbenchLayoutProfile` 增加 `val detailMaxLines: Int`,三档值 7/6/5,与 spec §4.1 表 1:1。
  2. `TileRenderer.drawInventoryWorkbenchDetail` 接收 profile 或经由 `GameShellBounds` 携带的 `detailMaxLines`,把 `take(7)` 改为 `take(profile.detailMaxLines)`。
  3. `compareRows.take(5)` 同步收到 profile(spec 未直接限,可保留 5 作为硬上限)。
  4. `InventoryWorkbenchPresenterTest` 增加一个 viewport-driven layout assertion(目前测试只检查 column widths / cell sizes,没检查 `detailMaxLines`)。
- **Tests**:
  - `InventoryWorkbenchPresenterTest` / `TileRendererCanvasTest` 增 case:1024 viewport + 8-row detail fixture,断言 renderer 只画 5 行。
  - 1672 / 1280 也分别加 case 防止回归。

---

### F2 [MEDIUM][Architecture / Spec drift] Stack identity 推导住在 client 端,缺 typed `stackKey` 上游字段,潜在 `materialNameKey` 漏判

- **Where**: `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt:125-143`、对照 spec §5.3.1 / §5.3.3 / §5.3.7。
- **Evidence**:
  ```kotlin
  // InventoryWorkbenchPresentation.kt:125-143
  private fun stackIdentity(item: ItemRenderSnapshot): String? {
      val typeId = item.typeId.uppercase()
      if (typeId !in stackableTypeIds || item.slotId != null || item.specialTemplateId != null || item.specialTierId != null) return null
      if (item.affixIds.isNotEmpty() || item.affixNameKeys.isNotEmpty() || item.stats != ItemStatModifierSnapshot()) return null
      return listOf(
          item.baseItemId, item.typeId, item.qualityTierId,
          item.effectTypeId.orEmpty(), item.resourceTypeId.orEmpty(),
          item.magnitude.toString(),
      ).joinToString(separator = "|")
  }
  private val stackableTypeIds = setOf("CONSUMABLE", "MATERIAL", "CURRENCY")
  ```
  - `ItemRenderSnapshot` 字段(`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:686-707`)有 `materialNameKey: String?`,但 stack identity **没**包含它。
  - §5.3.1: "Stack identity 必须来自 typed stack key 或 typed item identity,不得由 renderer 比较 localized name、display text、iconKey、asset path 或排序位置推导"。当前代码使用的是 typed 字段拼接,**形式上符合**这一条(没有用 localized name)。
  - §5.3.3: "装备、unique、artifact、带不同 material/affix/special template/stat roll 的 item 默认不自动合并"。当前 `stackIdentity` 不读 `materialNameKey`,且 `ItemRenderSnapshot` 没有 typed `materialId`。两个 baseItemId/typeId/qualityTierId/effectTypeId/resourceTypeId/magnitude 全部相同、仅 `materialNameKey` 不同的 consumable 会**错误合并**。
  - §5.3.7: "如果当前 `InventoryEntrySnapshot` 不足以表达 stack quantity,PR05-1 必须新增 typed presentation field 或上游 snapshot 字段"。当前实现选择"在 client 端推导"而不是"在上游加 typed `stackKey` 字段",这是 §5.3.7 的**精神违反**(虽然字面允许"typed item identity")。
  - 另:`stackableTypeIds` 集合是 client renderer 的硬编码策略,未来 core 新增可堆叠 typeId(如 `RUNE`、`SCROLL`)时,client 必须改代码同步,违反 §1 "不引入第二套 inventory state"的精神。
- **Impact**:
  1. **可见 bug 风险(低概率)**: 不同 material 的同款消耗品(罕见但合同允许)会合并显示为同一格,玩家无法区分。
  2. **演进风险(高概率)**: core/game 新增 stackable typeId 时,client 不更新就显示为单独格;反向若 core 限制更严,client 仍合并。两边逐渐 drift。
  3. **审查风险**: 下一个 PR 想做 typed `stackKey` 时,要先回收 client 推导逻辑、迁移所有 fixture,成本被 PR05-1 推后。
- **Standards**: CWE-710(Improper Adherence to Coding Standards)更接近"分层泄漏";本质是 `presentation derives policy` 反模式。
- **Repro**:
  1. 构造两个 consumable,`baseItemId="hp_potion"`、`typeId="CONSUMABLE"`、`qualityTierId="NORMAL"`、`effectTypeId="HEAL"`、`resourceTypeId=null`、`magnitude=30`,但 `materialNameKey` 一个是 `"item.material.iron"` 另一个是 `"item.material.glass"`。
  2. 当前实现合并为一格 `x2`;spec 要求两格各 `x1`(因为 material 不同)。
- **Recommendation**:
  1. **最小修复(本 PR 内可做)**: 在 `stackIdentity` 拼接列表中加入 `item.materialNameKey.orEmpty()`(只用其作为 typed 等价 key,不读其文本)。同时把 `stackableTypeIds` 从 client 移到 `ItemRenderSnapshot` 旁边的常量 or `companion object`,作为 `core` 层 typed 列表。
  2. **下一轮(推荐 PR05-2 或 PR05-1-followup)**: 在 `ItemRenderSnapshot` 增加 `val stackKey: String? = null`,由 `FoundationGameSession` typed 输出,client 仅消费;client 端 `stackIdentity` 改为 `return item.stackKey`,白名单与拼接逻辑全部下沉。
  3. 修复后增加 focused test:不同 materialNameKey 的同款 consumable 不合并;同 materialNameKey 合并;装备添加 affix 后不合并(已覆盖)。
- **Tests**:
  - `InventoryWorkbenchPresenterTest` 增 case:`material variant of stackable consumable does not merge`。
  - 若做 stackKey 上游字段:`FoundationGameSessionTest` 或 inventory snapshot test 断言 stackKey 输出。

---

### F3 [MEDIUM][UX / Spec drift] Footer hints 与 action rows 在窄屏静默裁剪,违反 §4.5.3 / §4.5.4

- **Where**:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:1076-1087`(`drawInventoryWorkbenchDetail` actions 绘制)
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:1098-1108`(`drawInventoryWorkbenchFooter` footer hints 绘制)
- **Evidence**:
  ```kotlin
  // TileRenderer.kt:1076-1087(actions)
  workbench.actions.forEach { action ->
      ...
      actionX += keyWidth + labelWidth + 16f
      if (actionX > column.right - 110f) { return@forEach }   // 静默丢弃后续 actions
  }
  // TileRenderer.kt:1098-1104(footer hints)
  workbench.footerHints.forEach { hint ->
      ...
      if (cursorX + keyWidth + labelWidth > footer.right - 10f) { return@forEach }  // 静默丢弃后续 hints
  }
  ```
  - §4.5.3:"如果动作不可用,**保留 key label 但置灰**,并显示短原因或禁用状态"。当前 renderer 直接 `return@forEach` 让整个 action 消失,既不保留 key label 也不置灰。
  - §4.5.4:"容量和页码必须可见"。footer hints 与 page/capacity 共享行高;在 1024x768 detail column min 280px + actions 4 个 + footer 6 hints 的组合下,有溢出风险。
  - 当前 manual 截图 `06-inventory-min-window-1024x768.png` 在 selectedEntry 为简单 consumable 时 actions 数较少,看不出来;但 selectedEntry 为有 `equip_one` + `drop_one` + `inspect` + `return` 的装备 + 多语言长 label 时会触发裁剪。
- **Impact**:
  1. 玩家在窄屏可能看不到 `D` drop 或 `Esc` return 的 footer 提示,导致键位不可发现。
  2. action 行如果裁掉 `return`,玩家以为退出方式只有 `Esc` keyboard;若同时 `Esc` 提示也被 footer hint 裁掉,操作可发现性归零。
  3. Golden 在当前 fixture 不暴露问题,所以 PR 通过。
- **Standards**: WCAG 操作可发现性、ToME 风格的 footer 一致性;CWE-1284(Improper Validation of Specified Quantity in Input)非典型适用。
- **Recommendation**(最小修复):
  1. Action 行:不让长 label 单独占满一栏。可加 `chip` 模式(只画快捷键 + 1 字符图标),并把超出区域改为换行而非丢弃;`equip_one` / `drop_one` / `use_one` 的长 label 在窄屏 fallback 到短 token(spec §4.5.3 允许置灰但要保留 key)。
  2. Footer hints:若总宽超 footer,自动启用 "single-letter key only" 紧凑模式;page/capacity 在 1024x768 必须画在 footer 最右,且独立 reserve 一段宽度。
  3. 改完后 `InventoryWorkbenchPresenterTest` 或 `TileRendererCanvasTest` 增 case:`actions and footer hints survive at min viewport with worst-case fixture`(覆盖 4 actions + 6 hints + 长中文 label)。
- **Tests**:
  - `TileRendererCanvasTest`: 1024x768 渲染,断言 6 hints 都进了 `canvas.textDraws` 且都在 footer bounds 内;4 actions 至少 keyText 都画了。

---

### F4 [LOW][Maintainability] `rightPanelBackpackPageSize = 8` 残留,§9 已声明删除但 map shell companion 仍使用

- **Where**: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:131` 与 `:1821`。
- **Evidence**:
  ```kotlin
  // InputHandler.kt:131
  private val rightPanelBackpackPageSize = 8
  // InputHandler.kt:1820-1822(在 resolveHoveredInventoryIndex 中)
  val pageStart = (selectedPosition / rightPanelBackpackPageSize) * rightPanelBackpackPageSize
  return sortedInventory.getOrNull(pageStart + slotIndex)?.index
  ```
  - §9 Removal Plan 明确把 "8-slot page size" 列入应删除。
  - §5.1 又允许 `EquipmentInventoryPresenter` 继续服务 map shell companion。
  - 现状:companion path 仍按 8 分页,但 page size 来源是 `InputHandler` 局部常量,而不是 `EquipmentInventoryPresenter` 或 `DemoShellLayout`。两边 drift 后,companion 上的 hover 与 presenter 显示分页可能不同步。
- **Impact**: 单点低风险,但与 §9 文字冲突;未来调整 companion 行/列时,会忘记同步这个常量。
- **Recommendation**: 把 `rightPanelBackpackPageSize` 改为从 `EquipmentInventoryPresenterRequest.inventoryColumns * inventoryVisibleRows` 推导,或下放到 `DemoShellLayout` 的 backpack 段;或最少在常量旁加注 "companion path only, §5.1 keeps it"。
- **Tests**: 现有 companion path 测试已覆盖 hover,补 1 个注释即可。

---

### F5 [LOW][Code smell] `textRow()` 已声明但无任何调用,属于 dead code

- **Where**: `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt:744-755`。
- **Evidence**: `rg '\btextRow\b'` 在整个仓库只命中本处声明,无调用点。`detailRows` 走 `row(...)` 或 `InventoryWorkbenchTextRowModel(...)` 直接 inline。
- **Impact**: 维护噪音、给读者错误信号(以为存在 generic text 行构造器);CLAUDE.md "不为单次逻辑提前抽象 / 不顺手重构相邻代码" 中,这一项属于本 PR 自己引入的死代码,**应清理**。
- **Recommendation**: 直接删除整个 `textRow` 函数;若未来真的需要,重新引入时再加。
- **Tests**: 无需新增。

---

### F6 [LOW][Code smell] `row()` helper 5-positional-name 反模式,把同一 value 传给 5 个不同 placeholder

- **Where**: `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt:656-670`,调用点仅 `:464`(`row(localizer, "effect:heal", "ui.inspect.restore_hp", item.magnitude.toString())`)。
- **Evidence**:
  ```kotlin
  // :656-670
  private fun row(localizer: Localizer, sourceFieldId: String, labelToken: String, value: String): InventoryWorkbenchTextRowModel {
      val text = localizer.text(labelToken, "slot" to value, "quality" to value, "material" to value, "affix" to value, "amount" to value)
      ...
  }
  ```
  - 把同一个 `value` 同时绑定到 `slot`、`quality`、`material`、`affix`、`amount` 五个占位,只是因为不知道 token 用哪个 placeholder。当前唯一调用只用 `amount`("ui.inspect.restore_hp" 的 token 模板)。
  - 风险:如果未来某个 token 同时含 `slot` 和 `quality`(罕见但允许),会被同一 value 灌满,渲染错乱。
- **Impact**: 维护反模式;CLAUDE.md "命名混乱 / 局部坏味道" 范畴。
- **Recommendation**: 把 `row()` 改为只接收一个 `placeholder: String` 与 `value: String`,调用点写 `row(localizer, "effect:heal", "ui.inspect.restore_hp", placeholder = "amount", value = item.magnitude.toString())`;或者直接 inline 这一处构造 `InventoryWorkbenchTextRowModel`,不要这个 helper。
- **Tests**: 无新增需求;`InventoryWorkbenchPresenterTest` 已覆盖 `restore_hp` 的输出文本。

---

### F7 [LOW][Premature abstraction] `InventoryWorkbenchStackActionScope` 单值枚举

- **Where**: `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt:66-68`。
- **Evidence**:
  ```kotlin
  internal enum class InventoryWorkbenchStackActionScope { SINGLE_ENTRY }
  ```
  - 只有一个值,无任何分支消费;`InventoryWorkbenchStackActionTarget.scope` 永远是 `SINGLE_ENTRY`。CLAUDE.md "不为单次逻辑提前抽象 / 不为将来可能需要增加配置项、扩展点或通用框架"。
- **Impact**: 维护噪音、隐含的"未来会有 BULK / FULL_STACK"假设没有 spec 支持。
- **Recommendation**: 删除枚举与字段;若 spec 后续(§7 stack action contract)真的要 BULK,作为该 PR 一部分再引入。
- **Tests**: 无。

---

### F8 [LOW][Spec / Implementation precision] `equipmentTargetSlotId` 表达方式

- **Where**: `client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt:146-159`(`InventoryWorkbenchEquipmentSocketModel`),对照 spec §5.1 表必填字段。
- **Evidence**: spec §5.1 列出顶层字段 `equipmentTargetSlotId`(target cue only when existing typed slot matches);实现把它分散在每个 socket 的 `targetCue: Boolean` + `selected: Boolean` 两个字段上,等价但不显式。
- **Impact**: 不影响功能;但合同审查时 reviewer 需要去 socket 列遍历推断 `targetSlotId`,降低可读性。
- **Recommendation**: 在 `InventoryWorkbenchPresentation` 顶层加一个 `val equipmentTargetSlotId: String?` 字段(由 `equipmentSockets.firstOrNull { it.targetCue }?.slotId` 推导,只读、不重复 source-of-truth)。或者把 spec §5.1 改成更贴近实际的描述,但 spec 是合同,实现就近。
- **Tests**: 新增 `equipmentTargetSlotId mirrors first target cue socket` 即可。

---

### F9 [NIT][Code smell] `equipmentSockets` 视觉占位的 `tooltipAnchorId` 命名

- **Where**: `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt:122-128`(legacy companion path)使用 `tooltipAnchorId = "visual-socket-$index"`,但在 workbench `InventoryWorkbenchPresentation.kt:319-323`,visual-only sockets 的 `tooltipToken=null` 且没生成稳定 anchorId(走默认)。
- **Evidence**: §4.2 表 `disabledReasonToken = ui.inventory.slot.visual_only_unavailable`,工程上现已对齐;但 anchorId 命名不统一,后续 tooltip 系统接入时需要补 anchorId。
- **Impact**: 仅是接入新 tooltip 实现时的轻微返工成本。
- **Recommendation**: 后续给 workbench visual-only socket 一致命名(如 `"workbench-visual-socket-$index"`)。

---

## Performance

### Hotspots / 复杂度
- `InventoryWorkbenchStacks.groups()`(`:79-94`)对 inventory 一次 sort + 一次 linkedMap 累积 + 一次 toList + 一次 sortBy,**总体 O(N log N)**,N 取决于背包大小(典型 24 ~ 100 量级),无性能压力。
- `pageIndexForEntry` / `coordinateForEntry`(`:99-116`)使用 `indexOfFirst`,N 量级下可接受。
- TileRenderer 的 workbench 路径在每帧都重新 walk `cells`(24)和 `equipmentSockets`(9),无 cache;符合既有 renderer 风格,无明显回归。

### Bench / 监控
- `dark-uiux-pr05-1-inventory-workbench` golden 已落地,后续若加大背包容量或 stack 维度,关注 `:client:goldenScreenshot` 与 `:client:packageMacApp` 时间。
- 没有 N+1 query / 远程调用 / 锁;无监控新增需求。

---

## Integration

### API / Contracts
- 无 cross-service contract 改动;`ItemRenderSnapshot` 未新增字段(F2 建议加 `stackKey` 列为后续 PR)。
- `EquipmentInventoryPresenter` 仍然存在并被 map shell companion 使用,**未删除**——符合 §5.1。
- `UiMode.INVENTORY` 全屏路由切换,`pollMapBackpackPagingNoop` 在 MAP mode 把 PgUp/PgDn 转 no-op(§12 / §9)。

### Migrations / Rollout
- 无 DB / migration / 持久化格式改动。
- `i18n` token 增量,en-US 与 zh-CN 双语已对齐;`localeLint` 应能 catch missing key。
- Feature flag:无,新 workbench 直接替换老 modal。回滚路径:revert 该 PR 即可,无下游依赖。

### Resilience
- `InventoryWorkbenchPresenter.present` 对空 `inventory` / 空 `equipment` / `selectedEntryId=null` 都已 typed empty state(`empty_selection` / `empty_compare`),test 覆盖。
- `resolveExact()` 在 visual key 不存在时 `require` 抛错——是 fail-fast,符合 dark UI manifest 合同。

### Rollback Plan
- `git revert` 该分支的合并 commit 即可;`InventoryWorkbenchPresentation.kt` / `InventoryWorkbenchLayout.kt` / golden labels 全部新增,删除后 map shell companion 自动接管。

---

## Testing

### 覆盖
- **§12 unit/input/renderer 必测项 vs 实测**:
  - `columns == 6 / rows == 4 / cells.size == 24` —— `InventoryWorkbenchPresenterTest`:`grid is six by four and exposes typed stack action target`。
  - 9 visual sockets(4 typed + 5 visual-only,visual-only `enabled=false` `tooltipToken=null` `disabledReasonToken="ui.inventory.slot.visual_only_unavailable"`)—— `workbench owns nine equipment sockets and disabled visual-only placeholders`。
  - Stack 合并阈值 `x2 / x10 / x99 / x99+` —— `stack quantity badge thresholds are stable`。
  - 装备/special/affix 不合并 —— `equipment and special identities do not merge`。
  - Empty selection / empty compare —— `empty selected entry renders empty state` / `empty inventory keeps full grid and disables item actions`。
  - 非装备物品不暴露 equip action —— `non equippable material-like item does not expose equip action`。
  - Detail / compare 来自 selected 而非 hover —— `detail compare and action rows are typed from selected entry rather than hover preview`。
  - Layout profiles column / cell minimums —— `layout profiles keep documented column and cell minimums`。
  - 输入:focus ↔ selection 分离、PgUp/PgDn、Esc、enter 时 reset focus/page —— `InputHandlerTest:1216-1548` 多个 case 覆盖。
  - Golden 4 labels —— `GoldenScreenshotHarnessTest:520-1156` 完整 capture。
- 总体覆盖:**§12 必测项 100% 命中**,且 9 个 presenter test + 多个 input test + 4 个 golden + 7 个手工截图。

### Gaps(本次审阅识别)
1. **缺少 viewport-driven `detailMaxLines` 断言**(详 F1)。
2. **缺少不同 `materialNameKey` 的 stack 合并断言**(详 F2)。
3. **缺少窄屏 worst-case fixture 下的 actions / footer 全可见断言**(详 F3)。
4. 没有看到针对 `EquipmentInventoryPresenter` 不再被 `UiMode.INVENTORY` 全屏路径消费的反向断言(可加一个 `inventory mode full screen does not consume EquipmentInventoryPresenter` lint / 注释,防止后续 drift)。

### 抖动 / Flakiness 风险
- Golden 截图依赖 PID 7464 与 macOS window id,手工记录提供 sha256 sidecar,可重复性 OK。
- `InventoryWorkbenchPresenterTest` 全 typed,无时间 / 顺序依赖。

### Targeted test plan(配合 finding 修复)
- Given a fixture with 8 description lines and statModifier rows, When workbench is rendered at 1024x768, Then `canvas.textDraws` for detail tone-tagged lines count ≤ 5。
- Given two consumables with same baseItemId/typeId/qualityTierId/effectTypeId/resourceTypeId/magnitude but different `materialNameKey`, When grouped, Then `groups.size == 2` and `quantityText == null`。
- Given a selected equipment with 4 actions and 6 footer hints at 1024x768, When rendered, Then footer textDraws contains all 6 keyText and detail textDraws contains all 4 shortcutText。

---

## Docs & Observability

### Docs
- **Spec ↔ Implementation 一致项**: §0 acceptance matrix 11 项 / §3.4 reference exclusion / §4.1 layout profiles 数值 / §4.2 socket mapping / §4.3 grid 数值 / §5.1 owner / §5.2 grid 6x4 / §5.3.1-5.3.6 stack 部分 / §6 input / §9 PageUp/PageDown no-op / §10 4 golden labels / §11 manual record / §12 focused tests / §13 non-goals —— **全部对齐**。
- **不一致项**: §4.1 `detailMaxLines`(F1)、§5.3.3 `materialId` 区分(F2)、§4.5.3/4.5.4 actions+footer 可见性(F3)、§5.1 表 `equipmentTargetSlotId` 命名(F8)。
- 不需要新增 README / ADR。`UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md` 已有 PASS 状态。

### Observability
- `app.log` / `pgrep-final-cleanup.txt` 已包含;无新的 metrics / traces 需求。
- 建议 `localeLint`、`contractLint`、`maintainabilityLint` 接入 PR 后再 check 一次(spec §0 gate budget 已声明,但 manual record 没显示运行结果)。

---

## Open questions

1. **是否在本 PR 内做 F1(`detailMaxLines` viewport-driven)的最小修复?** 它是 spec 明确数值偏差,修复 5-line patch + 1 test。建议合入。
2. **F2 的 stackKey 上游字段(`ItemRenderSnapshot.stackKey`)放在 PR05-1 还是 PR05-1-followup?** 合规角度看 PR05-1 内做更好(§5.3.7 是 blocking 语气);风险角度看 followup 也可接受,但要写入 spec 的 "deferred" 段落。
3. **是否给 `EquipmentInventoryPresenter` 在 inventory route 路径加 lint?**(防止后续 PR drift 回到双路径)。
4. **`pgrep-final-cleanup.txt` 显示空,但 manual record 没明确写各 gate(`localeLint / contractLint / maintainabilityLint / verifyChanged`)单独运行过吗?** 建议补一行 gate 运行证据。

---

## Final recommendation

### Decision
**comment**——可以合入,但建议在合并前修 F1 / F3 两条 MEDIUM(都属于 renderer 局部,patch 体量小、风险低、改完不需要重跑 manual);F2 可拆 followup,但要在 PR 描述里登记。

### Must-fix before merge
- Prioritization rule: spec-compliance and low-effort renderer fixes are blocking regardless of numeric severity.
- Rationale: **F4** is LOW but belongs in **Must-fix before merge** because it resolves a direct §9 spec/source-of-truth conflict with a tiny patch; **F2** is MEDIUM but stays in **Nice-to-have post-merge** because adding an upstream typed stack field changes the render snapshot contract and is better handled in a dedicated follow-up.
- **F1** `detailRows.take(7)` viewport-driven(加 `detailMaxLines` 到 layout profile,patch ≈ 30 行 + 1 个 test case)。
- **F3** actions / footer 窄屏不静默裁剪(改成 chip 模式或换行,patch ≈ 40 行 + 1 个 worst-case test)。
- **F4** `rightPanelBackpackPageSize` 加 source-of-truth 注释或下放到 layout(patch ≈ 5 行,与 spec §9 文字保持一致)。

### Nice-to-have post-merge
- **F2** 在 `ItemRenderSnapshot` 或 `EquipmentInventoryPresenterRequest` 加 typed `stackKey`,client 端把白名单与拼接逻辑下沉;同时补 `materialNameKey` 差异化测试。
- **F5 / F6 / F7** 死代码与 premature abstraction 清理;同一 follow-up PR 一起做。
- **F8** 顶层 `equipmentTargetSlotId` 字段(辅助 reviewer 直接读)。

### Confidence
**high**——所有 finding 都附 file:line 与 spec 章节交叉引用;9 个 presenter test / 多个 input test / 4 个 golden / 7 个手工截图均已落地并可重现;未识别 BLOCKER / HIGH 风险;未发现安全 / 性能 / 数据一致性问题;不存在 core / game 规则越界。

---

## Appendix: 合同 / 实现一致性矩阵(精简版)

| Spec 章节 | 关键要求 | 实现 | 状态 |
| --- | --- | --- | --- |
| §0 Acceptance Matrix(11 reqIds) | 11 条都落到 owner / fastCheck / gate / artifact / whitebox | `Phase4V4AcceptanceContractLintTest` + 现有路由 | PASS |
| §3.4 Reference Exclusion | denylist `ui.inventory.filter` / `weight` / `burden` / `负重` / `承重` 等 | `Pr05InventoryWorkbenchReferenceExclusionLintTest`(spec 声明位置) | PASS(待 lint 运行确认) |
| §4.1 Layout Profiles(数值 1672/1280/1024) | equipmentMinWidth/backpackMinWidth/detailMinWidth/gutter/footerHeight/cellSizeRange/iconInnerPadding/detailMaxLines | `InventoryWorkbenchLayout.kt:118-170` 三档,**缺 `detailMaxLines`** | **PARTIAL(F1)** |
| §4.2 Visual Socket(9 sockets) | 4 typed + 5 visual-only,disabledReasonToken="ui.inventory.slot.visual_only_unavailable" | `InventoryWorkbenchPresentation.kt:309-326`,`VISUAL_ONLY_SOCKET_COUNT=5` | PASS |
| §4.3 6x4 Grid | columns=6 rows=4 cellSize 稳定 / restrained cyan focus ring / quantity badge null·xN·x99+ / pagination 不烘焙进 icon | `InventoryWorkbenchGrid:30-46`、`quantityText:640-645`、`drawInventoryWorkbenchGrid` | PASS |
| §4.4 Detail / Compare(5-7 行) | typed detail / compare / 装备类有 current equipped compare / 非装备显示 use·drop·inspect | `detailRows / compareRows / actionRows` `:443-616` | PASS(行数偏差见 F1) |
| §4.5 Footer / Help | 6 hints / 不可用置灰保留 key label / 容量页码必须可见 | `drawInventoryWorkbenchFooter`、`footerHints:618-626` | **PARTIAL(F3)** |
| §5.1 Model Owner | `InventoryWorkbenchPresentation.kt` 唯一 owner;`EquipmentInventoryPresenter` 仅服务 companion | 实现一致 | PASS |
| §5.1 Required Fields | `stackGroups / hoveredCell / focusedCell / equipmentTargetSlotId` 等 | 全部存在,`equipmentTargetSlotId` 分散在 socket(F8) | **NOMINAL PARTIAL(F8)** |
| §5.2 Grid Assertions | columns/rows/cells.size/pageSize/page 来自 model | `InventoryWorkbenchPresenterTest` 已覆盖 | PASS |
| §5.3 Stack Grouping(7 条) | typed identity / 默认 only consumable·material·currency / 装备不合并 / x99+ / 6x4 容量按合并算 | `:79-143`,`materialId` 漏判 + 白名单在 client | **PARTIAL(F2)** |
| §6 Input Contract | Arrow/Enter/E/D/PgUp/PgDn/Esc;无 map mode 副作用;hover ≠ committed | `pollInventoryCommand:854-928`,`pollMapBackpackPagingNoop`,`updateInventoryWorkbenchHover` | PASS |
| §7 Typed Detail/Compare/Action | typed rows / disabledReasonToken | `detailRows / compareRows / actionRows` | PASS |
| §9 Removal Plan | 8-slot page size / hover-writes-selection / 硬编码 page 字符串 | 主路径已清理;**`rightPanelBackpackPageSize=8`** 残留在 companion path | **PARTIAL(F4)** |
| §10 Golden Labels(4) | workbench / compare / pagination / min-window | `GoldenScreenshotHarnessTest:162-167, 520-1156` | PASS |
| §11 Manual Record | 7 screenshots + sha256 + cleanup | `UI/manual-records/...` PASS | PASS |
| §12 Focused Tests | unit / input / renderer | 9 presenter + 多个 input + canvas test | PASS |
| §13 Non-goals | 不改 core/game rule / save / replay / profile / shop / manifest | git diff 内无该类改动 | PASS |

---

> 报告作者:Roguelike / ToME 设计审查负责人
> 输出日期:2026-05-21
> 适用分支:`codex/dark-uiux-pr05-1-inventory-workbench`
> 适用 commit 基线:`5f1d5de1` 及其上的 PR05-1 working tree
