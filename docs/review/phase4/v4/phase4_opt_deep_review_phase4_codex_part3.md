# phase4 深度审查报告（Part 3/4）

## 5. 当前 Phase4 最需要解决的关键问题

### 5.1 职业树与铭文没有形成 run 内构筑选择

**问题描述**

当前职业成长更像“开局给一组完整基础动作，等级到了继续自动开放技能，玩家主要把点数投进 rank 数值”。这和 ToME 类游戏应有的职业树构筑不一致：玩家应该在 run 内不断面对“先开哪条树、哪个技能升到断点、牺牲哪个 answer、铭文槽位换不换”的选择。

铭文问题更直接：运行时开局默认装备 `healing_light / phase_door / iron_shield / purge` 四个铭文；`InscriptionManager.canEquip` 满 4 槽后直接返回 false。商店虽然有 `controlled_phase`、`phase_door`、`purge` 等 inscription offer，但玩家没有替换流程，购买符文不是有效构筑选择。

本问题的详细改造方向已经固定到 `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`：基础职业不新增技能数量；每职业开局 3 个 starter；`unlockLevel` 只进入 learnable；学习新技能与升 rank 共用职业天赋点；Tier 2/Tier 3 必须受前置 rank 与树投入约束；开局 2 铭文；满槽 inscription purchase 进入替换流程。

**证据**

| 证据 | 位置 |
| --- | --- |
| Phase3 P3-W3 已要求天赋树 V2、`TalentAllocationDraft`、断点成长、前置关系、升级预览 | `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md` |
| Phase3 最小发布集要求基础职业正式树 `4 x 16 = 64` 天赋 | `docs/2026-03-13-phase2-to-phase5-final-roadmap.md` |
| `TalentProgression.unlockedTalentIds` 按 profession tree 与 `unlockLevel` 返回技能 | `game/src/main/kotlin/com/ktome/game/TalentProgression.kt` |
| 新解锁技能直接 `putIfAbsent(talentId, 1)` 进入 loadout | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` |
| 初始 player `TalentLoadout` 把传入 talents 全部按 slot 顺序装入，且 rank 全部为 1 | `game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt` |
| `ensurePlayerInscriptions` 开局补满 4 个默认铭文 | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` |
| `InscriptionManager.canEquip` 满 4 槽即拒绝，没有替换语义 | `core/src/main/kotlin/com/ktome/core/inscription/InscriptionManager.kt` |
| 商店已有 inscription offer，但满槽后不可购买 | `game/src/main/resources/data/shops/index.yaml` |
| 职业树与铭文改造补充设计 | `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md` |

**为什么这是当前必须解决的问题**

Phase4 的目标是“重复游玩差异明显的长局”。如果职业成长轴没有选择压力，奖励/掉落再怎么调也只能在装备层制造变化。玩家不会产生“这局走火法树还是奥术护盾树”“这次我要不要放弃治疗铭文换 controlled phase”的 run 内故事。

**为什么不能推迟**

Phase5 文档明确写的是 tactical AI、perception/hate、perf/soak、replay、death analysis、QA 和发布收口，并声明“不再引入大新玩法系统”。职业树选择和铭文替换是基础构筑系统，属于 Phase3/Phase4 应补齐的债，不应进入 Phase5 主线。

**不解决的后果**

1. 升级只带来数值增强，不带来 build 方向变化。
2. 商店和铭文 offer 变成无效复杂度。
3. 后续 tactical AI 变聪明后，玩家侧 build 表达仍不足，战斗只会更难，不会更有策略。
4. 局外 run history 即使记录得再完整，也缺少“下一局换构筑”的动力来源。

### 5.2 构筑 payoff 仍靠 approved debt 放行

**问题描述**

Phase4 的核心承诺是提高重复游玩差异、loot/build/content pack 的长期驱动。但当前 `reportPhase4` 中最接近“构筑是否真的成立”的两个 per-prof floor 仍是 approved debt，同时 terminal weapon identity 也压在阈值线：

1. `professionCapstoneAdoptionFloor.reportOnly=2/4 APPROVED_DEBT`
2. `nonWeaponBuildPayoffFloor.reportOnly=2/4 APPROVED_DEBT`
3. `terminalWeaponBaseDiversity=3` 刚好踩 floor
4. `crossProfessionTopWeaponDominance=50.0% top=long_sword` 刚好踩上限

