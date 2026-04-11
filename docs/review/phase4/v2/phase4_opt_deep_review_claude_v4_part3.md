# Phase 4 深度审查报告 v4 — Part 3：Phase 4 必须解决的问题 + 可执行优化建议

- **审查日期**：2026-04-11
- **审查分支**：`codex/phase4-opt-pr-06-terrain-uptake-tuning`
- **关联文档**：Part 1（一致性矩阵 CI-01~CI-18）/ Part 2（玩法体验总评）/ Part 4（延后问题与最终结论）

本部分的结构：
- **§1**：Phase 4 **必须** 解决的问题清单（基于 CI 矩阵的 P0 条目）
- **§2**：Phase 4 **应当** 解决的问题清单（P1 条目 + 系统耦合层修复）
- **§3**：可执行优化建议（按 P0 → P1 → P2 分层，每条含"影响文件"、"改动类型"、"验证方式"）
- **§4**：opt PR-07 建议任务清单（将 P0/P1 打包成一轮收尾 PR）
- **§5**：附录 A — DamageVsTag affix 全量清单（证据支撑）

---

## 1. Phase 4 必须解决的问题（P0 清单）

### 1.1 P0-A：mapgenProfileId 覆盖不全（CI-01 + CI-18）

**问题定义**：
`game/src/main/resources/data/zones/index.yaml` 的 11 个 zone 中只有 4 个配了 `mapgenProfileId: zone_mapgen.*.phase4`：`greenwood_fringe / deep_iron_pit / underground_river / abyssal_temple`。其余 7 个（**含 3 个强制主线 `shattered_outpost / grey_gate_depths / abyssal_heart`**）未配 profileId，根据 `RoutedMapgenPipeline` 路由全部落入 `BspBackedMapgenPipeline` 兼容管道，不具备 Phase 4 的 terrain painter、key gate plans、hidden entrance plans、vault pool、roomTagFilter 等核心能力。此外，`GameModule.kt:723` 有一段 `if (zone.mapgenProfileId == null) return null`，意味着这 7 个 zone 的 guaranteed elite 保底也一并跳过。

**影响**：
- 玩家主线一共经过 15 层，其中 shattered_outpost(1–3) + grey_gate_depths(7–10) + abyssal_heart(15) 共 **8 层**没有 Phase 4 特性，占比约 **53%**
- Phase 4 "全地图探险"的设计承诺在主线关键节点失效
- CI-18 并列揭示：这 7 个 zone 也丢失 guaranteed elite 保底，意味着**整局 elite 遭遇频率会比预期低得多**，直接影响 elite mutation / boss variant 系统的触发样本量

**必须在 Phase 4 内修复的原因**：
- 不是"内容量扩张"任务，而是"已有管道接上剩余 zone"的**结构性补齐**——推迟到 Phase 5 等同于承认 Phase 4 只对 4/11 zone 生效
- 至少 3 个强制主线区（shattered_outpost、grey_gate_depths、abyssal_heart）是玩家必经，**任何一个区的 Phase 4 含量缺失都直接破坏玩家的体验曲线**

**验收标准**：
- 11 个 zone 全部有 `mapgenProfileId`（或明确标记某些 optional zone 保留 BSP 并在文档中备案）
- 至少 3 个强制区必须进入 HybridTopology 管道
- `phase4-summary.md` 的 zoneSummary 至少覆盖所有带 profileId 的 zone

### 1.2 P0-B：terrain 指标的 zone 覆盖漂移（CI-02）

**问题定义**：
`tools/build/reports/phase4/phase4-summary.md:282-362` 的 `terrainCoverageByZone` 只列了 4 个 zone：`greenwood_fringe / deep_iron_pit / underground_river / crystal_cavern`。

**双向漂移**：
- **abyssal_temple**：**有** mapgenProfileId，是 Phase 4 迁移区，**却没有 terrain 采样数据**
- **crystal_cavern**：**没有** mapgenProfileId，是 Phase 3 兼容区，**却被 terrainInteractionBatch 采样**

这说明 `terrainInteractionBatch` 的 zone 选择**硬编码或配置错误**，与 zones/index.yaml 的 `mapgenProfileId` 列表不同步。

**必须在 Phase 4 内修复的原因**：
- 度量口径与 scope 定义不一致，意味着 Phase 4 的 "exit gate 合规" 本身是 **基于错误样本集** 得出的。修复 P0-A 后，P0-B 必须同步，否则新增的 zone 不会被验证
- 自动化回归的可信度决定未来每一轮 opt PR 是否能安心前进

