# Phase 5 Director Review — Round 1

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md`（281 行）
2. `docs/phase5/2026-03-13-phase5-regression-checklist.md`（81 行）
3. `docs/phase5/roadmap.md`（17 行）

**权威参考文档**：

1. `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`（§13 怪物与 AI、§17.4 Phase 5 细化）
2. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（§9.4 Phase 5 细节补充）

**审阅焦点**：Phase 5 是 K-ToME 封版发布阶段。本轮审阅重点是评估文档从"设计意向"到"可驱动实现的执行文档"之间的距离，对标 Phase 3/4 执行文档的成熟度标准。

---

## 0. 整体评估

Phase 5 执行文档当前处于 **Phase 4 R1 之前的水平**——系统目标清晰、工作包拆分合理、风险项有退路，但还停留在"设计意向"层面，尚未达到可直接驱动代码实现的执行文档标准。

核心差距与 Phase 4 R1 完全同构：

| 维度 | Phase 4（R1 时） | Phase 5（当前） |
| --- | --- | --- |
| Kotlin 骨架 | 有但不完整 | **完全没有** |
| YAML 示例 | 有但与骨架不一致 | **完全没有** |
| 公式/算法 | 有但变量来源不全 | 字段列表，无类型 |
| 权威链声明 | 缺失 | **缺失** |
| 工作包依赖 | 有但不完整 | **完全没有** |
| 验证门槛 | 定性描述 | **定性描述** |
| 与上游系统接线 | 部分 | **完全缺失** |

Phase 4 从 274 行（R1 前）经过 4 轮审查增长到 1085 行，核心增量全部来自：Kotlin 骨架、YAML 示例、合并规则、冻结口径和工作包依赖。Phase 5 需要走同样的路径。

---

## 1. R1 发现

### 发现统计

| 等级 | 数量 |
| --- | --- |
| P0 | 4 |
| P1 | 14 |
| P2 | 9 |
| **总计** | **27** |

---

### P0-R1-1：全文没有 Kotlin 骨架

**位置**：全文

**问题**：Phase 5 执行文档没有任何 Kotlin 数据类、接口或枚举定义。而两份权威参考文档已经提供了完整的起始骨架：

| 类型 | 来源 | Phase 5 文档引用情况 |
| --- | --- | --- |
| `Consideration(actionId, evaluator, weight)` | core-supplements §9.4.1 | 未引用 |
| `UtilityEvaluator` (fun interface) | core-supplements §9.4.1 | 未引用 |
| `AIContext`（11 字段） | core-supplements §9.4.1 | 未引用 |
| `DeathAnalysis`（11 字段含 `turn`） | core-supplements §9.4.3 | 有字段列表但缺 `turn`，无类型 |
| `AIDecisionTrace`（6 字段） | detailed-design §13.7 | 提及名称但无结构 |

**影响**：开发者在实现 W1/W2/W4 时没有可编译的 contract 锚点。Phase 4 的经验表明，没有 Kotlin 骨架的文档会导致实现者各自造型，W1→W2 接口返工概率极高。

**建议**：

1. 将 core-supplements §9.4.1/9.4.3 和 detailed-design §13.7 的骨架引入 Phase 5 执行文档，作为执行权威版本。
2. 新增以下缺失骨架：
   - `PerceptionState` 枚举（4 状态 + 转换条件）
   - `HateFocus` 数据类（含类型标注）
   - `PerceptionEntry` / `PerceptionSystem` 接口
   - `TacticalScoringLayer` 接口
   - `RunHistoryEntry` / `ReplayEvent` 数据类
   - `SoakReport` 数据类
3. 建立权威链声明（见 P0-R1-2）。

---

### P0-R1-2：缺少权威链声明

**位置**：文件头部

**问题**：Phase 4 执行文档在核心文档 §9.3 中有明确权威链："本节的类型名与 phase4 执行文档保持一致；若字段细节不同，以 phase4 执行文档为权威。" Phase 5 没有类似声明。

当前 core-supplements §9.4 提供了 `AIContext`、`Consideration`、`DeathAnalysis` 的 Kotlin 骨架。Phase 5 执行文档列出的 `DeathAnalysis` 字段少了 `turn`，且 `suggestions` 类型不同。开发者无法判断以哪份为准。

**建议**：在 core-supplements §9.4 补充一句权威链声明（与 §9.3 同模式），并在 Phase 5 执行文档中明确引用。

---

### P0-R1-3：工作包之间没有依赖关系

**位置**：§5

**问题**：Phase 5 定义了 5 个工作包（W1~W5），但没有依赖表。对比 Phase 4：

```
Phase 4: W1||W3, W2→W1, W4→W2+W3, W5→W3+W4
Phase 5: W1? W2? W3? W4? W5? ← 全部缺失
```

实际上存在明显的隐式依赖：

| 工作包 | 隐式依赖 | 原因 |
| --- | --- | --- |
| P5-W2 | P5-W1 | 感知/仇恨是 tactical scoring 的输入源 |
| P5-W3 (soak) | P5-W1 + P5-W2 | soak 需要完整 AI 才能测到真实负载 |
| P5-W4 (replay) | P5-W1 + P5-W2 | replay 需要记录完整 AI trace |
| P5-W5 (release) | P5-W1 ~ W4 | 封版依赖所有前置工作包 |

不明确依赖的后果：W3 在 W1 之前起步会测到旧 AI 的性能，W5 在 W4 之前起步无法出安装验证。

**建议**：补充工作包依赖表（与 Phase 4 §5.2 同格式），并说明哪些可以并行。初步建议：

```
W1 → Phase 4
W2 → W1
W3 可在 W1 后起步（基线 soak），W2 完成后重测（完整 soak）
W4 → W1 + W2
W5 → W1 ~ W4
```

---

### P0-R1-4：Roadmap 是空壳

**位置**：`docs/phase5/roadmap.md`

**问题**：Phase 5 roadmap 只有 17 行，仅包含主题说明和文档索引。对比 Phase 4 roadmap（79 行）：

| 内容 | Phase 4 roadmap | Phase 5 roadmap |
| --- | --- | --- |
| 检查点表（进入/退出条件） | ✅ | ❌ |
| 并行开发线 | ✅ | ❌ |
| 工作包依赖表 | ✅ | ❌ |
| 执行原则 | ✅ | ❌ |
| 进入/退出摘要 | ✅ | ❌ |

**建议**：将 roadmap 补充到与 Phase 4 同等结构，至少包含：检查点表（P5-A/B/C）、工作包依赖表、并行开发线和进入/退出条件。

---

### P1-R1-1：感知状态机只有状态名，没有转换条件

**位置**：§4.2

**问题**：4 个感知状态（UNAWARE / SUSPICIOUS / ALERT / SEARCHING）没有定义：

- 什么条件触发 UNAWARE → SUSPICIOUS？
- SUSPICIOUS → ALERT 需要多少回合或多大刺激？
- ALERT → SEARCHING 在什么条件下发生？
- 各状态下 AI 行为约束是什么？

Phase 3 已经冻结了 `perceptionRange`、`useLastKnownPosition` 和 STEALTH/TAUNT 交互合同。Phase 5 的感知状态机必须建立在这些已冻结 contract 之上。

**建议**：

1. 定义 `PerceptionState` 枚举。
2. 定义每个状态的 AI 行为约束和转换条件。
3. 明确与 Phase 3 `perceptionRange` / `useLastKnownPosition` / STEALTH / TAUNT 合同的衔接关系。

---

### P1-R1-2：`HateFocus` 没有类型定义

**位置**：§4.2

**问题**：5 个字段只列出名称，没有类型和语义：

| 字段 | 缺失的定义 |
| --- | --- |
| `targetId` | `Int`? `EntityId`? |
| `lastKnownPosition` | `Point`? 来自 `core.map`? |
| `confidence` | `Float`? 取值范围？衰减函数？ |
| `source` | `String`? 枚举？有哪些合法值（视觉/声音/被攻击/队友通知）？ |
| `updatedTurn` | `Long`? 对应 `turnId`? |

"confidence" 是 HateFocus 的核心机制，但没有衰减规则、初始值、最小阈值。

**建议**：定义 `HateFocus` 数据类，补充 `confidence` 衰减函数和 `source` 枚举（如 `HateSource { VISUAL, SOUND, DAMAGE_RECEIVED, ALLY_REPORT }`）。

---

### P1-R1-3：`DeathAnalysis` 与 core 文档不一致

**位置**：§4.3

**问题**：

1. **缺少 `turn` 字段**：core-supplements §9.4.3 的 `DeathAnalysis` 有 `val turn: Int`，Phase 5 只列了 10 个字段，没有 `turn`。
2. **`suggestions` 类型问题**：core-supplements 定义 `val suggestions: List<String>` 并举例"你的火焰抗性仅有 5%，考虑装备抗火装备"。这违反了 Phase 2 冻结的"core 不拼文案，只输出 token"原则（detailed-design §5.2 冻结规则 1）。
3. **`combatTrace` 和 `last5Turns` 类型未标注**：`combatTrace` 应为 Phase 3 的 `CombatTrace` 类型，`last5Turns` 应为 `List<TurnSummary>`——但 `TurnSummary` 在整个文档体系中未定义。
4. **`activeEffectsAtDeath` 类型未标注**：应为 `List<ActiveEffectV2>`，但 `ActiveEffectV2` 只在 core-supplements 出现一次，没有正式骨架。

**建议**：

1. 补 `turn` 字段。
2. 将 `suggestions` 改为 typed 结构（如 `DeathSuggestion(key: DeathSuggestionKey, args: Map<String, String>)`），由 client 渲染文案。
3. 显式标注 `combatTrace: CombatTrace` 和 `last5Turns: List<TurnSummary>`，并定义 `TurnSummary`。

---

### P1-R1-4：`AIDecisionTrace` 需要为 Utility AI 扩展

**位置**：§4.1

**问题**：detailed-design §13.7 定义的 `AIDecisionTrace` 只有 6 字段，面向 Phase 2~4 的脚本化 DSL。Phase 5 在 DSL 上叠加 Utility 评分层后，trace 需要额外记录：

- 候选动作列表及各自评分
- 最终选择的评分和原因
- Utility 评分与 DSL 规则之间的优先关系

当前 Phase 5 文档提到"AI trace 扩展"但未定义扩展字段。

**建议**：定义 `TacticalAIDecisionTrace`（继承或扩展 `AIDecisionTrace`），至少新增：

```kotlin
data class ScoredCandidate(
    val actionId: String,
    val score: Float,
    val evaluatorId: String,
)

