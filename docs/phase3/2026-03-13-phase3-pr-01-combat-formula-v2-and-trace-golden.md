> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-01 战斗公式 V2 与 Resolution Trace / Golden Corpus

**阶段**: `Phase 3 / P3-W1`  
**优先级**: `P0`  
**前置条件**: `Phase 2` 出口全部满足  
**对应问题**: Phase 2 战斗仍使用线性命中和简化减免模型，golden 只有单层阶段标记，没有规则版本和 corpus 分层，无法稳定承接 `P3-W2 ~ P3-W6` 的状态、Boss 与长局扩展。

---

## 1. 阶段目标

完成战斗公式 V2 全面升级，冻结 `CombatPipeline` 的公式级执行顺序，并建立**分层 golden + 版本化 trace envelope** 的第一版基线。

完成标准：

1. 命中公式从线性切换到 Sigmoid 模型。
2. 暴击系统冻结（基础暴击率 5%、暴击率上限 50%、基础暴击倍率 1.5、`critResistance` 直接抵扣有效暴击率）。
3. 物理减免冻结为 `armor / (armor + 100)`。
4. 元素抗性冻结为 `clamp(resistance - penetration, -25, 75)`。
5. `HOLY` 的正式伤害口径冻结为“标签增伤路径”，不把亡灵/恶魔的默认负神圣抗性作为通用基线。
6. `Power/Save` 公式与状态进入管线的 `ApplicationPolicy` 基线冻结。
7. `CombatPipeline` 固定为 12 步有序管线。
8. `W1` 只冻结 `formula corpus` 与 `CombatResolutionTrace`，不提前冻结 encounter 级追踪。
9. golden 文件从 `Phase 3` 起必须携带 `phaseId / rulesetVersion / traceSchemaVersion / corpusId`。

## 2. 当前问题

1. Phase 2 的线性命中模型在高/低属性区间辨识度不足，大幅偏差时输出不合理。
2. 无暴击抗性概念，无法制衡暴击堆叠。
3. 无 `Power/Save` 对抗体系，状态施加判定过于简单。
4. 无收益递减，二级属性在后期可能无限膨胀。
5. `CombatTrace` 只有“阶段标记”而没有规则版本和 corpus 分层，后续 `W2/W4/W6` 一接入就会反复重录。
6. `HOLY` 在 Phase 3 文档链里存在“标签增伤”和“默认负神圣抗性”两套口径。
7. 效果进入战斗系统的方式没有结构化 `ApplicationPolicy`，后续技能容易各自决定“先命中再豁免”还是“只走豁免”。

### 2.1 本 PR 必须冻结的口径

1. Phase 3 正式公式取代 Phase 2 的线性模型，这是预期内破坏性变更。
2. `HOLY` 对亡灵/恶魔的正式默认口径是标签乘算，不把“亡灵/恶魔默认 `holyResistance = -25`”作为通用基线。
3. `W1` 只冻结 `formula corpus` 与 `CombatResolutionTrace`，`status / integration / long-run` corpus 留给后续工作包。
4. golden 文件必须携带：
   - `phaseId`
   - `rulesetVersion`
   - `traceSchemaVersion`
   - `corpusId`
5. `CombatPipeline` 12 步顺序冻结，child trace、callback 优先级和 miss/cleanse/elemental interaction 的挂点必须可追踪。
6. 收益递减只作用于二级属性，不作用于 `STR/DEX/CON/WIL`。
7. 任何可施加效果都必须声明 `ApplicationPolicy`，不允许在技能私有逻辑里隐式决定进入路径。

## 3. 范围与非目标

### 3.1 范围

1. `core.combat` 公式全面升级（命中 Sigmoid、暴击、护甲、元素减免、`Power/Save`）。
2. 收益递减模型。
3. `CombatPipeline` 12 步有序管线。
4. `ApplicationPolicy` 第一版基线。
5. `CombatResolutionTrace` 与 `TraceEnvelope`。
6. `formula corpus` 的 golden harness。
7. Phase 2 公式相关 golden 的一次性重录。

### 3.2 非目标

