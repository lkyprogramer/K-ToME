# Dark UI/UX PR-08 Director Grade Asset Reset

**阶段**: `dark-uiux-pr08-director-grade-asset-reset`
**优先级**: `P1`
**工作量**: `XL`
**前置条件**: PR-07 已按当前 closeout 完结，包含 packaged app 白盒、final screen evidence index 和 doc-vs-implementation audit。
**资源生成结论**: 允许生成 / 替换正式资源，但必须先完成 subtractive spike、target comp sliceability 和 PR-08 owner coverage wiring。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。PR-08 是 PR-07 之后的彻底 UI/UX 质量重置，不修改 PR-07 的关闭定义；PR-07 evidence 作为 upstream baseline，PR-08 用新的 `dark-uiux-pr08-director-*` evidence 重新验收 map-stage、shell chrome、right panel 和 bottom HUD 的 director-grade quality。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI08-M01` | §1 / PR-07 boundary | `docs` | PR-07 close evidence exists | `acceptanceContractLint` | `UI/review/dark-uiux-final-doc-implementation-audit.md`, `UI/manual-records/dark-uiux-pr07-final-all-screens.md` | `required` |
| `UI08-M02` | §3 subtractive spike | `client` / `docs` | exploratory screenshot + side-by-side verdict | focused render inspection | `UI/review/dark-uiux-director-grade-gap-audit.md` | `N/A` |
| `UI08-M03` | §4 target comp sliceability | `assets` / `docs` | 32px tile-truth inset, resource-family table | contact sheet QA | `UI/manual-records/dark-uiux-director-grade-target-comp.md`, `UI/targets/` | `N/A` |
| `UI08-M04` | §5 PR-08 resource migration | `assets` / `tools` | `darkSpriteSheetLint`, `spriteSheetMapLint` | PR-08 owner coverage, `resourcePipelineLint` | `build/reports/verification/dark-uiux/director-grade-asset-readiness.json` | `N/A` |
| `UI08-M05` | §6 runtime compositor reset | `client` | focused layer / marker readability tests | `:client:clientSmoke`, `:client:goldenScreenshot` | `dark-uiux-pr08-director-*` golden set | `required` |
| `UI08-M06` | §7 shell / panel / bottom HUD unity | `client` / `assets` | focused renderer tests, crop review | `:client:goldenScreenshot` | right-panel and bottom-deck crops | `required` |
| `UI08-M07` | §8 final director close | `client` / `docs` / `tools` | manual side-by-side rubric | packaged app whitebox, `maintainabilityLint`, `verifyChanged` | `UI/manual-records/dark-uiux-director-grade-runtime-parity.md` | `required` |
| `UI08-M08` | D10 retained UI authority | `client` / `docs` | retained UI adoption matrix and screen adaptation plan | focused Scene2D host / Skin / layout tests when implemented | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | `required` |

### Gate Budget

PR-08 触发资源、runtime compositor、golden、packaged app 和 governance 全链路。探索阶段只跑 visual feedback loop；方向 accepted 后才跑 owner-routed program closure loop。

重型 gate 预计包括：

```bash
./gradlew acceptanceContractLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew assetLint styleLint manifestLint resourcePipelineLint
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
./gradlew :client:clientSmoke
./gradlew :client:goldenScreenshot
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-grade
./gradlew maintainabilityLint
./gradlew verifyChanged
```

### Canonical Artifact

| Artifact family | Required repo-relative path |
| --- | --- |
| PR plan source | `UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md` |
| PR execution contract | `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md` |
| D10 retained UI authority contract | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` |
| Gap audit | `UI/review/dark-uiux-director-grade-gap-audit.md` |
| Evidence and direction brief | `UI/review/dark-uiux-pr08-evidence-and-direction.md` |
| Target comp record | `UI/manual-records/dark-uiux-director-grade-target-comp.md` |
| Runtime parity record | `UI/manual-records/dark-uiux-director-grade-runtime-parity.md` |
| Readiness report | `build/reports/verification/dark-uiux/director-grade-asset-readiness.json` |
| Golden labels | `dark-uiux-pr08-director-*` |
| Packaged whitebox | `build/whitebox/dark-uiux-pr08-director-grade/` |

### Failure Rule

1. PR-08 不夹带进 PR-07；PR-07 先关闭当前全屏 coverage / packaged evidence。
2. 如果 subtractive spike 无法证明 root cause，不得直接大规模生成资源。
3. 如果 target comp 无法切成合法 sheet cell / manifest key / consumer / test，不得进入 runtime integration。
4. 如果 PR-08 需要接管 `PR-02-2` first-screen keys，必须在 PR-08 owner contract 中记录 supersession 和 rollback，不能静默复用 PR-02-2 reroll 口径。
5. 如果 packaged app 白盒未通过，不能用 debug golden 或 hash 更新代替 director-grade close。

## 1. 阶段目标

