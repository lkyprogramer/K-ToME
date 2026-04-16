# phase4 深度审查报告（Part 2）

## 4. 当前阶段玩法体验总评

### 4.1 总体判断

如果只从系统存在性看，当前版本已经具备“进地图、打怪、拿奖励、成长、遇到隐藏内容、进秘密区、读到 content pack”的 Phase4 轮廓；但如果从玩家持续投入的角度看，这个版本仍然是 **半闭环**：

1. 正反馈能启动，但中段经常掉速。
2. 成长能感知，但分化还不够大。
3. 奖励能出现，但不总能强力改写下一步决策。
4. 探索有未知感，但主线路径中后段会被目标点/交互点压扁。
5. 报表证明“系统存在”，却还没有完全证明“系统持续好玩”。

因此，本阶段更准确的结论不是“已经足够耐玩”，而是：**已经形成初步耐玩雏形，但尚未把核心驱动力打磨到稳定成立。**

### 4.2 核心循环

#### 当前循环长什么样

当前可重构出的主循环大致是：

1. 进入 zone，沿路径探索。
2. 遭遇若干战斗、事件、秘密入口提示或搜索点。
3. 获得掉落、里程碑奖励、秘密奖励或关键交互结果。
4. 更新装备与资源状态，推进职业 build。
5. 进入下一层 / 下一区域，继续重复。

这个循环“形式上”已经齐全，但**节奏强度分布不均**。

#### 最弱的环节：中后段关键路径的战斗与探索密度

最突出的问题不是起步阶段，而是中后段几张关键地图把“探索->遭遇->抉择->奖励”的密度压得过低：

- [game/src/main/resources/data/objectives/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/objectives/index.yaml) 中，多处 objective anchor 直接放在 `player_start` 或 `stairs_up`：
  - `greenwood_signal_hunt` 的 `trail_cache` 在 `player_start`
  - `grey_gate_seal_rite` 的 `seal_cache` 在 `player_start`
  - `underground_river_crossing` 的 `crystal_cache_chest` 在 `stairs_up`
- [game/src/main/kotlin/com/ktome/game/GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt) 里关键路径地图的怪物数量非常低：
  - `grey_gate_depths -> 0`
  - `abyssal_temple -> 0`
  - `underground_river -> 1`
- [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json) 的 zone 诊断直接印证节奏塌陷：
  - `greenwood_fringe.avgObjectiveAcquireTurn = 2.0`
  - `grey_gate_depths.avgObjectiveAcquireTurn = 2.0`
  - `underground_river.avgObjectiveAcquireTurn = 2.0`
  - `abyssal_temple.avgEnemyTurns = 0.0`
  - `abyssal_temple.avgVisibleHostileTurnCount = 0.0`
- [tools/build/reports/verification/phase4/report-phase4-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/verification/phase4/report-phase4-summary.md) 也明确提到 `abyssal_temple` 因 objective/pressure-driven path 压缩了 terrain combat denominator。

这意味着玩家在若干关键节点不是“通过一段有风险、有收益、有判断的推进路径达成目标”，而是“快速触达交互点，接着离开”。主循环从 Roguelike 的探索-战斗驱动，退化成了半线性流程推进。

#### 为什么这会直接伤到好玩度

1. 玩家的注意力没有被持续拉在“我怎么处理眼前空间与敌人”上，而是被拉到“我怎样更快触发目标点”。
2. Terrain、elite mutation、隐藏入口、路线价值这些本该自然嵌在探索流里的系统，没有足够多的战斗与空间张力去放大它们。
3. 当 objective 获取 turn 数稳定压到 2 左右，很多地图在玩家记忆里就不是“这层我经历了什么”，而是“这层我又点了一下东西然后走了”。

#### 结论

当前核心循环不是不存在，而是**中段存在显著空洞**。这个空洞不是靠更多内容量就能自动补齐，必须先修关键路径 pacing。

### 4.3 战斗体验

#### 当前战斗的基础面

正面的部分：

