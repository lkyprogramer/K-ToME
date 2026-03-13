# K-ToME 核心系统详细设计与阶段补充文档（Part 1：第 1~4 节）

> 日期：2026-03-13
> 状态：Draft — 权威设计参考
> 适用范围：Phase 2 ~ Phase 5 所有涉及战斗、元素、公式、追踪的核心系统
> 文档定位：补充 `2026-03-13-phase2-to-phase5-final-roadmap.md` 中未充分定义的核心系统设计细节

---

## 1. 文档定位与背景

### 1.1 目的

本文档是 K-ToME 项目核心系统的 **权威详细设计参考**。它补充最终路线图（`2026-03-13-phase2-to-phase5-final-roadmap.md`）中留白或仅以标题提及的关键系统，提供：

- 精确的数学公式与常量定义
- 完整的 Kotlin 数据结构与接口骨架
- 各系统在不同阶段的引入计划
- 系统间的交互规则与结算顺序
- 可直接用于单元测试的示例计算

### 1.2 输入文档

本文档在以下文档的基础上撰写，并综合了其中的设计意图与审阅反馈：

| 文档 | 作用 |
| --- | --- |
| `2026-03-13-phase2-to-phase5-final-roadmap.md` | 执行权威路线图，本文档对其做系统级补充 |
| `2026-03-13-phase2-5-review-and-recommendations.md` | 审阅报告，识别出的设计缺口是本文档的核心驱动力 |
| `K-ToME_Phase2_to_Phase5_PR_Development_Guide_v2_SinglePlayer_Tile_i18n.md` | 原始 PR 级开发指南，提供职业、内容、资源流水线等上下文 |
| `Roguelike 游戏开发指导文档.md` | 技术白皮书，提供公式、架构、AI 等深层设计思路 |
| Phase 1 实际代码（`core/` 模块） | 当前实现基线，所有设计必须考虑迁移路径 |

### 1.3 覆盖规则

1. 本文档 **不覆盖** 路线图的阶段划分、工作包切分和出口标准——那些仍以路线图为权威。
2. 本文档 **覆盖** 路线图中未充分定义的系统内部设计（公式、数据结构、结算顺序、交互规则）。
3. 若本文档与原始技术白皮书在同一系统上的设计存在冲突，以本文档为准。
4. 本文档中的所有 Kotlin 代码骨架是 **设计锚点**，实际实现可在不违背语义合同的前提下调整命名和内部结构。

### 1.4 当前代码基线

截至 Phase 1 完成（`v0.1.x`），以下是本文档涉及的核心系统的当前状态：

| 系统 | 当前文件 | 当前状态 | 本文档补充的内容 |
| --- | --- | --- | --- |
| 战斗结算 | `core/combat/CombatResolver.kt` | 仅物理近战，无伤害通道，线性防御减免 | 伤害通道、非线性公式、结算管线、追踪系统 |
| 伤害结果 | `core/combat/DamageResult.kt` | 3 字段（raw/reduced/final） | 扩展为完整 `CombatTrace` |
| 事件系统 | `core/event/GameEvent.kt` | 6 种事件，无中央调度 | 回调注册表、Power/Save 对抗 |
| 属性系统 | `core/ecs/Components.kt` + `core/stats/StatsCalculator.kt` | 4 主属性 + 线性派生 | 收益递减、抗性/穿透属性 |
| 状态效果 | `core/talent/TalentModels.kt` | 4 种 `StatusEffectType` | 扩展至 20+ 种，支持元素关联 |
| 回合调度 | `core/turn/TurnScheduler.kt` | 100 能量阈值 | 路线图已计划升级至 1000，本文档不重复 |

---

## 2. 总体设计评审与建议

### 2.1 当前设计的优势（简要）

在展开设计缺口之前，先确认当前文档体系中已经做得好的决策，这些决策应在后续开发中继续强化：

1. **Solo-Clear Contract（单通合同）**：每个职业必须独立具备输出、自保、位移、群处理、Boss 处理、Panic Answer 六条能力线。这是防止设计偏差的硬墙，后续不可退化。

2. **PR 模板六段结构**（背景/目标/范围/非目标/验证/门禁）：规范度高于大多数游戏项目的开发文档，直接可用。

3. **Golden Seed 回归体系**：同一 seed + 同一输入序列必须产出可哈希比对的确定性结果。这是自动化验证的基石。

4. **存档不存文案只存 token**：使得语言切换与存档恢复完全解耦，架构上非常优雅。

5. **Locale Session Lock**：进局锁语言，避免了运行时切换的复杂度爆炸。

6. **模块边界清晰**：`core`（规则真源，零引擎依赖）→ `game`（内容装配）→ `client`（渲染表现）的三层分离已在 Phase 1 得到验证。

7. **阶段内拆分为检查点**：每个 Phase 内部有 A/B/C 检查点，避免长时间处于"主干不可玩"的重构状态。

8. **资源可追溯性要求**：`AssetSpec → prompt → raw → processed → atlas → manifest` 的全链路追溯在 AI 生成时代尤为重要。

### 2.2 关键设计缺口总览

以下表格是对所有已知文档的系统性审阅结果。每个缺口按严重程度分级：

- **P0**：阻塞性缺口，不解决会导致后续阶段大规模返工
- **P1**：重要缺口，会导致实现歧义或系统间不一致
- **P2**：建议补充，不解决可以绕行但会降低系统质量

| ID | 缺口描述 | 严重程度 | 受影响阶段 | 建议解决阶段 | 不解决的影响 |
| --- | --- | --- | --- | --- | --- |
| GAP-01 | 无伤害类型通道：当前只有物理伤害，但 Phase 2 的奥术师/圣堂武士需要元素伤害 | P0 | P2~P5 | P2-A | Phase 2 的 4 职业天赋全部退化为"不同颜色的物理伤害"，Phase 3 引入通道时大规模返工 |
| GAP-02 | 无多资源系统：当前只有 `Stamina`，但 Phase 2 需要 Mana、正能量等 | P0 | P2~P5 | P2-A | 4 职业无法有差异化的资源管理体验 |
| GAP-03 | AI 真空期：Phase 1 的 CHASE/KITE/PATROL 与 Phase 5 的 Utility AI 之间无中间层 | P0 | P2~P4 | P2-A（骨架）, P3-A（脚本化行为） | Phase 2~4 的精英/Boss 行为过于简陋，不具备游戏性 |
| GAP-04 | 序列化引擎未决策：GSON vs `kotlinx.serialization`，影响全局数据类注解方式 | P0 | P2~P5 | P2-A（P2-W1） | 所有数据类的序列化方式不统一，Phase 4 mod 数据兼容性无从设计 |
| GAP-05 | 状态效果系统过于简陋：仅 `STUNNED`/`ARMOR_BREAK`/`WAR_CRY_BUFF`/`WAR_CRY_DEBUFF` 四种 | P0 | P2~P5 | P2-A | Phase 2 的 32 个天赋无法表达燃烧、冰冻、流血、护盾等核心效果 |
| GAP-06 | 无战斗公式体系：防御线性减免（`rawDamage - defense`），无收益递减 | P1 | P3~P5 | P3-A（P3-W1） | 高属性下数值膨胀失控，防御堆叠无限收益 |
| GAP-07 | 无 Boss/精英 telegraph 系统：高危技能无预警机制 | P1 | P3~P5 | P3-A（P3-W4） | Boss 战退化为"数值碾压或被碾压"，无战术深度 |
| GAP-08 | 无资源回复策略框架：`staminaRegen` 硬编码为 `3.0 + modifiers` | P1 | P2~P5 | P2-A | 无法实现"击杀回复"/"受击回复"/"条件回复"等职业差异化资源循环 |
| GAP-09 | 无元素交互规则：火+冰是否抵消？闪电+水地形是否传导？ | P1 | P3~P5 | P3-A | 元素系统沦为纯粹的伤害着色，缺乏战术深度 |
| GAP-10 | 无详细职业/专精设计：仅有职业名称和天赋数量预算，无技能树结构 | P1 | P2~P3 | P2-C / P3-B | 天赋设计缺乏一致性框架，各职业风格漂移 |
| GAP-11 | 缺少 Zone 规格定义：4 个 Phase 2 Zone 只有名字没有层数/面积/怪物池/机制 | P1 | P2~P4 | P2-B（P2-W6） | 地图生成和遭遇配置无据可依 |
| GAP-12 | 无关键词注册表：天赋/装备描述中的术语无统一定义和 tooltip 支持 | P2 | P3~P5 | P3-A（P3-W3） | 玩家无法通过 tooltip 理解"穿透""收益递减""Power/Save"等概念 |
| GAP-13 | 无铭文/符文系统设计：ToME 的铭文是核心构筑系统之一 | P2 | P3~P5 | P3-A（P3-W2） | 缺少一个重要的自定义轴，构筑多样性下降 |
| GAP-14 | 无战斗追踪/审计系统：无法回溯一次攻击的完整计算过程 | P1 | P3~P5 | P3-A（P3-W1） | 调试困难，平衡调优无数据支撑，死因分析不可追溯 |
| GAP-15 | Power/Save 对抗体系未定义：状态命中判定缺乏物理/精神/法术三维度对抗 | P1 | P3~P5 | P3-A（P3-W1） | 所有状态效果要么 100% 命中要么无法施加，缺少对抗博弈 |
| GAP-16 | 无收益递减数学模型：不清楚哪些属性有收益递减、使用什么函数 | P1 | P3~P5 | P3-A（P3-W1） | 高属性区间数值失控，属性堆叠策略退化 |
| GAP-17 | 暴击系统过于简单：仅基于 DEX 线性增长，无暴击伤害属性、无暴击抗性 | P2 | P3~P5 | P3-A（P3-W1） | 暴击构筑缺乏深度 |
| GAP-18 | 无护甲/抗性分离：防御只有 `defense` 一个值，物理和元素减免不分 | P1 | P3~P5 | P3-A（P3-W1） | 元素伤害通道引入后，防御系统无法区分物抗和元素抗 |

### 2.3 设计原则补充

在路线图已有的开发合同之上，以下设计原则适用于本文档涉及的所有核心系统：

#### 原则 1：确定性原则（Determinism）

> **同一 seed + 同一输入序列 = 完全相同的输出。**

