> 执行前必须先完整阅读并接受：
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md`
> `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
> `docs/opt/ui-pr/README.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md`

# Phase 4 UI/UX - PR-03 Item, Content Presentation, UI States

**阶段**: `Phase 4 late-development / phase4-uiux-pr03`  
**优先级**: `P0`  
**合并来源**: 原计划 `PR-04 图标全链路 + 品质 + 地面掉落提示` + `PR-05 内容扩张一致性、共享卡片模型与错误/空态`  
**前置条件**: `PR-01` token 已冻结，`PR-02` Look Mode / modal owner 已冻结。  
**硬依赖条款**:

1. `PR-01` 已落地 `BuildInfo.shortHash`；本 PR 只消费，不再新增第二 hash 来源。
2. `PR-02` 允许 `ui.inspect.empty.tile` 临时表达 Look Mode 空态；本 PR 必须迁移到 `UiEmptyState`，并删除该临时 key 的 `zh-CN/en-US` 条目。
3. `PR-02` 的 `ITEM_COMPARE` deferred frame 在本 PR 收口前必须继续保留占栈测试。
4. `ValidationPreset.LOOT_LAB` 当前已存在于 `com.ktome.game.validation.ValidationPreset` 枚举合同中，本 PR 复用 `seed=20260413`，不新建第二 preset。

**对应问题**: item icon、品质、地面掉落、事件/商店/奖励卡片、错误态、空态、加载态都共享同一条 content-ui 可读性链路。如果拆成两个 PR，会重复改 item display、visual manifest、contract lint、smoke/golden 和人工验证 fixture。

---

## 1. 阶段目标

把内容表现从“局部文字列表”推进到“icon/quality/card/state 都有正式合同和 gate”。

完成标准：

1. 所有官方可装备物和 special template item 都有可解析 `iconKey`。
2. `QualityPresentation` 区分 `RarityTier.NORMAL / MAGIC / RARE` 与 `SpecialTier.UNIQUE / ARTIFACT` 两条轴。
3. 地图态显示地面掉落 marker：单件 icon，多件 head icon + 数量 badge，数量上限 `9+`。
4. 事件、商店、奖励房共享 `ModalCardModel`。
5. 空态、错误态、加载态正式化，并冻结 `UiErrorPayload` 复制格式。
6. `UiErrorPayload` 消费 `PR-01` 的 `BuildInfo.shortHash`。
7. `contractLint` / `localeLint` 优先承接 icon、locale、卡片必填项校验；只有现有 owner 无法表达跨模块校验时，才新增 `contentUiLint`。

## 2. 当前问题

1. `ItemRenderSnapshot.iconKey` 当前 nullable，运行时可能退回纯文本。
2. `qualityTierId`、`qualityNameKey` 已存在，但 UI 未冻结名色、corner glyph、special accent 合成规则。
3. `MapCellSnapshot.items` 已表达同格掉落件数，但世界面没有 marker。
4. 事件/商店/奖励房目前容易各自拼一套展示结构。
5. 空背包、空商店、无可检视目标、空日志、manifest/save/snapshot 错误缺少统一状态模型。
6. 错误复制 payload 的 context 输出顺序若不冻结，会导致测试和 issue report 不稳定。

### 2.1 本 PR 必须冻结的口径

1. 当前正式品质合同只有 `NORMAL / MAGIC / RARE` 与 `UNIQUE / ARTIFACT`，不新增档位。
2. `RarityTier` 是主色和 corner glyph；`SpecialTier` 是叠加 accent，不改变 rarity 主色。
3. `MapCellSnapshot.items.size` 只代表同格掉落件数，不代表 item stack quantity。
4. `ItemRenderSnapshot.iconKey` 继续 nullable；完整性通过 lint + content coverage 收口。
5. `ModalCardModel` 是事件/商店/奖励房共享模型；PR-04 的 `ExplainPane` 必须复用它，不自造 inspect-only 卡片。
6. 错误态必须给 `Retry / Back To Menu / Copy Error Detail` 返回路径。
7. 加载态只允许短时过渡，不得形成长期输入遮罩。
8. 当前仓库真源中 `ItemRenderSnapshot` 没有 `specialTier`；本 PR 为完整 `UNIQUE / ARTIFACT` 轴显式扩 `ItemRenderSnapshot.specialTierId`，禁止 client 通过 `specialTemplateId` 反查 content template。

