> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`

# Phase 4 - ProcGen, Loot & Content Pack

**阶段**: `Phase 4`  
**版本目标**: `v0.4.x`  
**优先级**: `P1`  
**前置条件**: `Phase 3` 出口全部满足  
**对应问题**: `Phase 3` 已经能形成稳定长局，但不同 run 之间的地图、掉落、精英遭遇和隐藏内容差异还不够，且内容扩展还没有正式 overlay/harness 路径。

---

## 1. 阶段目标

把游戏从“可完成长局”推进到“重复游玩差异明显的长局”，同时让内容扩展进入可验证的数据包路径。

完成标准：

1. 混合拓扑地图、pattern room、vault、环路和 biome family 进入正式主线。
2. `lock-key DAG`、隐藏入口、任务拓扑具备自动化可解性验证。
3. `affix / unique / artifact / elite mutation / hidden event / secret zone` 形成成体系掉落与遭遇生态。
4. `content pack overlay + schema lint + harness` 能在不改 `core` 的前提下装载新增内容。
5. `Phase 4` 的地图、掉落、事件和内容包合同能直接作为 `Phase 5` soak、性能和 QA 的输入，而不是在 `Phase 5` 再返工。

## 2. 当前问题

1. `Phase 3` 的世界结构仍偏“固定骨架 + 内容拼接”。
2. 掉落系统足以驱动构筑，但还不能支撑长期重复游玩差异。
3. 隐藏区域、锁钥匙和 secret logic 缺少自动化可解性验证。
4. 新内容仍主要靠主仓库直写，没有正式 overlay/harness 路径。
5. `Phase 4` 涉及 `ProcGen / Loot / Hidden Content / Content Pack` 四套新合同，如果继续停留在“设计意向”层，进入代码实现后会同时出现算法分叉、schema 漂移和 QA 无法量化的问题。

### 2.1 本阶段必须冻结的系统

| 检查点 | 必须冻结的系统 | 冻结结果 |
| --- | --- | --- |
| `P4-A` | `MapgenPipeline`、`BiomeFamilyDef`、`TerrainTag`、`SolvabilityGraph` | 地图拓扑、环路、地形标签和可解性证明有统一词汇 |
| `P4-B` | `LootBudget`、`RarityTier`、`EliteMutationDef`、`HiddenEventDef`、`SecretZoneDef` | 掉落数学模型和隐藏内容 contract 固定 |
| `P4-C` | `ContentPackManifest`、`OverlayEntry`、pack schema lint / harness | 外部 pack 有清晰版本、冲突和验证边界 |

补充冻结说明：

1. `Phase 4` 不再增加新职业，只扩内容和 build 差异。
2. `castSpeed` 的收益递减在 `Phase 4` 正式启用；任何掉落、affix 或 mutation 提供的 `castSpeed` 都必须走既有递减函数，不允许直接旁路。
3. 元素亲和度系统只保留为 `optional lab`，不进入 `Phase 4` 主线门禁；本阶段主线只冻结地形联动交互和元素 affix。

## 3. 范围、非目标与执行模型

### 3.1 范围

1. ProcGen 深化与 map family。
2. 锁钥匙、隐藏入口、探索奖励与可解性验证。
3. Loot 生态 V2 与预算模型。
4. 精英突变、Boss 变体和隐藏事件。
5. 数据包 overlay、lint、headless harness 与示例 content pack。
6. `TerrainTag` 与 `CombatPipeline step 9` 的正式接线。

### 3.2 非目标

1. 不引入任何通用脚本 runtime，包括 `Lua / GraalJS / WASM / Python`。
2. 不做完整 Mod SDK，不做运行时脚本热更。
3. 不新增职业、主属性、伤害通道、资源类型或 `core` 规则语义。
4. content pack 不允许定义新的 `DamageType / PowerType / StatusEffectType / ResourceType`。
5. 元素亲和度系统不进入 `Phase 4` 主线出口，只允许作为 isolated lab 或后续阶段候选能力。

### 3.3 并行开发线与工作包执行原则

`Phase 4` 继续采用 `Rules / Client / Content / Tools-QA` 四条 lane，而不是单链串行推进：

1. `Rules Lane`
   - `MapgenPipeline`
   - `SolvabilityGraph`
   - `LootBudget`
   - `TerrainTag` 与战斗回调接线
2. `Content Lane`
   - biome family
   - vault / pattern room
   - affix / unique / artifact
   - elite mutation / hidden event / secret zone
3. `Client Lane`
   - 精英突变可读性
   - hidden entrance / secret zone 的表现约定
   - content pack 加载后的可见内容验证
4. `Tools/QA Lane`
   - `mapgenSmoke`
   - `solvabilityHarness`
   - `lootBalanceLab`
   - `hiddenContentHarness`
   - `contentPackHarness`

执行约束：

1. `P4-W1` 与 `P4-W3` 在 `Phase 3` 出口后允许并行起步。
2. `P4-W2` 依赖 `P4-W1` 的拓扑图和 room/vault 词汇，不允许在没有 `MapgenPipeline` 的前提下先写“可解性例外特判”。
3. `P4-W4` 必须建立在 `P4-W2 + P4-W3` 的正式 contract 上；hidden reward 不能绕过 loot budget，secret zone 不能绕过 solvability。
4. `P4-W5` 的 loader / lint 可以在 `P4-W3` 的 affix / item / event schema 稳定后起稿，但 headless harness 依赖 `P4-W4` 的 hidden content registry 完整；overlay 不能一边跟着 schema 漂移一边实现 loader。

### 3.4 工作量与人力建议

最低人力模型：

1. `1` 名 rules owner 负责 `P4-W1 / W2 / W3` 的正式 contract。
2. `1` 名 content owner 负责 `vault / biome / affix / hidden event / content pack` 数据样例。
3. `0.5 ~ 1` 名 client owner 负责突变可读性、秘密入口提示、pack 内容可见性。
4. `1` 名 tools/QA owner 负责 batch/harness、量化门槛和 reproducibility 报告。

原因：

1. `ProcGen` 和 `Content Pack` 都是高返工风险系统，不适合由纯内容人员在 contract 未冻结时先行自由扩写。
2. `LootBalanceLab` 和 `SolvabilityHarness` 是 `Phase 4` 的门禁核心，必须和规则 owner 同步推进，而不是事后补一个脚本。

## 4. 技术方案

### 4.1 混合拓扑地图

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/mapgen/*
game/src/main/resources/data/mapgen/*.yaml
core/src/test/kotlin/com/ktome/core/mapgen/*
```

