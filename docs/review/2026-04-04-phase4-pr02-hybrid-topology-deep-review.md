# Phase 4 PR-02 深度审查报告：Hybrid Topology、Pattern Room、Vault 与 Biome Family

**审查日期**: 2026-04-04  
**审查角色**: 资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人  
**对比基准**: `docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md`  
**分支**: `codex/phase4-pr02-hybrid-topology`

---

## 0. 审查总结

| 维度 | 评级 | 说明 |
|------|------|------|
| 与文档一致性 | ★★★★☆ | 核心流水线、数据结构、zone 升级口径高度一致，少数细节有偏差 |
| 架构质量 | ★★★★★ | 双管线路由 + planner/pipeline 分离 + deterministic seeding 设计扎实 |
| 内容数据完备性 | ★★★★☆ | 4 组 biome family × 2 = 8 个 family，每个 zone 2 vault + 2 pattern，覆盖面合理 |
| 测试与自证 | ★★★★☆ | smoke/golden/lint/contract 四层测试，但白盒验证部分有缺口 |
| Roguelike 玩法体验 | ★★★☆☆ | 拓扑差异已可感知，但 pattern room 和 vault 的视觉/玩法辨识度有提升空间 |

**一句话结论**：PR-02 的核心实现与设计文档高度一致，流水线架构、数据口径、zone 升级范围均满足出口门禁要求。当前分支已收口此前识别出的 `D-01 / D-02 / D-05`，剩余偏差项主要是 `D-03 / D-04` 这类低风险 follow-up，不阻塞合并。

**补充核实**：早前 inline review 中提到的 “`terrainHintsForPattern` 仍依赖 `patternId -> TerrainTag` 代码映射” 在当前分支已不成立。现实现已经完全基于 pattern YAML 模板中的 glyph（`~ / o / *`）推导 `TerrainTag`，不再保留 code-side fallback。

---

## 1. 逐项对比：设计文档要求 vs 实际实现

### 1.1 阶段目标（§1）完成度

| # | 目标 | 状态 | 证据 |
|---|------|------|------|
| 1 | TopologyPlanner 支持 room → loop → pattern → vault → biome paint 流水线 | ✅ | `HybridTopologyMapgenPipeline.run()` 严格按 plan → instantiate → carve → corridor → pattern → vault → corridor → anchor → paint 顺序执行 |
| 2 | RoomDef / PatternRoomDef / VaultDef / BiomeFamilyDef 进入 game 内容层 | ✅ | `game/src/main/resources/data/mapgen/{rooms,patterns,vaults,biomes}/index.yaml` 全部就位 |
| 3 | 4 组 biome family 正式化，4 个 zone 升级 | ✅ | forest+bog / mine+forge / flooded_cavern+crystal_bank / ruin+oil_catacomb，每组 2 family |
| 4 | zone 从 biome 单值迁移到 allowedBiomeFamilies | ✅ | `ZoneMapgenProfile.allowedBiomeFamilies` 替代旧 biome 字段，旧字段保留做兼容读取 |
| 5 | vault 与 pattern room 建立可 lint、可 seed 重放的数据入口 | ✅ | `Phase4MapgenContractLintTest` 交叉校验 + `TopologyFingerprinting` seed 重放 |

### 1.2 冻结口径（§2.1）合规性

| # | 口径 | 状态 | 说明 |
|---|------|------|------|
| 1 | 正式 planner = 拓扑模板 + 房间实例化 + 环路注入 + biome 着色 + 校验，不引入 WFC 或脚本宿主 | ✅ | 未引入任何 WFC 或脚本运行时 |
| 2 | 同层最多 2 个 biome family 混合 | ✅ | `selectBiomeFamilies()` 最多返回 2 个 family |
| 3 | vault 必须声明 pathClass，CRITICAL_PATH 禁止高回报 vault | ✅ | `VaultDef.init` 有 `require(pathClass != PathClass.CRITICAL_PATH \|\| rewardBudget == 0)` |
| 4 | pattern room 只是在标准房间边界内套固定模式 | ✅ | `applyPatternTemplate()` 仅操作房间内部 tile，不创建独立 runtime 解释器 |
| 5 | zone 仍不新增主线 node；只升级现有 4 个 zone | ✅ | `zones/index.yaml` 仅修改 4 个目标 zone 的 mapgenProfileId/rewardProfileId |

### 1.3 核心数据结构（§4.1）对比

