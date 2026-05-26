# Dark UI/UX Asset-First Director-Grade Redesign Plan

> Status: PR-08 contract promoted / execution starts after PR-07 close
> Date: 2026-05-26
> Owner: `client` + `assets` + `tools`
> Scope: player-visible dark UI/UX quality reset toward `UI/UI-demo-new.png` level

## 0. 直接结论

当前继续在 `TileRenderer` / `DemoShellRenderer` 里叠低透明矩形、cutline、雾层和局部 shadow，已经接近收益上限。它能修局部读图问题，但很难达到参考图的第一眼质感。

两份 review 反馈核实后，本计划需要修正一个关键诊断：当前不是简单的“缺 bitmap 资源”。第一屏 ruins 地面 / 墙体 bitmap 已存在并接入 runtime，主要疑点是这些 bitmap 被程序 overlay 栈糊住。因此下一步不是先大规模生成资源，而是先做 **subtractive spike**：只保留现有 bitmap tile、确定性 visibility fog、最小光照和 gameplay marker，关掉主要材质模拟 overlay 后截图对比。

要把 K-ToME UI/UX 提升到 UI 总监 / 美术总监验收标准，下一阶段应切换为 **promotion-gated asset/compositor vertical slice**：

1. PR-07 先按当前 closeout 完结。
2. PR-08 已作为正式合同落入 `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md`、`UI/pr/README.md` 和 `UI/pr/screen-coverage-matrix.md`。
3. PR-08 开始后先做 subtractive spike，用现有资源证明根因是 overlay、资源本身还是 lighting。
4. 再做可被验收且可切片的目标首屏视觉稿或离线 target comp。
5. 只有 evidence 证明资源不足时，才生成 / 替换真实 bitmap 资源。
6. 最后把资源接回现有 manifest / renderer / golden 链路，并用 focused tests、golden、clientSmoke、packaged whitebox、resource gate 和人工 director review 收口。

本方案不是要求完全复刻参考图，而是要求同等级的整体品质：第一眼有统一材质、强焦点、真实手绘资源密度、清晰信息层级和稳定可玩读图。

## 1. 为什么之前很难

### 1.1 参考图品质来自整套美术生产

参考图的地图、墙、地面、火光、UI chrome、装备图标和面板纹理是一套统一绘制的 bitmap art。它的质感由大形、材质、边缘、明暗和光源一起成立，不是单个 renderer pass 的结果。

当前很多改动是在运行时用矩形和透明色模拟石块、阴影和磨损。这个方式适合补可读性，不适合承担完整美术资源职责。review 中指出的更高置信诊断是：现有 bitmap floor/wall 已经具备基础材质，但被 `drawVisibleRoomFoundationGlaze`、`drawCellMaterial`、`renderWarmMapOverlay` 等程序层覆盖后，玩家看到的主色和质感主要来自 overlay，而不是资源。

### 1.2 32px tile 细节不是靠程序线条堆出来的

当前地图 tile 在运行时大多以 `32x32` 读图。真正能在这个尺寸下成立的墙地质感，通常来自高质量源图下采样后的轮廓、明暗、变体和纹理密度，而不是事后叠很多 1-3px 矩形。

继续叠矩形会出现两个问题：

1. 局部白盒测试能通过，但整体像程序叠层。
2. 每次全局共享 renderer 改动都会造成大量 golden hash 漂移，反馈循环很慢。
3. 如果不先减 overlay，即使重做资源，也可能继续被同一套程序色层压成泥。

### 1.3 当前工程门禁正确但不适合审美试错

`goldenScreenshot`、`clientSmoke`、resource lint、`maintainabilityLint` 和 `verifyChanged` 是正确的收口门禁，但它们不应该承担当日常审美调参循环。

下一阶段必须把调试循环前移：

1. 先在 target comp / contact sheet / side-by-side review 里判断美术方向。
2. 方向成立后才进入 runtime integration。
3. runtime integration 阶段只做确定性接线和少量 compositing，不再靠 renderer 大量“画假资源”。

### 1.4 本次 review 后的修订原则

1. **先减后加**：先验证现有 bitmap + 最小光照能否明显接近参考图，再决定是否生成新资源。
2. **先 PR-08 合同后 mutation**：任何资源、manifest、golden 或 runtime 改动前，必须先把 `PR-08` 写入 PR 索引、覆盖矩阵、owner/evidence namespace 和 failure rule。
3. **视觉循环先行**：审美探索默认用 target comp、contact sheet、downscaled preview、截图和人工 side-by-side；Gradle/golden 只做收口。
4. **可切片才算 accepted**：target comp 必须证明每个资源族能落到合法 sheet cell、manifest key、consumer 和 test。
5. **PR-07 先收口，PR-08 再重构**：PR-07 用于完结当前阶段的 golden/whitebox closeout；PR-08 是后续彻底改造，不把 PR-08 的资源重置混进 PR-07，也不让两条线同时改同一批 evidence。

## 2. 视觉目标

### 2.1 Visual Thesis

K-ToME 的首屏应读成一个低饱和暗黑战术地牢：中心地图是主舞台，墙地来自真实手绘石材，UI 是黑铁、旧皮革和磨损石板，少量火光与冷青色只用于焦点和状态。

