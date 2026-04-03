# Phase 4 PR-01 深度审查报告

**审查日期**: 2026-04-01  
**审查角色**: 资深 Roguelike 游戏开发设计总监 / 系统策划总监 / 玩法体验审查负责人  
**审查分支**: `codex/phase4-pr01-mapgen-contract-and-smoke-baseline`  
**规范文档**: `docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md`

---

## 0. 总体评价

**综合一致性: 92%** — PR-01 的核心 contract 与骨架交付完整度较高，关键词汇（`MapgenPipeline / GeneratedFloor / TopologyGraph / TerrainTag / mapgenSmoke`）全部进入正式代码，BSP 降级、最小 planner、smoke harness 三大支柱均已落地。存在若干偏差，主要集中在 floor-level persistence 字段缺失和 `MinimalTopologyPlanner` 的 loop 语义弱化两处，属于可快速修复的范围。

---

## 1. 逐条对照：完成标准（Section 1）

| # | 完成标准 | 状态 | 偏差说明 |
|---|---------|------|---------|
| 1 | `MapgenRequest / ZoneMapgenProfile / TopologyGraph / GeneratedFloor / MapgenPipeline` 进入正式词汇 | **PASS** | 全部定义在 `core.mapgen.MapgenContracts.kt`，接口签名与规范完全一致 |
| 2 | `TerrainTag` 第一版固定为 `WATER / OIL / ICE` | **PASS** | `enum class TerrainTag { WATER, OIL, ICE }` 完全匹配 |
| 3 | `BspGenerator` 降级为兼容后端 | **PASS** | `BspBackedMapgenPipeline` 实现 `MapgenPipeline`；`GameModule.generateFloor()` 已切到 pipeline 调用 |
| 4 | 交付最小非 BSP 的 `TopologyPlanner` 骨架 | **PASS（含偏差）** | `MinimalTopologyPlanner` 已交付，见偏差 D-01 |
| 5 | `mapgenSmoke` root Gradle alias 建立 | **PASS** | `./gradlew mapgenSmoke` 可用，报告路径正确 |
| 6 | 地图 golden 切到拓扑摘要 + seed + 约束结果 | **PASS** | 两份 golden JSON 已落盘，包含 fingerprint / topologySummary / terrainTagDistribution |

---

## 2. 逐条对照：冻结口径（Section 2.1）

| # | 冻结要求 | 状态 | 偏差说明 |
|---|---------|------|---------|
| 1 | `MapgenPipeline.run(request)` 成为正式 floor generation 入口 | **PASS** | `GameModule.generateFloor()` line 370 已切到 `content.mapgenPipeline.run(...)` |
| 2 | `GeneratedFloor` 保留 `zoneId / floorIndex / seed / topology` | **PASS** | 四字段全部存在 |
| 3 | `TopologyGraph` 预留 `pathClass / requiredKeys` | **PASS** | `TopologyNode.pathClass` + `TopologyEdge.requiredKeys` 均已冻结 |
| 4 | `TerrainTag` 只允许 `WATER / OIL / ICE` | **PASS** | enum 无额外值 |
| 5 | `BspGenerator` 不再直接暴露给 `game` 主路径 | **PASS** | `GameModule` 通过 `MapgenPipeline` 间接使用 |
| 6 | `./gradlew mapgenSmoke` 及固定报告路径 | **PASS** | summary → `tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json`；seeds → `...mapgen-smoke-seeds.jsonl` |
| 7 | `GeneratedFloor.map` 保留为兼容字段 | **PASS** | 字段存在，新逻辑优先消费 `topology / terrainTags / entrances` |
| 8 | `GeneratedFloor.map` 移除时点固定为 Phase 5 | **PASS**（文档级）| 代码中 `map` 仍为必填参数，符合预期 |

---

## 3. 逐条对照：技术方案（Section 4）

