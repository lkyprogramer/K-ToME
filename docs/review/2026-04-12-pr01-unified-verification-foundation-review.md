# PR-01 Unified Verification Task Foundation 深度审查报告

> 审查日期：2026-04-12  
> 审查分支：`codex/unified-verification-pr-01-task-foundation`  
> 对照文档：`docs/opt/pr/2026-04-12-unified-verification-pr-01-gradle-native-task-foundation-and-domain-contract.md`  
> 审查角色：资深 Roguelike 游戏开发设计总监 / 系统策划总监 / 玩法体验审查负责人

---

## 0. 总体评定

| 维度 | 评定 | 说明 |
|------|------|------|
| **计划一致性** | ✅ 高度一致 | 四大 Task 全部按序完成，核心 contract 冻结到位 |
| **边界控制** | ✅ 严格遵守 | 未迁移重型 domain，未改 root alias，未污染 core |
| **代码质量** | ✅ 优良 | 结构清晰、分层正确、测试覆盖合理 |
| **风险控制** | ✅ 可控 | 旧 `contractLint` 完整保留，回滚路径清晰 |
| **偏差项** | ⚠️ 存在 5 处小偏差 | 均为低风险，不影响上线，但建议后续修正 |

**综合判定：可以合并。** 下面逐项分析。

---

## 1. 逐项对照：计划 vs 实现

### 1.1 Task 1：建立 `build-logic`

| 计划要求 | 实际实现 | 判定 |
|----------|----------|------|
| 新建 `build-logic/` 复合构建 | ✅ 已建立，含 `settings.gradle.kts` + `build.gradle.kts` + README | 完全一致 |
| 根 `settings.gradle.kts` 添加 `includeBuild("build-logic")` | ✅ 第 4 行 `includeBuild("build-logic")` | 完全一致 |
| 根项目可加载 `build-logic` | ✅ `./gradlew help --task verifyContractLintPreflight` 成功 | 通过验收 |
| configuration cache 不被立即破坏 | ✅ Gradle 正常运行 | 通过验收 |

**偏差 D1（低风险）：语言选择**

计划文档建议目录结构为 `src/main/kotlin/.../*.kt`，实际实现为 `src/main/java/.../*.java`。

- **影响**：无功能影响。Java 在 Gradle plugin 模块中实际上更稳定（避免 Kotlin 版本冲突），这是一个合理的工程决策。
- **建议**：接受当前实现。文档未强制要求 Kotlin。

### 1.2 Task 2：定义 verification contract

| 计划要求 | 实际实现 | 判定 |
|----------|----------|------|
| `VerificationWorkloadClass` 枚举 | ✅ 4 个值完全匹配 | 完全一致 |
| `VerificationTier` 枚举 | ✅ 5 个值完全匹配 | 完全一致 |
| `BaselineMode` 枚举 | ✅ 5 个值完全匹配 | 完全一致 |
| `VerificationDomainSpec` | ✅ 含完整校验逻辑 | 超出预期 |
| `VerificationNodeSpec` | ✅ 含 `dependsOn` contract-only 字段 | 完全一致 |
| `BaselinePolicySpec` | ✅ 存在 | 完全一致 |
| `VerificationArtifactPolicy` | ✅ 存在 | 完全一致 |
| `VerificationCachePolicy` | ✅ 存在 | 完全一致 |
| contract 不放进 `core` | ✅ 经搜索确认 core/ 无泄漏 | 完全一致 |
| contract 统一放在 `tools/src/main/kotlin/com/ktome/tools/verification/` | ✅ 全部 11 个 Kotlin 文件均在此目录 | 完全一致 |

**超出计划的实现（正面）：**

1. `VerificationDomainSpec.init` 含 5 项严格校验（domainId 非空、phaseIds 非空、nodeSpecs 非空、nodeId 唯一、defaultTier 有对应 node、node workloadClass 与 domain 一致、dependsOn 引用合法）。计划只要求"冻结类型"，实际实现了完整的 spec 校验，这是高质量做法。
2. 新增了 `VerificationNodeKind` 枚举（`LEGACY_JUNIT_CLASS_SET` / `REPORT_ONLY`），计划未显式要求但对执行路径分发是必要的。
3. 新增了 `VerificationCli` 作为 Gradle task 到运行时 contract 之间的 CLI 桥梁。计划未显式提及此组件，但这是让 `AbstractVerificationExecTask.javaexec()` 路径跑通的必要胶水。
4. 新增了 `VerificationArtifacts.kt` 含 `VerificationSummary`、`VerificationMetadata` 等 `@Serializable` 输出模型，与计划 §5.5 的 report schema 对应。

### 1.3 Task 3：实现 `VerificationTask` / `VerificationReportTask`

