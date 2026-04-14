> 对照文档：`docs/opt/pr/2026-04-12-unified-verification-pr-06-phase4-gate-cutover-alias-stabilization-and-legacy-fallback-hardening.md`
>
> 审查分支：`codex/unified-verification-pr-06-phase4-gate-cutover-alias-stabilization-and-legacy-fallback-hardening`
>
> 审查日期：`2026-04-14`
>
> 审查视角：**资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人**

# 2026-04-14 Unified Verification PR-06 深度审查报告

## 1. 直接结论

1. **整体对齐度：高**（约 `95%`）。PR-06 的核心目标（`Phase 4` gate cutover、对外 alias 冻结、legacy 链路降级为手工 fallback、对账闭环）均已在 root / tools 构建脚本、`ReportPhase4Runner`、checklist/roadmap/架构文档与合同测试中落地。
2. **唯一的阻塞性偏差**是 `tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt:464` 的缩进事故（`repoRoot()` 函数外层缺失缩进，但函数体保留 `8` 空格缩进），属于 cutover 过程中的低级格式问题，必须在合入前修掉。
3. **两个中等偏差**：`content-pack` 域的 `ownerTaskPaths` 里多塞了 `:tools:whiteBoxContentPack`（严格意义上超出了 PR-06 "只切 gate、不改 domain 行为" 的约束）、合入证据链缺失（未随 PR 附带一次 `reportPhase4` 对账 artifact 快照）。
4. **无高风险玩法/内容回归**。本次 cutover 只动验证合同与报告聚合入口，未触及 `Phase 4` 主题的秘籍奖励身份、地形突变、boss/elite、organic hidden、terminal build 等核心体验 owner metric；所有 owner domain 仍命中同一组 baseline，`reportPhase4` parity 若失败 `require` 会立即阻断合入。

可以按 "修缩进 → 本地跑一次 `phase4LegacyReport + reportPhase4` 收一次对账 artifact → 决策是否接受 `whiteBoxContentPack` 扩边 → 合入" 的顺序推进。

---

## 2. 对照 PR-06 冻结合同的逐项核查

