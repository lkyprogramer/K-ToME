# PR-03 深度审查报告：Baseline 统一、Evaluation 拆分与 Report-Only Phase 4 聚合

**审查日期**: 2026-04-13  
**审查分支**: `codex/unified-verification-pr-03-baseline-schema-eval-split`  
**对照文档**: `docs/opt/pr/2026-04-12-unified-verification-pr-03-baseline-schema-evaluation-split-and-report-only-phase4-aggregation.md`  
**审查角色**: 游戏系统策划总监 + 验证架构审查

---

## 1. 总体评价

PR-03 的核心承诺是"把 kernel 结果 / evaluation baseline 对比 / phase 聚合报告真正拆开"。从实现来看，**主干架构目标基本达成，5 项完成标准中 4 项完全满足、1 项部分满足**。代码质量整体扎实，数据建模严谨，evaluation 拆分的三层结构（`KernelResult` → `EvaluationResult` → `RenderResult`）清晰落地。

但存在 **2 个中等偏差** 和 **3 个低等偏差**，部分违背了文档明确冻结的合同条款，需要修复后才能安全进入 domain 大规模迁移。

---

## 2. 完成标准逐项核验

| # | 完成标准 | 状态 | 偏差等级 |
|---|---------|------|---------|
| 1 | 统一 baseline schema，带 `schemaVersion` | **完全达标** | — |
| 2 | terrain baseline 可迁到新 schema | **完全达标** | — |
| 3 | 5 种 baseline mode 有统一比较器 | **完全达标** | — |
| 4 | `reportPhase4` 可以只读 artifact 重建 | **完全达标** | — |
| 5 | 新旧 `phase4Report` 可并行对账 | **完全达标** | — |

---

## 3. 冻结合同逐项核验

| # | 合同条款 | 状态 | 说明 |
|---|---------|------|------|
| 1 | baseline 文件统一有 `schemaVersion` | ✅ | 全部 7 个 baseline JSON 均含 `schemaVersion: 1` |
| 2 | baseline 更新必须显式走 update task 或迁移脚本 | ✅ | `VerificationBaseline.init` 校验 `schemaVersion`；`read()` 强制要求 JSON 字段存在 |
| 3 | `reportPhase4` 不得直接触发 producer | ✅ | `reportPhase4Only` 无 `dependsOn`；`reportPhase4` 仅 `dependsOn("phase4ReportOnly")` (旧版 artifact-only) |
| 4 | 新旧报告迁移期必须并行对账 | ✅ | `compareAgainstLegacy()` + `require(mismatchCount == 0)` 阻断不一致 |

---

## 4. 技术方案逐项核验

### 4.1 Baseline Schema（文档 §5.1）

**8 个必要字段全部实现**（`VerificationBaselineContracts.kt:21-36`）：

| 字段 | 实现 | 校验 |
|------|------|------|
| `schemaVersion` | ✅ | `init` 块强制 `== VERIFICATION_BASELINE_SCHEMA_VERSION` |
| `baselineId` | ✅ | `init` 块强制非空 |
| `domainId` | ✅ | `init` 块强制非空 |
| `mode` | ✅ | 枚举类型 `BaselineMode` |
| `metricDefinitionVersion` | ✅ | `init` 块强制非空 |
| `approvedDebtKeys` | ✅ | `List<String>` 带辅助方法 `approvedDebtKeySet()` |
| `ceilings` | ✅ | 带 `minValue`/`maxValue` 区间校验 |
| `expectedMetricRanges` | ✅ | 丰富的 range 模型，支持 relative increase/decrease |

**补充约束核验**：

| 约束 | 状态 | 偏差 |
|------|------|------|
| owner metric baseline 外置到 `docs/review/phase4/opt/baselines/` | ✅ | 7 个文件全部就位 |
| `reportPhase4` 不应继续在 Kotlin 里硬编码 owner threshold | ⚠️ **中等偏差** | 见 [§5.1 DEV-01] |

