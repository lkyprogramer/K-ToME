> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`

# Phase 4 - PR-04 LootBudget V2、RarityTier 与掉落入口重构

**阶段**: `Phase 4 / P4-B / P4-W3a`  
**优先级**: `P0`  
**前置条件**: `Phase 3` 出口全部满足  
**对应问题**: 当前 `core.item` 仍以 `COMMON / MAGIC / RARE + affixCount` 的旧模型生成物品，zone 和 source tier 也没有正式影响 `iLvl / qLvl / rarityScore`。如果不先做 contract 和过渡桥，`PR-05` 的 affix cost、unique/artifact 模板会直接压在旧掉落模型上。

---

## 1. 阶段目标

先完成 `Phase 4` 掉落预算词汇的冻结，并把当前掉落入口切到 `LootBudget` 驱动，为后续 affix cost、unique/artifact 和 `LootBalanceLab` 做桥接。

完成标准：

1. `LootRollContext / SourceTier / RarityTier / SpecialTierEligibility / LootBudget / ZoneRewardProfile` 进入正式词汇。
2. 当前掉落入口从“按 floor 直接选 quality”切到“先算 budget，再决定 rarity / qLvl”。
3. 旧 `ItemQuality` 保留为过渡表面，不再作为正式概率模型真源。
4. `MAGIC / RARE` 基础 rarity roll 先落地，`UNIQUE / ARTIFACT` 进入 eligibility + upgrade 流程，模板池留给 `PR-05`。
5. run-scoped pity contract 在本 PR 冻结，`PR-05` 负责结合模板池做 end-to-end 验证。

## 2. 当前问题

1. `ItemGenerator.chooseQuality()` 仍按 `floor` 直接给 `COMMON / MAGIC / RARE` 权重。
2. zone 侧虽然有 `lootProfiles`，但没有正式的 `ZoneRewardProfile` 参与预算运算。
3. `ItemQuality` 和 `Phase 4` 文档里的 `RarityTier` 不一致，若不先桥接会污染 save / snapshot / client。
4. 当前掉落没有任何 pity / rescue 机制，长局中存在长时间 0 件高质量装备的尾部体验风险。

### 2.1 本 PR 必须冻结的口径

1. `RarityTier` 取代 `ItemQuality` 成为规则层掉落数学模型真源。
2. `ItemQuality` 在过渡期只用于现有 save/client 兼容映射：
   - `COMMON -> NORMAL`
   - `MAGIC -> MAGIC`
   - `RARE -> RARE`
3. `UNIQUE / ARTIFACT` 不再进入基础 rarity roll，而是通过 `SpecialTierEligibility + upgrade roll` 进入候选流程；直到 `PR-05` 才允许正式产出模板物品。
4. `magicFindBonus` 只影响 rarity，不直接影响 `iLvl`。
5. `ZoneRewardProfile` 由 zone 内容稳定声明，不允许运行时随机摇点。
6. `iLvl` 在 `Phase 4` 只允许持平或向上波动，不允许因为负随机让高价值来源掉得比普通来源更差。
7. `magicFindBonus > 1.0` 的溢出部分没有任何额外效果；`1.50` 场景仅用于验证 clamp 边界。

## 3. 范围与非目标

### 3.1 范围

1. 新建 `core.loot` budget / rarity contract。
2. zone schema 增加 `ZoneRewardProfile` 数据入口。
3. 重构掉落入口，先算 `LootBudget` 再进入物品生成。
4. 保留旧 `ItemQuality` 到 save/snapshot/client 的兼容桥。

### 3.2 非目标

1. 不在本 PR 正式开放 `UNIQUE / ARTIFACT` 掉落内容池。
2. 不在本 PR 扩大量级 affix 数据。
3. 不在本 PR 引入 `lootBalanceLab` 的最终 root alias，`PR-05` 负责实验室收口。

## 4. 技术方案

### 4.1 新的掉落 contract

建议文件：

```text
core/src/main/kotlin/com/ktome/core/loot/LootBudgetModels.kt
core/src/main/kotlin/com/ktome/core/loot/LootBudgetResolver.kt
core/src/test/kotlin/com/ktome/core/loot/*
```

核心结构：

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

enum class RarityTier(
    val baseWeight: Int,
    val qualityBonus: Int,
    val baseBudget: Int,
) {
    NORMAL(720, 0, 0),
    MAGIC(220, 1, 6),
    RARE(50, 3, 14),
}

enum class SpecialTier {
    UNIQUE,
    ARTIFACT,
}

data class SpecialTierEligibility(
    val availableSpecialTiers: Set<SpecialTier>,
)
```

### 4.2 正式公式

本 PR 冻结公式，与主文档保持一致：

1. `iLvl = clamp(sourceLevel + sourceTier.itemLevelBonus + uniformInt(0, 1), 1, playerLevel + 3)`
2. `effectiveMagicFind = clamp(magicFindBonus, 0.0, 1.0)`
3. `rarityScore = sourceTier.rarityBonus + zoneRewardProfile.rarityBonus + effectiveMagicFind * 0.50`
4. `qLvl = clamp(iLvl + rarityTier.qualityBonus + zoneRewardProfile.qualityBonus, iLvl, iLvl + 6)`
5. `affixBudget = qLvl * 2 + rarityTier.baseBudget + sourceTier.affixBudgetBonus`

特殊 tier 主路径改为两段式：

1. 第一段基础 rarity roll 只参与：
   - `NORMAL`
   - `MAGIC`
   - `RARE`
2. 第二段只在 `SpecialTierEligibility` 非空时执行：
   - `UNIQUE` upgrade roll
   - `ARTIFACT` upgrade roll
3. 不可发放的 special tier 不再进入总权重，也不再走“命中后降级”主路径。
4. `PR-05` 只负责把这条 eligibility + template pool 路径做完整，不再反改本 PR 的数学入口。

基线概率速查：

| 上下文 | NORMAL | MAGIC | RARE | UNIQUE | ARTIFACT |
| --- | --- | --- | --- | --- | --- |
| `SourceTier.NORMAL + rarityScore=0.00` | `72.0%` | `22.0%` | `6.0%` | `upgrade-only` | `upgrade-only` |
| `SourceTier.BOSS + magicFind=0.50`（`rarityScore≈0.65`） | `~64.0%` | `~25.5%` | `~10.5%` | `upgrade-only` | `upgrade-only` |

这些概率表用于设计和 QA 直观评估；正式实现仍以加权归一化算法为准。`UNIQUE / ARTIFACT` 的真实概率在 `PR-05` 由 eligible source 的 upgrade roll 单独定义并记录版本号。

### 4.3 PityTracker 与长局保底

建议文件：

```text
core/src/main/kotlin/com/ktome/core/loot/PityTracker.kt
core/src/test/kotlin/com/ktome/core/loot/PityTrackerTest.kt
```

冻结口径：

```kotlin
data class PityTracker(
    val rollsSinceLastRare: Int = 0,
    val eligibleSpecialRollsSinceLastUnique: Int = 0,
)
```

规则：

1. 仅统计“会产出装备或装备模板候选”的正式 loot roll；纯 consumable 或脚本化固定奖励不计入 pity。
2. 连续 `20` 次未产出 `RARE+` 时，下次 `RARE` 权重乘以 `2.0`，直到真正结算出 `RARE+` 后重置。
3. 只有满足 `SpecialTierEligibility` 的 upgrade roll 才计入 special pity。
4. 连续 `50` 次 eligible special upgrade 未产出 `UNIQUE+` 时，下次 `UNIQUE` upgrade 权重乘以 `3.0`，直到真正结算出 `UNIQUE+` 后重置。
5. `ARTIFACT` 不设 pity；其受控发放主路径为 `BOSS / SECRET_ZONE / special reward chest`。
6. pity 为 run-scoped runtime 状态，允许进入 save/replay/session，但不得进入 `ProfileData`。
7. rare pity 和 special-tier pity 是两条独立规则：
   - rare pity 作用于基础 rarity roll
   - special-tier pity 作用于 eligible upgrade roll
8. 模板池缺失或来源不允许导致的 special tier 不发放，不重置 special-tier pity。

版本字段：

1. `lootFormulaVersion`
2. `specialTierEligibilityVersion`

两者都必须进入：

1. `lootBalanceLab` 报告头
2. run save / replay header
3. 相关 golden / batch 元数据

### 4.4 旧 `ItemQuality` 过渡桥

过渡策略：

1. `ItemGenerator` 内部先改为消费 `LootBudget`。
2. 产物上的旧 `ItemQuality` 仍由 `RarityTier` 映射生成，直到 `PR-05` 扩展 snapshot/save/client。
3. `UNIQUE / ARTIFACT` 在本 PR 不进入实际发放路径；只有当 `SpecialTierEligibility` 为真且 `PR-05` 模板池已接通时，才允许真正产出。

### 4.5 save / replay 最小状态

本 PR 需要把掉落侧最小持久化字段先冻结，避免 pity 和 special tier 语义在 `save / replay / tools` 各长一套。

最低要求：

1. `PityTracker`
2. `lootFormulaVersion`
3. `specialTierEligibilityVersion`
4. `activePackIds: List<PackId>`
5. `activePackManifestVersions: Map<PackId, String>`

约束：

1. `PityTracker` 是 run-scoped 状态，进入 save/replay/session，但不得进入 `ProfileData`。
2. `lootFormulaVersion` 与 `specialTierEligibilityVersion` 必须跟着 replay header 与 batch 报告一起输出，供 `PR-05` 和后续 `DeathAnalysis` 对账。
3. `activePackIds / activePackManifestVersions` 在 save/replay 与 batch report 中按全局 typed `PackId` 口径输出，不再单独定义 loot 层字符串版字段。
4. pack 环境不匹配时，不允许重放或读取旧 loot 状态后静默回退。

### 4.6 `magicFind` clamp 语义

说明：

1. `magicFindBonus > 1.0` 的场景只用于验证 clamp 边界条件。
2. `1.50` 与 `1.00` 的公式输入结果应一致到统计容差内。
3. 溢出的 `0.50` 不转化为 `iLvl`、`qLvl`、额外 pity 或任何隐藏加成。

### 4.7 zone 接线

zone schema 必须提供 sibling `ZoneRewardProfile`：

```kotlin
data class ZoneRewardProfile(
    val id: String,
    val zoneId: String,
    val rarityBonus: Float,
    val qualityBonus: Int,
    val baseRewardBudget: Int,
)
```

正式来源：

1. `greenwood_fringe`
2. `deep_iron_pit`
3. `underground_river`
4. `abyssal_temple`

旧 zone 未迁移时允许 fallback 到 `0.0 / 0`，但只作为兼容，不得再新增依赖。

## 5. 推荐改动面

### 5.1 `core`

1. 新建 `core.loot` package。
2. 把 `ItemGenerator.chooseQuality()` 替换为 `LootBudgetResolver`。
3. 为 `RarityTier -> ItemQuality` 兼容映射补单测。
4. 引入 `SpecialTierEligibility` 和 upgrade 预检查入口。

### 5.2 `game`

1. zone schema 增加 `ZoneRewardProfile`。
2. monster / boss / chest 掉落入口改为传入 `LootRollContext`。
3. 保持现有 `lootProfiles` registry，但让其消费基础 `RarityTier` 结果与 special tier eligibility，而不是直接决定 quality。

### 5.3 `tools / white-box` 补充改造

1. 本 PR 不直接交付 `whiteBoxLoot` root alias，但必须把 `PR-05` 会消费的 white-box 输入合同先冻结。
2. `LootRollContext / LootBudget / RarityTier / SpecialTierEligibility / PityTracker` 必须都能稳定映射到结构化 `JsonObject`，供后续 `WhiteBoxCaseReport.facts` 与 `WhiteBoxAggregateReport.metrics` 直接复用。
3. `HarnessReportHeader -> VerificationReportHeader` 的映射链必须保留：
   - `lootFormulaVersion`
   - `specialTierEligibilityVersion`
   - `activePackIds`
   - `activePackManifestVersions`
4. 任何新的 loot trace、golden 或 batch 输出都必须沿用 `seed + zoneId + floorIndex` 可 join 键，不能再发明只在 loot 域内部可读的局部 key。
5. 本 PR 结束时，`PR-05` 不应再需要从 runtime 对象临时反推 budget/rarity 语义；它应能直接消费这里冻结好的 typed DTO 与报告字段。

## 6. 测试与自证

### 6.1 必测行为

1. `magicFindBonus` 提高时高 rarity 权重单调不减。
2. `magicFindBonus > 1.0` 被 clamp 到 `1.0`。
3. `SourceTier` 不同会影响 `iLvl / rarityScore / affixBudget`。
4. `UNIQUE / ARTIFACT` 在本 PR 只通过 eligibility 预检查进入 trace，不会生成半成品。
5. pity 计数只在真正发出 `RARE+` / `UNIQUE+` 奖励时重置，不存在“命中后降级导致误清零”的主路径。

### 6.2 自动化命令

```bash
./gradlew :core:test
./gradlew lootBalanceLab
```

说明：

1. `lootBalanceLab` 的正式 root alias 和 batch 报告在 `PR-05` 落地。
2. 本 PR 至少要把 `PR-05` 将要消费的 `LootRollContext / LootBudget` 输入输出 contract 固定下来。

### 6.3 统一白盒框架预埋验证

1. `:core:test` 必须额外覆盖：
   - `LootBudgetResolver` 的确定性
   - `RarityTier -> ItemQuality` 兼容映射
   - `magicFind` clamp 边界
   - `PityTracker` 重置条件
2. 需补一组 contract 级测试，确保后续 `whiteBoxLoot` 可以直接消费：
   - `LootRollContext` 序列化 shape 稳定
   - `LootBudget` / `SpecialTierEligibility` 字段不漂移
   - `lootFormulaVersion / specialTierEligibilityVersion` 能进入统一报告头
3. 本 PR 虽不要求 `whiteBoxLoot` 任务落地，但必须让 `PR-05` 无需反改预算公式就能把这些字段接进统一 white-box 报告。

## 7. 出口门禁

1. `LootBudget`、`RarityTier`、`ZoneRewardProfile` 与 `SpecialTierEligibility` 口径冻结。
2. 当前掉落主路径已切到 budget 驱动。
3. 旧 `ItemQuality` 仍兼容 save/client，但不再作为规则层真源。
4. `PR-05` 可以直接追加 affix cost、unique/artifact 与实验室验证，而不需要再改预算公式。
5. `PR-05` 所需 white-box 输入合同已经在本 PR 冻结，不再允许后续为工具侧便利重命名或补第二套字段。