| PR-06 合同 | 规范要求 | 当前实现 | 结论 |
| --- | --- | --- | --- |
| 对外 alias 默认保留 (`contractLint` / `localeLint` / `lootBalanceLab` / `whiteBoxLoot` / `phase4Report` / `phase4ReportOnly`) | 六个 root alias 名称不变 | `build.gradle.kts` 对应 root `tasks.register` 均保留；`phase4Report` / `phase4ReportOnly` 的名字未动，仅把底层依赖切到新 canonical `:tools:phase4Report` / `:tools:phase4ReportOnly` | **对齐** |
| `phase4Report` 必须是 artifact-only 聚合 | 不触发 producer、不依赖上游 owner task | `tools/build.gradle.kts` 的 `phase4Report` / `phase4ReportOnly` 均只声明 `includeTags("reportPhase4")` + `excludeTags("phase4AggregationInput")`，无 `dependsOn`；`ReportPhase4BuildContractTest` 第二个用例专门锁死 "`phase4Report` task ... does not depend on producer tasks" | **对齐** |
| fallback 有明确退出条件，不无限期共存 | 架构文档给出 "两轮稳定回归通过 / 关键 metric 对账一致 / 文档切新路径" | `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md:1510` 明确写出三条退出条件；`phase4LegacyReport*` 被标记为 "只保留为手工 fallback，不进入默认 gate、默认 impact routing 或默认开发回路" | **对齐** |
| 新旧关键 owner metric 对账 | 对账在 `reportPhase4` 内做、失败必阻断 | `ReportPhase4Runner.compareAgainstLegacy()` 对 `sourceTaskId / currentValue / currentValueText / target / status` 四字段逐项比对，并在 `run()` 内以 `require(mismatchCount == 0)` 阻断；`reportPhase4` 的 Gradle task 显式注入 `compareLegacy=true` | **对齐** |
| `phase4Report` 对外语义切到新 contract | root `phase4Report` → `:tools:phase4Report`（canonical unified） | `:tools:phase4Report` 切到 `reports/verification/phase4/report-phase4-summary.{json,md}`、`compareLegacy=false`；legacy 降级为 `:tools:phase4LegacyReport*` → `reports/phase4/phase4-summary.{json,md}` | **对齐** |
| 旧链路作为 fallback 独立暴露 | 旧聚合必须仍能手工跑 | root 增加 `phase4LegacyReport` / `phase4LegacyReportOnly` 两个 alias；`Phase4ReportRunnerTest` 重新打上 `@Tag("phase4LegacyReport")`；`phase4LegacyReport*` 独占旧标签、不串进 `reportPhase4` tag 集合 | **对齐** |
| `verifyOwner` 作为后续 `Phase 4` 的联合验收入口 | 路由全部已迁 owner | root `verifyOwnerTaskPaths` 枚举了 `12` 个 owner task；`VerifyChangedBuildContractTest` 新增 "root verifyOwner task paths stay aligned with routed phase4 owner domains" 用 registry 做真实对账 | **对齐（但见 §3.2 细则）** |
| checklist / roadmap / 架构文档同步 | 文档不再误导到旧路径 | `docs/phase4/2026-03-13-phase4-verification-checklist.md`、`docs/phase4/roadmap.md`、`docs/opt/...-execution-architecture.md`、`docs/2026-04-04-unified-white-box-verification-framework.md`、`docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` 均显式把 canonical 产物路径改写为 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`，并把 `phase4-summary.{json,md}` 标注为手工 fallback 历史证据 | **对齐** |
| v2opt 后续 PR 计划改写为 `verifyOwner + phase4Report` 口径 | 不再把 legacy `phase4-summary` 写成默认产物 | `docs/review/phase4/v2opt/README.md`、`docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md` 以及 PR-03/04/05 三份 follow-up 均在开头增加 "PR-06 之后的统一验证约束" 一节 | **对齐** |
| 新增 authority doc 一致性测试 | — | `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4AuthorityDocConsistencyTest.kt` 检查 white-box framework 与 procgen loot 两份权威文档同时包含新 canonical 路径与 `phase4LegacyReport` 字符串，防止后续改 doc 时丢口径 | **加分项** |

---

## 3. 偏差、偏差量估算与优化建议

### 3.1 偏差 A（阻塞）：`ReportPhase4Runner.kt:464` 缩进事故

- **现象**：

  ```
  462:     }
  463:
  464: private fun repoRoot(): Path {
  465:         val configured = System.getProperty("ktome.repo.root")
  466:         return if (configured.isNullOrBlank()) Path.of(".").toAbsolutePath().normalize() else Path.of(configured).toAbsolutePath().normalize()
  467:     }
  468: }
  ```

  `private fun repoRoot()` 外层缩进是 `0` 空格，函数体却使用 `8` 空格、闭合 `}` 又回到 `4` 空格。它在语法上仍然被上层 `object` 吞掉、能正常编译和通过 `compareLegacy=true` 的 parity gate，但视觉上属于典型的 "cutover 过程中留下的半成品重构"，任何 ktlint / spotless / detekt 风格检查都会挂。

- **偏差量**：`1` 处代码格式错误、`0` 处功能回归。属于**低风险、阻塞评审**级别。

- **修复建议**：

  - 把 `reportDir()` / `legacyReportDir()` / `repoRoot()` 三个 helper 统一为 `4` 空格外层缩进、`8` 空格函数体；或者顺手把 `repoRoot()` 提成文件级 `private` 扩展，与 `stringValue` / `jsonObjectArray` / `formatRate` 同层。
  - 附上 `./gradlew :tools:ktlintCheck`（若启用）或 `./gradlew :tools:spotlessCheck` 结果，证明不再有格式告警。

### 3.2 偏差 B（中等）：`content-pack` 域 ownerTaskPaths 扩充超出 PR-06 边界

- **现象**：`VerificationTaskRegistry.contentPackDomain.ownerTaskPaths` 从单元素 `[":tools:contentPackHarness"]` 扩为 `[":tools:contentPackHarness", ":tools:whiteBoxContentPack"]`，并在 root `verifyOwnerTaskPaths` 与 `verifyChangedTaskPaths` 中同步落地；配套改了 `ScopeCoverageLintTest`、`VerificationDomainSpecTest`、`VerificationImpactAnalyzerTest`。

- **与 PR-06 的冲突**：PR-06 §8 的 Gotcha 2 明确 "不要在 cutover PR 里继续大改 domain 行为——只切任务与 gate"。把 `whiteBoxContentPack` 接进 owner set 虽然在 spirit 上是为了让 `verifyOwner` 真正覆盖新口径下 content-pack 域的双 owner（harness + white-box 评估），但它改变了 impact routing 的 requestedTaskPaths、以及 `verifyOwner` 的实际执行面积，属于 **domain 行为变更**。

- **偏差量**：`1` 个域被扩 owner、`1` 处 impact plan 口径被扩、对 `verifyChanged` 的命中集合有净增；估算 `M` 量级但未超 PR-06 的 `2~4` 人日工作量。

- **建议**：

  - **推荐做法**：保留现状，但在 PR 描述与本地评审记录里显式备注 "该扩 owner 仅为了把 `whiteBoxContentPack` 纳入 `verifyOwner` 的联合验收面积、不改变 content-pack domain 的 kernel / evaluation / baseline，与 cutover 合并进入同一 PR 以避免 `verifyOwner` 验收面积漏项"；
  - 或者退而求其次：**从 PR-06 拆出**一个名义上的 `PR-06.1 content-pack owner set alignment` 小跟进，降低 cutover PR 的 blast radius；
  - 并在 `2026-04-12-repository-wide-...-architecture.md` 的 "Phase 4 迁移到 owner gate 映射表" 中一并把 content-pack 行新增第二个 owner task，保证文档与 registry 面积一致（当前行仍是单 owner 表述，**会与 registry 产生轻微不一致**，建议补）。

### 3.3 偏差 C（中等）：缺合入证据链

- **现象**：PR-06 §4.2 定义 cutover 前置为 "新旧关键 owner metric 对账一致"，§7.2 要求必测 "新旧 metric 对账一致"。当前 diff 里没有随 PR 附带一次真实的 `reportPhase4` 对账 artifact（`report-phase4-legacy-comparison.json`）或 parity 通过截图。

- **影响**：`reportPhase4` 自己有 `require(mismatchCount == 0)`，所以 CI / 合入一刻会再算一次 parity；但合入决策者无法从 PR 本身直接看到 "此次 cutover 之前，legacy vs canonical 关键 metric 已为 `0` mismatch 的证据"。一旦之后某个 follow-up 改动让 parity 失败，很难追溯是 PR-06 合入就带入的问题、还是 follow-up 引入的回归。

- **偏差量**：`0` 代码行，但属于规范要求的**证据链缺失**。

- **建议**：

  - 在 `docs/review/phase4/opt/baselines/` 或一个 `docs/review/phase4/opt/PR/` 子目录下提交一份 `2026-04-14-pr06-cutover-parity-snapshot.md`（或 `.json`），内容为本地或 CI 运行：
    ```
    ./gradlew phase4LegacyReport
    ./gradlew reportPhase4   # compareLegacy=true
    ```
    得到的 `report-phase4-legacy-comparison.json` 摘要（`metricCount`、`mismatchCount`、关键 owner metric 列表）。
  - 把该快照路径在 PR 描述里明写、供后续回滚策略 §9 的 "关键 metric 对账异常立刻回退 alias 指向旧任务" 提供证据锚点。

### 3.4 偏差 D（轻微）：`phase4Report` 对外语义解释可更明确

- **现象**：PR-06 §1 完成标准写作 "`phase4Report` 对外语义切到新 `reportPhase4`"。现实做法是：
  - root `phase4Report` 不直接指向 `:tools:reportPhase4`，而是指向 `:tools:phase4Report`（即底层 task 也仍叫 `phase4Report`，只是换了产物目录、换了 tag、换了 compareLegacy 语义）；
  - `:tools:reportPhase4` 被定位为 **parity 对账入口**，只在 verification contract / baseline / aggregation schema 改动时显式调用。

- **评价**：这种分工其实比直接把 `phase4Report` 别名到 `:tools:reportPhase4` 更清晰：**"日常 artifact-only canonical aggregate" 和 "parity 对账 gate" 是两件事**，不应共用同一个 root 语义。架构文档与 v2opt 计划里都已写明这层口径，所以**符合 PR-06 的意图但不符合字面含义**。

- **偏差量**：`0` 实现偏差；**纯文档措辞问题**。

- **建议**：在 `docs/opt/pr/2026-04-12-unified-verification-pr-06-...md` 的完成标准 1 末尾补一句 "`phase4Report` 指向 canonical unified aggregate、`reportPhase4` 作为显式 parity 对账入口" 消除与 §4.1 内部映射表 "内部允许映射到 `verify...` / `reportPhase4`" 的潜在歧义。不改也行，但会省一次将来的 RFC 阅读成本。

### 3.5 偏差 E（轻微）：`phase4LegacyReportOnly` 与 `phase4LegacyReport` 配置 100% 重复

- **现象**：`tools/build.gradle.kts` 中两个 task 的 body 完全相同（同 tag、同 reportDir、同 outputs）。在后续一旦 legacy 产物或标签需要调整，需两处同步。

- **偏差量**：`1` 段 `~15` 行的重复，维护性隐患、不会出功能回归。

- **建议**：提成一个局部 lambda 或 `listOf("phase4LegacyReport", "phase4LegacyReportOnly").forEach { name -> tasks.register<Test>(name) { ... } }`；但考虑到 `phase4Report` / `phase4ReportOnly`、`reportPhase4` / `reportPhase4Only` 都是相似写法，统一重构可以放到 `PR-06.2`（或直接进 Phase 5 `reportPhase5` 实现时一起做），不阻塞本次 cutover。

---

## 4. 玩法 / 体验侧专项确认

作为设计总监视角，我更关心 cutover 是否会让 `Phase 4` 目前已收口的关键体验 owner metric 被静默放过。逐一核查：

1. **秘籍奖励身份（loot local reward identity）**
   - 对应 baseline：`docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json`
   - 进入新 canonical aggregate 的入口：`loot.owner-evaluation` → `reportPhase4Only`
   - `ReportPhase4Runner.buildOwnerMetrics` 要求所有 `Phase4MetricCatalog.specs` 的 metricId 都必须在 evaluation results 中找到（`checkNotNull`），保证 cutover 之后不会丢失该类指标；legacy parity `sameZoneSecretVsCadenceMaxOverlap / sameZoneSecretVsRewardMaxOverlap / localIdentityFailurePairs` 在 `Phase4ReportRunnerTest` 仍被断言。
   - 结论：**不放过**。

2. **scripted vs organic hidden / 隐藏内容双路径**
   - scripted baseline：`2026-04-12-phase4-scripted-hidden-owner-baseline.json`；organic baseline：`2026-04-12-phase4-organic-hidden-owner-baseline.json`
   - `hidden.owner-evaluation` / `organic-hidden.owner-evaluation` 两个 InputScope 都把 baseline 路径与 `reportPhase4Only` 绑死，cutover 后默认 gate 命中这两条路径。
   - 结论：**不放过**。

3. **terminal build identity**
   - baseline：`2026-04-12-phase4-terminal-build-identity-baseline.json`；owner：`longrunDomain.longrun.owner-evaluation` → `reportPhase4Only`。
   - 结论：**不放过**。

4. **terrain combat sample contract / terrain mutation**
   - baseline：`2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json` + `2026-04-12-phase4-terrain-per-zone-lower-bound-baseline.json`；`terrainDomain.terrain.owner-evaluation` 路径打包进 `reportPhase4Only`。
   - 结论：**不放过**，且 `RELATIVE_BASELINE` 模式仍强约束阈值。

5. **boss / elite variant、content-pack runtime**
   - `bossDomain.ownerTaskPaths = [":tools:bossHarness"]`、`contentPackDomain.ownerTaskPaths = [":tools:contentPackHarness", ":tools:whiteBoxContentPack"]`，均命中 `verifyOwner`。
   - 结论：**不放过**；content-pack 甚至被加强成双 owner 覆盖（见 §3.2 的偏差提示）。

6. **locale / i18n 风险**
   - `contractLint.staticGraph` 同时守 `schema-i18n.locale`（`game/src/main/resources/i18n/`）和 `schema-i18n.schema`（owner 必需）。cutover 没有动这块。
   - 结论：**不放过**。

结论：本次 cutover **不存在玩法或体验 owner metric 被绕开的结构性风险**，所有 Phase 4 已冻结的 "hidden / loot / terrain / terminal identity" 四条主 metric 都在新 canonical aggregate 的路由范围内。

---

## 5. 可执行的下一步（建议合入前执行）

1. **修掉 §3.1 缩进事故**
   - 文件：`tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt`
   - 动作：统一 `reportDir()` / `legacyReportDir()` / `repoRoot()` 的缩进；或提升 `repoRoot()` 到文件级 `private` helper；
   - 验证：`./gradlew :tools:compileKotlin :tools:test --tests com.ktome.tools.phase4.ReportPhase4BuildContractTest`，并运行仓库现有的 ktlint/spotless 任务（如存在）。

2. **补 §3.3 对账证据快照**
   - 本地跑：`./gradlew phase4LegacyReport` → `./gradlew reportPhase4`；
   - 产物：`tools/build/reports/verification/phase4/report-phase4-legacy-comparison.json`（parity）；
   - 把摘要以 Markdown 快照形式入库（例如 `docs/review/phase4/opt/PR/2026-04-14-pr06-cutover-parity-snapshot.md`）。

3. **决策 §3.2 的 domain 扩 owner**
   - 选项 A：接受当前合并方案，补上架构文档 "Phase 4 迁移到 owner gate 映射表"里 content-pack 的第二个 owner 行；
   - 选项 B：从本次 PR 拆出 `whiteBoxContentPack` 入 owner 的改动，放到独立 `PR-06.1`；
   - 推荐 A（它与 `verifyOwner` 的落地是同一目标）。

4. **跑 PR-06 §7.1 规定的 4 条测试命令**
   - 顺序：`./gradlew phase4Report` → `./gradlew phase4ReportOnly` → `./gradlew verifyChanged` → `./gradlew verifyOwner`
   - 期望：全部 PASS；`phase4Report` 只产出 `report-phase4-summary.{json,md}`、不触发任何 owner task；`verifyOwner` 命中 `12` 个 owner task。

5. **（可选）§3.5 去重**
   - 若时间允许，把 `phase4Report / phase4ReportOnly / phase4LegacyReport / phase4LegacyReportOnly / reportPhase4 / reportPhase4Only` 的重复 body 统一到一个 `registerPhase4ReportTask(name, ...)` helper，降低未来维护成本；否则放到 Phase 5 `reportPhase5` 落地时一起收口。

---

## 6. 风险与回滚对齐

- **回滚路径**：按 PR-06 §9，一旦 `reportPhase4` parity 出现任何 mismatch，root `phase4Report` 可直接改回 `dependsOn(":tools:phase4LegacyReport")`、canonical 目录产物保留留档。当前实现允许这种回滚，因为 legacy task 已独立存在、独立 tag、独立产物目录，不依赖 canonical 代码路径。
- **长期风险**：`phase4LegacyReport*` 永久存在会导致双轨维护。建议把 "§4.3 fallback 退出条件的 `2` 轮回归" 作为一个明确 checklist 项目落在 `docs/phase4/roadmap.md` 或 `docs/opt/2026-04-12-repository-wide-...-architecture.md` 的 Sprint 4 验收之后，由 Phase 5 开启前一轮专项清理任务删掉 legacy 聚合（而不是留给每个 follow-up PR 自行判断）。

---

## 7. 审查总评

- **一致性**：PR-06 的 cutover 合同、alias 表、fallback 策略、对账口径在代码与文档之间做到了**高度对齐**，且用合同测试（`ReportPhase4BuildContractTest`、`VerifyChangedBuildContractTest`、`Phase4AuthorityDocConsistencyTest`、`VerificationImpactAnalyzerTest`、`ScopeCoverageLintTest`、`VerificationCliTest`、`VerificationDomainSpecTest`）多点夹住，回归成本低。
- **风险控制**：没有触碰玩法 owner metric 的计算逻辑、baseline 内容或评估阈值；`reportPhase4` 的 `require(mismatchCount == 0)` 把 parity 做成硬 gate 而非观测指标。
- **工程质量**：除 §3.1 的缩进事故外，没有显著的 code smell 或架构边界越权；文档语言和原始 PR-06 规范一致、未引入新的术语漂移。
- **建议合入等级**：修缩进 + 补对账证据快照后，**可以合入**。若接受 §3.2 扩 owner 的 scope，`2026-04-12-repository-wide-...-architecture.md` 的 owner map 需顺手同步。

---

## 附录 A：关键代码与文档引用位置

- `build.gradle.kts:63-77` — `verifyOwnerTaskPaths` 定义
- `build.gradle.kts:89` — `verifyChangedTaskPaths` 追加 `:tools:whiteBoxContentPack`
- `build.gradle.kts:378-419` — `phase4LegacyReport{,Only} / phase4Report{,Only} / reportPhase4{,Only} / verifyOwner` 六个 alias
- `tools/build.gradle.kts:657-741` — `legacyPhase4ReportDir` / `unifiedPhase4ReportDir` 与对应 Test task
- `tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt:102-198` — canonical aggregate + parity 主流程
- `tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt:464` — **缩进事故**
- `tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt:330` — content-pack 域扩 owner
- `tools/src/test/kotlin/com/ktome/tools/phase4/ReportPhase4BuildContractTest.kt:50-65` — `phase4LegacyReport` 手工 fallback 合同
- `tools/src/test/kotlin/com/ktome/tools/phase4/ReportPhase4RunnerTest.kt:77-101` — canonical 运行清理陈旧 parity artifact
- `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4AuthorityDocConsistencyTest.kt` — 权威文档一致性
- `tools/src/test/kotlin/com/ktome/tools/verification/VerifyChangedBuildContractTest.kt:29-41` — `verifyOwnerTaskPaths` 与 registry 对账
- `docs/opt/2026-04-12-repository-wide-unified-verification-contract-and-execution-architecture.md:1216` / `1233-1246` / `1455-1480` — canonical 产物路径、alias 表、Sprint 4 验收
- `docs/phase4/2026-03-13-phase4-verification-checklist.md:23-39` — checklist 命令列表与新口径
- `docs/phase4/roadmap.md:52` — 默认 canonical gate 语义
