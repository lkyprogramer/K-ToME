# Phase 4 PR 级执行文档 — 总监深度审阅

**日期**：2026-04-01
**审阅视角**：资深游戏设计与开发总监
**审阅轮次**：PR 级首轮深度审阅

**审阅范围**（9 份 PR 级执行文档）：

| PR | 文档 | 核心主题 |
| --- | --- | --- |
| PR-01 | `phase4-pr-01-mapgen-contract-and-smoke-baseline.md` | MapgenPipeline contract、TerrainTag、mapgenSmoke |
| PR-02 | `phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md` | 混合拓扑 planner、vault/pattern/biome |
| PR-03 | `phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md` | SolvabilityGraph、hidden entrance、proof golden |
| PR-04 | `phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md` | LootBudget/RarityTier/ZoneLootConfig |
| PR-05 | `phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md` | AffixCost、UNIQUE/ARTIFACT、LootBalanceLab |
| PR-06 | `phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md` | 地形交互、EliteMutation、BossVariant |
| PR-07 | `phase4-pr-07-hidden-event-secret-zone-and-client-readability.md` | HiddenEvent、SecretZone、client 可读性 |
| PR-08 | `phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md` | ContentPack overlay loader、lint、harness |
| PR-09 | `phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md` | 示例 pack、resource pipeline |

**参考权威文档**：

1. `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`
2. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

**审阅方法论**：以 Phase 3 已通过终审的 PR 级文档体系为质量基线，从架构完整性、系统联动、玩家体验、工程可行性、数值健康度五个维度进行审阅。本轮审阅覆盖三个层次：(1) 最大胆的架构重构建议；(2) 系统级优化建议；(3) 最细小的可改进点。

---

## 0. 总体评估

Phase 4 的 PR 级文档体系相较之前 r1~r5 审阅时的 274 行「设计意向书」已有 **质的飞跃**，9 份 PR 文档总计覆盖了完整的 Kotlin 接口骨架、YAML 示例、冻结公式、验证 harness 规格、资源生产计划。从「能否直接作为实现输入」的角度，文档已达到可执行级别。

但深度审阅后，仍发现以下结构性问题：

**问题统计**：

| 等级 | 数量 | 说明 |
| --- | --- | --- |
| **S 级**（架构重构） | 3 | 涉及系统边界重划或核心机制补充，不处理将导致 Phase 4 核心目标（重复游玩差异）打折 |
| **A 级**（系统优化） | 11 | 涉及契约完善、数值模型补强或跨 PR 一致性，不处理将导致实现阶段返工或系统间裂缝 |
| **B 级**（细节改进） | 14 | 涉及命名、字段补充、边界条件、YAML 示例完善等，不处理不阻塞但降低系统完成度 |

---

## 1. S 级 — 架构级重构建议

### S-1：缺失「体验差异预算」顶层设计 — Phase 4 核心目标无量化锚点

**影响范围**：全部 9 个 PR

**问题**：Phase 4 的核心目标是「重复游玩差异明显」，但 9 份 PR 文档分别描述了各自系统的技术契约，**没有任何文档定义「差异」的玩家感知标准**。具体表现：

1. **无「差异贡献预算」**：ProcGen（拓扑/vault/biome）、Loot（rarity/affix/unique）、Encounter（elite mutation/boss variant）、Hidden Content（hidden event/secret zone）四大系统各自贡献多少可感知差异？占比多少？没有定义。
2. **无「最小可感知差异」定义**：两次相同 zone 的地图在哪些维度不同才算「差异明显」？仅仅房间位置不同但拓扑结构相同算不算？vault 出现位置不同但内容相同算不算？
3. **mapgenSmoke 的 500 seed 阈值是纯工程健壮性指标**，不是体验指标。solvabilityHarness 验证的是「不死锁」而不是「够有趣」。
4. **hiddenContentHarness 的 30%/10% 触发率** 是否足够支撑「差异感知」？一个 10 次游玩中只触发 1 次 secret zone 的系统，对重复游玩差异的贡献微乎其微。

**建议 — 新增「体验差异预算」文档或章节**：

```
体验差异贡献预算（推荐定义）：
┌──────────────────────┬──────────┬──────────────────────────────┐
│ 差异维度             │ 贡献权重 │ 最小可感知差异阈值           │
├──────────────────────┼──────────┼──────────────────────────────┤
│ 拓扑结构差异         │ 30%      │ ≥2 个房间拓扑位置不同        │
│ 内容填充差异（vault）│ 20%      │ ≥1 个 vault 种类不同         │
│ 掉落品质差异         │ 20%      │ 稀有度分布标准差 > 阈值      │
│ 遭遇组合差异         │ 15%      │ ≥1 个 elite mutation 不同    │
│ 隐藏内容差异         │ 15%      │ ≥1 个 hidden event 不同      │
└──────────────────────┴──────────┴──────────────────────────────┘

验收方式：同一 zone 连续 5 次游玩，玩家能说出 ≥3 个感知层面的差异。
```

