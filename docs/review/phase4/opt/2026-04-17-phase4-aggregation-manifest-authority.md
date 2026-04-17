# Phase 4 Aggregation Manifest Authority

## 1. 直接结论

`tools/src/main/resources/phase4/aggregation-manifest.yaml` 是当前 Phase 4 producer inventory 的唯一 authoritative source。

它统一定义：

1. `taskId`
2. `taskPath`
3. `artifactRelativePath`
4. `role`
5. report 聚合顺序

当前仓库不再保留 literal producer list，也不再保留 `legacyLiteral` 一类 build wiring fallback。

## 2. 权威边界

manifest 负责：

1. Phase 4 aggregate producer inventory
2. owner / aggregation-only 任务顺序
3. build wiring 与 runtime registry 的共享输入面

manifest 不负责：

1. baseline policy
2. owner metric 阈值
3. report 渲染细节

这些语义分别继续由以下组件负责：

1. `Phase4OwnerBaselineRegistry`
2. `VerificationTaskRegistry`
3. `ReportPhase4Runner` / `Phase4ReportRunner`

## 3. 实现落点

### 3.1 Build wiring

`tools/build.gradle.kts` 通过：

1. `Phase4AggregationManifestValueSource`
2. `Phase4TaskPathResolver`

把 manifest 投影为：

1. `phase4AggregateProducerArtifactRelativePaths`
2. `phase4AggregateProducerTaskPaths`
3. `phase4AggregateProducerInputs`
4. `phase4AggregateProducerTasks`

### 3.2 Runtime registry

`Phase4DomainArtifactRegistry` 通过 `Phase4AggregationManifestRuntime` 遍历同一份 manifest，再把任务交给 `taskReadersById` 读取 artifact。

registry 不再持有第二份 `relativeSourcePath / aggregationOnly` 真相。

## 4. 强约束

manifest 必须同时满足：

1. `taskId` 唯一
2. `taskPath` 唯一
3. `artifactRelativePath` 唯一
4. `taskId == taskPath.substringAfterLast(':')`
5. `taskPath` 只允许 `:tools:*` 或 `:game:*`
6. report 聚合顺序稳定跟随 YAML 中的 `tasks` 顺序

跨组件一致性必须满足：

1. `manifest.ownerTaskIds == VerificationTaskRegistry.phaseOwnerTaskIds("phase4") - Phase4OwnerTaskRoles.NON_AGGREGATED_OWNER_TASK_IDS`
2. `manifest.aggregationOnlyTaskIds == Phase4OwnerTaskRoles.AGGREGATION_ONLY_TASK_IDS`
3. `manifest.taskIds == Phase4DomainArtifactRegistry.registeredTaskIds()`
4. `phase4OwnerBaselineInputs == Phase4OwnerBaselineRegistry.allOwnerBaselinePaths()`

## 5. 恢复与回滚口径

当前不提供 `legacyLiteral` 一类 build wiring 回退开关。

manifest 相关故障的修复路径固定为：

1. 修 `aggregation-manifest.yaml`
2. 修 loader / resolver
3. 修一致性测试
4. 重新 materialize canonical / legacy report

`phase4LegacyReport*` 只保留为 legacy report materialization 的手工 fallback，不承担 inventory fallback。

## 6. 推荐验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
./gradlew :build-logic:test --tests com.ktome.build.verification.Phase4AggregationManifestTest
./gradlew :tools:test --tests com.ktome.tools.phase4.Phase4AggregationManifestDualLoaderParityTest --tests com.ktome.tools.phase4.Phase4AggregationManifestBaselineConsistencyTest --tests com.ktome.tools.phase4.Phase4RegistryConsistencyTest --tests com.ktome.tools.phase4.ReportPhase4BuildContractTest
./gradlew reportPhase4Only phase4LegacyReportOnly reportPhase4
```
