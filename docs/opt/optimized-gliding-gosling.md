# Repo-Local 测试性能监控系统：PR 级开发文档

## 1. 目标与边界

### 1.1 目标

本方案要在 K-ToME 当前 unified verification 基础上，补一条**repo-local、lane-based、verification-aware** 的 task 性能监控链路，解决以下问题：

1. 目前仓库虽然已经在 verification artifact 中保留了 `durationMillis`，但**没有跨次可比较的本地基线**。
2. 当前无法系统回答：
   - 本次哪些 task 变慢了；
   - 这是代码退化，还是 cache / workload / input 变化；
   - 哪些 task 可以作为“性能改善”证据，哪些 task 不应该拿来做结论。
3. 现有 Phase 4 / Phase 5 unified verification 已经形成了 task registry、cache、artifact reuse、aggregate/report contract；性能监控必须建立在这套真源上，而不是再造一层“通用 Gradle 事件监听器 + 自定义解释”。

### 1.2 非目标

本方案**不做**以下事情：

1. 不做 test class / method 粒度性能分析。
2. 不做 `perfSmoke` / `soakRun` 这类运行时帧率、内存、句柄监控。
3. 不做趋势图、Web UI、Gradle Enterprise / Develocity 接入。
4. 不做团队共享基线；v1 只做本地 `gitignored` 基线。
5. 不修改 `verifyOwner` / `verifyChanged` / `phase4Report` / `reportPhase5` 的既有语义。
6. 不为了拿 fingerprint 使用 Gradle internal API。
7. 不把根 `build.gradle.kts` 的 tag/task 列表升级成新的长期 authoritative inventory。

### 1.3 当前仓库约束

实施时必须遵守这些已核实的仓库事实：

1. `build-logic` 已经是 `java-gradle-plugin`，源码位于：
   - `build-logic/src/main/java/com/ktome/build/verification/`
   - `build-logic/src/test/java/com/ktome/build/verification/`
   新增插件应沿用这条 Java 路径，不额外引入 Kotlin build-logic 子体系。
2. 当前 task authority 已经分层：
   - verification domain / routing / cache / artifact contract 真源在 `tools/src/main/kotlin/com/ktome/tools/verification/`
   - `Phase 4` producer inventory 真源在 `tools/src/main/resources/phase4/aggregation-manifest.yaml`
   - `Phase 5` future catalog / aggregate contract 真源在 `docs/phase5/2026-04-14-phase5-unified-verification-and-development-spec.md`
3. `reportPhase4Only`、`phase4ReportOnly` 这类 task 是 artifact-only aggregate consumer，本身不适合拿来证明 cache 收益；真实重任务仍然是 `lootBalanceLab`、`whiteBoxLoot`、`longRunLab` 等。
4. 根 `.gitignore` 已经忽略 `.gradle/` 与 `build/`；v1 默认可以把本地状态落在 `.gradle/test-perf/` 下，不需要另建并行缓存根。
5. 涉及 `build-logic` 的 PR，必须补跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew -p build-logic test
./scripts/verify-bootstrap.sh
```

### 1.4 文档映射

本方案只定义 repo-local test perf monitor 的实现路径，不替代现有 verification authority。

固定映射：

1. `AGENTS.md` 继续定义仓库级模块边界、验证纪律与高风险红线。
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md` 继续定义 `Phase 4` owner gate、canonical evidence 与 aggregate contract。
3. `docs/phase5/2026-04-14-phase5-unified-verification-and-development-spec.md` 继续定义 `Phase 5` unified verification / aggregate future contract。
4. 本文只负责“如何在本地对 task 执行成本做可比较监控”，不新增 phase gate、不改 owner metric、也不提升 task 分类字符串为新的跨仓库真源。

### 1.5 contract 边界

以下名词在本文里都只代表 perf monitor 内部实现约定，不应外溢成 repo-level contract：

1. `workloadClass`
2. `LIGHT_AGGREGATE`
3. `HEAVY_VERIFICATION`
4. `reportPhase4Only` / `phase4ReportOnly` 在 perf monitor 内的 aggregate 分类

这些分类只能服务于“是否适合作为性能证据、如何解释时延变化”，不能替代 `VerificationTaskRegistry`、aggregation manifest、phase checklist 或 owner report 的权威语义。

### 1.6 全局阶段门禁

本开发文档采用**严格串行 PR** 策略。

