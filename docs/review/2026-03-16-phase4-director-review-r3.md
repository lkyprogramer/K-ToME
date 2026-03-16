# Phase 4 Director Review — Round 3

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`（995 行）
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md`（179 行）
3. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（§9.3 更新）

**审阅焦点**：R2 修复确认 + 全量深度扫描（YAML/Kotlin 一致性、跨引用完整性、公式变量闭合、验证边界自洽）。

---

## 0. R2 修复确认

### P1 修复状态

| R2 编号 | 问题 | 状态 | 说明 |
| --- | --- | --- | --- |
| P1-R2-1 | `TopologyGraph` 未定义字段 | ✅ 已修复 | §4.1.1 新增 `TopologyNode` / `TopologyEdge` / `TopologyGraph` 完整骨架（含 `pathClass`、`tags`、`isLoop`、`primaryPathNodeIds`） |
| P1-R2-2 | `ZoneMapgenProfile` 未定义 | ✅ 已修复 | §4.1.1 新增完整结构（7 字段），包含 `terrainTagWeights`、`roomTagFilter`、`zoneLootConfig` |
| P1-R2-3 | `zoneRarityBonus` / `zoneQualityBonus` 未定义 | ✅ 已修复 | 新增 `ZoneLootConfig` 数据类；公式改为引用 `zoneLootConfig.rarityBonus` / `zoneLootConfig.qualityBonus`；zone 表补充了每 zone 的数值列 |
| P1-R2-4 | 核心文档与 Phase 4 类型名不一致（5 处） | ✅ 已修复 | 核心文档 §9.3.1~§9.3.3 统一使用 `GeneratedFloor` / `SolvabilityNode` / `SolvabilityEdge` / `LootRollContext` / `OverlayOp`，并增加权威链声明 |
| P1-R2-5 | 稀有度 roll 算法未描述 | ✅ 已修复 | §4.3.1 新增"稀有度选择算法"段（过滤 → 修正 → 归一化 → 单次加权随机 → 降级链） |

### P2 修复状态

| R2 编号 | 状态 | 说明 |
| --- | --- | --- |
| P2-R2-1 | ✅ | `minSize/maxSize` 改为 `widthRange/heightRange` |
| P2-R2-2 | ✅ | `patternId` 补充了指向 `data/mapgen/patterns/{patternId}.yaml` 的说明（冻结口径 6） |
| P2-R2-3 | ✅ | `templateId` 补充了手工 vault 布局模板说明（冻结口径 7） |
| P2-R2-4 | ✅ | `GeneratedFloor` 增加 `zoneId / floorIndex / seed` 字段（冻结口径 5） |
| P2-R2-5 | ✅ | `conditions` 改为 `List<HiddenEventCondition>`，增加 `HiddenConditionKey` 枚举 |
| P2-R2-6 | ✅ | `statModifiers` 改为 `List<StatModifierRef>`，`grantedTalents` 改为 `List<TalentGrantRef>`，补充了引用来源说明（可读性合同 5） |
| P2-R2-7 | ✅ | Checklist §2.3 增加 `BOSS + magicFind=1.00` 和 `BOSS + magicFind=1.50`（验证 clamp） |
| P2-R2-8 | ✅ | 冲突规则 4 显式声明"不支持目录自动发现" |
| P2-R2-9 | ✅ | Checklist §1 条目 4 增加了设计理据段落 |
| P2-R2-10 | ✅ | 冻结定义 6 显式声明 `discoveryRule = null` 的含义 |

**R2 总计 15 项（5 P1 + 10 P2）全部已确认修复。**

---

## 1. R3 新发现

### 新发现统计

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 4 |
| P2 | 13 |

---

### P1-R3-1：`RoomInstance` 在流水线和 `GeneratedFloor` 中被引用但从未定义

**位置**：§4.1.1 流水线步骤 3 + `GeneratedFloor.rooms`

**问题**：流水线步骤 3 是 `TopologyGraph -> RoomInstance`，`GeneratedFloor` 的 `rooms: List<RoomInstance>` 直接引用这个类型。但 `RoomInstance` 从未定义。

`RoomDef` 是房间的**定义模板**（形状、尺寸范围、标签），而 `RoomInstance` 应该是**已实例化的房间**（实际位置、实际尺寸、实际内容）。开发者无法判断 `RoomInstance` 应包含什么字段。

**建议**：补齐 `RoomInstance` 最小骨架，至少包含：

```kotlin
data class RoomInstance(
    val nodeId: String,        // 对应 TopologyNode.id
    val roomDefId: String,     // 对应 RoomDef.id
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val patternId: String?,    // 若应用了 PatternRoomDef
)
```

