# Phase2 优化方案完成态深度审查报告 — Part 3

> 接 Part 2。本部分识别当前 Phase2 必须解决的关键问题，并给出按优先级排序的优化建议。

---

## 5. 当前 Phase2 最需要解决的关键问题

### 问题 #1（Critical）：天赋系统缺乏分支选择，构筑空间被压扁为零

**问题描述**：每个职业固定 4 个起始天赋，天赋点只能升级已有天赋等级，不能学习新天赋。同职业的两局游玩体验完全相同，Roguelike 核心的"每局不同 build"驱动力不存在。

**证据**：
- `ProfessionSchemaV2.startingTalents` 硬编码 4 个天赋（如 vanguard: `[power_strike, shield_bash, guard_stance, war_cry]`）
- `TalentLoadout.slotToTalentId` 支持 6 个槽位但当前无机制获取新天赋
- `talentTrees` 字段在 schema 中存在但游戏内无"从树中选择天赋"的交互
- 每职业有 8-9 个天赋定义（如 vanguard 还有 `charge, sweeping_strike, sunder_armor, intimidation, unyielding`），但玩家在游戏中只能用到固定的 4 个

**为什么这是当前必须解决的问题**：
- 这不是"内容不够"的问题，是**系统设计根本缺失**：天赋选择机制不存在
- 已有 33 个天赋被实现和测试，但玩家只能接触到其中 4 个——这是巨大的资源浪费
- 构筑分化是 Roguelike 耐玩性的基石，没有它，重复游玩价值趋近于零
- 后续 Phase 的内容扩展（更多天赋/更多职业）建立在"玩家能选择天赋"这个前提之上

**不解决的后果**：
- 重复游玩价值被锁定在"4 局"（每职业 1 局），与 Roguelike 定位严重矛盾
- 后续 Phase 新增的天赋将持续无法被玩家使用
- 天赋树数据结构成为永远不会被消费的死数据

---

### 问题 #2（Critical）：80% 怪物零抗性，DamageType 六通道系统名存实亡

**问题描述**：Stage B 实现了 DamageType 可视化和抗性计算，但仅 5/24 怪物有非零抗性。对绝大多数战斗，选择火球还是冰弹、物理还是魔法，在数值结果上完全等价。

**证据**：
- `monsters/index.yaml`：仅 `undead.restless_skeleton`, `goblin.scrapper`, `bandit.wild_huntmaster`, `orc.miner`, `cultist.ember_adept` 有抗性值
- 剩余 19 个怪物的 `resistances` 为空或零
- `CombatResolver.kt` 的抗性减免逻辑已完整实现（夹 [-25, 75]），但因数据缺失而空转

**为什么这是当前必须解决的问题**：
- DamageType 是 Phase2 PR-02 的核心语义合同之一，投入了大量设计和实现成本
- Stage B 已经完成了可视化框架（图标键、渲染管线），但数据不填充意味着框架价值为零
- 抗性不仅是"数值平衡"问题，更是**战术维度**：有了抗性差异，不同天赋/不同职业对不同怪物的效果会不同，这直接增加构筑决策深度
- 填充抗性数据是纯数据工作，不需要代码修改，ROI 极高

**不解决的后果**：
- 六通道伤害系统的设计投入全部沉没
- 元素被动装备（`furnace_talisman` 的 FIRE +15%）毫无意义——因为没有怪物会抵抗火焰
- 抗性被动装备（`seal_reliquary` 的 SHADOW +10）同样毫无意义
- 职业间的"元素适性"差异不存在

---

### 问题 #3（High）：statGrowth 升级静默，成长正反馈链最关键的一环缺失

**问题描述**：升级时 `applyStatGrowth()` 静默修改属性，无日志、无 UI 提示。玩家不知道升级让自己变强了什么、变强了多少。

**证据**：
- `FoundationGameSession.kt` L2220-2233: `applyStatGrowth()` 仅修改 `Stats`，不写入 `messageLog`
- `ExperienceSystem.applyReward()` 返回 `ExperienceGainResult` 含 `levelsGained`，但 statGrowth 信息未包含在结果中
- 对比：`ExperienceGainResult` 包含 `gainedStatPoints` 和 `gainedTalentPoints`（可分配资源的信息），但**不包含** statGrowth 带来的具体属性增长

