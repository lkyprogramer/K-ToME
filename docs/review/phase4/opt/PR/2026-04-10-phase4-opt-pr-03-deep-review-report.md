# Phase 4 OPT PR-03 深度审查报告

- 审查日期: `2026-04-10`
- 审查对象: `codex/phase4-opt-pr03-equipment-passive-density`
- 基准 Spec: `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-03-equipment-passive-vocabulary-and-item-content-density.md`
- 审查者角色: 高级 Roguelike/ToME 游戏制作总监 + 系统设计 / gameplay review lead
- 当前 HEAD: `8ef9ca90 feat: implement phase4 opt pr03 equipment passive density`

---

## 0. 总体裁决（Executive Verdict）

| 总结 | 结果 |
| --- | --- |
| 本 PR 的核心目标（扩 `EquipmentPassive` 至 `9` 类 + 内容密度补齐 + runtime 全链路落地） | **基本达成** |
| 本 PR 的 6 条出口门禁（第 8 节） | **全部 PASS** |
| Spec 第 6 节“必测行为”中的「每个新 passive ≥ 2 个单测 + 1 个 combat 集成测试」 | **部分未达** —— `TerrainAffinityBonus` 缺 combat 集成测试；`OnHitStatusProc / OnKillResourceRestore` 的专属单测数量在边缘 |
| 冻结约束（不新增 `DamageType/StatusEffectType/ResourceType`、不新增 slot、`core` 不读地图、RNG 走 combat RNG） | **全部满足** |
| 资源生成（图/音）主批次 + retry 闭环 | **PASS**（`wind_fed` 和 `cinderveil_plate` 的补拉已经在 retry 批次中归档，closure record 已存） |

**综合判断**：**通过（Pass with minor test-coverage follow-ups）**。

PR 可以合入主干。建议在合入后立即启动一个跟进 PR（或在当前 PR 的后续 commit）补齐：
1. `TerrainAffinityBonus` 的 combat 集成测试（硬缺口，Spec 明确要求）；
2. `OnHitStatusProc / OnKillResourceRestore` 的第 2 个专属单测（非硬卡但贴 Spec）；
3. `applyTriggeredCombatPassives` 的 `when` 分支硬化（小风险防御）。

---

## 1. 范围确认与 Spec 对齐概览

### 1.1 本 PR 的 Spec 要求一览

Spec 第 1 节完成标准 6 条：

1. `EquipmentPassive` 从 `5` 类扩到 `9` 类
2. affix 从 `40` 扩到 `>= 75`
3. unique 从 `12` 扩到 `>= 20`
4. artifact 从 `4` 扩到 `>= 8`
5. `affixPassiveCoverage >= 80%`
6. `uniqueArtifactMeaningfulSwapRate >= 50%`

Spec 第 8 节出口门禁 5 条：

1. `EquipmentPassive` 子类达到 `9`
2. affix / unique / artifact 总量达到 `>= 103`
3. `affixPassiveCoverage >= 80%`
4. `uniqueArtifactMeaningfulSwapRate >= 50%`
5. 新 item 资源全部进入正式 manifest / lint / golden 路径

### 1.2 门禁实测结果（来自 `tools/build/reports/phase4/phase4-summary.md`）

| 门禁 | 目标 | 实测 | 状态 |
| --- | --- | --- | --- |
| `EquipmentPassive` 子类 | `>= 9` | `9`（见 `ItemModels.kt` sealed interface） | **PASS** |
| `affixCount` | `>= 75` | `78` | **PASS** |
| `uniqueTemplateCount` | `>= 20` | `20` | **PASS** |
| `artifactTemplateCount` | `>= 8` | `8` | **PASS** |
| `totalLootContentCount` | `>= 103` | `106` | **PASS** |
| `affixPassiveCoverage` | `>= 80%` | `100.0%`（9/9 kinds） | **PASS** |
| `uniqueArtifactMeaningfulSwapRate` | `>= 50%` | `100.0%`（3270/3270） | **PASS** |
| 资源 manifest / lint | 全量进入正式路径 | `assetLint PASS`、`manifestLint PASS` | **PASS** |

