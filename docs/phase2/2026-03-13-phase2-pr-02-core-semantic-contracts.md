> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-01-serialization-and-version-discipline.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 2 - PR-02 Core Semantic Contracts

**阶段**: `Phase 2 / P2-W2`  
**优先级**: `P0`  
**前置条件**: `P2-W1` 完成  
**对应问题**: 当前 `100` 能量制、硬编码状态/资源、薄弱事件模型和过厚的 `FoundationGameSession` 仍然让规则核心处于临时态，无法稳定支撑后续 UI、i18n 和内容扩展。

---

## 1. 阶段目标

建立 Phase 2 的核心语义合同，覆盖回合、资源、伤害通道、状态骨架和事件总线。

完成标准：

1. `1000` 能量制正式取代 `100` 能量制。
2. `DamageType`、`ResourcePool`、基础状态扩展进入主路径。
3. `GameEvent / LogTokenEvent / callback registry` 可稳定驱动规则事件。
4. `FoundationGameSession` 完成首轮拆厚。
5. 旧 Phase 1 近战、4 技能、怪物仍能在新语义上运行。
6. 为后续职业、技能、怪物、地图正式内容对象预埋稳定 id 和注册表语义。

## 2. 当前问题

1. `100` 能量制精度不够，后续速度、状态 tick、技能成本都不够好表达。
2. `Stamina` 仍是特判资源，没有统一资源池。
3. `GameEvent` 过薄，缺来源、顺序、阶段和 payload。
4. `FoundationGameSession` 同时承担过多职责，后续极易继续膨胀。
5. 如果现在不冻结资源、状态、事件和内容 id 词汇，后面的职业、技能、怪物和 zone 会很快命名漂移。

### 2.1 本 PR 必须冻结的口径

1. 所有行动成本统一切到 `1000` 口径。
2. `DamageType` 先落枚举和数据通路，不在本 PR 冻结最终战斗公式。
3. 状态先完成注册表/实例骨架，不在本 PR 完成全部状态族。
4. 事件先确保“规则可追踪”，不在本 PR 做全部客户端消费。
5. 必须为 `profession / talent / monster / zone` 等正式对象预埋稳定 id 引用语义，但不在本 PR 完成全部内容。

## 3. 范围与非目标

### 3.1 范围

1. turn/economy 迁移
2. `DamageType` 主链打通
3. `ResourcePool` 与现有 `Stamina` 迁移
4. 基础状态模型
5. `GameEvent`、`LogTokenEvent`、callback registry
6. session 拆厚第一轮
7. 初始注册表与固定词汇预埋

### 3.2 非目标

1. 不在本 PR 冻结命中/暴击/PowerSave 最终公式。
2. 不在本 PR 实现完整状态生命周期大扩张。
3. 不在本 PR 完成 client Tile 路径。
4. 不在本 PR 直接生产正式美术和音频文件，但必须把它们依赖的语义 key 和事件家族打稳。

## 4. 技术方案

### 4.1 1000 能量调度迁移

建议文件：

```text
core/src/main/kotlin/com/ktome/core/turn/*
core/src/test/kotlin/com/ktome/core/turn/*
```

冻结口径：

1. 行动资格只由 `energy >= 1000` 决定。
2. 所有行动成本必须显式配置或集中常量化。
3. 同一实体在相同输入下行动顺序稳定。
4. `MOVE / BASIC_ATTACK / TALENT / INTERACT / WAIT` 的默认成本必须在本 PR 固定第一版常量，不允许继续散落在调用点。

首轮固定参数：

| 项 | 固定值 | 说明 |
| --- | --- | --- |
| 行动阈值 | `1000` | 取代 Phase 1 的 `100` |
| 标准速度 | `1000` | 旧 `speed` 统一乘 `10` 迁移 |
| 快速动作 | `750` | 轻量 instant/quick action 的第一版口径 |
| 标准动作 | `1000` | `MOVE / BASIC_ATTACK / INTERACT / WAIT` 默认落点 |
| 慢速动作 | `1250` | 重击、长施法或重交互的第一版口径 |
| 排序键 | `energy desc -> entityId asc` | 确保同能量下的稳定性 |

### 4.2 ResourcePool 落地

建议文件：

```text
core/src/main/kotlin/com/ktome/core/resource/*
core/src/test/kotlin/com/ktome/core/resource/*
```

冻结口径：

1. `Stamina` 迁入 `ResourcePoolState(STAMINA)`。
2. `HEALTH` 继续作为独立生命系统处理，不并入 `ResourceType`。
3. 先启用 `STAMINA / MANA / POSITIVE_ENERGY / ENERGY` 四类主资源结构。
4. `HATE / EQUILIBRIUM` 先占位 enum，不在本 PR 接入玩法主线。
5. 资源变化必须可事件化。

首轮必须冻结的回复策略类型：

