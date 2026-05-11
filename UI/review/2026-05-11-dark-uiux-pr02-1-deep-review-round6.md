# Dark UI/UX PR-02-1 Demo Shell Foundation Review (Round 6)

目标文档：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`（783 行版）

上一轮报告：`UI/review/2026-05-11-dark-uiux-pr02-1-deep-review-round5.md`

审查规范：`docs/review/rule/pr-level-review-standard.md`

审查结论：上一轮主要 P1/P2 已基本吸收，当前文档已经具备 demo shell 基础开发合同的主体结构；但仍有 2 个 P1 和 4 个 P2 会让实现者在白盒 scenario、验证顺序、manual evidence 和跨文档测试锚点上走出不同实现。按当前版本直接开发仍有返工风险，建议先修本文 P1/P2 后再进入 PR-02-1 实现。

## 预检摘要

| 项 | 结果 |
| --- | --- |
| 分支 | `codex/dark-uiux-pr02-ui-chrome-sprite-pilot` |
| 工作树 | 非干净；包含 PR-02 既有 UI/assets/tools 改动，以及 PR-02-1 新文档/历史 review 报告 |
| 本轮范围 | `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`、`UI/PLAN.md`、`UI/pr/README.md`、`UI/pr/screen-coverage-matrix.md`、validation scenario 现有代码锚点 |
| 不纳入范围 | 不把当前 PR-02 已修改的 renderer/assets 当作 PR-02-1 已实现代码验收 |
| 触碰合同 | client shell layout/rendering、dark-v1 sprite owner scope、runtime/canonical visual manifest、verifyChanged routing、packaged whitebox scenario |
| 稳定红线 | 文档仍声明不改 core 规则、save/replay/profile schema、loot/shop economy、item stats、input command 语义 |

## Round 5 闭环复核

| Round 5 finding | 当前状态 | 当前证据 |
| --- | --- | --- |
| P1-1 `VerifyChangedBuildContractTest` / `VerificationImpactAnalyzerTest` 二选一 | 已解决 | §6.1 item 5 改为两者同时覆盖，见目标文档 line 363 |
| P1-2 `DemoShellRenderer.kt` 缺 focused test owner | 已解决 | Acceptance M03、Canonical Artifact、Scope、§4.2、§9、§10 均加入 `DemoShellRendererTest`，见 line 21、62、132、289、532、553 |
| P1-3 PR-02 owner-scope report rename 缺 removal / rollback | 已解决 | Deleted/replaced、§6.1、PR 描述、回滚边界均补齐，见 line 164、360、744、771 |
| P2 `ownerExpectedKeys` 双源优先级 | 已解决 | §6.1 声明 §6.2 是 direct cell 真源，yaml 由其派生，见 line 351 |
| P2 `modalSafeBounds` 中心与边界 | 已解决 | §3.1/§3.2 写入 `mapStage.center` 与不相交断言，见 line 205、247 |
| P2 `ui.shell.*` consumer-region 限制 | 已解决 | §5.1 明确所有 shell key 只能用于 §6.2 consumer region，见 line 315 |
| P2 packaged whitebox window/crop/catalog 派生 | 已解决 | §8.1.1 固定 5 labels、2 crops、catalog 派生字段，见 line 449-486 |
| P3 nav.gear fallback / screen reader hint / rollback scan | 已解决或可接受 | fallback 被限制为 last-resort；screen reader 留给 PR-07；rollback scan 覆盖 tools/game，见 line 399、734、775-780 |

## Findings

### P0

无。

### P1

#### P1-1 `phase4-v4-scenarios.yaml` 的 evidence list 要求与当前 YAML parser 冲突

证据：

- 目标文档 line 145 要求 `ValidationScenarioRegistry.kt`、`ValidationScenarioPresentationCatalog.kt`、`phase4-v4-scenarios.yaml`、`Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 四方同名同 evidence。
- 目标文档 line 482 写明 `phase4-v4-scenarios.yaml` 写入相同 scenario id 与 evidence list。
- 当前 `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt:617-620` 明确拒绝除 `id` 以外的 YAML 字段：`Scenario yaml entry may only declare id`。
- 当前 `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml:1-10` 也只维护 `id` 列表。

