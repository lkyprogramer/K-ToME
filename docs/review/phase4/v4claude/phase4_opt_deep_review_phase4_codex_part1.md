# Phase 4 深度玩法体验审阅 — Part 1：执行摘要 / 审阅范围 / 设计实现一致性矩阵

**审阅身份**: 资深 Roguelike 游戏设计总监 / 系统企划总监 / 玩法体验审阅负责人
**审阅目标**: 在 `docs/phase4/` 定义的 Phase 4 语义与 `docs/review/phase4/v3` 的修复承诺下，判断当前版本是否已从"能完成长局"跨入"重复游玩差异明显且稳定好玩的长局"。
**审阅日期**: 2026-04-24
**审阅基准 buildId**: `phase4-opt-pr05-dev`
**审阅基准版本**: `0.4.0`（gradle.properties / DesktopLauncher / sample pack gameVersionRange `>=0.4.0 <0.5.0` 已对齐）
**最终判定（简版）**: `PASS_WITH_DEBT` — Phase 4 功能骨架与 V3 修订承诺基本落地，但"体验深度"层面仍有 3 处结构性薄弱，未达可稳定交付的"好玩"完成态。必须在 Phase 4 内收口，不建议延至 Phase 5。

---

## 1 · 执行摘要

### 1.1 一句话结论

> **"功能完成、指标绿、玩法体验接近临界但尚未达到稳定好玩"**。V3 PR-01..05 的修订承诺已体现为 phase4Report 的 PASS 状态，但 `crossProfessionTopWeaponDominance` 与 `professionCapstoneAdoptionRate` 都压在阈值线上，boss 语言还偏薄，organic hidden 发现分布严重偏斜。这些都不是"技术伪完成"级别的严重回归，但足以让 Phase 4 的核心卖点（重复游玩差异明显）在长局中出现"技术指标过关 / 玩家直觉不过关"的剪刀差。

### 1.2 十条关键结论（按优先级排序）

1. **【结构性 · P0】build identity 仍未真正分化成 4 条 profession 线。**
   `crossProfessionTopWeaponDominance=50.0% (6/12) top=long_sword`，其中 templar + vanguard 共占 6，正好压在阈值 `<= 50.0%` 上；`terminalWeaponBaseDiversity=3`（`arcane_staff / hunter_bow / long_sword`）压在 floor；templar 与 vanguard 在长局中都终盘于 `long_sword`。该指标是"刚好过线"，不是"已经分化"。证据锚点：`tools/build/reports/verification/phase4/report-phase4-summary.md` / `build/reports/harness/long-run-summary.md`。

2. **【结构性 · P0】每职业 capstone 采用 floor 有两个职业完全为 0。**
   `professionCapstoneAdoptionFloor.reportOnly: 2/4 APPROVED_DEBT`（arcanist=0/1, samples=3；rogue=0/1, samples=3）；`nonWeaponBuildPayoffFloor.reportOnly` 同样 2/4。虽然 aggregate `25.0% (3/12)` 刚好过 floor，但那 3 个采样全部来自 vanguard + templar，Phase 4 承诺的"每个职业都有可识别的终盘 build"并没有稳定兑现。

3. **【结构性 · P1】boss 变体还是"三件套 + 轻微换皮"。**
   `game/src/main/resources/data/boss-variants/index.yaml` 只有 3 个 variant（`molten_glass / grey_crown / abyssal_eclipse`），每个 2–4 个 action weight，**没有任何 `phaseOverrides`**。`bossHarness` 指标 100% PASS 是因为 base bosses 的 `phase_full + phase_enraged/desperate` 已提供阶段结构，但 variant 本身并没有引入"不同阶段、不同打法"的正式语言。`bossVariantBasePhaseCountMin=2` 只是对 base encounter 的 phase 数量下限，而不是 variant 自己引入的 phase。

4. **【结构性 · P1】organic hidden 发现严重向 abyssal_temple 倾斜，终端体感与 secret 分布严重脱节。**
   `zoneDiscoveryDistribution: abyssal_temple=52.0%, greenwood_fringe=20.3%, deep_iron_pit=18.3%, underground_river=9.3%`。但 `secretZoneDiscoveryDistribution` 是反过来的：`greenwood_hidden_cache=38.9%, deep_iron_smuggler_stash=28.7%, abyssal_temple_warded_archive=18.5%, underground_river_crystal_rift=13.9%`。`abyssal_temple` 的 lead 多但 secret 转化率低，`greenwood_fringe` 刚好反过来。`leadDiscoveryRate=46.6%` 是被 abyssal_temple 拉高的，单 zone 体感的均衡性并未达到。

