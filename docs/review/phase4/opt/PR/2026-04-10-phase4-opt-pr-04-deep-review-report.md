# Phase 4 OPT PR-04 深度审查报告

- 审查日期: `2026-04-10`
- 审查对象: `codex/phase4-opt-pr-04-loot-profile-v3`（本地工作树，尚未 commit）
- 基准 Spec: `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-04-loot-profile-v3-and-reward-pool-differentiation.md`
- 审查者角色: 高级 Roguelike/ToME 游戏制作总监 + 系统策划总监 + gameplay review lead
- 当前 HEAD: `85939105 Merge pull request #64 from ... pr03-equipment-passive-density`（PR-04 的全部改动位于 worktree，与 HEAD 的 diff 即本 PR 范围）

---

## 0. 总体裁决（Executive Verdict）

| 总结 | 结果 |
| --- | --- |
| 本 PR 的核心目标（`LootProfileSchemaV3` 单 schema 收口 + tag-weighted 候选池 + 奖励池差异化） | **基本达成** |
| Spec §8 出口门禁（distinctBaseItemCount ≥ 35、averageOverlap < 30%、V3 迁移通过校验、overlay 支持 V3、V2 fail-fast） | **全部 PASS** |
| Spec §4.1.1「破坏式迁移、不保留双 schema」 | **完全满足**（代码已无 `LootProfileSchemaV2 / LegacyLootProfileAdapter / 双 parser / 双 overlay 分支`） |
| Spec §4.4.1 结构化 fail-fast（packId / targetProfileId / actualSchemaVersion / expectedSchemaVersion） | **完全满足** |
| 设计质量（奖励池主观差异感、maxOverlap、mid-late zone 差异化） | **部分达标**：`averageOverlap=0.198` 大幅下降，但 `maxOverlap=1.0` 仍存在 12 组“一方完全子集”的 profile 配对，zone identity 在纸面上没完全到位 |
| 代码卫生（残留 PR-01 阈值文案、`specialTemplateTagPreference` 语义落地） | **有待修** |

**综合判断**：**通过（Pass with design-quality follow-ups）**。

- 所有 **硬门禁**（数值门禁、结构化错误、破坏式迁移、overlay 白名单、fail-fast 诊断、测试覆盖）都已满足 Spec 要求，PR 可以进入合入流程。
- 但存在 **1 个 Major 级设计质量缺口**（`maxOverlap=1.0` 与若干“完全子集”的 profile 组合）和 **2 个 Medium 级代码卫生问题**。建议在合入前或紧随其后的 follow-up PR 中处理。

---

## 1. 范围确认与 Spec 对齐概览

### 1.1 Spec 完成标准一览（§1 + §8）

| # | Spec 要求 | 类型 |
| --- | --- | --- |
| 1 | `lootProfileBaseItemOverlapMatrix` 平均重叠 `< 30%` | 数值门禁 |
| 2 | `lootProfileDistinctBaseItemCount >= 35` | 数值门禁 |
| 3 | `21` 个正式 loot profile 全部迁移到 `schemaVersion = 3` | 结构性门禁 |
| 4 | runtime 只认 V3，V2 fail-fast | 结构性门禁 |
| 5 | `contentPackHarness` 覆盖 V3 overlay + V2 fail-fast | 结构性门禁 |
| 6 | `lootBalanceLab` 整体分布仍在 checklist 容差内 | 数值门禁 |

### 1.2 出口门禁实测结果

数据来源：`tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json` 与 `tools/build/reports/phase4/phase4-summary.json`。

| 门禁 | 目标 | 实测 | 状态 |
| --- | --- | --- | --- |
| `lootProfileAverageBaseItemOverlap` | `< 0.30` | `0.1976` | **PASS** |
| `lootProfileMaxBaseItemOverlap` | Spec 无硬约束 | `1.0`（12 对 profile 完全子集） | **⚠ 设计质量缺口** |
| `lootProfileDistinctBaseItemCount` | `>= 35` | `53` | **PASS** |
| `whiteBoxLoot` 整体 verdict | PASS | PASS（40/40 assertions） | **PASS** |
| `affixCount / uniqueTemplateCount / artifactTemplateCount` | PR-03 范围 | `78 / 20 / 8` | **PASS**（非本 PR 范围） |
| `lootBalanceLab` 整体 | 容差内 | `verdict=PASS, failedExpectationCount=0`（见 `loot-balance-summary.json`） | **PASS** |

**数值解读**：

- `averageOverlap` 从 PR-01 基线（0.23～0.30 区间）继续收窄到 **0.1976**，下降幅度明显，数据层面达成“奖励池差异化”这一宏观目标。
- `distinctBaseItemCount = 53` 远超 `35` 底线，说明 tag-weighted pool 的横向展开、`itemIds + itemTagFilter` 合并后 **确实把候选池从“少数核心件”扩散到了 53 个独立 base**。
- **但** `maxOverlap = 1.0` 代表存在某对 profile 中一方的候选池 **完全是** 另一方的子集（Jaccard 下会更低，因为用的是 `|A∩B| / min(|A|,|B|)` 这条非对称定义）。见第 3.1 节。

