# Dark UI/UX PR-08 D10 Retained UI And Map-Stage Authority Optimization

**阶段**: `dark-uiux-pr08-d10-retained-ui-map-stage-authority`
**优先级**: `P1`
**工作量**: `XL`
**状态**: `pr-level-execution-contract`
**输入来源**: PR-08 current evidence chain、D9 / D10 map-stage evidence、当前 client UI 代码、libGDX Scene2D retained UI 体系、KTX Scene2D DSL、Skin Composer / TexturePacker、gdx-skins、VisUI、Unciv / Mindustry / Shattered Pixel Dungeon reference audit。
**文档权威**: 本文件是 `UI/pr` 下的 PR-08 D10 retained UI / map-stage authority 执行合同。它不替代 `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md` 的 PR-08 总目标，而是将 PR-08 的 UI/UX 修复路线从 renderer micro-polish 提升到 retained UI / theme authority。

## 0. 开发治理与验收矩阵

本 D10 包继承 `UI/pr/development-governance.md` 的 Acceptance Matrix、Gate Ladder、Visual Convergence Gate、canonical artifact 和失败规则，并受 `docs/verification/README.md` 与 `docs/rule/ai-change-governance.md` 的 verification / anti-bloat 纪律约束。它不修改 PR-08 根目标：map-stage、shell chrome、right panel、bottom HUD 达到 director-grade first-read quality，同时保持 gameplay、save、replay、profile、manifest schema 不变。

### 0.1 Precheck Summary

命中的上游入口：

1. `docs/INDEX.md`: dark UI/UX 当前入口走 `UI/PLAN.md`、`UI/pr/README.md` 与 PR-08 文档。
2. `UI/pr/README.md`: PR-08 是 PR-07 之后的 director-grade map-stage / shell / panel reset，必须遵守 `development-governance.md`。
3. `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md`: PR-08 不改 gameplay、save、replay、profile、manifest schema；资源生成必须走 owner coverage、manifest 与白盒证据。
4. `UI/pr/development-governance.md`: PR 级文档必须包含 Acceptance Matrix、Gate Budget、Canonical Artifacts、Failure Rule。
5. 当前目标文件旧版 D10: 聚焦 topology-risk source-band / movement blur / map-stage 微调，但没有把 UI authority 上移到 retained UI / theme 体系。

当前代码证据：

1. 生产代码没有使用 `com.badlogic.gdx.scenes.scene2d` / `Stage` / `Skin` / `Table` 主线。
2. `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` 定义 `TileCanvas`，并通过大量 `drawRect` / `drawAsset` / `drawText` 承担 map、shell、panel、HUD、overlay 绘制。
3. `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt` 继续用 `TileCanvas` 手写 shell chrome、right panel、bottom deck。
4. `client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt`、`MainMenuScreen.kt`、`ValidationSetupScreen.kt`、`VictoryScreen.kt`、`GameOverScreen.kt`、`UiErrorScreen.kt` 维护独立屏幕布局和文本绘制。
5. `client/build.gradle.kts` 已依赖 `com.badlogicgames.gdx:gdx`，因此 libGDX 官方 Scene2D / Scene2D UI 类型可直接使用；KTX Scene2D DSL 不是当前依赖，若引入必须作为依赖变更单独验证。

本 PR 的核心判断：

```text
D10 根因不是某个 map-stage PNG 或 alpha 没调好，
而是 K-ToME 当前绕过了 libGDX retained UI / Skin / Table 体系，
在 Kotlin immediate canvas 路径里手写了过多 UI、主题和视觉构图。
```

最佳方案：

```text
Scene2D Stage + Skin + Table 接管 UI tree、layout、theme、focus、modal、tooltip；
TileRenderer 继续只负责地图、tile、actor、fog、telegraph 和少量 map-stage 特效；
MapStageActor 把 TileRenderer 嵌入 Stage / Table 布局，不把地图 tile 全量改造成 Scene2D Actor。
```

受影响 owner：

| Owner | Scope |
| --- | --- |
| `docs` | D10 retained UI authority decision、PR-08 goal/log/review packet、manual record |
| `client` | Scene2D host、Skin bridge、root Table layout、screen presenter、focus graph、map-stage Actor adapter |
| `assets` | Skin drawable / nine-patch / atlas 只作为 manifest 消费产物，不成为资源 truth |
| `tools` | acceptance lint、focused client tests、golden screenshot、packaged whitebox、resource pipeline gate |

硬边界：

1. 不改 `core` / `game` 规则，不改 save、replay、profile、combat、visibility、pathing、content-pack。
2. 不新增 manifest schema、atlas schema、save schema、snapshot schema，除非另拆 public contract PR。
3. 不把 Scene2D Actor tree、Skin、Skin JSON、atlas、Table layout 变成 gameplay、visibility、targeting、telegraph 或 resource lookup authority。
4. 不引入 Arc / Mindustry 框架，不把 VisUI 作为长期 UI 系统依赖。
5. 不把 map tile、actor、fog、telegraph 拆成每格 Actor；map 仍由 `TileRenderer` 一次性渲染。
6. 不用保留旧 immediate UI 和新 Scene2D UI 两套长期真源；每个 screen 迁移完成后只能有一个 runtime route。
7. 不把本机绝对路径写进 docs、manifest、fixture、report、manual record 或 evidence index。

### Acceptance Matrix (0.2)

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI08-D10-R00` | §0 / §1 | `docs` | heading / hygiene scan | `acceptanceContractLint` | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | `N/A` |
| `UI08-D10-R01` | §2 | `docs` / `client` | reference decision table present | doc review + dependency policy check | `UI/review/dark-uiux-pr08-evidence-and-direction.md` | `N/A` |
| `UI08-D10-R02` | §3 | `client` | `KtomeUiStage` builds Stage root without rendering map | focused Scene2D host test | `client/src/main/kotlin/com/ktome/client/ui/scene2d/` | `N/A` |
| `UI08-D10-R03` | §3.2 / §5 | `client` / `assets` | `KtomeSceneSkin` consumes resolver-backed drawables | focused Skin bridge test + resource lint when assets change | `client/src/main/kotlin/com/ktome/client/ui/scene2d/KtomeSceneSkin.kt` | `N/A` |
| `UI08-D10-R04` | §3.3 / §5 | `client` | `MapStageActor` keeps stable bounds and delegates to `TileRenderer` | focused map actor test + existing map renderer tests | `client/src/main/kotlin/com/ktome/client/ui/scene2d/MapStageActor.kt` | `required` |
| `UI08-D10-R05` | §4.1 | `client` | main menu / validation / outcome screens have actor-tree tests | `:client:test` focused screen suite | `client/src/test/kotlin/com/ktome/client/ui/scene2d/` | `required` |
| `UI08-D10-R06` | §4.2 | `client` | in-game shell has one root Table and no overlapping major surfaces | `:client:test` layout suite + golden screenshot | `client/build/reports/golden/dark-uiux-pr08-d10-retained-shell/` | `required` |
| `UI08-D10-R07` | §3.4 / §5.5 | `client` | keyboard traversal covers primary flows | focused focus graph test + smoke | `client/src/test/kotlin/com/ktome/client/ui/scene2d/KtomeUiFocusGraphTest.kt` | `required` |
| `UI08-D10-R08` | §6 | `client` / `tools` | old immediate route removed or explicitly retired per screen | maintainability lint + code scan | `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` | `N/A` |
| `UI08-D10-R09` | §7 | `client` / `tools` | packaged forest / mine / shadow crops refreshed after runtime UI migration | packaged whitebox | `build/whitebox/dark-uiux-pr08-director-*-retained-ui/` | `required` |
| `UI08-D10-R10` | §1.3 | `docs` / `client` / `assets` | `UI/UI-demo-new.png` parity decomposition exists | retained shell golden + resource gate when assets change | `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` | `required` |
| `UI08-D10-R11` | §2.5 / §5.5 | `client` / `tools` | framework adoption matrix is closed | dependency hygiene scan + bootstrap if dependency changes | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | `N/A` |
| `UI08-D10-R12` | §8 | `client` / `docs` | main menu, talent tree and inventory workbench adaptation plans exist | focused screen suites when implemented | `UI/pr/screen-coverage-matrix.md` | `required` |
| `UI08-D10-R13` | §4 | `client` / `tools` / `docs` | each D10-P* phase has task-level plan, phase gate and next-phase stop rule | focused tests + golden/whitebox per phase | `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` | `required` |

### Gate Budget (0.3)

D10 分三层推进。PR 文档和索引更新先做静态检查与 acceptance lint；Kotlin / resource / packaged runtime 改动按 owner gate 分层执行。

`D10-P0` 只允许作为 docs-only authority freeze 关闭：文档、索引、manual record 和 acceptance lint 纳管可以同包提交；renderer、resource、manifest、golden harness、packaged crop 变更必须拆成独立 PR-08 map-stage evidence packet，或等 `D10-P1+` retained UI implementation packet 明确接管。下面的 implementation / resource / packaged 命令组是后续 phase gate，不是 `D10-P0` closure evidence。

When `verifyChanged` is required by a runtime phase, the phase evidence must record the canonical duration artifact at `build/verification/verify-changed/full-task-duration-summary.{json,md}`.

Docs / PR contract checks:

```bash
rg -n '^#{1,3} ' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
rg -n 'T[O]DO|T[B]D|F[I]XME' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
rg -n '[ \t]+$' UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
git diff --check -- UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md UI/pr/README.md UI/pr/screen-coverage-matrix.md UI/goal UI/review
```

PR docs gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint --no-configuration-cache
git diff --check -- UI/pr UI/goal UI/review UI/fix
```

Retained UI implementation:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*" --no-configuration-cache
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

The full `:client:goldenScreenshot` task is a broad regression gate only. It does not prove that a D10 phase-specific evidence label exists. Before any runtime D10 phase closes, that implementation PR must add and run the matching focused tag or test listed below, and the manual record must name the generated artifact labels.

