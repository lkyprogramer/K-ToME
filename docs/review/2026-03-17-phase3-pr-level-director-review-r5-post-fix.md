# Phase 3 PR 级开发文档深度审阅 — R5（修订后复查）

**日期**：2026-03-17
**审阅视角**：资深游戏设计与开发总监
**审阅方法**：在 R4 交叉审阅所提修订全部落地后，对 6 份 PR 文档和两份上游参考文档进行逐行复查
**前置审阅**：R1-R3（内部一致性）→ R4（交叉对齐）→ 本轮 R5（修订后残留问题）

**参考基线**：

1. `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`（以下简称"详设文档"）
2. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（以下简称"补充设计文档"）

---

## 0. 直接结论

R4 提出的 **3 P0 / 8 P1 / 9 P2** 已全部在文档层完成收口：

- 详设文档的命中公式已正式倒向 Sigmoid + `accuracy / evasion`
- Telegraph 统一为 `TelegraphSpec` + `LOW / MODERATE / HIGH / LETHAL`
- Targeting 枚举统一为 8 类 + `WALL / SUMMON_SLOT` 标为 Phase 4+ 保留
- PR-03 补齐了 `EffectOp` sealed class、`ActionCost` 引用、`resourceCosts: List<ResourceCost>`、`TalentAiHints`、`TalentBreakpoint`、`TalentAllocationDraft(ownerType + treeOwnerId)`
- PR-01 补齐了回合 9 步与 CombatPipeline 12 步的嵌套关系、Power/Save 属性来源引用、回调控制流合同、`armorPenetration` 不进 DR、`P2toP3FormulaComparisonTest` 原因标签
- PR-04 补齐了 `counterplayTags` + `stages` 预留 + `ThreatProfileDef` 注册表
- PR-05 补齐了 `SoloContractDef` + `DecayPolicy` 映射 + `ClassAvailabilityResolver`
- PR-06 补齐了 `WorldGraph` 方向性声明 + `MonsterTemplateV2` 引用 + `RouteReward.levelBandRef` + `AffordableRescueSlotPolicy`

**R4 的 P0 已清零。** 本轮复查聚焦于修订后新暴露的残留缝隙和最细粒度问题。

最终结论：**0 P0 / 7 P1 / 12 P2**。

P1 集中在"类型引用了但枚举值从未列出"这一模式，修复成本极低（每个补 3-5 行枚举定义）。P2 全部是"不影响编译但会让实施者多花 10 分钟查阅"的细节。

---

## 1. P1 问题（应在编码前补齐）

### 1.1 [PR-02] `OVERCHARGE` 消耗效果未量化

**位置**：PR-02 §4.2 L111 / §2.1 L53-56

**问题**：

PR-02 只说"`OVERCHARGE` 在下一次成功承受 `LIGHTNING` 伤害后被消耗"，但**没有说明消耗时产生什么机械效果**。补充设计文档 §3.1（L205）和元素关联状态表（L1313）明确定义了：

> 过载（`OVERCHARGE`）：目标下次受到的闪电伤害 **+25%**

这是一个关键的数值参数——`+25%` 闪电增伤是 `OVERCHARGE` 存在的全部意义。但 PR-02 作为状态生命周期的权威文档，完全遗漏了这个参数。实施者会遇到"OVERCHARGE 被消耗了，然后呢？"的问题。

**建议**：

在 PR-02 §4.2 的 `OVERCHARGE` 条目补充：

```
| `OVERCHARGE` | `debuff` | 2 turns | 不叠加，只刷新；挂在受害者身上，
  在其下一次成功承受 `LIGHTNING` 伤害时消耗，并使该次伤害 +25% |
```

并在 §4.2 补充合同中加一条："消耗 `OVERCHARGE` 产生的增伤在 `CombatPipeline` 的 `Pre-reduction callbacks` 阶段应用，增伤只作用于触发消耗的那一次 `LIGHTNING` 伤害。"

---

### 1.2 [PR-02] `KNOCKBACK` 存在性矛盾

**位置**：PR-02 §4.7 L238 / §2.1 L51

**问题**：

`KNOCKBACK` 出现在不可净化集合（`INVULNERABLE / KNOCKBACK / STEALTH / Boss phase 锁定状态`），但：

1. 不在 14 个 Phase 2 状态中（§4.1）
2. 不在 7 个 Phase 3 新增状态中（§4.2）
3. 不在堆叠矩阵的任何规则类型中（§4.4）
4. 总共 21 个状态中不包含 `KNOCKBACK`