#### 4.1.1 最小算法路径

`Phase 4` 的地图生成不采用 WFC 或图文法深水区；本阶段固定采用“拓扑模板 + 房间实例化 + 环路注入 + biome 着色 + 校验”的混合流水线。

固定流水线：

1. `Seed -> ZoneMapgenProfile`
2. `ZoneMapgenProfile -> TopologyGraph`
3. `TopologyGraph -> RoomInstance`
4. `RoomInstance -> Corridor / Loop / HiddenEntrance`
5. `RoomInstance + BiomeFamily -> TerrainTag paint`
6. `GeneratedFloor -> Invariant / Solvability validation`

`TerrainTag` 合并规则固定为：

1. 先以 `BiomeFamilyDef.terrainTagWeights` 作为默认值。
2. 若 `ZoneMapgenProfile.terrainTagWeights` 为某个 tag 显式给出权重，则覆盖 biome 默认值。
3. 未在 zone 级显式声明的 tag，继续沿用 biome 级默认值。

`room tag` 合并规则固定为：

1. 先以 `BiomeFamilyDef.allowedRoomTags` 作为候选上限。
2. 再用 `ZoneMapgenProfile.roomTagFilter` 从中过滤。
3. 最终候选集为两者交集；若 zone 未显式收窄，则沿用 biome 的完整候选集。

Kotlin 骨架：

```kotlin
data class MapgenRequest(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val targetWidth: Int,
    val targetHeight: Int,
)

data class ZoneLootConfig(
    val rarityBonus: Float,
    val qualityBonus: Int,
)

data class ZoneMapgenProfile(
    val zoneId: String,
    val allowedBiomeFamilies: Set<String>,
    val loopCountRange: IntRange,
    val vaultPool: Set<String>,
    val terrainTagWeights: Map<TerrainTag, Float>,
    val roomTagFilter: Set<String>,
    val zoneLootConfig: ZoneLootConfig,
)

data class TopologyNode(
    val id: String,
    val roomDefId: String,
    val pathClass: PathClass,
    val tags: Set<String>,
)

data class TopologyEdge(
    val from: String,
    val to: String,
    val isLoop: Boolean = false,
    val requiredKeys: Set<String> = emptySet(),
)

data class TopologyGraph(
    val nodes: List<TopologyNode>,
    val edges: List<TopologyEdge>,
    val primaryPathNodeIds: List<String>,
    val optionalLoopCount: Int,
)

data class HiddenEntranceInstance(
    val fromNodeId: String,
    val targetNodeId: String,
    val discoveryRule: DiscoveryRule,
)

data class VaultPlacement(
    val vaultId: String,
    val nodeId: String,
    val roomDefId: String,
)

data class RoomInstance(
    val nodeId: String,
    val roomDefId: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val patternId: String?,
)

data class GeneratedFloor(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val topology: TopologyGraph,
    val rooms: List<RoomInstance>,
    val terrainTags: Map<Point, Set<TerrainTag>>,
    val entrances: List<HiddenEntranceInstance>,
    val vaultPlacements: List<VaultPlacement>,
)

interface TopologyPlanner {
    fun plan(profile: ZoneMapgenProfile, seed: Long): TopologyGraph
}

interface MapgenPipeline {
    fun run(request: MapgenRequest): GeneratedFloor
}
```

#### 4.1.2 Room / Pattern / Vault / Biome 数据结构

```kotlin
enum class RoomShape {
    RECT,
    L_SHAPE,
    ROUND,
    IRREGULAR,
}

enum class TerrainTag {
    WATER,
    OIL,
    ICE,
}

data class RoomDef(
    val id: String,
    val shape: RoomShape,
    val widthRange: IntRange,
    val heightRange: IntRange,
    val tags: Set<String>,
)

data class PatternRoomDef(
    val id: String,
    val baseRoomId: String,
    val patternId: String,
    val requiredTags: Set<String>,
    val spawnWeight: Int,
)

data class VaultDef(
    val id: String,
    val templateId: String,
    val pathClass: PathClass,
    val threatBudget: Int,
    val rewardBudget: Int,
    val allowOnBiomeFamilies: Set<String>,
    val requiredTerrainTags: Set<TerrainTag>,
)

data class BiomeFamilyDef(
    val id: String,
    val primaryTileSet: String,
    val secondaryTileSet: String?,
    val terrainTagWeights: Map<TerrainTag, Float>,
    val allowedRoomTags: Set<String>,
)
```