5. **【结构性 · P1】milestoneRewardSlotDistribution 严重偏 OFF_HAND/WEAPON，ARMOR 几乎缺席。**
   `ARMOR=8, OFF_HAND=49, WEAPON=46`。Phase 4 的 loot budget V2 在 slot bias 上对 ARMOR 的分配远低于 OFF_HAND 和 WEAPON，这放大了"终盘都是同一把武器 + 同一件饰品"的体感。

6. **【修订承诺 · 基本达成】V3 PR-01 关键路径节奏 + V3 PR-03 organic hidden 假绿 + V3 PR-05 frontstage action cue 都已实际落地**。
   - `criticalPathCombatFloorSatisfied: 100.0% (5/5)`；最小 objective acquire turn 7.2，最小 visible hostile turn 10.2，最小 enemy turn 74.8，均大幅高于 floor。
   - `leadDiscoveryRate=46.6%` / `secretConversionRate=43.9%` / `searchUse=16.9%` 都显著高于 floor。
   - `frontstageHighPriorityCueRetainedRate=100%` / `frontstageCueDedupAppliedCount=1` / `frontstageCueExpiryParity=100%` / `frontstageSecretCueVisibilityRate=100%`。
   - typed `FrontstageActionCueSnapshot` 合同在 `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt` 真实落地。

7. **【修订承诺 · 基本达成】V3 PR-02 dynamic pool coverage 100% + secret reward single authority**。
   `dynamicPoolCoverage=100.0% (10/10)`；`specialTierPassiveFamilyDuplicateCount=0`；`secretZoneRewardAuthorityViolations=0`。`game/src/main/resources/data/loot/index.yaml` 全部 cadence/main reward profile 均已切 `TAG_WEIGHTED`，秘境 profile 保留 `FIXED_LIST`（策划性曲线）。

8. **【修订承诺 · 基本达成 · 但源数据偏薄】V3 PR-04 boss phase 指标达成但 variant 源数据没真正补齐**。
   bossHarness 全部 PASS，但 boss-variants 源数据只有 3 条、没有 phase-level 动作池重写。指标结构倾向于验证"能观察到 phase 过渡"而不是"每个 variant 有 variant-level 的终盘语言"。

9. **【修订承诺 · 对齐】version discipline 已统一到 0.4.x**。
   `gradle.properties version=0.4.0` / sample pack `gameVersionRange: ">=0.4.0 <0.5.0"` / generated `ktome-build.properties` 与 client window title 均对齐。

10. **【伪完成风险】sample content pack 仍以 REPLACE 为主，偏离 Phase 4 推荐的 ADD-first 示范**。
    Phase 4 的 content pack overlay 设计目标是"让第三方 pack 用 ADD 扩展，不用覆写主源"，但 sample pack 目前多条 hidden event/secret zone 使用 REPLACE ops。作为 sample 它需要承担"示范正确扩展姿势"的责任，否则下游 pack 作者会模仿错误模式。

### 1.3 是否已到"可以进入下一阶段"的完成态？

**我的判定：尚未。**

- 从**工程完成度**看：Phase 4 的 procgen、loot V2、pack overlay、elite mutation、hidden、secret、boss 变体骨架都已落地，`phase4Report` PASS_WITH_DEBT，V3 PR-01..05 修订承诺的 owner metric 都在 PASS；
- 从**玩法体验完成度**看：上述 5 个结构性薄弱点都会直接伤到 Phase 4 的核心卖点——"重复游玩差异明显的长局"。其中 (1)(2) 是同一个问题的两个测量维度：build identity 在指标层刚过线，但在玩家实际体感里只有 2 条 build 线真正分化；(3)(4) 一起决定"终盘 + 探索"两块体验是否有结构性重玩价值；(5) 决定"奖励多样性"是否能稳定支撑 15–20 次重玩。

**不能延到 Phase 5**，理由：

