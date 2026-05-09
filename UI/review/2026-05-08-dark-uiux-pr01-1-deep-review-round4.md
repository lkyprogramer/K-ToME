# Dark UI/UX PR-01-1 方案深度 Review 报告（第四轮）

> 评审日期：2026-05-08
> 评审范围：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`（296 行版本）
> 上游报告：[Round 1](./pr01-01.md)、[Round 2](./pr01-review2.md)、[Round 3](./review03.md)
> 本轮定位：Round 3 列出 P0(7) / P1(15) / P2(20+) 共 ~42 项。本轮做两件事：(a) 验证 Round 3 三类清单的修复情况；(b) 在新版本上挑出仍存在的合同瑕疵、新引入的边角矛盾与可被实施者一脚踩塌的细节。
> 视角：合同自洽 / 边界可计算 / 文档间引用一致 / 测试与 manual record 闭环 / Roguelike 玩家体验底线。

---

## 0. TL;DR

Round 3 三类清单修复进度：

| 等级 | 数量 | 状态 |
| --- | ---: | --- |
| P0 | 7 | **全部已修 ✅**（A1/A4/A5/A6/B7/B9/C6 全部落地） |
| P1 | 15 | **全部已修 ✅**（A2/A3/A7/B1–B6/B8/B10/C1–C5/C7–C10 全部落地） |
| P2 | 20+ | **基本已修 ✅**（D1/D3、E1–E9、F1–F12、R1–R6 全部落地；D2 部分落地——§2.7 已加程序化阈值，但 Acceptance Matrix UI01-1-M05 的 source 仍写 §2 整段未指向 §2.7） |

**整体定级**：`Approve, ready to merge after addressing 2 P0 + 8 P1 cleanups`。

本轮新发现：

- **P0 = 2**（N1 deadzone cap 在极小 visibleCells 下产生负数；N8 modalMaxWidth 默认值在 1024×768 最小窗口下超出 shell content 宽度）
- **P1 = 8**（N2 / N4 / N5 / N6 / N7 / N9 / N15 / N19）
- **P2 = 14**（N3 / N10 / N11 / N13 / N14 / N16 / N17 / N20 / N21 / N23 / N25 / N27 / N28 / N32 / N34 / N37 / N38 / N40 / N41）

P0 不修会导致合同首次实施时即触发未定义行为或 golden 抓不到；P1 不修会让实施者在边角自由选择，未来出 bug 时合同抓不住责任。P2 是清理项，可与实现 commit 合并处理。

---

## 1. Round 3 修复对账

### 1.1 P0 全部已修 ✅

| Round 3 # | 验证位置 | 状态 |
| --- | --- | --- |
| A1 TileMapViewportTest 三处一致 | §6 line 144 + §0 Acceptance Matrix line 35 + §9 line 277 "要求新增独立 TileMapViewportTest" | ✅ 选 A，三处对齐 |
| A4 §8.9 / §5.1 layer 措辞冲突 | §8.15 改为 "视觉层级高于 ... 但 placement 必须避开 bottom log reserved bounds" | ✅ |
| A5 §3.6 滚动量缺方向 | §3.6 "滚动方向 = focus tile 越出 deadzone 的方向（focus tile 在 deadzone 右侧时 viewport 向右滚，左 / 上 / 下同理）" | ✅ |
| A6 §Major Decision 4 列全 PR-01 finding | §0 Major Decision 4 增加 outcome body 行数 cap、talent tones 对齐、outcome recap hash 更新 | ✅ |
| B7 modal explicit tooltip 在 layer 中位置 | §5.1 显式：`... < modal < modal-internal explicit tooltip < dev-only debug overlay` | ✅ |
| B9 SpriteBatch begin/end 归属 | §4.11 "由 TileRenderer.renderToCanvas 在最外层一次性管理" | ✅ |
| C6 Token Contract 默认值 | §6 表格全部给具体值（tooltipMaxWidth=360f / tooltipMaxHeight=200f / tooltipPadding=8f / tooltipFlipMargin=4f / modalMaxWidth=640f / modalPadding=18f / modalBackdropAlpha=0.55f） | ✅ |

### 1.2 P1 全部已修 ✅

| Round 3 # | 状态 |
| --- | --- |
| A2 §6 与 Deletion Checklist 桥接 | ✅ §6 表最后一行：`ASCII renderer / model / manifest / evidence` → 删除条目见 Deletion Checklist |
| A3 §8.5 / §8.18 重叠 | ✅ §8.9 改用 `tile sprite overlay`，与 §3.12 byte-identical 断言互补 |
| A7 UI01-1-M07 source 引用 | ✅ 改为 "§0 / §6 Deletion Checklist / §7 / §9 governance, anti-bloat 与验证命令" |
| B1 viewportTopLeft / visibleRange 持久化 | ✅ §3.4 "非 identity 持久字段，identity 一致时随 deadzone 计算更新，identity 不一致时随 prev state 一起丢弃" |
| B2 sub-cell mapBounds snap | ✅ §3.2 "sub-cell 像素余数变化只更新 inner padding，不丢弃 prev state" |
| B3 小地图 deadzone 短路 | ✅ §3.10 "mapDimensions < visibleCells 的轴必须短路 deadzone" |
| B4 inspect deadzone 锚点 | ✅ §3.5 "inspect / targeting 期间 focus tile = cursor / target tile" |
| B5 tooltip flip 对齐 anchor 边 | ✅ §5.7 "right placement = tooltip 左缘贴 anchor 右缘 + tooltipFlipMargin" |
| B6 modal max height 与未来 scroll | ✅ §5.9 给公式 + 未来 PR 扩展 OverlayRenderFrame |
| B8 dev-only debug overlay 不允许 backdrop | ✅ §5.3 "不得生成 backdrop、全屏 dim 或 fullscreen blur" |
| B10 ShapeRenderer / 第二 batch 禁止 | ✅ §4.13 |
| C1 player tile 跨地图大跳变 | ✅ §3.8 "跳变超过当前轴 deadzone cells 数量时 → snap" |
| C2 identity 多字段同帧变化 snap | ✅ §3.3 "snap 仅基于本帧 final state 计算一次" |
| C3 RecordingSpriteBatch 标准化 | ✅ §9 强制路径 `client/src/test/kotlin/com/ktome/client/render/RecordingSpriteBatch.kt` |
| C4 manual record 子段清单 | ✅ §0 Canonical Artifact 列出七个必填子段 |
| C5 allocation 测量降级路径 | ✅ §4.14 "如无 allocation 测量基础设施，本预算作为 code-review 心智模型" |
| C7 cellSize token 归属 | ✅ §6 Token Contract 加 `cellSize: Int = 32` |
| C8 Golden namespace 与 PR-01 关系 | ✅ §0 Canonical Artifact 末段 "PR-01 已 ship 的 dark-uiux-pr01-* golden 不得静默删除" |
| C10 nested tooltip 抑制测试 | ✅ §8.23 "TileOverlayRenderer 任意 frame 渲染 tooltip 数量 <= 1" |

### 1.3 P2 表述清理（基本已修 ✅）

| Round 3 # | 状态 |
| --- | --- |
| D1 §1.6 表述强度 | ✅ 改为 "执行 §0 Major Decision 1 的 Tile-only 渲染合同" |
| D2 UI01-1-M05 source 程序化锚点 | ⚠️ §2.7 已加 `mapBounds.width * mapBounds.height >= 0.5 * shellUsableContentArea` 阈值，但 Acceptance Matrix UI01-1-M05 source 写 "§2 + §10.10 manual record"，未指向 §2.7（见本轮 N20） |
| D3 manual record 模板未指明 | ✅ "模板参考 UI/manual-records/dark-uiux-pr01-shell.md" |
| E1–E9 | ✅ 全部落地 |
| F1 Glossary 升格 | ✅ §0 已设独立 Glossary 段 |
| F2 Token Contract 列结构 | ✅ 拆为 Default value + Constraint |
| F3 Deletion Checklist 第 6/7 条范围 | ✅ 第 6 条列具体路径模式；第 7 条要求 manual record 列待删 key |
| F4 ensure_ascii 路径 | ✅ "Python resource scripts 中的 json.dumps(..., ensure_ascii=...) 参数" |
| F5 §8 必测行为分组 | ✅ 加 4 个小标题 |
| F6 "tile sprite overlay" 全文统一 | ✅ §3.12 / §4.7 / §8.9 已统一；§5.1 用 "props" 措辞，与 Glossary 兼容 |
| F7–F12 | ✅ 全部落地 |
| R1–R6 | ✅ 全部落地 |

---

## 2. 本轮新发现的硬阻塞（P0 = 2）

### N1. §3.5 deadzone cap 公式在 visibleCells < 2 时产生负数 — 未定义行为

**位置**：§3.5 line 84

```
实际 deadzone cells 使用 `max(minCells, floor(visibleCells * ratio))`，
并 cap 到 `visibleCells - 2`，保证两侧至少各有 1 cell 滚动余量。
```

**矩阵推演**：

| visibleCells | floor(vc*0.25) | max(4, .) | cap = vc - 2 | 最终 |
| ---: | ---: | ---: | ---: | ---: |
| 20 | 5 | 5 | 18 | **5** ✓ |
| 10 | 2 | 4 | 8 | **4** ✓ |
| 4 | 1 | 4 | 2 | **2** ✓ |
| 3 | 0 | 4 | 1 | **1** ✓ |
| 2 | 0 | 4 | 0 | **0** ✓（边界） |
| 1 | 0 | 4 | **-1** | **未定义 ❌** |
| 0 | 0 | 4 | **-2** | **未定义 ❌** |

虽然支持的最小窗口是 `1024×768` × `cellSize=32`（visibleCells ≈ 26 列 / 16 行甚至更多），实际 visibleCells 不会小到 < 2，但合同没有给出 floor。一旦未来 cellSize 或 layout 变化（Token Contract cellSize 标注 "if implementation changes it, PR must update all viewport/canvas/golden tests in the same commit" 是允许的），就可能触发负 cap。负 cap 会让 deadzone "= -1 cell"，要么实现做 `Math.abs`、要么 silent overflow，结果在 viewport 计算里出 NaN。

**最小修复**：§3.5 cap 公式改为 `max(0, visibleCells - 2)`，并显式说明：

> "cap = max(0, visibleCells - 2)；当 cap = 0 时 deadzone 退化为 0 cells，viewport 严格跟随 player（视为 STRICT_FOLLOW 退化），不视为 bug。"

### N8. §6 Token Contract `modalMaxWidth = 640f` 在 1024×768 + 双侧 panel 下超出可用宽度

**位置**：§6 Token Contract line 168

```
| `modalMaxWidth` / `modalMaxHeight` | `Float` | `640f` / computed |
  Height = `footerTop - headerBottom - 2 * modalPadding`; width also capped by shell content width |
