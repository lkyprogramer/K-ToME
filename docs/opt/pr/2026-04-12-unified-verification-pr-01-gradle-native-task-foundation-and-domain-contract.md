> 执行前必须先完整阅读并接受：
> `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`
> `docs/2026-04-04-unified-white-box-verification-framework.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/phase4/roadmap.md`

# Unified Verification PR-01 Gradle-Native 任务基础设施与 Domain Contract

**阶段**: `Cross-Phase / Verification Refactor / UVR-W1`  
**优先级**: `P0`  
**工作量评估**: `M`（`2~4` 人日）  
**前置条件**: 无  
**对应问题**:

1. 当前 `tools/build.gradle.kts` 和 `game/build.gradle.kts` 中大量 `tasks.register<Test> + includeTags` 是散装接线。
2. 还没有统一的 domain spec / node spec / workload tier / baseline policy contract。
3. 如果不先把任务基础设施和 contract 定下来，后续 preflight、baseline、phase report 都会继续散装扩张。

---

## 1. 阶段目标

建立后续所有验证重构都会复用的最小基础设施，但**不在本 PR 就迁移重型 domain**。

完成标准：

1. 仓库内存在统一的 `VerificationTask` / `VerificationReportTask` 基础类型。
2. 存在可复用的 `VerificationDomainSpec`、`VerificationNodeSpec`、`VerificationTier`、`BaselinePolicySpec`。
3. 能用一个最小 `STATIC_GRAPH` demo domain 跑通新的任务路径。
4. 当前 root task 不变，旧任务不受影响。

---

## 2. 为什么这个 PR 是第一优先

### 2.1 不先做基础设施，后续所有 PR 都会反复重写

后续 PR 都依赖：

1. 统一 task 类型
2. 统一 domain/node contract
3. 统一 artifact/report 入口

如果没有这层，`verifyLootPreflight`、baseline comparator、report-only 聚合都只能继续写成各自的 task glue。

### 2.2 为什么不一步做完整自定义引擎

本 PR 的策略是**Gradle 原生优先**：

1. 先建立 `build-logic` 插件和自定义 task 类型
2. 先复用 Gradle 的 task graph、input/output、build cache、worker API
3. 暂不引入完整外部 DAG 调度器

这样做的原因：

1. 当前最大痛点还不是“Gradle 不会调度”，而是“验证 contract 没统一”
2. 一步跳到完整 mini build system 风险过高

---

## 3. 本 PR 必须冻结的合同

1. 验证基础设施 contract **不放进 `core`**。
2. 验证基础设施统一放在：
   - `build-logic/` 负责 Gradle task/plugin
   - `tools/src/main/kotlin/com/ktome/tools/verification/` 负责运行时 spec / registry / cache 接口
3. 普通单元测试保持 `JUnit + ./gradlew test`。
4. root alias 先不改名，迁移期保留原公开 task 名。
5. 本 PR 不迁移具体重型 domain，只做基础设施和一个最小 demo。

---

## 4. 范围与非目标

### 4.1 范围

1. 新建 `build-logic` 复合构建
2. 新建统一 verification task/plugin 骨架
3. 新建 domain/node/baseline/cache policy contract
4. 跑通一个最小 `STATIC_GRAPH` demo domain

### 4.2 非目标

1. 不改 `HeadlessRunHarness`
2. 不迁移 `lootBalanceLab / longRunLab / phase4Report`
3. 不实现 shard cache
4. 不实现完整 impact analyzer
5. 不改 `Phase 5` 任务

---

## 5. 技术方案

### 5.1 新建 `build-logic` 复合构建

建议新增：

```text
build-logic/
  settings.gradle.kts
  build.gradle.kts
  src/main/kotlin/com/ktome/build/verification/
    VerificationTaskPlugin.kt
    VerificationTask.kt
    VerificationReportTask.kt
    LegacyHarnessAdapterTask.kt
```

根 `settings.gradle.kts` 需要：

1. `includeBuild("build-logic")`

理由：

1. 避免把构建基础设施直接塞进 `build.gradle.kts`
2. 支持后续扩展 task 类型和 plugin 测试

### 5.2 Domain / Node Contract

建议新增：

```text
tools/src/main/kotlin/com/ktome/tools/verification/
  VerificationDomainSpec.kt
  VerificationNodeSpec.kt
  VerificationTier.kt
  VerificationWorkloadClass.kt
  BaselinePolicySpec.kt
  VerificationArtifactPolicy.kt
  VerificationCachePolicy.kt
```

第一版至少冻结这些类型：

1. `VerificationWorkloadClass`
   - `STATIC_GRAPH`
   - `DETERMINISTIC_SCENARIO`
   - `STATISTICAL_BATCH`
   - `LONG_RUNNING_SYSTEM`
