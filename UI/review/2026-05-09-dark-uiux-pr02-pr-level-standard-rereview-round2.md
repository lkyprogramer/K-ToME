# Dark UI/UX PR-02 PR 级复审报告 Round 2

目标文档：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`

Review 标准：`docs/review/rule/pr-level-review-standard.md`

上一轮报告：`UI/review/2026-05-09-dark-uiux-pr02-pr-level-standard-review.md`

审查时间：2026-05-09

审查范围：

- 上游入口：`AGENTS.md`、`docs/rule/kotlin.md`、`docs/rule/ai-change-governance.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`
- 目标 PR 文档：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`
- 本轮改动面：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`、`scripts/verify_dark_manifest_coverage.py`、`tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`
- 当前结构化真源锚点：`UI/sprite-sheets/key-registry.yaml`、`UI/sprite-sheets/sheet-plan.yaml`、`assets-src/image/manifests/phase2-visual-manifest.json`、`client/src/main/resources/manifests/visual-manifest.json`
- 本轮重点：核实上一轮 P1/P2 是否被吸收，并判断优化后的 PR-02 文档是否已经达到 `docs/review/rule/pr-level-review-standard.md` 的可开发合同要求

预检摘要：

- 当前基准：`main...origin/main`
- 工作树状态：PR-02 文档、dark manifest coverage 脚本和工具测试有改动；未发现其它目标范围内 tracked 改动。
- 命中的 PR 系列：`UI/pr` dark UI/UX，执行顺序为 `PR-00 -> PR-01 -> PR-01-1 -> PR-02 -> PR-03 -> PR-04 -> PR-05 -> PR-06 -> PR-07`。
- 受影响模块：`docs`、`tools`、`assets`、`client`。
- 稳定合同：visual manifest、key registry、sheet plan、dark-v1 coverage artifact、golden / whitebox evidence、client manifest consumer。
- 主要风险：owner-scope coverage 是否真正 fail fast、三张 Round 1 sheet 是否被 owner gate 机械保证、manual evidence 是否会替代可自动化证据。

## Findings

### P0

无。

### P1

#### P1-1 `ownerSheetIds` 三 sheet 要求仍未接入 fail-fast gate

状态：上一轮 `P1-1` 部分解决。`ownerExpectedKeys=[]` 已经 fail fast，但 `ownerSheetIds` 必须包含三张 Round 1 sheet 仍只停留在文档和人工检查。

证据：

- PR-02 文档要求 PR-02 formal key 使用 `ownerPr: PR-02`，并要求 owner-scope artifact 的 `ownerSheetIds` 至少包含 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:60-62`
- PR-02 验收标准同样要求 `ownerExpectedKeys` 非空、`ownerSheetIds` 包含三张 Round 1 sheet、`ownerMissingKeys=[]`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:227-229`
- PR-02 close gate 明确要求 coverage artifact 满足三张 sheet：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:251-257`
- UI PR README 要求 PR-02 覆盖 Round 1 三张 sheet 的全部非 reserved cell：`UI/pr/README.md:97-100`
- 当前脚本只在 owner-scope 下检查 `ownerExpectedKeys` 非空和 `ownerMissingKeys`，没有把 required sheet set 接入 error list：`scripts/verify_dark_manifest_coverage.py:77-87`、`scripts/verify_dark_manifest_coverage.py:114-115`
- 当前脚本会写出 `ownerSheetIds`，但只是 report 字段，不参与 `status=FAIL` 判断：`scripts/verify_dark_manifest_coverage.py:137-156`、`scripts/verify_dark_manifest_coverage.py:176-177`
- Gradle task 目前只传 `coverageMode` 和 `ownerPr`，没有 `requiredOwnerSheetIds` 或等价输入：`tools/build.gradle.kts:597-630`
- 新增测试覆盖了空 owner scope，但没有覆盖“PR-02 只有一张 sheet 也应失败”的场景：`tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt:623-657`

问题：

PR-02 文档现在已经把三张 sheet 要求写清楚，但实现层的 owner gate 仍可能在以下状态下返回 green：`ownerPr=PR-02` 有非空 key、这些 key 都能解析为 dark output、但全部只来自 `r01-ui-chrome`，缺少 `r01-ui-controls` 和 `r01-ui-hud-icons`。这会让“Round 1 三张 sheet 最小可运行组”变成 reviewer 必须人工读 artifact 才能发现的问题。

影响：

