# Phase 4 深度审查报告 — Part 3：关键问题 + 优化建议

---

## 5. 当前 Phase 4 最需要解决的关键问题

### 问题 #1：精英突变数量严重不足（Critical）

**问题描述**：  
设计文档承诺 12 套精英变体能力，实际只交付 6 套（stonehide / battle_drill / dread_aura / hunt_protocol / emberblood / tidebound）。且 tier 分布为 1 MINOR + 4 MAJOR + 1 SIGNATURE，层次不均。

**证据**：  
- 设计承诺：`docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` §P4-W4 明确 "12套变体能力"
- 实际交付：`data/elites/index.yaml` 仅 6 个 `eliteMutations` 条目
- MutationKind 覆盖：5 种 kind 各仅 1~2 个实例，每种 kind 下无选择空间

**为什么必须在 Phase 4 解决**：  
- 精英遭遇是重复游玩差异的核心来源之一。6 个 mutation 在 `maxMutationsPerElite: 2` 规则下，去除互斥关系后有效组合约 12~15 种，4 个升级 zone 各 2 层 floor，约 3~4 次 run 即可穷尽所有精英变体组合
- Phase 5 的战术 AI 层（Utility 评分 + 感知状态机）将建立在精英行为多样性之上。如果突变池太浅，AI 升级只是让少数几种行为"更聪明"，而不是让遭遇"更多变"
- Content Pack 扩展也依赖基础突变池。如果 base game 只有 6 种，pack 作者很难在"补基础缺口"和"做创新内容"之间选择

**不解决的后果**：  
- 精英遭遇在第 3 次 run 后失去新鲜感
- Phase 5 AI 升级的收益被极大压缩
- Content Pack 生态被迫承担"补全基础内容"的角色

---

### 问题 #2：掉落体验的"预算公式正确，终端感知空洞"（Critical）

**问题描述**：  
iLvl/qLvl/rarity budget 数学模型完整且经过 lootBalanceLab 验证，但实际掉落体验被两个因素严重稀释：
1. Affix 池仅 ~40 个（22 prefix + 18 suffix），全部是数值增强型
2. Loot profile 使用固定 itemIds 列表（4~6 个 base item），不是预算驱动的随机池

**证据**：  
- `data/items/index.yaml`：affixes 段约 40 个词缀，全部结构为 `stats: {attack: N, defense: M, ...}` + optional `passive: {kind: X, amount: Y}`，无"机制改变"型词缀
- `data/loot/index.yaml`：每个 profile 的 `itemIds` 是静态列表。例如 `loot.greenwood_fringe.cadence.itemIds = [hunter_bow, emerald_charm, leather_armor, apprentice_robe]`
- UNIQUE 的 `fixedAffixIds` 只是普通 affix 组合（如 `[sanctified, of_precision]`），无独特效果

**为什么必须在 Phase 4 解决**：  
- 掉落是 Roguelike 核心循环的终端奖励。如果掉落无法驱动构筑变化，整个"探索→战斗→成长"链条的回报端是空的
- Phase 5 不会增加新的掉落机制（Phase 5 关注 AI + 稳定性 + 发布）。如果掉落问题留到 Phase 5 后修复，等于整个 beta 测试期玩家都在体验"无聊的掉落"
- Affix 池是所有装备系统的基础。池太浅意味着所有基于 affix 的后续扩展（如 set bonus、synergy affix、conditional affix）都缺乏地基

**不解决的后果**：  
- 核心循环的奖励驱动力坍塌
- "不同 seed 产生不同体验"的 Phase 4 核心承诺无法兑现
- Content Pack 即使新增物品，也受限于浅 affix 池的组合空间

---

### 问题 #3：隐藏内容发现机制严重同质化（High）

**问题描述**：  
设计定义了 6 种触发类型（ENTER_ROOM / OPEN_CHEST / KILL_ELITE / INTERACT_TILE / QUEST_STEP / PERCEPTION_REVEAL），但实际只使用了 2 种（PERCEPTION_REVEAL + INTERACT_TILE），且 4 个 secret zone 全部使用 PERCEPTION_CHECK 作为 entryRule。

