# Dark UI/UX PR-01-1 Deep Review Report Round 9

日期：2026-05-08

审查对象：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查口径：基于 round8 反馈后的当前文档版本，继续按“最细小问题也标出，尤其关注实际开发会卡住的细节”进行复审。对照范围包括 `UI/PLAN.md`、`UI/pr/README.md`、PR-02/03/04/05/06/07 后续文档、当前 client viewport / modal / layer 实现现状，以及 dark UI/UX repo-owned governance。

本轮结论：**无 P0 / P1。Round8 的核心问题已被吸收；剩余 3 个 P2 和 4 个 P3。** 这些问题不会推翻 PR-01-1 的长期方向，但会影响实现者能否直接按文档写出正确的 `TileMapViewport`、overlay focus mode、map sublayer order 和 focused test。

已运行验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：`BUILD SUCCESSFUL`。注意该 task 会执行 `syncPhase2Manifests`，本报告不把由该 task 同步产生的 manifest 变更视为本轮人工编辑内容。

已运行 ASCII 删除扫描：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii' \
  client/src assets-src/image/specs assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
```

结果：退出码 `1`，即当前扫描路径内 0 命中。

## Findings

### P0

未发现。

### P1

未发现。

### P2

#### 1. Jump snap 需要 `previousPlayer`，但 `TileMapViewport` 输入和 state 合同没有承载它

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:85` 定义 `TileMapViewport` 输入为 `mapDimensions`、`mapBounds`、`cellSize`、`playerTile`、`inspectCursor` 或等价 focus tile、上一帧 `TileMapViewportState`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:88` 只把 `viewportTopLeft` 与 `visibleRange` 定为 state 的非 identity 持久字段。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:92` 要求用 `dx = newPlayer.x - previousPlayer.x`、`dy = newPlayer.y - previousPlayer.y` 判断 teleport / respawn / reposition jump snap。

影响：

实现者按当前合同写 `TileMapViewport.resolve(...)` 时拿不到 `previousPlayer`。如果临时从 `viewportTopLeft`、deadzone 边界或 `visibleRange` 反推，会把“玩家正常走出死区”和“同 zone teleport”混在一起；如果在 `TileRenderer` 或 `FoundationGameScreen` 外挂上一帧 player 变量，又会绕开 `TileMapViewportState` 这个唯一 viewport truth。这个缺口会直接影响 §8.10 的 `== threshold`、per-axis 和 diagonal jump snap 测试。

修复方向：

把上一帧 focus/player tile 纳入 typed 合同，二选一即可：

- 在 `TileMapViewportState` 增加非 identity 字段，例如 `lastPlayerTile`、`lastFocusTile`、`lastFocusMode`，identity 变化时丢弃，identity 一致时用于 jump snap。
- 或把 resolver 输入显式扩成 `previousPlayerTile` / `previousFocusTile`，但必须声明该值只能来自上一帧 `TileMapViewportState`，不能来自 renderer-local 临时缓存。

同时补充首帧、identity 变化帧、inspect/targeting 关闭帧如何更新这些字段。

#### 2. Active focus mode 没有映射到现有 `UiMode` / `ModalFrameKind`，实现会在 modal 与 inspect 之间摇摆

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:86` 引入 active focus mode 值 `PLAYER / INSPECT / TARGETING`，并把它放进 viewport identity。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:91` 要求 inspect / targeting 关闭时 snap 回 player-centered。
- 当前代码已有更细的输入和 modal 状态：`UiMode` 包括 `MAP / SHOP / WORLD_MAP / INVENTORY / LOADOUT_EDIT / TARGETING / INSPECT / VALIDATION / STAT_ASSIGN / TALENT_ASSIGN`，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:34`；`ModalFrameKind` 包括 `INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / ACTIVE_TALENT_SLOT_CHOICE / INSPECT / TARGETING / ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION`，见 `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:8`。
- 当前 input 映射中 `ModalFrameKind.INSPECT -> UiMode.INSPECT`，`TARGETING / COMBAT_DECISION -> UiMode.TARGETING`，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1812`。

影响：

