# Phase 4 UI/UX PR04 深度审查报告

- **审查标的**: `phase4-uiux-pr04-status-description-and-readability`
- **审查分支**: `codex/phase4-uiux-pr04-status-readability`
- **审查基准文档**: `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md`
- **审查人角色**: 资深 Roguelike / 类 ToME 开发设计总监 · 系统策划总监 · 玩法体验审查负责人
- **审查日期**: 2026-04-23 (Asia/Shanghai)
- **审查方式**: 纯白盒静态审查 + 现有自动化记录核查，不修改源码，不执行手工白盒

---

## 0 · 总体结论

| 维度 | 评级 | 说明 |
| --- | --- | --- |
| 核心架构一致性 | `PASS` | `StatusPresentationModel` / `TelegraphPresentationModel` / `ExplainPaneModel` / `DescriptionPresenter` / `AccessibilityToggle` 已按 spec 4.1–4.6 完整实现，接线与职责分层符合规范。 |
| 公共契约稳定性 | `PASS` | `status` / `telegraph` / `modalCard` / `renderText` contract 未破坏；新增面向 UI 层的 presentation model 为 client-internal，无对 core/game 的签名级污染。 |
| UX 行为一致性 | `MINOR_DEVIATION` | Inspect+ExplainPane 的 Backspace / ESC 语义落地正确；但 `ExplainPaneModel.fromInspectSurface` 未自动聚合目标上的 keyword 到 `keywordChips`，与 spec 4.4「ExplainPane 是关键字入口」的叙述存在功能缺口。 |
| 门禁与防回归 | `MINOR_DEVIATION` | `keywordRegistryLint` 已落地并并入 `verifyChanged / verifyOwnerTaskPaths`，但 formal surface 扫描范围仍将 9 个 legacy keyword 退化为 WARN，且 lint input scope 尚未覆盖 `StatusPresentationModel.nameKey` 与 registry alias 的真实来源文件——与 spec 4.5「严格覆盖所有 formal surface」存在量化差距。 |
| 本地化 / 文案 | `MINOR_DEVIATION` | spec §4.7 列出的 10 个 locale key 全部就绪；但实现额外引入 `ui.status.badge.raw` 与 `ui.status.effect.line` 两个未在 §4.7 表中登记的 key，属于文档漂移（不是功能缺陷）。 |
| 可访问性 | `PASS` | 三开关 + `-Dktome.ui.a11y.*` system property 均生效；`TelegraphRenderer` 在 `colorBlindSafe / reduceMotion` 组合下返回非空 `riskCueBadge`，覆盖 spec §4.6 的冗余编码矩阵。 |
| 签收证据 | `GAP_BY_USER_REQUEST` | 手工白盒 (`BOSS_VARIANT / 20260412 / zh-CN / 1280x800`) 与 spec §6.3 要求的 all-three-flag startup run、phase4-uiux-pr04 screenshot/golden label 未执行，人工记录已在 `manual-records` 中以 `NOT_RUN_BY_REQUEST` 记录。 |

**落锤结论**: **有条件通过**。可以作为 PR04 的落地基线合并，但 §3 中 5 个偏差项必须在 PR05 出口前关闭；其中 `fromInspectSurface` 未聚合 keyword 与 lint formal surface 覆盖不全 2 项，若留给 PR05 则需在合并信息或 follow-ups 中明确 owner 与截止窗口。

---

## 1 · 审查方法学

- **阅读范围**: 规范全文 × 实现源文件交叉对照（参见附录 A）。
- **证据等级**:
  - `STRONG` = 规范原文 ↔ 源码行级一一对应；
  - `MODERATE` = 规范意图在实现中有对应代码，但存在命名/路径漂移；
  - `WEAK` = 规范要求存在，但实现未覆盖或覆盖程度弱化。
- **风险权重**: 用「偏差量化」衡量需要修改的文件数 × 抽象边界影响 × 是否跨模块。
- **非审查范围**: 运行时性能、线上监控、Steam / Launcher 集成（不在 PR04 scope）。

