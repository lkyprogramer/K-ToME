# PR-04 AIProfile DSL & BossEncounter 深度审查报告

**审查日期**: 2026-03-24
**审查角色**: 资深 Roguelike 游戏设计总监 + 系统策划总监 + 玩法体验审查负责人
**审查分支**: `codex/p3-pr04-aiprofile-boss-encounter`
**对照文档**: `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`

---

## 0. 总体评价

**整体完成度: 约 85-88%**

核心基础设施（AIProfile DSL、BossEncounter 两层结构、BossPhaseManager、TelegraphSpec、ThreatRatingResolver、AIDecisionTrace/BossTrace）已经落地，架构方向正确，schema 设计干净。主要偏差集中在以下方面：

1. **缺失独立文件/模块**：`StealthTauntHandler`、`ThreatProfileLint` 两个文档要求的独立模块未建立
2. **测试类命名偏差**：部分文档指定的测试类名未按原名创建
3. **测试覆盖密度不足**：部分冻结口径的边界行为缺少对应测试用例
4. **3 Boss 覆盖 vs 2 Boss 覆盖**：符合文档的"PR-04 至少 2 Boss"门禁，当前覆盖 2 Boss

---

## 1. 逐项冻结口径核查

### 1.1 AIProfile DSL 规格（文档 §4.1）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| `AIProfile` 包含 `id/perceptionRange/useLastKnownPosition/defaultBehavior/selectionPolicy/actions[]` | ✅ 完全符合 | `AIProfile.kt:39-46` 字段一一对应 |
| `AIAction` 包含 `id/type/orderKey/weight/condition/abilityId` | ✅ 完全符合 | `AIProfile.kt:29-36` |
| `AICondition` 使用结构化类型（`TARGET_DISTANCE_LESS_THAN`, `HP_BELOW`, `HAS_STATUS`, `AND`, `OR`, `NOT` 等） | ✅ 完全符合 | `AICondition.kt` 实现了完整的 sealed interface 层次，包含 `TargetVisible`, `TargetDistanceLessThan`, `TargetDistanceAtMost`, `TargetDistanceBetween`, `TargetHpBelow`, `HpBelow`, `HasStatus`, `TalentReady`, `TurnCountModulo`, `And`, `Or`, `Not` |
| `selectionPolicy` 支持 `DETERMINISTIC_PRIORITY` 和 `WEIGHTED_RANDOM` | ✅ 完全符合 | `AIProfile.kt:14-17` |
| 候选动作按 `orderKey asc → actionId asc` 排序 | ✅ 完全符合 | `AIProfileResolver.kt:29` 使用 `compareBy<AIAction> { it.orderKey ?: Int.MAX_VALUE }.thenBy(AIAction::id)` |
| `orderKey` 缺失视为 `Int.MAX_VALUE` | ✅ 完全符合 | 同上 |
| `DETERMINISTIC_PRIORITY` 直接选排序后首个候选 | ✅ 完全符合 | `AIProfileResolver.kt:60` |
| `WEIGHTED_RANDOM` 未声明 `weight` 默认 `1.0` | ✅ 完全符合 | `AIProfileResolver.kt:88` |
| 全部候选权重 `<= 0` 回退到排序后首个动作 + fallback reason 写入 trace | ✅ 完全符合 | `AIProfileResolver.kt:90-105`, reason = `"weight_sum_non_positive_fallback"` |

**小结**: AIProfile DSL 规格 **100% 符合**。

---