| 计划要求 | 实际实现 | 判定 |
|----------|----------|------|
| `VerificationTask` 声明 `domainId / tier / inputSnapshotHash / outputDir` | ✅ 在 `AbstractVerificationExecTask` 中通过 `@Input` / `@OutputDirectory` 声明 | 完全一致 |
| `VerificationTask` 使用 Gradle 原生 `@Input` / `@OutputDirectory` | ✅ 使用 `@Input`、`@InputFiles`、`@Classpath`、`@OutputDirectory`、`@PathSensitive` | 完全一致 |
| `VerificationTask` 标记 `@CacheableTask` | ✅ | 完全一致 |
| `VerificationReportTask` 只读取已存在 artifact | ✅ `rebuildReport()` 从 `--artifact-input` 读取，report-only 模式不写 raw-result | 完全一致 |
| `VerificationReportTask` 禁止触发 producer | ✅ 使用 `mustRunAfter` 而非 `dependsOn`，不会自动拉起 producer | 完全一致 |
| `LegacyHarnessAdapterTask` 包装旧 harness | ✅ 支持 `selectedClasses` + `selectedTags` | 完全一致 |
| warm rerun 可命中 Gradle 自带缓存 | ✅ 实测第二次运行显示 `UP-TO-DATE` | 通过验收 |

**偏差 D2（低风险）：task 类层次设计**

计划建议 `VerificationTask` 和 `VerificationReportTask` 是两个平级类型。实际实现引入了 `AbstractVerificationExecTask` 基类，三个 task 类型都继承它。

- **影响**：正面。减少重复代码，模板方法模式是合理抽象。
- **建议**：接受。

### 1.4 Task 4：接入 `contractLint` demo

| 计划要求 | 实际实现 | 判定 |
|----------|----------|------|
| 首选 `contractLint` 作为 STATIC_GRAPH demo | ✅ `VerificationTaskRegistry` 注册了 `contractLint` domain | 完全一致 |
| 新任务可产出 summary | ✅ 产出 `summary.json` / `metadata.json` / `raw-result.json` | 完全一致 |
| 不影响旧 `contractLint` | ✅ 旧 `Test("contractLint")` 保留在 `tools/build.gradle.kts:124`，root `contractLint` alias 保留在 `build.gradle.kts:202` | 完全一致 |
| root alias 先不改名 | ✅ 新任务使用 `verifyContractLintPreflight` 命名，旧 `contractLint` 不变 | 完全一致 |

### 1.5 Report Schema（§5.5）

| 计划要求的最小公共字段 | 实际 `VerificationSummary` | 判定 |
|------------------------|---------------------------|------|
| `domainId` | ✅ | 存在 |
| `tier` | ✅ | 存在 |
| `verdict` | ✅ | 存在 |
| `snapshotHash` | ✅ | 存在 |
| `cacheStatus` | ✅ | 存在 |
| `outputPaths` | ✅ | 存在 |

**超出计划的字段（正面）：** `nodeId`、`totalTests`、`failedTests`、`durationMillis`、`reportOnly`。这些是执行可观测性所需的必要信息，不违反"最小公共字段"的精神。

---

## 2. 非目标合规性审查

| 非目标条款 | 是否被违反 | 说明 |
|------------|------------|------|
| 不改 `HeadlessRunHarness` | ✅ 未触及 | — |
| 不迁移 `lootBalanceLab / longRunLab / phase4Report` | ✅ 未触及 | — |
| 不实现 shard cache | ✅ 未实现 | — |
| 不实现完整 impact analyzer | ✅ 未实现 | — |
| 不改 Phase 5 任务 | ✅ 未触及 | — |
| 不把 `dependsOn` 接进执行器 | ✅ `dependsOn` 仅作 contract-only 字段 | — |
| 不为 `LegacyHarnessAdapterTask` 实现 tag-only discovery | ✅ 当前使用显式 `selectedClasses` | — |

**非目标全部守住，零违反。**

---

## 3. 冻结合同审查

| 合同条款 | 是否遵守 | 说明 |
|----------|----------|------|
| verification contract 不放进 `core` | ✅ | `core/` 中无 `VerificationDomainSpec` 等类型引用 |
| Gradle task/plugin 放 `build-logic/` | ✅ | 6 个 Java 文件全在 `build-logic/src/main/java/com/ktome/build/verification/` |
| 运行时 spec/registry 放 `tools/.../verification/` | ✅ | 11 个 Kotlin 文件全在该目录 |
| 普通单测保持 `JUnit + ./gradlew test` | ✅ | `./gradlew :tools:test` 正常运行，不受影响 |
| root alias 先不改名 | ✅ | 旧 `contractLint` alias 完整保留 |
| 本 PR 不迁移重型 domain | ✅ | 仅 `contractLint` 一个 STATIC_GRAPH demo |