---

## 2 · 符合项清单（规范 → 实现映射）

> 以下每条都是 spec 要求 × 实际实现的正面印证，证据等级标注 `STRONG`。

### 2.1 Status presentation 统一模型 (spec §4.1)

- **要求**: 单一 `StatusPresentationModel` 贯穿 HUD + icon resolver，含 `group / priority / rawBadge` 三要素，builder 纯函数。
- **实现**:
  - `client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt` 定义 `StatusPresentationGroup { BUFF, DEBUFF, NEUTRAL, TELEGRAPH, ZONE_EFFECT }`、`StatusPresentationBuilder.build / buildZoneEffect / buildTelegraph / sorted`。
  - `StatusHudRenderer.kt` 与 `StatusIconResolver.kt` 均走同一 builder，无二次格式化。
- **证据等级**: `STRONG`

### 2.2 Priority 公式 (spec §4.1.3)

- **要求**:
  - `TELEGRAPH = 900 + dangerLevel*20 + previewTurnsInverse`
  - `DEBUFF = 700 + stackWeight + remainingTurnsWeight`
  - `ZONE_EFFECT = 650 + dangerLevel*10`
  - `BUFF = 500 + stackWeight + remainingTurnsWeight`
  - `NEUTRAL = 300 + stackWeight`
  - 同分 tiebreaker：`typeId` 字典序。
- **实现**: `StatusPresentationBuilder.computePriority(...)` 与 `TelegraphPresentationModel.computePriority(...)` 完整复现。缺 `previewTurns` 时 inverse bonus 置 0，符合 spec §4.1.3 脚注。
- **测试**: `StatusPresentationModelTest` / `TelegraphPresentationModelTest` 覆盖相邻分组与缺失 preview turns 分支。
- **证据等级**: `STRONG`

### 2.3 Raw badge formatter (spec §4.1.4)

- **要求**: `"x{stack}"` / `"{stack}/{stackCap}"` / `"{turns}t"` / 复合 `"3/5 4t"`；pure formatter，不经 locale 分词。
- **实现**: `StatusPresentationBuilder.rawBadge(...)` 为纯 Kotlin 字符串拼接，不依赖 `LocaleResolver`。
- **证据等级**: `STRONG`

### 2.4 Telegraph compact projection (spec §4.2)

- **要求**: `TelegraphPresentationModel` 是最小 compact shape，通过 `toStatusPresentation()` 回投影到 `StatusPresentationModel`，不引入 `OverlayRenderSnapshot` 的字段扩展。
- **实现**: `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphPresentationModel.kt` 仅承载 `id / dangerLevel / previewTurns / visualKey / warningKey` 等最小字段，`fromOverlay()` 构造、`toStatusPresentation()` 调 `StatusPresentationBuilder.buildTelegraph`。
- **证据等级**: `STRONG`

### 2.5 DescriptionPresenter 六面统一 (spec §4.3)

- **要求**: talent / inventory / shop / inspect / combat_action / status_effect 六种 surface 必须走同一 presenter，关键字 lookup 走 `KeywordRegistry.CORE.require` fail-fast。
- **实现**:
  - `DescriptionPresenter.kt` 含 `DescriptionSurface` 枚举六项；`presentSurfaceLines` 统一入口，`renderKeywordTooltips` 调 `KeywordRegistry.CORE.require`。
  - `TileRenderModel` / `AsciiRenderModel` 的 runtime 渲染路径经 `presentModalCardLines` 汇聚。
- **证据等级**: `STRONG`

### 2.6 ExplainPane 是 sub-view 而非新 modal 层 (spec §4.4)

- **要求**:
  - 不新增 `UiMode`，不增加 modal stack 深度；
  - `Backspace` 先关 ExplainPane；`ESC` 全退；
  - `ExplainPaneModel` 复用 `ModalCardModel` composition wrapper，不持有裸字符串说明。