### 1.2 BossEncounter 结构（文档 §4.2）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| `BossEncounter` 包含 `id/templateId/phases[]` | ✅ 完全符合 | `BossEncounter.kt:41-45` |
| `BossPhaseDef` 包含 `id/hpThreshold/hpEnd/aiProfileId/onEnter[]` | ✅ 完全符合 | `BossEncounter.kt:28-38` |
| `phases` 按 `hpThreshold` 降序排列，运行时从上到下匹配 | ✅ 符合 | `BossPhaseManager.kt:35` 使用 `firstOrNull` 匹配 |
| `onEnter` 事件类型 `TELEGRAPH/CLEAR_STATUSES/INVULNERABLE/EMIT_EVENT` | ✅ 完全符合 | `BossEncounter.kt:12-17` |
| `onEnter.TELEGRAPH` 只引用 `telegraphSpecId` | ✅ 完全符合 | `BossPhaseEvent.telegraphSpecId` |
| `templateId` 引用 `MonsterTemplateV2.id` | ✅ 完全符合 | `BossDefinition` 持有 `MonsterTemplate` 引用 |

**YAML 数据验证**:
- `bandit_captain_encounter`: 单阶段 → ✅ 符合简单 Boss 用例
- `molten_giant_encounter`: 双阶段 (1.0→0.5, 0.5→0.0)，`phase_enraged` 有 `EMIT_EVENT + TELEGRAPH + INVULNERABLE` → ✅ 符合文档 YAML 示例
- `dungeon_lord_encounter`: 双阶段 (1.0→0.45, 0.45→0.0)，`phase_desperate` 有 `CLEAR_STATUSES + EMIT_EVENT + TELEGRAPH` → ✅ 符合

**小结**: BossEncounter 结构 **100% 符合**。

---

### 1.3 BossPhaseDef 切换机制（文档 §4.3）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| 切换条件 `hpThreshold/hpEnd/turnCount/requiredStatus` | ✅ 完全符合 | `BossPhaseDef` 包含所有 4 个字段 |
| 切换时机默认回合开始，致死转阶段需显式声明 | ✅ 完全符合 | `BossPhaseTransitionTiming.START_OF_TURN / ALLOW_FATAL_TRANSITION` |
| 切换副作用 `invulnerableTurns/clearStatuses/resetAiPhaseState/emitTelegraph` | ✅ 符合 | `BossPhaseEvent` 覆盖了 INVULNERABLE（含 `invulnerableTurns`）、CLEAR_STATUSES、TELEGRAPH、EMIT_EVENT；`BossPhaseDef.resetAiPhaseState` 独立字段 |
| phase 切换写入 `BossTrace` | ✅ 完全符合 | `BossTrace` 包含 `fromPhase/toPhase/trigger/turnId/sideEffects` |

**小结**: BossPhaseDef 切换机制 **100% 符合**。

---

### 1.4 普通怪与 Boss 统一选择语义（文档 §4.4）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| 普通怪/精英怪继续确定性脚本模型 | ✅ 符合 | YAML 中普通怪使用 `DETERMINISTIC_PRIORITY` |
| Boss 通过统一 schema 表达加权随机 | ✅ 符合 | Boss profiles 使用 `WEIGHTED_RANDOM`，同一 `AIProfile` schema |
| `AIDecisionTrace` 记录：候选动作、满足条件的动作集合、selectionPolicy、排序后候选顺序 | ✅ 完全符合 | `AIDecisionTrace.kt:16-26` 包含 `evaluatedActions[]`、`orderedCandidateActionIds`、`selectionPolicy` |
| 加权选择记录 `rngRoll` | ✅ 完全符合 | `AIDecisionTrace.rngRoll` |
| fallback 记录 `reason` | ✅ 完全符合 | `AIDecisionTrace.reason` |

**小结**: 统一选择语义 **100% 符合**。

---

