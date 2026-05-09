# Dark UI/UX PR-01-1 Client Viewport Renderer And Overlay Architecture

**阶段**: `dark-uiux-pr01-1-client-viewport-renderer-overlay`
**优先级**: `P0`
**工作量**: `XL`
**前置条件**: PR-01 完成，并且 `GameShellLayout`、`StandaloneScreenLayout`、`ModalStack`、`PaneFocusController` 已进入当前分支真源。
**资源生成结论**: 不生成正式资源；tooltip / modal 初版使用 primitive chrome 和现有 manifest key，PR-02 资源交付后只替换 chrome paint source，不改本 PR 的 viewport / renderer / overlay model 合同。
**视觉参考**: [../UI-demo.png](../UI-demo.png) 作为 ToME-like layout reference，只约束信息架构、密度、层级和 map-first 方向，不要求逐像素复刻。该文件必须随 dark UI/UX 文档进入仓库；缺失时 PR-01-1 不允许关闭。
**Review standard**: [docs/review/rule/pr-level-review-standard.md](../../docs/review/rule/pr-level-review-standard.md)。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 client focused tests、client evidence 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

本 PR 是 PR-01 的架构补强，不替代 PR-02 的 UI chrome 资源试点，也不提前实现 PR-03/04/05 的内容表现。核心目标是把 player-centered viewport、`TileRenderer` 责任拆分、tooltip-modal layer 三个长期基础合同一次性冻结，避免后续 PR 继续在单个 renderer 里堆过程式补丁。

信息架构在本文中指区域分布、占比、焦点优先级和信息分层；装饰指具体配色、字体、icon 风格和 chrome 资源，这些由 PR-02 及后续资源 PR 决定。本 PR 不复制 `UI/UI-demo.png` 的装饰，只冻结 ToME-like map-first 结构。

### Glossary

1. stage viewport 指 LibGDX `FitViewport` / window-level viewport；map viewport 指 `TileMapViewport` 管理的 tile visible range。二者不得混用术语。
2. tile sprite overlay 指 map 内 prop / decoration / loot marker / fog / combat feedback 等 tile-anchored sprite layer；UI overlay 只指 tooltip / modal / toast / debug hint layer。
3. modal explicit tooltip 指 modal frame kind 在合同中声明的内部 secondary tooltip slot。它不是 nested tooltip，也不能与 passive tooltip 同帧并存。
4. visibleCells 指当前 `mapBounds` 在 `cellSize` 下能容纳的整 cell 数量，按轴分为 visibleColumns / visibleRows；visibleRange 指 visibleCells 对应的 tile 索引区间 `[topLeft, bottomRight]`。
5. mapDimensions 指 map 内容的 columns / rows；mapBounds 指由 shell layout、cellSize 和 inner padding 共同决定的像素矩形，不等同于完整地图像素尺寸。
6. focused interactive entity 指当前 `PaneFocusController` 或 overlay state 选中的、具有 typed anchor bounds 的 entity，例如 panel slot、panel row/card、quest row、modal explicit row 或 world tile anchor。它必须来自 layout / presenter 输出的 typed rect 或 tile coordinate，不能来自鼠标位置、raw screen coordinate 或 renderer 临时坐标。
7. shellContentBounds 指 `GameShellLayout` primary output 的上方主要工作区像素矩形，排除 outer padding 与 bottom HUD slot group，包含 left rail、mapBounds、right panel 及其 panel gaps。`shellUsableContentArea = shellContentBounds.width * shellContentBounds.height`。
8. modalSafeBounds 指 `GameShellLayout` primary output 的 modal 可用矩形，排除 outer padding、bottom HUD、footer hints 与 bottom log reserved bounds；字段必须能表达 rect-style `left / right / top / bottom` safe edge，或等价 typed bounds。`bottom` 是 footer hints 的 top edge / bottom-safe edge，不是 window bottom。`OverlayRenderFrame` 只能引用该 layout output，modal renderer 不得从 window size、rail width、panel width 或 magic gap 二次推导 safe area。
9. bottomLogReservedBounds 指 `GameShellLayout` primary output 中预留给最新日志反馈的像素矩形；tooltip / modal placement 只能消费该 typed bounds，不得由 overlay renderer 重新推导。

### Major Decisions

1. Tile dark UI 是唯一正式 client 渲染路径。本 PR 删除 `AsciiRenderer`、`AsciiRenderModel`、`AsciiRenderModelTest` 类型本体及所有 client 消费路径；不是只删除依赖关系或保留死代码。
2. 删除 client ASCII 后，旧 text snapshot 型 UI 断言必须迁移到 `RenderSnapshot` vector 断言、`TileRendererCanvasTest`、client smoke 或 golden；不允许新增 ASCII-like full-map text dump test 作为替代。
3. dev-only debug overlay 不是 ASCII 接班人，不得渲染整张地图 ASCII；只允许绘制 viewport bounds、mapBounds outline、focus tile / cursor 高亮、`deadzoneStart` / `deadzoneEndExclusive`、`viewportTopLeft`、layer id 等诊断 hint。
4. PR-01 已修复的 finding 不得在本 PR 回退，包括 continue-unavailable 本机路径泄漏、validation setup overlap、outcome body 行数 cap、talent tones 对齐、outcome recap hash 更新等；golden/manual record 必须继续记录这些项为 fixed/pass-after-fix 或继承通过。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI01-1-M01` | §3 player-centered deadzone viewport 与 focus projection | `client` | `GameShellLayoutTest`, `TileViewportFocusProjectionTest`, `TileMapViewportTest`, `TileRendererCanvasTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/tests/test/`<br>`client/build/test-results/test/`<br>`client/build/reports/golden/` | `required` |
| `UI01-1-M02` | §3 shell world size 与地图尺寸解耦 | `client` | `TileMapViewportTest`, `FoundationViewportSupportTest` | `clientSmoke` | `client/build/reports/tests/test/`<br>`client/build/test-results/test/` | `required` |
| `UI01-1-M03` | §4 `TileRenderer` orchestration 化、map/shell/UI-overlay renderer 拆分与 map sublayer owner | `client` | `TileRendererCanvasTest`, `TileLayerComposerTest`, `maintainabilityLint` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/tests/test/`<br>`client/build/test-results/test/`<br>`client/build/reports/golden/` | `required: layer focused lane + frame ownership self-audit` |
| `UI01-1-M04` | §5 tooltip / modal / toast overlay layer order | `client` | `TileOverlayLayerTest`, `TileRendererCanvasTest`, `InputHandlerTest`, `ModalStackTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/tests/test/`<br>`client/build/test-results/test/`<br>`client/build/reports/golden/` | `required` |
| `UI01-1-M05` | §2.7 程序化阈值 + §2.1-§2.6 设计原则 + §10.10 manual record | `client (primary) / docs` | `GameShellLayoutTest`, `TileRendererCanvasTest` | `goldenScreenshot` | `client/build/reports/tests/test/`<br>`client/build/test-results/test/`<br>`client/build/reports/golden/`<br>`UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` | `required` |
| `UI01-1-M06` | §4/§6 typed frame / text metrics / hot-loop discipline | `client` | `TileRendererCanvasTest`, `maintainabilityLint`, frame ownership self-audit | `clientSmoke` | `client/build/reports/tests/test/`<br>`client/build/test-results/test/`<br>`UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` | `required: frame ownership self-audit` |
| `UI01-1-M07` | §0 / §6 Deletion Checklist / §7 / §9 governance, anti-bloat 与验证命令 | `docs (primary) / tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |

带 `(primary)` 的 owner 是 requirement 验收责任方；secondary owner 只提供 evidence 或 gate，不改变行为 owner。

### Gate Budget

预计重型任务：`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。触发原因是本 PR 重排 client renderer ownership、引入 typed viewport / frame / overlay model、改变局内 shell 的 world size 口径，并影响所有 tile map、HUD、modal、tooltip golden。

Gate freshness / duration / failure review 合同固定如下：

1. Golden freshness：本 PR 只新增或更新 `dark-uiux-pr01-1-*` golden；PR-01 已 ship 的 `dark-uiux-pr01-*` golden 不得删除。若某个 PR-01 golden 被 PR-01-1 等价替换，必须在 manual record 写明旧 label、新 label、替换原因和保留状态。
2. Duration source：每次执行 `verifyChanged` 后读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。若文件不存在，manual record 必须写明原因，例如本轮只跑 focused lane 或 gate 未到达该阶段。
3. Failure review：同一重型 gate 连续失败超过 2 次时，停止继续 broad rerun，先补 focused viewport / layer / overlay test 或更窄复现，再把 root cause、失败命令、修复点写入 manual record 的 `Findings`，PR description 的 `Validation` 也必须引用该复盘。
4. 调试循环默认使用 focused lane；`goldenScreenshot` 和 `verifyChanged` 只在 focused owner tests 稳定后执行，不作为反复定位数学、placement 或 layer order 的第一工具。

### Canonical Artifact

canonical evidence 固定为 client golden output、client smoke report、focused test report、`UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` 和 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。focused test report 必须使用 module-specific repo-relative artifact path，例如 `client/build/reports/tests/test/` 和 `client/build/test-results/test/TEST-com.ktome.client.render.TileMapViewportTest.xml`，不得只写泛化的 `build/reports/tests/`。golden evidence 使用 `client/build/reports/golden/`，人工白盒 evidence 使用 `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` 或其同名证据目录。

`UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` 由 PR-01-1 实施时创建，模板参考 `UI/manual-records/dark-uiux-pr01-shell.md`，且必须包含 `Verification Source`、`Commands Run`、`Evidence`、`Frame Ownership Self-Audit`、`Overlay Conflict Evidence`、`ASCII Deletion Scan`、`Image Region Comparison`、`Findings`、`Residual Risk` 九个子段；缺失任一必填子段，canonical evidence 无效。`Verification Source` 子段必须包含 `requirementId | Must Test item | contract owner | test class | test method | artifact | status` 表格，用于追溯每个 Must Test 对应的自动化或人工证据。`UI01-1-M02` 必须单独列出 `FoundationViewportSupportTest` 的 `keepsWorldSizeFixedWhenSnapshotDimensionsChange`、`keepsViewportWorldSizeFixedAcrossResize`、`usesShellMapBoundsInsteadOfFullMapPixels` 方法和对应 test report artifact，不能只用 `TileMapViewportTest` 或 golden 代替。projection evidence 必须列出 `TileViewportFocusSourceKind`；涉及 validation explicit projection 时，还必须列出 `ValidationProjectionReason`，避免 fixture、manual probe 与 debug whitebox projection 混写。debug-only screenshot metadata、本机窗口坐标、临时 framebuffer hash 或 raw evidence timestamp 不得成为合同。

Golden namespace 固定为新增 / 更新 `dark-uiux-pr01-1-*`。PR-01 已 ship 的 `dark-uiux-pr01-*` golden 不得静默删除；若功能完全等价，例如 `shell-min-window`，manual record 必须显式说明替换关系并保留 PR-01 golden 的注释。

### Failure Rule

viewport / golden 失败时先补 focused viewport or canvas test，再修 `TileMapViewport` 或 layout math；overlay 失败时先修 `TileOverlayModel` / `TileOverlayRenderer` 的 placement contract，不得在具体 modal case 中写一次性坐标分支；maintainability 失败时先收敛 typed request/model，不加 allowlist 绕过。client ASCII renderer / model / manifest 删除导致 client 测试断裂时，优先迁移测试到 `RenderSnapshot` vector 断言或 Tile canvas/golden，不允许回滚 client ASCII renderer。core / game 模块 ASCII fixture 不在本 PR 删除范围；若误删导致 game test 断裂，必须恢复 core / game ASCII fixture，而不是回滚 client ASCII。PR-01-1 之后不允许任何 PR 重新引入 client ASCII renderer / model / manifest 字段。

## 1. 阶段目标

1. 冻结 player-centered deadzone viewport：地图大于 shell map 区域时，以玩家附近内容为主；玩家在死区内不滚动，靠近死区边缘时最小滚动；地图边缘 clamp；小地图居中显示，不拉伸 tile。
2. 让 `FoundationGameScreen` 的 viewport world size 与完整地图尺寸解耦；shell world size 由 UI token / layout contract 决定，地图尺寸只影响 `TileMapViewport.visibleRange`。
3. 把 `TileRenderer` 从 map、shell、HUD、log、tooltip、modal 全部混写的过程式宿主，收敛成只负责 frame orchestration 的入口。
4. 引入 typed `TileMapViewport`、`MapRenderFrame`、`ShellRenderFrame`、`OverlayRenderFrame`、`TileOverlayModel`，避免坐标换算、draw order 和 overlay placement 继续散落在多个函数。
5. 建立统一 overlay layer：tooltip、modal、toast/debug message 使用固定层级，不遮挡底部最新日志反馈，不绕过 `ModalStack` 和 `OverlayState`。
6. 执行 §0 Major Decision 1 的 Tile-only 渲染合同；本 PR 之后所有玩家可见 evidence（golden、manual screenshot、smoke artifact）都必须走 Tile dark UI。

