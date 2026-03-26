> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-07 Objective Runtime & Gate Hardening

**阶段**: `Phase 3 / follow-up`  
**优先级**: `P0`  
**前置条件**: `PR-06` 已完成，`WorldProgress / Quest / Gate / RouteReward / RunSummary` 主链已稳定  
**对应问题**: 当前 Phase 3 的世界结构合同已经存在，但 `objective / optional zone / zone metadata / long-run gate / boss gate` 仍有明显“数据已定义、运行时未成立”的断层。继续先做奖励随机化或内容补量，只会把后续改造叠在一个假完成地基上。

**Lane-parallel 拆分**：

- **W7a (Rules Lane)**: `completionRule` typed runtime evaluator、quest state 真正写回、zone clear 不再整条 quest 自动完成
- **W7b (Content Lane)**: optional / late-zone objective hook、interactable 最小闭环、zone metadata 至少进入一个 runtime 行为
- **W7c (Tools/QA Lane)**: `longRunLab / bossHarness` gate 收紧、branch-inclusive matrix、trace-required 报告字段

---

## 1. 阶段目标

把 Phase 3 当前“合同已存在、玩法未落地”的世界推进部分收成正式 runtime，并让核心 gate 能拒绝假阳性。

完成标准：

1. `completionRule` 不再是 schema 死字段，而是正式 runtime contract。
2. objective progress 不再只写日志，必须写回 `worldProgress.questStates`。
3. zone clear 不再无条件把整条 quest 的 objective 一次性置为 `COMPLETED`。
4. 四个 optional zone 都至少拥有一个真实 objective / interactable / zone-specific runtime hook。
5. `underground_river` 与 `abyssal_temple` 至少各有一个可观测的 objective/interactable 节点。
6. `environmentTheme / specialMechanics / uniqueContentTag` 至少各有一个真实 runtime 消费面。
7. `longRunLabFullTest` 不再依赖单一路线和假 `afterDeepIronRatio` 绿灯。
8. `bossHarness` 不再允许 `aiTraceCount=0` 的正式成功样本通过。

## 2. 当前问题

1. `completionRule` 当前只在 schema/data loader 里出现，运行时没有对应 evaluator。
2. `recordObjectiveProgress()` 当前只发 `log.objective.progress`，不更新 quest state。
3. `completeCurrentZoneQuest()` 当前仍在 zone clear 时把 linked quest 的 objective 整体置为 `COMPLETED`。
4. `bandit_camp / elven_ruins / molten_core / crystal_cavern` 当前仍缺 `objectiveSetId` 或缺最小 runtime hook。
5. `underground_river / abyssal_temple / abyssal_heart` 即使已有 objective 语义，也仍缺 interactable 密度，实际仍接近“清怪推进”。
6. `environmentTheme / specialMechanics / uniqueContentTag` 仍大量停留在 schema 字段，没有进入正式运行时行为。
7. `LongRunLabFullTest` 当前仍固定 `FOUNDATION_ZONE_ROUTE`，分支覆盖不足。
8. `afterDeepIronRatio` 当前在 `nonVictoryReports.isEmpty()` 时仍会被写成 `1.0`，这会把“没有失败样本”伪装成“失败都发生在 late game”。
9. `BossHarnessTest` 当前只有在 case 显式给 `expectedSelectedActionId` 时才要求 AI trace 非空，导致 `aiTraceCount=0` 仍可能整体通过。

### 2.1 本 PR 必须冻结的口径

1. `completionRule` 进入 runtime 后必须是 typed contract，不允许继续散落字符串分支。
2. quest/objective state 是正式真源，日志只是反馈副产物。
3. zone clear 只能通过 evaluator 完成合法 objective，不允许保留“整条 quest 自动完成”捷径。
4. 本 PR 只落现有 Phase 3 已出现的 objective rule，不扩成通用脚本系统。
5. optional zone 的“最小成立”标准固定为：
   - 有 `objectiveSetId`
   - 且至少有 `1` 个 interactable、或 `1` 个 zone-specific elite/cache trigger
6. `underground_river` 与 `abyssal_temple` 必须各有一个可被 runtime 观察到的 objective 节点。
7. `environmentTheme / specialMechanics / uniqueContentTag` 至少各进入一个 runtime 消费点，不允许继续只做 schema 完整性字段。
8. `longRunLabFullTest` 必须改成 branch-inclusive matrix，并把 `afterDeepIronRatio` 的零失败场景显式记为 `N/A`。
9. `bossHarness` 的正式 Phase 3 case 必须要求 phase-specific AI action trace，不允许把 trace 断言做成“可选增强”。
10. 本 PR 不引入第二套 objective 系统、第二套 gate、第二套 zone metadata 解释器。

