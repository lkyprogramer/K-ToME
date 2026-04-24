# Phase4 v4 PR 级开发文档 深度导演审阅报告

**审阅日期**: 2026-04-24
**审阅对象**: `docs/review/phase4/v4-pr/` 下 7 份 PR 开发文档 + `README.md`
**审阅视角**: Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
**审阅口径**: 不放过任何细微问题；按 Blocker / Major / Minor / Nit 分级；每条结论附定位（文件、章节、行号或字段名）
**源引用**:
`docs/review/phase4/v4-pr/README.md`
`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`
`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md`
`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md`
`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`
`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md`
`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md`
`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md`

---

## 0 · 审阅总判

7 份 PR 的**切分方向正确**、**体验目标锁定准确**、**依赖顺序清晰**、**资源管线零新增的约束合理**。相比 `v4/phase4_profession_tree_inscription_design_supplement.md` 的补充设计，这一轮 PR 文档在"落地"侧显著前进。

但当前文档存在一类反复出现的**"口径残缺"**风险：**玩家体验目标已经定义，但实现侧的判定公式、触发边界、迁移策略、runtime wiring 不足以让一个 fresh 开发者无歧义落地**。多数 PR 中"完成定义" 的 blocking 阈值，若不补充具体计算口径，会出现**"数值通过但体验没兑现"** 或 **"体验兑现但指标卡在定义分歧上"** 两种反向失败。

**总体建议**：开发启动前，每个 PR 必须先补一份 **"阈值-事件-公式字典"（Metric Glossary）**；否则 PR-03 / PR-04 / PR-06 的多条 blocking 指标都无法稳定跑绿。

下文按 PR 分节拆分问题，并在最后给出**跨 PR 系统性问题**与**执行前必须先修的硬阻塞清单**。

---

## 1 · 通读 README 的问题

### 1.1 Blocker - "基础职业" 语义在 README 与子 PR 之间不一致

* **位置**: `README.md` 第 9~15 行表格把所有 PR 的玩家体验目标统一为"基础职业"口径；子 PR（PR-01 §5.3、PR-02 §5.1）实际列出的是 6 个职业（BASE × 4 + ADVANCED × 2：`berserker`、`spellblade`）。但 PR-01 §8、PR-03 §1 又用"四个基础职业"作为完成定义关键语。
* **事实核对**: `game/src/main/resources/data/professions/index.yaml` 通过 `tier: BASE / ADVANCED` 区分；`shadowblade / warden` 为 `frozen`。
* **风险**: `starterProfessionTalentMaxCount <= 3`、`professionCapstoneAdoptionFloor=4/4` 等指标若未对 tier 口径做声明，实现侧会在"是否把 berserker/spellblade 纳入阻塞门槛"上直接报错/漂绿。
* **必修**: README 新增一节 `1.x 职业 tier 与阻塞门槛口径`，固定为"BASE 4 职业为阻塞评估；ADVANCED 2 职业用同一指标定义，但阈值以 `report-only` 记录；`frozen` 不纳入统计但不得被 report 静默丢掉"。

### 1.2 Major - README 资源管线结论与子 PR 的 manifest 新增行为未对齐

* **位置**: `README.md` §3 "7 个 PR 均不生成新图片、不生成新音频"；但 PR-05 §5.3 新增 3 条 `telegraph` spec id，PR-04 §5.3 新增 4 种 frontstage cue key，PR-07 §5.1 新增 5 条 sample pack `ADD` entry 的 visual/audio 引用。
* **本质**: "不生成新 asset 文件" ≠ "不新增 visual/audio manifest key 条目"。当前 README 的 "不生成" 口径会让实现者在 assetLint 上撞墙（缺 manifest 映射），进而误判为"违反本轮约束"。
* **必修**: README §3 明确两件事——(a) 不新增 `assets-src/image/specs/**` 与 `assets-src/audio/specs/**` plan；(b) 允许在 `visual-manifest`、`audio-manifest`、`data/telegraph/index.yaml` 等 runtime manifest 中 **ADD** 新 key 条目指向既有 asset。

### 1.3 Major - README 缺少"回滚与并行开发守则"

* **位置**: `README.md` §2 只写了串行执行规则；但 PR 依赖链 (PR-01 → PR-02 → PR-03；PR-06 前置 PR-03+PR-04) 任一节点被否决都会导致下游卡住。
* **缺项**: 没有说明"当 PR-01 合入后发现 talent event schema 有回退需求时，已在 review 的 PR-02/PR-03 分支如何 rebase；哪些逻辑单元可以独立于 PR-01 通过 report-only 跑绿"。
* **必修**: README 新增 §5 `回滚与并行开发守则`，列出 (a) 每个 PR 的"可独立验证子集"；(b) 回退触发条件与下游分支处理；(c) 迁移 schema 并存期。

### 1.4 Minor - README §2 第 2 条"未恢复已移除的 UI/UX 完成态证据债"语义含糊

* 文档并未指出"已移除 UI/UX 完成态证据"具体指哪些 artifact（report 字段？Markdown checklist？baseline snapshot？）。若只是文本约束，执行侧会忽略。
* **建议**: 引用 `docs/phase4/2026-03-13-phase4-verification-checklist.md` 中具体 checklist 锚点，或直接列出受控字段清单。

### 1.5 Minor - README §4 验证入口未给本地快路径

* 每个 PR 的验证命令均包含 `longRunLab` 或 `reportPhase4`，本地单次耗时数十分钟。PR-06 才引入 `verifyChanged` 最小路由 —— 意味着 PR-01~PR-05 的开发期没有轻量验证入口，开发者极易跳过本地自检。
* **建议**: README 新增 "开发期快路径" 段，列出每个 PR 允许使用的 `:module:test` 子集与 smoke 目标（例如 PR-01 本地只跑 `:core:test` + `TalentAllocationPlannerTest` 即可进 review）。

---

## 2 · PR-01 Profession Tree Run Choice

### 2.1 Blocker - "四个基础职业"口径与实际 6 职业覆盖冲突