## 2. ToME 类型游戏设计合同

本 PR 的新页面和局内 shell 必须符合 ToME 类型游戏的设计理念：地图是第一视觉焦点，信息密度高但分区稳定，玩家能在同一屏完成移动、查看地面物品、装备、铭刻、背包、读取日志和确认下一步行动。参考 `UI/UI-demo.png` 的结构方向；图中的石墙纹理、角色立绘、徽章、等级牌、sprite art、金属装饰和材质细节属于 PR-02 / PR-05 资源与 chrome 范围，PR-01-1 只冻结区域结构、信息分层、可验证占比和交互锚点。

1. 中央地图占据最大连续区域；fog、墙体、地面、actor、loot、cursor 和 telegraph 必须可读，不能被装饰性 chrome 压缩成展示图。
2. 左侧是窄 rail 或导航/状态入口，只放 dungeon、floor、任务摘要、关键提示、模式入口；不重复完整角色状态。
3. 右侧是稳定的垂直信息面板，默认分区顺序为地面物品、装备、铭刻、背包；资源摘要可作为面板内短行补充，但不得挤掉四个主分区。各分区使用 grid、slot、icon、数量和短标签；长解释进入 tooltip/modal/log，不挤占地图。
4. 底部是唯一 HUD 与日志带，必须拆成四个稳定横向子区：角色头像/纹章 + 名称 + dungeon/floor + HP + 职业资源或魔法类资源 + 攻击/防御等核心短数值、技能/天赋快捷栏卡片、紧凑快捷键提示、最新日志 + 目标/路线/方向键提示。不在本 PR 引入经验条或第二套状态栏；后续若需要 XP 表现，必须新开 PR 修改本条合同。
5. 视觉基调是暗色石质、金属边框、低饱和背景、少量高亮色表达选择/危险/稀有；禁止 marketing hero、说明页式首屏、大块装饰渐变或纯卡片堆叠。
6. 页面必须为键盘优先，焦点态、禁用态、返回路径和 modal top frame 清楚；不能因为 hover 或鼠标状态缺失而丢失核心信息。
7. map-first 必须可验证：标准局内 shell 下 `mapBounds` 像素面积应占 shell usable content 的主要部分；在 `1280x800` standard layout 下，`mapBounds.width * mapBounds.height >= 0.5 * shellUsableContentArea`。`shellUsableContentArea` 的分母按 Glossary §0.7 的 `shellContentBounds` 计算，排除 bottom HUD slot group，但包含 left rail、right panel 和 panel gaps。若 golden 中地图被 chrome 或卡片压缩成次要区域，PR 必须视为失败而不是视觉偏好差异。

## 3. Viewport 合同

1. `TileMapViewport` 是唯一 tile-to-screen 坐标真源，输入为 `mapDimensions`、`mapBounds`、`cellSize`、`playerTile`、`TileMapViewportIdentity`、`TileViewportFocusProjectionResult.resolvedMode`、`TileViewportFocusProjectionResult.resolvedFocusTile` 和上一帧 `TileMapViewportState`。`TileMapViewport` 不得直接消费 raw `inspectCursor`、raw `targetingCursor`、`validationPanel.inspectCursor` 或 modal local cursor；这些输入只能由 `TileViewportFocusProjection` 解析成 typed projection result。
2. 首帧、zone 变化、cell size 变化或 cell-aligned `mapBounds` 变化后必须按当前 resolved focus mode 初始化 snap：`PLAYER` snap 到 player-centered + clamp state，`INSPECT / TARGETING` snap 到 resolved focus tile-centered + clamp state。active focus mode 字段值变化（`PLAYER / INSPECT / TARGETING`）必须分方向处理：`PLAYER -> INSPECT/TARGETING` 或 `INSPECT <-> TARGETING` snap 到 resolved focus tile-centered + clamp state；`INSPECT/TARGETING -> PLAYER` snap 到 player-centered + clamp state。同一 focus mode 内 cursor / target tile 变化不触发 snap，由 deadzone 算法处理。sub-cell 像素余数变化只更新 inner padding，不丢弃 prev state。
3. identity 字段同帧变化时，snap 仅基于本帧 final state 计算一次，不允许中间状态被 prev state 算法捕获。snap 后产生的 `viewportTopLeft` 必须作为下一帧 deadzone 计算的合法 prev state，不能因为 prev state 来自 snap 而再次 recenter。
4. `TileMapViewportState.identity` 必须是 typed `TileMapViewportIdentity`，任一字段变化必须丢弃上一帧 state。`viewportTopLeft`、visibleRange、`lastPlayerTile`、`lastFocusTile` 和 `lastFocusMode` 是 state 的非 identity 持久字段，identity 一致时随 deadzone / jump snap 计算更新，identity 不一致时随 prev state 一起丢弃。`lastPlayerTile` / `lastFocusTile` 只能来自上一帧 `TileMapViewportState`，不得由 renderer-local 临时缓存、`viewportTopLeft` 或 visibleRange 反推。

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

`TileMapViewportIdentity` 字段来源固定为：`zoneId = snapshot.metadata.zoneId`；`currentFloor = snapshot.metadata.currentFloor`；`mapDimensions` 的 primary source 是 `TileMapDimensions(snapshot.metadata.width, snapshot.metadata.height)`；`cellSize` 来自 §6 Token Contract；`cellAlignedMapBounds` 来自 shell layout 对 `mapBounds` 的完整 cell 对齐结果；`focusMode` 来自 `TileViewportFocusProjectionResult.resolvedMode`。如果实现从 `TileRenderModel`、map cell extents 或其他 presentation model 派生 `mapDimensions`，必须与 `snapshot.metadata.width/height` 完全一致；不一致必须 fail fast，不得任选一个来源继续渲染。禁止使用 `snapshot.metadata.revision` 作为 viewport identity，因为它会随 turn 更新并导致每帧 recenter。后续如果引入 run/session id，只能作为 identity 的额外字段，不得替代 `zoneId + currentFloor`。
`RectInt` 可替换为本仓库等价的 cell-aligned integer bounds 类型，但不得使用 float `GameShellBounds` 原值直接作为 identity；identity 只接受已经按 `cellSize` 向下对齐后的整 cell 矩形。
5. 死区默认以 visibleRange 的 tile 几何中心为基准；几何中心索引 = `floor(visibleCells / 2)`。deadzone 锚点 = 当前 active focus tile，inspect / targeting 期间 focus tile = cursor / target tile。默认 token 为 `deadzoneHorizontalMinCells=4`、`deadzoneHorizontalRatio=0.25f`、`deadzoneVerticalMinCells=3`、`deadzoneVerticalRatio=0.25f`；实际 deadzone cells 使用 `max(minCells, floor(visibleCells * ratio))`，cap = `max(0, visibleCells - 2)`，保证两侧至少各有 1 cell 滚动余量。cap 优先级高于 minCells；当 cap = 0 时 deadzone 退化为 0 cells，viewport 严格跟随 player / focus tile，视为 supported STRICT_FOLLOW 退化而不是 bug。每个轴的 deadzone 必须按半开区间计算：`deadzoneStart = clamp(centerIndex - floor(effectiveDeadzoneCells / 2), 0, visibleCells - effectiveDeadzoneCells)`；`deadzoneEndExclusive = deadzoneStart + effectiveDeadzoneCells`；`focusLocalIndex = focusTileAxis - viewportTopLeftAxis`；只有 `focusLocalIndex in deadzoneStart until deadzoneEndExclusive` 才视为仍在死区内。该规则必须保证 even/odd visibleCells 与 even/odd deadzoneCells 下实际 deadzone cell 数量都等于 `effectiveDeadzoneCells`。
6. 玩家或 focus tile 离开死区时，滚动方向 = focus tile 越出 deadzone 半开区间的方向（focus tile 在 `deadzoneEndExclusive` 右侧时 viewport 向右滚，小于 `deadzoneStart` 时向左滚，上 / 下同理）；滚动量 = 让该 tile 回到最近 deadzone 边界内的最小整 cell 距离。玩家移动 1 cell 且仍在死区内时，`viewportTopLeft` 必须保持上一帧值。
7. Inspect / targeting 可以在模式激活期间使用 resolved focus tile 作为 focus tile；进入 inspect / targeting 的 identity-change 帧按 §3.2 snap 到 resolved focus tile-centered + clamp state，保证首帧焦点对象可见。关闭时按 §3.2 规则触发 snap 回 player-centered + clamp state，snap 后写入的新 state 必须以 live `playerTile` 作为 `lastPlayerTile` / `lastFocusTile`，并把 `lastFocusMode` 更新为 `PLAYER`。下一帧 deadzone 以该 state 为新基线，不能让 cursor-driven offset 成为后续移动的长期基线。
8. 玩家 tile 在单帧内跳变时必须按轴判断 snap 阈值：`previousPlayer = prevState.lastPlayerTile`、`dx = newPlayer.x - previousPlayer.x`、`dy = newPlayer.y - previousPlayer.y`；若 `abs(dx) > effectiveHorizontalDeadzoneCells || abs(dy) > effectiveVerticalDeadzoneCells`，例如同 zone respawn、teleport 或角色重定位，viewport 必须 snap 到新 player-centered + clamp state，不走 deadzone 最小滚动算法。任一轴 `==` 对应 threshold 时不触发 jump snap，仍走 deadzone 最小滚动算法；diagonal move 只有在至少一个轴超过自身 threshold 时才触发 snap。首帧或 identity 变化帧没有合法 `prevState.lastPlayerTile` 时，不执行 jump snap 比较，直接走 §3.2 的 snap 初始化路径。
9. 玩家靠近左上 / 右下边缘时，visible range clamp 到地图边界，不能出现负坐标、黑边、半格或越界 tile draw；水平和垂直 clamp 独立计算，边角场景两轴同时 clamp，不存在顺序依赖。
10. 地图小于可视 tile count 时，地图内容在 `mapBounds` 内居中显示，不拉伸 tile，不重复绘制。`mapDimensions < visibleCells` 的轴必须短路 deadzone 与 jump snap：该轴 `viewportTopLeftAxis = 0`、`visibleRangeAxis = 0 until mapDimensionAxis`，不响应玩家移动。居中只能通过 `innerPaddingAxis = floor((cellAlignedMapBoundsAxis - mapDimensionAxis * cellSize) / 2)` 表达，不得把 `viewportTopLeft` 写成负数、半格或虚拟 tile offset。
11. `mapBounds` 必须 cell-aligned：`mapBounds.width` 和 `height` 进入 `TileMapViewport` 前向下取整为完整 cell 区域；剩余像素作为 inner padding 居中处理。
12. terrain、prop、tile sprite overlay、actor、fog、ground loot marker、player tile indicator、targeting cursor、inspect cursor、combat feedback 必须通过同一个 `tileToScreen` transform。`tileToScreen(tile)` 返回该 tile 在 LibGDX Y-up 坐标系中的像素左下角 `Int` 坐标，所有 sprite draw 以此为基准，调用方不得各自 floor / round / cast。
13. `PaneFocusAnchor.WORLD` focus ring 绘制在 `layout.shell.mapBounds`，不是完整地图像素矩形。
14. `mapBounds` 内但超出 `mapDimensions` 的 padding / fog 区域只允许绘制 `fog.outOfMap` 或等价基础 fog fill，不参与 `tileToScreen(tile)`、hover/inspect hit test、loot marker 或 actor pass；canvas/golden 必须能区分真实地图边界与 out-of-map padding，不允许用黑边或透明洞代替。

### Viewport Focus Projection

Viewport focus mode 必须由一个 typed projection owner 计算，可命名为 `TileViewportFocusProjection` 或等价类型。它只消费 input / overlay / modal / validation 的 typed state，输出当前帧 projection facts；不得在 `TileRenderer` draw path 中临时判断，也不得直接把 raw cursor 塞给 `TileMapViewport` 或 tooltip placement。

`TileViewportFocusProjection` 不拥有 previous state，不输出 identity changed。identity 是否变化只由 `TileMapViewport` / orchestration 用本帧 `TileMapViewportIdentity` 与 `prevState.identity` 比较决定。

```kotlin
data class TileViewportFocusProjectionRequest(
    val playerTile: Point,
    val overlayState: OverlayState,
    val validationInspectProjection: ValidationInspectProjection? = null,
)

data class ValidationInspectProjection(
    val cursor: Point,
    val reason: ValidationProjectionReason,
)

data class TileViewportFocusProjectionResult(
    val resolvedMode: TileViewportFocusMode,
    val resolvedFocusTile: Point,
    val sourceKind: TileViewportFocusSourceKind,
    val anchorTile: Point?,
    val tooltipAnchorKind: TileOverlayAnchorKind?,
    val isTooltipAnchorValid: Boolean,
)

data class TileViewportFocusCandidate(
    val mode: TileViewportFocusMode,
    val sourceKind: TileViewportFocusSourceKind,
    val cursor: Point?,
    val anchorKind: TileOverlayAnchorKind?,
)
```