1. 不在本 PR 处理状态生命周期大扩张（`P3-W2`）。
2. 不在本 PR 处理天赋树 schema V2（`P3-W3`）。
3. 不在本 PR 处理 AI DSL、Boss phase 或 encounter 级追踪（`P3-W4`）。
4. 不在本 PR 冻结完整元素交互矩阵；首批元素交互只启用 `FIRE / COLD / HOLY / SHADOW` 的核心规则。
5. 不在本 PR 冻结 `status corpus / integration corpus / long-run corpus`。

## 4. 技术方案

### 4.1 命中公式升级

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/HitFormula.kt
core/src/test/kotlin/com/ktome/core/combat/HitFormulaSigmoidTest.kt
```

冻结口径：

1. `hitChance = clamp(0.05 + 0.90 * sigmoid(0.04 * (accuracy - evasion + 10)), 0.05, 0.95)`
2. 与补充设计文档 §3.2.1 的 `m = -10` 写法是同一公式的等价展开。
3. 首轮冻结参数以本节参数表为权威；补充设计文档只保留同一公式的标准形式与推导说明。
4. `evasion` 自 `Phase 3` 起恢复为命中系统的独立对抗属性；详设文档中 `defense` 的过渡性别名说明不再作为实现口径。

首轮固定参数表：

| 参数 | 值 | 说明 |
|------|-----|------|
| 基础命中偏移 | 0.05 | 最低保底 |
| 命中范围 | 0.90 | Sigmoid 的映射区间 |
| Sigmoid 斜率 k | 0.04 | 控制过渡陡峭度 |
| 中心偏移 m | 10 | 本表 `m = 10` 对应补充设计文档标准形式里的 `m = -10`，两者是同一公式的不同展开 |
| 下限 | 0.05 | 最低命中率 5% |
| 上限 | 0.95 | 最高命中率 95% |

### 4.2 暴击系统

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/CritFormula.kt
core/src/test/kotlin/com/ktome/core/combat/CritFormulaTest.kt
```

冻结口径：

1. 基础暴击率 5%。
2. 暴击率上限 50%。
3. 基础暴击倍率 1.5x。
4. `critResistance` 直接抵扣有效暴击率：
   - `effectiveCritRate = clamp(baseCritRate + critBonus - critResistance, 0, 0.50)`
5. 暴击倍率可由装备/天赋增幅，但本 PR 不冻结最终上限。

### 4.3 护甲与元素减免

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/DamageFormula.kt
core/src/test/kotlin/com/ktome/core/combat/ArmorReductionTest.kt
core/src/test/kotlin/com/ktome/core/combat/ElementalResistanceTest.kt
```

冻结口径：

1. 物理减免：`reduction = armor / (armor + 100)`。
2. 元素抗性：`effectiveResistance = clamp(resistance - penetration, -25, 75)`。
3. 负抗性（穿透超过抗性）允许增伤，下限 -25%。
4. 元素抗性上限 75%。
5. `HOLY` 对亡灵/恶魔的额外增伤继续走标签检查路径，不走“默认负神圣抗性”路径。
6. 若未来存在极少数显式负 `holyResistance` 的怪物或 affix，必须在模板层单独声明，不作为种族/标签的默认基线。

### 4.4 `Power/Save` 公式

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/PowerSaveFormula.kt
core/src/test/kotlin/com/ktome/core/combat/PowerSaveFormulaTest.kt
```

冻结口径：

1. `applyChance = clamp(0.10 + 0.80 * sigmoid(0.05 * (power - save)), 0.10, 0.90)`。
2. 保底施加率 10%，上限 90%。
3. 与补充设计文档 §3.4 保持一致。
4. `PowerType / SaveDimension` 已在运行时类型系统存在，本 PR 不再新增对抗维度。
5. `power / save` 的属性来源公式以补充设计文档 §3.4.2 为准；golden test 必须固定 `STR / DEX / CON / WIL / level` 组合，禁止只测最终黑箱输入。

### 4.4.1 回合时序与 `CombatPipeline` 的嵌套关系

冻结口径：

1. `CombatPipeline` 的 12 步不是独立于回合框架存在；它嵌套在详设文档 §6.3 的 9 步回合时序中。
2. 嵌套关系固定为：
   - `TurnStart`：冷却 tick、start-of-turn status、资源回复/衰减
   - `ActionValidate`：活体/资源/控制状态/目标/LoS 检查
   - `ActionCommit`：扣资源、上冷却、写入 `actionId`
   - `MovementPhase`
   - `HitPhase ~ DeathPhase`：进入 `CombatPipeline` 12 步
   - `TurnEnd`：end-of-turn status、过期清理、event flush