* **位置**: §1 完成标准第 1 条 / §8 第 1 条 / §5.3 起始技能表 / §6 指标 `starterProfessionTalentMaxCount`。
* **冲突**: §5.3 表中 6 职业全部被要求固定为 3 个起始技能，但 §1/§8 只说"四个基础职业"，blocking 指标又是对所有职业的 max。
* **事实核对**: 当前 `professions/index.yaml` 中 6 个可用职业的 `startingTalents` 全部为 4；若只按 BASE 4 职业 canonicalize，该阻塞指标会在 berserker/spellblade 上命中 `4 > 3` 而直接 fail。
* **必修**: §1 完成标准与 §8 完成定义全部改为"6 个可用职业"（或显式注明"ADVANCED 2 职业随同修改，但 report-only"）；明确说明 `shadowblade / warden` 是 frozen 故豁免。

### 2.2 Blocker - Tier 2 门槛 `>= 3` 级 + 同树投入 `>= 2` 过松，与 `multiTreeInvestmentRate >= 75%` 矛盾

* **位置**: §5.4 Tier 门槛表 / §6 指标 `multiTreeInvestmentRate`。
* **场景推演**: 玩家升到 3 级获得 3 点职业天赋点，若全部投入 Arms 树即满足"同树 `>= 2`" → 自动解锁 Tier 2。这就是**单树线性碾压**路径；与"10 分钟做构筑选择"的体验目标相反。
* **另一面**: `multiTreeInvestmentRate >= 75%` 要 75% 终局样本跨树投入，但 Tier 3 要求"同树 `>= 5`"。同树 5 点 / 多树分布两目标互相挤压：若玩家选 Tier 3 路线就几乎不跨树。
* **必修**:
  * Tier 2 门槛收紧为"同树投入 `>= 3`"或"当前树 `>= 2` AND 其他任一树 `>= 1`"。
  * `multiTreeInvestmentRate` 必须写明"跨树"定义（至少两棵树各 `>= 1` 点即算），并在 §6 显式说明与 Tier 3 的共存策略。

### 2.3 Blocker - `breakpointChoiceEventRate >= 75%` 事件定义不明

* **位置**: §5.5 "输出 `log.talent.breakpoint_chosen`" / §6 阻塞指标。
* **缺项**: 未定义 "chosen" 的语义——是 (a) 玩家打开了含断点预览的 draft 并确认？ (b) 玩家把 rank 升到刚好 N 后停投（触发断点解锁）？(c) 玩家主动 hover 到断点节点？
* **风险**: 三种定义会导出 3 个完全不同的指标曲线。阈值 75% 在 (a) 下轻松达到，在 (b) 下极难达到。
* **必修**: §5.5 新增"断点事件定义"子节，固定为（建议）"当 draft 确认时，draft 中至少有一次 rank 升到所预览 breakpoint 的门槛 rank 时，发出该事件"。

### 2.4 Major - 起始技能裁剪后，各树 Tier 1 的不对称性未说明

* **位置**: §5.3 起始技能表。
* **事实**: vanguard 原 `startingTalents=[power_strike, shield_bash, guard_stance, war_cry]`，裁掉 `war_cry` 后，Arms 树保留 `power_strike`，Shield 树保留 `shield_bash + guard_stance` (Shield 树吃到 2 个 freebie)，Warcry 树**没有任何起始免费技能**。
* **体验后果**: 玩家选 Warcry 路线时，第一次升级的决策是"花 1 点学 war_cry 占 Tier 1" vs "升 rank 已有技能"。其他两树路线是"升已有 rank vs 学新节点"。两类起点体验不同。
* **必修**: §5.3 下补一张"每职业 × 每树"的 Tier 1 起点分布表，显式说明每个职业是否有某棵树开局为空，并说明设计意图（是否故意惩罚 Warcry / Stealth / Warcry 类"纯学习"路径？）。

### 2.5 Major - 存档迁移策略完全缺失

* **位置**: §3.2 非目标清单未涵盖 migration；§5.1 "旧 `unlockedTalentIds` 保留一轮迁移" 是 API 层 migration，不是 save migration。
* **问题**: 已有的玩家 save 中，`TalentLoadout.talentLevels` 可能包含由 `syncUnlockedPlayerTalents` 自动写入的 rank 1 非 starter 技能。本 PR 合入后，这些技能：(a) 原样保留但不再退点？(b) 退点并恢复为 `LEARNABLE`？(c) 冻结成"legacy learned"状态？
* **风险**: 不写明策略，QA 会拿 save 测试随机出两种行为。
* **必修**: §5.5 之后新增 `5.7 存档迁移`，固定策略（建议方案 b：已 auto-learned 的非 starter 技能退回 learnable 并归还等值天赋点），并在 `FoundationGameSession.canonicalize` 时执行。

### 2.6 Major - UI 输入冲突：`1~4` 与 树 UI 的面板切换

* **位置**: §5.6 输入规则。
* **冲突**: 第 1 条 `T` 打开三列树 UI；第 6 条 `1~4` 只用于 active slot 绑定。但在三列树 UI 打开时，玩家按 `1` 含义未定义：(a) 切换到第 1 列 Arms？(b) 给当前选中主动技能绑定到 slot 1？(c) 忽略？
* **必修**: §5.6 第 6 条改为"`1~4` 仅在三列树 UI **关闭**时生效；UI 开启时 `1~4` 如需复用应绑定"列切换"（如有）或直接 no-op 并在 footer 明示"。

### 2.7 Major - 被动技能进入 `LEARNED_RESERVE` 的语义与"Preview" 缺失

* **位置**: §5.2 状态表"被动技能也进入该状态" / §5.6 Preview 示例。
* **缺项**: 被动技能学会即生效但不占槽；但 UI Preview 没展示"Learn intimidation (passive): costs 1 point; effect applies immediately" 这种备注。玩家会误以为被动也需要绑定槽位。
* **必修**: §5.6 Preview 示例补一行被动学习案例；§5.2 状态表 `LEARNED_RESERVE` 行增加 "被动技能 rank >= 1 后生效，无需绑定" 的说明。

### 2.8 Major - `syncUnlockedPlayerTalents` 下线前的消费者清单缺失

* **位置**: §5.1 "旧 `unlockedTalentIds` 保留一轮迁移，只委托 `learnableTalentIds`，调用点必须改名"。
* **缺项**: 文档没列出当前所有消费者（例如 `FoundationGameSession.syncUnlockedPlayerTalents`、tests、report runner、snapshot mapper 等）。"一轮迁移"的时间窗口是多少（下一个 PR？下一个 Phase？下个 release？）也未给出。
* **必修**: §3.1 生产代码清单下新增 "deprecated 调用点清单" 子段；§5.1 第 3 条固定"本 PR 所有调用点必须迁移完成；`unlockedTalentIds` 标 `@Deprecated(level = WARNING)` 并在 PR-02 前完成 ERROR 升级"。

