# K-ToME 核心系统详细设计与阶段补充文档

> 日期：2026-03-13
> 状态：Draft v2 — 2026-03-14 revised
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

1. 本文档 **不覆盖** 路线图的阶段划分、工作包切分和出口标准——那些仍以路线图为权威，执行编号统一使用 `W1 ~ Wn`。
2. 本文档 **覆盖** 路线图中未充分定义的系统内部设计（公式、数据结构、结算顺序、交互规则）。
3. 若本文档与原始技术白皮书在同一系统上的设计存在冲突，以本文档为准。
4. 本文档中的所有 Kotlin 代码骨架是 **设计锚点**，实际实现可在不违背语义合同的前提下调整命名和内部结构。

### 1.4 修订记录

1. `2026-03-13`：初版，建立战斗、元素、状态、职业与阶段补充骨架。
2. `2026-03-14`：补入事件总线、`SaveDataV2`、`RenderSnapshot` 协议、经济/难度/输入合同、Layer 2 AI 运行时与 Phase 2 预算/编号澄清。

### 1.5 当前代码基线

截至 Phase 1 完成（`v0.1.x`），以下是本文档涉及的核心系统的当前状态：

| 系统 | 当前文件 | 当前状态 | 本文档补充的内容 |
| --- | --- | --- | --- |
| 战斗结算 | `core/combat/CombatResolver.kt` | 仅物理近战，无伤害通道，线性防御减免 | 伤害通道、非线性公式、结算管线、追踪系统 |
| 伤害结果 | `core/combat/DamageResult.kt` | 3 字段（raw/reduced/final） | 扩展为 `CombatResolutionTrace + TraceEnvelope` |
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
| GAP-04 | 存档主链虽已落到 `kotlinx.serialization`，但 `Save Schema V2`、版本纪律与资产边界尚未完全冻结 | P0 | P2~P5 | P2-A（P2-W1） | Phase 2 之后的数据类虽可序列化，但 schema 演进、fail-fast 策略和 mod/内容边界仍会漂移 |
| GAP-05 | 状态效果系统过于简陋：仅 `STUN`/`ARMOR_BREAK`/`WAR_CRY_BUFF`/`WAR_CRY_DEBUFF` 四种 | P0 | P2~P5 | P2-A | Phase 2 的 32 个天赋无法表达燃烧、冰冻、流血、护盾等核心效果 |
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

- `CombatResolutionTrace` 记录每一步的数值变化和原因标签，但不包含任何显示文本或动画指令。
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
- Phase 3：升级为完整的非线性公式、Power/Save 对抗、`CombatResolutionTrace + TraceEnvelope`。
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
- **关联状态效果**：净化动作（`CLEANSE` / `PURIFY`，移除目标身上的一个负面状态或驱散一个增益）、驱邪标记（`BANE`，对亡灵/恶魔额外增伤 50%）。
- **抗性机制**：`holyResistance`，百分比减免，取值范围 `[0, 75]`。亡灵/恶魔的通用正式口径是**标签增伤路径**，不是默认负神圣抗性；若未来存在少数负 `holyResistance` 个体，必须在怪物模板或 affix 中显式声明。
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

#### 3.1.4 Damage DTO 流与 `DamageInstance` 的关系

`DamageInstance` 不是对 `DamageRequest / DamagePacket / DamageOutcome` 的替代，而是其中的核心载荷。统一关系如下：

```kotlin
data class DamageRequest(
    val sourceId: EntityId,
    val targetId: EntityId?,
    val abilityId: String,
    val damageType: DamageType,
    val baseAmount: Int,
    val tags: Set<String>,
)

data class DamagePacket(
    val request: DamageRequest,
    val instance: DamageInstance,
    val hitConfirmed: Boolean,
)

data class DamageOutcome(
    val packet: DamagePacket,
    val finalDamage: Int,
    val appliedEffects: List<String>,
)
```

冻结口径：

1. `DamageRequest` 表达“攻击意图”。
2. `DamagePacket` 表达“命中/暴击/穿透等已解析、但尚未完成最终结算的中间态”，其中承载单条 `DamageInstance`。
3. `DamageOutcome` 表达“减伤、状态施加、死亡判定后”的最终结果。
4. `CombatResolutionTrace` 审计整个过程；`DamageInstance` 只负责记录单条伤害语义，不承担完整流水线外壳。

#### 3.1.5 伤害通道配置示例（YAML）

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
    defaultAssociatedEffects: ["CLEANSE", "BANE"]

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
  m = -10  （中点向左偏移，使 d=0 时命中率约 59%，d=10 时约 67%）

  sigmoid(d) = 1 / (1 + e^(-k * (d - m)))
  hitChance = 0.05 + 0.90 * sigmoid(d)
  hitChance = clamp(hitChance, 0.05, 0.95)
```

**示例计算：**

| `accuracy - evasion` | Phase 1 线性 | Phase 3 Sigmoid | 说明 |
| --- | --- | --- | --- |
| -30 | 0.55 | 0.33 | 大劣势时命中率显著下降 |
| -20 | 0.65 | 0.41 | 明显劣势 |
| -10 | 0.75 | 0.50 | 中点（m=-10 使此处为 50%） |
| -5 | 0.80 | 0.54 | 轻微劣势 |
| 0 | 0.85 | 0.59 | 属性相等时保留基本命中保障 |
| 5 | 0.90 | 0.63 | 轻微优势 |
| 10 | 0.95 | 0.67 | 明确优势 |
| 20 | 0.95 | 0.74 | 大幅优势 |
| 30 | 0.95 | 0.80 | 极大优势，继续向上限逼近 |

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
│   此步骤记录穿透的具体贡献值到 `CombatResolutionTrace`。
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
2. 次生伤害（如荆棘反伤）会创建一个 **新的 Pipeline 实例**，但共享同一回合的 `CombatResolutionTrace` 上下文。
3. 回调的优先级由 `CallbackPriority`（`Int`）决定，数值越小越先执行。同优先级按 `entityId` 升序排列。
4. 回调返回 `CANCEL` 会中止当前步骤的后续回调执行，但不会跳过管线的下一步。只有 `ABSORB` 会完全终止管线（例如护盾完全吸收了伤害）。

### 3.3 战斗追踪系统（CombatResolutionTrace / TraceEnvelope）

> Phase 3 同步说明：`P3-W1` 冻结的是**公式级** `CombatResolutionTrace + TraceEnvelope + Golden Corpus` 基线；遭遇级 `BossTrace / AIDecisionTrace / LongRunTrace` 由后续工作包分别维护，不再把所有追踪语义强行塞进单一 trace 类型。

`CombatResolutionTrace` 记录一次战斗结算管线的每一步计算过程，`TraceEnvelope` 负责版本和 corpus 边界。两者组合后的用途是：

1. **调试**：开发时追踪伤害计算是否正确。
2. **死因分析**：Phase 5 的死因回溯功能依赖此数据。
3. **Tooltip 展示**：鼠标悬停在战斗日志上时，展示详细的伤害分解。
4. **平衡调优**：统计分析 DPS、减免率、穿透贡献等指标。
5. **Golden Seed 回归**：固定 seed 下比对 trace 的关键数值是否变化。

#### 3.3.1 数据结构

```kotlin
package com.ktome.core.combat

import com.ktome.core.ecs.EntityId

enum class CombatCorpusId {
    FORMULA,
    STATUS,
    INTEGRATION,
    LONG_RUN,
}

data class TraceEnvelope(
    val phaseId: String,
    val rulesetVersion: String,
    val traceSchemaVersion: String,
    val corpusId: CombatCorpusId,
)

data class CombatResolutionTrace(
    val traceId: String,
    val turn: Int,
    val attackerId: EntityId,
    val targetId: EntityId,
    val abilityId: String,
    val damageType: DamageType,
    val steps: List<ResolutionStep>,
    val result: ResolutionResult,
    val childTraceIds: List<String> = emptyList(),
)

data class ResolutionStep(
    val stepIndex: Int,
    val stepName: String,
    val inputs: Map<String, String>,
    val outputs: Map<String, String>,
    val flags: Set<String> = emptySet(),
    val callbacks: List<CallbackRecord> = emptyList(),
)

data class CallbackRecord(
    val ownerId: EntityId,
    val callbackName: String,
    val priority: Int,
    val result: String,
    val effect: String?,
)

data class ResolutionResult(
    val hit: Boolean,
    val critical: Boolean = false,
    val critMultiplier: Double = 1.0,
    val rawDamage: Int = 0,
    val preReductionAbsorbed: Int = 0,
    val armorResistanceReduced: Int = 0,
    val penetrationContribution: Int = 0,
    val postReductionModifier: Int = 0,
    val finalDamage: Int = 0,
    val targetKilled: Boolean = false,
    val deathPrevented: Boolean = false,
)
```

#### 3.3.2 Trace 输出示例

以下是一个典型的 `TraceEnvelope + CombatResolutionTrace` 序列化输出示例（JSON 格式，用于调试日志）：

```json
{
  "envelope": {
    "phaseId": "P3",
    "rulesetVersion": "3.0.0",
    "traceSchemaVersion": "1",
    "corpusId": "FORMULA"
  },
  "trace": {
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
      }
    ],
    "result": {
      "hit": true,
      "critical": false,
      "finalDamage": 30,
      "targetKilled": false
    },
    "childTraceIds": []
  }
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

Spell Power    = 10 + WIL * 1.0 + DEX * 0.5 + level * 0.5 + equipment/talent bonus
Spell Save     = 10 + WIL * 0.5 + CON * 1.0 + level * 0.5 + equipment/talent bonus
```

说明：
- `level` 指角色等级，提供随等级的自然增长。
- Save 通常同时受 CON（体质/韧性）和 WIL（意志）影响，但法术抗性更偏向体质，精神抗性更偏向意志。
- Power 主要受对应攻击属性影响：STR 用于物理控制，WIL 用于精神控制，Spell 额外吃到部分 DEX 以体现施法精准度与引导控制。
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

#### 3.4.4 每种状态/效果的对抗维度映射

| 状态/效果 | 对抗维度 | 来源示例 |
| --- | --- | --- |
| `STUN`（眩晕） | Physical | 战卫重击、闪电术 |
| `KNOCKBACK`（击退，瞬时位移） | Physical | 战卫盾击 |
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

    fun calculateSpellPower(wil: Int, dex: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (wil * 1.0).toInt() + (dex * 0.5).toInt() + (level * 0.5).toInt() + bonus
    }

    fun calculateSpellSave(wil: Int, con: Int, level: Int, bonus: Int = 0): Int {
        return 10 + (wil * 0.5).toInt() + (con * 1.0).toInt() + (level * 0.5).toInt() + bonus
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

### 3.6 事件总线与回调注册表

Phase 2 的事件系统承担三类职责：规则层通知、日志/i18n 输入、表现层拉取 snapshot 的同步锚点。此前这些定义散落在 `ResourceEvent`、`CombatResolutionTrace`、路线图工作包和 PR 草案中；本节统一冻结最小合同。

#### 3.6.1 事件层次

```kotlin
sealed interface GameEvent {
    val turn: Int
}

sealed interface CombatEvent : GameEvent {
    val sourceId: EntityId
    val targetId: EntityId?
}

sealed interface MovementEvent : GameEvent {
    val entityId: EntityId
}

sealed interface ResourceEvent : GameEvent {
    val entityId: EntityId
    val resourceType: ResourceType
}

sealed interface StatusEvent : GameEvent {
    val entityId: EntityId
    val statusType: StatusEffectType
}

sealed interface QuestEvent : GameEvent {
    val questId: String
}

sealed interface LogEvent : GameEvent {
    val logKey: String
    val args: Map<String, String>
}

data class WorldStateChangedEvent(
    override val turn: Int,
    val reason: WorldStateChangeReason,
) : GameEvent
```

#### 3.6.2 EventBus 与回调接口

```kotlin
fun interface EventListener<in E : GameEvent> {
    fun onEvent(event: E, context: EventContext)
}

interface EventBus {
    fun <E : GameEvent> subscribe(
        eventType: KClass<E>,
        priority: Int = 500,
        listener: EventListener<E>,
    )

    fun publish(event: GameEvent)

    fun drainPendingEvents(context: EventContext)
}

data class EventContext(
    val world: WorldView,
    val random: RandomSource,
    val recorder: CombatResolutionTraceRecorder?,
)

enum class CallbackPhase {
    ON_ACT,
    ON_MOVE,
    ON_HIT,
    ON_DAMAGE_TAKEN,
    ON_KILL,
    ON_TURN_START,
    ON_TURN_END,
}

fun interface CombatCallback {
    fun invoke(context: CallbackContext): CallbackResult
}

