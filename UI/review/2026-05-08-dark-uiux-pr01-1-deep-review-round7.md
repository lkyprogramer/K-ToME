# Dark UI/UX PR-01-1 Deep Review Report Round 7

日期：2026-05-08

审查对象：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查口径：基于 round6 反馈后的当前文档版本，重新对照 `UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/README.md`、`UI/pr/screen-coverage-matrix.md`、PR-03/04/05/06/07 后续文档、当前 client 实现与测试。审查标准按“即使是细小问题也标出”执行。

本轮结论：**Round6 的两个 P1 已基本吸收，无新增 P0/P1。仍建议修 5 个 P2 和 3 个 P3 后再进入实现。** 这些问题不是大方向错误，而是会影响 PR-01-1 作为长期 viewport / overlay / renderer 合同的可执行性、测试可追踪性和后续 PR 的实现一致性。

已运行验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：`BUILD SUCCESSFUL`。

已运行静态检查：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii\.' \
  client/src assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
```

结果：当前扫描路径内 0 命中。

## Findings

### P0

未发现。

### P1

未发现。上一轮 P1 中的 typed anchor family、modal/passive tooltip fixture 化、no-hover 白盒路径已经进入当前文档。

### P2

#### 1. Acceptance Matrix 没有把 `TileOverlayLayerTest` 挂到 overlay 合同的 fastCheck

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:41` 的 `UI01-1-M04` fastCheck 只列出 `TileRendererCanvasTest`, `InputHandlerTest`, `ModalStackTest`。
- 同一文档 `:162`、`:269-270`、`:286`、`:308` 已明确要求 `TileOverlayLayerTest` 覆盖 typed anchor family、modal/passive tooltip 冲突、corner flip 和 bottom log non-overlap。

影响：

文档正文已经把 overlay owner test 收紧，但 Acceptance Matrix 仍没有把它列为 `UI01-1-M04` 的 fastCheck。`acceptanceContractLint` 只检查矩阵结构，不会理解这一语义缺失。后续 PR close 时，reviewer 可能只按矩阵验收，漏掉真正锁 overlay layer 的 owner test。

修复方向：

把 `UI01-1-M04` fastCheck 改为：

```text
TileOverlayLayerTest, TileRendererCanvasTest, InputHandlerTest, ModalStackTest
```

如果希望 `TileRendererCanvasTest` 继续作为像素/层级 canvas 证据，应写成 canvas supplement，而不是替代 overlay focused owner。

#### 2. Modal safe bounds 使用了 `headerBottom` / `footerTop` / `shellContentWidth`，但这些几何真源未被当前 frame contract 明确输出

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:138` 要求 modal placement 基于 `headerBottom..footerTop`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:191-203` 使用 `shellContentWidth`、`footerTop`、`headerBottom` 计算 modal max width/height。
- 当前 `client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt:19-23` 只有 `leftRailBounds`、`mapBounds`、`rightPanelBounds`、`bottomHudBounds`。
- 当前 `client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt:39` 有 `panelGap`，但 `:200` 的 `1024x768` modal width 示例没有扣除左右 panel gap。

影响：

这是一个小但关键的 geometry authority gap。实现者可能各自在 renderer 里临时推导 `headerBottom`、`footerTop` 或 `shellContentWidth`，导致 modal 与 footer hints / bottom HUD 的 non-overlap 证据无法稳定复现。最小窗口下，`1024 - 184 - 240 - 2 * 18 = 564` 也没有说明是否应扣除 `2 * panelGap`，会让 `GameShellLayoutTest` 和 manual evidence 对 modal max width 产生不同解释。

修复方向：

在 §3 / §4 / §5 中新增一个 typed safe area，任选一种：

- `GameShellLayout.modalSafeBounds`
- `OverlayRenderFrame.modalSafeBounds`
- `ShellContentBounds(headerBottom, footerTop, leftContentEdge, rightContentEdge)`

同时把 worked examples 改成从该 typed bounds 推导，明确是否扣除 `panelGap`。不要让 modal renderer 自己从 window size、rail width 和 magic gap 二次计算。

#### 3. Deadzone 区间没有定义 inclusive / exclusive 语义，容易出现 off-by-one 和抖动

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:87` 定义几何中心索引、`deadzoneCells` 和 `floor(deadzoneCells / 2)`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:88` 要求离开死区时最小滚动。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:245-246` 要求 deadzone 内移动不变、离开后最小滚动。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:334` 手工步骤用 `N1 <= deadzoneCells - 1` 判断 still frame。

