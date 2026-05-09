# Dark UI/UX PR-01-1 PR 级复审报告

目标文档：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

Review 标准：`docs/review/rule/pr-level-review-standard.md`

复审时间：2026-05-08

## 结论

本轮文档相对上一轮已经补齐了大部分可执行性缺口：Acceptance Matrix、Gate Budget、artifact 路径、viewport projection candidate、safe bounds owner、SpriteBatch owner、anchor type family、manual record 字段和 focused test 名称都已经明显收敛。

当前未发现新的 P1 阻断级问题，但仍有 4 个 P2 和 1 个 P3 需要修正。它们不是措辞问题，而是会让开发者在实现时出现分叉、focused lane 跑不起来、或把 overlay 业务规则写回 renderer 的细节。

建议修完这些点后再进入实现，否则 PR-01-1 仍可能在 overlay anchor、tooltip arbitration、modal cursor 双写和测试执行入口上产生第二真源。

## 预检摘要

- 命中范围：dark UI/UX PR 系列，目标 PR 为 `dark-uiux-pr01-1-client-viewport-renderer-overlay`。
- 上游权威：`AGENTS.md`、`docs/INDEX.md`、`UI/PLAN.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`docs/review/rule/pr-level-review-standard.md`。
- 受影响模块：`client` 为主，`docs` / `tools` 提供合同与 lint 约束。
- 稳定合同触点：client Tile-only 渲染、viewport identity、overlay anchor、manual evidence、golden/client smoke/verifyChanged gate。
- 已执行验证：`source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint`，结果 `BUILD SUCCESSFUL`。
- 未执行：client focused tests、clientSmoke、goldenScreenshot、maintainabilityLint、verifyChanged。本轮是文档复审，不声明这些通过。

## Findings

### P2-1 `OverlayRenderFrame` 字段清单与 WORLD_TILE anchor 解析路径仍有矛盾

证据：

- `OverlayRenderFrame` 字段清单写成只包含 `TileOverlayModel`、`shellContentBounds`、`modalSafeBounds`、bottom log bounds、text metrics、modal stack projection：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:240`
- 后文又要求 `TileMapViewport` 被 `MapRenderFrame / OverlayRenderFrame` 通过引用消费：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:258`
- `WORLD_TILE` anchor 的坐标权威是 `TileMapViewport.tileToScreen(tile)`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:287`
- tooltip placement 又需要完整 anchor rect 的 `left/right/top/bottom`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:335`

问题：

开发者照第 240 行实现时，`OverlayRenderFrame` 不带 viewport；照第 258 行实现时，`OverlayRenderFrame` 又必须引用 viewport。两者都能解释为“正确”，会导致三种分叉：

1. overlay renderer 直接拿 `TileMapViewport` 转 WORLD_TILE anchor；
2. frame builder 预先把 `WorldTile` resolved 成 screen rect；
3. renderer 临时重新计算或从别处抓 map transform。

第 3 种会直接违反 tile-to-screen 单一真源；第 1 和第 2 种虽然都可行，但文档没有冻结 owner，后续测试也难以判断谁是正确实现。

建议修正文档：

- 在 §4 明确二选一：
  - 方案 A：`OverlayRenderFrame` minimum shape 显式包含 `viewport: TileMapViewport`，overlay renderer 只调用该 viewport resolve `WORLD_TILE`。
  - 方案 B：新增 `TileOverlayAnchorResolver` / frame build phase，将所有 `TileOverlayAnchor` 预解析成 `ResolvedTileOverlayAnchor(bounds: RectInt, source: ...)`，overlay renderer 不再接触 viewport。
- 同步更新 `Frame Ownership Self-Audit` 字段，明确 viewport 是 frame reference truth 还是 anchor resolver build truth。
- 给 `TileOverlayLayerTest.cornerAnchorsFlipInsideShellBounds` 增加断言：WORLD_TILE anchor 的 screen rect 来自同一个 `TileMapViewport.tileToScreen`，而不是 overlay renderer 自己推导。

### P2-2 `TileOverlayModel` 最小结构仍未冻结，tooltip/modal 仲裁 owner 会漂移

证据：

- PR 目标要求引入 `TileOverlayModel`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:80`
- `OverlayRenderFrame` 只说包含 `TileOverlayModel`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:240`
- `TileOverlayRenderer` 不得把具体 `ModalFrameKind` 业务规则写进 renderer：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:267`
- tooltip 优先级和 modal suppression 已冻结：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:281-283`
- focused test 要构造同一 `OverlayRenderFrame` 内 passive anchored tooltip 与 top modal 冲突：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:479`

