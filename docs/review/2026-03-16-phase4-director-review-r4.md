# Phase 4 Director Review — Round 4

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`（1075 行）
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md`（178 行）
3. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（§9.3）

**审阅焦点**：R3 修复确认 + 实现前最后一轮全量扫描（重点关注跨引用闭合、数据流完整性、示例间一致性）。

---

## 0. R3 修复确认

### P1 修复状态

| R3 编号 | 问题 | 状态 | 说明 |
| --- | --- | --- | --- |
| P1-R3-1 | `RoomInstance` 未定义 | ✅ 已修复 | §4.1.1 新增 `RoomInstance`（7 字段：nodeId / roomDefId / x / y / width / height / patternId?） |
| P1-R3-2 | `VaultDef` 缺 `requiredTerrainTags` | ✅ 已修复 | §4.1.2 `VaultDef` 已含 `val requiredTerrainTags: Set<TerrainTag>`，YAML 匹配 |
| P1-R3-3 | 缺 `HiddenEntranceDef` 桥接类型 | ✅ 已修复 | §4.2.3 新增 `HiddenEntranceDef`（id / discoveryRule / targetSecretZoneId / pathClass），YAML 示例使用嵌套 `discoveryRule:` 块，与 Kotlin 结构一致 |
| P1-R3-4 | Hidden event conditions / rewards YAML 与 Kotlin 不一致 | ✅ 已修复 | conditions 改为 `key: TERRAIN_TAG, value: WATER` 格式；rewards 改为 typed `HiddenEventReward`（新增 `HiddenEventRewardKey` 枚举），冻结口径 5 明确禁止自由字符串 |

### P2 修复状态

| R3 编号 | 状态 | 说明 |
| --- | --- | --- |
| P2-R3-1 `grants` 重命名 | ✅ | `SolvabilityNode.rewards` → `grants` |
| P2-R3-2 `MutationRef` | ✅ | `BossVariantDef.grantedMutations: List<MutationRef>` |
| P2-R3-3 rewards typed | ✅ | `HiddenEventDef.rewards: List<HiddenEventReward>` + `HiddenEventRewardKey` 枚举 |
| P2-R3-4 builder 接口 | ✅ | `SolvabilityGraphBuilder.build(floor)` + §4.2.2 构建路径段落 |
| P2-R3-5 接口去重 | ✅ | `MapGenerator` 已删除，仅保留 `MapgenPipeline` |
| P2-R3-6 uniformInt | ✅ | 公式 1 改为 `uniformInt(-1, 1)` |
| P2-R3-7 affixBudget 说明 | ✅ | 解释 5 明确"仅 MAGIC / RARE 消费" |
| P2-R3-8 环路比例条件 | ✅ | Checklist §2.1 改为"若 `optionalLoopCount > 0`" |
| P2-R3-9 SecretZoneDef YAML | ✅ | §4.2.3 新增 `forgotten_cache` 最小示例 |
| P2-R3-10 ZoneMapgenProfile YAML | ✅ | §4.1.5 新增 `underground_river` 最小示例 |
| P2-R3-11 spawnWeight 语义 | ✅ | 冻结口径 8 |
| P2-R3-12 budget 单位 | ✅ | 冻结口径 9 |
| P2-R3-13 理据重复 | ✅ | 重复总结句已删除 |

**R3 总计 17 项（4 P1 + 13 P2）全部已确认修复。**

---

## 1. R4 新发现

### 新发现统计

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 0 |
| P2 | 9 |

---

### P2-R4-1：§4.1.3 环路比例约束未同步 Checklist 的条件化修正

**位置**：§4.1.3 规则 4 vs Checklist §2.1

**问题**：Checklist §2.1 已正确修正为"若 `optionalLoopCount > 0`，则环路边 / 总连通边比例在 `0.15 ~ 0.35`"。但 §4.1.3 规则 4 仍写"环路边数量占总连通边数量的比例控制在 `0.15 ~ 0.35`"，没有加 `optionalLoopCount > 0` 条件。

