# phase4 深度审查报告（Part 4）

## 6. 优化建议（按优先级）

以下建议只保留当前 Phase4 内可落地、且能够直接改善体验闭环的问题。每条建议都尽量明确到 owner、修改面与收益。

### 6.1 P0：必须立刻处理

#### P0-1 重建关键路径 pacing，恢复探索->战斗->奖励 的主循环密度

- 问题本质：主线路径若干 zone 的 objective 与交互点过于靠近起点/楼梯，常规战斗数量过低，导致地图过程被压缩成流程通道。
- 影响范围：
  - [game/src/main/resources/data/objectives/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/objectives/index.yaml)
  - [game/src/main/kotlin/com/ktome/game/GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt)
  - `long-run` / `phase4 summary` / owner metric 文档
- 优化目标：让关键路径每个核心 zone 至少都能稳定产生一段“需要处理空间与敌人”的过程，而不是 2 回合内拿到 objective 就离开。
- 具体改法：
  1. 把 `trail_cache`、`seal_cache`、`crystal_cache_chest` 从 `player_start` / `stairs_up` 改为更深层 anchor，优先放到需要经过主路径中段或局部风险区才能触发的位置。
  2. 提升关键路径 zone 的常规敌人下限，建议至少先做：
     - `grey_gate_depths: 0 -> 2`
     - `underground_river: 1 -> 2`
     - `abyssal_temple: 0 -> 1~2`
  3. 在 `long-run` owner metric 中新增或提升以下阈值：
     - `avgObjectiveAcquireTurn` 下限
     - `avgVisibleHostileTurnCount` 下限
     - `avgEnemyTurns` 下限
  4. 把这些阈值同步回 [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md)。
- 预期收益：
  - 主循环中段不再塌陷。
  - terrain、hidden、reward、boss 前置张力都会被同步放大。
  - 玩家对每张图的记忆点会从“点一下目标然后走”恢复到“我在这层做了什么决策”。
- 可能副作用 / 风险：
  - 若敌人数和 objective 深度同时上调过猛，会把单局时长推高。
  - 需要小心不要让 Phase4 短中期 run 节奏突然拖沓。
- 需要同步修改：
  - 文档：是
  - 配置/数值：是
  - UI：否
  - 资源：可能不需要
  - 代码架构：少量，主要是 owner metric/report

#### P0-2 强化 profession-aware loot，并把中后段 reward pool 从局部动态化推进到主路径全覆盖

