> 审阅角色：资深 Roguelike / 类 ToME 开发设计总监 + 系统策划总监 + 玩法体验审查负责人
> 审阅范围：commit `823d651e feat: add phase4 sample pack first visibility`（branch `codex/phase4-v4-pr07-sample-pack-first-visibility`）
> 对照规范：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md`
> 出具日期：2026-05-06

# Phase4 v4 PR-07 Sample Pack ADD-first Visibility 实施深度审阅

## 0. 总体结论

实施整体到位，**Acceptance Matrix 6 行全部命中**，sample pack 已经从 REPLACE-heavy 工程证明转为 ADD-first 玩家可见示范，schema v2 切换、pack-local hidden branch binding、fixed-seed visibility case、validation overlay/main menu、fixture 重命名等关键改造都已落地。

但存在 **2 项需要修复的偏差**，其中一项触碰规范 §0 Failure Rule 红线：

| # | 偏差 | 严重度 | 是否触红线 | 推荐处理时机 |
|---|---|---|---|---|
| D1 | `WhiteBoxContentPackRunner` 把 `samplePackContentPlayerVisibilityRate` 当成 blocking 断言阈值 | **中（P2）** | **是**（违反规范 §0 + §5.3 `warn only` 语义） | 本 PR 内部修订或紧随 hotfix |
| D2 | `FoundationGameSession.preparePhase4V4Pr07SecondaryScene` 调用 `removeAllMonstersForValidation`，规范明确写 `killAllMonstersForValidation` | 低（P3） | 否（接口契约口径不一致） | 与 D1 同批修订 |

详见 §3。其余实现与规范一致，详见 §2 Acceptance Matrix 复核。

---

## 1. 复核流程与证据来源

复核遵循 §1 框架（依赖优先级 → 风险评估 → 假设溯因 → 自检），对每个 Acceptance Matrix 行做 spec ↔ 代码双向追溯。涉及证据：

- 规范：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md`
- Owner baseline：`docs/review/phase4/opt/baselines/2026-04-24-phase4-sample-pack-add-first-owner-baseline.json`
- Manifest：`examples/content-packs/sample.flooded_relics/manifest.yaml`、`README.md`
- Sample 数据：`examples/content-packs/sample.flooded_relics/data/secret-zones/**`、`data/events/**`、`data/loot/**`、`data/items/**`
- Runtime 模型：`game/.../contentpack/ContentPackModels.kt`、`ContentPackRuntimeResolver.kt`、`ContentPackValidationSummary.kt`、`game/.../data/DataLoader.kt`
- Validation 接入：`game/.../FoundationGameSession.kt`、`validation/ValidationScenarioRegistry.kt`
- Tools / 白盒：`tools/.../contentpack/ContentPackHarnessRunner.kt`、`WhiteBoxContentPackRunner.kt`
- Fixture：`tools/src/main/resources/fixtures/content-packs/packs/fixture_sample_flooded_relics_override/`、`tools/.../sample.flooded_relics.yaml`
- 测试：`game/src/test/kotlin/com/ktome/game/contentpack/ContentPackRuntimeManifestInventoryTest.kt`、`validation/ValidationScenarioRegistryTest.kt`

---

## 2. Acceptance Matrix 复核结果

| 编号 | 规范来源 | 状态 | 关键证据 |
|---|---|---|---|
| `PR07-M01` | §5.1 manifest ADD-first | ✅ 一致 | `manifest.yaml` 5 条 overlay 全部 `op: ADD`，namespace `sample_flooded_relics`，schemaVersion=2；REPLACE/precedence 已迁出至 `fixture_sample_flooded_relics_override` |
| `PR07-M02` | §5.2 pack-local hidden branch binding | ✅ 一致 | `ContentPackModels.ContentPackHiddenBranchBinding` 字段与表格一致；`ContentPackRuntimeResolver.validateManifestExtensions` 校验 namespace、duplicate zone+slot；`DataLoader.applyContentPackHiddenBranchBindings` 9 条 fail-fast diagnostic 完备 |
| `PR07-M03` | §5.3 player visibility metric | ⚠️ **部分偏差**（见 D1） | metrics 写出 `.reportOnly`、`samplePackTouchedContentIds`、`samplePackAddOnlyMainPath`、`samplePackSecondarySecretSlotUsed`、`samplePackFixedSeedVisibilityCase`，但同名 rate 被复用为 blocking 阈值 |
| `PR07-M04` | §5.4 validation overlay / main menu | ⚠️ **部分偏差**（见 D2） | `ContentPackValidationSummary` 字段全；`preparePhase4V4Pr07PrimaryScene` 输出符合规范返回串；secondary scene 行为正确，但内部调用名 `removeAllMonstersForValidation` ≠ 规范文字 |
| `PR07-M05` | manifest / resource reuse | ✅ 一致 | localeBundles、visualManifest、audioManifest 字段齐备；i18n/visual/audio key 经 `ContentPackValidationSummary` 暴露 resolved/overridden/warning 三组集合 |
| `PR07-M06` | governance inheritance | ✅ 一致 | 规范文头 9 条上游文档引用未删；`ContentPackRuntimeManifestInventoryTest` 强制全仓 manifest schemaVersion 与 `ContentPackManifest.SCHEMA_VERSION` 一致 |

