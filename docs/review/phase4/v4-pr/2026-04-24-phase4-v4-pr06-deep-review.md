# Phase 4 V4 PR06 深度审查报告：长程多样性

- **审查角色**：Roguelike / 类 ToME 开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- **审查对象**：分支 `codex/phase4-v4-pr06-long-run-route-diversity` 当前工作树
- **基准规范**：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md`
- **审查日期**：2026-05-05
- **审查范围**：长程烟雾测试语料、路径哈希算法、`HeadlessRunHarness` 路径校验、报表/基线、`verifyChanged` 路由、域内单测与缓存

> 结论先行：**代码功能与门槛已经达标，可以合规通过 PR06 验收门**；但与规范在「语义」「守卫范围」「字段命名」「测试覆盖度」上仍存在 5 处偏差，建议在合并前/合并后短期内分批修复。下文按「一致项 → 偏差与不足 → 修复优化建议 → 测试与验证补强」展开。

---

## 1. 总体一致性结论

| 维度 | 状态 | 备注 |
| --- | --- | --- |
| 验收矩阵 PR06-M01～M05 | ✅ 已覆盖 | 所有门槛指标在 `LongRunLabTest` / `Phase4V4Pr06RouteDiversityCorpus` 中均有强断言或来源 |
| 语料规模与分布 | ✅ 12 + 4 + 2 + 2 / 4-4-4 起点分布 | `Phase4V4Pr06RouteDiversityCorpus.kt` 与 `LongRunLabSeedBank.kt` 一致 |
| `topHashShare ≤ 40%` | ✅ 实测 4/16 = 25% | 由 `runtimeRoute` 而非 `routeIntent` 决定，仍达标 |
| `branchInclusiveCount ≥ 4`、`fullRouteCount ≥ 12` | ✅ | 基线 `minValue = 4 / 12` |
| `fullRouteIntentDistinctCount == 12` | ✅ | 直接由 `routeIntent` 计算，全部唯一 |
| `topologyCategoryDiversityPerSmokeRun` 仅观察 | ✅ | 基线 `metricId` 后缀 `.reportOnly` |
| Secret 路径标记规则 | ✅ | `RouteHash.kt` 的 `secretRouteMarker(...)` + 拒绝 `:`、`|`、`>`；`HeadlessRunHarness` 通过 runtime `automationVisitedSecretZoneIds` 注入 |
| 16 字符截断 SHA-256 哈希 | ✅ | `RouteHash.zoneRouteHash` |
| Owner 报表与 Markdown 章节 | ✅ | `ReportPhase4Runner.kt` 输出 `routeDiversity` JSON 与 `## Route Diversity` Markdown |
| `verifyChanged` 路由 | ✅（功能性达标） | `VerificationTaskRegistry` `longrunDomain` 覆盖 8 个文件路径与 6 个数据 index |
| 缓存可重放（`LongRunKernelCache`） | ✅ | `routeToken / visitedSecretZoneIds / seedString / primarySecretZoneId / routeIntent` 全部回填 |
| 验证场景登记 | ✅ | `ValidationScenarioRegistry` 与 `ValidationScenarioPresentationCatalog` 均已加 `phase4-v4-pr06` |

> 综合判断：**关键玩法门槛、报表面、域路由、缓存语义均按规范实现**，可以发布；偏差集中在「严谨度」与「长期可维护性」。

---

## 2. 一致项（精选）

1. **路径哈希算法**：`RouteHash.kt` 中 `zoneRouteHash = sha256(routeToken).take(16)`，`secretRouteMarker(id)` 强制 `secret:<id>`，并对 `:`、`|`、`>` 等保留分隔符提前 `require`。与规范 §3.1、§3.3 完全一致。
2. **Secret 标记的 runtime 注入**：`HeadlessRunHarness.recordVisitedSecretZones()` 通过会话事件流 `automationVisitedSecretZoneIds()` 写入 `routeTokenParts`，没有走任何「测试期硬编码」捷径，与规范 §6.1.7 的「runtime 校验」要求一致。
3. **场景类型与终端聚合分离**：`ScenarioModels.zoneRouteHashDiversity()` 在统计前剔除 `ROUTE_PROBE` / `LATE_ROUTE_PROBE`，确保 probe 不污染 topHashShare。`probeRouteHashSample` 单独输出。
4. **缓存指纹**：`LongRunKernelCache` 将 `routeTokenParts` join 后写入指纹，命中时还原 `routeToken / routeIntent / visitedSecretZoneIds / seedString / primarySecretZoneId / probeRouteHash`，与规范 §6.1.6 的「无副本路径，全部回填」目标契合。
5. **基线门槛形式化**：`docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json` 把 `topHashShare`（max=0.4）、`branchInclusiveCount`（min=4）、`fullRouteCount`（min=12）、`topologyCategoryDiversityPerSmokeRun.reportOnly`（min=0.25）四项写成 owner gate，`notes` 字段也明确说明 PR06 复用此基线。
6. **`fullRouteIntentDistinctCount` 强约束**：`LongRunLabTest` 直接断言 `== 12`，等价于「所有 12 个 full_route 的 routeIntent 都不同」，覆盖了规范要求的「文档化路径意图」唯一性。