### 2.2 Director-Grade Success Definition

同一张 `ui-demo-new-parity-1672x941` 或等价首屏 evidence 必须满足：

1. 中央地图第一眼是主视觉，不被右栏或底栏抢焦点。
2. 地面和墙体不是规则格子，也不是程序雾层；必须读成真实手绘石板与厚墙。
3. 房间外暗区不是平涂黑，也不是规则隐藏格；必须有非矩形暗场压迫。
4. 玩家、敌人、loot、telegraph、交互物仍然比材质更可读。
5. 右栏装备 / 铭文 / 背包是一个统一的角色面板，不是 slot 漂在黑面上。
6. 底栏 hero / action / log 是一个控制台，不是三个断开的卡片。
7. 中文文本、快捷键、状态数值不被材质或特效压住。
8. 不引入旧风格残留、AI-slop 资源、baked text、随机噪声或跨时代 collage。

## 3. Authority And Boundaries

本计划必须遵守现有权威链：

1. 产品与 PR 顺序：`UI/PLAN.md`、`UI/pr/README.md`
2. 风格：`UI/ART_STYLE_BIBLE.md`
3. Open Design 辅助参考：`UI/review/open-design/ktome-dark-ui-design.md`
4. 映射真源：`UI/sprite-sheets/sheet-plan.yaml`
5. key 真源：`UI/sprite-sheets/key-registry.yaml`
6. canonical manifest：`assets-src/image/manifests/phase2-visual-manifest.json`
7. runtime manifest：`client/src/main/resources/manifests/visual-manifest.json`，只能由同步生成
8. 运行时行为：`client` presenter / renderer / input 代码和 golden

稳定边界：

1. 不改变 gameplay rule。
2. 不改变 save / replay / profile schema。
3. 不把规则状态复制进 `client`。
4. 不把图片文件名或截图当资源映射真相。
5. 不在图片里烘焙中文、英文、数字、快捷键或 manifest key。
6. 不在本阶段引入 atlas / region manifest schema；若确有必要，另开 manifest epoch PR。
7. 不用 `TileRenderer` 新增第二套地图规则、visibility 规则、loot 规则或 telegraph authority。

## 4. 新工作方式

### 4.1 Stop Doing

下一阶段默认停止以下做法：

1. 为了接近参考图继续向 `TileRenderer` 追加大量一-off 矩形。
2. 每个审美想法都先改 production renderer 再跑 full golden。
3. 用 golden hash 变化本身证明视觉质量提升。
4. 用局部 focused test 证明“某条线存在”，然后把它当作整体品质改善。
5. 用 right panel / map / bottom deck 的局部 polish 替代全屏第一眼验收。

### 4.2 Start Doing

下一阶段默认采用：

1. **PR-07 Close Then PR-08 Execution**：先完结 PR-07，再用已建立的 PR-08 合同进入资源 / runtime mutation。
2. **Subtractive Spike First**：先关掉主要 overlay 栈，用现有 bitmap + 最小光照验证根因。
3. **Target Comp First**：先产出目标首屏图，允许离线合成和快速视觉试错。
4. **Resource When Proven Necessary**：核心质感优先由真实 PNG / sheet / manifest 承担，但只有 spike 证明现有资源不足时才重生成。
5. **Runtime As Compositor**：renderer 只负责布局、状态、裁剪、少量光照和 deterministic compositing。
6. **Evidence As Closure**：golden 只在方向确认后固化，不作为美术探索工具。
7. **Director Review Gate**：每个视觉里程碑都要有人工 side-by-side 记录，明确接受 / 拒绝原因。

### 4.3 Two Feedback Loops

**Visual Feedback Loop** 用于审美探索，不默认跑 Gradle：

1. target comp / contact sheet / downscaled runtime-size preview。
2. cheap runtime screenshot 或 packaged screenshot。
3. side-by-side director review。
4. explicit `accepted` / `rejected` / `needs reroll`。

**Program Closure Loop** 只在视觉方向 accepted 后执行：

1. owner-routed manifest / registry / sprite sheet sync。
2. focused renderer tests。
3. client smoke / golden / packaged whitebox。
4. maintainability / verifyChanged。

如果视觉方向尚未 accepted，不更新 golden baseline，不把 heavy gate 结果当 UI/UX 质量证据。

## 5. Proposed File And Artifact Layout

计划新增或更新的长期 artifact：

| Path | Purpose |
| --- | --- |
| `UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md` | 本计划 |
| `UI/targets/README.md` | target comp 目录职责、命名、非权威声明和清理规则 |
| `UI/review/dark-uiux-director-grade-gap-audit.md` | 当前首屏 vs 参考图的逐项差距审查 |
| `UI/manual-records/dark-uiux-director-grade-target-comp.md` | target comp 人工验收记录 |
| `UI/manual-records/dark-uiux-director-grade-runtime-parity.md` | runtime parity 人工验收记录 |
| `client/build/reports/golden/<owner-routed-dark-uiux-label>/` | 新首屏与裁剪证据输出；prefix 固定为 `dark-uiux-pr08-director-*` |
| `build/reports/verification/dark-uiux/director-grade-asset-readiness.json` | 资源接线 readiness 报告 |

