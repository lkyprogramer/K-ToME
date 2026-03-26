# Phase3 深度审查报告（Part 3/4）

## 5. 当前 Phase3 最需要解决的关键问题

### 5.1 世界分支和目标系统呈现为“有合同、少玩法”的伪完成

- 问题描述：`WorldProgress / Quest / Gate / RouteReward` 已经成型，但 objective 与 zone uniqueness 大量停留在 schema 层，玩家实际经历到的主支线推进非常薄。
- 证据：
- `completionRule` 只在 `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` 和 `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` 被定义和加载，运行时没有消费。
- `recordObjectiveProgress()` 只记日志，不写 quest state，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1985-1998`。
- `completeCurrentZoneQuest()` 在 zone clear 时直接把整条 quest 的 objective 全部置为 `COMPLETED`，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1615-1628`。
- optional zone 只要求 `uniqueContentTag != null`，对应测试也只校验“字段存在”，见 `game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt:15-23`。
- `bandit_camp / elven_ruins / molten_core / crystal_cavern` 都没有 `objectiveSetId`，`underground_river / abyssal_temple / abyssal_heart` 的 objectiveSet 存在但 interactable 数为 `0 / 0 / 0`，见 `game/src/main/resources/data/zones/index.yaml` 与 `game/src/main/resources/data/objectives/index.yaml`。
- 为什么这是当前必须解决的问题：
- Phase3 的核心承诺不是“地图数量变多”，而是“长局世界结构成立”。
- 如果主支线、optional zone、目标推进只是文案和字段，而不是实际决策与奖励约束，那么长局就只是更长，不会更成立。
- 为什么不能推到 Phase4：
- Phase4 是在 Phase3 世界结构上继续叠 ProcGen、隐藏内容、精英突变。
- 如果 Phase3 的分支/目标系统本身就是装饰字段，Phase4 只会把更多内容压在假地基上。
- 不解决的后果：
- 玩家会把分支视为“多打一段地图”，不会把它视为“有意义的路线决策”。
- 后段 mandatory zone 会进一步丢失节奏抓手，长局体验越来越平。

### 5.2 高价值奖励没有把 affix 系统转成真正的构筑驱动