---

## 3. 偏差详情与修复建议

### 3.1 D1 — `samplePackContentPlayerVisibilityRate` 被当作 blocking 阈值（中级 / 触红线）

**规范要求**

- §5.3 表格列定义：`metricKind=reportOnlyOwner`、`failSemantics: warn only`
- §5.3 第 5 条：「warning floor：`100%`；**低于该值只触发 report-only warning，不进入 release-facing blocking gate**」
- §0 Failure Rule（最高优先级红线）：「**不得把 `samplePackContentPlayerVisibilityRate.reportOnly` 当作 blocking 通过证据**」

**实施现状**

`tools/src/main/kotlin/com/ktome/tools/contentpack/WhiteBoxContentPackRunner.kt:147-159` 在 `whiteBoxContentPack` aggregate 里写了一条硬断言：

```kotlin
WhiteBoxAssertionResult(
    ruleId = "content-pack.aggregate.sample_pack_visibility_reported",
    passed =
        kernelRun.analysis.summary.samplePackContentPlayerVisibilityRate >= 1.0 &&  // ← 硬阈值
            kernelRun.analysis.summary.samplePackTouchedContentIds.containsAll(requiredSamplePackTouchedIds) &&
            kernelRun.analysis.summary.samplePackTouchedContentIds.any { contentId ->
                contentId.startsWith("sample.flooded_relics.") &&
                    contentId !in requiredSamplePackTouchedIds
            },
    ...
)
```

由于 `WhiteBoxAssertionResult.passed=false` 进入 `failedAssertions` 列表会让 `whiteBoxContentPack` Gradle 任务报失败，所以 rate < 100% 这条「warn only」语义实际上被升级成了**阻塞 owner gate** 的条件之一。这是规范 §0 红线明文禁止的形态——区别仅在于代码用了去掉 `.reportOnly` 后缀的字段名引用同一个数值。

**风险评估**

- 短期：本 PR 在固定 seed 下 rate 永远等于 1.0，看不出风险。
- 中期：一旦后续 PR-08/PR-09 引入新 sample fixed-seed run 或调整 touch 定义，rate 会先于 schema/binding 出问题之前先把 owner gate 拉红，team 会误把可见度回归当成 schema 故障，违背规范设计的「先修 ADD-first manifest / hidden branch binding / validation scenario」诊断顺序。
- 治理：违反 §0 Failure Rule 的字面契约，也削弱了 `failSemantics` 这套指标分级机制本身的权威性——后续别的 reportOnly 指标会有「也偷偷转成 blocking」的滑坡风险。

**修复方案（推荐 A）**

将 visibility rate 从 aggregate 断言里剥离，断言只保留**真正的硬证据**：

```kotlin
WhiteBoxAssertionResult(
    ruleId = "content-pack.aggregate.sample_pack_visibility_reported",
    passed =
        kernelRun.analysis.summary.samplePackTouchedContentIds.containsAll(requiredSamplePackTouchedIds) &&
            kernelRun.analysis.summary.samplePackTouchedContentIds.any { contentId ->
                contentId.startsWith("sample.flooded_relics.") &&
                    contentId !in requiredSamplePackTouchedIds
            } &&
            kernelRun.analysis.summary.samplePackAddOnlyMainPath &&
            kernelRun.analysis.summary.samplePackSecondarySecretSlotUsed &&
            kernelRun.analysis.summary.samplePackFixedSeedVisibilityCase,
    message = "...",
)
```

理由：

