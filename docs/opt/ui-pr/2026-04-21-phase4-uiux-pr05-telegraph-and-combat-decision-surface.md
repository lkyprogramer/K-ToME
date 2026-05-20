> 执行前必须先完整阅读并接受：
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md`
> `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
> `docs/opt/ui-pr/README.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md`

# Phase 4 UI/UX - PR-05 Telegraph and Combat Decision Surface

**阶段**: `Phase 4 late-development / phase4-uiux-pr05`  
**优先级**: `P0`  
**合并来源**: 原计划 `PR-08 telegraph 三位一体与战斗三层决策面`  
**前置条件**: `PR-02` ModalStack/输入语义已冻结；`PR-04` 状态/说明/telegraph 可读性基础已落地。  
**硬依赖条款**:

1. `PR-02` 的 `COMBAT_DECISION` deferred frame 必须被本 PR 替换为真实 `CombatDecisionFrame`，并清理 stub golden label。
2. `PR-04` 已冻结最小 `TelegraphPresentationModel` 与 `StatusPresentationModel[group=TELEGRAPH]` compact 投影；本 PR 只能 append 完整三位一体字段，不得改既有字段语义。
3. `PR-04` 的 `DescriptionPresenter.combat action` pure unit 分支必须在本 PR 通过 `CombatDecisionPanel` 完成端到端消费。
4. `ValidationPreset.BOSS_VARIANT` 当前已存在于 `com.ktome.game.validation.ValidationPreset` 枚举合同中，本 PR 复用 `seed=20260412`，不新建第二 preset。

**对应问题**: 当前战斗仍偏 `TARGETING` 输入状态，Boss/scripted telegraph 的地图、目标卡、日志表现没有形成三位一体。玩家需要明确看到“当前有什么风险、我在选哪一层动作、下一步确认会发生什么”。

---

## 1. 阶段目标

完成 telegraph 三位一体和 map-first 战斗决策面。

完成标准：

1. Boss/scripted telegraph 在地图 overlay、目标卡、日志前缀上同 icon、同主色、同 danger 语义。
2. 新增单一 `CombatDecisionFrame` 内部状态，phase 为 `ACTION -> METHOD -> TARGET`；该 frame 只作为输入/state owner，不在 `TARGET` 阶段绘制阻塞式居中 modal。
3. `ESC / Backspace / Tab / Shift+Tab / Enter / Space / 数字键 / 方向键` 行为符合 phase 状态图。
4. `ActionHintModel` 只消费规则层已暴露 typed hint，不在 client 预测普通敌人 intent。
5. 禁用动作、无合法目标、非法目标、单一方式动作都有可读反馈。
6. 人工白盒可用固定场景完整走通三层决策。

## 2. 当前问题

1. `UiMode.TARGETING` 只表达目标选择，不表达动作层、方式层和目标层。
2. `OverlayRenderSnapshot` 已有 telegraph 输入面，但目标卡和日志前缀对同一 telegraph 的 icon/color 语义不够统一。
3. `ActorRenderSnapshot.aiTypeId` 只是 AI 类型标签，不能被 client 解释为下一步意图。
4. 如果在本 PR 中顺手做普通敌人 intent，会引入新的 snapshot/AI 真源并扩大验证面。

### 2.1 本 PR 必须冻结的口径

1. 复用现有链路：`PendingTelegraphState -> OverlayRenderSnapshot -> TelegraphRenderer`。
2. 不新增 `AIPlanSnapshot` 或普通敌人 intent snapshot。
3. `CombatDecisionFrame` 是单一 stack frame 和内部 phase 机，不把三层拆成三个 stack frame；视觉上必须走地图优先 targeting，不能在目标选择时遮挡战场。
4. `Backspace` 回上一 phase；`ESC` 直接退出到 `MAP`。
5. 单一 method action 可跳过 `METHOD` 进入 `TARGET`；若玩家是从 `ACTION` 层确认进入，则 `Backspace` 回 `ACTION`；若玩家是技能/铭刻热键直达 `TARGET`，`Backspace` 直接取消回 `MAP`。
6. TARGET phase 的 defensive `legalTargets.isEmpty()` 不自动降级、不自动退出；玩家必须手动 `Backspace` 或 `ESC`。已知 `legalTargetSummary.count == 0` 的 action 在 ACTION phase 提前禁用。
7. 战斗决策期间 `Ctrl+S` 阻断并 toast `ui.message.save.blocked-in-combat-decision`。