**验收标准**：
- terrainInteractionBatch 的 zone 选择由 `mapgenProfileId` 派生，不再硬编码
- 度量运行后 zoneSummary 覆盖 = "所有带 profileId 的 zone"
- 加一条 assertion：zone 数量与 Phase 4 zone list 相等

### 1.3 P0-C：greenwood_fringe 单区 encounter rate 不达标（CI-03）

**问题定义**：
`phase4-summary.md:290` 显示 greenwood_fringe 的 `terrainInteractionEncounterRate = 0.11398963730569948`（11.4%），**显著低于 opt PR-06 exit gate 的 16.36% 目标**。当前聚合率 16.6% PASS 完全依靠 `underground_river = 25.3%` 把均值拉起来。

**根因假设**（按概率排序）：
1. **greenwood_fringe 的 elitePools = [bandit.sentry, bandit.wild_huntmaster]** 两个都是纯物理 bandit monster，**没有任何 fire/cold/lightning 能力**——即使站在 water tile 边，也没法主动触发 `terrain_cold_water_freeze`；这条规则只能由玩家发起（见 Part 2 §2.1）
2. `triggeredRuleIds: ["terrain_cold_water_freeze"]` 只有 1 条，而 deep_iron_pit / underground_river / crystal_cavern 都有 2-3 条 rule
3. greenwood 的 terrain 组成是纯 WATER 0.44（无 ICE / OIL），能触发的 rule 天生只有"和 water 相关的"那一两条

**必须在 Phase 4 内修复的原因**：
- greenwood 是玩家进入 Phase 4 的**第一个**区，"第一印象"决定玩家是否愿意继续推进
- Aggregate 能 pass 纯属结构性幸运，下一次 tuning 一旦 underground_river 回落（比如改了 terrain weight），aggregate 立刻变红
- 这是 "聚合掩盖单区不达标" 的典型案例，修它也就同时修正 gate 口径的设计缺陷

**验收标准**：
- greenwood_fringe 单区 encounter rate ≥ 16.36% 或 ≥ (baseline × 1.30)
- **并且** `triggeredRuleIds` 至少 2 条（引入第二条能被 bandit npc 主动触发的 rule）

### 1.4 P0-D：preferredTerrain 联动在 3/4 zone 未观测到（CI-04）

**问题定义**：
`phase4-summary.md:379-381` 显示 `preferredTerrainTagsSeen: ["WATER"]`——**ICE 和 OIL 在任何 runtime 事件中一次都没被匹配**。虽然 frostbound 的 `preferredTerrainTags=[WATER, ICE]`、emberblood / corrosion_cloud 的 `preferredTerrainTags=[OIL]`、`zone_mapgen.deep_iron_pit.phase4.terrainTagWeights.OIL=0.32`、`zone_mapgen.underground_river.phase4.terrainTagWeights.ICE=0.20` 都在 YAML 里，但数据流 **Mutation → PreferredTerrainAffinity → TerrainAwarePlacement → 战斗位置** 没有把 ICE/OIL 的 elite 放到 ICE/OIL tile 附近。

**病因分解**：
- **deep_iron_pit preferredTerrainCombatCount=0**：combatCount=43 样本偏小 + OIL-preferring mutation 权重不足（分析见 Part 2 §5.1 点 A/B）
- **crystal_cavern preferredTerrainCombatCount=0**：根因是 CI-01（无 mapgenProfileId），OIL/ICE tile 本来就没被 Phase 4 terrain painter 画出来；随 P0-A 修复会自动缓解
- **underground_river 只观察到 WATER**：frostbound 的 `preferredTerrainTags=[WATER, ICE]` 双 tag，但 `TerrainAwarePlacement` 可能按 **任一 tag 匹配** 就计数为 "WATER"（或者 ICE tile 数量太少）—— 需要查证 `TerrainAwarePlacement` 的放置逻辑与度量口径

**必须在 Phase 4 内修复的原因**：
- 这个链条是 opt PR-06 的核心 "tactical uptake" 卖点；如果 ICE 和 OIL 从未被触发，**opt PR-06 的设计承诺在 2/3 维度上完全未兑现**
- 修完后的长期回归依赖度量正确性（preferredTerrainTagsSeen 应覆盖 WATER/ICE/OIL）