| Phase | Required focused evidence tag before closure | Labels it must prove |
| --- | --- | --- |
| `D10-P2` | `d10StandaloneScreens` | `dark-uiux-pr08-d10-main-menu-retained`, `dark-uiux-pr08-d10-validation-retained`, `dark-uiux-pr08-d10-outcome-retained` |
| `D10-P3` | `d10FocusModalTooltip` | focus traversal, modal blocking and tooltip hover evidence for migrated surfaces |
| `D10-P4` | `d10RetainedShell` | `dark-uiux-pr08-d10-retained-shell`, map actor bounds, right panel host and bottom deck host |
| `D10-P5` | `d10InventoryEquipment` | `dark-uiux-pr08-d10-right-panel-inventory-retained`, `dark-uiux-pr08-d10-inventory-workbench-retained`, `dark-uiux-pr08-d10-shop-retained` |
| `D10-P6` | `d10TalentTree` | `dark-uiux-pr08-d10-talent-tree-retained`, `dark-uiux-pr08-d10-active-slot-modal-retained` |
| `D10-P7` | `d10FrontstageOverlay` | `dark-uiux-pr08-d10-combat-decision-retained`, `dark-uiux-pr08-d10-frontstage-retained`, `dark-uiux-pr08-d10-bottom-deck-retained`, `dark-uiux-pr08-d10-nav-rail-retained` |

Resource / Skin asset implementation:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --no-configuration-cache
./gradlew resourcePipelineLint --no-configuration-cache
```

Dependency change implementation, only if KTX is accepted:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./scripts/verify-bootstrap.sh
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*" --no-configuration-cache
```

Packaged evidence:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-forest-retained-ui --no-configuration-cache
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-mine-retained-ui --no-configuration-cache
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-retained-ui --no-configuration-cache
./gradlew verifyChanged --no-configuration-cache
```

### Canonical Artifacts (0.4)

| Artifact family | Required repo-relative path |
| --- | --- |
| D10 PR-level design | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` |
| Target quality bar | `UI/UI-demo-new.png` |
| PR-08 root plan | `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md` |
| PR governance | `UI/pr/development-governance.md` |
| Goal entry after promotion | `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md` |
| Iteration log after promotion | `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md` |
| Direction review after promotion | `UI/review/dark-uiux-pr08-evidence-and-direction.md` |
| Retained UI manual record | `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` |
| Scene2D host package | `client/src/main/kotlin/com/ktome/client/ui/scene2d/` |
| Scene2D host tests | `client/src/test/kotlin/com/ktome/client/ui/scene2d/` |
| Retained shell golden | `client/build/reports/golden/dark-uiux-pr08-d10-retained-shell/` |
| Packaged whitebox | `build/whitebox/dark-uiux-pr08-director-*-retained-ui/` |

### Failure Rule (0.5)

1. 如果 UI layout、focus、theme 继续主要由 `TileCanvas` 手写绘制，本 D10 不得声明 authority optimization 完成。
2. 如果 Scene2D `Skin`、Skin JSON、atlas 或 Table layout 成为资源 key / raw path 的第二真源，必须 reject。
3. 如果 `MapStageActor` 复制 visibility、terrain、pathing、telegraph、tile selection 或 combat semantics，必须 reject。
4. 如果新 Stage 抢占移动、目标选择、Look/Inspect、技能释放输入，且没有 focus mode / input routing 测试，必须 reject。
5. 如果迁移后右面板、bottom deck、modal、tooltip 与 map-stage 出现重叠或文本溢出，必须 reject。
6. 如果 KTX、VisUI、Arc 或其他依赖被静默引入，必须退回；依赖变化需要单独说明、bootstrap 验证和 rollback。
7. 如果 packaged evidence 未刷新，不得用 debug golden 或 actor-tree test 宣称玩家可见 UI 完成。
8. 如果旧 immediate UI 与新 retained UI 长期并行，必须把其中一条标记为 removal / rollback route，不能留下第二套 runtime authority。
9. 如果 Scene2D migration 只产出 generic widgets、灰色默认皮肤或无 dark-v1 资源的控件，不能声明已经接近 `UI/UI-demo-new.png` 的 director-grade 质感。

## 1. Problem Statement

### 1.1 当前问题不是单点视觉失败

旧版 D10 把问题压在 map-stage topology-risk、source-band、full-plate projection 和 movement blur 上。这些问题是真实的，但它们更像表层症状。更深的根因是：

1. 屏幕、panel、button-like row、HUD、tooltip、modal、状态栏、right/bottom surface 都通过 `TileCanvas` immediate draw 路径构成。
2. 布局和 theme 分散在 renderer、layout solver、screen class、presenter 和测试 helper 中。
3. UI 的状态树、焦点、z-order、hit test、tooltip、modal stacking 没有统一 retained authority。
4. 视觉问题反复变成“继续加一层 rect / asset / alpha / clip”，而不是调整 UI 结构。

因此，D10 的 PR 级目标应从“继续修地图绘制技巧”改为“切出 retained UI authority”。地图仍然重要，但它应当成为 UI shell 中的一个稳定 map-stage 内容区，而不是继续承载整个 UI 框架。

### 1.2 成功标准

D10 完成后，K-ToME client 至少具备：

1. 一个长期可维护的 `Stage` lifecycle owner。
2. 一个 resolver-backed `Skin` / theme bridge。
3. 一个 root `Table` contract，承载 standalone screen 与 in-game shell。
4. 一个 `MapStageActor`，把 `TileRenderer` 嵌入 retained UI layout。
5. 一个 keyboard-first focus contract，不靠 renderer draw order 猜交互。
6. 一组 actor-tree / layout / golden / packaged evidence，能证明新 UI authority 没有破坏 PR-08 可读性。

### 1.3 Relationship To `UI/UI-demo-new.png`

`UI/UI-demo-new.png` 是质量标尺，不是 pixel-match target。它代表的目标不是“换一个 UI 框架后自动变好看”，而是以下能力同时成立：

| Target property from `UI/UI-demo-new.png` | Solved by retained UI? | Needs assets / renderer? | D10 responsibility |
| --- | --- | --- | --- |
| dominant map stage with stable panel boundaries | yes | map renderer still owns tile / fog / lighting | `MapStageActor` + root `Table` 固定 map/right/bottom/nav geometry |
| dense but organized right equipment / inscription / backpack panels | yes | icons, frame pieces, item art still need manifest assets | retained panel widgets、grid、selection、tooltip、focus |
| consistent dark metal / leather / gold frame language | partly | high-quality nine-patch / atlas / frame assets required | `KtomeSceneSkin` consumes manifest-backed dark-v1 drawables |
| readable typography and hierarchy | yes | fonts and localized text still need existing locale/font owner | label styles、spacing、truncate/wrap、screen tests |
| modal / tooltip / hover / selected state polish | yes | state drawables and icons need assets | Stage overlay layer、Skin styles、focus graph |
| map tile / actor / prop / lighting fidelity | no | `TileRenderer`、tile assets、actor sprites、fog/lighting pipeline | D10 must not break it; PR-08 asset work continues separately |
| final director-grade visual texture | not alone | art direction、generated/sliced resources、manifest、golden、packaged evidence | D10 makes texture assets reusable and enforceable, but does not replace the resource pipeline |

结论：

1. 引入 `Scene2D / Stage / Skin / Table` 能向 `UI/UI-demo-new.png` 迈出结构性大步，因为它把 demo 图里的主 shell、right panel、bottom deck、modal、tooltip、控件状态和焦点体系变成 retained UI tree。
2. 它不能单独解决“质感”本身。质感来自 dark-v1 panel frame、nine-patch、icon、tile、actor、lighting、fog、gold edge、material texture 这些资源与渲染结果。
3. 它解决的是“质感资源接不住、接不稳、每个屏幕手写一遍”的问题。没有 retained UI，即使有好图，也会继续被 immediate draw 路径切碎、错位、重叠、不可复用。
4. D10 的验收不能只看 actor-tree tests；必须看 retained shell golden 和 packaged crops，确认 Skin 消费 dark-v1 资源后，右面板、bottom deck、map-stage frame 的观感真的接近目标质量条。

### 1.4 What This Actually Solves

| Problem | Current symptom | Retained UI answer | Remaining work |
| --- | --- | --- | --- |
| layout authority | bounds、baseline、panel spacing 散在多个 renderer / layout helper | root `Table` 和 widget tree 统一 major surfaces | 迁移 input hit-test 到 Table-derived bounds |
| theme consistency | 每个 screen 手写 rect、颜色、字体、frame draw | `Skin` style key 统一 panel/button/label/tooltip/modal | 需要 dark-v1 drawable / nine-patch 资源 |
| interaction state | hover、selected、disabled、modal focus 靠 renderer 分支 | Scene2D widget state + `UiFocusGraph` | 键盘优先 flow 测试 |
| z-order / overlay | tooltip/modal/map marker 容易互相遮挡 | Stage actor order + overlay layer | packaged crop 验证 |
| reuse and scale | 主菜单、验证、结算、right panel、bottom deck 各画一套 | shared widget/table factories | staged removal of immediate routes |
| texture quality | 好资源无法稳定贴到每个控件和面板 | Skin-backed drawable consumption | 资源生成、切片、manifest、golden 仍是必要条件 |

### 1.5 明确非目标

1. 不在本 PR 重写 `TileRenderer` 的 tile / actor / fog / telegraph 规则。
2. 不把每个 map cell 变成 Actor。
3. 不引入完整 mod UI SDK、Lua runtime、Arc runtime 或 VisUI 长期主题依赖。
4. 不重做存档、replay、profile、content pack 或 manifest schema。
5. 不在本 PR 追求 all-map resource closure；map-stage art plate / topology-risk 的后续取舍可在 retained UI authority 成立后另拆。

## 2. External Reference Decision

### 2.1 直接采用