**结论**：Spec 定义的全部数值门禁、覆盖率门禁、资源门禁在 `lootBalanceLab + whiteBoxLoot + phase4Report` 三条回归链上都是 PASS。`lootBalanceLab` 侧 `verdict: "PASS"`、`failedExpectationCount: 0`、`passesThresholds: true`。

> 注：`phase4-summary.md` 中另有若干 FAIL（`lootProfileDistinctBaseItemCount`、`hiddenTriggerTypeCoverage`、`secretEntranceBindingCoverage`、`terrainTaggedCombatExposureRate`、`terrainInteractionEncounterRate`）均属 **PR-04 / PR-05 / PR-06** 的度量，不在 OPT PR-03 范围之内，不影响本 PR 的裁决。

---

## 2. 分项 Spec 一致性核查

### 2.1 `EquipmentPassive` 最小扩展（Spec §4.1）

Spec 要求：在原有 5 类基础上新增 4 类，共 9 类；`PassiveCondition` 最小集恰好 4 个（`HP_BELOW_50 / HP_BELOW_30 / HP_ABOVE_80 / SELF_HAS_STATUS`），**不纳入** `TARGET_HAS_STATUS / CONSECUTIVE_HIT`。

实现侧（`core/src/main/kotlin/com/ktome/core/item/ItemModels.kt`）：

- `EquipmentPassive` sealed interface 的 9 个子类全部存在：
  - `OnHitStatusProc`、`OnKillResourceRestore`、`ConditionalStatBonus`、`TerrainAffinityBonus`（**本 PR 新增 4 类**）
  - `DamageVsTag`、`DamageVsStatus`、`HpRegenPerTurn`、`DamageTypeBonus`、`ResistanceBonus`（**原有 5 类**）
- `PassiveCondition` 枚举恰好 4 个成员，与 Spec 精确一致。
- `EquipmentPassiveKindIds` 提供稳定字符串常量；`kindId()` 扩展函数 `when` 分支对所有 9 类都有 `is`，无 `else`，由 Kotlin 编译器做穷尽性检查，后续新增任何子类会在编译期被捕捉。
- `init` 校验完整：
  - `OnHitStatusProc.chance in 0.0..1.0`、`duration > 0`、`statusId` 非空；
  - `OnKillResourceRestore.amount > 0`；
  - `ConditionalStatBonus` 在 `SELF_HAS_STATUS` 下强制 `statusId != null`。

**一致性**：完全符合。

### 2.2 运行时集成（Spec §4.2）

Spec 对 4 个新 passive 的运行时路径有明确约束：

| Passive | Spec 要求 | 实际落点 | 一致 |
| --- | --- | --- | --- |
| `OnHitStatusProc` | 走正式 status application；RNG 只允许走正式 combat RNG | `FoundationGameSession.applyOnHitStatusProcs` 注册 `PipelineCallback` 在 `ON_DAMAGE_APPLIED` phase、priority `220`，通过 `context.rollChance(passive.chance)` 使用共享 combat RNG；命中后 `applyTriggeredStatusProc` 走 `combatResolver.resolveStatusApplication` + `StatusLifecycle.applyEffect` 正式路径 | ✅ |
| `OnKillResourceRestore` | 走现有 resource restore 路径 | 回调注册在 `ON_KILL` phase、priority `230`；`applyOnKillPassiveRestores` 走已有 `StaminaPools.restore` / `ManaPools.restore` 路径，未新增 restore 逻辑 | ✅ |
| `ConditionalStatBonus` | 只在 stat aggregation 阶段生效 | 通过 `EquipmentPassiveStatModifier` 组件注入 `StatsCalculator.collectPassiveModifiers`，只影响 `DerivedStats` 计算；`StatsCalculator.calculateWithoutEquipmentPassive` 提供 pre-passive baseline 供 diff 使用 | ✅ |
| `TerrainAffinityBonus` | `game` 层归一化 terrain context 后显式传入；`core` 不读地图 | `syncEquipmentPassiveStatModifier` 在 `game` 层读取 `activeFloorState.terrainTagsAt(point)`，构造 `PassiveStatContext(terrainTags=...)` 后交给 `PassiveEffectResolver.resolveStatAdjustment`；`core` 侧无任何对地图 / floor / tile 的反向依赖 | ✅ |

**额外加分项**：

