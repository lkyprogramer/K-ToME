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
4. 长局实验室可稳定跑到终局。
5. Phase 3 启动时已完成对 Phase 2 `CombatTrace` / Golden Seed 的全量重录。

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

### 2.2 Boss Harness

1. 固定 Boss encounter seed。
2. 检查：
   - telegraph 出现时机
   - phase 切换门槛
   - trace 与视觉提示一致

### 2.3 Long Run Lab

1. 固定职业、种族、profile、world seed。
2. 检查：
   - 4~6 小时 run 可结束
   - 无卡死、无不可达主线
   - 长局主支线和可选支线都可达

### 2.4 Status Matrix

1. 固定同一组状态组合与 seed。
2. 检查：
   - 刷新持续时间
   - 独立叠层
   - 上限封顶
   - `FREEZE / BURN` 覆盖
   - `HASTE / SLOW` 抵消
   - 净化 / 驱散优先级

## 3. Manual White-Box Verification

1. 用 `Vanguard` 或 `Arcanist` 打一场 Boss 战，确认 telegraph 可读。
2. 用 `Rogue` 或 `Templar` 做一次洗点，确认说明、数值、资源同步。
3. 用至少一个进阶职业走完关键长局路线。
4. 检查中英双语在 talent、status、boss 命名上没有术语漂移。

## 4. Reproducibility Contract

1. 所有 golden/harness 都必须记录：
   - build id
   - seed
   - 职业/种族/profile
   - zone route
2. 失败时必须保留：
   - `CombatTrace`
   - `AIDecisionTrace`
   - `BossTrace`
   - 关键截图或录屏
