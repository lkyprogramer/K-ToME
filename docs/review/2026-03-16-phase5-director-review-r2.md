# Phase 5 Director Review — Round 2

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md`（741 行）
2. `docs/phase5/2026-03-13-phase5-regression-checklist.md`（162 行）
3. `docs/phase5/roadmap.md`（73 行）
4. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（§9.4）

**审阅焦点**：R1 修复确认 + YAML ↔ Kotlin 结构对齐 + 感知状态机数学一致性。

---

## 0. R1 修复确认

### P0 修复

| R1 编号 | 问题 | 状态 | 说明 |
| --- | --- | --- | --- |
| P0-R1-1 | 全文没有 Kotlin 骨架 | ✅ 已修复 | 新增 28 个 Kotlin 类型/接口/枚举：`TacticalScoringLayer`、`ActionCandidate`、`TacticalScoringResult`、`Consideration`、`UtilityEvaluator`、`AIContext`（14 字段）、`TacticalSelectionReason`、`ScoredCandidate`、`TacticalAIDecisionTrace`、`PerceptionState`、`HateSource`、`HateFocus`、`PerceptionStimulus`、`PerceptionEntry`、`PerceptionConfig`、`PerceptionSystem`、`TurnSummary`、`DeathSuggestionKey`、`DeathSuggestion`、`DeathAnalysis`（11 字段含 `turn`）、`ReplayHeader`、`ReplayEventType`、`ReplayEvent`、`ReplayFrame`、`RunResult`、`RunHistoryEntry`（13 字段）、`SoakConfig`、`SoakReport`（14 字段） |
| P0-R1-2 | 缺少权威链声明 | ✅ 已修复 | 文件头 3 条权威链声明完整：§9.4 只保留锚点 → 本文为执行权威 → checklist 为量化权威 |
| P0-R1-3 | 工作包无依赖 | ✅ 已修复 | §5.1 有完整依赖表 + 3 条执行原则 |
| P0-R1-4 | Roadmap 空壳 | ✅ 已修复 | 73 行，含检查点表、并行 lane、工作包依赖、执行原则、进出条件 |

### P1 修复

| R1 编号 | 问题 | 状态 | 说明 |
| --- | --- | --- | --- |
| P1-R1-1 | 感知状态机无转换条件 | ✅ 已修复 | §4.2.2 有完整 4 状态转换表，含进入条件、行为约束和退出条件 |
| P1-R1-2 | `HateFocus` 无类型 | ✅ 已修复 | 5 字段全类型化：`EntityId`、`Point?`、`Float`、`HateSource`、`Int` |
| P1-R1-3 | `DeathAnalysis` 与 core 不一致 | ✅ 已修复 | `turn: Int` 已补入；`suggestions` 改为 `List<DeathSuggestion>`（typed key + args）；`combatTrace: CombatTrace`、`last5Turns: List<TurnSummary>` 已显式标注 |
| P1-R1-4 | `AIDecisionTrace` 需 Utility 扩展 | ✅ 已修复 | `TacticalAIDecisionTrace` 含 `candidates: List<ScoredCandidate>`、`finalScore: Float`、`selectionReason: TacticalSelectionReason` |
| P1-R1-5 | 没有 YAML/JSON 示例 | ✅ 已修复 | 4 组示例：§4.1.3 tactical profile、§4.2.4 perception config、§4.3.3 replay JSON、§4.4.4 soak config |
| P1-R1-6 | `tacticalAiHarness` 未定义 | ✅ 已修复 | §6.2 定义 12 固定场景、85% 匹配率、0 非法目标、100% aiTraceHash 一致 |
| P1-R1-7 | Checklist 无量化门槛 | ✅ 已修复 | 所有 9 个 harness 均有量化通过标准 |
| P1-R1-8 | Replay schema 未定义 | ✅ 已修复 | `ReplayHeader`、`ReplayEvent`、`ReplayFrame` + §4.3.2 存储/哈希合同 |
| P1-R1-9 | 无 Phase 3/4 接线说明 | ✅ 已修复 | §4.6 有 8 个接线点的完整表格，含既有系统、接法和禁止事项 |
| P1-R1-10 | `BalanceLab` 未定义 | ✅ 已修复 | §4.5.2 有输入（8 build × 10 seed）、5 项输出和 3 条门槛 |
| P1-R1-11 | 未引用 Phase 3 STEALTH/TAUNT | ✅ 已修复 | §4.2.3 有 5 条显式接线规则 |
| P1-R1-12 | 性能基线缺测量方法 | ✅ 已修复 | §4.4.2 三列表含测量方式（GLProfiler、Benchmark、JVM 监控） |
| P1-R1-13 | Soak harness 未定义 | ✅ 已修复 | §4.4.3 含 `SoakConfig` + warmup/drift/handle delta 门槛 |
| P1-R1-14 | 缺少检查点 | ✅ 已修复 | §1.1 P5-A/B/C 检查点表含进入/退出条件 |

### P2 修复

| R1 编号 | 问题 | 状态 | 说明 |
| --- | --- | --- | --- |
| P2-R1-1 | 无并行开发线 | ✅ 已修复 | §5.2 + roadmap §3 均有 3 条 lane |
| P2-R1-2 | `TurnSummary` 未定义 | ✅ 已修复 | 8 字段数据类 |
| P2-R1-3 | DSL/Utility 优先关系 | ✅ 已修复 | §4.1.2 固定 5 步执行管线 |
| P2-R1-4 | `AIContext` 缺 Phase 4 信息 | ✅ 已修复 | 新增 `terrainTagsAtSelf`、`nearbyTerrainInteractionIds`、`activeMutationIds`、`bossPhaseId` |
| P2-R1-5 | Run history 模型未定义 | ✅ 已修复 | `RunHistoryEntry`（13 字段）+ §4.3.2 存储策略 |
| P2-R1-6 | Golden seed 策略缺失 | ✅ 已修复 | §6.4 有 4 条 golden 更新规则 |
| P2-R1-7 | Content pack 兼容性测试 | ✅ 已修复 | §6.2 `contentPackHarness` + §4.6 接线表 |
| P2-R1-8 | Accessibility 标准笼统 | ✅ 已修复 | §4.5.1 有对比度 ≥ 4.5:1、字号 ≥ 14px、纯键盘、颜色非唯一 |
| P2-R1-9 | Localization QA 不含 Phase 4 | ✅ 已修复 | §4.5 冻结口径 1 显式包含 mutation/hidden content/content pack |

**R1 总计 27 项（4 P0 + 14 P1 + 9 P2）全部已确认修复。**

---

## 1. R2 新发现

### 新发现统计

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 2 |
| P2 | 9 |

---

### P1-R2-1：§4.1.3 Tactical AI YAML ↔ Kotlin 结构不匹配

**位置**：§4.1.1 Kotlin 合同 + §4.1.3 YAML 示例

**问题**：YAML 示例描述的数据结构与 Kotlin 骨架存在四处结构性不对齐：

**① YAML 根元素没有对应 Kotlin 类型**

```yaml
# YAML 根结构
id: boss.molten_giant.phase_2
baseDslProfile: boss.molten_giant.phase_2_scripted
fallbackActionId: wait
candidateActions: [...]
selection:
  dslVetoFirst: true
  traceMode: tactical
