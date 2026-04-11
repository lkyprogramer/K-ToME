# Phase 4 审查反馈核实与优化 PR 计划

**日期**: 2026-04-08  
**目标**: 核实 `docs/review/phase4/phase4_opt_deep_review_claude_v4_part1~4.md` 中的问题判断，并把确实成立的问题整理为一份有先后顺序、可直接继续拆分的 PR 级优化方案。  
**定位**: 这是 `Phase 4` 完成态之后、进入 `Phase 5` 之前的 review follow-up 计划，不替代原始 `PR-01 ~ PR-09` 设计文档。

---

## 1. 核实结论

### 1.1 已核实成立的问题

| 审查问题 | 结论 | 证据 | 计划含义 |
| --- | --- | --- | --- |
| 精英突变数量不足 | **成立** | `docs/2026-03-13-phase2-to-phase5-final-roadmap.md:426-431` 把 `elite mutation package` 的最小可发布集写为 `12 套左右`；当前 `game/src/main/resources/data/elites/index.yaml` 只有 `6` 个 `eliteMutations` | 这是 `P4-B` 的真实内容缺口，必须进入第一优先级修复队列 |
| affix / unique / artifact 总量不足 | **成立，但原报告表述需要纠偏** | 路线图写的是 `affix / unique / artifact = 120 条左右总量`，不是“affix 单独 120+”；当前数据为 `40 affix + 12 unique + 4 artifact = 56`，仍明显低于目标 | 真实问题是“装备内容密度不足”，不是“affix 单独没到 120” |
| loot profile 终端掉落差异偏弱 | **成立** | 当前 `LootProfileSchemaV2` 是 `itemIds + rewardBudget` 模型：`game/.../SchemaModels.kt:128-140`；runtime 也是先从 `profile.itemIds` 选 base item，再走预算：`game/.../FoundationGameSession.kt:9885-9921` | 这不是单纯补数据能解决的问题，而是要决定是否升级 loot profile contract |
| hidden content 触发与入口过于同质 | **成立** | `game/src/main/resources/data/events/index.yaml` 只有 `4 PERCEPTION_REVEAL + 4 INTERACT_TILE`；`game/src/main/resources/data/secret-zones/index.yaml` 的 `4` 个 secret zone 全是 `PERCEPTION_CHECK`，且 `entranceBindingId` 全为 `optional.branch.1` | 这是 `PR-07` 内容层面的明显重复，需要单独的内容与验证 PR |
| unique / artifact 记忆点不足 | **成立，但原报告表述需要纠偏** | 当前模板本身只声明 `fixedAffixIds / fixedMaterialId`，见 `game/src/main/resources/data/items/index.yaml:710-912`；模板没有独立 mechanic contract | 真问题是“模板缺少专属效果层”，不是“系统完全没有非数值效果” |

### 1.2 部分成立的问题

| 审查问题 | 结论 | 证据 | 计划含义 |
| --- | --- | --- | --- |
| affix 全是纯数值增强 | **部分成立** | 当前 affix 总量是 `40`，其中至少 `7` 个带 `passive`；已有 `DamageVsStatus / DamageTypeBonus / ResistanceBonus / HpRegenPerTurn` 等被动，见 `game/src/main/resources/data/items/index.yaml` | 不能按“完全没有机制词缀”来规划；应表述为“机制词缀数量太少、被动家族太窄、无法支撑 build 分化” |
| Boss 变体过少且缺差异 | **部分成立** | 当前 `bossVariants` 的确只有 `3` 个：`game/src/main/resources/data/boss-variants/index.yaml`；但 Phase 4 权威文档没有写死数量下限 | 它是重要体验问题，但不是像精英突变那样的硬缺口，优先级应低于 mutation / affix / loot / hidden |
| 地形交互体验几乎不可感知 | **趋势上成立，但当前证据不充分** | 自动化与 white-box 当前证明的是“规则正确”和“来源可追溯”：`tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json`；`phase4Report` 也没有“实战触发率/战斗暴露率/玩家利用率”字段 | 这类问题不能只靠主观判断继续扩大实现，必须先补体验度量，再做内容和摆放调优 |

### 1.3 已过时或已被当前实现修复的问题

| 审查问题 | 结论 | 证据 | 计划含义 |
| --- | --- | --- | --- |
| sample-pack golden 仍是 synthetic snapshot | **已过时** | 当前 `client/.../GoldenScreenshotHarnessTest.kt` 已改为 `captureSamplePackRuntimeHash()`，真实启动 `GameModule.newFoundationSession(...)`，走 `claimSamplePackReward(session)` 进入 `underground_river` secret route，再用 live `renderSnapshot()` 渲染 | 不纳入后续优化 PR；后续只保留回归，不重复开返工项 |
| dual-pack precedence fixture 缺失 | **已过时** | `tools/src/main/resources/fixtures/content-packs/sample.flooded_relics.yaml` 已声明 `dualPackScenarios`；`fixture.sample_flooded_relics_override` 与 `ContentPackHarnessRunner` 也已接线 | 不纳入后续优化 PR |

### 1.4 额外结论

当前 `Phase 4` 的自动化和白盒体系偏向“正确性门禁”，并不直接度量“体验差异是否成立”。这不是某个单点 bug，而是 review 计划必须优先处理的结构性问题。  
证据：

1. `phase4Report` 当前聚合 `mapgen / solvability / loot / hidden / terrain / content-pack`，但没有：
   - mutation 覆盖与组合熵
   - loot profile 候选池重叠率
   - hidden trigger type 覆盖
   - terrain 实战暴露率 / 实际触发率
   - unique/artifact 的非 trivial 替换决策率
2. `hiddenContentHarness` 当前是“按 formal path 主动搜索”的验证，并不等同于真实玩家在 run 中自然发现的差异性指标。

因此，后续优化计划必须先把“体验问题”变成可度量问题，再开始改内容。

### 1.5 当前数据盘点基线

为后续各 PR 的具体改造提供精确数据参照：

| 维度 | 当前值 | 路线图目标 | 缺口 |
| --- | --- | --- | --- |
| 精英突变数 | 6 (1 MINOR + 4 MAJOR + 1 SIGNATURE) | ≈12 | 6 |
| Affix 总数 | 40 (22 PREFIX + 18 SUFFIX) | — | — |
| Unique 模板数 | 12 | ≥12 | 已达最低线，但无独特效果 |
| Artifact 模板数 | 4 | ≥4 | 已达最低线，但无独特效果 |
| **Affix + Unique + Artifact 总量** | **56** | **≈120** | **64** |
| Hidden event 触发类型使用 | 2/6 (PERCEPTION_REVEAL + INTERACT_TILE) | ≥4/6 | 2 |
| Secret zone entranceBindingId 种类 | 1 (全部 optional.branch.1) | ≥3 | 2 |
| Loot profile 总数 | 21 | — | 当前正式 schema 为 `itemIds + rewardBudget` |
| Loot profile distinct base item 数 | 25 | — | 21 个 profile 的候选池高度重叠 |
| EquipmentPassive 家族（core 合同） | 5 (`DamageVsTag / DamageVsStatus / HpRegenPerTurn / DamageTypeBonus / ResistanceBonus`) | — | 缺少 on-hit / on-kill / terrain / 条件触发类 |
| Affix 直接使用的 passive kind | 2/5 (`DamageVsStatus`, `HpRegenPerTurn`) | ≥4/5（规划目标） | affix 侧机制词汇过窄 |
| Boss 变体数 | 3 | — | 无 phase 文档硬性下限 |
| 地形覆盖率 | WATER 最高 0.28, OIL 最高 0.24 | — | 无体验度量 |

**当前 affix cost 分布**：

| cost 值 | 成本带 | PREFIX 数 | SUFFIX 数 | 合计 |
| --- | --- | --- | --- | --- |
| 1 | TRIVIAL | 2 | 0 | 2 |
| 3 | MINOR | 3 | 2 | 5 |
| 6 | MEDIUM | 10 | 7 | 17 |
| 10 | MAJOR | 5 | 8 | 13 |
| 14 | SIGNATURE | 2 | 1 | 3 |
| **合计** | | **22** | **18** | **40** |

**当前 affix 直接使用的 passive kind 分布**：

| passive kind | affix 中使用数 | 说明 |
| --- | --- | --- |
| DamageVsStatus | 6 | 目前唯一成体系的 affix 级 conditional passive |
| HpRegenPerTurn | 1 | 仅少量 sustain affix 使用 |
| DamageVsTag | 0 | 目前主要存在于部分 unique/artifact 对应 item 定义 |
| DamageTypeBonus | 0 | 同上 |
| ResistanceBonus | 0 | 同上 |

> 注：
> 1. 这里的统计口径严格限定为 `affixes:` 段的直接 passive 使用，不把 unique/artifact 对应 item base def 的 passive 混进 affix 池基线。
> 2. 路线图里的 `≈120` 是 `affix + unique + artifact` 总量目标，不是 affix 单独目标。

---

## 2. 规划原则

1. **先补体验度量，再补内容密度**  
   当前 review 里最容易失真的是 terrain 与奖励体验；没有量化基线，后续改动很难判断是“变好”还是“换了一种主观感觉”。

2. **先耗尽现有合同，再谨慎抬升合同**  
   `Phase 4` 已冻结 `TerrainTag / LootBudget / ContentPackManifest / OverlayEntry` 等合同。优化应先在现有 contract 下补齐内容；只有当现有模型明显无法承载目标时，才新增最小合同扩展。

3. **先修影响 Phase 5 前置条件的内容**  
   精英突变深度、装备/掉落驱动和 hidden diversity 会直接影响 `Phase 5` 的战术 AI、平衡和 QA 收益，优先级高于“让现有内容更华丽”。

4. **避免把体验问题伪装成系统重写**  
   本计划不引入新的脚本 runtime、第二套 AI、第二套 loot 解释器，也不新增 `DamageType / TerrainTag / ResourceType`。

5. **所有优化 PR 都必须同步扩白盒与 phase4Report**  
   后续每个 PR 都要同时产出新的度量、summary 和可解释 artifact，不能只改内容文件。

---

## 3. 有先后顺序的 PR 级优化方案

以下采用 `OPT-PR-01 ~ OPT-PR-07` 编号，避免与原始 `PR-01 ~ PR-09` 混淆。若后续决定进入正式 Phase 4 follow-up，可再映射为主线 PR 编号。

### 3.0 PR 依赖关系图

```
OPT-PR-01 (体验度量基线)
    │
    ├── OPT-PR-02 (精英突变 + Boss 变体)
    │       │
    │       └───────────┐
    │                   │
    ├── OPT-PR-03 (装备机制词汇扩容)
    │       │           │
    │       ▼           │
    │   OPT-PR-04 (affix / unique / artifact 内容密度补齐)
    │       │           │
    │       ▼           ▼
    │   OPT-PR-05 (Loot Profile V3 + 奖励池差异化)
    │       │
    │       ▼
    │   OPT-PR-06 (Hidden Content 多样化)
    │       │
    │       ▼
    └── OPT-PR-07 (地形可感知性调优)
```

**关键依赖说明**：
1. `OPT-PR-01` 是所有后续 PR 的度量基础，必须第一个完成。
2. `OPT-PR-02` 和 `OPT-PR-03` 相互独立，可并行开发。
3. `OPT-PR-04` 依赖 `OPT-PR-03` 的 passive 词汇扩展，否则新增的 unique/artifact 仍然只能使用数值型效果。
4. `OPT-PR-05` 依赖 `OPT-PR-04` 的内容池扩充，否则新的 pool strategy 只是在浅池上做差异化。
5. `OPT-PR-06` 依赖 `OPT-PR-05` 的掉落池差异化，否则隐藏奖励还是同样的掉落。
6. `OPT-PR-07` 放在最后，因为 terrain 度量需要 `OPT-PR-01` 的基线，且调优需要 `OPT-PR-02` 的精英-地形绑定。

### OPT-PR-01：体验度量基线与报告补强

**目标**

把当前 review 中”感觉不够好玩”的判断，转换为可回归、可比较的结构化指标。

**为什么排第一**