固定规则：

1. 任何时刻只允许推进当前 PR，不并行推进下一个 PR。
2. 当前 PR 的**必跑命令、必看产物、必过验收标准全部满足后**，才允许进入下一 PR。
3. 任一命令失败、产物不一致、或出现 contract 漂移，必须先在当前 PR 收口，不得带着已知失败进入下一阶段。
4. 所有 Gradle 命令必须串行执行，不得并行跑多个 Gradle 进程。

---

## 2. 设计总览

### 2.1 设计结论

这套监控系统不能按“纯 BuildService 监听所有 task 完成事件 + 全局 previous/current”直接落。

最终方案固定为：

1. **leaf-task only**
   - 只监控真正执行的 leaf verification task，不把 root alias 当 perf authority。
2. **lane-based baseline**
   - 基线按“可比较 invocation lane”滚动，不做全仓库唯一 `previous/current`。
3. **verification-aware**
   - 对 `VerificationTask` / `VerificationReportTask` 读取已有 typed metadata 与 artifact；
   - 对普通 `Test` 只补最小必要事实。
4. **local-first**
   - v1 只承诺本地基线对比；
   - CI 最多做 report-only，不承诺跨次 baseline compare。

### 2.2 架构摘要

```text
build-logic
  com.ktome.build.testperf
    ├─ TestPerfPlugin
    ├─ TestPerfExtension
    ├─ TestPerfBuildService
    ├─ TestPerfTaskMetadataRegistry
    ├─ TestPerfArtifactReader
    ├─ TestPerfLaneManager
    ├─ TestPerfComparator
    └─ TestPerfReporter

tools
  verification artifacts / metadata / cache summaries
    └─ 继续作为事实来源，被 TestPerfArtifactReader 回读
```

### 2.3 不允许的 shortcut

1. 不允许在 `build.gradle.kts` 再手写一份长期 monitored task inventory。
2. 不允许用正则或 task name 规则替代 `VerificationTaskRegistry` / manifest / future catalog 的 authority。
3. 不允许为 perf monitor 再引入 sidecar baseline schema，复制 `VerificationSummary` / `VerificationMetadata` 里的语义字段。
4. 不允许把 root alias 与 `:tools:*` / `:game:*` leaf task 双重计数。
5. 不允许把 `reportPhase4Only` 之类启动成本占主导的 task 当成“cache 提升多少”的主证据。

---

## 3. PR 拆分总览

| PR | 主题 | 主要模块 | 目标产出 | 进入下一 PR 的门槛 |
| --- | --- | --- | --- | --- |
| `PR-01` | Gradle-native sampling foundation + lane baseline | `build-logic`, root `build.gradle.kts` | lane 目录、leaf task 样本、`current/previous` 轮转骨架 | build-logic tests、bootstrap verify、lane 基线行为全绿 |
| `PR-02` | verification-aware enrichment + workload 建模 | `build-logic`, `tools` | `VerificationTask` / `VerificationReportTask` typed metadata、普通 `Test` 最小 workload | 三类 task 样本都能写出正确记录且无第二真源 |
| `PR-03` | comparator / reporter / root-cause hints / 本地告警 | `build-logic` | `NO_BASELINE/INCOMPARABLE/NORMAL/WARN/ALERT/INFO`、Markdown 报告、heavy/light 区分 | 重跑同 lane 能稳定比较，根因提示命中预期 |
| `PR-04` | CI report-only mode + 文档收口 + Phase 5 handoff note | `build-logic`, docs | `ci-report-only` 模式、README / docs 收口、后续接 `reportPhase5` 的边界说明 | report-only 模式不污染 baseline，文档与实现一致 |

补充规则：

1. `PR-01` 之前不做 comparator / root-cause。
2. `PR-02` 之前不扩 verification artifact schema。
3. `PR-03` 之前不启用任何 `failOnAlert` 或告警语义。
4. `PR-04` 不改 GitHub Actions 默认工作流语义；只建立 mode 和接线说明，不把 CI 持久化基线问题顺手做大。

---

## 4. 全局开发与验证纪律

### 4.1 环境前置

每次开始验证前都先执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

### 4.2 统一验证纪律

1. 所有 Gradle 命令串行执行。
2. `build-logic` 变更默认必须补跑：

```bash
./gradlew -p build-logic test
./scripts/verify-bootstrap.sh
```

