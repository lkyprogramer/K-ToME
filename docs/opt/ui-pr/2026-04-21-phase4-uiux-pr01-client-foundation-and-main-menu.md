> 执行前必须先完整阅读并接受：
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md`
> `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
> `docs/opt/ui-pr/README.md`

# Phase 4 UI/UX - PR-01 Client Foundation and Main Menu

**阶段**: `Phase 4 late-development / phase4-uiux-pr01`  
**优先级**: `P0`  
**合并来源**: 原计划 `PR-01 Token 与信息面骨架前置` + `PR-02 首页首屏优化`  
**前置条件**: 当前 `client` smoke / golden / `localeLint` / `contractLint` 基线可运行。  
**硬依赖条款**:

1. `docs/opt/ui-pr/README.md` 的 `phase4-uiux-pr01-*` golden label 所有权和人工白盒记录模板。
2. `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md` §5.3 / §5.4 中的 token、信息面骨架、错误复制 payload 与标题去开发态规则。
3. `DesktopLauncher` 当前只能作为启动入口，不能保存规则层状态；标题格式必须通过纯 formatter 生成。

**对应问题**: `client` 的 token、窗口标题、首页主路径和帮助信息高度耦合。若先只落 token，再单独开首页 PR，会重复改 `MainMenuScreen`、重复更新 golden，并让首页帮助区和输入语义反复回写。

---

## 1. 阶段目标

建立 client-local UI 基座，并把首页从开发入口改成正式开局面。

完成标准：

1. 新增 `UiDesignTokens`，统一颜色、字号、间距、焦点、透明度、圆角、描边和基础 motion token。
2. 新增 `InfoSurfaceLayout`，让当前 `TileRenderer` 的 `MapDominant` 信息面布局显式化。
3. `TileRenderer`、`TelegraphRenderer`、`StatusHudRenderer`、`MainMenuScreen`、`PlayerCreationPanel` 首批消费 token。
4. `DesktopLauncher` 窗口标题不再承载开发态操作说明。
5. `BuildInfo.shortHash` 与窗口标题 formatter 在本 PR 直接落地，不跨 PR 使用 `unknown` 占位作为长期接口。
6. 首页首焦点、快速开始、继续游戏、validation mode 次级入口、build 能力摘要、常驻帮助区全部冻结。
7. 无 save、可 continue、save 不可用三态都有自动化和人工白盒证据。

## 2. 当前问题

1. `TileRenderer.layoutMetrics(...)` 已经有地图、sidebar、底部 cards 雏形，但布局职责仍隐含在 renderer 常量里。
2. `TelegraphRenderer.fallbackColorHex(...)` 维护裸 hex danger 色，后续状态/telegraph 分层容易漂移。
3. `MainMenuScreen` 当前以固定坐标和 entry 列表组织首页，缺少 `MainMenuSummaryModel` 和 `MainMenuFocusPolicy`。
4. `MainMenuController.pollAction(continueEnabled)` 接收 continue 状态，但首焦点策略不是独立 contract。
5. `DesktopLauncher` 标题包含 `arrows/numpad move, Enter stairs, x inspect, Ctrl+S save`，正式窗口标题和开发态说明混在一起。

### 2.1 本 PR 必须冻结的口径

1. `UiDesignTokens` 只属于 `client`，不进入 `RenderSnapshot`、manifest、内容 schema 或 `core/game`。
2. `InfoSurfaceLayout` 是 `TileLayoutMetrics` 上游策略；本 PR只让 `MapDominant` 真正接管当前布局。
3. `WideSplit / ModalOverlay` 只能类型预留，不进入 production 分支。
4. 首页首焦点由 `MainMenuFocusPolicy` 决定，不在渲染时临时猜。
5. `继续游戏` 至少有 `AVAILABLE / ABSENT / UNAVAILABLE(reasonCode, reasonKey, copyPayload)` 三态。
6. `验证模式` 保留为开发入口，但不抢首焦点。
7. 窗口标题固定为 `K-ToME · <locale> · <seed>[· <save-slot>]`；信息不可得时逐段省略，最终回退为 `K-ToME`。

## 3. 范围与非目标

### 3.1 范围

1. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt`
   - `client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuSummaryModel.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuFocusPolicy.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/DesktopLauncherTitleFormatter.kt`
   - `client/src/main/kotlin/com/ktome/client/build/BuildInfo.kt`
2. 修改：
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/creation/PlayerCreationPanel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/status/StatusHudRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt`
   - `game/src/main/resources/i18n/zh-CN.json`
   - `game/src/main/resources/i18n/en-US.json`
   - `client/build.gradle.kts`
   - `build.gradle.kts`，仅当需要 root alias 或 processResources 接线时修改
   - `client/src/main/resources/build-info.properties` 或等价 generated resource 路径
3. 测试：
   - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
   - `client/src/test/kotlin/com/ktome/client/render/InfoSurfaceLayoutTest.kt`
   - `client/src/test/kotlin/com/ktome/client/screen/MainMenuControllerTest.kt`
   - `client/src/test/kotlin/com/ktome/client/screen/MainMenuFocusPolicyTest.kt`
   - `client/src/test/kotlin/com/ktome/client/screen/MainMenuScreenTextTest.kt`
   - `client/src/test/kotlin/com/ktome/client/screen/DesktopLauncherTitleFormatterTest.kt`
   - `client/src/test/kotlin/com/ktome/client/build/BuildInfoTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`
   - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`

本 PR 可拆成两阶段提交，但仍合成一个 PR 发起评审：

1. `foundation`: `UiDesignTokens`、`InfoSurfaceLayout`、`BuildInfo`、窗口标题 formatter、首批 renderer token 消费。
2. `main-menu`: `MainMenuSummaryModel`、`MainMenuFocusPolicy`、首页三态、continue 错误复制路径和 golden/smoke。

### 3.2 非目标

1. 不重做完整 HUD 或三栏布局。
2. 不实现 `ModalStack`、统一输入 truth table 或 Look Mode；这些属于 PR-02。
3. 不新增图片、音频或 manifest key。
4. 不新增 save schema 或兼容旧 save。
5. 不引入 tutorial、telemetry、profile/run history 页面。
6. 不承诺 OS 级 screen reader 集成。

## 4. 技术方案

### 4.1 `UiDesignTokens`

最小 token 面：

1. `color.text.primary / secondary / disabled`
2. `color.surface.base / raised / overlay`
3. `color.focus.ring`
4. `color.quality.normal / magic / rare`
5. `color.accent.special.unique / artifact`
6. `color.telegraph.low / moderate / high / lethal`
7. `color.status.buffAccent / debuffAccent / neutralAccent`
8. `color.status.badge.stack / badge.turns / badge.cap`
9. `color.menu.selection.focused / disabled / normal`
10. `spacing.xs / sm / md / lg / xl`
11. `typography.ui / body / caption / title`
12. `alpha.disabled / overlayDim / glass`
13. `radius.sm / md / lg`
14. `stroke.thin / medium / thick`
15. `motion.fastMs / mediumMs / slowMs`

实现约束：

1. 不写成 `Map<String, Any>` 动态袋子；用类型化字段保留可维护性。
2. 不读取 snapshot、manifest、locale 或 save。
3. 不为 token 新增全局可变主题状态。

### 4.2 `InfoSurfaceLayout`

建议结构：

```kotlin
sealed interface InfoSurfaceLayout {
    data object MapDominant : InfoSurfaceLayout
    data object WideSplit : InfoSurfaceLayout
    data object ModalOverlay : InfoSurfaceLayout
}
```

`MapDominant` 要求：

1. 包装当前 `TileRenderer.layoutMetrics(...)` 的计算规则。
2. 保留 `TileLayoutMetrics` 作为 renderer 最终消费模型。
3. 将 `sidebarGap / panelGap / bottomInset / cardHeight / focusWidth` 等核心数值改为 token 或 strategy 派生。
4. 保证现有 `1280x800` 基线不爆版。
5. 最小支持窗口尺寸为 `1024x768`；小于该尺寸时允许后续 PR 引入 `ModalOverlay` fallback 并输出 warn，本 PR 不实现小窗 fallback。

### 4.3 首页模型

建议新增：

```kotlin
data class MainMenuSummaryModel(
    val primaryAction: MainMenuPrimaryAction,
    val continueAvailability: ContinueAvailability,
    val buildSummary: List<BuildCapabilityLine>,
    val helpLines: List<String>,
    val localeLabel: String,
)
```

`BuildCapabilityLine` 最小 shape：

```kotlin
data class BuildCapabilityLine(
    val labelKey: String,
    val valueTextKey: String? = null,
    val disabled: Boolean = false,
)
```

焦点策略：

1. 无 save：首焦点 `QuickStart`
2. 有可加载 save：首焦点 `Continue`
3. save 存在但不可加载：首焦点 `QuickStart`，`Continue` disabled
4. validation mode 可键盘到达，但不能默认聚焦

`ContinueUnavailableReasonCode` 固定为：

| reasonCode | reasonKey | 说明 |
| --- | --- | --- |
| `CORRUPTED` | `ui.menu.continue.unavailable.corrupted` | save 内容损坏或无法反序列化 |
| `VERSION_MISMATCH` | `ui.menu.continue.unavailable.version-mismatch` | save contract version 不兼容 |
| `IO_ERROR` | `ui.menu.continue.unavailable.io-error` | 文件不存在、权限或读取失败 |
| `SCHEMA_MISMATCH` | `ui.menu.continue.unavailable.schema-mismatch` | save 字段结构与当前 schema 不匹配 |
| `UNKNOWN` | `ui.menu.continue.unavailable.unknown` | 未分类异常；payload 必须追加 `throwableClass / throwableMessage` |

首页信息顺序：

1. 标题和 build 摘要
2. 主行动区：`继续游戏` / `快速开始`
3. 职业/种族选择摘要
4. 常驻帮助区
5. 次级入口：validation mode、语言/设置

### 4.4 `BuildInfo.shortHash`

`BuildInfo.shortHash` 在本 PR 前置落地，作为所有 `[ktome/<build-hash>]` 的唯一来源。

实现要求：

1. `processResources` 或等价构建步骤注入 `git rev-parse --short HEAD` 结果。
2. 注入失败时回退为 `unknown`，并在 client 启动日志写 warn：`BuildInfo.shortHash resolution failed, fell back to 'unknown'`。
3. 测试不得把 `unknown` 当成唯一稳定期望；只断言字段存在、格式合法和失败回退可观测。
4. 后续 PR 只能消费 `BuildInfo.shortHash`，不得再自建 hash reader。

测试口径：

1. `BuildInfoTest` 断言 `BuildInfo.shortHash` 满足 `unknown|[0-9a-f]{7,40}`。
2. 回退路径必须能在 `TestLogCollector` 或等价日志收集器中观察到 `WARN`，message 包含 `BuildInfo.shortHash resolution failed`。

### 4.5 错误复制路径

save 不可用时必须提供：

1. 玩家可读摘要：`ui.menu.continue.corrupted.detail`
2. `Copy Error Detail` payload：

```text
<heading>
<detail>
savePath: <path>
reasonCode: <ContinueUnavailableReasonCode>
gameVersion: <version>
[ktome/<build-hash>]
```

payload 的 `build-hash` 必须来自 `BuildInfo.shortHash`。

payload 采用与 PR-03 `UiErrorPayload.contextKeyValuePairs` 一致的形态，`savePath / reasonCode / gameVersion` 是前三条 context，顺序固定为：

1. `savePath`
2. `reasonCode`
3. `gameVersion`

当 `reasonCode = UNKNOWN` 时，在末尾追加：

4. `throwableClass`
5. `throwableMessage`

### 4.6 窗口标题更新入口

建议新增纯 formatter：

```kotlin
data class DesktopLauncherTitleContext(
    val localeId: String? = null,
    val seed: Long? = null,
    val saveSlot: String? = null,
)
```

格式规则：

1. `localeId / seed / saveSlot` 都存在：`K-ToME · <locale> · <seed> · <save-slot>`
2. `saveSlot` 不存在：`K-ToME · <locale> · <seed>`
3. `seed` 不存在：`K-ToME · <locale>`
4. 全部不可得：`K-ToME`
5. 更新调用点优先放在 session 加载完成后的 client screen 边界，例如 `FoundationGameScreen` 初始化或 `GameApp` 切屏回调；不要从规则层反向驱动窗口。
6. `DesktopLauncherTitleFormatter` 只提供纯 formatter；LWJGL title 切换由 `GameApp` 在 `render()` 入口或既有 UI 主线程边界消费 formatter 结果，不从后台线程直接调用 title setter。

### 4.7 新增 locale key 列表

| key | 所属面 | 示例文本 |
| --- | --- | --- |
| `ui.menu.action.quick-start` | 首页主按钮 | `快速开始` |
| `ui.menu.action.continue` | 首页主按钮 | `继续游戏` |
| `ui.menu.action.validation` | 首页次级入口 | `验证模式` |
| `ui.menu.continue.unavailable.corrupted` | continue disabled reason | `存档已损坏，无法继续。` |
| `ui.menu.continue.unavailable.version-mismatch` | continue disabled reason | `存档版本不兼容，需要新开局。` |
| `ui.menu.continue.unavailable.io-error` | continue disabled reason | `读取存档失败。` |
| `ui.menu.continue.unavailable.schema-mismatch` | continue disabled reason | `存档结构与当前版本不匹配。` |
| `ui.menu.continue.unavailable.unknown` | continue disabled reason | `存档暂时不可用。` |
| `ui.menu.help.primary-keys` | 首页帮助区 | `方向键移动，Enter 确认，? 查看帮助。` |

## 5. 推荐改动面

### 5.1 `client/render`

1. `TileRenderer.layoutMetrics(...)` 改由 `InfoSurfaceLayout.MapDominant` 生成。
2. `TileRenderer.tone(...)`、`statusAccentColor(...)`、`statusBadgeColor(...)` 消费 token。
3. `TelegraphRenderer.fallbackColorHex(...)` 的 danger 色改为 token 映射。
4. `TileRendererCanvasTest` 增加 token/layout smoke：sidebar、bottom panels、focus ring、不越界。

### 5.2 `client/screen`

1. `MainMenuController.entries(...)` 返回 entry model，包含 enabled、focusable、disabledReason。
2. `MainMenuAction` 明确区分 `QuickStart`、`Continue`、`ValidationMode`。
3. `MainMenuScreen.textSnapshot()` 暴露 build summary、help lines、continue disabled reason。
4. `MainMenuScreen` 消费 token，禁止自带第二套颜色。

### 5.3 `client/ui`

1. `PlayerCreationPanel` 的可玩、锁定、不可用、focus 状态消费 token。
2. `StatusHudRenderer` 保持现有语义，只把颜色/徽标入口收敛到 token。

首批迁移清单：

| 当前使用点 | 目标 token |
| --- | --- |
| `TileRenderer.statusAccentColor(BUFF)` | `UiDesignTokens.color.status.buffAccent` 或等价 status token |
| `TileRenderer.statusAccentColor(DEBUFF)` | `UiDesignTokens.color.status.debuffAccent` |
| `TileRenderer.statusAccentColor(NEUTRAL)` | `UiDesignTokens.color.status.neutralAccent` |
| `TileRenderer.statusBadgeColor(...)` | `UiDesignTokens.color.status.badge.*` |
| `TelegraphRenderer.fallbackColorHex(low/moderate/high/lethal)` | `UiDesignTokens.color.telegraph.*` |
| `MainMenuScreen.selectionStateColor(...)` | `UiDesignTokens.color.menu.selection.*` |
| `PlayerCreationPanel` disabled/focus 状态 | `UiDesignTokens.alpha.disabled` / `UiDesignTokens.color.focus.ring` |

### 5.4 `DesktopLauncher`

1. `setTitle(...)` 删除操作说明。
2. 开发态键位说明只能出现在首页帮助区、`PR-02` 冻结的 `?` 帮助 overlay 或 validation 面。
3. 标题更新只调用 `DesktopLauncherTitleFormatter`，不在 launcher 内拼接临时字符串。

## 6. 测试与自证

### 6.1 必测行为

1. 首页、角色创建、局内地图、状态 HUD、telegraph 可见场景仍可渲染。
2. 无 save 时首焦点为 `快速开始`；有可用 save 时首焦点为 `继续游戏`。
3. save 不可用时 `继续游戏` disabled，且不可提交。
4. validation mode 仍可达，但不是主行动。
5. 窗口标题不包含键位说明。
6. token 没有反向进入 `core/game`。
7. `1024x768` 最小窗口下首页三态和局内 MapDominant 不重叠、不爆版。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint :client:test --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.screen.MainMenuControllerTest" --tests "com.ktome.client.screen.MainMenuScreenTextTest" --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.render.InfoSurfaceLayoutTest"
./gradlew :client:test --tests "com.ktome.client.screen.MainMenuFocusPolicyTest" --tests "com.ktome.client.screen.DesktopLauncherTitleFormatterTest" --tests "com.ktome.client.build.BuildInfoTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew maintainabilityLint
./gradlew clientSmoke goldenScreenshot verifyChanged
```

