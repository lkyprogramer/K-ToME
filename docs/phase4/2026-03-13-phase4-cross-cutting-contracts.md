> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 4 - Cross-Cutting Contracts

**阶段**: `Phase 4 / P4-X`  
**优先级**: `P0`  
**适用范围**: `PR-01 ~ PR-09`  
**对应问题**: `Phase 4` 的 PR 级文档已经足够细，但 `save/replay`、`reward/threat ledger`、`SearchAction`、`content pack runtime/test metadata split` 这些横切合同仍然分散在各 PR 内部推断。若不先收口，全局最容易在 `PR-03 / PR-04 / PR-07 / PR-08` 出现“局部正确、整体撕裂”的实现。

---

## 1. 文档目标

本文件只做一件事：冻结 `Phase 4` 的横切边界，避免同一概念在不同 PR 中长出第二套语义。

本文件不替代 `PR-01 ~ PR-09`，而是提供这些 PR 都必须共同遵守的总开关。

## 2. 必须冻结的横切合同

### 2.1 Run-State Persistence 与 Replay

`Phase 4` 的可重放，不等于“只存 seed 再祈祷能重算出来”。凡是已经被玩家交互改变、或会直接影响 reveal / reward / pack resolve 结果的状态，都必须进入 run-scoped persistence。

冻结口径：

1. `ProfileData` 不保存任何 run 内生成内容状态。
2. `RunSaveData` 至少保存：
   - `buildId`
   - `phaseId`
   - `contentSchemaVersion`
   - `activePackIds`
   - `activePackManifestVersions`
   - `topologyFingerprintVersion`
   - `rewardLedgerVersion`
   - `lootFormulaVersion`
   - `specialTierEligibilityVersion`
   - `searchRuleVersion`
   - `secretRuleVersion`
   - `overlayContractVersion`
   - `phase4RunState.pityTracker`
3. 每个已生成或已访问 floor 至少保存：
   - `zoneId`
   - `floorIndex`
   - `floorSeed`
   - `topologyFingerprint`
   - `terrainTagHash`
   - `resolvedHiddenEntranceBindings`
   - `revealedEntranceIds (SearchBindingId set)`
   - `visitedSecretZoneIds (ContentRef set)`
   - `searchState`
4. `GeneratedFloor.map` 只允许作为兼容装配字段存在，不得作为 save/replay 的规则真源。
5. replay header 至少保存：
   - `phaseId`
   - `buildId`
   - `contentSchemaVersion`
   - `activePackIds (PackId list)`
   - `activePackManifestVersions (PackId -> version map)`
   - `topologyFingerprintVersion`
   - `rewardLedgerVersion`
   - `lootFormulaVersion`
   - `specialTierEligibilityVersion`
   - `searchRuleVersion`
   - `secretRuleVersion`
   - `overlayContractVersion`
   - `seedCorpusId` 或等价 fixed-seed 标识
6. pack 环境、schema 版本或关键公式版本不匹配时，run save 和 replay 必须 fail-fast，不允许静默回退。

建议数据结构：

```kotlin
data class ResolvedEntranceBinding(
    val searchBindingId: SearchBindingId,
    val entranceAnchorId: NodeAnchorId,
    val resolvedTargetNodeId: NodeId,
)

data class SearchStateEntry(
    val bindingId: SearchBindingId,
    val result: SearchActionResult,
)

data class FloorGenerationState(
    val zoneId: String,
    val floorIndex: Int,
    val floorSeed: Long,
    val topologyFingerprint: String,
    val terrainTagHash: String,
    val resolvedHiddenEntranceBindings: List<ResolvedEntranceBinding>,
    val revealedEntranceIds: Set<SearchBindingId>,
    val visitedSecretZoneIds: Set<ContentRef>,
    val searchState: List<SearchStateEntry>,
)

data class Phase4RunState(
    val pityTracker: PityTracker,
    val floorStates: List<FloorGenerationState>,
)
```

### 2.2 Reward / Threat Ledger

`Phase 4` 不允许多个系统各自“多加一点奖励”或“多塞一点压力”而没有统一总账。

冻结词汇：