**证据**：  
- `data/events/index.yaml`：8 个事件中 4 个为 PERCEPTION_REVEAL，4 个为 INTERACT_TILE，其余 4 种类型零使用
- `data/secret-zones/index.yaml`：4 个 zone 的 entryRule 全部是 `type: PERCEPTION_CHECK`
- 所有 `entranceBindingId` 均为 `optional.branch.1`

**为什么必须在 Phase 4 解决**：  
- 隐藏内容是 Phase 4 新增的核心体验层。如果发现方式完全同质化，这个系统的存在价值被严重削弱
- "探索惊喜"的核心在于"不可预测"。当玩家学会"每次走到 optional branch 就搜索"后，惊喜为零
- Phase 5 不会扩展隐藏内容触发机制。Phase 4 是唯一的窗口

**不解决的后果**：  
- 隐藏内容系统成为"例行公事"而非"探索惊喜"
- SearchAction 的战术决策价值（是否值得花 1000 能量搜索）被取消——答案总是"到了 optional.branch.1 就搜"
- 系统架构支持 6 种触发但只用 2 种，浪费了已建好的基础设施

---

### 问题 #4：地形交互"技术实现完成但体验不存在"（High）

**问题描述**：  
5 种地形交互规则已编码到 CombatPipeline Step 9，但由于地形覆盖率低（~8%）、分布位置不在关键战斗区域、缺乏主动利用地形的玩家手段，地形交互在实际游戏中几乎不触发。

**证据**：  
- Biome terrainTagWeights 最高为 WATER:0.28（underground_river），大部分 zone 在 0.06~0.24
- TerrainTagPainter 将地形分散到房间边缘而非中心战斗区域
- 玩家没有"推拉敌人"的技能，无法主动制造地形交互条件
- 精英突变不与地形绑定（tidebound 精英不保证出现在有 WATER 的房间）

**为什么必须在 Phase 4 解决**：  
- 地形交互是 Phase 4 的核心设计承诺之一（PR-06 的三大交付之一）
- 如果留到 Phase 5，Phase 5 的优先级是 AI/稳定性/发布，没有空间做地形战术化
- 当前状态是"投入了开发成本但产出为零的体验价值"，是明确的负 ROI

**不解决的后果**：  
- Phase 4 的"地形交互"承诺变成伪完成项
- 5 种交互规则的代码成为无用的死代码
- 后续 content 扩展无法围绕地形交互做战术设计（因为基础体验不成立）

---

### 问题 #5：装备不驱动构筑选择（High）

**问题描述**：  
当前所有 affix 都是纯数值增强型（+attack, +defense, +stat, +resistance, +regen），没有"机制改变"型词缀。UNIQUE/ARTIFACT 也只是固定 affix 组合，无独特效果。装备替换决策退化为"数值更高就换"。

**证据**：  
- `data/items/index.yaml` 所有 affix 的 `stats` 字段都是 `{attack: N, defense: M, ...}` 结构
- `passive` 字段仅出现在少数 affix 中，且全部是 `HpRegenPerTurn` 类型
- UNIQUE 的 `fixedAffixIds` 如 `[sanctified, of_precision]` 等同于"带锁词缀的蓝装"

**为什么必须在 Phase 4 解决**：  
- 装备驱动构筑是 ARPG/Roguelike 的核心吸引力之一。没有它，"掉落→构筑→再战"的正循环断裂
- Phase 5 不增加装备机制。如果 Phase 4 不引入机制词缀，整个游戏的装备系统将永远停留在"数值装备"层面
- 当前 affix 框架（AffixCostModels）完全支持更复杂的词缀——问题不在架构，而在内容

**不解决的后果**：  
- 装备层的构筑空间为零
- "这局我想试不同 build"的掉落驱动力不存在
- UNIQUE/ARTIFACT 的设计价值大幅贬值（它们只是"高级数值装备"）

---

### 问题 #6：Boss 战缺乏专属战术要求（Medium）

**问题描述**：  
3 个 Boss 变体仅覆盖 actionWeightProfile 和 grantedMutations，不引入阶段切换、特殊机制、战场变化。Boss 战在体验上是"更强的精英"，而非"需要特殊策略的终极挑战"。