- **实现**:
  - `ModalStack.kt` 中 `ModalFrameLocalState.explainPaneOpen: Boolean` 是 frame 内部字段，不影响 stack 深度或 frame 类型；
  - `InputHandler.kt` 的 `pollInspectCommand` 在 Backspace 时先处理 `explainPaneOpen`，SLASH 作为打开入口，mode/focus 切换时清零；
  - `ExplainPaneModel.kt` 仅持 `ModalCardModel + keywordChips + referenceChain`，`init` 校验非空 key。
- **证据等级**: `STRONG`

### 2.7 Keyword registry lint 门禁 (spec §4.5)

- **要求**: 引入 `keywordRegistryLint`，在 `verifyChanged / verifyOwnerTaskPaths` 中启用，未知 id 必须 fail-fast。
- **实现**:
  - `tools/src/main/kotlin/com/ktome/tools/lint/KeywordRegistryLintRule.kt` 暴露 `Severity { WARN, ERROR, BLOCKED }` 与 `KeywordRegistryCoverageMode { CONSUMED_ONLY, FULL_REGISTRY }`，未知 id → `ERROR`；
  - `tools/build.gradle.kts` 注册 `keywordRegistryLint` task；根 `build.gradle.kts` 建别名；`VerificationTaskRegistry.kt` 将 `keywordRegistry` 纳入 STRICT_ZERO_FAILURE domain 并声明 input scope。
- **证据等级**: `STRONG`（覆盖范围偏差见 §3.2）

### 2.8 Accessibility toggle 三开关 (spec §4.6)

- **要求**: `highContrast / colorBlindSafe / reduceMotion` 三开关独立生效，接受 `-Dktome.ui.a11y.*`，并在 telegraph/overlay 层体现。
- **实现**:
  - `client/src/main/kotlin/com/ktome/client/ui/settings/AccessibilityToggle.kt`：三字段 + `distinctionMethods` 派生集合；`fromSystemProperties()` 读取 `ktome.ui.a11y.highContrast / colorBlindSafe / reduceMotion`；`overlayAlpha` 在 `highContrast` 时兜底 `0.82f`、`reduceMotion` 时 `0.74f`；`riskCueBadge(dangerLevel)` 仅在 `colorBlindSafe || reduceMotion` 时返回非空。
  - `TelegraphRenderer.kt` 的 `alpha()` / `riskCueBadge()` 接入上述 API。
- **测试**: `AccessibilityToggleTest` 覆盖三开关独立与组合矩阵。
- **证据等级**: `STRONG`

### 2.9 Locale key 就绪 (spec §4.7 表)

- **要求**: 10 个 `ui.status.*` / `ui.explain.*` / `ui.badge.*` key 必须同时在 `zh-CN.json` 与 `en-US.json` 落地。
- **实现**: `game/src/main/resources/i18n/{zh-CN,en-US}.json` 已补齐 10 个 key，文案风格与既有面板一致。
- **证据等级**: `STRONG`（新增两个未在 §4.7 登记的 key，见 §3.3）

---

## 3 · 偏差项与量化

> 每条偏差给出：规范锚点 → 实现事实 → 偏差等级（`MINOR / MODERATE / MAJOR`）→ 量化（文件数 × 影响面）→ 修复建议。

### 3.1 `ExplainPaneModel.fromInspectSurface` 未自动聚合 keyword chips

- **规范锚点**: spec §4.4「ExplainPane 是 inspect 目标→keyword 的入口，默认应呈现目标关联的核心 keyword chips」、§4.3 关于 description surface 关键字覆盖的叙述。
- **实现事实**:
  - `ExplainPaneModel.fromInspectSurface(...)` 仅调用 `DescriptionPresenter.inspectSurfaceCard(surface)` 后 `return fromCard(card)`，显式传 `keywordIds = emptyList()`（见 `ExplainPaneModel.kt:66`）。
  - `ExplainPaneModelTest.kt` 现有用例也仅覆盖 `fromCard` 手动传入 keywordIds 的路径，`fromInspectSurface` 用例断言都没有检查 `keywordChips` 非空。
