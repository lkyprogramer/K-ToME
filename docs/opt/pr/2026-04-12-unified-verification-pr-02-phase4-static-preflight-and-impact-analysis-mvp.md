> 执行前必须先完整阅读并接受：
> `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/phase4/roadmap.md`
> `docs/opt/pr/2026-04-12-unified-verification-pr-01-gradle-native-task-foundation-and-domain-contract.md`

# Unified Verification PR-02 Phase 4 静态 Preflight 与 Impact Analysis MVP

**阶段**: `Cross-Phase / Verification Refactor / UVR-W2`  
**优先级**: `P0`  
**工作量评估**: `M-L`（`3~5` 人日）  
**前置条件**: `UVR-PR-01` 完成  
**对应问题**:

1. 当前最痛的日常开发问题，是静态配置改动也会先炸慢任务。
2. 当前没有仓库级 input scope -> domain 影响分析。
3. `loot`、`hidden`、`content-pack` 都缺秒级 preflight。

---

## 1. 阶段目标

让 `Phase 4` 当前最常见的配置/合同改动先走秒级 preflight，再决定是否需要进入 owner/full 验证。

完成标准：

1. 新增 `verifyLootPreflight`
2. 新增 `verifyHiddenPreflight`
3. 新增 `verifyContentPackPreflight`
4. 新增 `verifyChanged`
5. 新增 `scopeCoverageLint`
6. 对 `core`、`DataLoader`、`FoundationGameSession`、`HeadlessRunHarness` 等关键路径有 false-negative 兜底

---

## 2. 为什么这个 PR 紧随基础设施之后

这是当前最高 ROI 的止痛 PR。

直接收益：

1. 改 `items/index.yaml / loot/index.yaml` 时，先在秒级暴露 pool overlap / subset / schema drift
2. 不再先跑 `whiteBoxLoot / lootBalanceLab / longRunLab`
3. `verifyChanged` 可以作为后续日常开发默认入口

---

## 3. 本 PR 必须冻结的合同

1. impact analysis 初期宁可过宽，不可过窄。
2. `core/`、`DataLoader`、`FoundationGameSession`、`HeadlessRunHarness` 命中时，必须有扩大验证范围的兜底。
3. preflight 只做静态/结构验证，不进入 Monte Carlo、long-run、perf/soak。
4. `verifyChanged` 初期只覆盖 `Phase 4` 已有域，不先纳入 `Phase 5`。

---

## 4. 范围与非目标

### 4.1 范围

1. `InputScope`
2. `VerificationImpactAnalyzer`
3. `verifyLootPreflight`
4. `verifyHiddenPreflight`
5. `verifyContentPackPreflight`
6. `scopeCoverageLint`
7. `verifyChanged`

### 4.2 非目标

1. 不在本 PR 统一 baseline schema
2. 不迁移 `phase4Report`
3. 不迁移 `lootBalanceLab / longRunLab`
4. 不做 shard cache
5. 不把 `phase4ReportOnly` 挂进 `verifyChanged` 的默认自动路由；aggregate cutover 留到后续 PR

---

## 5. 技术方案

### 5.1 `InputScope` 声明

建议先为 `Phase 4` 当前域声明最小 input scope：

1. `loot`
   - `game/src/main/resources/data/items/`
   - `game/src/main/resources/data/loot/`
   - `game/src/main/resources/data/world/`
   - `game/src/main/kotlin/com/ktome/game/loot/`
2. `hidden`
   - `game/src/main/resources/data/events/`
   - `game/src/main/resources/data/secret-zones/`
   - `tools/src/main/kotlin/com/ktome/tools/hidden/`
3. `content-pack`
   - `examples/content-packs/`
   - `tools/src/main/resources/fixtures/content-packs/`
   - `game/src/main/kotlin/com/ktome/game/contentpack/`
   - `tools/src/main/kotlin/com/ktome/tools/contentpack/`
4. `schema/i18n`
   - `game/src/main/resources/i18n/`
   - schema 目录

### 5.2 false-negative 兜底

必须硬编码兜底扩张规则：

1. 命中 `core/src/main/kotlin/com/ktome/core/`
   - 扩到当前 phase 的 owner 验证集
2. 命中 `game/data/DataLoader`
   - 扩到 `loot / hidden / content-pack`
3. 命中 `FoundationGameSession`
   - 扩到 `loot / hidden / boss / longrun`
4. 命中 `HeadlessRunHarness`
   - 扩到所有 `game` harness producer

### 5.3 `scopeCoverageLint`

