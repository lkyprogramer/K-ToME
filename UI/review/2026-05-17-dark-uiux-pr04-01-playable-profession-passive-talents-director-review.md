# Dark UI/UX PR-04-01 Playable Profession Passive Talents 深度 Review

审查日期：2026-05-17

目标文档：`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`

审查角色：Roguelike / ToME-like 玩法设计总监、系统策划总监、玩法体验审查负责人

## 结论

方向成立，但当前文档还不能直接作为实现合同进入开发。它抓住了 PR04 Talent Assign 面板暴露出的真实问题：职业树存在 `PASSIVE` 表现能力，但 6 个可玩职业缺少足够清晰的被动构筑密度，导致主动槽压力、长期成长身份和右侧详情说明都不完整。

阻断点主要不在“是否应该做被动”，而在合同切分和权威边界：

1. `PassiveEffect` 新模型没有覆盖现有装备被动完整能力族，却要求装备被动立即映射到新模型，存在破坏现有装备 passive 或保留第二真源的风险。
2. 文档继承 `UI/pr/development-governance.md`，但该治理合同本身以 UI / resource / client gate 为主；本 PR 实际改 `core/game/schema/runtime/content/tools`，owner 与 gate surface 不一致。
3. UI 右侧详情要求“当前 rank / 下一 rank 被动收益”，但没有先定义 game/core 到 snapshot / description model 的 typed presentation boundary，容易逼迫 client 解析 passive 规则或 locale 文案。
4. 单个 XL PR 同时做核心抽象、schema、resolver、StatsCalculator、12 个天赋转换、client detail 和 lint，超过仓库工作包规则的可控范围。

建议先修 PR04-01 文档，再进入实现。最小可行收敛是把它拆成 `contract/equipment parity -> Task A static passives -> Task B trigger/conditional passives + coverage lint` 三段，或至少在文档中明确为什么不能拆，并给每段独立验收 gate。

## Findings

### [P1] `PassiveEffect` 合同不是现有装备被动的行为保真超集

目标文档定义的新 `PassiveEffect` 只包含 `StatModifier`、`ConditionalStatBonus`、`DamageVsStatus`、`DamageTypeBonus`、`ResistanceBonus`、`OnKillResourceRestore`、`HpRegenPerTurn`，并要求 `EquipmentPassive` 只能作为 schema compatibility surface，必须立即映射到 `PassiveEffect`（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:127`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:176`）。

但当前代码中的 `EquipmentPassive` 还包括 `OnHitStatusProc`、`TerrainAffinityBonus`、`DamageVsTag`，并且 resolver、combat callback、frontstage cue 都围绕 `EquippedPassiveSource` 和 item source identity 运转（`core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:40`, `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:78`, `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:83`, `core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt:10`, `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:11745`）。

影响：实现者如果严格按文档替换 resolver，会面临两个坏选择：删掉或降级现有装备 passive 行为，或者把旧 `EquipmentPassive` 留作真实 resolver authority。前者是行为回归，后者违反文档禁止第二真源的目标。

必须修正文档：

1. 把新 `PassiveEffect` 定义成当前 `EquipmentPassive` 的完整超集，至少补齐 `OnHitStatusProc`、`TerrainAffinityBonus`、`DamageVsTag`。
2. `PassiveSource` 不能只保留 `sourceId/affixId/talentRank`，还要覆盖现有 trace 需要的 `sourceSpecialTemplateId` 或等价 typed identity。
3. 保留旧 `EquipmentPassive` 的过渡期可以接受，但必须写清：旧 schema 只在 loader boundary 转换，runtime resolver 只接受统一 `PassiveSource`；若分阶段迁移，必须有到期任务和 parity tests。

### [P1] UI/pr 治理合同与 PR04-01 的 gameplay/schema 改造不一致

PR04-01 声明继承 `development-governance.md`，并在 Acceptance Matrix 中使用 `core / game / client / tools / docs` owner（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:39`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:43`）。但 `development-governance.md` 把 owner 固定为 `client / assets / tools / docs`，且说明 PR-01 到 PR-04 以 client presentation 和 golden 为主，不改变 gameplay rule（`UI/pr/development-governance.md:20`, `UI/pr/development-governance.md:26`, `UI/pr/development-governance.md:107`）。

PR04-01 实际触碰的是 `TalentLevelEffect`、talent YAML schema、runtime resolver、StatsCalculator、FoundationGameSession、contract lint 和 12 个官方天赋数据转换。这不是普通 dark UI PR 的 gate surface。

