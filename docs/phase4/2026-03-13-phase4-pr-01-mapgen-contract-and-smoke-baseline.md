> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`

# Phase 4 - PR-01 Mapgen Contract、TerrainTag 与 Smoke Baseline

**阶段**: `Phase 4 / P4-A / P4-W1a`  
**优先级**: `P0`  
**前置条件**: `Phase 3` 出口全部满足  
**对应问题**: 当前运行时仍由 `BspGenerator -> GameMap` 直接产图，没有 `MapgenPipeline`、`GeneratedFloor`、`TerrainTag` 和独立的 `mapgenSmoke` 门禁。若先写 biome/vault/lock-key 内容，再补 contract，会导致 `core / game / tools` 同时返工。

---

## 1. 阶段目标

先冻结 `Phase 4` 地图生成的最小正式入口，让后续的混合拓扑、可解性验证和隐藏内容都建立在统一词汇上，而不是继续直接操作 BSP 细节。

完成标准：

1. `MapgenRequest / ZoneMapgenProfile / TopologyGraph / GeneratedFloor / MapgenPipeline` 进入正式词汇。
2. `TerrainTag` 第一版固定为 `WATER / OIL / ICE`。
3. `BspGenerator` 降级为兼容后端，不再是 `Phase 4` 的执行 contract。
4. `PR-01` 除了 BSP 兼容层外，还必须交付一个最小非 BSP 的 `TopologyPlanner` 骨架，用来验证新 contract 不是空包装。
5. `mapgenSmoke` 作为 root Gradle alias 建立，并约定固定报告产物。
6. 地图 golden 从“截图主导”切到“拓扑摘要 + seed + 约束结果”的可重放边界。

## 2. 当前问题

1. `game` 仍以 `BspGenerator` 直接生成 `GameMap`，没有可扩展的 mapgen 抽象边界。
2. zone 只有 `biome / mapSize / tilesetKey` 这类 Phase 3 口径，无法承载 `Phase 4` 的 biome family、loop、vault 和 floor-level generation metadata。
3. 当前没有 `TerrainTag` 的规则层归属；如果先把水面/油污写进 client 或 zone mechanic，会破坏模块边界。
4. `mapgenSmoke` 任务和报告产物尚不存在，无法把 `Phase 4` checklist 量化门禁落到命令级 contract。
5. 如果 `PR-01` 只交付 `BspBackedMapgenPipeline` 这种 pass-through wrapper，那么 contract 是否合理要等到 `PR-02` 才能验证，边界过薄。

### 2.1 本 PR 必须冻结的口径

1. `MapgenPipeline.run(request)` 成为正式 floor generation 入口；后续 `P4-W1b/W2` 只能消费该入口，不再各自直调 `BspGenerator`。
2. `GeneratedFloor` 必须保留 `zoneId / floorIndex / seed / topology`，作为 reproducibility 最小边界。
3. `TopologyGraph` 在本 PR 先冻结节点/边和主路径摘要，但必须预留 `pathClass / requiredKeys` 字段，避免 `PR-02 / PR-03` 再回头修改基础 DTO。
4. `TerrainTag` 第一版只允许 `WATER / OIL / ICE`；新增 tag 必须作为后续 schema 变更处理。
5. `BspGenerator` 只允许作为 `MapgenPipeline` 的兼容实现或 fallback，不再直接暴露给 `game` 主路径。
6. root alias `./gradlew mapgenSmoke` 必须在本 PR 起草，并固定报告路径：
   - `tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json`
   - `tools/build/reports/phase4/mapgen/mapgen-smoke-seeds.jsonl`
7. `GeneratedFloor.map` 在 `Phase 4` 内保留为兼容字段，但从 `PR-01` 起不再是拓扑、可解性或隐藏内容的真源；后续所有新增 Phase 4 逻辑必须优先消费 `GeneratedFloor.topology / terrainTags / entrances`。
8. `GeneratedFloor.map` 的移除时点固定为：`Phase 4` 全阶段保留兼容；只有当 `save/replay/client` 的 floor 装配都不再依赖 `GameMap` 逆推结构后，才允许在 `Phase 5` 单独发起删除 PR。

## 3. 范围与非目标

### 3.1 范围

1. 新建 `core.mapgen` contract 包，定义 Phase 4 正式 mapgen DTO / interface。
2. 给 `game` 增加 `ZoneMapgenProfile` 装配入口和 BSP 兼容适配层。
3. 给 `RenderSnapshot` / floor runtime 增加最小 `TerrainTag` 携带能力。
4. 建立 `mapgenSmoke` 的 root alias、报告格式和固定 seed 输入口径。
5. 约定地图 golden 重录策略和产物目录。

### 3.2 非目标

1. 不在本 PR 落混合拓扑 planner、pattern room、vault 布局或 biome family 数据。
2. 不在本 PR 落 `SolvabilityGraph`、lock-key DAG 或 hidden entrance。
3. 不在本 PR 改正式掉落预算。
4. 不在本 PR 引入任何新的 visual/audio key。

## 4. 技术方案

### 4.1 Contract 与兼容后端

建议文件：

```text
core/src/main/kotlin/com/ktome/core/mapgen/MapgenContracts.kt
core/src/main/kotlin/com/ktome/core/mapgen/BspBackedMapgenPipeline.kt
core/src/test/kotlin/com/ktome/core/mapgen/MapgenContractTest.kt
```

最小 contract：

```kotlin
@JvmInline
value class NodeId(val value: String)

