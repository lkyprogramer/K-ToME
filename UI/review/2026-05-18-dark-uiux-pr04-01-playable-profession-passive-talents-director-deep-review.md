# PR04-01 可玩职业被动天赋 — 设计总监 / 系统策划深度复审

**Review target**: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`
**Review date**: 2026-05-18
**Review stance**: 资深 Roguelike / 类 ToME 玩法设计总监 + 系统策划总监 + 玩法体验审查负责人;同时覆盖工程合同、白盒证据、内容包边界与跨 PR 协同。
**Baseline**: Round 5 (`2026-05-18-...-rereview-round5.md`) 已关闭。

---

## Summary

- **改了什么**: PR04-01 把 6 个可玩职业各 2 个 active 天赋转成 PASSIVE,引入 source-agnostic `PassiveEffect` 模型、`PassiveSource` / `PassiveSourceKind` 双源识别、`TalentLevelEffect.passiveEffects`、typed `TalentPassiveDetailSnapshot` 与 delta preview、PASSIVE action 抑制矩阵、coverage lint(`releaseBlocking` / `pr04_01BlockingDevPlayable` / `excludedFrozen`)、3 个白盒 scenario 与 `ValidationScenarioTalentSetupSpec` 数据 owner、Task A 静态 + Task B 触发/条件两批数值。
- **Top risks**:
  1. **跨职业被动 EV (期望价值)预算未横向校准**:`arcane_overload` 与 `devotion` / `flux_anchor` 等价值落差可见,§3.0 吸引力地板只有竖向 rank 1/3/5 检查,没有横向"职业被动平均 EV 锚点",存在 caster bias 与 niche 元素 res 浪费。
  2. **`StatModifier.hpRegen` 与 `HpRegenPerTurn` 是 PR04-01 自己埋下的第二条 hpRegen 真源**:§2.1 把两者并列、§3.1 强制 `pain_fuel` 使用前者、§2.1 仍把后者保留作 equipment parity,但没要求二者最终聚合到同一 `derivedStats.hpRegenPerTurn`;装备 + 天赋 hpRegen 求和、tick 顺序、heal-disabled 状态影响、UI 行融合都缺合同。
  3. **条件被动的实战 uptime 没有 acceptance 锚点**:`bulwark_march` / `last_stand` / `deathblow` / `beacon_of_zeal` 都依赖 GUARD / HP<30% / MARKED / HOLY-tagged 敌人的环境出现频次。§3.0 "rank 1 必须可感知" 是主观判断,没有定量目标(如"目标 30–50% 战斗回合内可触发"),实现层无法验收,且可能在前两层楼完全感知不到。
- **Approval**: `comment` —— 文档已经接近可实现,但游戏系统设计层面还有 3 条 HIGH 与若干 MEDIUM 风险必须在编码前补合同,否则会出现"合同全绿但实际玩起来失衡 / 数值错位 / 部分被动等于摆设"的失败模式。

---

## Affected files (本次审查重点)

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md` — 本 PR 合同主体 (modified)
- 实现层将影响:
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt` — `EquipmentPassive` → `PassiveEffect` 迁移面 (modified)
  - `core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt` — `EquippedPassiveSource` → `PassiveSource` (modified)
  - `core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt:189` — `TalentLevelEffect.passiveEffects` (modified)
  - `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt` — 新增 `TalentPassiveDetailSnapshot` 系列 (modified)
  - `core/src/main/kotlin/com/ktome/core/combat/DamageType.kt` — DamageType enum 顺序合同被 `mana_surge` 显示锁定 (依赖项)
  - `core/src/main/kotlin/com/ktome/core/resource/ResourceModels.kt` — `MANA / ENERGY / POSITIVE_ENERGY` 被本 PR 引用 (依赖项)
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` — passive source 收集、setup hook (modified)
  - `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt` — projection (modified)
  - `game/src/main/kotlin/com/ktome/game/validation/{ValidationScenario.kt, ValidationAction.kt, ValidationSessionOptions.kt, ValidationScenarioRegistry.kt}` — setup/focus owner (modified)
  - `game/src/main/resources/data/talents/index.yaml` — 12 个 talent 重写 (modified)
  - `game/src/main/resources/i18n/{en-US,zh-CN}.json` — passive label/desc (modified)
  - `client/src/main/kotlin/com/ktome/client/ui/talent/{TalentSidebarPresenter, DescriptionPresenter, TalentAssignPanelModel}.kt` (modified)
  - `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` — PASSIVE 行 `R` 抑制 (modified)
  - `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt`, `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml` (modified)
  - `UI/pr/screen-coverage-matrix.md`, `UI/pr/README.md` — 可发现性 (modified)

---

## Root cause & assumptions

PR04-01 是 "PR04 Talent Assign 暴露的 gameplay/content 缺口" 的补丁:UI 已经支持 ACTIVE / PASSIVE / SUSTAINED 三态,但所有可玩职业 talent 实际全是 active,导致玩家把 talent 等同主动按钮,active slot 模态、PR04 detail 三块布局、PR06 icon 设计的玩家心智都失真。

本 PR 选择 "不新增节点,只重写 12 个既有节点为 PASSIVE",并采用类 ToME 的 "static identity + conditional/trigger payoff" 双被动模式。设计方向健康,Round 5 也确认了"职业身份感已足以支撑类 ToME 被动 pass"。

**核心假设(本 review 接受)**:
- 12 个被选转换目标的活动语义 (`buff_self`, `restore_resource`, `panic`) 都已被同树其他主动技能覆盖,转换后 active rotation 不残缺。文档 §3.3 已给出 "tactical verb retained by" 列。
- `core` / `game` / `client` 层级合同(typed snapshot, projection at game boundary, client 不计算 gameplay) 与 PR04 一致。
- `EquipmentPassive` → `PassiveEffect` 是 source-agnostic 迁移,physical package 暂留 `core.item` 以避免一次性大改。
- `ValidationScenarioTalentSetupSpec` 已被声明为白盒 setup 唯一 owner。

