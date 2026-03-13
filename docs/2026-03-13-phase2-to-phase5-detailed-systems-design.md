# K-ToME Phase 2 ~ Phase 5 核心系统详细设计与阶段补充文档

> 日期：2026-03-13  
> 状态：Draft for execution  
> 定位：在 [phase2-to-phase5-final-roadmap.md](./2026-03-13-phase2-to-phase5-final-roadmap.md) 的基础上，补齐系统级详细设计、迁移策略、阶段细化与门禁  
> 适用范围：Phase 2 ~ Phase 5  
> 文档语言：中文  
> 约束前提：基于当前 Phase 1 实际代码现状，不假设不存在的系统已经落地

---

## 1. 直接结论

当前仓库已经有一条能支撑 Phase 1 的最小主线：

`100 能量回合 + 单一近战公式 + Stamina + 4 个硬编码战士天赋 + 3 种 AI + ASCII/glyph 表达`

这条主线“能玩”，但还不是后续阶段可以长期演进的基础设施。

后续如果直接堆 Tile、职业、元素、Boss、掉落和大内容量，而不先把以下基础合同提升为一等概念，返工几乎不可避免：

1. 回合与行动经济合同
2. 战斗结算管线合同
3. 伤害通道与元素防御合同
4. 多资源模型合同
5. 状态效果生命周期合同
6. 技能/天赋数据 schema 合同
7. 职业与怪物内容 schema 合同
8. 事件、日志、回放、存档的语义合同
9. AI 脚本化 DSL 与 Boss phase 合同
10. `RenderSnapshot` 与客户端消费边界

本文的目的不是重复路线图，而是把这些系统先设计清楚，作为后续 AI 开发和 AI 验证的实现锚点。

---

## 2. 当前 Phase 1 实际基线

### 2.1 当前代码已经具备的能力

1. `core` 已有 ECS、地图、FOV、A*、近战、经验、物品、天赋、随机源、存档骨架。
2. `game` 已有 YAML 数据加载、怪物/物品/Boss 工厂、会话装配和部分自动化测试。
3. `client` 已有 ASCII/glyph 渲染、输入、主菜单和基础 GameScreen。
4. `core` 已经具备较好的可测试性，很多规则都有固定 seed 单测。

### 2.2 当前系统的真实上限

| 系统 | 当前现状 | 当前上限 |
| --- | --- | --- |
| 回合调度 | `TurnScheduler`，阈值 `100`，按 `EntityId` 排序 | 只能表达简单速度差，不能自然表示快慢行动和高分辨率行动经济 |
| 战斗 | 单次近战标量结算，命中/暴击/伤害/死亡 | 无元素、无抗性、无穿透、无多段伤害、无护盾层级 |
| 资源 | `Health / Stamina / Energy` | 无通用资源池，无多职业资源策略 |
| 状态 | `STUNNED / ARMOR_BREAK / WAR_CRY_BUFF / WAR_CRY_DEBUFF` | 无完整生命周期、无堆叠规则、无清晰的 dispel/immunity |
| 天赋 | 4 个战士技能，`TalentResolver` 大 `when` 硬编码 | 无法扩展为多职业、多效果、多 telegraph 的技能系统 |
| AI | `CHASE / KITE / PATROL` | 无脚本化行为、无 Boss phase、无目标记忆、无 telegraph 感知 |
| 职业 | 没有独立职业模型，玩家固定 `Hero` | 无职业树、无起始套装、无职业 schema |
| 怪物/Boss | 怪物模板很薄，Boss 只是“怪物 + 两个技能” | 无 encounter script、无 phase、无高级 AI |
| 内容 schema | 仍写 `name/glyph/color` 等展示字段 | 无 i18n key、无 visual/audio key、无版本化 schema |
| 存档 | `Gson + SaveSnapshot`，直接保存 glyph、名称、消息日志字符串 | 无语义事件 token，无长期版本纪律 |

### 2.3 当前代码直接暴露出的断层

1. `TalentResolver` 只支持 4 个硬编码 talent id。
2. `GameEvent` 只有事件类型，没有事件边界、顺序、来源与 phase。
3. `FoundationGameSession` 同时承担了回合驱动、战斗接线、日志、存档、状态衰减、AI 执行等多项职责，已经过厚。
4. `StatsCalculator` 已经计算出 `critChance / talentPower / hpRegen`，但这些值还没有完整进入结算主链。
5. 状态衰减逻辑不在 `core`，而在 `game` 回合推进中。

### 2.4 关键设计缺口总览

后续实现优先级以“先补基础合同，再堆内容”为准，当前最关键的缺口如下：

| 缺口 | 当前问题 | 不先处理的后果 | 优先级 |
| --- | --- | --- | --- |
| 战斗语义缺口 | 只有单标量近战，无 `DamageType / PowerSave / Shield / Trace` | 后续职业、元素、Boss 技能都会重复造轮子 | `P0` |
| 状态生命周期缺口 | 状态少且衰减分散，缺堆叠/驱散/免疫真源 | 技能设计无法稳定复用 | `P0` |
| 技能 schema 缺口 | `TalentResolver` 硬编码 4 个技能 | 无法 AI 生成、AI 验证、批量扩展内容 | `P0` |
| 资源模型缺口 | 只有 `Stamina` 真正进入玩法主链 | 8 职业无法建立差异化循环 | `P1` |
| AI 合同缺口 | 只有 3 种模式，无 trace/DSL/Boss phase | 怪物行为只能继续写特判 | `P1` |
| 内容 schema 缺口 | 展示字段和规则字段混杂 | i18n、tile、音频和内容包无法稳定落地 | `P1` |

### 2.5 设计原则补充

1. 先冻结语义，再扩内容；任何新职业、新怪、新技能都必须挂在既有合同上。
2. 规则真值留在 `core`，表现 key 和资源解析留在 `game/client`。
3. 同一概念只能有一个权威 schema，例如 `CombatTrace`、`DamageTypeConfig`、`StatusEffectDef` 都不能并行出现第二套。
4. 优先写可 trace、可 seed、可 golden 的系统，而不是先写“看起来很完整”的内容量。
5. 在 phase 内允许数值再平衡，但不允许破坏已冻结的字段语义和阶段边界。

---

## 3. 全局冻结决策

以下决策从本文开始冻结，除非后续明确在文档中变更。

### 3.1 属性模型冻结

为了控制复杂度，并尊重当前 Phase 1 代码现实，`v1.0.0` 之前继续冻结为 4 主属性模型：

1. `STR`
2. `DEX`
3. `CON`
4. `WIL`

不在 `Phase 2 ~ Phase 5` 内再引入 `MAG/CUN` 六维属性体系。

原因：

1. 当前所有规则、工厂、存档、测试都基于 4 属性。
2. 4 属性足以支撑 8 个职业的最小差异化，只需要把“职业识别度”更多放进资源、技能树、状态和 telegraph，而不是增加统计维度。
3. 过早扩大主属性维度会显著放大 schema、UI、balance、存档和测试矩阵。

### 3.2 伤害与能力通道冻结

#### 伤害通道固定为 6 个

```kotlin
enum class DamageType {
    PHYSICAL,
    FIRE,
    COLD,
    LIGHTNING,
    HOLY,
    SHADOW,
}
```

说明：

1. `PHYSICAL` 用于武器、冲撞、流血类直接伤害。
2. `FIRE / COLD / LIGHTNING` 作为奥术师和环境危害的三元素主通道。
3. `HOLY / SHADOW` 用于圣堂武士、Boss 与暗影系机制。
4. `POISON / BLEED` 不是独立伤害通道，而是状态族；其 DOT 默认映射到 `PHYSICAL` 或 `SHADOW`。

#### 能力对抗通道固定为 3 个

```kotlin
enum class PowerType {
    PHYSICAL,
    SPELL,
    MENTAL,
}
```

说明：

1. `PHYSICAL` 用于击退、破甲、流血、眩晕等肉搏和器械类能力。
2. `SPELL` 用于元素法术、圣光、暗影法术。
3. `MENTAL` 用于诡计、感知、恐惧、印记、伏击和部分 Ranger/Shadow 技能。

### 3.3 资源模型冻结

所有可消耗数值统一走通用资源池，不再为每个职业新建专用组件。

### 3.4 状态模型冻结

状态系统从“枚举 + 特判”升级为“注册表 + 实例 + 生命周期 + 堆叠规则”。

### 3.5 技能系统冻结

技能/天赋系统不引入通用脚本 runtime。

`Phase 2 ~ Phase 5` 只使用：

1. 声明式 YAML/JSON schema
2. typed effect op
3. typed targeting
4. typed telegraph
5. typed AI hints

### 3.6 AI 系统冻结

`Phase 2 ~ Phase 4` 采用“脚本化 DSL + trace”的方式演进 AI，不引入 Lua。

`Phase 5` 的战术 AI 也必须建立在同一套 action catalog 与 perception model 上，而不是从头重写怪物系统。

---

## 4. 现有代码的重构落点

以下现有文件在 `Phase 2` 起必须被重构或替代：

| 当前文件 | 问题 | 后续定位 |
| --- | --- | --- |
| `core/turn/TurnScheduler.kt` | 阈值过低，无法表达丰富行动成本 | 升级为 `1000` 能量制调度器 |
| `core/combat/CombatResolver.kt` | 只能输出标量近战结果 | 升级为 `DamageRequest -> DamageOutcome -> CombatTrace` |
| `core/talent/TalentModels.kt` | 面向 4 个技能的字段袋 | 升级为 V2 schema 与 effect program |
| `core/talent/TalentResolver.kt` | `supportedTalentIds + when` 硬编码 | 升级为 executor registry |
| `core/event/GameEvent.kt` | 只有事件类型，没有时序边界 | 升级为带 `turnId/actionId/seq/phase/cause` 的语义事件 |
| `core/save/SaveManager.kt` | 仍基于 GSON | 切到 `kotlinx.serialization` |
| `game/data/DataLoader.kt` | 解析的还是展示导向 schema | 切到 key/manifest/schemaVersion 导向 |
| `game/model/MonsterTemplate.kt` | 无职业化/行为化/表现化字段 | 升级为 MonsterTemplate V2 |
| `game/model/BossDefinition.kt` | 只是怪物模板 + 技能等级 | 升级为 Encounter/BossPhase/AIProfile 组合 |
| `game/FoundationGameSession.kt` | 过厚 | 拆为会话编排层，不再承载全部规则 |