1. 在 PR-07 关闭后，以 PR-08 独立 owner 重置第一屏 UI/UX 品质。
2. 先用 subtractive spike 验证当前 bitmap 是否被 overlay 栈埋掉。
3. 产出可切片 target comp 和 32px tile-truth inset。
4. 将 map-stage、ruins floor/wall、right panel、slot chrome、bottom HUD 的质感提升到 `UI/UI-demo-new.png` 同等级第一眼质量。
5. 让 renderer 回到 compositor 角色：布局、裁剪、fog/light、selection、telegraph、少量 deterministic compositing，不再用大量微矩形模拟资源。
6. 保持 gameplay、save、replay、profile、manifest schema 不变。

## 2. PR-07 Boundary And Current Attempt Disposition

PR-07 的职责是完结当前 dark UI/UX screen coverage、golden、packaged app 白盒和 final audit。PR-08 不修改 PR-07 的关闭定义，也不把 PR-08 的资源重置混入 PR-07。

当前工作区存在一组 renderer-heavy / resource-heavy 变更，读作 PR-07 期间的 attempt evidence 或待审查输入，不自动成为 PR-08 implementation base。当前只读盘点结果：

| Evidence | Result |
| --- | --- |
| `git status --short` | tracked changes touch renderer, resources, validation, i18n, tools and tests; additional UI docs/reports are untracked |
| `git diff --stat` | 39 tracked files changed, 10637 insertions, 591 deletions |
| Largest hotspots | `TileRenderer.kt`, `TileRendererCanvasTest.kt`, `DemoShellRenderer.kt`, `TileRenderModel.kt`, PR-02-2 ruins tile sheets and reports |

PR-08 的默认执行基线应是 PR-07 close 后的 clean branch。若要复用当前 attempt 中的任何 renderer/resource change，必须先在 `UI/review/dark-uiux-director-grade-gap-audit.md` 标记为 retained / rejected / evidence-only，并说明原因。

## 3. Subtractive Spike

PR-08 第一项 runtime 探索不是生成新资源，而是临时关掉主要 decorative overlay 栈，只保留现有 bitmap terrain placement、deterministic visibility fog、最小光照、actor、loot、telegraph 和 selection。

必须记录：

1. `overlayFunctionCount`
2. `rectDrawCountByLayer`
3. `warmOverlaySubpassCount`
4. `materialRectCountPerVisibleCell`
5. `gridDissolveEnabled`
6. root-cause verdict: `overlay-root-cause` / `resource-gap` / `lighting-gap`

探索截图不得更新 golden baseline。

### 3.1 Exploration Result

2026-05-26 已完成 subtractive spike 和 gap audit，结论记录在 `UI/review/dark-uiux-director-grade-gap-audit.md`。

root-cause verdict: `resource-gap`。禁用 foundation glaze、cell material 和 warm overlay 后，地图清晰度提升，但仍暴露出单一 floor/wall tile 重复、硬格线、非矩形暗场不足和 UI chrome 割裂问题；因此 PR-08 后续方向不是纯 overlay cleanup，也不是继续堆 renderer micro-rectangles，而是 asset/compositor reset。

探索截图保留在 `UI/review/dark-uiux-pr08-exploration/`，只作为 PR-08 方向证据，不更新 `client/build/reports/golden/**` baseline。

完整证据链、否决路线、接受路线和下一步实现顺序已收敛到 `UI/review/dark-uiux-pr08-evidence-and-direction.md`。

## 4. Target Comp And Sliceability

target comp 必须附带 32px tile-truth inset，并为每个资源族记录：

`ownerPr`, `sheetId`, `targetKey`, `category`, `displaySize`, `tiling/stretch strategy`, `consumer`, `consumerTest`, `fallbackKey`, `requiresNewSchema=no`, `packagedRisk`.

任何需要 baked UI text、atlas/region schema、full-screen paintover 或新 manifest schema 才能成立的方案，必须 rejected 或拆到独立 manifest epoch PR。

2026-05-26 已建立 target comp 目录合同与人工记录：

1. `UI/targets/README.md`
2. `UI/manual-records/dark-uiux-director-grade-target-comp.md`

当前状态为 `family-pack-accepted`。attempt 1 质感较好但失败于 map sliceability；attempt 2 更接近 orthographic tile-map，但包含 baked English log text；attempt 3 已经成为当前最佳 mood/layout reference，且无明显 baked text，但从整屏 comp 抽样的 32px tile-truth 仍混入光照、毒池、道具、墙边黑场等 compositor/scene 内容，不能证明 floor/wall resource 可切片。

2026-05-27 已产出并接受 resource-family target pack 作为方向证据：

1. `UI/targets/dark-uiux-director-grade-target-family-floor-32px.png`
2. `UI/targets/dark-uiux-director-grade-target-family-wall-32px.png`
3. `UI/targets/dark-uiux-director-grade-target-family-floor-wall-repeat.png`
4. `UI/targets/dark-uiux-director-grade-target-family-map-compositor.png`
5. `UI/targets/dark-uiux-director-grade-target-family-ui-chrome.png`

