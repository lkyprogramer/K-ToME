> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md`

# Phase4 v4 PR-03 Build Identity Reward Adoption

**阶段**: `Phase 4 completion hardening / phase4-v4-pr03`
**优先级**: `P0`
**工作量**: `L`
**合并来源**: v4 P0-2、P1-1
**前置条件**: PR-00、PR-01、PR-02 完成；long-run 能记录职业树与铭文选择事件
**资源生成结论**: 不生成图片资源；不生成音频资源

## 0. 开发治理与验收矩阵

本 PR 是 [development-governance.md](./development-governance.md) 的 canary。执行前必须先通过 `acceptanceContractLint`，再进入 fast lane、owner producer、report gate 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../../verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../../rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `PR03-M01` | §5.6 / §9 `.reportOnly` cutover | `tools` | `acceptanceContractLint`, `Phase4AuthorityDocConsistencyTest` | `verifyOwner`, `reportPhase4Only`, `reportPhase4` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `N/A` |
| `PR03-M02` | §5.1 capstone x profession matrix | `game` | `SchemaV2LoaderTest`, `MilestoneRewardSelectorTest` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json` | `N/A` |
| `PR03-M03` | §5.2 adoption scoring formula | `game` | `MilestoneRewardSelectorTest` | `whiteBoxLoot`, `lootBalanceLab` | `tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json` | `N/A` |
| `PR03-M04` | §5.3 reward slot family balance | `game` / `tools` | `BuildIdentityAdoptionPolicyTest`, `MilestoneRewardSelectorTest` | `longRunLab`, `reportPhase4Only` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `N/A` |
| `PR03-M05` | §5.5 late quality floor / `basic_shield` ban | `game` | `MilestoneRewardSelectorTest` | `whiteBoxLoot`, `lootBalanceLab` | `tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json` | `N/A` |
| `PR03-M06` | §7 report fields and debug samples | `tools` | `ReportPhase4MaterializationTest`, `Phase4ReportRunnerTest` | `reportPhase4Only`, `reportPhase4` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `N/A` |
| `PR03-M07` | canary governance execution | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `skipped` |

### Gate Budget

预计重型任务：`whiteBoxLoot`、`lootBalanceLab`、`longRunLab`、`verifyOwner`、`reportPhase4Only`、`reportPhase4`、`verifyChanged`。触发原因是 PR-03 同时改 reward legality、build identity data、owner metric cutover 和 Phase4 report aggregation。耗时来源固定读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。

### Canonical Artifact

