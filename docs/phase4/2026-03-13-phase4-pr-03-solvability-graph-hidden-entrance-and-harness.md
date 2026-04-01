> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md`
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - PR-03 SolvabilityGraph、Hidden Entrance 与 Harness

**阶段**: `Phase 4 / P4-A / P4-W2`  
**优先级**: `P0`  
**前置条件**: `PR-02` 完成  
**对应问题**: 地图即使已经具备 loop、vault 和 biome family，如果没有统一的 `SolvabilityGraph`、hidden entrance contract 和 proof harness，`Phase 4` 仍会退化成“地图看起来更复杂，但 QA 无法证明主线必达”。

---

## 1. 阶段目标

建立 `Phase 4` 的可解性正式合同，并把 hidden entrance 的发现规则纳入可重放验证。

完成标准：

1. `PathClass / KeyType / DiscoveryRule / SolvabilityGraph / SolvabilityProof` 进入正式词汇。
2. `SolvabilityGraphBuilder` 只消费 `GeneratedFloor`，不直接读 tile 细节。
3. `hidden entrance` 具备 typed discovery rule，主动搜索统一升级为正式 `SearchAction` contract。
4. `solvabilityHarness` 作为 root alias 建立，并单独保存 proof golden。
5. `SECRET` 只冻结入口和 proof，不在本 PR 冻结 `SecretZoneDef` 奖励 registry。
6. proof 算法必须支持回溯场景，而不是只支持单向前推。

## 2. 当前问题

1. 现有代码没有 world/floor 级的可解性图。
2. hidden content 目前只能靠 zone mechanic 或手写逻辑表达，没有 typed discovery contract。
3. 若先做 secret reward/事件，再补 solvability，会出现“奖励和入口规则先写死，之后再回头拆”的返工。
4. 主动搜索已经不只是“一个发现条件”，如果本 PR 不把它升到动作 contract，`client / replay / tools` 会各自长出不同语义。

### 2.1 本 PR 必须冻结的口径

1. `CRITICAL_PATH / OPTIONAL / SECRET` 成为唯一正式路径分类。
2. `KeyType` 第一版只允许：
   - `KEY_ITEM`
   - `SWITCH`
   - `BOSS_SIGIL`
   - `QUEST_FLAG`
   - `PERCEPTION_REVEAL`
3. `SECRET` 不允许承载主线必需钥匙或必需 quest flag。
4. `discoveryRule` 必须是 typed 结构，不允许自由字符串或散落 `if/else`。
5. `solvabilityHarness` root alias 和报告路径固定：
   - `tools/build/reports/phase4/solvability/solvability-summary.json`
   - `tools/build/reports/phase4/solvability/solvability-proofs.jsonl`
   - `tools/src/test/resources/golden/phase4/solvability/*.json`
6. 物理通道默认视为双向可回溯，proof 不得把 loop 地图误当成 DAG 线性图。
7. 感知检定统一消费 `PerceptionScore`，不再直接把裸 `mentalPower` 写成长期 contract。

## 3. 范围与非目标

### 3.1 范围

1. `core.world.solvability` package 与 proof 算法。
2. `hidden entrance` 数据结构和 `GeneratedFloor` 接线。
3. `solvabilityHarness`、proof golden 和 root alias。
4. 对 `PR-02` planner 输出补充 key-gate 和 entrance 元数据入口。

### 3.2 非目标

1. 不在本 PR 正式引入 `SecretZoneDef` registry、reward profile 或 hidden event 奖励桥接。
2. 不在本 PR 落 elite mutation 或 terrain interaction。
3. 不在本 PR 新增 visual/audio key。

## 4. 技术方案

### 4.1 图结构、typed refs 与发现规则

建议文件：

```text
core/src/main/kotlin/com/ktome/core/world/solvability/SolvabilityModels.kt
core/src/main/kotlin/com/ktome/core/world/solvability/SolvabilityGraphBuilder.kt
core/src/test/kotlin/com/ktome/core/world/solvability/*
```

核心结构：

```kotlin
@JvmInline
value class NodeId(val value: String)

@JvmInline
value class NodeAnchorId(val value: String)

@JvmInline
value class SearchBindingId(val value: String)

@JvmInline
value class RegistryId(val value: String)

@JvmInline
value class RequirementRef(val value: String)

data class ContentRef(
    val registry: RegistryId,
    val id: String,
)

enum class PathClass {
    CRITICAL_PATH,
    OPTIONAL,
    SECRET,
}

enum class KeyType {
    KEY_ITEM,
    SWITCH,
    BOSS_SIGIL,
    QUEST_FLAG,
    PERCEPTION_REVEAL,
}

enum class DiscoveryPredicateType {
    PERCEPTION_CHECK,
    REQUIRED_TAG,
}

enum class RuleCombinator {
    AND,
    OR,
}

data class DiscoveryPredicate(
    val type: DiscoveryPredicateType,
    val difficulty: Int? = null,
    val requiredTag: String? = null,
)

data class DiscoveryRule(
    val combinator: RuleCombinator = RuleCombinator.AND,
    val predicates: List<DiscoveryPredicate>,
)

data class SolvabilityNode(
    val id: NodeId,
    val pathClass: PathClass,
    val roomId: String,
    val grants: Set<RequirementRef>,
)

data class SolvabilityEdge(
    val from: NodeId,
    val to: NodeId,
    val requiredKeys: Set<RequirementRef>,
    val discoveryRule: DiscoveryRule?,
)

data class SolvabilityProof(
    val criticalPathReachable: Boolean,
    val acquiredKeys: List<RequirementRef>,
    val unresolvedRequirements: List<RequirementRef>,
    val visitedNodes: List<NodeId>,
    val optionalPathCount: Int,
    val secretPathCount: Int,
    val totalReachableNodes: Int,
    val reachabilityRatio: Float,
)
```

`difficulty` 标尺固定为：

| difficulty | 难度口径 | 说明 |
| --- | --- | --- |
| `8` | 容易 | 大多数角色可通过 |
| `12` | 中等 | 需要适度投入感知相关属性 |
| `16` | 困难 | 需要专精探索型 build |
| `20` | 极难 | 仅少数探索取向职业/装备组合稳定通过 |

检定方式：

1. `Phase 4` 默认以 `PerceptionScore.total` 为感知检定值，而不是直接绑死裸 `mentalPower`。
2. `PerceptionScore` 允许由 `mentalPower + equipment bonus + buff bonus + passive bonus` 组成。
3. 成功条件为 `PerceptionScore.total >= difficulty`。
4. 多条件组合统一走 `predicates + combinator`，不再使用递归 `secondaryCondition`。

建议结构：

```kotlin
data class PerceptionScore(
    val baseMentalPower: Int,
    val equipmentBonus: Int = 0,
    val buffBonus: Int = 0,
    val passiveBonus: Int = 0,
) {
    val total: Int get() = baseMentalPower + equipmentBonus + buffBonus + passiveBonus
}
```

### 4.2 SearchAction contract

本 PR 把主动搜索从“发现条件”升级成正式动作 contract。

建议结构：

```kotlin
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
2. 它只允许在带 `SearchBindingId` 的 hidden entrance、room node 或 interactable 上执行。
3. 搜索成功、失败、无目标、已解决都必须写入：
   - `GameEvent`
   - `LogTokenEvent`
   - replay 输入/结果
4. 同一 `SearchBindingId` 在同一 reveal 状态下只允许一次有效判定；重复尝试不得重新掷 RNG。
5. `DiscoveryRule` 只描述“搜索动作后如何判定”，不再混入动作经济本身。
6. `1000` 能量成本是固定规则语义，不作为 `SearchAction` DTO 的可配置字段暴露。

### 4.3 proof 算法

固定流程：

1. 从 `entryNodeId` 建立 frontier。
2. 所有物理边默认视为双向可回溯；除非显式标记单向机关，否则 `from -> to` 与 `to -> from` 都必须参与 frontier 扩展。
3. 每一轮收集当前可达节点 grants，并将其加入全局已获取能力集。
4. 用当前 grants 扩展下一批可达边；已访问节点允许被重新经过，但不重复记数。
5. 若 frontier 不再增长且仍有未满足 `CRITICAL_PATH` 依赖，则 hard fail。
6. `OPTIONAL / SECRET` 失败不计主线失败，但必须保留 proof。
7. golden proof 至少包含一个「先走 OPTIONAL 拿 key -> 回到主路径开门」的回溯用例。

### 4.4 hidden entrance 的边界

本 PR 只冻结入口和发现规则：

```kotlin
data class HiddenEntranceDef(
    val id: SearchBindingId,
    val discoveryRule: DiscoveryRule,
    val targetSecretZoneId: ContentRef,
    val entranceBindingId: NodeAnchorId,
    val pathClass: PathClass,
)
```

约束：

1. `targetSecretZoneId` 在本 PR 可指向 secret zone stub content ref，不强制已经有完整 `SecretZoneDef` 奖励实体。
2. `PERCEPTION_REVEAL` 失败不会阻断主线。
3. Boss 门后不得新增 hidden entrance 指向主线资源。
4. 至少允许一类非主线路径要求玩家先执行 `SearchAction`，再用 `PERCEPTION_CHECK` 判定 reveal 结果，避免隐藏内容只靠被动触发。

### 4.5 harness 与 golden

建议任务落位：

```text
tools/src/main/kotlin/com/ktome/tools/solvability/SolvabilityHarnessRunner.kt
build.gradle.kts
```

固定输入：

1. 固定 seed 列表
2. `phaseId: P4`
3. `contentSchemaVersion`
4. `topologyFingerprintVersion`
5. `searchRuleVersion`
6. `secretRuleVersion`
7. `zoneId / floorIndex`
8. `MapgenPipeline` 产出的 `topologySummary`

固定输出：

1. `criticalPathReachable`
2. `visitedNodes`
3. `acquiredKeys`
4. `unresolvedRequirements`
5. `secretProofs`
6. `optionalPathCount / secretPathCount / totalReachableNodes / reachabilityRatio`
7. `searchActionCount / searchRevealCount / searchFailCount`

## 5. 推荐改动面

### 5.1 `core`

1. 新建 `core.world.solvability`。
2. 实现 builder 和 proof runner。
3. 增加 determinism、deadlock detection、secret non-blocking 单测。

### 5.2 `game`

1. 在 `GeneratedFloor` 或相邻 metadata 中增加 hidden entrance / key-gate 输入。
2. zone/content schema 增加 required keys、discovery rule、`SearchBindingId` 的数据入口。

### 5.3 `tools`

1. 新建 `solvabilityHarness`。
2. root `build.gradle.kts` 暴露 alias。
3. proof golden 单独存档，不与地图截图混用。

## 6. 测试与自证

### 6.1 必测行为

1. 主线 `100%` 可达。
2. `OPTIONAL / SECRET` proof 保留但不阻断主线通过。
3. `PERCEPTION_REVEAL` 失败不会阻断 `CRITICAL_PATH`。
4. Boss 门后不存在主线必需钥匙。
5. 至少存在一个可回溯 golden 用例，证明 proof 算法能处理先支线后主线的 key-gate 路径。
6. 同一 `SearchBindingId` 重复尝试不会重新掷 RNG，replay 中可直接解释玩家是否真的执行过搜索动作。

### 6.2 自动化命令

```bash
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
```

### 6.3 白盒验证

1. 至少触发一次 hidden entrance 的发现流程。
2. 人工确认发现失败时主线仍可推进。
3. 检查 `solvability-proofs.jsonl` 中的 `visitedNodes / unresolvedRequirements / optionalPathCount / secretPathCount` 与实际 seed 一致。
4. 至少手动执行一次搜索动作，确认会产生日志、动作结果和可重放输入痕迹。

## 7. 出口门禁

1. `SolvabilityGraph`、`DiscoveryRule` 和 proof 算法冻结。
2. `solvabilityHarness` 成为正式 root alias，并输出结构化报告。
3. `PR-07` 可以在不改 proof contract 的前提下追加 `SecretZoneDef` 和 hidden reward。
4. `SearchAction`、`PerceptionScore`、typed refs 和回溯 proof 都在本 PR 定义完毕，不留给 `PR-07` 再私有扩写。
