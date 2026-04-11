> 本报告为事后审查（post-development review），对照合同文档：
> `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-06-terrain-readability-and-tactical-uptake-tuning.md`
> `docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json`
> 以及实际落盘的运行结果 `tools/build/reports/phase4/phase4-summary.md|json`。

# Phase 4 · OPT PR-06 开发落地审查报告（2026-04-11）

**审查角色**: 资深 Roguelike/类 ToME 开发总监 + 系统策划总监 + 玩法体验审查负责人  
**审查范围**: 分支 `codex/phase4-opt-pr06-terrain-uptake-tuning` 当前工作树相对 `main` 的全部改动  
**结论优先级**: **NOT SHIPPABLE** — 存在 3 个 P0 级阻断问题，必须在合入前修复。

---

## 0. 快速裁决（TL;DR）

| 合同出口门 | 当前值 | 合同要求 | 裁决 |
| --- | --- | --- | --- |
| `terrainTaggedCombatExposureRate` | **34.0%** (240/706) ，Δ=**+50.48%** | ≥ 33.89%（+50.00%） | **PASS (刀口)** |
| `terrainInteractionEncounterRate` | **12.1%** (29/240)，Δ=**−4.00%** | ≥ 16.36%（+30.00%） | **FAIL (回归基线以下)** |
| `solvabilityHarness` 主线可达 | 1000/1000 | 100% | PASS |
| 不新增 `TerrainTag` | 仅使用 WATER/ICE/OIL | 不新增 | PASS |
| `phase4Report` provenance (baseline path / version / current / baseline / delta / target / decisionPath) | 全字段齐全 | 全字段齐全 | PASS |

**但是**，实际落盘的 `phase4-summary.md` 头部仍显示：

```
- taskCount: `12`
- passedTaskCount: `12`
- failedTaskCount: `0`
```

即使 `terrainInteractionEncounterRate` 在「体验度量基线」表格中写着 `FAIL`，聚合计数仍然把 12 个 task 全部算作 PASS。**这意味着出口门禁实际未生效**，CI 不会因为 encounter 回归而红脸，与合同 §8 的强制门禁意图直接冲突。

下述 3 个 P0 问题必须在本 PR 合入前闭环；否则即便绿灯也只是视觉绿灯。

---

## 1. P0 阻断项（必须修复）

### P0-1 · Encounter 门禁结构性失败：Δ=−4.00% vs baseline

**事实（来自 `tools/build/reports/phase4/phase4-summary.md`）**：

```
| terrainTaggedCombatExposureRate | 34.0% (240/706), delta=+50.48% vs baseline 22.6% | ... | >= 33.89% | PASS |
| terrainInteractionEncounterRate | 12.1% (29/240),  delta=-4.00% vs baseline 12.6% | ... | >= 16.36% | FAIL |
- decisionPathByCurrentMetrics = "Path B"
```

**事实（来自 `tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json`）**，按 zone 展开：

| zone | combatCount | taggedCombatCount | triggeredInteractionCombatCount | exposureRate | encounterRate |
| --- | --- | --- | --- | --- | --- |
| greenwood_fringe | 386 | 156 | 8 | 40.4% | **5.1%** |
| deep_iron_pit | 43 | 11 | 3 | 25.6% | 27.3% |
| underground_river | 113 | 48 | 13 | 42.5% | 27.1% |
| crystal_cavern | 164 | 25 | 5 | 15.2% | 20.0% |

**偏差量化**：

- 合同要求 encounterRate ≥ 16.36% 即 baseline 12.59% × 1.30
- 实际 12.08%，相对基线 **−4.00%**（绝对 −0.51pp，相对目标 −4.28pp）
- 相对 OPT PR-01 baseline **负向回归**，这不是"差一点"，是"比没做调优还差"

**根因**（按可能性排序）：

