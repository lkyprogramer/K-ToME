# Dark UI/UX PR-02-1 Demo Shell Foundation

**阶段**: `dark-uiux-pr02-1-demo-shell-foundation`
**优先级**: `P0`
**工作量**: `XL`
**前置条件**: PR-00、PR-01、PR-01-1、PR-02 完成。
**资源生成结论**: 本 PR 必须新增 Round 1B 最小 shell chrome / nav icon sheet：`r01b-ui-shell-chrome`，只用于 demo 级主界面结构；`UI-demo-new` 首屏视觉一致性复用并刷新既有 PR-02-2 `r02-ui-demo-ruins-tiles` / `r03-ui-demo-actor-props` cell，不生成 PR-03 item 或 PR-06 skill/status 资源，也不把这些 demo cell 升级为 PR-05 最终 terrain / actor / prop authority。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再按 Round 1B resource gate、shell layout、client renderer、golden/manual 和最终 `verifyChanged` 串行闭环；依赖 manifest freshness 的 client 测试必须放在 `syncPhase2Manifests` 之后。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

本 PR 的存在原因：PR-02 已把 UI chrome / HUD / standalone chrome 资源接入主路径，但当前界面仍是旧 text-first shell。`UI/UI-demo-new.png` 是新的唯一验收图，目标是 icon rail + dominant map stage + right equipment/inscription/backpack/operation panels + bottom hero/action/log deck。这个结构必须在 PR-03/05/06 继续填资源前先搭起来，不能等 PR-07 最终 audit 才发现主框架错位。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI02-1-M01` | §1 demo shell foundation goal | `docs` / `client` | `acceptanceContractLint` | `verifyChanged` | `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md` | `N/A` |
| `UI02-1-M02` | §3 shell layout contract | `client` | `DemoShellLayoutTest`, `GameShellLayoutTest`, `InfoSurfaceLayoutTest` | `:client:clientSmoke` | `client/src/main/kotlin/com/ktome/client/render/layout/`, `client/build/reports/tests/` | `required` |
| `UI02-1-M03` | §4 renderer / layer order contract | `client` | `DemoShellRendererTest`, `TileRendererCanvasTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/dark-uiux-pr02-1/` labels `ui-demo-new-parity-1672x941`, `ui-demo-new-parity-1280x800` | `required` |
| `UI02-1-M04` | §5 right panel grid scaffold | `client` | `TileRendererCanvasTest`, `ManifestResolveTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/dark-uiux-pr02-1/` label `ui-demo-new-right-panel-grid` | `required` |
| `UI02-1-M05` | §6 Round 1B shell chrome keys | `assets` / `tools` | `DarkSpriteSheetPipelineScriptTest`, `darkKeyRegistryLint`, `darkSpriteSheetLint`, `spriteSheetMapLint -Pktome.darkUiux.requireFullGrid=true -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl`, `darkManifestCoveragePr02_1OwnerScope` | `assetLint`, `styleLint`, `manifestLint`, `darkManifestCoveragePr02_1OwnerScope`, `verifyChanged` | `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml`, `assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl`, `build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-1-owner-scope.json` | `required` |
| `UI02-1-M06` | §7 standalone/menu shell alignment | `client` | `MainMenuScreenTextTest`, `StandaloneScreenLayoutTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/` labels `dark-uiux-pr02-1-demo-main-menu`, `dark-uiux-pr02-1-demo-validation-setup` | `required` |
| `UI02-1-M07` | §8 demo parity evidence | `docs` / `client` / `tools` / `game` | `Phase4V4WhiteboxScenarioCliTest`, `ValidationScenarioRegistryTest`, `ValidationCommandSourceTest`, manual demo-delta checklist | packaged app whitebox | `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`, `client/build/reports/golden/`, `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/` | `required` |
| `UI02-1-M08` | §9 validation / maintainability gates | `client` / `tools` | `VerifyChangedBuildContractTest`, `VerificationImpactAnalyzerTest`, `maintainabilityLint`, `git diff --check` | `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |

### Gate Budget

预计重型任务：`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。本 PR 必须运行 `assetLint`、`styleLint`、`manifestLint`、`darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint`、`darkManifestCoveragePr02_1OwnerScope`。

触发原因：

1. 本 PR 重排 in-game shell 主结构，是 player-visible UI 主路径变更。
2. 本 PR 新增 demo parity golden 和 packaged app 白盒证据。
3. 本 PR 新增 PR-02-1 owner-scope UI shell chrome keys。
4. 本 PR 会修改多个 `client` renderer / layout / screen 文件，必须跑 `maintainabilityLint`。

freshness 要求：

1. `r01b-ui-shell-chrome` 的 `raw sheet hash`、切分 PNG、`sprite map report`、contact sheet QA、canonical manifest、runtime manifest、owner-scope coverage、golden output 必须来自同一批 `sheet-plan.yaml` / `key-registry.yaml`。
2. `syncPhase2Manifests` 必须在 `ManifestResolveTest`、client focused evidence、`darkManifestCoveragePr02_1OwnerScope`、`:client:goldenScreenshot` 和最终 `verifyChanged` 之前完成。
3. `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` 必须列出 demo-delta checklist：哪些 demo 要素已达成、哪些剩余差异交给 PR-03/05/06/07；不得再把 `map backdrop/art quality deferred` 作为 PR-02-1 可接受差异。
4. PR close 前读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。若该文件不存在，PR 描述写明“本轮未产生 verifyChanged duration summary”，不得伪造耗时基线。
5. 若本轮 `verifyChanged` 耗时超过最近 dark UI/UX PR 基线 `1.5x`，先执行 doc-vs-implementation self-audit，确认没有把 golden / packaged app 当调试循环，再继续重跑。
6. 若同一 golden / packaged app 白盒连续失败超过 2 次，先补 `DemoShellLayoutTest` / `TileRendererCanvasTest` 中缺失的数值或 owner-bounds 断言，再重跑 screenshot；不得把 screenshot 当布局调试循环。

### Canonical Artifact

canonical artifact 固定为以下 repo-relative 路径族：

1. `UI/UI-demo-new.png`
2. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`
3. `UI/PLAN.md`
4. `UI/pr/README.md`
5. `UI/pr/screen-coverage-matrix.md`
6. `client/src/main/kotlin/com/ktome/client/render/layout/`
7. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
8. `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
9. `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
10. `client/src/test/kotlin/com/ktome/client/render/DemoShellLayoutTest.kt`
11. `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`
12. `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
13. `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
14. `client/build/reports/golden/`
15. `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`
16. `build/verification/verify-changed/full-task-duration-summary.{json,md}`

Round 1B shell chrome canonical artifact：

1. `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml`
2. `UI/sprite-sheets/sheet-plan.yaml`
3. `UI/sprite-sheets/key-registry.yaml`
4. `UI/sprite-sheets/prompts/dark-v1/*r01b-ui-shell-chrome*.prompt.txt`
5. `assets-src/image/raw/sheets/dark-v1/r01b-ui-shell-chrome.png`
6. `assets-src/image/contact-sheets/dark-v1/r01b-ui-shell-chrome-contact.png`
7. `assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl`
8. `assets-src/image/manifests/phase2-visual-manifest.json`
9. `client/src/main/resources/manifests/visual-manifest.json`
10. `client/src/main/resources/dark-v1/ui/`
11. `build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-1-owner-scope.json`
12. `scripts/dark_sprite_sheet_contract.py`
13. `scripts/verify_dark_key_registry.py`
14. `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`
15. `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt`
16. `tools/build.gradle.kts`
17. `tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt`
18. `tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt`
19. `tools/src/test/kotlin/com/ktome/tools/verification/VerifyChangedBuildContractTest.kt`
20. `build.gradle.kts`

### Failure Rule

本 PR 不能以“没有重叠”作为 pass。若截图仍呈现旧 text-first shell，即使 focused tests 和 golden hash 通过，也必须判定失败并回到 §3 / §4 修布局结构。

Demo parity 的人工判定主体固定为 PR owner + 至少 1 位非 owner reviewer。判定记录写入 `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` 的 `manualReviewers[]`、`reviewedAt`、`demoParityVerdict`、`blockingFindings`、`screenshotLabelCoverage` 字段；任一方判定 `fail`，本 PR 不能 close。

禁止事项：

1. 不得把 PR-03 item icon、PR-05 map tile/actor、PR-06 skill/status 资源提前塞入 PR-02-1。
2. 不得通过手写 runtime manifest 或裸资源路径绕过 key registry / canonical manifest。
3. 不得把右栏装备/背包继续作为纯文本列表主路径。
4. 不得把左侧任务文字栏当作 demo 左侧 icon rail 的替代。
5. 不得把 PR-07 final audit 当作本 PR shell mismatch 的兜底。
6. 不得修改 core 规则、save/replay/profile schema、loot/shop economy、item stats 和 input command 语义。

## 1. 阶段目标

1. 在 PR-02 chrome 已接入的基础上，建立 `UI/UI-demo-new.png` 对齐的主界面框架。
2. 局内主 shell 必须从旧三栏文字布局，升级为 demo-like 结构：
   - 左侧垂直 icon rail。
   - 中央 dominant map stage。
   - 右侧装备、铭刻栏、背包、操作提示分区；不再展示地面物品区域。
   - 底部英雄卡、4 个快捷技能卡、命令提示、日志 deck。