这不是美好愿望，而是 Phase 4 唯一的核心 KPI。没有它，各系统的验收标准就是纯技术指标，无法回答「Phase 4 完成后游戏是否真的更好玩」。

---

### S-2：PR-01 / PR-02 拆分粒度不合理 — 建议合并或重划边界

**影响范围**：PR-01、PR-02

**问题**：

PR-01 的内容是「定义接口 + BSP 兼容适配 + mapgenSmoke 骨架」，PR-02 是「实现混合拓扑 planner + 填充内容数据」。这个拆分存在以下问题：

1. **PR-01 交付物过薄**：PR-01 本质上是在给 BSP 穿新衣服（`BspBackedMapgenPipeline`），产出的是一个 pass-through wrapper。这不是一个有实质价值的交付单元——合并 PR-01 后游戏体验零变化，地图和之前完全一样。
2. **契约定义和首个消费者分离**：PR-01 定义 `TopologyGraph` 但 PR-01 的 BSP 适配器只是把 BSP 结果硬塞进新结构。真正验证契约是否合理的是 PR-02 的混合拓扑 planner。**先定义契约再验证合理性是瀑布思维**，容易导致 PR-02 发现契约不合适但改动成本已高。
3. **`GeneratedFloor.map: GameMap` 临时字段**在 PR-01 引入、PR-02 才能消除，造成跨 PR 的技术债窗口。

**建议**：

**方案 A（推荐）：合并 PR-01 和 PR-02**。一个 PR 完成「契约定义 + 最小混合拓扑实现 + mapgenSmoke」，确保契约和实现同步验证。BSP 兼容层作为 `fallback` 保留，而非独立 PR 的主要交付物。

**方案 B：如果坚持拆分**，将 PR-01 的范围扩大到包含「最小拓扑 planner 骨架」（至少能生成一个非 BSP 的 3-node 线性拓扑），让 PR-01 的产出能真正验证契约。PR-02 则聚焦于「丰富拓扑策略 + 内容填充 + 4 zone 升级」。

---

### S-3：Content Pack 的投入产出比存疑 — PR-08/PR-09 应考虑降级或后移

**影响范围**：PR-08、PR-09

**问题**：Phase 4 的核心目标是「重复游玩差异明显」。Content Pack 系统（PR-08 loader/lint + PR-09 示例 pack）对这个目标的直接贡献为零——它不会让任何现有玩家的游玩体验有任何改变。

从资源分配角度看：

1. **PR-08 + PR-09 消耗了约 22% 的 Phase 4 工作量**（2/9 PR），但产出仅是一个「示例数据包能被加载」的技术能力验证。
2. **Content Pack 的真正价值在 Phase 5+**（社区/DLC），Phase 4 急着做相当于提前投入但延迟收益。
3. **PR-08 的 overlay 语义（ADD/REPLACE/APPEND/DENY）设计复杂度不低**，但 Phase 4 的 sample pack 只用到 `ADD`，其他操作的正确性无法在 Phase 4 内验证。
4. **`DENY` 操作在 Phase 4 完全没有消费场景**，属于过度设计。

**建议**：

**方案 A（推荐）：将 PR-08/PR-09 降级为 P4-C 的「最小可行切片」**：
- PR-08 只实现 `ADD` 操作 + manifest 解析 + 基本 lint（不实现 REPLACE/APPEND/DENY）
- PR-09 缩小为「最小 smoke test pack」（1 个 hidden event + 1 个 item），不做完整资源管线
- 节省的工作量投入到 S-1 的体验差异预算系统和 A-1~A-4 的系统优化中

**方案 B：将 PR-08/PR-09 整体后移到 Phase 4.5 或 Phase 5 前置**，Phase 4 退出条件改为「Content Pack 契约文档冻结但不要求实现」。

---

## 2. A 级 — 系统优化建议

### A-1：LootBudget 缺少保底机制（Pity System）— 长局体验风险

**影响范围**：PR-04、PR-05

**问题**：当前掉落模型是纯加权随机，没有任何保底机制：

1. `ARTIFACT` 的 `baseWeight` 仅为 1（vs NORMAL 720），即使 BOSS + 满 magicFind 后权重修正为 `1 × (1 + 0.90 × 2.00) = 2.80`，在总权重中占比仍极低。
2. 一个 8 层长局（~40 次掉落机会），玩家有合理概率 **全局 0 件 UNIQUE**。对于 Phase 4 强调的「掉落差异可感知」目标，这是反模式。
3. 权威文档中的 `AffordableRescueSlotPolicy` 被提及但从未在 PR-04/05 的实现中出现。

**建议**：

在 `LootBudgetResolver` 中增加保底计数器：

```kotlin
data class PityTracker(
    val rollsSinceLastRare: Int = 0,    // 连续未出 RARE+ 的次数
    val rollsSinceLastUnique: Int = 0,  // 连续未出 UNIQUE+ 的次数
)
```

规则：
- 连续 N 次（如 20 次）未出 RARE+，下次 RARE 权重 ×2.0
- 连续 M 次（如 50 次）未出 UNIQUE+，下次 UNIQUE 权重 ×3.0
- ARTIFACT 不设保底（保持其极端稀有性），但 secret zone 的 `guaranteedContent` 应成为 ARTIFACT 的受控发放渠道