- **偏差等级**: `MINOR`（功能上 chip 仍可由 caller 手动传入，但默认入口体验缩水）。
- **量化**: 1 个文件变更 + 1 个测试补充；下游无签名影响。
- **修复建议**:
  1. 在 `DescriptionPresenter.inspectSurfaceCard(surface)` 内返回 `card` 的同时附带派生 `Set<String> keywordIds`（例如提取 `card.detailLines` 中 `[[keyword]]` 标记或由 `InspectDescriptionSurface.actor.statusEffects / item.tags` 折算）；
  2. `fromInspectSurface` 改为 `return fromCard(card, keywordIds.toList())`；
  3. 在 `ExplainPaneModelTest` 追加一个断言 `keywordChips.map { it.key }` 包含预期 keyword 的用例。
- **预计影响**: 仅触碰 `ExplainPaneModel.kt`、`DescriptionPresenter.kt` 的内部函数；无公共契约变化。

### 3.2 `keywordRegistryLint` formal surface 覆盖不全

- **规范锚点**: spec §4.5「formal surface = `DescriptionModel` / `ExplainPaneModel` / `StatusPresentationModel.nameKey` / registry alias」应全部纳入扫描；§6 出口门禁要求严格零 WARN。
- **实现事实**:
  - `VerificationTaskRegistry.kt` 将 `keywordRegistryDomain` 的 input scope 明确声明为 `KeywordRegistry.kt / DescriptionPresenter.kt / ExplainPaneModel.kt / StatusPresentationModel.kt` 四个文件；但实际 formal surface 还包括：
    - `StatusPresentationBuilder` 中对 `nameKey` 的传递路径（目前通过 typeId→locale 间接消费，不在 lint parse tree 中）；
    - `KeywordRegistry` alias 条目（例如 `damage / burn / stun` 的 alias 表），lint 规则只扫描 `require(id)` 调用点，没有校验 alias 值对应 keyword 的完整性。
  - 现状：repo 正面 case 触发 `WARN=9`（legacy keyword），follow-ups.md 将其降级到 PR-05 关闭。
- **偏差等级**: `MODERATE`。spec §6 明确要求「严格零 WARN 作为 PR04 出口条件」，当前用 follow-ups 推迟到 PR05，相当于软化出口门禁。
- **量化**: 1 个 lint 规则增强 + 1 个 input scope 扩容 + 最多 9 条 legacy keyword 的 formal surface 注册或清退；影响面限制在 `tools/` 模块。
- **修复建议**:
  1. 扩展 `KeywordRegistryLintRule`：在扫描 `DescriptionPresenter.replaceKeywordMarkup` 的同时，增加 `StatusPresentationModel.nameKey` 与 `KeywordRegistry` alias 集合的交叉校验；
  2. 将 legacy 9 个 keyword 要么补齐 formal surface 引用，要么在 `KeywordRegistry` 中显式标注 `legacy = true` 并让 lint 白名单放行（白名单需同时登记到 `follow-ups.md` 的 PR-05 清单）；
  3. 把 `StatusPresentationBuilder.kt`（若与 model 分文件）与 `StatusPresentationModel.kt` 同时纳入 `keywordRegistryDomain.inputs`；
  4. 重新跑 `./gradlew keywordRegistryLint`，确认 `WARN = 0` 或显式 acknowledged。
- **推荐时限**: PR04 合并前整改；若必须留给 PR05，须在 PR 描述中显式声明为 known-exception。

### 3.3 两个 locale key 未在 spec §4.7 登记