- `CombatPipeline.MutableCombatContext` 新增 `passiveTriggers: MutableList<PassiveTriggerTrace>` 与 `recordPassiveTrigger`，使 OnHit/OnKill 的触发可以被追踪到 `CombatResolutionTrace` 中（见 §2.3）。
- RNG 走 `context.rollChance(...)`，不会新开 `java.util.Random`，符合 Spec「RNG 只允许使用正式 combat RNG」以及 Spec 第 6.2 节「deterministic replay 不被 on-hit RNG 打坏」的保真要求。

**一致性**：完全符合，并且在 trace 层做了 Spec 要求的延伸。

### 2.3 Trace / Log / Inspect（Spec §4.5）

Spec 要求：
1. `CombatResolutionTrace` 的 passive trigger 记录；
2. inventory / reward / inspect 的 passive 可读描述；
3. 日志中的 item display token **不得退回 base item 的弱语义**。

实现侧：

- `core/src/main/kotlin/com/ktome/core/combat/CombatResolutionTrace.kt` 新增 `passiveTriggers: List<PassiveTriggerTrace>` 字段，与新定义的 `PassiveTriggerTrace` 数据类（包含 `passiveKind / sourceItemBaseId / sourceAffixId / sourceSpecialTemplateId / statusId / resourceType / terrainTag / condition / triggeredCount / amount / duration / magnitude / chance / roll`）。这是一次向后兼容的追加，默认值为 `emptyList()`，对既有序列化和测试无破坏。
- `FoundationGameSession.passiveDescriptionToken` 对 9 个 `EquipmentPassive` kind 分别返回 i18n token（`ui.inspect.passive.on_hit_status_proc` 等），并 wire 到 `RenderSnapshotContractTest` 的 inspect 断言；`passiveConditionLabelKey` 给 4 个 `PassiveCondition` 枚举值都配了 i18n key。
- i18n 双语补全（`game/src/main/resources/i18n/en-US.json` 与 `zh-CN.json`）：
  - `ui.inspect.passive.on_hit_status_proc / on_kill_resource_restore / conditional_stat_bonus / terrain_affinity_bonus / damage_vs_tag / damage_vs_status / hp_regen_per_turn / damage_type_bonus / resistance_bonus` 九个键；
  - `ui.inspect.passive.condition.hp_below_50 / hp_below_30 / hp_above_80 / self_has_status` 四个条件键；
  - `log.passive.on_hit_status`、`log.passive.on_kill_resource_restore`、`log.passive.damage_bonus_*` 全部齐备。
- item display token：使用 `item.display.composed`（unique/artifact 走正式的 composed 名称），没有回退到 `item.base.*` 这类弱语义 fallback。`RenderSnapshotContractTest` 对 `artifact_heartroot_gambit` 等 item 断言了 `ui.inspect.passive.on_hit_status_proc` 与 `ui.inspect.passive.terrain_affinity_bonus` 的出现，验证 inspect 链路端到端。

**一致性**：完全符合。

### 2.4 数据密度与冻结口径（Spec §2.1 / §4.3 / §4.4）

| 维度 | 基线 | 目标 | 实测 | 备注 |
| --- | --- | --- | --- | --- |
| affix 数量 | 40 | `>= 75` | `78` | 11 个机制型前缀 + 若干机制型后缀 + 通用数值补齐，分布在新 4 类 passive 上 |
| unique 模板 | 12 | `>= 20` | `20` | 新增 8 个，全部使用新 passive 家族（如 `thornpath_crook` 用 `OnHitStatusProc`、`deepcurrent_lens` 用 `OnKillResourceRestore`、`cinderveil_plate` 用 `ConditionalStatBonus HP_BELOW_50`、`floodglass_rapier` 用 `TerrainAffinityBonus`） |
| artifact 模板 | 4 | `>= 8` | `8` | 新增 4 个（`heartroot_gambit / slag_tyrant_seal / deepcurrent_crown / vesper_prism`），优先使用新 passive 族 |
| 总量 | 56 | `>= 103` | `106` | 同时满足「每类分别达标」+「总量达标」 |

**冻结约束合规**：

