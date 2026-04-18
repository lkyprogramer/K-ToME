# Phase 4 Build Identity End-to-End Hardening 落地后评审

**评审范围**: 对照 `docs/opt/2026-04-17-phase4-build-identity-end-to-end-hardening-and-fast-fail-plan.md` 中 `PR-01 ~ PR-04` 四组合同
**评审定位**: Roguelike / 类 ToME 设计总监 + 系统策划 + 玩法体验审查
**仓库基准**: 当前分支 `codex/phase4-phase4-build-identity-hardening`，工作区最近有改动的文件列表
**评审日期**: `2026-04-18`

---

## 0. 总体结论 (TL;DR)

| 维度 | 落地情况 | 与计划偏差 | 风险级别 |
| --- | --- | --- | --- |
| PR-01 Reward Routing Contract Freeze | `✅ 基本达成` | `小` | `L` |
| PR-02 Shared Build Identity Catalog | `✅ 基本达成` | `中（缺少独立策略聚合）` | `M` |
| PR-03 Milestone Selector Extraction | `✅ 基本达成` | `中（build-identity 偏好未回灌 selector）` | `M` |
| PR-04 Fast-Fail Diagnostics & Staged Gate | `✅ 基本达成` | `小（一项指标命名与语义自相矛盾）` | `L` |
| Contract 冻结项（第 3 节禁区） | `✅ 全部遵守` | `无` | `L` |
| 验收阶梯（第 7 节红绿顺序） | `✅ 已写入 checklist` | `无` | `L` |

主干结论：**authority 侧的 PR-01 / PR-02 / PR-04 基本按合同落地**；PR-03 抽取也完成，但 **build-identity 的 `preferredRewardSources` / `preferredReplacementSlots` 在 selector 与 adoption 层均未被消费**，属于"authority 已立但下游未接"的断链，这正是计划最担心的同类漂移。

另外计划 5.3 明确建议的 `BuildIdentityAdoptionPolicy.kt` 独立策略文件并未落地；当前 adoption 偏好以多个 `SmokeBot` 私有 score 函数分散实现，仍然违背了"adoption 是 shared contract"这一原则（计划 2.4 节）。下次同类 "看见但不穿" 问题还是会分散定位。

---

## 1. PR-01 · Reward Routing Contract Freeze

### 1.1 合同要求回顾

1. 新增 `data/reward-routing/index.yaml` 作为 support/cache/finale 路由的 authority
2. `FoundationGameSession` 不再硬编码 zone-specific `when`
3. `direct drop / route / cache / support / boss / hidden reward` 的 capstone cue parity 共用同一套
4. 缺路由必须 fail fast

### 1.2 已落地证据