```kotlin
data class RewardDelta(
    val source: String,
    val amount: Int,
)

data class ThreatDelta(
    val source: String,
    val amount: Int,
)

data class FloorRewardBudget(
    val zoneId: String,
    val floorIndex: Int,
    val baseBudget: Int,
    val rewardDeltas: List<RewardDelta> = emptyList(),
)

data class EncounterThreatBudget(
    val encounterId: String,
    val baseBudget: Int,
    val threatDeltas: List<ThreatDelta> = emptyList(),
)

data class ZoneRewardProfile(
    val id: String,
    val zoneId: String,
    val rarityBonus: Float,
    val qualityBonus: Int,
    val baseRewardBudget: Int,
)
```

强制规则：

1. `rewardBudget` 不再只属于 vault；它是 floor 额外回报的统一货币。
2. `threatBudget` 不再只属于 mapgen；它是 encounter 压力的统一货币。
3. 以下来源必须显式映射到 `FloorRewardBudget`：
   - `vault.rewardBudget`
   - `ZoneRewardProfile`
   - `hiddenEvent.rewards`
   - `secretZone.guaranteedContent`
   - `bossVariant.lootProfileOverride`
4. 以下来源必须显式映射到 `EncounterThreatBudget`：
   - `vault.threatBudget`
   - `elite mutation`
   - `boss variant`
   - `secret encounter`
5. `ZoneMapgenProfile` 与 `ZoneRewardProfile` 必须拆开；地图配置和奖励配置不再混成一个 profile。
6. `lootBalanceLab`、`hiddenContentHarness`、`bossHarness` 的报告都必须能回溯到统一的 reward/threat 词汇。

### 2.3 SearchAction 与 PerceptionScore

主动搜索在 `Phase 4` 不再只是 discovery 条件；它是一个正式玩家动作。

冻结词汇：

```kotlin
data class PerceptionScore(
    val baseMentalPower: Int,
    val equipmentBonus: Int = 0,
    val buffBonus: Int = 0,
    val passiveBonus: Int = 0,
) {
    val total: Int get() = baseMentalPower + equipmentBonus + buffBonus + passiveBonus
}

@JvmInline
value class SearchBindingId(val value: String)

data class SearchAction(
    val bindingId: SearchBindingId,
    val actorId: String,
)

enum class SearchActionResult {
    REVEALED,
    FAILED_CHECK,
    NO_TARGET,
    ALREADY_RESOLVED,
}
```

正式语义：

1. `SearchAction` 在目标有效且需要执行真实判定时，消耗一次标准 `1000` 能量行动。
2. 它只允许在带 `searchable` 或等价 binding 的节点 / 房间 / entrance 上执行。
3. 成功或失败都必须写入：
   - `GameEvent`
   - `LogTokenEvent`
   - replay 输入/结果
4. `DiscoveryRule` 不再自己承载“动作”；它只承载“搜索动作之后要过什么判定”。
5. 同一 `SearchBindingId` 在同一 reveal 状态下只允许触发一次有效判定；重复操作必须返回 `ALREADY_RESOLVED` 或 `NO_TARGET`，不得反复重掷 RNG。
6. `NO_TARGET / ALREADY_RESOLVED` 属于拒绝态，不消耗行动经济，但仍允许写入轻量日志或 UI 提示。
7. 感知判定统一消费 `PerceptionScore.total`，不再直接绑定裸 `mentalPower`。
8. `1000` 能量成本是 `SearchAction` 的固定规则语义，不单独作为 DTO 可配置字段下放给内容层。

### 2.4 Content Pack Runtime / Test Metadata Split

`ContentPackManifest` 只描述运行时内容，不再混入 `tools` 语义。

冻结口径：

1. `manifest.yaml` 只保留 runtime contract：
   - `id`
   - `version`
   - `schemaVersion`
   - `gameVersionRange`
   - `namespace`
   - `dependencies`
   - `overlays`
   - `localeBundles`
   - `visualManifest`
   - `audioManifest`
2. `harnessSeeds`、fixture 顺序、双包 precedence 场景等 QA 信息必须移出 runtime manifest。
3. `Phase 4` 统一使用 sidecar：
   - `tools/src/main/resources/fixtures/content-packs/<packId>.yaml`
4. `contentPackHarness` 只读取 sidecar 测试元数据，不要求 runtime loader 了解测试字段。

建议结构：