命名可等价，但 request / result 的 owner 与字段语义不可省略。`OverlayState.modalFrames` 是 projection 的唯一 modal stack source；request 不得再持有独立 `modalFrames`，也不得从 renderer / orchestration 传入第二个 modal list。`OverlayState.modalFrames` 顺序固定为 bottom-to-top：`first()` 是最底层 modal，`lastOrNull()` 是 top modal；projection 使用 `asReversed()` 扫描 top-to-bottom。测试 fixture、validation fixture 和手写 `OverlayState` 必须按同一 bottom-to-top 顺序构造，不得传入 top-first list。若实现为了测试 fixture 引入窄 `OverlayProjectionState`，也必须保证 modal frames 只有一个字段，并覆盖 `rejectsDivergentOverlayAndRequestModalFrames`、`ModalStackTest.framesAreBottomToTopForProjection` 和 `TileViewportFocusProjectionTest.scansModalFramesTopToBottomFromBottomFirstList` 或等价单一 authority 测试。

PR-01-1 默认冻结以下 enum 值。若实现确需等价命名，manual record 必须列出逐项映射；否则测试和 evidence 必须使用 exact names。

```kotlin
enum class TileViewportFocusMode {
    PLAYER,
    INSPECT,
    TARGETING,
}

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

`ValidationInspectProjection.reason` 只用于 evidence / diagnostics，不得改变 viewport 规则。

| Existing state | Viewport focus mode | Focus tile | Projection rule |
| --- | --- | --- | --- |
| 无 active inspect / targeting projection | `PLAYER` | `playerTile` | 普通 modal open/close 不新增 spatial candidate |
| `UiMode.INSPECT` 或 `ModalFrameKind.INSPECT` active | `INSPECT` | resolved inspect cursor，fallback 见下表 | 只输出本帧 `INSPECT` projection；snap 由 identity 比较触发 |
| `UiMode.TARGETING`、`ModalFrameKind.TARGETING` 或 `ModalFrameKind.COMBAT_DECISION` active | `TARGETING` | resolved targeting cursor / target tile，fallback 见下表 | 只输出本帧 `TARGETING` projection；snap 由 identity 比较触发 |
| `INVENTORY / ITEM_DETAIL / ITEM_COMPARE / TALENT_ASSIGN / ACTIVE_TALENT_SLOT_CHOICE / SHOP / LOADOUT_EDIT / STAT_ASSIGN / VALIDATION / WORLD_MAP` | 不新增 candidate；若下层或 overlay 无 inspect / targeting candidate 则为 `PLAYER` | 当前 spatial focus tile；默认 `playerTile` | 只影响 overlay z-order 或 input surface，不单独触发 viewport recenter |

Projection precedence 固定为 source tier first，再按 mode priority。source tier 优先级为：modal spatial candidates -> overlay-level spatial candidates -> explicit validation inspect -> player。只要存在任一 modal spatial candidate，overlay-level candidate 不论同 mode 还是跨 mode 都不能参与 selection，只能参与 consistency / stale-state check。modal tier 内的 mode priority 仍为 `TARGETING > INSPECT`，同 mode 内按 top-to-bottom modal order；overlay tier 内的 mode priority 也为 `TARGETING > INSPECT`。`ModalStack.top()` 不能单独决定 viewport focus mode；detail modal、compare modal、shop、talent 或 validation frame 只有在显式投影到 inspect / targeting 时才影响 viewport identity。

Projection owner 必须按当前帧构造 `TileViewportFocusCandidate` 列表，不得读取上一帧 projection 来“保持”状态。candidate 是 `mode / sourceKind / cursor / anchorKind` 的最小 authority；`sourceKind`、`resolvedFocusTile`、`anchorTile`、`tooltipAnchorKind` 和 `isTooltipAnchorValid` 都必须从同一个 selected candidate 派生，禁止先选 modal source、再从 overlay-level stale cursor 取 anchor。

candidate 收集规则固定如下：

1. modal candidates：按 `overlayState.modalFrames.asReversed()` 从 top 到 bottom 扫描，`TARGETING` 产生 `MODAL_TARGETING` candidate，`COMBAT_DECISION` 产生 `MODAL_COMBAT_DECISION` candidate，`INSPECT` 产生 `MODAL_INSPECT` candidate；candidate cursor 只能来自该 modal frame 的 local state，可为空；world-space cursor 的 `anchorKind = WORLD_TILE`。
2. overlay-level targeting candidate：`overlayState.mode == TARGETING` 或等价 active targeting surface 存在即产生 `OVERLAY_TARGETING` candidate；candidate cursor 只能来自 `overlayState.targetingCursor`，可为空；world-space cursor 的 `anchorKind = WORLD_TILE`。
3. overlay-level inspect candidate：`overlayState.mode == INSPECT` 或等价 active inspect surface 存在即产生 `OVERLAY_INSPECT` candidate；candidate cursor 只能来自 `overlayState.inspectCursor`，可为空；world-space cursor 的 `anchorKind = WORLD_TILE`。
4. explicit validation candidate：仅当 `TileViewportFocusProjectionRequest.validationInspectProjection != null` 时加入 `VALIDATION_INSPECT` candidate；candidate cursor 来自 explicit projection，`anchorKind = WORLD_TILE`。
5. player candidate：始终存在，作为最后兜底，`cursor = playerTile`，`anchorKind = null`。

Mode selection 固定为：先收集 modal spatial candidates；若非空，只在 modal tier 内按 `TARGETING > INSPECT` 选 selected candidate，同 mode 内按 top-to-bottom modal order。只有 modal tier 为空时，才允许 overlay-level candidates 参与 selection；overlay tier 内若存在 `OVERLAY_TARGETING` 则 selected 为 `TARGETING`，否则若存在 `OVERLAY_INSPECT` 则 selected 为 `INSPECT`。overlay tier 也为空时，才检查 `VALIDATION_INSPECT`；仍为空则使用 `PLAYER`。top modal 是 `COMBAT_DECISION / TARGETING / INSPECT` 时，modal local cursor 必须优先于 stale overlay-level cursor；overlay-level cursor 只能服务无任何 spatial modal candidate 的 overlay-level mode。

如果实现阶段仍临时保留 overlay-level cursor 与 modal local cursor 双写，`TileViewportFocusProjection` 必须在 candidate collection 后、selected result 输出前做 consistency check。check owner 不得分散到 `InputHandler`、renderer 或单个 modal presenter；这些位置只能维护 typed state，不能各自决定是否忽略 stale cursor。非 selected cursor 永远不得用于补齐 selected candidate 的 `cursor`。

双写 consistency matrix 固定如下：

| selected candidate | overlay-level candidate | 是否同一 surface | 允许值 | 不一致行为 |
| --- | --- | --- | --- | --- |
| `MODAL_TARGETING` | `OVERLAY_TARGETING` | yes，当 selected modal kind 是 `TARGETING` 且 overlay-level targeting surface 仍 active | overlay cursor 为 `null` 或等于 selected modal cursor | fail fast，错误信息包含 `MODAL_TARGETING`、`OVERLAY_TARGETING`、两个 cursor 值 |
| `MODAL_COMBAT_DECISION` | `OVERLAY_TARGETING` | yes，`COMBAT_DECISION` 复用 targeting cursor 双写路径 | overlay cursor 为 `null` 或等于 selected modal cursor | fail fast，错误信息包含 `MODAL_COMBAT_DECISION`、`OVERLAY_TARGETING`、两个 cursor 值 |
| `MODAL_INSPECT` | `OVERLAY_INSPECT` | yes，当 selected spatial modal kind 是 `INSPECT` 且 overlay-level inspect surface 仍 active | overlay cursor 为 `null` 或等于 selected modal cursor | fail fast，错误信息包含 `MODAL_INSPECT`、`OVERLAY_INSPECT`、两个 cursor 值 |
| `OVERLAY_TARGETING` | any modal candidate absent or non-spatial only | no | modal-local cursor 不存在；若存在 spatial modal candidate，selection 不应落到 overlay | fail fast on impossible selection |
| `OVERLAY_INSPECT` | any modal candidate absent or non-spatial only | no | modal-local cursor 不存在；若存在 spatial modal candidate，selection 不应落到 overlay | fail fast on impossible selection |
| `VALIDATION_INSPECT` | `OVERLAY_INSPECT` | no，explicit validation projection 是独立 evidence surface | overlay inspect candidate 若 active，应按 source priority 抢占 validation；否则不比较 | fail fast only if selection priority 被破坏 |

cross-mode stale overlay 检查固定为：存在任一 modal spatial candidate 时，任何 overlay-level candidate 都不能抢占 selection。若 modal tier selected 为 `MODAL_INSPECT`，但 `OVERLAY_TARGETING` 仍 active，则 fail fast，错误信息包含 `MODAL_INSPECT`、`OVERLAY_TARGETING` 和 overlay targeting cursor；若 modal tier selected 为 `MODAL_TARGETING / MODAL_COMBAT_DECISION`，但 `OVERLAY_INSPECT` 仍 active，则 fail fast，错误信息包含 selected source、`OVERLAY_INSPECT` 和 overlay inspect cursor。对应测试必须覆盖 `modalSpatialCandidateSuppressesCrossModeOverlayCandidate`、`rejectsOverlayTargetingWhenModalInspectCandidateExists`、`rejectsOverlayInspectWhenModalTargetingCandidateExists`。

non-spatial top modal 不遮蔽下层 spatial candidate；consistency check 以 selected spatial candidate 为准，而不是只看 `ModalStack.top()`。如果 selected candidate 来自下层 `MODAL_INSPECT`，仍按 `MODAL_INSPECT` / `OVERLAY_INSPECT` 行检查。manual record 的 `Frame Ownership Self-Audit` 必须列出 `modalFrames order truth` 与 `temporary dual-write fields`，至少包含 `OverlayState.modalFrames bottom-to-top`、`OverlayState.targetingCursor`、`OverlayState.inspectCursor`、`ModalFrameLocalState.targetingCursor`、`ModalFrameLocalState.inspectCursor`、当前 check owner 和删除条件。

active spatial surface / spatial modal frame 即使 cursor 暂时为空，也仍产生 candidate。因此 active `INSPECT / TARGETING` 但 selected candidate cursor 为空时，必须输出 `resolvedMode = INSPECT/TARGETING`、`resolvedFocusTile = playerTile`、`anchorTile = null`、`tooltipAnchorKind = null`、`isTooltipAnchorValid = false`，直到 typed state 明确退出该模式。

non-spatial modal 只是不新增 candidate，也不遮蔽下层 spatial candidate。若 `ITEM_DETAIL` 位于 top、下层还有 active `INSPECT` frame，则 projection 仍可解析为 `INSPECT`；若没有任何 spatial mode candidate，则结果为 `PLAYER / playerTile`。如果未来需要基于 previous projection 保留状态，必须把 `previousProjection` 显式加入 request 并新增测试，本 PR 默认禁止隐式缓存。

#### Focus Tile Resolution

`TileViewportFocusProjectionResult.resolvedFocusTile` 必须非空，仅服务 viewport。tooltip anchor 必须单独使用 nullable `anchorTile`、`tooltipAnchorKind` 和 `isTooltipAnchorValid` 表达。nullable cursor 只能在 projection owner 内解析，不得在 `TileMapViewport`、`TileRenderer` 或 canvas test 中各自 fallback。

| Selected candidate | Viewport focus tile | Anchor rule |
| --- | --- | --- |
| `PLAYER` | `playerTile` | 无 tooltip anchor；player tile 是必备 snapshot truth |
| `MODAL_INSPECT / OVERLAY_INSPECT / VALIDATION_INSPECT` | selected candidate cursor；为空时 fallback 到 `playerTile` | cursor 非空时 `anchorTile = cursor`、`tooltipAnchorKind = selected.anchorKind`、`isTooltipAnchorValid = true`；cursor 为空时 anchor 三字段为 `null / null / false` |
| `MODAL_TARGETING / MODAL_COMBAT_DECISION / OVERLAY_TARGETING` | selected candidate cursor；为空时 fallback 到 `playerTile` | cursor 非空时 `anchorTile = cursor`、`tooltipAnchorKind = selected.anchorKind`、`isTooltipAnchorValid = true`；cursor 为空时 anchor 三字段为 `null / null / false` |

Focus resolution order 就是 selected candidate priority；不得再在 Focus Tile Resolution 里独立扫描 `overlayState.inspectCursor -> modal local cursor` 或 `overlayState.targetingCursor -> modal local cursor`。selected candidate cursor 之外的 cursor 只能参与 consistency check，不能参与 anchor 或 viewport fallback。

`VALIDATION` 默认不改变 viewport focus identity。validation fixture 若需要 world-space cursor，必须通过 `TileViewportFocusProjectionRequest.validationInspectProjection` 显式投影为 `INSPECT` 并复用 `WORLD_TILE` anchor / `TileViewportFocusProjection`；`validationPanel.inspectCursor` 不能被 renderer/model 直接用于 viewport 或 tooltip anchor，也不得保留 validation-only cursor path 作为第二套 authority。`TileViewportFocusProjectionTest.validationCursorRequiresExplicitInspectProjection` 必须同时覆盖“无 explicit projection 时 VALIDATION 不改变 viewport”和“有 explicit projection 时进入 `INSPECT`”。

Projection fallback 不得改变 resolved focus mode：例如 `TARGETING` fallback 到 `playerTile` 时，identity 仍是 `TARGETING`，直到 typed state 明确退出 targeting；但此时 `anchorTile` 必须为空，不能在玩家脚下显示 passive tooltip 或确认 target。测试必须覆盖 `inspectFallbackUsesPlayerForViewportButSuppressesTooltipAnchor` 和 `targetingFallbackUsesPlayerForViewportButDoesNotConfirmTarget`。

## 4. Renderer 拆分合同

1. `TileRenderer` 退化为 orchestration：build model、resolve shell layout、resolve viewport、构造 frames、按固定 layer order 调度 renderer。
2. `MapRenderFrame` 只包含 map renderer 所需字段：snapshot/model map surface、layout map bounds、viewport、visual/audio resolver 需要的 presentation 引用；cell metrics 只能通过 `viewport.cellSize` 访问，frame 不独立持有 `cellWidth` / `cellHeight` 字段。
3. `ShellRenderFrame` 只包含 left rail、right panel 四分区、bottom HUD 四子区、pane focus ring 所需字段，不持有 raw `OverlayState`。
4. `OverlayRenderFrame` 只包含 overlay renderer 所需字段：`TileOverlayModel`、shellContentBounds、modalSafeBounds、bottom log reserved bounds、text metrics。shellContentBounds、modalSafeBounds、bottomLogReservedBounds 只能引用 `GameShellLayout` primary outputs，不得在 frame builder 或 renderer 中重新计算。`OverlayRenderFrame` 不直接暴露 raw `TileMapViewport`、raw `OverlayState`、`ModalFrame`、`ModalStack.top()` 或 `TileViewportFocusProjectionResult`；WORLD_TILE anchor 必须在 model / frame build phase 通过 `TileOverlayAnchorResolver` 预解析成 screen-space rect。`TileViewportFocusProjectionResult` 是 `TileOverlayModelBuilder` 的输入和 manual evidence，不是 overlay renderer frame field。

`OverlayRenderFrame` minimum shape 固定如下；等价命名必须保留同样的字段边界。

```kotlin
data class OverlayRenderFrame(
    val overlayModel: TileOverlayModel,
    val shellContentBounds: GameShellBounds,
    val modalSafeBounds: ModalSafeBounds,
    val bottomLogReservedBounds: GameShellBounds,
    val textMetrics: TileTextMetrics,
)
```

`GameShellLayout` minimum primary output shape 固定如下；类型名可等价，但字段责任不可省略。`ModalSafeBounds`、`RectInt`、`InsetsInt` 可替换为仓库等价 typed bounds，不得退化为 raw window size 或 renderer-local 临时数值。

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

5. `TileMapViewport` 是独立 typed 真源，既不归 frame 也不归 model；每帧由 orchestration 构造，被 `MapRenderFrame` 直接引用，并被 `TileOverlayAnchorResolver` 在 build phase 用于解析 `WORLD_TILE` anchor。`TileOverlayRenderer` 不得直接消费 viewport，也不得自行重算 tile-to-screen。

Overlay anchor resolution 最小合同如下；命名可等价，但 owner、输入、输出和诊断字段不可省略。

```kotlin
data class ResolvedTileOverlayAnchor(
    val source: TileOverlayAnchor,
    val bounds: RectInt,
    val coordinateAuthority: TileOverlayCoordinateAuthority,
)