- `game/src/main/resources/data/reward-routing/index.yaml` 落盘，覆盖 `GROUND_CACHE` × 6 + `SUPPORT_GRANT` × 10 共 16 条。
- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt:586-604` 定义 `RewardRoutingEntrySchemaV1`，loader 对同 `(zoneId, interactableId, grantMode)` 重复直接 `require` 失败。
- `FoundationGameSession.kt:5019-5039` 的 `rewardSpecForInteractable` 改为 authority lookup，`matches.size != 1` 直接 `require` 失败，符合计划 8.4.2。
- `FoundationGameSession.kt:9486 / 9529` 只保留两处 grant 入口，分别用 `RewardRoutingGrantMode.GROUND_CACHE / SUPPORT_GRANT`，runtime 不再承担路由语义决策。
- `game/src/test/kotlin/com/ktome/game/data/RewardRoutingContractTest.kt` 以 `expectedRoutingKeys()` 明示锁定全部 16 条 key，并对 `river_ferry_anchor / heart_ward_focus` 显式断言 profile 链。

### 1.3 残留偏差

| 偏差 | 位置 | 级别 | 解读 |
| --- | --- | --- | --- |
| `profileIds: []` 的 tutorial 条目 | `reward-routing/index.yaml:1-7` `shattered_outpost/supply_crate` | 小 | 空 profile 表示"走 fallbackBaseId"，`groundRewardItemFor` 的空列表分支明确走 `officialRewardItem`；但 contract test 未显式断言"空 profile 合法"，后续若强化成"非空必须非空"会变 breaking change。建议在 schema 或 contract test 里显式注释"空 profileIds 代表官方教学掉落，禁止用于非教学 zone"。 |
| capstone cue parity 未集中锁定 | `FoundationGameSession.kt` 多处 `recordMilestoneReward / logCacheRewardGrant` | 中 | 计划 8.4.3 要求"direct drop / route / cache / support / boss / hidden reward 的 capstone cue 共用同一套 (isCapstone / isNonWeaponCapstone / profession anchor message key)"。目前 cue 通过 `recordMilestoneReward` 落到 reward record，但仓库内没有单独的 `CapstoneCueParityTest` 来锁定"同一 item 在不同 reward source 下必须给出完全一致的 cue payload"。cue parity 回归只能靠 longRunLab 间接发现，违反计划 7.2 的红绿顺序。 |

### 1.4 优先修复建议

1. 新增 `game/src/test/kotlin/com/ktome/game/FoundationGameSessionCueParityTest.kt`：用同一个 profession capstone base，通过 direct drop / cache / support / route / boss 五条路径触发 `recordMilestoneReward`，断言 cue payload 的 `isCapstone / isNonWeaponCapstone / professionAnchorKey` 等字段完全一致。此项属于 PR-01 的退出标准（§8.9 第 3 条）未闭合的漏项。
2. 将 `profileIds: []` 的合法性显式写入 `RewardRoutingEntrySchemaV1` 注释，并在 contract test 中加一句"空 profile 仅允许 tutorial zone `shattered_outpost/supply_crate`"。

---

## 2. PR-02 · Shared Build Identity Catalog

### 2.1 合同要求回顾

1. 新增 `data/build-identity/index.yaml`，每 profession 含 7 项 identity facts
2. `SmokeBot / LongRunLabFullTest / tools report` 等统一切换至 catalog
3. 不再存在第二份 `PROFESSION_CAPSTONE_ITEM_IDS`
4. 计划 §5.3 建议新增 `BuildIdentityAdoptionPolicy.kt` 作为 adoption 策略聚合

### 2.2 已落地证据

- `game/src/main/resources/data/build-identity/index.yaml` 落盘，四个 base profession 均含 `capstoneBaseIds / nonWeaponCapstoneBaseIds / preferredRewardSources / preferredReplacementSlots / terminalIdentityTags / reportOnlyFloors(seenMinCount, adoptionMinCount, nonWeaponMinCount)`。
- `game/src/main/kotlin/com/ktome/game/loot/FoundationProfessionCapstoneCatalog.kt` 作为 runtime authority：以 `DataLoader().loadSchemaCatalog().buildIdentities` 为唯一真源，并导出 `foundationBuildIdentityByProfessionId / foundationBuildIdentityForResourceType / foundationProfessionCapstoneBaseIdsByProfessionId`。
- `SmokeBot.kt:326` 的 `offHandSlotScore` 通过 `foundationBuildIdentityForResourceType(observation.playerResource.typeId)` 读取 `nonWeaponCapstoneBaseIds / preferredReplacementSlots`，不再硬编码 OFF_HAND item 名单。
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt:1230 / 1233 / 1241 / 1296 / 1316` 全部从 `foundationBuildIdentityByProfessionId` 派生 `phase4FoundationProfessionIds` 与 `reportOnlyFloors`。
- `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt:1217` 的 `PROFESSION_CAPSTONE_ITEM_IDS = foundationProfessionCapstoneBaseIdsByProfessionId`，此处的常量是 authority 派生视图，不是第二真源。

### 2.3 残留偏差

