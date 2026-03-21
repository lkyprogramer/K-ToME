# Phase2 深度审查报告 — Part 3：关键问题与优化建议

---

## 5. 当前 Phase2 最需要解决的关键问题

### 5.1 [Critical-01] 战斗掉落完全缺失——核心循环断裂

**问题描述**：杀死怪物只获得 XP，不产生任何物品掉落。`loot/index.yaml` 定义了 common/elite/boss 三档掉落表，`ItemGenerator` 实现了完整的随机物品生成（品质/材料/词缀），但 `FoundationGameSession` 中怪物死亡流程没有调用掉落逻辑。

**证据**：
- `game/src/main/resources/data/loot/index.yaml`：3 个基础掉落表 + 3 个 zone reward 表
- `core/.../item/ItemGenerator.kt`：完整的 `generate(floor)` 方法
- `game/.../FoundationGameSession.kt`：怪物死亡段只调用 `ExperienceSystem.applyReward`，无 loot 相关代码

**为什么必须在 Phase 2 解决**：
1. 这是 Roguelike 核心循环最基础的环节——没有掉落就没有装备成长
2. Phase 2 的物品系统（24 物品 + 4 材料 + 7 词缀）、loot profile 分层、zone reward 全部建立在"掉落存在"的前提上
3. 如果 Phase 2 不接通这条管线，后续 Phase 的任何物品扩展（套装、深词缀、专属装备）都是建在空中
4. SoloClearLab 的验证如果不包含装备变化，就无法证明战斗-装备循环闭环

**不解决的后果**：
- Phase 2 所有物品/材料/词缀内容等于没做
- 玩家对"再来一局"毫无动力
- Phase 3 的装备深度扩展没有可验证的地基

### 5.2 [Critical-02] 资源消耗双轨并存——数据一致性隐患

**问题描述**：STAMINA 通过 `core/.../ecs/Stamina` 组件直接扣减（TalentResolver 中 `world.get<Stamina>(user)` 直接修改），其他资源（MANA/ENERGY/POSITIVE_ENERGY）通过 `ResourcePools` 扣减。`PlayerResourcePools.syncStaminaPoolFromComponent` 试图弥合两者，但这是一个状态同步的胶水模式。

**证据**：
- `core/.../talent/TalentResolver.kt`：`spendResources` 方法（未读到但基于注册的 talent resourceCosts，STAMINA 技能走 Stamina 组件扣费）
- `game/.../PlayerResourcePools.kt` L127-144：`syncStaminaPoolFromComponent` 将 Stamina 组件值镜像到 ResourcePools
- `PlayerResourcePools.kt` 注释 L123-126：明确说 "STAMINA is still mutated directly by core systems, so the component remains the canonical runtime value"

**为什么必须在 Phase 2 解决**：
1. 双轨写意味着任何修改 stamina 的代码路径（天赋消耗、装备效果、回合恢复）都必须记得同步 ResourcePools，漏一个就是不一致
2. save/load 的 snapshot 需要从两个来源合并——如果 sync 时序不对，读档后资源值可能错误
3. HUD 显示依赖 ResourcePools，如果与实际 Stamina 组件不同步，显示就是错的
4. Phase 3 如果加入更多资源交互（例如"消耗 HP 换 MANA"的跨资源技能），双轨模式会导致bug

**不解决的后果**：
- save/load 资源不一致的概率性 bug
- HUD 显示偶尔与实际值不同步
- 后续每个新的资源交互系统都要额外处理双轨同步

### 5.3 [Critical-03] Zone Route 未实现——短局不存在

**问题描述**：`FoundationGameConfig.FOUNDATION_ZONE_ROUTE` 定义了 4 zone 的路线列表，但 `FoundationGameConfig.zoneRoute` 默认值为 `listOf(zoneId)` 即单 zone。session 中没有 zone transition 逻辑，玩家打完 2 层就结束。

**证据**：
- `game/.../FoundationGameConfig.kt` L24：`zoneRoute: List<String> = listOf(zoneId)` — 默认单 zone
- `FoundationGameSession.kt`：胜利条件基于 boss 击杀或所有楼层完成，没有 route 推进逻辑