@JvmInline
value class RequirementRef(val value: String)

enum class PathClass {
    CRITICAL_PATH,
    OPTIONAL,
    SECRET,
}

data class MapgenRequest(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val targetWidth: Int,
    val targetHeight: Int,
)

enum class TerrainTag {
    WATER,
    OIL,
    ICE,
}

data class ZoneMapgenProfile(
    val zoneId: String,
    val allowedBiomeFamilies: Set<String>,
    val loopCountRange: IntRange,
    val vaultPool: Set<String>,
    val terrainTagWeights: Map<TerrainTag, Float>,
    val roomTagFilter: Set<String>,
)

data class TopologyNode(
    val id: NodeId,
    val roomDefId: String,
    val pathClass: PathClass,
    val tags: Set<String>,
)

data class TopologyEdge(
    val from: NodeId,
    val to: NodeId,
    val isLoop: Boolean = false,
    val requiredKeys: Set<RequirementRef> = emptySet(),
)

data class TopologyGraph(
    val nodes: List<TopologyNode>,
    val edges: List<TopologyEdge>,
    val primaryPathNodeIds: List<NodeId>,
    val optionalLoopCount: Int,
)

data class GeneratedFloor(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val topology: TopologyGraph,
    val terrainTags: Map<Point, Set<TerrainTag>>,
    val map: GameMap, // compatibility adapter for existing session assembly
)

interface ZoneMapgenProfileResolver {
    fun resolve(zoneId: String): ZoneMapgenProfile
}