- V3 PR-02 已经宣称 build identity 收口，如果现在就进入 Phase 5，Phase 4 会以"指标刚过线 + 源数据不足"的状态冻结；
- boss variant 源数据是一次性工作，Phase 5 理论上应聚焦于"replay 产品化 / 对战外层 / 元进程"，不应再回头补 Phase 4 的基础语言；
- organic hidden 分布倾斜与 milestone slot 分布偏斜都是 loot / zone 源数据的权重问题，属于典型的 Phase 4 清扫，拖到 Phase 5 会持续放大玩家感知偏差。

Part 4 将给出这些问题的 P0/P1/P2 具体修订方案。

---

## 2 · 审阅范围与依据

### 2.1 审阅范围

本次审阅覆盖以下三个维度：

| 维度 | 具体内容 |
| --- | --- |
| **设计文档 vs 实现** | `docs/phase4/` 下全部设计文档（含 cross-cutting contracts、PR-01..09 设计稿、verification checklist）与 `core/`、`game/`、`client/`、`tools/` 下的实际 Kotlin 实现 / resources 数据 / gradle 构建。 |
| **V3 修订承诺 vs 实现** | `docs/review/phase4/v3/` 下的 V3 deep review + V3 PR-01..05 的修订承诺是否真正落地，以及是否出现了新的结构性回归。 |
| **UI/UX PR 修订 vs 玩家前台** | `docs/opt/ui-pr/` 下 5 条 UI/UX PR（client foundation、ingame info/input/modal/look、item/content/UI states、status/description/readability、telegraph & combat decision surface）的完成状态与前台交互。 |

### 2.2 不在本次审阅范围

- Phase 5 规划（`docs/phase5/`、`docs/phase5-design/` 如有）。
- 外部对战/元进程层设计（Phase 4 非目标）。
- 引擎级性能优化（libGDX / LWJGL 渲染栈）。
- 工具链 CI/CD 策略（只要 phase4Report 能正常产出即视为工具链可用）。

### 2.3 核心依据文档

#### 2.3.1 Phase 4 原始设计

- `docs/phase4/roadmap.md` — Phase 4 主题：从"可完成长局"走向"重复游玩差异明显的长局"，Checkpoints P4-X/A/B/C。
- `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md` — `MapgenPipeline / SolvabilityGraph / LootBudget / RarityTier / SpecialTier / FloorRewardBudget / EncounterThreatBudget / ContentPackManifest` 八大合同。
- `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` — Phase 4 主设计稿，体验差异预算（topology 30% + content 20% + loot quality 20% + encounter 15% + hidden 15%）、最少 5 局中有 3 处可感差异。
- `docs/phase4/2026-03-13-phase4-pr-01..09-*.md` — 9 条原始 PR 设计稿。
- `docs/phase4/2026-03-13-phase4-verification-checklist.md` — Phase 4 验收清单。

#### 2.3.2 V3 修订承诺

- `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part1..4.md` — V3 深度审阅报告。
- `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-01..05-*.md` — V3 修订 PR 文档。
  - V3 PR-01：关键路径节奏 / 客观可感战斗量。
  - V3 PR-02：build identity + dynamic loot pool coverage + 每职业 capstone floor。
  - V3 PR-03：organic hidden loop + secret reward single authority。
  - V3 PR-04：boss phase identity + version discipline。
  - V3 PR-05：frontstage action cue typed contract。

#### 2.3.3 UI/UX 修订

- `docs/opt/ui-pr/README.md` — 5 条 UI/UX PR 的结构与 deferred 收口表。
- `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01..05-*.md` — 各 PR 文档。
- `docs/opt/ui-pr/manual-records/` — 人工白盒记录。

### 2.4 核心证据锚点