data class CallbackRegistration(
    val ownerId: String,
    val phase: CallbackPhase,
    val priority: Int,
    val callback: CombatCallback,
)

enum class CallbackResult {
    /** 继续执行当前步骤后续回调与后续管线 */
    CONTINUE,
    /** 中止当前步骤剩余回调，但不跳过后续步骤 */
    CANCEL,
    /** 完全吸收并终止当前管线 */
    ABSORB,
}

interface CallbackRegistry {
    fun register(registration: CallbackRegistration)
    fun callbacksFor(phase: CallbackPhase): List<CallbackRegistration>
}
```

#### 3.6.3 优先级与可重入约束

1. 优先级区间固定为：
   - `0 ~ 99`：系统内核（资源衰减、死亡处理、回合推进）
   - `100 ~ 199`：天赋/职业回调
   - `200 ~ 299`：装备、铭文、词缀回调
   - `300 ~ 399`：状态效果与临时 aura
   - `900+`：调试、审计、开发工具
2. 同一相位内始终按 `priority asc -> ownerId asc` 执行，确保可哈希、可重放。
3. 回调中允许 `publish()` 新事件，但新事件只能追加到当前 turn 的 pending queue，不能立刻递归重入同一战斗步骤。
4. 若确需生成子过程（例如连锁闪电或反击），必须通过 `childTrace` 记录并在当前步骤结束后入队执行。
5. `WorldStateChangedEvent` 是 client 拉取 `RenderSnapshot` 的唯一同步锚点；表现层不直接订阅内部中间态。

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
| `HOLY` | 净化动作 | `PURIFY` | 瞬时效果：移除目标 1 个负面状态（对友方）或驱散 1 个增益（对敌方），不作为持久状态入档 | 瞬时 | Spell |
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
| 抗性下限（自然） | 无下限 | 某些怪物可以有显式声明的负自然抗性，但不再把亡灵/恶魔的负神圣抗性作为通用基线 |

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

**场景 2**：带有亡灵标签的目标被神圣打击命中，原始神圣伤害 40，攻击者有 `holyPenetration = 10`，并触发标签增伤路径。

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

在 `CombatResolutionTrace` 中，穿透贡献量的计算方式：

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
| `FIRE` | 目标处于 `FREEZE` 状态 | 立即解除 `FREEZE`，触发"蒸发"效果：本次火焰攻击伤害额外 +30% | P3-A |
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
3. 交互产生的次生伤害走 **新的 CombatPipeline 实例**，有独立的 `CombatResolutionTrace`。
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
- `CombatResolutionTrace / TraceEnvelope` 完整实现
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
    STUN(EffectCategory.DEBUFF, introducedPhase = "P1"),
    ARMOR_BREAK(EffectCategory.DEBUFF, introducedPhase = "P1"),

    // ── Phase 2 新增：基础控制与伤害 ──
    GUARD(EffectCategory.BUFF),
    MARKED(EffectCategory.DEBUFF),
    ROOT(EffectCategory.DEBUFF),
    SILENCE(EffectCategory.DEBUFF),
    BLIND(EffectCategory.DEBUFF),
    CONFUSE(EffectCategory.DEBUFF),
    SLOW(EffectCategory.DEBUFF),
    FEAR(EffectCategory.DEBUFF),
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

> **Phase 1 兼容说明**：Phase 1 的 `STUNNED` 在 Phase 2 统一并名为 `STUN`；`WAR_CRY_BUFF` 和 `WAR_CRY_DEBUFF` 应迁移为通用的 `BUFF`/`DEBUFF` 效果，通过 `ActiveEffect.id` 和 `statModifiers` 来区分具体效果，而不是为每个天赋创建专用的 `StatusEffectType`。

---

## 5. 职业与资源系统详细设计

本节定义 K-ToME 的职业框架、资源模型、以及每个基础/进阶职业的设计骨架。这是路线图中 GAP-02（无多资源系统）和 GAP-10（无详细职业设计）的完整补充。

### 5.1 通用资源系统

#### 5.1.1 设计动机

Phase 1 仅有 `Stamina` 一种资源，所有天赋共享同一资源池。这在 4 职业差异化时不可持续：

- 战卫应使用 **耐力（Stamina）**，通过体力管理节奏战斗
- 奥术师应使用 **法力（Mana）**，代表魔法储备
- 游荡者应使用 **能量（Energy）**，快速回复、快速消耗的爆发节奏
- 圣堂武士应使用 **正能量（Positive Energy）**，通过战斗行为积攒资源

#### 5.1.2 Kotlin 数据结构

```kotlin
package com.ktome.core.resource

import com.ktome.core.ecs.EntityId

/**
 * 资源类型枚举。每种资源有不同的回复策略和 UI 表现。
 * defaultMax 只是枚举级默认值，职业或怪物配置可以覆盖它。
 * HP/Health 继续作为独立生命系统处理，不纳入 ResourceType。
 */
enum class ResourceType(
    val nameKey: String,
    val colorHint: String,
    val defaultMax: Int,
) {
    STAMINA("resource.stamina", "#DAA520", 40),
    MANA("resource.mana", "#4169E1", 50),
    ENERGY("resource.energy", "#32CD32", 100),
    POSITIVE_ENERGY("resource.positive_energy", "#FFD700", 0),
    HATE("resource.hate", "#8B0000", 0),
    EQUILIBRIUM("resource.equilibrium", "#9370DB", 0),
}

/**
 * 资源池实例，挂载在实体上作为组件。
 */
data class ResourcePool(
    val type: ResourceType,
    var current: Int,
    var max: Int,
    val regenPolicy: RegenPolicy,
) {
    fun canSpend(amount: Int): Boolean = current >= amount
    fun spend(amount: Int): Boolean {
        if (current < amount) return false
        current -= amount
        return true
    }
    fun restore(amount: Int) {
        current = (current + amount).coerceIn(0, max)
    }
    fun isFull(): Boolean = current >= max
    fun isEmpty(): Boolean = current <= 0
    fun percent(): Float = if (max > 0) current.toFloat() / max else 0f
}

/**
 * 资源回复策略。每种职业有不同的回复机制。
 */
sealed interface RegenPolicy {
    /** 每回合固定回复 */
    data class PerTurn(val amount: Int) : RegenPolicy

    /** 击杀敌人时回复 */
    data class OnKill(val amount: Int) : RegenPolicy

    /** 受到伤害时回复（如圣堂武士正能量） */
    data class OnDamageTaken(val percent: Float) : RegenPolicy

    /** 命中敌人时回复（如游荡者能量） */
    data class OnHit(val amount: Int) : RegenPolicy

    /** 每回合衰减（用于正能量/仇恨等脱战后归零的资源） */
    data class DecayPerTurn(val amount: Int, val outOfCombatOnly: Boolean = true) : RegenPolicy

    /** 组合策略：多种回复机制同时生效 */
    data class Composite(val policies: List<RegenPolicy>) : RegenPolicy

    /** 无自然回复 */
    data object None : RegenPolicy
}
```

#### 5.1.3 各职业资源配置

| 职业 | 主资源轴 | 状态轴 | 初始上限 | 回复策略 | 设计意图 |
| --- | --- | --- | --- | --- | --- |
| 战卫 Vanguard | `STAMINA` | `-` | 40 + WIL×5 | `PerTurn(3)` | 稳定节奏，需要管理消耗避免在关键时刻无力 |
| 奥术师 Arcanist | `MANA` | `-` | 50 + WIL×6 | `PerTurn(2)` | 储量大但回复慢，鼓励预判和精确释放 |
| 游荡者 Rogue | `ENERGY` | `-` | 100 | `Composite([PerTurn(5), OnHit(8)])` | 快攻快回，鼓励持续输出而非观望 |
| 圣堂武士 Templar | `POSITIVE_ENERGY` | `-` | 100 | `Composite([OnDamageTaken(0.15), OnHit(3), DecayPerTurn(5)])` | 需要通过战斗积攒资源，强力技能消耗正能量 |
| 狂战士 Berserker（P3） | `HATE` | `HATE` | 100 | `Composite([OnDamageTaken(x), OnHit(y), OnKill(z), DecayPerTurn(n)])` | 越战越怒，高张力段强化输出，但存在失控风险 |
| 咒剑士 Spellblade（P3） | `MANA` | `EQUILIBRIUM` | `MANA` 常规上限；`EQUILIBRIUM` 100（起始 50） | `MANA` 常规恢复；`EQUILIBRIUM` 按最近成功且 `affinity != NEUTRAL` 的主动技能每回合偏移 | 近战+魔法混合，在物理和法术之间动态切换 |

`Spellblade` 的 `EQUILIBRIUM` 还必须冻结以下最小动作归类合同：

1. `EquilibriumAffinity = PHYSICAL / ARCANE / NEUTRAL`。
2. 普攻与近战武技默认 `PHYSICAL`；法术主动技能默认 `ARCANE`；混合技能必须显式声明 affinity，缺失时按 `NEUTRAL`。
3. 铭文、被动触发、free action、sustain toggle 默认 `NEUTRAL`，不改变平衡值。

#### 5.1.4 资源系统与事件总线的集成

资源变化通过以下事件通知：

```kotlin
sealed interface ResourceEvent : GameEvent {
    val entityId: EntityId
    val resourceType: ResourceType

    data class Spent(
        override val entityId: EntityId,
        override val resourceType: ResourceType,
        val amount: Int,
        val abilityId: String,
        override val turn: Int,
    ) : ResourceEvent

    data class Restored(
        override val entityId: EntityId,
        override val resourceType: ResourceType,
        val amount: Int,
        val source: String, // "regen", "on_kill", "on_hit", "potion"
        override val turn: Int,
    ) : ResourceEvent

    data class Depleted(
        override val entityId: EntityId,
        override val resourceType: ResourceType,
        override val turn: Int,
    ) : ResourceEvent
}
```

### 5.2 职业框架设计

#### 5.2.1 职业定义数据结构

```kotlin
/**
 * 职业定义，从 YAML 加载。
 */
data class ProfessionDef(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val portraitKey: String,
    val tier: ProfessionTier,
    val resourceProfiles: List<ResourceProfileRef>,
    val primarySpendAxis: ResourceAxis?,
    val stateAxis: ResourceAxis?,
    val baseStats: BaseStatBlock,
    val statGrowth: StatGrowthBlock,
    val talentTrees: List<TalentTreeRef>,
    val startingTalents: List<String>,
    val startingKit: List<String>,
    val unlockCondition: UnlockCondition?,
    val tags: Set<String>,
)

enum class ResourceAxis {
    HP,
    STAMINA,
    MANA,
    ENERGY,
    POSITIVE_ENERGY,
    HATE,
    EQUILIBRIUM,
}

enum class ProfessionTier { BASE, ADVANCED }

data class BaseStatBlock(
    val str: Int, val dex: Int, val con: Int, val wil: Int,
    val baseHp: Int, val baseResource: Int,
    val baseAttack: Int, val baseDefense: Int,
    val baseAccuracy: Int, val baseEvasion: Int,
    val baseSpeed: Int,
)

data class StatGrowthBlock(
    val hpPerLevel: Int,
    val resourcePerLevel: Int,
    val statPointsPerLevel: Int = 2,
    val talentPointsPerLevel: Float = 0.5f, // 每 2 级 1 点
)

data class TalentTreeRef(
    val treeId: String,
    val unlockLevel: Int = 1,
)

