# Phase4 长期权威收口方案审查报告

审查对象：`codex/phase4-v2opt-pr-04-report-authority` 分支上当前的 secret reward identity 长期权威收口落地  
审查依据：`/Users/luo/Downloads/phase4report.md`（三层收口：schema → owner artifact → canonical report，加上 organic hidden 与 guardrail 收敛）  
审查角色：资深 Roguelike/ToME 玩法体验 & 系统设计总监  
结论先行：**整体已按长期方案落地，五条硬性收口全部对齐；遗留偏差均为命名/排版级，不影响 explainability 合同与 authority 单一性**。

---

## 1 · 五层收口对齐度一览

| 收口层 | 方案要求 | 当前实现锚点 | 对齐度 |
| --- | --- | --- | --- |
| schema 真源 | `LootProfileSchemaV3` 新增 `canonicalZoneId`，secret/cadence/reward tag 必须声明，缺失 fail fast | `SchemaModels.kt:134-173`（`LOCAL_IDENTITY_LOOT_PROFILE_TAGS` + `require(... canonicalZoneId != null)`） | ✅ 一致 |
| zone authority 下沉 | `LootProfileLocalIdentity.kt` 只读 `canonicalZoneId`，删掉 `canonicalZoneIdFromProfileId()` 与 `itemTagFilter.firstOrNull()` | `LootProfileLocalIdentity.kt:1-28` 仅保留 `localIdentityMetadata()`；旧 fallback 已无任何 usage | ✅ 一致 |
| index 合同 | `loot/index.yaml` 为参与 local identity 的 profile 全量补齐 | `game/src/main/resources/data/loot/index.yaml`：所有 10 条 cadence、6 条 reward、5 条 secret 全部显式声明 | ✅ 一致 |
| owner artifact 固定结构 | `whiteBoxLoot` / `lootBalanceLab` 产出单一 `SecretRewardIdentitySummary`，report 只消费 | `LootBalanceLabRunner.kt:386-438` 定义 `LootSecretProfileIdentitySummary`，字段覆盖文档 14 项全部要求（profileId / canonicalZoneId / identityAxes / rewardStructureKeys / fixedItemIds / candidateBaseIds / typeWeights / slotBias / specialTemplateTagPreference / affixTagPreference / sameZoneCadenceMaxOverlap / sameZoneRewardMaxOverlap / strictAllowedMaxOverlap / strictViolationPairIds） | ✅ 一致（名字偏差见 §3） |
| report contract | `ReportPhase4Runner` Markdown + JSON 固定渲染 `## Scripted vs Organic Hidden` 与 `## Local Reward Identity`，corpus 级结论 + per-secret 摘要 | `ReportPhase4Runner.kt:408-466` | ✅ 一致 |
| strict violation verdict 单一化 | owner evaluation / aggregation input / report 共用一处判定 | `LootOwnerThresholds.kt:152-191` 提供 `EvaluationResult.withStrictLocalIdentityViolations()`；`WhiteBoxLootRunner.kt:634` 与 `Phase4AggregationInputRunner.kt:497` 均调用同一 helper | ✅ 一致 |
| guardrail 判定单一化 | `LootProfileStructureAnalyzer` / `LootOwnerThresholds` 统一 helper | `LootOwnerThresholds.kt:79-90` 提供 `exceedsGlobalLocalIdentityGuardrail()`，analyzer `LootProfileStructureAnalyzer.kt:122` 直接复用 | ✅ 一致 |
| organic hidden explainability | `organicHiddenProbe` summary 固定保留解释字段，report 只渲染 | `ReportPhase4Runner.kt:414-434` 消费 `zoneDiscoveryDistribution` / `secretZoneDiscoveryDistribution` / `firstHiddenDiscoveryTurnP50/P90` / `firstSecretZoneEntryTurnP50/P90` / `comboCount` / `seedsPerZoneCombo` / `professionIds` / `raceIds` / `searchPromptRequired`，没有在 report 侧重新聚合 | ✅ 一致 |
| 契约 fail-fast | 聚合路径禁止 legacy `zoneId`，强制 `canonicalZoneId` | `Phase4DomainArtifactRegistry.kt:647-658` 的 `validatedSecretProfileIdentitySummaries()` | ✅ 一致 |

---

## 2 · 测试侧对齐度