Checklist 是验证权威，但 §4.1.3 是约束源头。两处描述同一规则但不一致。

**建议**：将 §4.1.3 规则 4 同步改为"若 `optionalLoopCount > 0`，则环路边数量占总连通边数量的比例控制在 `0.15 ~ 0.35`"。

---

### P2-R4-2：`HiddenEntranceDef.discoveryRule` 与 `SecretZoneDef.entryRule` 存在冗余

**位置**：§4.2.3

**问题**：`HiddenEntranceDef` 和 `SecretZoneDef` 各自持有一份 `DiscoveryRule`：

```kotlin
data class HiddenEntranceDef(
    val discoveryRule: DiscoveryRule,  // 入口发现规则
    val targetSecretZoneId: String,
)

data class SecretZoneDef(
    val entryRule: DiscoveryRule,      // zone 进入规则
)
```

YAML 示例中两者的值完全一致（`PERCEPTION_CHECK, difficulty: 14`）。当前模型下，每个 secret zone 只有一个入口（`targetSecretZoneId` 是单值），所以 `discoveryRule` 和 `entryRule` 实际上描述同一件事。

风险：如果内容编辑者更新了入口的 difficulty 但忘了同步 zone 的 entryRule，两处会悄悄发散。Solvability harness 可能使用其中一个，而 content indexing 使用另一个。

**建议**：两种处理方式（选一即可）：

1. 删除 `SecretZoneDef.entryRule`，入口规则只由 `HiddenEntranceDef.discoveryRule` 持有，zone 不再重复。
2. 保留两者但补充一句约定："Phase 4 每个 secret zone 有且只有一个入口，`SecretZoneDef.entryRule` 必须与对应 `HiddenEntranceDef.discoveryRule` 一致；harness 应校验两者匹配。"

---

### P2-R4-3：`P4-W5` 依赖表缺少 `P4-W4`

**位置**：§5.2 工作包拆分表

**问题**：工作包表中 `P4-W5` 依赖只列了 `P4-W3`，但 content pack harness 的"固定 seed headless run"（Checklist §1 条目 5）需要 hidden event / mutation / secret zone 系统正常运行——这些来自 `P4-W4`。

具体地，示例 pack manifest（§4.5.4 第 955 行）使用 `registry: hidden_event, targetId: drowned_altar, op: ADD`。`hidden_event` registry 是 `P4-W4` 交付的。如果 W5 在 W4 之前启动 headless harness，加载会失败。

W5 的 manifest / loader / lint 部分确实只依赖 W3 的 schema 稳定。但 harness 验证阶段隐式依赖 W4。

**建议**：将 `P4-W5` 的依赖改为 `P4-W3`（loader/lint 起步）+ `P4-W4`（harness headless run），或在 W5 中拆出两个子交付：`W5a: loader + lint`（依赖 W3）和 `W5b: harness`（依赖 W4）。

---

### P2-R4-4：`ZoneMapgenProfile.terrainTagWeights` 与 `BiomeFamilyDef.terrainTagWeights` 的合并规则未说明

**位置**：§4.1.1 + §4.1.2

**问题**：两个数据类都包含 `terrainTagWeights: Map<TerrainTag, Float>`：

- `ZoneMapgenProfile`（line 161）：zone 级权重（underground_river: WATER 0.24, ICE 0.08）
- `BiomeFamilyDef`（line 274）：biome 级权重（flooded_cavern: WATER 0.22, ICE 0.05）

YAML 示例中两套数值不同（zone 的 WATER 0.24 vs biome 的 WATER 0.22），说明它们不是简单复制。但两者如何合并（或谁优先）在 §4.1 中没有说明：

- zone weight 覆盖 biome weight？
- zone weight 乘以 biome weight？
- 取两者中的较大值？
- zone weight 仅在 biome 未覆盖的 tag 上生效？