### 3.1 Contract 与兼容后端（Section 4.1）

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 文件落位 `core.mapgen.MapgenContracts.kt` | **PASS** | |
| 文件落位 `core.mapgen.BspBackedMapgenPipeline.kt` | **PASS** | |
| 文件落位 `core.mapgen.MapgenContractTest.kt` | **PASS** | |
| `NodeId` / `RequirementRef` 为 `@JvmInline value class` | **PASS** | 且加了 `@Serializable` + `require(isNotBlank)` 校验，比规范更严格，好 |
| `TopologyNode.roomDefId` 兼容阶段回填 BSP id | **PASS** | `LinearTopologyProjector` 使用 `"bsp.rect.standard"` |
| `terrainTags` 允许为空 | **PASS** | `GeneratedFloor.compatibility()` 默认 `emptyMap()` |
| `PathClass` 在兼容阶段填默认值 | **PASS** | BSP 适配器全部填 `CRITICAL_PATH` |
| `MapgenPipeline` 不直接暴露 `ZoneMapgenProfile` | **PASS** | profile 解析在 pipeline 内部通过 `profileResolver` 完成 |

**额外收获（规范未要求但有价值）**:
- `GeneratedEntrance` DTO 已提前引入，为 PR-03 / PR-07 预留了 entrance 携带能力
- `TopologyPlanner` interface 独立抽象，允许 PR-02 只替换 planner 不改 pipeline
- `TopologyGraph.isPrimaryPathReachable()` 扩展函数 — 作为 reachability 断言的共享工具
- `GeneratedFloor.compatibility()` 工厂方法 — 让 legacy save 恢复和兼容路径更干净

### 3.2 最小 planner 骨架与 BSP 兼容适配层（Section 4.2）

| 检查项 | 状态 | 偏差 |
|--------|------|------|
| `MinimalTopologyPlanner` 不是 BSP wrapper | **PASS** | 独立的 3~4 节点 deterministic planner |
| 至少 3~5 节点 | **PASS** | 3 个主路径节点 + 0~1 个 optional 节点 |
| 至少 1 条主路径 | **PASS** | `start -> mid -> goal` |
| 0~1 条支路 | **PASS** | 当 `loopCountRange.last > 0` 时生成 `optional` 节点 |
| `BspBackedMapgenPipeline` 兼容后端 | **PASS** | |
| 两个实现同时交付 | **PASS** | |

### 3.3 TerrainTag 最小接线（Section 4.3）

| 检查项 | 状态 | 说明 |
|--------|------|------|
| `MapgenPipeline` 生产 `terrainTags` | **PASS** | `TerrainTagPainter.paint()` |
| `game` floor runtime 保存 `terrainTags` 到可重放 floor payload | **PASS** | `FloorRuntimeState.terrainTags` + `FloorSnapshot.terrainTagHash` |
| `RenderSnapshot` 携带 `TerrainTag` 只读结果 | **PASS** | `MapCellSnapshot.terrainTags: List<String>` |
| `CombatPipeline step 9` 元素交互留给 PR-06 | **PASS** | 无战斗交互代码 |

### 3.4 mapgenSmoke alias 与报告契约（Section 4.4）

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 落位 `tools/.../MapgenSmokeRunner.kt` | **PASS** | |
| root alias `./gradlew mapgenSmoke` | **PASS** | `build.gradle.kts` line 152 |
| 报告路径 `tools/build/reports/phase4/mapgen/` | **PASS** | |

**报告最小字段对照**:

| # | 规范要求字段 | 实际输出 | 状态 |
|---|------------|---------|------|
| 1 | `buildId` | `header.buildId` | **PASS** |
| 2 | `phaseId: P4` | `header.phaseId` | **PASS** |
| 3 | `contentSchemaVersion` | `header.contentSchemaVersion` | **PASS** |
| 4 | `topologyFingerprintVersion` | `header.topologyFingerprintVersion` | **PASS** |
| 5 | `harnessId: mapgenSmoke` | `header.harnessId` | **PASS** |
| 6 | `seed` | per-seed JSONL `seed` | **PASS** |
| 7 | `zoneId` | per-seed JSONL `zoneId` | **PASS** |
| 8 | `floorIndex` | per-seed JSONL `floorIndex` | **PASS** |
| 9 | `topologySummary` | per-seed JSONL `topologySummary` | **PASS** |
| 10 | `criticalPathReachable` | per-seed JSONL `criticalPathReachable` | **PASS** |
| 11 | `loopCount` | per-seed JSONL `loopCount` | **PASS** |
| 12 | `terrainTagDistribution` | per-seed JSONL `terrainTagDistribution` | **PASS** |
| 13 | `topologyFingerprint` | per-seed JSONL `topologyFingerprint` | **PASS** |