| 数据结构 | 文档定义 | 实际实现 | 一致性 |
|----------|----------|----------|--------|
| `RoomDef` | id, shape, widthRange, heightRange, tags | 完全一致 | ✅ |
| `PatternRoomDef` | id, baseRoomId, patternId, requiredTags, spawnWeight | 完全一致 | ✅ |
| `VaultDef` | id, templateId, pathClass, threatBudget, rewardBudget, allowOnBiomeFamilies, requiredTerrainTags | 完全一致 | ✅ |
| `BiomeFamilyDef` | id, primaryTileSet, secondaryTileSet?, terrainTagWeights, allowedRoomTags | 完全一致 | ✅ |

**额外增加的结构**（文档未明确要求但合理的扩展）：

- `PatternTemplateDef` / `VaultTemplateDef`：模板行数据，用于实际 tile carving
- `MapgenContentCatalog`：聚合所有内容数据的容器
- `ZoneRewardProfile`：将奖励配置从 mapgen profile 中拆出作为 sibling，**与 §4.3 迁移规则 #4 一致**
- `TopologyFingerprinting`：确定性哈希，用于 golden 回归

### 1.4 生成顺序（§4.2）

| # | 文档要求 | 实现 | 一致性 |
|---|----------|------|--------|
| 1 | ZoneMapgenProfile → TopologyGraph | `planner.plan(profile, request)` | ✅ |
| 2 | TopologyGraph → RoomInstance | `instantiateRooms(topology, request)` | ✅ |
| 3 | RoomInstance → Corridor / Loop | `topology.edges.forEach { carveCorridor(...) }` | ✅ |
| 4 | PatternRoomDef / VaultDef 覆盖 | `applyPatternRooms()` → `applyVaults()` | ✅ |
| 5 | BiomeFamilyDef + zone override → TerrainTag paint | `paintTerrainTags()` | ✅ |
| 6 | 产出 GeneratedFloor | `return GeneratedFloor(...)` | ✅ |

**注意**：实现中走廊被 carve 了两次（第 263-270 行和第 274-280 行），第二次是在 pattern/vault 应用之后。这是一个**合理的工程决策** — 确保 pattern/vault carving 不会阻断走廊连通性，但文档未提及此双 pass 策略。

### 1.5 zone 升级口径（§4.3）

| zone | 文档 biome family | 实际实现 | rarityBonus | qualityBonus | 一致性 |
|------|-------------------|----------|-------------|--------------|--------|
| greenwood_fringe | forest + bog | ✅ forest + bog | 0.00 ✅ | 0 ✅ | ✅ |
| deep_iron_pit | mine + forge | ✅ mine + forge | 0.05 ✅ | 1 ✅ | ✅ |
| underground_river | flooded_cavern + crystal_bank | ✅ flooded_cavern + crystal_bank | 0.08 ✅ | 1 ✅ | ✅ |
| abyssal_temple | ruin + oil_catacomb | ✅ ruin + oil_catacomb | 0.12 ✅ | 2 ✅ | ✅ |

迁移规则合规性：

| # | 规则 | 状态 |
|---|------|------|
| 1 | 旧 biome 字段仅做兼容读取 | ✅ `RoutedMapgenPipeline` 路由到 BSP 管线时仍读旧 biome |
| 2 | 修改过的 zone 必须声明 ZoneMapgenProfile | ✅ 4 个 zone 均有 mapgenProfileId |
| 3 | 未迁移 zone 走单 family fallback | ✅ `SchemaZoneMapgenProfileResolver` 生成 fallback profile |
| 4 | ZoneRewardProfile 作为 sibling profile 冻结 | ✅ 单独的 rewardProfileId 引用 |

### 1.6 vault 奖励预算（§4.4）

| # | 冻结规则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | rewardBudget 只表示额外回报强度 | ✅ | 不与具体掉落绑定 |
| 2 | OPTIONAL vault 才允许 rewardBudget > 0 | ✅ | `VaultDef.init` 校验 + lint 测试 |
| 3 | rewardBudget 与 threatBudget 只冻结为统一货币 | ✅ | 无兑换公式 |
| 4 | rewardBudget 不得承载主线钥匙/道具 | ✅ | vault 仅记录 budget 数值 |
| 5 | mapgenSmoke 输出 vaultId → budget bucket | ✅ | `vaultRewardBudgetBuckets` / `vaultThreatBudgetBuckets` 在 smoke summary 中输出 |

### 1.7 content 与 lint（§4.5）

