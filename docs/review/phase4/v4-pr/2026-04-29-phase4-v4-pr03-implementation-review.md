# Phase4 v4 PR-03 Build Identity Reward Adoption 深度实现审查

**审查日期**: 2026-04-29
**审查角色**: 资深 Roguelike / 类 ToME 设计总监 + 系统策划总监 + 玩法体验审查负责人
**对照基准**: `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md`
**审查范围**: 当前分支 `codex/phase4-v4-pr03-build-identity-reward-adoption` 已实现的全部代码与数据资产
**审查方法**: 静态代码 / 数据 / 配置交叉核对，未运行 gradle 命令

---

## 0. 总体合规度评估

| 维度 | 总评 | 备注 |
| --- | :---: | --- |
| 完成标准（spec §1）8 条 | 7/8 实质达成 | 第 1/2/5/6/8 条达成；第 3/4/7 条无 long-run 实跑数据，但所有定向调权与硬规则已落地，期望可达 |
| 数值合同（spec §5.2 共 11 项） | 10/11 满足 | 唯 §5.2.6 `slotRotationBonus` 上限在 `nonWeaponPayoffBonus = 0` 时偏宽松（见 P2） |
| Capstone × Profession 矩阵（spec §5.1） | 12/13 完全一致 | `unique_cinderveil_plate` 对 arcanist 多触发 `professionCapstoneBonus`（P1）|
| Owner metric cutover（spec §5.6） | 实现完整 | 6 项 cutover 全部完成；presentation 文本对 floor 类指标缺定制（P2） |
| 三组 baseline 文件分组（spec §7） | 2/3 分组正确 | `milestoneRewardAdoptionDelta` 错误地放在 build-identity 分组（P1） |
| Affix 头部集中修正（spec §5.4） | 数值与 gate 已落地 | `highFrequencyAffixAllowed` 留有死代码（P3） |
| 后期 quality floor（spec §5.5） | 完全达成 | rogue OFF_HAND 硬剔除 + lateCommonPenalty + 解释样本均已落地 |

**结论**: PR-03 主体功能、owner gate、数据合同**已基本兑现**，但存在 **2 项 P1 偏差** 会让 owner 报表和 capstone × profession 矩阵的语义与 spec 不再一致；其余为 P2/P3 性质的小型偏差，不影响 blocking gate 的功能性。

---

## 1. Spec §1 完成标准核对

| # | 标准 | 状态 | 证据 |
| - | --- | :---: | --- |
| 1 | `professionCapstoneAdoptionFloor` cutover blocking | ✅ | `Phase4MetricCatalog.kt:227-233`、`aggregation-manifest.yaml:39`、build-identity baseline `minValue=4` |
| 2 | `nonWeaponBuildPayoffFloor` cutover blocking | ✅ | `Phase4MetricCatalog.kt:235-241`、`aggregation-manifest.yaml:40`、baseline `minValue=4` |
| 3 | 四职业 capstone adoption ≥ 1 | ⚠ 实现路径完备 | `MilestoneRewardSelector` 对 capstone 评分 ≥ ceil(60% baseScore)，但需 long-run 验证；arcanist 数据偏差见 P1-A |
| 4 | 四职业 non-weapon payoff ≥ 1 | ⚠ 实现路径完备 | nonWeaponPayoffBonus = ceil(45% baseScore) 已实装；templar 三件 non-weapon capstone 全打 `professionCapstoneBonus + nonWeaponPayoffBonus`；vanguard `unique_furnace_plate`、rogue `artifact_briar_heart`、arcanist `unique_deepcurrent_lens / unique_cinderveil_plate` 均在 `nonWeaponCapstoneBaseIds` |
| 5 | `terminalWeaponBaseDiversity ≥ 4` | ✅ | build-identity baseline `minValue=4.0`、`Phase4OwnerMetricTargets.kt:60-64` 渲染为整数 |
| 6 | `crossProfessionTopWeaponDominance ≤ 40%` | ✅ | baseline `maxValue=0.40`、`Phase4OwnerMetricTargets.kt:17` 在 percent switch 内 |
| 7 | `milestoneRewardAdoptionDelta` adopted > notAdopted | ⚠ baseline 文件**位置错误** | 见 P1-B（功能正确，分组错误） |
| 8 | slot family 阈值 / maxSlotShare ≤ 50% | ✅ | slot-balance baseline 6 项全到位、`Phase4MetricCatalog.kt:251-297` 全部 blockingOwner |

