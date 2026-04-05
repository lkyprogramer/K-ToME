# 统一白盒验证框架 深度审查报告

**审查日期**: 2026-04-04  
**审查对象**: `docs/2026-04-04-unified-white-box-verification-framework.md` + 当前主干已落地实现  
**审查角色**: 资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人

---

## 0. 审查总结

| 维度 | 评级 | 说明 |
|------|------|------|
| 设计文档质量 | ★★★★★ | 五层架构清晰，权威关系界定精确，非目标边界明确 |
| 与实现一致性 | ★★★★☆ | 核心合同、报告四件套、领域 runner、Gradle task 全部对齐，少量细节偏差 |
| 架构扩展性 | ★★★★★ | 差异性域 vs 固定场景域的分类、`WhiteBoxCaseRule<T>` 泛型接口设计成熟 |
| 落地务实性 | ★★★★☆ | adapter 策略避免重写现有 harness，但部分设计承诺面向未来的验证成本需关注 |
| Roguelike QA 实用性 | ★★★★☆ | multi-pane overlay 策略解决了信息覆盖问题，AI triage 协议可操作 |

**一句话结论**：这是一份高质量的项目级验证基础设施设计文档，架构决策稳健，实现落地度高。当前主干已完成第一阶段改造（mapgen + solvability 两个 pilot domain），框架核心合同与 Gradle 接线均已就位。以下给出 3 个优化建议和 4 个观察点。

### 0.1 后续核实与落实状态

在本报告形成后，已对实现再次核实并完成以下收口：

1. 早期 code review 中的 "`WhiteBoxCaseRule / WhiteBoxAggregateRule` 只是纸面抽象" 已不再成立。
   - 当前 [WhiteBoxMapgenRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxMapgenRunner.kt) 与 [WhiteBoxSolvabilityRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxSolvabilityRunner.kt) 已通过显式 `caseRules / aggregateRules` 加 [WhiteBoxRuleEvaluator.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/whitebox/WhiteBoxRuleEvaluator.kt) 执行共享 rule contract。
2. 本报告中的优化建议 `S-01 / S-02 / S-03` 已落地：
   - `S-01`：`whiteBoxMapgen` aggregate metrics 新增 hidden entrance 差异维度
   - `S-02`：`summary.json` 顶层新增 AI fast-path 字段 `verdict / failedCaseCount / failedAggregateCount / firstFailedJoinKey`
   - `S-03`：terrain symbol 查找已改为 `TerrainTag` 枚举比较
3. 文档建议 `D-01 / D-02` 已同步到主文档：
   - unified framework 文档新增 solvability artifact 小节
   - `whiteBoxMapgen / whiteBoxSolvability` 当前状态已更新为“已落地并验证通过”

---

## 1. 设计文档逐章审查

### 1.1 §1-3 定位与目标 — 优秀

**亮点**：

- 准确识别了项目当前面临的结构性问题：三套验证路径（自动 harness / 手工白盒 / AI 审查）彼此脱节。
- 非目标界定精确 — "不是对游戏是否好玩的最终判官"、"不是要求所有 harness 立刻重写"。
- §2 的权威关系梳理（phase checklist 管阈值，本文件管框架）避免了权威冲突。

**无偏差。**

### 1.2 §4 当前基线与问题 — 准确

**验证**：文档声称 PR-01 ~ PR-03 已在主干落地，与实际一致。5 个问题的描述（共享层过薄 / 命名偏 phase 专属 / AI 协议缺失 / 人工口径分裂 / 执行入口误用）全部成立。

**无偏差。**

### 1.3 §5 五层架构 — 正确落地