### 3.5 floor-level persistence 最小边界（Section 4.5）

| # | 规范要求字段 | 实际落位 | 状态 |
|---|------------|---------|------|
| 1 | `zoneId` | `FloorSnapshot.zoneId` | **PASS** |
| 2 | `floorIndex` | `FloorSnapshot.floorIndex` | **PASS** |
| 3 | `floorSeed` | `FloorSnapshot.floorSeed` | **PASS** |
| 4 | `topologyFingerprint` | `FloorSnapshot.topologyFingerprint` | **PASS** |
| 5 | `terrainTagHash` | `FloorSnapshot.terrainTagHash` | **PASS** |
| 6 | `topologyFingerprintVersion` | `SaveSnapshot.topologyFingerprintVersion` | **PASS**（全局级，非 floor 级） |
| 7 | `activePackIds: List<PackId>` | `SaveSnapshot.activePackIds` | **PASS**（全局级） |
| 8 | `activePackManifestVersions: Map<PackId, String>` | `SaveSnapshot.activePackManifestVersions` | **PASS**（全局级） |

> 注：字段 6~8 放在 `SaveSnapshot` 全局级而非 `FloorSnapshot` 级别，这是合理的设计选择 — 同一存档内所有 floor 共享同一套 pack/version 上下文。符合规范约束 3 的 "正式 save/replay DTO 中复用全局 typed `PackId` 口径" 要求。

### 3.6 地图 golden 重录策略（Section 4.6）

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 拓扑 golden 落盘 `tools/src/test/resources/golden/phase4/mapgen/*.json` | **PASS** | 两份文件：`bsp-crystal_cavern-floor1.json` + `minimal-bandit_camp-floor1.json` |
| `MapgenGoldenContractTest` 消费 golden | **PASS** | 重新生成并逐字段 assertEquals |
| 截图/可视检查仍由 `goldenScreenshot` 消费 | **PASS**（保持不变）| |

---

## 4. 逐条对照：测试与自证（Section 6）

### 6.1 必测行为

| # | 必测项 | 测试覆盖 | 状态 |
|---|--------|---------|------|
| 1 | 同一 `MapgenRequest` + seed → `TopologyGraph` 稳定一致 | `MapgenContractTest.bsp backed pipeline is deterministic` | **PASS** |
| 2 | BSP 兼容后端 `GeneratedFloor` 非空 + `primaryPathNodeIds` 非空 | 同上测试 `assertTrue(left.topology.primaryPathNodeIds.isNotEmpty())` | **PASS** |
| 3 | `terrainTags` 空时序列化稳定 | `MapgenContractTest.compatibility generated floor preserves empty terrain tag contract` | **PASS** |
| 4 | `MinimalTopologyPlanner` 非 BSP 样例通过 smoke + `PathClass` 可用 | `MapgenContractTest.minimal topology pipeline produces non bsp optional branch contract` | **PASS** |
| 5 | `mapgenSmoke` 固定 seed → 完整 summary/report 文件 | `MapgenSmokeHarnessTest.mapgen smoke writes fixed reports and keeps baseline green` | **PASS** |

### 6.2 自动化命令

| 命令 | 状态 | 说明 |
|------|------|------|
| `./gradlew :core:test` | **PASS** | `MapgenContractTest` 在 core 模块 |
| `./gradlew mapgenSmoke` | **PASS** | root alias → `:tools:mapgenSmoke` |
| `./gradlew solvabilityHarness` | **PASS** | root alias → `:tools:solvabilityHarness`，状态 `PENDING_PR03` |

