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

完成 telegraph 三位一体和战斗三层决策面。

完成标准：

1. Boss/scripted telegraph 在地图 overlay、目标卡、日志前缀上同 icon、同主色、同 danger 语义。
2. 新增单一 `CombatDecisionFrame`，内部 phase 为 `ACTION -> METHOD -> TARGET`。
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
3. `CombatDecisionFrame` 是单一 modal frame，内部 phase 机，不把三层拆成三个 stack frame。
4. `Backspace` 回上一 phase；`ESC` 直接退出到 `MAP`。
5. 单一 method action 可跳过 `METHOD` 进入 `TARGET`；这种路径下 `Backspace` 直接回 `ACTION`。
6. `legalTargets.isEmpty()` 不自动降级、不自动退出；玩家必须手动 `Backspace` 或 `ESC`。
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
2. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionPanel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionFrame.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/combat/ActionHintModel.kt`
3. 如需窄扩 contract：
   - `core/src/main/kotlin/com/ktome/core/combat/CombatResolutionTrace.kt`
   - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
4. 资源：
   - 可选 `assets-src/image/specs/phase4-uiux-pr05-gemini-plan.yaml`
   - 可选 `assets-src/audio/specs/phase4-uiux-pr05-audio-plan.yaml`
5. 测试：
   - `client/src/test/kotlin/com/ktome/client/ui/combat/CombatDecisionFrameTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/combat/CombatDecisionPanelTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/combat/ActionHintModelBuilderTest.kt`
   - `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`
   - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`
   - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
   - `tools/src/test/kotlin/com/ktome/tools/lint/AiIntentLeakRuleTest.kt`

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

状态图：

```text
[enter frame] -> ACTION

ACTION
  数字键 N -> 选择并确认第 N 个 action
  Enter -> 确认当前高亮 action
  Space -> 等价 Enter
  Tab / Shift+Tab -> action 高亮循环
  Ctrl+S -> 保持 ACTION + toast ui.message.save.blocked-in-combat-decision
  若 N > action list size 或 N > 9 -> no-op，无 toast
  若 action.legalTargetSummary.count == 0 && action.legalTargetSummary.missingReason == null -> action 显示 disabled；提交时保持 ACTION + toast ui.message.combat.no-legal-target
  若 action.legalTargetSummary.count == null -> 不显示 no-legal-target disabled；展示 missingFactReason / missingReason，并禁止 client 反推合法目标数
  若 action.methodOptions.size > 1 -> METHOD
  若 action.methodOptions.size == 1 -> TARGET
  若 action.methodOptions.size == 0 -> 拒绝选中并保持 ACTION
  Backspace -> pop frame 回 MAP
  ESC -> pop frame 回 MAP

METHOD
  数字键 N -> 选择并确认第 N 个 method -> TARGET
  Enter -> 确认当前高亮 method -> TARGET
  Space -> 等价 Enter
  Tab / Shift+Tab -> method 高亮循环
  Ctrl+S -> 保持 METHOD + toast ui.message.save.blocked-in-combat-decision
  若 N > method list size 或 N > 9 -> no-op，无 toast
  Backspace -> ACTION
  ESC -> pop frame 回 MAP

TARGET
  数字键 N -> 选择并确认第 N 个合法 target -> resolve + pop frame
  Enter / Space -> 确认当前高亮 target -> resolve + pop frame
  Tab / Shift+Tab -> 合法 target 高亮循环
  Ctrl+S -> 保持 TARGET + toast ui.message.save.blocked-in-combat-decision
  若 N > legal target list size 或 N > 9 -> no-op，无 toast
  若 legalTargets.isEmpty() -> 拒绝提交 + toast ui.message.combat.no-legal-target
  若尝试确认非法位置 -> 保持 TARGET + toast ui.message.combat.illegal-target
  cursor 悬停非法格 -> 显示 illegal 边框，不 toast；确认时才 toast
  Backspace -> 若来自单一方式跳过路径则 ACTION，否则 METHOD
  ESC -> pop frame 回 MAP
```

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
5. `Backspace` 在 phase 内回退，不直接 pop stack，除非在 `ACTION`。
6. `ESC` 从任意 phase pop frame 回 `MAP`。
7. `Ctrl+S` 在 frame 内阻断并 toast。

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

### 5.2 `client/ui/combat`

1. 新增 `CombatDecisionFrame` 与 phase state。
2. 新增 `CombatDecisionPanel` 负责绘制动作层、方式层、目标层。
3. `ActionHintModel` 聚合 typed hint、禁用原因、resource/cooldown/range。

### 5.3 `client/input`