---

## 3. 偏差与不足

> 严重度按 P1 (阻断/语义错误) → P2 (中度/治理风险) → P3 (低/外观/治理弱化) 划分。

### D1 (P2) `routeIntent` 与 `runtimeRoute` 的语义裂口（中等-高）

**事实**：

- `Phase4V4Pr06RouteDiversityCorpus.kt:21,30-31`

  ```kotlin
  val runtimeRoute = runtimeRouteFromStart(entry.startZoneId)
  ...
  routeIntent = entry.routeIntent,
  routeTokenParts = runtimeRoute,
  ```

  12 条 full-route 中 `routeIntent` 为规范化的 12 种排列，但真正参与 `routeToken` / `zoneRouteHash` 计算的 `routeTokenParts` 却统一退化成「按 startZoneId 推导的 procgen 固定序列」。

- `Phase4V4Pr06RouteDiversityCorpus.kt:282`

  ```kotlin
  val hashDistribution = terminalSpecs.groupingBy { spec -> zoneRouteHash(spec.routeTokenParts) }
  ```

  topHashShare 基于 `routeTokenParts`，而非 `routeIntent`。

**含义**：

- 数值上仍达标：4 个起点 × 4 个意图 → 实际只产生 ≤4 个不同的 `routeTokenParts`，topHashShare = 4/16 = **25% ≤ 40%** 通过。
- 语义上：规范在 §3.2 / §5.4 描述的「12 条不同路径」其实只在 `routeIntent` 文档层面成立；实际 runtime 路径多样性来自起点分布（4-4-4）而非「同起点不同分支」。
- 风险：当未来扩缩 startZone 集合或调整 `runtimeRouteFromStart`，topHashShare 可能突然抖动至 ≥40%（例如减少为 3 个起点会立刻让 share = 33% 或 50%）。
- 玩法层面也有体验风险：审查报表上「12 条不同路径」对玩家/策划而言是误导，真正能从 hash 分布看到的多样性是 ~3。

**建议归类**：P2 — 语义偏差但已通过门槛；优先级在路径校验/守卫修复之后。

---

### D2 (P2) 「禁用起点」守卫只覆盖 `LONG_RUN_SMOKE_CORPUS_ID`，未覆盖 `MAPGEN_DIFF` 等场景预设

**事实**：

- `game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt:79-84`

  ```kotlin
  require(
      corpusId != HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID ||
          zoneId !in FORBIDDEN_LONG_RUN_SMOKE_START_ZONE_IDS,
  ) {
      "Scenario '$name' uses forbidden LONG_RUN_SMOKE start zone '$zoneId'."
  }
  ```

- `ValidationScenarioRegistry.kt:16,71,392` 中 `MAPGEN_DIFF` 预设大量复用了同一组场景模型，但目前没有任何 `corpusId` / 预设标识能让该 `require` 触发——除非用户主动构造 long-run-smoke 语料。

**含义**：

- 规范 §5.1.8 要求把 `grey_gate_depths`、`abyssal_temple` 列为长程 smoke / 路径多样性的禁用起点；当前实现只在 long-run-smoke 一类语料生效，`MAPGEN_DIFF` 等其它「需要保证起点合规性」的预设依旧可以使用上述起点。
- 当未来某个验证流程复用 `ScenarioSpec` 走 mapgen 差异审查时，禁用列表会被静默绕过；这是治理弱化，不是阻断错误。

**建议归类**：P2 — 守卫范围不足，长期治理风险。

---

### D3 (P3) 基线字段命名与规范文档不对齐（`warningFloor` ↔ `minValue`）

**事实**：