```

`id`、`baseDslProfile`、`fallbackActionId`、`candidateActions`、`selection` 这五个字段在 Kotlin 中没有任何类型对应。开发者不知道这个 YAML 应该反序列化为什么。

**② `considerations` 嵌套位置不一致**

YAML 中 `considerations` 嵌套在每个 `candidateAction` 内部：

```yaml
candidateActions:
  - actionId: melee_slam
    considerations:
      - evaluatorId: target_cluster
        weight: 1.25
```

但 Kotlin 的 `ActionCandidate` 没有 `considerations` 字段：

```kotlin
data class ActionCandidate(
    val actionId: String,
    val sourceRuleIds: List<String>,
    val hardConstraints: List<String>,
    // ← 缺少 considerations
)
```

而 `Consideration` 通过 `actionId` 反向引用动作：

```kotlin
data class Consideration(
    val actionId: String,      // ← 反向引用
    val evaluator: UtilityEvaluator,
    val weight: Float = 1.0f,
)
```

嵌套 vs 反向引用是两种不同的数据建模。

**③ `evaluator` 类型不匹配**

Kotlin `Consideration.evaluator` 是 `UtilityEvaluator`（fun interface 引用），YAML 中是 `evaluatorId: String`。YAML → 运行时需要一个 evaluator 注册表/工厂进行查找，但该机制未被文档化。

**④ `selection` 块没有 Kotlin 类型**

YAML 的 `selection: { dslVetoFirst: true, traceMode: tactical }` 没有对应 Kotlin 数据类。

**建议**：区分 **定义层类型**（YAML 反序列化目标）和 **运行时类型**（接口调用参数），新增：

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
```