**未声明的假设(本 PR 隐含但应显式)**:
- 装备 hpRegen 与天赋 hpRegen 在 derived `hpRegenPerTurn` 上必须代数求和,而不是分两条独立 tick。
- 主动槽位数(`activeSlotCount`)在转换后仍大于 starter loop 所需的 active 数;否则 PR04 active slot modal 价值掉到 0。
- `ResistanceBonus HOLY` 等元素抗性在生成层有足够元素敌人覆盖率,Templar 玩家在前几层就能感知。
- `StatModifier.castSpeedRating` 与 `talentPower` 不会撞 cap;否则 `arcane_overload` rank 5 的"+0.20 + +5" 在高装等下边际收益为零。

---

## Findings

### [HIGH] [Game Balance] 跨职业被动 EV 预算未横向校准 (caster bias + niche 元素 res 浪费)

- **Where**: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:543-573` (§3.0 attractiveness floor 与 value audit), `:579-599` (§3.1/§3.2 数值表)
- **Evidence**:
  - `arcane_overload` rank 5 = `talentPower +0.20`, `castSpeedRating +5` — 法系全局乘区 + 全局节奏乘区,几乎对每个 spell 生效。
  - `devotion` rank 5 = `maxHp +34`, `talentPower +0.12`, `ResistanceBonus HOLY +8` — HOLY res 在多数前期生物上等于零收益,有效 EV 实际接近 maxHp + talentPower 两项。
  - `flux_anchor` rank 5 = LIGHTNING dmg +15% + LIGHTNING res +7 + on-kill MANA +7;只在以 LIGHTNING 为主输出且能稳定击杀时全部生效。
  - `last_stand` rank 5 = attack +30% + defense +8,**条件 HP<30%**;在常规推进下平均触发回合 < 10%。
- **Impact**:
  - **横向**:Arcanist 选 `arcane_overload` 等于"始终在线的成型词条",Templar 选 `devotion` 等于"前 3 层 1/3 数值是装饰";会形成"职业被动一选优,一选弃"的隐性弱后期。
  - **纵向**:§3.0 第 4 条要求 "rank 5 接近一个中等装备词条组合或一个主动 buff 的长期平均价值",但既无 ground truth 也无 cross-passive 比较;实现/数值平衡迭代缺基准。
- **Standards**: 类 ToME 设计经验 — 同 tier 被动 EV 误差应控制在 ±20%;条件被动期望值 = 数值 × uptime,而不是 paper 峰值。
- **Repro**: 取 4 个 release-blocking 职业 rank 5 被动,用统一 EV 公式 `EV = static_value + conditional_value × estimated_uptime` 折算,差距应 ≤20%;按当前表,Arcanist EV ≈ 1.4–1.8× Templar EV(取决于 HOLY 敌人占比假设)。
- **Recommendation**:
  - 在 §3.0 加 **cross-passive EV 锚点表**:为每个 rank 5 被动给出 "估算 EV (DPS-equivalent or HP-equivalent)" 一栏,并要求实现期数值平衡迭代必须在 ±20% 锚点内。
  - 给条件被动定义 **目标 uptime 假设**:`bulwark_march` ≈ 50%(guard 频繁主动用),`last_stand` ≈ 10–15%,`deathblow` ≈ 30–40%(marked 由 rogue 自己施加),`beacon_of_zeal` ≈ 25%(holy 敌人占比);把这三项作为 §3.0 验收数据,而非主观文案。
  - `devotion` 与 `beacon_of_zeal` 的 HOLY 抗性叠到 rank5 共 +18;建议把 `devotion` HOLY res 改成 `con +N` 或 `wil +N`,把"holy 抗性身份"集中到 `beacon_of_zeal` 以避免重复且失效。
- **Tests / Acceptance**:
  - `NEW: TalentSchemaTest.passiveRank5EvAnchorWithinTwentyPercent` — 用 §3.0 锚点表计算 EV,断言所有 release-blocking 职业 rank 5 EV 在 ±20% 区间内。
  - `NEW: TalentSchemaTest.conditionalPassiveUptimeIsDeclaredInDoc` — lint:每个 ConditionalStatBonus / DamageVsStatus / OnKillResourceRestore talent 必须在 §3.0 表里声明 estimatedUptime。

---

### [HIGH] [Correctness / Integration] `StatModifier.hpRegen` 与 `HpRegenPerTurn` 双轨 hpRegen 真源未合一

- **Where**:
  - `UI/pr/...passive-talents.md:225-227` (PassiveEffect 同时有 `HpRegenPerTurn`)
  - `UI/pr/...passive-talents.md:266` (kind coverage 表声明 `pain_fuel.hpRegen` 用 StatModifier.hpRegen,不是 HpRegenPerTurn)
  - `UI/pr/...passive-talents.md:336` (stable-key 表把 `equipment HpRegenPerTurn` 当成独立 cue key 类别)
  - 现有代码:`core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:93-95` (`HpRegenPerTurn`) 与 `:153` (`StatModifier.hpRegen: Double`) 在生产代码里已经是两条独立路径
- **Evidence**: §2.1 把两者并列保留作 equipment parity,§3.1 又强制天赋 hpRegen 用 `StatModifier.hpRegen`;`StatsCalculator` 必须把两条最终聚合到 player.`derivedStats.hpRegenPerTurn`,但文档没明写合一规则。
- **Impact**:
  - 玩家在 inspect 面板看到的 "回血/回合" 数字可能 = sum(`StatModifier.hpRegen`) 或 = sum(`HpRegenPerTurn`) 或 = 二者代数和,实现可能任选其一并各自通过 focused test。
  - 装备给 `HpRegenPerTurn +1`,`pain_fuel` 给 `StatModifier.hpRegen +0.4`,如果 UI 行渲染分两行 ("装备回血 +1" / "天赋回血 +0.4"),玩家无法理解为何同一概念两个数。
  - tick 顺序可能不同:`HpRegenPerTurn` 来自装备聚合(整数,turn start),`StatModifier.hpRegen` 走 StatsCalculator(double,floor/ceil 行为不明)。负数 hpRegen(中毒等)若只衰减一条管道,会出现"被毒了但被动血还在加"的玩家可见 bug。
- **Standards**: 单真源原则;同一玩家可见 derived stat(回血/回合)必须在 resolver 层有唯一聚合点。
- **Recommendation**:
  - §2.1 / §2.3 增加 **hpRegen 合一规则**:`StatsCalculator.hpRegenPerTurn = floor(sum(StatModifier.hpRegen) + sum(HpRegenPerTurn.amount))`,且只产生一行 PASSIVE detail "回血/回合 +N"。
  - 在 detail projection (§2.5 line template 表) 把 `HpRegenPerTurn` 与 `StatModifier.hpRegen` **共用同一个 lineKind**(例如 `HP_REGEN_LINE`),典型 sortKey 一致,避免 UI 双行。
  - §10 self-audit 增加一行:"hpRegen single derived path: confirmed that StatModifier.hpRegen and HpRegenPerTurn collapse into one derivedStats.hpRegenPerTurn before display".
- **Tests / Acceptance**:
  - `NEW: PassiveEffectResolverTest.hpRegenStatModifierAndHpRegenPerTurnCollapseIntoSingleDerivedStat`
  - `NEW: SessionSnapshotMapperTest.painFuelPassiveAndEquipmentHpRegenRenderSingleDetailLine`
  - `NEW: FoundationGameSessionTest.hpRegenTickAppliesSumOnceAndRespectsHealDisabledStatus`

---

### [HIGH] [Game Design] 条件 / 触发型被动缺 uptime 与触发可用性验收

- **Where**: `UI/pr/...passive-talents.md:543-557` (§3.0 attractiveness floor),`:594-599` (§3.2 Task B)
- **Evidence**:
  - `bulwark_march`: ConditionalStatBonus SELF_HAS_STATUS=GUARD;Vanguard 学习 `bulwark_march` 时未必学过 `guard_stance`(只在 prereq 表 = 3 时强制),长游玩中 guard uptime 取决于 stamina 与 cooldown。
  - `last_stand`: HP_BELOW_30 — 健康玩家 ~5% 触发,极困难时段才能触发,Rank 5 +30% attack 实际平均 ≈ +1.5%。
  - `deathblow`: DamageVsStatus MARKED — Rogue 唯一稳定 marker 是哪个主动?文档没指出"marked 由 X 主动技能稳定施加"。
  - `beacon_of_zeal`: ResistanceBonus HOLY 与 DamageTypeBonus HOLY 都耦合于"敌人是 HOLY 元素 / 玩家用 HOLY 输出";前 2 层楼几乎无 HOLY-tagged 敌人。
- **Impact**: 玩家会发现:rank 1 完全感知不到,rank 3 才能在某些 boss 战感受到,rank 5 才"勉强值";违反 §3.0 第 2 条 "Rank 1 必须可感知"。这会导致 PR04-01 通过所有合同 / 测试,但玩家点完被动后 5 分钟内觉得"白点了"。
- **Recommendation**:
  - 在 §3.2 每个条件/触发被动加 **触发器可用性来源**(由哪些上游主动技能保证):
    - `bulwark_march` → `guard_stance` 是 Vanguard starter active 且 prereq 已 ≥3 → 文档应显式声明 "guarded 状态由玩家可在第 1 回合主动施加"。
    - `deathblow` → 必须列出 marked 的来源:同树 `poison_blade` 是否赋 MARKED?如果不是,deathblow 在 Rogue 自体不可触发,变成等装备 affix 产 MARKED。
    - `beacon_of_zeal` → 至少 Templar starter 中 `holy_light` / `holy_aura` 应已经产生 HOLY 伤害事件触发 self DamageTypeBonus → 这部分应在文档显式连接。
  - §3.0 Value audit 加一列 `triggerOwner / triggerUptimeEstimate`。
  - 把 §3.0 "rank 1 必须可感知" 改成可验收的:"rank 1 的有效 EV(数值×uptime) ≥ 同职业另一被动 rank 1 的 50%".
- **Tests / Acceptance**:
  - `NEW: TalentSchemaTest.conditionalPassivesDeclareTriggerOwnerInDoc` (acceptanceContractLint 子规则,parse 文档表 §3.0 / §3.2)
  - `NEW: FoundationGameSessionTest.deathblowMarkedSourceIsReachableFromStarterRogueLoadout` — 用 starter 配置模拟一次连击,断言 MARKED 状态可触发 deathblow 的 DamageVsStatus bonus 至少一次。

---

### [MEDIUM] [UI / Performance] 多效果 PASSIVE 行密度可能溢出 Talent Assign 右栏

- **Where**: `UI/pr/...passive-talents.md:459-471` (§2.5 line template 表), `:594-599` (§3.2 mana_surge / balance_point / flux_anchor 多效果配置), `:931-935` (§7 white-box 窗口 1280x840)
- **Evidence**:
  - `mana_surge`: 1 OnKill + 3 DamageTypeBonus = 4 current lines + 4 delta lines = **8 行**(rank 0 时 current 退化为 rank 1 preview,仍 4 + 4)。
  - `balance_point`: 4 stat (talentPower / attackMultiplierBonus / defense / castSpeedRating) = 4 current + 4 delta = 8 行。
  - `flux_anchor`: 3 effect + delta = 6 行。
  - `unyielding`: 3 effect + delta = 6 行。
  - PR04 right detail 已有 hero header / rank / cost / prereq / actions,加上 type line "被动" 与 next-preview 标题,共 ≥6 个固定行 + 8 个 PASSIVE 行 = 14+ 行。
- **Impact**:
  - 1280x840(白盒窗口)在当前 spacing 下可能需要滚动;Round 4/5 已经把 preview 改成 `previewExpanded=false` 减压,但本 PR 三个 scenario 都包含 "按 P 展开" 步骤,展开后必触发滚动或裁剪。
  - 类 ToME 玩家心智里 "preview" 应一屏可见;滚动 preview 等于 UX 失败。
- **Recommendation**:
  - 在 §2.5 加 **行密度上限**:single passive detail current + delta 总行数 ≤ 10;若超,implementation 必须把同维度合并成一行(如 `mana_surge` 的 3 个 elemental damage 用一行 "三系元素伤害 +X%"),但合并需 typed args 支持。
  - 或:在 PR04-01 文档显式声明 "preview 展开后允许垂直滚动",并在白盒 step 增加滚动断言;但更倾向行数上限。
  - 增加 client golden test 锚点窗口尺寸断言:`TalentSidebarPresenterTest.passiveDetailFitsViewportAtMinimumWindow` (1280x720)。
- **Tests / Acceptance**:
  - `NEW: TalentSidebarPresenterTest.passiveDetailLineCountPerTalentDoesNotExceedTen`
  - `NEW: contractLint.passiveLineCountBudget` — 解析 §3 数值表,聚合每个 talent 的 line 数,在 lint 中卡 10 行上限。

---

### [MEDIUM] [Correctness] PASSIVE rank-replace 与 equipment additive 的聚合语义未明写

- **Where**: `UI/pr/...passive-talents.md:323-355` (§2.3 resolver contract 与 stable key 表), `:359-376` (§2.4 runtime rule 2)
- **Evidence**:
  - §2.4 rule 2: "Rank N uses exactly levelEffects[N].passiveEffects; rank 1..N-1 are not accumulated".
  - §2.3 rule 3: "Stat adjustment combines equipment and talent passive modifiers deterministically".
  - 但是 §2.3 没说装备每件都加、天赋同 talent 不同 rank 取最高 rank 替换;一个共享 resolver 需要 source-aware 聚合策略,文档把这个核心规则塞进了一句话。
- **Impact**:
  - 实现可能写成 "所有 PassiveSource 全相加" → 玩家若曾点 rank 3 然后 respec 到 rank 1,内存里 cache 的 rank 3 PassiveSource 不被丢弃则会双倍 → §3.0 的 `respecRemovesTalentPassiveDerivedStats` 表面通过(只测 0 → 1),但 5 → 1 的过渡不覆盖。
  - resolver trace 会同时出现 `passive:talent:unyielding:StatModifier:rank3` 和 `passive:talent:unyielding:StatModifier:rank5`,违反 stable-key cleanup 直觉。
- **Recommendation**:
  - §2.3 / §2.4 加一节 **Aggregation policy**:
    - "Equipment passive sources: 所有装备并存,各自作为独立 PassiveSource 进 resolver,sum/合并由 effect kind 决定。"
    - "Talent passive sources: 每个 talentId 在任意时刻只有 0 或 1 个 PassiveSource,sourceTemplateId=talentId,talentRank=currentLearnedRank;rank-up / respec / save-load 时旧 talentRank 的 PassiveSource 必须从 collection 中**移除**而非追加。"
  - 把 §2.4 refresh test list 加一条:`PassiveEffectResolverTest.talentPassiveCollectionContainsExactlyOneEntryPerTalentIdAfterRankUp`.
  - stable-key §2.3 增加规则:"trace `passive:talent:<id>:<kind>:rank<rank>` 在 rank 升级时新 key 替换旧 key,旧 key 不出现在同一帧 frontstage trace。"
- **Tests / Acceptance**:
  - `NEW: PassiveEffectResolverTest.talentPassiveCollectionContainsExactlyOneEntryPerTalentIdAfterRankUp`
  - `NEW: PassiveEffectResolverTest.respecFromRank5ToRank1KeepsOnlyRank1Effects`
  - `NEW: FoundationGameSessionTest.passiveTraceStableKeyReplacesPreviousRankOnRankUp`

---

### [MEDIUM] [Game Balance] 同一职业内 resistance / attackMultiplierBonus 重复堆叠未审计

- **Where**: `UI/pr/...passive-talents.md:579-599` (§3.1 + §3.2 数值)
- **Evidence**:
  - Templar 满点:`devotion` (HOLY res +8) + `beacon_of_zeal` (HOLY res +10) = HOLY res **+18**。
  - Berserker 满点条件触发:`pain_fuel` (attackMultiplierBonus +0.18) + `last_stand` (attackMultiplierBonus +0.30) = 在 HP<30% 时 **+48%** 攻击,叠加装备/buff 后可能 >+100%。
  - Arcanist 满点:`arcane_overload` (talentPower +0.20) + 装备 / scroll 后 talentPower 进入边际收益区。
- **Impact**:
  - resistance cap 若存在(如 75)被叠满后多投入零边际;玩家不知情会浪费 2 个 talent point。
  - `attackMultiplierBonus` 进入伤害公式时若与 critMultiplier / damageType 乘区相乘,+48% 在 last_stand 状态可能产生设计未预期的"逆转一击"。
  - Templar 和 Berserker 的"双被动 stacking" 比 Vanguard (maxHp/defense 与 conditional defense 不同维度) 与 Rogue (accuracy/crit 与 marked dmg 不同维度) 健康度差。
- **Recommendation**:
  - 在 §3.0 加 **profession-level passive stacking audit** 一张表,列出"满点(rank5+rank5) 时同维度叠加值",并比对 cap / soft-cap / 边际收益拐点。
  - 若 cap 存在,优先调整:`devotion` HOLY res +8 → `con +5` 或 `wil +5`,把"holy resistance 身份"留给 `beacon_of_zeal` 独占。
  - `attackMultiplierBonus` 同理:`pain_fuel` rank 5 +18%,可考虑改为 `attack +6` (flat) + hpRegen 强化,避免与 `last_stand` 乘区双重峰值。
- **Tests / Acceptance**:
  - `NEW: TalentSchemaTest.sameDimensionPassiveStackPerProfessionDoesNotExceedSoftCap` — lint:聚合每个职业两个被动满点同维度数值,断言 ≤ 软上限(由 §3.0 提供)。

---

### [MEDIUM] [Integration] `DamageType` enum 顺序被 `mana_surge` UI 隐式锁定,但无 pin 合同

- **Where**: `UI/pr/...passive-talents.md:467` (DamageTypeBonus 排序 "damage type enum order"), `:905-909` (§6 display rules), `core/src/main/kotlin/com/ktome/core/combat/DamageType.kt`
- **Evidence**: §6 rule 5 明说 "mana_surge renders separate FIRE / COLD / LIGHTNING lines in `DamageType` enum order, not YAML authoring order"。但 `DamageType.kt` 内的 enum constant 顺序若被未来重排(例如按 reading-order 重排),所有 PASSIVE detail screenshot 静默变化。
- **Impact**: golden 比对失败、玩家阅读顺序变化、本 PR 白盒截图过期、§6 rule 5 与代码事实漂移。
- **Recommendation**:
  - 在 `DamageType.kt` 文件首注释加一行:`// Ordering pinned by PR04-01 passive detail rendering. See UI/pr/dark-uiux-pr04-01-...md §6.`
  - 增加 contractLint:`damageTypeEnumOrderPinned` — 解析 `DamageType.kt` enum 顺序,断言以已知 fingerprint 起头(`PHYSICAL, HOLY, FIRE, COLD, LIGHTNING, ...`)。
  - 文档 §6 rule 5 后补一句:"Reordering `DamageType` enum requires updating PR04-01 white-box golden and §3.2 mana_surge / flux_anchor expected ordering in the same change."
