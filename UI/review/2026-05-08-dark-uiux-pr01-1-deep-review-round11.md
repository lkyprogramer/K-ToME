# Dark UI/UX PR-01-1 深度 Review Round 11

目标文档：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查重点：确认文档是否已经变成可直接实施的开发执行文档，尤其检查输入/输出 owner、验证命令、后续 PR 继承口径、现有旧测试迁移和玩家可感知行为是否还需要开发者猜。

本轮结论：Round 10 的主要问题已经大体吸收，文档明显更接近可实施状态。但仍有 1 个 P1 和 4 个 P2 会让开发者在实现时做关键猜测，尤其是 `FoundationViewportSupportTest` gate 缺口、viewport identity 真源、projection preserve 语义、validation projection 输入模型和 tooltip anchor validity。

## Findings

### P0

无。

### P1

#### P1-1 `FoundationViewportSupportTest` 是必需 fastCheck，但没有进入正式验证命令，且当前测试仍断言旧行为

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:41` 把 `FoundationViewportSupportTest` 列为 `UI01-1-M02` 的 fastCheck。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:209-210` 明确要求 `FoundationViewportSupport` 迁移到 fixed shell world size，并删除或改写“snapshot 尺寸变化会更新 viewport world size”的旧断言。
- 但 §9 正式验证命令 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:334-338` 没有包含 `com.ktome.client.screen.FoundationViewportSupportTest`。
- 当前仓库测试 `client/src/test/kotlin/com/ktome/client/screen/FoundationViewportSupportTest.kt:14-36` 仍是旧行为测试：`syncViewport updates world size when snapshot dimensions change`，并断言 world size 跟随 larger snapshot。

影响：

这是执行文档层面的 gate 缺口。PR-01-1 的核心目标之一是让 shell viewport world size 与地图尺寸解耦；如果正式命令不跑 `FoundationViewportSupportTest`，开发者可能只实现 `GameShellLayoutTest / TileMapViewportTest`，但漏掉 `FoundationGameScreen` 上真正撑大 camera world 的旧路径。这个缺口会直接影响大地图首屏、resize、golden 和客户端可玩性。

修复方向：

1. 在 §6 影响范围中新增一行：
   - `client/src/test/kotlin/com/ktome/client/screen/FoundationViewportSupportTest.kt`：改写为 fixed shell world size、resize 不随 snapshot map dimensions 扩张、map rect 与 large map 解耦。
2. 在 §9 全量命令和 `viewport focused lane` 中加入：

```bash
--tests com.ktome.client.screen.FoundationViewportSupportTest
```

3. 在测试命名表中加入可执行 test name，例如：
   - `keepsWorldSizeFixedWhenSnapshotDimensionsChange`
   - `keepsViewportWorldSizeFixedAcrossResize`
   - `usesShellMapBoundsInsteadOfFullMapPixels`
4. 在 manual record `Verification Source` 中给 `UI01-1-M02` 单独列 `FoundationViewportSupportTest` 的方法名和 artifact。

### P2

#### P2-1 `TileMapViewportState` identity 写了 `zone/session id`，但没有定义输入字段和来源

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:85` 的 `TileMapViewport` 输入列表没有 identity / zone / floor 字段。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:88` 又要求 state identity 至少包含 `zone/session id`、cell size、cell-aligned mapBounds、mapDimensions 和 active focus mode。
- 当前 `RenderMetadataSnapshot` 有 `revision`、`zoneId`、`currentFloor`、`width`、`height`、player 坐标等字段：`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:22-37`。

问题：

开发者现在必须猜 `zone/session id` 到底是什么：

- 如果只用 `zoneId`，同一 zone 下切 floor 时可能复用上一层的 viewport state。
- 如果误用 `metadata.revision`，每个 turn 都会 identity 变化，viewport 会频繁 recenter，死区合同失效。
- 如果使用外部 session id，但文档没有指定来源，`TileMapViewport` 会被迫接收 renderer-local 临时 identity。

修复方向：

在 §3 增加 typed identity 合同，例如：

```kotlin
data class TileMapViewportIdentity(
    val zoneId: String,
    val currentFloor: Int,
    val mapDimensions: TileMapDimensions,
    val cellSize: Int,
    val cellAlignedMapBounds: RectInt,
    val focusMode: TileViewportFocusMode,
)
```

并明确：

- `zoneId = snapshot.metadata.zoneId`
- `currentFloor = snapshot.metadata.currentFloor`
- `mapDimensions = snapshot.metadata.width/height` 或 validated map model dimensions
- 禁止使用 `snapshot.metadata.revision` 作为 viewport identity
- 如果后续引入 run/session id，只能作为额外字段，不替代 `zoneId + currentFloor`

#### P2-2 `TileViewportFocusProjection` 同时被要求输出 identity change，但没有 previous projection / previous state 输入

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:102` 要求 projection 输出 `PLAYER / INSPECT / TARGETING`、focus tile 和“是否发生 identity change”。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:88` 又把 active focus mode 放入 `TileMapViewportState` identity，由 viewport state 负责 identity 丢弃。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:109` 还要求非 spatial modal “保持当前 spatial projection”。

