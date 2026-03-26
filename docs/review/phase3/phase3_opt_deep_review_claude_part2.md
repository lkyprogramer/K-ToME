# Phase3 深度审查报告（Part 2/4）

## 3. Phase3 设计实现一致性矩阵（续）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| 世界拓扑 / 11-zone baseline | `7 mandatory + 4 optional`，支持 route selection 与长局推进 | 已实现 | `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`; `game/src/main/resources/data/world/world_graph.yaml`; `game/src/main/resources/data/zones/index.yaml`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1645-1677` | 拓扑和 route selection 都在，问题不在 graph 缺失，而在分支内容厚度与后段玩法密度 | Medium |
| `WorldProgress / Quest / Gate / RouteReward` | Quest/Gate/Reward 是正式 runtime 合同，不再靠暗规则 | 偏离实现 | `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`; `game/src/main/resources/data/objectives/index.yaml`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1615-1628`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1679-1696`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1985-1998` | `completionRule` 只存在于 schema；`recordObjectiveProgress` 只写日志；quest 在 zone clear 时整体自动完成，目标系统缺少真实玩法约束 | Critical |
| 可选 zone 独有内容 | 每个 optional zone 至少有独有精英、独有奖励点或独有事件，不是换皮支线 | 偏离实现 | `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md` §4.4; `game/src/main/resources/data/zones/index.yaml`; `game/src/main/kotlin/com/ktome/game/GameModule.kt:757-810`; `game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt:15-23` | optional zone 只有 `uniqueContentTag` 这个数据字段；没有 objectiveSet，没有独立 interactable，`uniqueContentTag` 也没有 runtime 消费 | High |
| Affix v1 / 掉落驱动构筑 | `40` affix、build 相关性、路线/掉落/商店共同驱动 build 差异 | 偏离实现 | `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`; `core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1688-1696`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1865-1916`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5245-5258`; `build/reports/harness/long-run-full.md` | affix 主要作用于怪物掉落；Boss/路线/缓存奖励基本都回落成固定 base item，导致最该出爽点的奖励节点没有 build 变化 | Critical |
| 商店 / rescue economy | `2` shops + affordability / rescue guarantee，提供救火路径 | 已实现 | `game/src/main/resources/data/shops/index.yaml`; `game/src/main/kotlin/com/ktome/game/GameModule.kt:1380-1406`; `game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt:27-57` | 经济兜底合同成立，但 offer 池偏薄，主要承担保命功能，较少承担 build 分岔功能 | Medium |
| `RunSummary` / Profile history | 支撑解锁、run history、实验室复现与分析 | 部分实现 | `core/src/main/kotlin/com/ktome/core/profile/ProfileData.kt`; `core/src/main/kotlin/com/ktome/core/profile/ProfileManager.kt`; `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:479-494` | 结构化 summary 已有，但 `buildHash` 只覆盖装备 + active loadout + 铭文，不足以表达完整 build 与 race talent 差异 | Medium |
| Phase3 QA 门禁 | `bossHarness / longRunLab / soloClearLab / combatTraceGolden` 能真正证明 Phase3 完成态 | 偏离实现 | `docs/phase3/2026-03-13-phase3-verification-checklist.md`; `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt:22-167`; `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt:22-118`; `game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt:159-206`; `build/reports/harness/long-run-full.md`; `build/reports/harness/boss-harness.md` | 门禁能跑，但对“过于容易、过于线性、AI trace 缺失、进阶职业只到终局不胜利”都缺少否决力 | High |
| 世界地图 / 路线信息反馈 | route selection 应清晰展示推荐等级、奖励与路线价值 | 偏离实现 | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1344-1363`; `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:410-436`; `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt:270-281` | UI 直接展示 `lv3_5` / `MOVEMENT` / `CLEANSING` 等内部 token，信息质量明显低于 Phase3 应有标准 | Medium |
| New Game / Player Creation 信息面 | 玩家应清楚知道哪些职业/种族可玩、哪些只是未来 stub | 部分实现 | `client/src/main/kotlin/com/ktome/client/GameApp.kt:363-374`; `game/src/main/kotlin/com/ktome/game/GameModule.kt:75-108`; `client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt`; `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt` | locked / unavailable / frozen 选项都出现在同一入口里，技术上可用，但新手第一印象会被“还不能玩”的选项稀释 | Medium |

## 4. 当前阶段玩法体验总评

### 核心循环

当前循环已经完整到“能成局”：

- 进入 zone
- 探索房间与交互点
- 遭遇普通怪 / 精英 / Boss
- 获得掉落、碎片、路线奖励、商店补给
- 升级、分配属性与天赋、整理主动栏
- 进入下一段 world route

但这个循环最弱的环节不是探索，也不是战斗入口，而是“奖励如何改变下一段决策”。

直接判断：

- 前 `10~20` 分钟的体验基本成立。
- 进入 `deep_iron_pit` 之后，推动力开始越来越依赖“我还没通关”而不是“我刚拿到的东西让我想继续试”。
- 进入 `underground_river / abyssal_temple / abyssal_heart` 后，探索目标和奖励记忆点明显下滑。

原因很直接：

- 早期 mandatory zone 还有 objective interactable 与 shop 节点。
- 后段 mandatory zone 的 objectiveSet 虽然存在，但 `underground_river / abyssal_temple / abyssal_heart` 的 interactable 数分别是 `0 / 0 / 0`。
- 路线奖励与 Boss 奖励大多是固定白板物品，不足以把“打一关”转换成“我 build 变了”。

结论：

- 核心循环顺序是通的。
- 核心循环驱动力不够强。
- 最弱环节是“奖励 -> 构筑变化 -> 下一段决策”。

### 战斗体验

优点：