- 所有战斗结算、伤害计算、状态判定、掉落生成必须只依赖 `RandomSource` 提供的确定性随机数。
- 禁止在规则层使用系统时间、线程 ID、`HashMap` 迭代顺序等非确定性来源。
- 排序键必须完全定义，不允许出现"相等时顺序未定义"的场景。
- 验证方式：Golden Seed 回归测试，固定 seed 下的战斗结果哈希比对。

#### 原则 2：数据驱动原则（Data-Driven）

> **所有内容定义在数据文件（YAML/JSON）中，不在代码中硬编码。**

- 伤害类型的属性（名称 key、颜色、关联状态）通过数据定义。
- 战斗公式的常量（基础命中率、暴击倍率、收益递减系数）通过配置文件加载，代码中只给默认值。
- 新增一个怪物/天赋/装备不应需要修改任何 `.kt` 文件。

#### 原则 3：关注点分离原则（Separation of Concerns）

> **`core` 只输出语义数据，`client` 负责渲染表现。**

- `CombatTrace` 记录每一步的数值变化和原因标签，但不包含任何显示文本或动画指令。
- 伤害通道定义颜色代码只是 `client` 的 UI 提示，`core` 中以 `colorHint` 字段存在但不参与逻辑计算。
- 元素交互的视觉效果（蒸汽、传导特效）完全由 `client` 根据事件语义自行编排。

#### 原则 4：可测试性原则（Testability）

> **每个系统都必须能够用固定 seed 进行单元测试。**

- 战斗公式的每个子公式（命中、伤害、暴击、减免、穿透）都有独立的纯函数入口，接受原始数值参数。
- 状态效果判定可以通过注入固定 `RandomSource` 来验证 Power/Save 对抗。
- 收益递减函数是纯数学函数，无副作用，可独立测试。

#### 原则 5：渐进复杂度原则（Incremental Complexity）

> **系统从简单版本开始，跨阶段逐步增加深度。**

- Phase 2：引入 `DamageType` 枚举和 `DamageInstance`，但抗性/穿透暂用线性模型。
- Phase 3：升级为完整的非线性公式、Power/Save 对抗、`CombatTrace`。
- Phase 4：在 loot 系统中利用元素 affix，在地形中引入元素交互。
- 每次升级不应破坏已有的 Golden Seed 测试（或显式声明 seed 基线变更）。

---

## 3. 战斗系统详细设计

战斗系统是 K-ToME 所有核心系统中最复杂、最关键的一个。本节提供从伤害通道到结算管线的完整设计。

### 3.1 伤害通道体系

#### 3.1.1 通道定义

K-ToME 定义 **六大伤害通道**，覆盖物理和五种元素维度：

| 通道 ID | 中文名 | 英文名 | 主题色（Hex） | 主题描述 |
| --- | --- | --- | --- | --- |
| `PHYSICAL` | 物理 | Physical | `#C0C0C0` | 刀剑、钝器、弓箭、投掷。最基础的伤害形式 |
| `FIRE` | 火焰 | Fire | `#FF4500` | 燃烧、爆炸、熔岩。持续伤害、区域控制 |
| `COLD` | 寒冰 | Cold | `#00BFFF` | 冰冻、霜冻、减速。控制与削弱 |
| `LIGHTNING` | 闪电 | Lightning | `#FFD700` | 电击、链式传导、能量过载。爆发与穿透 |
| `HOLY` | 神圣 | Holy | `#FFFACD` | 净化、驱邪、神罚。对亡灵/恶魔特效 |
| `SHADOW` | 暗影 | Shadow | `#8B008B` | 腐蚀、诅咒、灵魂吞噬。消耗与压制 |

#### 3.1.2 各通道详细定义

**PHYSICAL（物理）**

- **主题身份**：战卫（Warrior）的核心通道；也是所有职业普通攻击的默认通道。游荡者（Rogue）的毒刃混合物理+暗影。大多数杂兵以物理伤害为主。
- **关联状态效果**：流血（`BLEED`）、破甲（`ARMOR_BREAK`）、击退（`KNOCKBACK`）、眩晕（`STUN`）。
- **抗性机制**：由 `armor`（护甲值）提供减免，使用减法模型 + 收益递减。物理抗性不以百分比计算，而是通过护甲换算。
- **穿透机制**：`armorPenetration` 降低目标的有效护甲值。

**FIRE（火焰）**

- **主题身份**：奥术师（Arcanist）的核心攻击通道之一。火焰系天赋倾向于持续伤害和区域控制。火焰类怪物（火元素、熔岩巨人）使用此通道。
- **关联状态效果**：燃烧（`BURN`，DoT，每回合造成基于初始伤害百分比的持续伤害）。
- **抗性机制**：`fireResistance`，百分比减免，取值范围 `[0, 75]`（上限 75%）。
- **穿透机制**：`firePenetration` 降低目标有效火焰抗性，穿透后抗性可为负（增伤）。

**COLD（寒冰）**

- **主题身份**：奥术师的控制通道。寒冰天赋倾向于减速、冰冻、控场。冰霜类怪物（冰元素、霜巨人）使用此通道。
- **关联状态效果**：减速（`SLOW`，降低目标速度 30%）、冰冻（`FREEZE`，完全无法行动，受到攻击时解除并承受碎冰伤害）。
- **抗性机制**：`coldResistance`，百分比减免，取值范围 `[0, 75]`。
- **穿透机制**：`coldPenetration`，穿透后抗性可为负。

**LIGHTNING（闪电）**

- **主题身份**：奥术师的爆发通道。闪电天赋倾向于高单次伤害和链式传导。闪电类怪物（雷元素、风暴构装体）使用此通道。
- **关联状态效果**：眩晕（`STUN`，短持续时间但高概率）、过载（`OVERCHARGE`，目标下次受到的闪电伤害 +25%）。
- **抗性机制**：`lightningResistance`，百分比减免，取值范围 `[0, 75]`。
- **穿透机制**：`lightningPenetration`，闪电穿透天然比其他元素穿透高 5~10 点（闪电通道的特色是高穿透）。

**HOLY（神圣）**

- **主题身份**：圣堂武士（Templar）的核心通道。神圣天赋倾向于净化、治疗增强、对亡灵/恶魔特效。神圣类怪物（堕落天使、光明构装体）使用此通道。
- **关联状态效果**：净化（`PURIFY`，移除目标身上的一个负面状态）、驱邪标记（`BANE`，对亡灵/恶魔额外增伤 50%）。
- **抗性机制**：`holyResistance`，百分比减免，取值范围 `[0, 75]`。亡灵/恶魔类怪物默认 `holyResistance = -25`（负抗性 = 增伤）。
- **穿透机制**：`holyPenetration`，穿透后抗性可为负。

**SHADOW（暗影）**

- **主题身份**：游荡者高级形态（影刃客）和暗影系怪物的核心通道。暗影天赋倾向于消耗目标资源、施加诅咒、灵魂损伤。亡灵、暗影生物使用此通道。
- **关联状态效果**：中毒（`POISON`，DoT + 降低治疗效果 30%）、诅咒（`CURSE`，降低目标所有属性 10%）、虚弱（`WEAKEN`，降低目标攻击力 20%）。
- **抗性机制**：`shadowResistance`，百分比减免，取值范围 `[0, 75]`。
- **穿透机制**：`shadowPenetration`，穿透后抗性可为负。

#### 3.1.3 Kotlin 数据结构

```kotlin
package com.ktome.core.combat

/**
 * 六大伤害通道枚举。
 * Phase 2 引入枚举和基础通道；Phase 3 激活完整抗性/穿透体系。
 */
enum class DamageType {
    PHYSICAL,
    FIRE,
    COLD,
    LIGHTNING,
    HOLY,
    SHADOW;

    /** 该通道是否为元素伤害（非物理） */
    val isElemental: Boolean
        get() = this != PHYSICAL
}

/**
 * 单条伤害实例，记录一次伤害的完整语义信息。
 *
 * @param baseAmount 基础伤害量（公式计算的原始值，未经减免）
 * @param type 伤害通道
 * @param sourceId 伤害来源实体
 * @param sourceAbilityId 造成伤害的天赋/技能 ID（普通攻击为 "melee_attack" 或 "ranged_attack"）
 * @param isCritical 是否暴击
 * @param critMultiplier 暴击倍率（非暴击时为 1.0）
 * @param penetration 穿透值（对物理为 armorPenetration，对元素为对应 elementPenetration）
 * @param tags 附加标签，用于回调过滤（如 "melee", "ranged", "spell", "dot", "aoe"）
 */
data class DamageInstance(
    val baseAmount: Int,
    val type: DamageType,
    val sourceId: EntityId,
    val sourceAbilityId: String = "melee_attack",
    val isCritical: Boolean = false,
    val critMultiplier: Double = 1.0,
    val penetration: Int = 0,
    val tags: Set<String> = emptySet(),
)

/**
 * 伤害通道的配置数据，从 YAML 加载。
 * 定义该通道的 UI 表现提示和默认特性。
 */
data class DamageTypeConfig(
    val type: DamageType,
    val nameKey: String,           // 如 "damage_type.fire"
    val colorHint: String,         // 如 "#FF4500"，供 client 使用
    val iconKey: String,           // 如 "icon_element_fire"
    val resistanceStatKey: String, // 如 "fireResistance"
    val penetrationStatKey: String,// 如 "firePenetration"
    val resistanceCap: Int = 75,   // 抗性上限百分比
    val defaultAssociatedEffects: List<String> = emptyList(),
)
```

#### 3.1.4 伤害通道配置示例（YAML）

```yaml
# data/damage_types.yaml
damage_types:
  - type: PHYSICAL
    nameKey: "damage_type.physical"
    colorHint: "#C0C0C0"
    iconKey: "icon_element_physical"
    resistanceStatKey: "armor"
    penetrationStatKey: "armorPenetration"
    resistanceCap: -1  # 物理使用护甲模型，非百分比上限
    defaultAssociatedEffects: ["BLEED", "ARMOR_BREAK", "KNOCKBACK", "STUN"]

  - type: FIRE
    nameKey: "damage_type.fire"
    colorHint: "#FF4500"
    iconKey: "icon_element_fire"
    resistanceStatKey: "fireResistance"
    penetrationStatKey: "firePenetration"
    resistanceCap: 75
    defaultAssociatedEffects: ["BURN"]

  - type: COLD
    nameKey: "damage_type.cold"
    colorHint: "#00BFFF"
    iconKey: "icon_element_cold"
    resistanceStatKey: "coldResistance"
    penetrationStatKey: "coldPenetration"
    resistanceCap: 75
    defaultAssociatedEffects: ["SLOW", "FREEZE"]

  - type: LIGHTNING
    nameKey: "damage_type.lightning"
    colorHint: "#FFD700"
    iconKey: "icon_element_lightning"
    resistanceStatKey: "lightningResistance"
    penetrationStatKey: "lightningPenetration"
    resistanceCap: 75
    defaultAssociatedEffects: ["STUN", "OVERCHARGE"]

  - type: HOLY
    nameKey: "damage_type.holy"
    colorHint: "#FFFACD"
    iconKey: "icon_element_holy"
    resistanceStatKey: "holyResistance"
    penetrationStatKey: "holyPenetration"
    resistanceCap: 75
    defaultAssociatedEffects: ["PURIFY", "BANE"]

  - type: SHADOW
    nameKey: "damage_type.shadow"
    colorHint: "#8B008B"
    iconKey: "icon_element_shadow"
    resistanceStatKey: "shadowResistance"
    penetrationStatKey: "shadowPenetration"
    resistanceCap: 75
    defaultAssociatedEffects: ["POISON", "CURSE", "WEAKEN"]
```

