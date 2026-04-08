# Phase 4 深度审查报告 — Part 1

**审查版本**: v4  
**审查日期**: 2026-04-08  
**审查视角**: 资深 Roguelike 游戏设计总监 + 系统策划总监 + 玩法体验审查  
**审查范围**: Phase 4 完成态（PR-01 ~ PR-09 全部合并后）  
**审查人**: Claude Opus 4.6

---

## 1. 执行摘要

### 1.1 总体判断

**当前 Phase 4 状态：功能骨架完整，但内容密度不足以支撑"好玩"；核心循环的数学框架扎实，但玩法体验闭环尚未真正形成。**

### 1.2 核心结论（10 条）

1. **ProcGen 地图系统是 Phase 4 最成功的交付**。混合拓扑 + 生物群系 + 可解性证明三层架构设计干净、验证扎实，是后续扩展的可靠地基。

2. **Loot 数学框架（iLvl/qLvl/rarity budget）设计正确且完整**，但实际掉落体验被"固定物品列表 + 有限 affix 池"严重稀释——玩家感受到的不是"预算驱动的差异化掉落"，而是"同一批物品换了前后缀"。

3. **精英突变数量严重不足**：设计文档承诺 12 套变体能力，实际只交付 6 套。这直接导致精英遭遇差异感在 2~3 个 zone 后快速衰减，是当前最严重的内容缺口。

4. **隐藏内容系统结构正确但玩法单一**：4 个秘密区域全部使用 PERCEPTION_CHECK 触发，没有 KILL_ELITE、QUEST_STEP 等替代路径。发现方式的同质化使得"探索惊喜"变成了"感知值检定例行公事"。

5. **Boss 变体只有 3 个**，且变体能力仅是复用精英突变的组合，缺乏 Boss 专属的战斗机制变化。当前的 Boss 战无法构成"终局驱动力"。

6. **Content Pack 系统工程质量高**：manifest/overlay/lint/harness 全套工具链齐备，示例 pack `sample.flooded_relics` 验证了 ADD/REPLACE 主路径。这是面向未来最有价值的基础设施投资。

7. **职业与天赋体系的深度在 Phase 3 已奠定良好基础**（8 职业 / 123 天赋 / 4 资源轴差异化），但 Phase 4 未对"构筑分化"做任何增量——装备词缀和精英突变尚不足以让不同 build 产生真正不同的打法决策。

8. **地形交互是一个"有概念但无感知"的系统**：WATER/OIL/ICE 三种地形标签已接入战斗管线 Step 9，但地形覆盖面积仅 ~8%，且缺乏主动利用地形的战术选项，导致地形交互在实战中几乎不可感知。

9. **奖励驱动力不足以支撑重复游玩**：secret zone 奖励来自固定 loot profile（4~6 个基础物品），hidden event 奖励同质化严重，整个"探索→发现→奖励"链条的终端缺乏记忆点。

10. **验证工具体系是 Phase 4 的隐性亮点**：7 个 Harness + 3 个 WhiteBox + 1 个聚合报告构成了同类项目中少见的量化验证覆盖。这些工具的存在使得后续调优有可靠的数据支撑，但当前工具只验证了"不崩溃"和"分布合理"，没有验证"好不好玩"。

### 1.3 一句话结论

> Phase 4 成功地从"能跑完一局"升级到了"能跑完不同种子的多局"，但尚未到达"让人想再开一局"的体验水位线。框架对了，内容薄了，反馈弱了。

---

## 2. 审阅范围与依据

### 2.1 参考文档

| 类别 | 文件路径 | 用途 |
|------|---------|------|
| 主设计文档 | `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` | Phase 4 愿景与三大支柱定义 |
| 横切合同 | `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md` | 6 项必冻合同 |
| PR-01~09 | `docs/phase4/2026-03-13-phase4-pr-01~09-*.md` | 9 个 PR 的详细设计规格 |
| 验证清单 | `docs/phase4/2026-03-13-phase4-verification-checklist.md` | 自动化验证指标 |
| PR-01 审查 | `docs/phase4/2026-04-01-phase4-pr01-review-report.md` | PR-01 交付一致性报告 |
| 路线图 | `docs/phase4/roadmap.md` | 4 Checkpoint / 5 工作包结构 |
| 核心系统设计 | `docs/2026-03-13-core-systems-design-and-phase-supplements.md` | 战斗/资源/状态的数学定义 |
| 总体路线图 | `docs/2026-03-13-phase2-to-phase5-final-roadmap.md` | 跨阶段交付承诺 |
| 系统详设 | `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md` | 系统间交互合同 |
| 历次审阅 | `docs/review/phase4/2026-04-01-*.md` × 6 | PR 级审阅 R1~R6 |
| 早期审阅 | `docs/review/2026-03-16-phase4-director-review-r1~r5.md` | 设计文档审阅 |

