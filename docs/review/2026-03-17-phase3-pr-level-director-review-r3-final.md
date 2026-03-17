# Phase 3 PR-Level Director Review — Round 3 Final

**日期**：2026-03-17  
**审阅视角**：资深游戏设计与开发总监  
**审阅范围**：

1. `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
2. `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
3. `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
4. `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
5. `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
6. `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
7. 交叉对照：
   - `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
   - `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
   - `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
   - `docs/phase3/2026-03-13-phase3-verification-checklist.md`

## 0. 直接结论

这轮修订已经把上一版 review 的主阻塞项基本处理掉了。

我对当前版本的判断是：

1. **没有剩余 `P0` 阻塞项**。
2. 文档体系已经从“方向有结构性问题”进入“边界和精度还需收口”的阶段。
3. 现在最值得修的，不再是大重构，而是把几个关键 contract 再钉死，避免 Phase 3 进入编码后因为小口径漂移产生返工。

本轮结论收口为：**0 P0 / 6 P1 / 7 P2**。

## 1. 本轮已确认修复

以下上一轮的核心问题，本轮已确认基本收口：

1. `HOLY` 已收口为标签增伤默认口径，不再把亡灵/恶魔默认负神圣抗性当通用基线。
2. `P3-W1` 已切成 `formula corpus + CombatResolutionTrace + TraceEnvelope`，不再试图提前冻结整条 Phase 3 trace。
3. `OVERCHARGE` 已改为 `debuff`，并补了 `ActorEffect / AreaEffectEmitter / WorldEffect` carrier 分层。
4. `WAR_CRY_BUFF` 的名字级特判已被移出正式规则面，改为通用唯一性字段。
5. 动态说明已收口为 `DescriptionModel` / presenter 模式，不再让 `core` 直接产出最终本地化字符串。
6. `prerequisite`、`TalentAllocationDraft`、`telegraphRef` 已进入正式 schema。
7. `selectionPolicy`、`TelegraphSpec`、`ThreatRatingResolver` 和 `bossHarness 2 -> 3 Boss` 的阶段口径已收口。
8. `P3-W5` 已拆成 `W5a / W5b / W5c`，并引入 `DEV_UNLOCKED` 开发态可用性。
9. `WorldProgress / Quest / Gate / RouteReward`、`RescueInventoryPolicy`、`AffixTagWeighting / AffixBlacklist`、`RunSummary` 与 `headlessTurnEquivalent` 都已经进入正式文本。

这说明本轮文档已经具备进入代码实施前的最后收边条件。

---

## 2. P1 问题

### P1-1 `WorldProgress` 已进入运行时合同，但 `SaveDataV2` 还没有对齐

**位置**

1. [PR-06 L122-L164](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L122)
2. [核心补充文档 L3011-L3018](../2026-03-13-core-systems-design-and-phase-supplements.md#L3011)

**问题**

`PR-06` 已经把 `WorldProgressDef(questStates / worldFlags / unlockedRoutes)` 提升成长局正式合同，但存档骨架仍然只有 `questState: QuestSnapshot`。这意味着 save/load round-trip 仍然没有正式承诺会保留：

1. `worldFlags`
2. `unlockedRoutes`
3. 未来 route reward / gate 依赖的运行态世界推进信息

**影响**

一旦 `W6` 代码实现推进到中途存档，长局推进状态有很大概率在读档后丢失或半丢失。

**建议**

把 `SaveDataV2` 的推进字段统一升级成：

1. `worldProgress: WorldProgressSnapshot`
2. 或把现有 `QuestSnapshot` 扩成能无损表达 `worldFlags / unlockedRoutes / defeatedBossIds / claimedRouteRewards`

同时补一条 `save/load preserves world progress` 的自动化验证。

### P1-2 三类 effect carrier 已拆开，但跨 carrier 的总执行顺序仍未冻结

**位置**

1. [PR-02 L146-L146](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L146)
2. [PR-02 L203-L206](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L203)
3. [核心补充文档 L1361-L1364](../2026-03-13-core-systems-design-and-phase-supplements.md#L1361)

**问题**

文档已经定义三类 carrier 共用 tick/expire/trace 引擎，但没有冻结它们之间的稳定总顺序。当前仍不清楚：

1. `ActorEffect` 与 `AreaEffectEmitter` 谁先 tick
2. `WorldEffect` 与 actor DoT 的先后关系
3. 死亡检查、净化、phase 切换在跨 carrier 场景下落在哪个边界点

**影响**

这会直接影响 replay hash、`CombatResolutionTrace` 的稳定性，以及“先被毒死还是先被净化”这类边界结果。

**建议**

在 `PR-02` 明确补一条总顺序，例如：

1. `ActorEffect`
2. `AreaEffectEmitter`
3. `WorldEffect`

并声明每层内部的 tie-break 和死亡检查点。

### P1-3 `ClassUnlockState` 与 UI 的 availability state 仍未形成单值映射

**位置**

1. [PR-05 L129-L142](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L129)
2. [PR-05 L318-L324](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L318)

**问题**

后端状态已经固定为：

1. `LOCKED`
2. `DEV_UNLOCKED`
3. `RELEASE_UNLOCKED`

但 UI 状态写成：

1. `LOCKED`
2. `UNLOCKED_BUT_UNAVAILABLE`
3. `PLAYABLE`

而且 `Shadowblade / Warden` 仍被描述为“`UNLOCKED_BUT_UNAVAILABLE` 或 `LOCKED`”，说明现在没有唯一映射。

**影响**

角色创建页、Profile、实验室入口和正式玩家解锁路径，仍然可能各自解释一遍状态机。

**建议**

显式引入：

1. `ClassUnlockState`
2. `ClassPlayabilityState`

并由 `ClassAvailabilityResolver` 唯一负责映射和输出 UI 状态，避免 UI 自己猜。

### P1-4 `AIProfile.selectionPolicy` 已补上，但 tie-break 规则仍不够硬

**位置**

1. [PR-04 L104-L115](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L104)
2. [PR-04 L176-L183](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L176)
3. [PR-04 L241-L247](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L241)

**问题**

现在已经有 `selectionPolicy`，但仍没有写死：

1. `DETERMINISTIC_PRIORITY` 下 `orderKey` 相同如何 tie-break
2. `WEIGHTED_RANDOM` 下候选动作在采样前按什么顺序归一化
3. `orderKey` 缺失、`weight` 缺失、候选集合全 0 权重时如何处理

**影响**

AI 看起来“逻辑正确”，但 replay 与 trace 仍然可能在 YAML 顺序变化或 loader 实现变化后漂移。

**建议**

补一条硬规则：

1. 候选动作先按 `orderKey asc`
2. 再按 `actionId asc`
3. 然后再进入 deterministic pick 或 weighted sampling

同时给 `weight` 缺省值和“全 0 权重”的 fallback 明确口径。

### P1-5 `Spellblade` 的 `EQUILIBRIUM` 仍然缺少动作归类合同

**位置**

1. [PR-05 L215-L220](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L215)
2. [PR-05 L377-L378](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L377)

**问题**

当前只说“按上一回合最后一个成功施放的技能流派偏移一次”，但没有冻结：

1. 普攻算什么流派
2. 铭文算不算流派
3. 混合技能如何归类
4. sustain toggle、被动触发、free action 是否影响平衡值

**影响**

`EQUILIBRIUM` 作为 Spellblade 的核心状态轴，仍然存在 combat、UI、tooltip、lab 各自解释的空间。

**建议**

补一个最小合同即可：

1. `EquilibriumAffinity = PHYSICAL / ARCANE / NEUTRAL`
2. 只有带 affinity 的已确认主动技能改变平衡值
3. 铭文、被动、free action 默认 `NEUTRAL`

### P1-6 `RescueInventoryPolicy` 只保证“库存存在”，没有保证“经济上可负担”

**位置**

1. [PR-06 L230-L244](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L230)
2. [PR-06 L401-L402](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L401)

**问题**

现在的 rescue policy 已经能保证第二商店有位移/净化/护盾类工具，但没有任何价格、期望 shard 预算或可负担性合同。

**影响**

文档只能保证“货架上有货”，不能保证“玩家在该 checkpoint 买得起”。长局稳定性仍然可能被 shard 曲线偶然性主导。

**建议**

补最小经济合同：

1. `AffordableRescueSlotPolicy`
2. `expectedShardBudgetByCheckpoint`
3. `mandatoryAffordableItemCount`

不需要完整经济表，但至少要保证关键救火工具对 checkpoint 期望 shard 可负担。

---

## 3. P2 问题

### P2-1 核心补充文档里仍残留几处旧骨架，容易误导实施

**位置**

1. [核心补充文档 L764-L783](../2026-03-13-core-systems-design-and-phase-supplements.md#L764)
2. [核心补充文档 L2170-L2177](../2026-03-13-core-systems-design-and-phase-supplements.md#L2170)
3. [核心补充文档 L2233-L2252](../2026-03-13-core-systems-design-and-phase-supplements.md#L2233)

**问题**

核心补充文档里还残留几处旧样例：

1. 3.3 节主体仍然是 `CombatTrace` 类型定义，而不是 `CombatResolutionTrace / TraceEnvelope`
2. 进阶职业设计骨架仍写着“4 棵天赋树”
3. talent YAML 示例还保留字符串式 `talentPrereqs` 注释和 `description` 字段

**建议**

把这些样例同步成当前执行口径。现在的大方向已经对了，剩下的主要是把旧样例清干净。

### P2-2 `Status HUD` 的 steady-state 数据源仍有边界歧义

**位置**

1. [PR-02 L32-L32](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L32)
2. [PR-02 L241-L247](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L241)
3. [核心补充文档 L1364-L1364](../2026-03-13-core-systems-design-and-phase-supplements.md#L1364)

**问题**

`PR-02` 仍然写“状态 UI 语义从 core 事件正确消费”，但没有把 steady-state 数据源明确锚到 `RenderSnapshot`。

**建议**

补一句硬规则：

1. icon / 层数 / 剩余回合的 steady-state 只看 `RenderSnapshot`
2. event 只负责瞬时提示，如覆盖、净化、隐匿被打破

### P2-3 `OVERCHARGE` 的“可被净化或驱散”表述仍然混用了 `cleanse / dispel`

**位置**

1. [PR-02 L116-L116](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L116)
2. [核心补充文档 L2427-L2429](../2026-03-13-core-systems-design-and-phase-supplements.md#L2427)

**问题**

核心术语里，`cleanse` 与 `dispel` 已经有明确分工。`OVERCHARGE` 已收口为负面状态，再写“可被净化或驱散”会重新把术语弄脏。

**建议**

改成：

1. `OVERCHARGE` 可被 `cleanse`
2. 若未来存在“移除敌方负面”的特殊能力，再单独声明，不复用 `dispel`

### P2-4 `DescriptionModel` 仍然把 placeholder 过早降成了字符串

**位置**

1. [PR-03 L153-L158](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L153)

**问题**

虽然 `core` 已不再输出最终 localized text，但 `DescriptionModel.placeholders` 仍然是 `Map<String, String>`，这会提前丢掉数值类型信息。

**建议**

改成：

1. `Map<String, DescriptionValue>`
2. 或最少保留 `Int / Double / Boolean / String` 的联合类型

这样 presenter 才能做 locale-aware 数值格式化、单位、颜色强调。

### P2-5 `ThreatRatingResolver` 已出现引用点，但标准 threat profile 还没有正式注册表

**位置**

1. [PR-04 L198-L209](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L198)

**问题**

`TelegraphSpec` 已经包含 `threatProfileId`，但当前文档还没有明确：

1. threat profile 在哪里注册
2. baseline profile 是否按 zone / difficulty / archetype 分层
3. content 作者能否在 YAML 中自由新增 profile

**建议**

补最小注册表合同，例如：

1. `ThreatProfileDef`
2. `threat_profiles/*.yaml`
3. `ThreatProfileLint`

### P2-6 `RouteReward` 的一次性领取语义还不够明确

**位置**

1. [PR-06 L145-L148](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L145)
2. [PR-06 L153-L163](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L153)

**问题**

`RouteReward` 已经进入正式合同，但文档还没有明确奖励是：

1. 首次解锁时发
2. 首次通过时发
3. 可重复领取

这件事最好不要留给实现者靠 `worldFlags` 猜。

**建议**

给 `RouteReward` 或 `WorldProgressDef` 加一个最小语义：

1. `claimPolicy`
2. 或 `claimedRouteRewards`

### P2-7 `P2toP3FormulaComparisonTest` 的 `±30%` 接受区间仍然偏粗

**位置**

1. [PR-01 L259-L259](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L259)
2. [Checklist L39-L41](../phase3/2026-03-13-phase3-verification-checklist.md#L39)

**问题**

当前仍然用单一 `±30%` 同时覆盖：

1. 伤害
2. 命中率
3. 状态施加率

这会让公式比较测试过于粗糙。

**建议**

至少拆成三类阈值：

1. 伤害类
2. 命中类
3. 状态施加率类

否则这个测试更像大范围烟雾，而不是精确回归。

---

## 4. 推荐修订顺序

1. 先修 `SaveDataV2 <-> WorldProgress` 的契约断层。
2. 再冻结跨 carrier 总顺序、AI tie-break 和 `EQUILIBRIUM` affinity。
3. 然后收口 `ClassUnlockState -> UI state` 映射和 `RescueInventoryPolicy` 的 affordability。
4. 最后一次性清理核心补充文档里的旧样例与 P2 精度问题。

---

## 5. 最终判断

这轮修订后的 Phase 3 PR 文档已经明显进入“可落地”区间。

如果把上面的 `P1` 再收掉，剩下的基本就是实施过程中的正常文档维护，而不是会在编码初期引发返工的设计缺口。换句话说：

1. **现在已经不是“不能开始写代码”**
2. 但也还**不建议在这些残余 contract 未收口前全面铺开实现**

优先修完 `P1`，再进代码，会更稳。  
