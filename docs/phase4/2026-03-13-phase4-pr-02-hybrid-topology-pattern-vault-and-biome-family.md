> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md`
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 4 - PR-02 Hybrid Topology、Pattern Room、Vault 与 Biome Family

**阶段**: `Phase 4 / P4-A / P4-W1b`  
**优先级**: `P0`  
**前置条件**: `PR-01` 完成  
**对应问题**: `PR-01` 只冻结了 mapgen 入口和 BSP 兼容壳；若没有正式的 topology planner、pattern/vault 词汇和 biome family 数据，`Phase 4` 的 run 差异依旧只会来自怪物/掉落变化，无法满足地图层 replayability 目标。

---

## 1. 阶段目标

把 `PR-01` 的 mapgen contract 落到正式的混合拓扑 planner，并给 `game` 建立 Phase 4 的第一批地图内容数据。

完成标准：

1. `TopologyPlanner` 不再只是 BSP 直出，而是支持 `room -> loop -> pattern -> vault -> biome paint` 的正式流水线。
2. `RoomDef / PatternRoomDef / VaultDef / BiomeFamilyDef` 进入 `game` 内容层。
3. 首批 `4` 组 biome family 正式化：`forest + bog`、`mine + forge`、`flooded_cavern + crystal_bank`、`ruin + oil_catacomb`，以 `4` 个 zone 升级包形式进入主线。
4. zone 从 `biome` 单值迁移到 `ZoneMapgenProfile.allowedBiomeFamilies`。
5. `vault` 与 `pattern room` 都建立可 lint、可 seed 重放的数据入口。

## 2. 当前问题

1. 现有地图只有 BSP 矩形房间与 L 形走廊，无法表达 `Phase 4` 的结构差异。
2. zone 只有一个 `biome` 字段，不能支持 family 混合和地形标签权重覆盖。
3. 没有 `vault / pattern room` 的内容 schema，内容人员无法在不写代码的前提下补地图差异。

### 2.1 本 PR 必须冻结的口径

1. `Phase 4` 正式 planner 固定为“拓扑模板 + 房间实例化 + 环路注入 + biome 着色 + 校验”，不引入 WFC 或脚本宿主。
2. 同层最多 `2` 个 biome family 混合，不做三种以上混合。
3. `vault` 必须声明 `pathClass`，且 `CRITICAL_PATH` 禁止高风险高回报 vault。
4. `pattern room` 只是在标准房间边界内套固定模式，不是独立 runtime 解释器。
5. zone 仍不新增主线 node；只升级现有 `greenwood_fringe / deep_iron_pit / underground_river / abyssal_temple`。

## 3. 范围与非目标

### 3.1 范围

1. 实现 `TopologyPlanner` 和 `RoomInstance` 生成流程。
2. 新增 room / pattern / vault / biome family YAML schema 与装配。
3. zone mapgen profile 从旧 `biome` 迁移到 family 组合 + terrain 权重 + roomTag 过滤。
4. 扩展 `mapgenSmoke` 报告，补 loop ratio、vault placement、biome mix 统计。

### 3.2 非目标

1. 不在本 PR 引入 lock-key、discovery rule 或 `SolvabilityGraph`。
2. 不在本 PR 给 `vault` 塞奖励或 secret 语义。
3. 不在本 PR 新增视觉资源；优先复用现有 `tilesetKey / zone visualKey / zone iconKey`。

## 4. 技术方案

### 4.1 正式 planner 数据结构

建议文件：

```text
core/src/main/kotlin/com/ktome/core/mapgen/TopologyPlanner.kt
game/src/main/resources/data/mapgen/rooms/*.yaml
game/src/main/resources/data/mapgen/patterns/*.yaml
game/src/main/resources/data/mapgen/vaults/*.yaml
game/src/main/resources/data/mapgen/biomes/*.yaml
game/src/main/resources/data/mapgen/zones/*.yaml
```

核心结构：

```kotlin
data class RoomDef(
    val id: String,
    val shape: RoomShape,
    val widthRange: IntRange,
    val heightRange: IntRange,
    val tags: Set<String>,
)

data class PatternRoomDef(
    val id: String,
    val baseRoomId: String,
    val patternId: String,
    val requiredTags: Set<String>,
    val spawnWeight: Int,
)

data class VaultDef(
    val id: String,
    val templateId: String,
    val pathClass: PathClass,
    val threatBudget: Int,
    val rewardBudget: Int,
    val allowOnBiomeFamilies: Set<String>,
    val requiredTerrainTags: Set<TerrainTag>,
)

data class BiomeFamilyDef(
    val id: String,
    val primaryTileSet: String,
    val secondaryTileSet: String?,
    val terrainTagWeights: Map<TerrainTag, Float>,
    val allowedRoomTags: Set<String>,
)
```

### 4.2 生成顺序

固定顺序：

1. `ZoneMapgenProfile -> TopologyGraph`
2. `TopologyGraph -> RoomInstance`
3. `RoomInstance -> Corridor / Loop`
4. `PatternRoomDef / VaultDef` 覆盖或填充房间内容
5. `BiomeFamilyDef + zone override -> TerrainTag paint`
6. 产出 `GeneratedFloor`

### 4.3 zone 升级口径

本 PR 同时冻结 `zone -> biome family -> reward hint` 的最小映射，但只冻结数据口径，不在本 PR 实现正式掉落消费；正式消费统一在 `PR-04` 接到 `ZoneRewardProfile + FloorRewardBudget`。

最小升级范围：

1. `greenwood_fringe` -> `forest + bog`
   - `rarityBonus = 0.00`
   - `qualityBonus = 0`
   - 理由：作为 Phase 4 第一批入口 zone，目标是主要放大布局差异与地形混合，不提前抬高掉落档位。
2. `deep_iron_pit` -> `mine + forge`
   - `rarityBonus = 0.05`
   - `qualityBonus = 1`
   - 理由：强化矿坑与锻炉主题下的高压战斗预期，并适度抬高中后段掉落质量，但不直接做 jackpot 式跃迁。
3. `underground_river` -> `flooded_cavern + crystal_bank`
   - `rarityBonus = 0.08`
   - `qualityBonus = 1`
   - 理由：强调探索回报和局部稀有资源点，提升物品质量而不是直接抬高高稀有率。
4. `abyssal_temple` -> `ruin + oil_catacomb`
   - `rarityBonus = 0.12`
   - `qualityBonus = 2`
   - 理由：作为更靠后的深层 zone，同时提高高稀有机会与词条质量，形成可感知的 run 后段差异。

迁移规则：

1. 保留旧 `biome` 字段仅做兼容读取，不再作为正式 planner 输入。
2. 所有 `Phase 4` 修改过的 zone 一律必须声明 `ZoneMapgenProfile`。
3. 未迁移 zone 允许暂时走单 family fallback，但不得影响已迁移 zone 的正式 contract。
4. zone 奖励配置从本 PR 起以 `ZoneRewardProfile` 作为 sibling profile 冻结，而不是继续挂在 `ZoneMapgenProfile` 里；`PR-04` 只能消费这里冻结过的字段，不再重新命名或平移语义。

### 4.4 vault 奖励预算与统一 reward/threat ledger 对接

`VaultDef.rewardBudget / threatBudget` 在本 PR 不是直接掉落脚本或直接战斗脚本，而是给后续统一 reward/threat ledger 提供稳定输入。

冻结规则：

1. `rewardBudget` 只表示 vault 额外回报强度，不表示必出某个具体稀有度。
2. `pathClass = OPTIONAL` 的 vault 才允许 `rewardBudget > 0`；`CRITICAL_PATH` vault 固定为 `0`。
3. `rewardBudget` 与 `threatBudget` 只冻结为统一货币，不在本 PR 提前冻结“多少预算一定换多少掉落/多少怪物”的完整兑换公式。
4. `rewardBudget` 只能提升可选奖励，不得承载主线钥匙或必须道具。
5. `mapgenSmoke` 需要把 `vaultId -> rewardBudget / threatBudget bucket` 输出到报告里，方便后续与 `lootBalanceLab / bossHarness` 交叉核对。

### 4.5 content 与 lint

建议给 `game` 加最小 lint：

1. `vault.templateId` 必须显式存在。
2. `requiredTerrainTags` 只能引用 `WATER / OIL / ICE`。
3. `allowedBiomeFamilies` 不允许为空。
4. `patternId` 与 `baseRoomId` 必须能解析到实际数据。
5. `rewardBudget` 不能为负数，且 `CRITICAL_PATH` vault 不得大于 `0`。
6. zone 的 `rarityBonus / qualityBonus` 必须与主 `Phase 4` 文档的冻结表完全一致，禁止在 PR 文档中维护第二套数值。

## 5. 推荐改动面

### 5.1 `core`

1. 实现 `TopologyPlanner` 与 room placement。
2. 在 `GeneratedFloor` 中正式保存 `rooms / vaultPlacements / terrainTags`。
3. 扩展 `MapgenContractTest` 覆盖 loop、biome 和 vault 不变量。

### 5.2 `game`

1. 新增 `data/mapgen/*` 内容目录。
2. zone schema 增加 `mapgenProfileId` 或等价映射入口。
3. 把现有 4 个 target zone 升级到 biome family 组合，并让 zone schema 同时引用 `ZoneMapgenProfile + ZoneRewardProfile`。

### 5.3 `tools`

1. 扩展 `mapgenSmoke` 输出：
   - `optionalLoopCount`
   - `loopEdgeRatio`
   - `vaultPlacements`
   - `biomeFamilies`
   - `terrainTagDistribution`
   - `vaultRewardBudgetBuckets`
   - `vaultThreatBudgetBuckets`

## 6. 测试与自证

### 6.1 必测行为

1. 每层至少 `1` 条主路径。
2. 环路数量保持在 `0 ~ 2`；若大于 `0`，`loopEdgeRatio` 在 `0.15 ~ 0.35`。
3. `vault` 只出现在 `OPTIONAL` 候选位置。
4. 同一 seed 下 `biomeFamilies`、`vaultPlacements`、`terrainTagDistribution` 稳定。
5. zone 的 `ZoneRewardProfile` 与 `vault.rewardBudget / threatBudget` 在报告中可追溯，供 `PR-04 / PR-06` 直接消费。

### 6.2 自动化命令

```bash
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
```

### 6.3 白盒验证

1. 连续开 `5` 个不同 seed 的 `greenwood_fringe / deep_iron_pit / underground_river` run，人工确认至少出现 `3` 类可感知差异：
   - 主路径/环路形态差异
   - vault 或 pattern room 出现差异
   - biome family 混合与 terrain tag 痕迹差异
2. 至少观察一次 `vault` 和一次 `pattern room` 的生成。
3. 确认 `underground_river` 或 `abyssal_temple` 已出现 `WATER / OIL / ICE` 标签痕迹，但仍不执行正式地形交互。
4. 抽查一个 `rewardBudget >= 6` 的 vault，确认它只记录统一 reward ledger 输入，不在本 PR 直接生成高稀有奖励。

## 7. 出口门禁

1. `TopologyPlanner`、room/pattern/vault/biome family 数据口径冻结。
2. 四个目标 zone 至少有一层正式跑在 Phase 4 planner 上。
3. `mapgenSmoke` 能稳定输出 loop/vault/biome 统计。
4. `PR-03` 可以直接基于 `GeneratedFloor` 构建 `SolvabilityGraph`，无需再回头改 planner 入口。
