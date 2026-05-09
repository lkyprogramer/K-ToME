# Dark UI/UX PR-01-1 深度 Review Round 10

目标文档：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查范围：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`
- `UI/pr/` 中与 viewport、overlay、map layer、PR-05 资源层级相关的后续 PR 文档
- 当前 `client` 侧已存在的 `InputHandler`、`TileRenderModel`、`TileLayerComposer` 形状，仅用于判断文档是否足够可开发

本轮结论：上一轮指出的关键合同大多已经吸收，当前没有 P0/P1 阻断。但仍有 3 个 P2 会直接影响实现分叉，另有若干 P3 会影响验收记录、focused test owner 和后续 PR 对齐。建议先修 P2，再进入 PR-01-1 实现。

## Findings

### P0

无。

### P1

无。

### P2

#### P2-1 `TileViewportFocusProjection` 没有定义 nullable cursor 的解析规则，实际开发会分叉

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:85` 要求 `TileMapViewport` 输入当前 focus tile。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:102-109` 要求 projection 输出 `PLAYER / INSPECT / TARGETING` 和 focus tile，但表格直接写 `inspectCursor`、`targetingCursor / target tile`。
- 当前 `OverlayState.targetingCursor` 与 `OverlayState.inspectCursor` 都是 nullable：`client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:72-75`。
- 当前渲染模型里也已经存在多条 fallback 路径，例如 `TileRenderModel` 使用 `modalFrames.lastOrNull()?.localState?.targetingCursor`、`validationPanel?.inspectCursor` 等。

问题：

PR-01-1 现在引入了 typed projection owner，但没有说明 projection 如何把 nullable runtime state 解析成 viewport 所需的非空 focus tile。实现者会出现至少三种互不兼容的写法：

1. 在 projection 中对 cursor 使用 `!!`，导致 modal pop / validation / targeting restore 的过渡帧崩溃。
2. fallback 到 `playerTile`，但 identity 是否仍保持 `INSPECT/TARGETING` 不明确，可能出现关闭 modal 前后重复 snap。
3. 从 `ModalFrame.localState` 或现有 overlay state 二次推导，和 `InputHandler` 当前 fallback 逻辑产生第二套真源。

建议修订：

在 `Viewport Focus Projection` 下新增 `Focus tile resolution` 子段，至少冻结：

- `PLAYER`: `focusTile = playerTile`。
- `INSPECT`: `overlayState.inspectCursor ?: topInspectFrame.localState.inspectCursor ?: defaultInspectCursor(snapshot)`，或明确选择 fail fast；不能留空。
- `TARGETING`: `overlayState.targetingCursor ?: topTargetingOrCombatFrame.localState.targetingCursor ?: playerTile`，并说明该 fallback 只服务 viewport 可视焦点，不替代命令确认路径的合法目标计算。
- projection 输出必须是非空 `focusTile`；若使用 fallback，identity 仍由 resolved `focusMode` 决定。
- focused test 增加 nullable cursor case：`resolvesInspectFocusFromModalLocalState`、`fallsBackTargetingFocusWithoutChangingCommandTargetAuthority` 或等价名称。

#### P2-2 `TileLayerComposer` 的输出模型没有定义，无法承载 fog、loot marker、cursor、combat feedback 这些非 sprite layer

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:123` 要求 `TileLayerComposer` 冻结完整 map sublayer order，且 `TileMapRenderer` 只消费 composer 输出。
- 当前代码中的 `TileLayerComposer.compose(model)` 只返回 `List<TileVisualPlacement>`，并且只包含 `terrainTiles`、`propTiles`、`overlayTiles`、`actorTiles`：`client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt:3-10`。
- PR-01-1 要纳入的 layer 包括 `fog/visibility veil`、`ground loot marker`、`player indicator`、`inspect/targeting cursor`、`combat feedback`，这些不是同一种 `TileVisualPlacement`。

问题：

文档已经把 layer-order owner 从 renderer draw path 收敛到 composer，这是正确方向。但没有冻结 composer 输出形状，开发时大概率会出现两类坏结果：

1. 为了满足“只消费 composer 输出”，把 fog fill、cursor rect、combat text 强行塞进 `TileVisualPlacement`，破坏 typed model。
2. `TileLayerComposerTest` 只覆盖 sprite 四层，其他非 sprite layer 顺序仍写在 `TileMapRenderer` draw path 内，形成第二套 layer authority。

建议修订：

在 §4 或 §8.17 增加 composer 输出合同，二选一即可：