// 扩展 AIDecisionTrace 新增字段
val candidates: List<ScoredCandidate>,
val finalScore: Float,
val selectionReason: String, // "utility_best" / "dsl_override" / "fallback"
```

---

### P1-R1-5：没有 YAML 示例

**位置**：全文

**问题**：Phase 4 有 9 组 YAML 示例（vault / biome / zone profile / hidden entrance / secret zone / hidden event / affix / content pack），Phase 5 没有任何 YAML 示例。以下系统至少需要最小示例：

1. **Tactical AI profile 扩展**：展示 Utility 评分如何叠加在既有 AIProfile DSL 上。
2. **Perception config**：展示感知参数与状态转换阈值。
3. **Soak config**：展示 soak harness 的配置格式。
4. **Replay event**：展示一个 replay 事件的序列化格式。

**建议**：为上述 4 个系统各补充一个最小 YAML/JSON 示例。

---

### P1-R1-6：`tacticalAiHarness` 未定义

**位置**：§6.3 / Checklist §1

**问题**：`./gradlew tacticalAiHarness` 出现在命令列表和 checklist 中，但没有定义：

- 跑哪些场景？（固定 seed Boss 战？精英遭遇？）
- 衡量什么指标？（决策改善率？反应时间？致死率差异？）
- 什么构成 pass/fail？
- 需要多少个固定场景？

Phase 4 的每个 harness 都有明确的量化门槛（如 mapgenSmoke 500 seed 0 崩溃）。

**建议**：定义 tacticalAiHarness 的最小规格，包括：场景列表、对照基线（脚本化 AI vs 战术 AI）、量化指标和通过标准。

---

### P1-R1-7：Regression checklist 缺少量化门槛

**位置**：`2026-03-13-phase5-regression-checklist.md`

**问题**：与 Phase 4 checklist 对比：

| Phase 4 checklist | Phase 5 checklist |
| --- | --- |
| `500` 个 seed，`0` 崩溃 | "tactical AI 固定场景回归稳定" |
| `1000` 个 seed，`100%` 可达率 | "replay 可重放关键 run" |
| `10000` 次 roll，`±5%` 偏差 | "perf smoke 达到预算" |
| `100` 个 seed，`30%` hidden event | "soak 无灾难性崩溃" |

Phase 5 的表述全部是定性的，无法用自动化工具判定 pass/fail。

**建议**：为每个 checklist 条目补充量化门槛，例如：

1. tactical AI：至少 `N` 个固定场景，AI 决策差异率 `< X%`
2. replay：至少 `N` 个 run 可 bit-exact 重放
3. perf smoke：达到 §4.4 性能基线表全部指标
4. soak：`8h` 无 OOM，内存增量 `< 50MB`，无 `> 50ms` GC 停顿
5. localization：`0` unresolved key，`0` 截断文本
6. accessibility：对比度 `>= 4.5:1`，最小字号 `>= 14px`

---

### P1-R1-8：未定义 Replay schema

**位置**：§4.3

**问题**：Replay 系统的设计只有一句话："replay 只记录语义事件与输入，不记录渲染层状态。"

缺少：

1. Replay 事件格式（`ReplayEvent` 数据类）
2. 输入事件格式（`PlayerInput` 数据类）
3. Replay 文件的 schema version 和兼容策略
4. Replay 的存储和压缩方式
5. Replay 验证方式（如何判定 replay 重放结果与原始 run 一致——trace hash 比对？）
6. 与 detailed-design §5.3 定义的 `CombatTrace` / `AIDecisionTrace` 的关系

**建议**：定义 `ReplayFrame` / `ReplayEvent` 数据类和 replay 文件 schema。Checklist §4 已经提到"replay 语义哈希 / trace hash"，但没有定义哈希算法和比对标准。

---

### P1-R1-9：没有与 Phase 3/4 系统的接线说明

**位置**：全文

**问题**：Phase 4 有 §4.6"与既有系统的接线边界"。Phase 5 没有类似章节。以下接线点未被文档化：

| 接线点 | Phase 3/4 系统 | Phase 5 系统 | 缺失内容 |
| --- | --- | --- | --- |
| AI_SHIFT mutation | Phase 4 `MutationKind.AI_SHIFT` | Phase 5 tactical AI | 战术评分层如何处理 AI_SHIFT overlay？ |
| Hidden content discovery | Phase 4 `DiscoveryRuleType.PERCEPTION_CHECK` | Phase 5 感知状态机 | `PERCEPTION_CHECK` 如何映射到感知状态？ |
| Terrain interaction | Phase 4 `TerrainTag` + `CombatPipeline step 9` | Phase 5 tactical AI | AI 是否感知地形交互机会？ |
| Boss phase + telegraph | Phase 3 `BossEncounterDef` / `BossPhaseDef` | Phase 5 tactical Boss AI | Utility AI 如何与 Boss phase graph 协作？ |
| STEALTH / TAUNT | Phase 3 冻结的交互合同 | Phase 5 perception/stealth | 感知状态机如何接入 STEALTH/TAUNT？ |
| CombatTrace | Phase 3 正式公式 | Phase 5 death analysis | `DeathAnalysis.combatTrace` 是否直接复用 `CombatTrace`？ |
| LootBudget | Phase 4 掉落模型 | Phase 5 death analysis | 死因建议是否包含"装备不适配"类分析？ |

**建议**：新增"与既有系统的接线边界"章节，至少覆盖上述 7 个接线点。

---

### P1-R1-10：`BalanceLab` 在 W5 中提及但未定义

**位置**：§5 P5-W5

**问题**：W5 包含"BalanceLab"，但 Phase 5 全文没有定义这是什么。它与 Phase 4 的 `LootBalanceLab` 什么关系？是一个新的 meta-lab？是 Phase 4 lab 的延续？还是一个独立的 Phase 5 平衡验证工具？

**建议**：明确 BalanceLab 的定位：

- 如果是 Phase 4 `LootBalanceLab` 的继承：说明 Phase 5 新增哪些验证维度（如 build diversity 分析）
- 如果是新工具：定义其输入、输出和验证标准

---

### P1-R1-11：感知系统未引用 Phase 3 的 STEALTH / TAUNT 合同

**位置**：§4.2

**问题**：Phase 3 已冻结以下 AI 交互合同：

1. STEALTH 使 AI 当前目标引用失效
2. `useLastKnownPosition = true` 时移动到最后已知位置
3. Boss 遇 STEALTH 不切 phase，执行无目标 fallback
4. TAUNT 强制锁定嘲讽源

Phase 5 的感知/潜行系统必须兼容这些合同，但文档中没有引用它们。

**建议**：在 §4.2 冻结口径中显式引用 Phase 3 的 STEALTH/TAUNT 合同，并说明感知状态机如何与之衔接（如 STEALTH 触发 ALERT → SEARCHING 转换）。

---

### P1-R1-12：性能基线缺少测量方法

**位置**：§4.4

**问题**：Phase 5 的性能基线表只有"指标 | 目标值"两列。core-supplements §9.4.2 有"测量方式"列（GLProfiler、Benchmark 测试、JVM 监控、Soak 测试）。Phase 5 执行文档应直接引用或包含测量方法。

**建议**：将 core-supplements §9.4.2 的测量方式列合并到 Phase 5 性能基线表中。

---

### P1-R1-13：Soak harness 规格未定义

**位置**：§4.4 / §6.3

**问题**：`./gradlew soakRun` 出现在命令列表中，但未定义：

- Soak 运行多少局？连续跑还是定时采样？
- 用什么 seed / 职业 / 路线？
- 监控哪些指标？采样频率？
- 什么条件判定为失败（内存增长阈值？GC 停顿阈值？FPS 下降阈值？）？
- Soak 报告输出什么格式？

**建议**：定义 `SoakConfig` 数据类和 soak harness 的最小规格。

---

### P1-R1-14：缺少检查点拆分

**位置**：§5 / roadmap

**问题**：Phase 4 有 P4-A / P4-B / P4-C 三个检查点，每个有明确的进入和退出条件。Phase 5 的 5 个工作包没有被组织到检查点中。

从工作包内容看，自然拆分为：

- **P5-A**（AI 深化）：W1 + W2
- **P5-B**（稳定性与工具）：W3 + W4
- **P5-C**（发布收口）：W5

但这只是推测，文档中没有明确定义。

**建议**：定义检查点及其进入/退出条件，与 Phase 4 格式对齐。

---

### P2-R1-1：Roadmap 缺少并行开发线

**位置**：`roadmap.md`

**问题**：Phase 4 roadmap 有 Rules / Content / Client / Tools-QA 四条并行 lane。Phase 5 也需要并行推进（AI 开发与 perf/soak 测试可以部分并行），但 roadmap 未定义。

**建议**：补充并行开发线，至少包含 Rules/AI Lane、Tools/QA Lane、Release Lane。

---

### P2-R1-2：未定义 `TurnSummary` 数据类

**位置**：§4.3

**问题**：`DeathAnalysis.last5Turns` 需要 `TurnSummary` 类型，但在整个文档体系中从未定义。`TurnSummary` 至少需要包含：回合号、玩家动作、受到的伤害、施加的状态、位置变化。

**建议**：在 Phase 5 执行文档中定义 `TurnSummary` 数据类。

---

### P2-R1-3：未说明 Tactical AI 与 DSL 的优先关系

**位置**：§4.1

**问题**：detailed-design §13.10 说"先生成动作候选，用评分层选高层意图，仍由同一执行层完成"。但没有说明当 DSL 规则与 Utility 评分冲突时怎么办：

- 如果 DSL 高优先级规则匹配（如 escape_low_hp, priority=100），Utility 评分能否覆盖？
- 是 DSL 规则先过滤、Utility 再排序？还是 Utility 先评分、DSL 作为约束？

**建议**：明确 DSL 与 Utility 的执行管线：建议采用"DSL 规则作为硬约束/veto，Utility 评分在满足约束的候选集中排序"的模式。

---

### P2-R1-4：`AIContext` 缺少 Phase 4 新增信息

**位置**：core-supplements §9.4.1

**问题**：`AIContext` 有 11 字段，但不包含 Phase 4 新增的信息：

- 当前 terrain tags（用于地形交互战术）
- 当前 hidden content 状态
- elite mutation 信息
- Boss phase 当前状态

Phase 5 的战术 AI 如果不感知这些信息，就无法做出利用地形或应对 mutation 的战术决策。

**建议**：在 Phase 5 文档中评估 `AIContext` 是否需要扩展以支持地形感知和 mutation 感知。

---

### P2-R1-5：`Run History` 数据模型未定义

**位置**：§4.3

**问题**：W4 包含"run history"，但没有定义 run history 的数据模型：

- 一次 run 记录什么？（seed / 职业 / build / 存活时间 / 死因 / 到达楼层 / 关键决策点）
- 存储在哪里？（本地文件？SQLite？）
- 如何索引和查询？
- 保留多少历史？

**建议**：定义 `RunHistoryEntry` 数据类和存储策略。

---

### P2-R1-6：Golden seed / batch 策略缺失

**位置**：全文

**问题**：Phase 4 有 §5.3 定义了 golden 更新策略。Phase 5 的 tactical AI 和 replay 都会改变 golden 基线（AI 决策变化 → 战斗结果变化 → trace hash 变化），但没有说明 golden 更新策略。

**建议**：补充 Phase 5 的 golden 更新策略，至少说明：

1. W1/W2 合入后哪些 golden 需要重录
2. tactical AI golden 与 DSL AI golden 是否分离管理
3. replay 的 golden 基线从何时开始

---

### P2-R1-7：Content pack 兼容性测试缺失

**位置**：全文

**问题**：Phase 4 的 content pack 可以添加新怪物、事件和 loot。Phase 5 的 tactical AI、perception、death analysis 需要正确处理 pack 加载的内容。但 Phase 5 没有提到 content pack 兼容性测试。

**建议**：在 §6.2 必测行为中增加一条：content pack 加载后，tactical AI / perception / death analysis / replay 仍正常工作。

---

### P2-R1-8：Accessibility QA 标准过于笼统

**位置**：§4.5

**问题**：Accessibility QA 只说"至少覆盖字体大小、对比度、日志可读性、色彩依赖提示"，没有量化标准。可访问性有明确的行业标准（如 WCAG 2.1 AA）。

**建议**：补充具体标准：

- 对比度：`>= 4.5:1`（WCAG AA 正文标准）
- 最小字号：`>= 14px`（或用户可调）
- 色彩依赖：所有关键信息不仅依赖颜色区分（如元素类型同时用图标/文字标注）
- 键盘导航：所有 UI 可纯键盘操作

---

### P2-R1-9：Localization QA 范围不含 Phase 4 内容

**位置**：§4.5

**问题**：Localization QA 说"覆盖主界面、战斗、背包、长文本、换行和占位符"。但没有提到 Phase 4 新增的内容：

- elite mutation 名称和描述
- hidden event 文案
- secret zone 文案
- content pack 的 i18n 验证
- vault / biome family 的 locale key

**建议**：扩展 Localization QA 范围，显式列出 Phase 4 新增的 locale 域。

---

## 2. 跨文档一致性检查

### 2.1 Phase 5 执行文档 vs core-supplements §9.4

| 检查点 | 状态 | 问题 |
| --- | --- | --- |
| `Consideration` / `UtilityEvaluator` / `AIContext` | ❌ 未引用 | P0-R1-1 |
| `DeathAnalysis` 字段一致性 | ❌ 缺 `turn`，`suggestions` 类型问题 | P1-R1-3 |
| 性能基线表数值一致 | ✅ | 数值相同 |
| 性能基线表测量方式 | ❌ Phase 5 缺少测量方式列 | P1-R1-12 |
| 权威链声明 | ❌ 缺失 | P0-R1-2 |

### 2.2 Phase 5 执行文档 vs detailed-design §13 / §17.4

| 检查点 | 状态 | 问题 |
| --- | --- | --- |
| AI 4 层架构（Perception/Intent/Action/Execution）| ❌ 未引用 | Phase 5 只说"评分层"，没有映射到 4 层架构 |
| `AIDecisionTrace` | ❌ 提及但无结构 | P1-R1-4 |
| DSL 条件词汇（§13.5） | ❌ 未引用 | 战术 AI 是否扩展 DSL 条件词汇？ |
| 动作原语（§13.6） | ❌ 未引用 | 战术 AI 是否新增动作原语？ |
| 工作包编号一致性 | ⚠️ 不同 | detailed-design 用 P5-A1/A2/B1/B2/C1；Phase 5 执行文档用 W1~W5 |

### 2.3 Phase 5 执行文档 vs Phase 3 冻结合同

| 检查点 | 状态 | 问题 |
| --- | --- | --- |
| STEALTH / TAUNT 交互合同 | ❌ 未引用 | P1-R1-11 |
| `perceptionRange` / `useLastKnownPosition` | ❌ 未引用 | P1-R1-1 |
| BossEncounterDef / BossPhaseDef | ❌ 未说明如何与 Utility AI 协作 | P1-R1-9 |
| `AIDecisionTrace` 已冻结字段 | ❌ 未引用 | P1-R1-4 |

### 2.4 Phase 5 执行文档 vs Phase 4 冻结合同

| 检查点 | 状态 | 问题 |
| --- | --- | --- |
| `MutationKind.AI_SHIFT` | ❌ 未引用 | P1-R1-9 |
| `DiscoveryRuleType.PERCEPTION_CHECK` | ❌ 未引用 | P1-R1-9 |
| `TerrainTag` + 战斗回调 | ❌ 未引用 | P1-R1-9 |
| Content pack 兼容性 | ❌ 未引用 | P2-R1-7 |

---

## 3. R1 总结

| 等级 | 数量 | 核心主题 |
| --- | --- | --- |
| P0 | 4 | Kotlin 骨架完全缺失、权威链未建、工作包无依赖、roadmap 空壳 |
| P1 | 14 | 感知/仇恨无类型定义、DeathAnalysis 不一致、无 YAML 示例、harness 未定义、无 Phase 3/4 接线、checklist 无量化 |
| P2 | 9 | DSL/Utility 优先关系、AIContext 扩展、run history 模型、golden 策略、content pack 测试、accessibility/localization 标准 |
| **总计** | **27** | |

**R1 结论**：

Phase 5 执行文档的当前水平与 Phase 4 R1 审阅前高度同构——目标清晰、边界合理、风险有退路，但尚未进入"可驱动实现"的状态。与 Phase 4 经历的演进路径对比：

| Phase 4 路径 | Phase 5 对应 |
| --- | --- |
| R1：补 Kotlin 骨架、补 YAML 示例、建权威链 | **当前需要做的** |
| R2：补中间类型、公式变量来源、选择算法 | 补感知转换、confidence 衰减、DSL/Utility 管线 |
| R3：YAML ↔ Kotlin 对齐 | 待骨架落地后进行 |
| R4：跨引用闭合、实现提示 | 待示例落地后进行 |

**建议处理优先级**：

1. **最先处理 4 个 P0**——没有骨架和依赖表，整个文档无法驱动实现。
2. **其次处理 P1-R1-1 ~ P1-R1-5**——感知/仇恨/DeathAnalysis/AI trace 是 W1/W2 的核心 contract。
3. **然后处理 P1-R1-6 ~ P1-R1-9**——harness、checklist、replay 是 W3/W4 的验证基础。
4. **P1-R1-9 ~ P1-R1-14 和全部 P2**——可在骨架补齐后的 R2 中收口。

---

*审阅人：资深游戏设计与开发总监视角*
*审阅版本：Round 1*
*审阅状态：需要重大补充后再进行 R2*