| 方案必补项 | 现状 |
| --- | --- |
| schema/装配验证 canonicalZoneId fail fast | `LootProfileSchemaV3.init` 自带 `require`，装配路径一旦缺失直接抛 `IllegalArgumentException` |
| 人造 strict 违规 fixture（`strictLocalIdentityViolations != []`、`loot.localRewardIdentity` 变 `FAIL`、entry `UNEXPECTED_REGRESSION`、note 含 `pairId` + `allowedMaxOverlap`） | `WhiteBoxLootRunnerTest.kt:261-331` 用 baseline 覆盖将 `loot.deep_iron_slag_cache.secret` 的 strict 上限压到 0.19，四条断言全部命中（含 `0.190` 字面量与 `deep_iron_pit:loot.deep_iron_slag_cache.secret` pairId） |
| canonical report negative fixture（strict violation 透传为非绿） | `ReportPhase4RunnerTest.kt:177-261` 的 `propagates strict local identity violations as unexpected regression` 注入人造违规，断言 owner metric `status=UNEXPECTED_REGRESSION`、note + markdown 都带 pairId |
| canonical report markdown 合同 | `ReportPhase4RunnerTest.kt:99-106` 断言 `## Scripted vs Organic Hidden`、`## Local Reward Identity`、`rewardStructureKeys`、per-secret 行、`share of total discoveries/secret-zone entries`、`searchPromptRequired` 全部存在 |
| canonical JSON 输入稳定性 | `ReportPhase4RunnerTest.kt:83-95` 断言 `secretProfileIdentitySummaries` 全部含 `canonicalZoneId`、无 legacy `zoneId` |
| legacy `zoneId` 出现即 fail-fast | `ReportPhase4RunnerTest.kt:135-175` 构造 legacy `zoneId` summary，断言 `IllegalStateException` 提及 `canonicalZoneId` |

测试链路与方案 Test Plan 条目一一对得上，没有发现漏网。

---

## 3 · 偏差与改进建议（均非阻塞）

### 3.1 命名偏差：`LootSecretProfileIdentitySummary` vs 文档 `SecretRewardIdentitySummary`

- **现状**：owner artifact 结构命名为 `LootSecretProfileIdentitySummary`，JSON key 为 `secretProfileIdentitySummaries`。文档正文使用 `SecretRewardIdentitySummary`。
- **判断**：方案 Assumptions 明确 “如果现有 `secretProfileIdentitySummaries` 已存在，则本次实现只做字段收口和 authority 迁移，不改 metric id”，因此**不触发重命名**。但 Kotlin 类型名与文档的 `SecretRewardIdentitySummary` 仍然漂移，后续 reviewer 按文档搜源会找不到。
- **建议**：在 `LootBalanceLabRunner.kt:386` 处加一行 KDoc 明示 “canonical name is `LootSecretProfileIdentitySummary`, referenced as `SecretRewardIdentitySummary` in phase4 long-term design doc”，或在 `docs/review/phase4/` 索引处记录别名。保持 metric id 不变。

### 3.2 `LootProfileStructureAnalyzer.kt` 存在死代码 `category()`

- **位置**：`LootProfileStructureAnalyzer.kt:170-176` 留着一个 private 扩展 `LootProfileSchemaV3.category()`，但所有 call site 已迁移到 `localIdentityMetadata().category`。
- **影响**：不影响行为；但这恰恰是方案提到的 “避免 authority 再次漂移” 的前置清理 —— 再保留容易让后来人以为有第二套 category 来源。
- **建议**：删除该私有扩展，保持 category 只从 `LootProfileLocalIdentity.kt` 出一次。

### 3.3 Markdown per-secret 字段顺序与文档略有差异

- **方案顺序**：rewardStructureKeys → identityAxes → fixedItemIds → typeWeights/slotBias → specialTemplateTagPreference/affixTagPreference → cadence/reward overlap → strictTarget → strictViolationPairIds。
- **代码顺序**（`ReportPhase4Runner.kt:450-464`）：axes → rewardStructureKeys → fixedItemIds → candidateBaseIds → typeWeights → slotBias → specialTemplateTagPreference → affixTagPreference → overlap(包含 strictTarget) → strictViolationPairIds。
- **判断**：axes 与 rewardStructureKeys 顺序互换、额外渲染 `candidateBaseIds`、把 `strictTarget` 并入 overlap 行。内容完整，只是视觉排布不同。考虑到策划 reviewer 是从上到下读，建议：
  - 按文档把 `rewardStructureKeys` 放在第一行 —— 它才是 “为什么独立身份” 的最强信号；`identityAxes` 是轴维度，更像定语。
  - `candidateBaseIds` 是 debug 级信息，量大时会污染视图，建议放到独立 `<details>`/末尾，或在 overlap > 上限时才强制展开。

### 3.4 `poolStrategy` 作为“非文档字段”混在 owner summary 中

- **位置**：`LootSecretProfileIdentitySummary.poolStrategy`，文档 Key Changes §2 的 14 项固定字段里没有列。
- **判断**：不构成第二套 metric id，属于 owner 侧 debug 元信息。未来如果要把这个结构序列化成对外契约（白盒报告外的任何下游），应该把它放进 “optional explanatory extras”，避免隐式扩张 contract。
- **建议**：要么在 doc 侧补充此字段，要么在 JSON 渲染时加 `if` 只保留 TAG_WEIGHTED 情境（因为 FIXED_LIST 看 `poolStrategy` 意义有限）。

### 3.5 legacy `Phase4ReportRunner` 并存

