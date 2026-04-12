> 执行前必须先完整阅读并接受：
> `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/pr/2026-04-12-unified-verification-pr-02-phase4-static-preflight-and-impact-analysis-mvp.md`
> `docs/opt/pr/2026-04-12-unified-verification-pr-03-baseline-schema-evaluation-split-and-report-only-phase4-aggregation.md`

# Unified Verification PR-04 Phase 4 确定性 Domain 迁移与 Writer 收敛

**阶段**: `Cross-Phase / Verification Refactor / UVR-W4`  
**优先级**: `P1`  
**工作量评估**: `L`（`4~6` 人日）  
**前置条件**: `UVR-PR-01 ~ PR-03` 完成  
**对应问题**:

1. `mapgen / solvability / terrain / boss / hidden / content-pack` 目前仍是散装 task + writer glue。
2. `game` 里有 `WhiteBoxHarnessWriter`，`tools` 里有 `WhiteBoxReportWriter`，存在重复实现。
3. `Phase 4` 当前确定性域是最适合先批量迁移的验证域。

---

## 1. 阶段目标

把 `Phase 4` 当前主要确定性 owner domain 挂到统一 contract 上，同时收敛两套 white-box writer。

完成标准：

1. `mapgen`
2. `solvability`
3. `terrain`
4. `boss`
5. `hidden owner`
6. `content-pack owner`

都能以新 verification domain 形式运行。

---

## 2. 本 PR 必须冻结的合同

1. 不重写 `HeadlessRunHarness`
2. `game` harness 继续作为 kernel producer
3. white-box writer 收敛为共享实现 + adapter，不允许两边继续分叉演进
4. 旧 task alias 保留

---

## 3. 范围与非目标

### 3.1 范围

1. `mapgen` domain 迁移
2. `solvability` domain 迁移
3. `terrain` domain 迁移
4. `boss` domain 迁移
5. `hidden owner` domain 迁移
6. `content-pack owner` domain 迁移
7. writer 收敛

### 3.2 非目标

1. 不迁移 `lootBalanceLab`
2. 不迁移 `organicHiddenProbe`
3. 不迁移 `longRunLab`
4. 不切正式 gate

---

## 4. 技术方案

### 4.1 共享 writer 收敛

现状：

1. `game/src/test/kotlin/com/ktome/game/harness/WhiteBoxHarnessWriter.kt`
2. `tools/src/main/kotlin/com/ktome/tools/whitebox/WhiteBoxReportWriter.kt`

两者有大量重复。

本 PR 必须收敛为：

1. 共享 writer 核心
2. `game` 侧 adapter
3. `tools` 侧 adapter

原则：

1. 输出 schema 完全一致
2. 不允许继续复制逻辑
3. `VerificationReportHeader` 仍沿用当前主干 DTO

### 4.2 Domain 迁移顺序

建议顺序：

1. `mapgen`
2. `solvability`
3. `terrain`
4. `boss`
5. `hidden owner`
6. `content-pack owner`

原因：

1. `mapgen/solvability` 已经最接近现有 `tools` runner 形态
2. `terrain/boss` 依赖 `game` harness writer 和 `HeadlessRunHarness`
3. `hidden/content-pack` 需要同时消费静态与确定性信息

### 4.3 `game` harness 的处理

本 PR 明确：

1. `BossHarnessTest`
2. `TerrainInteractionBatchTest`

仍留在 `game` 模块执行。

迁移方式不是搬走测试，而是：

1. 给它们增加统一 verification domain adapter
2. 让它们输出的新 artifact 能被 `reportPhase4` 直接消费

### 4.4 兼容旧任务

保留：

1. `whiteBoxMapgen`
2. `whiteBoxSolvability`
3. `terrainInteractionBatch`
4. `bossHarness`
5. `hiddenContentHarness`
6. `contentPackHarness`

但其实现切到新 domain/adapter。

### 4.5 `hidden owner` 的静态校验入口收口

当前 `Phase4StaticContentValidator` 可以继续作为 `GameContent` 与 `verifyHiddenPreflight / hidden owner` 共用的静态校验入口。

但 `PR-04` 在迁移 `hidden owner` domain 时，必须同步守住以下演进规则：

1. 若 hidden contract 在本 PR 或其后续收口中继续扩张，导致 validator 需要再承载新的 verification consumer，不能继续扩大“多 list 参数 + 线性扫描”式 API。
2. 若 `Phase4StaticContentValidator` 出现第 `3` 个调用方，必须升级为 `HiddenValidationContext` 或等价的 registry-backed access。
3. 迁移目标是保留单一 hidden contract 真源，而不是让 `GameContent`、preflight、owner harness 各自维护一套 hidden 静态解析路径。

---

## 5. 推荐改动面

### 5.1 `tools`

1. `tools/src/main/kotlin/com/ktome/tools/mapgen/*`
2. `tools/src/main/kotlin/com/ktome/tools/hidden/*`
3. `tools/src/main/kotlin/com/ktome/tools/contentpack/*`
4. `tools/src/main/kotlin/com/ktome/tools/whitebox/*`

### 5.2 `game`

1. `game/src/test/kotlin/com/ktome/game/harness/WhiteBoxHarnessWriter.kt`
2. `game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt`
3. `game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt`

---

## 6. 实施顺序

### Task 1：writer 收敛

- **目标**：消除 `game` / `tools` 双 writer 重复
- **验收**：
  - 输出 schema 不变
  - 共享核心只有一套

### Task 2：迁移 `mapgen / solvability`

- **目标**：先迁 `tools` 域内最容易的确定性域
- **验收**：
  - 新系统输出与旧系统一致

### Task 3：迁移 `terrain / boss`

- **目标**：把 `game` harness 接到统一 contract
- **验收**：
  - `HeadlessRunHarness` 不被重写
  - artifact 能被 phase report 消费

### Task 4：迁移 `hidden owner / content-pack owner`

- **目标**：完成 `Phase 4` 当前确定性 owner 域迁移

---

## 7. 测试策略

### 7.1 自动化命令

```bash
./gradlew whiteBoxMapgen
./gradlew whiteBoxSolvability
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew hiddenContentHarness
./gradlew contentPackHarness
```

### 7.2 必测行为

1. 新旧 writer 生成的 summary/cases/report/artifacts 一致
2. 新旧 mapgen/solvability 关键 owner metric 一致
3. `terrain/boss` 迁移后不破坏 `game` harness
4. `reportPhase4` 可读取这些新 artifact

---

## 8. 风险与 Gotchas

1. **不要在 writer 收敛时顺手改 schema**
   - schema 变更应单独做
2. **不要搬走 `game` harness`**
   - 它们是 producer，不是这轮要消灭的对象
3. **不要同时迁移统计域**
   - 否则问题难以定位

---

## 9. 回滚策略

1. 每个 deterministic domain 都保留旧 task alias
2. writer 收敛失败时，先恢复双 writer 并继续迁域，之后再单独收敛
3. 任一 domain 对账不一致时，不替换旧 task 的正式职责
