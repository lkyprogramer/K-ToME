# phase4 深度审查报告（Part 3）

## 5. 当前 Phase4 最需要解决的关键问题

以下问题按严重程度排序。排序标准不是“实现难不难”，而是“它是否正在直接伤害当前好玩度，并且如果现在不修，后续只会把坏地基放大”。

### 5.1 问题一：关键路径节奏塌陷，核心循环中段失去战斗与探索张力

#### 问题描述

当前若干关键路径地图已经不是“探索-遭遇-判断-奖励”的 Roguelike 空间，而是“很快触达 objective / 楼梯 / 交互点”的流程通道。其结果是：

1. 地图存在，但地图过程不成立。
2. 战斗语义存在，但没有被足够多地调用。
3. Terrain、隐藏内容、奖励、路线差异都失去足够的承接空间。

#### 证据

1. [game/src/main/resources/data/objectives/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/objectives/index.yaml)
   - `greenwood_signal_hunt` 的 `trail_cache` 在 `player_start`
   - `grey_gate_seal_rite` 的 `seal_cache` 在 `player_start`
   - `underground_river_crossing` 的 `crystal_cache_chest` 在 `stairs_up`
2. [game/src/main/kotlin/com/ktome/game/GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt)
   - `grey_gate_depths -> 0`
   - `abyssal_temple -> 0`
   - `underground_river -> 1`
3. [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json)
   - `greenwood_fringe.avgObjectiveAcquireTurn = 2.0`
   - `grey_gate_depths.avgObjectiveAcquireTurn = 2.0`
   - `underground_river.avgObjectiveAcquireTurn = 2.0`
   - `abyssal_temple.avgEnemyTurns = 0.0`
   - `abyssal_temple.avgVisibleHostileTurnCount = 0.0`
4. [tools/build/reports/verification/phase4/report-phase4-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/verification/phase4/report-phase4-summary.md)
   - 已经把 `abyssal_temple` 标记为 objective/pressure-driven path 导致 combat denominator 无法稳定采样的特殊项

#### 为什么这是当前必须解决的问题

1. 这是直接伤害核心循环的问题，不是 polish。
2. Phase4 的所有重点系统都建立在“地图过程本身有张力”的前提上。若地图过程塌陷，ProcGen、terrain、loot、hidden、boss 这些系统都只能在一个薄壳上发挥。
3. 这不是 Phase5 的工作。Phase5 做 tactical AI、replay、perf/soak 时，默认前提是玩法主循环已经成立；它不会替 Phase4 把地图过程补回来。

#### 如果不解决的后果

1. 玩家会把若干 zone 记成“过场层”而不是“玩法层”。
2. 内容越多，这种空洞越难发现，因为报表会继续证明“功能都在”，但玩家实际体感越来越稀薄。
3. 后续若把 AI 或更多 content 堆上去，只会让内容成本越来越高，而单位体验收益越来越低。

### 5.2 问题二：build 与奖励仍明显收敛，构筑驱动力没有真正拉开

#### 问题描述

当前职业 aware loot、affix、unique、artifact 都已经落地，但实战结果仍然显示：

1. 不同职业在后期奖励吸附上仍明显趋同。
2. 武器槽权重过高，副手/护甲/辅助型奖励的存在感偏弱。
3. 奖励很多时候只是“替换成更好数值”，而不是“推动我转向另一套玩法”。
4. 中后段若干区的 cadence/reward 仍停留在 `FIXED_LIST`，导致奖励惊喜在 run 后半段直接变平。
5. unique / artifact 并非没有 passive，但 special-tier 身份主要仍停留在复用通用 passive 词汇的“预烘焙组合”，拉升力度不够。

#### 证据

1. [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json)
   - `terminalWeaponBaseDiversity = 3`
   - `crossProfessionTopWeaponDominance = 0.5`
   - `templar -> long_sword`
   - `vanguard -> long_sword`
2. [build/reports/harness/long-run-summary.md](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-summary.md)
   - reward slot 统计高度偏向 `WEAPON`
   - adoption 并没有体现强 build shifting
3. [game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt)
   - build-aware 权重仍偏保守，强匹配加成不够强
