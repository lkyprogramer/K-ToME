# Phase 4 深度审查报告 v4 — Part 1：执行摘要与一致性矩阵

- **审查日期**：2026-04-11
- **审查分支**：`codex/phase4-opt-pr-06-terrain-uptake-tuning`
- **审查人**：Claude Opus 4.6（资深 Roguelike 游戏总监 / 系统设计总监 / 玩法体验审查官）
- **审查对象**：Phase 4 四大优化 PR 结束态（opt PR-01 ~ opt PR-06）
- **证据源**：
  - 设计契约：`docs/phase4/*`、`docs/review/phase4/opt/PR/*.md`、`docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
  - 实现代码：`core/src/main/kotlin/com/ktome/core/mapgen/**`、`game/src/main/kotlin/com/ktome/game/elites/**`、`game/src/main/kotlin/com/ktome/game/hidden/**`、`game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`、`game/src/main/kotlin/com/ktome/game/GameModule.kt`
  - 内容数据：`game/src/main/resources/data/{zones,mapgen/zones,elites,loot,events,items,secret-zones}/index.yaml`
  - 白盒度量：`tools/build/reports/phase4/phase4-summary.md`（buildId `phase4-opt-pr05-dev`，生成时间 `2026-04-11T08:18:05Z`）
  - 最近开发后审：`docs/review/phase4/opt/2026-04-11-phase4-opt-pr-06-post-dev-review.md`

---

## 1. 一句话结论

> Phase 4 在 opt PR-01/02/03/04/05/06 完成度量基线、hidden 多样性、loot V3、elite 12 变体、terrain uptake 六层修复后，**全部 12 个 harness task 与 15 条 experience metric 均 PASS**；但这份"全绿"建立在 **只有 4/11 zone 真正进入 Phase 4 HybridTopology 管道、terrain 度量只覆盖 4/11 zone 且 greenwood_fringe 单区 encounter rate 11.4% 仍显著低于 16.36% 阈值** 之上。**骨架收尾合格，但 Phase 4 的内容闭环只对一半的地图成立**。

**发版判断**：**尚不可按"Phase 4 完结"交付**。建议立即开 opt PR-07 收尾 7 个 structural gap（见 Part 3 §3 清单）。

---

## 2. 审查方法

### 2.1 三层比对

本审查采用 **"设计契约 ↔ 代码实现 ↔ 运行时度量"** 的三层交叉比对方法，每一项结论必须至少由两个层面的证据共同支撑：

- **设计契约**（Contracts）：Phase 4 opt PR 01~06 的 PR 文档、Phase 4 总体规划、验证优化计划。
- **代码实现**（Implementation）：Kotlin 代码、YAML 数据、schema 定义。
- **运行时度量**（Runtime）：`phase4-summary.md`、baseline artifact、harness 原始 JSON。

### 2.2 审查镜头

不以"代码 reviewer"视角，而以 **游戏总监 + 玩法体验审查** 视角提问：

1. **玩家能不能感知到？**（有实现但玩家感知不到 = 未交付）
2. **失败路径可解吗？**（和"可通关"同样重要的是"失败时玩家理解原因吗"）
3. **同质化的天花板在哪里？**（12 个 mutation × 4 个 zone 的组合空间是否真的展开）
4. **是"通过 gate"还是"完成内容"？**（最小证据集 ≠ 完整闭环）
5. **该被 Phase 4 解决的问题是否被延后到 Phase 5？**

### 2.3 结论分级

- **P0**：阻塞 Phase 4 交付，必须在 Phase 4 内修复。
- **P1**：玩法体验受损但可通关，可在本周期末期或 Phase 4 正式收尾前修复。
- **P2**：后续迭代关注点，可延后，但必须登记。

---

## 3. 执行摘要：10 条关键结论

1. **Phase 4 HybridTopology 管道只生效于 4/11 个 zone**。`game/src/main/resources/data/zones/index.yaml` 里 11 个 zone 中只有 `greenwood_fringe`、`deep_iron_pit`、`underground_river`、`abyssal_temple` 配了 `mapgenProfileId: zone_mapgen.*.phase4`；其余 7 个（含 **强制主线 `shattered_outpost`（floor 1–3 起始区）、`grey_gate_depths`（floor 7–10 主线）、`abyssal_heart`（floor 15 终局）**）没有 profileId，按 `RoutedMapgenPipeline` 路由全部落入 `BspBackedMapgenPipeline` 兼容管道。这意味着玩家跑一局完整主线，至少 3 个 zone 拿不到 Phase 4 的 terrain painter / keyGate / hidden entrance / vault pool。**这是 Phase 4 设计-实现最大的一处落差，且未在任何既有 PR 审查文档中列为 P0**。

2. **terrain uptake 指标只覆盖 4 个 zone，其中还有一个是"不该覆盖的"**。`phase4-summary.md:282-362` 的 `terrainCoverageByZone` 只列了 `greenwood_fringe / deep_iron_pit / underground_river / crystal_cavern` 四条，**`abyssal_temple` 虽然配了 Phase 4 mapgenProfileId 却没有任何 terrain 采样数据**，而 `crystal_cavern` 没有 mapgenProfileId 却被纳入采样。terrainInteractionBatch 的 zone 选择与 Phase 4 迁移列表存在**双向漂移**。

3. **greenwood_fringe 区级 encounter rate 11.4%，显著低于 16.36% 目标**（`phase4-summary.md:290`）。当前聚合 16.6% 能 PASS 完全是靠 `underground_river` 25.3% 拉起来的。**这个"单区拉低、聚合掩盖"的结构让 opt PR-06 的 exit gate 在新手区域的玩家实际体验上并未兑现**。greenwood 是玩家进入 Phase 4 内容的第一个区，这个区的 11.4% 比全游戏任何数字都更决定"第一印象"。

4. **`preferredTerrainTagsSeen: ["WATER"]` — ICE 和 OIL 零命中**（`phase4-summary.md:379-381`）。尽管 `frostbound.preferredTerrainTags=[WATER, ICE]`、`emberblood/corrosion_cloud.preferredTerrainTags=[OIL]`、`zone_mapgen.deep_iron_pit.phase4.terrainTagWeights.OIL=0.32`、`underground_river.ICE=0.20` 都在 YAML 里存在，但 runtime 一次都没有把一个"有 ICE/OIL 偏好的 elite"放到一个 ICE/OIL tile 旁边。这说明 `PreferredTerrainAffinity` → `TerrainAwarePlacement` 的联动在 deep_iron_pit / crystal_cavern 完全没有产生增益（`preferredTerrainCombatCount = 0`），在 greenwood / underground_river 也只观察到 WATER 一个维度。

5. **opt PR-06 post-dev review 中的 P0-1/P0-2/P0-3 三个原 P0 修复全部已应用**：
   - P0-2 `mutationSelectionTags` 注入 `elite` 标签 ✅（`EncounterDecorationService.kt:158-168`）
   - P0-3 `failedExperienceMetricCount` 字段已在 summary 中出现并且 `=0` ✅（`phase4-summary.md:9`）
   - P0-1 encounter rate 聚合已达 16.6% ✅（但 §3.3 指出"聚合 pass 掩盖单区不达标"）
   
   **修复本身没有问题，问题在于修复覆盖率未达"所有 Phase 4 zone"的预期目标**。

6. **12 个 elite mutation、5 个 MutationKind、3 个 Boss variant 的 YAML 内容全部存在**（`game/src/main/resources/data/elites/index.yaml`），`bossHarness` 报告 `eliteMutationDistinctCount=12, eliteMutationValidPairCount=51` PASS。但 **`boss_variant.grey_crown.grantedMutations = [dread_aura, war_caller]` 两个 mutation 的 `preferredTerrainTags` 均为空**（`phase4-summary.md` 的 bossHarness 段 `[]`）。这意味着 Phase 4 终局区 `grey_gate_depths / abyssal_temple` 的两个 signature boss 之一 **完全没有 terrain 偏好**，这个战斗在 terrain axis 上退化为 Phase 3 水准。

7. **Loot V3 schemaVersion 3 架构完整、21 个 profile 覆盖 FIXED_LIST / TAG_WEIGHTED 两种策略**（`DataLoader.kt:1822-1843`）。9 种 EquipmentPassive kind 在 68 个有 passive 的 affix 中全部覆盖（`affixPassiveCoverage = 100%` PASS）。**但 `lootProfileMaxBaseItemOverlap=0.917` 的 sanity threshold 被设为 `< 0.95`，而真实冲突是在 `loot.abyssal_temple_warded_archive.secret ∩ loot.abyssal_temple.cadence = 0.818`（`phase4-summary.md:591`）**——即同 zone 的"秘密宝藏"和"普通宝藏"82% 物品重叠。玩家进了秘境只会发现"和外面差不多的东西"，**探索奖赏感直接崩塌**。

8. **`DamageVsTag` 被动只覆盖 2 个 tag（`bandit`、`undead`），共 6 条 affix**（基于 `items/index.yaml` 的 `kind: DamageVsTag` grep：6 行，tag 字段全部是 bandit/undead）。这意味着玩家构筑 "anti-orc/anti-cultist/anti-abyssal" 向的 gear 在 11 个 zone 中 9 个都没有反应。与 `EquipmentPassive` 设计意图（通过 tag-based 装备做阵营克制）严重不对称。

9. **Hidden content 覆盖率指标漂亮但路径多样性偏紧**：12 个 hidden event、5 个 secret zone、6 种 trigger type 全覆盖、3 种 entranceBindingId 全覆盖（`hiddenContentHarness` trigger rate 1.0, discovery rate 1.0）。但 **5 个 secret zone 中有 3 个共用 `hidden.branch`、1 个用 `hidden.critical.adjacent`、1 个用 `hidden.goal.adjacent`——entranceBindingId 的"分布"只是恰好覆盖了目标集合，而不是"每种 binding 都能被玩家在不同情境下遇到"**。覆盖达标 ≠ 设计兑现。

10. **Content Pack 基础设施 V3 schema fail-fast 路径已打通**（`DataLoader.kt:558-592`：loot_profile overlay 当 schemaVersion≠3 时抛 `content-pack.loot-profile.schema-version-mismatch`），`contentPackHarness` 13/13 PASS。但 **runtime overlay 只实现 ADD/REPLACE**（`DataLoader.kt:523-530`：APPEND/DENY 被直接拒绝），APPEND/DENY 的 lint 路径在运行时不生效。这条是 Phase 4 设计契约里的软承诺，**opt PR-04 的 README 并未将其标红**，但玩家/mod 作者的体感会是"为什么我的 append 规则 lint 通过但 runtime 报错"。

---

## 4. 一致性矩阵：设计契约 ↔ 实现 ↔ 运行时

表格共 18 条 claim，每条一行，列含义：

- **ID**：本审查自编号
- **领域**：mapgen / elite / loot / hidden / contentpack / terrain / boss
- **契约**：设计文档承诺的状态
- **实现**：当前代码/YAML 实际状态
- **运行时**：summary 或 harness 数据
- **Δ**：契约与实现的差距
- **严重度**：P0 / P1 / P2
- **证据**：文件路径 + 行号或 metric 名

| ID | 领域 | 契约 | 实现 | 运行时 | Δ | 级别 | 证据 |
|---|---|---|---|---|---|---|---|
| CI-01 | mapgen | "所有 Phase 4 zone 通过 HybridTopology 管道生成" | 仅 4/11 zone 配 `mapgenProfileId` | 7 个 zone 落入 BSP 兼容管道 | 缺失 7 条 profile，包括 3 个强制主线 zone | **P0** | `zones/index.yaml:2,47,69,115,137,181,226`（无 mapgenProfileId）/ `GameModule.kt:723`（缺 profile 跳过 guaranteed elite） |
| CI-02 | terrain | "terrain uptake 指标覆盖所有 Phase 4 zone" | terrainInteractionBatch zone 列表 = {greenwood, deep_iron, underground_river, crystal_cavern} | `abyssal_temple` 无采样、`crystal_cavern` 被采但无 mapgenProfileId | 覆盖列表与 Phase 4 迁移列表双向漂移 | **P0** | `phase4-summary.md:282-362`（zoneSummary 只 4 项） |
| CI-03 | terrain | "每个 Phase 4 zone 的 encounter rate ≥ baseline×1.30 = 16.36%" | 聚合 16.6% PASS | **greenwood_fringe 单区 11.4%**，deep_iron_pit 27.3%, underground_river 25.3%, crystal_cavern 20.0% | 新手区单区不达标；聚合通过的"掩盖效应" | **P0** | `phase4-summary.md:290`（greenwood 11.4%） |
| CI-04 | terrain | "WATER/ICE/OIL 三种 tag 都能触发 preferredTerrainTags 联动" | frostbound/emberblood/corrosion_cloud 的 preferredTerrainTags 覆盖 ICE/OIL | `preferredTerrainTagsSeen: ["WATER"]`, `deep_iron_pit.preferredTerrainCombatCount=0`, `crystal_cavern.preferredTerrainCombatCount=0` | ICE/OIL 的 affinity 路径实际 **未被任何 runtime 事件触发** | **P0** | `phase4-summary.md:379-381, 307, 346` |
| CI-05 | loot | "secret loot 与 normal loot 身份差异显著" | `loot.abyssal_temple.cadence` 与 `loot.abyssal_temple_warded_archive.secret` 物品重叠 0.818 | max 0.917, average 0.205 | sanity threshold `< 0.95` 过于宽松，掩盖区内"身份一致" | **P0** | `phase4-summary.md:591` / threshold 定义 `metric:lootProfileMaxBaseItemOverlap` |
| CI-06 | elite | "Elite mutation `applyToTags` 保留 forge/river/crystal 的针对性" | opt PR-06 修复给 selection context 无条件注入 `elite`，所有 mutation 的 applyToTags 都含 `elite`，针对性过滤退化为"只要是 elite 都能挂" | frostbound (`[elite,river,crystal]`) 在 greenwood bandit 身上被挂载 | 标签约束实质丢失；原来"forge 区才有 forge mutation"的设计意图稀释 | **P1** | `EncounterDecorationService.kt:160-168` / `elites/index.yaml:174`（frostbound applyToTags） |
| CI-07 | boss | "Boss variant 通过 `preferredTerrainTags` 获得 terrain flavor" | grey_crown 的两个 granted mutation (dread_aura, war_caller) 的 `preferredTerrainTags=[]` | bossHarness `grey_crown=[]` | 终局 Boss 之一 terrain 为空 | **P1** | `elites/index.yaml:82,130`（两条 empty） |
| CI-08 | item | "`DamageVsTag` 覆盖主要阵营" | 仅 `bandit`×4 + `undead`×2 共 6 条 affix | 9 个 zone 的阵营（orc/cultist/forge/crystal/abyssal/river/…）无对应 anti-tag 选项 | 阵营克制链条 80% 空白 | **P1** | `items/index.yaml` grep `kind: DamageVsTag`（见 Part 3 附录） |
| CI-09 | hidden | "5 个 secret zone 的 entranceBindingId 分散在 hidden.branch/critical.adjacent/goal.adjacent" | 3 个用 hidden.branch，1 个用 critical.adjacent，1 个用 goal.adjacent | secretEntranceBindingCoverage=3 PASS | 分布刚好满足 ≥3，但 branch 过于集中 | **P2** | `secret-zones/index.yaml:24,48,74,101,125` |
| CI-10 | hidden | "Secret zone 路径有 1/2/3 步多种深度" | 1 步 (greenwood) / 2 步 (deep_iron_slag, deep_iron_smuggler, abyssal_temple) / 3 步 (underground_river_crystal_rift) | hiddenContentHarness 1.0 trigger rate PASS | 结构正确 | — | `events/index.yaml` |
| CI-11 | contentpack | "overlay 支持 ADD/REPLACE/APPEND/DENY 全 op" | runtime 只实现 ADD/REPLACE，APPEND/DENY 抛异常 | contentPackHarness 13/13 PASS（仅测 ADD/REPLACE） | 设计承诺 2/4 op 未在 runtime 兑现 | **P2** | `DataLoader.kt:523-530` |
| CI-12 | contentpack | "loot profile overlay 强制 schemaVersion=3" | `LOOT_PROFILE_SCHEMA_VERSION=3` fail-fast | contentPackHarness V2 schema 正确拒绝 | 已兑现 | — | `DataLoader.kt:566-581` |
| CI-13 | mapgen | "hidden entrance 通过 NodeAnchorId 绑定" | HiddenContentMapgenPipeline 校验 bindingId == entranceAnchorId | solvabilityHarness 4 个 discovery tag / 3 个 anchor family | 已兑现 | — | `HiddenContentMapgenPipeline.kt:24` |
| CI-14 | elite | "maxMutationsPerElite=2 冻结" | YAML `eliteConfig.maxMutationsPerElite: 2` | bossHarness valid pair 51 | 已兑现 | — | `elites/index.yaml:2` |
| CI-15 | loot | "每种 passive kind 都有 affix 覆盖" | 9 种 kind 全覆盖 | affixPassiveCoverage=100% | 已兑现 | — | `items/index.yaml` grep `kind:` |
| CI-16 | loot | "20 unique + 8 artifact + ≥75 affix" | 20 unique, 8 artifact, 106 affix | totalLootContentCount=106 PASS | 已兑现 | — | `items/index.yaml:1367,1618` |
| CI-17 | experience | "失败指标计入 taskFailed" | `failedExperienceMetricCount` 在 summary 中列出为字段 | `failedExperienceMetricCount=0` | opt PR-06 P0-3 已应用 | — | `phase4-summary.md:9` |
| CI-18 | mapgen | "Phase 4 的 guaranteed elite 在 mandatory zone 出现" | `GameModule.kt:723`：`if (zone.mapgenProfileId == null) return null` | 7 zone 无 mapgenProfile = 7 zone 无 guaranteed elite 保底 | 和 CI-01 耦合：boss/elite/threat budget 在缺失 profile 的 zone 全部失效 | **P0** | `GameModule.kt:723` |

**矩阵统计**：18 条总计 6 P0 + 3 P1 + 2 P2 + 7 已兑现。

---

## 5. 快速可视化：P0 路径图

```
玩家进入新一局
    │
    ▼
floor 1-3 shattered_outpost [无 mapgenProfileId → BSP 兼容管道]
    │  └── Phase 4 特性全部缺席：无 terrain painter、无 keyGate、无 hidden
    │
    ▼
floor 4-6 greenwood_fringe [HybridTopology ✅ / terrain 11.4% ❌]
    │  └── encounter rate 低于目标，首印象弱化
    │
    ▼
floor 7-10 grey_gate_depths [无 mapgenProfileId → BSP 兼容管道]
    │  └── 主线区缺 Phase 4 特性
    │
    ▼
floor 11-13 deep_iron_pit [HybridTopology ✅ / preferredTerrain=0 ⚠️ / combatCount 采样偏少]
    │
    ▼
floor 14 underground_river [HybridTopology ✅ / 25.3% ✅] （唯一全绿区）
    │
    ▼
floor 15 abyssal_heart [无 mapgenProfileId → BSP 兼容管道]
       └── 终局区缺 Phase 4 特性
```

**结论**：Phase 4 Hybrid 管道实际生效的"玩家路径覆盖率" ≈ 3/15 层（greenwood + deep_iron + underground_river，其中 deep_iron 还有 preferredTerrain=0 的次级问题），**低于 25%**。

---

## 6. 与既有 PR 审查文档的差异

将本审查的结论与最权威的两篇既有审查做对齐：

### 6.1 vs `2026-04-11-phase4-opt-pr-06-post-dev-review.md`

| 既有审查的 P0 | 本审查的判断 |
|---|---|
| P0-1 encounter rate 12.1%（聚合不达 16.36%） | **已修复** — 当前聚合 16.6% PASS，但 **greenwood 单区 11.4% 仍未解决**，升级为新的 **CI-03 P0** |
| P0-2 preferredTerrainCombatCount=0（ELITE tag 未注入） | **已修复** — `mutationSelectionTags` 注入 `elite`；**但 deep_iron_pit / crystal_cavern 的 preferredTerrainCombatCount 仍为 0**，病根从"标签不匹配"变成"zone 要么无 mapgenProfile 要么采样不足"，升级为新的 **CI-04 P0** |
| P0-3 failedTaskCount 不跟 experience metric 联动 | **已修复** — `failedExperienceMetricCount` 字段已独立存在 |

### 6.2 vs `2026-04-08-phase4-verified-optimization-pr-plan.md`

- opt PR 01~06 的 exit gate（affix ≥75, unique ≥20, artifact ≥8, 12 mutation, 3 boss variant, hidden×12, secret×5, terrain exposure +50%, encounter +30%）**逐条核对全部 PASS**。
- **但该计划未把"11 个 zone 全部进入 Phase 4 管道"写成 exit gate**，导致 CI-01 在所有已有审查视野外 "合规地漏掉"。这是 Phase 4 opt plan 设计的一处漏洞：**只盯度量不盯 scope 覆盖**。

### 6.3 vs 更早的 `phase4_opt_deep_review_claude_v4_part1.md`（位于 `docs/review/phase4/`，非本 v2 目录）

那份文件基于较旧的 Phase 4 snapshot（mutation 只有 6 个、loot 未升级到 V3），与当前 tree 状态存在显著差异。本 v2 报告完全按 2026-04-11 当前 tree 重新得出结论，建议将旧 v1 报告标记为 **historical**，以 v2 为准。

---

## 7. Part 1 终评

- **功能面**：**A−**（框架齐、harness 齐、V3 schema fail-fast 齐）
- **覆盖面**：**C+**（4/11 zone 进 Phase 4 管道；度量口径与迁移列表双向漂移）
- **玩家体感面**：**C**（首印象区 11.4% 不达标、secret loot 身份度差、DamageVsTag 阵营克制几乎空白）
- **验收合规面（按既有 gate）**：**通过**
- **设计兑现合规面（按 Phase 4 "全 zone 覆盖" 隐性目标）**：**未达标**

→ **总评**：**Phase 4 打到了 gate，但没打到目标。建议 opt PR-07 作为收尾 PR，以 CI-01/02/03/04/05 为任务清单**。

**Part 2（玩法体验总评）、Part 3（P0 清单与优化建议）、Part 4（延后问题与最终结论）见独立文件**。
