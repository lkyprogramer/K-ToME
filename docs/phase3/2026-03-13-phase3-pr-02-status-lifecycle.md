> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-02 状态生命周期与 Effect Carrier

**阶段**: `Phase 3 / P3-W2`  
**优先级**: `P0`  
**前置条件**: `P3-W1` 完成  
**对应问题**: Phase 2 状态系统只有注册表骨架和首批 seed（14 种），堆叠/驱散/免疫/tick 时机/互斥/净化的完整规则还没落地；同时 actor 身上的状态、地图 hazard、Boss arena aura 还没有 carrier 分层，后续一接内容就会出现大量私有特判。

**Lane-parallel 拆分**：

- **W2a (Rules Lane)**: 生命周期规则与 effect carrier（`core / game`）
- **W2b (Client Lane)**: 状态 UI 语义（`client`）

---

## 1. 阶段目标

将 Phase 2 的状态骨架升级为完整生命周期系统，冻结 actor 状态矩阵、tick/净化合同，并把 persistent effect 的宿主模型从一开始分清楚。

完成标准：

1. Phase 2 的 14 个骨架状态（`STUN / ROOT / SILENCE / BLEED / BURN / GUARD / MARKED / SHIELD / REGEN / HASTE / SLOW / FREEZE / POISON / ARMOR_BREAK`）升级为 Phase 3 正式主线。
2. 7 个新状态（`BANE / CURSE / WEAKEN / OVERCHARGE / INVULNERABLE / STEALTH / TAUNT`）进入正式主线。
3. actor 状态的完整堆叠矩阵冻结（7 种规则类型全部有对应分配）。
4. `BLEED / BURN / POISON` 的 tick 时机冻结为受影响实体回合开始、行动前，即使目标处于 `STUN / FREEZE` 也照常 tick。
5. 净化优先级可配置，不可净化集合冻结。
6. `OVERCHARGE` 明确归类为 `debuff`，并冻结其消耗语义。
7. actor 状态、区域 hazard、world aura 分别进入 `ActorEffect / AreaEffectEmitter / WorldEffect` 三类 carrier。
8. 状态 UI 语义进入正式合同，steady-state 数据源锚定到 `RenderSnapshot`，事件只负责瞬时提示（`W2b`）。

## 2. 当前问题

1. Phase 2 只有 14 种状态的注册表骨架，堆叠规则只覆盖最简单的子集。
2. 缺少 `BANE / CURSE / WEAKEN / OVERCHARGE / INVULNERABLE / STEALTH / TAUNT` 七种 Phase 3 必要状态。
3. tick 时机未正式冻结，`BLEED / BURN / POISON` 在 `STUN / FREEZE` 下是否 tick 没有明确定义。
4. 净化策略是隐式的，没有可配置的优先级和不可净化集合。
5. 互斥/覆盖规则（如 `FREEZE / BURN`、`HASTE / SLOW`）未正式实现。
6. `OVERCHARGE` 在文档链里被写成 `buff`，但实际语义是标准负面状态。
7. 现在把 `sustain / mark / ward / zone effect` 都塞进“状态系统”，却没有区分 effect carrier。
8. `WAR_CRY_BUFF` 仍以名字级规则出现在矩阵里，破坏了通用 effect 设计方向。
9. `Status HUD` 目前只说“消费事件”，steady-state icon / 层数 / 剩余回合的数据源仍有歧义。

### 2.1 本 PR 必须冻结的口径

1. actor 状态矩阵覆盖 21 种状态与 7 种规则类型。
2. `BLEED / BURN / POISON` 的 tick 时机固定为回合开始、行动前。
3. 默认净化策略：`STUN / ROOT` 硬控优先，否则清除剩余持续时间最长的负面状态。
4. 不可净化集合：`INVULNERABLE / STEALTH / Boss phase` 锁定状态；`KNOCKBACK` 作为瞬时位移不进入该集合。
5. `CURSED` 统一并名为 `CURSE`。
6. `OVERCHARGE` 明确为 `debuff`，挂在受害者身上，使其下一次成功承受的 `LIGHTNING` 伤害 `+25%`，随后被消耗。
7. `WAR_CRY_BUFF / WAR_CRY_DEBUFF` 不再作为名字级特判写入系统规则，唯一性与互斥性一律走通用字段。
8. `zone effect` 不再被视为 actor 状态；它们与 actor status 共用生命周期引擎，但宿主和净化语义不同。
9. 状态 HUD 的 steady-state（icon / 层数 / 剩余回合）只看 `RenderSnapshot`；事件只负责覆盖、净化、隐匿打破等瞬时反馈。