| # | lint 规则 | 状态 | 实现位置 |
|---|----------|------|----------|
| 1 | vault.templateId 必须显式存在 | ✅ | `Phase4MapgenContractLintTest:38` |
| 2 | requiredTerrainTags 只能引用 WATER/OIL/ICE | ✅ | `Phase4MapgenContractLintTest:42` |
| 3 | allowedBiomeFamilies 不允许为空 | ✅ | `Phase4MapgenContractLintTest:40` + `VaultDef.init:113` |
| 4 | patternId 与 baseRoomId 必须能解析到实际数据 | ✅ | `Phase4MapgenContractLintTest:26-27` |
| 5 | rewardBudget 不能为负，CRITICAL_PATH vault ≤ 0 | ✅ | `Phase4MapgenContractLintTest:43-46` + `VaultDef.init:115,119` |
| 6 | zone rarityBonus/qualityBonus 与冻结表一致 | ✅ | `Phase4MapgenContractLintTest:55-90` 逐 zone 断言 |

### 1.8 推荐改动面（§5）

| 模块 | 改动 | 状态 |
|------|------|------|
| core: TopologyPlanner + room placement | ✅ | `HybridTopologyPlanner` + `instantiateRooms()` |
| core: GeneratedFloor 保存 rooms/vaultPlacements/terrainTags | ✅ | `GeneratedFloor` data class 全部字段就位 |
| core: MapgenContractTest 覆盖 loop/biome/vault | ✅ | 5 个测试方法 |
| game: data/mapgen/* 内容目录 | ✅ | rooms/patterns/vaults/biomes/zones 五个 index.yaml |
| game: zone schema 增加 mapgenProfileId | ✅ | zones/index.yaml 4 个 zone 均有 |
| game: zone 引用 ZoneMapgenProfile + ZoneRewardProfile | ✅ | 双 profile 引用 |
| tools: mapgenSmoke 扩展输出 | ✅ | 见下表 |

**mapgenSmoke 输出检查**（§5.3）：

| 统计项 | 文档要求 | 实现 | 状态 |
|--------|----------|------|------|
| optionalLoopCount | ✅ | `MapgenSmokeRunner:91` | ✅ |
| loopEdgeRatio | ✅ | `MapgenSmokeRunner:93` | ✅ |
| vaultPlacements | ✅ | `MapgenSmokeRunner:101` | ✅ |
| biomeFamilies | ✅ | `MapgenSmokeRunner:99` | ✅ |
| terrainTagDistribution | ✅ | `MapgenSmokeRunner:100` | ✅ |
| vaultRewardBudgetBuckets | ✅ | `MapgenSmokeRunner:254` | ✅ |
| vaultThreatBudgetBuckets | ✅ | `MapgenSmokeRunner:258` | ✅ |

### 1.9 测试与自证（§6）

| # | 必测行为 | 状态 | 证据 |
|---|----------|------|------|
| 1 | 每层至少 1 条主路径 | ✅ | `MapgenContractTest:92` + smoke harness walkability check |
| 2 | 环路 0~2，loopEdgeRatio 0.15~0.35 | ✅ | `MapgenContractTest` 已按 `optionalLoopCount > 0` 条件化断言 |
| 3 | vault 只出现在 OPTIONAL 候选位置 | ✅ | `MapgenContractTest:97` + `applyVaults()` 过滤 OPTIONAL |
| 4 | 同 seed 下输出稳定 | ✅ | `MapgenContractTest:27-39` 确定性 + golden 回归 |
| 5 | ZoneRewardProfile 与 vault budgets 可追溯 | ✅ | smoke report + golden file 均输出 reward profile 快照 |

---

## 2. 偏差清单

### D-01（已修复）：loopEdgeRatio 断言条件补齐 optionalLoopCount > 0 前置判断

**文件**: `core/src/test/kotlin/com/ktome/core/mapgen/MapgenContractTest.kt:96`

**文档要求**（§6.1 #2）：
> 环路数量保持在 0 ~ 2；若大于 0，loopEdgeRatio 在 0.15 ~ 0.35。

**修复后实现**：
```kotlin
if (generated.topology.optionalLoopCount > 0) {
    assertTrue(generated.topology.loopEdgeRatio() in 0.15..0.35)
}
```

**结果**：测试口径已与 `docs/phase4/2026-03-13-phase4-verification-checklist.md` 保持一致，不再存在“未来 loopCountRange 放宽到 0 时误报失败”的风险。

---

### D-02（已修复）：VaultDef.requiredTerrainTags 已进入正式 init 白名单合同

**文件**: `core/src/main/kotlin/com/ktome/core/mapgen/MapgenContracts.kt:108`

**文档要求**（§4.5 #2）：
> requiredTerrainTags 只能引用 WATER / OIL / ICE。

**修复后实现**：
- `VaultDef.init` 已通过共享常量 `ALLOWED_VAULT_REQUIRED_TERRAIN_TAGS` 做正式白名单校验。
- `Phase4MapgenContractLintTest` 已复用同一份常量，不再在 lint 中手写第二套白名单。

**结果**：即使未来 `TerrainTag` 枚举扩展，vault terrain 合同仍会在 runtime model 边界 fail fast，不再只依赖 lint 守门。

---

### D-03（低风险）：pattern room 没有保证"每层至少出现一次"

**文件**: `HybridTopologyPipeline.kt:384-423`

**文档要求**（§6.3 #2）：
> 至少观察一次 vault 和一次 pattern room 的生成。

**实际实现**：
```kotlin
val shouldAssign =
    stableRandom(request.seed, "pattern:${room.nodeId.value}").nextInt(100) < 65 ||
        (!assigned && index == eligibleIndices.last())
```

**分析**：fallback 逻辑 `(!assigned && index == eligibleIndices.last())` 确保如果之前没有 pattern room 被分配，最后一个候选一定会被分配。**但前提是 `eligibleIndices` 非空且对应房间有可用的 eligible patterns。** 如果所有 `pattern_candidate` 节点的 `eligiblePatternsFor()` 返回空列表（因为 baseRoomId 不匹配），则整层不会有 pattern room。

从当前内容数据看，每个 zone 的 hub 节点被标记为 `pattern_candidate`，且有对应的 pattern room 数据，所以**实际运行中大概率总会出现 pattern room**。但这不是代码层面的硬保证。

**偏差程度**：低，文档 §6.3 是白盒验证建议而非硬性不变量。golden 文件显示 greenwood_fringe 确实生成了 pattern room。

---

### D-04（低风险）：corridor carving 双 pass 策略未在文档中记录

**文件**: `HybridTopologyPipeline.kt:263-280`

**现象**：走廊被 carve 了两次 — 一次在 pattern/vault 之前（L263-270），一次在之后（L274-280）。这是为了防止 pattern/vault 的 wall carving 阻断走廊连通性。

**影响**：从 Roguelike 关卡设计角度看，这是一个合理的工程决策，确保连通性。但从设计文档角度看，§4.2 的生成顺序只列出了一次 corridor 步骤。如果后续 PR 需要在走廊上做装饰或陷阱，需要知道走廊会被二次覆写。

**建议**：在文档或代码注释中记录此决策。无需代码修改。

---

### D-05（已修复）：模板白名单第二权威已删除

**文件**: `HybridTopologyPipeline.kt:12-34`

**原问题**：`MapgenTemplateCatalog.supportedPatternIds` 和 `supportedVaultTemplateIds` 硬编码在 `core` 层。如果 `game` 内容层新增模板，需要同步修改 `core` 代码。

**修复后实现**：`MapgenTemplateCatalog` 已从 `core` 删除；lint 现在直接以 `DataLoader` 解析出的 pattern/vault template 集合作为单一权威，只验证 `patternId / templateId` 是否能解析到真实数据。

**结果**：模板可解析性已经完全数据驱动，不再需要同步维护 `core` 层白名单。

---

## 3. 出口门禁检查

| # | 门禁条件（§7） | 状态 | 说明 |
|---|----------------|------|------|
| 1 | TopologyPlanner、room/pattern/vault/biome family 数据口径冻结 | ✅ | `MapgenContracts.kt` 所有 data class 带 init 校验 + lint 测试 |
| 2 | 四个目标 zone 至少有一层正式跑在 Phase 4 planner 上 | ✅ | `RoutedMapgenPipeline` 路由 + golden 文件证实 |
| 3 | mapgenSmoke 能稳定输出 loop/vault/biome 统计 | ✅ | smoke harness 测试断言 500+ case + 统计字段存在 |
| 4 | PR-03 可直接基于 GeneratedFloor 构建 SolvabilityGraph | ✅ | `GeneratedFloor` 暴露 topology/rooms/terrainTags/vaultPlacements，足够构建可达性图 |

**结论：全部 4 项出口门禁通过。**

---

## 4. Roguelike 玩法体验审查

从 ToME / Roguelike 设计总监视角的补充评估：

### 4.1 拓扑多样性 — 满足基本要求

- 4-5 primary nodes + 0-2 optional nodes，加上 biome family 交替分配，能产生视觉和路径差异。
- golden 文件显示 greenwood_fringe 可产出 6 nodes / 7 edges / 2 loops 的拓扑，是可接受的复杂度。
- `RoomShape` 四种形状（RECT / L_SHAPE / ROUND / IRREGULAR）提供了基本的空间差异。

### 4.2 环路质量 — 需关注

- 环路通过 optional 节点实现，一端连到主路径倒数第二/三个节点，另一端回连到最后一个节点。
- 这种"支路回环"模式合理但单一。所有环路的结构都是 `primary[n-1-i] → optional[i] → primary[last]`，缺乏拓扑变体。
- **建议**：后续 PR 考虑引入 mid-path 分叉环路，不一定总是回连到 goal 节点。

### 4.3 pattern room / vault 辨识度

- pattern template 使用 5×5 glyph grid 通过缩放应用到不同大小房间，效果取决于房间尺寸。
- vault 和 pattern room 的 tile carving 仅操作 FLOOR/WALL，没有视觉标记（如特殊 tile type）。从纯 ASCII 渲染角度看，vault 房间和普通房间在 tile 层面差异不大。
- **建议**：后续 PR 可考虑给 vault 房间的 terrain tag 增加标记性（如 vault 入口处固定放置特定 terrain tag），提升可感知差异。

### 4.4 biome 着色密度

- terrain tag 覆盖率约 14% × weight ratio，实际每个房间只有少量 tile 被标记。
- 从 Roguelike 地形交互设计角度看，这个密度适中 — 足够传达主题但不会过度干扰行走。
- `underground_river` 的 WATER 权重 0.28 + 0.22（biome + zone profile）会产生显著的水域覆盖，符合设计意图。

### 4.5 vault budget 分布合理性

| zone | vault 1 threat/reward | vault 2 threat/reward | 评估 |
|------|----------------------|----------------------|------|
| greenwood_fringe | 4/6 | 5/7 | ✅ 入门 zone，低风险低回报 |
| deep_iron_pit | 6/7 | 5/6 | ✅ 中段 zone，适度提升 |
| underground_river | 7/6 | 6/8 | ✅ 探索型，奖励高但威胁也高 |
| abyssal_temple | 8/8 | 7/6 | ✅ 终段 zone，高压高回报 |

budget 递增曲线合理，符合 Roguelike "risk-reward escalation" 基本原则。

---

## 5. 修复优先级与建议

| ID | 风险 | 修改量 | 是否阻塞合并 | 修复建议 |
|----|------|--------|-------------|----------|
| D-01 | 已修复 | ~3 行 | 否 | `MapgenContractTest` 已加 `optionalLoopCount > 0` 前置条件 |
| D-02 | 已修复 | ~5 行 | 否 | `VaultDef.init` 与 lint 已共享 terrain tag 白名单合同 |
| D-03 | 低 | 0 行 | 否 | 当前内容数据已覆盖，无需代码修改 |
| D-04 | 低 | 0 行 | 否 | 补注释说明双 pass corridor 策略即可 |
| D-05 | 已修复 | ~20 行 | 否 | `MapgenTemplateCatalog` 已删除，模板可解析性改为数据驱动 |

---

## 6. 已修复项摘录

```kotlin
if (generated.topology.optionalLoopCount > 0) {
    assertTrue(generated.topology.loopEdgeRatio() in 0.15..0.35)
}
```

```kotlin
require(requiredTerrainTags.all(ALLOWED_VAULT_REQUIRED_TERRAIN_TAGS::contains)) {
    "VaultDef.requiredTerrainTags must stay within the supported vault terrain contract."
}
```

---

## 7. 总结

PR-02 的实现质量整体优秀。核心流水线架构清晰，数据口径与文档高度一致，四层测试体系（unit/contract/lint/golden）提供了可靠的回归保护。zone 升级的 biome family / reward profile 数值与冻结表完全匹配。

**主要亮点**：
- deterministic seeding + topology fingerprinting 的确定性保证设计成熟
- dual pipeline routing（hybrid vs BSP compatibility）的架构决策干净
- lint 测试交叉校验内容数据引用完整性，防止数据腐化

**核心建议**：当前分支可以合并。若要继续 polish，后续只需把 `D-03 / D-04` 这类低风险说明性问题在 PR-03 或后续文档整理时顺手收口。 