1. 接入 `COMBAT_DECISION` frame。
2. 复用 PR-02 `ModalStack` 和 key semantics。
3. 覆盖 `Ctrl+S` blocked toast。

### 5.4 `tests`

1. `InputHandlerTest` 增加 combat frame phase tests。
2. `TileRendererCanvasTest` 增加 triple telegraph and combat decision panel tests。
3. `ClientSmokeHarnessTest` 增加 boss/scripted telegraph + action decision smoke。
4. `GoldenScreenshotHarnessTest` 增加 `phase4-uiux-pr05-*` label。
5. `CombatDecisionFrameTest` 覆盖 ACTION/METHOD/TARGET phase state machine，至少覆盖 §4.2 中 `Enter / Space / Tab / Ctrl+S / Backspace / ESC / 数字越界 / disabled action`。
6. `CombatDecisionPanelTest` 覆盖 `ActionHintModel` 的 resource/cooldown/range/legal target/disabled reason 可见性。
7. `ActionHintModelBuilderTest` 覆盖从 snapshot 到 model 的纯构建，不允许读取未进 snapshot 的规则状态；必须包含 `null fact -> missingFactReason`、`missing legal target count -> missingFactReason`、`known zero legal targets -> no-legal-target feedback`、`real zero cost -> 不显示 missingFactReason` 四组差异。
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
7. 普通敌人 intent 没有被伪造。
8. 缺失 resource cost、cooldown、legal target count、telegraph linkage 四类 typed fact 时，`missingFactReason` 可见，且不会显示 `0 cost / ready / no target / no risk`。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.input.InputHandlerTest" --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.ui.combat.CombatDecisionFrameTest" --tests "com.ktome.client.ui.combat.CombatDecisionPanelTest" --tests "com.ktome.client.ui.combat.ActionHintModelBuilderTest"
./gradlew :tools:test --tests "com.ktome.tools.lint.AiIntentLeakRuleTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew localeLint contractLint maintainabilityLint
./gradlew clientSmoke goldenScreenshot verifyChanged
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

若新增资源：

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

### 7.1 图片

默认优先复用现有图标。若需要战斗 affordance companion 资源：

```text
assets-src/image/specs/phase4-uiux-pr05-gemini-plan.yaml
assets-src/image/manifests/phase4-uiux-pr05-generation-report.jsonl
assets-src/image/manifests/phase4-uiux-pr05-processing-report.jsonl
```

允许范围：

1. 动作层 icon
2. 方式层区分 icon
3. 目标层锁定/危险标记

### 7.2 音频

默认复用 `audio.ui.*`、`audio.boss.warning`、战斗基础 cue。若确实需要：

```text
assets-src/audio/specs/phase4-uiux-pr05-audio-plan.yaml
assets-src/audio/manifests/phase4-uiux-pr05-processing-report.jsonl
```

允许范围：

1. telegraph 升级或锁定预警
2. 动作确认
3. 非法/禁用提交

禁止 spell-by-spell 专属音效工程。

### 7.3 约束

1. 新 key 必须同步 plan、manifest、`build.gradle.kts --extra-plan`。
2. 新 key 必须被 smoke/golden 消费。
3. 未生成正式资源时，必须保留 [Resource Fallback Audit](resource-fallback-audit-template.md)，不开放依赖新 key 的 UI 路径。

## 8. 出口门禁

1. `TelegraphPresentationModel`、`CombatDecisionFrame`、`CombatDecisionPanel`、`ActionHintModel` 已落地。
2. telegraph 三位一体一致。
3. 战斗三层决策可完整走通。
4. `ESC / Backspace / Ctrl+S` 与 PR-02 输入语义一致。
5. smoke/golden/locale/contract/maintainability 已执行或明确说明无法执行原因。
6. 新增 locale key `ui.message.combat.no-legal-target / ui.message.combat.illegal-target / ui.message.combat.no-available-action / ui.message.save.blocked-in-combat-decision` 已进入 `zh-CN` 与 `en-US`。
7. 停止任何 `PR-02` 遗留的 `combat-decision-stub` golden label，并重录为 `phase4-uiux-pr05-*`。
8. PR-02 truth table 第 5 行 `TARGETING` 已由 `CombatDecisionFrameTest` 中 TARGET phase tests 接管；旧 `InputHandlerTest.TARGETING` 用例保留但 re-target 到 frame phase 断言。
9. 人工白盒记录包含 telegraph 三位一体、三层决策、回退、禁用/非法/无目标、非目标核查证据。
10. 没有新增普通敌人 intent、AI plan snapshot、第四层解释页或第二 combat rule authority。