3. 地图舞台空区必须使用 `ui.shell.map_stage.backdrop` 暗色地砖/雾感 shell 背景；最终 terrain/actor/prop 质量仍归 PR-05，但 PR-02-1 不能再用程序化黑底 grid 作为可接受差异。
4. PR-03/05/06 后续资源进入时，不需要再次推翻 shell 布局。
5. 输出 demo parity manual record，明确剩余差异归属，避免 PR-07 才发现主结构不对。

## 2. 影响范围

| Action | 路径 / 合同 | 预期改动 |
| --- | --- | --- |
| Added | `client/src/main/kotlin/com/ktome/client/render/layout/DemoShellLayout.kt` | 定义 demo shell typed bounds，不把主界面区域散落在 `TileRenderer` 魔术坐标中 |
| Modified | `client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt` | 保留现有非重叠约束，并将 PR-02-1 demo shell 区域接入 layout test |
| Modified | `client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt` | 底部 hero/action/log deck 重新按 demo 分区 |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 增加 shell scaffold presentation model：nav rail、right panel zones、grid placeholder、hero summary、right operation hints |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 按 demo shell draw order 重排：backdrop -> map stage -> map layers -> shell chrome -> nav/right/bottom decks -> overlays |
| Added | `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt` | 承载 PR-02-1 shell chrome/nav/right/bottom deck 绘制；`TileRenderer` 只负责 orchestration、map layer 调度和 overlay 调度 |
| Modified | `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt` | 使用 demo shell viewport/layout；modal/inventory/talent overlay 通过 `modalSafeBounds` 锚定，保持输入和 session 生命周期不变 |
| Modified | `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt` | 首页/主菜单与 demo shell chrome 密度对齐，不再像旧 debug menu |
| Modified | `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | 断言 demo shell 各区域存在、非重叠、drawAsset/drawText 均落在 content bounds 内，并记录 draw order marker |
| Added | `client/src/test/kotlin/com/ktome/client/render/DemoShellLayoutTest.kt` | 断言 `1280x800`、`1024x768`、demo aspect ratio 下布局结构稳定 |
| Added | `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt` | 用 fake sink 断言 `DemoShellRenderer` 的 `ui.shell.*` key 与 §6.2 consumer region 一一绑定，且 step 2 / 9 / 10 / 11a-d 的调用顺序稳定 |
| Modified | `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` | 新增 PR-02-1 golden labels |
| Modified | `UI/PLAN.md` | 把 PR-02-1 插入 PR-02 与 PR-03 之间，记录证据和验证入口 |
| Modified | `UI/pr/README.md` | 把 PR-02-1 加入执行索引、ownerPr 规则、sheet ownership、evidence matrix |
| Modified | `UI/pr/screen-coverage-matrix.md` | 把局内主 shell / main menu / validation setup 的 owner 与 PR-02-1 labels 同步进矩阵 |
| Added | `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` | 白盒记录，包含 demo-delta checklist 与截图 hash |
| Added | `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml` | 记录 PR-02-1 direct keys、required sheet id、direct/alias/reserved/total counts |
| Modified | `UI/sprite-sheets/sheet-plan.yaml`, `UI/sprite-sheets/key-registry.yaml` | 同步 `r01b-ui-shell-chrome` registry / sheet plan / manifest source |
| Added | `assets-src/image/raw/sheets/dark-v1/r01b-ui-shell-chrome.png` | 只包含 shell chrome、hero placeholder、nav rail icon 资源，并同步更新 `assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl` 中该 sheet 的 `rawSheetHash` |
| Modified | `scripts/dark_sprite_sheet_contract.py`, `scripts/verify_dark_key_registry.py`, `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt` | 将 ownerPr pattern 固定为 `^PR-\d{2}(?:-\d+)?$`；`verify_dark_key_registry.py` 复用该 pattern；脚本回归必须覆盖 `PR-02-1` pass、`PR-02-1-1` fail、`pr-02-1` fail |
| Modified | `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt` | 将 `UI01-1`、`UI02-1` 加入 `acceptanceContractLint` 静态合同覆盖 |
| Modified | `tools/build.gradle.kts`, `tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt`, `tools/src/test/kotlin/com/ktome/tools/verification/VerifyChangedBuildContractTest.kt`, `tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt` | 新增 `darkManifestCoveragePr02_1OwnerScope`、独立 report、tools-side dark-uiux routing；静态 task path 与 changed-surface routing 必须同时覆盖 |
| Modified | `build.gradle.kts` | 将 `:tools:darkManifestCoveragePr02_1OwnerScope` 加入 root `verifyChangedTaskPaths`；`verifyChanged` 计划文件必须能看到该 task，不能只在 `tools` 子项目注册 |
| Modified | `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`, `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt`, `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`, `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt` | 新增 `dark-uiux-pr02-1-demo-shell-foundation` packaged whitebox scenario；registry、presentation、YAML、materialization 必须四方同名；`phase4-v4-scenarios.yaml` 只声明 `id`，`prId`、evidence summary、required evidence file names 只由 `ValidationScenarioRegistry.kt` 提供 |
| Modified | `game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt`, `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt`, `client/src/test/kotlin/com/ktome/client/input/ValidationCommandSourceTest.kt` | 覆盖 scenario registry / id-only YAML / presentation / materialization parity、`prId=PR-02-1`、expected evidence 文件名、manual record 路径、packaged runbook 截图捕获命令 |
| Modified | `game/src/main/resources/i18n/zh-CN.json`, `game/src/main/resources/i18n/en-US.json` | 新增 PR-02-1 validation scenario 标题、描述和 evidence summary 文案；文案只描述白盒用途，不引入玩法规则 |

实现增长约束：

1. `TileRenderer.kt` 本 PR 净增长不得超过 `+400` 行；shell-specific 绘制必须落入 `DemoShellRenderer.kt`，不得再在 `TileRenderer.kt` 内新增第二个私有 shell renderer。
2. `TileRenderModel.kt` 新增字段必须是 shell presentation model，不允许复制 core/game 规则状态；新增 nav rail、right panel、bottom deck 三类 presentation model 时分别落在明确 data class 中。
3. `DemoShellLayout.kt` 是本 PR 的主 layout 合同文件；不得把 PR-02-1 的核心几何继续埋在 `TileRenderer` 私有坐标里。
4. 如果进入 PR-02-1 时 `TileRenderer.kt` 已经存在 shell chrome / bounded text / panel scaffold 临时实现，第一步必须迁移：把 shell-specific draw helper 移入 `DemoShellRenderer.kt`，把几何移入 `DemoShellLayout.kt` / `InfoSurfaceLayout.kt`，再继续新增功能；未完成迁移前不得捕获 golden 或 packaged whitebox evidence。

Deleted / replaced 清单：

| Path / Contract | Action | removalOwner | Regression Scan |
| --- | --- | --- | --- |
| 左侧大段任务文字栏作为主 rail | replace with icon rail + compact selected hint | PR-02-1 | `TileRendererCanvasTest` confirms nav rail icon surfaces exist and old full-height text rail is not the primary surface |
| 右侧装备/背包纯文字列表作为主路径 | replace with right panel section + slot/grid scaffold | PR-02-1 / PR-03 | PR-02-1 confirms scaffold; PR-03 fills final item icons |
| 底部裸 hint / debug HUD 密度 | replace with hero card + action deck + command hint + log deck | PR-02-1 | `InfoSurfaceLayoutTest`, golden screenshot |
| map 被 chrome 背景和 text panel 视觉压低 | replace with dominant map stage bounds | PR-02-1 / PR-05 | PR-02-1 confirms stage; PR-05 replaces map assets |
| `dark-v1-manifest-coverage.json` 作为 PR-02 owner-scope output 文件名 | replace with `dark-v1-manifest-coverage-pr02-owner-scope.json`; 裸 `darkManifestCoverageLint` 仍保留 `dark-v1-manifest-coverage.json` | PR-02-1 | `VerifyChangedBuildContractTest` confirms both PR-02 and PR-02-1 owner-scope report names; `VerificationImpactAnalyzerTest` confirms both owner-scope tasks route |

## 2.1 实施执行顺序

开发必须按以下顺序推进，避免先生成资源或先改 renderer 后发现 shell 合同不成立：

1. **preflight**：读取本文件、[README.md](./README.md)、[development-governance.md](./development-governance.md)、[screen-coverage-matrix.md](./screen-coverage-matrix.md)、[UI/PLAN.md](../PLAN.md)、[UI/ART_STYLE_BIBLE.md](../ART_STYLE_BIBLE.md)，执行 `acceptanceContractLint`。
2. **migration audit**：执行 `rg 'ui\\.shell\\.' client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` 与 `rg 'private fun draw[A-Z].*Shell' client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`。任一命令返回非空时，先迁移到 `DemoShellRenderer.kt` / `DemoShellLayout.kt`；迁移完成标准是两条命令均无输出。
3. **demo decomposition**：以 `UI/UI-demo-new.png` 拆分 shell 区域，写入 §3 的 typed region，不允许直接在 `TileRenderer` 写坐标试错。
4. **layout tests**：新增 `DemoShellLayoutTest`，先证明目标尺寸下 nav/map/right/bottom deck 的 bounds 成立。
5. **render model**：扩展 `TileRenderModel` / presenter，把现有 snapshot 数据投影到 nav rail、right panel zones、bottom deck；不得在 client 新增规则状态副本。
6. **renderer integration**：按 §4 draw order 接入 demo shell。所有 text/icon 都必须绘制在对应 content bounds 内。
7. **resource contract**：新增 `r01b-ui-shell-chrome`，按 §6 direct cell 表创建 owner contract、registry、sheet plan，并先补 `subject`、ownerPr regex、`darkManifestCoveragePr02_1OwnerScope` 和 verifyChanged routing。
8. **resource generation**：按 PR-00/PR-02 流程生成 prompt、raw sheet、切分、contact sheet、sprite map report、canonical manifest、runtime manifest。
9. **validation scenario**：新增 `dark-uiux-pr02-1-demo-shell-foundation` scenario 到 registry、presentation catalog、YAML、materialization catalog 和 locale bundle；scenario 必须先通过 parity tests，才能进入 packaged app 白盒。
10. **golden baseline**：新增 PR-02-1 golden labels；hash 漂移必须在 manual record 中解释为 shell foundation 重排。
11. **packaged whitebox**：捕获标准局内 shell、inventory modal open、right panel grid、main menu、validation setup standalone surface。截图必须证明 shell 不再是旧 text-first 结构。
12. **doc-vs-implementation audit**：PR ready-for-review 前和 `verifyChanged` 通过后各做一次；逐条对 §1、§3、§4、§5、§8 做 self-audit。任何未覆盖 demo 要素必须明确归属 PR-03/05/06/07。

## 3. Demo Shell Layout Contract

PR-02-1 必须新增 `client/src/main/kotlin/com/ktome/client/render/layout/DemoShellLayout.kt` 作为 typed shell layout authority。`DemoShellLayout` 只计算区域，不读取 gameplay rule，不拼 locale 文案，不解析 manifest。

### 3.1 Required Regions

| Region | Purpose | Demo 对齐要求 | Content rule |
| --- | --- | --- | --- |
| `outerFrame` | 全屏暗黑 UI 框架 | `navRail` 与 `outerFrame` 是 typed regions 中的并列 region；`outerFrame` 提供整屏背景与外缘装饰，但不得绘制覆盖 `navRail.bounds` 的装饰层 | 只放 frame/background，不放文本 |
| `navRail` | 左侧垂直 icon rail | 类似 demo 左侧窄栏，icon 垂直排列，且只占地图舞台高度 | icon button + selected state；不放整段任务说明，不贯穿底栏 |
| `mapStage` | 中央地图主舞台 | 最大视觉面积，地图被暗区包围 | 先绘制 `ui.shell.map_stage.backdrop`，再绘制真实 terrain / props / actors / loot / fog |
| `rightPanel` | 右侧总面板 | 类似 demo 右侧整列 | 分成 equipment、inscriptions、backpack、operation hints；不再包含 ground loot |
| `rightEquipment` | 装备区 | icon-first socket 区 | 真实 `WEAPON/OFF_HAND/ARMOR/ACCESSORY` 映射到对应 socket；helmet/cape/gloves/ring/boots 等仅 display-only empty socket，不新增规则层装备槽，不画假装备图标 |
| `rightInscriptions` | 铭刻栏区 | 双列 8 行 framed rows | `5..8` 为真实可操作铭刻，`9..12` 为空 framed row + 弱热键；文字必须在 row plate 内 |
| `rightBackpack` | 背包 grid | 当前页 `4x2` | page size 固定 8；超过 8 件显示 `1/N` 与 PgUp/PgDn 提示 |
| `rightOperationHints` | 右栏短帮助区 | demo 右侧底部操作提示 | 唯一可见快捷键提示区；presentation 文案源可复用，但底部不再绘制独立 command hints 区 |
| `heroCard` | 底部英雄信息 | crest + 名称 + 层数/HP/resource/attack/defense | 使用 `ui.shell.hero_crest.placeholder`；替代独立 bottom stats 卡 |
| `actionDeck` | 底部技能快捷栏 | 大卡片技能槽 | 固定支持 `PLAYER_ACTIVE_TALENT_SLOT_COUNT = 4`；minimum profile 可 `2x2` |
| `logDeck` | 日志区 | 独立可读 message area | 中文长句 wrap，不覆盖 hotbar/hero card |
| `modalSafeBounds` | overlay/modal 安全区 | modal 不遮挡关键 HUD 时仍可读 | 几何中心固定为 `mapStage.center`；bounds 必须被 clamp 到 nav/right/bottom chrome 之外 |

### 3.1.1 Layout Ownership Matrix

| Typed region | Layout owner | Test owner | Boundary rule |
| --- | --- | --- | --- |
| `outerFrame`, `navRail`, `mapStage`, `rightPanel`, `rightEquipment`, `rightInscriptions`, `rightBackpack`, `rightOperationHints`, `modalSafeBounds` | `DemoShellLayout` | `DemoShellLayoutTest` | demo shell 几何权威只在 `DemoShellLayout`，不得回落到 `TileRenderer` 私有坐标 |
| `heroCard`, `actionDeck`, `logDeck` | `InfoSurfaceLayout` | `InfoSurfaceLayoutTest` | bottom deck 三分区由 `InfoSurfaceLayout` 计算，`DemoShellLayout` 只消费其外层 bounds；footer hint 兼容 bounds 必须保持零宽 |
| `viewportSafeBounds`, PR-01 / PR-01-1 legacy non-demo shell compatibility | `GameShellLayout` | `GameShellLayoutTest` | `GameShellLayout` 只保留兼容与安全边界断言，不拥有 PR-02-1 demo typed regions |

### 3.2 Required Size Profiles

| Profile | Viewport | Requirement |
| --- | ---: | --- |
| standard | `1280x800` | 所有 demo shell regions 同屏可见，rightPanel 不压 mapStage，bottom deck 不压 map |
| minimum | `1024x768` | rightPanel 可收窄，log/command 可减行，但 nav/map/right/bottom 结构不消失 |
| demo-aspect | `1672x941` equivalent | golden/headless layout 可验证 demo aspect 下结构比例接近 `UI/UI-demo-new.png` |

硬约束：

| Metric | `1280x800` standard | `1024x768` minimum | `1672x941` demo-aspect | Required assertion |
| --- | ---: | ---: | ---: | --- |
| `mapStage.width / viewport.width` | `>= 0.58` | `>= 0.55` | `>= 0.62` | `DemoShellLayoutTest` |
| `mapStage.width / rightPanel.width` | `>= 2.05` | `>= 1.85` | `>= 2.05` | `DemoShellLayoutTest` |
| `navRail.width` | `<= 64 px` | `<= 64 px` | `<= 72 px` | `DemoShellLayoutTest` |
| `navRail.width / viewport.width` | `<= 0.06` | `<= 0.08` | `<= 0.05` | `DemoShellLayoutTest` |
| `rightPanel.width / viewport.width` | `0.25..0.32` | `0.27..0.35` | `0.24..0.30` | `DemoShellLayoutTest` |
| `bottomDeck.height` | `>= 180 px` | `>= 164 px` | `>= 208 px` | `InfoSurfaceLayoutTest` |
| `bottomDeck.height / viewport.height` | `>= 0.22` | `>= 0.20` | `>= 0.22` | `InfoSurfaceLayoutTest` |
| `heroCard.width` | `>= 300 px` | `>= 260 px` | `>= 420 px` | `InfoSurfaceLayoutTest` |
| `hero portrait / crest bounds` | `>= 96x96 px` | `>= 80x80 px` | `>= 108x108 px` | `TileRendererCanvasTest` |
| `right slot side` | `>= 48 px` | `>= 40 px` | `>= 56 px` | `TileRendererCanvasTest` |
| `rightEquipment grid` | `2 cols x 4 rows + boots centered, 9 socket bounds` | same | same | `DemoShellLayoutTest`, `TileRendererCanvasTest` |
| `rightInscriptions grid` | `2 cols x 4 rows` | same | same | `DemoShellLayoutTest`, `TileRendererCanvasTest` |
| `rightBackpack grid` | `4 cols x 2 rows` | same | same | `TileRendererCanvasTest` |
| `modalSafeBounds.width / viewport.width` | `0.55..0.80` | `0.55..0.85` | `0.50..0.78` | `DemoShellLayoutTest` |
| `modalSafeBounds.height / viewport.height` | `0.55..0.78` | `0.55..0.85` | `0.55..0.78` | `DemoShellLayoutTest` |

附加硬约束：

1. `bottomDeck` 必须从窗口左边开始、到右栏左边结束，并能容纳 hero card、action deck、log deck 三类内容，不能用裸 footer 代替；可见快捷键说明只能出现在右栏 `operation hints`。
2. `rightBackpack`、`rightEquipment`、`rightInscriptions` 必须以 slot/grid bounds 表达，不能只返回 text baseline。
3. 所有 region 都必须有 content bounds；text/icon 不允许压到 frame edge。
4. `modalSafeBounds` 必须与 `mapStage`、`rightPanel`、`bottomDeck` 同源计算；不得在 `FoundationGameScreen` 里单独维护第二套 modal 坐标。
5. `modalSafeBounds.center == mapStage.center`；`modalSafeBounds.left >= navRail.right`，`modalSafeBounds.right <= rightPanel.left`，`modalSafeBounds.bottom >= bottomDeck.top`，且不得与 `navRail`、`rightPanel`、`bottomDeck` 任一区域相交。

## 4. Renderer And Layer Order Contract

`TileRenderer` 保持 orchestration owner，但 PR-02-1 必须新增 `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`。`TileRenderer` 只负责 `DemoShellLayout` 计算结果、map layer、overlay layer 的串行调度；`DemoShellRenderer` 负责 shell chrome、nav rail、right panel scaffold、bottom hero/action/log deck 的绘制。禁止把 PR-02-1 的 shell 绘制继续塞进 `TileRenderer` 私有大对象，也禁止增加与规则层耦合的第二 authority。

### 4.1 Draw Order

固定绘制顺序：

| step | layer |
| ---: | --- |
| 1 | clear / dark backdrop |
| 2 | `outerFrame` / shell background |
| 3 | `mapStage` frame and manifest-backed map-stage backdrop / scrim |
| 4 | terrain base |
| 5 | props / decals |
| 6 | actors / player indicator |
| 7 | ground loot markers |
| 8 | fog / light masks recording marker; no-op until PR-05 implements lighting/fog art |
| 8a | active cursor / combat feedback; still bounded to `mapStage` and must remain before shell chrome |
| 9 | nav rail chrome + nav icons |
| 10 | right panel chrome + zones + grid placeholders |
| 11a | bottom hero card |
| 11b | bottom action deck |
| 11c | bottom log deck |
| 12 | tooltip / modal backdrop |
| 13 | modal / explicit tooltip |
| 14 | validation overlay / debug-only evidence markers |

PR-02-1 不要求实现复杂动态光照，但必须先用 `ui.shell.map_stage.backdrop` 承担 UI-demo-new 首屏暗场质感，并预留 `mapStage` 内的 fog/light 层，不得把后续 PR-05 lighting/fog 只能塞进 UI panel 背后。

### 4.2 Rendering Rules

1. 所有 PR-02 chrome 继续通过 `VisualManifestResolver` 和 `ClientTextureRepository` 消费，不写 raw path。
2. `ChromeFramePainter` 是 frame 主路径；PR-02-1 shell frame key 必须通过同一 painter 的 9-slice/edge/corner/body 绘制。
3. `drawRect` 只能用于 scrim、bar、debug-safe fill、fallback outline；不能作为 demo 主 chrome 的唯一视觉。
4. `TileTextMetrics` 是 text fit 唯一入口；不再新增裸 `font.draw` 固定坐标。
5. `TileRendererCanvasTest` 必须记录并断言每个 shell surface 的 owner bounds。
6. `TileRendererCanvasTest` 必须能区分 draw order step 8、9、10、11a、11b、11c、11d、12、13、14；各 step 通过 invocation index 单调递增断言顺序，step 8 / 12 / 13 / 14 即使暂时 no-op 也要保留 recording marker，避免后续 PR 插入新层时改动主 shell 顺序。
7. `goldenScreenshot` 只能作为视觉回归，不替代 bounds / owner key focused tests。
8. demo shell 重排导致 golden hash 漂移时，manual record 的 `goldenLabels[]` 必须写 `hashDriftReason=dark-uiux-pr02-1-demo-shell-foundation`。
9. `DemoShellRendererTest` 是 `DemoShellRenderer` 的 focused test owner；它必须断言 step 2、9、10、11a、11b、11c、11d 的 `ui.shell.*` manifest key、consumer region、bounds 绑定和调用顺序。`TileRendererCanvasTest` 负责整合后的 owner-bounds 与 draw order marker，不替代 `DemoShellRendererTest` 的 key-binding 断言。

## 5. Right Panel And Bottom Deck Scaffold

本 PR 需要把右侧和底部先搭成 demo 的容器结构，但不得提前实现 PR-03/05/06 的资源范围。

### 5.1 Right Panel Scaffold

右侧面板必须从上到下稳定分区：

1. 装备：icon-first sockets；真实 `WEAPON/OFF_HAND/ARMOR/ACCESSORY` 只映射到 client presentation socket，helmet/cape/gloves/ring/boots 等为 display-only empty socket，不新增规则层装备槽，也不画假装备 icon。
2. 铭刻栏：`5..12` 双列 framed rows；`5..8` 是当前可操作铭刻，`9..12` 为空 framed row + 弱热键，禁止用假图标、假名称或 `预留` 文案撑满。
3. 背包：固定当前页 `4x2` page grid，空格和已有物品都有 slot bounds；首屏真实有 8 件时不显示页码，翻页 fixture 另证第 9 件以后可达。
4. 操作提示：右栏短行 operation hints，可换行但不压 backpack grid；底部不得再绘制独立 `commandHints` 区。

Slot 几何合同：

| rightPanel section | Minimum structure | slot side @1280 | slot side @1024 | Required assertion |
| --- | --- | ---: | ---: | --- |
| `rightEquipment` | `2 cols x 4 rows + boots centered` visual arrangement, `9` socket bounds | `>= 42 px` | `>= 38 px` | `DemoShellLayoutTest`, `TileRendererCanvasTest` |
| `rightInscriptions` | `2 cols x 4 rows`, row backing + icon/slot + `5..12` labels remain visible | `>= 48 px` | `>= 40 px` | `DemoShellLayoutTest`, `TileRendererCanvasTest` |
| `rightBackpack` | `4 cols x 2 rows` current page, plus pager footer | `>= 42 px` | `>= 38 px` | `TileRendererCanvasTest` |
| `rightOperationHints` | compact text lines inside section bounds | `N/A` | `N/A` | `TileRendererCanvasTest` text bounds |

PR-02-1 的 right panel grid 只允许使用 PR-02 `ui.frame.slot.*` slot chrome、空槽 scrim/fill、已有文本 count 和本 PR 定义的 `ui.shell.right_section.divider`。所有 `ui.shell.*` key 都只能用于 §6.2 表格 `consumer` 列声明的 typed region；任何跨 region 使用 `ui.shell.*` key 的行为均为失败。`ui.shell.hero_crest.placeholder` 只能用于 `heroCard`，`ui.shell.nav.*` 只能用于 `navRail`，不得作为装备、铭刻、背包 slot 的 placeholder。PR-03 必须在这个 scaffold 上替换 item-specific icon、quality、stack count、shop affordance。

### 5.2 Bottom Deck Scaffold

底部必须从左到右稳定分区：

1. hero card：职业/名称、层数、HP/resource gauges、attack/defense summary、portrait/crest placeholder；portrait/crest 不得小于 §3.2 的尺寸下限。
2. action deck：`PLAYER_ACTIVE_TALENT_SLOT_COUNT` 对应的 card slots；当前合同为 `4`，不得再按旧 demo 只画 3 个技能。standard 与 demo-aspect 断点使用一行 4 slots；minimum 断点允许用 `2 x 2` 保持 log/command 可读；slot side 下限分别为 minimum `72 px`、standard `68 px`、demo-aspect `96 px`。
3. log deck：近期日志，多行 wrap，直接接在 action deck 右侧；底部不得再保留独立 command hints 区。
层数、生命、资源、攻击/防御紧凑摘要统一归 `heroCard`，禁止再新增独立 `bottomStatsSummary` 卡挤压日志区。禁止把 HP bar 绘在 text baseline 上；禁止把 command hints 裸贴窗口底边；禁止 action card 文字超出 slot frame。

### 5.3 Left Navigation Rail Scaffold

左侧 nav rail 必须 icon-first：

1. 至少包含 compass/map、bag/equipment、scroll/log、book/talent、settings/accessibility 的视觉槽位。
2. `r01b-ui-shell-chrome` 必须生成 `ui.shell.nav.compass`、`ui.shell.nav.bag`、`ui.shell.nav.scroll`、`ui.shell.nav.book`、`ui.shell.nav.gear` 五个 nav icon key；只生成 `ui.shell.nav_button.active` 不足以完成 icon rail。
3. 当前选中状态必须有 frame/tint/focus，不依赖颜色唯一表达。
4. 任务摘要必须作为 tooltip/compact hint，不得重新占据左栏大面积文本。

## 6. Round 1B Shell Chrome Contract

本 PR 必须新增 `r01b-ui-shell-chrome`。PR-02 的 `ui.frame.panel.*`、`ui.frame.slot.*`、`ui.control.*`、`ui.hud.*` 继续作为 fallbackKey，不再作为 PR-02-1 主视觉完成标准。该 sheet 只允许 shell chrome、hero placeholder 和 nav rail UI icon；不得包含 item、map tile、actor、skill、status、quest 图。

### 6.1 Owner Scope Rule

PR-02-1 owner-scope 的实现分母来自 `key-registry.yaml.entries[].ownerPr == PR-02-1`，close gate 固定运行 `darkManifestCoveragePr02_1OwnerScope` 并读取 `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml`。

`ownerExpectedKeys` 必须与以下两类完全一致：

1. `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml.requiredCells[].targetKey`。
2. §6.2 的 direct cell keys。

`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md` §6.2 是 PR-02-1 direct cell 真源；`pr02-1-owner-keys.yaml.requiredCells[].targetKey` 由 §6.2 派生。任一漂移必须先修 §6.2，再同步 owner contract；不得反向修改 yaml 让 §6.2 适配。

PR-02 keys 不能被改成 PR-02-1 owner；PR-02-1 只拥有新增 shell scaffold keys。

本 PR 必须修改 `scripts/dark_sprite_sheet_contract.py` 的 `OWNER_PR_PATTERN` 为 `re.compile(r"^PR-\d{2}(?:-\d+)?$")`。`scripts/verify_dark_key_registry.py` 必须继续从 `dark_sprite_sheet_contract.py` import 同一个 `OWNER_PR_PATTERN`，不得复制第二份正则。`DarkSpriteSheetPipelineScriptTest` 必须新增以下回归：`PR-02-1` 合法，`PR-02-1-1` 非法，`pr-02-1` 非法。不得把 PR-02-1 keys 临时登记为 `PR-02` 来绕过 owner-scope。`scripts/verify_dark_key_registry.py` 与 `scripts/dark_sprite_sheet_contract.py` 的 ownerPr 诊断文本必须输出 `OWNER_PR_PATTERN` 正则字面量，例如 `ownerPr must match ^PR-\d{2}(?:-\d+)?$, got '<value>'`；不得硬编码列举 `PR-00`、`PR-02-1` 等具体编号。

PR-02-1 实施时必须同步修改 coverage routing：

1. `tools/build.gradle.kts` 新增 `darkManifestCoveragePr02_1OwnerScope`，固定 `mode=owner-scope`、`ownerPr=PR-02-1`、`ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml`、`reportFileName=dark-v1-manifest-coverage-pr02-1-owner-scope.json`。
2. `darkManifestCoveragePr02OwnerScope` 的 `reportFileName` 必须改为 `dark-v1-manifest-coverage-pr02-owner-scope.json`，禁止继续与裸 `darkManifestCoverageLint` 共用 `dark-v1-manifest-coverage.json`。旧 `dark-v1-manifest-coverage.json` 文件名仅在裸 `darkManifestCoverageLint` 路径下继续可见；PR-02 owner-scope 历史引用由本 PR 描述显式声明迁移到新文件名。
3. `VerificationTaskRegistry.darkUiuxPipelineDomain.ownerTaskPaths` 必须同时包含 `:tools:darkManifestCoveragePr02OwnerScope` 和 `:tools:darkManifestCoveragePr02_1OwnerScope`。
4. `VerificationImpactAnalyzerTest` 必须断言 dark UI/UX 资源、manifest、pipeline script 变更会请求 PR-02 与 PR-02-1 两个 owner-scope coverage task，且不会请求裸 `:tools:darkManifestCoverageLint`。
5. root `build.gradle.kts` 的 `verifyChangedTaskPaths` 必须包含 `:tools:darkManifestCoveragePr02_1OwnerScope`，并由 `VerifyChangedBuildContractTest` 和 `VerificationImpactAnalyzerTest` 同时覆盖；只在文档或 `tools/build.gradle.kts` 声明 task 不算完成。`VerifyChangedBuildContractTest` 断言 root `build.gradle.kts` 的 `verifyChangedTaskPaths` 字面量列表包含 `:tools:darkManifestCoveragePr02_1OwnerScope`；`VerificationImpactAnalyzerTest` 断言 dark UI/UX 资源、manifest、pipeline script 变更同时触发 PR-02 与 PR-02-1 两个 owner-scope task。
6. `VerifyChangedBuildContractTest` 必须断言 `darkManifestCoveragePr02OwnerScope.reportFileName == "dark-v1-manifest-coverage-pr02-owner-scope.json"`，且 `darkManifestCoveragePr02_1OwnerScope.reportFileName == "dark-v1-manifest-coverage-pr02-1-owner-scope.json"`，禁止任一 owner-scope task 回退到裸 `dark-v1-manifest-coverage.json`。

### 6.2 `r01b-ui-shell-chrome` direct cells

`r01b-ui-shell-chrome`: `large-sheet 1024x1024 / 4x4 / 256x256`。本 sheet 只允许 `ui_frame` / shell placeholder / shell nav icon，不允许 item、map tile、actor、skill、status、quest 图。

| row | col | targetKey | category | outputName | subject | fallbackKey | aliasOf | consumer | consumerTest |
| ---: | ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | 0 | `ui.shell.outer_frame` | `ui_frame` | `dark-v1/ui/ui_shell_outer_frame.png` | `forged iron full-screen shell frame with worn stone inset and subtle cyan rim light` | `ui.frame.panel.body` | `N/A` | demo shell outer frame | `TileRendererCanvasTest` |
| 0 | 1 | `ui.shell.map_stage.frame` | `ui_frame` | `dark-v1/ui/ui_shell_map_stage_frame.png` | `wide dark map stage frame with charcoal stone center and brass corner braces` | `ui.frame.panel.body` | `N/A` | map stage frame | `TileRendererCanvasTest` |
| 0 | 2 | `ui.shell.nav_rail.frame` | `ui_frame` | `dark-v1/ui/ui_shell_nav_rail_frame.png` | `narrow vertical navigation rail frame in black iron with worn rivets` | `ui.frame.panel.body` | `N/A` | left icon rail | `TileRendererCanvasTest` |
| 0 | 3 | `ui.shell.nav_button.active` | `ui_frame` | `dark-v1/ui/ui_shell_nav_button_active.png` | `selected square nav button frame with restrained cyan focus edge and ember brass ticks` | `ui.frame.slot.selected` | `N/A` | selected nav icon button | `TileRendererCanvasTest` |
| 1 | 0 | `ui.shell.hero_card.frame` | `ui_frame` | `dark-v1/ui/ui_shell_hero_card_frame.png` | `bottom hero card frame with crest socket, dark stone face, and iron lower lip` | `ui.frame.panel.body` | `N/A` | bottom hero card | `InfoSurfaceLayoutTest` |
| 1 | 1 | `ui.shell.action_deck.frame` | `ui_frame` | `dark-v1/ui/ui_shell_action_deck_frame.png` | `horizontal action card deck frame with four evenly spaced heavy card sockets and ember dividers` | `ui.frame.panel.body` | `N/A` | bottom action deck | `InfoSurfaceLayoutTest` |
| 1 | 2 | `ui.shell.log_deck.frame` | `ui_frame` | `dark-v1/ui/ui_shell_log_deck_frame.png` | `wide message log deck frame with deep charcoal interior and thin cyan top edge` | `ui.frame.tooltip.body` | `N/A` | bottom log deck | `InfoSurfaceLayoutTest` |
| 1 | 3 | `ui.shell.right_section.divider` | `ui_frame` | `dark-v1/ui/ui_shell_right_section_divider.png` | `thin right-panel section divider strip in worn brass and black iron` | `ui.frame.panel.body` | `N/A` | right panel section divider | `TileRendererCanvasTest` |
| 2 | 0 | `ui.shell.hero_crest.placeholder` | `icon` | `dark-v1/ui/ui_shell_hero_crest_placeholder.png` | `dark fantasy hero crest placeholder, shield silhouette with ember scratches` | `ui.control.equipment.icon` | `N/A` | hero card crest placeholder | `TileRendererCanvasTest` |
| 2 | 1 | `ui.shell.command_hint.plate` | `ui_frame` | `dark-v1/ui/ui_shell_command_hint_plate.png` | `small command hint plate with iron bevel and muted stone interior` | `ui.frame.tooltip.body` | `N/A` | command hint plate | `TileRendererCanvasTest` |
| 2 | 2 | `ui.shell.nav.compass` | `icon` | `dark-v1/ui/ui_shell_nav_compass.png` | `compass rose navigation icon in aged brass with tiny cyan center glow` | `ui.hud.quest_marker.icon` | `N/A` | nav rail map/compass button | `TileRendererCanvasTest` |
| 2 | 3 | `ui.shell.nav.bag` | `icon` | `dark-v1/ui/ui_shell_nav_bag.png` | `adventurer backpack nav icon in leather and dark brass, readable at small size` | `ui.control.backpack.icon` | `N/A` | nav rail backpack button | `TileRendererCanvasTest` |
| 3 | 0 | `ui.shell.nav.scroll` | `icon` | `dark-v1/ui/ui_shell_nav_scroll.png` | `rolled parchment scroll nav icon with dark wax seal and no letters` | `ui.hud.log_marker.icon` | `N/A` | nav rail log/scroll button | `TileRendererCanvasTest` |
| 3 | 1 | `ui.shell.nav.book` | `icon` | `dark-v1/ui/ui_shell_nav_book.png` | `closed spellbook nav icon with black cover, brass clasp, and cyan edge glint` | `ui.control.copy.icon` | `N/A` | nav rail talent/book button | `TileRendererCanvasTest` |
| 3 | 2 | `ui.shell.nav.gear` | `icon` | `dark-v1/ui/ui_shell_nav_gear.png` | `forged gear nav icon in dark iron with worn teeth and ember center` | `ui.hud.warning.icon` | `N/A` | nav rail settings/accessibility button | `TileRendererCanvasTest` |
| 3 | 3 | `ui.shell.map_stage.backdrop` | `ui_frame` | `dark-v1/ui/ui_shell_map_stage_backdrop.png` | `near-black radial dungeon fog backdrop for map-stage empty space, soft smoky vignette, subtle soot scratches, no visible tile grid, no brick wall, no actor, no item, no readable text, shell presentation only` | `ui.frame.panel.body` | `N/A` | map stage dark fog backdrop | `TileRendererCanvasTest`, `DemoShellRendererTest` |

Column ownership:

| Column | Destination |
| --- | --- |
| `sheetId`, `row`, `col`, `targetKey`, `category`, `outputName`, `subject`, `reserved`, `aliasOf` | `UI/sprite-sheets/sheet-plan.yaml` |
| `targetKey`, `category`, `ownerPr=PR-02-1`, `sheetId`, `fallbackKey`, `consumer`, `consumerTest`, `aliasOf` | `UI/sprite-sheets/key-registry.yaml` |
| `outputName` | canonical/runtime visual manifest `rawOutputPath` after `syncPhase2Manifests` |

`subject` 是 prompt、sheet-plan 和 contact sheet QA 的输入真源。contact sheet QA 不能改写 subject 来让失败资源通过；若 `subject` 与 §3 / §5 数量或用途冲突，必须回到本表修正文案并重新生成 / repack 资源。

`ui.shell.nav.gear` 的 `ui.hud.warning.icon` fallback 是 last-resort 视觉降级，仅用于 r01b 生成或 manifest 同步失败时的故障可见性。正常 PR-02-1 完成路径必须满足 §6.3 的 r01b `USED` 约束，不能让玩家看到 warning icon 代替 settings/accessibility gear。

### 6.3 Grid Occupancy Contract

| sheetId | grid | direct cells | alias cells | reserved cells | total slots |
| --- | ---: | ---: | ---: | ---: | ---: |
| `r01b-ui-shell-chrome` | `4x4` | `16` | `0` | `0` | `16` |

`r01b-ui-shell-chrome` 是 PR-02-1 的 mandatory sheet。未生成、未切分、未进入 canonical/runtime manifest、未通过 owner-scope coverage 时，本 PR 不能进入 client evidence 阶段。禁止只登记 owner keys 而不生成/slice/cover sheet。

`ui.shell.map_stage.backdrop` 只解决 PR-02-1 新 DEMO 所需的 shell 舞台暗场质感，不是 `tileset.*`，也不承担 PR-05 terrain / actor / prop / VFX 的最终美术质量。renderer 只能通过 manifest key 消费它，禁止写 raw path 或把它作为地图规则资源。

`fallbackKey` 只用于 manifest/resolver 降级安全，不构成 PR-02-1 视觉完成证据。manual record 中 `leftIconRail` 只有在五个 `ui.shell.nav.*` runtime PNG 都被实际消费且视觉可区分时才能写 `pass`；使用 fallback 渲染 nav icon 时必须写 `fail`，并返回 §6 resource gate 修复。

## 7. Standalone / Main Menu Alignment

PR-02 已经让 standalone screens 消费共享 chrome。PR-02-1 必须让 main menu 和 validation setup 与新的 demo shell 密度保持一致：

1. Main menu 不得继续像独立 debug setup panel；它必须使用与局内 shell 同族的 frame 密度、焦点态和 footer slot。
2. Main menu footer/help text 必须留在 chrome content slots 内，不能裸贴窗口底边。
3. Validation setup 的 scenario list、detail panel、footer help 必须使用与局内 right panel section 相同的 frame 密度和 row gap。
4. Victory/GameOver/UiError 本 PR 不做 PR-07 final polish，但 shared layout helpers 不能回退到旧固定坐标文本页。
5. Required evidence labels: `dark-uiux-pr02-1-demo-main-menu`, `dark-uiux-pr02-1-demo-validation-setup`.

Standalone minimum geometry:

| Surface | Minimum geometry contract | Required assertion |
| --- | --- | --- |
| main menu title / brand zone | title zone height `>= 96 px @1280x800`; primary action group stays inside chrome content bounds | `MainMenuScreenTextTest` |
| validation setup scenario list | row height `>= 48 px`; row gap `>= 8 px`; detail panel never overlaps footer help | `StandaloneScreenLayoutTest` |
| outcome/error shared card | card width `>= 480 px @1280x800` and `>= 420 px @1024x768`; detail text maxLines enforced | `StandaloneScreenLayoutTest` |

## 8. Demo Parity Manual Record

新增 `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`。它不是普通测试日志，必须以 `UI/UI-demo-new.png` 为参照输出 demo-delta checklist。

### 8.1 Required Screenshots

| label | required surface | golden artifact | packaged/manual artifact |
| --- | --- | --- | --- |
| `ui-demo-new-parity-1672x941` | demo-aspect 局内 shell | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.*` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-parity-1672x941.png` |
| `ui-demo-new-parity-1280x800` | 标准局内 shell | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1280x800.*` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-parity-1280x800.png` |
| `ui-demo-new-right-panel-grid` | 右侧装备/铭刻/背包/操作提示 scaffold | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.*` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-right-panel-grid.png` |
| `ui-demo-new-bottom-deck-no-command-hints` | 底部 hero/action/log deck | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.*` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-bottom-deck-no-command-hints.png` |
| `ui-demo-new-inventory-page-1` | inventory pagination page 1 | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-1.*` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-inventory-page-1.png` |
| `ui-demo-new-inventory-page-2` | inventory pagination page 2 | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-2.*` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-inventory-page-2.png` |

以上 label 是 golden/manual 的主截图标签。`ui-demo-new-nav-rail-crop` 与 `ui-demo-new-map-stage-crop` 允许复用同一帧作为 crop evidence，只用于放大核验 nav icon 和 map-stage 细节，不需要重复生成独立内容帧。

### 8.1.1 Packaged Whitebox Scenario Contract

`preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr02-1-demo-shell-foundation` 必须 materialize 一个 typed scenario，而不是依赖 reviewer 手工拼路径。

| 合同字段 | 固定值 |
| --- | --- |
| `scenarioId` | `dark-uiux-pr02-1-demo-shell-foundation` |
| `prId` | `PR-02-1` |
| `windowSize` | `1280x800` |
| `locale` | `zh-CN` |
| `startupMode` | `DIRECT_VALIDATION_SESSION` |
| `initialOverlaySection` | `PHASE4_V4_FAST` |
| `whiteboxRoot` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation` |
| `evidenceDir` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence` |
| `manualRecord` | `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` |
| `appLog` | `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/dark-uiux-pr02-1-demo-shell-foundation-app.log` |

`windowSize=1280x800` 适用于全部 5 个 screenshot label，包括 main menu 和 validation setup standalone surface。`1024x768` 与 demo-aspect profile 只由 `DemoShellLayoutTest` / `InfoSurfaceLayoutTest` headless assertions 覆盖，不进入 packaged whitebox evidence。

`windowSize`、`locale`、`whiteboxRoot`、`manualRecord` 与 app log 命名全部由 `Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 固化，CLI 只允许传 `-Pktome.whitebox.scenario=dark-uiux-pr02-1-demo-shell-foundation`，不得允许额外 `-P` 覆盖。`evidenceDir = ${whiteboxRoot}/evidence`，`appLog = ${evidenceDir}/${scenarioId}-app.log`；catalog 必须从 `whiteboxRoot` 与 `scenarioId` 派生其余 path，禁止独立维护多处字面量。若 launch helper 仍暴露 generic `evidence/app.log` 概念，materialization 输出必须把它归一到 `${scenarioId}-app.log`，manual record 和 runbook 只引用 `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/dark-uiux-pr02-1-demo-shell-foundation-app.log`。

