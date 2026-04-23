> 执行前必须先完整阅读并接受：
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md`
> `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
> `docs/opt/ui-pr/README.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md`

# Phase 4 UI/UX - PR-04 Status, Description, Readability

**阶段**: `Phase 4 late-development / phase4-uiux-pr04`  
**优先级**: `P0`  
**合并来源**: 原计划 `PR-06 状态语义、badge 规则与高风险视觉层级` + `PR-07 动态说明、关键词消费与解释型检视`  
**前置条件**: `PR-02` ModalStack/Look Mode 已冻结，`PR-03` `ModalCardModel` 已冻结。  
**硬依赖条款**:

1. `PR-02` 的 `INSPECT + Backspace` truth table 需要在本 PR 引入 `ExplainPane` 后同步修订优先级。
2. `PR-03` 的 `ModalCardModel` 是 ExplainPane 卡片结构来源；`ExplainPaneModel` 只能作为 `ModalCardModel` 的 composition wrapper，不新增 renderer 可绕过的 inspect-only 字段家族。
3. 本 PR 先冻结 `TelegraphPresentationModel` 的最小 compact shape；`PR-05` 只能 append 完整三位一体字段，不得改已定义字段名或语义。
4. `ValidationPreset.BOSS_VARIANT` 当前已存在于 `com.ktome.game.validation.ValidationPreset` 枚举合同中，本 PR 复用 `seed=20260412`，不新建第二 preset。

**对应问题**: 状态 badge、telegraph 权重、关键词、动态说明和解释型检视都属于“玩家如何读懂当前局面”的同一层。拆开会导致 `TargetCardModel`、`DescriptionPresenter`、`StatusHudRenderer`、`TelegraphRenderer` 和 inspect panel 反复改接口。

---

## 1. 阶段目标

把状态、badge、关键词、动态说明和解释型检视收敛成统一可读性入口。

完成标准：

1. 状态 badge 规则冻结：回合、stack/cap、分类。
2. `BUFF / DEBUFF / NEUTRAL` 在 HUD、目标卡、inspect/explain 中表现一致。
3. `TELEGRAPH` 和 `ZONE_EFFECT` 只作为 client-local presentation group，不污染 status contract。
4. 高风险 telegraph 视觉权重明显高于普通状态。
5. `DescriptionPresenter` 成为动态说明和关键词展示的统一入口。
6. `KeywordRegistry` 被复用，不新增第二套 keyword dictionary。
7. `ExplainPane` 是 `INSPECT` frame 内部 sub-view，不新增 `EXPLAIN` frame / `UiMode`。

## 2. 当前问题

1. `StatusHudRenderer` 和 `StatusIconResolver` 已有状态图标基础，但 badge 分类、优先级和跨面一致性不足。
2. `TelegraphRenderer` 已能渲染 overlay，但高风险提示与普通状态的视觉权重还未统一。
3. `DescriptionPresenter` 主要服务 talent 说明，关键词没有进入背包、商店、inspect 和动作说明主阅读路径。
4. `KeywordRegistry.CORE` 已存在，若另造 `KeywordDictionary` 会形成第二真源。
5. Explain 信息如果新增独立 `UiMode`，会破坏 PR-02 冻结的 modal stack 语义。

### 2.1 本 PR 必须冻结的口径

1. 状态语义继续来自 `StatusEffectRenderSnapshot.category = BUFF / DEBUFF / NEUTRAL`。
2. `TELEGRAPH` presentation group 只来自 `RenderSnapshot.overlays`。
3. `ZONE_EFFECT` presentation group 只来自 `MapCellSnapshot.terrainOverride`。
4. `DescriptionPresenter` 是动态说明消费入口；renderer 不再各自解析关键词。
5. `KeywordRegistry` 仍是 keyword authority；未知 keyword id fail fast。
6. `ExplainPane` 不占 modal stack 深度；`?` 在 `INSPECT` 中优先打开它，已打开时切一般 help。
7. `Advanced Tooltip` 继续后置，不作为当前 PR 阻塞项。