1. `PerTurn`
2. `OnKill`
3. `OnDamageTaken`
4. `OnHit`
5. `DecayPerTurn`
6. `Composite`
7. `None`

四基础职业的默认资源合同：

| profession | 主资源 | 初始上限 | 回复策略 |
| --- | --- | --- | --- |
| `vanguard` | `STAMINA` | `40 + WIL * 5` | `PerTurn(3)` |
| `arcanist` | `MANA` | `50 + WIL * 6` | `PerTurn(2)` |
| `rogue` | `ENERGY` | `100` | `Composite([PerTurn(5), OnHit(8)])` |
| `templar` | `POSITIVE_ENERGY` | `100` | `Composite([OnDamageTaken(0.15), OnHit(3), DecayPerTurn(5)])` |

### 4.3 DamageType 与基础状态骨架

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/DamageType.kt
core/src/main/kotlin/com/ktome/core/status/*
```

冻结口径：

1. `DamageType` 统一冻结为 `PHYSICAL / FIRE / COLD / LIGHTNING / HOLY / SHADOW` 六通道。
2. 现有近战和旧技能先全部走统一 `DamageType` 主链，不再保留“无通道伤害”。
3. Phase 2 的抗性/穿透先用简化模型模板：`effectiveResistance = clamp(targetResistance - penetration, -25, 75)`，不提前引入 Phase 3 的收益递减体系。
4. `HOLY` 对亡灵/恶魔在 Phase 2 就允许挂接额外增伤语义，避免圣堂武士切片退化成纯换色物理伤害。
5. `StatusEffectDef/StatusInstance` 进入主链。
6. 现有 `STUN / ARMOR_BREAK / WAR_CRY_BUFF / WAR_CRY_DEBUFF` 必须迁入新模型；其中 Phase 1 的 `STUNNED` 在 Phase 2 统一并名为 `STUN`。
7. `WAR_CRY_BUFF / WAR_CRY_DEBUFF` 在 Phase 2 必须迁成通用 buff/debuff + `statModifiers`，不要再保留专用临时枚举。
8. 首批固定状态 seed 至少覆盖：
   - `STUN`
   - `ARMOR_BREAK`
   - `GUARD`
   - `BLEED`
   - `BURN`
   - `MARKED`
   - `ROOT`
   - `SILENCE`
   - `BLIND`
   - `CONFUSE`
   - `SLOW`
   - `FEAR`
   - `KNOCKBACK`
   - `POISON`
   - `FREEZE`
   - `SHIELD`
   - `REGEN`
   - `HASTE`
9. 最小叠加/互斥规则也必须在本 PR 定第一版：
   - `STUN / SLOW / FREEZE / SILENCE`：不叠加，只刷新持续时间
   - `BLEED / BURN / POISON`：独立叠层
   - `ARMOR_BREAK`：上限 `3` 层
   - `SHIELD / REGEN`：取较强值
10. 虽然公式模板允许 `-25` 下限，但 Phase 2 实际运行时默认所有穿透值为 `0`、怪物元素抗性默认为 `0`；因此该模板在 Phase 2 不依赖负抗性增伤。`HOLY` 对亡灵/恶魔的 `+50%` 额外增伤走标签检查，而不是负抗性路径。

### 4.4 Combat DTO 壳与 Trace Schema

虽然 Phase 2 不冻结最终战斗公式，但必须先冻结统一战斗入口与 DTO。这里的口径与补充设计文档一致：`DamageInstance` 是单条已解析伤害载荷；`DamageRequest / DamagePacket / DamageOutcome` 是其前后包装层。

```text
DamageRequest -> DamagePacket -> DamageOutcome -> CombatTrace
```

本 PR 至少需要建立：

1. `DamageRequest`
2. `DamagePacket`（内部承载 `DamageInstance`）
3. `DamageOutcome`
4. `CombatTrace`
5. `CombatTraceStage`
6. `PowerType`
7. `SaveDimension`

冻结口径：

1. 现有近战与技能入口都必须先汇合到 `DamageRequest`。
2. `CombatTrace` 先冻结 schema 壳和阶段名，不提前冻结 Phase 3 金样本。
3. `PowerType/SaveDimension` 必须先进入运行时类型系统，避免后续技能和状态又发明平行通道。

### 4.5 TalentDef V2 与 Executor Registry 运行时骨架

本 PR 必须先建立天赋运行时骨架：

1. `TalentDef V2`
2. `TalentExecutorRegistry`
3. `TypedEffectExecutor`
4. `TalentTargeting`
5. `ActionCost`

冻结口径：

1. 现有 4 个战士技能必须迁入 V2 runtime 骨架。
2. `TalentResolver` 后续只负责解析和 dispatch，不再继续扩硬编码分支。
3. Phase 2 允许只支持最小 effect op 子集，但接口和扩展口必须先固定。

### 4.6 Event / LogToken / Callback

建议文件：

```text
core/src/main/kotlin/com/ktome/core/event/*
core/src/main/kotlin/com/ktome/core/log/*
```

冻结口径：

1. 事件至少包含：
   - type
   - turnId/actionId
   - source/target
   - phase
   - payload
2. `LogTokenEvent` 不直接拼 UI 文本。
3. callback registry 必须支持基础 on-apply/on-hit/on-death 级别接线。
4. 事件家族必须从第一天支持后续音频/视觉消费：
   - `ui`
   - `movement`
   - `melee`
   - `spell`
   - `status`
   - `monster`
   - `interactable`
5. 资源变化也必须进入正式事件主线，至少包含：
   - `ResourceSpent`
   - `ResourceRestored`
   - `ResourceDepleted`

### 4.7 Session 拆厚第一轮

建议拆分：

1. `TurnSystem`
2. `CombatSystem`
3. `TalentSystem`
4. `InventorySystem`
5. `ProgressionSystem`
6. `GameSession`

冻结口径：

1. `FoundationGameSession` / `GameSession` 仍可作为编排入口。
2. `SaveFacade`、`EventCollector` 若存在，只能作为辅助适配层，不得替代上述权威拆分边界。
3. 不再允许会话入口独占所有子逻辑。

### 4.8 Phase 2 初始固定词汇与注册表

本 PR 必须先冻结以下 id 命名空间，避免后续内容创建跑偏：

1. profession:
   - `vanguard`
   - `arcanist`
   - `rogue`
   - `templar`
2. zone:
   - `shattered_outpost`
   - `greenwood_fringe`
   - `deep_iron_pit`
   - `grey_gate_depths`
3. damage:
   - `PHYSICAL`
   - `FIRE`
   - `COLD`
   - `LIGHTNING`
   - `HOLY`
   - `SHADOW`
4. power:
   - `PHYSICAL`
   - `SPELL`
   - `MENTAL`
5. status seed:
   - `STUN`
   - `ARMOR_BREAK`
   - `GUARD`
   - `BLEED`
   - `BURN`
   - `MARKED`

约束：

1. 后续 PR 只能在这些命名空间下新增对象，不允许再发明平行命名风格。
2. 所有正式内容对象都必须用稳定 id 引用彼此，不允许靠裸字符串名字匹配。

## 5. 推荐改动面

### 5.1 `core`

1. `turn`
2. `resource`
3. `combat`
4. `status`
5. `event`
6. `log`
7. `talent`

### 5.2 `game`

1. session 拆厚
2. 旧技能/旧怪物接线迁移
3. profession/zone id 预埋

## 6. 测试与自证

### 6.1 必测类

1. `TurnScheduler1000EnergyTest`
2. `ResourcePoolTest`
3. `StatusRegistryTest`
4. `GameEventContractTest`
5. `FoundationGameSessionMigrationTest`
6. `RegistryIdContractTest`
7. `CombatTraceSchemaTest`
8. `TalentExecutorRegistryTest`

### 6.2 必测行为

1. `100` 到 `1000` 能量迁移后行动顺序合理且稳定。
2. 旧 `Stamina` 消耗行为在新资源池上保持等价。
3. 现有 4 个技能与现有怪物都能在新事件模型上跑通。
4. log token 事件能独立于 UI 输出。
5. profession/zone/status 等固定 id 可被稳定引用。
6. 现有 4 个技能必须经过统一 talent runtime 和 combat entry。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.turn.*"
./gradlew :core:test --tests "com.ktome.core.resource.*"
./gradlew :core:test --tests "com.ktome.core.event.*"
./gradlew :game:test
```

### 6.4 白盒验证

1. 新开一局，观察玩家与怪物的行动节奏。
2. 使用现有 4 技能，确认：
   - 资源消耗正确
   - 状态施加仍成立
   - 日志仍能显示
3. 人工比对迁移前后默认战斗手感是否出现灾难性偏移。

## 7. 出口门禁

1. `1000` 能量制进入正式主路径。
2. `ResourcePool`、基础状态与事件总线可稳定运行。
3. 现有近战、4 技能、怪物都能跑在新语义上。
4. `FoundationGameSession` 首轮拆厚完成。
5. Phase 2 首批内容对象命名空间冻结完成。
6. `DamageRequest -> DamagePacket -> DamageOutcome -> CombatTrace` 与 `TalentDef V2` runtime 骨架成立。

## 8. 风险与止损

1. 如果资源池和旧逻辑双轨并存，必须先清掉旧特判再继续。
2. 如果 session 继续加职责，必须停下来先拆。
3. 如果事件 payload 过度松散，必须尽快类型化，不允许全靠 `Map<String, Any>`.

## 9. 当前状态

1. 本文是 `P2-W2` 的 PR 级开发文档。
2. 该 PR 完成后，Phase 2 的核心规则语义才算真正稳定。
