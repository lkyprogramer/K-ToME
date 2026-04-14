> 执行前必须先完整阅读并接受：
> `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/phase4/roadmap.md`
> `docs/opt/pr/2026-04-12-unified-verification-pr-05-phase4-statistical-domain-migration-and-longrun-shard-reuse.md`

# Unified Verification PR-06 Phase 4 Gate Cutover、Alias 稳定与 Legacy Fallback 收口

**阶段**: `Cross-Phase / Verification Refactor / UVR-W6`  
**优先级**: `P1`  
**工作量评估**: `M`（`2~4` 人日）  
**前置条件**: `UVR-PR-01 ~ PR-05` 完成并对账稳定  
**对应问题**:

1. 新体系迁完后，如果不正式切 gate，开发和 CI 仍会继续依赖旧链路。
2. 对外 task 名称不能在迁移期打碎。
3. 旧链路不能无限期保留，否则会形成双轨维护。

---

## 1. 阶段目标

把 `Phase 4` 当前 gate 正式切到新 contract 上，同时保留清晰、有限的 fallback。

完成标准：

1. `phase4Report` 对外语义切到 canonical unified aggregate；`reportPhase4` 只作为显式 parity 对账入口
2. 旧 root alias 仍可用
3. 旧链路降级为 fallback，不再承担默认主路径
4. 新体系的关键 owner metric 与旧体系对账完成

---

## 2. 本 PR 必须冻结的合同

1. `Phase 4` checklist 中已冻结的对外 task 名称默认保留
2. `phase4Report` 必须变成 artifact-only 聚合器
3. fallback 必须有明确退出条件，不无限期共存

---

## 3. 范围与非目标

### 3.1 范围

1. gate cutover
2. alias stabilization
3. fallback 策略
4. 文档/checklist 同步

### 3.2 非目标

1. 不再继续新增新 engine 能力
2. 不实现 `Phase 5`

---

## 4. 技术方案

### 4.1 对外 alias

保留：

1. `contractLint`
2. `localeLint`
3. `lootBalanceLab`
4. `whiteBoxLoot`
5. `phase4Report`
6. `phase4ReportOnly`

内部允许映射到：

1. `verify...`
2. `reportPhase4`

但对外 alias 保持不变。

补充说明：

1. root `phase4Report` 指向默认 canonical unified aggregate，底层仍可保留 `:tools:phase4Report` 这一 task 名称。
2. `reportPhase4` 不承担日常 gate，而是 verification contract / baseline / aggregation schema / report schema 发生变化时的显式 parity 对账入口。

### 4.2 cutover 条件

只有在以下条件全部满足后，才切正式 gate：

1. 新旧关键 owner metric 对账一致
2. `phase4ReportOnly` 真正不依赖 producer
3. preflight / owner / full 路由稳定
4. 常见 Phase 4 改动路径的 wall time 达到目标

### 4.3 fallback 策略

旧链路保留，但降级为：

1. 手工回退入口
2. 新体系异常时的临时兜底

退出条件：

1. 两轮稳定回归通过
2. 所有关键 metric 对账一致
3. 文档/CI/checklist 已切新路径

---

## 5. 推荐改动面

1. `build.gradle.kts`
2. `tools/build.gradle.kts`
3. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
4. `docs/phase4/roadmap.md`
5. `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`

---

## 6. 实施顺序

### Task 1：对账与基线确认

- **目标**：确认新旧关键 metric 一致

### Task 2：切换 root alias 指向

- **目标**：对外保持名字不变，对内切新实现
- **补充约束**：若某个 domain 的 owner coverage 在切 gate 时存在明显漏项，允许把该补齐与 cutover 合并落地；但仅限 owner task 集对齐，不得顺手改动对应 domain 的 kernel、baseline、阈值或业务规则

### Task 3：固化 fallback

- **目标**：旧链路有明确保留和退出规则

### Task 4：同步文档

- **目标**：checklist 和 roadmap 不再误导到旧路径

---

## 7. 测试策略

### 7.1 自动化命令

```bash
./gradlew phase4Report
./gradlew phase4ReportOnly
./gradlew verifyChanged
./gradlew verifyOwner
```

### 7.2 必测行为

1. 对外 alias 不变
2. `phase4Report` 不触发 producer
3. fallback 仍可手工运行
4. 新旧 metric 对账一致

---

## 8. 风险与 Gotchas

1. **不要过早删旧任务**
   - 先降级再清理
2. **不要在 cutover PR 里继续大改 domain 行为**
   - 只切任务与 gate

---

## 9. 回滚策略

1. 任何关键 metric 对账异常，立刻回退 alias 指向旧任务
2. checklist 同步前，旧链路必须可运行