文档没有说明 active focus mode 从 `OverlayState.mode`、`ModalStack.top()` 还是 `PaneFocusAnchor` 推导。开发者容易在打开 `ITEM_DETAIL`、`ITEM_COMPARE`、`ACTIVE_TALENT_SLOT_CHOICE` 这类 modal 时误把 viewport identity 改成新的 mode，导致每次开关 modal 都 recenter；也可能把 `INSPECT` 既当 modal frame 又当 pane mode，导致 §5.5 的 inspect cursor 可见、tooltip suppressed、关闭后 snap 三件事互相冲突。

修复方向：

在 §3 或 §5 增加一张映射表，明确：

- `UiMode.INSPECT` 或 `ModalFrameKind.INSPECT` 存在时，viewport focus mode = `INSPECT`，focus tile = `inspectCursor`。
- `UiMode.TARGETING`、`ModalFrameKind.TARGETING`、`ModalFrameKind.COMBAT_DECISION` 存在时，viewport focus mode = `TARGETING`，focus tile = `targetingCursor` / target tile。
- `INVENTORY / ITEM_DETAIL / ITEM_COMPARE / TALENT_ASSIGN / ACTIVE_TALENT_SLOT_CHOICE / SHOP / LOADOUT_EDIT / STAT_ASSIGN / VALIDATION` 默认不改变 viewport focus mode，除非它们显式投影到 inspect/targeting。
- modal open/close 只影响 overlay z-order；只有 focus mode 从 `INSPECT/TARGETING` 回到 `PLAYER` 时才触发 §3.7 snap。

#### 3. PR-01-1 创建 `TileMapRenderer`，但 map 内部子层顺序仍留给 PR-05，当前实现会产生双 owner

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:108` 要求 `TileMapRenderer` 负责 terrain / prop / tile sprite overlay / actor / fog / loot marker / player tile indicator / inspect cursor / targeting cursor / combat feedback。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:121` 只把整个 map pass 粗略写成 `map terrain/props/actors/combat-feedback`，没有冻结 fog、loot marker、cursor、player indicator、telegraph / VFX 与 combat feedback 的内部顺序。
- PR-05 后续文档又要求 `TileLayerComposer` 或等价测试固定 VFX、telegraph、actor、ground loot marker 层级，见 `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:89` 和 `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:104`。
- 当前实现已经存在 `TileLayerComposer`，但只组合 `terrainTiles -> propTiles -> overlayTiles -> actorTiles`，见 `client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt:3`；fog、loot marker、cursor、combat feedback 仍在 `TileRenderer` 其他路径里绘制。

影响：

如果 PR-01-1 只拆出 `TileMapRenderer` 而不冻结最小 map sublayer contract，PR-05 再补 `TileLayerComposer` 时会出现两个 owner：PR-01-1 的 renderer pass 顺序和 PR-05 的 composer 顺序。最容易出问题的是 actor 与 ground loot marker、fog 与 cursor、combat feedback 与 modal 的相对关系；这些正是 ToME-like 战斗可读性和 PR-05 资源替换最敏感的部分。

修复方向：

PR-01-1 至少冻结一个最小 map sublayer 顺序，或者明确把顺序 owner 命名为 `TileLayerComposer` 并要求 `TileMapRenderer` 只消费 composer 输出。建议补：

```text
terrain/base -> prop/decal -> tile sprite overlay/telegraph/vfx -> actor -> fog/visibility veil -> ground loot marker -> player indicator -> inspect/targeting cursor -> combat feedback
```

如果最终顺序不同，也必须在 PR-01-1 明确哪一类测试负责锁定，避免 PR-05 才发现 renderer 拆分方向和 layer composer 不兼容。

### P3

#### 1. 小地图居中把 `viewportTopLeft` 写成“居中结果”，会诱导负 top-left 或半格 top-left

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:94` 写的是 `mapDimensions < visibleCells` 的轴 `viewportTopLeft` 固定为居中结果。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:95` 又规定剩余像素作为 inner padding 居中处理。

影响：