3. 任何涉及 `VerificationTask` / `VerificationReportTask` 读取逻辑的改动，都必须至少覆盖三类样本：
   - 一个 `VerificationTask`
   - 一个 `VerificationReportTask`
   - 一个普通 `Test`
   - 若要锁住 `LIGHT_AGGREGATE` 这种 plain `Test` aggregate，再额外补一个当前仓库的 aggregate `Test` 样本（如 `:tools:reportPhase4Only`）
4. 任何涉及 lane、compare、report 的改动，都必须至少做一次同 lane 连续两次运行验证。
5. 任何 PR 的手工验收都必须保留：
   - 实际执行命令
   - lane 路径
   - `current.json / previous.json`
   - `latest.md`

### 4.3 统一通过标准

任一 PR 结束时，必须同时满足：

1. 代码编译与功能测试通过。
2. 新增 / 修改的 build-logic functional test 能锁住该 PR 引入的核心 contract。
3. 本地最小真实任务链路验证通过。
4. 文档中的“当前 PR 退出标准”全部满足。

---

## 5. PR-01：Gradle-native Sampling Foundation + Lane Baseline

### 5.1 目标

建立最小可用的本地 task perf 基线系统，只解决以下问题：

1. 识别 monitored leaf task。
2. 记录 wall-clock duration、outcome、invocation context。
3. 生成 lane 目录，并在同 lane 内轮转 `current.json / previous.json`。

### 5.2 非目标

`PR-01` **不做**：

1. 不读取 `VerificationSummary` / `VerificationMetadata`。
2. 不做 comparator。
3. 不做 root-cause hints。
4. 不扩 artifact schema。
5. 不做 CI mode。

### 5.3 设计约束

#### 5.3.1 监控对象

默认只监控 leaf task：

1. `VerificationTask`
2. `VerificationReportTask`
3. 显式 opt-in 的普通 `Test`

root alias 只保留在 invocation context，不写成主要样本。

补充规则：

1. `test`、`unitAndToolsGate`、`gameHarnessGate`、`clientAndAssetsGate`、`verifyOwner`、`verifyChanged`、`whiteBoxVerify` 这类 lifecycle / aggregator task 不进入 `tasks[]`。
2. 它们只进入 `invocationContext.requestedTaskPaths`，用于解释本次 build 是如何被触发的。
3. `tasks[]` 只保留真正执行且可比较的 leaf task 记录。

#### 5.3.2 lane 规则

laneId 固定按下列信息计算：

```text
schemaVersion
sorted(requestedLeafTaskPaths)
buildCacheEnabled
configurationCacheEnabled
parallelEnabled
maxWorkers
javaMajorVersion
osFamily
```

刻意不把 `git head / branch / dirty` 纳入 laneId。

补充规则：

1. 读取 `previous.json` 时先校验 `schemaVersion`。
2. 若 schemaVersion 不匹配，则视为 `NO_BASELINE`，不做跨版本迁移，也不阻塞当前 build。

#### 5.3.3 存储结构

```text
.gradle/test-perf/
  lanes/
    <laneId>/
      current.json
      previous.json
      current.incomplete.json
      history/<runId>.json
      reports/
        latest.md
        latest.incomplete.md
        <runId>.md
```

### 5.4 代码范围

#### `build-logic` 新增

- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfPlugin.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfExtension.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfBuildService.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfFinalizeFlowAction.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfTaskMetadataRegistry.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfLaneManager.java`
- `build-logic/src/main/java/com/ktome/build/testperf/records/RunRecord.java`
- `build-logic/src/main/java/com/ktome/build/testperf/records/TaskRecord.java`

#### `build-logic` 测试

- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfPluginFunctionalTest.java`
- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfLaneManagerTest.java`

#### build 脚本

- `build-logic/build.gradle.kts`
- `build.gradle.kts`

#### 依赖决策

`build-logic` 的 JSON 序列化固定使用 `Gson`，不手写 JSON，也不引入第二套 YAML/JSON 混用方案。

原因：

1. 仓库已经有 `gsonVersion=2.11.0`。
2. `build-logic` 当前是 Java 源码路径，Gson 足够覆盖本方案的 record/POJO 序列化。
3. `snakeyaml` 继续只用于 YAML 读取场景，不混进本地 perf 状态写入。

### 5.5 必须实现的行为

1. root apply `com.ktome.build.testperf`。
2. plugin 在 configuration 阶段收集 monitored leaf task。
3. task 完成事件监听必须通过 `BuildEventsListenerRegistry.onTaskCompletion(...)` 注册 `BuildService<...> + OperationCompletionListener`。
4. build 收尾落盘不得依赖 `gradle.buildFinished` 或 `BuildService.close()`；统一通过 `FlowScope + FlowProviders.getBuildWorkResult()` 驱动 finalize action。
5. 所有 `durationMillis` 统一来自 `TaskFinishEvent.getResult().getEndTime() - getStartTime()`。
6. `daemonReused` 只作为 best-effort heuristic，默认按 JVM uptime 阈值判断，且只用于 `INFO` 级 warmup 提示，不参与硬 gate。
7. 若当前 build 中任一 monitored leaf task `FAILED`：
   - 写 `current.incomplete.json`
   - 写 `reports/latest.incomplete.md`
   - 不轮转 `current.json -> previous.json`
   - 控制台输出“本次 build 有失败 leaf，基线不更新”
8. 若当前 build 中所有 monitored leaf task 都成功：
   - 为当前 lane 写 `current.json`
   - 若已有旧 `current.json`，则轮转到 `previous.json`
   - 首次 lane 运行输出 `baseline established`
9. lane 轮转必须带文件锁：
   - 使用 `.gradle/test-perf/lanes/<laneId>/.lock`
   - 抢锁失败时跳过写入，不报失败
10. 不同 lane 之间绝不共享 `previous/current`。
11. `--configuration-cache` 打开时不允许出现弃用/不兼容接线。

### 5.6 必须新增的测试

#### 单测 / functional test

1. laneId 相同的两次 run 正确轮转 `current -> previous`。
2. laneId 不同时各自独立建目录。
3. root alias + leaf task 混合调用时，不出现重复 leaf 样本。
4. 没有 monitored leaf task 时，不写 lane 文件。
5. monitored leaf task 失败时只写 `current.incomplete.json`，不污染成功基线。
6. `--configuration-cache` 打开时 functional test 通过，且不出现弃用接线。
7. 并发写 lane 时不损坏 `previous/current`。

### 5.7 必跑命令

按顺序串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew -p build-logic test
./scripts/verify-bootstrap.sh
./gradlew :tools:hiddenContentHarness
./gradlew :tools:hiddenContentHarness
./gradlew :tools:reportPhase4Only
./gradlew help
```

### 5.8 必看结果

1. `:tools:hiddenContentHarness` 第一次运行后：
   - 新建 lane 目录
   - 只有 `current.json`
   - 控制台输出 `baseline established`
2. 第二次运行同命令后：
   - `previous.json` 出现
   - 新 `current.json` 覆盖
3. 运行 `:tools:reportPhase4Only` 后：
   - 创建新的 lane
   - 不复用 `hiddenContentHarness` 的 baseline
4. 运行 `./gradlew help` 后：
   - 不产生新的 lane 目录
   - 不更新既有 baseline

### 5.9 退出标准

以下全部满足，才允许进入 `PR-02`：

1. `build-logic` tests 全绿。
2. `verify-bootstrap.sh` 全绿。
3. lane 轮转行为与文档一致。
4. 没有 alias 双计数。

量化判据：

1. `./gradlew -p build-logic test` 中与 `testperf` 相关的 functional / lane tests 全绿。
2. 连续两次 `./gradlew :tools:hiddenContentHarness` 后，目标 lane 下 `previous.json` 存在，且 `schemaVersion == 1`。
3. `./gradlew help` 不产生任何 `.gradle/test-perf/lanes/` 变化。

---

## 6. PR-02：Verification-Aware Enrichment + Workload 建模

### 6.1 目标

在 `PR-01` 的基础上，把样本从“纯 wall-clock task record”升级成“带 verification 语义的可比较记录”。

### 6.2 非目标

`PR-02` **不做**：

1. 不做对比分类。
2. 不做告警。
3. 不做 CI mode。
4. 不修改 `VerificationTaskRegistry` 路由逻辑。

### 6.3 设计约束

#### 6.3.1 不允许第二真源

对 `VerificationTask` / `VerificationReportTask`，必须分两层读取：

1. **Task-input 层**：来自 `AbstractVerificationExecTask`
   - `domainId`
   - `tier`
   - `nodeId`
   - `inputSnapshotHash`
   - `declaredCacheStatusOnWrite`
   - `outputDir`