1. **曝光分母被过度充气而触发分子没有等比增长**  
   `combatCount` 从基线 633 升到 706（+11.5%），但 `taggedCombatCount` 从 143 升到 240（**+67.8%**）。本 PR 在 Path A 动作上同时叠加了：
   - `HybridTopologyPipeline.paintTerrainTags` / `MapgenInternals` 将 `TERRAIN_DENSITY_SCALE` 从历史的 `0.14/0.18f` 提到 `0.64f`，同时删除了 `weight / totalWeight` 归一（见 P2-1 讨论）。
   - `game/src/main/resources/data/mapgen/zones/index.yaml` 把 `greenwood_fringe WATER 0.22→0.44`（翻倍）、`underground_river WATER 0.28→0.36 / ICE 0.16→0.20`、`deep_iron_pit OIL 0.24→0.32`。

   两者叠加让 WATER/ICE/OIL tile 在房间候选点中的占比成倍放大，分母 `taggedCombatCount` 立即被撑大；但能触发 `triggeredInteractionCombatCount` 的只有「会打冰/水/油的攻击类型 × 正在上面打的怪」这一交集，本 PR 并没有扩大该交集——分子只从 18 走到 29。

2. **greenwood_fringe 是本 PR 的稀释震中**  
   156 次 tagged combat 只换来 8 次触发（5.1%）。greenwood_fringe 当前 `monsterPools = [beast.rat, beast.thorn_stalker, undead.bone_archer, undead.moss_archer, bandit.trapper, bandit.archer]`——全部物理 + 普通射击 AI，没人会丢 `terrain_cold_water_freeze` 以外的规则的冷/火/雷技能。WATER 拉满到 0.44 之后，低楼层怪就被塞进一大片"看着冷但没人会触发"的水面，`encounterRate` 在这里被结构性拖到 5.1%。

3. **Path B 动作没有实际发生**  
   合同 §4.1 明确写：「Path B：exposureRate ≥20% 但 encounterRate <15% → 调精英/战斗与地形相遇概率」。本 PR 做了 Path A（覆盖），Path C 的可感知性没碰，Path B 的"让精英与地形相遇"**理论上做了**（新增 `preferredTerrainTags` + 地形权重加分），但**运行时完全没生效**——这是下面 P0-2 的独立问题。

**修复方向（必须做其一）**：

- **（首选）收紧 Path A 的投放强度，让分母不被过度稀释**。把 `TERRAIN_DENSITY_SCALE` 回调到 `0.32f ~ 0.40f`，同时把 `greenwood_fringe WATER` 回调到 `0.28 ~ 0.32`——曝光仍能过 `+50%` 门槛，但不会把 156 的分母吹到 200+。
- **必须同时** 让 `preferredTerrainCombatCount > 0`（见 P0-2）。
- **可选补强** greenwood_fringe 怪物池：从 `monsterPools` 中增加至少一个会丢寒气/冷冻技能的原版怪（或直接让 `bandit.trapper` 带上触发 `terrain_cold_water_freeze` 的投掷技能），让 Path B 的分子有实际来源。**不**允许新增 `TerrainTag`，仅复用已有规则。

---

### P0-2 · `preferredTerrainCombatCount = 0` —— 整个 §4.3.1 正式化特性运行时完全 inert

**事实**（来自 `whitebox-terrain-summary.json` corpusAggregateMetrics）：

```json
"preferredTerrainCaseCount": 1,
"preferredTerrainImplementedCount": 1,
"preferredTerrainCombatCount": 0,
"preferredTerrainImplementedCombatCount": 0,
"preferredTerrainCombatImplementationRate": 0.0,
"preferredTerrainTagsSeen": ["WATER"],
"terrainCoverageByZone": {
  "greenwood_fringe":    { "preferredTerrainCombatCount": 0, "observedPreferredTerrainTags": [] },
  "deep_iron_pit":       { "preferredTerrainCombatCount": 0, "observedPreferredTerrainTags": [] },
  "underground_river":   { "preferredTerrainCombatCount": 0, "observedPreferredTerrainTags": [] },
  "crystal_cavern":      { "preferredTerrainCombatCount": 0, "observedPreferredTerrainTags": [] }
}
```

所有 4 个 zone 的 `preferredTerrainCombatCount` 均为 0。`preferredTerrainCaseCount=1` 只是 white-box 合成的 1 条单元用例在跑，而**真实 combat corpus 中一次都没发生**"带 preferredTerrainTags 的精英与任一怪交战"。

