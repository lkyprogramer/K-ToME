# Phase 2 深度审查吸收后的优化开发计划

**生成日期**: 2026-03-21
**最后审阅**: 2026-03-21（深度交叉验证后修订）
**适用基线**: `Sprint 0 ~ Sprint 7` 已完成、Phase 2 主门禁已恢复绿色的当前主线
**目标**: 吸收 `docs/review/phase2` 下 4 份深度审查文档中仍然有效的建议，把它们改写成一组可以直接执行的、边界清晰的 PR 级优化计划

---

## 1. 计划定位

这份计划不是重新做一遍 `P2-C` 收口，也不是把 Phase 3/4/5 的系统偷带回 Phase 2。

它只做三件事：

1. 核实深度 review 里的问题在当前代码基线上是否仍然成立
2. 把仍然有效的建议改写成适合当前代码现状的优化 PR
3. 明确哪些建议应该继续延后，避免后续又把跨阶段内容塞回 Phase 2

本计划默认遵守以下事实：

1. `P2-C` 核心闭环已经成立：掉落、4 zone route、四职业短局、formal-path required key 门禁、root gate 对齐均已完成
2. 后续优化不得破坏当前 `preReleaseAcceptance`、`clientSmoke`、`goldenScreenshot`、`soloClearLab`、`longRunLab`
3. 优化以"体验透明度、系统一致性、记忆点增强"为主；仅 `Stage A` 允许做一次边界明确的资源合同重构，其余阶段不重开大规模架构改造

---

## 2. Review 反馈核实矩阵

下表基于代码实际验证，而非文档自述。

| Review 项 | 来源 | 当前实际状态 | 验证依据 | 结论 |
|---|---|---|---|---|
| 怪物掉落未接入战斗循环 | Part 3 Critical-01 | **已解决** | `handleDeath()` L1851-1871 调用 `lootItemForMonsterDeath()` 并创建 ground item | 不进入新计划 |
| Zone Route 未实现 | Part 3 Critical-03 | **已解决** | `advanceToNextZoneInRoute()` 在 Boss 击杀后触发；`FOUNDATION_ZONE_ROUTE` 定义 4 zone 序列 | 不进入新计划 |
| statGrowth 未生效 | Part 3 High-03 | **已解决** | — | 不进入新计划 |
| 天赋解锁节奏过快 | Part 3 High-01 | **已解决** | — | 不进入新计划 |
| Rogue / Templar 未经实战验证 | Part 3 High-04 | **已解决** | — | 不进入新计划 |
| Blink levelEffects 空 | Part 3 Medium-03 | **已解决** | — | 不进入新计划 |
| DamageType 完全无差异 | Part 3 High-02 | **⚠️ 管线接通但数据为空** | `MonsterSchemaV2.resistances` 字段存在，`EntityFactory` L111-115 读取并创建组件，`DataLoader` L246 解析 YAML。**但 `monsters/index.yaml` 中 24 个怪物无一填写 `resistances` 字段**。抗性管线空转。 | **必须进入新计划**：既需要填充数据，也需要增强反馈 |
| STAMINA 资源双轨 | Part 3 Critical-02 | **桥接态可运行，但不适合作为最终形态** | 当前主线已经把部分读写迁到 `ResourcePools`，但仍保留 stamina 专用字段、`Stamina` 组件与双表达 save/view 合同。若继续沿此基线前进，后续所有 PR 都要承担资源双轨税。 | **必须进入新计划**：升级为 Stage A 资源合同重构 |
| 精英 / Boss 行为过于简单 | Part 3 Medium-01 | **部分解决** | AI 只有 `talentPriority` + `skipRules`（仅 2 个 Boss profile 有）。**`warning` 和 `telegraph` 不存在于 AI profile schema 中**——它们分别在 `interactables`（环境警告标签）和 `talents`（技能预览类型）中，不是 AI 行为触发器 | 进入新计划，但需要修正基线假设 |
| signature reward 仍像数值板 | Part 3 P2-3 | **未解决** | 4 件 reward 当前均为纯 `StatModifier` 数值（无被动效果框架），`ItemModels.kt` 无任何 trigger/passive/proc 机制 | 进入新计划 |
| 战斗日志不显示伤害类型 / 抗性效果 | Part 3 P2-1 | **未解决** | `DamageResult` 已携带 `type: DamageType` 和 `resistanceValue: Int`，但 session 日志输出未使用这些字段 | 进入新计划 |
| 升级反馈不够明确 | Part 3 P2-2 | **部分解决** | 已有 `log.level_up` / `log.talent.unlock` / audio cue，但不含属性增长摘要 | 进入新计划 |
| 掉落可视化 / 拾取反馈不足 | Part 3 P2-4 | **部分解决** | 已有 ground item 和 `log.loot.monster_drop`；仍缺质量/材料/词缀辨识度 | 进入新计划 |
| Death Recap 缺失 | Part 3 P2-6 | **⚠️ 基础框架已存在但信息不足** | `GameOverScreen.kt`（86 行）已实现，`RunSummary` 已有 5 字段（outcome/floor/maxFloor/turns/level）。`VictoryScreen.kt` 同样存在。缺失的是：击杀者、死因、最后关键事件、资源状态 | 进入新计划，但属"增强"而非"新建" |
| affix / material / difficulty placeholder budget | Part 1 / Part 4 | **有意保留** | 不阻塞当前阶段 | 可选后续项 |

### 2.1 核实中发现的关键事实修正

上表有 3 处与原计划的重大差异，需要在后续 PR 设计中吸收：

1. **DamageType 不是"大体解决"**：管线代码存在但数据层完全空白。`resistances` 字段在 24 个怪物中无一填写。这意味着 PR-B1 不仅是"增强反馈"，还必须包含**怪物抗性数据填充**，否则反馈无内容可展示。

2. **AI `warning`/`telegraph` 不存在于 AI profile**：`warning` 是 interactable 的环境标签（`data/interactables/index.yaml` 中 `tags: [warning]`），`telegraph` 是 talent 的预览类型（`data/talents/index.yaml` 中 `telegraph: melee_single`）。两者都不是 AI 行为触发器。PR-D1 需要**从零设计** typed trigger schema，而非"在现有基础上增加"。

3. **Death Recap 不是"缺失"而是"不足"**：`GameOverScreen.kt` 和 `RunSummary` 已经存在。`handleDeath()` 已接收 `killer: EntityId?` 参数但未传递给 `RunSummary`。PR-D2 是字段扩展和布线，不是整体新建。

### 2.2 本轮二次交叉验证补充（基于最新审阅报告）

在长期版计划回写后，又做了一轮代码交叉验证。确认以下补充结论需要同步吸收：

1. **`StatsCalculator` 不是 PR-A3 的主战场**
   - 当前 `recalculateAndStore()` 仍直接调整 `Health.current/max`
   - 但 `STAMINA` 路径已经不是直接写 `stamina.current`，而是通过 `StaminaPools.shiftMax(...)`
   - 它真正的问题是：仍以 `world.get<Stamina>(entity)` 作为分支条件，耦合了即将被删除的 `Stamina` 组件
   - 结论：这部分应归入 `PR-A2`；`PR-A3` 主要聚焦 `ExperienceSystem` 的 typed level-up 结果与 progression 编排

2. **`EntityFactory.createMonster()` 当前不为普通/精英怪创建 `TalentLoadout/CooldownState`**
   - Boss 走 `BossFactory` 已有这两个组件
   - 但普通/精英怪即使 schema 中配置 `talentLevels`，当前 factory 也不会注入运行时 talent 组件
   - 结论：`PR-D1` 必须显式纳入 `EntityFactory.kt`，否则 `forceTalent` 对非 Boss 怪物无法生效

3. **`DerivedStats.maxStamina/staminaRegen` 与 `StatModifierSnapshot.maxStamina/staminaRegen` 暂不纳入 Stage A 删除范围**
   - 它们目前属于规则层/数值层字段，不等同于 `currentStamina` 这类运行时状态重复合同
   - 结论：Stage A 删除的是“当前值与公开边界的双轨表达”，不是在本阶段顺手泛化所有 stamina 命名

4. **`energyCurrent` 不是和 `staminaCurrent` 同类的职业资源遗产字段**
   - `energyCurrent` 对应的是回合调度用 `Energy` 组件
   - 它不等于 `ResourceType.ENERGY` 的 Rogue 主资源池
   - 结论：Stage A 不应把 `energyCurrent` 误列为待删对象，但必须在文档里明确这层区分，避免后续 review 误判

5. **`SaveGame` 可以在战斗中触发**
   - 当前 `PlayerCommand.SaveGame` 直接调用 `persistRun()`，不要求脱战
   - 结论：`PR-D1` 的 `once: true` 不能默认只放进内存状态；只要 trigger 可能在战斗中途触发，就需要明确 save/load 语义

---

## 3. 吸收后的判断

### 3.1 已完成，不应重新打开

以下建议已经被 Sprint 1 ~ 7 吸收，后续不要再按"问题待修"处理：

1. 怪物掉落接入
2. 4 zone route 与 route save/load
3. statGrowth 生效
4. unlockLevel 节奏
5. Rogue / Templar 正式化与 SoloClearLab gate
6. formal-path required visual / audio 清零
7. gate 从 `P2-B` 重对齐到 `P2-C`

### 3.2 仍有效，但需要改写

深度 review 的以下建议仍有价值，但不能原样照搬：

1. **`STAMINA` 统一到 `ResourcePools`**
   - review 的方向是对的
   - 但在“长期优先、无兼容负担”的前提下，**不应停留在 `Stamina` 镜像兼容层**
   - 正确方向应是：删除 `Stamina` 作为正式运行时/存档合同，把 `ResourcePools(STAMINA)` 提升为唯一资源真相
   - **注意**：这不是局部修 bug，而是跨 `core / game / save / snapshot / view` 的合同清理；直接写 `stamina.current` 的旧路径和 stamina 专用字段都必须一起删除

2. **DamageType 反馈透明化**
   - review 说"系统已接通"，但实际是管线空转——24 个怪物全部没有 `resistances` 数据
   - 必须**先填充怪物抗性数据**，然后才能增强反馈显示
   - `DamageResult` 已有 `type` 和 `resistanceValue` 字段，日志侧只需接线