问题：

文档冻结了 overlay 行为，但没有冻结 `TileOverlayModel` 的最小字段和 builder owner。开发者可以把 tooltip 优先级、modal suppression、modal explicit tooltip 和 passive tooltip 互斥规则放在不同位置：

1. `TileOverlayModelBuilder` 内预仲裁；
2. `TileOverlayRenderer` draw path 内判断；
3. 每个 modal presenter 自己决定；
4. `OverlayRenderFrame` 构造处临时拼装。

这会让“renderer 不写 ModalFrameKind 业务规则”没有可执行边界，也会让 `TileOverlayLayerTest` 不知道该测 model output 还是 renderer side effect。

建议修正文档：

- 在 §5 或 §4 增加 `TileOverlayModel` minimum shape，例如：

```kotlin
data class TileOverlayModel(
    val selectedTooltip: TileTooltipModel?,
    val selectedTooltipSource: TileTooltipSource?,
    val activeModal: TileModalModel?,
    val modalBackdrop: TileModalBackdropModel?,
    val toast: TileToastModel?,
    val debugHints: List<TileDebugHintModel>,
    val suppressedTooltipSources: List<TileTooltipSuppression>,
)
```

- 明确 `TileOverlayModelBuilder` 或等价 owner 负责执行 tooltip priority 和 suppression；`TileOverlayRenderer` 只按 model 画，不 switch `ModalFrameKind`。
- 增加或改名测试：
  - `TileOverlayLayerTest.selectsModalExplicitTooltipBeforePassiveTooltip`
  - `TileOverlayLayerTest.recordsSuppressedPassiveTooltipWhenModalActive`
  - `TileOverlayLayerTest.rendererDoesNotBranchOnModalFrameKind`

### P2-3 modal local cursor 与 overlay-level cursor 的 consistency check 谓词仍不够精确

证据：

- 文档要求双写期间做 consistency check，但触发条件写成“同一 active surface / same mode”：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:213`
- 当前 `OverlayState` 仍有 overlay-level `targetingCursor` / `inspectCursor`：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:58-75`
- 当前 `ModalFrameLocalState` 也有 modal local `targetingCursor` / `inspectCursor`：`client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:20-32`
- 当前 combat decision / targeting 路径会同步写全局 cursor 与 modal local cursor：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1389-1428`、`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1563-1601`

问题：

“same active surface / same mode”不足以让开发者写出唯一 predicate。尤其是：

- `MODAL_TARGETING` vs `OVERLAY_TARGETING` 是否一定视为同一 surface；
- `MODAL_COMBAT_DECISION` vs `OVERLAY_TARGETING` 是否一定视为同一 surface；
- top non-spatial modal 下方的 `MODAL_INSPECT` 是否要与 overlay-level `INSPECT` 做一致性检查；
- overlay-level cursor 在 modal active 时应该 `null`、同值、还是只在特定 frame kind 下忽略。

这些差异会产生两类实际风险：一种是正常 combat decision 被误判为 divergent fail fast；另一种是 stale overlay cursor 未被 fail fast，继续污染 tooltip anchor。

建议修正文档：

- 在 §3.6 后补一张 matrix，显式冻结：

| selected candidate | overlay-level candidate | 是否同一 surface | 允许值 | 不一致行为 |
| --- | --- | --- | --- | --- |
| `MODAL_TARGETING` | `OVERLAY_TARGETING` | yes/no | `null` or equal / ignored | fail fast / ignored |
| `MODAL_COMBAT_DECISION` | `OVERLAY_TARGETING` | yes/no | ... | ... |
| `MODAL_INSPECT` | `OVERLAY_INSPECT` | yes/no | ... | ... |

- 明确 check owner 是 `TileViewportFocusProjectionRequest` 构造处，还是 `TileViewportFocusProjection` 内部。
- 如果 PR-01-1 仍保留双写，要求 manual record 的 `Frame Ownership Self-Audit` 记录“temporary dual-write fields”和后续删除条件。

### P2-4 focused lane 的 wildcard 过滤器与必需测试名不完全匹配，可能导致正式命令跑不起来

证据：

- `TileRendererCanvasTest` 的必需名称只固定了 `recordsExplicitLayerFlushBoundaries`，其他 viewport / overlay / modal / tooltip wiring 允许“等价名称”：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:522`
- viewport lane 使用 `--tests "com.ktome.client.render.TileRendererCanvasTest*viewport*"`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:532`
- map layer lane 使用 `--tests "com.ktome.client.render.TileRendererCanvasTest*layer*"`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:538`
- overlay lane 使用 `*overlay*`、`*modal*`、`*tooltip*`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:544`

问题：

这组命令是“正式 PR 的首选 lane”，但测试命名表没有保证 `TileRendererCanvasTest` 中一定存在匹配这些 lowercase wildcard 的方法。更细一点，`recordsExplicitLayerFlushBoundaries` 中是 `Layer` 大写，`*layer*` 若按大小写敏感匹配，可能无法匹配。开发者按第 522 行写了等价名称，可能仍会遇到 Gradle “No tests found for given includes”，或者被迫临时改命令。

建议修正文档，二选一：

1. 把 focused lane 改成 class-level：

```bash
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest
```

2. 或冻结 `TileRendererCanvasTest` 的 exact method names，并保证它们匹配 lane：
   - `viewportUsesShellMapBounds`
   - `layerRecordsExplicitFlushBoundaries`
   - `overlayDrawsAboveShellAndBottomHud`
   - `modalUsesModalSafeBounds`
   - `tooltipAvoidsBottomLogReservedBounds`

推荐方案 1。PR-01-1 已经有独立 owner tests，`TileRendererCanvasTest` 作为 wiring test，class-level focused lane 更稳定。

### P3-1 inspect stale overlay cursor 缺少与 targeting 对称的显式回归测试

证据：

- 同一 mode 内 selected candidate 优先级要求 `MODAL_INSPECT -> OVERLAY_INSPECT`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:211`
- 必测列表有 `modalTargetingCursorWinsOverStaleOverlayCursor`，但 inspect 只有泛化的 `inspectModeResolvesCursorFromOverlayOrModalState`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:455`
- 测试命名表同样缺少 inspect stale overlay 专项：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:520`

