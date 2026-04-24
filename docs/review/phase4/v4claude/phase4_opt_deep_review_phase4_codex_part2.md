# Phase 4 深度玩法体验审阅 — Part 2：玩法体验总评

本 Part 沿"玩家真实感知路径"走一遍 Phase 4 的七个体验维度：**核心循环、战斗、成长、奖励、探索、UI 反馈、系统联动**。每个维度按"设计承诺 → 当前实际体验（含证据） → 玩法判定 → 关键问题"四段式铺开。判定只用四档：`好玩` / `刚够` / `不够好玩` / `伪完成`。

---

## § 1 · 核心循环（开局 → 区域 → Boss → 终盘）

### 1.1 设计承诺（docs/phase4/roadmap.md）

Phase 4 主题是"从可完成长局 → 重复游玩差异明显的长局"。核心循环要求：

- 开局职业/种族选择 + 初始区域 → 沿 `shattered_outpost` → 分支区域（greenwood/deep_iron/grey_gate/underground_river/crystal_cavern）→ `abyssal_temple` → 终 boss 的主线成立；
- 5 次连续长局至少有 3 次在 topology + content + loot + encounter + hidden 其中 3 类维度可感差异；
- 关键路径战斗量下限与 objective 节奏下限满足（V3 PR-01）。

### 1.2 当前实际体验（证据）

- `longRunLab` 6 次 full_route 全部 `outcome=Victory(floor=1, reason=boss_defeated)`，`finalZone=abyssal_heart`，**平均 turns 572.8 / headless 1361**（`build/reports/harness/long-run-summary.md`）。
- `criticalPathCombatFloorSatisfied: 100% (5/5)`，min objective 7.2 / min visible hostile 10.2 / min enemy turns 74.8（phase4Report，原始 V3 PR-01 关键路径节奏承诺已兑现）。
- `zoneRouteHashDistribution` 呈现一个高频 hash `6871...4dbaaf=6`（6 条 full_route 走同一 route hash），其他 4 条 hash 各 1-2 条——**主路线 6 条 full_route 的区域序列高度同构**。
- `deathDistribution: {abyssal_heart=2, deep_iron_pit=2, grey_gate_depths=1}` — 死亡分布主要在后期，前中期阵亡很少（6 次 full_route 无开局/中段阵亡）。

### 1.3 玩法判定：**刚够**

- **正向**：关键路径节奏线性、战斗量充足、长局完成率 6/6（100%）；客观战斗负担不再"走过去就赢"，比 V3 前有质的改善。
- **负向**：6 条 full_route 有 6 条走同一个 `zoneRouteHash`。意味着在同一 seed 家族下，topology + node + reachable route 的排列几乎没变；区域选择的多样性在"路线层面"并未触及 Phase 4 承诺的"5 局 3 差异"。需要确认这是 `LONG_RUN_SMOKE` corpus 的确定性需求还是分支采样不足——从 `scenarioTypeDistribution: {full_route=6, branch_inclusive=1, ...}` 看，branch_inclusive 只 1 条，确实偏少。

### 1.4 关键问题

1. **【P1】长局 harness 的 branch 采样过少。** full_route=6 的多样性高度收敛到单一 route hash。`zoneRouteHashDistribution` 显示 6/11 条样本全走同一 hash，虽可能是 smoke corpus 设计使然，但作为 Phase 4 收口证据，branch 样本应至少拉到 3 条，让"区域选择的结构差异"进入 owner metric。
2. **【P1】前中期阵亡缺席。** 死亡集中在 `abyssal_heart/deep_iron_pit/grey_gate_depths`，且 6 full_route 全部成功。Phase 4 应有"前中期失败→换 build 重试"的重玩驱动力，目前 long_run smoke 不 expose 这类场景（可能只有更困难的 `hard_route` corpus 才会验证，但此处没有对应 owner metric 锚定前中期死亡率 floor）。

---

## § 2 · 战斗体验（telegraph / 决策面 / 风险成本 / boss 语言）

### 2.1 设计承诺

- PR-06 terrain interaction + elite mutation + boss variant 引入"地形 × 精英 × 变体"三层战斗语言；
- V3 PR-04 boss phase identity 承诺"不再只有血量和掉落"，每 boss 至少 2 阶段，variant 在 mutation / terrain / 掉落 / 表现有可感差异；
- UIUX PR-05 战斗 3 层决策面（ACTION → METHOD → TARGET）+ telegraph 三位一体；
- V3 PR-05 frontstage action cue typed contract（`FrontstageActionCueSnapshot`）。