影响：

当前文字没有说明 deadzone 是闭区间、半开区间，还是用 tile center 判断。以 `visibleCells=10`、`deadzoneCells=4` 为例，如果中心索引是 `floor(10/2)=5`，再向两侧取 `floor(4/2)=2`，实现很容易写成 `[3,7]` inclusive，实际包含 5 格而不是 4 格。这个 off-by-one 会直接改变“玩家移动几步仍不滚动”的体验和 §10.11 的截图判定。

修复方向：

把 deadzone 写成明确半开区间，例如：

```text
deadzoneStart = centerIndex - floor(deadzoneCells / 2)
deadzoneEndExclusive = deadzoneStart + deadzoneCells
focusLocalIndex in deadzoneStart until deadzoneEndExclusive
```

再补充 even/odd visibleCells、even/odd deadzoneCells 的 focused tests。`N1` 手工步骤应引用测试输出的 `deadzoneStart/endExclusive`，不要只写 `deadzoneCells - 1`。

#### 4. 单帧跳变 snap 阈值没有说明按轴还是按距离度量

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:90` 写“玩家 tile 在单帧内跳变 `> deadzoneCells` 时 snap；`== deadzoneCells` 时仍走最小滚动算法”。
- 同一节 token 是水平、垂直两套：`deadzoneHorizontalMinCells` / `deadzoneVerticalMinCells`。

影响：

`deadzoneCells` 在水平和垂直轴不同。Teleport、respawn、diagonal relocation 发生时，如果实现用 Manhattan distance、Chebyshev distance、horizontal-only 或 per-axis threshold，都会得到不同 snap 结果。这个分歧平时不明显，但会影响 respawn/teleport 后的玩家定位和 replay/golden 稳定性。

修复方向：

明确为 per-axis 判断：

```text
if abs(dx) > horizontalDeadzoneCells || abs(dy) > verticalDeadzoneCells then snap
```

并补充 `== threshold`、`x exceeds only`、`y exceeds only`、`diagonal both below` 四个测试。若设计意图是 Chebyshev / Manhattan，也必须直接写出公式。

#### 5. ASCII 删除扫描命令仍可能漏掉命令错误和 `.ascii` 结尾 key

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:213-214` 删除清单要求清除 `*.ascii*` client tileset key 和 visual key。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:285` 当前命令是 `if rg ...; then exit 1; fi`。
- 同一命令的正则只包含 `\.ascii\.`，不能覆盖以 `.ascii` 结尾或 `.ascii_` 之类的 key。

影响：

`if rg ...; then exit 1; fi` 会把 `rg` 的 no match 退出码 `1` 和命令错误退出码 `2` 都当成“扫描通过”。如果某个路径不存在、`rg` 出错或参数错误，验证会静默继续。另外，删除清单写的是 `*.ascii*`，但正则只抓 `.ascii.`，会漏掉 `foo.ascii` 这类后缀 key。

修复方向：

把命令改成区分三种退出码，并扩大 key regex：

```bash
set +e
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii([._-]|$)' \
  client/src assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
status=$?
set -e
if [ "$status" -eq 0 ]; then exit 1; fi
if [ "$status" -gt 1 ]; then exit "$status"; fi
```

如果不想在文档里放多行 shell，也至少要写明：`rg` error exit must fail the verification, no-match is the only accepted non-zero status。

### P3

#### 1. `shellUsableContentArea` 没有定义分母

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:79` 和 `:328` 都用 `mapBounds.width * mapBounds.height >= 0.5 * shellUsableContentArea`。

影响：

这是低级但会影响 golden 判定的数学口径问题。`shellUsableContentArea` 如果表示整个 `1280x800` shell、去掉外边距后的 content、去掉 bottom HUD 后的上半区，结论会不同。当前文档没有定义分母，reviewer 和实现者可能都能“合理地”算出不同 PASS/FAIL。

修复方向：

定义为一个可从 `GameShellLayout` 直接读出的矩形面积，例如：

```text
shellUsableContentArea = shellContentBounds.width * shellContentBounds.height
shellContentBounds excludes only outer padding, includes left rail/right panel/map, excludes bottom HUD
```

或改成更直接的比例：

```text
mapBounds.area / (mapBounds.area + leftRailBounds.area + rightPanelBounds.area) >= 0.5
```