1. terrain、hidden、loot 三类问题目前只有正确性证据，没有体验证据。
2. 后续任何内容扩容 PR，如果没有统一指标，都会重新回到主观争论。

**范围**

1. 扩展 `whiteBoxLoot / whiteBoxHiddenContent / terrain white-box / bossHarness / phase4Report` 的输出字段。
2. 新增以下体验指标（共 10 个）。
3. 为 `whiteBoxContentPack` / `phase4Report` 做一次 artifact freshness 对齐，避免 review 继续读到历史语义。

#### 3.1.1 新增指标详细定义

| 指标 ID | 所属域 | 计算公式 | 当前预期基线 | 改进后目标 | 落点文件 |
| --- | --- | --- | --- | --- | --- |
| `eliteMutationDistinctCount` | boss | `count(distinct mutation.id)` across data registry | 6 | ≥12 | `BossHarnessTest` white-box summary |
| `eliteMutationValidPairCount` | boss | `count(valid unordered mutation pairs)`，排除 `incompatibleWith`、双 `SIGNATURE`、zone/floor 不可同场成立的组合 | 待 `OPT-PR-01` 首次实测；当前总量仅 6，理论上上限偏低 | ≥40 | `BossHarnessTest` white-box summary |
| `lootProfileBaseItemOverlapMatrix` | loot | 对所有 cadence/secret/reward profile 两两计算 `|A ∩ B| / min(|A|, |B|)`，输出 N×N 矩阵 | 待 `OPT-PR-01` 首次实测 | <30% | `WhiteBoxLootRunner` |
| `lootProfileDistinctBaseItemCount` | loot | `count(distinct itemId)` across 所有 loot profile | 25 | ≥35 | `WhiteBoxLootRunner` |
| `affixPassiveCoverage` | loot | `count(distinct passive.kind in affix)` / `count(all EquipmentPassive subclasses)` | 2/5 = 40% (affix 只用 DamageVsStatus + HpRegenPerTurn) | ≥80% | `WhiteBoxLootRunner` |
| `hiddenTriggerTypeCoverage` | hidden | `count(distinct triggerType in events)` / 6 | 2/6 = 33% | ≥66% (4/6) | `WhiteBoxHiddenContentRunner` |
| `secretEntranceBindingCoverage` | hidden | `count(distinct entranceBindingId in secretZones)` | 1 (全部 optional.branch.1) | ≥3 | `WhiteBoxHiddenContentRunner` |
| `terrainTaggedCombatExposureRate` | terrain | `count(combats where at least 1 participant stands on tagged terrain)` / `count(all combats)` across 500 seeds | 未知（需首次度量） | ≥40% | `TerrainInteractionBatchTest` white-box summary |
| `terrainInteractionEncounterRate` | terrain | `count(combats where terrain rule actually triggers)` / `count(combats on tagged terrain)` | 未知 | ≥25% | `TerrainInteractionBatchTest` white-box summary |
| `uniqueArtifactMeaningfulSwapRate` | loot | 在 1000-roll batch 中，`count(unique/artifact whose passive differs from best-in-slot rare)` / `count(total unique/artifact rolls)` | 预估 ~0% (全为数值型，rare 也是数值型) | ≥50% (需 OPT-PR-03 完成后重测) | `WhiteBoxLootRunner` |

#### 3.1.2 具体改造清单

**文件 1**：`tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt`

- 在 `WhiteBoxLootCorpusCase` 或等价的 case 结构中，新增 `facts` 字段：
  - `baseItemOverlap: Map<String, Map<String, Double>>` — profile 两两重叠矩阵
  - `distinctBaseItemCount: Int`
  - `affixPassiveKindSet: Set<String>` — 实际使用的 passive kind 集合
  - `affixPassiveCoverageRatio: Double`
- 在 aggregate 报告中新增：
  - `lootProfileBaseItemOverlapMatrix` — 矩阵序列化为 JSON object
  - `lootProfileDistinctBaseItemCount`
  - `affixPassiveCoverage`
  - `uniqueArtifactMeaningfulSwapRate`
- 新增 assertion rule：
  - `loot.aggregate.overlap_below_threshold` — 断言平均重叠 < 0.50
  - `loot.aggregate.passive_coverage` — 断言 coverage ≥ 0.40

**文件 2**：`tools/src/main/kotlin/com/ktome/tools/hidden/WhiteBoxHiddenContentRunner.kt`

- 在 aggregate 报告中新增：
  - `hiddenTriggerTypeCoverage` — 从 event registry 直接计算
  - `secretEntranceBindingCoverage` — 从 secret zone registry 直接计算
  - `hiddenTriggerTypeSet: Set<String>` — 实际使用的 triggerType 集合
  - `secretBindingIdSet: Set<String>` — 实际使用的 entranceBindingId 集合
- 新增 assertion rule：
  - `hidden.aggregate.trigger_type_coverage` — 断言 ≥ 0.33 (2/6)
  - `hidden.aggregate.binding_coverage` — 断言 ≥ 1

**文件 3**：`game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt`

- 在 boss harness summary 中新增：
  - `eliteMutationDistinctCount`
  - `eliteMutationValidPairCount` — 计算 `valid unordered pairs`
  - `mutationTierDistribution: Map<String, Int>` — MINOR/MAJOR/SIGNATURE 分布
  - `bossVariantCount`
- 新增 assertion rule：
  - `boss.aggregate.mutation_count` — 断言 ≥ 6
  - `boss.aggregate.tier_balance` — 断言每个 tier 至少 1 个

**文件 4**：`game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt`

- 在 terrain white-box summary 中新增：
  - `terrainTaggedCombatExposureRate`
  - `terrainInteractionEncounterRate`
  - `terrainCoverageByZone: Map<String, Map<String, Double>>` — zone → tag → 覆盖率
- 新增 assertion rule：
  - `terrain.aggregate.exposure_rate` — 断言 ≥ 0.10 (当前预期较低)

**文件 5**：`tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`

- 在 `phase4-summary.json` 的 task metrics 中透传上述所有新增 aggregate 指标。
- 在 `phase4-summary.md` 的 markdown 报告中新增”体验度量基线”段落，一次性展示所有 10 个指标的当前值。

**文件 6**：runtime trace 补充（不改玩法行为）

- 若 `terrainTaggedCombatExposureRate` 无法从现有 `CombatResolutionTrace` 中提取，需在 `CombatPipeline` step 9 的 trace 输出中补充：
  - `terrainTagAtPosition: Map<Position, TerrainTag?>` — 参战实体所在格的 terrain tag
  - `terrainRuleTriggered: Boolean` — 本次战斗是否有 terrain rule 触发
- 补充位置：`core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt` 的 step 9 trace 写入点

#### 3.1.3 不涉及的修改

1. 不改任何数据文件（`items/index.yaml`、`elites/index.yaml` 等）。
2. 不改玩法行为——所有修改仅限于 tools 和 trace 层。
3. 不新增 Gradle task——复用现有 task，只扩展输出字段。

#### 3.1.4 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| terrain trace 补充改动 CombatPipeline 影响正确性 | 低 | 高 | 只在 trace/log 层追加字段，不改 step 9 的实际效果计算逻辑；补充后跑完整 `:core:test` |
| 新指标计算逻辑引入的 harness 运行时间增加 | 中 | 低 | 大部分指标是对 registry 的静态计算，不增加 seed 数量 |

**涉及模块**

1. `tools/src/main/kotlin/com/ktome/tools/phase4/`
2. `tools/src/main/kotlin/com/ktome/tools/loot/`
3. `tools/src/main/kotlin/com/ktome/tools/hidden/`
4. `tools/src/main/kotlin/com/ktome/tools/contentpack/`
5. `core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt` — 仅 trace 字段补充
6. `game/` 的 bossHarness runner

**验收**

```bash
./gradlew :core:test              # 确认 trace 补充不影响正确性
./gradlew whiteBoxLoot
./gradlew whiteBoxHiddenContent
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew whiteBoxVerify
./gradlew phase4Report
```

**量化验收标准**

1. `phase4Report` 的 `phase4-summary.json` 中包含全部 10 个新增 metrics 字段。
2. `phase4-summary.md` 中有”体验度量基线”段落，可读性好。
3. 所有 assertion rule 都运行且产出 PASS/FAIL（首次运行部分预期 FAIL，这是正常的——它们建立的是基线）。
4. `:core:test` 全部通过。

---

### OPT-PR-02：精英突变补齐与 Boss 变体差异拉开

**目标**

把 `elite mutation package` 从当前 `6` 套补到路线图要求的 `≈12 套`，同时拉开 Boss 变体的行为和可读性差异。

**为什么排第二**

1. 这是权威路线图里的硬缺口。
2. 它直接决定 `Phase 5` 的 AI 升级有没有足够的行为对象可用。

#### 3.2.1 新增 6 个精英突变设计

以下设计复用现有 `EliteMutationDef` 合同字段（`MutationKind` / `MutationTier` / `statModifiers` / `grantedTalents` / `aiProfileOverlay` / `auraStatusId` / `incompatibleWith`），但长期保留的 elite ability 不再直接复用玩家 talent id，而是通过 `grantedTalents` 挂载 dedicated elite-only talent。

| # | ID | Kind | Tier | threatCost | minFloor | allowedZones | 设计意图 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `elite.ironhide` | STAT_PACKAGE | MINOR | 2 | 1 | `[]` (全 zone) | 防御向 stat 包，与 `stonehide` 构成 STAT_PACKAGE 双选：`stonehide` 偏 HP，`ironhide` 偏 defense + 抗打断 |
| 2 | `elite.phase_runner` | AI_SHIFT | MINOR | 2 | 2 | `[]` | 脱战后传送到玩家背后；与 `hunt_protocol` 构成 AI_SHIFT 双选：`hunt_protocol` 是追猎型，`phase_runner` 是闪现型 |
| 3 | `elite.war_caller` | ABILITY_GRANT | MAJOR | 3 | 2 | `[]` | 低于 50% HP 时触发 dedicated elite-only 支援 talent；effect 语义可复用 `war_cry / rallying_banner` |
| 4 | `elite.corrosion_cloud` | AURA | MAJOR | 4 | 3 | `[deep_iron_pit, molten_core]` | ARMOR_BREAK 光环（radius: 1, duration: 2, magnitude: 0.10）；与 `dread_aura` 构成 AURA 双选 |
| 5 | `elite.frostbound` | ELEMENT_PACKAGE | MAJOR | 4 | 3 | `[underground_river, crystal_cavern]` | COLD 元素包（+accuracy, +resistance COLD, 通过 dedicated elite talent 复用 `frost_nova` 的 effect 语义）；填补 COLD 元素空缺 |
| 6 | `elite.void_mirror` | AURA | SIGNATURE | 5 | 4 | `[grey_gate_depths, abyssal_temple, abyssal_heart]` | 法系压制/防护型光环，优先复用现有防护类 status（如 `ARCANE_SHIELD_BUFF`）；不在本 PR 新增新的 `StatusEffectType` |

**补齐后的 tier 分布**：

| Tier | 当前 | 新增 | 合计 |
| --- | --- | --- | --- |
| MINOR | 1 (stonehide) | 2 (ironhide, phase_runner) | 3 |
| MAJOR | 4 (battle_drill, dread_aura, hunt_protocol, tidebound) | 3 (war_caller, corrosion_cloud, frostbound) | 7 |
| SIGNATURE | 1 (emberblood) | 1 (void_mirror) | 2 |
| **合计** | **6** | **6** | **12** |

#### 3.2.2 新增突变的互斥关系设计

```
既有互斥:
  battle_drill  ↔  dread_aura
  emberblood    ↔  tidebound

新增互斥:
  corrosion_cloud  ↔  dread_aura     # 不允许双 AURA
  frostbound       ↔  emberblood     # COLD 与 FIRE 互斥
  frostbound       ↔  tidebound      # 同族不叠加
  void_mirror      ↔  dread_aura     # 不允许双 AURA
  void_mirror      ↔  corrosion_cloud  # 不允许双 AURA
  war_caller       ↔  battle_drill   # 不允许双 ABILITY_GRANT

全局规则（Phase 4 已冻结）:
  - 同一 elite 最多 2 mutation
  - SIGNATURE + SIGNATURE 禁止
  - incompatibleWith 在注册表层 fail-fast
```