**验收标准**：
- `preferredTerrainTagsSeen` 至少覆盖 {WATER, ICE, OIL} 全集
- 所有带 mapgenProfileId 的 zone 的 `preferredTerrainCombatCount > 0`
- 加一条 experience metric：`preferredTerrainTagDistinctCount ≥ 3`

### 1.5 P0-E：secret loot 与 normal loot 身份重叠（CI-05）

**问题定义**：
`phase4-summary.md:591` 显示 `loot.abyssal_temple.cadence ∩ loot.abyssal_temple_warded_archive.secret = 0.818`——同 zone 的秘境宝藏和普通宝藏物品池 82% 重叠。max 0.917 的 sanity threshold `<0.95` 过宽，导致这个"身份灾难"合规通过。

**玩家感知**：冒险进入 secret zone 后，8/10 的 loot 是走主路也能拿到的东西。秘境奖赏感完全崩塌。

**必须在 Phase 4 内修复的原因**：
- 秘境是 Phase 4 opt PR-05 最重要的体验卖点之一
- 数据不对称："secret_profile 有 specialTemplateTagPreference 和 affixTagPreference"是 V3 schema 的独特能力，当前没有实际发挥作用
- 低成本修复：调整 5 个 secret profile 的 itemIds 或 typeWeights，让它们**优先/必选 unique/artifact**

**验收标准**：
- 所有 5 个 secret profile 与同 zone normal profile 的物品重叠 ≤ 0.50
- sanity threshold 从 `< 0.95` 收紧到 `< 0.70`
- 新增 metric：`secretProfileIdentityOverlap` 单列

### 1.6 P0 汇总表

| ID | 问题简述 | 主要受影响 zone/系统 | 修复难度 | 修复时间估算（人日等价） |
|---|---|---|---|---|
| P0-A | mapgenProfileId 覆盖不全 | 7 zone / 核心 mapgen | 中 | 2~3 天（需写 7 条 profile + 验证）|
| P0-B | terrain 度量 zone 漂移 | terrainInteractionBatch | 低 | 0.5 天 |
| P0-C | greenwood encounter 11.4% | greenwood_fringe | 中 | 1~2 天（引入新 elite + 新 rule）|
| P0-D | preferredTerrain 联动断链 | deep_iron_pit + underground_river + crystal_cavern | 中 | 1~2 天（依赖 P0-A 修完后重测）|
| P0-E | secret loot 身份崩塌 | 5 secret profile | 低 | 0.5~1 天 |

**合计**：约 5~8.5 人日。

---

## 2. Phase 4 应当解决的问题（P1 清单）

### 2.1 P1-A：applyToTags 针对性被 `elite` 稀释（CI-06）

**问题定义**：
opt PR-06 post-dev fix 在 `EncounterDecorationService.mutationSelectionTags`（`game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt:158-168`）给 selection context **无条件注入 `elite`** 标签。这修复了 "bandit.sentry 拿不到任何 mutation" 的 P0-2，但副作用是：**所有 12 个 mutation 的 applyToTags 都含 `elite`，结果任何 elite-eligible 怪物都可以拿任何 mutation**。原本 YAML 里 `applyToTags: [elite, forge, abyssal]` 这种"定向分发"的设计意图基本失效——现在 greenwood 的 bandit elite 理论上可以挂上 `elite.corrosion_cloud`（尽管还受 `allowedZones` 约束）。

**影响**：
- 玩法感：elite mutation 的"flavor 与所处 biome 吻合"这一体验属性被稀释
- 系统耦合：`allowedZones` 成了最后一道防线，如果某个 mutation 的 `allowedZones` 为空（见 battle_drill / hunt_protocol / phase_runner / war_caller 共 4 条），它就**完全没有 zone 约束**，在任何 zone 都能挂

**可选修复方案**：
- **方案 A（最小改动）**：不碰 mutationSelectionTags，但在 EliteMutationRegistry.select() 里加一条隐性规则——如果 mutation 的 applyToTags 包含非 `elite` 的任何具体 tag（forge / abyssal / bandit / …），**那些具体 tag 必须被 context 覆盖**。这样 elite 注入只作为"保底匹配"，不替代具体 tag 的针对性
- **方案 B（更显式）**：重命名 `applyToTags` → `eligibleTags`，并要求 mutation yaml 必须声明至少一个非 `elite` 的 eligible tag；模板 tag 集合必须至少覆盖一个
- **方案 C（配置驱动）**：保留现状，但给没 `allowedZones` 的 mutation 强制补上默认 zone 白名单