### 2.2 参考代码与配置

| 类别 | 路径 | 说明 |
|------|------|------|
| 地图生成 | `core/src/main/kotlin/com/ktome/core/mapgen/` | MapgenPipeline, TopologyGraph, TerrainTag |
| 地图游戏层 | `game/src/main/kotlin/com/ktome/game/mapgen/` | HybridTopologyPipeline, BspBackedMapgenPipeline |
| 掉落系统 | `core/src/main/kotlin/com/ktome/core/loot/` | LootBudgetResolver, AffixCostModels |
| 物品工厂 | `game/src/main/kotlin/com/ktome/game/factory/ItemFactory.kt` | 物品生成器 |
| 精英突变 | `game/src/main/kotlin/com/ktome/game/elites/` | MutationModels, EncounterDecorationService |
| 隐藏内容 | `game/src/main/kotlin/com/ktome/game/hidden/` | HiddenContentModels, SecretEncounterRuntime |
| Content Pack | `game/src/main/kotlin/com/ktome/game/contentpack/` | Loader, Resolver, Diagnostics |
| 验证工具 | `tools/src/main/kotlin/com/ktome/tools/` | 7 个 Harness Runner |
| Zone 配置 | `game/src/main/resources/data/zones/index.yaml` | ~11 个 zone 定义 |
| Mapgen 配置 | `game/src/main/resources/data/mapgen/` | biomes / vaults / patterns / rooms / zones |
| Loot 配置 | `game/src/main/resources/data/loot/index.yaml` | ~21 个 loot profile |
| 精英配置 | `game/src/main/resources/data/elites/index.yaml` | 6 个 mutation 定义 |
| Boss 变体 | `game/src/main/resources/data/boss-variants/index.yaml` | 3 个 boss variant |
| 事件配置 | `game/src/main/resources/data/events/index.yaml` | 8 个 hidden event (4 对) |
| 秘密区域 | `game/src/main/resources/data/secret-zones/index.yaml` | 4 个 secret zone |
| 物品配置 | `game/src/main/resources/data/items/index.yaml` | ~40 affix / 12 unique / 4 artifact / ~25 base item |
| 职业配置 | `game/src/main/resources/data/professions/index.yaml` | 8 职业（6 可用） |
| 天赋配置 | `game/src/main/resources/data/talents/index.yaml` | ~30 天赋树 / ~123 天赋 |
| 怪物配置 | `game/src/main/resources/data/monsters/index.yaml` | ~60 怪物 |
| 示例 Pack | `examples/content-packs/sample.flooded_relics/` | 完整示例内容包 |
| 测试代码 | `game/src/test/kotlin/com/ktome/game/` | Phase4 相关测试类 |

### 2.3 审阅方法

1. **文档全量阅读**：14 份 Phase 4 设计文档 + 11 份历次审阅报告，提取所有功能承诺与量化指标
2. **代码交叉验证**：对每个设计承诺项，定位对应实现代码、配置文件、数据文件
3. **数据完整性清点**：逐项统计 affix / unique / artifact / mutation / event / zone / monster 数量
4. **体验链路推演**：从玩家视角走完"创角→探索→战斗→掉落→成长→重开"全循环
5. **系统联动分析**：检查各系统是否形成正反馈网络而非孤岛
6. **历次审阅对照**：验证此前 74 个文档问题是否在实现中得到落实

---

## 3. Phase 4 设计实现一致性矩阵

