> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
> `docs/review/phase4/v4-pr/README.md`

# Phase4 v4 PR-01 Profession Tree Run Choice

**阶段**: `Phase 4 completion hardening / phase4-v4-pr01`
**优先级**: `P0`
**工作量**: `L`
**合并来源**: v4 P0-1 的职业树部分，职业树/铭文补充设计第 1~7、11~14 章
**前置条件**: PR-00 已完成；Phase4 UI/UX PR 已完成，现有 `DescriptionPresenter`、`ModalStack`、`TalentAllocationDraft`、`RenderSnapshot` 可被复用
**资源生成结论**: 不生成图片资源；不生成音频资源

## 1. 玩家体验目标

本 PR 的目标是让升级第一次真正成为构筑入口。玩家在前 10 分钟必须面对“学习新技能还是强化已有技能”的选择，前 30 分钟必须形成职业树路线承诺。

完成标准：

1. 六个可用职业开局职业技能数固定为 `3`。
2. 第 4 个职业行动槽开局为空。
3. `unlockLevel` 只让技能进入 `LEARNABLE`，不再自动写入 rank 1。
4. 学习新技能 rank 0 -> 1 消耗 1 点职业天赋点。
5. 提升已学技能 rank N -> N+1 消耗 1 点职业天赋点。
6. Tier 2 / Tier 3 同时受等级、前置 rank、树内投入约束。
7. Talent UI 以 sidebar 列式展示三棵职业树、全量节点、锁定原因、学习预览、断点预览。
8. `RunSummary`、`longRunLab`、`phase4Report` 能统计学习事件、断点事件和多树投入。
9. release-facing blocking 指标只统计 `vanguard / arcanist / rogue / templar` 四个 `BASE` 职业。
10. 数据改造同步覆盖 `berserker / spellblade` 的 3 starter 与 learnable 第四技能；这两个 `ADVANCED` 职业进入 report-only coverage，不扩技能数量。

## 2. 当前问题

1. `TalentProgression.unlockedTalentIds` 按 `unlockLevel` 返回节点后，被 `FoundationGameSession.syncUnlockedPlayerTalents` 自动写入 `TalentLoadout.talentLevels`。
2. 当前基础职业 `startingTalents=4`，第 1 层已经填满四个职业行动槽。
3. `TalentAllocationDraft` 的玩家体验主要是已有技能 rank 提升，学习新技能压力不足。
4. UI 侧只围绕 active/reserve loadout 展示技能，没有完整的三树 sidebar、locked/learnable/learned 树视图。
5. report 侧没有直接统计“玩家学了什么”和“断点选择是否发生”，只能从装备 payoff 间接推断构筑。

## 3. 范围与非目标

### 3.1 范围

生产代码：

- `game/src/main/kotlin/com/ktome/game/TalentProgression.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt`
- `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt`
- `game/src/main/kotlin/com/ktome/game/GameView.kt`
- `core/src/main/kotlin/com/ktome/core/talent/TalentAllocationDraft.kt`
- `core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt`
- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
- `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
- `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
- `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt`
- `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt`
- `client/src/main/kotlin/com/ktome/client/audio/AudioRouter.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/**`

数据与文档：

