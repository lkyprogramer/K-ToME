# Phase4 职业树选择机制与铭文构筑改造设计补充

## 1. 结论

本补充文档固定 Phase4 完成态前必须落地的职业树与铭文改造方向：

1. **不新增基础职业技能数量**。当前四个基础职业均已有 `3` 条职业树、每职业 `16` 个树内技能，已经达到 Phase3 最小发布集 `4 x 16 = 64` 的数量目标。当前问题不是技能数量不足，而是“等级到了自动获得技能、玩家只升 rank”的选择结构不足。
2. **职业技能必须从“自动获得”改为“达到条件后可学习”**。`unlockLevel` 只表示进入学习候选池，不再表示自动加入 `TalentLoadout.talentLevels`。
3. **起始职业技能必须收缩到每个职业 3 个已学习技能**。起始技能提供生存底线，不能填满全部职业行动槽，也不能跨三条树一次性给满核心 answer。
4. **升级点必须同时承担“学习新技能”和“提升 rank”两种成本**。学习新技能消耗 `1` 点职业天赋点；已学习技能升 rank 继续消耗 `1` 点职业天赋点。
5. **高阶技能必须通过 `unlockLevel + 前置 rank + 树内投入` 三重门槛进入候选池**。这保证玩家先投入路线，再拿核心 payoff。
6. **铭文开局固定为 2 个已装备铭文，最多仍为 4 槽**。第 3、4 个铭文通过 run 内商店、Boss、隐藏奖励安装；满槽后必须进入替换流程，不能直接拒绝购买。
7. **不引入数值型局外成长**。局外只记录 run history、职业解锁、最佳构筑摘要、图鉴/成就式记录。永久属性、永久金币、永久技能点不进入 Phase4。
8. **Phase5 不承接本改造**。Phase5 文档已限定为 tactical AI、perception/hate、replay、death analysis、perf/soak、QA 与发布收口；职业树选择和铭文经济是 Phase4 进入 Phase5 前必须补齐的基础构筑系统。

## 2. 外部设计参照

### 2.1 ToME 参照点

参考源：