sealed interface TileOverlayAnchorResolution {
    val source: TileOverlayAnchor

    data class Resolved(
        val anchor: ResolvedTileOverlayAnchor,
    ) : TileOverlayAnchorResolution {
        override val source: TileOverlayAnchor = anchor.source
    }

    data class Failed(
        override val source: TileOverlayAnchor,
        val reason: TileTooltipSuppressionReason,
    ) : TileOverlayAnchorResolution
}

enum class TileOverlayCoordinateAuthority {
    TILE_MAP_VIEWPORT,
    SHELL_LAYOUT,
    PRESENTER_LAYOUT,
}

interface TileOverlayAnchorResolver {
    fun resolve(anchor: TileOverlayAnchor): TileOverlayAnchorResolution
}
```

resolver instance 必须由 orchestration / frame builder 每帧用当前 `TileMapViewport`、`GameShellLayout` primary outputs 和 presenter-owned rect catalog 构造；不得作为跨帧 singleton 缓存 viewport、layout 或 stale bounds。

`WORLD_TILE` 只能由 resolver 调用本帧 `TileMapViewport.tileToScreen(tile)` 得到 tile rect；`PANEL_SLOT`、`PANEL_ROW_OR_CARD`、`QUEST_ROW`、`MODAL_ROW` 使用 layout / presenter 已输出的 rect。anchor 离开 visible range 时，resolver 必须返回 `Failed(reason = ANCHOR_OUTSIDE_VISIBLE_RANGE)`；presenter rect 缺失、entity stale 或 bounds 不合法时，返回 `Failed(reason = ANCHOR_RESOLUTION_FAILED)`。`TileOverlayModelBuilder` 必须把 failed resolution 转成 `suppressedTooltipSources` evidence，不得猜测失败原因。`TileOverlayLayerTest.cornerAnchorsFlipInsideShellBounds` 必须断言 WORLD_TILE anchor 的 `coordinateAuthority = TILE_MAP_VIEWPORT`，并且 resolved rect 来自同一个 `TileMapViewport.tileToScreen`，不是 overlay renderer 局部推导；`recordsAnchorOutsideVisibleRangeSuppressionReason` 和 `recordsAnchorResolutionFailedWhenPresenterRectMissing` 必须覆盖 typed failure reason。

6. 禁止一个总 `TileRenderFrame` 同时持有 raw `overlayState`、duplicated `cellWidth/cellHeight`、map/shell/overlay 全量 model 并作为长期依赖扩散；如果需要总 frame，只能作为 `TileRenderer` 类内部 `private` 嵌套类型，或 `TileRenderer.kt` 文件 top-level `private` 类型。不得使用 Kotlin `internal`，更不得 export 给其他 renderer 文件。
7. `TileMapRenderer` 负责 terrain / prop / tile sprite overlay / actor / fog / loot marker / player tile indicator / inspect cursor / targeting cursor / combat feedback；combat floating feedback 属于 map-anchored layer，不属于 modal/toast layer。player tile indicator 是玩家当前位置的低强度锚点高亮，随 actor pass 使用同一 tile bounds；inspect / targeting cursor 是模式态焦点，不得复用 player indicator 状态。

Map sublayer order 必须由 `TileLayerComposer` 或等价 typed owner 冻结，`TileMapRenderer` 只消费 composer 输出，不在 draw path 内维护第二套顺序。PR-01-1 的最小顺序为：`terrain/base -> prop/decal -> tile sprite overlay/telegraph/vfx -> actor -> fog/visibility veil -> ground loot marker -> player indicator -> inspect/targeting cursor -> combat feedback`。PR-05 可以扩展 telegraph / VFX / actor 资源细节，但不得引入与 PR-01-1 不同的 map layer authority；若顺序调整，必须同步更新 `TileLayerComposerTest` 或等价 composer-owner test。`TileRendererCanvasTest` 只能作为最终 canvas wiring 与 non-overlap 佐证，不能替代 composer owner。

Composer 输出必须是 typed `TileMapLayerPlan`，而不是裸 `List<TileVisualPlacement>` 的长期合同。`TileMapLayerPlan` 字段至少表达 `terrainBase`、`propsAndDecals`、`spriteOverlaysAndTelegraphs`、`actors`、`fogVeils`、`groundLootMarkers`、`playerIndicators`、`activeCursor`、`combatFeedback`。`activeCursor` 只接收 `TileViewportFocusProjectionResult.resolvedMode` 对应的 active projection cursor：`TARGETING` resolved 时不得同时绘制 inspect cursor；`INSPECT` resolved 时不得绘制 targeting cursor；`PLAYER` resolved 时 `activeCursor = null`。若 cursor fallback 到 `playerTile` 且 `isTooltipAnchorValid = false`，也不得生成 inspect / targeting cursor draw command。后续如果需要同时显示 secondary cursor，必须新开 PR 修改 layer contract。fog、loot marker、cursor、combat feedback 等非 sprite layer 不得强行塞进 `TileVisualPlacement`；`TileMapRenderer` 只能按 plan slice 顺序 draw，不能维护并行手写顺序。plan 对下游 renderer 必须只读，禁止暴露 mutable list。sealed command list 不作为 PR-01-1 默认方案；如果后续 PR 证明存在异构 draw command 的真实需求，必须显式替换本合同并同步更新 composer owner tests。

8. `TileShellRenderer` 负责 left rail icon column、right panel 四分区（地面物品 / 装备 / 铭刻 / 背包）、bottom HUD 四横向子区（角色画像与短数值 / hotbar cards / hotkey legend / log 与目标提示）、pane focus ring；不得重拼业务文案，只消费 presentation model。
9. `TileOverlayRenderer` 负责 tooltip、modal、toast/debug transient message；不得把具体 `ModalFrameKind` 的业务规则写进 renderer。
10. `TileTextMetrics` 或等价类型收敛 `approximateCharWidth`、`approximateLineHeight`、`truncateTextToWidth`、`wrapText`，map/shell/overlay 复用同一 metrics。`wrapText` / `truncateTextToWidth` 只能在 model / frame build phase 为稳定可见文本预计算 `TileTextLine` 或等价 line list；draw pass 只能消费预包装、预截断的文本布局，不得在每帧 draw path 对稳定文本重复 wrap / truncate。
11. `SpriteBatch.begin()` / `end()` 由 `FoundationGameScreen` 在 runtime `TileRenderer.render(...)` 外层 exactly once 管理。`TileRenderer.render(...)`、`TileRenderer.renderToCanvas(...)` 和所有 renderer pass 都不得调用 begin / end，也不得引入第二个 batch。`renderToCanvas` 是 test-only `TileCanvas` 路径，不是 `SpriteBatch` lifecycle owner。
12. renderer pass 之间共享同一 `SpriteBatch` / `TileCanvas` 时，每个 pass 退出前必须通过 `TileCanvas.flushLayer(reason)` 或等价 typed command 表达 layer boundary；runtime `GdxTileCanvas` 将该 command 映射为 `batch.flush()`，canvas test 使用 recording canvas 断言 layer boundary。layer order 由调用顺序和 flush boundary 保证，不依赖 z-coordinate、texture binding 切换或隐式 auto-flush。
13. renderer pass 内不得引入 `ShapeRenderer`、第二个 `SpriteBatch` 或 offscreen `FrameBuffer`。若 chrome 必须使用 `ShapeRenderer`，必须先在合同 PR 冻结 render pipeline 扩展点；本 PR 只允许 SpriteBatch 单一管线。
14. renderer 热循环指每帧 render 调用栈，包括 `renderToCanvas` 与 `draw*` 系列方法，不含 setup/build phase。热循环内不得新增 per-tile 临时 collection、string builder 或 wrapper 对象，也不得调用 `wrapText` / `truncateTextToWidth` 处理稳定可见文本；稳态（无 modal、无 inspect、玩家不移动）下 renderer pass 总 allocation 预算为 `< 1 KB/frame`。如无 allocation 测量基础设施，本预算作为 code-review 心智模型，实际由 immutable frame、无 cross-frame ref、precomputed text layout 和 frame ownership self-audit 约束；一旦引入 allocation 测量，该预算转为程序化合同，manual record 必须记录测量方法。
15. frame-level records 必须是 immutable；实现可用对象池或预分配数组降低分配，但 renderer 不得持有 frame 引用跨帧，也不得把可变集合泄漏给下游 renderer。
16. PR close 前必须在 `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` 写入 frame ownership self-audit 子段，字段至少包含 `frame name`、`field list`、frame 字段总数、frame 持有的 immutable model 列表、frame 通过 reference 消费的 viewport / layout truth、modalFrames order truth、anchor resolution owner、precomputed text layout fields、temporary dual-write fields、是否含 raw `OverlayState`、是否含 duplicated cell metrics、是否含完整 aggregate model；后续 PR 修改 frame 字段必须同步更新该子段。

## 5. Overlay Layer 合同

1. Layer 顺序固定为：`background < map sublayers (§4 TileLayerComposer order) < shell panes < bottom HUD slot group < passive tooltip < toast < modal backdrop < modal < modal-internal explicit tooltip < dev-only debug overlay`。这里的 bottom HUD slot group 指 §2.4 冻结的四个横向子区；dev-only debug overlay 仅指诊断/验证提示层，不是 ASCII renderer 或 ASCII fallback path，也不得渲染 full-map ASCII。
2. modal active 时，toast 不得压在 modal 上方；需要保留的即时反馈必须写入 bottom log 或 modal 内部提示，不新造 modal-above toast path。
3. Backdrop 只允许由 `TileOverlayModelBuilder` 基于 `ModalStack.top()` 的 active modal frame 生成一个 shared `TileModalBackdropModel`；禁止每个 modal 自己叠 backdrop。backdrop alpha / color 由 builder 从 stack top frame kind 配置解析进 model，renderer 只消费 `TileModalBackdropModel`，不得按 `ModalFrameKind` 再分支。frame kind 切换时 model 同帧切换，不做 fade 过渡。dev-only debug overlay 不得生成 backdrop、全屏 dim 或 fullscreen blur；只允许小区域诊断 hint，不得阻挡正常游戏视觉。
4. Tooltip 同一时间只有一个 source，优先级为：modal explicit tooltip > inspect cursor > targeting cursor > focused interactive entity。`active modal explicit tooltip` 指 modal frame kind 在合同中声明的 secondary tooltip slot，在 modal 上层渲染；passive tooltip 在 modal active 时被本层级与 suppression 规则压制。active modal explicit tooltip 与 passive tooltip 互斥，同一时间最多一个 tooltip，不存在 nested tooltip。
5. modal active 时，inspect / targeting cursor sprite 可以继续渲染在 map layer 以保留空间锚点，但 inspect / targeting tooltip 内容被抑制；modal 关闭后恢复 tooltip source 选择。
6. Tooltip anchor source 必须通过 sealed `TileOverlayAnchorKind` 或等价 typed family 表达；新增 anchor family 必须新开 PR 修改本合同。新增 family 内的具体 entity 只有在复用该 family 的 typed rect / tile coordinate authority，且不新增 renderer-local placement path 时才允许在下游 PR 扩展。不允许用鼠标位置、硬编码屏幕角落、raw screen coordinate 或上一帧残留坐标作为真源。anchor source 失效时必须立即隐藏关联 tooltip，不允许 fallback 到屏幕中心。

| Anchor family | Coordinate authority | First consumer | 覆盖对象 |
| --- | --- | --- | --- |
| `WORLD_TILE` | `TileMapViewport.tileToScreen(tile)` 或 tile center | PR-01-1 | map tile、inspect cursor、targeting cursor、ground loot marker 的 world anchor |
| `PANEL_SLOT` | shell / panel layout 输出的完整 slot rect | PR-01-1 / PR-03 | ground item slot、inventory slot、equipment slot、engraving slot、hotbar full card、talent node |
| `PANEL_ROW_OR_CARD` | presenter / layout 输出的 row 或 card rect | PR-03 / PR-07 | shop offer / sell row、world route option、stat assign row、reward/frontstage card、combat decision row、validation/settings row |
| `QUEST_ROW` | quest/log presenter 输出的 row rect | PR-06 / PR-07 | quest log row、objective row、route hint row |
| `MODAL_ROW` | `ModalFrameKind` presenter 输出的 explicit row rect | PR-01-1 / PR-03 | modal explicit row、replacement choice row、item compare row |

anchor type contract 最小形态如下；实现可以使用等价命名，但 variants、payload 字段、坐标空间和 owner 不得省略。`RectInt` 可替换为本仓库等价的 screen-space integer rect / cell-aligned rect 类型；bounds 必须由 layout / presenter 输出，renderer 只能消费，不能在 draw path 临时计算。

```kotlin
enum class TileOverlayAnchorKind {
    WORLD_TILE,
    PANEL_SLOT,
    PANEL_ROW_OR_CARD,
    QUEST_ROW,
    MODAL_ROW,
}

