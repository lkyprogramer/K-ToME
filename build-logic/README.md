# build-logic

这个目录承载 K-ToME 的仓库级 Gradle 构建基础设施，不属于 `core / game / client / tools` 的运行时规则路径。

当前职责：

1. 提供 `com.ktome.build.verification` 插件。
2. 提供 `com.ktome.build.testperf` 插件，用于 leaf-task 级 test task performance monitor。
3. 提供 `VerificationTask`、`VerificationReportTask`、`LegacyHarnessAdapterTask` 基础类型。
4. 为后续 unified verification 重构提供独立宿主，避免把任务基础设施继续堆在根 `build.gradle.kts`。

运行时 verification contract 与 registry 仍在 `tools/src/main/kotlin/com/ktome/tools/verification/`。

## `com.ktome.build.testperf`

这个插件只监控 Gradle leaf task 的执行时间和复用信号，产物默认写到 `.gradle/test-perf/`。

它不是 runtime perf / soak 系统：

1. 不替代 `Phase 5 perfSmoke / soakRun`
2. 不改变 `verifyOwner` / `verifyChanged` / `reportPhase5` 的既有语义
3. 不把 runtime 性能采样反向塞回 unified verification registry

plain `Test` 默认不会进入监控面。

只有两类任务默认纳入：

1. `VerificationTask`
2. `VerificationReportTask`

普通 `Test` 只有在任务定义处显式 opt-in 后才会进入 baseline。

当前仓库已经显式接入的基础 `Test` 任务包括：

1. `:core:test`
2. `:game:test`
3. `:client:test`
4. `:tools:test`

支持三种模式：

1. 默认模式：`local-baseline`
   - 维护 lane 级 `current.json / previous.json / history / reports`
   - 适合本地连续重跑和 root-cause 观察
2. `-Ptestperf.mode=ci-report-only`
   - 只输出本次 run 摘要
   - 产物写到 `.gradle/test-perf/ci/<laneId>/reports/`
   - 不要求 `previous.json`，也不会覆盖本地 lane baseline
3. `-Ptestperf.mode=off`
   - 完全绕过 test perf monitor
   - 不产生任何 test perf IO

未来如果要接 `reportPhase5`，只能做 additive summary ingestion，不能反向污染 `reportPhase5` 的 canonical aggregate contract。