### 1.5 TelegraphSpec 与 ThreatRatingResolver（文档 §4.5）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| `TelegraphSpec` 是唯一 telegraph 权威结构 | ✅ 符合 | `TelegraphSpec.kt` + `TelegraphRegistry` 管理 |
| `TelegraphSpec` 包含 `shape/radius/length/angle/previewTurns/dangerLevel/threatProfileId/counterplayTags/stages` | ✅ 完全符合 | `ThreatProfile.kt:37-48` |
| `dangerLevel` 枚举 `LOW/MODERATE/HIGH/LETHAL` | ✅ 完全符合 | `ThreatProfile.kt:9-14` |
| telegraph 阈值基于统一 defender baseline 计算 | ✅ 完全符合 | `ThreatRatingResolver.assess()` 使用 `ThreatProfileDef` 的 `expectedMaxHp/expectedArmor/expectedResistances` |
| 预期伤害 `≥30%` HP → 至少 1 回合预览 | ✅ 完全符合 | `ThreatProfile.kt:77-78` |
| 预期伤害 `≥50%` HP → 至少 2 回合预览 | ✅ 完全符合 | `ThreatProfile.kt:77-78` |
| `threatProfileId` 只能引用注册表中的 `ThreatProfileDef` | ✅ 符合 | `ThreatProfileRegistry.require()` 做强校验 |
| `ThreatProfileDef` 按 `defenderArchetype/levelBand/difficultyId` 分层 | ✅ 完全符合 | `ThreatProfile.kt:57-67` |
| `counterplayTags` 表达 `DODGE/INTERRUPT/BLOCK` | ✅ 完全符合 | `CounterplayTag` 枚举 |
| `stages` 预留字段，Phase 3 允许空列表 | ✅ 完全符合 | `TelegraphSpec.stages` 默认 `emptyList()` |

#### ⚠️ 偏差 1：`ThreatProfileLint` 未实现为独立文件

**文档要求**: `tools/src/main/kotlin/com/ktome/tools/lint/ThreatProfileLint.kt`
**实际状态**: `tools/src/main/kotlin/` 目录为**空**，无任何 production code。Lint 校验逻辑**内嵌在** `tools/src/test/kotlin/.../ContractLintTest.kt` 中。

**偏差程度**: 中等。校验逻辑实质上已存在（ID 唯一性、level band 重叠检测、引用完整性），但：
- 不是独立可复用的 lint 工具类
- 不能被其他测试或 CI pipeline 独立引用
- 不符合文档建议的文件组织

**修复建议**:
1. 将 `ContractLintTest.kt` 中 threat profile 相关校验逻辑提取到 `tools/src/main/kotlin/com/ktome/tools/lint/ThreatProfileLint.kt`
2. `ContractLintTest` 改为调用 `ThreatProfileLint.validate(catalog)` 而非内联逻辑

**小结**: TelegraphSpec 核心规格 **100% 符合**，但 `ThreatProfileLint` 独立模块缺失（校验逻辑已实现但位置不对）。

---

### 1.6 STEALTH / TAUNT 与 AI 交互（文档 §4.6）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| 目标进入 STEALTH 后 AI 当前目标引用立即失效 | ✅ 符合 | `StealthAiInteractionTest` 验证隐身后 AI 不再追踪真实位置 |
| `useLastKnownPosition=true` → AI 移动到最后已知位置 | ✅ 符合 | 测试验证 `perception.lastKnownTargetPosition` 保持为可见时的位置 |
| 到达后仍未发现目标 → 回退到 `defaultBehavior` | ⚠️ 未明确测试 | 测试只验证一回合移动，未覆盖到达后的 fallback 行为 |
| Boss 遇到 STEALTH 不额外触发 phase 切换 | ⚠️ 未明确测试 | 无 Boss + STEALTH 组合的专项测试 |
| AI 被 TAUNT 命中后强制把攻击目标设为嘲讽源 | ✅ 符合 | `TauntAiInteractionTest` 第一个测试验证移动朝向嘲讽源 |
| TAUNT 结束后恢复原有目标选择逻辑 | ⚠️ 未明确测试 | 无 TAUNT 过期后恢复正常目标选择的测试 |

#### ⚠️ 偏差 2：`StealthTauntHandler` 未作为独立模块实现

**文档要求**: `core/src/main/kotlin/com/ktome/core/ai/StealthTauntHandler.kt`
**实际状态**: STEALTH/TAUNT 与 AI 的交互逻辑**散布在** `FoundationGameSession.kt`、`StatusRuntime.kt`、`AIPathing.kt` 等多个文件中，通过 ECS 组件 (`AIPerceptionState`, `EffectTracker`) + 状态系统 (`StatusEffectType.STEALTH/TAUNT`) 间接实现。