2. **Artifact 层**：来自现有 `summary.json / metadata.json`
   - `snapshotHash`
   - `cacheStatus`
   - `outputPaths`
   - `reportOnly`
   - `totalTests`
   - `failedTests`
   - `durationMillis`
   - `workloadClass`
   - `declaredWorkloadClasses`
   - `selectedNodeWorkloadClass`

读取顺序固定为：

1. artifact 优先
2. task-input 作 fallback

禁止事项：

1. 不允许把 task input 字段名直接当作 artifact schema 字段名使用。
2. 不允许为了 perf monitor 再写一份平行 verification sidecar。

#### 6.3.2 workload 建模

`testCount` 不能作为全部 task 的统一 workload。

固定规则：

1. 普通 `Test`
   - 记录 `tests.total / tests.failed`
   - `workloadClass = "UNIT_TEST"`（仅作为 perf monitor 内部字符串，不扩成 repo-level 枚举）
2. verification domain
   - 直接复用现有 authority：
     - `workloadClass`
     - `declaredWorkloadClasses`
     - `selectedNodeWorkloadClass`
     - `reportOnly`
   - `workloadCount` 只作为补充数值，不新建第二套 workload 枚举

补充规则：

1. `VerificationReportTask` 不通过新枚举表达 report-only；统一使用 `summary.reportOnly == true`。
2. heavy / light 分层在 `PR-03` 基于：
   - `reportOnly`
   - `workloadClass`
   - task path contract
   派生，不新增 repo-level 第二套 workload authority。

### 6.4 代码范围

#### `build-logic` 新增 / 修改

- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfArtifactReader.java`
- `build-logic/src/main/java/com/ktome/build/testperf/records/VerificationRecord.java`
- `build-logic/src/main/java/com/ktome/build/testperf/records/WorkloadRecord.java`
- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfArtifactReaderTest.java`
- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfPluginFunctionalTest.java`

#### `tools` 最小 additive 改动

只允许对现有 JSON 追加**可选字段**：

- `workloadCount`
- `artifactReuseSource`
- `evaluationDurationMillis`

不允许：

1. 新建 `test-perf-sidecar.json`
2. 把已有 `VerificationSummary` / `VerificationMetadata` 的事实复制一份到别的 schema

### 6.5 必须实现的行为

1. `VerificationTask` 样本能带出：
   - `domainId`
   - `tier`
   - `nodeId`
   - `inputSnapshotHash`
   - `snapshotHash`
   - `cacheStatus`
2. `VerificationReportTask` 样本能识别为 `reportOnly == true`，并为 `PR-03` 的 `LIGHT_AGGREGATE` 分类提供足够信息
3. 普通 `Test` 样本能带出 test totals
4. `TaskRecord.kind` 必须正确区分：
   - `VERIFICATION_TASK`
   - `VERIFICATION_REPORT_TASK`
   - `TEST`
5. artifact 字段与 task-input 字段冲突时，以 artifact 为准。

### 6.6 必须新增的测试

1. `VerificationTask` 样本能正确回填 verification block。
2. `VerificationReportTask` 样本能正确读取 report-only 属性。
3. 普通 `Test` 样本能正确读取 test count。
4. 缺少可选字段时，reader 退化行为稳定，不报假错。
5. artifact 字段覆盖 task-input fallback 的优先级被测试锁住。

### 6.7 必跑命令

按顺序串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew -p build-logic test
./scripts/verify-bootstrap.sh
./gradlew :tools:test
./gradlew :tools:hiddenContentHarness
./gradlew :tools:verifyContractLintPreflight
./gradlew :tools:verifyContractLintPreflightReport
./gradlew :tools:whiteBoxLoot
./gradlew :tools:reportPhase4Only
```

### 6.8 必看结果

1. `:tools:hiddenContentHarness`
   - `TaskRecord.kind == VERIFICATION_TASK`
   - verification block 包含 `domainId=hidden`、`tier=OWNER`
2. `:tools:whiteBoxLoot`
   - `TaskRecord.kind == TEST`
   - 有 `tests.total / tests.failed`
   - 若 summary 有 cache / workload 字段，则能正确回读
3. `:tools:verifyContractLintPreflightReport`
   - `TaskRecord.kind == VERIFICATION_REPORT_TASK`
   - `summary.reportOnly == true`
   - `metadata.workloadClass` 与现有 verification metadata 一致