问题：

按文档实现者会自然把 evidence list 写进 YAML，但当前 parser 会直接 fail fast。另一种实现者可能为了让 YAML 接受 evidence list 而扩 schema；这又会改变现有 whitebox scenario YAML 合同，但 PR 文档没有声明 schema 扩展、兼容范围、测试更新或回滚边界。

影响：

- `ValidationScenarioRegistryTest` / `Phase4V4WhiteboxScenarioCliTest` 可能在 YAML parity 阶段失败。
- 即便开发者临时扩 YAML schema，也会把一个原本只负责 scenario id parity 的文件升级成 evidence 第二真源。
- PR-02-1 的 packaged whitebox evidence 可能出现 Kotlin registry、YAML、CLI expected-evidence 三份并行 truth。

修复方向：

优先保持当前 YAML schema 不变，修改目标文档：

```text
phase4-v4-scenarios.yaml 只声明相同 scenario id，用于 registry/materialization presence parity；
required evidence file names 的唯一真源是 ValidationScenarioRegistry.kt，
Phase4V4WhiteboxScenarioCli 从 registry 派生 expected-evidence.json。
```

同时把 line 145 的“四方同名同 evidence”改成“四方同名；evidence 由 registry 派生并由 CLI expected-evidence 验证”。如果确实要扩 YAML schema，则必须在本 PR 文档中新增 YAML schema 变更、parser 修改、旧场景兼容和测试断言。

推荐验证：

- `./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest`
- `./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest`

#### P1-2 §10 验证命令顺序违反自己的 manifest freshness 要求

证据：

- 目标文档 line 42 要求 `syncPhase2Manifests` 必须在 `ManifestResolveTest`、client focused evidence、`darkManifestCoveragePr02_1OwnerScope`、`:client:goldenScreenshot` 和最终 `verifyChanged` 之前完成。
- §10 line 548-560 的第一个 focused command 已经运行 `ManifestResolveTest`。
- §10 line 562-571 才在后面的 resource gate 中运行 `syncPhase2Manifests`。
- §10 line 574 又写“Before the resource gate”，但该说明位于 resource gate 命令块之后，执行顺序语义反了。

问题：

文档同时要求 “sync 在 ManifestResolveTest 之前” 和 “先跑包含 ManifestResolveTest 的 focused layout / renderer，再跑 sync”。这会让实现者不知道应按 Gate Ladder 还是按 freshness 要求执行。

影响：

- `ManifestResolveTest` 可能验证到 stale runtime manifest。
- 如果 `client/src/main/resources/manifests/visual-manifest.json` 未由最新 canonical manifest 同步，focused test 通过也不能证明 PR-02-1 runtime manifest 正确。
- 资源 freshness 失败会被推迟到 golden / packaged whitebox 才暴露，增加截图调试循环。

修复方向：

把 §10 调整成唯一顺序：

1. SDKMAN 环境。
2. `acceptanceContractLint` 与 ownerPr script regression。
3. `syncPhase2Manifests manifestLint assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint ...`。
4. `darkManifestCoveragePr02_1OwnerScope`。
5. 再运行包含 `ManifestResolveTest` 的 client focused tests。
6. client smoke / golden / maintainability / verifyChanged。

或者拆分 focused command：layout-only tests 可在 sync 前跑，`ManifestResolveTest` 必须移到 resource gate 后。

推荐验证：

- `./gradlew syncPhase2Manifests manifestLint assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint ...`
- `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest`

### P2

#### P2-1 Acceptance M07 漏掉 `ValidationCommandSourceTest`，且 owner 字段没有覆盖 whitebox tooling 面

证据：