## 3. 范围与非目标

### 3.1 范围

1. 修改：
   - `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
   - `game/src/main/resources/i18n/zh-CN.json`
   - `game/src/main/resources/i18n/en-US.json`
   - `build.gradle.kts`
2. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionPanel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionFrame.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/combat/ActionHintModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/combat/CombatAffordanceResourceKeys.kt`
3. 如需窄扩 contract：
   - `core/src/main/kotlin/com/ktome/core/combat/CombatResolutionTrace.kt`
   - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
4. 资源：
   - `assets-src/image/specs/phase4-uiux-pr05-gemini-plan.yaml`
   - `assets-src/image/manifests/phase4-uiux-pr05-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-uiux-pr05-processing-report.jsonl`
   - `assets-src/audio/specs/phase4-uiux-pr05-audio-plan.yaml`
   - `assets-src/audio/manifests/phase4-uiux-pr05-audio-generation-report.jsonl`
   - `assets-src/audio/manifests/phase4-uiux-pr05-audio-processing-report.jsonl`
5. 测试：
   - `client/src/test/kotlin/com/ktome/client/ui/combat/CombatDecisionFrameTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/combat/CombatDecisionPanelTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/combat/ActionHintModelBuilderTest.kt`
   - `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`
   - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`
   - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
   - `tools/src/test/kotlin/com/ktome/tools/lint/AiIntentLeakRuleTest.kt`
   - `tools/src/test/kotlin/com/ktome/tools/lint/CombatAffordanceResourceAuditRuleTest.kt`

### 3.2 非目标

1. 不消费普通敌人 intent。
2. 不新增通用 AI plan snapshot。
3. 不做 spell-by-spell 专属音效工程。
4. 不新增第四层解释页；解释继续走 PR-04 的 ExplainPane。
5. 不把 combat decision phase 拆成多个 modal stack frame。

## 4. 技术方案

### 4.1 Telegraph 三位一体

同一 `OverlayRenderSnapshot` 需要派生：

1. 地图 overlay：shape、danger color、preview turns、cells。
2. 目标卡风险摘要：sourceAbilityId、danger label、preview turns、受影响范围。
3. 日志前缀：icon、danger tone、warning text。

要求：

1. 三处使用同一 `TelegraphPresentationModel` 或同一 builder。
2. danger color 继续消费 `UiDesignTokens`。
3. warning text 继续走 `RenderTextTokenSnapshot` / locale。
4. 缺 warning message 时有可读 fallback，不静默隐藏。
5. `TelegraphPresentationModel` 是完整视图；PR-04 的 `StatusPresentationModel[group=TELEGRAPH]` 只能由它投影出 compact 视图，不允许反向再建第二套 telegraph source。

### 4.2 `CombatDecisionFrame`

frame state 必须显式记录 method 是否被跳过，以及 TARGET 阶段 `Backspace` 是否存在真实上一层：

```kotlin
data class CombatDecisionFrameState(
    val phase: CombatDecisionPhase,
    val selectedActionId: String?,
    val selectedMethodId: String?,
    val skippedMethod: Boolean = false,
    val targetBackspacePhase: CombatDecisionPhase? = null,
)
```

`skippedMethod == true` 只表达 method 层被跳过；它不能单独决定回退语义。`targetBackspacePhase` 是 TARGET 阶段的回退合同：从 ACTION 选择单一方式进入 TARGET 时为 `ACTION`，从 METHOD 进入 TARGET 时为 `METHOD`，从技能/铭刻热键直达 TARGET 时为 `null` 并由 `Backspace` 直接取消。禁止靠 method list size 或 hotkey 文案现场反推。

状态图：

```text
[enter frame] -> ACTION