3. **Boss simple scripted AI**
   - 不建议提前引入完整相位系统或 AI DSL
   - **修正**：当前 AI profile 只有 `talentPriority` + `skipRules`，不存在 `warning`/`telegraph` 触发器
   - 实际需要：在 `ai/index.yaml` schema 中新增 typed trigger 字段（如 `onCombatStart`/`hpBelowRatio`），并在 AI 行为解析代码中添加 trigger 检查逻辑

4. **signature reward 特殊效果**
   - 不建议直接打开通用装备技能系统
   - 只给 4 件 zone signature reward 增加极小范围的 typed passive contract
   - **注意**：当前 `ItemModels.kt` 完全没有被动效果概念（只有 `StatModifier` 的 flat 数值），需要新增数据模型和运行时解析路径

5. **升级反馈**
   - 现在已经有日志和音频
   - 后续应补"属性变化 / 新解锁"清晰可读，而不是扩成新手引导系统

6. **Death Recap**
   - `GameOverScreen.kt` 和 `RunSummary` 已存在
   - 任务是扩展 `RunSummary` 字段（击杀者、死因、最后事件摘要、资源快照），不是新建
   - `handleDeath()` 已接收 `killer: EntityId?` 但未传递到 `RunSummary`——属于简单布线

### 3.3 继续延后，不进入本计划

以下建议继续维持 review Part 4 的延期判断：

1. 多阶段 Boss 战
2. 抗性递减 / 穿透递减公式
3. 高级难度模式
4. Meta Progression
5. 随机事件大系统
6. 套装 / 深词缀 / 复杂装备联动
7. 完整 AI Profile DSL
8. 动态战斗音乐
9. Event Bus / Observer 架构改造
10. `FoundationGameSession` 大规模拆解式重构

理由很简单：这些都已经越过"Phase 2 审查吸收优化"的边界，属于 Phase 3+ 的系统设计问题。

---

## 4. 规划原则

后续 PR 必须遵守以下规则：

1. **不重开已关闭问题**
   - route、掉落、formal-path required key、四职业 gate 不再当成优化项重复施工

2. **每个 PR 必须可独立验证**
   - 不接受"先铺底层，后面一起验证"

3. **优先增强单一真相和玩家可理解性**
   - 先修一致性和反馈，再做体验增益

4. **不在 client 里发明第二套规则**
   - 所有新反馈优先来自 typed snapshot / log token / summary，而不是 client 猜测

5. **避免一次引入完整新系统**
   - signature reward、simple scripted AI、death recap 都走最小 typed contract

6. **新增字段继续遵守 key / id / token 真源纪律**
   - `game/core` 只保存 `id`、`nameKey`、`messageKey + args`、typed token、数值快照
   - 不在 `RunSummary`、AI trigger、副作用日志、recap 中引入最终本地化字符串

7. **每个 PR 必须能独立回滚**
   - schema 变更、运行时接线、测试、文档必须在同一个 PR 内闭合
   - 如果某个 PR 合并后使长期 gate 变红，优先整体回滚该 PR，而不是在主干上多次“热修复”

8. **feature branch 隔离**
   - 每个 PR 使用独立 feature branch（如 `opt/pr-a1-stamina-unify`）
   - 合并到主线前必须通过 §10 全部门禁
   - 如果合并后 gate 变红，立即 revert 并修复后重提

### 4.1 单一真相红线（补 review 中已踩过的坑）

以下内容后续不得在 harness、lint、golden 或测试内重新手写一份局部常量：

1. **官方短局 route**
   - 统一从 `FOUNDATION_ZONE_ROUTE` 或其正式导出入口读取
   - `headlessSmoke`、`longRunLab`、`FullGameLoop`、route coverage test、白盒脚本都不得各自维护一份 `["shattered_outpost", ...]`

2. **zone -> objectiveSetId 绑定**
   - 统一从 zone catalog / loader 结果读取
   - coverage test 允许断言“精确绑定结果”，但不允许先在测试里再定义一张平行映射表作为业务真源

3. **required formal-path key 清单**
   - visual/audio 必检 key 统一来自脚本真源（如 `asset_pipeline_common.py` 中的 required 集）
   - 单个测试可以验证“这些 key 必须能 resolve”，但不能各自复制一份长期维护的 key 列表

4. **RunSummary / recap 展示字段**
   - client screen 只消费 typed `RunSummary`
   - 不允许 screen 再回头从 session/world 临时抓 zone、killer、resource 做第二套拼装

5. **AI trigger 消费状态**
   - 若引入 `AiTriggerState`，save/load、session、harness 必须共用同一份 typed 状态
   - 不允许测试用本地布尔变量“模拟 once 已消费”

6. **STAMINA 资源合同**
   - `STAMINA` 的正式真相只能是 `ResourcePools(STAMINA)`
   - 不允许保留 `Stamina` 组件、`staminaCurrent`、`staminaCost`、`currentStamina/maxStamina` 作为第二套长期合同
   - 不包括 §2.2-3 明确保留的规则层内部字段（如 `DerivedStats.maxStamina/staminaRegen`）

### 4.2 常见反模式（后续 review 直接据此拦截）

1. 在测试里硬编码四区 route 字面量，而不是引用正式 route 常量
2. 在 harness 里复制 objective 绑定或 required key 清单
3. 在 `client` 里把 key/token 再格式化成长期字段存回 model
4. 在 `game` 里为了 UI 方便生成最终文案字符串
5. 在 save/load 之外额外维护“仅进程内可见”的行为状态
6. 通过新增 fixture 常量绕过正式 schema / loader / manifest 真源
7. 为了“平滑迁移”继续保留 `Stamina` 镜像写路径或双格式 save contract
8. 在 view / summary / snapshot 中保留 stamina 专用字段，同时再维护 generic resource 字段

---

## 5. 分阶段 PR 计划

下面的阶段顺序已经按风险和依赖排过。

### Stage A：资源合同重构

> 本阶段不再采用“兼容镜像迁移”思路。  
> 目标不是把旧双轨跑通，而是**删除 stamina 专用遗产合同**，为后续 `B/C/D` 阶段清掉结构性阻力。

#### Stage A 最终目标态

1. `ResourcePools(STAMINA)` 是唯一资源真相
2. `Stamina` 组件从正式运行时删除
3. `TalentDef / TalentSlotView / PlayerStatus / SaveSnapshot` 中的 stamina 专用字段删除
4. save contract 显式抬版本，旧 save fail fast
5. `StatsCalculator` 不再依赖 `Stamina` 组件存在性，升级相关编排通过 typed progression result 显式挂到 session

#### 为什么要从“迁移”升级成“重构”

如果仍保留 `Stamina` 组件、`staminaCurrent`、`staminaCost`、`currentStamina/maxStamina` 等专用字段，那么后续每一个优化 PR 都要继续支付 4 类长期成本：

1. `core` 规则层需要同时理解 generic resource 与 stamina 特化字段
2. `game`/save/load 继续承受两套结构表达
3. `client`/snapshot/view 继续承受通用资源与 stamina 专用 view 的重复
4. review 与 harness 继续需要判断“这次改的是正式真相，还是兼容层”

在明确“不考虑兼容”的前提下，这些成本都没有保留价值。

#### PR-A1：删除 stamina 专用合同字段

**目标**

先清合同，再动运行时。把 stamina 专用字段从稳定边界移除，让后续所有调用点只能走 generic resource 语义。

**要删除或改写的合同**

1. [TalentModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt)
   - 删除 `TalentDef.staminaCost`
   - `TalentDef` 只保留 `resourceCosts: Map<ResourceType, Int>`
   - `resolvedResourceCosts()` 不再从 `staminaCost` 兜底
2. [GameView.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameView.kt)
   - 删除 `PlayerStatus.currentStamina/maxStamina`
   - 删除 `TalentSlotView.staminaCost`
   - `PlayerStatus` 只保留通用战斗/成长字段，资源视图统一由 `PlayerResourceView` 表达
   - `TalentSlotView.resourceCost/resourceTypeId` 成为正式字段，不再依赖 `staminaCost` 的默认值
3. [SaveSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt)
   - 删除 `EntitySnapshot.staminaCurrent`
   - `EntitySnapshot` 只保留 `resourcePools`
4. [SaveContractVersion.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/save/SaveContractVersion.kt)
   - 抬版本，明确这是**有意破坏兼容**的合同清理
5. [TalentModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt)
   - 同步删除 `defaultResourceCosts(...)` 与 `resolvedResourceCosts()` 中对它的 fallback 调用

**建议范围**

- `core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt`
- `core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt`
- `core/src/main/kotlin/com/ktome/core/save/SaveContractVersion.kt`
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
- `game/src/main/kotlin/com/ktome/game/GameView.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt`
- `core/src/test/kotlin/com/ktome/core/talent/TalentResolverTest.kt`
- `game/src/test/kotlin/com/ktome/game/harness/SmokeBotTest.kt`
- `game/src/test/kotlin/com/ktome/game/harness/ScenarioModelsTest.kt`
- 对应 codec / mapper / test

**冻结决策**

1. `STAMINA` 继续是 `ResourceType`，但不再有任何 stamina 专用并行字段
2. `saveContractVersion` 直接升级，旧存档 fail fast，不做迁移桥
3. `PlayerStatus` 不再承载资源本体，统一由 `PlayerResourceView` / snapshot generic 字段表达
4. `CombatProfile.baseStamina`、`DerivedStats.maxStamina/staminaRegen`、`StatModifierSnapshot.maxStamina/staminaRegen` 仍保留现有命名；它们在本阶段被视为规则层内部数值字段，不属于要删除的“当前资源状态双轨合同”

**非目标**

1. 不把 `HEALTH` 并入资源池
2. 不引入通用 `Vitals` 框架
3. 不在这一 PR 内同时重做所有 runtime 逻辑
4. 不删除 `energyCurrent`；它对应的是行动经济 `Energy` 组件，而不是职业资源池

**验收标准**