如果 `KNOCKBACK` 是瞬时位移效果（不作为持久状态存档），它不应出现在"不可净化集合"中——你无法净化一个瞬时效果。如果它是持久状态（如"击退后 1 回合内无法被再次击退"），则需要加入堆叠矩阵。

**建议**：

两条路径二选一：

1. 如果 `KNOCKBACK` 是瞬时位移：从不可净化集合中移除，改为在 `EffectOp.Displacement` 中处理
2. 如果 `KNOCKBACK` 是持久抗性状态：加入状态表和堆叠矩阵，说明持续时间和堆叠规则

---

### 1.3 [PR-03 / PR-05] `ResourceAxis` 枚举从未定义

**位置**：PR-03 §4.1.1 L170, §4.1.2 L199 / PR-05 §4.1 L110-111

**问题**：

`ResourceAxis` 被 `ResourceCost`（PR-03）、`ProfessionDef`（PR-05）、`EquilibriumResource`（PR-05）、`EffectOp.ResourceRestore`（PR-03）等多处引用，但在整个文档链中**从未列出枚举值**。

**建议**：

在 PR-05 §4.1（职业资源合同）中定义：

```kotlin
enum class ResourceAxis {
    HP,
    STAMINA,
    MANA,
    ENERGY,
    POSITIVE_ENERGY,
    HATE,
    EQUILIBRIUM,
}
```

PR-03 通过前置阅读引用即可。

---

### 1.4 [PR-03] `DisplacementType` 从未定义

**位置**：PR-03 §4.1.1 L175

**问题**：

`EffectOp.Displacement` 引用 `DisplacementType`，但详设文档（L1453）和 PR-03 都没有给出枚举值。实施者不知道有哪些位移类型。

**建议**：

在 PR-03 §4.1.1 的 `EffectOp` sealed class 旁补充：

```kotlin
enum class DisplacementType {
    PUSH,       // 远离施法者
    PULL,       // 拉向施法者
    DASH,       // 使用者自身位移
    TELEPORT,   // 瞬移到目标位置
}
```

---

### 1.5 [PR-03] `TalentRole` 枚举从未定义

**位置**：PR-03 §4.1.2 L204

**问题**：

`TalentAiHints.role` 引用 `TalentRole`，但枚举值从未列出。PR-04 的 AI DSL 需要这些值来理解天赋用途。

**建议**：

在 PR-03 §4.1.2 补充：

```kotlin
enum class TalentRole {
    OFFENSE,
    DEFENSE,
    HEAL,
    CONTROL,
    MOBILITY,
    UTILITY,
}
```

---

### 1.6 [PR-06] `RunSummary` 缺少 `headlessTurnEquivalent` 字段

**位置**：PR-06 §4.8 L346-358 / §4.9 L365-371

**问题**：

§4.9 详细定义了 `headlessTurnEquivalent` 的换算规则，§4.10 的 `longRunLab` 用它作为 `<= 3000` 的核心门禁指标。但 §4.8 的 `RunSummary` 结构体不包含此字段。

`longRunLab` 需要在运行后分析 `RunSummary` 来输出统计报告。如果 `headlessTurnEquivalent` 不在 `RunSummary` 中，lab 要么无法做 post-hoc 分析，要么需要在运行时独立维护一个平行计数器。

**建议**：

在 `RunSummary` 中增加字段：

```kotlin
data class RunSummary(
    // ... existing fields ...
    val headlessTurnEquivalent: Int,
)
```

---

### 1.7 [上游 / PR-01] 补充设计文档 Sigmoid 参数表与注释自相矛盾

**位置**：补充设计文档 §3.2.1 L418 / L433

**问题**：

补充设计文档的参数定义为 `k = 0.04, m = -10`，但存在两处自相矛盾：

1. **注释**（L418）声称"`d=0` 时命中率约 **85%**"
2. **示例表**（L433）显示 `d=0` → Phase 3 sigmoid = **0.74**

而用 `k = 0.04` 实际计算：

```
d = 0, m = -10
sigmoid(0.04 * (0 + 10)) = sigmoid(0.4) = 1 / (1 + e^(-0.4)) ≈ 0.5987
hitChance = 0.05 + 0.90 * 0.5987 ≈ 0.59
```

实际结果约 **59%**，既不是注释声称的 85%，也不是表格显示的 74%。

反算表格值 0.74 对应的 k 约为 **0.12**；注释值 85% 对应的 k 约为 **0.25**。

这不是 PR-01 的问题——PR-01 正确引用了公式和参数。但实施者如果去查补充设计文档的参考表做验证，会发现自己的计算结果和文档对不上。

**建议**：