这是 Roguelike 领域的成熟实践（Hades、Slay the Spire 都有类似机制），不增加系统复杂度但显著改善尾部体验。

---

### A-2：Elite Mutation 缺少组合规则与层级缩放 — 遭遇差异天花板低

**影响范围**：PR-06

**问题**：

1. **无最大 mutation 数量**：`EliteMutationDef` 描述了单个 mutation 的结构，但没有定义一个 elite 可以携带多少 mutation。1 个？2 个？不限？
2. **无 mutation 互斥/协同规则**：`STAT_PACKAGE`（+物理防御）和 `ELEMENT_PACKAGE`（+火穿透）可以叠加吗？`AI_SHIFT`（激进）和 `AURA`（治疗光环）是否矛盾？
3. **无层级缩放**：Floor 1 的 elite 和 Floor 8 的 elite 使用相同 mutation pool。没有按层级过滤/加权的机制。
4. **`applyToTags` 是唯一的过滤维度**，没有 `minFloor / maxFloor / zoneFilter`。

**建议**：

在 `EliteMutationDef` 中补充：

```kotlin
data class EliteMutationDef(
    // ...existing fields...
    val mutationTier: Int,                    // 1~3，控制出现的楼层范围
    val incompatibleMutations: Set<String>,   // 互斥 mutation ID
    val maxMutationsPerElite: Int = 2,        // 全局默认值，可被 zone override
)
```

同时在 zone 或 mapgen profile 中增加：

```yaml
eliteMutationConfig:
  maxMutationsPerElite: 2
  tierRange: [1, 2]           # 该 zone 允许的 mutation tier
  mutationWeights:            # 可选：per-zone mutation 权重覆盖
    fire_aura: 1.5            # 火焰区域偏好火系 mutation
```

---

### A-3：BossVariantDef 过于受限 — 变体感知度不足

**影响范围**：PR-06

**问题**：

`BossVariantDef` 只能 overlay 三样东西：mutations、loot、visual tint。**不能修改 Boss 的阶段行为**。这意味着：

1. Boss 变体在战斗体验上的差异仅来自「多了一些属性/技能」（mutation），核心战斗节奏（阶段触发条件、阶段技能池、召唤波次）完全不变。
2. 玩家第二次打同一个 Boss 变体时，仍然能准确预测所有阶段转换节点——这与「重复游玩差异」目标矛盾。

**建议**：

在保持 `BossEncounterDef` 阶段图不变的前提下，允许 variant 做**受控的阶段参数覆盖**：

```kotlin
data class BossVariantDef(
    // ...existing fields...
    val phaseOverrides: Map<String, BossPhaseOverride>?,  // key = phaseId
)

data class BossPhaseOverride(
    val hpThresholdDelta: Float? = null,   // 阶段触发血量偏移（如 -0.05 = 提前 5% 血量进入）
    val enabledTalentAppend: List<String>? = null,  // 追加技能（不替换原有）
    val summonPackOverride: String? = null, // 替换召唤包
)
```

这不是「重写阶段图」（那是 Phase 3 的 `BossEncounterDef` 契约），而是「在阶段图骨架上调节参数」。效果：Boss 可能提前进入 Phase 2（因为 `hpThresholdDelta`），或者 Phase 2 会召唤不同的小怪包——**同一个 Boss 的不同变体在战斗节奏上有可感知差异**。

---

### A-4：Hidden Content 发现机制过于被动 — 玩家主动性不足

**影响范围**：PR-03、PR-07

**问题**：

当前隐藏内容的发现方式全部是被动触发：

```
ENTER_ROOM → 自动
OPEN_CHEST → 开箱
KILL_ELITE → 击杀
INTERACT_TILE → 交互
QUEST_STEP → 任务
PERCEPTION_REVEAL → 感知检定
```

除了 `PERCEPTION_REVEAL` 有一个 `difficulty` 门槛外，其他都是「走到就触发」或「做了某件其他事顺便触发」。**玩家没有主动搜索/探索隐藏内容的机制**。

对比成熟 Roguelike：
- ToME 4：玩家可以主动搜索隐藏门（Search action）
- DCSS：玩家可以挖掘墙壁到达隐藏区域
- Hades：特定的 NPC 关系链解锁隐藏房间

**建议**：

1. 增加 `ACTIVE_SEARCH` 触发类型——玩家在特定房间主动使用「搜索」动作：

```kotlin
enum class HiddenTriggerType {
    // ...existing...
    ACTIVE_SEARCH,  // 玩家主动搜索，消耗回合
}
```

2. 在 `DiscoveryRule` 中允许多条件组合（AND 逻辑）：

```kotlin
data class DiscoveryRule(
    val type: DiscoveryRuleType,
    val difficulty: Int? = null,
    val requiredTag: String? = null,
    val secondaryCondition: DiscoveryRule? = null,  // 可选的二级条件
)
```