问题：

projection 如果只消费当前 input / overlay / modal typed state，它无法可靠判断“是否发生 identity change”，也无法知道“当前 spatial projection”是上一帧结果、modal stack 下层 frame，还是当前 overlay state 中仍 active 的 spatial state。实现者会在三个位置中选一个：

1. projection 内部读取上一帧 projection。
2. viewport 用 `prevState.lastFocusMode` 比较。
3. renderer/orchestration 保存一个额外 projection cache。

这三种会形成不同 owner，文档目前没有裁决。

修复方向：

把 projection 和 viewport 的责任拆清：

- `TileViewportFocusProjection` 只输出 resolved `mode`、`focusTile`、`sourceKind`、`isTooltipAnchorValid` 等当前帧事实。
- `TileMapViewport` / orchestration 用 `TileMapViewportIdentity` 与 `prevState.identity` 比较，决定是否 identity change。

或者如果坚持让 projection 输出 identity change，就必须定义输入：

```kotlin
data class TileViewportFocusProjectionRequest(
    val playerTile: Point,
    val overlayState: OverlayState,
    val modalFrames: List<ModalFrame>,
    val validationInspectProjection: Point?,
    val previousProjection: TileViewportFocusProjection?,
)
```

推荐前一种：projection 不拥有 previous state，只产出当前帧 resolved projection。

#### P2-3 “保持当前 spatial projection”缺少可执行算法，non-spatial modal 会再次变成实现猜测

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:109` 写 `INVENTORY / ITEM_DETAIL / ... / VALIDATION / WORLD_MAP` 保持当前 spatial projection。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:111` 又写 `ModalStack.top()` 不能单独决定 viewport focus mode。
- 当前 `InputHandler` 已有 `ModalFrameKind.toUiMode()`，`ITEM_DETAIL / ITEM_COMPARE` 会投影到 `UiMode.INVENTORY`，`COMBAT_DECISION` 会投影到 `UiMode.TARGETING`：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1812-1826`。

问题：

“保持当前 spatial projection”不是一个可直接实现的算法。开发者仍要猜：

- 扫描 `modalFrames` top-down 找第一个 spatial frame？
- 使用 `overlayState.mode`？
- 使用上一帧 projection？
- 当 top 是 `ITEM_DETAIL`、下层是 `INSPECT` 时是否保留 inspect？
- 当 top 是 `ITEM_DETAIL`、没有下层 spatial frame 时是否回 player？

修复方向：

在 `Viewport Focus Projection` 下追加明确算法，建议如下：

1. 收集 spatial candidates：
   - overlay-level targeting state
   - overlay-level inspect state
   - `modalFrames.asReversed()` 中的 `TARGETING / COMBAT_DECISION / INSPECT`
   - explicit validation inspect projection
2. 按 `TARGETING > INSPECT > PLAYER` 选择 resolved mode。
3. non-spatial modal 只是不新增 candidate，也不遮蔽下层 spatial candidate。
4. 如果没有任何 spatial candidate，结果为 `PLAYER / playerTile`。
5. 禁止使用上一帧 projection 来维持 non-spatial modal 状态，除非文档显式把 `previousProjection` 加入 request。

这能让 `nonSpatialModalPreservesCurrentSpatialProjection` 变成可写测试，而不是口号。

#### P2-4 validation explicit projection 被写入 resolution order，但没有定义 typed 输入模型

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:120` 的 `INSPECT` resolution order 包含 `explicit validation inspect projection`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:123` 要求 validation fixture 若需要 world-space cursor，必须显式投影为 `INSPECT`。
- 影响范围 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:183-194` 只说 projection 解析 validation explicit projection，但没有说明该 projection 来自哪个字段、哪个 request、哪个类型。
- 当前代码存在旧 path：`TileRenderModel` 仍会从 `overlayState.validationPanel?.inspectCursor` 读取 focus：`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:624-630`、`:1382-1388`。