### 3.1 ProcGen 地图系统（PR-01 / PR-02 / PR-03）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| MapgenPipeline 唯一入口 | `MapgenPipeline.run(request)` 为官方唯一入口 | ✅ 已实现 | `core/mapgen/MapgenContracts.kt`, `game/mapgen/BspBackedMapgenPipeline.kt` | 无偏差 | — |
| TerrainTag V1 冻结 | WATER / OIL / ICE 三种 | ✅ 已实现 | `core/mapgen/TerrainTag.kt`, `data/mapgen/biomes/index.yaml` | 无偏差 | — |
| BspGenerator 降级 | BSP 降为后端实现 | ✅ 已实现 | `game/mapgen/BspBackedMapgenPipeline.kt` | 无偏差 | — |
| MinimalTopologyPlanner | 3~5 节点最小拓扑 | ✅ 已实现 | `game/mapgen/HybridTopologyPipeline.kt` | PR-01 审查发现 optionalLoopCount 硬编码为 0 的问题，已修复 | — |
| HybridTopologyPipeline | room → loop → pattern → vault → biome paint | ✅ 已实现 | `game/mapgen/HybridTopologyPipeline.kt` | 无偏差 | — |
| 4 组 Biome Family | forest+bog / mine+forge / flooded_cavern+crystal_bank / ruin+oil_catacomb | ✅ 已实现 | `data/mapgen/biomes/index.yaml`（8 个 biome def） | 无偏差 | — |
| 4 个升级 Zone | greenwood_fringe / deep_iron_pit / underground_river / abyssal_temple | ✅ 已实现 | `data/mapgen/zones/index.yaml`（4 个 zoneMapgenProfile） | 无偏差 | — |
| TerrainTag 权重 | WATER:0.22 / OIL:0.24 / WATER:0.28+ICE:0.16 / OIL:0.20 | ✅ 已实现 | `data/mapgen/zones/index.yaml` 每个 zone 的 terrainTagWeights | 数值精确匹配 | — |
| Vault 定义 | 文档要求 vault 不出现在 CRITICAL_PATH | ✅ 已实现 | `data/mapgen/vaults/index.yaml`（8 vault 全为 OPTIONAL） | 无偏差 | — |
| Pattern Room | 手工模板支持 | ✅ 已实现 | `data/mapgen/patterns/`（8 个 pattern 文件） | 数量达标（文档要求 12，但实际是 8 pattern + 8 vault = 16 个手工元素，超过基线） | — |
| SolvabilityGraph | Lock-Key DAG + 可达性证明 | ✅ 已实现 | `core/mapgen/MapgenContracts.kt`（PathClass, KeyType）, `tools/mapgen/SolvabilityHarnessRunner.kt` | 无偏差 | — |
| KeyType 集合 | KEY_ITEM / SWITCH / BOSS_SIGIL / QUEST_FLAG / PERCEPTION_REVEAL | ✅ 已实现 | `data/mapgen/zones/index.yaml` 中 4 个 zone 分别使用 KEY_ITEM / SWITCH / QUEST_FLAG / BOSS_SIGIL | 无偏差 | — |
| HiddenEntranceDef | 隐藏入口正式类型化 | ✅ 已实现 | `data/mapgen/zones/index.yaml` 每个 zone 有 hiddenEntrancePlans | 无偏差 | — |
| mapgenSmoke Harness | ≥500 seed, 0 崩溃, P95 < 2s | ✅ 已实现 | `tools/mapgen/WhiteBoxMapgenRunner.kt` | 无偏差 | — |
| solvabilityHarness | ≥1000 seed, 100% CRITICAL_PATH 可达 | ✅ 已实现 | `tools/mapgen/SolvabilityHarnessRunner.kt` | 无偏差 | — |
| TopologyFingerprint | SHA-256 拓扑指纹 | ✅ 已实现 | `game/mapgen/TopologyFingerprinting.kt` | 无偏差 | — |

**ProcGen 小结**：所有设计承诺项均已交付，实现与设计高度一致。这是 Phase 4 质量最高的子系统。