- **Tests / Acceptance**:
  - `NEW: contractLint.damageTypeEnumOrderPinned`

---

### [MEDIUM] [Content / Save] 内容包升级会静默改变玩家已学被动数值,无 changelog 合同

- **Where**: `UI/pr/...passive-talents.md:363-370` (§2.4 rule 7), `:861-871` (§5 contract change 表)
- **Evidence**: §2.4 rule 7 "Save/replay persists talent id/rank, not resolved passive values";§5 "No save schema migration is allowed"。
- **Impact**:
  - 发布后任何对 §3.1 / §3.2 数值的微调都会**追溯性**应用到老存档玩家。
  - 玩家可能截图 rank 5 `last_stand +0.30 attack`,下次启动看到 `+0.25`,无任何 in-game 通知。
  - 这对 release-blocking 4 个职业是真实用户体验问题。
- **Recommendation**:
  - 增加 §5.x **"Talent passive data versioning"** 一节:
    - 内容包 / 官方数据每次修改 §3.1 / §3.2 的有效数值,必须 bump `talentDataRevision`(已存在或新增字段)。
    - load 时若 saved `talentDataRevision` < current,UI 弹出一次性 toast "天赋被动数值已更新,请重新查看".
  - 这条不一定要 PR04-01 完成,但 §10 self-audit 必须显式声明 "future content delta will silently rebalance live characters" 作为 known limitation。