**为什么必须在 Phase 2 解决**：
1. "4 zone 短局"是 Phase 2 的核心承诺——不实现 route transition，Phase 2 的出口标准不可能达成
2. 这是执行计划 Sprint 4 的核心内容，但它直接影响 Phase 2 是否有足够的游戏时长和内容量

**不解决的后果**：
- Phase 2 的"短局"实际上是一个 10~15 分钟的单 zone 体验
- 4 zone 的数据设计（不同怪物池/tileset/难度曲线）完全无法被玩家触及
- zone reward loot profile 无法生效

### 5.4 [High-01] 天赋解锁节奏严重失衡——无成长驱动

**问题描述**：每个职业在 `professions/index.yaml` 中的 `startingTalents` 直接给出 4 个技能。配合 `TalentProgression.unlockedTalentIds` 的解锁逻辑，所有天赋在 level 5 前全部解锁。

**证据**：
- `professions/index.yaml`：每个职业 startingTalents 4 个
- `talents/index.yaml`：unlockLevel 分布为 1(×4), 2(×1), 3(×1), 4(×2), 5(×1) per profession
- `game/.../TalentProgression.kt`：`level >= schema.unlockLevel` 即解锁

**为什么必须在 Phase 2 解决**：
1. "升级解锁新技能"是 RPG 最基础的驱动力，没有这个驱动力等于游戏的成长维度少了一半
2. Phase 2 的 SoloClearLab 要求验证职业的构筑 answer——如果 1 级就拿满全部工具，就无法测试"在有限工具下如何应对"的构筑选择
3. 这个问题不是"以后加更多天赋"能解决的——即使 Phase 3 加到 16 个天赋/职业，如果解锁节奏同样快，问题只是推迟不是消除

**不解决的后果**：
- 玩家 level 5 后没有任何新解锁期待
- "这局我走不同 build"的动力不存在——所有技能都是全开的
- SoloClearLab 无法真正验证"受限工具下的构筑能力"

### 5.5 [High-02] DamageType 无实战差异——伤害系统名存实亡

**问题描述**：6 种伤害类型已定义，CombatResolver 有完整的抗性计算，但所有怪物 0 抗性、0 穿透。

**证据**：
- `monsters/index.yaml`：24 个怪物均无 `resistances` 字段
- `CombatResolver.kt` L46-58：元素抗性计算只在 `damageType.isElemental` 时生效，但 `targetResistance` 始终为 0
- `DamageType.kt`：HOLY 有 `isElemental = false` 的特殊设计，但 +50% 对亡灵的规则没有在代码中实现

**为什么必须在 Phase 2 解决**：
1. DamageType 差异化是 Arcanist/Templar 职业身份的核心——如果 FIRE/COLD/HOLY 没有区别，这两个职业的战术选择就是假的
2. Phase 3 的抗性深化公式建立在"Phase 2 已验证基础抗性体验"的前提上
3. 最少需要给关键怪物家族设置基础抗性/弱点，让 DamageType 产生可感知的差异

**建议的 Phase 2 最小版本**（不是完整抗性系统，而是最小可感知版本）：
- 亡灵系怪物：HOLY -25%（弱点）, SHADOW +25%（抗性）
- 兽人/盗匪：无特殊抗性（纯物理对标）
- 邪教徒：FIRE -15%, HOLY -15%（双弱点）
- 元素系（如果有）：对应元素 +50%

**不解决的后果**：
- Arcanist 的火系/冰系选择没有意义
- Templar 的 HOLY 伤害没有任何优势
- 战斗退化为"谁倍率高用谁"的无脑选择

### 5.6 [High-03] statGrowth 未自动生效——职业等级成长无差异

**问题描述**：profession schema 定义了 statGrowth（如 Vanguard str+2/con+2, Arcanist wil+3），但 `ExperienceSystem.applyReward` 只增加 `unspentStatPoints`，不叠加 statGrowth。

**证据**：
- `professions/index.yaml`：每个职业有 `statGrowth` 字段
- `core/.../progression/ExperienceSystem.kt` L31-46：升级只做 `experience.unspentStatPoints += 2`，没有读取或叠加 statGrowth