### 2.9 Minor - "主动技能绑定到第一个空槽" 规则未覆盖替换

* **位置**: §5.5 确认行为第 3~4 条。
* **情形**: 玩家已绑定 `power_strike/shield_bash/guard_stance/war_cry` 占 1~4 槽，此时学会新主动技能，按规则"四槽已满时进入 reserve"。但玩家会不会直接在学习流内触发"立即替换 active 槽"交互？PR-02 给了替换 UI，PR-01 没有。两个 PR 的交互一致性差。
* **建议**: §5.5 增加"主动技能满槽时，Learn 成功后立即打开 active slot 替换 UI（参考 PR-02 铭文替换 UI 的交互语法）"，或显式说明"仅进 reserve，玩家手动从 reserve 替换"。

### 2.10 Minor - `Enter / Space` 在 talent UI 与现有 modal 的惯例冲突

* **位置**: §5.6 第 3~4 条：Enter 创建预览，Space 确认。
* **事实**: 现有 modal 多数用 Enter 确认；该 PR 改变为 "Enter 预览、Space 确认" 与玩家肌肉记忆相反。
* **建议**: 若需分离"预览 draft" 与"确认 draft"，推荐 `Enter` 切入 draft + preview，`Enter` 再次 confirm（双击 Enter），`Esc` 取消。或 preview 持续显示，`Enter` 即提交。

---

## 3 · PR-02 Inscription Shop Replacement

### 3.1 Blocker - `InscriptionEquipFailure` 枚举声明未绑定任何函数签名

* **位置**: §5.2 新增结果模型 `InscriptionEquipFailure` 及新增函数 `canReplace / replace`。
* **缺项**: `canReplace` 返回 `Boolean`；`replace` 返回 `Boolean`。枚举既未进入 `canEquip` 返回值，也未进入 `canReplace` 返回值，仅是 dangling 类型。这让上层 UI 无从区分"类别上限 vs 同铭文 vs 目标槽不存在"。
* **事实**: 当前 `core/src/main/kotlin/com/ktome/core/inscription/InscriptionManager.kt` 的 `canEquip` 返回 `Boolean`，已有相同缺陷。
* **必修**: `canEquip`/`canReplace` 统一改为返回 `Result<Unit, InscriptionEquipFailure>`（或 sealed class）。`replace` 输出 `INSCRIPTION_REPLACED` 事件，失败路径按枚举分类。

### 3.2 Blocker - 替换后冷却状态行为未定义

* **位置**: §5.2 / §5.3。
* **缺项**:
  * 旧铭文 `remainingByInscriptionId[oldId]` 是否立即清除？
  * 新铭文是否立刻可用？还是进入一次"install cooldown"（防止"满 cd 时购买替换"洗冷却）？
  * 商店内部允许的 replacement 是否会绕过天然冷却循环？
* **风险**: 如果新铭文无冷却即可用，会让"满槽 cd 墙"瞬间被绕过，经济与紧张度被破坏。
* **必修**: §5.2 新增"替换后冷却"子节，固定策略（建议）：新铭文以其 `cooldown * 0.5` 为初始冷却；旧铭文冷却条目立即删除。此规则必须落到 `InscriptionCooldownState` 操作序列。

### 3.3 Blocker - "同一铭文替换自己直接拒绝" 未覆盖铭文升级关系

* **位置**: §5.2 替换规则第 4 条。
* **问题**: `healing_light → greater_healing_light` / `phase_door → controlled_phase` / `iron_shield → diamond_shield` / `purge → greater_purge` 是现有的"升级对"（在 `shops/index.yaml` 中已存在升级 offer）。
* **风险**: "同一铭文" 若按 id 比较，升级对会通过，但如果按 "升级系列 canonical id" 比较，升级会被拒绝——行为南辕北辙。玩家在商店看到 `greater_healing_light` 时是否能替换掉 `healing_light`？体验目标层面必须允许，但代码侧无判定来源。
* **必修**: §5.2 新增 `sameInscriptionEquivalenceKey` 定义，固定为"id 相同 视为 same；升级对不 same"。并说明升级路径在 UI 侧应高亮"这是 upgrade"。

### 3.4 Major - 热键"5~8"被硬编码，与 `INSCRIPTION_HOTKEY_START` 常量解耦

* **位置**: §5.1 "热键从 5 开始连续分配"；§5.4 "`5~8` 选择替换目标"。
* **风险**: `core/inscription/InscriptionManager.kt` 当前的 `INSCRIPTION_HOTKEY_START + loadout.slots.size` 公式以常量驱动；PR 文档把 5~8 当硬字面量。若未来 `INSCRIPTION_HOTKEY_START` 调整为 6 或 `F1`，文档与代码会不一致。
* **必修**: §5.4 所有"5~8"替换为"`INSCRIPTION_HOTKEY_START .. (INSCRIPTION_HOTKEY_START + MAX_INSCRIPTION_SLOTS - 1)`"，或在 §5.4 首句固定"以下 5~8 假定 `INSCRIPTION_HOTKEY_START=5 且 MAX_INSCRIPTION_SLOTS=4`"。

### 3.5 Major - `category_limit` 替换后 count 公式未显式给出

* **位置**: §5.2 替换规则第 2 条"临时移除目标槽后检查类别上限"。
* **风险**: 实现者易把"替换后 count = currentCount + 1"，漏掉"如果目标槽类别 == 候选类别，则 count 不变"。
* **必修**: §5.2 显式写出 `postCount = currentCount - (targetCategory == candidateCategory ? 1 : 0) + 1`；并列 2 个反例：
  * `targetCategory == candidateCategory` 且 `currentCount == 2`（上限）→ 允许（2→2）；
  * `targetCategory != candidateCategory` 且 `currentCount == 2`（已含 2 个同类别）→ 拒绝（2→3）。

### 3.6 Major - 旧 save canonicalize 行为过简

* **位置**: §5.1 "旧 save canonicalize 只补本职业起始 2 个"。
* **场景**: 旧 save 玩家已装备 `healing_light / phase_door / iron_shield / purge` 共 4 槽。canonicalize 时：
  * 若"补 2 个"理解为重置成 2 槽 → 失去玩家已有进度；
  * 若理解为"保留 4 槽，但起始仍是 2"（新 run 用）→ 容易，但与"`starterInscriptionMaxCount <= 2`"的终局报告字段定义冲突（因 starter 指的是 new run 起始 count）。