| Reference | Adopt | Reason |
| --- | --- | --- |
| libGDX Scene2D `Stage` | yes | 官方 retained scene graph、viewport、input dispatch、act / draw lifecycle，适合接管 screen / panel / modal |
| libGDX Scene2D UI `Skin` | yes | 官方 widget style 宿主，适合把 theme token、drawable、font、color 收口 |
| libGDX Scene2D UI `Table` | yes | 官方 layout 主线，能替代当前手写 bounds / baseline / overlap solver |
| libGDX `InputMultiplexer` | yes | Stage 与现有 map input 并存的最小风险路由 |
| TexturePacker / atlas workflow | yes as tool | 适合 Skin / drawable 产物，但必须通过 K-ToME canonical manifest / runtime manifest / resolver |

### 2.2 有条件采用

| Reference | Adopt | Boundary |
| --- | --- | --- |
| KTX Scene2D DSL | deferred | Kotlin DSL 能减少 boilerplate，但当前 K-ToME 已是 libGDX `1.14.0` / Kotlin `2.2.21`，而当前 KTX public docs / release examples仍以 `1.13.1-rc1` 一类版本线示例；D10-P1/P2 先不用 KTX |
| KTX style helpers | deferred | 可辅助 Skin style 构造；必须等 no-KTX host proof 后单独做 compatibility spike，不能成为资源 key、atlas path 或 theme token 的第二 authority |
| Skin Composer | tool only | 可用于人工编辑 / 预览 Skin；输出不能直接作为 runtime truth，必须同步到 manifest-backed Skin bridge |
| gdx-skins | prototype only | 只用于快速 spike 看 widget style，不允许提交为正式视觉来源 |

### 2.3 只借鉴，不采用依赖

| Reference | Use | Boundary |
| --- | --- | --- |
| VisUI | widget / skin discipline reference | 不作为长期依赖；K-ToME 需要暗黑 roguelike 定制 theme，不需要 VisUI 默认桌面风格 |
| Unciv | Kotlin + libGDX + Scene2D 大型项目组织参考 | 可借鉴 screen/table/skin 分层，不复制业务代码 |
| Mindustry | retained UI 架构与 game HUD 分层参考 | 使用 Arc 而非 libGDX Scene2D 原生主线，不引入 Arc |
| Shattered Pixel Dungeon | roguelike UI/UX 可读性参考 | 不是 Scene2D retained UI 模板，不作为架构依赖 |

### 2.4 明确拒绝

1. **继续手写 K-ToME 专属 UI framework**：这会扩大当前问题。
2. **把 VisUI / Arc / Mindustry UI runtime 接进项目**：依赖和风格边界过大。
3. **Skin JSON / atlas 直接引用 raw path**：违反 K-ToME manifest authority。
4. **地图全 Actor 化**：cell 数、fog、telegraph、targeting 和 deterministic renderer 复杂度都会失控。

### 2.5 Source-Checked Adoption Matrix

Source review date: `2026-06-03`.

Source references:

1. libGDX Scene2D UI: `https://libgdx.com/wiki/graphics/2d/scene2d/scene2d-ui`
2. libGDX Scene2D custom Actor: `https://libgdx.com/wiki/graphics/2d/scene2d/scene2d`
3. KTX project: `https://github.com/libktx/ktx`
4. Skin Composer tool docs: `https://libgdx.com/wiki/tools/skin-composer`
5. VisUI project: `https://github.com/kotcrab/vis-ui`
6. gdx-skins project: `https://github.com/czyzby/gdx-skins`

| Candidate | Source fact used | D10 decision | Development rule |
| --- | --- | --- | --- |
| libGDX Scene2D / Scene2D UI | official libGDX docs show `Stage`, `Skin`, root `Table`, `stage.act`, `stage.draw`, `resize` viewport update and custom `Actor.draw(Batch, parentAlpha)` as the intended retained UI path | adopt now | D10-P1/P2 must use official libGDX API first; no dependency change required |
| KTX `ktx-scene2d` | KTX docs expose type-safe Scene2D builders and Maven Central modules under `io.github.libktx`; example version is a KTX line that must be compatibility-checked against repo libGDX/Kotlin pins | defer | D10 may not add KTX in the first host/skin PR; only a later `D10-KTX-COMPAT` spike can add `ktx-scene2d` and `ktx-style` |
| KTX `ktx-style` | KTX docs describe Skin style builders | defer | Allowed only if `KtomeSceneSkin` boilerplate becomes large after S2 and compatibility spike passes |
| Skin Composer | libGDX tool documentation treats it as a Skin authoring tool; it is not a runtime framework | adopt as tool only | Use to preview nine-patch / style states; convert accepted output back into manifest-backed `KtomeSceneSkin` code |
| TexturePacker | libGDX toolchain supports atlas production | adopt as tool only | Atlas/nine-patch output must still pass key registry, canonical manifest, runtime manifest and resolver tests |
| gdx-skins | community sample skins are useful prototypes, not K-ToME dark-v1 resources | reject runtime | May be used locally for spike comparison; no gdx-skins file enters production resources |
| VisUI | project offers Scene2D widgets and a bundled skin, but its default UI identity and dependency surface are not K-ToME's dark-v1 style contract | reject runtime for D10 | Study widget patterns only; any future VisUI dependency requires separate PR and explicit reason |

### 2.6 KTX Compatibility Spike, Only If Needed

Default D10 implementation is `no-KTX`. If official Scene2D code becomes noisy enough to harm maintainability, a later compatibility spike can be proposed with this exact scope:

Allowed modules:

```kotlin
implementation("io.github.libktx:ktx-scene2d:<verified-compatible-version>")
implementation("io.github.libktx:ktx-style:<verified-compatible-version>")
```

Explicitly rejected modules for D10:

```text
ktx-app
ktx-assets-async
ktx-async
ktx-inject
ktx-vis
any broad KTX stack import
```

Spike rules:

1. Resolve a KTX version compatible with repo-pinned `libgdxVersion=1.14.0` and `kotlinVersion=2.2.21`.
2. Add a `ktxVersion` property only in the spike branch.
3. Run `./scripts/verify-bootstrap.sh`.
4. Run focused `KtomeUiStageTest`, `KtomeSceneSkinTest` and one migrated screen test both before and after DSL conversion.
5. Keep `KtomeRootTable` readable without DSL-specific magic; a libGDX developer must still be able to map widgets to Scene2D classes.
6. If binary/source compatibility is unclear, reject KTX and keep official API.

### 2.7 Skin Composer Development Guide

Skin Composer is useful only if it shortens asset/style iteration without becoming runtime truth.

Allowed workflow:

1. Create review-only Skin Composer experiments under a D10 evidence packet, for example:

```text
UI/review/dark-uiux-pr08-exploration/d10-retained-ui-skin-authoring-<date>/
```

2. Use it to preview:
   - panel body / raised panel / modal panel nine-patches;
   - button up/down/checked/disabled states;
   - slot empty/equipped/selected states;
   - tooltip and focus-ring states.
3. Copy only the accepted design decision into `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md`.
4. Promote runtime assets through:

```text
key registry -> sheet plan -> canonical manifest -> runtime manifest -> VisualResolver -> KtomeSceneSkin
```

5. Add `KtomeSceneSkinTest` assertions for every required style key.

Rejected workflow:

1. Loading Skin Composer exported JSON directly in runtime.
2. Letting `.atlas` or `.json` files contain unreviewed raw paths as production truth.
3. Treating a Skin Composer project file as the manifest key registry.
4. Accepting a style because it looks correct in Skin Composer without packaged app crop evidence.

### 2.8 VisUI And gdx-skins Boundaries

VisUI and gdx-skins can be useful as reading material because they show practical Scene2D widget/skin conventions. They are not adopted by D10.

Rejected D10 dependency shape:

```kotlin
implementation("com.kotcrab.vis:vis-ui:<version>")
implementation("<gdx-skins-artifact-or-copied-skin>")
```

Reason:

1. K-ToME needs `UI/UI-demo-new.png` dark-fantasy density, not a generic desktop widget look.
2. VisUI introduces its own widget and skin expectations; D10 already has enough risk with Stage/Skin/Table migration.
3. gdx-skins assets are sample/prototype resources and would violate PR-08 owner / dark-v1 manifest authority if copied into runtime.

Allowed use:

1. Inspect naming and style-state patterns.
2. Compare focus/disabled/checked states during local spike.
3. Borrow no production asset, no package, no runtime API.

## 3. Target Architecture

### 3.1 分层结构

```text
GameApp / Screen
  |
  | owns
  v
KtomeUiStage
  |
  | builds
  v
KtomeRootTable
  |
  | contains
  +-- StandaloneScreenTable
  +-- GameShellTable
        |
        +-- NavRail / RightPanel / BottomDeck / ModalLayer / TooltipLayer
        +-- MapStageActor
              |
              +-- TileRenderer.render(...)
```

Authority split：

| Layer | Owns | Must not own |
| --- | --- | --- |
| `KtomeUiStage` | Stage lifecycle、viewport、input routing、act/draw order | game rules、resource truth、snapshot mutation |
| `KtomeSceneSkin` | widget styles、colors、fonts、drawable construction | raw asset paths、manifest inventory、game semantics |
| `KtomeRootTable` | retained layout、major surfaces、modal/tooltip stacking | tile rendering、visibility、combat state |
| `KtomeScreenPresenter` | snapshot / app state -> UI view model | locale bundle truth、save/profile state |
| `MapStageActor` | actor bounds、clip、delegation to map renderer | tile semantics、topology classifier、resource resolver policy |
| `TileRenderer` | map rendering、actors、fog、telegraph、map-stage effects | standalone screens、right panel、bottom deck、modal UI |

### 3.2 Skin / Theme Authority

`Skin` 是 UI 表现的消费层，不是资源 truth。正式资源链仍然是：