| 类别 | 证据路径 |
| --- | --- |
| Phase 4 汇总报告 | `tools/build/reports/verification/phase4/report-phase4-summary.md`（31 个 owner metric，2 个 APPROVED_DEBT，0 回归） |
| Long-run lab | `build/reports/harness/long-run-summary.md`（6 个 full_route + 1 branch_inclusive + 2 route_probe + 2 late_route_probe；6 个职业采样） |
| Loot 源数据 | `game/src/main/resources/data/loot/index.yaml`（cadence/main/secret 三类 profile） |
| Boss 变体源数据 | `game/src/main/resources/data/boss-variants/index.yaml`（3 variants, 2–4 action weights each, 无 phaseOverrides） |
| Boss 源数据 | `game/src/main/resources/data/bosses/index.yaml`（phase_full + phase_enraged/desperate） |
| Frontstage typed contract | `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`（`FrontstageActionCueSnapshot` data class） |
| Client UI 前台 | `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionPanel.kt / CombatDecisionFrame.kt / CombatDecisionValidationSurface.kt` + `client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt` |
| Sample pack | `examples/content-packs/sample.flooded_relics/manifest.yaml`（version 1.0.0, gameVersionRange ">=0.4.0 <0.5.0"） |
| Version 锚点 | `gradle.properties: version=0.4.0`、生成的 `ktome-build.properties`、`DesktopLauncher` window title |

### 2.5 审阅方法

1. **证据优先**：每条结论绑定 owner metric / 源数据文件 / 长局采样输出，不使用"我感觉"类主观判断。
2. **体验锚定**：按"玩家从第 1 局到第 N 局的真实感知路径"回推"指标是否支撑了感知"，而不是只验证指标本身。
3. **伪完成识别**：任何一条"指标 PASS 但玩家直觉可能不 PASS"的情况，单独作为独立判据标出（本次命中：build identity dominance、boss variant、organic hidden 分布、slot bias、sample pack REPLACE）。
4. **不模糊化**：任何偏差按 P0 / P1 / P2 分级；不写"可能需要关注一下"、"建议看看"等模糊措辞。

---

## 3 · Phase 4 设计实现一致性矩阵

下表 20 条涵盖 Phase 4 主设计、cross-cutting contracts、V3 修订承诺和 UI/UX 修订。严重级别按 P0（阻塞 Phase 4 收口）/ P1（影响体验深度但不阻塞）/ P2（细节问题）。