4. `:tools:reportPhase4Only`
   - `TaskRecord.kind == TEST`
   - 仅作为当前仓库 aggregate `Test` 样本存在，不冒充 `VerificationReportTask`

### 6.9 退出标准

以下全部满足，才允许进入 `PR-03`：

1. 三类 task 样本都能被正确区分。
2. 没有新增第二真源或 parallel sidecar contract。
3. 可选字段缺失时 reader 行为稳定。
4. `tools` additive 字段没有破坏现有 task / report contract。

量化判据：

1. `current.json` 中 `:tools:hiddenContentHarness` 条目的 `verification.domainId == "hidden"`。
2. `current.json` 中 `:tools:verifyContractLintPreflightReport` 条目的 `verification.reportOnly == true`。
3. `current.json` 中普通 `Test` 条目存在 `tests.total` 字段。

---

## 7. PR-03：Comparator / Reporter / Root-Cause Hints / 本地告警

### 7.1 目标

在已有 lane + enrichment 基础上，交付可用的本地比较与报告系统。

### 7.2 非目标

`PR-03` **不做**：

1. 不做 CI baseline compare。
2. 不把告警接进现有 phase gate。
3. 不改 `verifyOwner` / `verifyChanged` 的 task 选择逻辑。

### 7.3 设计约束

#### 7.3.1 比较前提

只有满足以下条件，两个样本才参与正式 compare：

1. 同一个 `laneId`
2. 同一个 `comparableKey`
3. 两次都是实际执行样本

否则只能输出：

- `NO_BASELINE`
- `INCOMPARABLE`
- `INFO`

`comparableKey` 固定定义为：

1. 普通 `Test`
   - `taskKind + taskPath`
2. `VerificationTask` / `VerificationReportTask`
   - `taskKind + taskPath + domainId + tier + (nodeId ?: "default")`

补充规则：

1. lane 已经吸收 `buildCacheEnabled / parallel / maxWorkers / javaMajorVersion`，这些信息不重复进入 `comparableKey`。
2. `tier` 或 `nodeId` 变化时，应视为新 key，进入 `INCOMPARABLE`，不得误报性能退化。

#### 7.3.2 heavy / light task 分层

固定分层：

1. `HEAVY_PRODUCER`
2. `HEAVY_EVALUATION`
3. `LIGHT_AGGREGATE`
4. `SMALL_TEST`

`reportPhase4Only`、`phase4ReportOnly` 一律按 `LIGHT_AGGREGATE` 处理。

判定顺序：

1. `VerificationReportTask` 且 `summary.reportOnly == true` 的 task，默认先归 `LIGHT_AGGREGATE`
2. 当前仓库中显式约定的 aggregate `Test`（如 `:tools:reportPhase4Only`、`:tools:phase4ReportOnly`）也归 `LIGHT_AGGREGATE`
3. 再结合 `workloadClass` 与显式 task path contract 做细分
4. 不允许把 report-only task 或 aggregate `Test` 升成 `HEAVY_*`

#### 7.3.3 根因提示只接可稳定实现的信号

v1 允许的 root-cause hints：

1. cache 状态翻转
2. artifact reuse 变化
3. input snapshot 变化
4. workload 变化
5. lane 环境变化
6. cold daemon
7. 机器负载高
8. 未命中常见模式

明确不做：

- CPU time / wall-clock ratio 推断

### 7.4 代码范围

- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfComparator.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfReporter.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfRootCauseAnalyzer.java`
- `build-logic/src/main/java/com/ktome/build/testperf/records/DiffRecord.java`
- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfComparatorTest.java`
- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfRootCauseAnalyzerTest.java`
- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfPluginFunctionalTest.java`

### 7.5 必须实现的行为

1. classification 固定输出：
   - `NO_BASELINE`
   - `INCOMPARABLE`
   - `NORMAL`
   - `WARN`
   - `ALERT`
   - `INFO`
2. 控制台输出 lane、上次可比 run、每个 task 的结论。
3. Markdown 报告至少包含：
   - `Invocation Context`
   - `Comparable Tasks`
   - `Warnings / Alerts`
   - `Cache And Artifact Reuse Signals`
   - `Non-comparable Samples`
   - `Heavy Task Comparison Summary`
4. 报告必须显式区分：
   - 可作为性能改善证据的 heavy task
   - 仅供观察的 light aggregate / small test
