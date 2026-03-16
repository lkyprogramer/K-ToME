> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 5 - Tactical AI, Stability & Release

**阶段**: `Phase 5`  
**版本目标**: `v1.0.0`  
**优先级**: `P1`  
**前置条件**: `Phase 4` 出口全部满足  
**对应问题**: 到 `Phase 4` 为止，游戏应当已经“系统齐全、内容可玩”，但还不等于“能发布”。`Phase 5` 只处理发布必须项，不再引入大新系统。

执行权威说明：

1. `docs/2026-03-13-core-systems-design-and-phase-supplements.md` 的 `§9.4` 只保留锚点级骨架；完整字段集、YAML/JSON 示例、量化门槛与工作包依赖以本文为执行权威。
2. 本文的类型名与 `§9.4` 保持一致；若字段细节不同，以本文为权威。
3. `Phase 5` 的量化通过标准、golden 更新策略和白盒步骤以 [2026-03-13-phase5-regression-checklist.md](./2026-03-13-phase5-regression-checklist.md) 为权威。

---

## 1. 阶段目标

把 K-ToME 收口到真正可发布的单机版本。

完成标准：

1. 精英/Boss 的战术 AI 与感知系统达到清晰可感知提升，且决策可解释、可回归。
2. 建立 `perf smoke`、`soak`、`run history`、`death analysis`、`replay` 的稳定工具链。
3. 做完 `Localization QA`、`Accessibility QA`、安装验证和发布文档。
4. 形成可验收的 `v1.0.0` 安装包。

### 1.1 检查点摘要

| checkpoint | 主题 | 最小交付 | 进入条件 | 退出条件 |
| --- | --- | --- | --- | --- |
| `P5-A` | 战术 AI 与感知深化 | `TacticalScoringLayer`、`PerceptionState`、`HateFocus`、`TacticalAIDecisionTrace` | `Phase 4` 出口满足，`BossEncounterDef`、`TerrainTag`、`AIProfile`、`MutationKind` 已冻结 | 固定场景下精英/Boss 决策可解释提升，潜行/仇恨/最后已知位置合同稳定 |
| `P5-B` | 稳定性与取证工具 | `ReplayHeader`、`ReplayFrame`、`RunHistoryEntry`、`DeathAnalysis`、`SoakReport` | `P5-A` 合同稳定 | replay 可复现、死因可解释、perf/soak 预算可量化、QA 可留痕 |
| `P5-C` | 发布收口 | `BalanceLab`、`packageRelease`、已知问题清单、安装验证与发布文档 | `P5-B` 门禁满足 | `v1.0.0` 安装包可验收、双语言与可访问性清盘、发布资料齐全 |

## 2. 当前问题

1. `Phase 4` 解决了内容深度，但发布级稳定性仍未完成。
2. AI 仍偏脚本化执行，缺少更强的战术评分层。
3. 长局 soak、死因解释、性能预算和 replay 还缺统一收口。
4. 双语言、可访问性、安装验证和封板资料仍没有正式门禁。

### 2.1 本阶段必须冻结的基础系统

1. `tactical scoring` 接口、候选动作模型与 trace 扩展。
2. 感知、仇恨、潜行、最后已知信息状态机。
3. replay schema、run history 与 death analysis。
4. perf/soak 预算、采样方式与报告格式。
5. Localization QA、Accessibility QA、BalanceLab 与 package 验收口径。

### 2.2 必须沿用的上游合同

1. `Phase 3` 的 `CombatPipeline`、`CombatTrace`、`BossEncounterDef`、`BossPhaseDef`、`STEALTH / TAUNT` 合同继续有效，`Phase 5` 只能在其上叠加解释层与取证层。
2. `Phase 4` 的 `TerrainTag`、`ElementInteractionRule`、`AI_SHIFT` mutation、`DiscoveryRuleType.PERCEPTION_CHECK`、`ContentPackManifest` 与 `LootBudget` 继续是规则真源。
3. `Phase 5` 不得引入新的规则解释器、运行时脚本宿主或与 `Phase 3/4` 并行的第二套怪物/Boss 行为树。

## 3. 范围与非目标

### 3.1 范围

1. 战术 AI 与感知/潜行系统。
2. replay、run history、death analysis。
3. perf smoke、profiling、soak。
4. 本地化 QA、可访问性 QA、BalanceLab、安装打包与发布说明。

### 3.2 非目标

1. 不再引入大新玩法系统。
2. 不做完整脚本平台或通用 planner。
3. 不因为追求 AI“看起来聪明”而牺牲可解释性、确定性与回归定位能力。
4. 不在 `client` 中复制规则真源；所有 AI/perception/replay 语义仍由 `core` 决定。