```

**算术推演**（数据来自 `UiDesignTokens.fixed`）：

```
shellLeftRailMinWidth   = 184f
shellRightPanelMinWidth = 240f
shellMinWorldWidth      = 1024f
modalPadding            = 18f

shellContentWidth (在 1024×768 最小窗口) ≈ 1024 - 184 - 240 = 600f
modalMaxWidth_safe       ≈ 600 - 2 * 18 = 564f
```

合同给的 `modalMaxWidth = 640f` **比 1024×768 下 shell content 宽度 + padding 还宽 76f**。约束列虽然写了 "width also capped by shell content width"，但默认值与最小窗口约束直接矛盾——实施者必然要在两者间二选一：

- 选默认值 640f：在 `dark-uiux-pr01-1-shell-min-window` golden 里 modal 会越过 right panel 边界
- 选 cap 后值 564f：默认值变成 informational only，不能用于 golden 一致性

**最小修复**（任选其一）：

- **选 A（推荐）**：默认值改为 `min(640f, shellContentWidth - 2 * modalPadding)`，并把 "受最小窗口约束的稳定值 = 564f" 写进 Token Contract 的 Constraint 列。
- **选 B**：默认值降到 `560f`（向下取整保留 4f 安全 buffer），约束列说明 "default 在 1024×768 下严格不越界；1280×800 / 1440×900 下仍按此默认值，剩余空间作 padding"。

附带：modalMaxHeight 标 "computed" 也是同理风险（见 N9 P1）。

---

## 3. 合同灰色地带（P1 = 8）

### N2. §3.8 player 跨帧跳变阈值 `>` vs `>=` 未定

§3.8 "玩家 tile 在单帧内**跳变超过**当前轴 deadzone cells 数量时" — "超过" 在中文里通常是严格大于（>），但 viewport 数学里 `>=` 才能保证合理 snap。

**修复**：明示 ">"：

> "Δtile > deadzoneCells 时触发 snap；Δtile == deadzoneCells 时仍走最小滚动算法（focus tile 恰好滑到 deadzone 边缘，不触发 snap）。"

或同等清晰的 `>=` 语义。

### N4. §3.5 "visible range 的 tile 几何中心" 在偶数 visibleCells 下未定义中心 cell

§3.5 "死区默认以 visible range 的 tile 几何中心为基准"。但 visibleCells = 8 时 "中心" 是第 3 cell 还是第 4 cell？

**影响**：deadzone 边界 = 中心 ± deadzoneCells / 2。中心定义不明会让两个实现得到 ±1 cell 的不同 visible range，导致 golden hash 跨实现不稳定。

**修复**：§3.5 加一句：

> "几何中心 = `floor(visibleCells / 2)` 索引；deadzone 跨度 = 中心 ± `floor(deadzoneCells / 2)`，奇偶性差异由向下取整一致化。"

或者直接给 `[centerLow, centerHigh]` 公式。

### N5. §5.7 right `+` / down `-` 方向标注不对称，读着易反

§5.7：

```
right placement = tooltip 左缘贴 anchor 右缘 + `tooltipFlipMargin`
down  placement = tooltip 上缘贴 anchor 下缘 - `tooltipFlipMargin`
```

LibGDX Y-up 里 down placement 用 `-` 是对的（Y 越小越下），但 right 用 `+`、down 用 `-` 排版让人误以为 down placement 把 tooltip 推回 anchor 内部。实施者可能写 `+ margin` 反而得到 down 方向"重叠"的 bug。

**修复**：统一表述为 "tooltip 在 anchor placement 方向外侧 `tooltipFlipMargin` 像素，无符号"，或显式注明坐标系：

> "本合同基于 LibGDX Y-up 坐标系：right = +X / down = -Y / left = -X / up = +Y。flip 各方向公式为 `tooltipEdge = anchorEdge + sign(direction) * tooltipFlipMargin`。"

### N6. §5.4 tooltip source 优先级 — "keyboard selection" 与 "focused item/detail source" 区分不明

§5.4 优先级链：

```
active modal explicit tooltip > inspect cursor > keyboard selection > focused item/detail source
```

"keyboard selection" 与 "focused item/detail source" 在键盘优先 UI 里几乎是同一件事（focus = keyboard selection）。两个并列项无法判别哪种 anchor 优先。

**修复**：合并或改写为：

```
modal explicit tooltip > inspect cursor > targeting cursor > focused interactive entity (inventory / equipment / talent / quest)
```

并在 Glossary 加一条：

> "focused interactive entity 指当前 PaneFocusController 选中的、具有可见 bounds 的 entity；它已经隐含 keyboard selection 语义。"

### N7. §5.6 anchor source 列表 "等" — 与 §7.2 "本 PR 不为此预留扩展点，不允许预先放宽 anchor source 列表" 直接冲突

§5.6：

> "Tooltip anchor 必须来自 tile center、inventory slot bounds、equipment slot bounds、talent tree node bounds、quest log row bounds **等**具有可见 bounds 的 entity"

§7.2 末段：

> "若后续引入鼠标 hover，tooltip placement contract 需要新方案；本 PR 不为此预留扩展点，**不允许预先放宽 anchor source 列表**。"

§5.6 用 "等" 说列表非穷尽 → 实施者可以新增 anchor 类型，与 §7.2 "不允许预先放宽" 直接矛盾。

**修复**：§5.6 改为穷尽列表，去掉 "等"，并明确：

> "本列表为 PR-01-1 的穷尽合同；新增 anchor source 必须新开 PR 修改本合同。"

如果实施时发现确有遗漏（如 hotbar slot bounds），现在补进去而非留 "等"。

### N9. §6 Token Contract `modalMaxHeight = computed` 缺乏 worked example，golden 跨窗口不稳定

§6：

```
| modalMaxHeight | Float | computed | Height = footerTop - headerBottom - 2 * modalPadding |
```

`footerTop` / `headerBottom` 在不同 shell 高度下不同，没有 worked example 时 reviewer 无法判断 golden 高度是否合规。

**修复**：在 Token Contract 下方增加示例段：

```
worked example (shellMinWorldHeight = 768f):
  headerBottom    = 64f   # PR-01 GameShellLayout 实测
  footerTop       = 768 - shellBottomHudHeight = 768 - 224 = 544f
  modalMaxHeight  = 544 - 64 - 2*18 = 444f