建议的会话拆分：

```kotlin
data class RunState(...)

interface TurnEngine {
    fun advance(state: RunState): TurnStepResult
}

interface ActionResolver {
    fun resolve(state: RunState, command: Command): ActionResolution
}

interface EventRecorder {
    fun record(resolution: ActionResolution)
}

interface SnapshotAssembler {
    fun assemble(state: RunState): RenderSnapshot
}

interface SaveFacade {
    fun save(state: RunState): SaveResult
    fun load(): RunState?
}
```

---

## 5. 序列化、事件、日志与回放基础合同

虽然用户本轮重点点名了战斗、职业、元素、技能、AI、状态、资源，但这些系统要能落地，必须先补齐以下基础合同。

### 5.1 序列化引擎

从 `P2-A` 开始，核心序列化引擎固定为 `kotlinx.serialization`。

原因：

1. 编译期生成代码，便于长期维护。
2. 多态建模更稳定。
3. 更适合后续 `SaveSnapshot V3+`、event log、replay 和 content pack schema。

建议：

```kotlin
@Serializable
sealed interface SerializableComponent

@Serializable
@SerialName("resource_pool")
data class ResourcePoolState(...)

@Serializable
@SerialName("status_instance")
data class StatusInstance(...)
```

### 5.2 事件总线与日志 token

所有战斗、状态、移动、经验、掉落、交互事件都走统一事件结构：

```kotlin
@Serializable
sealed interface GameEvent {
    val turnId: Long
    val actionId: Long
    val sequence: Int
    val phase: EventPhase
    val actorId: Int?
    val cause: EventCause
}

@Serializable
data class LogTokenEvent(
    val eventId: String,
    val localeKey: String,
    val args: Map<String, String>,
    val category: LogCategory,
)
```

冻结规则：

1. `core` 只产生事件和 token，不拼最终文案。
2. `client` 负责把 token 渲染成中文或英文日志。
3. 存档保存最近 N 条 token event，而不是已渲染文本。
4. replay 也基于事件与输入序列重建，而不是保存完整帧。

### 5.3 回放与 trace

必须同时存在两种 trace：

1. `CombatTrace`
   - 面向规则与平衡验证
2. `AIDecisionTrace`
   - 面向怪物决策与 Boss 脚本验证

不在 `Phase 2` 就做图形化回放播放器，但从 `P2-A` 起必须把 trace schema 定下来。

---

## 6. 回合与行动经济系统

### 6.1 设计目标

1. 保留当前能量制的确定性优势。
2. 支持快行动、慢行动、瞬发技能、Boss 多段动作。
3. 支持日志、回放、AI 和 telegraph 共享同一时间轴。

### 6.2 核心合同

#### 标准阈值

```kotlin
const val ACTION_THRESHOLD = 1000
```

#### 速度口径

内容层继续保留“100 为标准速度”的人类可读语义，内部统一乘以 10：

```kotlin
effectiveSpeedPerTick = displayedSpeed * 10
```

这样可以最大程度平滑迁移 Phase 1 的数值和怪物模板。

#### 行动成本

```kotlin
enum class ActionCost(val energy: Int) {
    FREE(0),
    FAST(750),
    STANDARD(1000),
    SLOW(1250),
    VERY_SLOW(1500),
}
```

所有技能、物品使用、交互都必须显式声明行动成本。

#### 排序规则

不再引入未定义的 `initiative` 字段。

统一排序为：

1. `energy desc`
2. `entityId asc`

说明：

1. `entityId` 作为稳定次级键，足够保证跨运行一致性。
2. 高层“抢先”效果通过直接修改 `energy`、`speed` 或 `actionCost` 表达，不单独造一个 `initiative` 概念。

### 6.3 回合时序

固定时序如下：

1. `TurnStart`
   - tick 冷却
   - start-of-turn status
   - 资源回复/衰减
2. `ActionValidate`
   - 活体检查
   - 资源检查
   - 控制状态检查
   - 目标/LoS/路径检查
3. `ActionCommit`
   - 扣资源
   - 上冷却
   - 写入 actionId
4. `MovementPhase`
   - 位移
   - 冲锋
   - 闪现
   - 击退
5. `HitPhase`
   - 命中/闪避/格挡/招架
6. `DamagePhase`
   - 基础伤害
   - 暴击
   - 穿透
   - 护甲/抗性
   - 护盾/结界
   - 生命或资源扣减
7. `OnHitPhase`
   - 附加状态
   - 资源吸取
   - 反击
   - 溅射
8. `DeathPhase`
   - 死亡
   - 击杀奖励
   - 经验
   - 掉落
9. `TurnEnd`
   - end-of-turn status
   - 过期清理
   - event flush

### 6.4 迁移策略

`Phase 2` 不立刻改变所有技能和怪物的行为，只做：

1. 调度阈值从 `100` 升到 `1000`
2. 当前 `speed` 全部按 `*10` 映射
3. 当前所有 Phase 1 行动默认 `STANDARD`
4. 新 talent 才开始显式区分快慢行动

---

## 7. 战斗系统详细设计

### 7.1 设计目标

1. 保持当前 deterministic test 能力。
2. 支持元素伤害、状态、护盾、抗性、穿透、反击、DOT。
3. 能输出可解释的 `CombatTrace`。
4. 不在 `Phase 2` 一次性做完所有高级数值，但架构必须一次到位。

### 7.2 数据结构

```kotlin
enum class HitModel {
    AUTO_HIT,
    ACCURACY_VS_DEFENSE,
    POWER_VS_SAVE,
}

data class DamagePacket(
    val type: DamageType,
    val baseAmount: Int,
    val sourcePower: PowerType,
    val tags: Set<DamageTag> = emptySet(),
)

data class DamageRequest(
    val attacker: EntityId,
    val target: EntityId,
    val actionId: Long,
    val hitModel: HitModel,
    val packets: List<DamagePacket>,
    val critEligible: Boolean = true,
    val sourceTags: Set<ActionTag> = emptySet(),
)

data class DamageOutcome(
    val hit: Boolean,
    val crit: Boolean,
    val appliedPackets: List<AppliedDamagePacket>,
    val totalDamage: Int,
    val targetKilled: Boolean,
    val appliedStatuses: List<String> = emptyList(),
)

data class AppliedDamagePacket(
    val type: DamageType,
    val originalAmount: Int,
    val afterCrit: Int,
    val afterPenetration: Int,
    val afterMitigation: Int,
    val absorbedByShield: Int,
    val finalApplied: Int,
)
```

### 7.3 基础派生属性

#### 继续保留的生存/输出字段

1. `maxHp`
2. `armor`
3. `defense`
4. `accuracy`
5. `critChance`
6. `critMultiplier`
7. `physicalSave`
8. `spellSave`
9. `mindSave`
10. `meleePower`
11. `spellPower`
12. `mindPower`

#### 推荐的 4 属性派生公式

```text
meleePower    = baseMeleePower + STR * 2 + DEX / 2 + bonuses
spellPower    = baseSpellPower + WIL * 2 + bonuses
mindPower     = baseMindPower + DEX + WIL + bonuses

accuracy      = baseAccuracy + DEX * 2 + bonuses
defense       = baseDefense + DEX + bonuses
armor         = baseArmor + CON / 2 + gearArmor + bonuses

physicalSave  = basePhysicalSave + CON + STR / 2 + bonuses
spellSave     = baseSpellSave + WIL + CON / 2 + bonuses
mindSave      = baseMindSave + WIL + DEX / 2 + bonuses

maxHp         = baseHp + CON * 8 + STR * 2 + bonuses
maxStamina    = baseStamina + WIL * 4 + CON * 2 + bonuses
maxMana       = baseMana + WIL * 6 + bonuses
```

说明：

1. `Attack` 字段在 `Phase 2` 迁移阶段等价映射为 `meleePower`。
2. `Evasion` 在 `Phase 2` 迁移阶段等价并入 `defense`。
3. `talentPower` 废弃为统一的 `spellPower`/`mindPower`/树系系数。

### 7.4 命中公式

固定采用“有边际递减的差值函数”，避免后期完全堆满。

```text
normalizedDelta(delta, scale) = delta / (abs(delta) + scale)

hitChance =
  clamp(
    0.10,
    0.95,
    0.55 + normalizedDelta(accuracy - defense, 35) * 0.35
  )
```

说明：

1. `ACCURACY_VS_DEFENSE` 用于近战、投射物和部分可闪避技能。
2. `POWER_VS_SAVE` 不决定命中本体，而决定附加状态或特殊效果是否成立。

### 7.5 暴击公式

```text
normalizedPositive(value, scale) = value / (value + scale)

critChance =
  clamp(
    0.00,
    0.60,
    baseCritChance + normalizedPositive(critRating, 60) * 0.35 - critAvoidance
  )

critMultiplier =
  1.50 + critPowerBonus
```

### 7.6 Power/Save 对抗体系

`Power/Save` 是命中之后的第二层检定，只负责判断附加状态、控制和特殊效果是否成立，不回头修改这次攻击是否命中。

#### 三个对抗维度

| 维度 | 攻击方属性 | 防御方属性 | 主要用途 |
| --- | --- | --- | --- |
| `PHYSICAL` | `physicalPower` | `physicalSave` | 眩晕、击退、流血、破甲 |
| `MENTAL` | `mentalPower` | `mentalSave` | 恐惧、混乱、沉默、士气打击 |
| `SPELL` | `spellPower` | `spellSave` | 燃烧、冻结、诅咒、净化、元素控制 |

#### 派生公式

```text
physicalPower = 10 + STR * 1.5 + level * 0.5 + bonus
physicalSave  = 10 + CON * 1.5 + level * 0.5 + bonus

mentalPower   = 10 + WIL * 1.5 + level * 0.5 + bonus
mentalSave    = 10 + WIL * 1.0 + CON * 0.5 + level * 0.5 + bonus

spellPower    = 10 + WIL * 1.5 + level * 0.5 + bonus
spellSave     = 10 + WIL * 1.0 + CON * 0.5 + level * 0.5 + bonus
```

冻结规则：