#### 3.2.3 新增突变的 stat modifier 与 talent 设计

```yaml
# stat modifiers (新增到 elites/index.yaml 的 statModifiers 段)
mod.mutation.ironhide:
  defense: +6
  maxHp: +8

mod.mutation.frostbound:
  accuracy: +3
  evasion: +2

# talent grants（复用 effect/status 语义，但不长期复用玩家 talent id）
elite.war_caller:
  grantedTalents: [elite_war_call] # dedicated elite talent
                                    # effect 语义可复用 war_cry / rallying_banner 的现有 EffectOp 组合

elite.frostbound:
  grantedTalents: [elite_frost_nova] # dedicated elite talent
                                      # effect/status 语义可复用现有 frost_nova

# aura definitions
elite.corrosion_cloud:
  auraStatusId: ARMOR_BREAK
  auraRadius: 1
  auraDuration: 2
  auraMagnitude: 0.10

elite.void_mirror:
  auraStatusId: ARCANE_SHIELD_BUFF
  auraRadius: 1
  auraDuration: 1
  auraMagnitude: 0.08
```

> **约束**：本 PR 默认不新增新的 `StatusEffectType`、不新增新的 boss action id。status/effect 语义可复用现有资产，但长期存在的 elite 行为不应继续直接绑定玩家 talent id。

#### 3.2.4 新增突变的 zone 分布矩阵

| zone | MINOR 候选 | MAJOR 候选 | SIGNATURE 候选 |
| --- | --- | --- | --- |
| greenwood_fringe | stonehide, ironhide, phase_runner | battle_drill, hunt_protocol, war_caller | — |
| deep_iron_pit | stonehide, ironhide, phase_runner | battle_drill, hunt_protocol, war_caller, corrosion_cloud | emberblood |
| underground_river | stonehide, ironhide, phase_runner | battle_drill, hunt_protocol, war_caller, tidebound, frostbound | — |
| grey_gate_depths | stonehide, ironhide, phase_runner | battle_drill, hunt_protocol, war_caller, dread_aura | void_mirror |
| abyssal_temple | stonehide, ironhide, phase_runner | battle_drill, hunt_protocol, war_caller, dread_aura, corrosion_cloud | emberblood, void_mirror |

> `allowedZones: []` 表示全 zone 可用；有 zone 限制的 mutation 只出现在指定 zone。

#### 3.2.5 Boss 变体差异拉开

在现有 3 个 Boss variant 的 `actionWeightProfile` 基础上，重新绑定 mutation 组合以利用新增 mutation：

| Boss Variant | 当前 mutations | 建议改为 | 权重调整方向 |
| --- | --- | --- | --- |
| `molten_glass` | stonehide + emberblood | ironhide + emberblood | 保持防御+火焰主题，但换成新的 MINOR 以增加差异 |
| `grey_crown` | dread_aura | dread_aura + war_caller | 增加支援/buff 行为，让 boss 战有"随从辅助"层 |
| `abyssal_eclipse` | emberblood + dread_aura | void_mirror + corrosion_cloud | 完全换成新的防护+减甲组合，强调法系压制的独特战术 |

同时扩大 `actionWeightProfile` 差异幅度，但**只允许在各 Boss 现有 action catalog 内重配权重**：
- `molten_glass`：提高已有近战压制/火焰爆发动作的权重，降低重复位移动作
- `grey_crown`：提高已有支援/增益类动作权重，使其更能放大 `war_caller`
- `abyssal_eclipse`：降低重复自保动作，提升已有 area denial / debuff 动作权重

#### 3.2.6 新增内容的 i18n / visual / audio 需求

| 新增 mutation | nameKey | iconKey | 需新增 visual | 需新增 audio |
| --- | --- | --- | --- | --- |
| ironhide | `mutation.ironhide.name` | `icon.mutation.ironhide` | 1 icon | 1 SFX |
| phase_runner | `mutation.phase_runner.name` | `icon.mutation.phase_runner` | 1 icon | 1 SFX |
| war_caller | `mutation.war_caller.name` | `icon.mutation.war_caller` | 1 icon | 1 SFX |
| corrosion_cloud | `mutation.corrosion_cloud.name` | `icon.mutation.corrosion_cloud` | 1 icon | 1 SFX |
| frostbound | `mutation.frostbound.name` | `icon.mutation.frostbound` | 1 icon | 1 SFX |
| void_mirror | `mutation.void_mirror.name` | `icon.mutation.void_mirror` | 1 icon | 1 SFX |

共计：6 组 mutation nameKey/descKey（zh-CN + en-US）、6 个 icon、6 个 SFX。
dedicated elite talent 默认复用 mutation 的视觉/音频/telegraph 资源，不单独再铺一套玩家向资源。

#### 3.2.7 具体改造文件清单

| 文件 | 改动 |
| --- | --- |
| `game/src/main/resources/data/elites/index.yaml` | 新增 6 条 `eliteMutations` 条目 + 2 条 `statModifiers` 条目 |
| `game/src/main/resources/data/boss-variants/index.yaml` | 修改 3 条 variant 的 `grantedMutations` 和 `actionWeightProfile` |
| `game/src/main/resources/data/talents/index.yaml` | 新增 dedicated elite-only talent（如 `elite_war_call`、`elite_frost_nova`） |
| `game/src/main/resources/i18n/zh-CN.json` | 新增 6 组 mutation name/desc key；elite-only talent 仅在需要独立可读描述时补最小文本 |
| `game/src/main/resources/i18n/en-US.json` | 同上 |
| `assets-src/image/specs/` | 新增 6 个 mutation icon 的生成计划；dedicated elite talent 默认复用 mutation 资源，不追加一套玩家向素材 |
| `assets-src/audio/specs/` | 新增 6 个 mutation SFX 的生成计划 |

#### 3.2.8 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| 设计误用当前不存在的 status / action id | 中 | 中 | 先以现有 registry 为准做白名单审计；默认只复用已存在的 status 与 talent/action |
| 新 mutation 的 `threatCost` 不平衡 | 高 | 中 | 完成后必须跑 `bossHarness` + 人工 playtest 至少 3 局 |
| 互斥关系导致某些 zone 有效候选太少 | 低 | 高 | 完成后用 `eliteMutationValidPairCount` 与按-zone pair 审计检查，确保每个 zone 至少 3 种有效组合 |

**验收**

```bash
./gradlew :game:test
./gradlew bossHarness
./gradlew terrainInteractionBatch
./gradlew whiteBoxVerify
./gradlew phase4Report
```

**量化验收标准**

1. `eliteMutationDistinctCount` ≥ 12。
2. `eliteMutationValidPairCount` ≥ 40。
3. `mutationTierDistribution` 中 MINOR ≥ 2, MAJOR ≥ 5, SIGNATURE ≥ 2。
4. 每个 target zone 至少有 5 种有效 mutation 候选。
5. 3 个 Boss variant 的 mutation 组合两两不同。
6. `:game:test` 全部通过。

---

### OPT-PR-03：装备机制词汇最小扩容

**目标**

在不引入第二套装备系统的前提下，给 affix / unique / artifact 提供足够的”非纯 stat”表达能力。

**为什么排在内容扩容之前**

当前真正的瓶颈不是”只缺更多 YAML”，而是当前 `EquipmentPassive` 词汇过窄，无法稳定承载 review 里要求的 build-driving 装备。

当前 `EquipmentPassive` sealed interface 只有 5 个子类（`DamageVsTag` / `DamageVsStatus` / `HpRegenPerTurn` / `DamageTypeBonus` / `ResistanceBonus`）。其中 affix 侧实际只覆盖 `DamageVsStatus` 与 `HpRegenPerTurn`，unique/artifact 虽有元素伤害/抗性方向，但整体仍缺少 on-hit、on-kill、自身状态条件、地形条件这类真正会改变 build 选择的词汇。

#### 3.3.1 新增 EquipmentPassive 子类设计

严格遵守”最小扩展”原则，只新增 4 个 passive 家族，全部走现有 `CombatPipeline` / `StatusEffect` / `ResourceType` 管线：

```kotlin
// core/src/main/kotlin/com/ktome/core/item/ItemModels.kt
// 在 sealed interface EquipmentPassive 中新增：

/** 攻击命中时概率施加状态效果 */
data class OnHitStatusProc(
    val statusId: String,        // 复用现有 StatusEffectType
    val chance: Double,          // 触发概率 0.0~1.0
    val duration: Int,           // 持续回合
    val magnitude: Double = 0.0, // 效果强度（部分 status 需要）
) : EquipmentPassive

/** 击杀时恢复资源 */
data class OnKillResourceRestore(
    val resourceType: String,    // 复用现有 ResourceType 枚举名
    val amount: Int,             // 恢复量（固定值，不是百分比）
) : EquipmentPassive

/** 条件触发的 stat 加成 */
data class ConditionalStatBonus(
    val condition: PassiveCondition,  // 见下方枚举
    val statModifier: StatModifier,   // 复用现有 StatModifier
) : EquipmentPassive

/** 地形亲和加成 */
data class TerrainAffinityBonus(
    val terrainTag: String,      // 复用现有 TerrainTag 枚举名
    val statModifier: StatModifier,
) : EquipmentPassive
```

```kotlin
// 新增枚举，严格限定条件种类
enum class PassiveCondition {
    HP_BELOW_50,       // HP < 50%
    HP_BELOW_30,       // HP < 30%
    HP_ABOVE_80,       // HP > 80%
    SELF_HAS_STATUS,   // 自身已有某个 status（仅在现有 combat state 可直接判断时启用）
}
```

#### 3.3.2 PassiveEffectResolver 改造

`core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt` 需要扩展以处理新的 4 种 passive：

```kotlin
// 在现有 when 分支中新增：

is EquipmentPassive.OnHitStatusProc -> {
    // 只在 DAMAGE_DEALT 事件中触发
    // 走 CombatPipeline 的 status application 管线
    // 概率判定使用 combatRng（确保 deterministic replay）
}

is EquipmentPassive.OnKillResourceRestore -> {
    // 只在 TARGET_KILLED 事件中触发
    // 走现有 ResourcePool.restore() 管线
}

is EquipmentPassive.ConditionalStatBonus -> {
    // 在 stat aggregation 阶段检查条件
    // 条件成立时将 statModifier 加入 effective stats
    // 条件检查走 combatState 不走 RNG
}

is EquipmentPassive.TerrainAffinityBonus -> {
    // terrain 上下文不在 core 侧重新取地图状态
    // 由 game/session 层先汇总"当前实体所在格 terrain tags"并作为显式上下文传入
    // 匹配时将 statModifier 加入 effective stats
}
```

#### 3.3.3 YAML 序列化 / 反序列化适配

`game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` 中的 affix/template passive 字段当前使用 `kind` 作为类型判别字段。新增 passive 需要在反序列化时支持新的 kind 值：

```yaml
# 新增 passive kind 的 YAML 表示示例：
passive: { kind: OnHitStatusProc, statusId: SLOW, chance: 0.15, duration: 2 }
passive: { kind: OnKillResourceRestore, resourceType: MANA, amount: 8 }
passive: { kind: ConditionalStatBonus, condition: HP_BELOW_50, statModifier: { attack: 5 } }
passive: { kind: TerrainAffinityBonus, terrainTag: WATER, statModifier: { evasion: 3, speed: 5 } }
```

#### 3.3.4 CombatPipeline 集成点

| 新增 passive 类型 | 集成点 | 触发时机 |
| --- | --- | --- |
| `OnHitStatusProc` | `CombatPipeline` 的伤害结算后、status 应用前 | 每次成功命中 |
| `OnKillResourceRestore` | `CombatPipeline` 的击杀回调（`onTargetKilled` 或等价） | 击杀确认后 |
| `ConditionalStatBonus` | stat 聚合阶段（`effectiveStats` 计算） | 每回合开始 |
| `TerrainAffinityBonus` | `game` 层地形上下文汇总后进入 stat 聚合 | 每回合开始 |