- Acceptance Matrix M07 line 25 的 fastCheck 只有 `Phase4V4WhiteboxScenarioCliTest`、`ValidationScenarioRegistryTest`、manual demo-delta checklist。
- Scope line 146、§8.1.1 line 486、§9 line 536、§10 line 588 均要求 `ValidationCommandSourceTest` 覆盖 presentation catalog parity / title key。
- M07 owner 写成 `docs / client`，但该行 fastCheck 直接包含 `tools` test，且目标文档 line 145-146 涉及 `game` registry 与 `tools` materialization/CLI。

问题：

验收矩阵是开发者和 reviewer 的第一入口。M07 少列 `ValidationCommandSourceTest` 会让 presentation catalog parity 成为正文里有、矩阵里无的半隐式要求。owner 字段不包含 tooling owner，也会弱化 packaged whitebox materialization 的归属。

影响：

- PR close checklist 可能只跑 M07 矩阵中的两个测试，漏掉 client presentation catalog parity。
- PR-07 final audit 使用 `screen-coverage-matrix` / Acceptance Matrix 回溯时，无法机械追踪 validation setup scenario title key 由谁保证。

修复方向：

将 M07 fastCheck 改为：

```text
Phase4V4WhiteboxScenarioCliTest,
ValidationScenarioRegistryTest,
ValidationCommandSourceTest,
manual demo-delta checklist
```

同时把 owner 改为 `docs / client / tools`，或拆成两行：`M07a packaged whitebox registry/materialization`（tools/client，必要时注明 game registry exception）和 `M07b demo parity manual record`（docs/client）。

#### P2-2 manual record `screenshots[]` 模板没有覆盖 §8.1 要求的每个主 label 双来源

证据：

- §8.1 line 439-443 对 5 个主 label 均列出 golden artifact 和 packaged/manual artifact。
- manual record template line 671-703 只包含：
  - `demo-shell-1280x800`: golden + packaged
  - `inventory-open`: packaged only，缺 golden
  - `right-panel-grid`: golden only，缺 packaged
  - `main-menu`: golden only，缺 packaged
  - `validation-setup`: golden only，缺 packaged
  - 两个 crop: packaged supporting evidence

问题：

模板的 `screenshotLabelCoverage.required` 写了 7 个 label，但 `screenshots[]` 没有要求每个主 label 记录 golden 与 packaged 两个 source。实现者按模板填写时会留下 evidence 空洞，却仍可把 `missing: []` 写成通过。

影响：

- reviewer 无法判断某个 label 是只通过 headless golden，还是也通过 packaged app。
- `PR 描述必须引用 client/build/reports/golden/ 中 PR-02-1 labels` 与 packaged evidence 之间无法逐项对应。
- demo parity fail 时难以判断问题来自渲染 harness、packaged app、窗口尺寸还是人工截图。

修复方向：

二选一：

1. 在 `screenshots[]` 模板中为 5 个主 label 全部列出 `source=golden` 和 `source=packaged` 两条记录。
2. 将模板拆为 `goldenScreenshots[]`、`packagedEvidence[]`、`supportingCrops[]`，并让 `screenshotLabelCoverage.missing` 按 `(label, source)` 维度记录。

推荐至少补齐缺失的 4 条 packaged 和 1 条 golden 记录。

#### P2-3 packaged whitebox scenario 的 `prId` 未冻结

证据：

- 当前 `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt:89-94` 的 `ValidationScenarioDef` 必填 `prId`。
- 现有 `ValidationScenarioRegistryTest` 会断言各 scenario 的 `prId`，例如 `PR-00`、`PR-01`、dark UI/UX PR-02 scenario。
- 目标文档 §8.1.1 line 453 固定 `scenarioId`，但未声明新 scenario 的 `prId` 应为 `PR-02-1` 还是 `PR-02`。
- 目标文档 line 480-486 只要求 evidence、presentation、materialization parity，没有明确 `ValidationScenarioRegistryTest` 必须断言 `prId`。

问题：

PR-02-1 是合法细分 owner，资源 ownerPr 也强制为 `PR-02-1`。如果 validation scenario 继续写 `prId="PR-02"`，实现仍可能通过大部分 parity 测试，但 manual record、expected evidence 和后续 final audit 会把 PR-02 与 PR-02-1 混在一起。

影响：