canonical owner evidence 固定为 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` 与本节列出的 repo-relative baseline / whitebox summary。raw long-run、loot debug 或 cache 诊断不得作为 fixture 合同；timestamp、kernel cache status、reused shard count、本机绝对路径不得进入 canonical artifact。

### Failure Rule

同一重型 gate 失败超过 2 次，或单轮验证超过 90 分钟，必须先补一条 fast check 或 PR-03 implementation review 复盘，再重跑 owner gate。当前实现任务按用户要求不进行人工白盒测试，因此 `whitebox=skipped`，替代证据为自动化 owner gate、canonical report 和 doc-vs-implementation self-audit；剩余风险是 packaged app 可见性未由人工白盒确认。

## 1. 玩家体验目标

本 PR 让奖励系统兑现构筑选择。玩家看到 capstone、非武器 payoff 和 milestone reward 时，必须经常产生“这件东西让我打法变了”的判断，而不是大多数奖励被忽略。

完成标准：

1. `professionCapstoneAdoptionFloor` 从 report-only approved debt 升级为 blocking。
2. `nonWeaponBuildPayoffFloor` 从 report-only approved debt 升级为 blocking。
3. 四个基础职业均满足 capstone adoption `>= 1`。
4. 四个基础职业均满足 non-weapon payoff `>= 1`。
5. `terminalWeaponBaseDiversity >= 4`。
6. `crossProfessionTopWeaponDominance <= 40%`。
7. milestone reward `adopted > notAdopted`。
8. milestone reward slot 任一 family 占比 `<= 50%`，`ARMOR >= 15%`、`WEAPON >= 20%`、`OFF_HAND >= 20%`、`ACCESSORY >= 10%`、`CONSUMABLE_OR_UTILITY >= 5%`。

## 2. 当前问题

1. `professionCapstoneAdoptionFloor.reportOnly=2/4 APPROVED_DEBT`。
2. `nonWeaponBuildPayoffFloor.reportOnly=2/4 APPROVED_DEBT`。
3. arcanist 与 rogue 的 capstone / non-weapon payoff 未达成。
4. templar 与 vanguard 终盘都回到 `long_sword` 语义。
5. arcanist 终局 ARMOR 会采用 vanguard capstone `unique_furnace_plate`。
6. `unique_deepcurrent_lens` 被普通 `emerald_charm` 压住。
7. `artifact_briar_heart` 多次出现，但 rogue 终局 OFF_HAND 仍保留 `basic_shield`。
8. milestone reward `notAdopted=57` 高于 `adopted=46`。
9. affix 头部集中在 `sentinel / of_strength / vampiric / of_life`。

## 3. 范围与非目标

### 3.1 范围

生产代码：

- `game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardSelector.kt`
- `game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardScoreBreakdown.kt`
- `game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardSelectionSupport.kt`
- `game/src/main/kotlin/com/ktome/game/AffixBuildTags.kt`
- `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4DomainArtifactRegistry.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerMetricTargets.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerBaselineRegistry.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt`

数据：

- `game/src/main/resources/data/build-identity/index.yaml`
- `game/src/main/resources/data/loot/index.yaml`
- `game/src/main/resources/data/items/index.yaml`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `tools/src/main/resources/phase4/aggregation-manifest.yaml`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json`
- `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json`
- `docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json`

测试与 harness：

- `game/src/test/kotlin/com/ktome/game/loot/MilestoneRewardSelectorTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/loot/WhiteBoxLootRunnerTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4ReportRunnerTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4MetricCatalogTest.kt`

### 3.2 非目标

1. 不新增 item id。
2. 不新增 special template。
3. 不新增 affix id。
4. 不新增 visual key。
5. 不新增 audio key。
6. 不通过提高掉落数量掩盖采用不足。
7. 不在 tools 中复制 reward authority。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

执行要求：

1. 本 PR 只重用现有 `phase4-pr05` item visual/icon 资源。
2. `unique_cinderveil_plate`、`unique_deepcurrent_lens`、`artifact_river_echo`、`artifact_briar_heart`、`artifact_eclipsed_relic`、`unique_vesper_chainmail`、`unique_voidlit_seal` 均已具备正式 visual/icon 覆盖。
3. 不新增 image plan、generation report、processing report。

### 4.2 音频资源

不生成新音频资源。

执行要求：

1. 本 PR 只重用现有 `phase4-pr05` item audio key。
2. 不新增 audio plan、generation report、processing report。
3. `audioLint` 必须证明所有被调权的 special item audio key 可解析。

## 5. 技术方案

### 5.1 Build identity 数据修正

当前 `game/src/main/resources/data/build-identity/index.yaml` 快照必须在 PR diff 中按下表核对：