```text
key registry / sheet plan
  -> canonical visual manifest
  -> runtime visual manifest
  -> VisualResolver
  -> KtomeSceneSkin drawable factory
  -> Scene2D widget style
```

Rules：

1. `KtomeSceneSkin` 只接收 `ResolvedVisualAsset`、font handle、color token、spacing token。
2. `Skin` style name 使用稳定 semantic key，例如 `ktome.panel.dark`, `ktome.button.primary`, `ktome.tooltip`.
3. Skin / Table 不保存 `rawOutputPath`、filesystem path、manifest key inventory mirror。
4. Skin Composer 输出只能作为 authoring artifact，经 manifest / resolver / test 后才能进入 runtime。
5. 如果使用 atlas / nine-patch，atlas entry 仍必须能从 manifest key 追溯。

### 3.3 MapStageActor Boundary

`MapStageActor` 是保守适配层：

1. 它是 Scene2D `Actor` 或 `Widget`，拥有 bounds、clip、hit area。
2. 它内部调用现有 `TileRenderer`，传入 `SpriteBatch`、resolver、snapshot、overlay state、cell size、map-stage bounds。
3. 它不解析 terrain、AI、visibility、loot、telegraph、pathing。
4. 它不维护地图视觉 key inventory。
5. 它只向 Stage 暴露高层 input handoff：map click / hover / focus request。

Map rendering stays immediate inside the actor. UI layout becomes retained outside the actor.

### 3.4 Input And Focus

需要一个明确的输入合同：

1. `Stage` 先接收 UI input，用于 button、select row、modal、tooltip、text focus。
2. 未被 UI 消费的 map movement / targeting / Look/Inspect 输入交给现有 `InputHandler`。
3. Focus state 使用 `UiFocusGraph` 或同等薄模型，而不是从 draw order 反推。
4. modal 打开时，map movement 必须被阻断或显式降级。
5. map-stage focus 下，键盘移动仍走现有 gameplay input，不通过 Scene2D button 模拟。

### 3.5 Rendering Order

目标顺序：

```text
Stage background / shell chrome
  -> MapStageActor background and TileRenderer map
  -> retained right panel / bottom deck / nav rail
  -> retained tooltip / modal / overlay controls
```

如果 Stage 与 `TileRenderer` 共享 `SpriteBatch`，必须保证 begin/end lifecycle 单一 owner。推荐在 `KtomeUiStage.draw()` 内部统一调度，避免 `FoundationGameScreen` 和 `TileRenderer` 各自拥有 batch lifecycle。

### 3.6 Long-Term Refactor Findings

当前代码排查结论支持一次大改，而不是继续做兼容式小修：

| Area | Current evidence | Long-term decision |
| --- | --- | --- |
| Scene2D usage | production code has no `Stage` / `Skin` / `Table` runtime route | build one official retained UI route; do not keep the immediate UI route as equal authority |
| Standalone screens | `MainMenuScreen`, `ValidationSetupScreen`, `VictoryScreen`, `GameOverScreen`, `UiErrorScreen` draw with `SpriteBatch` and `DarkStandaloneScreenLayout` | migrate all standalone screens in one phase family, then retire standalone immediate layout |
| In-run shell | `DemoShellRenderer.kt` is a large `TileCanvas` shell/panel renderer with many handwritten draw paths | replace shell, nav, right panel, bottom deck, modal and tooltip with Scene2D tables/widgets |
| Map renderer | `TileRenderer.kt` is still the correct owner for map, actor, fog, telegraph and room-art layers | keep it, but confine it behind `MapStageActor` and remove non-map UI responsibility over time |
| Input / hit-test | `InputHandler.kt` still resolves pointer/focus against `DemoShellLayoutSolver`, inventory grid solvers and talent layout bounds | introduce retained hit-test/focus adapters; stop deriving UI input from old draw-layout solvers |
| Inventory / equipment | presenter contracts already exist, but dense UI is still rendered through Tile model / renderer paths | preserve presenters, replace drawing/layout with retained widgets |
| Talent tree | `TalentAssignPanelModel` is a usable view model, but `TalentAssignPanelLayoutSolver` and renderer draw paths still own final layout | preserve model, replace layout/rendering with retained tree widgets and Stage modal |
| Tooltip / modal | `ModalStack`, `PaneFocusController`, `ModalCardModel` are good semantic state, but visual layout is immediate | keep semantic state, move visual ownership to Stage overlay and modal layer |

Refactor policy:

1. Do not add compatibility flags for old/new UI. Each migrated surface must have one production route.
2. A temporary adapter is allowed only inside the phase that consumes it and must have a named removal task in the same or next phase.
3. No new player-visible UI may be added to `TileCanvas`, `DemoShellRenderer`, `StandaloneScreenLayout`, `TalentAssignPanelLayoutSolver` or `InventoryWorkbenchLayoutSolver` after D10-P1 starts, unless it belongs to the map renderer.
4. `TileRenderer` may keep map-stage rendering, room-art presentation and telegraph overlays; it should not own standalone screens, shell panels, dense inventory, talent tree, modal or tooltip chrome after the corresponding phase exits.
5. If a phase cannot delete or disable the old route, it must fail the phase gate rather than silently proceed to the next phase.

## 4. Phase-Level Big Refactor Plan

This section is the execution plan for the bold D10 route. It intentionally plans large UI cuts because long-term stability is worse if the project keeps both the retained UI system and the handwritten immediate UI system.

Phase progression rule:

1. Phases must be executed in order unless a later phase is explicitly split into a new PR with its own acceptance matrix.
2. Each phase must end in a runnable app, focused automated tests, golden or actor-tree evidence where applicable, packaged whitebox when player-visible runtime surfaces changed, and a manual record update.
3. The next phase may not start while any required gate from the previous phase is failing.
4. The old route for a migrated surface must be removed, disabled or quarantined with a documented rollback owner before the phase is considered complete.
5. Compatibility with old UI routes is not a product goal. Rollback is allowed; long-term dual authority is not.

### 4.1 D10-P0: Authority Freeze And Cutover Inventory

Goal: freeze the migration boundary before touching Kotlin. This phase defines exactly what will be replaced and what must not receive new immediate UI work.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P0-T1` | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | record D10 as phase-level retained UI authority and no-long-term-compatibility route | this section exists and names phase gates |
| `D10-P0-T2` | `UI/pr/screen-coverage-matrix.md` | ensure all affected screens have D10 evidence labels: main menu, validation, outcome, shell, inventory, talent, active slot modal | every D10 surface has a required label |
| `D10-P0-T3` | `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md` | record that further PR-08 UI work should not add new `TileCanvas` UI outside map rendering | future prompt contract points to D10 |
| `D10-P0-T4` | `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` | add phase checklist and current route inventory | manual record names old routes and target retained owners |
| `D10-P0-T5` | `tools` acceptance lint owner | register D10 and add retained-UI doc checks: phase headings, manual record, phase transition checklist, placeholder-free bash gates and no numeric `D10-S*` execution labels | `acceptanceContractLint` fails if any D10 doc check drifts |

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint --no-configuration-cache
git diff --check -- UI/pr UI/goal UI/review UI/manual-records
```

Whitebox evidence:

1. No runtime whitebox required for docs-only P0.
2. Manual record must state that runtime migration has not started.
3. Runtime renderer / resource / manifest / golden changes are not valid P0 closure evidence; split them out or reclassify the packet as a later implementation phase.

Do not enter P1 if:

1. any D10 screen lacks an evidence label;
2. the document still describes old/new UI as long-term compatible routes;
3. D10 is not visible from `UI/PLAN.md` and `UI/pr/README.md`.
4. current PR/worktree closure still mixes P0 docs-only evidence with renderer/resource/golden runtime implementation changes.

### 4.2 D10-P1: Scene2D Kernel, Skin Authority And Input Boundary

Goal: create the retained UI kernel that all later phases consume. This phase is a hard dependency for every screen migration.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P1-T1` | `client/src/main/kotlin/com/ktome/client/ui/scene2d/KtomeUiStage.kt` | create Stage lifecycle owner: viewport, root install, act/draw, resize, dispose | viewport resize test passes |
| `D10-P1-T2` | `client/src/main/kotlin/com/ktome/client/ui/scene2d/KtomeRootTable.kt` | create root table factories for standalone and game shell routes | root table fills viewport and exposes stable bounds |
| `D10-P1-T3` | `client/src/main/kotlin/com/ktome/client/ui/scene2d/KtomeSceneSkin.kt` | build Skin styles from resolver-backed assets, fonts and tokens | required style keys are present; no raw path |
| `D10-P1-T4` | `client/src/main/kotlin/com/ktome/client/ui/scene2d/KtomeInputRouter.kt` | define Stage-first input routing with fallback to map/game input | consumed UI input does not leak to movement |
| `D10-P1-T5` | `client/src/test/kotlin/com/ktome/client/ui/scene2d/` | add kernel, skin and input-boundary tests | focused suite passes |
| `D10-P1-T6` | `client/build.gradle.kts` | keep dependency set unchanged; no KTX in P1 | dependency diff is empty |

Implementation notes:

1. Use official libGDX `Stage`, `Skin`, `Table`, `Actor`, `Widget` APIs directly.
2. Do not introduce KTX in P1. If boilerplate is painful, record it as a P1 finding and handle it after P8 or in a separate compatibility PR.
3. Do not load Skin JSON as source truth. `KtomeSceneSkin` should construct styles from manifest/resolver-owned assets.
4. `KtomeUiStage` is the draw lifecycle owner for retained UI; avoid nested `batch.begin()` / `batch.end()` ownership.

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*" --no-configuration-cache
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Whitebox evidence:

1. actor-tree dump for empty standalone root;
2. actor-tree dump for empty game shell root;
3. skin style key report with resolver-backed drawable provenance.

Do not enter P2 if:

1. Stage route cannot be constructed in tests;
2. Skin requires raw file paths;
3. Stage consumes gameplay movement without an explicit fallback test;
4. KTX or VisUI appears in dependency diff.

### 4.3 D10-P2: Standalone Screens Hard Cut

Goal: migrate all non-map standalone screens to retained UI and retire the old standalone immediate drawing route.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P2-T1` | `client/src/main/kotlin/com/ktome/client/ui/scene2d/StandaloneScreenTable.kt` | create shared retained standalone scaffold: header, body, action row, footer help | layout test covers min and 1280x800 viewport |
| `D10-P2-T2` | `UiErrorScreen.kt` | replace `SpriteBatch` draw path with retained table | error title/detail/actions visible in actor tree |
| `D10-P2-T3` | `VictoryScreen.kt`, `GameOverScreen.kt` | migrate outcome screens through `OutcomeSummaryPresenter` | presenter tests remain green; outcome actor-tree passes |
| `D10-P2-T4` | `ValidationSetupScreen.kt` | migrate validation list and action rows | validation controller tests stay green; keyboard flow passes |
| `D10-P2-T5` | `MainMenuScreen.kt`, `PlayerCreationPanel.kt` | migrate home screen, player creation selectors, continue state, validation entry and language toggle | main menu text/focus tests stay green; no text overlap |
| `D10-P2-T6` | `StandaloneScreenLayout.kt`, `StandaloneScreenChrome` | remove or quarantine old standalone layout/chrome after all screens migrate | no production screen imports old standalone layout |
| `D10-P2-T7` | `client/src/test/kotlin/com/ktome/client/screen/` and `ui/scene2d/` | convert old layout assertions into actor-tree and text constraints | old tests either pass or are replaced by equivalent retained tests |