---

## 2. 分项 Spec 一致性核查

### 2.1 `LootProfileSchemaV3` 结构（Spec §4.1）

文件：`game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt`

Spec 要求保留的字段：`itemIds`、`rewardBudget`。  
Spec 要求新增的字段：`poolStrategy`、`itemTagFilter`、`excludeIds`、`typeWeights`、`slotBias`、`specialTemplateTagPreference`、`affixTagPreference`。  
Spec 明确 **不纳入** 的字段：`categoryWeights`、`zoneExclusiveIds`、`rewardBudgetToDropCount`。

实现：

```kotlin
data class LootProfileSchemaV3(
    val id: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val itemIds: List<String>,
    val rewardBudget: Int,
    val poolStrategy: LootPoolStrategy,
    val itemTagFilter: List<String> = emptyList(),
    val excludeIds: List<String> = emptyList(),
    val typeWeights: Map<ItemType, Int> = emptyMap(),
    val slotBias: Map<EquipSlot, Int> = emptyMap(),
    val specialTemplateTagPreference: List<String> = emptyList(),
    val affixTagPreference: List<String> = emptyList(),
)
```

- 7 个新字段 **全部存在**；`LootPoolStrategy` 枚举收敛到 `FIXED_LIST / TAG_WEIGHTED`。
- 明确被拒的 `categoryWeights / zoneExclusiveIds / rewardBudgetToDropCount` **全部未出现**（grep 结果为空）。
- `init` 校验约束：`itemIds` 与 `excludeIds` 去重、`typeWeights / slotBias` 非负、`rewardBudget > 0`、`specialTemplateTagPreference / affixTagPreference` 非空串集合 —— 落地到位。

**结论**：**完全符合 Spec §4.1**。

---

### 2.2 破坏式迁移与“不留双 schema”（Spec §4.1.1 + §4.3.1）

Spec 明令：不保留 `LegacyLootProfileAdapter`、双 parser、双 white-box summary、双 overlay merge 分支；V2 数据 fail-fast；所有官方 data、sample pack、fixture pack 必须在同一 PR 内完成迁移。

**代码验证（grep 全仓）**：

| 关键字 | 命中数 |
| --- | --- |
| `LegacyLootProfileAdapter` | 0 |
| `LootProfileSchemaV2`（作为类型使用） | 0 |
| `schemaVersion == 2`（loot profile 上下文） | 0 |
| `LOOT_PROFILE_SCHEMA_VERSION = 3` in `DataLoader.kt` | 1（唯一真源） |

**数据验证**：

- 正式数据 `game/src/main/resources/data/loot/index.yaml`：21 个 profile，**全部** `schemaVersion: 3`。
- Sample pack `examples/content-packs/sample.flooded_relics/data/loot/*.yaml`：已迁移为 `schemaVersion: 3 + poolStrategy: TAG_WEIGHTED`。
- Fixture pack `tools/src/main/resources/fixtures/content-packs/packs/fixture.append_loot_pool/data/loot/loot.foundation.common.append.yaml`：已迁移为 V3。
- 新增固件 `fixture.legacy_v2_loot_profile`（`tools/src/main/resources/fixtures/content-packs/packs/fixture.legacy_v2_loot_profile/`）：**只为测试 V2 被拒路径** 而存在，manifest 和 loot yaml 都保留 `schemaVersion: 2` 作为反例样本，这正符合 Spec §4.4.1 对「fail-fast 诊断需要真实素材」的要求。

**结论**：**完全符合 Spec §4.1.1 / §4.3.1 破坏式迁移约束**。没有任何“先双跑再清理”的中间态残留。

---

### 2.3 候选池算法（Spec §4.2）

Spec 目标流程：
1. 先按 `itemTagFilter` 构建 base pool
2. 排除 `excludeIds`
3. 按 `typeWeights` 做 `ItemType` 分桶偏置
4. 按 `slotBias` 做 `EquipSlot` 二次偏置
5. 其余交给现有 rarity / pity / special template 流水线

实现：`game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt:56-93`

```kotlin
fun resolve(profile: LootProfileSchemaV3): LootProfileCandidatePool {
    val explicitBaseIds = resolveExplicitIds(profile.itemIds, ...)
    val allCandidateBaseIds = linkedSetOf<String>().apply {
        addAll(explicitBaseIds)
        if (profile.poolStrategy == LootPoolStrategy.TAG_WEIGHTED) {
            addAll(resolveTagMatchedIds(profile.itemTagFilter))
        }
    }
    val excludedIds = resolveExplicitIds(profile.excludeIds, ...)
    allCandidateBaseIds.removeAll(excludedIds)
    ...
    val specialLinkedBaseIds = ...
    val standardCandidateBaseIds = allCandidateBaseIds - specialLinkedBaseIds
    require(standardCandidateBaseIds.isNotEmpty()) { ... }
    return LootProfileCandidatePool(
        ...,
        typeWeights = profile.typeWeights,
        slotBias = profile.slotBias,
        routeBiasTags = (profile.specialTemplateTagPreference + profile.affixTagPreference)...,
    )
}
```

