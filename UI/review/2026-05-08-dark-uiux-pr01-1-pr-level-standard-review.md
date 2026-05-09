# Dark UI/UX PR-01-1 PR 级 Review Standard Deep Review

目标文档：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查依据：`docs/review/rule/pr-level-review-standard.md`

审查范围：

- 上游入口：`UI/PLAN.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`
- 当前代码锚点：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`、`client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt`、`client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`、`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt`
- 本轮重点：按 PR 级规范重新检查 public contract freeze、state machine dry-run、implementation dry-run、test/gate/artifact、cross-PR consistency 和 removal/regression audit。

## Findings

### P0

无。

### P1

#### P1-1 overlay cursor 与 modal local cursor 仍是双 authority，`sourceKind` 与 `anchorTile` 可以来自不同来源

证据：

- PR 文档已经把 modal stack 单一来源收敛到 `OverlayState.modalFrames`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:121-142`。
- 但 current typed state 里仍有两套 cursor 字段：`OverlayState.targetingCursor / inspectCursor` 和 `ModalFrameLocalState.targetingCursor / inspectCursor`：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:58-79`、`client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:20-33`。
- PR 文档的 `sourceKind` 选择可能选中 modal candidate：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:183-189`。
- 但 `Focus Tile Resolution` 又始终先读 `overlayState.inspectCursor / targetingCursor`，再读 top-to-bottom modal local cursor：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:197-203`。

问题：

现在文档只解决了 `modalFrames` list 双来源，但没有解决 cursor 双来源。实现者按当前文字实现时，可能得到：

- `sourceKind = MODAL_COMBAT_DECISION`，但 `anchorTile` 来自 stale `overlayState.targetingCursor`；
- top modal local cursor 已更新，但 viewport / tooltip anchor 仍优先吃 overlay-level cursor；
- source diagnostics 和 actual anchor 不一致，manual record 里列出的 `TileViewportFocusSourceKind` 无法解释截图上的 cursor。

这违反 `docs/review/rule/pr-level-review-standard.md` 的 source-of-truth 要求：同一个 frame 的 source、focus tile、anchor validity 必须来自同一个被选中的 candidate，除非文档显式定义 consistency check。

修复方向：

把 candidate model 继续补完整：

```kotlin
data class TileViewportFocusCandidate(
    val mode: TileViewportFocusMode,
    val sourceKind: TileViewportFocusSourceKind,
    val cursor: Point?,
    val anchorKind: TileOverlayAnchorKind?,
)
```

然后写死规则：

1. 先按 mode/source priority 选中一个 candidate。
2. `sourceKind`、`resolvedFocusTile`、`anchorTile`、`tooltipAnchorKind`、`isTooltipAnchorValid` 都从同一个 selected candidate 派生。
3. 如果 implementation 仍保留 overlay-level cursor 与 modal local cursor 双写，必须在 projection request 构造处检查同一 active surface 的两者一致；不一致 fail fast。
4. 如果 top modal 是 `COMBAT_DECISION / TARGETING / INSPECT`，优先使用对应 modal local cursor；overlay-level cursor 只能服务无 spatial modal 的 overlay-level mode。

推荐测试：

- `TileViewportFocusProjectionTest.modalTargetingCursorWinsOverStaleOverlayCursor`
- `TileViewportFocusProjectionTest.sourceKindAndAnchorComeFromSameCandidate`
- `TileViewportFocusProjectionTest.rejectsDivergentOverlayAndModalCursorForSameSurface`

#### P1-2 `SpriteBatch.begin/end` owner 写到 `TileRenderer.renderToCanvas`，但当前 API 与影响范围无法实施，容易产生 double-begin 或测试路径失真

证据：

- PR 文档要求 `SpriteBatch.begin()` / `end()` 由 `TileRenderer.renderToCanvas` 在最外层一次性管理：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:226`。
- 当前运行时代码由 `FoundationGameScreen` 管理 begin/end，再调用 `renderer.render(...)`：`client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt:139-147`。
- 当前 `TileRenderer.renderToCanvas` 是 canvas/testing path，接收 `TileCanvas`，不是 `SpriteBatch` owner；runtime `render(batch, ...)` 才持有 batch：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:122-135`。
- 影响范围表只说 `FoundationGameScreen` 改 fixed viewport，没有明确迁移 batch lifecycle：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:275`。
- 文档又要求 pass order 用 `RecordingSpriteBatch` 验证：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:381-432`，但如果 `renderToCanvas` 仍是 `TileCanvas` path，`SpriteBatch.flush()` 边界无法在该 API 层证明。

问题：

开发者会有两种都“符合部分文字”的实现：

1. 保持 `FoundationGameScreen.batch.begin/end`，同时让 `TileRenderer.renderToCanvas` begin/end，导致 runtime double-begin。
2. 让 `renderToCanvas` 管理 batch，但测试仍走 `RecordingTileCanvas`，导致 layer flush / pass order 测试证明不了真实 SpriteBatch 路径。

这是会直接引发运行时渲染错误的 implementation contract 缺口。

修复方向：

必须二选一冻结 batch lifecycle owner：

推荐方案：

```text
FoundationGameScreen owns exactly one SpriteBatch.begin/end around TileRenderer.render(...).
TileRenderer.render / renderer passes never call begin/end.
TileRenderer.renderToCanvas remains test-only TileCanvas path and never owns SpriteBatch.
Layer boundaries are expressed as TileCanvas.flushLayer(reason) or equivalent testable command, and GdxTileCanvas maps it to batch.flush().
```

如果坚持让 `TileRenderer` owns begin/end，则 PR 文档必须同步：

1. 把 `FoundationGameScreen` 影响范围改成“删除 runtime begin/end ownership”。
2. 新增 test：`FoundationGameScreenDoesNotBeginBatchAroundTileRenderer` 或等价 runtime wiring test。
3. 把 `renderToCanvas` 与 `render(SpriteBatch)` 的职责分开，避免测试 API 伪装成 SpriteBatch lifecycle owner。

推荐测试：

- `TileRendererCanvasTest.recordsExplicitLayerFlushBoundaries`
- `FoundationViewportSupportTest` 或 screen-level wiring test 覆盖 runtime batch lifecycle 只由一个 owner 管理。

### P2

#### P2-1 `shellContentBounds / modalSafeBounds / bottomLogReservedBounds` primary owner 没冻结，overlay placement 仍要猜

证据：

- Glossary 允许 `modalSafeBounds` 由 `GameShellLayout`、`OverlayRenderFrame` 或等价 typed layout 输出：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:27`。
- `OverlayRenderFrame` 又被定义为包含 `shellContentBounds`、`modalSafeBounds`、`bottom log reserved bounds`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:214`。
- Tooltip / modal placement 都依赖这些 bounds：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:252-254`。
- 影响范围里 `GameShellLayout.kt` 只写 map viewport content bounds，没有明确新增 `shellContentBounds / modalSafeBounds / bottomLogReservedBounds`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:263-275`。
- 当前 `GameShellLayout` 只有 `leftRailBounds / mapBounds / rightPanelBounds / bottomHudBounds`：`client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt:19-24`。

问题：

这会让实现者在三个位置里任选一个计算 safe bounds：layout、overlay frame builder、overlay renderer。只要 tooltip 和 modal 分别二次推导，就会出现 bottom log 避让、footer hints、modal 居中不一致。

修复方向：

冻结 primary owner：

```kotlin
data class GameShellLayout(
    val leftRailBounds: GameShellBounds,
    val mapBounds: GameShellBounds,
    val rightPanelBounds: GameShellBounds,
    val bottomHudBounds: GameShellBounds,
    val shellContentBounds: GameShellBounds,
    val modalSafeBounds: ModalSafeBounds,
    val bottomLogReservedBounds: GameShellBounds,
    val cellAlignedMapBounds: RectInt,
    val mapInnerPadding: InsetsInt,
)
```

`OverlayRenderFrame` 只引用这些 typed layout outputs，不重新计算；`TileOverlayRenderer` 只消费，不推导。

推荐测试：

- `GameShellLayoutTest.producesModalSafeBoundsAboveFooterAndBottomLog`
- `TileOverlayLayerTest.usesLayoutModalSafeBoundsWithoutWindowRecalculation`

#### P2-2 Gate Budget 不满足治理文档要求，缺 golden freshness、duration source 和重型 gate 失败复盘入口

证据：

- `UI/pr/development-governance.md` 要求每个 PR 的 Gate Budget 声明重型任务、触发原因、resource / manifest / golden freshness，以及最近耗时来源或 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 读取方式：`UI/pr/development-governance.md:54-64`。
- 目标 PR 的 Gate Budget 只列了重型任务和触发原因：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:48-50`。

