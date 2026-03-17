> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
> `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-06 Long Run World Structure

**阶段**: `Phase 3 / P3-W6`  
**优先级**: `P0`  
**前置条件**: `P3-W4` 完成 + `P3-W5` schema 冻结  
**对应问题**: Phase 2 只有 4 zone 短局，没有世界分支、任务推进、经济循环、affix 驱动和长局回归实验室。没有 `WorldProgress / Quest / Gate` 合同时，4~6 小时 run 只会退化为“地图变长”，不会成为稳定长局。

**Lane-parallel 拆分**：

- **W6a (Content Lane)**: 世界分支与 zone 入口（`7 mandatory + 4 optional zones`）、zone 等级范围、连接拓扑、可选 zone 独有内容
- **W6b (Rules Lane)**: `WorldProgress / Quest / Gate / RouteReward` + affix v1 + 经济循环（`shard` 唯一货币、固定 2 商店节点、rescue policy）
- **W6c (Tools/QA Lane)**: `longRunLab`（smoke + full 模式）+ 量化门槛 + `RunSummary`

---

## 1. 阶段目标

把 Phase 2 的 4 zone 短局扩展为带世界推进合同的完整长局结构，建立经济循环、affix v1、run summary 与长局回归实验室。

完成标准：

1. 长局世界分支冻结：`7 mandatory + 4 optional zones`。
2. `WorldProgress / Quest / Gate / RouteReward` 正式进入运行时合同。
3. Zone 等级范围冻结（`Lv1-15`）。
4. Boss 名册冻结（`molten_giant / dungeon_lord / abyssal_guardian`）。
5. 经济模型冻结（`shard` 唯一货币、固定 2 商店节点）。
6. Affix v1 最低预算冻结（共 `40` 种词缀），并补 build 相关性与 rescue policy。
7. `RunSummary` 扩展到足以支撑复现、实验室统计与玩家 run history。
8. `longRunLab` 建立（smoke + full 两种模式）。
9. 量化门槛在基础职业矩阵外，还要补 2 个可玩进阶职业的 smoke 验证。

## 2. 当前问题

1. Phase 2 只有 4 zone 短局，没有世界分支和主支线推进结构。
2. 没有 `WorldProgress / Quest / GateCondition / RouteReward`，world graph 只能表达连接关系，无法表达主线推进。
3. 没有经济循环（货币 / 商店），build 多样性受限。
4. 没有 affix 系统，掉落无法驱动构筑差异。
5. 商店、铭文、位移、净化、护盾等 rescue 工具没有保底策略。
6. 第二个商店位置仍存在“`deep_iron_pit` 或 `underground_river`”漂移。
7. `RunSummary` 过瘦，不能支撑 `longRunLab` 分析与玩家 run history。
8. `headlessTurnEquivalent <= 3000` 没有换算规则，门禁不可稳定复现。
9. `longRunLab` 只对 4 基础职业给量化门槛，尚未把 2 个可玩进阶职业纳入 smoke。

### 2.1 本 PR 必须冻结的口径

1. Phase 3 仍是纯 run-based Roguelike，无数值 meta progression。
2. 局间持久化只有：正式进阶职业解锁 + profile / 历史 run summary。
3. 通关定义：击败深渊之心并完成结算页面。
4. run 内装备 / affix / 铭文 / 货币不跨 run 继承。
5. `WorldProgress / Quest / Gate / RouteReward` 是本 PR 的正式合同，不允许继续作为“隐含规则”存在。
6. 第二个商店节点固定放在 `deep_iron_pit` 路线。
7. affix v1 不只冻结数量，还必须冻结：
   - build 相关性
   - rescue 工具保底
   - blacklist 约束
8. `headlessTurnEquivalent` 必须给出确定义。
9. `bossHarness` 在 `W6` 升级为覆盖 3 Boss，与 `PR-04` 的 2 Boss 工具基线不冲突。
10. `SaveDataV2` 或等价存档合同必须能无损 round-trip `WorldProgress`，不允许只保留 `QuestSnapshot`。
11. `RouteReward` 必须冻结一次性领取语义，不允许把“首次解锁发还是首次通关发”留给实现者猜。
12. `RescueInventoryPolicy` 除了保证库存存在，还必须冻结 checkpoint 级 affordability 合同。

## 3. 范围与非目标

### 3.1 范围

1. [W6a] 世界分支拓扑（7 mandatory + 4 optional zones + 连接关系）。
2. [W6a] 11 个 zone 的完整规格（等级范围 / floorCount / mapSize / 主题 / 机制）。
3. [W6a] 可选 zone 最小内容约束。
4. [W6a] Boss 名册与 `bossHarness` 扩展。
5. [W6b] `WorldProgress / Quest / Gate / RouteReward`。
6. [W6b] 经济模型（`shard + 2 shops + rescue inventory policy`）。
7. [W6b] Affix v1（武器前缀 12 / 后缀 10、防具前缀 10 / 后缀 8）+ 相关性/blacklist。
8. [W6c] `RunSummary` 与 `longRunLab`（smoke + full 模式）。
9. [W6c] 量化门槛与 headless 指标。

### 3.2 非目标

1. 不在本 PR 进入深度 ProcGen（Phase 4 的混合地图生成器）。
2. 不在本 PR 做打造 / 附魔 / 合成（Phase 4）。
3. 不在本 PR 追求最终生态复杂度的 affix 词条。
4. 不在本 PR 要求可选 zone 都有独立叙事任务。
5. 不在本 PR 把可选 zone 继续扩张为超过当前 11-zone baseline 的规模。

## 4. 技术方案

### 4.1 [W6a] 世界分支拓扑

建议文件：

```text
core/src/main/kotlin/com/ktome/core/world/WorldGraph.kt
core/src/main/kotlin/com/ktome/core/world/ZoneConnection.kt
game/src/main/resources/data/world/world_graph.yaml
core/src/test/kotlin/com/ktome/core/world/WorldGraphTest.kt
```

冻结口径：

连接关系固定为：

```text
shattered_outpost
  -> greenwood_fringe
    -> elven_ruins (optional)
    -> bandit_camp (optional)
    -> deep_iron_pit
      -> molten_core (optional)
      -> grey_gate_depths (contains grey_gate_throne boss room)
        -> underground_river
          -> crystal_cavern (optional)
          -> abyssal_temple
            -> abyssal_heart