### 3.2 战斗公式体系

#### 3.2.1 命中判定公式

**Phase 1 当前实现：**

```
hitChance = clamp(0.85 + (accuracy - evasion) * 0.01, 0.05, 0.95)
```

这是一个简单的线性模型，在低等级下表现尚可，但在高属性区间会出现以下问题：

- `accuracy - evasion = 10` 时命中率即达到 95% 上限，属性差距的辨识度过低。
- 线性模型意味着"从 50% 命中堆到 60%"和"从 90% 堆到 95%"的边际收益相同，不符合直觉。

**Phase 3 升级方案：非线性 Sigmoid 变换**

使用修正的 Logistic 函数，在中间区间提供平滑过渡，在两端提供自然的收益递减：

```
令 d = accuracy - evasion
令 k = 0.04          （斜率系数，控制曲线陡度）
令 m = 0.0           （中点偏移，d = m 时命中率为 50%）
令 baseHitRate = 0.85 （基础命中率偏移）

rawHitChance = 1 / (1 + e^(-k * (d - m)))

// 将 [0, 1] 的 Sigmoid 输出映射到 [minHit, maxHit] 区间
minHit = 0.05
maxHit = 0.95
hitChance = minHit + (maxHit - minHit) * rawHitChance
hitChance = clamp(hitChance, minHit, maxHit)
```

但考虑到"基础命中率应大于 50%"的设计意图（英雄级角色的基本能力保障），我们对 Sigmoid 做中点偏移：

```
最终公式：
  d = accuracy - evasion
  k = 0.04
  m = -10  （中点向左偏移，使 d=0 时命中率约 85%）

  sigmoid(d) = 1 / (1 + e^(-k * (d - m)))
  hitChance = 0.05 + 0.90 * sigmoid(d)
  hitChance = clamp(hitChance, 0.05, 0.95)
```

**示例计算：**

| `accuracy - evasion` | Phase 1 线性 | Phase 3 Sigmoid | 说明 |
| --- | --- | --- | --- |
| -30 | 0.55 | 0.19 | 大劣势时命中率急剧下降 |
| -20 | 0.65 | 0.35 | 明显劣势 |
| -10 | 0.75 | 0.50 | 中点（m=-10 使此处为 50%） |
| -5 | 0.80 | 0.63 | 轻微劣势 |
| 0 | 0.85 | 0.74 | 属性相等时仍有不错命中率 |
| 5 | 0.90 | 0.83 | 轻微优势 |
| 10 | 0.95 | 0.89 | 明确优势 |
| 20 | 0.95 | 0.95 | 上限封顶 |
| 30 | 0.95 | 0.95 | 远超对手时自然封顶 |

> 注意：Phase 2 阶段可继续使用 Phase 1 的线性模型，因为低等级数值范围内两者差异不大。Phase 3 正式切换时，Golden Seed 基线需要显式更新。

**Kotlin 实现：**

```kotlin
object HitFormula {
    // Phase 1 线性模型，Phase 2 期间继续使用
    fun linearHitChance(accuracy: Int, evasion: Int): Double {
        return (0.85 + (accuracy - evasion) * 0.01).coerceIn(0.05, 0.95)
    }

    // Phase 3 Sigmoid 模型
    private const val K = 0.04        // 斜率系数
    private const val M = -10.0       // 中点偏移
    private const val MIN_HIT = 0.05
    private const val MAX_HIT = 0.95

    fun sigmoidHitChance(accuracy: Int, evasion: Int): Double {
        val d = (accuracy - evasion).toDouble()
        val sigmoid = 1.0 / (1.0 + kotlin.math.exp(-K * (d - M)))
        return (MIN_HIT + (MAX_HIT - MIN_HIT) * sigmoid).coerceIn(MIN_HIT, MAX_HIT)
    }
}
```

#### 3.2.2 伤害计算公式

伤害计算分为 **物理伤害** 和 **元素伤害** 两条路径，前者使用护甲减免模型，后者使用百分比抗性模型。

##### 3.2.2.1 基础伤害计算

```
rawDamage = baseDamage + randomVariance
```

其中：
- `baseDamage`：天赋/武器的基础伤害值 + 属性加成。对于近战普攻，`baseDamage = attack + str_scaling`。
- `randomVariance`：`random.nextInt(-variance, variance + 1)`，Phase 1 中 `variance = 2`，Phase 3 可提升为基于武器类型的比例方差。

##### 3.2.2.2 暴击系统

```
critChance = baseCritChance + dex * 0.002 + bonusCritChance
critChance = clamp(critChance, 0.0, 0.50)

// Phase 3 新增：暴击抗性
effectiveCritChance = max(0.0, critChance - targetCritResistance)

if (roll < effectiveCritChance):
    isCrit = true
    critMultiplier = baseCritMultiplier + bonusCritMultiplier
    // baseCritMultiplier = 1.5 (默认)
    // bonusCritMultiplier 来自天赋、装备
    damage = rawDamage * critMultiplier
else:
    damage = rawDamage
```

暴击相关属性定义：

| 属性 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `baseCritChance` | 0.05 (5%) | 固定 | 所有角色的基础暴击率 |
| `bonusCritChance` | 0.0 | 装备/天赋/buff | 额外暴击率加成 |
| `critResistance` | 0.0 | 装备/天赋/buff | 降低被暴击的概率 |
| `baseCritMultiplier` | 1.5 (150%) | 固定 | 暴击伤害倍率 |
| `bonusCritMultiplier` | 0.0 | 装备/天赋/buff | 额外暴击伤害倍率 |

##### 3.2.2.3 物理伤害减免（护甲模型）

物理伤害使用 **护甲减免** 模型，带收益递减：

```
effectiveArmor = max(0, targetArmor - attackerArmorPenetration)

// 收益递减函数（详见 3.5 节）
damageReduction = effectiveArmor / (effectiveArmor + K_armor)

// K_armor = 100 （护甲常数，控制减免曲线的陡度）
// 当 armor = 100 时，减免 50%
// 当 armor = 200 时，减免 66.7%
// 当 armor = 300 时，减免 75%

physicalDamageAfterArmor = rawDamage * (1 - damageReduction)
finalPhysicalDamage = max(1, floor(physicalDamageAfterArmor))
```

**护甲值减免对照表（`K_armor = 100`）：**

| 有效护甲值 | 减免率 | 100 点原始伤害 → 实际伤害 |
| --- | --- | --- |
| 0 | 0.0% | 100 |
| 25 | 20.0% | 80 |
| 50 | 33.3% | 67 |
| 75 | 42.9% | 57 |
| 100 | 50.0% | 50 |
| 150 | 60.0% | 40 |
| 200 | 66.7% | 33 |
| 300 | 75.0% | 25 |
| 500 | 83.3% | 17 |

##### 3.2.2.4 元素伤害减免（百分比抗性模型）

元素伤害使用 **百分比抗性** 模型：

```
effectiveResistance = clamp(targetResistance - attackerPenetration, -25, resistanceCap)
// resistanceCap = 75 （所有元素通道统一上限）
// 穿透可以把抗性打到负值，下限 -25（即最多增伤 25%）

elementalDamageAfterResistance = rawDamage * (1 - effectiveResistance / 100.0)
finalElementalDamage = max(1, floor(elementalDamageAfterResistance))
```

**元素抗性减免对照表：**

| 有效抗性 | 减免率 | 100 点原始伤害 → 实际伤害 |
| --- | --- | --- |
| -25 | -25.0%（增伤） | 125 |
| -10 | -10.0%（增伤） | 110 |
| 0 | 0.0% | 100 |
| 15 | 15.0% | 85 |
| 30 | 30.0% | 70 |
| 45 | 45.0% | 55 |
| 60 | 60.0% | 40 |
| 75 | 75.0%（上限） | 25 |

##### 3.2.2.5 综合伤害公式 Kotlin 实现

```kotlin
object DamageFormula {
    /** 护甲常数：护甲值等于此常数时，物理减免恰好 50% */
    const val K_ARMOR: Double = 100.0

    /** 元素抗性上限 */
    const val RESISTANCE_CAP: Int = 75

    /** 穿透后抗性下限（负值 = 增伤） */
    const val RESISTANCE_FLOOR: Int = -25

    /** 暴击基础倍率 */
    const val BASE_CRIT_MULTIPLIER: Double = 1.5

    /**
     * 计算物理伤害的护甲减免率。
     * @param armor 目标有效护甲值（已扣除穿透）
     * @return 减免率，范围 [0.0, 1.0)
     */
    fun armorDamageReduction(armor: Int): Double {
        val effectiveArmor = maxOf(0, armor)
        return effectiveArmor.toDouble() / (effectiveArmor.toDouble() + K_ARMOR)
    }

    /**
     * 计算元素伤害的抗性减免率。
     * @param resistance 目标原始抗性
     * @param penetration 攻击者穿透值
     * @return 减免率（可为负值，负值表示增伤），范围 [-0.25, 0.75]
     */
    fun elementalDamageReduction(resistance: Int, penetration: Int): Double {
        val effectiveResistance = (resistance - penetration).coerceIn(RESISTANCE_FLOOR, RESISTANCE_CAP)
        return effectiveResistance / 100.0
    }

    /**
     * 计算暴击后的伤害。
     * @param rawDamage 原始伤害
     * @param isCrit 是否暴击
     * @param bonusCritMultiplier 额外暴击倍率（来自装备/天赋）
     * @return 暴击后伤害（非暴击则原样返回）
     */
    fun applyCritical(rawDamage: Int, isCrit: Boolean, bonusCritMultiplier: Double = 0.0): Int {
        if (!isCrit) return rawDamage
        val totalMultiplier = BASE_CRIT_MULTIPLIER + bonusCritMultiplier
        return maxOf(1, (rawDamage * totalMultiplier).toInt())
    }

    /**
     * 计算最终伤害，根据伤害类型选择不同的减免模型。
     *
     * @param rawDamage 原始伤害（已含暴击）
     * @param damageType 伤害通道
     * @param targetArmor 目标护甲值
     * @param attackerArmorPen 攻击者护甲穿透
     * @param targetResistance 目标对应元素抗性
     * @param attackerElementPen 攻击者对应元素穿透
     * @return 最终伤害，至少为 1
     */
    fun calculateFinalDamage(
        rawDamage: Int,
        damageType: DamageType,
        targetArmor: Int = 0,
        attackerArmorPen: Int = 0,
        targetResistance: Int = 0,
        attackerElementPen: Int = 0,
    ): Int {
        return if (damageType == DamageType.PHYSICAL) {
            val effectiveArmor = maxOf(0, targetArmor - attackerArmorPen)
            val reduction = armorDamageReduction(effectiveArmor)
            maxOf(1, (rawDamage * (1.0 - reduction)).toInt())
        } else {
            val reduction = elementalDamageReduction(targetResistance, attackerElementPen)
            maxOf(1, (rawDamage * (1.0 - reduction)).toInt())
        }
    }
}
```