| 偏差 | 位置 | 级别 | 解读 |
| --- | --- | --- | --- |
| `BuildIdentityAdoptionPolicy.kt` 未落地 | 计划 §5.3 建议的 `game/src/main/kotlin/com/ktome/game/harness/BuildIdentityAdoptionPolicy.kt` 不存在 | 中 | `SmokeBot` 里 `offHandSlotScore / preferredWeaponScore / emptySlotEquipThreshold / wantsToEquipBetterItem` 等多个 private function 各自消费 identity 字段。一旦 identity schema 再扩（例如 preferred reward source），每个 scoring 入口都要改一次，等于把 catalog 扩散成"carry by callsite"。计划 §2.4 的核心诉求是"adopt 行为即契约"，应把 adoption 判定聚合到一个 policy 对象。 |
| `preferredRewardSources` / `preferredReplacementSlots` 在 selector 未被消费 | `MilestoneRewardSelector.kt:1-277` 无任何 `preferredRewardSources` 引用；`FoundationGameSession.kt:4391` 的 `milestoneReplacementSlotPriority()` 走硬编码 `MILESTONE_REPLACEMENT_SLOT_PRIORITY` | 中 | catalog 中定义的 `preferredRewardSources=[CACHE, BOSS]` 等字段目前处于"authority 已立、下游未接"状态，等于数据 schema 的死代码。下一次希望"给 rogue 的 capstone 在 cache 上加一点权重偏好" 时，仍然要同时改 yaml + selector；与计划 §1 "单一 authority"目标背离。 |
| `terminalIdentityTags` 的消费点仅在报表 | 仅 `Phase4AggregationInputRunner` 用于渲染 | 小 | runtime scoring 没有用，属于"只给 report 看的 authority"。短期可接受，但 identity schema 内 7 个字段里有 2 个是半死状态，建议在 schema 注释里区分"runtime-authoritative" vs "report-only"。 |

### 2.4 偏差幅度

| 合同条款 | 完成度 |
| --- | --- |
| 新增 catalog 数据 | `100%` |
| Schema loader + test 锁定 | `100%` |
| 共享消费点 (LootBaseSelectionContext / SmokeBot / LongRunLab / Phase4AggregationInputRunner / Phase4ReportRunner / doc consistency test) | `~85%`：主要消费点接通，但 SmokeBot 侧仍然是"per-callsite 读 identity"，没有形成 `BuildIdentityAdoptionPolicy` |
| selector 侧消费 identity | `~30%`：仅通过 `capstoneBaseIds / nonWeaponCapstoneBaseIds` 间接起效，`preferredRewardSources / preferredReplacementSlots` 未接 |

### 2.5 优先修复建议

1. **新增 `BuildIdentityAdoptionPolicy.kt`**（高优先）：
   - 输入 `FoundationProfessionBuildIdentity + RunObservation + CandidateItem`，输出 `AdoptionDecision`（含 `preferThisOverEquipped / equipScoreBonus`）；
   - 把 `SmokeBot.offHandSlotScore / preferredWeaponScore` 的 identity 判定搬进来，SmokeBot 只做 observation 适配；
   - 这样后续 `preferredReplacementSlots`、`terminalIdentityTags` 的调参不会再扩散到 harness 私有 function。
2. **selector 接通 `preferredRewardSources` / `preferredReplacementSlots`**（高优先）：
   - 在 `MilestoneRewardScoreBreakdown` 中新增 `preferredRewardSourceScore`（例如 `rewardSource in identity.preferredRewardSources ? +20 : 0`）；
   - 把 `milestoneReplacementSlotPriority()` 改成 `identity.preferredReplacementSlots.takeIf { it.isNotEmpty() } ?: MILESTONE_REPLACEMENT_SLOT_PRIORITY`；
   - 补一个 selector unit test：两个同分 capstone candidate，其 reward source 分别在 preferred / non-preferred，断言 preferred 侧胜出。
3. **schema 注释区分 runtime vs report-only 字段**（低优先）：在 `SchemaModels.kt` 的 `BuildIdentityEntry` 上方加注释，列清楚哪些字段进 runtime scoring，哪些只给 report 消费。

---

## 3. PR-03 · Milestone Selector Extraction & Score Breakdown

### 3.1 合同要求回顾

1. 新增 `MilestoneRewardSelector / MilestoneRewardCandidate / MilestoneRewardScoreBreakdown / MilestoneRewardRejectionReason` 四文件
2. `standard / special-linked` 走同一条 selector，但 legality 明确区分
3. replacement 先 exact-profession capstone，再回退 slot priority
4. selector 必须有 diagnostic 输出

### 3.2 已落地证据