运行时类型（`ActionCandidate`、`Consideration`、`TacticalScoringLayer`）保持不变，由 loader 从 def 转换为 runtime 实例。

---

### P1-R2-2：§4.2.4 Perception YAML ↔ Kotlin 不匹配

**位置**：§4.2.1 Kotlin 合同 + §4.2.4 YAML 示例

**问题**：YAML 示例与 Kotlin 骨架存在两处结构不对齐：

**① `useLastKnownPosition` 不在 `PerceptionConfig` 中**

```yaml
perception:
  suspiciousThreshold: 0.25
  # ... 5 个 PerceptionConfig 字段 ...
  useLastKnownPosition: true    # ← 不在 PerceptionConfig 中
```

`useLastKnownPosition` 是 Phase 3 `AIProfile` 的字段，不属于 `PerceptionConfig`。YAML 把两个不同类型的字段混在了同一个 `perception` 块中。

**② `stimuli` 是定义层概念，但 Kotlin 只有运行时类型**

```yaml
stimuli:
  - type: SOUND
    intensity: 0.30
    fromAction: door_break    # ← PerceptionStimulus 没有 fromAction
```

YAML 的 `stimuli` 是 **刺激规则定义**（"当 `door_break` 动作发生时，产生 `SOUND` 类型、强度 `0.30` 的刺激"）。但 Kotlin 的 `PerceptionStimulus` 是 **运行时事件**（`source: HateSource, emitterId: EntityId?, origin: Point, intensity: Float`），两者是不同概念：

| | YAML stimuli（规则定义） | Kotlin PerceptionStimulus（运行时事件） |
| --- | --- | --- |
| 用途 | 告诉系统何时产生刺激 | 表示已产生的具体刺激 |
| `type/source` | ✅ 都有 | ✅ |
| `intensity` | ✅ | ✅ |
| `fromAction` | ✅ 触发动作 ID | ❌ 没有 |
| `emitterId` | ❌ | ✅ 运行时才知道 |
| `origin` | ❌ | ✅ 运行时才知道 |

**建议**：与 P1-R2-1 同模式，新增定义层类型：

```kotlin
data class StimulusRuleDef(
    val type: HateSource,
    val intensity: Float,
    val fromAction: String,
)

data class PerceptionProfileDef(
    val config: PerceptionConfig,
    val useLastKnownPosition: Boolean,
    val stimulusRules: List<StimulusRuleDef>,
)
```

---

### P2-R2-1：`PerceptionStimulus.intensity` → `HateFocus.confidence` 的积累规则未定义

**位置**：§4.2.1 + §4.2.2

**问题**：§4.2.2 状态转换依赖 `confidence` 阈值（`>= 0.25` → SUSPICIOUS，`>= 0.50` → ALERT），但没有说明 `PerceptionStimulus.intensity` 如何转化为 `HateFocus.confidence`：

- 直接赋值？（`confidence = intensity`）
- 累加？（`confidence += intensity`，上限 1.0）
- 取最大值？（`confidence = max(confidence, intensity)`）
- 加权累加？（考虑距离衰减？）

