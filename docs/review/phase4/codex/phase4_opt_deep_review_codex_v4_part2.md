# phase4 深度审查报告（Part 2/4）

## 4. 当前阶段玩法体验总评

### 4.1 核心循环

当前版本的核心循环已经具备完整骨架：

`进入 zone -> 探索 -> 遭遇普通战 / 精英 / Boss / 隐藏入口 -> 获得装备 / 资源 / Secret 奖励 -> 形成更强构筑 -> 进入更深 zone`

从 `build/reports/harness/solo-clear-lab.md`、`build/reports/harness/headless-smoke.md`、`build/reports/harness/long-run-full.md` 来看，这条链路已经不是纸面存在，而是能稳定跑通的。`tools/build/reports/phase4/phase4-summary.md` 中 mapgen、solvability、loot、hidden、terrain 全部通过，也说明底层支撑是完整的。

问题在于：**循环已经顺，但最关键的“继续玩下去的理由”还不够强。** 当前最弱的一环不是探索入口，也不是战斗执行，而是“奖励如何把下一步玩法显著改变”。大量系统在告诉玩家“你变强了”，但没有足够频繁地告诉玩家“你这一局会变得不一样”。

结论：

- 前 10 分钟到 30 分钟，当前版本是能成立的。
- 30 分钟之后，动力不会断，但会逐渐从“我想试新的玩法”退化成“我继续把这个有效底盘磨到通关”。
- 这意味着当前核心循环属于**可用、可玩、但中后段驱动还不够锋利**的状态。

### 4.2 战斗体验

战斗层相较早期版本已经明显成立，主要依据有三点：

1. `EncounterDecorationService.kt` 已把精英变异、Boss 变体、地形偏好联动打到正式运行时，而不是测试专用逻辑。
2. `tools/build/reports/phase4/phase4-summary.md` 显示 `eliteMutationDistinctCount=12`、`eliteMutationValidPairCount=51`、`terrainTaggedCombatExposureRate=40.0%`、`terrainInteractionEncounterRate=16.6%`，说明战斗语义不再是低频偶发装饰。
3. `AsciiRenderModel.kt` / `TileRenderModel.kt` / `FoundationGameSession.kt` 已经把地形规则、变异摘要、被动触发、搜索结果都暴露到 inspect / log / snapshot 层。

所以，这个战斗系统不是“会动但没决策”。它已经有以下正面特征：

- 地形开始提供位置与时机价值，而不是纯贴图。
- 精英与 Boss 不再只是血量变厚，而是具备组合式语义差异。
- 玩家能从事件日志和 inspect 中读出“我为什么受伤 / 为什么有效”。

但它仍有两个体验短板：

- **战斗构筑差异没有完全传导到装备底盘层。** 技能树与职业动作是不同的，但终盘武器收敛严重，会把很多战斗判断压回“拿大斧头继续输出”。
- **强反馈多存在于二级信息层。** 当前更多是“你能查到”，而不是“你在决策瞬间就会感到明显”。对熟练玩家这不是大问题，对普通玩家会造成战术价值被低估。

结论：**战斗层已经从“有流程”进入“有意义”，但距离“稳定地产生职业化爽点”还差一层装备与反馈协同。**

### 4.3 成长与构筑

这部分是当前 Phase4 最大的体验分水岭。

正面：

- `long-run-full.md` 已记录 `breakpointPayoffObservationCount=12`，说明职业技能断点收益不是假的。
- `synergyAffixRewardCount=122`、`synergyAffixAdoptionCount=52` 说明奖励系统开始尝试把 affix 与构筑联动起来。
- `AsciiRenderModel.kt` 与 `RenderSnapshotContractTest.kt` 也证明被动说明、条件触发、装备词汇已经进入正式表现合同。

负面：

- `long-run-full.md` 中多个职业的 full-route 终盘都收敛到 `battle_axe`。其中 `vanguard`、`arcanist`、`templar`、`rogue` 都能在不同 run 中走到相似底盘。
- `SmokeBot.kt` 已经对职业武器做了偏好加权，例如 `arcanist` 给 `arcane_staff` 更高分、`rogue` 给 `short_sword` / `hunter_bow` 更高分，但实际长局仍然大量收敛到 `battle_axe`。这意味着问题不只是 bot 策略，而是物品经济和底盘价值本身在推动收敛。
- `items/index.yaml` 中基础武器的底盘差距非常大：`short_sword.baseAttack=5`、`long_sword.baseAttack=8`、`battle_axe.baseAttack=11`、`arcane_staff.baseAttack=4`。而 `LootProfileCandidatePoolResolver.kt` 在基底池层面又没有引入 profession/build 感知。

这会带来非常具体的体验后果：

- 成长“有数值感”，但不够“有身份感”。
- 构筑“有分支”，但很多分支最终站在同一个装备底盘上。
- 玩家会感觉自己换的是技能词条，而不是换了整套玩法。

