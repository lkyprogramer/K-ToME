# Verification Docs

这个目录收敛 verification 基础设施和报告消费相关文档。

当前入口：

1. [test-task-performance-monitoring.md](./test-task-performance-monitoring.md)
   - `com.ktome.build.testperf` 的目标、非目标、模式边界和产物位置

共享自动化入口：

1. PR CI 默认先跑 `./gradlew verifyChanged`
   - 复用既有 `VerifyChangedPlanGate` / impact routing，不在 workflow 里再手写第二套治理判断
   - full gate 耗时排查读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`
   - UI-only `ItemRenderSnapshot.specialTierId` diff 可由 semantic classifier 收窄，不触发宽泛 Phase 4 owner fallback
2. nightly harness 默认跑 `./gradlew nightlyGovernanceGate`
   - 包含 `scopeCoverageLint`
   - 包含 `maintainabilityLint` 的 report-only 夜跑
   - 包含 `verifyOwner + mapgenSmoke + solvabilityHarness + reportPhase4`，用于 freshness / aggregate smoke

边界说明：

1. 这里描述的是 Gradle task perf monitor，不是 runtime perf / soak system
2. `Phase 5 perfSmoke / soakRun` 仍由 `docs/phase5/*` 和对应 domain contract 单独定义
3. 未来若要把 task perf summary 接入 `reportPhase5`，只能做 additive summary ingestion，不能反向污染 unified verification registry 或 `reportPhase5` canonical aggregate contract