1. 四职业基础身份已经可辨认。
2. Terrain、elite mutation、boss variant、loot affix、资源与职业装备倾向都已经进入系统。
3. 从 Phase3 继承下来的规则语义与 combat trace 基础仍在，所以战斗不是瞎打，底层合同是稳的。

负面的部分：

1. 战斗出现频率在关键地图不稳定，导致系统再丰富也缺乏足够多的上场机会。
2. Boss variant 的“存在”没有稳定转化成“体感不同”。
3. 若 reward/build 最终仍吸附到相似武器与相似决策，战斗中的流派差异也会被压扁。

#### Boss 变体的问题不是“没做”，而是“玩家感觉不到值”

[tools/build/reports/phase4/whitebox/boss/boss-harness.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/boss/boss-harness.md) 显示：

1. `molten_glass` 的 base/variant 选中动作与 trace hash 一致。
2. `abyssal_eclipse` 的 base/variant 同样缺少足够动作差异。
3. `abyssal_eclipse` 甚至出现 `preferredTerrain=[OIL]`、`bossTerrain=none`、`preferenceAvailable=false`、`preferenceApplied=false` 的情况。

这说明当前 boss variant 更像“schema 已建立、测试已能过”，却还不是“玩家会因为变体不同而调整打法”的设计完成态。

#### 决策密度与反馈强度的矛盾

当前战斗的问题不是纯粹“太简单”或“太难”，而是：

1. 有些地图给不了足够多战斗回合去承接系统深度。
2. 有些战斗差异存在于数据层，却没有被足够清晰地反馈到玩家层。
3. 变体、terrain、mutation、本应形成“判断->执行->反馈”的回路，但目前更常处于“知道系统在那儿，却不一定被迫认真对待”的状态。

#### 结论

当前战斗可以支撑 Phase4 的系统展示，但还不足以支撑“靠战斗本身就能让玩家持续兴奋”。它更像是**有框架、有差异点，但差异密度与体感强度还没到位**。

### 4.4 成长与构筑

#### 成长感：存在，但偏向装备数值替换

当前成长并非没有提升感。玩家会通过：

1. 里程碑奖励与掉落更新主武器/副手/护甲。
2. 职业资源与装备标签逐渐贴合。
3. affix/unique/artifact 带来若干额外语义。

但问题在于，这种成长还不够频繁地变成“玩法改写”，更多时候仍是“我又把当前最好用的那把武器换得更强了一点”。

#### 构筑分化不足的直接证据

[build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json) 给出了很清楚的信号：

1. `terminalWeaponBaseDiversity = 3`
2. `crossProfessionTopWeaponDominance = 0.5`
3. `professionTerminalWeaponDistribution` 中：
   - `arcanist -> arcane_staff`
   - `rogue -> hunter_bow`
   - `templar -> long_sword`
   - `vanguard -> long_sword`

也就是说，四职业里有两个职业在终局基底上仍收敛到同一把 `long_sword`。这不是“偶然会发生”，而是报表已经把它识别成结构性收敛。

再看 [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)：

- `long_sword` 同时带有 `[item, weapon, melee, frontline, guard, discipline]`

这让它自然成为多个前排职业的共同吸附点。而 [game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt) 里的 build-aware 权重只做到：

1. weak match 大约 `1.15x`
2. strong match 大约 `1.35x`
3. anti-collapse 主要只惩罚 `dominant_risk` 且只在 mismatch 情况下生效

这套力度不足以真正拉开职业终局差异。

#### “假选择”风险

当表面上掉落池很多、affix 很多、build tag 很多，但最终最优吸附仍高度收敛时，玩家感受到的不是“丰富”，而是“系统很复杂，但其实答案差不多”。这正是 Roguelike 构筑体验里最危险的一类负资产：

1. 增加认知成本。
2. 没增加决策收益。
3. 反而削弱了“这局我想试另一套”的动力。

#### 高稀有度身份的问题，比“完全没做”更微妙