- 规范 §6.1.4 / §6.1.5 多处使用 `warningFloor` 描述报告型门槛（如 `topologyCategoryDiversityPerSmokeRun.warningFloor`）。
- `docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json:47-48` 实际字段是：

  ```json
  {
      "metricId": "topologyCategoryDiversityPerSmokeRun.reportOnly",
      "minValue": 0.25,
      ...
  }
  ```

  即「指标名后缀 + 通用 `minValue`」组合实现了 reportOnly 语义。

**含义**：

- 不影响数值门槛执行（`Phase4MetricCatalog` 与 baseline runner 都用通用接口）。
- 但规范引用的字段名在仓库里搜不到，治理人员/未来排查者会困惑。

**建议归类**：P3 — 命名一致性问题。

---

### D4 (P3) `ScopeCoverageLint` 缺少对 `AffixBuildTags.kt` / `BuildIdentityAdoptionPolicy.kt` / `MilestoneRewardSelector.kt` 的显式路由用例

**事实**：

- `tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt` 的 `longrunDomain` 在 `longrun.runtime` 范围下覆盖了 8 个 `.kt` 文件，包括 `AffixBuildTags.kt`、`BuildIdentityAdoptionPolicy.kt`、`MilestoneRewardSelector.kt`。
- `tools/src/main/kotlin/com/ktome/tools/verification/ScopeCoverageLintRunner.kt` 中的用例集合仅显式覆盖了 `RouteHash.kt`、`ScenarioModels.kt`、`HeadlessRunHarness.kt`、`LongRunLabSeedBank.kt`，以及 6 个 `*_data_longrun_owner_scope` 数据用例和一个 negative `talent_sidebar_presentation_only_scope`。`grep` 在该 runner 内未匹配 `AffixBuildTags|BuildIdentityAdoption|MilestoneRewardSelector`。
- `ScopeCoverageLintTest` 仅断言 `caseCount >= 26`，并未对这 3 个文件的路由结果做存在性检查。

**含义**：

- 当前 `verifyChanged` 行为是正确的：因为 `longrunDomain` 是「文件路径前缀匹配」类型，新增/编辑那 3 个文件仍会触发 `:game:longRunLab`。
- 但 lint 用例缺这 3 个，`ScopeCoverageLint` 失去了对「未来某次重构挪走文件后仍应路由到 longRunLab」这类 regress 的兜底。
- 规范 §5.5.x 强调 lint 用例需正反对照覆盖；此处落地时省略了一部分对照。

**建议归类**：P3 — 治理覆盖缺口。

---

### D5 (P3) `LongRunKernelCache` 对旧版 JSON 的 `routeToken` 回退会丢掉 secret 标记

**事实**：

- `game/src/test/kotlin/com/ktome/game/harness/LongRunKernelCache.kt:229-231`

  ```kotlin
  routeToken =
      payload["routeToken"]?.jsonPrimitive?.content
          ?: routeToken(payload.requiredArray("zonePath").map { zoneId -> zoneId.jsonPrimitive.content }),
  ```

  当缓存 JSON 缺失 `routeToken` 字段时，回退到「按 `zonePath` 拼接 routeToken」。但 `zonePath` 在分支场景中是 zone id 序列，不包含 `secret:<id>` 标记。

**含义**：

- 对**当前会话写出来的新缓存**完全无影响——`routeToken` 永远有值。
- 但若有跨版本回放历史 JSON（譬如从 PR05 之前留下的工件）的需求，回放出来的 `routeToken` 与 `zoneRouteHash` 会与原始运行不一致，且不会有任何提示。
- 这是个「悄悄退化」型风险：拿来比对历史时 hash 不匹配，会被误判为代码回归。

**建议归类**：P3 — 仅在跨版本回放时才会触发。

---

## 4. 修复优化建议（按优先级）

### F1（修 D2）扩展守卫到所有需要禁用列表的预设

在 `ScenarioModels.kt` 内把守卫从「按语料 ID」改为「按预设/角色集合」，例如：

```kotlin
private val PRESETS_REQUIRING_FORBIDDEN_START_GUARD: Set<String> = setOf(
    HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
    HarnessMetadata.MAPGEN_DIFF_CORPUS_ID,         // 若已存在；否则在 HarnessMetadata 中补常量
)

require(
    corpusId !in PRESETS_REQUIRING_FORBIDDEN_START_GUARD ||
        zoneId !in FORBIDDEN_LONG_RUN_SMOKE_START_ZONE_IDS,
) { ... }
```