sealed interface UnlockCondition {
    data object AlwaysUnlocked : UnlockCondition
    data class RequireProfessionCleared(val professionId: String) : UnlockCondition
    data class RequireLevel(val level: Int) : UnlockCondition
    data class RequireBossKilled(val bossId: String) : UnlockCondition
}
```

#### 5.2.1.1 数值锚点与消耗/冷却分级

职业和天赋的具体数值必须依附统一锚点，避免不同树各自漂移：

| 锚点参数 | Lv1 | Lv5 | Lv10 | Lv15 |
| --- | --- | --- | --- | --- |
| 玩家 HP | 40~60 | 100~150 | 200~300 | 350~500 |
| 单次普攻伤害 | 5~10 | 15~25 | 30~50 | 60~100 |
| 普通怪 HP | 15~30 | 40~70 | 80~150 | 150~250 |
| 精英怪 HP | - | 120~200 | 250~400 | 500~800 |
| Boss HP | - | - | 500~800 | 1000~1500 |

Phase 2~3 的主动能力按以下分级设计，不允许每个职业单独发明一套成本体系：

| 级别 | 资源消耗 | 基础冷却 | 典型用途 |
| --- | --- | --- | --- |
| Basic | `0 ~ 8` | `0 ~ 2` | 普攻强化、短位移、轻控 |
| Core | `8 ~ 18` | `3 ~ 5` | 主力输出、稳定群攻、标准控制 |
| Heavy | `18 ~ 35` | `6 ~ 10` | 爆发、强控制、强护盾 |
| Ultimate | `35+` 或特殊资源条件 | `10+` / 分阶段限制 | Boss answer、保命大招、终结技 |

`SUSTAINED` 技能不走上表冷却，但必须定义：

1. 开启成本
2. 每回合维持成本或机会成本
3. 关闭后是否有锁定回合

`BaseStatBlock.baseSpeed` 从 Phase 2 起统一使用 `1000` 能量制口径，标准角色为 `1000`，快职为 `1100`，慢职为 `950` 等。

#### 5.2.2 四大基础职业详细设计

以下四套树形是长期目标集，每个基础职业完整形态仍按 `3 树 × 4 节点 = 12` 设计。但 `Phase 2` 的执行预算以后续路线图为准，只落每职业 `8` 个已冻结节点：

1. 优先实现每棵树前 `2` 个节点。
2. 第 `3` 个节点只允许在该职业的签名玩法缺少关键 answer 时提前进入 `Phase 2`。
3. 第 `4` 个节点默认进入 `Phase 3`，除非路线图显式上调预算。

##### 5.2.2.1 战卫（Vanguard）

**设计理念**：前排肉盾与控场专家。以物理伤害为核心，通过护甲、格挡和状态控制在前线创造安全空间。

**资源**：耐力（Stamina），每回合自然回复 3 点。

**基础属性**：`str=14, dex=8, con=12, wil=6, baseHp=60, baseStamina=40, baseAttack=7, baseDefense=4, baseAccuracy=10, baseEvasion=3, baseSpeed=1000`

**天赋树（长期目标 12 天赋；Phase 2 先落 8 个冻结节点）**：

| 天赋树 | 定位 | 天赋 1（Lv1 解锁） | 天赋 2（Lv3 解锁） | 天赋 3（Lv5 解锁） | 天赋 4（Lv7 解锁） |
| --- | --- | --- | --- | --- | --- |
| 武技（Arms） | 主输出 | **猛力打击** — 150% 物理单体，3 级破甲，5 级穿透 +15 | **横扫** — 100% 物理锥形 3 格，3 级击退 1 格 | **碎甲** — 120% 物理，目标破甲 4 回合，被动提供 armorPen +5 | **斩杀** — 200% 物理单体，目标 HP<30% 时伤害翻倍 |
| 防御（Shield） | 自保+控制 | **盾击** — 80% 物理+眩晕 1 回合，3 级击退 2 格 | **格挡姿态**（持续） — +20% 护甲，受到近战攻击时 30% 概率格挡（减伤 50%） | **嘲讽** — 强制周围 3 格敌人攻击自己 2 回合 | **铁壁**（持续） — +40% 护甲，-20% 移动速度 |
| 战吼（Warcry） | 群控+增益 | **战吼** — 自身攻击 +20%，周围敌人防御 -15%，3 回合 | **威压** — 周围 4 格敌人恐惧 2 回合（Mental Save 对抗） | **坚毅** — 被动，HP<30% 时自动获得护盾（每局 1 次） | **不屈** — 下次致死伤害改为 HP 保留 1 点，8 回合 CD |

**Solo-Clear 验证路径**：
- 主输出：Arms 树 → 猛力打击 + 碎甲 + 斩杀
- 自保：Shield 树 → 格挡姿态 + 铁壁
- 群处理：横扫 + 战吼
- Boss answer：碎甲持续破甲 + 斩杀斩杀线
- Panic answer：不屈 + 盾击眩晕脱身

##### 5.2.2.2 奥术师（Arcanist）

**设计理念**：远程法术输出与区域控制专家。在三种元素之间切换以应对不同局面。

**资源**：法力（Mana），每回合回复 2 点，最大值较高但消耗也高。

**基础属性**：`str=4, dex=8, con=8, wil=14, baseHp=40, baseMana=60, baseAttack=3, baseDefense=1, baseAccuracy=8, baseEvasion=5, baseSpeed=1000`

**天赋树（长期目标 12 天赋；Phase 2 先落 8 个冻结节点）**：

| 天赋树 | 定位 | 天赋 1 | 天赋 2 | 天赋 3 | 天赋 4 |
| --- | --- | --- | --- | --- | --- |
| 火焰（Flame） | 持续伤害+AoE | **火球** — 120% 火焰 AoE 半径 2，燃烧 3 回合 | **烈焰之墙** — 在目标线上创建火墙 3 回合，经过者受火焰伤害 | **焚身** — 180% 火焰单体，燃烧中目标额外 +50% 伤害 | **陨石术** — 250% 火焰 AoE 半径 3，2 回合延迟，telegraph 明显 |
| 寒冰（Frost） | 控制+减速 | **冰箭** — 100% 寒冰单体，减速 30%×3 回合 | **霜冻新星** — 80% 寒冰自身周围 3 格，减速所有敌人 | **冰封** — 对减速中的目标冻结 2 回合 | **暴风雪** — 60% 寒冰 AoE 半径 4×4 回合，每回合 60% 寒冰伤害+减速 |
| 奥术（Arcane） | 护盾+位移 | **奥术护盾**（持续） — 吸收等于最大 Mana 20% 的伤害 | **闪现** — 瞬移到视野内 6 格位置 | **法力涌动** — 恢复 30% 最大 Mana | **奥术超载** — 下 3 回合所有法术伤害 +30%，结束后眩晕自己 1 回合 |

**Solo-Clear 验证路径**：
- 主输出：火焰树（火球+焚身）或寒冰树（冰箭+暴风雪）
- 自保：奥术护盾 + 闪现
- 群处理：火球 AoE + 霜冻新星 + 暴风雪
- Boss answer：焚身（单体爆发）+ 奥术超载
- Panic answer：闪现 + 冰封（冻住近身威胁后拉开距离）

##### 5.2.2.3 游荡者（Rogue）

**设计理念**：高机动性的近战爆发手。依赖位移和隐匿创造有利战斗条件，精准打击高价值目标。

**资源**：能量（Energy），快速自然回复（每回合 5）+ 命中回复（每次 8），鼓励持续进攻。

**基础属性**：`str=8, dex=14, con=8, wil=6, baseHp=42, baseEnergy=100, baseAttack=5, baseDefense=1, baseAccuracy=12, baseEvasion=8, baseSpeed=1100`

**天赋树（长期目标 12 天赋；Phase 2 先落 8 个冻结节点）**：

| 天赋树 | 定位 | 天赋 1 | 天赋 2 | 天赋 3 | 天赋 4 |
| --- | --- | --- | --- | --- | --- |
| 暗杀（Assassination） | 单体爆发 | **背刺** — 180% 物理，从隐身发动时额外 +100% 伤害 | **毒刃** — 普攻附加暗影 DoT（中毒 3 回合） | **致命一击** — 200% 物理，暴击率 +20%，暴击伤害 +50% | **处刑** — 250% 物理+暗影混合，目标 HP<25% 时自动暴击 |
| 诡术（Subtlety） | 隐匿+控制 | **隐匿** — 进入隐身 3 回合，被攻击或攻击时解除 | **烟雾弹** — AoE 致盲 2 回合，自身进入隐身 | **绊脚索** — 定身目标 2 回合 | **暗影步** — 瞬移到目标身后并进入隐身 1 回合 |
| 敏捷（Agility） | 机动+被动 | **翻滚** — 移动 3 格，移动过程免疫机会攻击 | **刀刃乱舞** — 120% 物理攻击周围所有敌人 | **要害嗅觉**（被动） — 暴击率 +5%，暴击伤害 +20% | **影遁** — 受到致命伤害时自动进入隐身并恢复 20% HP，每局 1 次 |

##### 5.2.2.4 圣堂武士（Templar）

**设计理念**：神圣战士，兼具惩击输出和自我维持。通过战斗积攒正能量来释放强力技能。

**资源**：正能量（Positive Energy），通过受击（受伤时回复 15% 伤害值的正能量）和命中（每次 3 点）积攒，脱战衰减。

**基础属性**：`str=12, dex=6, con=10, wil=12, baseHp=55, basePositiveEnergy=100, baseAttack=6, baseDefense=3, baseAccuracy=8, baseEvasion=3, baseSpeed=950`

**天赋树（长期目标 12 天赋；Phase 2 先落 8 个冻结节点）**：

| 天赋树 | 定位 | 天赋 1 | 天赋 2 | 天赋 3 | 天赋 4 |
| --- | --- | --- | --- | --- | --- |
| 惩击（Smite） | 神圣输出 | **神圣打击** — 130% 神圣伤害，对亡灵 +50% | **审判之锤** — 150% 神圣 AoE 十字形（1+4 格），驱邪标记 3 回合 | **圣火** — 120% 神圣+火焰，燃烧 3 回合（神圣火焰不受火抗影响，走神圣抗性） | **天罚** — 300% 神圣伤害，消耗 80 正能量，对驱邪标记目标额外 +100% |
| 神佑（Grace） | 自保+增益 | **圣光** — 治疗自身 HP 20%+WIL×2 | **圣盾**（持续） — 吸收伤害等于 CON×3，每 10 回合刷新 | **净化** — 移除自身 2 个负面状态 | **神圣庇护** — 3 回合内受到致死伤害改为不低于 1 HP，每局 1 次 |
| 信仰（Faith） | 被动+光环 | **虔诚**（被动） — 正能量回复效率 +20% | **神圣光环**（持续） — 周围 2 格敌方亡灵/恶魔每回合受神圣伤害 | **坚定信念**（被动） — Mental Save +15，不可被恐惧 | **神罚回响**（被动） — 造成神圣伤害时，10% 概率对目标施加沉默 2 回合 |

#### 5.2.3 四大进阶职业设计骨架（Phase 3）

进阶职业在 Phase 3 引入，每个进阶职业有 4 棵天赋树（4×4=16 天赋），加上从基础职业继承的部分天赋树。

| 进阶职业 | 基础来源 | 资源 | 核心机制 | 天赋树方向 |
| --- | --- | --- | --- | --- |
| 狂战士 Berserker | 战卫 | 仇恨 | 越战越强，HP 越低伤害越高，有失控风险 | 狂怒、毁灭、血战、蛮力 |
| 咒剑士 Spellblade | 奥术师 | 平衡值 | 近战+魔法混合，在物理和法术之间动态切换 | 附魔之刃、元素涌动、战斗法术、奥术武技 |
| 影刃客 Shadowblade | 游荡者 | 能量 | 暗影元素融入刺杀，持续控制+暗影DoT | 暗杀术、暗影步法、毒术、夜行者 |
| 守林者 Warden | 圣堂武士 | 正能量 | 自然系防御+治疗强化，区域控制+护盾 | 自然之力、生命守护、大地之盾、森林之怒 |

### 5.3 种族系统设计骨架（Phase 3）

| 种族 | `nameKey` | 核心被动 | 天赋树 1 | 天赋树 2 | 种族特殊规则 |
| --- | --- | --- | --- | --- | --- |
| 人类 | `race.human` | 每级额外 +1 属性点 | 适应力 | 领悟 | 无特殊弱点，全能型 |
| 精灵 | `race.elf` | 视野 +2，闪避 +5% | 敏锐感知 | 自然亲和 | 负重上限降低 15% |
| 矮人 | `race.dwarf` | 物理抗性 +10%，中毒持续时间 -50% | 韧性 | 锻造亲和 | 移动速度 -5% |
| 兽人 | `race.orc` | 攻击 +10%，HP<20% 时攻速 +20% | 蛮力 | 战意 | Mental Save -5 |
| 亡者 | `race.undead` | 免疫中毒/流血，治疗效果 -30% | 亡者意志 | 不死之躯 | 受神圣伤害 +25% |

---

## 6. 技能/天赋系统详细设计

### 6.1 天赋定义 Schema（V2）

Phase 1 的天赋定义过于简陋，仅支持伤害倍率和少量状态效果。Phase 2 起需要支持完整的数据驱动天赋系统。

#### 6.1.1 完整天赋 YAML Schema

```yaml
# 示例：战卫 - 猛力打击
talent:
  id: "power_strike"
  nameKey: "talent.power_strike"
  descKey: "talent.power_strike.desc"
  iconKey: "icon_talent_power_strike"
  visualKey: "vfx_power_strike"
  audioProfile: "sfx_melee_heavy"

  # 基础属性
  tier: 1                    # 天赋阶级（1=初始可学，2=需要前置，3=高级）
  maxPoints: 5               # 最大可投入点数
  category: ACTIVE           # ACTIVE / PASSIVE / SUSTAINED
  damageType: PHYSICAL       # 伤害通道
  resourceCosts:
    - axis: STAMINA
      amount: 8              # 基础资源消耗
  cooldown: 3                # 基础冷却回合数
  castTime: STANDARD         # `ActionCost` 的 YAML 输入别名

  # 目标选择
  targeting:
    type: SINGLE_TARGET       # SINGLE_TARGET / LINE / CONE / RADIUS / SELF / GROUND_TARGET
    range: 1                  # 最大距离
    minRange: 0               # 最小距离
    requiresLineOfSight: true
    friendlyFire: false

  # Telegraph（预警）
  telegraphRef: "power_strike_warning"   # 引用统一 TelegraphSpec；shape / preview / danger 不在 talent YAML 内联重复维护

  aiHints:
    role: OFFENSE
    preferredRange: [1, 1]
    isSustainToggle: false

  # 前置条件
  requirements:
    level: 1
    stats: {}                 # 例如 { str: 16 } 表示需要 16 力量
    talentPrereqs:
      - talentId: "basic_attack"
        minRank: 2

  # 每级效果（断点成长）
  levelEffects:
    1:
      damageMultiplier: 1.5
      unlockTags: []
    2:
      damageMultiplier: 1.7
    3:
      damageMultiplier: 1.9
      additionalEffect: "ARMOR_BREAK"
      effectDuration: 3
      unlockTags: ["armor_break_on_hit"]
    4:
      damageMultiplier: 2.1
    5:
      damageMultiplier: 2.5
      armorPenetration: 15
      unlockTags: ["armor_penetration"]

  # 关键词标签
  keywords: ["melee", "physical", "single_target", "armor_break"]

  # 回调挂载
  callbacks:
    onHit:
      - type: APPLY_STATUS
        statusId: "ARMOR_BREAK"
        duration: "{effectDuration}"
        saveDimension: PHYSICAL
        minLevel: 3