若修改 `client/build.gradle.kts`、`build.gradle.kts`、`processResources`、generated resource 或 bootstrap/dependency wiring，必须补：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./scripts/verify-bootstrap.sh
```

### 6.3 人工白盒验证流程

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. 记录文件建议：`docs/opt/ui-pr/manual-records/phase4-uiux-pr01-foundation-menu.md`

流程：

1. 启动客户端：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run
```

2. 无 save 首启检查：
   - 首焦点落在 `快速开始`
   - `Enter` 直接进入新局
   - 帮助区不遮挡主按钮
   - OS 窗口标题不包含操作说明
3. 有 save 检查：
   - 开一局并保存
   - 重启客户端
   - 首焦点切到 `继续游戏`
   - `Enter` 进入保存局
4. 不可用 save 检查：
   - 用 fixture 或手动破坏 save 版本/内容
   - `继续游戏` disabled
   - 键盘导航不会卡死在 disabled entry
   - `Copy Error Detail` payload 顺序和字段符合文档
5. 局内 token/layout 检查：
   - 进入地图态，确认地图、sidebar、底部 info/log/focus/action 面没有重叠
   - 制造 telegraph 或进入 boss warning 场景，确认 danger 色和 HUD 状态色不冲突
   - 检查角色创建可玩/锁定/不可用状态不只靠文字区分