流水线步骤 5 说"RoomInstance + BiomeFamily → TerrainTag paint"——只提到 BiomeFamily，没提 ZoneMapgenProfile。

**建议**：补充一句合并规则，如"mapgen paint 以 `BiomeFamilyDef.terrainTagWeights` 为基础，`ZoneMapgenProfile.terrainTagWeights` 作为 zone 级覆盖——若 zone 明确设置某 tag 的权重，则覆盖 biome 默认值"。

---

### P2-R4-5：Key-gate placement 数据未在 mapgen 输出中显式建模

**位置**：§4.1.1 / §4.2.1 / §4.2.2

**问题**：`SolvabilityEdge.requiredKeys: Set<String>` 描述了通过某条边所需的钥匙。`SolvabilityNode.grants: Set<String>` 描述了节点提供的钥匙。但在 mapgen 侧：

- `TopologyEdge` 只有 `from / to / isLoop`——没有 gate / lock 信息。
- `TopologyNode.tags: Set<String>` 是通用标签集——没有显式的 key placement 语义。

`SolvabilityGraphBuilder.build(floor: GeneratedFloor)` 以 `GeneratedFloor` 为唯一输入。但 `GeneratedFloor` 中不包含"哪个房间放了哪把钥匙"或"哪条边需要哪把钥匙"的数据。开发者在实现 W2 时需要以下二选一：

1. 在 `TopologyEdge` 上增加 `requiredKeys` 字段（修改 W1 骨架）。
2. 通过 `TopologyNode.tags` 编码 key 信息（如 `"grants:gold_key"`），但这需要约定格式。

当前文档没有说明采用哪种路径。由于 W2 在 P4-A freeze 之前完成，骨架修改技术上可行，但提前在文档中标注会减少 W1→W2 的接口返工。

**建议**：在 §4.2.2 构建路径段落中增加一句，如"key-gate placement 数据在 `P4-W2` 中落地时可能需要扩展 `TopologyEdge`（增加 `requiredKeys`）或约定 `TopologyNode.tags` 的 key 编码格式，具体方案在 W2 实现时确定"。

---

### P2-R4-6：`SecretZoneDef.guaranteedContent` 的字符串格式约定未文档化

**位置**：§4.2.3

**问题**：`SecretZoneDef.guaranteedContent: List<String>` 的 YAML 示例使用了隐式的 `registry.id` 格式：

```yaml
guaranteedContent:
  - vault.flooded_cache
  - hidden_event.drowned_altar
```

`vault.flooded_cache` 中的 `vault` 是 registry name，`flooded_cache` 是 entry ID。但这种"registry 前缀 + 点号分隔"的编码约定没有被冻结口径或任何正式说明定义。开发者无法确定：

- 前缀是否对应 overlay 中的 `registry` 字段名（如 `hidden_event` / `vault`）？
- 是否允许嵌套（如 `zone.secret.deep_cave`）？
- content pack 中的 namespaced 条目如何表达？

**建议**：补充一句格式约定，如"`guaranteedContent` 使用 `{registryName}.{entryId}` 格式引用内容条目"。

---

### P2-R4-7：Content pack 示例中的 `drowned_altar` 与主文档示例 ID 重复

**位置**：§4.5.4 vs §4.4.3

**问题**：§4.4.3 的 hidden event 最小示例定义了 `id: drowned_altar`。§4.5.4 的示例 pack 使用 `op: ADD, targetId: drowned_altar`。

如果 `drowned_altar` 是 base game 的内容（由 §4.4.3 定义），那么 pack 对同 ID 执行 `ADD` 会违反冲突规则 2（"同一 registry entry 若未声明 `REPLACE` 却出现重复 ID，lint 直接失败"）。

如果 `drowned_altar` 只是文档中的示例占位符、不是真实 base game 内容，那文档读者仍可能误解。

**建议**：将 pack 示例的 event ID 加上 namespace 前缀，如 `targetId: sample_flooded_relics.sunken_altar`，与 base game 示例区分开。