| item id | 当前归属 | PR 动作 | 目标归属 |
| --- | --- | --- | --- |
| `artifact_forge_oath` | `vanguard` capstone | KEEP | `vanguard` |
| `unique_furnace_plate` | `vanguard` capstone | KEEP + non-vanguard penalty | `vanguard` |
| `unique_quenchbreaker_maul` | `vanguard` capstone | KEEP | `vanguard` |
| `artifact_river_echo` | `arcanist` capstone | KEEP | `arcanist` |
| `unique_deepcurrent_lens` | `arcanist` payoff | KEEP + OFF_HAND payoff boost | `arcanist` |
| `unique_cinderveil_plate` | arcanist-facing item | ADD to arcanist non-weapon payoff observation | `arcanist` |
| `artifact_briar_heart` | `rogue` payoff | KEEP + OFF_HAND payoff boost | `rogue` |
| `artifact_heartroot_gambit` | `rogue` payoff | KEEP | `rogue` |
| `unique_thornpath_crook` | `rogue` payoff | KEEP | `rogue` |
| `unique_briarbound_bow` | `rogue` payoff | KEEP | `rogue` |
| `artifact_eclipsed_relic` | `templar` payoff | KEEP | `templar` |
| `unique_vesper_chainmail` | `templar` payoff | KEEP | `templar` |
| `unique_voidlit_seal` | `templar` payoff | KEEP | `templar` |

`game/src/main/resources/data/build-identity/index.yaml` 必须改成：

| 职业 | capstone 口径 |
| --- | --- |
| `vanguard` | 保留 `artifact_forge_oath / unique_furnace_plate / unique_quenchbreaker_maul` |
| `arcanist` | 保留 `artifact_river_echo / unique_deepcurrent_lens`，并把已有 `unique_cinderveil_plate` 纳入 arcanist non-weapon payoff 观察面 |
| `rogue` | 保留 `artifact_briar_heart / artifact_heartroot_gambit / unique_thornpath_crook / unique_briarbound_bow` |
| `templar` | 保留 `artifact_eclipsed_relic / unique_vesper_chainmail / unique_voidlit_seal` |

固定规则：

1. `unique_furnace_plate` 对非 vanguard 的 adoption scoring 必须降权。
2. `unique_cinderveil_plate` 对 arcanist 的 ARMOR identity 必须升权。
3. `unique_deepcurrent_lens` 对 arcanist 的 OFF_HAND payoff 必须压过 `emerald_charm`。
4. `artifact_briar_heart` 对 rogue 的 OFF_HAND payoff 必须压过 `basic_shield`。
5. templar 终局身份必须优先落到 holy/protection 非武器 capstone，而不是 `long_sword`。

capstone × profession 权重矩阵：

| capstone item | vanguard | arcanist | rogue | templar |
| --- | --- | --- | --- | --- |
| `unique_furnace_plate` | `professionCapstoneBonus` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` |
| `artifact_forge_oath` | `professionCapstoneBonus` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` |
| `unique_quenchbreaker_maul` | `professionCapstoneBonus` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` |
| `artifact_river_echo` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` |
| `unique_cinderveil_plate` | `wrongProfessionCapstonePenalty` | `nonWeaponPayoffBonus + terminalIdentityBonus` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` |
| `unique_deepcurrent_lens` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus + nonWeaponPayoffBonus` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` |
| `artifact_briar_heart` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus + nonWeaponPayoffBonus` | `wrongProfessionCapstonePenalty` |
| `artifact_heartroot_gambit` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus` | `wrongProfessionCapstonePenalty` |
| `unique_thornpath_crook` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus` | `wrongProfessionCapstonePenalty` |
| `unique_briarbound_bow` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus` | `wrongProfessionCapstonePenalty` |
| `artifact_eclipsed_relic` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus + nonWeaponPayoffBonus` |
| `unique_vesper_chainmail` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus + nonWeaponPayoffBonus` |
| `unique_voidlit_seal` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `wrongProfessionCapstonePenalty` | `professionCapstoneBonus + nonWeaponPayoffBonus` |

### 5.2 Adoption scoring

`MilestoneRewardSelector` 必须增加解释性 breakdown 字段：

| 字段 | 作用 |
| --- | --- |
| `professionCapstoneBonus` | 职业 capstone 匹配加成 |
| `nonWeaponPayoffBonus` | 非武器 payoff 加成 |
| `wrongProfessionCapstonePenalty` | 其他职业 capstone 降权 |
| `slotRotationBonus` | 最近 milestone 未出现 slot 加成 |
| `duplicateSlotPenalty` | 连续重复 slot 降权 |
| `terminalIdentityBonus` | 终局身份缺失时的补偿 |

评分规则：

1. artifact + profession capstone 的总加成必须大于普通 base item。
2. non-weapon capstone 的加成必须足以进入终局采用候选。
3. wrong-profession capstone penalty 必须阻止 arcanist 穿 vanguard capstone 成为默认终局。
4. slot rotation 不得覆盖职业匹配；它只在同等评分区间内生效。

ScoreFormula 固定为：

```text
finalScore =
  baseScore
  + professionCapstoneBonus
  + nonWeaponPayoffBonus
  + slotRotationBonus
  + terminalIdentityBonus
  - wrongProfessionCapstonePenalty
  - duplicateSlotPenalty
