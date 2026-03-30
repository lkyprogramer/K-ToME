# Phase 3 深度审查报告（V3）— Part 2：玩法体验总评

> 接续 Part 1 一致性矩阵。本部分从玩家视角评估当前 Phase 3 完成态的体验质量。

---

## 4. 当前阶段玩法体验总评

### 4.1 核心循环（Core Loop）

**评分：7.0/10**（V2 审查：6.5/10，+0.5）

#### 当前循环链路

```
进入 Zone → 探索楼层 → 遭遇战斗 → 获取经验/shard/装备
    ↓                                      ↓
cadence 奖励兜底 ←── 层级结算 ←── 击杀/清层
    ↓                                      ↓
升级（天赋点+属性点+满血恢复）         掉落评估/装备替换
    ↓                                      ↓
商店消费 ←── Zone 推进 ←── Boss 挑战 ←── 构筑强化
    ↓
下一个 Zone / 支线选择
```

#### 链路各环节评估

| 环节 | 状态 | 问题 |
|------|------|------|
| 探索 | ✅ 基本成立 | BSP 地图有合理房间结构；ambush_lane 增加走廊风险感 |
| 战斗 | ✅ 显著改善 | 56/60 怪有独立 AI，战斗不再纯平；furnace_pressure 增加环境压力 |
| 奖励 | ✅ 结构性改善 | cadence 兜底消除了"旱层"；Boss/route 奖励仍是主要高光 |
| 成长 | ⚠️ 仍有不足 | 19 天赋点比 10 好很多，但基础职业 15-16 天赋 × 多 rank 仍显紧张 |
| 构筑决策 | ⚠️ 仍有不足 | 天赋选择有真实权衡，但装备 affix 对构筑的影响感知不够清晰 |
| Zone 推进 | ✅ 成立 | 世界图分支 + 可选 zone 提供了路径选择 |

#### 循环最弱环节

**构筑感知层**：天赋点经济改善后，玩家确实需要做选择了（19 点 vs 15-16 个天赋 × 多 rank）。但问题在于——**玩家能否清楚感知到"这次我选了 A build 和上次选 B build 的体验差异"？** 当前差异主要体现在数值上（伤害更高 vs 更肉），而非玩法模式上（除了少数标志性技能如 STEALTH、TAUNT、EQUILIBRIUM）。

**"前 15 分钟到 45 分钟"的体验跌落**：初始几层因为新职业新技能有新鲜感。但进入 deep_iron_pit 前后，如果玩家还没感受到 build 分化带来的"我越来越强且我的强和别人不一样"，动力会开始下降。Cadence 奖励缓解了奖励旱区，但不能替代构筑深度。

#### V2→V3 改善总结

- **战斗无决策**：从 Critical 降到 Medium。56/60 怪有独立行为，patrol_pressure 增加时间压力，ambush_lane 增加空间风险。
- **奖励旱区**：从 High 降到 Low。Cadence 兜底 + REFRESH_STOCK 基本解决。
- **天赋饥荒**：从 Critical 降到 Medium。19 点比 10 点好很多，但基础职业构筑分化仍然有限。

---

### 4.2 战斗体验

**评分：6.5/10**（V2 审查：5.5/10，+1.0）

#### 正面评价

1. **AI 行为多样性是 V3 最大进步**：
   - `ai.artillery.volley`（远程齐射）、`ai.skirmisher.flank`（侧翼机动）、`ai.controller.pressure`（控制施压）、`ai.forge.guard`（锻造守卫）、`ai.crystal.weaver`（水晶编织）、`ai.river.lurker`（河道潜伏）等——不同 archetype 的怪物有真实不同的战斗节奏。
   - 证据：`data/ai/index.yaml` 101 个独立 profile，`data/monsters/index.yaml` 60 个怪物中仅 4 个 basic-profile。