例如：「在有 WATER 标签的房间 + 主动搜索 + 感知检定 DC14」才能发现隐藏入口。这让玩家有理由在探索时做出主动决策。

---

### A-5：TerrainTag 种类过少 — 与现有 Zone 描述不匹配

**影响范围**：PR-01、PR-02、PR-06

**问题**：

Phase 4 冻结 `TerrainTag` 仅 3 种：`WATER / OIL / ICE`。但基础文档中 Deep Iron Pit 的描述明确包含「Lava (fire DoT)」，Abyssal Temple 描述包含暗河（dark river）。

- `WATER + LIGHTNING → chain` ✓
- `OIL + FIRE → ignite` ✓
- `WATER + COLD → freeze` ✓
- **Lava + 任何 → ???** ✗（无对应 TerrainTag）

3 种 terrain interaction 对于 4 个升级 zone 来说过于单薄，尤其是 Deep Iron Pit（mine + forge）和 Abyssal Temple（ruin + oil_catacomb）缺少独特的地形体验。

**建议**：

Phase 4 增加 `LAVA` 作为第 4 个 TerrainTag：

```kotlin
enum class TerrainTag {
    WATER,
    OIL,
    ICE,
    LAVA,  // Phase 4 新增
}
```

对应交互规则：
- `LAVA` + 站立：持续 FIRE DoT（与 OIL 燃烧共享 BURN 状态但独立触发）
- `COLD + LAVA → OBSIDIAN`：熔岩冷却为黑曜石地形（可通行，3 回合后恢复）

这与 Deep Iron Pit 的「furnace/lava」主题完美契合，且复用现有 `ElementInteractionRule` 框架，实现成本低。

---

### A-6：PR-04 iLvl 公式中的 `uniformInt(-1, 1)` 引入不必要的负面体验

**影响范围**：PR-04

**问题**：

```
iLvl = clamp(sourceLevel + sourceTier.itemLevelBonus + uniformInt(-1, 1), 1, playerLevel + 3)
```

`uniformInt(-1, 1)` 意味着同一 source 掉落的 iLvl 有 ±1 的随机波动。对于 BOSS 掉落，这会导致：

1. BOSS 掉落可能比同层普通怪掉落的 iLvl 更低（BOSS +2 -1 = +1，普通怪 +0 +1 = +1）
2. 对玩家的直觉冲击：「我打了半天 BOSS 结果掉了个比小怪还差的东西」

`iLvl` 的微小波动对 build diversity 贡献极低，但负面情绪成本高。

**建议**：

将 `uniformInt(-1, 1)` 改为 `uniformInt(0, 1)` — 只允许向上波动：

```
iLvl = clamp(sourceLevel + sourceTier.itemLevelBonus + uniformInt(0, 1), 1, playerLevel + 3)
```

或者完全移除随机波动，让 `iLvl` 成为确定性函数（差异性通过 rarity 和 affix 随机化提供）。

---

### A-7：SolvabilityGraph 未考虑回溯场景

**影响范围**：PR-03

**问题**：

`SolvabilityProof` 的验证算法是「前向可达性证明」——从 entry 出发，收集 key，扩展 frontier。但实际游戏中：

1. 玩家可能先走 OPTIONAL 路径拿到 key，再回到主路径使用
2. 玩家可能需要回溯（走过的门仍可通行）
3. 某些 `SECRET` 路径的 `returnBridgeNodeId` 意味着存在回溯边

当前 proof 算法是否处理了双向边？文档描述的是 DAG（有向无环图），但实际地图拓扑是有环的（PR-02 引入了 loop）。

**建议**：

在 PR-03 文档中明确补充：

1. **SolvabilityGraph 的边是否双向**：所有物理通道默认双向可通行（除非有单向机关）
2. **proof 算法必须支持回溯**：frontier 扩展时，已访问节点的 key 应回溯可用于未展开的边
3. **增加回溯测试用例**：至少一个 golden proof 要求「先走 OPTIONAL 拿 key → 回到主路径 → 用 key 开门」

---

### A-8：PR-06 依赖链条过长 — 需要同时等待 PR-03 和 PR-05

**影响范围**：PR-06、整体执行计划

**问题**：

PR-06 声明依赖 PR-03（solvability）和 PR-05（loot balance lab）。但仔细分析实际内容依赖：

1. **PR-06 的 terrain interaction** 只需要 PR-01 的 `TerrainTag` 和 PR-02 的 `GeneratedFloor` — 不需要 PR-03 的 solvability
2. **PR-06 的 elite mutation** 需要怪物模板和 AI profile — 这是 Phase 3 的交付物，不需要 PR-05 的 affix cost
3. **PR-06 的 boss variant** 需要 `BossEncounterDef` — 同样是 Phase 3 交付物

真正需要 PR-03/PR-05 的是 PR-07（hidden event 的 reward 需要 loot budget，secret zone 需要 solvability proof）。

**建议**：

将 PR-06 的依赖从 `{PR-03, PR-05}` 放宽为 `{PR-02}`（仅需要 `GeneratedFloor` + `TerrainTag`），将 PR-06 和 PR-04/PR-05 并行开发：

