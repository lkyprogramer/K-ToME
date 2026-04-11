# Phase 4 深度审查报告 v4 — Part 2：玩法体验总评

- **审查日期**：2026-04-11
- **审查分支**：`codex/phase4-opt-pr-06-terrain-uptake-tuning`
- **关联文档**：Part 1（一致性矩阵）/ Part 3（P0 清单与优化建议）/ Part 4（延后问题与最终结论）

本部分以 **游戏总监 + 玩法体验审查** 视角，不罗列代码细节，而是回答 **"玩家开一局会遭遇什么"**。所有结论配合 Part 1 一致性矩阵的 CI-XX 编号做交叉引用。

---

## 1. 核心循环体感审查

### 1.1 一局游戏的"感知路径"

按 `zones/index.yaml` 的 `floorRange`，一局完整主线大约经过 11 个 zone × 15~18 层。以下是玩家实际感知到的"Phase 4 含量"：

| 区 | 层 | mapgenProfileId | 玩家感知的"新东西" | 体验等级 |
|---|---|---|---|---|
| shattered_outpost | 1–3 | ❌ 无 | Phase 3 水准：BSP 房间 + 固定 elite pool | **冷启动期毫无 Phase 4 含量** |
| greenwood_fringe | 4–6 | ✅ phase4 | WATER tile + bandit elite + hidden cache（1 步）| 首次看到地形，但 encounter rate 11.4% |
| bandit_camp | 可选 | ❌ 无 | Phase 3 水准 | — |
| elven_ruins | 可选 | ❌ 无 | Phase 3 水准 | — |
| deep_iron_pit | 4–7 | ✅ phase4 | OIL tile + orc forge elite + hidden slag/smuggler | **`preferredTerrainCombatCount=0`**（关键 flavor 未触发）|
| molten_core | 可选 | ❌ 无 | Phase 3 水准 | — |
| grey_gate_depths | 7–10 | ❌ 无 | **强制主线却无 Phase 4 特性** | 主线腰斩 |
| underground_river | 7–12 | ✅ phase4 | WATER + ICE + 3 步 hidden | **当前唯一"名副其实"的 Phase 4 zone** |
| crystal_cavern | 可选 | ❌ 无（但被采样） | BSP 却被 terrain 指标纳入 | 度量漂移来源 |
| abyssal_temple | 11–14 | ✅ phase4 | OIL + boss variant + hidden warded_archive | 完整但 terrain 指标零覆盖 |
| abyssal_heart | 15 | ❌ 无 | **终局强制区无 Phase 4 特性** | 终局腰斩 |

**核心问题**：
- 玩家从 floor 1 到 floor 3 完全碰不到 Phase 4 特性（**首 20 分钟无新东西**）
- 玩家在 floor 7–10 的主线再次碰不到 Phase 4 特性（**中盘再次回到 Phase 3 观感**）
- 玩家到 floor 15 终局时还是碰不到（**终局 boss 战失去 Phase 4 flavor**）

**可持续感知的 Phase 4 内容 = 3 个区 × 3~4 层 = ~12 层** = 整局体感占比 **约 67%**（去掉强制 BSP 的 3 个强制区）。而其中 greenwood 首印象已经有"编码上进入 phase4 但 terrain 11.4%"的尴尬。

### 1.2 "好玩"的临界值推理

从玩法设计学角度，**玩家形成"这游戏有 Phase 4 特性"印象的临界样本量**是：

- **前 5 层内至少 2 次 terrain 交互反馈**（烧油、冻水、滑冰任一）
- **前 3 个 elite 中至少 1 个有明显"偏好地形"行为差异**
- **前 2 个 secret zone 中至少 1 个有"和外面明显不同的奖励"**

当前实况：
- 前 5 层全在 shattered_outpost，**terrain 交互机率 0**
- 第一个 elite 在 greenwood floor 4，applyToTags `[elite]` 过滤后 mutation 池**对 bandit 无针对性**（CI-06），随机挂的 mutation 50% 概率是 stonehide/ironhide（纯数值），玩家**看不出任何行为差异**
- 第一个 secret zone 是 greenwood_hidden_cache，loot profile 与 `loot.greenwood_fringe.*` 之间的 overlap 虽然 ≤0.3，但**奖励物品都是 phase 3 就已存在的基础 affix 装**

**结论**：**前 6 层玩家没有机会感知到 Phase 4 的任何"新东西"**。这是体验层面的 P0。

---

## 2. 战斗体验审查

### 2.1 Combat Pipeline Step 9（terrain interaction）