```

说明：

1. `grey_gate_throne` 在 Phase 3 继续作为 `grey_gate_depths` 内部的区域 Boss 房存在，不单独作为 world graph 的独立 zone 节点。
2. Phase 3 当前执行口径采用 11-zone baseline；若后续排期压缩，只允许减少 optional zone 的内容量，不允许改动 `WorldProgress / Gate / RouteReward` 的核心合同。
3. world graph 默认为**双向可通行图**；若某条连接是单向或一次性门，必须在 `ZoneConnection` 上显式声明，而不能靠 ASCII 拓扑图自行脑补。

### 4.2 [W6b] `WorldProgress / Quest / Gate / RouteReward`

建议文件：

```text
core/src/main/kotlin/com/ktome/core/world/WorldProgressDef.kt
core/src/main/kotlin/com/ktome/core/world/QuestDef.kt
core/src/main/kotlin/com/ktome/core/world/GateCondition.kt
core/src/main/kotlin/com/ktome/core/world/RouteReward.kt
core/src/test/kotlin/com/ktome/core/world/WorldProgressTest.kt
```

冻结口径：

1. `WorldProgressDef` 负责表达 run 内的主线推进状态，而不是只靠暗规则。
2. `QuestDef` 至少表达：
   - `questId`
   - `objectiveStates`
   - `completionFlags`
3. `GateCondition` 至少表达：
   - `requiredQuestId`
   - `requiredWorldFlag`
   - `requiredBossKill`
4. `RouteReward` 至少表达：
   - 固定奖励
   - 货币奖励
   - affix / 铭文 / 资源补给奖励
   - 一次性领取语义

建议最小结构：

```kotlin
data class WorldProgressDef(
    val questStates: Map<String, ObjectiveState>,
    val worldFlags: Set<String>,
    val unlockedRoutes: Set<String>,
    val defeatedBossIds: Set<String>,
    val claimedRouteRewards: Set<String>,
)

enum class RewardClaimPolicy {
    ON_ROUTE_UNLOCK,
    ON_FIRST_ROUTE_CLEAR,
}