`viewportTopLeft` 是 tile index，不适合表达小地图居中的像素偏移。实现者可能为了“居中结果”把 top-left 写成负数、半格或虚拟 tile offset，后续 `containsTile`、hit test、visibleRange 和 out-of-map fog 都会变复杂。

修复方向：

把 §3.10 改成：小地图轴 `viewportTopLeftAxis = 0`、`visibleRangeAxis = 0 until mapDimensionAxis`；居中只通过 `innerPaddingAxis = floor((cellAlignedMapBoundsAxis - mapDimensionAxis * cellSize) / 2)` 表达，deadzone 与 jump snap 在该轴短路。

#### 2. Tooltip flip 的公式仍是泛化占位，缺少可直接实现的四方向 edge mapping

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:138` 写 `tooltipEdge = anchorEdge + sign(direction) * tooltipFlipMargin`，但没有说明 right/down/left/up 分别使用 anchor 的哪条边、tooltip 的哪条边。

影响：

LibGDX Y-up 坐标下，`down` 和 `up` 最容易被写反；`left` 也容易变成从 anchor center 偏移。PR-01-1 的 tooltip placement 是后续 PR-03 shop row、PR-04 talent node、PR-07 reward/frontstage card 的共同基础，这里如果靠实现者猜，会在后续 PR 产生坐标口径漂移。

修复方向：

在 §5.7 直接补四条公式：

- right: `tooltip.left = anchor.right + margin`
- down: `tooltip.top = anchor.bottom - margin`
- left: `tooltip.right = anchor.left - margin`
- up: `tooltip.bottom = anchor.top + margin`

并说明每次候选 placement 后都用完整 tooltip rect 检查 `shellContentBounds - bottomLogReservedBounds`。

#### 3. Jump snap 必测五类场景没有完全进入测试命名表

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:251` 要求 jump snap focused tests 覆盖 `== threshold`、仅 X 超过、仅 Y 超过、diagonal 两轴均未超过、diagonal 任一轴超过五类场景。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:307` 的 viewport 命名表包含 `snapsWhenHorizontalJumpExceedsThreshold`、`snapsWhenVerticalJumpExceedsThreshold`、`keepsDiagonalMoveWhenNeitherAxisExceedsThreshold`，但没有能反推出 `== threshold` 和 `diagonal 任一轴超过` 的名称。

影响：

这会削弱文档自己要求的“测试命名必须能从失败名反推出合同”。实现者可能写了覆盖，但失败时 reviewer 无法从方法名判断到底保护的是 threshold equality，还是普通 deadzone exit。

修复方向：

在 §9 命名表补两个名称，例如：

- `keepsThresholdEqualJumpOnDeadzonePath`
- `snapsWhenDiagonalAnyAxisExceedsThreshold`

#### 4. `INSPECT` 在 modal / mode / pane 三种语义之间仍有轻微不一致

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:141` 列出的 sealed `ModalFrameKind` frame kind 不包含 `INSPECT`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:266` 又把 `INSPECT` 放进“打开后 `ESC / Backspace` 等 `ModalStack` 语义不回退”的列表。
- 当前代码实际存在 `ModalFrameKind.INSPECT`，见 `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:13`，且 `InputHandler` 会按 `X` 打开该 modal frame，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:425`。

影响：

这不是大方向错误，但会影响开发者判断：inspect 到底是 modal frame、pane mode，还是只属于 overlay tooltip source。如果后续 PR-06/07 写 Look/Inspect evidence 时按不同解释实现，关闭路径、tooltip suppression 和 viewport snap 会出现不一致。

修复方向：

把 §5.10 改成两段：

- “现有 frame kind 包括 `INVENTORY / INSPECT / TARGETING / TALENT_ASSIGN / ...`，PR-01-1 保留其关闭语义。”
- “本 PR 新增或冻结的 detail frame kind 包括 `ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION / ACTIVE_TALENT_SLOT_CHOICE`，不得新增对应 `UiMode`。”

这样既不误删现有 `INSPECT`，也不鼓励为 item/combat/talent 新造 mode。

## Requirement Alignment