## 3. 范围与非目标

### 3.1 范围

1. 修改：
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/render/RoutePreviewText.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
   - `client/src/main/kotlin/com/ktome/client/assets/RenderSnapshotAssetAudit.kt`
   - `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
   - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
   - `client/src/main/resources/manifests/visual-manifest.json`
   - `game/src/main/resources/data/items/index.yaml`
   - `game/src/main/resources/i18n/*`
   - `build.gradle.kts`
   - `docs/2026-03-13-core-systems-design-and-phase-supplements.md`，记录 `ItemRenderSnapshot.specialTierId` contract 变更
   - `docs/phase4/2026-03-13-phase4-verification-checklist.md`，同步 snapshot/contract owner 验证入口
2. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/item/QualityPresentation.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/item/GroundLootMarkerModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/state/UiEmptyState.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/state/UiErrorState.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/state/UiLoadingState.kt`
   - `tools` 侧 `ItemIconKeyCoverageRule`
   - 若 §4.5 判定现有 owner 无法表达，再新增 `tools` 侧 `ContentUiLintRule`
3. 资源：
   - `assets-src/image/specs/phase4-uiux-pr03-gemini-plan.yaml`
   - `assets-src/image/manifests/phase4-uiux-pr03-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-uiux-pr03-processing-report.jsonl`
   - 可选 `assets-src/audio/specs/phase4-uiux-pr03-audio-plan.yaml`
   - 可选 `assets-src/audio/manifests/phase4-uiux-pr03-audio-generation-report.jsonl`
   - 可选 `assets-src/audio/manifests/phase4-uiux-pr03-audio-processing-report.jsonl`
4. 测试：
   - `client/src/test/kotlin/com/ktome/client/ui/item/QualityPresentationTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/item/GroundLootMarkerModelTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/card/ModalCardModelTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/state/UiErrorPayloadTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/state/UiLoadingStateTest.kt`
   - `core/src/test/kotlin/com/ktome/core/snapshot/RenderSnapshotSerializationTest.kt`
   - `core/src/test/kotlin/com/ktome/core/snapshot/RenderSnapshotHasherTest.kt`
   - `game/src/test/kotlin/com/ktome/game/RenderSnapshotContractTest.kt`
   - `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
   - `tools/src/test/kotlin/com/ktome/tools/lint/ItemIconKeyCoverageRuleTest.kt`

### 3.2 非目标

1. 不新增真实 stack quantity contract。
2. 不新增额外品质档位。
3. 不实现 advanced tooltip。
4. 不为每个事件/商店/奖励房单独生成插画。
5. 不新增用户行为 telemetry。
6. 不把错误态变成吞异常的 default-success fallback。

## 4. 技术方案

### 4.1 `QualityPresentation`

字段前提：

1. snapshot 或等价 presentation 输入必须提供 `qualityTierId: String(NORMAL/MAGIC/RARE)`。
2. special 轴必须由 `specialTierId: String?(UNIQUE/ARTIFACT)` 与 `specialTemplateId: String?` 判断。
3. 本 PR 显式新增 `ItemRenderSnapshot.specialTierId`，并同步 `FoundationGameSession` snapshot 构建、contract lint、render snapshot tests；禁止 client 查询 template 自行推断 special tier。
4. 该字段是 presentation-only snapshot 字段，不进入 save schema；若实现发现需要持久化，必须先回到上游 phase4 contract 文档修订。

映射：

| 输入 | 主色 | corner glyph | special accent |
| --- | --- | --- | --- |
| `NORMAL` | `color.quality.normal` | 无 | 无 |
| `MAGIC` | `color.quality.magic` | `◆` | 无 |
| `RARE` | `color.quality.rare` | `◆◆` | 无 |
| `specialTemplateId != null && specialTierId == UNIQUE` | rarity 主色 | rarity glyph | `color.accent.special.unique` |
| `specialTemplateId != null && specialTierId == ARTIFACT` | rarity 主色 | rarity glyph | `color.accent.special.artifact` |
| `specialTemplateId != null && specialTierId == null` | N/A | N/A | fail fast，不渲染 accent |
| `specialTemplateId == null && specialTierId != null` | N/A | N/A | fail fast，不渲染 accent |

禁止把 `RARE + UNIQUE` 渲染成第四个 rarity 主色。

### 4.2 `GroundLootMarkerModel`

head item 排序：

1. `specialTemplateId != null`
2. `qualityTierId`: `RARE > MAGIC > NORMAL`
3. `iconKey` 字典序

设计意图：special item 对玩家判断的优先级高于普通 rarity，且 special accent 在地图 marker 上更需要被看见；若后续 UX 反馈 rarity 应先于 special，再单独调整排序合同。

`qualityTierId` 是 snapshot 字符串，client-local 排序权重固定为：

| `qualityTierId` | 权重 |
| --- | --- |
| `NORMAL` / `normal` | `0` |
| `MAGIC` / `magic` | `1` |
| `RARE` / `rare` | `2` |
| unknown | defensive `-1` 仅用于避免排序崩溃，同时必须产生 contract error artifact 并使 owner lint/test fail fast |

unknown `qualityTierId` 是 snapshot/contract 违法，不是资源 fallback。`QualityPresentationTest`、`ItemIconKeyCoverageRuleTest` 或等价 `contractLint` 必须覆盖 unknown quality fail-fast；renderer 不得把它静默显示成普通物品。

绘制：

1. 单件显示 item icon。
2. 多件显示 head icon + count badge。
3. `items.size >= 10` 显示 `9+`。
4. 玩家站在掉落格上时，用 actor 上方或右上角 corner badge，不把完整 icon 压在 actor 正中。

### 4.3 `ModalCardModel`

建议字段：

```kotlin
data class ModalCardModel(
    val stableKey: String,
    val title: RenderTextTokenSnapshot,
    val iconKey: String?,
    val summary: RenderTextTokenSnapshot?,
    val detailLines: List<RenderTextTokenSnapshot>,
    val costLines: List<RenderTextTokenSnapshot> = emptyList(),
    val rewardLines: List<RenderTextTokenSnapshot> = emptyList(),
    val disabledReason: RenderTextTokenSnapshot? = null,
    val primaryAction: ModalCardAction,
    val secondaryAction: ModalCardAction,
)
```

`ModalCardAction` 是 client-local sealed enum，最小值集：

1. `Confirm`
2. `Cancel`
3. `Buy`
4. `Sell`
5. `EnterRoute`
6. `ReadMore`
7. `Close`

使用面：

1. route / reward preview
2. shop offer / sell entry
3. event/reward room
4. PR-04 `ExplainPane`

action 语义：

| action | 触发键 | 适用面 | 副作用 |
| --- | --- | --- | --- |
| `Confirm` | `Enter / Space` | 通用确认 | 执行当前卡片默认动作 |
| `Cancel` | `ESC / Backspace` | 任意可取消卡片、loading cancel | 取消当前流程或关闭卡片 |
| `Buy` | `Enter / Space` | shop offer | 执行购买；cost 不足时 disabled 并显示原因 |
| `Sell` | `Enter / Space` | shop sell entry | 执行出售；不可出售时 disabled 并显示原因 |
| `EnterRoute` | `Enter / Space` | route / reward preview | 确认进入路线；不同于普通 `Confirm`，必须写入 route 选择 |
| `ReadMore` | `? / Enter` | event、reward、ExplainPane | 打开说明或 expand 内容；不得替代主确认 |
| `Close` | `ESC / Backspace` | 只读卡片或错误态 | 关闭当前卡片；若无其他 action，必须保证仍有返回路径 |

### 4.4 `UiErrorPayload`

复制格式固定：

```text
<heading>
<detail>
<key>: <value>
<key>: <value>
[ktome/<build-hash>]
```

字段：

1. `heading`: 当前 locale 已解析文本
2. `detail`: 当前 locale 已解析文本
3. `contextKeyValuePairs`: 英文 debug key + 稳定值，按 builder 插入顺序输出
4. `build-hash`: `PR-01` 的 `BuildInfo.shortHash`

测试必须用固定插入序断言 payload，禁止改成 map 字典序导致跨 JVM / 序列化实现漂移。

PR-01 `Copy Error Detail` 的前三条 context 顺序必须保持为 `savePath -> reasonCode -> gameVersion`。后续 PR 只能 append 新 context 到末尾，不得在中间插入或重排既有 key。

### 4.5 空态 / 加载态 / 错误态模型

最小 shape：

```kotlin
data class UiEmptyState(
    val title: RenderTextTokenSnapshot,
    val detail: RenderTextTokenSnapshot,
    val primaryCta: ModalCardAction? = null,
)

data class UiLoadingState(
    val message: RenderTextTokenSnapshot,
    val showsSpinner: Boolean,
    val allowsCancel: Boolean,
)

data class UiErrorState(
    val heading: RenderTextTokenSnapshot,
    val detail: RenderTextTokenSnapshot,
    val actions: List<ModalCardAction>,
    val payload: UiErrorPayload,
)
```

加载态预算：

1. 默认参考一次可见循环 `~200ms`。
2. `ClientSmokeHarnessTest` 至少增加 `loading screen transitions within 500ms` 软断言。
3. 超过预算时必须允许取消或显示明确状态，不允许长期遮罩吞输入。

软断言定义：记录到 `build/reports/client-smoke/loading-timing.jsonl`，不直接 fail test；PR description 必须附本地测量值。若后续连续 3 次 CI 记录超过 `500ms`，再单独把该检查升级为 hard fail。

`allowsCancel == true` 时必须配套 `ModalCardAction.Cancel` 与 locale key `ui.loading.cancel`。

### 4.6 lint / checklist

优先扩现有 `contractLint / localeLint` owner 面；只有跨模块校验无法表达时才新增 `contentUiLint`。

阻塞规则：

1. 正式 item/special template 缺 `iconKey` 或 icon 不可解析。
2. 新 UI 文案缺 locale。
3. `ModalCardModel.title / primaryAction` 是 empty token，或只剩 `Close / Cancel` 且没有有效返回路径。
4. 错误/空态没有返回路径或下一步引导。
5. 新资源 key 未被 smoke/golden 消费。
6. `qualityTierId` 不属于 `NORMAL / MAGIC / RARE`，或 special 双字段不变量失效：`specialTemplateId != null` requires `specialTierId in UNIQUE/ARTIFACT`，`specialTierId != null` requires `specialTemplateId != null`。

### 4.7 新增 locale key 列表

| key | 所属面 | 示例文本 |
| --- | --- | --- |
| `ui.empty.inventory.title` | empty state | `背包为空。` |
| `ui.empty.inventory.detail` | empty state | `探索、战斗或商店中获得物品后会显示在这里。` |
| `ui.empty.shop.title` | empty state | `商店暂无可购买物品。` |
| `ui.empty.shop.detail` | empty state | `换一条路线或稍后再来。` |
| `ui.empty.inspect.title` | empty state | `这里没有可检视目标。` |
| `ui.empty.inspect.detail` | empty state | `移动光标到敌人、地形、掉落物或预警格查看详情。` |
| `ui.empty.log.title` | empty state | `暂无新的日志。` |
| `ui.empty.log.detail` | empty state | `行动、战斗和事件记录会显示在这里。` |
| `ui.loading.generic` | loading state | `正在加载...` |
| `ui.loading.cancel` | loading action | `取消` |
| `ui.error.action.retry` | error action | `重试` |
| `ui.error.action.back-to-menu` | error action | `返回主菜单` |
| `ui.error.action.copy-detail` | error action | `复制错误详情` |

允许实现时补充更细粒度 `detail` key，但 title/action key 必须进入 `zh-CN` 与 `en-US`，并由 `localeLint` 覆盖。

## 5. 推荐改动面

### 5.1 `client/render`

1. 从 `MapCellSnapshot.items` 构建 `GroundLootMarkerModel`。
2. 背包、商店、地面检视统一消费 `QualityPresentation`。
3. `RoutePreviewText` 或其替代模型输出 `ModalCardModel`。
4. 空态走 `UiEmptyState`，不直接写 `Empty`。

### 5.2 `client/screen`

1. `FoundationGameScreen` 接入 `UiErrorState` 和 `UiLoadingState`。
2. `Copy Error Detail` 使用唯一 `UiErrorPayload` builder。
3. loading 不得长期吞输入；超过一次明显交互节拍必须可取消或给出状态。

### 5.3 `client/assets`

1. `RenderSnapshotAssetAudit` 增加 item icon 和 card icon audit。
2. 缺失时定位到 `baseItemId / specialTemplateId / stableKey / iconKey`。

### 5.4 `game data / tools`

1. 补齐官方可装备物和 special template item `iconKey`。
2. 新增 `ItemIconKeyCoverageRule` 或等价 lint，并覆盖 `iconKey` 不能在 `visual-manifest.json` 中解析时 error。
3. 若新增资源 plan，root `build.gradle.kts` 必须接 `--extra-plan`。

## 6. 测试与自证

### 6.1 必测行为

1. 正式可装备物和 special template item 的 `iconKey` 非空且可解析。
2. `NORMAL / MAGIC / RARE` 与 special accent 可观察。
3. 单件、多件、`9+`、玩家站立掉落格都可见。
4. 事件/商店/奖励房共享卡片结构。
5. 空背包、空商店、无可检视目标、空日志都有下一步引导。
6. manifest/save/snapshot 错误态有 `Retry / Back To Menu / Copy Error Detail`。
7. `UiErrorPayload` 包含 `[ktome/<build-hash>]`，context 行顺序稳定。
8. 加载态可在 smoke 中证明不会长期遮罩。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint styleLint manifestLint
./gradlew :core:test
./gradlew :core:test --tests "com.ktome.core.snapshot.RenderSnapshotSerializationTest" --tests "com.ktome.core.snapshot.RenderSnapshotHasherTest"
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.render.RoutePreviewTextTest" --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.ui.item.QualityPresentationTest" --tests "com.ktome.client.ui.item.GroundLootMarkerModelTest" --tests "com.ktome.client.ui.card.ModalCardModelTest" --tests "com.ktome.client.ui.state.UiErrorPayloadTest" --tests "com.ktome.client.ui.state.UiLoadingStateTest"
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:test --tests "com.ktome.game.RenderSnapshotContractTest"
./gradlew :tools:test --tests "com.ktome.tools.lint.ItemIconKeyCoverageRuleTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew clientSmoke goldenScreenshot verifyChanged
```

若新增/修改 `build.gradle.kts` 或资源脚本 wiring：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./scripts/verify-bootstrap.sh
```

本 PR scope 已满足 `README.md` 的 maintainability hard trigger，必须执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew maintainabilityLint
```

若新增 `ContentUiLintRule`，必须在同一 PR 中写清：

1. owner task：优先扩 `contractLint`；只有现有 owner 无法表达跨模块 presentation 校验时才新增 `contentUiLint`。
2. root alias：新增 task 必须暴露 root `./gradlew contentUiLint` 或并入 `./gradlew contractLint`。
3. impact routing：同步 `VerificationTaskRegistry`、`VerifyChangedBuildContractTest` 与 `ScopeCoverageLintTest`，确保 `verifyChanged` 能覆盖。
4. rule/test class：文档和 PR description 必须列出具体 rule class 与 test selector。

### 6.3 人工白盒验证流程

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. validation preset：`LOOT_LAB` seed `20260413`
4. 记录文件建议：`docs/opt/ui-pr/manual-records/phase4-uiux-pr03-item-content-states.md`

流程：

1. 启动：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run
```

2. 地面掉落：
   - 制造单件 `NORMAL / MAGIC / RARE`
   - 制造 special item
   - 制造同格 `2~9` 和 `>=10`
   - 站到掉落格，确认 marker 仍可见
3. 三面一致性：
   - 背包、商店、Look Mode 地面检视对同一 item 的 icon、品质名色、corner glyph、special accent 一致
4. 共享卡片：
   - 进入事件房、商店、奖励房
   - 检查标题、icon、说明、收益/代价、禁用原因、确认/取消语义一致
5. 空态：
   - 空背包、空商店、无可检视目标、空日志
   - 检查有下一步引导，不只显示 `Empty`
6. 错误态：
   - 模拟 manifest 版本不匹配、save 恢复失败、snapshot 不完整
   - 检查 `Retry / Back To Menu / Copy Error Detail`
   - 复制 payload，核对 heading/detail/context/build hash 顺序
7. 加载态：
   - 进入大 modal 或资源 bootstrap
   - 确认没有长期遮罩或输入吞噬
8. 保存证据：
   - `phase4-uiux-pr03-ground-loot-single`
   - `phase4-uiux-pr03-ground-loot-stack`
   - `phase4-uiux-pr03-quality-variants`
   - `phase4-uiux-pr03-card-shared`
   - `phase4-uiux-pr03-empty-state`
   - `phase4-uiux-pr03-error-copy-detail`

通过标准：

1. 玩家无需打开 sidebar 就能知道地图格有掉落。
2. 内容卡片不再各写一套表现语言。
3. 错误/空/加载态都有明确返回路径。

### 6.4 统一验证框架关系

本 PR 不新增 Phase 4 white-box domain，但资源和 UI 可见性必须进入 smoke/golden。若 golden 被跳过，必须保留人工截图；不能把 skipped golden 当视觉验收完成。

## 7. 资源生成计划

### 7.1 图片

计划文件：

```text
assets-src/image/specs/phase4-uiux-pr03-gemini-plan.yaml
assets-src/image/manifests/phase4-uiux-pr03-generation-report.jsonl
assets-src/image/manifests/phase4-uiux-pr03-processing-report.jsonl
```

允许范围：

1. 当前正式可装备物缺失 item icon
2. special template item 正式 icon fallback
3. 地面掉落 marker 缺口
4. 共享卡片头部 icon、空态/错误态 fallback icon、必要商店/奖励房标识

禁止范围：

1. 大幅插画背景
2. 每个事件单独图
3. 品质档位装饰图

### 7.2 音频

默认复用现有 `audio.ui.* / audio.item.*`。只有当前 cue 无法区分 `购买成功 / 购买失败 / 打开卡片 / 关键错误 / high-value pickup` 时才新增：

```text
assets-src/audio/specs/phase4-uiux-pr03-audio-plan.yaml
assets-src/audio/manifests/phase4-uiux-pr03-audio-generation-report.jsonl
assets-src/audio/manifests/phase4-uiux-pr03-audio-processing-report.jsonl
```

### 7.3 约束

1. 新 plan 必须同步 `build.gradle.kts --extra-plan`。
2. 必须运行 `python3 scripts/sync_phase2_manifests.py`。
3. 新资源 key 必须被 smoke/golden 消费。
4. 未生成正式资源时，PR description 必须包含 [Resource Fallback Audit](resource-fallback-audit-template.md)。

## 8. 出口门禁

1. `QualityPresentation`、`GroundLootMarkerModel`、`ModalCardModel`、`UiEmptyState`、`UiErrorState`、`UiLoadingState` 已落地，并消费 `PR-01` 的 `BuildInfo.shortHash`。
2. item icon coverage / content UI lint 阻塞缺失和 icon 不可解析。
3. 地图、背包、商店、Look Mode 的 icon/品质表现一致。
4. image/audio plan、manifest 同步、`build.gradle.kts --extra-plan`、asset/style/audio/manifest lint 已按触发范围收口。
5. 人工白盒记录包含掉落、品质、共享卡片、空态、错误复制、加载态证据。
6. 除 `ItemRenderSnapshot.specialTierId` 外，没有新增品质档位、stack quantity、额外 snapshot 字段、telemetry 或第二规则真源。
7. `ui.inspect.empty.tile` 已从 `zh-CN/en-US` 删除，并由 locale deprecated-key 断言或等价 `localeLint` case 防止回归。
8. unknown `qualityTierId` 和非法 `specialTierId` 已由 contract lint/test fail fast，不走 Resource Fallback Audit。
9. snapshot serialization/hash/contract tests 已覆盖 `specialTierId` 的 presence、null、`specialTemplateWithoutTierFails`、`specialTierWithoutTemplateFails`。