**证据**：  
- `data/boss-variants/index.yaml`：3 个变体的结构完全相同——`baseEncounterId` + `grantedMutations` + `actionWeightProfileId`
- 设计文档（PR-06）明确约束"不重写 phase graph"，这意味着 Boss 变体不能改变 Boss 的阶段逻辑
- 对比：design doc 中 BossEncounterDef 和 BossPhaseDef 支持多阶段，但变体没有利用阶段差异

**为什么必须在 Phase 4 解决**：  
- Boss 战是每个 zone 的体验高点。如果高点不够高，整个 zone 的体验曲线就是平的
- Phase 5 会升级 Boss AI（战术评分），但不改变 Boss 战的内容设计。如果战斗内容本身太浅，更好的 AI 只是让浅内容执行得更"聪明"

**不解决的后果**：  
- Zone 体验缺乏终局高点
- Boss 变体成为"数值差异"而非"战术差异"

---

## 6. 优化建议（按优先级）

### P0：必须立刻处理

#### P0-1：补全精英突变至 12 套

**问题本质**：精英遭遇差异在 2~3 zone 后穷尽  
**影响范围**：所有升级 zone 的精英遭遇  
**优化目标**：将有效突变从 6 个提升到 12 个，且 tier 分布合理

**具体改法**：

在 `data/elites/index.yaml` 中新增以下 6 个突变（建议方向，需平衡调优）：

| 新增 ID | Kind | Tier | 设计意图 |
|---------|------|------|---------|
| `elite.phase_runner` | AI_SHIFT | MINOR | 精英具有"脱战后传送到玩家背后"行为，增加位置博弈 |
| `elite.corrosive_touch` | ELEMENT_PACKAGE | MINOR | 物理攻击附带 SHADOW 减防效果，让输出窗口更有价值 |
| `elite.unstoppable` | STAT_PACKAGE | MAJOR | 免疫 STUN/SLOW 但移动速度降低 30%，迫使玩家换控制策略 |
| `elite.regenerator` | STAT_PACKAGE | MAJOR | 每回合恢复 3% HP，必须集火或使用减疗效果 |
| `elite.spell_mirror` | AURA | SIGNATURE | 反射 30% 法术伤害光环（2 格范围），迫使法系角色调整打法 |
| `elite.frenzy_caller` | ABILITY_GRANT | MINOR | 精英低于 50% HP 时获得 `blood_rush`，创造"斩杀窗口"决策 |

**tier 分布调整后**：3 MINOR + 5 MAJOR + 3 SIGNATURE + 1 已有 MINOR = 更均衡的梯度

**改动优先级**：**P0**  
**预期收益**：精英遭遇有效组合从 ~15 种提升到 ~60 种，差异感显著增加  
**可能副作用**：新突变可能需要平衡调优，建议配合 bossHarness + 手动 playtest  
**需同步修改**：
- `data/elites/index.yaml`：新增 6 条 mutation + 对应 statModifiers
- i18n bundle：新增 nameKey / descKey
- `data/monsters/index.yaml`：确认 applyToTags 覆盖足够多的精英
- bossHarness 验证

---

#### P0-2：扩充 Affix 池并引入"机制词缀"

**问题本质**：affix 全部是数值增强，装备不驱动构筑  
**影响范围**：整个装备系统 + 掉落体验 + 构筑深度  
**优化目标**：affix 总量从 ~40 提升到 80+，其中至少 15~20 个为"机制词缀"

**具体改法**：

1. **新增机制型 Prefix（建议 8~10 个）**：

| Affix ID | Tier | Cost | 效果 | 设计意图 |
|----------|------|------|------|---------|
| `vampiric_strike` | MEDIUM | 6 | 攻击回复 5% 伤害为 HP | 持续战斗流派 |
| `thunderchain` | MAJOR | 10 | 攻击 20% 几率对相邻敌人造成 LIGHTNING 伤害 | AOE 辅助 |
| `frozen_edge` | MEDIUM | 6 | 攻击 15% 几率施加 SLOW (2 回合) | 控制流派 |
| `mana_siphon` | MAJOR | 10 | 击杀回复 8 MANA/ENERGY | 资源续航流派 |
| `berserking` | SIGNATURE | 14 | HP<50% 时攻击 +30% | 高风险高回报流派 |
| `spellweave` | MEDIUM | 6 | 施法后下次普攻额外 +40% 伤害 | 法战混合流派 |
| `guardian_mark` | MEDIUM | 6 | 被攻击时 10% 几率获得 GUARD_STANCE 1 回合 | 被动防御流派 |
| `flame_conduit` | MAJOR | 10 | FIRE 技能造成额外地面燃烧效果 2 回合 | 地形联动（与 OIL 地形联动） |