1. 保持当前 `STR / DEX / CON / WIL` 四主属性，不新增第五主属性。
2. 怪物可在模板中直接覆盖 `Power/Save`，不强制通过玩家属性公式推导。
3. `DEX` 继续主要服务于命中、闪避、先手和机动，不进入 `Power/Save` 主公式。

#### 施加率公式

```text
d = power - save
sigmoid(d) = 1 / (1 + e^(-0.05 * d))

applyChance =
  clamp(
    0.10,
    0.90,
    0.10 + 0.80 * sigmoid(d)
  )
```

解释：

1. `power == save` 时，施加率约为 `50%`。
2. 下限 `10%`，保留逆风命中的可能性。
3. 上限 `90%`，避免控制链绝对锁死。

#### 状态到对抗维度的映射

| 状态/效果 | 维度 | 说明 |
| --- | --- | --- |
| `STUN` `KNOCKBACK` `BLEED` `ARMOR_BREAK` | `PHYSICAL` | 武器、盾击、冲锋、物理控制 |
| `SILENCE` `CONFUSE` `FEAR` `WEAKEN` | `MENTAL` | 精神冲击、暗影恐惧、心智扰乱 |
| `BURN` `SLOW` `FREEZE` `CURSE` `BANE` | `SPELL` | 元素、神圣、暗影法术效果 |
| `ROOT` `BLIND` | 由技能显式声明 | 物理来源走 `PHYSICAL`，法术来源走 `SPELL` |

#### 数据结构

```kotlin
enum class SaveDimension {
    PHYSICAL,
    MENTAL,
    SPELL,
}

data class PowerSaveStats(
    val physicalPower: Int = 10,
    val physicalSave: Int = 10,
    val mentalPower: Int = 10,
    val mentalSave: Int = 10,
    val spellPower: Int = 10,
    val spellSave: Int = 10,
)
```

冻结规则：

1. 技能定义必须显式声明 `saveDimension`，不能靠通道名隐式推断。
2. 伤害通道与 `saveDimension` 允许不同，但必须在技能 schema 里显式标明。
3. `CombatTrace` 必须记录 `power`、`save`、`applyChance` 和最终 roll。

### 7.7 收益递减模型

收益递减只作用于二级属性，不作用于四大主属性。

#### 通用模型

```text
effective = raw * C / (raw + C)
```

其中 `C` 是半收益常数，`raw == C` 时有效值为 `C / 2`。

#### 首批启用点

| 属性 | `C` | 说明 |
| --- | --- | --- |
| `evasion` | `150` | 防止高闪避构筑过早极化 |
| `critRating` | `200` | 限制暴击率膨胀 |
| `hpRegenRating` | `80` | 防止回复流破坏消耗战 |
| `castSpeed` | `100` | `Phase 4` 再正式启用 |

#### 集成规则

1. `accuracy` 不做收益递减，允许进攻构筑专门反制高闪避。
2. `armor / (armor + K)` 本身已是双曲线，不再二次套 DR。
3. 元素抗性靠硬上限和穿透下限控制，不再额外套 DR。
4. Tooltip 展示原始值和有效值，避免玩家误判构筑收益。

#### 推荐工具函数

```kotlin
object DiminishingReturns {
    fun hyperbolic(rawValue: Double, halfValueConstant: Double): Double {
        require(halfValueConstant > 0) { "halfValueConstant must be positive" }
        if (rawValue <= 0.0) return 0.0
        return rawValue * halfValueConstant / (rawValue + halfValueConstant)
    }
}
```

### 7.8 护甲、抗性与穿透

#### 物理伤害

```text
effectiveArmor = max(0, armor - physicalPenetration)
physicalMitigation = clamp(0.00, 0.70, effectiveArmor / (effectiveArmor + 100.0))
```

#### 元素伤害

```text
effectiveResist =
  clamp(-50, 75, resistance[type] - penetration[type] + vulnerability[type])

elementMultiplier = 1.0 - effectiveResist / 100.0
```

说明：

1. 允许抗性为负，表示易伤。
2. 默认抗性上限 `75`。
3. `penetration` 不可把抗性压到低于 `-50`。

### 7.9 护盾层级

护盾/结界结算顺序冻结为：

1. 类型护盾
   - 例如 `FIRE_WARD`
2. 通用护盾
   - 例如 `ARCANE_SHIELD`
3. 生命值

原因：

1. 类型护盾的价值来自“正确应对正确类型”。
2. 通用护盾不能完全压制类型护盾的存在意义。

### 7.10 CombatTrace

所有核心战斗必须可导出 trace：

```kotlin
data class CombatTrace(
    val turnId: Long,
    val actionId: Long,
    val traceId: Long,
    val parentTraceId: Long? = null,
    val attacker: Int,
    val target: Int,
    val sourceAbilityId: String,
    val stages: List<CombatTraceStage>,
    val childTraceIds: List<Long> = emptyList(),
)

data class CombatTraceStage(
    val stage: CombatStage,
    val values: Map<String, String>,
)
```

补充语义：

1. `parentTraceId` 用于火焰蒸发、连锁闪电、反击等次生效果。
2. `sourceAbilityId` 必须能定位到技能、武器 proc、地形 hazard 或状态 tick。
3. `PENETRATION` 阶段必须记录穿透前后防御值与 `damageDeltaFromPenetration`。
4. `STATUS_APPLY` 阶段必须记录 `saveDimension`、`power`、`save`、`applyChance`、`roll`。
5. 回调触发的额外伤害必须保留 `callbackSource` 或等价字段，避免 trace 失去来源。

固定阶段：

1. `VALIDATE`
2. `HIT_ROLL`
3. `CRIT_ROLL`
4. `PACKET_BUILD`
5. `PENETRATION`
6. `MITIGATION`
7. `SHIELD_ABSORB`
8. `HP_APPLY`
9. `STATUS_APPLY`
10. `DEATH_CHECK`

### 7.11 Phase 2 与 Phase 3 的分工

#### `Phase 2`

1. 引入 `DamageType` 和 `DamagePacket`，但仍可先保留较简单的 damage amount 计算。
2. 先让所有现有攻击走 `PHYSICAL`。
3. 先让现有技能通过 `DamageRequest` 走统一管线。

#### `Phase 3`

1. 冻结完整命中、暴击、Power/Save、抗性、穿透和 CombatTrace 公式。
2. 所有职业技能、Boss 技能和地表危险区必须进入正式 damage pipeline。

---

## 8. 元素系统详细设计

### 8.1 六通道定义

| 通道 | 主用途 | 代表状态/反馈 |
| --- | --- | --- |
| `PHYSICAL` | 武器、冲锋、投射物、流血 | 破甲、流血、击退、眩晕 |
| `FIRE` | 燃烧、爆裂、火焰地面 | 燃烧、火焰易伤 |
| `COLD` | 冰枪、冻结区、减速 | 冷却、迟缓、冻结 |
| `LIGHTNING` | 连锁电击、位移惩罚 | 感电、麻痹、连锁 |
| `HOLY` | 审判、净化、圣域 | 净化、驱散、圣光灼烧 |
| `SHADOW` | 诅咒、吸取、夜幕 | 虚弱、致盲、腐蚀、暗影印记 |

### 8.2 通道与状态的关系

冻结规则：

1. 伤害通道不等于状态名字。
2. 一个状态可以默认对应某个通道，但仍然独立建模。
3. 例外：
   - `BLEED` 是状态，DOT 通常走 `PHYSICAL`
   - `POISON` 是状态，DOT 默认走 `PHYSICAL`，部分暗影系技能可改成 `SHADOW`

### 8.3 抗性、穿透、易伤

所有单位默认具备：

```kotlin
data class ResistanceProfile(
    val resistance: Map<DamageType, Int>,
    val penetration: Map<DamageType, Int>,
    val vulnerability: Map<DamageType, Int>,
)
```

冻结规则：

1. 抗性是目标属性。
2. 穿透是攻击方属性。
3. 易伤是目标状态或地图效果带来的修正。
4. 先穿透，后易伤，最后 clamp。

来源叠加顺序：

1. 种族/怪物模板基础抗性
2. 装备与常驻被动
3. 临时 buff / debuff
4. 地形与 zone modifier
5. 本次攻击附带的 penetration
6. 本次结算临时 vulnerability

约束：

1. 叠加顺序必须进入 `CombatTrace` 或等价调试视图。
2. `allElementalResistance` / `allElementalPenetration` 只能在内容层出现，进入结算前必须展开。
3. `vulnerability` 不单独建第七类属性，继续作为 `ResistanceProfile` 的第三轴。

### 8.4 DamageTypeConfig 与命名规范

所有伤害通道都必须有统一 registry，避免规则、schema、日志和数值层各写一套硬编码。

```kotlin
data class DamageTypeConfig(
    val type: DamageType,
    val nameKey: String,
    val resistanceStatKey: String,
    val penetrationStatKey: String,
    val resistanceCap: Int = 75,
    val defaultAssociatedEffects: List<String> = emptyList(),
)
```

命名规则：

1. 元素抗性统一为 `{element}Resistance`。
2. 元素穿透统一为 `{element}Penetration`。
3. 物理例外保留 `armor` 与 `armorPenetration`。
4. 允许 `allElementalResistance` / `allElementalPenetration` 作为内容层便捷字段，但运行时必须展开为 5 个元素通道。

配置示例：

```yaml
damageTypes:
  - type: PHYSICAL
    nameKey: damage_type.physical
    resistanceStatKey: armor
    penetrationStatKey: armorPenetration
    resistanceCap: -1
    defaultAssociatedEffects: [BLEED, ARMOR_BREAK, STUN]

  - type: FIRE
    nameKey: damage_type.fire
    resistanceStatKey: fireResistance
    penetrationStatKey: firePenetration
    resistanceCap: 75
    defaultAssociatedEffects: [BURN, EXPOSE_FIRE]

  - type: COLD
    nameKey: damage_type.cold
    resistanceStatKey: coldResistance
    penetrationStatKey: coldPenetration
    resistanceCap: 75
    defaultAssociatedEffects: [SLOW, FREEZE]

  - type: LIGHTNING
    nameKey: damage_type.lightning
    resistanceStatKey: lightningResistance
    penetrationStatKey: lightningPenetration
    resistanceCap: 75
    defaultAssociatedEffects: [SHOCKED]

  - type: HOLY
    nameKey: damage_type.holy
    resistanceStatKey: holyResistance
    penetrationStatKey: holyPenetration
    resistanceCap: 75
    defaultAssociatedEffects: [BANE]

  - type: SHADOW
    nameKey: damage_type.shadow
    resistanceStatKey: shadowResistance
    penetrationStatKey: shadowPenetration
    resistanceCap: 75
    defaultAssociatedEffects: [CURSED, SHADOWMARK]
```