#### 3.3.5 Trace / Log / Inspect 支持

每个新增 passive 必须在以下 3 层可追溯：

1. **CombatResolutionTrace**：新增 `passiveTriggered` 字段记录本次战斗中触发的 passive 事件（passive ID + 触发次数 + 效果值）
2. **LogToken**：新增 passive 触发的 log token，格式如 `[item_passive_trigger: {passiveKind: OnHitStatusProc, statusId: SLOW, source: item.xxx}]`
3. **Client Inspect**：在装备 inspect 面板中显示 passive 效果描述（由 `descKey` 驱动）

#### 3.3.6 具体改造文件清单

| 文件 | 改动 |
| --- | --- |
| `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt` | 新增 4 个 `EquipmentPassive` 子类 + `PassiveCondition` 枚举 |
| `core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt` | 扩展 `when` 分支处理 4 种新 passive |
| `core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt` | 在合适的 step 调用 passive trigger（on-hit / on-kill）；不在 core 内重新查询地图地形 |
| `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` | 扩展 passive YAML 反序列化以支持新 kind |
| `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` | 新 kind 的验证逻辑（如 chance 范围检查） |
| `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | 如启用 `TerrainAffinityBonus`，负责把当前格 terrain 上下文归一化后传入 combat/stat 聚合路径 |
| `core/src/test/kotlin/com/ktome/core/item/PassiveEffectResolverTest.kt` | 每种新 passive 至少 2 个测试用例 |
| `core/src/test/kotlin/com/ktome/core/combat/` | passive 在 combat 中的集成测试 |

#### 3.3.7 不涉及的修改

1. **不在本 PR 新增具体 affix 数据**——affix YAML 扩充是 OPT-PR-04 的范围。
2. **不引入脚本解释器**——所有新 passive 都是编译时类型安全的 Kotlin sealed class。
3. **不新增 DamageType / StatusEffectType / ResourceType**——全部复用现有枚举。

#### 3.3.8 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| on-hit proc 引入 RNG 破坏 deterministic replay | 高 | 高 | 必须使用 `combatRng` 而非 `ThreadLocalRandom`；补充 fixed-seed replay 测试 |
| ConditionalStatBonus 的条件检查引入性能回归 | 低 | 低 | 条件检查只在 stat 聚合时执行一次，不是每帧 |
| TerrainAffinityBonus 误把地图状态拉进 core 形成第二真源 | 中 | 高 | 明确定义：terrain affinity 只消费 `game` 层显式传入的归一化 terrain context，不在 core 内读取 floor/map 状态 |
| 新增 Kotlin 子类导致现有 `when` exhaustive check 编译失败 | 确定 | 低 | 这是好事——编译器会指出所有需要修改的分支 |

**验收**

```bash
./gradlew :core:test
./gradlew :game:test
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew clientSmoke
```

**量化验收标准**

1. `EquipmentPassive` 子类从 5 个增加到 9 个。
2. 每个新 passive 有 ≥2 个单元测试 + ≥1 个 combat 集成测试。
3. `PassiveEffectResolver` 的所有 `when` 分支都有覆盖。
4. `lootBalanceLab` 报告能展示新 passive 类型（即使尚无使用它们的 affix）。
5. deterministic replay 测试通过（同 seed 产出相同结果）。

---

### OPT-PR-04：affix / unique / artifact 内容密度补齐

**目标**

把当前 `40 affix + 12 unique + 4 artifact = 56` 的内容规模，提升到接近路线图要求的 `≈120` 总量，并让 special template 真正形成 build 方向差异。

**为什么放在 OPT-PR-03 之后**

如果先扩 YAML、后补 mechanic contract，大量新数据要重写两次。

#### 3.4.1 Affix 扩充计划

**目标**：从 40 扩到 75~80，新增 35~40 个 affix。

**新增 affix 分类设计**：

##### A. 机制型 PREFIX（新增 10 个，全部使用 OPT-PR-03 的新 passive）

> **命名约束**：下表是“候选语义方向”，不是最终冻结 ID。实施前必须先跑 registry 去重，避免与现有 `vampiric` 等 affix id/family 冲突。

| 候选语义方向 | 目标 cost | equipType | passive kind | 推荐状态/条件 | zone 倾向 |
| --- | --- | --- | --- | --- | --- |
| 寒冷减速武器前缀 | 6 | WEAPON | OnHitStatusProc | `SLOW` | underground_river |
| 雷击眩晕武器前缀 | 6 | WEAPON | OnHitStatusProc | `STUN` | 全局 |
| 破甲武器前缀 | 6 | WEAPON | OnHitStatusProc | `ARMOR_BREAK` | deep_iron_pit |
| 灼烧武器前缀 | 10 | WEAPON | OnHitStatusProc | `BURN` | molten/deep_iron |
| 低血爆发武器前缀 | 10 | WEAPON | ConditionalStatBonus | `HP_BELOW_50` | 全局 |
| 残血狂战武器前缀 | 14 | WEAPON | ConditionalStatBonus | `HP_BELOW_30` | 全局 |
| 高血守护护甲前缀 | 10 | ARMOR | ConditionalStatBonus | `HP_ABOVE_80` | 全局 |
| 水域亲和武器前缀 | 6 | WEAPON | TerrainAffinityBonus | `WATER` | underground_river |
| 油面亲和护甲前缀 | 6 | ARMOR | TerrainAffinityBonus | `OIL` | deep_iron_pit |
| 击杀回体武器前缀 | 10 | WEAPON | OnKillResourceRestore | `STAMINA` | 全局 |

##### B. 机制型 SUFFIX（新增 8 个）

| 候选语义方向 | 目标 cost | 适用类型 | passive kind | 推荐状态/条件 | zone 倾向 |
| --- | --- | --- | --- | --- | --- |
| 击杀回蓝后缀 | 10 | WEAPON / ARMOR（分别命名） | OnKillResourceRestore | `MANA` | 全局 |
| 击杀回能量后缀 | 10 | WEAPON / ARMOR（分别命名） | OnKillResourceRestore | `ENERGY` | 全局 |
| 水域防护后缀 | 6 | ARMOR | TerrainAffinityBonus | `WATER` | underground_river |
| 油面进攻后缀 | 6 | WEAPON | TerrainAffinityBonus | `OIL` | deep_iron_pit |
| 冰冻附着后缀 | 10 | WEAPON | OnHitStatusProc | `FREEZE` | underground_river |
| 灼烧附着后缀 | 10 | WEAPON | OnHitStatusProc | `BURN` | deep_iron_pit |
| 低血防守后缀 | 6 | ARMOR | ConditionalStatBonus | `HP_BELOW_50` | 全局 |
| 高血稳态后缀 | 6 | ARMOR | ConditionalStatBonus | `HP_ABOVE_80` | 全局 |

##### C. 通用数值型 affix 补齐（新增 17~22 个）

补齐目标：让每个 cost tier 都有足够的选择空间（目标每 tier ≥6 个）。

| cost tier | 当前数量 | 目标数量 | 需补充 | 补充方向 |
| --- | --- | --- | --- | --- |
| TRIVIAL (1) | 2 | 4~5 | 2~3 | +evasion, +accuracy, +speed |
| MINOR (3) | 6 | 8~9 | 2~3 | +maxStamina, +maxMana, +critChance |
| MEDIUM (6) | 14 | 18~20 | 4~6 | 元素 resistance (COLD/LIGHTNING/HOLY 方向补齐)、双 stat 组合 |
| MAJOR (10) | 11 | 14~16 | 3~5 | 高阶 stat 包 (str+attack, dex+crit, wil+talentPower) |
| SIGNATURE (14) | 3 | 5~6 | 2~3 | 特殊组合 (如 allResistance, 全属性+2) |

##### D. Affix 扩充后的 exclusiveGroup 增补

新增机制型 affix 需要与既有 affix 建立互斥/同族关系：

```yaml
# 新增 exclusiveGroup（示意；最终成员以 registry audit 后的正式 ID 为准）:
weapon_proc_prefix:    [<cold_proc_weapon>, <lightning_proc_weapon>, <armor_break_weapon>, <burn_weapon>]
weapon_conditional:    [<low_hp_burst_weapon>, <berserk_weapon>]
terrain_prefix:        [<water_affinity_weapon>, <oil_affinity_armor>]
suffix_kill_resource:  [<mana_on_kill_suffix>, <energy_on_kill_suffix>, <stamina_on_kill_prefix>]
suffix_terrain:        [<water_armor_suffix>, <oil_weapon_suffix>]
suffix_proc:           [<freeze_weapon_suffix>, <burn_weapon_suffix>]
```

#### 3.4.2 Unique 模板扩充计划

**目标**：从 12 扩到 20~24。每个新 unique 必须使用 OPT-PR-03 的新 passive 类型。

##### 按 zone 分配的新增 unique

| zone | 当前 unique 数 | 新增 unique | passive 设计 |
| --- | --- | --- | --- |
| greenwood_fringe | 3 (watcher_blade, briarbound_bow, warden_barkmail) | **unique.trapper_net** (WEAPON): OnHitStatusProc(SLOW, 0.20, 2) | 控制流：命中减速 |
| greenwood_fringe | | **unique.duskweave_cloak** (ARMOR): ConditionalStatBonus(HP_BELOW_50, {evasion: 6, speed: 8}) | 低血逃生 |
| deep_iron_pit | 3 (slagbreaker_pick, furnace_plate, coal_ward_talisman) | **unique.magma_gauntlet** (WEAPON): OnHitStatusProc(BURN, 0.25, 3, magnitude: 0.08) | 持续灼伤 |
| deep_iron_pit | | **unique.forgefather_aegis** (ARMOR): TerrainAffinityBonus(OIL, {defense: 6, maxHp: 12}) | OIL 地形防御特化 |
| underground_river | 3 (tideglass_staff, currentstrider_cloak, ferrywarden_lantern) | **unique.icewalker_boots** (ARMOR): TerrainAffinityBonus(WATER, {speed: 10, evasion: 4}) + TerrainAffinityBonus(ICE, {defense: 4}) | 水域特化 |
| underground_river | | **unique.drowned_blade** (WEAPON): OnKillResourceRestore(MANA, 12) | 击杀回蓝流 |
| grey_gate_depths | 2 (voidlit_seal, eclipse_mail) | **mindshatter focus**（`OFF_HAND` 或 `ARMOR` 兼容 base item，最终 ID 待定）：OnHitStatusProc(BANE, 0.18, 2) | 精神控制 |
| grey_gate_depths | | **unique.phantom_cowl** (ARMOR): ConditionalStatBonus(HP_BELOW_30, {evasion: 10, critChance: 0.06}) | 高风险高回报 |
| abyssal_temple | 1 (sanctum_lance) | **unique.abyssal_conduit** (WEAPON): OnKillResourceRestore(MANA, 10) + ConditionalStatBonus(HP_BELOW_50, {talentPower: 0.06}) | 法术续航 |
| abyssal_temple | | **unique.eclipse_barrier** (ARMOR): ConditionalStatBonus(HP_BELOW_50, {defense: 5, maxHp: 8}) | 危机反打 |

#### 3.4.3 Artifact 模板扩充计划

**目标**：从 4 扩到 8。每个 artifact 必须有显著的 build-defining 效果。

| 当前 artifact | 当前 passive | 改造方向 |
| --- | --- | --- |
| `briar_heart` | DamageTypeBonus(HOLY, 0.12) | → ConditionalStatBonus(HP_BELOW_30, {attack: 8, critChance: 0.08}) + OnKillResourceRestore(STAMINA, 10) |
| `forge_oath` | DamageTypeBonus(FIRE, 0.18) | → OnHitStatusProc(BURN, 0.30, 3) + TerrainAffinityBonus(OIL, {attack: 5}) |
| `river_echo` | DamageTypeBonus(LIGHTNING, 0.18) | → OnKillResourceRestore(MANA, 15) + TerrainAffinityBonus(WATER, {talentPower: 0.10}) |
| `eclipsed_relic` | HpRegenPerTurn(3) | → ConditionalStatBonus(HP_BELOW_50, {defense: 8, maxHp: 20}) + HpRegenPerTurn(4) |

| 新增 artifact | zone | 来源 | passive 设计 | build 方向 |
| --- | --- | --- | --- | --- |
| `artifact.blood_pact` | greenwood_fringe | BOSS | OnHitStatusProc(BANE, 0.20, 2) + ConditionalStatBonus(HP_BELOW_50, {attack: 6}) | 高风险压制战士 |
| `artifact.ironstar_core` | deep_iron_pit | SECRET_ZONE | TerrainAffinityBonus(OIL, {defense: 8, maxHp: 15}) + ResistanceBonus(FIRE, 15) | OIL 坦克专精 |
| `artifact.tidal_heart` | underground_river | BOSS | OnKillResourceRestore(ENERGY, 8) + TerrainAffinityBonus(WATER, {speed: 10}) | 水域机动型 |
| `artifact.void_lens` | grey_gate_depths / abyssal_temple | SECRET_ZONE | OnHitStatusProc(BANE, 0.25, 3) + ConditionalStatBonus(HP_BELOW_50, {talentPower: 0.12}) | 控制法师 |

#### 3.4.4 i18n / visual / audio 资源需求汇总

| 类别 | 新增数量 | nameKey/descKey | icon | visual | SFX |
| --- | --- | --- | --- | --- | --- |
| 机制型 affix | 18 | 36 (zh + en) | 18 | — | — |
| 通用数值 affix | ~20 | ~40 | ~20 | — | — |
| unique 模板 | 10 | 20 | 10 | 10 | 10 |
| artifact 模板 | 4 | 8 | 4 | 4 | 4 |
| artifact 改造 | 4 | 8 (更新) | — | — | — |
| **合计** | **~52** | **~112** | **~52** | **~14** | **~14** |

#### 3.4.5 具体改造文件清单

| 文件 | 改动 |
| --- | --- |
| `game/src/main/resources/data/items/index.yaml` | 新增 ~38 affix + 10 unique + 4 artifact + 改造 4 artifact 的 passive；最终 ID/affixFamily/exclusiveGroup 以 registry 审计结果为准 |
| `game/src/main/resources/i18n/zh-CN.json` | 新增 ~112 条 name/desc |
| `game/src/main/resources/i18n/en-US.json` | 同上 |
| `assets-src/image/specs/` | 新增 ~52 icon + ~14 visual 的生成计划 |
| `assets-src/audio/specs/` | 新增 ~14 SFX 的生成计划 |

#### 3.4.6 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| 新增 affix 与既有 affix 的 affixFamily / exclusiveGroup 冲突 | 中 | 中 | 完成后跑 `lootBalanceLab` 检查 affix 冲突报告 |
| unique/artifact 的 passive 过强导致平衡失调 | 高 | 中 | 数值设计参照现有 SIGNATURE affix (cost=14) 的强度上限；完成后人工 playtest，并用 `lootBalanceLab` 看候选池是否被少数模板垄断 |
| i18n 条目遗漏 | 中 | 低 | `assetLint` / `manifestLint` 会捕获缺失的 key |

**验收**

```bash
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./gradlew phase4Report
```

**量化验收标准**

1. affix 总数 ≥ 75。
2. unique 总数 ≥ 20。
3. artifact 总数 ≥ 8。
4. **affix + unique + artifact 总量 ≥ 103**（接近路线图 120 目标）。
5. 使用新 passive kind 的 affix ≥ 18 个。
6. `affixPassiveCoverage` ≥ 80%（使用了 ≥7/9 种 passive kind）。
7. 每个 target zone 至少有 3 个使用新 passive 的 unique/artifact。
8. `whiteBoxLoot` 报告中 `uniqueArtifactMeaningfulSwapRate` ≥ 50%。

---

### OPT-PR-05：Loot Profile V3 与奖励池差异化

**目标**

解决”固定 `itemIds` 列表 + rewardBudget”导致的候选池重叠和终端掉落差异不足。

**为什么这是单独一 PR**

这一步触及 schema/runtime/content-pack overlay，是高风险合同变更，必须和内容扩容分开。

#### 3.5.1 LootProfileSchemaV2 → V3 的 schema 变更

当前 schema（`SchemaModels.kt:129-139`）：

```kotlin
data class LootProfileSchemaV2(
    val id: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val itemIds: List<String>,     // 固定候选列表
    val rewardBudget: Int,
)
```

V3 新增字段（采用单 schema 破坏式迁移，不保留长期 V2/V3 双轨）：

```kotlin
data class LootProfileSchemaV3(
    val id: String,
    val schemaVersion: Int,
    val tags: List<String>,

    // === 保留的业务语义字段 ===
    val itemIds: List<String> = emptyList(),
    val rewardBudget: Int,

    // === V3 新增字段 ===
    val poolStrategy: PoolStrategy = PoolStrategy.FIXED_LIST,  // 默认走旧逻辑
    val itemTagFilter: List<String> = emptyList(),  // 按 tag 过滤 item registry
    val excludeIds: List<String> = emptyList(),     // 显式排除
    val typeWeights: Map<String, Double> = emptyMap(),      // ItemType 权重：WEAPON/ARMOR/CONSUMABLE
    val slotBias: Map<String, Double> = emptyMap(),         // 可选：EquipSlot 偏置（WEAPON/OFF_HAND/ARMOR）
    val specialTemplateTagPreference: List<String> = emptyList(), // 仅作为 special template pool 的权重偏好
    val affixTagPreference: List<String> = emptyList(),     // 偏好的 affix tag (提升权重)
)

