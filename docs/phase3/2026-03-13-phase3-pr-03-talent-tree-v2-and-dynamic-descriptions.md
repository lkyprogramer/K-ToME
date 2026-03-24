> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
> `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-03 天赋树 V2、语义说明与 Allocation Draft

**阶段**: `Phase 3 / P3-W3`  
**优先级**: `P0`  
**前置条件**: `P3-W1` 完成；`W3` 的 schema 与说明建模可与 `P3-W2` 并行推进，但任何依赖状态词汇表、cleanse/status 语义的校验必须在 `P3-W2` 口径冻结后收口。  
**对应问题**: Phase 2 的天赋系统只有 V2 runtime 骨架和最小 `effect op` 子集，没有正式树结构、断点成长、语义说明和 draft-based 分配流程。若继续沿用“直接修改运行时状态”的点法，后续 `W5` 的正式职业树会非常脆弱。

---

## 1. 阶段目标

完成天赋树 V2 的完整基础设施，建立**语义说明模型**、断点成长 UX、`AllocationDraft`、关键词注册表与 telegraph 引用基线，并把 talent-local effect 从 `core.status` 的专用枚举依赖中抽离。

完成标准：

1. Talent schema 扩展支持 `rank / breakpoint / prerequisites / targeting / telegraphRef / typed effect op`。
2. 动态说明从 schema + 实际数值推导，禁止手写第二套文本逻辑。
3. `core` 只产出语义说明模型，不直接产出最终本地化字符串。
4. prerequisite 支持结构化 `talentId + minRank`。
5. 断点成长 UX 冻结（当前 rank 已激活效果 + 下一断点预览）。
6. Respec / rollback 改为 `TalentAllocationDraft` 驱动，preview 与 rollback 不再直接改动 live runtime。
7. 关键词注册表与 talent schema 联通。
8. `WAR_CRY` 这类只服务于 talent/content 的局部 buff/debuff 不再要求新增 `StatusEffectType`，改为由 schema + typed effect op 驱动。

## 2. 当前问题

1. Phase 2 的 `TalentDef` 只有 V2 runtime 骨架，缺少 rank / breakpoint / prerequisites 的完整字段。
2. 动态说明仍依赖 `name / description` 裸字符串，未迁移到 key + schema 推导。
3. 现有文档把动态说明 pipeline 定义成 `localized output`，会把最终本地化文本下沉到 `core`。
4. prerequisite 被弱化成“指定 talent 至少 1 点”，无法表达 `power_strike:2` 这类 rank 化前置。
5. 没有断点成长 UX，玩家无法预览下一断点效果。
6. `respec / rollback` 仍是对真实世界状态直接动手，后续状态、副作用和即时效果都会越来越难维护。
7. `talent.telegraph` 还没有和 `PR-04` 的统一 `TelegraphSpec` 权威对齐。
8. `WAR_CRY_BUFF / WAR_CRY_DEBUFF` 虽已改为通用唯一性规则，但仍作为专用 `StatusEffectType` 存在，导致 talent schema、职业内容和 `core.status` 的语义边界继续耦合。

### 2.1 本 PR 必须冻结的口径

1. Talent schema V2 的完整字段定义以结构化类型为准，而不是“弱字符串列表”。
2. 动态说明只从 schema 和实际数值推导，不允许 `descriptionOverride`。
3. `core` 只输出 `DescriptionModel` 等语义结构，不输出最终 localized string。
4. prerequisite 固定为 `talentId + minRank`。
5. `telegraph` 只保留引用式入口，不再在本 PR 定义第二套 telegraph 权威结构。
6. `respec / rollback` 都建立在 `TalentAllocationDraft` 之上。
7. talent-local sourced buff/debuff 若不依赖全局 tick / cleanse lock / mutual exclusion / combat callback 等状态引擎语义，则不得继续通过新增 `StatusEffectType` 建模；首个收口对象固定为 `WAR_CRY`。

## 3. 范围与非目标

### 3.1 范围

1. Talent schema V2 扩展（`rank / breakpoint / prerequisites / targeting / telegraphRef / typed effect op`）。
2. 语义说明 pipeline（schema -> semantic model -> presenter localized output）。
3. 断点成长 UX（当前效果 + 下一断点预览 + 灰态规则）。
4. `TalentAllocationDraft`。
5. Respec v1。
6. Rollback v1。
7. 关键词注册表。
8. talent-local effect 去类型化（`WAR_CRY` 首轮收口）。

### 3.2 非目标

1. 不在本 PR 创建完整的职业正式树数据（`P3-W5`）。
2. 不在本 PR 处理进阶职业或种族天赋的具体内容（`P3-W5`）。
3. 不在本 PR 处理 telegraph 渲染（`P3-W4 W4b`）。
4. 不在本 PR 把 `core` 做成本地化引擎或 tooltip presenter。
5. 不在本 PR 一次性迁移全部职业局部效果；只要求建立去类型化合同，并完成 `WAR_CRY` 首轮收口，剩余同类效果由 `P3-W5` 内容接线时按新合同接入。

## 4. 技术方案

### 4.1 Talent Schema V2

建议文件：

```text
core/src/main/kotlin/com/ktome/core/talent/TalentDef.kt
core/src/main/kotlin/com/ktome/core/talent/TalentNode.kt
core/src/main/kotlin/com/ktome/core/talent/EffectOp.kt
game/src/main/resources/data/talents/*.yaml
```

冻结口径：

1. 每个 `TalentDef` 必须包含：
   - `id`
   - `nameKey`
   - `descriptionTemplateKey`
   - `iconKey`（可选）
   - `visualKey`（可选）
   - `audioProfile`（可选）
   - `maxRank`
   - `tier`
   - `prerequisites`
   - `targetingDef`
   - `telegraphRef`（可选）
   - `effectOps`
    - `aiHints`
    - `actionCost`
    - `cooldown`
    - `resourceCosts`
2. rank 从 1 开始计数，每个 rank 对应一组 `effectOps`。
3. breakpoint 是特殊 rank，在该 rank 解锁新的 effect op 类别，而非仅数值增长。
4. prerequisite 固定为结构化类型：

```kotlin
data class TalentPrerequisite(
    val talentId: String,
    val minRank: Int,
)
```

5. `targeting` 必须使用 `TalentTargeting` 类型（`SELF / SINGLE_TARGET / LINE / CONE / RADIUS_SELF / RADIUS_TARGET / GROUND_TARGET / CROSS`）。
6. 旧命名 `ACTOR / TILE / RADIUS` 在 `Phase 3` 统一映射为 `SINGLE_TARGET / GROUND_TARGET / RADIUS_SELF|RADIUS_TARGET`；`WALL / SUMMON_SLOT` 只保留为 `Phase 4+` 预留概念，不进入本 PR runtime targeting 枚举。
7. `telegraphRef` 只引用 `PR-04` 定义的 `TelegraphSpec`，本 PR 不再定义第二份 telegraph schema。
8. `actionCost` 使用详设文档 §6.2 冻结的 `ActionCost` 枚举；YAML 的 `castTime` 只是该字段的输入别名。
9. `EffectOp.ApplyStatus.statusId` 固定引用 status schema id，而不是运行时 enum 名称。
10. 只有具备全局状态引擎语义的效果才允许占用 `StatusEffectType`；talent-local sourced effect 必须优先走 schema + `statusId`。

首轮字段映射参考：

| TalentDef 字段 | YAML 字段 | 说明 |
|------|------|------|
| `id` | `talent.id` | 唯一标识 |
| `nameKey` | `talent.nameKey` | 本地化 name key |
| `descriptionTemplateKey` | `talent.descKey` | 语义说明模板 key |
| `iconKey` | `talent.iconKey` | 可选 icon key |
| `visualKey` | `talent.visualKey` | 可选视觉资源 key |
| `audioProfile` | `talent.audioProfile` | 可选音频 profile |
| `maxRank` | `talent.maxPoints` | 最大可投入点数 |
| `tier` | `talent.tier` | 天赋阶级 |
| `category` | `talent.category` | `ACTIVE / PASSIVE / SUSTAINED` |
| `damageType` | `talent.damageType` | 默认伤害通道；`EffectOp.Damage` 可覆盖 |
| `resourceCosts` | `talent.resourceCosts` | 基础资源消耗列表 |
| `cooldown` | `talent.cooldown` | 基础冷却回合数 |
| `actionCost` | `talent.castTime` | `ActionCost` 的 YAML 输入别名 |
| `targetingDef` | `talent.targeting` | 目标选择定义 |
| `telegraphRef` | `talent.telegraphRef` | 统一 telegraph 引用（可选） |
| `effectOps` | `talent.levelEffects` | 每级效果（按 rank 分段） |
| `breakpoints` | `talent.breakpoints` | 断点列表（按 `atRank` 排序） |
| `aiHints` | `talent.aiHints` | AI 语义提示 |
| `prerequisites` | `talent.requirements.talentPrereqs` | 结构化前置 |
| `keywords` | `talent.keywords` | 关键词标签列表 |
| `callbacks` | `talent.callbacks` | 回调挂载 |

### 4.1.1 `EffectOp` 最小类型系统

冻结口径：

1. `typed effect op` 不能只停留在字段名，必须冻结最小 sealed class 集合。
2. `damageType` 是 talent 级默认通道；若某个 `EffectOp.Damage` 显式声明 `damageType`，以 op 级定义覆盖 talent 默认值。
3. `Vanguard` 的单体战技与 `Arcanist` 的投射法术必须都能由该最小集合表达。
4. 首批 6 类只覆盖 `Phase 3` 主线天赋；`GrantShield / SustainToggle / AreaPlace` 等复合类型允许在实施中按需追加，但追加时必须回写本节。
5. `WAR_CRY` 这类 talent-local sourced buff/debuff 必须能只靠 `EffectOp.ApplyStatus(statusId=...) + status schema` 表达，不再要求在 `core.status` 新增专用枚举成员。

建议最小结构：

```kotlin
sealed interface EffectOp {
    data class Damage(
        val damageType: DamageType? = null,
        val baseAmount: IntRange,
        val scaling: ScalingDef,
    ) : EffectOp

    data class Heal(
        val baseAmount: IntRange,
        val scaling: ScalingDef,
    ) : EffectOp

    data class ApplyStatus(
        val statusId: String,
        val duration: Int,
        val applicationPolicy: ApplicationPolicy,
    ) : EffectOp

    data class ResourceRestore(
        val axis: ResourceAxis,
        val amount: Int,
    ) : EffectOp

    data class Displacement(
        val type: DisplacementType,
        val distance: Int,
    ) : EffectOp

    data class StatModifier(
        val statId: String,
        val amount: Int,
        val duration: Int,
    ) : EffectOp
}

enum class DisplacementType {
    PUSH,
    PULL,
    DASH,
    TELEPORT,
}
```

### 4.1.2 `ResourceCost`、`TalentAiHints` 与 `TalentBreakpoint`

冻结口径：

1. `resourceCost` 统一升级为 `resourceCosts: List<ResourceCost>`，以支持双轴职业和未来多资源技能。
2. `TalentAiHints` 是 AI 理解 talent 用途的正式输入，不允许留给 `AIProfile` 侧猜测。
3. breakpoint 不是隐式布尔标记，必须有结构化对象表达“在哪一级、解锁什么、如何预览”。
4. `ResourceAxis` 统一复用 `PR-05 §4.1` 的职业资源轴枚举，`W3` 不再自造第二套资源轴常量。

建议最小结构：

```kotlin
data class ResourceCost(
    val axis: ResourceAxis,
    val amount: Int,
)

enum class TalentRole {
    OFFENSE,
    DEFENSE,
    HEAL,
    CONTROL,
    MOBILITY,
    UTILITY,
}

data class TalentAiHints(
    val role: TalentRole,
    val preferredRange: IntRange?,
    val isSustainToggle: Boolean = false,
)

data class TalentBreakpoint(
    val atRank: Int,
    val unlockedEffects: List<EffectOp>,
    val descriptionAddendumKey: String?,
)
```

### 4.2 语义说明 Pipeline

建议文件：

```text
core/src/main/kotlin/com/ktome/core/talent/DescriptionModel.kt
core/src/main/kotlin/com/ktome/core/talent/DynamicDescriptionResolver.kt
core/src/main/kotlin/com/ktome/core/talent/DescriptionContext.kt
client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt
core/src/test/kotlin/com/ktome/core/talent/DynamicDescriptionTest.kt
```

冻结口径：

1. `core` 的职责是从 schema 和当前数值计算出**语义模型**，而不是最终本地化文本。
2. 最终 localized output 和 tooltip 拼装放在 `client` 或 presenter 层。
3. 不允许 `descriptionOverride` 式硬编码。
4. 描述模板使用 key 引用（如 `talent.vanguard.power_strike.desc`），模板中用占位符引用实际数值（如 `{damage}`, `{duration}`）。
5. 数值来自当前 rank 的 `effectOps`。
6. 关键词使用 `[[keyword_id]]` 语法，由 `KeywordRegistry` 解析为关键词语义，而不是直接在 `core` 拼出最终文本。
7. placeholder 不得过早降成纯字符串；`core` 必须保留数值 / 布尔 / 文本等类型信息，交给 presenter 做 locale-aware 格式化。

建议最小结构：

```kotlin
sealed interface DescriptionValue {
    data class IntValue(val value: Int) : DescriptionValue
    data class DecimalValue(val value: Double) : DescriptionValue
    data class BooleanValue(val value: Boolean) : DescriptionValue
    data class TextValue(val value: String) : DescriptionValue
}

data class DescriptionModel(
    val templateKey: String,
    val placeholders: Map<String, DescriptionValue>,
    val keywords: List<String>,
)
```

Pipeline 流程：

```text
descriptionTemplateKey
        │
        ▼
1. Schema 解析
        │
        ▼
2. Keyword 语义解析
        │
        ▼
3. 数值占位符注入
        │
        ▼
4. 输出 DescriptionModel
        │
        ▼
5. client/presenter 本地化与 tooltip 拼装
```

### 4.3 断点成长 UX

建议文件：

```text
client/src/main/kotlin/com/ktome/client/ui/talent/TalentTreePanel.kt
client/src/main/kotlin/com/ktome/client/ui/talent/BreakpointPreview.kt
client/src/main/kotlin/com/ktome/client/ui/talent/TalentTooltip.kt
```

冻结口径：

1. 当前 rank 已激活效果必须清晰展示。
2. 下一断点会新增什么必须可预览。
3. 未达到的断点效果允许以灰态或次级文案预览，但不得伪装成已激活效果。
4. 满级 talent 必须有明确的“已满”视觉标记。

### 4.4 `TalentAllocationDraft`

建议文件：

```text
core/src/main/kotlin/com/ktome/core/talent/TalentAllocationDraft.kt
core/src/test/kotlin/com/ktome/core/talent/TalentAllocationDraftTest.kt
```

冻结口径：

1. 所有未确认的加点、洗点、回退都只操作 draft。
2. draft 只表达“计划中的 rank 分布”，不直接修改 live runtime。
3. confirm 之前不触发即时效果、不写状态、不调用 `on_learn / on_unlearn`。
4. confirm 时才把 draft 提交到运行时对象。

建议最小结构：

```kotlin
enum class TalentTreeOwnerType {
    PROFESSION,
    RACE,
}

data class TalentAllocationDraft(
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
    val pendingRanks: Map<String, Int>,
)
```

### 4.5 Respec v1

建议文件：

```text
core/src/main/kotlin/com/ktome/core/talent/RespecManager.kt
core/src/test/kotlin/com/ktome/core/talent/RespecManagerTest.kt
```

冻结口径：

1. 仅允许在非战斗状态触发。
2. 本阶段默认免费。
3. 清空当前职业树已投点数并全额返还。
4. Respec 不直接逐个 live unlearn，而是生成全新的 `TalentAllocationDraft`。
5. 用户确认后，系统根据 live tree 与 draft 的差异执行最终提交。
6. respec 必须写入事件日志。

### 4.6 Rollback v1

建议文件：

```text
core/src/main/kotlin/com/ktome/core/talent/RollbackManager.kt
core/src/test/kotlin/com/ktome/core/talent/RollbackTest.kt
```

冻结口径：

1. 仅对最近一次未确认分配生效。
2. 不支持跨多步历史撤销。
3. 确认分配后，rollback 不可用。
4. rollback 只回退 draft，不回退已提交到 live runtime 的内容。

### 4.7 关键词注册表

建议文件：

```text
core/src/main/kotlin/com/ktome/core/talent/KeywordRegistry.kt
core/src/main/kotlin/com/ktome/core/talent/KeywordSemantic.kt
core/src/test/kotlin/com/ktome/core/talent/KeywordRegistryTest.kt
```

冻结口径：

1. 关键词注册表统一维护术语 ID、语义类型、tooltip key。
2. `core` 返回关键词语义引用，不返回最终 tooltip 字符串。
3. 术语的最终展示语言和格式化由 presenter 层负责。

### 4.8 `telegraphRef` 与统一权威

冻结口径：

1. `TalentDef` 中只保留 `telegraphRef`。
2. `TelegraphSpec` 的 shape / preview / danger / defender baseline 一律由 `PR-04` 的统一结构权威维护。
3. 本 PR 不再维护能力级 telegraph 的第二套字段定义。

## 5. 推荐改动面

### 5.1 `core`

1. `talent` 包扩展（`TalentDef` V2 / `TalentNode` / `EffectOp` / `DescriptionModel` / `DynamicDescriptionResolver` / `TalentAllocationDraft` / `RespecManager` / `RollbackManager` / `KeywordRegistry`）。

### 5.2 `game`

1. `talents/*.yaml` 的前置条件、telegraph 引用、effect op 数据扩展。
2. `WAR_CRY` 的 talent/schema 接线改为引用 schema-driven `statusId`，不再依赖专用 `StatusEffectType.WAR_CRY_*`。

### 5.3 `client`

1. `DescriptionPresenter` 新建。
2. talent UI 断点成长预览扩展。

## 6. 测试与自证

### 6.1 必测类

1. `TalentSchemaV2Test`
2. `DynamicDescriptionTest`
3. `KeywordRegistryTest`
4. `TalentAllocationDraftTest`
5. `RespecManagerTest`
6. `RollbackTest`
7. `TalentPrerequisiteRankTest`
8. `TalentTooltipPresenterTest`

### 6.2 必测行为

1. prerequisite 能正确表达 `talentId + minRank`。
2. `EffectOp` 最小类型系统足以表达单体伤害、状态施加、回复、位移与属性修饰。
3. `ActionCost` 使用详设文档冻结的枚举，不直接在 YAML 中散落裸能量值。
4. `resourceCosts: List<ResourceCost>` 能表达单轴与双轴消耗。
5. `TalentAiHints` 能为 AI 提供 role / preferredRange / sustainToggle 语义。
6. `DescriptionModel` 只返回语义信息，不直接返回最终 localized output。
7. `DescriptionModel.placeholders` 保留类型信息，presenter 能基于 locale 做数值格式化、单位与强调。
8. 关键词能解析成语义引用并交给 presenter 层渲染。
9. 当前 rank 效果与下一断点预览区分清楚。
10. `TalentAllocationDraft` 以 `ownerType + treeOwnerId` 同时覆盖职业树和种族树，在 confirm 前不修改 live runtime。
11. respec 生成新的 draft，并在确认后一次性提交。
12. rollback 只回退最近一次未确认分配。
13. `telegraphRef` 能正确引用 `PR-04` 的统一 telegraph 规格。
14. `breakpoints` 能稳定映射到 YAML，并按 `atRank asc` 排序。
15. `WAR_CRY` 的职业局部 buff/debuff 通过 schema/`statusId` 和通用唯一性字段表达，而不是新增或继续保留专用 `StatusEffectType` 规则分支。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.talent.*"
./gradlew :client:test
./gradlew test
```

### 6.4 白盒验证

1. 用 `Rogue` 或 `Templar` 做一次未确认加点 -> rollback -> 再确认，确认 live 数值只在 confirm 后变化。
2. 打开一个带断点成长的 talent，确认当前效果与下一断点预览区分清楚。
3. 检查关键词 tooltip，确认术语没有在 `core` 被直接格式化成最终文本。

## 7. 出口门禁

1. Talent schema V2 的结构化字段全部进入运行时合同。
2. prerequisite 收口为 `talentId + minRank`。
3. 动态说明收口为语义模型输出，最终本地化不再下沉到 `core`。
4. `TalentAllocationDraft` 进入正式路径。
5. respec / rollback 的关键路径有自动化测试且不再直接改 live runtime。
6. `telegraphRef` 与 `PR-04` 的统一权威结构对齐。
7. `WAR_CRY` 完成首轮去类型化收口，不再依赖专用 `StatusEffectType` 作为 talent/content 的表达边界。