Break compatibility rule:

1. Once a standalone screen is migrated, do not keep its `SpriteBatch` renderer active behind a runtime switch.
2. `renderEnabled=false` test paths may remain only as construction helpers if they do not define a second production route.

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.screen.MainMenuControllerTest --tests com.ktome.client.screen.MainMenuScreenTextTest --tests com.ktome.client.screen.ValidationSetupControllerTest --tests com.ktome.client.screen.OutcomeSummaryPresenterTest --no-configuration-cache
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*Standalone*" --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Phase-specific closure gate: add and run focused evidence tag `d10StandaloneScreens`. Full golden passing alone is not D10-P2 closure evidence.

Whitebox evidence:

1. `dark-uiux-pr08-d10-main-menu-retained`;
2. `dark-uiux-pr08-d10-validation-retained`;
3. `dark-uiux-pr08-d10-outcome-retained`;
4. packaged app launch screenshot after main menu route switches.

Do not enter P3 if:

1. any standalone screen still uses `DarkStandaloneScreenLayout` in production;
2. locale text overflows or is pre-truncated before entering Scene2D labels;
3. main menu keyboard flow cannot reach continue, quick start, validation, locale toggle and exit.

### 4.4 D10-P3: Unified Focus, Modal, Tooltip And Hit-Test Model

Goal: stop deriving UI interaction from draw-layout math. Stage owns UI hit-test and focus; existing semantic state remains the model source.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P3-T1` | `KtomeUiFocusGraph.kt` | create focus scopes for standalone, shell, panel and modal | keyboard traversal tests pass |
| `D10-P3-T2` | `KtomeModalLayer.kt` | render `ModalStack` / `ModalCardModel` through Stage modal layer | modal actor tree matches semantic state |
| `D10-P3-T3` | `KtomeTooltipLayer.kt` | move tooltip layout to Stage overlay; consume tooltip view models | tooltip is not drawn by map renderer for migrated surfaces |
| `D10-P3-T4` | `InputHandler.kt` integration boundary | create retained hit-test adapter so UI hover/click can update semantic state without `DemoShellLayoutSolver` | pointer tests use actor bounds |
| `D10-P3-T5` | `PaneFocusController`, `ModalStack` | preserve semantic state but remove visual ownership assumptions | existing unit tests stay green |
| `D10-P3-T6` | tests | add keyboard-only and mouse hover regression coverage | Stage modal blocks map movement |

Break compatibility rule:

1. Do not keep both retained hit-test and old solver hit-test for the same migrated surface.
2. Old `TileTooltipPlacementSolver` may remain for map-only tooltip until map overlay migration, but not for retained panels.

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.ui.layout.ModalStackTest --tests com.ktome.client.ui.layout.PaneFocusControllerTest --no-configuration-cache
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*Focus*" --tests "com.ktome.client.ui.scene2d.*Modal*" --tests "com.ktome.client.ui.scene2d.*Tooltip*" --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
```

Phase-specific closure gate: add and run focused evidence tag `d10FocusModalTooltip`. Full golden passing alone is not D10-P3 closure evidence.

Whitebox evidence:

1. keyboard-only main menu to validation to game shell smoke;
2. modal-open map movement blocking log;
3. tooltip hover evidence for equipment, inventory and map focus where migrated.

Do not enter P4 if:

1. any migrated UI click still depends on old draw-layout bounds;
2. modal focus cannot restore previous focus;
3. hover changes committed gameplay selection unexpectedly.

### 4.5 D10-P4: In-Run Shell Hard Cut And MapStageActor

Goal: replace the handwritten shell framework with retained layout, while keeping `TileRenderer` as the map authority.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P4-T1` | `GameShellTable.kt` | create retained root shell with nav rail, map stage, right panel host, bottom deck and overlay layer | major surfaces are disjoint across viewport sizes |
| `D10-P4-T2` | `MapStageActor.kt` | delegate map rendering to `TileRenderer` inside Table-derived bounds | map actor bounds are stable and clipped |
| `D10-P4-T3` | `FoundationGameScreen.kt` | install retained shell route in production | runtime uses Stage shell |
| `D10-P4-T4` | `DemoShellLayoutSolver` integration | stop using solver as final UI layout authority; keep only temporary adapter if still needed for map-only calculations | no retained surface reads final bounds from old solver |
| `D10-P4-T5` | `DemoShellRenderer.kt` | remove outer frame, nav rail, right panel host and bottom deck host draw paths once retained equivalents exist | renderer no longer draws shell chrome for migrated shell |
| `D10-P4-T6` | `TileRenderer.kt` | narrow non-map flush layers and route map-stage frame through retained shell where possible | map tests remain green |

Break compatibility rule:

1. Do not keep a production flag that can switch between `DemoShellRenderer` shell and `GameShellTable`.
2. If rollback is needed, revert the phase; do not maintain both shells.

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.render.DemoShellLayoutTest --no-configuration-cache
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*GameShell*" --tests "com.ktome.client.ui.scene2d.*MapStageActor*" --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Phase-specific closure gate: add and run focused evidence tag `d10RetainedShell`. Full golden passing alone is not D10-P4 closure evidence.

Whitebox evidence:

1. `dark-uiux-pr08-d10-retained-shell`;
2. map-stage crop with actor bounds recorded;
3. right panel crop host visible even before dense widgets migrate;
4. bottom deck crop host visible;
5. movement / targeting smoke confirms map input fallback.

Do not enter P5 if:

1. map crop shifts or blurs when only UI shell changes;
2. right panel or bottom deck overlaps map actor;
3. shell is still drawn by `DemoShellRenderer` in production.

### 4.6 D10-P5: Equipment, Inventory, Shop And Detail Workbench

Goal: migrate the densest item-management UI family into retained widgets and remove old inventory/equipment draw and layout paths.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P5-T1` | `EquipmentPaperDollTable.kt`, `EquipmentSlotButton.kt` | render equipment slots, empty/equipped/selected/disabled states through Skin | slot actor-tree and style tests pass |
| `D10-P5-T2` | `InventoryGridTable.kt`, `InventoryCellButton.kt` | render compact right-panel backpack grid and stack badges | stack/pagination tests pass |
| `D10-P5-T3` | `ItemDetailPane.kt`, `ItemCompareRows.kt` | render typed detail and compare rows from presenter output | deterministic locale-safe rows |
| `D10-P5-T4` | `InventoryWorkbenchTable.kt` | migrate full PR-05-1 inventory workbench with 6x4 grid, detail, compare and footer hints | workbench actor-tree covers selected item and hover preview |
| `D10-P5-T5` | shop/frontstage item surfaces | migrate shop buy/sell offer cards and item tooltips into retained table family | shop focus and affordability states visible |
| `D10-P5-T6` | `DemoShellRenderer.kt`, `InventoryWorkbenchLayout.kt` | remove equipment/backpack/workbench draw and layout authority for migrated surfaces | no production imports for retired layout solver |
| `D10-P5-T7` | `InputHandler.kt` | route inventory pointer/focus through retained cell ids instead of old cell bounds | inventory keyboard and mouse tests pass |

Preserved authorities:

1. `EquipmentInventoryPresenter` remains the equipment/inventory view-model source.
2. `InventoryWorkbenchPresenter` remains the full workbench view-model source.
3. `DescriptionPresenter.presentInventoryItemLines` remains text/detail source.

Break compatibility rule:

1. Do not leave compact inventory in old renderer while full workbench uses retained UI; both belong to the same item-management family and should converge in this phase.
2. If shop item cards share inventory detail components, migrate them in P5 rather than leaving a separate immediate shop UI.

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.EquipmentInventoryPresenterTest --tests com.ktome.client.render.InventoryWorkbenchPresenterTest --tests com.ktome.client.input.InputHandlerTest --no-configuration-cache
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*Inventory*" --tests "com.ktome.client.ui.scene2d.*Equipment*" --tests "com.ktome.client.ui.scene2d.*Shop*" --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Phase-specific closure gate: add and run focused evidence tag `d10InventoryEquipment`. Full golden passing alone is not D10-P5 closure evidence.

Whitebox evidence:

1. `dark-uiux-pr08-d10-right-panel-inventory-retained`;
2. `dark-uiux-pr08-d10-inventory-workbench-retained`;
3. item detail and compare screenshot;
4. stack badge and pagination screenshot;
5. shop buy/sell retained surface if shop route is migrated in this phase.