enum class PoolStrategy {
    FIXED_LIST,        // 旧逻辑：从 itemIds 选
    TAG_WEIGHTED,      // 新逻辑：从 itemTagFilter 匹配的 item 池中按 typeWeights 选
}
```

#### 3.5.2 破坏式迁移策略

1. 全部 `21` 个正式 loot profile 一次性迁移到 `schemaVersion: 3`。
2. foundation fallback 与早期 zone 仍可使用 `poolStrategy = FIXED_LIST`，但 schema 仍统一为 V3。
3. runtime / tools / content-pack overlay 只接受 V3；`schemaVersion: 2` 的 loot profile 在 DataLoader 层直接 fail-fast。
4. 不保留第二套 V2 parser、adapter、overlay merge 分支或 white-box summary 口径。
5. sample pack 与所有 fixture pack 在同一 PR 内同步迁到 V3，不接受“官方数据先迁、pack 后补”的长期中间态。

#### 3.5.3 TAG_WEIGHTED 候选池构建算法

```
输入: profile, itemRegistry, rewardBudget
1. basePool = itemRegistry.filter(item.tags 包含 profile.itemTagFilter 中的至少一个)
2. basePool -= profile.excludeIds
3. 如果 profile.typeWeights 非空:
     按 item.type (WEAPON/ARMOR/CONSUMABLE) 分桶
     每桶按 typeWeight 概率加权
4. 如果 profile.slotBias 非空:
     对可装备物品再按 EquipSlot 做二次偏置（WEAPON/OFF_HAND/ARMOR）
5. selectedItem = weightedSelect(basePool, typeWeights, slotBias)
6. rarity = rarityRoll(rewardBudget, magicFind, pityTracker)  // 复用现有
7. if rarity in {UNIQUE, ARTIFACT}:
      仍走现有 special template eligibility / pool 逻辑
      只允许用 specialTemplateTagPreference 做权重偏好，不建立第二套 zone-exclusive 真源
   else:
      roll affixes with budget, preferring profile.affixTagPreference
```

#### 3.5.4 21 个 Loot Profile 的迁移计划

| Profile 类别 | 数量 | 迁移策略 |
| --- | --- | --- |
| `loot.foundation.*` (common/elite/boss) | 3 | **迁移到 V3 + FIXED_LIST**：仍是全局 fallback，但不再保留 V2 schema |
| 早期 zone cadence (shattered_outpost, bandit_camp, elven_ruins) | 3 | **迁移到 V3 + FIXED_LIST**：早期 zone 可保守，但 schema 统一 |
| **升级 zone cadence** (greenwood, deep_iron, molten_core, grey_gate, underground_river, crystal_cavern, abyssal_temple) | 7 | **迁移到 V3**：使用 TAG_WEIGHTED + zone-specific itemTagFilter |
| zone reward (greenwood, deep_iron, grey_gate, abyssal_heart) | 4 | **迁移到 V3**：reward profile 需要更大的候选差异 |
| secret zone loot (greenwood, deep_iron, underground_river, abyssal_temple) | 4 | **迁移到 V3**：secret loot 应有独特内容 |

迁移到 V3 的 profile 示例：

```yaml
# 当前 V2：
- id: loot.greenwood_fringe.cadence
  schemaVersion: 2
  tags: [loot, cadence, greenwood]
  rewardBudget: 4
  itemIds: [hunter_bow, emerald_charm, leather_armor, apprentice_robe]

# 改为 V3：
- id: loot.greenwood_fringe.cadence
  schemaVersion: 3
  tags: [loot, cadence, greenwood]
  rewardBudget: 4
  poolStrategy: TAG_WEIGHTED
  itemTagFilter: [greenwood, forest, bog]            # 匹配所有带这些 tag 的 base item
  typeWeights:
    WEAPON: 0.30
    ARMOR: 0.30
    CONSUMABLE: 0.15
  slotBias:
    OFF_HAND: 0.20
    ARMOR: 0.10
  specialTemplateTagPreference: [greenwood, forest, control]
  affixTagPreference: [forest, physical, cold]       # greenwood 偏好自然/寒冷系 affix
  excludeIds: []
```

```yaml
# secret zone 示例：
- id: loot.greenwood_hidden_cache.secret
  schemaVersion: 3
  tags: [loot, secret, greenwood]
  rewardBudget: 5                                     # 高于 cadence
  poolStrategy: TAG_WEIGHTED
  itemTagFilter: [greenwood, forest, secret]          # 包含 secret tag 的独有物品
  typeWeights:
    WEAPON: 0.25
    ARMOR: 0.25
    CONSUMABLE: 0.15
  slotBias:
    OFF_HAND: 0.25                                   # secret 可偏好副手/法器，但不引入 ACCESSORY
  specialTemplateTagPreference: [secret, greenwood, control]
  affixTagPreference: [forest, cold, control]
  excludeIds: [healing_potion, stamina_draught]       # 排除普通消耗品
```

#### 3.5.5 具体改造文件清单

| 文件 | 改动 |
| --- | --- |
| `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` | 用 `LootProfileSchemaV3` 替换正式 schema，不保留长期 V2/V3 双类型 |
| `game/src/main/kotlin/com/ktome/game/GameContent.kt` | 将 `lootProfilesById` 等读取点升级到 V3 类型或共享父类型，避免调用层继续写死 V2 |
| `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` | 只接受 V3 schema；对 V2 正式 profile 与 V2 overlay 直接 fail-fast |
| `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | 维持 profile 到 runtime 候选池的调用边界；若生成逻辑仍集中在 session，补 `TAG_WEIGHTED` 分支 |
| `game/src/main/resources/data/loot/index.yaml` | 一次性迁移全部 21 个正式 profile 到 V3 |
| `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt` | 确保 overlay REPLACE 支持 V3 profile |
| `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt` | 更新 overlap matrix 计算以适配 TAG_WEIGHTED |
| `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt` | 适配新 profile 格式 |
| `tools/src/main/kotlin/com/ktome/tools/contentpack/ContentPackHarnessRunner.kt` | 验证 content pack 在 V3 下仍能正确 overlay |
| `examples/content-packs/sample.flooded_relics/manifest.yaml` | 若 sample pack 覆盖了迁移的 profile，需同步更新 |

#### 3.5.6 Content Pack 与 Fail-Fast 迁移

