# Phase 5 Unified Verification 与开发规范

**阶段**: `Phase 5 / P5-A ~ P5-C`  
**定位**: `Phase 5` 验证框架、任务注册、phase report 与后续功能接入规范  
**前置文档**:

1. [roadmap.md](./roadmap.md)
2. [2026-03-13-phase5-tactical-ai-stability-and-release.md](./2026-03-13-phase5-tactical-ai-stability-and-release.md)
3. [2026-03-13-phase5-regression-checklist.md](./2026-03-13-phase5-regression-checklist.md)
4. [../2026-04-04-unified-white-box-verification-framework.md](../2026-04-04-unified-white-box-verification-framework.md)
5. [../opt/pr/2026-04-12-unified-verification-pr-07-phase5-domain-spec-and-phase-report-design-freeze.md](../opt/pr/2026-04-12-unified-verification-pr-07-phase5-domain-spec-and-phase-report-design-freeze.md)
6. [../opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md](../opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md)
7. [../opt/pr/README.md](../opt/pr/README.md)

---

## 1. 直接结论

`Phase 5` 从第一天起统一采用 **single-source verification catalog**，不再复制 `Phase 4` 早期的散装路径。

固定原则：

1. `Phase 5` 新 task 不得直接以 `Test + @Tag + build.gradle.kts` 手工 glue 作为唯一注册方式。
2. `Phase 5` 的 domain、tier、node DAG、impact scope、artifact 路径、baseline/metric 归属、phase aggregate 收录关系，必须只在一处声明。
3. `reportPhase5` 是 `Phase 5` 的唯一 phase aggregate 入口，canonical 产物路径固定为 `tools/build/reports/verification/phase5/report-phase5-summary.{json,md}`；`reportPhase5Only` 是 artifact-only rebuild alias。
4. 若较早的跨 phase 文档仍出现 `phase5Report` 命名，以本文件及 `docs/phase5/*` 当前权威链为准；`Phase 5` 不再引入平行 phase report 语义。
5. 若未来需要兼容 alias、fallback 或 sibling aggregate/report task，必须从同一 shared helper / declarative generation path 派生；不得复制新的 `tasks.register<Test>` family。
6. `contentPackHarness`、`longRunLab`、`LootBalanceLab` 等现有 `Phase 3/4` 产物只能作为 **legacy upstream provider** 被声明引用，不得在 `Phase 5` 再复制第二套 runner。
7. 后续 `Phase 5` 新功能或 `Phase 5` 之后的系统开发，若命中 verification/report/gate，必须继续复用这一套 catalog 规范。

---

## 1.1 继承自 `PR-01 ~ PR-05` 的已验证经验

`Phase 5` 不是重新设计验证框架，而是**在 `PR-01 ~ PR-05` 已经验证过的路径上直接起步**。下列经验是后续所有新开发都必须显式继承的：

1. `PR-01`
   - 统一 task foundation、domain spec、node spec、cacheable inputs/outputs 必须先落，再接具体 domain。
   - build-logic 与运行时 registry 的职责必须分离，不能把业务规则推进 Gradle plugin。
2. `PR-02`
   - 新 domain 若进入日常开发主回路，就必须同时交付 `inputScopes`、`verifyChanged` 路由与 scope coverage lint。
   - `verifyChanged` 默认先命中最便宜的 preflight，再决定 owner/full；不能直接把所有改动推到最慢任务。
3. `PR-03`
   - baseline schema、evaluation split、report-only aggregation 必须在 domain 接线前固定。
   - phase aggregate 只能消费 artifact，不得通过 `dependsOn` 间接重跑 producer。
4. `PR-04`
   - deterministic owner domain 必须收敛到统一 writer / artifact contract，不能每个域再发明一套 summary/report 结构。
   - 同一套 owner task、baseline、metric、phase aggregate inclusion 若分散维护，必须有一致性 lint；否则后续必然漂移。
5. `PR-05`
   - 统计域与长时域默认按 shard cache 设计，而不是先写单体 runner 再补复用。
   - baseline-only / evaluation-only / report-only 场景必须是 first-class path。
   - fingerprint 必须覆盖真实 runtime closure，同时避免把 report-only 变更混进 kernel key。