Claude 报告在这里给出的判断方向有价值，但原结论说得过头了。当前 unique / artifact 并不是“没有玩法身份”：

1. [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml) 里的 special item 已经带有 `OnHitStatusProc`、`OnKillResourceRestore`、`TerrainAffinityBonus`、`ConditionalStatBonus`、`DamageTypeBonus`、`HpRegenPerTurn` 等 passive。
2. 但 [core/src/main/kotlin/com/ktome/core/item/ItemModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/item/ItemModels.kt) 的 `SpecialItemTemplate` 仍只提供 `fixedAffixIds / fixedMaterialId` 这一级模板约束，没有进一步的 special-tier 专属合同。

所以当前真正的问题不是“高稀有度掉落没有 identity”，而是**它们的 identity 主要仍由通用 passive 词汇 + 固定组合来承担**。玩家会记得它“比普通装备特殊”，但未必会因为它而真正改写整局 build 逻辑。

#### 职业终局锚点缺位，是成长驱动力不够强的另一层原因

从用户体验角度看，当前 Phase4 还缺一层非常关键的“我为什么继续打这局”的中后段目标：**职业终局锚点装备**。

这里不应做成“每职业只有一把纯低概率掉落的终极武器”，那样会同时踩中三个坑：

1. 大部分玩家一局里根本见不到，提升的是想象，不是实际体验。
2. 每职业只有一个标准答案，会进一步压扁 build 分歧。
3. 全都做成武器，会无视当前项目里副手/护甲/神器已经承担职业 identity 的事实。

更合理的落法，是把它设计成 **每个基础职业 1 到 2 个终局锚点装备，其中至少 1 个不是武器**，并优先复用现有 special item 作为种子：

1. `vanguard`
   - 武器锚点：`artifact_forge_oath` / `unique_quenchbreaker_maul`
   - 防御锚点：`unique_furnace_plate`
   - 主题：`oil / frontline / guard / retaliation`
2. `arcanist`
   - 武器锚点：`artifact_river_echo`
   - 副手锚点：`unique_deepcurrent_lens`
   - 主题：`mana / lightning / water positioning`
3. `rogue`
   - 武器锚点：`artifact_heartroot_gambit` / `unique_thornpath_crook`
   - 备选武器锚点：`unique_briarbound_bow`
   - 主题：`stealth / marked / crit opener`
4. `templar`
   - 副手锚点：`artifact_eclipsed_relic` / `unique_voidlit_seal`
   - 护甲锚点：`unique_vesper_chainmail`
   - 主题：`holy bulwark / crisis sustain / bane control`

这些锚点的获取方式也不应纯靠随机，而应采用“**主路径可追求 + 低概率直掉作捷径**”：

1. 主获取路径：
   - secret zone / hidden event 的 `guaranteedContent`
   - 条件 boss 掉落
   - finale `abyssal_heart.reward` 的职业终局奖励
2. 捷径路径：
   - 少量 eligible elite / boss 的低概率直掉
   - 但必须带 unseen bias 或保底，不做纯黑箱彩票

当前仓库里已经具备这套方案的大部分落脚点：

1. [game/src/main/resources/data/secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml) 已有 `rewardProfileId + guaranteedContent`
2. [game/src/main/resources/data/events/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/events/index.yaml) 已有 `LOOT_PROFILE / GRANT_BUFF / TRIGGER_ENCOUNTER`
3. [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml) 已有 `lootProfileOverride`
4. [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml) 已有 zone-local secret reward profile

也就是说，当前缺的不是“从零发明一套新系统”，而是**把现有 reward/secret/boss 节点重新组织成 profession capstone chase path**。

#### 结论

当前成长与构筑已经有 Phase4 该有的结构骨架，但**还没有把职业分化、build 分歧、装备选择压力真正拉开**。这是当前版本耐玩性不足的核心来源之一。

### 4.5 奖励驱动

#### 奖励系统不是稀缺，而是“有效奖励比例不够高”