5. `reports/latest.md` 生成时，同时归档 `reports/<runId>.md`；归档与 JSON history 一起受 `historyLimit` 清理。

### 7.6 阈值规则

#### Heavy task

| 信号 | 等级 |
| --- | --- |
| 绝对增量 `>= 30s` 且相对增量 `>= 50%` | `ALERT` |
| 绝对增量 `>= 5s` 且相对增量 `>= 25%` | `WARN` |
| workload 变化 `>= 10%` | `WARN` |
| cache / artifact reuse 状态翻转 | `INFO` |

#### Light aggregate

| 信号 | 等级 |
| --- | --- |
| 绝对增量 `>= 10s` 且相对增量 `>= 100%` | `WARN` |
| cache / input 变化 | `INFO` |

### 7.7 必须新增的测试

1. 首次 run 输出 `baseline established`。
2. 同 lane 第二次 run 正确生成 comparable summary。
3. cache flip 命中 `INFO` 而非误判退化。
4. `daemonReused=false` 时命中 warmup 提示。
5. `LIGHT_AGGREGATE` 不会被误写成 heavy cache 收益证据。
6. `:tools:reportPhase4Only` / `:tools:phase4ReportOnly` 的 `LIGHT_AGGREGATE` 分类有 contract test 锁定。

### 7.8 必跑命令

按顺序串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew -p build-logic test
./scripts/verify-bootstrap.sh
./gradlew :tools:whiteBoxLoot
./gradlew :tools:whiteBoxLoot
./gradlew --stop
./gradlew :tools:whiteBoxLoot
./gradlew :tools:reportPhase4Only
./gradlew :tools:reportPhase4Only
```

### 7.9 必看结果

1. 第一次 `:tools:whiteBoxLoot`
   - 控制台输出 `baseline established`
2. 第二次 `:tools:whiteBoxLoot`
   - 有 comparable summary
   - heavy task 被归入 `Heavy Task Comparison Summary`
3. `:tools:reportPhase4Only` 连续两次运行
   - 记录为 `LIGHT_AGGREGATE`
   - 不出现在 heavy cache 收益主结论中
4. `--stop` 之后再跑 `:tools:whiteBoxLoot`
   - 报告命中 cold daemon 提示

### 7.10 退出标准

以下全部满足，才允许进入 `PR-04`：

1. classification 与文档一致。
2. Markdown 报告章节齐全。
3. 根因提示只使用稳定可实现的信号。
4. heavy / light 分层没有误导性输出。

量化判据：

1. `latest.md` 同时包含：
   - `Comparable Tasks`
   - `Warnings / Alerts`
   - `Heavy Task Comparison Summary`
2. `:tools:reportPhase4Only` 不出现在 heavy cache 收益主结论中。
3. `--stop` 后的 `:tools:whiteBoxLoot` 样本在报告中带有 cold daemon 提示。

---

## 8. PR-04：CI Report-Only Mode + 文档收口 + Phase 5 Handoff Note

### 8.1 目标

为后续 CI 接入建立最小边界，但不把“CI 持久化基线”在本轮做大。

### 8.2 非目标

`PR-04` **不做**：

1. 不改 GitHub Actions 默认工作流。
2. 不做跨次 CI baseline compare。
3. 不把 test perf 结果接成 blocking gate。
4. 不把这套系统直接塞进 `reportPhase5`。

### 8.3 设计约束

#### 8.3.1 `ci-report-only` 语义

新增 mode：

```text
-Ptestperf.mode=ci-report-only
```

固定语义：

1. 只输出本次 run 摘要。
2. 不要求存在 `previous.json`。
3. 不承诺 lane compare。
4. 不污染本地 baseline 轮转逻辑。

同时增加：

```text
-Ptestperf.mode=off
```

语义：

1. 完全 bypass test perf monitor
2. 不写任何 lane / report 文件
3. 适合 IDE 背景构建或一次性关闭

#### 8.3.2 文档落位

必须同步更新：

1. `build-logic/README.md`
2. `docs/INDEX.md`
3. `docs/verification/README.md`
4. 新文档：
   - `docs/verification/test-task-performance-monitoring.md`

文档必须明确：

1. 这是 task perf monitor，不是 runtime perf/soak system。
2. `Phase 5 perfSmoke / soakRun` 仍然走独立 domain。
3. 若未来要接入 `reportPhase5`，只能通过 additive summary ingestion，不得反向污染 unified verification registry。

### 8.4 代码范围

- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfExtension.java`
- `build-logic/src/main/java/com/ktome/build/testperf/TestPerfReporter.java`
- `build-logic/src/test/java/com/ktome/build/testperf/TestPerfPluginFunctionalTest.java`
- `build-logic/README.md`
- `docs/INDEX.md`
- `docs/verification/README.md`
- `docs/verification/test-task-performance-monitoring.md`