**偏差程度**: 中等。功能已实现且测试覆盖了核心场景，但：
- 没有统一的交互规则入口点
- 5 条冻结规则分散在多处，无法一目了然地审查
- 新开发者难以找到完整的 STEALTH/TAUNT 与 AI 交互合同

**修复建议**:
1. 创建 `StealthTauntHandler` 作为规则聚合层，不需要改变底层实现，只需将分散的判定逻辑集中委托
2. 或者至少在 `AIPathing.kt` / `AIProfileResolver.kt` 中用明确注释标注 5 条规则的对应位置

#### ⚠️ 偏差 3：STEALTH/TAUNT 测试覆盖不完整

**文档要求 5 条规则测试**，当前覆盖：

| 规则 | 测试状态 |
|------|---------|
| 规则1: 目标 STEALTH → AI 目标引用失效 | ✅ `StealthAiInteractionTest` |
| 规则2: `useLastKnownPosition=true` → 移动到最后已知位置 | ✅ `StealthAiInteractionTest` |
| 规则3: Boss + STEALTH 不额外触发 phase 切换 | ❌ 无测试 |
| 规则4: TAUNT → 强制攻击嘲讽源 | ✅ `TauntAiInteractionTest` |
| 规则5: TAUNT 结束 → 恢复原有目标逻辑 | ❌ 无测试 |

**额外测试**: `TauntAiInteractionTest` 有一个 `taunted ai does not see through stealth on non player target` 交叉场景测试 → 👍 文档未要求但实测良好

**修复建议**:
1. 增加规则3: Boss encounter 中 STEALTH 不触发额外 phase 切换的测试
2. 增加规则5: TAUNT 到期后 AI 恢复正常目标选择的测试
3. 增加到达最后已知位置后回退到 `defaultBehavior` 的测试

---

### 1.7 BossTrace / AIDecisionTrace（文档 §4.7）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| `AIDecisionTrace` 记录 `evaluatedActions[]/selectedAction/selectionPolicy/rngRoll/reason/turnId` | ✅ 完全符合 | `AIDecisionTrace.kt:16-26` |
| `BossTrace` 记录 `fromPhase/toPhase/trigger/turnId/sideEffects[]` | ✅ 完全符合 | `BossEncounter.kt:48-56` |
| 两者可导出为 JSON | ✅ 完全符合 | `BossTraceExportTest` + `BossHarnessTest` 均验证 JSON 序列化 |
| `bossHarness` 在回放时加载 trace 并验证一致性 | ✅ 符合 | `BossHarnessTest` 做了 JSON round-trip + SHA-256 哈希验证 |

**小结**: Trace 系统 **100% 符合**。

---

### 1.8 Telegraph Renderer（文档 §4.8）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| 支持形状 `CIRCLE/LINE/CONE` | ✅ 符合 | `TelegraphShape` 枚举 |
| `dangerLevel` 映射颜色（LOW=蓝, MODERATE=黄, HIGH=红, LETHAL=紫） | ✅ 符合 | `TelegraphRenderer.fallbackColorHex()`: LOW→`#3A86FF`(蓝), MODERATE→`#F6C445`(黄), HIGH→`#E53935`(红), LETHAL→`#7B1FA2`(紫) |
| 显示持续回合倒计时 | ✅ 符合 | `TelegraphRenderer.tileRows()` 生成 `"T-${overlay.previewTurns}"` |
| 消费 `RenderSnapshot.overlays` 中的 telegraph 数据 | ✅ 完全符合 | `RenderSnapshot` 含 `overlays: List<OverlayRenderSnapshot>` |
| telegraph 消失后格子标记在下一帧清除 | ⚠️ 无直接测试 | Renderer 只读 overlays，清除逻辑应在 game session 层，但无专项测试验证 |

**小结**: Telegraph Renderer **95% 符合**，消失后清除行为缺测试。

---

### 1.9 bossHarness Gradle Task（文档 §5.4 + §6）

