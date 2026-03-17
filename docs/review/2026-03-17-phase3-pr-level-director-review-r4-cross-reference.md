# Phase 3 PR 级开发文档深度交叉审阅

**日期**：2026-03-17
**审阅视角**：资深游戏设计与开发总监
**审阅方法**：以下两份权威参考文档作为基线，对 6 份 PR 级开发文档进行逐条交叉校验

**参考基线**：

1. `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`（以下简称"详设文档"）
2. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（以下简称"补充设计文档"）

**审阅对象**：

1. `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
2. `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
3. `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
4. `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
5. `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
6. `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`

**审阅关系**：

本轮审阅与之前 R1-R3 轮次的关系为互补而非覆盖：

- R1-R3 聚焦于 PR 文档内部一致性与 PR 间的依赖完整性
- 本轮（R4）聚焦于 PR 文档与两份上游权威参考文档的**交叉对齐**
- 本轮也包含之前未被触及的结构性优化建议

---

## 0. 直接结论

**两份参考文档之间存在 6 处尚未被统一的硬冲突。** PR 文档全面倒向补充设计文档，但详设文档中仍有若干合同定义比 PR 文档更完整或更精确，尚未被 PR 文档吸收。

当前 6 份 PR 文档的设计完整度约 **85%**，可实施度约 **75%**。

核心风险不在"设计不够多"，而在两份上游权威文档本身的内部冲突还没有被正式消解。如果不在开始编码前先把上游对齐，后续会在 hit formula、telegraph schema、targeting 类型、attribute naming 和 talent schema 等至少 5 个维度产生"选了这边就得改那边"的返工。

最终结论收口为：**3 P0（均为上游权威间硬冲突） / 8 P1 / 9 P2**。

---

## 1. 两份上游权威文档之间的硬冲突

这是本轮审阅最关键的发现。PR 文档不可能同时满足两份互相冲突的上游文档；必须先在上游消解冲突，再回写 PR。

### 1.1 [P0] 命中公式：Sigmoid vs normalizedDelta

| 维度 | 详设文档 | 补充设计文档 | PR-01 采用 |
| --- | --- | --- | --- |
| 公式模型 | `normalizedDelta(d, 35)` | `sigmoid(0.04 * d)` | Sigmoid |
| 数学函数 | `d / (abs(d) + scale)` | `1 / (1 + e^(-k*d))` | Sigmoid |
| 属性名 | `accuracy - defense` | `accuracy - evasion` | `accuracy - evasion` |
| 中心值 | `0.55` | `0.50`（由 `m=10` 偏移后约 `0.54`） | ~`0.54` |
| 下限 | `0.10` | `0.05` | `0.05` |
| 上限 | `0.95` | `0.95` | `0.95` |

**位置**：

1. [详设文档 L509-L519](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L509)
2. [补充设计文档 L320-L340](../2026-03-13-core-systems-design-and-phase-supplements.md#L320)（约）
3. [PR-01 L87-L100](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L87)

**问题**：

这不是"换了个数学记法"，而是两种本质不同的映射曲线：

1. `normalizedDelta` 在 `|d| >> scale` 时线性逼近 ±1，曲线始终对称且仅在零附近有弯折。
2. `sigmoid` 在远端指数饱和，零附近则几乎线性。
3. 两者在 `d = ±50` 之后的行为明显分叉，直接影响高/低属性区间的命中感知。

**更关键的差异**：详设文档使用 `defense`（防御值），补充设计文档使用 `evasion`（闪避值）。详设文档 L504 明确写到"`Evasion` 在 Phase 2 迁移阶段等价并入 `defense`"。这意味着两份文档在**属性模型**层面就没有对齐。

**建议**：

1. 必须在两份上游文档中选定一种公式模型作为 Phase 3 正式口径。
2. PR-01 当前已选 Sigmoid——如果这是最终决定，那么详设文档的 §7 应被标记为"Phase 2 过渡设计，已被补充设计文档取代"。
3. `defense` vs `evasion` 的属性名必须统一。建议保留 `evasion` 作为命中系统的对抗属性（Sigmoid 的输入），`defense` 仅在需要时映射为 `evasion` 的别名。

### 1.2 [P0] Telegraph 语义完全不同

| 维度 | 详设文档 | 补充设计文档 + PR-04 |
| --- | --- | --- |
| 数据结构名 | `TelegraphDef` | `TelegraphSpec` |
| 危险级枚举 | `INFO / WARNING / DANGER` | `LOW / MODERATE / HIGH / LETHAL` |
| 关键字段 | `severity`, `indicatorKey`, `logKey`, `audioCue`, `counterplayTags` | `shape`, `radius/length/angle`, `previewTurns`, `dangerLevel`, `threatProfileId` |
| 四段式模型 | 有：`预警 -> 起手 -> 爆发 -> 残留` | 无，只有 `previewTurns` |
| AI hints | `counterplayTags` | 无 |

**位置**：

1. [详设文档 L1349-L1378](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L1349)
2. [PR-04 L208-L223](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L208)

**问题**：

两套 telegraph schema 不只是字段名不同，语义模型完全不同：

1. 详设文档的 telegraph 是**四段式生命周期**（预警/起手/爆发/残留），隐含持续多回合的完整状态机。
2. 补充设计文档和 PR-04 的 telegraph 是**预览+触发**的二段模型。
3. 详设文档的 `counterplayTags` 给了 AI 可编程的反制提示，PR-04 没有这个概念。

**建议**：

1. 如果 Phase 3 不做四段式，必须显式声明"详设文档 §11.4 的四段式 telegraph 推迟到 Phase 5"。
2. 即使推迟四段式，也应该把 `counterplayTags` 从详设文档吸收到 `TelegraphSpec` 中——它对 AI 调试和 Boss 设计极有价值。
3. 危险级枚举必须统一。建议采用 PR-04 的 `LOW/MODERATE/HIGH/LETHAL` 并回写详设文档。

### 1.3 [P0] Targeting 类型集合不一致

| 详设文档 targeting | PR-03 targeting |
| --- | --- |
| `SELF` | `SELF` |
| `ACTOR` | `SINGLE_TARGET` |
| `TILE` | `GROUND_TARGET` |
| `LINE` | `LINE` |
| `CONE` | `CONE` |
| `RADIUS` | `RADIUS_SELF`, `RADIUS_TARGET` |
| `WALL` | — |
| `SUMMON_SLOT` | — |
| — | `CROSS` |

**位置**：

1. [详设文档 L1326-L1345](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L1326)
2. [PR-03 L106](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L106)

**问题**：

1. PR-03 拆了 `RADIUS` 成 `RADIUS_SELF / RADIUS_TARGET`，但丢了 `WALL / SUMMON_SLOT`。
2. PR-03 新增了 `CROSS`，详设文档没有。
3. `ACTOR` vs `SINGLE_TARGET` 是命名差异还是语义差异不清楚。

**建议**：

1. 冻结一个统一的 targeting 枚举。
2. 如果 Phase 3 不需要 `WALL / SUMMON_SLOT`（合理，这些更像 Phase 4+ 的内容），应显式声明为"保留但不启用"。
3. 回写详设文档或补充设计文档，消除枚举漂移。

---

## 2. PR 文档对上游合同的遗漏

以下内容存在于至少一份上游权威文档中，但 PR 文档未吸收或仅部分吸收。

### 2.1 [P1] `TalentEffectOp` 类型系统在所有文档中都是空白

**位置**：

1. [详设文档 L1321](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L1321)
2. [PR-03 L76-L77](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L76)

**问题**：

PR-03 在 `TalentDef` 的 `effectOps` 字段上标注了 `typed effect op`，但从来没有定义这个类型系统的内容。详设文档也只提到 `List<TalentEffectOp>` 但不给出成员。这意味着天赋的实际效果——伤害、治疗、buff、状态施加、位移、召唤——在所有文档中都是未定义的。

**影响**：

实施者在写第一个天赋时就会立刻卡住：`effectOps` 应该包含哪些 sealed class 成员？

**建议**：

在 PR-03 中补充 `EffectOp` 的最小可用 sealed class：

```kotlin
sealed interface EffectOp {
    data class Damage(val type: DamageType, val baseAmount: IntRange, val scaling: ScalingDef) : EffectOp
    data class Heal(val baseAmount: IntRange, val scaling: ScalingDef) : EffectOp
    data class ApplyStatus(val statusId: String, val duration: Int, val applicationPolicy: ApplicationPolicy) : EffectOp
    data class ResourceRestore(val resourceAxis: ResourceAxis, val amount: Int) : EffectOp
    data class Displacement(val type: DisplacementType, val distance: Int) : EffectOp
    data class StatModifier(val stat: String, val amount: Int, val duration: Int) : EffectOp
}
```

不需要覆盖所有可能性，但至少要让 `Vanguard` 的 `Power Strike` 和 `Arcanist` 的 `Fireball` 能被表达。

### 2.2 [P1] `ActionCost` 枚举未被 PR-03 吸收

**位置**：

1. [详设文档 L327-L334](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L327)
2. [PR-03 L122](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L122)

**问题**：

详设文档冻结了 `ActionCost` 枚举（`FREE / FAST / STANDARD / SLOW / VERY_SLOW`，对应 0/750/1000/1250/1500 能量）。PR-03 的 `TalentDef` 有 `actionCost` 字段并映射到 `talent.castTime`，但没有引用这个枚举。这会导致：

1. YAML 中 `castTime` 填什么值？数字？枚举名？
2. 能量系统的 `1000` 阈值在 PR 文档中从未出现。

**建议**：

PR-03 应显式声明 `actionCost` 使用详设文档 §6.2 冻结的 `ActionCost` 枚举，并标注 `castTime` 是 YAML 层面的别名映射。

### 2.3 [P1] 回合时序的 9 步与 CombatPipeline 的 12 步没有对齐

**位置**：

1. [详设文档 L356-L396](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L356)（9 步回合时序）
2. [补充设计文档 L660-L732](../2026-03-13-core-systems-design-and-phase-supplements.md#L660)（12 步 CombatPipeline）
3. [PR-01 L207-L215](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L207)

**问题**：

详设文档定义了 `TurnStart -> ActionValidate -> ActionCommit -> Movement -> Hit -> Damage -> OnHit -> Death -> TurnEnd` 的 9 步回合框架。补充设计文档的 12 步 `CombatPipeline` 是嵌套在其中的 `Hit -> Damage -> OnHit -> Death` 区间的细化展开。但两者的嵌套关系从来没有被显式声明。

PR-01 只引用 12 步管线，不引用回合时序框架。这导致：

1. `TurnStart`（冷却 tick、资源回复、start-of-turn status）在 PR-01 里没有锚点。
2. `ActionValidate`（资源检查、控制状态检查）在 PR-01 和 PR-03 之间飘浮。
3. `TurnEnd`（end-of-turn status、event flush）只在 PR-02 间接触及。

**建议**：

在 PR-01 中增加一节"回合时序与 CombatPipeline 的嵌套关系"，明确 12 步管线嵌套在 9 步回合框架的 `HitPhase ~ DeathPhase` 区间内，并把 `TurnStart / ActionValidate / ActionCommit / TurnEnd` 的权威引用指向详设文档 §6.3。

### 2.4 [P1] `ResourceCost` 数据结构缺失

**位置**：

1. [详设文档 L1311](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L1311)
2. [PR-03 L94](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L94)

**问题**：

详设文档的 `TalentDef` 使用 `resourceCosts: List<ResourceCost>`（复数列表），意味着一个天赋可能消耗多种资源（例如 Spellblade 的 `MANA + EQUILIBRIUM` 双轴消耗）。PR-03 用的是 `resourceCost`（单数），只有一个值。

对于 Spellblade 这类双轴职业，单 `resourceCost` 不够用。

**建议**：

改成 `resourceCosts: List<ResourceCost>` 与详设文档对齐：

```kotlin
data class ResourceCost(
    val axis: ResourceAxis,
    val amount: Int,
)
```

### 2.5 [P1] `DecayPolicy` 未被 PR-05 吸收

**位置**：

1. [详设文档 L1037-L1044](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L1037)
2. [PR-05 L213-L220](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L213)

**问题**：

详设文档的 `ResourcePoolDef` 已包含 `decayPolicy: DecayPolicy?`。PR-05 讨论 Berserker 的 `HATE` 有 `DecayPerTurn` 行为，但没有引用这个已有的 `DecayPolicy` 合同。这意味着 `HATE` 的衰减行为会被实现为私有逻辑，而不是走通用资源池。

**建议**：

PR-05 应显式引用详设文档的 `DecayPolicy` 并定义 `HATE` 的衰减为 `DecayPolicy.PER_TURN(amount = N)`，而不是新造一个私有衰减机制。

### 2.6 [P1] `TalentAiHints` 完全缺失

**位置**：

1. [详设文档 L1315](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L1315)
2. PR-03（不存在此字段）

**问题**：

详设文档的 `TalentDef` 包含 `aiHints: TalentAiHints`，这是 AI 理解天赋用途的关键数据（例如"这是一个近战输出技能"、"这是一个治疗技能"）。PR-04 的 AI DSL 需要评估候选动作，但没有任何结构来理解天赋的类别和用途。

**建议**：

在 PR-03 的 `TalentDef` 中增加 `aiHints` 字段，至少包含：

```kotlin
data class TalentAiHints(
    val role: TalentRole,           // OFFENSE / DEFENSE / HEAL / CONTROL / MOBILITY / UTILITY
    val preferredRange: IntRange?,
    val isSustainToggle: Boolean,
)
```

### 2.7 [P1] Power/Save 属性来源公式缺失

**位置**：

1. [补充设计文档 L878-L893](../2026-03-13-core-systems-design-and-phase-supplements.md#L878)
2. [详设文档 L486-L499](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L486)
3. PR-01（只引用公式结果，不引用属性来源）

**问题**：

两份参考文档都定义了 Power/Save 属性从 `STR/DEX/CON/WIL` 的派生公式，但版本不一致：

| 属性 | 详设文档 | 补充设计文档 |
| --- | --- | --- |
| `Physical Power` | `baseMeleePower + STR*2 + DEX/2` | `10 + STR*1.5 + level*0.5` |
| `Spell Power` | `baseSpellPower + WIL*2` | `10 + WIL*1.0 + DEX*0.5 + level*0.5` |

PR-01 没有选择任何一版。这意味着 `Power/Save` 的 Sigmoid 公式在 PR-01 虽然冻结了，但**输入值从哪来**没有冻结。

**建议**：

PR-01 应在 `§4.4 Power/Save` 中增加一条引用，明确 Power/Save 属性值的来源公式以补充设计文档 §3.4.2 为准，并在 golden test 中固定属性组合。

### 2.8 [P1] `TalentBreakpoint` 数据结构未定义

**位置**：

1. [详设文档 L1322](../2026-03-13-phase2-to-phase5-detailed-systems-design.md#L1322)
2. [PR-03 L96-L97](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L96)

**问题**：

详设文档定义了 `breakpoints: List<TalentBreakpoint>`，但没有给出 `TalentBreakpoint` 的结构。PR-03 说"breakpoint 是特殊 rank，在该 rank 解锁新的 effect op 类别"，但也没有给出数据结构。

实施者需要知道：breakpoint 是一个布尔标记（某些 rank 是 breakpoint），还是一个独立数据结构？

**建议**：

在 PR-03 §4.1 中补充最小结构：

```kotlin
data class TalentBreakpoint(
    val atRank: Int,
    val unlockedEffects: List<EffectOp>,
    val descriptionAddendum: String,    // 用于断点预览的描述 key
)
```

---

## 3. 详设文档中的优质设计未被 PR 吸收

以下内容不是冲突，而是详设文档中比 PR 文档更完整的设计，值得吸收。

### 3.1 [P2] 四段式 telegraph 模型

详设文档 L1374-1378 定义了 Boss 大技能的四段式：`预警 -> 起手 -> 爆发 -> 残留`。

PR-04 只有 `previewTurns`（预警时长），没有区分起手、爆发、残留。这对 Phase 3 来说可能够用，但 Phase 5 一定需要。

**建议**：在 PR-04 的 `TelegraphSpec` 中预留 `stages` 字段为空列表，并在非目标中说明"四段式留给 Phase 5"，避免后续重构整个 telegraph 管线。

### 3.2 [P2] `counterplayTags`

详设文档的 `TelegraphDef` 包含 `counterplayTags: List<String>`，表示"这个 telegraph 可以被什么类型的反制行为应对"（如 `DODGE / INTERRUPT / BLOCK`）。

这个字段对 AI 调试非常有价值：在 `bossHarness` 中可以验证"每个 telegraph 至少存在 1 种可用反制"。

**建议**：在 `TelegraphSpec` 中吸收此字段。

### 3.3 [P2] `ProfessionDef` 的 `soloContract` 合同检查

详设文档 L1464 定义了 `SoloContractDef`：

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

PR-05 只在文字层面说"每个职业必须有 panic answer、位移方案和 boss answer"，但没有结构化的检查字段。

**建议**：在 `ProfessionDef` 中增加 `soloContract: SoloContractDef`，并在 `SoloClearLab` 中增加自动化 lint 检查"每个 tag 列表非空"。

### 3.4 [P2] `startingResources` 已经支持多资源

详设文档 L1454 的 `ProfessionDef` 已经使用 `startingResources: List<ResourcePoolDef>`，天然支持多资源职业。PR-05 新引入了 `resourceProfiles: List<ResourceProfileRef>` 和 `ResourceAxis` 概念。

这两套 schema 虽然方向一致，但命名和结构不同。实施者会困惑到底用哪个。

**建议**：统一为一套。建议保留 PR-05 的 `ResourceAxis` 语义（更清晰），但让它映射到详设文档已有的 `ResourcePoolDef` 数据结构，避免出现两套并行的资源定义。

### 3.5 [P2] 回调优先级与管线控制流

补充设计文档 L738-L739 明确了管线回调的控制流：

1. `CallbackPriority` 数值越小越先执行。
2. 同优先级按 `entityId asc` 排序。
3. `CANCEL` 中止当前步骤后续回调但不跳过管线。
4. `ABSORB` 完全终止管线。

PR-01 提到"callback 优先级"但没有冻结这些控制流语义。

**建议**：PR-01 §4.7 应补充引用补充设计文档的回调控制流合同。

### 3.6 [P2] 事件总线的 `turnId / actionId / sequence / phase / cause` 结构

详设文档 L258-L276 定义了统一事件结构，包含 `turnId / actionId / sequence / phase / cause`。PR 文档中没有任何一份引用这个事件结构。

PR-02 的 `StatusEvent`、PR-04 的 `AIDecisionTrace`、PR-06 的 `RunSummary` 都需要和事件总线对接，但事件总线本身没有出现在任何 PR 的技术方案中。

**建议**：在 PR-01 或 PR-02 中增加一条引用："事件总线结构以详设文档 §5.2 为权威"。

### 3.7 [P2] `defense` vs `evasion` 的属性合并策略

详设文档 L504 明确写到："`Evasion` 在 Phase 2 迁移阶段等价并入 `defense`"。

PR-01 使用 `evasion` 作为 Sigmoid 命中公式的输入，但没有说明 `evasion` 是独立属性还是 `defense` 的别名。

**建议**：PR-01 应显式声明 Phase 3 是否沿用 `evasion` 独立属性（推荐，与 Sigmoid 配合更清晰），还是把它映射为 `defense` 的别名。如果独立，则回写详设文档标注"Phase 3 起 `evasion` 恢复为独立属性"。

### 3.8 [P2] `visualKey / audioProfile / iconKey` 的 schema 对齐

详设文档在 `TalentDef`、`ProfessionDef`、`MonsterTemplate` 中都包含 `visualKey / audioProfile / iconKey`。PR-03 的 `TalentDef` 只有 `nameKey / descriptionTemplateKey`，没有表现层 key。

这在 Phase 3 不一定阻塞（战斗公式和规则层不需要它们），但 `W5c` 的 client 工作一定会需要。

**建议**：在 PR-03 §4.1 的字段表中增加 `iconKey / visualKey / audioProfile` 为可选字段，即使 Phase 3 不填充所有值。

### 3.9 [P2] 怪物模板 V2 schema

详设文档 L193 提到 `MonsterTemplate V2` 应升级包含职业化/行为化/表现化字段。PR-06 讨论了普通怪 ~40、精英 ~12、Boss 3-4 的内容预算，但没有引用怪物模板 V2 的结构。

**建议**：PR-06 的 zone 规格表应增加怪物模板 V2 的引用，至少要求每个 zone 的怪物池引用同一套 `MonsterTemplateV2` schema。

---

## 4. 最大胆重构建议

以下是我认为"如果做了会大幅提升 Phase 3 质量，但不做也不会阻塞"的结构性优化。

### 4.1 把"回合框架"与"结算管线"合并为统一 Pipeline

当前有两套管线：

1. 详设文档的 9 步回合框架（覆盖从 `TurnStart` 到 `TurnEnd` 的全生命周期）
2. 补充设计文档的 12 步 `CombatPipeline`（覆盖从 `HitCheck` 到 `Loot` 的战斗结算）

建议合并为一个 **分层管线**：

```text
TurnPipeline (9 steps, from detailed-systems-design §6.3)
  └── CombatPipeline (12 steps, nested at HitPhase~DeathPhase)