本方案已明确作为正式 PR-08 的计划来源。当前已完成 PR 索引、覆盖矩阵和 PR-08 execution contract；进入具体资源执行阶段时，再按实际 touch 范围同步更新：

1. `UI/pr/README.md`
2. `UI/pr/screen-coverage-matrix.md`
3. `UI/sprite-sheets/key-registry.yaml`
4. `UI/sprite-sheets/sheet-plan.yaml`
5. `tools` 中 dark UI coverage / acceptance contract lint 规则

本计划本身先不改 PR 索引，避免把方案草案误声明成已排期执行合同。

### 5.1 `director-grade-asset-readiness.json` Minimum Schema

该报告只有在 PR-08 resource migration / runtime integration 开始后才允许生成。它不是新的视觉真源，只是资源接线 readiness 汇总。

必填字段：

| Field | Meaning |
| --- | --- |
| `schemaVersion` | readiness report schema version |
| `ownerPr` | fixed formal owner, `PR-08` |
| `sourcePlan` | repo-relative path to this plan or promoted PR doc |
| `resourceFamily` | map floor, map wall, shell chrome, slot chrome, bottom deck, light overlay |
| `targetKey` | existing or proposed manifest key |
| `sheetId` | sheet-plan owner sheet |
| `canonicalRawOutputPath` | repo-relative canonical raw output path |
| `runtimeRawOutputPath` | repo-relative runtime output path after sync |
| `contactSheetPath` | repo-relative QA/contact sheet path |
| `qaStatus` | `pending`, `accepted`, `rejected`, or `blocked` |
| `directorVerdict` | side-by-side verdict and manual record path |
| `requiredGates` | exact owner/resource/runtime gates required for this family |
| `blockingFindings` | non-empty list when `qaStatus` is not `accepted` |

## 6. Sprint Plan

## Completed Preflight: PR-08 Contract And Attempt Disposition

已完成 PR-08 合同前置项，不再作为待办任务保留：

1. `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md` 已建立为正式 PR-08 execution contract。
2. `UI/pr/README.md` 已加入 PR-08 顺序、owner/evidence 规则和 PR-08 evidence matrix。
3. `UI/pr/screen-coverage-matrix.md` 已加入 PR-08 post-close director reset 规则和必填 `dark-uiux-pr08-director-*` labels。
4. 当前 renderer-heavy / resource-heavy 工作区 diff 已在 PR-08 文档 §2 标记为 attempt evidence / 待审查输入；PR-08 默认从 PR-07 close 后的 clean branch 执行，除非 gap audit 明确逐项保留。

## Sprint 0.5: Subtractive Spike

目标：用最小成本验证“现有 bitmap 是否被 overlay 栈埋掉”，避免在未证实根因前投入多天资源生成。

### Task 0.5.1: Strip Decorative Overlay Stack For One Screenshot

- **Location**:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `UI/review/dark-uiux-director-grade-gap-audit.md`
- **Description**: 在 prototype / scratch 实验中临时禁用主要程序材质模拟层，只保留现有 bitmap terrain placement、确定性 visibility fog、最小 torch / player warm light / edge vignette / shadow veil、actor、loot、telegraph、selection。
- **Candidate Subtractions**:
  - disable `drawVisibleRoomFoundationGlaze`
  - disable `drawCellMaterial` full layer
  - disable most decorative `renderWarmMapOverlay` subpasses
  - disable `drawVisibleRoomGridDissolve`
- **Acceptance Criteria**:
  - 生成一张 `ui-demo-new-parity-1672x941` 等价截图。
  - 截图记录为 exploratory evidence，不更新 golden baseline。
  - manual record 明确 verdict：
    - `overlay-root-cause`: bitmap + minimal light 明显更接近参考图，资源重生成降级或取消。
    - `resource-gap`: bitmap 仍不可读，进入资源 reroll。
    - `lighting-gap`: tile 可用但光照 / 暗场需要重做。
- **Validation**:
  - cheap screenshot + manual side-by-side review.
  - `git diff --check -- UI/review/dark-uiux-director-grade-gap-audit.md` if report edited.

### Task 0.5.2: Quantify Overlay Baseline And Budget

- **Location**: `UI/review/dark-uiux-director-grade-gap-audit.md`
- **Description**: 将 DG-04 从“少画一些”变成可验收指标。
- **Acceptance Criteria**:
  - 记录 `overlayFunctionCount`。
  - 记录 `rectDrawCountByLayer`。
  - 记录 `warmOverlaySubpassCount`。
  - 记录 `materialRectCountPerVisibleCell`。
  - 记录 `gridDissolveEnabled`。
  - 初始 target budget：decorative warm overlay subpasses `<= 4`，material simulation rectangles per visible cell `<= 2`，`gridDissolveEnabled=false`，non-gameplay decorative rectangles per visible cell `<= 3`。
- **Validation**:
  - reviewer can compare baseline vs budget from the audit table.
  - no golden baseline update in Sprint 0.5.

## Sprint 0: Evidence Freeze And Gap Audit

目标：先证明“差在哪里”，不要继续盲目加 patch。

### Task 0.1: Freeze Current Evidence Set

- **Location**:
  - `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`
  - `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`
  - `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`