---

## 5. 逐条对照：出口门禁（Section 7）

| # | 门禁要求 | 状态 | 说明 |
|---|---------|------|------|
| 1 | `MapgenPipeline` contract 和 `TerrainTag` 归属冻结 | **PASS** | |
| 2 | `game` 主路径不再直接依赖 `BspGenerator` | **PASS** | `GameModule.generateFloor()` 通过 `content.mapgenPipeline.run(...)` |
| 3 | `mapgenSmoke` root alias + 固定报告产物路径 | **PASS** | |
| 4 | 交付最小非 BSP planner | **PASS** | `MinimalTopologyPlanner` + `MinimalTopologyMapgenPipeline` |
| 5 | `topologyFingerprintVersion` 与 floor-level persistence 最小边界冻结 | **PASS** | `Phase4ContractVersions.TOPOLOGY_FINGERPRINT_VERSION = 1` |
| 6 | 后续 PR-02/PR-03 可不改 session 主装配 | **PASS** | pipeline 可替换，session 只消费 `GeneratedFloor` |

---

## 6. 发现的偏差与问题

### D-01: `MinimalTopologyPlanner` 的 loop 语义弱化（中等偏差）

**规范要求**: "至少能产出 1 条主路径 + 0~1 条支路"  
**实际实现**: optional 节点产出条件为 `profile.loopCountRange.last > 0`，但 `optionalLoopCount` 始终返回 `0`。

**问题**: 当存在 optional 分支时，`optionalLoopCount` 应为 `1`（它确实形成了一条支路），但代码硬编码为 `0`。这使得 smoke report 中 `loopCount` 永远是 `0`，即使 minimal planner 产出了 optional 分支。从 Roguelike 关卡设计角度看，loop 是地图可玩性的核心指标 — loop 越多意味着玩家可选路径越多，如果 harness 永远报告 0 loop，后续 PR-02 混合拓扑 planner 的 loop 回归检测就没有对照基线。

**影响范围**: `MinimalTopologyPipeline.kt:56-61`  
**偏差程度**: 中等 — 不影响 contract 正确性，但 smoke 报告的 `loopCount` 指标失真。

**修复建议**:
```kotlin
// MinimalTopologyPipeline.kt line 56-61
return TopologyGraph(
    nodes = nodes,
    edges = edges,
    primaryPathNodeIds = primaryNodes.map(TopologyNode::id),
    optionalLoopCount = if (optionalBranch.isNotEmpty()) 1 else 0,  // fix
)
```

### D-02: `MinimalTopologyPlanner` 的 optional 分支未形成真正的 loop（低偏差）

**规范要求**: "0~1 条支路"  
**实际实现**: optional 节点只有 `mid -> optional` 一条边，是死胡同（dead-end），不是 loop。从拓扑学意义上，loop 意味着从 A 出发经过不同路径可以回到同一节点，形成环路。

**问题**: 当前 optional 分支是一条单向死胡同，不构成拓扑 loop。这对于验证 `TopologyEdge.isLoop` 字段的设计合理性是不充分的 — PR-01 的 minimal planner 从未产生 `isLoop = true` 的边，这个字段在本 PR 没有被任何非空值消费过。

**影响范围**: `MinimalTopologyPipeline.kt:48-55`  
**偏差程度**: 低 — 规范说的是"支路"而非严格 loop，但 `isLoop` 字段作为 contract 的一部分应当在 minimal planner 中至少有一个样例验证它。

**修复建议**: 添加一条 `optional -> goal` 的 loop 边：
```kotlin
if (optionalBranch.isNotEmpty()) {
    add(TopologyEdge(from = NodeId("mid"), to = NodeId("optional")))
    add(TopologyEdge(from = NodeId("optional"), to = NodeId("goal"), isLoop = true))
}
```
同时将 `optionalLoopCount` 设为 `1`。