**技术完整度**：优。`core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt` 中 step 9 的 terrain rule lookup 逻辑完整，`terrain_cold_water_freeze / terrain_fire_oil_ignite / terrain_fire_ice_melt / terrain_physical_ice_slip` 四条规则都已经在 `triggeredRuleIds` 数据里出现。

**玩家感知度**：**差**。以下细节决定了"有但感知不到"：

1. **触发场景单一**：greenwood 只观察到 `terrain_cold_water_freeze` 一条规则（`phase4-summary.md:301`），但 greenwood 的 bandit elite 没有"能释放 fire/cold"的 talent，所以这条规则触发的主体**只能是玩家**。玩家 cast ice → 敌人踩水 → freeze，这种主动 setup 要求玩家已经掌握系统，而新手在 floor 4 几乎不会有这种意识。
2. **被动 setup 缺席**：没有"怪物主动把地形点燃"的 npc talent；terrain 交互缺少"被动演示"这一关键教学。
3. **11.4% 触发率**：按 193 个 tagged combat / 22 个 triggered 推算，玩家在 greenwood 平均每 **9 场 tagged 战斗才触发 1 次 terrain 规则**，而 greenwood 整个区通常只有 30~40 场 tagged combat，**玩家整个 greenwood 生涯只会看到 3~4 次 terrain 反应**。
4. **UI 反馈未覆盖**：client 侧是否有 terrain reaction 的 telegraph / popup 没有在本次审查范围内验证，但从 `visualKey`、`audioProfile` 的引用看，当前 reaction 的反馈完全依赖 status effect 的通用 pipeline——**没有"爆炸 / 蒸汽 / 冻结全图"这类区分度的反馈设计**。

**改进方向（P1）**：见 Part 3 §4.1。

### 2.2 Elite 战斗

**valid pair = 51** 的数据乍看鲜亮，但实际玩家感知：

- 每场 elite 战平均挂 1~2 个 mutation（按 tierWeight 分布估算）
- MINOR (stonehide/ironhide/phase_runner) 都是纯数值或小 AI 漂移，**玩家难以从战斗行为上分辨**
- MAJOR (battle_drill/dread_aura/hunt_protocol/war_caller/corrosion_cloud/frostbound/tidebound) 中真正**可感知的**只有：
  - dread_aura（WEAKEN 光环，有 debuff icon）
  - war_caller（召唤/增益 npc，有 cast 动画）
  - corrosion_cloud（ARMOR_BREAK AURA，依赖 Oil 触发）
  - frostbound/tidebound（element package，有冻结 / glacial_seal）
- SIGNATURE (emberblood/void_mirror)：只有 floor 4+ 出现，且 SIGNATURE 在同 zone 多次重复的概率不低，**"第一次见"的惊喜只有 1 次**

**可感知 mutation 数量**：12 中大约 **4~5 条**能让普通玩家"说得出感受"。这对 Roguelike 的"精英遭遇叙事"来说偏薄。

**核心建议**：
- 给 stonehide/ironhide/phase_runner 加可视化 hook（冲击波、闪现残影），让数值型 MINOR 也有表演
- 将 battle_drill 的 `linebreaker` talent 与 terrain（如踢烂 water tile）挂钩
- war_caller 的"增援"npc 差异化：按 zone 召唤不同援军（greenwood 招 bandit 群、deep_iron 招 orc raider）

### 2.3 Boss 战

**3 个 boss variant**：
- `molten_glass = [ironhide, emberblood]` → OIL 偏好 → deep_iron 区 ✅
- `grey_crown = [dread_aura, war_caller]` → **preferredTerrain=[]** → 无地形 flavor ❌（CI-07）
- `abyssal_eclipse = [void_mirror, corrosion_cloud]` → 混合（void_mirror 空 + corrosion_cloud OIL）→ 半 flavor

**问题**：grey_crown 是 grey_gate_depths 的 boss，而 grey_gate_depths 本身**没有 mapgenProfileId**（CI-01），双重缺失叠加 —— **Phase 4 中盘 boss 在地图、terrain、mutation 三个维度全部失去 Phase 4 特征**。

---

## 3. 奖励与构筑体验审查

### 3.1 Loot 深度

**量化面**：106 = 78 affix + 20 unique + 8 artifact，`lootProfileAverageBaseItemOverlap = 0.2046`（低耦合好），`uniqueArtifactMeaningfulSwapRate = 100%`。

**质化面问题**：

1. **Secret loot = Normal loot（+同 zone）**（CI-05）：`loot.abyssal_temple_warded_archive.secret ∩ loot.abyssal_temple.cadence = 0.818`。这个 profile 的 `specialTemplateTagPreference` 和 `affixTagPreference` 并没有把同 zone 物品过滤掉。玩家去秘境"冒着 DC15 感知检定"拿到的东西有 82% 的概率是走主路也能拿到的。