从结构上看，当前奖励来源并不算少：

1. 普通掉落
2. milestone reward
3. secret reward
4. content pack / overlay 可扩路径
5. unique / artifact / affix

但 [build/reports/harness/long-run-summary.md](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-summary.md) 与 [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json) 反映出两个问题：

1. milestone reward slot 分布过于偏向 `WEAPON=93`，而 `OFF_HAND=29`、`ARMOR=2` 很低。
2. adoption 数据并不强，存在 `64 adopted / 60 notAdopted` 这类接近对半的结果。
3. affix synergy 激活很集中，`of_shadow=6` 远高于其他命中。

这说明奖励系统更像“持续发东西”，但不是“持续发真正让玩家想调整策略的东西”。

#### 奖励记忆点仍然偏少

一套真正耐玩的 reward loop，不只是让玩家更强，而是让玩家记住：

1. 我为什么这一局突然转向某种打法。
2. 我在哪一层拿到了改变局势的东西。
3. 我为什么愿意冒险去打某个隐藏内容。

当前 hidden reward、milestone reward、weapon 主导掉落，整体仍然偏向“推进资源”，而不是稳定制造“这次拿到的东西改变了这局”的记忆点。

#### 中后段 reward pool 覆盖不均，是奖励曲线变平的直接原因

[game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml) 进一步说明，当前 reward 平坦不只是“数值没调好”，而是**很多区还没进入真正的动态掉落语义**：

1. `greenwood_fringe`、`deep_iron_pit` 的 cadence/reward，以及 `underground_river.cadence`、`abyssal_temple.cadence` 已经是 `TAG_WEIGHTED`。
2. 但 `shattered_outpost`、`bandit_camp`、`elven_ruins`、`molten_core`、`grey_gate_depths`、`crystal_cavern` 的 cadence，以及 `grey_gate_depths.reward`、`underground_river.reward`、`abyssal_temple.reward`、`abyssal_heart.reward` 仍停留在 `FIXED_LIST`。

这意味着玩家到了这些区，掉落的惊喜感不是“少一点”，而是**直接从带局部身份的奖励生态，退回到固定几件物品的清单抽取**。这也是 run 后 30 分钟更容易开始变平的结构性原因。

#### 结论

奖励并非太少，而是**高价值奖励密度不够高，且奖励类型的体感权重不均衡**。它推动前进，但还不足以强力驱动“再打一局”。

### 4.6 探索与重复游玩价值

#### 优点

1. Phase4 的地图、隐藏内容、secret zone、terrain theme、content pack path，已经让“每次进入地图都完全一样”这件事不成立了。
2. 当前工作区中 `greenwood` 的 floor-aware profile 与新增 `hideout_switchback` pattern，说明设计方向正在往“更早、更自然的 replay hook”靠。
3. hidden event 与 secret zone 的存在，能为探索提供额外不确定性。

#### 问题：未知感存在，但“发现感”还没完全变成收益闭环

真正的问题不是地图没变化，而是这些变化还没有总是导向：

1. 更强的路线抉择；
2. 更明确的风险与收益交换；
3. 更有记忆点的独特奖励。

例如当前新加的 `greenwood_ambush_hideout` 虽然增加了路线变化，但仍复用 `greenwood_hidden_cache` 的 reward profile 以及相近视觉/音频身份。这样一来，玩家感受到的更多是“走法不一样了”，而不是“我遇到了另一种值得记住的秘密内容”。

#### 内容重复度问题

当前内容量并没有到“严重匮乏”的程度，真正的问题是：

1. 主线路径里几张图的流程节奏太短，没把已有内容消化成足够多的体验片段。
2. 构筑与奖励分化不够强，导致不同 run 虽然在地图细节上有变化，但最终回忆点还是容易收敛。

#### 结论

当前 Phase4 已经有“再来一局”的基础理由，但还缺一层“每局都有更鲜明的路线与回报差异”。探索是有的，新鲜感是半成立的，长期重复游玩价值还不够稳。