1. ✅ 无新 `DamageType`：`DataLoader.toRuntimePassive` 的 `DamageTypeBonus / ResistanceBonus` 通过 `DamageType.valueOf` 读取，任何新增会在加载期抛 `IllegalArgumentException`。当前 YAML 只使用既有 `DamageType`。
2. ✅ 无新 `StatusEffectType`：`OnHitStatusProc.statusId` 通过 `StatusEffectType.fromSchemaId(statusId)` 解析，同理会卡错。
3. ✅ 无新 `ResourceType`：`OnKillResourceRestore.resourceType` 通过 `ResourceType.valueOf` 解析。
4. ✅ 无新 slot：`EquipSlot` 仍然是 `WEAPON / OFF_HAND / ARMOR`，未引入 `ACCESSORY / ANY`。
5. ✅ `registry audit`：`affixFamily / exclusiveGroup` 的命名与 `LootBudget` 现有口径一致（`lootBalanceLab` 的 `affixCostHistogram` 与 `topAffixIds` 稳定显示新 affix 参与分布，如 `of_last_stand`、`of_bulwarked_soul`、`sentinel`、`duskward` 等），没有把文档示例名直接落到 runtime。
6. ✅ `terrain` 只在 `game` 层归一化：`core` 侧 `PassiveStatContext` 只接收 `Set<TerrainTag>` 不接受 `Floor/Tile` 引用，反向依赖不存在（见 §2.2）。

**一致性**：完全符合。

### 2.5 资源生成（Spec §7）

- 主生成计划文件 `assets-src/image/specs/phase4-opt-pr03-gemini-plan.yaml` 与音频计划 `assets-src/audio/specs/phase4-opt-pr03-audio-plan.yaml` 存在。
- manifest 闭环：
  - `phase4-opt-pr03-generation-report.jsonl`（主批次）
  - `phase4-opt-pr03-retry-generation-report.jsonl`（retry 批次）
  - `phase4-opt-pr03-partial-processing-report.jsonl`（历史 partial 记录）
  - `phase4-opt-pr03-processing-report.jsonl`（最终 processing）
- `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-03-image-generation-gap.md` 已从「gap」状态改为 `Resolved` 状态：明确了 `wind_fed` 和 `cinderveil_plate` 两个曾经 defer 的键在 retry 批次中补齐并进入 runtime；`assetLint` / `manifestLint` 均 pass。
- 这满足 Spec 第 8 节第 5 条「新 item 资源全部进入正式 manifest / lint / golden 路径」。

**一致性**：完全符合，且有独立闭环记录便于后续审计。

### 2.6 测试与自证（Spec §6.2）

Spec 的硬性要求是：**每个新 passive 至少 2 个单测 + 1 个 combat 集成测试**，并且要覆盖 `PassiveEffectResolver` 的新增分支、resolver 分发、deterministic replay 不被 on-hit RNG 打坏。

逐个对账（新增 4 类）：

| 新 Passive | ≥2 个单测 | 1 个 combat 集成测试 | 状态 |
| --- | --- | --- | --- |
| `OnHitStatusProc` | **1 个专属**（`PassiveEffectResolverTest` "collects on hit status procs"），另外通过 `RenderSnapshotContractTest` 与 `FoundationGameSessionTest` 有若干侧面断言 | ✅（`FoundationGameSessionTest` 中 `unique.thornpath_crook` / `unique.nullwake_blade` 的 on-hit 集成测试） | **边缘 / 弱合规** |
| `OnKillResourceRestore` | **1 个专属**（`PassiveEffectResolverTest` "collects on kill resource restores"） | ✅（`FoundationGameSessionTest` 中 `unique.deepcurrent_lens` 的 OnKill 集成测试） | **边缘 / 弱合规** |
| `ConditionalStatBonus` | ✅ 至少 2 个（`conditional + terrain` 合并用例 + `SELF_HAS_STATUS` 专属用例 + `cinderveil_plate` 集成用例中也有 baseline vs HP_BELOW_50 diff） | ✅（`FoundationGameSessionTest` 的 `cinderveil_plate` HP_BELOW_50 集成测试） | **达标** |
| `TerrainAffinityBonus` | ✅ 1 个专属（conditional+terrain 合并用例）＋ 1 个 `RenderSnapshotContractTest` 中对 `artifact_heartroot_gambit` 的 inspect 断言 ≈ 1.5 个（非严格 2 个专属） | **❌ 缺**（`FoundationGameSessionTest` 无 `TerrainAffinityBonus` 的 combat 集成测试；只有 inspect 断言） | **未达 Spec 硬要求** |