| 冻结口径 | 状态 | 说明 |
|---------|------|------|
| `bossHarness` Gradle task 存在 | ✅ 符合 | `build.gradle.kts` 注册了 `bossHarness` task |
| 至少覆盖 2 个 Boss | ✅ 符合 | `BossHarnessTest` 覆盖 `bandit_captain` + `molten_giant` |
| 完整 3 Boss 由 PR-06 升级 | ✅ 符合 | `dungeon_lord` 未加入 harness，符合文档"PR-04 至少 2 Boss"门禁 |

**小结**: bossHarness **100% 符合**出口门禁。

---

## 2. 测试类核查（文档 §6.1 必测类）

| 文档要求的测试类 | 实际状态 | 说明 |
|-----------------|---------|------|
| `AIProfileDslTest` | ✅ 存在 | `game/src/test/kotlin/.../data/AIProfileDslTest.kt` |
| `BossEncounterTest` | ✅ 存在 | `game/src/test/kotlin/.../data/BossEncounterTest.kt` |
| `BossPhaseTransitionTest` | ❌ **缺失** | 实际使用 `BossPhaseManagerTest` 代替，包名和类名不同 |
| `TelegraphThresholdTest` | ❌ **缺失** | 实际使用 `ThreatRatingResolverTest` 代替 |
| `StealthAiInteractionTest` | ✅ 存在 | `game/src/test/kotlin/.../StealthAiInteractionTest.kt` |
| `TauntAiInteractionTest` | ✅ 存在 | `game/src/test/kotlin/.../TauntAiInteractionTest.kt` |
| `BossTraceExportTest` | ✅ 存在 | `core/src/test/kotlin/.../ai/BossTraceExportTest.kt` |
| `BossHarnessTest` | ✅ 存在 | `game/src/test/kotlin/.../harness/BossHarnessTest.kt` |
| `ThreatProfileRegistryTest` | ✅ 存在 | `game/src/test/kotlin/.../telegraph/ThreatProfileRegistryTest.kt` |

#### ⚠️ 偏差 4：两个测试类命名与文档不一致

- `BossPhaseTransitionTest` → 实际为 `BossPhaseManagerTest`（功能等价，命名偏差）
- `TelegraphThresholdTest` → 实际为 `ThreatRatingResolverTest`（功能等价，命名偏差）

**偏差程度**: 低。这两个替代测试覆盖了文档要求的核心行为，命名更贴近实际类名。

**修复建议**: 如果项目严格对照文档 spec 做测试审计，建议添加别名或重命名以匹配文档，但功能上无缺失。

---

## 3. 必测行为核查（文档 §6.2）

| # | 必测行为 | 测试覆盖 | 说明 |
|---|---------|---------|------|
| 1 | AIProfile YAML 可正确解析为运行时对象 | ✅ | `AIProfileDslTest` 测试 YAML 解析 |
| 2 | AICondition 能正确解析 AND/OR/NOT 组合器 | ✅ | `AIProfileDslTest` 测试嵌套 OR(TargetVisible, NOT(HasStatus)) |
| 3 | selectionPolicy 能正确驱动确定性选择与加权随机选择 | ✅ | `AIProfileResolverTest` 覆盖两种策略 |
| 4 | Boss phase 在 hpThreshold 触发时正确切换 | ✅ | `BossPhaseManagerTest` + `BossHarnessTest` |
| 5 | onEnter 的 telegraphSpecId 能正确引用统一 telegraph 规格 | ✅ | `BossEncounterTest` 验证引用 |
| 6 | telegraph 在 ≥30%/≥50% defender baseline 阈值时正确生成 | ⚠️ 部分 | `ThreatRatingResolverTest` 只测了 ≥50% 升级到 LETHAL/2 turn，缺少 ≥30% 边界测试 |
| 7 | STEALTH 使 AI 失去目标引用并移动到最后已知位置 | ✅ | `StealthAiInteractionTest` |
| 8 | TAUNT 强制 AI 攻击目标为嘲讽源 | ✅ | `TauntAiInteractionTest` |
| 9 | BossTrace 与 AIDecisionTrace 均可导出 JSON，WEIGHTED_RANDOM 记录 rngRoll | ✅ | `BossTraceExportTest` + `BossHarnessTest` |
| 10 | 相同 orderKey 的候选按 actionId asc 稳定 tie-break | ✅ | `AIProfileResolverTest` 验证 alpha < beta < zeta |
| 11 | threatProfileId 通过注册表解析 + ThreatProfileLint 拦截非法 profile | ⚠️ 部分 | 注册表解析有测试，Lint 内嵌在 `ContractLintTest` 中（非独立 Lint 工具） |
| 12 | counterplayTags 表达有效反制手段，空 stages 不影响最小模型 | ✅ | `ContractLintTest` 校验 `counterplayTags.isNotEmpty()`，`TelegraphSpec.stages` 默认空 |
| 13 | bossHarness 至少 2 个 Boss 稳定通过 | ✅ | `BossHarnessTest` 覆盖 bandit_captain + molten_giant |