2. **新增机制型 Suffix（建议 6~8 个）**：

| Affix ID | Tier | Cost | 效果 | 设计意图 |
|----------|------|------|------|---------|
| `of_retribution` | MAJOR | 10 | 被击时反射 15% 物理伤害 | 坦克构筑 |
| `of_swiftcast` | MEDIUM | 6 | 施法速度 +15%（受 DR 限制） | 法系 DPS |
| `of_ambush` | MAJOR | 10 | 从隐身攻击伤害 +40% | Rogue 构筑深化 |
| `of_endurance` | MEDIUM | 6 | 每 3 回合自动清除 1 个 debuff | 状态对抗 |
| `of_opportunity` | SIGNATURE | 14 | 暴击时重置一个随机技能冷却 | 暴击构筑 |
| `of_elementalist` | MAJOR | 10 | 元素伤害 +15%，物理伤害 -10% | 元素专精 trade-off |

3. **扩充通用数值 affix（新增 ~15 个）**：确保每个 tier 有 5~6 个选择

**改动优先级**：**P0**  
**预期收益**：
- affix 组合空间从 ~40^2 ≈ 800 种提升到 ~80^2 ≈ 3200 种
- 首次出现"这件装备改变了我的 build"的体验
- UNIQUE/ARTIFACT 可以使用机制词缀，获得真正的"独特感"

**可能副作用**：
- 机制词缀需要与天赋系统交互，可能需要扩展 combat pipeline 的 effect resolution
- 平衡需要更多 lootBalanceLab 测试
- on-hit / on-kill 型效果可能增加 combat trace 复杂度

**需同步修改**：
- `data/items/index.yaml`：新增 affix 条目
- `core/loot/AffixCostModels.kt`：如果新增 cost tier 或 passive kind
- i18n bundle：新增所有 affix 的 nameKey / descKey
- UNIQUE/ARTIFACT 的 fixedAffixIds 更新为使用新机制词缀
- lootBalanceLab 扩展测试矩阵

---

#### P0-3：重构 Loot Profile 为预算驱动的动态池

**问题本质**：loot profile 的 itemIds 是静态列表，违背"预算驱动差异化"的设计目标  
**影响范围**：所有掉落来源  
**优化目标**：掉落从"固定列表选一件"变为"预算约束下从 zone 物品池随机生成"

**具体改法**：

当前结构：
```yaml
- id: loot.greenwood_fringe.cadence
  rewardBudget: 4
  itemIds: [hunter_bow, emerald_charm, leather_armor, apprentice_robe]
```

改为预算池结构：
```yaml
- id: loot.greenwood_fringe.cadence
  rewardBudget: 4
  poolStrategy: BUDGET_WEIGHTED
  baseItemPool:
    weapons: [short_sword, long_sword, hunter_bow, battle_axe]
    armor: [leather_armor, chain_mail, apprentice_robe]
    accessories: [emerald_charm, bandit_trophy]
    consumables: [healing_potion, stamina_draught]
  itemWeights:
    weapons: 0.35
    armor: 0.30
    accessories: 0.20
    consumables: 0.15
  affixStrategy: BUDGET_FILL  # 用 affixBudget 剩余预算填充 affix
  zoneExclusiveItems: [unique.greenwood_watcher_blade, unique.briarbound_bow]
```

核心变化：
- `itemIds` 改为 `baseItemPool`（按类型分池 + 权重）
- 新增 `poolStrategy: BUDGET_WEIGHTED`，让 `LootBudgetResolver` 根据 rewardBudget 决定掉落数量和品质
- `affixStrategy: BUDGET_FILL` 让 affix 预算自动分配
- `zoneExclusiveItems` 保留 zone 特色但走 eligibility 逻辑