1. 正式代码中不再存在 `staminaCost`、`staminaCurrent`、`currentStamina/maxStamina`
2. 编译期就无法再写出旧 stamina 特化调用
3. save contract 版本抬升后，旧 save 明确 fail fast
4. `DataLoader.toRuntimeTalent()` 不再从 `resourceCosts["STAMINA"]` 回填 `staminaCost`
5. `TalentSlotView` 构造路径显式传入 `resourceCost/resourceTypeId`，不再依赖 stamina 默认值
6. `defaultResourceCosts(...)` 与任何 `staminaCost` fallback 逻辑都已删除

**验证**

- `./gradlew :core:test`
- `./gradlew :game:test`
- `./gradlew preReleaseAcceptance`

#### PR-A2：删除 `Stamina` 组件并统一运行时到 `ResourcePools`

**目标**

把运行时从“`Stamina` + `ResourcePools`”双表示，收敛成“只有 `ResourcePools(STAMINA)`”。

**核心调整**

1. [Components.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/ecs/Components.kt)
   - 删除 `Stamina` 组件
2. [StaminaPools.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/resource/StaminaPools.kt)
   - 取消所有 `world.get<Stamina>()` / `seedFromComponent()` / `syncComponentFromPool()` 语义
   - 要么重写成 `ResourcePools(STAMINA)` 的纯便捷委托，要么直接删除并让调用方改用 `ResourcePools`
3. [EntityFactory.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt)
   - 玩家创建时直接建 `ResourcePools(STAMINA)`
4. [BossFactory.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/factory/BossFactory.kt)
   - Boss 不再注入 `Stamina`
5. [TalentResolver.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/talent/TalentResolver.kt)
   - `resourceAmount/spendResources` 全量使用 `ResourcePools`
6. [StatsCalculator.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt)
   - 删除 `world.get<Stamina>(entity)` 的条件分支
   - `STAMINA` 上限调整改为直接以 pool 为前提，不再依赖组件存在性
7. [InventoryManager.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/item/InventoryManager.kt)
   - STAMINA 恢复药剂只写 `ResourcePools`
8. [ExperienceSystem.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/progression/ExperienceSystem.kt)
   - 删除 `stamina: Stamina?` 参数与组件 fallback 分支
   - 升级后的主资源恢复只走 `ResourcePools`
