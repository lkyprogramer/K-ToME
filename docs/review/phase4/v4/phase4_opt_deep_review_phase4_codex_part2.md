# phase4 深度审查报告（Part 2/4）

## 4. 当前阶段玩法体验总评

### 4.1 核心循环

当前 Phase4 的核心循环已经成立，但强度还不到“玩家自然愿意反复刷”的水平。

可成立的部分：

1. **进入地图 / 探索**：mandatory zone 的关键路径不再塌缩，`greenwood_fringe`、`deep_iron_pit`、`grey_gate_depths`、`underground_river`、`abyssal_temple` 都满足 objective/combat floors。`avgObjectiveAcquireTurn` 最低为 `7.167`，说明目标不再出生点即拿。
2. **遭遇战斗**：terrain encounter、Boss phase、telegraph、combat decision surface 都有实现和验证，玩家至少能看到危险来源并做行动选择。
3. **获取奖励**：dynamic pool、secret reward、milestone reward、reliquary purchase、special tier 等奖励源都存在。
4. **角色成长 / 构筑变化**：long-run report 已能记录 terminal weapon、capstone seen/adoption、non-weapon payoff、breakpoint payoff、affix synergy。
5. **隐藏探索**：有机 bot 不使用 primer/reveal API，仍能达到 `secretZoneEntryRate=20.5%`，说明隐藏内容不只是脚本假通。
6. **继续下一局的动机雏形**：职业终局武器至少有 3 类，Boss variant trace 已有 divergence，content pack overlay 也能提供局部新鲜感。

最弱环节是 **奖励转化为构筑变化**。Phase4 主循环现在不是缺入口，而是奖励经常没有改变玩家决策：`milestoneRewardAdoptionDistribution={adopted=46, notAdopted=57}`，未采用奖励多于采用奖励；capstone 与 non-weapon payoff 都只有总体踩线，且 per-prof floor 仍是 approved debt。

这会导致一个典型体验问题：玩家前 10 分钟能感到系统很多，但 30 分钟后会发现多数选择仍回到职业默认武器、默认防御、默认行动顺序。Roguelike 的“再来一局”不是靠系统数量堆出来的，而是靠本局掉落确实改变打法。当前 Phase4 已经有雏形，但还没有把这个闭环打硬。

### 4.2 战斗体验

战斗体验比 v3 明显进步，尤其是 PR05 把“判断-执行-反馈”从日志/键盘暗操作推进到正式 surface。

已成立的部分：

1. `CombatDecisionFrame` 提供 ACTION/METHOD/TARGET 三段式决策面，降低玩家从按键到规则行为之间的黑箱。
2. Telegraph 三位一体已经覆盖 map overlay、target card、log prefix，Boss 风险不再只藏在日志。
3. `ActionHintModel` 对 missing fact、disabled action、illegal target 有区分，避免把未知字段伪装成 `0/ready/no-risk`。
4. `AiIntentLeakRule` 守住了普通敌人 intent 非目标，避免 client 侧从 `aiTypeId` 编造下一步行动。
5. Boss harness 显示 variant action trace 已有差异，旧的“Boss variant 只是换名/换 loot”问题已改善。

仍不足的部分：

1. **战斗爽点仍偏工程正确，不够玩法锋利**。当前报告能证明“可读、可决策、可验证”，但还不能证明不同职业/构筑在战斗中的手感差异足够强。证据是 capstone/non-weapon payoff 仍弱，而战斗差异最终要靠这些构筑输入来放大。
2. **Boss variant 的结构差异仍浅**。`boss-harness.md` 中每对均 `phaseGraphUnchanged=true`、`structuralDiffCount=0`。当前 action trace 通过，说明它不是伪实现；但玩家长期重复游玩时，variant 的记忆点可能仍停在“这次 Boss 更偏某个技能/terrain/reward”，而不是“这次 Boss 的打法变了”。

结论：当前战斗已经从“能打”提升到“可读可判定”，但还没有稳定达到 ToME 类游戏需要的“职业/装备/技能组合导致打法明显变化”。战斗体验的下一步不是继续加 UI surface，而是让奖励与构筑真正改变战斗选择。

### 4.3 成长与构筑

这是当前 Phase4 最大短板，而且需要拆成两个层级：一是报告已经暴露的 capstone/non-weapon payoff 采用不足；二是更上游的 run 内职业树/铭文构筑轴不够成立。

正面证据：

1. `professionAlignedWeaponAdoptionRate=100.0%`，说明职业默认武器语义没有乱。
2. `terminalWeaponBaseDiversity=3`，终局不是单一武器完全统治。
3. `professionCapstoneSeenRate=100.0%`，所有职业至少能看到 capstone 来源。
4. breakpoint payoff 有记录：`blink`、`guard_stance`、`holy_mark`、`shadowstep` 都出现过 build hash change。

负面证据：