## 4. 技术合同

### 4.1 Tactical AI 在脚本化 AI 之上演进

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/ai/tactical/*
core/src/test/kotlin/com/ktome/core/ai/tactical/*
```

冻结口径：

1. tactical scoring 必须建立在 `Phase 3/4` 已有 `AIProfile DSL`、`BossPhaseDef`、action catalog 与 `PerceptionState` 上。
2. `DSL` 规则先做硬约束和 veto，`Utility` 评分只在通过约束的候选动作集中排序；不得让 Utility 绕过 `BossPhaseDef` 的动作白名单。
3. 重点提升对象是精英/Boss，不是把所有普通怪都升级为复杂 planner。
4. `AIDecisionTrace` 必须保留 `Phase 3/4` 的原始字段，同时扩展候选评分、最终分值和选择原因。
5. `AIContext` 必须包含 `Phase 4` 新增的地形、mutation 与 Boss phase 语义，防止评分层对 procgen/terrain 内容失明。

#### 4.1.1 Kotlin 合同

```kotlin
data class TacticalProfileDef(
    val id: String,
    val baseDslProfile: String,
    val fallbackActionId: String,
    val candidateActions: List<TacticalCandidateDef>,
    val selection: TacticalSelectionConfig,
)

data class TacticalCandidateDef(
    val actionId: String,
    val sourceRuleIds: List<String>,
    val hardConstraints: List<String>,
    val considerations: List<ConsiderationDef>,
)

data class ConsiderationDef(
    val evaluatorId: String,
    val weight: Float = 1.0f,
)

data class TacticalSelectionConfig(
    val dslVetoFirst: Boolean = true,
    val traceMode: String = "tactical",
)

fun interface TacticalScoringLayer {
    fun selectAction(
        context: AIContext,
        candidates: List<ActionCandidate>,
    ): TacticalScoringResult
}

data class ActionCandidate(
    val actionId: String,
    val sourceRuleIds: List<String>,
    val hardConstraints: List<String>,
    val considerations: List<Consideration>,
)

data class TacticalScoringResult(
    val selectedActionId: String,
    val trace: TacticalAIDecisionTrace,
)

data class Consideration(
    val evaluatorId: String,
    val evaluator: UtilityEvaluator,
    val weight: Float = 1.0f,
)

fun interface UtilityEvaluator {
    fun evaluate(context: AIContext): Float
}

data class AIContext(
    val self: EntitySnapshot,
    val target: EntitySnapshot?,
    val alliesInRadius: List<EntitySnapshot>,
    val enemiesInRadius: List<EntitySnapshot>,
    val distanceToTarget: Int,
    val selfHpPercent: Float,
    val targetHpPercent: Float?,
    val availableTalents: List<String>,
    val currentTurn: Int,
    val terrainAtSelf: TileType,
    val terrainAroundSelf: Map<Point, TileType>,
    val terrainTagsAtSelf: Set<TerrainTag>,
    val nearbyTerrainInteractionIds: List<String>,
    val activeMutationIds: List<String>,
    val bossPhaseId: String?,
)

enum class TacticalSelectionReason {
    UTILITY_BEST,
    DSL_OVERRIDE,
    FALLBACK,
}

data class ScoredCandidate(
    val actionId: String,
    val score: Float,
    val evaluatorId: String,
)

data class TacticalAIDecisionTrace(
    val turnId: Long,
    val actorId: Int,
    val profileId: String,
    val perceivedTargetId: Int?,
    val matchedRuleIds: List<String>,
    val selectedAction: String,
    val candidates: List<ScoredCandidate>,
    val finalScore: Float,
    val selectionReason: TacticalSelectionReason,
)
```

定义层 / 运行时分层固定为：

1. YAML 的反序列化目标固定为 `TacticalProfileDef`。
2. loader 负责把 `ConsiderationDef.evaluatorId` 解析到 evaluator registry，生成运行时 `ActionCandidate` 与 `Consideration`。
3. `ActionCandidate` 的 `considerations` 是运行时已解析对象，不再要求评分层做二次查表。
4. 未注册的 `evaluatorId` 视为 contract-lint 错误，而不是运行时 fallback。

#### 4.1.2 DSL 与 Utility 的执行顺序

固定执行管线：

1. `AIProfile DSL` 先产出候选动作与硬约束。
2. 违反 `BossPhaseDef`、无目标、资源不足、路径不可达等硬约束的动作直接被 veto。
3. `TacticalScoringLayer` 只在剩余候选集中排序。
4. 若候选集为空，执行 `fallbackActionId` 或 `WAIT`。
5. `selectionReason` 只能是：
   - `UTILITY_BEST`
   - `DSL_OVERRIDE`
   - `FALLBACK`

#### 4.1.3 最小 YAML 示例

```yaml
id: boss.molten_giant.phase_2
baseDslProfile: boss.molten_giant.phase_2_scripted
fallbackActionId: wait
candidateActions:
  - actionId: melee_slam
    sourceRuleIds: [close_range_punish]
    hardConstraints: [target_in_melee_range]
    considerations:
      - evaluatorId: target_cluster
        weight: 1.25
      - evaluatorId: finish_low_hp_target
        weight: 1.10
  - actionId: lava_wave
    sourceRuleIds: [zone_control]
    hardConstraints: [phase_allows_lava_wave, target_visible]
    considerations:
      - evaluatorId: line_coverage
        weight: 1.40
      - evaluatorId: punish_standing_in_oil
        weight: 1.35
selection:
  dslVetoFirst: true
  traceMode: tactical
```

该 YAML 根结构对应 `TacticalProfileDef`，不是运行时 `ActionCandidate` 列表本身。

### 4.2 感知、仇恨与潜行

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/perception/*
core/src/test/kotlin/com/ktome/core/perception/*
```

冻结口径：

1. AI 只能基于可见信息、声音/事件刺激和最后已知位置行动，不得作弊式全图透视。
2. `STEALTH / TAUNT / useLastKnownPosition` 继续沿用 `Phase 3` 冻结合同；`Phase 5` 只把它们接入显式感知状态机。
3. `HateFocus.confidence` 取值范围固定为 `0.0 .. 1.0`，并且必须显式衰减。
4. `DiscoveryRuleType.PERCEPTION_CHECK` 只能通过 `PerceptionState` 与刺激事件进入可发现态，不能绕过感知系统直接 reveal。

#### 4.2.1 Kotlin 合同

```kotlin
data class PerceptionProfileDef(
    val config: PerceptionConfig,
    val useLastKnownPosition: Boolean,
    val stimulusRules: List<StimulusRuleDef>,
)

data class StimulusRuleDef(
    val type: HateSource,
    val intensity: Float,
    val fromAction: String,
)

enum class PerceptionState {
    UNAWARE,
    SUSPICIOUS,
    ALERT,
    SEARCHING,
}

enum class HateSource {
    VISUAL,
    SOUND,
    DAMAGE_RECEIVED,
    ALLY_REPORT,
    TAUNT,
}

data class HateFocus(
    val targetId: EntityId,
    val lastKnownPosition: Point?,
    val confidence: Float,
    val source: HateSource,
    val updatedTurn: Int,
)

data class PerceptionStimulus(
    val source: HateSource,
    val emitterId: EntityId?,
    val origin: Point,
    val intensity: Float,
)

data class PerceptionEntry(
    val ownerId: EntityId,
    val state: PerceptionState,
    val hateFocus: HateFocus?,
    val lastStimulusTurn: Int?,
    val visibleTargetIds: List<EntityId>,
)

data class PerceptionConfig(
    val suspiciousThreshold: Float = 0.25f,
    val alertThreshold: Float = 0.50f,
    val searchDurationTurns: Int = 3,
    val suspiciousDecayPerTurn: Float = 0.10f,
    val searchDecayPerTurn: Float = 0.15f,
)

interface PerceptionSystem {
    fun update(entry: PerceptionEntry, currentTurn: Int): PerceptionEntry
    fun applyStimulus(
        entry: PerceptionEntry,
        stimulus: PerceptionStimulus,
        currentTurn: Int,
    ): PerceptionEntry
}
```

定义层 / 运行时分层固定为：

1. YAML 的反序列化目标固定为 `PerceptionProfileDef`。
2. `StimulusRuleDef` 负责描述“什么动作会产生什么刺激”。
3. `PerceptionStimulus` 只代表运行时已经发生的刺激事件，因此包含 `emitterId` 和 `origin`，不再回写 `fromAction`。

#### 4.2.2 状态转换与行为约束

数学口径：

1. `applyStimulus` 时，`confidence = min(1.0f, currentConfidence + stimulus.intensity)`；同一回合多个刺激按累加后再 `clamp(0.0, 1.0)`。
2. `SEARCHING` 持续 `searchDurationTurns` 结束后，若 `confidence >= alertThreshold`，强制把 `confidence` 设为 `alertThreshold - 0.01f`，确保退出 `SEARCHING` 后至多回到 `SUSPICIOUS`。

| 状态 | 进入条件 | 行为约束 | 退出条件 |
| --- | --- | --- | --- |
| `UNAWARE` | 初始态；长时间无刺激且无目标 | 正常巡逻或脚本 idle | 感知到声音/队友报告/环境提示且 `confidence >= 0.25` 时进入 `SUSPICIOUS` |
| `SUSPICIOUS` | 有弱刺激但未直视确认目标 | 可转头、靠近刺激源、保留高冷却技能 | 直视目标或 `confidence >= 0.50` 时进入 `ALERT`；衰减到 `< 0.25` 回到 `UNAWARE` |
| `ALERT` | 直视目标、被攻击或强制嘲讽 | 正常战斗；允许消耗关键技能 | 失去目标但保留 `lastKnownPosition` 时进入 `SEARCHING` |
| `SEARCHING` | `ALERT` 状态下目标消失、进入隐匿或离开 LOS | 只能围绕 `lastKnownPosition` 搜索；Boss 只能执行无目标 fallback 或范围扫描 | 重新直视目标时回 `ALERT`；搜索 `3` 回合后仍无结果则衰减到 `SUSPICIOUS` 或 `UNAWARE` |

#### 4.2.3 与 `STEALTH / TAUNT` 的接线

1. 目标进入 `STEALTH` 后，当前目标引用立即失效；若 `useLastKnownPosition = true`，则进入 `SEARCHING` 并保留 `lastKnownPosition`。
2. `STEALTH` 被 AoE 实际伤害打破后，AI 才允许从 `SEARCHING` 返回 `ALERT`；未造成伤害时不得破隐。
3. Boss 遇到 `STEALTH` 时不切 phase，只能在当前 phase 内执行 `fallbackActionId`、范围扫描或守位动作。
4. `TAUNT` 视为 `HateSource.TAUNT`，强制把 `confidence` 设为 `1.0` 且目标锁定为嘲讽源。
5. `TAUNT` 结束时，清除强制目标锁定，并把 `confidence` 恢复到 `alertThreshold`；后续按普通衰减规则处理，不得保留“隐式强制目标”。

#### 4.2.4 最小 YAML 示例

```yaml
id: elite.bandit_captain
profile:
  useLastKnownPosition: true
config:
  suspiciousThreshold: 0.25
  alertThreshold: 0.50
  searchDurationTurns: 3
  suspiciousDecayPerTurn: 0.10
  searchDecayPerTurn: 0.15
stimulusRules:
  - type: SOUND
    intensity: 0.30
    fromAction: door_break
  - type: DAMAGE_RECEIVED
    intensity: 1.00
    fromAction: backstab
```

### 4.3 Replay、Run History 与 Death Analysis

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/replay/*
client/src/main/kotlin/com/ktome/client/history/*
tools/src/main/kotlin/com/ktome/tools/replay/*
```

冻结口径：

1. replay 只记录语义事件、输入与校验哈希，不记录渲染层状态。
2. replay 使用 `kotlinx.serialization` JSON + gzip；schema 版本与 `saveContractVersion` 独立维护。
3. `DeathAnalysis` 必须直接复用 `CombatTrace`，不得再拼装第二份“解释版 combat trace”。
4. `suggestions` 不允许直接输出拼接文案，只能输出 typed key + args，由 `client` 做 i18n 渲染。
5. `run history` 只做本地持久化，不引入数据库服务端或跨设备同步。

#### 4.3.1 Kotlin 合同

```kotlin
data class TurnSummary(
    val turn: Int,
    val playerActionId: String?,
    val damageTaken: Int,
    val healingReceived: Int,
    val appliedEffectIds: List<String>,
    val removedEffectIds: List<String>,
    val startPosition: Point,
    val endPosition: Point,
)

enum class DeathSuggestionKey {
    LOW_RESISTANCE,
    LOW_ARMOR,
    NO_ESCAPE_TOOL,
    STACKED_DOT,
    MISPLAY_TELEGRAPH,
}

data class DeathSuggestion(
    val key: DeathSuggestionKey,
    val args: Map<String, String>,
)

data class DeathAnalysis(
    val turn: Int,
    val killerEntityId: EntityId,
    val killerName: String,
    val killingAbilityId: String,
    val damageType: DamageType,
    val finalDamage: Int,
    val playerHpBefore: Int,
    val combatTrace: CombatTrace,
    val last5Turns: List<TurnSummary>,
    val activeEffectsAtDeath: List<ActiveEffectV2>,
    val suggestions: List<DeathSuggestion>,
)

data class ReplayHeader(
    val schemaVersion: Int,
    val phaseId: String,
    val buildId: String,
    val seed: Long,
    val professionId: String,
    val raceId: String,
    val difficultyId: String,
    val zoneRoute: List<String>,
)

enum class ReplayEventType {
    PLAYER_INPUT,
    WORLD_EVENT,
    TRACE_CHECKPOINT,
    SAVE_MARKER,
}

data class ReplayEvent(
    val turn: Int,
    val eventIndex: Int,
    val type: ReplayEventType,
    val payloadKey: String,
    val payload: Map<String, String>,
)

data class ReplayFrame(
    val turn: Int,
    val playerInputId: String?,
    val events: List<ReplayEvent>,
    val combatTraceHash: String?,
    val aiTraceHash: String?,
)

enum class RunResult {
    WIN,
    DEATH,
    ABANDONED,
}

data class RunHistoryEntry(
    val runId: String,
    val buildId: String,
    val seed: Long,
    val professionId: String,
    val raceId: String,
    val difficultyId: String,
    val result: RunResult,
    val zoneReachedId: String,
    val turnsPlayed: Int,
    val durationSeconds: Long,
    val deathAnalysisId: String?,
    val replayId: String?,
    val contentPackIds: List<String>,
    val runSemanticHash: String?,
    val runTraceHash: String?,
)
```

#### 4.3.2 存储与哈希合同

1. replay 文件固定为 `.replays/<runId>.replay.json.gz`。
2. `run history` 索引固定为 `.history/run-history.json`，默认保留最近 `200` 条；更老记录只保留摘要，不强制保留完整 replay。
3. `DeathAnalysis` 固定存储在 `.history/<runId>.death.json`；当 `result == DEATH` 时，`deathAnalysisId` 默认等于 `runId`。
4. `runSemanticHash` 固定为归一化 `ReplayFrame + CombatTrace + TacticalAIDecisionTrace` 序列的 `SHA-256`。
5. `runTraceHash` 固定为归一化 `CombatTrace + TacticalAIDecisionTrace` 序列的 `SHA-256`。
6. `runSemanticHash` 与 `runTraceHash` 在 run 结束时一次性计算，存储在 `RunHistoryEntry`；`ReplayFrame.combatTraceHash` 与 `ReplayFrame.aiTraceHash` 继续保留为 per-frame hash。
7. `replayHarness` 通过标准是相同 build 下 `runSemanticHash` 与 `runTraceHash` 全量一致；schema bump 不提供旧 replay 自动迁移。

#### 4.3.3 最小 JSON 示例

```json
{
  "header": {
    "schemaVersion": 1,
    "phaseId": "P5",
    "buildId": "local-dev",
    "seed": 424242,
    "professionId": "rogue",
    "raceId": "elf",
    "difficultyId": "normal",
    "zoneRoute": ["shattered_outpost", "greenwood_fringe", "deep_iron_pit"]
  },
  "frames": [
    {
      "turn": 127,
      "playerInputId": "move_west",
      "events": [
        {
          "turn": 127,
          "eventIndex": 0,
          "type": "TRACE_CHECKPOINT",
          "payloadKey": "boss.telegraph",
          "payload": {
            "bossId": "molten_giant",
            "phaseId": "phase_2"
          }
        }
      ],
      "combatTraceHash": "sha256:combat-trace",
      "aiTraceHash": "sha256:ai-trace"
    }
  ]
}
```

### 4.4 Perf / Soak / Profiling

建议文件与模块：

```text
client/src/main/kotlin/com/ktome/client/perf/*
tools/src/main/kotlin/com/ktome/tools/perf/*
```

冻结口径：

1. 必须同时冻结预算值和测量方法。
2. `perfSmoke` 是快速门禁，`soakRun` 是发布级门禁；两者不能互相替代。
3. `P5-W3` 可以在 `P5-W1` 后先跑基线 soak，但 `P5-W2` 完成后必须全量重测。

#### 4.4.1 Kotlin 合同

```kotlin
data class SoakConfig(
    val durationHours: Int = 8, // total wall-clock budget for the whole matrix
    val professionIds: List<String>,
    val raceIds: List<String>,
    val seeds: List<Long>,
    val sampleIntervalSeconds: Int = 30,
    val reportIntervalMinutes: Int = 5,
)

data class SoakReport(
    val buildId: String,
    val durationMinutes: Int,
    val runsCompleted: Int,
    val crashes: Int,
    val peakHeapMb: Int,
    val postWarmupHeapDriftMb: Int,
    val maxGcPauseMs: Int,
    val avgFps: Float,
    val p1Fps: Float,
    val p95DrawCalls: Int,
    val p95TextureBindings: Int,
    val p95FovMs: Float,
    val p95PathfindingMs: Float,
    val atlasHandleDelta: Int,
    val audioHandleDelta: Int,
)
```

#### 4.4.2 性能基线

| 指标 | 目标值 | 测量方式 |
| --- | --- | --- |
| 帧率 | 桌面端稳定 `60 FPS` | GLProfiler + 帧间时间统计 |
| Draw Calls / frame | `< 50` | GLProfiler |
| Texture Bindings / frame | `< 10` | GLProfiler |
| FOV 耗时 | `< 2 ms`（`80 x 50`） | Benchmark 测试 |
| A* 耗时 | `< 1 ms`（`80 x 50`） | Benchmark 测试 |
| 内存占用 | `< 512 MB` 峰值 | JVM 监控 |
| Soak 稳定性 | `8h` 无 OOM、无 `> 50 ms` GC 停顿 | Soak 测试 |

#### 4.4.3 Soak 与 Perf Harness 最小规格

1. `perfSmoke`
   - 固定 `3` 个场景：主菜单加载、Boss 战、长局背包/日志切换。
   - 每个场景连续采样 `30` 次。
   - 所有指标都必须落在 `4.4.2` 预算内。
2. `soakRun`
   - 默认总时限为 `8` 小时，不是每个组合 `8` 小时。
   - `profession × race × seed` 全组合按顺序轮转执行；单个组合的 run 到达胜利、死亡或总时限耗尽时结束。
   - 采样频率固定为 `30` 秒。
   - `30` 分钟 warmup 之后，`postWarmupHeapDriftMb` 必须 `< 50`。
   - `p1Fps` 必须 `>= 30`。
   - `atlasHandleDelta` 与 `audioHandleDelta` 都必须 `<= 3`。

#### 4.4.4 最小 YAML 示例

```yaml
id: release_soak_normal
durationHours: 8
professionIds: [vanguard, arcanist, rogue, templar]
raceIds: [human, elf]
seeds: [101, 202, 303, 404]
sampleIntervalSeconds: 30
reportIntervalMinutes: 5
```

### 4.5 QA 与发布收口

建议文件与模块：

```text
docs/releases/*
tools/src/main/kotlin/com/ktome/tools/release/*
```

冻结口径：

1. Localization QA 必须覆盖基础 UI、战斗日志、背包、铭文/天赋、`Phase 4` 新增的 mutation/hidden content/content pack 文案。
2. Accessibility QA 至少冻结对比度、字号、色彩依赖、日志可读性和键盘导航。
3. `BalanceLab` 不是新造一套 runner，而是建立在 `Phase 3` 的 `longRunLab` 和 `Phase 4` 的 `LootBalanceLab` 基础设施之上的 release 平衡实验室。
4. `packageRelease` 必须产出安装包、版本说明、已知问题清单和最小安装验证记录。

#### 4.5.1 Localization / Accessibility 标准

1. Localization QA：
   - `0` unresolved key
   - `0` placeholder mismatch
   - `0` 关键 UI 截断
   - `zh-CN` / `en-US` 双语言都必须通过
2. Accessibility QA：
   - 正文对比度 `>= 4.5:1`
   - 默认最小字号 `>= 14px` 或提供等价可调选项
   - 所有关键信息不得只依赖颜色区分
   - 所有 release 必经流程都必须支持纯键盘完成

#### 4.5.2 BalanceLab 定位

`BalanceLab` 固定输入与输出：

1. 输入：
   - `8` 个代表性 build
   - 每个 build 至少 `10` 个固定 seed
   - `Normal` 难度
2. 输出：
   - `clearRateByBuild`
   - `deathCauseHistogram`
   - `medianTurnsByBuild`
   - `resourceUsageByBuild`
   - `outlierBuilds`
3. 门槛：
   - 代表性 canonical build 的通关率不得 `< 20%`
   - 单一 canonical build 的通关率不得 `> 70%`
   - 成功 run 的 `medianTurns` 离群度不得超过全组中位数的 `±25%`

### 4.6 与既有系统的接线边界

| 接线点 | 既有系统 | `Phase 5` 接法 | 禁止事项 |
| --- | --- | --- | --- |
| `AI_SHIFT` mutation | `Phase 4 MutationKind.AI_SHIFT` | 通过 `AIContext.activeMutationIds` 传递给 `UtilityEvaluator` 调整分值；allow-list 与 fallback 的覆盖留在 DSL overlay 层 | 不得把 mutation 变成第二套脚本宿主 |
| `PERCEPTION_CHECK` | `Phase 4 DiscoveryRuleType.PERCEPTION_CHECK` | 通过 `PerceptionStimulus` 与 `PerceptionState` 触发发现 | 不得直接 reveal 未感知到的 secret 内容 |
| `TerrainTag` | `Phase 4 TerrainTag + ElementInteractionRule` | 通过 `AIContext.terrainTagsAtSelf` 与 `nearbyTerrainInteractionIds` 进入评分层 | 不得在 `client` 侧私自追加 AI 战术机会 |
| Boss phase / telegraph | `Phase 3 BossEncounterDef / BossPhaseDef` | phase graph 仍是权威；Utility 只在当前 phase 可用动作中排序 | 不得让 Utility 直接跳 phase |
| `STEALTH / TAUNT` | `Phase 3` 冻结合同 | 通过 `PerceptionState` 与 `HateSource` 显式接线 | 不得破坏 `lastKnownPosition` 与嘲讽锁定合同 |
| `CombatTrace` | `Phase 3` 正式公式与 trace | `DeathAnalysis.combatTrace` 直接复用 | 不得另造非权威 combat 解释副本 |
| `LootBudget` | `Phase 4 LootBudget` | `DeathSuggestion` 可以引用“装备不适配”类指标，但来源必须是 `LootBudget` 与装备快照 | 不得在 `Phase 5` 新造另一套装备评分器 |
| Content pack | `Phase 4 ContentPackManifest` | pack 加载后的怪物、事件、Boss 必须继续通过 tactical/perception/replay/death/perf 验证 | 不得把 pack 内容排除在 release QA 之外 |

## 5. 推荐 PR / 工作包拆分

### 5.1 工作包与依赖

| 工作包 | checkpoint | 依赖 | 主要模块 | 交付目标 |
| --- | --- | --- | --- | --- |
| `P5-W1` | `P5-A` | `Phase 4` | `core` | `TacticalScoringLayer`、`AIContext` 扩展、`TacticalAIDecisionTrace` |
| `P5-W2` | `P5-A` | `P5-W1` | `core`, `client` | `PerceptionState`、`HateFocus`、潜行/仇恨/最后已知位置接线 |
| `P5-W3` | `P5-B` | `P5-W1` | `client`, `tools` | `perfSmoke`、profiling budget、基线 soak |
| `P5-W4` | `P5-B` | `P5-W1`, `P5-W2` | `core`, `client`, `tools` | `ReplayHeader`、`RunHistoryEntry`、`DeathAnalysis`、Localization/Accessibility QA |
| `P5-W5` | `P5-C` | `P5-W1` ~ `P5-W4` | `client`, `tools`, `docs` | `BalanceLab`、`packageRelease`、release docs、Gold Master 封板 |

执行原则：

1. `P5-W3` 可以在 `P5-W1` 完成后先起基线 perf/soak，但 `P5-W2` 合入后必须全量重测。
2. `P5-W4` 不允许在没有 `TacticalAIDecisionTrace` 与 `PerceptionState` 正式合同的情况下先写 replay/death 临时字段。
3. `P5-W4` 可以与 `P5-W3` 并行推进；`P5-B` 的出口仍要求 replay/death 与 perf/soak 两条线同时通过。
4. `P5-W5` 的 `BalanceLab` 必须复用 `longRunLab` / `LootBalanceLab` 的 batch 基础设施，不允许新建第三套统计 runner。

### 5.2 并行开发线

1. `Rules/AI Lane`
   - `TacticalScoringLayer`
   - `PerceptionState`
   - `HateFocus`
   - `TacticalAIDecisionTrace`
2. `Tools/QA Lane`
   - `tacticalAiHarness`
   - `replayHarness`
   - `perfSmoke`
   - `soakRun`
   - `BalanceLab`
3. `Release Lane`
   - Localization QA
   - Accessibility QA
   - package / install verification
   - known issues / release notes

## 6. 测试与自证

### 6.1 必测模块

1. `core.ai.tactical`
2. `core.perception`
3. `core.replay`
4. `client.history`
5. `client.perf`
6. `tools.perf`
7. `tools.release`

### 6.2 Harness 与量化门槛

| Harness | 最小输入 | 通过标准 |
| --- | --- | --- |
| `tacticalAiHarness` | `12` 个固定场景：`4` 精英、`4` Boss、`4` 潜行/仇恨 | 期望动作族匹配率 `>= 85%`，非法目标选择 `0`，相同 build/seed 的 `aiTraceHash` `100%` 一致 |
| `replayHarness` | `20` 个已录制 run | `runSemanticHash` 与 `runTraceHash` `100%` 一致，事件计数和 turn 计数完全一致 |
| `perfSmoke` | `3` 个固定场景，各 `30` 次采样 | `4.4.2` 全部预算满足，单场景无 `> 10%` 的异常尖峰 |
| `soakRun` | `8h` 总预算、`4` 职业 × `2` 种族 × `4` seed 矩阵轮转 | `0` 崩溃、`0` OOM、`postWarmupHeapDriftMb < 50`、`maxGcPauseMs <= 50`、`p1Fps >= 30` |
| `localizationQa` | `zh-CN` / `en-US` + 示例 content pack | unresolved key / placeholder mismatch / truncation 全部为 `0` |
| `accessibilityQa` | release 主流程 + HUD/日志/背包/结算 | 对比度 `>= 4.5:1`，纯键盘流程通过，颜色非唯一信息载体 |
| `balanceLab` | `8` 个代表性 build × `10` seed | canonical build 通关率在 `20% ~ 70%`，成功 run `medianTurns` 离群度 `<= ±25%` |
| `packageRelease` | release 配置 + 当前支持平台矩阵 | 安装包可生成、可启动、可完成最小开局流程，发布资料齐全 |
| `contentPackHarness` | base game + 示例 pack | 加载后 tactical/perception/replay/death/perf 全链路仍可通过 |

### 6.3 自动化命令

```bash
./gradlew test
./gradlew :core:test
./gradlew tacticalAiHarness
./gradlew replayHarness
./gradlew perfSmoke
./gradlew soakRun
./gradlew localizationQa
./gradlew accessibilityQa
./gradlew balanceLab
./gradlew contentPackHarness
./gradlew packageRelease
```

### 6.4 Golden 与 batch 更新策略

1. `P5-W1` 合入后，所有 `AIDecisionTrace` golden 必须带 `phase: P5` 与 `aiMode: tactical` 或等价标记。
2. `P5-W2` 合入后，潜行/仇恨相关 fixed-seed 场景必须全量重录；旧 `Phase 3` stealth trace 不再与 `Phase 5` 混用。
3. `P5-W4` 开始建立 replay golden，独立于 `CombatTrace` golden 存档，不混进 `Phase 3/4` 的 trace 目录。
4. `BalanceLab`、`soakRun`、`contentPackHarness` 都必须保留 batch 摘要，不得只保一条“通过/失败”结论。

### 6.5 白盒验证

完整白盒步骤以 [2026-03-13-phase5-regression-checklist.md](./2026-03-13-phase5-regression-checklist.md) 为权威；本节只保留最低验收动作：

1. 固定 Boss 对局，观察战术 AI 的走位、留技能、换目标与 telegraph 响应是否可感知提升。
2. 做一次潜行接敌与脱战，确认 AI 基于 `lastKnownPosition` 行动，而不是全图透视。
3. 读取一局失败 run 的 death analysis，确认死因、关键状态和最近 `5` 回合轨迹都可解释。
4. 切换语言与可读性选项，确认双语言和 Accessibility 设置真实生效。
5. 从安装包启动，完成一次从开局到结束的基本流程。

## 7. 出口门禁

1. `tacticalAiHarness`、`replayHarness`、`perfSmoke`、`soakRun`、`balanceLab`、`packageRelease` 全绿。
2. `DeathAnalysis`、`ReplayFrame`、`RunHistoryEntry`、`SoakReport` 都有稳定的 schema 与 batch 证据。
3. Localization QA、Accessibility QA 与 content pack compatibility 全绿。
4. 安装包、已知问题、操作说明、验证说明和 release notes 齐全。

## 8. 风险与止损

1. 如果 tactical AI 破坏可解释性，优先退回脚本化规则并保留评分层最小集。
2. 如果 replay 成本过高，优先保语义重放，不引入渲染帧录制。
3. 如果 soak 或 perf 长期不稳定，停止补内容，优先做资源装载、句柄与内存收口。
4. 如果 Localization QA、Accessibility QA 或 content pack compatibility 仍有大面积问题，不允许封版。

## 9. 当前状态

1. 本文已把 `Phase 5` 的 contract、工作包、量化门槛和既有系统接线边界具体化。
2. 当前尚未开始代码实现，`tacticalAiHarness / replayHarness / perfSmoke / soakRun / balanceLab / packageRelease` 仍需后续建设。