- ToME Wiki: [Talent](https://te4.org/wiki/Talent)
- ToME Wiki: [Category Point](https://te4.org/wiki/Category_Point)
- ToME Wiki: [Inscription](https://te4.org/wiki/Inscription)

本项目不照搬 ToME 全量 category/mastery/category point 复杂度，但必须吸收三个核心原则：

1. **天赋点不是单纯数值升级点**：天赋点既用于学习新技能，也用于提升已学技能 rank。学习新技能本身就是构筑选择。
2. **职业树是选择压力来源**：树的主题、前置、投入深度和高阶 payoff 共同决定 build 身份，而不是角色等级自动打开全部工具。
3. **铭文/符文是独立构筑轴**：铭文提供恢复、位移、防护、净化、控制等通用 answer。槽位有限和替换成本让铭文成为 run 内决策，而不是开局默认完整工具包。

### 2.2 业界经典原则

本改造采用固定的行业通用原则：

1. **稀缺点数制造身份**：同一阶段不能拿满所有答案，玩家才会形成“这局走哪条路线”的判断。
2. **前置关系制造承诺**：高阶技能必须要求低阶技能投入，避免玩家只拿每棵树的最优单点。
3. **断点必须改变玩法**：rank 2/4 的断点要改变范围、资源、状态、冷却、触发条件、连携，不只提高伤害百分比。
4. **开局保底不等于开局满配**：起始包保证玩家能活下来，但必须留下清晰的成长空位。
5. **商店必须卖选择，不卖无效项**：满槽铭文购买必须进入替换比较；直接拒绝购买会把经济系统变成负资产。

## 3. 当前项目事实

### 3.1 当前职业技能数量

数据来源：

- `game/src/main/resources/data/professions/index.yaml`
- `game/src/main/resources/data/talents/index.yaml`

| 职业 | 阶层 | 职业树数量 | 树内技能总数 | 树内分布 | 当前 startingTalents | 当前 1 级自动可获得技能 | 最高 unlockLevel | 有前置节点数 |
| --- | --- | ---: | ---: | --- | ---: | ---: | ---: | ---: |
| `vanguard` | BASE | 3 | 16 | `vanguard_arms:6`, `vanguard_shield:5`, `vanguard_warcry:5` | 4 | 4 | 6 | 4 |
| `arcanist` | BASE | 3 | 16 | `arcanist_flame:6`, `arcanist_frost:5`, `arcanist_arcane:5` | 4 | 4 | 6 | 5 |
| `rogue` | BASE | 3 | 16 | `rogue_assassination:6`, `rogue_subtlety:5`, `rogue_agility:5` | 4 | 4 | 6 | 5 |
| `templar` | BASE | 3 | 16 | `templar_smite:6`, `templar_grace:5`, `templar_faith:5` | 4 | 4 | 6 | 6 |
| `berserker` | ADVANCED / DEV_UNLOCKED | 3 | 12 | `berserker_wrath:4`, `berserker_ruin:4`, `berserker_bloodwar:4` | 4 | 2 | 8 | 0 |
| `spellblade` | ADVANCED / DEV_UNLOCKED | 3 | 12 | `spellblade_enchanted_blade:4`, `spellblade_elemental_flux:4`, `spellblade_battle_spell:4` | 4 | 3 | 7 | 0 |
| `shadowblade` | ADVANCED / frozen | 3 | 0 | 占位树，无节点 | 0 | 0 | 0 | 0 |
| `warden` | ADVANCED / frozen | 3 | 0 | 占位树，无节点 | 0 | 0 | 0 | 0 |

结论固定如下：

1. 四个基础职业不新增技能。每职业 `16` 个技能足够表达三条树。
2. `berserker` 和 `spellblade` 在 Phase4 只补前置与断点，不扩成完整 16 技能职业。
3. `shadowblade` 和 `warden` 继续冻结，不在本改造中补内容。
4. 职业树感不足的根因是运行时自动获得和 UI 选择结构，不是 YAML 节点数量。

PR01 实现后，`vanguard / arcanist / rogue / templar / berserker / spellblade` 的 `startingTalents` 已固定为 `3`，`unlockLevel` 不再自动 materialize 非 starter rank 1；`shadowblade / warden` 仍保持 frozen 占位且 starter 为空。

### 3.2 当前实现偏差

| 偏差 | 证据 |
| --- | --- |
| `TalentProgression.unlockedTalentIds` 根据职业树与 `unlockLevel` 返回节点 | `game/src/main/kotlin/com/ktome/game/TalentProgression.kt` |
| `syncUnlockedPlayerTalents` 对新解锁 talent 执行 `putIfAbsent(talentId, 1)` | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` |
| `TalentAllocationPlanner` 的当前使用方式只覆盖已存在于 `TalentLoadout.talentLevels` 的技能 rank 提升 | `core/src/main/kotlin/com/ktome/core/talent/TalentAllocationDraft.kt`、`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` |
| 玩家职业行动槽固定为 4，当前基础职业开局 `startingTalents=4`，直接填满行动槽 | `game/src/main/kotlin/com/ktome/game/GameContracts.kt`、`game/src/main/resources/data/professions/index.yaml` |
| `ensurePlayerInscriptions` 开局补满 `healing_light / phase_door / iron_shield / purge` | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` |
| `InscriptionManager.canEquip` 满 4 槽直接返回 false | `core/src/main/kotlin/com/ktome/core/inscription/InscriptionManager.kt` |
| 商店已有 inscription offer，但满槽后无法产生购买/替换决策 | `game/src/main/resources/data/shops/index.yaml` |

## 4. 职业树改造目标

### 4.1 玩家体验目标

1. 玩家进入第 1 层时只有完整生存底线，没有完整构筑。
2. 玩家在前 10 分钟至少做出一次“学习新技能还是强化旧技能”的选择。
3. 玩家在前 30 分钟至少形成一次树路线承诺：主攻树投入达到 3 点以上，辅树投入达到 1 点以上。
4. 玩家能从 UI 直接看见：已学习、可学习、未满足等级、未满足前置、未满足树投入。
5. 每个基础职业在 level 6 前都能形成两种明确 build 方向，不依赖装备掉落才成立。

### 4.2 系统目标

1. `TalentProgression` 输出学习候选，不输出已学习事实。
2. `TalentLoadout.talentLevels` 只表示已学习技能及其 rank。
3. `unlockLevel` 只控制候选出现，不触发自动 rank 1。
4. `TalentAllocationDraft` 同时支持 rank 0 -> rank 1 的学习和 rank N -> N+1 的升级。
5. 职业树 UI 展示所有本职业树节点，不再只展示 loadout 中已有技能。
6. long-run 与 `phase4Report` 记录树选择事件，避免再次出现“机制存在但体验不可证”的状态。

## 5. 职业树规则模型

### 5.1 Talent 状态定义

每个职业树节点在运行时只有四种状态：

| 状态 | 判定 | 行为 |
| --- | --- | --- |
| `LOCKED` | 等级、前置 rank、树投入任一条件未满足 | UI 显示锁定原因，不能投入点数 |
| `LEARNABLE` | 条件满足，`TalentLoadout.talentLevels` 中没有该 talent | 消耗 1 点职业天赋点后变为 rank 1 |
| `LEARNED_RESERVE` | rank >= 1，未绑定到 1~4 职业行动槽 | 被动立即生效；主动技能不显示在热键栏，保留在 reserve |
| `LEARNED_ACTIVE` | rank >= 1，绑定到 1~4 职业行动槽 | 主动技能可用；被动继续按 rank 生效 |

固定规则：

1. `LOCKED` 节点不能出现在可投入列表顶部，必须保留在树 UI 中展示原因。
2. `LEARNABLE` 节点默认 rank 为 0，不进入 `loadout.talentLevels`。
3. 学习 `LEARNABLE` 节点花费 1 点职业天赋点，并写入 `loadout.talentLevels[talentId] = 1`。
4. 新学习的主动技能在存在空职业行动槽时自动绑定到第一个空槽。
5. 四个职业行动槽已满时，新学习主动技能进入 reserve，玩家通过现有 slot assignment 指令替换热键。
6. 被动技能学习后不占用主动槽。

### 5.2 起始技能

`startingTalents` 从“开局四槽满配”改成“开局三技能保底”。四个基础职业固定如下：

| 职业 | 保留 startingTalents | 移出开局、进入显式 LEARNABLE 池 | 目的 |
| --- | --- | --- | --- |
| `vanguard` | `power_strike`, `shield_bash`, `guard_stance` | `war_cry` | 保留单体输出、盾击控制、防御架势；把团队/范围节奏留给升级选择 |
| `arcanist` | `fireball`, `blink`, `arcane_shield` | `ice_bolt` | 保留远程输出、位移、防护；把冰控路线留给升级选择 |
| `rogue` | `backstab`, `stealth`, `roll` | `poison_blade` | 保留爆发、潜行、位移；把毒系持续伤害留给升级选择 |
| `templar` | `holy_strike`, `holy_light`, `holy_shield` | `devotion` | 保留近战、治疗、防护；把信仰续航路线留给升级选择 |

进阶 DEV_UNLOCKED 职业固定如下：

| 职业 | 保留 startingTalents | 移出开局、进入显式 LEARNABLE 池 | 目的 |
| --- | --- | --- | --- |
| `berserker` | `blood_rush`, `savage_hew`, `kill_frenzy` | `reckless_slam` | 保留接战、主输出、击杀滚动；把范围/风险动作留给升级选择 |
| `spellblade` | `arcane_edge`, `mana_lunge`, `spell_parry` | `flux_anchor` | 保留混合输出、位移接战、防御；把资源稳定器留给升级选择 |

`spellblade` 的 starter 分布固定为 `spellblade_enchanted_blade=1`、`spellblade_battle_spell=2`、`spellblade_elemental_flux=0`；`flux_anchor` 是 `spellblade_elemental_flux` 的首次升级 learnable 节点。

锁定职业固定如下：

| 职业 | 处理 |
| --- | --- |
| `shadowblade` | 保持 frozen 占位，不补技能，不进入 release 路径 |
| `warden` | 保持 frozen 占位，不补技能，不进入 release 路径 |

### 5.3 学习候选规则

`TalentProgression.unlockedTalentIds` 的语义改名为 `learnableTalentIds`。函数返回“玩家当前等级与前置下允许投入点数的候选”，不再表达“玩家已经拥有这些技能”。

固定判定顺序：

1. 读取职业的 `talentTrees`。
2. 按树内 `nodes` 顺序建立节点列表。
3. 对每个节点检查 `unlockLevel <= player.level`。
4. 对每个节点检查 `requirements` 中的前置 talent rank。
5. 对每个节点检查树内已投入点数门槛。
6. 满足全部条件的节点进入 `LEARNABLE`。
7. 已存在于 `TalentLoadout.talentLevels` 的节点进入 `LEARNED_RESERVE` 或 `LEARNED_ACTIVE`。

树内已投入点数定义：

```text
treeInvestedPoints = sum(rank for learned talents whose talentId is in tree.nodes)
```

`startingTalents` 的 rank 计入树内投入点数。

### 5.4 树层级与门槛

每条基础职业树按固定三层处理：

| 层级 | 位置 | 门槛 | 作用 |
| --- | --- | --- | --- |
| Tier 1 | 树内第 1、2 个节点 | `unlockLevel <= 2` | 建立树主题，形成第一轮路线选择 |
| Tier 2 | 树内第 3、4 个节点 | `unlockLevel >= 3`，同树投入 `>= 2`，其他任一职业树投入 `>= 1`，指定前置 rank `>= 2` | 形成路线承诺，提供控制、AOE、资源、机动、增伤中的一个 build pivot |
| Tier 3 | 树内第 5、6 个节点 | `unlockLevel >= 5`，同树投入 `>= 5`，指定前置 rank `>= 3` | 提供 capstone 或 build-defining payoff |

固定数据要求：

1. 每条非 frozen 职业树的每个 Tier 2 节点都必须要求指定前置 rank `>= 2`。
2. 每条非 frozen 职业树的每个 Tier 3 节点都必须要求指定前置 rank `>= 3`。
3. 每个基础职业至少 2 条树拥有 Tier 3 payoff。
4. Tier 3 payoff 必须改变玩法，不得只有线性数值。
5. 进阶 DEV_UNLOCKED 职业不扩数量，但必须补齐 Tier 2、Tier 3 前置关系。
6. `berserker / spellblade` 保持 4 节点 compact tree：第 3 个节点按 Tier 2 校验，第 4 个节点按 Tier 3 校验；每棵 compact tree 仍必须满足 Tier 2 `minRank >= 2` 与 Tier 3 `minRank >= 3` 前置校验。

PR01 data review 结论：

| 职业 | 最小 Tier 3 路线所需职业点数 | 等级上限内可获得职业点数 | 结论 |
| --- | ---: | ---: | --- |
| `vanguard` | 5 | 19 | 满足至少一条 Tier 3 路线 |
| `arcanist` | 5 | 19 | 满足至少一条 Tier 3 路线 |
| `rogue` | 5 | 19 | 满足至少一条 Tier 3 路线 |
| `templar` | 5 | 19 | 满足至少一条 Tier 3 路线 |
| `berserker` | 5 | 19 | report-only compact tree 满足至少一条 Tier 3 路线 |
| `spellblade` | 5 | 19 | report-only compact tree 满足至少一条 Tier 3 路线 |

最小路线按 `同树 starter rank 1 -> 前置 rank 2 -> Tier 2 rank 3 -> Tier 3` 计算，并包含 Tier 2 所需的副树 `>= 1` 投入；`ExperienceSystem.talentPointsGrantedForLevel(level) = 1` 且 level cap 为 `20`，因此四个 BASE 职业有充足点数完成至少一条 Tier 3 路线。`berserker / spellblade` 只作为 ADVANCED report-only compact tree 可行性记录，不进入 release-facing blocking 分母。

### 5.5 点数消耗

沿用 `ExperienceSystem.talentPointsGrantedForLevel(level) = 1` 的节奏。

固定消耗：

| 行为 | 消耗 |
| --- | ---: |
| 学习 `LEARNABLE` 技能到 rank 1 | 1 职业天赋点 |
| 已学习技能 rank +1 | 1 职业天赋点 |
| 调整主动槽位 | 0 职业天赋点 |
| 取消未确认 draft | 0 职业天赋点 |
| 非战斗 respec 回滚到本次 draft 前 | 0 职业天赋点 |

确认规则：

1. Draft 只能在非战斗中确认。
2. Draft 期间 UI 预览 rank、消耗点数、剩余点数、下一个断点。
3. Confirm 后写入 `TalentLoadout.talentLevels` 并扣除点数。
4. Confirm 后空行动槽自动绑定新学习主动技能。
5. Confirm 后不自动学习同等级解锁的其他技能。

### 5.6 Rank 断点

每个基础职业至少满足以下断点密度：

| 范围 | 固定要求 |
| --- | --- |
| 每个基础职业 | 至少 6 个技能拥有 rank 2 或 rank 4 玩法断点 |
| 每条基础职业树 | 至少 1 个技能拥有玩法断点 |
| 每个基础职业 Tier 3 节点 | 必须有进入战斗循环的效果变化 |

断点效果只接受以下类型：

1. 新增状态效果。
2. 改变目标形状、范围、穿透、连锁、区域持续。
3. 改变资源曲线。
4. 改变冷却、即时性、回合成本。
5. 与铭文、装备 affix、职业资源产生明确联动。
6. 触发新的防御、位移、反击、处决、控制窗口。

不接受的断点：

1. 只把伤害从 `X` 提到 `X + n`。
2. 只把护盾从 `X` 提到 `X + n`。
3. 只把命中、暴击、防御线性提高。

## 6. 职业树 UI 与输入流程

### 6.1 UI 布局

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
- Learn war_cry: costs 1 point, unlocks intimidation at rank 2.
- Remaining points after confirm: 1.
```

固定显示字段：

1. 树名。
2. 节点状态：`Active`、`Reserve`、`Learn`、`Locked`。
3. 当前 rank。
4. 下一个 rank 的效果变化。
5. 下一个玩法断点。
6. 锁定原因。
7. 当前 draft 的点数消耗和剩余点数。

### 6.2 输入流程

固定流程：

1. 玩家打开 Talent UI。
2. UI 展示三条职业树全量节点。
3. 玩家选择 `LEARNABLE` 或 `LEARNED` 节点。
4. 系统创建 `TalentAllocationDraft`。
5. UI 立即刷新预览：点数、rank、断点、锁定变化。
6. 玩家确认 draft。
7. 系统写入 `TalentLoadout.talentLevels`，扣除职业点数，更新主动槽。
8. 日志输出 token 化事件：学习、升级、断点触发、树投入变化。

拒绝规则：

1. 战斗中不能确认 draft。
2. 点数不足不能创建 draft。
3. 前置不足不能创建 draft。
4. 树内投入不足不能创建 draft。
5. max rank 不能创建 draft。
6. 不同 owner 的 profession/race draft 不能混合。

## 7. 职业树实现改造流程

### 7.1 生产代码改造顺序

固定按以下顺序执行：

1. **改 `TalentProgression` 语义**
   - 新增 `learnableTalentIds(...)`。
   - 保留旧函数名一轮迁移时只委托新函数，所有调用点改读新语义。
   - 函数不再被 `syncUnlockedPlayerTalents` 当成自动学习清单。

2. **改玩家 loadout 初始化**
   - `EntityFactory` 只把 `startingTalents` 写入 `TalentLoadout.talentLevels`。
   - 四个基础职业 `startingTalents` 改为 3 个。
   - 初始主动槽只填充已学习 starter。
   - 第 4 个主动槽允许为空。

3. **停止自动学习**
   - `FoundationGameSession.syncUnlockedPlayerTalents` 只做 starter 缺失修复和非法 talent 清理。
   - 等级提升不再执行 `putIfAbsent(talentId, 1)`。
   - save/load canonicalize 不自动补齐 level 可学技能。

4. **扩展 allocation candidate**
   - Talent UI 的候选列表改为 `learned + learnable + locked`。
   - `minimumTalentRanks` 对 starter 返回 `1`，对 learnable rank 0 节点返回 `0`。
   - `TalentAllocationPlanner.preview` 支持 rank 0 -> 1 的点数消耗。
   - `TalentPrerequisiteValidator` 对 candidate ranks 判定前置。

5. **增加树投入门槛**
   - Data loader 读取树内节点顺序。
   - UI/runtime 根据树节点顺序派生 Tier。
   - 对 Tier 2/Tier 3 加树投入判定。
   - 锁定原因进入 token log 和 UI。

6. **更新技能数据**
   - 改基础职业 `startingTalents`。
   - 给基础职业和 DEV_UNLOCKED 进阶职业补前置。
   - 补足 rank 2/rank 4 玩法断点。
   - 不新增基础职业技能。

7. **接入 report**
   - `RunSummary` 增加职业树选择事件摘要。
   - long-run report 增加树投入、学习次数、断点触发、主动槽替换统计。
   - `phase4Report` 增加 blocking 指标。

### 7.2 必须修改的文件范围

| 范围 | 文件 |
| --- | --- |
| 职业/技能数据 | `game/src/main/resources/data/professions/index.yaml`, `game/src/main/resources/data/talents/index.yaml` |
| 进度语义 | `game/src/main/kotlin/com/ktome/game/TalentProgression.kt` |
| session 接线 | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` |
| 初始化 | `game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt` |
| allocation | `core/src/main/kotlin/com/ktome/core/talent/TalentAllocationDraft.kt` |
| UI snapshot | `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`, `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt` |
| client 输入/展示 | `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`, `client/src/main/kotlin/com/ktome/client/ui/talent/**` |
| harness/report | `game/src/main/kotlin/com/ktome/game/harness/**`, `tools/src/main/kotlin/com/ktome/tools/**` |

### 7.3 不允许的实现方式

1. 不允许把 `unlockLevel` 改名后继续自动写 rank 1。
2. 不允许通过降低敌人强度掩盖起始技能减少的影响。
3. 不允许新增一套 client-only 技能树状态。
4. 不允许在 tools 中复制 runtime 的树投入判定。
5. 不允许让 report 只统计“解锁过”，必须统计“玩家学习并投入过”。
6. 不允许把基础职业扩成 20+ 技能来制造表面深度。

## 8. 铭文/符文改造目标

### 8.1 玩家体验目标

1. 玩家开局拥有 2 个安全铭文，能处理基础恢复与一个职业弱点。
2. 玩家在 run 内通过商店、Boss、隐藏奖励获得第 3、4 个铭文。
3. 玩家满槽后购买铭文会进入替换比较，看到旧铭文和新铭文的冷却、类别、效果、限制。
4. 玩家必须在“恢复、位移、防护、净化、控制、爆发”之间做取舍。
5. 铭文变化进入 run summary，成为复盘和再开一局的动机来源。

### 8.2 起始铭文

起始铭文固定如下：

| 职业 | 起始铭文 | 留出的构筑缺口 |
| --- | --- | --- |
| `vanguard` | `healing_light`, `iron_shield` | 位移、净化 |
| `arcanist` | `healing_light`, `phase_door` | 稳定防护、净化 |
| `rogue` | `healing_light`, `phase_door` | 防护、净化、控制 |
| `templar` | `healing_light`, `purge` | 位移、爆发防护 |
| `berserker` | `healing_light`, `iron_shield` | 净化、位移 |
| `spellblade` | `healing_light`, `phase_door` | 净化、稳定防护 |

固定规则：

1. 开局只写入 2 个 `InscriptionSlot`。
2. 热键从 `5` 开始连续分配。
3. 第 3、4 槽不是永久解锁系统，只是 run 内装备空位。
4. 最大槽数继续使用 `MAX_INSCRIPTION_SLOTS = 4`。
5. 单类别上限继续使用 `MAX_INSCRIPTION_PER_CATEGORY = 2`。

## 9. 铭文安装与替换规则

### 9.1 安装规则

当玩家获得 inscription offer 时，系统按固定流程处理：

1. 读取当前 `InscriptionLoadout.slots`。
2. 如果槽位数 `< 4`，检查类别上限。
3. 类别合法时安装到第一个空热键。
4. 类别不合法时拒绝，并展示类别上限原因。
5. 安装成功后写入 run event：`INSCRIPTION_INSTALLED`。

### 9.2 替换规则

当玩家满 4 槽购买 inscription offer 时，系统按固定流程处理：

1. 打开替换界面。
2. 展示候选铭文与四个现有铭文。
3. 玩家选择目标槽。
4. 系统临时移除目标槽中的旧铭文，再检查候选铭文类别上限。
5. 检查通过后用候选铭文覆盖目标槽，热键保持不变。
6. 旧铭文销毁，不进入背包，不返还金币。
7. 替换成功后写入 run event：`INSCRIPTION_REPLACED`。

拒绝规则：

1. 候选铭文不存在时拒绝。
2. 目标槽不存在时拒绝。
3. 替换后类别上限超过 2 时拒绝。
4. 金币不足时拒绝。
5. 同一铭文替换自己时拒绝，并提示已装备。

### 9.3 商店流程

商店 purchase flow 固定如下：

1. 玩家选择 inscription offer。
2. 商店先做金币检查。
3. 商店调用 inscription install/replace 预检。
4. 槽位未满时直接安装。
5. 槽位已满时进入替换界面。
6. 替换成功后扣金币。
7. 安装成功后扣金币。
8. 任一步失败都不扣金币。
9. 成功后刷新 UI、日志、run summary。

### 9.4 铭文 UI 字段

安装/替换界面固定展示：

1. 名称。
2. 类别。
3. 冷却。
4. 作用标签：`heal`, `mobility`, `shield`, `cleanse`, `control`, `damage`, `resource`。
5. 资源消耗。
6. 当前装备数量。
7. 替换后类别数量。
8. 与被替换铭文的核心差异。

不展示普通敌人 intent，不从 AI 类型推断铭文价值。

## 10. 铭文实现改造流程

固定按以下顺序执行：

1. **改开局装配**
   - `FoundationGameSession.ensurePlayerInscriptions` 不再补满四槽。
   - 根据职业 id 写入 2 个起始铭文。
   - 已有 save 缺铭文时只补本职业起始 2 个。

2. **改 `InscriptionManager`**
   - 保留 `canEquip` 用于空槽安装。
   - 新增 `canReplace(loadout, equippedDefinitions, candidate, targetHotkey)`。
   - 新增 `replace(loadout, equippedDefinitions, candidate, targetHotkey)`。
   - `replace` 保持热键不变。

3. **改 shop purchase**
   - `loadout.slots.size < 4` 时走 install。
   - `loadout.slots.size == 4` 时返回 `REQUIRES_REPLACEMENT_TARGET`。
   - client 收到该状态后打开替换界面。
   - 替换确认后重新提交 purchase command，附带 target hotkey。

4. **改 snapshot/UI**
   - `RenderSnapshot` 增加 inscription replace pending 状态。
   - client 展示替换比较。
   - 日志使用 token，不写死本地化文案。

5. **改 run summary/report**
   - 记录安装次数、替换次数、购买被拒原因、终局铭文组合。
   - `phase4Report` 增加铭文构筑指标。

## 11. Report 与验收指标

### 11.1 新增 blocking 指标

| 指标 | 阈值 | 解释 |
| --- | ---: | --- |
| `starterProfessionTalentMaxCount` | `<= 3` | 基础职业开局不能填满 4 个职业行动槽 |
| `autoLearnedNonStarterTalentCount` | `0` | 非 starter 技能不得因等级自动 rank 1 |
| `learnedTalentChoiceEventRate` | `>= 90% terminal runs` | 终局 run 必须发生至少一次学习新技能事件 |
| `multiTreeInvestmentRate` | `>= 75% terminal runs` | 终局 run 至少两条职业树有投入 |
| `breakpointChoiceEventRate` | `>= 75% terminal runs` | 终局 run 至少触发一次玩法断点选择 |
| `starterInscriptionMaxCount` | `<= 2` | 开局铭文不能满槽 |
| `inscriptionInstallOrReplaceRate` | `>= 50% terminal runs` | 终局 run 至少一半发生安装或替换 |
| `fullSlotInscriptionPurchaseRejectedCount` | `0` | 满槽购买不得因缺替换流程被直接拒绝 |

### 11.2 Supporting 指标

| 指标 | 用途 |
| --- | --- |
| `talentTreePrimaryInvestmentDistribution` | 看每职业主树是否被单一最优解压扁 |
| `talentReserveSwapCount` | 看 active slot 选择是否发生 |
| `rankBreakpointAdoptionByTalent` | 看断点是否真的被采用 |
| `terminalInscriptionLoadoutDiversity` | 看终局铭文组合是否集中 |
| `inscriptionCategoryCountDistribution` | 看类别上限是否产生合理取舍 |
| `shopInscriptionOfferConversionRate` | 看商店是否成为构筑入口 |

## 12. 测试与验证入口

### 12.1 单元测试

必须新增或更新：

| 测试 | 覆盖 |
| --- | --- |
| `TalentProgressionTest` | level 达标后节点进入 learnable，不自动 learned |
| `TalentAllocationPlannerTest` | rank 0 -> 1 学习消耗 1 点；rank 1 -> 2 升级消耗 1 点 |
| `TalentPrerequisiteValidatorTest` | 前置 rank 和树投入不足时拒绝 |
| `InscriptionManagerTest` | 空槽安装、满槽替换、类别上限、热键保持 |
| `ShopPurchaseFlowTest` | 满槽 inscription offer 进入替换，不直接拒绝 |

### 12.2 Harness

必须更新：

| Harness | 验证 |
| --- | --- |
| `soloClearLab` | 起始技能减少后四基础职业仍可通关 |
| `longRunLab` | 树选择、断点、铭文安装/替换进入 run summary |
| `phase4Report` | 新 blocking 指标进入 canonical report |
| `clientSmoke` | Talent UI 与铭文替换界面不会破坏输入流 |

### 12.3 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :core:test :game:test :client:clientSmoke longRunLab soloClearLab reportPhase4
```

本补充文档只定义设计，不声称上述命令已运行。

## 13. 改造完成定义

该改造完成必须同时满足：

1. 四个基础职业开局职业技能数均为 `3`。
2. 角色升级不再自动获得非 starter 职业技能。
3. Talent UI 显示 locked/learnable/learned/reserve/active 全状态。
4. 学习新技能和提升 rank 共用职业天赋点。
5. Tier 2/Tier 3 受 `unlockLevel + 前置 rank + 树投入` 约束。
6. 四个基础职业不新增技能数量。
7. `berserker` 和 `spellblade` 补齐前置，不扩内容规模。
8. 开局铭文数为 `2`。
9. 满槽 inscription purchase 进入替换界面。
10. 满槽购买因缺替换流程直接失败的次数为 `0`。
11. long-run report 能看到 talent choice event 与 inscription change event。
12. `phase4Report` 不再只通过 capstone/non-weapon payoff 推断构筑，必须直接统计职业树和铭文选择。

## 14. 对 Phase4/Phase5 边界的固定判断

1. 本改造属于 Phase4 进入 Phase5 前必须处理的 P0。
2. Phase5 只继续消费本改造产生的 run history、build summary、tactical AI 输入，不再补建职业树或铭文购买系统。
3. 局外奖励不做永久数值成长。职业解锁、run history、最佳 build 复盘、死亡分析和图鉴记录属于允许范围。
4. 当前版本的“再来一局动力不足”优先通过 run 内构筑选择、铭文替换、capstone/non-weapon payoff 修复，不通过永久属性奖励修复。