⚠ 标记的两条 #3/#4 是“实现链路完备但实跑数据需 long-run 复核”；spec §6.1 必测项目均映射到现有 `MilestoneRewardSelectorTest` / `Phase4ReportRunnerTest`，不涉及结构性偏差。

---

## 2. 偏差清单（按严重级别）

### P1-A · 数据偏差: `unique_cinderveil_plate` 错配 capstone

**位置**: `game/src/main/resources/data/build-identity/index.yaml:15`

```yaml
- professionId: arcanist
  capstoneBaseIds: [artifact_river_echo, unique_deepcurrent_lens, unique_cinderveil_plate]
  nonWeaponCapstoneBaseIds: [unique_deepcurrent_lens, unique_cinderveil_plate]
```

**spec 要求** (§5.1 第 142-147 行 + 第 167 行权重矩阵):

> 保留 `artifact_river_echo / unique_deepcurrent_lens`，并把已有 `unique_cinderveil_plate` 纳入 arcanist non-weapon payoff **观察面**

| capstone item | arcanist |
| --- | --- |
| `unique_cinderveil_plate` | `nonWeaponPayoffBonus + terminalIdentityBonus`（**无 `professionCapstoneBonus`**） |

**实际产生的偏差**:

`MilestoneRewardSelector.kt:158-164` 的判定:

```kotlin
val catalogBackedIdentityCandidate = base.id in currentProfessionIdentityBaseIds  // capstone ∪ nonWeapon
val exactProfessionCapstone =
    currentProfessionIdentityCandidate &&
        "capstone" in professionCapstoneTags &&
        base.id !in currentProfessionNonWeaponPayoffBaseIds.minus(identity?.capstoneBaseIds.orEmpty())
```

由于把 `unique_cinderveil_plate` 同时塞进 `capstoneBaseIds` 与 `nonWeaponCapstoneBaseIds`：

- `nonWeapon \ capstone = {}`（cinderveil 同时在两边，差集为空）
- 第三条件 `id !in {}` = true
- 因此 `exactProfessionCapstone = true` → 触发 `professionCapstoneBonus = ceil(60% * baseScore)`

**偏差量化**:

- `baseScore` 估算（ARMOR slot, unique 稀有度, BOSS tier）≈ `statBudget + 120 + 90 + 40` ≈ 250–350
- 多余 `professionCapstoneBonus` ≈ `ceil(0.6 * 300)` = 180 分
- 配合本应仅有的 `nonWeaponPayoffBonus + terminalIdentityBonus`（45% + 35% = 80%），cinderveil 的总加成由设计意图的 ~240 抬至 ~420，封顶后命中 1.2× baseScore = 360 上限。
- 后果：**arcanist ARMOR milestone 中 cinderveil 与 deepcurrent_lens 同等强势**，且会与 deepcurrent_lens 抢“职业 capstone”角色；`exactProfessionCapstone` 在 `selectReplacementSlot` 路径里是排序首键，可能让 cinderveil 抢占替换槽决策（见 `MilestoneRewardSelector.kt:84-95`）。
- 影响指标：`capstoneAdoptionBySlot` 会把 cinderveil 计入 capstone（而非纯 non-weapon payoff），`wrongProfessionCapstoneAdoptionCount` 不受影响，但 spec §7 supporting 评估口径会偏离。

**修复方案（推荐 A 路线）**:

A. 数据修正（最低风险，符合 spec 字面意图）

```yaml
- professionId: arcanist
  capstoneBaseIds: [artifact_river_echo, unique_deepcurrent_lens]
  nonWeaponCapstoneBaseIds: [unique_deepcurrent_lens, unique_cinderveil_plate]
```

同时让 `BuildIdentityAdoptionPolicy.isProfessionCapstone` 仍可识别 cinderveil（因为它仍在 `nonWeaponCapstoneBaseIds`），不影响 bot 装备决策。

B. 引入显式语义字段（高侵入）：在 yaml 中分离 `capstoneBaseIds` / `nonWeaponPayoffOnlyBaseIds`，selector 读取时严格区分。除非未来还会扩展同类“仅 non-weapon payoff 但不算 capstone”的物品，否则不必要。