冻结规则：

1. `Phase 2 ~ Phase 5` 内不再新增第七伤害通道。
2. `DamageTypeConfig` 是伤害语义真源，视觉图标和颜色映射在 `client/content` 层派生，不反向污染 `core`。
3. `defaultAssociatedEffects` 只做默认提示，不替代技能显式定义。

### 8.5 元素交互规则

元素交互用于提供战术层，而不是替代基础伤害公式。所有交互必须满足“条件清楚、结果确定、trace 可见”。

#### 首批交互矩阵

| 触发元素 | 条件 | 结果 | 引入阶段 |
| --- | --- | --- | --- |
| `FIRE` | 目标有 `FREEZE` | 解除 `FREEZE`，触发 `蒸发`，额外增伤 `30%` | `P3-A` |
| `COLD` | 目标有 `BURN` | 解除 `BURN`，施加 `SLOW(2)` | `P3-A` |
| `HOLY` | 目标标签含 `UNDEAD` 或 `DEMON` | 本次伤害额外乘算 `1.5x` | `P3-A` |
| `SHADOW` | 目标有 `BANE` | 结算一次额外暗影伤害并消耗 `BANE` | `P3-B` |
| `HOLY` / `SHADOW` | 同回合互相命中同一目标 | `BANE` 与 `CURSE` 优先互耗 | `P3-B` |
| `LIGHTNING` | 目标站在 `WATER` 地形 | 伤害 `+25%` 并向相邻水格传导半伤 | `P4-A` |
| `FIRE` | 目标站在 `OIL` 地形 | 点燃油面 `3` 回合 | `P4-A` |
| `COLD` | 目标站在 `WATER` 地形 | 冻结水面为 `ICE` `3` 回合 | `P4-A` |

#### 实现约束

1. 交互判定发生在主伤害完成后，再生成次生效果。
2. 次生伤害必须走新的 `CombatPipeline`，并记录父子 trace 关系。
3. 交互链最大深度固定为 `2`，防止无限连锁。
4. 交互默认不额外掷骰；条件满足即稳定触发。

#### 数据结构

```kotlin
data class ElementInteractionRule(
    val id: String,
    val triggerElement: DamageType,
    val conditionType: InteractionCondition,
    val conditionParam: String,
    val effectType: InteractionEffect,
    val effectParams: Map<String, String>,
)

enum class InteractionCondition {
    TARGET_HAS_STATUS,
    TARGET_ON_TERRAIN,
    TARGET_HAS_TAG,
    TARGET_TOOK_ELEMENT_THIS_TURN,
}

enum class InteractionEffect {
    REMOVE_STATUS_AND_TRIGGER,
    DAMAGE_MULTIPLIER,
    TERRAIN_TRANSFORM,
    CHAIN_DAMAGE,
    MUTUAL_CONSUME,
}
```

配置示例：

```yaml
interactions:
  - id: fire_vs_freeze
    triggerElement: FIRE
    conditionType: TARGET_HAS_STATUS
    conditionParam: FREEZE
    effectType: REMOVE_STATUS_AND_TRIGGER
    effectParams:
      removeStatus: FREEZE
      bonusDamageMultiplier: "1.30"
      logKey: combat.interaction.evaporate

  - id: lightning_on_water
    triggerElement: LIGHTNING
    conditionType: TARGET_ON_TERRAIN
    conditionParam: WATER
    effectType: CHAIN_DAMAGE
    effectParams:
      damageMultiplier: "1.25"
      splashMultiplier: "0.50"
      maxDistance: "1"
      logKey: combat.interaction.conductive_water
```

### 8.6 UI 与日志约束

1. 技能描述必须明确写出主要伤害通道。
2. 抗性面板只展示 6 通道，不展示额外隐藏元素。
3. 日志 token 不直接拼元素文字，而是通过 key 渲染。
4. 元素交互必须有独立 `logKey`，不能只靠数值变化让玩家自己猜。
5. Tooltip 需要区分“基础伤害类型”和“可能触发的交互”。

### 8.7 元素系统分阶段引入计划

#### `Phase 2`

1. 所有现有攻击统一先走 `PHYSICAL`。
2. 建立 `DamageTypeConfig`、`ResistanceProfile`、日志和 i18n key 骨架。
3. 不启用正式元素交互，只保留字段和 registry。

#### `Phase 3`

1. 正式启用 6 通道、抗性、穿透、易伤和 `Power/Save` 联动。
2. 落地 `FIRE/COLD/HOLY/SHADOW` 的首批交互。
3. Boss 技能和职业核心技能必须进入元素主链。

#### `Phase 4`

1. 启用地形联动交互，如 `WATER / OIL / ICE`。
2. 让环境 hazard、elite affix、artifact proc 接入元素交互 registry。
3. 对多 seed 和多职业组合做交互覆盖验证。

---

## 9. 资源系统详细设计

### 9.1 设计目标

1. 支持多职业资源，但不让每个职业都自造一套恢复逻辑。
2. 同时兼容：
   - 条形资源
   - 堆叠资源
   - 战斗中积累、脱战衰减型资源
3. 资源变化必须进入日志、AI、存档和 UI。

### 9.2 通用数据结构

```kotlin
enum class ResourceType {
    HEALTH,
    STAMINA,
    MANA,
    POSITIVE,
    RAGE,
    MOMENTUM,
    ARCANE_CHARGE,
    SHADOW,
    FOCUS,
}

enum class ResourceDisplayMode {
    BAR,
    PIPS,
}

data class ResourcePoolDef(
    val type: ResourceType,
    val minValue: Int = 0,
    val maxValue: Int,
    val displayMode: ResourceDisplayMode,
    val regenPolicy: RegenPolicy,
    val decayPolicy: DecayPolicy? = null,
)

data class ResourcePoolState(
    val type: ResourceType,
    var current: Int,
    var max: Int,
)
```

### 9.3 回复与衰减策略

```kotlin
sealed interface RegenPolicy {
    data class PassivePerTurn(val amount: Int) : RegenPolicy
    data class OnBasicAttack(val amount: Int) : RegenPolicy
    data class OnKill(val amount: Int) : RegenPolicy
    data class OnDamageTaken(val amount: Int) : RegenPolicy
    data object None : RegenPolicy
}

sealed interface DecayPolicy {
    data class TowardZeroOutOfCombat(val amountPerTurn: Int) : DecayPolicy
    data class ResetOutOfCombat(val delayTurns: Int) : DecayPolicy
    data object None : DecayPolicy
}
```

### 9.4 各职业资源分配

| 职业 | 主资源 | 次资源/堆叠 | 说明 |
| --- | --- | --- | --- |
| Vanguard | `STAMINA` | 无 | 传统武技和盾姿态 |
| Arcanist | `MANA` | 无 | 稳定法力池 |
| Rogue | `STAMINA` | `MOMENTUM` | 机动与终结技 |
| Templar | `POSITIVE` | 无 | 战斗中累积，脱战回零 |
| Berserker | `RAGE` | 无 | 受伤和杀敌增长，脱战衰减 |
| Spellblade | `MANA` | `ARCANE_CHARGE` | 技能循环驱动爆发窗口 |
| Shadowblade | `STAMINA` | `SHADOW` | 潜伏与暗影位移消耗 |
| Warden | `FOCUS` | `STAMINA` | 远近切换与印记联动 |

### 9.5 资源规则约束

1. `HEALTH` 仍然特殊，不走普通消耗语义。
2. 除 `HEALTH` 以外，任何资源都必须能通过 `ResourceSpent/ResourceGained/ResourceDecayed` 事件追踪。
3. 一个职业最多同时管理两个资源轴。
4. 不在 `Phase 2 ~ Phase 5` 内引入第三条职业资源轴。

### 9.6 Phase 2 与 Phase 3 的分工

#### `Phase 2`

1. 落地通用资源池。
2. 先启用 `STAMINA / MANA / POSITIVE`。
3. 让现有 `Stamina` 迁移到 `ResourcePoolState(STAMINA)`。

#### `Phase 3`

1. 扩到 `RAGE / MOMENTUM / ARCANE_CHARGE / SHADOW / FOCUS`。
2. 为职业 UI 和 AI 暴露资源 hint。

---

## 10. 状态效果系统详细设计

### 10.1 设计目标

1. 把当前 4 个效果扩展成通用状态框架。
2. 不再用技能名直接当状态类型。
3. 明确持续时间、堆叠、驱散、免疫、tick 时机。

### 10.2 状态注册表模型

```kotlin
enum class StatusCategory {
    CONTROL,
    DAMAGE_OVER_TIME,
    BUFF,
    DEBUFF,
    SHIELD,
    STANCE,
    MARK,
    ZONE,
}

enum class StackPolicy {
    REPLACE,
    REFRESH_DURATION,
    STACK_COUNT,
    STACK_POTENCY,
    UNIQUE_PER_SOURCE,
}

enum class DurationClock {
    START_OF_TURN,
    END_OF_TURN,
}

data class StatusEffectDef(
    val id: String,
    val category: StatusCategory,
    val family: String,
    val defaultDuration: Int,
    val durationClock: DurationClock,
    val stackPolicy: StackPolicy,
    val maxStacks: Int = 1,
    val dispelTags: Set<String> = emptySet(),
    val immunityTags: Set<String> = emptySet(),
    val statModifiers: CombatModifierBundle = CombatModifierBundle(),
    val periodicEffects: List<StatusPeriodicEffect> = emptyList(),
)

data class StatusInstance(
    val id: String,
    val source: EntityId?,
    var remainingTurns: Int,
    var stacks: Int = 1,
    var potency: Double = 1.0,
    val appliedAtTurn: Long,
)
```

### 10.3 推荐的 v1 状态分类

#### 控制类

1. `STUN`
2. `DAZE`
3. `ROOT`
4. `SILENCE`
5. `BLIND`
6. `CONFUSE`
7. `SLOW`

