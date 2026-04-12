# 全仓统一验证 Contract 重构 PR 拆分索引

> 基于：[2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md](../2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md)
>  
> 本目录把仓库级统一验证重构方案拆成可直接执行的 PR 级开发文档。拆分原则不是按章节平均切，而是按：
>
> 1. **先止痛，再扩展**
> 2. **先 Phase 4 回绿，再谈 Phase 5**
> 3. **先复用 Gradle 原生能力，再决定是否继续抽完整 engine**
> 4. **先迁移静态与确定性域，再迁移统计与长时域**

## 1. PR 顺序总览

| 顺序 | PR | 优先级 | 工作量 | 主题 | 主要产出 |
| --- | --- | --- | --- | --- | --- |
| 1 | [PR-01](./2026-04-12-unified-verification-pr-01-gradle-native-task-foundation-and-domain-contract.md) | P0 | M | Gradle-native 验证任务基础设施与 domain contract | `build-logic`、`VerificationTask`、domain/node spec、最小 demo domain |
| 2 | [PR-02](./2026-04-12-unified-verification-pr-02-phase4-static-preflight-and-impact-analysis-mvp.md) | P0 | M-L | `Phase 4` 静态 preflight 与 impact analysis MVP | `verifyLootPreflight`、`verifyChanged`、`scopeCoverageLint` |
| 3 | [PR-03](./2026-04-12-unified-verification-pr-03-baseline-schema-evaluation-split-and-report-only-phase4-aggregation.md) | P0 | M-L | baseline 统一、kernel/evaluation 解耦、report-only 聚合 | baseline schema、迁移脚本、`reportPhase4` |
| 4 | [PR-04](./2026-04-12-unified-verification-pr-04-phase4-deterministic-domain-migration-and-writer-convergence.md) | P1 | L | `Phase 4` 确定性域迁移与 writer 收敛 | mapgen/solvability/terrain/boss/hidden/content-pack owner domain |
| 5 | [PR-05](./2026-04-12-unified-verification-pr-05-phase4-statistical-domain-migration-and-longrun-shard-reuse.md) | P1 | XL | `Phase 4` 统计域与长时域复用 | `lootBalanceLab`、`organicHiddenProbe`、`longRunLab` shard cache |
| 6 | [PR-06](./2026-04-12-unified-verification-pr-06-phase4-gate-cutover-alias-stabilization-and-legacy-fallback-hardening.md) | P1 | M | `Phase 4` gate 切换、alias 稳定与 fallback 收口 | 新旧并行、phase4 gate cutover、旧链路降级 |
| 7 | [PR-07](./2026-04-12-unified-verification-pr-07-phase5-domain-spec-and-phase-report-design-freeze.md) | P2 | S-M | `Phase 5` 验证 domain spec 与 report 设计冻结 | tactical/replay/perf/soak/QA/release 的 docs-only 设计 |

## 2. 拆分理由

### 2.1 为什么不是一步做完整引擎

因为当前最痛的不是“缺一个完美 DAG 引擎”，而是：

1. 没有静态 preflight，导致小改动先炸慢任务。
2. 没有统一 baseline/debt，导致报告语义脆弱。
3. `phase4Report` 仍依赖 producer，导致 report rebuild 成本高。
4. `lootBalanceLab / longRunLab` 这类任务没有 kernel/evaluation 分离，导致重跑成本高。

所以拆分顺序优先把这 4 个痛点拆开解决。

### 2.2 为什么 `Phase 5` 放最后

当前仓库中：

1. `Phase 4` 的验证任务已经真实存在，且正在日常开发中制造痛点。
2. `Phase 5` 目前只有文档，没有实现。

因此正确顺序是：

1. 先把 `Phase 4` 迁到新体系并回绿。
2. 再把 `Phase 5` 作为“直接生在新 contract 上”的设计输出，而不是边迁 `Phase 4` 边实现 `Phase 5`。

## 3. 全局执行纪律

所有 PR 共同遵守：

1. **对外 root task 名称在 `Phase 4` 迁移期默认保留**
   - 如 `lootBalanceLab`、`whiteBoxLoot`、`phase4Report`
2. **旧系统必须允许并行运行**
   - 直到关键 owner metric 对账稳定
3. **`Phase 5` 不进入本轮代码实现**
   - 只输出设计冻结文档
4. **不重写 `HeadlessRunHarness`**
   - 它继续是 `game` 侧编排器
5. **普通 unit test 保持 JUnit 运行方式**
   - 新体系只替换验证域，不替换常规单测

### 3.1 关于 `dependsOn` 与 legacy JUnit adapter 的额外口径

为避免后续 review 或增量 PR 对 `PR-01` 范围产生误判，统一补充以下判断规则：

1. `VerificationNodeSpec.dependsOn` 在 `PR-01` 阶段先冻结为 **contract-only 字段**
   - 当前职责仅限于表达 node graph 形状、校验依赖 id 合法性、为后续 planner/executor 留稳定 contract
   - 在 dedicated planner/executor 任务落地前，不要求 `PR-01 ~ PR-03` 把它真正接进执行闭包或 Gradle task dependency
2. 只有当仓库开始出现真实多 node domain，且需要 `kernel -> evaluation -> render` 之类的依赖闭包时，才应把 `dependsOn` 升级为正式执行语义
   - 这属于 DAG/planner 能力建设，不应作为 `contract/demo` PR 的顺手扩展
3. `LegacyHarnessAdapterTask` 在迁移期允许继续使用显式 `selectedClasses` 和可选 tag filter
   - `tag-only classpath discovery` 属于 legacy adapter 优化，不是当前 PR 序列的主线里程碑
   - 只有当显式 class list 已经成为持续维护负担，或 legacy adapter 需要长期保留时，才值得单独补这项能力
4. 因此，后续 review 遇到这两类点时，默认先判断：
   - 它是不是当前 PR 的显式目标？
   - 它是不是已经阻塞真实多 node domain 或迁移效率？
   - 若都不是，应记录为后续能力缺口，而不是回判 `PR-01` 不完整

## 4. 预期里程碑

完成 `PR-01 ~ PR-03` 后，应达到：

1. 静态 preflight 可独立运行
2. baseline 统一 schema 成立
3. `reportPhase4` 可只读 artifact 重建

完成 `PR-04 ~ PR-06` 后，应达到：

1. `Phase 4` 当前 owner domain 都跑在新 contract 上
2. `Phase 4` 常见改动先命中 preflight 和 impact closure
3. 统计/长时域支持 shard 级复用
4. 当前“一改就炸、炸了还跑很久”的主痛点在 `Phase 4` 范围内解除

完成 `PR-07` 后，应达到：

1. `Phase 5` 后续开发不再需要重新设计验证框架
2. tactical AI / replay / perf / soak / QA / release 从第一天就挂在统一 contract 上