这意味着合同 §4.3.1 要求一次到位的 `EliteMutationDef.preferredTerrainTags` 字段：
- 在 YAML 层面写全了（12 个 mutation 都显式声明，空表用 `[]` 而非 null） ✅
- 在 `MutationModels.kt` 层面有 `terrainAffinityWeightBonus` 加分 ✅
- 在 `EncounterDecorationService` 层面向下游聚合 ✅
- 在 `TerrainAwarePlacement` 层面做了"直接命中 > 相邻命中 > 回退"打分 ✅

**但是运行时没有任何一场战斗里的任一攻击方/防御方真的挂着 preferredTerrainTags**。整条链路架构上正确，行为上 0 生效。

**根因（已经用代码验证）**：

调用链如下：

```
GameModule.populateZone
  └─ selectMonsterTemplates → bandit.sentry / bandit.wild_huntmaster  (greenwood_fringe.elitePools)
      └─ encounterDecorationService.selectDecoration(
             forceEliteMutationEligibility = (template.id in zone.elitePools && !isElite))
          └─ eliteMutationRegistry.select(
                 MutationSelectionContext(applyToTags = request.template.tags.toSet(), …))
              └─ 筛选：candidates = all().filter {
                     …
                     context.applyToTags.any(definition.applyToTags::contains)
                 }
```

关键是最后这行 `context.applyToTags.any(definition.applyToTags::contains)`。

对照 `game/src/main/resources/data/monsters/index.yaml`：

```yaml
- id: bandit.sentry
  tags: [monster, bandit]   # 只有这两个 tag
```

对照 `game/src/main/resources/data/elites/index.yaml`：

```yaml
- id: elite.frostbound
  applyToTags: [elite, river, crystal]
  minFloor: 1
  allowedZones: [greenwood_fringe, underground_river, crystal_cavern]
  preferredTerrainTags: [WATER, ICE]

- id: elite.tidebound
  applyToTags: [elite, river, crystal]
  minFloor: 1
  allowedZones: [greenwood_fringe, underground_river, crystal_cavern]
  preferredTerrainTags: [WATER]
```

`bandit.sentry.tags = {monster, bandit}` 和 `elite.frostbound.applyToTags = {elite, river, crystal}` **交集为空**。所以 `registry.select()` 直接返回空集，`EncounterDecoration.mutations = []`，`preferredTerrainTags = ∅`，`EliteMutationLoadout` 组件从未被挂上，`FoundationGameSession.entityPreferredTerrainTags(...)` 读到的永远是空集。

换句话说：
- `forceEliteMutationEligibility` 只决定"要不要尝试挑 mutation"，但下游 `applyToTags` 过滤器**不会**因此把 `elite` tag 注入到上下文里
- 想让 greenwood_fringe 的 `bandit.sentry` 拿到 frostbound / tidebound，**必须** 要么在过滤前让 bandit 带上 `elite` tag，要么让 mutation 的 `applyToTags` 覆盖 bandit 原始 tag
- 两条路当前都没做，所以 preferredTerrainTags 从未成为运行时事实

**这同时解释了 P0-1**：Path B 的设计意图是"通过 terrain-affinity 精英去拉高 encounter"，而 Path B 的输入前提（精英身上有 preferredTerrainTags）从来没被点亮，Path B 动作结构性无效。

**修复方向（二选一，对 §4.3.1 语义保真度最高的是路线 A）**：

**路线 A（推荐，显式注入 elite 上下文）**：在 `EncounterDecorationService.selectDecoration` 里，当 `forceEliteMutationEligibility=true || isEliteTemplate(...)` 时，把上下文的 `applyToTags` 扩展为 `template.tags + "elite"`。即：

```kotlin
applyToTags =
    if (request.forceEliteMutationEligibility || isEliteTemplate(request.template))
        request.template.tags.toSet() + "elite"
    else
        request.template.tags.toSet()
```