- **Tests / Acceptance**:
  - 若同步实现:`NEW: FoundationGameSessionTest.saveLoadWithChangedPassiveDataEmitsRebalanceNotification`
  - 否则在 §11 Open Risks 增加 1 条 "live rebalance silent" 显式认领。

---

### [MEDIUM] [Game Design] starter active count 与 active slot 总数关系未量化,可能让 PR04 modal 价值掉到 0

- **Where**: `UI/pr/...passive-talents.md:605-612` (§3.3 portfolio audit 表 "starter active count after conversion: 3")
- **Evidence**: 表显示 6 个职业转换后 starter active count = 3。但 active slot 总数(玩家可同时绑定多少 active 到 hotbar)文档未引用。
- **Impact**:
  - 若 active slot 总数 ≥ 3 且 starter loop 只用这 3 个,PR04 的 active slot replacement modal 在前 N 小时不会触发,PR04 的 acceptance contract(LearnActive 触发 modal)在白盒主路径上变成"理论存在但难复现"。
  - 反之若 active slot 总数 < starter 3,新玩家在第一个 active talent 就被迫 replace,体验割裂。
- **Recommendation**:
  - §3.3 补一列:`activeSlotCount(profession starter)`,并显式声明 "starter loop fits in starter active slot count;learning a 4th active 触发 PR04 active slot modal,被 PR04-01 white-box 覆盖"(如果 PR04-01 scenario 已经走到 4th active learn 才出 modal,白盒不会触发,这点应该承认)。
  - 如果发现 starter active count 已经满足 starter slot,在 PR04-01 §11 Open Risks 显式列 "active slot modal coverage 移至 PR-04-02 / PR-05 dev playable run-through"。