---

## 4. 偏差清单与修复建议

### D1：build-logic 使用 Java 而非 Kotlin（无需修复）

- **偏差程度**：仅文件后缀/语言不同
- **风险**：无。Java 在 Gradle plugin 模块中避免 Kotlin 版本冲突，是更稳健的选择
- **建议**：接受现状。如需对齐文档，更新 PR 计划文档中的目录树即可

### D2：引入 `AbstractVerificationExecTask` 基类（无需修复）

- **偏差程度**：额外抽象层
- **风险**：无。减少重复，模板方法模式恰当
- **建议**：接受现状

### D3：`VerificationCli` 未在计划中显式描述（无需修复）

- **偏差程度**：中等——这是一个 ~260 行的非平凡组件
- **风险**：低。作为 `javaexec()` 的入口类，是让 Gradle task 与 tools 运行时解耦的必要桥梁
- **建议**：在 PR 计划文档中补充说明此组件的职责。它事实上承担了"统一 artifact/report 入口"的角色

### D4：`VerificationReportTask` 的"禁止触发 producer"语义为弱保证（建议关注）

- **偏差程度**：低
- **说明**：计划要求 `VerificationReportTask` "禁止触发 producer"。当前实现通过 `mustRunAfter("verifyContractLintPreflight")` 实现顺序约束，但**不建立 `dependsOn`**，因此不会自动拉起 producer——这是正确的。然而如果用户手动先删除 artifact 目录再单独运行 report 任务，会因为 artifact 不存在而抛出 `require` 异常
- **风险**：当前行为是合理的 fail-fast。但语义上"禁止触发"可以更强——例如在 task 配置时就检查 artifactInputs 是否已配置
- **建议**：当前行为可接受，PR-03 baseline 阶段可根据需要加强

### D5：缺少 `build-logic` 的 plugin/task 配置测试（建议补充）

- **偏差程度**：中等
- **说明**：计划 §8.3 "推荐新增测试" 中明确建议 "plugin/task 配置测试"。当前 `build-logic/src/test/` 目录不存在
- **风险**：当前 build-logic 代码量极小（6 个文件，~240 行），逻辑简单，风险可控。但随着后续 PR 扩展 task 类型，缺少 plugin 配置测试会降低迁移信心
- **建议**：建议在 PR-02 或 PR-03 中补充 Gradle TestKit 测试。至少覆盖：
  1. `VerificationTaskPlugin` 正确注册 extension
  2. `AbstractVerificationExecTask` 的 `buildArguments()` 参数拼装
  3. 不同 task 类型的 convention 值正确

---

## 5. 代码质量审查

### 5.1 亮点

1. **`VerificationDomainSpec.init` 校验链**：5 项 `require` 校验覆盖了 nodeId 唯一性、defaultTier 映射、workloadClass 一致性、dependsOn 引用合法性——这在 Roguelike 这类"组合爆炸"规则系统中极为重要。每新增一个 domain，spec 校验就能在构造时拦截配置错误，而不是在运行时产生难以追踪的 NPE
2. **Artifact 三件套分离**：`raw-result.json`（原始执行数据）、`summary.json`（verdict + cache status）、`metadata.json`（domain 配置快照）。report-only 模式正确地不复制 raw-result，只重建 summary + metadata。这为后续 phase report 聚合提供了干净的消费接口
3. **snapshot hash 设计**：基于 SHA-256 的 content-addressed hash，输入包括 build 配置文件 + verification 源码 + 被验证资源文件。这为 Gradle 增量构建提供了准确的失效判断
4. **`CollectingListener` 排序策略**：失败测试排在前面 (`status != "SUCCESSFUL"`)，相同状态按 `uniqueId` 排序——便于人类阅读报告
5. **CLI 参数解析的确定性**：`selectedClasses` 和 `selectedTags` 在构建参数时排序，确保参数顺序不影响 cache key

### 5.2 需要关注的点

1. **`VerificationCli.writeArtifacts` 的 `rawResult` 参数类型**：当前固定为 `LegacyJUnitRawResult`。当 PR-04/PR-05 引入非 JUnit 执行路径时，需要将 raw result 泛化为接口或 sealed class。这不是本 PR 的问题，但需要在后续 PR 计划中标记为必须处理的接口变更
2. **`outputPaths` 使用绝对路径**：`summary.json` 中的 `outputPaths` 值是本地绝对路径。这对本地开发无影响，但如果后续需要在 CI 间共享 artifact，绝对路径不具备可移植性
3. **`VerificationTaskPlugin.apply` 中 `extension.getDefaultGroup().get()` 的调用时机**：`configureEach` 回调中使用 `.get()` 获取 eager 值是安全的（因为 convention 已设置），但如果用户在 `afterEvaluate` 中修改 `defaultGroup`，此处已经读取了旧值。当前无此场景，但 lazy evaluation 更稳健。改为 `task.setGroup(extension.getDefaultGroup().getOrElse("verification"))` 或延迟读取