影响：`acceptanceContractLint` 如果按现有治理合同执行，可能直接判 owner 非法；如果为了 PR04-01 放宽，又会隐式修改整个 `UI/pr` 系列的治理口径，形成第二套未声明规则。

必须修正文档：

1. 明确 PR04-01 是 `UI/pr` 下的 gameplay/content bridge 特例，还是应迁回 Phase4 v4 gameplay 文档链。
2. 若保留在 `UI/pr`，必须同步更新 `UI/pr/development-governance.md` 和对应 lint，使 `core/game` owner 以及 gameplay gate 成为显式合法 subtype。
3. Gate ladder 不能只借用 UI PR 口径，必须把 schema/runtime owner gate 放在和 client golden 同等的 blocking 位置。

### [P1] 被动详情缺少 typed presentation boundary，client 很容易被迫计算规则

文档要求 PASSIVE current detail 和 next preview 显示精确收益与增量，且 description text 不能成为唯一数值来源（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:246`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:253`）。PR04 上游合同要求 current/next detail 来自 typed snapshot/schema/description model，renderer 和 presenter 不能硬编码数值（`UI/pr/dark-uiux-pr04-profession-tree-ui.md:42`, `UI/pr/dark-uiux-pr04-profession-tree-ui.md:644`, `UI/pr/dark-uiux-pr04-profession-tree-ui.md:653`）。

当前实现里，`DescriptionModel` 只保存 `templateKey/placeholders/keywords`，`DynamicDescriptionResolver` 只从 active `TalentLevelEffect` 字段和 `EffectOp` 生成 placeholder（`core/src/main/kotlin/com/ktome/core/talent/DescriptionModel.kt:15`, `core/src/main/kotlin/com/ktome/core/talent/DescriptionModel.kt:48`, `core/src/main/kotlin/com/ktome/core/talent/DescriptionModel.kt:112`）。`TalentSidebarPresenter` 当前 rank 和 next rank 也只识别 active damage/range/status/heal/resource restore 等 placeholder（`client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:761`, `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:824`）。

影响：如果不先定义 passive presentation DTO，client 只能解析 `PassiveEffect` 类型、talent id、locale 文案或临时 placeholder。前两者把规则模型泄漏进 client，后两者会让数值说明变成第二真源。

必须修正文档：

1. 在 Task 1 或 Task 2 中新增明确的 `PassiveEffectPresentationLine` / `DescriptionModel.passiveLines` / `TalentPassiveDetailSnapshot` 之一，且由 `game` 从 `TalentDef.levelEffects[N].passiveEffects` 生成。
2. current rank 与 next rank delta 的差分必须在 game/core presentation boundary 完成，client 只渲染 `labelKey/value/token/tone`。
3. `DescriptionPresenterTest.passiveEffectsRenderAsKeywordLines` 应测试未知 passive kind fail fast，但 fail fast 应发生在 presentation projection 或 presenter boundary，而不是让 renderer 默默丢行。

### [P1] 工作包过大，超过 K-ToME 当前的可审查粒度

文档标记工作量 `XL`，并要求 Task B 在同 PR 内 mandatory，不允许 deferred（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:5`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:272`）。实现范围覆盖 `core`、`game`、`client`、`tools`、i18n、12 个 talent 数据转换、schema、runtime resolver、save/load、contract lint、golden/client evidence（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:299`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:406`）。

仓库级规则要求每个工作包只引入一个核心抽象族，同时触碰的生产模块不超过两个；超过时必须说明为何无法再拆，且自动化验证和白盒步骤同步交付（`AGENTS.md:146`, `AGENTS.md:150`, `AGENTS.md:152`）。

影响：这个 PR 同时改变模型、数据、战斗回调、表现、lint 和职业体验。任何一个环节失败都会让 rollback 进入混合状态，例如 schema 已加、lint 已加、但 talent data 回退，或 resolver 迁移一半仍保留 item-only trace。

必须修正文档：

1. 推荐拆分为：
   - `04-01a`: source-agnostic passive contract + equipment parity，暂不改官方 talent data。
   - `04-01b`: Task A 6 个静态 passive，验证 stat modifier、rank replacement、active slot suppression、current/next detail。
   - `04-01c`: Task B trigger/conditional passive + playable coverage lint。
2. 如果坚持单 PR，必须增加“不可拆理由”、每个 task 的独立 rollback invariant，以及每个阶段失败时不能进入下一阶段的 gate。