## 3. 范围与非目标

### 3.1 范围

1. 修改：
   - `client/src/main/kotlin/com/ktome/client/ui/status/StatusHudRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt`
   - `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt`
   - `game/src/main/resources/i18n/zh-CN.json`
   - `game/src/main/resources/i18n/en-US.json`
   - `core/src/main/kotlin/com/ktome/core/talent/KeywordRegistry.kt`
   - `core/src/main/kotlin/com/ktome/core/talent/DescriptionModel.kt`
2. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt`
   - `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphPresentationModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/inspect/ExplainPaneModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/settings/AccessibilityToggle.kt`
   - `tools` 侧 `keywordRegistryLint`
3. 资源：
   - 可选 `assets-src/image/specs/phase4-uiux-pr04-gemini-plan.yaml`
   - 可选 `assets-src/audio/specs/phase4-uiux-pr04-audio-plan.yaml`
4. 测试：
   - `client/src/test/kotlin/com/ktome/client/ui/status/StatusPresentationModelTest.kt`
   - `client/src/test/kotlin/com/ktome/client/telegraph/TelegraphPresentationModelTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/inspect/ExplainPaneModelTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/settings/AccessibilityToggleTest.kt`
   - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/talent/DescriptionPresenterTest.kt`
   - `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`
   - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
   - `tools/src/test/kotlin/com/ktome/tools/lint/KeywordRegistryLintTest.kt`

### 3.2 非目标

1. 不新增 status contract、status enum 或 snapshot 字段，除非发现无法用现有 snapshot 表达且必须先回到上游文档修订。
2. 不做普通敌人 intent。
3. 不做 Advanced Tooltip。
4. 不为每个关键词或每条说明生成装饰图。
5. 不新增大规模 ambience、职业氛围音或 spell-by-spell cue。

## 4. 技术方案

### 4.1 `StatusPresentationModel`

建议字段：

```kotlin
data class StatusPresentationModel(
    val typeId: String,
    val nameKey: String?,
    val iconKey: String?,
    val category: StatusEffectCategorySnapshot,
    val rawBadge: String,
    val priority: Int,
    val group: StatusPresentationGroup,
)
```

badge 规则：

1. `remainingTurns > 0` 显示回合。
2. `stackCount > 1` 显示 `xN`。
3. `stackCap != null` 可显示 `N/cap`，但不能挤掉 danger/turn 信息。
4. 同一状态在 HUD、目标卡、inspect/explain 中 raw badge 字符串一致，并由单一 builder 生成，不允许 renderer 各自格式化。
5. badge 内容不走 locale，由 pure formatter 生成，固定形态为 `x3`、`3/5`、`4t`；若未来需要本地化，再单独修订 badge formatter contract。

分组：

1. `BUFF`
2. `DEBUFF`
3. `NEUTRAL`
4. `TELEGRAPH`，只来自 overlay
5. `ZONE_EFFECT`，只来自 terrain override

priority 计算矩阵：

| group/category | base priority |
| --- | --- |
| `TELEGRAPH` | `900 + dangerLevel * 20 + previewTurnsInverse` |
| `DEBUFF` | `700 + stackWeight + remainingTurnsWeight` |
| `ZONE_EFFECT` | `650 + dangerWeight` |
| `BUFF` | `500 + stackWeight + remainingTurnsWeight` |
| `NEUTRAL` | `300 + stackWeight + remainingTurnsWeight` |

公式定义：

1. `dangerLevel`: `LOW=1 / MODERATE=2 / HIGH=3 / LETHAL=4`。
2. `previewTurnsInverse = previewTurnsRemaining?.let { max(0, 5 - it) } ?: 0`；`previewTurnsRemaining` 缺失时取 `0`，不能被公式误算成 `5`。
3. `dangerWeight = dangerLevel * 10`。
4. `remainingTurnsWeight = max(0, 20 - remainingTurns.coerceIn(0, 20))`。
5. `stackWeight = stackCount.coerceIn(0, 10)`。
6. 同 priority 的稳定 tiebreaker 为 `typeId` 字典序。