sealed interface TileOverlayAnchor {
    val kind: TileOverlayAnchorKind

    data class WorldTile(val tile: Point) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.WORLD_TILE
    }

    data class PanelSlot(val bounds: RectInt, val slotId: String) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.PANEL_SLOT
    }

    data class PanelRowOrCard(val bounds: RectInt, val sourceId: String) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.PANEL_ROW_OR_CARD
    }

    data class QuestRow(val bounds: RectInt, val questId: String) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.QUEST_ROW
    }

    data class ModalRow(
        val bounds: RectInt,
        val frameKind: ModalFrameKind,
        val rowId: String,
    ) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.MODAL_ROW
    }
}
```

hotbar slot bounds 指包含数字、icon 和短标签的完整卡片矩形，不是 icon 子矩形。shop、route、reward、stat assign、combat decision 等后续 PR 必须复用 `PANEL_ROW_OR_CARD` 或 `MODAL_ROW`，不得在各自 renderer 内硬编码 tooltip 坐标。

`TileOverlayModelBuilder` 或等价 owner 负责执行 tooltip priority、modal suppression、modal explicit tooltip 与 passive tooltip 互斥、toast/modal 层级裁决。`TileOverlayRenderer` 只按 `TileOverlayModel` 绘制，不得 switch `ModalFrameKind`，也不得在 draw path 重新选择 tooltip source。

`TileOverlayModel` minimum shape 固定如下；具体文本/图标字段可由后续 PR 扩展，但 arbitration 输出字段不可省略。

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

data class TileTooltipModel(
    val anchor: ResolvedTileOverlayAnchor,
    val titleLine: TileTextLine,
    val bodyLines: List<TileTextLine>,
)

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

enum class TileTooltipSource {
    MODAL_EXPLICIT,
    INSPECT_CURSOR,
    TARGETING_CURSOR,
    FOCUSED_ENTITY,
}

data class TileTooltipSuppression(
    val source: TileTooltipSource,
    val reason: TileTooltipSuppressionReason,
)

enum class TileTooltipSuppressionReason {
    ACTIVE_MODAL_SUPPRESSED_PASSIVE,
    ANCHOR_OUTSIDE_VISIBLE_RANGE,
    ANCHOR_RESOLUTION_FAILED,
    LOWER_PRIORITY_TOOLTIP,
}
```

`ColorSpec` 可替换为本仓库等价 token / resolved color 类型，但 backdrop `bounds / color / alpha` 必须来自 model；renderer 不得根据 `ModalFrameKind` 再计算 backdrop alpha 或 color。`TileModalModel.frameKind` 只作为 diagnostics / test evidence，不作为 renderer branch key。

`selectedTooltip` 为 `null` 时不得绘制 tooltip；`suppressedTooltipSources` 是 manual record / focused test evidence，不是 renderer fallback 输入。`TileOverlayLayerTest.selectsModalExplicitTooltipBeforePassiveTooltip`、`TileOverlayLayerTest.recordsSuppressedPassiveTooltipWhenModalActive`、`TileOverlayLayerTest.rendererDoesNotBranchOnModalFrameKind` 必须覆盖 builder output 与 renderer wiring 的分工。

7. Tooltip placement 顺序为 `right -> down -> left -> up`，每一步都要检查 shell content bounds 和 bottom log reserved bounds；全部不足时 clamp 并 cap width/height。本合同基于 LibGDX Y-up 坐标系：right = +X、down = -Y、left = -X、up = +Y；四方向 edge mapping 固定为：right 时 `tooltip.left = anchor.right + tooltipFlipMargin`；down 时 `tooltip.top = anchor.bottom - tooltipFlipMargin`；left 时 `tooltip.right = anchor.left - tooltipFlipMargin`；up 时 `tooltip.bottom = anchor.top + tooltipFlipMargin`。每个候选 placement 都必须用完整 tooltip rect 检查 `shellContentBounds - bottomLogReservedBounds`，不允许使用 anchor center 作为通用偏移基准。RTL locale tooltip flip mirroring 不在本 PR 实现范围内。
8. Tooltip 不得遮挡 bottom log 最新反馈区域；如果 source 附近空间不足，优先 flip，其次 clamp，最后只保留 title + body 前 `floor(maxHeight / lineHeight) - 1` 行，末尾以 `…` 表示截断。
9. Modal placement 居中于 `modalSafeBounds`，不以整个 window 中心为准；`modalSafeBounds.bottom` 是 footer hints 的 top edge / bottom-safe edge，不是 window bottom。modal max width = `min(modalMaxWidthCap, modalSafeBounds.width - 2 * modalPadding)`；modal max height = `modalSafeBounds.height - 2 * modalPadding`。`modalSafeBounds` 必须由 layout / frame 提供，不允许 modal renderer 自行从 window size、rail width、panel width 或 panel gap 重算。超出高度的 body 内容必须由 presenter 截断或 cap 行数。modal 内部 scroll 不在本 PR 实现范围内，后续需要新 PR 扩展 `OverlayRenderFrame` 字段。modal body 不得覆盖 footer hints。
10. 现有 `ModalFrameKind` 包括 `INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT / TARGETING / ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION / ACTIVE_TALENT_SLOT_CHOICE`，PR-01-1 保留其 `ModalStack` 关闭语义。`ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION / ACTIVE_TALENT_SLOT_CHOICE` 等 detail / decision frame kind 不新增对应 `UiMode`，只允许投影到现有 `UiMode.INVENTORY` / `TARGETING` / `TALENT_ASSIGN` 等 input dispatch surface；其中只有 `INSPECT / TARGETING / COMBAT_DECISION` 可按 §3 Viewport Focus Projection 影响 viewport focus mode。overlay layer、visual z-order 与 backdrop authority 必须由 `TileOverlayModelBuilder` 基于 `ModalStack.top()` 与 `OverlayRenderFrame` typed bounds 生成到 `TileOverlayModel`，不得由投影后的 `UiMode` 或 renderer draw path 重新决定。modal 关闭只触发 `ModalStack` pop；viewport snap 由 §3.7 inspect / targeting 关闭路径独立触发，不与非 spatial modal 关闭耦合。新增 frame kind 不得改变 layer order；如需新 layer，必须新开 PR 修改本合同。
11. 对同一 entity / item / actor，tooltip 与 modal 的 stat、status、icon、quality 字段必须来自同一 presenter 输出；renderer / overlay model 不允许各自从 `RenderSnapshot` 二次派生属性。
12. `ESC / Backspace` 等输入路径不得因 overlay model 拆分而回退；focused tests 必须覆盖 modal top frame、inspect、targeting、item detail 的关闭路径。