当精英怪同时收到多个刺激（例如听到破门声 + 看到同伴受伤）时，这个规则决定 AI 是否直接跳到 ALERT。

**建议**：补充一句积累规则，例如：

> `applyStimulus` 时，`confidence = min(1.0, max(confidence, stimulus.intensity))`。同一回合多个刺激取运算后最大值。

---

### P2-R2-2：SEARCHING 退出的衰减数学与阈值不一致

**位置**：§4.2.2 + §4.2.1 `PerceptionConfig`

**问题**：SEARCHING 退出条件为"搜索 `3` 回合后仍无结果则衰减到 `SUSPICIOUS` 或 `UNAWARE`"。但默认参数下存在数学矛盾：

```
进入 SEARCHING 时 confidence = 1.0（刚被 TAUNT 或满 confidence ALERT 后目标消失）
searchDecayPerTurn = 0.15
searchDurationTurns = 3

3 回合后: confidence = 1.0 - 3 × 0.15 = 0.55
alertThreshold = 0.50

0.55 > 0.50 → 仍处于 ALERT 级别 confidence，无法按描述衰减到 SUSPICIOUS/UNAWARE
```

3 回合的固定搜索时间 + 0.15/回合的衰减只能降低 0.45，不保证 confidence 低于 `suspiciousThreshold`（0.25）。

**建议**：补充一条兜底规则：

> 若 `searchDurationTurns` 结束时 confidence 仍 `>= alertThreshold`，强制将 confidence 设为 `alertThreshold - 0.01`（确保退出 SEARCHING 后至多回到 SUSPICIOUS）。

---

### P2-R2-3：TAUNT 结束后 `HateFocus.confidence` 处理未定义

**位置**：§4.2.3 第 4~5 条

**问题**：§4.2.3 第 4 条说"TAUNT 强制把 confidence 设为 1.0"，第 5 条说"TAUNT 结束后不得保留隐式强制目标，AI 回到普通 PerceptionState 与 HateFocus 选择逻辑"。但没有说明 TAUNT 结束时 confidence 值如何处理：

- 保持 1.0？→ AI 立即以最高 confidence ALERT 锁定前嘲讽源，实际上"隐式保留"了目标。
- 恢复到 TAUNT 前的值？→ 需要额外保存"pre-TAUNT confidence"。
- 设为 0？→ AI 立即变为 UNAWARE，不合理（刚打完架就失忆）。
- 按一般衰减规则处理？→ 从 1.0 开始自然衰减，和上面的"保持 1.0"一样，至少在当回合 AI 仍处于 ALERT。

**建议**：在第 5 条补充一句：

> TAUNT 结束时，将 confidence 恢复到 `alertThreshold`（0.50），保证 AI 处于 ALERT 但不锁定嘲讽源；后续按正常衰减规则处理。

---

### P2-R2-4：`semanticHash` / `traceHash` 的计算时机与存储未明确

**位置**：§4.3.2

**问题**：§4.3.2 定义了两种 hash：

- `semanticHash`：归一化 `ReplayFrame + CombatTrace + TacticalAIDecisionTrace` 序列的 SHA-256
- `traceHash`：归一化 `CombatTrace + TacticalAIDecisionTrace` 序列的 SHA-256

但没有说明：

1. **计算时机**——是 run 结束时一次性计算，还是每 frame 增量计算？
2. **存储位置**——hash 存在哪里？`ReplayFrame` 有 `combatTraceHash` 和 `aiTraceHash`（per-frame hash），但 `semanticHash` 和 `traceHash` 是全 run 级别的 hash，没有对应字段。
3. **per-frame hash vs per-run hash 关系**——`ReplayFrame.combatTraceHash` 是单帧的 combat trace hash，而 §4.3.2 的 `traceHash` 是全 run 级别的。两者名称相似但语义不同，容易混淆。

**建议**：区分命名并补充存储位置：

