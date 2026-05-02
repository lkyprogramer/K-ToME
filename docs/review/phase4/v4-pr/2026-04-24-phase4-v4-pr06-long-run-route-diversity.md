> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md`

# Phase4 v4 PR-06 Long Run Route Diversity

**阶段**: `Phase 4 completion hardening / phase4-v4-pr06`
**优先级**: `P1`
**工作量**: `M`
**合并来源**: v4 P1-5
**前置条件**: PR-00、PR-03、PR-04、PR-07 已完成；PR-03 的 reward/build 指标已经进入 long-run，PR-04 的 zone hook 字段已经进入 route summary，PR-07 的 sample pack route 字段已经进入 content pack summary
**资源生成结论**: 不生成图片资源；不生成音频资源

## 0. 开发治理与验收矩阵

本 PR 排在 PR-03、PR-04、PR-07 之后，用于验证最终 route diversity 与 `verifyChanged` routing 代表性。执行规则见 [development-governance.md](./development-governance.md)，通用验证阶梯见 [docs/verification/README.md](../../../verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../../rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `PR06-M01` | §5.1 long-run corpus 扩容 | `game` | `LongRunLabSeedBank`, harness corpus tests | `longRunLab` | `build/reports/harness/long-run-full.json` | `required` |
| `PR06-M02` | §5.2 route diversity metric | `game` / `tools` | route hash and summary tests | `longRunLab`, `reportPhase4Only` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `required` |
| `PR06-M03` | §5.3 `verifyChanged` routing | `tools` | `ScopeCoverageLintTest`, `VerificationImpactAnalyzer` tests | `scopeCoverageLint`, `verifyChangedPreflight` | `build/verification/verify-changed/preflight-task-duration-summary.{json,md}` | `required` |
| `PR06-M04` | §5.4 report 展示 | `tools` | `ReportPhase4RunnerTest`, `ReportPhase4MaterializationTest` | `reportPhase4Only`, `reportPhase4` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `N/A` |
| `PR06-M05` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |

### Gate Budget

预计重型任务：`longRunLab`、`reportPhase4Only`、`reportPhase4`、`scopeCoverageLint`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-06 验证长局代表性和 changed-file routing，不新增宽样本到 `verifyChanged`。

### Canonical Artifact

route diversity 的 canonical report 是 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`。`build/reports/harness/long-run-full.json` 是 owner producer evidence；cache 诊断、shard reuse 和本机路径不得进入 fixture 合同。

### Failure Rule

如果 route diversity 失败，先修 corpus 分类、route token 或 hash 口径；如果 routing 失败，只扩 `VerifyChangedPlanGate` / `scopeCoverageLint` 覆盖，不新增第二套 impact analyzer。

## 1. 玩家体验目标

本 PR 修 long-run 的代表性。Phase4 的奖励和构筑调参必须在多路线、多 secret、多 branch 下成立，不能只对一条 full_route 主线跑绿。

完成标准：

1. `LONG_RUN_SMOKE` 的 `full_route` 样本数达到 `12`。
2. `branch_inclusive` 样本数达到 `4`。
3. `zoneRouteHashDiversity.topHashShare <= 40%`。
4. `branchInclusiveCount >= 4`。
5. 每条 branch 样本覆盖不同 mandatory/secret 组合。
6. `verifyChanged` 只路由最小 affected subset，nightly/owner gate 承担宽样本。

## 2. 当前问题

1. 当前 `scenarioTypeDistribution={full_route=6, branch_inclusive=1, route_probe=2, late_route_probe=2}`，本 PR 固定重建为 `full_route=12, branch_inclusive=4, route_probe=2, late_route_probe=2`。
2. `zoneRouteHashDistribution` 中同一 hash 占 6 条。
3. 当前证据能证明主路径稳定，不能充分证明重复游玩差异。
4. Phase5 replay/death analysis 会继承偏窄样本。

## 3. 范围与非目标

### 3.1 范围

生产代码：