#### 3.2.3 结算顺序（Combat Resolution Pipeline）

战斗结算按以下 **严格有序的 12 步管线** 执行。每一步都可以被回调（callback）拦截或修改，回调的执行也遵循优先级顺序。

此管线适用于所有形式的伤害（近战/远程/技能/DoT），入口统一为 `CombatPipeline.resolve()`。

```
步骤 1  ─  命中判定（Hit Check）
│   输入：attacker.accuracy, target.evasion, RandomSource
│   输出：hit: Boolean
│   回调点：onPreHitCheck — 可修改 accuracy/evasion（如闪避 buff）
│   如果 miss → 触发 onMiss 回调 → 生成 MissEvent → 结束
│
步骤 2  ─  暴击判定（Critical Check）
│   输入：attacker.critChance, target.critResistance, RandomSource
│   输出：isCrit: Boolean, critMultiplier: Double
│   回调点：onPreCritCheck — 可修改 critChance（如暗杀加成）
│
步骤 3  ─  原始伤害计算（Raw Damage Calculation）
│   输入：baseDamage, damageType, critMultiplier, RandomSource
│   输出：rawDamage: Int（含暴击加成、含随机方差）
│   回调点：onPreDamageCalculation — 可乘以伤害系数（如天赋等级加成）
│
步骤 4  ─  前减免回调（Pre-Reduction Callbacks）
│   触发事件：onPreDamageApply
│   用途：
│     - 护盾吸收（Shield absorbs damage before armor/resistance）
│     - 伤害转换（将部分物理伤害转为神圣伤害）
│     - 伤害分摊（如替身术，将部分伤害转移给召唤物）
│   输出：可能修改 rawDamage 或 damageType
│
步骤 5  ─  护甲/抗性减免（Armor/Resistance Reduction）
│   输入：rawDamage, damageType, targetArmor/Resistance, attackerPenetration
│   输出：reducedDamage: Int
│   物理 → 护甲减免（收益递减模型）
│   元素 → 百分比抗性减免
│
步骤 6  ─  穿透应用（Penetration Application）
│   说明：穿透已在步骤 5 中作为减免计算的输入使用。
│   此步骤记录穿透的具体贡献值到 CombatTrace。
│   计算：ignoredArmor = min(targetArmor, attackerArmorPen)
│        或 ignoredResistance = min(targetResistance, attackerElementPen)
│
步骤 7  ─  后减免回调（Post-Reduction Callbacks）
│   触发事件：onPostDamageReduction
│   用途：
│     - 固定减伤（如减免最终伤害的 X 点）
│     - 条件减伤（如 HP 低于 20% 时减伤 30%）
│     - 最终伤害倍率（如脆弱状态增伤 25%）
│   输出：可能修改 reducedDamage
│
步骤 8  ─  最终伤害应用（Final Damage Application）
│   输入：finalDamage（经所有减免和回调后的值）
│   执行：target.health.current -= finalDamage
│   保证：finalDamage >= 1（只要命中就至少造成 1 点伤害）
│   回调点：onDamageApplied — 通知所有监听者最终实际造成的伤害
│
步骤 9  ─  受伤后回调（On-Damage-Taken Callbacks）
│   触发事件：onDamageTaken（在目标上）
│   用途：
│     - 荆棘反伤（受到近战伤害时反弹 X% 伤害给攻击者）
│     - 受击回复（受伤时回复正能量）
│     - 破甲触发（受到物理伤害时有概率触发破甲状态）
│     - 状态施加（对应伤害通道的关联状态效果检定）
│   输出：可能触发次生伤害或状态效果（次生伤害走新的 Pipeline）
│
步骤 10 ─  死亡检查（Death Check）
│   输入：target.health.current
│   如果 <= 0 → 触发 onPreDeath 回调（可被拦截，如不死鸟天赋）
│   如果死亡确认 → 设置 target 状态为 DEAD → 触发 onDeath 事件
│
步骤 11 ─  击杀回调（On-Kill Callbacks）
│   触发事件：onKill（在攻击者上）
│   用途：
│     - 击杀回复（击杀时回复 HP/资源）
│     - 击杀链（连续击杀增加攻击力）
│     - 经验获取触发
│   前提：步骤 10 确认目标死亡
│
步骤 12 ─  经验与掉落（Experience & Loot）
│   执行：如果目标死亡且攻击者为玩家
│     - 计算经验值并触发 ExperienceGainedEvent
│     - 检查升级并触发 LevelUpEvent
│     - 生成掉落物并放置在地图上
│   此步骤不可被回调拦截
```

**管线的设计约束：**

1. 管线是 **同步执行** 的，不存在异步步骤。
2. 次生伤害（如荆棘反伤）会创建一个 **新的 Pipeline 实例**，但共享同一回合的 `CombatTrace` 上下文。
3. 回调的优先级由 `CallbackPriority`（`Int`）决定，数值越小越先执行。同优先级按 `entityId` 升序排列。
4. 回调返回 `CANCEL` 会中止当前步骤的后续回调执行，但不会跳过管线的下一步。只有 `ABSORB` 会完全终止管线（例如护盾完全吸收了伤害）。

### 3.3 战斗追踪系统（CombatTrace）

`CombatTrace` 记录一次战斗结算管线的每一步计算过程，用于：

1. **调试**：开发时追踪伤害计算是否正确。
2. **死因分析**：Phase 5 的死因回溯功能依赖此数据。
3. **Tooltip 展示**：鼠标悬停在战斗日志上时，展示详细的伤害分解。
4. **平衡调优**：统计分析 DPS、减免率、穿透贡献等指标。
5. **Golden Seed 回归**：固定 seed 下比对 Trace 的关键数值是否变化。

#### 3.3.1 数据结构

```kotlin
package com.ktome.core.combat

import com.ktome.core.ecs.EntityId

/**
 * 一次完整战斗结算的追踪记录。
 * 从 Pipeline 入口到最终伤害应用的每一步都被记录。
 */
data class CombatTrace(
    /** 唯一追踪 ID，用于关联次生伤害 */
    val traceId: String,
    /** 所属回合号 */
    val turn: Int,
    /** 攻击者实体 ID */
    val attackerId: EntityId,
    /** 目标实体 ID */
    val targetId: EntityId,
    /** 触发此次结算的能力 ID */
    val abilityId: String,
    /** 伤害通道 */
    val damageType: DamageType,
    /** 管线各步骤的记录 */
    val steps: List<TraceStep>,
    /** 最终结果摘要 */
    val result: TraceResult,
    /** 如果此次结算触发了次生伤害，记录子 Trace 的 ID */
    val childTraceIds: List<String> = emptyList(),
)

/**
 * 管线中一步的记录。
 */
data class TraceStep(
    /** 步骤编号（1~12） */
    val stepIndex: Int,
    /** 步骤名称（如 "HIT_CHECK", "CRIT_CHECK" 等） */
    val stepName: String,
    /** 该步骤的输入参数快照 */
    val inputs: Map<String, String>,
    /** 该步骤的输出结果 */
    val outputs: Map<String, String>,
    /** 该步骤中触发的回调及其效果 */
    val callbacks: List<CallbackRecord> = emptyList(),
)

/**
 * 回调执行的记录。
 */
data class CallbackRecord(
    /** 回调所有者实体 ID */
    val ownerId: EntityId,
    /** 回调名称（如天赋 ID、装备 ID） */
    val callbackName: String,
    /** 回调优先级 */
    val priority: Int,
    /** 回调返回值 */
    val result: String, // "CONTINUE", "CANCEL", "ABSORB"
    /** 回调产生的数值变更描述 */
    val effect: String?, // 如 "shield absorbed 15 damage"
)

/**
 * 结算结果摘要。
 */
data class TraceResult(
    /** 是否命中 */
    val hit: Boolean,
    /** 是否暴击 */
    val critical: Boolean = false,
    /** 暴击倍率（非暴击时为 1.0） */
    val critMultiplier: Double = 1.0,
    /** 原始伤害（步骤 3 输出） */
    val rawDamage: Int = 0,
    /** 护盾/前减免吸收量（步骤 4） */
    val preReductionAbsorbed: Int = 0,
    /** 护甲/抗性减免量（步骤 5~6） */
    val armorResistanceReduced: Int = 0,
    /** 穿透贡献量（因穿透而多造成的伤害） */
    val penetrationContribution: Int = 0,
    /** 后减免修正量（步骤 7，正值为减伤，负值为增伤） */
    val postReductionModifier: Int = 0,
    /** 最终实际造成的伤害 */
    val finalDamage: Int = 0,
    /** 目标是否死亡 */
    val targetKilled: Boolean = false,
    /** 死亡是否被拦截（如不死鸟天赋） */
    val deathPrevented: Boolean = false,
)
```

#### 3.3.2 Trace 输出示例

以下是一个典型的 `CombatTrace` 序列化输出示例（JSON 格式，用于调试日志）：