`TELEGRAPH` 使用 `dangerLevel * 20` 是为了让临近高危 telegraph 稳定压过普通 debuff；`ZONE_EFFECT` 使用 `dangerWeight = dangerLevel * 10` 是因为 zone effect 持续存在，不应抢占所有短时状态。该系数差异必须由 `StatusPresentationModelTest` 覆盖。

实现应提供 pure builder，例如 `StatusPresentationBuilder.build(...)`，并用 `StatusPresentationModelTest` 覆盖排序。

`TelegraphPresentationModel` 在本 PR 只冻结 compact 所需最小 shape：`typeId / nameKey / iconKey / dangerLevel / previewTurnsRemaining / rawBadge`。`StatusPresentationGroup.TELEGRAPH` 必须由 `TelegraphPresentationModel.toStatusPresentation()` 投影得到，投影规则固定为：

1. `group = StatusPresentationGroup.TELEGRAPH`
2. `category = StatusEffectCategorySnapshot.NEUTRAL`，除非后续正式新增 telegraph-only category
3. `priority` 必须复用本节公式，不能在 `TelegraphRenderer` 内另算
4. `badge` 由同一 pure builder 生成，缺 `previewTurnsRemaining` 时仍按 `previewTurnsInverse = 0`

地图 overlay、日志前缀、cells、shape 等完整字段由 PR-05 append 到同一模型，禁止 PR-04 另建 compact telegraph source。

### 4.2 高风险视觉层级

规则：

1. high/lethal telegraph 在地图 overlay、目标卡、日志前缀中权重高于普通状态。
2. 色盲/高对比场景下必须靠形状、描边、badge 或节奏区分。
3. `Reduce Motion` 下不使用依赖闪烁的唯一提示。

a11y 组合矩阵：

| colorBlindSafe | reduceMotion | 可用区分手段 |
| --- | --- | --- |
| `off` | `off` | color + shape + motion + badge |
| `on` | `off` | shape + motion + badge |
| `off` | `on` | color + shape + badge |
| `on` | `on` | shape + badge；禁止 color-only 和 motion-only |

### 4.3 `DescriptionPresenter`

扩展入口：

1. talent active/reserve 说明
2. inventory item 说明
3. shop item 说明
4. inspect object 说明
5. combat action 说明
6. status/effect 说明

要求：

1. 继续使用 `KeywordRegistry.CORE.require(keywordId)`。
2. 未知 keyword fail fast，不静默丢。
3. 动态数值格式化继续复用现有 localizer/formatter。
4. 输出结构必须能被 `ModalCardModel` 或 `ExplainPaneModel` 消费。
5. `combat action` 入口在本 PR 只要求 pure unit test 覆盖；端到端消费由 PR-05 的 `CombatDecisionPanel` 落地时补充。

`ExplainPaneModel` 约束：

1. 它是 `ModalCardModel` 的 composition wrapper，可追加 keyword chips、引用链和说明分段。
2. 它不得持有 renderer 可绕过 `ModalCardModel` 直接绘制的裸 `String` 说明段。
3. `rewardLines / costLines / actions` 等共享卡片能力必须继续由 `ModalCardModel` 承载。

### 4.4 `ExplainPane`

规则：

1. `ExplainPane` 是 `INSPECT` frame 的 sub-view。
2. 不新增 `UiMode.EXPLAIN`。
3. 不占 `ModalStack` 深度。
4. `Backspace` 关闭 ExplainPane sub-view；`ESC` 仍全退。
5. `?` 在 `INSPECT` 中优先打开 ExplainPane；已打开时切 panel help。

与 PR-02 truth table 的修订：

1. PR-02 的默认规则仍是 `INSPECT + Backspace -> MAP`。
2. 本 PR 引入 `ExplainPane` 后，优先级改为：若 `ExplainPane` 已打开，`Backspace` 先关闭 `ExplainPane`；否则按 PR-02 规则回 `MAP`。
3. `InputHandlerTest` 必须新增覆盖：ExplainPane 打开态 Backspace 关闭 sub-view，ESC 仍全退。