```

顶层管线管回合流转，嵌套管线管战斗结算。所有 PR 文档都只引用这一个分层管线，而不是各自引用不同粒度的管线片段。

### 4.2 引入 `SystemContract` 注解约定

当前 PR 文档中频繁出现"冻结口径"段落，但在代码层面没有对应的标记。建议引入一个轻量注解约定：

```kotlin
@SystemContract(
    owner = "PR-01",
    frozenAt = "P3-W1",
    dependants = ["PR-02", "PR-03"]
)
object HitFormula { ... }
```

这不是要引入重量级框架，而是让代码层面的合同冻结与文档层面的冻结口径对应起来，减少"文档说冻结了但代码还在改"的漂移。

### 4.3 把 `EffectOp` 作为统一效果语言

当前天赋效果、状态效果、铭文效果、affix 效果分散在不同 PR 的不同 schema 中。如果把 `EffectOp` 作为统一效果表达语言：

```text
TalentDef.effectOps: List<EffectOp>
StatusEffectDef.tickEffect: EffectOp?
InscriptionDef.useEffect: EffectOp
AffixDef.equippedEffect: EffectOp
```

这样 `CombatPipeline` 只需要一个 `EffectOpResolver`，而不是每种效果来源各自实现一套结算逻辑。

### 4.4 在 zone 中引入 "encounter budget" 替代纯 monster count

PR-06 当前用"普通怪 ~40, 精英 ~12, Boss 3-4"的数量预算。但数量不等于体验密度。建议改为 **encounter budget**：

```kotlin
data class ZoneEncounterBudget(
    val totalEncounterPoints: Int,    // e.g., 100
    val minEliteEncounters: Int,      // e.g., 2
    val maxConsecutiveTrivial: Int,   // e.g., 3 (no more than 3 trivial encounters in a row)
)
```

每种怪有一个 encounter point 权重，zone 在生成时按预算分配，而不是硬编码数量。这对 `longRunLab` 的平衡调优更有杠杆。

---

## 5. 最小改进建议

以下是不需要重构，只需要修改几行文字或补一个字段的小优化。

### 5.1 PR-01: 补充收益递减对 `armorPenetration` 的说明

PR-01 §4.6 的收益递减常量表只列了 `evasion / critRating / castSpeed / hpRegen`，但 `armorPenetration` 也应该有递减吗？当前没有说明，实施者会犹豫。

**建议**：显式说明 `armorPenetration` 在 Phase 3 不做递减，或者给一个 C 值。

### 5.2 PR-02: `ARMOR_BREAK` 的跨来源上限语义需要示例

PR-02 说 `ARMOR_BREAK` 全局 3 层上限。但"全局"是指单个攻击者还是所有攻击者累计？

**建议**：补一句"无论来源数量，同一目标身上的 `ARMOR_BREAK` 总层数上限为 3"。

### 5.3 PR-03: `TalentDef` 缺少 `damageType` 如何映射到伤害通道

PR-03 §4.1 字段表有 `damageType`，但没有说明它和 `CombatPipeline` 的关系。当一个天赋的 `damageType` 是 `FIRE` 时，是整个天赋的所有 `effectOps` 都走火焰通道，还是每个 `EffectOp` 单独声明？

**建议**：明确 `damageType` 是默认通道，`EffectOp.Damage` 可以覆盖。

### 5.4 PR-04: `AICondition` 缺少 `NOT` 组合器

PR-04 §4.1 的 `AICondition` 使用结构化类型（`TARGET_DISTANCE_LESS_THAN / HP_BELOW / HAS_STATUS`），但没有组合器。如果 AI 需要"目标没有 STEALTH"，需要 `NOT(HAS_STATUS("STEALTH"))`。

**建议**：增加 `NOT / AND / OR` 三个逻辑组合器，否则 Boss YAML 会被迫为每种否定条件新建专用类型。

### 5.5 PR-05: 种族天赋树的投点与 `TalentAllocationDraft` 的关系

PR-05 说"种族天赋点独立于职业天赋点"，PR-03 定义了 `TalentAllocationDraft`。但 `TalentAllocationDraft` 只有 `professionId`，没有 `raceId`。

**建议**：`TalentAllocationDraft` 应改为 `treeOwnerId` 或增加 `raceId` 字段。

### 5.6 PR-06: `WorldGraph` 的连接关系缺少方向性

PR-06 的 ASCII 拓扑图用 `->` 表示连接方向，但没有说明连接是否双向。玩家能否从 `deep_iron_pit` 回到 `greenwood_fringe`？

**建议**：显式声明"所有连接双向可通行"或"zone 间连接为有向图，回退路径需要显式声明"。

### 5.7 PR-06: `shardReward` 缺少等级范围感知

`RouteReward` 的 `shardReward` 是固定值，但 zone 等级范围从 Lv1 到 Lv15。如果 `shardReward` 不随等级调整，早期 zone 的奖励可能远低于或远高于同等级期望。

**建议**：`shardReward` 应基于 zone 的 `levelBand` 做最小参考值映射，或者在 `AffordableRescueSlotPolicy` 中显式关联 zone level band。

### 5.8 PR-02: `TAUNT` 的多源覆盖缺少 trace 记录

PR-02 说 `TAUNT` 后来者覆盖。但 `CombatResolutionTrace` 中没有专门记录"嘲讽源切换"的 step。对于 Boss 战调试，这个信息很重要。

**建议**：在 PR-02 §4.4 的堆叠矩阵中补一句"TAUNT 覆盖时必须写入 `StatusEvent.TAUNT_OVERRIDE`"。

### 5.9 PR-01: `P2toP3FormulaComparisonTest` 应记录变化原因

PR-01 §4.9 的 `P2toP3FormulaComparisonTest` 只检查数值在阈值内，但不记录"为什么变了"。

**建议**：每个超出 ±5% 的场景应在测试报告中自动输出变化原因标签（如 `CAUSE: SIGMOID_UPGRADE / CAUSE: CRIT_RESISTANCE_ADDED`），帮助平衡设计师快速定位。

---

## 6. 修订优先级排序

### 6.1 必须在编码前解决（P0）

| # | 问题 | 修复位置 | 修复方式 |
| --- | --- | --- | --- |
| 1 | 命中公式两套模型未统一 | 详设文档 §7.2 | 标注"Phase 2 过渡设计，Phase 3 起以补充设计文档 §3.2 的 Sigmoid 为准" |
| 2 | Telegraph schema 两套未统一 | 详设文档 §11.4 + PR-04 | 在详设文档标注四段式推迟到 Phase 5，PR-04 吸收 `counterplayTags` |
| 3 | Targeting 枚举两套未统一 | 详设文档 §11.3 + PR-03 | 冻结 PR-03 版本，在详设文档标注新增/废弃 |

### 6.2 应在 W1-W3 启动前解决（P1）

| # | 问题 | 修复位置 |
| --- | --- | --- |
| 1 | `EffectOp` 类型系统空白 | PR-03 §4.1 |
| 2 | `ActionCost` 枚举未引用 | PR-03 §4.1 |
| 3 | 回合时序 vs CombatPipeline 嵌套关系 | PR-01 新增 §4.x |
| 4 | `ResourceCost` 单数改复数 | PR-03 §4.1 |
| 5 | `DecayPolicy` 未引用 | PR-05 §4.4 |
| 6 | `TalentAiHints` 缺失 | PR-03 §4.1 |
| 7 | Power/Save 属性来源公式缺失 | PR-01 §4.4 |
| 8 | `TalentBreakpoint` 结构未定义 | PR-03 §4.3 |

### 6.3 可以在实施过程中逐步解决（P2）

| # | 问题 | 修复位置 |
| --- | --- | --- |
| 1 | 四段式 telegraph 预留 | PR-04 |
| 2 | `counterplayTags` 吸收 | PR-04 |
| 3 | `SoloContractDef` 结构化 | PR-05 |
| 4 | `startingResources` 与 `resourceProfiles` 统一 | PR-05 |
| 5 | 回调控制流合同 | PR-01 |
| 6 | 事件总线引用 | PR-01 或 PR-02 |
| 7 | `defense` vs `evasion` 属性声明 | PR-01 |
| 8 | 表现层 key 字段 | PR-03 |
| 9 | 怪物模板 V2 引用 | PR-06 |

---

## 7. 最终判断

上一轮 R3-final 判断"0 P0, 6 P1, 7 P2"是基于 PR 文档之间的内部一致性。本轮交叉审阅揭示了一个 R1-R3 没有覆盖的维度：**两份上游权威文档本身的内部冲突**。

这 3 个 P0 不是 PR 文档的问题——PR 文档已经选边站了（全面倒向补充设计文档）。问题在于详设文档没有被同步更新来反映这些选择，导致"两份权威文档说的不一样"的局面仍然存在。

如果有人在实施中去翻详设文档（这是合理的，因为它也被标记为权威），他们会看到一套不同的公式、不同的 telegraph schema、不同的 targeting 枚举。这不是理论风险——实施者几乎一定会遇到。

**修复路径**：

1. 先花半天时间在详设文档的相关章节加上"Phase 3 起以补充设计文档 §X 为准"的同步标记。
2. 然后花半天在 PR-03 补 `EffectOp` 最小 sealed class 和 `ActionCost` 引用。
3. 之后就可以正式启动 W1 编码。

这两步加起来大约 1 天工作量，但能避免后续至少 3-5 天的"选了这边就得改那边"返工。