权重计算：

```kotlin
fun weightFor(base: ItemBaseDef): Int {
    val typeWeight = typeWeights[base.type] ?: 1
    val slotWeight = base.slot?.let { slot -> slotBias[slot] ?: 1 } ?: 1
    return (typeWeight * slotWeight).coerceAtLeast(1)
}
```

- 步骤 1 & 2：`itemIds ∪ (tag 匹配结果 if TAG_WEIGHTED) − excludeIds` —— 贴 Spec。
- 步骤 3 & 4：`weightFor = typeWeight × slotWeight`，相乘后下限为 1 —— 符合“typeWeights 先、slotBias 再二次偏置”的语义。
- 步骤 5：special tier / rarity 仍由 `preferredSpecialTierEligibility + ItemGenerator.rollAndGenerate` 链路负责（见 `FoundationGameSession.kt:10584-10618`），**没有** 建立“第二套 zone-exclusive special template 真源”，符合 Spec §2.1 第 4 条约束。

另有一个硬 guard：`standardCandidateBaseIds.isNotEmpty()` 的 `require`，保证无论用户怎么配，纯 special-linked 的 profile 会在 DataLoader 层就炸出来 —— 这个 guard Spec 没有明令要求，但是一个合理的 defensive check，**加分**。

**结论**：**符合 Spec §4.2**，仅在 `specialTemplateTagPreference` 的语义落地上有偏差，见 3.2 节。

---

### 2.4 迁移策略与 21 个 profile（Spec §4.3）

`game/src/main/resources/data/loot/index.yaml`（共 21 条）：

| 类别 | 数量 | `FIXED_LIST` | `TAG_WEIGHTED` |
| --- | --- | --- | --- |
| `loot.foundation.{common/elite/boss}` | 3 | 3 | 0 |
| `*.cadence` | 10 | 6 | 4 |
| `*.reward` | 4 | 2 | 2 |
| `*.secret` | 4 | 0 | 4 |
| **合计** | **21** | **11** | **10** |

- 21/21 profile 已经 `schemaVersion: 3` —— 合 Spec §4.3 第 1 条。
- `loot.foundation.*` 保持 `FIXED_LIST`（Spec §4.3 第 2 条允许）。
- `*.secret` 全部走 `TAG_WEIGHTED` 并带 `specialTemplateTagPreference + affixTagPreference` —— Spec §4.3 第 4 条的核心要求。
- 然而：
  - `loot.shattered_outpost.cadence / loot.bandit_camp.cadence / loot.elven_ruins.cadence / loot.molten_core.cadence / loot.grey_gate_depths.cadence / loot.crystal_cavern.cadence / loot.grey_gate_depths.reward / loot.abyssal_heart.reward` 共 **8 个非 foundation profile 仍为 `FIXED_LIST`**。
  - 这 8 个里，`crystal_cavern / grey_gate_depths / molten_core` 都属于 Spec §4.3 第 4 条明确点名的“中后期 zone”。允许走 `FIXED_LIST` 会让这些 zone 的奖励池 **无法享受 `itemTagFilter` 的扩池红利**，这是本 PR 能继续收窄 `maxOverlap` 的最大杠杆点（详见 3.1 节）。

**结论**：**Spec §4.3 第 1/2/5 条合规**；第 4 条“中后期 zone、reward、secret 走 TAG_WEIGHTED”在 `*.secret` 路径合规，**在中后期 `cadence / reward` 路径仅达成 50%**。这属于 Spec 的“推荐”口径（使用了“若”、“仍可”之类的弱约束词），**不计作 Spec 违反**，但属于设计质量欠账。

---

### 2.5 DataLoader 与 runtime 的 V3 单 schema 收口（Spec §4.1.1 + §4.4）

文件：`game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`

- `LOOT_PROFILE_SCHEMA_VERSION = 3` 是唯一真源（DataLoader.kt:217 前后）。
- `parseLootProfileSchemas(..., expectedSchemaVersion = 3)` 会在 parser 层就拒绝非 3。
- Content-pack overlay 路径（DataLoader.kt:558-583）在 `"loot_profile"` 分支中：
  1. 先 `parseLootProfileSchemas(root, expectedSchemaVersion = null)` 以便读到原始 schemaVersion；
  2. 再显式比对 `entry.schemaVersion != LOOT_PROFILE_SCHEMA_VERSION`；
  3. 若不匹配则抛出带结构化 detail 的 `packLoadException`，code 为 `content-pack.loot-profile.schema-version-mismatch`。

```kotlin
details = mapOf(
    "packId" to pack.id.value,
    "targetProfileId" to entry.id,
    "actualSchemaVersion" to entry.schemaVersion.toString(),
    "expectedSchemaVersion" to LOOT_PROFILE_SCHEMA_VERSION.toString(),
),
```