ACTION
  数字键 N -> 选择并确认第 N 个 action；0 固定 no-op，无 toast
  Enter -> 确认当前高亮 action
  Space -> 等价 Enter
  Tab / Shift+Tab -> action 高亮循环
  Ctrl+S -> 保持 ACTION + toast ui.message.save.blocked-in-combat-decision
  若 N > action list size 或 N > 9 -> no-op，无 toast
  若 action.legalTargetSummary.count == 0 && action.legalTargetSummary.missingReason == null -> action 显示 disabled；提交时保持 ACTION + toast ui.message.combat.no-legal-target
  若 action.legalTargetSummary.count == null -> 不显示 no-legal-target disabled；展示 missingFactReason / missingReason，并禁止 client 反推合法目标数
  若 action.methodOptions.size > 1 -> METHOD
  若 action.methodOptions.size == 1 -> TARGET，并设置 skippedMethod=true、targetBackspacePhase=ACTION
  若 action.methodOptions.size == 0 -> 拒绝选中并保持 ACTION
  Backspace -> pop frame 回 MAP
  ESC -> pop frame 回 MAP

METHOD
  数字键 N -> 选择并确认第 N 个 method -> TARGET；0 固定 no-op，无 toast
  Enter -> 确认当前高亮 method -> TARGET，并设置 skippedMethod=false、targetBackspacePhase=METHOD
  Space -> 等价 Enter
  Tab / Shift+Tab -> method 高亮循环
  Ctrl+S -> 保持 METHOD + toast ui.message.save.blocked-in-combat-decision
  若 N > method list size 或 N > 9 -> no-op，无 toast
  Backspace -> ACTION
  ESC -> pop frame 回 MAP

TARGET
  数字键 N -> 选择并确认第 N 个合法 target -> resolve + pop frame；0 固定 no-op，无 toast
  Enter / Space -> 确认当前高亮 target -> resolve + pop frame
  Tab / Shift+Tab -> 合法 target 高亮循环
  Ctrl+S -> 保持 TARGET + toast ui.message.save.blocked-in-combat-decision
  若 N > legal target list size 或 N > 9 -> no-op，无 toast
  若 legalTargets.isEmpty() -> 仅作为 post-method snapshot drift / count unknown 后的防御路径；拒绝提交 + toast ui.message.combat.no-legal-target
  若尝试确认非法位置 -> 保持 TARGET + toast ui.message.combat.illegal-target
  cursor 悬停非法格 -> 显示 illegal 边框，不 toast；确认时才 toast
  Backspace -> 若 targetBackspacePhase=ACTION 则 ACTION；若 targetBackspacePhase=METHOD 则 METHOD；若 targetBackspacePhase=null 则取消回 MAP
  ESC -> pop frame 回 MAP
```

技能/铭刻热键直达 TARGET 是 ToME-like map-first targeting 路径：热键已经确定 action 和 method，画面必须保持地图为主，直接显示 targeting cursor、合法目标高亮、非法 hover 边框和侧栏/底部 HUD 提示。`CombatDecisionPanel` 可以继续生成 action/method/target 文本模型供侧栏与底部动作条消费，但 `TileOverlayModelBuilder` 不得为 `COMBAT_DECISION` 生成居中 `activeModal` 或 modal backdrop。

已知 `legalTargetSummary.count == 0` 的 action 必须在 ACTION phase 禁用并短路；TARGET phase 的 `legalTargets.isEmpty()` 只处理选择 method 后目标集合被刷新、snapshot 漂移或合法目标数未知但列表为空的防御场景。

phase 内键位：

| phase | 数字键 | `Tab / Shift+Tab` | 方向键 |
| --- | --- | --- | --- |
| `ACTION` | 选择并确认 action | action 高亮循环 | 上下滚 action list |
| `METHOD` | 选择并确认 method | method 高亮循环 | 上下滚 method list |
| `TARGET` | 选择并确认合法 target | 合法 target 循环 | 自由移动 targeting cursor |

`Space` 在 `ACTION / METHOD / TARGET` 中等价 `Enter`，不再承担等待语义。

### 4.3 `ActionHintModel`

只能消费已暴露的 typed fact：

1. talent/inscription 当前可用性
2. resource cost / cooldown / range
3. legal target count
4. telegraph danger 与 action 关联信息，如果规则层已经暴露
5. 禁用原因

禁止：

1. 根据 `aiTypeId` 推测普通敌人下一步意图。
2. 在 client 根据 monster stats 自行算推荐动作。
3. 读取未进入 snapshot 的规则状态。

最小字段：

```kotlin
data class ActionHintModel(
    val availability: ActionAvailability,
    val resourceCosts: List<RenderTextTokenSnapshot>,
    val cooldownTurns: Int?,
    val rangeSummary: RenderTextTokenSnapshot?,
    val legalTargetSummary: LegalTargetSummary,
    val disabledReason: RenderTextTokenSnapshot?,
    val telegraphLinkage: TelegraphLinkageHint?,
    val missingFactReason: RenderTextTokenSnapshot? = null,
)

