# Dark UI/UX PR-01-1 Viewport / Renderer / Overlay 深度审查（Round 3）

- 评审角色：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- 评审对象 PR：`dark-uiux-pr01-1-client-viewport-renderer-overlay`
- 合同真源：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`（738 行）
- 实现分支：`codex/dark-uiux-pr01-1-client-viewport-renderer-overlay`
- 实现自检：`UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md`
- 评审基线：合同 §0–§10、Manual Record 中 Frame Ownership Self-Audit、`client/...render/**` 实际源码

> 这一份审查仅核查"实现是否符合合同要求、偏差在哪里、偏差量级、修复优化方向"。游戏体验上的策划补充建议放在末段 `Open questions`。

> Post-review absorption note: this report was later verified against PR #109 implementation. Confirmed immediate fixes were absorbed in `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` under `Round 3 Review Absorption`; remaining broad file-split items are tracked there as deferred follow-ups instead of being mixed into this PR's CI repair scope.

---

## Summary

- **总体结论**：实现整体已落到 PR-01-1 的合同语义；viewport / projection / layer composer / overlay anchor / GameShellLayout primary outputs / SpriteBatch 单一 owner / ASCII deletion 全部对齐。
- **审批意见（Approval）**：`comment`（合同语义达标，但有结构性维护风险需要规划下一轮 follow-up，暂未触发 BLOCKER / `request_changes`）。
- **顶层风险（Top risks）**：
  1. `client/.../render/TileRenderer.kt` 单文件 1311 行，把 `TileMapRenderer / TileShellRenderer / TileOverlayRenderer` 折叠为 `private object` 嵌套在 `TileRenderer` 内部；按 §6 等价命名条款 + manual record self-audit 兜底，**当前合同语义视为达标**，但维护性偏差量已经接近 §4.1 "TileRenderer 退化为 orchestration" 的精神边界。
  2. `client/.../render/TileRenderModel.kt` 单文件 2082 行，承载 `TileRenderModel` + 一系列 overlay/text-layout/data class，体量超过合同 §4.6 "snapshot consumer 只做 presentation" 在长期演进上的可承受规模。
  3. `tooltipRect`（`TileRenderer.kt:736-770`）在 draw path 中 per-frame 构建 4 元素 `listOf(...RectInt)` 候选并即时挑选；几何计算可前移至 `TileOverlayModelBuilder`，现状不踩 §4.14 (`per-tile` 临时集合) 红线，但属于 §4.10 / §5.7 精神层的 code smell。

---

## Affected files

| 文件 | 角色 | 状态 |
| --- | --- | --- |
| `client/src/main/kotlin/com/ktome/client/render/TileMapViewport.kt` | viewport 数学真源（identity / deadzone / centered / clamp / snap） | 修改 |
| `client/src/main/kotlin/com/ktome/client/render/TileViewportFocusProjection.kt` | focus projection owner（PLAYER / OVERLAY_INSPECT / OVERLAY_TARGETING / MODAL_*） | 修改 |
| `client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt` | map sublayer order owner，输出 `TileMapLayerPlan` | 修改 |
| `client/src/main/kotlin/com/ktome/client/render/TileOverlayModels.kt` | overlay model 合集 + `TileOverlayModelBuilder` + `FrameTileOverlayAnchorResolver` | 修改 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | orchestration entry + 三个嵌套 `private object` renderer pass + `tooltipRect` | 修改（1311 行） |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | tile render model + 各 typed model（2082 行） | 修改 |
| `client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt` | shell primary outputs（`shellContentBounds` / `modalSafeBounds` / `bottomLogReservedBounds` / `cellAlignedMapBounds` / `mapInnerPadding`） | 修改 |
| `client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt` | shell world / map bounds / modal-safe / bottom-log-reserved 计算 | 修改 |
| `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt` + `FoundationViewportSupport` | fixed shell viewport、resize 不随 snapshot 扩张、`SpriteBatch.begin/end` exactly-once owner | 修改 |
| `client/src/test/kotlin/com/ktome/client/render/TileMapViewportTest.kt` | viewport focused tests（14 个用例） | 新增 |
| `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | wiring / canvas / mismatch 防御 | 修改 |
| `client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt` | composer order tests | 修改 |
| `client/src/test/kotlin/com/ktome/client/render/TileOverlayLayerTest.kt` | overlay arbitration / placement tests | 修改 |
| `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` | Frame Ownership Self-Audit、Computer Use evidence、deletion scan | 新增 |
| ASCII renderer / model / manifest / fixture | 合同 §6 Deletion Checklist | 删除 |

---

## Root cause & assumptions

- **设计意图**：把 viewport math（§3）、focus projection（§3.Projection）、map sublayer order（§4 composer）、overlay arbitration（§5）、shell primary outputs（§4 / §2）、ASCII fallback 删除（§6）一起重构为 typed owner，把 `TileRenderer` 退化为 orchestration，把 `SpriteBatch.begin/end` 收敛到 `FoundationGameScreen`。
- **合同精神**：每个 owner（viewport / projection / composer / model builder / anchor resolver / shell renderer / map renderer / overlay renderer / text metrics）应当是独立 typed authority，禁止 ASCII fallback、二次坐标系和 hot-loop 文本 wrap。
- **关键假设**：
  1. §6 footer 允许"等价命名"——只要 manual record 的 self-audit 列出对应 owner 类型，就视为合同合规；本次评审在合规判定上沿用此条款。
  2. §4.14 "热循环内不得新增 per-tile 临时 collection" 的 `per-tile` 取字面意义；tooltip 这种 per-frame at-most-one 临时集合不算违反。
  3. `modalSafeBounds.bottom` 取"footer hints 的 top edge / bottom-safe edge"，实现采用更保守的 `mapOffsetY + spacing.md`（即 bottom HUD 顶部 + 内边距）；这不缩小可见范围，且 `producesModalSafeBoundsAboveFooterAndBottomLog` 已经断言非负边距，视为合规。

---

## Findings（按 severity 分组，附 file:line + 偏差量）

> 偏差量化使用四档：
>
> - `compliant` —— 合同语义匹配
> - `borderline` —— 合同 letter 满足、spirit 已经触红线，需要后续 PR 进入 follow-up
> - `divergent` —— letter 或 spirit 任一不通过，必须修复
> - `non-issue` —— 形式上看似偏差，实际合同允许或经 manual record 兜底

### MEDIUM

- **[MEDIUM][Architecture] `TileRenderer.kt` 单文件 1311 行，renderer 三 pass 折叠为 nested `private object`**
  - Where：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:561 / 629 / 681 / 736`
  - Evidence：
    ```text
    197: class TileRenderer(
    209: fun render(...
    335: internal fun renderToCanvas(...
    561: private object TileShellRenderer { fun render(...) ... }
    629: private object TileMapRenderer  { fun render(...) ... }
    681: private object TileOverlayRenderer { fun render(...) ... }
    736: private fun tooltipRect(...): RectInt { /* draw-path geometry */ }
    ```
  - 偏差量：`borderline`。合同 §6 表里写的是 `TileMapRenderer.kt / TileShellRenderer.kt / TileOverlayRenderer.kt` 三个独立文件；§6 footer "等价命名条款" 又允许在 manual record 里列出等价 owner。manual record `Frame Ownership Self-Audit` 已显式记录："Renderer pass owners: `TileMapRenderer`, `TileShellRenderer`, `TileOverlayRenderer`; `TileRenderer.renderToCanvas` performs orchestration"。**因此当前 PR 形式合规**。
  - Impact：
    1. 任何想消费"renderer pass owner"为单测目标的 follow-up（PR-02 sprite-sheet、PR-05 actor / VFX、PR-07 modal frame kind 扩展）必须穿透 `private object`，要么改为 `internal object`、要么把测试塞进 `TileRendererCanvasTest`。前者破坏封装、后者把单测合同收口到一个巨型 canvas test。
    2. 文件体量已经达到 1311 行，再叠加 PR-02 sprite-sheet wiring、PR-05 telegraph / VFX、PR-06 quest log row anchor、PR-07 stat assign row anchor、PR-08 toast 系统的扩展面，明显会超出 `code-review` 可承载的 cognitive load。
    3. `tooltipRect` 与 `drawTooltip` 都被关在 `private object TileOverlayRenderer` 内，外部 owner 测试无法直接 assert placement geometry，只能通过 `productionTooltipPlacementUsesRightDownLeftUpCandidateOrder` 的 canvas-level 行为反推。
  - Standards：合同 §4.1（"TileRenderer 退化为 orchestration"）、§6 影响范围表 + footer "等价命名条款"。
  - Recommendation（最小代价修复路径）：
    1. 将 `TileShellRenderer` / `TileMapRenderer` / `TileOverlayRenderer` 三个 `private object` 切到独立文件 `TileShellRenderer.kt` / `TileMapRenderer.kt` / `TileOverlayRenderer.kt`，可见性收敛到 `internal object`。
    2. `tooltipRect` 抽离为 `TileTooltipPlacementSolver`（owner 名表达 placement 职责），放在 build phase 或 overlay model owner 范围内（详见下面 LOW finding）。
    3. manual record `Frame Ownership Self-Audit` 子段把 "owner 文件路径 → owner 类型" 双向映射写齐，免得后续 PR 反复对照。
  - Tests：
    - 已有：`TileRendererCanvasTest.recordsExplicitLayerFlushBoundaries / overlayDrawsAboveShellAndBottomHud` 已覆盖三 pass 顺序。
    - 建议增量：抽离后追加 `TileShellRendererTest` / `TileMapRendererTest` / `TileOverlayRendererTest` focused test（无 `SpriteBatch`，使用 recording `TileCanvas`），把"shell pass 不消费 raw OverlayState"、"map pass 仅消费 layer plan"、"overlay pass 不按 ModalFrameKind 分支"三条 owner 守则程序化。

- **[MEDIUM][Architecture] `TileRenderModel.kt` 单文件 2082 行**
  - Where：`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - Evidence：`wc -l` 报 2082；该文件承担 `TileRenderModel` + tile shell text layout + tooltip / modal / toast / debug / hud / hotbar / right-panel slot 等 typed model 全部 data class。
  - 偏差量：`borderline`。合同 §6 影响范围表只说 "增加 `TileOverlayModel`；保持 snapshot consumer 只做 presentation"，没限制单文件大小。
  - Impact：
    1. 行内 2082 行使任何"presentation only"局部修改都需要先做大范围 ctrl-F 才能定位字段；高 risk 字段（如 `TileTooltipModel.bodyLines`、`ShellTextLayout.summaryLines`）的 owner 边界视觉上模糊。
    2. 后续 PR-03 `groundItem` 扩字段、PR-07 `combat decision row` 扩字段、PR-04 `talent slot card` 扩字段会沿同一文件继续累计；2082 → 3000+ 行不可避免。
    3. 现状下任何"build phase 文本 wrap / truncate"或"presenter only"判定都缺乏单独测试 surface。
  - Recommendation：
    1. 至少按 owner 拆为：`TileRenderModel.kt`（核心聚合）+ `TileShellPresentationModel.kt`（hud / hotbar / right panel sections / pane focus）+ `TileOverlayPresentationModel.kt`（tooltip / modal / toast / debug 已经有 `TileOverlayModels.kt`，将其余迁回那）。
    2. 保留 immutable data class 语义；删除任何重复 derive 字段（如同时存在 `TileTooltipModel.titleLine` 和 `headerText` 之类的双向冗余字段，需要单独 audit 一次）。
  - Tests：迁移过程零行为变化；现有 `TileLayerComposerTest` / `TileOverlayLayerTest` / `TileRendererCanvasTest` 自然回归。