总体指标虽然踩线通过，例如 `professionCapstoneAdoptionRate=25.0%`、`nonWeaponBuildPayoffRate=25.0%`，但 per-prof floor 暴露出 arcanist 与 rogue 没有达到采用要求。terminal weapon 分布也暴露出 templar 与 vanguard 终盘都回到 `long_sword`。这是 Phase4 玩法体验问题，不是单纯验证策略问题。

**证据**

| 证据 | 位置 |
| --- | --- |
| `phaseVerdict=PASS_WITH_DEBT`、`approvedDebtCount=2` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `professionCapstoneAdoptionFloor.reportOnly`：arcanist `0/1`、rogue `0/1` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `nonWeaponBuildPayoffFloor.reportOnly`：arcanist `0/1`、rogue `0/1` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| milestone reward `adopted=46`、`notAdopted=57` | `build/reports/harness/long-run-summary.md` |
| `terminalWeaponBaseDiversity=3`、`crossProfessionTopWeaponDominance=50.0% top=long_sword` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| top weapon semantics：templar 与 vanguard 都是 `long_sword[item, weapon, melee, frontline, guard, discipline]` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| arcanist 终局 ARMOR 为 `unique_furnace_plate`，该物品属于 vanguard capstone；`unique_deepcurrent_lens` 出现但未替换 `emerald_charm` | `build/reports/harness/long-run-summary.md`；`game/src/main/resources/data/build-identity/index.yaml` |
| rogue 多次看到 `artifact_briar_heart`，但终局 OFF_HAND 仍为 `basic_shield` | `build/reports/harness/long-run-summary.md` |

**为什么这是当前必须解决的问题**

Phase4 不是 UI polish 阶段，也不是纯验证阶段。Phase4 的核心就是 procgen、loot、hidden、content pack 与重复游玩差异。如果职业 capstone 和非武器 payoff 仍不能稳定被采用，就说明奖励/构筑系统没有把内容转化为玩法变化。

**为什么不能推迟**

Phase5 如果继续添加 tactical AI、replay、death analysis 或更多 content，它们都会依赖当前构筑分化。如果现在不修，后续新增内容会继续被同一套低采用率漏斗吞掉，变成“内容更多，但打法仍旧”。

**不解决的后果**

1. 玩家觉得系统很多，但多数掉落只是卖掉/忽略。
2. arcanist/rogue 的非武器路线缺乏说服力，职业构筑分化不稳定。
3. templar/vanguard 的终局视觉和玩法身份撞车，削弱四职业重玩的差异。
4. 后续 content pack 扩张会优先复制已有问题，新增物品越多，筛掉无效奖励的成本越高。
5. `reportOnly` debt 长期存在会削弱 owner gate 的信用：核心体验指标变成“知道没过但允许过”。

### 5.3 奖励曲线仍偏“产出足够，采用不足”

**问题描述**

Phase4 已经解决了不少“奖励是否能产出”的问题，但“奖励是否让玩家改变下一步决策”仍弱。dynamic pool、secret identity、source coverage 都通过，但采用率、slot 结构和 affix synergy 分布说明奖励还没有足够频繁地制造 build shift。

**证据**

| 证据 | 位置 |
| --- | --- |
| `dynamicPoolCoverage=100.0% (10/10)` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `sameZoneSecretVsCadenceMaxOverlap=0.400`、`sameZoneSecretVsRewardMaxOverlap=0.400` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `milestoneRewardAdoptionDistribution={adopted=46, notAdopted=57}` | `build/reports/harness/long-run-summary.md` |
| `milestoneRewardSlotDistribution={ARMOR=8, OFF_HAND=49, WEAPON=46}` | `build/reports/harness/long-run-summary.md` |
| `affixSynergyActivationDistribution={of_shadow=5}` | `build/reports/harness/long-run-summary.md` |
| `synergyAffixDistribution={of_piercing=11, of_shadow=6, of_smite=1}` | `build/reports/harness/long-run-summary.md` |
| `routeRewardAffixUsageSummary={sentinel=8, of_strength=7, vampiric=7, of_life=6, ...}` | `build/reports/harness/long-run-summary.md` |

**为什么这是当前必须解决的问题**

Roguelike 的奖励不是库存填充物。奖励必须持续回答：“我现在要不要换打法？” 当前系统已经能产出奖励，但被采用的奖励少于未采用奖励，且 synergy 记忆点集中在少数 affix 上。这会让玩家把奖励环节看成噪声。

**为什么不能推迟**

这不是“内容量不足”。内容量不足可以后续补；奖励采用不足是结构问题。若先进入 Phase5，新增奖励只会增加未采用数量，调参面和解释成本都会变大。