### 2.2 当前实际体验（证据）

- **决策面与 cue 层（UI / 输入侧）**：
  - `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionFrame.kt` / `CombatDecisionPanel.kt` / `CombatDecisionValidationSurface.kt` / `CombatDecisionFeedbackKeys.kt` — UIUX PR-05 的 3 层决策面已实现；
  - `client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt` + core `FrontstageActionCueSnapshot` — V3 PR-05 typed cue 合同落地；
  - phase4Report cue 4 项 100%（`frontstageHighPriorityCueRetainedRate / frontstageCueDedupAppliedCount / frontstageCueExpiryParity / frontstageSecretCueVisibilityRate`）。
- **Telegraph 与地形交互**：
  - `terrainInteractionEncounterRate.aggregate=16.6% (+31.95% vs baseline 12.6%)`；per-zone floor 11.0% 全部达标；
  - telegraph 合同属于 V3 PR-05 一部分，已纳入 action cue 合同。
- **Boss 阶段与变体**：
  - `phaseTransitionObservedRatio=100% (6/6)` — 每个 base boss 都被 harness 观察到 phase 过渡；
  - `variantTraceDivergenceRatio=100% (3/3)`、`minVariantActionTraceDivergenceScore=1.000`、`bossVariantBasePhaseCountMin=2` — 3 个 variant 的 trace 与 base 有差异；
  - **但** `game/src/main/resources/data/boss-variants/index.yaml` 只有 3 条（`molten_glass / grey_crown / abyssal_eclipse`），每条仅 2–4 个 `actionWeights` 覆写，**无 `phaseOverrides` 字段**；
  - 对应的 base boss 源数据（`game/src/main/resources/data/bosses/index.yaml`）有 `phase_full + phase_enraged/desperate`，但 variant 不贡献 variant-level 阶段语言。

### 2.3 玩法判定：**刚够 / 局部伪完成**

- **刚够的部分（UI / 决策面 / cue）**：UIUX PR-05 + V3 PR-05 的前台决策输入链已正式合同化；玩家在战斗中"看得到 priority、看得到 secret cue、被 TTL 正确回收、被 dedup 合并"这些都成立。
- **伪完成的部分（boss variant 语言）**：`bossHarness` 四项指标全部 100% PASS 是一个**测量角度误导**的典型例子：它测的是"trace 有没有 divergence"、"phase 有没有过渡"，但观察到过渡的是 base encounter 的 phase（`phase_full → phase_enraged/desperate`），而不是 variant 引入的新阶段。当一个玩家在 `grey_gate_depths` 碰到 `grey_crown` 变体时，他在体感层面得到的是"还是同一个 dungeon_lord，只是换了几个 action 权重"；Phase 4 "不再是终盘只看血条"的承诺在 source data 层面没有达成。
- **行动池多样性低**：variant 的 `actionWeights` 只有 2–4 条，`phaseOverrides` 空缺，意味着即便 harness 能证明 trace 有差异，玩家前台感知到的只是"某几个动作权重被抬高"，而不是"新阶段 / 新模式"。

### 2.4 关键问题

1. **【P1】boss variant 仅靠 actionWeight 差分，未引入 phase-level 重写。** 必须在 Phase 4 内要求每个 variant 至少贡献 1 个 `phaseOverrides`——例如 `molten_glass` 在 `phase_enraged` 引入 "glass shard" 地形互动、`grey_crown` 在 `phase_desperate` 引入 "crown summon" 召唤行为、`abyssal_eclipse` 在 `phase_enraged` 引入 "eclipse field" AoE telegraph。没有这类 variant-only 阶段重写，bossHarness 的指标就是"测量合同对的但玩家感知不到"的典型伪完成。
2. **【P1】variant 数量对 terminalWeaponBaseDiversity 的反作用。** 目前 6 full_route 的 final_zone 都是 `abyssal_heart`（boss），但 variant 选择不会影响终盘装备的 build identity 路径（见 § 3），因为 boss reward profile 对所有 variant 公用。应在 V3 PR-02 capstone source preference 层引入"variant-aware" 的 capstone 优先：例如 `grey_crown` 更偏向 templar capstone `artifact_eclipsed_relic`、`abyssal_eclipse` 更偏向 arcanist capstone `artifact_river_echo`。目前这层没有接线。
3. **【P2】决策面金流确认。** UIUX PR-05 的 deferred `COMBAT_DECISION` frame 已正式收口，无残余 stub；但 `docs/opt/ui-pr/README.md` §Golden Label 所有权要求 "上线时清理任何 PR-02 遗留 combat-decision-stub label"——需要确认 golden 库已清理。当前证据链显示 label 命名空间正确（`phase4-uiux-pr05-*`），但最终上线时需要 grep 全库确认无 `phase4-uiux-pr02-combat-decision-stub-*` 遗留。