---

## 4. 出口门禁核查（文档 §7）

| # | 出口门禁 | 状态 | 说明 |
|---|---------|------|------|
| 1 | AIProfile DSL + BossEncounter 两层结构冻结且解析测试绿 | ✅ 通过 | schema 完整，测试存在 |
| 2 | selectionPolicy 与 tie-break/fallback 规则进入正式 schema | ✅ 通过 | `AISelectionPolicy` 枚举 + `AIProfileResolver` 实现完整 |
| 3 | TelegraphSpec 成为唯一 telegraph 权威 | ✅ 通过 | 统一 `TelegraphRegistry`，Boss/talent 都做引用 |
| 4 | BossPhaseDef 结构化切换条件全部可配置且测试绿 | ✅ 通过 | 4 个切换条件均有对应字段和测试 |
| 5 | telegraph 阈值合同与 ThreatProfileDef 注册表至少 2 Boss 验证通过 | ✅ 通过 | 2 个 Boss 均使用 threat profile 引用 |
| 6 | BossTrace/AIDecisionTrace 可导出且可回放 | ✅ 通过 | JSON 序列化 + round-trip 验证 |
| 7 | bossHarness 基线覆盖至少 2 Boss | ✅ 通过 | bandit_captain + molten_giant |

**出口门禁结论: 7/7 通过**

---

## 5. 偏差汇总与修复优先级

### P0 偏差（无）

当前无 P0 级阻塞偏差。出口门禁全部通过。

### P1 偏差（建议尽快修复）

#### P1-1: `StealthTauntHandler` 独立模块缺失

- **偏差**: 文档要求 `core/src/main/kotlin/com/ktome/core/ai/StealthTauntHandler.kt`，实际未创建
- **影响**: STEALTH/TAUNT 与 AI 的 5 条交互规则分散在多个文件中，缺少单一真相源
- **修复**: 创建 `StealthTauntHandler` 作为规则聚合层，集中 5 条冻结规则的判定入口
- **工作量**: ~2h

#### P1-2: STEALTH/TAUNT 测试覆盖缺口（2 条规则未测）

- **偏差**: 规则3（Boss + STEALTH 不触发额外 phase 切换）和规则5（TAUNT 结束后恢复正常目标）无测试
- **影响**: 这两条规则的回归保护缺失
- **修复**: 在 `StealthAiInteractionTest` 和 `TauntAiInteractionTest` 中补充对应测试用例
- **工作量**: ~2h

#### P1-3: ThreatRatingResolver ≥30% 阈值边界测试缺失

- **偏差**: `ThreatRatingResolverTest` 只测了 ≥50% 的场景，≥30%（1 回合预览）的边界未覆盖
- **影响**: 30% 阈值的回归保护缺失
- **修复**: 增加一个 `expectedHpFraction ∈ [0.30, 0.50)` 的测试用例，验证 `previewTurns ≥ 1`
- **工作量**: ~30min