## 3. 范围与非目标

### 3.1 范围

1. `completionRule` typed runtime evaluator。
2. objective progress 与 `worldProgress.questStates` 真正同步。
3. zone clear 结算逻辑收口。
4. optional / late-zone objective hook 与 interactable 最小闭环。
5. zone metadata 的最小 runtime 消费。
6. `longRunLab / bossHarness` gate 与报告字段收紧。

### 3.2 非目标

1. 不做路线奖励 / Boss 奖励 / cache 奖励的 affix milestone 化；这属于 `PR-08`。
2. 不做基础职业 `64` talent 与 monster roster `~60` 的补量；这属于 `PR-09`。
3. 不做 world map / player creation / outcome summary 的玩家信息面收口；这属于 `PR-10`。
4. 不做 Phase 4 级别的 hidden content、ProcGen、artifact、unique。
5. 不修改 save/profile 持久化策略；当前目标只是在既有正式状态上把 runtime 真正做实。
6. 不新增 zone-exclusive BGM、整套 tileset 或独立插画资产；若 objective/interactable 需要最小反馈，默认复用现有 `prop.* / icon.* / audio.interactable.* / audio.boss.warning` 表现资源。

## 4. 技术方案

### 4.1 [W7a] Objective Runtime Contract

建议文件：

```text
game/src/main/kotlin/com/ktome/game/objective/ObjectiveCompletionRule.kt
game/src/main/kotlin/com/ktome/game/objective/ObjectiveRuntimeEvaluator.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/test/kotlin/com/ktome/game/ObjectiveRuntimeEvaluatorTest.kt
game/src/test/kotlin/com/ktome/game/LongRunWorldStructureSessionTest.kt
```

冻结口径：

1. Phase 3 第一版只支持当前已出现的 runtime rule：
   - `defeat_zone_boss`
   - `explore_floor_pair`
   - `secure_forge_path`
2. `completionRule` 需要在 `game` 层解析成 typed model，而不是在 `FoundationGameSession` 里用字符串分支散落判断。
3. 未识别 rule 必须 fail fast，不允许静默回落到“zone clear 即完成”。
4. `recordObjectiveProgress()` 需要升级为正式状态写回入口：
   - 接收 `objectiveId`
   - 写回 linked quest 的 objective state
   - 支持 progress token 去重
   - 合法时推进 `AVAILABLE -> IN_PROGRESS -> COMPLETED`
5. `completeCurrentZoneQuest()` 需要改成 evaluator 驱动：
   - 只完成已满足 rule 的 objective
   - 若仍有未满足 objective，则 quest 保持未完成
   - gate 不得因为 zone clear 提前开放
6. quest state 与 objective state 是正式真源；日志、summary、route hint 都只能从真源派生。

### 4.2 [W7b] Optional / Late-Zone Hook Activation

建议文件：

```text
game/src/main/resources/data/objectives/index.yaml
game/src/main/resources/data/zones/index.yaml
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt
game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt
```

冻结口径：

1. 四个 optional zone 全部需要补到“最小可感知 runtime hook”：
   - `bandit_camp`: cache / ambush / shard-risk 主题
   - `elven_ruins`: relic / cleanse / ward 主题
   - `molten_core`: forge-path / heat-pressure 主题
   - `crystal_cavern`: node / line-of-sight / shard resonance 主题
2. 每个 optional zone 至少满足下列之一：
   - `1` 个 interactable
   - `1` 个 zone-specific elite/cache trigger
   - `1` 个 objective completion hook
3. `underground_river` 必须补一个与位移、地形穿越或路径开通相关的 objective 节点。
4. `abyssal_temple` 必须补一个与净化、护盾、抗性或 ritual interrupt 相关的 objective 节点。
5. `abyssal_heart` 只允许加很轻的 pre-boss hook；若超出范围，可明确延后，不强塞进本 PR。
6. `environmentTheme / specialMechanics / uniqueContentTag` 的最低 runtime 消费要求固定为：
   - `environmentTheme` 或 `specialMechanics` 至少进入 zone intro / route hint
   - `uniqueContentTag` 至少驱动一个真实行为：interactable 生成、elite injection、cache bias、reinforcement 或等价机制
7. 这一步不追求 Phase 4 级复杂机制，但也不能只补提示文案而没有行为挂钩。