必须生成的 evidence 文件名：

1. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-parity-1672x941.png`
2. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-parity-1280x800.png`
3. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-right-panel-grid.png`
4. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-bottom-deck-no-command-hints.png`
5. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-inventory-page-1.png`
6. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-inventory-page-2.png`
7. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-nav-rail-crop.png`
8. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-map-stage-crop.png`
9. `build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/dark-uiux-pr02-1-demo-shell-foundation-app.log`

实现文件与测试：

1. `ValidationScenarioRegistry.kt` 声明 `prId=PR-02-1`、scenario evidence summary 和 required evidence file names。
2. `ValidationScenarioPresentationCatalog.kt` 声明 `DIRECT_VALIDATION_SESSION` 和 initial overlay section。
3. `phase4-v4-scenarios.yaml` 只写入相同 scenario id，不写 evidence、prId、manual record 或 path 字段；当前 `ValidationScenarioRegistry.loadYamlScenarioIds` 会拒绝除 `id` 外的 YAML key，不能把 YAML 扩展成第二份 scenario authority。
4. `Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 声明 window size、locale、derived evidence path、app log path、manual record path 和 launch script values。
5. `ValidationScenarioRegistryTest` 断言 registry/YAML parity、`scenario.prId == "PR-02-1"`、id-only YAML 约束和精确 required evidence file names。
6. `Phase4V4WhiteboxScenarioCliTest` 断言 launch script 包含 PR-02-1 whitebox root、manual record path、app log path、`1280x800` viewport、`zh-CN` locale，并断言 generated runbook 的 capture commands 覆盖五个 screenshot labels 和两个 crop evidence labels；expected evidence list 必须从 `ValidationScenarioRegistry.kt` 派生，不能从 YAML 派生。
7. `ValidationCommandSourceTest` 断言 presentation catalog parity 包含 PR-02-1，并暴露 scenario title key。

### 8.2 Demo Delta Checklist

manual record 必须逐项记录：

| demo element | PR-02-1 expected status | judgeMode | required evidence |
| --- | --- | --- | --- |
| left icon rail | must be structurally present and must use `ui.shell.nav.*` keys | machine + manual | `DemoShellLayoutTest`, screenshot |
| dominant center map stage | must be structurally present and satisfy §3.2 map ratios | machine + manual | `DemoShellLayoutTest`, screenshot |
| right equipment sockets | must be structurally present; final item art may defer to PR-03 | machine | `DemoShellLayoutTest`, `TileRendererCanvasTest` |
| right inscription rows/slots | must be structurally present as `5..12` framed rows; final inscription art may defer to PR-03/06 | machine | `DemoShellLayoutTest`, `TileRendererCanvasTest` |
| backpack grid | must be structurally present and satisfy §5.1 slot geometry | machine | `TileRendererCanvasTest` |
| right operation hints | must be inside a shell section and share command presentation text | machine + manual | `TileRendererCanvasTest`, screenshot |
| bottom hero card | must be structurally present; `ui.shell.hero_crest.placeholder` must meet §3.2 minimum size | machine + manual | `InfoSurfaceLayoutTest`, screenshot |
| bottom action deck | must be structurally present; final skill art may defer to PR-06 | machine | `InfoSurfaceLayoutTest` |
| bottom command hints | must not render as a visible bottom region; shortcuts live only in right operation hints | machine + manual | `DemoShellRendererTest`, `TileRendererCanvasTest`, screenshot |
| log deck | must be inside its own deck and readable in zh-CN | machine + manual | `TileRendererCanvasTest`, screenshot |
| map stage backdrop | `ui.shell.map_stage.backdrop` must render inside mapStage before terrain | machine + manual | `DemoShellRendererTest`, `TileRendererCanvasTest`, screenshot |
| UI-demo-new first-screen terrain/actor/prop readability | must be acceptable through PR-02-2 demo keys; broader content art remains PR-05 | machine + manual | `ManifestResolveTest`, `TileRendererCanvasTest`, screenshot |
| item/icon art quality | allowed remaining gap for PR-03 | manual | `remaining gap` table |
| final all-screen polish | allowed remaining gap for PR-07 | manual | `remaining gap` table |

如果本 PR 顺手降低了 PR-03/05/06/07 的剩余 gap，PR 描述必须新增 `outOfScopeReductions` 小节，说明独立 commit、触发证据和不改变后续 owner 合同的理由。

Blocking finding examples:

1. Left rail still shows full-height task text as primary UI.
2. Right panel still uses long text list as the only equipment/backpack surface.
3. Bottom HUD still reads as debug bars and loose hint text.
4. Map is visually secondary to side panels in standard window.
5. Any shell text overlaps frame, exits content bounds, and fails the owner-bounds assertion.

## 9. 验收标准

1. `DemoShellLayoutTest` proves `1280x800`、`1024x768`、demo aspect ratio 下 nav/map/right/bottom regions 不重叠。
2. `TileRendererCanvasTest` proves left icon rail、right panel zones、backpack grid、hero card、action deck、log deck all draw inside owner bounds.
3. `TileRendererCanvasTest` proves right panel no longer relies on pure text list as the only equipment/backpack representation.
4. `InfoSurfaceLayoutTest` proves bottom hero/action/log decks do not overlap and remain readable in zh-CN; footer hint compatibility bounds stay zero-width.
5. `MainMenuScreenTextTest` / `StandaloneScreenLayoutTest` prove main menu and standalone shared chrome did not regress.
6. `darkManifestCoveragePr02_1OwnerScope` reports non-empty `ownerExpectedKeys`, `ownerCoveredKeys == ownerExpectedKeys`, `ownerMissingKeys=[]`, `ownerUnexpectedKeys=[]`, `ownerOldStyleKeys=[]`.
7. `goldenScreenshot` contains PR-02-1 labels and stable hashes.
8. Packaged app whitebox captures standard shell and inventory/right-panel grid evidence.
9. Manual record states the shell is demo-like in structure, confirms there is no duplicate bottom command hint region, and does not carry UI-demo-new first-screen map backdrop / actor / stairs quality as PR-05 deferred.
10. `DemoShellLayoutTest` must assert §3.2 中由 `DemoShellLayout` 拥有的数值约束：mapStage ratios、navRail max width/ratio、rightPanel width band、modalSafeBounds width/height bands、`modalSafeBounds.center == mapStage.center`、且 `modalSafeBounds` 不与 nav/right/bottom regions 相交。
11. `InfoSurfaceLayoutTest` must assert §3.2 / §5.2 中由 `InfoSurfaceLayout` 拥有的 bottomDeck height/ratio、hero card width、actionDeck card width `>= 96 px`、fixed slot gap `>= 12 px`、centered group alignment for `cardCount in 1..4`。
12. `DemoShellRendererTest` must assert `shellKeysBindToConsumerRegionOnly`: every `ui.shell.*` key is drawn only in the §6.2 `consumer` region and in the expected draw step; it must fail if any shell key is reused as an unrelated item/grid placeholder.
13. `TileRendererCanvasTest` must assert hero portrait bounds、right slot side、rightBackpack grid columns/rows、owner-bounds draw calls、draw order markers for steps 8..14, and the integrated `shellKeysBindToConsumerRegionOnly` evidence emitted by `DemoShellRenderer`。
14. `darkKeyRegistryLint` must accept `ownerPr=PR-02-1` through `OWNER_PR_PATTERN = ^PR-\d{2}(?:-\d+)?$` and explicit script regression, not through ownerPr downgrading to `PR-02`; failure diagnostics must print the regex literal, not a hard-coded PR owner example.
15. `VerifyChangedBuildContractTest` and `VerificationImpactAnalyzerTest` must both cover PR-02-1 owner-scope coverage: static root `verifyChangedTaskPaths` membership, report file names, and changed-surface routing.
16. `ValidationScenarioRegistryTest`、`Phase4V4WhiteboxScenarioCliTest`、`ValidationCommandSourceTest` must prove PR-02-1 packaged whitebox scenario registry / id-only YAML / presentation / materialization parity, fixed `prId=PR-02-1`、`1280x800` / `zh-CN` launch settings, five screenshot labels, two crop evidence labels, and runbook capture commands。
17. `verifyChanged` passes after all required focused/resource/client gates.

## 10. 验证

Gradle 命令必须串行执行。所有命令先进入 SDKMAN 环境：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

Preflight and script regression:

```bash
./gradlew :tools:acceptanceContractLint
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
```

Round 1B resource and manifest freshness gate:

```bash
./gradlew syncPhase2Manifests manifestLint assetLint styleLint \
  darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint \
  -Pktome.darkUiux.requireFullGrid=true \
  -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml \
  -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl

./gradlew darkManifestCoveragePr02_1OwnerScope
```

Focused layout / renderer / manifest:

```bash
./gradlew :client:test \
  --tests com.ktome.client.render.DemoShellLayoutTest \
  --tests com.ktome.client.render.DemoShellRendererTest \
  --tests com.ktome.client.render.GameShellLayoutTest \
  --tests com.ktome.client.render.InfoSurfaceLayoutTest \
  --tests com.ktome.client.render.TileRendererCanvasTest \
  --tests com.ktome.client.screen.StandaloneScreenLayoutTest \
  --tests com.ktome.client.screen.MainMenuScreenTextTest \
  --tests com.ktome.client.assets.ManifestResolveTest
```

Verification routing and packaged whitebox registry:

```bash
./gradlew :tools:test --tests com.ktome.tools.verification.VerifyChangedBuildContractTest
./gradlew :tools:test --tests com.ktome.tools.verification.VerificationImpactAnalyzerTest
./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest
./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest
./gradlew :client:test --tests com.ktome.client.input.ValidationCommandSourceTest
```

Client evidence and governance:

```bash
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint verifyChanged
git diff --check
```

Packaged app whitebox:

```bash
./gradlew :client:packageMacApp preparePhase4V4Whitebox \
  -Pktome.whitebox.scenario=dark-uiux-pr02-1-demo-shell-foundation