#### 2. 文档仍残留 “三条 ASCII 删除扫描命令” 的旧表述

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:218` 写“§9 中三条 ASCII 删除扫描命令”。
- 当前 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:285` 只有一条合并后的 `rg` 扫描命令。

影响：

这是纯文档一致性问题，但会让 manual record 作者误以为还要找三条命令输出，或者把一条命令拆回旧版本。

修复方向：

改为“§9 中 ASCII 删除扫描命令”或“§9 中合并后的 ASCII 删除扫描命令”。

#### 3. 文档混用 `ModalStack.peek()` 与当前 API / 同文档的 `ModalStack.top()`

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:121` 和 `:265` 使用 `ModalStack.peek()`。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:139` 使用 `ModalStack.top()`。
- 当前 `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:54` 的 API 是 `top()`。

影响：

这是小命名偏差，但 PR-01-1 正在冻结 overlay / modal authority，最好不要让实现者为了贴文档新增 `peek()` alias，或在 reviewer 之间产生“top/peek 是否语义不同”的误会。

修复方向：

全篇统一为 `ModalStack.top()`，或明确 `peek()` 是概念名、实现 API 仍为 `top()`。推荐直接统一为当前 API。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| Round6 P1: typed anchor family 覆盖下游 PR-03/07 | 当前文档 §5.6 新增 `WORLD_TILE`、`PANEL_SLOT`、`PANEL_ROW_OR_CARD`、`QUEST_ROW`、`MODAL_ROW`，且 §6.4 要求先落 typed family | 一致 |
| Round6 P1: no-hover 手工白盒改为 fixture / keyboard path | §8.24-25、§10.6、§10.13 已要求 `TileOverlayLayerTest` / validation fixture，不依赖 mouse hover | 一致 |
| Round6 P2: ASCII scan 扩大覆盖 | §9 扫描路径覆盖 client source/resources、assets manifest、examples、tools fixtures、game tileset resources；当前实际扫描 0 命中 | 部分一致，命令错误处理和 `.ascii` 后缀 regex 仍需修 |
| Round6 P2: test-name lint 降级或落 gate | §9 明确当前不声明 `acceptanceContractLint` 已具备 test-name lint，manual record 列映射 | 一致 |
| Round6 P2: ModalFrameKind vs UiMode projection | §5.10 已明确不新增 UiMode，但允许投影到现有 `INVENTORY` / `TARGETING` / `TALENT_ASSIGN`，overlay authority 来自 `ModalStack.top()` / `OverlayRenderFrame` | 一致 |
| Round6 P2: FoundationViewportSupport 旧合同替换 | §6 Implementation Migration Locks 已明确旧 snapshot-size-driven 合同不得保留 | 一致，当前代码仍未实现，属后续实现任务 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Overlay layer / tooltip anchor | typed anchor family + modal/passive suppression + corner flip | 文档部分完成，代码未实现 | `UI/pr/...:124-134`, `:269-270` | Acceptance Matrix 未列 `TileOverlayLayerTest`，影响验收追踪 | Medium |
| Modal placement | modal 在 shell safe area 内，不遮挡 footer hints / bottom log | 文档部分完成，代码未实现 | `UI/pr/...:138`, `:191-203`; `GameShellLayout.kt:19-23` | `headerBottom/footerTop/shellContentWidth` 未成为 typed geometry authority | Medium |
| TileMapViewport deadzone | player-centered deadzone、最小滚动、edge clamp | 文档部分完成，代码未实现 | `UI/pr/...:87-90`, `:245-246`, `:334` | 区间闭开与 jump threshold 度量不够明确 | Medium |
| ASCII deletion | 删除 client ASCII renderer/model/manifest/evidence | 文档接近完成；当前扫描 0 命中 | `UI/pr/...:205-218`, `:285`; rg scan | 命令错误可能被误判通过，regex 仍有后缀漏扫风险 | Medium |
| Current implementation readiness | fixed shell viewport + renderer split + overlay model | 未实现/旧实现仍存在 | `FoundationGameScreen.kt:301-324`, `FoundationViewportSupportTest.kt:18-37` | 文档已锁定迁移方向，后续实现必须替换旧测试 | Low |

## 玩法与体验审查

### 核心循环

当前文档已经把地图第一视觉焦点、固定三栏、底部 HUD 和 deadzone viewport 收到同一合同里，方向正确。剩余关键体验风险在 deadzone 的 off-by-one：如果玩家在死区内仍轻微滚动，Roguelike 长廊移动会显得抖；如果死区多 1 格，靠近危险边缘时反馈又会偏慢。