Do not enter P6 if:

1. inventory selection differs between keyboard and mouse;
2. hover commits selection where the contract says preview-only;
3. old `InventoryWorkbenchLayoutSolver` still owns production cell bounds;
4. compact inventory and full workbench use different style/state semantics.

### 4.7 D10-P6: Talent Tree, Active Slot Modal And Deep Tree UI

Goal: migrate the most complex retained hierarchy after the shell, focus and inventory foundations are stable.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P6-T1` | `TalentAssignTable.kt` | render talent assignment root from `TalentAssignPanelModel` | header, tree, detail and footer visible |
| `D10-P6-T2` | `TalentTreeColumn.kt`, `TalentNodeButton.kt` | render node states, icon states and learned/locked/available markers | actor-tree covers all four states |
| `D10-P6-T3` | `TalentConnectorActor.kt` | render prerequisite connectors as retained actor, not renderer line math | connector state test passes |
| `D10-P6-T4` | `TalentDetailPane.kt` | render current/next/passive/active detail blocks from presenter lines | passive/current/next detail tests pass |
| `D10-P6-T5` | `ActiveTalentSlotModal.kt` | migrate active slot replacement modal to Stage modal layer | modal blocks map movement and restores focus |
| `D10-P6-T6` | `TalentAssignPanelLayoutSolver`, `TileRenderer.kt` talent draw paths | remove old talent modal layout and draw authority | no production talent assign route uses old solver |
| `D10-P6-T7` | `InputHandler.kt` | route talent pointer and active slot choice through retained selection ids | passive action suppression and slot choice tests pass |

Preserved authorities:

1. `TalentSidebarPresenter` remains model/text source.
2. `TalentAssignPanelModel` remains retained input model.
3. Talent legality, prerequisites, cost and passive/active semantics remain outside Scene2D.

Break compatibility rule:

1. Do not keep a text-only talent sidebar as fallback after retained talent tree goes production.
2. Do not keep old active-slot modal drawing as a backup route.

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --no-configuration-cache
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*Talent*" --tests "com.ktome.client.ui.scene2d.*ActiveSlot*" --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Phase-specific closure gate: add and run focused evidence tag `d10TalentTree`. Full golden passing alone is not D10-P6 closure evidence.

Whitebox evidence:

1. `dark-uiux-pr08-d10-talent-tree-retained`;
2. `dark-uiux-pr08-d10-active-slot-modal-retained`;
3. passive talent selected with no active slot modal;
4. active talent selection opens modal and blocks map movement;
5. minimum-window talent tree coexistence with right panel.

Do not enter P7 if:

1. passive talent can open active slot modal;
2. active slot modal does not restore focus;
3. old talent layout solver still owns production visible bounds;
4. tree connector state is only implied by text and not visible.

### 4.8 D10-P7: Combat, Frontstage, Log, Tooltip And Overlay Closure

Goal: finish the remaining UI surfaces that are still too easy to leave as hidden immediate-renderer debt.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P7-T1` | `CombatDecisionTable.kt` | migrate combat action/method/target decision panel to retained UI while map targeting overlay stays in `TileRenderer` | combat decision input tests pass |
| `D10-P7-T2` | `FrontstageSurfaceTable.kt` | migrate route, reward, stat assign and non-item frontstage surfaces | modal card and route preview tests pass |
| `D10-P7-T3` | `BottomDeckTable.kt` | complete hero/action/log/resource retained deck and remove bottom-deck immediate draw | log/action text does not overlap |
| `D10-P7-T4` | `NavRailTable.kt` | complete icon rail, mode affordance and focus marker | nav rail keyboard and pointer smoke pass |
| `D10-P7-T5` | `TooltipLayer.kt` | ensure all non-map panel tooltips are retained; map tooltip only remains with map actor if needed | tooltip evidence covers item, talent and combat |
| `D10-P7-T6` | `DemoShellRenderer.kt`, `TileRenderModel.kt` | remove remaining non-map UI draw helpers and non-map Tile render-model projection | code scan shows `TileRenderer` only owns map and map-overlay render paths |

Break compatibility rule:

1. If a frontstage surface is player-visible and modal-like, it should not stay in `DemoShellRenderer` after P7.
2. Map targeting overlay may remain in `TileRenderer`, but its surrounding action decision UI must be retained.

Phase gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.ui.card.ModalCardModelTest --tests com.ktome.client.render.RoutePreviewTextTest --no-configuration-cache
./gradlew :client:test --tests "com.ktome.client.ui.scene2d.*Combat*" --tests "com.ktome.client.ui.scene2d.*Frontstage*" --tests "com.ktome.client.ui.scene2d.*BottomDeck*" --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Phase-specific closure gate: add and run focused evidence tag `d10FrontstageOverlay`. Full golden passing alone is not D10-P7 closure evidence.

Whitebox evidence:

1. combat decision retained surface;
2. reward/frontstage retained surface;
3. bottom deck retained crop;
4. nav rail retained crop;
5. tooltip layer evidence for migrated surfaces.

Do not enter P8 if:

1. `DemoShellRenderer` still draws non-map player-visible UI;
2. bottom deck has text overlap at minimum window;
3. combat decision UI and map target overlay consume conflicting input.

### 4.9 D10-P8: Dead Route Removal, Evidence Freeze And Final Gates

Goal: remove old UI framework debt and prove the retained route is the only production UI route for migrated scope.

Tasks:

| Task | Location | Work | Acceptance |
| --- | --- | --- | --- |
| `D10-P8-T1` | `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt` | delete or reduce to map-only compatibility shim if no production UI route consumes it | code scan confirms no non-map production calls |
| `D10-P8-T2` | `StandaloneScreenLayout.kt`, `StandaloneScreenChrome` | delete old standalone immediate layout/chrome if P2 completed fully | no production imports |
| `D10-P8-T3` | `DemoShellLayout.kt`, `GameShellLayout.kt`, `InventoryWorkbenchLayout.kt`, `TalentAssignPanelLayout.kt` | delete or rename old solver classes whose authority moved to retained tables | retained tables own bounds; tests updated |
| `D10-P8-T4` | `TileLayerFlushReason`, `TileRenderModel` | remove non-map UI flush reasons and projection models no longer needed | map renderer tests stay green |
| `D10-P8-T5` | `tools` / lint | add guard against adding new player-visible UI draw paths to retired immediate classes | maintainability lint or acceptance lint catches regression |
| `D10-P8-T6` | `UI/manual-records/dark-uiux-pr08-d10-retained-ui.md` | write final phase evidence, removed routes, rollback note and residual risks | manual record complete |
| `D10-P8-T7` | golden / whitebox | refresh retained UI golden and packaged whitebox across forest, mine and shadow-depths | evidence accepted |

Final gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint --no-configuration-cache
./gradlew :client:test --no-configuration-cache
./gradlew :client:goldenScreenshot --no-configuration-cache
./gradlew resourcePipelineLint --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
./gradlew verifyChanged --no-configuration-cache
```

Packaged whitebox:

```text
dark-uiux-pr08-director-forest-retained-ui
dark-uiux-pr08-director-mine-retained-ui
dark-uiux-pr08-director-shadow-depths-retained-ui
```

Do not claim D10 complete if:

1. any migrated surface has both immediate and retained production routes;
2. `TileRenderer` or `DemoShellRenderer` still owns standalone screen, inventory, talent, bottom deck, right panel or modal drawing;
3. final packaged whitebox is missing or only debug golden exists;
4. retained UI looks structurally correct but uses generic/default widget styling without dark-v1 Skin evidence.

### 4.10 Phase Transition Checklist

Every implementation PR under D10 must include this phase checklist in its PR body or manual record:

1. `phase`: exact D10-P* id.
2. `old_route_removed`: yes/no with file list.
3. `temporary_adapter`: yes/no with removal phase.
4. `focused_tests`: exact commands and results.
5. `golden_or_actor_tree`: exact artifact labels.
6. `packaged_whitebox`: exact scenario labels or explicit reason it is not required for that phase.
7. `next_phase_allowed`: yes/no with blocker list.

## 5. Implementation Design By File

### 5.1 New Package: `client/src/main/kotlin/com/ktome/client/ui/scene2d/`

Suggested files:

| File | Responsibility |
| --- | --- |
| `KtomeUiStage.kt` | Stage lifecycle、viewport、root installation、input processor exposure |
| `KtomeSceneSkin.kt` | Skin construction from resolver-backed theme assets |
| `KtomeRootTable.kt` | root Table factory for standalone screen and game shell |
| `KtomeScreenPresenter.kt` | view model to widget binding helpers |
| `KtomeUiFocusGraph.kt` | keyboard focus scopes and traversal |
| `MapStageActor.kt` | TileRenderer actor adapter |
| `GameShellTable.kt` | retained in-game shell layout |
| `StandaloneScreenTable.kt` | retained standalone screen layout |

Minimal API sketch:

```kotlin
internal class KtomeUiStage(
    private val viewport: Viewport,
    private val skin: Skin,
) : Disposable {
    val stage: Stage = Stage(viewport)

    fun setRoot(root: Actor) {
        stage.clear()
        stage.addActor(root)
    }

    fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }
}
```

```kotlin
internal class MapStageActor(
    private val renderer: TileRendererDelegate,
    private val snapshotProvider: () -> RenderSnapshot,
    private val overlayProvider: () -> OverlayState,
) : Widget() {
    override fun draw(batch: Batch, parentAlpha: Float) {
        validate()
        renderer.renderInto(
            batch = batch,
            bounds = KtomeBounds(x, y, width, height),
            snapshot = snapshotProvider(),
            overlay = overlayProvider(),
        )
    }
}
```

The sketch defines shape only. Final implementation should align with existing `TileRenderer` batch lifecycle and libGDX `Batch` type.

### 5.2 Existing Screen Classes

| File | Change |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/GameApp.kt` | installs retained screen route, creates Skin/Stage through client asset owner |
| `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt` | replace manual draw with `StandaloneScreenTable` after D10-P2 |
| `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupScreen.kt` | replace manual list / footer rendering after D10-P2 |
| `client/src/main/kotlin/com/ktome/client/screen/VictoryScreen.kt` | use retained outcome table |
| `client/src/main/kotlin/com/ktome/client/screen/GameOverScreen.kt` | use retained outcome table |
| `client/src/main/kotlin/com/ktome/client/screen/UiErrorScreen.kt` | first migration target |
| `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt` | owns retained game shell and `MapStageActor` integration |