| 层 | 文档定义 | 实现状态 |
|----|----------|----------|
| Phase Gate | `phase4Report` 聚合 | ✅ `Phase4ReportRunner` 已聚合 5 个子 task |
| Root Gradle Alias | `whiteBoxVerify` / `phase4Report` | ✅ `build.gradle.kts` 已注册 |
| Domain Runner | per-domain runner | ✅ `WhiteBoxMapgenRunner` + `WhiteBoxSolvabilityRunner` |
| Shared Framework | contracts + writer + evaluator | ✅ `WhiteBoxContracts.kt` + `WhiteBoxReportWriter` + `WhiteBoxRuleEvaluator` |
| Artifacts/Reports | 四件套 | ✅ summary.json + cases.jsonl + report.md + artifacts/ |

**无偏差。**

### 1.4 §6 核心抽象与合同 — 完全匹配

逐项比对 `WhiteBoxContracts.kt` 与文档 §6 的 Kotlin 代码块：

| 合同 | 文档定义 | 实现 | 一致性 |
|------|----------|------|--------|
| `ContractVersionStamp` | ✅ | ✅ init 校验 | 完全一致 |
| `VerificationReportHeader` | ✅ | ✅ 含 corpusId、contractVersions | 完全一致 |
| `WhiteBoxJoinKey` | seed/zoneId/floorIndex/scenarioId 全 nullable | ✅ 含"至少一个坐标"约束 | 完全一致 |
| `WhiteBoxAssertionResult` | ruleId, passed, severity, message, context | ✅ severity 默认 "ERROR" | 完全一致 |
| `WhiteBoxArtifact` | artifactId, kind, format, relativePath, summary, tags | ✅ | 完全一致 |
| `WhiteBoxCaseReport` | joinKey, facts, fingerprints, assertions, artifacts | ✅ | 完全一致 |
| `WhiteBoxAggregateReport` | groupId, sampleCount, metrics, assertions | ✅ | 完全一致 |
| `WhiteBoxCorpusSpec` | corpusId, description, sampleCount | ✅ | 完全一致 |
| `ArtifactRetentionPolicy` | ALL / FAILURES_PLUS_SAMPLES / SUMMARY_ONLY | ✅ | 完全一致 |
| `WhiteBoxCaseRule<T>` | fun interface | ✅ | 完全一致 |
| `WhiteBoxAggregateRule<T>` | fun interface | ✅ | 完全一致 |

**关键决策 §6.1 合规性**：

| 决策 | 状态 |
|------|------|
| 不用 `Map<String, Any>`，改用 typed DTO 或 JsonObject | ✅ `facts` 和 `metrics` 均为 `JsonObject` |
| VariabilityAnalyzer 不是共享框架核心命名 | ✅ 共享层无此类，差异指标在 domain runner 内部 |
| 共享层不持有领域大对象 | ✅ `GeneratedFloor` 仅在 `WhiteBoxMapgenRunner` 内部使用 |

**§6.2 演进策略**：
- 文档要求通过 adapter 将 `HarnessReportHeader` 映射到 `VerificationReportHeader`
- 实现：`VerificationReportHeaderAdapter.kt` 提供 `HarnessReportHeader.toVerificationReportHeader(corpusId)` 扩展函数
- **完全匹配。**

### 1.5 §7 报告与产物合同 — 一致

**四件套验证**：

| 文件 | 文档要求 | 实现 |
|------|----------|------|
| `whitebox-<domain>-summary.json` | 供 gate、CI、AI 首次读取 | ✅ `WhiteBoxReportWriter.write()` |
| `whitebox-<domain>-cases.jsonl` | 一行一个 case | ✅ JSONL 输出 |
| `whitebox-<domain>-report.md` | 面向人类 reviewer | ✅ `WhiteBoxMarkdownRenderer.render()` |
| `artifacts/` | 证据文件 | ✅ 按 joinKey 组织子目录 |

**§7.1 annotated-maps.txt 定位**：文档明确 "属于领域附加 artifact，不是全局主合同" — 实现中 mapgen 的 ASCII 地图确实作为 artifact 写入 `artifacts/` 子目录（base-map.txt / room-overlay.txt / semantic-overlay.txt / legend.md），而非独立的顶层文件。**一致。**