### D-03: `MapgenRequest.floorIndex` 校验要求 `> 0`，但 `MinimalTopologyMapgenPipeline` smoke 用 `floorIndex = 1`（无偏差，但值得注意）

**说明**: 规范没有明确约定 floorIndex 是否从 0 还是 1 开始。实现选择了 `> 0`（即从 1 开始），这与 `FloorSnapshot` 的 `require(floorIndex > 0)` 一致。Smoke runner 中 minimal probe 使用 `floorIndex = 1`，是正确的。无偏差。

### D-04: `topologyFingerprintVersion` 在 floor-level 是全局级而非每 floor 独立（设计选择，无偏差）

**说明**: 规范 Section 4.5 列出的最小字段中 `topologyFingerprintVersion` 作为 floor-level 字段。实际实现放在 `SaveSnapshot` 全局级。这是一个合理的设计简化 — 同一存档内不会混用不同版本的 fingerprint 算法。后续如果需要跨版本迁移，全局级反而更容易做版本升级判断。无偏差。

### D-05: Smoke harness 中 `BSP_SEEDS_PER_FLOOR = 24` 的 500 seed 下限（无偏差，但紧凑）

**说明**: 规范 Section 6.2 要求 "至少 500 seed"。实现的种子计算方式为 `zones * floors * BSP_SEEDS_PER_FLOOR + minimal_probe_cases`。当前 `MapgenSmokeHarnessTest` 断言 `run.totalCases >= 500` 且 `run.distinctSeedCount >= 500`。这取决于 zone schema 中的 zone 数量和每个 zone 的 floorCount。如果 zone 数据发生变化导致总案例数低于 500，测试会正确失败。设计合理。

### D-06: `RenderSnapshotContractTest` 新增了 terrain tag 渲染路径测试（超出规范，良好）

实现新增了 `render snapshot exposes visible terrain tags without reconstructing map semantics in client` 测试，验证了 `MapCellSnapshot.terrainTags` 在客户端渲染路径中的正确传递。这是规范 Section 4.3 要求 "RenderSnapshot 只携带消费 TerrainTag 所需的只读结果" 的良好自证。

---

## 7. 架构与设计质量评估

### 7.1 模块边界（优秀）

- `core.mapgen` 包只包含纯 contract（DTO + interface），没有引入 game-specific 依赖
- `core.mapgen.MapgenInternals.kt` 将 `LinearTopologyProjector` 和 `TerrainTagPainter` 标记为 `internal`，防止外部模块绕过 pipeline 直接使用投影工具
- `game.mapgen.SchemaZoneMapgenProfileResolver` 正确地将 zone schema 到 profile 的映射逻辑限制在 game 模块内
- `TopologyPlanner` interface 独立于 `MapgenPipeline`，允许 PR-02 只替换 planner 实现

### 7.2 确定性保证（优秀）

- `TerrainTagPainter.paint()` 使用 `seed xor ((index + 1L) shl 32) xor tag.ordinal.toLong()` 确保同一 seed 下不同 tag 类型有独立的随机流
- `TopologyFingerprinting.fingerprint()` 使用排序后的 node/edge 序列化 + SHA-256，保证拓扑等价但节点顺序不同的图产出相同 fingerprint
- `candidatePoints` 按 `(y, x)` 排序后选点，消除了遍历顺序对 terrain painting 的影响

### 7.3 兼容性设计（优秀）

- `GeneratedFloor.compatibility()` 工厂方法 + `LinearTopologyProjector.project()` 让 legacy save 可以无感恢复
- `SessionSnapshotMapper.restoreGeneratedFloor()` 有三层防护：
  1. 无 Phase 4 metadata → 走 compatibility 路径
  2. 有 metadata 但无 pipeline → 走 compatibility 路径
  3. 有 metadata + pipeline → 重新生成并做 fingerprint 验证
- `FloorSnapshot` 的 `floorSeed / topologyFingerprint / terrainTagHash` 全部有默认值，确保旧存档可反序列化

### 7.4 Harness 工程质量（优秀）