- `game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt`
- `game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt`
- `game/src/main/kotlin/com/ktome/game/harness/HarnessMetadata.kt`
- `game/src/main/kotlin/com/ktome/game/RouteHash.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4DomainArtifactRegistry.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerMetricTargets.kt`
- `tools/src/main/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzer.kt`

数据与 baseline：

- `docs/review/phase4/opt/baselines/**`
- `tools/src/main/resources/phase4/aggregation-manifest.yaml`
- long-run owner baseline files governed by `Phase4OwnerBaselineRegistry`

测试：

- `game/src/test/kotlin/com/ktome/game/harness/**`
- `tools/src/test/kotlin/com/ktome/tools/phase4/**`
- `tools/src/test/kotlin/com/ktome/tools/verification/**`
- `tools/src/test/kotlin/com/ktome/tools/verification/ScopeCoverageLintTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/verification/VerifyChangedBuildContractTest.kt`

### 3.2 非目标

1. 不改 procgen algorithm。
2. 不改 reward scoring。
3. 不新增 zone。
4. 不新增 content pack。
5. 不把 wide sample 全部塞进 `verifyChanged`。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

本 PR 只扩 harness corpus 与 report schema，完全不触碰 visual manifest。

### 4.2 音频资源

不生成新音频资源。

本 PR 只扩 harness corpus 与 report schema，完全不触碰 audio manifest。

## 5. 技术方案

### 5.1 Corpus 扩容

`LONG_RUN_SMOKE` 固定重建为以下 final corpus。实现不得依赖旧 6 条 full route 的隐式保留。

| seed | scenarioType | route intent |
| ---: | --- | --- |
| `2026042401` | `full_route` | greenwood -> deep_iron -> grey_gate -> underground_river -> abyssal_temple |
| `2026042402` | `full_route` | greenwood -> underground_river -> grey_gate -> deep_iron -> abyssal_temple |
| `2026042403` | `full_route` | greenwood -> grey_gate -> underground_river -> deep_iron -> abyssal_temple |
| `2026042404` | `full_route` | greenwood -> grey_gate -> deep_iron -> underground_river -> abyssal_temple |
| `2026042405` | `full_route` | deep_iron -> greenwood -> underground_river -> grey_gate -> abyssal_temple |
| `2026042406` | `full_route` | deep_iron -> grey_gate -> greenwood -> underground_river -> abyssal_temple |
| `2026042407` | `full_route` | deep_iron -> underground_river -> greenwood -> grey_gate -> abyssal_temple |
| `2026042408` | `full_route` | deep_iron -> underground_river -> grey_gate -> greenwood -> abyssal_temple |
| `2026042409` | `full_route` | underground_river -> greenwood -> deep_iron -> grey_gate -> abyssal_temple |
| `2026042410` | `full_route` | underground_river -> deep_iron -> grey_gate -> greenwood -> abyssal_temple |
| `2026042411` | `full_route` | underground_river -> grey_gate -> greenwood -> deep_iron -> abyssal_temple |
| `2026042412` | `full_route` | underground_river -> greenwood -> grey_gate -> deep_iron -> abyssal_temple |
| `2026042413` | `branch_inclusive` | greenwood -> underground_river secret branch |
| `2026042414` | `branch_inclusive` | deep_iron -> grey_gate -> crystal_cavern branch |
| `2026042415` | `branch_inclusive` | abyssal_temple secret branch |
| `2026042416` | `branch_inclusive` | grey_gate -> greenwood hidden detour branch |

probe corpus 固定为：

| seed | scenarioType | route intent |
| ---: | --- | --- |
| `2026042417` | `route_probe` | greenwood -> grey_gate route hash probe |
| `2026042418` | `route_probe` | deep_iron -> underground_river route hash probe |
| `2026042419` | `late_route_probe` | grey_gate -> abyssal_temple late route hash probe |
| `2026042420` | `late_route_probe` | underground_river -> abyssal_temple late route hash probe |

固定结果：