4. [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
   - `long_sword` 的标签同时覆盖多个前排职业语义
5. [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)
   - `shattered_outpost`、`bandit_camp`、`elven_ruins`、`molten_core`、`grey_gate_depths`、`crystal_cavern` 的 cadence 仍为 `FIXED_LIST`
   - `grey_gate_depths.reward`、`underground_river.reward`、`abyssal_temple.reward`、`abyssal_heart.reward` 也仍为 `FIXED_LIST`
6. [core/src/main/kotlin/com/ktome/core/item/ItemModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/item/ItemModels.kt)
   - `SpecialItemTemplate` 合同仍只提供 `fixedAffixIds / fixedMaterialId`
7. [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
   - unique / artifact 已经带 passive，但这些 passive 仍主要复用 `OnHitStatusProc`、`TerrainAffinityBonus`、`ConditionalStatBonus`、`OnKillResourceRestore` 等通用种类
8. [game/src/main/resources/data/secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml) 与 [game/src/main/resources/data/events/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/events/index.yaml)
   - secret zone / hidden reward 仍以 zone 固定 `rewardProfileId` 和固定 `LOOT_PROFILE` 为主，没有 profession-specific capstone chase path
9. [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml)
   - boss reward override 仍按 zone 固定，没有为基础职业提供明确的终局锚点获取路径

#### 为什么这是当前必须解决的问题

1. Phase4 的价值之一，就是把“长局内容扩张”建立在正确的奖励生态上。如果奖励生态已经结构性收敛，后续扩内容只会继续喂给同一组 dominant 解。
2. 这不是简单数值平衡问题，而是“游戏为什么值得重开一局”的驱动力问题。
3. 如果 `FIXED_LIST` 仍覆盖大量中后段区，profession-aware loot 的收益就只会局部成立，run 后半段的奖励峰值会继续塌掉。
4. 如果 special-tier 身份始终只靠“更好的通用 passive 组合”抬升，玩家会记得自己拿到了高稀有度掉落，但不一定会因此改变构筑逻辑。
5. 如果中后段没有一个玩家能明确追逐的 profession capstone target，玩家在 30 分钟后仍然只能继续赌随机池，而不是围绕某个职业锚点调整路线与风险承受。
6. 如果现在不处理，Phase5 做 AI/稳定性只会把同质化 run 变得更稳定，而不会变得更好玩。

#### 如果不解决的后果

1. 构筑表面复杂、实际单一，玩家很快看穿最佳路径。
2. 中后段若干区会持续沦为“固定清单抽奖”，让 reward 系统的后半程价值越来越低。
3. special-tier 掉落会变成“值得拿，但未必值得围绕它改玩法”的半成品奖励。
4. 玩家缺少“这局我要追这件装备”的明确中后段目标，run 后半段更容易退化成被动吃随机数。
5. 装备系统会沦为高认知成本、低回报感的复杂外壳。
6. 内容扩张越多，后期平衡与内容维护成本越高，因为所有新物品都在和既有 dominant base 做无效竞争。

#### 为什么这里不该做成“纯低概率终极武器”

针对这个问题，最容易想到的补法是“每职业一把极低概率终极武器”。但这不是当前项目的优解，原因很明确：

1. 纯极低概率掉落，大多数 run 根本体验不到，无法承担当前 Phase4 的用户体验修复任务。
2. 每职业只有一把武器，会直接把 build 收敛成新的标准答案。
3. 当前仓库里职业 identity 已部分落在副手、护甲、artifact 上，只做武器会浪费现有 special item 资产。

Phase4 更合理的补法，是 **每个基础职业 1 到 2 个终局锚点装备，至少 1 个不是武器**，并通过：

1. secret zone / hidden event 的 guaranteed reward
2. 条件 boss / finale reward
3. 低概率直掉作为捷径而非主路径

来形成“可追逐、可记忆、但不压死构筑”的 profession capstone chase path。

### 5.3 问题三：organic hidden loop 指标假绿，隐藏内容闭环没有被真正约束

#### 问题描述

当前 hidden content 不是没做，而是**owner metric 把“看见 hidden 信号”误当成“完成了有机隐藏闭环”**。这会导致团队误以为隐藏玩法已经成立，实际玩家却没有稳定走完：

线索 -> 搜索/判断 -> 入口揭示 -> 进入秘密区 -> 获得独特回报

#### 证据

1. [tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt)
   - `discovered = revealedBindingIds.isNotEmpty() || hiddenEventIds.isNotEmpty() || secretZoneIds.isNotEmpty()`
   - 这意味着只要 hidden event 命中，就能算发现
2. [tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md)
   - `abyssal_temple.discoveryRate = 100%`
   - 同时 `searchUseRate = 0%`
   - `secretEntryRate = 0%`
3. 同一份报告中，真正转成 secret zone entry 的分布并不覆盖全部 zone，说明“发现率很高”并不等于“秘密循环成立”
4. 当前工作区虽然新增 `greenwood_ambush_hideout`，但它仍复用原有 secret reward/visual 语义，进一步说明“新的入口变化”并没有自动变成“更强的闭环身份”

#### 为什么这是当前必须解决的问题

1. 这是 owner metric 的定义错误，不是普通调参问题。
2. 如果 owner metric 本身是假的，后续所有关于 hidden 体验是否成立的判断都会偏掉。
3. Phase4 的一项核心承诺就是“hidden content 成立”。如果指标本身不能证明这一点，就不应把问题推给 Phase5。

#### 如果不解决的后果

1. 团队会持续在一个假阳性的指标上做增量优化。
2. 未来再加 secret zone / hidden event 时，只会放大“看似很多，实际不转化”的问题。
3. 玩家会感受到“我似乎偶尔看到提示，但很少真正得到值得追的秘密收益”，从而逐步忽略隐藏系统。

### 5.4 问题四：boss variant 合同安全，但战斗体感过薄

#### 问题描述

当前 boss variant 更接近“数据与合同存在”，还不是“玩家会明显调整打法”的内容。结果就是：

1. 变体占据了设计复杂度与维护成本。
2. 但没有带来相应的战斗识别度与记忆点。

这类系统如果体感不足，会从资产变成负资产。

#### 证据

1. [tools/build/reports/phase4/whitebox/boss/boss-harness.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/boss/boss-harness.md)
   - 多个 base/variant 的 action trace 基本一致
   - `abyssal_eclipse` 的 terrain preference 未落到实际可用地形
2. [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml)
   - 当前只有 `molten_glass / grey_crown / abyssal_eclipse` 3 个 variant
   - 每个 `actionWeightProfile` 只有 2–3 个 action
   - 不存在 `phaseOverrides` 一类阶段切换合同

#### 为什么这是当前必须解决的问题

1. Boss variant 是 Phase4 引入的新复杂度之一，如果它目前没有带来可感知差异，那就是当期就该止损和修正的结构性问题。
2. 这不是一定要等 Phase5 tactical AI 才能修的事。当前阶段就能通过 terrain availability、action 权重、trigger 条件、frontstage 文本提示来把差异拉出来。

#### 如果不解决的后果

1. 玩家感受不到“同一个 boss 这次为什么不一样”。
2. 后续内容扩展时会继续复制出更多“理论不同、体感相同”的变体。
3. 维护成本增加，但不会增加好玩度，典型负资产。

### 5.5 问题五：版本纪律与 content-pack 兼容口径漂移

#### 问题描述

文档层已明确进入 Phase4 语义，但 runtime/build/sample pack 仍停留在 `0.1.0` 口径。这不是简单标签问题，而是：

1. Phase 语义和 build identity 断裂；
2. content-pack 的 `gameVersionRange` 与当前阶段认知不一致；
3. Phase4 最重要的 pack boundary discipline 被自身版本口径削弱。

#### 证据

1. 文档使用的是 Phase4 / `v0.4.x` 语义：
   - [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](/Users/luo/Documents/github/K-ToME/docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
   - [docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)
2. runtime/build 仍是 `0.1.0`：
   - [gradle.properties](/Users/luo/Documents/github/K-ToME/gradle.properties)
   - [client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt)
3. sample pack 仍声明：
   - [examples/content-packs/sample.flooded_relics/manifest.yaml](/Users/luo/Documents/github/K-ToME/examples/content-packs/sample.flooded_relics/manifest.yaml)
   - `gameVersionRange: ">=0.1.0 <0.2.0"`

#### 为什么这是当前必须解决的问题

1. content pack boundary 是 Phase4 的正式冻结合同之一，版本纪律正是它的一部分。
2. 如果连官方 sample/fixture 都在错误版本区间上运行，pack compatibility 的演示价值会被削弱。
3. 这类问题拖到后面再改，会波及更多 sample、fixture、文档、report 元数据与玩家认知。

#### 如果不解决的后果

1. pack loader 的“版本校验”会在实际认知上变得含糊。
2. 后续阶段切版本时会变成一次更大的清算，而不是一次正常推进。
3. 玩家和内容作者会得到错误信号，不知道当前主线到底处在什么阶段语义上。

### 5.6 问题六：设计文档本身存在一个覆盖盲点，导致主线路径出现 phase3 级平谷

#### 问题描述

当前不仅实现有问题，文档本身也有一个盲点：v2opt 重点加固的 zone 集合没有完整覆盖玩家主路径中的关键空档层，尤其是 `grey_gate_depths`。

如果设计层没有把这类关键路径“平谷层”纳入体验 owner 视野，就会出现文档说得通、局部系统也做得对，但玩家实际 run 中间还是空掉的情况。

#### 证据

1. [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md) 与相关优化文档，重点提升的 zone 并未完整覆盖所有关键路径空档。
2. [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json) 显示 `grey_gate_depths` 的 objective 获取节奏依旧非常短。
3. [game/src/main/kotlin/com/ktome/game/GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt) 中该区怪物数量仍为 0。

#### 为什么这是当前必须解决的问题

1. 这不是实现 bug，而是设计 coverage 不足。
2. 若不在 Phase4 修正文档与 owner metric，后续优化会继续在“看得到的 zone”上打补丁，而把真正的主线路径平谷留在那儿。

#### 如果不解决的后果

1. 玩家 run 的中段乏味问题会反复出现，但很难通过现有 owner doc 直接暴露。
2. 后续越迭代，团队越容易误以为“都是个别数值问题”，而不是设计 coverage 问题。

### 5.7 小结：哪些问题绝不能留给 Phase5

以下四类问题都不应被偷渡到 Phase5：

1. **关键路径 pacing**：这是核心循环问题，不是 AI 问题。
2. **build/reward 收敛**：这是 Phase4 奖励生态问题，不是后续内容量问题。
3. **organic hidden loop 假绿**：这是 owner metric 定义问题，不是以后再丰富 secret content 就会自动修好。
4. **boss variant 体感不足**：这是当前新增复杂度的收益问题，不是以后接 tactical AI 再顺带解决。

换句话说，Phase5 应该建立在“已经成立的玩法结构”上，而不是接手一个“工程上完成、体验上半成品”的 Phase4。