1. `professionCapstoneAdoptionRate=25.0%` 只是踩最低线。
2. `professionCapstoneAdoptionFloor.reportOnly=2/4 APPROVED_DEBT`，arcanist 和 rogue adoption 都是 `0/1`。
3. `nonWeaponBuildPayoffFloor.reportOnly=2/4 APPROVED_DEBT`，arcanist 和 rogue non-weapon 都是 `0/1`。
4. `crossProfessionTopWeaponDominance=50.0% top=long_sword` 正好踩上限，templar 与 vanguard 的 terminal semantics 都回到 long_sword。
5. long-run 里 milestone reward 未采用多于采用，说明成长选择经常只是“看过但不用”。
6. `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md` 已把天赋树 V2、`TalentAllocationDraft`、断点成长、前置关系和升级预览列为 P0；但当前运行时仍更接近“等级到了就自动获得技能，玩家主要投入 rank 数值”的模型。
7. `game/src/main/kotlin/com/ktome/game/TalentProgression.kt` 会按 profession tree 和 `unlockLevel` 返回已解锁 talent，`FoundationGameSession.syncUnlockedPlayerTalents` 对新解锁技能 `putIfAbsent(talentId, 1)`；这提供了技能开放节奏，但没有提供同层技能之间的选择压力。
8. `FoundationGameSession.ensurePlayerInscriptions` 开局直接塞满 `healing_light / phase_door / iron_shield / purge` 四个铭文槽；`InscriptionManager.canEquip` 满 4 槽直接拒绝。商店有 inscription offer，但默认满槽导致“购买/替换铭文”不是有效构筑轴。
9. `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md` 已把改造方向固定为：基础职业不新增技能数量、每职业开局 3 个 starter、`unlockLevel` 只进入 learnable、学习新技能消耗职业天赋点、Tier 2/Tier 3 受前置 rank 与树投入约束、开局 2 铭文、满槽购买进入替换流程。
10. arcanist 终局 ARMOR 会采用 `unique_furnace_plate`，而该物品在 `game/src/main/resources/data/build-identity/index.yaml` 中属于 vanguard 的 non-weapon capstone。arcanist 自己的 capstone 只有 `artifact_river_echo / unique_deepcurrent_lens`，且当前长局中 `unique_deepcurrent_lens` 会被 `emerald_charm` 压住不采用。
11. rogue 的 `artifact_briar_heart` 多次作为 OFF_HAND 奖励出现，但终局仍保留 `basic_shield`；这说明 capstone seen/source 覆盖不能替代 adopt 评分修正。

这会压扁构筑空间。表面上系统有职业、武器、off-hand、armor、affix、capstone、reliquary、secret reward；实际玩家会问：“我为什么要为了这次掉落改变打法？”如果答案经常是“不值得”，构筑驱动就不成立。

当前阶段必须修这个问题，因为 Phase4 的目标正是让重复游玩差异明显。如果把 capstone/non-weapon payoff 推到 Phase5，Phase5 的新内容只会继续掉进同一套低采用率漏斗。

职业树与铭文也不能推给 Phase5。Phase5 文档把范围限定为 tactical AI、感知/仇恨、perf/soak、run history、death analysis、replay、QA 和发布收口，并明确“不再引入大新玩法系统”。如果 Phase4 结束时 run 内构筑仍只是装备/affix 单轴，Phase5 不应该再承担补建职业树和铭文经济的责任。

### 4.4 奖励驱动

奖励系统的架构比体验更成熟。

已成立：

1. dynamic loot pool 已 100% 覆盖，`game/src/main/resources/data/loot/index.yaml` 中多个 zone cadence/reward 已使用 `TAG_WEIGHTED`。
2. secret reward identity 通过，secret 与 cadence/reward overlap 受控。
3. reliquary 有 visit/purchase/spend 记录，说明经济入口不是空文档。
4. special tier、quality presentation、ground loot marker、item modal card 等 UI surface 已经让奖励更可见。

未成立：

1. **奖励采用率不够**：`adopted=46`、`notAdopted=57`，说明奖励频率可能足够，但奖励改变决策的频率不足。
2. **slot 层级不平衡**：`ARMOR=8, OFF_HAND=49, WEAPON=46`，护甲作为 build-shifting 载体明显偏弱。
3. **affix 记忆点集中**：synergy activation 只看到 `of_shadow=5`，reward 分布也被 `of_piercing` / `of_shadow` 主导。
4. **capstone 来源覆盖不等于 capstone 采用**：`professionCapstoneSourceCoverage.reportOnly=8/8 PASS` 只能证明能产出，不能证明值得用。
5. **通用 affix 曝光过强**：`routeRewardAffixUsageSummary` 中 `sentinel=8`、`of_strength=7`、`vampiric=7`、`of_life=6` 占据头部曝光。若继续扩内容而不控制头部 affix 占比，玩家会反复看到相似奖励文本。

奖励系统当前不是“没有奖励”，而是“奖励经常没有产生下一步决策”。这类问题比内容量不足更危险，因为后续即使加更多物品，也可能只是增加更多不被采用的掉落。

### 4.5 探索与重复游玩价值

探索层已经具备可玩基础，但未知感和玩家主动性仍不均。