```json
{
  "traceId": "t-0042",
  "turn": 15,
  "attackerId": 1,
  "targetId": 7,
  "abilityId": "fireball",
  "damageType": "FIRE",
  "steps": [
    {
      "stepIndex": 1,
      "stepName": "HIT_CHECK",
      "inputs": {"accuracy": "22", "evasion": "10", "hitFormula": "SIGMOID"},
      "outputs": {"hitChance": "0.91", "roll": "0.34", "hit": "true"}
    },
    {
      "stepIndex": 2,
      "stepName": "CRIT_CHECK",
      "inputs": {"critChance": "0.12", "critResistance": "0.02"},
      "outputs": {"effectiveCritChance": "0.10", "roll": "0.65", "isCrit": "false"}
    },
    {
      "stepIndex": 3,
      "stepName": "RAW_DAMAGE",
      "inputs": {"baseDamage": "35", "variance": "2", "critMultiplier": "1.0"},
      "outputs": {"varianceRoll": "-1", "rawDamage": "34"}
    },
    {
      "stepIndex": 4,
      "stepName": "PRE_REDUCTION_CALLBACKS",
      "inputs": {"rawDamage": "34"},
      "outputs": {"modifiedDamage": "34"},
      "callbacks": []
    },
    {
      "stepIndex": 5,
      "stepName": "RESISTANCE_REDUCTION",
      "inputs": {"targetFireRes": "20", "attackerFirePen": "8"},
      "outputs": {"effectiveResistance": "12", "reductionRate": "0.12", "reducedDamage": "30"}
    },
    {
      "stepIndex": 6,
      "stepName": "PENETRATION_RECORD",
      "inputs": {"rawResistance": "20", "penetration": "8"},
      "outputs": {"ignoredResistance": "8", "penContribution": "3"}
    },
    {
      "stepIndex": 7,
      "stepName": "POST_REDUCTION_CALLBACKS",
      "inputs": {"reducedDamage": "30"},
      "outputs": {"finalDamage": "30"},
      "callbacks": []
    },
    {
      "stepIndex": 8,
      "stepName": "DAMAGE_APPLICATION",
      "inputs": {"finalDamage": "30", "targetHpBefore": "65"},
      "outputs": {"targetHpAfter": "35"}
    },
    {
      "stepIndex": 9,
      "stepName": "ON_DAMAGE_TAKEN_CALLBACKS",
      "inputs": {"damageType": "FIRE", "finalDamage": "30"},
      "outputs": {},
      "callbacks": [
        {
          "ownerId": 7,
          "callbackName": "burn_application",
          "priority": 100,
          "result": "CONTINUE",
          "effect": "BURN applied for 3 turns, 10 damage/turn"
        }
      ]
    },
    {
      "stepIndex": 10,
      "stepName": "DEATH_CHECK",
      "inputs": {"targetHp": "35"},
      "outputs": {"isDead": "false"}
    }
  ],
  "result": {
    "hit": true,
    "critical": false,
    "critMultiplier": 1.0,
    "rawDamage": 34,
    "preReductionAbsorbed": 0,
    "armorResistanceReduced": 4,
    "penetrationContribution": 3,
    "postReductionModifier": 0,
    "finalDamage": 30,
    "targetKilled": false,
    "deathPrevented": false
  },
  "childTraceIds": []
}
```

### 3.4 Power/Save 对抗体系

Power/Save 体系用于判定 **状态效果是否成功施加到目标身上**。它是独立于命中判定的第二层检定，体现了"你可以命中敌人但不一定能眩晕它"的设计意图。

#### 3.4.1 三维度定义

K-ToME 定义三个对抗维度，每个维度包含一个攻击方属性（Power）和一个防御方属性（Save）：

| 维度 | 攻击方属性 | 防御方属性 | 主要用途 |
| --- | --- | --- | --- |
| 物理（Physical） | Physical Power | Physical Save | 击退、流血、破甲、眩晕（物理来源） |
| 精神（Mental） | Mental Power | Mental Save | 恐惧、混乱、魅惑、士气打击 |
| 法术（Spell） | Spell Power | Spell Save | 燃烧、冰冻、中毒、诅咒、元素状态 |

#### 3.4.2 属性来源

```
Physical Power = 10 + STR * 1.5 + level * 0.5 + equipment/talent bonus
Physical Save  = 10 + CON * 1.5 + level * 0.5 + equipment/talent bonus

Mental Power   = 10 + WIL * 1.5 + level * 0.5 + equipment/talent bonus
Mental Save    = 10 + WIL * 1.0 + CON * 0.5 + level * 0.5 + equipment/talent bonus

Spell Power    = 10 + WIL * 1.5 + level * 0.5 + equipment/talent bonus
Spell Save     = 10 + WIL * 1.0 + CON * 0.5 + level * 0.5 + equipment/talent bonus
```

说明：
- `level` 指角色等级，提供随等级的自然增长。
- Save 通常同时受 CON（体质/韧性）和 WIL（意志）影响，体现韧性角色的天然抗性。
- Power 主要受对应攻击属性影响（STR 用于物理控制，WIL 用于法术/精神控制）。
- 怪物的 Power/Save 在怪物模板中直接定义，不通过属性公式计算。

#### 3.4.3 状态施加判定公式

当一个能力试图对目标施加状态效果时：

```
令 power = 攻击方对应维度的 Power
令 save  = 防御方对应维度的 Save
令 d = power - save

// 使用与命中判定类似的 Sigmoid 模型，但参数不同
k_save = 0.05          // 比命中判定更陡，对抗差距的敏感度更高
m_save = 0.0           // 中点不偏移，power = save 时施加率为 50%

sigmoid(d) = 1 / (1 + e^(-k_save * d))

applyChance = 0.10 + 0.80 * sigmoid(d)
// 范围 [0.10, 0.90]
// 最低 10% 基础施加率（保留英雄一击的可能性）
// 最高 90% 施加率上限（保留目标抵抗的可能性）

applyChance = clamp(applyChance, 0.10, 0.90)

if (roll < applyChance):
    状态效果成功施加
else:
    状态效果被抵抗
```

**施加率对照表（`k_save = 0.05`, `m_save = 0`）：**

| `power - save` | 施加率 | 说明 |
| --- | --- | --- |
| -40 | 0.12 | 大幅劣势，接近下限 |
| -20 | 0.22 | 明显劣势 |
| -10 | 0.33 | 轻微劣势 |
| 0 | 0.50 | 五五开 |
| 10 | 0.67 | 轻微优势 |
| 20 | 0.78 | 明显优势 |
| 40 | 0.88 | 大幅优势，接近上限 |

#### 3.4.4 每种状态效果的对抗维度映射

| 状态效果 | 对抗维度 | 来源示例 |
| --- | --- | --- |
| `STUN`（眩晕） | Physical | 战卫重击、闪电术 |
| `KNOCKBACK`（击退） | Physical | 战卫盾击 |
| `ARMOR_BREAK`（破甲） | Physical | 战卫撕裂、游荡者弱点打击 |
| `BLEED`（流血） | Physical | 战卫撕裂、游荡者割喉 |
| `ROOT`（定身） | Physical / Spell | 冰系定身（Spell）、藤蔓缠绕（Spell） |
| `SLOW`（减速） | Spell | 冰系天赋 |
| `FREEZE`（冰冻） | Spell | 冰系天赋高级版 |
| `BURN`（燃烧） | Spell | 火系天赋 |
| `POISON`（中毒） | Spell | 暗影系/游荡者 |
| `CURSE`（诅咒） | Spell | 暗影系天赋 |
| `WEAKEN`（虚弱） | Mental | 暗影系精神打击 |
| `SILENCE`（沉默） | Mental | 精神系天赋 |
| `BLIND`（致盲） | Physical / Spell | 闪光术（Spell）、抛沙（Physical） |
| `CONFUSE`（混乱） | Mental | 精神系天赋 |
| `FEAR`（恐惧） | Mental | 暗影系/Boss 技能 |
| `BANE`（驱邪标记） | Spell | 圣堂武士天赋（对亡灵/恶魔自动成功） |

#### 3.4.5 Kotlin 实现

```kotlin
/**
 * Power/Save 对抗维度。
 */
enum class SaveDimension {
    PHYSICAL,
    MENTAL,
    SPELL,
}

/**
 * 扩展的属性集，包含 Power/Save 值。
 * 在 Phase 3 引入，作为 DerivedStats 的扩展。
 */
data class PowerSaveStats(
    val physicalPower: Int = 10,
    val physicalSave: Int = 10,
    val mentalPower: Int = 10,
    val mentalSave: Int = 10,
    val spellPower: Int = 10,
    val spellSave: Int = 10,
)

object PowerSaveFormula {
    private const val K_SAVE = 0.05
    private const val MIN_APPLY = 0.10
    private const val MAX_APPLY = 0.90

    /**
     * 计算状态效果的施加概率。
     * @param power 攻击方的 Power 值
     * @param save  防御方的 Save 值
     * @return 施加概率，范围 [0.10, 0.90]
     */
    fun applyChance(power: Int, save: Int): Double {
        val d = (power - save).toDouble()
        val sigmoid = 1.0 / (1.0 + kotlin.math.exp(-K_SAVE * d))
        return (MIN_APPLY + (MAX_APPLY - MIN_APPLY) * sigmoid).coerceIn(MIN_APPLY, MAX_APPLY)
    }

    /**
     * 计算 Power/Save 属性值。
     */
    fun calculatePhysicalPower(str: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (str * 1.5).toInt() + (level * 0.5).toInt() + bonus
    }

    fun calculatePhysicalSave(con: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (con * 1.5).toInt() + (level * 0.5).toInt() + bonus
    }

    fun calculateMentalPower(wil: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (wil * 1.5).toInt() + (level * 0.5).toInt() + bonus
    }

    fun calculateMentalSave(wil: Int, con: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (wil * 1.0).toInt() + (con * 0.5).toInt() + (level * 0.5).toInt() + bonus
    }

    fun calculateSpellPower(wil: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (wil * 1.5).toInt() + (level * 0.5).toInt() + bonus
    }

    fun calculateSpellSave(wil: Int, con: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (wil * 1.0).toInt() + (con * 0.5).toInt() + (level * 0.5).toInt() + bonus
    }
}
```

### 3.5 收益递减模型

#### 3.5.1 问题背景

在 Phase 1 的当前实现中，所有属性都是线性叠加的。这在低数值区间可以工作，但在 Phase 3 以后随着装备、天赋、buff 的叠加，会导致：