- PR-02 仍可能在没有 HUD icons 或 controls sheet 的情况下被误关。
- 首页 / 验证 / outcome / error / loading 的 screen marker、HUD icon、combat/action glyph 可能缺失，但 owner-scope task 只要已有 key 都 covered 就能通过。
- 后续 PR-03 / PR-04 / PR-07 会继承一个不完整的 Round 1 resource foundation。
- 这不符合 PR 级标准对 blocking requirement 的要求：当前 PR 关闭条件必须有可证伪 owner gate，不能依赖“Gradle 绿后人工继续看字段”。

修复方向：

1. 给 `scripts/verify_dark_manifest_coverage.py` 增加通用参数，例如 `--required-owner-sheet-ids r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons`，并在 owner-scope 下对 `ownerSheetIds` 做 set containment fail-fast。
2. 在 `tools/build.gradle.kts` 暴露 `-Pktome.darkUiux.requiredOwnerSheetIds=...`，PR-02 文档 §7 的 close command 必须带这个属性。
3. 增加测试：`coverage lint fails owner scope when required owner sheet ids are missing`。测试输入应构造非空 `ownerExpectedKeys` 且 manifest/runtime 都是 `dark-v1`，但 registry 只包含 `r01-ui-chrome`，期望脚本 exit code 为 1。
4. PR-02 文档 §7 更新命令示例，避免文档要求和 Gradle task 能力再次分叉。

推荐测试 / gate：

- `DarkSpriteSheetPipelineScriptTest.coverage lint fails owner scope when required owner sheet ids are missing`
- `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons`

### P2

无。

### P3

#### P3-1 Acceptance Matrix 中 `UI02-M03` artifact 仍是泛称

证据：

- `UI02-M03` 的 artifact 写成 `dark manifest coverage report`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:19`
- dark UI/UX governance 要求 artifact 是 repo-relative canonical artifact path：`UI/pr/development-governance.md:24-30`
- 同一文档的 Canonical Artifact 已经写出准确路径 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:38-50`

问题：

这不阻塞实现，因为 §0 Canonical Artifact 和 §7 已经给出准确路径。但 Acceptance Matrix 是开发者和 lint 最先读到的合同入口，`dark manifest coverage report` 这种泛称不如 exact path 可机械摘录。

修复方向：