1. per-frame hash 保留现有命名（`combatTraceHash` / `aiTraceHash`）。
2. per-run hash 改为 `runSemanticHash` / `runTraceHash`，存储在 replay 文件尾部或 `RunHistoryEntry` 中。
3. 明确计算时机为 run 结束时一次性计算。

---

### P2-R2-5：`DeathAnalysis` 存储位置未定义

**位置**：§4.3.2

**问题**：§4.3.2 定义了 replay 存储路径（`.replays/<runId>.replay.json.gz`）和 run history 路径（`.history/run-history.json`）。但 `DeathAnalysis` 存储在哪里？

- 嵌入 replay 文件？（replay 定位为"输入 + 语义事件"，塞入分析结论不合适）
- 嵌入 run history？（`RunHistoryEntry.deathAnalysisId: String?` 说明它有独立 ID，暗示独立存储）
- 独立文件？（如 `.history/<runId>.death.json`？）

**建议**：补充一句存储规则，例如：

> `DeathAnalysis` 存储在 `.history/<runId>.death.json`；`RunHistoryEntry.deathAnalysisId` 为 `runId`（当 `result == DEATH` 时存在）。

---

### P2-R2-6：`P5-W4` 对 `P5-W3` 的依赖缺少理由

**位置**：§5.1 工作包与依赖

**问题**：工作包依赖表中 `P5-W4`（replay / run history / death analysis / Localization-Accessibility QA）依赖 `P5-W1, P5-W2, P5-W3`。执行原则 2 只解释了为什么需要 W1 和 W2（"不允许在没有 TacticalAIDecisionTrace 与 PerceptionState 正式合同的情况下先写 replay/death 临时字段"），但没有解释为什么需要 W3（perf smoke / soak）。

replay schema 和 death analysis 的设计不依赖 perf baseline。Localization / Accessibility QA 也不依赖 soak 结果。将 W4 阻塞在 W3 之后会推迟 replay 与死因分析的开发，而这两者与 perf 没有技术依赖。

**建议**：

方案 A：从 W4 的依赖中移除 W3，改为 `P5-W1, P5-W2`。
方案 B：如果确有理由（如 Localization QA 需要在 perf 稳定后才能测全流程），在执行原则中补充说明。

---

### P2-R2-7：`SoakReport` 只有 `avgFps`，缺少帧率稳定性指标

**位置**：§4.4.1 + §4.4.2

**问题**：性能基线表要求"桌面端稳定 `60 FPS`"，但 `SoakReport` 只包含 `avgFps: Float`。一个平均 60 FPS 但偶尔掉到 15 FPS 的 run 满足 `avgFps` 但不满足"稳定"。

`SoakReport` 有 `p95DrawCalls`、`p95TextureBindings`、`p95FovMs`、`p95PathfindingMs` 等 P95 指标，但帧率本身缺少分位数指标。

**建议**：新增 `p1Fps: Float`（最差 1% 帧率）或 `minFps: Float`，并在 soak 通过标准中补充"p1Fps >= 30"之类的下限。

---

### P2-R2-8：`AI_SHIFT` mutation 修改候选权重的具体机制未定义

**位置**：§4.6 接线边界表

**问题**：§4.6 说 `AI_SHIFT` mutation "只能覆盖 candidate weight、allow-list 或 fallback，不得绕过 DSL veto"。但接口层面上看：

- `TacticalScoringLayer.selectAction(context, candidates)` 的 `candidates` 中每个 `ActionCandidate` 只有 `actionId / sourceRuleIds / hardConstraints`——没有 weight。
- `AIContext.activeMutationIds: List<String>` 让评分层知道当前有哪些 mutation。但评分层如何根据 mutation 修改候选权重？是通过 `UtilityEvaluator` 内部逻辑读取 `activeMutationIds` 并调整分值？还是在 `selectAction` 调用前修改 `Consideration.weight`？

"覆盖 candidate weight"和"覆盖 allow-list"分别在哪一层发生、通过什么接口完成，没有被文档化。

**建议**：补充一条说明：