2. **DamageVsTag 阵营克制空白**（CI-08）：仅 bandit（4 affix）+ undead（2 affix）= 6 条 affix。  
   - `orc` 无 anti-tag（deep_iron_pit、molten_core 受影响）
   - `cultist` 无 anti-tag（grey_gate_depths、abyssal_temple、abyssal_heart 受影响）
   - `forge` / `abyssal` / `river` / `crystal` 四个 biome 阵营都无 anti-tag
   - **玩家 build 中"anti-faction gear"这一维度只对 greenwood（bandit）和遗忘的 undead 生效**

3. **Artifact 的 allowedZones 过严**：每个 artifact 只允许 1 个 zone（`artifact_briar_heart` 只在 greenwood_fringe，`artifact_forge_oath` 只在 deep_iron_pit ...）。玩家选择某个"偏科" build 时，只能在一个 zone 拿到对应 artifact，没法跨 zone 重复加强。Roguelike 常用做法是 artifact 可以在跨 zone 的 Boss 宝池出现（即使是低概率），这能让"偏科构筑"获得**堆叠快感**。

4. **Material tier (IRON/STEEL/MITHRIL/ADAMANTITE) 的进阶曲线平坦**：4 个材料等级但 artifact 绝大多数被硬编码为 STEEL，`material:IRON` 在 Phase 4 后半段几乎没有使用场景。

### 3.2 构筑体验

目前的 build 轴线清单（来自 affix/unique 标签）：

- **offense / defense**（贯穿所有 affix）
- **fire / cold / lightning / holy / shadow**（element package）
- **bandit / undead**（anti-tag，但只对 2 个阵营）
- **TerrainAffinityBonus**（9 条 affix 覆盖 WATER/OIL，没有 ICE 的 affix）
- **ConditionalStatBonus**（8 条 affix 覆盖 HP_ABOVE_80 / HP_BELOW_50 / HP_BELOW_30）

**构筑空间评估**：
- **element axis**：完善，5 种元素都有 affix
- **terrain axis**：**不对称**——TerrainAffinityBonus 只对 WATER/OIL 生效，没有 ICE affix，玩家在 underground_river（ICE 0.20）拿不到地形加成
- **tag axis**：**残缺**——6 条反阵营 affix 相对 16+ 阵营需求严重不足
- **conditional axis**：HP_ABOVE_80 / HP_BELOW_50 / HP_BELOW_30 三档分布不均，HP_BELOW_30 的风险收益 build 只有 1~2 条

### 3.3 拾装决策张力

`uniqueArtifactMeaningfulSwapRate = 100%`（3270/3270）说明**每一次装备比较的被动特征都有差异**。这是一个非常健康的信号——玩家换装时总会做一个非平凡决策。

但这个决策的**信息量不高**：78 个 affix 中，76 个只改动 StatModifier；真正有 passive 行为的 affix 占比 `68/106 = 64%`（affix 中 68 条有 passive）。passive 机制的"可感知差异"是构筑体验的核心，这个比例尚可，但下探到 **68 条 passive 覆盖 9 种 kind** 时，平均每 kind 只有 7.5 条，**DamageVsTag 只有 6 条、HpRegenPerTurn 只有 3 条**。构筑多样性的数据密度在尾部明显稀薄。

---

## 4. 探索体验审查

### 4.1 隐藏内容的"惊喜曲线"

Phase 4 opt PR-05 承诺的 **hidden path 多样性** 按数据（hidden event 12、secret zone 5、trigger type 6/6、entranceBindingId 3/3）**已经达标**。但玩家感知维度的一些细节：

**路径结构分布**：
| Zone | 路径结构 | 步数 | 玩家感知 |
|---|---|---|---|
| greenwood_hidden_cache | PERCEPTION_REVEAL | 1 | 一次感知检定就出现 |
| deep_iron_slag_cache | KILL_ELITE → INTERACT_TILE | 2 | 打完 primer elite 然后互动 |
| deep_iron_smuggler_stash | ENTER_ROOM → INTERACT_TILE | 2 | 进入特定房间后互动 |
| underground_river_crystal_rift | OPEN_CHEST → PERCEPTION_REVEAL → INTERACT_TILE | 3 | 最长链条 |
| abyssal_temple_warded_archive | QUEST_STEP → PERCEPTION_REVEAL → INTERACT_TILE | 3 | 任务触发后感知 |

这是好结构。**问题不在结构，在奖励**：