* **必修**: §5.1 新增 `save canonicalize` 子节，固定：旧 save 的 active run 不变（保留现有 4 槽），但 `RunSummary.startingInscriptionCount` 的回填值以"该职业 PR-02 后的 starter 2 个"为准（因为这是事后统计，非实时状态）。

### 3.7 Major - `fullSlotInscriptionPurchaseRejectedCount = 0` 事件口径

* **位置**: §6 阻塞指标。
* **缺项**: "rejected" 包含哪些子路径？
  * 玩家点击 → 进入替换 UI → `Esc` 放弃 → 算 rejected 还是 cancelled？
  * 金币不足 → 算 rejected 还是 gold_denied？
* **风险**: 本意"因缺替换流程失败=0"若按字面执行，玩家主动放弃会错误触发 fail。
* **必修**: 重命名为 `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` 或 `fullSlotInscriptionPurchaseMissingReplacementCount`，并在 §6 给出"玩家已进入替换 UI 后 `Esc` 不计入"的精确定义。

### 3.8 Minor - 替换 UI 中"替换前/后类别数量"的表达形式

* **位置**: §5.4 展示字段。
* **建议**: 在 UI 中以"Recovery: 2/2 → 2/2 | Movement: 1/2 → 2/2"的前后对比呈现，而不是孤立数字；否则玩家看到一列"2,2,1"难以在 UI 中做决策。PR 文档应固定 UI 字串 i18n key 的格式。

### 3.9 Minor - 商店 "`REQUIRES_REPLACEMENT_TARGET`" 客户端 round-trip 未定义幂等性

* **位置**: §5.3 流程第 4~7 条。
* **问题**: 客户端打开替换 UI 后，玩家选择 hotkey → 再次提交 `BuyShopOffer(offerIndex=X, replacementHotkey=5)`。若此间 shop offer pool 因其他玩家操作刷新（同 run 内不会，但 save/load 可能），`offerIndex=X` 指向的 offer 可能已失效。
* **建议**: 把 `BuyShopOffer` 扩展为包含 `offerFingerprint`（如 shopId + offerIndex + offerDef.id 的 hash），服务端比对一致才成交。

---

## 4 · PR-03 Build Identity Reward Adoption

### 4.1 Blocker - `wrongProfessionCapstonePenalty` 未给出评分公式

* **位置**: §5.2 breakdown 字段 / §5.1 固定规则。
* **缺项**: 字段是 additive 还是 multiplicative？最小 floor？与 `professionCapstoneBonus` 的相对量级？若 penalty 只减 5%，arcanist 仍会采用 `unique_furnace_plate`（本 PR 要阻止的核心案例）。
* **风险**: 不给公式，实现侧易选"轻微降权"，白盒 lab 指标过不了；或选"硬 ban"，让跨职业趣味性丢失。
* **必修**: §5.2 下补 `ScoreFormula` 子节，固定:
  * `finalScore = baseScore + professionCapstoneBonus + nonWeaponPayoffBonus + slotRotationBonus + terminalIdentityBonus - wrongProfessionCapstonePenalty - duplicateSlotPenalty`
  * `wrongProfessionCapstonePenalty = 0.8 * baseScore`（硬降权到总分接近 0.2x），并说明 "non-capstone 的 wrong profession 匹配不触发 penalty，仅触发 0 bonus"。

### 4.2 Blocker - Slot balance 阈值可满足性未建模

* **位置**: §5.3 slot balance 阈值：ARMOR `>= 20%`、WEAPON `>= 25%`、OFF_HAND `>= 25%`、`maxSlotShare <= 50%`。
* **问题**: 这 3 个"lower bound"合计 70%，剩 30% 分给其他 slot（HELMET / NECK / RING / TRINKET 等）。若实际 slot 枚举 >= 6 类，其他每类只剩 5% 份额 —— rotation 规则"连续两次不得给同一 slot"在剩余 slot 占比过低时几乎必然失败（rotation 找不到候选）。
* **必修**: §5.3 新增完整 slot 份额表（列出所有 slot 枚举），保证 `sum(lowerBounds) + 其他 slot 合理 floor <= 100%`；或把 ARMOR/WEAPON/OFF_HAND 的 lower bound 改为 15% / 20% / 20%。

### 4.3 Blocker - `basic_shield` 进入 rogue 终局 OFF_HAND 的根因不是评分差，是池

* **位置**: §2 问题 7 "rogue 终局 OFF_HAND 仍保留 `basic_shield`" / §5.1 第 4 条"`artifact_briar_heart` 对 rogue 的 OFF_HAND payoff 必须压过 `basic_shield`"。
* **事实核对**: `professions/index.yaml` rogue startingKit 为 `[short_sword, leather_armor, healing_potion]`，**不含 basic_shield**。rogue 是从 loot 捡到的 basic_shield。单纯把 artifact_briar_heart 加 bonus 并不能阻止 rogue 在中前期捡到 basic_shield 并一路穿到终局。
* **必修**: §5 新增 "`5.5 后期 quality floor`" 子节，固定"rogue 在 level >= 5 时，OFF_HAND 普通 rarity 不再进入 milestone candidate pool（但仍可自然掉落）"，或"milestone selector 对 `rarity==COMMON` 应用额外 penalty"。否则本 PR 跑不绿。

### 4.4 Blocker - `topFiveAffixExposureShare <= 40%` 阈值闭环不完整

* **位置**: §5.4。
* **问题**: 下调 `sentinel / of_strength / vampiric / of_life` 权重 + 仅提升 `of_smite / of_shadow / of_piercing` 的 floor。当前 affix 池远 > 7 个；若其余 affix 权重未同比调整，下调后头部仍会落在 4~5 个相近 affix 上，share 依然可能 > 40%。
* **必修**: §5.4 新增"affix 权重总表校准"子节，要求 `lootBalanceLab` 输出 full affix distribution diff；阈值调整必须附带 diff 样本。

### 4.5 Major - `unique_cinderveil_plate` 升/降权 方向不对称

* **位置**: §5.1 第 1、2 条。
* **问题**:
  * `unique_furnace_plate` 对非 vanguard 降权 —— 单向 penalty；
  * `unique_cinderveil_plate` 对 arcanist 升权 —— 单向 bonus，但没对称声明 "对 vanguard/rogue/templar 的影响"。