2. **Boss 战斗有真实决策层**：
   - 4 个 Boss 全有 phase 切换、telegraph 预警、独立 AI profile per phase。
   - `molten_giant` 的 stomp vs ground_slam 切换、`abyssal_guardian` 的多 phase 设计提供了需要反应和预判的战斗。
   - 证据：`data/bosses/index.yaml`（4 个 encounter 定义）。

3. **CombatFeedbackSnapshot 让战斗信息可结构化传递**：
   - DAMAGE（含 critical 标记）、HEAL、MISS、STATUS_APPLIED、STATUS_REMOVED 五种反馈类型。
   - ASCII 和 Tile 渲染均已接入。
   - 证据：`core/snapshot/RenderSnapshot.kt:151`、`client/render/AsciiRenderModel.kt:162`。

4. **Zone 机制增加了环境维度**：
   - patrol_pressure：定时巡逻刷怪，给探索增加时间压力。
   - ambush_lane：走廊埋伏，增加空间风险。
   - furnace_pressure：周期性地面危害，增加走位需求。
   - 证据：`ZoneMechanicRuntime.kt`（564 行完整实现）。

#### 仍存在的问题

1. **`ai.guard.basic` 覆盖 7 个怪物，行为是否足够丰富？**
   - vault_watcher、ward_lancer、orc.miner、undead.chain_thrall、forge.anvil_guard、forge.chain_overseer、river.undertow_brute——这些中后期怪物都使用 `ai.guard.basic`。
   - 如果 guard.basic 的行为模式只是"站桩+靠近攻击"，则这 7 个中后期怪的战斗体验可能仍然偏平。
   - **严重级别：Medium**——不如之前 14 个 basic-profile 严重，但 7 个中后期怪共享同一行为模式仍值得关注。

2. **战斗节奏在普通战斗中可能偏长**
   - 有独立 AI 不等于战斗快速有趣。如果 guard/forge 类怪物的 HP 偏高但威胁不大，战斗可能变成"安全但无聊的磨血"。
   - 这需要数值调校而非系统改动，但值得在 Phase 3 尾声关注。

3. **战斗反馈的"感知力"取决于渲染层质量**
   - CombatFeedbackSnapshot 数据层完备，但 ASCII 渲染中的 `takeLast(5)` 文本列表是否足以产生"爽感"？
   - Tile 渲染层有 damage number overlay，但缺少动画（shake/flash/particle）——这是 Phase 3 的合理边界，但需要在 Phase 4 优先补齐。

4. **战斗中缺少"反击窗口"和"组合触发"的正反馈设计**
   - 当前战斗是"我打你/你打我"的轮替，缺少"利用敌人的 telegraph 窗口反击"或"A 状态 + B 技能 = 触发额外效果"的组合奖励。
   - 状态交互规则（FREEZE/BURN 互斥、OVERCHARGE+LIGHTNING）存在但数量少，且触发条件对玩家不够透明。

---

### 4.3 角色成长与构筑驱动

**评分：6.0/10**（V2 审查：5.0/10，+1.0）

#### 正面评价

1. **天赋点经济显著改善**：19 点 vs 之前 10 点，玩家有了真实选择。尤其是进阶职业 12 天赋 × 多 rank，19 点不可能全点满，必须有所取舍。

2. **8 职业 × 5 种族 × 3 棵天赋树**的组合理论上提供了大量 build 空间。

3. **铭文系统（8 个铭文、4 槽位）**作为 build 的第三轴（天赋+装备+铭文）增加了构筑维度。

4. **Respec 机制**允许玩家在 run 内调整方向——降低了"点错了就废了"的焦虑。

#### 仍存在的问题

1. **基础职业构筑分化不如进阶职业**（反直觉问题）
   - 基础职业 15-16 天赋，19 点可以覆盖大部分（假设每天赋 1-2 rank）。
   - 进阶职业 12 天赋但天赋内部 rank 更多，19 点需要精细取舍。
   - 结果：Berserker/Spellblade 的 build 选择反而比 Vanguard/Arcanist 更有意义——这对新玩家体验不友好，因为他们先接触基础职业。
   - **严重级别：Medium**