这一改对 §4.3.1 要求的「避免靠 tag/name 猜 affinity」**没有**违反——`elite` 是显式状态，`applyToTags` 是 mutation 数据里显式声明的。改完后：
- `bandit.sentry` (tags `[monster, bandit]`) 在 greenwood_fringe 被 `forceEliteMutationEligibility=true` 推进来，上下文变成 `{monster, bandit, elite}`
- `elite.frostbound.applyToTags = {elite, river, crystal}` ⋂ 上下文 = `{elite}` → 通过过滤
- `terrainAffinityWeightBonus` 额外 +4 权重生效
- `EncounterDecoration.preferredTerrainTags` = `{WATER, ICE}`（frostbound）或 `{WATER}`（tidebound）
- `TerrainAwarePlacement.chooseTerrainAwareRoomPlacement` 首次得到非空的 `preferredTerrainTags`，从而真正按"直接命中 > 相邻命中 > 回退"去挑房间
- 下一次跑 `terrainInteractionBatch`，`preferredTerrainCombatCount` 在 greenwood_fringe/underground_river/crystal_cavern 应立即非零

**路线 B（只 patch YAML，不改代码）**：扩大 frostbound/tidebound/corrosion_cloud 等的 `applyToTags`，加入 `bandit`、`beast`、`orc`、`goblin` 等真实低楼层怪的 tag。

- 缺点：mutation 数据会与具体怪种绑定，将来换怪池就又丢失 affinity，违反合同里"避免隐式路径"的精神。
- 除非路线 A 有不可接受的回归风险，否则不建议走 B。

任何一条路完成后都必须复跑 `terrainInteractionBatch` 验证 `preferredTerrainCombatCount > 0`，否则 §4.3.1 在事实层面仍是 dead code。

---

### P0-3 · 门禁开关脱钩：`failedTaskCount = 0` 但 encounter 度量已 FAIL

**事实**（`tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt:159`）：

```kotlin
val failedTaskCount = taskReports.count { task -> task.status == "FAIL" }
```

而 `terrainInteractionBatch` 这个 task 的 `status` 来自 `Phase4ReportRunner.kt:368`：

```kotlin
status = if (failedAssertions == 0) "PASS" else "FAIL",
```

也就是说：task 的 PASS/FAIL 只读 white-box assertion 的失败数，**完全不受** `buildExperienceMetrics` 里 `verdictOf(terrainEncounterRate >= terrainEncounterBaseline.targetRate)` 的裁决影响。

运行结果也验证了这一点：

```
- taskCount: `12`
- passedTaskCount: `12`
- failedTaskCount: `0`
...
| terrainInteractionEncounterRate | 12.1%, delta=-4.00% | ... | >= 16.36% | FAIL |
### terrainInteractionBatch - PASS
```

一个度量 FAIL，但聚合数字仍然 `12/12 PASS`。合同 §8 第 5 条写明：

> `phase4Report` 对两个 terrain 指标必须显式展示：baseline path / metric definition version / 当前值 / baseline 值 / 相对提升 / target rate / `decisionPathByCurrentMetrics`

`phase4Report` 确实显示了这些字段（provenance 合同项 PASS），**但**合同 §8 同时把这两条度量列为"出口门禁"——意图是出口门禁不过就挡 CI。现在 CI 上即使 encounter FAIL，也不会被 `failedTaskCount != 0` 挡下，`./gradlew phase4Report` 的 exit code 会继续是 0，开发者不会看到红色信号，合同 §8 的效力被架空。

**修复方向**：

在 `Phase4ReportRunner.run()` 生成 aggregate 之后，新增一段 "experience metric gate"：

```kotlin
val failedExperienceMetrics =
    aggregate.experienceMetrics.count { metric -> metric.status == "FAIL" }
val failedTaskCount =
    taskReports.count { task -> task.status == "FAIL" } + failedExperienceMetrics
```

或者更稳妥地：在 `Phase4AggregateReport` 增加 `failedExperienceMetricCount` 字段并计入 `Phase4ReportRun.failedTaskCount`。两种写法都需要同步更新 `Phase4ReportRunnerTest`。

这条修复独立于 P0-1/P0-2：即便 encounter 真的达标了，这个脱钩本身仍然是下一个 PR 的隐患，因为任何体验度量 FAIL 都不会阻塞 CI。**P0-3 必须单独 commit 一次**，否则无法保证 P0-1/P0-2 修完后能被 CI 正确拦截重现问题。

---

## 2. P1 问题（合入前至少有书面回应）