```
修正后的依赖图：
PR-01 → PR-02 → PR-03 ──────────────────────→ PR-07 → PR-08 → PR-09
              ↘ PR-06（与 PR-03 并行）──────↗
PR-04 → PR-05 ──────────────────────────────↗
```

这可以将 Phase 4 的关键路径缩短 1 个 PR 周期。

---

### A-9：mapgenSmoke / solvabilityHarness / lootBalanceLab / hiddenContentHarness / contentPackHarness 报告格式缺少统一规范

**影响范围**：PR-01、PR-03、PR-05、PR-07、PR-08

**问题**：

5 个 harness/lab 分别在各自 PR 中定义了独立的报告格式，但缺少统一的：

1. **公共元数据头**：每个 harness 的 `buildId / phaseId / contentSchemaVersion / timestamp` 格式略有不同
2. **报告聚合入口**：没有一个 `./gradlew phase4Report` 命令能一次跑完所有 harness 并输出汇总
3. **Phase 5 消费契约**：Phase 5 的 soak test / DeathAnalysis 需要消费 Phase 4 的多个 harness 报告，但报告间没有可 join 的 key

**建议**：

1. 定义公共报告头：

```kotlin
data class HarnessReportHeader(
    val harnessId: String,        // e.g., "mapgenSmoke", "lootBalanceLab"
    val phaseId: String,          // "P4"
    val buildId: String,
    val contentSchemaVersion: Int,
    val timestamp: Instant,
    val seedList: List<Long>,
)
```

2. 增加 `./gradlew phase4Report` 根别名，顺序调用所有 harness 并输出到 `tools/build/reports/phase4/phase4-summary.json`

3. 所有 per-seed 报告使用 `seed` + `zoneId` + `floorIndex` 作为可 join 的复合 key

---

### A-10：PR-05 AffixCost 成本带跳跃过大 — 预算浪费风险

**影响范围**：PR-05

**问题**：

当前 cost bands：`MINOR(3) / MEDIUM(6) / MAJOR(10) / SIGNATURE(14)`

假设一个 RARE 物品的 `affixBudget = 20`（qLvl=3 的低层 RARE），需要 2~4 affix：
- 4 × MINOR = 12（浪费 8 预算）
- 2 × MEDIUM + 1 × MINOR = 15（浪费 5 预算）
- 1 × MAJOR + 1 × MEDIUM = 16（浪费 4 预算）
- 1 × SIGNATURE + 1 × MINOR = 17（浪费 3 预算）

低层 RARE 很难凑满预算——**cost band 之间的间距太大**，导致低预算物品总是有大量浪费。

**建议**：

增加一个 `TRIVIAL(1)` 成本带，或将 MINOR 降为 2：

```
TRIVIAL(1)：纯数值小加成（+3 单属性抗性）
MINOR(3)：标准基础属性
MEDIUM(6)：中等强度
MAJOR(10)：高强度
SIGNATURE(14)：定义性词缀
```

同时在 affix 装配算法中增加「预算残值处理」：若剩余预算 ≤ 最小 affix cost，允许选择一个 cost ≤ 剩余预算的 bonus affix（从 TRIVIAL 池中选）。

---

### A-11：PR-07 的 `returnBridgeNodeId` 仅为 String — 缺少完整性校验

**影响范围**：PR-07

**问题**：

```kotlin
data class SecretZoneDef(
    // ...
    val returnBridgeNodeId: String,  // 指向主路径的返回节点
)
```

这个字段仅是一个 String ID，但：

1. **没有在 `SolvabilityGraph` 中被自动验证**：如果 `returnBridgeNodeId` 指向一个不存在的节点，或指向一个需要 key 才能进入的节点（导致从 secret zone 出来后被卡住），当前契约无法检测。
2. **PR-03 的 solvabilityHarness 不检查 return bridge**：PR-03 冻结的 proof 只验证「entry → exit 可达」，不验证「secret zone → return bridge → 主路径可达」。

**建议**：

1. 在 `SolvabilityGraphBuilder` 中，将 `returnBridgeNodeId` 作为 `SECRET → returnBridge` 的一条边加入 graph
2. 在 solvabilityHarness 的 proof 中增加检查：从 secret zone 的所有节点出发，`returnBridgeNodeId` 必须无条件可达（不需要额外 key）
3. `returnBridgeNodeId` 必须指向 `CRITICAL_PATH` 或 `OPTIONAL` 节点（不能指向另一个 `SECRET`）

---

## 3. B 级 — 细节改进建议

### B-1：PR-01 `GeneratedFloor.map: GameMap` 临时字段需要消除计划

PR-01 引入了 `GeneratedFloor.map: GameMap` 作为临时兼容字段，但文档仅说「PR-02 可以替换 planner 而不触及 session 主路径」。应明确：
- 该字段在哪个 PR 被移除？（建议 PR-02 退出时）
- 移除后 session/floor 组装如何消费 `GeneratedFloor`？

---

### B-2：PR-01 `TopologyNode` 缺少 `pathClass` 字段