把 `UI02-M03` 的 artifact 改成 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`。

#### P3-2 raw sheet 生成命令在同一文档内有轻微不一致

证据：

- §2.1 的 raw sheet generation 使用了 `--smoke-report <buildReportPath>`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:93-95`
- §3 Raw sheet 生成交接重复命令时只写了 `--out <rawSheetPath> --overwrite`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:115-119`
- README 示例包含 `--smoke-report build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-chrome.json`：`UI/pr/README.md:192-199`

问题：

该差异不会改变资源主流程，但会让 PR 描述中“source folder/source image 摘要”来源不稳定。PR-02 已经要求 transient source 只能进入 smoke 摘要，不进入长期合同；因此 smoke report path 最好在两个命令块中保持一致。

修复方向：

在 §3 的 raw sheet command 中补 `--smoke-report build/reports/verification/dark-uiux/codex-image-smoke-<sheetId>.json`，并说明 smoke report 是 transient evidence，不属于 canonical manifest / coverage artifact。

## 上一轮 Findings 状态

| Previous Finding | 当前状态 | 证据 / 说明 |
| --- | --- | --- |
| `P1-1 owner-scope gate 可以在 ownerExpectedKeys=[] 时通过` | 部分解决 | 脚本已新增空 owner fail-fast；实跑 owner-scope 在当前 PR-02 无 owner key 时失败。剩余问题是三张 required sheet 未 fail-fast。 |
| `P1-2 Client 消费合同过泛` | 已解决（文档层面） | §4.6 明确 `TileRenderer` / `StandaloneScreenChrome`、`VisualManifestResolver`、`drawAsset` 和 focused test 断言：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:206-216`。当前代码尚未实现属于 PR-02 后续 implementation 范围。 |
| `P1-3 syncPhase2Manifests 顺序错误` | 已解决 | §2.1 和 §7 都要求 sync 在 resolver / coverage / golden / verifyChanged 前：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:96-98`、`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:238-248`。 |
| `P2-1 key 表缺逐 key row/col/outputName/aliasOf` | 已解决 | §4.2-§4.5 已逐 key 列出 direct cells 和 alias entries：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:135-204`。 |
| `P2-2 白盒证据缺 scenario/expected/skip rule` | 已解决 | §8 已补白盒 evidence matrix：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:261-270`。 |
| `P2-3 Gate Budget 缺 freshness / duration source` | 已解决 | Gate Budget 已补 freshness、duration summary 和重试规则：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:28-34`。 |
| `P3-1 主命令未显式包含 owner-scope coverage` | 已解决 | §7 主流程包含 owner-scope command：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:240-248`。 |
| `P3-2 Canonical Artifact 泛称` | 已解决 | Canonical Artifact 已列 repo-relative 路径族：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:38-50`。 |

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-02 依赖 PR-00、PR-01、PR-01-1，并按 README 串行推进 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:6`、`UI/pr/README.md:37-40` | 一致 |
| PR-02 必须生成 Round 1 三张 sheet 最小可运行组 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:7`、`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:103-111`、`UI/pr/README.md:97-100` | 部分一致：文档合同清楚，但 gate 尚未机械保证三张 sheet 都进入 owner scope |
| 新 key 必须从 registry / sheet plan / manifest / sync / resolver / coverage 串行闭环 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:86-101`、`UI/pr/README.md:133-141` | 一致 |
| owner-scope 空分母必须 fail fast | `scripts/verify_dark_manifest_coverage.py:83-87`、`tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt:623-657`、实跑 owner-scope 失败 | 一致 |
| owner-scope 必须包含 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons` | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:251-257`；脚本仅写 `ownerSheetIds` 不校验 required set：`scripts/verify_dark_manifest_coverage.py:137-156` | 部分一致 |
| client 必须实际消费 panel / slot / modal / HUD key | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:206-216` | 一致（文档层面）；实现仍待 PR-02 development |
| 白盒证据必须有 scenario / steps / expected evidence / skip rule | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:261-270`、`UI/pr/development-governance.md:32-37` | 一致 |
| resource / manifest / golden freshness 必须声明 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:28-34`、`UI/pr/development-governance.md:58-65` | 一致 |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| PR-02 文档合同 | Round 1 三张 sheet、逐 key cell contract、client consumer、whitebox、sync-before-evidence | 当前文档已基本冻结合同 | 仅剩 gate required sheet set 未机械化 | P1 |
| Coverage script | owner-scope 必须拒绝空 owner，并证明 PR-02 三张 sheet 完整 | 空 owner 已 fail fast；`ownerSheetIds` 只写 artifact，不参与失败判断 | 可漏掉 controls / HUD sheet | P1 |
| Coverage test | 必须保护 owner-scope 分母和 PR-02 required sheets | 已补空 owner test | 缺 missing required owner sheet ids test | P1 |
| Acceptance Matrix | artifact 应为 repo-relative path | Canonical Artifact 和 §7 有 exact path；matrix 单元格仍是泛称 | 可读性 / 机械摘录问题 | P3 |
| Raw sheet workflow | Codex source folder/source image 只能作为 transient smoke evidence | §2.1 有 smoke report；§3 命令块省略 smoke report | 文档内轻微不一致 | P3 |

## 玩法与体验审查

### 核心循环

PR-02 文档现在已经把“从 layout token 到真实 dark UI chrome 资源”的主路径讲清楚：资源必须从 registry / sheet plan 进入 manifest，再被 `TileRenderer` 和 standalone chrome 消费。核心循环层面，上一轮“玩家仍可能只看到 token 色块”的文档风险已经明显收敛。

### 战斗体验

`ui.combat.*` 被纳入 `r01-ui-controls` formal cells，且通过 resolver test 锁定。不足是 owner gate 目前无法证明 `r01-ui-controls` 一定进入 PR-02 owner scope；如果 P1-1 不修，战斗 action / method / target glyph 仍可能在 PR-02 关闭时缺席。

### 成长与构筑驱动

`ui.state.locked/learnable/active/reserve` 已被明确列入 PR-02 direct cells，并保留 `TalentSidebarPresenterTest` 锚点。文档层面不再需要实现者猜这些 glyph 属于 PR-02 还是 PR-06。

### 新手体验与信息反馈

首页、验证 setup、outcome、error、loading marker 已进入 direct/alias key 表和白盒矩阵。文档已经能支持 PR-02 给新手首屏和错误/加载反馈统一 chrome/control 语言；剩余风险仍是 gate 没有自动保证 controls sheet 存在。

### 系统耦合与体验断层

当前唯一结构性断层是文档合同比工具 gate 更强。PR-02 文档说三张 sheet 缺一不可，但工具只知道 owner key 是否非空和 missing key 是否为空。这个断层必须在 PR-02 当前阶段解决，否则后续 PR 会在“报告字段可见但 task 不拦”的基础上扩张。