### 4.3 [W7c] Gate Hardening

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt
game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt
build/reports/harness/long-run-full.md
build/reports/harness/boss-harness.md
```

冻结口径：

1. `LongRunLabFullTest` 必须改为 branch-inclusive matrix，至少覆盖：
   - mainline
   - `bandit_camp` 分支
   - `elven_ruins` 分支
   - 一条 late optional 分支
2. `afterDeepIronRatio` 在 `nonVictoryCount == 0` 时不能再写成 `1.0`。
3. 零失败场景的正式处理固定为：
   - 报告输出 `N/A`
   - gate 由 branch coverage / route diversity / reach metrics 单独承担
4. `long-run-full` 第一版新增硬门槛：
   - `branchSampleCount >= 4`
   - `routeHashDistribution >= 3`
5. `BossHarnessTest` 的正式 Phase 3 case 必须显式声明 phase-specific action trace 断言。
6. `boss-harness` 对正式 case 的最低门槛固定为：
   - `warning` trace 存在
   - `phase transition` trace 存在
   - `aiTraceCount > 0`
   - 至少一条 phase-specific selected action 被断言
7. 报告字段至少新增：
   - `routeHashDistribution`
   - `branchSampleCount`
   - `afterDeepIronRatio = N/A` 场景标记
   - `per-zone objective completion summary`
   - `requiredAiTraceCount / observedAiTraceCount`

## 5. 推荐改动面

### 5.1 `game`

1. `FoundationGameSession.kt`
2. objective runtime evaluator / zone mechanic helper 新文件
3. `DataLoader.kt`
4. `SchemaModels.kt`

### 5.2 `data`

1. `game/src/main/resources/data/objectives/index.yaml`
2. `game/src/main/resources/data/zones/index.yaml`
3. 必要时新增少量 interactable / trigger / reward bias data

### 5.3 `client`

1. 本 PR 不做完整玩家信息面改造
2. 若 route preview 或 zone intro 需要最小配套，只允许补必要的 hint/snapshot 字段

### 5.4 `tools / QA`

1. `LongRunLabFullTest.kt`
2. `BossHarnessTest.kt`
3. harness report 模型与 markdown 输出

## 6. 测试与自证

### 6.1 必测类

1. `ObjectiveRuntimeEvaluatorTest`
2. `LongRunWorldStructureSessionTest`
3. `ZoneContentCoverageTest`
4. `BossHarnessTest`
5. `LongRunLabFullTest`

### 6.2 推荐自动化命令

```bash
./gradlew :game:test --tests "com.ktome.game.ObjectiveRuntimeEvaluatorTest"
./gradlew :game:test --tests "com.ktome.game.LongRunWorldStructureSessionTest"
./gradlew :game:test --tests "com.ktome.game.data.ZoneContentCoverageTest"
./gradlew :game:bossHarness --tests "*BossHarnessTest"
./gradlew :game:longRunLab --tests "*LongRunLabFullTest"
./gradlew check
```

### 6.3 白盒验证

1. 从 world map 进入 `bandit_camp` 或 `elven_ruins`，确认存在真实 objective/interactable，而不只是普通清图。
2. 进入 `underground_river` 与 `abyssal_temple`，确认能观察到对应 mechanic hook，而不是只看到名字不同的普通战斗。
3. 触发一场 Phase 3 Boss phase 切换，确认 harness 报告同时记录 `warning / phase transition / AI action trace`。

## 7. 出口门禁

本 PR 合并前必须满足：

1. `completionRule` 已进入 runtime，不再是死字段。
2. `recordObjectiveProgress()` 已写回 quest state。
3. zone clear 不再无条件完成整条 quest。
4. 四个 optional zone 全部具备最小 runtime 独有内容。
5. `underground_river / abyssal_temple` 至少各有一个真实 objective/interactable。
6. `environmentTheme / specialMechanics / uniqueContentTag` 至少各有一个 runtime 消费点。
7. `longRunLabFullTest` 不再依赖单一路线与 `afterDeepIronRatio=1.0` 假阳性。
8. `bossHarness` 不再接受 `aiTraceCount=0` 的正式样本。
9. `./gradlew check` 仍保持绿色。

## 8. 风险与止损

### 8.1 风险

1. objective 真正落地后，bot 可能因为新 interactable 与路径切换出现新卡点。
2. optional / late-zone hook 增加后，`headlessTurnEquivalent` 可能短期上升。
3. gate 收紧后，当前看似稳定的 green build 可能转红。

### 8.2 止损策略

1. runtime objective 第一版只支持当前三类已存在 rule，不扩成通用 DSL。
2. optional zone 先做“最小可感知机制”，不引入 Phase 4 级复杂系统。
3. gate 收紧必须与报告字段扩展同步，避免只知道失败、不知道失败原因。
4. 若 `abyssal_heart` pre-boss hook 会明显拖慢本 PR 节奏，允许显式延后，不影响本 PR 主门禁。