### P1-1 · `terrainTaggedCombatExposureRate` 刀口边际 0.48pp

- 绝对值 34.0% vs 合同目标 33.89% → 余量 0.11 个绝对百分点，换算成相对增幅是 50.48% vs 50.00%。
- 任何一个种子变动、任何一次 candidatePoints 洗牌、任何一次被 P0-1 修正触发的 density 回调，都可能让曝光 PASS 翻 FAIL。
- P0-1 的推荐动作（把 `TERRAIN_DENSITY_SCALE` 回调到 0.32~0.40、同时 WATER 回调）将**让曝光也下降**。需要在调参前先做一次 grid 扫描，确保 `exposureRate ∈ [0.34, 0.42]` 稳定，不让 P0-1 的修复把 P1-1 直接掉出门槛。
- 建议做法：用 `terrainInteractionBatch` 的多 seed 基础上跑一次 ±10% 的 density sweep，记录 exposure/encounter 双指标的 (min, median, max)，并在最终合入 PR 的描述里粘贴该表作为鲁棒性证据。

### P1-2 · greenwood_fringe encounter rate = 5.1% 是唯一离群 zone

- 其他三个 zone：deep_iron_pit 27.3%，underground_river 27.1%，crystal_cavern 20.0%，均显著高于合同目标 16.36%。
- 一旦去掉 greenwood_fringe 的贡献：`(29 − 8) / (240 − 156) = 21 / 84 ≈ 25.0%`，是能过门槛的。
- 这说明 Path B 的根本问题局限在 greenwood_fringe 的怪物池——低楼层怪打不出冷/火/雷，即便被塞进水面也只有 `terrain_physical_ice_slip`（而且 greenwood 主要是 WATER 不是 ICE）能依赖。
- 合入前至少在本报告基础上决策：是"降曝光（P0-1 路线）"还是"改 greenwood 怪池（需要额外工作但更健康）"。两种都不引入新 TerrainTag，都合规。

### P1-3 · `bossHarness` / `boss-harness-v4` 与 preferredTerrain 的联动只是 gate，不是证据

- `BossHarnessTest.kt` 新加的 `boss.aggregate.preferred_terrain_applied` 是 `(!terrainPreferenceAvailable || terrainPreferenceImplemented)`。形式上无论是否有 preferred terrain 都能 PASS——"可用即必须实现，不可用不追究"。
- 现在 `preferredTerrainCaseCount` 在 corpus 里 = 1（合成用例），boss harness 是没有做 preferredTerrain-aware boss 的。这意味着 boss-harness-v4 的门禁在 boss 层实际上是空转。
- 这条 **不阻塞合入**，但应该在 PR 描述里明确：`bossHarness` 的 preferredTerrain 断言在本 PR 中**没有任何 boss 触发过**，当前只在 `terrainInteractionBatch` 的 1 条合成用例上验证。P0-2 修好后要同步拉一次 boss 的验证，或者在下一 PR 里补一个 preferredTerrain-aware boss 用例，否则门禁永远是 vacuous truth。

---

## 3. P2 问题（后续清债）

### P2-1 · `TERRAIN_DENSITY_SCALE` 语义无声翻转

`HybridTopologyPipeline.kt:916` 和 `MapgenInternals.kt:175` 现在都是：

```kotlin
val targetCount =
    max(
        1,
        min(candidatePoints.size, (candidatePoints.size * weight * TERRAIN_DENSITY_SCALE).roundToInt()),
    )
```

历史版本是 `(candidatePoints.size * (weight / totalWeight) * 0.14f).roundToInt()`。语义变化：

- 旧语义：YAML 里的 weight 是「相对份额」，多个 tag 在一个 14% 左右的预算里按比例瓜分，每个 tile 最多被一个 tag 覆盖。
- 新语义：YAML 里的 weight 是「绝对密度比例」，乘以 `0.64f` 后的结果就是"候选点里大约这个比例被染色"，**多个 tag 独立覆盖，可以叠在同一个 tile 上，也可以合计超过 100% 候选点**（会退化为重复随机 sample）。