### 4.2 Baseline Mode（文档 §5.2）

5 种模式全部实现于 `BaselineMode.kt` 并在 `VerificationBaselineComparator` 中有对应比较方法：

| Mode | Comparator 方法 | 单测 | Baseline 文件实际使用 |
|------|----------------|------|--------------------|
| `STRICT_ZERO_FAILURE` | `compareStrictZeroFailure()` | ✅ | 用于每个 domain 的 `taskStatus` evaluation |
| `APPROVED_DEBT_SET` | `compareApprovedDebtSet()` | ✅ | 测试覆盖，未被 owner baseline 直接使用 |
| `EXPECTED_FAILURE_CODE_SET` | `compareExpectedFailureCodeSet()` | ✅ | 测试覆盖，未被 owner baseline 直接使用 |
| `RELATIVE_BASELINE` | `compareRelativeBaseline()` | ✅ | `terrain-metrics-baseline-unified.json` |
| `BUDGET_THRESHOLD` | `compareBudgetThreshold()` | ✅ | scripted-hidden, organic-hidden, loot, terminal-build, terrain per-zone |

### 4.3 Terrain Baseline 迁移（文档 §5.3）

| 要求 | 状态 |
|------|------|
| 一次性迁移脚本 | ✅ `scripts/migrate_verification_baseline.py` |
| 保留原 baseline 信息 | ✅ `notes` 字段记录迁移来源 |
| 补齐 `schemaVersion` | ✅ `schemaVersion: 1` |
| 输出新文件路径和迁移日志 | ✅ `*-unified.json` + `*-migration-log.json` |

迁移日志结构合理，记录了 `sourcePath`、`targetPath`、`metricIds` 等关键溯源信息。

### 4.4 Evaluation 拆分（文档 §5.4）

三层概念模型全部落地于 `VerificationExecutionResults.kt`：

| 概念 | 数据类 | 职责 |
|------|-------|------|
| `KernelResult` | ✅ | 承载 domain 源 artifact 的 raw metrics |
| `EvaluationResult` | ✅ | 承载 baseline 比较后的 verdict + entry 明细 |
| `RenderResult` | ✅ | 承载报告产物路径和 artifact 来源跟踪 |

**增量重跑规则核验**：

| 规则 | 实现方式 | 状态 |
|------|---------|------|
| baseline 变化 → 只重跑 evaluation | `buildFingerprints()` 中 `baselineFingerprints` 变化触发 cache invalidation | ✅ |
| report 模板变化 → 只重跑 render | `PHASE4_AGGREGATION_INPUT_CONTRACT_VERSION` 变更触发 invalidation | ✅ |
| kernel 未变化 → 不重跑 producer | `reportPhase4Only` Gradle 无 producer `dependsOn` | ✅ |

`Phase4AggregationInputRunnerTest` 验证了冷/热运行的缓存行为：冷跑 `reusedInputCount=0`，热跑 `reusedInputCount=14`。

### 4.5 `reportPhase4` 聚合（文档 §5.5）

| 要求 | 状态 | 实现位置 |
|------|------|---------|
| 只消费 domain artifact | ✅ | `Phase4ReportRunner.collectTaskAggregates()` 只读文件 |
| 读取 domain summary / evaluation result | ✅ | `Phase4AggregationInputRunner.materialize()` |
| 应用 `Phase4MetricCatalog` | ✅ | `ReportPhase4Runner.buildOwnerMetrics()` |
| 输出 phase summary | ✅ | JSON + Markdown 双格式 |
| 保留旧 `phase4Report` | ✅ | `Phase4ReportRunner` 未修改 |
| 新旧关键 metric 并行对账 | ✅ | `compareAgainstLegacy()` 逐 metric 字段比对 |
| 不一致时阻断替换 | ✅ | `require(mismatchCount == 0)` |

---

## 5. 偏差清单

### DEV-01（中等）：Phase4MetricCatalog 仍硬编码 owner 阈值常量