**为什么必须在 Phase 2 解决**：
1. 没有 statGrowth，Vanguard 和 Arcanist 的基础属性差异只来自初始 baseStats
2. 到高等级时，手动分配的 stat points 会淹没初始差异，所有职业趋同
3. Phase 3 的高级职业分化建立在"基础职业已有明确成长曲线差异"的前提上

**不解决的后果**：
- 高等级时职业数值趋同
- "Vanguard 就是坦克，Arcanist 就是法师"的身份在等级 10+ 后模糊

### 5.7 [High-04] Rogue / Templar 未经实战验证

**问题描述**：两个职业的 YAML 数据完整，TalentResolver 已实现所有 16 个天赋的 resolution 逻辑，但未通过 SoloClearLab 或 headlessSmoke 的实战验证。

**证据**：
- `TalentResolver.kt` L107-142：32 个 supportedTalentIds 包含全部 Rogue/Templar 天赋
- 但 headlessSmoke / clientSmoke 当前主要覆盖 vanguard + arcanist
- SoloClearLab 框架存在但四职业验证未全部通过

**为什么必须在 Phase 2 解决**：
- Phase 2 的出口标准是 4 职业 SoloClearLab 全绿
- 未验证意味着可能存在资源闭环 bug（ENERGY 回复不足导致技能无法使用等）

### 5.8 [Medium-01] AI 行为过于简单——战斗无战术压力

**问题描述**：当前 AI 只有 CHASE/KITE/PATROL 三种行为模式，没有技能使用、相位转换、策略响应。

**证据**：
- `core/.../ai/AIDecision.kt`：只实现 chase/kite/patrol 三种决策
- `ai/index.yaml`：AI profile 仅包含行为类型和参数
- 精英/Boss 与普通怪的 AI 完全相同，只是数值不同

**为什么应在 Phase 2 解决**：
- 文档要求精英/Boss "至少使用 Layer 2 simple scripted AI"
- 当前 Boss 只是 HP 更高的追击怪，没有任何特殊行为
- SoloClearLab 的 Boss 场景如果 Boss 只会追击+普攻，测试的不是"玩家 build 能否应对"，而是"数值能否碾压"

**最小可行改进**（Phase 2 范围内）：
- Boss 低 HP 时触发狂暴（攻击力+50%）
- 精英有一个签名技能（如 sentry 的 shield_bash, forge_guard 的 fire AOE）
- Boss 使用 1~2 个天赋（如 Bandit Captain 使用 war_cry + sweeping_strike）

### 5.9 [Medium-02] 物品没有主动效果——装备系统是数值板

**问题描述**：当前所有装备只提供 baseAttack/baseDefense 等纯数值属性，没有主动技能、被动触发、或改变玩法的特殊效果。消耗品有 `effect`（heal/restore）但装备没有。

**证据**：
- `items/index.yaml`：所有装备只有 `baseAttack`/`baseDefense`
- `ItemGenerator.kt`：词缀只叠加 `StatModifier`（ATK+3, DEF+2, SPD+10 等）
- 没有"装备 X 时，火系技能伤害+15%"或"受到暴击时有 20% 几率眩晕攻击者"这类效果

**为什么应在 Phase 2 考虑（至少做最小版本）**：
- 纯数值装备无法创造构筑分化——所有玩家都选 ATK 最高的武器和 DEF 最高的防具
- Phase 2 有 4 个 zone signature reward item（bandit_trophy, emerald_charm, furnace_talisman, seal_reliquary），如果它们只是数值略高的普通装备，就失去了 signature 的意义
- 不需要复杂的装备联动系统，但 signature reward 至少应有一个简单的特殊效果

### 5.10 [Medium-03] Blink 的 levelEffects 空——升级无收益

**问题描述**：blink 技能的 levelEffects 只影响范围判定，没有提供 damage/buff/utility 方面的等级提升。

**证据**：
- `talents/index.yaml` 中 blink 的 levelEffects（需验证具体内容，执行计划已识别）

**为什么应在 Phase 2 修复**：
- blink 是 Arcanist 的核心生存技能，如果升级没有收益，talent point 分配就浪费了
- 修复简单：增加 levelEffects 中的冷却缩短或范围扩大或免伤窗口

---

## 6. 优化建议（按优先级）

### P0：必须立刻处理

#### P0-1：接入怪物掉落系统