冻结口径：

1. `pattern room` 是“在标准房间边界内应用固定布局模式”的房间，不是独立脚本宿主。
2. `vault` 必须声明 `pathClass`；`CRITICAL_PATH` 禁止使用高风险高回报 vault。
3. 同层最多允许 `2` 个 biome family 混合，不做三种以上混合。
4. `TerrainTag` 在 `Phase 4` 只引入 `WATER / OIL / ICE` 三类，不把 lava、poison cloud 等再提前塞进主线。
5. `Point` 复用 `core.map.Point`；`GeneratedFloor` 必须保留 `zoneId / floorIndex / seed` 以支撑 reproducibility。
6. `patternId` 指向 `data/mapgen/patterns/{patternId}.yaml`，用于定义房间内 tile 覆盖、机关、陷阱和实体放置规则。
7. `templateId` 指向手工制作的 vault 布局模板；`Phase 4` 不支持参数化 vault 生成器，也不支持从目录自动推断模板。
8. `spawnWeight` 是同一 `baseRoomId` 下多个候选 pattern 之间的相对权重，数值越大越常见。
9. `threatBudget` 使用遭遇预算货币，`14` 约等于 `3` 个普通怪 + `1` 个精英怪的压力；`rewardBudget` 使用奖励预算货币，`18` 约等于 `2` 次高质量掉落 roll 或 `1` 个 secret chest。

#### 4.1.3 环路与拓扑约束

环路生成固定约束：

1. 每层至少 `1` 条主路径。
2. 环路数量 `0 ~ 2` 条；boss floor 默认 `0 ~ 1` 条。
3. 单条环路长度至少 `3` 个节点，最多 `7` 个节点。
4. 若 `optionalLoopCount > 0`，则环路边数量占总连通边数量的比例控制在 `0.15 ~ 0.35`。
5. `vault`、秘密入口和隐藏奖励只允许落在 `OPTIONAL / SECRET` 路径，不允许直接落在 entry room 或 final exit room。

#### 4.1.4 biome family 与 zone 升级口径

`Phase 4` 不新增主线 zone；主线做法是升级既有 zone 的 biome family 和可选 secret content：

| Zone ID | Phase 4 biome family | 关键 TerrainTag | `rarityBonus` | `qualityBonus` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `greenwood_fringe` | `forest` + `bog` | `WATER` | `0.00` | `0` | 视野遮挡与潮湿地形并存 |
| `deep_iron_pit` | `mine` + `forge` | `OIL` | `0.05` | `1` | 油污、机械区和锻炉区混合 |
| `underground_river` | `flooded_cavern` + `crystal_bank` | `WATER`, `ICE` | `0.08` | `1` | 地形交互主实验场 |
| `abyssal_temple` | `ruin` + `oil_catacomb` | `OIL` | `0.12` | `2` | hidden altar 与高压战斗区 |

#### 4.1.5 YAML 示例

`vault` 最小示例：

```yaml
id: flooded_cache
templateId: vault.flooded_cache
pathClass: OPTIONAL
threatBudget: 14
rewardBudget: 18
allowOnBiomeFamilies: [flooded_cavern]
requiredTerrainTags: [WATER]
```

`biome family` 最小示例：

```yaml
id: flooded_cavern
primaryTileSet: biome.cavern.wet
secondaryTileSet: biome.cavern.crystal
terrainTagWeights:
  WATER: 0.22
  OIL: 0.00
  ICE: 0.05
allowedRoomTags: [ambush, shrine, hidden_cache]
```

`zone mapgen profile` 最小示例：

```yaml
zoneId: underground_river
allowedBiomeFamilies: [flooded_cavern, crystal_bank]
loopCountRange: [0, 2]
vaultPool: [flooded_cache, crystal_shrine]
terrainTagWeights:
  WATER: 0.24
  OIL: 0.00
  ICE: 0.08
roomTagFilter: [ambush, shrine, hidden_cache]
zoneLootConfig:
  rarityBonus: 0.08
  qualityBonus: 1
```