#### DOT 类

1. `BLEED`
2. `BURN`
3. `POISON`
4. `SHOCKED`

#### 增益/减益类

1. `ARMOR_BREAK`
2. `WEAKEN`
3. `HASTE`
4. `REGEN`
5. `GUARD`
6. `EXPOSE_FIRE`
7. `EXPOSE_COLD`
8. `EXPOSE_LIGHTNING`
9. `VULNERABLE_HOLY`
10. `CURSED`

#### 护盾/姿态类

1. `ARCANE_SHIELD`
2. `FIRE_WARD`
3. `CONSECRATION`
4. `STEALTH`
5. `BERSERK`

#### 标记类

1. `MARKED`
2. `JUDGED`
3. `SHADOWMARK`

### 10.4 生命周期顺序

固定顺序：

1. `OnApply`
2. `OnRefresh`
3. `TurnStartTick`
4. `PreActionGate`
5. `PreDamageTaken`
6. `PostDamageDealt`
7. `TurnEndTick`
8. `OnExpire`

### 10.5 堆叠规则

| 类别 | 默认策略 | 说明 |
| --- | --- | --- |
| 控制 | `REPLACE` 或 `REFRESH_DURATION` | 不允许无限叠加硬控 |
| DOT | `STACK_POTENCY` 或 `UNIQUE_PER_SOURCE` | 允许多个来源叠加 |
| 通用 buff | `REFRESH_DURATION` | 避免重复刷爆面板 |
| 护盾 | `STACK_COUNT` 或数值合并 | 需限制总量 |
| 姿态 | `REPLACE` | 同组姿态互斥 |
| 标记 | `UNIQUE_PER_SOURCE` | 便于刺客/猎手类机制 |

### 10.6 驱散与免疫

所有状态必须声明：

1. `dispelTags`
2. `immunityTags`

例子：

1. `SILENCE`
   - `dispelTags = [MENTAL, HOLY]`
2. `BURN`
   - `dispelTags = [SPELL, HOLY]`
3. `BLEED`
   - `dispelTags = [PHYSICAL, HOLY]`

冻结约束：

1. `cleanse` 只移除符合 tag 的状态。
2. 免疫不是简单无敌，默认只减少 duration 或直接拒绝 apply。
3. `Boss` 不允许完全免疫所有控制，必须用：
   - duration reduction
   - category-specific immunity
   - phase exception

### 10.7 当前状态系统的迁移要求

1. `WAR_CRY_BUFF` 和 `WAR_CRY_DEBUFF` 必须被替换为通用状态 id，例如 `battle_rally`、`shaken`。
2. `skipNextDecay` 必须删除，改为：
   - `durationClock`
   - `applyPhase`
3. 状态衰减逻辑必须回到 `core`。

---

## 11. 技能与天赋系统详细设计

### 11.1 设计目标

1. 从 4 个战士技能扩到多职业、多类型技能。
2. 保持数据驱动，但不引入自由脚本。
3. 把 targeting、telegraph、cooldown、resource、AI hint 全部纳入正式 schema。

### 11.2 数据结构

```kotlin
enum class TalentKind {
    ACTIVE,
    PASSIVE,
    SUSTAIN,
    TRIGGERED,
}

enum class TargetMode {
    SELF,
    ACTOR,
    TILE,
    LINE,
    CONE,
    RADIUS,
    WALL,
    SUMMON_SLOT,
}

data class TalentDef(
    val id: String,
    val treeId: String,
    val kind: TalentKind,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val visualKey: String,
    val audioProfile: String,
    val maxLevel: Int,
    val actionCost: ActionCost,
    val cooldown: CooldownDef,
    val resourceCosts: List<ResourceCost>,
    val targeting: TalentTargeting,
    val telegraph: TelegraphDef?,
    val keywords: List<String>,
    val aiHints: TalentAiHints,
    val ranks: Map<Int, TalentRankDef>,
)

data class TalentRankDef(
    val rank: Int,
    val effects: List<TalentEffectOp>,
    val breakpoints: List<TalentBreakpoint> = emptyList(),
)
```

### 11.3 目标选择模型

#### 允许的 targeting

| 类型 | 用途 |
| --- | --- |
| `SELF` | 自身姿态、护盾、恢复 |
| `ACTOR` | 单体敌人或盟友目标 |
| `TILE` | 指定地块 |
| `LINE` | 光束、冲锋、投射轨迹 |
| `CONE` | 扇形战技 |
| `RADIUS` | 爆炸、地面区域 |
| `WALL` | 召唤墙、障碍 |
| `SUMMON_SLOT` | 召唤类 |

冻结规则：

1. Phase 2~5 不支持任意自由形状。
2. 所有 targeting 都必须能映射到 Tile telegraph。
3. 所有 targeting 都必须能被 AI 推理和测试。

### 11.4 Telegraph 语法

```kotlin
enum class TelegraphSeverity {
    INFO,
    WARNING,
    DANGER,
}

data class TelegraphDef(
    val shape: TargetMode,
    val leadTurns: Int,
    val severity: TelegraphSeverity,
    val indicatorKey: String,
    val logKey: String,
    val audioCue: String?,
    val counterplayTags: List<String>,
)
```

冻结规则：

1. 大伤害技能必须有 telegraph。
2. telegraph 至少包含：
   - 图形提示
   - 日志提示
   - 可选音频提示
3. `Phase 3` 起 Boss 大技能必须使用四段式：
   - 预警
   - 起手
   - 爆发
   - 残留

### 11.5 技能效果操作集

Phase 2~5 只允许在 typed executor 中实现以下 effect op：

1. `DEAL_DAMAGE`
2. `APPLY_STATUS`
3. `REMOVE_STATUS`
4. `MOVE_SELF`
5. `MOVE_TARGET`
6. `GAIN_RESOURCE`
7. `SPEND_RESOURCE`
8. `GAIN_SHIELD`
9. `CLEANSE`
10. `DISPEL`
11. `SPAWN_ZONE`
12. `SUMMON_ACTOR`
13. `MODIFY_COOLDOWN`
14. `CHAIN_TO_TARGETS`

不允许在 `Phase 2 ~ Phase 5` 内引入“任意表达式脚本”作为技能效果源。

### 11.6 技能描述模板

技能说明必须全部使用模板渲染：

```yaml
descTemplateKey: talent.arcanist.fire_burst.desc
templateArgs:
  damage: { from: effect, field: primaryDamage }
  radius: { from: targeting, field: radius }
  cooldown: { from: talent, field: cooldown }
```

冻结规则：

1. 描述永远从数据和公式读值。
2. 不能在 YAML 中把最终数值写死两遍。

### 11.7 当前天赋系统的迁移要求

1. `TalentLevelEffect` 不再继续扩字段。
2. 当前 4 个战士技能必须迁移成 V2 schema 的首批样板。
3. `TalentResolver` 改为：
   - 解析
   - 校验
   - dispatch 到 typed executor

---

## 12. 职业系统详细设计

### 12.1 设计目标

1. 让职业成为正式内容对象，而不是一组硬编码天赋。
2. 从第一天满足单通合同。
3. 在不增加主属性维度的前提下，靠资源、树系、状态、telegraph 和构筑路线做出职业区分。

### 12.2 职业 schema

```kotlin
enum class ProfessionTier {
    BASIC,
    ADVANCED,
}

data class ProfessionDef(
    val id: String,
    val tier: ProfessionTier,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val visualKey: String,
    val audioProfile: String,
    val primaryStats: List<String>,
    val startingResources: List<ResourcePoolDef>,
    val startingKit: StartingKitDef,
    val talentTrees: List<TalentTreeRef>,
    val soloContract: SoloContractDef,
)
```

### 12.3 单通合同字段

```kotlin
data class SoloContractDef(
    val offenseTags: List<String>,
    val defenseTags: List<String>,
    val mobilityTags: List<String>,
    val aoeAnswerTags: List<String>,
    val bossAnswerTags: List<String>,
    val panicAnswerTags: List<String>,
)
```

### 12.4 4 个基础职业

#### Vanguard

| 项 | 设计 |
| --- | --- |
| 核心定位 | 近战前压、盾姿态、破甲与线性推进 |
| 主资源 | `STAMINA` |
| 主通道 | `PHYSICAL` |
| 对抗通道 | `PHYSICAL` |
| 防御机制 | 护甲、格挡、Guard 状态 |
| 位移 | 冲锋、突进、推线 |
| panic answer | `Last Bastion` / `Unstoppable March` |

树系：

1. `Steel Discipline`
   - `Power Strike`
   - `Sundering Blow`
   - `Executioner's Arc`
   - `Steel Tempest`
2. `Bulwark`
   - `Shield Bash`
   - `Guard Stance`
   - `Brace`
   - `Last Bastion`
3. `Warcries`
   - `War Cry`
   - `Challenge`
   - `Rally`
   - `Terror Shout`
4. `Tactics`
   - `Charge`
   - `Intercept`
   - `Formation Break`
   - `Unstoppable March`

#### Arcanist

| 项 | 设计 |
| --- | --- |
| 核心定位 | 元素法术、区域控制、护盾与 Blink 自保 |
| 主资源 | `MANA` |
| 主通道 | `FIRE / COLD / LIGHTNING` |
| 对抗通道 | `SPELL` |
| 防御机制 | Arcane Shield、Blink、减速控制 |
| 位移 | `Blink` / `Storm Step` |
| panic answer | `Mirror Ward` / `Arcane Recovery` |

树系：

1. `Pyromancy`
   - `Fire Bolt`
   - `Flame Burst`
   - `Burning Ground`
   - `Meteor Drop`
2. `Cryomancy`
   - `Frost Lance`
   - `Ice Nova`
   - `Frozen Ground`
   - `Absolute Zero`
3. `Stormcraft`
   - `Spark Jolt`
   - `Chain Lightning`
   - `Storm Step`
   - `Tempest Cage`
4. `Aegis`
   - `Mana Shield`
   - `Mirror Ward`
   - `Blink`
   - `Arcane Recovery`

#### Rogue