PR-01 定义的 `TopologyNode` 没有 `pathClass`：

```kotlin
data class TopologyNode(
    val id: String,
    val roomDefId: String,
    val tags: Set<String>,
)
```

但 PR-03 的 `SolvabilityNode` 需要 `pathClass`。这意味着 PR-03 必须从其他来源推断 `pathClass`，或者 PR-02 需要补上。

**建议**：PR-01 直接在 `TopologyNode` 中增加 `pathClass: PathClass` 字段（初始值可设为 `CRITICAL_PATH`，后续 PR 再丰富）。

---

### B-3：PR-02 VaultDef 的 `threatBudget` / `rewardBudget` 缺少与 LootBudget 的换算规则

PR-02 定义了：
- `threatBudget: Int` — `~14 = 3 普通怪 + 1 elite`
- `rewardBudget: Int` — `~18 = 2 高品质掉落 OR 1 secret chest`

但这两个「预算货币」与 PR-04 的 `LootBudget.affixBudget` 和 `SourceTier` 之间的换算关系未定义。vault 的 `rewardBudget = 18` 具体转化为多少次 `LootRollContext` 调用？每次调用的 `sourceTier` 是什么？

**建议**：在 PR-02 或 PR-04 中补充换算规则。例如：

```
rewardBudget 消耗规则：
- 1 standard loot roll (NORMAL tier) = cost 5
- 1 quality loot roll (ELITE tier) = cost 9
- 1 secret chest = cost 18
```

---

### B-4：PR-03 `DiscoveryRule.difficulty` 没有明确数值标尺

`difficulty: Int` 用于 `PERCEPTION_CHECK`，但文档仅给出了一个示例值 `14`。没有定义：
- 玩家感知值的期望范围
- difficulty 的合理区间
- 与 Phase 3 的 Power/Save 系统如何对接

**建议**：补充标尺定义：

```
difficulty 标尺（对标 Power/Save 系统）：
DC 8  — 容易（大多数角色可通过）
DC 12 — 中等（需要适度投入感知相关属性）
DC 16 — 困难（需要专精探索型 build）
DC 20 — 极难（仅特定 class + 装备组合）

检定方式：perception = mentalPower (或 spellPower)
成功条件：perception ≥ difficulty
```

---

### B-5：PR-04 `RarityTier` 的 weight 总和与概率不直观

当前 base weights：`NORMAL(720) + MAGIC(220) + RARE(50) + UNIQUE(9) + ARTIFACT(1) = 1000`

这个设计虽然总和恰好 1000（方便计算），但修正后的权重不再是 1000，导致概率计算需要重新归一化。建议在文档中补充一个「基线概率表」：

```
基线概率（无 rarityBonus、magicFind=0、SourceTier=NORMAL）：
NORMAL   = 72.0%
MAGIC    = 22.0%
RARE     = 5.0%
UNIQUE   = 0.9%
ARTIFACT = 0.1%

BOSS + magicFind=0.5 后的概率（rarityScore=0.65）：
NORMAL   = ~63.8%
MAGIC    = ~24.7%
RARE     = ~8.2%
UNIQUE   = ~2.9%
ARTIFACT = ~0.4%
```

这让设计师和 QA 无需自行计算即可评估体验。

---

### B-6：PR-05 UNIQUE/ARTIFACT 模板缺少最小数量要求

PR-05 声明 UNIQUE 和 ARTIFACT 使用预定义模板池，但没有定义 Phase 4 需要交付多少个模板。

**建议**：

```
Phase 4 最小模板数量：
- UNIQUE 模板：≥ 12 个（每 zone ≥ 3 个候选）
- ARTIFACT 模板：≥ 4 个（每 zone ≥ 1 个候选）
- 每个 UNIQUE/ARTIFACT 必须有独立 nameKey/descKey/iconKey/visualKey
```

没有最小数量约束，PR-05 可能只交付 2~3 个模板就声称「系统完成」。

---

### B-7：PR-06 terrain interaction 只有 3 条规则 — 至少补充 ICE 相关

Phase 4 冻结了 3 条 terrain interaction：
- `terrain_lightning_water_chain`
- `terrain_fire_oil_ignite`
- `terrain_cold_water_freeze`

但 `ICE` 作为 `TerrainTag` 的三大成员之一，没有独立的交互规则。`ICE` 地形目前仅通过 `COLD + WATER → freeze to ICE` 间接产生，但 `ICE` 地形本身对战斗的影响未定义（如：站在 ICE 上被击中是否有 knockdown 检定？FIRE + ICE 是否融化为 WATER？）。

**建议**：补充至少 2 条 ICE 相关规则：

```yaml
- id: terrain_fire_ice_melt
  triggerElement: FIRE
  conditionType: TARGET_ON_TERRAIN
  conditionParam: ICE
  effectType: TERRAIN_TRANSFORM
  effectParams:
    resultTerrain: WATER
    vfxKey: vfx_ice_melt

- id: terrain_ice_knockdown
  triggerElement: ANY_PHYSICAL   # 物理攻击
  conditionType: TARGET_ON_TERRAIN
  conditionParam: ICE
  effectType: KNOCKDOWN_CHECK
  effectParams:
    saveType: PHYSICAL
    saveDC: 12
    vfxKey: vfx_ice_slip
```