1. **Secret reward 和 normal reward 身份辨识度低**（见 §3.1 CI-05）
2. **路径 primer 的提示缺失**：比如 `hidden.primer.deep_iron.slag_cache` 这种 required tag 是怎么获得的？是先杀死一个特定 primer elite。但玩家**怎么知道这个 primer elite 就是这个 secret 的钥匙**？如果 hint 系统缺失，玩家"发现"秘境会变成偶然，而不是"顿悟"。
3. **secret zone pathClass = SECRET 的返回机制**：`HiddenContentMapgenPipeline.resolveReturnBridgeNodeId` 有 3 种 return policy，但玩家是否能感知到"我是从副线分支进入的，出来后回到主线另一个点"这一空间叙事，取决于客户端 UI 是否做了 minimap 对比。**这个反馈链条没有在本审查范围内看到代码支持**。

### 4.2 探索奖赏的结构性平衡

| 奖赏类型 | 分布 | 玩家拿到的次数（一局）| 体感 |
|---|---|---|---|
| 正常战斗 loot | 21 个 profile | ~80+ 次（每场战斗都可能触发） | 常态背景 |
| Boss loot | 4 个 boss | 4 次 | 关键节点 |
| Hidden event reward | 12 条 event | 0~5 次（取决于玩家探索深度） | 奖赏峰值 |
| Secret zone loot | 5 个 profile | 0~3 次 | 玩法高潮 |

**Secret zone 应该是整局奖赏曲线的峰值**，但当前 secret profile 的 rarityBonus/qualityBonus 并没有比同 zone 的 reward profile 高出显著量级，`zone_reward.*.phase4` 中：

```
greenwood_fringe   rarityBonus 0.00, qualityBonus 0, budget 6
deep_iron_pit      rarityBonus 0.05, qualityBonus 1, budget 8
underground_river  rarityBonus 0.08, qualityBonus 1, budget 10
abyssal_temple     rarityBonus 0.12, qualityBonus 2, budget 12
```

Secret profile 的 rewardBudget 在 `loot/index.yaml` 里需要独立核对，但 CI-05 的 0.818 overlap 已经暗示 "secret ≈ normal + 少量 preference 偏移"。玩家的"冒险去开 secret"的 motivation 是 **稀缺性 + 独特性**，而当前的 secret profile 在这两个维度都不够突出。

---

## 5. 系统耦合审查

### 5.1 PreferredTerrainAffinity 的"断链"

追溯这个组件的数据流：

1. **生成时**：EncounterDecorationService.selectDecoration → EncounterDecoration.preferredTerrainTags = mutations.flatMap(preferredTerrainTags)
2. **应用时**：EncounterDecorationService.applyDecoration 挂 `PreferredTerrainAffinity` 组件
3. **放置时**：TerrainAwarePlacement（在 HybridTopologyPipeline 内）按组件的 preferredTerrainTags 对 candidate tile 加权选择

**断链点**：
- **点 A**：mutation 必须有 preferredTerrainTags，但 12 个 mutation 中只有 **4 个非空**（frostbound/tidebound/emberblood/corrosion_cloud）
- **点 B**：mutation 必须被 select() 选中，而 select() 的权重包含 `terrainAffinityWeightBonus()=4` 加成，这个加成只在 **zoneId ∈ allowedZones 且 preferredTerrainTags ≠ ∅** 时生效
- **点 C**：放置必须在一个**真的画了 WATER/OIL/ICE 的 zone**——要求 mapgenProfileId 存在
- **点 D**：战斗必须发生在这个 tile 附近

当任意一点 miss，`preferredTerrainCombatCount` 就会 = 0。**deep_iron_pit 的 0 是点 D 的问题（combat 未发生在 OIL 附近）或点 A/B 的统计波动**；**crystal_cavern 的 0 是点 C 的问题（无 mapgenProfileId → 没有 OIL/WATER 画图，观察到的 tag 来自 BSP 退路）**。

**这个系统依赖链过长**，任何一个点松动都会让整条数据流无效。**opt PR-06 修的是点 A 的注入（`elite` tag），但点 C 没修（mapgenProfileId 覆盖），点 B 的 weight bonus 偏弱（+4 相对 MAJOR base 7 只是 +57%，一旦基础池大容易被稀释）**。

### 5.2 failedExperienceMetricCount 与 taskFailed 的"弱耦合"

opt PR-06 post-dev review 的 P0-3 修复加了 `failedExperienceMetricCount` 字段。但审查 `Phase4ReportRunner.kt:368` 附近的逻辑时注意到：

