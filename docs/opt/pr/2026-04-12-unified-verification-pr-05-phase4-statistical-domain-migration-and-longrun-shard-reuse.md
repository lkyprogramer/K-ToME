> 执行前必须先完整阅读并接受：
> `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/pr/2026-04-12-unified-verification-pr-03-baseline-schema-evaluation-split-and-report-only-phase4-aggregation.md`
> `docs/opt/pr/2026-04-12-unified-verification-pr-04-phase4-deterministic-domain-migration-and-writer-convergence.md`

# Unified Verification PR-05 Phase 4 统计域迁移与 Long-Run Shard 复用

**阶段**: `Cross-Phase / Verification Refactor / UVR-W5`  
**优先级**: `P1`  
**工作量评估**: `XL`（`5~8` 人日）  
**前置条件**: `UVR-PR-01 ~ PR-04` 完成  
**对应问题**:

1. `lootBalanceLab`、`organicHiddenProbe`、`longRunLab` 是当前最贵的任务。
2. 这些任务没有 shard 级复用，改一个 baseline 或 report 经常被迫重跑。
3. `longRunLab` 也是 `Phase 4` 终盘身份指标的 owner，不能等到最后再迁。

---

## 1. 阶段目标

把 `Phase 4` 当前统计域和长时域接到统一 contract，并建立 shard 级 kernel 复用。

完成标准：

1. `lootBalanceLab` 支持 shard cache
2. `organicHiddenProbe` 支持 shard cache
3. `longRunLab` 支持 shard cache 和 evaluation-only 重跑
4. `whiteBoxLoot` 直接复用 `loot` kernel + preflight 结果
5. 本阶段**不引入 sequential early stop**

---

## 2. 为什么早停不放在这个 PR

虽然总方案里给了 `SPRT / Sequential CI` 作为算法候选，但本 PR 明确不做。

原因：

1. 迁移期不应同时改变统计方法和基础设施
2. 当前更大的收益来自 shard cache、kernel/evaluation 解耦、固定样本复用
3. 多重检验和相关样本问题应在体系稳定后再引入

---

## 3. 本 PR 必须冻结的合同

1. 统计域第一阶段仍使用固定样本量
2. shard 划分必须固定、可追溯、可重复
3. 同一输入下，baseline 改动不得重跑 kernel
4. `longRunLab` 迁移后仍保留现有 owner metric：
   - `terminalWeaponBaseDiversity`
   - `crossProfessionTopWeaponDominance`
   - `professionAlignedWeaponAdoptionRate`

---

## 4. 范围与非目标

### 4.1 范围

1. `lootBalanceLab`
2. `organicHiddenProbe`
3. `longRunLab`
4. `whiteBoxLoot` 对 `loot` kernel 的复用

### 4.2 非目标

1. 不引入 sequential early stop
2. 不引入 Phase 5 统计域
3. 不切正式 gate

---

## 5. 技术方案

### 5.1 shard 策略

#### `lootBalanceLab`

按以下维度切 shard：

1. `matrixId`
2. `seedRange` 或 `rollRange`

要求：

1. shard 合并后结果与现有固定样本定义完全一致

#### `organicHiddenProbe`

按以下维度切 shard：

1. `seedSlice`
2. 必要时 `profession/race slice`

#### `longRunLab`

按以下维度切 shard：

1. `profession x race`
2. `seedSlice`

### 5.2 kernel / evaluation 拆分

必须拆成：

1. `KernelResult`
   - 原始样本 / 汇总统计
2. `EvaluationResult`
   - gate / baseline / threshold 比较
3. `RenderResult`
   - markdown/json/report

### 5.3 `whiteBoxLoot`

新职责：

1. 不再重新生成 `loot` 重型结果
2. 直接消费：
   - `lootBalanceLab` kernel result
   - `verifyLootPreflight` 静态分析结果
3. 输出：
   - owner summary
   - baseline/debt 对比
   - culprit diff

### 5.4 streaming 统计

本 PR 可落地：

1. `Welford` 在线均值/方差
2. `t-digest` 或 `HDR Histogram` 做 P95/P99

本 PR 不落地：

1. SPRT
2. sequential CI

---

## 6. 推荐改动面

### 6.1 `tools/loot`

1. `LootBalanceLabRunner.kt`
2. `WhiteBoxLootRunner.kt`
3. `Loot kernel shard` 相关新类

### 6.2 `tools/hidden`

1. `OrganicHiddenProbeRunner.kt`

### 6.3 `game` harness

1. `LongRunLabTest.kt`
2. `LongRunLabFullTest.kt`
3. `HeadlessRunHarness.kt`（仅必要的结构化输出扩展）

---

## 7. 实施顺序

### Task 1：`lootBalanceLab` shard 化

- **目标**：先迁当前最核心统计域
- **验收**：
  - shard 合并后与旧结果一致

### Task 2：`whiteBoxLoot` 复用 `loot` kernel

- **目标**：消除 `whiteBoxLoot` 对重型 producer 的重复依赖
- **验收**：
  - baseline/evaluation-only 重跑成立

### Task 3：`organicHiddenProbe` shard 化

- **目标**：降低 organic probe 重跑成本

### Task 4：`longRunLab` shard 化

- **目标**：让终盘身份 owner metric 支持局部复用
- **验收**：
  - 保留现有 metric 语义

---

## 8. 测试策略

### 8.1 自动化命令

```bash
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew organicHiddenProbe
./gradlew longRunLab
```

### 8.2 必测行为

1. shard 合并结果与旧固定样本一致
2. baseline 改动不重跑 kernel
3. `whiteBoxLoot` 复用 `loot` kernel
4. `longRunLab` 指标语义不漂移

### 8.3 性能目标

1. 相同输入下二次运行命中 kernel cache
2. `loot` baseline/evaluation-only 重跑 `<= 10s`
3. `longRunLab` 常见局部变更不再整批重跑

---

## 9. 风险与 Gotchas

1. **不要改变统计语义**
   - 只改执行与缓存
2. **不要让 shard 划分引入非确定性**
   - seed partition 必须固定
3. **不要在本 PR 引入早停**
   - 否则难以区分“统计变了”还是“执行变了”

---

## 10. 回滚策略

1. 每个统计域保留旧单体 runner 路径
2. shard 结果与旧结果对账不一致时，旧路径继续承担正式 gate
3. `longRunLab` 如局部复用不稳定，先只保留 `loot/organic` 的 shard 迁移
