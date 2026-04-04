> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md`
> `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`

# Phase 4 - PR-06 Terrain Interaction、Elite Mutation 与 Boss Variant

**阶段**: `Phase 4 / P4-B / P4-W4a`  
**优先级**: `P0`  
**前置条件**: `PR-02` 完成；`PR-03 / PR-04 / PR-05` 可并行推进，但合入前需复核与 `PR-05` 的奖励和表现 key 接线  
**对应问题**: 地图、掉落和可解性 contract 就位后，`Phase 4` 还缺少“遭遇差异”的正式入口。若不把地形交互、elite mutation 和 Boss variant 一次收敛到既有 `CombatPipeline / BossEncounter` 体系，后续最容易出现第二套 AI 或第二套 encounter 语义。

---

## 1. 阶段目标

完成 Phase 4 第一批遭遇差异系统，并保持所有新行为仍走已有正式 runtime path。

完成标准：

1. `TerrainTag -> CombatPipeline step 9 -> ElementInteractionRule` 正式接线。
2. `EliteMutationDef` 与 `BossVariantDef` 进入正式 contract。
3. mutation 的 inspect / log / icon / telegraph 可读性冻结。
4. Boss 变体只允许 overlay mutation、loot 和表现，不重写 `BossEncounterDef` phase graph。

## 2. 当前问题

1. 现有 zone mechanic 和 affix 还没有统一走 `TerrainTag + ElementInteractionRule`。
2. elite / boss 目前仍偏向固定模板，重复 run 差异不足。
3. 如果直接在 client 或 Boss encounter 私有逻辑里做变体，会破坏 `Phase 3` encounter contract。

### 2.1 本 PR 必须冻结的口径

1. 正式地形交互 rule id 固定为：
   - `terrain_lightning_water_chain`
   - `terrain_fire_oil_ignite`
   - `terrain_cold_water_freeze`
   - `terrain_fire_ice_melt`
   - `terrain_physical_ice_slip`
2. `CombatPipeline step 9` 是唯一地形交互挂点。
3. `EliteMutationDef` 只允许 `STAT_PACKAGE / ABILITY_GRANT / AURA / AI_SHIFT / ELEMENT_PACKAGE`。
4. `BossVariantDef` 不能修改 phase graph，只能 overlay mutation、lootProfile、表现 key 和有限权重。
5. `LAVA` 在 `Phase 4` 仍只允许作为 zone mechanic/visual 主题，不进入正式 `TerrainTag` 枚举。

## 3. 范围与非目标

### 3.1 范围

1. 地形联动正式接线。
2. elite mutation registry。
3. Boss variant overlay。
4. inspect/log/readability contract。
5. 对应 visual/audio 资源计划。

### 3.2 非目标

1. 不在本 PR 正式引入 hidden event / secret zone 奖励桥接。
2. 不在本 PR 新建第二套 AI DSL、第二套 telegraph 或第二套 mutation 体系。

## 4. 技术方案

### 4.1 地形交互接线

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt
core/src/main/kotlin/com/ktome/core/combat/ElementInteractionRegistry.kt
core/src/test/kotlin/com/ktome/core/combat/TerrainInteractionTest.kt
```

正式路径：

1. `MapgenPipeline` 只生成 `TerrainTag`。
2. `CombatPipeline step 9` 检查 `TARGET_ON_TERRAIN`。
3. 调用正式 `ElementInteractionRule` registry。
4. 结果通过 trace/log/snapshot 输出给 client。

补充规则：

1. `terrain_fire_ice_melt` 只把 `ICE` 转成 `WATER` 或清除冻结状态，不得在本 PR 顺带引入蒸汽/视线遮挡等第二层环境系统。
2. `terrain_physical_ice_slip` 只影响位移稳定性、额外位移或命中修正，不得在 `client` 私有逻辑里做“看起来滑了一下”的伪规则。
3. `LAVA` 相关机制若已有旧实现，只允许作为 zone 脚本化内容或视觉主题继续存在；凡需要正式进入 `TerrainTag`，必须推迟到独立后续 phase 文档。

### 4.2 Elite mutation contract

建议文件：

```text
game/src/main/resources/data/elites/*.yaml
game/src/main/kotlin/com/ktome/game/elites/*
```

核心结构：