### [P2] 12 个主动技能转被动缺少职业循环与技能身份审计

文档明确不新增天赋节点、不重排树布局，而是把 12 个现有 talent 改成 PASSIVE（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:31`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:261`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:274`）。当前数据中这些 talent 多数是有 cooldown、resourceCosts、telegraphRef 的 active/self-buff 技能，例如 `mana_surge`、`deathblow`、`devotion`、`arcane_overload`、`flux_anchor` 等（`game/src/main/resources/data/talents/index.yaml:1367`, `game/src/main/resources/data/talents/index.yaml:1833`, `game/src/main/resources/data/talents/index.yaml:2071`, `game/src/main/resources/data/talents/index.yaml:2647`, `game/src/main/resources/data/talents/index.yaml:4984`）。

文档只在 Open Risks 中承认 `mana_surge` 和 `deathblow` 有主动身份风险，但没有逐职业说明转被动后保留了哪些 tactical verb、资源循环、burst window、defensive panic button 和 finisher payoff（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:523`）。

Roguelike / ToME-like 最佳实践不是简单提高 passive 数量，而是保证每个职业同时拥有：

1. 少量高决策密度主动按钮，提供 turn-by-turn agency。
2. 被动身份锚点，塑造长期 build bias。
3. 条件触发或资源回路，让 passive 影响玩家站位、击杀顺序、状态铺垫和风险管理。

当前方案有“主动槽减压”收益，但也可能削弱前 10 分钟选择密度，尤其是把 resource/burst/finisher 类 active 直接转为被动时。

必须修正文档：

1. 增加每个职业转换前后 active / passive / sustained 数量表。
2. 对 12 个 talent 增加逐项“为什么它适合转被动、转被动后职业循环由谁补位、是否影响 starter / tier pacing”的审计列。
3. 对 `mana_surge`、`deathblow`、`devotion`、`flux_anchor` 这类明显有主动身份的技能，要求单独 review 签收或替换为同树更低风险目标。

### [P2] dev playable 覆盖口径与上游 release/report-only 口径冲突

PR04-01 把 `berserker / spellblade` 标为 blocking coverage，要求每职业 2 个 passive（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:113`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:118`）。但 README、UI/PLAN 和 Phase4 v4 PR01 都把它们描述为 dev playable / report-only，而 release-facing blocking 指标只统计 `vanguard / arcanist / rogue / templar`（`README.md:35`, `UI/PLAN.md:99`, `UI/PLAN.md:100`, `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:34`）。

影响：如果 PR04-01 是有意把 dev playable 提升为本 PR blocking，应明确这是“PR04-01 passive coverage gate”而非 release-facing blocking metric。否则 coverage lint 与 Phase4 report 可能出现两个分母。

必须修正文档：

1. 将 `berserker / spellblade` 标为 `PR04-01 blocking, release report-only`，或回到上游 report-only 口径。
2. coverage artifact 同时输出 `releaseBlocking`, `pr04_01BlockingDevPlayable`, `excludedFrozen`，避免把 PR 特例扩散成全局 release truth。

### [P2] PASSIVE 的 action affordance 还没有明确从 active/reserve 行为中剥离

文档明确 PASSIVE 不进入 active slot，不打开 `ACTIVE_TALENT_SLOT_CHOICE`，也隐藏 cooldown、range、resource cost 等 active-only 行（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:231`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:246`）。但当前 PR04 presenter 的 actions block 对所有 talent 都显示 `R reserve`（`client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:733`, `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:755`）。

影响：即使 passive 不触发 slot modal，右侧动作区仍可能出现“Reserve”这类 active slot 管理语义，让玩家误以为被动也能进入 reserve loadout。这个问题属于 playable UX，而不是纯渲染细节。

必须修正文档：

1. 增加 PASSIVE action matrix：unlearned passive 只显示 Learn/Rank Up/Back；learned max rank passive 不显示 Reserve；locked passive 不显示 active-slot 行为。
2. 增加 `TalentSidebarPresenterTest.passiveActionsDoNotShowReserveOrActiveSlotManagement`。
3. 将 `R reserve` 明确限定为 `ACTIVE / SUSTAINED`。

### [P2] `SELF_HAS_STATUS=guard` 使用大小写不一致，容易混淆 status schema id 与 keyword/localized key