如果 `HarnessMetadata` 里目前没有 `MAPGEN_DIFF_CORPUS_ID` 常量，需在 `HarnessMetadata.kt` 与 `ValidationScenarioRegistry.kt`（line 16/71/392）一并补齐——这是规范 §5.1.8 要求的「跨预设强约束」语义。

**风险**：低；只是新增了约束，可能让某些历史脚本失败，但这正是预期效果。

---

### F2（修 D1）让 `routeTokenParts` 真正反映 `routeIntent`，或在 owner gate 中显式区分意图哈希

二选一：

- **方案 A（推荐，长期）**：让 `runtimeRoute` 在不破坏 procgen 的前提下，至少把 `routeIntent` 的不同前缀「编码」进 `routeTokenParts`（例如 `routeIntent + secretMarkers + runtimeRoute` 三段拼接），并在 `RouteHash.kt` 中以 `routeToken` 单独提供「含意图」与「runtime 实测」两套 token：

  ```kotlin
  val routeTokenParts = entry.routeIntent + listOf("|") + runtimeRoute
  ```

  然后在 `Phase4V4Pr06RouteDiversityCorpus.kt:282` 改为：

  ```kotlin
  val hashDistribution = terminalSpecs
      .groupingBy { spec -> zoneRouteHash(spec.routeIntent) } // 真正考核意图多样性
      .eachCount()
  ```

  并新增字段 `runtimeRouteHashDistribution` 作为辅助观察（不进入 owner gate）。

  这样 `topHashShare` 直接受 12 条意图约束，而非起点桶；若未来缩减起点也不会破阈。

- **方案 B（短期）**：保持现状，但在 `Phase4V4Pr06RouteDiversitySummary` 里把 `actualFullRouteHashDistinctCount`（当前 ~3-4）写入 owner 报表的「informational」一列，并在 `LongRunLabTest` 里补一句：

  ```kotlin
  assertTrue(actualFullRouteHashDistinctCount >= startZoneCount) {
      "Runtime route hash diversity must at least match the number of distinct start zones."
  }
  ```

  这样可以在不重写 hash 语义的前提下把「门槛敏感性」显式化。

**建议**：上线后立即采用方案 B 锁定下界，下一个 PR（PR07 或 v4 收尾）再以方案 A 收口。

---

### F3（修 D4）补齐 `ScopeCoverageLint` 用例

在 `ScopeCoverageLintRunner.kt` 中追加 3 个 positive case：

```kotlin
ScopeCoverageCase(
    id = "affix_build_tags_longrun_owner_scope",
    changedPaths = listOf("game/src/main/kotlin/com/ktome/game/foundation/AffixBuildTags.kt"),
    expectedTaskPaths = setOf(":game:longRunLab"),
),
ScopeCoverageCase(
    id = "build_identity_adoption_policy_longrun_owner_scope",
    changedPaths = listOf("game/src/main/kotlin/com/ktome/game/foundation/BuildIdentityAdoptionPolicy.kt"),
    expectedTaskPaths = setOf(":game:longRunLab"),
),
ScopeCoverageCase(
    id = "milestone_reward_selector_longrun_owner_scope",
    changedPaths = listOf("game/src/main/kotlin/com/ktome/game/foundation/MilestoneRewardSelector.kt"),
    expectedTaskPaths = setOf(":game:longRunLab"),
),
```

并把 `ScopeCoverageLintTest` 的 `assertTrue(run.caseCount >= 26)` 改为 `>= 29`，同时为 3 个新 case 各加一条 `assertContains(run.requestedTaskPaths(...), ":game:longRunLab")`。

**风险**：极低；纯测试增量。

---

### F4（修 D3）统一 `warningFloor` 命名

两种修法二选一：

- **修文档**（成本最低）：把规范文中「`warningFloor` 0.25」改为「`metricId` 后缀 `.reportOnly`、`minValue` 0.25」。
- **修代码**：在 `Phase4MetricCatalog` 与基线 runner 中接受 `warningFloor` 别名，并在 baseline JSON 里改字段名。

考虑到 `minValue / maxValue` 是 baseline 通用 schema，**优先建议改文档**（保持代码简洁）。

---

### F5（修 D5）`LongRunKernelCache` 兼容性硬化

把回退路径替换为「直接拒绝 / 降级标记」：