| # | 系统/模块 | 设计目标（出自 docs/phase4 或 docs/review/phase4/v3 或 docs/opt/ui-pr） | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `MapgenPipeline` 合同 | 拓扑 / 节点 / 房间 三层分离；biome family × pattern vault 可混合；`SolvabilityGraph` 可回溯 | ✅ 已实现，`revealSuccessCasesWithBacktrackProof: 40/40`，`revealFailTaxonomy: FAILED_CHECK` 下 6/6 全部可回溯 | phase4Report §Solvability WhiteBox；`core/.../mapgen/` 实际源 | 无偏差。 | — |
| 2 | Biome × Pattern Vault 混合 | 同一区域多种地形语言混搭；混合应在长局采样中可观察 | ✅ `terrainInteractionEncounterRate.aggregate=16.6% (+31.95% vs baseline)`，单 zone 均 >= 11% | phase4Report 同名 metric | `abyssal_temple` 因 runtime objective-driven 被从 combat-sampled 集合排除，属于已知合理豁免。 | — |
| 3 | Solvability 回放可重入 | 同 seed 可严格回放；fail case 6 条全部可复现 | ✅ `revealSuccessCaseCount=40, revealFailCaseCount=6`，全部具备 fail-capable 断言 | 同上 §Solvability WhiteBox | 无偏差。 | — |
| 4 | Loot Budget V2 / TAG_WEIGHTED 动态池 | cadence/main reward profile 全部切 TAG_WEIGHTED；secret profile 保留 FIXED_LIST；`dynamicPoolCoverage=100%` | ✅ 全部 cadence/main profile 已 TAG_WEIGHTED；10/10 zone 动态池覆盖；`specialTierPassiveFamilyDuplicateCount=0` | `game/src/main/resources/data/loot/index.yaml`；phase4Report | 无偏差。 | — |
| 5 | Rarity Pipeline (MAGIC/RARE/...) | 稀有度梯度 + milestone 反馈 | ✅ `milestoneRewardQualityDistribution: {MAGIC=68, RARE=35}`；稀有度比率合理 | long-run-summary | 无偏差。 | — |
| 6 | FloorRewardBudget — slot 分布均衡 | 不同 slot 在 milestone 上应大致均衡；ARMOR 作为终盘 identity 的载体应有合理曝光 | ⚠️ `milestoneRewardSlotDistribution: {ARMOR=8, OFF_HAND=49, WEAPON=46}` — ARMOR 只占 7.7% | long-run-summary 同名分布 | ARMOR 严重缺席；这是"终盘只有武器 + 饰品"体感的源头之一。 | **P1** |
| 7 | Build Identity / terminal weapon diversity | 至少 3 个 terminal base；每职业有稳定 profession-aligned weapon；`crossProfessionTopWeaponDominance <= 50%` | ⚠️ `terminalWeaponBaseDiversity=3`（刚好过 floor）；`topWeapon=long_sword`；**templar + vanguard 两个职业都终盘 long_sword** | phase4Report `crossProfessionTopWeaponDominance=50.0% (6/12)`；long-run `templar#... WEAPON:long_sword / vanguard#... WEAPON:long_sword` | 技术指标刚压在阈值线，体验上 long_sword 同时承担两个职业的 terminal，是 Phase 4 最显眼的 build 收敛残留。 | **P0** |
| 8 | 每职业 capstone 采用 floor | 每职业至少 seen>=1, adoption>=1, nonWeapon>=1 | ⚠️ `professionCapstoneAdoptionFloor.reportOnly: 2/4` — **arcanist=0/1, rogue=0/1**（APPROVED_DEBT）；`nonWeaponBuildPayoffFloor.reportOnly: 2/4` 同样 arcanist/rogue 为 0 | phase4Report 同名 reportOnly 条目 | "每个职业都有可识别的终盘 build" 这一 Phase 4 承诺在 arcanist 与 rogue 上未兑现。 | **P0** |
| 9 | Cross-profession capstone 采用 | seen rate >=100%, adoption rate >=25% | ✅ `professionCapstoneSeenRate=100% adoption=25.0%`、`professionCapstoneAdoptionRate=25.0% (3/12)` | phase4Report | 数字压在 floor，但采样集中在 vanguard + templar；见 #8。 | **P0**（与 #8 合并） |
| 10 | Profession-aligned weapon adoption | `professionAlignedWeaponAdoptionRate >= 75%` | ✅ 100.0% (12/12) | phase4Report | 严格按 profession 语义 align 到 long_sword / arcane_staff / hunter_bow；问题在于 templar & vanguard 的 aligned 都是 long_sword。 | — |
| 11 | Elite Mutation 合同 | 精英变体 + terrain preference；variant/base trace divergence | ✅ `variantTraceDivergenceRatio=100% (3/3)`；`minVariantActionTraceDivergenceScore=1.000`；`abyssal_eclipse` 已切 `void_mirror + phase_runner` | phase4Report；`game/src/main/resources/data/boss-variants/index.yaml` | 指标满分，但 variant 源数据见 #12。 | — |
| 12 | Boss Variant 终盘语言 | 每个 boss 至少 2 阶段；variant 引入新 action pool 或 `phaseOverrides`；不同 variant 在 mutation / terrain / 掉落 / 表现有可感差异 | ⚠️ boss variants 只有 3 个，每个 2–4 个 action weight，**无 `phaseOverrides`**；base boss 的 phase 由 base encounter 提供（`phase_full + phase_enraged`） | `game/src/main/resources/data/boss-variants/index.yaml`；`game/src/main/resources/data/bosses/index.yaml` | `bossHarness` PASS 的原因是 base encounter 已有 phase 结构，variant 本身并未贡献 variant-level 阶段语言。源数据对 Phase 4 "终盘不再只有血条"的承诺偏薄。 | **P1** |
| 13 | BossVariant base phase floor | `bossVariantBasePhaseCountMin >= 2` | ✅ 恰好 =2（floor） | phase4Report | 刚好压 floor；与 #12 合并。 | **P1**（与 #12 合并） |
| 14 | Hidden Loop / organic probe | `leadDiscoveryRate >= 30%`；`searchActionUseRate >= 15%`；`secretZoneEntryRate >= 15%`；分 zone 均需 >= 5% | ✅ `leadDiscoveryRate=46.6%`；`searchUse=16.9%`；`secretEntry=20.5%`；分 zone floor 0 failing | phase4Report | 指标达成；但分布倾斜见 #15。 | — |
| 15 | Hidden 分 zone 均衡 | 各 zone 的 lead/secret 分布应大致均衡，避免 hidden 体感全在一张图 | ⚠️ `zoneDiscoveryDistribution: abyssal_temple=52.0%, greenwood=20.3%, deep_iron=18.3%, underground_river=9.3%`；secret 分布反向：`greenwood_hidden_cache=38.9%, deep_iron_smuggler_stash=28.7%, abyssal_temple_warded=18.5%, underground_river_crystal_rift=13.9%` | phase4Report §Scripted vs Organic Hidden | 50% 以上 lead 集中在 abyssal_temple（因 objective-driven runtime 增加曝光），但 secret 兑现率却低于 greenwood；整体是"lead 集中 / secret 兑现分散"的错位。 | **P1** |
| 16 | Secret reward 单一权威 | `secretZoneRewardAuthorityViolations <= 0`；secret zone 只能通过 `secretZoneDef.rewardProfileId` 配置奖励 | ✅ 0 violations | phase4Report | 无偏差。 | — |
| 17 | Frontstage Action Cue typed contract | `FrontstageActionCueSnapshot`（SEARCH / SECRET / PASSIVE + CRITICAL/HIGH/MEDIUM/LOW + stableKey + TTL） | ✅ `recentActionCues: List<FrontstageActionCueSnapshot>` 已在 core snapshot；4 项 cue 指标全 100% | `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`；phase4Report 4 条 frontstage 指标 | 无偏差。 | — |
| 18 | Content Pack Overlay | pack manifest + gameVersionRange；ADD / MODIFY / REPLACE 三种 ops；sample pack 示范正确扩展姿势 | ⚠️ overlay 机制已落地；但 sample pack (`examples/content-packs/sample.flooded_relics`) 对 hidden event / secret zone 大量使用 REPLACE，偏离"ADD-first"的示范责任 | `examples/content-packs/sample.flooded_relics/manifest.yaml` + pack ops | 作为官方 sample，它的示范作用被削弱；下游 pack 作者会复制 REPLACE 模式。 | **P2**（非功能偏差，但属于"官方示范权威性"问题） |
| 19 | Version Discipline | runtime/build/sample pack 对齐到 Phase 4 口径（0.4.x） | ✅ 全部对齐到 `0.4.0` | `gradle.properties`、`DesktopLauncher`、sample manifest `gameVersionRange: ">=0.4.0 <0.5.0"` | 无偏差。 | — |
| 20 | UI/UX — Combat Decision & Telegraph（UIUX PR-05） | 战斗 3 层决策面（ACTION → METHOD → TARGET）+ telegraph 三位一体；golden 通过 | ✅ `CombatDecisionFrame / CombatDecisionPanel / CombatDecisionValidationSurface` 已实现；golden label 所有权按 `phase4-uiux-pr05-*` 收口 | `client/src/main/kotlin/com/ktome/client/ui/combat/*`；`docs/opt/ui-pr/README.md` §Golden Label 所有权 | 无偏差（仅需注意与 #12 boss variant 源数据薄弱的耦合——decision surface 很漂亮但被决策对象的语言薄弱所削弱）。 | — |