- **Tests / Acceptance**:
  - `NEW: FoundationGameSessionTest.starterLoadoutFitsInActiveSlotCountAfterPr04_01Conversion` — 防止本 PR 数据改动让 starter 强制触发 active slot modal。

---

### [MEDIUM] [White-box] 三个 scenario 都用 `greenwood_fringe / floor=2`,trigger passive 难以验证不可触发面

- **Where**: `UI/pr/...passive-talents.md:931-935` (§7 runtime spec 表)
- **Evidence**: 所有 scenario `zone=greenwood_fringe, floor=2, routeIndex=-1`,意味着 evidence 截图都在最早期生物群。
- **Impact**:
  - `mana_surge` 的 trigger passive 截图只覆盖 "学习后 panel 显示数值",**没有 in-game OnKillResourceRestore log 触发**;若 resolver 与 trigger wiring 误把 talent kind 路由到 equipment branch,白盒不会发现。
  - `bulwark_march` 的 DamageVsStatus TAUNT bonus 触发,需要至少一次 TAUNT 被施加且玩家攻击 TAUNT-affected 敌人;greenwood_fringe floor 2 是否有触发面无声明。
- **Recommendation**:
  - 至少给 `trigger-passive-detail` scenario 加一条 in-game evidence step:学习后接近一只低血敌人,击杀,log 中出现 `passive:talent:mana_surge:OnKillResourceRestore:rank1` 触发记录与 MANA 实际恢复行。
  - `passive-action-suppression` 不必加 in-game trigger;`static-passive-detail` 同样不必。
  - 或者在 §11 Open Risks 显式承认 "白盒不验证 talent passive trigger 真实在战斗中执行,该断言由 `FoundationGameSessionTest.talentPassiveOnKillRestoreUsesTalentSource` 单独覆盖"。
- **Tests / Acceptance**:
  - 增补白盒 scenario `dark-uiux-pr04-01-trigger-passive-detail` 的 cua step:`Walk down (1 tile)` / `Attack (Space)` × N → 击杀 → 验 log 出现 `log.passive.on_kill_resource_restore` 且 `sourceKind=TALENT`、`sourceTemplateId=mana_surge`。

---

### [LOW] [Doc Clarity] `active verb budget` 在 §10 self-audit 出现但未定义