### 5.3 Existing Renderer Classes

| File | Change |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | keep map renderer; stop drawing retained UI panels after migration |
| `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt` | staged retirement per panel; map-stage frame may move to Skin / shell table |
| `client/src/main/kotlin/com/ktome/client/render/layout/DemoShellLayout.kt` | short-term adapter for bounds; final authority should move to Table layout |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | preserve view model source for panel content; do not create Actor here |

### 5.4 Assets And Theme

No manifest schema change is planned. If new UI drawable assets are needed:

1. Add key registry / sheet plan entry under the existing dark-v1 PR-08 owner path.
2. Add canonical manifest entry.
3. Sync runtime manifest.
4. Add resolver test.
5. Build Skin drawable through `VisualResolver`.
6. Run `resourcePipelineLint`.

Do not add `.json` Skin files that contain direct raw resource paths unless they are generated / validated artifacts and are not treated as source of truth.

### 5.5 Gradle / Dependency Policy

Scene2D itself is available through current libGDX `gdx` dependency. Therefore D10-P1/P2 must start without dependency changes.

KTX adoption options:

| Option | Decision |
| --- | --- |
| No KTX in first implementation | required for D10-P1/P2, lowest dependency risk |
| Add `ktx-scene2d` after host proof | deferred to `D10-KTX-COMPAT`; only if boilerplate becomes noisy |
| Add broad KTX stack | reject for D10 unless a specific module is justified |

If KTX is added:

1. Add explicit version property aligned with the current libGDX and Kotlin versions after compatibility verification.
2. Add only `ktx-scene2d` and `ktx-style`.
3. Run `./scripts/verify-bootstrap.sh`.
4. Keep final widget structure readable to libGDX developers who do not know KTX DSL.

## 6. Test And Evidence Plan

### 6.1 Focused Unit / Headless Tests

Suggested tests:

1. `KtomeUiStageTest.createsRootTableAndUpdatesViewportOnResize`
2. `KtomeSceneSkinTest.buildsRequiredStylesFromManifestBackedAssets`
3. `KtomeSceneSkinTest.rejectsRawPathBackedDrawables`
4. `StandaloneScreenTableTest.laysOutMainMenuWithoutTextOverlap`
5. `StandaloneScreenTableTest.laysOutOutcomeScreenWithinPanelBounds`
6. `GameShellTableTest.keepsMapStageRightPanelAndBottomDeckDisjoint`
7. `MapStageActorTest.delegatesRenderingWithinActorBounds`
8. `KtomeUiFocusGraphTest.traversesPrimaryFlowByKeyboard`
9. `KtomeUiFocusGraphTest.modalBlocksMapMovementAndRestoresFocus`

### 6.2 Existing Test Suites To Keep Green

1. `MainMenuScreenTextTest`
2. `StandaloneScreenLayoutTest` until the relevant screen is removed from old layout
3. `GameShellLayoutTest` until shell layout authority moves to Table
4. `DemoShellRendererTest` for non-migrated panels
5. `TileRendererCanvasTest`
6. `InputHandlerTest`
7. `ClientSmokeHarnessTest`
8. `GoldenScreenshotHarnessTest` focused PR-08 labels

### 6.3 Golden / Whitebox Evidence

Required crops after runtime migration:

| Scenario | Evidence |
| --- | --- |
| `dark-uiux-pr08-director-forest-retained-ui` | full window, map-stage crop, right panel crop, bottom deck crop, modal/tooltip if active |
| `dark-uiux-pr08-director-mine-retained-ui` | same crop set; prevents forest-only validation |
| `dark-uiux-pr08-director-shadow-depths-retained-ui` | same crop set; covers disconnected / dark topology stress |
| `dark-uiux-pr08-d10-main-menu-retained` | standalone screen crop and actor-tree report |
| `dark-uiux-pr08-d10-validation-retained` | validation setup crop and keyboard flow evidence |
| `dark-uiux-pr08-d10-outcome-retained` | victory / game-over / error table evidence |
| `dark-uiux-pr08-d10-inventory-workbench-retained` | inventory workbench / detail / compare retained surface evidence |
| `dark-uiux-pr08-d10-right-panel-inventory-retained` | compact right-panel equipment / backpack retained surface evidence |
| `dark-uiux-pr08-d10-shop-retained` | shop buy/sell offer cards, price/disabled state, replacement modal and item tooltip evidence |
| `dark-uiux-pr08-d10-talent-tree-retained` | talent assign tree, connector and detail retained surface evidence |
| `dark-uiux-pr08-d10-active-slot-modal-retained` | active talent slot modal focus and blocking evidence |
| `dark-uiux-pr08-d10-combat-decision-retained` | combat action/method/target retained surface with map targeting handoff evidence |
| `dark-uiux-pr08-d10-frontstage-retained` | route, reward, stat assign and frontstage retained modal/card evidence |
| `dark-uiux-pr08-d10-bottom-deck-retained` | hero/action/log/resource bottom deck retained layout evidence |
| `dark-uiux-pr08-d10-nav-rail-retained` | icon rail, mode affordance and focus marker retained layout evidence |

Evidence must record:

1. route: `retained-ui-stage`
2. screen id
3. skin style version or style key set hash
4. map-stage actor bounds
5. focused actor path, if modal or tooltip is open
6. manual verdict

## 7. Migration And Rollback

### 7.1 Migration Rules

1. Migrate one screen or panel family at a time.
2. Each migrated surface must have a single production route.
3. Old layout helper may stay only for unmigrated surfaces.
4. Do not rewrite unrelated PR-08 map art / source-band code while establishing retained UI host.
5. Do not combine KTX dependency adoption with first functional migration unless the no-KTX prototype already proved too costly.

### 7.2 Rollback Rules

Rollback must be narrow:

1. Repoint the affected screen back to the prior immediate route.
2. Keep `KtomeUiStage` host if other migrated screens still use it.
3. Do not delete manifest assets as part of UI rollback unless they were introduced only for the failed slice.
4. Manual record must state which route is active after rollback.

### 7.3 Staged Retirement

| Old component | Retirement condition |
| --- | --- |
| `DarkStandaloneScreenLayout` | all standalone screens migrated and actor-tree/golden evidence passes |
| standalone screen manual `SpriteBatch` draw blocks | each corresponding screen table passes tests and whitebox |
| `DemoShellRenderer` right panel paths | right panel retained table passes layout/golden and input smoke |
| `DemoShellRenderer` bottom deck paths | bottom deck retained table passes layout/golden and log/action smoke |
| `DemoShellLayoutSolver` as final authority | root Table exposes stable map/right/bottom bounds and input hit-test uses those bounds |

## 8. Screen Adaptation Plan

D10 引入 retained UI 后，首页、天赋树配置、背包详情页都需要适配。原因不是这些 screen 当前一定最丑，而是它们分别覆盖 retained UI 的三类高风险能力：

1. 首页 / 主菜单验证 standalone screen 的品牌层级、主操作、焦点和语言切换。
2. 天赋树配置验证深层树状列表、技能图标、前置 connector、详情 pane、主动槽 modal。
3. 背包详情页验证 dense grid、装备纸娃娃、typed detail / compare、stack badge、分页和 tooltip。

### 8.1 Main Menu / Home Screen

Current owner files:

| File | Current role | D10 target |
| --- | --- | --- |
| `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt` | manual `SpriteBatch` drawing | install `StandaloneScreenTable.mainMenu` |
| `client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt` | fixed standalone layout math | retained only for unmigrated screens |
| `client/src/test/kotlin/com/ktome/client/screen/MainMenuScreenTextTest.kt` | text / locale assertion | keep as presenter/text contract |
| `client/src/test/kotlin/com/ktome/client/GameAppLifecycleTest.kt` | app flow smoke | add retained route assertions |

Required retained widgets:

1. `MainMenuRootTable`
2. `BrandHeaderGroup`
3. `MainActionStack`
4. `ContinueStatusPanel`
5. `ValidationEntryButton`
6. `LanguageToggleButton`
7. `FooterHelpLabel`

Style keys:

```text
ktome.screen.main_menu.root
ktome.screen.brand.title
ktome.screen.brand.subtitle
ktome.button.primary.large
ktome.button.secondary.row
ktome.panel.status
ktome.focus.ring.gold
```

Implementation rules:

1. `MainMenuController` and existing action model stay the behavior authority.
2. Actor callbacks call existing commands; Actors do not decide continue availability.
3. No baked title, hotkey, validation label or language text in images.
4. `UI/UI-demo-new.png` is only density/material reference; main menu should be usable first screen, not a landing page.

Evidence:

1. `StandaloneScreenTableTest.laysOutMainMenuWithoutTextOverlap`
2. `KtomeUiFocusGraphTest.traversesMainMenuPrimaryActions`
3. `dark-uiux-pr08-d10-main-menu-retained`
4. packaged app launch screenshot after retained route is active.

### 8.2 Validation / Error / Outcome Standalone Screens

These screens migrate before main menu if a lower-risk proof is needed.

Order:

1. `UiErrorScreen`
2. `VictoryScreen`
3. `GameOverScreen`
4. `ValidationSetupScreen`
5. `MainMenuScreen`

Shared table:

```text
StandaloneScreenTable
  -> HeaderBand
  -> BodyPanel
  -> ActionRow
  -> FooterHelp
```