**推荐**：方案 A + 少量 yaml 补白（给 4 条 allowedZones 为空的 mutation 补白名单）。

### 2.2 P1-B：grey_crown boss variant 无 terrain flavor（CI-07）

**问题定义**：
`boss_variant.grey_crown` 的 `grantedMutations = [dread_aura, war_caller]`，两者的 `preferredTerrainTags` 均为空。这意味着 grey_gate_depths 的 signature boss variant 在 terrain 轴上完全没有特征，和 Phase 3 boss 无差别。叠加 CI-01（grey_gate_depths 本身就没有 mapgenProfileId），这一战是 Phase 4 中盘完全失去 Phase 4 含量的关键 encounter。

**修复方案**：
- 给 `elite.dread_aura` 加 `preferredTerrainTags: [OIL]` 或 `[ICE]`（搭配 grey_gate_depths 的 biome family）
- 或者，在 `boss_variant.grey_crown` 里独立声明 `preferredTerrainTags`（需要 schema 扩展）
- 同时修完 CI-01 让 grey_gate_depths 进入 Phase 4 管道后，画 terrain tag

### 2.3 P1-C：DamageVsTag 阵营克制空白（CI-08）

**问题定义**：
`items/index.yaml` 中 `kind: DamageVsTag` 出现 **共 6 次**，tag 字段全部是 `bandit`（4 条）或 `undead`（2 条）。这意味着玩家可以构筑的 "anti-faction" 装备只针对 2 个阵营，而 Phase 4 涉及的主要阵营至少有 `bandit / undead / orc / cultist / forge / river / crystal / abyssal`（8 个）。

**数据证据（见 §5 附录）**：

```
行 1056: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.12 }
行 1073: { kind: DamageVsTag, tag: undead, bonusPercent: 0.12 }
行 1751: { kind: DamageVsTag, tag: undead, bonusPercent: 0.10 }
行 1926: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.10 }
行 1995: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.15 }
行 2161: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.12 }
```

**修复方案**：
- 新增 6 条 affix，每个空缺阵营（orc / cultist / forge / crystal / abyssal / river）至少 1 条
- 优先 PREFIX/SUFFIX 各半，tier 2，cost 6，minFloor 按阵营首次出现区对齐

### 2.4 P1-D：PreferredTerrainAffinity → TerrainAwarePlacement 的联动权重偏弱

Part 2 §5.1 指出 `PREFERRED_TERRAIN_WEIGHT_BONUS = 4`（`MutationModels.kt:200`）相对 MAJOR base 7 只是 +57%，容易被池子稀释。建议：
- 把 bonus 提到 **+6 或 +8**，让有 terrain 偏好的 mutation 在对应 zone 成为主导
- 或者改为乘法加成：base × 1.5

### 2.5 P1-E：terrain interaction rule 不足

当前度量观察到的 `triggeredRuleIds` 集合：`terrain_cold_water_freeze / terrain_fire_oil_ignite / terrain_fire_ice_melt / terrain_physical_ice_slip`（4 条）。建议补 1-2 条低风险规则：
- `terrain_lightning_water_chain`：雷电攻击击中 water tile 链式传导
- `terrain_fire_water_steam`：火+水生成 1 turn 的 mist 阻挡视线

**低成本高回报**：新规则只需在 core terrain rule registry 加条目 + 给 bandit/orc monster template 各补一个能发动的 talent。

---

## 3. 可执行优化建议（按优先级排列）

每条含：**变更点**、**影响文件**、**改动类型**、**验证方式**、**预期 metric 变化**。

### 3.1 P0 级建议

#### R-P0-1：为 7 个缺失 zone 补 `mapgenProfileId`

**变更点**：
- `game/src/main/resources/data/mapgen/zones/index.yaml` 中新增 7 条 `zoneMapgenProfiles` 条目
- `game/src/main/resources/data/zones/index.yaml` 中 7 个 zone 加上 `mapgenProfileId: zone_mapgen.*.phase4`