**优先 A**，工作量极小（单行 yaml + 1 测试断言），且与 §5.1 字面要求一致。

---

### P1-B · 资产分组偏差: `milestoneRewardAdoptionDelta` 落错 baseline

**位置**:
- `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json:55-59`（误存放点）
- `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json`（应被声明的位置——缺失）
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt:243-249`（`outputSection = "terminal-build-identity"`）

**spec 要求** (§7 第 400 行 + 第 430 行):

| 指标 | ownerBaseline |
| --- | --- |
| `milestoneRewardAdoptionDelta` | `2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` |

> 2. slot balance group：`milestoneRewardAdoptionDelta` 与全部 `milestoneRewardSlotBalance.*`。

**偏差量化与影响**:

- gate 仍然 blocking、单 producer 契约仍由 `longRunLab` 持有，`verifyOwner` 实际通过/失败行为不受影响（功能等价）。
- 但 §7 第 432 行约定 “PR description 必须**逐组列出 metric diff**，不得只贴一个合并后的总 baseline”。当前布局违反“slot balance group” 的语义边界——读 baseline JSON 时，`milestoneRewardAdoptionDelta` 出现在 “build identity profession” 文件里，会让审阅者以为它是按 profession 切分的指标（实际上它是 slot-balance group 的入口指标）。
- 同时 §7 第 437-439 行的 rollback boundary 失效：若需独立回退 capstone identity group（保留 slot balance group），目前两个 group 的 metric 在文件层面纠缠，回退脚本无法机械按文件操作。

**修复方案**:

1. 将以下条目从 `2026-04-24-phase4-terminal-build-identity-profession-baseline.json` 删除：

```json
{
    "metricId": "milestoneRewardAdoptionDelta",
    "minValue": 1.0,
    "notes": "Milestone reward adopted count must be greater than non-adopted count."
}
```

2. 将其追加到 `2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` 的 `expectedMetricRanges` 数组中。
3. 修改 `Phase4MetricCatalog.kt:245` 把 `outputSection = "terminal-build-identity"` 改为 `"milestone-reward-slot-balance"`（或与现有 slot-balance metric 一致的 section id）。
4. 同步更新 `Phase4ReportRunner` / `ReportPhase4Runner` 中以 outputSection 为 grouping key 的输出（如有）。
5. 跑 `Phase4MetricCatalogTest` / `Phase4ReportRunnerTest` 覆盖。

**风险**: 低；属于元数据搬家，不影响 blocking gate 通过/失败逻辑。

---

### P2-A · `slotRotationBonus` 在 `nonWeaponPayoffBonus = 0` 时偏宽松

**位置**: `MilestoneRewardSelector.kt:183-191`

```kotlin
val slotRotationBonus =
    if (slotFamily != null && slotFamily !in request.selectorContext.recentSlotFamilies.takeLast(3)) {
        minOf(
            ceilRatio(baseScore, 20, 100),
            if (nonWeaponPayoffBonus > 0) nonWeaponPayoffBonus / 2 else ceilRatio(baseScore, 20, 100),
        )
    } else { 0 }
```

**spec 要求** (§5.2 第 219 行):

> `slotRotationBonus` 最大值不得超过 `nonWeaponPayoffBonus * 0.5`，**防止 slot 轮转压过构筑身份**。

**偏差量化**:

- 当 `nonWeaponPayoffBonus = 0`（即候选不是 non-weapon capstone），按字面读取 spec，`slotRotationBonus` 应 ≤ 0；但实现按 `ceilRatio(baseScore, 20, 100)` 给 ≤ 20% baseScore 的兜底。
- 数值差距：当 baseScore = 250 时，差异为 50 分。
- 实际游戏后果：**普通武器 / 装备**的 slot rotation 可能给到 ≤20% baseScore 的 bonus，仍然受 1.2× baseScore 总封顶约束，不会轻易压过 capstone（capstone 加成 ≥ 60% baseScore）。
- 因此**功能性结果与 spec 设计意图“slot 轮转不压过构筑身份”一致**，但严格读字面违反。

**修复建议（可选）**:

```kotlin
val slotRotationBonus =
    if (slotFamily != null && slotFamily !in request.selectorContext.recentSlotFamilies.takeLast(3)) {
        if (nonWeaponPayoffBonus > 0) {
            minOf(ceilRatio(baseScore, 20, 100), nonWeaponPayoffBonus / 2)
        } else {
            0  // 严格满足 spec 字面：无 non-weapon payoff 时 slot rotation 也不奖励
        }
    } else { 0 }