```kotlin
data class EliteMutationDef(
    val id: String,
    val kind: MutationKind,
    val tier: MutationTier,
    val threatCost: Int,
    val nameKey: String,
    val iconKey: String,
    val applyToTags: Set<String>,
    val minFloor: Int,
    val maxFloor: Int?,
    val allowedZones: Set<String>,
    val statModifiers: List<StatModifierRef>,
    val grantedTalents: List<TalentGrantRef>,
    val aiProfileOverlay: String?,
    val incompatibleWith: Set<String>,
)
```

```kotlin
data class EliteMutationConfig(
    val maxMutationsPerElite: Int,
)
```

冻结规则：

1. `MutationTier` 固定为 `MINOR / MAJOR / SIGNATURE`，且默认权重按 floor/zone 递进，不允许低层直接刷出 `SIGNATURE`。
2. `EliteMutationConfig.maxMutationsPerElite = 2`，默认 elite 只取 `1` 个 mutation；只有明确标记的高压来源允许取 `2` 个。
3. `incompatibleWith` 必须在注册表层 fail-fast，禁止运行时“抽到了再跳过”导致概率漂移。
4. 同一 elite 若拿到两个 mutation，至少一个必须是 `MINOR` 或 `MAJOR`；`SIGNATURE + SIGNATURE` 组合在 Phase 4 禁止。
5. zone 过滤和 floor 过滤都必须参与抽样前候选集构建，不能抽样后再硬裁剪。
6. `threatCost` 是正式 balancing 字段，必须进入 `EncounterThreatBudget`，不能只靠设计师主观理解“这个 mutation 看起来更强”。

### 4.3 Boss variant 边界

```kotlin
data class BossVariantDef(
    val id: String,
    val baseEncounterId: String,
    val threatCost: Int,
    val grantedMutations: List<MutationRef>,
    val lootProfileOverride: String?,
    val visualTintKey: String?,
    val actionWeightProfileId: String? = null,
)
```

约束：

1. `baseEncounterId` 必须已存在。
2. `grantedMutations` 必须引用 `EliteMutationDef`。
3. `actionWeightProfileId` 只允许覆写既有 encounter 内已经暴露给 variant 的少量候选动作权重，不得新增 phase、删除 phase 或改 phase 跳转条件。
4. 若需要改 phase graph 或 phase 参数结构，退回 `Phase 3` encounter 文档处理，不在本 PR 偷做。
5. `lootProfileOverride` 必须进入统一 `FloorRewardBudget` 记账；`threatCost` 必须进入 `EncounterThreatBudget`，两者都不能绕开横切总账。

### 4.4 并行执行边界

本 PR 的真正上游是 `PR-02` 提供的 `TerrainTag`、biome/zone contract 和基础 report 字段，不需要等待 hidden content 或完整 loot 实现全部结束。

执行边界固定为：

1. 可先并行完成：
   - `core` 的 `CombatPipeline step 9`
   - `game` 的 `EliteMutationDef / BossVariantDef` registry
   - `client` 的 mutation / boss variant 可读性接线
2. 需要在合入前对齐但不阻塞开发启动的依赖：
   - `PR-04/05` 的 `LootBudget`、模板来源和 inspect key
   - `PR-03` 的 proof/log 字段命名一致性
   - `P4-X` 的 reward/threat ledger 版本字段
3. `PR-07` 的 hidden reward/secret zone 只消费这里冻结好的 mutation/boss/terrain contract，不得反向改写本 PR 结构。

## 5. 推荐改动面

### 5.1 `core`

1. `CombatPipeline` 补地形交互挂点和 trace。
2. 为 terrain interaction 增加 isolated batch test。
3. 增加 mutation 组合约束和 action-weight overlay 限制的单测。
4. 为 `threatCost` 汇总和 `bossHarness` 解释线增加 regression test。

### 5.2 `game`

1. 新建 elite mutation / boss variant 数据。
2. elite 和 boss spawn path 接入 mutation/variant 解析。
3. 日志 token、inspect 数据和 snapshot 显式携带 mutation 来源。
4. mutation 候选集必须按 zone/floor/incompatibility 先过滤再抽样。
5. elite mutation 与 boss variant 都必须把 `threatCost` 写入 encounter summary。

### 5.3 `client`

1. mutation icon、名称、inspect 信息读取正式 key。
2. 若引入新 overlay/tint，必须能通过现有 render path 消费，不另建 client-only 规则。

### 5.4 `tools / white-box` 补充改造