---

## § 3 · 成长体验（天赋 / 装备 / affix / capstone）

### 3.1 设计承诺

- V3 PR-02：每职业至少一条 capstone 路径稳定兑现；dynamic pool coverage 100%；每职业 seen>=1, adoption>=1, nonWeapon>=1 floor。
- PR-04/05 loot budget v2 + affix cost：保证 affix 在 milestone 反馈里不同稀有度、不同 affix 组合能被观察到。

### 3.2 当前实际体验（证据）

#### 3.2.1 终盘 build（long-run full_route 6 条采样）

| 职业 | 终盘 WEAPON | 终盘 OFF_HAND | 终盘 ARMOR | capstone 命中 | 类别 |
| --- | --- | --- | --- | --- | --- |
| vanguard | `long_sword`（MITHRIL + ironroot+of_storms+warforged+of_piercing） | `basic_shield` | `unique_furnace_plate`（sentinel+of_cleansing+sturdy+of_fortitude） | ✅ vanguard capstone `unique_furnace_plate` 已 adopted ARMOR | aligned + capstone |
| arcanist | `arcane_staff` | `emerald_charm` | `unique_furnace_plate`（emberguard+of_fortitude） | ⚠️ arcanist 采用了 vanguard capstone；arcanist 自己的 `artifact_river_echo / unique_deepcurrent_lens` 未 adopt | aligned weapon + cross-profession armor |
| rogue | `hunter_bow`（MITHRIL + starforged+of_shadow+vampiric） | `basic_shield` | `leather_armor` | ❌ rogue capstone `artifact_briar_heart` seen 未 adopt | aligned weapon only |
| templar | `long_sword`（MITHRIL + sanctified+of_smite+warforged+of_strength） | `artifact_eclipsed_relic`（hallowed+of_cleansing） | `chain_mail` | ✅ templar capstone `artifact_eclipsed_relic` 已 adopt OFF_HAND | aligned + capstone |
| berserker | `war_maul` | 空 | `unique_cinderveil_plate`（duskwoven+of_life+spite_fed+of_focus） | — berserker 非 4 个 canonical profession 之一 | — |
| spellblade | `arcane_staff` | `furnace_talisman` | `apprentice_robe` | — 同上 | — |

#### 3.2.2 Affix / 稀有度 / milestone

- `milestoneRewardQualityDistribution: {MAGIC=68, RARE=35}` — MAGIC 占 66%，RARE 占 34%，稀有度梯度合理。
- `milestoneAffixCountDistribution: {2=72, 3=10, 4=21}` — 2 affix 占 70%、3 占 10%、4 占 20%，affix 数量分布有多样性。
- `affixSynergyActivationCount: 5` / `synergyAffixAdoptionCount: 15` — affix synergy 小规模成立。

#### 3.2.3 Capstone 每职业分项（`professionCapstoneAdoptionFloor.reportOnly`）

- vanguard: ✅ adoption>=1（`unique_furnace_plate`）；
- templar: ✅ adoption>=1（`artifact_eclipsed_relic`）；
- arcanist: ❌ `adoption=0/1, samples=3`；`artifact_river_echo / unique_deepcurrent_lens` seen 但未 adopt；
- rogue: ❌ `adoption=0/1, samples=3`；`artifact_briar_heart / unique_briarbound_bow` seen 但未 adopt。

### 3.3 玩法判定：**不够好玩**（两职业 build identity 未兑现）