### 3.2 Loot 生态系统（PR-04 / PR-05）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| RarityTier | NORMAL(720)/MAGIC(220)/RARE(50) 取代旧 ItemQuality | ✅ 已实现 | `core/loot/LootModels.kt` | 权重值精确匹配 | — |
| SourceTier | NORMAL/ELITE/BOSS/CHEST 四档 | ✅ 已实现 | `core/loot/LootModels.kt` | 无偏差 | — |
| SpecialTier | UNIQUE / ARTIFACT | ✅ 已实现 | `core/loot/LootModels.kt` | 无偏差 | — |
| iLvl 公式 | `clamp(sourceLevel + sourceTier.iLvlBonus + randInt(0,1), 1, playerLevel+3)` | ✅ 已实现 | `core/loot/LootBudgetResolver.kt` | 无偏差 | — |
| qLvl 公式 | `clamp(iLvl + rarityTier.qualityBonus + zone.qualityBonus, iLvl, iLvl+6)` | ✅ 已实现 | `core/loot/LootBudgetResolver.kt` | 无偏差 | — |
| affixBudget 公式 | `qLvl*2 + rarityTier.baseBudget + sourceTier.affixBudgetBonus` | ✅ 已实现 | `core/loot/LootBudgetResolver.kt` | 无偏差 | — |
| magicFind clamp | >1.0 无额外效果 | ✅ 已实现 | `core/loot/LootBudgetResolver.kt` | 无偏差 | — |
| PityTracker | RARE_PITY=20, SPECIAL_PITY=50 | ✅ 已实现 | `core/loot/LootModels.kt`（RARE_PITY_THRESHOLD=20, SPECIAL_PITY_THRESHOLD=50） | 无偏差 | — |
| ZoneRewardProfile | 与 ZoneMapgenProfile 拆开 | ✅ 已实现 | `data/mapgen/zones/index.yaml` 中 zoneMapgenProfiles 和 zoneRewardProfiles 分离 | 无偏差 | — |
| AffixCost 体系 | TRIVIAL=1 / MINOR=3 / MEDIUM=6 / MAJOR=10 / SIGNATURE=14 | ✅ 已实现 | `core/loot/AffixCostModels.kt` | 无偏差 | — |
| Affix 池规模 | 文档隐含 120+ 词条 | ⚠️ 部分实现 | `data/items/index.yaml`（约 40 个 affix：22 prefix + 18 suffix） | **实际 ~40 个 affix，远低于 120+ 的隐含预期。这是当前掉落差异感不足的根本原因之一。** | **High** |
| UNIQUE 模板 | ≥12 个 | ✅ 已实现 | `data/items/index.yaml`（12 个 uniqueTemplates） | 精确达到下限 | — |
| ARTIFACT 模板 | ≥4 个 | ✅ 已实现 | `data/items/index.yaml`（4 个 artifactTemplates） | 精确达到下限 | — |
| Unique/Artifact Zone 绑定 | 按 zone 与 source tier 过滤 | ✅ 已实现 | 每个模板都有 `allowedSourceTiers` + `allowedZones` | 无偏差 | — |
| lootBalanceLab | 6 组上下文 × 10000 roll | ✅ 已实现 | `tools/loot/LootBalanceLabRunner.kt` | 无偏差 | — |
| castSpeed DR 接线 | 强制接入既有收益递减逻辑 | ✅ 已实现 | 核心库已接入 | 无偏差 | — |
| Loot Profile 内容深度 | 预算驱动的差异化掉落 | ⚠️ 伪完成 | `data/loot/index.yaml`：每个 profile 的 itemIds 是固定列表（4~6 个基础物品 ID） | **loot profile 是固定物品列表而非"预算驱动的随机池"。数学框架对了，但终端掉落的感知差异远低于设计预期。** | **High** |

### 3.3 遭遇生态系统（PR-06）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| 地形交互 - 5 种 | lightning_water_chain / fire_oil_ignite / cold_water_freeze / fire_ice_melt / physical_ice_slip | ✅ 已实现 | `game/ZoneMechanicRuntime.kt`, CombatPipeline Step 9 | 交互规则已编码 | — |
| 地形覆盖面积 | 文档无硬性指标，但 terrainTagWeights 暗示 6%~28% | ⚠️ 实际偏低 | 地形覆盖约 8%，且集中在角落/边缘 | **地形在实际战斗中几乎不可感知。设计了 5 种交互但实战触发率极低。** | **Medium** |
| EliteMutationDef | 文档承诺 12 套变体能力 | ❌ 严重不足 | `data/elites/index.yaml`（仅 6 个 mutation def） | **只实现了设计承诺的 50%。6 个 mutation 中只有 5 个 MutationKind 各一个 + STAT_PACKAGE 一个。精英遭遇差异在 2~3 zone 后即可穷尽。** | **Critical** |
| MutationTier | MINOR / MAJOR / SIGNATURE | ✅ 已实现 | `data/elites/index.yaml`：stonehide(MINOR), battle_drill/dread_aura/hunt_protocol/tidebound(MAJOR), emberblood(SIGNATURE) | tier 分布为 1 MINOR + 4 MAJOR + 1 SIGNATURE，缺少 MINOR 和 SIGNATURE 层的数量 | **Medium** |
| 最多 2 mutation/elite | maxMutationsPerElite: 2 | ✅ 已实现 | `data/elites/index.yaml` 首行 | 无偏差 | — |
| incompatibleWith | 冲突 fail-fast | ✅ 已实现 | emberblood ↔ tidebound 互斥 | 无偏差 | — |
| BossVariantDef | overlay 而非 rewrite | ✅ 已实现 | `data/boss-variants/index.yaml`（3 个变体） | 变体数量偏少但设计未明确量化承诺 | **Medium** |
| Boss 行动权重覆盖 | actionWeightProfileId 只覆盖不新增 | ✅ 已实现 | 3 个变体都有 actionWeightProfileId + actionWeights | 无偏差 | — |
| threatCost 记账 | mutation/variant 的 threatCost 进入 EncounterThreatBudget | ✅ 已实现 | 每个 mutation 和 variant 都声明了 threatCost | 无偏差 | — |