```

Manual whitebox must capture:

1. Standard in-game shell at `1280x800`.
2. Inventory modal open state.
3. Right panel grid open state.
4. Right panel focused evidence for equipment sockets, `5..12` inscription rows, `4x2` backpack page, and operation hints.
5. Bottom deck focused evidence for hero card, action deck, log deck, and absence of a duplicate bottom command-hints block.
6. Main menu / standalone shell alignment.
7. Validation setup list/detail/footer alignment.
8. Screenshot metadata with repo-relative evidence paths in manual record.

Manual whitebox detailed checklist:

| Step | Surface | Required capture | Pass condition | Fail condition |
| ---: | --- | --- | --- | --- |
| 1 | standard shell | `ui-demo-new-parity-1280x800.png` | `navRail` is icon-first, `mapStage` is visually dominant, right and bottom decks match §3.2 ratios | left rail shows task paragraph as primary UI; map looks secondary; side/bottom text overlaps chrome |
| 2 | map stage | same screenshot plus bounds overlay metadata | `ui.shell.map_stage.backdrop` fills map-stage empty space before terrain; map terrain, actor, cursor, loot marker remain inside `mapStage` | map empty space is black procedural grid; map tiles draw behind right/bottom chrome; cursor exits stage; loot marker exits stage |
| 3 | nav rail | `ui-demo-new-nav-rail-crop.png` | five nav icons are visible: compass, bag, scroll, book, gear; selected state uses `ui.shell.nav_button.active` | any icon missing, text substitutes icon, selected state depends only on color |
| 4 | right panel | `ui-demo-new-right-panel-grid.png` | no ground loot section; equipment sockets are icon-first; inscriptions show `5..12` framed rows; backpack grid is `4x2` current page; operation hints stay in their section | ground loot section still appears; equipment/backpack is only a long text list; inscription text floats without row backing; operation hints overlap backpack |
| 5 | bottom deck | `ui-demo-new-bottom-deck-no-command-hints.png` | hero crest meets §3.2; hero card contains floor/HP/resource/attack/defense; action cards align to `PLAYER_ACTIVE_TALENT_SLOT_COUNT`; log starts directly after action deck with no duplicate command hint region | gauges sit on text baseline; command hints appear as a separate bottom region; log covers hotbar; independent stats card squeezes log |
| 6 | inventory/modal | `ui-demo-new-inventory-page-1.png`, `ui-demo-new-inventory-page-2.png` | modal/page evidence is clamped to `modalSafeBounds`; right panel grid remains readable; footer/log not occluded; pagination does not truncate item 9+ | modal uses window-center magic coordinate; modal covers nav rail and bottom log without clamp; pagination hides item 9+ |
| 7 | main menu | `dark-uiux-pr02-1-demo-main-menu.png` | title zone, primary actions, footer help and focus state fit §7 geometry | page still looks like debug text menu; footer/help text exits chrome content |
| 8 | validation setup | `dark-uiux-pr02-1-demo-validation-setup.png` | list row height/gap and detail panel match §7; footer help stays inside content slot | scenario description overlaps footer; selection/focus state unclear |

Manual record minimum template:

```yaml
manualReviewers:
  - id: "<owner-reviewer-id>"
    role: "owner"
    verdict: "pass | fail"
  - id: "<non-owner-reviewer-id>"
    role: "non-owner"
    verdict: "pass | fail"