> `AI_SHIFT` mutation 通过 `AIContext.activeMutationIds` 传递给 `UtilityEvaluator`；evaluator 根据 mutation 类型调整自身返回的分值。allow-list 和 fallback 的覆盖发生在 DSL 层（由 mutation overlay AIProfile），不在评分层。

---

### P2-R2-9：`SoakConfig` 的组合迭代策略未说明

**位置**：§4.4.1 + §4.4.3

**问题**：`SoakConfig` 定义了 `professionIds`（4 个）、`raceIds`（2 个）、`seeds`（4 个），Checklist §2.3 要求 `4 职业 × 2 种族 × 4 seed`。全组合为 32 种配置，但没有说明：

- 是 32 个独立的 8 小时 soak（总计 256 小时）？
- 还是一个 8 小时 run 循环执行 32 种组合？
- 还是 32 个并行 soak？

如果是 32 × 8h = 256h，这在 CI 中不现实。如果是一个 8h run 轮转执行，那 `durationHours: 8` 的含义需要重新理解。

**建议**：补充一句迭代规则，例如：

> `soakRun` 按 `profession × race × seed` 全组合依次执行；每组合运行一个完整 run（从开局到通关或死亡）；`durationHours` 是总时限，不是每组合时限。超时的组合标记为 `TIMEOUT` 但不判定失败。

---

## 2. 跨文档一致性检查

### 2.1 核心文档 §9.4

| 检查点 | 状态 | 说明 |
| --- | --- | --- |
| 权威链声明 | ✅ | §9.4 开头明确"以 phase5 执行文档为权威" |
| 类型名一致 | ✅ | 所有共有类型名完全对齐 |
| 枚举值一致（`TacticalSelectionReason` / `PerceptionState` / `HateSource` / `DeathSuggestionKey`） | ✅ | 值与顺序完全一致 |
| `AIContext` 字段一致 | ✅ | 14 字段完全一致 |
| `TacticalAIDecisionTrace` 字段一致 | ✅ | 9 字段完全一致 |
| `HateFocus` 字段一致 | ✅ | 5 字段完全一致 |
| `DeathAnalysis` 字段一致 | ✅ | Phase 5 为权威，§9.4 同步已更新 |
| `SoakReport` 字段差异 | ✅ 权威链覆盖 | §9.4 保留 7 字段锚点，Phase 5 扩展为 14 字段；权威链声明"以本文为权威" |
| `RunHistoryEntry` 字段差异 | ✅ 权威链覆盖 | §9.4 的 `result: String` + 10 字段 → Phase 5 的 `result: RunResult` + 13 字段 |
| 性能基线数值一致 | ✅ | 目标值与测量方式完全一致 |

### 2.2 Phase 5 内部 YAML/JSON ↔ Kotlin 一致性

| 示例 | 状态 | 备注 |
| --- | --- | --- |
| §4.1.3 tactical profile YAML | ❌ | P1-R2-1：无 wrapper 类型、considerations 嵌套不匹配、evaluatorId vs evaluator |
| §4.2.4 perception YAML | ❌ | P1-R2-2：useLastKnownPosition 溢出、stimuli 是 def 而非 runtime |
| §4.3.3 replay JSON | ✅ | `ReplayHeader` / `ReplayFrame` / `ReplayEvent` 字段完全匹配 |
| §4.4.4 soak config YAML | ✅ | `SoakConfig` 6 字段完全匹配 |

### 2.3 Phase 3 冻结合同衔接

| 检查点 | 状态 |
| --- | --- |
| STEALTH → 目标引用失效 → SEARCHING | ✅ §4.2.3 第 1 条 |
| `useLastKnownPosition` 接入 SEARCHING | ✅ §4.2.3 第 1 条 |
| Boss 遇 STEALTH 不切 phase | ✅ §4.2.3 第 3 条 |
| TAUNT 强制锁定 | ✅ §4.2.3 第 4 条 |
| TAUNT 结束后恢复 | ✅ §4.2.3 第 5 条（但 confidence 处理缺失，见 P2-R2-3） |
| Boss phase graph 仍为权威 | ✅ §4.6 接线表 |
| `CombatTrace` 直接复用 | ✅ §4.6 接线表 |
| `AIDecisionTrace` 保留原始字段 | ✅ §4.1 冻结口径 4 |