1. 本 PR 不强制额外新建独立 `whiteBoxTerrain` root alias；优先要求 `terrainInteractionBatch` 与 `bossHarness` 直接补齐统一 white-box artifact/report contract。
2. 两个现有任务至少要能输出 AI 可读 artifact：
   - terrain interaction trace matrix
   - `TerrainTag -> ElementInteractionRule` provenance table
   - elite mutation 兼容性与来源表
   - boss variant `threatCost / lootProfileOverride / phase graph` 摘要
3. 若后续场景矩阵扩大到超出 `terrainInteractionBatch + bossHarness` 现有边界，再单独抽 `whiteBoxTerrain` 或等价 fixed-scenario domain；本 PR 不允许为“框架整洁”强行复制第二套 runner。
4. `phase4Report` 至少要能聚合：
   - `terrainInteractionBatch`
   - `bossHarness`
   - 上述两个任务新增的结构化 artifact / metrics

## 6. 测试与自证

### 6.1 必测行为

1. 五种 terrain interaction 可在 isolated test 中稳定复现。
2. mapgen 产生的 `TerrainTag` 能被 combat 正确消费。
3. elite mutation 会输出 log token 和 inspect 来源。
4. Boss 变体不会破坏原有 phase graph。
5. `SIGNATURE + SIGNATURE` 不会在单个 elite 上出现，`incompatibleWith` 冲突会被 fail-fast。
6. `terrain_fire_ice_melt` 与 `terrain_physical_ice_slip` 的结果可在 trace 中直接解释。
7. `threatCost` 汇总与实际 encounter 强度变化在 `bossHarness` 或等价报告中可追溯。

### 6.2 自动化命令

```bash
./gradlew :core:test
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew clientSmoke
./gradlew goldenScreenshot
./gradlew phase4Report
```

说明：

1. `terrainInteractionBatch` 是本 PR 的地形交互主验证入口。
2. `bossHarness` 是 Boss variant 的主验证入口。
3. `hiddenContentHarness` 在本 PR 只允许作为补充回归，不再承担 terrain/mutation/boss variant 的主验证职责。

### 6.3 统一白盒框架验证

1. `terrainInteractionBatch` 必须提供可被 AI 直接读取的结构化 case 数据，而不只是一段控制台日志。
2. `bossHarness` 对本 PR 新增内容至少要补出：
   - variant id
   - `threatCost`
   - `lootProfileOverride`
   - phase graph 未变化的证明
3. 本 PR 的 white-box 目标不是替代 `bossHarness`，而是把它和 `terrainInteractionBatch` 的证据面收口到统一 artifact/report contract 中，方便 `phase4Report` 与后续 AI triage。

## 7. 资源生成计划

### 7.1 图片

1. 计划文件：`assets-src/image/specs/phase4-pr06-gemini-plan.yaml`
2. 覆盖对象：
   - `icon.mutation.*`
   - `vfx.terrain.interaction.*`
   - `vfx.boss.variant.*`
3. 报告文件：
   - `assets-src/image/manifests/phase4-pr06-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-pr06-processing-report.jsonl`
4. `*-gemini-plan.yaml` 只是复用现有图片生成计划文件命名，不代表引入新的运行时资源格式。

### 7.2 音频

1. 计划文件：`assets-src/audio/specs/phase4-pr06-audio-plan.yaml`
2. 覆盖对象：
   - `audio.mutation.*`
   - `audio.terrain.*`
   - `audio.boss.variant.*`
3. 报告文件：
   - `assets-src/audio/manifests/phase4-pr06-processing-report.jsonl`

### 7.3 约束

1. 新增 visual/audio key 必须复用现有 JSON manifest schema。
2. 任何 mutation/boss cue 不允许只存在于 pack 或 client 私有表，必须走正式 manifest/lint。
3. `gemini` 仅是资产生成 plan 文件历史命名，不能被实现方误读成需要在运行时区分“Gemini 资源”。

## 8. 出口门禁

1. 地形交互、elite mutation、Boss variant 全部走正式 runtime path。
2. 无第二套 AI / encounter / telegraph 权威。
3. client 可稳定读出 mutation 来源和对应表现资源。
4. 本 PR 能在 `PR-02` 完成后独立推进，不被 `PR-07` 反向卡住。
5. `threatCost` 和 `lootProfileOverride` 已接入统一 reward/threat ledger，不再是局部私有强度字段。
6. `terrainInteractionBatch / bossHarness` 的新增 artifact 已接入统一 white-box 报告消费面。