- 四个 Spec §4.4.1 强制要求的字段（`packId / targetProfileId / actualSchemaVersion / expectedSchemaVersion`）**全部落地**。
- runtime 侧没有任何 `if (schemaVersion == 2) ...` 分支（grep 验证无命中）。
- 测试覆盖：
  - `game/src/test/kotlin/com/ktome/game/contentpack/DataLoaderContentPackTest.kt:121-128` 验证 V2 overlay 被 `content-pack.loot-profile.schema-version-mismatch` 结构化错误拒绝。
  - `game/src/test/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolverTest.kt:147-186` 用 `fixture.append_loot_profile_bias` 验证 V3 overlay 的 `APPEND` 到 `itemTagFilter` 字段。
  - `tools/src/main/kotlin/com/ktome/tools/contentpack/ContentPackHarnessRunner.kt:454-461` 新增 `legacy_v2_loot_profile_rejected` 场景，`phase4Report` 可读到拒绝原因。

**结论**：**完全符合 Spec §4.1.1 / §4.4 / §4.4.1**。

---

### 2.6 `ContentPackRuntimeResolver` overlay 白名单（Spec §4.4.1 特别约束）

Spec 要求扩展到 6 个字段：`itemTagFilter`、`excludeIds`、`typeWeights`、`slotBias`、`specialTemplateTagPreference`、`affixTagPreference`。

`game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt:675-686 / 742-755` 均已把上述 6 个字段加入 `loot_profile` overlay 的可 `APPEND / REPLACE` 白名单；对应的 merge policy（`APPEND_LIST / MERGE_MAP / REPLACE_VALUE`）也与字段语义匹配：

- 列表字段（`itemTagFilter / excludeIds / specialTemplateTagPreference / affixTagPreference`）：`APPEND_LIST` 并允许 `dedupeKey: tag`。
- Map 字段（`typeWeights / slotBias`）：`MERGE_MAP`（entry-wise）或 `REPLACE_VALUE`（整体替换）。

`ContentPackRuntimeResolverTest.kt:86-145` 同时验证「白名单之外的 field 被拒绝 (`content-pack.overlay.append-target-forbidden`)」，形成闭环。

**结论**：**完全符合 Spec §4.4.1 特别约束**。

---

### 2.7 测试与 whitebox/lootbalance/phase4Report 集成（Spec §6）

| Spec §6 必测行为 | 覆盖位置 | 状态 |
| --- | --- | --- |
| `schemaVersion = 2` 的 loot profile 被明确拒绝 | `DataLoaderContentPackTest.kt:121-128`、`ContentPackHarnessRunner` 的 `legacy_v2_loot_profile_rejected` 场景 | **PASS** |
| tag-weighted 候选池不为空 | `LootProfileCandidatePoolResolver.resolve` 的 `require(allCandidateBaseIds.isNotEmpty())` 作为 defensive 守卫 + `whiteBoxLoot` 的 `lootProfileDistinctBaseItemCount=53` 实测 | **PASS** |
| `lootProfileBaseItemOverlapMatrix` 明显下降 | `phase4-summary.json` 的 `averageOverlap=0.198`（远低于 0.30 阈值） | **PASS** |
| rarity / pity 分布不出现不可解释漂移 | `lootBalanceLab` 全部 assertion PASS，`failedExpectationCount=0` | **PASS** |
| pack overlay 不裁剪 V3 字段 | `ContentPackRuntimeResolverTest.kt` 的 `fixture.append_loot_profile_bias` + `DataLoaderContentPackTest.kt:28-54` 的 round-trip | **PASS** |

**结论**：**Spec §6 必测行为全部落地**。

---

## 3. 问题清单与严重性评估

> 严重性定义：
> - **Major**：直接违反 Spec 显式要求 / 破坏出口门禁 / 明显损害玩法目标。
> - **Medium**：不违反 Spec 硬约束，但存在代码卫生、语义落地或设计质量缺口，需要在短期内修复。
> - **Minor**：未来工作 / 微优化。

### 3.1 【Major，设计质量缺口】`maxOverlap = 1.0` 与 12 对“完全子集”奖励池

**事实**：  
`whitebox-loot-summary.json` 中 `lootProfileMaxBaseItemOverlap = 1.0`。overlap 定义为 `|A ∩ B| / min(|A|, |B|)`（来自 `Phase4ReportRunner.kt:648` 的 `note`），意味着存在 profile A 的 base pool **完全被** profile B 包含（或反之）的配对。

**实测对照表（从 `whitebox-loot-summary.json` 的 `lootProfileBaseItemOverlapMatrix` 提取）**：