- packaged whitebox evidence 归属不清，PR-02 历史 evidence 和 PR-02-1 demo shell evidence 会混淆。
- PR-07 final audit 按 PR owner 汇总时无法精确追踪 demo shell foundation。

修复方向：

在 §8.1.1 contract table 增加：

```text
prId = PR-02-1
```

并在 §8.1.1 实现文件与测试中补充：`ValidationScenarioRegistryTest` 必须断言 `scenario.prId == "PR-02-1"`。

#### P2-4 `screen-coverage-matrix.md` 未同步 `DemoShellRendererTest`

证据：

- 目标文档 M03、Canonical Artifact、Scope、§4.2、§9、§10 均明确 `DemoShellRendererTest` 是 renderer key-binding focused test owner。
- `UI/pr/screen-coverage-matrix.md:33` 的“局内主 shell”必填证据只列 `DemoShellLayoutTest`、`GameShellLayoutTest`、`TileRendererCanvasTest`，未列 `DemoShellRendererTest`。

问题：

`screen-coverage-matrix.md` 是 PR-07 final audit 的全量界面覆盖入口。它遗漏 `DemoShellRendererTest` 后，后续 reviewer 只按矩阵检查可能漏掉 `ui.shell.*` key 与 consumer region 的专属绑定测试。

影响：

- PR-02-1 文档内已经修掉的 Round 5 P1-2 可能在 PR-07 覆盖矩阵里复活。
- key 绑错但 bounds 正确的 renderer regression 可能只靠 golden 才暴露。

修复方向：

将 `UI/pr/screen-coverage-matrix.md:33` 的必填证据追加 `DemoShellRendererTest`，并注明它负责 `ui.shell.*` key-binding / draw step，而 `TileRendererCanvasTest` 负责 integrated owner-bounds / draw order markers。

### P3

#### P3-1 Canonical Artifact 列表未列 root `build.gradle.kts`

证据：

- 目标文档 line 144 要求修改 root `build.gradle.kts`，把 `:tools:darkManifestCoveragePr02_1OwnerScope` 加入 root `verifyChangedTaskPaths`。
- §6.1 line 363-364 与 M08 line 26 均把 root verifyChanged wiring 作为验收合同。
- Canonical Artifact line 82-89 列了 scripts、`tools/build.gradle.kts`、registry 和 tests，但未列 root `build.gradle.kts`。

影响：

自审时可能只看 `tools/build.gradle.kts` 和 test，而漏掉 root `verifyChangedTaskPaths` 是 PR-02-1 的 canonical artifact。虽然 §6.1 和 §10 已有测试保护，但 artifact 清单本身仍不完整。

修复方向：在 Round 1B shell chrome canonical artifact 增加 `build.gradle.kts`。

#### P3-2 `UI/pr/README.md` Evidence Matrix 未提示 supporting crop 与 coverage artifact

证据：

- 目标文档 §8.1 line 445、§8.1.1 line 469-476 要求两个 supporting crop evidence。
- 目标文档 §12 line 740-744 要求 PR 描述引用 PR-02-1 manual record、golden labels、verifyChanged duration summary 和 owner-scope coverage report。
- `UI/pr/README.md:242` 的 PR-02-1 Evidence Matrix 只列 5 个主 label 和 manual record。

影响：

README 作为系列入口时，reviewer 可能只检查 5 个主 label，漏掉 crop evidence 和 coverage report。目标 PR 文档本身已经写清，因此这是入口提示不完整，不是阻塞项。

修复方向：在 README PR-02-1 evidence row 末尾补 `supporting crop evidence` 与 `dark-v1-manifest-coverage-pr02-1-owner-scope.json`，或注明 README 只列主 golden/manual label，完整 evidence 以 PR-02-1 §8/§12 为准。

## Requirement Alignment