**文档条款**：§5.1 补充约束 2 —— "`reportPhase4` 不应继续在 Kotlin 代码里硬编码 hidden / loot / long-run / terrain owner threshold"

**现状**：`Phase4MetricCatalog.kt:9-16` 保留了 7 个阈值常量：

```kotlin
internal const val SAME_ZONE_SECRET_CADENCE_MAX_OVERLAP_TARGET: Double = 0.75
internal const val SAME_ZONE_SECRET_REWARD_MAX_OVERLAP_TARGET: Double = 0.75
internal const val TERMINAL_WEAPON_BASE_DIVERSITY_TARGET: Int = 3
internal const val CROSS_PROFESSION_TOP_WEAPON_DOMINANCE_TARGET: Double = 0.50
internal const val PROFESSION_ALIGNED_WEAPON_ADOPTION_TARGET: Double = 0.75
internal const val ORGANIC_HIDDEN_DISCOVERY_RATE_TARGET: Double = 0.30
internal const val SCRIPTED_HIDDEN_VERIFICATION_RATE_TARGET: Double = 1.0
```

这些常量用于 `Phase4MetricSpec.targetText` 的人类可读展示文本。实际的 evaluation 判定已正确走 baseline 文件。但这造成了**阈值来源二元化**：

- **评估判定**：读 baseline JSON（正确）
- **展示文本**：读 Kotlin 常量（有漂移风险）

**偏差程度**：如果有人更新了 baseline 文件的阈值但忘记同步 Kotlin 常量，report 中的 `targetText` 将与 evaluation 实际使用的阈值不一致。虽不影响判定正确性，但对于游戏策划和 QA 审阅 report 时会产生混淆。

**修复方案**：

- **方案 A（推荐）**：`Phase4MetricCatalog.specs` 在构建时接收 baseline registry，从 baseline 文件中读取展示阈值。`targetText` 由 baseline 数据动态生成。
- **方案 B（最小化）**：保留 Kotlin 常量但添加 CI 守卫测试，断言每个常量与对应 baseline 文件中的值一致。不一致时 CI 红灯。

---

### DEV-02（中等）：content-pack 领域缺少外置 baseline 和 evaluation 接入

**文档条款**：
- §7 Task 1 验收："terrain / loot / content-pack 都能表达各自语义"
- §8.2："content-pack expected failure 可表达"

**现状**：
- `VerificationBaselineComparator.compareExpectedFailureCodeSet()` 已实现，且有单测覆盖 ✅
- `Phase4OwnerBaselineRegistry` 未注册任何 content-pack baseline 路径 ❌
- `Phase4AggregationInputRunner.buildAggregationInput()` 的 `when` 分支中无 content-pack ❌
- `docs/review/phase4/opt/baselines/` 下无 content-pack baseline 文件 ❌

**偏差程度**：comparator 层面的"可表达"已满足，但 end-to-end 的"已接入"未完成。content-pack domain 在新聚合管线中仅获得 `taskStatus` 级别的 `STRICT_ZERO_FAILURE` evaluation，其特有的 expected failure 语义没有被 baseline 管理。

**修复方案**：

1. 创建 `2026-04-13-phase4-content-pack-expected-failure-baseline.json`，模式为 `EXPECTED_FAILURE_CODE_SET`
2. 在 `Phase4OwnerBaselineRegistry` 中注册 content-pack baseline 路径
3. 在 `Phase4AggregationInputRunner.buildAggregationInput()` 中添加 `"whiteBoxContentPack"` 或 `"contentPackHarness"` 分支
4. 在 `ReportPhase4RunnerTest` 中添加 content-pack evaluation mode 断言

---

### DEV-03（低）：`ReportAggregationInput` 未按推荐拆到独立文件

**文档条款**：§6.1 推荐 `ReportAggregationInput.kt` 作为独立文件

**现状**：`ReportAggregationInput` data class 定义在 `VerificationExecutionResults.kt:30-36`，与 `KernelResult` 和 `RenderResult` 同文件。