interface MapgenPipeline {
    fun run(request: MapgenRequest): GeneratedFloor
}
```

约束：

1. `GeneratedFloor.map` 暂时保留 `GameMap`，避免一次性重写现有 session/floor 装配；但从本 PR 起任何新的拓扑/可解性/隐藏内容逻辑都不得再从 `GameMap` 反推结构语义。
2. `TopologyNode.roomDefId` 在兼容阶段允许回填 BSP room synthetic id，例如 `bsp.rect.standard`。
3. `terrainTags` 在本 PR 允许为空，但字段本身必须固定存在。
4. `PathClass` 在本 PR 可先由最小 planner 和 BSP 适配器填默认值，但字段必须进入正式 contract。
5. `MapgenPipeline` 的正式调用边界不直接暴露完整 `ZoneMapgenProfile`；profile 解析责任留在 `game` 的 resolver 或 pipeline 内部依赖里，避免 `PR-02 / PR-08` 之后 profile 解析路径在多个模块分叉。

### 4.2 最小 planner 骨架与 BSP 兼容适配层

`game` 当前 `GameModule.generateFloor(...)` 不直接调 `BspGenerator`，而是改为：

1. 先由 `ZoneMapgenProfileResolver` 从 zone schema 解析或派生 `ZoneMapgenProfile`。
2. 调用 `MapgenPipeline.run(request)`。
3. 从 `GeneratedFloor.map` 继续走现有 monster / item / interactable 装配。

本 PR 必须同时交付两个实现：

1. `MinimalTopologyPlanner`
   - 不是 BSP wrapper，而是最小 `3~5` 节点的 deterministic planner
   - 至少能产出 `1` 条主路径 + `0~1` 条支路
   - 作用是验证 `TopologyGraph / PathClass / primaryPathNodeIds` 的字段设计
2. `BspBackedMapgenPipeline`
   - 作为当前主路径的兼容后端
   - 负责把既有 BSP 结果投影到新 contract

这样 `PR-02` 可以专注替换为真正的混合拓扑 planner，而不是第一次验证 contract 是否合理。

### 4.3 `TerrainTag` 的最小接线

本 PR 只冻结归属，不冻结玩法：

1. `MapgenPipeline` 生产 `terrainTags`。
2. `game` floor runtime 保存 `terrainTags` 到可重放 floor payload。
3. `RenderSnapshot` 只携带消费 `TerrainTag` 所需的只读结果，不在 client 中重建规则语义。
4. `CombatPipeline step 9` 的正式元素交互接线留给 `PR-06`。

### 4.4 `mapgenSmoke` alias 与报告契约

建议任务落位：

```text
tools/src/main/kotlin/com/ktome/tools/mapgen/MapgenSmokeRunner.kt
build.gradle.kts
```

root alias：

```text
./gradlew mapgenSmoke
```

报告最小字段：

1. `buildId`
2. `phaseId: P4`
3. `contentSchemaVersion`
4. `topologyFingerprintVersion`
5. `harnessId: mapgenSmoke`
6. `seed`
7. `zoneId`
8. `floorIndex`
9. `topologySummary`
10. `criticalPathReachable`
11. `loopCount`
12. `terrainTagDistribution`
13. `topologyFingerprint`

### 4.5 floor-level persistence 最小边界

本 PR 不实现完整 save/replay，但必须把 `Phase 4` floor generation 的最小持久化字段先冻结，避免 `PR-03 / PR-07` 各自补第二套 floor metadata。

最低字段：

1. `zoneId`
2. `floorIndex`
3. `floorSeed`
4. `topologyFingerprint`
5. `terrainTagHash`
6. `topologyFingerprintVersion`
7. `activePackIds: List<PackId>`
8. `activePackManifestVersions: Map<PackId, String>`

说明：

1. `revealedEntranceIds / visitedSecretZoneIds / searchState` 在 `PR-03 / PR-07` 冻结具体内容，并挂在这里已经预留好的 run-scoped floor state 上。
2. `pityTracker` 由 `PR-04` 明确冻结为 run-scoped 状态，不挂在 floor state 下。
3. `activePackIds / activePackManifestVersions` 在正式 save/replay DTO 中复用全局 typed `PackId` 口径，不再回退为裸字符串集合。
4. `GeneratedFloor.map` 不进入这些持久化字段列表。

### 4.6 地图 golden 重录策略

本 PR 起正式改为双层产物：

1. `拓扑 golden`：`tools/src/test/resources/golden/phase4/mapgen/*.json`
2. `截图/可视检查`：继续由 `goldenScreenshot` 消费，但只作为 client 表现辅助，不再承担规则真源职责。

## 5. 推荐改动面

### 5.1 `core`

1. 新建 `core.mapgen` 包并引入正式 contract。
2. 为现有 BSP 生成器提供 `MapgenPipeline` 兼容实现。
3. 增加 contract 单测，覆盖 determinism、empty terrain、primary path 最小不变量。

### 5.2 `game`

1. 为 zone 增加 `ZoneMapgenProfile` 的数据入口或派生策略。
2. `GameModule.generateFloor(...)` 切到 `MapgenPipeline`。
3. 保存 `GeneratedFloor` 的 seed/topology/terrainTag 元数据，供后续 harness 和 save/replay 使用。
4. 提供 `ZoneMapgenProfileResolver`，避免调用方直接持有完整 profile。

### 5.3 `tools`

1. 新建 `mapgenSmoke` runner。
2. 固定输出 summary JSON + per-seed JSONL。
3. 在 root `build.gradle.kts` 暴露 alias。

## 6. 测试与自证

### 6.1 必测行为

1. 同一 `MapgenRequest` 在相同 `ZoneMapgenProfileResolver` 快照输入与 seed 下，经 `MapgenPipeline.run(request)` 生成的 `TopologyGraph` 稳定一致。
2. BSP 兼容后端生成的 `GeneratedFloor` 不为空，且 `primaryPathNodeIds` 非空。
3. `terrainTags` 字段即使为空也序列化稳定，不破坏现有 floor 装配。
4. `MinimalTopologyPlanner` 生成的非 BSP 样例能通过 smoke，并验证 `PathClass` 字段可用。
5. `mapgenSmoke` 固定 seed 运行时输出完整 summary/report 文件。

### 6.2 自动化命令

```bash
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
```

说明：

1. `solvabilityHarness` 在本 PR 仍可为空骨架或 pending alias，但命名和输入边界必须与 `PR-03` 保持一致。
2. Phase 4 checklist 的正式阈值以 `500 seed / 0 crash / 0 empty map / P95 < 2s` 为后续收口标准，本 PR 至少要能稳定跑固定 seed smoke。

## 7. 出口门禁

1. `MapgenPipeline` contract 和 `TerrainTag` 归属冻结。
2. `game` 主路径不再直接依赖 `BspGenerator`。
3. `mapgenSmoke` root alias 建立，并有固定报告产物路径。
4. `PR-01` 已交付一个最小非 BSP planner，用于证明新 contract 可被真实消费者使用，而不是纯 pass-through 包装。
5. `topologyFingerprintVersion` 与 floor-level persistence 最小边界已经冻结，后续 `PR-03 / PR-07` 不再自定义第二套 floor metadata。
6. 后续 `PR-02 / PR-03` 可以在不改 session 主装配的前提下替换 planner 和追加 solvability。