2. **属性点系统（38 点）缺乏断点驱动**
   - 每级 +2 属性点，总计 38 点。如果没有"STR 达到 30 时解锁特殊效果"这样的断点，属性分配就是纯线性数值堆叠，没有决策乐趣。
   - 收益递减只作用于二级属性（evasion C=150、critRating C=200 等），但这个信息对玩家可能不够透明。
   - **严重级别：Low**（属性断点是 Phase 4 明确的扩展方向）

3. **装备 affix 对构筑的影响不够感知化**
   - 209 个 item 定义和 affix 系统存在，但玩家是否能感知到"这把剑的 affix 改变了我的玩法"？
   - 如果 affix 只是 "+5 STR, +3 DEX" 这样的纯数值加成，则 affix 不构成构筑选择——它只是"数字更大的装备"。
   - **严重级别：Medium**——Phase 3 文档明确说"affix v1 目的仅是验证掉落驱动构筑差异"，但如果差异只体现在数值上，则验证目标未达成。

4. **成长的"变质点"不清晰**
   - 升级提供天赋点 + 属性点 + 满血恢复，但缺少"Lv5 解锁第二棵树"、"Lv10 获得终极技能"这样的成长里程碑。
   - 天赋树的 prerequisite 系统存在，但 breakpoint 机制是否被有效利用来创造"每 N 级有一个兴奋点"的节奏？

---

### 4.4 奖励驱动与掉落体验

**评分：6.0/10**（V2 审查：4.5/10，+1.5）

#### 正面评价

1. **Cadence 奖励兜底是 V3 最重要的奖励系统改善**：
   - 每个 zone 都有独立的 cadence loot profile（13 个 zone-specific profile）。
   - `FloorRewardStateSnapshot` 追踪每层奖励状态，确保不会出现"空层"。
   - 证据：`data/loot/index.yaml`（`loot.*.cadence` 13 个 profile）。

2. **REFRESH_STOCK 服务为 shard 提供了新消费出口**：
   - greenwood 35 shard / deep_iron 60 shard——定价合理，不会让玩家在第一个商店就耗尽资金。
   - 每店每 run 限购 1 次——防止无限刷。

3. **Boss 掉落使用独立 loot profile**（`loot.foundation.boss`），包含高价值独特物品（battle_axe, plate_armor, arcane_staff 等），Boss 击杀有明确的奖励期待。

#### 仍存在的问题

1. **掉落缺少"惊喜层"——没有稀有度引发的兴奋**
   - 当前掉落系统有 ItemQuality（COMMON/MAGIC/RARE 等），但玩家在战斗中拾取物品时，是否有"哇这是一个 RARE 掉落"的视觉/音效/文本突出反馈？
   - CombatFeedbackSnapshot 处理了战斗反馈，但 **掉落反馈** 似乎没有对应的"LootFeedbackSnapshot"机制。
   - **严重级别：Medium**

2. **Cadence 奖励虽然消除了旱区，但可能产生"义务感"而非"惊喜感"**
   - 设计上 cadence reward 是"兜底"——如果这层没掉 meaningful reward 就给一个。
   - 但从玩家体验看，这是"系统补偿"而非"探索发现"。如果 cadence reward 的掉落方式和普通掉落一样，玩家可能不会注意到这是一个特殊机制——它默默工作，减少了痛苦但没增加快乐。
   - **建议**：cadence reward 应该有区别于普通掉落的呈现方式（例如专门的日志消息、独特物品名称、或出现在特定位置而非怪物身上）。

3. **Shard 经济在后半段可能溢出**
   - 两个商店分别在 greenwood 和 deep_iron，之后的 6 个 zone（grey_gate_depths → abyssal_heart）没有商店。
   - 如果后半段怪物仍然掉 shard 但没有消费出口，shard 会变成无意义的数字累积。
   - **严重级别：Medium**——Phase 3 文档明确只做 2 个固定商店，但如果 shard 在后半段失去价值，会削弱前半段积攒 shard 的动力。