- Requirement: PR-01-1 要冻结 player-centered deadzone viewport、jump snap、small map centering 和 shell world size 解耦。
- Evidence: §3 已补充 half-open deadzone、per-axis jump snap、cell alignment、out-of-map fog；但 jump snap 缺 previous tile state，small-map top-left 表述仍会误导实现。
- Conclusion: 部分一致。

- Requirement: PR-01-1 要把 `TileRenderer` 收敛成 orchestration，并拆出 map/shell/overlay typed renderer。
- Evidence: §4 明确 frame ownership、SpriteBatch 边界、hot-loop discipline；但 map 内部子层 order 与 PR-05 `TileLayerComposer` owner 仍未接上。
- Conclusion: 部分一致。

- Requirement: PR-01-1 要建立 tooltip / modal / toast overlay layer，后续 PR-03/04/06/07 复用 typed anchor family。
- Evidence: §5 已冻结 anchor family、modalSafeBounds、single tooltip、single backdrop、bottom log reserved bounds；但 focus mode 和 `INSPECT` modal/mode 映射仍需补表。
- Conclusion: 部分一致。

- Requirement: 删除 client ASCII renderer / model / manifest 字段，并确保扫描覆盖 source specs、runtime manifests、fixtures 和 game tilesets。
- Evidence: §6 Deletion Checklist 和 §9 scan 已覆盖 `assets-src/image/specs`；本轮 ASCII scan 0 命中。
- Conclusion: 一致。

- Requirement: 文档必须可被 `acceptanceContractLint` 结构化检查。
- Evidence: 本轮 `./gradlew acceptanceContractLint` 通过。
- Conclusion: 一致。

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| `TileMapViewport` state | deadzone、jump snap、small-map、visible range 统一由 typed viewport 管 | 未实现 / 文档部分可执行 | `UI/pr/...:85-95` | jump snap 需要上一帧 player/focus tile，但 state 未承载 | Medium |
| Viewport focus mode | `PLAYER / INSPECT / TARGETING` 进入 identity | 部分实现口径可推断 | `UI/pr/...:86-92`; `InputHandler.kt:1812-1827` | 缺少从现有 `UiMode` / `ModalFrameKind` 到 focus mode 的映射表 | Medium |
| Map renderer layering | `TileMapRenderer` 承接 terrain/prop/actor/fog/loot/cursor/feedback | 部分实现 / owner 漂移风险 | `UI/pr/...:108`; `TileLayerComposer.kt:3-10`; `PR-05:89,104` | PR-01-1 与 PR-05 都会拥有 map sublayer order | Medium |
| Overlay layer | tooltip/modal/toast/debug fixed order，single backdrop，single tooltip | 文档大体一致 | `UI/pr/...:121-143` | `INSPECT` frame kind 与 mode/pane 表述需收紧 | Low |
| ASCII 删除 | client ASCII renderer/model/manifest 字段删除并扫描 | 一致 | `UI/pr/...:207-220,291-305`; scan exit `1` | round8 的 specs 漏扫已修 | Low |
| Verification evidence | focused tests、golden、manual record、verification source 映射 | 大体一致 | `UI/pr/...:54,298-325` | jump snap 命名表缺两类边界名称 | Low |

## 玩法与体验审查

### 核心循环

PR-01-1 的 map-first 目标已经清楚：地图主导、底部 HUD 保留即时反馈、右侧 panel 不挤压地图。剩余风险集中在 viewport state：如果 jump snap 和 focus mode 映射不补清，玩家在 inspect/targeting/modal 之间切换时会看到不必要 recenter 或 long corridor 抖动，直接影响探索节奏。

### 战斗体验

combat feedback 被归入 map-anchored layer 是正确方向；但 map sublayer order 需要在 PR-01-1 或明确 owner 中冻结，否则 PR-05 加 telegraph/VFX 资源时，Boss warning、ground loot marker、actor、combat number 的遮挡关系会靠实现顺序漂移。

### 新手体验与信息反馈

tooltip/modal 互斥、bottom log reserved bounds、single backdrop 都已合理。需要补的是可执行的 flip edge formula，否则四角 tooltip 在中文长文本下很容易出现“看似通过 golden，换一个 anchor 就遮挡日志”的局部问题。