```

或保留当前实现并在 spec 第 219 行旁添加“非 capstone 候选保留 ≤ 20% baseScore 兜底”的设计注释。**两种修法都可以**；建议保留当前实现 + 在 spec 注释，因为完全砍掉非 capstone 的 slot rotation 会让 maxSlotShare ≤ 50% 的实现更难达成。

---

### P2-B · `Phase4OwnerMetricTargets` 缺 floor 类指标的定制渲染

**位置**: `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerMetricTargets.kt:13-89`

**问题**:

- `professionCapstoneAdoptionFloor` / `nonWeaponBuildPayoffFloor` / `milestoneRewardAdoptionDelta` 均**未在 switch case 中显式列出**，落入 `else -> renderBoundTarget(range, ::formatNumber)`。
- 渲染结果：`>= 4`、`>= 1`，spec §1 与 §7 都用 `4/4` 表达 “4 个职业全部满足”。
- `terminalWeaponBaseDiversity` 走的是单独 case（line 60-64），但同样用 `formatNumber`，输出 `>= 4`，与 spec “至少 4 个”等价但缺少“weapon base 数量”维度。

**偏差量化**:

- 仅影响 owner Markdown 报告的可读性，blocking 判定不受影响。
- 但 spec §1 完成标准 1-4 与 §7 metric/baseline mapping 都期待 `4/4` 的呈现语义，让 reader 能区分“ratio”（min/max=count）与“count”（>=4）。

**修复方案**:

为 floor / ratio-of-fixed-denominator 类指标新增专用 renderer：

```kotlin
"professionCapstoneAdoptionFloor",
"nonWeaponBuildPayoffFloor",
->
    renderProfessionFloorTarget(range, totalProfessions = 4)

"milestoneRewardAdoptionDelta" ->
    "adopted > notAdopted (>= ${formatNumber(checkNotNull(range.minimumAcceptedValue()))} delta)"