- **Description**: 记录当前首屏、地图裁剪、右栏裁剪作为 gap audit 输入。
- **Acceptance Criteria**:
  - 记录当前 evidence path。
  - 明确它们是 current state，不是目标图。
- **Validation**:
  - 文件存在。
  - manual record 使用 repo-relative path。

### Task 0.2: Write Director-Grade Gap Audit

- **Location**: `UI/review/dark-uiux-director-grade-gap-audit.md`
- **Description**: 对比 `UI/UI-demo-new.png` 与当前 runtime evidence，逐项列出地图、墙体、地面、暗区、UI chrome、右栏、底栏、文本层级差距。
- **Acceptance Criteria**:
  - 至少覆盖 `map-stage`、`tiles/walls`、`lighting`、`right panel`、`bottom deck`、`text/readability` 六类。
  - 每类给出 `blocking gap` / `acceptable` / `defer` 判定。
  - 不把“已有 focused test”当作视觉达标证据。
  - 包含 Sprint 0.5 overlay baseline / budget，并说明 root-cause verdict。
- **Validation**:
  - `git diff --check -- UI/review/dark-uiux-director-grade-gap-audit.md`

### Task 0.3: Define Visual Rubric

- **Location**: `UI/review/dark-uiux-director-grade-gap-audit.md`
- **Description**: 从 `UI/ART_STYLE_BIBLE.md` 派生 10 条 director review rubric，用于后续每轮 target comp 和 runtime parity 验收，避免另起一套审美权威。
- **Acceptance Criteria**:
  - 每条 rubric 都能通过截图或人工 side-by-side 判断。
  - 每条 rubric 标注对应 `UI/ART_STYLE_BIBLE.md` 章节或明确说明是 gameplay readability 补充约束。
  - rubric 不要求完全复刻参考图。
  - rubric 不允许牺牲 gameplay readability。
- **Validation**:
  - reviewer 能用 rubric 对任一截图给出 pass/fail。

## Sprint 1: Target Comp Before Runtime

目标：先把视觉方向调对，再进入资源和代码。

### Task 1.1: Produce First Target Comp

- **Location**:
  - `UI/targets/dark-uiux-director-grade-target-1672x941.png`
  - `UI/targets/dark-uiux-director-grade-target-32px-tile-truth.png`
  - `UI/manual-records/dark-uiux-director-grade-target-comp.md`
- **Description**: 用图像生成、手工合成或设计工具产出一张目标首屏。它可以不来自 runtime，但必须尊重最终 runtime 布局：左 rail、中心地图、右 panel、底 HUD。
- **Dependencies**: PR-07 close, PR-08 contract, Sprint 0.5, Sprint 0
- **Acceptance Criteria**:
  - 首屏保持 K-ToME 三栏 + 底栏结构。
  - 无 baked text / hotkey / fake UI 字符。
  - 中央地图、右栏、底栏材质来自同一风格。
  - 能在 1672x941 一眼读出地图主舞台。
  - 附带 `32px tile-truth` inset：同一地面 / 墙体素材按 runtime 尺寸重复铺设，证明目标不是不可切片的全屏 paintover。
  - target comp accepted 前不得触发 golden baseline update。
- **Validation**:
  - 人工 director review 记录 `accepted` 或 `rejected with reasons`。
  - 若 rejected，不进入 runtime integration。

### Task 1.2: Cut Target Comp Into Resource Families

- **Location**:
  - `UI/manual-records/dark-uiux-director-grade-target-comp.md`
  - `UI/targets/README.md`
- **Description**: 把 target comp 拆成资源族：map floor/wall、map-stage background、panel chrome、slot chrome、bottom deck chrome、light overlays。
- **Dependencies**: Task 1.1 accepted
- **Acceptance Criteria**:
  - 每个资源族映射到现有 owner：`PR-02-1` shell chrome、`PR-05` tiles/actors/props、`PR-03` item/slot，或声明需要新 owner。
  - `ui-demo-new` 首屏 ruins keys 当前归 `PR-02-2`；进入 PR-08 时必须作为 formal owner migration / supersession 记录，不归 generic `PR-05`，也不静默继续按 PR-02-2 reroll 执行。
  - 每个资源族表格必须包含：`ownerPr`、`sheetId`、`targetKey`、`category`、`displaySize`、`tiling/stretch strategy`、`consumer`、`consumerTest`、`fallbackKey`、`requiresNewSchema=no`、`packagedRisk`。
  - 不新增裸 runtime key。
  - 不把 target comp 当 manifest truth。
  - 任何需要 baked UI text、atlas/region schema、full-screen paintover 或新 runtime manifest schema 才能成立的视觉元素，必须 rejected 或转入单独 manifest epoch PR。
- **Validation**:
  - 与 `UI/pr/README.md` SheetId Ownership 对齐。
  - `git diff --check -- UI/manual-records/dark-uiux-director-grade-target-comp.md UI/targets/README.md`

### Task 1.3: Decide Integration Strategy

- **Location**: `UI/manual-records/dark-uiux-director-grade-target-comp.md`
- **Description**: 决定每个资源族是替换现有 key、增加 alias、新增 key，还是只保留为 future concept。
- **Dependencies**: Task 1.2
- **Acceptance Criteria**:
  - 每个新增 key 有 owner、consumer、consumerTest。
  - 每个替换资源有 rollback path。
  - 不改变 manifest schema。
  - 明确本轮是 `PR-08 formal owner migration / supersession`，还是 no resource change。