问题：

`acceptanceContractLint` 当前通过，但按新 PR 级 review standard，开发者仍不知道：

- golden 是全量刷新还是只新增 `dark-uiux-pr01-1-*`；
- PR-01 已 ship golden 如何判定 freshness；
- 同一重型 gate 失败两次时复盘写到哪里；
- 如何读取最近耗时，避免把 `goldenScreenshot` 或 `verifyChanged` 当调试循环。

修复方向：

补充 Gate Budget 小节：

```text
Golden freshness:
- 新增/更新 `dark-uiux-pr01-1-*` golden。
- PR-01 已存在 `dark-uiux-pr01-*` golden 不删除；等价替换必须在 manual record 显式映射。

Duration source:
- 读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。

Failure review:
- 同一重型 gate 失败超过 2 次，先补 focused viewport/layer/overlay test，并把原因写入 manual record 的 Findings 或 PR description。
```

#### P2-3 Acceptance Matrix 的 artifact 仍过于泛化，无法直接追到每个 focused test 的真实产物

证据：

- `UI01-1-M02` 和 `UI01-1-M06` 的 artifact 写 `build/reports/tests/`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:41-45`。
- 实际 client test report 默认在 module 下，例如 `client/build/reports/tests/test/`；root `build/reports/tests/` 并不是这些 client focused tests 的默认报告目录。
- 新 review standard 要求 artifact 能被开发者直接执行和追溯，不要只给泛化目录：`docs/review/rule/pr-level-review-standard.md:118-134`、`docs/review/rule/pr-level-review-standard.md:352-375`。

问题：

manual record 虽然要求列 `test class | test method | artifact | status`，但 Acceptance Matrix 顶层 artifact 不精确，会让 implementation review 时无法确认某个 requirement 的证据到底来自：

- `client/build/reports/tests/test/`
- `client/build/test-results/test/`
- root aggregate report
- `client/build/reports/golden/`

修复方向：

把 matrix artifact 改为可追溯的 repo-relative path，例如：

```text
client/build/reports/tests/test/
client/build/test-results/test/TEST-com.ktome.client.render.TileMapViewportTest.xml
client/build/reports/golden/
UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md
```

如果单行 artifact 太长，允许写多个 path，用 `<br>` 分隔或在 Canonical Artifact 中建 `requirementId -> artifact` 表。

#### P2-4 map-first 0.5 面积阈值没有冻结自动化测试方法名，容易退化成纯 manual/golden 判断

证据：

- 文档要求 `1280x800` 下 `mapBounds.width * mapBounds.height >= 0.5 * shellUsableContentArea`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:81`。
- Acceptance Matrix 把该项挂到 `UI01-1-M05`，fastCheck 为 `GameShellLayoutTest`, `TileRendererCanvasTest`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:44`。
- 必测行为只在 visual evidence 中描述 map-first，没有给出 exact focused test method：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:400-404`。
- 测试命名表列了 viewport、foundation、projection、map layer、overlay 方法名，但没有 map-first ratio 方法名：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:430`。
- manual record 表才要求填写 PASS/FAIL：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:469-477`。