```kotlin
enum class OverlayOp {
    ADD,
    REPLACE,
    APPEND,
    DENY,
}

data class ContentPackHarnessSpec(
    val packId: PackId,
    val harnessSeeds: List<Long>,
    val dualPackScenarios: List<DualPackScenario> = emptyList(),
    val overlayContractVersion: Int = 1,
)

data class DualPackScenario(
    val fixturePackId: PackId,
    val expectedOrder: List<PackId>,
    val expectedOps: List<OverlayOp> = emptyList(),
)
```

说明：

1. sidecar YAML 仍使用字符串序列化 `PackId`，但 schema 语义已冻结为 typed `PackId`，不再把 pack 标识视为自由字符串。

### 2.5 Overlay Allowed Targets 与诊断模型

`APPEND / DENY` 不能在 `Phase 4` 里继续保持“对任意 registry 任意字段都能用”的宽语义。

冻结规则：

1. runtime 主路径正式支持：
   - `ADD`
   - whole-entry `REPLACE`
2. `APPEND / DENY` 在 `Phase 4` 保留 contract，但只允许在 whitelist 目标上进入 fixture/harness 路径。
3. 若实现方坚持让 `APPEND` 进入 runtime 主路径，必须同时声明：
   - `fieldPath`
   - `mergePolicy`
   - `dedupeKey`
4. `DENY` 只允许作用于明确标记为 optional 的 registry entry，不允许删除主线必需内容。
5. loader/lint/harness 对以下失败面必须输出可读诊断：
   - 缺失依赖
   - 依赖环
   - `versionRange` 冲突
   - namespace 冲突
   - 同优先级 pack 覆盖同一 target
   - 非白名单 `APPEND / DENY`

### 2.6 Typed Refs 与 Version Fields

凡是跨 loader、runtime、harness、report 传递的 ID，不再继续裸用 `String` 作为长期 contract。

Phase 4 最小 typed ref：

```kotlin
@JvmInline value class NodeId(val value: String)
@JvmInline value class RegistryId(val value: String)
@JvmInline value class PackId(val value: String)

data class ContentRef(
    val registry: RegistryId,
    val id: String,
)
```

这些 typed ref 必须至少进入以下 persistence / report 断面：

1. `FloorGenerationState.revealedEntranceIds: Set<SearchBindingId>`
2. `FloorGenerationState.visitedSecretZoneIds: Set<ContentRef>`
3. `HarnessReportHeader.activePackIds: List<PackId>`
4. `HarnessReportHeader.activePackManifestVersions: Map<PackId, String>`

最小版本字段：

1. `topologyFingerprintVersion`
2. `rewardLedgerVersion`
3. `lootFormulaVersion`
4. `specialTierEligibilityVersion`
5. `searchRuleVersion`
6. `secretRuleVersion`
7. `overlayContractVersion`

这些版本字段必须进入：

1. harness report header
2. save/replay header
3. golden/统计报告元数据

## 3. 对各 PR 的约束映射

1. `PR-01`
   - 使用 `MapgenPipeline.run(request)` 正式入口
   - 明确 floor-level persistence 元数据
   - 引入 `topologyFingerprintVersion`
2. `PR-02`
   - 拆出 `ZoneRewardProfile`
   - 只冻结 budget 词汇，不提前冻结完整 reward 兑换公式
3. `PR-03`
   - 把主动搜索升级为 `SearchAction`
   - discovery 判定消费 `PerceptionScore`
4. `PR-04 / PR-05`
   - 把 `UNIQUE / ARTIFACT` 从总 rarity roll 中拆成 eligibility + upgrade 流程
   - `PityTracker` 进入 run-scoped persistence
5. `PR-06 / PR-07`
   - reward/threat 统一记账
   - search / reveal / secret zone 状态进入 run save / replay
6. `PR-08 / PR-09`
   - runtime manifest 与 harness spec 分层
   - loader 失败诊断和 deterministic pack order 必须写实

## 4. 出口门禁

1. 本文档被 `roadmap`、phase4 主文档和受影响 PR 文档显式引用。
2. `save/replay`、`reward/threat ledger`、`SearchAction`、`pack runtime/test split` 不再有第二套口径。
3. 后续 `PR-01 ~ PR-09` 如与本文档冲突，必须先回写本文档或主文档，再改 PR 文档，不能静默并存双份权威。
