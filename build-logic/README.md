# build-logic

这个目录承载 K-ToME 的仓库级 Gradle 构建基础设施，不属于 `core / game / client / tools` 的运行时规则路径。

当前职责：

1. 提供 `com.ktome.build.verification` 插件。
2. 提供 `VerificationTask`、`VerificationReportTask`、`LegacyHarnessAdapterTask` 基础类型。
3. 为后续 unified verification 重构提供独立宿主，避免把任务基础设施继续堆在根 `build.gradle.kts`。

运行时 verification contract 与 registry 仍在 `tools/src/main/kotlin/com/ktome/tools/verification/`。