---

### 4.5 探索与重复游玩价值

**评分：6.0/10**（V2 审查：5.5/10，+0.5）

#### 正面评价

1. **世界图分支是 Phase 3 最大的探索资产**：
   - 4 个可选 zone（bandit_camp/elven_ruins/molten_core/crystal_cavern）提供了真实的路径选择。
   - 每个可选 zone 有独有奖励节点（uniqueContentRewardProfiles 映射）。
   - 证据：`ZoneMechanicRuntime.uniqueContentRewardProfiles()`。

2. **Zone 机制增加了空间层探索动力**：
   - patrol_pressure → 不能磨磨蹭蹭，有时间压力。
   - ambush_lane → 走廊有风险，需要警觉。
   - furnace_pressure → 地面危害需要走位。

3. **8 职业 × 5 种族理论上支持多次 run**。

#### 仍存在的问题

1. **11 个 zone 中仅 3 种机制有运行时实现**
   - `patrol_pressure`：shattered_outpost, deep_iron_pit（2 个 zone 使用）
   - `ambush_lane`：bandit_camp（1 个 zone 使用）
   - `furnace_pressure`：deep_iron_pit（1 个 zone 使用）
   - 其他 zone 的 specialMechanics（如 `lore_cache`、`ore_cart`、`currents`、`crystal_shards`、`abyssal_ward`、`void_pressure` 等）仅有 introHint 映射，无运行时行为。
   - **结果**：underground_river（currents）、crystal_cavern（crystal_shards）、abyssal_temple（abyssal_ward）、grey_gate_depths（sealed_gate）的探索体验与主线无实质差异——zone 名字和怪物不同，但玩法相同。
   - **严重级别：High**——这直接影响后半段 run 的新鲜感。前半段有 patrol/ambush/furnace 加持，后半段反而回到纯战斗推图。

2. **BSP 地图缺少手工特色元素**
   - 所有 zone 使用相同的 BSP 生成器，地图结构（房间+走廊）在不同 zone 间没有视觉或拓扑差异。
   - 没有隐藏房间、宝箱、随机事件——探索动力来自"清图推进"而非"发现未知"。
   - **严重级别：Medium**——Phase 3 明确不做深度 ProcGen，但完全没有手工点缀的地图缺少探索惊喜。

3. **"再来一局"的动力主要来自职业差异，而非 run 差异**
   - 同一职业的不同 run，如果路径选择相同，体验高度相似。
   - 缺少 run-to-run 的随机变化源（随机事件、随机 NPC、稀有怪物变体、变异器等）。
   - **严重级别：Low**——这更多是 Phase 4 的扩展方向。

---

### 4.6 UI / 反馈 / 新手体验

**评分：5.5/10**（V2 审查：5.0/10，+0.5）

#### 正面评价

1. **PR-10（player-facing information cleanup）清理了内部 token 泄露**——levelBandRef、rescueTags 等不再暴露给玩家。

2. **CombatFeedbackSnapshot 提供了结构化战斗信息**——至少 ASCII 和 Tile 模式都能显示 damage/heal/miss/status。

3. **双语支持（en-US / zh-CN）**——包括天赋、状态、Boss 命名在内的全量本地化。

#### 仍存在的问题

1. **死亡原因分析缺失**
   - 玩家死亡时，是否能看到"你死于 molten_giant 的 ground_slam，造成了 45 点 FIRE 伤害"？
   - 如果死亡屏幕只是"Game Over"而没有死因回顾，玩家无法从失败中学习。
   - **严重级别：Medium**

2. **新手引导缺失**
   - 没有教程关卡或引导提示告诉玩家"怎么使用天赋"、"怎么使用铭文"、"商店怎么买东西"。
   - shattered_outpost 有 `tutorial_lane` 和 `simple_traps` 标签，但这些只有 introHint，没有运行时行为。
   - **严重级别：Medium**——Phase 3 面向的是测试/开发环境，但如果 Phase 4 要开始面向外部测试者，新手引导不能继续缺失。