data class RouteReward(
    val routeId: String,
    val claimPolicy: RewardClaimPolicy = RewardClaimPolicy.ON_FIRST_ROUTE_CLEAR,
    val levelBandRef: String,
    val shardReward: Int,
    val guaranteedDropIds: List<String>,
    val rescueTags: Set<String>,
)

data class GateCondition(
    val requiredQuestId: String? = null,
    val requiredWorldFlag: String? = null,
    val requiredBossKill: String? = null,
)
```

### 4.2.1 [W6b] `SaveDataV2` 与 `WorldProgressSnapshot` 对齐

建议文件：

```text
core/src/main/kotlin/com/ktome/core/save/WorldProgressSnapshot.kt
core/src/test/kotlin/com/ktome/core/save/SaveLoadWorldProgressTest.kt
```

冻结口径：

1. 长局存档必须持久化 `worldProgress: WorldProgressSnapshot` 或等价结构，不允许只保留 `QuestSnapshot`。
2. `WorldProgressSnapshot` 至少无损表达：
   - `questStates`
   - `worldFlags`
   - `unlockedRoutes`
   - `defeatedBossIds`
   - `claimedRouteRewards`
3. save/load round-trip 后，gate 判定、路线奖励领取状态和 Boss 击杀状态不得丢失或半丢失。
4. `RouteReward.shardReward` 必须与 `levelBandRef` 或等价 zone level band 绑定，不能脱离 checkpoint 等级带做裸固定值。

### 4.3 [W6a] Zone 规格表

建议文件：

```text
core/src/main/kotlin/com/ktome/core/world/ZoneDef.kt
core/src/main/kotlin/com/ktome/core/monster/MonsterTemplateV2.kt
game/src/main/resources/data/zones/*.yaml
core/src/test/kotlin/com/ktome/core/world/ZoneReachabilityTest.kt
```

Zone 等级范围：

| Zone ID | 中文名 | 等级范围 | 类型 |
| --- | --- | --- | --- |
| `shattered_outpost` | 破碎前哨 | `Lv1-4` | mandatory |
| `greenwood_fringe` | 绿林边缘 | `Lv3-6` | mandatory |
| `bandit_camp` | 盗贼营地 | `Lv3-5` | optional |
| `elven_ruins` | 精灵遗迹 | `Lv5-7` | optional |
| `deep_iron_pit` | 深铁矿坑 | `Lv5-8` | mandatory |
| `molten_core` | 熔岩核心 | `Lv7-9` | optional |
| `grey_gate_depths` | 灰门深窟 | `Lv7-10` | mandatory |
| `underground_river` | 地下河 | `Lv10-12` | mandatory |
| `crystal_cavern` | 水晶洞穴 | `Lv11-13` | optional |
| `abyssal_temple` | 深渊神殿 | `Lv12-15` | mandatory |
| `abyssal_heart` | 深渊之心 | `Lv15` | mandatory |

### 4.4 [W6a] 可选 zone 最小内容约束

冻结口径：

1. 每个可选 zone 至少包含 `1` 个独有精英怪、独有奖励节点或独有事件，不能只是主线路径换皮。
2. 可选 zone 不得承载主线 Boss 或主线解锁道具。
3. 掉落预算和固定奖励必须与等级范围对齐。
4. Phase 3 不要求独立叙事任务，但必须提供至少 `1` 个明确的探索收益点。
5. zone 怪物池必须统一引用 `MonsterTemplateV2` schema，不允许在 `W6` 再临时造一套 zone 私有怪物结构。

### 4.5 [W6a] Boss 名册与 `bossHarness`

冻结口径：

| Boss ID | Zone | 类型 | 来源 |
| --- | --- | --- | --- |
| `molten_giant` | `deep_iron_pit` | 中间 Boss | 继承 Phase 2 |
| `dungeon_lord` | `grey_gate_depths` | 区域 Boss | 继承 Phase 2 |
| `abyssal_guardian` | `abyssal_heart` | 最终 Boss | Phase 3 新增 |

收口说明：

1. `PR-04` 的工具基线是“至少 2 个 Boss”。
2. `PR-06` 的 roster-complete 门禁升级为“3 Boss 全部通过 `bossHarness`”。

### 4.6 [W6b] 经济模型与固定商店节点

建议文件：

```text
core/src/main/kotlin/com/ktome/core/economy/ShardEconomy.kt
core/src/main/kotlin/com/ktome/core/economy/ShopNode.kt
core/src/main/kotlin/com/ktome/core/economy/ShopInventory.kt
core/src/main/kotlin/com/ktome/core/economy/RescueInventoryPolicy.kt
core/src/main/kotlin/com/ktome/core/economy/AffordableRescueSlotPolicy.kt
game/src/main/resources/data/shops/*.yaml
core/src/test/kotlin/com/ktome/core/economy/ShardEconomyTest.kt
core/src/test/kotlin/com/ktome/core/economy/ShopNodeTest.kt
```

冻结口径：

1. Phase 3 只做一种局内货币：`shard`。
2. `shard` 来源：怪物掉落 / 支线奖励 / Boss 奖励 / 出售装备。
3. Phase 3 只做固定商店节点，不做打造 / 附魔 / 合成。
4. 商店位置固定为：
   - `greenwood_fringe` 入口后 `1` 个补给商人
   - `deep_iron_pit` 路线 `1` 个中段商店
5. 商店行为：购买装备 / 购买消耗品和铭文 / 出售多余掉落。

`RescueInventoryPolicy` 最小合同：

1. 第一商店必须保底至少 1 个回复类或护盾类工具。
2. 第二商店必须保底至少 1 个位移类和 1 个净化类工具。
3. 若当前 build 缺少基础保命工具，商店库存必须有一条补救路径。
4. `AffordableRescueSlotPolicy` 必须为每个 checkpoint 冻结：
   - `expectedShardBudgetByCheckpoint`
   - `mandatoryAffordableItemCount`
   - `requiredAffordableTags`
5. 第一商店至少 `1` 个救火工具必须在 checkpoint 期望 shard 预算内可负担。
6. 第二商店至少 `2` 个救火工具必须在 checkpoint 期望 shard 预算内可负担，且覆盖：
   - `MOVEMENT`
   - `CLEANSING` 或 `PROTECTION`

### 4.7 [W6b] Affix v1、相关性与 blacklist

建议文件：

```text
core/src/main/kotlin/com/ktome/core/item/AffixDef.kt
core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt
core/src/main/kotlin/com/ktome/core/item/AffixPool.kt
core/src/main/kotlin/com/ktome/core/item/AffixTagWeighting.kt
core/src/main/kotlin/com/ktome/core/item/AffixBlacklist.kt
game/src/main/resources/data/affixes/*.yaml
core/src/test/kotlin/com/ktome/core/item/AffixGeneratorTest.kt
```

冻结口径：

| 词缀类型 | 数量 | 示例 |
| --- | --- | --- |
| 武器前缀 | 12 | 锋利的(+攻击)、燃烧的(+火伤)、冰霜的(+冰伤)、雷击的(+雷伤) |
| 武器后缀 | 10 | 力量之(+STR)、速度之(+攻速)、吸血之(命中回血)、穿甲之(+护甲穿透) |
| 防具前缀 | 10 | 坚固的(+护甲)、抗火的(+火抗)、镇定的(+Mental Save) |
| 防具后缀 | 8 | 生命之(+HP)、再生之(+HP 回复)、抗性之(+全元素抗) |

补充合同：

1. `AffixTagWeighting` 用于把 affix 与职业、伤害通道、资源轴建立相关性。
2. `AffixBlacklist` 用于避免明显的无效或误导组合。
3. Affix v1 的目标不只是“40 条词缀进入掉落池”，而是让掉落能驱动构筑差异且不把 run 随机判死刑。

建议最小结构：

```kotlin
data class AffixDef(
    val id: String,
    val nameKey: String,
    val slotType: AffixSlotType,
    val equipType: EquipType,
    val tier: Int,
    val statModifiers: List<StatModifier>,
    val tags: Set<String>,
    val blacklistTags: Set<String>,
)
```

### 4.8 [W6c] 局间合同与 `RunSummary`

建议文件：

```text
core/src/main/kotlin/com/ktome/core/profile/RunSummary.kt
core/src/test/kotlin/com/ktome/core/profile/RunSummaryTest.kt
```

冻结口径：

1. Phase 3 仍是纯 run-based Roguelike：角色死亡后 run 重置。
2. 持久化到局间的只有：正式进阶职业解锁 / 已发现 profile / 历史 `RunSummary`。
3. `RunSummary` 从本 PR 起扩展为支撑实验室复现和玩家 run history 的结构化对象。

建议最小结构：

```kotlin
data class RunSummary(
    val seed: Long,
    val finishedAtEpochMillis: Long,
    val classId: String,
    val raceId: String,
    val finalZoneId: String,
    val turnCount: Int,
    val headlessTurnEquivalent: Int,
    val zoneRouteHash: String,
    val buildHash: String,
    val rulesetVersion: String,
    val victory: Boolean,
    val defeatReason: String? = null,
)
```

4. `classId / raceId` 无论 run 胜负都必须填充；输赢由 `victory` 单独表达。

### 4.9 [W6c] `headlessTurnEquivalent`

冻结口径：

1. `headlessTurnEquivalent` 定义为：
   - 每完成 1 次 actor 的完整行动结算记 1
   - 纯视觉帧记 0
   - 多段伤害、连锁触发、child trace 若属于同一 actor 行动，不额外加回合
   - 独立世界 tick 若触发下一次调度点，记 1
2. `BossPhase` 切换、副作用和 telegraph 预告若包含在当前行动结算中，不额外增加等价回合数。
3. `headlessTurnEquivalent <= 3000` 的门槛以该定义为准。

### 4.10 [W6c] `longRunLab`

建议文件：

```text
tools/src/main/kotlin/com/ktome/tools/lab/LongRunLab.kt
tools/src/main/kotlin/com/ktome/tools/lab/LongRunConfig.kt
tools/src/main/kotlin/com/ktome/tools/lab/LongRunReport.kt
core/src/test/kotlin/com/ktome/core/world/LongRunLabSmokeTest.kt
```

冻结口径：

1. **smoke 模式**：
   - 固定职业 / 种族 / profile / world seed
   - 验证 run 能到达终局状态，不要求通关
2. **full 模式**：
   - 扩展到代表性职业 / 种族矩阵
   - 统计死亡分布、平均时长、不可达主线
3. 两种模式共同检查项：
   - 无卡死 / 无不可达主线
   - 主支线和可选支线都可达
   - `headlessTurnEquivalent <= 3000`
4. full 模式额外检查项：
   - `4` 基础职业 × `3` 种族的 `12` 个组合中，至少 `8` 个能到达 `abyssal_temple`
   - `50%` 以上失败 run 发生在 `deep_iron_pit` 之后
5. 额外 smoke：
   - `Berserker` 至少 1 组 smoke
   - `Spellblade` 至少 1 组 smoke

## 5. 推荐改动面

### 5.1 `core`

1. `world` 包扩展（`WorldGraph / ZoneDef / WorldProgressDef / QuestDef / GateCondition / RouteReward`）。
2. `economy` 包扩展（`ShardEconomy / ShopNode / ShopInventory / RescueInventoryPolicy`）。
3. `item` 包扩展（`AffixDef / AffixGenerator / AffixPool / AffixTagWeighting / AffixBlacklist`）。
4. `profile` 包扩展（`RunSummary`）。

### 5.2 `game`

1. `zones/*.yaml` 扩展（从 4 个扩展到 11 个）。
2. `world/*.yaml` 新建（`world_graph`、quest/gate 数据）。
3. `affixes/*.yaml` 新建。
4. `shops/*.yaml` 新建。

### 5.3 `client`

1. 世界地图 UI（zone 选择 / 分支显示）。
2. 商店 UI。
3. 通关结算页面。

### 5.4 `tools`

1. `LongRunLab` 新建（smoke + full 模式）。
2. `longRunLab` Gradle task。

## 6. 测试与自证

### 6.1 必测类

1. `WorldGraphTest`
2. `WorldProgressTest`
3. `ZoneReachabilityTest`
4. `AffixGeneratorTest`
5. `ShardEconomyTest`
6. `ShopNodeTest`
7. `AffordableRescueInventoryPolicyTest`
8. `SaveLoadWorldProgressTest`
9. `RunSummaryTest`
10. `LongRunLabSmokeTest`
11. `LongRunLabFullTest`
12. `ZoneContentCoverageTest`

### 6.2 必测行为

1. `WorldProgress / Quest / Gate / RouteReward` 可以表达主线推进。
2. save/load round-trip 后 `worldFlags / unlockedRoutes / defeatedBossIds / claimedRouteRewards` 保持一致。
3. `RouteReward` 的 `claimPolicy` 行为稳定，不会重复领取或提前发放。
4. `WorldGraph` 的连接方向与回退路径语义稳定，单向连接只能来自显式声明。
5. zone 怪物池统一引用 `MonsterTemplateV2`，不存在 zone 私有怪物 schema。
6. smoke 模式：固定职业 / 种族 / seed，验证 run 能到达终局状态。
7. full 模式：12 组合矩阵，至少 8 个到达 `abyssal_temple`。
8. `50%` 以上失败 run 发生在 `deep_iron_pit` 之后。
9. 无卡死、无不可达主线。
10. 主支线和可选支线都可达。
11. `headlessTurnEquivalent <= 3000`，且换算方式稳定。
12. 经济循环可运行（`shard + 2 shops`）。
13. `RescueInventoryPolicy` 能提供位移/净化/护盾等保底工具，且 checkpoint 预算下至少存在规定数量的可负担救火物品。
14. Affix v1 的 `40` 种词缀全部进入掉落池，且通过 `AffixTagWeighting / AffixBlacklist` 避免明显无效组合。
15. `Berserker / Spellblade` 各有至少 1 组 smoke 成功到达终局状态。
16. 3 Boss 全部通过 `bossHarness`。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.world.*"
./gradlew :core:test --tests "com.ktome.core.economy.*"
./gradlew :core:test --tests "com.ktome.core.item.Affix*"
./gradlew :core:test --tests "com.ktome.core.profile.RunSummary*"
./gradlew bossHarness
./gradlew longRunLab
./gradlew test
```

补充要求：

1. `longRunLab` 至少支持 smoke 与 full 两种模式。
2. `longRunLab` 必须输出统计报告（到达率、死亡分布、平均回合数、zone route hash 分布）。

### 6.4 白盒验证

1. 用 `Vanguard / Arcanist / Rogue / Templar` 各完成一次长局。
2. `Berserker / Spellblade` 各完成至少一次 smoke 白盒验证。
3. 至少一次走完含可选 zone 的非最短路线。
4. 到达商店并完成购买 / 出售操作。
5. 确认 affix 出现在掉落装备上并影响角色属性。
6. 完成通关后确认结算页面和 `RunSummary` 写入。

## 7. 出口门禁

1. `7` mandatory + `4` optional zones 全部可达。
2. `WorldProgress / Quest / Gate / RouteReward` 进入正式合同，且 save/load round-trip 无损保留。
3. `3` Boss 全部通过 `bossHarness`。
4. `longRunLab` smoke 模式可到达终局状态。
5. `longRunLab` full 模式满足量化门槛（`>= 8/12` 到达 `abyssal_temple`，`> 50%` 失败在 `deep_iron_pit` 后）。
6. `Berserker / Spellblade` 各至少有 1 组 smoke 到达终局状态。
7. Affix v1 共 `40` 种词缀全部进入掉落池，并通过相关性/blacklist 约束。
8. 经济循环（`shard + 2 shops + rescue inventory policy + affordability`）可运行。
9. `RunSummary` 足以支撑复现、实验室统计与玩家 run history。
10. 4~6 小时 run 可稳定收敛到死亡或通关。

## 8. 风险与止损

1. 如果长局时长失控，优先压缩 optional zone 的内容量，不先破坏 `WorldProgress / Gate` 合同。
2. 如果某些 zone 的怪物池内容不足，优先在已有模板基础上做变种，不从零新建大量模板。
3. 如果 affix v1 导致某些 build 过度膨胀，优先限制权重和 blacklist，不删除词缀系统。
4. 如果 `longRunLab` full 模式的量化门槛无法达标，优先调整怪物 / 掉落 / 商店 / rescue policy 数值，不放宽门槛。
