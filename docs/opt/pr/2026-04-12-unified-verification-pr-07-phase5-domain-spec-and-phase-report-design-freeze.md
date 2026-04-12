> 执行前必须先完整阅读并接受：
> `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`
> `docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md`
> `docs/phase5/2026-03-13-phase5-regression-checklist.md`
> `docs/phase5/roadmap.md`

# Unified Verification PR-07 Phase 5 Domain Spec 与 Phase Report 设计冻结

**阶段**: `Cross-Phase / Verification Refactor / UVR-W7`  
**优先级**: `P2`  
**工作量评估**: `S-M`（`1~2` 人日）  
**前置条件**: `Phase 4` 统一验证迁移稳定  
**对应问题**:

1. `Phase 5` 目前只有文档，没有实现。
2. 如果不先冻结验证 domain spec，后续 tactical AI / replay / perf / soak / QA 很容易再走回散装 task 路线。

---

## 1. 阶段目标

本 PR 是 **docs-only design freeze**，不做 `Phase 5` 代码实现。

完成标准：

1. tactical AI / replay / perf / soak / localization QA / accessibility QA / balance / release 的 domain spec 固定
2. `reportPhase5` 的输入、输出、metric catalog 固定
3. `Phase 5` 后续开发不需要重新设计验证框架

---

## 2. 为什么这个 PR 放最后

因为：

1. 当前真实痛点在 `Phase 4`
2. `Phase 5` 还没有代码和任务
3. 正确顺序是先让 `Phase 4` 跑在新体系上，再把 `Phase 5` 直接生在新体系上

---

## 3. 本 PR 必须冻结的合同

1. `Phase 5` 不复刻 `Test + @Tag` 散装任务模式
2. `Phase 5` 任务从第一天就采用 domain spec + node DAG + report-only aggregation
3. `Phase 5` 本 PR 只出文档，不写代码

---

## 4. 范围与非目标

### 4.1 范围

1. `tactical-ai` domain spec
2. `replay` domain spec
3. `perf` domain spec
4. `soak` domain spec
5. `qa-localization` domain spec
6. `qa-accessibility` domain spec
7. `balance` domain spec
8. `release` domain spec
9. `reportPhase5` 设计

### 4.2 非目标

1. 不实现任何 `Phase 5` task
2. 不改 `Phase 4`

---

## 5. 技术方案

### 5.1 必须冻结的 Phase 5 domain

1. `tactical-ai/OWNER`
2. `replay/OWNER`
3. `perf/FULL`
4. `soak/NIGHTLY|RELEASE`
5. `qa-localization/OWNER`
6. `qa-accessibility/OWNER`
7. `balance/FULL|NIGHTLY`
8. `release/RELEASE`

### 5.2 必须冻结的关键 metric

1. `actionFamilyMatchRate`
2. `illegalTargetCount`
3. `aiTraceHashConsistencyRate`
4. `runSemanticHashConsistencyRate`
5. `runTraceHashConsistencyRate`
6. `p50/p95 frame time`
7. `heapDriftMb`
8. `maxGcPauseMs`
9. `contrastViolationCount`
10. `keyboardFlowFailureCount`

### 5.3 `reportPhase5`

必须定义：

1. 输入 artifact 列表
2. phase5 metric catalog
3. summary markdown/json schema
4. release summary 最小字段

---

## 6. 推荐输出

1. `Phase 5` 验证 domain spec 文档
2. `Phase 5` DAG 文档
3. `Phase 5` report schema 文档

---

## 7. 测试策略

本 PR 无代码实现。

验收方式：

1. 文档内部交叉引用完整
2. 与 `Phase 5` roadmap / checklist 一致
3. 不引入与 `Phase 4` 方案冲突的第二套 contract

---

## 8. 风险与 Gotchas

1. **不要把 `Phase 5` 写成实现计划**
   - 本 PR 只冻结验证设计
2. **不要遗漏 perf/soak/report-only 聚合**
   - 这三项是 `Phase 5` 最容易重新散装化的部分