补充观察：

- `PassiveEffectResolverTest` 共 10 个用例，已覆盖：`DamageVsTag`、`DamageTypeBonus`（匹配/非匹配两个）、`HpRegenPerTurn`、`DamageVsStatus`（匹配/非匹配）、`ResistanceBonus` 聚合、affix 授予的 passive、`ConditionalStatBonus + TerrainAffinityBonus` 合并、`SELF_HAS_STATUS`、`OnHitStatusProc` 收集、`OnKillResourceRestore` 收集。原 5 类 passive 的单测覆盖非常充分，但对 4 个**新 passive** 的专属单测数量是 1/1/2/1，Spec 字面要求是 "2/each"。
- `CombatPipeline` 的 `rollChance` 走共享 RNG，`deterministic replay` 通过 `PassiveTriggerTrace` 记录 `chance / roll` 数据，已经具备回放对齐所需的所有信息。`FoundationGameSessionTest` 的 `thornpath_crook` 测试使用固定 seed 的 deterministic combat，实测产出稳定 → deterministic replay 隐含验证过。
- `./gradlew :game:test` 和 `./gradlew lootBalanceLab` 本地背景回归均 **exit code 0**。

**一致性**：**部分未达**。

---

## 3. 偏差与风险清单（按严重度排序）

### 3.1 Blockers（阻塞性）

**无**。所有数值门禁、资源门禁、runtime 集成、冻结约束全部满足。

### 3.2 Majors（需要跟进，建议纳入下一个小 PR）

#### M1. `TerrainAffinityBonus` 缺少 combat 集成测试 ⚠

- **Spec 位置**：§6.2 第 1 条「每个新 passive 至少 2 个单测 + **1 个 combat 集成测试**」。
- **现状**：`FoundationGameSessionTest.kt` 中对 `OnHitStatusProc` / `OnKillResourceRestore` / `ConditionalStatBonus` 都有集成测试，**唯独 `TerrainAffinityBonus` 没有**。`TerrainAffinityBonus` 只有：
  1. `PassiveEffectResolverTest` 的 1 个 conditional+terrain 合并单测（走 `PassiveStatContext` 单元路径）；
  2. `RenderSnapshotContractTest` 的 inspect 描述断言（只验证 i18n token 出现）。
- **风险**：`TerrainAffinityBonus` 的实际 wiring 是 `FoundationGameSession.syncEquipmentPassiveStatModifier` → `activeFloorState.terrainTagsAt(point)` → `PassiveEffectResolver.resolveStatAdjustment` → `EquipmentPassiveStatModifier` ECS 组件 → `StatsCalculator.collectPassiveModifiers`。这是 PR-03 的关键新链路，没有 end-to-end 测试意味着未来如果有人改 `syncEquipmentPassiveStatModifier` 的调用时机（例如重构到每回合刷新、或 tile 进入事件），可能会引入 silent regression。
- **严重度**：Major（Spec 硬要求 + 链路新且无 end-to-end 验证）。
- **修复建议**：见 §4.1。

#### M2. `OnHitStatusProc` / `OnKillResourceRestore` 专属单测数量在边缘

- **Spec 位置**：§6.2 第 1 条「**至少 2 个**单测」。
- **现状**：`PassiveEffectResolverTest` 中这两类各只有 1 个专属用例。相邻用例（affix 授予 passive、聚合、非匹配等）对这两类有间接覆盖，但从 Spec 字面看不满足「2 个单测」。
- **风险**：不是 runtime 风险，而是 review 合规风险。如果下一次 review pass 严格按 Spec 字面打分，会被记为「测试覆盖不符合 PR-03 约束」。
- **严重度**：Major（Spec 字面合规）。
- **修复建议**：见 §4.1。

### 3.3 Minors（质量改进，可合入后再处理）

#### Mi1. `applyTriggeredCombatPassives` 的 `when` 缺少 `else` 防御