### 4.5 keyword lint

lint 名固定为 `keywordRegistryLint`，owner 归 `tools/lint/`。

规则：

1. `DescriptionPresenter` 实际消费的 keyword id 必须存在于 `KeywordRegistry`。
2. `KeywordRegistry` 定义的核心 keyword 至少被一个 formal description surface 引用。formal surface 只包括 `DescriptionModel`、`ExplainPaneModel`、`StatusPresentationModel.nameKey` 和 `KeywordRegistry.CORE.aliases`；测试 fixture、注释和 dead locale key 不算引用。
3. 未知 keyword id error。

只要 `DescriptionPresenter` 在本 PR 后继续消费 keyword id，`keywordRegistryLint` 就是本 PR 必选 gate；不得仅靠人工 review 维持唯一 authority。

lint 实现策略：

1. 优先采用编译期符号扫描或现有 lint owner 能访问的 typed API，直接校验 `DescriptionPresenter` 消费点与 `KeywordRegistry.CORE`。
2. 若 tools 侧暂时无法接入符号扫描，可退化为对 `DescriptionPresenter` 和正式 description fixtures 的源码/数据扫描；只要本 PR 新增/修改的 keyword 消费面覆盖不足，必须 BLOCKED，不允许 WARN 放行。
3. 未知 keyword id 仍必须 fail fast；退化策略不能把运行时 unknown 改成 default-success。
4. 若实现为独立 task，root alias 固定为 `./gradlew keywordRegistryLint`，并同步 `VerificationTaskRegistry`、`VerifyChangedBuildContractTest`、`ScopeCoverageLintTest`。
5. 若复用 `contractLint`，必须在 `ContractLintTest` 或 `KeywordRegistryLintTest` 中有明确 rule selector，并在 PR description 写明 `contractLint` 覆盖 `keywordRegistryLint` 规则。
6. 历史未修改 keyword 面若只能 WARN，必须在 `docs/opt/ui-pr/follow-ups.md` 写 owner、关闭 PR、验证方式和不阻塞理由；不得影响本 PR 新增/修改面。
7. `KeywordRegistryLintTest` 必须覆盖“扫描覆盖面不足时失败或产出 BLOCKED artifact”的用例。

### 4.6 Accessibility toggle

本 PR 认领最小 a11y 控件入口：

1. `AccessibilityToggle.highContrast`
2. `AccessibilityToggle.colorBlindSafe`
3. `AccessibilityToggle.reduceMotion`

实现可先使用启动参数、环境变量或 debug settings 面，不要求正式设置页。三项开关必须能被 renderer 读取并进入人工白盒验证；不能只写文档。

本 PR 不要求运行期 hotkey 或正式设置页切换；如果实现没有 runtime toggle，验证只走启动参数/环境变量。后续若新增 hotkey，必须另开 PR 同步输入语义表和人工白盒步骤。

最小行为：

1. 默认值全部为 `off`。
2. 启动参数固定为 `-Dktome.ui.a11y.highContrast=true`、`-Dktome.ui.a11y.colorBlindSafe=true`、`-Dktome.ui.a11y.reduceMotion=true`。
3. 运行期切换如果存在，允许下一次 `render()` 生效；本 PR 不要求全局 invalidate。
4. 不写入 save；持久化设置页是后续非目标。

### 4.7 新增 locale key 列表

| key | 所属面 | 示例文本 |
| --- | --- | --- |
| `ui.status.group.buff` | status group | `增益` |
| `ui.status.group.debuff` | status group | `减益` |
| `ui.status.group.neutral` | status group | `状态` |
| `ui.status.group.telegraph` | telegraph compact | `危险预兆` |
| `ui.status.group.zone-effect` | zone effect | `区域效果` |
| `ui.status.effect.name` | status fallback | `{name}` |
| `ui.status.effect.line` | status explain line | `{name} {badge}` |
| `ui.explain.title` | ExplainPane | `说明` |
| `ui.explain.empty` | ExplainPane | `当前目标没有额外说明。` |
| `ui.accessibility.high-contrast` | accessibility toggle | `高对比` |
| `ui.accessibility.color-blind-safe` | accessibility toggle | `色盲安全` |
| `ui.accessibility.reduce-motion` | accessibility toggle | `减少动态效果` |

