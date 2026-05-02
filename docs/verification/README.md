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

Phase4 v4 开发验证阶梯：

1. 文档合同快路径：`./gradlew acceptanceContractLint`
   - 只检查 v4 PR 文档是否具备 Acceptance Matrix、Gate Budget、canonical artifact 和失败规则。
   - 不替代 owner gate、report gate 或 `verifyChanged`。
2. fast lane
   - 运行 PR 文档声明的 focused unit / lint。
   - fast lane 失败时先补确定性测试或修实现，不直接进入重型 owner gate 试错。
3. owner producer
   - 按 PR owner surface 运行 producer，例如 `whiteBoxLoot`、`lootBalanceLab`、`longRunLab`、`bossHarness`、`contentPackHarness`。
   - producer freshness 必须与数据、schema、harness、report 字段同批刷新。
4. report gate
   - `reportPhase4Only` 与 `reportPhase4` 读取同批 canonical owner evidence。
5. final closure
   - 最后运行 `./gradlew verifyChanged`。
   - `verifyChanged` 仍是最终 PR CI 权威；不得为 v4 PR 再造第二套 impact routing。

边界说明：

1. 这里描述的是 Gradle task perf monitor，不是 runtime perf / soak system
2. `Phase 5 perfSmoke / soakRun` 仍由 `docs/phase5/*` 和对应 domain contract 单独定义
3. 未来若要把 task perf summary 接入 `reportPhase5`，只能做 additive summary ingestion，不能反向污染 unified verification registry 或 `reportPhase5` canonical aggregate contract
4. `testperf`、`full-task-duration-summary` 和 Gate Budget 只用于耗时诊断；不得反向决定 `reportPhase4`、`verifyChanged` 或 owner gate 的 canonical 结论