已成立：

1. organic hidden probe 不走 primer/reveal API，仍然有 `leadDiscoveryRate=46.6%`、`secretConversionRate=43.9%`、`secretZoneEntryRate=20.5%`。
2. failing secret-entry zones 为 none，说明没有哪个 zone 完全失效。
3. hidden content 与 frontstage cue 绑定，`frontstageSecretCueVisibilityRate=100%`，说明发现行为能进入玩家可见反馈面。
4. secret reward identity 与 local reward overlap 受控，隐藏发现不只是普通奖励换皮。

问题：

1. `abyssal_temple` 贡献了 52.0% 的 discovery share，`leadDiscoveryRate=97.0%`，但 `searchUseRate=0.0%`。这意味着它很可能靠路径/事件/提示推到玩家眼前，而不是通过 Search 行为训练玩家。
2. `deep_iron_pit searchUseRate=1.5%`，但 `secretEntryRate=23.5%`、`secretConversionRate=68.9%`，同样说明“进了 secret”不等于“玩家学会了主动搜索”。
3. `underground_river leadDiscoveryRate=17.4%` 低于总体，但 searchUseRate=28.0%，说明有些 zone 的 cue 可见性不足，有些 zone 的主动行为不足，体验曲线不统一。
4. `secretZoneDiscoveryDistribution` 与 lead 分布错位：`greenwood_hidden_cache=38.9%` entry share，但 `greenwood_fringe=20.3%` lead share；`abyssal_temple=52.0%` lead share，但 `abyssal_temple_warded_archive=18.5%` entry share。aggregate 通过掩盖了 per-zone 学习曲线差异。

当前隐藏内容已经不再是 v3 的伪完成，但仍缺一个稳定的玩家学习曲线：看到异常 -> 判断风险 -> 主动 Search -> 得到反馈 -> 下次更愿意探索。这个学习曲线应该在 Phase4 先打牢，不能等 Phase5 扩内容后再修。

### 4.6 UI / 反馈 / 新手体验

UI/反馈是 Phase4 后期最明显的增量之一，整体方向正确。

1. PR01 把 token、布局、main menu、BuildInfo 和 continue states 固化，减少“debug app”感。
2. PR02 把 modal/input/look/focus/save block 纳入正式状态机，降低误操作和模式混乱。
3. PR03 把 item quality、special tier、ground loot marker、modal card、empty/error/loading 做成正式 presentation。
4. PR04 把 status、telegraph、description、explain、accessibility 变成可测试 surface。
5. PR05 把 combat decision 的 ACTION/METHOD/TARGET 和 illegal/missing fact 反馈前台化，解决“按键能用但玩家不知道发生什么”的核心问题。

对玩家来说，UI 已经从“能看懂一部分”升级到“主要决策有正式反馈”。本轮 review 不再把 UI/UX 完成态作为 Phase4 阻塞项；后续重心必须转到职业树、铭文、奖励采用率这些真正改变玩法决策的系统。

### 4.7 系统联动性

当前系统联动已经有明显进步，但最关键的联动仍是奖励/构筑/战斗之间。

联动做得好的地方：

1. hidden content -> frontstage cue -> secret reward -> local identity -> phase4 report，这条链路已经有 typed contract 和 owner metrics。
2. telegraph -> target card -> log prefix -> combat decision，这条链路已经避免了多套展示语义。
3. content pack -> manifest version range -> official runtime boundary，这条链路没有越权进入 core rule type。
4. dynamic loot -> whiteBoxLoot -> longRunLab -> reportPhase4，这条链路让奖励系统不再只靠数据文件审阅。

联动不足的地方：

1. reward 出现和 build 改变之间仍不稳定。`source coverage`、`dynamic coverage`、`local identity` 都通过，但 adoption floor 仍是 debt，说明“能产出”到“玩家会用”之间还缺设计推力。
2. zone mechanics 的设计名词和 runtime hook 不完全一致。Critical Path Design Audit 明确列出多个 mechanics without dedicated runtime hook，这会让 zone 体验停在“文案/报告知道它存在”，玩家不一定感到它存在。
3. content pack 的 runtime 合同已经成立，但官方 sample pack 仍把两个核心条目做成 `REPLACE`。Phase4 允许 `ADD + whole-entry REPLACE`，但 sample 的示范职责应该以 ADD-first 为主，REPLACE-heavy 会把下游 pack 作者引向不可叠加的覆盖式写法。
4. long-run 证据链的主路线同构较强：`scenarioTypeDistribution={full_route=6, branch_inclusive=1, route_probe=2, late_route_probe=2}`，`zoneRouteHashDistribution` 中同一 hash 占 6 条。它不直接伤玩家体验，但会让“重复游玩差异明显”的验证语言偏弱。

最终判断：系统之间已经不再是完全“各做各的”，但 Phase4 最该追求的联动是“升级、商店、探索、战斗得到的选择能改变构筑，构筑再改变战斗与探索策略”。这条主线仍需要本阶段加固。