- **防御无限堆叠**：护甲值越高减免越多，到一定程度后变得几乎无敌。
- **命中/闪避极化**：极端的 accuracy/evasion 差距导致"100% 命中"或"永远 miss"。
- **属性同质化**：既然每 1 点属性的边际收益相同，最优策略永远是堆单一属性到极限。

收益递减模型的目的是：让属性在低值区间高效增长、在高值区间逐渐放缓，鼓励均衡发展。

#### 3.5.2 数学模型

K-ToME 选择 **双曲线模型**（Hyperbolic Diminishing Returns），其形式为：

```
effectiveValue = rawValue * C / (rawValue + C)
```

其中 `C` 是半收益常数（half-value constant）：当 `rawValue = C` 时，`effectiveValue = C/2`，即收益恰好为理论最大值的 50%。

**模型特性：**

- `rawValue → 0` 时，`effectiveValue ≈ rawValue`（低值区间近似线性）。
- `rawValue → ∞` 时，`effectiveValue → C`（渐近上限为 C）。
- 曲线平滑，没有突变点。

不同的属性使用不同的 `C` 值，以控制各自的递减速率。

#### 3.5.3 各属性的收益递减参数

| 属性类别 | `C`（半收益常数） | 设计意图 | 适用阶段 |
| --- | --- | --- | --- |
| 护甲（Armor） | 不直接使用此模型 | 物理减免使用 `armor / (armor + 100)` 模型，见 3.2.2.3 节 | P3 |
| 元素抗性 | 直接使用百分比 | 硬上限 75%，不用收益递减 | P3 |
| 闪避（Evasion） | 150 | 150 点闪避时提供 75 有效闪避，用于 Sigmoid 命中公式的输入 | P3 |
| 暴击率加成 | 200 | 200 点暴击评级时提供 100 有效评级，防止暴击率膨胀 | P3 |
| 施法速度 | 100 | 100 点加速时提供 50% 的减 CD 效果 | P4 |
| 生命回复 | 80 | 80 点回复评级时提供 40 有效值，防止回复过高 | P3 |

> **重要**：主属性（STR/DEX/CON/WIL）本身 **不** 应用收益递减。收益递减应用在由主属性 **派生的二级属性** 上。这是因为主属性的增长速度受等级和装备自然约束，不需要额外的递减机制。

#### 3.5.4 关键断点对照表

以闪避（`C = 150`）为例：

| 原始值（Raw） | 有效值（Effective） | 边际收益率 | 说明 |
| --- | --- | --- | --- |
| 10 | 9.4 | 93.8% | 低值区间，几乎线性 |
| 20 | 17.6 | 88.2% | 接近线性 |
| 50 | 37.5 | 75.0% | 开始出现明显递减 |
| 100 | 60.0 | 60.0% | 已有 40% 的损失 |
| 150 | 75.0 | 50.0% | 半收益点 |
| 200 | 85.7 | 42.9% | 收益显著下降 |
| 300 | 100.0 | 33.3% | 三分之二被吞噬 |
| 500 | 115.4 | 23.1% | 极端堆叠，绝大部分被浪费 |

以暴击评级（`C = 200`）为例：

| 原始值（Raw） | 有效值（Effective） | 边际收益率 |
| --- | --- | --- |
| 10 | 9.5 | 95.2% |
| 20 | 18.2 | 90.9% |
| 50 | 40.0 | 80.0% |
| 100 | 66.7 | 66.7% |
| 200 | 100.0 | 50.0% |
| 300 | 120.0 | 40.0% |
| 500 | 142.9 | 28.6% |

#### 3.5.5 Kotlin 实现

```kotlin
object DiminishingReturns {
    /**
     * 双曲线收益递减函数。
     *
     * @param rawValue 原始累加值（来自属性、装备、天赋、buff 的总和）
     * @param halfValueConstant 半收益常数 C：rawValue = C 时，有效值 = C/2
     * @return 经过收益递减后的有效值
     */
    fun hyperbolic(rawValue: Double, halfValueConstant: Double): Double {
        require(halfValueConstant > 0) { "Half-value constant must be positive" }
        if (rawValue <= 0.0) return 0.0
        return rawValue * halfValueConstant / (rawValue + halfValueConstant)
    }

    /**
     * 整数版本的收益递减函数，四舍五入到最近整数。
     */
    fun hyperbolic(rawValue: Int, halfValueConstant: Int): Int {
        if (rawValue <= 0) return 0
        return (rawValue.toDouble() * halfValueConstant / (rawValue + halfValueConstant))
            .toInt()
    }

    // ── 预定义的各属性收益递减参数 ──

    /** 闪避收益递减：C = 150 */
    fun effectiveEvasion(rawEvasion: Int): Int = hyperbolic(rawEvasion, 150)

    /** 暴击评级收益递减：C = 200 */
    fun effectiveCritRating(rawCritRating: Int): Int = hyperbolic(rawCritRating, 200)

    /** 施法速度收益递减：C = 100 */
    fun effectiveCastSpeed(rawCastSpeed: Int): Int = hyperbolic(rawCastSpeed, 100)

    /** 生命回复收益递减：C = 80 */
    fun effectiveHpRegen(rawHpRegen: Int): Int = hyperbolic(rawHpRegen, 80)

    /**
     * 计算当前原始值下，再增加 1 点的边际收益。
     * 可用于 Tooltip 显示"下一点属性的价值"。
     */
    fun marginalValue(rawValue: Int, halfValueConstant: Int): Double {
        val current = hyperbolic(rawValue.toDouble(), halfValueConstant.toDouble())
        val next = hyperbolic((rawValue + 1).toDouble(), halfValueConstant.toDouble())
        return next - current
    }
}
```

#### 3.5.6 收益递减与战斗公式的集成

收益递减模型在战斗管线中的集成点：

1. **命中判定（步骤 1）**：闪避值在传入 `HitFormula.sigmoidHitChance()` 之前，先经过 `DiminishingReturns.effectiveEvasion()` 转换。Accuracy 不做递减（鼓励进攻方堆命中来打高闪避目标）。

2. **暴击判定（步骤 2）**：暴击评级在转换为暴击率之前，先经过 `DiminishingReturns.effectiveCritRating()` 转换。

3. **护甲减免（步骤 5）**：物理伤害的护甲模型 `armor / (armor + K)` 本身就是双曲线形式，自带收益递减效果，不需要额外套一层 DR 函数。

4. **元素抗性（步骤 5）**：硬上限 75% 提供了隐式的收益递减（因为接近上限时每 1% 的来源越来越难获得），不需要 DR 函数。

5. **生命回复（回合间结算）**：回复值在实际执行回复之前，经过 `DiminishingReturns.effectiveHpRegen()` 转换。

---

## 4. 元素系统详细设计

### 4.1 六大伤害通道定义

本节是对第 3.1 节伤害通道的元素维度补充，重点关注 UI 表现、内容绑定和跨系统引用。

#### 4.1.1 完整通道定义表

| ID | `nameKey`（中） | `nameKey`（英） | 色值（Hex） | UI 色彩说明 | 主要使用职业 | 主要使用怪物类别 |
| --- | --- | --- | --- | --- | --- | --- |
| `PHYSICAL` | `元素.物理` | `element.physical` | `#C0C0C0` | 银灰色，金属质感 | 战卫、游荡者 | 野兽、土匪、构装体 |
| `FIRE` | `元素.火焰` | `element.fire` | `#FF4500` | 橙红色，火焰 | 奥术师（火系） | 火元素、熔岩兽、赤龙 |
| `COLD` | `元素.寒冰` | `element.cold` | `#00BFFF` | 冰蓝色，霜冻 | 奥术师（冰系） | 冰元素、霜巨人、白龙 |
| `LIGHTNING` | `元素.闪电` | `element.lightning` | `#FFD700` | 金黄色，电弧 | 奥术师（雷系） | 雷元素、风暴构装体 |
| `HOLY` | `元素.神圣` | `element.holy` | `#FFFACD` | 淡金色，光辉 | 圣堂武士 | 堕落天使、光明构装体 |
| `SHADOW` | `元素.暗影` | `element.shadow` | `#8B008B` | 深紫色，腐蚀 | 影刃客（进阶） | 亡灵、恶魔、暗影生物 |

#### 4.1.2 各通道关联 Debuff 详表

| 通道 | 关联 Debuff | Debuff `statusKey` | 效果描述 | 默认持续回合 | 对抗维度 |
| --- | --- | --- | --- | --- | --- |
| `PHYSICAL` | 流血 | `BLEED` | 每回合损失原始伤害的 15% | 3 | Physical |
| `PHYSICAL` | 破甲 | `ARMOR_BREAK` | 降低目标护甲 30% | 4 | Physical |
| `PHYSICAL` | 击退 | `KNOCKBACK` | 将目标推后 1~3 格 | 瞬时 | Physical |
| `PHYSICAL` | 眩晕 | `STUN` | 目标无法行动 | 1~2 | Physical |
| `FIRE` | 燃烧 | `BURN` | 每回合损失原始伤害的 20% | 3 | Spell |
| `COLD` | 减速 | `SLOW` | 降低目标速度 30% | 3 | Spell |
| `COLD` | 冰冻 | `FREEZE` | 目标无法行动，受攻击时解除并承受碎冰伤害 | 2 | Spell |
| `LIGHTNING` | 眩晕 | `STUN` | 短时间无法行动 | 1 | Spell |
| `LIGHTNING` | 过载 | `OVERCHARGE` | 下次受到闪电伤害 +25% | 2 | Spell |
| `HOLY` | 净化 | `PURIFY` | 移除目标 1 个负面状态（对友方）或驱散 1 个增益（对敌方） | 瞬时 | Spell |
| `HOLY` | 驱邪标记 | `BANE` | 对亡灵/恶魔额外增伤 50% | 4 | Spell |
| `SHADOW` | 中毒 | `POISON` | 每回合损失原始伤害的 10%，并降低治疗效果 30% | 5 | Spell |
| `SHADOW` | 诅咒 | `CURSE` | 降低目标所有主属性 10% | 4 | Spell |
| `SHADOW` | 虚弱 | `WEAKEN` | 降低目标攻击力 20% | 3 | Mental |

#### 4.1.3 抗性属性命名规范

为了在 `StatModifier`、`DerivedStats`、装备词条、Tooltip 中保持一致，抗性和穿透属性的命名遵循以下规范：

