# Test Task Performance Monitoring

## 1. 定位

`com.ktome.build.testperf` 是面向 Gradle leaf task 的本地 task perf monitor。

它关注的是：

1. 同 lane 重跑时的 task wall-clock 变化
2. cache / artifact reuse / workload / input snapshot / cold daemon 等稳定信号
3. heavy task 与 light aggregate 的证据层级区分
4. 记录本次 build 的 `requestedTaskPaths` 与实际采样的 `requestedLeafTaskPaths`

它不负责：

1. runtime perf / soak 采样
2. `Phase 5 perfSmoke / soakRun` 指标
3. `reportPhase5` 的 canonical aggregate 语义
4. 跨次 CI baseline compare

补充边界：

1. `verifyChangedPreflight` 的 `build/verification/verify-changed/preflight-task-paths.txt` 与 `task-duration-summary.{json,md}` 属于 routed preflight artifact
2. 这些 artifact 只用于说明本次 PR 轻量入口实际选中了哪些 task、每个 task 耗时多少
3. 它们不是 `.gradle/test-perf/` lane baseline，也不能反向充当 unified verification registry

默认监控面：

1. `VerificationTask`
2. `VerificationReportTask`
3. 显式 opt-in 的普通 `Test`

普通 `Test` 默认不会进入 lane baseline。

当前仓库已显式接入的基础 `test` 任务：

1. `:core:test`
2. `:game:test`
3. `:client:test`
4. `:tools:test`

## 2. 默认模式

默认模式等价于：

```text
-Ptestperf.mode=local-baseline
```

行为：

1. 维护 lane 级 baseline
2. 写入 `.gradle/test-perf/lanes/<laneId>/`
3. 轮转 `current.json / previous.json`
4. 归档 `history/<runId>.json` 与 `reports/<runId>.md`
5. 生成 `reports/latest.md` 与失败场景的 `reports/latest.incomplete.md`

适用场景：

1. 本地连续重跑同一批 leaf task
2. 观察 heavy task 的回归、cache flip、artifact reuse 和 cold daemon 提示

## 3. CI Report-Only 模式

命令行入口：

```text
-Ptestperf.mode=ci-report-only
```

固定语义：

1. 只输出本次 run 摘要
2. 不要求存在 `previous.json`
3. 不承诺 lane compare
4. 不覆盖本地 lane baseline

产物位置：

```text
.gradle/test-perf/ci/<laneId>/reports/latest.md
.gradle/test-perf/ci/<laneId>/reports/<runId>.md
```

这个模式适合：

1. CI 中保留当前 run 的 task perf 摘要
2. 需要 artifact 但不想把 CI 临时执行结果写回本地 baseline 轮转

## 4. Off 模式

命令行入口：

```text
-Ptestperf.mode=off
```

固定语义：

1. 完全绕过 test perf monitor
2. 不注册 task perf IO
3. 不写任何 `.gradle/test-perf/` 文件

适用场景：

1. IDE 背景构建
2. 一次性关闭 monitor

## 5. 与 Phase 5 的边界

`Phase 5 perfSmoke / soakRun` 仍然是独立 domain：

1. 其 contract、artifact 和 aggregate 以 `docs/phase5/*` 为准
2. `reportPhase5` 是 `Phase 5` 的 canonical aggregate gate
3. task perf monitor 未来若要接入 `reportPhase5`，只能通过 additive summary ingestion
4. 不允许把 test perf lane、baseline 或 report-only 目录反向当成 unified verification registry 的权威输入