| Requirement | 状态 | 说明 |
| --- | --- | --- |
| UI02-1-M01 文档治理 | 部分一致 | 主矩阵结构成立；P1/P2 需修后才算可开发 |
| UI02-1-M02 shell layout | 一致 | typed regions、profile 数值、modal bounds 已补齐 |
| UI02-1-M03 renderer/layer order | 部分一致 | `DemoShellRendererTest` 已写入目标文档；screen coverage matrix 未同步 |
| UI02-1-M04 right panel scaffold | 一致 | slot/grid scaffold、consumer-region 限制已明确 |
| UI02-1-M05 Round 1B resources | 部分一致 | owner scope、report name、regex 已清晰；验证顺序仍需调整 |
| UI02-1-M06 standalone/menu alignment | 一致 | main menu / validation setup density 与 evidence label 已声明 |
| UI02-1-M07 demo parity evidence | 部分一致 | scenario YAML 合同冲突、M07 fastCheck 漏项、manual template 缺双来源截图 |
| UI02-1-M08 validation / maintainability gates | 部分一致 | static/root routing 已清晰；root `build.gradle.kts` 未列入 canonical artifact |

## 功能/系统一致性矩阵

| Surface | 文档状态 | 当前实现表面 |
| --- | --- | --- |
| Demo shell layout / renderer | 文档已定义 `DemoShellLayout.kt` / `DemoShellRenderer.kt` / tests | 当前工作树尚未出现 `DemoShellLayout.kt`、`DemoShellRenderer.kt`、`DemoShellLayoutTest.kt`、`DemoShellRendererTest.kt` |
| Round 1B resources | 文档定义 `r01b-ui-shell-chrome`、15 direct cells、1 reserved cell | 当前资源/manifest 尚未出现 `r01b-ui-shell-chrome` 或 `PR-02-1` entries；这符合“文档先行”状态 |
| ownerPr regex | 文档要求 `^PR-\d{2}(?:-\d+)?$` | 当前 `scripts/dark_sprite_sheet_contract.py` 仍是 `^PR-\d{2}$`；这是 PR-02-1 implementation todo，不是文档缺陷 |
| verifyChanged routing | 文档要求 PR-02 与 PR-02-1 owner-scope task 同时路由 | 当前 root/tools routing 尚未出现 PR-02-1 task；这是 implementation todo |
| packaged whitebox | 文档要求新增 scenario、catalog、YAML、materialization、locale、tests | 当前 YAML schema 只允许 id；目标文档关于 YAML evidence list 需先修 |

## 玩法与体验审查

方向正确：当前文档已经把“从 text-first 三栏旧 shell 改为 demo-like 主框架”拆成 left icon rail、dominant map stage、right scaffold、bottom hero/action/log deck，并且明确 PR-03/05/06 只补资源质量和细分面板，不再重推主框架。

仍需修正的是验收证据，不是玩法目标：P1/P2 若不修，最坏情况是开发者实现了看起来像 demo 的界面，但 validation scenario / manual record / matrix 无法证明 packaged app、manifest freshness、key binding 和 golden 双来源都一致。这会让 PR-02-1 交付后仍可能在 PR-07 才发现主 shell 证据链不闭合。

## 当前阶段必须解决的问题

1. 修 P1-1：明确 `phase4-v4-scenarios.yaml` 是否仍只允许 `id`。推荐不要扩 schema，避免 evidence 第二真源。
2. 修 P1-2：重排 §10 命令，让 `syncPhase2Manifests` 先于 `ManifestResolveTest` 和 client evidence。
3. 修 P2-1：M07 增加 `ValidationCommandSourceTest`，并修 owner 覆盖。
4. 修 P2-2：manual record template 覆盖所有主 label 的 golden + packaged source。
5. 修 P2-3：冻结 packaged whitebox scenario 的 `prId=PR-02-1`。
6. 修 P2-4：同步 `screen-coverage-matrix.md` 的 `DemoShellRendererTest`。

## Removal / Iteration Plan

本轮未发现新的 removal blocker。Round 5 提出的 PR-02 owner-scope report rename 已进入 Deleted/replaced 清单与 rollback 边界。

仍建议追加两个小清理：

1. Canonical Artifact 补 root `build.gradle.kts`，避免 root `verifyChangedTaskPaths` 在 self-audit 中被漏看。
2. README Evidence Matrix 增补 crop/coverage 提示，避免系列入口与 PR 文档证据口径不同步。