6. `1024x768` 快速复核：
   - 将窗口调整到 `1024x768`
   - 重复首页三态焦点检查
   - 进入地图态，确认 map、sidebar、bottom cards 不重叠，主要文本不爆版
7. 保存证据：
   - `phase4-uiux-pr01-main-menu-first-run`
   - `phase4-uiux-pr01-main-menu-continue`
   - `phase4-uiux-pr01-main-menu-corrupted-save`
   - `phase4-uiux-pr01-map-dominant-layout`
   - `phase4-uiux-pr01-telegraph-token`

通过标准：

1. 首页三态都可键盘完成。
2. 视觉层级没有明显跳变。
3. 任一主要文本不爆版。
4. 人工记录包含窗口标题实际值。
5. `1024x768` 复核有截图或输入记录。

### 6.4 统一验证框架关系

本 PR 不新增 Phase 4 white-box domain。`GoldenScreenshotHarnessTest` 是像素稳定证据，但不能替代人工首焦点、窗口标题和 disabled 行为验证。若 golden 因 LWJGL backend unavailable 被跳过，必须保留人工截图或输入记录。

## 7. 资源生成计划

### 7.1 图片

默认不新增 image plan。禁止为 token、focus ring、首页背景、panel chrome 生成静态图片。

只有当首页主入口缺少稳定识别点且现有 token/文本/fallback icon 不足时，才允许后续补极小 companion plan；本 PR 默认不做。

