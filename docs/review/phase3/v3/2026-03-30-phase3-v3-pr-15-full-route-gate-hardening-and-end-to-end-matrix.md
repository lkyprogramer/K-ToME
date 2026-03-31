> 执行前必须先完整阅读并接受：
> `docs/review/phase3/phase3_opt_deep_review_final.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`
> `docs/review/phase3/2026-03-26-phase3-follow-up-pr-07-objective-runtime-and-gate-hardening.md`

# Phase 3 V3 - PR-15 Full Route Gate Hardening And End-To-End Matrix

**阶段**: `Phase 3 / v3 follow-up`  
**优先级**: `P0`  
**前置条件**: 现有 `longRunLab / bossHarness / smoke bot` 主链稳定，允许先收紧门禁再继续补内容  
**对应问题**: 当前实验室已经能证明局部稳定，但仍存在“中途起跑替代完整 run”“进阶职业只测 late-route smoke”“full-route 成立度被高估”的风险。继续先做内容补量，会把所有后续判断建立在偏软的验证口径上。  

**Lane-parallel 拆分**：

- **W15a (QA Lane)**: `LongRunLabFullTest` 变成真正的 full-route matrix
- **W15b (QA Lane)**: 进阶职业补完整 smoke / route probe 降级
- **W15c (QA Lane)**: harness report 字段与门禁条件收紧

---

## 1. 阶段目标

把 `Phase 3` 当前“系统稳定性不错”升级为“完整 run 的起点到终点也被正式 gate 覆盖”。

完成标准：

1. `LongRunLabFullTest` 至少有一组矩阵全部从 `shattered_outpost` 起跑。
2. 中途起跑场景不再与 full-route 成功等价，而是明确标注为 `route_probe / late_route_probe`。
3. `berserker / spellblade` 至少各补一条从开局出发的完整 smoke。
4. `afterDeepIronRatio` 在 `nonVictoryCount == 0` 时输出 `N/A`，不再伪装成 `1.0`。
5. `long-run-full` 报告必须能区分：
   - full-route
   - branch-inclusive
   - late-route probe
6. `bossHarness` 与 `longRunLab` 的成功不再默认等价于“完整 Phase 3 长局已成立”。

## 2. 当前问题

1. `LongRunLabFullTest` 的部分组合直接从 `molten_core / crystal_cavern / underground_river / bandit_camp / elven_ruins` 起跑。
2. `LongRunLabTest` 中 `berserker / spellblade` 是从 `abyssal_temple` 起跑的 late-route smoke，不是完整 run。
3. 当前报告里的 `branchSampleCount / routeHashDistribution` 有参考价值，但还不足以约束完整起跑矩阵。
4. `afterDeepIronRatio` 对零失败样本的表达不严谨，容易制造假阳性叙述。
5. 现有实验室更像“稳定性实验室”，还不是严格的“Phase 3 体验闭环 gate”。

### 2.1 本 PR 必须冻结的口径

1. `full-route` 的正式定义固定为：
   - `zoneId = shattered_outpost`
   - `routeIndex = 0`
   - 必须从完整主线起跑
2. `late-route smoke` 可以保留，但一律降级为：
   - `route_probe`
   - `late_route_probe`
   - 不再用于替代 end-to-end gate
3. `Phase 3` 的 `full-route` matrix 只要求基础职业全覆盖，不要求一次就把所有进阶职业也做成 full-matrix。
4. 进阶职业第一版要求固定为：
   - `berserker` 至少 1 条完整 smoke
   - `spellblade` 至少 1 条完整 smoke
5. `afterDeepIronRatio` 的零失败场景必须明确输出 `N/A`。
6. 新 gate 不得反过来偷改游戏逻辑以适配测试。

## 3. 范围与非目标

### 3.1 范围

1. `LongRunLabTest`
2. `LongRunLabFullTest`
3. `HeadlessRunHarness`
4. `ScenarioModels`
5. 报告字段和 markdown 结构

### 3.2 非目标

1. 不改 `SmokeBot` 的核心玩法策略，除非新 matrix 直接暴露出卡死缺陷
2. 不做新的 world content
3. 不做新的 zone mechanic
4. 不改 reward / affix / talent 数值