---

### P1-R3-2：`VaultDef` Kotlin 骨架缺少 YAML 示例中的 `requiredTerrainTags` 字段

**位置**：§4.1.2 `VaultDef` vs §4.1.5 vault YAML

**问题**：vault YAML 示例包含 `requiredTerrainTags: [WATER]` 字段：

```yaml
id: flooded_cache
templateId: vault.flooded_cache
pathClass: OPTIONAL
threatBudget: 14
rewardBudget: 18
allowOnBiomeFamilies: [flooded_cavern]
requiredTerrainTags: [WATER]         # ← YAML 中有
```

但 `VaultDef` Kotlin 类不包含此字段：

```kotlin
data class VaultDef(
    val id: String,
    val templateId: String,
    val pathClass: PathClass,
    val threatBudget: Int,
    val rewardBudget: Int,
    val allowOnBiomeFamilies: Set<String>,
    // requiredTerrainTags 缺失
)
```

这是 YAML 与 Kotlin 之间的直接不一致。文档已确立"Kotlin 骨架是设计锚点"的标准，所以 YAML 出现了骨架上不存在的字段会在实现时产生歧义。

**建议**：在 `VaultDef` 中补充 `val requiredTerrainTags: Set<TerrainTag>`。

---

### P1-R3-3：Hidden entrance YAML 示例与 `HiddenEntranceInstance` 数据类不匹配，缺少 `HiddenEntranceDef`

**位置**：§4.2.3 YAML 示例 vs §4.1.1 `HiddenEntranceInstance`

**问题**：hidden entrance YAML 示例是一个**定义模板**：

```yaml
id: moss_wall_crevice
type: PERCEPTION_CHECK
difficulty: 14
targetSecretZoneId: forgotten_cache
pathClass: SECRET
```

而 `HiddenEntranceInstance` 是一个**已放置的实例**：

```kotlin
data class HiddenEntranceInstance(
    val fromNodeId: String,
    val targetNodeId: String,
    val discoveryRule: DiscoveryRule,
)
```

两者的字段集完全不同，代表不同的抽象层次：

| YAML 字段 | 对应 Kotlin | 存在？ |
| --- | --- | --- |
| `id` | — | ❌ Instance 无 id |
| `type` | `discoveryRule.type` | ⚠️ 嵌套在 DiscoveryRule 中 |
| `difficulty` | `discoveryRule.difficulty` | ⚠️ 同上 |
| `targetSecretZoneId` | — | ❌ Instance 只有 targetNodeId |
| `pathClass` | — | ❌ Instance 无 pathClass |
| — | `fromNodeId` | ❌ YAML 无 |

**建议**：补充 `HiddenEntranceDef`（模板定义），让 YAML 映射到 Def，实例化后变成 Instance：

```kotlin
data class HiddenEntranceDef(
    val id: String,
    val discoveryRule: DiscoveryRule,
    val targetSecretZoneId: String,
    val pathClass: PathClass,
)
```

---

### P1-R3-4：Hidden event YAML 示例的 `conditions` 和 `rewards` 格式与 Kotlin 类型不一致

**位置**：§4.4.3 YAML 示例 vs `HiddenEventCondition` / `HiddenEventDef.rewards`

**问题**：存在两处 YAML/Kotlin 不一致。

**（1）conditions 格式**

Kotlin 类型要求 `key: HiddenConditionKey, value: String`：

```kotlin
data class HiddenEventCondition(
    val key: HiddenConditionKey,  // 枚举：ZONE_ID, TERRAIN_TAG 等
    val value: String,
)
```

但 YAML 示例使用短写 key-value map：

```yaml
conditions:
  - terrainTag: WATER          # 应为 key: TERRAIN_TAG, value: WATER
  - zoneId: underground_river  # 应为 key: ZONE_ID, value: underground_river
```

YAML 的 `terrainTag` 是 camelCase 字符串，Kotlin 枚举是 `TERRAIN_TAG` SCREAMING_SNAKE。更关键的是，YAML 的每个条目是单键 map（`{terrainTag: WATER}`），而 Kotlin 类型是双字段结构体（`key + value`）——这在 `kotlinx.serialization` 下无法自然映射，需要自定义反序列化器。

**（2）rewards 格式**

Kotlin 类型是 `rewards: List<String>`，但 YAML 示例展示的是结构化 key-value：

```yaml
rewards:
  - revealSecretZone: sunken_shrine
  - grantBuff: shrine.magic_find.small
```