### 8.5 必须实现的行为

1. `ci-report-only` 模式下可运行。
2. 该模式不会要求 `previous.json`。
3. 该模式不会覆盖本地 lane baseline。
4. README 与 docs 文档清楚描述：
   - 目标
   - 非目标
   - 本地模式
   - CI mode
   - off mode
   - 未来与 `reportPhase5` 的边界

### 8.6 必须新增的测试

1. `ci-report-only` 模式下 functional test 通过。
2. 没有 baseline 时仍能输出 current-run summary。
3. 不会执行 lane compare / baseline rotation。
4. `mode=off` 时不发生任何 test perf IO。

### 8.7 必跑命令

按顺序串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew -p build-logic test
./scripts/verify-bootstrap.sh
./gradlew :tools:reportPhase4Only -Ptestperf.mode=ci-report-only
./gradlew :tools:whiteBoxLoot -Ptestperf.mode=ci-report-only
./gradlew :tools:reportPhase4Only -Ptestperf.mode=off
```

### 8.8 必看结果

1. 两条命令都能输出本次 run 摘要。
2. 不要求存在 previous baseline。
3. 不会把 CI mode 的结果写回本地 lane `previous/current`。
4. `mode=off` 下不发生任何 test perf IO。
5. 文档与实现行为一致。

### 8.9 退出标准

以下全部满足，本方案才算完整收口：

1. `ci-report-only` 模式功能稳定。
2. 文档与实现一致。
3. 本地 baseline 模式与 CI report-only 模式边界清楚。
4. 没有把 Phase 5 runtime perf/soak 语义误并进来。

量化判据：

1. `docs/INDEX.md` 已收录新文档入口。
2. `docs/verification/README.md` 与 `docs/verification/test-task-performance-monitoring.md` 已创建并说明目录定位。
3. `mode=off` 下不产生任何 `.gradle/test-perf/` 变更。

---

## 9. 每个 PR 的统一验收模板

后续每个 PR 提交前，必须按以下模板收口：

### 9.1 已执行命令

按真实执行记录填写：

```text
1. ...
2. ...
3. ...
```

### 9.2 已检查产物

至少列出：

```text
- .gradle/test-perf/lanes/<laneId>/current.json
- .gradle/test-perf/lanes/<laneId>/previous.json
- .gradle/test-perf/lanes/<laneId>/current.incomplete.json
- .gradle/test-perf/lanes/<laneId>/reports/latest.md
- .gradle/test-perf/lanes/<laneId>/reports/latest.incomplete.md
```

### 9.3 必答问题

1. 当前 PR 有没有引入新的 authoritative inventory？
2. 当前 PR 有没有复写或复制 `VerificationSummary` / `VerificationMetadata` 语义？
3. 当前 PR 有没有把 light aggregate 错写成 heavy performance evidence？
4. 当前 PR 的 functional tests 是否锁住了新增 contract？
5. 是否满足进入下一 PR 的全部门槛？

若任一问题回答为“没有证据证明已满足”，则不得进入下一 PR。

---

## 10. 最终完成定义

整套 task perf monitor 合格收口，至少应满足：

1. leaf-task only 监控成立，没有 alias 双计数。
2. lane-based baseline 成立，没有伪对比。
3. `VerificationTask` / `VerificationReportTask` / 普通 `Test` 三类记录都能正确建模。
4. comparator / reporter / root-cause hints 行为稳定。
5. heavy task 与 light aggregate 的证据层级明确。
6. `ci-report-only` 模式成立，但没有伪装成跨次 CI baseline。
7. README / docs 文档同步收口，并明确与 `Phase 5 perfSmoke / soakRun` 的边界。

---

## 11. 一句话原则

这套系统的职责不是“监听所有 Gradle task 然后猜发生了什么”，而是：

**在不破坏 K-ToME 现有 unified verification authority 的前提下，把本地可比较的 task 性能事实稳定记录下来。**