1. runtime 只接受 V3 loot profile overlay。
2. 旧 V2 pack 若仍试图覆盖 loot profile，直接 fail-fast，并输出结构化错误：`packId / targetProfileId / actualSchemaVersion / expectedSchemaVersion`。
3. `ContentPackRuntimeResolver` 对 loot profile overlay 的白名单字段需要同步扩到 `itemTagFilter / excludeIds / typeWeights / slotBias / specialTemplateTagPreference / affixTagPreference`，否则 V3 profile 会在 merge 层被静默裁剪。
4. `contentPackHarness` 必须覆盖以下场景：
   - V3 profile 被另一个 V3 profile REPLACE
   - V3 profile `ADD` 新增
   - 旧 V2 overlay 被明确拒绝
   - sample pack / fixture pack 在 V3 环境下仍通过

#### 3.5.7 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| 破坏式迁移把 sample pack / fixture / tools 留成半升级状态 | 中 | 高 | schema、official data、sample pack、fixture pack、overlay harness 必须在同一 PR 内一起迁完 |
| TAG_WEIGHTED 候选池为空（tag 不匹配） | 中 | 高 | DataLoader 加载时验证每个 V3 profile 的 itemTagFilter 至少匹配 1 个 base item |
| Content pack overlay 与 V3 schema 不兼容 | 中 | 高 | contentPackHarness 必须覆盖所有 overlay 场景 |
| 迁移后掉落分布偏移过大 | 中 | 中 | 先跑 `lootBalanceLab` 对比 V2 和 V3 的掉落分布，确认偏差在可接受范围内 |

**验收**

```bash
./gradlew :game:test
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew contentPackHarness
./gradlew whiteBoxContentPack
./gradlew phase4Report
```

**量化验收标准**

1. `lootProfileBaseItemOverlapMatrix` 平均重叠率从 `OPT-PR-01` 首次实测值降到 <30%。
2. `lootProfileDistinctBaseItemCount` ≥ 35。
3. 21 个 V3 profile 全部通过 DataLoader 验证。
4. `contentPackHarness` 的 V3 场景全部 PASS，且 V2 overlay 被结构化拒绝。
5. `lootBalanceLab` 的 rarity 分布偏差在 V2 基线的 ±15% 以内。
6. `lootFormulaVersion` 已升版。

---

### OPT-PR-06：Hidden Content 多样化与入口熵提升

**目标**

把 hidden content 从”到 optional.branch.1 就搜”的单一路径，改成多触发类型、多入口锚点、多奖励结构的系统。

**为什么放在 Loot Profile V3 之后**

隐藏奖励质量依赖更丰富的掉落池；否则只是把同样的奖励换一种触发方式。

#### 3.6.1 当前 Hidden Content 详细现状

**Events（8 个 = 4 对 reveal + reward）**：

| Event | triggerType | reward |
| --- | --- | --- |
| greenwood.hidden_cache.reveal | PERCEPTION_REVEAL | REVEAL_SECRET_ZONE |
| greenwood.hidden_cache.reward | INTERACT_TILE | LOOT_PROFILE |
| deep_iron.slag_cache.reveal | PERCEPTION_REVEAL | REVEAL_SECRET_ZONE |
| deep_iron.slag_cache.reward | INTERACT_TILE | GRANT_BUFF + LOOT_PROFILE |
| underground_river.crystal_rift.reveal | PERCEPTION_REVEAL | REVEAL_SECRET_ZONE |
| underground_river.crystal_rift.reward | INTERACT_TILE | TRIGGER_ENCOUNTER + LOOT_PROFILE |
| abyssal_temple.warded_archive.reveal | PERCEPTION_REVEAL | REVEAL_SECRET_ZONE |
| abyssal_temple.warded_archive.reward | INTERACT_TILE | LOOT_PROFILE |

**Secret Zones（4 个）**：

| Zone | PERCEPTION_CHECK diff | entranceBindingId | returnBridgePolicy |
| --- | --- | --- | --- |
| greenwood_hidden_cache | 8 | optional.branch.1 | LAST_MAINLINE_BRANCH |
| deep_iron_slag_cache | 12 | optional.branch.1 | NEAREST_OPTIONAL_ANCHOR |
| underground_river_crystal_rift | 16 | optional.branch.1 | EXPLICIT_ANCHOR (hub) |
| abyssal_temple_warded_archive | 20 | optional.branch.1 | NEAREST_OPTIONAL_ANCHOR |

**问题总结**：
- reveal event 全部使用 PERCEPTION_REVEAL，4/6 trigger type 未使用
- entranceBindingId 全部是 `optional.branch.1`
- 虽然 reward 有 4 种变化（纯 loot / buff+loot / encounter+loot / 纯 loot），但发现路径完全相同

#### 3.6.2 改造方案

##### A. 触发类型多样化

| zone | 当前 reveal trigger | 改为 | 设计意图 |
| --- | --- | --- | --- |
| greenwood | PERCEPTION_REVEAL | **保持 PERCEPTION_REVEAL** | 作为最简单的教学用例，保留原始体验 |
| deep_iron | PERCEPTION_REVEAL | **KILL_ELITE** | 击杀 forge zone 的特定精英后，reveal event 触发。条件从 `SEARCH_BINDING_ID` 改为 `ZONE_ID: deep_iron_pit + KILL_ELITE` |
| underground_river | PERCEPTION_REVEAL | **OPEN_CHEST** | 打开特定 vault 中的宝箱后触发。条件改为 `ZONE_ID: underground_river + INTERACTABLE_ID: vault.underground_river.crystal_cache.chest` |
| abyssal_temple | PERCEPTION_REVEAL | **QUEST_STEP** | 完成 abyssal_temple 的 key gate 目标后触发。条件改为 `ZONE_ID: abyssal_temple + QUEST_STEP` |

##### B. Secret Zone entryRule 多样化

| zone | 当前 entryRule | 改为 | 说明 |
| --- | --- | --- | --- |
| greenwood | PERCEPTION_CHECK(8) | **保持** | 教学用，最低难度感知 |
| deep_iron | PERCEPTION_CHECK(12) | **无 entryRule（reveal 即可进入）** | 击杀精英本身就是”发现代价”，不需要再加 perception 门槛 |
| underground_river | PERCEPTION_CHECK(16) | **PERCEPTION_CHECK(10) + 需要 OPEN_CHEST 已完成** | 降低 perception 要求但增加前置条件，组合型 discoveryRule |
| abyssal_temple | PERCEPTION_CHECK(20) | **PERCEPTION_CHECK(15)** | 降低但保留，因为 QUEST_STEP reveal 已提供了前置条件；至少保留一条 perception 教学路径 |

##### C. entranceBindingId 打散

| zone | 当前 bindingId | 改为 | mapgen 影响 |
| --- | --- | --- | --- |
| greenwood | optional.branch.1 | **optional.branch.1** (保持) | 无变化 |
| deep_iron | optional.branch.1 | **`<selected-critical-anchor>`** | 先从 `whiteBoxMapgen` / `mapgenSmoke` 产物中挑选稳定存在的 critical anchor；若现有 family 不足，升级 topology contract |
| underground_river | optional.branch.1 | **`<selected-optional-anchor>`** | 先确认该 zone 是否稳定产出第二类 optional anchor；若 family 不足，升级 topology contract，而不是永久回退到单一入口 |
| abyssal_temple | optional.branch.1 | **`<selected-goal-adjacent-anchor>`** | 必须先用 corpus 审计证明其既靠近目标，又不破坏 solvability；若现有 topology 无法承载，升级 hidden anchor family |

##### D. Reward 结构差异化增强

| zone | 当前 reward | 改为 | 差异化方向 |
| --- | --- | --- | --- |
| greenwood.reward | LOOT_PROFILE | **LOOT_PROFILE + GRANT_BUFF**(PERCEPTION_UP, 5 turns, 0.15) | 发现奖励 = 掉落 + 临时增强感知（帮助后续发现） |
| deep_iron.reward | GRANT_BUFF + LOOT_PROFILE | **TRIGGER_ENCOUNTER(精英级) + LOOT_PROFILE(高 budget)** | 高风险高回报：必须打一场精英战，但奖励 budget 从 6→8 |
| underground_river.reward | TRIGGER_ENCOUNTER + LOOT_PROFILE | **LOOT_PROFILE（提高 rewardBudget，并用 `specialTemplateTagPreference` 偏向 river/control 方向）** | 安全但更偏向主题化稀有掉落，不建立 zone-exclusive 第二真源 |
| abyssal_temple.reward | LOOT_PROFILE | **LOOT_PROFILE + TRIGGER_ENCOUNTER(boss 级小型) + REVEAL_SECRET_ZONE** | 最终 zone 的隐藏内容是一条链：发现 archive → 战斗 → 掉落 → 揭示更深层秘密 |

##### E. 新增 2 个 hidden event（扩充总量从 8→12）

为增加隐藏内容的总量和多样性，新增 2 对事件（4 个 event）：

| 新增 Event | trigger | zone | reward | 说明 |
| --- | --- | --- | --- | --- |
| deep_iron.smuggler_stash.reveal | ENTER_ROOM (特定 vault room) | deep_iron | REVEAL_SECRET_ZONE | 进入特定 vault room 自动触发 |
| deep_iron.smuggler_stash.reward | INTERACT_TILE | deep_iron | LOOT_PROFILE (消耗品为主) | 走私者缓存，偏向消耗品 |
| abyssal.void_whisper.reveal | PERCEPTION_REVEAL (高难度) | abyssal | GRANT_BUFF (temporary vision) | 纯 perception 发现，但只给 buff 不给入口 |
| abyssal.void_whisper.reward | — (无 reward event，buff 即奖励) | abyssal | — | 这是一个无独立奖励的事件，展示事件不一定有 loot |

#### 3.6.3 Mapgen Zone Profile 改造

`game/src/main/resources/data/mapgen/zones/index.yaml` 中每个 zone 的 `hiddenEntrancePlans` 需要同步更新：

```yaml
# deep_iron_pit: 改 sourceAnchorId 和 entranceAnchorId
hiddenEntrancePlans:
  - bindingId: search.deep_iron.slag_cache
    sourceAnchorId: <selected-critical-anchor>
    entranceAnchorId: <selected-critical-anchor>
    targetAnchorId: secret.deep_iron.slag_cache
    targetSecretZoneId:
      registry: secret_zone
      id: deep_iron_slag_cache
    discoveryRule:
      combinator: AND
      predicates:
        - type: KILL_ELITE               # 改：不再是 PERCEPTION_CHECK
          zoneId: deep_iron_pit
  - bindingId: search.deep_iron.smuggler_stash   # 新增第二个
    sourceAnchorId: optional.branch.1
    entranceAnchorId: optional.branch.1
    targetAnchorId: secret.deep_iron.smuggler_stash
    targetSecretZoneId:
      registry: secret_zone
      id: deep_iron_smuggler_stash
    discoveryRule:
      combinator: AND
      predicates:
        - type: ENTER_ROOM
          roomTag: vault
```

若现有 topology 审计无法稳定提供 `>=3` 类 hidden 入口家族，则本 PR 不接受长期停留在“从现有 incidental anchor 中勉强挑几个”的状态，而是直接升级正式 contract：

1. 在 topology / mapgen schema 中引入 hidden 专用 anchor family（如 `hidden.branch`、`hidden.goal.adjacent`）
2. `hiddenEntrancePlans` 只依赖正式 family，不再写死偶然 anchor id
3. `whiteBoxMapgen / solvabilityHarness` 负责证明这些 family 在目标 zone/floor 上稳定存在

#### 3.6.4 具体改造文件清单

| 文件 | 改动 |
| --- | --- |
| `game/src/main/resources/data/events/index.yaml` | 修改 3 个 reveal event 的 triggerType 和 conditions；新增 4 个 event |
| `game/src/main/resources/data/secret-zones/index.yaml` | 修改 3 个 zone 的 entryRule 和 entranceBindingId；若 anchor 审计通过，再新增 1 个 zone (`deep_iron_smuggler_stash`) |
| `game/src/main/resources/data/mapgen/zones/index.yaml` | 修改 3 个 zone 的 hiddenEntrancePlans（anchor 和 discoveryRule）；若现有 family 不足，同步升级 topology/anchor 合同 |
| `game/src/main/resources/data/loot/index.yaml` | 若新增 `deep_iron_smuggler_stash`，补 1 个 loot profile；并更新部分 secret loot 的 rewardBudget / tag 偏好 |
| `game/src/main/resources/i18n/zh-CN.json` | 新增 5 个 secret zone / event 的 name/desc |
| `game/src/main/resources/i18n/en-US.json` | 同上 |
| `tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt` | 适配新的 trigger type 测试用例 |
| `tools/src/main/kotlin/com/ktome/tools/hidden/WhiteBoxHiddenContentRunner.kt` | 更新 coverage 指标计算 |