- **Where**: `UI/pr/...passive-talents.md:1122`
- **Evidence**: `| active verb budget | conversion audit remains true after implementation |` —— 仅以"保持 §3.3 conversion audit 表一致"为内容,但术语 "active verb budget" 未在文档其它处定义。
- **Impact**: acceptanceContractLint 无法消费,reviewer 不清楚是否要重新核对所有 12 talent。
- **Recommendation**:
  - 删除该行,或重命名为 "conversion audit parity";如果保留 "active verb budget" 概念,在 §3.3 之前补 1 段定义:"each profession's active rotation must retain the verbs listed in §3.3 conversion audit `tactical verb retained by` column"。
- **Tests / Acceptance**:
  - 如果保留概念:`NEW: acceptanceContractLint.activeVerbBudgetMatchesConversionAudit`

---

### [LOW] [Doc Clarity] §2.5 "fixed stat order from the stat display coverage table" 未明确序号

- **Where**: `UI/pr/...passive-talents.md:462`, `:482-502` (stat display coverage table)
- **Evidence**: §2.5 line template 表 "ordering rule: fixed stat order from the stat display coverage table",但 §2.5 表(line 482 起)只是按 row 列出 statId,没显式声明 row 顺序 == 渲染顺序。
- **Impact**: 文档 reviewer 如果按字母重排表行,实现侧 stat 顺序静默改变;golden 截图过期。
- **Recommendation**:
  - 在 §2.5 stat display coverage table 标题下加一句:**"行序即 PASSIVE detail 中 stat 行的最终排序契约。重排此表必须同步重置 PR04-01 golden / white-box 与 SessionSnapshotMapper test ordering 期望。"**
  - 或为每行加一个显式 `sortOrder` 数字列。
- **Tests / Acceptance**:
  - `NEW: SessionSnapshotMapperTest.passiveDetailStatOrderMatchesDocumentedTable` —— 直接断言 `unyielding` rank 1 三行顺序为 `maxHp, defense, PHYSICAL resistance`,`balance_point` rank 1 四行为 `defense, castSpeedRating, talentPower, attackMultiplierBonus`(或文档明确顺序)。

---

### [LOW] [Doc / i18n] `LocaleLintTest` 只覆盖 stat modifier 标签

- **Where**: `UI/pr/...passive-talents.md:80` (M09 fastCheck 列 `LocaleLintTest.passiveDetailStatModifierLabelsExist`)
- **Evidence**: 文档只声明 stat-modifier label lint,但 `OnKillResourceRestore` / `DamageVsStatus` / `ConditionalStatBonus` / `TerrainAffinityBonus` 的中英文短语未列入。
- **Impact**: PR04-01 落地后 `pain_fuel` / `bulwark_march` 数值正确但短语缺失或带占位符 `{statusId}` 不会被 lint 抓住,只能靠白盒人眼判断。
- **Recommendation**:
  - 增加 `LocaleLintTest.passiveDetailNonStatLabelsExist` 覆盖 ResourceType / DamageType / status / PassiveCondition 的本地化模板。
- **Tests / Acceptance**:
  - `NEW: LocaleLintTest.passiveDetailNonStatLabelsExist`

---

### [LOW] [Game Design] `beacon_of_zeal` rank 1 `POSITIVE_ENERGY +1` 的设计可读性低于 baseline

- **Where**: `UI/pr/...passive-talents.md:597`
- **Evidence**: rank 1 给 1 点 POSITIVE_ENERGY / 击杀,玩家阅读时容易把"1"等同"无";`mana_surge` rank 1 MANA +3 与 `deathblow` rank 1 ENERGY +4 都从 ≥3 起步。
- **Impact**: 玩家在 rank 1 选择面前判断"它好像不值",更难命中 §3.0 "rank 1 必须可感知"。
- **Recommendation**:
  - 将 `beacon_of_zeal` rank 1 POSITIVE_ENERGY +1 → +2;rank 2 → +3;后续按曲线推。
  - 若 POSITIVE_ENERGY 资源本身基线小(总池 < 10),则在 §3.0 给出资源池规模并解释"1 单位等于 X% 主动技能消耗"。
- **Tests / Acceptance**: 数值调整 + `TalentSchemaTest.passiveRank1IsPerceivable` (用 §3.0 定义的 perceivable 阈值,例如 ≥10% 资源池或 ≥5% damage swing)。

---

### [LOW] [Doc / Engineering] 旧 rank cue key cleanup 未写进 stable-key 表

- **Where**: `UI/pr/...passive-talents.md:332-348` (§2.3 stable-key 表 + Trace contract)
- **Evidence**: talent cue key 含 `rank<rank>`,但 rank-up 后旧 key 是否从 trace / log buffer 移除未声明。
- **Impact**: log 时间窗口内可能同时出现 `passive:talent:unyielding:StatModifier:rank2` 与 `:rank3`,误导 debug。
- **Recommendation**:
  - 在 §2.3 Trace contract 加 rule 4:"Talent passive cue key 在 rank 变化(learn / rank-up / respec)时,旧 rank 的 key 必须在当帧从 frontstage trace 中清除,不与新 rank 的 key 共存"。
- **Tests / Acceptance**:
  - `NEW: FoundationGameSessionTest.talentPassiveCueKeyDoesNotPersistAfterRankUp`

---

### [LOW] [Doc Hygiene] SUSTAINED 边界未由 lint 守住

- **Where**: `UI/pr/...passive-talents.md:23-30` (§0.0 declared SUSTAINED 不新增)
- **Evidence**: §0.0 声明本 PR 不新增 SUSTAINED,但 acceptance matrix 与 contractLint 没有 SUSTAINED 计数守护。
- **Impact**: 实现可能顺手把某个 PASSIVE 写成 SUSTAINED("开关型 self buff" 直觉),lint 不抓。
- **Recommendation**:
  - 增加 contractLint:`pr04_01ConvertedTalentsAreExactlyPassive` — 12 个 talentId 必须 `category == PASSIVE && kind == PASSIVE`;不得为 SUSTAINED。
- **Tests / Acceptance**:
  - `NEW: contractLint.pr04_01ConvertedTalentsAreExactlyPassive`

---