## 6. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt` | 增加 shell preferred/min world dimensions、overlay max width/height、tooltip/modal padding、deadzone token 等固定尺寸 token |
| `client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt` | 保持 shell 四区域 bounds 真源，新增 `shellContentBounds`、`modalSafeBounds`、`bottomLogReservedBounds`、`cellAlignedMapBounds`、`mapInnerPadding` primary outputs |
| `client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt` | `InfoSurfaceLayoutRequest` 接收 shell world size、map dimensions、player/focus position；不再用整张地图像素宽高推导 world size |
| `client/src/main/kotlin/com/ktome/client/render/layout/TileMapViewport.kt` 或等价命名 | 新增 typed viewport model，输出 visible tile range、tile-to-screen 映射、containsTile / clamp / deadzone 规则 |
| `client/src/main/kotlin/com/ktome/client/render/TileViewportFocusProjection.kt` 或等价命名 | 将 `OverlayState.modalFrames` / validation explicit projection 解析为 non-null viewport focus、nullable tooltip anchor validity 和 source diagnostics |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 增加 `TileOverlayModel`；保持 snapshot consumer 只做 presentation，不反向定义规则 |
| `client/src/main/kotlin/com/ktome/client/render/TileOverlayModelBuilder.kt` 或等价命名 | 负责 tooltip priority、modal suppression、selected tooltip、suppressed source evidence 和 active modal/backdrop model；renderer 不得接管 arbitration |
| `client/src/main/kotlin/com/ktome/client/render/TileOverlayAnchorResolver.kt` 或等价命名 | 在 build phase 使用当前 `TileMapViewport`、layout/presenter rect 解析 `TileOverlayAnchor` 为 `ResolvedTileOverlayAnchor`；renderer 不直接消费 viewport |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 退化为 orchestration：build model、resolve layout、resolve viewport、构造 frames、按固定 layer order 调度 renderer；不得管理 `SpriteBatch.begin/end` |
| `client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt` 或等价命名 | 作为 map sublayer order 的 typed owner，输出 `TileMapRenderer` 消费的稳定绘制顺序 |
| `client/src/main/kotlin/com/ktome/client/render/TileMapRenderer.kt` 或等价命名 | 新增 map renderer，绘制 terrain / prop / tile sprite overlay / actor / fog / loot marker / player tile indicator / inspect cursor / targeting cursor / combat feedback |
| `client/src/main/kotlin/com/ktome/client/render/TileShellRenderer.kt` 或等价命名 | 新增 shell renderer，绘制 left rail icon column、right panel 四分区、bottom HUD 四横向子区、pane focus ring |
| `client/src/main/kotlin/com/ktome/client/render/TileOverlayRenderer.kt` 或等价命名 | 新增 overlay renderer，绘制 tooltip、modal、toast/debug transient message，保证固定层级和 placement |
| `client/src/main/kotlin/com/ktome/client/render/TileTextMetrics.kt` 或等价命名 | 新增或等价收敛文本 approximate width、wrap、truncate；只在 model / frame build phase 预计算 text lines，draw pass 不调用 wrap/truncate |
| `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt` | `FoundationViewportSupport` 改为 shell fixed viewport；resize / sync 不再因地图大尺寸扩大 camera world；runtime `SpriteBatch.begin/end` 保持唯一 owner |
| `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | 不新增输入语义；仅在已有 `OverlayState` / `ModalStack` 字段不足以表达 overlay model 时做最小同步 |
| `client/src/test/kotlin/com/ktome/client/render/TileMapViewportTest.kt` | 新增 center / deadzone / clamp / small-map / full-cell-range focused tests |
| `client/src/test/kotlin/com/ktome/client/render/TileViewportFocusProjectionTest.kt` 或等价 projection focused test | 覆盖 nullable cursor resolution、anchor validity、validation explicit projection、single modal source、source diagnostics、non-spatial modal preservation 和 projection precedence |
| `client/src/test/kotlin/com/ktome/client/render/GameShellLayoutTest.kt` | 增加真实 zone 尺寸、shell world size 不随地图膨胀、map bounds cell 对齐、map-first area ratio、modal safe bounds 测试 |
| `client/src/test/kotlin/com/ktome/client/screen/FoundationViewportSupportTest.kt` | 改写为 fixed shell world size、resize 不随 snapshot map dimensions 扩张、map rect 与 large map 解耦；删除旧的 snapshot-size-driven world size 断言 |
| `client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt` 或等价 layer-order test | 覆盖 map sublayer order、actor / fog / loot marker / activeCursor / combat feedback 的相对顺序 |
| `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | 覆盖 viewport clipping、tile-to-screen 一致性、draw order、explicit layer flush boundaries、tooltip/modal non-overlap |
| `client/src/test/kotlin/com/ktome/client/render/TileOverlayLayerTest.kt` 或等价 overlay focused test | 构造 typed anchor family、modal/passive tooltip 冲突、corner flip、bottom log non-overlap、layout-owned modal safe bounds；不得依赖 mouse hover |
| `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | 复核 modal stack / inspect / targeting / item detail 语义未因 overlay layer 回退 |
| `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt` | 覆盖真实 session 下大地图 viewport、modal overlay 和 shell render model |
| `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` | 更新 harness 注册表新增 `dark-uiux-pr01-1-*` golden 项；harness 主体不变，覆盖大地图、edge clamp、inspect tooltip、modal layer |
| `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` | 记录 focused tests、owner gates、人工白盒证据与剩余风险 |
| ASCII renderer / model / manifest / evidence | 删除条目见下方 Deletion Checklist；不在影响范围表重复维护第二份删除真相 |

除明确要求新增的 `TileMapViewportTest.kt` 外，如果实现未使用上表建议文件名，必须在 manual record 和 doc-vs-implementation self-audit 中列出等价 owner 类型。等价命名必须表达职责边界，禁止用 `Helper`、`Utils`、`Manager` 或 `Support` 这类不能说明 owner 的长期类型名承接核心合同。

### Implementation Migration Locks

1. `FoundationViewportSupport` 必须迁移到 fixed shell world size / shell map rect invariant。旧的 snapshot-size-driven `worldWidth(snapshot)` / `worldHeight(snapshot)` 合同不得作为兼容路径保留。
2. `FoundationViewportSupportTest` 必须删除或改写“snapshot 尺寸变化会更新 viewport world size”的断言，改为固定 shell world size、map rect 与大地图尺寸解耦、resize 不扩张 camera world 的断言。
3. `TileMapViewportTest` 负责 map rect、deadzone、camera clamp、out-of-map fog fill；`GameShellLayoutTest` 只锁 shell 区域和 cell alignment，不复制 viewport 数学。
4. Overlay anchor 实现必须先落 typed family，再接 shop / route / reward / stat / combat decision 具体 row/card；不得在 PR-03/PR-07 中补第二套 screen-local tooltip placement。

### Token Contract

新增 token 默认落在 `UiDesignTokens.fixed` 或等价 dedicated token group，字段名必须稳定；如果实现采用等价命名，manual record 必须列出映射关系。

| Token | Type | Default value | Constraint |
| --- | --- | --- | --- |
| `shellPreferredWorldWidth` / `shellPreferredWorldHeight` | `Float` | `1280f` / `800f` | Standard shell world size for golden/manual evidence |
| `shellMinWorldWidth` / `shellMinWorldHeight` | `Float` | `1024f` / `768f` | Minimum supported shell world size |
| `cellSize` | `Int` | `32` | Existing PR-01 tile cell truth; if implementation changes it, PR must update all viewport/canvas/golden tests in the same commit |
| `deadzoneHorizontalMinCells` / `deadzoneVerticalMinCells` | `Int` | `4` / `3` | Cap priority can reduce actual cells below min in extremely narrow visible ranges |
| `deadzoneHorizontalRatio` / `deadzoneVerticalRatio` | `Float` | `0.25f` / `0.25f` | Applied to visible columns / rows before cap |
| `tooltipMaxWidth` / `tooltipMaxHeight` | `Float` | `360f` / `200f` | Must fit inside `1024x768` shell content without covering bottom log reserved bounds |
| `tooltipPadding` / `tooltipFlipMargin` | `Float` | `8f` / `4f` | 默认映射 `UiDesignTokens.spacing.sm` / `xs`，除非实现新增 explicit overlay token group |
| `modalMaxWidthCap` / `modalMaxHeight` | `Float` | `640f` / computed | width = `min(modalMaxWidthCap, modalSafeBounds.width - 2 * modalPadding)`；height = `modalSafeBounds.height - 2 * modalPadding`；`modalSafeBounds` 必须由 typed layout/frame 输出 |
| `modalPadding` / `modalBackdropAlpha` | `Float` | `18f` / `0.55f` | Padding mirrors `UiDesignTokens.spacing.lg`; alpha must match single-layer backdrop contract |
| `rightPanelSectionCount` | `Int` | `4` | 固定对应地面物品 / 装备 / 铭刻 / 背包；新增、删除或重排分区必须同步修改 §2、§6、§10.10 和 manual record |
| `bottomHudSlotCount` | `Int` | `4` | 固定对应角色画像与短数值 / hotbar cards / hotkey legend / log 与目标提示；新增、删除或重排子区必须同步修改 §2、§6、§10.10 和 manual record |

Modal worked examples:

| Shell | Formula | Expected |
| --- | --- | --- |
| `1024x768` width | `min(640, modalSafeBounds.width - 2 * modalPadding)` | locked by `GameShellLayoutTest`; panel gaps are already reflected in `modalSafeBounds.width` |
| `1280x800` width | `min(640, modalSafeBounds.width - 2 * modalPadding)` | `640f` only when typed `modalSafeBounds.width` leaves at least `676f` usable width |
| `1024x768` height | `modalSafeBounds.height - 2 * modalPadding` | locked by `GameShellLayoutTest` using PR-01 layout truth |
| `1280x800` height | `modalSafeBounds.height - 2 * modalPadding` | locked by `GameShellLayoutTest` using PR-01 layout truth |

### Deletion Checklist

本 PR 删除 client ASCII fallback / debug renderer 时，必须同步移除或证明不存在以下条目：

1. `client/src/main/kotlin/com/ktome/client/render/AsciiRenderer.kt`
2. `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
3. `client/src/test/kotlin/com/ktome/client/render/AsciiRenderModelTest.kt`
4. `client/src/main/resources/debug/tileset_foundation_ascii_*.png`
5. `game/src/main/resources/data/tilesets/index.yaml` 中的 `*.ascii*` client tileset key
6. visual manifest / source spec 中的 `asciiGlyph`、`asciiColorHex` 和 `*.ascii*` client visual key，路径至少包括 `assets-src/image/specs/*.json`、`assets-src/image/manifests/*.json`、`client/src/main/resources/manifests/*.json`、`examples/content-packs/*/visual/*.json`、`tools/src/main/resources/fixtures/content-packs/**/visual/*.json`
7. locale 中只服务 client ASCII renderer 的 key；删除前必须在 manual record 列出待删 key 和消费者扫描结果
8. golden/manual evidence 中的 `*ascii*` client screenshot 或 text snapshot

允许 `GameMap.fromAscii`、core/game test fixture 的 ASCII map literal、Python resource scripts 中的 `json.dumps(..., ensure_ascii=...)` 参数、white-box raw ASCII map summary 继续存在；这些不是 client renderer，也不得作为玩家 UI evidence。PR close 前必须记录 §9 中合并后的 ASCII 删除扫描命令结果摘要：期望 client renderer/model/manifest 字段为 0 命中；如出现 `GameMap.fromAscii` 或 core/test fixture 命中，必须标记为允许的非 client renderer 路径。

## 7. 非目标

1. 不改 `core`、`game` 规则模型，不改 `RenderSnapshot` schema，不改 save / replay / profile 合同。
2. 不新增鼠标 hover、鼠标点击移动、D-pad 或新的输入语义。若后续引入鼠标 hover，tooltip placement contract 需要新方案；本 PR 不为此预留扩展点，也不允许预先放宽 anchor source 列表。
3. 不实现 PR-03 的完整地面物品/装备/铭刻/背包 item tooltip 内容扩展，只建立 tooltip layer 与基础 placement。
4. 不实现 PR-04 的职业树 UI 细节；active slot modal 只验证 layer contract。
5. 不实现 PR-05 的 actor / tile / VFX 资源替换；map renderer 只消费已有 `ResolvedVisualAsset`。
6. 不生成新的正式 sprite sheet；`ui.frame.tooltip.body`、`ui.frame.modal.body` 等资源由 PR-02 接入。
7. 不把 validation 系列 standalone screen（`ValidationSetupScreen` / `ValidationSummaryScreen`）改成正式玩家 `ModalStack` frame；validation 仍由 validation owner 用 standalone screen 管理。
8. 不为了拆文件新增 `Helper` / `Utils` / `Manager` 类型，不新增 boolean option matrix 或 compat branch。
9. 不保留或新增 client ASCII fallback / debug renderer，不用 ASCII 截图、ASCII model 测试或 ASCII-like full-map text dump 作为 UI evidence。
10. 不实现 modal / tooltip / viewport 过渡动画；layer 切换、modal 打开、tooltip 出现均为瞬间生效。
11. 不实现 nested tooltip；status effect 二级信息必须并入主 tooltip，或留待后续 PR 显式修改合同。
12. 不实现 RTL locale tooltip flip mirroring；若后续引入 RTL，需要新方案重审 tooltip placement。
13. 不用人工白盒替代 focused viewport / canvas tests、golden 或 `verifyChanged`。

## 8. 必测行为

### Viewport Geometry