## 4. 技术方案

### 4.1 [W15a] Full-Route Matrix Hardening

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt
```

冻结口径：

1. `LongRunLabFullTest` 至少拆成两层：
   - `fullRouteMatrix`
   - `branchProbeMatrix`
2. `fullRouteMatrix` 第一版固定为：
   - `4` 基础职业 × `3` 种族
   - 全部从 `shattered_outpost` 起跑
3. `branchProbeMatrix` 第一版固定为：
   - `bandit_camp`
   - `elven_ruins`
   - `molten_core`
   - `crystal_cavern`
   - 至少覆盖 `3` 条不同 route hash
4. `branchProbeMatrix` 只负责证明分支稳定，不承担 full-route 通过率门槛。

### 4.2 [W15b] Advanced-Class End-To-End Smoke

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt
game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt
```

冻结口径：

1. `berserker / spellblade` 保留 late-route smoke，但要新增：
   - 各 1 条从 `shattered_outpost` 起跑的完整 smoke
2. 这两条 smoke 的目标固定为：
   - 到达终局状态即可
   - 不强制通关
3. late-route smoke 继续存在，但必须明确作为：
   - `late-route viability probe`
   - 不是 `Phase 3` 完整 run 的唯一证明

### 4.3 [W15c] Report And Gate Semantics

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt
game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt
```

冻结口径：

1. 新增或强化的报告字段至少包括：
   - `scenarioType`
   - `isFullRoute`
   - `branchSampleCount`
   - `routeHashDistribution`
   - `nonVictoryCount`
   - `afterDeepIronRatio = N/A`
2. `full-route` gate 第一版建议门槛：
   - `12` 个基础职业矩阵中至少 `8` 个到达 `abyssal_temple`
   - `headlessTurnEquivalent <= 3000`
   - `nonVictory` 失败样本中 `50%+` 发生在 `deep_iron_pit` 之后
3. 当 `nonVictoryCount == 0` 时：
   - 输出 `N/A`
   - 不再把该字段当成正向通过证据
4. `bossHarness` 只需要同步确认 `Phase 3` 关键 boss 的正式 trace 约束不变，不在本 PR 内重做 boss runtime。

## 5. 推荐改动面

### 5.1 `game / harness`

1. `LongRunLabFullTest.kt`
2. `LongRunLabTest.kt`
3. `HeadlessRunHarness.kt`
4. `ScenarioModels.kt`

### 5.2 `tools / QA`

1. harness report markdown / json 输出
2. build/report 元数据字段

## 6. 测试与自证

### 6.1 必测类

1. `LongRunLabFullTest`
2. `LongRunLabTest`
3. `HeadlessSmokeSuiteTest`

### 6.2 必测行为

1. full-route matrix 全部从 `shattered_outpost` 起跑
2. `berserker / spellblade` 各有完整 smoke
3. branch probes 仍覆盖多条 route hash
4. `afterDeepIronRatio` 零失败场景不再输出 `1.0`

### 6.3 自动化命令

```bash
./gradlew :game:longRunLab --tests "com.ktome.game.harness.LongRunLabTest"
./gradlew :game:longRunLab --tests "com.ktome.game.harness.LongRunLabFullTest"
./gradlew longRunLab
./gradlew check
```

### 6.4 白盒验证

1. 抽查一条基础职业完整 full-route 报告
2. 抽查一条进阶职业完整 smoke 报告
3. 确认报告里能区分 full-route 和 late-route probe

## 7. 出口门禁

1. full-route matrix 正式建立
2. advanced-class 完整 smoke 正式建立
3. route probe 降级完成，不再冒充 end-to-end 成功
4. `afterDeepIronRatio` 语义修正完成
5. `./gradlew longRunLab` 仍保持绿色

## 8. 风险与止损

### 8.1 风险

1. gate 收紧后当前 green build 可能转红
2. `SmokeBot` 的早中段弱点会被更真实地暴露出来

### 8.2 止损

1. 先收紧口径，不在本 PR 里顺手补 gameplay 内容
2. 若 full-route matrix 大面积转红，优先用报告定位问题，再交给后续 PR 修玩法