新增静态 lint：

1. 检查 domain spec 的 `inputScopes` 是否覆盖实际关键 package/import
2. 首期不追求完全精准
3. 重点检查：
   - `core/src/main/kotlin/com/ktome/core/`
   - `DataLoader`
   - `FoundationGameSession`
   - `HeadlessRunHarness`

### 5.4 Preflight 设计

#### `verifyLootPreflight`

职责：

1. resolve loot profile candidate pools
2. 计算 overlap / subset / local identity pair
3. 输出 culprit pair diff

必须输出：

1. `sharedBaseIds`
2. `leftOnlyBaseIds`
3. `rightOnlyBaseIds`
4. `explicitVsTagMatched` 来源拆分

#### `verifyHiddenPreflight`

职责：

1. 检查 hidden event / secret zone / reward bridge / search binding 的静态一致性
2. 不跑 organic probe

#### `verifyContentPackPreflight`

职责：

1. pack schema
2. namespace / precedence / overlay rule 静态检查
3. 不跑 headless runtime

### 5.5 `verifyChanged`

第一版只做：

1. 收集 changed files
2. 命中 input scopes
3. 计算受影响 domain
4. 执行：
   - 所有命中的 preflight
   - 必要的 owner task alias

本 PR 先不做：

1. phase5 domain 路由
2. statistical shard 智能拼装
3. `phase4ReportOnly` 的 artifact-only cutover

---

## 6. 推荐改动面

### 6.1 `tools`

1. `tools/src/main/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzer.kt`
2. `tools/src/main/kotlin/com/ktome/tools/loot/LootProfileStructureAnalyzer.kt`
3. `tools/src/main/kotlin/com/ktome/tools/hidden/*Preflight*`
4. `tools/src/main/kotlin/com/ktome/tools/contentpack/*Preflight*`

### 6.2 Gradle

1. `build-logic/*`
2. `tools/build.gradle.kts`
3. `build.gradle.kts`

### 6.3 文档/配置

1. domain registry
2. scope registry

---

## 7. 实施顺序

### Task 1：InputScope 与 ImpactAnalyzer

- **目标**：先算出受影响 domain 闭包
- **验收**：
  - `loot` 改动能命中 loot preflight
  - `core` 改动能自动扩大范围

### Task 2：`verifyLootPreflight`

- **目标**：把当前最痛的静态结构问题前移
- **验收**：
  - same-zone overlap / subset 问题可秒级定位

### Task 3：`verifyHiddenPreflight` / `verifyContentPackPreflight`

- **目标**：补齐 `Phase 4` 另外两条高频配置验证
- **验收**：
  - hidden/content-pack 改动先命中静态 preflight

### Task 4：`scopeCoverageLint`

- **目标**：防止 impact scope 漏判
- **验收**：
  - 关键共享路径被检测

### Task 5：`verifyChanged`

- **目标**：形成日常开发默认入口
- **验收**：
  - 常见 `Phase 4` 改动不再直接触发最慢任务

---

## 8. 测试策略

### 8.1 自动化命令

```bash
./gradlew verifyLootPreflight
./gradlew verifyHiddenPreflight
./gradlew verifyContentPackPreflight
./gradlew verifyChanged
```

### 8.2 必测行为

1. 改 `loot/index.yaml` -> 命中 `verifyLootPreflight`
2. 改 `core/` -> 命中更宽 owner 集合
3. `verifyLootPreflight` 能输出 culprit pair diff
4. `verifyChanged` 不误判为“无任务可跑”
5. base ref 缺失时，`verifyChanged` 退回保守 changed-file 收集，而不是让独立 preflight 任务在配置期失败

### 8.3 性能目标

1. `verifyLootPreflight` warm run `<= 5s`
2. `verifyHiddenPreflight` warm run `<= 5s`
3. `verifyContentPackPreflight` warm run `<= 5s`

---

## 9. 风险与 Gotchas

1. **不要把 preflight 写成新一轮慢任务**
   - 不能依赖 `lootBalanceLab`
2. **不要让 scope 声明过窄**
   - 初期宁可多跑
3. **不要把 `verifyChanged` 设计成隐藏魔法**
   - 必须打印命中的 domain 和原因

---

## 10. 回滚策略

1. preflight 任务可独立回退，不影响旧 owner/full task
2. `verifyChanged` 只是新增入口，回退后仍可手工跑旧任务
3. 若 impact analyzer 误判频繁，先退回只保留 preflight task，不强推 `verifyChanged`