```kotlin
routeToken = payload["routeToken"]?.jsonPrimitive?.content
    ?: error("Cached scenario '${payload.requiredString("name")}' missing routeToken; cache version too old, please rebuild.")
```

或者保留回退、但在 `parseScenarioReport` 顶部记录一个 warning，并在 `LongRunKernelCacheTest` 中加一个用例：「JSON 缺 routeToken 字段时，缓存命中需被丢弃 / 报警」。

**风险**：需要清理一次本地缓存；但 PR06 已经引入字段变更，本就预期重建。

---

## 5. 测试与验证补强建议

| 编号 | 建议 | 原因 |
| --- | --- | --- |
| T1 | 在 `LongRunLabTest` 增加：所有 `LONG_RUN_SMOKE` 与 `MAPGEN_DIFF` 预设的 `ScenarioSpec.zoneId !in FORBIDDEN_LONG_RUN_SMOKE_START_ZONE_IDS`（配合 F1） | 守卫扩面后，避免回归 |
| T2 | `ScenarioModelsTest` 新增 negative case：构造 `corpusId = MAPGEN_DIFF`、`zoneId = abyssal_temple` 的 spec，应当 throw | 锁住 D2 修复语义 |
| T3 | 新增 `Phase4V4Pr06RouteDiversitySummaryTest`：当 startZone 集合从 4 缩到 3 时，`actualFullRouteHashDistinctCount` 仍 ≥ startZoneCount，且 topHashShare 仍 ≤ 0.4 | 校验 F2 后的最坏情况 |
| T4 | 在 `LongRunLabFullTest`（已修改）跑完后追加：`zoneRouteHashDiversity.runtimeRouteHashDistribution.distinctSize == startZoneCount`（或 F2 方案 A 后的 `== 12`） | 让运行期 hash 多样性变成显式断言 |
| T5 | `ScopeCoverageLintTest` 增 3 case 后，再加一条「talent_sidebar 路由不应触发 longRunLab」的 negative 加固 | 强化正反对照 |
| T6 | `ReportPhase4RunnerTest`（若存在）补一条：当 corpus 中存在 `secret:` 前缀的 `routeTokenParts` 时，Markdown 输出的 `## Route Diversity` 必须包含至少 1 个 secret marker 文本 | 防止 secret 标记在报表上被脱字符化 |

如果不便加 T4，至少在 PR 描述里把「实际 runtimeRouteHashDistinctCount = 4，源自 startZone 桶」写进交付说明，便于评审者理解 25% topHashShare 的来源。

---

## 6. 推荐验证命令

合并前/合并后建议至少跑一次：

```bash
# 1. 全部域内单测
./gradlew :game:longRunLab :tools:test

# 2. 验证场景 + 报表
./gradlew :tools:verifyChanged :tools:reportPhase4

# 3. 改动 RouteHash.kt / ScenarioModels.kt 等任意一个 longrun.runtime 文件后，
#    确认 verifyChanged 选中了 longRunLab
git checkout -b scratch/verify
echo "// noop" >> game/src/main/kotlin/com/ktome/game/RouteHash.kt
./gradlew :tools:verifyChanged --info | grep longRunLab
git checkout -- game/src/main/kotlin/com/ktome/game/RouteHash.kt
```

> 上述命令仅为推荐，不应在审查报告里声称已运行。

---

## 7. 修复优先级一览

| 优先级 | 编号 | 任务 | 估时 |
| --- | --- | --- | --- |
| P2 | F1 | `MAPGEN_DIFF` 等预设并入禁用起点守卫 | 0.5–1 人日 |
| P2 | F2 | `routeTokenParts` 携带意图段 / 显式锁定 runtime hash 下界 | 1–2 人日（方案 B 半天） |
| P3 | F3 | 补齐 3 条 `ScopeCoverageLint` 用例 | 0.25 人日 |
| P3 | F4 | 文档与基线 `warningFloor` 命名统一（建议改文档） | 0.25 人日 |
| P3 | F5 | `LongRunKernelCache` routeToken 回退硬化 | 0.5 人日 |

---

## 8. 一句话总结

> **PR06 在「门槛达标」与「报表面 + 域路由 + 缓存」上落地完整，可以发布**；但 `routeIntent` 与 `runtimeRoute` 之间的语义裂口、跨预设禁用起点守卫范围两项需要尽快收尾，否则未来对 startZone 或 procgen 的任何调整都可能让 PR06 的 25% topHashShare 一夜失守。