* **风险**: 在 vanguard run 里看到 `unique_cinderveil_plate` 会出现什么？按默认规则它是 arcanist capstone，`wrongProfessionCapstonePenalty` 应触发，但未显式说明。
* **必修**: §5.1 新增一张 "(capstone × profession)" 权重矩阵，显式声明每个 capstone 对 4 职业的 penalty/bonus 方向。

### 4.6 Major - `build-identity/index.yaml` 现状未核对

* **位置**: §5.1 "保留 `artifact_briar_heart / artifact_heartroot_gambit / unique_thornpath_crook / unique_briarbound_bow`"。
* **风险**: "保留"暗示这些 id 已经在 build-identity 里。但 PR 文档未列出当前 YAML 的实际内容快照。若任一 id 缺失或类别分类错误，"保留" 指令没有实际语义。
* **必修**: §5.1 前补一段"Build identity 现状快照"表格，逐行对当前 `game/src/main/resources/data/build-identity/index.yaml` 里每个 capstone 的 profession/slot 绑定做 "KEEP / MOVE / ADD / REMOVE" 标注。

### 4.7 Major - milestone reward slot history 的共享范围

* **位置**: §5.3 第 2 条"route/cache/support/boss source 共享 slot history"。
* **问题**: 当前 milestone source 是 4 种 (route/cache/support/boss)。若共享 history，则"连续两次同 slot" 会在**跨 source** 也被触发——玩家刚从 boss 拿到 WEAPON，下一个 route milestone 也不能给 WEAPON。这对 rogue/vanguard 这种以武器为主要身份的职业可能反效果。
* **必修**: §5.3 新增 "history scope" 定义，建议"同一 source 连续计数" + "跨 source 软计数（作为 slotRotationBonus 输入，而非硬 ban）"。

### 4.8 Minor - `rewardScoreBreakdownSamples` 缺采样规模

* **位置**: §6 supporting 指标。
* **建议**: 增加 `>= 20 sampled decisions per run, grouped by profession` 的采样约束。

### 4.9 Minor - `capstoneAdoptionBySlot` 与 `milestoneRewardSlotBalance` 计数 overlap

* **位置**: §6 阻塞 + supporting 指标。
* **建议**: 说明两者的 ground truth 差异（前者按"采用"记，后者按"候选池出现"记），并在 `Phase4MetricCatalog` 中显式 cross-reference。

---

## 5 · PR-04 Hidden Search And Zone Hooks

### 5.1 Blocker - `zoneLeadDiscoveryMaxShare <= 40%` 与 `greenwood_fringe` 体验目标冲突

* **位置**: §5.1 第 4 条"`greenwood_fringe` 保持高 entry 转化"；§5.4 阻塞指标。
* **问题**: greenwood_fringe 天然是"易发现"区；若全局阈值 40% 硬卡，实现只能降低 greenwood lead density 来凑数。这违反 §3.2 非目标第 6 条"不降低 secret 发现难度来刷指标"。
* **必修**: 阈值改为 "**secret-bearing zone** 中 `topLeadShareAcrossSecretZones <= 40%`（不含 greenwood_fringe）"，并把 greenwood_fringe 在此指标中标为 `excluded.beginnerOnramp`。

### 5.2 Blocker - `perZoneSearchUseWarning > 0%` 空洞阈值

* **位置**: §5.4 阻塞指标。
* **问题**: 把 abyssal_temple 从 0% 调到 0.01% 即过关，但 Search 学习链完全未建立。
* **必修**: 改为 "`secretZoneSearchConversionFloor.reportOnly`: 每个 secret-bearing zone 至少 `>= 5%` successful Search→secretEntry conversion"；阻塞层用 `zoneSearchPromptVisibility: 每个 secret-bearing zone 玩家在非 flavor 状态下至少有一次 Search prompt 可见`。

### 5.3 Blocker - Runtime hook 的"玩法深度"边界未划清

* **位置**: §5.2 runtime hook 清单。
* **问题**: 例 `ritual_pressure`"影响下一次 encounter pressure"——"影响" 指什么？增加敌人 spawn weight？增加 boss 阶段优先级？不清不楚。PR 标题是"hidden search and zone hooks"，但如果 hook 要动 encounter pressure，属于 encounter system 改动，可能是另一 PR 规模。
* **必修**: §5.2 为每个 hook 加一行 "minimal wiring" 描述，明确 runtime 影响的具体字段（`ZoneMechanicRuntime` 中 哪个状态发生变化、变化上限）。Hooks 必须全部是"前台可感知 + 数据可观测 + 不触及 combat balance 核心公式"。

### 5.4 Major - `flavorOnlyMechanics` 白名单机制未闭环

* **位置**: §5.2 末尾。
* **问题**: "未进入 runtime 的名词必须在 report 中标为 flavorOnlyMechanics" —— 但如果开发者新增了 flavor 名词却忘记加白名单，报告里什么都不会报。
* **必修**: §5.2 新增 "`mechanicsWithoutDedicatedRuntimeHook` 必须 == `flavorOnlyMechanics ∪ knownRuntimeHooks`；否则 validator fail"。

### 5.5 Major - Frontstage cue 的 TTL / dedup 未给数值

* **位置**: §5.3。
* **问题**: 只说"stable key、TTL、dedup"。TTL = 当前回合？3 回合？永久到玩家交互？dedup key 粒度（zone × type × cell × entity？）没定义。
* **必修**: §5.3 给出 `lead_discovered TTL=until player moves into adjacent cell or 10 turns`、`search_available TTL=玩家离开 cue 格`、`secret_entry_nearby TTL=until entry consumed`、`zone_hook_triggered TTL=本 zone visit 内`。dedup key 固定 `{zoneId, cueType, anchorEntityId or anchorCell}`。

### 5.6 Minor - `deep_iron_pit` slag/ore cue 的最小密度保证

* **位置**: §5.1 `deep_iron_pit` 行。
* **问题**: Search prompt 仅在玩家接触 slag/ore cue 时出现。若地图生成 cue 密度过低，玩家走不到即为 0 触发。
* **建议**: 新增 `deep_iron_pit.slagCueMinDensityPerRoom >= 1`（或等效约束），固定 mapgen pipeline 的下限。

### 5.7 Nit - "zoneLeadDiscoveryMaxShare" 命名与"perZoneLeadDiscoveryMaxShare" 不同语义