```

`baseScore` 固定为 `item.baseBudgetScore + rarityScore + slotBaseValue + sourceTierScore`，只反映 item 本体、稀有度、slot 基础价值和来源层级，不读取 profession、build tag、terminal identity、slot history 或 affix history。

数值合同：

1. `wrongProfessionCapstonePenalty = ceil(baseScore * 0.8)`。
2. `professionCapstoneBonus >= ceil(baseScore * 0.6)`。
3. `nonWeaponPayoffBonus >= ceil(baseScore * 0.45)`。
4. `terminalIdentityBonus` 只在当前职业 `equippedItems + adoptedMilestoneItems` 中没有本职业 capstone 或 non-weapon payoff 时生效；候选必须属于当前职业，且本次 terminal reward 只触发一次。
5. 非 capstone 的 wrong profession 匹配不触发 penalty，只是不获得 profession bonus。
6. `slotRotationBonus` 最大值不得超过 `nonWeaponPayoffBonus * 0.5`，防止 slot 轮转压过构筑身份。
7. `duplicateSlotPenalty = ceil(baseScore * 0.25)`，同一 milestone source 连续两次给出同一 slot family 时生效。
8. `lateCommonPenalty = ceil(baseScore * 0.35)`，所有职业 level `>= 5` 的 COMMON milestone candidate 生效；rogue level `>= 5` 的 COMMON OFF_HAND 在 penalty 后额外不进入 shortlist。
9. soft history window 固定为最近 `3` 次 milestone reward decision。
10. 所有正向 bonus 之和封顶为 `ceil(baseScore * 1.2)`，封顶前后的 breakdown 都必须写入 `rewardScoreBreakdownSamples`。
11. finalScore 相同时，tie-break 顺序固定为：当前职业 capstone 匹配、non-weapon payoff、较高 rarity、最近 `3` 次未出现的 slot family、`itemId` 字典序升序。

### 5.3 Reward slot balance

新增指标：

| 指标 | 阈值 |
| --- | ---: |
| `milestoneRewardSlotBalance.ARMOR` | `>= 15%` |
| `milestoneRewardSlotBalance.WEAPON` | `>= 20%` |
| `milestoneRewardSlotBalance.OFF_HAND` | `>= 20%` |
| `milestoneRewardSlotBalance.ACCESSORY` | `>= 10%` |
| `milestoneRewardSlotBalance.CONSUMABLE_OR_UTILITY` | `>= 5%` |
| `milestoneRewardSlotBalance.maxSlotShare` | `<= 50%` |

实现要求：

1. 同一 source 连续两次不得给同一 slot。
2. 跨 source 只写入最近 `3` 次 soft history，用作 `slotRotationBonus` 与 `duplicateSlotPenalty` 输入，不做硬 ban。
3. ARMOR 与 OFF_HAND 的 unique/artifact payoff 必须进入 milestone 候选池。
4. slot family 聚合固定为 `WEAPON / OFF_HAND / ARMOR / ACCESSORY / CONSUMABLE_OR_UTILITY`；report 必须输出原始 slot 与 family 双层分布。

### 5.4 Affix 头部集中修正

固定调整：

1. 下调 `sentinel`、`of_strength`、`vampiric`、`of_life` 在通用池的权重。
2. 提升 `of_smite` 在 templar/holy/protection reward 中的最低出现面。
3. 提升 `of_shadow` 在 rogue/shadow/marked reward 中的最低出现面。
4. 提升 `of_piercing` 在 rogue/precision/ranged reward 中的最低出现面。
5. 新增 `topFiveAffixExposureShare`，阈值 `<= 40%`。

affix 权重总表校准要求：

1. `lootBalanceLab` 必须输出 full affix distribution diff，包含调整前 share、调整后 share、delta、样本数。
2. `topFiveAffixExposureShare` 取调整后分布中前五 affix share 之和，不固定特定 affix id。
3. `sentinel / of_strength / vampiric / of_life` 调整后仍可掉落，但任一单项 share 不得超过 `15%`。
4. `sentinel + of_strength + vampiric + of_life` 调整后合计 share 不得超过 `30%`。
5. `topFiveAffixExposureShare <= 40%` 与单项 `<= 15%` 同时成立；任一单项不超标但前五合计超标仍然 fail owner gate。
6. `of_smite / of_shadow / of_piercing` 的 floor 只在匹配职业或 build tag reward 中生效，不污染全局通用池。

### 5.5 后期 quality floor

为修复 rogue 终局 OFF_HAND 被普通 `basic_shield` 压住的问题，本 PR 固定增加 milestone selector quality floor：

1. 所有职业 level `>= 5` 的 milestone COMMON candidate 均应用与 `duplicateSlotPenalty` 同级的 `lateCommonPenalty`。
2. `rogue` level `>= 5` 的 milestone OFF_HAND candidate pool 中，`rarity=COMMON` 的 OFF_HAND 在 `lateCommonPenalty` 之后额外升级为 selected shortlist 排除。
3. 自然掉落仍保留 `basic_shield`，本规则只作用于 milestone selector。
4. `rewardScoreBreakdownSamples` 必须包含至少 1 条 `basic_shield` 被 quality floor 排除或降权的样本。
5. `rewardScoreBreakdownSamples` 必须包含至少 1 条 rogue level `>= 5` 的非 OFF_HAND COMMON candidate 被 `lateCommonPenalty` 降权的样本。

### 5.6 Owner metric contract cutover

本 PR 必须一次性完成两个 report-only floor 的 blocking 切换：

| 旧 metric id | 新 metric id | 处理 |
| --- | --- | --- |
| `professionCapstoneAdoptionFloor.reportOnly` | `professionCapstoneAdoptionFloor` | 从 `Phase4MetricCatalog`、`Phase4AggregationInputRunner`、`Phase4ReportRunner`、`ReportPhase4Runner` 的 canonical owner path 删除旧 id，新增 blocking id |
| `nonWeaponBuildPayoffFloor.reportOnly` | `nonWeaponBuildPayoffFloor` | 从 `Phase4MetricCatalog`、`Phase4AggregationInputRunner`、`Phase4ReportRunner`、`ReportPhase4Runner` 的 canonical owner path 删除旧 id，新增 blocking id |

执行要求：

1. `Phase4OwnerMetricTargets` 写入 blocking range。
2. `Phase4OwnerBaselineRegistry` 继续以 terminal build owner baseline 为唯一 baseline source。
3. `tools/src/main/resources/phase4/aggregation-manifest.yaml` 保持 `longRunLab` 为 producer；不得新增第二 producer。
4. `docs/phase4/2026-03-13-phase4-verification-checklist.md` 同步移除两个 `.reportOnly` id，写入 blocking id 与 fail semantics。
5. `reportPhase4Only`、`reportPhase4`、owner aggregation JSON、Markdown report 的 metric id、status、fail semantics 必须完全一致。
6. 旧 `.reportOnly` id 不保留兼容 alias；实现完成后所有 canonical owner evidence 只出现新 blocking id。

## 6. 测试与自证

### 6.1 必测行为

1. `professionCapstoneAdoptionFloor.reportOnly` 与 `nonWeaponBuildPayoffFloor.reportOnly` 从 canonical owner evidence 删除。
2. `professionCapstoneAdoptionFloor` 与 `nonWeaponBuildPayoffFloor` 作为 blocking owner metrics 进入 `verifyOwner`。
3. `arcanist / rogue / templar / vanguard` 均满足 capstone adoption `>= 1`。
4. `arcanist / rogue / templar / vanguard` 均满足 non-weapon payoff `>= 1`。
5. `wrongProfessionCapstonePenalty` 阻止 arcanist 默认采用 `unique_furnace_plate`。
6. `unique_deepcurrent_lens`、`artifact_briar_heart`、templar holy/protection 非武器 payoff 能进入终局采用样本。
7. milestone reward `adopted > notAdopted`，slot balance 满足 `WEAPON / OFF_HAND / ARMOR / ACCESSORY / CONSUMABLE_OR_UTILITY` family 阈值。
8. `rewardScoreBreakdownSamples` 记录 selected、rejected、wrong-profession penalty、non-weapon payoff 压过普通 base item、`basic_shield` 被 quality floor 排除或降权的解释样本。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint :game:test :tools:whiteBoxLoot :tools:lootBalanceLab longRunLab verifyOwner reportPhase4Only reportPhase4 audioLint contractLint maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr03
```