### [NIT] 文档冗余

- §11 Open Risks 第 3 条 ("Static passive flattening is controlled by the mandatory Task B portfolio") 与 §3.0 第 1 条 ("每个 playable 职业的 2 个被动必须一静一动") 表述重复,可合并。
- §0.0 与 §0 间标题层级跳跃("0.0 Design Direction" 在 "0. 开发治理与验收矩阵" 前),阅读时容易误以为 0.0 是 0 的子节,建议改名为 "Design Direction" 或挪到 §1 之前。

---

## Performance / Operations

- **Hotspots**: PassiveEffectResolver 每帧重新计算 derivedStats 的成本随 (装备件数 × 装备 passive 数 + 已学 talent 数) 线性增长;PR04-01 引入 ≤2 个 PASSIVE 每职业,常态 ≤ 12 PassiveSource,总规模可忽略。
- **Complexity notes**: projection (`SessionSnapshotMapper`) 对每个被高亮 talent 生成 current + next lines;复杂度 O(rank effect count) 每次 right-pane 重绘。不是性能瓶颈,但需确保 projection 不在 input loop 每帧执行(应该有 dirty flag)。
- **Bench / Monitoring plan**:
  - `:client:clientSmoke` runtime 期望 ≤ 当前 baseline + 100ms;新增 8 行 passive detail 不应明显增加。
  - 关注 `goldenScreenshot` 任务时间(PR04-01 三个 scenario × 5 evidence ≈ 15 screenshot)。

---

## Integration

- **API / contracts**:
  - `TalentLevelEffect.passiveEffects` 是 additive 字段,默认 emptyList,兼容旧 talent;但本 PR 的 12 个 talent 强制声明,需保证 schema loader 对其它 talent 不要求该字段非空。
  - `PassiveSource` 引入是 source-agnostic,**会 break 既有 `EquippedPassiveSource` callers**(`PassiveEffectResolver.kt:10`);Task 1 必须列出所有 caller 的迁移点,文档当前未枚举(查看 `PassiveEffectResolver` 的导入/调用方:`FoundationGameSession`、`StatsCalculator`、保存层、log emitter)。
- **DB / save migrations**:无 schema migration(§5);但隐含 "live data rebalance silent" 风险已在 [MEDIUM] 一条覆盖。
- **Feature flags / rollout**: 无显式 flag。建议在实现层加 `ktome.passive.unified.enabled` 内部 flag,允许在出 prod 问题时 24h 回退到旧 equipment-only resolver。文档目前完全依赖 commit revert。
- **Resilience**:resolver 改造期间 retry 不适用;equipment passive parity 测试是唯一防线,文档已经声明 `04-01a` blocking gate,合理。
- **Rollback plan**:§9 已分 slice 描述,与 §0 Execution Slices 一致;`04-01b` 失败时 "Task A 数据 + typed passive detail + coverage report-only 必须**一起**回滚或一起保留" — 这条要在 Task 6 完成前就要落实,以免 lint rule 与数据状态分裂。

---

## Testing

- **Coverage**:文档新增 ~30 个 focused test 名,覆盖 schema / resolver / runtime refresh / detail projection / action 抑制 / coverage lint / white-box registry / locale lint。是本 PR 类型见过最完整的合同。
- **Gaps**(由本审查识别):
  1. cross-passive EV 平衡断言(HIGH-1)
  2. hpRegen 单真源 collapse(HIGH-2)
  3. trigger passive 可用性来源(HIGH-3)
  4. talent passive collection 单条目(MEDIUM-4)
  5. 同维度被动 stacking cap(MEDIUM-5)
  6. DamageType enum 顺序 pin(MEDIUM-6)
  7. 行密度上限(MEDIUM-4)
  8. 白盒 in-game trigger 触发(MEDIUM-8)
  9. starter slot fit(MEDIUM-7)
  10. cue key cleanup(LOW)
- **Flakiness risks**:
  - DamageType / stat display 顺序若依赖隐式 enum constant 顺序,golden 易失败 → MEDIUM-6 / LOW (stat order)。
  - 白盒 step 用 `F9, Enter, Esc, T` 进入 Talent Assign,然后假设 setup 已 focus 目标 talent;若 setup 未正确触发 focus,step 1 会捕获错 talent。`expectedFocusedTalentId` 已声明守护,但白盒 step 没有显式 "verify focused talent id" 断言行(只在 typedAssertions 里)。
- **Targeted test plan additions**(Given / When / Then):
  - **Given** Vanguard 起步配置,**When** 学习 `unyielding` 到 rank 5 + `bulwark_march` 到 rank 5 并保持 GUARD 状态,**Then** derivedStats.defense 等于基础 + unyielding +5 + bulwark_march +6,且 trace 只包含两个 PassiveSource,不重复。
  - **Given** Berserker 起步配置 HP=10/100(< 30%),**When** PassiveEffectResolver 解析,**Then** `last_stand` PassiveSource 被激活;HP 治疗到 35% 后,**Then** `last_stand` 不再贡献 attackMultiplierBonus 与 defense。
  - **Given** Templar 装备一件 `HOLY ResistanceBonus +3` 装备,**When** 学习 `devotion` rank 5(+8) 与 `beacon_of_zeal` rank 5(+10),**Then** derivedStats.holyResistance = base + 3 + 8 + 10(或 cap 后值);若有 cap,test 必须断言 cap 行为而不是数值峰值。
  - **Given** 玩家学习 `mana_surge` rank 1,**When** 击杀一只敌人,**Then** log 出现 `sourceKind=TALENT` 的 OnKillResourceRestore,且 MANA 增加 3;若装备另带 OnKillResourceRestore MANA +1,**Then** log 出现两条独立 trigger 而非合并。

---

## Docs & Observability

- **Docs to update / create**:
  - `UI/pr/screen-coverage-matrix.md`、`UI/pr/README.md`(§7 / §10 已声明)
  - `UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md`(§7 已声明)
  - 缺:`docs/balance/passive-attractiveness-floor.md`(本 review 建议产物 — 包含 cross-passive EV 锚点 + 条件被动 uptime 假设);若不单独建文件,§3.0 必须强化到包含这些字段。