```

#### 6.1.2 天赋目标类型定义

```kotlin
enum class TargetingType {
    /** 单体目标 */
    SINGLE_TARGET,
    /** 直线（如冲锋、射线） */
    LINE,
    /** 锥形范围 */
    CONE,
    /** 以自身为中心的圆形 */
    RADIUS_SELF,
    /** 以目标点为中心的圆形 */
    RADIUS_TARGET,
    /** 仅对自身 */
    SELF,
    /** 地面目标（如陷阱、火墙） */
    GROUND_TARGET,
    /** 十字形 */
    CROSS,
}

/**
 * 目标选择结果。
 */
data class TargetingResult(
    val primaryTarget: EntityId?,
    val affectedCells: Set<Point>,
    val affectedEntities: Set<EntityId>,
)
```

补充冻结：

1. `Phase 3` 的正式 targeting 枚举固定为上述 8 类。
2. 旧命名 `ACTOR / TILE / RADIUS` 在 `Phase 3` 分别收口为 `SINGLE_TARGET / GROUND_TARGET / RADIUS_SELF|RADIUS_TARGET`。
3. `WALL / SUMMON_SLOT` 只作为 `Phase 4+` 的扩展保留概念，不进入 `Phase 3` runtime targeting 枚举。

#### 6.1.3 Telegraph 系统

Telegraph（预警）系统用于在 Boss 或精英使用高危技能时给玩家反应时间：

```kotlin
data class TelegraphSpec(
    val shape: TelegraphShape,
    val previewTurns: Int,
    val dangerLevel: DangerLevel,
    val threatProfileId: String,
    val counterplayTags: List<String>,
    val stages: List<TelegraphStage> = emptyList(),
)

data class TelegraphStage(
    val type: TelegraphStageType,
    val durationTurns: Int,
)

enum class TelegraphShape {
    SINGLE_CELL,
    LINE,
    CONE,
    CIRCLE,
    CROSS,
    RING,
    CUSTOM,
}

enum class DangerLevel {
    LOW,
    MODERATE,
    HIGH,
    LETHAL,
}

enum class TelegraphStageType {
    WARNING,
    WINDUP,
    IMPACT,
    RESIDUAL,
}
```

**Boss Telegraph 设计规则**：

1. 所有 Boss 技能中伤害大于等于玩家最大 HP 30% 的，必须有至少 1 回合的 telegraph
2. 伤害大于等于 50% 的技能必须有 2 回合 telegraph
3. `counterplayTags` 是正式字段，用于表达 `DODGE / INTERRUPT / BLOCK` 等可应对手段。
4. `Phase 3` 只冻结 `preview + trigger` 的最小模型；`stages` 作为 `Phase 5` 四段式 telegraph 的预留字段，当前默认空列表。
5. Telegraph 必须同时通过视觉（地面高亮）、日志（文字警告）、音频（预警音效）三通道传达。
6. 玩家站在 telegraph 区域内时 HUD 应有额外闪烁提示。

### 6.2 天赋树结构

#### 6.2.1 树结构定义

```yaml
# 示例：战卫 - 武技树
talent_tree:
  id: "vanguard_arms"
  nameKey: "talent_tree.vanguard_arms"
  iconKey: "icon_tree_arms"
  professionId: "vanguard"
  layout:
    rows: 4
    columns: 1
  nodes:
    - talentId: "power_strike"
      row: 0
      col: 0
      prerequisites: []
    - talentId: "sweeping_strike"
      row: 1
      col: 0
      prerequisites: ["power_strike:1"]
    - talentId: "sunder_armor"
      row: 2
      col: 0
      prerequisites: ["power_strike:2"]
    - talentId: "execute"
      row: 3
      col: 0
      prerequisites: ["sunder_armor:1", "sweeping_strike:1"]
```

#### 6.2.2 天赋点获取与分配

| 属性 | 值 | 说明 |
| --- | --- | --- |
| 天赋点获取 | 每 2 级 1 点 | 1,3,5,7,9...级获得 |
| 每棵树最大投入 | 无硬限，受天赋 `maxPoints` 约束 | 但前置条件自然限制了投入顺序 |
| 洗点 | `Phase 3` 默认免费；`Phase 4+` 可再引入限次或道具成本 | 洗点时触发所有天赋的 `on_unlearn`；执行口径与 `docs/phase3/*` 保持一致 |
| 前置检查时机 | 仅在分配瞬间检查 | 分配后属性下降不影响已分配天赋 |

Phase 2 的执行文档（`P2-W6/P2-W7`）只允许从上述长期树中裁剪冻结子集，不允许额外发明独立于 `Flame/Frost/Arcane`、`Assassination/Subtlety/Agility` 等长期树之外的新天赋方向。

### 6.3 关键词注册表（Keyword Registry）

```kotlin
/**
 * 关键词注册表，用于在天赋描述和战斗日志中提供统一的术语解释。
 */
data class KeywordDef(
    val id: String,
    val nameKey: String,      // 如 "keyword.armor_break"
    val tooltipKey: String,   // 如 "keyword.armor_break.tooltip"
    val iconKey: String,
    val colorHint: String,
    val relatedKeywords: List<String> = emptyList(),
)
```

**Phase 3 必须注册的关键词清单**：

| 关键词 ID | 中文名 | 说明 |
| --- | --- | --- |
| `armor_break` | 破甲 | 降低目标护甲值 |
| `bleed` | 流血 | 每回合物理持续伤害 |
| `burn` | 燃烧 | 每回合火焰持续伤害 |
| `freeze` | 冰冻 | 无法行动，受攻击解除 |
| `stun` | 眩晕 | 无法行动 |
| `shield` | 护盾 | 吸收一定量伤害 |
| `penetration` | 穿透 | 无视部分护甲/抗性 |
| `diminishing_returns` | 收益递减 | 属性越高边际收益越低 |
| `power_save` | 强度/豁免 | 状态施加的攻防对抗 |
| `telegraph` | 预警 | Boss 技能的提前警告 |
| `crit` | 暴击 | 命中时有概率造成额外伤害 |
| `dot` | 持续伤害 | 每回合造成伤害的效果 |
| `sustain` | 持续技能 | 开关式天赋，激活时持续生效 |
| `cleanse` | 净化 | 移除负面状态 |
| `dispel` | 驱散 | 移除正面状态 |

---

## 7. 怪物 AI 系统详细设计

### 7.1 AI 分层架构

当前 Phase 1 仅有 `CHASE`/`KITE`/`PATROL` 三种硬编码行为。路线图中 Phase 5 才引入 Utility AI + 行为树。为填补 Phase 2~4 的 AI 真空，设计以下三层架构：

```
┌─────────────────────────────────────────────┐
│  Layer 3: Utility AI + 行为树（Phase 5）      │
│  用于：精英/Boss 的战术级智能决策              │
├─────────────────────────────────────────────┤
│  Layer 2: 脚本化行为配置（Phase 3）            │
│  用于：精英怪、Boss 的数据驱动行为             │
├─────────────────────────────────────────────┤
│  Layer 1: 基础行为模板（Phase 1，继续使用）    │
│  用于：普通杂兵的简单行为                      │
└─────────────────────────────────────────────┘
```

每一层向下兼容：Layer 2 可以在行为脚本中引用 Layer 1 的基础行为作为默认 fallback；Layer 3 可以在行为树叶节点中调用 Layer 2 的脚本行为。

### 7.2 Layer 1：基础行为模板（继承 Phase 1）

保持当前实现不变，作为普通怪物的默认行为：

| 行为类型 | 触发条件 | 行为逻辑 |
| --- | --- | --- |
| `CHASE` | 目标可见 | A* 寻路到目标相邻格，然后攻击 |
| `KITE` | 目标可见 | 维持优选距离，过近后退，过远追击，距离合适时攻击 |
| `PATROL` | 默认/无目标 | 按路点巡逻，发现目标后切换到 CHASE |

### 7.3 Layer 2：脚本化行为配置（Phase 2~3 引入）

#### 7.3.1 设计原则

脚本化行为不是完整的脚本语言（不引入 Lua），而是 **YAML 驱动的优先级行为列表**。每个行为条目有条件、动作和优先级，AI 每回合从上到下检查条件，执行第一个满足条件的行为。

#### 7.3.2 行为脚本 Schema

```yaml
# 示例：兽人萨满（远程施法+自保）
monster:
  id: "orc_shaman"
  ai:
    type: SCRIPTED
    sightRadius: 8
    defaultBehavior: KITE
    preferredRange: [3, 5]

    behaviors:
      # 最高优先级：低血量自保
      - id: "heal_self"
        priority: 100
        condition:
          type: SELF_HP_BELOW
          threshold: 0.30
        action:
          type: USE_TALENT
          talentId: "heal"
        cooldownOverride: 8

      # 被近身时后退
      - id: "retreat"
        priority: 90
        condition:
          type: TARGET_DISTANCE_LESS_THAN
          distance: 2
        action:
          type: MOVE_AWAY
          distance: 3

      # 有火球可用且目标在范围内
      - id: "cast_fireball"
        priority: 80
        condition:
          type: AND
          conditions:
            - type: TALENT_READY
              talentId: "fireball"
            - type: TARGET_DISTANCE_BETWEEN
              min: 2
              max: 6
        action:
          type: USE_TALENT
          talentId: "fireball"

      # 目标可见但不在法术范围
      - id: "approach"
        priority: 50
        condition:
          type: TARGET_VISIBLE
        action:
          type: KITE
          preferredRange: [3, 5]

      # 默认：巡逻
      - id: "idle_patrol"
        priority: 0
        condition:
          type: ALWAYS
        action:
          type: PATROL
```

#### 7.3.3 条件类型定义

```kotlin
enum class AIConditionType {
    ALWAYS,                       // 永远为真
    SELF_HP_BELOW,                // 自身 HP 百分比低于阈值
    SELF_HP_ABOVE,                // 自身 HP 百分比高于阈值
    SELF_RESOURCE_BELOW,          // 自身资源百分比低于阈值
    SELF_RESOURCE_ABOVE,          // 自身资源百分比高于阈值
    SELF_HAS_STATUS,              // 自身有指定状态
    SELF_NOT_HAS_STATUS,          // 自身没有指定状态
    TARGET_VISIBLE,               // 目标可见
    TARGET_DISTANCE_LESS_THAN,    // 目标距离小于
    TARGET_DISTANCE_GREATER_THAN, // 目标距离大于
    TARGET_DISTANCE_BETWEEN,      // 目标距离在区间内
    TARGET_HP_BELOW,              // 目标 HP 百分比低于
    TARGET_HAS_STATUS,            // 目标有指定状态
    TALENT_READY,                 // 指定天赋可用（未冷却+资源足够）
    ALLIES_IN_RADIUS,             // 指定范围内友军数量
    ENEMIES_IN_RADIUS,            // 指定范围内敌军数量
    AND,                          // 多条件与
    OR,                           // 多条件或
    NOT,                          // 条件取反
    TURN_COUNT_MODULO,            // 回合数取模（用于周期性行为）
}
```

#### 7.3.4 动作类型定义

```kotlin
enum class AIActionType {
    MOVE_TOWARD,         // A* 寻路向目标移动
    MOVE_AWAY,           // 远离目标移动指定距离
    MOVE_TO_FLANKING,    // 移动到目标侧翼
    ATTACK_MELEE,        // 近战攻击
    ATTACK_RANGED,       // 远程攻击
    USE_TALENT,          // 使用指定天赋
    CHASE,               // Layer 1 追击行为
    KITE,                // Layer 1 风筝行为
    PATROL,              // Layer 1 巡逻行为
    WAIT,                // 原地等待
    FLEE,                // 逃跑到最远可达点
    SUMMON,              // 召唤援军
    TELEGRAPH,           // 放出技能预警（不立即释放）
}
```

#### 7.3.5 脚本化 AI 的运行时骨架

Layer 2 脚本化 AI 只在实体真正轮到行动时评估，不在每个全局 turn 对所有怪物做全量扫描。

```kotlin
interface AIResolver {
    fun decide(entityId: EntityId, world: WorldView): AIAction
}