1. `samplePackTouchedContentIds.containsAll(required)` + 「至少一条 generated sample item」已经覆盖了「fixed-seed 必触达」证据，与 rate=100% 的语义等价但表述方式是 supporting/display；
2. `samplePackAddOnlyMainPath` 已由 `manifestLint` 作为 fail-fast gate 兜底（规范 §5.3「ADD-only 主路径由 `manifestLint` 作为 fail-fast gate 负责」），WhiteBox 这里只做「与之 cross-check」即可；
3. rate 字段保留为纯 reportOnly metric 输出（line 98 不动），与 §5.3 的语义层级回到一致。

**修复方案（备选 B，保守）**

如果团队认为 rate 必须留作 gate，则把它移出 `WhiteBoxAssertionResult.passed`，用独立的 `warningAssertions` / `reportOnlyAssertions` 集合承载（需要 framework 侧扩展），并保证它**不进入 `failedAssertions` 计数**。代价是要在 `WhiteBoxAssertionResult` 旁边加一个语义层级字段，工作量比 A 高。

**首选 A**——成本低、与现有指标分级一致、不改 framework。

### 3.2 D2 — Secondary scene 怪物清场调用名与规范不一致（低级）

**规范要求**

§7「实现映射」第 2 条：

> `FoundationGameSession.preparePhase4V4Pr07SecondaryScene` **先执行 `killAllMonstersForValidation`**，再打开 `crystal_cache_chest`，移动到 `sample.flooded_relics.search.flooded_reliquary`，返回 `sample_pack_secret_search_ready; bindingId=sample.flooded_relics.search.flooded_reliquary`。

**实施现状**

`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:2020`：

```kotlin
private fun preparePhase4V4Pr07SecondaryScene(): String {
    ...
    removeAllMonstersForValidation()
    val crystalCachePoint = automationInteractablePoint("crystal_cache_chest") ?: return "sample_pack_crystal_cache_missing"
    ...
}

private fun removeAllMonstersForValidation() { ... }   // line 2032
```

行为等价（destroyEntity 清空当前场景的活体怪物），但**接口契约的字面口径**与规范不一致。

**风险评估**

- 现实危害极低：返回串、副作用、可观测性都符合规范预期。
- 但口径差异会带来两类潜在成本：
  1. 后续 PR 若以规范文档为唯一接口契约（例如 PR-07 follow-up 或外部贡献者读 spec 找钩子），按 `killAllMonstersForValidation` 搜索会落空；
  2. `ValidationScenarioRegistryTest` / 客户端 e2e 若按 spec 检查源码符号（grep 风格），会出现 false negative。

**修复方案**

将私有函数改名为 `killAllMonstersForValidation`：

```kotlin
private fun preparePhase4V4Pr07SecondaryScene(): String {
    ...
    killAllMonstersForValidation()
    ...
}

private fun killAllMonstersForValidation() {
    // 原 removeAllMonstersForValidation 实现
}
```

注意：

1. 仅是 rename，IDE 重构即可；
2. 同步搜索整仓引用（应该只有 secondary scene 一处），避免遗漏；
3. 不需要保留兼容别名（私有函数，无外部依赖）。

---

## 4. 实施做对的部分（值得保留的设计决策）

### 4.1 Schema v2 严格切换 + fail-fast

`ContentPackManifest.SCHEMA_VERSION = 2` + `ContentPackRuntimeResolver.validateSchemaVersion` 直接抛 `content-pack.schema-version.mismatch`，且 `ContentPackRuntimeManifestInventoryTest` 把 `examples/content-packs` 与 `tools/.../packs` 全仓扫一遍。这是规范 §5.2 「不保留 dual-read、legacy alias、字段回填或降级加载」的精确兑现，配 sidecar harness spec 的过滤逻辑也已就绪。

### 4.2 Hidden branch binding 走 manifest data，零 production flag

`secondarySecretSlot=true`、`fixedSeedVisibilityCase=sample_flooded_relics_active_2026042437` 全部通过 manifest YAML 数据驱动，没有在 `HiddenContentMapgenPipeline` 或生产代码加任何 system property / test flag。这条契约规范在 §5.2 第 2/3 条写得很硬，实施忠实落地，对 long-run 下游 PR 的代码可读性贡献很大。

### 4.3 ADD-only 主路径 + REPLACE 隔离

5 个 sample 内容（secret_zone / hidden_event / loot_profile / 2 个 item）全部 ADD，REPLACE/precedence/conflict 整体迁到 `fixture_sample_flooded_relics_override`，`fixture.sample_flooded_relics_override` 旧路径已全仓清扫完毕。这正是「让 pack 作者从官方样例学到正确写法」的体验目标。

