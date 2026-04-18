# Phase 5 Regression Checklist

统一的 `Phase 5` verification/task/report wiring 与 `reportPhase5` contract，以 [2026-04-14-phase5-unified-verification-and-development-spec.md](./2026-04-14-phase5-unified-verification-and-development-spec.md) 为权威。

## 0. Development Preflight

后续 `Phase 5` 开发默认先走增量 preflight，再决定是否进入完整 release 回归：

```bash
./gradlew verifyChanged
./gradlew maintainabilityLint
```

约束：

1. `verifyChanged` 是默认开发主回路；新 domain、新 report wiring、catalog/build 接线、governance 文档变更都必须先经过 impact routing。
2. `maintainabilityLint` 只在 non-trivial Kotlin 结构/边界、verification/report/gate/catalog/build wiring、repo governance 变更时强制执行；它是 quick preflight，不替代完整 release gate。
3. 若 `maintainabilityLint` 或 review 命中 `option-sprawl / helper-sprawl / second-authority / temp-path-without-expiry`，必须先解释 owner / contract / boundary，或直接修掉，再继续后续实现。
4. `Boolean` 参数、默认参数矩阵、`Helper / Utils / Manager`、compat path、第二真源、没有 `debt(id)` 与删除条件的临时逻辑，在 `Phase 5` 默认按阻塞项处理。
5. 共享 PR CI 也应先跑 `verifyChanged`，避免 repo 文档里的默认 preflight 只停留在本地开发约定。
6. nightly automation 可以把 `maintainabilityLint` 切到 report-only，但仍应保留 `scopeCoverageLint` 与 aggregate smoke，持续观察 routing / governance / artifact freshness 漂移。

## 1. Automated Verification

```bash
./gradlew test
./gradlew :core:test
./gradlew tacticalAiHarness
./gradlew replayHarness
./gradlew perfSmoke
./gradlew soakRun
./gradlew localizationQa
./gradlew accessibilityQa
./gradlew balanceLab
./gradlew contentPackHarness
./gradlew packageRelease
./gradlew reportPhase5
```

说明：

1. `reportPhase5` 是 `Phase 5` 的 canonical aggregate gate，默认产物路径固定为 `tools/build/reports/verification/phase5/report-phase5-summary.{json,md}`。
2. `reportPhase5Only` 只做 artifact-only rebuild，不触发 producer；若较早文档仍出现 `phase5Report` 命名，以本 checklist 与 `Phase 5` unified verification spec 为准。
3. 若后续为 `Phase 5` 新增 sibling aggregate/report alias 或 fallback 入口，必须复用 shared helper / declarative generation path，并用 semantic build contract test 锁定 canonical 行为。
4. `maintainabilityLint` 不是 `Phase 5` release checklist 的默认重型 gate，但它是后续开发的 mandatory preflight；只要命中 `Phase 5` Kotlin 结构、verification/report/build wiring 或治理接线，就不能跳过。

### 必须检查的结果

1. `tacticalAiHarness`
   - `12` 个固定场景全部完成
   - 期望动作族匹配率 `>= 85%`
   - 非法目标选择 `0`
   - 相同 build/seed 的 `aiTraceHash` `100%` 一致
2. `replayHarness`
   - `20` 个固定 run 的 `runSemanticHash` 与 `runTraceHash` `100%` 一致
   - 事件计数、turn 计数与原 run 完全一致
3. `perfSmoke`
   - `3` 个场景、各 `30` 次采样全部在预算内
   - 单场景无 `> 10%` 的异常尖峰
4. `soakRun`
   - `8h` 无崩溃、无 OOM
   - warmup 后 `heap drift < 50 MB`
   - `max GC pause <= 50 ms`
   - `p1Fps >= 30`
   - `atlas/audio handle delta <= 3`
5. `localizationQa`
   - unresolved key / placeholder mismatch / truncation 全部为 `0`
   - `zh-CN` / `en-US` + 示例 content pack 全部通过
6. `accessibilityQa`
   - 正文对比度 `>= 4.5:1`
   - 默认最小字号 `>= 14px` 或可调
   - 纯键盘主流程通过
   - 关键信息不只依赖颜色
7. `balanceLab`
   - `8` 个 canonical build × `10` seed 完整跑完
   - canonical build 通关率在 `20% ~ 70%`
   - 成功 run 的 `medianTurns` 离群度 `<= ±25%`
8. `contentPackHarness`
   - tactical AI / perception / replay / death analysis / perf 全链路通过
9. `packageRelease`
   - 安装包可生成、可启动
   - 已知问题、安装说明、验证摘要齐全
10. `reportPhase5`
   - 必须收录全部 `Phase 5` owned domains 与声明的 legacy upstream inputs
   - summary 必须保留 `failedGateCount / ownerMetricCount / artifactReuseRate / domainCacheHitRate / releaseReadiness`
   - owner metric catalog、legacy upstream inventory 与 release summary 最小字段必须可追溯
   - canonical summary / markdown 默认位于 `tools/build/reports/verification/phase5/report-phase5-summary.{json,md}`

## 2. Fixed-Seed Harness Verification

### 2.1 Tactical AI Fixed Scenarios