**问题本质**：核心循环断裂——战斗无奖励
**影响范围**：全局游戏体验
**优化目标**：杀怪产生物品掉落，common/elite/boss 分层体验成立

**具体改法**：
1. 在 `FoundationGameSession` 的怪物死亡处理段，根据怪物的 `lootProfileId` 查询 `loot/index.yaml`
2. 调用 `ItemGenerator.generate(currentFloor)` 或从 loot profile 直接抽取
3. 掉落物品放置在怪物死亡位置（GroundItem 组件已存在）
4. 掉落概率建议：
   - Normal 怪：30% 基础概率，产生 COMMON 为主的物品
   - Elite 怪：60% 概率，可产生 MAGIC 品质
   - Boss：100% 概率，至少 1 件 RARE + 从 boss loot profile 中额外抽取 1 件
5. 同时接入 loot 拾取的 i18n 日志 token（`session.log.pick_up` 已存在于 en-US.json）
6. 同时更新 AudioRouter 的掉落/拾取 cue

**改动优先级**：**P0**
**预期收益**：核心循环闭合，战斗从"纯消耗"变为"有产出"，24 物品 + 品质/材料/词缀系统开始参与游戏
**可能副作用**：掉落过于频繁可能导致背包溢出；掉落品质不平衡可能导致难度崩溃
**风险控制**：先用保守的掉落率（30/60/100），后续根据 SoloClearLab 数据调整
**需要同步修改**：FoundationGameSession, i18n 日志, AudioRouter, 可能需要增加背包容量上限提示

#### P0-2：统一资源消耗到 ResourcePools

**问题本质**：STAMINA 双轨写导致一致性风险
**影响范围**：core 层天赋消耗 + game 层资源同步 + save/load + HUD
**优化目标**：所有资源消耗和恢复都走 ResourcePools 唯一路径

**具体改法**：
1. TalentResolver 的 `spendResources` 改为从 `ResourcePools` 扣减 STAMINA，而不是直接修改 `Stamina` 组件
2. `Stamina` 组件可以保留作为 `ResourcePool(STAMINA)` 的薄包装或直接删除，用 ResourcePools 完全替代
3. `StatsCalculator` 中引用 maxStamina 的逻辑也统一到 ResourcePools
4. save/load 的 stamina 字段映射到 ResourcePools 中的 STAMINA 池
5. 删除 `PlayerResourcePools.syncStaminaPoolFromComponent` 胶水方法

**改动优先级**：**P0**
**预期收益**：消除数据一致性风险，为 Phase 3+ 的资源交互系统扫清障碍
**可能副作用**：save format 微调（但 Schema V2 支持版本迁移）
**需要同步修改**：TalentResolver, PlayerResourcePools, SessionSnapshotMapper, Stamina 组件, StatsCalculator

#### P0-3：实现 Zone Route Transition

**问题本质**：4 zone 短局是 Phase 2 核心承诺，route 不通则短局不成立
**影响范围**：GameSession, SaveSnapshot, Config, 全部 route 相关测试
**优化目标**：玩家在一次 run 中可以依次通过 4 个 zone

**具体改法**：
1. `FoundationGameConfig.zoneRoute` 默认值改为 `FOUNDATION_ZONE_ROUTE`
2. `FoundationGameSession` 增加 zone transition 逻辑：
   - 当前 zone 的 boss 被击杀或 objective 完成后，触发 zone transition
   - 保存 routeIndex 到 SaveSnapshot
   - 加载下一个 zone 的地图、怪物池、tileset、ambience
3. `SaveSnapshot` 增加 `routeIndex` 和 `zoneRoute` 字段
4. `SessionSnapshotMapper` 增加 route 状态的序列化/反序列化

**改动优先级**：**P0**
**预期收益**：4 zone 短局成立，游戏时长从 10~15 分钟扩展到 30~60 分钟
**可能副作用**：zone transition 时的状态迁移可能引入 bug
**需要同步修改**：FoundationGameConfig, FoundationGameSession, SaveSnapshot, SessionSnapshotMapper, 相关测试

---

### P1：建议本阶段尽快处理

#### P1-1：修复天赋解锁节奏