reviewedAt: "YYYY-MM-DDTHH:MM:SSZ"
demoParityVerdict: "pass | fail"
blockingFindings: []
screenshotLabelCoverage:
  required:
    - label: "ui-demo-new-parity-1672x941"
      sources: ["golden", "packaged"]
    - label: "ui-demo-new-parity-1280x800"
      sources: ["golden", "packaged"]
    - label: "ui-demo-new-right-panel-grid"
      sources: ["golden", "packaged"]
    - label: "ui-demo-new-bottom-deck-no-command-hints"
      sources: ["golden", "packaged"]
    - label: "ui-demo-new-inventory-page-1"
      sources: ["golden", "packaged"]
    - label: "ui-demo-new-inventory-page-2"
      sources: ["golden", "packaged"]
    - label: "ui-demo-new-nav-rail-crop"
      sources: ["golden", "packaged"]
    - label: "ui-demo-new-map-stage-crop"
      sources: ["golden", "packaged"]
  missing: [] # if non-empty, each entry must be {label, source, reason}
r01b:
  status: "USED"
  rawSheet: "assets-src/image/raw/sheets/dark-v1/r01b-ui-shell-chrome.png"
  contactSheet: "assets-src/image/contact-sheets/dark-v1/r01b-ui-shell-chrome-contact.png"
  spriteMapReport: "assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl"
  rawSheetHash: "<sha256-from-sprite-map-report>"
  coverageReport: "build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-1-owner-scope.json"