## 当前阶段必须解决的问题

1. `P1-1` 必须当前 PR 修。
   - 为什么当前必须修：PR-02 的核心交付单位就是 Round 1 三张 sheet。如果 owner gate 不验证三张 sheet，PR-02 close gate 与 PR 文档完成定义不一致。
   - 为什么不能推迟：PR-03/PR-04/PR-07 会消费 PR-02 的 `ui.frame.*`、`ui.control.*`、`ui.hud.*` 和 `ui.screen.*`，缺 sheet 的问题越晚发现，越难区分是资源缺失还是 client 消费缺失。
   - 修复方向：把 required owner sheet ids 作为 coverage task 输入并 fail fast，补 focused script test。

P3 项建议顺手修；它们不阻塞进入实现，但修复成本低，可以减少 PR 描述和命令复制时的歧义。

## Removal/Iteration Plan

本轮没有建议删除生产代码。

PR-02 文档中的 staged migration 已经足够表达 PR-00 dry-run 与 PR-02 formal owner 的边界：

| Item | Details |
| --- | --- |
| Location | `UI/sprite-sheets/key-registry.yaml`、`UI/sprite-sheets/sheet-plan.yaml`、`assets-src/image/manifests/phase2-visual-manifest.json`、`client/src/main/resources/manifests/visual-manifest.json` |
| Current state | 当前结构化真源仍只有 PR-00 dry-run entries；PR-02 implementation 尚未生成 formal owner entries |
| Target state | PR-02 formal entries 使用 `ownerPr: PR-02`，三张 Round 1 sheet 的 owner scope 非空、完整、可解析、可绘制 |
| Preconditions | `requiredOwnerSheetIds` fail-fast 接入后，再开始正式资源 / manifest / client consumption 实现 |
| Affected gates | `darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint`、`darkManifestCoverageLint owner-scope`、`ManifestResolveTest`、`:client:goldenScreenshot` |
| Rollback | 回滚 PR-02 registry entries、sheet-plan cells、canonical/runtime manifest entries、runtime PNG、renderer consumption points |

## Additional Suggestions

- `UI02-M03` artifact 改成 exact path 后，建议后续把 `acceptanceContractLint` 扩展为能识别 artifact 单元格是否为 repo-relative path，避免“正文写对、矩阵写泛称”的轻微漂移重复出现。
- 如果为 `requiredOwnerSheetIds` 增加 Gradle property，建议让脚本错误信息同时输出 `ownerPr`、`actualOwnerSheetIds` 和 `requiredOwnerSheetIds`，方便 PR reviewer 直接定位漏了哪张 sheet。

## Open Questions

无需要阻塞用户确认的问题。推荐默认判断：

- PR-02 的 required owner sheet set 固定为 `r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons`。
- 该 required set 应作为工具 gate 输入，而不是只写在 PR 描述或 manual record 中。

## Suggested Verification

本轮已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：通过，`BUILD SUCCESSFUL`。

本轮已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest --rerun-tasks
```

结果：通过，`BUILD SUCCESSFUL`。执行中有既有 deprecation / Json format warning，不影响本轮结论。

本轮已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
```

结果：按预期失败。当前结构化真源还没有 `ownerPr=PR-02` entries，脚本输出 `owner-scope coverage found no expected keys for PR-02 from UI/sprite-sheets/key-registry.yaml.`。这证明上一轮空 owner-scope green 的问题已经修掉。

修复 P1-1 后建议新增并执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons
```

进入 PR-02 implementation 后，再按目标文档补跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew verifyChanged
```

本轮未运行 client implementation gate，因为当前改动仍是文档 / tools gate 复审面，不声明 client 资源消费已通过。

## Summary

PR-02 文档本轮已经大幅接近可开发状态：client consumer contract、逐 key cell contract、sync-before-evidence、Gate Budget freshness、whitebox evidence matrix 和 canonical artifact path 都已补齐。上一轮的三个 P1 中，client 合同和验证顺序已解决；owner-scope 空集通过也已经由脚本和测试修掉。

当前仍不建议把 PR-02 视为完全可关闭的开发合同，原因只有一个阻塞点：三张 Round 1 sheet 的 required owner scope 仍未进入 fail-fast gate。修掉 `requiredOwnerSheetIds` 之后，PR-02 文档可以进入实现阶段；剩余 P3 属于文档机械性收口。