结论：**当前版本已经有成长，但还没有把成长稳定地做成“职业幻想强化器”。**

### 4.4 奖励驱动

奖励节奏并不差。`phase4-summary.md` 显示：

- `totalLootContentCount=106`
- `affixCount=78`
- `uniqueTemplateCount=20`
- `artifactTemplateCount=8`
- `uniqueArtifactMeaningfulSwapRate=100.0%`

这说明奖励内容量、唯一物价值、白盒意义度都已经上来了。

问题在于，**奖励的层次感和辨识度还不稳定**：

- `whitebox-loot-summary.json` 中 corpus 平均 overlap 只有 `0.205`，看起来很好。
- 但同一份报告里，`loot.deep_iron_pit.reward` 与 `loot.deep_iron_slag_cache.secret` 的 base item overlap 达到 `0.9166666666666666`，与 `loot.deep_iron_smuggler_stash.secret` 也达到 `0.9090909090909091`。
- 这意味着系统总体“分化”成立，但局部关键 reward channel 仍可能给玩家同质化印象。

玩家不会因为“全局平均 overlap 合格”而觉得奖励有趣，玩家只会记住：“我冒了风险进秘密点，结果拿到的还是差不多那几类武器。”

结论：**奖励系统已经有密度，但记忆点还不够稳定；秘密奖励和主线奖励在部分 zone 里仍没有拉开足够的身份差。**

### 4.5 探索与重复游玩价值

这一块的结构比表面看起来更好。

正面：

- `events/index.yaml` 已有 12 个 hidden event，覆盖 6 种 trigger type。
- `secret-zones/index.yaml` 已有 5 个 secret zone，并覆盖 3 种入口锚点家族。
- `whitebox-hidden-content-report.md`、`whitebox-mapgen-summary.json` 证明隐藏入口和地图结构已经是 mapgen 正式合同的一部分。
- `boss-variants`、`elite mutations`、`terrain interactions`、`loot profiles` 的组合已经足以支撑第一轮到第二轮的重复体验差异。

负面：

- `greenwood_fringe` 在 `whitebox-mapgen-summary.json` 中只有 `differenceCategoryCount=3`、`distinctPatternRoomCount=1`、`distinctEntranceLayoutCount=1`，属于刚好过线，不是强重复游玩区。
- 隐藏内容当前更多证明“存在且能触发”，没有证明“自然跑图时足够诱人且足够常见”。
- 长局重复游玩中，探索差异和战斗差异已经有，但构筑差异没有同步放大，导致重复游玩的后劲被部分抵消。

结论：**探索层已经成立，不是 Phase4 的塌陷点；但它还没强到足以单独撑起“再来一局”的强驱动力。**

### 4.6 UI / 反馈 / 新手体验

当前反馈系统的优点是：**解释性已经被工程化了。**

- `FoundationGameSession.kt` 对搜索、隐藏奖励、Secret Zone 进入返回、被动触发、地形规则都写入了语义事件和日志消息。
- `zh-CN.json` 有完整的 `log.search.*`、`log.hidden.*`、`log.passive.*` 文案。
- `AsciiRenderModel.kt` / `TileRenderModel.kt` 对 terrain、mutation、boss variant、passive description 都能做 inspect 展示。

这意味着当前版本不是“输都不知道怎么输”的黑盒游戏。

但它仍有两个现实问题：

- 新手理解成本偏高，因为很多重要信息存在于 inspect 与日志，而不是主战斗视野中的高优先级反馈。
- 反馈虽然正确，但还不够“立即可用”。例如玩家能知道某地形有规则、某敌人有变异，但未必能在当回合迅速感到“这件事值得我改变站位或技能序列”。

结论：**信息不是缺失，而是还没有完全做到低摩擦消费。**

### 4.7 系统联动性

Phase4 目前最值得肯定的一点，是系统之间不是各做各的：

- Mapgen 为 hidden anchor、地形分布、secret entrance 提供底盘。
- Elite mutation 与 terrain tag 之间已有正式联动。
- Hidden content 与 reward pool、search action、zone route 能互相接上。
- Content pack overlay 被限制在内容注册表，不会反向污染规则真源。

真正的问题发生在另一条链上：

`奖励 -> 装备 -> 构筑 -> 职业身份`

这条链目前还没有像 `地图 -> 战斗 -> 事件` 那样打磨到位。也就是说，**世界层系统已经开始互相扶持，但成长层系统仍然没有完全把这些变化转化为职业化、可回味的 build 分歧。**

总评：

- **系统联动整体是成立的。**
- **联动最弱的一环不是探索层，而是成长层。**
- **当前体验不是“系统碎片化”，而是“系统集成不错，但玩家最在意的成长身份回报还不够强”。**