3. `W1` 只冻结战斗结算管线本体，但不得与回合框架脱钩成第二套时序权威。

### 4.5 `ApplicationPolicy`

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/ApplicationPolicy.kt
core/src/test/kotlin/com/ktome/core/combat/ApplicationPolicyTest.kt
```

冻结口径：

1. 所有会施加状态、debuff、mark、cleanse、tag interaction 的 ability/effect 都必须显式声明进入战斗系统的策略。
2. 第一版至少覆盖：
   - `SELF_AUTO`
   - `HOSTILE_HIT_THEN_SAVE`
   - `HOSTILE_SAVE_ONLY`
   - `TAG_AUTO`
   - `INSTANT_ACTION`
3. `INSTANT_ACTION`：绕过命中与豁免，声明后立即生效；用于地形效果、环境伤害、Boss phase 切换副作用等非“攻击者命中目标”语义。
4. `SELF_AUTO` 只用于施法者自身获得的即时效果，不等同于无来源的环境或世界副作用。
5. `ApplicationPolicy` 是 `W1` 先冻结的入口合同；`W2/W3/W5` 只消费该合同，不再各自定义第二套入口语义。

### 4.6 收益递减

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/DiminishingReturns.kt
core/src/test/kotlin/com/ktome/core/combat/DiminishingReturnsTest.kt
```

冻结口径：

1. 仅作用于二级属性，不作用于 `STR/DEX/CON/WIL`。
2. 公式：`effectiveValue = rawValue * C / (rawValue + C)`。
3. `accuracy` 与 `armorPenetration` 在 `Phase 3` 不进入收益递减表，保持线性语义；若后续要加 DR，必须作为新版本公式变更显式冻结。

首批常量表：

| 属性 | C 值 | 说明 |
|------|------|------|
| `evasion` | 150 | 闪避 |
| `critRating` | 200 | 暴击等级 |
| `castSpeed` | 100 | 施法速度 |
| `hpRegen` | 80 | 生命回复 |

### 4.7 `CombatPipeline` 12 步

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt
core/src/test/kotlin/com/ktome/core/combat/CombatPipelineTest.kt
```

冻结口径：

1. 完整 12 步定义见补充设计文档 §3.2.3；Phase 3 冻结实现必须保持该顺序与挂点。
2. child trace、callback 优先级和 miss/cleanse/elemental interaction 的挂点必须保持可追踪。
3. 所有现有近战与技能入口都必须汇合到统一管线：
   - `DamageRequest`
   - `DamagePacket`
   - `DamageOutcome`
   - `CombatResolutionTrace`
4. 回调控制流合同以补充设计文档 §3.6.3 为准：
   - `priority` 数值越小越先执行
   - 同优先级按 `entityId asc`
   - `CANCEL` 只中止当前步骤后续回调，不跳过整条管线
   - `ABSORB` 才完全终止当前管线

### 4.8 `CombatResolutionTrace` 与 `TraceEnvelope`

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/CombatResolutionTrace.kt
core/src/main/kotlin/com/ktome/core/combat/TraceEnvelope.kt
tools/src/main/kotlin/com/ktome/tools/golden/CombatTraceGolden.kt
core/src/test/kotlin/com/ktome/core/combat/CombatTraceGoldenTest.kt
```

冻结口径：

1. `W1` 只冻结**公式级** resolution trace，不提前把 Boss、long-run、world progress 追踪塞进同一对象。
2. `TraceEnvelope` 至少包含：
   - `phaseId`
   - `rulesetVersion`
   - `traceSchemaVersion`
   - `corpusId`
3. `corpusId` 在 `W1` 固定为 `FORMULA`。
4. `CombatResolutionTrace` 必须覆盖的 golden 场景：
   - 普攻
   - 暴击
   - 元素减伤
   - 护盾吸收
   - 状态施加成功/失败
   - 元素交互触发
5. encounter 级的 `BossTrace / AIDecisionTrace / LongRunTrace` 不属于本 PR 的冻结对象。