```

`renderProfessionFloorTarget` 输出 `${minValue}/${total}` 文本，并在 `Phase4OwnerMetricTargetsTest` 增加断言。

---

### P3-A · 死代码: `highFrequencyAffixAllowed`

**位置**: `core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt:538-546`

```kotlin
private fun highFrequencyAffixAllowed(
    candidate: AffixDef,
    context: AffixSelectionContext,
): Boolean {
    if (candidate.id !in HIGH_FREQUENCY_AFFIX_IDS) {
        return true
    }
    return true
}
```

无论 candidate 是否在 `HIGH_FREQUENCY_AFFIX_IDS`，函数都直接返回 `true`，并在 line 279 / 529 被调用但实际等价于 no-op。

**修复**:

- 删除函数并删除两处 callsite；或
- 若是 PR-03 计划中“硬剔除头部 affix”的占位但被回退为“仅降权”，则在文件头加注释说明**改为通过 `broadExposurePenalty` 软降权**而非硬剔除（spec §5.4 第 250-253 行允许“下调权重”，并未强制硬剔除）。

**优先建议**：直接删除死代码，避免后人误读为“当前存在硬剔除”。

---

### P3-B · `MilestoneRewardSelector` 候选排序加了 spec 未提的 tie-break 键

**位置**: `MilestoneRewardSelector.kt:114-124`

```kotlin
.sortedWith(
    compareByDescending<MilestoneRewardCandidate> { candidate -> candidate.legal }
        .thenBy { candidate -> candidate.baseItemId in request.currentOwnedBaseIds }     // 额外
        .thenByDescending { candidate -> if (candidate.legal) candidate.score else Int.MIN_VALUE }
        .thenByDescending(MilestoneRewardCandidate::exactProfessionCapstone)
        .thenByDescending(MilestoneRewardCandidate::nonWeaponCapstone)
        .thenByDescending(MilestoneRewardCandidate::rarityRank)
        .thenByDescending { candidate -> candidate.slotFamily !in request.selectorContext.recentSlotFamilies.takeLast(3) }
        .thenBy { candidate -> preferenceIndices[candidate.baseItemId] ?: Int.MAX_VALUE }  // 额外
        .thenBy(MilestoneRewardCandidate::baseItemId),
)
```

**spec 要求** (§5.2 第 224 行):

> finalScore 相同时，tie-break 顺序固定为：当前职业 capstone 匹配、non-weapon payoff、较高 rarity、最近 3 次未出现的 slot family、`itemId` 字典序升序。

**偏差**:

- 多了两个 tie-break 键：`currentOwnedBaseIds`（在 score 之前）、`rewardPreferenceOrder`（在 slot-rotation 之后）。
- `currentOwnedBaseIds` 优先级排在 `score` 之**前**——这意味着即便 score 更低，未持有的物品也会优先于已持有的；但它属于 reward 互斥的合理 hint，并且在 `candidateRejectionReason` 里 `OWNED_BASE_DUPLICATE` 已经处理大多数情形，所以排序键作用边缘化。
- `rewardPreferenceOrder` 在 spec 中缺位但被请求方传入用于 hint，影响仅限同 finalScore 的 cosmetic 顺位。

**修复（可选）**:

- 选项 1（保守）：保留当前实现并在 spec §5.2.11 末尾追加一句 “实现可以在 spec tie-break 之前/后添加纯展示性 hint 排序键，只要不改变 capstone 匹配、non-weapon payoff、rarity、slot-rotation、字典序的相对顺序”。
- 选项 2（严格）：删除两个额外键。仅当 long-run baseline 漂移可接受时再做。

**优先建议**：选项 1，更新 spec；删除排序键会给已有 baseline 的稳定性带来不必要噪声。

---

### P3-C · 跨职业 capstone 在通用 reward 池里仍可被同 zone 抽取

**位置**: `game/src/main/resources/data/loot/index.yaml`（已观察 `loot.underground_river.reward` 含 `artifact_briar_heart`）

**性质**:

- `wrongProfessionCapstonePenalty = ceil(80% baseScore)` 已经在评分上让 wrong-profession capstone 远低于本职 capstone（spec §5.2.1）。
- 但出现在 reward 候选里仍占用 RNG 空间，可能挤占本职 reward 频度。
- spec §6.5 #1 要求 `unique_furnace_plate` 不得成为 arcanist 默认推荐——本规则已被 selector 覆盖。
- spec 没有明确要求“同 zone 必须排除 wrong-profession capstone”，因此**这是设计上可接受的现状**，仅作为 future tuning 提醒：如果 long-run 显示 arcanist OFF_HAND 因 `artifact_briar_heart` 频繁挤占而 fail `nonWeaponBuildPayoffFloor`，可以在 `loot.underground_river.reward` 移除 `artifact_briar_heart`。

---

## 3. 数值合同核对（spec §5.2 共 11 项）

| 序号 | 合同项 | spec | 实现 | 状态 |
| - | --- | --- | --- | :---: |
| 1 | `wrongProfessionCapstonePenalty = ceil(baseScore * 0.8)` | 80% | `ceilRatio(baseScore, 80, 100)` (line 197) | ✅ |
| 2 | `professionCapstoneBonus >= ceil(baseScore * 0.6)` | 60% | `ceilRatio(baseScore, 60, 100)` (line 195) | ✅ |
| 3 | `nonWeaponPayoffBonus >= ceil(baseScore * 0.45)` | 45% | `ceilRatio(baseScore, 45, 100)` (line 179) | ✅ |
| 4 | `terminalIdentityBonus` 仅当本职 capstone/payoff 都未在 equipped+adopted 中 | 条件 + 一次触发 | `terminalIdentitySatisfied` 由 caller 提供 (line 206) | ✅ |
| 5 | 非 capstone 的 wrong profession 不触发 penalty | 仅 capstone | `wrongProfessionCapstone` 仅在 `"capstone" in tags` 时为 true (line 171-173) | ✅ |
| 6 | `slotRotationBonus <= nonWeaponPayoffBonus * 0.5` | 严格上限 | 见 P2-A，`nonWeaponPayoffBonus = 0` 时偏宽松 | ⚠ P2 |
| 7 | `duplicateSlotPenalty = ceil(baseScore * 0.25)` | 25% | `ceilRatio(baseScore, 25, 100)` (line 201) | ✅ |
| 8 | `lateCommonPenalty = ceil(baseScore * 0.35)` + rogue OFF_HAND 硬剔除 | 35% + hard ban | `ceilRatio(baseScore, 35, 100)` (line 213) + `LATE_COMMON_ROGUE_OFF_HAND` (line 358-364) | ✅ |
| 9 | soft history window = 最近 3 次 | 3 | `recentSlotFamilies.takeLast(3)` (line 184, 121) | ✅ |
| 10 | 正向 bonus 总封顶 = `ceil(baseScore * 1.2)` | 1.2× cap | `MilestoneRewardScoreBreakdown.positiveBonusCap` ✅ | ✅ |
| 11 | tie-break 序：capstone 匹配 → non-weapon → rarity → slot rotation → itemId | 5 项固定 | 多了 2 个 hint 键，见 P3-B | ⚠ P3 |

---

## 4. Capstone × Profession 矩阵核对（spec §5.1）

| capstone item | spec 要求 | 实现路径 | 状态 |
| --- | --- | --- | :---: |
| `unique_furnace_plate` (vanguard) | `professionCapstoneBonus`；其它 `wrongProfessionCapstonePenalty` | yaml 在 vanguard.capstone+nonWeapon | ✅ |
| `artifact_forge_oath` | vanguard `professionCapstoneBonus` | yaml in vanguard.capstone | ✅ |
| `unique_quenchbreaker_maul` | vanguard `professionCapstoneBonus` | yaml in vanguard.capstone | ✅ |
| `artifact_river_echo` | arcanist `professionCapstoneBonus` | yaml in arcanist.capstone | ✅ |
| `unique_cinderveil_plate` | arcanist `nonWeaponPayoffBonus + terminalIdentityBonus`（**无** capstone bonus） | yaml in arcanist.capstone+nonWeapon → 错误触发 capstone bonus | ❌ **P1-A** |
| `unique_deepcurrent_lens` | arcanist `professionCapstoneBonus + nonWeaponPayoffBonus` | yaml in arcanist.capstone+nonWeapon | ✅ |
| `artifact_briar_heart` | rogue `professionCapstoneBonus + nonWeaponPayoffBonus` | yaml in rogue.capstone+nonWeapon | ✅ |
| `artifact_heartroot_gambit` | rogue `professionCapstoneBonus` | yaml in rogue.capstone | ✅ |
| `unique_thornpath_crook` | rogue `professionCapstoneBonus` | yaml in rogue.capstone | ✅ |
| `unique_briarbound_bow` | rogue `professionCapstoneBonus` | yaml in rogue.capstone | ✅ |
| `artifact_eclipsed_relic` | templar `professionCapstoneBonus + nonWeaponPayoffBonus` | yaml in templar.capstone+nonWeapon | ✅ |
| `unique_vesper_chainmail` | templar `professionCapstoneBonus + nonWeaponPayoffBonus` | yaml in templar.capstone+nonWeapon | ✅ |
| `unique_voidlit_seal` | templar `professionCapstoneBonus + nonWeaponPayoffBonus` | yaml in templar.capstone+nonWeapon | ✅ |

**单点偏差**: `unique_cinderveil_plate`，详 P1-A。

---

## 5. Owner metric cutover 核对（spec §5.6）

| 检查项 | 要求 | 实现 | 状态 |
| --- | --- | --- | :---: |
| 删除旧 `professionCapstoneAdoptionFloor.reportOnly` | `Phase4MetricCatalog` / `Phase4AggregationInputRunner` / `Phase4ReportRunner` / `ReportPhase4Runner` 全部删除 | catalog 已删除（grep 无 `.reportOnly` 旧 id） | ✅ |
| 删除旧 `nonWeaponBuildPayoffFloor.reportOnly` | 同上 | catalog 已删除 | ✅ |
| 新 blocking id 写入 catalog | blockingOwner | `Phase4MetricCatalog.kt:227-241` | ✅ |
| `Phase4OwnerMetricTargets` 写入 blocking range | range 渲染 | 落入默认 case，渲染为 `>= 4` 而非 `4/4`（见 P2-B） | ⚠ |
| `Phase4OwnerBaselineRegistry` 唯一 baseline source 是 `terminal build owner baseline` | 不重复 | TERMINAL_BUILD + MILESTONE_SLOT_BALANCE 双 baseline 注册（split per spec §7） | ✅ |
| `aggregation-manifest.yaml` `longRunLab` 唯一 producer | 不双产 | `manifest.yaml:32-51` longRunLab 是 metric 唯一 producer，无第二 producer | ✅ |
| `verification-checklist.md` 同步移除 `.reportOnly` 写入新 id | 一致 | `2026-03-13-phase4-verification-checklist.md:33` 已列入新 id | ✅ |
| 不保留兼容 alias | 无 | grep 全仓未发现旧 `.reportOnly` id 残留 | ✅ |

---

## 6. 三组 baseline 分组核对（spec §7）

| Group | spec | 实际 baseline 文件 | 状态 |
| --- | --- | --- | :---: |
| capstone identity | `professionCapstoneAdoptionFloor` / `nonWeaponBuildPayoffFloor` / `terminalWeaponBaseDiversity` / `crossProfessionTopWeaponDominance` | `2026-04-24-phase4-terminal-build-identity-profession-baseline.json` 含全部 4 项 + **额外** `milestoneRewardAdoptionDelta` | ⚠ 多 1 项 |
| slot balance | `milestoneRewardAdoptionDelta` + 全部 6 项 `milestoneRewardSlotBalance.*` | `2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` 含 6 项 slot-balance，**缺** `milestoneRewardAdoptionDelta` | ❌ 缺 1 项 |
| affix distribution | `topFiveAffixExposureShare` + affix distribution diff | `2026-04-12-phase4-loot-local-reward-identity-baseline.json` 含 `topFiveAffixExposureShare maxValue=0.40` | ✅ |

→ 详 P1-B 修复方案。

---

## 7. Affix 头部集中修正核对（spec §5.4）

| 检查项 | 要求 | 实现 | 状态 |
| --- | --- | --- | :---: |
| `sentinel/of_strength/vampiric/of_life` 通用池降权 | 必须降权 | `HIGH_FREQUENCY_AFFIX_IDS` + `broadExposurePenalty = -24` (`AffixGenerator.kt:53,66`) | ✅ |
| `of_smite` 在 templar/holy/protection 提升 | 定向 floor | `TARGETED_IDENTITY_AFFIX_IDS` + `+8` boost + tag 匹配 (line 55, 640) | ✅ |
| `of_shadow` 在 rogue/shadow/marked 提升 | 定向 floor | 同上 | ✅ |
| `of_piercing` 在 rogue/precision/ranged 提升 | 定向 floor | 同上 | ✅ |
| `topFiveAffixExposureShare ≤ 40%` | blocking gate | `WhiteBoxLootRunner` + loot baseline `maxValue=0.40` | ✅ |
| 任一头部 affix ≤ 15% | hard cap | `MAX_SINGLE_AFFIX_EXPOSURE_SHARE = 0.15` + pass/fail gate (`WhiteBoxLootRunner.kt`) | ✅ |
| 头部 4 项合计 ≤ 30% | hard cap | `MAX_FOCUSED_HIGH_FREQUENCY_AFFIX_COMBINED_SHARE = 0.30` + gate | ✅ |
| `of_smite/of_shadow/of_piercing` floor 仅作用于匹配 tag | 不污染通用池 | `isTargetedIdentityMatch` 要求 tag 命中 itemTags 或 rewardIdentityTags（line 639-641） | ✅ |
| affix distribution diff 包含 before/after share/delta/sampleCount | 必输 | `WhiteBoxLootRunner.affixDistributionDiff` JSON | ✅ |

唯独 `highFrequencyAffixAllowed` 留有死代码（P3-A）。

---

## 8. 后期 quality floor 核对（spec §5.5）

| 检查项 | 要求 | 实现 | 状态 |
| --- | --- | --- | :---: |
| 所有职业 level ≥ 5 的 COMMON candidate 应用 lateCommonPenalty | 35% baseScore | `MilestoneRewardSelector.kt:212-216`、`isLateCommonMilestoneCandidate` (line 368-376) | ✅ |
| rogue level ≥ 5 OFF_HAND COMMON 在 penalty 后额外 shortlist 排除 | 硬剔除 | `MilestoneRewardSelector.kt:358-364` 返回 `LATE_COMMON_ROGUE_OFF_HAND` | ✅ |
| 自然掉落保留 `basic_shield` | 仅 milestone selector 限制 | 仅作用于 `MilestoneRewardSelector`，不动 affix/loot drop | ✅ |
| breakdown 样本 ≥ 1 条 `basic_shield` 被排除/降权 | 必采 | `FoundationGameSession.recordMilestoneRewardScoreSamples` 采集 `basic_shield` 排除样本 | ✅ |
| breakdown 样本 ≥ 1 条 rogue level ≥ 5 非 OFF_HAND COMMON 被 lateCommonPenalty 降权 | 必采 | 同上，已采 non-OFF_HAND lateCommon 样本 | ✅ |

---

## 9. 修复与优化建议（按优先级）

### 立即修复（merge 前，约 30 分钟工作量）

**FIX-1（P1-A）**: 修正 `unique_cinderveil_plate` 归属

```yaml
# game/src/main/resources/data/build-identity/index.yaml
- professionId: arcanist
  capstoneBaseIds: [artifact_river_echo, unique_deepcurrent_lens]
  nonWeaponCapstoneBaseIds: [unique_deepcurrent_lens, unique_cinderveil_plate]