1. 对 `(1024x768, 1280x800, 1440x900) x (60x40, 70x45, 90x56)` 共 9 个组合，断言 `mapBounds` cell-aligned、shell world 不被地图尺寸撑大、窗口仍显示固定三栏 + 底部 HUD；`FoundationViewportSupportTest` 必须证明 `FoundationGameScreen` 的 `FitViewport` world size 不随 snapshot map dimensions 扩张。`GameShellLayoutTest.keepsMapAreaAtLeastHalfShellUsableContentAt1280x800` 必须锁定 §2.7 的 0.5 map-first 面积阈值，`GameShellLayoutTest.computesShellUsableContentAreaExcludingBottomHud` 必须锁定 `shellUsableContentArea` 分母排除 bottom HUD slot group，`GameShellLayoutTest.producesModalSafeBoundsAboveFooterAndBottomLog` 必须锁定 modal safe area 与 bottom log/footer hints 的避让关系。
2. 玩家位于地图中心死区内时，viewport 不因每步移动抖动；越过死区后只做最小滚动。
3. 玩家位于左上 / 右下边缘时，viewport range clamp 到地图边界，玩家仍可见且不会产生负坐标或越界 tile draw。
4. 小地图小于可视 tile count 时，`viewportTopLeftAxis = 0`、`visibleRangeAxis = 0 until mapDimensionAxis`，地图内容只通过 `innerPaddingAxis` 在 `mapBounds` 内居中，不拉伸 tile，不出现负 top-left、虚拟 tile offset 或半格。
5. §8.1 的 9 组合矩阵必须分别输出 cell-aligned 断言结果与 shell world size 断言结果；不能只用一条聚合失败隐藏半格、sub-pixel drift 或按窗口余数拉伸 tile 的具体原因。
6. 同一 viewport identity 下，玩家在 deadzone 内移动一格时 `viewportTopLeft` 不变；离开 deadzone 后 `viewportTopLeft` 只变化使玩家回到最近 deadzone 边界。
7. 窄窗口下 deadzone cap 后仍保留可滚动空间；visible tile count 太小时不得让 deadzone 覆盖整个 viewport。
8. inspect / targeting 关闭时必须 snap 到 live player-centered viewport；不能保留 inspect cursor / target tile 作为新的 center。
9. deadzone focused tests 必须覆盖 even/odd visibleCells 与 even/odd effectiveDeadzoneCells，断言 `deadzoneStart` / `deadzoneEndExclusive` 是半开区间且实际 cell 数量等于 effective deadzone cells。
10. jump snap focused tests 必须覆盖 `== threshold`、仅 X 轴超过、仅 Y 轴超过、diagonal 两轴均未超过、diagonal 任一轴超过五类场景，证明 snap 阈值按轴计算。
11. identity focused tests 必须覆盖 `changesIdentityWhenCellAlignedMapBoundsChanges` 与 `rejectsMismatchedSnapshotAndModelMapDimensions`，证明 cell-aligned bounds 参与 identity，且 snapshot metadata / model dimensions 不一致时 fail fast。
12. `TileViewportFocusProjectionTest` 或等价 projection focused test 必须覆盖 `playerModeUsesPlayerTile`、`inspectModeResolvesCursorFromOverlayOrModalState`、`targetingModeResolvesCursorFromOverlayOrModalState`、`modalTargetingCursorWinsOverStaleOverlayCursor`、`modalInspectCursorWinsOverStaleOverlayCursor`、`modalSpatialCandidateSuppressesCrossModeOverlayCandidate`、`rejectsOverlayTargetingWhenModalInspectCandidateExists`、`rejectsOverlayInspectWhenModalTargetingCandidateExists`、`sourceKindAndAnchorComeFromSameCandidate`、`rejectsDivergentOverlayAndModalCursorForSameSurface`、`nonSpatialModalPreservesCurrentSpatialProjection`、`targetingProjectionWinsOverInspectProjection`、`validationCursorRequiresExplicitInspectProjection`、`inspectFallbackUsesPlayerForViewportButSuppressesTooltipAnchor`、`targetingFallbackUsesPlayerForViewportButDoesNotConfirmTarget`、`keepsSpatialModeCandidateWhenCursorIsNull`、`usesOverlayStateModalFramesAsSingleAuthority`、`scansModalFramesTopToBottomFromBottomFirstList` 和 nullable cursor fallback 行为。`modalInspectCursorWinsOverStaleOverlayCursor` 必须断言 `sourceKind = MODAL_INSPECT`、`anchorTile` 来自 modal local cursor、stale `overlayState.inspectCursor` 不参与 anchor 或 viewport fallback。

### Renderer And Layering

13. terrain、prop、tile sprite overlay、actor、fog、ground loot marker、player tile indicator、targeting cursor、inspect cursor、combat feedback 对同一 tile 使用同一 screen transform。
14. terrain / actor / cursor / feedback pass 对同一 tile 的 `tileToScreen(tile)` 返回 byte-identical 的整数坐标；禁止各 pass 自己重算比例或偏移。
15. 不在 visible range 内的 tile / actor / feedback 不绘制；canvas test 能证明 clipping 生效。
16. canvas 测试断言 `PaneFocusAnchor.WORLD` focus ring 矩形与 §3.13 的 `layout.shell.mapBounds` 一致，不能退回完整地图像素矩形。
17. `TileRenderer` 主入口不再直接承载大段 map/shell/UI overlay 过程式函数；renderer 拆分后 `maintainabilityLint` 无新增 debt。
18. renderer layer 切换必须有 `TileCanvas.flushLayer(reason)` 或等价 typed command 边界，`TileRendererCanvasTest.recordsExplicitLayerFlushBoundaries` 必须用统一 recording canvas / spy 证明 pass order 不是依赖 texture binding、implicit z-order 或 runtime auto-flush。`FoundationViewportSupportTest.ownsSingleSpriteBatchLifecycleAroundTileRenderer` 或等价 screen-level wiring test 必须证明 runtime `SpriteBatch.begin/end` 只有 `FoundationGameScreen` 一个 owner。
19. `TileLayerComposerTest` 或等价 layer-order test 必须锁定 §4 map sublayer 顺序与 typed output shape，至少覆盖 actor、fog/visibility veil、ground loot marker、active projection cursor、combat feedback 的相对顺序，并断言 `TileMapLayerPlan` slice order；只有未来 PR 把 plan 替换为 sealed command list 时，才改为 stable `layerKey` 序列。PR-05 只能扩展该 owner，不得重建第二套 layer order。
20. `TileLayerComposerTest` 必须覆盖 `drawsOnlyActiveProjectionCursor` 或等价测试：targeting resolved 时只产生 targeting active cursor，inspect resolved 时只产生 inspect active cursor，player resolved 或 fallback anchor invalid 时不产生 inspect / targeting cursor。`TileRendererCanvasTest.overlayFrameDoesNotCarryRawOverlayState` 或 frame ownership self-audit 必须确认 `OverlayRenderFrame` 不含 raw `OverlayState` / `ModalFrame` / `TileViewportFocusProjectionResult`；`TileRendererCanvasTest` 或 frame ownership self-audit 还必须确认 overlay draw pass 不调用 `wrapText` / `truncateTextToWidth` 处理稳定可见文本，文本布局字段已在 frame build phase 预计算。

### Overlay Layer

21. Tooltip 与 modal 视觉层级高于 shell panes 和 bottom HUD；但 tooltip placement 必须避开 bottom log reserved bounds，确保 bottom log 最新反馈仍可见。
22. Modal backdrop/card 高于 tooltip 和 toast，并且 footer hints 仍可见或可读，不被 modal body 覆盖。
23. `ITEM_DETAIL / ITEM_COMPARE / INSPECT / COMBAT_DECISION / ACTIVE_TALENT_SLOT_CHOICE` 打开后，`ESC / Backspace` 等 `ModalStack` 语义不回退。
24. 中文 locale 下 tooltip / modal 长文本在最小窗口内不越界；英文长 dungeon / quest / item 名称使用同一 width-based 截断。
25. 同一 item 在 focused/passive anchored tooltip 与 `ITEM_DETAIL` modal 中显示的 title、rarity、resource line、effect summary 来自同一 presenter；只允许布局和截断策略不同。
26. nested modal 下 backdrop 只有一层，alpha / color 来自 `ModalStack.top()`；关闭顶层 modal 后，同一帧 `ModalStack.top()` 切换为下层 modal，backdrop alpha/color 同帧切换，不允许跨帧 fade 或闪烁。
27. `ModalStack.size >= 2` 时，backdrop 仍只一层，所有 modal body 均不覆盖 footer hints。
28. modal active 时，inspect cursor 可以保留可视状态，但 passive anchored tooltip 被抑制；anchor tile 离开 visible range 后 tooltip 自动隐藏。
29. `TileOverlayRenderer` 任意 frame 渲染 tooltip 数量 `<= 1`，必须覆盖 modal explicit tooltip 与 passive tooltip 同时存在的边界场景。
30. `TileOverlayLayerTest` 或等价 focused test 必须构造同一 `OverlayRenderFrame` 内同时存在 passive anchored tooltip 与 active top modal frame 的场景，断言 active modal / modal explicit tooltip 胜出且 passive tooltip 被 suppress；不得依赖 runtime mouse hover 路径证明该规则。`selectsModalExplicitTooltipBeforePassiveTooltip`、`recordsSuppressedPassiveTooltipWhenModalActive`、`rendererUsesSelectedTooltipFromModelOnly` 和 `rendererDoesNotBranchOnModalFrameKind` 必须证明 arbitration 属于 `TileOverlayModelBuilder`，renderer 只消费 model。
31. `TileOverlayLayerTest` 或等价 focused test 必须用 typed anchors 覆盖 viewport 四角 `right / down / left / up` flip，断言 tooltip 留在 shell content bounds 内且不覆盖 bottom log reserved bounds。`TileOverlayLayerTest.cornerAnchorsUseTileMapViewportResolvedWorldTileRect` 必须证明 WORLD_TILE anchor 由 resolver 使用同一个 `TileMapViewport.tileToScreen` 解析；`recordsAnchorOutsideVisibleRangeSuppressionReason` 和 `recordsAnchorResolutionFailedWhenPresenterRectMissing` 必须证明 resolver failure reason 可追溯；`TileOverlayLayerTest.usesLayoutModalSafeBoundsWithoutWindowRecalculation` 必须证明 overlay renderer 只消费 `GameShellLayout` 输出的 `modalSafeBounds`，不从 window size / rail width / panel width 二次推导。
32. 新增 overlay frame kind 必须扩展 sealed `ModalFrameKind` / typed frame；禁止使用 stringly layer id、boolean flag 或 map payload 偷渡。

### Evidence And Compliance

33. 页面视觉保持 ToME-like map-first：中央地图、左侧窄导航、右侧地面物品/装备/铭刻/背包、底部角色状态/hotbar/快捷键/log 的信息层级与 `UI/UI-demo.png` 方向一致；装饰资源、材质、立绘和 sprite 细节不作为 PR-01-1 通过条件。
34. client 侧无 `AsciiRenderer`、`AsciiRenderModel`、`AsciiRenderModelTest` 或 ASCII fallback manifest 字段依赖。
35. 桌面标题、manual record、golden/report artifact 不包含本机绝对路径。

## 9. 验证

```bash
set -e
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
set +e
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii' \
  client/src assets-src/image/specs assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
asciiScanStatus=$?
set -e
if [ "$asciiScanStatus" -eq 0 ]; then exit 1; fi
if [ "$asciiScanStatus" -gt 1 ]; then exit "$asciiScanStatus"; fi
./gradlew :client:test --tests com.ktome.client.render.TileViewportFocusProjectionTest --tests com.ktome.client.render.TileMapViewportTest --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.TileOverlayLayerTest
./gradlew :client:test --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.ui.layout.ModalStackTest --tests com.ktome.client.ui.layout.PaneFocusControllerTest
./gradlew :client:clientSmoke
./gradlew :client:goldenScreenshot
./gradlew localeLint contractLint maintainabilityLint verifyChanged
```

`rg` 必须在 CI / 本地验证环境可用；若环境缺失 `rg`，manual record 必须提供等价 `grep -rIn` 命令与输出。ASCII 删除扫描必须覆盖 Deletion Checklist 中的 client source、client resources、assets source specs、assets manifests、examples content packs、tools fixtures 和 game tileset resources；只接受 `rg` no-match 退出码 `1` 作为通过，命中退出码 `0` 必须失败，命令错误退出码 `>1` 必须原样失败。允许保留的非 client fallback 例外只能写在 manual record 的 `ASCII Deletion Scan` 子段，不能通过缩小扫描路径隐藏。`acceptanceContractLint` 必须在 PR open 后第一次 commit 跑通；后续每次修改 Acceptance Matrix、Major Decisions、Token Contract 或 Deletion Checklist 都必须重跑。

测试命名必须能从失败名反推出合同：

| Owner | Required test method names |
| --- | --- |
| `TileMapViewportTest` | `centersSmallMapWithinBounds`、`centersSmallMapWithInnerPaddingAndZeroTopLeft`、`keepsTopLeftInsideDeadzone`、`keepsDeadzoneHalfOpenForEvenAndOddCells`、`keepsThresholdEqualJumpOnDeadzonePath`、`snapsWhenHorizontalJumpExceedsThreshold`、`snapsWhenVerticalJumpExceedsThreshold`、`keepsDiagonalMoveWhenNeitherAxisExceedsThreshold`、`snapsWhenDiagonalAnyAxisExceedsThreshold`、`clampsBottomRightEdge`、`snapsBackToPlayerAfterInspect`、`producesCellAlignedVisibleRange`、`usesIntegerTileToScreenForEveryLayer`、`changesIdentityWhenCellAlignedMapBoundsChanges`、`rejectsMismatchedSnapshotAndModelMapDimensions` |
| `GameShellLayoutTest` | `keepsMapAreaAtLeastHalfShellUsableContentAt1280x800`、`computesShellUsableContentAreaExcludingBottomHud`、`producesModalSafeBoundsAboveFooterAndBottomLog` |
| `FoundationViewportSupportTest` | `keepsWorldSizeFixedWhenSnapshotDimensionsChange`、`keepsViewportWorldSizeFixedAcrossResize`、`usesShellMapBoundsInsteadOfFullMapPixels`、`ownsSingleSpriteBatchLifecycleAroundTileRenderer` 或等价 screen-level wiring method |
| `ModalStackTest` | `framesAreBottomToTopForProjection` |
| `TileViewportFocusProjectionTest` | `playerModeUsesPlayerTile`、`inspectModeResolvesCursorFromOverlayOrModalState`、`targetingModeResolvesCursorFromOverlayOrModalState`、`modalTargetingCursorWinsOverStaleOverlayCursor`、`modalInspectCursorWinsOverStaleOverlayCursor`、`modalSpatialCandidateSuppressesCrossModeOverlayCandidate`、`rejectsOverlayTargetingWhenModalInspectCandidateExists`、`rejectsOverlayInspectWhenModalTargetingCandidateExists`、`sourceKindAndAnchorComeFromSameCandidate`、`rejectsDivergentOverlayAndModalCursorForSameSurface`、`nonSpatialModalPreservesCurrentSpatialProjection`、`targetingProjectionWinsOverInspectProjection`、`validationCursorRequiresExplicitInspectProjection`、`inspectFallbackUsesPlayerForViewportButSuppressesTooltipAnchor`、`targetingFallbackUsesPlayerForViewportButDoesNotConfirmTarget`、`keepsSpatialModeCandidateWhenCursorIsNull`、`usesOverlayStateModalFramesAsSingleAuthority`、`scansModalFramesTopToBottomFromBottomFirstList` |
| `TileLayerComposerTest` | `ordersMapSublayersForFogLootCursorAndCombatFeedback`、`drawsOnlyActiveProjectionCursor` 或等价名称 |
| `TileRendererCanvasTest` | `recordsExplicitLayerFlushBoundaries`、`overlayFrameDoesNotCarryRawOverlayState`、`viewportUsesShellMapBounds`、`overlayDrawsAboveShellAndBottomHud`、`modalUsesModalSafeBounds`、`tooltipAvoidsBottomLogReservedBounds` 或等价 class-level wiring names |
| `TileOverlayLayerTest` | `sharesItemPresenterBetweenTooltipAndModal`、`suppressesPassiveTooltipWhenModalActive`、`usesTopModalBackdropOnly`、`hidesTooltipWhenAnchorLeavesVisibleRange`、`rendersAtMostOneTooltipPerFrame`、`activeModalSuppressesPassiveAnchoredTooltip`、`selectsModalExplicitTooltipBeforePassiveTooltip`、`recordsSuppressedPassiveTooltipWhenModalActive`、`rendererUsesSelectedTooltipFromModelOnly`、`rendererDoesNotBranchOnModalFrameKind`、`cornerAnchorsFlipInsideShellBounds`、`cornerAnchorsUseTileMapViewportResolvedWorldTileRect`、`recordsAnchorOutsideVisibleRangeSuppressionReason`、`recordsAnchorResolutionFailedWhenPresenterRectMissing`、`usesLayoutModalSafeBoundsWithoutWindowRecalculation` |

