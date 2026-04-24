# Phase4 v4 PR 开发文档深度审阅报告 R5

审阅日期：2026-04-24

审阅范围：

- `docs/review/phase4/v4-pr/README.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md`

参考真源：

- `AGENTS.md`
- `docs/phase4/roadmap.md`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `build.gradle.kts`
- `tools/build.gradle.kts`
- 当前代码真源：`BossPhaseManager`、`ContentPackManifest`、`ContentPackRuntimeResolver`、content pack resolver/data loader tests、fixture pack inventory
- 上轮报告：`docs/review/phase4/v4-pr/review/2026-04-24-phase4-v4-pr-director-review-r4.md`

## 结论

R4 的主要反馈已经被吸收：PR-01 不再把 `FROZEN` 写成新增 enum，PR-02 补齐 shop fingerprint，PR-03 增加 `localeLint` 与 metric group，PR-05 补了 client render snapshot，PR-06 路线表去重，PR-07 已把 schema v2 切换范围提升到全 runtime manifest。

当前没有发现新的 P1 级阻塞。实现前仍建议修 4 个 P2 问题和 4 个 P3 问题。核心风险集中在：PR-05 的 zone trigger 解析边界仍不够硬，PR-07 的 schema v2 切换缺少 `:game:test` owner suite，PR-04/05/07 对 canonical report 的定向验证入口少了 `reportPhase4Only`。

## Findings

### P1

本轮未发现新的 P1 阻塞项。

### P2-01 PR-05 的 boss override trigger 边界仍可能把 zone 事实解析泄漏进 core

证据：

- PR-05 规定 `BossPhaseManager` 在 base phase threshold 达成后评估 override trigger：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:143`
- 同一节又写明 `core` 只消费 semantic override 与 trigger resolution result，不读取 YAML、registry source 或 content pack 路径：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:148`
- variant 表包含 `zone.trigger.oil_or_fire_seen`、`zone.trigger.void_pressure_active`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:154`
- 当前 `BossPhaseEvaluationContext` 只有 `healthRatio`、`encounterTurnCount`、`activeStatusIds`，没有 zone trigger fact 输入：`core/src/main/kotlin/com/ktome/core/ai/BossPhaseManager.kt:3`
- 当前 phase match 只检查 hp、turn、required status：`core/src/main/kotlin/com/ktome/core/ai/BossPhaseManager.kt:68`

风险：

- 实现者可能把 zone hook 或 DataLoader 侧状态直接塞进 `core`，破坏 `core` 规则真源零引擎/零内容装配依赖边界。
- 也可能反过来只在 `game` 做 trigger resolution，导致 `BossPhaseManager` 文档中的评估职责与实现职责不一致。

建议：

1. 在 PR-05 文档中明确新增 `BossPhaseEvaluationContext.activeTriggerIds: Set<String>` 或等价的 `BossVariantTriggerFacts`。
2. 由 `game` / zone hook runtime 生成 `zone.trigger.*` 与 encounter-local trigger facts，`core` 只按 id 做 `AllOf` / `AnyOf` / `Not` 语义匹配。
3. 把测试清单补成三类：`core` expression matching、`game` zone hook fact materialization、`bossHarness` 端到端 override trigger coverage。

### P2-02 PR-07 schema v2 全量切换触碰 game owner，但验证命令缺 `:game:test`

证据：

- PR-07 scope 明确修改 `ContentPackModels.kt` 与 `ContentPackRuntimeResolver.kt`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:61`
- PR-07 要求 `ContentPackManifest.SCHEMA_VERSION` 升级到 `2`，`schemaVersion: 1` runtime manifest 进入 loader 必须 fail fast：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:210`
- 当前 `ContentPackManifest.SCHEMA_VERSION` 是 `1`：`game/src/main/kotlin/com/ktome/game/contentpack/ContentPackModels.kt:65`
- 当前 resolver 已对 schema mismatch fail fast：`game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt:197`
- 当前 `ContentPackRuntimeResolverTest` 中多处 inline manifest 仍使用 `schemaVersion: 1`，并且 mismatch test 用 `schemaVersion: 2` 作为失败输入：`game/src/test/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolverTest.kt:60`
- PR-07 自动化命令只包含 `contentPackHarness`、`whiteBoxContentPack`、lint、client smoke 与 report，没有 `:game:test`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:311`