goldenLabels:
  - label: "ui-demo-new-parity-1672x941"
    hashDriftReason: "ui-demo-new-visual-parity"
  - label: "ui-demo-new-parity-1280x800"
    hashDriftReason: "ui-demo-new-visual-parity"
  - label: "ui-demo-new-right-panel-grid"
    hashDriftReason: "ui-demo-new-visual-parity"
  - label: "ui-demo-new-bottom-deck-no-command-hints"
    hashDriftReason: "ui-demo-new-visual-parity"
  - label: "ui-demo-new-inventory-page-1"
    hashDriftReason: "ui-demo-new-pagination"
  - label: "ui-demo-new-inventory-page-2"
    hashDriftReason: "ui-demo-new-pagination"
screenshots:
  - label: "ui-demo-new-parity-1672x941"
    source: "golden"
    path: "client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-parity-1672x941"
    source: "packaged"
    path: "build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-parity-1672x941.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-parity-1280x800"
    source: "golden"
    path: "client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1280x800.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-parity-1280x800"
    source: "packaged"
    path: "build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-parity-1280x800.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-right-panel-grid"
    source: "golden"
    path: "client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-right-panel-grid"
    source: "packaged"
    path: "build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-right-panel-grid.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-bottom-deck-no-command-hints"
    source: "golden"
    path: "client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-bottom-deck-no-command-hints"
    source: "packaged"
    path: "build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-bottom-deck-no-command-hints.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-inventory-page-1"
    source: "golden"
    path: "client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-1.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-inventory-page-2"
    source: "golden"
    path: "client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-2.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-nav-rail-crop"
    source: "packaged"
    path: "build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-nav-rail-crop.png"
    sha256: "<sha256>"
  - label: "ui-demo-new-map-stage-crop"
    source: "packaged"
    path: "build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/ui-demo-new-map-stage-crop.png"
    sha256: "<sha256>"