**问题本质**：1 级拿满工具箱，成长驱动消失
**影响范围**：professions/index.yaml, TalentProgression
**优化目标**：建立清晰的解锁里程碑，让玩家每 2~3 级有新技能期待

**具体改法**：
1. 减少每职业的 `startingTalents` 到 2 个（1 个主输出 + 1 个核心工具）
2. 调整 `unlockLevel` 分布：
   ```
   Level 1: 2 个起始天赋
   Level 3: 解锁第 3 个天赋（副输出或控制）
   Level 5: 解锁第 4 个天赋（AOE 或增强）
   Level 7: 解锁第 5~6 个天赋（构筑分支）
   Level 9: 解锁第 7 个天赋（核心升级）
   Level 11+: 解锁第 8 个天赋（panic answer 或终极技能）
   ```
3. 这样到 level 10（Phase 2 短局预期终点），玩家解锁了 7~8 个技能，最后 1 个作为"下次我到更高级会有什么"的钩子
4. 解锁时在日志中明确提示"你解锁了新技能：XXX"

**示例 Vanguard 调整**：
- startingTalents: [power_strike, guard_stance]（原来是 4 个）
- Level 1: power_strike(1), guard_stance(1)
- Level 3: shield_bash(3)
- Level 5: war_cry(5)
- Level 7: sweeping_strike(7), sunder_armor(7)
- Level 9: intimidation(9)
- Level 11: unyielding(11)

**改动优先级**：**P1**
**预期收益**：每次升级有期待感，构筑选择空间增大
**需要同步修改**：professions/index.yaml 的 startingTalents, talents/index.yaml 的 unlockLevel

#### P1-2：给关键怪物家族添加基础抗性

**问题本质**：DamageType 无感，伤害类型选择无意义
**影响范围**：monsters/index.yaml, 可能需要 ResistanceProfile 组件接入
**优化目标**：让 2~3 种伤害类型在实战中产生可感知差异

**具体改法**：
1. 在 monster schema 中增加 `resistances` 字段（或确认现有 `ResistanceProfile` 组件可接入）
2. 最小抗性配置：

| 怪物家族 | PHYSICAL | FIRE | COLD | HOLY | SHADOW |
|----------|---------|------|------|------|--------|
| beast | 0 | 0 | 0 | 0 | 0 |
| undead | 10 | -10 | 0 | -25 | 25 |
| bandit | 0 | 0 | 0 | 0 | 0 |
| orc | 10 | -10 | 0 | 0 | 0 |
| cultist | 0 | -15 | 0 | -15 | 15 |
| goblin | 0 | 0 | -10 | 0 | 0 |

3. 确保 CombatResolver 已有的抗性计算逻辑能正确读取这些数据
4. 在战斗日志中显示抗性效果（"Fire damage! Undead is vulnerable (-10% resist)"）

**改动优先级**：**P1**
**预期收益**：DamageType 开始产生实战意义，Arcanist 需要选择合适的元素，Templar 的 HOLY 对亡灵有优势
**需要同步修改**：monsters/index.yaml, EntityFactory（确保 ResistanceProfile 被创建），战斗日志 i18n

#### P1-3：statGrowth 自动生效

**问题本质**：职业等级成长无差异
**影响范围**：ExperienceSystem, EntityFactory, profession schema
**优化目标**：升级时自动叠加 statGrowth，让职业成长曲线分化

**具体改法**：
1. 在 `ExperienceSystem.applyReward` 中，每次升级时叠加当前职业的 statGrowth 到 Stats 组件
2. 或者在升级后由 `FoundationGameSession` 调用一个 growStats 方法

**改动优先级**：**P1**
**预期收益**：Vanguard 高级时明显更坦（+2 STR/+2 CON/级），Arcanist 高级时法力更强（+3 WIL/级）
**需要同步修改**：ExperienceSystem 或 FoundationGameSession

#### P1-4：Rogue / Templar 的 SoloClearLab 验证

**问题本质**：两职业数据就位但未经实战验证
**影响范围**：SoloClearLabTest, SoloClearLabSupport
**优化目标**：4 职业 × 3 场景全部通过

**具体改法**：
1. 运行现有 SoloClearLab 测试套件，确认 Rogue/Templar 各场景结果
2. 如果某职业某场景失败，分析原因：是数值不平衡？是资源闭环有 bug？是 AI 不足？
3. 调整对应天赋数值/资源参数直到通过