问题：

targeting 的 stale overlay cursor 被单独锁住了，inspect 没有对称测试。由于当前状态模型同样存在 `OverlayState.inspectCursor` 和 `ModalFrameLocalState.inspectCursor` 两个字段，开发者可能只覆盖“overlay inspect 可用”和“modal inspect 可用”，但漏掉“modal inspect 必须压过 stale overlay inspect”的边界。

建议修正文档：

- 在 §8.12 和测试命名表增加 `modalInspectCursorWinsOverStaleOverlayCursor`。
- 该测试应断言 `sourceKind = MODAL_INSPECT`、`anchorTile` 来自 modal local cursor、stale `overlayState.inspectCursor` 不参与 anchor 或 viewport fallback。

## 已确认关闭的上一轮问题

- `sourceKind`、`resolvedFocusTile`、`anchorTile` 从同一 selected candidate 派生，已补充。
- `SpriteBatch.begin/end` owner 与 `renderToCanvas` test-only 边界，已补充。
- `shellContentBounds`、`modalSafeBounds`、`bottomLogReservedBounds` owner，已补充到 `GameShellLayout` primary output。
- Gate Budget 的 freshness、duration、failure review，已补充。
- canonical artifact 已改为 module-specific repo-relative path。
- map-first 面积阈值测试方法、debug overlay deadzone 字段、PR-01-1 golden prefix、primary owner 说明，已补齐。

## 建议修正顺序

1. 先修 P2-1，冻结 `OverlayRenderFrame` 如何消费 viewport 或 resolved anchor rect。
2. 再修 P2-2，冻结 `TileOverlayModel` 最小 shape 和 tooltip arbitration owner。
3. 然后修 P2-3，给 modal/overlay cursor 双写加精确 matrix。
4. 最后修 P2-4 / P3-1，避免实现阶段 focused lane 和 test naming 返工。

完成后建议重跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

如果只改测试命名和 focused lane，不涉及 Acceptance Matrix、Major Decisions、Token Contract 或 Deletion Checklist，仍建议至少重跑一次 `acceptanceContractLint`，因为 PR-01-1 本身把它定义为文档合同快路径。