Rules:

1. Outcome summary text remains produced by `OutcomeSummaryPresenter`.
2. Validation scenario list remains produced by existing validation presentation catalog.
3. Long localized text must wrap/truncate through Scene2D label/table constraints, not pre-cut in renderer draw loops.
4. Existing old layout tests can remain until the corresponding screen exits old route.

### 8.3 In-Game Shell Foundation

The in-game shell is the D10 structural center. It owns the layout that makes `UI/UI-demo-new.png` feasible.

Target table:

```text
GameShellTable
  +-- NavRailTable
  +-- MapStageActor
  +-- RightPanelStack
  +-- BottomDeckTable
  +-- OverlayLayer
```

Migration rule:

1. First create `GameShellTable` with the same major surface proportions as current shell.
2. Then move chrome and non-map panels out of `DemoShellRenderer`.
3. Only after right/bottom panels are retained should map-stage art/topology work continue in this route.

Required tests:

1. `GameShellTableTest.keepsMapStageRightPanelAndBottomDeckDisjoint`
2. `MapStageActorTest.delegatesRenderingWithinActorBounds`
3. `InputHandlerTest` hit-test adapter coverage once Table-derived bounds are consumed.

### 8.4 Equipment / Inventory Detail Page

Inventory and equipment must be adapted because they are the densest `UI/UI-demo-new.png`-like right-panel surface.

Current owner files:

| File / area | D10 target |
| --- | --- |
| `TileRenderModel` equipment / inventory sections | keep as view model source |
| `DemoShellRenderer` equipment/backpack draw paths | retire after retained widgets pass |
| PR-05-1 inventory workbench presenter/tests | reuse contracts; do not duplicate item logic |

Required retained widgets:

1. `EquipmentPaperDollTable`
2. `EquipmentSlotButton`
3. `InventoryGridTable`
4. `InventoryCellButton`
5. `ItemDetailPane`
6. `ItemCompareRows`
7. `StackBadgeLabel`
8. `InventoryFooterHints`

Style keys:

```text
ktome.slot.empty
ktome.slot.equipped
ktome.slot.selected
ktome.slot.disabled
ktome.badge.stack_count
ktome.panel.item_detail
ktome.panel.compare.positive
ktome.panel.compare.negative
```

Implementation rules:

1. Inventory selection remains committed by existing input/controller flow; hover only previews tooltip unless the existing contract says otherwise.
2. Item tooltip and detail pane consume typed presenter output, not raw item objects.
3. Stack count badge only appears when typed quantity exists; do not infer from display text.
4. Equipment compare rows must remain deterministic and locale-safe.
5. Full workbench and compact right-panel inventory share style keys, but not necessarily one table class.

Evidence:

1. `dark-uiux-pr08-d10-inventory-workbench-retained`
2. `dark-uiux-pr08-d10-right-panel-inventory-retained`
3. `InventoryWorkbenchPresenterTest` remains green.
4. focused actor-tree test asserts item grid, selected detail, stack badge and footer hints.

### 8.5 Talent Tree Configuration

Talent tree must adapt because it is the most complex retained hierarchy: tree list, icon states, connectors, detail pane and modal coexist.

Current owner files:

| File / area | D10 target |
| --- | --- |
| `TalentSidebarPresenter` | remains behavior / text view model source |
| `TalentTreeNodeSnapshot` | remains data contract |
| `TalentAssignPanelModel` | retained UI input model |
| active slot modal input path | route into Stage modal layer |

Required retained widgets:

1. `TalentAssignTable`
2. `TalentTreeColumn`
3. `TalentNodeButton`
4. `TalentConnectorActor`
5. `TalentDetailPane`
6. `TalentPointHeader`
7. `TalentLegendRow`
8. `ActiveTalentSlotModal`

Style keys:

```text
ktome.talent.node.available
ktome.talent.node.learned
ktome.talent.node.locked
ktome.talent.node.selected
ktome.talent.connector.available
ktome.talent.connector.locked
ktome.panel.talent.detail
ktome.modal.active_slot
```

Implementation rules:

1. Talent legality, cost, prerequisites and passive/active semantics remain outside Scene2D.
2. `TalentSidebarPresenter` continues producing current / next-level lines.
3. `PASSIVE` talents never open active slot modal.
4. Active slot modal is a Stage modal and must block map movement until closed.
5. The retained tree must coexist with right companion equipment/inventory/status surfaces at minimum window sizes.

Evidence:

1. `dark-uiux-pr08-d10-talent-tree-retained`
2. `dark-uiux-pr08-d10-active-slot-modal-retained`
3. existing PR-04 / PR-04-01 focused tests stay green.
4. actor-tree test asserts selected node, locked node, connector, detail pane and modal focus.

### 8.6 Rollout Order

The screen rollout must follow the phase plan in §4. Historical `D10-S*` shorthand must not be used as an execution label; implementation packets use `D10-P*` phase ids.

Recommended order:

1. D10-P0: authority freeze and cutover inventory.
2. D10-P1: Scene2D kernel, Skin authority and input boundary.
3. D10-P2: standalone screens hard cut, including main menu.
4. D10-P3: unified focus, modal, tooltip and hit-test model.
5. D10-P4: in-run shell hard cut and `MapStageActor`.
6. D10-P5: equipment, inventory, shop and detail workbench.
7. D10-P6: talent tree, active slot modal and deep tree UI.
8. D10-P7: combat, frontstage, log, tooltip and overlay closure.
9. D10-P8: dead route removal, evidence freeze and final gates.

Do not start talent tree or full inventory workbench before P1-P4 pass. They should consume the retained host, Skin, shell and focus contracts after the simpler standalone and shell surfaces are stable. If the team intentionally pulls inventory or talent earlier, it must create a separate PR with its own phase gate and explain why the dependency inversion is worth the risk.

## 9. Risk Register

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Stage steals gameplay input | Movement / targeting can regress invisibly | input multiplexer tests, modal blocking tests, keyboard smoke |
| Skin becomes second resource authority | Breaks K-ToME manifest contract | resolver-backed drawable factory, raw path scan, resource lint |
| Table layout fights existing input hit-test | Mouse hover/click areas drift from visual bounds | expose Table-derived bounds to input adapter and add hit-test tests |
| KTX dependency expands scope | Dependency verification can dominate PR | start with libGDX official API, add KTX only after host proof |
| Retained UI duplicates old immediate route | Long-term maintenance gets worse | one-screen-at-a-time migration and staged retirement matrix |
| Map rendering inside Actor breaks batch lifecycle | blank frames or nested begin/end errors | single owner draw lifecycle test and packaged smoke |
| Scene2D widgets look generic | VisUI-like default styling can miss dark UI bar | PR-08 Skin bridge consumes dark-v1 assets and golden evidence |
| Golden passes but packaged app differs | Desktop packaging / asset loading can diverge | packaged whitebox remains required for runtime migration |

## 10. Completion Definition

D10 retained UI PR is complete only when:

1. PR-08 D10 direction is recorded as retained UI / theme authority optimization, not more map texture micro-polish.
2. D10-P0 through D10-P8 either pass in order or are explicitly split into separate PRs with equivalent phase gates.
3. `KtomeUiStage` exists and owns Stage lifecycle for migrated surfaces.
4. `KtomeSceneSkin` builds required styles without raw path or manifest inventory mirror.
5. `MapStageActor` embeds `TileRenderer` without copying gameplay or resource authority.
6. standalone screens, in-game shell, inventory/equipment/shop workbench, talent tree, active slot modal, combat/frontstage and bottom deck are retained UI production routes or explicitly out of D10 split scope.
7. keyboard focus, modal blocking, tooltip layering and map input handoff have focused tests.
8. right panel、bottom deck、map-stage crops pass golden / packaged evidence for migrated runtime surfaces.
9. `UI/UI-demo-new.png` parity decomposition is recorded, and retained UI evidence covers structural parity plus dark-v1 Skin consumption.
10. KTX / Skin Composer / VisUI / gdx-skins adoption decisions are closed by §2.5 and any dependency change has a separate compatibility record.
11. Old immediate routes for migrated surfaces are removed or quarantined with a rollback-only note; they are not long-term production compatibility routes.
12. No `core` / `game` / save / replay / profile / content-pack / manifest schema change is introduced.
13. Actual commands and results are recorded in the iteration log or manual record.
14. No machine-local absolute path enters committed docs, manifests, reports, fixtures or evidence index.

## 11. Summary

D10 的最佳方案是改变 UI authority，而不是继续在 `TileCanvas` 里堆更多视觉构图：

1. libGDX 官方 `Scene2D / Stage / Skin / Table` 是 K-ToME 应该回归的 retained UI 主线。
2. KTX Scene2D DSL 可以借鉴或后续引入，但不能阻塞官方 Scene2D host proof。
3. Skin Composer、TexturePacker 是资源与 Skin authoring 工具，不是 runtime truth。
4. VisUI、Unciv、Mindustry、Shattered Pixel Dungeon 只提供不同层面的参考，不应成为 K-ToME 的直接架构依赖。
5. `TileRenderer` 保留地图渲染权威，`MapStageActor` 把它嵌入 retained shell。
6. 右面板、bottom deck、modal、tooltip、standalone screens 必须逐步迁移到 retained UI，结束长期手写 UI framework 扩散。
7. 质感不会由框架自动产生；D10 的价值是让 dark-v1 资源、Skin style、Table layout 和 packaged evidence 形成可复用、可验证的 UI 质量闭环。
8. 首页、天赋树配置和背包详情页都需要适配；它们分别验证 standalone screen、复杂树状交互和 dense inventory/detail surface。
9. 既然进入 D10 retained UI 大改造，就不应保留旧 immediate UI 作为长期兼容路线；每个 phase 必须完成旧 route 删除或隔离、自动化测试、golden / 白盒证据和 manual record 后才能进入下一 phase。