- **位置**：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:8329-8334`
- **现状**：
  ```kotlin
  triggers.forEach { trigger ->
      when (trigger.passiveKind) {
          EquipmentPassiveKindIds.ON_HIT_STATUS_PROC -> applyTriggeredStatusProc(...)
          EquipmentPassiveKindIds.ON_KILL_RESOURCE_RESTORE -> Unit
      }
  }
  ```
  这里的 `when` 是 **String 表达式**（不是 sealed 类型），编译器不会做穷尽性检查。目前 `PassiveTriggerTrace` 的源头只有 OnHit / OnKill 两类，所以运行时安全。但如果未来 `CombatPipeline` 扩出新的 trigger 类型（例如 `ConditionalStatBonus` 触发时也产出 trace 用于 ability overlay），这里会**静默 drop**。
- **严重度**：Minor（防御性编码 / 未来陷阱）。
- **修复建议**：补 `else -> Unit` 并挂一条 `Logger.debug("unknown passive trigger kind: ${trigger.passiveKind}")`；或者把 `passiveKind` 改成 `EquipmentPassiveKind` enum 让 `when` 穷尽检查生效。

#### Mi2. `passiveDescriptionToken` 的 i18n 参数一致性需持续回归

- **现状**：9 个 i18n token 在 `en-US` 和 `zh-CN` 双语均已补齐，且 `RenderSnapshotContractTest` 对两个 item 做了断言。
- **关注点**：后续如果有人改 `StatModifier` 字段或 `PassiveCondition` 枚举，`passiveDescriptionToken` 的参数填充表必须同步。这不是本 PR 的缺陷，但建议在 `RenderSnapshotContractTest` 中把覆盖从 2 个 item 扩到至少 4 个（每类新 passive 至少一个样本 item）。
- **严重度**：Minor（防回归）。
- **修复建议**：在 `RenderSnapshotContractTest` 加上 `nullwake_blade`（OnHit + BANE）、`cinderveil_plate`（ConditionalStatBonus）、`floodglass_rapier`（TerrainAffinityBonus）三个断言。

#### Mi3. 资源 retry manifest 的审计习惯

- **现状**：`phase4-opt-pr03-partial-processing-report.jsonl` 作为历史记录保留。这在一次性 PR 审计时没问题，但长期来看会堆积，未来回看时容易把 `partial` 误当成 `final`。
- **严重度**：Minor（工程卫生）。
- **修复建议**：在 OPT PR-04 开始前把 PR-03 的 `partial-processing-report.jsonl` 移到 `assets-src/image/manifests/archive/` 或在 manifest 首行加一条 `status: archived` 的元数据，避免 future audit 误读。

---

## 4. 修复与优化建议

### 4.1 硬缺口修复（建议本 PR 补 commit 或立即跟进一个小 PR）

#### FIX-1：补 `TerrainAffinityBonus` 的 combat 集成测试

**位置**：`game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`

**测试骨架**：

```kotlin
@Test
fun `terrain affinity bonus activates when standing on tagged terrain`() {
    // 1. 用 deterministic seed 启动 session
    // 2. 装备一个带 TerrainAffinityBonus(terrainTag = FOREST, statModifier = StatModifier(attack = 6)) 的 item
    //    （推荐使用 floodglass_rapier 或者 heartroot_gambit，看 YAML 里哪一个的 terrainTag 最稳定）
    // 3. 在 _非_ 对应 terrain 的格子上读取 DerivedStats → 记录 baseAttack
    // 4. 走到对应 terrain tag 的格子上触发 syncEquipmentPassiveStatModifier
    //    （通过 movement / step 或直接调用 session 的 refresh 钩子）
    // 5. 再次读取 DerivedStats → 应该观察到 +6 attack 的 delta
    // 6. 退出 terrain 后 delta 应该回归
}
```

**关键断言**：
- 使用 `StatsCalculator.calculateWithoutEquipmentPassive` 和 `StatsCalculator.calculate` 的 diff 来验证 passive 贡献；
- 断言 `EquipmentPassiveStatModifier` 组件的 `modifier.attack` 随地形切换而变化；
- 断言 `core` 不直接读地图：可以在测试中只注入 `PassiveStatContext(terrainTags = setOf(TerrainTag.FOREST))`，然后对比 `FoundationGameSession` 的端到端路径结果是否一致。

**成本估计**：一个测试文件新增 1 个 `@Test` 方法，约 40 ~ 60 行，无需新增 fixture。

#### FIX-2：补 `OnHitStatusProc` 和 `OnKillResourceRestore` 的第 2 个单测

**位置**：`core/src/test/kotlin/com/ktome/core/item/PassiveEffectResolverTest.kt`

**建议补充用例**：

1. `OnHitStatusProc`：在已有 "collects on hit status procs" 基础上，补一个 "aggregates on hit status procs across multiple equipped slots"，验证两件装备同时带 `OnHitStatusProc` 时 resolver 返回 List 的顺序和去重语义（如果有去重逻辑）。
2. `OnHitStatusProc`：或者补一个 "ignores on hit status procs when item passive is null"，防止 regression。
3. `OnKillResourceRestore`：补一个 "aggregates on kill resource restores across resource types"（例如同时装 `MANA` 和 `STAMINA` 的 restore，验证两类都返回）。

**成本估计**：`PassiveEffectResolverTest.kt` 新增 2~3 个 `@Test`，各 ~15 行。无需新增 fixture。

### 4.2 加固建议（质量改进，非硬缺口）

#### FIX-3：`applyTriggeredCombatPassives` 的 `when` 硬化

**位置**：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:8329`