- **现状**：`tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt` 仍然在仓库里，被 `ReportPhase4RunnerTest` 间接通过 `compareLegacy=true` 调用做 parity 校验。
- **判断**：方案禁止 “再加 legacy report 路径或 sibling aggregate task”。这里是既存代码保留，严格说没有违背 “再加” —— 但它仍是未来 authority 漂移的温床（万一有人又加新的 section 到 legacy，就会破坏 canonical 的唯一解释面）。
- **建议**：在 `Phase4ReportRunner` 头部加 `@Deprecated(...)` + 或加 lint 规则禁止在其内部新增渲染分支；长期安排一次删除（与 `phase4LegacyReport` gradle 任务一起），用 canonical 全量替代。

### 3.6 organic hidden 分布指标渲染健壮性

- **位置**：`ReportPhase4Runner.kt:429-433`，`secretZoneDiscoveryDistribution` 使用 `getValue(...)`，若未来 owner 产物暂缺该字段会抛 `NoSuchElementException`。
- **判断**：方案文档明确 “如当前链路已支持，也保留”，目前链路支持，所以直接必需字段没问题；但对于剧变（如短暂关闭 secret 入口遥测）不够宽容。
- **建议**：改用 `get(...)?.jsonObject?.takeIf(JsonObject::isNotEmpty)?.let { ... }`，并在缺失时渲染 `- secretZoneDiscoveryDistribution: n/a`，保持 section 合同仍然闭合。

### 3.7 fixture 内容包也同步补了 `canonicalZoneId`

- 例 `examples/content-packs/sample.flooded_relics/data/loot/sample.flooded_relics.loot.flooded_reliquary.secret.yaml:5` 与 `tools/src/main/resources/fixtures/content-packs/packs/fixture.sample_flooded_relics_bias_split/.../secret.yaml`。
- 符合 “fail fast 后，所有 content-pack 样本必须自己声明 canonicalZoneId”。这里没有偏差，仅记录为 **完整性加分项**。

---

## 4 · 长期 authority 质量评估（玩法体验视角）

| 维度 | 评价 |
| --- | --- |
| reviewer 闭环 | 只读 `report-phase4-summary.md` 即可回答 “zone 归属 / identity axis / reward structure / overlap / strict 状态 / organic 发现路径”，达到方案目标 |
| content designer 闭环 | 新增 secret profile 时 schema 就强制写 `canonicalZoneId`，不再靠命名约定；rewardStructureKeys 一处产出，策划不会再在 markdown 里看到两种不一致的解释 |
| data 漂移防护 | 三处（schema init / analyzer localIdentityMetadata / registry validation）形成 defence in depth；canonical aggregate path 禁 legacy `zoneId` |
| explainability 复用 | 没有为 explainability 新开 metric namespace（`localRewardIdentity.*` / `secretRewardIdentity.*`），严格遵守 Assumptions |
| organic hidden | `zoneDiscoveryDistribution` 与 `secretZoneDiscoveryDistribution` 都在 canonical section 显式渲染，策划可直接读出 “secret zone 漏斗偏斜” |
| strict 违规故事 | 违规触达 canonical markdown、owner metric note、canonical JSON entry 三个面，而且同一套 `withStrictLocalIdentityViolations` helper 统一产出，不会再出现 “某一路没变红” 的回归 |

**一句话定性**：长期方案要求的 “canonical `phase4Report` = secret reward identity 唯一可引用解释面” 已经形成稳定闭环；剩余偏差不触及合同层，属于清理 + 文档漂移。

---

## 5 · 建议的跟进清单（按优先级）

1. **P1**：删除 `LootProfileStructureAnalyzer.kt:170-176` 的死代码 `category()` 扩展，杜绝潜在的 category 第二真源。
2. **P1**：把 Markdown per-secret 第一行改为 `rewardStructureKeys`，与方案文档顺序一致；`candidateBaseIds` 收入 `<details>` 折叠块。
3. **P2**：在 `LootSecretProfileIdentitySummary` 上加 KDoc 明示它就是文档里的 `SecretRewardIdentitySummary`；或在 `docs/review/phase4/` 加别名索引。
4. **P2**：`secretZoneDiscoveryDistribution` 渲染用 `get(...)?.jsonObject` 兜底，避免未来 owner 端暂缺字段导致 report 直接崩。
5. **P3**：给 legacy `Phase4ReportRunner` 打 `@Deprecated` 并规划删除节奏，避免 authority 再漂。
6. **P3**：在 owner summary 里把 `poolStrategy` 正式写进 doc 合同，或改成 optional extras block，避免隐式扩张。

---

## 6 · 结论

- **与 `phase4report.md` 的一致性**：**高度一致**。三层收口 + Test Plan + Assumptions 全部落地，fail-fast / 单一 helper / canonical contract 都到位。
- **偏差总数**：7 项，均为命名、渲染顺序、清理型偏差；**0 项阻塞合同**。
- **发布建议**：可以按当前分支发布，跟进项以 P1/P2/P3 拆 follow-up PR；其中 P1 两项建议在合并本次长期方案 PR 前一起清理，避免埋下 “二次漂移” 的钩子。