### 2.4 Phase 4 冻结合同衔接

| 检查点 | 状态 |
| --- | --- |
| `AI_SHIFT` mutation 接线 | ✅ §4.6（机制待细化，见 P2-R2-8） |
| `PERCEPTION_CHECK` 通过感知状态机 | ✅ §4.6 + §4.2 冻结口径 4 |
| `TerrainTag` 通过 `AIContext` 进入评分层 | ✅ `AIContext.terrainTagsAtSelf` + `nearbyTerrainInteractionIds` |
| `ContentPackManifest` 兼容性 | ✅ §4.6 + §6.2 `contentPackHarness` |
| `LootBudget` 接入死因分析 | ✅ §4.6 接线表 |

### 2.5 主文档 ↔ Roadmap 一致性

| 检查点 | 状态 |
| --- | --- |
| 检查点表（P5-A/B/C）一致 | ✅ |
| 工作包依赖表一致 | ✅ |
| 执行原则措辞一致 | ✅ |
| 并行 lane 一致 | ✅ |
| 进入/退出条件一致 | ✅ |

### 2.6 主文档 ↔ Checklist 一致性

| 检查点 | 状态 |
| --- | --- |
| 9 个 harness 名称一致 | ✅ |
| 量化门槛数值一致 | ✅ |
| golden 更新策略一致 | ✅ |
| 白盒步骤与主文档 §6.5 一致 | ✅ |
| 报告模板覆盖所有 harness | ✅ |

---

## 3. R2 总结

Phase 5 文档体系经过两轮审阅，质量演进如下：

| 轮次 | P0 | P1 | P2 | 总计 | 核心主题 |
| --- | --- | --- | --- | --- | --- |
| R1 | 4 | 14 | 9 | 27 | 骨架缺失、权威链缺失、依赖缺失、roadmap 空壳 |
| R2 | 0 | 2 | 9 | 11 | YAML ↔ Kotlin 结构对齐、感知数学一致性、存储/哈希细节 |
| **累计** | **4** | **16** | **18** | **38** | |

**R2 结论**：

1. **R1 的 27 项全部修复。** Phase 5 从"设计意向"跨越到了"可执行规格"，文档体量从 ~380 行增长到 ~980 行，增量结构与 Phase 4 R1→R2 演进完全同构。
2. **零 P0**。核心合同（Kotlin 骨架、权威链、工作包依赖、检查点）已完全稳定。
3. **2 个 P1 集中在 YAML ↔ Kotlin 结构对齐**，本质是定义层类型（YAML 反序列化目标）与运行时类型（接口调用参数）没有区分。这与 Phase 4 R3 的 17 个发现（"YAML/Kotlin 结构不匹配"）高度同构。修复模式也相同：为 YAML 根结构和嵌套结构新增 `*Def` 类型。
4. **9 个 P2 分布在三个主题**：
   - 感知数学（P2-R2-1 ~ P2-R2-3）：confidence 积累、衰减数学、TAUNT 后处理——需要一两句规则收口。
   - 存储/哈希（P2-R2-4 ~ P2-R2-5）：hash 命名与 DeathAnalysis 存储位置——需要明确存储合同。
   - 工作包/指标（P2-R2-6 ~ P2-R2-9）：W4 依赖、FPS 分位数、mutation 机制、soak 迭代——各一句话即可收口。

**放行建议**：Phase 5 核心合同已就绪。R2 的 2 个 P1 需要在 P5-W1 实现前修复（YAML ↔ Kotlin 对齐直接影响 loader 设计）。9 个 P2 可在 R3 中与 P1 修复一并收口。建议下一轮（R3）聚焦 YAML ↔ Kotlin 全量对齐确认和感知数学闭合验证。

---

*审阅人：资深游戏设计与开发总监视角*
*审阅版本：Round 2*
*审阅状态：P1 修复后可进入 P5-W1 起步*