Task B 表中 `bulwark_march` 写的是 `ConditionalStatBonus SELF_HAS_STATUS=guard`（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:276`），而 status schema 真源使用大写 `GUARD`、`MARKED` 等 id（`game/src/main/resources/data/statuses/index.yaml:24`, `game/src/main/resources/data/statuses/index.yaml:35`）。现有 resolver 的 `PassiveStatContext.selfStatusIds` 使用 active effect 的 `schemaId`，不是 localized key 或 keyword（`core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt:21`, `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:11566`）。

影响：如果实现者按表格写小写 `guard`，条件 passive 可能永远不触发；如果 loader 做隐式 lowercase/uppercase，又会把 schema id contract 弱化。

必须修正文档：表格和 YAML 示例统一使用 canonical status id：`GUARD`、`MARKED`。展示层再通过 `statusNameKey` 或 `status.<id>` 映射本地化。

### [P2] 玩家可见被动详情不应只在“golden 改变时”才有 evidence

文档说 PR04-01 默认不需要 packaged app whitebox，只有 visual layout 或 golden output 变化时才添加 manual evidence（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:468`）。但本 PR 的核心验收之一是 Talent Assign 右侧详情能显示 PASSIVE current/next，且学习 passive 不打开 active slot modal。这是玩家可见行为变化，不只是内部 gameplay。

上游治理要求 player-visible UI 改动必须有 focused test、clientSmoke、golden 或白盒证据；`whitebox=skipped` 必须写明原因、替代证据和剩余风险（`UI/pr/development-governance.md:34`, `UI/pr/development-governance.md:36`）。

必须修正文档：

1. `:client:goldenScreenshot` 应作为默认 gate 保留，除非明确证明 detail 行不影响 golden surface。
2. packaged app whitebox 可以跳过，但 PR 描述或 manual record 必须记录 skip reason、替代证据和剩余风险。
3. 两张 evidence 图里的场景应升级为固定脚本：选中一个 Task A passive 和一个 Task B passive，各覆盖 current/next 与 active-slot suppression。

### [P2] content pack / overlay 兼容性没有落到合同

仓库稳定合同要求 content pack / overlay namespaced、可 lint、可版本校验、冲突 fail fast，且不能覆盖 core 规则语义（`AGENTS.md:140`, `AGENTS.md:141`）。PR04-01 为 talent YAML schema 增加 `passiveEffects`，并声明 old talents default empty（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:431`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:432`），但没有说明 content pack overlay 是否允许添加/替换 talent passiveEffects、unknown passive kind 如何在 pack overlay 中 fail fast、sample content pack 是否需要 fixture 更新。

影响：官方数据能通过不代表 content pack 路径安全。这个项目已经有 sample content pack，并且 README 把它列为当前可玩切片的一部分（`README.md:39`）。

必须修正文档：

1. 在 Public Interface / Contract Changes 中增加 content pack/overlay 兼容性条目。
2. 明确 `content pack` 是否允许声明 `passiveEffects`。如果允许，只能使用 core 枚举和 typed passive model，不能定义新 passive kind / damage type / resource type。
3. 增加至少一个 content-pack schema lint 或 fixture 断言，证明 unknown passive kind fail fast。

### [P3] Rollback plan 会把 lint 和数据质量绑定得过紧

Rollback plan 说如果回滚 12 个 talent 数据，且 playable passive count 低于 2，就移除新的 contract lint rule（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:494`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:496`）。

影响：这会让 lint gate 随内容回滚消失，后续仍可能回到“职业树 UI 支持 passive，但内容全部 active”的状态。更稳妥的做法是把 lint rule 分阶段启用：contract 存在但以 `expectedPassiveCoverageMode=report-only/blocking` 或 PR-specific owner profile 控制，而不是删除 rule。

建议修正文档：

1. rollback 后保留 lint report-only 模式，artifact 仍列出当前 passive count。
2. 只有 blocking threshold 可以随 rollback 关闭，rule 本身不应删除。
3. Rollback invariant 应补充：schema/resolver/content/lint 任意两者不得处于互相不认识的混合状态。

## Requirement Alignment