## 3. 范围与非目标

### 3.1 范围

1. 14 个 Phase 2 状态升级为 Phase 3 正式主线。
2. 7 个新状态进入正式主线。
3. actor 状态矩阵完整实现（7 种规则 x 21 种状态）。
4. 互斥/覆盖规则实现（`FREEZE / BURN`、`HASTE / SLOW`、`STEALTH` 特殊打破规则）。
5. tick 时机与净化合同。
6. `ActorEffect / AreaEffectEmitter / WorldEffect` carrier 抽象。
7. 状态 UI 语义同步（`W2b`）。

### 3.2 非目标

1. 不在本 PR 完成全部职业技能的状态接线（`P3-W5`）。
2. 不在本 PR 处理 AI 对状态的响应逻辑（`P3-W4` 的 `STEALTH / TAUNT` 交互合同）。
3. 不在本 PR 做 encounter 级或世界推进级追踪（`P3-W4 / W6`）。
4. 不在本 PR 大规模创建全部 status YAML 内容文件，只建立必需 schema 与首批示例。

## 4. 技术方案

### 4.1 Phase 2 状态升级表

建议文件：

```text
core/src/main/kotlin/com/ktome/core/status/StatusEffectDef.kt
core/src/main/kotlin/com/ktome/core/status/StatusInstance.kt
core/src/main/kotlin/com/ktome/core/status/StatusRegistry.kt
```

冻结口径：

1. Phase 2 的 14 个状态必须全部迁入新的堆叠规则模型。
2. Phase 1 的 `STUNNED` 已在 Phase 2 统一并名为 `STUN`。
3. `WAR_CRY_BUFF / WAR_CRY_DEBUFF` 不再作为专用状态类型维护唯一性逻辑，只作为通用 effect 的实例 ID。

### 4.2 Phase 3 新增状态

建议文件：

```text
core/src/main/kotlin/com/ktome/core/status/StatusEffectType.kt
game/src/main/resources/data/statuses/*.yaml
```

新增 7 种状态的语义与默认参数：

| 状态 | 类别 | 默认持续时间 | 特殊行为 |
| --- | --- | --- | --- |
| `BANE` | `debuff` | 3 turns | 不叠加，只刷新持续时间 |
| `CURSE` | `debuff` | 4 turns | 不叠加，只刷新；`CURSED` 统一并名为 `CURSE` |
| `WEAKEN` | `debuff` | 3 turns | 不叠加，只刷新；与 `CURSE` 独立生效 |
| `OVERCHARGE` | `debuff` | 2 turns | 不叠加，只刷新；挂在受害者身上，使其下一次成功承受的 `LIGHTNING` 伤害 `+25%`，随后被消耗 |
| `INVULNERABLE` | `buff` | 1 turn | 取较强值；不可净化 |
| `STEALTH` | `buff` | 3 turns | 不叠加，只刷新；仅在实际受到伤害时打破；不可净化 |
| `TAUNT` | `debuff` | 2 turns | 后来者覆盖，同时只保留一个嘲讽源 |

补充合同：

1. `OVERCHARGE` 可被 `cleanse`，因为它是挂在目标身上的负面状态。
2. 非 `LIGHTNING` 伤害不会消耗 `OVERCHARGE`。
3. 若 `OVERCHARGE` 持续期间再次施加，只刷新持续时间，不叠层。
4. 消耗 `OVERCHARGE` 产生的增伤固定挂在 `CombatPipeline` 的 `Pre-reduction callbacks` 阶段，只作用于触发消耗的那一次 `LIGHTNING` 伤害。