本 PR 必须复用 PR-03 已冻结的共享状态 key：`ui.empty.inspect.title`、`ui.empty.inspect.detail`、`ui.loading.cancel`、`ui.error.action.*`。这些 key 是最小清单；新增 keyword 文案仍归 `KeywordRegistry` 与 `localeLint` 共同约束。

## 5. 推荐改动面

### 5.1 `client/ui/status`

1. 从 `StatusEffectRenderSnapshot` 构建 `StatusPresentationModel`。
2. `StatusHudRenderer.renderCompact(...)` 与 `renderTurns(...)` 收敛到同一 badge helper。
3. `StatusIconResolver` 输出 category、badge、priority。

### 5.2 `client/render`

1. `TargetCardModel` 和 inspect/explain 消费同一 status presentation。
2. `TelegraphRenderer` 的 danger level 与 token、status priority 对齐。
3. `TileRendererCanvasTest` 增加 dense status、zone effect、high risk telegraph case。

### 5.3 `client/ui/talent`

1. `DescriptionPresenter` 扩展为通用 presenter。
2. 保持 existing talent tests，并新增 item/shop/inspect/action 用例。

### 5.4 `core/talent`

1. `KeywordRegistry` 只在必要时补缺失 keyword。
2. 不把 client presentation 信息塞进 core keyword 定义。

## 6. 测试与自证

### 6.1 必测行为

1. `BUFF / DEBUFF / NEUTRAL` badge 一致。
2. zone effect 与 telegraph 不伪装成 status contract。
3. high/lethal telegraph 不被普通状态淹没。
4. `DescriptionPresenter` 服务 talent、item、shop、inspect；combat action 分支由 `DescriptionPresenterTest` 或独立 `CombatActionDescriptionPresenterTest` pure unit case 覆盖，端到端由 PR-05 补。
5. 未知 keyword id fail fast。
6. `ExplainPane` 不新增 `UiMode`，不占 stack。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.ui.talent.DescriptionPresenterTest" --tests "com.ktome.client.input.InputHandlerTest" --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.ui.status.StatusPresentationModelTest" --tests "com.ktome.client.telegraph.TelegraphPresentationModelTest" --tests "com.ktome.client.ui.inspect.ExplainPaneModelTest" --tests "com.ktome.client.ui.settings.AccessibilityToggleTest"
./gradlew :core:test
./gradlew :tools:test --tests "com.ktome.tools.lint.KeywordRegistryLintTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew localeLint contractLint maintainabilityLint
./gradlew clientSmoke goldenScreenshot verifyChanged
```

若 `keywordRegistryLint` 实现为独立 root task，必须额外运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew keywordRegistryLint
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
4. 记录文件建议：`docs/opt/ui-pr/manual-records/phase4-uiux-pr04-status-description.md`

流程：

1. 启动：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run
```