结论：PR-08 后续大改造方向确定为 resource-family-first reset。floor/wall 可以走 `PR-08` formal supersession；光照、黑场、selection、hazard、telegraph 留在 deterministic compositor；right panel、slot、bottom deck 走 reusable `ui_frame` chrome；actor/prop 暂缓，除非新 floor/wall/compositor 后 marker readability 失败。正式 runtime integration、resource migration、manifest sync 或 golden baseline update 仍必须等 owner coverage、resource readiness、focused renderer tests 和 PR-08 golden labels 接好后再做。

## 5. Resource And Owner Contract

PR-08 固定使用 `ownerPr=PR-08`。如果 `UI-demo-new` 首屏 ruins keys 被 PR-08 接管，必须记录它们从 PR-02-2 baseline 被 supersede 的原因和回滚路径：

1. `tileset.ruins.ground_01`
2. `tileset.ruins.wall_01`
3. `actor.vanguard`
4. `prop.stairs.down`

PR-02-2、PR-05、PR-06 只作为上游合同和 migration 约束来源，不是 PR-08 的替代路线。

### 5.1 Owner Coverage Implementation

2026-05-27 首轮 owner wiring 已把 `tileset.ruins.ground_01` 与 `tileset.ruins.wall_01` 接入 `ownerPr=PR-08`，并新增 `darkManifestCoveragePr08OwnerScope` 与 `UI/sprite-sheets/owner-contracts/pr08-owner-keys.yaml`。`actor.vanguard` 与 `prop.stairs.down` 仍归 `PR-02-2`，除非新 floor/wall/compositor 后 marker readability 失败。

Rollback rule: 若 PR-08 floor/wall runtime candidates 未通过 director review，只回退 PR-08 owner migration、floor/wall generated resources 和 `dark-uiux-pr08-director-*` evidence；不得回退 PR-07 close evidence，也不得把 actor/prop 从 PR-02-2 静默迁出。

## 6. Runtime Layer Contract

PR-08 runtime layer order 固定为：

1. `base stage art`
2. `semantic tile/floor/wall`
3. `deterministic visibility fog`
4. `gameplay overlays/actors/loot/telegraph`
5. `focus/selection`
6. `UI chrome`

decorative darkness 不能隐藏 player、enemy、loot、telegraph 或 selection。

## 7. Evidence Labels

### 7.1 D10 Retained UI Authority Packet

PR-08 D10 is the retained UI / theme authority sub-package for the same director-grade target. It does not replace this PR-08 root contract; it explains how the UI shell should stop accumulating hand-written `TileCanvas` UI composition.

Authoritative D10 contract:

```text
UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md
```

D10 decisions:

1. Adopt libGDX official Scene2D / Stage / Skin / Table as the retained UI path.
2. Defer KTX to a separate compatibility spike; do not introduce it in the first host / Skin phase.
3. Use Skin Composer and TexturePacker as authoring tools only.
4. Do not adopt VisUI or gdx-skins as runtime dependencies for PR-08.
5. Keep `TileRenderer` as map rendering authority and embed it through `MapStageActor`.
6. Execute D10 as `D10-P0 ~ D10-P8` phase-gated hard refactor; migrated surfaces must not keep old immediate UI as long-term compatibility routes.
7. Main menu, shell, talent tree configuration, inventory/detail workbench, shop, combat/frontstage, bottom deck and nav rail require retained UI adaptation plans and evidence labels.

PR-08 必填 golden / manual evidence：

1. `dark-uiux-pr08-director-parity-1672x941`
2. `dark-uiux-pr08-director-map-stage-crop`
3. `dark-uiux-pr08-director-right-panel-crop`
4. `dark-uiux-pr08-director-bottom-deck-crop`
5. `dark-uiux-pr08-director-telegraph-combat-crop`
6. `UI/manual-records/dark-uiux-director-grade-target-comp.md`
7. `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
8. `build/whitebox/dark-uiux-pr08-director-grade/cua-runbook.md`

Existing `ui-demo-new-*` labels remain PR-02-1 / PR-02-2 upstream evidence and baseline, not PR-08 close evidence.

## 8. Completion Definition

PR-08 完成时必须满足：

1. PR-07 close artifacts 已存在且未被 PR-08 反向修改。
2. Subtractive spike root-cause verdict 已记录。
3. Target comp 通过 director review 且可切片。
4. PR-08 resource readiness report 存在并无 blocking findings。
5. Runtime 首屏达到 director rubric；fail 项不得用 golden hash 更新掩盖。
6. Packaged app whitebox 通过，或明确记录工具无法绑定窗口的 blocker 和 fallback evidence；不能伪造通过。
7. `verifyChanged` 通过，或失败项被记录为阻塞。