风险：

- 只跑 harness 可能漏掉 resolver 单测、DataLoader content pack 单测和 inline manifest expectation 的全量反转。
- clean checkout 下，schema 常量升到 2 后，未刷新测试 fixture 会先在 `:game:test` 暴露，而不是在白盒路径暴露。

建议：

1. PR-07 README owner suite 和 PR-07 文档命令都补 `:game:test`，最好显式点名 `ContentPackRuntimeResolverTest` 与 `DataLoaderContentPackTest`。
2. PR-07 scope 补一行测试更新范围：`game/src/test/kotlin/com/ktome/game/contentpack/*Test.kt`。
3. 增加一个 repository runtime manifest inventory test：所有 runtime `manifest.yaml` 的 schemaVersion 必须等于 `ContentPackManifest.SCHEMA_VERSION`，sidecar harness spec 不参与该断言。

### P2-03 PR-04、PR-05、PR-07 改 report 字段但 owner suite 未包含 `reportPhase4Only`

证据：

- verification checklist 固定 phase aggregate 定向回归命令为 `reportPhase4Only`、`phase4LegacyReportOnly`、`reportPhase4`：`docs/phase4/2026-03-13-phase4-verification-checklist.md:43`
- root `reportPhase4Only` 描述为从已有 domain summaries 重建 canonical unified report：`build.gradle.kts:589`
- tools `reportPhase4` 是 canonical vs legacy parity gate，`reportPhase4Only` 是 canonical report rebuild：`tools/build.gradle.kts:891`、`tools/build.gradle.kts:909`
- README owner suite 中 PR-04、PR-05、PR-07 只有 `reportPhase4`，没有 `reportPhase4Only`：`docs/review/phase4/v4-pr/README.md:110`
- PR-04、PR-05、PR-07 各自验证命令也只写 `reportPhase4`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md:251`、`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:216`、`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:311`

风险：

- 这些 PR 都新增或调整 owner/report 字段，只跑 parity gate 容易把 canonical artifact-only rebuild 的字段缺失、freshness 和 producer 输入问题留到后续 PR。
- PR-02、PR-03、PR-06 已经采用 `reportPhase4Only reportPhase4`，PR-04/05/07 不一致会提高执行者误判成本。

建议：

1. README owner suite 对 PR-04、PR-05、PR-07 统一补 `reportPhase4Only reportPhase4`。
2. 三个 PR 文档的 `6.2 自动化命令` 与 `验证命令` 同步补齐。
3. 自证产物统一写 canonical path：`tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`。

### P2-04 PR-05 的 report-only owner 指标缺 warning floor 与公式口径

证据：

- README 要求每个新增指标给出事件来源、聚合公式、分母口径和 fail semantics：`docs/review/phase4/v4-pr/README.md:148`
- README 要求 report-only owner 指标必须登记 warning floor，不得进入 release gate：`docs/review/phase4/v4-pr/README.md:154`
- PR-05 的 `bossVariantPhaseOverrideActionDistinctCount.reportOnly` 标成 `reportOnlyOwner`，但表头没有阈值、事件来源、公式或分母，`failSemantics` 只有 `warn only`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:311`

风险：

- report-only owner 没有 warning floor 时，后续实现只能在 producer 或 renderer 里临时猜阈值，容易出现 metric catalog 与文档不一致。
- `DistinctCount` 如果没有分母口径，无法判断是 per variant、per phase、per fixed seed，还是全 run 聚合。

建议：

1. 给该指标补完整定义：事件来源、聚合公式、分母、warning floor。
2. 推荐口径：`distinct emphasized action ids observed per variant phase override >= 1 for each of 3 variants`。
3. 如果只是 debug 说明字段，应去掉 `.reportOnly` 与 `ownerBaseline`，改为 `supporting` / `display only`。

### P3-01 PR-05 完成定义漏写 telegraph coverage blocking 指标

证据：

