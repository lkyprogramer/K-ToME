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

1. `CombatTrace` 金样本稳定。
2. Boss telegraph / phase harness 稳定。
3. 四基础职业与两进阶职业通过核心实验室。
4. `longRunLab` 同时覆盖 `smoke` 与 `full` 两种模式。
5. Phase 3 启动时已完成对 Phase 2 `CombatTrace` / Golden Seed 的全量重录。
6. Phase 3 golden 文件带有 `phase: P3` 或等价阶段标记。

## 2. Fixed-Seed Harness Verification

### 2.1 Combat Trace

1. 固定攻击者/防御者/seed。
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
   - 默认接受区间为 `±30%`

### 2.2 Boss Harness

1. 固定至少 `2` 个不同类型的 Boss encounter seed。
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
   - headless run 的等价回合数不超过 `3000`
4. `full` 模式额外检查项：
   - `4` 基础职业 × `3` 种族的 `12` 个组合中，至少 `8` 个能到达 `abyssal_temple`
   - `50%` 以上失败 run 发生在 `deep_iron_pit` 之后

### 2.4 Status Matrix

1. 固定同一组状态组合与 seed。
2. 检查：
   - 刷新持续时间
   - 独立叠层
   - 上限封顶
   - `STUN / ROOT / HASTE / SLOW / FREEZE / SILENCE / MARKED / BANE / CURSE / WEAKEN / OVERCHARGE / STEALTH` 的刷新语义
   - `GUARD / INVULNERABLE` 的取较强值规则
   - `TAUNT` 的后来者覆盖
   - `WAR_CRY_BUFF` 的唯一效果规则：同一施法者重复施加只刷新，不同施法者可共存
   - `FREEZE / BURN` 覆盖
   - `HASTE / SLOW` 抵消
   - `OVERCHARGE` 在承受 `LIGHTNING` 伤害后被消耗，非 `LIGHTNING` 伤害不触发消耗
   - 净化 / 驱散优先级
   - 不可净化集合包含 `KNOCKBACK / INVULNERABLE / STEALTH` 与 Boss phase 锁定状态
   - `ARMOR_BREAK` 的全局 `3` 层上限
   - `STEALTH` 仅在实际受伤时解除
   - `BLEED / BURN / POISON` 在目标回合开始、行动前 tick，且 `STUN / FREEZE` 不会阻止 tick

### 2.5 Inscription Verification

1. 验证装备第 `5` 个铭文时被系统拒绝。
2. 验证同类装备第 `3` 个铭文时被系统拒绝。
3. 验证铭文冷却独立于天赋冷却。
4. 验证净化类铭文遵循默认净化策略：硬控优先，然后 `LONGEST_REMAINING`。
5. 验证铭文在非战斗和战斗状态都可正常使用。

## 3. Manual White-Box Verification

1. 用 `Vanguard` 或 `Arcanist` 打一场 Boss 战，确认 telegraph 可读。
2. 用 `Rogue` 或 `Templar` 做一次洗点，确认说明、数值、资源同步。
3. 用至少一个进阶职业走完关键长局路线。
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
   - seed
   - 职业/种族/profile
   - zone route
2. 失败时必须保留：
   - `CombatTrace`
   - `AIDecisionTrace`
   - `BossTrace`
   - 关键截图或录屏