### 4.3 Effect Carrier 抽象

建议文件：

```text
core/src/main/kotlin/com/ktome/core/effect/PersistentEffect.kt
core/src/main/kotlin/com/ktome/core/effect/ActorEffect.kt
core/src/main/kotlin/com/ktome/core/effect/AreaEffectEmitter.kt
core/src/main/kotlin/com/ktome/core/effect/WorldEffect.kt
core/src/test/kotlin/com/ktome/core/effect/EffectCarrierTest.kt
```

冻结口径：

1. **`ActorEffect`**：
   - 挂在 actor 身上
   - 进入 actor 状态矩阵
   - 可被净化/驱散/免疫规则直接影响
2. **`AreaEffectEmitter`**：
   - 挂在地图 tile、trap、zone hazard 或召唤物上
   - 可持续发射或应用 effect
   - 不直接进入 actor 状态矩阵
3. **`WorldEffect`**：
   - 挂在 encounter、Boss arena、zone modifier 或全局世界规则上
   - 可以影响多个 actor 或区域
   - 存档和回放语义独立于 actor 状态
4. 三类 carrier 共用 tick / expire / trace 机制，但持有者、净化语义和存档语义必须分开。

### 4.3.1 跨 Carrier 总执行顺序

冻结口径：

1. 同一 actor 调度点的 due effect 总顺序固定为：
   - `ActorEffect`
   - `AreaEffectEmitter`
   - `WorldEffect`
2. `ActorEffect` 层内部按 `tickPriority asc -> appliedTurn asc -> effectInstanceId asc`。
3. `AreaEffectEmitter` 层内部按 `emitterPriority asc -> sourceEntityId asc -> emitterId asc`。
4. `WorldEffect` 层内部按 `worldPriority asc -> effectId asc`。
5. 每层完成后先提交本层产生的 apply/remove/cleanse 结果，再执行一次死亡检查；若目标已死亡，后续层对该 actor 的处理直接跳过。
6. Boss phase 切换、arena `WorldEffect` 的 `onEnter / onExit` 与其他跨遭遇副作用，固定落在 `WorldEffect` 层后的边界点，不得在 actor status 层中途插入。

### 4.4 完整堆叠矩阵

建议文件：

```text
core/src/main/kotlin/com/ktome/core/status/StackingRule.kt
core/src/main/kotlin/com/ktome/core/status/EffectUniqueness.kt
core/src/test/kotlin/com/ktome/core/status/StackingRuleTest.kt
```

7 种规则类型的完整分配：

1. **不叠加、只刷新持续时间**：`STUN`, `ROOT`, `HASTE`, `SLOW`, `FREEZE`, `SILENCE`, `MARKED`, `BANE`, `CURSE`, `WEAKEN`, `OVERCHARGE`, `STEALTH`
2. **独立叠层**：`BLEED`, `BURN`, `POISON`
3. **上限封顶**：`ARMOR_BREAK`（最多 3 层，跨来源全局上限）
4. **取较强值**：`SHIELD`, `REGEN`, `GUARD`, `INVULNERABLE`
5. **后来者覆盖**：`TAUNT`
6. **唯一效果**：走通用唯一性字段，不再写 `WAR_CRY_BUFF` 专名规则
7. **互斥/覆盖**：`FREEZE / BURN` 互斥触发元素交互；`HASTE / SLOW` 折算为 `speedModifier` 净值

唯一性通用字段：

1. `uniquenessKey`
2. `exclusiveGroup`
3. `sourceScopedUnique`
4. `replacePolicy`
5. `TAUNT` 覆盖时必须写入显式事件（如 `StatusEvent.TAUNT_OVERRIDE`），用于 Boss 战调试与目标切换回放。

### 4.5 互斥与覆盖规则

建议文件：

```text
core/src/main/kotlin/com/ktome/core/status/MutualExclusion.kt
core/src/main/kotlin/com/ktome/core/status/ElementalInteraction.kt
core/src/test/kotlin/com/ktome/core/status/MutualExclusionTest.kt
```