class ScriptedAIResolver(
    private val entries: List<BehaviorEntry>,
    private val fallback: AIAction,
) : AIResolver {
    override fun decide(entityId: EntityId, world: WorldView): AIAction {
        for (entry in entries.sortedWith(compareByDescending<BehaviorEntry> { it.priority }.thenBy { it.id })) {
            if (ConditionEvaluator.evaluate(entry.condition, entityId, world)) {
                return entry.action
            }
        }
        return fallback
    }
}
```

运行时约束：

1. 条件表达式允许 `AND/OR/NOT` 递归嵌套，但深度上限固定为 `3`。
2. 精英/Boss 单次 `decide()` 的目标预算是 `< 0.1ms`；超预算的脚本必须回退为预计算标签或更粗粒度条件。
3. `TARGET_VISIBLE`、`TARGET_DISTANCE_*`、`HP_*` 等高频条件必须基于当前 `WorldView` 快照读取，不能在脚本层重复跑昂贵搜索。
4. 脚本默认在加载关卡或启动游戏时解析；Phase 3 不承诺热加载，修改 YAML 后允许重启或重新载入会话。
5. `defaultBehavior` 必须始终存在，防止所有条件都 miss 时出现空决策。
6. 普通怪与精英怪的一般行为选择固定使用 `priority + condition` 的确定性模型；若 Boss 需要在激活 phase 内增加不确定性，只允许在**已通过条件筛选**的候选动作集合上做加权选择，并且必须把权重、随机种子与最终选中动作写入 trace。
7. `AIProfile` 进入 `selectionPolicy` 时代后，候选动作排序固定为 `orderKey asc(null=Int.MAX_VALUE) -> actionId asc`；不允许依赖 YAML 加载顺序。

### 7.4 Boss 阶段状态机

Boss 战是 Roguelike 的核心体验之一。每个 Boss 有多个阶段（Phase），各阶段有不同的行为脚本和技能组。`Phase 3` 起，Boss 行为采用两层结构：`BossEncounter` 负责 phase 边界与进入事件，`AIProfile` 负责 phase 内动作选择；若本节示例与 `docs/phase3/*` 的冻结 schema 存在细节差异，一律以 `Phase 3` 执行文档 `4.5` 为权威。

#### 7.4.1 Boss 定义 Schema

```yaml
boss:
  id: "dungeon_lord"
  nameKey: "boss.dungeon_lord"
  templateId: "dungeon_lord_template"

  phases:
    - id: "phase_1"
      nameKey: "boss.dungeon_lord.phase1"
      hpThreshold: 1.0
      hpEnd: 0.70
      aiProfileId: "dungeon_lord_phase1"
      onEnter:
        - type: LOG_MESSAGE
          logKey: "boss.dungeon_lord.phase1.enter"
        - type: PLAY_AUDIO
          cueId: "sfx_boss_roar"

    - id: "phase_2"
      nameKey: "boss.dungeon_lord.phase2"
      hpThreshold: 0.70
      hpEnd: 0.30
      aiProfileId: "dungeon_lord_phase2"
      onEnter:
        - type: LOG_MESSAGE
          logKey: "boss.dungeon_lord.phase2.enter"
        - type: APPLY_BUFF
          statusId: "ENRAGE"
          duration: -1
        - type: PLAY_AUDIO
          cueId: "sfx_boss_enrage"
        - type: TELEGRAPH
          telegraphSpecId: "ground_slam_phase_warning"

    - id: "phase_3"
      nameKey: "boss.dungeon_lord.phase3"
      hpThreshold: 0.30
      hpEnd: 0.0
      aiProfileId: "dungeon_lord_phase3"
      onEnter:
        - type: LOG_MESSAGE
          logKey: "boss.dungeon_lord.phase3.enter"
        - type: SUMMON
          monsterId: "skeleton_guard"
          count: 2
          positions: FLANKING
```

#### 7.4.2 Boss 行为脚本示例

```yaml
# dungeon_lord_phase2.yaml - 狂暴阶段，对应 BossEncounter 中 phase_2 引用的 AIProfile
ai_profile:
  id: "dungeon_lord_phase2"
  perceptionRange: 12
  useLastKnownPosition: true
  defaultBehavior: "CHASE"
  selectionPolicy: "WEIGHTED_RANDOM"

  actions:
    - id: "ground_slam"
      type: "USE_ABILITY"
      orderKey: 10
      abilityId: "ground_slam"
      condition:
        type: "TURN_COUNT_MODULO"
        divisor: 4
        remainder: 0
      weight: 50

    - id: "execute_strike"
      type: "USE_ABILITY"
      orderKey: 20
      abilityId: "execute_strike"
      condition:
        type: "AND"
        conditions:
          - type: "TARGET_HP_BELOW"
            threshold: 0.25
          - type: "TALENT_READY"
            talentId: "execute_strike"
      weight: 30

    - id: "melee"
      type: "ATTACK_MELEE"
      orderKey: 30
      condition:
        type: "TARGET_DISTANCE_LESS_THAN"
        distance: 2
      weight: 15

    - id: "chase"
      type: "CHASE"
      orderKey: 40
      condition:
        type: "TARGET_VISIBLE"
      weight: 5
```

说明：

1. `weight` 是相对权重，不要求预先归一化；运行时对通过条件过滤后的候选动作集合重新求和归一化。
2. `WEIGHTED_RANDOM` 采样前固定按 `orderKey asc -> actionId asc` 排序；`orderKey` 缺失按 `Int.MAX_VALUE`。
3. 未声明 `weight` 的候选动作默认按 `1.0` 处理；若候选集合总权重 `<= 0`，回退到排序后的首个候选动作并写 trace reason。
4. telegraph 的 defender baseline 只允许经由 `ThreatProfileDef` 注册表（如 `game/src/main/resources/data/telegraph/threat_profiles/*.yaml`）引用，不允许在 Boss / talent YAML 内联一套新的 baseline。

### 7.5 怪物感知系统（Phase 3~5 渐进引入）

#### 7.5.1 感知状态机

```
UNAWARE ──目标进入感知范围──> SUSPICIOUS ──确认目标──> ALERT ──失去目标──> SEARCHING ──超时──> UNAWARE
                                                         │
                                                         └──目标在视野内──> ALERT（保持）
```

| 状态 | 行为 | 引入阶段 |
| --- | --- | --- |
| `UNAWARE` | 执行默认行为（巡逻/待机），不关注玩家 | P1 |
| `SUSPICIOUS` | 向最后已知位置移动，视野搜索范围 +50% | P3 |
| `ALERT` | 全力追击/攻击，使用全部技能 | P1 |
| `SEARCHING` | 向仇恨焦点移动，到达后搜索周围 3 回合 | P3 |

#### 7.5.2 仇恨焦点系统

```kotlin
/**
 * 仇恨焦点：记录 AI 对玩家位置的最后已知信息。
 * AI 不直接获取玩家当前坐标，而是基于推断行动。
 */
data class HateFocus(
    val position: Point,
    val confidence: Float,     // 0.0~1.0，随时间衰减
    val lastUpdateTurn: Int,
    val source: FocusSource,
)

enum class FocusSource {
    DIRECT_SIGHT,       // 直接看到
    HEARD_COMBAT,       // 听到战斗声
    TOOK_DAMAGE,        // 受到伤害
    ALLY_REPORTED,      // 友军通报
    LAST_KNOWN,         // 最后已知位置
}
```

**`STEALTH / TAUNT` 与 AI 的交互合同**：

1. 目标进入 `STEALTH` 后，AI 当前目标引用立即失效。
2. 若启用了 `useLastKnownPosition`，AI 必须先移动到最后已知位置；到达后仍未重新发现目标，则回退到 `defaultBehavior`。
3. Boss 在 `STEALTH` 场景下不切换 phase，只能在当前 phase 内执行无目标 fallback 或范围扫描行为，不得锁定隐身目标。
4. AI 被 `TAUNT` 命中后，持续期间必须强制把攻击目标设为嘲讽源；若目标不在攻击范围内，优先移动靠近，不得攻击其他目标。
5. `TAUNT` 结束后，AI 恢复原有目标选择逻辑。

**Phase 5 完整实现**：精英/Boss 使用 Utility AI 评估仇恨焦点的可信度，决定是继续追踪还是放弃搜索。

### 7.6 各阶段怪物模板预算

| 阶段 | 普通怪 | 精英怪 | Boss | AI 层级 |
| --- | --- | --- | --- | --- |
| Phase 2 | 18 种 | 4 种 | 2 个 | Layer 1（普通）+ Layer 2 简单脚本（精英/Boss） |
| Phase 3 | 40 种 | 12 种 | 3~4 个 | Layer 2 完整脚本 + Boss 阶段状态机 |
| Phase 4 | 60 种 | 18 种 + 突变 | 6 个 + 变体 | Layer 2 + 精英突变行为变体 |
| Phase 5 | 80 种 | 24 种 | 8 个 | Layer 3（Utility AI + 行为树） |

---

## 8. 状态效果与铭文系统详细设计

### 8.1 状态效果完整分类

第 4 节已定义了 `StatusEffectType` 枚举。本节补充完整的生命周期、交互规则和优先级体系。

#### 8.1.1 效果生命周期

```kotlin
/**
 * 状态效果的完整生命周期定义。
 */