* **位置**: §1 完成标准第 5 条 / §5.4。
* **建议**: 重命名为 `topZoneLeadShare` 避免和 per-zone 指标混淆。

---

## 6 · PR-05 Boss Variant Phase Language

### 6.1 Blocker - `phaseOverrides` 只有 schema 没有 runtime wiring

* **位置**: §5.1 Schema / §5.4 Boss harness 输出。
* **问题**: §5.1 定义了数据结构，但 `BossPhaseManager` / `BossEncounter` 里如何读 `phaseOverrides`、如何在 `phase_enraged` 阶段注入新 trigger、如何提升 `actionEmphasisIds` 权重完全没写。`phaseOverrideCoverage=3/3` 如果只是 schema coverage，report 通过但玩家体验无变化。
* **必修**: §5.1 下补 "`5.1.2 Runtime wiring`"：
  * `BossPhaseManager` 在进入 phase 时，若该 variant 有 override 且 override.phaseId == currentPhase，触发 `onEnterEventKey` 并叠加 `actionEmphasisIds` 的 weight multiplier（给出公式，如 × 1.5）。
  * 指标必须区分 `phaseOverrideSchemaCoverage` 与 `phaseOverrideRuntimeTriggerCoverage`，两者均 3/3 才算完成。

### 6.2 Blocker - 复合 trigger (`hp_below_X_and_Y`) 解析器不存在

* **位置**: §5.2 trigger 列表 `hp_below_50_and_oil_or_fire_seen` 等。
* **事实**: 当前 trigger 一般是 `hp_below_threshold` 单维度。复合 trigger 需新 evaluator。
* **必修**: §5.1 Schema 下补 "`TriggerExpression`" 子结构（建议 AND/OR 二元树），在 `BossPhaseManager` 中加 `TriggerEvaluator`；或者把 trigger 设计成`triggerExpression: { and: [hp_below:50, any_of: [oil_seen, fire_seen]] }`。schema 与实现必须同步。

### 6.3 Major - `grey_crown` trigger `war_caller_active` 的 canonical 来源

* **位置**: §5.2。
* **问题**: `war_caller` 是 `grey_crown` 的扈从 boss？还是某个 action id？还是 buff id？未定义。trigger reference 若是 free-form 字符串，会出现静默 miss。
* **必修**: §5.2 trigger 列下加一列"reference resolution"，指明每个 trigger 引用的 canonical 来源 (`bossEntity.id`, `action.id`, `buff.id`)。

### 6.4 Major - Telegraph spec 必须在 `data/telegraph/index.yaml` 显式 ADD

* **位置**: §5.3 "新增 telegraph spec id" / §3.1 范围已列 `telegraph/index.yaml`。
* **问题**: 文档明确 "不新增资源"，但 telegraph spec **entry** 必须新增 3 条，否则 boss-variants 里的 reference 会 dangling。README §3 关于"不新增"应与此对齐（见 1.2）。
* **必修**: §5.3 新增"`必须 ADD 3 条 telegraph spec entry 于 data/telegraph/index.yaml`"，并说明 validator 必须在 DataLoader 验证阶段报 dangling reference。

### 6.5 Major - override 与现有 phase hp threshold 的关系

* **位置**: §5.1 第 5 条"override 不改变 hp threshold"。
* **问题**: 现有 phase (如 `phase_enraged`) 已经有进入 threshold；override 的 trigger 又是一个 hp-based 条件。当两者不一致时（例 phase_enraged 的 base threshold 是 hp_below_60，override trigger 是 hp_below_50）—— 到底谁先触发？
* **必修**: §5.1 新增"`overlay 语义`"：trigger 是**附加**条件（override 仅在 phase 已进入且额外 trigger 满足时生效）。默认 override.trigger == null 时仅按 phase 入口触发。

### 6.6 Minor - `phaseGraphUnchanged` 指标的保留理由

* **位置**: §5.4 末行。
* **建议**: 增补解释 "该字段用于让审查者一眼看出 Phase4 没有做 phase graph mutation，Phase5 才做"，避免未来被误当成 bug 保留。

---

## 7 · PR-06 Long Run Route Diversity

### 7.1 Blocker - `zoneRouteHashDiversity.topHashShare <= 40%` 在现提 seed 集下不可满足

* **位置**: §5.1 新 9 条 seed / §5.2 阈值。
* **事实检查**: 9 条新 seed 中 4 条以 `greenwood` 开局、3 条以 `deep_iron` 开局、1 条 `underground_river`、1 条 `abyssal_temple` branch。若 route hash 按 "首 zone + 顺序" 计算，greenwood 开局占 12 条 full_route 的约 5/12 ~ 42%；接近或突破阈值。
* **必修**: §5.2 明示 route hash 算法（建议 `hash(seq(zoneId), ignoreBranchOrder=false)`），并**重新列出**新 9 条 seed 使 `greenwood 开局 / deep_iron 开局 / underground_river 开局` 分布更均衡（建议 4:4:4 左右）。

### 7.2 Blocker - branch_inclusive 样本数与列表不对齐

* **位置**: §5.1 第 7~9 条新增 3 条 branch_inclusive；§1 完成标准要求 `branchInclusiveCount >= 4`；§2 当前 `branch_inclusive=1`。
* **算术**: `1 (现有) + 3 (新增) = 4` 仅当现有 1 条未与新增重合。但 §5.1 新增 `underground_river secret branch` 与 `abyssal_temple secret branch` 可能已在现有 1 条覆盖。
* **必修**: §5.1 列出现有 1 条 branch_inclusive 的 seed 与路线；确保新 3 条与现有不重合；或直接新增 4 条 branch_inclusive 把 "`>= 4`" 当硬下限。

### 7.3 Major - `route_probe / late_route_probe` "保持现有职责"但未固定数量

* **位置**: §5.1 表末。
* **问题**: 若这两类未给目标样本数，owner baseline diff 可能把"未显式声明"当成变化。
* **必修**: §5.1 加一行"`route_probe: keep existing count (N=?)`；`late_route_probe: keep existing count (N=?)`"。不给数量的"保持现状"在 CI 侧无法断言。

### 7.4 Major - `verifyChanged` 路由规则不覆盖 reward/build-identity 改动

* **位置**: §5.3 路由规则。
* **问题**: 第 1 条"修改 long-run corpus、route hash、harness runner" 路由 longRunLab。PR-03 会改 `MilestoneRewardSelector / build-identity`，这些影响 route diversity 样本结论但不在第 1 条 cover 列表。
* **必修**: 扩为"corpus、route hash、harness runner、milestone reward selector、build identity、affix weights"全部路由 longRunLab。

