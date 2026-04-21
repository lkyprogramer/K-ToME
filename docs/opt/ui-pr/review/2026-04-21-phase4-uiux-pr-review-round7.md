# Phase 4 UI/UX PR 文档深度 Review（第 7 轮）

**日期**: 2026-04-21
**范围**: `docs/opt/ui-pr/` 下 `PR-01 ~ PR-05`、`README.md`、`follow-ups.md`、`resource-fallback-audit-template.md`、`manual-records/_template.md` 以及上游源计划 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md` 的交叉引用。
**基线**: 本轮以 round6 review（`2026-04-21-phase4-uiux-pr-review-round6.md`）为对齐点，先确认残留条目闭合情况，再挖掘 round6 未列出的最小粒度问题。
**结论摘要**: 文档主干已高度稳定，本轮 **没有 P0 级阻塞**；P1 集中在 contract 歧义与状态机隐式假设，P2 集中在口径/措辞/清单遗漏，P3 为纯修辞。建议按 §7 排期。

---

## 0. Round6 残留条目闭合确认

| round6 编号 | 条目 | 当前状态 | 证据 |
| --- | --- | --- | --- |
| P2-1 | 源计划旧前缀 `phase4-uiux-pr06/07/08` 残留 | ✅ 已闭合 | 源计划 `:14` 声明"保留旧 8 PR 设计语义，执行编号以 `docs/opt/ui-pr/` 为准"；`:179-186` 给出合并映射；`:489-496` 概览表双列新旧编号；旧 PR-06/07/08 各章内部截图 / plan 路径已统一为 `phase4-uiux-pr04-*` / `phase4-uiux-pr05-*` |
| P2-2 | PR-05 §4.2 仍引用 `legalTargetCountPreview` | ✅ 已闭合 | PR-05 `:130-131` 改为 `legalTargetSummary.count / missingReason`，与 §4.3 模型字段对齐 |
| P3-1 | special 双字段反向非法态未 fail fast | ✅ 已闭合 | PR-03 §4.1 映射表新增两行反向 fail fast；§4.6 阻塞规则 6 引入双向不变量；§8 出口门禁 `:500` 加 `specialTemplateWithoutTierFails / specialTierWithoutTemplateFails` |
| P3-2 | fallback high-risk 例外偏宽 | ✅ 已闭合 | `resource-fallback-audit-template.md` 拆成 rule 5（high 且依赖新 key 禁开放）/ rule 6（复用既有 key 方可临时开放） |
| P3-3 | "owner-defined" 偏宽 | ✅ 已闭合 | PR-02 §4.3 delegation contract 三条将 SHOP/WORLD_MAP/STAT_ASSIGN/VALIDATION/TARGETING 的 owner 边界具体化 |

round6 所列问题已全部闭合。下列为本轮**新**发现的细粒度问题，按 PR 分章。

---

## 1. PR-01 Client Foundation and Main Menu

### 1.1 `ui.menu.continue.unavailable.unknown` 示例文案语义弱（P1）

- §4.7 `:281` 定义该 key 示例文本为 `存档暂时不可用。`。
- 其余四条 disabled reason（`corrupted / version-mismatch / io-error / schema-mismatch`）都清楚描述了具体故障原因；唯独 `unknown` 用"暂时不可用"，容易被玩家误解为"稍后重试即可"，不体现"未分类异常，需要复制错误详情上报"的语义。
- 建议示例文本至少包含一次引导："无法识别的存档问题，请复制错误详情上报。"与 §4.5 追加 `throwableClass / throwableMessage` 形成闭环。

### 1.2 `1024x768` 最小窗口证据链不完整（P2）

- §4.2 `:153` 声明最小支持 `1024x768`；§6.1 `:333` 把"`1024x768` 最小窗口下首页三态和局内 MapDominant 不重叠、不爆版"列为必测行为；§6.3 `:395-398` 人工复核有 1024x768 步骤。
- 但 §6.2 自动化命令未指定 `InfoSurfaceLayoutTest` 或 `TileRendererCanvasTest` 必须覆盖 1024x768 分辨率 case，且 golden harness 固定在 1280x800（§6.3 `:363`）。
- 建议 §6.1 或 §6.2 追加一条硬要求：`InfoSurfaceLayoutTest` / `TileRendererCanvasTest` 至少覆盖 `1024x768 / 1280x800` 两个断点；否则该最小窗口声明仅靠人工截图支撑。

### 1.3 Copy Error Detail payload `UNKNOWN` 追加字段缺剪裁规则（P1）

- §4.5 `:244-247` 约定 `reasonCode = UNKNOWN` 时末尾追加 `throwableClass / throwableMessage`。
- 未声明 `throwableMessage` 是否允许包含 stacktrace、文件绝对路径、用户名或其他敏感信息。实际 Java 异常 message 可能把 save 路径（含 OS 用户名）原样带出。
- 建议显式规则："`throwableMessage` 必须经 pure formatter 截断（例如 ≤ 200 字符），禁止原样输出 stacktrace；`throwableClass` 使用完整 FQCN 以便定位。"并在 `UiErrorPayloadTest` 增加截断断言。

### 1.4 `GameApp` 未列入 §3.1 修改面（P2）

- §4.6 规则 6 `:268` 将窗口 title setter 消费点明确钉在 `GameApp.render()` 入口或现有 UI 主线程边界。
- §3.1 修改清单（`:69-81`）未列 `client/.../GameApp.kt`；若实施时发现 `GameApp` 需要接入 formatter / BuildInfo，会导致 scope 外扩。
- 建议 §3.1 显式加入 `GameApp.kt`（修改面），或在 §4.6 规则 6 注明"仅写入 formatter，调用点由既有 `FoundationGameScreen` / `GameApp` 承接，不在本 PR 新增 render loop hook"。

### 1.5 `DesktopLauncher` 与 `GameApp` 标题写入职责未切清（P2）

- §5.4 `:319-321` 要求 `DesktopLauncher.setTitle(...)` 删除操作说明、标题更新只调用 `DesktopLauncherTitleFormatter`；§4.6 规则 6 又要求 title 切换由 `GameApp` 在 render 边界消费。
- 两处表述不一致会被误读为 `DesktopLauncher` 仍持有 `setTitle` 调用链。建议统一："启动时 `DesktopLauncher` 调用一次初始 title（可为 `K-ToME`）；运行期切换全部由 `GameApp` 的 UI 主线程边界完成。"

---

## 2. PR-02 In-Game Info, Input, Modal, Look

### 2.1 `STAT_ASSIGN` truth table 行为矩阵覆盖不全（P1）

- §4.3 truth table 第 8 行 `:161` 只列 `1-4 / Ctrl+S`。
- 行为矩阵 `:174` 把 `SHOP / WORLD_MAP / STAT_ASSIGN` 一条合并为"owner keys"，但 `STAT_ASSIGN` 被动态没说明 `ESC / Backspace / Tab / I / L / X` 在 owner 未 claim 时是走 no-op 还是走 delegation 吞噬。
- 实施时若 owner 未处理 `ESC`，client 会退回 MAP 导致 stat allocation 丢失；没 contract 约束极易漏测。
- 建议行为矩阵拆分 `STAT_ASSIGN` 单独一行，至少明确"owner 必须消费 `1-4 / Ctrl+S`；其余键 delegation owner fallthrough 至 client-local no-op 或 delegation 显式吞噬，二选一并写入测试"。

### 2.2 truth table / 行为矩阵两处 SHOP 口径冲突（P2）

- §4.3 truth table `:159` 明确 `SHOP: I close / X no-op / Ctrl+S`——`I` 关闭是 client-local 行为。
- 行为矩阵 `:174` 把 `SHOP` 的 `I` 吸进 "owner keys" 归类，但定义"owner keys" 为"delegated passive owner"；`I` 的闭合语义到底谁负责不清。
- 建议在 delegation contract 第 1 条明确："`SHOP` 的 `I` 是 client-local close shortcut，不透传给 shop owner；`SHOP` 的确认 / 购买 / 取消由 owner 处理。"

### 2.3 `VALIDATION` 行 debug log 为 "optional"（P3）

- 行为矩阵 `:176` 对 `VALIDATION` 的 `I / J / K / L` 标 `debug log optional`。
- "optional" 等价于"可以不实现"，测试无法断言；建议改为"debug log 允许但无 toast"或"不产生 debug log"，固化一种形态。

### 2.4 `TARGETING` forward-compatible note 未界定 delete 时机（P2）

- §4.3 末尾 `:194` 的 note 声明"删除条件是旧直接 targeting 入口完全消失并由 PR-05 的 `CombatDecisionFrameTest` 覆盖"。
- PR-05 §8 出口门禁第 8 条只要求"旧 `InputHandlerTest.TARGETING` 用例保留但 re-target 到 frame phase 断言"，并未强制 PR-05 删除 `UiMode.TARGETING` 行。两处口径存在轻微错位：PR-02 说"行删除"，PR-05 说"re-target 保留"。
- 建议统一："`UiMode.TARGETING` 保留为内部 cursor 兼容壳；truth table 行不删除，但断言迁移到 CombatDecisionFrame。"

### 2.5 `?` no-op + debug log 的 log level / format 未定义（P2）

- §4.4 规则 5 `:204` 把 PR-02 阶段 `?` 定义为 "no-op + debug log，不产生 renderer 可见变化"。
- 没声明 log level（`DEBUG / INFO`）、log message key 或测试断言点。`InputHandlerTest` 若要 assert 这条路径，缺少抓钩。
- 建议固化："`debug log` 使用 `DEBUG` level，message 为稳定英文字面量（例如 `inspect.explain-stub.invoked`），并通过 `TestLogCollector` 断言。"

### 2.6 `ModalStack.push` 超深度 fail fast 的 client 入口防御未列（P2）

- §4.1 规则 1 `:119` 要求 push 超深度抛 `IllegalStateException`，"client 入口必须保证合法 push"。
- §4.1 规则 4 `:122` 又要求 deferred frame "真实占用一层 stack 深度"，则 A→B→COMBAT_DECISION(stub) 已到深度 3，后续若用户再触发 `I` 打开 INVENTORY，client 入口应该预判并 toast / no-op。
- 文档未列 client 入口防御点（在 `InputHandler` 的 `pollCommand` 前置检查？还是在 `ModalStack.push` 之前）。建议 §4.1 追加一条"client 入口在触发 push 前必须查询 `modalStack.depth`，并在超限时产出 `ui.message.modal.stack-overflow` toast 或等价 debug log，而非让异常冒泡到 render loop"。

### 2.7 首页 help 文本与 truth table 键集合未声明一致（P2）

- §6.1 必测行为第 7 条 `:264` 要求 `ui.menu.help.primary-keys` 与 truth table 一致。
- PR-01 §4.7 示例文本 `"方向键移动，Enter 确认，? 查看帮助。"` 只覆盖 3 个键；truth table 新增了 `ESC / Backspace / Tab / I / L / X / Ctrl+S` 等。
- 建议 §6.1 明确"help 文本不必逐键枚举，但必须显式涵盖 `ESC / Backspace / I / L / ? / Ctrl+S` 六个主键位的至少引用"，或在 PR-02 直接改写 PR-01 的示例文案。

---

## 3. PR-03 Item, Content Presentation, UI States

### 3.1 软断言升级 owner / 判定阈值含糊（P2）

- §4.5 `:273` 定义 "若后续连续 3 次 CI 记录超过 `500ms`，再单独把该检查升级为 hard fail"。
- 未定义：谁 own 这 3 次统计（CI 还是某个 owner manifest）、记录保留位置 / 保留时长、"连续"的时间窗口、升级动作由哪个 PR / follow-up 执行。
- 建议显式 owner："统计写入 `build/reports/client-smoke/loading-timing.jsonl`；由 PR-03 出口门禁登记 follow-up '若 T+30 天内 CI 连续 3 build 超 500ms，打开 hard-fail PR'。"

### 3.2 `ModalCardModel.title / primaryAction` 阻塞规则"empty token"未定义（P1）

- §4.6 阻塞规则 3 `:285` 使用了"`empty token`"和"有效返回路径"两个未定义词。
- "empty token" 可能指 `RenderTextTokenSnapshot.key == ""`、`key == null`、或 `key` 存在但未 resolve。不同解释会导致 lint 漏判或误判。
- "有效返回路径"缺形式化断言；建议改为：`primaryAction ∉ {Close, Cancel} ∨ secondaryAction ∉ {Close, Cancel}`；`title.key` 必须是非空字符串且存在于 `locale`。

### 3.3 `Enter` 在同卡多动作下的触发优先级未定义（P1）

- §4.3 action 语义表（`:209-218`）对 `Buy / Sell / EnterRoute / Confirm` 都把 `Enter / Space` 列为触发键，`ReadMore` 也标 `? / Enter`。
- 当一张 `shop offer` 卡同时携带 `primary=Buy` 与 `secondary=ReadMore` 时，`Enter` 是触发 Buy 还是 ReadMore 没有规则。
- 建议显式"主键 `Enter / Space` 始终优先触发 `primaryAction`；`ReadMore` 仅在 primary 不消费 `Enter` 的只读卡片上有效，否则只能由 `?` 触发"。

### 3.4 `Cancel` 与 `Close` 触发键重合（P2）

- §4.3 action 语义表 `Cancel` 和 `Close` 都映射到 `ESC / Backspace`，且副作用描述互相覆盖（"取消当前流程" vs "关闭当前卡片"）。
- secondaryAction 允许同时出现 Cancel / Close 会歧义。建议约束"单张卡片 secondaryAction 不得同时为 Cancel 和 Close，二者互斥"，并在 `ModalCardModelTest` 增加冲突 case。

### 3.5 `UiLoadingState.allowsCancel` 与 cancel action 的绑定仅在文字层（P2）

- §4.5 `:275` 要求 `allowsCancel == true` 必须配套 `ModalCardAction.Cancel` 与 locale key `ui.loading.cancel`。
- `UiLoadingState` shape 没有 `cancelAction: ModalCardAction?` 字段来承载该约束；绑定只靠实现者自觉或 lint。
- 建议在 shape 中显式追加 `cancelAction: ModalCardAction? = null` 并加不变量："`allowsCancel == true` ⇔ `cancelAction == Cancel`"，由 `UiLoadingStateTest` 覆盖。

### 3.6 §6.2 `:core:test` 与子 selector 重复运行（P2）

- §6.2 `:357-358` 先 `./gradlew :core:test` 再 `./gradlew :core:test --tests "...RenderSnapshotSerializationTest" --tests "...RenderSnapshotHasherTest"`。
- 后者是前者真子集。本 PR 已明确触及 `core/snapshot/RenderSnapshot.kt`，应保留全量 `:core:test` 一次，删除重复 selector，或改为"若只想快速 iteration 可只跑 selector，提交前必须 `:core:test` 全量"。

### 3.7 `contextKeyValuePairs` 容器类型未定义（P2）

- §4.4 `:235` 约定 "`contextKeyValuePairs`: 英文 debug key + 稳定值，按 builder 插入顺序输出"。
- shape 未列出容器类型。如果是 `Map<String, String>`（普通 HashMap）则 JVM 版本间顺序漂移；必须是 `LinkedHashMap` 或 `List<Pair<String,String>>`。
- 建议显式："`contextKeyValuePairs: List<Pair<String, String>>`，builder 插入顺序即输出顺序，`UiErrorPayloadTest` 用固定插入序断言。"

### 3.8 core systems design doc 修订定位缺章节引用（P2）

- §3.1 `:79-80` 把 `docs/2026-03-13-core-systems-design-and-phase-supplements.md` 和 `docs/phase4/2026-03-13-phase4-verification-checklist.md` 列入修改面，但 §4.1 仅泛泛说"同步 snapshot 构建、contract lint、render snapshot tests"。
- 缺对这两份上游文档的具体章节 anchor（哪一节记录 `specialTierId` contract 变更）。执行者极易漏改上游文档从而破坏"snapshot 合同 single source of truth"。
- 建议 §4.1 末尾追加一条"上游文档修订定位：core systems design doc 的 `snapshot-contract` 节 / phase4 checklist 的 `owner-gate` 节"。

---

## 4. PR-04 Status, Description, Readability

### 4.1 `TelegraphPresentationModel.toStatusPresentation()` 投影规则不完整（P1）

- §4.1 `:160` 约定 compact shape 仅含 `typeId / nameKey / iconKey / dangerLevel / previewTurnsRemaining / badge`，但 `StatusPresentationModel` 完整字段包含 `group / category / priority`。
- 投影时：
  - `group` 必然为 `StatusPresentationGroup.TELEGRAPH`，但未显式写明。
  - `category` 属于 `StatusEffectCategorySnapshot`（BUFF/DEBUFF/NEUTRAL）；telegraph 不在此枚举，投影值未定义，违反 non-null 假设。
  - `priority` 由 `§4.1` 公式算，需在投影函数内调用 builder。
- 建议补一条"投影规则：`group = TELEGRAPH`、`category = NEUTRAL`（或引入 `StatusEffectCategorySnapshot.TELEGRAPH_STUB` 合同）、`priority = StatusPresentationBuilder.telegraphPriority(dangerLevel, previewTurnsRemaining)`"。

### 4.2 TELEGRAPH 与 ZONE_EFFECT danger 权重系数不一致需注释（P3）

- §4.1 priority matrix：`TELEGRAPH = 900 + dangerLevel*20 + previewTurnsInverse`，`ZONE_EFFECT = 650 + dangerWeight`（`dangerWeight = dangerLevel*10`）。
- 系数 `*20` vs `*10` 是故意（telegraph 比 zone 更紧迫），但当前公式 `:151-155` 把 `dangerWeight` 放在 `ZONE_EFFECT` 行后，读者首见会以为 `*20` 是笔误。
- 建议在公式下加一行注：`TELEGRAPH 用 dangerLevel*20 是刻意放大；两者 base gap (900 vs 650) 已将群组分层，系数差额进一步放大 LETHAL 情境下的排序强度`。

### 4.3 `previewTurnsRemaining` 缺失时 `previewTurnsInverse=5` 反而最高优（P1）

- §4.1 `:152` 定义 `previewTurnsInverse = max(0, 5 - previewTurnsRemaining)`；"`previewTurnsRemaining` 缺失时取 `0`"。
- 结果：缺字段 → `previewTurnsInverse = 5`（最高），优先级比 `previewTurnsRemaining = 0` 的 LETHAL telegraph（也是 5）并列最顶，比 `previewTurnsRemaining = 2` 的真实 LETHAL（inverse=3）还高。
- 缺字段本应触发 `missingFactReason` 降级显示，而非把 unknown 压到最顶抢眼。
- 建议改为"`previewTurnsRemaining` 缺失时 `previewTurnsInverse = 0`，并由 `TelegraphPresentationModelTest` 覆盖"。

### 4.4 `AccessibilityToggle` "运行期切换如果存在"含糊（P2）

- §4.6 规则 3 `:256` 原文："运行期切换如果存在，允许下一次 `render()` 生效；本 PR 不要求全局 invalidate。"
- "如果存在" 允许实现者在本 PR 做 hotkey，也允许完全不做；读测试时无从判断正向/负向断言。
- 建议改为："本 PR 不实现 runtime hotkey；仅允许通过 `-Dktome.ui.a11y.*` 启动时读取。若后续 PR 引入 hotkey，生效时机为下一次 `render()`。"

### 4.5 keywordRegistryLint WARN 分支流程未绑定出口门禁（P2）

- §4.5 lint 实现策略第 6 条 `:239` 要求若走 WARN 放行必须在 `follow-ups.md` 写条目。
- `follow-ups.md` 当前为空模板（只有"当前无 deferred follow-up"声明）。§8 出口门禁没把"`follow-ups.md` 若触发需同步"列为必检项。
- 建议 §8 追加一条："若 `keywordRegistryLint` 采用退化策略且触发历史面 WARN，必须在合入前登记到 `follow-ups.md` 并由 `ktome-diff-doc-review` 核对。"

### 4.6 locale key 清单未声明对 PR-03 的复用（P2）

- §4.7 locale 表未包含 `ui.loading.cancel / ui.empty.inspect.title / ui.empty.inspect.detail` 等由 PR-03 引入、被 ExplainPane / ModalCardModel 复用的 key。
- PR-04 若独立 rebase / cherry-pick，可能漏掉这些依赖 key。
- 建议在 §4.7 表下加一段："以下 key 复用自 PR-03，不在本 PR 新增但本 PR 消费：`ui.empty.inspect.title / detail`、`ui.loading.cancel`、`ui.error.action.*`。PR-04 独立合并时必须先合入 PR-03 的 locale 条目。"

### 4.7 §6.2 第二行 focused tests 漏跑 `DescriptionPresenterTest`（P2）

- §6.2 `:318-319` 第一行选了 `DescriptionPresenterTest`；第二行选 `StatusPresentationModelTest / TelegraphPresentationModelTest / ExplainPaneModelTest / AccessibilityToggleTest`。
- 若本 PR 新增的"item/shop/inspect/action 分支用例"落在独立测试类（如 `DescriptionPresenterCombatActionTest`），第二行未引用；若落在既有 `DescriptionPresenterTest`，第一行已覆盖但第二行重复执行也不影响。
- 建议 §3.1 明确"combat action 分支用例归入 `DescriptionPresenterTest` 或独立新文件"，然后在 §6.2 对应选中。

### 4.8 lint 规则 2 "或正式说明引用" 口径含糊（P2）

- §4.5 规则 2 `:227-228` "`KeywordRegistry` 定义的核心 keyword 至少被一个 `DescriptionModel` 或正式说明引用"。
- "正式说明"未枚举：是否包含 inscription / combat action description / locale 里直接使用的 keyword key？如果仅接受 `DescriptionModel`，则 talent inscription 直接 localize 的 keyword 会被误判孤儿。
- 建议列白名单："`DescriptionModel / ExplainPaneModel / StatusPresentationModel.nameKey 引用 / KeywordRegistry.CORE.aliases` 四类之一"。

---

## 5. PR-05 Telegraph and Combat Decision Surface

### 5.1 `TARGET` phase `Backspace` 回退路径依赖隐式状态（P1）

- §4.2 `:157-158` "`Backspace` -> 若来自单一方式跳过路径则 ACTION，否则 METHOD"。
- 判定需要 "是否跳过 METHOD" 这一历史事实；当前状态机显式字段只有 phase，没有 `skippedMethod: Boolean` 或等价标记。若仅运行时再查 `action.methodOptions.size`，存在两个风险：
  1. action 在 phase 进行中如果 legal options 动态变化（例如 target 变化影响方式可用性），`size == 1` 从 true 变 false 会让回退目标漂移。
  2. 单元测试很难固定该路径的 seed / state。
- 建议显式增加 `CombatDecisionFrame.skippedMethod: Boolean`，由 ACTION→TARGET 直跳路径置 true，`Backspace` 语义 `if (skippedMethod) ACTION else METHOD`。`CombatDecisionFrameTest` 必须覆盖这两种路径。

### 5.2 TARGET phase `legalTargets.isEmpty()` 进入条件自相矛盾（P1）

- §4.2 `:130` 在 ACTION phase 处理 `legalTargetSummary.count == 0` 时写"disabled 并保持 ACTION"；意味着 `legalTargets.isEmpty()` 的 action 不会进 TARGET。
- §4.2 `:154` 又在 TARGET phase 声明"`若 legalTargets.isEmpty() -> 拒绝提交 + toast ui.message.combat.no-legal-target`"——等价于承认 TARGET 可能以 empty legal 进入。
- 两条规则冲突。要么 ACTION phase 强制 short-circuit，TARGET phase 不需再判 empty；要么 ACTION 允许进入，应说明进入条件（例如"单一方式 + 进入方式后才发现 target empty"）。
- 建议只保留 ACTION phase 的 fail-fast，TARGET phase 删除该分支；或明确"ACTION→METHOD→TARGET 路径下 METHOD 切换可能导致 legal target 变空，此时 TARGET phase 保守提示并要求 Backspace 手动回 METHOD"。

### 5.3 数字键 `0` 行为未定义（P2）

- §4.2 多处写 "若 N > action list size 或 N > 9 -> no-op"，未定义 N=0。
- 通常游戏数字键 `0` 映射到第 10 个条目或 no-op；文档缺约束会导致 `InputHandlerTest` 覆盖漏洞。
- 建议统一："`0` 键映射到第 10 个条目（若存在），否则 no-op；三个 phase 一致。"

### 5.4 `missingFactReason` 与 `legalTargetSummary.missingReason` 优先级未定义（P1）

- §4.3 模型同时有 `ActionHintModel.missingFactReason` 和 `ActionHintModel.legalTargetSummary.missingReason`。
- §4.2 `:131` 说"展示 `missingFactReason / missingReason`"并列；若两者同时非空，UI 应优先显示哪条、是否合并展示，没有规则。
- 建议："`legalTargetSummary.missingReason` 作为特定字段的 inline 提示（靠近 target 合法目标区域显示）；`missingFactReason` 作为 action-level summary 显示在动作卡底部；两者允许同时存在，但 renderer 必须去重（若内容相同，只显示一处）。"由 `ActionHintModelBuilderTest` 覆盖。

### 5.5 CombatDecisionFrame 与 PaneFocusController 交互未声明（P1）

- §3.1 新增面未包含 `PaneFocusController`；PR-02 §4.2 规则 3 已规定 "modal 打开后暂停地图锚点"。
- `CombatDecisionFrame` 通过 `ModalStack.push(COMBAT_DECISION)` 成为 modal，按 PR-02 规则应暂停地图锚点；但 CombatDecisionFrame 内 `Tab / Shift+Tab` 的语义在 ACTION/METHOD/TARGET 三 phase 各不相同（§4.2 表格），与 PR-02 "modal 内只循环当前 frame 焦点"一致但未显式引用。
- 建议 §4.2 或 §4.4 追加一条："`CombatDecisionFrame` 遵循 PR-02 modal 焦点约束；push 时暂停 `PaneFocusController`，pop 时恢复打开前锚点。三 phase 内 `Tab` 焦点范围分别为 action list / method list / legal target list。"

### 5.6 §5.4 测试清单缺 `TelegraphPresentationModelTest` 的三位一体覆盖（P2）

- §5.4 `:276-279` 列出了 5 组 combat decision / AI intent 相关测试，但未包含 `TelegraphPresentationModelTest` 的 append 字段 case。
- PR-04 只 freeze compact shape；PR-05 append 完整字段（overlay cells、shape、warning text、log prefix）后，必须有测试证明三处 consumer（地图 overlay / 目标卡 / 日志）走同一 builder。
- 建议 §3.1 测试清单加 `client/src/test/kotlin/com/ktome/client/telegraph/TelegraphPresentationModelTest.kt`（扩展既有），§5.4 追加"三位一体 single-source builder 测试"。

### 5.7 §6.1 必测第 7 条未引用 lint 规则（P2）

- §6.1 第 7 条 `:300` 只写"普通敌人 intent 没有被伪造"。
- §5.4 `:279` 已定义 `AiIntentLeakRuleTest` 覆盖 locale keyword + `aiTypeId` 消费点两维；§6.3 步骤 6 也只描述人工 grep。
- 建议 §6.1 第 7 条补"具体断言由 `AiIntentLeakRuleTest` 承载（locale 预测前缀 + `aiTypeId` 消费分类）"，避免人工白盒漏扫 code path。

### 5.8 §6.1 必测第 8 条缺 `rangeSummary`（P2）

- §6.1 第 8 条 `:301` 列出"resource cost / cooldown / legal target count / telegraph linkage 四类 typed fact"缺失场景。
- §4.3 `ActionHintModel` shape 含 5 类 optional：`resourceCosts / cooldownTurns / rangeSummary / legalTargetSummary / telegraphLinkage / disabledReason`；`rangeSummary` 未列入。
- 建议第 8 条加一句："`rangeSummary` 缺失时同样走 `missingFactReason`；不得显示 'range: 0' 或空字段。"`ActionHintModelBuilderTest` 增加该 case。

### 5.9 §8 第 7 条 stub label 清理方式未显式引用 README（P3）

- §8 `:452` 写 "停止任何 `PR-02` 遗留的 `combat-decision-stub` golden label，并重录为 `phase4-uiux-pr05-*`"。
- README 跨 PR deferred 表 `:50` 已定义"收口 PR 按前缀删除或重录"。两处文字口径等价但措辞略异。
- 建议 §8 追加一句："清理方式遵循 README 跨 PR Deferred 表第 `COMBAT_DECISION` 行。"避免后续独立修订漂移。

### 5.10 §7.3 约束未重申 high-risk 不开放新玩家路径（P2）

- §7.3 约束 3 `:442` 只写"未生成正式资源时，必须保留 Resource Fallback Audit"。
- `resource-fallback-audit-template.md` rule 5 明确"`失效风险等级=high` 且 UI 路径依赖新 key 时，不得开放正式玩家路径"。combat decision 入口本身就是"新玩家路径"；若本 PR 计划 defer `telegraph 升级预警 audio cue`，必须显式 high-risk 判定。
- 建议 §7.3 加一条："若使用 fallback，必须填写 Resource Fallback Audit 完整风险等级；若风险等级 `high` 且依赖新 key 启用 combat decision 新路径，出口门禁阻塞，不得开放玩家路径。"

---

## 6. 跨 PR / README / follow-ups / templates

### 6.1 README UI root gate 与 PR-close gate 合并命令口径不一致（P1）

- README 执行纪律第 10 条 `:32` 要求 root 出口 `./gradlew clientSmoke goldenScreenshot verifyChanged`（三 task 并列单次调用）。
- README "每 PR 必跑命令总览" `:38-42` 把 `clientSmoke goldenScreenshot` 归为"UI root gate"，`verifyChanged` 归为"PR-close gate"，暗示可分阶段执行。
- 两处阶段划分不一致：执行者先跑 UI gate 通过后，若 verifyChanged 把改动范围扩大到 `:core:test` 未跑，会在最后一刻补跑造成 rework。
- 建议统一："开发内循环自由选择；**PR close 前必须一次性运行 `./gradlew clientSmoke goldenScreenshot verifyChanged`**；若 CI 把三者拆阶段，文档不区分。"

### 6.2 README deferred 表 ExplainPane 未提 debug log 约定（P2）

- README `:51` 对 `ExplainPane sub-view` "临时形态"写"`INSPECT + ?` 只预留 sub-view 入口；未打开时 `Backspace` 仍按 `PR-02` 回退"。
- PR-02 §4.4 规则 5 要求 `?` no-op + debug log。README 未提及 debug log，导致跨文档不统一（见本报告 §2.5）。
- 建议 README 临时形态栏加"`?` 触发 debug log，不显示可见变化"。

### 6.3 resource-fallback-audit-template "既有 key" 判定来源未定义（P2）

- `resource-fallback-audit-template.md` rule 6 "只有 fallback 复用既有 key，且不依赖新 key 启用新 UI 路径时，才允许在同 PR 证据齐全后临时开放"。
- 未定义"既有 key"来源：`visual-manifest.json` / `audio-manifest.json` / `KeywordRegistry.CORE` / `locale` 四个 registry 各管一类。
- 建议显式："`既有 key` 指 PR 启动 commit 之前，已存在于对应面 registry（图片 `visual-manifest.json` / 音频 `audio-manifest.json` / locale `zh-CN.json & en-US.json` / 关键词 `KeywordRegistry.CORE`）的 key。"

### 6.4 manual-records/_template.md 双人签收缺时间戳（P2）

- §7 表格只列 `角色 / 姓名 / 结论 / 备注`。header 日期字段 `:6` 只有一个"日期"。
- 记录人和复核人若不同日签收，时间戳缺失导致无法审计。
- 建议 §7 表增加"时间戳"列，或 header 日期字段改为"记录日期 / 复核日期"两行。

### 6.5 follow-ups.md 缺"触发时同步规则"（P2）

- `follow-ups.md` 只有空模板 + 一句"当前无 deferred follow-up"。
- 当前流程中 PR-04 `keywordRegistryLint` WARN 分支（§4.5 规则 6）明确指向本文件，但文件头部没声明"若触发必须同步的流程"。
- 建议文件顶部增加 "同步规则：任一 PR 执行中若命中 WARN 降级 / stub deferred / lint 覆盖不足等分支，必须在合入前追加条目；`当前无 deferred follow-up` 声明应在追加条目的 commit 中同步删除。"

### 6.6 源计划 PR 概览表双编号容易误读（P3）

- 源计划 `:14` 声明保留旧 8 PR 语义，执行以 ui-pr 目录为准。
- `:489-496` 的"PR 概览"表同时保留 `PR-01~PR-08` 行和 `current phase4-uiux-pr0X` 映射列。
- 读者若直接定位到 `:489` 会误以为仍是 8 PR 执行；`:14` 的免责声明位置距离较远。
- 建议在 `:488` 表标题前补一行 reminder："**执行不采用此 8 PR 拆分；以下表仅保留旧设计语义对照，实际执行按 `docs/opt/ui-pr/README.md` 的 5 PR。**"

---

## 7. 优先级分级

| 级别 | 条目 | 说明 |
| --- | --- | --- |
| **P0** | 无 | 无阻塞缺陷 |
| **P1** | §1.1, §1.3, §2.1, §3.2, §3.3, §4.1, §4.3, §5.1, §5.2, §5.4, §5.5, §6.1 | contract 歧义、状态机隐式假设、排序不稳定、入口语义冲突 |
| **P2** | §1.2, §1.4, §1.5, §2.2, §2.4, §2.5, §2.6, §2.7, §3.1, §3.4, §3.5, §3.6, §3.7, §3.8, §4.4, §4.5, §4.6, §4.7, §4.8, §5.3, §5.6, §5.7, §5.8, §5.10, §6.2, §6.3, §6.4, §6.5 | 口径未定义、清单遗漏、流程未绑定、文档交叉引用 |
| **P3** | §2.3, §4.2, §5.9, §6.6 | 纯措辞 / 注释级 |

---

## 8. 建议的最小修订动作

按成本排序，从便宜到贵：

1. **纯文字修订（P3 + 部分 P2 口径）**: §4.2（加注释）、§6.6（reminder 行）、§2.3（optional → 固化）、§4.4（改"如果存在"为"本 PR 不实现"）、§4.6（PR-04 locale 复用声明）、§5.9（引用 README）——合计 ~ 10 处一行修改。
2. **清单补完（P2）**: §1.4（GameApp 加入 §3.1）、§5.6（加 TelegraphPresentationModelTest）、§4.7（focused test 对齐）、§3.8（上游文档章节 anchor）。
3. **规则具体化（P1）**: §3.2（empty token 定义）、§3.3（Enter 优先级）、§4.1（投影规则）、§4.3（缺字段 inverse=0）、§5.1（skippedMethod 字段）、§5.2（TARGET empty 边界）、§5.4（missingReason 优先级）、§5.5（PaneFocusController 约束）、§6.1（root gate 合并命令）。
4. **模板 / 流程调整（P2）**: `follow-ups.md` 顶部同步规则、`manual-records/_template.md` 双签收时间戳、`resource-fallback-audit-template.md` "既有 key"来源。

以上 P1 条目若全部收敛，后续 PR 实施阶段出现 contract 返工或测试漏判的概率显著下降。P2 建议在各 PR 开工前一次性修订；P3 可随 PR commit 顺手改。

## 9. Round7 结论

1. Round6 提出的 5 条残留（P2×2 + P3×3）本轮核查全部闭合。
2. Round7 发现 48 条新细粒度问题（P1 × 12 / P2 × 32 / P3 × 4），覆盖所有 5 份 PR、README、follow-ups、fallback 模板、manual-records 模板和源计划的交叉引用。
3. 核心薄弱点集中在"状态机隐式假设"（PR-05 §4.2）、"contract 可选字段优先级"（PR-04 §4.1 / PR-05 §4.3）、"阻塞规则未形式化"（PR-03 §4.6）、"跨文档口径不一致"（README vs 执行纪律、PR-02 vs PR-04 vs PR-05 对 TARGETING 退役时机的描述）。
4. 无 P0 阻塞；建议按 §8 从上到下一次性收敛 P1，再在各 PR 开工前随手过掉 P2 / P3。