- PR-05 必测行为要求 `bossVariantPhaseOverrideTelegraphCoverage=3/3`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:205`
- Report 指标表也把 `bossVariantPhaseOverrideTelegraphCoverage` 列为 blocking owner：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:309`
- 完成定义只写了 schema coverage 和 runtime trigger coverage，没有 telegraph coverage：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:331`

建议：完成定义第 3 条补上 `bossVariantPhaseOverrideTelegraphCoverage=3/3`，避免实现者把 telegraph 只当视觉白盒项而非 owner metric。

### P3-02 PR-07 的 `Rate` 指标名与阈值口径不匹配

证据：

- PR-07 指标名是 `samplePackContentPlayerVisibilityRate.reportOnly`，但阈值写成 `>= 1 observed touch per fixed-seed sample run`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:234`

风险：`Rate` 暗示有分母和百分比，但阈值实际是 count / observed touch。后续 catalog、renderer、baseline 可能对同一字段产生不同解释。

建议二选一：

1. 保留 `Rate` 名称，改成 `touchedSampleRuns / activeSampleFixedSeedRuns = 100%`，并补 denominator。
2. 改名为 `samplePackContentPlayerObservedTouchCount.reportOnly` 或 `samplePackContentPlayerVisibilityObserved.reportOnly`，阈值保持 `>= 1`。

### P3-03 PR-07 的 fixture id 规则描述过宽，和现有 fixture inventory 不完全一致

证据：

- PR-07 写明 `ContentPackIdParser` 规则固定为：`.` 永远表示 namespace 分隔；fixture pack id 使用 `_`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:143`
- 同一节 rename 范围只覆盖 `fixture.sample_flooded_relics_override` 到 `fixture_sample_flooded_relics_override`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:145`
- 当前 fixture inventory 仍有大量合法 fixture pack id 使用点号或连字符，例如 `fixture.add_monster`、`fixture.same_priority_left`、`fixture.namespace-collision`。

风险：实现者可能误以为本 PR 要全量迁移所有 fixture pack id，导致 diff 扩大；也可能只改 sample fixture，却留下一个看似全局的新规则没有被执行。

建议：把第 6 条改窄为“sample pack 对照 fixture id 使用 `_`，避免和 official sample namespace 混用”；如果要全局改 fixture id 规范，应拆独立治理 PR。

### P3-04 PR-02 的失败模型命名在 prose 与 Kotlin 草案之间不一致

证据：

- Kotlin 草案定义 `RequiresReplacementTarget`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:262`
- 流程描述写返回 `REQUIRES_REPLACEMENT_TARGET`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:272`

风险：这不是架构问题，但会影响实现者在 sealed object、event token、UI copy key 和 diagnostic code 之间命名取舍。

建议：如果这是 Kotlin sealed object，统一写 `RequiresReplacementTarget`；如果需要 event/token code，再另列 `shop.purchase.requires_replacement_target` 或等价稳定 token，避免把 enum-style 常量混进 Kotlin model。

## Requirement Alignment

| PR | 当前状态 | 仍需补齐 |
| --- | --- | --- |
| PR-00 | 白盒 validation mode 文档完整，路径纪律和 command source 已补齐 | 暂无 must-fix |
| PR-01 | `FROZEN` 已改为 tag 派生，tree id 与 profession tiers 已对齐 | 暂无 must-fix |
| PR-02 | shop fingerprint 与 replacement flow 已基本闭合 | 统一 `RequiresReplacementTarget` 命名 |
| PR-03 | identity/reward adoption 的 metric group、baseline、`localeLint` 已补齐 | 暂无 must-fix |
| PR-04 | hidden search / zone hooks 文档可实现 | 补 `reportPhase4Only` |
| PR-05 | boss variant phase language 方向正确 | 明确 zone trigger fact 边界；补 report-only warning floor；补 telegraph completion；补 `reportPhase4Only` |
| PR-06 | long-run route diversity 文档稳定 | 暂无 must-fix |
| PR-07 | schema v2 范围已升到全 runtime manifest | 补 `:game:test`；补 `reportPhase4Only`；收窄 fixture id 规则；修 metric 命名口径 |

## 功能/系统一致性矩阵