**影响文件**：
- `game/src/main/resources/data/mapgen/zones/index.yaml`（新增条目）
- `game/src/main/resources/data/zones/index.yaml`（改每个 zone）
- `game/src/main/resources/data/zones/index.yaml` 的 `zoneRewardProfiles` 补 7 条（可复用现有模板）
- 可能需要 `game/src/main/resources/data/mapgen/biome-families/*` 中补新 family（shattered_outpost 需要 "outpost/ruin" family，grey_gate_depths 需要 "gate/shrine" family 等）

**改动类型**：YAML 数据新增 + 无代码改动（若 family 已存在）

**验证方式**：
- `mapgenSmoke` 运行后 504 个 case 扩展到更多（含新 zone）不失败
- `solvabilityHarness` 对新 zone 的 critical path 100% 成功
- Phase 4 summary zoneSummary 覆盖从 4 → 11

**预期变化**：
- greenwood 仍是唯一 terrain 待优化区，deep_iron 和 underground_river 继续 pass，abyssal_temple / grey_gate / shattered_outpost / abyssal_heart 开始贡献数据

**风险**：shattered_outpost (floor 1-3) 如果接入 Phase 4 管道可能增加早期难度——需要在 zoneRewardProfile 调参保持新手友好

#### R-P0-2：修复 terrainInteractionBatch zone 选择

**变更点**：
- 找到 `terrainInteractionBatch` 的 zone 选择代码（`tools/src/main/kotlin/com/ktome/tools/phase4/` 相关文件）
- 将 zone 列表改为从 `gameContent.zones.filter { it.mapgenProfileId != null }` 派生
- 加 assertion：`zoneSummary.size == migratedZoneIds.size`

**影响文件**：
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`（根据现有结构）
- `tools/src/main/kotlin/com/ktome/tools/phase4/TerrainInteractionBatch.kt`（或同名 harness 文件）

**改动类型**：Kotlin 逻辑修改（移除硬编码 zone 列表）

**验证方式**：
- 单元测试：断言 zoneSummary.keys == Phase 4 zone list
- 运行 `phase4Report` task 后 summary 的 zoneSummary 覆盖与 R-P0-1 后的 zone list 一致

**预期变化**：
- crystal_cavern（如果不进入 Phase 4 管道）从 zoneSummary 中消失
- abyssal_temple 出现在 zoneSummary 中

#### R-P0-3：修复 greenwood_fringe encounter rate

**三种路径**（可组合）：
1. **给 greenwood elite pool 加一个 "能烧水 / 冻水" 的 template**：
   - 新增 `bandit.pyro_marauder`（tier 1，火焰能力 + perceptions），或
   - 从现有 template 中挑一个 reskin 为 greenwood-eligible
2. **新增一条新 terrain rule**（见 P1-E）：`terrain_physical_water_splash`（重击 water tile 形成短暂减速区域），让 bandit 的物理攻击也能触发 terrain
3. **提高 `zone_mapgen.greenwood_fringe.phase4.terrainTagWeights.WATER` 的权重 0.44 → 0.55**，让地形出现更密集

**影响文件**：
- `game/src/main/resources/data/monsters/index.yaml`（新增或修改 template）
- `core/src/main/kotlin/com/ktome/core/combat/terrain/TerrainRuleRegistry.kt`（或同类文件，新增 rule）
- `game/src/main/resources/data/mapgen/zones/index.yaml`（调 terrainTagWeights）

**验证方式**：
- `phase4-summary.md` 中 `greenwood_fringe.terrainInteractionEncounterRate ≥ 0.1636`
- `triggeredRuleIds` for greenwood 至少 2 条
- `bossHarness` 的 greenwood boss 仍 PASS（不被新 rule 破坏）

**预期变化**：
- greenwood 单区 encounter rate：11.4% → ≥16.4%
- aggregate 率保持或提升

#### R-P0-4：修 preferredTerrain 联动覆盖率

**依赖**：R-P0-1 完成后

**变更点**：
- `MutationModels.kt:200` 的 `PREFERRED_TERRAIN_WEIGHT_BONUS: Int = 4` → **改为 6**（或 8）
- 检查 `TerrainAwarePlacement` 对 preferredTerrainTags 的放置算法，确保多 tag 时每个 tag 都有机会被匹配
- 补一条 metric：`preferredTerrainTagDistinctCount`（观察到几种不同 terrain tag）

**影响文件**：
- `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt`
- `core/src/main/kotlin/com/ktome/core/mapgen/TerrainAwarePlacement.kt`（如存在）
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`（新增 metric gate）