- 原来 `taskCount` / `passedTaskCount` / `failedTaskCount` 只统计 harness task 层级
- 新增的 `failedExperienceMetricCount` 独立统计指标 gate fail
- **两者是"并列"关系，不是"蕴含"关系**

这意味着 future failure mode：如果所有 harness task 内部都正常跑完，但有一条 experience metric fail（比如 `terrainInteractionEncounterRate` 跌破阈值），`failedTaskCount=0` 但 `failedExperienceMetricCount=1`。当前 `failedGateCount` 字段应当把两者聚合，验证命名和数据流是否一致需要进一步代码 audit（见 Part 3 §2.3）。

### 5.3 Zone-level vs Aggregate 的 "聚合掩盖"

更广义的一个系统耦合问题：**所有带"zone 维度"的度量指标都没有 "per-zone gate"**。当前所有 gate 都是聚合值（aggregate exposure rate ≥ 33.89%, aggregate encounter rate ≥ 16.36%）。这种 gate 设计导致：

- 某 zone 表现特别差，只要其他 zone 拉起来，gate 依然 pass
- 玩家体验是 per-zone 的，但 gate 是 aggregate 的——**gate 和玩家体感脱钩**

这是一种典型的"统计学 gate"和"体验学 gate"的错配。Phase 4 opt PR-06 的 exit gate 定义本身就存在这个缺陷。**修复路径**：引入 per-zone gate，例如 "每个 Phase 4 zone 的 encounter rate 必须 ≥ 12%（目标的 70%）"（保留 aggregate + 补 per-zone lower bound）。

---

## 6. 可感知性总评分（1-10 分）

| 维度 | 分数 | 评语 |
|---|---|---|
| 地图生成的"看起来不一样" | 7 | Hybrid 管道生效的 4 zone 房间布局、vault、keyGate 明显有差异 |
| 地图生成的"玩起来不一样" | **4** | 7 zone 退化到 Phase 3；terrain 交互在玩家实际视线内平均 3-4 次每区 |
| Elite 遭遇的"叙事感" | **5** | 12 mutation 但可感知分化只有 4-5 条；applyToTags 针对性在 opt PR-06 后被稀释 |
| Boss 战的"仪式感" | **4** | 3 个 boss variant 且 1/3 无 terrain flavor；grey_crown 与所在 zone 双重掉链 |
| Loot 的"拾得惊喜" | **5** | 数量达标、swap rate 100%，但 secret/normal 身份混淆 + DamageVsTag 阵营克制空白 |
| Hidden 探索的"发现快感" | 6 | 路径结构好、trigger 覆盖全，但 reward 辨识度低 + primer 提示不足 |
| 构筑决策的"有得选" | 6 | element / terrain / conditional 三轴较完整，tag 轴残缺 |
| Phase 4 整体"新鲜感" | **5** | 系统名头够大，但 3 个强制区 Phase 3 水准稀释了整体新鲜度 |

**总评**：**5.3 / 10**。**尚未跨过 "好玩" 门槛**。需要 opt PR-07 收尾前加一轮体验驱动的内容补强（不是度量驱动）。

---

## 7. 快速对比：Phase 4 目标 vs 交付

| 原目标（2026-04-08 opt plan） | 交付状态 | 体感反差 |
|---|---|---|
| Phase 4 = 让整张地图"像一次真正的探险" | ⚠️ 67% 的层是 Phase 4 内容 | 主线 3 个强制区退化拖低印象 |
| Elite = 12 变体 × 4 zone 的组合空间 | ✅ 12 个 + 4 zone | opt PR-06 对 applyToTags 稀释了针对性，组合感降格 |
| Hidden = 让探索变成"解谜" | ⚠️ 路径结构优秀 | Primer 提示缺失，玩家解的是"随机碰碰运气"的谜 |
| Loot = 秘境比主线更"值得"  | ❌ | 82% overlap，身份辨识度崩塌 |
| Terrain = 让地形成为"战术语言" | ⚠️ 40% exposure 达标 | 只有 1 个 zone（underground_river）真正体感完整 |
| Content Pack = 让 mod 作者能做"小扩展" | ✅ | ADD/REPLACE 路径完整；APPEND/DENY 尚未在 runtime 生效 |
| 度量体系 = 让"好不好玩"可量化 | ⚠️ | 度量到位，但 gate 是 aggregate 口径，漏掉 per-zone 体验 |

**核心反差**：**设计雄心是"全地图的探险"，度量体系验证的是"平均值合格"，玩家感受的是"前中后三段都有 Phase 3 断档"**。这是 Phase 4 opt 系列最需要在发版前修正的**体验学对齐**。

**Part 3 将给出 P0/P1/P2 的可执行优化清单**。