- **规范锚点**: spec §4.7 以表格形式列出 10 个 key；未列明的 key 视为未授权扩展。
- **实现事实**: `game/src/main/resources/i18n/{zh-CN,en-US}.json` 新增了 `ui.status.badge.raw = "{value}"` 与 `ui.status.effect.line = "{name} {badge}"`，用于 builder 构造 `RenderTextTokenSnapshot`。这两个 key 不在规范表中。
- **偏差等级**: `MINOR`（文档漂移，功能本身正当）。
- **量化**: 文档侧 1 次 spec amendment 或 1 条 follow-ups 登记；代码无需动。
- **修复建议**:
  - 在规范 §4.7 中追加两行，并注明用途（前者：raw badge formatter 的 token 包装；后者：ExplainPane / HUD 的 status line 模板）。
  - 若团队约束规范冻结，则在 `follow-ups.md` 明确列出「PR04 实现附加两个 locale key，等下一次 spec 刷新补登」。
- **附加提醒**: `ui.status.badge.raw = "{value}"` 只有一个 argument，等价于 pure formatter；但它仍走了 `LocaleResolver` 路径。若希望彻底落实 spec §4.1.4「raw badge formatter 不经 locale」，应考虑直接用 `RenderTextTokenSnapshot.literal(badgeText)` 或让 `badgeText` 字段直接存原始字符串，避免 resolver 参与（见 §3.5）。

### 3.4 手工白盒 `NOT_RUN_BY_REQUEST` 与 spec §6.3 冲突

- **规范锚点**: spec §6.3「出口门禁必须覆盖 `BOSS_VARIANT / 20260412 / zh-CN / 1280x800` packaged manual run + all-three-flag startup run + PR04 screenshot/golden label」。
- **实现事实**: `docs/opt/ui-pr/manual-records/phase4-uiux-pr04-status-description.md` 结论为 `AUTOMATED_PASS_MANUAL_NOT_RUN_BY_REQUEST`；第 4 行、第 34–46 行均标注用户要求本轮不跑手工白盒。
- **偏差等级**: `MAJOR`（流程风险，非代码风险）。若严格遵守 spec，当前 PR 不具备合规 signoff。
- **量化**: 不改代码；约 30 分钟 manual run + 截图留档 + 人工记录回填；或由项目负责人对 §6.3 进行书面豁免。
- **修复建议**:
  1. 按 spec §6.3 在合并前补跑 packaged manual run 并采集截图；
  2. 在 `manual-records/phase4-uiux-pr04-status-description.md` 更新 signoff，结论改为 `AUTOMATED_PASS_MANUAL_PASS`；
  3. 若项目负责人确认豁免，须在 `docs/opt/ui-pr/follow-ups.md` 与 PR 描述中互相引用，并给定可追溯 owner。

### 3.5 Status badge 仍经 locale 通道渲染

- **规范锚点**: spec §4.1.4「raw badge formatter 纯函数，不进 locale 管道」。
- **实现事实**: `StatusPresentationBuilder.rawBadge(...)` 本身是纯拼接，但其输出被包装成 `RenderTextTokenSnapshot(key="ui.status.badge.raw", arguments=[RenderTextArgument(name="value", value=badgeText)])`，最终在 UI 层由 `LocaleResolver` 解析为 `{value}` 模板后才显示。因此严格看，badge 文本穿过了 locale resolver 一次，违背了 spec 「不经 locale」的字面规定。
- **偏差等级**: `MINOR`（实际显示结果与 spec 一致，但抽象边界上引入了不必要的 locale 依赖）。
- **量化**: 1 个 API 小调整 + `StatusHudRenderer` / `StatusIconResolver` 显示路径更新；下游使用 `RenderTextTokenSnapshot` 的 UI 组件需要接受一个 `literal` 变体。
- **修复建议**:
  - 方案 A（推荐）: 在 `RenderTextTokenSnapshot` 上提供 `literal(text: String)` 工厂（或复用已有字段），让 `rawBadge` 直接构造字面量 token，不再进入 locale 表；同时删除 `ui.status.badge.raw` 这个 locale key。
  - 方案 B（最小改动）: 保持现状，在规范 §4.1.4 追加脚注「允许以零参数 locale alias 形式中转，前提是 alias 内容只能是 `{value}`」。
