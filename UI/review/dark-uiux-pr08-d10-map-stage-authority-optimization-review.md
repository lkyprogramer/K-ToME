# Dark UI/UX PR-08 D10 Map-Stage Authority Optimization Review

**Review target**: `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md`
**Local date**: `2026-06-04`
**Verdict**: `request_changes`

## 0. Precheck Summary

本轮按 K-ToME non-trivial review 预检执行，已读取 `docs/INDEX.md`、`docs/rule/kotlin.md`、`docs/rule/ai-change-governance.md`、`UI/PLAN.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`、PR-08 总文档、D10 目标文档、D10 manual record、当前 acceptance lint 与相关 client renderer / golden 变更。

命中的 phase / packet 是 PR-08 D10。目标是把 PR-08 后续 UI/UX 修复路线从 `TileCanvas` / immediate renderer micro-polish 上移到 libGDX Scene2D `Stage` / `Skin` / `Table` retained UI authority，同时保持 `TileRenderer` 只作为 map-stage rendering authority。

关键风险不是 Scene2D 方向本身，而是当前执行合同没有形成可合并边界：D10-P0 声称是 docs-only authority freeze，当前 worktree 同时包含 map-stage renderer、manifest、resource、golden harness 的实现变更；D10 文档声称 `acceptanceContractLint` 覆盖 D10，但当前 lint 没有纳管该文档，也不会拒绝 `<...>` 测试名占位。

## 1. Findings

### P1-1. D10 文档声明 `acceptanceContractLint` 覆盖 D10 合同，但当前 lint 实际没有纳管 D10

**证据**:

1. D10-P0 任务表把 `D10-P0-T5` 定义为 acceptance lint owner，并要求验收文案是 `acceptanceContractLint covers the D10 contract`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:523-529`。
2. D10-P0 phase gate 要求运行 `./gradlew acceptanceContractLint --no-configuration-cache`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:531-538`。
3. 当前 `Phase4V4AcceptanceContractLintTest.assertPrDocContract` 只检查文档包含固定 heading、至少 N 行 Acceptance Matrix、row 中不含 `TBD`、row 中没有机器绝对路径：`tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt:227-247`。
4. `uiPrDocs` 静态列表只包含 `UI00` 到 `UI07`，最后一个条目是 `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md`，没有 `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md`：`tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt:373-430`。

**影响**:

`acceptanceContractLint` 当前不会读取 D10 文档的 `UI08-D10-*` 行，不会验证至少 14 行 acceptance matrix，不会检查 D10 phase gate、manual record、placeholder 命令、D10-P0/P1 顺序或 retained UI cutover stop-rule。开发者按文档执行会得到一个假闭环：命令跑绿不代表 D10 合同被保护。

**修复要求**:

1. 把 D10 加入 `uiPrDocs`，例如 `PrDoc(requirementPrefix = "UI08-D10", path = "UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md", minimumRows = 14)`。
2. 对 D10 增加专门 lint：禁止命令块出现 `<...>` 占位；检查 `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` 存在；检查 D10-P0/P1/P2... phase heading 和 stop-rule 存在。
3. 在 D10 文档里把 `D10-P0-T5` 的验收改成可验证子规则名，而不是泛称 `acceptanceContractLint covers the D10 contract`。

### P1-2. Gate Budget 和各 phase gate 仍含不可执行的测试名占位

**证据**:

1. 顶部 Retained UI implementation gate 使用 `GoldenScreenshotHarnessTest.<targetedPr08D10RetainedUiTest>`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:110-118`。
2. D10-P2 使用 `<d10StandaloneScreensTest>`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:617-625`。
3. D10-P3 使用 `<d10FocusModalTooltipTest>`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:661-669`。
4. D10-P4 使用 `<d10RetainedShellTest>`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:703-711`。
5. D10-P5 使用 `<d10InventoryEquipmentTest>`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:755-763`。
6. D10-P6 使用 `<d10TalentTreeTest>`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:808-816`。
7. D10-P7 使用 `<d10FrontstageOverlayTest>`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:854-862`。

**影响**:

这些命令复制后直接失败，且失败模式会被误判为测试不存在或环境问题。更严重的是，D10 的 test boundary 现在无法精确回答“哪个 phase 要求哪个 golden / whitebox / actor-tree artifact”。这与 D10 自身要求 phase gate 不能模糊相冲突。

**修复要求**:

1. 对已经存在的测试，写完整类名和方法名。
2. 对尚未实现的测试，不要放进可执行 command block；改成 `Must Add Tests` 清单，等实现 phase 创建测试后再移入 gate。
3. 增加 doc lint：任意 `UI/pr/dark-uiux-pr*.md` 的 bash command block 中出现 `<[^>]+>` 直接 fail。

### P1-3. 当前 worktree 把 D10-P0 docs-only authority freeze 和 map-stage runtime 实现混在同一变更边界

**证据**:

1. D10-P0 明确目标是 “freeze the migration boundary before touching Kotlin”：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:517-520`。
2. D10-P0 whitebox evidence 明确 “No runtime whitebox required for docs-only P0”，manual record 必须说明 runtime migration has not started：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:540-543`。
3. D10-P1 才开始创建 `KtomeUiStage`、`KtomeRootTable`、`KtomeSceneSkin`、`KtomeInputRouter`：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:551-564`。
4. 本轮检索 `class KtomeUiStage|class KtomeRootTable|class KtomeSceneSkin|class KtomeInputRouter|scenes\.scene2d` 在 `client/core/game/tools/build.gradle.kts/settings.gradle.kts/gradle.properties` 无匹配，当前没有 Scene2D kernel 落点。
5. 当前 status 同时显示 `RoomArtPlatePresentation.kt`、`TileRenderer.kt`、`GoldenScreenshotHarnessTest.kt` 和 acceptance lint 被修改，D10 PR 文档与 manual record 是新增未跟踪文件。
6. D10 shell 迁移规则要求先建 `GameShellTable`，再移动 chrome / non-map panels，只有 right/bottom panels retained 后才继续本 route 的 map-stage art/topology work：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:1241-1245`。
7. 当前 renderer diff 仍在 `TileRenderer.TileMapRenderer` 增加 `RoomCompositorStrategy.TOPOLOGY_RISK_HYBRID_PRESENTATION` 分支和 room art plate interaction grammar：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:842-933`；`RoomArtPlatePresentation` 继续增加 topology-risk hybrid 多层绘制：`client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt:273-292`。

**影响**:

如果这是 D10-P0 PR，runtime renderer / resource / golden 变更必须拆出去；否则 P0 的 docs-only gate 和 manual record 口径不成立。如果这是 D10 implementation PR，又没有 P1 的 Scene2D kernel，变更仍在沿旧 immediate renderer 路线继续 micro-polish，与 D10 的 retained authority cutover 顺序相反。

**修复要求**:

1. 拆分变更边界：D10-P0 只提交文档、索引、manual record 和 D10 lint 纳管；renderer/resource/golden 作为独立 PR-08 map-stage evidence packet。
2. 或者把当前 PR 改成 D10-P1 implementation packet，先落 `KtomeUiStage` / `KtomeRootTable` / `KtomeSceneSkin` / `KtomeInputRouter` 和 focused tests，再谈 shell / map-stage 后续。
3. 不要在 D10-P0 manual record 里写 docs-only，同时在同一提交里引入 runtime crop / renderer topology 变化。

### P2-1. Manual record 只列 planned validation，不能作为 D10-P0 closure evidence

**证据**:

1. Manual record 状态是 `docs-only-authority-promotion`：`UI/manual-records/dark-uiux-pr08-d10-retained-ui.md:1-6`。
2. Phase transition rule 要求每个 phase 在下一阶段前记录 actual commands and artifacts：`UI/manual-records/dark-uiux-pr08-d10-retained-ui.md:36-41`。
3. Evidence Status 明确当前只包含 planning evidence，不声称 runtime visual closure：`UI/manual-records/dark-uiux-pr08-d10-retained-ui.md:43-60`。
4. Validation 段标题是 `Planned docs validation`，只列命令，没有结果、时间、exit code 或 artifact：`UI/manual-records/dark-uiux-pr08-d10-retained-ui.md:62-72`。