data class LegalTargetSummary(
    val count: Int?,
    val missingReason: RenderTextTokenSnapshot? = null,
)
```

builder 签名建议为 `ActionHintModelBuilder.build(snapshot, frameState, actionId)`。若现有 snapshot 无法提供某字段，字段保持 `null`、空集合或 `LegalTargetSummary(count = null, missingReason = ...)`，并通过 `missingFactReason` 显示“规则层暂未暴露该信息”；不得把未知渲染成“没有成本/没有冷却/没有风险/没有合法目标”，也不得从 client 反推规则状态。

缺失原因展示优先级：

1. 字段级缺失使用对应 inline `missingFactReason`，例如 resource cost / cooldown / `rangeSummary` / telegraph linkage。
2. 合法目标摘要缺失使用 `legalTargetSummary.missingReason`。
3. 两者同时存在时可以都保留，但 renderer 必须按 token key 去重，避免同一句“规则层暂未暴露该信息”重复显示。

`TelegraphLinkageHint` 最小 shape：

```kotlin
data class TelegraphLinkageHint(
    val telegraphId: String,
    val dangerLevel: DangerLevel,
    val previewTurnsRemaining: Int,
)
```

该 shape 必须复用 `TelegraphPresentationModel` 已有字段语义，禁止 client 再建第二套 danger / preview turns 解释。

### 4.3.1 Contract 扩张触发条件

当前 PR 默认使用现有 snapshot 字段覆盖 `TelegraphPresentationModel / ActionHintModel`。只有满足以下条件之一，才允许窄扩 `CombatResolutionTrace` 或 `FoundationGameSession`：

1. 需要展示的 typed hint 已是规则层权威事实，但未进入任何 snapshot。
2. 缺字段会导致 client 自行计算伤害、命中、AI 行动或 legality。
3. 需要把 owner gate / white-box artifact 中已有的 combat fact 暴露给正式 UI。

触发任一条件时，必须先回到 phase4 roadmap / checklist 更新 owner 说明和验证入口，不能在 client 内临时补字段。

### 4.4 输入接线

1. 入口固定为玩家在 `MAP` 按攻击键或施法/动作入口键；若 `legalActions >= 1`，push `COMBAT_DECISION` frame。
2. 若 `legalActions == 0`，保持 `MAP` 并 toast `ui.message.combat.no-available-action`。
3. active modal stack 非空时，不被动清栈进入 combat decision；玩家必须先用 `ESC / Backspace` 退出当前 modal，再进入战斗决策。
4. PR-02 `TARGETING` 行在本 PR 后由 `CombatDecisionFrame.TARGET` phase 接管；旧 `TARGETING` 只允许作为内部 cursor 兼容壳。
5. `Backspace` 在 phase 内只回到用户真实访问过的上一层；热键直达 `TARGET` 没有上一层，直接 pop stack 回 `MAP`。
6. `ESC` 从任意 phase pop frame 回 `MAP`。
7. `Ctrl+S` 在 frame 内阻断并 toast。
8. `CombatDecisionFrame` push 后必须暂停 PR-02 `PaneFocusController` 的跨面板焦点循环；frame pop 后恢复原焦点锚点。`Tab / Shift+Tab` 在 frame 内只循环当前 phase 的候选项，不切到世界面/上下文面/角色动作面。

新增 locale key：

| key | 所属面 | 示例文本 |
| --- | --- | --- |
| `ui.message.combat.no-legal-target` | combat decision target | `没有合法目标。` |
| `ui.message.combat.illegal-target` | combat decision target | `该目标当前不可选。` |
| `ui.message.combat.no-available-action` | combat decision entry | `当前没有可用动作。` |
| `ui.message.save.blocked-in-combat-decision` | combat decision save | `战斗决策中不能保存。` |

## 5. 推荐改动面

### 5.1 `client/telegraph`

1. 扩展 PR-04 已冻结的最小 `TelegraphPresentationModel`，只 append 三位一体所需字段。
2. `TelegraphRenderer` 不再只服务地图 overlay，也能输出目标卡/日志前缀所需字段。
3. 统一 fallback icon/color/text。
4. `TelegraphPresentationModelTest` 必须覆盖 PR-04 compact 投影仍不变，以及 PR-05 append 字段不会产生第二 builder。

### 5.2 `client/ui/combat`

1. 新增 `CombatDecisionFrame` 与 phase state。
2. 新增 `CombatDecisionPanel` 负责生成动作层、方式层、目标层的文本/图标模型；该模型只进入侧栏和底部动作条，不作为 `TARGET` 阶段居中弹窗。
3. `ActionHintModel` 聚合 typed hint、禁用原因、resource/cooldown/range。

### 5.3 `client/input`

1. 接入 `COMBAT_DECISION` frame。
2. 复用 PR-02 `ModalStack` 和 key semantics。
3. 覆盖 `Ctrl+S` blocked toast。

### 5.4 `tests`

1. `InputHandlerTest` 增加 combat frame phase tests。
2. `TileRendererCanvasTest` 增加 triple telegraph、map-first combat targeting highlight 和 no blocking modal tests。
3. `ClientSmokeHarnessTest` 增加 boss/scripted telegraph + action decision smoke。
4. `GoldenScreenshotHarnessTest` 增加 `phase4-uiux-pr05-*` label。
5. `CombatDecisionFrameTest` 覆盖 ACTION/METHOD/TARGET phase state machine，至少覆盖 §4.2 中 `Enter / Space / Tab / Ctrl+S / Backspace / ESC / 数字越界 / 0 no-op / disabled action / skippedMethod Backspace`。
6. `CombatDecisionPanelTest` 覆盖 `ActionHintModel` 的 resource/cooldown/range/legal target/disabled reason 可见性。
7. `ActionHintModelBuilderTest` 覆盖从 snapshot 到 model 的纯构建，不允许读取未进 snapshot 的规则状态；必须包含 `null fact -> missingFactReason`、`missing rangeSummary -> missingFactReason`、`missing legal target count -> missingFactReason`、`known zero legal targets -> no-legal-target feedback`、`real zero cost -> 不显示 missingFactReason` 五组差异。
8. `AiIntentLeakRuleTest` 或等价 lint/test 扫描普通敌人 intent 泄漏：locale 中 `下一步 / 预测 / 即将 / next action / predicted` 只允许 boss/scripted telegraph allow-list；`aiTypeId` 消费点只能作为 type label。

golden label 清单：

1. `phase4-uiux-pr05-telegraph-triple-surface`
2. `phase4-uiux-pr05-combat-decision-action`
3. `phase4-uiux-pr05-combat-decision-method`
4. `phase4-uiux-pr05-combat-decision-target`
5. `phase4-uiux-pr05-combat-decision-disabled`
6. `phase4-uiux-pr05-combat-decision-illegal-target`

## 6. 测试与自证

### 6.1 必测行为

1. 地图、目标卡、日志三处 telegraph 同语义。
2. `ACTION -> METHOD -> TARGET -> resolve` 可走通。
3. 单一 method 跳过 `METHOD`，但 `Backspace` 回 `ACTION`。
4. 无合法目标不自动退出，有 toast。
5. 禁用动作不提交，有原因。
6. `ESC / Backspace / Tab / Shift+Tab / Enter / Space / 数字键 / 方向键 / Ctrl+S` 行为符合文档。
7. 普通敌人 intent 没有被伪造，并由 `AiIntentLeakRuleTest` 或等价 lint/test 覆盖。
8. 缺失 resource cost、cooldown、rangeSummary、legal target count、telegraph linkage 五类 typed fact 时，`missingFactReason` 可见，且不会显示 `0 cost / ready / no range / no target / no risk`。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.input.InputHandlerTest" --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.ui.combat.CombatDecisionFrameTest" --tests "com.ktome.client.ui.combat.CombatDecisionPanelTest" --tests "com.ktome.client.ui.combat.ActionHintModelBuilderTest"
./gradlew :client:test --tests "com.ktome.client.telegraph.TelegraphPresentationModelTest"
./gradlew :tools:test --tests "com.ktome.tools.lint.AiIntentLeakRuleTest"
./gradlew :tools:test --tests "com.ktome.tools.lint.CombatAffordanceResourceAuditRuleTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew localeLint contractLint assetLint styleLint manifestLint audioLint maintainabilityLint
./gradlew clientSmoke goldenScreenshot verifyChanged
./scripts/verify-bootstrap.sh
```