### 4.2 Lock-Key 与可解性验证

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/world/solvability/*
tools/src/main/kotlin/com/ktome/tools/solvability/*
```

#### 4.2.1 图结构与词汇

```kotlin
enum class PathClass {
    CRITICAL_PATH,
    OPTIONAL,
    SECRET,
}

enum class KeyType {
    KEY_ITEM,
    SWITCH,
    BOSS_SIGIL,
    QUEST_FLAG,
    PERCEPTION_REVEAL,
}

enum class DiscoveryRuleType {
    AUTO_REVEAL,
    PERCEPTION_CHECK,
    INTERACTABLE_TILE,
    DROP_FROM_ELITE,
    QUEST_STEP,
}

data class SolvabilityNode(
    val id: String,
    val pathClass: PathClass,
    val roomId: String,
    val grants: Set<String>,
)

data class SolvabilityEdge(
    val from: String,
    val to: String,
    val requiredKeys: Set<String>,
    val discoveryRule: DiscoveryRule?,
)

data class SolvabilityGraph(
    val nodes: List<SolvabilityNode>,
    val edges: List<SolvabilityEdge>,
    val entryNodeId: String,
    val exitNodeId: String,
)

data class SolvabilityProof(
    val criticalPathReachable: Boolean,
    val acquiredKeys: List<String>,
    val unresolvedRequirements: List<String>,
    val visitedNodes: List<String>,
)

interface SolvabilityGraphBuilder {
    fun build(floor: GeneratedFloor): SolvabilityGraph
}
```

冻结定义：

1. `Node` 代表 room/zone 层级的可达性单元，不直接把“单扇门”当成节点。
2. `Edge` 代表从一个可达单元到另一个可达单元的进入条件。
3. `CRITICAL_PATH` 代表推进主线或进入最终出口所需的节点。
4. `OPTIONAL` 可包含奖励、支线、elite、sub-boss。
5. `SECRET` 允许需要发现，但不得承载主线必需钥匙或必需剧情旗标。
6. `discoveryRule = null` 表示该边默认可通行，不需要任何发现条件。

#### 4.2.2 可解性验证算法

`SolvabilityHarness` 固定采用“拓扑排序 + 前向可达性证明”：

1. 先从 `entryNodeId` 建立初始 frontier。
2. 在每一步中，收集当前可达节点提供的 `key / switch / quest flag`。
3. 用当前持有能力扩展下一批可达边。
4. 若 `CRITICAL_PATH` 存在未满足依赖且 frontier 不再增长，则判定为不可解。
5. `OPTIONAL / SECRET` 不参与主线 hard fail，但其返回主线的桥接边必须仍可证明。

构建路径：

1. `MapgenPipeline` 先生成 `GeneratedFloor`。
2. `SolvabilityGraphBuilder` 从 `GeneratedFloor.topology`、hidden entrance、钥匙投放和 zone flags 构建 `SolvabilityGraph`。
3. `SolvabilityHarness` 只消费 `SolvabilityGraph`，不直接操作 map tiles。
4. key-gate placement 的真源是 `TopologyEdge.requiredKeys`；`TopologyNode.tags` 只承载内容标签，不编码主线钥匙语义。

最小可解性约束固定为：

1. 从入口到出口至少存在一条可达主路径。
2. 所有钥匙 / 开关都必须在被需求之前可获取。
3. 所有 `OPTIONAL / SECRET` 区域都必须至少有一条能回主线的路径。
4. Boss 门后不允许藏任何主线必需物品或必需钥匙。
5. 隐藏入口允许需要发现，但 discovery rule 失败不应阻断主线。

#### 4.2.3 Hidden Entrance 与 Secret Zone

```kotlin
data class DiscoveryRule(
    val type: DiscoveryRuleType,
    val difficulty: Int? = null,
    val requiredTag: String? = null,
)

data class HiddenEntranceDef(
    val id: String,
    val discoveryRule: DiscoveryRule,
    val targetSecretZoneId: String,
    val pathClass: PathClass,
)

data class SecretZoneDef(
    val id: String,
    val entryRule: DiscoveryRule,
    val pathClass: PathClass,
    val rewardProfileId: String,
    val guaranteedContent: List<String>,
)
```

冻结口径：

1. `secret zone` 是 world graph 中的隐藏可达区域，不是普通 zone 的视觉换皮。
2. 进入 `secret zone` 的发现条件必须结构化表达，不能写“代码里掷一个随机数”。
3. `secret zone` 的内容最少包含以下之一：
   - 1 个 secret vault
   - 1 个 named elite / boss variant
   - 1 个 hidden event + 1 个奖励节点
4. `secret zone` 的奖励允许明显偏高，但不得承担主线必需钥匙。
5. `Phase 4` 默认每个 `secret zone` 只有一个入口；`SecretZoneDef.entryRule` 必须与对应 `HiddenEntranceDef.discoveryRule` 一致，`SolvabilityHarness` 需要校验两者匹配。
6. `guaranteedContent` 使用 `{registryName}.{entryId}` 格式引用内容条目，`registryName` 必须与 overlay 中使用的 registry 名称一致。
7. `REVEAL_SECRET_ZONE` 只使既有入口在地图或 UI 上可见，等同于自动通过 discovery check，不创建新的物理入口。
8. 用于揭示 secret zone 的 hidden event 默认位于主 zone 或外层 optional 路径，不计入该 secret zone 的 `guaranteedContent`。

`hidden entrance` 最小示例：

```yaml
id: moss_wall_crevice
discoveryRule:
  type: PERCEPTION_CHECK
  difficulty: 14
targetSecretZoneId: forgotten_cache
pathClass: SECRET
```

`secret zone` 最小示例：

```yaml
id: forgotten_cache
entryRule:
  type: PERCEPTION_CHECK
  difficulty: 14
pathClass: SECRET
rewardProfileId: secret.cache.river
guaranteedContent:
  - vault.flooded_cache
```

### 4.3 Loot 生态 V2

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/loot/*
game/src/main/resources/data/items/*.yaml
tools/src/main/kotlin/com/ktome/tools/balance/*
```

#### 4.3.1 预算与概率模型

```kotlin
data class LootRollContext(
    val sourceLevel: Int,
    val sourceTier: SourceTier,
    val zoneId: String,
    val playerLevel: Int,
    val magicFindBonus: Float,
    val seed: Long,
)

enum class RarityTier(
    val baseWeight: Int,
    val qualityBonus: Int,
    val baseBudget: Int,
) {
    NORMAL(720, 0, 0),
    MAGIC(220, 1, 6),
    RARE(50, 3, 14),
    UNIQUE(9, 5, 0),
    ARTIFACT(1, 7, 0),
}

data class LootBudget(
    val iLvl: Int,
    val qLvl: Int,
    val rarityTier: RarityTier,
    val rarityScore: Float,
    val affixBudget: Int,
)

enum class SourceTier(
    val itemLevelBonus: Int,
    val rarityBonus: Float,
    val affixBudgetBonus: Int,
) {
    NORMAL(0, 0.00f, 0),
    ELITE(1, 0.15f, 2),
    BOSS(2, 0.40f, 4),
    CHEST(1, 0.10f, 1),
}
```

正式公式：

1. `iLvl = clamp(sourceLevel + sourceTier.itemLevelBonus + uniformInt(-1, 1), 1, playerLevel + 3)`
2. `effectiveMagicFind = clamp(magicFindBonus, 0.0, 1.0)`
3. `rarityScore = sourceTier.rarityBonus + zoneLootConfig.rarityBonus + effectiveMagicFind * 0.50`
4. `qLvl = clamp(iLvl + rarityTier.qualityBonus + zoneLootConfig.qualityBonus, iLvl, iLvl + 6)`
5. `affixBudget = qLvl * 2 + rarityTier.baseBudget + sourceTier.affixBudgetBonus`

`rarityScore` 的权重修正规则：

1. `NORMAL` 不受 `rarityScore` 直接放大，只作为归一化余量存在。
2. `MAGIC` 权重乘以 `1 + rarityScore * 0.50`
3. `RARE` 权重乘以 `1 + rarityScore * 1.00`
4. `UNIQUE` 权重乘以 `1 + rarityScore * 1.50`
5. `ARTIFACT` 权重乘以 `1 + rarityScore * 2.00`

稀有度选择算法固定为：

1. 先根据 zone 与 source tier 过滤出当前上下文允许的 `RarityTier` 候选集。
2. 对候选集应用上表修正后的权重并做归一化。
3. 用单次加权随机决定最终 tier。
4. 若 roll 到 `UNIQUE / ARTIFACT` 但当前上下文没有可用模板，则按 `ARTIFACT -> UNIQUE -> RARE -> MAGIC` 顺序降级，直到命中可用模板。

解释：

1. `BOSS 0.40` 的设计含义不是“必掉高稀有”，而是把高 tier 权重提升到明显可感知，但仍需要 batch 才能稳定观测的级别。
2. `magicFindBonus` 只影响稀有度，不直接改 `iLvl`；否则会让低层 Boss 通过堆 `magic find` 产出越级基础物品。
3. `castSpeed` 若作为 affix 出现，只能通过既有收益递减函数生效，不能因为掉落词条而跳过 `DR_CAST_SPEED_C`。
4. `zoneLootConfig` 来自 zone 本身的内容定位，而不是运行时随机摇点；同一 zone 的 rarity/quality 调性必须稳定。
5. 只有 `MAGIC / RARE` 实际消费 `affixBudget`；`NORMAL` 忽略此值，`UNIQUE / ARTIFACT` 使用固定模板不走 budget 扣减。

#### 4.3.2 affix cost 体系

```kotlin
data class AffixCost(
    val affixId: String,
    val cost: Int,
    val slotTags: Set<String>,
    val phase: String,
)
```

首批成本带固定为：

| 成本带 | cost | 示例 |
| --- | --- | --- |
| `MINOR` | `3` | `+8 fireResistance` |
| `MEDIUM` | `6` | `+8 firePenetration` |
| `MAJOR` | `10` | `+12 castSpeedRating` |
| `SIGNATURE` | `14` | `chance to ignite on crit` |

统一规则：

1. `MAGIC` 允许 `1~2` 个 affix，预算只从 `affixBudget` 扣减。
2. `RARE` 允许 `2~4` 个 affix，消耗总和不能超过 `affixBudget`。
3. `UNIQUE / ARTIFACT` 在 `Phase 4` 默认使用固定模板，不再做二次随机 affix 拼装。
4. 同一 affix family 不能在同一物品上重复出现。

#### 4.3.3 Unique / Artifact / MagicFind 边界

冻结口径：

1. `Unique` 来自预定义模板池，按 zone 和 source tier 过滤后进入候选表。
2. `Artifact` 只允许来自 `BOSS / SECRET_ZONE / special reward chest`，不允许普通怪随机掉出。
3. `magicFindBonus` 的来源固定为：
   - 装备 affix
   - shrine / hidden event 的临时 buff
   - zone modifier
4. `magicFindBonus` 使用加法累积，最终 clamp 到 `1.0`；`Phase 4` 不做单独的 diminishing returns。

`affix` 最小示例：

```yaml
id: of_flame
slotTags: [weapon]
phase: P4
cost: 6
modifiers:
  - stat: firePenetration
    amount: 8
```

#### 4.3.4 LootBalanceLab 与已有工具关系

`LootBalanceLab` 不是单独重写的工具链，而是建立在 `Phase 3` 已有 seed lab / harness 基础设施之上的新实验室：

1. 继续复用固定 seed、矩阵化输入、批量报告和 golden 摘要格式。
2. `Phase 3` 的 `bossHarness / longRunLab` 关注 battle/world；`Phase 4` 新增的 `LootBalanceLab` 只负责掉落统计与预算偏差，不复写一套运行器。
3. 所有 loot 实验上下文必须输出结构化 `LootRollReport`，供后续 `Phase 5` DeathAnalysis 与 build diversity 分析复用。

#### 4.3.5 与元素系统的接线

`Phase 4` 与元素系统的连接必须引用正式 `ElementInteractionRule`，不允许另写一套散装 if/else：

1. `terrain_lightning_water_chain`
2. `terrain_fire_oil_ignite`
3. `terrain_cold_water_freeze`

接线规则：

1. `CombatPipeline step 9` 负责检查 `TARGET_ON_TERRAIN` 条件。
2. `MapgenPipeline` 只负责生成 `TerrainTag`，不负责执行伤害或状态逻辑。
3. `elite affix`、`artifact proc` 若声明元素交互，必须通过同一 registry 引用这些规则 ID。

### 4.4 精英突变、Boss 变体与隐藏内容

建议文件与模块：

```text
game/src/main/resources/data/elites/*.yaml
game/src/main/resources/data/events/*.yaml
game/src/main/resources/data/secret-zones/*.yaml
```

#### 4.4.1 精英突变 contract

```kotlin
enum class MutationKind {
    STAT_PACKAGE,
    ABILITY_GRANT,
    AURA,
    AI_SHIFT,
    ELEMENT_PACKAGE,
}

data class EliteMutationDef(
    val id: String,
    val kind: MutationKind,
    val nameKey: String,
    val iconKey: String,
    val applyToTags: Set<String>,
    val statModifiers: List<StatModifierRef>,
    val grantedTalents: List<TalentGrantRef>,
    val aiProfileOverlay: String?,
)

data class StatModifierRef(
    val modifierId: String,
)

data class TalentGrantRef(
    val talentId: String,
)
```

首批突变类型固定为：

1. `STAT_PACKAGE`：明确的防御/输出包，不允许“全属性 +50%”这种无解释膨胀。
2. `ABILITY_GRANT`：添加 1 个额外 talent。
3. `AURA`：以可追踪 aura 进入战斗。
4. `AI_SHIFT`：切换到更激进或更保守的 `aiProfileId`。
5. `ELEMENT_PACKAGE`：添加元素抗性/穿透/相关 on-hit 行为。

可读性合同：

1. 精英名必须有 mutation 前缀或后缀。
2. 必须有 `iconKey` 或等价 UI 标识。
3. 进入战斗日志时必须输出 mutation token。
4. inspect 面板必须能列出 mutation 来源和主要能力。
5. `statModifiers` 引用 `game` 内容层中的正式 modifier 定义；`grantedTalents` 必须引用已经注册的 talent id。

#### 4.4.2 Boss 变体与 Phase 3 BossEncounter 的关系

```kotlin
data class BossVariantDef(
    val id: String,
    val baseEncounterId: String,
    val grantedMutations: List<MutationRef>,
    val lootProfileOverride: String?,
    val visualTintKey: String?,
)

data class MutationRef(
    val mutationId: String,
)
```

边界：

1. `BossVariantDef` 只能覆盖 mutation、掉落池、表现 key 和少量候选动作权重，不允许重写 `BossEncounterDef` 的 phase graph 语义。
2. Boss 变体不是 elite mutation 的简单复用；它是建立在 `BossEncounterDef` 之上的受控 overlay。
3. 若需要改变 Boss phase 结构，必须回到 `Phase 3` 的 encounter contract 修改，而不是在 `Phase 4` 用变体偷偷新增第二套 schema。
4. `grantedMutations` 必须引用 `EliteMutationDef` registry 中已有的 mutation id，不允许定义 boss-only 的平行 mutation 体系。

#### 4.4.3 HiddenEvent 与 SecretZone

```kotlin
enum class HiddenTriggerType {
    ENTER_ROOM,
    OPEN_CHEST,
    KILL_ELITE,
    INTERACT_TILE,
    QUEST_STEP,
    PERCEPTION_REVEAL,
}

enum class HiddenConditionKey {
    ZONE_ID,
    TERRAIN_TAG,
    QUEST_FLAG,
    KILL_SOURCE_TAG,
    PLAYER_LEVEL_GTE,
}

enum class HiddenEventRewardKey {
    REVEAL_SECRET_ZONE,
    GRANT_BUFF,
    LOOT_PROFILE,
    TRIGGER_ENCOUNTER,
}

data class HiddenEventCondition(
    val key: HiddenConditionKey,
    val value: String,
)

data class HiddenEventReward(
    val key: HiddenEventRewardKey,
    val value: String,
)

data class HiddenEventDef(
    val id: String,
    val triggerType: HiddenTriggerType,
    val conditions: List<HiddenEventCondition>,
    val rewards: List<HiddenEventReward>,
    val optionalOnly: Boolean = true,
)
```

冻结口径：

1. hidden event 默认只能落在 `OPTIONAL / SECRET` 路径。
2. hidden event 奖励允许是 `loot profile / temporary buff / special encounter / secret entrance reveal`。
3. hidden event 的发现逻辑必须可被 harness 重放，不允许“只有人工白盒能看出来”。
4. `secret zone` 必须明确进入条件、奖励 profile、最低内容量和返回主线的桥接方式。
5. `rewards` 统一使用 typed 结构，不接受未声明前缀的自由字符串。

`hidden event` 最小示例：

```yaml
id: drowned_altar
triggerType: INTERACT_TILE
conditions:
  - key: TERRAIN_TAG
    value: WATER
  - key: ZONE_ID
    value: underground_river
rewards:
  - key: REVEAL_SECRET_ZONE
    value: forgotten_cache
  - key: GRANT_BUFF
    value: shrine.magic_find.small
optionalOnly: true
```

### 4.5 Content Pack Overlay

建议文件与模块：

```text
game/src/main/kotlin/com/ktome/game/contentpack/*
tools/src/main/kotlin/com/ktome/tools/contentpack/*
examples/content-packs/*
```

#### 4.5.1 Manifest 与 overlay 语义

```kotlin
data class PackDependency(
    val id: String,
    val versionRange: String,
)

enum class OverlayOp {
    ADD,
    REPLACE,
    APPEND,
    DENY,
}

data class OverlayEntry(
    val registry: String,
    val targetId: String,
    val op: OverlayOp,
    val sourceFile: String,
)

data class ContentPackManifest(
    val id: String,
    val version: String,
    val schemaVersion: Int,
    val gameVersionRange: String,
    val namespace: String,
    val dependencies: List<PackDependency>,
    val overlays: List<OverlayEntry>,
    val localeBundles: List<String>,
    val visualManifest: String?,
    val audioManifest: String?,
    val harnessSeeds: List<Long>,
)
```

overlay 语义固定为：

| op | 含义 | 允许范围 |
| --- | --- | --- |
| `ADD` | 新增 registry entry | 新怪物、新事件、新 item、新 vault |
| `REPLACE` | 显式替换同 ID 内容定义 | 只能替换 `game` registry 层内容，不得改 `core` 规则常量 |
| `APPEND` | 向列表字段追加内容 | loot pool、zone pool、event pool |
| `DENY` | 显式禁用某个可选内容项 | 只允许作用于内容注册表，不允许删除主线必需 entry |

冲突规则：

1. base game < dependency pack < current pack。
2. 同一 registry entry 若未声明 `REPLACE` 却出现重复 ID，lint 直接失败。
3. pack 不允许直接覆盖官方资源文件路径，只允许通过 manifest overlay 和 namespaced key 显式替换。
4. `Phase 4` 不支持目录自动发现；每个 overlay entry 必须在 manifest 中显式列出，目录扫描留给后续阶段评估。

#### 4.5.2 i18n / visual / audio 集成

pack 目录最小结构：

```text
packs/<pack-id>/
  manifest.yaml
  i18n/
    zh-CN.yaml
    en-US.yaml
  visual/
    visual-manifest.yaml
  audio/
    audio-manifest.yaml
  data/
    monsters/*.yaml
    items/*.yaml
    events/*.yaml
```

校验规则：

1. `nameKey / descKey / logKey` 缺失时为 lint error。
2. `visualKey / iconKey / audioKey` 缺失时：
   - 若 entry 标记为运行时必需，则为 lint error
   - 若 entry 标记为可选表现，则为 warning
3. 缺失 key 不允许静默回落到裸字符串。
4. 所有 pack key 必须带独立 namespace。

#### 4.5.3 版本兼容策略

冻结策略：

1. `ContentPackManifest.schemaVersion` 与 base game `contentSchemaVersion` 分离管理。
2. pack 兼容性的最低要求是：
   - `schemaVersion` 匹配
   - `gameVersionRange` 覆盖当前 base game 版本
3. `0.4.x` 内的小版本升级只要 `schemaVersion` 不变，pack 可继续兼容。
4. 若 schemaVersion 不匹配，loader 必须 fail-fast，而不是尝试部分加载。

#### 4.5.4 schema lint 与 harness

`contentPackHarness` 最低输出必须包含：

1. manifest 解析结果
2. overlay 冲突报告
3. i18n / visual / audio key 解析结果
4. registry 完整性报告
5. 固定 seed headless run 结果

最小 pack 示例：

```yaml
id: sample.flooded_relics
version: 0.4.0
schemaVersion: 1
gameVersionRange: ">=0.4.0 <0.5.0"
namespace: sample_flooded_relics
dependencies: []
localeBundles:
  - i18n/zh-CN.yaml
  - i18n/en-US.yaml
visualManifest: visual/visual-manifest.yaml
audioManifest: audio/audio-manifest.yaml
harnessSeeds: [401, 777, 1407]
overlays:
  - registry: hidden_event
    targetId: sample_flooded_relics.sunken_altar
    op: ADD
    sourceFile: data/events/sample_flooded_relics.sunken_altar.yaml
```

### 4.6 与既有系统的接线边界

1. `Phase 4` 不新增职业；build 差异只能通过 affix、unique、artifact、mutation、hidden reward 和 biome/terrain 差异表达。
2. `TerrainTag` 由 mapgen 持有，元素交互由 `CombatPipeline step 9` 执行，表现层只消费结果。
3. `elite mutation` 和 `artifact proc` 若要接入元素交互，必须引用正式 `ElementInteractionRule`，不允许在 `client` 做视觉特判后反推规则。
4. `element affinity` 只保留为可选 lab，不写入 `Phase 4` 出口门禁；如果未来上主线，必须在 `Phase 5+` 重新单独冻结 schema 和 balance。

## 5. 推荐工作包、检查点与依赖

### 5.1 Phase 4 检查点

| checkpoint | 目标 | 最小交付 |
| --- | --- | --- |
| `P4-A` | ProcGen 深化 | `MapgenPipeline`、`TerrainTag`、`SolvabilityGraph`、mapgen smoke |
| `P4-B` | Loot 与遭遇生态 | `LootBudget`、affix/unique/artifact、elite mutation、hidden content |
| `P4-C` | 数据包扩展点 | `ContentPackManifest`、overlay loader、schema lint、content pack harness |

### 5.2 工作包拆分

| 工作包 | checkpoint | 核心内容 | 模块 | 依赖 |
| --- | --- | --- | --- | --- |
| `P4-W1` | `P4-A` | 混合拓扑地图：room、loop、pattern room、vault、biome family | `core`, `game` | `Phase 3` |
| `P4-W2` | `P4-A` | lock-key DAG、hidden entrance、可解性验证 | `core`, `game`, `tools` | `P4-W1` |
| `P4-W3` | `P4-B` | Loot V2：`iLvl/qLvl/rarity budget`、affix cost、`LootBalanceLab` | `core`, `game`, `tools` | `Phase 3` |
| `P4-W4` | `P4-B` | elite mutation、Boss variant、hidden event、secret zone | `core`, `game`, `client` | `P4-W2`, `P4-W3` |
| `P4-W5` | `P4-C` | content pack overlay、schema lint、headless harness、示例 pack | `game`, `tools`, `client` | `P4-W3`, `P4-W4` |

### 5.3 Golden / Harness 基线策略

`Phase 4` 会直接改变地图与掉落，因此必须明确 golden 更新策略：

1. 所有地图相关 golden seed 在 `P4-W1` 合入后重录。
2. `solvability proof` 的 golden 从 `P4-W2` 开始单独存档，不与地图截图 golden 混用。
3. `LootBalanceLab` 使用独立 batch report，不复用 `CombatTrace` golden。
4. `contentPackHarness` 使用自己的 `pack-manifest + seed-list + report` 三件套作为 reproducibility 边界。

## 6. 测试与自证

### 6.1 必测模块

1. `core.mapgen`
2. `core.world.solvability`
3. `core.loot`
4. `game.contentpack`
5. `tools.solvability`
6. `tools.balance`
7. `tools.contentpack`

### 6.2 必测行为

1. 多 seed 地图可达、可读、无主线死局。
2. 锁钥匙和隐藏入口逻辑能被 harness 发现和证明。
3. 掉落预算与阶段匹配，不出现明显爆表或废条目泛滥。
4. 精英突变、Boss 变体与隐藏事件可以稳定触发和验证。
5. 示例 content pack 可被独立加载并通过 lint/harness。
6. `LIGHTNING + WATER / FIRE + OIL / COLD + WATER / ICE` 在 isolated batch 和 mapgen batch 中都能稳定复现。

### 6.3 自动化命令

```bash
./gradlew test
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew lootBalanceLab
./gradlew hiddenContentHarness
./gradlew contentPackHarness
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 6.4 白盒验证

1. 连续运行多个不同 seed 的新局。
2. 观察：
   - 地图拓扑和秘密区域确有差异
   - 但主线仍可达
3. 在至少两套不同 build 下走完长局关键路径，确认掉落和精英遭遇差异可感知。
4. 装载示例 content pack，确认新增内容可进入主线且不破坏 base game。

说明：完整量化门槛、批量 seed 数和 isolated terrain 场景以 [2026-03-13-phase4-verification-checklist.md](./2026-03-13-phase4-verification-checklist.md) 为权威，本节只列最小验证意图。

## 7. 出口门禁与 Phase 5 输入映射

1. 多 seed 下 mapgen 与 solvability harness 稳定。
   - 这是 `Phase 5` soak、长局压力测试和 AI 稳定性的前置条件。
2. loot 预算和阶段匹配通过实验室验收。
   - 这是 `Phase 5` build diversity、DeathAnalysis 和 release QA 的输入基线。
3. 精英突变、hidden event、secret zone 都可被自动化与白盒验证。
   - 这是 `Phase 5` tactical AI 和 replayability QA 的输入内容。
4. 示例 content pack 可在不改 `core` 的前提下装载和验证。
   - 这是 `Phase 5` localization QA、manifest QA 和 release packaging 的输入边界。

## 8. 风险与止损

1. 如果 ProcGen 差异只来自装饰层，则降级方案是：
   - 先固定 `TopologyGraph + loop injection + vault placement`
   - 暂停新增 tile / biome 细节
2. 如果可解性验证不稳定，则降级方案是：
   - 限制 `KeyType` 到 `KEY_ITEM / SWITCH / BOSS_SIGIL`
   - 暂停 `PERCEPTION_REVEAL` 参与主线桥接
3. 如果 loot 预算出现多套平行词汇，则降级方案是：
   - 先只保留 `NORMAL / MAGIC / RARE / UNIQUE / ARTIFACT`
   - 暂停新的 affix family 扩展
4. 如果 hidden content 难以批量验证，则降级方案是：
   - 保留 `secret zone + hidden event` 二选一
   - 暂停更复杂的 chain event
5. 如果 overlay 开始尝试改规则语义，则立即收回到 `content-only` 边界，并只允许 `ADD / APPEND`。

## 9. 当前状态

1. `Phase 4` 的主线目标、工作包依赖、核心 contract 和验证门槛已经对齐。
2. `MapgenPipeline / SolvabilityGraph / LootBudget / ContentPackManifest` 已作为正式执行词汇冻结。
3. 当前仍未开始实际代码实现，实验室和 harness 仍需建设。
