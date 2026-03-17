# Phase 3 PR-Level Director Review — Round 2

**日期**：2026-03-17  
**审阅范围**：

1. `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
2. `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
3. `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
4. `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
5. `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
6. `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
7. 对照：
   - `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
   - `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
   - `docs/phase3/2026-03-13-phase3-verification-checklist.md`

**结论**：

1. 上一轮的结构性 `P0` 基本已经修掉了。
2. 当前版本已经接近“可以开始写代码”，但还没有完全达到“执行合同闭环”。
3. 剩余问题主要集中在：
   - `W5` 的 unlock state 与 UI availability state 还没完全闭环
   - `W6` 的 `WorldProgress` 已进入运行时合同，但 save/persistence 和 reward claim 语义还没完全跟上
   - 少数 contract 已从“大方向正确”进入“还差一层精度”的阶段

本轮结论：**0 P0 / 6 P1 / 5 P2**。

---

## 1. Findings

### [P1] `WorldProgress` 已进入运行时合同，但 `SaveDataV2` 仍然只保存 `questState`

**位置**

1. [PR-06 L122-L164](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L122)
2. [核心补充文档 L3011-L3018](../2026-03-13-core-systems-design-and-phase-supplements.md#L3011)

**问题**

`PR-06` 已经把 `WorldProgressDef(questStates/worldFlags/unlockedRoutes)` 提升成正式运行时合同，但 `SaveDataV2` 的最小 schema 仍然只有 `questState: QuestSnapshot`，没有保存 `worldFlags`、`unlockedRoutes` 或同等聚合对象。

**影响**

一旦 `W6` 真把 gate、route 和世界推进建立起来，save/load round-trip 就会丢失长局推进状态。这个问题不是实现细节，而是当前文档链的契约断裂。

**建议**

把 `SaveDataV2` 的推进字段统一升级为：

1. `worldProgress: WorldProgressSnapshot`
2. 或至少把 `questState` 扩成能无损表达 `worldFlags / unlockedRoutes / claimedRewards / defeatedBosses`

同时补一条 `save/load preserves world progress` 的自动化验证。

### [P1] `GateCondition` 与 `RouteReward` 需要的状态，在 `WorldProgressDef` 里还不完整

**位置**

1. [PR-06 L141-L148](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L141)
2. [PR-06 L153-L163](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L153)

**问题**

`GateCondition` 已允许依赖 `requiredBossKill`，`RouteReward` 也被提升为正式合同；但 `WorldProgressDef` 当前只保存：

1. `questStates`
2. `worldFlags`
3. `unlockedRoutes`

它没有显式保存：

1. `defeatedBossIds`
2. `claimedRouteRewards`

**影响**

1. `requiredBossKill` 目前没有稳定的数据归属。
2. `RouteReward` 默认会落入“是否可重复领取”未定义状态。
3. gate/reward 逻辑后续容易退化成 event callback 特判。

**建议**

二选一，但必须明确：

1. Boss 击杀和 route reward claim 都统一折叠进 `worldFlags`
2. 或给 `WorldProgressDef` 显式增加 `defeatedBossIds` 与 `claimedRouteRewards`

现在不写清楚，后面实现一定各写各的。

### [P1] `ClassUnlockState` 与 UI 的 `LOCKED / UNLOCKED_BUT_UNAVAILABLE / PLAYABLE` 还没有闭环

**位置**

1. [PR-05 L131-L136](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L131)
2. [PR-05 L318-L324](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L318)
3. [PR-05 L399-L401](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L399)

**问题**

`W5a` 定义的后端状态是：

1. `LOCKED`
2. `DEV_UNLOCKED`
3. `RELEASE_UNLOCKED`

但 `W5c` 的 UI 状态却是：

1. `LOCKED`
2. `UNLOCKED_BUT_UNAVAILABLE`
3. `PLAYABLE`

而且 `Shadowblade / Warden` 被描述为“`UNLOCKED_BUT_UNAVAILABLE` 或 `LOCKED`”，说明当前版本仍然没有把后端状态与 UI 状态做成单值映射。

**影响**

角色创建页、Profile、实验室入口和正式玩家解锁路径可能各自解释一遍状态机。

**建议**

显式拆成两个对象：

1. `ClassUnlockState`
2. `ClassPlayabilityState`

然后由 `ClassAvailabilityResolver` 唯一输出 UI 状态。不要让 UI 文案层自己推测。

### [P1] 进阶职业的“3 树轻量版执行口径”仍然没有同步回核心补充文档

**位置**

1. [PR-05 L61-L61](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L61)
2. [PR-05 L186-L189](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L186)
3. [PR-05 L211-L214](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L211)
4. [核心补充文档 L2168-L2174](../2026-03-13-core-systems-design-and-phase-supplements.md#L2168)

**问题**

`PR-05` 已经把 Phase 3 执行权威改成“进阶职业 3 树轻量版”，但核心补充文档仍然写着“每个进阶职业 4 棵天赋树（4×4=16 天赋）”。

**影响**

内容预算、天赋树数量、后续 Phase 4 接续口径仍然存在双轨。

**建议**

必须二选一：

1. 在核心补充文档中明确“Phase 3 执行口径为 3 树轻量版，4 树是长期目标”
2. 或撤销 `PR-05` 的 3 树口径

现在这种写法依然要求实现者自己裁决。

### [P1] `RescueInventoryPolicy` 只保证“有货”，没有保证“买得起”

**位置**

1. [PR-06 L230-L244](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L230)
2. [PR-06 L401-L402](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L401)

**问题**

现在的 rescue policy 已经保证了第一、第二商店的工具类型，但没有任何价格、shard budget、最小可负担性合同。也就是说：

1. 物品可以在库存里
2. 但玩家可能根本买不起

**影响**

这会让“有保底救火路径”变成名义保底，而不是实际保底。`longRunLab` 通过与否可能继续被 shard 曲线偶然性主导。

**建议**

补最小经济合同：

1. `AffordableRescueSlotPolicy`
2. `expectedShardBudgetByCheckpoint`
3. `mandatoryAffordableItemCount`

如果不想上完整经济表，至少也要定义“第二商店的位移/净化保底必须对 checkpoint 期望 shard 可负担”。

### [P1] `Spellblade` 的 `EQUILIBRIUM` 仍然缺“动作归类”合同

**位置**

1. [PR-05 L215-L220](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L215)
2. [PR-05 L378-L378](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L378)

**问题**

当前口径只说“按上一回合最后一个成功施放的技能流派偏移一次”，但没有定义：

1. 普攻算哪一类
2. 铭文算不算流派
3. 混合技能如何归类
4. 持续技能开关、被动触发、免费动作是否影响平衡值

**影响**

`Spellblade` 的核心状态轴仍然存在实现自由度，后续最容易在 combat、UI、lab、tooltip 四处各自解释。

**建议**

补一个最小合同即可：

1. `EquilibriumAffinity = PHYSICAL / ARCANE / NEUTRAL`
2. 只有带 affinity 的已确认主动技能才影响平衡值
3. 铭文、被动、free action 默认 `NEUTRAL`

---

## 2. Secondary Findings

### [P2] `DescriptionModel` 仍然把 placeholder 提前字符串化了

**位置**

1. [PR-03 L153-L158](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L153)

**问题**

虽然 `core` 不再输出最终 localized text 了，但 `DescriptionModel.placeholders` 仍然是 `Map<String, String>`。这会把数字格式、百分号、复数、单位等格式化决策提前做掉。

**建议**

把 placeholder 改成 typed slot，例如：

1. `Map<String, SemanticValue>`
2. `Map<String, NumericSlot>`

否则 i18n 只是从“整句字符串”退化成了“槽位字符串”。

### [P2] `W2b` 仍然没有把 “steady-state 看 `RenderSnapshot`，瞬时提示看事件” 说死

**位置**

1. [PR-02 L32-L32](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L32)
2. [PR-02 L232-L247](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L232)
3. [核心补充文档 L1364-L1364](../2026-03-13-core-systems-design-and-phase-supplements.md#L1364)

**问题**

`PR-02` 仍然写“状态 UI 语义从 core 事件正确消费”，但没有把 client 稳态同步锚点明确绑定到 `WorldStateChangedEvent -> RenderSnapshot`。

**建议**

补一句硬规则：

1. icon / 层数 / 剩余回合的 steady-state 只看 `RenderSnapshot`
2. event 只负责瞬时反馈，如覆盖、净化、打破隐匿

### [P2] `P2toP3FormulaComparisonTest` 的 `±30%` 口径依然过粗

**位置**

1. [PR-01 L259-L259](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L259)
2. [PR-01 L305-L305](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L305)
3. [Checklist L39-L41](../phase3/2026-03-13-phase3-verification-checklist.md#L39)

**问题**

统一 `±30%` 对 damage、hit chance、apply chance 一视同仁，仍然过于钝。它容易出现：

1. 某个分项退化很多，但整体仍落在容忍区间
2. 极端边界回归被“平均差异”吃掉

**建议**

后续最好拆成：

1. damage band
2. hit chance band
3. apply chance band
4. 关键边界点 exact expectation

### [P2] `Berserker` 的 `HATE` 合同里仍有两处模糊表述

**位置**

1. [PR-05 L191-L196](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L191)

**问题**

当前写法里还有：

1. `OnKill 可选爆发增益`
2. `高 HATE 段提供伤害或技能强化`

这两句还不是“冻结口径”，而是“设计意图”。

**建议**

在真正落地前再补一层：

1. `OnKill` 是否进入正式基线
2. 高 `HATE` 的强化究竟作用于数值、技能可用性，还是两者兼有

### [P2] “已发现 profile” 仍然没有 schema 归属

**位置**

1. [PR-06 L286-L288](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L286)
2. [核心补充文档 L3166-L3172](../2026-03-13-core-systems-design-and-phase-supplements.md#L3166)

**问题**

`PR-06` 仍然说局间持久化包括“已发现 profile”，但 `ProfileData` schema 只保存：

1. `releaseUnlockedClasses`
2. `runHistory`

“已发现 profile” 当前没有字段承载，也没有权威定义。

**建议**

二选一：

1. 删除这句，回到当前最小 schema
2. 显式加字段并说明用途

---

## 3. Final Judgement

这轮修订已经把大部分真正会导致 Phase 3 推倒重来的问题解决了。当前剩余问题不再是“大方向错了”，而是“合同模型还差最后一层闭环”。

如果要决定是否开始代码实施，我的结论是：

1. 可以开始
2. 但最好先把上面的 `P1` 再收一轮

优先顺序建议如下：

1. 先补 `WorldProgress <-> SaveDataV2` 的对齐
2. 再收口 `ClassUnlockState <-> UI availability` 的状态机
3. 再补 `RescueInventoryPolicy` 的 affordability 合同
4. 最后修 `Spellblade` affinity、`DescriptionModel` typed slot 和几处 wording / schema 漂移

做到这一步，Phase 3 PR 级文档基本就可以视为稳定实施输入了。