- **Validation**:
  - `darkKeyRegistryLint` 设计上可覆盖。

## Sprint 2: Map-Stage Compositor Reset And PR-08 Asset Migration

目标：先解决最大质感差距：中心地图、墙体、地面和暗区。但执行顺序必须是先验证 compositing，再按证据决定是否重做资源。

### Task 2.1: Runtime Map Compositor Reset From Existing Assets

- **Location**:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
- **Description**: 让现有 floor/wall bitmap 成为主要读图来源。renderer 只保留必要 light、fog、selection、telegraph、contact shadow，不再继续用大量矩形模拟石材本体。
- **Dependencies**: PR-08 contract, Sprint 0.5 verdict, Sprint 1 accepted if target comp is used
- **Acceptance Criteria**:
  - 中心地图主要质感来自 bitmap floor/wall。
  - 程序 overlay 不遮挡 actor / loot / telegraph。
  - DG-04 budget 达标：decorative warm overlay subpasses `<= 4`，material simulation rectangles per visible cell `<= 2`，`drawVisibleRoomGridDissolve` 不参与 accepted runtime path，non-gameplay decorative rectangles per visible cell `<= 3`。
  - 如果删除某个微型材质矩形不会降低 gameplay readability，应删除或降低其权重。
  - 不引入新的 visibility、map、loot、telegraph authority。
- **Validation**:
  - focused `TileRendererCanvasTest` cases for layer order and marker readability.
  - cheap runtime screenshot + manual director side-by-side.
  - only after visual acceptance: `:client:goldenScreenshot`.

### Task 2.2: Migrate Existing PR-02-2 First-Screen Cells Into PR-08 Scope

- **Location**:
  - `UI/sprite-sheets/sheet-plan.yaml`
  - `UI/sprite-sheets/prompts/dark-v1/`
  - `assets-src/image/raw/sheets/dark-v1/`
  - `assets-src/image/contact-sheets/dark-v1/`
  - PR-08 owner coverage report path
- **Description**: 当 Sprint 0.5 / Task 2.1 证明资源本身仍不足时，将现有 `PR-02-2` first-screen cells 以 formal migration / supersession 方式纳入 `PR-08`，再 reroll 或替换资源。
- **Affected Existing Keys**:
  - `tileset.ruins.ground_01`
  - `tileset.ruins.wall_01`
  - `actor.vanguard`
  - `prop.stairs.down`
- **Acceptance Criteria**:
  - `PR-08` 文档明确这些 key 从 `PR-02-2` evidence baseline 被 supersede 的原因和回滚路径。
  - sheetId / row / col / targetKey / outputPath 的变化必须通过 PR-08 owner contract 记录；没有变更也要记录为 PR-08 重新验收。
  - 刷新 raw sheet、processed PNG、contact sheet、hash、PR-08 owner coverage 和 PR-08 evidence。
  - wall/floor 在 contact sheet 上有清楚主体、统一光源和足够 value range。
  - ground / wall cell 填满格子，无跨格、无文字、无水印。
  - 32px 缩放仍能读出 stone mass、edge、damage。
  - floor 至少规划多变体路径；如果当前 schema 只能消费单格，记录为 future resource limitation，不能用 overlay 伪装变体。
- **Validation**:
  - `darkSpriteSheetLint`
  - `spriteSheetMapLint`
  - PR-08 owner coverage task
  - contact sheet manual record

### Task 2.3: PR-08 Owner Contract And Coverage Wiring

- **Location**:
  - `UI/pr/README.md`
  - `UI/pr/screen-coverage-matrix.md`
  - `UI/sprite-sheets/owner-contracts/`
  - `UI/sprite-sheets/key-registry.yaml`
  - `UI/sprite-sheets/sheet-plan.yaml`
  - `assets-src/image/manifests/`
- **Description**: 为 PR-08 增加 owner contract、coverage wiring、screen matrix 和 evidence namespace。PR-02-2 / PR-05 / PR-06 是被 supersede 或被消费的上游合同，不再作为替代路线。
- **Acceptance Criteria**:
  - 更新 owner contract、PR05 source rules、final-full inventory、coverage tasks、manual records 和 evidence labels。
  - new owner coverage report path 明确且 repo-relative。
  - acceptance lint / owner regex / script regression 已同步。
  - migration 不改变 gameplay / save / replay / manifest schema。
- **Validation**:
  - `acceptanceContractLint`
  - owner-scope coverage for `PR-08`
  - `resourcePipelineLint`
  - `git diff --check`

### Task 2.4: Canonical Manifest And Runtime Sync

- **Location**:
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `client/src/main/resources/dark-v1/tiles/`
  - `build/reports/verification/dark-uiux/director-grade-asset-readiness.json`
- **Description**: 仅对 accepted asset reroll 或 approved owner migration 做 canonical-first 更新，并运行 sync 生成 runtime manifest。
- **Dependencies**: Task 2.2 accepted and Task 2.3 accepted
- **Acceptance Criteria**:
  - runtime manifest 由 sync 产生，不手写。
  - `rawOutputPath` repo-relative。
  - no old-style residue for touched floor/wall keys。
  - readiness report 满足 §5.1 schema。
