# Phase 3 PR-Level Director Review R2

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

## 0. 直接结论

这一轮修订已经把上一版 review 的主阻塞项基本处理掉了。

当前判断：

1. **没有新的 P0 阻塞项**。
2. 剩余问题主要是“定义不够精确”，不是“方向错了”。
3. 只要把下面的 `P1 / P2` 收口，Phase 3 PR 文档就可以进入稳定实施阶段。

---

## 1. P1 问题

### P1-1 `ActorEffect / AreaEffectEmitter / WorldEffect` 已拆开，但跨 carrier 的总执行顺序仍未冻结

**位置**

1. [PR-02 L146-L146](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L146)
2. [PR-02 L203-L206](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L203)
3. [核心补充文档 L1361-L1364](../2026-03-13-core-systems-design-and-phase-supplements.md#L1361)

**问题**

文档已经说明三类 carrier 共用 tick/expire/trace 引擎，但**没有定义它们之间的稳定总顺序**。现在仍然不清楚：

1. actor 身上的 `BLEED` 和地面 `AreaEffectEmitter`，谁先 tick
2. `WorldEffect` 与 actor DoT 的先后关系
3. 同一时点发生死亡、净化、phase 切换时，在哪个边界点做死亡检查

**影响**

这会直接影响：

1. replay hash 稳定性
2. `CombatResolutionTrace` 一致性
3. 边界案例，例如“先被毒死还是先被净化”

**建议**

在 `PR-02` 明确冻结一条总顺序，例如：

1. `ActorEffect`
2. `AreaEffectEmitter`
3. `WorldEffect`

并补充每一层内部的 tie-break 与死亡检查点。

### P1-2 `AIProfile` 已引入 `selectionPolicy`，但 tie-break 仍未冻结，确定性仍有缝

**位置**

1. [PR-04 L104-L115](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L104)
2. [PR-04 L176-L183](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L176)
3. [PR-04 L241-L247](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L241)

**问题**

`selectionPolicy` 已经补上，但仍然没有明确：

1. `DETERMINISTIC_PRIORITY` 下多个动作 `orderKey` 相同怎么办
2. `WEIGHTED_RANDOM` 下候选动作在采样前按什么顺序归一化
3. `orderKey` 缺失时是否退回 `actionId asc`

**影响**

这会让 AI 决策在“逻辑正确”的同时仍然出现 replay 漂移，尤其是在：

1. YAML 顺序被重排
2. loader 改成非稳定容器
3. 多个动作同权重 / 同优先级

**建议**

补一句硬规则：

1. 候选动作先按 `orderKey asc`
2. 再按 `actionId asc`
3. 再进入 deterministic pick 或 weighted sampling

### P1-3 `Phase 3 进阶职业 3 树轻量版` 仍未同步回核心权威文档

**位置**

1. [PR-05 L61-L61](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L61)
2. [PR-05 L186-L186](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L186)
3. [PR-05 L211-L211](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L211)
4. [核心补充文档 L2170-L2173](../2026-03-13-core-systems-design-and-phase-supplements.md#L2170)

**问题**

PR 级执行文档已经把 `Berserker / Spellblade` 固定为 **Phase 3 的 3 树轻量版**，但核心补充文档仍然写的是“进阶职业 4 棵天赋树（4×4=16）”。

**影响**

这是仍然存在的跨文档权威冲突。内容作者、QA 和后续 reviewer 还会被两套预算拉扯。

**建议**

二选一：

1. 直接修改核心补充文档对应段落，声明 `Phase 3` 执行口径为 3 树轻量版
2. 或在该段前加一条显式 override，说明 Phase 3 以 `PR-05` 为权威

---

## 2. P2 问题

### P2-1 `Status HUD` 的 steady-state 数据源仍有边界歧义

**位置**

1. [PR-02 L32-L32](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L32)
2. [PR-02 L241-L247](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L241)
3. [核心补充文档 L1364-L1364](../2026-03-13-core-systems-design-and-phase-supplements.md#L1364)
4. [核心补充文档 L3044-L3047](../2026-03-13-core-systems-design-and-phase-supplements.md#L3044)

**问题**

`PR-02` 仍然写“状态 UI 语义从 core 事件正确消费”，但核心补充文档已经明确 `WorldStateChangedEvent -> RenderSnapshot` 才是 client 的唯一同步锚点。

**建议**

补一句：

1. steady-state 的图标、层数、剩余回合来自 `RenderSnapshot`
2. event 只负责瞬时提示，如“被覆盖”“被净化”“隐匿被打破”

### P2-2 `OVERCHARGE` 的“可被净化或驱散”表述仍然混用了 `cleanse / dispel`

**位置**

1. [PR-02 L116-L116](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L116)
2. [核心补充文档 L2428-L2429](../2026-03-13-core-systems-design-and-phase-supplements.md#L2428)

**问题**

核心术语里：

1. `cleanse` = 移除负面状态
2. `dispel` = 移除正面状态

`OVERCHARGE` 已经收口为负面状态，再写“可被净化或驱散”会让术语重新变脏。

**建议**

改成：

1. `OVERCHARGE` 可被 `cleanse`
2. 若未来存在“移除敌方负面”的特殊能力，单独声明，不复用 `dispel`

### P2-3 `DescriptionModel` 仍然把 placeholder 过早降成了字符串

**位置**

1. [PR-03 L153-L158](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L153)

**问题**

`DescriptionModel.placeholders: Map<String, String>` 仍然过早丢失了数值类型信息。这样 presenter 很难做：

1. locale-aware number formatting
2. 百分比/整数/浮点/单位的差异化渲染
3. 后续 tooltip 比较或颜色强调

**建议**

改成：

1. `Map<String, DescriptionValue>`
2. 或至少保留 `Int / Double / Boolean / String` 的联合类型

### P2-4 `CombatResolutionTrace` 的命名已经更新，但工具名还停留在旧 `CombatTrace` 心智

**位置**

1. [PR-01 L221-L225](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L221)

**问题**

核心概念已经切成 `CombatResolutionTrace + TraceEnvelope + corpus`，但工具名仍然叫：

1. `CombatTraceGolden.kt`
2. `CombatTraceGoldenTest.kt`

**建议**

把工具名同步为 `ResolutionTraceGolden` 或等价命名，避免旧概念残留。

### P2-5 `±30%` 的接受区间仍然过粗

**位置**

1. [PR-01 L259-L259](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L259)

**问题**

现在仍然用单一的 `±30%` 接受区间覆盖：

1. 伤害
2. 命中率
3. 状态施加率

这三类量的统计性质不同，用一个阈值会掩盖局部严重回归。

**建议**

至少拆成：

1. damage band
2. hit/apply probability band
3. trace step invariant

### P2-6 `BossPhaseDef.turnCount` 的作用域仍未定义

**位置**

1. [PR-04 L167-L167](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L167)

**问题**

`turnCount` 仍然没有说明是：

1. encounter-global
2. phase-local
3. boss-actor-local

**建议**

补一条显式说明，不然后续 phase 触发条件很容易在数据和实现里各自理解。

### P2-7 `AIDecisionTrace` 仍然偏瘦，不足以支撑隐匿/嘲讽类问题排查

**位置**

1. [PR-04 L241-L255](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L241)

**问题**

现在 trace 已有 `selectionPolicy` 和 `rngRoll`，但仍缺：

1. `filteredOutReasons`
2. `targetRef`
3. `lastKnownPosition`
4. `perceptionSnapshot`

**建议**

至少补前两项，否则 `STEALTH / TAUNT` 的白盒异常还会很难排。

### P2-8 `ClassUnlockState` 与 UI 展示态的映射仍然没有被正式写死

**位置**

1. [PR-05 L129-L142](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L129)
2. [PR-05 L318-L323](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L318)

**问题**

现在运行时有：

1. `LOCKED / DEV_UNLOCKED / RELEASE_UNLOCKED`

UI 又有：

1. `LOCKED / UNLOCKED_BUT_UNAVAILABLE / PLAYABLE`

但两者没有正式映射表。

**建议**

加一张映射表，避免 UI、实验室和 Profile 各自解释。

### P2-9 `PR-06` 里“已发现 profile”仍然是一个未定义术语

**位置**

1. [PR-06 L287-L287](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L287)

**问题**

“已发现 profile”不是当前文档体系里一个清晰对象。这里大概率是旧表述残留。

**建议**

明确改成以下之一：

1. `releaseUnlockedClasses`
2. `runHistory`
3. `discoveredLoreProfile` / `contentProfile`

不要继续保留模糊名词。

---

## 3. 结论

这一轮修订后，Phase 3 PR 文档已经明显进入“可实施”区间了。剩下的问题都集中在：

1. **排序规则没写死**
2. **语义边界还差最后一层收口**
3. **少量跨文档/跨层命名漂移还没完全抹平**

如果只优先修 3 件事，我建议顺序是：

1. 冻结 effect carrier 的跨宿主执行顺序
2. 冻结 AI 动作 tie-break 与 deterministic secondary sort
3. 把进阶职业 `3 树轻量版` 同步回核心权威文档

做完这三件事，剩余项就都属于可控的 polish，而不是结构性返工风险。