若触及 `core/src/main/kotlin/com/ktome/core/combat/CombatResolutionTrace.kt`：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :core:test
```

若触及 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、snapshot/session mapper 或正式 UI 所需 render snapshot contract：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:test --tests "*RenderSnapshot*"
```

本 PR 新增 combat affordance image/audio plan，必须补跑资源与 bootstrap gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint audioLint
./scripts/verify-bootstrap.sh
```

### 6.3 人工白盒验证流程

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. validation preset：`BOSS_VARIANT` seed `20260412`
4. 记录文件建议：`docs/opt/ui-pr/manual-records/phase4-uiux-pr05-combat-decision.md`

流程：

1. 启动：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run
```

2. telegraph 三位一体：
   - 进入 boss/scripted telegraph 固定场景
   - 等待 telegraph 出现
   - 检查地图 overlay、目标卡、日志前缀 icon/color/danger label 一致
3. 战斗三层：
   - 进入 `CombatDecisionFrame`
   - `ACTION -> METHOD -> TARGET -> resolve` 完整执行一次
   - 对数字键、`Enter`、`Tab / Shift+Tab`、方向键逐项记录
4. 回退语义：
   - 在 `METHOD` 按 `Backspace` 回 `ACTION`
   - 在 `TARGET` 按 `Backspace` 回上一 phase
   - 在任意 phase 按 `ESC` 回 `MAP`
