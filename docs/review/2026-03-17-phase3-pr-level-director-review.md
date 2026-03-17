# 2026-03-17 Phase 3 PR 级开发文档总监审阅（核实增强版）

**日期**：2026-03-17  
**审阅视角**：资深游戏设计与开发总监  
**审阅目标**：核实 `Phase 3` 的 `PR-01 ~ PR-06` 是否已经达到“可直接稳定实施”的文档质量，并把原始审阅稿补成一份可执行的修订输入。  

**本次核实范围**：

1. `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
2. `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
3. `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
4. `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
5. `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
6. `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
7. 交叉对照：
   - `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
   - `docs/phase3/2026-03-13-phase3-verification-checklist.md`
   - `docs/phase3/roadmap.md`
   - `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
   - `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
   - `docs/mvp-development-guide.md`

**本稿相较原版新增**：

1. 明确哪些高优先级判断已经被源文档直接支撑，哪些只是建议性优化。
2. 给出统一修订基线，避免这份 review 自己再变成“第二套权威”。
3. 增加按文档的落地修改清单，便于直接回写上游文档。
4. 收口推荐修订顺序，避免修了下游又被上游打回。

**行号说明**：

1. 文中的行号锚点基于 2026-03-17 的仓库快照。
2. 如果后续文档继续编辑导致锚点漂移，应以节标题和关键句搜索为准，而不是机械依赖旧行号。

## 0. 直接结论

原审阅稿的核心判断成立，而且我没有在源文档里找到足以推翻这些 `P0` 结论的反证。

当前这组 `PR-01 ~ PR-06` 文档已经明显比 `Phase 3` 总览更接近实施，但仍然**不能直接作为稳定的代码实施输入**。主要原因不是“内容还不够多”，而是下面三类问题仍未收口：

1. 上游权威合同存在硬冲突，同一系统出现两套口径。
2. 下游 PR 文档过早冻结了一些依赖上游稳定性的 schema、golden 和验收门禁。
3. `W5 / W6` 已经开始跨 `core / game / client / tools` 同时拉多条主线，超出单个工作包的合理粒度。

因此最合理的结论不是“重写整套 Phase 3”，而是：

1. 保留当前 Phase 3 的主框架。
2. 先修掉下文列出的 `P0` 和关键 `P1`。
3. 把统一修订基线写回权威文档后，再启动大规模代码实施。

## 1. 核实摘要

下表只列最关键、且已经被源文档直接支撑的结论：

| 主题 | 核实结果 | 直接证据 | 建议动作 |
| --- | --- | --- | --- |
| `HOLY` 伤害模型冲突 | 已核实，且属于硬冲突 | [PR-01](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L121) 要求走标签增伤；[核心补充文档](../2026-03-13-core-systems-design-and-phase-supplements.md#L213) 又给亡灵/恶魔默认 `holyResistance = -25` | 删除通用负神圣抗性基线，只保留标签乘算为默认正式口径 |
| `W5` 进阶职业解锁依赖 `W6` 通关终局 | 已核实 | [PR-05](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L43) 固定通关解锁；[PR-06](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L47) 把通关定义绑定到 `abyssal_heart` | 把玩家正式解锁与开发态可用性拆开 |
| 职业资源合同不稳定 | 已核实，且会影响 `core` 契约 | [PR-05](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L145) 把 `Spellblade` 写成 `MANA + EQUILIBRIUM`；[核心补充文档](../2026-03-13-core-systems-design-and-phase-supplements.md#L1959) 仍是单 `EQUILIBRIUM`；`ProfessionDef` 仍是单 `resourceType` | 先升级资源 schema，再冻结职业资源语义 |
| 动态说明越过模块边界 | 已核实 | [PR-03](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L107) 把动态说明实现放在 `core`，并在 [L114](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L114) 产出 `localized output` | `core` 只输出语义模型，最终本地化字符串放到 `client` 或 presenter |
| `W5 / W6` 工作包过大且漏合同 | 已核实 | [路线图](../2026-03-13-phase2-to-phase5-final-roadmap.md#L361) 要求 `W6` 包含世界分支、任务、经济和长局回归；[PR-06](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L57) 实际没有 `Quest / Gate / WorldProgress` 合同 | 拆工作包，并补结构化世界推进合同 |
| 微漂移已经足够影响实施 | 已核实 | `dangerLevel`、`bossHarness` 覆盖数、第二商店位置、zone 数量、`grey_gate_depths` 终局定义等在多文档间漂移 | 在开始实现前做一次全量同步修订 |

这意味着原 review 的方向是对的，但原稿更像“问题清单”；本稿的任务是把它补成“修订输入”。

## 2. P0 问题

### P0-1 `HOLY` 伤害模型在权威文档间互相冲突

**位置**

1. [PR-01 L121-L125](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L121)
2. [核心补充文档 L209-L213](../2026-03-13-core-systems-design-and-phase-supplements.md#L209)
3. [核心补充文档 L1744-L1744](../2026-03-13-core-systems-design-and-phase-supplements.md#L1744)

**问题**

`PR-01` 明确要求 `HOLY` 对亡灵/恶魔继续走“标签增伤路径，不走负抗性路径”；核心补充文档又把亡灵/恶魔写成默认 `holyResistance = -25`。这不是注释层差异，而是同一伤害通道被定义成了两套计算口径。

**核实结果**

这条判断成立，而且是最典型的上游合同冲突之一。

**影响**

1. `Templar`、`BANE`、怪物模板和 `CombatTrace` 都会出现双重解释空间。
2. `holyPenetration`、负抗性、标签增伤之间的优先顺序无法稳定冻结。
3. `W1` 的公式与 golden 即使实现出来，也没有真正冻结到单一模型。

**建议**

保留“标签乘算”作为正式默认基线，把“亡灵/恶魔默认负神圣抗性”从通用怪物基线删除；只有极少数怪物、affix 或特殊 encounter 才允许显式声明负 `holyResistance`。

### P0-2 `P3-W5` 的进阶职业正式解锁依赖 `P3-W6` 的通关终局

**位置**

1. [PR-05 L22-L27](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L22)
2. [PR-05 L43-L47](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L43)
3. [PR-05 L260-L285](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L260)
4. [PR-06 L47-L50](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L47)
5. [深战斗总览 L275-L275](../phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md#L275)

**问题**

`Berserker / Spellblade` 的正式解锁被绑定到基础职业“通关”，而“通关”又被定义为击败 `abyssal_heart` 并完成结算。于是 `W5` 想验证“两个进阶职业进入可玩路径”，却被 `W6` 的最终终局结构反向卡住。

**核实结果**

这不是 review 的主观放大，而是现有文档链路的真实依赖断裂。

**影响**

1. `W5` 的完成定义无法独立自证。
2. `SoloClearLab` 对进阶职业的验证会被正式解锁流程阻塞。
3. 实施时一定会引入开发态 bypass，但现在文档没有把 bypass 变成正式合同。

**建议**

把“正式玩家解锁”和“开发/实验室可用性”拆开，最小合同建议固定为：

```kotlin
enum class UnlockState {
    LOCKED,
    DEV_UNLOCKED,
    RELEASE_UNLOCKED,
}
```

`W5` 验证的是 runtime/playability ready；正式 release unlock 的接线可以放到 `W6`。

### P0-3 Spellblade / Berserker 的资源合同仍然互相冲突

**位置**

1. [PR-05 L53-L54](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L53)
2. [PR-05 L123-L131](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L123)
3. [PR-05 L145-L151](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L145)
4. [核心补充文档 L1269-L1269](../2026-03-13-core-systems-design-and-phase-supplements.md#L1269)
5. [核心补充文档 L1958-L1959](../2026-03-13-core-systems-design-and-phase-supplements.md#L1958)

**问题**

1. `PR-05` 把 `Spellblade` 定义成 `MANA + EQUILIBRIUM` 双轴。
2. 核心补充文档仍把 `Spellblade` 写成 `EQUILIBRIUM` 单资源。
3. `ProfessionDef` 仍是 `resourceType: ResourceType` 单字段，装不下双资源职业。
4. `Berserker` 在 `PR-05` 被写成“受伤 + 命中积累 HATE，并有失控风险”，核心补充文档却仍是 `OnHit + OnKill + DecayPerTurn`。

**核实结果**

这条结论成立，而且属于 `core` 数据模型级冲突，不是职业内容细节冲突。

**影响**

继续往前写代码，只会把职业差异变成私有特判，污染职业资源层。

**建议**

先把职业资源合同升级为多轴模型，再冻结具体职业：

```kotlin
data class ProfessionDef(
    val id: String,
    val resourceProfiles: List<ResourceProfileRef>,
    val primarySpendAxis: ResourceAxis?,
    val stateAxis: ResourceAxis?,
)
```

然后再分别确定 `Spellblade` 的双轴语义和 `Berserker` 的积累/衰减/失控规则。

### P0-4 动态说明把最终本地化文本下沉到了 `core`

**位置**

1. [PR-03 L107-L118](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L107)
2. [PR-03 L256-L257](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L256)
3. [路线图 L359-L359](../2026-03-13-phase2-to-phase5-final-roadmap.md#L359)

**问题**

`PR-03` 把动态说明 pipeline 定义成 `schema -> keyword resolution -> value interpolation -> localized output`，并且核心实现文件落在 `core/talent/*`。这等于要求 `core` 直接产出最终本地化字符串和 tooltip 文案。

**核实结果**

这条判断成立，而且直接撞到了项目已经固定的模块边界。

**影响**

1. `core` 会被迫理解 locale、格式化、tooltip 展示语义。
2. 后续 talent、Boss、铭文、affix 的说明都会反向污染规则层。
3. 双语、字体、平台 UI 差异会变成 `core` 的长期负担。

**建议**

把 `PR-03` 的输出改成语义模型，而不是最终字符串：

```kotlin
data class DescriptionModel(
    val templateKey: String,
    val placeholders: Map<String, String>,
    val keywords: List<KeywordSemanticRef>,
)
```

`core` 只负责解析和数值注入；最终的 localized string 与 tooltip 拼装留在 `client` 或专门的 presenter 层。

## 3. P1 问题

### P1-1 `P3-W1` 的 golden 重录策略过早，而且版本粒度过粗

**位置**

1. [PR-01 L24-L27](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L24)
2. [PR-01 L168-L172](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L168)
3. [验证清单 L13-L13](../phase3/2026-03-13-phase3-verification-checklist.md#L13)

**问题**

`W1` 之后 `W2` 会改状态结算，`W3` 会改 talent schema，`W4` 会加 Boss trace，`W5` 才接入正式职业树。现在就要求“全量重录 Phase 2 golden 基线”，会导致 `Phase 3` 中段持续作废。

**建议**

1. `W1` 只冻结 formula corpus。
2. `W2` 冻结 status corpus。
3. `W5` 后再冻结 integration corpus。
4. `W6` 单独维护 long-run corpus。
5. 所有 golden 至少携带 `phaseId / rulesetVersion / traceSchemaVersion / corpusId`。

### P1-2 `CombatTrace` 冻结过早，而且正在被塞成“万能追踪对象”

**位置**

1. [PR-01 L144-L172](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L144)
2. [PR-02 L153-L155](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L153)
3. [PR-04 L229-L234](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L229)

**问题**

`W1` 想冻结 `CombatTrace`；`W2` 又要把 tick / 净化写进去；`W4` 还要对齐 `BossTrace`。这说明文档没有区分“战斗结算追踪”和“遭遇级追踪”。

**建议**

两条路任选一条，但要现在定：

1. 拆成 `CombatResolutionTrace` + `EncounterTrace`
2. 保留单入口，但固定为 `TraceEnvelope + typed streams`

### P1-3 `OVERCHARGE` 在 `PR-02` 被写成了 `buff`

**位置**

1. [PR-02 L99-L105](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L99)
2. [核心补充文档 L1387-L1397](../2026-03-13-core-systems-design-and-phase-supplements.md#L1387)
3. [核心补充文档 L1840-L1840](../2026-03-13-core-systems-design-and-phase-supplements.md#L1840)

**问题**

`OVERCHARGE` 的语义是“目标下次受到的闪电伤害 +25%”，这是标准负面状态；`PR-02` 却把它归类成了 `buff`。

**建议**

立刻改成 `debuff`，并同时明确：

1. 挂在谁身上
2. 能否被敌方驱散
3. 玩家能否主动净化自己身上的 `OVERCHARGE`

### P1-4 `PR-02` 过度吸纳了 world-scoped effect，但没有 effect carrier 抽象

**位置**

1. [PR-02 L31-L31](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L31)
2. [PR-02 L118-L124](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L118)
3. [Phase 3 总览 L148-L149](../phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md#L148)

**问题**

actor 身上的 `ActiveEffect`、地图上的毒云/火地、Boss arena aura 并不是同一种宿主对象。现在文档把 `sustain / mark / ward / zone effect` 都塞到状态生命周期里，却没有 carrier 抽象。

**建议**

增加效果宿主分层：

1. `ActorEffect`
2. `AreaEffectEmitter`
3. `WorldEffect`

共享 tick / expire / trace 机制，但分开持有者、净化规则和存档语义。

### P1-5 `WAR_CRY_BUFF` 被重新塞回专用状态矩阵

**位置**

1. [PR-02 L49-L49](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L49)
2. [PR-02 L123-L124](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L123)
3. [核心补充文档 L1856-L1856](../2026-03-13-core-systems-design-and-phase-supplements.md#L1856)

**问题**

核心补充文档已经要求把 `WAR_CRY_BUFF / WAR_CRY_DEBUFF` 迁移到通用 effect；`PR-02` 却仍围绕它定义唯一效果规则，等于把 ability-specific 名字特判重新塞回状态系统。

**建议**

不要再保留名字级特判，改成通用字段：

1. `uniquenessKey`
2. `exclusiveGroup`
3. `sourceScopedUnique`
4. `replacePolicy`

### P1-6 状态“进入系统的方式”没有被冻结

**位置**

1. [PR-01 L127-L141](../phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md#L127)
2. [PR-02 L95-L105](../phase3/2026-03-13-phase3-pr-02-status-lifecycle.md#L95)
3. [核心补充文档 L946-L1044](../2026-03-13-core-systems-design-and-phase-supplements.md#L946)
4. [核心补充文档 L2805-L2805](../2026-03-13-core-systems-design-and-phase-supplements.md#L2805)

**问题**

文档冻结了 `Power/Save` 和 `saveDimension`，却没有冻结 effect 的 application mode。后续每个技能都能自己决定是“先命中再豁免”还是“只走豁免”。

**建议**

增加 `ApplicationPolicy` 或 `EffectResolutionMode`，至少覆盖：

1. `SELF_AUTO`
2. `HOSTILE_HIT_THEN_SAVE`
3. `HOSTILE_SAVE_ONLY`
4. `TAG_AUTO`
5. `INSTANT_ACTION`

### P1-7 `PR-03` 把 prerequisite 收缩成“至少 1 点”的弱版本

**位置**

1. [PR-03 L74-L79](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L74)
2. [核心补充文档 L2230-L2233](../2026-03-13-core-systems-design-and-phase-supplements.md#L2230)
3. [核心补充文档 L2373-L2381](../2026-03-13-core-systems-design-and-phase-supplements.md#L2373)

**问题**

`PR-03` 把前置条件定义成 `talentId` 列表且默认“至少 1 点”；核心补充文档已经存在 `power_strike:2` 这种 rank 化 prerequisite。

**建议**

统一成结构化类型：

```kotlin
data class TalentPrerequisite(
    val talentId: String,
    val minRank: Int,
)
```

### P1-8 Telegraph 权威被分成两份，而且阈值的参考防御对象未定义

**位置**

1. [PR-03 L74-L80](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L74)
2. [PR-04 L107-L114](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L107)
3. [核心补充文档 L2346-L2349](../2026-03-13-core-systems-design-and-phase-supplements.md#L2346)

**问题**

1. `talent.telegraph` 和 `BossEncounter.onEnter.TELEGRAPH` 同时在写 telegraph。
2. `>=30% / >=50% 玩家最大 HP` 没有定义参考的是哪一种 defender baseline。

**建议**

1. 只保留一个 `TelegraphSpec` 权威结构。
2. Boss phase 只负责“何时触发”，能力本身负责“触发后长什么样”。
3. 增加 `ThreatRatingResolver`，明确 telegraph 评估所用的 defender profile。

### P1-9 `AIProfile` 同时存在 `priority` 语义和 `weight` 语义，但没有统一抽象

**位置**

1. [PR-04 L27-L30](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L27)
2. [PR-04 L50-L50](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L50)
3. [PR-04 L87-L91](../phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md#L87)

**问题**

普通怪继续沿用 `priority`，Boss 又走 `weight`，但 schema 本身没有告诉运行时“这套 profile 用什么 selection policy”。

**建议**

显式加：

1. `selectionPolicy`
2. `orderKey`
3. `weight`
4. `rngRoll` 写入 trace

### P1-10 `respec / rollback` 仍然是 live runtime 改状态，不是 draft transaction

**位置**

1. [PR-03 L178-L212](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L178)
2. [PR-03 L219-L249](../phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md#L219)

**问题**

`respec` 依赖 `on_unlearn` 和状态清除，`rollback` 又明确“不回退已触发的即时效果”。这说明点数分配已经直接改动了真实世界状态，而不是先在 draft 上演算。

**建议**

改成 draft transaction：

1. `TalentAllocationDraft`
2. preview / rollback 只改 draft
3. confirm 才提交到运行时

### P1-11 `P3-W5` 的拆分已经违反工作包设计规则

**位置**

1. [路线图 L173-L196](../2026-03-13-phase2-to-phase5-final-roadmap.md#L173)
2. [PR-05 L52-L59](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L52)
3. [PR-05 L300-L351](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L300)

**问题**

`W5` 同时拉入职业树、双资源、种族、铭文、Profile、UI、SoloClearLab，触碰 `core / game / client / tools` 四层，已经不是一个 PR，而是一段小 phase。

**建议**

把 `W5` 拆成：

1. `W5a Rules/Core`
2. `W5b Content/Game`
3. `W5c Client+QA`

### P1-12 进阶职业树结构本身也存在跨文档漂移

**位置**

1. [PR-05 L123-L125](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L123)
2. [PR-05 L145-L151](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L145)
3. [核心补充文档 L2166-L2173](../2026-03-13-core-systems-design-and-phase-supplements.md#L2166)

**问题**

核心补充文档写的是“进阶职业 4 棵树（4x4=16 talent）”；`PR-05` 实际给 `Berserker / Spellblade` 冻结的是 3 支线方向。内容预算和职业身份边界已经漂移。

**建议**

二选一，但必须统一：

1. 明确进阶职业在 `Phase 3` 只做 3 树轻量版，并同步改权威文档
2. 保留 4 树正式口径，但把 `W5` 的最小可玩范围降成 2 新树 + 2 继承树

### P1-13 `PR-06` 漏掉了长局最关键的 `WorldProgress / Quest / Gate` 合同

**位置**

1. [路线图 L361-L361](../2026-03-13-phase2-to-phase5-final-roadmap.md#L361)
2. [Phase 3 总览 L430-L432](../phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md#L430)
3. [PR-06 L57-L64](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L57)

**问题**

路线图写的是“世界分支、zone 入口、主支线任务、affix v1、经济循环、长局回归”；`PR-06` 实际只冻结了拓扑、zone、Boss、经济、affix、lab，没有 `questId / worldFlag / gateCondition / objectiveState`。

**建议**

至少补最小结构：

1. `WorldProgressDef`
2. `QuestDef`
3. `GateCondition`
4. `RouteReward`

### P1-14 `PR-06` 的经济和 affix 只冻结“数量”，没有冻结“救火能力”和“build 相关性”

**位置**

1. [PR-06 L188-L197](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L188)
2. [PR-06 L210-L223](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L210)
3. [PR-06 L343-L350](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L343)

**问题**

现在只有 `1 currency + 2 shops + 40 affixes` 的数量约束，但没有约束：

1. 商店保底库存
2. 位移/净化/护盾等 rescue 工具的稳定来源
3. affix 与职业、伤害通道、资源轴之间的相关性

**建议**

增加：

1. `RescueInventoryPolicy`
2. `AffixTagWeighting`
3. `AffixBlacklist`

### P1-15 `RunSummary` 太瘦，而且与复现合同脱节

**位置**

1. [PR-05 L266-L281](../phase3/2026-03-13-phase3-pr-05-class-formalization.md#L266)
2. [PR-06 L237-L252](../phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md#L237)
3. [验证清单 §4 Reproducibility Contract](../phase3/2026-03-13-phase3-verification-checklist.md#4-reproducibility-contract)

**问题**

验证清单要求 golden / harness 记录 `build id / phase id / seed / 职业 / 种族 / profile / zone route`；而 `RunSummary` 只记了 `finalZoneId` 和胜负，无法为 `longRunLab`、玩家 run history 和回归定位提供足够上下文。

**建议**

最小增强字段建议：

1. `turnCount`
2. `zoneRouteHash`
3. `buildHash`
4. `rulesetVersion` 或 `phaseId`
5. `defeatReason`（可选）

## 4. P2 与微调项

这些问题不一定会立刻阻塞实现，但如果不收口，会持续制造返工和口径漂移：

1. `PR-03` 写“只依赖 `P3-W1`，与 `W2` 可并行”，但路线图又写 `W3` 依赖 `W2`。要么拆 `W3a / W3b`，要么修正文案。
2. 核心补充文档仍保留 [L3092](../2026-03-13-core-systems-design-and-phase-supplements.md#L3092) “`grey_gate_depths` 是最终 Boss 战”的旧表述，必须和 `abyssal_heart` 终局同步。
3. 核心补充文档的 talent YAML 示例仍保留 `description` / `breakpointDescription` 字段，而 `PR-03` 已禁止第二套文本逻辑，需要删掉或改成语义占位字段。
4. `dangerLevel` 枚举漂移：核心补充文档是 `MODERATE`，`PR-04` 用的是 `MEDIUM`，必须统一。
5. `bossHarness` 覆盖数量漂移：`PR-04` / 验证清单写“至少 2 个 Boss”，`PR-06` 出口门禁又写成“3 Boss 通过”，要收口。
6. 第二个商店节点仍写成“`deep_iron_pit` 或 `underground_river`”，这会让 `longRunLab` baseline 漂移，必须固定成单一位置。
7. 路线图 `Phase 3` 最小可发布集写的是“世界 Zone 8 个左右”，`PR-06` 一次性冻结 11 个 zone。要么明确这是 scope expansion，要么把出口门禁改回 critical-path 8-zone。
8. `headless run 等价回合数 <= 3000` 还没有定义换算方式，当前门禁不可稳定复现。
9. `Shadowblade / Warden` 需要明确 `locked / unlocked-but-unavailable / playable` 三态 UI；否则角色创建页和 Profile 页会产生误导。
10. 需要补“起始保底铭文 / 保命工具来源”合同，不然 `W5 / W6` 初期很容易被随机掉落拖垮。
11. `longRunLab` 现在只对 4 基础职业给量化门槛，至少应给 2 个可玩进阶职业加 smoke 级门槛，否则长局验证仍然偏空。
12. `ProfileData / RunSummary` schema 现在在核心补充文档、`PR-05`、`PR-06` 各自复制一份，属于明显的漂移源，应该改成单一权威 + 交叉引用。
13. `phase3/roadmap.md` 已声明权威层级，但没有说明“当 PR 文档与上游权威冲突时，必须先修上游再修 PR”，执行顺序仍然容易被误解成就地 patch 下游。

## 5. 统一修订基线

这一节不是要新增第二套文档，而是给出“应该写回上游权威文档”的统一基线。只要这一层不收口，后面的 PR 文档修订都会反复漂移。

### 5.1 战斗、Trace 与效果入口基线

应固定为：

1. `HOLY` 默认只走标签增伤路径；负神圣抗性不是亡灵/恶魔的通用基线。
2. golden 必须分层，不再把 `Phase 3` 当成一个单层 corpus。
3. `CombatTrace` 不直接承担全部 encounter 追踪语义。
4. effect 的进入路径必须有结构化 `ApplicationPolicy`。

建议最小结构：

```kotlin
enum class GoldenCorpus {
    FORMULA,
    STATUS,
    INTEGRATION,
    LONG_RUN,
}

data class TraceEnvelope(
    val phaseId: String,
    val rulesetVersion: String,
    val traceSchemaVersion: String,
    val corpus: GoldenCorpus,
)

enum class ApplicationPolicy {
    SELF_AUTO,
    HOSTILE_HIT_THEN_SAVE,
    HOSTILE_SAVE_ONLY,
    TAG_AUTO,
    INSTANT_ACTION,
}
```

### 5.2 职业、天赋与说明基线

应固定为：

1. 职业资源用多轴模型表达，不再用单 `resourceType`。
2. 进阶职业可用性拆成 `LOCKED / DEV_UNLOCKED / RELEASE_UNLOCKED`。
3. prerequisite 必须支持 `minRank`。
4. 天赋分配必须有 draft transaction。
5. `core` 只产出说明语义，不产出最终本地化字符串。

建议最小结构：

```kotlin
data class TalentPrerequisite(
    val talentId: String,
    val minRank: Int,
)

data class TalentAllocationDraft(
    val professionId: String,
    val pendingRanks: Map<String, Int>,
)

data class DescriptionModel(
    val templateKey: String,
    val placeholders: Map<String, String>,
    val keywords: List<String>,
)
```

### 5.3 AI、Telegraph 与效果宿主基线

应固定为：

1. `TelegraphSpec` 是唯一 telegraph 权威。
2. Boss phase 只声明 phase trigger 和 on-enter side effects，不重复定义能力级 telegraph。
3. AI profile 需要显式 `selectionPolicy`。
4. actor status、地图 hazard、world aura 必须分 carrier。

建议最小结构：

```kotlin
enum class AISelectionPolicy {
    DETERMINISTIC_PRIORITY,
    WEIGHTED_RANDOM,
}

data class TelegraphSpec(
    val shape: TelegraphShape,
    val previewTurns: Int,
    val dangerLevel: DangerLevel,
    val threatProfileId: String,
)
```

### 5.4 长局世界、经济与回归基线

应固定为：

1. `W6` 不只是 world graph，还必须有世界推进合同。
2. 商店、铭文、affix 需要 rescue policy，而不是只有数量预算。
3. `RunSummary` 至少要支撑回归分析、玩家历史和故障定位。

建议最小结构：

```kotlin
data class WorldProgressDef(
    val questStates: Map<String, ObjectiveState>,
    val worldFlags: Set<String>,
    val unlockedRoutes: Set<String>,
)

data class GateCondition(
    val requiredQuestId: String? = null,
    val requiredWorldFlag: String? = null,
)

data class RunSummary(
    val seed: Long,
    val turnCount: Int,
    val finalZoneId: String,
    val zoneRouteHash: String,
    val buildHash: String,
    val victory: Boolean,
    val rulesetVersion: String,
)
```

## 6. 按文档落地的修改清单

### 6.1 必须先改的上游权威文档

#### `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

1. 删除亡灵/恶魔默认 `holyResistance = -25` 的通用基线。
2. 把 `OVERCHARGE` 明确修成 `debuff`。
3. 统一 `dangerLevel` 枚举命名。
4. 把进阶职业资源、树结构、`RunSummary`、world progress、telegraph、effect carrier 的正式口径补齐。
5. 删除或改写 talent 示例中的 `description` / `breakpointDescription` 第二套文本字段。

#### `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`

1. 明确 `W3` 与 `W2` 的依赖口径，必要时拆 `W3a / W3b`。
2. 把 `W5` 拆成 `W5a / W5b / W5c`。
3. 把 `W6` 的验收口径补成“world graph + quest/gate + economy/affix + lab”。
4. 收口 `8-zone` 与 `11-zone` 的范围差异。

#### `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`

1. 作为 `Phase 3` 总览，需要同步吸收本稿的统一修订基线。
2. 不能继续保留 `deep_iron_pit or underground_river` 这类运行时漂移点。
3. 需要把 `W5 / W6` 的拆分和开发态可用性写清楚。

#### `docs/phase3/2026-03-13-phase3-verification-checklist.md`

1. 把 golden 改成分层验证，而不是单层全量重录。
2. 定义 `headless run 等价回合数` 的换算规则。
3. 收口 `bossHarness` 覆盖数。
4. 给 2 个可玩进阶职业补 smoke 级门槛。
5. 让复现合同和 `RunSummary` 字段对齐。

#### `docs/phase3/roadmap.md`

1. 在“权威层级”后补一句执行规则：下游 PR 文档与上游冲突时，必须先修上游权威。
2. 依赖拓扑要么拆 `W3 / W5 / W6` 子包，要么在拓扑图里标出并行 lane 的前置条件。

### 6.2 逐 PR 文档修改建议

#### `PR-01`

1. 移除 `HOLY` 的双口径空间。
2. 把 golden 改成分层 + 版本化，而不是只写 `phase: P3`。
3. 补 `ApplicationPolicy` 或至少引用其权威定义。
4. 不要把 `CombatTrace` 冻成 encounter 级万能对象。

#### `PR-02`

1. 把 `OVERCHARGE` 改成 `debuff`。
2. 不再保留 `WAR_CRY_BUFF` 名字级特判。
3. 把 `zone effect` 从 actor status 体系里拆出 carrier 概念。
4. 说明哪些 effect 可以净化，哪些只能 expire。

#### `PR-03`

1. 把动态说明输出改成语义模型。
2. prerequisite 改成 `talentId + minRank`。
3. `respec / rollback` 改成 draft transaction。
4. `telegraphDef` 只保留引用 `TelegraphSpec` 的能力级入口。

#### `PR-04`

1. 明确 `selectionPolicy`，收口 `priority / weight` 双语义。
2. telegraph 只引用单一 `TelegraphSpec`。
3. 给 telegraph 阈值增加 `ThreatRatingResolver` 或 defender baseline。
4. `dangerLevel` 与上游统一。

#### `PR-05`

1. 显式拆成 `W5a / W5b / W5c`。
2. 用 `UnlockState` 区分开发态可用与正式解锁。
3. 把 `ProfessionDef` 升级到多资源轴。
4. 收口进阶职业是 3 树轻量版还是 4 树正式版。
5. 不再在多个文档重复维护 `ProfileData / RunSummary` schema。

#### `PR-06`

1. 补 `WorldProgressDef / QuestDef / GateCondition / RouteReward`。
2. 固定第二商店节点位置。
3. 给经济与 affix 加 rescue / build relevance 合同。
4. `RunSummary` 至少补 `turnCount / zoneRouteHash / buildHash / rulesetVersion`。
5. 长局门槛要覆盖 2 个可玩进阶职业的 smoke。

## 7. 最值得现在补的结构件

如果只允许补少量结构件，我认为最值回票价的是下面四个：

### 7.1 `AbilityPreview / DescriptionModel`

一套语义模型同时服务：

1. 动态说明
2. telegraph 阈值估算
3. tooltip
4. QA / harness 预期值展示

这样不会再出现“战斗一套公式、UI 一套估算、Boss telegraph 再一套估算”。

### 7.2 `PersistentEffect` 抽象族

不要继续把 actor status、zone hazard、Boss aura 混成一种对象。最小建议：

1. `ActorEffect`
2. `AreaEffectEmitter`
3. `WorldEffect`

### 7.3 分层版本化 golden

最小也要拆成：

1. formula corpus
2. status corpus
3. integration corpus
4. long-run corpus

### 7.4 `WorldProgress` 子系统

`W6` 如果没有 `Quest / Gate / WorldFlag / RouteReward`，就只是“多了几个 zone”，还不算真正的长局主线结构。

## 8. 推荐修订顺序

推荐按下面顺序修，不要先修 PR 文档、再让上游权威把它们推翻：

1. 先收口上游权威：`core-systems-design-and-phase-supplements`、`final-roadmap`、`phase3 deep-combat`、`verification-checklist`、`phase3/roadmap`。
2. 再修 `W1 / W2` 的基础合同：`HOLY`、golden 分层、`ApplicationPolicy`、effect carrier、`OVERCHARGE`、状态唯一性规则。
3. 然后修 `W3 / W4` 的 schema：`DescriptionModel`、structured prerequisite、draft allocation、`TelegraphSpec`、`selectionPolicy`。
4. 再修 `W5`：拆包、`UnlockState`、多资源轴、进阶职业树口径。
5. 最后修 `W6`：`WorldProgress / Quest / Gate`、经济 rescue policy、固定商店节点、`RunSummary`、long-run 指标。
6. 所有文档修完后，再做一轮 micro drift 清扫：`dangerLevel`、Boss 数量、zone 数量、`grey_gate_depths` 旧描述、重复 schema 片段。

## 9. 最终判断

如果只看 `Phase 3` 总览和 checklist，这套文档体系已经很完整；但下钻到 `PR-01 ~ PR-06` 后，确实还能看到几处会在实施期放大成返工的结构性问题。

因此我的最终判断保持不变，但表述更明确：

1. 这套文档**值得继续沿用**，不需要推翻重写。
2. 这套文档**还不能直接进入稳定实施**，必须先做一轮合同收口。
3. 最小正确路径不是“再加更多内容”，而是“先统一权威，再修 PR 级入口”。

只要先修掉本稿里的 `P0` 和关键 `P1`，`Phase 3` 的实施成本会明显下降，后续也更不容易在 `W1 / W2` 的 golden、`W3 / W4` 的 schema、`W5 / W6` 的验收门禁上反复返工。
