> 执行前必须先完整阅读并接受：
> `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/2026-04-04-unified-white-box-verification-framework.md`

# Phase 4 - OPT PR-01 体验度量基线与 Phase4Report 扩域

**阶段**: `Phase 4 / Post-Review Follow-up / OPT-W1`  
**优先级**: `P0`  
**前置条件**: `Phase 4` 主干验证稳定，`whiteBoxMapgen / whiteBoxSolvability / phase4Report` 可正常运行  
**对应问题**: 当前 `Phase 4` 具备正确性门禁，但 review 里最关键的“体验差异不足”仍缺少统一度量，导致后续 PR 很难证明自己真的提升了重复游玩体验。

---

## 1. 阶段目标

把当前 review 中关于 `loot / hidden / terrain / mutation / special item` 的主观判断，转成可回归、可比较、可被 AI 与人类共同消费的结构化指标。

完成标准：

1. 新增 `10` 个体验指标，并把定义、来源和目标值正式写入 `white-box + phase4Report`。
2. `phase4-summary.json` 与 `phase4-summary.md` 能直接展示这些指标。
3. `bossHarness / terrainInteractionBatch / whiteBoxLoot / whiteBoxHiddenContent` 的 aggregate summary 进入统一 artifact contract。
4. 若现有 trace 不足以支撑指标提取，只允许做 trace 字段补充，不改玩法行为。

## 2. 当前问题

1. `phase4Report` 当前偏向“系统是否正确”，而不是“差异是否成立”。
2. `whiteBoxLoot / whiteBoxHiddenContent / terrain white-box / bossHarness` 之间没有统一体验指标面。
3. review 已经证明“缺口存在”，但没有形成后续 PR 的 before/after 基线。

### 2.1 本 PR 必须冻结的口径

1. `eliteMutationDistinctCount`
2. `eliteMutationValidPairCount`
3. `lootProfileBaseItemOverlapMatrix`
4. `lootProfileDistinctBaseItemCount`
5. `affixPassiveCoverage`
6. `hiddenTriggerTypeCoverage`
7. `secretEntranceBindingCoverage`
8. `terrainTaggedCombatExposureRate`
9. `terrainInteractionEncounterRate`
10. `uniqueArtifactMeaningfulSwapRate`

补充规则：

1. 指标口径一旦进入 `phase4Report`，后续优化 PR 只能在本文档更新阈值，不能各自重新定义。
2. 本 PR 不改变任何内容数据、地图生成、掉落逻辑或客户端表现。
3. 若某项指标依赖 trace 字段补充，只允许新增 trace 元信息，不允许改结算顺序。

## 3. 范围与非目标

### 3.1 范围

1. 扩展 `whiteBoxLoot / whiteBoxHiddenContent / BossHarnessTest / TerrainInteractionBatchTest` 的 aggregate summary。
2. 扩展 `phase4Report` 的 metrics 汇总与 markdown 输出。
3. 必要时补充 `CombatResolutionTrace` 或同级 trace 的只读字段。
4. 补一次 `whiteBoxContentPack / phase4Report` 的 artifact freshness 对齐，避免继续读取历史语义。

### 3.2 非目标

1. 不修改 `items/index.yaml`、`events/index.yaml`、`secret-zones/index.yaml`、`elites/index.yaml`。
2. 不引入新的 harness task；只扩已有 task 的 summary / artifact。
3. 不把 `Phase 5` 的体验模型提前引进 `Phase 4`。

## 4. 技术方案

### 4.1 指标与数据来源