在补充设计文档中修正：要么更新 k 值（如果 0.74/85% 是期望的设计目标），要么更新注释和表格（如果 k = 0.04 是最终参数）。PR-01 可在 §4.1 增加一条注释："参数以本节冻结的参数表为权威。"

---

## 2. P2 问题（可在实施过程中逐步解决）

### 2.1 [PR-01] `ApplicationPolicy.INSTANT_ACTION` 语义未解释

**位置**：PR-01 §4.5 L188

**问题**：5 种 `ApplicationPolicy` 中，`SELF_AUTO`（自动施加于自身）、`HOSTILE_HIT_THEN_SAVE`（先命中再豁免）、`HOSTILE_SAVE_ONLY`（只走豁免）、`TAG_AUTO`（标签自动触发）都能从名称推断语义，但 `INSTANT_ACTION` 的适用场景不明确——它和 `SELF_AUTO` 有什么区别？

**建议**：补一句适用描述，如"`INSTANT_ACTION`：绕过命中和豁免，立即生效；用于地形效果、环境伤害和 Boss phase 切换副作用"。

### 2.2 [PR-02] `STEALTH` + DoT 交互未显式确认

**位置**：PR-02 §4.5 L207 / §4.6 L221

**问题**：PR-02 说"STEALTH 仅在实际受伤时解除"，又说"BLEED / BURN / POISON 即使 STUN / FREEZE 下也照常 tick"。由此可推断：隐匿目标身上的 DoT tick 产生实际伤害，应当打破隐匿。但这一组合场景没有被显式确认。

**建议**：在 §4.5 补一条："`BLEED / BURN / POISON` 在 `STEALTH` 期间照常 tick；tick 产生的实际伤害会打破 `STEALTH`。"

### 2.3 [PR-03] `breakpoints` 字段未出现在字段映射表中

**位置**：PR-03 §4.1 L117-138

**问题**：§4.1 的 `TalentDef`-to-YAML 映射表有 30 个字段，但 `breakpoints` 没有对应行。§4.1.2 定义了 `TalentBreakpoint` 结构体，但实施者不知道 YAML 中怎么写。

**建议**：在映射表中增加一行：

| `breakpoints` | `talent.breakpoints` | 断点列表（按 `atRank` 排序） |

### 2.4 [PR-04] `STEALTH` 下 Boss phase 切换措辞歧义

**位置**：PR-04 §4.6 L259

**问题**：Rule 3 写"Boss 遇到 `STEALTH` 时不切换 phase"。字面理解可能是"Boss 在玩家隐匿期间冻结所有 phase 切换"。但 HP 触发的 phase 切换（来自 DoT 或环境伤害）应当仍然生效。实际意图应该是"Boss 不会因为玩家进入 STEALTH 而额外触发 phase 切换反应"。

**建议**：改为"Boss 遇到 `STEALTH` 目标时，不因此额外触发 phase 切换；HP 驱动的 phase 切换条件仍正常判定。在当前 phase 内执行无目标 fallback 或范围扫描行为。"

### 2.5 [PR-04] `BossEncounter.templateId` 与 `MonsterTemplateV2` 关系未显式关联

**位置**：PR-04 §4.2 L137

**问题**：`BossEncounter` 有 `templateId` 字段，YAML 示例填了 `"molten_giant_template"`。但没有说明这个 ID 引用的是什么。它是指 PR-06 §4.3 引用的 `MonsterTemplateV2`？还是一个独立的 boss template？

**建议**：在 §4.2 冻结口径中补一句："`templateId` 引用 `MonsterTemplateV2.id`，共享 stats / combat profile / resistance 等基础数据。"

### 2.6 [PR-06] `RunSummary` 字段命名：`winningClassId` / `winningRaceId`

**位置**：PR-06 §4.8 L349-350

**问题**：`winningClassId: String?` 和 `winningRaceId: String?` 暗示这两个字段仅在胜利时填充。但不论胜负，run 总有一个职业和种族。失败 run 也需要知道"用什么职业死的"，否则 `longRunLab` 无法做死亡分布统计。

**建议**：改为 `classId: String`（非空）和 `raceId: String`（非空），始终填充。`victory: Boolean` 已经表达了输赢。

### 2.7 [PR-06] `shard` 来源不含出售

**位置**：PR-06 §4.6 L276-277

**问题**：`shard` 来源列为"怪物掉落 / 支线奖励 / Boss 奖励"。但 §4.6 L283 明确说商店支持"出售多余掉落"。出售产出 `shard`，但 `shard` 来源列表遗漏了它。

