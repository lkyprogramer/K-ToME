# Dark UI/UX PR-01-1 PR 级复审报告 Round 2

目标文档：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

Review 标准：`docs/review/rule/pr-level-review-standard.md`

上一轮报告：`UI/review/2026-05-08-dark-uiux-pr01-1-pr-level-standard-rereview.md`

复审时间：2026-05-09

## Findings

### P0

无。

### P1

#### P1-1 cross-mode modal/overlay candidate 规则自相矛盾，会让 stale overlay state 抢走 spatial modal focus

证据：

- mode selection 规定：只要存在任一 `TARGETING` candidate，`resolvedMode = TARGETING`；同 mode 内再按 modal -> overlay 选：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:211`
- consistency matrix 又规定：`OVERLAY_TARGETING` 被选中时，若存在 spatial modal candidate，selection 不应落到 overlay，应 fail fast：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:222`
- `OVERLAY_INSPECT` 同样规定存在 spatial modal candidate 时不应被选中：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:223`
- non-spatial top modal 不遮蔽下层 spatial candidate，且 consistency check 以 selected spatial candidate 为准：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:226`

问题：

文档新增的 matrix 修复了 same-mode 双写问题，但没有处理 cross-mode stale state。一个可构造的状态如下：

1. `modalFrames` 下层有 `MODAL_INSPECT` spatial candidate。
2. top 是 `ITEM_DETAIL` 这类 non-spatial modal，不遮蔽下层 inspect。
3. `overlayState.mode == TARGETING` 且有 stale `overlayState.targetingCursor`。

按第 211 行，`OVERLAY_TARGETING` 因为 `TARGETING > INSPECT` 会赢；按第 222 行，只要存在 spatial modal candidate，`OVERLAY_TARGETING` 又不应该被选中。两条都是 MUST，开发者无法唯一实现。

影响：

- 可能把仍处于 modal inspect 的 viewport 误切到 stale overlay targeting，造成首帧 snap、cursor、tooltip anchor 和 manual evidence 漂移。
- 也可能不同开发者选择“先 fail fast”或“先按 mode priority 选”，导致 focused tests、golden 和 manual record 对不上。
- 这是 PR-01-1 的核心 foundation contract，不能推给后续 PR。

修复方向：

明确 cross-mode conflict 的唯一规则，建议二选一：

1. **modal spatial candidate 优先**：只要存在任一 modal spatial candidate，所有 overlay-level candidate 不论同 mode 还是跨 mode 都只能作为 consistency check 输入，不能参与 selection；如果 overlay-level 不为 `null` 且与 modal state 冲突，则 fail fast。
2. **global mode priority 优先**：允许 `OVERLAY_TARGETING` 抢过 `MODAL_INSPECT`，但必须删除第 222/223 行的“存在 spatial modal candidate 时 selection 不应落到 overlay”语义，并补充为何 overlay mode 是更高 authority。

从当前文档“modal local cursor 必须优先于 stale overlay-level cursor”的长期目标看，推荐方案 1。

推荐测试：

- `TileViewportFocusProjectionTest.modalSpatialCandidateSuppressesCrossModeOverlayCandidate`
- `TileViewportFocusProjectionTest.rejectsOverlayTargetingWhenModalInspectCandidateExists`
- `TileViewportFocusProjectionTest.rejectsOverlayInspectWhenModalTargetingCandidateExists`

### P2

#### P2-1 `TileOverlayAnchorResolver.resolve()` 用 nullable 表达失败，无法区分 suppression reason

证据：

- resolver 最小合同返回 `ResolvedTileOverlayAnchor?`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:288-290`
- 文档要求 anchor 离开 visible range 或 entity 失效时 resolver 返回 `null`，`TileOverlayModelBuilder` 据此隐藏或 suppress：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:295`
- `TileTooltipSuppressionReason` 已区分 `ANCHOR_OUTSIDE_VISIBLE_RANGE`、`ANCHOR_RESOLUTION_FAILED`、`LOWER_PRIORITY_TOOLTIP` 等原因：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:401-411`
- `suppressedTooltipSources` 又被定义为 manual record / focused test evidence：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:414`

问题：

resolver 只返回 `null`，builder 无法知道失败原因是 out-of-visible-range、entity stale、layout missing，还是 resolver 内部 invariant broken。这样 `suppressedTooltipSources` 的 reason 字段会被迫猜测或统一写成 `ANCHOR_RESOLUTION_FAILED`，削弱了 manual evidence 和 focused test 的可诊断性。

影响：