| 需求 | 当前判断 | 证据 | 处理建议 |
| --- | --- | --- | --- |
| 6 个可玩职业每职业至少 2 个 PASSIVE | 目标合理，但分母需修正 | PR04-01 使用 6 职业 blocking；上游 release blocking 只有 4 职业 | 区分 `releaseBlocking` 与 `pr04_01DevBlocking` |
| PASSIVE 不占主动槽 | 主方向正确，UI action 仍缺口 | runtime active slot 过滤只允许 ACTIVE/SUSTAINED；presenter actions 仍无差别显示 Reserve | 增加 passive action matrix 和 focused test |
| 装备与天赋 passive 共享 resolver | 目标正确，合同不完整 | 新 PassiveEffect 少于现有 EquipmentPassive 族 | 先做 equipment parity contract |
| current/next 被动详情 | 玩家价值高，但缺 presentation model | 当前 description model 没有 passive line/delta | 定义 typed passive detail snapshot |
| 不把被动写进 client | 文档目标正确，实现路径未封死 | presenter 当前只能读 active placeholder | 增加 projection boundary，client 只渲染 |
| contract lint 防回退 | 必要，但治理 owner 不一致 | UI governance owner 不含 core/game | 更新 governance/lint subtype |
| 不新增资源 | 合理 | PR04-01 明确不生成图片/音频 | 保留，必要时只刷新 golden/manual evidence |

## 功能 / 系统一致性矩阵

| 系统面 | 目标文档设计 | 当前项目状态 | 主要差距 | 严重级别 |
| --- | --- | --- | --- | --- |
| Passive typed model | 新 source-agnostic `PassiveEffect` 替代 equipment-only 命名 | `EquipmentPassive` 已有 9 类，resolver/source/trace 均 item-centric | 新模型不是完整超集，迁移会破坏 parity 或形成双轨 | P1 |
| Talent schema | `TalentLevelEffect.passiveEffects` | `TalentLevelEffectSchemaV2` 和 runtime `TalentLevelEffect` 只有 active fields/effectOps | 需要 schema/parser/runtime/presentation 同步 | P1 |
| Runtime stat / resolver | learned passive 从 `TalentLoadout.talentLevels` 派生 | StatsCalculator 只消费 `EquipmentPassiveStatModifier`；session 只收集 equipped passives | 需要统一 source collection 和 derived stat refresh | P1 |
| Combat triggers | on-kill / on-hit / damage bonus 保留 source kind | callbacks 和 stable keys 使用 item base id / affix id | 需要 source-aware trace/log/frontstage cue | P1 |
| Talent Assign detail | PASSIVE current/next exact lines | presenter 只识别 active placeholder 和 description text | 需要 typed passive detail projection | P1 |
| Active slot UX | PASSIVE 不打开 slot modal | runtime 已有 active/sustained 过滤，actions block 仍显示 reserve | 需要 action affordance 修正 | P2 |
| Coverage lint | 6 included + 2 frozen excluded | 上游 release/dev/frozen 口径已有三档 | 需要避免新增 release 分母冲突 | P2 |
| Whitebox/evidence | 默认不需要 packaged whitebox | 玩家可见 detail 与 modal suppression 改变 | 至少需要 focused client + golden/skip record | P2 |

## 玩法与体验审查

PR04-01 的产品动机是对的。ToME-like 职业树如果只有主动技能，玩家会把所有成长都理解成“多一个按钮或更强按钮”，结果是主动槽、冷却轮转和资源消耗都被放大，长期构筑身份反而变弱。每个职业至少两个明确被动，可以让职业身份在没有新增 UI 复杂度的情况下更稳定。

但被动不是“把 active 变成永久在线数值”。好的职业 passive 应该服务三个层次：

1. Evergreen identity：例如 Vanguard 更硬、Rogue 更准更容易暴击，降低理解成本。
2. Conditional play pattern：例如 while guarded、against marked、below HP threshold，让玩家改变站位、击杀顺序和风险选择。
3. Resource loop：例如 on kill restore mana/energy，让职业节奏从“按按钮拿资源”变成“规划击杀链拿资源”。

当前 Task A 偏 evergreen stat，Task B 偏 conditional/trigger，这个配比合理。但具体 talent 转换仍需审计：`mana_surge`、`deathblow`、`devotion`、`flux_anchor` 这类原本有主动资源/爆发/finisher 语义的技能，直接改被动可能让职业更平，但不是更深。文档应证明每个职业在转换后仍有足够 active tactical verbs，特别是前 10 分钟至少保留 2-3 个能改变回合决策的按钮。

## 当前阶段必须解决的问题

1. 先补 `PassiveEffect` parity。只要新模型不能覆盖现有装备 passive 全部行为，就不能开始 resolver 迁移。
2. 先修 governance owner。`core/game` owner 必须被 `UI/pr` 文档治理正式接受，或 PR04-01 应从 UI PR 特例迁回 gameplay 文档链。
3. 先定义 passive detail presentation boundary。没有 typed snapshot / description extension，就不能要求 client 显示 exact current/next delta。
4. 先拆分或说明不可拆。当前 XL PR 触碰面过大，不符合 K-ToME 工作包规则。
5. 先完成 12 个 talent 转换审计。不能只以“需要 2 个 passive”作为替换现有 active identity 的理由。