**改动优先级**：**P0**  
**预期收益**：
- 不同 run 的同一 zone 会掉落不同物品 + 不同 affix 组合
- rewardBudget 的数学框架真正发挥作用
- 为后续 Content Pack 新增物品提供正确的扩展点

**可能副作用**：
- 需要修改 `ItemGenerator` 的物品选择逻辑
- Loot profile 格式变化需要迁移现有所有 profile
- 可能需要调整 lootBalanceLab 的验证逻辑

**需同步修改**：
- `data/loot/index.yaml`：所有 21 个 profile 重构
- `core/loot/LootBudgetResolver.kt` 或 `game/factory/ItemFactory.kt`：实现 pool 选择逻辑
- `tools/loot/LootBalanceLabRunner.kt`：适配新 profile 格式
- content pack 的 sample.flooded_relics 同步更新

---

### P1：建议本阶段尽快处理

#### P1-1：多样化隐藏内容触发机制

**问题本质**：6 种触发类型只用了 2 种  
**影响范围**：隐藏内容发现体验  
**优化目标**：至少使用 4 种不同触发类型，让 4 个 zone 的发现方式各不相同

**具体改法**：

| Secret Zone | 当前触发 | 建议改为 | 设计意图 |
|------------|---------|---------|---------|
| greenwood_hidden_cache | PERCEPTION_CHECK(8) | **保持** PERCEPTION_CHECK(8) | 作为最简单的教学用例 |
| deep_iron_slag_cache | PERCEPTION_CHECK(12) | **改为** KILL_ELITE + 精英掉落 key item | 击杀特定精英后揭示入口，增加战斗奖励 |
| underground_river_crystal_rift | PERCEPTION_CHECK(16) | **改为** QUEST_STEP（完成 zone 目标后揭示） | 奖励完成主线目标的玩家 |
| abyssal_temple_warded_archive | PERCEPTION_CHECK(20) | **改为** OPEN_CHEST + 特定 vault 内宝箱 | 必须找到并打开特定 vault 的宝箱才能揭示 |

同时将 `entranceBindingId` 从全部 `optional.branch.1` 改为不同锚点：
- greenwood: `optional.branch.1`
- deep_iron: `critical.route.2`（主线上的精英）
- underground_river: `critical.goal`（目标附近）
- abyssal: `optional.branch.2`（不同分支）

**改动优先级**：**P1**  
**预期收益**：
- 4 个 zone 的隐藏内容发现体验完全不同
- SearchAction 不再是"例行公事"，不同 zone 需要不同策略
- 已建好的 6 种触发类型被充分利用

**需同步修改**：
- `data/events/index.yaml`：修改 4 个 reveal event 的 triggerType 和 conditions
- `data/secret-zones/index.yaml`：修改 entryRule 和 entranceBindingId
- `data/mapgen/zones/index.yaml`：调整 hiddenEntrancePlans
- hiddenContentHarness：适配新触发条件的测试用例

---

#### P1-2：提升地形交互的可感知性

**问题本质**：地形交互存在但不可感知  
**影响范围**：战斗战术层 + 探索价值  
**优化目标**：让至少 2 种地形交互在每次 run 中可被感知到

**具体改法**：

1. **提升关键 zone 的地形覆盖率**：
   - underground_river: WATER 从 0.28 提升到 0.40，ICE 从 0.16 提升到 0.22
   - deep_iron_pit: OIL 从 0.24 提升到 0.35
   - 其他 zone 保持当前值

2. **改变地形分布策略**：修改 TerrainTagPainter 将地形优先铺设在：
   - 房间中心区域（而非边缘）
   - 走廊瓶颈处
   - vault / pattern room 内部

3. **将精英突变与地形绑定**：
   - `elite.tidebound` 增加约束：`preferredTerrainTag: WATER`，优先生成在有 WATER 的房间
   - `elite.emberblood` 增加约束：`preferredTerrainTag: OIL`
   - 新突变 `elite.flame_conduit`：在 OIL 地形战斗时火焰伤害 +50%