- `TileOverlayLayerTest.hidesTooltipWhenAnchorLeavesVisibleRange` 只能证明 tooltip 消失，不能证明消失原因。
- manual record 里的 `Overlay Conflict Evidence` 很容易变成“隐藏了，但不知道为什么”。
- 后续 PR-03/PR-07 扩展 panel row/card anchor 时，stale presenter rect 与 viewport 外 world tile 会混成同一种失败。

修复方向：

把 resolver 返回值改成 typed result，例如：

```kotlin
sealed interface TileOverlayAnchorResolution {
    data class Resolved(val anchor: ResolvedTileOverlayAnchor) : TileOverlayAnchorResolution
    data class Failed(
        val source: TileOverlayAnchor,
        val reason: TileTooltipSuppressionReason,
    ) : TileOverlayAnchorResolution
}
```

或者保留 `ResolvedTileOverlayAnchor?`，但必须增加同帧可读的 failure diagnostic；不建议，因为会形成第二路径。

推荐测试：

- `TileOverlayLayerTest.recordsAnchorOutsideVisibleRangeSuppressionReason`
- `TileOverlayLayerTest.recordsAnchorResolutionFailedWhenPresenterRectMissing`

#### P2-2 `OverlayState.modalFrames` 的顺序没有在 PR 文档中冻结，projection fixture 仍可能写反

证据：

- PR 文档要求 `OverlayState.modalFrames` 是唯一 modal stack source：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:162`
- candidate 收集使用 `overlayState.modalFrames.asReversed()` 从 top 到 bottom 扫描：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:205`
- 当前代码中 `activeModalKind` 使用 `modalFrames.lastOrNull()` 作为 top：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:82-84`
- 当前 `ModalStack.frames()` 返回 `frames.toList()`，push 后 top 在 list 末尾：`client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:54-56`

问题：

文档依赖 `modalFrames` bottom-to-top 顺序，但没有在合同里明说“bottom first, top last”。实现者读到 `asReversed()` 可以推断，但测试 fixture、validation fixture 或未来 builder 很容易直接按 top-to-bottom 构造 `OverlayState.modalFrames`，导致 projection priority 反转。

影响：

- `nonSpatialModalPreservesCurrentSpatialProjection` 和 modal cursor priority 这类测试可能因 fixture 顺序写错而给出假阳性或假阴性。
- 后续如果 `OverlayState` 被手工构造而不是来自 `ModalStack.frames()`，top frame 语义会漂移。

修复方向：

- 在 §3.3 或 Glossary 增加硬合同：`OverlayState.modalFrames` order is bottom-to-top; `lastOrNull()` is top; projection uses `asReversed()` to scan top-to-bottom。
- 增加测试名：
  - `ModalStackTest.framesAreBottomToTopForProjection`
  - `TileViewportFocusProjectionTest.scansModalFramesTopToBottomFromBottomFirstList`
- manual record 的 `Frame Ownership Self-Audit` 也应记录 modal frame order truth。

#### P2-3 `OverlayRenderFrame` 仍缺少 exact minimum shape，`modal stack projection` 字段语义不够可执行

证据：

- 文档只用 prose 描述 `OverlayRenderFrame` 字段，并保留未命名的 `modal stack projection`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:253`
- `TileViewportFocusProjectionResult` 已经有明确字段：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:145-152`
- `TileOverlayModelBuilder` 已负责 tooltip priority、modal suppression、modal explicit tooltip 与 passive tooltip 互斥：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:373-414`

问题：

上一轮的 viewport/reference 矛盾已解决，但 `OverlayRenderFrame` 本身仍没有 data class 级最小 shape。尤其是 `modal stack projection` 不是冻结类型名，可能被实现成：

1. `TileViewportFocusProjectionResult`
2. `ModalFrame`
3. `ModalStack.top()`
4. 自定义 modal layer projection
5. raw `OverlayState`

其中 2/3/5 都会让 renderer 或 frame 重新接触 modal 业务状态，削弱 `TileOverlayModelBuilder` 的 arbitration owner。

影响：

- frame ownership self-audit 虽然要求列字段，但开发前无法判断哪些字段合法。
- renderer 可能拿 `modal stack projection` 再次判断 tooltip/modal layer，形成第二 arbitration path。
- 后续 `TileOverlayRenderer` 的测试会不知道断言 frame shape 还是 builder output。

修复方向：

在 §4 增加 exact minimum shape，例如：

```kotlin
data class OverlayRenderFrame(
    val overlayModel: TileOverlayModel,
    val shellContentBounds: GameShellBounds,
    val modalSafeBounds: ModalSafeBounds,
    val bottomLogReservedBounds: GameShellBounds,
    val textMetrics: TileTextMetrics,
    val focusProjection: TileViewportFocusProjectionResult,
)
```