6. `PR-06`
   - sibling aggregate/report task 若只在 `taskName / includeTag / outputDir / aggregateRole / compatAlias` 这类显式参数上有差异，必须复用 shared generation path，而不是复制整组 Gradle DSL。
   - canonical aggregate、artifact-only rebuild alias 与未来可能存在的 compat/fallback 语义必须显式分离，默认 gate 不得残留 alias-only artifact。
   - build contract test 必须优先校验生成参数与 semantic invariant，不得把“大段 DSL 文本切片”当长期 contract。

换句话说，`Phase 5` 不只是采用新命名，而是必须继承：

1. `Gradle-native task foundation`
2. `impact analysis + verifyChanged`
3. `kernel / evaluation / render / aggregate` 分层
4. `artifact-only phase report`
5. `shard cache + evaluation-only reuse`
6. `registry consistency lint`

---

## 2. 单一真源与职责边界

### 2.1 单一真源位置

`Phase 5` 及之后的新 verification wiring 统一落在：

```text
config/verification/catalog-v2/
  domains/
  phases/
```

其中：

1. `domains/*` 负责 domain contract。
2. `phases/phase5.*` 负责 `reportPhase5` 与 phase gate contract。

### 2.2 Catalog 必须声明的字段

每个 `Phase 5` domain 至少必须声明：

1. `domainId`
2. `phaseIds`
3. `publicTaskId`
4. `moduleTaskPath`
5. `workloadClass`
6. `supportedTiers`
7. `defaultTier`
8. `executionNodes`
9. `impactScopes`
10. `artifactBinding`
11. `baselineBinding`
12. `ownerMetricIds`
13. `aggregationRole`
14. `legacyUpstreamRefs`

### 2.3 代码层职责

1. `build-logic`
   - 只负责根据 catalog 生成任务与执行宿主
   - 不持有 `Phase 5` 业务规则、metric 公式或 artifact 解析细节
2. `tools/src/main/kotlin/com/ktome/tools/verification/*`
   - 只负责 catalog loader、generic engine、strategy registry、impact analysis、baseline comparator、aggregate runner
3. `tools/src/main/kotlin/com/ktome/tools/phase5/*`
   - 只放 `Phase 5` domain-specific artifact reader、evaluation strategy、owner metric provider、report presenter

### 2.4 明确禁止

1. 新增 `Phase5MetricCatalog`、`Phase5OwnerBaselineRegistry`、`Phase5DomainArtifactRegistry` 这一类 phase 专用第二套 registry。
2. 在 `build.gradle.kts` 再手写 `Phase 5` task inventory、report input 列表或 verifyChanged allowlist。
3. 为 `BalanceLab`、`packageRelease`、`contentPackHarness` 新建与上游 batch/harness 并行的第三套统计或 compatibility runner。

---

## 3. Canonical Phase 5 Domains

| domainId | canonical task | tiers | workloadClass | 主要产物 | 关键 metric / gate | legacy upstream |
| --- | --- | --- | --- | --- | --- | --- |
| `tactical-ai` | `tacticalAiHarness` | `OWNER` | `DETERMINISTIC_SCENARIO` | `summary.json` `cases.jsonl` `report.md` `artifacts/` | `actionFamilyMatchRate` `illegalTargetCount` `aiTraceHashConsistencyRate` | `bossHarness` 只作为场景语义来源，不作为 owner gate |
| `replay` | `replayHarness` | `OWNER` | `DETERMINISTIC_SCENARIO` | `summary.json` `cases.jsonl` `report.md` `artifacts/` | `runSemanticHashConsistencyRate` `runTraceHashConsistencyRate` | `longRunLab`、recorded runs、content pack sample runs |
| `perf` | `perfSmoke` | `FULL` | `LONG_RUNNING_SYSTEM` | `summary.json` `samples.jsonl` `report.md` | `p50FrameTimeMs` `p95FrameTimeMs` `frameSpikeViolationCount` | 可读 `clientSmoke` / `longRunLab` 场景配置，不复制其执行器 |
| `soak` | `soakRun` | `NIGHTLY` `RELEASE` | `LONG_RUNNING_SYSTEM` | `summary.json` `samples.jsonl` `report.md` `artifacts/` | `heapDriftMb` `maxGcPauseMs` `p1Fps` `crashCount` | `longRunLab` scenario matrix、`contentPackHarness` compatibility matrix |
| `qa-localization` | `localizationQa` | `OWNER` | `STATIC_GRAPH` | `summary.json` `report.md` | `unresolvedKeyCount` `placeholderMismatchCount` `truncationCount` | `contentPackHarness` pack manifest / locale payload |
| `qa-accessibility` | `accessibilityQa` | `OWNER` | `DETERMINISTIC_SCENARIO` | `summary.json` `cases.jsonl` `report.md` | `contrastViolationCount` `keyboardFlowFailureCount` `colorOnlySignalCount` | package/release UI fixtures |
| `balance` | `balanceLab` | `FULL` `NIGHTLY` | `STATISTICAL_BATCH` | `summary.json` `cases.jsonl` `report.md` | `clearRateByBuild` `medianTurnsByBuild` `outlierBuildCount` | `longRunLab` + `lootBalanceLab` 必须以 legacy upstream provider 方式复用 |
| `release` | `packageRelease` | `RELEASE` | `LONG_RUNNING_SYSTEM` | `summary.json` `report.md` `artifacts/` | `packageBuildSuccess` `launchSmokeSuccess` `installDocCompleteness` | `contentPackHarness`、`localizationQa`、`accessibilityQa`、`reportPhase5` |