- 四个文件均存在（`game/src/main/kotlin/com/ktome/game/loot/Milestone*.kt`），且分层清晰：`Candidate` 携带 `legal + rejectionReason + scoreBreakdown + specialLinkedBase + exactProfessionCapstone + nonWeaponCapstone`。
- `MilestoneRewardSelector.kt:195-217` 将 special-linked base 的 legality 路径分开：只看 `template.allowedSourceTiers / template.allowedZones`，绕开普通 `dropFloors`，对齐计划 §10.5 第 2 条。
- `MilestoneRewardSelector.kt:90-98` 的 `selectReplacementSlot` 先按 `exactProfessionCapstone → nonWeaponCapstone → score → priorityIndex` 排序，再回退 `minByOrNull(priorityIndex)`，与计划 §10.6 一致。
- `FoundationGameSession.kt:4277-4326 / 4353-4389` 的 `milestoneRewardItemFromProfiles` 把所有 standard/boss/route/cache/support 五类 milestone 路径统一收到 `MilestoneRewardSelector`（`includeSpecialLinkedBaseIds = true`）。
- `game/src/test/kotlin/com/ktome/game/loot/MilestoneRewardSelectorTest.kt`（`19.9KB`）存在，承担 selector 级单测职责。

### 3.3 残留偏差

| 偏差 | 位置 | 级别 | 解读 |
| --- | --- | --- | --- |
| selector 不消费 `preferredRewardSources` | 见 §2.3 | 中 | 与 PR-02 绑定，需一起修 |
| `milestoneReplacementSlotPriority()` 是硬编码而非 identity 驱动 | `FoundationGameSession.kt:4391-4396` | 中 | 只在 `FOUNDATION_AFFIX_WEAPON_PRIORITY_PROFESSION_IDS` 做特判；`preferredReplacementSlots` 未进入 selector 请求 |
| `allCandidateBaseIds / standardCandidateBaseIds` 分叉仍保留 | `LootProfileCandidatePoolResolver.kt:100 / FoundationGameSession.kt:11138-11181` | 小 | 分叉是 non-milestone 走 `lootWeightedBaseCandidatesFromPools(includeSpecialLinkedBaseIds=false)`（例如 shop 库存 / 掉落包），milestone 路径已经用 `true`。虽然没违反计划，但两套语义并存仍容易引新 bug。建议增加一个对比测试：在同一 pool 下，milestone selector 的 pool vs non-milestone loot 的 pool 断言差异只来自 `specialLinkedBaseIds`。 |
| diagnostic 输出未走 `whiteBoxLoot` 直接可见 | `MilestoneRewardSelectionResult.rankedCandidates` 只在 runtime 存活 | 小 | 计划 §10.7 要求 diagnostic 供 `unit test / white-box reward coverage / phase4 report debug note` 三处。目前仅 unit test + report debug artifact 可见；`whiteBoxLoot` 内部没有消费 selector 的 `rejectionReason` 来证明"某个期望的 capstone 为什么在某 source 被拒"。下次如果再遇到 "capstone 可见但 0 adopted"，仍只能靠大 JSON grep。 |

### 3.4 偏差幅度

| 合同条款 | 完成度 |
| --- | --- |
| selector 抽取 + 四文件 | `100%` |
| standard/special legality 区分 | `100%` |
| replacement exact-profession capstone 优先 | `100%` |
| diagnostic 向外暴露 | `~60%`：unit test 可解释；whiteBoxLoot 层的 rejection breakdown 缺席 |
| river / temple / heart fixture case | `需核对 MilestoneRewardSelectorTest.kt` 具体 fixture；文件大小 `19.9KB` 足够，但本轮未逐行核对是否每个关键 case 都有 |

### 3.5 优先修复建议

1. **selector 进 `whiteBoxLoot`**（中优先）：在 `WhiteBoxLootRunner` 里新增一段"per-profession × per-critical-source 模拟选择"，直接调用 `MilestoneRewardSelector.select(...)`，把 top N 的 `rejectionReason` 写入 `rewardRoutingCoverageSummary.topRejectedCapstoneCandidates`。目前这一字段的数据来自 `Phase4ReportRunner` 的长链反推，而不是 selector 的真实拒绝日志。
2. **分叉收口测试**（低优先）：见表内。
3. 再次确认 `MilestoneRewardSelectorTest.kt` 是否覆盖计划 §10.9 的五个 case（special-linked legality / unified weighting / non-weapon replacement preference / river-temple-heart fixture / generic aligned weapon vs exact capstone 对抗）。本次评审没有逐行展开，建议本 PR 内留一次快速自查。