## Removal / Iteration Plan

### 建议迭代切分

1. `04-01a-passive-contract-parity`
   - 新增完整 `PassiveEffect` / `PassiveSource` 超集。
   - equipment schema 在 loader boundary 转统一 passive source。
   - 所有现有 equipment passive resolver tests 行为不变。
   - 不改官方 talent data。

2. `04-01b-static-profession-passives`
   - 只做 Task A 六个 static passive。
   - 打通 `TalentLevelEffect.passiveEffects`、rank replacement、StatsCalculator unified modifier、active slot suppression、current/next detail。
   - coverage lint 先 report-only 输出 passive count。

3. `04-01c-trigger-conditional-passives`
   - 做 Task B 六个 resolver-backed passive。
   - source-aware combat trace、frontstage cue、on-kill restore、DamageVsStatus、conditional status/HP context。
   - passive coverage lint 升 blocking。

### 不建议现在删除

1. 不要在第一步删除 `EquipmentPassive` schema surface；先通过 loader adapter 和 parity test 证明等价。
2. 不要删除现有 equipment frontstage log/cue 路径；先把 source identity 泛化后再替换 stable key。
3. 不要删除 active talent 原行为前的测试基线；12 个转换至少需要能证明 active slot/learn/rank/save-load 行为变化是预期变化。

### 必须补的 rollback invariant

1. `PASSIVE` talent 绝不能没有 `passiveEffects`。
2. resolver 迁移不完整时，不允许 talent passive 进入 runtime。
3. coverage lint rule 可从 blocking 降到 report-only，但不应被删除。
4. schema/loader 已支持 `passiveEffects` 时，unknown passive kind 必须仍 fail fast。

## Additional Suggestions

1. 增加职业 passive portfolio 表：每个职业至少一个 evergreen passive 和一个 conditional/trigger passive，避免全员都变成静态数值。
2. 增加 active verb budget：每个职业转换后，starter/前两层仍保留多少主动战术按钮、多少资源按钮、多少逃生/防御按钮。
3. 被动数值表建议追加 budget rationale：说明 `+5 defense`、`+12% talentPower`、`+14% vs MARKED` 在当前数值体系中对应多少战斗收益，避免 passive power creep。
4. `passiveEffects` YAML 示例应直接展示 `statusId: GUARD`、`damageType: HOLY`、`resourceType: MANA` 的 canonical enum/id，减少实现歧义。
5. UI detail 示例应增加英文/中文 token 策略：数值行由 typed line 生成，本地化只负责 label，不负责数值。
6. Open Risks 建议升级成 blocking self-audit fields，尤其是“被转换 active talent 是否仍有职业循环替代来源”。

## Suggested Verification

本次 review 未运行 Gradle 或客户端验证；以下是文档修正后实现阶段建议的验证入口。所有 Gradle 命令执行前必须先运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

建议按顺序执行，Gradle 不并行：

```bash
./gradlew acceptanceContractLint
./gradlew :core:test --tests '*PassiveEffectResolver*' --tests '*StatsCalculator*'
./gradlew :game:test --tests '*Passive*' --tests '*Talent*' --tests '*FoundationGameSession*'
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest
./gradlew contractLint localeLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

如果修改 `acceptanceContractLint`、Gradle task 接线或 bootstrap 相关文件，还必须补跑：

```bash
./scripts/verify-bootstrap.sh
```

白盒建议：

1. 若 packaged app whitebox 继续跳过，PR 描述必须写明 skip reason、替代证据和剩余风险。
2. 至少保留 `passive-detail` 和 `passive-no-active-slot-modal` 两个场景的 golden 或手工证据路径，路径必须 repo-relative。

## Summary

PR04-01 是值得做的玩法补强，但现在文档把“补齐职业 passive 密度”和“重构全局 passive authority”揉成一个 XL 变更。玩法上，它需要先证明 12 个 active 转 passive 不会削弱职业回合决策；工程上，它必须先让新 passive model 成为现有 equipment passive 的完整超集；UI 上，它必须先建立 typed passive detail projection，不能让 client 根据规则或文案推导数值。

建议先按本报告的 P1 项修正文档，再进入实现。