- 问题本质：奖励系统表面丰富，但 build-aware 权重不足，而且中后段大量 cadence/reward profile 仍是 `FIXED_LIST`；再叠加 item taxonomy 对 `long_sword` 这类 base 的多职业吸附，最终把 run 后半段奖励压回同质化。
- 影响范围：
  - [game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt)
  - [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
  - [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)
  - `long-run` reward/build identity report
- 优化目标：把职业终局装备和中后期奖励走向真正拉开，让“这局换一套 build”成为真实驱动力，同时让主路径中后段不再退回固定清单抽奖。
- 具体改法：
  1. 提高 build-aware 匹配加权，建议把 strong match 从当前级别再上调，并增加“职业已出现 dominant terminal base 时的递减权重”。
  2. 让 `templar` 与 `vanguard` 的高价值 reward pool 不再共享同一组主导终局 base，至少拆出：
     - 一个更偏 `guard / discipline / retaliation`
     - 一个更偏 `frontline / impact / charge`
  3. 把 `shattered_outpost`、`bandit_camp`、`elven_ruins`、`molten_core`、`grey_gate_depths`、`crystal_cavern` 的 cadence，以及 `grey_gate_depths.reward`、`underground_river.reward`、`abyssal_temple.reward`、`abyssal_heart.reward` 从 `FIXED_LIST` 推进到 `TAG_WEIGHTED` 或等价的 zone-aware 动态池。
  4. 降低 milestone reward 对 `WEAPON` 的单边偏重，提升 `OFF_HAND` / `ARMOR` / build-defining passive affix 的进入率。
  5. 对 `items/index.yaml` 做一次 taxonomy 审查，避免单一 base 同时承载过多职业语义。
  6. 在 owner metric 中把 `crossProfessionTopWeaponDominance` 继续压低，并显式增加“profession terminal diversity”与 `dynamicPoolCoverage` 要求。
- 预期收益：
  - build identity 变得可感知。
  - 奖励拿到手后更容易改变后续决策，而不是只变成线性升级。
  - 长期内容扩张将建立在更健康的掉落地基上。
- 可能副作用 / 风险：
  - 过度拆分后可能导致单职业掉落过窄。
  - 需要配合 adoption 数据重新看是否出现新 dominant base。
- 需要同步修改：
  - 文档：是
  - 配置/数值：是
  - UI：否
  - 资源：可能涉及 item text / icon 说明
  - 代码架构：少量

#### P0-3 重写 organic hidden loop owner metric，禁止“看见提示=闭环成功”

- 问题本质：当前 `discovered` 定义过宽，导致 hidden event / primer 命中即可把 discoveryRate 刷绿，无法证明秘密循环真正成立。
- 影响范围：
  - [tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt)
  - [tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md)
  - [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md)
- 优化目标：让 owner metric 真正度量“玩家是否走完了 Phase4 想要的秘密闭环”。
- 具体改法：
  1. 把当前单一的 `discoveryRate` 拆成至少两层：
     - `leadDiscoveryRate`：玩家是否接收到隐藏线索
     - `secretConversionRate` 或 `entranceRevealRate`：玩家是否真的揭示并进入秘密区
  2. 只有 `revealedBindingIds` 或实际 `secretZoneIds` 命中，才能算闭环成功；纯 hidden event 不再算“已发现秘密内容”。
  3. 对 `abyssal_temple` 这类当前 `searchUseRate=0% / secretEntryRate=0%` 的 zone 设立明确下限，不满足就直接红灯。
  4. 把新 greenwood 支线纳入 report owner coverage，避免代码已加、指标仍只看旧 secret zone。
- 预期收益：
  - hidden 系统的真实完成度会被正确暴露。
  - 团队不会再被假阳性指标误导。
  - 后续 secret content 扩张有了可信的 owner gate。
- 可能副作用 / 风险：
  - 指标会短期“变差”，但这是必要暴露，不是坏事。
  - 需要同步调整 hidden 数据与事件节奏，不能只改报表。
- 需要同步修改：
  - 文档：是
  - 配置/数值：是
  - UI：可能需要少量提示补强
  - 资源：可能涉及文本
  - 代码架构：主要在 tools/report

### 6.2 P1：建议本阶段尽快处理

#### P1-1 把 boss variant 从“合同存在”提升到“玩家可感知”

- 问题本质：变体 boss 已存在，但当前只有 3 个 variant、每个 profile 只有 2–3 个 action、且没有 `phaseOverrides`，导致 action mix 与 terrain 互动都不足以构成鲜明体感差异。
- 影响范围：
  - [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml)
  - [tools/build/reports/phase4/whitebox/boss/boss-harness.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/boss/boss-harness.md)
- 优化目标：让玩家在一次 encounter 内明确感到“这个变体迫使我换打法”。
- 具体改法：
  1. 对 base/variant 的 action 权重做强分叉，而不是只做轻微偏置。
  2. 为至少一部分 boss 增加 `phaseOverrides` 或等价阶段切换合同，让血量阈值能真正引入新的 action pool。
  3. 对 `abyssal_eclipse` 这类 terrain preference 不落地的变体，要么提供对应地形条件，要么直接换成当前 zone 中能稳定出现的 terrain 语义。
  4. 在 harness 里新增“variant 与 base 的 trace divergence 最低阈值”，不要只验证 contract parity。
  5. 必要时通过前台文本或 telegraph 强化“这次 boss 有什么不同”。
- 优先级：P1
- 预期收益：
  - boss fight 记忆点变强。
  - terrain/mutation/theme 的价值被更集中地展示。
- 可能副作用 / 风险：
  - 若分叉过猛，可能破坏既有平衡。
  - 需要补一轮 boss harness 与白盒对照。
- 需要同步修改：
  - 文档：建议同步
  - 配置/数值：是
  - UI：可能少量
  - 资源：可能少量文本
  - 代码架构：主要在 harness 断言

#### P1-2 对齐版本纪律与 content-pack 兼容范围

- 问题本质：Phase 文档语义与 runtime/sample pack 的版本口径漂移。
- 影响范围：
  - [gradle.properties](/Users/luo/Documents/github/K-ToME/gradle.properties)
  - [client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt)
  - [examples/content-packs/sample.flooded_relics/manifest.yaml](/Users/luo/Documents/github/K-ToME/examples/content-packs/sample.flooded_relics/manifest.yaml)
  - `ktome-build.properties` 生成链路
- 优化目标：让 Phase4 的 pack boundary 与 build identity 在所有层面保持单一真源。
- 具体改法：
  1. 把仓库版本、窗口标题、build metadata、sample pack `gameVersionRange` 统一到当前阶段口径。
  2. 把所有白盒/fixture 中依赖旧版本区间的示例同步更新。
  3. 在 content pack 文档中明确版本范围更新后的约束示例。
- 优先级：P1
- 预期收益：
  - pack discipline 更可信。
  - 阶段语义更清晰。
  - 避免未来积累更大的版本清算成本。
- 可能副作用 / 风险：
  - 会牵动部分 fixture / docs / report metadata。
- 需要同步修改：
  - 文档：是
  - 配置/数值：是
  - UI：是，窗口标题
  - 资源：sample pack manifest
  - 代码架构：少量

#### P1-3 把 replay hook 从“布局扰动”升级为“内容记忆点”

- 问题本质：当前 greenwood 第二秘密分支已经增加了路线变化，但 secret reward / visual / audio identity 仍复用旧内容， replay hook 增加的主要是几何变化，不是体验身份变化。
- 影响范围：
  - [game/src/main/resources/data/secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml)
  - [game/src/main/resources/data/events/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/events/index.yaml)
  - [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)
- 优化目标：让 replay hook 直接产出“这一局遇到的是另一种秘密内容”的感受。
- 具体改法：
  1. 给 `greenwood_ambush_hideout` 单独的 reward profile，而不是复用 `loot.greenwood_hidden_cache.secret`。
  2. 至少补一个独立 visual/audio/text identity，避免与原 cache 完全同脸。
  3. 在前台回显里对 secret reward 类型做更明确差异描述。
- 优先级：P1
- 预期收益：
  - replay hook 的价值从“地图扰动”升级到“内容分支”。
  - 玩家更容易记住不同秘密区。
- 可能副作用 / 风险：
  - 需要额外维护一份 secret reward profile。
- 需要同步修改：
  - 文档：建议同步
  - 配置/数值：是
  - UI：建议同步前台文本
  - 资源：可能需要
  - 代码架构：很少

#### P1-4 修正文档 coverage 盲点，把 `grey_gate_depths` 等主线平谷纳入体验 owner 范围

- 问题本质：v2opt 关注点是对的，但没有完整覆盖主线路径中的所有体验平谷层。
- 影响范围：
  - [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md)
  - 相关长局报告 owner metric
- 优化目标：让文档本身能正确暴露玩家主路径中段的空洞，而不是只优化局部热点。
- 具体改法：
  1. 在 experience gate 文档中把 `grey_gate_depths`、`abyssal_temple` 这类主线平谷单独列为 owner zone。
  2. 把“关键路径 zone 不得出现 0 战斗/0可见敌人”的底线写进 owner metric。
- 优先级：P1
- 预期收益：
  - 后续 review 不再漏掉主线空档层。
  - 设计文档与实际体验路径更一致。
- 可能副作用 / 风险：
  - 指标可能短期更难达标，但这是正确约束。
- 需要同步修改：
  - 文档：是
  - 配置/数值：可能随后要动
  - UI：否
  - 资源：否
  - 代码架构：少量 report

#### P1-5 为 4 个基础职业建立“终局锚点装备”追逐路径，而不是纯低概率终极武器

- 问题本质：当前中后段成长缺少一个玩家能明确追逐的 profession capstone target。只靠继续赌随机池，build 驱动会在 run 后半段变弱；但如果做成“每职业只有一把极低概率终极武器”，又会同时制造体验不可见和构筑标准答案两个新问题。
- 影响范围：
  - [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
  - [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)
  - [game/src/main/resources/data/secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml)
  - [game/src/main/resources/data/events/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/events/index.yaml)
  - [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml)
  - [game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
- 优化目标：给每个基础职业建立 1 到 2 个可追求的终局锚点装备，让玩家在 30 分钟后仍有明确 chase 目标；同时避免把系统压成“每职业唯一标准答案武器”。
- 具体改法：
  1. 设计规则：
     - 每个基础职业至少 2 个锚点
     - 至少 1 个不是武器，而是副手 / 护甲 / artifact
     - 低概率直掉只作捷径，不作唯一来源
  2. 第一轮优先复用现有 special item 作为种子，而不是全新开 asset：
     - `vanguard`: `artifact_forge_oath` + `unique_furnace_plate` / `unique_quenchbreaker_maul`
     - `arcanist`: `artifact_river_echo` + `unique_deepcurrent_lens`
     - `rogue`: `artifact_heartroot_gambit` + `unique_thornpath_crook` / `unique_briarbound_bow`
     - `templar`: `artifact_eclipsed_relic` + `unique_vesper_chainmail` / `unique_voidlit_seal`
  3. 获取路径采用“主路径可追求 + 低概率捷径”：
     - secret zone / hidden event：给 profession-tagged capstone reward 或 shard
     - boss / finale：给本职业未见过的锚点高权重或 choice reward
     - elite / boss：保留小概率直掉，但必须带 unseen bias / pity，不做纯黑箱彩票
  4. 第一版不要求新建复杂 crafting 系统；优先复用现有 `rewardProfileId`、`guaranteedContent`、`lootProfileOverride`、`abyssal_heart.reward` 等正式路径完成。
  5. owner metric 追加：
     - `professionCapstoneSeenRate`
     - `professionCapstoneAdoptionRate`
     - `professionCapstonePathDistribution`（secret / boss / finale / direct drop）
- 优先级：P1
- 预期收益：
  - 玩家在中后段有明确追逐目标，而不是继续盲赌随机池。
  - build identity 会从“我拿到更好的东西”升级成“我开始围绕这件东西重组这局”。
  - special-tier、secret content、boss reward 三条线会第一次在玩家体验层真正汇合。
- 可能副作用 / 风险：
  - 如果每职业只给 1 把唯一神兵，会把构筑压扁。
  - 如果主获取路径仍然太随机，玩家依旧体验不到。
  - 如果只做武器，不做副手/护甲/artifact，会浪费现有职业 identity 的实际落点。
- 需要同步修改：
  - 文档：是
  - 配置/数值：是
  - UI：是，需要在 reward/frontstage 上明确“职业锚点”来源
  - 资源：优先不新增，先复用现有 special item
  - 代码架构：中等，主要是 reward resolver 与 owner metric

### 6.3 P2：可以排期，但不应忽略

#### P2-1 提升非武器奖励的 build-defining 价值

- 问题本质：当前奖励生态过度围绕武器槽展开，副手/护甲/辅助语义不够能改写打法。
- 影响范围：
  - [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
  - [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)
- 优化目标：让玩家不只盯着武器升级，也愿意围绕副手、护甲、特殊 affix 做选择。
- 具体改法：
  1. 为副手/护甲引入更明确的 build-defining affix 组合。
  2. 在 reward pool 中提高这类条目的关键节点出现率。
- 优先级：P2
- 预期收益：奖励面更立体，构筑分歧更丰富。
- 可能副作用 / 风险：需要避免信息过载。
- 需要同步修改：文档/数值/资源。

#### P2-2 继续加强前台可读性，把“debug 可解释”转成“玩家可理解”

- 问题本质：当前前台可读性补强已经开始，且标签本地化与 cue cap 都已到位，但 ACTION 通道仍缺少 source priority，玩家侧记忆结构仍弱。
- 影响范围：
  - [core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt)
  - [client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt)
  - [client/src/main/kotlin/com/ktome/client/render/RewardPresentationText.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/RewardPresentationText.kt)
- 优化目标：让玩家更容易知道“我为什么输、为什么这次掉落重要、为什么值得搜这块区域”。
- 具体改法：
  1. 让 `recentActionHighlights` 不再只按“最近两条”回放，而改成“按 priority 再按 recency”挑选。
  2. 在 ACTION 通道内部明确至少三档优先级：`search/secret reveal > hidden reward / boss phase cue > passive trigger`。
  3. 补充 reward reason / hidden clue reason 的简短前台归因文本。
  4. 把秘密入口、独特奖励、boss 变体的关键信息做更强的 frontstage 突出。
- 优先级：P2
- 预期收益：降低理解成本，提高记忆点。
- 可能副作用 / 风险：文案太多会造成噪音。
- 需要同步修改：UI、文本资源、可能的 snapshot 字段。

#### P2-3 把 special-tier identity 从“通用 passive 组合”再往前推一层

- 问题本质：当前 unique / artifact 并非没有 identity，但其 identity 主要来自通用 passive 词汇的预烘焙组合，special-tier 的层次拉升还不够深。
- 影响范围：
  - [core/src/main/kotlin/com/ktome/core/item/ItemModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/item/ItemModels.kt)
  - [game/src/main/kotlin/com/ktome/game/data/DataLoader.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/data/DataLoader.kt)
  - [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
- 优化目标：让 special-tier 掉落更稳定地成为“值得围绕它重构这局玩法”的锚点，而不是只是更高级的组合包。
- 具体改法：
  1. 先不急着开新大系统，优先为 special-tier 建立 owner metric：
     - 同区 unique / artifact 的 passive kind 分布不得高度重复
     - special-tier 在长局中的采用后玩法转向率要有可观察指标
  2. 对每个区至少保留一个 truly build-defining 的 special item，避免同区 special template 全部落到相近 passive 家族。
  3. 在前台描述上，把 special-tier 的核心 payoff 从“物品说明的一部分”抬升为“拾取时即被看见”的信号。
- 优先级：P2
- 预期收益：高稀有度掉落的身份感更稳，reward peak 更容易形成记忆点。
- 可能副作用 / 风险：若 owner metric 过强，可能反向挤压普通 rare/magic 的存在感。
- 需要同步修改：文档、数值、item 文本、可能的 report 统计。

## 7. 可延后到后续阶段的问题

这里只保留那些确实更适合 Phase5 或之后处理的问题；不把当前应该修的体验缺陷偷渡出去。

### 7.1 Tactical AI 深化与更复杂的敌我意图博弈

- 为什么可以延后：
  - 这属于 [docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md](/Users/luo/Documents/github/K-ToME/docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md) 的明确范围。
  - 当前更优先的是把地图过程、奖励驱动、boss 变体体感先修到合格线。
- 当前是否需要做轻量兜底：
  - 需要。
  - 至少应保证现有 boss/elite 的行为差异已经足够可感知，否则 Phase5 的 AI 深化会建立在一个“基础体感都不明显”的表层之上。

### 7.2 Replay 产品化、Run History 展示、Death Analysis 完整化

- 为什么可以延后：
  - 完整 replay/persistence/product surface 属于 Phase5 主战场。
  - 当前 Phase4 只需要保留 seed、trace、owner report、早期 replay hook 即可。
- 当前是否需要做轻量兜底：
  - 需要。
  - 应保留并补齐当前已开始的 early replay hook 与 frontstage readability，不要在 Phase4 把玩家可读性丢空。

### 7.3 大规模内容扩张

- 为什么可以延后：
  - 当前不是“内容数量完全不够”，而是“已有内容没有被正确转化成稳定体验”。
  - 在 pacing、reward、hidden loop 没修好之前，继续扩内容收益不高。
- 当前是否需要做轻量兜底：
  - 需要。
  - 先修关键路径平谷、奖励收敛和秘密闭环，再考虑更大规模内容投放。

### 7.4 更完整的 content-pack 生态与更多样例包

- 为什么可以延后：
  - 当前 Phase4 已经完成 runtime/overlay/lint/sample pack 的最小正式路径。
  - 更大的 pack 生态属于 Phase4 之后的自然扩展，而不是当前必须补齐的闭环项。
- 当前是否需要做轻量兜底：
  - 需要。
  - 先把版本纪律与 sample pack `gameVersionRange` 对齐，确保现有 pack path 本身是可信的。

## 8. 最终结论

### 8.1 当前 Phase4 是否达到了文档预期

如果只看“系统有没有做完、contract 有没有落地、whitebox/harness/report 有没有建立”，答案是：**大体达到了，而且工程完成度相当高**。

如果看“文档设计目标是否真正转化为玩家可感知的体验结果”，答案是：**只达成了一部分**。当前最明显的差距在：

1. 关键路径 pacing 仍有结构性空洞。
2. build/reward identity 仍偏收敛，而且中后段 reward pool 的动态化覆盖明显不足。
3. hidden loop 的 owner metric 不能真实代表闭环完成度。
4. boss variant 的体验差异不足，源数据本身也过薄。
5. 版本纪律与 pack boundary 口径漂移。
6. 当前仍缺少玩家能明确追逐的 profession capstone target。

所以更准确的判断是：**Phase4 的系统与验证承诺基本完成，但体验承诺尚未完全兑现。**

### 8.2 当前版本是否已经“好玩”

当前版本不是不好玩，而是**好玩度不稳定**：

1. 它已经能提供短期的探索、战斗、成长与发现乐趣。
2. 但这些乐趣还没有被稳定组织成持续数局都成立的强正反馈。
3. 当 run 进入中后段，节奏空洞、奖励收敛、hidden 假绿和 boss 体感不足会逐步暴露。

因此，本报告不认同“功能做完了，所以现在已经好玩”的判断。

### 8.3 当前版本是否具备“耐玩雏形”

**具备。**

理由是：

1. ProcGen、hidden、reward、content pack、profession-aware loot 这些真正支撑耐玩的结构都已经在。
2. 当前欠缺的不是方向，而是把这些结构变成稳定体验的最后一层打磨和 owner 约束。

换句话说，它已经有耐玩的骨架，但还没有把骨架长成可靠的肌肉。

### 8.4 是否适合进入下一阶段开发

**不建议直接全面转入 Phase5 主开发。**

更合理的顺序是：

1. 先完成本报告列出的 P0。
2. 至少把 P1 中的 boss variant 体感、version discipline、replay hook 内容身份补到位。
3. 然后再进入 Phase5 的 tactical AI / replay / stability / release 线。

否则，Phase5 很可能变成：

1. 在一个节奏偏空的主循环上做 AI；
2. 在一个奖励收敛的成长系统上做稳定化；
3. 在一个 hidden metric 假绿的系统上做更多可视化。

这会让后续开发越来越“正确”，但不一定越来越“好玩”。

### 8.5 进入下一阶段前必须先补的内容

进入 Phase5 前，至少应先完成以下事项：

1. 修复关键路径 pacing，确保主线路径不再出现 0 战斗或 2 回合拿 objective 的平谷层。
2. 强化 profession-aware loot，并把中后段 reward pool 从局部动态化推进到主路径全覆盖。
3. 重定义 organic hidden loop owner metric，确保 hidden 闭环能被真实度量。
4. 提升 boss variant 的可感知差异，并为其补上阶段切换或等价的 source-level 变化。
5. 对齐版本纪律与 sample pack 兼容区间，守住 Phase4 的 pack boundary 可信度。
6. 把当前已开始的 frontstage readability / replay hook 补到“有记忆点”的程度，而不是停在“多了点说明文字”。
7. 至少补一层 ACTION 通道 priority，避免 PR-05 的可读性收益在高噪音场景里被稀释。
8. 为 4 个基础职业补出 1 到 2 个可追逐的终局锚点装备，主路径可追求，低概率直掉只作捷径。

### 8.6 最终一句话结论

当前 Phase4 不是失败品，也不是成熟完成态。它的真实状态是：

**系统做到了，体验只做对了一半；现在最需要的不是立刻进入 Phase5，而是把 Phase4 最关键的几处坏地基先修平。**