**为什么这是当前必须解决的问题**：
- "升级"是 RPG 最核心的正反馈时刻，让它静默发生等于主动浪费最高效的体验设计工具
- 修复成本极低——只需在 `applyStatGrowth()` 后添加一条日志消息，i18n 键添加一个 `log.levelup.stat_growth`
- 不修复意味着所有职业的 statGrowth 差异化设计（vanguard STR+3/CON+3 vs arcanist WIL+4）对玩家完全透明

**不解决的后果**：
- 成长正反馈链断裂：玩家只看到"Level Up!"，但不知道自己具体变强了什么
- 职业差异化的感知被削弱——STR 型战士和 WIL 型法师的成长区别无法被感知

---

### 问题 #4（High）：2/4 区域没有 Boss，区域过渡缺乏结构性锚点

**问题描述**：4 个区域中只有 `shattered_outpost`（第 1 区域）和 `grey_gate_depths`（第 4 区域）有 Boss 遭遇。中间两个区域（`greenwood_fringe`, `deep_iron_pit`）没有 Boss，导致这两个区域缺乏高潮和"通过感"。

**证据**：
- Zone 数据中 `greenwood_fringe` 和 `deep_iron_pit` 的 boss 字段为空
- `BossFactory.kt` 仅处理 2 个 Boss 遭遇模板
- 这些区域有区域特定掉落配置（`loot.greenwood_fringe.reward`, `loot.deep_iron_pit.reward`），但没有 Boss 来触发这些奖励的高光时刻

**为什么这是当前必须解决的问题**：
- Boss 战是 Roguelike 最重要的节奏锚点——它标记了"你通过了一个阶段"的成就感
- 没有 Boss 的区域在体验上退化为"走廊"：玩家穿过它只是为了到达下一个区域
- 区域特定掉落配置已存在但无法被消费——`loot.greenwood_fringe.reward` 和 `loot.deep_iron_pit.reward` 各有 4 件独特物品，但没有 Boss 或里程碑来触发它们
- Boss 数据定义成本低（复用现有怪物模板 + 添加触发器），代码框架已完备

**不解决的后果**：
- 游戏节奏变成"平坦的 8 层怪物磨砺 + 两个高点"，中间 4 层体验空洞
- 区域特定奖励无法被消费，掉落池浪费
- 后续 Phase 添加更多区域时，同样会面临"没有 Boss = 没有节奏"的问题

---

### 问题 #5（High）：普通怪战斗无决策窗口，低层战斗是纯走过场

**问题描述**：低层怪物（floor 1-3）的 HP 和攻击力远低于玩家能力，大部分战斗 1-2 回合结束，不需要任何战术思考。这使得游戏前期大量时间被"无脑击杀"填充。

**证据**：
- `beast.rat`: 基础怪物，低 HP、低攻击
- Vanguard 初始 `power_strike` 配合 STR 加成可以一击击杀大部分 floor 1-2 怪物
- AI 类型 `ai.chase.basic` 的行为仅为"追着打"，无法构成战术压力

**为什么这是当前必须解决的问题**：
- 前 2-3 层的战斗体验定义了玩家的"第一印象"——如果前 10 分钟全是无脑碾压，玩家对游戏的判断就是"没有挑战性"
- 解决方案不是把怪变强（那会破坏数值曲线），而是让低层战斗也有最小限度的决策需求——比如怪物组合出现（2-3 只同时出现需要优先级选择）、低层怪有简单的特殊行为

**不解决的后果**：
- 新玩家在形成"这个游戏值得深入"的判断前就已经觉得战斗单调
- 前期体验无法代表中后期的战斗深度，造成"虚假的简单→突然变难"的节奏断裂

---

### 问题 #6（High）：属性分配的最优解过于明确，决策沦为走过场

**问题描述**：每级 +2 属性点分配到 str/dex/con/wil，但由于公式线性且透明，每个职业的最优分配是确定的。这不是一个有意义的选择。

**证据**：
- `StatsCalculator.kt`: attack = baseAttack + str*2, maxHp = baseHp + con*8, critChance = 0.05 + dex*0.002
- STR 对 Vanguard 的 DPS 贡献（+2 attack per point）远高于 DEX（+0.002 critChance per point）
- WIL 对 Arcanist 的贡献（+0.01 talentPower + +5 maxMana per point）是唯一有效的属性
- 不存在"加到某个阈值后收益跳变"的非线性机制

**为什么这是当前必须解决的问题**：
- "伪选择"比没有选择更糟——它浪费玩家时间并产生"被忽悠"的感觉
- 属性分配是 RPG 中最经典的"让玩家感觉在塑造角色"的设计工具，但只有在选择真正影响结果时才有效
- 修复方向有两种：(a) 增加非线性收益/阈值门槛使混合分配有意义，或 (b) 移除手动分配、完全依赖 statGrowth（简化但诚实）