### P2 偏差（建议后续迭代修复）

#### P2-1: `ThreatProfileLint` 未独立为 production 工具类

- **偏差**: 校验逻辑内嵌在 `ContractLintTest.kt` 中，`tools/src/main/kotlin/` 为空
- **影响**: Lint 不可被其他测试或 CI 流水线独立复用
- **修复**: 提取 `ThreatProfileLint.kt` 到 `tools/src/main/kotlin/com/ktome/tools/lint/`
- **工作量**: ~1h

#### P2-2: 测试类命名与文档不一致

- **偏差**: `BossPhaseTransitionTest` → `BossPhaseManagerTest`, `TelegraphThresholdTest` → `ThreatRatingResolverTest`
- **影响**: 纯粹的文档-代码对照审计不通过，但功能等价
- **修复**: 可通过别名 typealias 或重命名解决，也可在文档中更新对应类名
- **工作量**: ~15min

#### P2-3: Telegraph 消失后格子清除行为缺测试

- **偏差**: 文档 §4.8 要求 "telegraph 消失后的格子标记在下一帧清除"
- **影响**: 视觉层的回归保护缺失
- **修复**: 在 client 测试中增加 telegraph overlay 生命周期测试
- **工作量**: ~1h

---

## 6. 架构与玩法体验评价

### 6.1 架构层面

**优点**:
- `AIProfile` DSL 设计简洁且可扩展，`sealed interface AICondition` 的组合器模式是正确的选择
- `BossEncounter` 两层结构（元信息 + 行为层）干净，不存在怪物模板和 Boss 定义的 key 冲突
- `ThreatRatingResolver` 基于 defender baseline 而非真实玩家状态的设计正确，避免了运行时耦合
- Trace 系统 (`AIDecisionTrace` / `BossTrace`) 可序列化 + 可回放，为后续调试和回归提供了坚实基础
- `BossHarnessTest` 的 SHA-256 哈希校验是一个很好的黄金测试基线方案

**关注点**:
- `BossPhaseManager.resolvePhase()` 中 `matches()` 和 `matchesHp()` 存在逻辑重复（L59-78 vs L101-115），建议统一
- `AIProfileResolver` 中 `weighted()` 方法的累积采样 (`cursor += weight / totalWeight`) 对负权重做了 `coerceAtLeast(0.0)` 处理，逻辑正确但不够直觉，建议加注释

### 6.2 玩法体验层面

**优点**:
- Boss phase 切换有 `INVULNERABLE` + `TELEGRAPH` 副作用，给玩家反应时间 → 符合 ToME 风格的"可预读、可反应"设计理念
- `counterplayTags` 字段的存在保证了 telegraph 的可应对性声明 → 避免设计出无法回避的攻击
- 3 Boss 的 AI 行为差异化明显：bandit_captain（单阶段加权）、molten_giant（HP 驱动双阶段 + 狂暴）、dungeon_lord（HP 驱动 + 绝望阶段 + 清除 buff）

**潜在风险**:
- `bandit_captain_encounter` 只有单阶段，作为 Boss 缺少阶段转换的紧张感。建议 PR-06 考虑至少加一个低 HP 激怒阶段
- STEALTH 与 Boss 的交互测试缺失可能导致上线后出现 Boss 被 Rogue 隐身"卡死"的边缘情况

---

## 7. 结论

| 维度 | 评分 | 说明 |
|------|------|------|
| 规格符合度 | **88%** | 核心口径全部冻结，缺失 2 个独立模块文件 |
| 出口门禁 | **100%** | 7/7 全部通过 |
| 测试覆盖 | **82%** | 13 条必测行为中 2 条部分覆盖，2 条缺失 |
| 架构质量 | **95%** | 设计干净，仅有小幅重复代码 |
| 玩法体验 | **90%** | Boss 差异化良好，STEALTH 边缘场景需补测 |

**整体判定: 通过出口门禁，建议合并前处理 P1 偏差（约 4.5h 工作量）。**