data class ActiveEffectV2(
    val id: String,               // 唯一实例 ID
    val effectType: StatusEffectType,
    val sourceId: EntityId,       // 施加来源
    val sourceAbilityId: String,  // 施加的天赋 ID
    var remainingTurns: Int,      // 剩余回合（-1=永久，直到手动移除）
    val magnitude: Double,        // 效果强度（伤害/减速百分比/护盾量等）
    val statModifiers: StatModifier?, // 对属性的修正
    val tickDamage: Int?,         // DoT 每回合伤害
    val tickDamageType: DamageType?, // DoT 伤害通道
    val stackCount: Int = 1,     // 叠加层数
    val maxStacks: Int = 1,      // 最大叠加层数
    val saveDimension: SaveDimension?, // 施加时使用的对抗维度

    // 生命周期回调
    val onApplyCallbackId: String? = null,
    val onTickCallbackId: String? = null,
    val onExpireCallbackId: String? = null,
    val onRemoveCallbackId: String? = null,
)
```

第一版 DoT 时序固定为：

1. `BLEED / BURN / POISON` 在**受影响实体的回合开始、行动前**结算。
2. 若目标处于 `STUN / FREEZE`，DoT 依然正常 tick。
3. DoT 的 tick、致死与后续移除都必须可在 `CombatResolutionTrace` 中定位。
4. 同一 actor 调度点的跨 carrier 总顺序固定为 `ActorEffect -> AreaEffectEmitter -> WorldEffect`；每层内部必须有稳定 tie-break，并在层结束后执行一次死亡检查。

Phase 2 首批非伤害 seed 中，`GUARD` 与 `MARKED` 的语义固定为：

| 状态 | 类别 | 最小效果 | 默认持续 | 引入阶段 |
| --- | --- | --- | --- | --- |
| `GUARD` | BUFF | 提升护甲/格挡效率，或为下次近战命中提供减伤窗口 | 1~3 回合或持续姿态 | P2 |
| `MARKED` | DEBUFF | 使目标更容易被聚焦，承受额外定向伤害或命中修正 | 2~4 回合 | P2 |

#### 8.1.2 效果叠加规则

| 叠加类型 | 规则 | 适用效果 |
| --- | --- | --- |
| **不可叠加，刷新持续时间** | 新的同类效果替换旧的，取较长持续时间 | STUN, ROOT, HASTE, SLOW, FREEZE, SILENCE, MARKED, BANE, CURSE, WEAKEN, OVERCHARGE, STEALTH |
| **可叠加，独立计时** | 每层独立倒计时，效果按层数线性增强 | BLEED, BURN, POISON |
| **可叠加，上限封顶** | 最多 N 层，超过后刷新全部层的持续时间；`ARMOR_BREAK` 为跨来源全局上限 | ARMOR_BREAK（最多 3 层） |
| **不可叠加，取较强** | 同类效果按 `magnitude` 优先、`remainingTurns` 次级比较；较弱者丢弃 | SHIELD, REGEN, GUARD, INVULNERABLE |
| **后来者覆盖** | 同时只保留一个来源，新效果覆盖旧效果 | TAUNT |
| **唯一效果** | 通过 `uniquenessKey / exclusiveGroup / sourceScopedUnique / replacePolicy` 表达，同一来源只能存在一个，不同来源可共存 | 例如某些战吼、姿态、光环类效果 |

#### 8.1.3 效果互斥与优先级

某些效果之间存在互斥关系：

| 效果 A | 效果 B | 交互规则 |
| --- | --- | --- |
| `FREEZE` | `BURN` | 新的覆盖旧的并触发元素交互（见 4.4 节） |
| `HASTE` | `SLOW` | 本阶段都不叠加，只保留单个当前值；统一折算为 `speedModifier` 净值：`effectiveSpeed = baseSpeed + hasteModifier - slowModifier` |
| `STUN` | `STUN` | 不叠加，取较长持续时间 |
| `STEALTH` | 任何 AoE 伤害 | 仅在实际受到伤害时解除隐身 |
| `INVULNERABLE` | 任何伤害 | 伤害为 0，但状态效果仍可施加 |

补充冻结：

1. `CURSE` 与 `WEAKEN` 独立生效，由最终属性自然下限兜底，不额外定义负值奖励。
2. `OVERCHARGE` 在下一次成功承受 `LIGHTNING` 伤害时使该次伤害 `+25%` 并被消耗；若期间再次施加，只刷新持续时间。
3. `TAUNT` 同时只能有一个有效嘲讽源，后来者覆盖前一个来源。
4. `zone effect`、arena aura、world modifier 不再视为 actor status；它们与 actor effect 共用生命周期引擎，但宿主和净化语义不同。
5. `AreaEffectEmitter / WorldEffect` 默认不受 actor 级 `cleanse` 影响；若设计要求可被技能移除，必须单独声明 `remoteRemovalPolicy`，不复用 `dispel` 术语。

#### 8.1.4 净化与驱散

```kotlin
/**
 * 净化规则：移除负面状态。
 */
data class CleanseAction(
    val maxEffectsRemoved: Int = 1,       // 一次净化移除的效果数
    val priorityOrder: CleanseOrder = CleanseOrder.LONGEST_REMAINING,
    val canCleanseTypes: Set<StatusEffectType>? = null,  // null=移除任意
    val excludeTypes: Set<StatusEffectType> = emptySet(), // 无法被净化的类型
)

enum class CleanseOrder {
    MOST_RECENT,     // 最近施加的优先移除
    LONGEST_REMAINING, // 剩余时间最长的优先
    HIGHEST_MAGNITUDE, // 强度最高的优先
}
```

`PURIFY/CLEANSE` 是瞬时操作，不进入 `ActiveEffectV2`、不参与叠层，也不写入正式存档。

第一版推荐净化配置：

1. 若存在 `STUN / ROOT`，优先清除这两类硬控。
2. 否则按 `LONGEST_REMAINING` 处理。

**不可被净化的效果**：`INVULNERABLE`、`STEALTH`、Boss phase 锁定状态。`KNOCKBACK` 作为瞬时位移效果处理，不进入持久状态矩阵，也不属于净化目标。

### 8.2 铭文系统设计（Phase 3 引入）

铭文（Inscription）是 ToME 风格的核心构筑维度之一。玩家可以在角色上激活有限数量的铭文，提供额外的主动能力。

#### 8.2.1 铭文定义

```kotlin
/**
 * 铭文定义。铭文独立于天赋系统，提供额外的主动技能栏位。
 */
data class InscriptionDef(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val iconKey: String,
    val category: InscriptionCategory,
    val cooldown: Int,
    val effect: InscriptionEffect,
    val tier: Int,               // 1~3，高阶铭文效果更强但更稀有
)

enum class InscriptionCategory {
    /** 治疗铭文：恢复 HP */
    HEALING,
    /** 位移铭文：传送/冲刺 */
    MOVEMENT,
    /** 防护铭文：临时护盾/减伤 */
    PROTECTION,
    /** 净化铭文：移除负面状态 */
    CLEANSING,
    /** 攻击铭文：额外伤害/增益 */
    OFFENSE,
}

sealed interface InscriptionEffect {
    data class Heal(val amount: Int, val percentMax: Float = 0f) : InscriptionEffect
    data class Teleport(val range: Int) : InscriptionEffect
    data class Shield(val amount: Int, val duration: Int) : InscriptionEffect
    data class Cleanse(val count: Int, val alsoHeal: Int = 0) : InscriptionEffect
    data class DamageBoost(val multiplier: Float, val duration: Int, val damageType: DamageType?) : InscriptionEffect
}
```

#### 8.2.2 铭文栏位规则

| 规则 | 值 | 说明 |
| --- | --- | --- |
| 最大铭文数 | 4 | 角色最多同时装备 4 个铭文 |
| 同类限制 | 每类最多 2 个 | 不允许装 3 个治疗铭文 |
| 铭文来源 | 掉落 / 任务 / Boss 奖励 | `Phase 3` 以前不依赖商店/制作；若后续引入经济系统，再单独扩展来源 |
| 铭文消耗 | 有冷却无资源消耗 | 铭文不消耗 Stamina/Mana 等 |
| 铭文热键 | 5~8 键 | 与天赋热键 1~4 分开 |

#### 8.2.3 基础铭文清单（Phase 3 交付）

| 铭文 ID | 类别 | 阶 | 冷却 | 效果 |
| --- | --- | --- | --- | --- |
| `healing_light` | HEALING | 1 | 15 | 恢复最大 HP 的 20% |
| `healing_surge` | HEALING | 2 | 12 | 恢复最大 HP 的 35% |
| `healing_miracle` | HEALING | 3 | 20 | 恢复最大 HP 的 50% + 净化 1 个负面 |
| `phase_door` | MOVEMENT | 1 | 20 | 随机传送到 10 格范围内 |
| `controlled_phase` | MOVEMENT | 2 | 15 | 传送到视野内指定位置（8 格） |
| `iron_shield` | PROTECTION | 1 | 18 | 获得等于 CON×4 的护盾，持续 5 回合 |
| `diamond_shield` | PROTECTION | 3 | 25 | 获得等于 CON×8 的护盾 + 3 回合免疫控制 |
| `purge` | CLEANSING | 1 | 12 | 移除 1 个负面状态 |
| `greater_purge` | CLEANSING | 2 | 15 | 移除 2 个负面状态 + 3 回合免疫同类状态 |

---

## 9. 各阶段细节补充

### 9.1 Phase 2 细节补充

#### 9.1.1 P2-A（合同与迁移基线）补充

**序列化合同决策**：`main` 基线已经使用 `kotlinx.serialization`，因此 `P2-W1` 不再讨论引擎迁移，而是继续冻结 `Save Schema V2`、版本纪律与资产边界。执行要点：

1. 所有进入正式存档路径的数据类必须带 `@Serializable`
2. 多态模型统一使用 `sealed class` + `@SerialName`
3. `classDiscriminator` 固定为 `"type"`
4. 不再兼容旧 Phase 1 临时存档；版本不匹配直接 fail-fast
5. `saveContractVersion` 与 `asset/style/manifest` 版本不得混用

**1000 能量制迁移**：

| 参数 | Phase 1 值 | Phase 2 值 | 迁移方式 |
| --- | --- | --- | --- |
| 行动阈值 | 100 | 1000 | 常量替换 |
| 标准速度 | 100 | 1000 | `speed` 值 ×10 |
| 快速行动消耗 | 不支持 | 750 | 新增 `castTime` 字段 |
| 慢速行动消耗 | 不支持 | 1250 | 新增 |
| 排序键 | `entityId asc` | `energy desc → entityId asc` | 修改 `TurnScheduler` |

**DamageType 在 Phase 2 的最小引入**：

- 定义完整的 6 种 `DamageType` 枚举
- Phase 2 中实际使用：`PHYSICAL`（战卫/游荡者主链）、`FIRE`/`COLD`（奥术师主链）、`HOLY`（圣堂武士主链）、`SHADOW`（游荡者毒刃/处刑系语义）
- 抗性/穿透系统在 Phase 2 使用简化版：`effectiveResistance = targetResistance - penetration`，不做收益递减
- Phase 3 升级为完整的非线性模型

**会话编排边界**：

`Phase 2` 引入事件总线、资源池和新状态后，不允许继续把规则堆进单一会话巨类。推荐最小拆分为：

1. `TurnSystem`：行动队列、能量推进、回合边界事件
2. `CombatSystem`：命中、伤害、状态施加、`CombatResolutionTrace`
3. `TalentSystem`：主动/持续技能、冷却、资源消耗
4. `InventorySystem`：物品、装备、掉落、使用
5. `ProgressionSystem`：经验、升级、奖励
6. `GameSession`：只做编排、查询入口和 snapshot 提供者

#### 9.1.1.1 Save Schema V2 最小模型

```kotlin
@Serializable
data class SaveDataV2(
    val saveVersion: Int,
    val saveContractVersion: Int,
    val seed: Long,
    val turn: Int,
    val zoneId: String,
    val floorIndex: Int,
    val difficultyId: String = "normal",
    val localeId: String? = null,
    val nextEntityId: Long,
    val player: SerializedEntity,
    val monsters: List<SerializedEntity>,
    val items: List<SerializedItem>,
    val mapState: SerializedMapState,
    val worldProgress: WorldProgressSnapshot,
    val aiState: List<SerializedAiState>,
    val logHistory: List<LogTokenEvent>,
)

@Serializable
data class SerializedMapState(
    val width: Int,
    val height: Int,
    val exploredCells: Set<Point>,
    val openedDoors: Set<Point>,
    val terrainOverrides: Map<Point, TerrainOverride>,
    val groundItems: Map<Point, List<String>>,
)