**验证方式**：
- `preferredTerrainTagsSeen` 覆盖 {WATER, ICE, OIL}
- 所有带 mapgenProfileId 的 zone 的 `preferredTerrainCombatCount > 0`

**预期变化**：
- deep_iron_pit 从 0 → ≥30%（OIL）
- underground_river 从只有 WATER 变为 {WATER, ICE}
- abyssal_temple 第一次出现 preferredTerrain 数据（受 R-P0-2 影响）

#### R-P0-5：secret loot 身份差异化

**变更点**：
对 5 个 secret profile 的 itemIds / typeWeights 做重写，让它们**优先/必选 unique 或 artifact**，并从 itemIds 列表中**移除与同 zone normal profile 重复的 base item**。

例（以 `loot.abyssal_temple_warded_archive.secret` 为例）：
```yaml
- id: loot.abyssal_temple_warded_archive.secret
  schemaVersion: 3
  tags: [secret, abyssal_temple]
  rewardBudget: 3
  poolStrategy: FIXED_LIST
  itemIds: []   # ← 改为空或不引用 base
  typeWeights: {}
  slotBias: {}
  specialTemplateTagPreference: [abyssal_temple, warded_archive, special_chest]
  affixTagPreference: [shadow, holy, anti_undead]
  # 新增 guaranteed unique/artifact
  guaranteedSpecialTemplateIds: [artifact.eclipsed_relic]
  guaranteedUniqueIds: [unique.voidlit_seal]
```

**影响文件**：
- `game/src/main/resources/data/loot/index.yaml`（改 5 条 secret profile）
- 可能需要 `LootProfileSchemaV3` 增加 `guaranteedSpecialTemplateIds` / `guaranteedUniqueIds` 字段
- `LootPoolStrategy` 的 resolver 逻辑需要处理 guaranteed drop

**验证方式**：
- `lootProfileBaseItemOverlapMatrix` 中 secret ∩ normal 的任意一对 ≤ 0.50
- `lootBalanceLab` 不报错
- 玩家实际 run：secret zone drop 必含 artifact/unique

**预期变化**：
- max overlap：0.917 → ≤0.70
- 新增 metric `secretProfileIdentityOverlap` 分布清晰

### 3.2 P1 级建议

#### R-P1-1：applyToTags 针对性保护（对应 P1-A）

**变更点**：
在 `EliteMutationRegistry.select()` 里，过滤逻辑从：

```kotlin
context.applyToTags.any(definition.applyToTags::contains)
```

改为：

```kotlin
val nonEliteTags = definition.applyToTags - "elite"
if (nonEliteTags.isEmpty()) {
  true  // mutation 是"泛用"型（如 stonehide/ironhide），elite tag 即可匹配
} else {
  context.applyToTags.any(nonEliteTags::contains) || 
    // 保留最小保底：context 含 elite 且 mutation 的 applyToTags 也含 elite
    (definition.applyToTags.contains("elite") && context.applyToTags.contains("elite"))
}
```

**变更风险**：会减少部分 zone 的 mutation 选择空间——建议先跑 `bossHarness` 确认 `eliteMutationValidPairCount` 仍 ≥40。

#### R-P1-2：grey_crown boss 地形 flavor

**变更点**：`game/src/main/resources/data/elites/index.yaml` 给 `elite.dread_aura` 加 `preferredTerrainTags: [OIL]`；同时检查是否需要让 grey_gate_depths 的 biome family 支持 OIL tag。

#### R-P1-3：DamageVsTag 阵营补齐

**变更点**：`game/src/main/resources/data/items/index.yaml` 新增 6 条 affix：
- `cultist_smiter`（PREFIX weapon，anti-cultist）
- `orcslayer`（PREFIX weapon，anti-orc）
- `of_forge_breaker`（SUFFIX weapon，anti-forge）
- `of_crystal_shatter`（SUFFIX weapon，anti-crystal）
- `of_abyssal_ward`（SUFFIX armor，anti-abyssal）
- `river_stalker`（PREFIX weapon，anti-river）

每条 tier 2, cost 6, minFloor 按首次出现区对齐。

#### R-P1-4 / R-P1-5：详见 §2.4 / §2.5

### 3.3 P2 级建议