必须保留以下自证产物：

1. `build/reports/tests/` 中 `MilestoneRewardSelectorTest`、`WhiteBoxLootRunnerTest`、`Phase4ReportRunnerTest`、`Phase4MetricCatalogTest` 的结果。
2. `tools/build/reports/` 中 `whiteBoxLoot`、`lootBalanceLab`、`longRunLab`、`reportPhase4Only` producer 产物。
3. `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` canonical report 产物。
4. `build/reports/verification/` 中 `verifyOwner`、`verifyChanged`、`maintainabilityLint` 产物。
5. `build/whitebox/phase4-v4-pr03/evidence/` 中人工白盒截图、日志、manual record。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

已有游戏 Validation Mode 改造要求：

1. 本 PR 必须接入 PR-00 的 `PHASE4_V4_FAST` section，scenario id 固定为 `phase4-v4-pr03`。
2. `prepare-primary-scene` 必须在现有游戏内 validation session 中生成 `arcanist` milestone reward 展示面，reward cards 包含 capstone 或 non-weapon payoff，并展示 slot、profession identity、score reason。
3. `prepare-secondary-scene` 必须生成 `rogue` OFF_HAND reward 展示面，包含 `artifact_briar_heart` 或同类 rogue OFF_HAND payoff。
4. `show-evidence-summary` 必须展示 owner metric cutover 摘要，确认 `professionCapstoneAdoptionFloor.reportOnly` 和 `nonWeaponBuildPayoffFloor.reportOnly` 不在 approved debt 列表中。
5. reward 生成、采用结果、装备面变化必须由 game 层 validation action materialize，不得由 client 写死 reward card。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr03`
4. preset：`LOOT_LAB`
5. seed：`2026042433`
6. runtime home：`build/whitebox/phase4-v4-pr03/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr03/evidence`
8. manual record：`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr03-build-identity-reward-adoption.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr03
```

2. 执行 `build/whitebox/phase4-v4-pr03/launch-packaged-app.sh` 启动 packaged app，Computer Use 目标 app 固定为 `com.ktome.client`。
3. 按 `build/whitebox/phase4-v4-pr03/cua-runbook.md` 打开 validation overlay，执行 `PHASE4_V4_FAST / prepare-primary-scene`。
4. 截图记录 reward cards 中出现 arcanist capstone 或 non-weapon payoff，且卡片展示 slot、profession identity、score reason 或等价 explanation line。
5. 选择 arcanist 非武器 payoff，确认装备面/角色面体现 build identity，而不是默认采用 vanguard armor。
6. 执行 `PHASE4_V4_FAST / prepare-secondary-scene`，确认 `artifact_briar_heart` 或同类 rogue OFF_HAND payoff 能被展示并采用。
7. 执行 `PHASE4_V4_FAST / show-evidence-summary`，截图记录 capstone/non-weapon floor 不再显示 approved debt。
8. 保存证据：
    - `phase4-v4-pr03-arcanist-reward-card.png`
    - `phase4-v4-pr03-arcanist-adopted-nonweapon.png`
    - `phase4-v4-pr03-rogue-offhand-payoff.png`
    - `phase4-v4-pr03-report-no-approved-debt.png`
    - `phase4-v4-pr03-app.log`

通过标准：

1. 玩家能从 reward card 看出奖励为何适合当前职业与构筑。
2. 至少两个职业的 reward 采用路径在 packaged app 中可见。
3. report UI 或 validation summary 不再把 capstone/non-weapon floor 显示为 approved debt。
4. manual record 写明 packaged app 路径、runtime home、seed、输入序列、截图路径和结论。

### 6.4 统一验证框架关系

本 PR 是 owner metric cutover PR。`verifyOwner`、`reportPhase4Only`、`reportPhase4` 是 blocking 证据；人工白盒只证明玩家能感知 reward explanation 与 build identity payoff，不能替代 owner baseline、metric catalog、aggregation manifest 和 report runner 的一致性测试。

### 6.5 玩家体验 Golden Path

1. 玩家用 `arcanist` 到达 milestone reward 时，reward card 必须展示 arcanist non-weapon payoff 原因，且 `unique_furnace_plate` 不得成为默认推荐。
2. 玩家采用 arcanist 非武器 payoff 后，装备面必须体现 build identity，而不是只显示更高通用数值。
3. 玩家用 `rogue` 到达 OFF_HAND milestone 时，`artifact_briar_heart` 或同类 rogue OFF_HAND payoff 必须压过 `basic_shield`。
4. 玩家用 `templar` 到达终局 reward 时，holy/protection 非武器 payoff 必须成为可见身份，不再回到 `long_sword` 语义。
5. 玩家查看 reward explanation 时，必须看到 profession bonus、non-weapon payoff、wrong-profession penalty 或 slot reason 中至少两类解释。

## 7. Report 与验收指标

新增 blocking 指标：

| 指标 | 阈值 | metricKind | producer | ownerBaseline | failSemantics |
| --- | ---: | --- | --- | --- | --- |
| `professionCapstoneAdoptionFloor` | `4/4` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json` | `fail owner gate` |
| `nonWeaponBuildPayoffFloor` | `4/4` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json` | `fail owner gate` |
| `terminalWeaponBaseDiversity` | `>= 4` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json` | `fail owner gate` |
| `crossProfessionTopWeaponDominance` | `<= 40%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json` | `fail owner gate` |
| `milestoneRewardAdoptionDelta` | `adopted > notAdopted` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `fail owner gate` |
| `milestoneRewardSlotBalance.maxSlotShare` | `<= 50%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `fail owner gate` |
| `milestoneRewardSlotBalance.WEAPON` | `>= 20%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `fail owner gate` |
| `milestoneRewardSlotBalance.OFF_HAND` | `>= 20%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `fail owner gate` |
| `milestoneRewardSlotBalance.ARMOR` | `>= 15%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `fail owner gate` |
| `milestoneRewardSlotBalance.ACCESSORY` | `>= 10%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `fail owner gate` |
| `milestoneRewardSlotBalance.CONSUMABLE_OR_UTILITY` | `>= 5%` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json` | `fail owner gate` |
| `topFiveAffixExposureShare` | `<= 40%` | `blockingOwner` | `whiteBoxLoot` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json` | `fail owner gate` |

新增 supporting 指标：

| 指标 | 用途 | metricKind | producer | ownerBaseline | failSemantics |
| --- | --- | --- | --- | --- | --- |
| `wrongProfessionCapstoneAdoptionCount` | 确认跨职业 capstone 没有污染终局 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `capstoneAdoptionBySlot` | 区分 weapon 与 non-weapon payoff | `supporting` | `longRunLab` | `N/A` | `display only` |
| `professionTerminalIdentityItemIds` | 展示每职业终局身份 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `rewardScoreBreakdownSamples` | 让失败可解释 | `debugSample` | `longRunLab` | `N/A` | `display only` |

`rewardScoreBreakdownSamples` 必须至少包含：

1. selected candidate 的 profession、slot、source、score breakdown。
2. top rejected capstone candidates 与 rejected reason。
3. wrong-profession capstone penalty 生效样本。
4. non-weapon payoff 压过普通 base item 的样本。
5. 每个 terminal run 至少 `20` 条 sampled reward decisions，并按 profession 分组输出。
6. `capstoneAdoptionBySlot` 统计 adopted capstone item 的 slot family；`milestoneRewardSlotBalance` 统计 milestone offer 或 selected reward 的 slot family，两者不得共用分母。

owner 接线顺序固定为：

1. capstone identity group：`professionCapstoneAdoptionFloor`、`nonWeaponBuildPayoffFloor`、`terminalWeaponBaseDiversity`、`crossProfessionTopWeaponDominance`。
2. slot balance group：`milestoneRewardAdoptionDelta` 与全部 `milestoneRewardSlotBalance.*`。
3. affix distribution group：`topFiveAffixExposureShare` 与 affix distribution diff。
4. 三组 baseline 分文件存放；PR description 必须逐组列出 metric diff，不得只贴一个合并后的总 baseline。
5. 三组 baseline 必须全部登记到 `Phase4OwnerBaselineRegistry` 与 `tools/src/main/resources/phase4/aggregation-manifest.yaml`。

Rollback boundary 固定为：

1. capstone identity group 可单独回退，不回退 milestone slot balance group。
2. milestone slot balance group 可单独回退，不回退 capstone report-only cutover。
3. `topFiveAffixExposureShare` 归 `whiteBoxLoot` owner，可独立回退到原 loot-local baseline。
4. 任一 group 回退时必须同步回退 `Phase4MetricCatalog`、`Phase4OwnerMetricTargets`、`Phase4OwnerBaselineRegistry` 和 aggregation manifest，不保留半接线字段。

## 8. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint :game:test :tools:whiteBoxLoot :tools:lootBalanceLab longRunLab verifyOwner reportPhase4Only reportPhase4 audioLint contractLint maintainabilityLint verifyChanged
```

## 9. 完成定义

1. `phase4Report` 不再显示 capstone/non-weapon approved debt。
2. `professionCapstoneAdoptionFloor.reportOnly` 与 `nonWeaponBuildPayoffFloor.reportOnly` 从 canonical owner evidence 中删除。
3. `professionCapstoneAdoptionFloor` 与 `nonWeaponBuildPayoffFloor` 作为 blocking owner metric 进入 `verifyOwner`。
4. arcanist、rogue、templar、vanguard 均有 capstone adoption。
5. arcanist、rogue、templar、vanguard 均有 non-weapon payoff。
6. arcanist 终局不再采用 `unique_furnace_plate` 作为默认身份。
7. rogue 终局 OFF_HAND 能采用 `artifact_briar_heart`。
8. templar 与 vanguard 不再共同以 `long_sword` 作为终盘主身份。
9. milestone reward adopted 数量高于 notAdopted。
10. `verifyOwner + reportPhase4Only + reportPhase4 + verifyChanged` 同批通过。
11. 没有新增图片计划文件。
12. 没有新增音频计划文件。