5. 边界：
   - 选择禁用动作
   - 选择无合法目标动作
   - 选择非法目标
   - 选择单一方式动作
   - 在 combat decision 中按 `Ctrl+S`
   - 使用缺少 resource cost、cooldown、legal target count 或 telegraph linkage typed hint 的 fixture/debug preset，确认面板显示“规则层暂未暴露该信息”或等价 `missingFactReason` 文案，而不是显示 `0 cost / ready / no target / no risk`
6. 非目标核查：
   - 目标卡中不出现 `下一步:` / `预测:` / `即将:` 等普通敌人行动预测前缀
   - 日志前缀不含 boss/scripted telegraph 以外来源的 AI 动作预测
   - `AsciiRenderModel / TileRenderModel` 中搜索 `aiTypeId` 的消费点只用于类型标签，不作为 intent
7. 保存证据：
   - `phase4-uiux-pr05-telegraph-triple-surface`
   - `phase4-uiux-pr05-combat-decision-action`
   - `phase4-uiux-pr05-combat-decision-method`
   - `phase4-uiux-pr05-combat-decision-target`
   - `phase4-uiux-pr05-combat-decision-script`

通过标准：

1. 玩家能看懂当前风险和当前决策层。
2. 回退语义稳定且与 PR-02 一致。
3. 无合法/非法/禁用路径均有反馈。