#### 3.6.5 需要验证的 Mapgen 兼容性

1. `<selected-critical-anchor>` 是否在所有 deep_iron_pit zone 的 topology 中稳定存在——需以 `mapgenSmoke` + `whiteBoxMapgen` 实测确认。
2. underground_river 是否稳定产出第二类 optional anchor——若现有 family 不足，则在本 PR 内升级 topology/anchor 合同，而不是长期退回单一入口。
3. goal-adjacent anchor 是否有足够空间放置入口——需 `solvabilityHarness` + `whiteBoxMapgen` 共同验证。

#### 3.6.6 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| KILL_ELITE trigger 在无精英的 seed 中不触发 | 中 | 高 | 确保 deep_iron zone 的精英生成率 ≥ 80%；或添加 fallback：如果该 floor 无精英，自动降级为 PERCEPTION_CHECK |
| 现有 anchor family 不足以支撑 hidden diversity | 中 | 高 | 在同一 PR 内升级 topology/hidden anchor 合同，不接受“暂时只做 trigger 变化”的长期中间态 |
| 新增的链式 hidden event (abyssal) 增加复杂度 | 低 | 中 | 链式触发只是两个独立 event 的前后依赖，不引入新的合同 |

**验收**

```bash
./gradlew mapgenSmoke              # 验证新 anchor 在 500 seed 中可达
./gradlew solvabilityHarness       # 验证新 hidden entrance 的 solvability
./gradlew hiddenContentHarness
./gradlew whiteBoxHiddenContent
./gradlew whiteBoxVerify
./gradlew phase4Report
```

**量化验收标准**

1. `hiddenTriggerTypeCoverage` ≥ 4/6 = 66%（使用 PERCEPTION_REVEAL + KILL_ELITE + OPEN_CHEST + QUEST_STEP；如新增 `ENTER_ROOM` 则作为加分项）。
2. `secretEntranceBindingCoverage` ≥ 3（来自至少 3 个已审计通过的 binding/anchor 家族）。
3. hidden event 总数从 8 增加到 12。
4. 若现有 topology 足够，则 secret zone 总数从 4 升到 5；若现有 topology 不足，则必须先完成 topology/anchor 合同升级，再把 secret zone 提升到 5。
5. `hiddenContentHarness` 500 seed 中 ≥30% run 触发 ≥1 个 hidden event（与基线持平或提升）。
6. `solvabilityHarness` 1000 seed 中 CRITICAL_PATH 可达性保持 100%。

---

### OPT-PR-07：地形可感知性与战术 uptake 调优

**目标**

基于 `OPT-PR-01` 的新指标，决定 terrain 系统是只需要摆放/读图调优，还是需要轻量的 encounter 绑定增强。

**为什么排最后**

terrain 问题当前证据最弱；先做度量、再做 mutation/装备/hidden 修正后，才能知道剩余差距在哪。

#### 3.7.1 决策树（依赖 OPT-PR-01 基线数据）

```
读取 OPT-PR-01 的基线指标:
  terrainTaggedCombatExposureRate
  terrainInteractionEncounterRate
  terrainCoverageByZone

路径 A: exposureRate < 20%
  → 问题在”地形覆盖不够 / 位置不对”
  → 执行 3.7.2A: 调整 biome terrainTagWeights 和 TerrainTagPainter 分布策略

路径 B: exposureRate ≥ 20% 但 encounterRate < 15%
  → 问题在”有地形但不触发规则”（技能与地形不匹配）
  → 执行 3.7.2B: 补充精英-地形绑定 + 少量玩家地形互动手段

路径 C: exposureRate ≥ 20% 且 encounterRate ≥ 15%
  → 问题在”触发了但玩家不知道”（UI 反馈不足）
  → 执行 3.7.2C: client readability 增强

实际可能是 A+B 或 A+C 的混合，按权重执行。
```

#### 3.7.2A 地形覆盖率调整方案

**当前 zone terrainTagWeights**：

| zone | WATER | OIL | ICE |
| --- | --- | --- | --- |
| greenwood_fringe | 0.22 | — | — |
| deep_iron_pit | — | 0.24 | — |
| underground_river | 0.28 | — | 0.16 |
| abyssal_temple | — | 0.20 | — |

**建议调整**（如果 path A 成立）：

| zone | WATER | OIL | ICE | 变化说明 |
| --- | --- | --- | --- | --- |
| greenwood_fringe | **0.30** | — | — | +0.08: 森林/沼泽区域应有更多水域 |
| deep_iron_pit | — | **0.32** | — | +0.08: 矿坑/锻造区域应有更多油 |
| underground_river | **0.35** | — | **0.22** | +0.07/+0.06: 水域是该 zone 的核心身份 |
| abyssal_temple | — | **0.28** | — | +0.08: 油库区域强化主题 |

**TerrainTagPainter 分布策略调整**：

当前问题：terrain 倾向于被铺设在房间边缘和走廊中，而非战斗发生的中心区域。

建议在 `game/src/main/kotlin/com/ktome/game/mapgen/` 的 `TerrainTagPainter`（或等价组件）中调整：

1. **优先铺设位置**：房间中心 2×2 区域 > 走廊瓶颈 > 房间边缘
2. **vault/pattern room 内部**：vault 内部地形覆盖率应高于普通房间
3. **精英/boss 生成房间**：标记为 `elite_spawn` 或 `boss_room` 的房间，地形覆盖率翻倍

#### 3.7.2B 精英-地形绑定方案

在 OPT-PR-02 完成后，利用新增的 mutation 与地形建立关联：

| mutation | 建议添加的房间偏好 | 效果 |
| --- | --- | --- |
| `elite.tidebound` | WATER 偏好 | 优先生成在有 WATER 的房间 |
| `elite.emberblood` | OIL 偏好 | 优先生成在有 OIL 的房间 |
| `elite.frostbound` | WATER / ICE 偏好 | 在 WATER/ICE 地形战斗有优势 |
| `elite.corrosion_cloud` | OIL 偏好 | OIL 地形增强 aura 效果 |

> **合同约束**：若 Path B 被验证为长期正式设计，则不接受长期停留在“按 mutation tag 猜 terrain affinity”的隐式逻辑。应把该语义提升为 `EliteMutationDef` 的显式字段，例如 `preferredTerrainTags: List<TerrainTag>`，并让 loader / harness / report 都能直接读取。

此外，OPT-PR-04 中新增的 `tidecaller` / `oilwalker` 等 terrain affix 和 `TerrainAffinityBonus` passive 已为玩家提供了”主动利用地形”的手段。

#### 3.7.2C Client Readability 增强

1. **地形 tile 视觉增强**：WATER / OIL / ICE 的 tile 颜色饱和度提升，边缘添加动态效果（波纹/油光/冰晶）
2. **战斗 log 增强**：terrain interaction 触发时在 log 中高亮显示规则名称和效果值
3. **inspect 面板增强**：当玩家查看某格时，显示该格的 terrain tag 和已知的交互规则
4. **combat UI 提示**：当实体站在 tagged terrain 上时，在实体头顶显示 terrain tag icon

#### 3.7.3 具体改造文件清单

| 文件 | 改动 | 条件 |
| --- | --- | --- |
| `game/src/main/resources/data/mapgen/biomes/index.yaml` | 调整 terrainTagWeights | Path A |
| `game/src/main/kotlin/com/ktome/game/mapgen/` | TerrainTagPainter 分布策略 | Path A |
| `game/src/main/resources/data/mapgen/zones/index.yaml` | 调整 zone 的 terrainTagWeights | Path A |
| `game/src/main/resources/data/elites/index.yaml` | 若 Path B 成立，新增 `preferredTerrainTags` 等显式字段 | Path B |
| `game/src/main/kotlin/com/ktome/game/elites/` | 精英生成房间选择消费显式 terrain affinity 字段，而不是私有猜测逻辑 | Path B |
| `client/` | terrain tile visual / combat log / inspect | Path C |
| `tools/` harness runners | 指标验证 | 所有 path |

#### 3.7.4 硬约束

1. **不新增 `TerrainTag` 枚举值**（Phase 4 已冻结）。
2. **正式 terrain rule 结算仍保持单一路径**；若增加 affinity 判定，优先由 `game` 层汇总 terrain context 后输入既有 combat/stat 管线，而不是在 `core` 中引入新的地图状态真源。
3. **不引入第二套环境规则**。
4. **不修改 5 种正式地形交互规则的效果逻辑**（只改覆盖率和 readability）。

#### 3.7.5 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
| --- | --- | --- | --- |
| terrainTagWeights 提升后地图美观度下降 | 中 | 低 | 控制提升幅度在 +0.10 以内；跑 mapgenSmoke 目视检查 |
| 精英-地形绑定导致特定房间的战斗难度突增 | 中 | 中 | 通过 bossHarness 验证 threatCost 是否仍在预算内 |
| TerrainTagPainter 改动影响 solvability | 低 | 高 | solvabilityHarness 1000 seed 验证 |

**验收**

```bash
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew whiteBoxMapgen
./gradlew whiteBoxVerify
./gradlew phase4Report
```

**量化验收标准**

1. `terrainTaggedCombatExposureRate` 相比 OPT-PR-01 基线提升 ≥50%（例如从 10%→15% 或从 20%→30%）。
2. `terrainInteractionEncounterRate` 相比基线提升 ≥30%。
3. `solvabilityHarness` 的 CRITICAL_PATH 可达性保持 100%。
4. `mapgenSmoke` 500 seed 无崩溃。
5. terrain 不新增 `TerrainTag` 枚举值。

---

## 4. 推荐执行顺序与阶段门

### 4.1 最小阻塞集（Phase 5 最低前置）

如果目标是”尽快补齐进入 Phase 5 的必要前置条件”，最低建议先完成：

| 顺序 | PR | 主要交付 | 阻塞 Phase 5 的原因 |
| --- | --- | --- | --- |
| 1 | `OPT-PR-01` | 10 个体验指标基线 | 后续所有 PR 的 before/after 度量依赖 |
| 2 | `OPT-PR-02` | 12 mutation + 3 boss variant 调整 | P5-W1 战术 AI 的行为对象池 |
| 3 | `OPT-PR-03` | 4 个新 EquipmentPassive 子类 | P5 平衡调参的基础词汇 |
| 4 | `OPT-PR-04` | ~52 个新 affix/unique/artifact | 内容密度目标 120 |
| 5 | `OPT-PR-05` | LootProfile V3 + 15 profile 迁移 | P5 掉落调参的 pool 差异化 |

**阶段门 Gate-1**（OPT-PR-01~05 完成后）：

```bash
./gradlew phase4Report
```

检查以下指标全部达标后方可考虑进入 Phase 5：
- `eliteMutationDistinctCount` ≥ 12 ✓
- `affix + unique + artifact 总量` ≥ 100 ✓
- `affixPassiveCoverage` ≥ 80% ✓
- `lootProfileBaseItemOverlapMatrix` 平均 < 30% ✓
- `lootProfileDistinctBaseItemCount` ≥ 35 ✓

### 4.2 推荐完整集（最佳体验水位线）

| 顺序 | PR | 可并行 | 说明 |
| --- | --- | --- | --- |
| 1 | `OPT-PR-01` | — | 必须第一 |
| 2a | `OPT-PR-02` | 可与 2b 并行 | 精英突变（纯数据 + 少量 core） |
| 2b | `OPT-PR-03` | 可与 2a 并行 | 装备词汇（纯 core 代码） |
| 3 | `OPT-PR-04` | — | 依赖 OPT-PR-03 |
| 4 | `OPT-PR-05` | — | 依赖 OPT-PR-04 |
| 5 | `OPT-PR-06` | — | 依赖 OPT-PR-05 |
| 6 | `OPT-PR-07` | — | 依赖 OPT-PR-01 基线 + OPT-PR-02 精英数据 |