**改动优先级**：**P1**
**预期收益**：4 职业正式化完成，Phase 2 出口标准达成
**需要同步修改**：可能需要微调天赋数值、资源回复参数

#### P1-5：Boss / Elite 最小行为差异化

**问题本质**：精英/Boss 只是 HP 更高的普通怪
**影响范围**：AI 系统, boss 配置, TalentResolver（怪物使用天赋）
**优化目标**：Boss 至少有 1~2 个特殊行为，精英至少有 1 个签名技能

**具体改法**：
1. Bandit Captain：
   - 低 HP（<30%）时触发 "Enrage"（攻击力 +50%, 速度 +20%, 持续到战斗结束）
   - 每 5 回合使用一次 war_cry（对玩家造成 debuff）
2. Cultist Dungeon Lord：
   - 每 3 回合释放一次 shadow bolt（SHADOW 伤害，范围 6）
   - 低 HP 时召唤 2 个 chain_thrall
3. 精英怪至少有一个 simple script：
   - bone_guard：有 guard_stance
   - forge_guard：有 fire AOE
   - wild_huntmaster：有 multi-shot

**实现方式**：不需要完整的 AI DSL，只需在怪物 AI profile 中增加一个 `scripts` 列表，每个 script 是 `if condition then action` 的简单规则。FoundationGameSession 在怪物行动时检查 scripts。

**改动优先级**：**P1**
**预期收益**：Boss 战有策略深度，精英遭遇有差异感
**需要同步修改**：AI 系统, monster schema, FoundationGameSession 怪物行动逻辑

---

### P2：可以排期但不应忽略

#### P2-1：在战斗日志中显示伤害类型和抗性效果

**问题本质**：数值反馈不透明
**改法**：日志从 "You hit the Skeleton for 12 damage" 改为 "You hit the Skeleton for 12 FIRE damage (vulnerable!)"
**优先级**：P2
**收益**：玩家理解 DamageType 系统，形成"选对元素打弱点"的学习循环

#### P2-2：升级时明确显示新解锁和属性变化

**问题本质**：成长反馈不足
**改法**：升级日志增加 "Level Up! New talent unlocked: Shield Bash" + "STR +2, CON +2 (stat growth)"
**优先级**：P2
**收益**：每次升级有明确的"变强了"的反馈

#### P2-3：给 4 个 zone signature reward 添加简单特殊效果

**问题本质**：zone reward 没有记忆点
**改法**：
- bandit_trophy：装备后 +10% 对 bandit 系伤害
- emerald_charm：每回合恢复 1 HP
- furnace_talisman：FIRE 伤害 +15%
- seal_reliquary：HOLY 伤害 +15%

**优先级**：P2
**收益**：zone reward 有"值得拿"的感觉，奖励驱动增强

#### P2-4：增加怪物死亡的掉落预览/确认 UI

**问题本质**：掉落接入后需要玩家能看到和理解掉落物品
**改法**：掉落物品在地面显示图标/颜色区分品质（COMMON 白色/MAGIC 蓝色/RARE 黄色），拾取时日志显示完整属性
**优先级**：P2
**收益**：掉落系统的反馈闭环完整

#### P2-5：修复 blink 的 levelEffects

**问题本质**：核心生存技能升级无收益
**改法**：
```yaml
levelEffects:
  1: { rangeBonus: 0 }
  2: { rangeBonus: 1 }
  3: { rangeBonus: 1, cooldownReduction: 1 }
  4: { rangeBonus: 2, cooldownReduction: 1 }
  5: { rangeBonus: 2, cooldownReduction: 2, buffDuration: 1, buffMagnitude: 0.15 }  # 闪现后 1 回合减伤 15%
```
**优先级**：P2
**收益**：blink 升级有明确收益，Arcanist 的 talent point 分配有意义

#### P2-6：增加死亡回顾界面

**问题本质**：死因不透明
**改法**：GameOverScreen 增加 "Death Recap"：最后 5 回合的日志、杀死你的怪物信息、你的最终属性
**优先级**：P2
**收益**：玩家理解失败原因，产生"下次我应该..."的学习动力