这些是 `Map<String, String>` 语义，不是 `List<String>`。如果序列化为字符串，需要约定编码格式（如 `"revealSecretZone:sunken_shrine"`），但文档中没有说明。

**建议**：

1. 将 YAML conditions 改为与 Kotlin 类型一致的规范形式：
   ```yaml
   conditions:
     - key: TERRAIN_TAG
       value: WATER
     - key: ZONE_ID
       value: underground_river
   ```
2. 将 `rewards` 改为 typed 结构，或在文档中显式约定字符串编码格式（如 `"类型:目标"`）。

---

### P2-R3-1：`SolvabilityNode.rewards` 命名在可解性图语境中有歧义

**位置**：§4.2.1

**问题**：`SolvabilityNode.rewards: Set<String>` 在可解性验证算法中的实际用途是"该节点提供的钥匙/能力/旗标"（§4.2.2 步骤 2："收集当前可达节点提供的 key / switch / quest flag"）。但 `rewards` 这个名字暗示"掉落奖励"，会让读者误解其含义。

**建议**：将字段重命名为 `grants` 或 `providedKeys`，更准确地反映其在 DAG 前向可达性分析中的语义。

---

### P2-R3-2：`BossVariantDef.grantedMutations: List<String>` 未类型化

**位置**：§4.4.2

**问题**：R2 修复中 `EliteMutationDef` 已将 `statModifiers` 和 `grantedTalents` 升级为 `List<StatModifierRef>` 和 `List<TalentGrantRef>` 的 typed ref 方式。但 `BossVariantDef.grantedMutations` 仍为 `List<String>`。

冻结口径 4 要求这些 ID 必须引用 `EliteMutationDef` registry，所以引用来源是清晰的，但类型风格不一致。

**建议**：改为 `List<MutationRef>` 并定义包装类型 `data class MutationRef(val mutationId: String)`，保持与 `StatModifierRef` / `TalentGrantRef` 一致的类型化风格。

---

### P2-R3-3：`HiddenEventDef.rewards: List<String>` 缺少引用规范

**位置**：§4.4.3

**问题**：冻结口径 2 说"奖励允许是 loot profile / temporary buff / special encounter / secret entrance reveal"，但 `rewards: List<String>` 没有定义允许的字符串格式或前缀约定。YAML 示例中使用了 `revealSecretZone:` 和 `grantBuff:` 前缀，暗示了一种"类型:目标"编码，但这种编码未被正式定义。

**建议**：要么改为 typed union（如 `sealed interface HiddenEventReward`），要么显式定义允许的前缀枚举（`revealSecretZone` / `grantBuff` / `lootProfile` / `triggerEncounter`）。

---

### P2-R3-4：`SolvabilityGraph` 的构建路径未定义

**位置**：§4.2.1 ~ §4.2.2

**问题**：`TopologyPlanner` 构建 `TopologyGraph`，`MapgenPipeline` 输出 `GeneratedFloor`，`SolvabilityHarness` 验证 `SolvabilityGraph`。但 `SolvabilityGraph` 从何而来？谁负责把 `GeneratedFloor` 转化为 `SolvabilityGraph`？

没有定义类似 `SolvabilityGraphBuilder.build(floor: GeneratedFloor): SolvabilityGraph` 的构建接口。开发者需要自行推断从 `TopologyGraph` + `HiddenEntranceInstance` + key placement 到 `SolvabilityGraph` 的转换逻辑。

**建议**：在 §4.2 中补充一行构建入口说明，如"由 `SolvabilityGraphBuilder` 从 `GeneratedFloor` 的拓扑和隐藏入口数据构建"。

---

### P2-R3-5：`MapGenerator` 与 `MapgenPipeline` 接口职责重叠

**位置**：§4.1.1

**问题**：两个接口签名几乎相同：

```kotlin
interface MapGenerator {
    fun generate(request: MapgenRequest): GeneratedFloor
}

interface MapgenPipeline {
    fun run(request: MapgenRequest): GeneratedFloor
}
```

两者都接受 `MapgenRequest` 并返回 `GeneratedFloor`。如果 `MapgenPipeline` 是公开的编排接口，`MapGenerator` 的存在意义是什么？

**建议**：删除 `MapGenerator`，只保留 `MapgenPipeline` 作为唯一公开入口；或明确说明两者的层级关系（如 `MapGenerator` 是低层实现接口，`MapgenPipeline` 是高层编排 + 校验入口）。

---

### P2-R3-6：iLvl 公式中 `variance(-1..1)` 的类型和分布未明确

**位置**：§4.3.1 公式 1

**问题**：`iLvl = clamp(sourceLevel + sourceTier.itemLevelBonus + variance(-1..1), 1, playerLevel + 3)`