**阶段门 Gate-2**（全部完成后）：

在 Gate-1 的基础上增加检查：
- `hiddenTriggerTypeCoverage` ≥ 66% ✓
- `secretEntranceBindingCoverage` ≥ 4 ✓
- `terrainTaggedCombatExposureRate` 相比基线提升 ≥ 50% ✓
- `terrainInteractionEncounterRate` 相比基线提升 ≥ 30% ✓

### 4.3 各 PR 规模估算

| PR | 新增/修改文件数 | 新增 YAML 条目 | 新增 Kotlin 代码量 | 新增资源 | 风险等级 |
| --- | --- | --- | --- | --- | --- |
| OPT-PR-01 | ~6 | 0 | ~300 行 | 0 | 低 |
| OPT-PR-02 | ~7 | ~18 | ~40 行 | ~12 (mutation icon+SFX) | 中 |
| OPT-PR-03 | ~7 | 0 | ~400 行 | 0 | 中（core 改动） |
| OPT-PR-04 | ~5 | ~52 | ~30 行 | ~80 (icon+visual+SFX) | 中（内容量大，且依赖新 passive 词汇） |
| OPT-PR-05 | ~9 | ~15 迁移 | ~500 行 | 0 | **高**（schema 变更） |
| OPT-PR-06 | ~8 | ~10 | ~100 行 | ~10 (i18n) | 中（mapgen 影响） |
| OPT-PR-07 | ~6 | ~4 | ~200 行 | ~5 (visual) | 中（条件性） |

### 4.4 资源 Companion 计划

不是所有优化 PR 都需要新美术或音频。按当前核实后的实施范围，资源需求应当这样拆：

| PR | 新图片 | 新音频 | 结论 |
| --- | --- | --- | --- |
| `OPT-PR-01` | 否 | 否 | 纯指标/报告 PR，不开资源批次 |
| `OPT-PR-02` | 是 | 是 | 新增 mutation icon 与 mutation cue；默认不新增 talent 资源 |
| `OPT-PR-03` | 否 | 否 | 纯 contract / core 扩展 PR，不开资源批次 |
| `OPT-PR-04` | 是 | 是 | item affix / unique / artifact 的正式资源主批次 |
| `OPT-PR-05` | 否 | 否 | loot schema/runtime 迁移，不新增玩家可见资源 key |
| `OPT-PR-06` | 条件性 | 条件性 | 只有在新增 secret zone / reveal prop / secret-zone ambience 时才开资源批次；若只是 trigger/binding 改写则不开 |
| `OPT-PR-07` | 条件性 | 可选 | Path C 的 readability 增强通常需要新图片；音频只在新增独立 terrain cue 时才需要 |

#### 4.4.1 统一命名与落点

所有优化 PR 的资源计划统一复用现有目录与命名约定：

1. 图片 plan：`assets-src/image/specs/phase4-opt-prNN-gemini-plan.yaml`
2. 图片报告：
   - `assets-src/image/manifests/phase4-opt-prNN-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-opt-prNN-processing-report.jsonl`
3. 音频 plan：`assets-src/audio/specs/phase4-opt-prNN-audio-plan.yaml`
4. 音频报告：
   - `assets-src/audio/manifests/phase4-opt-prNN-generation-report.jsonl`（若使用 PR 专用 raw 生成脚本）
   - `assets-src/audio/manifests/phase4-opt-prNN-processing-report.jsonl`
5. 若需要程序化生成 raw 音频，脚本命名统一为：`scripts/generate_opt_prNN_audio.py`

#### 4.4.2 统一管线约束

1. 图片必须复用既有管线：`scripts/generate_assets.sh -> scripts/process_assets.py`
2. 正式图片批量生成时固定使用并发：

```bash
GEMINI_API_KEY=your_key \
GEMINI_CONCURRENCY=4 \
./scripts/generate_assets.sh \
  assets-src/image/specs/phase4-opt-prNN-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase4-opt-prNN-generation-report.jsonl
```

3. 优化 PR 的图片资源都写回 base runtime，`RUNTIME_ASSET_DIR` 默认保持 `client/src/main/resources`，不另起 pack-root。
4. 音频必须复用既有“raw 生成/导入 -> process_audio.py”管线：

```bash
python3 scripts/generate_opt_prNN_audio.py \
  --plan assets-src/audio/specs/phase4-opt-prNN-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-prNN-generation-report.jsonl

python3 scripts/process_audio.py \
  --filter-plan assets-src/audio/specs/phase4-opt-prNN-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-prNN-processing-report.jsonl
```

5. 图片 plan 结构继续沿用现有 `styleTag / artStyleBible / stylePrompt / defaults / phase2AssetGates`。
6. 音频 plan 结构继续沿用现有 `manifestVersion / fallbackKey / packId / sampleRate / entries / targets`。
7. `gemini` 仅是图片 plan 文件历史命名，不代表新的运行时 schema。
8. 没有净新增玩家可见资源 key 的 PR，不允许为了“形式统一”创建空 plan 文件。

#### 4.4.3 各 PR 的资源开发计划

**OPT-PR-02**

1. 图片 plan：`assets-src/image/specs/phase4-opt-pr02-gemini-plan.yaml`
2. 音频 plan：`assets-src/audio/specs/phase4-opt-pr02-audio-plan.yaml`
3. 覆盖对象：
   - `icon.mutation.*`
   - `audio.mutation.*`
4. 资源规模：
   - 图片：6 个 mutation icon
   - 音频：6 个 mutation cue
5. 约束：
   - dedicated elite talent 默认复用对应 mutation 的 icon/cue 或现有 telegraph，不再平铺一套玩家向资源
   - 若 boss variant 只是重配现有 actionWeight，不新增 `vfx.boss.variant.*`

**OPT-PR-04**

1. 图片 plan：`assets-src/image/specs/phase4-opt-pr04-gemini-plan.yaml`
2. 音频 plan：`assets-src/audio/specs/phase4-opt-pr04-audio-plan.yaml`
3. 覆盖对象：
   - `item.unique.*.icon`
   - `item.artifact.*.icon`
   - `item.unique.*.visual`
   - `item.artifact.*.visual`
   - `audio.item.unique.*`
   - `audio.item.artifact.*`
4. 推荐拆成 3 个资源 batch，避免一次性生成过大：
   - Batch A：10 个 new unique 的 icon/visual/audio
   - Batch B：4 个 new artifact + 4 个 reworked artifact 的 icon/visual/audio
   - Batch C：affix 相关净新增 icon（以最终 registry audit 后的正式 key 为准）
5. 约束：
   - affix family / item template 若最终复用现有 key，则对应资源不重复生成
   - `OPT-PR-04` 的 plan 文件与 `items/index.yaml` 最终新增 key 集必须一一对应，不能先生成一批再靠 fallback 吞掉

**OPT-PR-06**

1. 若只是 trigger/binding/entryRule 改写：不开资源批次。
2. 只有当新增 secret zone、secret-zone portrait/icon、hidden entrance prop、或独立 reveal/ambience cue 时，才补：
   - `assets-src/image/specs/phase4-opt-pr06-gemini-plan.yaml`
   - `assets-src/audio/specs/phase4-opt-pr06-audio-plan.yaml`
3. 可复用 `PR-07` 的 key 组织方式：
   - 图片：`prop.hidden_*`、`zone.secret.*.icon`、`zone.secret.*.visual`
   - 音频：`audio.hidden.reveal.*`、`audio.secret_zone.*`
4. 开工前提：
   - anchor 审计通过
   - 确认不是单纯复用现有 `PR-07` 资源即可满足

**OPT-PR-07**

1. Path A/B 默认不强制新资源；优先通过地形权重、房间选择和 log/inspect 文案增强解决。
2. 若走 Path C 且需要新增视觉提示，则补：
   - `assets-src/image/specs/phase4-opt-pr07-gemini-plan.yaml`
3. 建议覆盖对象：
   - terrain readability overlay
   - terrain badge / state icon
   - 可选的 interact-highlight prop
4. 音频仅在确实新增独立 terrain cue 时才补：
   - `assets-src/audio/specs/phase4-opt-pr07-audio-plan.yaml`
5. 约束：
   - 纯 recolor / shader / existing tile atlas 调优不应强行开新资源批次
   - 若新增资源，必须同步补 `goldenScreenshot`

#### 4.4.4 资源门禁

所有开了资源批次的优化 PR，完成前至少要满足：

1. `./gradlew assetLint styleLint manifestLint`
2. `./gradlew audioLint`（若有音频）
3. `./gradlew goldenScreenshot`（若新增 client 可见图片资源）
4. 对应 `generation-report / processing-report` 文件存在且可追溯
5. `build.gradle.kts` 的相关 lint 入口已接入本 PR 的 `--extra-plan` 或等价扩展配置

---

## 5. 不纳入本轮计划的事项

1. **不重复返工 sample-pack golden real-path 问题**  
   该问题已由当前 `GoldenScreenshotHarnessTest` 的真实 runtime 路径修复，不再单独开 PR。

2. **不重复开 dual-pack precedence fixture 问题**  
   第二 precedence 夹具已存在，当前只需要在后续报告与回归中持续保留。

3. **不在本计划内引入脚本 runtime / Mod SDK / 新 TerrainTag / 新 DamageType**  
   这些都越过了 `Phase 4` 冻结边界。

---

## 6. 最终建议

基于当前核实结果，**不建议直接进入 Phase 5**。  
更稳妥的路径是：

1. 先做 `OPT-PR-01` 把 review 结论转成量化基线。
2. 再完成 `OPT-PR-02 ~ 05`，把 mutation、装备、掉落这三条直接影响重复游玩的主链补齐。
3. 最后根据新指标决定 `OPT-PR-06 ~ 07` 的力度，避免在 terrain / hidden 上做”看起来很多、实际上没有提升”的无效扩写。

### 6.1 预期改进效果总结

| 维度 | 当前 | OPT-PR-01~05 后 | OPT-PR-06~07 后 |
| --- | --- | --- | --- |
| 精英突变总数 | 6 | 12 | 12 |
| 精英有效组合数 | 待 `OPT-PR-01` 首测 | ≥40 | ≥40 |
| Affix + Unique + Artifact 总量 | 56 | ≥103 | ≥103 |
| EquipmentPassive 种类 | 5 | 9 | 9 |
| Loot profile 平均重叠率 | 待 `OPT-PR-01` 首测 | <30% | <30% |
| Hidden trigger type 覆盖 | 2/6 | 2/6 | ≥4/6（理想 5/6） |
| Secret zone binding 种类 | 1 | 1 | ≥3（若新增第 5 个 secret zone 则可到 4） |
| Terrain exposure rate | 未知 | 已度量 | 相比基线 +50% |
| Phase 4 审查评分预期 | 6.5/10 | 8/10 | 8.5~9/10 |

### 6.2 后续流程建议

1. 本文档已包含足够详细的 PR 级输入，可直接作为实施起点；但 **OPT-PR-03 / OPT-PR-05 / OPT-PR-06** 这类触及 core 合同、loot schema、mapgen anchor 的改动，仍建议在开工前各补一页简短 implementation note，确认最终字段与锚点没有漂移。
2. 实施时建议以本文档为主 source of truth，在 PR review 中引用对应章节编号（如”参见 OPT-PR-03 §3.3.2”）；若 implementation note 与本文档冲突，以修订后的本文档为准。
3. 每个 OPT-PR 完成后，更新本文档的”量化验收标准”段落，记录实际达到的数值。
4. Gate-1 通过后再决定是否执行 OPT-PR-06~07，或直接进入 Phase 5。
terrain 两个指标一旦被 `OPT-PR-06 / OPT-PR-07` 当作 before/after 门禁，固定消费：

1. `docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json`
2. `phase4Report` 中显式展示的 baseline provenance / delta / target rate

禁止后续 PR 直接在 `Phase4ReportRunner` 内写死绝对阈值而不更新 baseline artifact。