| 指标 | 来源 | 当前基线 | 目标 |
| --- | --- | --- | --- |
| `eliteMutationDistinctCount` | `BossHarnessTest` white-box summary | 6 | ≥12 |
| `eliteMutationValidPairCount` | 同上 | 待首测 | ≥40 |
| `lootProfileBaseItemOverlapMatrix` | `WhiteBoxLootRunner` | 待首测 | 平均重叠 <30% |
| `lootProfileDistinctBaseItemCount` | `WhiteBoxLootRunner` | 25 | ≥35 |
| `affixPassiveCoverage` | `WhiteBoxLootRunner` | 2/5 | ≥80% |
| `hiddenTriggerTypeCoverage` | `WhiteBoxHiddenContentRunner` | 2/6 | ≥4/6 |
| `secretEntranceBindingCoverage` | `WhiteBoxHiddenContentRunner` | 1 | ≥3 |
| `terrainTaggedCombatExposureRate` | `TerrainInteractionBatchTest` | 待首测 | ≥40% |
| `terrainInteractionEncounterRate` | `TerrainInteractionBatchTest` | 待首测 | ≥25% |
| `uniqueArtifactMeaningfulSwapRate` | `WhiteBoxLootRunner` | 近 0 | ≥50% |

### 4.2 White-box / Harness 改造

建议落点：

```text
tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt
tools/src/main/kotlin/com/ktome/tools/hidden/WhiteBoxHiddenContentRunner.kt
tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt
game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt
game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt
```

每个入口的最小改造：

1. `WhiteBoxLootRunner`
   - 输出候选池重叠矩阵
   - 输出 affix passive 覆盖
   - 输出 `uniqueArtifactMeaningfulSwapRate`
2. `WhiteBoxHiddenContentRunner`
   - 输出 triggerType 集合
   - 输出 entranceBindingId 集合
3. `BossHarnessTest`
   - 输出 mutation 数量、有效 pair 数、tier 分布
4. `TerrainInteractionBatchTest`
   - 输出 tagged combat exposure / real trigger rate
5. `Phase4ReportRunner`
   - 透传全部 aggregate metrics
   - markdown 新增“体验度量基线”段落

### 4.3 Trace 补充

若现有 trace 无法支撑 terrain 相关指标，允许在 `CombatPipeline` step 9 的 trace 写入点补充：

1. 参战实体所在格 terrain tag 摘要
2. 本次 combat 是否真的发生 terrain rule trigger

约束：

1. 不改 step 9 的规则逻辑。
2. 不把地图状态作为第二真源拉进 `core`。
3. 仅输出已经在正式路径里计算过的语义结果。

### 4.4 报告合同

本 PR 完成后，以下 artifact 必须可直接被 review 与 AI 消费：

1. `tools/build/reports/phase4/phase4-summary.json`
2. `tools/build/reports/phase4/phase4-summary.md`
3. 对应 white-box summary / cases / artifacts 目录

## 5. 推荐改动面

### 5.1 `tools`

1. 扩 `WhiteBoxLootRunner`
2. 扩 `WhiteBoxHiddenContentRunner`
3. 扩 `Phase4ReportRunner`
4. 保持统一 white-box artifact 合同

### 5.2 `game`

1. 在 `BossHarnessTest` 的 white-box summary 中补 mutation 指标
2. 在 `TerrainInteractionBatchTest` 的 white-box summary 中补 exposure/encounter 指标

### 5.3 `core`

仅在必要时补 trace 字段，不改玩法行为。

## 6. 测试与自证

### 6.1 自动化命令

```bash
./gradlew :core:test
./gradlew whiteBoxLoot
./gradlew whiteBoxHiddenContent
./gradlew bossHarness
./gradlew terrainInteractionBatch
./gradlew whiteBoxVerify
./gradlew phase4Report
```

### 6.2 必测行为

1. 新增指标都能在 summary 中落盘。
2. `phase4-summary.md` 能一次性展示体验基线。
3. 所有新增 assertion rule 都能产出 `PASS/FAIL`。
4. 无 gameplay regression。

## 7. 资源生成计划

### 7.1 图片

本 PR 不新增图片资源。

### 7.2 音频

本 PR 不新增音频资源。

### 7.3 约束

1. 不允许为了形式统一创建空资源 plan。
2. 不允许引入新的 visual/audio key。

## 8. 出口门禁

1. `phase4Report` 已能展示全部体验基线指标。
2. `BossHarnessTest / TerrainInteractionBatchTest / WhiteBoxLootRunner / WhiteBoxHiddenContentRunner` 的 aggregate summary 已统一。
3. 体验问题后续不再以“感觉”讨论，而能以结构化字段对比。