### 4.4 玩家可见证据完整闭环

- `ContentPackHarnessRunner.generateSampleRewardTrace` 走真正的 Search + Interact runtime route，而不是 inject id；
- `ValidationScenarioRegistryTest.pr07 scenario actions expose sample pack summary and runtime touched ids` 在 game 层端到端跑通 primary + secondary 两幕，断言 touched ids 命中 secret zone / hidden event / unique item / loot profile；
- `ContentPackValidationSummary` 暴露 resolvedVisualKeys/resolvedAudioKeys/resolvedLocaleKeys + warning 集合；
- `preparePhase4V4Pr07PrimaryScene` 返回串与规范字符串完全一致。

这套链路把 §1 的玩家体验目标（active/no-pack 切换、touched ids、key resolution）从 manifest → mapgen → search/interact → validation summary → client overlay 整链贯通。

### 4.5 测试粒度合理

- ContentPackRuntimeManifestInventoryTest：schema 守门；
- DataLoader / ContentPackRuntimeResolver 单测：binding fail-fast 9 条诊断；
- ValidationScenarioRegistryTest：scenario 定义 + actions 行为 + touched ids 三层断言；
- WhiteBoxContentPackRunner：metrics 输出 + aggregate 断言。

整体测试金字塔形态健康，没有把全部责任压在端到端 white-box 上。

---

## 5. 给后续 PR 的建议（非本 PR 阻塞项）

1. **指标分级机制可显式化**：D1 暴露的根因是 `WhiteBoxAssertionResult` 没有 severity / blocking 字段，所有断言一律默认 blocking。建议下个 cleanup window 引入 `enum class WhiteBoxAssertionLevel { Blocking, Warning, ReportOnly }` 把规范的 failSemantics 直接编码到 framework，避免「字段命名靠后缀 `.reportOnly`，blocking 与否靠 reviewer 人肉记忆」。
2. **规范 ↔ 实现 symbol 一致性 lint**：D2 提示我们可以在 `acceptanceContractLint` 里加一条规则——扫描规范 §7「实现映射」中以反引号包裹的 Kotlin 符号，断言这些符号在源码中存在。这能预防「行为对、名字偏」的悄悄漂移。
3. **Owner baseline 校对**：本 PR baseline 已就位但属于「首批落地」，建议在下一次 long-run 稳定后再做一次 baseline refresh，确认 `samplePackContentPlayerVisibilityRate.reportOnly` 在多 seed 抽样下确实稳定 100%——否则 D1 修复后失去硬阈值保护，需要 owner baseline 来兜底回归探测。

---

## 6. 验证计划（修复 D1 + D2 后）

| 步骤 | 命令 / 操作 | 预期结果 |
|---|---|---|
| 1 | 重命名 `removeAllMonstersForValidation` → `killAllMonstersForValidation`；调整 D1 断言 | 编译通过 |
| 2 | `./gradlew :game:test --tests "com.ktome.game.validation.ValidationScenarioRegistryTest"` | 包含 `pr07 scenario actions expose sample pack summary and runtime touched ids` 在内的全部 PR-07 测试通过 |
| 3 | `./gradlew :game:test --tests "com.ktome.game.contentpack.*"` | binding/inventory 测试通过 |
| 4 | `./gradlew :tools:contentPackHarness :tools:whiteBoxContentPack` | sample fixed-seed 仍 touched ids ⊇ required；rate 仍 1.0；新断言不再依赖 rate 也通过 |
| 5 | `grep -R "removeAllMonstersForValidation" game tools client` | 应无残留 |
| 6 | `grep -R "samplePackContentPlayerVisibilityRate >= " tools` | 应无残留（除非选了备选方案 B 并将其放进 warning 通道） |
| 7 | 复审 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `samplePackContentPlayerVisibilityRate.reportOnly` 字段保留；aggregate 断言列表里 `content-pack.aggregate.sample_pack_visibility_reported` 仍 passed=true |

如果上述 7 步全绿，可视为 PR-07 与规范文档**完全一致**。

---

## 7. 一句话总结

PR-07 的工程实施把 sample pack 从「工程人能读懂的覆盖 demo」升级成了「玩家、pack 作者、CI gate 三方都能 cross-check 的 ADD-first 可见性示范」，唯一需要在合并前抹掉的，是 white-box 那条偷偷把 reportOnly 指标变成 blocking 阈值的旧习惯断言，以及 secondary scene 一个名字写错的私有函数。