问题：

文档禁止 validation-only cursor path 成为第二 authority，这是对的。但“explicit validation inspect projection”本身还没有类型。开发者可能直接继续读 `overlayState.validationPanel?.inspectCursor`，只是换个名字，实际仍是 validation-only path。

修复方向：

定义一个明确的 request 字段或 typed wrapper，例如：

```kotlin
data class TileViewportFocusProjectionRequest(
    val validationInspectProjection: ValidationInspectProjection? = null,
)

data class ValidationInspectProjection(
    val cursor: Point,
    val reason: ValidationProjectionReason,
)
```

并写清：

- `validationPanel.inspectCursor` 不能被 renderer/model 直接用于 viewport 或 tooltip anchor。
- 只有 `TileViewportFocusProjectionRequest.validationInspectProjection` 非空时，validation 才作为 `INSPECT` candidate。
- `TileViewportFocusProjectionTest.validationCursorRequiresExplicitInspectProjection` 必须同时覆盖“无 explicit projection 时 VALIDATION 不改变 viewport”和“有 explicit projection 时进入 INSPECT”。

#### P2-5 focus tile fallback 与 tooltip anchor validity 没有完全分离

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:115` 要求 projection 输出非空 `focusTile`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:120-121` 允许 `INSPECT/TARGETING` fallback 到 `playerTile`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:154-156` 又要求 tooltip source 由 typed anchor 决定，anchor source 失效时必须隐藏 tooltip，不允许 fallback 到屏幕中心。

问题：

fallback 到 `playerTile` 对 viewport 是合理的，但它不应自动生成 tooltip anchor。否则一个临时缺失的 inspect cursor 会让 tooltip 在玩家脚下出现，违反“anchor source 失效时隐藏”的 overlay 合同。

修复方向：

在 projection result 中分离：

- `focusTile`: non-null，用于 viewport。
- `anchorTile`: nullable，仅当真实 inspect/targeting/world anchor 有效时存在。
- `isTooltipAnchorValid` 或 `tooltipAnchorKind`: 明确 fallback tile 不产生 passive tooltip。

测试新增：

- `inspectFallbackUsesPlayerForViewportButSuppressesTooltipAnchor`
- `targetingFallbackUsesPlayerForViewportButDoesNotConfirmTarget`

### P3

#### P3-1 §3.1 仍把 `TileMapViewport` 输入写成 raw cursor，容易绕过 projection owner

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:85` 写 `INSPECT=inspectCursor`、`TARGETING=targetingCursor/targetTile`。
- 后文已经引入 `TileViewportFocusProjection` 和 `Focus Tile Resolution`。

建议：

把 §3.1 改为：

> `TileMapViewport` 输入为 `mapDimensions`、`mapBounds`、`cellSize`、`playerTile`、`TileViewportFocusProjectionResult.resolvedMode`、`resolvedFocusTile`、`TileMapViewportIdentity` 和上一帧 `TileMapViewportState`。

这样能防止实现者绕过 projection 直接把 raw cursor 塞给 viewport。

#### P3-2 PR-05 仍有“等价 canvas 层级断言”的残留措辞

证据：

- `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:104` 写 actor Y-sort、ground loot marker 与 boss telegraph 必须有 `TileLayerComposerTest` 或等价 canvas 层级断言。
- 同文档 `:124` 已修正为 `TileRendererCanvasTest` 只作为最终 canvas wiring 和 non-overlap 佐证。

建议：

把 `:104` 改成：

> 必须由 `TileLayerComposerTest` 或等价 composer-owner test 锁定；`TileRendererCanvasTest` 只能作为最终 canvas wiring 佐证。

这能避免 PR-05 实施者只补 canvas test 而不扩展 composer owner。