| 项 | 设计 |
| --- | --- |
| 核心定位 | 机动、爆发、印记与残局处理 |
| 主资源 | `STAMINA` |
| 次资源 | `MOMENTUM` |
| 主通道 | `PHYSICAL / SHADOW` |
| 对抗通道 | `PHYSICAL / MENTAL` |
| 防御机制 | 规避、潜伏、短时消失 |
| 位移 | `Shadowstep` / `Fade` |
| panic answer | `Smoke Veil` / `Escape Plan` |

树系：

1. `Cutthroat`
   - `Quick Slash`
   - `Hemorrhage`
   - `Eviscerate`
   - `Assassinate`
2. `Shadow Arts`
   - `Vanish`
   - `Shadowstep`
   - `Smoke Veil`
   - `Nightfall`
3. `Dirty Tricks`
   - `Sand Throw`
   - `Crippling Strike`
   - `Caltrops`
   - `Escape Plan`
4. `Opportunist`
   - `Mark Prey`
   - `Flank`
   - `Riposte`
   - `Killing Spree`

#### Templar

| 项 | 设计 |
| --- | --- |
| 核心定位 | 圣光近战、自我净化、惩击与圣域控场 |
| 主资源 | `POSITIVE` |
| 主通道 | `HOLY`，辅以 `PHYSICAL` |
| 对抗通道 | `SPELL` |
| 防御机制 | 圣域、护盾、净化、反击 |
| 位移 | `Judgement Step` / `Chain of Oath` |
| panic answer | `Divine Intervention` |

树系：

1. `Judgement`
   - `Smite`
   - `Holy Lash`
   - `Verdict Beam`
   - `Final Reckoning`
2. `Sanctuary`
   - `Consecrate`
   - `Purifying Light`
   - `Bastion Prayer`
   - `Sanctuary Dome`
3. `Retribution`
   - `Rebuke`
   - `Aegis of Faith`
   - `Martyr's Guard`
   - `Wrath Unbound`
4. `Edicts`
   - `Silence Heretic`
   - `Chain of Oath`
   - `Exorcism`
   - `Divine Intervention`

### 12.5 4 个进阶职业

#### Berserker

| 项 | 设计 |
| --- | --- |
| 来源 | Vanguard 进阶 |
| 主资源 | `RAGE` |
| 主通道 | `PHYSICAL` |
| 特点 | 低血高收益、斩杀、暴走、强前摇强回报 |

树系：

1. `Frenzy`
2. `Bloodlust`
3. `Reckless Guard`
4. `Slayer`

#### Spellblade

| 项 | 设计 |
| --- | --- |
| 来源 | Arcanist 进阶 |
| 主资源 | `MANA` |
| 次资源 | `ARCANE_CHARGE` |
| 主通道 | `PHYSICAL + FIRE/COLD/LIGHTNING` |
| 特点 | 近战附魔、Blink 斩、元素轮转 |

树系：

1. `Imbued Blade`
2. `Blink Assault`
3. `Spellguard`
4. `Weave Mastery`

#### Shadowblade

| 项 | 设计 |
| --- | --- |
| 来源 | Rogue 进阶 |
| 主资源 | `STAMINA` |
| 次资源 | `SHADOW` |
| 主通道 | `PHYSICAL + SHADOW` |
| 特点 | 潜伏、诅咒、暗影位移、连环爆发 |

树系：

1. `Ambush`
2. `Venom Arts`
3. `Shadow Magic`
4. `Escape`

#### Warden

| 项 | 设计 |
| --- | --- |
| 来源 | 远近混合进阶 |
| 主资源 | `FOCUS` |
| 次资源 | `STAMINA` |
| 主通道 | `PHYSICAL + LIGHTNING` |
| 特点 | 远近双形态、印记猎杀、位移战术 |

树系：

1. `Twin Arms`
2. `Marks`
3. `Waystep`
4. `Storm Discipline`

### 12.6 职业系统冻结点

1. `Phase 2`
   - 只落地 4 基础职业
   - 每职业 2~3 树即可起步
2. `Phase 3`
   - 冻结 4 基础 + 4 进阶职业 roster
   - 冻结每个职业的资源模型和主树系方向
3. `Phase 4`
   - 不再增加新职业，只扩内容和 build 差异
4. `Phase 5`
   - 只做平衡、AI、UI 和 QA，不改职业核心语义

---

## 13. 怪物与 AI 系统详细设计

### 13.1 设计目标

1. 从 3 种模板行为升级为可数据驱动、可追踪、可做 Boss phase 的 AI。
2. 在 `Phase 2 ~ Phase 4` 内不引入通用脚本 runtime。
3. `Phase 5` 的战术 AI 必须建立在同一套 perception 和 action catalog 上。

### 13.2 MonsterTemplate V2

```kotlin
enum class MonsterRank {
    NORMAL,
    ELITE,
    BOSS,
}

enum class MonsterArchetype {
    BRUTE,
    DUELIST,
    SKIRMISHER,
    ARTILLERY,
    CONTROLLER,
    SUMMONER,
}

data class MonsterTemplateV2(
    val id: String,
    val rank: MonsterRank,
    val archetype: MonsterArchetype,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val audioProfile: String,
    val stats: StatsBlock,
    val combatProfile: CombatProfileV2,
    val resources: List<ResourcePoolDef>,
    val resistances: ResistanceProfile,
    val aiProfileId: String,
    val talents: List<GrantedTalentRef>,
    val lootProfileId: String,
    val tags: List<String>,
)
```

### 13.3 AI 分层

AI 必须拆成 4 层：

1. `Perception Layer`
   - 视野
   - 听觉
   - 上次已知位置
   - 仇恨焦点
   - 目标优先级
2. `Intent Layer`
   - 当前高层意图
   - 追击/拉扯/施法/召唤/撤退
3. `Action Selection Layer`
   - 选择使用哪个 talent、攻击、移动或等待
4. `Execution Layer`
   - 路径、目标点、telegraph、占位冲突处理

### 13.4 Phase 2~4 的脚本化 AI DSL

不使用代码字符串，使用结构化 DSL：

```yaml
aiProfiles:
  - id: artillery_fire_mage_v1
    archetype: ARTILLERY
    perception:
      sight: 8
      hearing: 5
      memoryTurns: 4
    targetPolicy:
      primary: LOWEST_EFFECTIVE_HP
    rules:
      - id: escape_low_hp
        priority: 100
        when:
          all:
            - self_hp_pct_lte: 0.30
            - talent_ready: blink
        then:
          action: USE_TALENT
          talentId: blink
          target: SAFE_TILE_AWAY_FROM_TARGET
      - id: cast_aoe
        priority: 80
        when:
          all:
            - enemies_in_radius_gte: { radius: 2, count: 2 }
            - talent_ready: flame_burst
        then:
          action: USE_TALENT
          talentId: flame_burst
          target: CLUSTER_CENTER
      - id: hold_distance
        priority: 50
        when:
          any:
            - target_distance_lt: 3
        then:
          action: STEP_AWAY
      - id: default_attack
        priority: 10
        then:
          action: BASIC_ATTACK
```

### 13.5 支持的条件词汇

首批 DSL 只支持有限条件：

1. `self_hp_pct_lte`
2. `self_resource_gte`
3. `target_visible`
4. `target_distance_lt/lte/gte`
5. `talent_ready`
6. `has_status`
7. `target_has_status`
8. `enemies_in_radius_gte`
9. `allies_in_radius_gte`
10. `phase_active`
11. `can_kill_estimate`

### 13.6 支持的动作原语

1. `BASIC_ATTACK`
2. `USE_TALENT`
3. `STEP_TOWARD`
4. `STEP_AWAY`
5. `REPOSITION_FLANK`
6. `HOLD_POSITION`
7. `INTERACT_OBJECT`
8. `SUMMON`
9. `WAIT`

### 13.7 AIDecisionTrace

AI 决策必须输出 trace：

```kotlin
data class AIDecisionTrace(
    val turnId: Long,
    val actorId: Int,
    val profileId: String,
    val perceivedTargetId: Int?,
    val matchedRuleIds: List<String>,
    val selectedAction: String,
)
```

### 13.8 BossEncounter 与 BossPhase

Boss 不再只是“怪物模板 + 技能等级”。

```kotlin
data class BossEncounterDef(
    val id: String,
    val bossTemplateId: String,
    val arenaId: String,
    val phases: List<BossPhaseDef>,
    val rewards: RewardProfile,
)

data class BossPhaseDef(
    val id: String,
    val enterCondition: BossPhaseTrigger,
    val aiProfileId: String,
    val enabledTalents: List<String>,
    val arenaHazards: List<String>,
    val summonPackIds: List<String>,
    val reactWindow: Int,
)
```

### 13.9 Boss phase 触发条件

首批支持：

1. `HP_PCT_LTE`
2. `TURN_COUNT_GTE`
3. `SUMMONS_DEAD`
4. `OBJECT_DESTROYED`

### 13.10 Phase 5 的战术 AI

`Phase 5` 不是推翻 DSL，而是在 DSL 上方叠一个高层评分层：

1. 先生成动作候选
2. 用评分层选高层意图
3. 仍由同一执行层完成目标点与动作执行

这样：

1. 早期怪物不会返工
2. `Boss` 和精英可以逐步增强
3. `AIDecisionTrace` 保持统一

---

## 14. 客户端消费边界与 RenderSnapshot

虽然本轮重点不是渲染，但技能 telegraph、Boss 预警、状态图标、资源条、回放截图都依赖 snapshot 边界，因此必须先冻结。

### 14.1 RenderSnapshot 合同

```kotlin
data class RenderSnapshot(
    val turnId: Long,
    val actionId: Long,
    val mapCells: List<CellView>,
    val props: List<PropView>,
    val actors: List<ActorView>,
    val overlays: List<OverlayView>,
    val uiState: UiStateView,
    val logEvents: List<LogTokenEvent>,
)
```

冻结规则：

1. `core` 只输出语义视图，不输出纹理对象。
2. 相同 world state 必须得到相同 snapshot 哈希。
3. 动画补间属于 `client`。
4. `golden screenshot` 以 snapshot 为输入，不以实时线程状态为输入。

### 14.2 Snapshot 生成时机

固定在以下时机生成：

1. 每次 `ActionResolution` 完成后
2. 每次显式 UI 状态变化后
3. 载入存档后

不在每一帧重新生成逻辑 snapshot。

---

## 15. 美术与音频资源生产管线系统