---

### P2-R4-8：`VaultPlacement.roomId` 与 `nodeId` 语义重叠

**位置**：§4.1.1

**问题**：

```kotlin
data class VaultPlacement(
    val vaultId: String,
    val nodeId: String,
    val roomId: String,
)
```

`RoomInstance` 的唯一标识是 `nodeId`（每个 TopologyNode 对应一个 RoomInstance）。`VaultPlacement.nodeId` 和 `VaultPlacement.roomId` 都指向同一个房间——但 `RoomInstance` 没有独立 `id` 字段，只有 `nodeId` 和 `roomDefId`。

那 `VaultPlacement.roomId` 实际指向什么？如果是 `RoomInstance.nodeId`，则与 `VaultPlacement.nodeId` 重复。如果是 `RoomInstance.roomDefId`（模板 ID），命名为 `roomId` 会产生歧义。

**建议**：删除其中一个字段（如果 node 和 room 是 1:1 关系，只保留 `nodeId`），或将 `roomId` 重命名为 `roomDefId` 以明确其含义。

---

### P2-R4-9：YAML 示例中 `sunken_shrine` 被 hidden event reward 引用但无对应定义

**位置**：§4.4.3 + §4.2.3

**问题**：hidden event YAML 示例中：

```yaml
rewards:
  - key: REVEAL_SECRET_ZONE
    value: sunken_shrine
```

但 §4.2.3 定义的 secret zone 示例是 `id: forgotten_cache`，不是 `sunken_shrine`。`sunken_shrine` 在整个文档中没有对应的 `SecretZoneDef` 定义。

从叙事角度理解：`forgotten_cache` 是通过物理入口 `moss_wall_crevice` 进入的，在 `forgotten_cache` 内触发 `drowned_altar` 事件后揭示更深处的 `sunken_shrine`——这是一条合理的发现链。但 `sunken_shrine` 作为孤立引用，在文档中无法自证。

**建议**：要么补充 `sunken_shrine` 的 SecretZoneDef 示例（作为 chain discovery 的演示），要么将 reward value 改为已定义的 `forgotten_cache`（简化示例集）。

---

## 2. 跨文档一致性检查

### 2.1 核心文档 §9.3

| 检查点 | 状态 |
| --- | --- |
| 类型名统一（GeneratedFloor / LootRollContext / OverlayOp 等） | ✅ |
| 权威链声明（"以 phase4 执行文档为权威"） | ✅ 合并为一句 |
| 简化骨架不引入与 Phase 4 冲突的新字段 | ✅ |
| Phase 4 新增类型（RoomInstance / HiddenEntranceDef / HiddenEventReward / MutationRef / SolvabilityGraphBuilder）核心文档无需同步 | ✅ 权威链已覆盖 |

### 2.2 Phase 4 内部 YAML ↔ Kotlin 一致性

| 数据类型 | YAML ↔ Kotlin | 状态 |
| --- | --- | --- |
| MapgenRequest | — | ✅ 无 YAML |
| ZoneMapgenProfile | 7 字段完全匹配 | ✅ |
| TopologyNode / Edge / Graph | — | ✅ 无 YAML |
| RoomInstance | — | ✅ 无 YAML |
| RoomDef / PatternRoomDef | — | ✅ 无 YAML |
| VaultDef | 含 requiredTerrainTags | ✅ |
| BiomeFamilyDef | 完全匹配 | ✅ |
| HiddenEntranceDef | 嵌套 discoveryRule 块匹配 | ✅ |
| SecretZoneDef | 完全匹配 | ✅ |
| LootRollContext | — | ✅ 无 YAML |
| RarityTier / SourceTier | — | ✅ 无 YAML |
| LootBudget | — | ✅ 无 YAML |
| AffixCost | 完全匹配 | ✅ |
| EliteMutationDef | — | ✅ 无 YAML |
| BossVariantDef | — | ✅ 无 YAML |
| HiddenEventDef | conditions key/value 匹配，rewards key/value 匹配 | ✅ |
| ContentPackManifest | 完全匹配 | ✅ |
| OverlayEntry | 完全匹配 | ✅ |