### 系统耦合与体验断层

PR-01-1 已经把 ASCII 删除、Tile-only evidence、modal/tooltip anchor family 写清楚。现在最值得收紧的是 owner 边界：viewport previous state 属于 `TileMapViewportState`，map sublayer order 属于 `TileLayerComposer` 或 `TileMapRenderer`，focus mode projection 属于 overlay/input 到 viewport 的 typed adapter；不要让这些规则散在 renderer 临时变量里。

## 当前阶段必须解决的问题

1. Jump snap previous state：当前 Phase 必须修。它是 §3.8 required behavior 的输入缺口，不补会迫使实现者引入 renderer-local hidden state。
2. Focus mode mapping：当前 Phase 必须修。它决定 inspect/targeting/modal 是否触发 viewport identity 变化，后续 PR 很难补救。
3. Map sublayer owner：当前 Phase 必须修到“最小明确”。PR-05 可以深化 VFX/actor 资源，但 PR-01-1 拆 renderer 时必须先给 map pass 顺序一个 typed owner。
4. Tooltip edge formula、small-map top-left、test method names：当前 Phase 建议一起修，改动很小，但能避免实现和 review 反复对齐。

## Removal/Iteration Plan

本轮没有新增需要立即删除的代码路径。当前已知 staged iteration 仍是 PR-01-1 文档本身要求的迁移：

- 删除 / 迁移旧 `FoundationViewportSupport.worldWidth(snapshot)`、`worldHeight(snapshot)` 和 snapshot-size-driven 测试；当前代码仍存在这些旧路径，见 `FoundationGameScreen.kt:301-319` 与 `FoundationViewportSupportTest.kt:18-37`。这属于 PR-01-1 实施范围，不作为本轮文档缺陷单独升级。
- 迁移 ASCII text snapshot evidence 到 Tile canvas/golden；本轮 ASCII scan 已确认当前扫描口径 0 命中。

## Additional Suggestions

1. 在 §3 增加 `TileViewportFocusProjection` 或等价小节，把 `OverlayState` / `ModalStack.top()` / `PaneFocusAnchor` 到 viewport focus mode 的转换收口成一个可测试 owner。
2. 在 §4 增加 `TileMapLayerOrder` 或 `TileLayerComposer` 小节，哪怕只写最小顺序，也能避免 PR-05 后续补资源时重新解释 PR-01-1 的 map renderer。
3. 在 manual record 的 `Verification Source` 表里预留 `contract owner` 列，值如 `TileMapViewportTest`、`TileOverlayLayerTest`、`TileLayerComposerTest`、`InputHandlerTest`，后续 reviewer 能更快看出测试覆盖归属。

## Suggested Verification

本轮已运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：`BUILD SUCCESSFUL`。

本轮已运行：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii' \
  client/src assets-src/image/specs assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
```

结果：退出码 `1`，0 命中。

建议在文档修复后补跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

PR-01-1 实施时重点新增或核对：

```bash
./gradlew :client:test \
  --tests com.ktome.client.render.TileMapViewportTest \
  --tests com.ktome.client.render.TileOverlayLayerTest \
  --tests com.ktome.client.input.InputHandlerTest \
  --tests com.ktome.client.ui.layout.ModalStackTest
```

如果采用 `TileLayerComposer` 作为 map sublayer owner，应把以下 test 加入 PR-01-1 或明确留给 PR-05 前置 gate：

```bash
./gradlew :client:test --tests com.ktome.client.render.TileLayerComposerTest
```

## Summary

PR-01-1 当前文档已经足够支撑长期 UI/UX 改造主线：Tile-only、map-first、fixed shell world size、typed overlay anchor、single backdrop、ASCII deletion 和 manual record 都比 round8 更完整。

剩余问题集中在实现入口的“最后一厘米”：jump snap 缺上一帧 player/focus tile，focus mode 缺从现有 input/modal 状态到 viewport identity 的映射，map 内部子层顺序缺 typed owner。建议先修这 3 个 P2，再顺手补 4 个 P3，之后这份 PR 文档才更适合作为后续开发者直接执行的长期合同。