**不解决的后果**：
- 每次升级的属性分配变成"按照攻略点一下"的机械操作
- 后续增加更多属性或阈值系统时需要回溯修复公式，越晚改成本越高

---

### 问题 #7（Medium）：战斗反馈缺乏差异化，暴击/抗性/被动/控制效果在日志中都是同质化呈现

**问题描述**：当前战斗日志对所有事件使用统一格式，暴击、元素伤害被抗性减免、被动效果触发、控制效果命中等"高信息量事件"没有区别于普通攻击的反馈方式。

**证据**：
- `FoundationGameSession.kt` 中战斗事件写入 `messageLog`，格式统一
- `RenderSnapshot` 的 `logEvents` 为 `List<RenderTextTokenSnapshot>`，但未区分事件重要性
- Tile 渲染层的日志显示无颜色分级或动画差异

**为什么这是当前必须解决的问题**：
- 战斗反馈是"判断—执行—反馈"循环中最关键的环节——玩家通过反馈学习什么有效、什么无效
- 当暴击和普通攻击看起来一样时，暴击率 stat 的构筑价值在体验层面被清零
- 当抗性减免在日志中不显示时，DamageType 系统在玩家认知中不存在
- 修复成本低——在日志事件中增加标签字段（如 `isCritical`, `resistanceApplied`），渲染层根据标签调整颜色

**不解决的后果**：
- 玩家永远无法通过游玩学会"用火打冰系弱点怪更有效"，因为反馈不告诉他
- 暴击/控制/buff 的战术价值在感知层面被清零

---

### 问题 #8（Medium）：区域之间缺乏叙事/机制过渡，4 个区域在体验上是 "换皮走廊"

**问题描述**：从 shattered_outpost 到 greenwood_fringe 到 deep_iron_pit 到 grey_gate_depths，区域过渡只是"下楼梯 → 新 tileset + 新怪物"。没有叙事、没有过渡事件、没有解锁条件、没有区域特有机制。

**证据**：
- Zone 数据仅含 biome/floors/monsterPool/boss 等配置字段，无叙事或特殊规则字段
- `FoundationGameSession.kt` 的楼层过渡逻辑仅切换地图和怪物池
- i18n 中有区域名称键，但无区域描述或过渡文本

**为什么这是当前必须解决的问题**：
- 区域差异化是 Roguelike 世界构建的基础——如果 4 个区域只是数值梯度不同的走廊，世界观无法建立
- 当前修复成本低：在区域入口添加文本描述即可提供基本的叙事锚点
- 不修复意味着后续增加更多区域时，同样没有差异感

---

### 问题 #9（Medium）：被动装备仅 4 件且全部为固定配件，武器/防具缺乏身份感

**问题描述**：被动效果仅存在于 4 件 ACCESSORY 类固定装备上，所有武器和防具都是"数值棒"——看攻击力/防御力高低决定换不换。

**证据**：
- `items/index.yaml`: `bandit_trophy`, `emerald_charm`, `furnace_talisman`, `seal_reliquary` 均为 ACCESSORY 类型
- 武器类（`short_sword`, `long_sword`, `battle_axe` 等）仅有 baseAttack + 材料修正 + 词缀修正
- `PassiveEffectResolver` 完全可以处理武器/防具上的被动，但数据层未配置

**为什么这是当前必须解决的问题**：
- 武器是 RPG 中最有身份感的装备类型——"这把火焰战斧比普通战斧更适合打亡灵"是构筑驱动的核心
- `PassiveEffectResolver` 框架已完备，添加武器被动只需修改 `items/index.yaml`
- 纯数据改动，零代码风险

---

### 问题 #10（Low）：仅 1 个难度设置，缺乏挑战性调节

**问题描述**：仅有 `normal` 难度，所有乘数为 1.0。没有难度选择意味着挑战性完全由数值曲线固定。

**证据**：
- 难度数据仅 1 条：`normal (1.0, 1.0, 1.0, 0.0)`
- `GameModule` 支持传入 difficulty 参数，代码框架已支持多难度

**为什么这可以适度延后**：
- 多难度在内容量充足后更有意义，当前 4 区域 × 2 层的规模下一个难度足够
- 但应在 Phase2 结束前至少添加一个"Hard"模式，为核心玩家提供挑战选项

---