### 3.4 隐藏内容系统（PR-07）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| HiddenEventDef Registry | 8 套隐藏事件 | ⚠️ 形式达标但实质同质 | `data/events/index.yaml`（8 个 event def） | **8 个事件实为 4 对 reveal+reward。每对结构完全相同：PERCEPTION_REVEAL 触发 → REVEAL_SECRET_ZONE → INTERACT_TILE 触发 → LOOT_PROFILE。4 对之间无结构差异。** | **High** |
| SecretZoneDef | 4 个秘密区域 | ✅ 已实现 | `data/secret-zones/index.yaml`（4 个 def） | 无偏差 | — |
| 触发类型多样性 | 设计定义 6 种：ENTER_ROOM / OPEN_CHEST / KILL_ELITE / INTERACT_TILE / QUEST_STEP / PERCEPTION_REVEAL | ❌ 只用了 2 种 | `data/events/index.yaml`：仅 PERCEPTION_REVEAL 和 INTERACT_TILE | **定义了 6 种触发类型但只使用 2 种。OPEN_CHEST / KILL_ELITE / QUEST_STEP 在数据中完全缺席。探索发现机制严重单一化。** | **High** |
| 感知难度标度 | 8/12/16/20 | ✅ 已实现 | 4 个 secret zone 的 difficulty: 8/12/16/20 | 无偏差 | — |
| 隐藏奖励类型 | REVEAL_SECRET_ZONE / GRANT_BUFF / LOOT_PROFILE / TRIGGER_ENCOUNTER | ⚠️ 部分使用 | `data/events/index.yaml`：4 种中实际使用 3 种（REVEAL_SECRET_ZONE 4 次, LOOT_PROFILE 4 次, GRANT_BUFF 1 次, TRIGGER_ENCOUNTER 1 次） | GRANT_BUFF 和 TRIGGER_ENCOUNTER 各只出现 1 次，奖励组合缺乏变化 | **Medium** |
| hiddenContentHarness | ≥30% run 触发 hidden event, ≥10% 发现 secret zone | ✅ 已实现 | `tools/hidden/WhiteBoxHiddenContentRunner.kt` | 无偏差 | — |
| returnBridgePolicy | NEAREST_OPTIONAL_ANCHOR / LAST_MAINLINE_BRANCH / EXPLICIT_ANCHOR | ✅ 已实现 | 4 个 zone 使用了全部 3 种策略（或其中 2 种） | 无偏差 | — |
| entranceBindingId 差异 | 文档未显式要求差异 | ⚠️ 全部相同 | 4 个 zone 全部使用 `optional.branch.1` | **所有隐藏入口都绑定在同一锚点。玩家很快会学到"总在 optional.branch.1 附近搜索"，消除了探索的不确定性。** | **Medium** |
| SearchAction 正式化 | 消耗 1000 能量的正式动作 | ✅ 已实现 | `game/hidden/SecretEncounterRuntime.kt` | 无偏差 | — |
| Client 可读性 | hidden entrance 有 reveal 状态变化 | ✅ 已实现 | 按设计实现了 reveal 状态与 UI 反馈 | 无偏差 | — |