- `TileMapLayerPlan`：包含命名 sublayer slices，例如 `terrainBase`、`props`、`spriteOverlays`、`actors`、`fogVeils`、`groundLootMarkers`、`playerIndicators`、`cursors`、`combatFeedback`。
- sealed command list：`TileMapLayerCommand.Sprite`、`FogFill`、`LootMarker`、`FocusIndicator`、`Cursor`、`CombatFeedbackText` 等，每个 command 带稳定 `layerKey`，测试断言 layer key 顺序，不用裸 ordinal。

同时要求 `TileMapRenderer` 不再维护并行的手写顺序；它只能按 `TileMapLayerPlan` 或 command list 顺序 draw。

#### P2-3 focus mode 变化全部 snap 到 player-centered，与 inspect/targeting focus tile 规则冲突

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:86` 写明 active focus mode 字段值变化后必须 snap 到 `player-centered + clamp state`。
- 同文档 `:89-91` 又要求 inspect / targeting 期间 deadzone 锚点是 cursor / target tile，关闭时 snap 回 player。
- `Viewport Focus Projection` 表格 `:107-108` 表示进入 `INSPECT/TARGETING` 会改变 identity。

问题：

“进入 inspect/targeting” 和“离开 inspect/targeting”都属于 focus mode change，但当前 §3.2 把两者都写成 `player-centered`。如果 targeting 初始焦点是远端合法目标，开发者按 §3.2 实现会先把 viewport 放回玩家中心，再由 deadzone 算法处理目标焦点。由于 identity change frame 又要求 snap 只基于 final state 计算一次，实际实现很容易出现：

- 远端 target 初始不可见或只在下一帧滚动。
- 进入 targeting 时 player-centered 与 focus-centered 两套预期打架。
- `TileMapViewportTest` 名称看起来通过，但玩家体验中 targeting first frame 抖动。

建议修订：

把 §3.2 拆成两类：

- `PLAYER -> INSPECT/TARGETING`：snap 基准为 resolved focus tile + clamp，或明确声明进入时保持 player-centered 且必须同帧 deadzone correction 保证 focus tile 可见。
- `INSPECT/TARGETING -> PLAYER`：snap 到 `player-centered + clamp state`，并写入 `lastPlayerTile/lastFocusTile=live playerTile`。

推荐选择第一种：进入 spatial mode 时 focus-centered，退出时 player-centered。这样与 ToME 类键盘 inspect/targeting 的首帧可读性更一致。

### P3

#### P3-1 PR-05 仍保留“不新增独立 `TileLayerComposerTest`”的降级口径

证据：

- PR-01-1 已经在 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:42`、`:123`、`:279`、`:317`、`:339` 多处把 `TileLayerComposerTest` 作为 map sublayer owner。
- PR-05 在 `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:52-53` 已列出 `TileLayerComposer.kt` 和 `TileLayerComposerTest.kt`。
- 但 PR-05 `:124` 仍写“如果仓库最终不新增独立 `TileLayerComposerTest`，必须在 `TileRendererCanvasTest` 中以同名测试覆盖...”

问题：

PR-05 是资源与 actor/VFX 扩展阶段，已经依赖 PR-01-1 的 layer owner。继续保留 fallback 会让后续实现者误以为 `TileLayerComposerTest` 仍是可选项，削弱 PR-01-1 刚刚冻结的 owner 边界。

建议修订：

删除 PR-05 `:124` 的可选 fallback，改为：

> `TileLayerComposerTest` 是 PR-01-1 冻结的 map sublayer owner。PR-05 只能在该 test 中追加 ground / wall / decal / actor / VFX / boss telegraph / Y-sort / loot marker 的覆盖，不得退回只用 `TileRendererCanvasTest` 承接 layer authority。

#### P3-2 manual evidence mapping 没有 map sublayer order 证据标签

证据：

- PR-01-1 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:382-400` 的必填 evidence label 列出了 viewport、tooltip、modal、ASCII、ToME layout 等。
- 但没有 `TileLayerComposerTest` 或 map sublayer order 对应标签。
- 同文档 `:40-42` 已把 `UI01-1-M03` 定义为 renderer 拆分与 map sublayer owner。

问题：

这会导致 PR close manual record 可以完整填写截图和 overlay 证据，却没有显式证明 map sublayer order 已由 owner test 固定。对后续 PR-05 来说，资源层级问题会变成“测试跑了但 evidence 不可追溯”。

建议修订：

在必填证据映射中增加：

| Evidence label | 来源步骤 |
| --- | --- |
| `dark-uiux-pr01-1-map-sublayer-order` | §8.17 / `TileLayerComposerTest.ordersMapSublayersForFogLootCursorAndCombatFeedback` |

同时在 manual record 模板中要求贴出该测试名或 focused lane 摘要。

#### P3-3 新增 projection owner 后，focused lane 没有独立覆盖 projection 解析

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:102` 新增 `TileViewportFocusProjection` 或等价 typed owner。
- `viewport focused lane` 目前只有 `TileMapViewportTest`、`GameShellLayoutTest` 和 `TileRendererCanvasTest*viewport*`：同文档 `:333-334`。
- `overlay focused lane` 有 `InputHandlerTest`：同文档 `:342-346`。
- 同文档 `:326` 说明 `InputHandlerTest` 只断言 mode / modal stack / focus transition，不复制 viewport 计算。