## 6. 优化建议（按优先级）

### P0：必须立刻处理

#### P0-1：实现天赋选择机制——从天赋树中学习新天赋

**问题本质**：构筑空间为零，重复游玩价值的系统级缺失

**影响范围**：
- `FoundationGameSession.kt`（增加天赋学习命令处理）
- `TalentLoadout`（可能需要扩展槽位管理）
- `PlayerCommand`（增加 `LearnTalent` 命令）
- `RenderSnapshot`（增加可学习天赋列表到 UI 状态）
- `InputHandler`（增加天赋学习 UI 模式）
- i18n（增加天赋学习相关文本）

**优化目标**：玩家在升级获得天赋点时，可以选择"升级已有天赋"或"学习一个新天赋"（从职业天赋树中）。

**具体改法**：
1. 在 `TalentAssign` UI 模式中增加"学习新天赋"选项
2. `ProfessionSchemaV2.talentTrees` 已有天赋树数据——从中读取当前职业可学习的天赋列表
3. 学习条件：花费 1 天赋点，天赋等级初始化为 1
4. 槽位限制：最多 6 个天赋同时装载（`slotToTalentId` 的容量）
5. 超出槽位时允许"替换"已有天赋（保留等级，只是移出快捷栏）

**改动优先级**：**P0** —— 这是"游戏能否产生重复游玩价值"的分水岭

**预期收益**：
- 同职业不同 build 成为可能（如 Vanguard 可走 `war_cry + sweeping_strike` 群体流 vs `charge + sunder_armor` 单体爆发流）
- 33 个已实现天赋全部可被玩家接触
- 天赋点从"升级数值"变为"构筑决策"
- 重复游玩价值从"4 局"提升到"4 职业 × N 种天赋组合"

**可能副作用/风险**：
- 需要平衡天赋树中的强弱差距，避免出现"必学天赋"
- UI 工作量：需要新增天赋学习界面
- 需要同步更新 SoloClearLab 测试以覆盖非默认天赋组合

**需要同步修改**：代码（`FoundationGameSession`, `InputHandler`, `RenderSnapshot`）、UI（新 UiMode）、测试、i18n

---

#### P0-2：填充全部怪物的抗性数据

**问题本质**：六通道伤害类型系统的数据层空转

**影响范围**：
- `monsters/index.yaml`（纯数据修改）
- 无代码修改

**优化目标**：所有 24 个怪物都有有意义的抗性配置，使得"对什么怪用什么伤害类型"成为可感知的战术选择。

**具体改法**：

按怪物家族设计抗性主题：

| 家族 | 设计方向 | 建议抗性 |
|------|---------|---------|
| beast.* | 自然生物，怕火耐寒 | FIRE: -15, COLD: 10 |
| undead.* | 亡灵，怕圣光抗暗影 | HOLY: -20, SHADOW: 15, FIRE: -10 |
| bandit.* | 人类，无特殊抗性但轻甲 | PHYSICAL: -5（代表轻甲弱点） |
| goblin.* | 小体型，怕火 | FIRE: -15, LIGHTNING: -10 |
| orc.* | 重装，耐物理怕魔法 | PHYSICAL: 10, FIRE: 10, COLD: -10, LIGHTNING: -15 |
| cultist.* | 暗系法师，耐暗怕圣光 | SHADOW: 15, HOLY: -20, FIRE: -10 |

**注意**：抗性值应在 [-25, 75] 区间内（与 `CombatResolver` 的夹范围一致），且弱点（负值）应比抗性（正值）更突出——这样玩家会优先寻找弱点而非规避抗性。

**改动优先级**：**P0** —— 纯数据工作、零代码风险、ROI 极高

**预期收益**：
- DamageType 系统从"装饰"变为"战术维度"
- 元素被动装备（`furnace_talisman`, `seal_reliquary`）获得实际价值
- 不同职业对不同区域的难度产生差异（Arcanist 在亡灵区域有 HOLY 弱点优势）
- 构筑决策增加一个维度：选什么伤害类型的天赋

**可能副作用/风险**：
- 如果某个职业的主伤害类型恰好对某个区域的所有怪物都有效，会出现"某个区域特别简单"的问题——需要每个区域混合多个家族
- 当前怪物池已按区域混合家族（如 shattered_outpost 有 beast + bandit + undead），所以风险可控

**需要同步修改**：`monsters/index.yaml`（数据）、可能需要更新 `SoloClearLab` 测试期望

---

#### P0-3：statGrowth 升级日志——1 小时修复，最大化成长正反馈