- **Validation**:
  - `manifestLint`
  - PR-08 owner coverage task
  - `resourcePipelineLint`

### Task 2.5: Map-Stage Dark Field And Lighting Layer Contract

- **Location**:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - optional shell chrome resource keys under `ui.shell.*`
- **Description**: 用真实 map-stage background / shadow / vignette 资源或 minimal deterministic light 替代大面积程序黑场。保留 deterministic visibility fog，但让视觉暗区更像美术构图。
- **Layer Order Contract**:
  - `base stage art`
  - `semantic tile/floor/wall`
  - `deterministic visibility fog`
  - `gameplay overlays/actors/loot/telegraph`
  - `focus/selection`
  - `UI chrome`
- **Acceptance Criteria**:
  - hidden-stage 不读成规则 grid。
  - visibility semantics 不变。
  - decorative darkness cannot hide player、enemy、loot、telegraph、selection。
  - player focal、torch、telegraph 优先级明确。
- **Validation**:
  - focused tests for layer order and visibility non-regression.
  - focused tests proving decorative darkness cannot hide required gameplay markers.
  - `:client:goldenScreenshot` only after visual acceptance.

## Sprint 3: Shell Chrome And Panel Material Reset

目标：让右栏、底栏、左 rail、map frame 从“黑色容器 + 程序线条”变成统一的 forged iron / worn stone UI。

### Task 3.0: Existing Resource And Compositing Probe

- **Location**:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`
  - `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
- **Description**: 对 right panel、slot frame、item icon、bottom deck 先做现有资源接线 / 减 overlay 验证，再决定是否生成新 chrome。
- **Dependencies**: PR-08 contract, Sprint 0.5, Sprint 1
- **Acceptance Criteria**:
  - 明确哪些 visual gaps 是 resource gap，哪些是 layout/compositing gap。
  - 不因为“slot 漂在黑面上”就默认生成新资源；先证明现有 PR-02/PR-03/PR-02-1 资源无法满足。
  - 若需要新 shell chrome，目标资源族已通过 Task 1.2 sliceability contract。
- **Validation**:
  - cheap screenshot or crop review.
  - focused renderer test only for actual runtime behavior changes.

### Task 3.1: Generate Shell Chrome Target Assets

- **Location**:
  - `UI/sprite-sheets/sheet-plan.yaml`
  - `assets-src/image/raw/sheets/dark-v1/`
  - `assets-src/image/contact-sheets/dark-v1/`
- **Description**: 仅当 Task 3.0 证明现有资源 / compositing 无法达到 target comp 时，为 map-stage frame、right panel body、slot well、bottom deck rail、nav rail ornament 生成或替换真实 chrome 资源。
- **Dependencies**: Sprint 1 target comp, Task 3.0 verdict
- **Acceptance Criteria**:
  - 资源无文字、无数字、无 baked hotkey。
  - frame / panel / slot 的材质来自同一风格。
  - 资源在 runtime 尺寸不糊、不抢文字。
  - 若需要 nine-slice / tiling behavior，必须说明能否用现有 single-PNG manifest 合同表达；不能表达则转入 manifest epoch PR。
- **Validation**:
  - `darkSpriteSheetLint`
  - `spriteSheetMapLint`
  - contact sheet QA

### Task 3.2: Right Panel Runtime Integration

- **Location**:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- **Description**: 替换 right panel section body、equipment rig、empty slot、inscription rows、backpack tray 的主要视觉来源。
- **Dependencies**: Task 3.1
- **Acceptance Criteria**:
  - right panel 读成一个角色面板。
  - slot empty / equipped / selected / disabled 状态仍清楚。
  - 中文文本不被纹理压低到不可读。
- **Validation**:
  - focused renderer tests
  - `ui-demo-new-right-panel-grid`
  - `dark-uiux-pr03-equipment-slots`

### Task 3.3: Bottom HUD Runtime Integration

- **Location**:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- **Description**: 用真实 bottom console chrome 承载 hero card、action deck、log deck；减少程序拼接的 fragmented card feeling。
- **Dependencies**: Task 3.1
- **Acceptance Criteria**:
  - hero/action/log 是一个控制台。
  - HP/stamina/action/log 信息层级清楚。
  - 快捷键提示仍在低层级，不抢主操作。
- **Validation**:
  - focused tests
  - `ui-demo-new-bottom-deck-no-command-hints`
  - `:client:clientSmoke`

## Sprint 4: Full Runtime Parity And Coverage

目标：把 target comp 的视觉质量落进真实运行时，而不是停在设计图。

### Task 4.1: New Director-Grade Golden Labels