- 玩家职业侧的资源轴差异是真实存在的。`STAMINA / MANA / ENERGY / POSITIVE_ENERGY / HATE / EQUILIBRIUM` 都已经进入运行时与 HUD。
- 玩家主动技能、铭文、loadout、telegraph、状态系统之间已经形成了可操作的决策层。
- `SoloClearLab` 报告显示 6 职业都能完成固定场景，说明“职业完全不会动”这种低级问题已经过去了。

短板：

- 敌方决策密度明显偏低。`26` 个怪物里只有 `6` 个带 talent，绝大多数普通战斗仍接近“数值块 + 基础移动脚本”。
- AI profile 复用率高。`ai.chase.basic / ai.kite.basic / ai.guard.basic / ai.patrol.basic` 覆盖了大量怪物，导致实战压力的变化主要来自数值，不来自行为。
- Boss 辨识度不够。`dungeon_lord` 和 `abyssal_guardian` 共享 `war_cry / power_strike / charge` 核心技能包，phase 差异更多体现在 telegraph 与权重，而不是完全不同的战斗语言。

直接判断：

- 战斗“有意义”，但还没有足够多“让我记住这场战斗”的时刻。
- 玩家侧决策比敌人侧决策丰富得多，导致中后程战斗容易从“读局”退化成“执行标准解”。
- 目前不存在明显的“系统完全单调”问题，但存在明显的“敌方内容支撑不起 Phase3 长局时长”问题。

### 成长与构筑

当前成长不是没有，而是“方向对了，厚度不够”。

成立的部分：

- 升级、属性点、天赋点、种族天赋点、主动栏编辑、铭文装配都在。
- 职业资源轴和起手技能让 6 个可玩职业的前期体验有真实差异。
- `Berserker / Spellblade` 不是假职业，至少已经能被跑进实验室。

不成立的部分：

- Phase3 路线图要求基础职业正式树 `64` 天赋，当前只有 `44`。这不是“小缺口”，而是直接削弱构筑分叉数量。
- 两个可玩进阶职业都只有 `6` talents，离“可玩”不算假，但离“值得反复试 build”还很远。
- `long-run-full` 的 buildHash 显示，四个基础职业在 12 个组合里普遍仍围绕起手四技能与统一铭文包收束，说明成长更多是在强化现有套路，而不是打开新打法。

结论：

- 当前版本存在“成长感”。
- 当前版本缺少“成长后玩法被重写”的频率。
- 这会直接伤害耐玩性，因为玩家会感到自己在“变强”，但不太会感到自己在“变得不一样”。

### 奖励驱动

这是当前 Phase3 最大短板。

证据很清楚：

- 路线奖励通过 `claimRouteReward()` 发放 guaranteed drop，但直接走 `itemBaseDef(...).toRuntimeItem()`。
- Boss / cache / support 奖励也最终走 `rewardItemFromProfiles()` -> `officialRewardItem()` -> `toRuntimeItem()`。
- `toRuntimeItem()` 只复制 base item，不生成材质、不滚 affix。

这意味着：

- 玩家在最重要的奖励节点拿到的，多数不是“带 build 方向的成长件”，而是“适配本职业的固定白板件”。
- affix 系统被降格成普通掉落调味料，而不是构筑驱动器。
- 路线奖励的战略意义，更多是“保底补给 / 职业适配件”，不是“做选择换来打法变化”。

直接判断：

- 奖励发放频率不算低。
- 奖励层次是有的。
- 但奖励带来的决策波动太小，所以爽点不够。

### 探索与重复游玩价值

有基础，但后段塌陷。

正面：

- 11-zone baseline 已存在，玩家确实能在主线外做可选选择。
- world graph、gate、route reward、shop 都让长局具备基本骨架。

负面：

- optional zone 没有 objectiveSet，没有独立 interactable，`uniqueContentTag` 也没有 runtime 行为。
- `bandit_camp / elven_ruins / molten_core / crystal_cavern` 更像“附加两层地图 + 奖励节点”，不是足够自立的小体验段。
- 后段四个 zone 大量复用 `zone.grey_gate_depths.visual / ambient.grey_gate_depths / tileset.shadow_depths`，视觉与氛围新鲜感下降明显。

直接判断：

- 现在已经有“再来一局”的雏形。
- 但重复游玩的主要变量，仍更多来自职业选择，而不是路线、掉落、敌人组合。
- 这不够支撑一个被称为“Phase3 完成态”的 Roguelike 长局版本。

### UI / 反馈 / 新手体验

基础可用，信息质量不足。

已经成立的部分：

- 主菜单、角色创建、loadout、talent assign、shop、inspect、route selection 都有入口。
- Game Over / Victory 页面至少会回顾 zone、turn、level、last events。

不足的部分：

- 世界地图把内部 token 原样暴露给玩家，明显破坏理解门槛。
- player creation 直接展示 frozen / unavailable 选项，会把“当前能玩什么”稀释掉。
- 死亡信息能看，但还不够解释性；玩家知道“被谁杀”，未必知道“为什么这局在这里崩”。

### 系统联动性

规则层联动明显强于内容层联动。

强项：

- `combat -> status -> talent -> ai -> save/load -> renderSnapshot -> harness` 这一串是打通的。
- 这也是为什么项目看起来“完成度不低”。

弱项：

- `world / objective / route reward / optional zone / specialMechanics / uniqueContentTag` 这一串联动偏弱。
- 内容字段很多，但真正参与运行时决策的字段比文档承诺少。

总评一句话：

- 当前系统不是“各做各的”，但确实存在“规则层很完整，内容层只把其中一部分转成了真实玩法”的断层。

Part 3 进入“当前 Phase3 必须解决的问题”，只讨论现在就该修的结构性问题。
