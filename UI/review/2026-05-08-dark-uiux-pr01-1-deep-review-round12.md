# Dark UI/UX PR-01-1 深度 Review Round 12

目标文档：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查重点：本轮只看“是否还能让开发者猜”。Round 11 的大项已经被吸收：`FoundationViewportSupportTest` 进入命令，`TileMapViewportIdentity`、projection request/result、validation projection、fallback anchor validity、`TileMapLayerPlan` 都已经写入合同。剩余问题集中在 projection candidate 激活条件、request 双来源、enum 值冻结和少数 layer plan 可视细节。

## Findings

### P0

无。

### P1

#### P1-1 spatial candidate 必须 `cursor != null` 与 fallback 合同冲突，会让 active inspect/targeting 被错误降级成 `PLAYER`

证据：

- §3 `Focus Tile Resolution` 要求 `INSPECT / TARGETING` 在 cursor 缺失时 fallback 到 `playerTile`，且 resolved mode 仍保持原 mode，直到 typed state 明确退出：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:171-176`。
- 但 projection candidate 收集规则要求 overlay-level targeting / inspect candidate 必须同时满足 active surface 且 `targetingCursor != null` / `inspectCursor != null`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:156-157`。
- modal candidates 也只接受 spatial frame 的 `localState.targetingCursor` 或 `localState.inspectCursor`，没有说明 spatial frame active 但 local cursor 为空时仍应产生 mode candidate：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:158`。

影响：

这会让开发者按 candidate 规则实现时，在 active targeting / inspect 但 cursor 暂时为空的帧直接没有 spatial candidate，最终落到 `PLAYER / playerTile`。这样会绕过 §3.176 的关键合同：`TARGETING` fallback 到 player 时 identity 仍应是 `TARGETING`，只是 tooltip anchor invalid。结果会出现：

- entering targeting 的首帧可能不触发 targeting identity；
- closing targeting / inspect 时 snap 方向不可预测；
- nullable cursor fallback 测试写不出来，或测试与算法互相矛盾；
- `targetingFallbackUsesPlayerForViewportButDoesNotConfirmTarget` 无法稳定表达。

修复方向：

把 candidate 分成“mode candidate”和“anchor candidate”两层：

1. active spatial surface / spatial modal frame 只要存在，就产生 mode candidate，即使 cursor 为空。
2. cursor 非空只决定 `anchorTile`、`tooltipAnchorKind`、`isTooltipAnchorValid`，不决定 mode 是否存在。
3. fallback 到 `playerTile` 时保留 `resolvedMode = INSPECT/TARGETING`，但 `anchorTile = null`。

建议改写为：

```text
overlay-level targeting candidate：overlayState.mode == TARGETING 或 active targeting surface 存在；cursor 可为空。
overlay-level inspect candidate：overlayState.mode == INSPECT 或 active inspect surface 存在；cursor 可为空。
modal candidate：top-to-bottom spatial frame kind 存在即产生 candidate；local cursor 可为空。
cursor 非空只影响 anchor validity，不影响 candidate mode。
```

### P2

#### P2-1 `OverlayState` 和 `modalFrames` 在 projection request 中是双来源，缺少一致性规则

证据：

- `TileViewportFocusProjectionRequest` 同时包含 `overlayState: OverlayState` 和 `modalFrames: List<ModalFrame>`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:121-126`。
- 当前 `OverlayState` 自身已经持有 `modalFrames: List<ModalFrame>` 和 `activeModalKind`：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:58-84`。
- candidate 收集规则又要求扫描 request 里的 `modalFrames.asReversed()`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:158`。

问题：

这会让实现者猜到底以 `request.modalFrames` 为准，还是以 `request.overlayState.modalFrames` 为准。如果两者因为构造顺序、测试 fixture 或 renderer orchestration 传参不一致，projection owner 会出现第二 truth。

修复方向：

二选一写死：

- 推荐：request 只保留 `overlayState`，projection 从 `overlayState.modalFrames` 读取 modal stack；删除独立 `modalFrames` 字段。
- 如果必须保留 `modalFrames`，则明确 `modalFrames` 是唯一 modal source，`overlayState.modalFrames` 不得读取，并在 request 构造处 `check(modalFrames == overlayState.modalFrames)` 或构造一个去掉 modalFrames 的窄 `OverlayProjectionState`。

同时补测试：