### 3.1 一致性矩阵小结

- **PASS 项（10）**：#1/2/3/4/5/9-主指标/10/11/14-总量/16/17/19/20 — Phase 4 的大部分合同与指标达成。
- **P0（2）**：#7 build identity dominance、#8 每职业 capstone floor（一个问题的两个测量维度）。
- **P1（3）**：#6 slot 分布、#12/13 boss variant 源数据、#15 hidden 分 zone 倾斜。
- **P2（1）**：#18 sample pack REPLACE 偏离 ADD-first。

三个 P1 与两个 P0 都指向同一个"体验深度"问题——**Phase 4 已经在技术层面把所有系统架好，但在"数据/源内容填充"的厚度上还偏薄**。这不是架构问题，而是内容密度 + 分布曲线的收尾问题，必须在 Phase 4 内补完，否则 V3 PR-02 宣称的 build identity 收口就只是指标层面的"到位"。

---

> Part 2 将在 `docs/review/phase4/v4claude/phase4_opt_deep_review_phase4_codex_part2.md` 展开"玩法体验总评"：核心循环、战斗、成长、奖励、探索、UI 反馈、系统联动七个维度，每个维度按"是否好玩"锚定到具体证据，重点暴露"技术上完成 / 体验上不到位"的伪完成点。