- **正向**：vanguard 有稳定终盘 `long_sword + unique_furnace_plate`；templar 有稳定终盘 `long_sword + artifact_eclipsed_relic`；affix 稀有度与数量梯度合理；天赋 breakpoint payoff 5 次观察到 build hash 变化。
- **严重负向**：arcanist 与 rogue 的 capstone 采用率为 0/1，整局 3 个 sample 没有任何一次 adopt 自己的 profession capstone；arcanist 甚至 end up 穿 vanguard 的 `unique_furnace_plate`（cross-profession armor），这对"arcanist 的 build identity"是语义级破坏——arcanist 的终盘造型会与 vanguard 撞车；rogue 的 ARMOR 也始终停在 `leather_armor`，没有任何 armor-level build identity。
- **收敛问题**：templar + vanguard 都终盘 `long_sword`，虽然长剑的 semantic tag `[item, weapon, melee, frontline, guard, discipline]` 对两个职业都 align，但玩家感知上"两个职业都是长剑党"属于典型的 identity 撞车。Phase 4 承诺的"每个 profession 有独特终盘 weapon" 没兑现。

### 3.4 关键问题

1. **【P0】arcanist / rogue 的 capstone adoption 为 0。** 这是 V3 PR-02 的直接回归风险区。必须在 Phase 4 内：
   - 检查 arcanist capstone `artifact_river_echo` 在 `underground_river_crystal_rift` secret zone 的 fixedItemIds 已包含（证据：phase4Report §Local Reward Identity 已列出）；
   - 检查 adoption 逻辑是否受"前置装备 threshold"影响，以至于 arcanist 持有 `arcane_staff` 时对 OFF_HAND slot 的 artifact 评分过低；
   - 至少需要把 arcanist / rogue 的终盘 capstone 采用率从 0/3 抬到 1/3（即 1/3 的局里能 adopt），否则 V3 PR-02 的"每职业 floor" 承诺只是 lexicographic 的 reportOnly。
2. **【P0】templar + vanguard 终盘武器撞车 long_sword。** 应引入 profession-favored terminal weapon：
   - templar 应有独立 terminal weapon candidate（例如 `unique_vesper_chainmail` 同族的 templar 专属 weapon `unique_zealight_maul`，或在 `greenwood_ambush_hideout.secret` 中为 templar 引入 `unique_greenwood_watcher_blade` 的 boss 掉率）；
   - vanguard 保持 `unique_quenchbreaker_maul`（已在 capstone 列表）作为 terminal weapon 候选，把 long_sword 降级为 mid-route 过渡；
   - 目标：`crossProfessionTopWeaponDominance` 从 50% 压到 ≤40%，且 `terminalWeaponBaseDiversity` 从 3 提到 4。
3. **【P1】arcanist 的 ARMOR 路径被 `unique_furnace_plate` 吞掉。** arcanist 应偏向 `apprentice_robe` 或 arcanist-specific 的 armor（如 `unique_tideveil_robe`，当前不在源数据中）。这是奖励侧的 slot-bias 与 affix tag preference 在 arcanist 上没有对 ARMOR 做向内收敛。

---

## § 4 · 奖励体验（loot 多样性 / slot 分布 / rarity 反馈）

### 4.1 设计承诺

- PR-04/05 loot budget v2；每 zone 有 cadence/main/secret 三层 profile；secret zone reward 单一权威；dynamic pool coverage 100%。

### 4.2 当前实际体验（证据）

- **Loot profile 类型切换**（`game/src/main/resources/data/loot/index.yaml`）：全部 10 个 cadence + main reward profile 已为 `TAG_WEIGHTED`；secret profile 保留 `FIXED_LIST`（策划性曲线）——符合设计。
- **Dynamic pool coverage**：`dynamicPoolCoverage=100% (10/10)`，`duplicateFamilies=0`，`meaningfulSwap=100%`。
- **Milestone slot 分布**（long-run 103 次 milestone 奖励）：
  - `ARMOR=8 (7.8%)`
  - `OFF_HAND=49 (47.6%)`
  - `WEAPON=46 (44.7%)`
- **Milestone 稀有度分布**：`MAGIC=68, RARE=35`（66/34 合理）。
- **Affix usage**（route reward 视角）：`sentinel=8, of_strength=7, vampiric=7, of_life=6, of_cleansing=4, of_storms=2, of_fortitude=2` ... — 头部 5 个 affix 集中度约 50%。
- **Secret zone reward 权威**：`secretZoneRewardAuthorityViolations=0`；secret zone 只通过 `secretZoneDef.rewardProfileId` 配奖。
- **Special tier 家族重复**：`specialTierPassiveFamilyDuplicateCount=0`。

