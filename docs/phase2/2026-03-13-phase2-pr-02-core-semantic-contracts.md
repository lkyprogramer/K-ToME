> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-01-serialization-and-version-discipline.md`
> `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`

# Phase 2 - PR-02 Core Semantic Contracts

**阶段**: `Phase 2 / P2-W2`  
**优先级**: `P0`  
**前置条件**: `PR-01` 完成  
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

### 4.2 ResourcePool 落地

建议文件：

```text
core/src/main/kotlin/com/ktome/core/resource/*
core/src/test/kotlin/com/ktome/core/resource/*
```

冻结口径：

1. `Stamina` 迁入 `ResourcePoolState(STAMINA)`。
2. 先启用 `HEALTH / STAMINA / MANA / POSITIVE` 的结构。
3. 资源变化必须可事件化。
4. `RAGE / MOMENTUM / ARCANE_CHARGE / SHADOW / FOCUS` 先占位 enum，不在本 PR 接入玩法主线。

### 4.3 DamageType 与基础状态骨架

建议文件：

```text
core/src/main/kotlin/com/ktome/core/combat/DamageType.kt
core/src/main/kotlin/com/ktome/core/status/*
```

冻结口径：

1. 现有攻击先全部走 `PHYSICAL`。
2. `StatusEffectDef/StatusInstance` 进入主链。
3. 现有 `STUNNED / ARMOR_BREAK / WAR_CRY_BUFF / WAR_CRY_DEBUFF` 必须迁入新模型。
4. `STUN / ARMOR_BREAK / GUARD / BLEED / BURN / MARKED` 必须作为首批固定 id 进入注册表，后续职业和怪物围绕这些 seed 扩展。

### 4.4 Combat DTO 壳与 Trace Schema

虽然 Phase 2 不冻结最终战斗公式，但必须先冻结统一战斗入口与 DTO：

```text
DamageRequest -> DamagePacket -> DamageOutcome -> CombatTrace
```

本 PR 至少需要建立：

1. `DamageRequest`
2. `DamagePacket`
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

### 4.7 Session 拆厚第一轮

建议拆分：

1. `TurnDriver`
2. `CombatFacade`
3. `ResourceController`
4. `StatusController`
3. `SaveFacade`
4. `EventCollector`

冻结口径：

1. `FoundationGameSession` 仍可作为编排入口。
2. 但不再独占所有子逻辑。

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
   - `deep_iron_mine`
   - `ashgate_depths`
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
6. `DamageRequest -> DamageOutcome -> CombatTrace` 与 `TalentDef V2` runtime 骨架成立。

## 8. 风险与止损

1. 如果资源池和旧逻辑双轨并存，必须先清掉旧特判再继续。
2. 如果 session 继续加职责，必须停下来先拆。
3. 如果事件 payload 过度松散，必须尽快类型化，不允许全靠 `Map<String, Any>`.

## 9. 当前状态

1. 本文是 `P2-W2` 的 PR 级开发文档。
2. 该 PR 完成后，Phase 2 的核心规则语义才算真正稳定。