**YAML ↔ Kotlin 一致性：全部通过。R3 的 4 个 P1 结构不匹配问题已完全消除。**

### 2.3 公式变量闭合检查

| 公式 | 变量 | 来源 | 状态 |
| --- | --- | --- | --- |
| 1 `iLvl` | `sourceLevel` | `LootRollContext` | ✅ |
| 1 | `sourceTier.itemLevelBonus` | `SourceTier` enum | ✅ |
| 1 | `uniformInt(-1, 1)` | 显式标注 | ✅ |
| 1 | `playerLevel` | `LootRollContext` | ✅ |
| 2 `effectiveMagicFind` | `magicFindBonus` | `LootRollContext` | ✅ |
| 3 `rarityScore` | `sourceTier.rarityBonus` | `SourceTier` enum | ✅ |
| 3 | `zoneLootConfig.rarityBonus` | `ZoneLootConfig` via zone | ✅ |
| 3 | `effectiveMagicFind` | 公式 2 | ✅ |
| 4 `qLvl` | `rarityTier.qualityBonus` | `RarityTier` enum | ✅ |
| 4 | `zoneLootConfig.qualityBonus` | `ZoneLootConfig` via zone | ✅ |
| 5 `affixBudget` | `rarityTier.baseBudget` | `RarityTier` enum | ✅ |
| 5 | `sourceTier.affixBudgetBonus` | `SourceTier` enum | ✅ |

**公式变量闭合：全部通过。每个变量都有明确的数据来源。**

---

## 3. R4 总结

Phase 4 文档体系经过四轮审阅，质量演进如下：

| 轮次 | P0 | P1 | P2 | 总计 | 核心主题 |
| --- | --- | --- | --- | --- | --- |
| R1 | 4 | 14 | 12 | 30 | 文档从设计意向升级为执行文档 |
| R2 | 0 | 5 | 10 | 15 | 补齐中间类型、公式变量、选择算法 |
| R3 | 0 | 4 | 13 | 17 | YAML/Kotlin 结构不匹配 |
| R4 | 0 | 0 | 9 | 9 | 跨引用闭合、冗余消除、示例一致性 |

**R4 结论**：

1. **无 P0、无 P1**。文档已达到可驱动实现的质量标准。
2. R4 的 9 个 P2 问题分为三类：
   - **内部一致性**（P2-R4-1 环路约束同步、P2-R4-2 discoveryRule 冗余、P2-R4-8 roomId 重叠）—— 实现时会自然收敛，但提前修正可以减少接口返工。
   - **示例间交叉引用**（P2-R4-7 ID 冲突、P2-R4-9 sunken_shrine 孤立）—— 纯编辑问题，不影响 contract。
   - **实现路径提示**（P2-R4-3 W5 依赖、P2-R4-4 terrainTag 合并、P2-R4-5 key-gate 数据流、P2-R4-6 guaranteedContent 格式）—— 开发者在 W1/W2 起步时就会触碰的设计决策点，提前标注可降低返工概率。
3. **核心合同（Kotlin 骨架、数学公式、验证门槛）已完全闭合**，YAML ↔ Kotlin 一致性全部通过，公式变量全部有源。

**放行建议**：Phase 4 文档体系已就绪，可以进入 P4-W1 / P4-W3 并行实现。9 个 P2 问题可在实现过程中渐进处理——其中 P2-R4-1（环路约束同步）和 P2-R4-3（W5 依赖补充）建议在 W1 起步前快速修正，其余可在各自工作包实现过程中自然收口。

---

*审阅人：资深游戏设计与开发总监视角*
*审阅版本：Round 4*
*审阅状态：建议放行实现*