**建议**：来源补充为"怪物掉落 / 支线奖励 / Boss 奖励 / 出售装备"。

### 2.8 [PR-06] `AffixDef` 结构体未定义

**位置**：PR-06 §4.7 L304-311

**问题**：`AffixGenerator`、`AffixPool`、`AffixTagWeighting`、`AffixBlacklist` 都有建议文件路径，但 `AffixDef` 本身的字段列表从未给出。实施者不知道一个 affix 需要哪些字段（`id / nameKey / tier / statModifiers / tags / slotType` 等）。

**建议**：在 §4.7 补充最小 `AffixDef` 结构：

```kotlin
data class AffixDef(
    val id: String,
    val nameKey: String,
    val slotType: AffixSlotType,    // PREFIX / SUFFIX
    val equipType: EquipType,       // WEAPON / ARMOR
    val tier: Int,
    val statModifiers: List<StatModifier>,
    val tags: Set<String>,
    val blacklistTags: Set<String>,
)
```

### 2.9 [PR-01] `accuracy` 是否有收益递减未说明

**位置**：PR-01 §4.6 L206-213

**问题**：DR 常量表列了 `evasion / critRating / castSpeed / hpRegen`。`accuracy` 也是二级属性，但不在表中。PR-01 L203-204 专门说了 `armorPenetration` 不进 DR，但 `accuracy` 没有类似声明。

**建议**：显式声明 `accuracy` 在 Phase 3 是否有 DR。如果有，给 C 值；如果没有，写一句 "`accuracy` 在 Phase 3 不进入收益递减，保持线性语义"。

### 2.10 [PR-03] `EffectOp` 缺少若干 Phase 3 可能需要的类型

**位置**：PR-03 §4.1.1 L150-184

**问题**：当前 `EffectOp` sealed class 有 6 个子类型：`Damage / Heal / ApplyStatus / ResourceRestore / Displacement / StatModifier`。但 Phase 3 状态系统包含 `SHIELD / GUARD / REGEN`（取较强值规则），这些状态的施加需要传入 `magnitude`。通过 `ApplyStatus` 可以间接处理（`statusId = "SHIELD", duration = N`），但 `magnitude`（护盾量/回复量）作为比较维度没有出入口。

此外，`SUSTAINED` 类天赋（category = SUSTAINED）的开关切换效果没有对应的 `EffectOp` 子类型。

**建议**：不需要立即扩充，但建议在 §4.1.1 补一条非目标声明："Phase 3 的 `EffectOp` 首批 6 类足以覆盖主线天赋；`GrantShield / SustainToggle / AreaPlace` 等复合类型允许在 `W3` 实施过程中按需追加，追加时必须回写本节。"

### 2.11 [PR-01] 参数表 `m` 值符号约定与补充设计文档不一致

**位置**：PR-01 §4.1 L99

**问题**：PR-01 参数表写 `中心偏移 m = 10`（公式形式为 `sigmoid(k * (d + m))`）。补充设计文档写 `m = -10`（公式形式为 `sigmoid(k * (d - m))`）。数学结果完全相同，但符号约定不同会让实施者困惑。

**建议**：在 PR-01 参数表的 `m` 行增加注释："本表 `m = 10` 对应补充设计文档的 `m = -10`，两者是同一公式的不同展开形式。"

### 2.12 [PR-04] Telegraph shape 未覆盖 `CROSS` 目标类型

**位置**：PR-04 §4.8 L301 / PR-03 §4.1 L110

**问题**：Targeting 枚举包含 `CROSS`，但 telegraph renderer 只支持 `CIRCLE / LINE / CONE` 三种形状。如果一个 `CROSS` 目标技能需要 telegraph 预览，用哪种形状？

**建议**：在 §4.8 补一句："`CROSS` 目标类型的 telegraph 预览可分解为两条正交 `LINE` 渲染。Phase 3 不要求独立 `CROSS` shape，实施时可复用 `LINE` 叠加。"

---

## 3. 验证通过项（R4 修订确认清零）

以下为 R4 提出的 P0-P2 项的修订验证结果：