`variance(-1..1)` 的含义不明确：
- 是均匀分布的随机**整数** `{-1, 0, 1}`？
- 还是均匀分布的随机**浮点数** `[-1.0, 1.0]` 然后取整？

考虑到 `iLvl` 是整数且 variance 范围仅 3 个值，合理推断为随机整数。但应显式标注。

**建议**：改为 `uniformInt(-1, 1)` 或直接说明"从 `{-1, 0, +1}` 均匀取整数"。

---

### P2-R3-7：`affixBudget` 公式对 NORMAL / UNIQUE / ARTIFACT 产出非零值但不被消费

**位置**：§4.3.1 公式 5 + §4.3.2

**问题**：公式 5 `affixBudget = qLvl * 2 + rarityTier.baseBudget + sourceTier.affixBudgetBonus` 对所有 rarity tier 都产出值。但 §4.3.2 明确：

- `NORMAL`：无 affix（隐含 budget 无意义）
- `UNIQUE / ARTIFACT`：固定模板，不做二次随机 affix 拼装（budget 同样无意义）

只有 `MAGIC` 和 `RARE` 实际消费 `affixBudget`。这不是 bug，但未加说明会让实现者困惑"为什么 NORMAL 物品也有非零的 affixBudget"。

**建议**：在公式后补充一句："仅 `MAGIC` 和 `RARE` 消费 `affixBudget`；`NORMAL` 忽略此值，`UNIQUE / ARTIFACT` 使用固定模板不走 budget 扣减。"

---

### P2-R3-8：Checklist §2.1 环路边比例约束 `0.15 ~ 0.35` 与允许 0 条环路矛盾

**位置**：Checklist §2.1 条目 3 vs §4.1.3 规则 2

**问题**：§4.1.3 允许环路数量为 `0 ~ 2`（即可以没有环路），但 Checklist §2.1 要求"环路边 / 总连通边比例在 `0.15 ~ 0.35`"。当环路数 = 0 时，比例 = 0，不满足 `0.15 ~ 0.35`。

**建议**：将检查条件改为"若 `optionalLoopCount > 0`，则环路边 / 总连通边比例在 `0.15 ~ 0.35`"，或将下界改为 `0.00 ~ 0.35`。

---

### P2-R3-9：`SecretZoneDef` 缺少 YAML 示例

**位置**：§4.2.3

**问题**：Phase 4 的每个主要数据类型都有 YAML 示例（vault、biome family、affix、hidden event、content pack manifest），但 `SecretZoneDef` 只有 Kotlin 骨架和冻结口径，没有 YAML 示例。考虑到 secret zone 是隐藏内容体系的核心，缺少示例会让 content owner 无法直观判断数据格式。

**建议**：补充一个 `SecretZoneDef` 的最小 YAML 示例。

---

### P2-R3-10：Zone profile 缺少 YAML 示例

**位置**：§4.1.1 / §4.1.4

**问题**：`ZoneMapgenProfile` 是 mapgen 的核心配置入口（7 个字段），§4.1.4 有人类可读的 zone 表，但没有对应的 YAML 示例展示 zone profile 的数据文件长什么样。vault、biome family、affix 都有 YAML 示例，zone profile 反而没有。

**建议**：补充一个 `ZoneMapgenProfile` 的最小 YAML 示例（如 `underground_river.yaml`），帮助 content owner 理解 zone 配置的完整结构。

---

### P2-R3-11：`PatternRoomDef.spawnWeight` 语义未解释

**位置**：§4.1.2

**问题**：`PatternRoomDef.spawnWeight: Int` 的含义未说明。是在同一 room 内多个 pattern 之间的竞争权重？还是相对于不使用 pattern 的普通房间的额外权重？数值越大是越常见还是越稀有？

**建议**：补充一句说明，如"在同一 `baseRoomId` 下多个可选 pattern 之间的相对权重，数值越大出现概率越高"。

---

### P2-R3-12：`VaultDef.threatBudget` / `rewardBudget` 单位和值域未说明

**位置**：§4.1.2

**问题**：vault YAML 示例中 `threatBudget: 14, rewardBudget: 18`，但这些数字的含义和量纲不明确。`threatBudget` 是怪物数量？是战斗 CR？与 affix cost 的 budget 是同一货币体系吗？`rewardBudget` 是物品数量？还是 loot roll 次数？

**建议**：补充一两句说明单位和典型值域（如"`threatBudget` 以怪物 CR 累加为单位，`14` 约等于 `3` 只 NORMAL + `1` 只 ELITE"）。

---