问题：

map-first 是 PR-01-1 的玩家体验核心，不应只靠 golden 或人工截图判断。没有 exact test method，开发者可能只在 manual record 写 PASS，而没有自动锁定 `shellUsableContentArea` 分母和 0.5 阈值。

修复方向：

新增必测方法并写进 §8 / §9 命名表：

```text
GameShellLayoutTest.keepsMapAreaAtLeastHalfShellUsableContentAt1280x800
GameShellLayoutTest.computesShellUsableContentAreaExcludingBottomHud
```

`TileRendererCanvasTest` 只佐证实际 draw，没有资格替代 layout ratio owner。

#### P2-5 `TileOverlayAnchorKind` family 只有表格，没有冻结 type shape 和 payload contract

证据：

- 文档要求 anchor source 通过 sealed `TileOverlayAnchorKind` 或等价 typed family 表达：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:240`。
- 表格列出了 anchor family、coordinate authority、first consumer 和覆盖对象：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:242-249`。
- 但没有冻结 `TileOverlayAnchorKind` 的 exact enum / sealed variants，也没有说明每个 anchor payload 是 `Point`、`RectInt`、`GameShellBounds` 还是 presenter-owned id。

问题：

`WORLD_TILE`、`PANEL_SLOT`、`PANEL_ROW_OR_CARD`、`QUEST_ROW`、`MODAL_ROW` 是长期 public contract。只写 family 表格，不写 payload shape，会让 overlay model、presenter、renderer 和 tests 各自定义不同的 rect/tile carrier。