### 15.1 设计目标

1. 把资源生产从“素材堆积”升级为正式系统。
2. 让图片与音频都具备：
   - 可追溯
   - 可批处理
   - 可 lint
   - 可导入 runtime
3. 让 AI 生成、人工挑选、程序化处理和游戏导入形成一条稳定流水线。
4. 保证后续所有正式图片都服从统一美术风格圣经。

风格权威文档固定为：

[2026-03-13-art-style-bible.md](./2026-03-13-art-style-bible.md)

后续所有图像生成都必须引用该文档，不允许各自发散生成。

### 15.2 资源生产总原则

1. 运行时只消费 cooked 资产，不消费 raw 资产。
2. 所有资产都必须先有 `AssetSpec`，再有图、音频和 manifest。
3. 生成模型只是 provider，不是风格定义者。
4. 没有 raw 来源、处理步骤、manifest 映射的资产，不允许进入主线。

### 15.3 推荐目录结构

```text
assets-src/
  style/
    art-style-bible.md
    palette-guides/
    prompt-templates/
  image/
    specs/
    prompts/
    raw/
    selected/
    processed/
    atlases/
    manifests/
  audio/
    specs/
    raw/
    cleaned/
    mastered/
    cues/
    manifests/
tools/
  asset-pipeline/
  audio-pipeline/
  smoke/
```

### 15.4 图像资源分类

所有图像都必须归入固定资产族：

1. `tile_ground`
2. `tile_wall`
3. `tile_decal`
4. `prop_interactable`
5. `prop_environment`
6. `actor_sprite`
7. `portrait`
8. `icon_skill`
9. `icon_status`
10. `icon_item`
11. `ui_frame`
12. `vfx_plate`

### 15.5 图像资产规格

#### 作者规格

| 类型 | 作者尺寸 | 运行时默认 |
| --- | --- | --- |
| 地形 Tile | 64x64 | 48x48 |
| Prop | 64x64 / 128x128 / 128x64 | 48x48 对齐 |
| Actor 主体 | 64x64 | 48x48 |
| Portrait | 512x512 或 768x768 | UI 缩放 |
| Icon | 128x128 | 48x48 / 64x64 |
| VFX plate | 256x256 / 512x512 | 运行时裁切 |

#### 几何约束

1. Tile 顶面边界必须清晰。
2. Actor 主体不得超出默认 footprint 太多，避免遮挡误导。
3. 图标中心主体占画面约 `60% ~ 75%`。
4. 交互物必须有清晰基底和热点区域。

### 15.6 图像生产流程

固定流程：

```text
AssetSpec
 -> Style Tag Resolve
 -> Prompt Compile
 -> Image Provider Generate
 -> Raw Store
 -> Human Select
 -> Segmentation / Cleanup
 -> Crop / Pivot / Scale Normalize
 -> Palette / Lighting Normalize
 -> Readability Pass
 -> Atlas Pack
 -> VisualManifest Emit
 -> Runtime Resolve
```

#### Step 1: AssetSpec

每个图像先定义 `AssetSpec`：

```kotlin
data class AssetSpec(
    val id: String,
    val assetType: String,
    val subject: String,
    val biome: String?,
    val faction: String?,
    val profession: String?,
    val materialTags: List<String>,
    val moodTags: List<String>,
    val footprint: String,
    val styleTag: String = "ktome-middle-fantasy-painterly-tile-v1",
)
```

#### Step 2: Prompt Compile

Prompt 模板必须：

1. 引入风格 tag
2. 引入材质和情绪标签
3. 引入可读性与技术约束
4. 自动追加 negative constraints

#### Step 3: Provider Generate

Provider 允许多实现，但接口固定：

```kotlin
interface AssetGenerationProvider {
    fun generate(spec: AssetSpec, prompt: CompiledPrompt): GeneratedAssetBatch
}
```

允许的 provider：

1. `GeminiProvider`
2. `ManualImportProvider`
3. `InHousePaintoverProvider`

冻结规则：

1. Gemini 不是唯一来源。
2. 如果模型输出不稳定，允许切换到手工导入或人工二改。

#### Step 4: Human Select

必须保留人工挑选步骤。

原因：

1. 风格一致性
2. 版权风险排除
3. 可读性过滤
4. footprint 与玩法识别过滤

#### Step 5: Cleanup

处理包括：

1. 去背景
2. 去多余枝节和噪点
3. 修正边缘溢出
4. 清除烘焙文字、水印、徽章

#### Step 6: Normalize

处理包括：

1. 统一 pivot
2. 统一 safe area
3. 统一相机高度感
4. 统一阴影方向
5. 统一主光色温

#### Step 7: Readability Pass

必须检查：

1. 48x48 下是否仍可识别
2. 危险物/交互物是否有足够对比
3. 同类资产是否属于同一家族

#### Step 8: Atlas Pack

打包要求：

1. 按功能和层级分 atlas，不按“随便塞满”分。
2. 地形、角色、UI、VFX、icons 至少分开。
3. atlas 名称和 region 名必须稳定，不能因打包顺序漂移。

#### Step 9: VisualManifest

```kotlin
data class VisualManifestEntry(
    val visualKey: String,
    val atlasId: String,
    val regionName: String,
    val pivotX: Float,
    val pivotY: Float,
    val footprint: String,
    val tags: List<String>,
)
```

### 15.7 图像导入到游戏的规则

1. `game` 只存 `visualKey`。
2. `client` 启动时加载 `VisualManifest`。
3. 运行时不允许直接按文件路径找图。
4. 缺资源时必须有显式 fallback 图和错误日志。

### 15.8 美术风格使用规则

1. 所有 prompt 必须显式引用 [2026-03-13-art-style-bible.md](./2026-03-13-art-style-bible.md) 对应的 style tag。
2. 若生成图与风格圣经冲突，以风格圣经为准，不以模型“发挥”作为理由。
3. 不允许不同 phase 各自定义新的主风格方向。
4. 若风格更新，必须 bump 风格版本并记录变更。

### 15.9 音频资源分类

所有音频必须归入固定 cue 家族：

1. `ui`
2. `footstep`
3. `melee`
4. `projectile`
5. `spell`
6. `status`
7. `monster`
8. `interactable`
9. `ambience`
10. `music`

### 15.10 音频生产目标

1. 先建立 cue routing，再补文件。
2. 每类关键反馈都要有可识别声纹。
3. 不靠文件名猜用途。
4. 不在 `Phase 2 ~ Phase 5` 做大规模配音系统。

### 15.11 音频生产流程

固定流程：

```text
AudioSpec
 -> Source Acquire / Synthesis
 -> Trim / Clean
 -> Loudness Normalize
 -> Loop Check
 -> Variant Grouping
 -> Cue Manifest Emit
 -> Runtime Routing
 -> Bus Mix / Ducking / Priority
```

#### Step 1: AudioSpec

```kotlin
data class AudioSpec(
    val id: String,
    val category: String,
    val eventFamily: String,
    val biome: String?,
    val profession: String?,
    val mood: String?,
    val oneShot: Boolean,
    val looping: Boolean,
)
```

#### Step 2: Source Acquire

音频来源允许：

1. 自制录音
2. 合法样本包
3. 合法合成
4. 人工设计

冻结规则：

1. 必须可追溯来源。
2. 不允许匿名来源音效直接进库。

#### Step 3: Clean & Normalize

至少处理：

1. 去静音头尾
2. 去削波
3. 响度统一
4. 格式统一

#### Step 4: Variant Grouping

同类事件必须支持变体组，例如：

1. `footstep_stone_01..04`
2. `melee_hit_light_01..03`
3. `ui_confirm_01..03`

#### Step 5: Cue Manifest

```kotlin
data class AudioCueManifestEntry(
    val audioKey: String,
    val files: List<String>,
    val bus: String,
    val category: String,
    val volumeDb: Float,
    val pitchVariance: Float = 0f,
)
```

### 15.12 音频导入到游戏的规则

1. `game` 和 `core` 只存 `audioProfile`。
2. `client` 通过 `AudioRouter` 映射到 cue group。
3. 所有关键事件都必须通过语义事件触发音频，而不是 gameplay 代码直调音频 API。

### 15.13 资源适配与导入验证

必须有以下 lint / smoke：

1. `asset-lint`
   - 命名
   - 分辨率
   - region 对齐
   - pivot
   - footprint
2. `style-lint`
   - 是否标注 style tag
   - 是否有来源
   - 是否有评审记录
3. `audio-lint`
   - 时长
   - 循环点
   - 响度
   - cue 分类
4. `manifest-lint`
   - `visualKey/audioProfile` 是否都能解析

### 15.14 资源版本合同

资源系统必须单独维护版本，不和存档版本混用：

```kotlin
data class AssetVersionContract(
    val styleVersion: String,
    val visualManifestVersion: Int,
    val audioManifestVersion: Int,
    val assetPipelineVersion: Int,
)
```

冻结规则：

1. 风格版本变化不必强制 bump 存档版本。
2. `visualManifestVersion` 或 `audioManifestVersion` 发生破坏式变化时，客户端必须在启动期校验失败并给出明确错误。
3. `assetPipelineVersion` 用于标识离线工具链变化，便于追溯 raw -> cooked 的处理链。

### 15.15 运行时资源加载策略

客户端资源加载固定分三层：

1. `Bootstrap Load`
   - logo
   - font
   - main menu
   - locale bundle
2. `Session Load`
   - 当前 tileset atlas
   - 职业主体
   - 基础 icon
   - 当前 zone ambience
3. `Encounter Load / Warm Cache`
   - Boss arena overlays
   - 高价值 VFX
   - 稀有 portrait

冻结规则：

1. 不允许首次进入 Boss 房时临时同步解析大体量原始资源。
2. `VisualManifest/AudioManifest` 在启动时一次校验。
3. 缺失 `visualKey` 时使用 `missing_visual`，缺失 `audioProfile` 时使用静默 fallback，并写错误日志。
4. atlas 必须按使用层级和内存预算切分，禁止把所有资源打进单一超大 atlas。

### 15.16 Content Pack / Mod 资源命名空间

即使 `Phase 4` 还不做 Lua runtime，资源命名空间也要提前固定：