## Additional Suggestions

1. 在 §8.1.1 的 “实现文件与测试” 中增加一条：`Phase4V4WhiteboxScenarioCliTest` 必须断言 generated runbook 中的 capture commands 覆盖 5 个主 screenshot + 2 个 crop，不只断言 launch script 字符串。
2. 在 manual record template 中将 `missing: []` 改成对象数组，例如 `{ label, source, reason }`，避免 label 存在但 source 缺失时无法表达。
3. 若保留 generic `evidence/app.log` 作为 CLI 启动诊断，同时又要求 `${scenarioId}-app.log` 作为 canonical app log，建议在 §8.1.1 明确二者关系：前者是 launch helper，后者是 required evidence。

## Suggested Verification

文档修复后建议串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
git diff --check -- UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md UI/pr/screen-coverage-matrix.md UI/pr/README.md
```

进入 PR-02-1 实现后，按修订后的 §10 顺序执行：

```bash
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
./gradlew syncPhase2Manifests manifestLint assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint \
  -Pktome.darkUiux.requireFullGrid=true \
  -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml \
  -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl
./gradlew darkManifestCoveragePr02_1OwnerScope
./gradlew :client:test \
  --tests com.ktome.client.render.DemoShellLayoutTest \
  --tests com.ktome.client.render.DemoShellRendererTest \
  --tests com.ktome.client.render.GameShellLayoutTest \
  --tests com.ktome.client.render.InfoSurfaceLayoutTest \
  --tests com.ktome.client.render.TileRendererCanvasTest \
  --tests com.ktome.client.screen.StandaloneScreenLayoutTest \
  --tests com.ktome.client.screen.MainMenuScreenTextTest \
  --tests com.ktome.client.assets.ManifestResolveTest
./gradlew :tools:test --tests com.ktome.tools.verification.VerifyChangedBuildContractTest
./gradlew :tools:test --tests com.ktome.tools.verification.VerificationImpactAnalyzerTest
./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest
./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest
./gradlew :client:test --tests com.ktome.client.input.ValidationCommandSourceTest
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint verifyChanged
```

## Executed Validation

本轮实际执行：

```bash
git status --short --branch
git diff --check -- UI/PLAN.md UI/pr/README.md UI/pr/screen-coverage-matrix.md
git --no-pager diff --check --no-index /dev/null UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md
awk '...' UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md UI/PLAN.md UI/pr/README.md UI/pr/screen-coverage-matrix.md
rg -n --type md '/(U[s]ers|tmp)/|[A-Za-z]:\\' UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md UI/PLAN.md UI/pr/README.md UI/pr/screen-coverage-matrix.md
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint
```

结果：

- `git diff --check`：无输出。
- untracked 目标文档用 `--no-index` 检查：无 whitespace finding；exit code 为 1 是 no-index diff 存在差异的常规返回。
- fenced code block 检查：4 个文档 `FENCE_OPEN=0`。
- 本机绝对路径扫描：无匹配。
- `acceptanceContractLint`：BUILD SUCCESSFUL in 15s。

未执行：

- client focused tests。
- Round 1B resource gate。
- `:client:clientSmoke` / `:client:goldenScreenshot`。
- packaged app whitebox。
- `maintainabilityLint` / `verifyChanged`。

未执行原因：本轮是 PR-02-1 文档复审；当前 PR-02-1 implementation surface 尚未落地，先修文档合同更有效。

## Summary

PR-02-1 文档已经从上一轮的“框架方向对但关键合同缺口多”，推进到“主体可开发但仍有少数关键证据链冲突”。当前不建议直接进入实现，原因不是 demo shell 目标不清，而是 P1/P2 会导致白盒 scenario、manifest freshness、manual evidence 和 final coverage matrix 出现不同实现路径。

修完本文 P1/P2 后，文档即可作为 PR-02-1 的开发基础；P3 可以同批修，也可以作为进入实现前的轻量清理。