如果 renderer 不应该消费 `focusProjection`，则从 frame 中删除该字段，改为 `TileOverlayModelBuilder` 的输入，并在 frame audit 里声明 renderer 只消费 `TileOverlayModel`。

推荐测试：

- `TileRendererCanvasTest.overlayFrameDoesNotCarryRawOverlayState`
- `TileOverlayLayerTest.rendererUsesSelectedTooltipFromModelOnly`

### P3

#### P3-1 `TileModalModel` / `TileModalBackdropModel` 被引用但没有最小字段，后续实现仍要猜 renderer 所需数据

证据：

- `TileOverlayModel` 引用 `TileModalModel` 和 `TileModalBackdropModel`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:378-383`
- modal backdrop alpha / color 来自 stack top frame kind 配置：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:318`
- renderer 不得 switch `ModalFrameKind`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:373`

问题：

这不是当前阻塞项，因为主要 arbitration 字段已经冻结，但如果不补最小字段，开发者仍需要猜 `TileModalBackdropModel` 是否应携带 `alpha/color/bounds`，以及 `TileModalModel` 是否携带 title/body/footer hints / explicit tooltip slot。尤其 backdrop 的 alpha/color 若不在 model 中，renderer 很容易回头根据 `ModalFrameKind` 再算一次。

修复方向：

补一个最小 shape 即可，不需要扩大 PR：

```kotlin
data class TileModalBackdropModel(
    val bounds: GameShellBounds,
    val color: ColorSpec,
    val alpha: Float,
)