### 7.2 音频

默认复用现有 `audio.ui.*`。不新增首页 confirm/cancel/help cue，除非实现过程中证明现有 cue 无法支撑 disabled/confirm 区分。

### 7.3 约束

1. 不改 image/audio manifest。
2. 不新增 runtime raw asset path。
3. 不把资源缺口作为 PR 阻塞，除非正式 UI 路径已经依赖该 key。

## 8. 出口门禁

1. `UiDesignTokens`、`InfoSurfaceLayout.MapDominant`、`MainMenuSummaryModel`、`MainMenuFocusPolicy` 已落地。
2. 首批 client UI 面消费 token，且 `TelegraphRenderer` danger 色不再自管裸 hex。
3. 首页无 save、可 continue、不可 continue 三态有自动化与人工证据。
4. `localeLint`、`contractLint`、命中的 client tests、smoke/golden 已执行或明确说明无法执行原因。
5. `BuildInfo.shortHash` 与 `DesktopLauncherTitleFormatter` 已有单测，注入失败回退可观测。
6. 人工白盒记录包含首页三态、窗口标题、地图布局、telegraph token 证据。
7. 没有新增资源 key、manifest、snapshot 字段、save schema、telemetry 或规则层依赖。
8. 新增 locale key 已进入 `zh-CN/en-US`，并由 `localeLint` 覆盖。