**§7.2 join key 纪律**：地图域使用 `seed + zoneId + floorIndex` — 实现中 `WhiteBoxMapgenCaseData.joinKey` 恰好如此。**一致。**

### 1.6 §8 域分类 — 设计合理，实现对齐

| 分类 | 文档定义 | 当前实现 |
|------|----------|----------|
| 差异性域 | 5 seed corpus + aggregate 差异指标 | ✅ mapgen runner `SEEDS_PER_FLOOR = 5` + `DIFFERENCE_THRESHOLD = 3` |
| 固定场景域 | 固定 scenario corpus，不强行套 5 seed | 尚无实现（Phase 5），但框架已预留 `scenarioId` 坐标 |

**三步法**（corpus → rule → artifact renderer）在两个已落地 domain 中均有体现。

### 1.7 §9 领域 artifact 设计原则 — mapgen 已达标

文档要求 mapgen 输出四层：base map / room topology 标注 / semantic overlay / legend。

实际实现的 4 个 artifact：

| 文档要求 | 实现 artifact | 状态 |
|----------|---------------|------|
| base map | `base-map.txt` | ✅ |
| room/topology 标注 | `room-overlay.txt` | ✅ |
| semantic overlay (vault/pattern/entrance/terrain) | `semantic-overlay.txt` | ✅ |
| legend 与 structural table | `legend.md` | ✅ |

**文档 §9.1 关键要求**："不允许只有单层覆盖结果，必须保留多 pane 语义" — 实现使用 3 个独立 overlay 文件 + 1 个 legend，**完全满足多 pane 要求**。

### 1.8 §10 AI 使用设计 — 协议已定义

文档定义的 AI 入口顺序（summary → failed case → joinKey → cases.jsonl → artifacts）在报告结构中有自然支撑。triage 流程的 7 步在数据模型上完全可行。

**观察**：AI 使用纪律（§10.3）是文档级约束，不是代码强制。后续可考虑在 report.md 中嵌入 AI 指引注释来强化。

### 1.9 §11 人工一致性 — 策略合理

三层对齐（规则一致性 / 感知一致性 / 校准一致性）+ 四项必须保留的人工环节（package 启动 / UI 可读性 / Boss 战体感 / Accessibility），界定清晰。

### 1.10 §14 Gradle 设计 — 完全落地

| 命令 | 文档定义 | 实现 |
|------|----------|------|
| `./gradlew whiteBoxMapgen` | domain task | ✅ `tools/build.gradle.kts` |
| `./gradlew whiteBoxSolvability` | domain task | ✅ `tools/build.gradle.kts` |
| `./gradlew whiteBoxVerify` | developer convenience | ✅ `build.gradle.kts` root alias |
| `./gradlew phase4Report` | phase gate | ✅ `tools/build.gradle.kts` + root alias |

**excludeTags**：`whiteBoxMapgen` 和 `whiteBoxSolvability` 已加入 root test task 的排除列表。**一致。**

### 1.11 §15 迁移与 rollout — 第一阶段已完成

文档 §15.1 列出的第一阶段内容全部已落地：
- ✅ 共享层最小可用层
- ✅ `MapgenSmokeRunner` 改造为同时支持 smoke + white-box
- ✅ `SolvabilityHarnessRunner` 改造为同时支持 harness + white-box
- ✅ dedicated Gradle task
- ✅ `phase4Report` 第一阶段接线

---

## 2. 优化建议

### S-01（中优先级）：差异类别计算可增加 "入口差异" 维度

**位置**：`WhiteBoxMapgenRunner.kt:281-288`

**当前实现**：
```kotlin
val differenceCategories = listOf(
    topologyDistinct > 1,
    biomeMixDistinct > 1,
    vaultLayoutDistinct > 1,
    terrainDistinct > 1,
    patternCountDistinct > 1,
).count { differs -> differs }
```