补充规则：

1. `contentPackHarness` 不是 `Phase 5` 新 domain，但它是 `Phase 5` release gate 的 mandatory upstream provider。
2. `longRunLab` 与 `lootBalanceLab` 继续归属于 `Phase 3/4`，`Phase 5` 只能通过 legacy provider 引用其 artifact。
3. 所有 `OWNER/FULL/NIGHTLY/RELEASE` tier 必须在 catalog 中显式声明，不允许在实现里临时推断。

---

## 4. `reportPhase5` Contract

### 4.1 Canonical Commands

`Phase 5` 聚合入口固定为：

```bash
./gradlew reportPhase5
./gradlew reportPhase5Only
```

规则：

1. `reportPhase5` 是正式 aggregate gate。
2. `reportPhase5Only` 只做 artifact-only rebuild，不触发 producer。
3. canonical 产物路径固定为 `tools/build/reports/verification/phase5/report-phase5-summary.{json,md}`。
4. 不再引入 `phase5Report` 作为平行公开命名；若未来需要兼容 alias，也只能是 `reportPhase5` 的别名，不能成为第二套语义。
5. 若未来兼容 alias 或 fallback task 与 canonical 共享输出目录，则 plain `reportPhase5` 后不得残留 alias-only artifact；否则视为 phase gate contract 未收口。

### 4.2 Inputs

`reportPhase5` 必须同时消费两类输入：

1. `Phase 5 owned domain artifacts`
   - `tacticalAiHarness`
   - `replayHarness`
   - `perfSmoke`
   - `soakRun`
   - `localizationQa`
   - `accessibilityQa`
   - `balanceLab`
   - `packageRelease`
2. `legacy upstream artifacts`
   - `contentPackHarness`
   - `longRunLab`
   - `lootBalanceLab`

legacy upstream 只允许通过 catalog 中的 `legacyUpstreamRefs` 声明接入；`reportPhase5` 实现中不得直接写死 source path。

### 4.3 `reportPhase5` Summary 最小字段

`reportPhase5` 的 `summary.json` 至少固定：

1. `phaseId`
2. `buildId`
3. `generatedAt`
4. `taskCount`
5. `ownedInputCount`
6. `legacyInputCount`
7. `ownerMetricCount`
8. `failedGateCount`
9. `unexpectedRegressionCount`
10. `approvedDebtCount`
11. `artifactReuseRate`
12. `domainCacheHitRate`
13. `slowestDomain`
14. `topInvalidationReasons`
15. `releaseReadiness`

### 4.4 `reportPhase5` Markdown 最小章节

必须固定：

1. `Summary`
2. `Owner Metrics`
3. `Stability & Perf`
4. `Replay & Death Analysis`
5. `Localization & Accessibility`
6. `Release Readiness`
7. `Legacy Upstream Inputs`

---

## 5. 开发规范

### 5.1 新增 Phase 5 功能时的接线顺序

只要新功能命中 verification/gate/report，接线顺序固定为：

1. 先在 `catalog-v2` 增加 domain 或扩展已有 domain。
2. 再增加 `artifactReaderId` / `evaluationStrategyId` / `ownerMetricProviderId`。
3. 再由 build-logic 自动生成任务。
4. 最后才写 runner / harness / report presenter。