### 7.5 Minor - seed 字面 `2026042401` 与现有格式

* **位置**: §5.1 seed 列。
* **问题**: 若现有 seed 是纯整数 long 或 shortrandom，`2026042401`（约 20 亿）在 Int32 范围内但边缘；`2026042409` 同。
* **建议**: 确认 `HeadlessRunHarness` 对 seed 的解析位数；若只支持 Int32，建议改为 `26_04_24_0001~0009` 或给前缀。

---

## 8 · PR-07 Sample Pack ADD-first Visibility

### 8.1 Blocker - sample pack 从 REPLACE 改 ADD 后，base `underground_river_crystal_rift` 仍存在，并行生成策略未定义

* **位置**: §5.1 第 3 条"sample pack 不覆盖 base `underground_river_crystal_rift`"。
* **问题**: 原 REPLACE 的语义是"装 pack 则替代 base 体验"。改成 ADD 后，underground_river 区生成时会同时持有 `crystal_rift`（base）与 `flooded_reliquary`（sample pack）两个 secret zone 候选。若该 region 的 secret slot 容量是 1，ADD 的 pack zone 永远不会被生成，`samplePackContentPlayerVisibilityRate` 永远为 0。
* **必修**: §5.1 新增 "`secret zone selection`" 规则：
  * underground_river region `secretZoneCandidates` 扩展为多候选；
  * sample pack 装载后，`flooded_reliquary` 至少有 50% 概率生成（或 alternate 策略）；
  * 或官方 sample pack 声明"`sample.flooded_relics` 挂到一个 base 不占用的 secondary slot"。

### 8.2 Blocker - `samplePackContentPlayerVisibilityRate >= 1 per fixed-seed sample run` 与 mapgen 概率不匹配

* **位置**: §5.2。
* **问题**: "固定 seed"下，sample pack content 是否必然出现？如果 secret zone 生成概率 < 100%，`>= 1 observed touch` 在固定 seed 下仍可能是 0。
* **必修**: §5.2 定义 `sample-run fixed-seed must force-inject sample secret zone at mapgen`；或把阈值改为 "在一组 fixed-seed sample runs 中至少一条 run 有 touch"（更弱但实际可达）。

### 8.3 Major - fixture pack 作为 REPLACE 学习范本的"发现路径"断开

* **位置**: §3.2 非目标第 2 条。
* **问题**: 官方 sample 改纯 ADD 后，content pack 作者学习"如何安全 REPLACE" 的唯一官方范本只剩 `tools/src/main/resources/fixtures/content-packs/**`。fixture 通常是测试资产，不在文档首页；作者可能完全看不到。
* **必修**: 本 PR 需同步更新 pack authoring 文档（或在 `examples/content-packs/sample.flooded_relics/README.md` 注明"REPLACE 学习范本请参阅 fixture pack 路径"），否则教育闭环断裂。

### 8.4 Major - Validation overlay 字段"pack-local visual/audio/i18n key resolution status" 的 UX 密度

* **位置**: §5.3。
* **问题**: "resolution status" 是 boolean？每 key 一行？最小信息密度与排版未定义。若直接 dump 几百个 key 到 validation overlay，玩家/QA 无法阅读。
* **必修**: §5.3 补 UI sketch —— 建议聚合成 `resolved=120 / dangling=0 / overridden=2` 三数字摘要；展开时才列明细。

### 8.5 Minor - `fixture.sample_flooded_relics_override` 命名 dot/underscore 混用

* **位置**: §3.1 fixture 路径。
* **建议**: 若 pack id 解析器对 `.` 敏感，id 写成 `fixture.sample_flooded_relics_override` 可能被按 `fixture.sample_flooded_relics_override` 整体识别或被错误 split；建议验证 `ContentPackIdParser` 兼容性，或重命名为 `fixture_sample_flooded_relics_override`。

### 8.6 Nit - sample pack README 未提及

* 建议本 PR 同步更新 `examples/content-packs/sample.flooded_relics/README.md`（若无则新建），列出 ADD-first 示范与"作者指南"。

---

## 9 · 跨 PR 系统性问题

### 9.1 Blocker - 职业范围口径分歧（见 1.1）

贯穿 PR-01 / PR-02 / PR-03 / PR-06。必须在 README 层先 canonicalize。

### 9.2 Blocker - "不生成新资源" vs "新增 manifest key" 口径（见 1.2）

贯穿 PR-04 / PR-05 / PR-07。必须在 README 层先划分。

### 9.3 Blocker - 每 PR 新增 blocking 指标 ≈ 5~7 条，7 PR 合计接近 40 条，无全局字段冲突检查

* **缺项**: `Phase4MetricCatalog` 里没有"字段 id 不重复 + 命名规则一致 + blocking/supporting 互斥"的 linter 要求。
* **必修**: README §4 之后新增一节 `指标目录扩增纪律`：
  * 所有新 field 必须在 `Phase4MetricCatalog` 里登记，走单一 owner；
  * blocking 指标命名统一前缀 `blockingX.Y`；
  * supporting 指标统一后缀 `.reportOnly` 或 `.observation`；
  * 新 field 登记时必须同步更新 `Phase4OwnerMetricTargets`。

### 9.4 Major - i18n key 命名在 7 PR 间无共同风格

* **缺项**: 每 PR 都动 `en-US.json / zh-CN.json`，但没有统一 key 前缀规范。
* **建议**: README 新增 "i18n key convention"：`ui.<module>.<screen>.<label>` / `log.<module>.<event>` / `metric.<phase4Metric>.desc`。所有 7 PR 的新 key 必须遵守。

### 9.5 Major - UI layout 缺少 snapshot 测试要求

* **缺项**: PR-01 §5.6、PR-02 §5.4、PR-07 §5.3 新 UI layout 没有要求"ASCII render snapshot" 或 "AsciiRenderModel test"。无法防 UI 回归。
* **必修**: 每个新 UI 必须同步新增 `client/src/test/kotlin/com/ktome/client/ui/**/*SnapshotTest.kt`。README 增补该要求。

### 9.6 Major - 开发期快路径与 CI 重路径并存