1. `full_route` 固定为 12 条。
2. `branch_inclusive` 固定为 4 条。
3. `route_probe` 固定为 2 条，`late_route_probe` 固定为 2 条，seed 与 route intent 以 probe corpus 表为准。
4. 最终 `full_route` 12 条的 start-zone 分布固定为 `greenwood_fringe=4`、`deep_iron_pit=4`、`underground_river=4`。
5. `branch_inclusive` 4 条必须覆盖 4 组不同 mandatory/secret 组合；同一 `secretZoneId` 不得在 branch corpus 中重复作为主 branch。
6. `route_probe=2` 与 `late_route_probe=2` 不进入 `branchInclusiveCount` 分母；它们只用于 verifyChanged routing 粒度与 route hash 观测。
7. 所有 seed 均小于 Int32 上限 `2147483647`，`HeadlessRunHarness` 必须按 `Long` 解析并在 report 中输出原始 seed 字符串。
8. `grey_gate_depths` 与 `abyssal_temple` 在 `LONG_RUN_SMOKE` 与 `MAPGEN_DIFF` preset 下不得作为 start zone；`HeadlessRunHarness` 在 run 起始检查 start zone，违反时 fail fast。
9. 主 branch 定义固定为 route token 中第一个 `secret:<secretZoneId>` marker 对应的 secret zone；该主 branch 不得跨 branch run 重复。

### 5.2 Route diversity metric

新增模型：

```kotlin
data class ZoneRouteHashDiversity(
    val totalRuns: Int,
    val distinctHashes: Int,
    val fullRouteIntentDistinctCount: Int,
    val topHash: String,
    val topHashCount: Int,
    val topHashShare: Double,
    val probeRouteHashSample: List<String>,
)
```

route hash 算法固定为：

```text
routeToken = join(">", visitedZoneIdsWithSecretMarkers)
zoneRouteHash = sha256(routeToken).take(16)
```

执行规则：

1. `visitedZoneIdsWithSecretMarkers` 按真实访问顺序记录 mandatory zone id、secret zone id 和 branch marker。
2. secret branch token 格式固定为 `secret:<secretZoneId>`。
3. late route probe 的 probe-only zone 不写入 terminal route hash；它只写入 `probeRouteHash`。
4. report 必须同时输出 `routeTokenSample` 与 `zoneRouteHash`，便于人工核对 hash 冲突。
5. `probeRouteHash` 独立于 `zoneRouteHash`，进入 supporting 字段 `probeRouteHashSample`；不参与 `zoneRouteHashDiversity` 聚合。
6. `secretZoneId` 不得包含 `:`, `|`, `>`；`ContentPackIdParser` 或等价 id validator 必须 fail fast。
7. `fullRouteIntentDistinctCount` 统计 12 条 `full_route` 的 route intent 去重数，必须等于 `12`。

新增指标：

| 指标 | 阈值 | metricKind | producer | ownerBaseline | failSemantics |
| --- | ---: | --- | --- | --- | --- |
| `zoneRouteHashDiversity.topHashShare` | `<= 40%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json` | `fail owner gate` |
| `branchInclusiveCount` | `>= 4` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json` | `fail owner gate` |
| `fullRouteCount` | `>= 12` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json` | `fail owner gate` |
| `topologyCategoryDiversityPerSmokeRun.reportOnly` | `>= baseline.topologyCategoryDiversityPerSmokeRun.warningFloor` | `reportOnlyOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json` | `warn only` |
| `fullRouteIntentDistinctCount` | `12` | `supporting` | `longRunLab` | `N/A` | `display only` |

### 5.3 verifyChanged routing

规则：

