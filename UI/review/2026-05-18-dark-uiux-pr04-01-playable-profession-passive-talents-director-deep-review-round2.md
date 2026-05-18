# PR04-01 可玩职业被动天赋 — 设计总监 / 系统策划深度复审 Round 2

**Review target**: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`(1270 行 / 118 KB)
**Review date**: 2026-05-18
**Review stance**: 资深 Roguelike / 类 ToME 玩法设计总监 + 系统策划总监 + 玩法体验审查负责人;同时覆盖工程合同、白盒证据、内容包边界与跨 PR 协同。
**Baseline**: 本仓库已合入上一轮 deep review(`2026-05-18-...-director-deep-review.md`)的 HIGH-1 / HIGH-2 / HIGH-3 与 MEDIUM-1~6、LOW-1~6 的全部修订(见 §"Closure check" 表)。
**本轮发现来源**: 文档全文 1270 行通读、与 `core/combat/DiminishingReturns.kt`、`core/combat/DamageType.kt`、`core/resource/ResourceModels.kt`、`core/item/ItemModels.kt`、`core/stats/StatsCalculator.kt`、`game/.../talents/index.yaml` (shadowstep 节)、`game/SessionSnapshotMapper.kt`、`game/FoundationGameSession.kt` 的交叉核验。

---

## Summary

- **修订后是否可推进 `04-01a` 实现**: 可以,但 **§2.3 hp regen 单一 derived path 公式仍漏了 `DiminishingReturns.effectiveHpRegen` 衰减层**,以及 **`castSpeedRating` / `critChance` 的 "rating vs percent" 语义未在 §3.0 EV anchor 计算中折算**;若不补,实现侧大概率出现"测试全绿但 UI 显示与实际治疗 / 施法节奏不一致"。
- **Top risks (本轮)**:
  1. **HIGH-A `hpRegen` 单真源公式止步于 raw 求和,未对接 `DiminishingReturns.effectiveHpRegen(raw, c=80)`**;UI 行 / 实际治疗 / EV anchor 三者各取一处会失真。
  2. **HIGH-B `castSpeedRating` 与 `critChance` 在 §3.1 / §3.0 EV 计算时被默认为线性 percent,但 `StatsCalculator` / `DiminishingReturns` 已经是 hyperbolic rating**;`arcane_overload` rank 5 / `balance_point` rank 5 / `killer_instinct` rank 5 的 EV anchor 隐含线性假设,叠装备后衰减实际收益。
  3. **MEDIUM `shadowstep` 的 MARKED 来源是 `talent.breakpoint.rogue.shadowstep.marked` 分支 breakpoint,不是基础效果**;starter loadout 是否预选该 breakpoint 文档没声明,`deathblow` 在 Rogue 自体可触发性依赖 breakpoint 路径而非"学过 shadowstep 即可"。
- **Approval**: `comment` —— 不阻塞 `04-01a` 启动,但 HIGH-A / HIGH-B 必须在 `04-01b` Task A 数据落地前 freeze 公式与 anchor,否则后续 rebalance 会全面回滚 §3.1 / §3.2 数值。

---

## Affected files

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md` — 本 PR 合同主体 (modified)
- 实现层将影响:
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt` — `PassiveEffect`、`PassiveSource`、`StatModifier` 拓展面 (modified)
  - `core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt` — source-agnostic resolver (modified)
  - `core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt` — `TalentLevelEffect.passiveEffects` (modified)
  - `core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt:117,133,134` — `effectiveCastSpeed`、`hpRegen` 单真源拼装点 (modified)
  - `core/src/main/kotlin/com/ktome/core/combat/DiminishingReturns.kt:24,26` — castSpeed/hpRegen 衰减入口(本 PR 是否调用,文档未声明)
  - `core/src/main/kotlin/com/ktome/core/combat/DamageType.kt:5-12` — 枚举顺序已经是 `PHYSICAL, FIRE, COLD, LIGHTNING, HOLY, SHADOW`,与 §6 / M18 pin 一致;但文档未引用现行 fingerprint
  - `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt` — `TalentPassiveDetailSnapshot` 系列 (modified)
  - `core/src/main/kotlin/com/ktome/core/resource/ResourceModels.kt:7-9` — `MANA / ENERGY / POSITIVE_ENERGY` 资源池基线缺 (依赖项)
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` — passive source 收集、scenario setup hook (modified)
  - `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt` — projection (modified)
  - `game/src/main/kotlin/com/ktome/game/validation/{ValidationScenario.kt, ValidationAction.kt, ValidationSessionOptions.kt, ValidationScenarioRegistry.kt}` — setup/focus owner (modified)
  - `game/src/main/resources/data/talents/index.yaml` — 12 个 talent 重写;`shadowstep` 节内 `shadowstep_marked` 是 breakpoint 而非基础效果 (modified)
  - `game/src/main/resources/i18n/{en-US,zh-CN}.json` — passive label/desc (modified)
  - `client/src/main/kotlin/com/ktome/client/ui/talent/*` — typed passive detail 渲染 + action 抑制 (modified)
  - `tools/src/main/kotlin/com/ktome/tools/whitebox/*` + `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml` (modified)
  - `UI/pr/screen-coverage-matrix.md`, `UI/pr/README.md`,`UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md` (modified)

---

## Root cause & assumptions

Round 1 deep review 把核心问题指向"EV / uptime / 双真源 / 行密度 / stacking",已经被 §3.0、§2.3、M16/M17/M18 全部吸收成 lint + 数据合同。本轮把重点放在 **公式与代码事实的对接**:文档建立的"100 EV anchor 表"、"hpRegen 求和"、"trigger owner"、"line template" 在落地时会撞到三类既有代码事实:

1. `DiminishingReturns` 已经为 castSpeed、critRating、evasion、hpRegen 全部预置 hyperbolic 衰减,且 `StatsCalculator` 已经走 `DiminishingReturns.effectiveCastSpeed(raw)`。PR04-01 §2.3 公式没显式承接,容易让实现/balance 双轨发散。
2. `shadowstep` 的 MARKED 应用走 breakpoint(`descriptionAddendumKey: talent.breakpoint.rogue.shadowstep.marked`、`effectId: shadowstep_marked`),而不是 shadowstep 主体效果;`deathblow` 的 "self-reachable" 隐含 breakpoint 路径。
3. `DamageType` 当前枚举顺序已经是 `PHYSICAL, FIRE, COLD, LIGHTNING, HOLY, SHADOW`,与 §6 / M18 pin 完全一致;但 §6 没把"现行顺序与 pin 顺序一致"作为 closure 写进 self-audit,留 ambiguous 描述。

**本 review 接受的假设**:

- `StatModifier` 是天赋 + 装备共享的同一 `core.item.StatModifier` 数据类;`PassiveEffect.StatModifierEffect.modifier` 直接复用之。
- `TalentAllocationDraft.previousPendingRanks` / `pendingRanks` / `TalentLoadout.slotToTalentId` 是现行字段,§2.6 no-mutation surface 引用它们合法(`SessionSnapshotMapper.kt:909-910, 1227-1228, 1383-1384` 与 `FoundationGameSession.kt:2008-2034` 已验证)。
- §3.0 表里所有职业 / 被动 id 与 `index.yaml` 已存在 talent id 对齐(本 review 抽样了 `shadowstep`、未抽样全部 12 个 id,信任 §3.3 audit)。

**未声明的假设(应明确)**:

- §2.3 公式 `DerivedStats.hpRegen = sum(...)` 之后是否再过 `DiminishingReturns.effectiveHpRegen(rawSum, c=80)`。
- §3.0 EV anchor `rank5NormalizedEv` 计算时,castSpeedRating / critChance 是按 raw 整数 / 直接 percent,还是按 effective hyperbolic 折算。
- `shadowstep` 的 MARKED breakpoint 是否进入"Rogue starter loadout" 的预选(否则 deathblow 在 Rogue 自体不可触发)。
- 资源池(MANA / ENERGY / POSITIVE_ENERGY)起始 maxCap,缺乏池子规模就无法判断 `+2 POSITIVE_ENERGY` 是否触达 §3.0 "Rank 1 必须可感知"。

---

## Findings

### [HIGH] [Correctness / Game Balance] `hpRegen` 单真源公式漏了 `DiminishingReturns.effectiveHpRegen` 衰减层

- **Where**:
  - `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:365-377` (§2.3 "Aggregation and hp regen single-path contract" 公式段)
  - `core/src/main/kotlin/com/ktome/core/combat/DiminishingReturns.kt:7,26` (`DR_HP_REGEN_C = 80.0`, `effectiveHpRegen(rawHpRegen) = hyperbolic(rawHpRegen, 80)`)
  - 文档 §6 example `:1006-1009` 中没有 hp regen 行,但 §2.5 line template `:483-491` 把 `StatModifier.hpRegen` 与 `HpRegenPerTurn` 合到 "shared hp regen line after §2.3 aggregation"
- **Evidence**:
  ```text
  DerivedStats.hpRegen =
    profile.baseHpRegen
    + effectiveStats.con * 0.2
    + sum(passive StatModifier.hpRegen)
    + sum(passive HpRegenPerTurn.amount as Double)
  ```
  - 这是 raw 求和。代码侧 `DiminishingReturns.effectiveHpRegen(raw) = raw*80/(raw+80)`:`raw=1.6` → `effective≈1.569`(偏差 < 2%);`raw=8` → `effective≈7.27`(偏差 ~9%);`raw=20` → `effective≈16.0`(偏差 20%)。
  - 玩家在前 2 层装备少时偏差可忽略;堆完 `pain_fuel` rank 5 (+1.2) + 装备 hpRegen affix(常见 +2~5)后,raw 5~8 区间偏差就开始可感知。