修复方向：

补一个最小 type contract：

```kotlin
sealed interface TileOverlayAnchor {
    val kind: TileOverlayAnchorKind

    data class WorldTile(val tile: Point) : TileOverlayAnchor
    data class PanelSlot(val bounds: RectInt, val slotId: String) : TileOverlayAnchor
    data class PanelRowOrCard(val bounds: RectInt, val sourceId: String) : TileOverlayAnchor
    data class QuestRow(val bounds: RectInt, val questId: String) : TileOverlayAnchor
    data class ModalRow(val bounds: RectInt, val frameKind: ModalFrameKind, val rowId: String) : TileOverlayAnchor
}
```

如果不想冻结 exact class names，至少冻结 variants、payload fields、coordinate space 和 owner。

#### P2-6 `TileTextMetrics` 的 wrap/truncate owner 与 hot-loop allocation 合同冲突，缺少 build phase / draw phase 边界

证据：

- 文档要求 `TileTextMetrics` 收敛 `approximateCharWidth`、`approximateLineHeight`、`truncateTextToWidth`、`wrapText`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:225`。
- 同时要求 render hot-loop 内不得新增 per-tile collection、string builder 或 wrapper object，稳态 allocation 预算 `< 1 KB/frame`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:229`。
- 影响范围把 `TileTextMetrics` 放在 renderer 路径下，但没有说明 wrap/truncate 是 model build phase 还是 draw phase 执行：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:274`。

问题：

tooltip/modal 的 wrap/truncate 很容易在 draw pass 中每帧重新分配字符串和列表。文档一边要求统一 metrics，一边没有冻结“文本布局必须预计算到 frame/model”的边界。

修复方向：

补充：

```text
TileTextMetrics may be called during model/frame build phase.
Renderer draw pass only consumes pre-wrapped/pre-truncated TileTextLine lists.
No draw pass may call wrapText/truncateTextToWidth for stable visible text.
```

推荐测试或自审：

- `Frame Ownership Self-Audit` 增加 `precomputed text layout fields`。
- `TileRendererCanvasTest` 或 maintainability review 检查 overlay draw pass 不调用 `wrapText`。

### P3

#### P3-1 PR-01-1 golden prefix 与 `UI/pr/README.md` 的 `dark-uiux-prNN-*` 通用约定有特例但未回写

证据：

- `UI/pr/README.md` 仍写每个 PR 的 golden label 使用 `dark-uiux-prNN-*` 前缀：`UI/pr/README.md:24`。
- PR-01-1 文档冻结新增 / 更新 `dark-uiux-pr01-1-*`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:56`。