**建议**：PR-03 已引入 hidden entrance，但差异类别中没有 "entrance binding 差异" 这一维度。当前 5 个维度（拓扑 / biome / vault / terrain / pattern）已经能轻松满足 `≥ 3` 的阈值，但从 Roguelike replayability 设计角度看，hidden entrance 的有无是玩家非常容易感知的差异。

建议追加：
```kotlin
val entranceDistinct = zoneCases.map { data ->
    data.executedCase.generatedFloor?.entrances?.map { it.bindingId.value }?.sorted() ?: emptyList()
}.distinct().size
```

**影响**：低风险，只影响 aggregate metric 的丰富度，不改变规则真源。

### S-02（低优先级）：考虑给 summary.json 增加 "AI fast-path" 字段

**背景**：§10 定义了 AI 读取协议，但 `summary.json` 目前的结构是通用的（header + corpus + aggregates + assertion counts）。在实际 AI triage 场景中，AI 需要先判断"有没有失败"再决定是否深入读 cases.jsonl。

**建议**：在 summary.json 顶层增加一个 `verdict` 字段：
```json
{
  "verdict": "PASS",
  "failedCaseCount": 0,
  "failedAggregateCount": 0,
  "firstFailedJoinKey": null
}
```

这样 AI 可以在读到第 2 行时就完成 triage 分支判断，不需要遍历整个 aggregates 数组。

**影响**：纯追加字段，不破坏现有合同。

### S-03（低优先级）：terrain symbol 查找应使用枚举比较而非字符串

**位置**：`WhiteBoxMapgenRunner.kt:463-474`

**当前实现**：
```kotlin
private fun terrainSymbol(floor: GeneratedFloor, point: Point): Char? {
    val tags = floor.terrainTags[point].orEmpty()
    return when {
        "ICE" in tags.map { tag -> tag.name } -> '*'
        "OIL" in tags.map { tag -> tag.name } -> 'o'
        "WATER" in tags.map { tag -> tag.name } -> '~'
        else -> null
    }
}
```

**问题**：对每个 point 创建一个临时字符串列表来做 contains 检查。应该直接用 `TerrainTag.ICE in tags` 这样的枚举比较。

**建议**：
```kotlin
private fun terrainSymbol(floor: GeneratedFloor, point: Point): Char? {
    val tags = floor.terrainTags[point].orEmpty()
    return when {
        TerrainTag.ICE in tags -> '*'
        TerrainTag.OIL in tags -> 'o'
        TerrainTag.WATER in tags -> '~'
        else -> null
    }
}
```

**影响**：微小性能优化 + 代码简洁度提升，且避免未来枚举 name 变更导致的隐式失配。

---

## 3. 观察点

### O-01：mapgen runner 复用 MapgenSmokeRunner 的执行上下文而非独立构建

**观察**：`WhiteBoxMapgenRunner.run()` 调用 `MapgenSmokeRunner.loadExecutionContext()` 和 `MapgenSmokeRunner.executeCase()` 来复用 smoke runner 的管线构建和 case 执行逻辑。这意味着 white-box mapgen 依赖 smoke runner 的内部 API。

**评价**：这是一个**务实的工程决策**，避免了在两个 runner 中重复构建 `HybridTopologyMapgenPipeline`。但它也意味着 `MapgenSmokeRunner` 的内部方法（`loadExecutionContext` / `executeCase` / `buildCases`）事实上成了半公开 API。

**建议**：无需立即修改，但如果未来 `MapgenSmokeRunner` 重构内部结构，需同步更新 `WhiteBoxMapgenRunner`。文档 §12.2 的 "先接入统一合同，后统一实现" 策略在这里起到了正确的保护作用。

### O-02：retention policy 当前固定为 ALL