---

### B-8：PR-07 hiddenContentHarness 的阈值缺少统计学依据

文档规定「100 seeds，≥30% 触发 1 hidden event，≥10% 发现 1 secret zone」。

问题：
1. 100 seeds 的样本量对于检测 10% 的事件来说统计功效不足（置信区间宽达 ±6%）
2. 阈值本身缺少设计依据——为什么是 30% 而不是 50%？

**建议**：
- 将 seed 数量增加到 500（与 mapgenSmoke 对齐）
- 补充阈值的设计依据：`30% hidden event rate` 意味着「平均每 3.3 次游玩发现 1 次新 hidden event」
- 增加方差检查：不仅检查平均率，还检查「是否存在某些 zone 永远不触发 hidden event」

---

### B-9：PR-08 `OverlayOp.DENY` 在 Phase 4 无消费场景

`DENY` 操作的语义是「显式禁用可选内容」。但 Phase 4 的 sample pack 是 ADD-only 的内容扩展包，不需要禁用任何基础游戏内容。

**建议**：
- 在 PR-08 中将 `DENY` 标记为 `Phase 5+`，Phase 4 仅实现 `ADD / REPLACE / APPEND`
- 减少实现和测试范围，降低 PR-08 的复杂度

---

### B-10：PR-09 示例 pack 只有 1 个 — 无法验证 pack 间冲突

单一 sample pack 无法验证：
- 两个 pack 的 namespace 冲突检测
- 依赖链（A depends B）的加载顺序
- `REPLACE` 操作的多 pack 优先级

**建议**：
- 增加一个最小化的第二 pack（如 `sample.iron_shadows`），仅含 1 个 item overlay，用于测试双 pack 加载
- 如果工作量不允许，至少在 `contentPackHarness` 中用 fixture 模拟双 pack 场景

---

### B-11：PR-04 `magicFindBonus` clamp 到 1.0 后剩余值被丢弃 — 缺少溢出处理说明

文档说 `effectiveMagicFind = clamp(magicFindBonus, 0.0, 1.0)`，但 PR-05 的 lootBalanceLab 矩阵中有 `magicFind=1.50` 的测试场景。

如果 1.50 被 clamp 到 1.0，那 1.50 和 1.00 的结果应该完全一致。这意味着：
- 要么 lootBalanceLab 的 1.50 场景是验证 clamp 正确性的（应在文档中说明）
- 要么溢出的 0.50 应该有某种转化（如微量 iLvl 加成）

**建议**：在 PR-04 文档中明确：「`magicFind > 1.0` 的场景用于验证 clamp 边界条件；溢出部分无任何效果，不转化。」

---

### B-12：全部 PR 的资源计划文件路径使用 Gemini 命名 — 建议统一

PR-05/06/07/09 的资源计划路径均为 `assets-src/image/specs/phase4-prNN-gemini-plan.yaml`。`gemini` 这个命名暗示了特定的资源生成工具，但这应该是工具无关的规格文件。

**建议**：重命名为 `phase4-prNN-asset-plan.yaml`，或至少在文档中解释为什么叫 `gemini-plan`。

---

### B-13：PR-02 zone 升级 rarityBonus/qualityBonus 值缺少设计依据

```
greenwood_fringe:  rarityBonus=0.00, qualityBonus=0
deep_iron_pit:     rarityBonus=0.05, qualityBonus=1
underground_river: rarityBonus=0.08, qualityBonus=1
abyssal_temple:    rarityBonus=0.12, qualityBonus=2
```

递增趋势合理，但具体数值缺少锚定。`0.12` vs `0.08` 的 4% 差异在实际掉落分布中的体感差异有多大？

**建议**：在文档中补充「rarityBonus 每 0.05 增量对 RARE 出现率的影响百分比」，让 QA 和设计师能判断数值是否合理。例如：

```
rarityBonus 影响速查：
0.00 → RARE 基线 5.0%
0.05 → RARE ~5.25%
0.10 → RARE ~5.50%
0.15 → RARE ~5.75%
（以上为 SourceTier.NORMAL + magicFind=0 基线）
```

---

### B-14：PR-03 `SolvabilityProof` 缺少 `optionalPathCount` 和 `secretPathCount` 统计

当前 proof 输出：

```kotlin
data class SolvabilityProof(
    val criticalPathReachable: Boolean,
    val acquiredKeys: List<String>,
    val unresolvedRequirements: List<String>,
    val visitedNodes: List<String>,
)
```

缺少地图丰富度的统计信息。

**建议**：补充：

```kotlin
data class SolvabilityProof(
    // ...existing...
    val optionalPathCount: Int,    // 可达的 OPTIONAL 路径数
    val secretPathCount: Int,      // 可达的 SECRET 路径数
    val totalReachableNodes: Int,  // 总可达节点数
    val reachabilityRatio: Float,  // 可达节点 / 总节点
)
```