demoDeltaChecklist:
  leftIconRail: "pass | fail"
  dominantCenterMapStage: "pass | fail"
  mapStageBackdrop: "pass | fail"
  noRightGroundLootSection: "pass | fail"
  rightEquipmentSockets: "pass | fail"
  rightInscriptionRows5To12: "pass | fail"
  backpackGrid: "pass | fail"
  backpackPagination: "pass | fail"
  rightOperationHints: "pass | fail"
  bottomHeroCard: "pass | fail"
  bottomActionDeck: "pass | fail"
  bottomCommandHintsRemoved: "pass | fail"
  logDeck: "pass | fail"
remainingGaps:
  pr03: "final item/equipment/shop icon quality"
  pr05: "broader final terrain/actor/prop/fog/light art quality beyond UI-demo-new first screen"
  pr06: "final skill/status/quest icon quality"
  pr07: "full packaged all-screen audit and final polish"
outOfScopeReductions: []
```

同一 PR-02-1 label 的 `:client:goldenScreenshot` 连续失败两次时，停止截图重跑，先补齐 `DemoShellLayoutTest` / `TileRendererCanvasTest` 中缺失的数值断言与 bounds 断言，再重新捕获截图。

## 11. 非目标

1. 不生成 PR-03 item/equipment/material/affix 资源。
2. 不生成 PR-05 全量 tile/prop/actor/portrait/VFX 资源；本轮只刷新既有 PR-02-2 `tileset.ruins.*`、`actor.vanguard`、`prop.stairs.down` demo cell 用于 UI-demo-new 首屏验收。
3. 不生成 PR-06 skill/status/quest/profession/tree/fallback 资源。
4. 不修改 item stats、loot budget、shop price、drop rules、combat rules、AI、save/replay/profile schema。
5. 不引入 atlas / region manifest schema。
6. 不把 demo image 当 runtime asset；`UI/UI-demo-new.png` 只作为人工和文档验收参照。
7. 不删除 accessibility text；辅助文本必须保留在 tooltip 与 compact help slot 中，玩家主视觉不能继续是 text-first shell。screen reader hint 留给 PR-07 final polish 实施，PR-02-1 不引入新的 accessibility API。

## 12. PR 描述要求

PR 描述必须引用：

1. `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`
2. `client/build/reports/golden/` 中 PR-02-1 labels
3. `build/verification/verify-changed/full-task-duration-summary.{json,md}`
4. `build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-1-owner-scope.json`
5. `darkManifestCoveragePr02OwnerScope` 的 output rename：`dark-v1-manifest-coverage.json` 仅保留给裸 lint，PR-02 owner-scope output 迁移为 `dark-v1-manifest-coverage-pr02-owner-scope.json`

PR 描述必须明确剩余差异：

| remaining gap | owner |
| --- | --- |
| final item/equipment/shop icon quality | PR-03 |
| broader final map tile/actor/fog/light art quality beyond UI-demo-new first screen | PR-05 |
| final skill/status/quest icon quality | PR-06 |
| full packaged all-screen audit and final polish | PR-07 |

若出现 `outOfScopeReductions`，必须逐条写明 commit / evidence / why-not-owner-change；否则写 `outOfScopeReductions: N/A`。

不得写“整体接近 demo”这种空话；必须列出 demo-delta checklist 的实际状态。

## 13. 回滚边界

本 PR 必须可通过单 commit 完整回退，回退后恢复 PR-02 末态的 text-safe chrome shell，不影响 PR-02 已生成的 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons` 三张 sheet。

回滚范围：

1. `DemoShellLayout.kt`、`GameShellLayout.kt`、`InfoSurfaceLayout.kt`、`TileRenderer.kt`、`TileRenderModel.kt`、`FoundationGameScreen.kt`、`MainMenuScreen.kt` 的 PR-02-1 shell foundation 改动一起回退。
2. `GoldenScreenshotHarnessTest`、`DemoShellLayoutTest`、`TileRendererCanvasTest` 中 PR-02-1 labels / assertions 一起回退。
3. 必须同时移除 `r01b-ui-shell-chrome` owner contract、sheet-plan/key-registry entries、raw sheet、contact sheet、sliced runtime PNG、canonical/runtime manifest entries、sprite map report、coverage artifact。
4. 回滚不能删除或修改 PR-02 owner keys，也不能把 PR-02-1 fallback 留在 runtime manifest 中。
5. 禁止通过长期 feature flag 临时关闭 demo shell 路径；紧急降级必须采用单 commit 回退本 PR，并在 PR 描述中列出 follow-up 与重新启用条件，且 `maintainabilityLint` 不能新增 temporary path finding。
6. 回滚删除 `r01b-ui-shell-chrome` 条目后必须重新运行 `syncPhase2Manifests`，确保 canonical manifest 与 runtime manifest 同步移除 PR-02-1 keys。
7. 若回滚本 PR，`tools/build.gradle.kts` 中 `darkManifestCoveragePr02OwnerScope.reportFileName` 同步回退到默认 `dark-v1-manifest-coverage.json`；PR-02 历史 artifact 不必恢复，owner-scope task 重新生成时即覆盖。

回滚后必须执行以下扫描并确认无输出：

```bash
rg "ui\\.shell\\.|r01b-ui-shell-chrome|PR-02-1|dark-uiux-pr02-1-demo-shell-foundation" \
  UI/sprite-sheets assets-src/image client/src/main/resources \
  client/src/main/kotlin client/src/test/kotlin \
  tools/src/main tools/src/test \
  game/src/main game/src/test
```

`docs/`、`UI/review/` 与已归档 manual record 中的 historical references 保留，不计入回滚扫描；若需要清理历史引用，必须使用独立 follow-up commit。