- **选型建议**: 方案 A 更符合 spec 的设计意图；但若团队评估文档漂移成本更低，可取 B 并在 spec 正式版登记。

---

## 4 · 风险评估与合并策略

| 风险项 | 概率 | 影响 | 当前缓解 | 合并建议 |
| --- | --- | --- | --- | --- |
| `fromInspectSurface` 缺省不给 keyword chips → 真实对局中 inspect 体验 regression | `MED` | `MED` | 手工 caller 可补传；测试未覆盖默认路径 | PR04 合并前修复或显式 follow-ups 指派 |
| lint formal surface 覆盖不全 → 后续 PR 有再引入未知 keyword 的通路 | `MED` | `HIGH`（门禁失效） | 现有 WARN 已在 follow-ups 登记 | 合并前整改；或 PR 描述中显式声明 known-exception |
| locale key 未登记 → 后续 i18n 审计会误杀 | `LOW` | `LOW` | 已在 en/zh 两端齐备 | 随 spec 修订或 follow-ups 登记即可 |
| 手工白盒未执行 → 真实渲染/输入问题有逃逸风险 | `MED` | `MED`-`HIGH` | 自动化 + golden hash 已覆盖，但非像素级 | 由项目负责人书面决策：补跑 or 豁免 |
| badge 经 locale 通道 → 未来局部化改 `{value}` 模板会污染所有 badge | `LOW` | `MED` | 已有单测断言 badge 字面量；locale key 稳定 | 可延后到一次 presenter 统一重构里处理 |

### 4.1 合并推荐路径（按成本排序）

1. **最小修正路径（推荐）**: 合并前关闭 §3.1 + §3.2 + §3.4，后两者至少完成 PR 描述级豁免与 owner 指派。§3.3 / §3.5 登记到 `follow-ups.md`。
2. **严格路径**: 5 条偏差全部关闭后再合并——对 PR04 节奏偏重，但与 spec 出口条件完全一致。
3. **加速路径（最低合规底线）**: 只关闭 §3.1（功能性），其余全部进入 follow-ups；需 DEO 显式批准，不建议。

---

## 5 · 复核清单（供二审人使用）

- [ ] `./gradlew :client:test` focused selectors 列表是否覆盖 `ExplainPaneModelTest.fromInspectSurface` 新增断言
- [ ] `./gradlew keywordRegistryLint` 的 `WARN` 数（期望 0 或显式 acknowledged）
- [ ] `./gradlew clientSmoke goldenScreenshot verifyChanged` 报告最后一次执行时间是否在 HEAD 之后
- [ ] `docs/opt/ui-pr/manual-records/phase4-uiux-pr04-status-description.md` 结论是否已从 `NOT_RUN_BY_REQUEST` 升级为 `AUTOMATED_PASS_MANUAL_PASS`（或已登记豁免）
- [ ] `docs/opt/ui-pr/follow-ups.md` 是否引用本报告 §3 对应条目并指派 owner
- [ ] `game/src/main/resources/i18n/{zh-CN,en-US}.json` diff 与 spec §4.7 表逐行比对

---

## 6 · 修复补丁骨架建议（非必须）

以下是 §3.1 修复建议的最小代码骨架，仅供参考：

```kotlin
// DescriptionPresenter.kt
data class InspectSurfaceOutcome(
    val card: ModalCardModel,
    val keywordIds: List<String>,
)

fun inspectSurfaceOutcome(surface: InspectDescriptionSurface): InspectSurfaceOutcome {
    val card = inspectSurfaceCard(surface)
    val keywordIds = extractKeywordIds(card.detailLines)
    return InspectSurfaceOutcome(card, keywordIds)
}
```

```kotlin
// ExplainPaneModel.kt
fun fromInspectSurface(
    actor: ActorRenderSnapshot?,
    item: ItemRenderSnapshot?,
    prop: PropRenderSnapshot?,
    terrainOverride: TerrainOverrideRenderSnapshot?,
    overlay: OverlayRenderSnapshot?,
): ExplainPaneModel {
    val outcome = DescriptionPresenter.inspectSurfaceOutcome(
        InspectDescriptionSurface(actor, item, prop, terrainOverride, overlay),
    )
    return fromCard(card = outcome.card, keywordIds = outcome.keywordIds)
}
```