---

## 4. PR-04 · Fast-Fail Diagnostics & Staged Gate Hardening

### 4.1 合同要求回顾

1. 同区 passive-family duplicate 从 `phase4Report` 前移到 `whiteBoxLoot / verifyLootPreflight`
2. reward routing coverage 前移，culprit 显式可指向
3. staged metric（per-profession adoption / non-weapon / source coverage）先 `report-only`，blocking 由单独 gate-cutover PR 处理
4. 新增 report-only additive artifact `build-identity-debug.json`

### 4.2 已落地证据

- `LootPreflightRunner.kt:40-80` 的 `LootPreflightSummary` 直接嵌入 `specialTierPassiveFamilyDuplicateSummary` + `rewardRoutingCoverageSummary`，preflight 层已具备 culprit 列示能力。
- `LootIdentityDerivations.kt:130-182` 的 `computeSpecialTierPassiveFamilyDuplicateSummary` 输出 `canonicalZoneId / passiveFamily / templateIds / itemIds / professionTags`，完整满足计划 §11.4.1 四项要求。
- `LootIdentityDerivations.kt:264-312` 的 `RewardRoutingCoverageSummary` 导出 `criticalSources / professionSourceCoverage / topRejectedCapstoneCandidates`，对应计划 §11.4.2 三条。
- `WhiteBoxLootRunner.kt:79 / 98 / 402-466 / 683-724` 将 duplicate 与 source coverage 一起下发到 `whiteBoxLoot` 的 corpus aggregate metrics，且 summary 会带 `duplicateFamilies / duplicatedZones / culprit` 字段。
- `Phase4MetricCatalog.kt:170-184` 落地 `professionCapstoneAdoptionFloor.reportOnly` 与 `nonWeaponBuildPayoffFloor.reportOnly`，`decisionNotes` 明示 "intentionally report-only in PR-04" / "should only become blocking in a later gate-cutover PR"。
- `ReportPhase4Runner.kt:108 / 187-192 / 420-485` 显式产出 `build-identity-debug.json`，包含 `rewardSourceSelections / topRejectedCapstoneCandidates / perProfessionSourceCoverage`，与计划 §11.4.4 一致。工作区已存在 `tools/build/reports/verification/phase4/build-identity-debug.json`，大小 35.9KB。
- `docs/phase4/2026-03-13-phase4-verification-checklist.md:33 / 36` 已同步 canonical evidence 字段与 `build-identity-debug.json` 的强制产出约定。

### 4.3 残留偏差

| 偏差 | 位置 | 级别 | 解读 |
| --- | --- | --- | --- |
| `professionCapstoneSourceCoverage.reportOnly` 命名 vs 语义自相矛盾 | `Phase4MetricCatalog.kt:82-89` | 小（但是契约级） | 指标 `id` 带 `.reportOnly` 后缀，但 `failSemantics = "FAIL means ... must fail before long-run verification"`，`decisionNotes = "must fail before long-run verification"`；换言之这是 **blocking** 指标却戴了 **report-only** 帽子。计划 §11.4.3 第一步明确把"source coverage floor"归入 report-only stronger metrics，第二步才 gate-cutover。目前的实现相当于偷跑了 gate cutover，但把名字按第一步写成 `.reportOnly`——将来出问题时，name 与 fail 行为冲突会让人更难定位。 |
| `specialTierPassiveFamilyDuplicateCount` 前移是"前移到 whiteBoxLoot"而非"前移到 preflight" | `LootPreflightRunner.kt` 只写 summary，不直接 `require == 0` | 小 | 计划 §11.4.1 要求"至少有一层"在 whiteBox / preflight 前直接失败并列 culprit；目前 preflight 只写 summary，`whiteBoxLoot` 拿 summary 判 verdict。这符合"至少一层"的底线，但 preflight 未自动 fail，意味着 preflight 可以 PASS、whiteBoxLoot 才 FAIL。严格按计划 §4.1 第 5 条"preflight 作为 fast-fail 入口"解读，preflight 自身应 fail；建议 preflight 在 `duplicateFamilies.isNotEmpty()` 时返回非零退出码或 require 失败。 |
| blocking gate 未与 staged metric 严格区分 | `Phase4MetricCatalog.kt:82-89 vs 170-184` | 中 | 计划 §3 第 5 条："owner gate 当前 blocking 语义与下一阶段计划升级的更强语义必须分层明确"。目前的现状是：source coverage 跟命名自称 reportOnly 但实际 blocking；adoption/non-weapon floor 命名自称 reportOnly 且确实 reportOnly。两条规则同一后缀不同执行，下次 reviewer 不读 `failSemantics` 就无法判断实际语义。 |