### 6.4 统一验证框架关系

本 PR 不默认新增 Phase 4 white-box domain；若实现中扩了 combat contract 或 report artifact，必须先回到 phase4 checklist 和 unified white-box framework 更新 owner。人工白盒是当前 PR 的必需验收面，不能被 skipped golden 替代。

## 7. 资源生成计划

### 7.1 Combat Affordance Resource Audit

本 PR 新增正式 combat affordance resource audit。当前仓库没有 `phase4-uiux-pr05-*` image/audio plan，也没有 action、method、target、lock、invalid 五类 combat decision formal key；这些 key 是玩家识别“我正在选动作 / 方式 / 目标、目标是否已锁定、当前提交为什么无效”的第一层感知锚点，因此本 PR 必须补最小 image/audio plan，不再把该资源面写成可选 fallback。

实现前先用以下命令确认当前基线；若任一 key 已由前序分支补齐，则仍必须在本 PR 记录为复用的正式 key，并由 lint/manifest 证明可解析：

```bash
rg -n "ui\\.combat\\.(action|method|target|lock|invalid)|audio\\.combat\\.(action|method|target|lock|invalid)" \
  client/src/main/resources/manifests \
  assets-src/image/manifests \
  assets-src/audio/manifests
```

必须交付的 formal key：

| 面 | visual key | audio cue key | 用途 |
| --- | --- | --- | --- |
| action phase | `ui.combat.action.icon` | `audio.combat.action.confirm` | 进入/确认动作层，帮助玩家区分“选动作”不是普通菜单 hover |
| method phase | `ui.combat.method.icon` | `audio.combat.method.confirm` | 进入/确认方式层，帮助玩家理解同一动作下的方式差异 |
| target phase | `ui.combat.target.icon` | `audio.combat.target.confirm` | 进入/确认目标层，帮助玩家区分自由移动 cursor 与正式目标选择 |
| target lock | `ui.combat.lock.icon` | `audio.combat.target.lock` | 当前目标被锁定或即将提交时的稳定锚点 |
| invalid / disabled | `ui.combat.invalid.icon` | `audio.combat.invalid.submit` | 禁用动作、无合法目标、非法目标、数字越界提交的统一错误锚点 |

约束：

1. `CombatDecisionPanel`、`TileRenderModel`、golden fixture 和 smoke 路径必须消费这些 exact key；不得只把 key 写入 manifest。
2. `audio.combat.invalid.submit` 只用于真实提交失败或确认非法目标；hover 到非法格只显示 `ui.combat.invalid.icon` / invalid border，不播放错误音。
3. `ui.combat.lock.icon` 与 `audio.combat.target.lock` 只表示玩家确认的锁定语义，不表示普通敌人 intent，也不能用于预测普通敌人下一步。
4. 缺任一 formal key 或 key 不可解析时，`assetLint / styleLint / manifestLint / audioLint` 必须失败；不得通过 `missing_visual` 或 `audio.fallback.silence` 开放正式 combat decision 路径。
5. 若实现发现现有正式 key 可复用，必须在 Resource Fallback Audit 中标记为“formal reuse”，并写明 PR 启动 commit 前已存在于对应 manifest；不能把新 UI 路径依赖的缺失 key 写成 fallback。

### 7.2 图片

本 PR 必须新增最小 combat affordance companion visual plan：

```text
assets-src/image/specs/phase4-uiux-pr05-gemini-plan.yaml
assets-src/image/manifests/phase4-uiux-pr05-generation-report.jsonl
assets-src/image/manifests/phase4-uiux-pr05-processing-report.jsonl
```

生成范围固定为：

1. `ui.combat.action.icon`
2. `ui.combat.method.icon`
3. `ui.combat.target.icon`
4. `ui.combat.lock.icon`
5. `ui.combat.invalid.icon`

视觉约束：