1. 修改 long-run corpus、route hash、harness runner 时，`verifyChanged` 路由 `:game:longRunLab`。
2. 修改 PR-03 reward/build identity、milestone scoring、affix weight、build identity data 时，`verifyChanged` 路由 `:game:longRunLab` 或等价 owner subset。
3. 修改 PR-04 hidden route hook、secret zone binding、Search cue mapgen 时，`verifyChanged` 路由 `:game:longRunLab` 或等价 owner subset。
4. 普通 client/UI 变更不路由 full long-run。
5. nightly/owner gate 执行完整 wide corpus。
6. `VerifyChangedPlanGate` 必须覆盖本 PR 的 long-run owner case。
7. `scopeCoverageLint` 必须包含正反两类 fixture：long-run owner surface 触发 `:game:longRunLab`，presentation-only client surface 不触发 `:game:longRunLab`。
8. route diversity 的最小 owner subset 必须保留 reward/build/affix/hidden 影响面，不得只看 `game/harness` 目录。
9. `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt` 的文案、role、tone 映射调用面属于 presentation-only client surface；未改变 talent rule、event、harness 或 report 字段时，不得路由 full long-run。

### 5.4 Report 展示

`reportPhase4` 必须输出：

1. `scenarioTypeDistribution`。
2. `zoneRouteHashDistribution`。
3. `zoneRouteHashDiversity.topHashShare`。
4. `branchInclusiveCount`。
5. `topologyCategoryDiversityPerSmokeRun.reportOnly`。
6. `probeRouteHashSample`。
7. `fullRouteIntentDistinctCount`。

## 6. 测试与自证

### 6.1 必测行为

1. `LONG_RUN_SMOKE` 的 `full_route` 样本数为 `12`。
2. `branch_inclusive` 样本数为 `4`。
3. `zoneRouteHashDiversity.topHashShare <= 40%`。
4. `branchInclusiveCount >= 4`。
5. 每条 branch 样本覆盖不同 mandatory/secret 组合。
6. 修改 long-run corpus / route hash / harness runner 时，`verifyChanged` 包含 `:game:longRunLab`。
7. 修改 presentation-only client UI 文件时，`verifyChanged` 不包含 `:game:longRunLab`。
8. 修改 `TalentSidebarPresenter` 的 presentation-only 行为时，`verifyChanged` 不包含 `:game:longRunLab`，但仍需走 client render / golden 相关验证。
9. `reportPhase4` 与 `reportPhase4Only` 输出 route diversity 字段。
10. `fullRouteIntentDistinctCount == 12`。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test longRunLab reportPhase4Only reportPhase4 scopeCoverageLint maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr06
```

必须保留以下自证产物：

1. `build/reports/tests/` 中 `LongRunLabTest`、`LongRunLabFullTest`、`ScopeCoverageLintTest`、`VerificationImpactAnalyzerTest`、`VerifyChangedBuildContractTest` 的结果。
2. `tools/build/reports/` 中 `longRunLab`、`reportPhase4Only` producer 产物。
3. `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` canonical report 产物。
4. `build/reports/verification/scope-coverage/` 和 `build/verification/verify-changed/` 产物。
5. `build/whitebox/phase4-v4-pr06/evidence/` 中人工白盒截图、日志、manual record。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

已有游戏 Validation Mode 改造要求：

1. 本 PR 必须接入 PR-00 的 `PHASE4_V4_FAST` section，scenario id 固定为 `phase4-v4-pr06`。
2. `prepare-primary-scene` 必须在现有游戏内 validation session 中打开 route diversity summary 面，展示 `scenarioTypeDistribution`、`zoneRouteHashDistribution`、branch-inclusive route samples。
3. `prepare-secondary-scene` 必须打开 verifyChanged routing summary 面，展示 long-run owner surface 与 presentation-only client surface 的路由差异。
4. `show-evidence-summary` 必须列出 `full_route=12`、`branch_inclusive=4`、`topHashShare <= 40%`、`verifyChanged` routing 四组证据。
5. route diversity summary 必须读取 `longRunLab` / `reportPhase4` 产物或同源 runtime summary，不得由 client 写死数字。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr06`
4. preset：`MAPGEN_DIFF`
5. seed：`2026042436`
6. runtime home：`build/whitebox/phase4-v4-pr06/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr06/evidence`
8. manual record：`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr06-long-run-route-diversity.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr06
```