### 4.3 玩法判定：**刚够 / ARMOR 维度伪完成**

- **正向**：动态池覆盖、special tier 家族唯一、secret reward 单一权威三项 V3 PR-02/03 核心承诺全部达成；稀有度梯度合理。
- **负向（ARMOR）**：103 次 milestone 奖励中 ARMOR 只占 7.8%（8 次）。玩家在整局几乎只在 SUPPORT / CACHE 偶尔看到 ARMOR 奖励，大部分 milestone 都是 OFF_HAND 或 WEAPON 的互相覆盖。对比 WEAPON=44.7% / OFF_HAND=47.6%，ARMOR 完全处于"陪跑"状态。Phase 4 主设计要求 loot 有跨 slot 的 build 决策力，但现状把 ARMOR 这条维度从"玩家需要做取舍"压扁成"反正没多少选择，能穿就穿"。

### 4.4 关键问题

1. **【P1】milestone slot 分布需要重新平衡。** 目前 WEAPON + OFF_HAND = 92%，ARMOR = 8%。建议：
   - 在 cadence / main reward profile 的 `slotBias` 中，将 ARMOR 至少抬到 20%；
   - 将 BOSS milestone 的第二条奖励锁定为 ARMOR 或 OFF_HAND 中的"另一个 slot"（避免连续 WEAPON BOSS 奖励）；
   - 对长局 harness 加入 `milestoneRewardSlotBalance` owner metric：要求每 slot >= 15%。
2. **【P2】头部 affix 集中度偏高。** `sentinel / of_strength / vampiric / of_life` 占据大部分 rare affix 曝光。`synergyAffixDistribution: {of_piercing=11, of_shadow=6, of_smite=1}` 的 synergy affix 中 `of_smite` 只有 1 条 adoption。Phase 4 并无硬性 affix 分布 floor，但从体感上"长局装备上反复看到 sentinel" 会削弱奖励惊喜感。这不阻塞 Phase 4 收口，归为 P2。

---

## § 5 · 探索体验（organic hidden / secret zone / 地形语义）

### 5.1 设计承诺

- V3 PR-03：organic hidden loop；leadDiscoveryRate >= 30%, searchUse >= 15%, secretEntry >= 15%；per-zone floor 5%；`sameZoneSecretVsCadenceMaxOverlap <= 0.5`；`sameZoneSecretVsRewardMaxOverlap <= 0.5`；secret reward single authority。

### 5.2 当前实际体验（证据）

- **Aggregate hidden metrics**：
  - `leadDiscoveryRate=46.6% (246/528)` / `searchUse=16.9%` / `secretEntry=20.5%` — 三项全部过 floor；
  - `secretConversionRate=43.9% (108/246)` — 过 floor 20%；
  - `scriptedHiddenVerificationRate=100% (625/625)`；
- **Zone 分布**（`zoneDiscoveryDistribution` / `secretZoneDiscoveryDistribution`）：

| Zone | lead discovery share | secret zone | secret entry share |
| --- | --- | --- | --- |
| `abyssal_temple` | **52.0%** | `abyssal_temple_warded_archive` | 18.5% |
| `greenwood_fringe` | 20.3% | `greenwood_hidden_cache` | **38.9%** |
| `deep_iron_pit` | 18.3% | `deep_iron_smuggler_stash` | 28.7% |
| `underground_river` | 9.3% | `underground_river_crystal_rift` | 13.9% |

- **P50/P90 首次发现**：`firstHiddenDiscoveryTurn P50/P90: 18/41`，`firstSecretZoneEntryTurn P50/P90: 40/46`。
- **Secret 与 cadence/reward overlap**：`sameZoneSecretVsCadenceMaxOverlap=0.400`、`sameZoneSecretVsRewardMaxOverlap=0.400`（≤ 0.5 floor）。

### 5.3 玩法判定：**刚够 / 分布失衡**