这些统计量是 S-1「体验差异预算」的直接输入。

---

## 4. 跨 PR 一致性检查

### 4.1 术语一致性

| 术语 | PR-01/02 定义 | PR-03 定义 | 差异 |
| --- | --- | --- | --- |
| `TopologyNode.pathClass` | **未定义** | `PathClass` enum | PR-01 缺失，PR-03 需要消费 |
| `HiddenEntranceInstance` | PR-01 `GeneratedFloor` 中有 | PR-03 `HiddenEntranceDef` | 结构名不同，需确认是否同一概念 |
| `TopologyEdge.requiredKeys` | PR-01 仅有 `isLoop` | PR-03 需要 `requiredKeys` | PR-02 应补充 |

**建议**：在 PR-01 文档中预留 `pathClass` 和 `requiredKeys` 的字段（即使初始值为空/默认），避免 PR-02/03 需要修改 PR-01 的数据结构。

### 4.2 依赖链完整性

```
声明的依赖：
PR-01 → PR-02 → PR-03 → PR-07 → PR-08 → PR-09
                        ↗
PR-04 → PR-05 → PR-06 ─┘

实际最小依赖（建议修正）：
PR-01 → PR-02 ──→ PR-03 ──→ PR-07 → PR-08 → PR-09
              ↘ PR-06 ────↗
PR-04 → PR-05 ──────────↗
```

关键改进：PR-06 不需要等待 PR-03 和 PR-05，可以在 PR-02 之后与 PR-03/PR-04/PR-05 并行。

### 4.3 harness 覆盖矩阵

| 验证维度 | 负责 harness | 首次可用 PR | 最终消费 PR |
| --- | --- | --- | --- |
| 地图拓扑健壮性 | mapgenSmoke | PR-01 | PR-02 完善 |
| 可解性证明 | solvabilityHarness | PR-03 | PR-07 消费 |
| 掉落分布 | lootBalanceLab | PR-05 | PR-07 消费 |
| 隐藏内容触发率 | hiddenContentHarness | PR-07 | PR-09 消费 |
| 内容包完整性 | contentPackHarness | PR-08 | PR-09 消费 |

**缺失**：没有一个 harness 验证「地形交互 + 战斗管线」的集成正确性。PR-06 引入 terrain interaction 后，应有一个 `terrainInteractionSmoke` 或将其纳入现有 `combatTrace` golden 体系。

---

## 5. 最终建议优先级排序

### 必须在 PR 实现前解决（阻塞级）

| ID | 建议 | 影响 PR |
| --- | --- | --- |
| S-1 | 新增体验差异预算 | 全部 |
| A-1 | LootBudget 保底机制 | PR-04/05 |
| A-2 | Elite Mutation 组合规则 | PR-06 |
| A-7 | SolvabilityGraph 回溯支持 | PR-03 |
| B-2 | TopologyNode 补充 pathClass | PR-01 |

### 建议在实现中同步处理

| ID | 建议 | 影响 PR |
| --- | --- | --- |
| S-2 | PR-01/02 合并或重划 | PR-01/02 |
| A-3 | BossVariant 阶段参数覆盖 | PR-06 |
| A-4 | Hidden Content 主动搜索 | PR-03/07 |
| A-6 | iLvl 移除负向随机 | PR-04 |
| A-8 | PR-06 依赖放宽 | PR-06 |
| A-10 | AffixCost 增加 TRIVIAL 带 | PR-05 |

### 可延后但应计划

| ID | 建议 | 影响 PR |
| --- | --- | --- |
| S-3 | Content Pack 降级/后移 | PR-08/09 |
| A-5 | TerrainTag 增加 LAVA | PR-01/06 |
| A-9 | harness 报告统一规范 | 全部 |
| A-11 | returnBridgeNodeId 校验 | PR-07 |

---

## 6. 总结

Phase 4 的 PR 级文档在技术契约层面已达到高质量标准：接口骨架完整、公式冻结明确、YAML 示例充分、harness 规格清晰。这是一个可执行的文档体系。

但从「游戏设计总监」的视角看，文档体系存在一个根本性的视角盲区：**所有文档都在回答「系统如何工作」，没有文档在回答「玩家如何感知差异」**。Phase 4 的核心目标是「重复游玩差异明显」，但 9 个 PR 中没有任何一个 PR 的退出条件包含体验层面的验收指标。mapgenSmoke 验证的是「不崩溃」，lootBalanceLab 验证的是「分布正确」，solvabilityHarness 验证的是「不死锁」——这些都是必要条件，但不是充分条件。

**核心建议**：在开始实现之前，先完成 S-1（体验差异预算），让每个 PR 的退出条件中包含至少一个体验层面的验收指标。这不需要大量额外工作——只需要在现有 harness 的输出中增加若干统计量（如拓扑相似度、vault 种类方差、掉落品质方差、遭遇组合方差），并设定阈值。

Phase 4 的技术骨架已经稳固。现在需要的是一个「灵魂」——一个能回答「什么叫足够不同」的标准。