禁止反向顺序：

1. 先写 `Test + @Tag`
2. 先在 `build.gradle.kts` 注册 root task
3. 最后才补 catalog

补充硬约束：

1. 若该功能需要日常增量开发入口，必须同步补 `verifyChanged` coverage 与 scope coverage lint。
2. 若该功能需要 phase aggregate 指标，必须先定义 `reportPhase5` inclusion、owner metric ids 与 summary schema，再实现 runner。
3. 若该功能是统计型或长时型验证，必须先定义 cache layering、fingerprint 边界与 evaluation-only 行为，再实现执行器。

### 5.2 新增 task 的最小清单

每新增一个 `Phase 5` task，必须同时补齐：

1. domain entry
2. tier entry
3. impact scopes
4. artifact binding
5. baseline or explicit no-baseline policy
6. owner metrics or explicit no-owner-metric declaration
7. `reportPhase5` inclusion mode
8. verifyChanged coverage
9. checklist 命令与验收项
10. cache layering / fingerprint policy
11. artifact-only rebuild behavior
12. shared generation path / helper ownership
13. semantic build contract test

### 5.3 适用于 Phase 5 之后的新功能

从本规范生效起，后续新功能若满足以下任一条件，必须复用同一套 catalog：

1. 需要 root 公开 gate task
2. 需要 phase aggregate
3. 需要 baseline / expected debt / budget threshold
4. 需要 verifyChanged routing
5. 需要 report-only rebuild

### 5.4 文档同步规则

若 `Phase 5` verification contract 发生变化，必须同步更新：

1. [roadmap.md](./roadmap.md)
2. [2026-03-13-phase5-tactical-ai-stability-and-release.md](./2026-03-13-phase5-tactical-ai-stability-and-release.md)
3. [2026-03-13-phase5-regression-checklist.md](./2026-03-13-phase5-regression-checklist.md)
4. 本文

---

## 6. 工作包映射与工作量

本规范对应的测试改造工作量按 `8~10` 人日估算，作为 `Phase 5` 开发前置基础设施。

| 工作包 | 对应 checkpoint | 目标 |
| --- | --- | --- |
| `P5-V1` | `P5-A` | 建立 `catalog-v2`、Phase 5 domain contract、task generation 与 `reportPhase5` aggregate skeleton |
| `P5-V2` | `P5-A` | 接入 `tactical-ai`、`replay` 两个 owner domain |
| `P5-V3` | `P5-B` | 接入 `perf`、`soak`、`qa-localization`、`qa-accessibility` |
| `P5-V4` | `P5-C` | 接入 `balance`、`release`，并补 legacy upstream bindings |
| `P5-V5` | `P5-C` | 完成 checklist/doc lint、verifyChanged 派生、`reportPhase5Only` 稳定化 |

默认要求：

1. `P5-V1` 必须先于任何 `Phase 5` 业务实现落地。
2. `P5-V2 ~ P5-V4` 可以跟业务实现并行，但新 domain 不得脱离 catalog 先行。
3. `P5-V5` 必须在 `P5-C` 封板前完成。

### 6.1 需要显式避免的历史回退

后续实现若出现以下任一情况，默认视为回退到 `PR-01 ~ PR-05` 已经证明错误的旧路径：

1. 新 domain 先落 `Test + @Tag`，之后再补 catalog
2. baseline 改动触发重型 kernel 重跑
3. phase report 通过 `dependsOn` 或隐式 producer 依赖触发上游执行
4. cache key 混入 report/template/无关 phase 输入，导致本该命中的场景频繁失效
5. 统计域缺失 shard cache 或 evaluation-only 路径
6. owner task / baseline / metric / aggregate inclusion 分散在多处维护但没有一致性 lint

---

## 7. 验收标准

文档冻结完成后，后续实现必须满足：

1. `Phase 5` 新任务全部能从 catalog 自动推导 task wiring。
2. `reportPhase5` 的输入、metric catalog、summary schema 不再需要第二套 phase 专用 registry。
3. `contentPackHarness`、`longRunLab`、`lootBalanceLab` 仅以 legacy provider 方式被复用，不会被 `Phase 5` 再复制 parallel runner。
4. `Phase 5` 之后的新 verification domain 继续沿用本文，而不是重新引入 phase 专用 registry。