| 维度 | 结论 | 说明 |
| --- | --- | --- |
| 模块边界 | 基本通过，PR-05 需加硬边界 | `zone.trigger.*` 必须由 game materialize，core 只匹配语义 id |
| Content pack 合同 | 方向通过，验证不足 | PR-07 已承认 schema v2 是全局 runtime manifest 切换，但缺少 game tests |
| Report 合同 | 局部不一致 | PR-04/05/07 少 `reportPhase4Only`；PR-05 report-only 指标缺 warning floor |
| 玩家体验闭环 | 基本通过 | PR-05/07 仍需把可见反馈 metric 与 completion 对齐 |
| 文档可执行性 | 基本通过 | 剩余问题都是实现者容易误读的精度问题 |

## 玩法与体验审查

1. PR-05 的设计目标是让 variant phase 不只是数值差异，而是有 telegraph、action emphasis 和阶段反馈；因此 `telegraphCoverage=3/3` 必须出现在完成定义中，否则玩家可感知差异会被弱化成 harness 附带项。
2. PR-07 的 sample pack add-first path 对玩家最关键的是“至少在固定路线里真正看见 sample secret zone、hidden event、item id”。如果字段名叫 `Rate`，就应表达 run-level visibility rate；如果只统计触达次数，就应改名，避免玩法验收和报表语义分裂。
3. PR-05 的 zone trigger 与 boss phase 绑定是好方向，但必须保持“区域事实由 game 产生，boss phase 规则由 core 消费”的边界。否则后续做更多 zone-boss 联动时，会逐步把地图/内容装配语义渗进 core。

## 当前阶段必须解决的问题

进入实现前建议先改文档，顺序如下：

1. PR-05：补 `BossPhaseEvaluationContext` 或等价 trigger fact 输入边界，并明确 `zone.trigger.*` 的 producer 属于 game。
2. PR-07：把 `:game:test` 加入 README owner suite、PR-07 自动化命令和验证命令。
3. PR-04/05/07：统一补 `reportPhase4Only reportPhase4`。
4. PR-05：补 `bossVariantPhaseOverrideActionDistinctCount.reportOnly` 的 warning floor、公式和分母。

## Removal / Iteration Plan

| 项 | 处理建议 |
| --- | --- |
| PR-05 `BossPhaseManager` 直接评估 zone trigger 的模糊表述 | 改为 core 评估 semantic trigger ids，game 提供 active trigger facts |
| PR-07 fixture id 全局化表述 | 收窄为 sample fixture 专属规则，或拆独立 fixture id 规范 PR |
| PR-05 `.reportOnly` 指标但无 warning floor | 补 warning floor；若不需要 owner baseline，则降级为 supporting |
| PR-02 enum-style prose token | 改成 Kotlin object 名或单独列 event token |

## Additional Suggestions

1. README 的 owner suite 表建议按 PR 编号排序为 PR-00 到 PR-07；当前 PR-06 位于 PR-07 后面，虽不影响执行，但降低检索效率。
2. PR-07 schema v2 切换建议在“非目标”中明确“不做 v1 runtime manifest 兼容读、不提供 migration shim、不保留 legacy alias”，虽然 schema rules 已经写了，非目标再写一遍能降低实现时反复争论。
3. PR-05 对 `actionEmphasisIds` 的倍率固定为 `1.5x`，建议补一句“倍率不进入 content pack 可配置面”，避免后续把它误扩成 overlay scripting knob。

## Suggested Verification

文档修订后建议至少执行以下验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test bossHarness hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent contentPackHarness whiteBoxContentPack reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

PR-07 单独实现时建议额外保留定向测试：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests com.ktome.game.contentpack.ContentPackRuntimeResolverTest --tests com.ktome.game.contentpack.DataLoaderContentPackTest manifestLint contentPackHarness whiteBoxContentPack reportPhase4Only reportPhase4
```

PR-05 单独实现时建议额外保留定向测试：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :core:test --tests com.ktome.core.ai.BossPhaseManagerTest :game:test bossHarness goldenScreenshot clientSmoke reportPhase4Only reportPhase4
```

## Summary

这批 PR 文档已经从“可讨论方案”接近“可执行合同”。剩余问题不是大方向错误，而是边界和 gate 精度：PR-05 要把 zone trigger fact 的生产者写死在 game 侧，PR-07 要把 schema v2 的 game owner tests 纳入必跑，PR-04/05/07 要用 `reportPhase4Only` 锁住 canonical report rebuild。修完这些后，Phase4 v4 PR 文档可以进入实现排期。