### 战斗体验

combat feedback 继续作为 map-anchored layer，而 combat decision / targeting 走 modal / cursor overlay，方向正确。需要把 snap threshold 和 modal safe bounds 写清，否则 teleport、targeting 退出、combat decision 弹层这几类高压场景最容易出现视觉跳动或遮挡 footer hints。

### 成长与构筑驱动

PR-03/04 所需 item compare、replacement modal、active talent slot choice 已经能接到同一 `ModalFrameKind` / `TileOverlayAnchorKind` 模型。当前没有发现会阻断装备、铭文、职业树长期改造的问题。

### 奖励驱动与掉落体验

`PANEL_ROW_OR_CARD` 已覆盖 reward/frontstage、shop offer、world route option，解决了上一轮最大的长期兼容风险。建议 PR-07 最终 audit 反向引用这个 family，避免 reward/frontstage 再描述一套独立 tooltip anchor。

### 探索与新鲜感

out-of-map fog fill、small map centering、edge clamp 都有明确设计。`shellUsableContentArea` 的分母需要补齐，否则“地图是第一视觉焦点”的量化判断仍可能变成审美争议。

### 新手体验与信息反馈

bottom log reserved bounds、modal/passive tooltip suppression、tooltip max height cap 都符合信息层级目标。`TileOverlayLayerTest` 必须进入 Acceptance Matrix，否则新手提示、tooltip 与 modal 互斥这些行为容易在实现中只靠手工检查。

### 系统耦合与体验断层

当前文档基本避免了 second-authority：不改 core/game，不从 renderer 二次派生业务字段，不引入 mouse hover。剩余耦合风险主要是 modal safe bounds 的几何权威未建模，容易让 renderer 重新计算 layout。

## 当前阶段必须解决的问题

1. `UI01-1-M04` 补 `TileOverlayLayerTest`：这是当前 PR 的 overlay owner gate，不能推迟到 PR-03/07。
2. 定义 modal safe bounds：这是 renderer/overlay 基础合同，后续每个 modal 都依赖，不能让后续 PR 各自计算。
3. 明确 deadzone 半开区间和 jump per-axis 阈值：这是 viewport 算法核心合同，必须在 PR-01-1 落地前冻结。
4. 修正 ASCII scan shell semantics：这是删除合同的验证闭环，不能只靠 `acceptanceContractLint`。
5. 定义 `shellUsableContentArea`：这是 map-first 的量化验收口径，至少要在文档中给出唯一公式。

## Removal/Iteration Plan

当前不建议新增删除计划。Round6 指出的旧 `FoundationViewportSupport` snapshot-size 合同已经写入 Implementation Migration Locks；实际删除/替换应在 PR-01-1 代码实施时执行，并同步更新 `FoundationViewportSupportTest`。

## Additional Suggestions

1. 在 `TileOverlayAnchorKind` 表中增加一列 `Introduced / First Consumer`，例如 PR-01-1、PR-03、PR-07，方便后续判断某个具体 entity 是否只是 family 内扩展。
2. 在 §9 的 focused lane 后补一句：`TileOverlayLayerTest` 是 overlay placement owner，`TileRendererCanvasTest` 只证明最终 canvas non-overlap 和 draw order。
3. 在 manual record 模板中把 `Overlay Conflict Evidence` 拆成三行固定 checklist：`passive suppressed by modal`、`corner flip`、`bottom log reserved bounds`。

## Suggested Verification

已运行并通过：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

已运行静态扫描，当前 0 命中：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii\.' \
  client/src assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
```

建议文档修正后再运行：

```bash
./gradlew acceptanceContractLint
```

PR-01-1 实施完成后建议至少运行文档 §9 的完整 lane，并特别确认：

- `TileOverlayLayerTest` 存在且覆盖 modal/passive suppression、corner flip、bottom log reserved bounds。
- `TileMapViewportTest` 明确覆盖 deadzone half-open interval、per-axis jump threshold、small map centering、edge clamp。
- ASCII 删除扫描区分 `rg` no-match 和 command error。

## Summary

当前文档已经吸收上一轮结构性反馈，整体可以作为 PR-01-1 的长期改造底座。剩余问题集中在“验收矩阵是否完整追踪 owner test”和“几何/算法/扫描命令是否足够精确”。这些不是方向性错误，但建议在实现前修掉，因为它们一旦进入代码阶段，会变成 renderer-local 坐标推导、deadzone off-by-one、或删除扫描误通过这类高返工成本问题。