```kotlin
// 抗性属性名 = "{element}Resistance"
// 穿透属性名 = "{element}Penetration"
// 物理例外：armor / armorPenetration

// Phase 3 扩展后的 StatModifier（示意）
data class ExtendedStatModifier(
    // ... 继承现有字段 ...

    // 护甲与物理穿透
    val armor: Int = 0,
    val armorPenetration: Int = 0,

    // 元素抗性
    val fireResistance: Int = 0,
    val coldResistance: Int = 0,
    val lightningResistance: Int = 0,
    val holyResistance: Int = 0,
    val shadowResistance: Int = 0,

    // 元素穿透
    val firePenetration: Int = 0,
    val coldPenetration: Int = 0,
    val lightningPenetration: Int = 0,
    val holyPenetration: Int = 0,
    val shadowPenetration: Int = 0,

    // 全元素抗性/穿透（便捷字段，同时加到所有 5 个元素通道）
    val allElementalResistance: Int = 0,
    val allElementalPenetration: Int = 0,
)
```

### 4.2 抗性系统

#### 4.2.1 设计决策

K-ToME 的抗性系统采用 **百分比减免模型**，而非扁平值减免。这意味着：

- `30% 火焰抗性` = 受到的火焰伤害减少 30%
- 抗性值直接对应减免百分比，1 点 = 1%
- 这使得抗性的理解非常直观

**与物理防御（护甲）的区别**：物理伤害使用护甲的收益递减模型（`armor / (armor + 100)`），而元素伤害使用直接百分比抗性。这是故意的设计差异：

- 物理伤害是最常见的伤害来源，需要更精细的收益递减控制。
- 元素伤害是特化来源，通过硬上限 + 稀缺性控制数值。

#### 4.2.2 抗性值范围与上限

| 参数 | 值 | 说明 |
| --- | --- | --- |
| 默认抗性 | 0 | 角色出生时各元素抗性均为 0 |
| 抗性上限 | 75 | 任何角色的单项元素抗性不得超过 75%（穿透扣除前） |
| 抗性下限（穿透后） | -25 | 穿透可以把有效抗性打到负值，但下限为 -25%（增伤 25%） |
| 抗性下限（自然） | 无下限 | 某些怪物可以有负的自然抗性（如亡灵 `holyResistance = -25`） |

#### 4.2.3 抗性来源

抗性可以来自以下来源，按计算顺序叠加：

| 优先级 | 来源类型 | 示例 | 叠加方式 |
| --- | --- | --- | --- |
| 1 | 种族固有抗性 | 矮人 `fireResistance = 10` | 加法 |
| 2 | 怪物模板抗性 | 火元素 `fireResistance = 50, coldResistance = -20` | 加法 |
| 3 | 装备抗性 | 抗火戒指 `fireResistance = 15` | 加法 |
| 4 | 天赋被动抗性 | 奥术护盾 `allElementalResistance = 5` | 加法 |
| 5 | Buff 临时抗性 | 抗火药水 `fireResistance = 20, duration = 10` | 加法 |
| 6 | 地形/区域抗性 | 火焰祭坛区域 `fireResistance = -10`（降低抗性） | 加法 |

所有来源加法叠加后，上限裁切到 `resistanceCap`（默认 75）。

#### 4.2.4 抗性减免公式

```
总抗性 = sum(所有来源的抗性加成)
上限裁切后抗性 = min(总抗性, resistanceCap)  // resistanceCap = 75
有效抗性 = max(上限裁切后抗性 - 穿透值, resistanceFloor)  // resistanceFloor = -25

减免率 = 有效抗性 / 100.0
// 减免率为正 → 减伤
// 减免率为负 → 增伤

最终伤害 = max(1, floor(原始伤害 * (1 - 减免率)))
```

#### 4.2.5 典型场景计算示例

**场景 1**：玩家（`fireResistance = 30`）被火球术击中，原始火焰伤害 50，攻击者无火焰穿透。

```
有效抗性 = max(30 - 0, -25) = 30
减免率 = 30 / 100 = 0.30
最终伤害 = max(1, floor(50 * (1 - 0.30))) = max(1, 35) = 35
```

**场景 2**：亡灵（`holyResistance = -25`）被神圣打击命中，原始神圣伤害 40，攻击者有 `holyPenetration = 10`。

```
总抗性 = -25
上限裁切 = min(-25, 75) = -25
有效抗性 = max(-25 - 10, -25) = -25  // 已触底，穿透无额外效果
减免率 = -25 / 100 = -0.25  // 增伤 25%
最终伤害 = max(1, floor(40 * (1 - (-0.25)))) = max(1, floor(40 * 1.25)) = 50
```

**场景 3**：矮人战士（种族 `fireRes=10` + 装备 `fireRes=30` + buff `fireRes=40` = 总 80），上限裁切后 75。被火焰攻击（原始伤害 100），攻击者 `firePenetration = 20`。

```
总抗性 = 10 + 30 + 40 = 80
上限裁切 = min(80, 75) = 75
有效抗性 = max(75 - 20, -25) = 55
减免率 = 55 / 100 = 0.55
最终伤害 = max(1, floor(100 * 0.45)) = 45
```

### 4.3 穿透系统

#### 4.3.1 设计原则

穿透是 **进攻方降低防御方有效抗性/护甲** 的手段。它与抗性/护甲形成攻防博弈的核心轴：

- 防御方堆抗性来减伤 → 进攻方堆穿透来无视抗性
- 穿透过高时可以把有效抗性打到负值 → 形成增伤效果

#### 4.3.2 物理穿透（Armor Penetration）

```
有效护甲 = max(0, 目标护甲 - 攻击者护甲穿透)
// 护甲穿透不能把护甲打到负值（物理伤害不存在"负护甲增伤"的概念）
// 穿透完全忽略对应数值的护甲，护甲模型的减免随之降低
```

物理穿透的来源：
- 武器特性（如长矛穿透 +10）
- 天赋效果（如战卫"碎甲"天赋提供 +15 护甲穿透）
- 装备词条（如 `prefix: armor_piercing, armorPenetration: +8`）

物理穿透无上限，但护甲不会被打到负值，所以穿透超过目标护甲后就无收益。

#### 4.3.3 元素穿透（Elemental Penetration）

```
有效抗性 = max(抗性下限, 上限裁切后抗性 - 元素穿透)
// 抗性下限 = -25
// 元素穿透 CAN 把抗性打到负值（这是元素穿透的核心价值）
```

元素穿透的来源：
- 天赋效果（如奥术师"元素精通"被动提供 `firePenetration +10`）
- 装备词条（如 `suffix: of_scorching, firePenetration: +5`）
- 状态效果（如 `OVERCHARGE` 对闪电穿透的间接贡献）
- 闪电通道固有穿透加成（闪电天赋的穿透值天然比其他元素高 5~10 点）

#### 4.3.4 穿透贡献计算

在 `CombatTrace` 中，穿透贡献量的计算方式：

```
// 对于物理穿透
ignoredArmor = min(目标护甲, 攻击者护甲穿透)
穿透贡献伤害 = 无穿透时的最终伤害 - 有穿透时的最终伤害
// 即：穿透让你多造成了多少伤害

// 对于元素穿透
ignoredResistance = min(上限裁切后抗性 - 抗性下限, 元素穿透)
穿透贡献伤害 = 无穿透时的最终伤害 - 有穿透时的最终伤害
```

#### 4.3.5 穿透平衡参考值

以下是各阶段的穿透值参考范围，用于指导装备和天赋的数值设计：

| 阶段 | 护甲穿透范围 | 元素穿透范围 | 说明 |
| --- | --- | --- | --- |
| Phase 2 | 0~10 | 0~5 | 基础阶段，穿透来源有限 |
| Phase 3 早期 | 5~20 | 5~15 | 天赋树提供主要穿透来源 |
| Phase 3 后期 | 10~35 | 10~25 | 高级天赋 + 装备组合 |
| Phase 4 | 15~50 | 15~35 | Unique/Artifact 装备提供极端穿透 |
| Phase 5（满构筑） | 25~60 | 20~40 | 理论最大值，需要专精构筑才能达到 |

### 4.4 元素交互规则

元素交互提供了超越纯数值计算的战术层面。以下规则在 Phase 3~4 逐步引入。

#### 4.4.1 交互矩阵

| 攻击元素 | 交互条件 | 效果 | 引入阶段 |
| --- | --- | --- | --- |
| `FIRE` | 目标处于 `FREEZE` 状态 | 立即解除 `FREEZE`，触发"蒸发"效果：两种伤害的 **较大者** 额外增伤 30% | P3-A |
| `COLD` | 目标处于 `BURN` 状态 | 立即解除 `BURN`，触发"淬灭"效果：目标减速 50% 持续 2 回合 | P3-A |
| `LIGHTNING` | 目标站在水地形上 | 闪电伤害 +25%，且传导到相邻水地形上的所有实体（半伤） | P4-A |
| `FIRE` | 目标站在油地形上 | 油地形燃烧 3 回合，每回合对站立者造成火焰伤害 | P4-A |
| `COLD` | 作用于水地形 | 水地形冻结为冰面 3 回合，冰面上移动需要闪避检定否则摔倒（失去 1 回合） | P4-A |
| `HOLY` | 目标为亡灵或恶魔标签 | 伤害自动 +50%（不占穿透预算，是额外乘算） | P3-A |
| `SHADOW` | 目标处于 `BANE` 标记 | 额外造成 `BANE` 剩余回合数 × 5 的暗影伤害，然后消耗 `BANE` | P3-B |
| `HOLY` + `SHADOW` | 同一回合内对同一目标 | 互相抵消各自的持续效果（`BANE` 消耗 `CURSE`，`CURSE` 消耗 `BANE`），剩余部分仍然生效 | P3-B |

#### 4.4.2 交互的实现原则

1. **元素交互在战斗管线的步骤 9（受伤后回调）中触发**，作为 `onDamageTaken` 回调的一部分。
2. 交互效果是 **确定性的**——不需要额外的随机判定，只要条件满足就触发。
3. 交互产生的次生伤害走 **新的 CombatPipeline 实例**，有独立的 `CombatTrace`。
4. 交互链的深度限制为 **2 层**，防止无限递归（例如火 → 蒸发 → 产生的蒸汽伤害不再触发新的元素交互）。

#### 4.4.3 Kotlin 接口骨架