| R4 编号 | 问题 | 修订状态 |
| --- | --- | --- |
| P0-1 | 命中公式 Sigmoid vs normalizedDelta | ✅ 详设文档已正式倒向 Sigmoid + `accuracy / evasion` |
| P0-2 | Telegraph schema 两套 | ✅ 详设文档已对齐 `TelegraphSpec` + `DangerLevel` |
| P0-3 | Targeting 枚举两套 | ✅ 详设文档已统一，`WALL / SUMMON_SLOT` 标为 Phase 4+ |
| P1-1 | `EffectOp` 类型空白 | ✅ PR-03 §4.1.1 已补最小 sealed class |
| P1-2 | `ActionCost` 未引用 | ✅ PR-03 §4.1 L113 已显式引用 |
| P1-3 | 回合 9 步 vs CombatPipeline 12 步 | ✅ PR-01 §4.4.1 已补嵌套关系 |
| P1-4 | `resourceCost` 单数改复数 | ✅ PR-03 §4.1.2 L191 已升级为 `List<ResourceCost>` |
| P1-5 | `DecayPolicy` 未引用 | ✅ PR-05 §4.4 L233-234 已显式引用 |
| P1-6 | `TalentAiHints` 缺失 | ✅ PR-03 §4.1.2 L203-207 已补结构 |
| P1-7 | Power/Save 属性来源缺失 | ✅ PR-01 §4.4 L155 已补引用 |
| P1-8 | `TalentBreakpoint` 结构未定义 | ✅ PR-03 §4.1.2 L209-213 已补结构 |
| P2-1 | 四段式 telegraph 预留 | ✅ PR-04 §4.5 L217, L227 已补 `stages` |
| P2-2 | `counterplayTags` 吸收 | ✅ PR-04 §4.5 L216, L226 已补 |
| P2-3 | `SoloContractDef` 结构化 | ✅ PR-05 §4.1 L124-131 已补 |
| P2-4 | `startingResources` 与 `resourceProfiles` 统一 | ✅ PR-05 §4.1 L119 已声明映射关系 |
| P2-5 | 回调控制流合同 | ✅ PR-01 §4.7 L233-237 已补 |
| P2-6 | 事件总线引用 | ✅ PR-02 §4.8 L248 已引用详设文档 §5.2 |
| P2-7 | `defense` vs `evasion` 属性声明 | ✅ PR-01 §4.1 L90-91 已显式声明 |
| P2-8 | 表现层 key 字段 | ✅ PR-03 §4.1 L124-126 已补 `iconKey / visualKey / audioProfile` |
| P2-9 | 怪物模板 V2 引用 | ✅ PR-06 §4.3 L214 已引用 `MonsterTemplateV2` |

R4 的 20 个修订项全部验证通过。

---

## 4. 整体评估

### 4.1 当前状态

| 维度 | R4 评估 | R5 评估 | 变化 |
| --- | --- | --- | --- |
| P0 问题 | 3 | **0** | 全部清零 |
| P1 问题 | 8 | **7** | R4 P1 已修，新发现 7 个 |
| P2 问题 | 9 | **12** | R4 P2 已修，新发现 12 个 |
| 设计完整度 | ~85% | **~92%** | +7pp |
| 可实施度 | ~75% | **~88%** | +13pp |

### 4.2 新发现 P1 的性质

本轮 7 个 P1 集中在一个模式：**引用了类型但没有定义枚举值**。修复方式统一且简单：

| P1 编号 | 未定义类型 | 引用位置 | 修复成本 |
| --- | --- | --- | --- |
| 1.1 | `OVERCHARGE` 消耗效果值 | PR-02 | 补 1 个数值 + 1 条管线挂点 |
| 1.2 | `KNOCKBACK` 存在性 | PR-02 | 二选一决策 + 补 1-3 行 |
| 1.3 | `ResourceAxis` | PR-05 | 补 1 个 7 值枚举 |
| 1.4 | `DisplacementType` | PR-03 | 补 1 个 4 值枚举 |
| 1.5 | `TalentRole` | PR-03 | 补 1 个 6 值枚举 |
| 1.6 | `headlessTurnEquivalent` | PR-06 | 补 1 个字段 |
| 1.7 | Sigmoid 参数表 vs 注释 | 上游 | 更新注释或参数 |

这 7 个 P1 的总修复工作量预估为 **2-3 小时**。

### 4.3 启动编码的前置条件

1. 补齐 §1 的 7 个 P1（~3 小时）
2. 验证补充设计文档的 Sigmoid 参数（k 值 vs 表格值），确认最终数值后回写 PR-01

完成后即可启动 W1 编码。P2 问题可在对应 PR 的实施过程中逐步修复。

---

## 5. 与 R4 审阅的关系

- R4 发现的问题已全部修复并验证（§3）
- R5 新发现的问题与 R4 无重叠
- R5 的 P1 主要来自"R4 修订时新增的结构体引用了未定义的类型"——这是修订过程中自然产生的边缘案例
- R5 的 P2 主要来自逐行审阅时发现的措辞歧义和缺失字段——这些在 R4 的宏观交叉审阅粒度下不可见
