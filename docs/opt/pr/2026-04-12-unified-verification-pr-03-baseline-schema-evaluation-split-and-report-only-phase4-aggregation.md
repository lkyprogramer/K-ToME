> 执行前必须先完整阅读并接受：
> `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`
> `docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/pr/2026-04-12-unified-verification-pr-01-gradle-native-task-foundation-and-domain-contract.md`

# Unified Verification PR-03 Baseline 统一、Evaluation 拆分与 Report-Only Phase 4 聚合

**阶段**: `Cross-Phase / Verification Refactor / UVR-W3`  
**优先级**: `P0`  
**工作量评估**: `M-L`（`3~5` 人日）  
**前置条件**: `UVR-PR-01` 完成，`UVR-PR-02` 至少跑通 `verifyLootPreflight`  
**对应问题**:

1. 当前 baseline/debt/expected failure 语义散装。
2. `Phase4ReportRunner` 体量大、聚合逻辑硬编码多。
3. baseline 变动和 report 模板变动仍可能拖重型 producer。

---

## 1. 阶段目标

把“kernel 结果”“evaluation/baseline 对比”“phase 聚合报告”真正拆开。

完成标准：

1. 统一 baseline schema，带 `schemaVersion`
2. terrain baseline 可迁到新 schema
3. `APPROVED_DEBT_SET / EXPECTED_FAILURE_CODE_SET / RELATIVE_BASELINE / BUDGET_THRESHOLD` 有统一比较器
4. `reportPhase4` 可以只读 artifact 重建
5. 新旧 `phase4Report` 可并行对账

---

## 2. 为什么这个 PR 必须在大规模 domain 迁移前完成

如果不先统一 baseline 和 evaluation：

1. `loot` 仍然只能靠“失败条数”等脆弱语义
2. `terrain` 和 `content-pack` 继续维持不同 baseline/expected-failure 模型
3. `phase4Report` 没办法从“聚合器”退化成真正的只读 artifact consumer

---

## 3. 本 PR 必须冻结的合同

1. baseline 文件统一有 `schemaVersion`
2. baseline 更新必须显式走 update task 或迁移脚本，不允许隐式刷新
3. `reportPhase4` 不得直接触发 producer
4. 新旧报告迁移期必须并行对账

---

## 4. 范围与非目标

### 4.1 范围

1. baseline schema
2. baseline migration script
3. `VerificationBaselineComparator`
4. `EvaluationResult`
5. `reportPhase4` / `reportPhase4Only`
6. 新旧 `phase4Report` 的 metric 对账

### 4.2 非目标

1. 不迁移所有 domain producer
2. 不做 shard cache
3. 不做 Phase 5 report

---

## 5. 技术方案

### 5.1 新 baseline schema

统一格式至少包含：

1. `schemaVersion`
2. `baselineId`
3. `domainId`
4. `mode`
5. `metricDefinitionVersion`
6. `approvedDebtKeys`
7. `ceilings`
8. `expectedMetricRanges`

### 5.2 baseline mode

第一版必须支持：

1. `STRICT_ZERO_FAILURE`
2. `APPROVED_DEBT_SET`
3. `EXPECTED_FAILURE_CODE_SET`
4. `RELATIVE_BASELINE`
5. `BUDGET_THRESHOLD`

### 5.3 terrain baseline 迁移

必须提供一次性脚本，把现有：

1. `docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json`

迁移到新 schema。

要求：

1. 保留原 baseline 信息
2. 补齐 `schemaVersion`
3. 输出新文件路径和迁移日志

### 5.4 Evaluation 拆分

新增概念：

1. `KernelResult`
2. `EvaluationResult`
3. `RenderResult`

规则：

1. baseline 变化 -> 只重跑 evaluation/render/aggregate
2. report 模板变化 -> 只重跑 render/aggregate
3. kernel 未变化 -> 不重跑 heavy producer

### 5.5 `reportPhase4`

新 `reportPhase4` 必须：

1. 只消费 domain artifact
2. 读取 domain summary / evaluation result
3. 应用 `Phase4MetricCatalog`
4. 输出 phase summary

迁移期要求：

1. 保留旧 `phase4Report`
2. 新旧关键 owner metric 并行对账
3. 不一致时阻断替换

---

## 6. 推荐改动面

### 6.1 `tools/verification`

1. `VerificationBaselineContracts.kt`
2. `VerificationBaselineComparator.kt`
3. `EvaluationResult.kt`
4. `ReportAggregationInput.kt`

### 6.2 `tools/phase4`

1. `Phase4ReportRunner.kt`
2. `Phase4MetricCatalog.kt`

### 6.3 脚本/文档

1. baseline migration script
2. 现有 baseline 文件

---

## 7. 实施顺序

### Task 1：定义 baseline schema

- **目标**：统一 baseline 表达
- **验收**：
  - terrain / loot / content-pack 都能表达各自语义

### Task 2：迁移现有 terrain baseline

- **目标**：证明新 schema 可兼容现有数据
- **验收**：
  - 新 baseline 可被 comparator 读取

### Task 3：实现 comparator 和 evaluation 结果

- **目标**：把回归语义从 domain runner 中拔出来
- **验收**：
  - 支持 pass / approved debt / unexpected regression / improvement

### Task 4：改 `reportPhase4` 为 artifact-only

- **目标**：phase 聚合不再重跑 producer
- **验收**：
  - `reportPhase4Only` 可只读 artifact 重建

### Task 5：新旧 report 对账

- **目标**：降低迁移风险
- **验收**：
  - 关键 metric 一致

---

## 8. 测试策略

### 8.1 自动化命令

```bash
./gradlew reportPhase4
./gradlew reportPhase4Only
./gradlew :tools:test
```

### 8.2 必测行为

1. terrain baseline 可迁移
2. loot approved debt 可表达
3. content-pack expected failure 可表达
4. baseline 更新不触发 heavy producer
5. `reportPhase4Only` 可只读 artifact 重建

### 8.3 性能目标

1. `reportPhase4Only` warm run `<= 15s`
2. baseline/evaluation-only 重跑 `<= 10s`

---

## 9. 风险与 Gotchas

1. **不要边迁移 baseline 边修改 metric 定义**
   - 否则无法对账
2. **不要让 `reportPhase4` 暗中 `dependsOn` producer**
   - 必须是真正 artifact-only
3. **不要把所有领域特殊逻辑继续留在 report 里**
   - 应尽量下沉到 evaluation 层

---

## 10. 回滚策略

1. 保留旧 `phase4Report`
2. baseline 新 schema 出问题时，先保留迁移脚本和 comparator，不切换正式 report
3. 若新旧 metric 对账不一致，旧 report 继续承担正式 gate