### 4.4 偏差幅度

| 合同条款 | 完成度 |
| --- | --- |
| duplicate 前移 | `90%`（culprit 可见，但 preflight 未 fail-fast） |
| routing coverage 前移 | `100%` |
| staged metric report-only | `~70%`（两条 floor metric 达标，但 source coverage 已 blocking 且名称写成 reportOnly） |
| `build-identity-debug.json` 产出 | `100%` |
| checklist 更新 | `100%` |

### 4.5 优先修复建议

1. **rename `professionCapstoneSourceCoverage.reportOnly → professionCapstoneSourceCoverage`**（高优先）：或者反过来，把当前 verdict 语义降回 report-only。总之命名与 gate 行为必须对齐。如果确实要作为 blocking，建议：
   - `id` 去掉 `.reportOnly`
   - `failSemantics` 保留当前文案
   - 同步更新 baseline snapshot / docs / `Phase4AuthorityDocConsistencyTest` 里引用该 metricId 的地方
2. **preflight fail-fast**（中优先）：在 `LootPreflightRunner.run` 结尾加 `require(summary.specialTierPassiveFamilyDuplicateSummary.duplicateFamilyCount == 0) { ... culprit culprit ... }` 与 `require(summary.rewardRoutingCoverageSummary.professionCapstoneSourceCoverageRate >= 1.0)`，让计划 §4.1 第 5 条的 fast-fail 不再依赖 whiteBoxLoot 执行到 corpus 聚合阶段。
3. **schema 侧显式区分 blocking vs report-only 后缀规则**（低优先）：在 `Phase4MetricSpec` 增加枚举字段 `gateKind: { BLOCKING, REPORT_ONLY }`，让命名不再是唯一语义承载。

---

## 5. 合同冻结项核查（计划 §3）

| 编号 | 内容 | 核查结果 |
| --- | --- | --- |
| 1 | 不新增 `core` 枚举 / 战斗语义 / 资源轴 / content-pack 权限 | `✅ 未发现` |
| 2 | 不引入第二份 profession build identity authority | `✅ 已通过 FoundationProfessionCapstoneCatalog 收束为单真源` |
| 3 | `tools` 不单独维护 capstone / non-weapon ID 列表 | `✅ Phase4AggregationInputRunner / LootIdentityDerivations 全部从 game 模块派生` |
| 4 | `FoundationGameSession` 不再扩张 reward-routing `when` | `✅ 只保留两条 grant 入口并走 authority lookup` |
| 5 | `phase4Report` 之外不新增 canonical owner aggregate | `✅ build-identity-debug.json 明确 additive、非 canonical` |
| 6 | 同一 PR 不同时改 authority schema + gate + baseline | 需对 PR 合并历史做一次 `git log --diff-filter=M` 复核，workspace 中同时存在 baseline / authority / metric catalog 的改动未必来自同一 commit；此条属于 **流程级风险**，建议在 PR 合并顺序上由负责人最后再审一次。 |
| 7 | Gradle 命令串行执行 | 无法从代码侧直接验证，checklist 已写明串行语义 |

---

## 6. 必须修复 / 建议优化清单

### 6.1 必须修复（P0，影响合同语义）