| Profile A | Profile B | 语义解释 |
| --- | --- | --- |
| `loot.grey_gate_depths.reward` | `loot.foundation.boss` | 灰门深处 reward 4 件全是 boss 候选的子集 |
| `loot.bandit_camp.cadence` | `loot.foundation.common` | 盗贼营地 cadence 4 件全是 common 候选的子集 |
| `loot.greenwood_fringe.reward` | `loot.greenwood_hidden_cache.secret` | reward 与 secret 候选池一模一样 |
| `loot.abyssal_temple.cadence` ↔ `loot.abyssal_temple_warded_archive.secret` | 双向 1.0（cadence 完全 = secret） |
| `loot.grey_gate_depths.reward` ↔ `loot.abyssal_temple.cadence` | 双向 1.0 |
| `loot.grey_gate_depths.reward` ↔ `loot.abyssal_temple_warded_archive.secret` | 双向 1.0 |
| ...（合计 12 组 1.0 配对） | | |

**为什么 Spec 没把 `maxOverlap` 当硬门禁**：  
Spec §8 只列了 `averageOverlap < 30%`，原因是写 Spec 时接受“平均值已经足够描述差异化”。但这导致一个漏洞：只要多数 profile 差异大，即使少数 profile **彼此完全相同**，`averageOverlap` 还是能被摊薄。53 个 distinct base 的“广度 OK” + 12 对 1.0 的“尖峰 OK”就同时成立了。

**为什么这是 Major 级设计缺口而不是 Spec 违反**：  
- 数值门禁按字面通过，**不阻挡合入**。
- 但“奖励池差异化”是 Spec §1 第一句话的 **玩法目标**，`maxOverlap=1.0` 直接对应“某些 zone 的奖励完全不可区分”，这是玩家手感层面的缺陷：
  - `grey_gate_depths.reward ⊂ foundation.boss`：玩家打灰门深处 boss 的奖励池和随便一个 boss 完全同分布，**zone identity 为 0**。
  - `abyssal_temple.cadence == abyssal_temple_warded_archive.secret`：深渊神殿的普通 cadence reward 和隐藏 cache secret reward **候选池完全相同**，secret 的“值得探索”感被抹平。
  - `greenwood_fringe.reward == greenwood_hidden_cache.secret`：同上。
- `.secret` 路径已经走了 `TAG_WEIGHTED + specialTemplateTagPreference + affixTagPreference`，但因为 `specialTemplateTagPreference` 实际只作用于 **affix / base item scoring**（见 3.2 节），从 base item 候选池维度上看，`.secret` 并不比对应的 `.cadence` 多出任何物品。

**修复建议（推荐优先级从高到低）**：

1. **让剩余的中后期 `cadence / reward` profile 也走 `TAG_WEIGHTED`**（Spec §4.3 第 4 条推荐口径）：`grey_gate_depths.reward / grey_gate_depths.cadence / abyssal_temple.cadence / crystal_cavern.cadence / molten_core.cadence / elven_ruins.cadence` 都应补 `itemTagFilter + typeWeights + slotBias`。这会让同 zone 的 cadence 与 secret 在候选池上自动拉开（因为 tag 扩池对候选池的增量在 cadence 与 secret 之间不会完全一致）。
2. **给 `.secret` profile 配 `excludeIds`**：每个 `.secret` 的 `excludeIds` 列上同 zone `.cadence` 的 `itemIds`，强行剥离交集。这不需要新增 tag，是 Spec §4.2 第 2 步的“原生能力”的利用，现在 21/21 profile 的 `excludeIds` **全部为空**，这是个零成本的杠杆点。
3. **`.reward` 与 `foundation.*` 的子集关系**：`grey_gate_depths.reward` 这类纯粹为了“后期 boss 掉落”的 profile，如果硬要保持 FIXED_LIST，也应该 `excludeIds: [...从 foundation.boss 的前 N 个 base 排除...]` 或者干脆 4 件里至少换 2 件成 zone 专属 flavor。
4. **在 `phase4Report` / `whiteBoxLoot` 增加 `maxOverlap` 软门禁（例如 < 0.75 or `assertWarn`）**：让将来再加 profile 时 CI 能先报警。这不需要改 Spec，属于 OPT 类 follow-up。

**改哪里（具体 file:line）**：