冻结口径：

1. `FREEZE` 与 `BURN` 互斥，后施加的覆盖前者，并触发元素交互事件。
2. `HASTE` 与 `SLOW` 都不叠加，只保留单个当前值；统一折算为：
   - `effectiveSpeed = baseSpeed + hasteModifier - slowModifier`
3. `STEALTH` 只在实际受到伤害时被 AoE 打破；仅进入 AoE 覆盖范围但最终未受伤，不解除隐匿。
4. `BLEED / BURN / POISON` 在 `STEALTH` 期间照常 tick；若 tick 造成了实际伤害，则同样打破 `STEALTH`。

### 4.6 Tick 时机合同

建议文件：

```text
core/src/main/kotlin/com/ktome/core/status/StatusTickResolver.kt
core/src/test/kotlin/com/ktome/core/status/StatusTickResolverTest.kt
```

冻结口径：

1. `BLEED / BURN / POISON` 固定为在受影响实体的回合开始、行动前结算。
2. 即使目标处于 `STUN / FREEZE` 也照常 tick。
3. tick 产生的伤害必须写入 `CombatResolutionTrace`，可追踪来源。
4. `AreaEffectEmitter / WorldEffect` 的 tick 也走统一的 tick engine，但不进入 actor 状态矩阵。

### 4.7 净化合同

建议文件：

```text
core/src/main/kotlin/com/ktome/core/status/CleansePolicy.kt
core/src/test/kotlin/com/ktome/core/status/CleanseTest.kt
```

冻结口径：

1. 默认净化策略：若存在 `STUN / ROOT`，优先清除这两类硬控；否则清除剩余持续时间最长的负面状态。
2. 净化优先级必须可配置（预留 `CleansePolicy` 接口）。
3. `KNOCKBACK` 作为 `EffectOp.Displacement(type = PUSH)` 的瞬时位移处理，不进入 actor 持久状态矩阵，因此不属于可/不可净化集合。
4. 不可净化集合：`INVULNERABLE`, `STEALTH`, Boss phase 锁定状态。
5. `AreaEffectEmitter / WorldEffect` 默认不受 actor 级 `cleanse` 影响；若设计要求可被技能移除，必须单独声明 `remoteRemovalPolicy`，不得复用 `dispel` 术语。

### 4.8 状态进入系统的入口约束

冻结口径：

1. 所有 `ActorEffect` 的施加都必须显式声明来自 `P3-W1` 冻结的 `ApplicationPolicy`。
2. `W2` 不再单独定义“状态施加入口”的第二套语义。
3. `HOSTILE_HIT_THEN_SAVE / HOSTILE_SAVE_ONLY` 的差异必须能在 trace 中定位。
4. 事件总线的 `turnId / actionId / sequence / phase / cause` 结构以详设文档 §5.2 为权威；状态生命周期相关事件不得自造第二套事件头。

### 4.9 [W2b] 状态 UI 语义

建议文件：

```text
client/src/main/kotlin/com/ktome/client/ui/status/StatusHudRenderer.kt
client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt
```

冻结口径：

1. 状态 icon 显示必须区分 buff（正面）和 debuff（负面）。
2. 独立叠层状态（`BLEED / BURN / POISON`）必须显示当前层数。
3. 上限封顶状态（`ARMOR_BREAK`）必须显示当前层数和上限。
4. 持续时间必须在 HUD 中可见（回合数倒计时）。
5. 互斥状态被覆盖时必须有视觉反馈（如 `FREEZE` 被 `BURN` 覆盖）。
6. steady-state 的状态 icon / 层数 / 剩余回合只消费 `RenderSnapshot.statusHud`（或等价 snapshot 字段），不得靠事件流自己维持权威状态。
7. `StatusEvent` 只负责覆盖、净化、隐匿打破、层数变化等瞬时提示，不承担 steady-state 对账。

## 5. 推荐改动面

### 5.1 `core`