影响：

目标文档本身够明确，但跨 PR 入口没有说明 `PR-01-1` 是合法特例。后续 reviewer 或 harness 注册时可能误以为应该落到 `dark-uiux-pr01-*`。

修复方向：

在 `UI/pr/README.md` 的 golden label 规则中补一句：`PR-01-1` 使用 `dark-uiux-pr01-1-*`，不得复用 `dark-uiux-pr01-*`，但可在 manual record 里说明等价替换关系。

#### P3-2 manual deadzone 白盒需要 `deadzoneStart / deadzoneEndExclusive`，但 debug overlay 允许列表未同步包含这些诊断字段

证据：

- Major Decisions 只允许 debug overlay 绘制 viewport bounds、mapBounds outline、focus tile / cursor 高亮、layer id 等诊断 hint：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:33`。
- manual step 要求截图标注 `viewportTopLeft`、`mapBounds`、`deadzoneStart`、`deadzoneEndExclusive`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:479`。

影响：

这不阻塞实现，因为 manual step 也可以从 `TileMapViewportTest` 或 test output 读取。但如果开发者选择 debug overlay 白盒路径，会发现允许列表里没有 deadzone 区间。

修复方向：

把 Major Decision 3 的 debug overlay 允许字段补成：`viewport bounds、mapBounds outline、focus tile / cursor、deadzoneStart / deadzoneEndExclusive、viewportTopLeft、layer id`。同时强调仍禁止 full-map ASCII。

#### P3-3 Acceptance Matrix 的多 owner 行建议拆 primary owner，避免 implementation review 时责任不清

证据：

- `UI01-1-M05` owner 为 `client` / `docs`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:44`。
- `UI01-1-M07` owner 为 `docs` / `tools`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:46`。

影响：

这不造成错误实现，但 PR 级规范要求每个 requirement 有明确 owner。多 owner 行容易让 review 时分不清是 client 行为缺口、docs evidence 缺口，还是 tools lint 缺口。

修复方向：

保留主行时增加 `primaryOwner`，或拆成：

- `UI01-1-M05A`：client map-first layout behavior
- `UI01-1-M05B`：docs/manual image comparison evidence
- `UI01-1-M07A`：docs acceptance contract
- `UI01-1-M07B`：tools verification closure

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-01-1 位于 `PR-01 -> PR-01-1 -> PR-02` 之间 | `UI/pr/README.md:21`；目标文档前置条件 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:6` | 一致 |
| Tile-only，不恢复 client ASCII fallback | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:31-33`、`:403`、`:414-420` | 一致 |
| Viewport identity 不使用 `revision`，mapDimensions primary source 来自 snapshot metadata | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:90-102`；`RenderMetadataSnapshot` 具有 `zoneId/currentFloor/width/height`：`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:21-37` | 一致 |
| Projection owner 不持有 previous state | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:116-118` | 一致 |
| Modal stack list 单一来源 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:121-142`；`OverlayState.modalFrames` 当前存在：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:58-84` | 一致 |
| Cursor source-of-truth | `overlayState` 和 `ModalFrameLocalState` 均持有 cursor；文档 resolution order 仍允许 sourceKind 与 anchor 脱钩 | 不一致 |
| Batch lifecycle owner | 文档写 `TileRenderer.renderToCanvas` owns begin/end；当前 runtime 由 `FoundationGameScreen` owns begin/end；影响范围没冻结迁移 | 不一致 |
| Safe bounds / bottom log reserved bounds | 文档要求使用 typed bounds，但 primary owner 仍在 layout/frame 间模糊 | 部分一致 |
| Acceptance Matrix / Gate Budget | `acceptanceContractLint` 通过；但 Gate Budget 缺 freshness / duration source，artifact path 过泛 | 部分一致 |
| Player-facing map-first evidence | 有 0.5 面积阈值与 manual table；缺 exact automated test method | 部分一致 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Viewport projection | typed request/result，mode/source/anchor/fallback 一次性解析 | 部分实现 / 文档仍有缺口 | 目标文档 `:121-207`；`InputHandler.kt:58-79`；`ModalStack.kt:20-33` | cursor 仍有 overlay 与 modal local 双 authority | High |
| Runtime render lifecycle | renderer orchestration，pass layer order，SpriteBatch lifecycle 单一 owner | 文档与当前代码不一致 | 目标文档 `:226`；`FoundationGameScreen.kt:139-147`；`TileRenderer.kt:122-135` | `renderToCanvas` 与 runtime `render(batch)` owner 不清 | High |
| Overlay safe area | tooltip/modal 统一避让 shell content、bottom log、footer hints | 部分实现 / 文档缺 primary owner | 目标文档 `:27`、`:214`、`:252-254`；`GameShellLayout.kt:19-24` | safe bounds 应归 layout primary owner | Medium |
| Acceptance/Gate | PR 文档含矩阵、Gate Budget、canonical artifact、failure rule | 部分一致 | 目标文档 `:36-60`；治理文档 `UI/pr/development-governance.md:54-64` | Gate Budget 和 artifact path 不够可执行 | Medium |
| Map-first UX | map-first 0.5 面积阈值、manual image comparison | 部分一致 | 目标文档 `:81`、`:469-477` | 自动 test method 未冻结 | Medium |
| ASCII removal | 删除 client ASCII renderer/model/manifest 字段，不删 core/game fixture | 一致 | 目标文档 `:31-33`、`:326-339`、`:414-428` | 未发现文档层冲突 | Low |