### 5.3 从 Roguelike 游戏设计角度的审查

从类 ToME 游戏的验证体系角度看：

1. **`STATIC_GRAPH` 作为首个 demo 域是正确选择**。Roguelike 的核心数据管线（物品表、Loot Profile、地图图结构、技能树等）是确定性的，天然适合 static graph 验证。`contractLint` 覆盖的 schema 交叉引用、key namespace 完整性，是游戏内容包系统 (content pack system) 的质量基石
2. **`DETERMINISTIC_SCENARIO` / `STATISTICAL_BATCH` / `LONG_RUNNING_SYSTEM` 三级 workload 分类**，精确对应了 Roguelike 验证的三个维度：
   - 固定种子场景回放（mapgen pilot、战斗回合序列）
   - 统计抽样（loot balance、encounter rate）
   - 长时 soak（内存泄漏、状态机死锁、回合数上限）
3. **五级 Tier 分层**（`PREFLIGHT → OWNER → FULL → NIGHTLY → RELEASE`）是 ToME 级规模项目必需的。当内容包超过 300+ 物品、50+ 地图模板、20+ 职业时，全量验证可能耗时 30 分钟以上；preflight 秒级反馈是开发体验的生命线

---

## 6. 测试覆盖审查

| 测试类 | 覆盖范围 | 质量评价 |
|--------|----------|----------|
| `VerificationDomainSpecTest` (201 行, 6 个用例) | spec 校验：tier 映射、node 唯一性、workload 一致性、依赖引用合法性 | ✅ 优秀。每个校验规则都有对应的反向测试 |
| `VerificationCliTest` (224 行, 4 个用例) | CLI report 模式：正常重建、多 artifact 拒绝、node id 不匹配拒绝、legacy adapter 集成 | ✅ 良好。覆盖了 report-only 核心路径和关键异常路径 |
| `LegacyJUnitClassSetExecutorTest` (26 行, 1 个用例) | executor 基本执行路径 | ⚠️ 较薄。仅覆盖 happy path，缺少失败场景测试 |
| `VerificationDemoProbeTest` (17 行, 2 个用例) | 测试框架自身的 probe 桩 | ✅ 适当。作为 executor 测试的被执行目标 |

**缺失的测试场景（建议后续补充）：**

1. `LegacyJUnitClassSetExecutor` 执行包含失败测试的场景——验证 `failedTests` 计数和 `errorMessage` 采集是否正确
2. `VerificationCli.runVerification` 的端到端 happy path——当前 CLI test 只测了 report 和 legacy-adapter，未测 `run` 命令
3. snapshot hash 为空时的 `requireSnapshotHash()` 异常路径

---

## 7. 回滚安全性审查

| 回滚场景 | 可行性 | 说明 |
|----------|--------|------|
| 删除 `build-logic/` 并恢复 `settings.gradle.kts` | ✅ 安全 | 旧 task 路径完全独立，`contractLint` / `localeLint` 等未被修改 |
| 只删除新 `verifyContractLintPreflight` 任务 | ✅ 安全 | 其余 task 无依赖关系 |
| 只删除 `tools/.../verification/` 目录 | ✅ 安全 | 需同时删除 `build.gradle.kts` 和 `tools/build.gradle.kts` 中的引用 |

**回滚安全性：优秀。** 新旧系统完全并行，零耦合。

---

## 8. 结论与行动项

### 合并判定：**✅ 建议合并**

本 PR 忠实执行了计划文档的全部 4 个 Task，严格遵守了冻结合同和非目标边界。5 处偏差中 D1/D2/D3 为正面偏差（更好的工程决策），D4 为可接受的弱保证，D5 为推荐但非阻塞的测试补充。

### 后续行动项

| 优先级 | 行动项 | 建议时机 |
|--------|--------|----------|
| P2 | 补充 build-logic plugin 配置测试（Gradle TestKit） | PR-02 或 PR-03 |
| P3 | 补充 `LegacyJUnitClassSetExecutor` 失败场景测试 | PR-02 |
| P3 | 补充 `VerificationCli.runVerification` 端到端测试 | PR-02 |
| P3 | 更新 PR-01 计划文档中的目录树（Java vs Kotlin）和 `VerificationCli` 组件描述 | 合并后 |
| P4 | 考虑 `outputPaths` 使用相对路径或不包含路径（仅记录 fileName） | PR-03 |
| P4 | `VerificationTaskPlugin` 中延迟读取 `defaultGroup` | 后续扩展时 |

---

*审查完成。*