- `game/src/main/resources/data/professions/index.yaml`
- `game/src/main/resources/data/talents/index.yaml`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`
- `docs/review/phase4/opt/baselines/2026-04-24-phase4-profession-tree-run-choice-owner-baseline.json`

测试与 harness：

- `core/src/test/kotlin/com/ktome/core/talent/TalentAllocationPlannerTest.kt`
- `game/src/test/kotlin/com/ktome/game/TalentProgressionTest.kt`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- `game/src/test/kotlin/com/ktome/game/SessionSnapshotMapperTest.kt`
- `client/src/test/kotlin/com/ktome/client/ui/talent/**`
- `tools/src/test/kotlin/com/ktome/tools/phase4/**`

### 3.2 非目标

1. 不新增基础职业技能数量。
2. 不扩 `vanguard / arcanist / rogue / templar` 到 20 个以上技能。
3. 不补 `shadowblade / warden` 的冻结职业内容。
4. 不引入数值型局外成长。
5. 不把技能树状态放进 client-only 第二真源。
6. 不通过降低敌人强度掩盖起始技能减少。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

执行要求：

1. 职业、天赋、树图标继续复用现有 `icon.skill.*`、`icon.tree.*`、`tree_*`、`portrait_*` 资源。
2. 本 PR 不新增 image plan、generation report、processing report。
3. 新 UI 状态使用现有 `UiDesignTokens`、文本、边框、色彩和已存在 icon key。
4. `assetLint` 必须证明所有 talent/tree/profession visual key 仍可解析。

### 4.2 音频资源

不生成新音频资源。

执行要求：

1. 学习新技能使用已有 `audio.ui.talent_unlock`。
2. 技能预览和树切换使用现有 UI hover/select 音频。
3. 本 PR 不新增 audio plan、generation report、processing report。
4. `audioLint` 必须证明新增事件引用只消费已有 key。

## 5. 技术方案

### 5.1 `TalentProgression` 语义重命名

新增函数：

```kotlin
fun learnableTalentIds(request: TalentProgressionRequest): List<String>
```

规则：

1. `learnableTalentIds` 返回当前条件下能投入点数的候选。
2. 返回值不代表玩家已学会技能。
3. 旧 `unlockedTalentIds` 删除；正式调用点必须改为 `learnableTalentIds`。
4. `TalentProgressionRequest` 是调用合同，必须聚合 `schemaCatalog`、`profession`、`level`、`learnedRanks` 与可选 `race`；不得在生产路径新增 5-arg facade 形成第二入口。
5. `learnableTalentIds` 必须从 `schemaCatalog.talents` 与 `schemaCatalog.talentTrees` 解析节点、`unlockLevel`、tree membership 与 race tree。
6. `syncUnlockedPlayerTalents` 不再消费该列表自动学习。

待删除调用点清单：

| 调用点 | 本 PR 处理 |
| --- | --- |
| `FoundationGameSession.syncUnlockedPlayerTalents` | 删除自动学习语义，改为只 materialize starter |
| `EntityFactory` 创建玩家 loadout | 只写入 starter rank，不再从 unlockLevel 衍生 rank |
| `SessionSnapshotMapper` talent snapshot | 消费 `learnableTalentIds` 与 live ranks，输出节点状态 |
| `GameView` / validation summary | 展示 learnable 与 learned，不使用 unlocked 表述 |
| `tools/src/main/kotlin/com/ktome/tools/phase4/**` | report 字段统一使用 learned / learnable / autoLearnedNonStarter 命名 |
| 相关测试 fixture | 移除“unlockLevel 等于 learned”的断言 |

破坏性改造纪律：

1. 本 PR 内所有生产调用点必须改造为 `learnableTalentIds`。
2. `unlockedTalentIds` 函数、测试 fixture 和 report 字段全部删除，不保留 `@Deprecated` wrapper。
3. PR-02 开发开始前不得存在任何生产路径对 `unlockedTalentIds` 的直接调用；PR-02 不允许新增该调用。
4. 旧 save / replay / validation fixture 仍包含 `unlocked` 语义时 fail fast，并要求刷新 fixture 或重新开局。

### 5.2 Talent 状态模型

新增 snapshot 状态枚举：

```kotlin
enum class TalentNodeState {
    LOCKED,
    LEARNABLE,
    LEARNED_RESERVE,
    LEARNED_ACTIVE,
}
```

状态判定：

| 状态 | 判定 |
| --- | --- |
| `LOCKED` | 等级、前置 rank、树内投入任一条件不满足 |
| `LEARNABLE` | 条件满足，`TalentLoadout.talentLevels` 中没有该 talent |
| `LEARNED_RESERVE` | rank >= 1，主动技能未绑定到 1~4 槽；被动技能 rank >= 1 后立即生效，无需绑定槽位 |
| `LEARNED_ACTIVE` | rank >= 1，主动技能绑定到 1~4 槽 |

### 5.3 起始技能固定改造

`game/src/main/resources/data/professions/index.yaml` 必须固定为：

| 职业 | 开局技能 |
| --- | --- |
| `vanguard` | `power_strike`, `shield_bash`, `guard_stance` |
| `arcanist` | `fireball`, `blink`, `arcane_shield` |
| `rogue` | `backstab`, `stealth`, `roll` |
| `templar` | `holy_strike`, `holy_light`, `holy_shield` |
| `berserker` | `blood_rush`, `savage_hew`, `kill_frenzy` |
| `spellblade` | `arcane_edge`, `mana_lunge`, `spell_parry` |

移出的第 4 个技能从开局 loadout 中移除，进入显式 `LEARNABLE` 池；具体 `unlockLevel` 以 `game/src/main/resources/data/talents/index.yaml` 为准，并必须满足本 PR 的 first-screen validation 场景能看到至少 1 个可学习节点。

范围口径：

1. `vanguard / arcanist / rogue / templar` 是 release-facing blocking metrics 的统计对象。
2. `berserker / spellblade` 只执行开局 3 技能、learnable 第四技能、Tier 2 / Tier 3 prerequisites 与断点校验。
3. `berserker / spellblade` 不扩为完整 16 技能职业，不进入 release-facing blocking metric 分母。
4. `shadowblade / warden` 保持 frozen，不纳入 starter、learnable、breakpoint 分母；report 必须列出 `excludedFrozenProfessions=[shadowblade, warden]`。
5. `FROZEN` 是 report / eligibility classification，不属于职业发布层级枚举；`shadowblade / warden` 继续保留当前 YAML `tier` 字段与 `frozen` tag，不在 `ProfessionTier` 增加 frozen 成员。

起始树分布固定记录到数据 review 中：

| 职业 | 开局有 starter 的树 | 开局为空的树 | 设计效果 |
| --- | --- | --- | --- |
| `vanguard` | `vanguard_arms` `1`、`vanguard_shield` `2` | `vanguard_warcry` | Warcry 路线第一点必须学习 `war_cry`，形成“新技能 vs 升 rank”的早期取舍 |
| `arcanist` | `arcanist_flame` `1`、`arcanist_arcane` `2` | `arcanist_frost` | Frost 路线第一点必须学习 `ice_bolt`，形成元素路线选择 |
| `rogue` | `rogue_assassination` `1`、`rogue_subtlety` `1`、`rogue_agility` `1` | 无 | 三树均有 starter，避免 rogue 初期被动等待 |
| `templar` | `templar_smite` `1`、`templar_grace` `2` | `templar_faith` | Faith 路线第一点必须学习 `devotion`，形成治疗防护之外的长期投入选择 |
| `berserker` | `berserker_wrath` `2`、`berserker_bloodwar` `1` | `berserker_ruin` | ADVANCED 只做 starter / learnable coverage |
| `spellblade` | `spellblade_enchanted_blade` `1`、`spellblade_battle_spell` `2` | `spellblade_elemental_flux` | ADVANCED 只做 starter / learnable coverage；`flux_anchor` 是 `elemental_flux` 的首次升级 learnable |

树 id 映射固定为：

| 职业 | tree id |
| --- | --- |
| `vanguard` | `vanguard_arms`、`vanguard_shield`、`vanguard_warcry` |
| `arcanist` | `arcanist_flame`、`arcanist_frost`、`arcanist_arcane` |
| `rogue` | `rogue_assassination`、`rogue_subtlety`、`rogue_agility` |
| `templar` | `templar_smite`、`templar_grace`、`templar_faith` |
| `berserker` | `berserker_wrath`、`berserker_ruin`、`berserker_bloodwar` |
| `spellblade` | `spellblade_enchanted_blade`、`spellblade_elemental_flux`、`spellblade_battle_spell` |

执行规则：

1. 本 PR 不重命名 talent tree id。
2. `game/src/main/resources/data/talents/index.yaml` 与 `game/src/main/resources/data/professions/index.yaml` 的现有 tree id 是权威真源。
3. 文档、report、i18n、visual/audio key、owner metric 中不得新增点号格式 tree id。

### 5.4 树投入与 Tier 门槛

树内投入公式固定为：

```text
treeInvestedPoints = sum(rank for learned talents whose talentId is in tree.nodes)
```

Tier 门槛：

| Tier | 节点位置 | 门槛 |
| --- | --- | --- |
| Tier 1 | 树内第 1、2 个节点 | `unlockLevel <= 2` |
| Tier 2 | 树内第 3、4 个节点 | `unlockLevel >= 3`，同树投入 `>= 2`，其他任一职业树投入 `>= 1`，指定前置 rank `>= 2` |
| Tier 3 | 树内第 5、6 个节点 | `unlockLevel >= 5`，同树投入 `>= 5`，指定前置 rank `>= 3` |

`berserker / spellblade` 的 ADVANCED compact tree 只有 4 个节点，切片固定为：树内第 1、2 个节点按 Tier 1，第 3 个节点按 Tier 2，第 4 个节点按 Tier 3；compact tree 仍必须满足 Tier 2 `minRank >= 2` 与 Tier 3 `minRank >= 3` 前置校验，并进入 report-only coverage。

执行要求：

1. Tier 2 / Tier 3 锁定原因必须进入 snapshot。
2. 锁定原因必须能被 `DescriptionPresenter` 转成 tokenized line。
3. data loader 必须 fail fast 校验每条非 frozen 职业树中每个 Tier 2 节点都有指定前置 rank `>= 2`，每个 Tier 3 节点都有指定前置 rank `>= 3`。
4. `multiTreeInvestmentAboveThresholdRate` 的跨树定义固定为：terminal run 中至少两棵职业树各投入 `>= 3` 点。
5. Tier 3 路线与跨树投入共存口径固定为：Tier 3 主树满足同树 `>= 5`，副树满足 `>= 1` 即计入 multi-tree；不得用“只冲主树”绕过 Tier 2 的副树投入门槛。
6. `careerTalentPointsByLevel` 必须证明基础职业在等级上限内满足至少一条 Tier 3 路线的点数需求；data review 记录每个基础职业的最小所需点数与实际可获得点数，并对 ADVANCED compact tree 保留 report-only 可行性记录。

### 5.5 Allocation Draft

`TalentAllocationDraft` 必须支持：

1. `LEARNABLE` rank 0 -> 1。
2. 已学技能 rank N -> N+1。
3. 同一 draft 中多次投入同一技能时，预览 rank 连续递增。
4. 非战斗确认。
5. 取消 draft 不改 live state。

确认行为：

1. 写入 `TalentLoadout.talentLevels`。
2. 扣除职业天赋点。
3. 新学习主动技能在存在空槽时绑定到第一个空槽。
4. 四槽已满时确认 draft 先打开 pre-commit active slot modal；玩家选择 `1~4` 替换、选择 `R` 学入 reserve、按 `Esc` 取消 draft 且不扣点。
5. 输出 `log.talent.learned`、`log.talent.rank_up`、`log.talent.breakpoint_chosen`。

断点事件定义：

1. `log.talent.breakpoint_chosen` 只在 draft 确认时发出。
2. 同一 draft 中至少一个投入使某个 talent rank 达到 UI 已展示的 breakpoint rank，才发出该事件。
3. 只 hover、只预览、只打开 tree UI 不发出该事件。
4. 事件 payload 固定包含 `professionId`、`talentId`、`treeId`、`breakpointRank`、`rankBefore`、`rankAfter`、`remainingTalentPoints`。
5. `breakpointChoiceEventRate` 的分母是 terminal run 中至少出现一次 breakpoint preview 的 run；分子是同一 run 中至少确认一次 breakpoint 事件的 run。
6. 同一 draft 中多个 talent 或多个 breakpoint 被确认时，必须为每个达成的 `talentId + breakpointRank` 输出一条事件，排序固定为 `treeId`、`talentId`、`breakpointRank` 升序。
7. `learnedTalentChoiceEventRate` 的分母固定为所有 terminal run；分子是同一 run 中至少触发一次 rank `0 -> 1` 非 starter learn 的 run。starter rank 与 rank-up 不进入该指标分子。

### 5.6 Talent UI

Talent UI 固定为 sidebar 列式树视图：三棵职业树按 tree header 分段顺序展示；输入语义仍保留树列切换，视觉不要求水平三栏。

```text
Profession: Vanguard          Talent Points: 2

[Arms]
* power_strike R2
+ charge Learn
x sweeping_strike Locked: requires charge R2

[Shield]
r shield_bash R1
r guard_stance R1
x taunt Locked: requires guard_stance R2

[Warcry]
+ war_cry Learn
x intimidation Locked: requires war_cry R2
x rallying_banner Locked

Preview:
- Learn war_cry: costs 1 point.
- Learn intimidation (passive): costs 1 point; effect applies immediately and does not require an active slot.
- Next breakpoint: intimidation unlocks at war_cry R2.
- Remaining points after confirm: 1.
```

输入规则：

1. `T` 打开三树 sidebar UI。
2. 方向键移动节点。
3. `P` 切换 draft preview 展开状态。
4. `Enter` 确认当前 draft；没有 draft 时，先对当前节点创建 draft 并立即展示 preview。
5. `Esc` 取消 draft 并返回地图。
6. `1~4` 只在 active slot 绑定 modal 中生效；三树 sidebar UI 打开时按 `1~4` 不改变选择，并在 footer 显示 `Active slots are edited from the slot panel`。
7. footer 文案 i18n key 固定为 `ui.talent.tree.footer.active_slots_from_slot_panel`。

### 5.7 存档与 fixture 破坏性边界

策略固定为：

1. 旧 save / replay 不做保留、不做回填、不做退点；检测到旧 talent schema 或 auto-learned non-starter state 时 fail fast。
2. fail-fast error 固定为 `INCOMPATIBLE_PHASE4_V4_TALENT_SCHEMA`，提示重新开局。
3. `FoundationGameSession` 只 canonicalize 当前新 run 的 starter / learnable state，不读取旧 unlocked 字段。
4. validation fixture、long-run seed fixture、golden fixture 必须按新 schema 全量刷新。
5. `RunSummary` 不输出 migration 字段，只输出 `starterProfessionTalentCount`、`learnableNonStarterTalentCount`、`autoLearnedNonStarterTalentCount=0`。

## 6. 测试与自证

### 6.1 必测行为

1. 新 run 的 `vanguard / arcanist / rogue / templar` 开局职业技能数为 `3`，第 4 个职业行动槽为空。
2. `berserker / spellblade` 开局职业技能数为 `3`，第 4 个技能只进入 `LEARNABLE`。
3. 升级到 `unlockLevel` 后，非 starter 技能不会自动写入 rank 1。
4. `LEARNABLE` rank 0 -> 1 与已学技能 rank N -> N+1 都消耗 1 点职业天赋点。
5. Tier 2 / Tier 3 锁定原因同时展示等级、树内投入、前置 rank；Tier 3 至少一个节点要求前置 rank `>= 3`。
6. Talent UI 三树 sidebar 展示 `LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE` 四类状态。
7. 新学习主动技能在存在空槽时绑定到第一个空槽，四槽已满时打开 pre-commit active slot modal；选择 reserve 后技能保留在 reserve，按 `Esc` 后 draft 取消且点数不变。
8. 取消 draft 不改 live state，确认 draft 后输出 `log.talent.learned / log.talent.rank_up / log.talent.breakpoint_chosen`。
9. `longRunLab`、`reportPhase4Only`、`reportPhase4` 输出本 PR 新增 owner metrics，且 `metricKind / producer / ownerBaseline / failSemantics` 一致。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab soloClearLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01
```

必须保留以下自证产物：

1. `build/reports/tests/` 中 `TalentAllocationPlannerTest`、`TalentProgressionTest`、`FoundationGameSessionTest`、`SessionSnapshotMapperTest` 的结果。
2. `tools/build/reports/` 中 `longRunLab`、`soloClearLab`、`reportPhase4Only` producer 产物。
3. `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` canonical report 产物。
4. `build/reports/verification/` 中 `maintainabilityLint`、`verifyChanged` 产物。
5. `build/whitebox/phase4-v4-pr01/evidence/` 中人工白盒截图、日志、manual record。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

已有游戏 Validation Mode 改造要求：

1. 本 PR 必须接入 PR-00 的 `PHASE4_V4_FAST` section，scenario id 固定为 `phase4-v4-pr01`。
2. `prepare-primary-scene` 必须在现有游戏内 validation session 中完成以下状态：`vanguard` 有 1 点职业天赋点，Talent UI 三树 sidebar 可打开，starter 三技能 learned，第 4 职业行动槽为空，至少 1 个同职业技能为 `LEARNABLE`。
3. `prepare-secondary-scene` 必须切换或重启到 `arcanist` 快速场景，展示 `fireball / blink / arcane_shield` learned 与第 4 技能 learnable。
4. `show-evidence-summary` 必须在 validation overlay 中列出本 PR 的 4 个证据截图名和 `log.talent.learned / log.talent.rank_up / log.talent.breakpoint_chosen`。
5. 这些状态必须由 game 层 validation action materialize，不得由 client 伪造 snapshot。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr01`
4. preset：`MAPGEN_DIFF`
5. seed：`2026042431`
6. runtime home：`build/whitebox/phase4-v4-pr01/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr01/evidence`
8. manual record：`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01
```

2. 执行 `build/whitebox/phase4-v4-pr01/launch-packaged-app.sh` 启动 packaged app，Computer Use 目标 app 固定为 `com.ktome.client`。
3. 按 `build/whitebox/phase4-v4-pr01/cua-runbook.md` 打开 validation overlay，执行 `PHASE4_V4_FAST / prepare-primary-scene`。
4. 记录开局三树 sidebar：starter 技能为 learned，第四职业槽为空，至少 1 个同职业技能处于 `LEARNABLE`。
5. 升级或使用 validation preset 触发 1 点职业天赋点，学习一个 rank 0 技能，确认点数扣除、空槽绑定、日志出现 `log.talent.learned`。
6. 对同一技能再投入 1 点，确认 rank 提升、断点预览更新、日志出现 `log.talent.rank_up`。
7. 移动到 Tier 3 节点，截图记录锁定原因包含等级、树内投入、前置 rank `>= 3`。
8. 打开 reserve / active slot 绑定界面，确认新学主动技能在空槽存在时自动绑定，四槽满时保留 reserve。
9. 执行 `PHASE4_V4_FAST / prepare-secondary-scene`，对 `arcanist` 重复开局检查：starter 数量为 `3`，`fireball / blink / arcane_shield` learned，第 4 个技能 learnable。
10. 执行 `PHASE4_V4_FAST / show-evidence-summary`，确认证据清单与本节文件名一致。
11. 保存证据：
    - `phase4-v4-pr01-talent-tree-start.png`
    - `phase4-v4-pr01-learnable-confirm.png`
    - `phase4-v4-pr01-tier3-locked-reason.png`
    - `phase4-v4-pr01-reserve-active-slot.png`
    - `phase4-v4-pr01-app.log`

通过标准：

1. 玩家能在 UI 中理解“学习新技能”和“升 rank”是同一资源竞争。
2. 玩家能在 Tier 2 / Tier 3 节点上看到明确锁定原因。
3. 任何非 starter 职业技能都不是升级后自动学会。
4. manual record 写明 packaged app 路径、runtime home、seed、输入序列、截图路径和结论。

### 6.4 统一验证框架关系

本 PR 新增 profession tree owner metrics，必须进入 `Phase4MetricCatalog`、`Phase4OwnerMetricTargets`、`Phase4OwnerBaselineRegistry` 和 `tools/src/main/resources/phase4/aggregation-manifest.yaml`。人工白盒记录是玩家可理解性验收面，不能替代 `longRunLab`、`reportPhase4Only`、`verifyChanged`。

### 6.5 玩家体验 Golden Path

1. 从 `vanguard` 新 run 开始，打开 Talent UI，玩家第一眼必须看到 3 个 learned starter、1 个空 active slot、至少 1 个 learnable 节点和至少 1 个 locked 节点。
2. 玩家升级获得 1 点职业天赋点后，必须在同一屏理解“学习 `war_cry`”与“升 `power_strike` rank”共享点数。
3. 玩家确认学习 `war_cry` 后，日志必须出现 learned 事件，UI 必须显示剩余点数变化和下一 breakpoint。
4. 玩家查看 Tier 2 / Tier 3 节点时，锁定原因必须同时解释等级、同树投入、副树投入或前置 rank。
5. 玩家在四个 active slot 已满时学习新主动技能，必须看到 pre-commit 替换 modal；选择 reserve 后不丢失刚学技能，按 `Esc` 后不扣点。

## 7. Report 与验收指标

新增 blocking 指标：

| 指标 | 阈值 | metricKind | producer | ownerBaseline | failSemantics |
| --- | ---: | --- | --- | --- | --- |
| `starterProfessionTalentMaxCount` | `<= 3` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-profession-tree-run-choice-owner-baseline.json` | `fail owner gate` |
| `learnedTalentChoiceEventRate` | `>= 90% terminal runs` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-profession-tree-run-choice-owner-baseline.json` | `fail owner gate` |
| `multiTreeInvestmentAboveThresholdRate` | `>= 75% terminal runs` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-profession-tree-run-choice-owner-baseline.json` | `fail owner gate` |
| `breakpointChoiceEventRate` | `>= 75% terminal runs` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-profession-tree-run-choice-owner-baseline.json` | `fail owner gate` |

新增 supporting 指标：

| 指标 | 用途 | metricKind | producer | ownerBaseline | failSemantics |
| --- | --- | --- | --- | --- | --- |
| `talentTreePrimaryInvestmentDistribution` | 观察单一主树统治 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `talentReserveSwapCount` | 观察 active slot 选择压力 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `rankBreakpointAdoptionByTalent` | 观察断点是否被采用 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `autoLearnedNonStarterTalentCount` | schema 不变量巡检，必须恒为 `0` | `supporting` | `longRunLab` | `N/A` | `display only` |

owner 接线要求：

1. 新 blocking 指标必须进入 `Phase4MetricCatalog`、`Phase4OwnerMetricTargets`、`Phase4OwnerBaselineRegistry` 和 `tools/src/main/resources/phase4/aggregation-manifest.yaml`。
2. `phase4Report` 与 `reportPhase4Only` 对这些指标必须输出同一套 `metricKind / producer / ownerBaseline / failSemantics`。
3. 职业维度指标必须按 README 的 release classification 输出 `includedProfessions=[vanguard, arcanist, rogue, templar]`、`advancedReportOnlyProfessions=[berserker, spellblade]`、`excludedFrozenProfessions=[shadowblade, warden]`。
4. `breakpointChoiceEventRate` 必须按 §5.5 的分母与分子定义聚合，不得把 hover 或 preview 当作 chosen。
5. `autoLearnedNonStarterTalentCount=0` 是 contract lint 与 longRunLab supporting 字段；它不得进入 blocking owner target。

## 8. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab soloClearLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

## 9. 完成定义

1. 六个可用职业开局职业技能数均为 `3`；release-facing blocking 分母只包含四个 `BASE` 职业。
2. 升级不再自动学习非 starter 职业技能。
3. Talent UI 显示 locked、learnable、reserve、active 全状态。
4. 学习新技能与升 rank 共用职业天赋点。
5. Tier 2 / Tier 3 受等级、前置 rank、树投入三重约束。
6. Tier 3 至少一个节点要求前置 rank `>= 3`。
7. `berserker / spellblade` 只执行 starter/learnable/prerequisite 数据改造，不扩技能数量，并进入 report-only coverage。
8. `longRunLab` 和 `phase4Report` 中出现新增 blocking 指标，并进入 canonical owner evidence。
9. `verifyChanged` 覆盖本 PR 的 code/data/report/client 影响面。
10. 没有新增图片计划文件。
11. 没有新增音频计划文件。