- **Location**:
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - PR-08 owner-routed golden report directory
- **Description**: 增加一组 owner-routed evidence labels，不覆盖旧 PR-02-1 证据，直到新方向稳定。
- **Acceptance Criteria**:
  - label prefix fixed to `dark-uiux-pr08-director-*`.
  - required labels:
    - `dark-uiux-pr08-director-parity-1672x941`
    - `dark-uiux-pr08-director-map-stage-crop`
    - `dark-uiux-pr08-director-right-panel-crop`
    - `dark-uiux-pr08-director-bottom-deck-crop`
    - `dark-uiux-pr08-director-telegraph-combat-crop`
  - existing `ui-demo-new-*` labels remain PR-02-1 / PR-02-2 evidence and are treated as upstream baseline, not PR-08 close evidence.
  - No freeform `director-grade-*` labels without `dark-uiux-pr08-*` prefix.
- **Validation**:
  - `:client:goldenScreenshot`

### Task 4.2: Runtime Director Review Record

- **Location**: `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
- **Description**: 对 runtime golden 与 target comp / reference 做 side-by-side 审查。
- **Acceptance Criteria**:
  - 每个 rubric 有 pass / fail / deferred。
  - fail 项不得用 hash 更新掩盖。
  - 若中心地图仍像程序叠层，必须回到 Sprint 2，不继续小修。
- **Validation**:
  - manual record exists and uses repo-relative paths
  - `git diff --check`

### Task 4.3: Full Gate Closure

- **Location**: standard gate outputs
- **Description**: 方向成立后执行完整 gate ladder。
- **Acceptance Criteria**:
  - focused tests pass
  - resource gates pass
  - `:client:clientSmoke` pass
  - `:client:goldenScreenshot` pass
  - packaged app whitebox pass for Sprint 4 / package-facing runtime parity
  - `maintainabilityLint` pass if renderer/presentation structure changed
  - `verifyChanged` pass
- **Validation**:
  - exact command output recorded in final implementation report
  - if Computer Use cannot bind the libGDX window, record blocker and provide fallback packaged screenshot/manual evidence path; do not mark packaged whitebox as passed

## 7. Acceptance Matrix

This matrix is intentionally governance-shaped. Sprint 0/1 may fill only proposal fields; Sprint 2+ cannot start with missing owner, failure rule or canonical artifact.

| requirementId | Requirement | Source | Owner | Dependencies / Freshness | Fast Check | Owner Gate | Canonical Artifact | Failure Rule | Whitebox | Promotion State |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DG-00 | Gap audit proves why current runtime misses reference-level quality | review reports + `UI/ART_STYLE_BIBLE.md` | docs | Current evidence frozen before mutation | `git diff --check` | N/A | `UI/review/dark-uiux-director-grade-gap-audit.md` | If no root-cause verdict, Sprint 1/2 blocked | N/A | proposal |
| DG-01 | Target comp accepted before runtime mutation | `UI/ART_STYLE_BIBLE.md` + this plan | docs/assets | PR-08 contract exists; Sprint 0.5 verdict recorded | manual director review | N/A | `UI/manual-records/dark-uiux-director-grade-target-comp.md` + `UI/targets/*32px*` | If not sliceable or accepted, runtime integration blocked | N/A for Sprint 1 | proposal |
| DG-02 | Existing bitmap / overlay relation is proven before resource reroll | Claude review + Sprint 0.5 | client/docs | Existing bitmap path and overlay baseline fresh | cheap screenshot + side-by-side | N/A | `UI/review/dark-uiux-director-grade-gap-audit.md` | If spike inconclusive, no resource generation | N/A | proposal |
| DG-03 | Floor/wall quality comes from bitmap assets, not renderer micro-rectangles | Sprint 2 PR-08 route | assets/client | `ownerPr=PR-08`; migration / supersession recorded | `darkSpriteSheetLint`, focused renderer test | `spriteSheetMapLint`, `resourcePipelineLint`, PR-08 owner coverage | contact sheet + PR-08 coverage report + PR-08 golden crop | If DG-04 budget fails, resource accepted status is invalid | N/A | execution only after promotion |
| DG-04 | Renderer acts as compositor, not second art asset generator | Sprint 0.5 overlay budget | client | Baseline metrics recorded | `TileRendererCanvasTest` focused layer tests | `maintainabilityLint` when structure changes | runtime code diff + gap audit budget table | If decorative overlay exceeds budget or hides gameplay marker, Sprint 2 fails | N/A | execution only after promotion |
| DG-05 | Manifest remains canonical-first and repo-relative | `UI/pr/README.md` + resource governance | assets/tools | PR-08 owner coverage report path known | `manifestLint` | PR-08 owner coverage + `resourcePipelineLint` | `assets-src/image/manifests/phase2-visual-manifest.json`, runtime manifest from sync, `build/reports/verification/dark-uiux/director-grade-asset-readiness.json` | Any hand-written runtime manifest or absolute path blocks close | N/A | execution only after promotion |
| DG-06 | Right panel and bottom HUD read as one material system | target comp + probe | client/assets | Task 3.0 probe complete | focused renderer tests | `:client:goldenScreenshot` after acceptance | owner-routed right-panel and bottom-deck crops | If existing resources were not probed, new chrome generation blocked | N/A | execution only after promotion |
| DG-07 | Gameplay readability survives art upgrade | layer order contract | client | layer order tests exist before golden close | focused actor/loot/telegraph tests | `:client:clientSmoke` | combat / telegraph golden or crop evidence | Any decorative darkness hiding actor/loot/telegraph blocks close | required in Sprint 4 package-facing close | execution only after promotion |
| DG-08 | Final runtime first screen meets director rubric | director rubric derived from Art Style Bible | client/docs | target comp and runtime evidence fresh | manual side-by-side review | `:client:goldenScreenshot` | `UI/manual-records/dark-uiux-director-grade-runtime-parity.md` | Fail item cannot be hidden by baseline update | required in Sprint 4; fallback only if tool cannot bind window | execution only after promotion |
| DG-09 | No old-style residue or AI-slop resources in touched paths | resource governance | assets/tools | touched key list comes from PR-08 owner contract | contact sheet QA | PR-08 dark manifest coverage | exact PR-08 owner coverage artifact path | Any old-style residue / baked text / missing owner blocks close | N/A | execution only after promotion |
| DG-10 | Full routed closure remains green | `UI/pr/development-governance.md` | tools | all above artifacts accepted | N/A | `verifyChanged` | verifyChanged output and final implementation report | If full gate fails, PR cannot close | required if package-facing | execution only after promotion |

## 8. Gate Ladder

### 8.1 Visual Feedback Loop

审美探索阶段默认不跑 Gradle，不更新 golden baseline。推荐顺序：

1. Freeze current evidence and root-cause screenshot.
2. Run subtractive spike screenshot.
3. Review target comp, 32px tile-truth inset, contact sheet, downscaled preview.
4. Record side-by-side verdict in `UI/manual-records/` or `UI/review/`.
5. Only if accepted, enter Program Closure Loop.

### 8.2 Program Closure Loop

方向 accepted 后再执行 owner-routed gate。示例顺序：

```bash
./gradlew acceptanceContractLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew assetLint styleLint manifestLint resourcePipelineLint
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
./gradlew :client:clientSmoke
./gradlew :client:goldenScreenshot
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=<formal-scenario-id>
./gradlew maintainabilityLint
./gradlew verifyChanged
git diff --check
```

实际执行时遵守仓库 SDKMAN 规则：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

Gradle 必须串行执行。审美探索阶段不要反复跑重型 gate；只有 resource / runtime 接线进入可验收状态后再跑 full gates。

如果只编辑本计划或 review 文档，只运行：

```bash
git diff --check -- UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md
```

## 9. Stop Rules

以下情况必须停止当前路径，不能继续堆 patch：

1. PR-08 合同文档、索引或 screen matrix 被删除 / 漂移，后续任务仍准备修改资源 / runtime / golden。
2. Sprint 0.5 未能给出 root-cause verdict，却直接进入资源生成。
3. target comp 未通过 director review。
4. target comp 不能切成合法 sheet cell / manifest key / consumer / test。
5. contact sheet 有跨格、baked text、主体不清、风格漂移。
6. 32px 缩放后 floor/wall 不可读。
7. runtime golden 中地图仍主要像程序雾层而不是 bitmap stone。
8. DG-04 overlay budget 超标但没有明确豁免和 gameplay 证据。
9. 新资源需要新增 manifest schema 才能成立。
10. renderer 为了表现资源开始复制 visibility、map、loot、telegraph 规则。
11. PR-07 closeout 与 PR-08 redesign 同时改同一批 map/shell golden，导致 evidence 并行漂移。
12. Sprint 4 package-facing close 无 packaged whitebox 证据，却试图标记通过。
13. 同一重型 gate 失败超过 2 次但没有新增 focused fast check。

## 10. Rollback Plan

每个 sprint 必须可回滚：

1. Sprint 0.5 是 exploratory spike；可丢弃 prototype diff，只保留 repo-relative evidence 和结论。
2. Sprint 0/1 只新增 docs/manual/target image，可直接删除或标记 rejected。
3. Sprint 2 PR-08 资源迁移失败时，回滚 raw sheet、processed PNG、contact sheet、canonical manifest、runtime manifest、coverage artifact、golden hash；旧 `PR-02-2` baseline 不受影响。
4. Sprint 2 PR-08 owner wiring 失败时，整体回滚 owner contract / PR index / coverage task / labels，不允许半迁移。
5. Sprint 3 shell chrome 失败时，回滚 new chrome keys 和 renderer consumption，不影响 gameplay。
6. Sprint 4 golden parity 失败时，不更新旧 PR-02-1 baseline；保留 owner-routed director evidence 作为 rejected attempt。

禁止通过降低 golden 门禁、删 owner gate 或改 gameplay snapshot 来让视觉方案过关。

## 11. Recommended Next Action

下一步不是继续向 `TileRenderer` 加 overlay，也不是马上重做资源，而是按以下顺序收口：

1. 先完结 PR-07 当前 closeout。
2. 执行 Sprint 0.5：关掉主要 overlay 栈，用现有 bitmap + 最小光照截图，判断 root cause。
3. 执行 Sprint 0：写 gap audit，量化 overlay baseline / budget，rubric 从 `UI/ART_STYLE_BIBLE.md` 派生。
4. 执行 Sprint 1：产出 target comp 和 32px tile-truth inset，并证明可切片。
5. 只有 spike 和 target comp 都支持资源不足结论时，才进入 Sprint 2 的 PR-08 resource migration / reroll。

只有 target comp 达到参考图同等级第一眼质量，且 sliceability / owner route 成立，runtime integration 才值得继续。