2. 执行 `build/whitebox/phase4-v4-pr06/launch-packaged-app.sh` 启动 packaged app，Computer Use 目标 app 固定为 `com.ktome.client`。
3. 按 `build/whitebox/phase4-v4-pr06/cua-runbook.md` 打开 validation overlay，执行 `PHASE4_V4_FAST / prepare-primary-scene`。
4. 截图记录 `scenarioTypeDistribution`，确认 `full_route=12`、`branch_inclusive=4`。
5. 截图记录 `zoneRouteHashDistribution` 与 `zoneRouteHashDiversity.topHashShare <= 40%`。
6. 截图记录至少 3 条 branch-inclusive route intent，确认 mandatory/secret 组合不同。
7. 执行 `PHASE4_V4_FAST / prepare-secondary-scene`，截图记录 long-run owner surface 会路由 `:game:longRunLab`，presentation-only client surface 不路由 `:game:longRunLab`。
8. 执行 `PHASE4_V4_FAST / show-evidence-summary`，确认证据清单与本节文件名一致。
9. 保存证据：
    - `phase4-v4-pr06-scenario-distribution.png`
    - `phase4-v4-pr06-route-hash-diversity.png`
    - `phase4-v4-pr06-branch-inclusive-routes.png`
    - `phase4-v4-pr06-verifychanged-routing.png`
    - `phase4-v4-pr06-app.log`

通过标准：

1. 玩家和开发者能从 report/validation 面确认 long-run 覆盖多路线。
2. route diversity 指标不依赖单一路线跑绿。
3. PR preflight 没有被 wide corpus 无差别拖慢。
4. manual record 写明 packaged app 路径、runtime home、seed、输入序列、截图路径和结论。

### 6.4 统一验证框架关系

本 PR 是验证代表性修复，不是内容扩张。`longRunLab`、`reportPhase4Only`、`scopeCoverageLint`、`verifyChanged` 是 blocking 证据；人工白盒只证明 route diversity summary 和 routing contract 对开发者可见，不能替代 owner gate。

### 6.5 玩家体验 Golden Path

1. 开发者打开 validation route diversity summary 时，必须直接看到 `full_route=12`、`branch_inclusive=4`、`route_probe=2`、`late_route_probe=2`。
2. summary 必须展示至少 3 条 branch-inclusive route token，且 mandatory/secret 组合不同。
3. summary 必须展示 `routeTokenSample` 与 `zoneRouteHash`，能人工判断同 hash 是否来自同一路径。
4. verifyChanged routing summary 必须同时展示 long-run owner surface 命中和 presentation-only client surface 未命中。
5. 修改 reward/build/affix/hidden route 相关文件时，开发者必须看到 long-run owner subset 被纳入 plan。

## 7. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test longRunLab reportPhase4Only reportPhase4 scopeCoverageLint maintainabilityLint verifyChanged
```

## 8. 完成定义

1. `full_route` 样本数为 `12`。
2. `branch_inclusive` 样本数为 `4`。
3. `zoneRouteHashDiversity.topHashShare <= 40%`。
4. `branchInclusiveCount >= 4`。
5. `reportPhase4` 输出 route diversity 字段。
6. 修改 long-run corpus / route hash / harness runner 时，`verifyChanged` 包含 `:game:longRunLab`。
7. 修改 reward/build/affix/hidden route owner surface 时，`verifyChanged` 包含 long-run owner subset。
8. 修改 presentation-only client UI 文件时，`verifyChanged` 不包含 `:game:longRunLab`。
9. 修改 `TalentSidebarPresenter` 的 presentation-only 行为时，`verifyChanged` 不包含 `:game:longRunLab`，但 `goldenScreenshot / clientSmoke` 仍按 UI 变更执行。
10. `scopeCoverageLint` 和 `VerifyChangedPlanGate` 测试覆盖上述正反路由。
11. `reportPhase4` 输出 `routeTokenSample` 与 `zoneRouteHash`。
12. `fullRouteIntentDistinctCount == 12`。
13. 没有新增图片计划文件。
14. 没有新增音频计划文件。