这是一个**隐式破坏兼容性的改动**，将来任何人看 `greenwood_fringe WATER: 0.44` 都会以为是"相对权重 0.44 份"，但实际上是"大约 28% 候选点被涂上 WATER"（0.44 × 0.64）。更糟的是：

- 如果某个 zone 写 `{WATER: 0.8, ICE: 0.8}`，新语义会让 target count 分别达到 `51.2% × candidatePoints`，两者都会触顶到 `min(candidatePoints.size, …)`，出现水冰完全重叠、tile 大量带双 tag 的场面——`terrain_fire_ice_melt` 等规则的触发概率会因此被放大得很奇怪，跟原意不符。

**修复方向**（P2 不阻塞本 PR，但必须进入下一 PR 的债务表）：

- 在 `HybridTopologyPipeline` 和 `MapgenInternals` 各自 const 附近加一行文档注释，说明"weight 为每 tag 独立的候选点比例，范围 [0,1]"。
- 在 `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md` 或新加的 `docs/mapgen/terrain-density-semantics.md` 里把新语义写死。
- 在 `ZoneMapgenProfile` 数据加载时对 `terrainTagWeights` 每项加 `require(value in 0f..1f)` 的 sanity（目前 loader 没有这条 sanity，会无声接受 > 1 的数值）。
- 跟上单元测试：`MapgenInternalsTest` 新增一条"同房间 WATER=1, ICE=1 两 tag 独立覆盖互不冲突"的断言，把新语义锁死。

### P2-2 · `MutationModels.kt` 里 `terrainAffinityWeightBonus` 的 +4 权重是 magic number

- `terrainAffinityWeightBonus` 无条件返回 `4`，而 `tierWeight` 最小为 `2`（MINOR at floor 4）、最大为 `6`（MINOR at floor 1）、MAJOR 范围 `3~7`、SIGNATURE 范围 `0~4`。
- `+4` 的意思是"我比平均 MINOR 还高，和 MAJOR 持平"。这个强度在 allowedZones 命中时几乎会必选 preferred 精英。
- 不是错，只是魔数无文档。建议在 `preferredTerrainTags` 的字段注释里写明："如本字段非空且 context.zoneId 命中 allowedZones，在选择权重上加 4（与 MAJOR tier 的 floor-1 平均相当）。"
- 这条也是 P0-2 修复后才有讨论的意义——当前 bonus 0 生效，调它没意义。

### P2-3 · `TerrainAwarePlacement` 只做了 room-level 和 point-level 的偏好命中，没做"偏好失败时的 fallback 可观测性"

- 当前 `chooseTerrainAwareRoomPlacement` 的 fallback 路径（direct 空 → adjacent 空 → 任意点）会静默使用，没有 metric 跑到 `preferredTerrainCombatImplementationRate` 相关的计数器里区分"因为房间里没 WATER 所以 fallback 走了普通点"和"根本没 preferred tags"。
- 这会让未来诊断"为什么 preferredTerrainCombatCount 又低了"变得非常困难——你不知道是 P0-2 类问题（根本没下发 preferred tags）还是 zone painting 问题（下发了但没房间能接）。
- 建议在 `TerrainPreferenceMatch` 里增加三路计数：`directMatchCount / adjacentMatchCount / fallbackCount`，让 `Phase4ReportRunner` 能把这三个数字作为 `terrainInteractionEncounterRate` 的 note 输出。不影响门禁，只是诊断可观测性。

---

## 4. 合同符合项（保留，作为正向证据）

以下条目与合同和 baseline 一致，已经做对，不需要动：

- **§4.3.1 正式化规则**
  - 字段落在 `EliteMutationDef`（`MutationModels.kt`），不在生成器私有配置 ✅
  - 类型 `preferredTerrainTags: List<TerrainTag> = emptyList()` ✅
  - `game/src/main/resources/data/elites/index.yaml` 12 个 mutation 全部显式声明，空表写 `[]`（frostbound=[WATER,ICE]、tidebound=[WATER]、emberblood/corrosion_cloud=[OIL]、其余 8 个 `[]`）✅
  - `DataLoader.explicitStringList("preferredTerrainTags")` 强制 key 必须存在，缺失就抛 ✅
  - loader / bossHarness / terrainInteractionBatch 都能读出并上报该字段 ✅