- **正向**：V3 PR-03 核心承诺全部达成；hidden loop 不再是 V3 前"假绿"；secret reward 单一权威锁死；local identity overlap ≤ 0.4 合格。
- **严重负向（分布失衡）**：
  - 52% 的 lead 集中在 `abyssal_temple`，但这个 zone 的 `secret entry share` 只有 18.5%，转化率严重偏低；
  - 反过来，`greenwood_fringe` 只占 20.3% 的 lead 却贡献 38.9% 的 secret entry。
  - **体感错位**：玩家在 `abyssal_temple` 频繁触发 lead 但进入 secret 的手感差；在 `greenwood_fringe` lead 少但 secret 发现率高。这种错位会让"hidden loop"的奖励反馈变得不可预期——要么在 abyssal_temple"看到线索但进不去"，要么在 greenwood"玩完半天没看到 lead 但 secret 却解开了"。
  - **原因推断**：`abyssal_temple` 是 objective-driven runtime，lead 触发密度高但 objective flow 不给玩家足够时间回探；`greenwood_fringe` 节奏稍慢、secret search 动作完成度更高。

### 5.4 关键问题

1. **【P1】organic hidden 分布需要做 per-zone conversion floor。** 目前 owner metric 只看 aggregate。应引入 `perZoneSecretConversionFloor.reportOnly`：要求每 zone `secretEntry / lead >= 25%`（目前 abyssal_temple 约为 `18.5/52 ≈ 35.6%` 实际是 OK，但这个分母混合了不同 seed 下的统计；需要单 zone 看）。更关键的是让 `zoneDiscoveryDistribution` 的最大值不能超过 40%（目前 52%）。
2. **【P1】abyssal_temple 的 lead 曝光需要下调或转化入口增加。** objective-driven runtime 带来的 lead 爆发需要对应的"secret entry 入口在 objective flow 中至少出现一次"的保证。如果 objective 赶路导致 secret 不可达，应在 objective 完成前保留一个 detour node。
3. **【P1】greenwood_fringe 的 lead 密度可以再升一点。** 让分布更均衡，同时保留 greenwood 的高 conversion 特性。

---

## § 6 · UI 反馈（frontstage cue / combat decision / modal / look）

### 6.1 设计承诺（docs/opt/ui-pr/README.md + V3 PR-05）

- 5 条 UIUX PR 全部收口；
- UIUX PR-05 战斗 3 层决策面 + telegraph 三位一体；
- UIUX PR-02..04 内 modal/look/item/status 收口；
- V3 PR-05 `FrontstageActionCueSnapshot` typed 合同落地，取代旧 `recentActionHighlights`。

### 6.2 当前实际体验（证据）

- **Client UI 文件结构完整**（`client/src/main/kotlin/com/ktome/client/ui/`）：`card/ combat/ creation/ hud/ inspect/ item/ layout/ panel/ settings/ state/ status/ talent/ token/`。
- **Combat decision surface**：`CombatDecisionFrame.kt / CombatDecisionPanel.kt / CombatDecisionValidationSurface.kt / CombatDecisionFeedbackKeys.kt` 全部存在。
- **Frontstage text**：`client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt` 已接 typed cue。
- **Deferred 收口**：
  - `ITEM_COMPARE` frame: PR-02 引入 → PR-03 收口；
  - `COMBAT_DECISION` frame: PR-02 引入 → PR-05 已收口为正式 phase 机；
  - `ExplainPane` sub-view: PR-02 引入 → PR-04 收口；
  - `BuildInfo.shortHash`: PR-01 直接落入；
  - `UiEmptyState` 统一模型: PR-02 → PR-03 收口。
- **Golden label 所有权**：按 `phase4-uiux-pr0N-*` 前缀划分（见 docs/opt/ui-pr/README.md §Golden Label 所有权）。
- **Cue 核心指标**：4 项全部 100%（见 § 2）。

### 6.3 玩法判定：**好玩**（局部 P2 检查）

- 决策面三层机制完成 + 合同 typed + golden 所有权清晰；
- Frontstage cue dedup / ttl / priority 合同 OK；
- PR-01..05 deferred 全部收口表上有闭环。

### 6.4 关键问题

1. **【P2】combat-decision-stub label 清理验证。** `docs/opt/ui-pr/README.md` §Golden Label 所有权 要求上线时清理任何 PR-02 combat-decision-stub label。需要在 Phase 4 收尾 PR 里跑一次 grep + 清理，并在 manual-record 中留凭证。
2. **【P2】ExplainPane 的使用频次未被 owner metric 锚定。** `docs/opt/ui-pr/README.md` 提到 `INSPECT + ?` 打开 explain pane，但目前没有 owner metric 证明玩家真的用到了 explain pane；这属于 UX 闭环的验证空白，可放到 Phase 5 再补。