data class TileModalModel(
    val frameKind: ModalFrameKind,
    val bounds: RectInt,
    val titleLine: TileTextLine,
    val bodyLines: List<TileTextLine>,
    val footerHintLines: List<TileTextLine>,
)
```

如果不想冻结具体 body 字段，至少冻结 backdrop model 的 `alpha/color/bounds`，避免 renderer branch。

## 上一轮 Findings 吸收状态

| 上一轮问题 | 本轮状态 | 证据 |
| --- | --- | --- |
| `OverlayRenderFrame` 与 WORLD_TILE anchor 解析路径矛盾 | 已解决，但衍生出 P2-3 exact frame shape 问题 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:253-295` |
| `TileOverlayModel` minimum shape 缺失 | 已解决 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:373-414` |
| modal local cursor 与 overlay-level cursor consistency 谓词不精确 | 部分解决，并新增 P1-1 cross-mode 矛盾 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:213-226` |
| focused lane wildcard 与测试名不匹配 | 已解决 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:612-630` |
| inspect stale overlay cursor 缺少对称测试 | 已解决 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:538`、`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:603` |

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-01-1 作为 PR-01 架构补强，不提前实现 PR-02/03/04/05 内容表现 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:15-17`、`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:507-521` | 一致 |
| Tile dark UI 是唯一正式 client 渲染路径，client ASCII fallback 删除 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:31-36`、`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:492-505` | 一致 |
| Viewport focus projection 必须有唯一 source of truth、fallback、ordering、failure semantics | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:127-246` | 部分一致，cross-mode modal/overlay candidate 冲突仍未冻结 |
| Overlay anchor 必须 typed、不能 renderer-local 推导 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:253-295`、`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:321-371` | 部分一致，resolver failure reason 丢失 |
| Renderer 拆分必须避免第二 arbitration path | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:248-312`、`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:373-414` | 部分一致，`OverlayRenderFrame` exact shape 未冻结 |
| Focused tests、owner gate、manual evidence 必须可追溯 | `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:38-73`、`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:572-686` | 一致 |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| Viewport projection | candidate/source/fallback/consistency 统一由 `TileViewportFocusProjection` 负责 | 文档已大幅补齐，但 cross-mode stale overlay 与 spatial modal 冲突未定义唯一结果 | 状态机矛盾 | High |
| Overlay anchor resolution | WORLD_TILE 由 viewport resolver 解析，panel/modal rect 由 layout/presenter 输出 | 文档新增 resolver，但 nullable result 无法表达失败原因 | evidence 语义不足 | Medium |
| Overlay render frame | renderer 只消费稳定 frame/model，不碰 raw state | 文档仍用 prose 字段和 `modal stack projection` | frame shape 可执行性不足 | Medium |
| Modal stack order | projection top-to-bottom 扫描 | 当前代码 top 在 list 末尾，文档未显式冻结 bottom-to-top order | fixture 容易写反 | Medium |
| Focused lane | class-level lane 避免 wildcard no-match | 文档已修正 | 无 | Low |

## 玩法与体验审查

### 核心循环

PR-01-1 的核心目标是让地图成为第一视觉焦点，并保证 inspect / targeting / modal 不破坏移动和查看信息的闭环。文档总体已经接近可执行，但 P1-1 会直接影响玩家在 modal/inspect/targeting 切换时看到的地图中心和 cursor anchor，是当前 PR 的体验基础问题。

### 战斗体验

`COMBAT_DECISION` 复用 targeting cursor 的方向是合理的；但 cross-mode conflict 不解决时，战斗选择 modal 可能被 stale overlay targeting 或 stale inspect state 影响，导致确认目标、feedback tile 和 modal layer evidence 不一致。

### 新手体验与信息反馈

tooltip/modal suppression reason 已经被建模，但 resolver 返回 nullable 会损失“为什么 tooltip 消失”的信息。对玩家来说表现都是 tooltip 不见；对开发和白盒来说必须能区分 anchor 出屏、entity 失效、低优先级 suppression。

### 系统耦合与体验断层

`TileOverlayModelBuilder` 的加入是正确方向，但 `OverlayRenderFrame` exact shape 未冻结，会让 renderer、builder、projection 的边界继续有缝隙。这个问题现在修比实现后返工便宜。

## 当前阶段必须解决的问题

1. P1-1 必须当前 PR 修。它是 projection ordering / source of truth 冲突，直接决定 viewport identity、cursor 和 tooltip anchor。
2. P2-1 必须当前 PR 修。resolver failure reason 是 manual evidence 和 focused tests 的基础字段，后续 PR 扩展 anchor family 会依赖它。
3. P2-2 建议当前 PR 修。只需补一句 order contract 和两个测试名，但能避免所有 projection fixture 写反。
4. P2-3 建议当前 PR 修。PR-01-1 本来就是 renderer/frame foundation，frame shape 不应留给实现者猜。
5. P3-1 可随 P2-3 一起补，不需要单独扩大范围。

## Removal/Iteration Plan

本轮没有新增可立即删除的代码路径。既有 client ASCII 删除计划已经在目标文档的 Deletion Checklist 中维护，当前复审未发现需要另开 removal plan 的新增旧路径。

需要迭代冻结的只有文档合同：

- `TileViewportFocusProjection` cross-mode conflict rule。
- `TileOverlayAnchorResolution` typed failure result。
- `OverlayState.modalFrames` bottom-to-top order。
- `OverlayRenderFrame` exact minimum shape。

## Additional Suggestions

- 把 `TileOverlayAnchorResolver` 改名为 `TileOverlayAnchorResolutionResolver` 没必要，保持现在的短名即可；关键是返回 typed result。
- `TileTooltipSuppressionReason.LOWER_PRIORITY_TOOLTIP` 可以暂时保留，但后续如果 manual evidence 要写出胜出 source，可加 `winner: TileTooltipSource` 到 `TileTooltipSuppression`，不必在本 PR 强制。
- `Frame Ownership Self-Audit` 可以把 `modalFrames order truth` 与 `temporary dual-write fields` 放在同一小表中，便于人工复核。

## Open Questions

无需要向用户阻塞确认的问题。P1-1 的推荐方向是 modal spatial candidate 优先，但如果设计上希望 global mode priority 绝对优先，文档必须显式改写 matrix。

## Suggested Verification

本轮已执行：

```bash
for f in UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md docs/review/rule/pr-level-review-standard.md; do awk 'BEGIN{c=0} /^```/{c++} END{print FILENAME ":FENCE_OPEN=" c%2}' "$f"; done
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n --type md "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md docs/review/rule/pr-level-review-standard.md
git diff --check -- UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md docs/review/rule/pr-level-review-standard.md
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint
```

结果：

- fenced code block 检查通过，两个文档 `FENCE_OPEN=0`。
- 目标文档与 review standard 无本机绝对路径命中。
- `git diff --check` 无输出。
- `acceptanceContractLint` 结果 `BUILD SUCCESSFUL`。

修复本报告 findings 后建议重跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

进入实现后还必须按目标文档补跑 focused lanes、`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint` 和最终 `verifyChanged`。本轮未运行这些实现级 gate，不声明通过。

## Summary

这轮优化已经吸收了上一轮几乎所有直接问题，文档整体比上一版更接近“可开发执行文档”。但新增 consistency matrix 后暴露出一个更深的状态机矛盾：cross-mode stale overlay candidate 与 spatial modal candidate 同时存在时，mode priority 和 matrix impossible selection 冲突。这个必须在开发前修，否则 projection owner 会重新分叉。其余 P2/P3 都是小范围文档补强，但会显著降低实现返工。