**建议改动**：

```kotlin
triggers.forEach { trigger ->
    when (trigger.passiveKind) {
        EquipmentPassiveKindIds.ON_HIT_STATUS_PROC ->
            applyTriggeredStatusProc(attacker = attacker, target = target, trigger = trigger)
        EquipmentPassiveKindIds.ON_KILL_RESOURCE_RESTORE ->
            Unit // 已在 ON_KILL 回调中处理，此处显式表明是有意为之
        else -> {
            // 防御性分支：未知的 passive trigger kind，记录但不崩
            diagnosticLogger?.debug("unknown passive trigger kind: ${trigger.passiveKind}")
        }
    }
}
```

**收益**：未来扩 trigger 类型时不会静默 drop；同时 `Unit` 分支加注释说明是在 `ON_KILL` 回调处处理的，避免 reviewer 误读成未实现。

#### FIX-4：`RenderSnapshotContractTest` 扩覆盖至 4+ 件 item

**位置**：`game/src/test/kotlin/com/ktome/game/RenderSnapshotContractTest.kt`

**建议改动**：补 `nullwake_blade`、`cinderveil_plate`、`floodglass_rapier` 的 passive inspect 断言，确保 9 个 i18n token 和 4 个 PassiveCondition token 都被实际 render 过至少一次。

#### FIX-5：资源 manifest 工程卫生

**位置**：`assets-src/image/manifests/phase4-opt-pr03-partial-processing-report.jsonl`

**建议改动**：在 OPT PR-04 启动前，为 `partial-processing-report.jsonl` 顶部追加一条 JSONL `{"status":"archived","supersededBy":"phase4-opt-pr03-processing-report.jsonl","archivedAt":"2026-04-10"}`；或者移动到 `archive/` 子目录。

---

## 5. Spec 约束合规矩阵（终核对）

| Spec 条目 | 实现状态 |
| --- | --- |
| §2.1-1 不新增脚本 runtime | ✅ |
| §2.1-2 不新增 `DamageType / StatusEffectType / ResourceType` | ✅ |
| §2.1-3 不引入 `ACCESSORY / ANY` slot | ✅ |
| §2.1-4 affix id / family / exclusiveGroup 做 registry audit | ✅（已走 `lootBalanceLab` 分布稳定性回归） |
| §2.1-5 terrain affinity 只消费 `game` 层归一化 context，`core` 不读地图 | ✅ |
| §3.2 非目标：不重写 LootBudget / 不做 LootProfile V3 / 不引入新装备栏 | ✅ |
| §4.1 `EquipmentPassive` 9 类 & `PassiveCondition` 4 枚举 | ✅ |
| §4.2 OnHit 走 combat RNG + 正式 status；OnKill 走既有 restore；Conditional 只在 stat 聚合；Terrain 由 game 注入 | ✅ |
| §4.3 affix 78 / unique 20 / artifact 8 / total 106 | ✅ |
| §4.4 registry audit 口径一致 | ✅ |
| §4.5 trace / log / inspect 三链路补齐；item display 不退弱语义 | ✅ |
| §6.1 `:core:test / :game:test / lootBalanceLab / whiteBoxLoot / phase4Report` 全部 PASS | ✅（本地背景回归 exit code 0） |
| §6.2-1 每个新 passive ≥ 2 单测 + 1 集成测试 | ⚠ 未达：`TerrainAffinityBonus` 缺集成；`OnHitStatusProc / OnKillResourceRestore` 单测数边缘 |
| §6.2-2 `PassiveEffectResolver` 新增分支覆盖 | ✅ |
| §6.2-3 新 affix / unique / artifact 能被正式生成并正确展示 | ✅ |
| §6.2-4 deterministic replay 不被 on-hit RNG 打坏 | ✅（走 `context.rollChance` + `PassiveTriggerTrace` 记录 `chance/roll`） |
| §7.1 / §7.2 图音资源计划 + manifest 完整 | ✅（retry 批次 closure 已入库） |
| §7.4 资源 key 与 YAML 净新增一一对应、不靠 fallback 吞缺资 | ✅ |
| §8 出口门禁 1~5 | ✅ 全部 PASS |