- **§4.3.2 禁止项**
  - `EncounterDecorationService` / `GameModule` 没有任何 "if mutation.id == foo" 或 "if 'bandit' in name" 的猜测分支 ✅
  - 只有一条显式字段路径，没有第二套隐式推断表 ✅
  - client / tools 侧没有 maintain 第二份 affinity 表 ✅
- **合同出口门禁 §8**
  - §8.1 exposure ≥+50%：Δ=+50.48%，PASS（但刀口，见 P1-1）
  - §8.2 encounter ≥+30%：**FAIL**（见 P0-1）
  - §8.3 solvabilityHarness 主线可达 100%：`1000/1000 criticalPathFailureCount=0`，PASS
  - §8.4 不新增 TerrainTag：仅 WATER/ICE/OIL，PASS
  - §8.5 phase4Report 对 terrain 两指标显式展示 baseline path / version / current / baseline / delta / targetRate / decisionPathByCurrentMetrics：全字段齐全，PASS
  - §8.6 client-visible assets：本 PR 没走 Path C，无 goldenScreenshot 需求，N/A

- **baseline provenance 强制**
  - `Phase4ReportRunner` 读取 `docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json`，版本字段 `phase4-terrain-v2` 与 runtime corpus 的 `terrainMetricDefinitionVersion` 做 `require` 断言（运行时不匹配直接抛），符合合同 §1 "若 metric definition version 变化，必须先更新 baseline artifact" ✅
  - encounter 公式归一化为 `triggeredInteractionCombatCount / taggedCombatCount`，与 baseline JSON 的 `normalizedFormula` 一致 ✅

---

## 5. 修复计划（建议按顺序执行，不得跳步）

### 第 1 步 · 单独落 P0-3（先把门禁接回来）

1. 修改 `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`，把 `failedTaskCount` 的计算扩展为 `taskFailures + experienceMetricFailures`，或新增 `failedExperienceMetricCount` 字段并让 gradle task 在该字段 > 0 时也返回非零 exit code。
2. 更新 `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4ReportRunnerTest.kt`，加用例：「存在一条 experienceMetric status=FAIL 时聚合 failedTaskCount > 0」。
3. 本地跑 `./gradlew phase4Report`，验证当前跑完**应返回非零 exit code**（因为 P0-1 仍未修）。CI 会红——这正是我们想要的状态，直到 P0-1/P0-2 完成。

**为什么第 1 步必须先做**：如果 P0-1/P0-2 先修但门禁没接回来，下一个 PR 再回归时仍然不会被 CI 挡下。这条是结构性的质量栅栏，必须先立起来。

### 第 2 步 · 落 P0-2（让 preferredTerrainTags 在运行时被拉起来）

1. 首选路线 A：在 `EncounterDecorationService.selectDecoration` 里，当 `forceEliteMutationEligibility || isEliteTemplate(...)` 时，上下文 `applyToTags` 增加常量 `"elite"`。
2. 加单元测试：
   - `EncounterDecorationServiceTest`："`bandit.sentry` 在 `greenwood_fringe` + forceEliteMutationEligibility=true 应能拿到 `frostbound` 或 `tidebound`，`decoration.preferredTerrainTags` 非空。"
   - `EliteMutationRegistryTest`："`applyToTags={monster, bandit, elite}` 上下文对 frostbound 通过筛选。"
3. 跑 `./gradlew terrainInteractionBatch`，期望：
   - `preferredTerrainCombatCount > 0`
   - `preferredTerrainTagsSeen` 至少包含 `WATER, ICE`
   - `greenwood_fringe.observedPreferredTerrainTags` 不再为空

### 第 3 步 · 落 P0-1（把 encounter 拉上目标线）

1. **先验证"只改 P0-2 够不够"**：跑一次 `terrainInteractionBatch`，如果 preferredTerrain 精英已经能让 greenwood_fringe 的 encounter 从 5.1% 拉到 16.4% 以上，则可能不需要回调 density。
2. 如果仍然不够：
   - 调 `HybridTopologyPipeline.TERRAIN_DENSITY_SCALE` 与 `MapgenInternals.TERRAIN_DENSITY_SCALE` 同步降到 `0.40f` 左右（和 WATER 0.44 相乘 ≈ 0.176，每房间约 17~18% 候选点——仍比历史 14% 略高）。
   - 或者把 `game/src/main/resources/data/mapgen/zones/index.yaml` 里 greenwood_fringe `WATER: 0.44 → 0.32`。