- **Impact**:
  - **UI 数字与实际治疗背离**:文档 §2.5 一行 "hp regen +N" 没说是 raw 还是 effective;客户端按 typed args 渲染 raw 值,但 turn-start tick 走 `effectiveHpRegen`,玩家会发现"面板显示 +2.0,实际每回合只回 1.6"。
  - **§3.0 EV anchor `pain_fuel` = 100 假设线性**:rank 5 `attackMultiplierBonus +0.18` + `hpRegen +1.2`(raw)被记成 100;但若装备 + 被动 + con 求和后 raw 进入 5~10 区间,边际收益接近 50%,EV 实际偏低,与 §3.0 第 4 条 "Rank 5 ... 接近一个中等装备词条组合或一个主动 buff 的长期平均价值" 不可比。
  - **PR04-01 没合一,后续 PR 也不会自动合一**:`HpRegenPerTurn` 仍是装备路径的 first-class kind,如果 §2.3 公式被工程师理解成 "raw 求和即可,不需要 effective",`StatsCalculator` 输出与 `DerivedStats` 命名(`hpRegen` 单字段)产生二义性;`pain_fuel` 落地时大概率走"显示 raw / tick effective"的取舍。
- **Standards**: 单真源 + UI 显示与游戏行为同口径;ARPG 设计中 rating-to-effective 转换必须在 derived 层完成。
- **Recommendation**:
  - §2.3 把公式扩成两步式,显式承接 DR:
    ```text
    rawHpRegen = profile.baseHpRegen
               + effectiveStats.con * 0.2
               + sum(passive StatModifier.hpRegen)
               + sum(passive HpRegenPerTurn.amount as Double)
    DerivedStats.hpRegen = DiminishingReturns.effectiveHpRegen(rawHpRegen)
    ```
    或显式声明 "PR04-01 维持现行 `StatsCalculator` 既有公式;passive 求和等同于 raw 入参,DR 由 `StatsCalculator` 既有路径统一处理"。两者择一,**当前的纯 `sum(...)` 表述会让实现误以为不需要 DR**。
  - §2.5 line template hpRegen 行说明"显示值与 `DerivedStats.hpRegen`(即 effective 后)一致,UI 不展示 raw 求和"。
  - §10 self-audit 增加一条:`hp regen DR routing | DerivedStats.hpRegen 是 effectiveHpRegen(rawSum) 的结果;passive 行显示 effective 数值,不显示 raw 求和`。
- **Tests / Acceptance**:
  - `NEW: PassiveEffectResolverTest.passiveHpRegenSumFedThroughEffectiveHpRegenBeforeDisplay`(给 raw +8, 期望 detail 行显示 `+7.3` 或类似 effective 值,断言 UI typed arg 已是 effective)
  - `NEW: SessionSnapshotMapperTest.painFuelPassiveDisplayValueMatchesEffectiveHpRegenAfterDr`(断言 `pain_fuel` rank 5 + 装备 `+5` 时,显示行 ≠ raw 6.2,而是 ≈ `6.2 × 80 / (6.2+80)` ≈ 5.75)
  - 若改成 "raw 显示 + tick effective" 的取舍,反向断言:`SessionSnapshotMapperTest.painFuelPassiveDisplayValueIsRawSumNotEffective` 并在 `FoundationGameSessionTest` 用一条 explicit `hpTickAppliesEffectiveHpRegenButDisplayShowsRawSum` 守护"显示 ≠ 实际"的故意取舍。这种取舍违背单真源直觉,**不建议**。

---

### [HIGH] [Game Balance / Doc Clarity] `castSpeedRating` 与 `critChance` 的 rating-vs-percent 语义未在 §3.0 EV anchor 中折算

- **Where**:
  - `UI/pr/...passive-talents.md:524-527` (§2.5 stat display 表 `castSpeedRating | signed integer` / `critChance | signed percent`)
  - `UI/pr/...passive-talents.md:594-606` (§3.0 EV anchor 表 — `arcane_overload` 108, `killer_instinct` 102, `balance_point` 104)
  - `UI/pr/...passive-talents.md:638,640,642` (§3.1 数值 — `castSpeedRating +5` / `critChance +0.14` / `castSpeedRating +8`)
  - `core/src/main/kotlin/com/ktome/core/combat/DiminishingReturns.kt:24` (`effectiveCastSpeed = hyperbolic(raw, 100)`)
  - `core/src/main/kotlin/com/ktome/core/combat/DiminishingReturns.kt:22` (`effectiveCritRating = hyperbolic(raw, 200)`)
  - `core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt:117,134` (`castSpeedRating = modifiers.castSpeedRating`, `effectiveCastSpeed = DiminishingReturns.effectiveCastSpeed(castSpeedRating)`)
- **Evidence**:
  - `StatModifier.castSpeedRating: Int` 是 rating,经过 `hyperbolic(raw, 100)` 衰减:+5 → 4.76, +8 → 7.41, +20(满堆装备 + balance_point + arcane_overload + 全 buff)→ 16.67。
  - `StatModifier.critChance: Double` 字面看是 percent("signed percent" 在 §2.5 表),但 `StatsCalculator` 输出 `critChance` 是否还走 `effectiveCritRating(raw)=hyperbolic(raw, 200)`?**§2.5 表没声明**,代码 `critChance: Double` 写法暗示是直接 percent(0.0 ~ 1.0),但 `DiminishingReturns.effectiveCritRating(rawCritRating: Int)` 又留了一个 critRating 入口。
  - 若 `StatModifier.critChance` 是直接 percent(显示按 §2.5 `signed percent` 渲染 `+14%`),那 §3.0 `killer_instinct` rank 5 +0.14 等于直接 +14% crit,可加性叠装备无衰减 → caster bias 反方向再次出现:**rogue crit 是 evergreen 线性,任何 critChance 装备 affix 在 rogue 玩家上是 100% 边际收益**。
  - 若 `StatModifier.critChance` 是 critRating(字面 +14 rating),那 effective = 14*200/(14+200)=13.08(几乎线性,因为 c=200 太大);仍接近 percent,但 EV 评估方法不同。
- **Impact**:
  - **§3.0 EV anchor 隐含线性假设**:`arcane_overload` 108 把 `castSpeedRating +5` 当线性 +5;实际 effective ≈ 4.76,叠装备 +20 后边际 ≈ 0.83 / point,rank 5 → rank 0 边际 ≈ 4 而不是 5。EV 误差在 ±20% 范围内,**目前未越线但临界**。
  - **`balance_point` 与 `arcane_overload` 同 spellblade / arcanist 树堆叠**:不同职业,无 stacking 风险;但 spellblade `balance_point` rank 5 castSpeedRating +8 与装备 affix +N 共存时,玩家堆到 `raw≈30` 后 effective 已经 22.5 → +N 边际 0.5,玩家会发现"再点也没用"。
  - **`critChance` 的 rating-vs-percent 含糊把 `killer_instinct` rank 5 = +14%(直接百分比 cumulative)还是 critRating +14(可加进装备 rating)做成 wave 1 implementation 的选择**;两种实现 EV 差异可达 30%。
- **Standards**: 类 ToME / ARPG 设计 — rating 必须通过 DR 折算成 effective;文档 EV 表应给出 effective 化的 anchor 公式或显式记录 raw-only 假设。
- **Recommendation**:
  - §2.5 stat display 表给 `castSpeedRating` 增加一列 "DR 折算" = `effective = raw * 100 / (raw + 100)`;`critChance` 显式说明是直接 percent 还是 rating(若是 Double,字面 0.0~1.0,应注 "direct probability,no DR")。
  - §3.0 EV anchor 列定义 EV 计算口径:
    - "EV 100 表示 rank 5 effective stat 折算后,与中等装备词条组合的长期平均效用一致;evergreen passive 用 effective × 1.0,conditional passive 用 effective × uptime。"
    - "castSpeedRating EV 贡献按 `effectiveCastSpeed - baseEffective` 计;critChance 直接 percent 视为 EV 贡献 = 数值 × 1.0(假设直接概率)。"
  - 若 critChance 走 critRating 路径,§2.5 表需把 `signed percent` 改成 `signed integer` 并增加 "effective via DR_CRIT_C=200" 说明。
  - §10 self-audit 增加 1 条:`castSpeed / crit rating routing | castSpeedRating 与 critRating 通过 DiminishingReturns hyperbolic 衰减折算,passive 行显示 effective; critChance 是直接 percent,不再走 DR`。
- **Tests / Acceptance**:
  - `NEW: SessionSnapshotMapperTest.arcaneOverloadCastSpeedDisplayUsesEffectiveValue` — `arcane_overload` rank 5 单独装备时,detail 行 `cast speed +5` 应显示 effective `+4.8` 或显式声明"显示 raw rating + 旁注 effective"
  - `NEW: TalentSchemaTest.passiveStatModifierFieldsDocumentDrRouting` — 解析 §2.5 表,断言 `castSpeedRating`、`hpRegen`、`critChance`、`evasion` 等 DR-capable 字段在 §2.5 表里都有 "DR via X" 或 "no DR" 显式注。
  - `NEW: TalentSchemaTest.passiveRank5EvAnchorAccountsForDrInRatingFields` — 重新计算 `arcane_overload`、`balance_point`、`killer_instinct` 的 EV anchor,以 effective(而非 raw)为基础;断言文档表与重算结果误差 ≤5%。

---

### [MEDIUM] [Game Design / Acceptance] `shadowstep` 的 MARKED 是 breakpoint 路径,`deathblow` 自体可触发性需附加 breakpoint 前提

- **Where**:
  - `UI/pr/...passive-talents.md:599` (§3.0 row: `deathblow` trigger owner `shadowstep and marked-target loop own MARKED`)
  - `UI/pr/...passive-talents.md:663` (§3.2 Task B trigger availability map `deathblow` 行:`Rogue shadowstep owns MARKED`)
  - `game/src/main/resources/data/talents/index.yaml:1760-1830` 内 `shadowstep` 节:`shadowstep_marked` 是 `effectId` 出现在 `descriptionAddendumKey: talent.breakpoint.rogue.shadowstep.marked` 的 breakpoint 内,**不在基础 levelEffects**
- **Evidence**:
  - 现行数据:`shadowstep` 基础 levelEffects 只是位移类效果;`shadowstep_marked` 通过 breakpoint(rank-up 选支 / 分支)挂上去。
  - PR04-01 引用 "Rogue shadowstep owns MARKED" 等同 "玩家学了 shadowstep 就有 MARKED";但真实条件是 "玩家在 shadowstep 的 breakpoint 选择 MARKED 路径"。
  - 测试名 `FoundationGameSessionTest.deathblowMarkedSourceIsReachableFromStarterRogueLoadout` 暗示 starter loadout 应当自带可达性。Starter loadout 是否预选 MARKED breakpoint,文档没说;若 starter 不预选,test 会真红 → 实现层被迫修改 starter 默认 breakpoint(违反 PR04-01 "不新增节点、不重排树" 的 §0 invariant)。
