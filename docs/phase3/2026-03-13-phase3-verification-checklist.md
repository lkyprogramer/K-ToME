# Phase 3 Verification Checklist

## 1. Automated Verification

```bash
./gradlew test
./gradlew :core:test
./gradlew combatTraceGolden
./gradlew bossHarness
./gradlew soloClearLab
./gradlew longRunLab
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 必须检查的结果

1. `FORMULA / STATUS / INTEGRATION / LONG_RUN` 分层 golden 与 harness 稳定。
2. Boss telegraph / phase harness 稳定。
3. 四基础职业与两进阶职业通过核心实验室。
4. `longRunLab` 同时覆盖 `smoke` 与 `full` 两种模式。
5. `W1` 已完成对 Phase 2 `FORMULA` corpus / Golden Seed 的全量重录。
6. golden / harness 文件带有 `phaseId / rulesetVersion / traceSchemaVersion / corpusId`。

## 2. Fixed-Seed Harness Verification

### 2.1 Resolution Trace / Golden Corpus

1. 固定攻击者/防御者/seed，并按 corpus 分层回归。
2. 检查：
   - 命中
   - 暴击
   - 暴击抗性
   - 抗性/穿透
   - `Power/Save` 成功 / 失败两侧
   - 状态施加
   - 元素交互
   - child trace 触发
3. 固定标准角色 vs 标准怪物场景，比较 P3 公式与 P2 公式输出：
   - 伤害、命中、状态施加率的变化不出现数量级崩塌
   - 伤害类默认接受区间为 `±30%`
   - 命中率类默认接受区间为 `±10` percentage points
   - 状态施加率类默认接受区间为 `±10` percentage points

### 2.2 Boss Harness

1. Phase 3 总出口固定 `3` 个 Boss encounter seed；`PR-04` 的 2 Boss 只视为工具基线。
2. 检查：
   - telegraph 出现时机
   - phase 切换门槛
   - trace 与视觉提示一致

### 2.3 Long Run Lab

1. `smoke`：
   - 固定职业、种族、profile、world seed
   - 验证 run 能到达终局状态，不要求通关
2. `full`：
   - 扩展到代表性职业/种族矩阵
   - 统计死亡分布、平均时长、不可达主线
3. 两种模式共同检查项：
   - 无卡死、无不可达主线
   - 长局主支线和可选支线都可达
   - `headlessTurnEquivalent` 不超过 `3000`
   - save/load round-trip 后 `worldFlags / unlockedRoutes / defeatedBossIds / claimedRouteRewards` 保持一致
   - checkpoint 商店至少存在规定数量的可负担 rescue 工具
4. `full` 模式额外检查项：
   - `4` 基础职业 × `3` 种族的 `12` 个组合中，至少 `8` 个能到达 `abyssal_temple`
   - `50%` 以上失败 run 发生在 `deep_iron_pit` 之后
5. 进阶职业 smoke：
   - `Berserker` 至少 `1` 组 smoke 到达终局状态
   - `Spellblade` 至少 `1` 组 smoke 到达终局状态

### 2.4 Status Matrix

1. 固定同一组状态组合与 seed。
2. 检查：
   - 刷新持续时间
   - 独立叠层
   - 上限封顶
   - `STUN / ROOT / HASTE / SLOW / FREEZE / SILENCE / MARKED / BANE / CURSE / WEAKEN / OVERCHARGE / STEALTH` 的刷新语义
   - `GUARD / INVULNERABLE` 的取较强值规则
   - `TAUNT` 的后来者覆盖
   - 通用唯一性字段（`uniquenessKey / exclusiveGroup / sourceScopedUnique / replacePolicy`）正确生效
   - `FREEZE / BURN` 覆盖
   - `HASTE / SLOW` 抵消
   - `OVERCHARGE` 在承受 `LIGHTNING` 伤害后使该次伤害 `+25%` 并被消耗，非 `LIGHTNING` 伤害不触发消耗
   - 净化优先级与远程移除边界
   - 不可净化集合包含 `INVULNERABLE / STEALTH` 与 Boss phase 锁定状态；`KNOCKBACK` 作为瞬时位移不进入该集合
   - `ARMOR_BREAK` 的全局 `3` 层上限
   - `STEALTH` 仅在实际受伤时解除
   - `BLEED / BURN / POISON` 在目标回合开始、行动前 tick，且 `STUN / FREEZE` 不会阻止 tick；若对 `STEALTH` 目标造成实际伤害，也会打破隐匿
   - 跨 carrier 总顺序固定为 `ActorEffect -> AreaEffectEmitter -> WorldEffect`
   - `Status HUD` 的 steady-state 只看 `RenderSnapshot`，`StatusEvent` 只负责瞬时反馈

### 2.5 Inscription Verification

1. 验证装备第 `5` 个铭文时被系统拒绝。
2. 验证同类装备第 `3` 个铭文时被系统拒绝。
3. 验证铭文冷却独立于天赋冷却。
4. 验证净化类铭文遵循默认净化策略：硬控优先，然后 `LONGEST_REMAINING`。
5. 验证铭文在非战斗和战斗状态都可正常使用。

## 3. Manual White-Box Verification

1. 用 `Vanguard` 或 `Arcanist` 打一场 Boss 战，确认 telegraph 可读。
2. 用 `Rogue` 或 `Templar` 做一次洗点，确认说明、数值、资源同步。
3. 用 `Berserker` 与 `Spellblade` 各完成至少一次 smoke 级长局验证。
4. 检查中英双语在 talent、status、boss 命名上没有术语漂移。
5. 检查断点成长 UI：
   - 当前效果与下一断点预览区分清楚
   - 未激活效果不会伪装成已生效
6. 用 `Rogue` 或具备隐匿能力的职业触发一次 `STEALTH`，确认：
   - AI 停止直接追踪并移动到最后已知位置
   - `STEALTH` 被 AoE 实际伤害打破后 AI 恢复追踪
7. 用 `Vanguard` 或具备嘲讽能力的职业对精英使用 `TAUNT`，确认：
   - 精英在 `TAUNT` 期间只攻击嘲讽源
   - `TAUNT` 结束后恢复正常目标选择

## 4. Reproducibility Contract

1. 所有 golden/harness 都必须记录：
   - build id
   - phase id
   - ruleset version
   - trace schema version
   - corpus id
   - seed
   - 职业/种族/profile
   - zone route / zoneRouteHash
   - buildHash
2. 失败时必须保留：
   - `CombatResolutionTrace`
   - `AIDecisionTrace`
   - `BossTrace`
   - 关键截图或录屏

## 5. Headless Turn Contract

1. `headlessTurnEquivalent` 的定义固定为：
   - 每完成 `1` 次 actor 的完整行动结算记 `1`
   - 纯视觉帧记 `0`
   - 同一 actor 行动内的多段伤害、child trace、连锁触发不额外加回合
   - 独立 world tick 若触发下一次调度点，记 `1`
2. `BossPhase` 切换与 telegraph 预告若包含在当前行动结算中，不额外增加等价回合数。