**观察**：`WhiteBoxMapgenRunner` 中 retention policy 硬编码为 `ArtifactRetentionPolicy.ALL`（L209, L239）。对于 40 cases × 4 artifacts = 160 个文件，这是可接受的规模。但文档 §16.2 已经预见了 "产物过大" 风险并建议对大 corpus 使用 `FAILURES_PLUS_SAMPLES`。

**评价**：当前规模无问题。后续 Phase 5 的 perf/soak domain 如果 corpus 达到数百个 case，需要按文档建议切换 retention policy。

### O-03：Phase4ReportRunner 通过磁盘文件读取子 task 结果

**观察**：`Phase4ReportRunner` 读取 `whiteBoxMapgen` / `whiteBoxSolvability` 的 summary.json 文件来提取 metrics，而非 in-process 聚合。这依赖 Gradle task 的 `dependsOn` 保证执行顺序。

**评价**：这是**正确的解耦方式** — 聚合层不应该直接调用 domain runner，否则会引入不必要的耦合。磁盘文件是最稳定的合同边界。

### O-04：solvability domain 的 aggregate rules 设计较 mapgen 更丰富

**观察**：从 exploration 报告看，solvability runner 有更多 aggregate rules（`corpus_reveal_coverage` / `corpus_fail_coverage` / `corpus_backtrack_coverage` / `corpus_critical_path_zero_failures`），这体现了 solvability 作为 "proof-based" domain 的特性 — 它不仅要求 "差异存在"，还要求 "特定证明行为的覆盖率"。

**评价**：这种领域专属的 aggregate rule 设计正是文档 §8 "差异性域 vs 固定场景域" 分类的正确实践。solvability 虽然仍算差异性域，但它的 aggregate 规则更偏向 "coverage proof" 而非 "variability proof"，说明框架的泛型设计确实支持了领域差异。

---

## 4. 文档完善建议

### D-01：§9 的 solvability artifact 设计未像 mapgen 一样展开

**现状**：§9.1 详细列出了 mapgen 的 4 层 artifact 要求，但 solvability 的 artifact 设计在文档中没有专门的 §9.x 小节。实际实现中 solvability 有 4 个 artifact（topology-overview.md / proof-path.md / search-state-table.md / secret-proof-table.md），但这些在文档中只是隐含在 §13 映射表的 "proof DAG、search state、backtrack proof" 中。

**建议**：在 §9 中追加 §9.1.5（或 §9.2 重编号为 §9.3）一小节，明确 solvability 的 artifact 设计原则，与 mapgen 对齐的深度。

### D-02：§13 映射表中 "当前状态" 列可以更精确

**现状**：`whiteBoxMapgen` 和 `whiteBoxSolvability` 的当前状态写的是 "PR-03 后首批改造，已在主干落地第一阶段"。

**建议**：如果这两个 domain 已经通过了完整的验证（即 `./gradlew whiteBoxMapgen` 和 `./gradlew whiteBoxSolvability` 均为绿），建议将状态更新为 "已落地并验证通过"，以区分 "代码已合并但未验证" 和 "代码已合并且验证通过" 两种状态。

---

## 5. 整体结论

这份设计文档达到了**项目级基础设施设计权威**的标准。它的核心价值在于：

1. **明确的权威边界**：checklist 管阈值、本文件管框架、PR 文档不维护第二套框架 — 避免了权威冲突。
2. **务实的演进策略**：adapter 而非重写、先统一合同后统一实现 — 保护了现有投资。
3. **可操作的 AI 协议**：入口顺序 → triage 流程 → 使用纪律 — 不是空话，是可编码的步骤。
4. **清晰的扩展模式**：三步法（corpus → rule → artifact）+ 域分类（差异性 vs 固定场景）— 新 domain 接入有据可循。

当前实现（mapgen + solvability 两个 pilot）与文档高度一致，核心合同完全匹配，无阻塞性偏差。3 个优化建议（S-01 ~ S-03）均为增量改进，可在后续 PR 中逐步收口。