**偏差程度**：极低。三个结构在语义上强相关，同文件放置不影响可维护性。仅为命名规范偏差。

**修复方案**：可接受当前状态，不阻塞合并。如后续 `ReportAggregationInput` 增长复杂度再拆分。

---

### DEV-04（低）：性能目标未显式断言

**文档条款**：§8.3 —— `reportPhase4Only` warm run ≤ 15s，baseline/evaluation-only 重跑 ≤ 10s

**现状**：`Phase4AggregationInputRunnerTest` 验证了缓存行为（冷跑/热跑 reuse 计数），但未断言执行耗时。`ReportPhase4RunnerTest` 也无耗时断言。

**偏差程度**：低。当前的缓存机制（fingerprint 比对 + input artifact 复用）理论上能满足目标，但缺少回归守卫。如果未来某次改动引入了性能退化，现有测试无法捕获。

**修复方案**：在 `Phase4AggregationInputRunnerTest` 的热跑路径中添加 `assertTrue(warmRunDurationMillis <= 15_000)` 断言，或在 CI 中以 JUnit 超时参数兜底。

---

### DEV-05（低）：`Phase4AggregationInputRunner` 对 `Phase4ReportRunner.collectTaskAggregates()` 的运行时耦合

**现状**：`Phase4AggregationInputRunner.materialize()` 在 L95 直接调用 `Phase4ReportRunner.collectTaskAggregates(repoRoot)` 来收集 14 个 domain task 的源 artifact。

**偏差分析**：
- `collectTaskAggregates()` 仅读文件、不触发 producer，满足 artifact-only 合同 ✅
- 但新旧代码共享 `Phase4TaskDescriptor` 列表和 reader 函数，意味着旧 runner 的 task 定义变更会直接影响新 runner
- `collectTaskAggregates()` 已标记为 `internal fun`，限制了泄漏范围 ✅

**修复方案**：可接受当前状态。这是一个合理的渐进式解耦选择，避免了在本 PR 中复制 14 个 task descriptor。当旧 `Phase4ReportRunner` 在后续 PR 退役时，应将 task descriptor 迁移到新的独立 registry。

---

## 6. Baseline 文件完整性审计

| Baseline 文件 | domainId | mode | metricIds | 完整性 |
|--------------|----------|------|-----------|-------|
| `terrain-metrics-baseline-unified.json` | terrain | RELATIVE_BASELINE | terrainTaggedCombatExposureRate, terrainInteractionEncounterRate.aggregate | ✅ |
| `terrain-per-zone-lower-bound-baseline.json` | terrain | BUDGET_THRESHOLD | terrainInteractionEncounterRate.per_zone_lower_bound | ✅ |
| `scripted-hidden-owner-baseline.json` | hidden | BUDGET_THRESHOLD | scriptedHiddenVerificationRate (minValue=1.0) | ✅ |
| `organic-hidden-owner-baseline.json` | organic-hidden | BUDGET_THRESHOLD | organicHiddenDiscoveryRate (minValue=0.3) | ✅ |
| `loot-local-reward-identity-baseline.json` | loot | BUDGET_THRESHOLD | sameZoneSecretVsCadence/RewardMaxOverlap (maxValue=0.75) | ✅ |
| `terminal-build-identity-baseline.json` | longrun | BUDGET_THRESHOLD | diversity≥3, dominance≤0.5, adoption≥0.75 | ✅ |

**审计结论**：6 个新增 baseline 文件结构完整，`schemaVersion` / `baselineId` / `domainId` / `mode` / `metricDefinitionVersion` 五字段无遗漏。阈值与 `Phase4MetricCatalog` 中的 Kotlin 常量一致（但这正是 DEV-01 的风险所在）。

---

## 7. 游戏体验视角审查

从 Roguelike 玩法体验的角度，对 owner metric 评估语义做额外审查：

### 7.1 隐藏内容发现率分离（scripted vs organic）