- `rejectsDivergentOverlayAndRequestModalFrames`，或
- `usesRequestModalFramesAsSingleAuthority`。

#### P2-2 `mapDimensions = snapshot metadata 或 validated map model dimensions` 仍是二选一口径，缺 fail-fast 规则

证据：

- `TileMapViewportIdentity` 字段来源写为 `mapDimensions = TileMapDimensions(snapshot.metadata.width, snapshot.metadata.height) 或 validated map model dimensions`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:101`。

问题：

`snapshot.metadata.width/height` 与 render model/map cell extents 如果不一致，说明 upstream snapshot 或 model build 已经异常。文档现在允许实现者任选一个来源，可能把不一致悄悄吞掉。viewport 是后续 clipping、hit test、fog padding、out-of-map fill 的基础，不能用“或”来隐藏 contract drift。

修复方向：

改为：

```text
mapDimensions 的 primary source 是 snapshot.metadata.width/height。
如果实现从 TileRenderModel / map cell extents 派生 mapDimensions，必须与 snapshot.metadata.width/height 完全一致；不一致时 fail fast，并在 focused test 覆盖。
```

新增测试名建议：

- `rejectsMismatchedSnapshotAndModelMapDimensions`

#### P2-3 `TileViewportFocusSourceKind`、`ValidationProjectionReason` 没有冻结 enum 值，测试和 diagnostics 仍会各写各的

证据：

- projection result 定义了 `sourceKind: TileViewportFocusSourceKind`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:133-140`。
- validation projection 定义了 `reason: ValidationProjectionReason`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:128-131`。
- 文档只说 `reason` 至少区分 validation fixture / manual validation probe / debug whitebox projection：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:143`，没有列出 exact enum entries。

问题：

这是小而实际的开发缺口。既然文档要求 `sourceKind` 和 `reason` 进入 typed result，就必须冻结值域，否则实现、测试、manual record 会出现不同命名，例如 `MODAL_TARGETING` vs `TARGETING_MODAL`，`DEBUG` vs `DEBUG_WHITEBOX`。

修复方向：

在 `Viewport Focus Projection` 下新增 enum 合同：

```kotlin
enum class TileViewportFocusMode { PLAYER, INSPECT, TARGETING }

enum class TileViewportFocusSourceKind {
    PLAYER,
    OVERLAY_INSPECT,
    OVERLAY_TARGETING,
    MODAL_INSPECT,
    MODAL_TARGETING,
    MODAL_COMBAT_DECISION,
    VALIDATION_INSPECT,
}

enum class ValidationProjectionReason {
    VALIDATION_FIXTURE,
    MANUAL_VALIDATION_PROBE,
    DEBUG_WHITEBOX_PROJECTION,
}
```

如果命名可等价，manual record 必须列映射；但 PR-01-1 默认最好冻结 exact names。

#### P2-4 `TileMapLayerPlan.cursors` 没有定义 inspect 与 targeting 同时存在时的互斥/排序

证据：