---

## § 7 · 系统联动（pack overlay / elite mutation / boss variant / content pack）

### 7.1 设计承诺

- PR-08 content pack overlay loader；PR-09 sample pack；pack 用 ADD/MODIFY/REPLACE 三种 ops；gameVersionRange 对齐；sample pack 承担"示范正确扩展姿势"的责任；
- PR-06 terrain × elite × boss variant 三层联动；
- V3 PR-04 version discipline：runtime/build/sample pack 对齐到 Phase 4 口径。

### 7.2 当前实际体验（证据）

- **Version 对齐**（phase4Report + gradle.properties + sample manifest）：
  - project version: `0.4.0`；
  - generated `ktome-build.properties` + `DesktopLauncher` window title 对齐；
  - sample pack `gameVersionRange: ">=0.4.0 <0.5.0"`；
- **Content pack overlay**：加载器完整，manifest lint 通过；
- **Sample pack**（`examples/content-packs/sample.flooded_relics/manifest.yaml`）：version=1.0.0；多条 hidden event 与 secret zone 以 REPLACE ops 形式存在（偏离 ADD-first 示范）；
- **Elite mutation × boss variant**：
  - `terrainInteractionEncounterRate.aggregate=16.6% (+31.95%)`；
  - `variantTraceDivergenceRatio=100% (3/3)`；
  - `abyssal_eclipse` 的 terrain preference 已修正为 `void_mirror + phase_runner`（不再引用不可落地的 OIL）。

### 7.3 玩法判定：**刚够 / sample pack 示范伪完成**

- **正向**：overlay 合同落地，version discipline 对齐，3 个 variant 的 trace divergence 满分；
- **负向**：sample pack 作为唯一官方示范，承担"让下游 pack 作者学会正确姿势"的责任。目前它大量用 REPLACE ops 覆盖主源 hidden/secret，违背 Phase 4 "ADD-first, MODIFY next, REPLACE last" 的推荐顺序；下游 pack 作者 copy-paste 会直接落入错误模式。

### 7.4 关键问题

1. **【P2】sample pack 改为 ADD-first 示范。** 将 sample.flooded_relics 中的 REPLACE ops 重构为 ADD + MODIFY 组合；如确实需要 REPLACE 演示，在 sample 注释中明确标出"此条 REPLACE 仅用于演示 override 能力，实际 pack 应优先 ADD"。
2. **【P2】Overlay 在 sample pack 的 runtime 行为缺 owner metric。** 目前 `contentPackHarness` 只验证加载与 manifest 合规，未验证"pack 开启后，玩家长局实际感知到 pack 内容"的比率。可选 owner metric：`samplePackContentPlayerVisibilityRate`。

---

## § 体验总评（七维判定汇总）

| 维度 | 判定 | 关键阻塞点 |
| --- | --- | --- |
| 核心循环 | 刚够 | long_run smoke 样本路线同构；branch 采样不足 |
| 战斗 | 刚够 / 局部伪完成 | boss variant 无 `phaseOverrides`；variant 体感 = 权重微调 |
| 成长 | **不够好玩** | arcanist/rogue capstone adoption 0；templar+vanguard 终盘撞车 long_sword |
| 奖励 | 刚够 / ARMOR 伪完成 | milestone ARMOR 只占 7.8% |
| 探索 | 刚够 / 分布失衡 | abyssal_temple 独吞 52% lead，secret 分布反向错位 |
| UI 反馈 | **好玩** | PR-02 combat-decision-stub label 清理验证 |
| 系统联动 | 刚够 / sample pack 示范伪完成 | sample pack REPLACE ops 偏离 ADD-first |

**一个关键观察**：成长是七个维度中唯一达到"不够好玩"的维度。这与 Phase 4 的核心卖点——"重复游玩差异明显"——直接冲突；它决定了玩家会不会愿意再开第二局。其他维度的问题都不是阻塞级，但成长维度的 **arcanist/rogue capstone adoption=0** 与 **templar+vanguard long_sword 撞车** 是真正会让"重复游玩不值得"的结构性缺陷。

---

> Part 3 将把这些问题正式抽成"关键问题清单"，每条问题按 严重级别 / 现状证据 / 可能原因 / 影响面 / 伪完成与否 五段式呈现，并给出优先级排序。