`Phase4AggregationInputRunner` 将 scripted hidden（正确性守卫）和 organic hidden（体验观测）拆成两个独立 evaluation，分别由两个 baseline 文件管理。这是正确的设计决策：

- **scripted** 使用 `minValue=1.0`（100% 正确率），失败即阻断 → 保护机制正确性
- **organic** 使用 `minValue=0.3`（30% 发现率），失败为体验观测 → 符合渐进式玩法验证

两者不再混为一谈，避免了旧版 `Phase4ReportRunner` 中"scripted 测试通过就代表隐藏内容体验良好"的误判风险。

### 7.2 局部奖励身份（loot local identity）

将 `sameZoneSecretVsCadenceMaxOverlap` 和 `sameZoneSecretVsRewardMaxOverlap` 外置为 `BUDGET_THRESHOLD` baseline 是正确的。这两个指标防止同区域秘密奖励与常规奖励趋同，是 Roguelike 探索激励的核心守卫。

`maxValue=0.75` 与旧版硬编码一致。如果未来需要按区域分化调整阈值，当前 baseline 结构的 `expectedMetricRanges` 数组支持多 metric 扩展。

### 7.3 终局构建多样性（terminal build identity）

三个指标（weapon diversity ≥ 3，dominance ≤ 50%，profession adoption ≥ 75%）的外置化消除了旧版中"long-run 体验指标藏在 500 行 Kotlin 的 `buildExperienceMetrics` 里"的问题。策划和 QA 可以直接编辑 JSON 调整阈值，无需改代码。

### 7.4 地形交互覆盖（terrain interaction）

双 baseline 设计（aggregate RELATIVE_BASELINE + per-zone BUDGET_THRESHOLD）是本 PR 的亮点。aggregate 通过率高但某个区域可能极低的"均值陷阱"，被 per-zone lower bound 兜底捕获。

`terrainPerZoneLowerBoundEvaluation()` 中的 runtime-baseline drift 校验（`require(abs(runtimeLowerBoundTarget - lowerBoundTarget) < 1.0e-9)`）确保了 baseline 文件和运行时配置不会静默漂移。

---

## 8. 回滚策略验证

| 回滚策略 | 实现状态 |
|---------|---------|
| 保留旧 `phase4Report` | ✅ `Phase4ReportRunner` 未被修改或删除 |
| baseline schema 出问题时不切换正式 report | ✅ 新旧 Gradle task 独立注册，旧 `phase4Report` 可独立运行 |
| 新旧 metric 对账不一致时旧 report 继续承担正式 gate | ✅ `require(mismatchCount == 0)` 阻断新 report 替换 |

---

## 9. 修复优先级与建议

| 优先级 | 编号 | 修复项 | 影响范围 | 建议时机 |
|-------|------|--------|---------|---------|
| P1 | DEV-01 | 消除 `Phase4MetricCatalog` 中的阈值常量二元化 | 报告展示可信度 | **本 PR 合并前** |
| P2 | DEV-02 | 补齐 content-pack expected failure baseline 端到端接入 | 文档完成标准一致性 | **本 PR 合并前或紧跟 PR-04** |
| P3 | DEV-04 | 添加性能回归守卫断言 | CI 回归检测 | PR-04 |
| — | DEV-03 | 文件拆分（可选） | 代码规范 | 不阻塞 |
| — | DEV-05 | 旧 runner 退役时迁移 task descriptor | 长期解耦 | 后续 PR |

---

## 10. 结论

PR-03 完成了从"散装 baseline + 硬编码聚合"到"schema 统一 + evaluation 拆分 + artifact-only 聚合"的关键架构转型。**核心合同全部满足，回滚路径完备，测试覆盖充分**。

两个中等偏差（DEV-01 阈值二元化、DEV-02 content-pack 缺口）建议在合并前修复。DEV-01 的修复成本低（方案 B 只需加一个 CI 测试），DEV-02 需要约 0.5 人日的端到端接线。

修复完成后，本 PR 可以安全合入主干，为后续 domain 大规模迁移提供坚实的 baseline 基础设施。