**不解决的后果**

1. 掉落频率越高，玩家越麻木。
2. secret reward 的本地身份即使通过，也未必转化为“我想去找它”的动机。
3. 装备/affix 系统会变成复杂度负担，而不是乐趣来源。

### 5.4 有机隐藏探索已过线，但主动 Search 学习曲线不稳定

**问题描述**

当前 hidden loop 已不是 v3 的假绿。总体发现率、转化率和 secret entry 都过线，没有 failing zone。但 zone 级数据显示，一些 secret 的进入更像由路径或事件推到玩家面前，而不是玩家形成“看到异常 -> 主动 Search -> 获得反馈”的学习链。

**证据**

| 证据 | 位置 |
| --- | --- |
| 总体 `leadDiscoveryRate=46.6%`、`secretConversionRate=43.9%`、`secretZoneEntryRate=20.5%` | `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md` |
| `failingSecretEntryZoneIds=none` | `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md` |
| `abyssal_temple leadDiscoveryRate=97.0%`、`searchUseRate=0.0%`、`secretEntryRate=15.2%` | `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md` |
| `deep_iron_pit searchUseRate=1.5%`、`secretEntryRate=23.5%`、`secretConversionRate=68.9%` | `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md` |
| `underground_river leadDiscoveryRate=17.4%`、`searchUseRate=28.0%` | `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md` |
| `zoneDiscoveryDistribution` 与 `secretZoneDiscoveryDistribution` 错位：`abyssal_temple=52.0%` lead share 只转成 `abyssal_temple_warded_archive=18.5%` entry share；`greenwood_fringe=20.3%` lead share 转成 `greenwood_hidden_cache=38.9%` entry share | `tools/build/reports/verification/phase4/report-phase4-summary.md` |

**为什么这是当前必须解决的问题**

Phase4 的 hidden content 不只是 bonus content，而是探索重复游玩的重要支柱。玩家必须学会主动发现异常、判断风险、使用 Search 或交互方式进入 secret。否则 hidden content 会变成“系统偶尔给你一个入口”，而不是玩家主动探索的奖励。

**为什么不能推迟**

后续 content expansion 会继续添加 hidden/secret。如果当前不同 zone 的提示强度、Search 行为和 entry 转化不一致，后续新增 secret 会更难调。隐藏内容一旦被玩家理解为随机赠品，探索驱动会被削弱。

**不解决的后果**

1. 玩家不会形成 Search 习惯。
2. 一些 zone 隐藏内容显得过度自动，另一些 zone 又显得提示不足。
3. frontstage cue 虽然 100% 可见，但可能只是事后提醒，而不是推动玩家主动探索的前置信号。
4. aggregate 通过会掩盖 per-zone 错位，后续新增 hidden content 可能继续复制“lead 很多但 entry 不兑现”的错误节奏。

### 5.5 Zone 机制名词仍有一部分没有 runtime hook

**问题描述**

Critical Path Design Audit 把每个 mandatory zone 的 `mechanicsWithoutDedicatedRuntimeHook` 列出来了。这是好事，说明报告没有伪造机制存在；但也暴露出一个体验风险：如果这些名词在文档中被当成玩法机制，而 runtime 里没有对应 hook，玩家感受到的是“zone 叫法不同”，不是“zone 玩起来不同”。

**证据**

| zone | 未绑定 runtime hook 的机制名词 | 位置 |
| --- | --- | --- |
| `greenwood_fringe` | `trail_pressure`, `beacon_warning` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `deep_iron_pit` | `ore_cart`, `slag_alert` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `grey_gate_depths` | `sealed_gate`, `ritual_pressure`, `shadow_warning` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `underground_river` | `ferry_crossing`, `drowned_ambush` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| `abyssal_temple` | `prayer_hall`, `void_pressure` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |

**为什么这是当前必须解决的问题**

Phase4 的地图/探索/隐藏内容需要形成重复游玩差异。如果 zone 机制只是文档词汇，玩家会很快把不同 zone 视为不同皮肤的走廊。当前可以不把每个机制都做成完整子系统，但至少要做最小 runtime effect、frontstage cue 或文档降级。

**为什么不能全部推迟**

如果机制名词已经进入 Phase4 文档和报告，它就会影响验收口径。后续扩 content 时，策划会继续引用这些名词写事件、奖励和敌人，最终形成“文档上系统很多，runtime 没有对应权威”的第二真源。

**不解决的后果**

1. zone 差异感不稳定。
2. hidden/reward/terrain 内容难以与 zone identity 形成联动。
3. content pack 作者会误以为这些机制可被 overlay 引用，导致边界混乱。