- **R-P2-1**：扩展 content pack runtime 支持 APPEND / DENY 运算（目前只支持 ADD / REPLACE）
- **R-P2-2**：hidden secret zone 的 entranceBindingId 分散到更多 binding 家族（目前 3/5 用 hidden.branch）
- **R-P2-3**：扩展 hidden content primer 的 UX hint（玩家怎么知道 slag_cache 的 primer 是哪个 elite）
- **R-P2-4**：artifact 的 allowedZones 从 "1 zone" 扩展为 "可在其他 zone 的 Boss 宝池以低概率出现"
- **R-P2-5**：material tier (IRON/STEEL/MITHRIL/ADAMANTITE) 的进阶使用率提升（当前 artifact 多数硬编码 STEEL）

---

## 4. opt PR-07 建议任务清单

将 P0 和关键 P1 打包成一轮 "收尾 PR"，确保 Phase 4 能真正结束。

**PR 标题**：`opt PR-07: phase4 structural coverage close-out`

**任务清单**：

1. **TASK-01**：为 7 个缺失 zone 补 `mapgenProfileId`（R-P0-1），含 biome family 补白
2. **TASK-02**：修复 terrainInteractionBatch zone 选择（R-P0-2）
3. **TASK-03**：greenwood encounter rate 修复（R-P0-3），含新 terrain rule
4. **TASK-04**：preferredTerrain 联动覆盖修复（R-P0-4），含 weight bonus 调整
5. **TASK-05**：secret loot 身份差异化（R-P0-5），含 schema 扩展
6. **TASK-06**：applyToTags 针对性保护（R-P1-1），可选
7. **TASK-07**：DamageVsTag 阵营补齐（R-P1-3），可选
8. **TASK-08**：grey_crown boss terrain flavor（R-P1-2），可选
9. **TASK-09**：metric 新增 `preferredTerrainTagDistinctCount`、`secretProfileIdentityOverlap`，并补 per-zone lower bound gate
10. **TASK-10**：重新执行 phase4Report 全量回归

**退出 gate**（建议）：
- 11 个 zone 全部有 `mapgenProfileId`
- `zoneSummary` 覆盖所有带 profileId 的 zone
- **每个 Phase 4 zone 单独**满足 `terrainInteractionEncounterRate ≥ 12%`（per-zone gate 下限）
- **聚合**满足 ≥ 16.36%（原 gate 保留）
- `preferredTerrainTagsSeen ⊇ {WATER, ICE, OIL}`
- `lootProfileMaxBaseItemOverlap < 0.70`（新 sanity）
- 所有 12 个 harness task + 15 条 experience metric 继续 PASS

**估算总时间**：8~12 人日。**若 9~10 不算**，P0 部分约 5~8 人日可落地。

---

## 5. 附录 A：DamageVsTag affix 全量清单

执行 `grep -n "kind: DamageVsTag"` on `game/src/main/resources/data/items/index.yaml` 得到：

```
1056: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.12 }
1073: { kind: DamageVsTag, tag: undead, bonusPercent: 0.12 }
1751: { kind: DamageVsTag, tag: undead, bonusPercent: 0.10 }
1926: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.10 }
1995: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.15 }
2161: { kind: DamageVsTag, tag: bandit, bonusPercent: 0.12 }
```

**出现次数**：bandit × 4，undead × 2，**总共只有 2 种 tag，6 条 affix**。

**对比**：`DamageTypeBonus`（元素加成）共 6 条覆盖 FIRE × 2、LIGHTNING × 2、HOLY × 2，这是"属性轴"；`DamageVsStatus` 共 6 条覆盖 MARKED / ARMOR_BREAK / BURN / FREEZE / BANE，这是"状态轴"。而"阵营轴" (DamageVsTag) 只有 2 种，相对其他两轴严重不对称。

**修复建议**：见 §3.2 R-P1-3（补 6 条新 affix）。

---

## 6. Part 3 小结

**P0 清单**（5 条）+ **P1 清单**（5 条）+ **P2 清单**（5 条）= **共 15 条可执行建议**。

**核心收敛点**：
- **CI-01 是所有其他 P0 的根因或放大器**：修完 P0-A 后，P0-B、P0-D、P0-E、P1-B 都会自然缓解一部分
- **P0-C 的修复需要写新内容**（新 terrain rule + 新 monster talent），这是 opt PR-07 中最具创造性的任务
- **Gate 设计缺陷** 是隐形的 "第 6 个 P0"：当前 exit gate 全部是聚合口径，缺 per-zone lower bound——在 R-P0 完成后应补上

**Part 4 将给出延后问题（P2 的明确记录）与最终结论**。