- 问题描述：affix v1 已经实现，但最该制造 build 爽点的奖励节点仍然发固定白板 base item。
- 证据：
- 路线奖励在 `claimRouteReward()` 里直接用 `itemBaseDef(...).toRuntimeItem()` 发放，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1679-1696`。
- `rewardItemFromProfiles()` 会按职业偏好先选 base item，再走 `officialRewardItem()` 返回 `base.toRuntimeItem()`，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1865-1916`。
- `ItemBaseDef.toRuntimeItem()` 不生成材质、不生成 affix，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5245-5258`。
- `build/reports/harness/long-run-full.md` 中 12 条胜利 buildHash 大多仍是无 affix 的基础装备和统一铭文包。
- `game/src/main/resources/data/world/world_graph.yaml` 的 route reward 也基本是一条路线绑定一个固定 guaranteed item。
- 为什么这是当前必须解决的问题：
- Roguelike 长局的中后程驱动力，不能只靠“下一关还没打完”，必须靠“我刚拿到的东西改变了打法”。
- Phase3 文档已经把 affix v1 和 build correlation 放进核心承诺；现在它没有进入主奖励节点，等于承诺没有落到玩家爽点上。
- 为什么不能推到 Phase4：
- Phase4 的 loot 生态深化、artifact、unique 都是在 Phase3 affix loop 之上扩张。
- 如果 Phase3 的关键奖励节点仍然不滚 affix，Phase4 只会扩张一个本质仍然偏平的奖励框架。
- 不解决的后果：
- 路线奖励与 Boss 奖励会越来越像“进度补丁”，不是“构筑里程碑”。
- 玩家对奖励的期待会转成“拿到一个应该拿到的职业件”，而不是“抽到一个改变打法的结果”。

### 5.3 基础职业和敌方 roster 明显低于 Phase3 内容底线

- 问题描述：当前系统深度远高于内容厚度，导致职业、怪物、路线都没有把 Phase3 的结构潜力用满。
- 证据：
- 路线图明确要求基础职业正式树达到 `4 x 16 = 64` 天赋，见 `docs/2026-03-13-phase2-to-phase5-final-roadmap.md:373`。
- 当前基础职业天赋总数只有 `44`，来自 `game/src/main/resources/data/talents/index.yaml` 的实际统计：`vanguard 12 / arcanist 11 / rogue 11 / templar 10`。
- 路线图要求怪物模板 `60` 左右，见 `docs/2026-03-13-phase2-to-phase5-final-roadmap.md:377`；当前 `game/src/main/resources/data/monsters/index.yaml` 只有 `26` 个 monster。
- `26` 个 monster 中只有 `6` 个带 talents，其余大多是 stat block + 基础 AI。
- 为什么这是当前必须解决的问题：
- 这不是“后续再加内容更丰富”的问题，而是 Phase3 对“构筑深度”和“长局变体”的最低交付没有达到。
- 当前的 build 不够深，不是因为玩家不理解系统，而是因为系统能吃的内容还没填满。
- 为什么不能推到 Phase4：
- Phase4 重点不是补 Phase3 最小职业树和常规怪 roster，而是 ProcGen 与 loot 生态深化。
- 如果基础职业树和 monster roster 本身就偏瘦，Phase4 只会让“地图变化很多，但战斗语言还是薄”。
- 不解决的后果：
- 角色成长会更像数值推进，不像 build 分化。
- 路线差异和敌人差异也会继续被压缩成“同一套系统换血量和抗性”。

### 5.4 中后程战斗辨识度不足，Boss 差异不够站得住

- 问题描述：玩家侧系统够多，但敌方侧尤其后段 Boss 的技能语义不够分化，导致长局战斗记忆点不够。
- 证据：
- `orc.molten_giant` 的核心 talents 是 `power_strike + charge`。
- `cultist.dungeon_lord` 与 `abyssal.guardian` 都使用 `war_cry + power_strike + charge`，见 `game/src/main/resources/data/monsters/index.yaml`。
- Boss phase 主要通过 `aiProfile` 权重和 `onEnter` telegraph 区分，见 `game/src/main/resources/data/bosses/index.yaml` 与 `game/src/main/resources/data/ai/index.yaml`。
- `build/reports/harness/boss-harness.md` 里三个 Boss 都能触发 telegraph 和 phase，但 `aiTraceCount` 全为 `0`，说明当前工具并没有真正证明“Boss 的行动语义成立”。
- 为什么这是当前必须解决的问题：
- Phase3 的“深层战斗”不是只让玩家技能多，而是让玩家面对的战斗题目也更丰富。
- 如果中后段 Boss 语言不够分化，玩家会把 Phase3 终局理解成“更硬的同类战斗”。
- 为什么不能推到 Phase4/5：
- Phase4 的内容扩张会放大 encounter 池；Phase5 的 Tactical AI 只会建立在 Phase3 行动目录之上。
- 如果 Boss 和常规敌人的技能目录本身就薄，后续只能在薄目录上做更复杂调度。
- 不解决的后果：
- Boss 战通关后缺少角色记忆点。
- 长局的最后三分之一很容易从“进阶战斗”退化为“耐久战”。

### 5.5 当前验证门禁无法可靠识别“太容易、太线性、太同质”

- 问题描述：自动化任务能通过，不等于 Phase3 体验达标；现有门禁对错误方向的容忍度偏高。
- 证据：
- `LongRunLabFullTest` 固定使用 `FOUNDATION_ZONE_ROUTE`，见 `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt:22-38`。
- 同一个测试里，如果 `nonVictoryReports` 为空，就直接把 `afterDeepIronRatio` 记为 `1.0`，见 `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt:57-65`。
- 本次实际运行 `build/reports/harness/long-run-full.md` 得到 `12/12` 全胜、`nonVictoryCount: 0`、`zoneRouteHashDistribution` 只有 `1` 个 route hash。
- `LongRunLabTest` 的进阶职业 smoke 目标是 `ReachTerminal`，允许终局失败仍记成功，见 `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt:49-66`。
- `BossHarnessTest` 只有在显式传入 `expectedSelectedActionId` 时才要求 `AIDecisionTrace` 非空，当前三个 Boss case 都没传，见 `game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt:159-206`。
- 为什么这是当前必须解决的问题：
- 当前 user story 是“Phase3 完成态审阅”，不是“任务都定义了就算完成”。
- 如果门禁会把“太顺”“太线性”“trace 缺失”都当成成功，那它保护不了阶段边界。
- 为什么不能推到后续 Phase：
- Phase4 / Phase5 会继续把更多内容接到这些门禁上。
- 如果门禁的判断逻辑本身偏松，后续阶段只会更难定位体验退化。
- 不解决的后果：
- 团队会被“全绿”误导，以为当前长局已经足够成立。
- 真玩家遇到的问题会晚于开发阶段暴露，返工成本更高。

## 6. 优化建议（按优先级）

### P0-1 让 Objective / Quest / Optional Zone 从“字段”变成“玩法”

- 问题本质：世界推进合同名词已经齐全，但 objective 完成条件、optional zone 独有内容、zone mechanic 多数没有进入运行时。
- 影响范围：`core/world`、`game/FoundationGameSession.kt`、`game/resources/data/objectives/index.yaml`、`game/resources/data/zones/index.yaml`、相关 harness。
- 优化目标：把 Phase3 的长局结构从“地图更长”提升到“路线选择真的改变这段 run 在玩什么”。
- 具体改法：
- 让 `completionRule` 至少支持当前已出现的 `defeat_zone_boss / explore_floor_pair / secure_forge_path` 三类规则，并在运行时真正检查，而不是 zone clear 时整体自动完成。
- 让 `recordObjectiveProgress()` 写回 quest state 或 progress token，而不是只写日志。
- 给 4 个 optional zone 全部接入已有 interactable 机制，每个 zone 至少补 `1` 个独有交互点或独有 elite 触发。
- 给 `underground_river / abyssal_temple` 各补 `1` 个中后段 objective interactable，让 late-game 目标感不至于塌成“清图下楼”。
- 把 `uniqueContentTag` 至少接到一条实际逻辑上，例如独有 cache、独有精英、独有 route reward 子池。
- 优先级：P0
- 预期收益：分支的意义会从“多打一层”变成“多打一段不同内容”；晚期 mandatory zone 会重新获得节奏抓手。
- 可能副作用 / 风险：如果直接把 objective 做成硬门，会拉长 headless turn；需要同步调节 quest 完成门槛和 reward 发放时机。
- 需要同步修改：文档、数据、运行时代码、`longRunLab`、`ZoneContentCoverageTest`。

### P0-2 把路线 / Boss / 缓存奖励升级成带 affix 的里程碑奖励

- 问题本质：高价值奖励节点绕开了 affix 系统，导致 build 差异主要靠职业底盘，不靠 loot。
- 影响范围：`game/FoundationGameSession.kt` 奖励发放路径、`core/item/ItemGenerator.kt`、`core/item/AffixGenerator.kt`、loot data、balance/harness。
- 优化目标：让玩家在“打过一个关口”之后，真正拿到会改变 build 的奖励，而不是白板补件。
- 具体改法：
- 把 `claimRouteReward()`、`zoneRewardItem()`、`activeBossRewardItem()` 从 `toRuntimeItem()` 改成走 `ItemGenerator.generate(base, floor, affixContext)`。
- 对 route/boss/cache reward 增加最低品质与 affix 数下限，例如 checkpoint 之后保证至少 `MAGIC`，Boss 奖励可保证 `1~2` 个 affix。
- 保留 `shop` 的 rescue 物品固定性，不把兜底商店也随机化。
- 把 `RouteReward.rescueTags` 接到 affix bias，而不只是 UI 文案，让“选这条路”真的影响掉落方向。
- 优先级：P0
- 预期收益：路线决策与 Boss 击杀会真正带来 build 爽点；当前 affix v1 才算进入玩家主体验。
- 可能副作用 / 风险：数值波动会明显增大，需要补 boss/long-run balance smoke。
- 需要同步修改：文档、loot/route reward 配置、代码、balance/harness 报告。

### P0-3 先补到 Phase3 的最低内容底线，再谈“已完成”

- 问题本质：Phase3 的规则深度已经超过内容厚度，导致职业和敌人无法把系统潜力转成体验。
- 影响范围：职业 talent data、monster roster、Boss data、AI profile、战斗/掉落平衡。
- 优化目标：至少把基础职业树和常规敌人池补到能支撑长局 build/encounter 差异的水平。
- 具体改法：
- 先把 4 基础职业补到路线图最小口径 `64` talents；当前缺口是 `20`。
- 优先补齐目前只有 `3` 节点的 tree，再给每个职业至少补 `1` 条真正改变打法的中后段节点，而不只是数值抬升。
- 把 regular / elite monster 中“带 talent 的怪”从 `6` 提到至少 `12`，优先补 late-game 怪和 optional zone 独有怪。
- 给 `abyssal_guardian` 换掉与 `dungeon_lord` 高度重叠的技能组，让最终 Boss 有自己的战斗语言。
- 如果短期确实无法补齐全部内容，至少先明确回写 Phase3 权威文档，不要继续宣称“完成态”。
- 优先级：P0
- 预期收益：build 深度、遭遇差异和 Boss 记忆点会同时提升；Phase3 完成态才更接近文档预期。
- 可能副作用 / 风险：内容补量会牵一发而动全身，需要同步更新掉落、AI、golden 与 localization。
- 需要同步修改：Phase3 roadmap/PR 文档、数据、i18n、audio/visual manifest、harness。

### P1-1 重写长局与 Boss 门禁，让它们能识别“成功但不好玩”

- 问题本质：当前验证更像“稳定性 smoke”，不是“体验质量 gate”。
- 影响范围：`LongRunLabFullTest`、`LongRunLabTest`、`BossHarnessTest`、相关报告格式。
- 优化目标：让门禁能拒绝太线性、太容易、AI trace 缺失的版本。
- 具体改法：
- `long-run-full` 增加 branch-inclusive matrix，不再只跑 `FOUNDATION_ZONE_ROUTE`。
- 把 `afterDeepIronRatio` 的零失败情况改成 `N/A` 或直接 fail，避免“0 死亡 = 自动通过”。
- 对 `Spellblade` 增加至少 `1` 条固定 seed victory smoke，而不是只到 terminal。
- `bossHarness` 至少补一段“phase 切换后强制让 Boss 执行一次 phase-specific action”，要求 `AIDecisionTrace` 非空并与 warning 对齐。
- 优先级：P1
- 预期收益：阶段完成判断会更可信，后续扩展时更容易发现体验退化。
- 可能副作用 / 风险：测试波动和维护成本会上升，需要先把随机性控制好。
- 需要同步修改：checklist、test code、报告模板。

Part 4 继续给出剩余优化建议、可延后问题与最终结论。