## 玩法与体验审查

### 核心循环

PR-01-1 不改战斗、奖励、成长规则，但它决定玩家局内行动的可读性基础。player-centered viewport、map-first layout、tooltip/modal 层级如果落实，能显著降低移动、查看、目标选择、确认行动时的视线切换成本。

当前剩余风险在 projection 和 overlay safe bounds：如果 `sourceKind` 与 `anchorTile` 脱钩，玩家会看到 cursor、tooltip、modal evidence 指向不同对象；如果 modal safe area 由 renderer 二次推导，最小窗口下底部日志和 footer hints 可能被遮挡。

### 战斗体验

文档对 targeting / combat decision 保留空间锚点是正确方向。但 `overlayState.targetingCursor` 与 `ModalFrameLocalState.targetingCursor` 的双 authority 会直接影响战斗目标选择：玩家可能以为正在确认 modal 内目标，viewport/tooltip 却使用 overlay-level stale cursor。这是当前阶段必须修的体验风险。

### 新手体验与信息反馈

map-first 0.5 阈值、bottom HUD 四分区、bottom log reserved bounds 都是新手理解当前目标与反馈的基础。文档已经写出体验目标，但必须把 map-first threshold 和 safe bounds owner 转成自动化合同，否则容易变成截图主观判断。

### 系统耦合与体验断层

当前最大断层不是玩法系统，而是 `InputHandler` state、modal local state、projection owner、overlay renderer 之间的 presentation authority 还未完全单一。PR-01-1 是后续 PR-03/05/07 的基础，必须在本 PR 文档中冻结。

## 当前阶段必须解决的问题

1. Cursor source-of-truth 必须现在修。
   - 为什么必须现在处理：PR-03 item modal、PR-05 combat/telegraph、PR-07 final whitebox 都会复用 overlay/projection/anchor 合同。
   - 为什么不能推迟：后续 PR 一旦按不同假设接入 tooltip 或 combat decision，就会产生第二套 anchor authority。
   - 修复方向：selected candidate 同时拥有 sourceKind、cursor、anchorKind；modal local 与 overlay-level cursor 必须按 active surface 分权或一致性检查。
   - 优先级：P1。