- `game/src/main/resources/data/loot/index.yaml`：
  - `loot.grey_gate_depths.reward`（115-121 行）补 `poolStrategy: TAG_WEIGHTED` + `itemTagFilter: [grey_gate]` + `excludeIds: [seal_reliquary]`（脱离 foundation.boss 子集）。
  - `loot.abyssal_temple.cadence` / `loot.abyssal_temple_warded_archive.secret`：secret 的 `excludeIds` 加上 cadence 的 itemIds。
  - `loot.greenwood_fringe.reward` / `loot.greenwood_hidden_cache.secret`：同理。
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt:636-649`：在 `lootProfileBaseItemOverlapMatrix` 指标下补一个 `maxOverlap` 的 secondary metric 或 warning，不上硬门禁。

---

### 3.2 【Medium，语义落地偏差】`specialTemplateTagPreference` 未按字面落在 special template 选择上

**事实**：

Spec §4.1 新增字段第 6 个是 `specialTemplateTagPreference`，从字段名看，语义是“对 special template 的偏好”。实现侧：

1. `LootProfileCandidatePoolResolver.kt:91` 把 `specialTemplateTagPreference` 与 `affixTagPreference` **合并** 到一个共享的 `routeBiasTags` 集合。
2. `FoundationGameSession.kt:10582` 里这个 `routeBiasTags` 只做两件事：
   - `affixContext = affixContext.copy(routeBiasTags = ... + pool.routeBiasTags)` —— 进入 **affix 选择** 的 scoring 路径。
   - `FoundationGameSession.kt:4133` 的 `routeBiasScore = baseTags.count(rewardContext.routeBiasTags::contains) * 6` —— 进入 **base item 选择** 的 scoring 路径。
3. 真正传给 `preferredSpecialTierEligibility(preferredTemplateIds = ...)` 的是 `pool.preferredSpecialTemplateIds`（`FoundationGameSession.kt:10612`）。而 `LootProfileCandidatePoolResolver.kt:86-88` 里，`preferredSpecialTemplateIds` 的来源是 **显式 `itemIds` 中能映射到 `specialTemplateForItemId`** 的那部分，**与 `specialTemplateTagPreference` 完全无关**。

**结果**：Spec 设计中“`specialTemplateTagPreference` 偏置 special template 路径 / `affixTagPreference` 偏置 affix 路径”的二元分工，在实现里 **坍缩成同一个 `routeBiasTags` 集合**；而真正用于 special template 偏好的反而是另一个间接派生字段。

**为什么 Spec 没明令“字段 X 必须作用于 Y 路径”**：  
Spec §4.1 只列出 7 个新字段，没有对每个字段的 runtime 入口做规范。所以 **严格讲这不是 Spec 违反**，但：

1. 字段名与实际行为不一致，未来维护者会误读。
2. 如果今后需要“secret profile 更偏好地扔某个 tag 的 special template”，会发现这个字段根本没通路径去拉 special template。
3. 当前 `routeBiasTags` 的使用已经同时覆盖 affix 和 base item，再把 `specialTemplateTagPreference` 塞进去其实是“重复通道”：`affixTagPreference` 一个字段就够了。

**修复建议**：

- **方案 A（最小改动）**：把字段名改为 `affixTagPreferenceSecondary` 或干脆合并到 `affixTagPreference`，让名称对齐行为。后向兼容性在本 PR 阶段不是问题（破坏式迁移时机）。
- **方案 B（推荐）**：保留两个字段语义独立：
  - `specialTemplateTagPreference` → 在 `LootProfileCandidatePoolResolver.resolve` 里用来“扩充 `preferredSpecialTemplateIds`”：遍历 `itemBundle.specialTemplates`，把 `template.tags ∩ specialTemplateTagPreference` 非空的 template id 也加入 `preferredSpecialTemplateIds`。
  - `affixTagPreference` → 保留现有 `routeBiasTags` 流。
  - 这需要 `ItemDataBundle` 提供 `specialTemplatesByTag(...)` 或 `specialTemplates` 的只读视图。

**改哪里（具体 file:line）**：

- `game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt:86-92`：拆分 `routeBiasTags` 的来源（只放 `affixTagPreference`）；`preferredSpecialTemplateIds` 的构造增加“通过 tag 拉 template”这一支。
- 根据 `core/item/ItemDataBundle` 是否已暴露 template 遍历，可能需要补一个查询 API。

---

### 3.3 【Medium，代码卫生】`WhiteBoxLootRunner` 的 overlap 阈值 / 文案仍停留在 PR-01

**事实**：

`tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt:194-199`：

```kotlin
WhiteBoxAssertionResult(
    ruleId = "loot.aggregate.overlap_below_threshold",
    passed = kernelRun.profileOverlapSummary.averageOverlap < 0.50,
    message = "Average loot-profile base item overlap stays below the OPT PR-01 baseline threshold.",
    context = kernelRun.profileOverlapSummary.toJson(),
),
```

- 阈值仍是 `0.50`，文案仍是 "OPT PR-01 baseline threshold"。
- Spec PR-04 §8 的硬门禁是 `< 0.30`。

**为什么这没有爆出来**：当前值是 `0.198`，对 `0.50` 和 `0.30` 都通过。双检查是冗余但不致命。

**为什么我依然认为需要改**：

1. 这里是 **持续 sanity 门禁**，不是 Phase4Report 的出口门禁。如果有人 revert 掉 `.secret` 的 `TAG_WEIGHTED` 让 `averageOverlap` 漂到 0.35，**`whiteBoxLoot` 会继续 PASS**，只有 `phase4Report` 会 FAIL。持续集成的 early-warning 失效。
2. 文案里还留着 "OPT PR-01 baseline threshold"，对阅读代码的人是误导 —— 实际当前 PR 已经是 PR-04，阈值应当贴 PR-04 的语义。

**修复建议**：

```kotlin
WhiteBoxAssertionResult(
    ruleId = "loot.aggregate.overlap_below_threshold",
    passed = kernelRun.profileOverlapSummary.averageOverlap < 0.30,
    message = "Average loot-profile base item overlap stays below the OPT PR-04 exit threshold.",
    context = kernelRun.profileOverlapSummary.toJson(),
),
```

如果出于“保持 whiteBox 宽松、phase4Report 严”的架构考虑故意分层，也请在文案里写清楚，例如 `"continuous sanity gate, tighter exit gate owned by phase4Report"`。

**改哪里（具体 file:line）**：

- `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt:194-199`：阈值从 `0.50` → `0.30`，文案更新。

---

### 3.4 【Minor】所有 profile 的 `excludeIds` 都是空数组

21/21 profile 的 `excludeIds: []`。Spec §4.1 把 `excludeIds` 列为必备新字段，但没有要求必须用。现状是“字段加了，schema 校验通了，数据实际没用这条能力”。

这直接关联 3.1：`excludeIds` 恰恰是解决 `maxOverlap=1.0` 的最低成本工具。

**建议**：与 3.1 合并修复。或至少在注释 / docs 里说明 `excludeIds` 的预期使用场景，避免未来维护者以为这是个死字段。

---

### 3.5 【Minor】`Phase4Report` 只检查 averageOverlap，不检查 maxOverlap

`tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt:636-649`：`status = verdictOf(lootAverageOverlap < 0.30)`，没有对 `lootMaxOverlap` 做任何断言；但 `currentValueText` 里的确展示了 `max=1.000`。

这是 3.1 的必然配套缺口：连 Phase4Report 都没把 maxOverlap 当硬门禁，`maxOverlap=1.0` 就能静默通过。

**建议**：与 3.1 合并处理，在 Phase4Report 增加软门禁（`< 0.80` 作为 warning 或 `< 0.90` 作为 fail）。

---

### 3.6 【Minor】mid-late zone 的 cadence / reward 仍是 FIXED_LIST

见 2.4 节表格第二行。Spec §4.3 第 4 条用了“中后期 zone、reward、secret profile 走 `TAG_WEIGHTED`”的推荐口径，secret 已经 100%，cadence/reward 只有 50%。

这是“没用上新工具”的典型 —— 新 schema 已经能支持 `TAG_WEIGHTED`，但数据端只挑了 `.secret` 作为示范。

**建议**：与 3.1 一起推进，让 mid-late zone 的差异化路径完整起来。

---

## 4. 修复建议与 follow-up 计划

### 4.1 合入前必须处理（Medium 起步）

| # | 问题 | 预计影响面 | 风险 |
| --- | --- | --- | --- |
| FIX-1 | 3.3 `WhiteBoxLootRunner` 阈值 0.50→0.30 与文案更新 | 1 个文件、2 行 | 极低（当前实测 0.198，仍会 PASS） |
| FIX-2 | 3.2 `specialTemplateTagPreference` 与 `affixTagPreference` 拆分（方案 A：合并/改名；方案 B：让 specialTemplateTagPreference 真的作用于 template 选择） | 2~3 个文件 | 低；需补一条专项单测覆盖新行为 |

### 4.2 强烈建议合入时或紧随其后处理（Major）

| # | 问题 | 预计影响面 | 风险 |
| --- | --- | --- | --- |
| FIX-3 | 3.1 `maxOverlap=1.0`：对 12 对 1.0 配对逐个配 `excludeIds` 或 `TAG_WEIGHTED + itemTagFilter`；优先处理 `.secret ↔ .cadence`、`grey_gate_depths.reward ⊂ foundation.boss` | 主要集中在 `data/loot/index.yaml`，不碰 runtime | 中；需要重新跑 `whiteBoxLoot + phase4Report + lootBalanceLab` 验证分布没有副作用 |
| FIX-4 | 3.5 Phase4Report 补 `maxOverlap` 软门禁 | 1 个文件、十几行 | 低 |

**建议顺序**：FIX-3 → FIX-4 → FIX-1 → FIX-2。FIX-3 完成后 FIX-4 的阈值更好定（能知道修完后 maxOverlap 落到哪个区间）。

### 4.3 验证命令

```bash
./gradlew :game:test
./gradlew whiteBoxLoot
./gradlew lootBalanceLab
./gradlew contentPackHarness
./gradlew phase4Report
```

所有命令都应保持当前 PASS 状态，并且：

- `phase4-summary.json` 中 `lootProfileMaxBaseItemOverlap` 从 `1.0` 降到一个明显更低的值（目标 `< 0.85`，stretch goal `< 0.75`）。
- `lootProfileAverageBaseItemOverlap` 不应反弹上 `0.30`。
- `lootProfileDistinctBaseItemCount >= 35` 不变。
- `lootBalanceLab` 的 rarity / pity 分布 failedExpectationCount 仍为 0。

---

## 5. 未列入问题的核查结论（已通过项）

为了避免“没提到=没看”，下面列出我核查过但没有问题的条目：

- **Spec §2.1 冻结口径（5 条）**：`rewardBudget` 未被转义成多掉落数量、仍走单 item reward 正式路径、special item 仍走 `SpecialTierEligibility`、V3 没有建立第二套 zone-exclusive special template 真源、没有双 schema 共存 —— 全部合规。
- **Spec §3.1 范围 & §3.2 非目标**：没有重写 `ItemGenerator` 主合同、没有新增玩家可见资源 key、没有引入掉落脚本、没有兼容旧 schema 的第二套 loader / overlay / runtime 分支。
- **Spec §4.1 明确不纳入的字段**：`categoryWeights / zoneExclusiveIds / rewardBudgetToDropCount` 全仓无命中。
- **Spec §4.3.1「不接受的做法」**：没有“先让 runtime 同时接受 V2/V3”、没有“只迁移一半”、没有“把 sample/fixture pack 留到后续 PR”这三种反模式。
- **Spec §5 推荐改动面**：`SchemaModels.kt / DataLoader.kt / GameContent.kt / FoundationGameSession.kt / data/loot/index.yaml / ContentPackRuntimeResolver.kt / sample pack / WhiteBoxLootRunner / LootBalanceLabRunner / ContentPackHarnessRunner / Phase4ReportRunner` 全部命中。
- **Spec §7 资源生成计划**：本 PR 确实没有新增图片 / 音频。
- **测试**：`SchemaV2LoaderTest.kt` 已改名/更新到 V3 语义（文件名保留但内容是 V3），`DataLoaderContentPackTest.kt` / `ContentPackRuntimeResolverTest.kt` / `ContentPackHarnessRunnerTest.kt` / `Phase4ReportRunnerTest.kt` / `ContractLintTest.kt` / `FoundationGameSessionTest.kt` 均有对应调整。

---

## 6. 最终裁决

**建议：通过合入（Pass with Major design follow-up）**。

- **硬门禁**（Spec §8 的 5 条 + §6 的 5 条必测行为 + §4.1.1 破坏式迁移 + §4.4.1 结构化 fail-fast）：**10/10 PASS**。
- **设计质量门禁**（非 Spec 强制但贴玩法目标）：
  - `averageOverlap` 从 0.23+ 收到 **0.198** ✓
  - `distinctBaseItemCount` 从 ~30 扩到 **53** ✓
  - `maxOverlap = 1.0`、12 对“一方完全子集”的 profile 配对 ✗ —— 要求在本 PR 或紧随的 follow-up PR 内处理。
- **代码卫生**：2 个 Medium 问题（`WhiteBoxLoot` 阈值/文案、`specialTemplateTagPreference` 语义落地），建议进入 same-day follow-up commits。

> 合入后，建议用一个 **PR-04-Hotfix / PR-04b** 处理 3.1 的 `maxOverlap` 与 3.2 的字段语义，这两件事都不改 runtime 主链路，是纯数据 + 解析器内的小范围改动，风险低、收益直接。

---

## 附录 A：本次核查涉及的主要文件（非测试）

| 文件 | 角色 |
| --- | --- |
| `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` | `LootProfileSchemaV3` 定义 + `LootPoolStrategy` 枚举 |
| `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` | V3 parse / 结构化 fail-fast |
| `game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt` | 候选池构造算法（新增文件） |
| `game/src/main/kotlin/com/ktome/game/GameContent.kt` | `lootProfilesById` / `lootProfileCandidatePool` 入口 |
| `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | runtime 如何消费候选池 / routeBiasTags |
| `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt` | overlay 白名单 |
| `game/src/main/resources/data/loot/index.yaml` | 21 个正式 profile |
| `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt` | whitebox sanity assertions |
| `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt` | phase4 出口门禁 |
| `tools/src/main/kotlin/com/ktome/tools/contentpack/ContentPackHarnessRunner.kt` | V3 overlay + V2 rejection scenario |
| `tools/src/main/resources/fixtures/content-packs/packs/fixture.legacy_v2_loot_profile/` | V2 rejection 反例固件（新增） |
| `tools/src/main/resources/fixtures/content-packs/packs/fixture.append_loot_pool/data/loot/loot.foundation.common.append.yaml` | append fixture 迁 V3 |
| `examples/content-packs/sample.flooded_relics/data/loot/sample.flooded_relics.loot.flooded_reliquary.secret.yaml` | sample pack 迁 V3 |

## 附录 B：实测度量原始值

`tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json`：

```json
"lootProfileAverageBaseItemOverlap": 0.19760822510822515,
"lootProfileMaxBaseItemOverlap": 1.0,
"lootProfileDistinctBaseItemCount": 53,
"affixCount": 78,
"uniqueTemplateCount": 20,
"artifactTemplateCount": 8,
"totalCount": 106
```

`whiteBoxLoot` 总体：`totalAssertions=40, passedAssertions=40, failedAssertions=0, verdict=PASS`。

`phase4-summary.json` 中 `lootProfileBaseItemOverlapMatrix` 的 `currentValueText = "average=0.198, max=1.000"`，`status = PASS`（因为只检查 `average < 0.30`）。