PR-01-1 当前不声明 `acceptanceContractLint` 已具备 test-name lint 能力；在新增自动 lint 前，manual record 必须在 `Verification Source` 中列出每个 Must Test 对应的 contract owner、测试类和方法名；`Overlay Conflict Evidence` 只记录 overlay 冲突证据，`Frame Ownership Self-Audit` 只记录 frame 字段与 ownership。若后续扩展 `acceptanceContractLint` 或新增 `testNamingLint`，必须同步更新本文档命名表和 §9 命令。`InputHandlerTest` 只断言 mode / modal stack / focus transition，不复制 viewport 计算；projection 解析只属于 `TileViewportFocusProjectionTest`，viewport 数学只属于 `TileMapViewport` 测试。

Layer / flush 测试 fixture 必须使用统一的 `RecordingTileCanvas`、`RecordingSpriteBatch` 或等价 typed spy，放在 `client/src/test/kotlin/com/ktome/client/render/` 下的共享 test fixture 路径；不允许 ad-hoc spy 散落在多个测试。若 runtime 使用 `GdxTileCanvas.flushLayer(reason) -> SpriteBatch.flush()` 映射，canvas focused test 断言 `flushLayer(reason)` 序列，screen-level wiring test 断言 `FoundationGameScreen` 是唯一 `SpriteBatch.begin/end` owner。

viewport focused lane：

```bash
./gradlew :client:test --tests com.ktome.client.render.TileViewportFocusProjectionTest --tests com.ktome.client.render.TileMapViewportTest --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest --tests com.ktome.client.render.TileRendererCanvasTest
```

map layer focused lane：

```bash
./gradlew :client:test --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest
```

overlay focused lane：

```bash
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.TileOverlayLayerTest --tests com.ktome.client.input.InputHandlerTest
```

上述命令是正式 PR 的首选 lane，并要求新增独立 `TileMapViewportTest`。`TileRendererCanvasTest` 在 focused lane 中按 class-level 运行，避免 wildcard 与方法大小写或等价命名不匹配导致 Gradle `No tests found for given includes`；该类只承担 wiring / final canvas evidence，不替代独立 owner tests。`TileMapViewportTest` 负责 viewport 数学；`FoundationViewportSupportTest` 负责 `FoundationGameScreen` / `FitViewport` world size 与 shell fixed viewport 接缝；`GameShellLayoutTest` 负责 shell bounds 与 cell alignment。不得把 viewport 数学只塞进 `GameShellLayoutTest` 或 `TileRendererCanvasTest` 规避独立 owner；如果新增 `TileTextMetrics` 或 frame 类型后触发 `maintainabilityLint`，必须优先调整 typed model 边界，不允许通过 baseline 或 allowlist 掩盖。

`TileOverlayLayerTest` 是 overlay placement 与 tooltip/modal conflict 的 owner；`TileRendererCanvasTest` 只证明最终 canvas non-overlap、draw order 与 renderer pass wiring，不得替代 overlay focused owner。

## 10. 人工白盒

manual record 的 `Overlay Conflict Evidence` 子段必须固定记录三行 checklist：`passive suppressed by modal`、`corner flip`、`bottom log reserved bounds`；每行必须列出证据来源（focused test、validation fixture、golden 或人工截图）和 PASS / FAIL。

1. 桌面端进入 `shattered_outpost` 或任一 `60x40+` zone，确认地图不会横向撑大窗口，shell 三栏和底部 HUD 保持固定结构。
2. 在 `1280x800` 下沿长廊连续移动，确认玩家留在 viewport 死区内时画面不每步抖动，越过死区后才滚动。
3. 移动玩家到左上边缘，确认 viewport clamp 后地图不出现黑边、负坐标 tile 或半格。
4. 移动玩家到右下边缘，确认 actor、fog、loot marker、cursor、combat feedback 与 tile 对齐。
5. 切换最小窗口 `1024x768`，确认 left rail、map viewport、right panel 四分区、bottom HUD 四子区不重叠。
6. 通过 `TileOverlayLayerTest` 或 validation fixture 构造同一 `OverlayRenderFrame` 内同时存在 passive item anchor 与 top active `ITEM_DETAIL` modal frame 的状态，确认 passive tooltip 被 §5.4 suppression 规则压制，item detail modal 位于 overlay layer 且不遮挡底部最新日志反馈。若运行时已有键盘路径，可补充 inventory pane 选中装备并按 `Enter` 的截图；不得为了本步新增 mouse hover 入口。本步产出 `item-modal-layer` 与 `Overlay Conflict Evidence`，不产出 `inspect-tooltip-layer`。
7. 打开 inspect / explain pane，确认进入 inspect 的首帧以 resolved inspect focus tile 初始化 viewport，nullable cursor fallback 不崩溃；`Backspace` 退一层、`ESC` 全退，关闭后 pane focus 回到 `PaneFocusAnchor.WORLD`，viewport 立即 snap 到 player-centered + clamp，下一帧 deadzone 从该 state 重算。
8. 进入 targeting 或 combat decision，确认 targeting cursor 使用同一 viewport transform，进入 targeting 的首帧以 resolved target focus tile 初始化 viewport，modal 高于 tooltip/toast 且不越出 shell content。
9. 切换 zh-CN，确认 tooltip / modal / log 长文本可读且不越界。
10. 对照 `UI/UI-demo.png`，在 manual record 的 `Image Region Comparison` 子段以 side-by-side 截图呈现，并逐行填写下表 PASS / FAIL / N/A。PR-01-1 只验收信息架构与可用性；石墙纹理、金属边框、角色立绘、徽章、等级牌、sprite art 和材质装饰差异必须标记为 PR-02 / PR-05 范围，不得作为本 PR 失败原因。

| Region | 验收点 | 通过判定 |
| --- | --- | --- |
| central map | 地图是第一视觉焦点，`mapBounds` 不被左右/底部 chrome 压缩 | `mapBounds.width * mapBounds.height >= 0.5 * shellUsableContentArea`，`shellUsableContentArea` 使用 Glossary §0.7 分母；actor / fog / loot / cursor 可读 |
| left rail | 窄 rail 只承载导航、模式入口、dungeon/floor/任务短提示 | manual record 列出实际 rail entry；不得重复完整角色状态 |
| right panel | 右侧稳定包含地面物品、装备、铭刻、背包四分区 | manual record 列出四分区实际顺序；缺任一分区为 FAIL |
| bottom HUD | 底部稳定包含角色画像与短数值、hotbar cards、hotkey legend、log 与目标提示四子区 | manual record 标注 HP、职业资源或魔法类资源、攻击/防御、hotbar、快捷键、最新日志位置；不得出现第二状态栏 |
| visual tone | 暗色石质/金属/低饱和背景服务可读性 | 禁止 marketing hero、说明页首屏、纯卡片堆、装饰渐变压过地图；材质和立绘细节差异按 PR-02 / PR-05 记录为 N/A |

11. 在长廊中先根据 `TileMapViewportTest` 或 debug overlay 输出的 `deadzoneStart` / `deadzoneEndExclusive` 选择仍处于半开区间内的连续移动路径截 still frame，确认 `viewportTopLeft` 不变；再继续移动到越过最近 deadzone 边界后截 scroll frame，确认 `viewportTopLeft` 只位移最小整 cell。两张截图必须在 manual record 标注 `viewportTopLeft`、`mapBounds`、`deadzoneStart`、`deadzoneEndExclusive`。
12. 触发 combat feedback 后立刻打开 `COMBAT_DECISION` modal，确认 feedback tile 坐标仍对齐，modal backdrop/card 在 tooltip/toast 之上，footer hints 不被覆盖。
13. 在 inspect mode 中通过键盘 inspect cursor，或通过 validation fixture / `TileOverlayLayerTest` typed anchor，将 focused anchor 分别移动到 viewport 四角实体，确认 tooltip flip right/down/left/up 后不越出 shell content；anchor 离开 visibleRange 后 tooltip 隐藏。不得使用鼠标 hover 作为唯一证据。本步产出 `tooltip-flip-corners` 与 `inspect-tooltip-layer`。
14. 打开同一装备的 focused tooltip、`ITEM_DETAIL` 和 `ITEM_COMPARE`，确认信息来源一致、仅布局差异，且 modal 不遮挡最新 bottom log。
15. 在 `ITEM_DETAIL` 中触发 `ITEM_COMPARE`，确认 backdrop 仍只一层、footer hints 可读、第二层 modal body 不覆盖第一层 modal title。
16. 提交 manual record 时必须贴出 `ASCII Deletion Scan` 摘要，格式为 manual record 内嵌 fenced code block 或 `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay/ascii-deletion-scan.txt` 文本证据；内容包含 rg/grep 命令、命中数、允许的非 client renderer 命中标注。
17. 提交 manual record 时必须贴出 `FoundationViewportSupportTest`、`TileViewportFocusProjectionTest` 和 `TileLayerComposerTest` focused lane 摘要，证明 screen viewport 接缝、projection owner 与 map sublayer owner 均已由独立测试锁定；projection 摘要必须列出 `TileViewportFocusSourceKind`，validation explicit projection 还必须列出 `ValidationProjectionReason`；不能只用最终截图代替 owner evidence。

必填证据映射：

| Evidence label | 来源步骤 |
| --- | --- |
| `dark-uiux-pr01-1-viewport-deadzone-still` | §10.11 |
| `dark-uiux-pr01-1-viewport-deadzone-scroll` | §10.11 |
| `dark-uiux-pr01-1-viewport-edge-clamp-top-left` | §10.3 |
| `dark-uiux-pr01-1-viewport-edge-clamp-bottom-right` | §10.4 |
| `dark-uiux-pr01-1-shell-min-window` | §10.5 |
| `dark-uiux-pr01-1-inspect-tooltip-layer` | §10.13 |
| `dark-uiux-pr01-1-item-modal-layer` | §10.6 |
| `dark-uiux-pr01-1-overlay-conflict-fixture` | §10.6 |
| `dark-uiux-pr01-1-targeting-cursor-viewport` | §10.8 |
| `dark-uiux-pr01-1-focus-projection-resolution` | §8.12 / §10.17 |
| `dark-uiux-pr01-1-foundation-viewport-fixed-world` | §8.1 / §10.17 |
| `dark-uiux-pr01-1-map-sublayer-order` | §8.19 / §10.17 |
| `dark-uiux-pr01-1-modal-backdrop-stack` | §10.15 |
| `dark-uiux-pr01-1-combat-feedback-with-modal` | §10.12 |
| `dark-uiux-pr01-1-tooltip-flip-corners` | §10.13 |
| `dark-uiux-pr01-1-item-tooltip-vs-modal-parity` | §10.14 |
| `dark-uiux-pr01-1-ascii-deletion-scan` | §10.16 |
| `dark-uiux-pr01-1-tome-layout-reference` | §10.10 |