#### P3-3 Acceptance Matrix 的 whitebox 字段低估了现在的 required evidence

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:42` 中 `UI01-1-M03` whitebox 是 `doc-only self-audit`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:45` 中 `UI01-1-M06` whitebox 是 `N/A`。
- 但 §10.17 要求提交 `TileViewportFocusProjectionTest` 和 `TileLayerComposerTest` focused lane 摘要，§4.16 要求 frame ownership self-audit。

建议：

- `UI01-1-M03` whitebox 改为 `required: layer focused lane + frame ownership self-audit`。
- `UI01-1-M06` whitebox 改为 `required: frame ownership self-audit`。

这不是功能 bug，但会影响 PR close 时 reviewer 按矩阵判断证据是否缺失。

#### P3-4 `TileMapLayerPlan` 与 sealed command list 二选一仍留给开发者决策

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:137` 允许 `TileMapLayerPlan` 或 sealed command list 两种输出形状。

建议：

如果目标是“不需要猜测的开发执行文档”，建议直接指定默认实现为 `TileMapLayerPlan`，sealed command list 只作为未来 PR 在确有 heterogeneous command 需求时替换。当前 layer 集合是固定有限集合，named slices 更容易审查，也更容易让 `TileLayerComposerTest` 断言 slice order。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| viewport world size 与完整地图尺寸解耦 | 文档 §1.2、§6、§7.1 已要求；但 §9 未跑 `FoundationViewportSupportTest`，当前测试仍锁旧行为 | 部分一致 |
| `TileMapViewport` 作为唯一 viewport 数学 owner | 文档 §3 已冻结 deadzone、clamp、小地图、jump snap；identity 输入未定义 | 部分一致 |
| `TileViewportFocusProjection` 作为 focus projection owner | 文档已新增 resolution 与 test；但 identity-change、preserve algorithm、validation projection input 和 anchor validity 仍缺 | 部分一致 |
| `TileLayerComposer` 作为 map layer owner | 文档已定义 typed plan / command 输出与 tests；PR-05 仍有 canvas 等价措辞 | 部分一致 |
| overlay tooltip/modal layer 可验证 | 文档 edge mapping、suppression、typed anchor、manual evidence 已较完整；fallback anchor validity 需补 | 部分一致 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| `FoundationViewportSupport` | fixed shell world size，不再随 snapshot map dimensions 扩张 | 当前实现/测试仍是旧行为，文档要求迁移但命令未覆盖 test | `FoundationViewportSupportTest.kt:14-36`；PR doc `:209-210`、`:334-338` | gate 缺口会漏掉核心 viewport 解耦 | High |
| `TileMapViewportIdentity` | zone/session + layout + focus mode 变化丢弃 prev state | 文档未定义 typed identity 输入 | PR doc `:85`、`:88`；`RenderSnapshot.kt:22-37` | floor/revision 选择会影响死区稳定性 | Medium |
| `TileViewportFocusProjection` | 当前帧 resolved focus mode/tile owner | 文档仍混合 current output 与 identity change 判断 | PR doc `:102`、`:109`、`:120-123` | previous state owner 不清 | Medium |
| `TileOverlayAnchorKind` / tooltip | anchor source 失效隐藏，不用 fallback 坐标 | fallback focus tile 与 tooltip anchor validity 未分离 | PR doc `:115`、`:120-121`、`:154-156` | tooltip 可能错误出现在 player tile | Medium |
| PR-05 layer extension | 继承 PR-01-1 composer owner | 已大体修正，仍有 canvas 等价残留 | PR-05 `:104`、`:124` | 后续资源 PR 可能弱化 owner | Low |

## 玩法与体验审查

### 核心循环

viewport identity 如果误用 `revision`，玩家每走一步都可能被强制 recenter，死区设计失效；如果只用 `zoneId`，换楼层时可能继承旧 viewport，首屏位置异常。这会直接破坏探索移动的稳定感。

### 战斗体验

targeting/combat decision 的 focus projection 需要严格区分“viewport 可视焦点”和“合法目标确认”。fallback 到 player 只能保证不崩溃，不能让确认路径误认为 player 是合法 target。

### 新手体验与信息反馈

tooltip anchor validity 不清会制造错误反馈：玩家明明没有有效 inspect/targeting anchor，却可能在玩家脚下看到 tooltip。这类问题比纯布局偏差更伤害理解成本。

### 系统耦合与体验断层

`FoundationViewportSupportTest` 缺少正式命令覆盖，会让 layout、viewport、screen 三层各自“看起来通过”，但真正的 `FoundationGameScreen` camera world 仍可能跟地图尺寸耦合。

## 当前阶段必须解决的问题

1. 当前 Phase 必须修：把 `FoundationViewportSupportTest` 加入 §9 全量命令和 viewport focused lane，并在影响范围写出该测试的迁移目标。不能推迟，因为 PR-01-1 的核心目标就是 world size 解耦。
2. 当前 Phase 必须修：定义 `TileMapViewportIdentity` 的字段与来源，明确禁止 `metadata.revision`。不能推迟，因为 identity 直接决定 deadzone 是否稳定。
3. 当前 Phase 必须修：拆清 projection result 与 viewport identity-change owner。不能推迟，因为否则 renderer/orchestration 会产生 hidden cache。
4. 当前 Phase 必须修：定义 validation explicit projection typed 输入。不能推迟，因为当前代码已有 validation cursor 旧路径，容易被原样保留成第二 authority。
5. 当前 Phase 必须修：把 fallback focus tile 与 tooltip anchor validity 分离。不能推迟，因为它影响实际玩家反馈。

## Removal/Iteration Plan

### Defer Removal / Migration Required: snapshot-size-driven viewport support

| Field | Details |
| --- | --- |
| Location | `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt` / `client/src/test/kotlin/com/ktome/client/screen/FoundationViewportSupportTest.kt` |
| Phase/Work Package | `dark-uiux-pr01-1` |
| Touched contract | client viewport world size / shell map bounds |
| Evidence | PR doc §6 / §7.1 要求 fixed shell world size；当前 test 仍断言 snapshot dimensions 改变会更新 viewport world size |
| Preconditions | §9 命令加入 `FoundationViewportSupportTest`；文档写出新 test names |
| Iteration steps | 1. 定义 shell world size token 2. 改写 `FoundationViewportSupport` 3. 改写旧 test 4. 用 `GameShellLayoutTest` + `FoundationViewportSupportTest` 锁 screen/layout 接缝 |
| Affected gates | `:client:test --tests com.ktome.client.screen.FoundationViewportSupportTest`、viewport focused lane、`clientSmoke`、`goldenScreenshot` |
| Rollback | 恢复该局部实现；不得恢复 snapshot-size-driven contract 作为兼容路径 |

## Additional Suggestions

1. 给 `TileViewportFocusProjectionResult` 明确字段名，例如 `mode`、`focusTile`、`source`、`anchorTile`、`isTooltipAnchorValid`。这会让测试名和 renderer 消费都更直接。
2. `TileMapViewportIdentity` 建议作为单独 value/data class，不要散落在 `TileMapViewportState` 构造参数里。
3. `TileLayerComposerTest` 建议断言 layer keys 或 named slices，不断言 draw call 总数；draw call 总数容易因 chrome 或 debug hint 变化而失去语义。
4. PR-05 可以在 §6 必测行为里显式引用 PR-01-1 的 `ordersMapSublayersForFogLootCursorAndCombatFeedback`，避免资源 PR 自己发明新 test 名。

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
./gradlew :client:test --tests com.ktome.client.screen.FoundationViewportSupportTest --tests com.ktome.client.render.TileViewportFocusProjectionTest --tests com.ktome.client.render.TileMapViewportTest --tests com.ktome.client.render.GameShellLayoutTest
./gradlew :client:test --tests com.ktome.client.render.TileLayerComposerTest --tests "com.ktome.client.render.TileRendererCanvasTest*layer*"
./gradlew acceptanceContractLint
```

## Summary

当前文档已经不是方向性方案，而是接近执行合同。但如果目标是“开发者不需要猜”，还必须把 `FoundationViewportSupportTest` gate、`TileMapViewportIdentity`、projection request/result、validation explicit projection 和 tooltip anchor validity 写成具体类型/字段/命令。否则开发时最可能出错的不是视觉细节，而是 viewport state、projection ownership 和 screen viewport 接缝。
