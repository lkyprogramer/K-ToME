# Critical-Path Pacing Contract

## 1. 直接结论

Phase 4 的 critical-path pacing 已经收口为一套 shared evaluator 合同：

1. shared evaluation authority: `CriticalPathPacingEvaluator`
2. canonical authority: `ReportPhase4Runner`
3. legacy projection: `Phase4ReportRunner`

任何 consumer 都不得再从 long-run raw metrics 二次推导 pacing verdict。

## 2. 核心模型

共享模型固定为：

1. `CriticalPathPacingThresholds`
2. `CriticalPathPacingEvidence`
3. `CriticalPathPacingMetricResult`
4. `CriticalPathPacingEvaluation`
5. `CriticalPathZoneDesignAuditSnapshot`

其中：

1. `Thresholds` 只从 baseline 读取门槛
2. `Evidence` 只承载共享证据，不承载二次 verdict
3. `Evaluation` 是 canonical 与 legacy 的唯一投影来源

## 3. Null 语义硬合同

`avgObjectiveAcquireTurn == null` 的语义固定为：

1. 缺失 objective sample
2. 该 zone 进入 `failingZones`
3. `sampleMissing = true`
4. 对应 metric 判为 `UNEXPECTED_REGRESSION`

禁止：

1. `?: 0.0`
2. `Double.NaN` fallback
3. 各 runner 各自解释 null

## 4. Canonical Summary Contract

canonical summary 当前 schema 固定为 `report-phase4-v2`。

critical-path pacing 的共享证据只保留一份：

```json
{
  "sections": {
    "criticalPathPacing": {
      "criticalPathZoneIds": [],
      "zoneSnapshots": [],
      "zoneBreakdown": {},
      "designAudit": [],
      "sampleMissingZoneIds": []
    }
  }
}
```

四个 pacing owner metric 只允许通过 `details` 指向这份 section：

1. `sectionRef = "criticalPathPacing"`
2. `metricKind = minimum | ratio`
3. `zoneFailures`
4. `sampleMissing`
5. `designAudit` 只允许通过 shared pacing projection 的 additive details 透传；render 侧禁止再次从 raw `longRunLab.metrics.criticalPathZoneDesignAudit` 解析

## 5. Legacy Projection Contract

legacy report 不再自己重建 pacing verdict。

它只能消费：

1. `CriticalPathPacingEvaluation.toExperienceMetrics(...)`

因此 canonical / legacy parity 的固定比较面仍是：

1. `sourceTaskId`
2. `currentValue`
3. `currentValueText`
4. `target`
5. `status`

`details / sections / schemaVersion` 属于 additive canonical surface，不在 parity 比较面内。

## 6. Design Audit 身份

`criticalPathZoneDesignAudit` 当前被正式定义为：

1. supporting evidence
2. 非 gate 输入
3. canonical 与 legacy markdown 都必须显示
4. canonical summary 的 `sections.criticalPathPacing.designAudit` 必须保留完整 JSON

markdown 列集固定为：

1. `zoneId`
2. `floorCount`
3. `mapSize`
4. `worldRole`
5. `objectiveSetId`
6. `objectiveCompletionRule`
7. `mechanicsWithoutDedicatedRuntimeHook`

改动 design audit 字段值不得影响 pacing verdict。

## 7. 推荐验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
./gradlew :tools:test --tests com.ktome.tools.phase4.CriticalPathPacingEvaluatorTest --tests com.ktome.tools.phase4.CriticalPathPacingEvaluatorLegacyEquivalenceTest --tests com.ktome.tools.phase4.Phase4AggregationInputRunnerTest --tests com.ktome.tools.phase4.ReportPhase4RunnerTest --tests com.ktome.tools.phase4.ReportPhase4MaterializationTest --tests com.ktome.tools.phase4.Phase4ReportRunnerTest
./gradlew phase4Report reportPhase4Only
./gradlew phase4LegacyReportOnly reportPhase4
```