9. [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
   - regen / armory / HUD / inspect / snapshot 全走 pool
   - `prepareActorTurn()` 的 STAMINA 回复不再以 `world.get<Stamina>` 为前置
   - `gainExperience()` 不再 `requireNotNull(world.get<Stamina>(playerId))`
10. [PlayerResourceService.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/PlayerResourceService.kt)
   - 删除 `syncStaminaPoolFromComponent` 等兼容追赶逻辑
   - 玩家正式资源池合同的 fail-fast 校验位于 `game` 的 load 边界，而不是 `core.save`；原因是职业到主资源的映射属于 `game schema` 责任
11. [SessionSnapshotMapper.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt)
   - capture/restore 只处理 `resourcePools`

**实现原则**

1. 所有 `STAMINA` 读写必须通过 `ResourcePools(STAMINA)`
2. 不允许新增“helper 内部仍写组件，再同步池”的折中桥接
3. 初始化点必须显式且唯一：创建玩家 / 恢复存档时就保证 pool 存在
4. `StatsCalculator` 在本 PR 中只负责清掉对 `Stamina` 组件存在性的耦合，不把它扩写成新的 progression/service 入口
5. `StatsCalculator.recalculateAndStore()` 删除 `world.get<Stamina>(entity)` 条件后，不能改成无条件 `shiftMax()`；新 guard 必须是“该实体已持有 `ResourcePools(STAMINA)` 时才调 STAMINA max 同步”

**非目标**

1. 不顺手改 `MANA/ENERGY/POSITIVE_ENERGY` 的平衡或算法
2. 不顺手重命名所有 resource 相关类型
3. 不在这一 PR 内扩展 `ExperienceGainResult` 或引入新的 progression typed result

**验收标准**

1. 正式运行时代码里不再引用 `world.get<Stamina>()`
2. `soloClearLab`、`longRunLab`、`preReleaseAcceptance` 通过
3. save/load、HUD、snapshot、player view 全部只依赖 `ResourcePools`
4. `ExperienceSystem` 已不再接受 `Stamina?` 参数，也不存在组件 fallback 分支
5. `StaminaPools` 已不再能通过任意调用“复活” `Stamina` 组件
6. `SessionSnapshotMapper` 的旧 `staminaMax <- staminaCurrent` fallback 整段恢复逻辑已直接删除，不做保留式修复

**验证**

- `./gradlew :core:test`
- `./gradlew :game:test`
- `./gradlew soloClearLab`
- `./gradlew longRunLab`
- `./gradlew preReleaseAcceptance`

#### PR-A3：升级编排 typed 化（收缩版）

**目标**

把升级相关的“规则计算结果”和“session 编排副作用”拆开，让 `PR-B2` 的升级反馈、后续资源 recap 和 progression 日志有稳定的 typed 挂载点。

**推荐调整**

1. [ExperienceSystem.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/progression/ExperienceSystem.kt)
   - 不再直接修改 `Health` / `ResourcePools`
   - 扩展现有 `ExperienceGainResult`，把它从“事后报告”提升为 typed progression result，例如：

```kotlin
data class ExperienceGainResult(
    val experience: Experience,
    val levelsGained: Int,
    val gainedStatPoints: Int,
    val gainedTalentPoints: Int,
    val shouldRestoreHealthToMax: Boolean,
    val shouldRestorePrimaryResourceToMax: Boolean,
)
```

2. [StatsCalculator.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt)
   - **不是本 PR 主战场**
   - 如果 `PR-A2` 已完成，本 PR 对 `StatsCalculator` 只允许做极小整理，不再把它作为“需要去副作用”的主要对象
3. [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
   - 统一负责：
     - 等级提升后的回满策略
     - 属性变化后的当前值 clamp / delta 应用
     - profession 主资源的恢复编排

**为什么这一 PR 需要独立存在**

如果不做这一步，后续：
- `PR-B2` 的升级反馈
- `PR-C1` 的被动效果
- `PR-D2` 的资源 recap

都会继续耦合在 `gainExperience()` 的具体实现细节上。  
这一步的价值不在于再清一次 `Stamina`，而在于给 progression 相关增强建立稳定 typed result。

**非目标**

1. 不重写完整战斗/成长系统
2. 不引入新的 service 层框架
3. 不做 Phase 3 的公式升级
4. 不把 `StatsCalculator` 扩展成新的万能编排入口
5. 本阶段不把 `Experience` 本身改造成纯值返回；Phase 2 只提纯 HP / Resource 的回满副作用，`Experience` 对象的直接修改保持不变

**验收标准**

1. `ExperienceSystem` 返回稳定 typed progression 结果，session 明确消费该结果并完成回满/日志编排
2. `PR-B2` 不再依赖 `gainExperience()` 内部隐式副作用顺序
3. 对升级、属性变化、save/load 的行为测试仍然稳定

**验证**

- `./gradlew :core:test`
- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest --tests com.ktome.game.RenderSnapshotContractTest --tests com.ktome.game.SessionSnapshotMapperTest`
- `./gradlew soloClearLab`
- `./gradlew longRunLab`

#### Stage A 总体验收标准

1. 不再存在 `Stamina` 组件
2. 不再存在 stamina 专用合同字段
3. save contract 已显式抬版本，旧 save fail fast
4. 资源读写、save/load、HUD、snapshot、talent cost 全部只走 generic resource contract
5. `soloClearLab`、`longRunLab`、`preReleaseAcceptance` 全绿

#### Stage A 风险提示

1. 这是本计划里唯一一个真正意义上的“架构级清理阶段”
2. 会触碰 `core + game + save + snapshot + tests`，影响面显著大于原版 PR-A1
3. 但它完成后，`B/C/D` 的复杂度和返工概率都会明显下降

---

### Stage B：DamageType 激活 + 反馈透明化

#### PR-B1：怪物抗性数据填充 + DamageType 反馈可视化

**目标**

分两步走：(1) 填充怪物抗性数据，让 DamageType 在运行时产生实际差异；(2) 在日志中展示伤害类型和抗性效果，让玩家可以感知这些差异。

**为什么必须同时做数据和反馈**

当前状态是：
- `MonsterSchemaV2.resistances` 字段已存在（`SchemaModels.kt` L172）
- `EntityFactory` L111-115 会读取 `resistances` 并创建 ECS 组件
- `CombatResolver` 已有抗性减免公式（`finalDamage * (1.0 - resistance/100.0)`）
- `DamageResult` 已携带 `type: DamageType` 和 `resistanceValue: Int`
- **但 `monsters/index.yaml` 中 24 个怪物全部没有 `resistances` 数据**

管线完整但数据为空 = 系统空转。只做反馈没有内容可展示。

**建议范围**

- `game/src/main/resources/data/monsters/index.yaml` ← 核心：填充抗性数据
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` ← 日志输出增强
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `core/src/test/kotlin/com/ktome/core/combat/CombatResolverTest.kt` ← 抗性/脆弱路径断言
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- `game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt` ← 验证抗性加载

**建议怪物家族抗性配置**

基于 review Part 3 P1-2 的最小抗性方案，抗性值控制在 -25 ~ +25 范围（避免线性公式极端情况）：

| 怪物家族 | FIRE | COLD | LIGHTNING | HOLY | SHADOW | 设计意图 |
|----------|------|------|-----------|------|--------|----------|
| beast | 0 | 0 | 0 | 0 | 0 | 基准线，纯物理取向 |
| undead | 10 | 10 | 0 | -25 | 20 | 惧怕神圣，抗冰火，亲暗 |
| bandit | 0 | 0 | -10 | 0 | -10 | 人形，略弱雷/暗 |
| goblin | -10 | 0 | 0 | 0 | 0 | 略弱火 |
| orc | 0 | -10 | 0 | 0 | 0 | 略弱冰 |
| cultist | 0 | 0 | 0 | -15 | 15 | 惧怕神圣，亲暗 |

**建议日志增强做法**

1. 保持现有抗性公式不变
2. 在 session 的战斗日志输出中，使用 `DamageResult.type` 和 `DamageResult.resistanceValue` 生成可读提示
3. 新增 i18n key 示例：
   - `log.combat.damage_typed`: "{attacker} deals {damage} {damageType} damage to {target}"
   - `log.combat.resisted`: "({target} resists {reduced} {damageType})"
   - `log.combat.vulnerable`: "({target} is vulnerable! +{bonus} {damageType})"
4. 抗性值为 0 时不显示额外提示（保持简洁）
5. inspect 面板可选显示目标关键抗性摘要，但不做完整百科界面
6. `FoundationGameSession` 只输出 token / key+args，不在 game 层拼接最终文本
7. 基础普通攻击固定走 `PHYSICAL` 路径；元素抗性反馈只要求在带 `DamageType` 的 talent 命中时出现，不为普通近战额外注入元素类型

**数据冻结建议**

1. Phase 2 只给 route 主线怪物补“家族级弱差异”抗性，不做模板级大幅偏离
2. 单个抗性值限制在 `[-25, +25]`，避免线性公式在短局里形成极端免疫/秒杀
3. 同一怪物模板本阶段最多给 2 个非零元素抗性，保持玩家可读性
4. `MonsterSchemaTest` 应冻结最小家族覆盖线，而不是冻结每只怪的精确数值，避免后续小调参造成低价值破坏

**非目标**

1. 不做抗性递减 / 穿透递减公式
2. 不做完整 combat trace UI
3. 不改 `CombatResolver` 核心公式

**验收标准**

1. 至少 `undead / bandit / goblin / orc / cultist` 5 个家族各有 1 个模板具备非零抗性，`beast` 保留 0 抗性基线
2. 非 `0` 抗性命中时，日志存在可读提示（正抗性 = resisted，负抗性 = vulnerable）
3. 脆弱 / 抗性两种路径都有自动化断言（`CombatResolverTest`）
4. `MonsterSchemaTest` 验证抗性字段正确加载，并冻结最小家族覆盖线
5. `goldenScreenshot` 中日志区域可以稳定体现新反馈

**验证**

- `./gradlew :core:test --tests com.ktome.core.combat.CombatResolverTest`
- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest --tests com.ktome.game.data.MonsterSchemaTest`
- `./gradlew soloClearLab`
- `./gradlew goldenScreenshot`

#### PR-B2：升级 / 解锁 / 属性变化反馈增强

**目标**

把当前"只有 `log.level_up` 和 `log.talent.unlock`"的最小反馈，增强成玩家能立即读懂"自己变强了什么"。

**建议范围**

- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`

**建议做法**

1. `gainExperience()` 升级分支中，在 `addMessage("log.level_up", ...)` 之后，增加 `statGrowth` 摘要日志：
   - `log.level_up.stats`: "+{str} STR, +{dex} DEX, +{con} CON, +{wil} WIL"（只输出非零项）
2. 新解锁 talent 的 `log.talent.unlock` 在升级日志之后紧跟输出（当前可能已如此，需确认顺序）
3. 增加 `log.level_up.hp_max` / `log.level_up.resource_max` 摘要（最大 HP 和最大资源的变化量）

**依赖说明**

- `PR-B2` 应在 `PR-A3` 之后实施
- 原因不是资源合同本身，而是 `PR-A3` 会把升级流程 typed 化；`PR-B2` 直接挂在这个 typed 结果上，比继续绑定 `gainExperience()` 的隐式副作用顺序更稳

**非目标**

1. 不做完整天赋树升级界面重做
2. 不做新手引导系统
3. 不在 client 侧增加弹窗或 toast 组件——所有反馈走日志

**验收标准**

1. 升级时至少能看到等级、属性增长、新解锁 talent 名称
2. 四职业的升级日志都经过 `FoundationGameSessionTest` 最小场景覆盖
3. 属性增长全为 0 时不输出多余空行

**验证**

- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest`
- `./gradlew soloClearLab`

**与 PR-B1 的并行性分析**

PR-B1 和 PR-B2 都修改 `FoundationGameSession.kt` 和 i18n 文件。但：
- PR-B1 修改的是**战斗日志输出路径**（`resolveMelee` 结果处理区域）
- PR-B2 修改的是**升级日志输出路径**（`gainExperience` 区域）
- 两者在 `FoundationGameSession.kt` 中接触的代码区域不重叠
- i18n 文件是 JSON，新增 key 不冲突

**结论：可以并行，但建议在分支创建时先 rebase 到同一基线，合并时先入 PR-B1 再入 PR-B2**。

---

### Stage C：奖励辨识度与奖励身份

#### PR-C1：4 件 signature reward 的最小被动效果合同

**目标**

让 `bandit_trophy / emerald_charm / furnace_talisman / seal_reliquary` 不再只是略高数值的 accessory，而是有可感知的路线记忆点。

**当前状态**

通过代码验证确认：
- 4 件 reward 在 `items/index.yaml` 中定义为 `ARMOR` 类型、`OFF_HAND` 槽位
- 只有 `StatModifier` 数值（如 `dex: 1, critChance: 0.04`）
- `ItemModels.kt`（`core/item`）完全没有被动效果概念——只有 `StatModifier` 的 flat 数值加成
- 没有任何 trigger、proc、conditional buff 基础设施

**这意味着什么**

PR-C1 不是"给现有字段填数据"，而是需要**引入一个最小的被动效果子系统**。这是本计划中唯一需要新建模型层的 PR，复杂度显著高于其他 PR。

**建议范围**

- `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt` ← 新增 `EquipmentPassive` sealed class
- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` ← 新增 `EquipmentPassiveSchemaV2`
- `core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt` ← 新增：解析装备被动
- `game/src/main/resources/data/items/index.yaml` ← 4 件 reward 增加 `passive` 字段
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` ← 解析 `passive` 字段
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` ← 在战斗/回合逻辑中调用 passive 解析
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` ← `inspectItemView()/itemDetailLines()` 将被动描述落到现有 `InspectItemView.details`
- `game/src/main/resources/i18n/en-US.json` ← 被动效果描述
- `game/src/main/resources/i18n/zh-CN.json`
- `core/src/test/kotlin/com/ktome/core/item/PassiveEffectResolverTest.kt` ← 新增
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- `game/src/test/kotlin/com/ktome/game/SessionSnapshotMapperTest.kt` ← 验证 passive 不引入第二套 save 语义

**建议被动效果模型（最小 sealed class）**

```kotlin
sealed interface EquipmentPassive {
    data class DamageVsTag(val tag: String, val bonusPercent: Double) : EquipmentPassive
    data class HpRegenPerTurn(val amount: Int) : EquipmentPassive
    data class DamageTypeBonus(val type: DamageType, val bonusPercent: Double) : EquipmentPassive
    data class ResistanceBonus(val damageType: DamageType, val amount: Int) : EquipmentPassive
}
```

只支持这 4 种效果，不做通用 proc 框架。

**schema / 存档边界**

1. `passive` 只允许出现在明确的 base item schema 上，不允许 affix / material 动态注入
2. save/load 不新增独立 passive 持久化字段；继续只存 `baseId/materialId/affixIds`，被动效果由 base item schema 恢复解析
3. inspect / tooltip / log 对被动效果的说明优先来自 schema key，不在 client 侧硬编码解释文案

**建议映射**

| 物品 | 被动类型 | 效果 | 设计意图 |
|------|---------|------|---------|
| `bandit_trophy` | `DamageVsTag("bandit", 0.15)` | 对 `bandit` 标签目标 +15% 伤害 | zone 1 记忆——征服匪巢的战利品 |
| `emerald_charm` | `HpRegenPerTurn(2)` | 每回合回复 2 HP | zone 2 记忆——林地治愈 |
| `furnace_talisman` | `DamageTypeBonus(FIRE, 0.15)` | FIRE 输出 +15% | zone 3 记忆——熔炉赐福 |
| `seal_reliquary` | `ResistanceBonus(SHADOW, 10)` | SHADOW 抗性 +10 | zone 4 记忆——圣印护佑 |

**依赖**

- **强依赖 PR-B1**：`furnace_talisman` 的 FIRE 加成和 `seal_reliquary` 的 SHADOW 抗性加成，只有在怪物有对应抗性数据时才有意义
- **与 A1 的关系仅是全局基线前置**：`emerald_charm` 的 `HpRegenPerTurn` 作用于 `HEALTH`，C1 的被动效果本身不涉及 `STAMINA` 路径

**实现集成点**

被动效果的生效位置：
1. `DamageVsTag`：通过 `MonsterTemplateId -> template.tags` 反查 schema 标签，不新增运行时 Tags 组件
2. `DamageTypeBonus`：在 session 层先计算 `damageMultiplier`，再传给 `CombatResolver`
3. `HpRegenPerTurn`：在回合开始的 `onTurnStart()` 中，遍历装备被动
4. `ResistanceBonus`：不修改 `CombatResolver` 签名；在 session 的装备/属性重算编排点为玩家创建/更新有效 `ResistanceProfile`，把装备提供的额外抗性叠加到角色当前抗性组件上
5. inspect 展示走现有 `InspectItemView.details` 路径，不要求为 PR-C1 新增专门的 inspect DTO；若后续需要更强结构化展示，再单开后续 PR

**非目标**

1. 不做通用装备技能树
2. 不做触发器链、套装、复杂 proc
3. 不做随机被动词缀生成——只有 4 件 hardcoded 映射

**验收标准**

1. 4 件 signature reward 至少各有 1 个能在实战中验证的独特效果
2. 不引入第二套装备语义解释路径——`StatModifier` 仍然是基础加成，`EquipmentPassive` 只是额外效果
3. `PassiveEffectResolverTest` 对 4 种效果都有单元覆盖
4. `SoloClearLab` 仍应通过，且 `longRunLab` 不因被动效果引入而出现明显平衡偏移

**验证**

- `./gradlew :core:test --tests com.ktome.core.item.PassiveEffectResolverTest`
- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest --tests com.ktome.game.SessionSnapshotMapperTest`
- `./gradlew soloClearLab`
- `./gradlew longRunLab`

**风险提示**

- 这是唯一需要新建核心模型层的 PR，修改面横跨 core 和 game
- `CombatResolver` 是最频繁测试的路径——引入 modifier 需要确保不破坏现有战斗平衡
- 建议：被动效果的数值刻意保守（如 15% 而非 50%），Phase 3 再调参

#### PR-C2：掉落与拾取反馈增强

**目标**

把 review 中"掉落接通了，但玩家仍然不够容易理解地上东西值不值得拿"的问题收敛掉。

**建议范围**

- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` ← 掉落/拾取日志增强
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- `game/src/test/kotlin/com/ktome/game/RenderSnapshotContractTest.kt`

**建议做法**

1. 不做阻断式"掉落确认弹窗"
2. 改做：
   - 掉落日志包含品质信息：`log.loot.monster_drop_quality`: "{monster} drops a {quality} {item}"
   - 拾取日志显示完整构成名（品质 + 材料 + 基础名 + 词缀摘要）
   - inspect 显示 material 和 affix 可读名称
3. 品质对应 i18n key：`item.quality.common` / `item.quality.magic` / `item.quality.rare`
4. inspect 与掉落展示如果要进入 Tile UI，应同步命中 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`，避免只在 game 层补日志却不更新 inspect 可读性

**验收标准**

1. 玩家能从日志区分普通掉落与更高价值掉落
2. pickup/drop 日志不再丢失质量/材料/词缀信息
3. `FoundationGameSessionTest` 覆盖至少一个 MAGIC/RARE 品质掉落场景的日志断言

**验证**

- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest --tests com.ktome.game.RenderSnapshotContractTest`
- `./gradlew soloClearLab`

---

### Stage D：遭遇差异化与失败可学习性

#### PR-D1：精英 / Boss simple scripted 行为增强

**目标**

在不引入完整 Boss phase system 或 AI DSL 的前提下，让 Phase 2 路线中的精英与 Boss 至少具有可辨认的战术身份。

**当前 AI 现状（代码验证）**

`ai/index.yaml` 中 12 个 AI profile，当前只有以下能力：
- **全部 profile**：`id` + `schemaVersion`（无行为差异）
- **仅 2 个 Boss**（`ai.boss.dungeon_lord`, `ai.boss.bandit_captain`）有 `talentPriority` + `skipRules`
- **0 个 profile** 有 `warning`、`telegraph` 或任何条件触发器

`telegraph` 实际存在于 `talents/index.yaml`（talent 预览类型如 `melee_single`、`charge_lane`），不是 AI 行为字段。
`warning` 实际存在于 `interactables/index.yaml`（环境警告标签），不是 AI 行为字段。

**这意味着什么**

PR-D1 需要**新增 typed trigger schema 字段到 AI profile**，并在 AI 行为解析代码中实现 trigger 检查逻辑。这不是"在现有基础上增加"，而是"设计并实现一个最小 trigger 系统"。

**建议范围**

- `game/src/main/resources/data/ai/index.yaml` ← schema 扩展：新增 trigger 字段
- `game/src/main/resources/data/bosses/index.yaml`
- `game/src/main/resources/data/monsters/index.yaml` ← 精英怪物 AI profile 分配
- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` ← AI profile schema 模型扩展
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` ← 解析新字段
- `game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt` ← 为带 monster talents 的非 Boss 怪注入 `TalentLoadout/CooldownState`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` ← AI 决策逻辑中集成 trigger 检查
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- `game/src/test/kotlin/com/ktome/game/harness/SoloClearLabSupport.kt`
- `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt`
- `game/src/test/kotlin/com/ktome/game/data/SchemaV2LoaderTest.kt` ← 新 schema 字段加载验证

**建议 trigger 最小模型**

在 AI profile schema 中新增可选的 `triggers` 列表：

```yaml
# ai/index.yaml 示例
- id: ai.boss.bandit_captain
  schemaVersion: 2
  talentPriority: [war_cry, power_strike, shield_bash]
  skipRules:
    - talentId: war_cry
      selfHasStatus: WAR_CRY_BUFF
  triggers:
    - triggerId: bc_enrage_40
      condition: hpBelowRatio
      threshold: 0.4
      action: forceTalent
      talentId: war_cry
      postMessageKey: log.boss.enrage
      once: true
    - triggerId: bc_opening_shield_bash
      condition: onCombatStart
      action: forceTalent
      talentId: shield_bash
```

只支持 2 种 condition：
1. `onCombatStart`：怪物回合开始且首次检测到敌对目标在视野内时触发
2. `hpBelowRatio`：HP 低于阈值时触发

额外支持 1 个执行修饰符：
1. `once: true`：每场战斗只触发一次

只支持 1 种主 action：
1. `forceTalent`：强制使用指定天赋

每个 trigger 允许带一个可选副作用：
1. `postMessageKey + postMessageArgs`：在主 action 之后输出日志 token

副作用日志不允许裸字符串，必须使用 `messageKey + args` 形式，以保持 i18n contract 单一真源。

**建议覆盖目标**

| 实体 | trigger | 行为 | 体验效果 |
|------|---------|------|---------|
| `bandit_captain` | `onCombatStart` | `forceTalent: shield_bash` | 开场先手防御——有节奏感 |
| `bandit_captain` | `hpBelowRatio(0.4)` | `forceTalent: war_cry` + `postMessageKey: log.boss.enrage` | 残血狂暴——紧张感 |
| `cultist.dungeon_lord` | `onCombatStart` | `forceTalent: war_cry` | 开场 buff——Boss 气场 |
| `cultist.dungeon_lord` | `hpBelowRatio(0.3)` | `forceTalent: charge` + `postMessageKey: log.boss.desperate` | 残血反扑——危险信号 |
| `bandit.wild_huntmaster` | `hpBelowRatio(0.5)` | `forceTalent: power_strike` | 精英怒击——区分于普通怪 |
| `orc.forge_guard` | `onCombatStart` | `forceTalent: shield_bash` | 精英开场压制——建立矿坑守卫身份 |

日志副作用参数示例：

```yaml
    - condition: hpBelowRatio
      threshold: 0.4
      action: forceTalent
      talentId: war_cry
      postMessageKey: log.boss.enrage
      postMessageArgs:
        sourceNameKey: monster.bandit.captain.name
      once: true
```

**实现前置**

1. 当前选定的 elite / boss 里，除两个 boss 外，大多 `talents: {}`；PR-D1 必须同步在 `monsters/index.yaml` 中补入最小 talent 配置
2. `EntityFactory.createMonster()` 必须为带 monster talents 的精英/怪物注入 `TalentLoadout + CooldownState`
   - `BossFactory` 已有这两个组件；PR-D1 需要把非 Boss 路径补齐，而不是只改 YAML
3. trigger 不允许指向怪物当前未配置或 resolver 不支持的 talent
4. `DataLoader` 或 session 初始化阶段应 fail fast 校验 `forceTalent` 是否存在于该怪物可用 talent 集
5. 当前 `SaveGame` 可在战斗中触发，因此 `once: true` 的消费状态必须有明确 save/load 语义，不能默认只放进内存

**推荐的最小持久化设计**

默认采用最小 typed 状态，而不是进程内布尔缓存：

```kotlin
data class AiTriggerState(
    val consumedTriggerIdsByActor: Map<EntityId, Set<String>>,
)
```

约束：
1. `triggerId` 优先作为 schema 显式字段写在 YAML 中，而不是依赖加载器临时拼接
2. 同一 `aiProfile` 内 `triggerId` 必须唯一；必要时增加 loader/lint 校验
3. save/load 恢复后，已消费的 `once: true` trigger 不能重复触发
4. 怪物死亡、战斗结束或 zone 切换后，状态必须清理，不能污染下一场遭遇

当前前提说明：
1. `SessionSnapshotMapper` 目前按 `snapshot.id` 预创建实体，因此 save/load 后 `EntityId` 保持稳定
2. 基于这个前提，Phase 2 中 `AiTriggerState` 继续以 `EntityId` 作为 key 是可接受的
3. 若后续 save/load 改成重新分配实体 ID，则必须同步提升 trigger state key 设计

**非目标**

1. 不做多阶段 Boss
2. 不做完整行为树
3. 不做 Phase 3 的 AIProfile DSL 扩展
4. 不改变基础怪物的 AI——只增强 Boss 和 elite

**验收标准**

1. Boss 战不再等价于"HP 更高的 CHASE 怪"
2. 至少 2 个 elite + 2 个 boss 的行为差异能被 harness 观测到
3. `SchemaV2LoaderTest` 验证新 trigger 字段正确加载
4. trigger 检查逻辑有独立单元测试
5. `once: true` 的触发消费在 save/load 后语义稳定，不会因读档重复触发或丢失触发
6. `soloClearLab` 和 `longRunLab` 不因 AI 增强产生回归

**验证**

- `./gradlew :game:test --tests com.ktome.game.data.SchemaV2LoaderTest`
- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest`
- `./gradlew soloClearLab`
- `./gradlew longRunLab`
- `./gradlew headlessSmoke`

**风险提示**

- AI trigger 直接影响战斗体验和 SoloClearLab 通过率
- 如果 Boss 在错误时机强制使用高伤天赋，可能导致 SoloClearLab 回归
- `EntityFactory` 一旦开始为非 Boss 怪创建 `TalentLoadout/CooldownState`，精英遭遇的战斗循环会真实改变；需要额外关注 SoloClearLab 与 headlessSmoke 基线
- 建议：trigger 的 `once: true` 默认开启，避免无限循环；threshold 保守设置

#### PR-D2：RunSummary 增强（Death Recap + Victory Recap）

**目标**

把"知道自己死了/赢了，但不知道关键细节"的反馈问题，通过扩展已有的 `RunSummary` 和 `GameOverScreen`/`VictoryScreen` 解决。

**当前已有基础（代码验证）**

| 组件 | 状态 | 位置 |
|------|------|------|
| `GameOverScreen.kt` | 已存在，86 行 | `client/screen/GameOverScreen.kt` |
| `VictoryScreen.kt` | 已存在 | `client/screen/VictoryScreen.kt` |
| `RunSummary` | 5 字段：outcome/floorReached/maxFloor/turns/playerLevel | `game/GameView.kt` L105-111 |
| `RunOutcome.Defeat` | 有 floor 和 reason（默认 "player_died"） | `core/run/RunOutcome.kt` |
| `handleDeath(target, killer)` | 接收 killer EntityId 但未传递到 RunSummary | `FoundationGameSession.kt` L1826 |
| i18n | 已有 `ui.game_over.title/floor_reached`、`ui.summary.turns_taken/final_level` | 两种语言都有 |

**这意味着什么**

不需要新建 screen 或数据结构。任务是：
1. 扩展 `RunSummary` 增加一组 typed recap 字段
2. 在 `handleDeath()` 中捕获 killer 信息
3. 在 `GameOverScreen`/`VictoryScreen` 中展示新字段

**建议范围**

- `game/src/main/kotlin/com/ktome/game/GameView.kt` ← `RunSummary` 字段扩展
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` ← 在 `handleDeath`/`runSummary()` 中填充新字段
- `client/src/main/kotlin/com/ktome/client/screen/GameOverScreen.kt` ← 展示新字段
- `client/src/main/kotlin/com/ktome/client/screen/VictoryScreen.kt` ← 展示新字段
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`

**建议 RunSummary 扩展字段**

```kotlin
data class RunSummary(
    val outcome: RunOutcome,
    val floorReached: Int,
    val maxFloor: Int,
    val turns: Int,
    val playerLevel: Int,
    // --- 新增 ---
    val zoneNameKey: String,
    val killerNameKey: String?,
    val killerTemplateId: String?,
    val finalHpCurrent: Int,
    val finalHpMax: Int,
    val finalResourceTypeId: String,
    val finalResourceCurrent: Int,
    val finalResourceMax: Int,
    val lastEvents: List<RenderTextTokenSnapshot>,
)
```

**建议做法**

1. `FoundationGameSession` 维护一个 `recentSummaryEvents: RingBuffer<RenderTextTokenSnapshot>(5)` 或等价结构
   - 每次 `addMessage()` 都追加
   - `runSummary()` 构造时读取当前快照
   - 不单独做 save/load 恢复；如果玩家在较早时刻存档、继续推进后才死亡/胜利，结算页只需要反映终局前最后几步事件即可
2. 在 `handleDeath()` 中，把运行时 `killer: EntityId?` 归一化为 `killerTemplateId` / `killerNameKey`，**不要把 `EntityId` 带进 summary**
3. `runSummary()` 构造时，从 session 当前状态读取 zone key、HP、资源、最近 token 事件
4. `GameOverScreen` / `VictoryScreen` 通过 `app.text(token.key, ...)` 渲染，不存最终本地化字符串
5. `RunSummary` 继续保持 typed summary，不引入第二套文案真源
6. `RunOutcome.Defeat.reason` 继续作为 typed reason key 使用；如需细化死因，优先补 reason key，不补裸字符串字段

**展示裁剪规则**

1. `lastEvents` 最多保留最近 `5` 条 token，避免结算页变成日志终端
2. 只展示与死亡/胜利直接相关的最后几条事件，不回放完整战斗历史
3. 若 `killerNameKey` 为空（如环境伤害或自损），UI 回退到 `RunOutcome.Defeat.reason`，不展示空占位
4. 若 `lastEvents` 为空（如读档后首回合即死亡），screen 层隐藏该区域，不显示空列表占位
5. Victory 与 Defeat 共用同一份 `RunSummary` 结构，避免两套 recap 字段漂移

**非目标**

1. 不做 Phase 5 的完整 `DeathAnalysis`
2. 不做 replay 回放式复盘
3. 不做伤害来源统计/饼图

**验收标准**

1. `GameOverScreen` 能显示"死在谁手里"和"最终 zone"
2. `VictoryScreen` 能显示最终 zone 和关键统计
3. `RunSummary` 中的新增展示字段继续保持 key / token 语义，不引入最终本地化字符串真源
4. 信息来自 typed `RunSummary`，不是 client 临时抓世界状态
5. `FoundationGameSessionTest` 断言 `runSummary()` 在 defeat 场景包含 killer 信息和最近 token 事件

**验证**

- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest`
- `./gradlew :client:test`
- `./gradlew goldenScreenshot`

---

### Stage E：可选后续项（不阻塞前四阶段）

#### PR-E1：affix / material / difficulty formalization 决策与资产预算处理

**目标**

只在后续要明显增强物品体验时再启动，决定 `25 visual / 13 audio` debug budget 是否需要升级为正式资产。

当前已触发并执行的决策是：

1. 由于 `Stage C / PR-C2` 已把 `material / affix` 明确带入 inspect、inventory list 与 log token 的正式玩家路径，继续让这批 key 停留在 `missing_visual / silence.ogg` debug budget 中会让“正式语义已成立、资源仍算可降级”这两套口径并存
2. 因此 `Phase 2` 当前集合内的 `affix / material / difficulty` key 统一升级为 minimal formal assets
3. 本次 formalization 只要求脱离 fallback budget，并允许复用现有正式图片/音频；更细粒度的专属 affix/material 资产制作继续留给 `Phase 3/4`

**为什么放到最后**

1. 当前它们不阻塞任何 Phase 2 完成态 gate
2. 先做资产，后面如果物品体验方向变了，会重复劳动

**触发条件**

1. Stage C（PR-C2）要求在 UI 中明确展示 material / affix，发现 placeholder 视觉严重影响体验
2. 或 Phase 3 内容包准备显著扩展装备生态

当前采用的是触发条件 1。

**验证**

当前这 4 个 lint task 都已作为 root Gradle task 存在：
- `./gradlew assetLint`
- `./gradlew styleLint`
- `./gradlew audioLint`
- `./gradlew manifestLint`

完成态预期：

1. `phase2PlaceholderBudget` 从 `25` 收敛到 `1`（仅保留 `missing_visual` sentinel）
2. `phase2SilenceBudget` 从 `13` 收敛到 `1`（仅保留 `audio.fallback.silence` sentinel）

---

## 6. 依赖关系图与建议执行顺序

```
PR-A1 (删除 stamina 专用合同字段)
  │
  ├── PR-A2 (删除 Stamina 组件，运行时统一到 ResourcePools)
  │     │
  │     └── PR-A3 (升级编排 typed 化，session 显式消费 progression result)
  │           │
  │           ├── PR-B2 (升级反馈)
  │           ├── PR-D1 (AI trigger)
  │
  ├── PR-B1 (DamageType 数据+反馈)
  │     │
  │     └── PR-C1 (signature passive) ←── 强依赖 B1
  │
  ├── PR-C2 (掉落反馈) ←── 依赖 B1 的掉落辨识度基线
  │
  └── PR-D2 (RunSummary 增强，建议在 D1 后)

PR-E1 (可选资产 formalization，触发条件见 Stage E)
```

推荐执行顺序：

| 序号 | PR | 前置依赖 | 预估相对复杂度 |
|------|-----|---------|------------|
| 1 | PR-A1 | 无 | 中高（合同删改 + save 版本抬升） |
| 2 | PR-A2 | A1 完成 | 高（运行时真相重构） |
| 3 | PR-A3 | A2 完成 | 中高（规则/编排职责拆分） |
| 4a | PR-B1 | A1 完成 | 中（数据填充 + 日志增强） |
| 4b | PR-B2 | A3 完成 | 低（纯日志增强） |
| 5a | PR-C1 | A1 + B1 完成（建议在 A2 后启动以减少 session 冲突） | 高（新子系统引入） |
| 5b | PR-D1 | Stage A 完成 | 中高（新 trigger schema + 解析逻辑） |
| 6a | PR-C2 | A1 + B1 完成（建议在 A2 后启动以减少 session 冲突） | 低（日志增强） |
| 6b | PR-D2 | A1 完成（建议在 D1 后） | 低（字段扩展 + 布线） |
| — | PR-E1 | 触发条件满足时 | 视情况 |

原因：

1. 长期版 Stage A 不再是一个 PR，而是先清合同、再清运行时、最后清职责边界
2. `PR-A2/A3` 没完成前，不建议启动 `B2/D1`；`C1/C2` 从依赖上可在 `A1+B1` 后启动，但若 `A2` 正在重写 session/resource 路径，建议延后以减少冲突
3. `PR-B1` 仍然是 `PR-C1` 的直接上游，因为 FIRE/SHADOW 被动依赖抗性数据
4. `PR-B2` 应在 `PR-A3` 之后启动；一旦进入 typed progression 基线后，可以和 `PR-B1` 并行
5. `PR-D1` 与 `PR-C1` 仍不建议并行——两者都改变战斗体验和长期 harness 稳定性
6. `PR-D2` 业务上独立，只要求建立在 `PR-A1` 的新 save/view 合同之上；建议晚于 `PR-D1` 只是为了减小 `FoundationGameSession.kt` 冲突

---

## 7. 建议并行度

在依赖满足后，可以并行的组合：

1. `PR-B1` 可在 `PR-A1` 合并后独立启动；`PR-A2/A3` 与 `PR-B1` 可分支并行推进，`PR-B1` 合并入主线时不要求等待 `PR-A2/A3`
2. `PR-C1/C2` 从依赖上可在 `A1+B1` 后启动；若 `A2` 正在改 `FoundationGameSession.kt`，建议只并行准备、不立即合并
3. `PR-B1` 与 `PR-B2`（A3 完成后）
4. `PR-D2` 可以在 A1 后独立启动，但不建议与另一个重 `FoundationGameSession.kt` 的 PR 同时落地
5. `PR-D1` 可以在 `PR-B1` 进行时同步启动，但不建议和 `PR-C1` 同时进行

不建议并行：

1. `PR-A1` 与任何后续 Stage PR
   - 它改 save/view 合同，是所有后续 PR 的新基线
2. `PR-A2` 与 `PR-A3`
   - 前者改正式运行时真相，后者改升级编排 typed result；同时做只会放大定位成本
3. `PR-A2` 与任何重 `FoundationGameSession.kt` / `PlayerResourceService.kt` 的 PR
   - 包括 `C1`、`D1`、以及任何试图提前落地的 runtime 反馈 PR
4. `PR-C1` 与 `PR-D1`
   - 两者都可能影响战斗体验判断与 SoloClearLab 稳定性
   - 如果同时引入被动效果和 AI trigger，回归时难以定位原因

---

## 8. 每个优化 PR 的统一交付模板

后续按本计划实施时，每个 PR 描述建议都按同一模板落，避免再次出现“范围没写死、验收没写死、回滚点不明确”：

1. **目标**
   - 只写本 PR 改善什么，不写泛化愿景
2. **非目标**
   - 明确哪些相邻系统本 PR 不碰
3. **代码触点**
   - 文件列表按 `core / game / client / tools / docs` 分组
4. **真源引用**
   - 写清楚本 PR 引用的 canonical source 是什么，例如 `FOUNDATION_ZONE_ROUTE`、loader 输出、required key 脚本集合
   - 如果出现“测试或 harness 也需要知道同一事实”，默认要求复用这个真源，而不是复制字面量
5. **冻结决策**
   - 本 PR 新增的 key / schema / token / summary 字段有哪些硬约束
6. **自动化验证**
   - 最小必跑命令
7. **白盒验证**
   - 若需要 client 观察，列出固定脚本和语言
8. **回滚策略**
   - 如果主 gate 变红，是整 PR revert，还是允许局部开关回退

建议 PR 标题格式：

```text
[P2-OPT][PR-A1] Remove stamina-specialized contracts
[P2-OPT][PR-A2] Delete Stamina component and unify runtime resource state
[P2-OPT][PR-D1] Add minimal AI trigger contract for elites and bosses
```

---

### 8.1 推荐提交拆分（降低单 PR 风险）

下表不是强制 git 历史模板，但建议按这个粒度拆提交，便于 review、回滚和回归定位：

对 `PR-A1 / PR-A2` 这类“删除旧定义”的重构，必须遵守一个额外顺序：**先迁移全部直接引用并恢复编译，再删除旧字段/旧组件定义**。否则会与“每个提交都应保持编译可过”的原则冲突。

| PR | 推荐提交 1 | 推荐提交 2 | 推荐提交 3 | 推荐提交 4 |
|----|-----------|-----------|-----------|-----------|
| PR-A1 | 删除 stamina 专用合同字段 + 同步修复 `DataLoader/playerStatus/SessionSnapshotMapper/TalentSlotView` 直接编译断点 | save contract version 抬升 + codec / mapper 测试 | grep 清理旧字段引用 + `TalentResolverTest/SmokeBotTest/ScenarioModelsTest` fixture 迁移 | 文档同步 |
| PR-A2 | 将全部 `Stamina` 组件读写点切换到 `ResourcePools`（`StatsCalculator/ExperienceSystem/InventoryManager/FoundationGameSession/PlayerResourceService/SessionSnapshotMapper/factory`） | 删除 `Stamina` 组件定义 + `StaminaPools` 桥接语义 | 测试迁移（`StatsCalculatorTest/InventoryManagerTest/BossFactoryTest/EntityFactoryTest` 等） | save/load、SoloClearLab、longRunLab 集成验证 |
| PR-A3 | `ExperienceSystem` 改为返回 typed resolution | session 消费 typed progression 结果并完成回满/日志编排 | 升级/属性变化测试补齐 | 文档同步 |
| PR-B1 | 怪物抗性数据填充 + schema/test 冻结 | session 日志 token 接线 | i18n + golden/update | 文档与 review 记录 |
| PR-B2 | level-up / unlock / growth 日志接线 | i18n + game test | golden 或白盒记录 | — |
| PR-C1 | `EquipmentPassive` 模型 + schema/loader | 4 件 reward 数据 + session 集成 | core/game 测试补齐 | 文档与平衡注释 |
| PR-C2 | drop/pickup/inspect 命名接线 | snapshot / Tile inspect 若需要同步更新 | test + golden / 白盒记录 | — |
| PR-D1 | AI trigger schema + loader + fail-fast 校验 | runtime 触发判定与消费状态 | harness / save-load / regression test | 文档与审查记录 |
| PR-D2 | `RunSummary` 字段扩展 + session 填充 | GameOver/Victory screen 展示与 i18n | test + golden | 文档同步 |

拆分原则：

1. schema / model 变更优先独立成首个可审提交
2. runtime 接线与测试尽量不要揉成一个巨提交
3. 若引入新的 typed state（如 `AiTriggerState`），save/load 与回归测试必须和 runtime 接线在同一 PR 内闭合
4. 每个提交都应保持编译可过，避免“中间提交不可用”

### 8.2 Reviewer 快速检查清单

后续每个优化 PR 在 code review 时，至少逐项检查：

1. 是否复用了现有 canonical source，而不是在测试/harness/screen 里复制常量
2. 是否把 key / token / id 在 `game/client` 层错误地降级成最终字符串真值
3. 是否新增了 save/load 无法表达的运行时隐式状态
4. 是否把 Phase 3+ 的系统边界偷偷带入了 Phase 2 优化 PR
5. 是否自动化验证与白盒验证都写清楚了，而不是只写“理论上应该通过”
6. 是否存在“测试为方便直接写旧真值”的 setup，例如继续改 `Stamina.current`、直接构造 `staminaCurrent`、或继续断言 `currentStamina/maxStamina`
7. 若改动触碰 harness / longRunLab / smoke，是否确认 route、objective 绑定、required key 仍只来自单一真源
8. 若改动触碰 Stage A，是否真的删除了遗产合同，而不是引入“新真相 + 旧镜像”双轨
9. 若改动删除了 save field，是否同步抬升版本并让旧 save 明确 fail fast

### 8.3 PR 级禁止事项速查表

| PR | 明确禁止 |
|----|---------|
| PR-A1 | 保留 `staminaCost` / `staminaCurrent` / `currentStamina/maxStamina` 作为“临时兼容字段”；只删 model 不抬 save version |
| PR-A2 | 删除 `Stamina` 组件后又在 helper / fixture / harness 中重造镜像；继续让 save/load 同时解析两套 stamina 真相；保留 `ExperienceSystem` 的组件 fallback |
| PR-A3 | 把 `ExperienceSystem` 变成更大的万能 service；或为了 typed result 再反向污染 `StatsCalculator` / session 边界 |
| PR-B1 | 新增伤害类型、引入穿透/递减公式、在 client 里拼抗性说明真值 |
| PR-B2 | 新增弹窗/引导系统、把升级反馈做成 client 私有状态 |
| PR-C1 | 打开通用 proc 系统、让 affix/material 注入 passive、为 passive 增加独立存档格式 |
| PR-C2 | 做阻断式拾取确认 UI、在 client 新造第二套物品命名规则 |
| PR-D1 | 引入行为树/phase system、用脚本字符串解释 trigger、把 `once` 状态只放进内存 |
| PR-D2 | 在 `RunSummary` 存最终本地化字符串、让 screen 直接抓 world/session 拼 recap |

---

## 9. 阶段完成定义（DoD）

### Stage A DoD

1. 正式代码中不再存在 `Stamina` 组件与 stamina 专用合同字段
2. `ResourcePools(STAMINA)` 成为唯一资源真相；save/load、HUD、snapshot、talent cost 全部只走 generic resource contract
3. `saveContractVersion` 已抬升，旧 save 明确 fail fast，不再存在兼容桥
4. `ExperienceSystem` 不再承担升级后的隐式回满副作用；升级相关编排改由 session 基于 typed result 显式完成
5. `soloClearLab`、`longRunLab`、`preReleaseAcceptance` 通过

### Stage B DoD

1. route 主线怪物已有最小抗性覆盖，DamageType 不再空转
2. 升级日志能说明“变强了什么”
3. `goldenScreenshot` 中日志差异可见且稳定

### Stage C DoD

1. 4 件 signature reward 各自形成可验证记忆点
2. 掉落/拾取反馈可区分价值层次
3. 不引入第二套装备存档或解释路径

### Stage D DoD

1. 至少 2 个 elite + 2 个 boss 有可观测 scripted identity
2. GameOver / Victory recap 可解释，不依赖 client 临时抓世界状态
3. save/load 后 AI trigger 与 recap 语义稳定

### 全计划 DoD

1. `PR-A1 ~ PR-A3` 与 `PR-B1 ~ PR-D2` 全部合入后，`preReleaseAcceptance` 仍为绿色
2. 不新增第二套资源真相、第二套文案真相、第二套 AI 状态真相
3. 官方 route、objective 绑定、required formal-path key 不出现第二套测试内真相
4. 所有新增 schema / key / token / summary 字段都有测试和文档落点
5. 未越界引入 Phase 3+ 系统（phase/boss DSL/meta progression/通用 proc 框架）

---

## 10. 每个 PR 都必须补的门禁

### 10.1 自动化

所有 PR 必须通过：

- `./gradlew :core:test`
- `./gradlew :game:test`
- `./gradlew preReleaseAcceptance`（包含 clientSmoke / goldenScreenshot / soloClearLab / longRunLab / headlessSmoke）

按改动面追加：

| PR | 额外重点验证 |
|-----|------------|
| PR-A1 | `TalentResolverTest`、`SmokeBotTest`、`RenderSnapshotContractTest`、`SessionSnapshotMapperTest`、`SchemaV2LoaderTest`、save contract version / fail-fast 场景 |
| PR-A2 | `TalentResolverTest`、`ExperienceSystemTest`、`StatsCalculatorTest`、`InventoryManagerTest`、`BossFactoryTest`、`EntityFactoryTest`、`FoundationGameSessionTest`、`RenderSnapshotContractTest`、`SessionSnapshotMapperTest`、`soloClearLab`、`longRunLab` |
| PR-A3 | `ExperienceSystemTest`、`FoundationGameSessionTest`、`RenderSnapshotContractTest`、`preReleaseAcceptance` |
| PR-B1 | `CombatResolverTest`、`MonsterSchemaTest`、`goldenScreenshot` 日志区域 |
| PR-B2 | 四职业升级日志覆盖 |
| PR-C1 | `PassiveEffectResolverTest`（新增）、`soloClearLab`、`longRunLab` 平衡不显著漂移 |
| PR-C2 | `RenderSnapshotContractTest`、至少 1 个 RARE 掉落场景 |
| PR-D1 | `SchemaV2LoaderTest`、`EntityFactoryTest`、trigger 单元测试（新增）、SoloClearLab 通过率 |
| PR-D2 | `RunSummary` 字段断言、`goldenScreenshot` GameOver 画面 |

### 10.2 白盒验证

按 PR 类型分级，不做一刀切：

**PR-A1**（合同删改类）：
1. 使用一个旧 `saveContractVersion` 存档样例，确认加载时直接 fail fast，而不是静默回退
2. 启动新局，确认 HUD/角色面板不再依赖 stamina 专用 view 字段

**PR-A2**（运行时真相切换类）：
1. 使用 Vanguard 或 Rogue 连续释放消耗 `STAMINA` 的技能，确认 HUD 资源条、快照、读档后数值一致
2. 在 armory/升级/回合回复后分别确认资源恢复都只体现为同一条资源值变化

**PR-A3**（升级编排 typed 化类）：
1. 触发一次升级，确认加点/升级后的 HP 与主资源恢复由 session 编排触发，表现与日志不丢失
2. 在属性变化后观察当前值 clamp 行为，确认没有出现“max 变了、current 没跟上”或反向越界

**PR-B1/B2、PR-C2**（反馈增强类）：
1. 一次英文，一次中文
2. 确认日志中能看到新增的反馈文本

**PR-C1**（被动效果类）：
1. 带 `bandit_trophy` 打 bandit 确认伤害加成日志
2. 带 `emerald_charm` 确认回合回复
3. 带 `furnace_talisman` 对有 FIRE 抗性的怪确认加成
4. 带 `seal_reliquary` 确认 SHADOW 防护

**PR-D1**（AI 行为类）：
1. 观察 Boss 战是否有开场/残血行为变化
2. 确认精英遭遇和普通怪遭遇有可感知的区别
3. 在触发过一次 `once: true` 后立刻存档再读档，确认 trigger 不会重复触发

**PR-D2**（结算类）：
1. 至少一次 defeat 场景——确认 GameOverScreen 显示 killer 和 zone
2. 至少一次 victory 场景——确认 VictoryScreen 显示完整统计

---

### 10.3 失败时优先排查清单

自动化 gate 变红时，优先按下面顺序排查，避免在错误层面来回打补丁：

| PR | gate 红点 | 优先排查 |
|----|----------|---------|
| PR-A1 | 编译断点或 save fail-fast 场景异常 | 是否还有 `staminaCost` / `staminaCurrent` / `currentStamina` 残留引用；`SaveContractVersion`、serializer、mapper 是否同步修改；`playerStatus()` / `TalentSlotView` 构造是否仍依赖旧字段；`TalentResolverTest/SmokeBotTest` fixture 是否还在构造旧字段；grep 清理时注意区分删除目标与保留的 `DerivedStats.maxStamina` / `StatModifier.maxStamina` / `StatModifierSnapshot.maxStamina` |
| PR-A2 | HUD / snapshot / save-load 不一致 | 是否仍有 `world.get<Stamina>()` 残留；`StaminaPools` 是否还会自动重建组件；`StatsCalculator` 是否错误地把 `shiftMax()` 改成无条件调用而为非玩家实体创建空 pool；`ResourcePools(STAMINA)` 是否在新开局/读档时统一初始化；`ExperienceSystem` 是否还保留组件 fallback；`gainExperience()` 是否还直接抓 `Stamina`；`SessionSnapshotMapper` 旧的 `staminaMax <- staminaCurrent` 恢复逻辑是否被整段删除而不是修补后保留 |
| PR-A3 | 升级/属性变化日志或回满顺序异常 | `ExperienceSystem` 是否真的返回 typed result；session 是否仍偷偷依赖旧副作用顺序；`PR-B2` 是否直接绑死在 `gainExperience()` 内部实现细节上 |
| PR-B1 | 战斗日志没显示抗性 / golden 漂移 | 怪物是否真的加载到 `resistances`；日志是否输出 token 而不是字符串；0 抗性路径是否错误地也输出了提示 |
| PR-B2 | 升级反馈缺字段或顺序错 | `gainExperience()` 是否在升级后统一输出；全 0 增长是否被正确裁剪；unlock log 是否仍落在同一升级事务里 |
| PR-C1 | SoloClearLab / longRunLab 回归 | passive 是否重复生效；`DamageTypeBonus` / `ResistanceBonus` 是否和 B1 数据面一致；save/load 是否把 passive 解析成第二套状态 |
| PR-C2 | inspect / 掉落命名不一致 | 游戏层命名与 Tile inspect 是否共用同一套 item display 组装逻辑；quality/material/affix 是否有缺失字段回退 |
| PR-D1 | boss/elite 行为异常或读档重复触发 | trigger schema 是否与怪物 talent 实际配置一致；`EntityFactory` 是否真的为非 Boss 怪注入 `TalentLoadout/CooldownState`；`once: true` 是否被持久化；战斗结束后 trigger state 是否清理 |
| PR-D2 | GameOver/Victory 信息缺失 | `handleDeath()` 是否真的传入 killer 归一化结果；`recentSummaryEvents` 是否在 summary 构建前被覆盖或清空；screen 是否只消费 `RunSummary` |

通用排查顺序：

1. 先确认 canonical source 是否仍唯一
2. 再确认 typed token / key / id 没被中途降级成字符串
3. 最后才看 client 展示层是否渲染错误

### 10.4 文件冲突热点矩阵

以下文件是后续多个 PR 最容易产生 merge conflict 的位置，建议并行开发时提前避让：

| 热点文件 | 易冲突 PR | 冲突原因 | 建议 |
|---------|----------|---------|------|
| `core/.../TalentModels.kt` / `game/.../GameView.kt` / `core/.../SaveSnapshot.kt` | A1 | Stage A 合同删改都集中在这里 | A1 独占这些文件，不要和任何下游 PR 并行触碰 |
| `core/.../resource/StaminaPools.kt` | A2 | 这是当前 `Stamina` ↔ `ResourcePools` 的唯一桥接层；A2 不处理它就无法真正删除组件 | A2 独占，不允许并行触碰 |
| `core/.../Components.kt` / `game/.../EntityFactory.kt` / `game/.../BossFactory.kt` / `game/.../SessionSnapshotMapper.kt` | A2 | 删除 `Stamina` 组件、重写初始化与 save/load 恢复路径 | A2 完成前不要启动任何依赖新资源合同的 runtime PR |
| `core/.../ExperienceSystem.kt` / `core/.../StatsCalculator.kt` / `game/.../FoundationGameSession.kt` | A2 / A3 | A2 要清组件依赖与调用路径，A3 要改升级编排 typed result；两阶段都会碰这些文件 | A2/A3 严格串行；若有其他 PR 误碰这些文件，优先等待 A 线落完 |
| `game/.../EntityFactory.kt` / `game/.../monsters/index.yaml` | D1 | 非 Boss 怪 talent 运行时装配与 schema 配置需要同时改 | D1 合并前必须同时验证 factory 注入与 YAML talentLevels，不要只改其中一侧 |
| `game/.../FoundationGameSession.kt` | B1 / B2 / C1 / C2 / D1 / D2 | 几乎所有反馈、AI、summary 接线都在这里 | 同时只允许一个“重 session”PR 进入最终 rebase；其余尽量先落 schema/test |
| `game/.../DataLoader.kt` | A1 / B1 / C1 / D1 | A1 要清 `staminaCost` 兼容读取，后续 schema 扩展也都要改 loader | A1 完成前不要并行启动新的 loader schema PR；之后按 schema family 分块提交，避免一个 PR 混多个 schema |
| `game/.../SchemaModels.kt` | B1 / C1 / D1 | 多个 typed schema 同时扩展 | 每个 PR 只加自己那一组数据模型，不顺手整理 unrelated schema |
| `game/.../FoundationGameSessionTest.kt` | B1 / B2 / C1 / C2 / D1 / D2 | 大量 session 行为断言集中在这里 | 新测试尽量按主题分 test method / fixture helper，避免同一测试块多人改 |
| `game/.../en-US.json` / `zh-CN.json` | B1 / B2 / C1 / C2 / D2 | 文案 key 新增集中 | 合并前统一跑一次 key 排序/去重检查，避免纯顺序冲突 |
| `client/.../golden/GoldenScreenshotHarnessTest.kt` | B1 / D2 | 日志/结算画面 golden 都会动 | 先合入更改范围较小的一侧，再集中重录 hash |
| `tools/.../MonsterSchemaTest.kt` / coverage test | B1 / 后续 route/objective gate | 数据冻结线都集中在工具测试 | 尽量把“新冻结规则”与“行为变更”放同 PR，减少交叉重写 |

并行开发建议：

1. 同一时间最多只并行一个重 `FoundationGameSession.kt` 的 PR
2. schema 扩展 PR 可以和纯 screen / golden PR 并行，但不要同时改同一 loader
3. 如果 `PR-D1` 先启动，`PR-D2` 最好晚一拍，避免同时争抢 session summary / death path

---

## 11. 一句话结论

深度 review 中真正还值得继续吸收的，不是已经完成的 `P2-C` 收口项，而是 5 类后续优化：

1. `Stage A` 资源合同重构（清掉 stamina 遗产与副作用边界）
2. DamageType 数据激活 + 战斗反馈透明化（让已有系统真正生效）
3. signature reward 被动效果与掉落反馈的辨识度增强（记忆点）
4. 精英 / Boss 遭遇身份的 trigger 系统引入（战术差异）
5. RunSummary 增强（失败/成功可学习性）

其中 PR-B1 的优先级被上调——它不仅是反馈优化，更是 DamageType 从"管线存在"到"数据生效"的关键补全，直接影响 PR-C1 的体验基础。

按本计划推进，可以在不重开 Phase 2 主线的前提下，把 deep review 的有效建议沉淀为一组低返工、可直接执行的优化 PR。

---

## 12. 2026-03-23 Follow-up Status

后续玩家侧 polish 已按拆分计划落地到 `PR-F1 ~ PR-F5`，当前状态补充如下：

1. `Phase 2` 已解锁 talent 现在通过最小 `loadout remap` 进入正式可操作路径；正式战斗热栏仍固定为 `1-4`
2. route 主线怪物抗性覆盖已从“家族最低覆盖线”升级为“短局可感知覆盖”，并额外纳入 boss floor / encounter gating 的 runtime 校验
3. zone `descKey` 已进入 snapshot metadata 与正式入口提示，且不会污染 outcome summary
4. 非 accessory passive 装备已扩到武器 / 防具，并补固定 seed loot corpus 验证它们能在短局奖励路径里真实出现
5. 前期遭遇调优与日志显著化已接入正式 `game/client` 路径；tone 分类继续只依赖现有 `message.key` 家族