- map layer order 只写 `player indicator -> inspect/targeting cursor -> combat feedback`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:188`。
- `TileMapLayerPlan` 只要求一个 `cursors` slice：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:190`。
- projection precedence 固定为 `TARGETING > INSPECT > PLAYER`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:152`。

问题：

如果 inspect frame 下方仍存在、上方进入 targeting/combat decision，或者旧 `inspectCursor` 没被清理，开发者需要猜：

- `cursors` slice 是否允许同时放 inspect cursor 和 targeting cursor；
- 如果允许，谁后画；
- 如果不允许，是否由 projection owner 抑制 losing cursor，还是由 composer 抑制；
- `TileLayerComposerTest.ordersMapSublayersForFogLootCursorAndCombatFeedback` 是否要覆盖该互斥。

修复方向：

在 §4 增加：

```text
cursors slice 只接收 active projection cursor。`TARGETING` resolved 时不得同时绘制 inspect cursor；`INSPECT` resolved 时不得绘制 targeting cursor。若后续需要同时显示 secondary cursor，必须新增 layer contract。
```

或者明确排序：

```text
cursors slice 内顺序固定为 inspect cursor -> targeting cursor，targeting cursor 后绘制。
```

从 ToME-like 可读性看，推荐互斥：active projection 只有一个 cursor。

### P3

#### P3-1 `TileMapLayerPlan` 默认方案已冻结，但测试命名仍说 `layerKey` 序列

证据：

- §4 已明确 sealed command list 不作为 PR-01-1 默认方案，默认输出是 `TileMapLayerPlan`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:190`。
- §8.18 仍写断言 stable `layerKey` 序列或 `TileMapLayerPlan` slice order：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:350`。

问题：

这不会阻断实现，但会让测试作者继续考虑不需要的 `layerKey`。既然默认方案已经固定，PR-01-1 的测试应优先锁 `TileMapLayerPlan` slice order。

建议：

改为：

```text
断言 `TileMapLayerPlan` slice order；只有未来 PR 替换为 sealed command list 时才改为 stable layerKey 序列。
```

#### P3-2 `ValidationInspectProjection.reason` 说明了用途，但没有要求写入 evidence

证据：

- 文档说 `reason` 只用于 evidence / diagnostics，不改变 viewport 规则：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:143`。
- manual record 必填 evidence label 中有 `dark-uiux-pr01-1-focus-projection-resolution`，但没有要求记录 validation projection reason：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:467-468`。

建议：

在 manual record 的 `Verification Source` 或 projection evidence 中要求列出 `ValidationProjectionReason`。这能防止 debug/manual/fixture 投影混在一起，尤其方便未来回查 validation 白盒证据。

#### P3-3 §6 影响范围仍只说 `TileViewportFocusProjection.kt` 解析 focus mode 与 focus tile，没有提 anchor validity 输出

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:236` 只写“解析为非空 viewport focus mode 与 focus tile”。
- §3 result 已要求 `anchorTile`、`tooltipAnchorKind`、`isTooltipAnchorValid`：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:133-140`。

建议：

把影响范围行补成“解析为 non-null viewport focus、nullable tooltip anchor validity 和 source diagnostics”。这属于小文档一致性，但能避免实现只交付 focus tile，漏掉 tooltip anchor validity。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| `FoundationViewportSupportTest` 进入执行命令 | §9 已加入 `com.ktome.client.screen.FoundationViewportSupportTest`；manual record 也要求列出方法名 | 一致 |
| viewport identity 字段和来源 | 已新增 `TileMapViewportIdentity`，禁止 `revision`；但 mapDimensions 来源仍有二选一 ambiguity | 部分一致 |
| projection request/result 可执行 | 已新增 request/result 和 no previous state 规则；但 candidate 激活与 fallback 冲突，modalFrames 双来源 | 部分一致 |
| validation explicit projection | 已有 request 字段和禁止 `validationPanel.inspectCursor` 直读；但 reason/source enum 值未冻结 | 部分一致 |
| map layer owner | `TileMapLayerPlan` 默认方案已冻结；cursor slice 互斥/排序未定义 | 部分一致 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| `TileViewportFocusProjection` | 当前帧 projection facts，不持 previous state，输出 mode/focus/anchor | 文档已细化，但 candidate 激活条件与 fallback 矛盾 | PR doc `:156-176` | active spatial mode cursor 空时会掉到 `PLAYER` | High |
| `TileViewportFocusProjectionRequest` | 单一 projection 输入合同 | 文档同时给 `OverlayState` 与 `modalFrames` | PR doc `:121-126`; `InputHandler.kt:58-84` | modal stack 双 truth | Medium |
| `TileMapViewportIdentity` | typed identity，禁止 revision | 基本完整，但 mapDimensions 源仍有二选一 | PR doc `:91-102` | metadata/model mismatch 可能被吞 | Medium |
| `ValidationInspectProjection` | validation 显式投影，不改变规则 | 有字段，无冻结 enum 值 | PR doc `:128-143` | 测试/证据命名会分叉 | Medium |
| `TileMapLayerPlan` | map sublayer typed owner | 默认方案已明确，cursor slice 细节不足 | PR doc `:188-190` | inspect/targeting cursor 同时存在时不确定 | Medium |

## 玩法与体验审查

### 核心循环

viewport identity 已经足够接近执行合同，但 `mapDimensions` 的二选一来源会影响地图边界、padding 和 out-of-map fog。这里必须 fail fast，不能让渲染层替上游 metadata/model mismatch 擦边。

### 战斗体验

targeting 是战斗操作最敏感的 UI 状态。active targeting 但 cursor 暂空时，玩家仍处于 targeting mental model；viewport 应保持 `TARGETING` identity 并只让 anchor invalid，而不是掉回 `PLAYER`。当前 candidate 规则会破坏这一点。

### 新手体验与信息反馈

fallback focus tile 与 anchor validity 已经分离，这是正确方向。但如果 candidate 仍要求 cursor 非空，tooltip 不显示的问题会被“看起来正常”掩盖，实际是 mode identity 错了，后续关闭 modal / snap 回 player 时才暴露。

### 系统耦合与体验断层

`OverlayState.modalFrames` 与 request `modalFrames` 双来源会让 InputHandler、ModalStack、TileRenderModel 三处边界继续纠缠。PR-01-1 的目标是拆出 owner，这里必须单一来源。

## 当前阶段必须解决的问题

1. 当前 Phase 必须修：candidate 激活条件不能依赖 cursor 非空。否则 nullable fallback 合同无法实现。
2. 当前 Phase 必须修：projection request 的 modal stack 只能有一个 authority。否则会重新引入 renderer/input 双真源。
3. 当前 Phase 必须修：mapDimensions metadata/model mismatch 必须 fail fast。否则 viewport 边界错误会被隐藏。
4. 当前 Phase 必须修：冻结 `TileViewportFocusSourceKind` / `ValidationProjectionReason` 值域。否则 focused tests 和 evidence 仍然要猜。
5. 当前 Phase 必须修：定义 `TileMapLayerPlan.cursors` 的互斥或内部顺序。否则 targeting/inspect 可视焦点可能冲突。

## Removal/Iteration Plan

### Defer Removal / Migration Required: modal stack duplicate source

| Field | Details |
| --- | --- |
| Location | `TileViewportFocusProjectionRequest` / `OverlayState.modalFrames` |
| Phase/Work Package | `dark-uiux-pr01-1` |
| Touched contract | viewport focus projection / modal stack authority |
| Evidence | request 同时持有 `overlayState` 和 `modalFrames`，当前 `OverlayState` 已自带 `modalFrames` |
| Preconditions | 文档先选择单一 modal source |
| Iteration steps | 1. 删除 request 独立 `modalFrames` 或改成窄 projection state 2. 增加 divergence test 3. manual record 写明 projection modal source |
| Affected gates | `TileViewportFocusProjectionTest`, `InputHandlerTest`, `ModalStackTest` |
| Rollback | 保持当前 `OverlayState` 为唯一来源；不要同时传两个不同 modal list |

## Additional Suggestions

1. `TileViewportFocusProjectionResult.sourceKind` 最好进入 manual record 的 `Verification Source` 表，至少对 validation / modal / overlay 三类来源可追溯。
2. `TileMapViewportIdentity` 的 `cellAlignedMapBounds` 建议在测试名中出现一次，例如 `changesIdentityWhenCellAlignedMapBoundsChanges`，避免只测 map dimensions。
3. `TileMapLayerPlan` 可以把 `cursors` 拆成 `inspectCursor` 和 `targetingCursor` nullable 字段；如果只允许 active projection 一个 cursor，则字段可命名 `activeCursor`，更不容易误画两个。

## Suggested Verification

本轮已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：`BUILD SUCCESSFUL`。该任务执行了 `syncPhase2Manifests`，并同步 canonical visual/audio manifest 到 runtime manifest。

本轮已执行 Markdown fence 检查：

```bash
awk 'BEGIN{c=0} /^```/{c++} END{print "FENCE_OPEN=" c%2}' UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md UI/pr/*.md UI/PLAN.md
```

结果：`FENCE_OPEN=0`。

本轮已执行 ASCII 删除扫描：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii' \
  client/src assets-src/image/specs assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
```

结果：0 命中，`rg` no-match 退出码为 `1`，符合文档预期。

本轮已执行 diff whitespace 检查：

```bash
git diff --check -- UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md UI/PLAN.md UI/pr/README.md
```

结果：通过。

文档修订后建议补跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

实现阶段再跑 §9 的 full client lane；当前相关 owner tests 尚未实现，直接跑会得到缺类/未实现失败，不能作为文档修订验证。

## Summary

Round 12 的结论是：文档主体已经可以指导实现，但 projection 这一块还差最后一层“机器可执行”的精确性。最需要修的是 active mode candidate 与 cursor fallback 的冲突；其次是 modal stack 双来源、mapDimensions mismatch fail-fast、enum 值域和 cursor layer 互斥。修完这些，PR-01-1 文档基本可以作为无需猜测的开发执行文档。