如果实现采用启动参数入口，a11y 复核必须另起一次：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
JAVA_TOOL_OPTIONS="-Dktome.ui.a11y.highContrast=true -Dktome.ui.a11y.colorBlindSafe=true -Dktome.ui.a11y.reduceMotion=true" ./gradlew --no-daemon :client:run
```

若最终实现不通过 `JAVA_TOOL_OPTIONS` 透传 JVM system properties，必须在人工记录中写明实际入口，例如 Gradle `applicationDefaultJvmArgs`、debug settings 面或正式设置页路径。
人工记录必须包含截图或日志，证明 runtime 已读取 `highContrast=true / colorBlindSafe=true / reduceMotion=true`。

2. 状态场景：
   - 制造 `BUFF / DEBUFF / NEUTRAL`
   - 制造 stack、cap、remaining turns
   - 在 HUD、目标卡、Look/Inspect、ExplainPane 中逐项对比 badge
3. zone / telegraph：
   - 制造 terrain override zone effect
   - 制造 low/moderate/high/lethal telegraph
   - 确认 `TELEGRAPH / ZONE_EFFECT` 是 presentation group，不显示为普通 status
4. a11y：
   - 打开高对比/色盲回退/Reduce Motion 或等价开发开关
   - 确认高风险和普通状态仍可区分
5. 动态说明：
   - 打开 talent、背包物品、商店物品、Inspect/Look 对象、战斗动作说明
   - 确认关键词、资源、构筑标签都走统一展示规则
6. ExplainPane：
   - 在 `INSPECT` frame 内按 `?`
   - 确认 ExplainPane 不新增 frame，不改变 `ESC / Backspace`
   - 关闭后 inspect cursor 和焦点恢复
7. 保存证据：
   - `phase4-uiux-pr04-status-badges`
   - `phase4-uiux-pr04-telegraph-risk`
   - `phase4-uiux-pr04-explain-pane`
   - `phase4-uiux-pr04-keyword-dynamic-description`

通过标准：

1. 玩家能一眼区分普通状态、区域效果和高风险 telegraph。
2. 关键词不再只存在于 talent tooltip。
3. ExplainPane 不破坏 PR-02 的 stack 语义。

### 6.4 统一验证框架关系

本 PR 不新增 Phase 4 white-box domain。人工白盒记录必须覆盖 a11y 和说明可读性；golden 只能证明场景稳定，不能代替人工判断“是否读得懂”。

## 7. 资源生成计划

### 7.1 图片

默认不批量扩张。只有重复出现、确实需要 formal key 的状态/预警/关键词 chip 才允许：

```text
assets-src/image/specs/phase4-uiux-pr04-gemini-plan.yaml
assets-src/image/manifests/phase4-uiux-pr04-generation-report.jsonl
assets-src/image/manifests/phase4-uiux-pr04-processing-report.jsonl
```

禁止为每个关键词或每条说明单独生成装饰图。

### 7.2 音频

默认复用 `audio.boss.warning`、`audio.ui.*` 和既有战斗 cue。只有高风险预警在听觉层面仍无法分离时，才允许：

```text
assets-src/audio/specs/phase4-uiux-pr04-audio-plan.yaml
assets-src/audio/manifests/phase4-uiux-pr04-processing-report.jsonl
```

禁止 ambience、职业氛围音和大规模 variation bank。

### 7.3 约束

1. 新资源 plan 必须同步 `build.gradle.kts --extra-plan`。
2. 新 key 必须被 smoke/golden 消费。
3. 颜色、描边、节奏、badge 数字优先由 token/renderer 实现。
4. 若新增 image/audio plan 或 `--extra-plan` 接线，必须运行 `assetLint / styleLint / manifestLint / audioLint` 与 `./scripts/verify-bootstrap.sh`。
5. 未生成正式资源但使用 fallback 时，PR description 必须包含 [Resource Fallback Audit](resource-fallback-audit-template.md)。

## 8. 出口门禁

1. `StatusPresentationModel`、最小 `TelegraphPresentationModel` 与 `ExplainPaneModel` 已落地。
2. 状态 badge、telegraph/zone presentation group、DescriptionPresenter 消费入口已统一。
3. `KeywordRegistry` 仍是唯一 keyword authority，`keywordRegistryLint` 已接入并作为本 PR gate。
4. `AccessibilityToggle` 最小入口已落地，a11y 人工复核完成。
5. `DescriptionPresenter` 的 combat action 分支在本 PR 至少有 pure unit test；端到端覆盖登记到 PR-05。
6. smoke/golden/locale/contract/maintainability 已执行或明确说明无法执行原因。
7. 人工白盒记录包含状态、telegraph、a11y、动态说明、ExplainPane 证据。
8. 没有新增 status contract、第二 keyword registry、普通敌人 intent 或独立 explain mode。
9. 新增 locale key 已进入 `zh-CN/en-US`，并由 `localeLint` 覆盖。