3. **天赋说明/断点预览的可读性未验证**
   - DescriptionModel 机制完整，但在实际 UI 中，玩家是否能快速理解"这个天赋第 2 rank 会增加什么"？
   - breakpoint 预览是否清晰区分了"已激活"和"下一断点"？
   - **严重级别：Low**——需要人工白盒验证。

4. **zone 机制的提示可能不够直观**
   - patrol_pressure 有 floorHintShown 标记，说明会显示提示。但 "每 20 回合可能刷怪" 这个信息以什么形式传达给玩家？
   - 如果只是一行日志消息，玩家可能在忙于战斗时错过。
   - **严重级别：Low**

---

### 4.7 系统联动性

**评分：6.5/10**（V2 审查：6.0/10，+0.5）

#### 正面联动

1. **战斗↔状态↔天赋三角形依然是最强联动**：
   - 天赋施加状态 → 状态影响战斗结算 → 战斗结果触发事件 → 事件驱动反馈。
   - 例：Rogue 的 STEALTH → AI 失效 → backstab 高爆发 → 战斗结束经验奖励。

2. **Zone 机制↔战斗新增联动**：
   - furnace_pressure 在战斗中增加地面危害 → 影响走位决策。
   - patrol_pressure 定时增加怪物 → 影响探索节奏。
   - ambush_lane 走廊伏击 → 影响路线选择。

3. **经济↔装备新增联动**：
   - REFRESH_STOCK → shard 消费 → 新装备选项 → 构筑调整。

#### 仍然断裂的联动

1. **装备/affix ↔ 构筑**：affix 如果只是数值加成，则装备系统和天赋系统是平行的两条线，没有交叉点。理想状态应该是"某些 affix 配合某些天赋有额外效果"或"某些装备开启新的 build 路线"。

2. **探索 ↔ 奖励**：可选 zone 有独特奖励节点（uniqueContentRewardProfiles），但除了 4 个可选 zone 的独有物品外，主线 zone 的探索奖励和战斗奖励没有明确区分——探索本身不独立产出奖励。

3. **Zone 机制 ↔ 后半段体验**：前半段（shattered_outpost/bandit_camp/deep_iron_pit）有运行时 zone 机制，后半段（grey_gate_depths → abyssal_heart）的 zone 机制只有 introHint——系统联动在后半段 run 中断裂。

---

### 4.8 体验综合评分

| 维度 | V2 评分 | V3 评分 | 变化 | 关键改善点 |
|------|---------|---------|------|-----------|
| 核心循环 | 6.5 | 7.0 | +0.5 | cadence 奖励兜底消除旱区 |
| 战斗体验 | 5.5 | 6.5 | +1.0 | 56/60 怪有独立 AI，zone 机制增加环境维度 |
| 成长与构筑 | 5.0 | 6.0 | +1.0 | 天赋点 10→19，进阶职业 12 天赋 |
| 奖励驱动 | 4.5 | 6.0 | +1.5 | cadence 兜底 + REFRESH_STOCK + 13 个 zone 专属 loot profile |
| 探索与重玩 | 5.5 | 6.0 | +0.5 | zone 机制运行时实现 + 可选 zone 独有奖励 |
| UI/反馈 | 5.0 | 5.5 | +0.5 | CombatFeedbackSnapshot + PR-10 信息清理 |
| 系统联动 | 6.0 | 6.5 | +0.5 | zone 机制↔战斗、经济↔装备新联动 |
| **综合** | **5.4** | **6.2** | **+0.8** | |

> **V3 综合评价：6.2/10——从"能玩但不好玩"升级到"基本可玩且核心循环初步成立"。V2 改进（PR-11~14）成功地把最低分维度（奖励驱动 4.5→6.0）拉平，消除了明显的体验洼地。但距离"耐玩雏形"（7.5+）仍有差距，主要瓶颈在构筑深度感知、后半段 zone 机制缺失、掉落惊喜感不足。**