- **Impact**:
  - 玩家学完 shadowstep 但没选 MARKED breakpoint → `deathblow` 在 Rogue 自体不可触发,仅靠装备 affix 产生 MARKED → `deathblow` 退化成"装备依赖被动",违背 §3.0 attractiveness floor 第 1 条 "rank 1 必须已经可感知"。
  - PR04-01 当前 lint `conditionalPassivesDeclareTriggerOwnerInDoc` 检测的是文档表里的 trigger owner 文本;它不会验证"trigger owner 的可达性是 starter loadout 自带 vs 选支 vs 装备"。
  - 测试 `deathblowMarkedSourceIsReachableFromStarterRogueLoadout` 若是 "starter loadout = starter active loadout(3 active talents)",且 starter 不含 shadowstep MARKED breakpoint → 永久红;若 starter 已隐含选 breakpoint → test pass 但隐藏假设。
- **Recommendation**:
  - §3.2 Task B trigger availability map 把 `deathblow` 行改成:`Rogue shadowstep MARKED breakpoint(rank ≥ X)owns MARKED; player kill event owns ENERGY restore`,并附 breakpoint 在 `index.yaml` 中的 id / rank 要求。
  - §3.0 trigger owner 列同步精确化。
  - 测试 `deathblowMarkedSourceIsReachableFromStarterRogueLoadout` 在 fixture 内显式声明 "starter Rogue loadout 预设 `shadowstep` 含 MARKED breakpoint at rank 1"(或与之相当的 prereq grant);该 fixture 与 §7 white-box `dark-uiux-pr04-01-passive-action-suppression` 的 `ValidationScenarioTalentSetupSpec.prerequisiteRanks` 协同。
  - §11 Open Risks 加 1 条:`deathblow MARKED 可达性依赖 shadowstep MARKED breakpoint;若该 breakpoint 在未来 PR 移除,deathblow 必须重新选择 trigger 来源`。
- **Tests / Acceptance**:
  - `NEW: FoundationGameSessionTest.shadowstepBreakpointGrantsMarkedAndIsReachableFromStarterLoadout` — 显式覆盖 breakpoint 路径
  - `NEW: TalentSchemaTest.conditionalPassiveTriggerOwnerReferencesExistingTalentOrBreakpointId` — 解析 §3.2 trigger 表,断言每个 trigger owner 引用的 talent / breakpoint id 都在 `index.yaml` 实际存在

---

### [MEDIUM] [UI / Correctness] `ConditionalStatBonus` 渲染行数语义未明确(condition prefix 是 1 行 header / 行内 prefix / 合并 header?)

- **Where**:
  - `UI/pr/...passive-talents.md:487` (§2.5 line template `ConditionalStatBonus` 行:`condition prefix plus one stat line per changed stat`)
  - `UI/pr/...passive-talents.md:496-501` (§2.5 Line density contract:`mana_surge renders 4 current lines, balance_point renders 4 current lines, and no official talent exceeds 5 current lines`)
  - `UI/pr/...passive-talents.md:1117-1119` (§7 first-detail contract `passive-action-suppression` typed assertions 中 condition 写为 `condition=SELF_HAS_STATUS statusId=GUARD statId=defense value=+2`)
- **Evidence**:
  - 文档 "condition prefix plus one stat line per changed stat" 没说 condition prefix 是:
    a) 独立一行 header(如 `While GUARD active:`),后跟 N 行 stat;此时 `bulwark_march` rank 1 = 1 header + 2 stat + 1 TAUNT line = **4 行**(typed args 也要拆 condition 行与 stat 行的 lineKind)
    b) 行内 prefix(如 `GUARD: defense +2` / `GUARD: speed +1`);此时 = 2 + 1 = **3 行**
    c) 合并 header(如 `GUARD: defense +2, speed +1`);此时 = 1 + 1 = **2 行**
  - typed assertion 写 `condition=SELF_HAS_STATUS statusId=GUARD statId=defense value=+2` 在一个 list element 里,**暗示**是 b) 选项;但 `PassiveDetailLineKindSnapshot` 包含独立的 `CONDITIONAL_STAT_BONUS` 与独立的 `STAT_MODIFIER`,**暗示**可能是 a) 选项。
  - §2.5 "no official talent exceeds 5 current lines" 在 b/c) 假设下成立,在 a) 假设下 `bulwark_march` rank 1 仍 = 4 行不破 5;但 §10 self-audit 行 `passive line templates | every supported passive kind has declared current/delta template args and deterministic ordering` 没明确每种 kind 的 lineKind 应当 emit 几条 PassiveDetailLineSnapshot。
- **Impact**:
  - 实现可任选 a/b/c,client 渲染行数与 line density budget 静默漂移;`contractLint.passiveLineCountBudget` 解析 §3 数值表得 `bulwark_march = 3 effects(GUARD-defense, GUARD-speed, TAUNT-damage)`,与 typed snapshot 真实行数 4(if a) 不一致 → lint pass 但实际 UI 超 budget。
  - `lastStandPassiveDetailRendersAttackMultiplierConditionalLine` 在 a) 实现下应有 2 行(condition header + attackMultiplierBonus + defense = 3 行?或 1 + 2 = 3);测试名只点 "ConditionalLine" 单数,实现可能写成 b) 单行也通过。
- **Recommendation**:
  - §2.5 line template 表 `ConditionalStatBonus` 行扩成:
    ```
    | ConditionalStatBonus | one CONDITIONAL_STAT_BONUS header line (e.g., "While GUARD active") + one STAT_MODIFIER line per non-zero stat | header line + one delta STAT_MODIFIER line per changed stat | condition, statusId?, statId, value | condition first (header), then fixed stat order |
    ```
    或选 b) 表述:
    ```
    | ConditionalStatBonus | one CONDITIONAL_STAT_BONUS line per non-zero stat, each prefixed with condition+statusId in args | one delta line per changed stat with same prefix | condition, statusId?, statId, value (single lineKind) | fixed stat order |
    ```
    任选一,但要 freeze。
  - §2.5 Line density contract 加一行:"`bulwark_march` rank 1 渲染 [X] 行 = [Y(condition header / inline prefix)] + [Z stat 行] + [1 TAUNT 行]";给出实际数字。
  - `lastStandPassiveDetailRendersAttackMultiplierConditionalLine` test 名应改成 `lastStandPassiveDetailRendersHpThresholdConditionalLines`(覆盖 attackMultiplierBonus + defense 两 stat,断言 lineKind 序列)。
- **Tests / Acceptance**:
  - `NEW: SessionSnapshotMapperTest.bulwarkMarchPassiveDetailLineKindsMatchSelectedConditionRenderingPolicy`
  - `UPDATE: SessionSnapshotMapperTest.lastStandPassiveDetailRendersAttackMultiplierConditionalLine` → `renderHpThresholdConditionalLines`,覆盖 stat 序列。

---

### [MEDIUM] [Doc Clarity / Implementation Risk] §3.0 trigger owner 行把 `OnKillResourceRestore` 与 `DamageTypeBonus` 共写,容易让实现误以为 "OnKill 只对元素击杀触发"