4. **新增至少 1 个利用地形的玩家技能/inscriptions**：
   - 如 inscription: `oil_flask`（在目标位置创造 OIL 地形 3 回合）
   - 或天赋增强：vanguard 的 `charge` 穿过 WATER 时对路径上的敌人造成 SLOW

**改动优先级**：**P1**  
**预期收益**：
- 地形从"装饰"变为"战术资源"
- 精英+地形组合创造了新的战术层
- 5 种交互规则从"死代码"变为"活体验"

**需同步修改**：
- `data/mapgen/biomes/index.yaml`：调整部分 zone 的 terrainTagWeights
- `game/mapgen/` 中的 TerrainTagPainter：修改分布策略
- `data/elites/index.yaml`：新增 preferredTerrainTag 字段
- `data/inscriptions/index.yaml` 或 `data/talents/index.yaml`：新增地形互动选项
- terrainInteractionBatch：扩展测试覆盖

---

#### P1-3：为 UNIQUE/ARTIFACT 引入独特效果

**问题本质**：UNIQUE/ARTIFACT 只是固定 affix 组合，无独特效果  
**影响范围**：掉落记忆点 + 构筑驱动力  
**优化目标**：每个 UNIQUE/ARTIFACT 至少有 1 个独有效果

**具体改法**：

在 UNIQUE/ARTIFACT 模板中新增 `uniquePassive` 字段：

```yaml
- id: unique.greenwood_watcher_blade
  fixedAffixIds: [sanctified, of_precision]
  uniquePassive:
    kind: ON_HIT_CHANCE
    chance: 0.15
    effectId: entangle  # 15% 攻击时缠绕目标 1 回合
    descKey: item.unique.greenwood_watcher_blade.passive.desc

- id: artifact.briar_heart
  fixedAffixIds: [vampiric, of_shadow]
  uniquePassive:
    kind: THRESHOLD_TRIGGER
    threshold: { stat: HP, below: 0.30 }
    effectId: stealth_1_turn  # HP<30% 时自动隐身 1 回合（每场战斗 1 次）
    cooldown: ONCE_PER_COMBAT
    descKey: item.artifact.briar_heart.passive.desc
```

**建议每个 Unique/Artifact 的独特效果方向**：

| 物品 | 效果方向 |
|------|---------|
| greenwood_watcher_blade | ON_HIT: 缠绕（控制辅助） |
| briarbound_bow | ON_KILL: 获得隐身 1 回合（暗杀链） |
| warden_barkmail | ON_DAMAGE_TAKEN: 棘甲反伤 |
| slagbreaker_pick | 对有护甲目标额外伤害 |
| furnace_plate | 受到 FIRE 伤害时回复 STAMINA |
| coal_ward_talisman | FIRE 技能消耗降低 20% |
| tideglass_staff | LIGHTNING 技能在 WATER 地形上 AOE 范围 +1 |
| currentstrider_cloak | 在 WATER/ICE 地形上移动速度 +30% |
| ferrywarden_lantern | 照亮隐藏入口（自动揭示 SECRET path） |
| voidlit_seal | HOLY 技能暴击率 +20% |
| eclipse_mail | 在 SHADOW 状态下防御 +50% |
| sanctum_lance | 暴击时施加 HOLY 标记，标记目标受到 HOLY 伤害 +20% |
| artifact.briar_heart | HP<30% 自动隐身 |
| artifact.forge_oath | 连续攻击同一目标，每次伤害 +5%（最多 +25%） |
| artifact.river_echo | 每使用 3 个技能，下一个技能无消耗 |
| artifact.eclipsed_relic | 暴击时恢复 5% 最大 HP |

**改动优先级**：**P1**  
**预期收益**：
- UNIQUE/ARTIFACT 成为真正的"构筑改变器"
- 掉落产生记忆点（"我找到了那把自动隐身的匕首！"）
- 不同 run 因 unique 掉落不同而走向不同构筑

**需同步修改**：
- `data/items/index.yaml`：所有 unique/artifact 新增 uniquePassive 字段
- `core/loot/` 或 `game/factory/ItemFactory.kt`：解析 uniquePassive
- combat pipeline：执行 uniquePassive 效果
- i18n bundle：新增所有 passive 描述

---