### 4.9 Golden 分层与重录策略

冻结口径：

1. golden 从 Phase 3 起按 corpus 分层：
   - `FORMULA`
   - `STATUS`
   - `INTEGRATION`
   - `LONG_RUN`
2. `W1` 只重录 `FORMULA` corpus。
3. `W2` 接管 `STATUS` corpus。
4. `W5` 后再冻结 `INTEGRATION` corpus。
5. `W6` 单独维护 `LONG_RUN` corpus。
6. `P2toP3FormulaComparisonTest` 必须按指标类型拆分阈值，不再用单一 `±30%` 吃掉全部比较：
   - 伤害类：相对变化默认允许 `±30%`
   - 命中率类：绝对变化默认允许 `±10` percentage points
   - 状态施加率类：绝对变化默认允许 `±10` percentage points
7. `P2toP3FormulaComparisonTest` 对任一超出 `±5%` 的场景，至少输出一条变化原因标签：
   - `SIGMOID_UPGRADE`
   - `CRIT_RESISTANCE_ADDED`
   - `POWER_SAVE_MODEL`
   - `RESISTANCE_CLAMP_UPDATE`

## 5. 推荐改动面

### 5.1 `core`

1. combat 公式全面升级（`HitFormula` / `CritFormula` / `DamageFormula` / `PowerSaveFormula` / `DiminishingReturns`）。
2. `ApplicationPolicy` 新建。
3. `CombatPipeline` 12 步实现。
4. `CombatResolutionTrace` 与 `TraceEnvelope` 新建或扩展。

### 5.2 `tools`

1. `combatTraceGolden` Gradle task。
2. golden 文件管理（版本字段校验、按 corpus 组织、重录脚本）。

### 5.3 `game`

1. 若 session 层仍直接引用旧 `DamageResult`，需迁移到新管线。
2. 任何施加效果的 schema 必须显式写出 `ApplicationPolicy`。

## 6. 测试与自证

### 6.1 必测类

1. `HitFormulaSigmoidTest`
2. `CritFormulaTest`
3. `ArmorReductionTest`
4. `ElementalResistanceTest`
5. `PowerSaveFormulaTest`
6. `ApplicationPolicyTest`
7. `DiminishingReturnsTest`
8. `CombatPipelineTest`
9. `CombatTraceGoldenTest`
10. `P2toP3FormulaComparisonTest`

### 6.2 必测行为

1. Sigmoid 命中在边界值（`accuracy >> evasion / accuracy << evasion / accuracy ≈ evasion`）输出正确。
2. 暴击率被 `critResistance` 抵扣后不超过 50% 上限。
3. 护甲减免在 `armor = 0` 和 `armor = 极大值` 时输出合理。
4. 元素抗性在穿透超过抗性时输出负值增伤，不低于 -25%。
5. `HOLY` 对亡灵/恶魔的额外增伤只来自标签路径，不来自默认负神圣抗性。
6. `ApplicationPolicy` 对 `HOSTILE_HIT_THEN_SAVE` 与 `HOSTILE_SAVE_ONLY` 两条路径区分清楚。
7. `CombatPipeline` 12 步在固定输入下顺序稳定且 trace 可追踪。
8. `FORMULA` corpus 的 golden 在固定 seed 下可重现。
9. `P2toP3FormulaComparisonTest` 按三类阈值验证：
   - 伤害类 `±30%`
   - 命中率类 `±10` percentage points
   - 状态施加率类 `±10` percentage points
10. `P2toP3FormulaComparisonTest` 对显著差异自动输出原因标签，方便平衡定位。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.combat.*"
./gradlew combatTraceGolden
./gradlew test
```

## 7. 出口门禁

1. Sigmoid 命中、暴击、护甲、元素减免、`Power/Save`、收益递减测试全绿。
2. `HOLY` 的正式口径收口为标签增伤路径。
3. `ApplicationPolicy` 第一版进入运行时合同并有自动化测试。
4. `CombatResolutionTrace` 与 `TraceEnvelope` 冻结完成。
5. `FORMULA` corpus 的 golden 可稳定重现。
6. `W1` 不再要求提前冻结 `STATUS / INTEGRATION / LONG_RUN` corpus。