- **Where**:
  - `UI/pr/...passive-talents.md:597` (§3.0 row `mana_surge` trigger owner: `kill event from any player elemental spell`)
  - `UI/pr/...passive-talents.md:601` (§3.0 row `beacon_of_zeal` trigger owner: `holy damage events from holy_strike / holy_aura; kill event owns POSITIVE_ENERGY restore`)
  - `UI/pr/...passive-talents.md:605` (§3.0 row `flux_anchor` trigger owner: `lightning damage from arcane_edge / flux_burst; kill event owns MANA restore`)
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:60-66`(`PassiveEffect.OnKillResourceRestore(resourceType, amount)`,无 damage type filter)
- **Evidence**:
  - `mana_surge` 实际有两类独立 effects:`OnKillResourceRestore MANA` + `DamageTypeBonus FIRE/COLD/LIGHTNING`。OnKill 对**任何**击杀触发,DamageTypeBonus 在每次该元素伤害事件上加成。
  - §3.0 "kill event from any player elemental spell" 把两者混写,让人误以为 MANA 恢复要求"用元素法术击杀";beacon_of_zeal 的 "holy damage events" 与 "kill event owns POSITIVE_ENERGY restore" 分开写比较清晰,mana_surge 没分。
- **Impact**:
  - 实现工程师阅读 §3.0 后可能误把 mana_surge 的 OnKillResourceRestore 加 damage-type filter(裸 OnKill 不存在该字段,实现可能在 `FoundationGameSession` 击杀处理路径加额外条件),让 MANA 恢复只在元素击杀触发 → §3.0 uptime 0.35 实测降到 ~0.20 → caster identity 进一步弱化。
  - 反向也成立:实现工程师按代码事实(任意击杀)走,白盒 reviewer 看 §3.0 文本以为是元素击杀,manual record 给出错误 expected 结果。
- **Recommendation**:
  - §3.0 trigger owner 拆成两栏 / 多行:
    ```
    | Arcanist | mana_surge | elemental kill-chain
      OnKillResourceRestore MANA: any player kill (no damage-type filter)
      DamageTypeBonus FIRE/COLD/LIGHTNING: each elemental damage event
    ```
  - flux_anchor / beacon_of_zeal / deathblow 同样拆分(虽然这三个目前表述较清,仍统一格式)。
  - §3.2 Task B trigger availability map 同步,加入"trigger filter:任何击杀 / 元素击杀 / 状态目标"列。
- **Tests / Acceptance**:
  - `NEW: FoundationGameSessionTest.manaSurgeOnKillRestoreTriggersOnNonElementalKill` — 用 starter 法师配置,用 melee weapon 击杀,断言 MANA +N 恢复仍触发(防止实现错加 damage filter)
  - `NEW: FoundationGameSessionTest.manaSurgeDamageTypeBonusDoesNotApplyToNonElementalDamage` — 与上互为对偶
  - `NEW: FoundationGameSessionTest.fluxAnchorOnKillManaRestoreTriggersOnNonLightningKill`

---

### [MEDIUM] [UI / Correctness] rank 0 时 "next preview" 的 from→to 范围未明示

- **Where**:
  - `UI/pr/...passive-talents.md:541` (§2.5 PASSIVE detail rule 5: `If current rank is 0, current detail previews rank 1 passive effect.`)
  - `UI/pr/...passive-talents.md:1146` (§7 trigger-passive-detail step 2 expected: `Next preview is expanded and lists MANA restore plus FIRE / COLD / LIGHTNING damage deltas in DamageType enum order.`)
- **Evidence**:
  - rule 5 只说 "current detail previews rank 1",没说 next preview 在 rank 0 时是 rank 0→rank 1 还是 rank 1→rank 2。
  - 白盒 step 2 expected 写 "deltas" 但没给具体数字;CUA reviewer / 实现者各自理解。
  - 若 next preview = rank 0→rank 1:全部正向(无前一级),delta 显示 `+0 → +3`;rank 0 current detail 与 rank 0 next preview 数据完全重复(rank 1 effects)。
  - 若 next preview = rank 1→rank 2:current 显示 rank 1 effects,next 显示 rank 2 - rank 1 增量;符合 §6 example `:1013-1018` 的 `+16 → +22` 增量风格。
  - 实现侧若选前者,玩家在 rank 0 状态下既看不到 "升级后的变化" 也看不到 "学习收益 vs 学习前 0";若选后者,玩家在 rank 0 看到 "rank 1 effects + rank 1→2 deltas",但首次升级行为是 rank 0→1,玩家看到的 delta 与首次升级实际收益不符。
- **Impact**:
  - 玩家在 talent assign 首次选中未学被动时,看到的 "下一级 +6 HP" 含义会因实现选择不同而错位,违反 PR04 typed detail 的"预览所见即学习收益"心智。
  - 测试 `passiveDetailSnapshotContainsCurrentAndNextLines`、`passiveDetailFitsViewportAtMinimumWindow` 都不验 from/to 边界。
- **Recommendation**:
  - §2.5 rule 5 改成:
    ```
    5. If current rank is 0:
       - Current detail previews rank 1 passive effects (the values gained if the player learns now).
       - Next preview displays rank 1 → rank 2 deltas (the values gained on the next rank up after learning).
       - If maxPoints == 1, next preview is hidden.
    ```
  - 或反过来选 "rank 0 → rank 1 deltas" 但与 §6 `+16 → +22` 例样矛盾,**不建议**。
- **Tests / Acceptance**:
  - `NEW: SessionSnapshotMapperTest.unyieldingRank0NextPreviewShowsRank1ToRank2Deltas` — 给 talent rank 0,断言 nextLines 是 rank 1 → rank 2 数值
  - `NEW: SessionSnapshotMapperTest.passiveAtMaxPointsOneHasNoNextPreview` — 边界

---

### [MEDIUM] [Game Balance / Doc] 资源池基线缺,`+N` 资源恢复无法判断"可感知"

- **Where**:
  - `UI/pr/...passive-talents.md:600-606` (§3.0 EV 表 — `mana_surge`、`deathblow`、`beacon_of_zeal`、`flux_anchor` 都基于 OnKillResourceRestore)
  - `UI/pr/...passive-talents.md:651-655` (§3.2 数值 — MANA / ENERGY / POSITIVE_ENERGY 各 +2..+12 区间)
  - `core/src/main/kotlin/com/ktome/core/resource/ResourceModels.kt:7-9` (`MANA / ENERGY / POSITIVE_ENERGY` 三类)
  - 没有显式 maxResource / refill rate / 主动技能消耗基线
- **Evidence**:
  - `mana_surge` rank 1 MANA +3:若 Arcanist 起手 MANA pool = 30, +3 = 10% 池子,可感知;若 pool = 100, +3 = 3%,几乎无感。
  - `beacon_of_zeal` rank 1 POSITIVE_ENERGY +2:Templar pool 完全不知,可能 +2 是 1 个 holy_strike 的成本,也可能等于 0.2 个主动施放。
  - §3.0 EV anchor `beacon_of_zeal = 96` 隐含 "POSITIVE_ENERGY +2 等价 X DPS";没池子规模就无法验证。
- **Impact**:
  - 数值平衡迭代缺基线;若后续职业池子调整(例如 Arcanist 起手 MANA 改成 20 或 50),所有 OnKillResourceRestore 被动的 EV 都跟随漂移,但 §3.0 表保留不变 → lint 仍 pass,玩家实际感受变化。
  - 玩家阅读 detail 时只看到 "+2 POSITIVE_ENERGY 每击杀",无法换算为"打多久才积 1 次 holy_aura"。
- **Recommendation**:
  - §3.0 EV anchor 表后增加 "resource pool baseline" 子节:
    ```
    | resource | starter pool | typical active cost | 1 unit ≈ |
    | MANA | 30 (Arcanist) / 25 (Spellblade) | 6~10 / spell | ~10% pool / 1 unit ≈ 0.13 spell |
    | ENERGY | 40 (Rogue) | 8~15 / active | ~2.5% pool / 1 unit ≈ 0.08 backstab |
    | POSITIVE_ENERGY | 20 (Templar) | 4~8 / blessing | ~5% pool / 1 unit ≈ 0.2 holy_strike |
    ```
    (具体数值用文档真源 `profession/*.yaml` 填写,本 review 未读职业 YAML)
  - 文档里若不放表,§10 self-audit 加一行 `resource pool baseline | values listed for MANA / ENERGY / POSITIVE_ENERGY consistent with profession YAML at PR04-01 close`。
- **Tests / Acceptance**:
  - `NEW: TalentSchemaTest.passiveOnKillResourceRestoreRank1IsAtLeastFivePercentOfStartingPool` — lint 解析职业起手池子与 rank 1 +N,断言 ≥5% 池子(可感知阈值)

---

### [MEDIUM] [Correctness] §2.3 aggregation rule "merges by typed dimension" 没分 kind 给出具体聚合策略

- **Where**:
  - `UI/pr/...passive-talents.md:362-366` (§2.3 Aggregation rule 1:`If two equipped sources expose the same passive kind, resolver output merges by typed dimension while preserving source-aware trace rows where a visible trigger/log event exists.`)
- **Evidence**:
  - "merges by typed dimension" 在不同 kind 有不同含义:
    - `StatModifier`:additive(显然)
    - `ResistanceBonus`:同 damageType additive?有 cap?基础抗性 + N 装备 +M 天赋 = base + sum?
    - `DamageTypeBonus`:同 type additive percent → 1.0 + 5% + 7% = 1.12 vs (1+5%)(1+7%) = 1.1235?
    - `DamageVsStatus`:同 statusId additive percent?
    - `OnKillResourceRestore`:同 resourceType 多源 — 取 max?sum?分别触发?
    - `OnHitStatusProc`:同 statusId 多源 — 取最高 chance?各自独立 roll?
    - `HpRegenPerTurn`:additive(明确)
    - `ConditionalStatBonus`:同 (condition, statusId) 多源 — 同 StatModifier additive?或不同源不同 condition 独立?
    - `TerrainAffinityBonus`:同 terrainTag 多源 — additive?
    - `DamageVsTag`:同 tag 多源 — additive?
  - PR04-01 §2.3 / §2.4 没逐项落字。
- **Impact**:
  - 实现工程师不同 kind 各凭直觉,与"behavior-preserving superset" 合同冲突:现有 `EquipmentPassive` 已有的 `OnHitStatusProc / OnKillResourceRestore / ConditionalStatBonus / TerrainAffinityBonus / DamageVsTag / DamageVsStatus / HpRegenPerTurn / DamageTypeBonus / ResistanceBonus` 在 `PassiveEffectResolver.kt` 已经各自有 merge 规则,但文档没把它们 lift 成合同。后续天赋 + 装备双源时,行为可能与"equipment parity"预期不符却 lint pass。
  - 例:玩家装备 2 件都给 `OnKillResourceRestore MANA +1`,实现 a)sum = +2;实现 b)各自触发 → 同一击杀触发 2 次 log;实现 c)取一件触发 +1。三种实现都"behavior preserving"于当时的孤立测试场景,但 PR04-01 引入 mana_surge 后,装备双源 + 天赋 mana_surge = 三源,行为不同。
- **Recommendation**:
  - §2.3 在 stable-key 表后增加 "Aggregation per kind" 子表:
    ```
    | kind | aggregation across sources | trace policy |
    | StatModifier | additive per stat field | one row per source |
    | ResistanceBonus | additive per damageType, no cap in PR04-01 | one row per source |
    | DamageTypeBonus | additive per damageType (percent stacking is additive, not multiplicative) | one row per source |
    | DamageVsStatus | additive per statusId | one row per source |
    | DamageVsTag | additive per tag | one row per source |
    | OnKillResourceRestore | additive per resourceType (single combined restore event per kill) | one log row per source kind+template |
    | OnHitStatusProc | independent rolls per source (each source rolls chance separately) | one log row per source |
    | ConditionalStatBonus | additive per (condition, statusId?, stat field) when condition active | one row per source |
    | HpRegenPerTurn | additive (collapses with StatModifier.hpRegen per §2.3 single-path rule) | one row per source |
    | TerrainAffinityBonus | additive per (terrainTag, stat field) when terrain matches | one row per source |
    ```
    或显式声明"PR04-01 不改变现有 EquipmentPassive 聚合行为,仅记录现状;未来若改聚合策略需新增 PR"。
- **Tests / Acceptance**:
  - `NEW: PassiveEffectResolverTest.twoEquippedSourcesOnKillRestoreMergeAdditively` — 防止"两件装备各 +1 MANA 但只生效一件" / "触发两次独立 log"
  - `NEW: PassiveEffectResolverTest.equipmentAndTalentDamageTypeBonusUsesAdditivePercentNotMultiplicative` — caster bias 数值取舍

---

### [MEDIUM] [Game Balance] `bulwark_march` uptime 0.50 假设玩家以 guard_stance 高频维持,违背"被动 = 少按一个键"语义

- **Where**:
  - `UI/pr/...passive-talents.md:595` (§3.0 row `bulwark_march` estimated uptime `0.50`)
  - `UI/pr/...passive-talents.md:23` (§0 Design Direction 第 1 条 PASSIVE 解释)
- **Evidence**:
  - GUARD 状态由 `guard_stance` 主动技能授予;PR04 active slot rules 下 `guard_stance` 占主动槽,有 stamina / cooldown 成本。
  - 0.50 uptime ≈ 玩家每 2 turn 维持一次 GUARD;意味着 `guard_stance` 进入高频轮转,几乎是常驻"sustained" 而非偶尔触发。
  - PR04-01 §0 把 PASSIVE 定义为"永久或条件性职业能力,学习后立即生效";但 `bulwark_march` 的条件实际由 active rotation 维持,玩家心智里这是"`guard_stance` + `bulwark_march` 双键 combo"而非"少按一个键"。
- **Impact**:
  - 玩家选择 `bulwark_march` 后,实际操作复杂度 = 选 `unyielding`(完全无脑)+ 一直按 `guard_stance`;两个被动操作差异巨大,违反 §3.0 "一静一动" 设计意图的"动"是"被动触发"而非"被动等待主动维持"。
  - 0.50 uptime 没有 stamina / cooldown 折算;若 `guard_stance` cd = 4 turns、duration = 3 turns,玩家最大 uptime = 3/4 = 0.75;若 cd = 6 turns、duration = 2 turns,uptime = 2/6 ≈ 0.33。文档 0.50 是手测中位还是 cd/duration 计算未说。
- **Recommendation**:
  - §3.0 Value audit 加注:`bulwark_march` 0.50 uptime 是 `guard_stance` cd/duration 比 + 玩家主动维持的合并估算;若 guard_stance 调整,uptime 需同步刷新。
  - 或把 uptime 降到 0.33(保守 cd/duration 比),EV anchor 重算后仍在 [80, 120],可接受。
  - 若 PR04-01 不调,§11 Open Risks 加 1 条:`bulwark_march uptime 假设玩家以 guard_stance 高频轮转;若 guard_stance balance 变化,uptime 与 EV 需重算`。
- **Tests / Acceptance**:
  - `NEW: TalentSchemaTest.bulwarkMarchUptimeMatchesGuardStanceDurationCooldownRatio` — 解析 `guard_stance.yaml` 的 cooldown/duration,与 §3.0 uptime 比对,断言误差 ≤0.15

---

### [MEDIUM] [Doc / Engineering] `EquippedPassiveSource` 长期生命周期未声明何时彻底移除

- **Where**:
  - `UI/pr/...passive-talents.md:779-780` (§4 Task 2 rule 9: `Keep a source-only compatibility adapter only at loader/test boundaries if required for fixture migration; such adapter must be internal, test-covered by equipment parity, and absent from FoundationGameSession runtime paths.`)
  - `UI/pr/...passive-talents.md:1231` (§10 self-audit row `ECS passive stat component | EquipmentPassiveStatModifier no longer exists on production runtime paths`)
- **Evidence**:
  - PR04-01 允许 `EquippedPassiveSource` 作为 internal test-only adapter 保留;但没声明 "PR04-02 / PR-05 must finalize removal"。
- **Impact**:
  - 死代码长期遗留;后续工程师维护 `core.item` 包时不确定 `EquippedPassiveSource` 是否仍是 active code,可能误用。
  - `maintainabilityLint` 在没有显式 baseline 的情况下会持续 nag。
- **Recommendation**:
  - §4 Task 2 rule 9 加一句:"This adapter is fixture-only and must be removed by PR04-02 or by an explicit follow-up `core.passive` cleanup PR. PR04-01 close requires the adapter to be marked `@Deprecated` with removal target PR id."
  - §11 Open Risks 加 1 条:`EquippedPassiveSource internal adapter 在 PR04-01 close 时仍存在; PR04-02 / 后续 cleanup PR owns 最终移除`。
- **Tests / Acceptance**:
  - `NEW: ContractLintTest.internalPassiveSourceAdaptersAreAnnotatedDeprecatedWithRemovalOwner` — 解析 `core.item.*` 中残留的 `EquippedPassiveSource` adapter,断言已加 `@Deprecated` 注解或 KDoc owner 行

---

### [MEDIUM] [Acceptance / Doc] M16 `passiveDetailExplainsEveryHighValuePassivePayoff` 中 "high-value" 未定义

- **Where**:
  - `UI/pr/...passive-talents.md:88` (M16 fastCheck 测试名)
  - `UI/pr/...passive-talents.md:1227` (§10 self-audit row `high-value payoff coverage`)
- **Evidence**:
  - "high-value passive payoff" 在文档主体没明确定义;§10 self-audit 行列了具体 effect kind(resource restore, damage bonus, resistance, conditional stat bonus, attack multiplier, StatModifier.hpRegen, cast speed),但 M16 test name 用 "every high-value" 不精确。
- **Impact**:
  - lint 实现者若把 "high-value" 解读为 "non-StatModifier" 或 "rank 5 EV ≥ 100" 或别的,断言面 / 覆盖面会不同;§10 self-audit 行又像"具体清单",test 实现可能漏检某一项。
- **Recommendation**:
  - §3.0 增加一节 "High-value payoff classification":
    ```
    PR04-01 把以下 PassiveEffect 子类标记为 high-value payoff,detail projection 必须能解释每一条:
    - OnKillResourceRestore (any resource type)
    - ConditionalStatBonus (any condition)
    - DamageVsStatus
    - DamageTypeBonus
    - ResistanceBonus
    - StatModifier.attackMultiplierBonus, StatModifier.hpRegen, StatModifier.castSpeedRating, StatModifier.talentPower
    其余 StatModifier 字段、TerrainAffinityBonus、DamageVsTag、OnHitStatusProc 为 supportive payoff,detail 行存在即可,不强求 KW 模板拆解。
    ```
  - 测试名 `passiveDetailExplainsEveryHighValuePassivePayoff` 改成解析该子节、断言每条 payoff 在 SessionSnapshotMapper 都有 template mapping 与 typed args。
- **Tests / Acceptance**:
  - `NEW: SessionSnapshotMapperTest.highValuePassivePayoffTemplateMappingExistsForEveryClassifiedKind` — 直接对接子节定义

---

### [MEDIUM] [Robustness] §2.6 PASSIVE action matrix 未覆盖 `LEARNABLE` 但有 prereq 未满足的 transient 状态

- **Where**:
  - `UI/pr/...passive-talents.md:548-555` (§2.6 PASSIVE action matrix:`LOCKED / LEARNABLE rank 0 / learned rank < maxRank / learned rank == maxRank`)
- **Evidence**:
  - matrix 只列 LOCKED、LEARNABLE rank 0、已学;没列"prereq 暂未满足、但玩家有足够点数学其他依赖"的 transitional 状态。`TalentSidebarPresenter` 当前的 status enum 可能包含 `LOCKED_PREREQ` / `LOCKED_LEVEL` / `LOCKED_POINTS` 等子状态。
  - PASSIVE 在 `LOCKED_PREREQ` 状态下,玩家按 R 应当与 LOCKED 一致(无 reserve / 无 slot 操作);但 matrix 没说,实现可能误把 `LOCKED_PREREQ` 当 `LEARNABLE` 处理 (因为它有 "if you learn prereq first, this becomes learnable" 心智),允许 reserve attempt → 违背 §2.6 forbidden。
- **Impact**:
  - InputHandler 在 PASSIVE + locked-prereq 状态下若误 emit Reserve,白盒 `passive-action-suppression` 不一定覆盖(scenario 使用 LEARNABLE,不是 LOCKED);log 中可能出现 `ConfirmTalentDraftToReserve` 但因 LOCKED 而被 game 层拒绝,forbidden log fragment 假阳性。
- **Recommendation**:
  - §2.6 matrix 增加 LOCKED 子行:`LOCKED (any subreason: PREREQ / LEVEL / POINTS / FROZEN)` 全部对应 "Esc Back; locked reason 文案" + 显式 forbidden `R Reserve, active slot replacement`。
  - 或者明确 "PASSIVE + LOCKED any subreason 与 LOCKED 同等处理"。
  - 增加白盒 / focused test:`InputHandlerTest.passiveTalentLockedByPrereqDoesNotEmitReserveCommand`。
- **Tests / Acceptance**:
  - `NEW: InputHandlerTest.passiveTalentLockedByPrereqDoesNotEmitReserveCommand`
  - `NEW: TalentSidebarPresenterTest.passiveLockedByLevelShowsLockedActionsOnly`

---

### [LOW] [Doc / Test Naming] `lastStandPassiveDetailRendersAttackMultiplierConditionalLine` 漏 `defense` 子项

- **Where**:
  - `UI/pr/...passive-talents.md:81,890` (M09 / Task 5 validation list)
  - `UI/pr/...passive-talents.md:654` (§3.2 row `last_stand`:`attackMultiplierBonus +0.10, defense +2`)
- **Evidence**:
  - last_stand 每个 rank 都同时有 attackMultiplierBonus + defense 两个 stat,测试名只点 "AttackMultiplier",覆盖意图不全。
- **Recommendation**:
  - 改名 `lastStandPassiveDetailRendersHpThresholdConditionalLines`(复数),断言 condition prefix + attackMultiplierBonus 行 + defense 行 全部存在且顺序符合 §2.5 stat order(defense in HUD-visible 顺序 比 attackMultiplierBonus 早,需要明确 stat order 对 conditional 也适用)。
- **Tests / Acceptance**:
  - `RENAME / EXPAND: SessionSnapshotMapperTest.lastStandPassiveDetailRendersHpThresholdConditionalLines`

---

### [LOW] [Doc Clarity] 白盒 evidence 文件名 `passive-static-panel-entry.png` 在执行顺序中误导

- **Where**:
  - `UI/pr/...passive-talents.md:118` (Canonical Artifact `evidence/passive-static-panel-entry.png` 注:`证明预览折叠后仍聚焦 unyielding 且无 R Reserve`)
  - `UI/pr/...passive-talents.md:1143` (§7 CUA step 3 `P` 折叠后 evidence = `evidence/passive-static-panel-entry.png`)
- **Evidence**:
  - 文件名暗示"刚进入 panel",但实际是 `F9, Enter, Esc, T, P, P` 后(展开过再折叠)的截图。
- **Impact**:
  - Reviewer 看 evidence 标题以为是初始进入态;manual record 撰写时可能与实际抓图时机不符。
- **Recommendation**:
  - 改名 `evidence/passive-static-preview-collapsed-after-toggle.png` 或 `passive-static-preview-reset.png`;在 §7 evidence 表与 Canonical Artifact 同步。
- **Tests / Acceptance**:
  - 仅文档/文件名修改;无新测试

---

### [LOW] [Whitebox Coverage] 白盒 trigger scenario 缺 `log.passive.on_kill_resource_restore` 作为 `requiredLogEventKeys`

- **Where**:
  - `UI/pr/...passive-talents.md:352` (§2.3 trace contract rule 5:emit `log.passive.on_kill_resource_restore` 等 source-aware 事件)
  - `UI/pr/...passive-talents.md:1132-1135` (§7 Log assertion contract — 三个 scenario 的 `requiredLogEventKeys` 都只有 `log.validation.phase4_v4.action`, `log.talent.learned`)
- **Evidence**:
  - trigger-passive-detail scenario 没击杀 step,所以无 OnKill log;但 §2.3 声明的 source-aware log key 在任何白盒 evidence 都不出现,说明该 log 路径"声明了但白盒不覆盖"。
- **Impact**:
  - source-aware log 的实现是否真的 emit / 路由正确,完全靠 focused test (`talentPassiveOnKillRestoreUsesTalentSource`) 覆盖;白盒无法做"端到端真实战斗中触发" 的验证。
- **Recommendation**:
  - 至少给 `trigger-passive-detail` 加一个 in-game kill evidence step(走 D-pad 到敌人、Space 攻击、击杀)→ evidence `passive-trigger-on-kill-log.png` 与 `requiredLogEventKeys += [log.passive.on_kill_resource_restore]`;或显式在 §11 Open Risks 写 "source-aware passive trigger log 的端到端验证由 focused FoundationGameSessionTest 拥有,不在 PR04-01 白盒 evidence 内"。
- **Tests / Acceptance**:
  - 可选:增补 cua step + evidence;或不变,显式在 Open Risks 写 deferred

---

### [LOW] [Doc] `previousPendingRanks` / `pendingRanks` / `slotToTalentId` 字段未在 PR04-01 文档自给定义,完全依赖 PR04 上游

- **Where**:
  - `UI/pr/...passive-talents.md:561-562` (§2.6 No-mutation assertion surface 列字段)
  - `UI/pr/...passive-talents.md:1240` (§10 self-audit `passive actions` 行)
- **Evidence**:
  - 字段名直接引用,但 PR04-01 文档没给出 `TalentAllocationDraft` 字段定义或对接位置(PR04 视为上游)。本仓库代码确认 `SessionSnapshotMapper.kt:909-910, 1227-1228, 1383-1384` 已使用这些字段。
- **Impact**:
  - 实现者若不熟 PR04 上游,看 §2.6 测试名后需另查 PR04 文档定位字段定义;增加上下文负担,但不阻塞实现。
- **Recommendation**:
  - §2.6 段首加引用注 `(see PR04 talent assign panel: TalentAllocationDraft.pendingRanks / previousPendingRanks 与 TalentLoadout.slotToTalentId 由 PR04 拥有)`,并附 `core/save/TalentAllocationDraftSnapshot` 路径作为典型 owner。
- **Tests / Acceptance**:
  - 无新测试;文档引用补充

---

### [LOW] [Doc] PassiveCondition 可选值未在 PR04-01 文档列出

- **Where**:
  - `UI/pr/...passive-talents.md:200-205` (§2.1 `PassiveEffect.ConditionalStatBonus` 引用 `condition: PassiveCondition`)
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:133-138` 现行 `PassiveCondition { HP_BELOW_50, HP_BELOW_30, HP_ABOVE_80, SELF_HAS_STATUS }`
- **Evidence**:
  - PR04-01 引用 `HP_BELOW_30` / `SELF_HAS_STATUS`,但完整 enum 未列。实现者需翻 `ItemModels.kt`。
- **Impact**:
  - Future review 难判断 "PR04-01 是否允许 HP_BELOW_50 或 HP_ABOVE_80",虽然 §3 数值表没用到。
- **Recommendation**:
  - §2.1 给 `PassiveCondition` 列出当前所有 variants:
    ```
    Supported PassiveCondition variants (PR04-01 frozen list):
    - HP_BELOW_50, HP_BELOW_30 (player HP ratio condition)
    - HP_ABOVE_80 (high-HP condition)
    - SELF_HAS_STATUS (status-id-paired; requires statusId field)
    Content packs cannot declare new PassiveCondition values (§5).
    ```
- **Tests / Acceptance**:
  - 无新测试;文档补充

---

### [LOW] [Game Design] PR04-01 沿用既有 active skill icon 渲染 PASSIVE,未在 Open Risks 显式承认视觉债

- **Where**:
  - `UI/pr/...passive-talents.md:8` (`PR04-01 不新增 reference-crop 资源,正式全量 skill icon rebaseline 仍归 PR-06`)
  - `UI/pr/...passive-talents.md:1191-1196` (§8 Cross-PR Dependencies)
  - `game/src/main/resources/data/talents/index.yaml:1763-1764` 例:`shadowstep` icon key 为 `icon.skill.rogue.shadowstep`(active style)
- **Evidence**:
  - 12 个被转 talent 仍用既有 icon;icon 视觉风格(主动剑舞、火球、护盾启动)与"PASSIVE 永久 / 条件性"心智不符。
  - PR04 right detail hero icon + Talent Assign 列表行 icon 都会显示原 active icon。
- **Impact**:
  - 玩家在 PR04-01 落地后看到 "passive 行用主动技 icon",可能与 type line `Passive` 文字冲突,理解为"我点这个会怎样使用?"。
  - 不阻塞实现,但视觉债显式声明 = 后续 PR06 owner 不会被忽略。
- **Recommendation**:
  - §11 Open Risks 加 1 条:`PR04-01 不重绘 icon;12 个被转 PASSIVE talent 沿用既有 active icon 直到 PR06 rebaseline。type line "被动" 文案与 icon 主动风格的视觉张力,是已知视觉债。`
- **Tests / Acceptance**:
  - 无新测试

---

### [LOW] [Doc] §2.1 kind coverage 表 `StatModifier` "equipment parity = yes" 暗示有装备数据,实际无

- **Where**:
  - `UI/pr/...passive-talents.md:263` (§2.1 kind coverage 表 `StatModifier | Task A | yes | yes`)
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:40-105` (`EquipmentPassive` sealed interface 子类列表不含 `StatModifier`)
- **Evidence**:
  - 现行 `EquipmentPassive` 没有 `StatModifierEffect`;装备给 stat 通过 `ItemInstance.stats` 或 affix 走,非 passive 路径。
  - PR04-01 引入 `PassiveEffect.StatModifierEffect` 是新增能力(forward-compatible);"equipment parity = yes" 暗示 "现有装备 schema 已用"。
- **Impact**:
  - reviewer / 实现者会去找现有装备的 StatModifier passive 数据来构造 parity test,结果找不到;实际 parity 是"代码上能映射,数据上无现行 fixture"。
- **Recommendation**:
  - §2.1 kind coverage 表 `StatModifier` 行的 "equipment parity" 列改成 `future-compatible (no current equipment data uses StatModifier as a passive)`,或 `loader-mapped (no current equipment data)`。
- **Tests / Acceptance**:
  - 无新测试;`equipmentPassiveParityCoversAllExistingKinds` 测试范围天然不含 StatModifier(因为现行没数据)

---

### [LOW] [Doc] §6 display rule 不涵盖负数 stat modifier(未来 nerf 或交换设计)

- **Where**:
  - `UI/pr/...passive-talents.md:1022-1027` (§6 Display rules)
- **Evidence**:
  - 所有 rule 都是 `+N` / `+N%` 正向格式;若未来某 passive 引入"+10% damage, -5 defense"交换型,§2.5 表 `signed integer` 字面允许负数,但 §6 display rule 没明确 `-5` 如何渲染(`- 5` / `-5` / 「降低 5」),locale token 是否复用同一标签。
- **Impact**:
  - 未发生时无 impact;若未来 PR 加负数 stat,locale 与渲染需新增 negative branch。本 PR 无需修复,但 §10 self-audit 应认领"不覆盖负数"。
- **Recommendation**:
  - §6 末尾加一行:`PR04-01 数值表均为正向 (+N / +N%);负数 stat modifier 的本地化与渲染规则将在引入第一个含负数 passive 的 PR 中补合同。`
  - §10 self-audit 加一行:`negative stat modifier policy | PR04-01 不涉及负数 stat modifier;未来引入需新增 §6 negative rendering rule 与 locale labels`
- **Tests / Acceptance**:
  - 无新测试

---

### [NIT] CUA `F9, Enter, Esc, T` key 序列缺逐键语义注释

- **Where**:
  - `UI/pr/...passive-talents.md:1141-1152` (§7 CUA steps)
- **Evidence**:
  - `F9, Enter, Esc, T` 来自 validation mode 约定;reviewer / CUA executor 必须翻 ValidationScenarioRegistry 与 phase4-v4 governance 才知道每个键义。
- **Recommendation**:
  - §7 cua step 表后加一段 key glossary:`F9 = enter Phase4 v4 validation prep menu; Enter = confirm prep; Esc = return to map; T = open Talent Assign panel; P = toggle next-rank preview; R = (suppressed for PASSIVE) reserve action; Enter (in tree) = learn / rank up`。

---

### [NIT] `attackMultiplierBonus` / `talentPower` 在伤害公式中的 additive vs multiplicative 语义未在 §3.0 注

- **Where**:
  - `UI/pr/...passive-talents.md:677` (§3.2 Same-dimension stacking audit:`combined same-dimension peak is +48%, below PR04-01 passive soft cap +50%`)
- **Evidence**:
  - `pain_fuel +0.18 attackMultiplierBonus` 与 `last_stand +0.30 attackMultiplierBonus` 同 dimension。"+48%" 隐含 additive(0.18+0.30=0.48);若实际公式是 `(1+a)*(1+b)-1 = 0.534`,则 cap "+50%" 实际被穿透。
- **Recommendation**:
  - §3.2 末尾注一句:`Same-dimension stacking 假设 attackMultiplierBonus / talentPower 在伤害公式中为 additive percent(即 final = base*(1 + sum(bonuses)));若 StatsCalculator 实际是 multiplicative,§3.2 cap 必须重算。`
  - 或显式在 §2.3 / §2.5 提到公式形态。

---

## Performance / Operations

- **Hotspots**: passive collection 仍是 (#equipped passives + #learned talents) 线性遍历,常态 ≤ 12,无瓶颈。
- **Complexity notes**: `SessionSnapshotMapper` projection 在 talent assign 视图打开时按 right-pane 重绘频率执行;`bulwark_march` 这类 conditional 在 GUARD 状态变化时刷新 derived stats,但 projection 仅当 right-pane 焦点切换时执行,无每帧开销。建议 §2.4 在"required refresh"加一行:"GUARD 状态过期 / 重新施加触发 projection re-emit"(已隐含在 `conditionalTalentPassiveRefreshesWhenStatusExpires` 测试中)。
- **Bench / Monitoring**:`:client:goldenScreenshot` 在 PR04-01 三个 scenario × 5 evidence ≈ 15 截图,timing 应在 baseline + ≤200ms。

---

## Integration

- **API / contracts**:
  - `TalentLevelEffect.passiveEffects = emptyList()` 默认值兼容旧 talent;PR04-01 12 个 talent 强制非空。
  - `PassiveSource` 取代 `EquippedPassiveSource` 是 breaking runtime change;Task 2 rule 8 已枚举所有 caller(`FoundationGameSession`, `StatsCalculator`, combat damage adjustment, on-hit/on-kill trigger, log/trace, save/load),但 §5 contract change 表没单独列 `EquippedPassiveSource` 删除/降级,只列 `EquipmentPassiveStatModifier` rename。建议补一行。
  - `PassiveDetailLineKindSnapshot` enum 与 `PassiveDetailLineToneSnapshot` enum 是 `@Serializable`,序列化兼容性:PR04-01 引入后,后续添加新 lineKind 需保证 deserialization fallback(client 端读不到新枚举时怎么办?当前 `core.snapshot` 序列化是否带 fallback policy?)。建议 §5 加一行 `RenderSnapshot enum forward compatibility | new lineKind/tone variants 需走 RenderSnapshot 既有 fallback policy(若存在)或在新增时同步刷新 client 版本`。

- **DB / save migrations**:无,§5 已声明。

- **Feature flags / rollout**:无显式 flag;§9 rollback 走 commit revert。建议:PassiveEffect 路径较关键,生产可加 `ktome.passive.unified.resolver.enabled`,默认 true,允许在数据不稳时回退。文档当前未声明此 toggle 需求,可不强求。

- **Resilience**:resolver 改造期间 parity 测试是唯一防线;`04-01a` blocking gate 合理。

- **Rollback plan**:§9 与 §0 Execution Slices 一致;但"`04-01a` 单独保留" 的语义需要 `EquipmentPassive` schema 仍可被 loader 接收(向后兼容)。文档 §2.1 rule 7 已说 "loader 边界 convert",这条仍 OK。

---

## Testing

- **Coverage**:本轮文档新增 M16/M17/M18 + 大量 lint 与 typed assertion,合同覆盖度已经很高。

- **Gaps (本轮新加)**:
  1. hpRegen DR 路由(HIGH-A)
  2. castSpeed/critChance DR 路由(HIGH-B)
  3. shadowstep MARKED breakpoint 可达性(MEDIUM)
  4. ConditionalStatBonus line kind 数量(MEDIUM)
  5. OnKillResourceRestore filter 语义(MEDIUM)
  6. rank 0 next preview 边界(MEDIUM)
  7. 资源池基线(MEDIUM)
  8. Aggregation per kind 显式策略(MEDIUM)
  9. PASSIVE + LOCKED subreason 行为(MEDIUM)

- **Flakiness risks**:
  - `passiveDetailFitsViewportAtMinimumWindow` 1280x720 vs `1280x840`(scenario window)差 120px;若 viewport 测试在 720 高度下 conditional + delta 8 行刚好溢出,会真红;白盒 840 高度下能放下,golden 通过 → flakiness on viewport assumption。
  - `damageTypeEnumOrderPinned` 已通过 `DamageType.kt` 当前枚举顺序验证(本 review 确认 `PHYSICAL, FIRE, COLD, LIGHTNING, HOLY, SHADOW` 一致);未来如果 enum 顺序漂移,lint 会真红 → 这正是 pin 的目的,但实现者第一次 run lint 前应 acknowledge 当前 fingerprint。

- **Targeted test plan additions**(Given / When / Then):
  - **Given** 玩家 `pain_fuel rank 5 (hpRegen +1.2)` + 装备 `HpRegenPerTurn +5` + con=30(等价 +6),raw hpRegen ≈ 1.2 + 5 + 6 = 12.2,**When** turn-start,**Then** 实际治疗量 = `effectiveHpRegen(12.2) = 12.2*80/(12.2+80) = 10.58`(不是 raw 12.2);UI 显示行也应是 10.58,**或**显式说明显示 raw 12.2 与实际 10.58 不同(两者择一,目前文档不明)。
  - **Given** Rogue starter loadout,**When** 学 shadowstep rank 1(基础位移效果,无 breakpoint 选支),**Then** 击杀敌人后 deathblow 的 `DamageVsStatus MARKED bonus` **不触发**(因为 MARKED 未被赋);除非 starter loadout 隐含选 MARKED breakpoint,实测应红 → 文档 §3.2 trigger map 必须显式声明 breakpoint 前提。
  - **Given** 玩家 `bulwark_march rank 5`,GUARD 状态生效中,**When** projection re-emit,**Then** detail current line 中 `bulwark_march` 行展示 active state(可能用 POSITIVE tone);GUARD 过期后,**Then** detail current line 仍展示数值但用 SECONDARY tone(或类似"未激活"提示)。当前 §2.5 `PassiveDetailLineToneSnapshot { SECONDARY, POSITIVE, WARNING }` 暗示有 tone 分支,但 conditional 是否 tone-switch 没明说。
  - **Given** 装备 2 件都给 `OnHitStatusProc statusId=BLEED, chance=0.1, duration=2`,**When** 攻击命中,**Then** 触发概率:additive (1 - (1-0.1)*(1-0.1) ≈ 0.19)? max (0.10)? sum (0.20 capped at 1.0)? — `equipmentPassiveParityCoversAllExistingKinds` 应该已隐含答案,但 PR04-01 文档没把它 lift 成显式合同。

---

## Docs & Observability

- **Docs to update / create**:
  - §2.3 / §2.5 / §3.0 三处补 DR routing 与 line-kind / rank-0 / resource-pool 等本轮 finding 修订(必做);
  - §3.0 后增 "high-value payoff classification" 子节(MEDIUM-I);
  - §3.0 后增 "resource pool baseline" 子节(MEDIUM-E);
  - §3.2 trigger availability map `deathblow` 行增 breakpoint id(MEDIUM-A);
  - §2.6 matrix 增 LOCKED subreason 行(MEDIUM-J);
  - §2.3 增 "Aggregation per kind" 子表(MEDIUM-H);
  - §10 self-audit 同步增对应行;
  - §11 Open Risks 增 `bulwark_march uptime 假设`、`EquippedPassiveSource adapter 长期生命周期`、`PR04-01 passive icon 沿用 active style`、`OnHitStatusProc multi-source roll policy(若 §2.3 显式选了)`。

- **Logs / Metrics / Traces**:
  - 建议给 source-aware log 加 `log.passive.conditional_bonus_activated` 与 `log.passive.conditional_bonus_deactivated` 写入 `conditional-passive` 类 scenario 的 `requiredLogEventKeys`(目前只在 §2.3 trace contract 文本里出现,白盒无强制)。

- **Runbook**:无更新。

---

## Open questions

1. **HIGH-A 选择题**:PR04-01 选 "DerivedStats.hpRegen 是 effectiveHpRegen(rawSum) 的结果,UI 行显示 effective" 还是 "DerivedStats.hpRegen 是 raw 求和,turn-start tick 单独走 effective,UI 行显示 raw"?后者会让 UI 数字与实际治疗不一致,违反单真源直觉,但若现行 `StatsCalculator` 已经是后者,PR04-01 也许继承现状即可 — 需要在文档明写。
2. **HIGH-B 选择题**:`StatModifier.critChance: Double` 是直接 percent (0.0~1.0) 还是 critRating(整数化的 rating)?现行代码字段类型 Double 偏向 percent;但 `DiminishingReturns.effectiveCritRating(Int)` 入口又留了一个 rating 路径。两者择一。
3. **MEDIUM-A 选择题**:starter Rogue loadout 是否包含 `shadowstep` 的 MARKED breakpoint 自动预选?或者 PR04-01 接受 "deathblow 在 Rogue starter 完全不可触发,要点 breakpoint 后才能触发" 这一玩法体验?若接受,§3.0 attractiveness floor 第 1 条 "rank 1 必须已经可感知" 在 deathblow 上会真假平衡。
4. **MEDIUM-D 选择题**:rank 0 next preview 选 `rank 1 → rank 2`(与 §6 example 一致)还是 `rank 0 → rank 1`(直观但 current detail 与 next preview 数据重复)?
5. **MEDIUM-H 选择题**:`OnHitStatusProc` 多源 chance 走 additive cap、independent rolls 还是 max?这直接决定多件装备触发面;PR04-01 是 "behavior-preserving",但 behavior 在文档里不显式。
6. PR04-01 12 个被转 talent 是否需要在 PR04-01 close 时 owner 提交 "active rotation playtested with passive conversion" 的 manual record?目前 §3.3 conversion audit 是 paper review,没要求 in-game playthrough 录像。

---

## Final recommendation

- **Decision**: **comment** —— 可推进 `04-01a` parity 实现;但 HIGH-A / HIGH-B 必须在 `04-01b` Task A 数据落地前完成文档修订,否则数值平衡迭代会撞到代码事实(`DiminishingReturns` 已存在)而推翻 §3.0 EV anchor 表。

- **Must-fix before `04-01b` Task A 启动**:
  1. [HIGH-A] §2.3 hpRegen 公式扩成 "raw → effective" 两步式,显式承接 `DiminishingReturns.effectiveHpRegen`;§10 self-audit 增 `hp regen DR routing` 行;新增 `painFuelPassiveDisplayValueMatchesEffectiveHpRegenAfterDr` 类测试。
  2. [HIGH-B] §2.5 stat display 表给 castSpeedRating / critChance / evasion 增 "DR routing" 列;§3.0 EV anchor 计算口径显式声明 raw vs effective;新增 `arcaneOverloadCastSpeedDisplayUsesEffectiveValue` 类测试。
  3. [MEDIUM-A] §3.2 trigger availability map `deathblow` 行写明 shadowstep MARKED breakpoint id / rank 要求;`deathblowMarkedSourceIsReachableFromStarterRogueLoadout` fixture 显式声明 breakpoint 预选。
  4. [MEDIUM-B] §2.5 line template ConditionalStatBonus 行 freeze 渲染 lineKind 结构(header 1 行 + N stat 行 还是 inline prefix N 行);更新 `lastStand...` 测试名与覆盖。
  5. [MEDIUM-C] §3.0 trigger owner 拆分 OnKillResourceRestore filter / DamageTypeBonus filter,防止"任何击杀 vs 元素击杀" 实现取舍。
  6. [MEDIUM-D] §2.5 rule 5 显式写 rank 0 next preview = rank 1 → rank 2。
  7. [MEDIUM-E] §3.0 增 resource pool baseline 子节,锚定每个职业起手 MANA/ENERGY/POSITIVE_ENERGY pool 与 1 unit 占比。
  8. [MEDIUM-H] §2.3 增 "Aggregation per kind" 子表,落字现有 EquipmentPassive 各 kind 的多源合并策略。
  9. [MEDIUM-I] §3.0 增 "high-value payoff classification" 子节,定义 M16 lint 测试范围。

- **Nice-to-have post-merge / 后续 PR**:
  - [MEDIUM-F] `bulwark_march` uptime 与 `guard_stance` cd/duration 比对(可在数值 playtest 后再调,Open Risks 显式认领即可)。
  - [MEDIUM-G] `EquippedPassiveSource` adapter 的最终移除 owner 由 PR04-02 / 后续 cleanup PR 显式承接。
  - [MEDIUM-J] PASSIVE + LOCKED subreason 行为 matrix 扩充。
  - [LOW-A~H] 测试名 / 文件名 / 文档补全。
  - [NIT] CUA key glossary 与 attackMultiplier additive/multiplicative 注。

- **Confidence**: medium-high —— 本轮 review 是基于 Round 1 修订全部吸收的二轮深挖,findings 集中在"公式与代码事实对接"以及"细节文案模糊"层面,不存在结构性失败。HIGH-A / HIGH-B 是必须落字的工程合同细节,但选择题简单(选 effective 化或 raw 化,任选明写即可)。Round 2 修订完成后 PR04-01 应可一次性走完 `04-01a → 04-01b → 04-01c`。

---

## Suggested verification(文档修订后)

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

Implementation 期 owner gates(本轮新增测试名应进入):

```bash
./gradlew :core:test --tests '*PassiveEffectResolver*' \
                     :game:test --tests '*Passive*' --tests '*Talent*'
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest \
                       --tests com.ktome.client.ui.talent.DescriptionPresenterTest \
                       --tests com.ktome.client.input.InputHandlerTest
./gradlew contractLint localeLint maintainabilityLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression
./gradlew verifyChanged
```

本审查未运行 Gradle;仅读了文档 (1270 行) 、Round 1 deep review (`2026-05-18-...-director-deep-review.md`) 与若干生产源文件(`DamageType.kt`, `DiminishingReturns.kt`, `StatsCalculator.kt`, `ItemModels.kt`, `ResourceModels.kt`, `index.yaml` shadowstep 节, `SessionSnapshotMapper.kt`, `FoundationGameSession.kt`)以确认代码事实。

---

## Closure check vs Round 1 deep review

| Round 1 finding | severity | 本轮状态 |
| --- | --- | --- |
| HIGH-1 cross-passive EV 预算未横向校准 | HIGH | **已关闭**(§3.0 EV anchor `[80,120]` + `rank5NormalizedEv` 列 + M16 `passiveRank5EvAnchorWithinTwentyPercent`、`conditionalPassiveUptimeIsDeclaredInDoc`、`conditionalPassivesDeclareTriggerOwnerInDoc`;devotion HOLY res 移除,beacon_of_zeal 独占 HOLY 抗性) |
| HIGH-2 StatModifier.hpRegen 与 HpRegenPerTurn 双轨 | HIGH | **形式上关闭**(§2.3 "single-path contract" + M17 + `hpRegenStatModifierAndHpRegenPerTurnCollapseIntoSingleDerivedStat`),**但漏 DR 衰减层** → 本轮 HIGH-A 新增 |
| HIGH-3 条件 / 触发型被动 uptime 与可用性 | HIGH | **已关闭**(§3.0 uptime 列 + trigger owner 列 + §3.2 Task B trigger availability map + 三条 `*MarkedSourceIsReachableFromStarter*` / `*HolyDamageSourceIsReachable*` / `*LightningSourceIsReachable*` 测试),**但 deathblow MARKED 来源是 breakpoint 而非基础效果** → 本轮 MEDIUM-A 细化 |
| MEDIUM-1 UI 行密度 | MEDIUM | **已关闭**(§2.5 "Line density contract" + M18 budget lint + `passiveDetailLineCountPerTalentDoesNotExceedTen` + `passiveDetailFitsViewportAtMinimumWindow`),**但 ConditionalStatBonus 行 kind 数量模糊** → 本轮 MEDIUM-B 细化 |
| MEDIUM-2 Aggregation policy | MEDIUM | **形式上关闭**(§2.3 "Aggregation and hp regen single-path contract" 三条),**但 per-kind 聚合策略仍模糊** → 本轮 MEDIUM-H 细化 |
| MEDIUM-3 同维度 stacking cap | MEDIUM | **已关闭**(§3.2 "Same-dimension stacking audit" + `sameDimensionPassiveStackPerProfessionDoesNotExceedSoftCap`),**但 attackMultiplier additive vs multiplicative 未注** → 本轮 NIT |
| MEDIUM-4 DamageType enum 顺序 pin | MEDIUM | **已关闭**(§6 rule 5 显式 `PHYSICAL → FIRE → COLD → LIGHTNING → HOLY → SHADOW` + M18 + `contractLint.damageTypeEnumOrderPinned`;代码侧本 review 确认当前枚举顺序与 pin 完全一致) |
| MEDIUM-5 内容包升级静默 rebalance | MEDIUM | **政策性关闭**(§5 "Talent data revision policy" + §11 Open Risk #7) |
| MEDIUM-6 starter active count | MEDIUM | **已关闭**(§3.3 表 `activeSlotCount=4` / `starter active count after conversion=3` + `starterLoadoutFitsInActiveSlotCountAfterPr04_01Conversion`) |
| MEDIUM-7 白盒 in-game trigger 缺 | MEDIUM | **deferred + 显式认领**(§7 "Whitebox scenarios prove player-visible Talent Assign detail ... they do not replace runtime trigger tests"),**但 `requiredLogEventKeys` 仍可补 source-aware log 行** → 本轮 LOW-D 微调 |
| LOW-1 active verb budget undefined | LOW | **已关闭**(§10 self-audit 改成 `conversion audit parity`) |
| LOW-2 stat order undefined | LOW | **已关闭**(§2.5 表 + `passiveDetailStatOrderMatchesDocumentedTable`) |
| LOW-3 LocaleLint NonStat | LOW | **已关闭**(`LocaleLintTest.passiveDetailNonStatLabelsExist`) |
| LOW-4 beacon_of_zeal rank 1 POSITIVE_ENERGY +1 | LOW | **已关闭**(rank 1 = +2,EV anchor 有 `rank 1 POSITIVE_ENERGY restore is at least +2` blocking) |
| LOW-5 cue key cleanup | LOW | **已关闭**(§2.3 rule 4) |
| LOW-6 SUSTAINED 边界 lint | LOW | **已关闭**(`contractLint.pr04_01ConvertedTalentsAreExactlyPassive`) |

Round 1 所有 findings 都已被 PR04-01 文档吸收,本轮新发现集中在:
1. 与代码事实(`DiminishingReturns` 衰减、shadowstep breakpoint)对接的合同细节;
2. ConditionalStatBonus / OnKillResourceRestore / rank 0 next preview 这类小 UI / 行为语义模糊;
3. 资源池基线、aggregation per kind、high-value payoff classification 这类锚定缺失。

---

## Summary(本轮一句话)

PR04-01 在 Round 1 修订后,**工程合同 + 设计 attractiveness + EV anchor + uptime 申明 + line density + DamageType pin** 已全部落字;但 **代码层 `DiminishingReturns` 对 hpRegen / castSpeed / crit 的衰减是既有事实**,文档 §2.3 / §3.0 / §3.1 当前仍按线性 raw 计算 — `04-01b` Task A 数据落地前必须 freeze "DR 路由"两步式公式;同时 `shadowstep` 的 MARKED 来源是 breakpoint 路径,`deathblow` 自体可触发性需把 breakpoint 前提显式写到 §3.2 trigger availability map;`ConditionalStatBonus` 行 kind 渲染 / `OnKillResourceRestore` filter 语义 / rank 0 next preview from-to 这类细节模糊也需要在 freeze 前一次性 closed。完成这九条 must-fix 之后,PR04-01 可一次性走完 `04-01a → 04-01b → 04-01c`,玩家不会再撞到"测试全绿但 hpRegen 显示值与实际治疗对不上 / deathblow 在 Rogue 自体不触发 / preview deltas 与首学收益不符" 这类 game design 失败模式。