3. 做 P1-1 的 density sweep，确保 exposure `∈ [0.34, 0.42]` 稳定过门禁。
4. 重跑全套：`mapgenSmoke → solvabilityHarness → terrainInteractionBatch → bossHarness → whiteBoxMapgen → whiteBoxVerify → phase4Report`。
5. 验收：`phase4-summary.md` 顶部 `failedTaskCount: 0`，体验度量表里两条 terrain 都 PASS。

### 第 4 步（可选）· 清 P1-3 / P2-1

- `BossHarnessTest` 的 preferredTerrain 断言写在 PR 描述里作为"当前仍 vacuous"的已知限制，或者补一条 preferredTerrain-aware boss 用例。
- 在 `HybridTopologyPipeline.kt` 和 `MapgenInternals.kt` 两处 const 附近补一行文档注释，把新语义写死，同时在 PR 描述 的 "Known behavior change" 一节里说明。

---

## 6. 证据索引（审查人可直接跳读）

| 证据 | 文件 / 行 |
| --- | --- |
| Encounter FAIL 原始表格 | `tools/build/reports/phase4/phase4-summary.md:27` |
| 四个 zone 分细指标 | `tools/build/reports/phase4/phase4-summary.md:279-353` (JSON 段) |
| `preferredTerrainCombatCount: 0` | 同上，所有 zone 字段 |
| `failedTaskCount` 计算逻辑 | `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt:159` |
| terrainInteractionBatch task 状态判定 | `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt:368` |
| experience metric verdict | `Phase4ReportRunner.kt:831` (exposure)、`:857` (encounter) |
| mutation selection filter | `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt:200-206` |
| terrainAffinityWeightBonus +4 | `MutationModels.kt:257-267` |
| forceEliteMutationEligibility 流向 | `game/src/main/kotlin/com/ktome/game/GameModule.kt:600-614` |
| Decoration 向下聚合 preferredTerrainTags | `EncounterDecorationService.kt:80-85` |
| applyToTags 限定窗口 | `EncounterDecorationService.kt:72` |
| terrain density scale（新语义） | `HybridTopologyPipeline.kt:521, 913-917` / `MapgenInternals.kt:119, 170-180` |
| zone WATER 翻倍 | `game/src/main/resources/data/mapgen/zones/index.yaml:6` (greenwood_fringe WATER: 0.44) |
| frostbound / tidebound 数据 | `game/src/main/resources/data/elites/index.yaml:168-195` |
| greenwood_fringe 怪物池（物理 bandit/beast/undead） | `game/src/main/resources/data/zones/index.yaml:42-43` |
| bandit.sentry tags = [monster, bandit] | `game/src/main/resources/data/monsters/index.yaml:55` |
| OPT PR-01 baseline artifact | `docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json` |

---

## 7. 合入决策

**当前状态：NOT SHIPPABLE。**

给出 3 个 P0 修复（P0-3 → P0-2 → P0-1 的顺序）后再走一次 post-dev review。合入前须提供下列 4 条附录作为证据，缺一不可：

1. `phase4-summary.md` 截图 / 复制片段，证明 `failedTaskCount: 0` 且两条 terrain 度量均 PASS。
2. `whitebox-terrain-summary.json` 的 `preferredTerrainCombatCount` 字段 > 0，至少在 `greenwood_fringe` 和另一个 zone 里 > 0。
3. 一次 density sweep 的 (exposure, encounter) 表格，证明修复后的参数对 seed 扰动是稳定的（不再是刀口 PASS）。
4. PR 描述里列出 P2-1 作为"已知语义变更"的 known behavior change 条目，避免下一个接手的人踩同样的坑。

---

*Reviewed by: senior roguelike/ToME dev director + systems planner director + gameplay review lead (proxy role), 2026-04-11, against commit baseline `a91294fd` + working tree on `codex/phase4-opt-pr06-terrain-uptake-tuning`.*