问题：

projection owner 既不属于纯 input，也不属于 viewport 数学。若没有 focused test，开发者可能把 projection 判断塞进 `TileRenderer`、`InputHandler` 或 `TileRenderModel`，只靠 canvas 测试间接证明，后续维护时容易漂移。

建议修订：

在 §9 增加 projection lane 或合并到 viewport lane：

```bash
./gradlew :client:test --tests com.ktome.client.render.TileViewportFocusProjectionTest --tests com.ktome.client.render.TileMapViewportTest
```

测试至少覆盖：

- `playerModeUsesPlayerTile`
- `inspectModeResolvesCursorFromOverlayOrModalState`
- `targetingModeResolvesCursorFromOverlayOrModalState`
- `nonSpatialModalPreservesCurrentSpatialProjection`
- `targetingProjectionWinsOverInspectProjection`
- nullable cursor fallback 行为

#### P3-4 `VALIDATION` 是否继续影响 world cursor 需要显式迁移决策

证据：

- PR-01-1 `Viewport Focus Projection` 表格 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:109` 把 `VALIDATION` 放在非 spatial frame 列表中，默认不单独触发 viewport recenter。
- 当前 `TileRenderModel` 仍有 `UiMode.VALIDATION -> overlayState.validationPanel?.inspectCursor` 的 world cursor / tooltip 相关逻辑。

问题：

这不一定是设计错误，但必须写清楚迁移意图。否则 validation fixture / debug overlay 可能继续认为 validation cursor 是 world-space inspect cursor，而 PR-01-1 实现却把它当作非 spatial modal，导致调试证据、tooltip anchor 和 viewport focus 不一致。

建议修订：

在 §3 或 §5 增补：

- `VALIDATION` 默认不改变 viewport focus identity。
- 若 validation fixture 需要 world-space cursor，只能显式投影为 `INSPECT`，并复用 `WORLD_TILE` anchor / `TileViewportFocusProjection`，不能走 validation-only cursor path。
- 对应测试放入 projection test 或 `TileOverlayLayerTest`。

## Requirement Alignment

| 维度 | 当前状态 | 风险 |
| --- | --- | --- |
| ToME map-first 信息架构 | 已明确地图主视觉、左右底三栏、right panel 四分区、bottom HUD 四子区 | 无新增阻断 |
| Viewport 数学 | deadzone、half-open、small map、jump snap、state 字段已明显收敛 | P2-3 仍需拆清进入/退出 spatial mode 的 snap 基准 |
| Renderer 拆分 | orchestration/frame/renderer 边界比上一轮清楚 | P2-2 composer output 未冻结，非 sprite layer 仍可能回流 draw path |
| Overlay 合同 | tooltip/modal/backdrop/layer order/edge mapping 已具备可测合同 | P2-1 projection nullable cursor 会影响 tooltip/world anchor 与 viewport |
| 后续 PR 对齐 | PR-05 已部分吸收 `TileLayerComposerTest` | P3-1 仍有降级口径 |
| 验收与证据 | manual evidence 已覆盖 viewport、overlay、ASCII、ToME layout | P3-2 缺 map sublayer evidence label；P3-3 缺 projection focused lane |

## 功能/系统一致性矩阵

| 系统切片 | 必须保持的长期方向 | 本轮发现 |
| --- | --- | --- |
| `TileMapViewport` | 唯一 tile-to-screen / visible range 真源 | 需要明确 focus tile 非空解析与 spatial mode snap 基准 |
| `InputHandler` / modal stack | 只产出 typed state 与 mode transition，不承接 viewport 数学 | projection test 缺失会诱导把判断分散回 input/render |
| `TileLayerComposer` | map sublayer authority | 输出模型未定义，会导致非 sprite layer 无处可放 |
| `TileMapRenderer` | 只消费 composer plan 并绘制 | 需要防止保留 renderer-local layer order |
| PR-05 assets/VFX | 扩展 PR-01-1 layer owner | 不能保留“没有独立 composer test”的 fallback |
| manual record | 用固定证据标签证明合同完成 | 需要补 map sublayer order 和 projection owner 证据 |

## 玩法与体验审查

1. focus mode 切换是玩家体验高敏感点。ToME 类游戏里 inspect / targeting 的首帧必须让焦点对象可读；如果进入 targeting 仍先 player-centered，远端目标、Boss telegraph 或路线选择会出现首帧不可见或抖动。
2. fog、loot marker、cursor、combat feedback 的相对顺序不是纯技术细节。它决定“危险是否压过装饰”“光标是否压过 loot”“战斗反馈是否压过 cursor”，必须由 typed owner 冻结，而不是散落在 draw 方法顺序里。
3. validation/debug cursor 的迁移必须清楚。它不是玩家正式玩法，但它会影响 golden、fixture 和人工证据，如果保留旧 cursor 语义却不写进 projection 合同，后续验收容易误判。

## 当前阶段必须解决的问题

建议 PR-01-1 进入实现前先改文档：

1. 补 `TileViewportFocusProjection` 的 nullable cursor resolution、fallback / fail-fast 策略、identity 输出规则和 focused test 名称。
2. 补 `TileLayerComposer` 输出模型合同，明确非 sprite layer 如何进入同一 layer authority。
3. 拆分 focus mode change 的进入/退出 snap 语义，避免 `player-centered` 与 cursor/target focus 冲突。

建议同步修后续文档：

1. 修改 PR-05，删除 `TileLayerComposerTest` 可选 fallback。
2. 给 PR-01-1 manual evidence label 增加 map sublayer order。
3. 在 §9 增加 `TileViewportFocusProjectionTest` 或等价 projection lane。

## Removal/Iteration Plan

最小修订顺序：

1. 在 PR-01-1 §3 的 `Viewport Focus Projection` 后追加 `Focus tile resolution`，并把 nullable cursor fallback 规则写成表格。
2. 修改 §3.2 / §3.7：进入 spatial mode 以 resolved focus tile 初始化，退出 spatial mode 以 player tile 初始化。
3. 在 §4 `Map sublayer order` 后追加 `Composer output shape`，冻结 `TileMapLayerPlan` 或 sealed command list。
4. 修改 §8 / §9：补 projection test、composer output test、manual evidence label。
5. 修改 PR-05 §7：把 `TileLayerComposerTest` 从可选 fallback 改为继承 PR-01-1 owner 的必选扩展点。

## Additional Suggestions

1. `TileViewportFocusProjection` 最好不要放在 `InputHandler` 包下。推荐放在 `client/render` 或 `client/render/viewport`，因为它的输出直接服务 viewport identity，不应把 renderer 计算反向塞进 input。
2. composer test 不要只断言 draw call 总顺序；应断言 stable layer key 序列，避免 texture batch flush、sprite sort 或 draw implementation 变化导致测试失去语义。
3. `TileMapLayerPlan` 如果采用 named slices，不建议暴露 mutable list。使用 immutable list / value object，renderer 只读消费。
4. manual record 中建议把 `TileViewportFocusProjection` 与 `TileLayerComposer` 都列入 `Verification Source`，否则后续 reviewer 很难从截图反推 owner 是否真的存在。

## Suggested Verification

本轮已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：`BUILD SUCCESSFUL`。注意该命令执行了 `syncPhase2Manifests`，并把 canonical manifests 同步到 runtime manifest。

本轮已执行 ASCII 删除扫描：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii' \
  client/src assets-src/image/specs assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
```

结果：0 命中。

本轮已执行 Markdown fence 检查：

```bash
awk 'BEGIN{c=0} /^```/{c++} END{print "FENCE_OPEN=" c%2}' UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md UI/pr/*.md UI/PLAN.md
```

结果：`FENCE_OPEN=0`。

建议文档修订后再跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.render.TileViewportFocusProjectionTest --tests com.ktome.client.render.TileMapViewportTest
./gradlew :client:test --tests com.ktome.client.render.TileLayerComposerTest --tests "com.ktome.client.render.TileRendererCanvasTest*layer*"
```

若 `TileViewportFocusProjectionTest` 尚未实现，PR-01-1 实现时必须新增或在文档中列出等价 owner test。

## Summary

Round 10 的主要风险已经不是宏观方向，而是开发落地细节：projection 如何从 nullable cursor 得到非空 focus tile、composer 如何承载非 sprite layer、进入/退出 inspect/targeting 时 snap 基准到底是谁。它们都属于 PR-01-1 实现前必须冻结的小合同，否则实现者很容易写出能跑但 owner 边界漂移的版本。