worked example (shellPreferredWorldHeight = 800f):
  headerBottom    = 64f
  footerTop       = 800 - 224 = 576f
  modalMaxHeight  = 576 - 64 - 36 = 476f
```

实际数值需要 implementer 验证 PR-01 真源；如不便给数字，至少要在 §6 写明 "worked example 在实施 commit 中由 GameShellLayoutTest 锁定"。

### N15. §4.6 "private / internal" — Kotlin `internal` 是模块作用域，会跨 renderer 子类型

§4.6：

> "如果需要总 frame，只能作为 `TileRenderer.renderToCanvas` 内部 `private` / `internal` orchestration 局部类型，不得 export 给文件外或 renderer 子模块。"

Kotlin 的 `internal` = 模块（module）可见，client 整个 Gradle module 内任何 .kt 文件都能访问。如果 `TileMapRenderer.kt`、`TileShellRenderer.kt`、`TileOverlayRenderer.kt` 与 `TileRenderer.kt` 在同一 module，`internal` 等于公开给所有 renderer 子模块——正是合同要禁止的扩散路径。

**修复**：§4.6 把 `internal` 删除，只保留：

> "...只能作为 `TileRenderer` 类内部 `private` 嵌套类型或 `TileRenderer.kt` 文件 top-level `private` 类型；不得使用 `internal`，更不得 export 给其他 renderer 文件。"

### N19. §0 Failure Rule "ASCII 删除导致测试断裂时...不允许回滚 client ASCII renderer" 缺 client 限定

§0 Failure Rule 末段：

> "ASCII 删除导致**测试**断裂时，优先迁移测试到 `RenderSnapshot` vector 断言或 Tile canvas/golden，不允许回滚 client ASCII renderer。"

但 §0 Major Decision 1 + Deletion Checklist 明确允许 `GameMap.fromAscii`、core/game test fixture 的 ASCII map literal 保留。如果删除过程中 game-side ASCII fixture 被误删导致 `GameMapFromAsciiTest` 断裂，按 Failure Rule 字面读法 "测试" 无范围限定，会被错误解释为 "继续删 game/core ASCII fixture"。

**修复**：明示 client / game 边界：

> "**client** ASCII renderer / model / manifest 删除导致 client 测试断裂时，优先迁移到 RenderSnapshot vector 断言或 Tile canvas/golden，不允许回滚 client ASCII renderer。core / game 模块 ASCII fixture 不在本 PR 删除范围；若误删导致 game test 断裂，必须立刻恢复 core / game ASCII fixture 而不是回滚 client ASCII。"

---

## 4. 表述与结构清理（P2 = 14）

### N3. §3.4 identity 字段 `map dimensions` 与 `cell-aligned map bounds` 部分重叠

`map bounds` 由 `mapDimensions × cellSize + inner padding` 推导，两者并列为 identity 略冗余。建议改为：

> "identity 至少包含 zone/session id、cell size、cell-aligned mapBounds（含 mapDimensions）、active focus mode。"

并在 Glossary 加一条 "mapDimensions = (rows, cols) of map content; mapBounds = pixel rect derived from mapDimensions, cellSize, inner padding"。

### N10. §0 Acceptance Matrix UI01-1-M02 fastCheck "或等价 focused test" 留余地过宽

```
| UI01-1-M02 | ... | TileMapViewportTest, GameAppLifecycleTest / FoundationGameScreen viewport focused test 或等价 focused test |
```

"或等价 focused test" 让 owner gate 失去确定性。Round 3 已对 UI01-1-M01 做了 "选 A" 处理，UI01-1-M02 没同步。

**修复**：要么固定具体测试类名，要么删掉 "或等价 focused test"——保留 `TileMapViewportTest` + `FoundationViewportSupportTest`（如不存在则要求新增）作为硬合同。

### N11. §3.13 与 §8.12 verbatim 重复

```
§3.13: PaneFocusAnchor.WORLD focus ring 绘制在 layout.shell.mapBounds，不是完整地图像素矩形。
§8.12: PaneFocusAnchor.WORLD focus ring 绘制在 layout.shell.mapBounds，不是完整地图像素矩形。
```

两处一字不差。允许合同+测试断言重复，但修订时容易只改一处忘另一处。建议 §8.12 改为 "见 §3.13；canvas 测试断言 focus ring 矩形与 layout.shell.mapBounds 一致"。

### N13. Glossary §3 与 §5.4 用词不完全统一

- Glossary §3：modal explicit tooltip 指 modal frame kind **在合同中声明**的内部 secondary tooltip slot
- §5.4：active modal explicit tooltip 指 modal frame kind **明确声明**的 secondary tooltip slot

意思一致，但 "在合同中声明" / "明确声明" 是两种措辞。建议统一为前者。

### N14. §3.7 重复 §3.2 的 snap 触发规则

§3.2 已说 active focus mode 字段值变化触发 snap；§3.7 又说 "关闭瞬间必须 snap 回 player-centered + clamp state"。冗余。

**修复**：§3.7 改为 "Inspect / targeting 关闭时按 §3.2 规则触发 snap 回 player-centered + clamp state；关闭后下一帧 deadzone 以该 state 为新基线。"

### N16. §10 步骤与 §10.16 evidence 标签缺一对一映射

§10.16 列了 14 个 evidence 标签，但 §10.1–10.15 没有显式说哪一步产出哪个标签。例如 `inspect-tooltip-layer` 由 §10.6 还是 §10.13 产出？`tome-layout-reference` 由 §10.10 产出？reviewer 必须靠猜。

**修复**：§10.16 改为表格：

| label | 来源步骤 |
| --- | --- |
| `viewport-deadzone-still` / `-scroll` | §10.11 |
| `viewport-edge-clamp-top-left` | §10.3 |
| `viewport-edge-clamp-bottom-right` | §10.4 |
| `shell-min-window` | §10.5 |
| `inspect-tooltip-layer` | §10.6 / §10.13（实施时确认） |
| `item-modal-layer` | §10.6 |
| `targeting-cursor-viewport` | §10.8 |
| `modal-backdrop-stack` | §10.8 / nested |
| `combat-feedback-with-modal` | §10.12 |
| `tooltip-flip-corners` | §10.13 |
| `item-tooltip-vs-modal-parity` | §10.14 |
| `ascii-deletion-scan` | §10.15 |
| `tome-layout-reference` | §10.10 |

### N17. §9 lint 命令顺序 — `rg` ASCII 扫描应前置以快速失败

当前 §9 顺序：acceptanceContractLint → focused tests → smoke / golden → localeLint contractLint maintainabilityLint verifyChanged → 三条 `rg` scan。

如果 ASCII 还没删干净，等到所有重型任务跑完再发现 → CI 时间成本高。`rg` 是秒级命令，应放在 `acceptanceContractLint` 之后立即跑。

**修复**：调整 §9 命令块顺序，把三条 `rg` 放到 `./gradlew acceptanceContractLint` 之后；smoke / golden / verifyChanged 放最后。

### N20. §0 Acceptance Matrix UI01-1-M05 source 仍指 §2 整段而非 §2.7

```
| UI01-1-M05 | §2 ToME-like map-first shell 与设计理念 + §10.10 manual record | ...
```

§2.7 是程序化阈值（mapBounds 占 shellUsableContentArea ≥ 50%）；§2.1–2.6 是设计原则。fastCheck 需要程序化 source 才能挂 GameShellLayoutTest 断言。

**修复**：source 改为 "§2.7 程序化阈值 + §2.1–2.6 设计原则 + §10.10 manual record"。

### N21. §10.16 `inspect-tooltip-layer` 标签来源不明（与 N16 同根）

如 N16 表所示，inspect-tooltip-layer 究竟由 §10.6（inventory item detail）还是 §10.13（inspect 四角 hover）产出？语义上更像后者，但 §10.6 也用 "tooltip / item detail modal" 描述。

**修复**：在 §10.6 加注 "本步产出 `item-modal-layer`，不产出 inspect-tooltip-layer"；§10.13 加注 "本步产出 `tooltip-flip-corners` 与 `inspect-tooltip-layer`（共四角各一帧或汇总一帧）"。

### N23. visibleCells / visibleRange / visibleRange.cellCount 三种术语未在 Glossary 收口

§3.5、§3.10、§8.6、§8.7 混用 "visible cells"、"visible range"、"可视 tile count"。Glossary 只定义 "stage viewport / map viewport / tile sprite overlay / modal explicit tooltip" 四条。

**修复**：Glossary 加一条：

> "visibleCells = 当前 mapBounds 在 tileToScreen 下能容纳的整 cell 数量，按轴分（visibleColumns / visibleRows）；visibleRange = visibleCells 对应的 tile 索引区间 [topLeft, bottomRight]。"

### N25. §8.21 nested modal manual coverage 缺失

§8.21 自动断言 nested modal backdrop 单层 + footer 不被覆盖。但 §10 manual 没有 nested modal 步骤，`modal-backdrop-stack` 标签虽然在 §10.16 列了，但没有对应 §10 步骤。

**修复**：§10 增加一步 "§10.16 nested modal：在 ITEM_DETAIL 中触发 ITEM_COMPARE，确认 backdrop 仍只一层、footer hints 可读、第二层 modal body 不覆盖第一层 modal title。"

### N27. §10.6 "inventory item detail" 措辞不够具体

§10.6 "打开 inventory item detail，确认 tooltip / item detail modal 位于 overlay layer" — 实施者不知道按 `i` 还是右键还是直接键盘 enter。建议：

> "在 inventory pane 选中一个装备 → 按 `Enter` 触发 `ITEM_DETAIL` modal（或 PR-01 真源对应键位），同时 hover 触发 passive tooltip；确认 tooltip 与 item-detail modal 同时存在时，passive tooltip 被 §5.4 抑制规则压制。"

### N28. §0 Acceptance Matrix UI01-1-M03 whitebox = N/A 与 §4.16 frame ownership self-audit 关系

M03 whitebox=N/A 意味着无需 manual record 截图，但 §4.16 要求 manual record 写入 frame ownership self-audit 子段。Self-audit 是 doc artifact 不是 screenshot，所以 N/A 没错；但 reviewer 看到 N/A 可能误以为 manual record 完全不需要覆盖 M03。

**修复**：M03 whitebox 列改为 `N/A (manual self-audit only, no screenshot)` 或 `doc-only`。

### N32. §4.16 self-audit 字段 "是否含 raw OverlayState" 全部预期为 false → 退化为形式

§4.16 字段：

```
- 是否含 raw OverlayState
- 是否含 duplicated cell metrics
- 是否含完整 aggregate model
```

按 §4.6 设计三个都应该是 "false"。self-audit 退化为 checkbox 流程。建议加正向字段：

> "- frame 字段总数（用于 PR-02+ 修改时对比膨胀）
> - frame 持有的 immutable model 列表
> - frame 通过 reference 消费的 viewport / layout truth"

### N34. §6 影响范围 GoldenScreenshotHarnessTest 行的预期改动描述

```
| GoldenScreenshotHarnessTest.kt | 更新或新增 dark-uiux-pr01-1-* golden ... |
```

行文说 "更新或新增" — 但 harness test 类本身代码可能不需要改，需要改的是 `dark-uiux-pr01-1-*` 数据 / 注册 / fixture。建议改为：

> "更新 harness 注册表新增 `dark-uiux-pr01-1-*` golden 项；harness 主体不变。"

### N37. §0 Failure Rule 末段 "不允许回滚 client ASCII renderer" — 删除后无回滚目标，应改为前向约束

PR-01-1 删除 client ASCII 后，仓库已无回滚目标。"不允许回滚" 实际是面向后续 PR 的合同。建议：

> "**PR-01-1 之后** 不允许任何 PR 重新引入 client ASCII renderer / model / manifest 字段；ASCII 测试迁移由 RenderSnapshot vector 断言或 Tile canvas/golden 承接。"

### N38. §5.10 "关闭语义继续由 ModalStack 管" 与 §10.7 "viewport 立即 snap" 范围分离需互相 reference

§5.10 不提 viewport snap；§10.7 不提 ModalStack pop。两者实际是 modal 关闭的两个独立 side-effect，建议在 §5.10 末尾加：

> "modal 关闭只触发 ModalStack pop；viewport snap 由 §3.7 inspect/targeting 关闭路径独立触发，不与 modal 关闭耦合。"

### N40. §7.7 "validation overlay" 措辞模糊

§7.7 "不把 validation overlay 改成正式玩家 modal stack；validation 仍由 validation owner 管理。"

"validation overlay" 在仓库实际指 `ValidationSetupScreen`、`ValidationSummaryScreen` 等独立 standalone screen，不是 overlay。建议改为：

> "不把 validation 系列 standalone screen（`ValidationSetupScreen` / `ValidationSummaryScreen`）改成正式玩家 ModalStack 帧；validation 仍由 validation owner 用 standalone screen 管理。"

### N41. §8.5 cell-aligned 自检多窗口与 §8.1 矩阵的去重

§8.1 已断言 9 组合下 mapBounds cell-aligned；§8.5 单独断言 `1024x768 / 1280x800 / 1440x900` 下 `mapBounds.{w,h} % cellSize == 0` — 前者 superset 后者。建议 §8.5 删除或合并：

> "§8.5 已包含在 §8.1 9 组合矩阵；本条删除以避免双重维护。"

---

## 5. Roguelike 玩法与跨 PR 稳定性复核

| 维度 | 二轮 | 三轮 | 四轮 | 说明 |
| --- | --- | --- | --- | --- |
| 合同冻结完整度 | B+ | A- | **A** | Token Contract 给具体值 + Glossary + Deletion Checklist 全闭环；只剩 N1/N8 两个数学边界 |
| 内部一致性 | B | B+ | **A-** | A1–A7 已消；剩 N3/N10/N11/N13/N14 是细节 |
| 命名与术语 | B | B+ | **A-** | "tile sprite overlay" 全文统一；visibleCells 三种叫法待 Glossary 收口（N23） |
| 测试设计严密度 | B+ | A- | **A** | RecordingSpriteBatch 路径 + 命名约定 + acceptanceContractLint 重跑触发全部冻结 |
| 与下游 PR 稳定性 | A- | A | **A** | sealed `ModalFrameKind` + `dark-uiux-pr01-1-*` golden namespace + Token Contract 全部冻结 |
| Roguelike 玩法体验 | A- | A | **A** | deadzone / overlay layer / map-first ≥ 50% / inspect snap 全到位 |
| 大决策（ASCII 删除）透明度 | C | A- | **A** | Major Decisions + Deletion Checklist 路径明示 + 三条 rg scan + non-client 允许列表全部清晰 |

**整体定级**：`Approve, ready to merge after addressing 2 P0 + 8 P1 cleanups`。

---

## 6. 行动清单

### 必改（P0 = 2，合同实施前）

1. **N1**：§3.5 cap 公式改为 `max(0, visibleCells - 2)`，并说明 cap = 0 时 deadzone 退化为 STRICT_FOLLOW 不视为 bug。
2. **N8**：§6 Token Contract `modalMaxWidth` 默认值改为 `min(640f, shellContentWidth - 2 * modalPadding)`，并在 Constraint 列写出 1024×768 下稳定值（约 564f）。

### 应补（P1 = 8，golden / 实施前）

3. **N2**：§3.8 显式 `>` vs `>=`。
4. **N4**：§3.5 "几何中心" 改为 `floor(visibleCells / 2)` 或同等显式公式。
5. **N5**：§5.7 flip 各方向公式改为 `tooltipEdge = anchorEdge + sign(direction) * tooltipFlipMargin`，或加 LibGDX Y-up 坐标系前缀说明。
6. **N6**：§5.4 "keyboard selection" 与 "focused item/detail source" 合并为单一 "focused interactive entity"，Glossary 加定义。
7. **N7**：§5.6 anchor source 列表删 "等"，明确为穷尽合同。
8. **N9**：§6 Token Contract 增加 `modalMaxHeight` worked example（1024×768 + 1280×800）。
9. **N15**：§4.6 删除 `internal` 选项，只保留 `private`。
10. **N19**：§0 Failure Rule 末段加 `client` / `game` 边界限定。

### 表述清理（P2 = 14，可与实施 commit 合并）

11. **N3**、**N10**、**N11**、**N13**、**N14**、**N16**、**N17**、**N20**、**N21**、**N23**、**N25**、**N27**、**N28**、**N32**、**N34**、**N37**、**N38**、**N40**、**N41**

### 强项保留（不要在清理 commit 中误改）

- §0 Glossary 三条术语澄清（stage viewport / tile sprite overlay / modal explicit tooltip）
- §0 Major Decisions 4 条强声明（特别是 Tile-only 渲染路径与 PR-01 finding 不回退）
- §0 Acceptance Matrix 七列结构 + Failure Rule 三段
- §3.2–3.4 identity / persistence / snap 一致性合同
- §3.5 deadzone 公式 + cap 优先级（仅修 N1 边界）
- §3.8 player tile 跨帧跳变 snap
- §3.12 LibGDX Y-up + Int 像素坐标
- §4.6 aggregate frame 局部化禁止
- §4.11 SpriteBatch begin/end 单点管理
- §4.13 ShapeRenderer / 第二 batch / framebuffer 禁止
- §4.14 allocation 预算降级路径
- §4.16 frame ownership self-audit
- §5.1 完整 layer 顺序（含 modal-internal explicit tooltip）
- §5.3 backdrop 单层 + dev-only debug overlay 禁止 backdrop
- §5.7 tooltip flip 顺序与对齐 anchor 边
- §5.9 modal max height 公式与 future scroll 不在范围
- §6 Token Contract 默认值表（仅修 N8 边界）
- §6 Deletion Checklist 路径明示
- §8 四个 subsection 分组 + nested modal 单层 backdrop（§8.20、§8.21）
- §9 RecordingSpriteBatch 路径 + 命名约定 + acceptanceContractLint 重跑触发
- §10 必填 evidence 标签列表（仅按 N16 加映射表）

---

## 7. 终评

P0(2) + P1(8) 一次清理 commit + manual record 同步，可正式进入实现。P2(14) 可并入实现 commit 或在 manual record 留痕处理；不强制独立 commit。

合同已经从 Round 3 "Approve, with cleanup before merge" 进展到 "Approve, ready to merge after addressing 2 P0 + 8 P1"。Round 1 提的三个长期合同（player-centered viewport / TileRenderer 拆分 / overlay layer）冻结目标已经达到 Roguelike 工程合同的可执行强度——下一个里程碑是 PR-01-1 实施 commit + 第一份 manual record。到那一步，本评审可以转为 **post-implementation 验证**。