@Serializable
data class WorldProgressSnapshot(
    val questStates: Map<String, ObjectiveStateSnapshot>,
    val worldFlags: Set<String>,
    val unlockedRoutes: Set<String>,
    val defeatedBossIds: Set<String>,
    val claimedRouteRewards: Set<String>,
)
```

保存范围冻结为：

1. 存规则态，不存渲染态；禁止把 `glyph/color/displayName/messageString` 直接写入正式存档。
2. `EntityId` 使用本局局部稳定 ID 持久化；恢复后继续从 `nextEntityId` 递增。
3. AI 只保存最小必要状态，如 `perceptionState`、`hateFocus`、脚本阶段、冷却和 patrol index；不保存 path cache。
4. 地图必须保存已探索格、门开关、地形覆盖、地面掉落、Boss 房状态等会影响规则的内容。
5. `WorldProgressSnapshot` 必须能无损 round-trip `questStates / worldFlags / unlockedRoutes / defeatedBossIds / claimedRouteRewards`；坏档或缺字段按 fail-fast 处理。

#### 9.1.1.2 RenderSnapshot 更新协议

`RenderSnapshot` 只承担逻辑可视化快照，不承担动画编排。协议固定为：

1. `core/game` 在每次**已提交的世界状态变化**后发布 `WorldStateChangedEvent`。
2. client 收到该事件后拉取最新 `RenderSnapshot`；不订阅内部半成品中间态。
3. 最低刷新时机包括：`session load`、玩家行动提交、AI 整轮提交、切层/切区、存档恢复完成。
4. `RenderSnapshot` 只包含 tile、actor、overlay、HUD、telegraph 等逻辑表现字段；浮字、震屏、Tween、过渡动画由 client 根据事件自行驱动。
5. 伤害数字、miss、暴击提示、阶段切换 warning 通过 `CombatEvent/StatusEvent/LogEvent` 传递，不作为 snapshot 历史缓存的一部分。
6. `Status HUD` 的 steady-state（icon / 层数 / 剩余回合）必须来自 `RenderSnapshot`；`StatusEvent` 只负责覆盖、净化、隐匿打破等瞬时反馈。
7. `ItemRenderSnapshot.specialTierId` 是 presentation-only 字段，只能与 `specialTemplateId` 成对出现，用于 client 区分 `UNIQUE / ARTIFACT` accent；它不进入 save schema，不允许 client 通过 content template 反查 special tier。

#### 9.1.1.3 Phase 2 经济、难度与输入最小合同

为避免短局阶段继续扩 scope，Phase 2 直接冻结：

1. 不做商店、货币和制作系统。
2. 装备与消耗品只来自初始套装、掉落、任务奖励和 Boss 奖励。
3. 难度仅交付 `Normal` 一个正式档位，但数据结构上必须预留 `DifficultyDef`。
4. 难度只能在新开局选择，进行中的 session 不允许切换。
5. 输入范围固定为键盘 + 鼠标，不做手柄和触屏。

```kotlin
data class DifficultyDef(
    val id: String,
    val nameKey: String,
    val monsterHpMultiplier: Float = 1.0f,
    val monsterDamageMultiplier: Float = 1.0f,
    val xpMultiplier: Float = 1.0f,
    val lootRarityBonus: Float = 0.0f,
)
```

推荐最小输入映射：

| 输入 | 行为 |
| --- | --- |
| `WASD / 方向键` | 移动 |
| `1 ~ 4` | 职业主动技能 |
| `5 ~ 8` | 铭文/额外主动栏 |
| `I` | 背包 |
| `C` | 角色面板 |
| `Tab` | 检视 / 目标切换 |
| 鼠标左键 | 移动 / 攻击 / 交互 |
| 鼠标右键 | 检视 / tooltip |

#### 9.1.2 P2-B（最小 Tile 可玩切片）补充

**Zone 设计规格**：

| Zone ID | 中文名 | 英文名 | 层数 | 地图尺寸 | 推荐等级 | 环境主题 | 特殊机制 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `shattered_outpost` | 破碎前哨 | Shattered Outpost | 2 | 60×40 | 1~4 | 废墟/石材/杂草 | 教程区域，简单陷阱 |
| `greenwood_fringe` | 绿林边缘 | Greenwood Fringe | 2 | 70×45 | 3~6 | 森林/小溪/苔藓 | 视野遮挡（树木），巡逻怪 |
| `deep_iron_pit` | 深铁矿坑 | Deep Iron Pit | 2 | 80×50 | 5~8 | 矿洞/铁轨/熔炉 | 熔岩地形（持续火焰伤害），矿车机关 |
| `grey_gate_depths` | 灰门深窟 | Grey Gate Depths | 2 | 80×50 | 7~10 | 古城地下/石柱/暗河 | Boss 区域，含灰门王座区域 Boss 房 |

**每个 Zone 的怪物池**：

| Zone | 普通怪（4~5 种） | 精英怪（1 种） | Boss |
| --- | --- | --- | --- |
| 破碎前哨 | 鼠, 哥布林, 骷髅, 强盗 | 强盗头目 | 无 |
| 绿林边缘 | 狼, 蜘蛛, 林地精, 毒蛇, 树人幼体 | 蛛后 | 无 |
| 深铁矿坑 | 矿工亡灵, 火蜥蜴, 铁傀儡, 蝠群 | 锻炉守卫 | 熔岩巨人（中间 Boss） |
| 灰门深窟 | 暗影行者, 骨弓手, 深渊爬行者, 堕落骑士, 召灵师 | 暗影领主 | 地牢主宰（最终 Boss） |

#### 9.1.3 P2-C（短局扩展）补充

**SoloClearLab V1 设计**：

每个职业必须在以下三种标准化场景中验证通过：

| 场景 | 配置 | 通过标准 |
| --- | --- | --- |
| 杂兵包 | 6 只普通怪在 10×10 封闭房间 | 清光所有怪物，HP 剩余 >30% |
| 精英战 | 1 只精英 + 2 只普通怪在 15×15 房间 | 击杀精英，HP 剩余 >0 |
| Boss 战 | 地牢主宰在 20×20 Boss 房 | 击杀 Boss |

测试参数：固定 seed（3 个黄金 seed）、标定等级（杂兵=5，精英=7，Boss=10）、标定装备预算（阶段对应的蓝色品质装备全套）。

### 9.2 Phase 3 细节补充

#### 9.2.1 P3-A（战斗深度核心）补充

**战斗公式升级清单**：

1. 命中判定从线性模型切换到 Sigmoid 模型（3.2.1 节）
2. 引入 Power/Save 对抗体系（3.4 节）
3. 引入收益递减模型（3.5 节）
4. 引入 `CombatResolutionTrace / TraceEnvelope` 记录系统（3.3 节）
5. 元素抗性/穿透从简化版升级为完整百分比模型
6. 引入暴击抗性和暴击伤害属性

**Golden Seed 基线变更声明**：由于战斗公式从线性切换为非线性，Phase 2 的 `FORMULA` corpus / Golden Seed 基线在 Phase 3 开始时需要全量重新录制。这是预期内的破坏性变更。自 Phase 3 起，golden 产物至少携带 `phaseId / rulesetVersion / traceSchemaVersion / corpusId`。

#### 9.2.2 P3-B（构筑与角色扩展）补充

**进阶职业解锁条件**：

| 进阶职业 | 解锁条件 | 说明 |
| --- | --- | --- |
| 狂战士 | 战卫通关 | 基于战卫的进阶 |
| 咒剑士 | 奥术师通关 | 基于奥术师的进阶 |
| 影刃客 | 游荡者通关 | 基于游荡者的进阶 |
| 守林者 | 圣堂武士通关 | 基于圣堂武士的进阶 |

说明：`Phase 3` 只有 `Normal` 难度，因此“通关”即击败 `深渊之心` 并生成 run summary；若后续阶段引入更多难度选项，任意难度通关均满足解锁条件。

**开发态可用性与正式解锁分离**：

```kotlin
enum class ClassUnlockState {
    LOCKED,
    DEV_UNLOCKED,
    RELEASE_UNLOCKED,
}

enum class AvailabilityContext {
    PLAYER_CREATION,
    DEV_LAB,
    WHITE_BOX,
}

enum class ClassPlayabilityState {
    LOCKED,
    UNLOCKED_BUT_UNAVAILABLE,
    PLAYABLE,
}
```

说明：

1. `DEV_UNLOCKED` 仅用于实验室、白盒验证和开发调试，不代表正式玩家已解锁。
2. `P3-W5` 可以让 `Berserker / Spellblade` 以 `DEV_UNLOCKED` 进入可玩验证。
3. 只有正式通关条件满足后，才写入 `RELEASE_UNLOCKED` 局间数据。
4. `ClassAvailabilityResolver` 是唯一的映射出口：
   - `PLAYER_CREATION`：`LOCKED -> LOCKED`，`DEV_UNLOCKED -> UNLOCKED_BUT_UNAVAILABLE`，`RELEASE_UNLOCKED -> PLAYABLE`
   - `DEV_LAB / WHITE_BOX`：`LOCKED -> LOCKED`，`DEV_UNLOCKED -> PLAYABLE`，`RELEASE_UNLOCKED -> PLAYABLE`
5. UI 不得绕过 resolver 自己猜测职业卡片状态。

**种族天赋点分配**：种族天赋点独立于职业天赋点。每 4 级获得 1 点种族天赋点，用于投资种族天赋树。

**局间持久化最小 schema**：

```kotlin
@Serializable
data class ProfileData(
    val profileVersion: Int,
    val releaseUnlockedClasses: Set<String>,
    val runHistory: List<RunSummary>,
)

@Serializable
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

冻结边界：

1. `ProfileData` 与 `SaveDataV2` 分文件、分版本号管理，不允许混存。
2. 当前 run 内的装备、货币、临时状态和地图探索信息不得进入 `ProfileData`。
3. 正式进阶职业解锁只写入 `releaseUnlockedClasses`；`DEV_UNLOCKED` 不进入局间档。
4. run 结束摘要必须写结构化 `RunSummary`，以支撑实验室复现、玩家 run history 与回归分析。

#### 9.2.3 P3-C（长局结构）补充

**世界分支结构**：

```
破碎前哨 (Lv1-4)
    ├── 绿林边缘 (Lv3-6)
    │       ├── 精灵遗迹 [可选] (Lv5-7)
    │       ├── 盗贼营地 [可选] (Lv3-5)
    │       └── 深铁矿坑 (Lv5-8)
    │               ├── 熔岩核心 [可选] (Lv7-9)
    │               └── 灰门深窟 (Lv7-10，含灰门王座 Boss 房)

Phase 3 扩展：
    灰门深窟 ──> 地下河 (Lv10-12)
                     ├── 水晶洞穴 [可选] (Lv11-13)
                     └── 深渊神殿 (Lv12-15)
                             └── 深渊之心 [最终 Boss] (Lv15)
```

说明：`灰门王座` 在 `Phase 3` 继续作为 `grey_gate_depths` 内部的区域 Boss 房存在，不单独作为 world graph 的独立 zone 节点。

**WorldProgress 最小合同**：

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

data class GateCondition(
    val requiredQuestId: String? = null,
    val requiredWorldFlag: String? = null,
    val requiredBossKill: String? = null,
)

data class RouteReward(
    val routeId: String,
    val claimPolicy: RewardClaimPolicy = RewardClaimPolicy.ON_FIRST_ROUTE_CLEAR,
    val shardReward: Int,
    val guaranteedDropIds: List<String>,
    val rescueTags: Set<String>,
)
```

说明：

1. `world graph` 只表达连接关系；主支线推进必须由 `WorldProgress / Quest / GateCondition / RouteReward` 表达。
2. `SaveDataV2` 的 `worldProgress` 必须无损保留 `worldFlags / unlockedRoutes / defeatedBossIds / claimedRouteRewards`，否则读档后 gate / reward 语义会漂移。
3. `RouteReward.claimPolicy` 必须显式声明是“首次解锁发”还是“首次通关发”，不允许靠 `worldFlags` 侧推。
4. 商店与路线奖励必须能为 build 提供最小救火路径，不允许完全依赖随机掉落。

**Affix V1 设计**（Phase 3 引入前缀/后缀词缀的基础版）：

| 词缀类型 | Phase 3 数量 | 示例 |
| --- | --- | --- |
| 武器前缀 | 12 种 | 锋利的(+攻击)、燃烧的(+火伤)、冰霜的(+冰伤)、雷击的(+雷伤) |
| 武器后缀 | 10 种 | 力量之(+STR)、速度之(+攻速)、吸血之(命中回血)、穿甲之(+护甲穿透) |
| 防具前缀 | 10 种 | 坚固的(+护甲)、抗火的(+火抗)、镇定的(+Mental Save) |
| 防具后缀 | 8 种 | 生命之(+HP)、再生之(+HP回复)、抗性之(+全元素抗) |

补充冻结：

1. `AffixTagWeighting` 用于把 affix 与职业、伤害通道、资源轴建立相关性。
2. `AffixBlacklist` 用于避免明显无效或误导组合。
3. `RescueInventoryPolicy` 用于约束固定商店节点必须提供位移 / 净化 / 护盾等保底工具。
4. `AffordableRescueSlotPolicy` 还必须冻结每个 checkpoint 的 `expectedShardBudgetByCheckpoint / mandatoryAffordableItemCount / requiredAffordableTags`，确保关键救火工具不只是“货架上有”，而且“预算内买得起”。

### 9.3 Phase 4 细节补充

#### 9.3.1 ProcGen 深化目标

**混合地图生成器需求**：

| 特性 | BSP（Phase 1） | 混合生成器（Phase 4） |
| --- | --- | --- |
| 房间形状 | 矩形 | 矩形 + L 形 + 圆形 + 不规则 |
| 走廊 | L 型连接 | 弯曲走廊 + 隐藏通道 |
| 拓扑 | 树形（每两点仅 1 条路径） | 含环路（多条路径可选） |
| 特殊房间 | 无 | Vault（高风险高回报）、陷阱房、宝藏房 |
| 锁钥匙 | 无 | 钥匙/开关/Boss 门的拓扑验证 |
| biome 变体 | 单一 | 同层内可有 2 种 biome 混合 |

Phase 4 的 mapgen 不采用通用脚本或 WFC 深水区；执行口径固定为：

1. `MapgenRequest -> TopologyGraph -> RoomInstance -> Corridor/Loop -> TerrainTag paint -> validation`
2. 地图生成与可解性验证共用一份抽象词汇：`TopologyGraph / SolvabilityGraph / PathClass`
3. `Phase 4` 不新增 world node，只升级 `Phase 3` 已冻结 zone 的 biome mix、vault 与 secret 逻辑

说明：本节只保留锚点级骨架；完整字段集、YAML 示例和验证门槛以 `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` 为执行权威。同时，本节的类型名与 `phase4` 执行文档保持一致；若字段细节不同，以 `phase4` 执行文档为权威。

```kotlin
enum class TerrainTag {
    WATER,
    OIL,
    ICE,
}

data class MapgenRequest(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val targetWidth: Int,
    val targetHeight: Int,
)