#### P1-4：增加 Boss 战的阶段性战术变化

**问题本质**：Boss 变体只调整行动权重，无战术层变化  
**影响范围**：zone 体验高点  
**优化目标**：Boss 变体引入至少 1 种特殊机制

**具体改法**：

在 BossVariantDef 中新增 `phaseOverrides` 字段，允许变体在特定阶段触发特殊行为：

```yaml
- id: boss.variant.molten_glass
  baseEncounterId: molten_giant_encounter
  grantedMutations: [elite.stonehide, elite.emberblood]
  phaseOverrides:
    - triggerCondition: { stat: HP, below: 0.50 }
      effect: TERRAIN_FLOOD  # Boss HP<50% 时房间 30% 面积变为 OIL
      duration: REST_OF_FIGHT
      descKey: boss.variant.molten_glass.phase2.desc
```

3 个变体的建议机制：
- **molten_glass**：HP<50% 时地面出现 OIL → 配合 FIRE 技能自动点燃
- **grey_crown**：HP<50% 时召唤 2 个 cultist 小怪 + 开启 WEAKEN 光环
- **abyssal_eclipse**：HP<50% 时房间变暗（视野减半）+ 获得隐身后偷袭

**改动优先级**：**P1**  
**预期收益**：Boss 战产生"前半段 → 阶段切换 → 后半段需要换策略"的战术层变化

**需同步修改**：
- `data/boss-variants/index.yaml`：新增 phaseOverrides
- Boss encounter runtime：执行 phaseOverrides
- i18n bundle：boss 阶段切换描述

---

### P2：可以排期但不应忽略

#### P2-1：丰富 Loot Profile 的 Secret Zone 奖励差异

**问题本质**：secret zone loot profile 与普通掉落高度重叠  
**影响范围**：隐藏内容的奖励感知  
**具体改法**：每个 secret zone 的 loot profile 新增 2~3 件该 zone 独有的物品（base item 或 inscription），且这些物品不出现在普通掉落中  
**改动优先级**：**P2**

#### P2-2：新增 2~3 个非战斗型隐藏事件

**问题本质**：所有隐藏事件都是"揭示+掉落"，缺乏非战斗互动  
**影响范围**：探索层的叙事感  
**具体改法**：新增"lore_cache"（阅读获得临时 buff）、"trapped_shrine"（有风险的选择：触发陷阱 or 获得大奖）、"wandering_trader"（隐藏商人）类型的事件  
**改动优先级**：**P2**

#### P2-3：Boss 变体数量从 3 提升到 5~6

**问题本质**：3 个变体覆盖不了所有 zone  
**影响范围**：Boss 战多样性  
**具体改法**：为 underground_river 和 greenwood_fringe 区域补充 Boss 变体（当前这两个 zone 的 bossEncounterId 为空，需先确认 Boss encounter 是否存在）  
**改动优先级**：**P2**

#### P2-4：引入 affix exclusiveGroup 和 synergy 机制

**问题本质**：affix 间无互斥/协同关系  
**影响范围**：构筑决策深度  
**具体改法**：
- 部分强力 affix 加入 `exclusiveGroup`（如 `vampiric` 和 `berserking` 互斥，逼迫玩家选择防御续航 or 进攻爆发）
- 部分 affix 对加入 `synergyBonus`（如 `frozen_edge` + `of_storms` 同时存在时有额外效果）  
**改动优先级**：**P2**

#### P2-5：新增怪物行为差异化

**问题本质**：大部分杂兵的战斗行为是"靠近→攻击"  
**影响范围**：普通战斗的有趣度  
**具体改法**：
- 为至少 10 种怪物新增特殊行为标签（召唤、逃跑、治疗同伴、设陷阱）
- 配合 `data/ai/index.yaml` 现有框架扩展行为模板  
**改动优先级**：**P2**

#### P2-6：确保双 pack precedence fixture 存在

**问题本质**：设计要求 ≥1 个双 pack 场景但未确认实现  
**影响范围**：content pack 测试覆盖  
**具体改法**：创建第二个 fixture pack（如 `sample.iron_forge_expansion`），测试 precedence 和 conflict 场景  
**改动优先级**：**P2**