```text
core:actor.vanguard
core:tile.ruins.wall_01
core:icon.status.burn
pack.shadowlands:tile.shadow.root_altar
pack.shadowlands:audio.ambience.shadow_wind
```

冻结规则：

1. 官方资源默认命名空间为 `core:`.
2. 外部 content pack 必须带独立 namespace。
3. content pack 不允许覆盖官方资源文件路径，只允许通过 manifest overlay 显式替换。
4. 禁用 content pack 后，运行时必须能回落到官方 manifest，而不留下悬挂 key。

### 15.17 分阶段资源预算与 DoD

#### Phase 2

最低 DoD：

1. 4 zone 的基础 Tile family
2. 4 基础职业的主体 sprite 或 portrait
3. 核心 icon、状态 icon、任务 icon
4. UI 核心 cue、脚步、近战、施法、Boss 预警、基础 ambience

#### Phase 3

最低 DoD：

1. 4 基础职业正式树和 4 进阶职业的辨识资产
2. biome 变体
3. Boss 套件
4. 更完整的元素 VFX 和状态音效

#### Phase 4

最低 DoD：

1. artifact / elite mutation / hidden zone 资产集
2. content pack 资源命名空间与 overlay 验证

#### Phase 5

最低 DoD：

1. 资源覆盖率清盘
2. 混音与优先级打磨
3. 低端机和长局场景的资源性能回归

### 15.18 Phase 切分建议

#### `Phase 2`

1. 先建立最小图像与音频 pipeline。
2. 先覆盖 HUD、基础 Tile、职业主体、基础 icon、基础 UI 音效、脚步、近战、施法、Boss 预警。

#### `Phase 3`

1. 扩到职业辨识度、Boss 套件、biome 变体、关键状态图标与特效。

#### `Phase 4`

1. 扩到隐藏区域、artifact、elite mutation 和 content pack 资源映射。

#### `Phase 5`

1. 只做 polish、性能、混音、QA 和覆盖清盘。

---

## 16. 内容 schema 约束

### 16.1 所有正式内容对象必须具备

1. `id`
2. `nameKey`
3. `descKey`
4. `visualKey`
5. `iconKey`
6. `audioProfile`
7. `schemaVersion`
8. `tags`

### 16.2 ZoneSpec V1

```yaml
zone:
  id: shattered_outpost
  nameKey: zone.shattered_outpost.name
  descKey: zone.shattered_outpost.desc
  biome: RUINS
  recommendedLevel: 1
  floorCount: 2
  ambientProfile: ambience.ruins_wind
  tilesetKey: tileset.ruins
  monsterPools: [rat, bandit, skeleton]
  elitePools: [bandit_captain]
  bossEncounterId: null
  objectiveSetId: zone_objectives.shattered_outpost
```

### 16.3 Phase 2 四个 zone 最低规格

| zone | 推荐等级 | 主要敌群 | 主要通道 | 主要任务物件 |
| --- | --- | --- | --- | --- |
| `Shattered Outpost` | 1~2 | 鼠群、盗匪、骷髅 | `PHYSICAL / SHADOW` | 武库门、补给箱、警报篝火 |
| `Greenwood Fringe` | 2~3 | 狼、巡林守卫、弓手 | `PHYSICAL / COLD / LIGHTNING` | 狩猎印记、路障、祭树 |
| `Deep Iron Mine` | 3~4 | 兽人矿工、铸炉守卫、元素工匠 | `PHYSICAL / FIRE` | 升降机、熔炉、钥匙架 |
| `Ashgate Depths` | 4~5 | 暗影祭司、亡骸、Boss | `SHADOW / HOLY` | 封印门、祭坛、Boss arena |

---

## 17. 分阶段补充

### 17.1 Phase 2 细化

#### 目标

把当前 MVP 规则迁移到可长期演进的语义合同上，并形成最小 Tile + i18n 短局。

#### 必须冻结的基础系统

1. `1000` 能量制
2. `DamageType`
3. `ResourcePool`
4. `StatusEffectDef/StatusInstance`
5. `TalentDef V2`
6. `GameEvent/LogTokenEvent`
7. `RenderSnapshot`
8. `kotlinx.serialization`

#### 建议工作包

#### `P2-A1` 规则合同迁移

涉及：

1. `core/turn`
2. `core/combat`
3. `core/event`
4. `core/save`
5. `core/talent`
6. `game/FoundationGameSession`

完成定义：

1. 现有近战、现有 4 技能、现有怪物都已经跑在新事件和新存档合同上。
2. 旧消息日志字符串退出主逻辑路径。

#### `P2-A2` schema 升级

涉及：

1. `game/data`
2. `game/model`
3. `game/resources/data/*.yaml`

完成定义：

1. 现有 talent/monster/boss schema 带上 key、version、visual/audio 字段。
2. 当前加载器支持 V2 schema。

#### `P2-B1` 最小 Tile / Locale 可玩切片

完成定义：

1. 首页语言切换
2. 1 个 zone
3. 2 个职业
4. 1 个 Boss
5. golden screenshot 基线

#### `P2-C1` 短局扩展

完成定义：

1. 4 个职业
2. 4 个 zone
3. 24 怪 + 24 物品
4. `SoloClearLab v1`

#### Phase 2 门禁

1. 事件、日志、snapshot 哈希稳定
2. save/load 往返稳定
3. `locale-lint` 全绿
4. `contract-lint` 全绿
5. 4 职业短局通关

### 17.2 Phase 3 细化

#### 目标

冻结正式战斗公式、职业树、脚本化 AI、Boss phase 和长局结构。

#### 必须冻结的基础系统

1. 命中/暴击/Power vs Save 公式
2. 护甲/抗性/穿透
3. 状态生命周期与清理规则
4. 技能 targeting / telegraph 语法
5. 4 基础 + 4 进阶职业 roster
6. AIProfile DSL
7. BossPhaseDef

#### 建议工作包

1. `P3-A1` CombatTrace 与正式公式
2. `P3-A2` 状态与 sustain/mark/ward
3. `P3-A3` Talent tree V2、断点成长、动态说明
4. `P3-B1` 4 基础职业正式树
5. `P3-B2` 4 进阶职业与 Profile
6. `P3-B3` AIProfile/BossEncounter/BossTrace
7. `P3-C1` 世界地图、分支 zone、affix v1

#### Phase 3 门禁

1. `CombatTrace` 金样本
2. 洗点/回滚回归
3. Boss telegraph 白盒验证
4. 职业/种族/Profile 组合可进局
5. 4~6 小时 run 可稳定结束

### 17.3 Phase 4 细化

#### 目标

深化 ProcGen、Loot 和重复游玩差异，同时引入不依赖脚本 runtime 的 content pack 扩展点。

#### 必须冻结的基础系统

1. map family
2. pattern/vault 元数据
3. lock-key DAG 校验
4. loot budget
5. elite mutation
6. content pack overlay schema

#### 建议工作包

1. `P4-A1` map family + loop + pattern room
2. `P4-A2` lock-key / 可解性验证
3. `P4-B1` affix/unique/artifact budget
4. `P4-B2` elite mutation / hidden event / rand boss 约束
5. `P4-C1` overlay / content pack / harness

#### Phase 4 门禁

1. 多 seed 下地图可达、可读、无死局
2. 掉落质量与层级匹配
3. 隐藏内容可被验证
4. content pack 可通过 schema + lint + harness 加载

### 17.4 Phase 5 细化

#### 目标

把已经稳定的规则和内容提升到发布级：战术 AI、性能、回放、QA、安装包。

#### 必须冻结的基础系统

1. 战术 AI 评分接口
2. 感知/仇恨/潜行状态机
3. replay schema
4. perf/soak 基线
5. Localization QA 与 Accessibility QA 清单

#### 建议工作包

1. `P5-A1` tactical scoring on top of scripted AI
2. `P5-A2` hate focus / stealth / memory
3. `P5-B1` perf smoke / soak / profiling budgets
4. `P5-B2` run history / death analysis / replay
5. `P5-C1` install package / balance lab / release docs

#### Phase 5 门禁

1. 固定场景 AI 回归稳定
2. 长局 soak 稳定
3. replay 可复现
4. 死因可解释
5. 安装包可验收

---

## 18. 验证与门禁总表

| 类别 | 必须存在的验证 |
| --- | --- |
| 规则 | `./gradlew test`、`CombatTrace`、固定 seed 回归 |
| 存档 | 当前版本往返读写、版本不匹配错误提示 |
| i18n | `locale-lint`、双语言 snapshot/screenshot |
| 内容 schema | `contract-lint` |
| 职业 | `SoloClearLab` |
| Boss/AI | `BossHarness`、`AIDecisionTrace` |
| 客户端 | `golden screenshot`、headless smoke |
| 性能 | `perf-smoke`、`soak` |

---

## 19. 明确禁止事项

1. 不要继续扩 `TalentLevelEffect` 字段袋。
2. 不要继续把状态名写成技能名。
3. 不要让 `client` 承担规则状态机。
4. 不要在 `Phase 2 ~ Phase 4` 直接上 Lua runtime。
5. 不要为追求内容量而跳过 trace、lint 和 harness。
6. 不要再往正式内容里写裸中文/英文文案。
7. 不要在没有 schemaVersion 的情况下扩内容格式。

---

## 20. 下一步实现顺序建议

如果从今天开始进入真正实现，建议严格按以下顺序落地：

1. `P2-A`
   - `SaveManager` -> `kotlinx.serialization`
   - `TurnScheduler` -> 1000 能量制
   - `GameEvent` -> 语义事件 V2
   - `TalentDef/TalentResolver` -> V2 schema + executor
   - `StatusEffectDef/StatusInstance`
2. `P2-B`
   - `RenderSnapshot`
   - `VisualManifest/AudioManifest`
   - 最小 Tile/HUD/locale 切片
3. `P2-C`
   - 4 基础职业
   - 4 个 phase2 zone
   - `SoloClearLab`
4. `P3-A`
   - CombatTrace + 正式公式
   - AIProfile DSL
   - BossPhaseDef

这不是“建议优先级”，而是后续系统复杂度最低的真实依赖顺序。

---

## 21. 一句话原则

后续所有系统设计都必须回答同一个问题：

`这个抽象是否让规则更稳定、内容更好扩、客户端更好消费、AI 更容易验证。`

如果答案是否定的，就不应该进入当前主线设计。