data class ZoneMapgenProfile(
    val zoneId: String,
    val allowedBiomeFamilies: Set<String>,
    val loopCountRange: IntRange,
    val vaultPool: Set<String>,
)

data class TopologyNode(
    val id: String,
    val roomDefId: String,
)

data class TopologyEdge(
    val from: String,
    val to: String,
)

data class TopologyGraph(
    val nodes: List<TopologyNode>,
    val edges: List<TopologyEdge>,
)

data class GeneratedFloor(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val topology: TopologyGraph,
)

interface MapgenPipeline {
    fun run(request: MapgenRequest): GeneratedFloor
}

data class SolvabilityNode(
    val id: String,
    val pathClass: PathClass,
)

data class SolvabilityEdge(
    val from: String,
    val to: String,
)

data class SolvabilityGraph(
    val entryNodeId: String,
    val exitNodeId: String,
    val nodes: List<SolvabilityNode>,
    val edges: List<SolvabilityEdge>,
)
```

补充约束：

1. 地形标签第一版固定为 `WATER / OIL / ICE`，并由 `CombatPipeline` 的步骤 `9` 接入元素交互检查。
2. 环路数量每层 `0 ~ 2`，`vault` 只允许落在 `OPTIONAL / SECRET` 路径。
3. `PERCEPTION_REVEAL` 可以用于 hidden entrance，但 discovery 失败不应阻断主线。

#### 9.3.2 掉落生态 V2

**掉落生成参数总览**：

```kotlin
data class LootRollContext(
    val sourceLevel: Int,
    val sourceTier: SourceTier,
    val zoneId: String,
    val playerLevel: Int,
    val magicFindBonus: Float,
    val seed: Long,
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

data class LootBudget(
    val iLvl: Int,
    val qLvl: Int,
    val rarityScore: Float,
    val affixBudget: Int,
)
```

补充约束：

1. `MagicFind` 只修正 rarity roll，不直接提高 `iLvl`；允许来源为装备 affix、临时 buff、区域 modifier，最终 clamp 到 `1.0`。
2. `iLvl / qLvl / affixBudget` 的正式计算以 `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` 为执行权威。
3. `Unique / Artifact` 必须是预定义模板，不允许退化成随机 affix 伪稀有。
4. 任何来自 affix、elite mutation、artifact proc 的元素伤害，都必须复用 `ElementInteractionRule` 与 `CombatPipeline` 正式路径。
5. `castSpeed` 的收益递减在 `Phase 4` 正式启用；任何掉落词条都不能绕过 `DR_CAST_SPEED_C`。
6. 元素亲和度系统只保留为 optional lab，不进入 `Phase 4` 主线出口。

#### 9.3.3 Content Pack Overlay 最小边界

```kotlin
data class ContentPackManifest(
    val id: String,
    val version: String,
    val schemaVersion: Int,
    val gameVersionRange: String,
    val namespace: String,
)

enum class OverlayOp {
    ADD,
    REPLACE,
    APPEND,
    DENY,
}
```

冻结口径：

1. pack 只能扩内容定义与表现资源声明，不能注入新的规则解释器、存档格式或运行时脚本宿主。
2. `Phase 4` 只保证 `schemaVersion` 匹配且 `gameVersionRange` 覆盖当前 base game 的 pack 兼容；schema bump 不提供自动迁移。
3. manifest、i18n、visual/audio key 与 headless harness 结果都必须进入 pack lint。
4. pack 不允许新增职业或修改 `core` 规则常量，只能通过 registry overlay 扩展 `game` 内容层。

### 9.4 Phase 5 细节补充

说明：本节只保留锚点级骨架；完整字段集、YAML/JSON 示例、量化门槛与工作包依赖以 `docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md` 为执行权威。同时，本节的类型名与 `phase5` 执行文档保持一致；若字段细节不同，以 `phase5` 执行文档为权威。

#### 9.4.1 Utility AI 评分层

`Phase 5` 的 Utility AI 不是从零开始，而是在 Layer 2 脚本化行为之上增加一个评分层：

```kotlin
data class Consideration(
    val evaluatorId: String,
    val evaluator: UtilityEvaluator,
    val weight: Float = 1.0f,
)

fun interface UtilityEvaluator {
    fun evaluate(context: AIContext): Float
}

data class AIContext(
    val self: EntitySnapshot,
    val target: EntitySnapshot?,
    val alliesInRadius: List<EntitySnapshot>,
    val enemiesInRadius: List<EntitySnapshot>,
    val distanceToTarget: Int,
    val selfHpPercent: Float,
    val targetHpPercent: Float?,
    val availableTalents: List<String>,
    val currentTurn: Int,
    val terrainAtSelf: TileType,
    val terrainAroundSelf: Map<Point, TileType>,
    val terrainTagsAtSelf: Set<TerrainTag>,
    val nearbyTerrainInteractionIds: List<String>,
    val activeMutationIds: List<String>,
    val bossPhaseId: String?,
)

enum class TacticalSelectionReason {
    UTILITY_BEST,
    DSL_OVERRIDE,
    FALLBACK,
}

data class ScoredCandidate(
    val actionId: String,
    val score: Float,
    val evaluatorId: String,
)

data class TacticalAIDecisionTrace(
    val turnId: Long,
    val actorId: Int,
    val profileId: String,
    val perceivedTargetId: Int?,
    val matchedRuleIds: List<String>,
    val selectedAction: String,
    val candidates: List<ScoredCandidate>,
    val finalScore: Float,
    val selectionReason: TacticalSelectionReason,
)
```

冻结口径：

1. `DSL` 规则先做硬约束与 veto，`Utility` 评分只在通过约束的候选集中排序。
2. 普通怪继续使用 Layer 1/Layer 2；精英怪可选择性叠加 Utility；Boss 由 Utility 决定高层意图，再回到既有 action catalog 执行。
3. YAML 定义层 `*Def` 类型与 loader 解析规则以 `phase5` 执行文档为权威；本节不重复展开。

#### 9.4.2 感知、仇恨与性能锚点

```kotlin
enum class PerceptionState {
    UNAWARE,
    SUSPICIOUS,
    ALERT,
    SEARCHING,
}

enum class HateSource {
    VISUAL,
    SOUND,
    DAMAGE_RECEIVED,
    ALLY_REPORT,
    TAUNT,
}

data class HateFocus(
    val targetId: EntityId,
    val lastKnownPosition: Point?,
    val confidence: Float,
    val source: HateSource,
    val updatedTurn: Int,
)

data class SoakReport(
    val buildId: String,
    val durationMinutes: Int,
    val runsCompleted: Int,
    val crashes: Int,
    val peakHeapMb: Int,
    val postWarmupHeapDriftMb: Int,
    val maxGcPauseMs: Int,
    val p1Fps: Float,
)
```

| 指标 | 目标值 | 测量方式 |
| --- | --- | --- |
| 帧率 | 桌面端稳定 `60 FPS` | GLProfiler + 帧间时间统计 |
| Draw Calls / 帧 | `< 50` | GLProfiler |
| Texture Bindings / 帧 | `< 10` | GLProfiler |
| FOV 计算耗时 | `< 2 ms`（`80×50`） | Benchmark 测试 |
| A* 寻路耗时 | `< 1 ms`（`80×50`） | Benchmark 测试 |
| 内存占用 | `< 512 MB` | JVM 监控 |
| 长局稳定性 | `8h` 无 OOM / 无 GC 停顿 `> 50 ms` | Soak 测试 |

#### 9.4.3 回放、run history 与死因分析

```kotlin
data class TurnSummary(
    val turn: Int,
    val playerActionId: String?,
    val damageTaken: Int,
    val healingReceived: Int,
    val appliedEffectIds: List<String>,
    val removedEffectIds: List<String>,
    val startPosition: Point,
    val endPosition: Point,
)

enum class DeathSuggestionKey {
    LOW_RESISTANCE,
    LOW_ARMOR,
    NO_ESCAPE_TOOL,
    STACKED_DOT,
    MISPLAY_TELEGRAPH,
}

data class DeathSuggestion(
    val key: DeathSuggestionKey,
    val args: Map<String, String>,
)

data class DeathAnalysis(
    val turn: Int,
    val killerEntityId: EntityId,
    val killerName: String,
    val killingAbilityId: String,
    val damageType: DamageType,
    val finalDamage: Int,
    val playerHpBefore: Int,
    val combatTrace: CombatResolutionTrace,
    val last5Turns: List<TurnSummary>,
    val activeEffectsAtDeath: List<ActiveEffectV2>,
    val suggestions: List<DeathSuggestion>,
)

data class ReplayHeader(
    val schemaVersion: Int,
    val phaseId: String,
    val buildId: String,
    val seed: Long,
    val professionId: String,
    val raceId: String,
    val difficultyId: String,
    val zoneRoute: List<String>,
)

data class RunHistoryEntry(
    val runId: String,
    val buildId: String,
    val seed: Long,
    val professionId: String,
    val raceId: String,
    val difficultyId: String,
    val result: String,
    val zoneReachedId: String,
    val turnsPlayed: Int,
    val durationSeconds: Long,
)
```

---

## 10. 附录

### 10.1 关键常量汇总表

| 常量 | 值 | 用途 | 所在节 |
| --- | --- | --- | --- |
| `ACTION_THRESHOLD` | 1000 | 行动能量阈值 | 9.1.1 |
| `K_ARMOR` | 100 | 护甲减免半收益常数 | 3.2.2.3 |
| `RESISTANCE_CAP` | 75 | 元素抗性上限 | 3.2.2.4 |
| `RESISTANCE_FLOOR` | -25 | 穿透后抗性下限 | 3.2.2.4 |
| `BASE_CRIT_MULTIPLIER` | 1.5 | 暴击基础倍率 | 3.2.2.2 |
| `CRIT_CHANCE_CAP` | 0.50 | 暴击率上限 | 3.2.2.2 |
| `HIT_SIGMOID_K` | 0.04 | 命中 Sigmoid 斜率 | 3.2.1 |
| `HIT_SIGMOID_M` | -10 | 命中 Sigmoid 中点偏移 | 3.2.1 |
| `SAVE_SIGMOID_K` | 0.05 | Power/Save Sigmoid 斜率 | 3.4.3 |
| `MIN_HIT_CHANCE` | 0.05 | 最低命中率 | 3.2.1 |
| `MAX_HIT_CHANCE` | 0.95 | 最高命中率 | 3.2.1 |
| `MIN_APPLY_CHANCE` | 0.10 | 状态最低施加率 | 3.4.3 |
| `MAX_APPLY_CHANCE` | 0.90 | 状态最高施加率 | 3.4.3 |
| `DR_EVASION_C` | 150 | 闪避收益递减常数 | 3.5.3 |
| `DR_CRIT_C` | 200 | 暴击评级收益递减常数 | 3.5.3 |
| `DR_CAST_SPEED_C` | 100 | 施法速度收益递减常数 | 3.5.3 |
| `DR_HP_REGEN_C` | 80 | 生命回复收益递减常数 | 3.5.3 |
| `MAX_INSCRIPTION_SLOTS` | 4 | 铭文栏位上限 | 8.2.2 |
| `ELEMENT_INTERACTION_DEPTH` | 2 | 元素交互链最大深度 | 4.4.2 |

### 10.2 术语表

| 术语 | 中文 | 英文 | 定义 |
| --- | --- | --- | --- |
| Solo-Clear Contract | 单通合同 | Solo-Clear Contract | 职业独立通关的六条能力线检验标准 |
| DamageType | 伤害通道 | Damage Channel | 六大伤害类型之一 |
| CombatResolutionTrace | 战斗追踪 | Combat Resolution Trace | 记录完整战斗结算过程的审计数据 |
| Power/Save | 强度/豁免 | Power vs Save | 状态效果施加的三维对抗体系 |
| Diminishing Returns | 收益递减 | Diminishing Returns | 高属性区间边际收益下降的数学模型 |
| Telegraph | 预警 | Telegraph | Boss/精英技能释放前的视觉警告 |
| Inscription | 铭文 | Inscription | 独立于天赋的额外主动技能栏位 |
| ResourcePool | 资源池 | Resource Pool | 职业差异化的能力消耗资源 |
| Layer 1/2/3 AI | AI 分层 | AI Layers | 从基础模板到 Utility AI 的三层递进架构 |
| Golden Seed | 黄金种子 | Golden Seed | 用于确定性回归测试的固定随机种子 |
| Keyword Registry | 关键词注册表 | Keyword Registry | 统一术语定义和 tooltip 的注册系统 |

---

> 本文档构成 K-ToME 核心系统的权威设计参考。所有后续阶段的开发应以本文档中的数据结构、公式和规则为锚点。