2. SpriteBatch lifecycle owner 必须现在修。
   - 为什么必须现在处理：这是 renderer 拆分的 runtime boundary；写错会直接 double-begin、missing end 或测试伪通过。
   - 为什么不能推迟：后续 renderer 拆分和 pass flush test 都依赖同一 batch owner。
   - 修复方向：建议保留 `FoundationGameScreen` 为 begin/end owner，`TileRenderer.renderToCanvas` 保持 test canvas path；或者显式把 begin/end 迁到 `TileRenderer.render` 并更新 screen impact/test。
   - 优先级：P1。

3. Safe bounds primary owner 必须现在修。
   - 为什么必须现在处理：tooltip/modal/bottom log/footer hints 是 PR-01-1 的核心交互层。
   - 为什么不能推迟：PR-03/04/05/07 都会新增 row/card/modal tooltip，若 safe bounds 现在不统一，后续每个 surface 都会产生局部坐标分支。
   - 修复方向：`GameShellLayout` 输出 primary safe bounds，`OverlayRenderFrame` 只引用。
   - 优先级：P2。

4. Gate Budget 和 artifact path 必须现在修。
   - 为什么必须现在处理：该 PR 是 XL 且重型 gate 多，如果没有 freshness 和 artifact mapping，review/CI 修复会反复依赖人工记忆。
   - 为什么不能推迟：实现开始后证据路径会散落到测试报告、golden、manual record 和 verifyChanged summary。
   - 修复方向：补 requirementId -> artifact 表、golden freshness、duration source 和失败复盘位置。
   - 优先级：P2。

## Removal/Iteration Plan

### Safe to Remove Now

无新增安全删除项。本轮发现的问题都属于 PR 文档 contract 补全，不建议在 review 阶段删除现有文件。

### Defer Removal

client ASCII 删除计划已经在目标文档中有明确 Deletion Checklist 和扫描命令；当前 review 未发现需要额外 staged removal 的项目。

## Additional Suggestions

1. 在目标文档顶部新增一行 `Review standard: docs/review/rule/pr-level-review-standard.md`，让后续 reviewer 直接知道本 PR 采用新版 PR 级规范。
2. 在 §8 必测行为里按 owner 分组补 `requirementId`，例如 `UI01-1-M01 -> tests 1-12`，减少 manual record 填写时的映射成本。
3. 如果后续扩展 `acceptanceContractLint`，优先检查 Gate Budget 四字段、artifact path 是否为 module-specific、`or equivalent` 是否带 primary source。

## Suggested Verification

已运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：通过。执行过程中 `syncPhase2Manifests` 同步了 canonical visual/audio manifest 到 runtime manifest。

建议目标文档修复后再运行：

```bash
git diff --check -- UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md
awk 'BEGIN{c=0} /^```/{c++} END{print "FENCE_OPEN=" c%2}' UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md
ABS_PATH_PATTERN='/(Users|tmp)/|[A-Za-z]:\\\\'
rg -n "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

进入实现后最小开发验证仍按目标 PR 文档执行，重点补充：

```bash
./gradlew :client:test --tests com.ktome.client.render.TileViewportFocusProjectionTest --tests com.ktome.client.render.TileMapViewportTest --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.TileOverlayLayerTest
./gradlew :client:clientSmoke
./gradlew :client:goldenScreenshot
./gradlew maintainabilityLint verifyChanged
```

## Summary

按新的 PR 级 review standard 复审后，目标文档已经解决上一轮的大部分显性缺口：viewport identity、projection enum、modal stack 单一来源、fallback anchor validity、active cursor layer plan、FoundationViewportSupportTest 都已写入合同。

剩余问题集中在“开发实施时仍会猜”的边界：cursor 仍有 overlay 与 modal local 双 authority；SpriteBatch lifecycle owner 与当前 runtime API 冲突；safe bounds primary owner 未冻结；Gate Budget、artifact path 和 map-first 自动化 test 还不够精确。这些问题不是文字润色，尤其前两个会直接影响实现正确性，建议在进入编码前先修目标 PR 文档。