### P2-R3-13：Checklist §1 条目 4 设计理据重复

**位置**：Checklist §1 条目 4

**问题**：当前内容存在信息重复：

> - `30%` 保证平均每 `3~4` 局至少出现一次显式隐藏发现
> - `10%` 保持 secret zone 的稀缺感，但不会在长期游玩中完全不可见
> - 上述阈值的设计意图是：平均每 `3~4` 局至少出现一次隐藏发现，同时保持 secret zone 稀缺但长期可见

第三句完整复述了前两句的含义。

**建议**：删除第三句总结段，保留分条理据即可。

---

## 2. 跨文档一致性检查

### 2.1 核心文档 §9.3 与 Phase 4 文档

R2 的 5 处类型名不一致已全部统一。核心文档现在使用与 Phase 4 完全一致的类型名，并显式声明了权威链（§9.3.1 第 3~4 条）。

| 检查点 | 状态 |
| --- | --- |
| `GeneratedFloor` 统一 | ✅ |
| `SolvabilityNode` / `SolvabilityEdge` typed | ✅ |
| `LootRollContext` 统一 | ✅ |
| `OverlayOp` 统一 | ✅ |
| 权威链声明 | ✅ "以 phase4 执行文档为权威" |

核心文档的简化骨架（`TopologyNode` 少了 `pathClass`/`tags`，`SolvabilityEdge` 少了 `requiredKeys`/`discoveryRule` 等）属于设计意图上的精简，已被权威链声明覆盖，不再视为问题。

### 2.2 Phase 4 内部 YAML ↔ Kotlin 一致性

本轮重点检查了所有 YAML 示例与对应 Kotlin 骨架的字段匹配：

| 数据类型 | YAML ↔ Kotlin | 状态 |
| --- | --- | --- |
| vault | `requiredTerrainTags` YAML 有 / Kotlin 无 | ❌ **P1-R3-2** |
| biome family | 完全匹配 | ✅ |
| affix | 完全匹配 | ✅ |
| hidden entrance | YAML 是 Def / Kotlin 是 Instance，结构完全不同 | ❌ **P1-R3-3** |
| hidden event conditions | YAML 短写 map / Kotlin typed 结构体 | ❌ **P1-R3-4** |
| hidden event rewards | YAML 结构化 / Kotlin 扁平 `List<String>` | ⚠️ **P2-R3-3** |
| content pack manifest | 完全匹配 | ✅ |

### 2.3 与 Phase 3/5 文档的衔接

| 检查点 | 状态 |
| --- | --- |
| Phase 4 入口引用 Phase 3 出口 | ✅ |
| Phase 4 出口映射到 Phase 5 输入 | ✅ §7 |
| Phase 4 不重复 Phase 3 已冻结系统 | ✅ |
| Phase 4 不提前做 Phase 5 工作 | ✅ §3.2 |

---

## 3. R3 总结

Phase 4 文档体系经过 R1 → R2 → R3 三轮修订，质量稳步提升：

- **R1**：4 P0 + 14 P1 + 12 P2 = 30 项 → 文档从"设计意向书"升级为"执行文档"
- **R2**：0 P0 + 5 P1 + 10 P2 = 15 项 → 补齐了中间类型、公式变量和选择算法
- **R3**：0 P0 + 4 P1 + 13 P2 = 17 项 → 问题下沉到 YAML/Kotlin 一致性和边缘定义精度

R3 的 4 个 P1 问题核心主题是 **YAML 示例与 Kotlin 骨架之间的结构不匹配**：

1. `RoomInstance` 缺失 → 流水线产物没有数据结构
2. `VaultDef` 少字段 → YAML 比 Kotlin 多一个过滤字段
3. `HiddenEntranceDef` 缺失 → YAML 是定义模板，Kotlin 是放置实例，缺少桥接
4. Hidden event YAML 格式 → conditions 和 rewards 的序列化形式与 Kotlin 类型不匹配

P2 问题集中在命名精度（`rewards` → `grants`）、边缘约束自洽（环路比例 vs 零环路）和缺失的 YAML 示例（SecretZoneDef、ZoneMapgenProfile）。

**放行建议**：R3 无 P0 问题。P1-R3-1 ~ P1-R3-4（YAML/Kotlin 不匹配）应在 P4-W1 起步前修复——这些不一致会在 YAML loader 实现时立即暴露为编译错误或反序列化失败。P2 问题不阻塞实现起步，可在 W1/W2 过程中渐进处理。

---

*审阅人：资深游戏设计与开发总监视角*
*审阅版本：Round 3*
*下一步：待 P1 修复后进行 R4 确认审阅*