1. `status` 包全面扩展（`StackingRule` / `CleansePolicy` / `StatusTickResolver` / `MutualExclusion` / `ElementalInteraction`）。
2. `effect` 包新建（`PersistentEffect` / `ActorEffect` / `AreaEffectEmitter` / `WorldEffect`）。
3. `StatusEffectDef` 与 `StatusInstance` 扩展唯一性、净化和 carrier 相关字段。

### 5.2 `game`

1. `statuses/*.yaml` 数据文件扩展。
2. zone hazard / arena aura 的数据不再伪装成 actor status。

### 5.3 `client`

1. `StatusHudRenderer` 新建。
2. `StatusIconResolver` 新建。
3. 状态 icon 资源接线。

## 6. 测试与自证

### 6.1 必测类

1. `StackingRuleTest`
2. `EffectCarrierTest`
3. `CarrierExecutionOrderTest`
4. `StatusTickResolverTest`
5. `CleanseTest`
6. `MutualExclusionTest`
7. `StatusLifecycleIntegrationTest`
8. `TauntOverrideTest`
9. `StealthBreakTest`
10. `OverchargeConsumptionTest`
11. `ArmorBreakCapTest`
12. `StatusRenderSnapshotSyncTest`

### 6.2 必测行为

1. `STUN / ROOT / HASTE / SLOW / FREEZE / SILENCE / MARKED / BANE / CURSE / WEAKEN / OVERCHARGE / STEALTH` 的刷新语义正确。
2. `BLEED / BURN / POISON` 独立叠层且在 `STUN / FREEZE` 下仍 tick。
3. `ARMOR_BREAK` 全局 3 层上限生效。
4. `SHIELD / REGEN / GUARD / INVULNERABLE` 按强度优先、持续时间次级比较。
5. `TAUNT` 后来者覆盖。
6. `OVERCHARGE` 是 `debuff`，挂在受害者身上，在承受 `LIGHTNING` 伤害时使该次伤害 `+25%` 并消耗。
7. `WAR_CRY` 唯一性通过通用字段实现，而不是专名特判。
8. `FREEZE / BURN` 互斥触发元素交互。
9. `STEALTH` 仅在实际受伤时解除。
10. `AreaEffectEmitter / WorldEffect` 不会错误进入 actor 状态矩阵。
11. 跨 carrier 的总顺序、层内 tie-break 与层后死亡检查稳定。
12. 净化优先级：硬控优先 -> 最长剩余。
13. 不可净化集合正确。
14. `Status HUD` 的 steady-state 只来自 `RenderSnapshot`，事件只提供瞬时反馈。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.status.*"
./gradlew :core:test --tests "com.ktome.core.effect.*"
./gradlew test
```

### 6.4 白盒验证

1. 施加 `BLEED + STUN` 于同一目标，确认 `BLEED` 在 `STUN` 期间照常 tick。
2. 先施加 `FREEZE` 再施加 `BURN`，确认 `FREEZE` 被移除并触发元素交互。
3. 对同一目标施加两个不同源的 `TAUNT`，确认后来者覆盖。
4. 进入 AoE 覆盖范围但 `STEALTH` 目标最终未受伤，确认隐匿不打破。
5. 在 Boss arena 中制造 `WorldEffect`，确认它不会以 actor status 的形式出现在 HUD 状态矩阵里。

## 7. 出口门禁

1. 21 种 actor 状态全部进入正式主线。
2. actor 状态矩阵 7 种规则全部有自动化测试。
3. `OVERCHARGE` 的 `debuff` 口径收口完成。
4. tick 时机、净化优先级、不可净化集合测试全绿。
5. `ActorEffect / AreaEffectEmitter / WorldEffect` carrier 分层进入正式合同。
6. 跨 carrier 的总顺序、死亡检查边界与 phase 切换边界收口完成。
7. 状态 UI 的 steady-state 锚定 `RenderSnapshot`，叠层和持续时间可见，事件只承担瞬时提示。
8. `core.status` 与 `core.effect` 的关键路径可稳定回归。