- **Logs / Metrics / Traces**:
  - `requiredLogEventKeys` / `forbiddenLogFragments` 已合同化,这是 PR04-01 文档的亮点。
  - 建议补 `log.passive.on_kill_resource_restore` 与 `log.passive.conditional_bonus_activated` / `_deactivated` 两类事件 key 到 §7 `requiredLogEventKeys`,以便 trigger passive scenario 真正捕获触发面。
- **Runbook**:无需更新;rollback 已经 slice-aware。

---

## Open questions

1. `DamageType` enum 实际顺序是 `PHYSICAL, HOLY, FIRE, COLD, LIGHTNING, ...` 还是另一个序?`mana_surge` / `flux_anchor` 的截图期望必须以此为准,文档须给出当前 fingerprint(本 review 未深入 `DamageType.kt` 全部内容,值得在文档 §6 留一处显式记录)。
2. `activeSlotCount` 在 starter 阶段是几?决定 PR04 active-slot modal 是否真在玩家前几小时被触发。文档应引用 PR04 或 PR04-01 给出数字。
3. `MARKED` 状态在 Rogue starter loadout 中由哪一/几个主动技能稳定施加?如果只有 affix 来源,`deathblow` 在 Rogue 自体不可触发是设计问题。
4. 内容包升级时是否允许追溯性 rebalance,还是必须 talentDataRevision bump + in-game 通知?这是 release 政策问题,§5 没明说。
5. `attackMultiplierBonus` 与 `talentPower` 在 damage / spell power 公式中是 additive 还是 multiplicative?这直接决定 `last_stand` + `pain_fuel` 联合峰值与 `arcane_overload` 的 cap 拐点。

---

## Final recommendation

- **Decision**: **comment**(可进入 implementation,但建议在 `04-01a` 启动前先吸收 HIGH-1/2/3 的文档修正)
- **Must-fix before merge**(文档侧,不阻塞 `04-01a` 启动但应在 `04-01b` 数据落地前完成):
  1. [HIGH-2] §2.1 / §2.3 增加 hpRegen 合一规则与 detail 单行渲染合同;新增 collapse test。
  2. [HIGH-1] §3.0 增加 cross-passive EV 锚点表与 conditional uptime 列;数值平衡迭代以此为 ground truth。
  3. [HIGH-3] §3.2 每个条件/触发被动明写 `triggerOwner` 与 estimatedUptime;`beacon_of_zeal` HOLY 抗性的 niche 问题或调整或显式认领。
  4. [MEDIUM] resolver aggregation policy(equipment additive / talent rank-replace 单条目)在 §2.3 / §2.4 显式落字。
  5. [MEDIUM] PASSIVE detail 行密度上限或 viewport-fit 合同(§2.5)。
  6. [MEDIUM] DamageType enum 顺序 pin 合同(§6 + contractLint)。
- **Nice-to-have post-merge / 后续 PR 跟进**:
  - 同维度 stacking cap 表(`devotion` vs `beacon_of_zeal`、`pain_fuel` vs `last_stand`)。
  - 内容包数据 revision 与 in-game notification 机制(可放进 PR04-02 或 PR-08 内容包治理)。
  - 白盒 trigger passive 增加 in-game 击杀 step,验证 OnKillResourceRestore 路径在战斗中真实执行。
  - SUSTAINED 0 计数 lint 守护(防止顺手转 SUSTAINED)。
- **Confidence**: medium-high — 文档已经经过 Round 1–5 收敛,本轮发现的问题主要是 game design 层(EV 平衡 / uptime / 行密度 / stacking)与一处明确的工程双真源(hpRegen),不是结构性失败。Round 5 关闭后的合同骨架可以继续推进 `04-01a` parity 工作,平行修复文档。

---

## Suggested verification (文档修订后)

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

Implementation 期 owner gates(与 Round 5 一致,可补本 review 建议的新测试名):

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

本审查未运行 Gradle;只读了文档、Round 5 报告与若干生产源文件(`ItemModels.kt`、`PassiveEffectResolver.kt`、`TalentModels.kt`、`ValidationScenario.kt`、`ResourceModels.kt`、`StatusRuntime.kt`、`DamageType.kt`)以确认依赖项(ResourceType / StatusEffectType / PassiveCondition / StatModifier 字段)与文档一致。

---

## Closure check vs Round 5

| Round 5 P-level | 本轮状态 |
| --- | --- |
| P1 `PassiveEffect.StatModifier` 命名陷阱 | 已修(`StatModifierEffect`,§2.1) |
| P1 白盒 setup data owner | 已修(`ValidationScenarioTalentSetupSpec`,§7) |
| P2 source-agnostic model physical package | 已修(§2.1 显式 legacy `core.item` placement) |
| P2 Canonical Artifact 完整性 | 已修(§7 evidence 全列入 Canonical Artifact 与 M14) |
| P2 `app.log` evidence | 已修(三个 scenario 均加 `*-app.log`) |
| P3 `callbacks` 空列表/缺省 | 已修(§2.2 rule 5) |
| P3 unknown projection seam | 已修(loader fail-fast + projection exhaustive,§2.5) |
| P3 ZH_CN visible vs typed assertion | 已修(§7 first-detail 拆 typed / visible 两列) |
| P3 no-mutation assertion surface | 已修(§2.6 显式列出 `TalentAllocationDraft.pendingRanks`、`previousPendingRanks`、`slotToTalentId`、learned ranks) |

Round 5 全部 P1–P3 已关闭。本轮新增 findings 都是更深层的 game-design / balance / 隐藏双真源问题,不构成 BLOCKER。

---

## Summary (本轮一句话)

PR04-01 的工程合同已经接近 production-ready,但作为类 ToME 职业被动天赋的**首批落地**,文档对"被动到底有多吸引、跨职业是否平衡、条件被动是否真在战斗里触发、玩家可见数值是否单真源" 这些 game design ground truth 还是用主观语言一笔带过;实现层完全可以"测试全绿但玩家点完觉得白点"。建议在 `04-01b` Task A 数据落地前,补齐 HIGH-1/2/3 三条合同与对应 lint,把"被动天赋的玩法价值"从主观判断变成可验收数据。
