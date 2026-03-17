> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
> `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-04 AIProfile DSL、TelegraphSpec 与 BossEncounter

**阶段**: `Phase 3 / P3-W4`  
**优先级**: `P0`  
**前置条件**: `P3-W2` 完成（因为 `STEALTH / TAUNT` 交互依赖状态生命周期合同），`P3-W3` 的 `telegraphRef` 与说明语义已冻结  
**对应问题**: Phase 2 的 AI 仍是 Layer 2 简单脚本（`CHASE / KITE / PATROL`），没有 Boss phase 切换、统一 telegraph 规格、`BossTrace / AIDecisionTrace` 和可回放的选择语义，无法支撑 Phase 3 的深度 Boss 战与长局结构。

**Lane-parallel 拆分**：

- **W4a (Rules Lane)**: `AIProfile` DSL + `BossEncounter` + `BossPhaseDef` + `TelegraphSpec` + `STEALTH/TAUNT` 交互 + `AIDecisionTrace / BossTrace`
- **W4b (Client Lane)**: telegraph renderer（形状/颜色/危险级/持续回合显示）

---

## 1. 阶段目标

将 Phase 2 的简单脚本化 AI 升级为支持 Boss phase 切换、统一 telegraph 规格和可导出调试追踪的完整 AI 基础设施。

完成标准：

1. AI 继续走脚本化 DSL，不进入行为树平台化。
2. `BossEncounter` 两层结构冻结（`BossEncounter` 元信息层 + `AIProfile` 行动层）。
3. 普通怪/精英怪的确定性模型保持，但 schema 上不再隐式混用 `priority` 与 `weight`。
4. `AIProfile` 显式声明 `selectionPolicy`，并冻结 tie-break 规则。
5. `BossPhaseDef` 切换条件使用结构化字段（`hpThreshold / hpEnd / requiredStatus / turnCount`），不引入字符串表达式解析器。
6. `TelegraphSpec` 成为唯一 telegraph 权威结构，`PR-03` 与 `BossEncounter` 都只做引用。
7. telegraph 伤害阈值基于统一的 `ThreatRatingResolver` / defender baseline 计算。
8. `STEALTH / TAUNT` 与 AI 交互的 5 条规则冻结。
9. `AIDecisionTrace` 与 `BossTrace` 可导出。
10. `bossHarness` 在本 PR 建立工具基线，至少覆盖 2 个 Boss；Phase 3 roster-complete 的 3 Boss 覆盖扩展留给 `PR-06`。

## 2. 当前问题

1. Phase 2 AI 只有 `CHASE / KITE / PATROL` 三种行为模板，没有 Boss 级 phase 切换和动作选择权重。
2. `priority` 和 `weight` 两套选择语义同时存在，但 schema 没有显式告诉运行时该用哪套策略。
3. `talent.telegraph` 与 `BossEncounter.onEnter` 都在写 telegraph，正在形成两套权威。
4. telegraph 的 `>= 30% / >= 50% 玩家最大 HP` 阈值没有定义“参考哪种玩家防御基线”。
5. 没有 `BossTrace / AIDecisionTrace`，Boss 战调试依赖人工观察。
6. 没有 `bossHarness` 自动化回归工具。
7. `selectionPolicy` 已有类型名，但 `orderKey / weight` 缺失值、同序候选和全 0 权重的 fallback 仍未冻结。
8. `TelegraphSpec.threatProfileId` 已出现，但 baseline threat profile 还没有正式注册表与 lint 入口。

### 2.1 本 PR 必须冻结的口径

1. AI 走脚本化 DSL，不走行为树平台。
2. `BossEncounter` 两层结构（`BossEncounter + AIProfile`）保持。
3. `AIProfile` 必须显式声明 `selectionPolicy`。
4. `BossPhaseDef` 切换条件只使用结构化字段。
5. `TelegraphSpec` 是唯一 telegraph 权威结构。
6. telegraph 伤害阈值合同固定为：
   - 预期伤害 `>= 30%` 标准 defender 最大 HP：至少 1 回合预览
   - 预期伤害 `>= 50%`：至少 2 回合预览
7. 危险级枚举统一使用 `LOW / MODERATE / HIGH / LETHAL`。
8. `STEALTH / TAUNT` 与 AI 的 5 条交互规则冻结。
9. `bossHarness` 在 `PR-04` 的出口门禁是“至少 2 个 Boss”，完整 3 Boss 覆盖由 `PR-06` 升级，不视为口径冲突。
10. 候选动作在进入 policy 前统一按 `orderKey asc -> actionId asc` 排序；`orderKey` 缺失按 `Int.MAX_VALUE` 处理。
11. `WEIGHTED_RANDOM` 的缺省 `weight` 为 `1.0`；若候选集合权重和 `<= 0`，回退到排序后的首个动作并写入 trace reason。

## 3. 范围与非目标

### 3.1 范围

1. [W4a] `AIProfile` DSL 规格与解析。
2. [W4a] `BossEncounter` 两层结构实现。
3. [W4a] `BossPhaseDef`（切换条件 / 时机 / 副作用）。
4. [W4a] 普通怪 / 精英怪确定性模型与 Boss 加权模型的统一表达。
5. [W4a] `TelegraphSpec` 与 `ThreatRatingResolver`。
6. [W4a] `STEALTH / TAUNT` 与 AI 交互合同。
7. [W4a] `BossTrace / AIDecisionTrace`。
8. [W4b] telegraph renderer。
9. `bossHarness` Gradle task。

### 3.2 非目标

1. 不在本 PR 创建全部 Boss/精英怪的 AI profile 数据（`P3-W6`）。
2. 不在本 PR 实现 Phase 5 的 Tactical AI / Utility scoring（Phase 5）。
3. 不在本 PR 做行为树引擎或更复杂的 AI 框架。
4. 不在本 PR 把 long-run 的 3 Boss 全量覆盖要求提前搬进工具基线。

## 4. 技术方案

### 4.1 [W4a] `AIProfile` DSL 规格

建议文件：

```text
core/src/main/kotlin/com/ktome/core/ai/AIProfile.kt
core/src/main/kotlin/com/ktome/core/ai/AIAction.kt
core/src/main/kotlin/com/ktome/core/ai/AICondition.kt
game/src/main/resources/data/ai/*.yaml
core/src/test/kotlin/com/ktome/core/ai/AIProfileDslTest.kt
```

冻结口径：

1. `AIProfile` 包含：
   - `id`
   - `perceptionRange`
   - `useLastKnownPosition`
   - `defaultBehavior`
   - `selectionPolicy`
   - `actions[]`
2. 每个 `AIAction` 包含：
   - `id`
   - `type`
   - `orderKey`（可选）
   - `weight`（可选）
   - `condition`（可选）
   - `abilityId`（可选）
3. `AICondition` 使用结构化类型（如 `TARGET_DISTANCE_LESS_THAN`, `HP_BELOW`, `HAS_STATUS`, `AND`, `OR`, `NOT` 等），不使用字符串表达式。
4. `selectionPolicy` 第一版至少支持：
   - `DETERMINISTIC_PRIORITY`
   - `WEIGHTED_RANDOM`
5. 普通怪通常走 `DETERMINISTIC_PRIORITY`；Boss phase 通常走 `WEIGHTED_RANDOM`。
6. 候选动作在进入 policy 前统一按 `orderKey asc -> actionId asc` 排序；`orderKey` 缺失视为 `Int.MAX_VALUE`。
7. `DETERMINISTIC_PRIORITY` 直接选择排序后的首个候选动作。
8. `WEIGHTED_RANDOM` 在排序后的候选集合上归一化采样；未声明 `weight` 时默认 `1.0`，若全部候选权重 `<= 0`，回退到排序后的首个动作并将 fallback reason 写入 `AIDecisionTrace`。

### 4.2 [W4a] `BossEncounter` 结构

建议文件：

```text
core/src/main/kotlin/com/ktome/core/ai/BossEncounter.kt
core/src/main/kotlin/com/ktome/core/ai/BossPhaseDef.kt
game/src/main/resources/data/boss/*.yaml
core/src/test/kotlin/com/ktome/core/ai/BossEncounterTest.kt
```

冻结口径：

1. `BossEncounter` 包含：`id`, `templateId`, `phases[]`。
2. 每个 `BossPhaseDef` 包含：`id`, `hpThreshold`, `hpEnd`, `aiProfileId`, `onEnter[]`（可选）。
3. `phases` 按 `hpThreshold` 降序排列，运行时从上到下匹配第一个满足条件的 phase。
4. `onEnter` 事件类型：`TELEGRAPH`, `CLEAR_STATUSES`, `INVULNERABLE`, `EMIT_EVENT` 等。
5. `onEnter.TELEGRAPH` 只允许引用统一 `telegraphSpecId`，不再内联定义 shape/preview/danger。
6. `templateId` 引用 `MonsterTemplateV2.id`，共享基础 `stats / resistance / combat profile` 数据，不再为 Boss 单独造第二套怪物模板主键。

YAML 示例：

```yaml
boss_encounter:
  id: "molten_giant"
  templateId: "molten_giant_template"
  phases:
    - id: "phase_full"
      hpThreshold: 1.0
      hpEnd: 0.50
      aiProfileId: "molten_giant_phase_full"
    - id: "phase_enraged"
      hpThreshold: 0.50
      hpEnd: 0.0
      aiProfileId: "molten_giant_phase_enraged"
      onEnter:
        - type: "TELEGRAPH"
          telegraphSpecId: "ground_slam_phase_warning"
```

### 4.3 [W4a] `BossPhaseDef` 切换机制

建议文件：

```text
core/src/main/kotlin/com/ktome/core/ai/BossPhaseManager.kt
core/src/test/kotlin/com/ktome/core/ai/BossPhaseTransitionTest.kt
```

冻结口径：

1. 切换条件：`hpThreshold / hpEnd / turnCount / requiredStatus`。
2. 切换时机：默认在回合开始时判定；若遭遇“致死转阶段”例外，必须显式声明。
3. 切换副作用：`invulnerableTurns / clearStatuses / resetAiPhaseState / emitTelegraph`。
4. phase 切换必须写入 `BossTrace`，并能和 telegraph / 视觉提示对齐。

### 4.4 [W4a] 普通怪与 Boss 的统一选择语义

冻结口径：

1. 普通怪 / 精英怪继续沿用补充设计文档中的确定性脚本模型。
2. Boss 可以使用加权随机，但必须通过统一 schema 表达，而不是在实现里特判。
3. `AIDecisionTrace` 必须记录：
   - 候选动作
   - 满足条件的动作集合
   - `selectionPolicy`
   - 排序后的候选顺序
   - 若为加权选择，则记录 `rngRoll`
   - 若触发 fallback，则记录 `reason`

### 4.5 [W4a] `TelegraphSpec` 与 `ThreatRatingResolver`

建议文件：

```text
core/src/main/kotlin/com/ktome/core/ai/TelegraphSpec.kt
core/src/main/kotlin/com/ktome/core/ai/ThreatRatingResolver.kt
core/src/main/kotlin/com/ktome/core/ai/ThreatProfileDef.kt
game/src/main/resources/data/telegraph/threat_profiles/*.yaml
tools/src/main/kotlin/com/ktome/tools/lint/ThreatProfileLint.kt
core/src/test/kotlin/com/ktome/core/ai/TelegraphThresholdTest.kt
```

冻结口径：

1. `TelegraphSpec` 是唯一 telegraph 权威结构。
2. `PR-03` 的 `talent.telegraphRef` 和 `BossEncounter.onEnter.TELEGRAPH` 都只引用该结构。
3. `TelegraphSpec` 至少包含：
   - `shape`
   - `radius / length / angle`
   - `previewTurns`
   - `dangerLevel`
   - `threatProfileId`
   - `counterplayTags`
   - `stages`
4. `dangerLevel` 枚举固定为：
   - `LOW`
   - `MODERATE`
   - `HIGH`
   - `LETHAL`
5. telegraph 阈值不是对“真实玩家当前状态”硬算，而是对统一的 defender baseline 计算。
6. `threatProfileId` 只能引用注册表中的 `ThreatProfileDef`；内容作者不得在 talent / boss YAML 内联一套新的 defender baseline。
7. `ThreatProfileDef` 至少按 `defenderArchetype / levelBand / difficultyId` 分层，并由 `ThreatProfileLint` 校验引用完整性与重复定义。
8. `counterplayTags` 是正式字段，用于表达 `DODGE / INTERRUPT / BLOCK` 等可应对手段。
9. `stages` 作为四段式 telegraph 的预留字段保留；`Phase 3` 默认允许空列表，不要求实现完整 `WARNING -> WINDUP -> IMPACT -> RESIDUAL` 生命周期。

### 4.5.1 `ThreatProfileDef` 注册表

冻结口径：

1. `ThreatProfileDef` 负责定义 telegraph 阈值评估所依赖的标准 defender baseline。
2. `ThreatRatingResolver` 只能消费注册表中的 profile，不允许把 `expectedHp / mitigation` 散落在各处 YAML。
3. 注册表默认按以下维度分层：
   - `defenderArchetype`
   - `levelBand`
   - `difficultyId`
4. 新 profile 必须通过 `ThreatProfileLint` 校验：
   - ID 唯一
   - 引用完整
   - 不与现有 level band / archetype 定义重叠冲突

### 4.6 [W4a] `STEALTH / TAUNT` 与 AI 交互

建议文件：

```text
core/src/main/kotlin/com/ktome/core/ai/StealthTauntHandler.kt
core/src/test/kotlin/com/ktome/core/ai/StealthAiInteractionTest.kt
core/src/test/kotlin/com/ktome/core/ai/TauntAiInteractionTest.kt
```

冻结口径：

1. 目标进入 `STEALTH` 后，AI 当前目标引用立即失效。
2. 若 `useLastKnownPosition = true`，AI 移动到目标最后已知位置；到达后仍未重新发现目标，回退到 `defaultBehavior`。
3. Boss 遇到 `STEALTH` 目标时，不因此额外触发 phase 切换；HP 驱动的 phase 切换条件仍正常判定。Boss 只在当前 phase 内执行无目标 fallback 或范围扫描行为，不得作弊式锁定隐身目标。
4. AI 被 `TAUNT` 命中后，持续期间必须强制把攻击目标设为嘲讽源；若目标不在攻击范围内，优先移动靠近。
5. `TAUNT` 结束后，AI 恢复原有目标选择逻辑。

### 4.7 [W4a] `BossTrace / AIDecisionTrace`

建议文件：

```text
core/src/main/kotlin/com/ktome/core/ai/AIDecisionTrace.kt
core/src/main/kotlin/com/ktome/core/ai/BossTrace.kt
core/src/test/kotlin/com/ktome/core/ai/BossTraceExportTest.kt
```

冻结口径：

1. `AIDecisionTrace` 记录每次 AI 决策：
   - `evaluatedActions[]`
   - `selectedAction`
   - `selectionPolicy`
   - `rngRoll`
   - `reason`
   - `turnId`
2. `BossTrace` 记录 phase 切换：
   - `fromPhase`
   - `toPhase`
   - `trigger`
   - `turnId`
   - `sideEffects[]`
3. 两者都必须可导出为 JSON 用于调试和回归。
4. `bossHarness` 在回放时加载 trace 并验证一致性。

### 4.8 [W4b] Telegraph Renderer

建议文件：

```text
client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt
client/src/main/kotlin/com/ktome/client/telegraph/TelegraphStyle.kt
```

冻结口径：

1. 支持形状：`CIRCLE`, `LINE`, `CONE`；`RADIUS_SELF / RADIUS_TARGET` 统一映射到 `CIRCLE`，`CROSS` 预览由两条正交 `LINE` 叠加渲染，`Phase 3` 不要求单独 `CROSS` shape。
2. `dangerLevel` 映射到颜色编码：
   - `LOW` = 蓝
   - `MODERATE` = 黄
   - `HIGH` = 红
   - `LETHAL` = 紫
3. 显示持续回合倒计时。
4. telegraph 消失后的格子标记在下一帧清除。
5. 消费 `RenderSnapshot.overlays` 中的 telegraph 数据。

## 5. 推荐改动面

### 5.1 `core`

1. `ai` 包扩展（`AIProfile` / `AIAction` / `AICondition` / `BossEncounter` / `BossPhaseDef` / `BossPhaseManager` / `TelegraphSpec` / `ThreatRatingResolver` / `StealthTauntHandler` / `AIDecisionTrace` / `BossTrace`）。

### 5.2 `game`

1. `ai/*.yaml` 新建（`AIProfile` 数据）。
2. `boss/*.yaml` 扩展（`BossEncounter` 两层结构）。
3. 所有 telegraph 数据都改成引用统一 `telegraphSpecId`。

### 5.3 `client`

1. `telegraph` 包新建（`TelegraphRenderer` / `TelegraphStyle`）。

### 5.4 `tools`

1. `BossHarness` 新建。
2. `bossHarness` Gradle task。

## 6. 测试与自证

### 6.1 必测类

1. `AIProfileDslTest`
2. `BossEncounterTest`
3. `BossPhaseTransitionTest`
4. `TelegraphThresholdTest`
5. `StealthAiInteractionTest`
6. `TauntAiInteractionTest`
7. `BossTraceExportTest`
8. `BossHarnessTest`
9. `ThreatProfileRegistryTest`

### 6.2 必测行为

1. `AIProfile` YAML 可正确解析为运行时对象。
2. `AICondition` 能正确解析 `AND / OR / NOT` 组合器。
3. `selectionPolicy` 能正确驱动确定性选择与加权随机选择。
4. Boss phase 在 `hpThreshold` 触发时正确切换。
5. `onEnter` 的 `telegraphSpecId` 能正确引用统一 telegraph 规格。
6. telegraph 在 `>= 30% / >= 50%` defender baseline 阈值时正确生成。
7. `STEALTH` 使 AI 失去目标引用并移动到最后已知位置。
8. `TAUNT` 强制 AI 攻击目标为嘲讽源。
9. `BossTrace` 与 `AIDecisionTrace` 均可导出 JSON，且 `WEIGHTED_RANDOM` 记录 `rngRoll`。
10. 相同 `orderKey` 的候选动作按 `actionId asc` 稳定 tie-break；`weight` 缺失与全 0 权重 fallback 行为稳定。
11. `threatProfileId` 能通过注册表解析，并由 `ThreatProfileLint` 拦截非法 profile。
12. `counterplayTags` 至少能表达一种有效反制手段，空 `stages` 不影响 `Phase 3` 的 telegraph 最小模型。
13. `bossHarness` 至少 2 个 Boss 稳定通过。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.ai.*"
./gradlew bossHarness
./gradlew test
```

### 6.4 白盒验证

1. 固定至少 2 个不同类型的 Boss encounter seed。
2. 检查 telegraph 出现时机、phase 切换门槛、trace 与视觉提示一致。
3. 用 `Vanguard` 或 `Arcanist` 打一场 Boss 战，确认 telegraph 可读。
4. 用 `Rogue` 触发 `STEALTH`，确认 AI 停止直接追踪并移动到最后已知位置。
5. 用 `Vanguard` 对精英使用 `TAUNT`，确认精英在 `TAUNT` 期间只攻击嘲讽源。

## 7. 出口门禁

1. `AIProfile` DSL + `BossEncounter` 两层结构冻结且解析测试绿。
2. `selectionPolicy` 与 tie-break / fallback 规则进入正式 schema。
3. `TelegraphSpec` 成为唯一 telegraph 权威。
4. `BossPhaseDef` 的结构化切换条件全部可配置且测试绿。
5. telegraph 阈值合同与 `ThreatProfileDef` 注册表在至少 2 个 Boss 上验证通过。
6. `BossTrace / AIDecisionTrace` 可导出且可回放。
7. `bossHarness` 的基线覆盖至少 2 个 Boss；完整 3 Boss roster 覆盖升级由 `PR-06` 承接。