1. 图标必须是小尺寸可读的 painterly UI icon，不出现文字、数字、完整人物立绘或纯装饰背景。
2. action/method/target 三个 icon 必须能在 24-32px 侧栏尺寸下区分层级。
3. lock 必须读作“目标锁定/即将提交”，invalid 必须读作“不可提交/错误”，不能只靠颜色区分。
4. 所有 raw / processed / runtime 资源必须走现有 image pipeline 和 `sync_phase2_manifests.py`。

### 7.3 音频

本 PR 必须新增最小 combat affordance audio plan：

```text
assets-src/audio/specs/phase4-uiux-pr05-audio-plan.yaml
assets-src/audio/manifests/phase4-uiux-pr05-audio-generation-report.jsonl
assets-src/audio/manifests/phase4-uiux-pr05-processing-report.jsonl
```

生成范围固定为：

1. `audio.combat.action.confirm`
2. `audio.combat.method.confirm`
3. `audio.combat.target.confirm`
4. `audio.combat.target.lock`
5. `audio.combat.invalid.submit`

音频约束：

1. 使用现有 repo 音频生成/处理管线，不新增第二套工具链。
2. 每个 cue 必须短、清晰、非旋律化；不得做 spell-by-spell 专属音效工程。
3. action/method/target confirm 可以共享音色家族，但必须有可听层级差异。
4. invalid submit 必须和 `audio.ui.cancel` 可区分，避免玩家把非法目标误解成普通返回。
5. target lock 必须和 boss warning 区分，避免把玩家目标确认误读成 boss/scripted telegraph 升级。

### 7.4 约束

1. 新 key 必须同步 plan、manifest、`build.gradle.kts --extra-plan`。
2. 新 key 必须被 smoke/golden 消费。
3. PR 合并前不得处于“未生成正式资源但开放 combat decision 正式路径”的状态；实现阶段若临时缺 key，必须登记 [Resource Fallback Audit](resource-fallback-audit-template.md)，且 `失效风险等级=high` 或 UI 路径依赖新 key 时不开放正式玩家路径，不能用 fallback 证据代替资源落地。
4. PR description 和 manual record 必须附 combat affordance resource audit：列出 action/method/target/lock/invalid 五类 key 的 `visualKey / audioCueId / manifest path / consumer test / smoke or golden label`。
5. 资源生成并接线后必须运行 `assetLint / styleLint / manifestLint / audioLint` 与 `./scripts/verify-bootstrap.sh`。

## 8. 出口门禁

1. `TelegraphPresentationModel`、`CombatDecisionFrame`、`CombatDecisionPanel`、`ActionHintModel` 已落地。
2. telegraph 三位一体一致。
3. 战斗三层决策可完整走通。
4. `ESC / Backspace / Ctrl+S` 与 PR-02 输入语义一致。
5. smoke/golden/locale/contract/maintainability 已执行或明确说明无法执行原因。
6. 新增 locale key `ui.message.combat.no-legal-target / ui.message.combat.illegal-target / ui.message.combat.no-available-action / ui.message.save.blocked-in-combat-decision` 已进入 `zh-CN` 与 `en-US`。
7. Combat affordance resource audit 已完成；action/method/target/lock/invalid 五类 formal visual/audio key 已进入 plan、manifest、runtime resources、consumer tests 与 smoke/golden evidence。
8. 停止任何 `PR-02` 遗留的 `combat-decision-stub` golden label，并重录为 `phase4-uiux-pr05-*`。
9. README “跨 PR Deferred 与收口”中的 `COMBAT_DECISION` stub 条目已在本 PR 关闭；PR-02 truth table 的 `TARGETING` 行已由 `CombatDecisionFrameTest` 中 TARGET phase tests 接管，旧 `InputHandlerTest.TARGETING` 用例保留但 re-target 到 frame phase 断言。
10. 人工白盒记录包含 telegraph 三位一体、三层决策、回退、禁用/非法/无目标、非目标核查证据。
11. 没有新增普通敌人 intent、AI plan snapshot、第四层解释页或第二 combat rule authority。