1. 固定 `12` 个场景：
   - `4` 个精英遭遇
   - `4` 个 Boss encounter
   - `4` 个潜行/仇恨/最后已知位置场景
2. 检查：
   - 走位
   - 技能时机
   - 仇恨切换
   - telegraph 响应
   - `UNAWARE / SUSPICIOUS / ALERT / SEARCHING` 转换
   - `HateFocus` 来源、置信度与衰减
   - `selectionReason = UTILITY_BEST / DSL_OVERRIDE / FALLBACK`
3. 同一 build/seed 连续运行两次，`aiTraceHash` 必须完全一致。

### 2.2 Replay / Death Analysis Run

1. 固定 `20` 个已录制 run，其中至少：
   - `10` 个死亡 run
   - `5` 个 Boss 胜利 run
   - `5` 个示例 content pack run
2. 检查：
   - `ReplayHeader.schemaVersion`
   - `ReplayFrame` / `ReplayEvent` 计数
   - `runSemanticHash` / `runTraceHash`
   - `DeathAnalysis.turn`
   - `killer / ability / damageType / hpBefore / combatTrace / last5Turns / activeEffects / suggestions`
3. `DeathSuggestion` 必须是 typed key + args，不允许出现直接拼接文案。

### 2.3 Soak Baseline Run

1. 固定 soak 配置：
   - `4` 职业 × `2` 种族 × `4` seed
   - `8h` 总预算
   - 采样间隔 `30s`
2. 组合策略：
   - `profession × race × seed` 全组合按顺序轮转
   - 单个组合运行到通关、死亡或总预算耗尽
   - 超时组合记为 `TIMEOUT`，但不会单独判定 harness 失败
3. 检查：
   - 内存曲线
   - atlas/audio 句柄
   - 异常与卡死
   - FPS
   - `p1Fps`
   - draw calls
   - texture bindings
   - FOV / A* 耗时
   - GC 停顿
4. warmup `30min` 后，`heap drift` 必须 `< 50 MB`。

### 2.4 BalanceLab

1. 固定 `8` 个代表性 build：
   - 每个 build 固定 `10` 个 seed
   - `Normal` 难度
2. 检查：
   - `clearRateByBuild`
   - `medianTurnsByBuild`
   - `deathCauseHistogram`
   - `resourceUsageByBuild`
   - `outlierBuilds`
3. canonical build 的 clear rate 不得 `< 20%` 或 `> 70%`。

## 3. Manual White-Box Steps

统一白盒验证框架架构、artifact/report 合同、AI 消费协议与人工一致性策略，以 [../2026-04-04-unified-white-box-verification-framework.md](../2026-04-04-unified-white-box-verification-framework.md) 为权威。

本节只保留 `Phase 5` 封版前必须人工校准的玩家路径动作。`Phase 5` 的战术 AI、replay、perf、soak、localization、accessibility 等 domain 属于固定场景域，正式自动化主路径使用 scenario corpus，不套用 `5 seed / >=3 类差异` 规则。

1. 从安装包启动游戏，完成一次新开局。
2. 进入一场中后期 Boss 战，确认：
   - 战术 AI 的走位、留技能、换目标可感知提升
   - telegraph 与最终动作一致
3. 用具备隐匿能力的职业做一次潜行接敌与脱战，确认：
   - AI 转入 `SEARCHING`
   - `lastKnownPosition` 被正确消费
   - `STEALTH` 未被“无伤 AoE”错误打破
4. 对精英使用 `TAUNT`，确认：
   - 目标在持续期间只追嘲讽源
   - `TAUNT` 结束后恢复普通目标选择
5. 读取一局失败 run 的 death analysis 和 replay，确认：
   - replay 可重放
   - 死因可解释
   - `runSemanticHash` / `runTraceHash` 与 per-frame hash 都可追溯
6. 切换 `zh-CN` / `en-US`、调整字体或可读性选项，确认：
   - 关键 UI、日志、背包、结算页无术语漂移
   - Accessibility 设置真实生效
7. 加载示例 content pack，重复执行第 `2`、`5`、`6` 步。

## 4. Golden / Batch Update Strategy

1. `P5-W1` 合入后，所有 AI golden 必须带 `phase: P5` 与 `aiMode: tactical`。
2. `P5-W2` 合入后，潜行/仇恨/最后已知位置场景必须全量重录；旧 `Phase 3` stealth trace 不得混用。
3. `P5-W4` 开始建立 replay golden；它独立于 `CombatTrace` golden 存档。
4. `balanceLab`、`soakRun`、`contentPackHarness` 都必须保留 batch 摘要，不得只保单条结论。

## 5. Reporting Template

每次封版回归必须记录：

1. `buildId`
2. `phaseId`
3. 平台与运行环境
4. `seed / profession / race / zone route`
5. `contentPackIds`
6. tactical AI 观察结论
7. replay / death analysis 结论
8. perf / soak 结果
9. localization / accessibility 结果
10. `balanceLab` 摘要
11. 安装包验收结果
12. `runSemanticHash / runTraceHash / aiTraceHash`
13. `reportPhase5` summary / markdown 路径与对应 owner metric 结论
   - 默认检查 `tools/build/reports/verification/phase5/report-phase5-summary.{json,md}`