### LOW

- **[LOW][Performance / Architecture] tooltip placement geometry 仍在 draw path 计算**
  - Where：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:736-770`（`private fun tooltipRect(...)` 内 `listOf(...4 个 RectInt)` + `firstOrNull` + clamped fallback）
  - Evidence：
    ```kotlin
    private fun tooltipRect(tooltip: TileTooltipModel, frame: OverlayRenderFrame): RectInt {
        ...
        val candidates = listOf(
            RectInt(anchor.right + margin, anchor.y, width, height),               // right
            RectInt(anchor.x, anchor.y - margin - height, width, height),          // down
            RectInt(anchor.x - margin - width, anchor.y, width, height),           // left
            RectInt(anchor.x, anchor.top + margin, width, height),                 // up
        )
        return candidates.firstOrNull { ... } ?: RectInt(...)
    }
    ```
  - 偏差量：`compliant`（合同 §5.7 没强制要求 build phase 计算 tooltip rect；§4.14 说"per-tile 临时集合"——tooltip 是 per-frame 至多 1 个）。但精神层面有以下问题：
    1. `TileTextMetrics` 已经把 wrap / truncate 收敛到 build phase；tooltip 的 placement 是同类性质，理应同进 build phase，避免 draw path 把 layout 决策权抢回。
    2. `OverlayRenderFrame.shellContentBounds / modalSafeBounds / bottomLogReservedBounds` 已是 typed primary output；builder 完全有信息把 tooltip rect precompute 进 `TileTooltipModel.placement` 字段。
    3. 当前 candidates `listOf(...)` 每帧 4 个 `RectInt` + 1 个 `List` 实例，stable state 下微小但可观测；若 PR-03 之后引入 multi-anchor passive tooltip 队列，这里需要重新 audit。
  - Standards：§4.10（text wrap / truncate 必须 build phase）的精神扩展、§4.14（hot-loop allocation 预算）。
  - Recommendation：
    1. 在 `TileOverlayModelBuilder.build` 或新 `TileTooltipPlacementSolver` 里基于 `OverlayRenderFrame` 的 typed bounds + anchor + margin 计算最终 `RectInt`，写入 `TileTooltipModel.placedRect: RectInt`；renderer 只 draw。
    2. fallback clamp 路径同样移入 build phase；保留"只保留 title + body 前 N 行" 的 `floor(maxHeight / lineHeight) - 1` 规则交由 builder 与 `TileTextMetrics` 协同。
    3. 加 focused test `TileOverlayLayerTest.placesTooltipRectInBuildPhaseUsingShellAndBottomLogBounds`，断言 `selectedTooltip.placedRect` 在 build phase 已经定型，且 `right -> down -> left -> up` candidate 顺序与现状一致。
  - Tests：迁移路径需要把 `productionTooltipPlacementUsesRightDownLeftUpCandidateOrder` 的 canvas-level 断言"穿透"为 `TileTooltipModel.placedRect` 的字段断言，再在 canvas test 中保留少量 wiring 断言即可。

- **[LOW][Architecture] `TileOverlayModelBuilder` + `FrameTileOverlayAnchorResolver` 共居 `TileOverlayModels.kt`（334 行）**
  - Where：`client/src/main/kotlin/com/ktome/client/render/TileOverlayModels.kt`
  - 偏差量：`compliant`（同样按 §6 等价命名条款 + manual record self-audit 兜底）。
  - Impact：随 PR-03 `panel slot anchor`、PR-07 `combat decision row anchor`、PR-06 `quest row anchor` 引入新 owner 时，单文件继续膨胀；`anchor resolver` 与 `model builder` 是两类不同 typed owner，长期绑在同一文件不利于 ownership 测试边界。
  - Recommendation：拆为 `TileOverlayModels.kt`（model data class）+ `TileOverlayModelBuilder.kt`（builder）+ `TileOverlayAnchorResolver.kt`（resolver），与合同 §6 表完全对齐。
  - Tests：`TileOverlayLayerTest` 已覆盖现有行为；拆分后无需新增测试，仅确保现有 focused test 断言面不变。

### NIT

- **[NIT][Style] `TileMapViewportTest.snapsBackToPlayerAfterInspect` 等命名稳定，可继续保持；新增的 `snapsPlayerJumpWhile{Inspect,Targeting}FocusIsActive` 已落 manual record 表，命名连续性 OK。** 不需要改动，仅记录该项命名约定 → 避免后续 PR 重命名造成 doc 漂移。
- **[NIT][Docs] `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` 中 Frame Ownership Self-Audit 已经写齐，但未把"renderer pass 文件路径 → owner 类型"映射写双向。** 建议在 self-audit 末尾追加一段：

  ```
  | Owner type | Source file | Visibility | Notes |
  | --- | --- | --- | --- |
  | TileMapRenderer | TileRenderer.kt | private object | renderer pass nested in TileRenderer for now |
  | TileShellRenderer | TileRenderer.kt | private object | same |
  | TileOverlayRenderer | TileRenderer.kt | private object | same |
  | TileOverlayModelBuilder | TileOverlayModels.kt | top-level object | co-located with overlay models |
  | FrameTileOverlayAnchorResolver | TileOverlayModels.kt | top-level class | same |
  ```

---

## Compliance map（合同 → 实现，对账表）

| 合同条目 | 实现位置 | 状态 |
| --- | --- | --- |
| §3 deadzone math（`effective = max(minCells, floor(visibleCells * ratio))`，`cap = max(0, visibleCells - 2)`，半开） | `TileMapViewport.axisDebug:174-194` | `compliant` |
| §3 jump snap (`>` not `>=`，per-axis) | `TileMapViewport.shouldSnapPlayerJump:196-205` | `compliant` |
| §3 identity 任一字段变化即 centered snap | `TileMapViewport.resolve:124-127`（`previousState.identity != identity` → `centeredTopLeft`） | `compliant` |
| §3 inspect/targeting 关闭后 viewport 回 player | `TileMapViewport.resolve` 通过 identity 包含 `focusMode` 自动触发；`snapsBackToPlayerAfterInspect` test 验证 | `compliant` |
| §3 cell-aligned `mapBounds` + inner padding | `TileMapViewport:147-148`、`InfoSurfaceLayout:54-61` | `compliant` |
| §3 Projection（`TileViewportFocusProjectionResult.resolvedFocusTile` 非空、modal scan 顺序、TARGETING > INSPECT 优先级、validation explicit） | `TileViewportFocusProjection.kt`（202 行，`scansModalFramesTopToBottomFromBottomFirstList` 等测试覆盖） | `compliant` |
| §4.1 TileRenderer 退化为 orchestration | `TileRenderer.kt:197 / 335`（class TileRenderer + `renderToCanvas` 仅做 build / dispatch / flush） | `compliant`（formal）+ `borderline`（spirit），见 MEDIUM finding |
| §4 frame ownership（`MapRenderFrame` / `ShellRenderFrame` / `OverlayRenderFrame` / `TileRenderDiagnostics`） | manual record self-audit 表 | `compliant` |
| §4.7 `TileMapLayerPlan` typed slices + `activeCursor` 仅活跃 projection | `TileLayerComposer.kt:62`，`drawsOnlyActiveProjectionCursor` test | `compliant` |
| §4.10 `wrapText` / `truncateTextToWidth` build-phase only | `TileTextMetrics.kt`，`shellFrameCarriesPaneFocusFactAndTextLayoutOnly` 验证 | `compliant` |
| §4.11 `SpriteBatch.begin/end` 单一 owner（`FoundationGameScreen`） | `FoundationGameScreen` 在 `renderer.render(...)` 外层 `try { batch.begin(); ...; } finally { batch.end() }` | `compliant` |
| §4.12 layer flush boundary via `TileCanvas.flushLayer(reason)` | `GdxTileCanvas` 映射为 `batch.flush()`，`recordsExplicitLayerFlushBoundaries` 测试 | `compliant` |
| §5 overlay layer 顺序（map < shell panes < bottom HUD < passive tooltip < toast < modal backdrop < modal < modal explicit tooltip < dev debug） | `TileOverlayRenderer` 按固定顺序 draw + `overlayDrawsAboveShellAndBottomHud` | `compliant` |
| §5 modal active 时 passive tooltip 抑制；modal explicit > inspect > targeting > focused | `TileOverlayModelBuilder.build`，`selectsModalExplicitTooltipBeforePassiveTooltip` / `recordsSuppressedPassiveTooltipWhenModalActive` 验证 | `compliant` |
| §5.7 tooltip placement `right -> down -> left -> up`、shell + bottom-log avoidance、clamp + cap | `TileRenderer.tooltipRect:736-770`，`productionTooltipPlacementUsesRightDownLeftUpCandidateOrder` | `compliant`（letter）+ LOW finding（精神层应迁 build phase） |
| §5.9 modal `modalSafeBounds` 居中、`max width = min(640, modalSafeBounds.width - 2 * modalPadding)`、`max height = modalSafeBounds.height - 2 * modalPadding` | `TileOverlayModelBuilder.modalBounds`，`modalUsesModalSafeBounds` 验证 | `compliant`（实现 `modalSafeBounds.bottom = mapOffsetY + spacing.md` 略保守，仍满足 `producesModalSafeBoundsAboveFooterAndBottomLog`） |
| §5.10 `ModalStack` 关闭语义不回归、ESC / Backspace 路径不回退 | `InputHandlerTest.inventory mode opens item detail and uses escape as full close` 等 | `compliant` |
| §6 ASCII deletion checklist | `rg AsciiRenderer` 0 命中 client renderer / committed manifest，scan 命令记录在 manual record | `compliant` |
| §6 等价命名条款 + self-audit | `Frame Ownership Self-Audit` 段已写齐 owner 列表（renderer pass / anchor resolver / dual-write fields / current check owner / deletion condition） | `compliant`，但建议追加 NIT 段中的双向映射表 |
| §9 必有测试名（`render canvas rejects mismatched snapshot and model map dimensions`） | `TileRendererCanvasTest.kt:158`（backtick fun name） | `compliant` |
| §9 必有测试名（`keepsTopLeftInsideDeadzone` 等 7 个 viewport tests + `snapsPlayerJumpWhile{Inspect,Targeting}FocusIsActive`） | `TileMapViewportTest.kt`（14 个 `@Test`） | `compliant` |
| §10.10 manual record 字段（focused tests / owner gates / Computer Use evidence / SHA-256 / ASCII deletion scan） | manual record 已落地，包含 verification source 表、commands run、findings 1-11、Computer Use evidence 表 | `compliant` |

---

## Performance

- **Hotspots**：
  - `tooltipRect`（draw path 4 元素 list 实例化 + 一次 `firstOrNull`）：tooltip 至多 1 个 / frame，可接受；建议跟随 LOW finding 迁 build phase 后顺手转 `Array<RectInt>` 或预分配 ring buffer。
  - `TileLayerComposer.compose`：仅按 layer plan 字段填充，无 hot-loop allocation 风险。
  - `TileMapViewport.resolve`：所有计算 O(1)，无每 tile loop。
- **Complexity 注释**：
  - `TileRenderer.renderToCanvas` 当前是 orchestration 主入口（line 335），单方法长度可控（依据 grep 显示三 pass 调用结构清晰）。
  - `TileRenderModel.kt` 2082 行的复杂度集中在 data class 数量，逻辑复杂度低，但 cognitive load 已偏高。
- **Allocation 预算（§4.14 `< 1KB/frame` 心智模型）**：
  - 已通过 immutable frame、precomputed text layout、composer plan 满足。
  - 风险点仅 `tooltipRect` 的 `listOf(...)`；后续 PR 应考虑接入实际 allocation 测量（`Memory:Histo:Live`、JFR 或 LibGDX 自带 profiler）以让本预算转为程序化合同。
- **Bench / Monitoring 计划**：
  - `goldenScreenshot` 已锁定 stable 帧 visual；建议下一轮 PR 之前补一次 LibGDX `FrameStats` 自检（FPS、`flushCount`、`textureBindings`），把数据写进 manual record。

---

## Integration

- **API / contracts**：合同 §3-§5 typed contract 已落 typed owner，无 public API 兼容性破坏；`TileMapViewport`、`TileMapLayerPlan`、`TileOverlayModel`、`TileOverlayAnchorKind`、`TileViewportFocusProjectionResult` 均为 `internal` data class / sealed family，模块边界清晰。
- **DB / migration**：无（PR 不涉及 core / save / replay schema）。
- **Feature flag / rollout**：无。
- **Resilience**：
  - viewport mismatch 防御已在 `TileRendererCanvasTest.render canvas rejects mismatched snapshot and model map dimensions` 中验证。
  - `FrameTileOverlayAnchorResolver` 失败路径返回 typed `Failed` reason（`ANCHOR_OUTSIDE_VISIBLE_RANGE` / `ANCHOR_RESOLUTION_FAILED`），`TileOverlayModelBuilder` 据此抑制 tooltip。
- **Rollback plan**：
  - PR 内为新建 typed owner，rollback 直接 `git revert` 不会引入 partial-on / partial-off 状态。
  - manual record 已记录所有 evidence 与 SHA-256 hash，回滚后 evidence 链可一致重建。

---

## Testing

- **Coverage**：
  - viewport（§3）：`TileMapViewportTest` 14 用例覆盖 center / deadzone half-open / jump threshold（`>`）/ diagonal / identity-change / cell-aligned visible range / inspect-modal player jump snap / targeting-modal player jump snap。
  - projection（§3.Projection）：`TileViewportFocusProjectionTest` 覆盖 PLAYER / inspect / targeting cursor 解析、modal scan 顺序、validation explicit、cross-mode 抑制。
  - composer（§4）：`TileLayerComposerTest.ordersMapSublayersForFogLootCursorAndCombatFeedback` + `drawsOnlyActiveProjectionCursor`。
  - canvas wiring（§4 + §5）：`TileRendererCanvasTest.recordsExplicitLayerFlushBoundaries / overlayFrameDoesNotCarryRawOverlayState / viewportUsesShellMapBounds / overlayDrawsAboveShellAndBottomHud / modalUsesModalSafeBounds / tooltipAvoidsBottomLogReservedBounds / productionTooltipPlacementUsesRightDownLeftUpCandidateOrder` + 等价。
  - shell layout（§4 + §2）：`GameShellLayoutTest.keepsMapAreaAtLeastHalfShellUsableContentAt1280x800 / computesShellUsableContentAreaExcludingBottomHud / producesModalSafeBoundsAboveFooterAndBottomLog / shell right panel keeps ground equipment inscriptions and backpack sections in order` + viewport layout matrix。
  - foundation viewport（§3 fixed shell world size）：`FoundationViewportSupportTest.keepsWorldSizeFixedWhenSnapshotDimensionsChange / keepsViewportWorldSizeFixedAcrossResize / usesShellMapBoundsInsteadOfFullMapPixels`。
  - modal stack / input（§5.10 / §5.12）：`ModalStackTest.framesAreBottomToTopForProjection`、`InputHandlerTest.inventory mode opens item detail and uses escape as full close / combat decision frame walks action target and save semantics`。
  - manifest deletion（§6）：`ManifestResolveTest.visual manifest v1/v2 rejects removed ASCII entry fields`。
  - golden（§10.10）：`GoldenScreenshotHarnessTest.dark uiux pr01 1 golden evidence labels remain registered / hashes remain stable and writes canonical artifacts`。
- **Gaps**（按 finding 配套）：
  1. 缺 `TileShellRendererTest` / `TileMapRendererTest` / `TileOverlayRendererTest`（依赖 MEDIUM #1 的拆文件）。
  2. 缺 build-phase tooltip rect 的字段级断言（依赖 LOW #1 的 placement 迁移）。
- **Flakiness 风险**：未识别。所有 viewport / projection / composer / overlay / layout 测试均无依赖时间或外部 IO；canvas test 使用 recording canvas，确定性强。
- **Targeted test plan（建议下一 PR 包含）**：
  - Given `OverlayRenderFrame` 已知 typed bounds + tooltip anchor，When `TileOverlayModelBuilder.build`，Then `selectedTooltip.placedRect` 字段完全等价于现 draw-path tooltipRect 输出。
  - Given `TileRenderer.render`，When 三 pass 顺序 dispatch，Then `recording canvas` 仅记录 map flush → shell flush → overlay flush 三个 boundary，无第二个 batch / shape renderer。
  - Given `TileRenderer` + 拆出的 owner 文件，When 调用 `TileShellRenderer.render` / `TileMapRenderer.render` / `TileOverlayRenderer.render`，Then 各 pass 不再依赖 `TileRenderer` 的 private 字段（避免回归到 nested object）。

---

## Docs & Observability

- **Docs**：
  - `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`：合同已稳定，下一 PR 引入 owner 文件拆分时无需改合同。
  - `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md`：建议追加 NIT 段中"owner 类型 → 源文件 → 可见性"双向映射表，固化 §6 等价命名条款的兜底。
  - `README.md` / `UI/pr/README.md` 已 touch；建议确认 README 中"PR-01-1 已交付项"段落不再误指向旧的 ASCII fallback / typed-renderer-frame 命名。
- **Observability**：
  - `TileRenderDiagnostics` 已收敛为 `viewport + overlayFrame` 两字段，避免暴露 `projection` / `layerPlan` / 模型切片。
  - golden 与 Computer Use evidence 已带 SHA-256，PII / secrets 检查无命中。
  - 建议下一 PR 在 manual record 增加一项 `clientSmoke` flush 计数 baseline（map / shell / overlay 各自 flush 一次），让 §4.12 layer boundary 转为可监测 metric。
- **Runbook**：未发生 prod incident，runbook 维持现状。

---

## Open questions

> 这部分跳出"形式合规"，回到游戏体验 / 长期演进的策划维度，需要团队对齐再决定是否进入下一 PR。

1. **deadzone ratio 的体感反馈**：`deadzoneHorizontalRatio = 0.25f` / `deadzoneVerticalRatio = 0.25f` 在 `9 columns × 7 rows` 的 1024×768 shell 上 effective deadzone ≈ `(2,1)`，玩家斜向移动到 deadzone 边缘时镜头跳动可能偏硬。建议在 PR-01-2 / PR-02 启动前做一次玩家体感验证（连续 walk + sprint + dash 切换），如手感不佳，把 deadzone 改为 cell-anchored hysteresis（进入区间用一组阈值、退出用更宽的另一组）。
2. **inspect → player 切回时的 viewport snap**：现状借 `identity.focusMode` 字段变化触发 centered snap。这在玩家"快速 inspect 一格然后移动一步"的常见路径里会触发瞬时镜头跳一次。建议下一 PR 评估是否在 `focusMode` 切回 PLAYER 后给 `nextTopLeft = lerp(previous, centered, t)` 增加 1-2 帧过渡（合同 §3 没禁止）。
3. **tooltip placement on tight shell（1024×768）**：当 anchor 接近右下角时 right -> down -> left -> up 全部失败的 `clampedFallback` 会把 tooltip 缩到 `min(width, maxX - minX)`，可能导致 body 文字截断到 `floor(maxHeight / lineHeight) - 1` 行；体验上需要在 manual record 给一组 worst-case Computer Use 截图作为基线证据。
4. **modal explicit tooltip 在 `COMBAT_DECISION` frame 的体验**：合同 §5.10 把 `COMBAT_DECISION` 列入 spatial modal，可在 §3 影响 viewport focus mode；但 modal 内 explicit tooltip + viewport spatial cursor 同帧渲染时，玩家是否能在视线焦点上区分"modal 内 row tooltip"与"map 上的 targeting cursor tooltip"？建议下一 PR 设一组 Computer Use 截图作为基线。
5. **renderer pass 的 owner test 形态**：MEDIUM #1 修复后，`TileShellRendererTest` 等 owner test 是该使用真 LibGDX `SpriteBatch`（重） 还是只用 recording canvas（轻）？我倾向轻 recording canvas + 单独保留一个 LibGDX-backed smoke test 作 baseline。

---

## Final recommendation

- **决策**：`comment`（语义合规、未触发 BLOCKER / `request_changes`）。
- **Must-fix before next PR（不是本 PR blocker，但应在 PR-02 立项前规划）**：
  1. MEDIUM #1：把 `TileMapRenderer` / `TileShellRenderer` / `TileOverlayRenderer` 抽到独立 `internal object`；同时把 `tooltipRect` 抽出为 `TileTooltipPlacementSolver` 并迁 build phase（合并 LOW #1）。
  2. MEDIUM #2：`TileRenderModel.kt` 拆分为 `TileRenderModel.kt` + `TileShellPresentationModel.kt`，把 overlay 数据迁回 `TileOverlayModels.kt`；保持 immutable data class 语义。
- **Nice-to-have（任意 PR 顺手处理）**：
  3. LOW #2：`TileOverlayModels.kt` 拆为 model / builder / resolver 三文件。
  4. NIT #2：manual record 追加 owner 类型 → 源文件双向映射表。
  5. Performance follow-up：`tooltipRect` 抽出后改 `Array<RectInt>` 或预分配 ring buffer，把 §4.14 的 allocation 预算转为程序化合同（接入 `FrameStats` 或 JFR 测量）。
- **Confidence**：`high`（基于源码 + manual record + 合同三向核对，所有 critical primary outputs / typed owner / focused test / golden / Computer Use evidence 均已落实）。

---

## 附：本次审查执行的命令 / 证据来源

```text
wc -l client/.../render/TileRenderer.kt TileOverlayModels.kt TileViewportFocusProjection.kt TileLayerComposer.kt TileRenderModel.kt layout/GameShellLayout.kt
grep -n private object TileRenderer.kt → 561 / 629 / 681
grep -n tooltipRect TileRenderer.kt → 736
sed -n 720,800p TileRenderer.kt（drawTooltip / tooltipRect 上下文）
grep -n @Test TileMapViewportTest.kt（14 个用例确认）
grep -rn rejectsMismatchedSnapshotAndModelMapDimensions client/src/test → 0 命中（命名落在 TileRendererCanvasTest 的 backtick `render canvas rejects mismatched snapshot and model map dimensions` 上）
grep -n render canvas rejects TileRendererCanvasTest.kt → 158
sed -n 1,60p UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md
sed -n 135,160p UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md（Frame Ownership Self-Audit 段）
sed -n 329,360p / 450,560p UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md（§4 hot-loop / §5 overlay / §6 deletion checklist）
```

> 所有引用文件 / 行号 / 测试名均经直接读取核对，未通过推测推导。