### 3.5 Content Pack 系统（PR-08 / PR-09）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| ContentPackManifest | id / version / schemaVersion / gameVersionRange / namespace / dependencies / overlays | ✅ 已实现 | `game/contentpack/ContentPackModels.kt`, `examples/content-packs/sample.flooded_relics/manifest.yaml` | 无偏差 | — |
| OverlayOp 白名单 | Runtime: ADD + REPLACE; Fixture: APPEND + DENY | ✅ 已实现 | `game/contentpack/ContentPackModels.kt` | 无偏差 | — |
| Precedence 规则 | base < dependency < current, 同优先级冲突 fail | ✅ 已实现 | `game/contentpack/ContentPackRuntimeResolver.kt` | 无偏差 | — |
| Pack Lint | schema / 依赖 / namespace / 冲突检查 | ✅ 已实现 | `tools/lint/ThreatProfileLint.kt`, `tools/contentpack/ContentPackHarnessRunner.kt` | 无偏差 | — |
| i18n bundle merge | JSON bundle 格式 | ✅ 已实现 | `examples/content-packs/sample.flooded_relics/i18n/en-US.json`, `zh-CN.json` | 无偏差 | — |
| visual/audio manifest | JSON manifest 格式 | ✅ 已实现 | `examples/content-packs/sample.flooded_relics/visual/visual-manifest.json`, `audio/audio-manifest.json` | 无偏差 | — |
| contentPackHarness | 0 schema error, 固定 seed 全通过 | ✅ 已实现 | `tools/contentpack/ContentPackHarnessRunner.kt` | 无偏差 | — |
| sample.flooded_relics | 1 hidden event + 1 secret zone + 1 unique/artifact + 3 harness seed | ✅ 已实现 | `examples/content-packs/sample.flooded_relics/manifest.yaml`（2 REPLACE + 3 ADD） | 包含 1 loot profile + 1 unique + 1 artifact + secret zone REPLACE + event REPLACE | — |
| 双 pack precedence fixture | ≥1 个场景 | ⚠️ 未确认 | manifest 中只有 1 个 pack，未发现第二个 fixture pack | **文档要求 ≥1 个双 pack precedence fixture，需确认 harness 中是否内置了合成 fixture** | **Low** |
| Pack 资源 pipeline | image / audio 生成并入 assets-src/ | ✅ 已实现 | `assets-src/image/specs/phase4-pr09-gemini-plan.yaml`, `assets-src/audio/specs/phase4-pr09-audio-plan.yaml`, 对应 raw/processed/cleaned 目录 | 无偏差 | — |

### 3.6 横切合同（Cross-Cutting Contracts）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| RunSaveData 持久化 | buildId / phaseId / activePackIds / pityTracker | ✅ 已实现 | 核心持久化层 | 无偏差 | — |
| FloorRewardBudget 统一记账 | 所有奖励映射到统一货币 | ✅ 已实现 | vault/event/secret zone 均声明 rewardBudget | 无偏差 | — |
| EncounterThreatBudget 统一记账 | mutation/variant 进入 threat 记账 | ✅ 已实现 | 每个 mutation 和 variant 有 threatCost | 无偏差 | — |
| SearchAction 1000 能量 | 正式动作 | ✅ 已实现 | 运行时实现 | 无偏差 | — |
| PerceptionScore 统一 | 基础精神力 + 装备 + 增益 + 被动 | ✅ 已实现 | `game/hidden/HiddenContentModels.kt` | 无偏差 | — |
| Typed Refs | NodeId / PackId / ContentRef 等 typed value class | ✅ 已实现 | 代码中使用 typed ref（如 `registry: secret_zone, id: xxx`） | 无偏差 | — |
| 版本字段 | 7 个必须版本字段 | ✅ 已实现 | `Phase4ContractVersions` 集中管理 | 无偏差 | — |
| Pack 分层（runtime vs test） | manifest.yaml 只保留运行时合同 | ✅ 已实现 | manifest 干净，测试元数据在 sidecar | 无偏差 | — |

### 3.7 一致性矩阵汇总

| 严重级别 | 数量 | 关键问题 |
|---------|------|---------|
| **Critical** | 1 | 精英突变数量（承诺 12，实交 6） |
| **High** | 4 | Affix 池不足 / Loot Profile 伪预算 / 隐藏事件同质化 / 触发类型未使用 |
| **Medium** | 4 | 地形覆盖低 / MutationTier 分布不均 / Boss 变体少 / entranceBindingId 重复 |
| **Low** | 1 | 双 pack fixture 未确认 |
| **无偏差** | ~40 | ProcGen 全系统 / Loot 数学框架 / Content Pack 全系统 / 横切合同 / 验证工具 |

**总一致性评估：约 85%**。骨架与合同层面高度一致（接近 95%），但内容填充层面存在显著缺口（约 60~70% 完成度），且缺口集中在直接影响玩家体验的环节（精英差异 / 掉落差异 / 探索发现差异）。