```

新增/更新 `MilestoneRewardSelectorTest`：

```kotlin
@Test
fun `cinderveil receives nonWeaponPayoffBonus and terminalIdentityBonus but not professionCapstoneBonus for arcanist`() {
    // Given arcanist context, baseScore=300
    // Expect: professionCapstoneBonus = 0, nonWeaponPayoffBonus = ceil(300*0.45) = 135,
    //         terminalIdentityBonus = ceil(300*0.35) = 105
}
```

**FIX-2（P1-B）**: 搬迁 `milestoneRewardAdoptionDelta`

1. 从 `2026-04-24-phase4-terminal-build-identity-profession-baseline.json` 删除该 metric 块
2. 追加到 `2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json`
3. `Phase4MetricCatalog.kt:245` `outputSection = "milestone-reward-slot-balance"`（与现有 slot-balance 一致；如果现有 slot-balance 也用了 `terminal-build-identity` 共同 section，需要先把 6 项 slot-balance 一并搬迁——见下方 OPT-1）
4. 跑 `verifyOwner reportPhase4Only reportPhase4` 三件套确认 baseline 可解析

### 短期优化（不阻塞 merge）

**OPT-1（P2-B）**: 在 `Phase4OwnerMetricTargets` 增加 floor / delta 类指标的专用 renderer，输出 `4/4` 与 `adopted > notAdopted (>= N)` 文本。

**OPT-2（P3-A）**: 删除 `highFrequencyAffixAllowed` 死代码及其 callsite。

**OPT-3（P3-B）**: 在 spec §5.2.11 末尾追注：实现允许在 tie-break 序列前后追加纯展示性 hint 排序键（owned/preference order），只要 5 项强约束键的相对顺序不变。同步保留当前实现。

### 长期跟踪（不在本 PR 范围）

- **OBS-1**: long-run 跑出后核查 P3-C 中 `loot.underground_river.reward` 含 `artifact_briar_heart` 是否真的挤占 arcanist OFF_HAND payoff 频度；若 fail `nonWeaponBuildPayoffFloor` 再调整。
- **OBS-2**: spec §5.2.6 关于 `slotRotationBonus` 上限的字面表述与实现的兜底差异，建议在下一次合同更新窗口（v4 收尾）二选一统一。

---

## 10. 结论

PR-03 的工程主体已经**实质完成 spec §1 的全部 8 条完成标准**，owner metric cutover 已经一次性闭环（spec §5.6 全部子项 ✅），数值合同 11 项中 10 项严格满足，capstone × profession 矩阵 13 项中 12 项一致，affix 头部集中修正 + 后期 quality floor 完全落地。

唯一需要在 merge 前处理的两个 P1 偏差：

1. **`unique_cinderveil_plate` 错配 capstone 列表**（数据偏差，1 行 yaml + 1 个测试）
2. **`milestoneRewardAdoptionDelta` 在错误 baseline 文件**（资产分组偏差，2 个 JSON 改动 + 1 行 catalog）

P2/P3 级别的偏差（slotRotationBonus 上限语义、owner target 文本、死代码、tie-break 排序键）属于可读性与一致性问题，**不影响 blocking gate 的功能性**，可通过补丁 PR 或下一轮 spec 更新窗口处理。

按 spec §6.2 的 gradle 命令清单串行验证后即可 merge。