- `HarnessReportHeader` 统一了所有 Phase 4 harness 的报告头格式
- `Phase4ContractVersions` 集中管理所有版本号，避免散落
- `SolvabilityHarnessSkeleton` 提前冻结了 alias + input boundary，PR-03 可以直接在骨架上扩展
- seed 构造使用 `composeSeed()` 确保 zone/floor/seedOrdinal 组合唯一

### 7.5 游戏设计层面的审视

从 Roguelike / 类 ToME 的角度：

1. **TerrainTag 的权重推导逻辑合理**: `SchemaZoneMapgenProfileResolver.deriveTerrainTagWeights()` 根据 biome 和 specialMechanics 决定 terrain tag 分布，例如 cavern/river → WATER，mine/forge → OIL，crystal/frozen → ICE。这与类 ToME 的 zone mechanic 风味一致。

2. **TerrainTagPainter 的密度控制**: 使用 `candidatePoints.size * ratio * 0.08f` 控制每种 tag 的覆盖面积约为地板面积的 8%，这在视觉和玩法上是合理的 — 太密会让地图变成泥泞，太稀会让 terrain effect 形同虚设。

3. **主路径可达性检查**: `TopologyGraph.isPrimaryPathReachable()` 使用 BFS 验证所有主路径节点双向可达，这对于 Roguelike 地图生成是基本保障 — 不可达的主路径意味着玩家可能被卡死。

4. **Golden 选择的 zone**: BSP golden 选了 `crystal_cavern`（有 ICE + WATER terrain tag），minimal golden 选了 `bandit_camp`（有 optional 分支）。两个样例分别验证了 terrain tag 产出和 PathClass 多样性，覆盖了关键 contract 维度。

---

## 8. 改进建议（非阻塞）

### S-01: 为 `core.mapgen` 包添加 JaCoCo 覆盖率规则

`core/build.gradle.kts` 已在 JaCoCo 规则中加入了 `"com.ktome.core.mapgen.*"` 包（85% line coverage 要求），这很好。建议确保 `TopologyFingerprinting` 和 `TerrainTagPainter` 的边界情况（空 map、空 rooms、单 room）在测试中有覆盖。

### S-02: `MinimalTopologyPlanner` 增加第 5 个节点（SECRET）

规范提到 "3~5 节点"，当前实现最多 4 个。建议增加一个 `SECRET` pathClass 的节点，例如 `hidden-alcove`，这样可以在 PR-01 阶段就验证三种 PathClass 的 golden 覆盖。

### S-03: 考虑 `TerrainTagPainter` 的 determinism 隔离测试

当前 `MapgenContractTest` 只通过 fingerprint 比较间接验证了 terrain tag 确定性。建议增加一个直接比较 `terrainTags` map 的测试，确保每个点位的 tag 集合在相同 seed 下完全一致。

---

## 9. 偏差汇总与修复优先级

| 偏差编号 | 严重度 | 修复工作量 | 是否阻塞合并 | 修复建议 |
|---------|--------|-----------|-------------|---------|
| D-01 | 中 | 1 行 | 否（但建议合并前修） | `optionalLoopCount = if (optionalBranch.isNotEmpty()) 1 else 0` |
| D-02 | 低 | 3 行 | 否 | 添加 `optional -> goal` 的 loop 边，验证 `isLoop = true` |
| S-01 | 建议 | 10 行 | 否 | 增加 edge case 测试 |
| S-02 | 建议 | 15 行 | 否 | 增加 SECRET 节点 |
| S-03 | 建议 | 8 行 | 否 | 增加 direct terrainTags equality 测试 |

---

## 10. 结论

PR-01 的整体实施质量高，contract 设计清晰，模块边界干净，兼容性考虑周全。核心偏差仅有 `MinimalTopologyPlanner` 的 `optionalLoopCount` 语义弱化一处，属于快速修复范围。建议修复 D-01 后合并，D-02 和 S-* 可作为 follow-up 在 PR-02 之前处理。

**合并建议**: 修复 D-01 后可合并。