```kotlin
/**
 * 元素交互规则的定义。
 * 从 YAML 加载，在 Phase 3 引入。
 */
data class ElementInteractionRule(
    /** 触发元素（本次攻击的伤害类型） */
    val triggerElement: DamageType,
    /** 交互条件类型 */
    val conditionType: InteractionCondition,
    /** 条件参数（如目标状态 ID 或地形类型） */
    val conditionParam: String,
    /** 交互效果类型 */
    val effectType: InteractionEffect,
    /** 效果参数（如伤害倍率、持续时间等） */
    val effectParams: Map<String, String>,
)

enum class InteractionCondition {
    /** 目标身上有指定状态 */
    TARGET_HAS_STATUS,
    /** 目标站在指定地形上 */
    TARGET_ON_TERRAIN,
    /** 目标具有指定标签（如 UNDEAD, DEMON） */
    TARGET_HAS_TAG,
    /** 同一回合内目标受到指定元素伤害 */
    TARGET_TOOK_ELEMENT_THIS_TURN,
}

enum class InteractionEffect {
    /** 移除目标的一个状态并触发效果 */
    REMOVE_STATUS_AND_TRIGGER,
    /** 对目标伤害加成 */
    DAMAGE_MULTIPLIER,
    /** 对地形产生效果 */
    TERRAIN_TRANSFORM,
    /** 传导伤害到相邻实体 */
    CHAIN_DAMAGE,
    /** 消耗双方的持续效果 */
    MUTUAL_CONSUME,
}
```

#### 4.4.4 交互规则配置示例（YAML）

```yaml
# data/element_interactions.yaml
interactions:
  - id: "fire_vs_freeze"
    triggerElement: FIRE
    conditionType: TARGET_HAS_STATUS
    conditionParam: "FREEZE"
    effectType: REMOVE_STATUS_AND_TRIGGER
    effectParams:
      removeStatus: "FREEZE"
      bonusDamageMultiplier: "1.30"  # 增伤 30%
      applyToLargerDamage: "true"    # 适用于较大者
      vfxKey: "vfx_evaporate"
      logKey: "combat.interaction.evaporate"

  - id: "cold_vs_burn"
    triggerElement: COLD
    conditionType: TARGET_HAS_STATUS
    conditionParam: "BURN"
    effectType: REMOVE_STATUS_AND_TRIGGER
    effectParams:
      removeStatus: "BURN"
      applyStatus: "SLOW"
      slowMagnitude: "0.50"
      slowDuration: "2"
      vfxKey: "vfx_quench"
      logKey: "combat.interaction.quench"

  - id: "lightning_on_water"
    triggerElement: LIGHTNING
    conditionType: TARGET_ON_TERRAIN
    conditionParam: "WATER"
    effectType: CHAIN_DAMAGE
    effectParams:
      selfDamageBonus: "1.25"        # 自身伤害 +25%
      chainDamageRatio: "0.50"       # 传导伤害 = 原始伤害 50%
      chainRange: "1"                # 传导范围（相邻格）
      chainOnlyOnTerrain: "WATER"    # 只传导到水地形上的实体
      vfxKey: "vfx_electrocute"
      logKey: "combat.interaction.conduct"

  - id: "holy_vs_undead"
    triggerElement: HOLY
    conditionType: TARGET_HAS_TAG
    conditionParam: "UNDEAD"
    effectType: DAMAGE_MULTIPLIER
    effectParams:
      multiplier: "1.50"             # 增伤 50%
      logKey: "combat.interaction.smite_undead"

  - id: "holy_vs_demon"
    triggerElement: HOLY
    conditionType: TARGET_HAS_TAG
    conditionParam: "DEMON"
    effectType: DAMAGE_MULTIPLIER
    effectParams:
      multiplier: "1.50"
      logKey: "combat.interaction.smite_demon"
```

### 4.5 元素在各阶段的引入计划

元素系统遵循 **渐进复杂度原则**，在不同阶段逐步引入：

#### Phase 2（P2-A / P2-W2）

**引入内容：**

- `DamageType` 枚举（全部 6 个值）
- `DamageInstance` 数据类（包含 `type` 字段）
- 所有天赋和攻击在数据中指定 `damageType`
- 基础抗性字段添加到 `StatModifier`（但大多数值为 0）
- 战斗结算管线使用 `damageType` 分支物理/元素减免路径

**暂不引入：**

- 元素穿透（所有穿透值为 0）
- 元素交互规则
- 抗性的收益递减
- 负抗性的增伤效果

**行为表现：**

- 奥术师的火球术造成 `FIRE` 类型伤害，但目标默认 `fireResistance = 0`，不会被减免。
- 圣堂武士的神圣打击造成 `HOLY` 类型伤害，对亡灵标签怪物的 +50% 增伤 **已在 Phase 2 生效**（这是标签检查，不是抗性系统）。
- 物理伤害的护甲减免仍使用线性模型（与 Phase 1 一致），Phase 3 升级为收益递减模型。

**迁移影响：**

- `CombatResolver.resolveMelee()` 需要接受 `DamageType` 参数（默认 `PHYSICAL`）。
- `DamageResult` 扩展为包含 `damageType` 字段。
- `DamageDealtEvent` 扩展为包含 `damageType` 字段。
- Golden Seed 基线需要更新（`DamageResult` 结构变更）。

#### Phase 3（P3-A / P3-W1）

**引入内容：**

- 完整的抗性系统：6 通道各自的抗性值，上限 75%
- 完整的穿透系统：6 通道各自的穿透值
- 护甲模型升级为收益递减（`armor / (armor + 100)`）
- 负抗性的增伤效果（下限 -25%）
- 元素交互规则（火+冰蒸发、冰+火淬灭、神圣 vs 亡灵/恶魔）
- `CombatTrace` 完整实现
- Power/Save 对抗体系
- 怪物模板添加元素抗性数据
- 装备 affix 添加抗性/穿透词条

**迁移影响：**

- `StatsCalculator` 扩展，计算 `PowerSaveStats` 和所有通道的抗性/穿透。
- `StatModifier` 扩展，添加 12 个抗性/穿透字段。
- `CombatResolver` 重构为 `CombatPipeline`，实现 12 步结算管线。
- 所有怪物 YAML 文件添加 `resistances` 和 `immunities` 字段。
- 所有天赋 YAML 文件添加 `powerDimension` 和 `associatedEffects` 字段。
- Golden Seed 基线 **重大更新**（防御公式和伤害计算全面变更）。

#### Phase 4（P4-B / P4-W3）

**引入内容：**

- 装备 affix 池中的元素词条（如 `of_flame: firePenetration +8`、`fireproof: fireResistance +15`）
- 元素地形交互（闪电+水、火+油、冰+水面）
- 元素亲和度系统（可选）：连续使用同一元素的天赋提升该元素的伤害/效果
- Unique/Artifact 装备的元素特殊效果

**迁移影响：**

- affix 定义 YAML 扩展元素词条。
- 地图生成器引入元素地形标签（`WATER`、`OIL`、`ICE` 等）。
- `CombatPipeline` 的步骤 9 回调系统接入地形交互检查。

---

### 4.6 扩展的 StatusEffectType 枚举

为支持上述元素系统和 Phase 2~3 的天赋需求，`StatusEffectType` 需要从 Phase 1 的 4 种大幅扩展。以下是完整定义：

```kotlin
/**
 * 状态效果类型。
 * Phase 2 引入基础扩展；Phase 3 引入完整集合。
 */
enum class StatusEffectType(
    /** 效果分类：正面（BUFF）、负面（DEBUFF）、中性（NEUTRAL） */
    val category: EffectCategory,
    /** 是否可以被净化类技能移除 */
    val dispellable: Boolean = true,
    /** 是否可以叠加（同一来源的多次施加） */
    val stackable: Boolean = false,
    /** 引入阶段 */
    val introducedPhase: String = "P2",
) {
    // ── Phase 1 遗留（保持兼容） ──
    STUNNED(EffectCategory.DEBUFF, introducedPhase = "P1"),
    ARMOR_BREAK(EffectCategory.DEBUFF, introducedPhase = "P1"),

    // ── Phase 2 新增：基础控制与伤害 ──
    ROOT(EffectCategory.DEBUFF),
    SILENCE(EffectCategory.DEBUFF),
    BLIND(EffectCategory.DEBUFF),
    CONFUSE(EffectCategory.DEBUFF),
    SLOW(EffectCategory.DEBUFF),
    FEAR(EffectCategory.DEBUFF),
    KNOCKBACK(EffectCategory.DEBUFF, dispellable = false),

    // ── Phase 2 新增：持续伤害（DoT） ──
    BLEED(EffectCategory.DEBUFF, stackable = true),
    BURN(EffectCategory.DEBUFF, stackable = true),
    POISON(EffectCategory.DEBUFF, stackable = true),
    FREEZE(EffectCategory.DEBUFF),

    // ── Phase 2 新增：增益 ──
    SHIELD(EffectCategory.BUFF),
    REGEN(EffectCategory.BUFF),
    HASTE(EffectCategory.BUFF),

    // ── Phase 3 新增：高级效果 ──
    OVERCHARGE(EffectCategory.DEBUFF, introducedPhase = "P3"),
    PURIFY(EffectCategory.BUFF, dispellable = false, introducedPhase = "P3"),
    BANE(EffectCategory.DEBUFF, introducedPhase = "P3"),
    CURSE(EffectCategory.DEBUFF, introducedPhase = "P3"),
    WEAKEN(EffectCategory.DEBUFF, introducedPhase = "P3"),
    INVULNERABLE(EffectCategory.BUFF, dispellable = false, introducedPhase = "P3"),
    STEALTH(EffectCategory.BUFF, dispellable = false, introducedPhase = "P3"),
    TAUNT(EffectCategory.DEBUFF, introducedPhase = "P3"),
}

enum class EffectCategory {
    BUFF,
    DEBUFF,
    NEUTRAL,
}
```

> **Phase 1 兼容说明**：Phase 1 的 `WAR_CRY_BUFF` 和 `WAR_CRY_DEBUFF` 应在 Phase 2 迁移时重构为通用的 `BUFF`/`DEBUFF` 效果，通过 `ActiveEffect.id` 和 `statModifiers` 来区分具体效果，而不是为每个天赋创建专用的 `StatusEffectType`。

---

> **本文档为 Part 1，涵盖第 1~4 节。后续 Part 2 将涵盖第 5 节（资源系统设计）、第 6 节（AI 过渡层设计）、第 7 节（铭文/符文系统设计）、第 8 节（关键词注册表与 Tooltip）等内容。**