### 4.7 UI / 反馈 / 新手体验

#### 当前方向是对的

当前工作区里已经能看到明显的可读性补强：

1. [core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt) 新增 `uiState.frontstageReadability`
2. `recentRewards[*].detailText` 已从 session 注入到 render model
3. [client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt) 与 [client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt) 已消费这些前台信息

这说明团队已经意识到：Phase4 的问题不只是系统少，而是玩家看不清“刚刚发生了什么”和“这次奖励为什么重要”。

#### 仍然不够的地方

1. Claude 在 PR-05 深审里提到的两个问题，当前工作区其实已经被修掉：
   - `ui.hud.frontstage.title / ui.sidebar.frontstage` 已经本地化成 `Awareness / 态势感知`
   - `recentFrontstageActionCues` 已受 `FRONTSTAGE_RECENT_ACTION_CUE_CAP` 限制
2. 真正残留的问题，是 [game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt) 的 `buildRecentActionHighlights()` 仍然只是对最近两条 cue 做回放，而 [client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt) 只区分 `MUTATION / TERRAIN / ACTION` 三类。
3. 这意味着 passive trigger、search 成败、hidden reward、secret encounter 仍然会在同一个 ACTION 通道里竞争有限展位，缺的不是“有没有前台”，而是“谁该先被看见”的 source priority。
4. hidden reward 与 replay hook 的身份差异仍弱，导致信息虽然更清楚了，但记忆点并没有同步变强。
5. 部分失败、伤害、奖励转换的解释，在现有白盒工件中更清楚，在真实游玩里不一定同等清楚。这是“工程可解释性强于玩家可理解性”的典型症状。

#### 结论

UI/反馈目前不是最糟糕的层，但仍属于“方向已纠正、闭环还没完成”。它已经能支撑 debug 和验证，不等于已经足够支撑玩家理解与记忆。

### 4.8 系统联动性

#### 当前真正的强项

Phase4 的系统联动在“合同”层面相当强：

1. mapgen、terrain、hidden entrance、loot budget、secret zone、content pack 都被放进了统一 schema / lint / report 体系。
2. 多个系统之间共享的是 typed contract，而不是随意拼接的脚本式临时逻辑。
3. 这为后续内容扩张和 Phase5 稳定性工作建立了相对健康的基础。

#### 当前真正的断层

断层不在 `core/game/tools` 之间，而在“系统联动 -> 玩家体感联动”这一步：

1. 战斗、奖励、探索、隐藏内容、构筑系统都存在，但并没有总是把彼此的价值放大给玩家。
2. 当关键路径过短、奖励收敛、boss variant 体感不足时，系统联动更多存在于 report 和 contract 中，而不是存在于玩家一次 run 的情绪起伏中。
3. 特别是中后段 `FIXED_LIST` reward pool、模板级 special-tier 身份偏浅、以及 boss 仅 3 个 variant / 无 phase 切换这三点，会把系统联动压回“都接上了，但玩家感不到增益”的状态。

#### 结论

当前系统不是“各做各的”，但它们还没有稳定汇聚成强烈的玩家体验复利。**系统联动已经成立，体验联动还没有完全成立。**

### 4.9 当前阶段玩法总评

综合上面七个维度，当前版本的评价应该是：

1. 不是“功能完成但完全不好玩”的失败版本。
2. 也不是“已经具备稳定耐玩性的成熟 Phase4 完成态”。
3. 更准确的描述是：**基本可玩，短期内能支撑继续推进，具备耐玩雏形，但主循环中段、奖励分化、隐藏闭环和体感反馈仍不足，暂时不适合直接把这些问题留给 Phase5。**

如果现在继续扩内容或直接转 Phase5，最可能出现的不是“系统更强”，而是：

1. AI 与稳定性工作被用来服务一个本身节奏偏空的主循环；
2. 更多内容被堆在奖励/构筑仍收敛的掉落地基上；
3. 未来越做越难判断“是内容不够，还是底层驱动力没立住”。