* **问题**: `longRunLab + reportPhase4` 本地耗时高；PR-06 才引入 `verifyChanged` 子集。前 5 个 PR 的开发期本地迭代成本极高，会诱导开发者"只跑单测即提交"。
* **必修**: 每 PR §7（验证命令）额外列一条"开发期快路径"，只跑 `:module:test` + 相关 harness smoke。PR-06 merge 后再替换为 `verifyChanged`。

### 9.7 Major - 所有 PR 均无"玩家体验验证"（非自动化）指令

* **观察**: Phase4 是 completion hardening，但 7 个 PR 的 "完成定义" 全部是自动化指标/字段。没有一个 PR 写"人工手测 golden path"的最小 checklist。
* **必修**: 每个 PR §8 之后新增一节 `人工手测 golden path`（3~5 条即可），按用户视角描述玩家第一分钟 / 十分钟 / 三十分钟 / 终局的体验期待。尤其 PR-01、PR-02、PR-04 的 UX 目标很难只靠 report 字段验证。

### 9.8 Minor - 资源 key 是否都在 manifest 中的预检脚本缺失

* **建议**: README §4 新增"执行每个 PR 前必须跑 `./gradlew assetLint audioLint dataLint` 并附到 PR description"。

### 9.9 Minor - PR-01/PR-02 的 UI 交互语法一致性

* **问题**: PR-01 talent UI 用 `Enter=preview, Space=confirm`；PR-02 inscription UI 用 `Enter=confirm`。两条语法矛盾。
* **必修**: 在 PR-01 阶段就对齐到统一语法（建议 `Enter=confirm, P=preview toggle`），由 client `InputHandler` 层保证一致。

### 9.10 Minor - 所有 PR 的 "资源生成结论" 重复相同段落

* **观察**: 每个 PR 都有 §4.1/§4.2 样板文字，语义高度同质。
* **建议**: 把通用段落抽到 README，每个 PR 只写差异点（引用了哪些现有 key）。降低文档漂移面。

---

## 10 · 执行前必须先修的硬阻塞清单（Must-fix Before Implementation Starts）

以下 **每一条都必须在 PR 开工前回修到文档**，否则实现阶段将出现"报告绿了但体验没兑现" 或 "体验对了但指标卡死" 的反向失败：

1. **README.1.1**：职业 tier 与阻塞门槛口径。
2. **README.1.2**：manifest key ADD vs asset 生成的明确区分。
3. **PR-01.2.1**：6 职业 vs 4 基础职业统一口径。
4. **PR-01.2.2**：Tier 2 门槛收紧 + `multiTreeInvestmentRate` 跨树定义。
5. **PR-01.2.3**：`breakpointChoiceEventRate` 事件精确定义。
6. **PR-01.2.5**：存档迁移策略。
7. **PR-02.3.1**：`InscriptionEquipFailure` 接入函数签名。
8. **PR-02.3.2**：替换后冷却状态行为。
9. **PR-02.3.3**：铭文升级对的 "sameInscription" 判定。
10. **PR-03.4.1**：`wrongProfessionCapstonePenalty` 评分公式。
11. **PR-03.4.2**：slot balance 阈值可满足性全表。
12. **PR-03.4.3**：后期 OFF_HAND quality floor（rogue basic_shield 修根）。
13. **PR-03.4.4**：`topFiveAffixExposureShare` 闭环数据核对。
14. **PR-04.5.1**：`zoneLeadDiscoveryMaxShare` 对 greenwood_fringe 豁免。
15. **PR-04.5.2**：Search 学习链阈值由 `> 0%` 升级为可观测 conversion。
16. **PR-04.5.3**：runtime hook 玩法深度边界。
17. **PR-05.6.1**：`phaseOverrides` runtime wiring。
18. **PR-05.6.2**：复合 trigger 解析器。
19. **PR-06.7.1**：route hash 算法定义 + seed 分布重排。
20. **PR-06.7.2**：branch_inclusive 样本不重合证明。
21. **PR-07.8.1**：sample pack ADD 后的 secret slot 并行生成规则。
22. **PR-07.8.2**：fixed-seed 下 sample content 必现机制。
23. **跨.9.3**：指标目录扩增纪律 + 全局字段冲突检查。
24. **跨.9.5**：新 UI 必配 snapshot 测试。
25. **跨.9.7**：每 PR 必配人工手测 golden path。

---

## 11 · 不是问题但值得肯定

* 7 个 PR 均明确 **"不引入局外数值成长"**、**"不引入 Lua / 脚本宿主"**、**"不动 Phase5 AI / replay"** 等非目标 —— 边界守得很紧，避免 scope creep。
* PR-03 把 `professionCapstoneAdoptionFloor` 从 approved_debt 升到 blocking，是非常正确的"把债转成门" 做法。
* PR-05 明确 "data-level phase language vs full phase graph" 的界线，避免把 Phase5 工作量塞进 Phase4。
* PR-06 把 `verifyChanged` 最小路由与 nightly/owner gate 拆开，是对 CI 成本的正确建模。
* PR-07 把 sample pack 教育责任与 fixture pack 测试责任分离，是 content pack 生态良性演化所需。

这些结构决策应该在 PR 的 "Why it works" 段保留，避免将来被误读为保守退让。

---

## 12 · 结语

这一轮 PR 级开发文档在**方向**、**优先级**、**依赖顺序**、**资源管线约束**上整体**走在正确的路上**。最后冲刺阶段的障碍，不在于"要不要做"（几乎每条都该做），而在于"做出来是否能稳定跑绿 + 能被玩家感知"。上述 25 条硬阻塞回修 + 系统性问题，是把"文档看起来完备" 转为 "实现能兑现体验" 的最后一公里。

按现有文档直接开工的风险评估：

| PR | 直接开工成功率估计 | 主要瓶颈 |
| --- | ---: | --- |
| PR-01 | 40% | Tier 门槛、存档迁移、事件定义 |
| PR-02 | 50% | 冷却状态、升级对、枚举未落地 |
| PR-03 | 30% | 评分公式缺失、rogue basic_shield 修根 |
| PR-04 | 40% | greenwood 阈值、runtime hook 边界 |
| PR-05 | 35% | runtime wiring、复合 trigger |
| PR-06 | 55% | route hash 定义 |
| PR-07 | 50% | secret slot 并行生成 |

回修上述 25 条后，整体开工成功率预计可提升至 85% 以上。

**建议的下一步**：按"硬阻塞清单"逐条修订文档后，由同一审阅者做一次二轮复核（focus 在修订 delta 而非全文），确认无新缺口后开工 PR-01。