2. `VerificationTier`
   - `PREFLIGHT`
   - `OWNER`
   - `FULL`
   - `NIGHTLY`
   - `RELEASE`
3. `VerificationDomainSpec`
4. `VerificationNodeSpec`
5. `BaselineMode`
   - `STRICT_ZERO_FAILURE`
   - `APPROVED_DEBT_SET`
   - `EXPECTED_FAILURE_CODE_SET`
   - `RELATIVE_BASELINE`
   - `BUDGET_THRESHOLD`

### 5.3 Task 类型

#### `VerificationTask`

职责：

1. 声明 `domainId / tier / inputSnapshotHash / outputDir`
2. 执行单个 verification node
3. 产出：
   - raw result
   - summary
   - metadata

必须使用 Gradle 原生：

1. `@Input`
2. `@OutputDirectory`
3. `inputs.property(...)`
4. `outputs.dir(...)`

#### `VerificationReportTask`

职责：

1. 只读取已存在 artifact
2. 重建 phase summary / report-only 输出
3. 禁止触发 producer

#### `LegacyHarnessAdapterTask`

职责：

1. 在迁移期包装旧 `Test + @Tag` harness
2. 让新 registry 能先把旧任务当成 node 看待
3. 便于新旧系统并行对账

### 5.4 最小 demo domain

本 PR 只选一个最简单的 `STATIC_GRAPH` 域做 demo：

首选：

1. `contractLint`

备选：

1. `localeLint`

理由：

1. 无随机性
2. 无长时依赖
3. 不依赖 `game` harness
4. 最适合验证：
   - new task type
   - domain registry
   - output schema
   - build cache / inputs / outputs

### 5.5 Report schema

本 PR 只定义最小公共字段，不做 baseline 细节：

1. `domainId`
2. `tier`
3. `verdict`
4. `snapshotHash`
5. `cacheStatus`
6. `outputPaths`

baseline/debt 细节放到 `PR-03`。

---

## 6. 推荐改动面

### 6.1 `build-logic`

1. `build-logic/settings.gradle.kts`
2. `build-logic/build.gradle.kts`
3. `build-logic/src/main/kotlin/com/ktome/build/verification/*`

### 6.2 `tools`

1. `tools/src/main/kotlin/com/ktome/tools/verification/*`
2. `tools/build.gradle.kts`

### 6.3 根构建

1. `settings.gradle.kts`
2. `build.gradle.kts`

---

## 7. 实施顺序

### Task 1：建立 `build-logic`

- **目标**：让 verification task/plugin 有独立宿主
- **文件**：`build-logic/*`, `settings.gradle.kts`
- **验收**：
  - 根项目可加载 `build-logic`
  - configuration cache 不被立即破坏

### Task 2：定义 verification contract

- **目标**：冻结 domain/node/workload/baseline mode 的最小数据结构
- **文件**：`tools/src/main/kotlin/com/ktome/tools/verification/*`
- **验收**：
  - 至少一个 demo domain 可声明完整 spec

### Task 3：实现 `VerificationTask` / `VerificationReportTask`

- **目标**：跑通最小 node 执行路径
- **文件**：`build-logic/src/main/kotlin/com/ktome/build/verification/*`
- **验收**：
  - task 可声明 inputs/outputs
  - warm rerun 可命中 Gradle 自带缓存

### Task 4：接入 `contractLint` demo

- **目标**：用最小 domain 验证新体系
- **文件**：`tools/build.gradle.kts`, `build.gradle.kts`
- **验收**：
  - 新任务可产出 summary
  - 不影响旧 `contractLint`

---

## 8. 测试策略

### 8.1 自动化验证

```bash
./gradlew :tools:test
./gradlew contractLint
./gradlew help --task contractLint
```

### 8.2 必测行为

1. `build-logic` 加载成功
2. 新 `VerificationTask` 支持 cacheable inputs/outputs
3. `contractLint` demo domain 可独立运行
4. 旧 `contractLint` 路径未被破坏

### 8.3 推荐新增测试

1. plugin/task 配置测试
2. domain spec 校验测试
3. cache key 生成测试

---

## 9. 风险与 Gotchas

1. **不要把 verification contract 放进 `core`**
   - 会污染规则真源边界
2. **不要在本 PR 就迁移重型 domain**
   - 否则基础设施 bug 和 domain bug 混在一起
3. **不要先改对外 task 名**
   - 会打碎后续 checklist 和开发习惯

---

## 10. 回滚策略

1. `build-logic` 可独立回退，不影响旧 task
2. `contractLint` demo 必须保留旧实现
3. 若新 task 类型破坏 configuration cache 或 build cache，回退到仅保留 contract/type skeleton，不切实际接线