**问题本质**：成长正反馈链的关键环节断裂

**影响范围**：
- `FoundationGameSession.kt`（在 `applyStatGrowth()` 后添加日志）
- i18n（添加 `log.levelup.stat_growth` 键）

**优化目标**：升级时在消息日志中显示"力量 +3, 敏捷 +2, 体质 +3, 意志 +1"。

**具体改法**：
```kotlin
// FoundationGameSession.kt, applyStatGrowth() 末尾
messageLog.add(
    localizedText("log.levelup.stat_growth",
        "str" to profession.statGrowth.str,
        "dex" to profession.statGrowth.dex,
        "con" to profession.statGrowth.con,
        "wil" to profession.statGrowth.wil
    )
)
```

i18n 添加：
```json
"log.levelup.stat_growth": "Attributes grew: STR +{str}, DEX +{dex}, CON +{con}, WIL +{wil}"
```

**改动优先级**：**P0** —— 1 小时工作量，但直接影响每次升级的体验质量

**预期收益**：
- 玩家每次升级都能看到自己变强了什么
- 职业差异化成长路径变得可感知
- 成长正反馈链完整闭合

**可能副作用/风险**：无

**需要同步修改**：代码、i18n（en-US + zh-CN）

---

### P1：建议本阶段尽快处理

#### P1-1：为中间 2 个区域添加 Boss 遭遇

**问题本质**：游戏节奏的中段塌陷

**影响范围**：
- `data/bosses/index.yaml`（或对应数据文件，添加 2 个 Boss 定义）
- `data/monsters/index.yaml`（可复用现有怪物模板升级为 Boss 变种）
- `data/ai/index.yaml`（为新 Boss 添加 AI 触发器配置）
- Zone 数据（为 `greenwood_fringe` 和 `deep_iron_pit` 配置 Boss 字段）

**优化目标**：4 个区域各有 1 个 Boss，形成"探索 → 挑战 → Boss 战 → 奖励 → 下一区域"的完整节奏循环。

**具体改法**：
1. `greenwood_fringe` Boss：建议使用 `bandit.wild_huntmaster` 变种——狩猎大师在森林区域主题契合
   - 给予 `power_strike` + 额外天赋（如 `charge`）
   - 触发器：`onCombatStart` 用一个标志性开局 + `hpBelowRatio: 0.4` 狂暴化
2. `deep_iron_pit` Boss：建议使用 `orc.forge_guard` 变种——锻炉守卫在矿坑区域主题契合
   - 已有 `war_cry` + `shield_bash` 天赋
   - 触发器：已有 `elite_forge_guard_opening_shield_bash`，升级为 Boss 级触发器
3. 为新 Boss 配置区域特定奖励（`loot.greenwood_fringe.reward`, `loot.deep_iron_pit.reward` 已存在）

**改动优先级**：**P1**

**预期收益**：
- 游戏节奏从"2 个高点 + 6 个平坦层"变为"4 个高点 + 4 个上升段"
- 已存在的区域特定掉落配置得以被消费
- 每个区域都有明确的"通关标记"

**可能副作用/风险**：
- Boss 数值需要合理设计，避免中间 Boss 比最终 Boss 更难
- 需要为新 Boss 配置竞技场数据

**需要同步修改**：Boss 数据、Zone 数据、AI 数据、竞技场数据、测试

---

#### P1-2：战斗反馈差异化——暴击/抗性/被动触发的视觉区分

**问题本质**：战斗反馈的信息密度不足

**影响范围**：
- `RenderSnapshot` 中的日志事件模型（可能需要增加事件标签）
- `FoundationGameSession.kt`（为不同战斗结果生成不同事件类型）
- Tile/ASCII 渲染层的日志颜色映射

**优化目标**：暴击、元素抗性减免、被动效果触发在日志中有明显不同的呈现方式。

**具体改法**：
1. 在 `RenderTextTokenSnapshot` 中增加可选的 `tone` 字段（如 `CRITICAL`, `RESISTED`, `PASSIVE_TRIGGERED`）
2. 暴击伤害日志使用高亮色（如金色 `GOLD`）
3. 抗性减免日志显示"(resisted X%)" 后缀
4. 被动效果触发时添加独立日志行（如 "Bandit Trophy: +15% damage vs bandit"）
5. 控制效果命中（STUNNED, ARMOR_BREAK）使用特殊颜色（如青色 `CYAN`）

**改动优先级**：**P1**