**影响**:

这份 record 可以作为计划记录，但不能作为 D10-P0 已关闭证据。若实施者按它进入 D10-P1，phase transition rule 自己被违反。

**修复要求**:

1. D10-P0 合并前，把 `Planned docs validation` 改成实际执行记录，至少列出命令、结果、失败/跳过原因和 artifact。
2. 若未执行，明确写 `not run`，并把 D10-P0 状态保持为 `open` / `planning-only`，不要允许进入 P1。

### P2-2. 新增 golden helper 绕过 LWJGL3 cleanup，容易形成顺序依赖和 native resource 漂移

**证据**:

1. 新 helper `withSingleShotLwjgl3Context` 自定义 `Lwjgl3Application.loop()`，通过反射取 `windows` 并在 result 出现后 `lwjgl3Windows().clear()`：`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:3904-3945`。
2. 同一 helper 将 `cleanupWindows()` 和 `cleanup()` override 为空，注释理由是 macOS hidden-window cleanup can block / avoid native GLFW terminate hangs：`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:3947-3953`。
3. 反射访问 `Lwjgl3Application.windows` 的 helper 暴露在同一测试文件：`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:3970-3974`。

**影响**:

这不是普通 focused helper，而是在 golden suite 里绕过 native window teardown。风险是后续测试顺序、GLFW 全局状态、隐藏窗口、音频/graphics 后端状态变成隐式环境依赖。当前注释解释了为什么这么做，但没有给出隔离策略、单测 tag 边界、失败后 cleanup 行为或 suite 顺序约束。

**修复要求**:

1. 优先复用已有可 teardown 的 LWJGL3 golden harness；如果 cleanup 卡死，需要先把卡死原因记录为单独问题。
2. 若必须保留 single-shot helper，把它限制为专用 probe / tag，并在 manual record 写清楚它不是通用 golden infrastructure。
3. 至少补一条 harness-level 测试或文档约束：该 helper 不得和常规 golden batch 混跑，失败时不得污染后续 golden。

### P2-3. D10 文档已经禁止继续新增 non-map immediate UI，但当前 implementation surface 仍在扩大旧 renderer 责任

**证据**:

1. D10 stable boundary 规定 D10-P1 开始后不得再向 `TileCanvas`、`DemoShellRenderer`、standalone layout solver、talent/inventory layout solver 添加新的 player-visible UI，除非属于 map renderer：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:500-503`。
2. D10 in-run shell 规则又进一步要求先 retained shell，再从 `DemoShellRenderer` 移走 non-map panels，之后才继续本 route 的 map-stage art/topology work：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:1230-1245`。
3. 当前 `RoomArtPlatePresentation` 的 topology-risk hybrid path 新增了 11 个顺序绘制层：band mantle、material runs、seam dissolve、ambient depth、source cropped bands、aperture pressure、wall mass slabs、wall veils、local lights、boundary marks、wall components：`client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt:282-292`。
4. 其中大量表现参数继续以内联颜色、alpha、几何比例落在 renderer 内：`client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt:370-405`。

**影响**:

这类 map-stage rendering technically 可以留在 `TileRenderer`，但在 D10 当前 phase 顺序下不能和 retained authority promotion 混成一个 packet。否则 D10 会变成“文档上说停止 immediate micro-polish，代码上继续扩大 renderer 视觉语法”的双轨路线。

**修复要求**:

1. 当前 map-stage topology-risk hybrid 改动如果要保留，必须独立标记为 PR-08 map-stage packet，并配套它自己的 evidence / golden / resource gate。
2. D10 packet 只能接受 retained UI authority、Scene2D kernel、cutover inventory 和 route removal 相关变更。
3. 若 map-stage 改动继续前进，至少把 hard-coded palette / alpha / geometry 收敛到具名 presentation policy，避免后续 D10 又需要从 renderer 里挖出主题逻辑。

### P3-1. D10-S* 命名残留会误导 implementation packet 编号

**证据**:

1. Framework decision 表仍写 “D10-S1/S2 must use official libGDX API first”：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:295`。
2. 同一文档 §8.6 又声明旧 `D10-S*` label 只保留为 historical shorthand，implementation packets 应使用 `D10-P*` phase ids：`UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md:1356-1372`。

**影响**:

这是小问题，但会在后续任务拆分时制造歧义：到底 `S1/S2` 是旧方案、spike、还是 P1/P2 的别名。D10 是大改造，编号必须严格。

**修复要求**:

统一替换为 `D10-P1/P2` 或明确写 “historical D10-S1/S2, now D10-P1/P2”。不要在执行合同里保留两个并行编号体系。

## 2. Requirement Alignment

| Requirement | Current State | Review Result |
| --- | --- | --- |
| D10 作为 PR-08 retained UI authority | 文档方向明确，`UI/PLAN.md` 和 screen matrix 已出现 D10 路由 | 部分一致；lint 未纳管，不能算执行合同闭环 |
| D10-P0 docs-only authority freeze | D10 文档和 manual record 写了 docs-only / no runtime claim | 不一致；当前 worktree 同时混入 renderer / golden / acceptance lint runtime-facing 改动 |
| Scene2D kernel first | 文档要求 `KtomeUiStage` / `KtomeRootTable` / `KtomeSceneSkin` / `KtomeInputRouter` | 未实现；当前检索没有 Scene2D kernel 类 |
| 不引入 KTX / VisUI / gdx-skins runtime dependency | 文档方向正确；当前检索未看到新增 runtime dependency | 当前一致，但依赖变更 spike gate 仍需未来执行 |
| 测试验证边界清晰可执行 | phase gate 有结构，但 golden method 多处为 `<...>` 占位 | 不一致；必须先修命令与 lint |
| 旧 immediate UI 不长期兼容 | 文档写得明确 | 未验证；没有 route removal 或 hard cut implementation |

## 3. Removal / Iteration Plan

1. **先收口 D10-P0**：只保留 D10 文档、`UI/PLAN.md`、`UI/pr/README.md`、screen matrix、manual record 和 acceptance lint 纳管。不要带 renderer/resource/golden runtime 改动。
2. **再开 D10-P1**：新增 Scene2D kernel 四件套和 focused tests。P1 不迁移复杂 screen，不碰 KTX，不让 Skin JSON 成为资源真源。
3. **map-stage renderer 改动单独排队**：当前 topology-risk hybrid / new wall material / runtime crop evidence 若要继续，应作为 PR-08 map-stage packet，验证它自己的 forest/mine/shadow fixed scenarios 和 resourcePipelineLint，不挂在 D10-P0 上。
4. **旧 route removal 不提前声明完成**：在 P2-P8 每个 migrated surface 结束时再删或 quarantine 对应 immediate route，并在 manual record 写明实际 removed routes。

## 4. Suggested Verification

本轮实际执行并看到结果：

```bash
git diff --check
```

结果：PASS，无输出。

建议先补跑 D10-P0 文档 gate，且这些命令应在 manual record 里记录实际结果：

```bash
rg -n '<[^>]+>' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
rg -n 'D10-S[0-9]' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
rg -n 'T[O]DO|T[B]D|F[I]XME' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/manual-records/dark-uiux-pr08-d10-retained-ui.md
git diff --check -- UI/pr UI/goal UI/review UI/manual-records
```

补齐 D10 acceptance lint 后再跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

如果保留当前 map-stage / resource / golden 改动，不能只跑 D10 docs gate，至少建议单独跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --no-configuration-cache
./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache
```

如果保留 `withSingleShotLwjgl3Context`，需要一个明确的 owner gate 证明它不会污染常规 golden batch；在证明前不要把它视为通用 golden harness。

## 5. Bottom Line

D10 的大方向是对的：继续在 `TileCanvas` / `DemoShellRenderer` 上做 UI framework 会把 PR-08 带入长期不可维护的 renderer micro-polish。当前不能合并的原因也很具体：执行合同不可执行、lint 假覆盖、phase 边界混乱、manual record 只有计划没有实际验证。

先修 P1-1 / P1-2 / P1-3，再决定当前 renderer/golden 改动是拆出独立 map-stage packet，还是改造成真正的 D10-P1 retained UI kernel packet。