1. **`professionCapstoneSourceCoverage.reportOnly` 命名与 gate 行为自相矛盾**（§4.5-1）——本轮最显性的契约漂移。
2. **`BuildIdentityAdoptionPolicy.kt` 缺失**（§2.5-1）——计划 §2.4 的核心诉求未落地，下次"看见但不穿"问题仍会分散。
3. **selector 未消费 `preferredRewardSources / preferredReplacementSlots`**（§2.5-2）——authority 已立但下游不用，等同 schema 死代码。

### 6.2 建议优化（P1，提升 fast-fail 质量）

4. **preflight 直接 `require` 失败**（§4.5-2）
5. **capstone cue parity 单测**（§1.4-1）
6. **selector 的 rejection breakdown 接通 `whiteBoxLoot`**（§3.5-1）

### 6.3 可选优化（P2，降低未来维护成本）

7. schema 层显式标注 identity 字段是 runtime 还是 report-only（§2.5-3）
8. `Phase4MetricSpec` 增加 `gateKind` 字段替代后缀命名（§4.5-3）
9. `allCandidateBaseIds vs standardCandidateBaseIds` 分叉收口测试（§3.3）

---

## 7. 总体评价（策划 / 开发 / 玩法体验三视角）

### 7.1 设计总监视角

Build identity 的四条 authority（routing / identity / selector / owner metric）在**数据层**已经基本"单一化"，说明团队确实吸收了过去"长链漂移"的教训。本轮 PR 把 architectural gravity 真正压在 `data/*` 与 `game/loot/*`，这是长周期收益的结构性投资。

但是 **adoption 与 selector 的 identity 偏好 wiring 仍没接通**，等价于"设计稿画完了、下游引擎还在用旧参数跑模拟"，这会直接让 `preferredRewardSources / preferredReplacementSlots` 在下个数值调优周期变成"假配置"。这是本次落地最大的体验性风险——**数据看起来可调，实测调了不生效**。

### 7.2 系统策划视角

1. 当前 adoption = 50% / nonWeapon = 50% 的均衡是因为 `templar / vanguard` 打满，`arcanist / rogue` 完全靠 seen 数补位，而不是因为 adoption 决策广泛生效。
2. 在 selector 消费 `preferredRewardSources` 之前，per-profession adoption / non-weapon 的 report-only floor 只能反映 **路由 + special-linked legality 的成果**，不能反映职业偏好 scoring 的成果。因此当前的 `.reportOnly` floor 可见性虽然对齐计划，但 **它们的数值并不真正来自 identity 偏好**；后续真要 gate-cutover 时，会发现 floor 数值对 identity 参数几乎不敏感。建议在 §6 P0-3 落地后，再观察 long-run 数值是否如期偏移，再讨论 gate-cutover 时机。

### 7.3 玩法体验审查视角

1. cue parity 的缺失意味着同一件职业 capstone 在 `cache` vs `support` vs `boss` 可能打出不同提示，玩家感受上容易出现"这件装备上次有播 anchor 语音，这次没有"的 regression。P1-5 单测是兜底。
2. `reward-routing/index.yaml` 的 `profileIds: []` 出现在 tutorial zone，从玩法体验看是"新手引导不干扰主线掉落池"的正向设计，但合同里没有专门注释，后续策划看到会误以为是笔误。在 schema 注释里明示该空条目的玩法意图是对 onboarding 的尊重。

---

## 8. 结论

- 本轮 hardening 在 **authority 收束 + fast-fail 前移** 两条主轴上 **基本达成** 计划要求，未触碰计划第 3 节任何禁区；canonical `phase4Report = PASS` 的背书是真实的，不是长链反推的假绿。
- 但是 **authority 与 runtime 的最后一公里** 有 3 处明显断点（§6.1），其中 `professionCapstoneSourceCoverage.reportOnly` 命名矛盾 与 selector 未消费 identity 偏好 是最典型的 "下次 6 小时定位" 风险来源，属于本轮未真正兑现的 "工程化教训"。
- 建议在 merge 进 main 之前 **至少完成 §6.1 的 3 项 P0**；§6.2 / §6.3 可以拆到后续小 PR。
- 后续若启动 gate-cutover PR，必须先等 §6.1-3（selector 消费 identity 偏好）落地至少一个 long-run 周期的观测，避免在 "偏好未生效" 的状态下把 adoption floor 切成 blocking。