### 5.6 Boss variant 已不伪完成，但长期记忆点仍薄

**问题描述**

Boss variant 当前通过了关键指标，但结构上仍是轻量 variant：每对都 `phaseGraphUnchanged=true`、`structuralDiffCount=0`。这不构成 Phase4 P0，因为文档目标是 Phase4 overlay/variant 级扩展，不是完整 tactical AI 或新 boss runtime。但若把它包装成“强 Boss replayability”，就会过度宣传。

**证据**

| 证据 | 位置 |
| --- | --- |
| `variantTraceDivergenceRatio=100%`、`minVariantActionTraceDivergenceScore=1.000` | `tools/build/reports/verification/phase4/report-phase4-summary.md` |
| 3 个 boss variant 均只有 `actionWeightProfileId`，没有 `phaseOverrides` | `game/src/main/resources/data/boss-variants/index.yaml` |
| `molten_glass` variant selectedActions 从 base 多动作变为 `[linebreaker]`，preferredTerrain `[OIL]` 可用且应用 | `tools/build/reports/phase4/whitebox/boss/boss-harness.md` |
| 每个 pair 均 `phaseGraphUnchanged=true`、`structuralDiffCount=0` | `tools/build/reports/phase4/whitebox/boss/boss-harness.md` |

**为什么这是当前要明确的问题**

这不是当前必须重做完整 Boss 系统的缺陷，但必须在报告里讲清边界：Phase4 已经从 v3 的伪完成推进到可接受 variant；但它仍不是长期 Boss 系统的设计完成态。当前阶段应该补的是 data-level 的最小 variant phase language：每个 variant 至少贡献 1 个 `phaseOverrides`，并在 override 内声明阶段内独有 trigger/telegraph/action，而不是扩成 Phase5 tactical AI。

**为什么可以部分延后**

完整 Boss phase graph mutation、tactical AI decision trace、普通敌人 intent 等更适合 Phase5，因为 PR05 已明确不引入 `AIPlanSnapshot`，Phase4 也禁止扩大到 runtime script host 或完整 Mod SDK。最小 `phaseOverrides` 不属于 Phase5 tactical AI，它只是让现有 Phase4 variant 数据兑现“变体阶段语言”。

**不解决边界说明的后果**

1. 后续会误把轻量 variant 当作 Boss 终局设计。
2. 内容扩张可能只复制 variant 参数，而不是增加真实战斗记忆点。
3. 玩家重复打 Boss 的体验提升会低于报告指标看起来的提升。

### 5.7 Long-run 证据链主路线同构，重复游玩差异验证不够硬

**问题描述**

当前 long-run 能暴露 capstone、reward adoption、slot distribution 和死亡分布，但 smoke corpus 的主路线同构较强。它不直接证明玩家体验失败，却会削弱 Phase4 “重复游玩差异明显”的验收证据：当 6 条 full_route 都落在同一 route hash 时，报告可以证明一条主路径稳定，但不能充分证明多路径、多分支下的构筑/奖励问题都被覆盖。

**证据**

| 证据 | 位置 |
| --- | --- |
| `scenarioTypeDistribution={full_route=6, branch_inclusive=1, route_probe=2, late_route_probe=2}` | `build/reports/harness/long-run-summary.md` |
| `zoneRouteHashDistribution` 中 hash `6871f273...dbaaf=6`，其余 hash 分布为 `1/2/1/1` | `build/reports/harness/long-run-summary.md` |

**为什么这是当前必须解决的问题**

这不是内容缺口，而是证据缺口。Phase4 的核心卖点是重复游玩差异；如果 long-run 的 owner metric 没有 route-hash diversity 或 branch-inclusive floor，后续构筑/奖励调参可能只对一条主路径过拟合。当前 capstone adoption 和 slot distribution 已经暴露问题，说明更宽的路线采样会直接影响设计判断。

**为什么不能推迟**

Phase5 会接入 tactical AI、death analysis、replay 和 run history。如果 Phase4 的 long-run corpus 仍以单主线为主，Phase5 的分析工具会继承偏窄样本，死亡分析和 replay 可能解释得很细，但解释对象仍不够代表真实重复游玩。

**不解决的后果**

1. reward/build 调参容易过拟合当前 12 个 terminal samples。
2. branch route 中的 secret、boss、capstone 采用风险无法稳定暴露。
3. Phase5 run history 会记录更多结果，但缺少足够多样的 Phase4 验证基线。