**预期收益**：
- 玩家通过视觉反馈学习"什么有效"
- 暴击率 stat 的构筑价值可感知
- DamageType 系统的效果可观察
- 被动装备的价值可验证

**可能副作用/风险**：
- 日志信息量增加可能导致消息区域"太吵"——需要控制显示频率
- 需要确保 ASCII 渲染模式也能展示差异（通过 TextTone）

**需要同步修改**：`RenderSnapshot`（模型）、`FoundationGameSession`（事件生成）、渲染层、i18n

---

#### P1-3：为武器/防具添加被动效果数据

**问题本质**：装备决策空间单一

**影响范围**：
- `items/index.yaml`（纯数据修改）

**优化目标**：至少 30% 的武器和防具携带被动效果，使装备选择从"看数值"变为"看功能匹配"。

**具体改法**：

建议添加以下被动武器/防具：

| 物品 | 类型 | 被动 | 设计意图 |
|------|------|------|---------|
| `battle_axe` | WEAPON | `DamageVsTag(tag: orc, +15%)` | 反兽人专用武器 |
| `arcane_staff` | WEAPON | `DamageTypeBonus(damageType: FIRE/COLD/LIGHTNING, +10%)` | 元素法杖 |
| `hunter_bow` | WEAPON | `DamageVsTag(tag: beast, +20%)` | 猎人弓 |
| `chain_mail` | ARMOR | `ResistanceBonus(FIRE, +10)` | 抗火锁甲 |
| `shadow_cloak` | ARMOR | `ResistanceBonus(SHADOW, +15)` | 暗影斗篷 |
| `sanctified_seal` | ACCESSORY | `ResistanceBonus(HOLY, +10)` | 已有物品扩展 |

**改动优先级**：**P1** —— 纯数据改动、零代码风险

**预期收益**：
- 装备选择从"数值比较"变为"功能匹配"
- 与怪物抗性系统形成正向联动
- 不同区域偏好不同装备

**可能副作用/风险**：无——`PassiveEffectResolver` 框架已完备

---

#### P1-4：区域入口文本——最低成本的叙事锚点

**问题本质**：世界观空洞

**影响范围**：
- i18n（添加区域描述文本）
- `FoundationGameSession.kt`（区域过渡时写入消息日志）

**优化目标**：进入每个新区域时，玩家看到 1-2 句描述文本，建立最基本的世界观锚点。

**具体改法**：
- 在 `FoundationGameSession` 的区域过渡逻辑中，首次进入新区域时写入 `messageLog`
- i18n 添加 `zone.description.shattered_outpost` 等键

**改动优先级**：**P1** —— 极低成本、显著改善体验

---

### P2：可以排期但不应忽略

#### P2-1：增加"Hard"难度模式

**问题本质**：挑战性无法调节

**影响范围**：
- 难度数据文件（添加 1 条 hard 难度数据）
- `MainMenuScreen`（添加难度选择 UI）

**优化目标**：提供至少 2 个难度选择（Normal / Hard），Hard 模式怪物 HP/伤害 ×1.5。

**改动优先级**：**P2**

---

#### P2-2：属性分配增加非线性机制

**问题本质**：属性分配是伪选择

**影响范围**：
- `StatsCalculator.kt`（修改公式）

**优化目标**：引入属性阈值效果——例如 STR 达到 20 时解锁"重击概率"、DEX 达到 15 时解锁"闪避反击"。使混合分配有独特收益。

**具体改法**：在 `StatsCalculator` 中添加阈值检测，在 `DerivedStats` 中增加对应字段。

**改动优先级**：**P2** —— 需要较多设计和平衡工作

---

#### P2-3：探索增加随机事件

**问题本质**：探索缺乏独立奖励和惊喜

**影响范围**：
- 新增事件系统（代码量中等）
- 地图生成逻辑
- i18n

**优化目标**：在地下城中随机出现简单事件（宝箱、陷阱、NPC 交互等），为探索提供独立于战斗之外的奖励。

**改动优先级**：**P2** —— 需要新系统但可以从极简版本开始

---

#### P2-4：增加跨局进度系统的骨架

**问题本质**：重复游玩缺乏长期目标

**影响范围**：
- 新增元进度系统（代码量较大）
- 存档系统扩展

**优化目标**：至少实现一个跨局解锁条件——如"用 Vanguard 通关后解锁 Hard 模式"或"击杀 100 只怪物解锁新起始装备"。

**改动优先级**：**P2** —— 可以做骨架但不适合在 Phase2 做完整系统