> 以上片段仅示意；实际 `extractKeywordIds` 可能要接入 `replaceKeywordMarkup` 的中间产物，避免重复解析。

---

## 附录 A · 审查对照表（规范 → 实现文件）

| spec 章节 | 主题 | 关键实现文件 |
| --- | --- | --- |
| §4.1 | Status presentation model | `client/.../ui/status/StatusPresentationModel.kt`、`StatusHudRenderer.kt`、`StatusIconResolver.kt` |
| §4.2 | Telegraph presentation | `client/.../telegraph/TelegraphPresentationModel.kt`、`TelegraphRenderer.kt` |
| §4.3 | Description presenter 六面 | `client/.../ui/talent/DescriptionPresenter.kt`、`TileRenderModel.kt`、`AsciiRenderModel.kt` |
| §4.4 | ExplainPane sub-view | `client/.../ui/inspect/ExplainPaneModel.kt`、`InputHandler.kt`、`ui/layout/ModalStack.kt` |
| §4.5 | keywordRegistryLint | `tools/.../lint/KeywordRegistryLintRule.kt`、`tools/build.gradle.kts`、根 `build.gradle.kts`、`tools/.../verification/VerificationTaskRegistry.kt` |
| §4.6 | Accessibility toggle | `client/.../ui/settings/AccessibilityToggle.kt`、`TelegraphRenderer.kt` |
| §4.7 | Locale keys | `game/src/main/resources/i18n/zh-CN.json`、`en-US.json` |
| §6 | 出口门禁 | `docs/opt/ui-pr/manual-records/phase4-uiux-pr04-status-description.md`、`docs/opt/ui-pr/follow-ups.md` |

## 附录 B · 推荐命令

```bash
# 关键字 formal surface lint（期望 WARN=0）
./gradlew keywordRegistryLint

# 聚焦测试
./gradlew :client:test \
  --tests "com.ktome.client.ui.status.StatusPresentationModelTest" \
  --tests "com.ktome.client.telegraph.TelegraphPresentationModelTest" \
  --tests "com.ktome.client.ui.inspect.ExplainPaneModelTest" \
  --tests "com.ktome.client.ui.settings.AccessibilityToggleTest" \
  --tests "com.ktome.client.ui.talent.DescriptionPresenterTest" \
  --tests "com.ktome.client.input.InputHandlerTest" \
  --tests "com.ktome.client.render.TileRendererCanvasTest"

# 出口 gate
./gradlew clientSmoke goldenScreenshot verifyChanged

# 手工白盒（需补跑）
./gradlew :client:run \
  -Dktome.validation.seed=20260412 \
  -Dktome.validation.variant=BOSS_VARIANT \
  -Dktome.locale=zh-CN \
  -Dktome.window.size=1280x800 \
  -Dktome.ui.a11y.highContrast=true \
  -Dktome.ui.a11y.colorBlindSafe=true \
  -Dktome.ui.a11y.reduceMotion=true
```

## 附录 C · 签收意见

| 角色 | 结论 | 备注 |
| --- | --- | --- |
| 开发设计总监 | `有条件通过` | §3.1 + §3.2 + §3.4 至少需要 PR 描述级闭环 |
| 系统策划总监 | `有条件通过` | §3.3 属文档漂移，可登记 follow-ups；§3.5 建议 spec 侧澄清 |
| 玩法体验审查负责人 | `有条件通过` | 手工白盒未跑（§3.4）是最大体验盲点，建议至少补一次 BOSS_VARIANT 路由 |

---

**报告版本**: v1.0
**下一次回归窗口**: 随 PR05 (`phase4-uiux-pr05-telegraph-and-combat-decision-surface`) 出口合并评审一起复查。
