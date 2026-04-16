> 执行前必须先完整阅读并接受：
> `docs/rule/kotlin.md`
> `docs/rule/ai-change-governance.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part1.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part3.md`

# Phase 4 V3 PR-01 体验 Gate 与关键路径 Pacing 硬化

**阶段**: `Phase 4 / Post-Review Follow-up / V3-PR-01`  
**优先级**: `P0`  
**工作量评估**: `M`（`2~4` 人日）  
**前置条件**: 无  
**对应问题**:

1. 关键路径 zone 存在 `0 战斗 / 2 回合拿 objective` 的平谷层
2. 现有 owner metric 对这类平谷暴露不够硬
3. `v2opt` 文档 coverage 没完全覆盖主线路径中的 `grey_gate_depths`、`abyssal_temple`

## 0. 验证约束

1. 默认开发回路先跑 `./gradlew verifyChanged`。
2. 本 PR 默认联合验收固定为：
   - `longRunLab`
   - `verifyOwner`
   - `phase4Report`
3. 若修改 `phase4Report` 指标定义或 report schema，再额外执行 `reportPhase4` 做显式 parity 对账。
4. canonical 证据只引用 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` 与 `build/reports/harness/long-run-full.json`。

## 1. 阶段目标

把当前主线路径里“像过场层”的 zone，重新拉回探索->战斗->奖励 的正式循环中，同时把这个问题写进 owner metric，防止后续再隐性回退。

完成标准：

1. 关键路径 zone 不再出现 `avgEnemyTurns = 0` 或 `avgVisibleHostileTurnCount = 0`
2. `avgObjectiveAcquireTurn` 不再稳定压到 `2.0`
3. experience gate 文档与 report 能明确暴露这些 zone 的下限

## 2. 为什么单独成 PR

这个 PR 先做，不是因为它最炫，而是因为它最基础：

1. 不先修主线路径 pacing，后续 reward、boss、hidden 的任何增强都会落在半空的地图过程上。
2. 不先修 owner metric，后续 PR 仍可能在“体验有问题但报告全绿”的状态下推进。

## 3. 当前问题拆解

### 3.1 关键路径 objective 过浅

当前 [game/src/main/resources/data/objectives/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/objectives/index.yaml) 中，多处 objective 直接放在 `player_start / stairs_up`。

### 3.2 中后段 zone 常规敌人下限过低

当前 [game/src/main/kotlin/com/ktome/game/GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt) 中：

1. `grey_gate_depths -> 0`
2. `abyssal_temple -> 0`
3. `underground_river -> 1`

### 3.3 体验 owner coverage 不完整

`v2opt` 当前更强调热点区，但对 `grey_gate_depths` 这种主线平谷层约束不足。

## 4. 必须冻结的合同

1. 不新增 zone，不改 route graph。
2. 不引入新战斗系统或新地形系统。
3. 只修主线路径节奏与对应 owner metric，不顺手扩内容量。

## 5. 范围与非目标

### 5.1 范围

1. objective anchor 深度调整
2. key zone 怪物数量下限调整
3. experience gate / long-run owner metric 回写
4. Phase4 审阅/验证文档同步

### 5.2 非目标

1. 不改 loot 分发合同
2. 不改 hidden reward identity
3. 不做 boss variant 重写

## 6. 技术方案

### 6.1 objective 深度回收

优先处理：

1. `greenwood_signal_hunt.trail_cache`
2. `grey_gate_seal_rite.seal_cache`
3. `underground_river_crossing.crystal_cache_chest`

原则：

1. 不能贴脸起点/楼梯
2. 必须迫使玩家经过主路径中段
3. 最好与已有 optional/pressure 路径形成风险交换

### 6.2 key zone 敌人下限回收

建议第一版直接把下限拉到：

1. `grey_gate_depths: 0 -> 2`
2. `underground_river: 1 -> 2`
3. `abyssal_temple: 0 -> 1~2`

### 6.3 owner metric 硬化

`long-run` 与 `phase4Report` 新增或抬升以下 owner 指标：

1. `avgObjectiveAcquireTurn`
2. `avgVisibleHostileTurnCount`
3. `avgEnemyTurns`
4. `criticalPathCombatFloorSatisfied`

### 6.4 文档回写

至少同步：

1. `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md`
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
3. 必要时 `docs/phase4/roadmap.md`

## 7. 推荐改动面

1. `game/src/main/resources/data/objectives/index.yaml`
2. `game/src/main/kotlin/com/ktome/game/GameModule.kt`
3. `build/reports/harness/long-run-*` 生成链
4. `tools/src/main/kotlin/com/ktome/tools/phase4/*`
5. 上述文档

## 8. 任务拆解

### Task 1：关键路径 objective 深度回收

- **Owner**: `game` content
- **目标**: 消除 2 回合 objective 获取
- **验收**:
  - 目标点不再位于 `player_start / stairs_up`
  - `avgObjectiveAcquireTurn` 明显上升

### Task 2：关键路径 encounter 下限回收

- **Owner**: `game` runtime/content
- **目标**: 消除 `0 战斗` zone
- **验收**:
  - `avgEnemyTurns > 0`
  - `avgVisibleHostileTurnCount > 0`

### Task 3：owner metric 文档和报告同步

- **Owner**: `tools` + docs
- **目标**: 让后续 PR 不再遗漏主线平谷
- **验收**:
  - report 明确列出 critical-path zone 下限
  - 文档中 owner zone 列表与 report 对齐

## 9. 推荐命令

```bash
./gradlew longRunLab
./gradlew verifyOwner
./gradlew phase4Report
```

## 10. 完成后才能进入下一 PR 的条件

1. 主线路径的 pacing 问题已经能在 canonical report 中被直接看见。
2. `grey_gate_depths`、`abyssal_temple` 不再是“报告上勉强解释、玩家体验里发空”的例外层。