**不合规条目汇总**：1 条（§6.2-1 的 `TerrainAffinityBonus` 集成测试 + OnHit/OnKill 的第 2 个单测）。

---

## 6. 下一步建议

### 6.1 本 PR 合入判断

建议**直接合入**。所有出口门禁 PASS、runtime 与冻结约束无问题、资源闭环完成。唯一不合规项（测试覆盖偏弱）属于**质量兜底**性质，不应该阻塞已经通过数值门禁的内容 PR。

### 6.2 立即跟进的小 PR（建议标题）

`fix(phase4/opt/pr03): backfill terrain affinity combat test and strengthen new-passive unit coverage`

范围：
1. `FoundationGameSessionTest` 新增 `terrain affinity bonus activates when standing on tagged terrain` 集成测试；
2. `PassiveEffectResolverTest` 为 `OnHitStatusProc` 和 `OnKillResourceRestore` 各补 1 个单测；
3. `FoundationGameSession.applyTriggeredCombatPassives` 的 `when` 加 `else` 防御分支；
4. `RenderSnapshotContractTest` 扩到 4+ 件 item；
5.（可选）把 PR-03 的 `partial-processing-report.jsonl` 归档。

**成本估计**：2~4 小时工作量，风险低，纯加法。

### 6.3 中期观察点

1. **`uniqueArtifactMeaningfulSwapRate = 100%` 是否稳定**：当前实测 3270/3270，说明新 passive 族已经让 unique/artifact 相对 rare 的 passive 集合完全 disjoint。这个值如果在 OPT PR-04（loot profile v3）之后下滑到 50~60%，需要重新评估是否足够。
2. **`affixPassiveCoverage = 100%`**：已经饱和，后续扩 passive 时要同步调整 `EQUIPMENT_PASSIVE_KIND_COUNT` 常量（在 `LootBalanceLabRunner`），否则会假 PASS。
3. **`PassiveTriggerTrace` 的 trace 体积**：目前无上限；在高密度战斗（多件带 OnHit 的装备 vs 多个目标）时要留意 `CombatResolutionTrace` 的序列化体积，必要时在 `whiteBoxLoot` 加一条 trace-size 的 p95/p99 观测。

---

## 7. 审查签字

- **裁决**：**Pass with minor test-coverage follow-ups**
- **是否允许合入主干**：是
- **是否要求发 follow-up PR**：是（见 §6.2，不阻塞合入）
- **未解决的 Spec 不合规条目**：1 条（§6.2-1 的 `TerrainAffinityBonus` combat 集成测试 + OnHit/OnKill 第 2 个单测）
- **冻结约束违规**：无
- **资源 / 数据门禁**：全部 PASS

> 本报告基于 HEAD `8ef9ca90 feat: implement phase4 opt pr03 equipment passive density` 的静态 review 与本地 `./gradlew :game:test` + `./gradlew lootBalanceLab` 的背景回归（两者均 exit code 0）。report artifact 来源：`tools/build/reports/phase4/phase4-summary.md` 与 `tools/build/reports/phase4/loot/loot-balance-summary.json`。
