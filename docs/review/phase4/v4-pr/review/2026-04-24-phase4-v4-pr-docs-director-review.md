# Phase4 v4 PR 级开发文档深度审阅报告

审阅日期：2026-04-24
审阅范围：`docs/review/phase4/v4-pr/*.md`
审阅定位：面向 Phase 4 后续开发优化任务的 PR 级设计审查，重点检查玩法体验闭环、系统策划一致性、模块边界、owner evidence、验证入口与当前代码事实的一致性。

## 1. 直接结论

当前 `docs/review/phase4/v4-pr` 的 7 份 PR 文档总体方向正确：优先修职业树选择、铭文替换、构筑奖励采用、主动隐藏探索、Boss variant 阶段语言、长局代表性和 sample pack ADD-first 可见性，符合 Phase 4 “重复 run 差异与可扩展内容”收口目标。

但这些文档还不能直接进入实现队列。主要问题不是愿景不足，而是 PR 级开发合同仍存在几类硬缺口：

1. 多个 PR 的“验证命令”低于自身声明的改动范围，尤其遗漏 `verifyChanged`、`maintainabilityLint`、`localeLint`、`contractLint`、`assetLint`、`audioLint`、`manifestLint`、`goldenScreenshot` 或 owner gate。
2. PR-03 把 report-only 债务升级为 blocking，但没有完整声明 owner baseline、canonical owner evidence、aggregation manifest、metric catalog 与 verification checklist 的同步改动。
3. PR-04 把 frontstage cue 写入 `RenderSnapshot`，却遗漏 core snapshot owner 文件和 client/golden 验证入口，容易形成“report 绿但玩家看不到”的断层。
4. PR-07 把 sample pack 从 REPLACE 改成 ADD-first，但没有明确新增 secret zone / hidden event 如何被 mapgen、hidden binding 或 fixed-seed harness 触达，存在“schema ADD 成功但玩家不可见”的风险。
5. 部分 PR 的代码片段或路径与当前代码事实不一致，可能在实现时引入不必要 API churn 或遗漏真实 owner。

建议先修本文 P1/P2 问题，再按 PR-01 -> PR-07 执行。P3 可作为实现前的文档打磨项。

## 2. Findings

### P1-1：各 PR 的最小验证入口系统性低于自身改动范围

证据：

- `docs/review/phase4/v4-pr/README.md:22-23` 要求每个 PR 同步代码、数据、i18n、report、harness、verification checklist，且新报告字段必须进入 canonical owner evidence。
- PR-01 修改 core/game/client/tools、i18n、snapshot、audio router、asset/audio key，验证却只有 `:core:test :game:test :client:clientSmoke longRunLab soloClearLab reportPhase4`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:46-80`、`:91-113`、`:261-267`。
- PR-02 修改 inscription core API、shop flow、client replacement UI、i18n，验证却只有 `:core:test :game:test :client:clientSmoke longRunLab reportPhase4`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:45-75`、`:86-109`、`:231-237`。
- PR-03 修改 reward owner metric、blocking 指标、audio key 引用和 report runner，验证却遗漏 owner gate、baseline consistency、resource lint 和 `verifyChanged`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:51-78`、`:101-109`、`:202-208`。
- PR-04 修改 frontstage cue、i18n、hidden harness、report，验证却没有 core/client/golden/locale/contract 入口，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md:45-73`、`:135-145`、`:157-163`。
- PR-05 修改 boss variant schema、telegraph/audio/visual data、i18n、boss harness，验证却只有 `:core:test :game:test bossHarness reportPhase4`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:41-67`、`:78-100`、`:172-178`。
- PR-07 修改 sample pack manifests、visual/audio manifests、i18n、client screens、content pack reports，验证却只有 `contentPackHarness whiteBoxContentPack reportPhase4`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:44-67`、`:76-97`、`:147-153`。
- 仓库级规则要求 non-trivial Kotlin / verification wiring 跑 `maintainabilityLint`，PR CI 默认入口是 `verifyChanged`，见 `AGENTS.md:197`、`AGENTS.md:200`、`docs/verification/README.md:12`。

影响：

这些 PR 都属于 Phase 4 completion hardening，目标是把玩家体验和 owner evidence 收口。如果验证入口只跑核心 smoke，不跑 lint、manifest、golden、owner gate、impact routing，会出现以下失败模式：

1. 玩家界面或资源 key 断了，但 `phase4Report` 仍然绿。
2. i18n / visual / audio / manifest 不可解析，但开发者只看 long-run 指标误判完成。
3. 新 owner metric 只在 Markdown 或临时 report 中出现，没有进入 canonical owner evidence。
4. PR CI 路由没有覆盖新增 owner surface，后续改动可以绕过 gate。

修复建议：

每个 PR 的“验证命令”需要从“局部 smoke 命令”升级成“最小 owner suite”。建议在 README 增加统一矩阵，并在每份 PR 文档替换对应命令。

建议最低要求：

| PR | 额外必须补的验证入口 |
| --- | --- |
| PR-01 | `localeLint contractLint assetLint audioLint goldenScreenshot clientSmoke maintainabilityLint verifyChanged`，并保留 `:core:test :game:test longRunLab soloClearLab reportPhase4` |
| PR-02 | `localeLint contractLint assetLint audioLint goldenScreenshot clientSmoke maintainabilityLint verifyChanged`，并保留 `:core:test :game:test longRunLab reportPhase4` |
| PR-03 | `verifyOwner reportPhase4Only reportPhase4 verifyChanged maintainabilityLint audioLint contractLint`，必要时加 `:tools:test --tests` 覆盖 metric catalog / report runner |
| PR-04 | `:core:test clientSmoke goldenScreenshot localeLint contractLint audioLint maintainabilityLint verifyChanged`，并保留 `hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent reportPhase4` |
| PR-05 | `contractLint assetLint audioLint localeLint maintainabilityLint verifyChanged`，并保留 `:core:test :game:test bossHarness reportPhase4` |
| PR-06 | `scopeCoverageLint maintainabilityLint verifyOwner reportPhase4Only`，并保留 `:game:test longRunLab reportPhase4 verifyChanged` |
| PR-07 | `manifestLint contractLint localeLint assetLint audioLint clientSmoke goldenScreenshot maintainabilityLint verifyChanged`，并保留 `contentPackHarness whiteBoxContentPack reportPhase4` |

### P1-2：PR-03 将 report-only 债务升级为 blocking，但 scope 没覆盖 owner contract 切换所需文件

证据：

- PR-03 完成标准要求 `professionCapstoneAdoptionFloor` 与 `nonWeaponBuildPayoffFloor` 从 report-only approved debt 升级为 blocking，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:24-33`。
- PR-03 新增 blocking 指标直接使用无 `.reportOnly` 后缀的 metric id，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:179-191`。
- 当前 Phase 4 verification checklist 明确 canonical evidence 仍包含 `professionCapstoneAdoptionFloor.reportOnly` 与 `nonWeaponBuildPayoffFloor.reportOnly`，见 `docs/phase4/2026-03-13-phase4-verification-checklist.md:33`。
- checklist 明确 `.reportOnly` 后缀只能表示“不参与 blocking gate”，若实际阻塞 owner gate 必须移除后缀，且命名、baseline、`failSemantics`、render status 必须一致，见 `docs/phase4/2026-03-13-phase4-verification-checklist.md:44`。
- 当前 code owner surface 仍有 `.reportOnly` 口径：`Phase4MetricCatalog`、`Phase4ReportRunner`、`ReportPhase4Runner`、`Phase4AggregationInputRunner` 都直接引用这两个 report-only metric，见 `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt`、`tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`、`tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt`、`tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt`。

影响：

PR-03 当前文档容易让实现者只改 reward scoring 和 report 文案，却漏掉 metric contract 切换。结果可能出现：

1. report 渲染上像 blocking，owner baseline 仍按 report-only 解释。
2. `Phase4OwnerMetricTargets` 和 `Phase4MetricCatalog` 命名不一致，报告聚合或 owner gate 出现第二真源。
3. `reportPhase4` 和 `reportPhase4Only` 对同一指标给出不同语义。
4. 后续 PR 以为债务已清，但 CI 没有真正 blocking。

修复建议：

PR-03 的 scope 必须补充：

- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `tools/src/main/resources/phase4/aggregation-manifest.yaml`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerMetricTargets.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerBaselineRegistry.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/ReportPhase4Runner.kt`
- 对应 owner baseline 文件和 registry consistency tests

并把完成定义改成：两个 `.reportOnly` metric 被正式迁移为 blocking metric，旧 report-only id 被删除或保留为兼容 alias 时必须明确 sunset plan，`verifyOwner + reportPhase4Only + reportPhase4 + verifyChanged` 同批通过。

### P1-3：PR-04 要求 frontstage cue 进入 `RenderSnapshot`，但遗漏 core snapshot owner 和 client 可见性验证

证据：

- PR-04 要求前台 cue 区分 `lead_discovered`、`search_available`、`secret_entry_nearby`、`zone_hook_triggered`，并且“必须有 stable key、TTL、dedup，并进入 `RenderSnapshot`”，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md:135-145`。
- PR-04 生产代码 scope 包含 `SessionSnapshotMapper` 和 client presentation，但没有列出 `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md:45-58`。
- PR-04 验证命令只包含 `:game:test hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent reportPhase4`，没有 `:core:test`、client smoke、golden screenshot、locale/contract lint，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md:157-163`。

影响：

Hidden content 的体验目标不是“harness 能发现入口”，而是玩家能看到异常、主动 Search、得到反馈。若 cue 只在 game report 里成立，没有 core snapshot serialization/hash/constructor invariant 和 client golden 覆盖，容易形成三类断层：

1. `RenderSnapshot` 字段没有稳定序列化，save/replay/golden 或 snapshot hash 不稳定。
2. client 没消费新 cue，玩家仍看不到 Search 学习链。
3. i18n token 或 TTL/dedup 策略缺失，frontstage cue 堆叠、过期或覆盖已有高优先级提示。

修复建议：

PR-04 scope 必须补充：

- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
- core snapshot serialization/hash/constructor invariant tests
- `clientSmoke`、`goldenScreenshot` 或针对 frontstage cue 的 selector/golden task
- `localeLint contractLint audioLint maintainabilityLint verifyChanged`

同时把 completion 增加为：fixed-seed 下玩家视角能看到四类 cue，golden/smoke 证明 cue 不遮挡现有 critical/high priority frontstage cue。

### P1-4：PR-07 ADD-first sample pack 缺少“新增内容如何被触达”的 authority 路径

证据：

- PR-07 要求 official sample pack 主路径不再包含 `REPLACE`，secret zone、hidden event、loot profile、special item 全部使用 namespaced `ADD`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:23-29`。
- PR-07 将新增 secret zone id 固定为 `sample.flooded_relics.secret_zone.flooded_reliquary`，hidden event id 固定为 `sample.flooded_relics.hidden_event.flooded_reliquary.reward`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:98-118`。
- 当前 official sample manifest 仍通过 `REPLACE` 覆盖 base `underground_river_crystal_rift` 和 `hidden.event.underground_river.crystal_rift.reward`，见 `examples/content-packs/sample.flooded_relics/manifest.yaml:12-22`。
- 当前 base mapgen/hidden binding 绑定的是 official `underground_river_crystal_rift` 路径，见 `game/src/main/resources/data/mapgen/zones/index.yaml` 中的 `search.underground_river.crystal_rift`、`secret.underground_river.crystal_rift`、`underground_river_crystal_rift`。
- PR-07 scope 没有声明 mapgen binding、hidden route injection、zone objective hook 或 fixed-seed sample run 如何触达新的 namespaced secret zone，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:44-67`。

影响：

把 sample pack 从 REPLACE 改成 ADD 是正确方向，但 ADD 只证明 registry 可扩展，不自动证明玩家可见。如果新增 secret zone 没有被 mapgen、hidden content pipeline 或 harness route 挂上，`samplePackContentPlayerVisibilityRate.reportOnly` 会退化为“报告人工扫描注册表”，不能证明玩家体验。

修复建议：

PR-07 必须在设计里补一个明确触达方案，二选一即可：

1. overlay loader 允许 pack ADD 一条 namespaced hidden/search binding，并由 content pack lint 校验 binding 不覆盖 core 规则语义；
2. white-box sample run 在 pack-active scenario 中显式注入 sample hidden branch，并把触达定义限定为 fixed-seed 玩家路径事件，而不是 registry presence。

无论选择哪种，都必须补充：

- secret zone / hidden event / loot profile 的 route binding 权威文件或测试 fixture
- `contentPackHarness` 对 ADD entry reachability 的断言
- `whiteBoxContentPack` 对 touched content ids 的 fixed-seed 事件证据
- `manifestLint contractLint localeLint assetLint audioLint clientSmoke goldenScreenshot verifyChanged`

### P2-1：PR-01 Tier 3 前置 rank 与上游补充设计不一致

证据：

- 上游补充设计要求每条基础职业树至少 1 个 Tier 3 节点要求前置 rank `>= 3`，见 `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md:179-185`。
- PR-01 的 Tier 3 表写成“指定前置 rank `>= 2`”，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:174-194`。

影响：

Tier 3 是职业树 build-defining payoff。若 PR 文档把前置 rank 从 `>= 3` 降为 `>= 2`，玩家在前 30 分钟的路线承诺会变浅，且 implementation 可能与补充设计、数据 loader fail-fast 测试产生冲突。

修复建议：

把 PR-01 Tier 3 表改为“指定前置 rank `>= 3`”，或者明确修改上游补充设计并说明为何 Phase4 改为 `>= 2` 更符合点数节奏。当前没有看到足够理由，建议按上游补充设计修 PR-01。

### P2-2：PR-01 的 `TalentProgression` API 草案与当前代码签名不一致

证据：

- 当前 `TalentProgression.unlockedTalentIds` 签名需要 `SchemaCatalog`、`ProfessionSchemaV2`、`level`、`RaceDef?`，见 `game/src/main/kotlin/com/ktome/game/TalentProgression.kt:8-14`。
- PR-01 新函数草案写成 `ProfessionDef`、`RaceDef?`、`level`、`learnedRanks`，没有传入 `SchemaCatalog`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:117-128`。

影响：

这不是单纯命名问题。当前实现要从 `schemaCatalog.talents` 和 `schemaCatalog.talentTrees` 解析树节点、unlockLevel 和 race tree。PR 文档若沿用不存在的 `ProfessionDef`，实现者可能在 game 层再造 profession/talent lookup helper，形成第二 authority 或不必要的大改。

修复建议：

PR-01 应把草案改成贴近现状的签名，例如：

```kotlin
fun learnableTalentIds(
    schemaCatalog: SchemaCatalog,
    profession: ProfessionSchemaV2,
    level: Int,
    learnedRanks: Map<String, Int>,
    race: RaceDef? = null,
): List<String>
```

并明确旧 `unlockedTalentIds` 只能作为迁移 wrapper，所有正式调用点改为 learnable 语义，不再让 `syncUnlockedPlayerTalents` 自动写入 rank 1。

### P2-3：PR-01 对基础职业和 DEV_UNLOCKED 职业的范围口径容易误导实现

证据：

- PR-01 完成标准写“四个基础职业开局职业技能数固定为 `3`”，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:23-32`。
- PR-01 起始技能表又包含 `berserker` 和 `spellblade`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:159-172`。
- 上游补充设计固定：四个基础职业不新增技能，`berserker` 和 `spellblade` 在 Phase4 只补前置与断点，不扩成完整 16 技能职业；但 DEV_UNLOCKED 职业的 startingTalents 也应按 3 技能处理，见 `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md:62-66`、`:122-139`。

影响：

当前文档容易导致两种相反误解：

1. 只改四个 base，漏掉 `berserker` / `spellblade` 的开局 3 技能与第四技能 learnable 迁移。
2. 把 `berserker` / `spellblade` 当成完整基础职业扩到 16 技能。

修复建议：

PR-01 应显式区分：

- release-facing blocking metrics 只统计四个 base profession。
- data migration 同步覆盖 `berserker` / `spellblade` 的 3 starter 与 learnable 第四技能。
- `berserker` / `spellblade` 不扩技能数量，只补 prerequisites / breakpoint。

### P2-4：PR-02 的 `BuyShopOffer` 草案引入不必要字段重命名

证据：

- 当前 `PlayerCommand.BuyShopOffer` 字段名是 `index`，见 `game/src/main/kotlin/com/ktome/game/GameView.kt:31-34`。
- PR-02 草案把字段改为 `offerIndex`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:172-181`。

影响：

字段重命名没有带来玩法收益，却会扩大 call site diff、破坏命令日志/serialization/测试 fixtures 的可审阅性。PR-02 本来已经要改 inscription replacement 状态机，没必要叠加 API churn。

修复建议：

保持字段名 `index`，只增加 replacement target：

```kotlin
data class BuyShopOffer(
    val index: Int,
    val replacementHotkey: Int? = null,
) : PlayerCommand
```

如果确实要改为 `offerIndex`，必须在 PR 文档里列出所有 call site、snapshot/log 影响和迁移测试。当前建议不改名。

### P2-5：PR-05 漏列真实 `BossVariantDef` owner，schema 改造范围不够精确

证据：

- 当前 `BossVariantDef` 实际定义在 `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt:108-116`。
- PR-05 的生产代码 scope 列了 `core/src/main/kotlin/com/ktome/core/ai/BossEncounter.kt`、`core/src/main/kotlin/com/ktome/core/ai/BossPhaseManager.kt`、`game/src/main/kotlin/com/ktome/game/model/BossDefinition.kt`、`game/src/main/kotlin/com/ktome/game/factory/BossFactory.kt`、`game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`，但没有列出 `MutationModels.kt`，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:37-67`。
- 当前 data loader 直接 parse `BossVariantDef`，见 `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` 中对 `BossVariantDef` 和 `parseBossVariants` 的引用。

影响：

PR-05 的目标是 data-level phase language，不是重写 boss runtime。漏列 variant schema owner 会让实现者在 `BossDefinition` 或 `BossFactory` 侧做旁路结构，或者把 variant override 解释成 boss definition mutation，从而偏离“最小 data-level 阶段语言”。

修复建议：

PR-05 scope 应补充：

- `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt`
- `game/src/main/kotlin/com/ktome/game/elites/MutationModelsTest.kt` 或对应 schema test
- `DataLoader.parseBossVariants` 的 fail-fast 校验

同时明确：`phaseOverrides` 属于 boss variant schema；core boss phase manager 只消费已经解析出的语义，不知道 content pack / YAML 路径。

### P2-6：PR-06 的 verifyChanged 路由目标正确，但缺少反向防过载测试入口

证据：

- PR-06 明确要求 `verifyChanged` 只路由最小 affected subset，nightly/owner gate 承担宽样本，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md:21-29`。
- PR-06 技术方案要求普通 client/UI 变更不路由 full long-run，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md:132-140`。
- PR-06 验证命令没有包含 `scopeCoverageLint` 或直接的 `VerifyChangedPlanGate` 测试入口，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md:151-157`。

影响：

PR-06 的核心不是多加 9 条 seed，而是建立“宽样本归 owner/nightly，PR preflight 不被拖死”的 routing contract。若没有反向测试，后续可能出现两类回归：

1. long-run 变更没有路由到 `:game:longRunLab`。
2. 无关 UI 变更错误路由到 full long-run，拖慢 PR CI。

修复建议：

PR-06 验证命令补充 `scopeCoverageLint` 或定向 `VerifyChangedPlanGate` 测试。完成定义增加两条：

- 修改 long-run corpus / route hash / harness runner 时，`verifyChanged` 包含 `:game:longRunLab`。
- 修改无关 client UI 文件时，`verifyChanged` 不包含 full long-run owner suite。

### P3-1：资源“无需生成”和“必须 lint”应统一成显式规则

证据：

- README 统一说明 7 个 PR 均不生成新图片、不生成新音频，见 `docs/review/phase4/v4-pr/README.md:27-31`。
- 多个 PR 的资源章节又要求证明 key 可解析，例如 PR-01 `assetLint` / `audioLint`，PR-03 `audioLint`，PR-05 visual/audio key，PR-07 sample visual/audio manifest，见对应 PR 资源章节。

影响：

“不生成资源”容易被误读为“不跑资源相关检查”。实际这里的要求是复用已有资源，但所有新增或复用引用仍必须被 manifest/lint 验证。

修复建议：

在 README 资源管线结论后补一句统一规则：

> 本轮不新增 image/audio generation plan，但凡 PR 新增或改动 visualKey、iconKey、audioProfile、locale key、content pack visual/audio manifest 引用，必须跑对应 `assetLint`、`audioLint`、`manifestLint`，证明复用资源可解析。

### P3-2：PR 文档应区分“report 字段新增”和“owner evidence 生效”

证据：

- README 要求新报告字段进入 canonical owner evidence，见 `docs/review/phase4/v4-pr/README.md:22-23`。
- 各 PR 的 `Report 与验收指标` 多数只列新增字段和阈值，没有说明字段属于 blocking、report-only、supporting、owner baseline、producer artifact 还是 display-only。

影响：

Phase 4 已经多次出现“report 里能看到，但 gate 不知道”的问题。继续用宽泛“新增指标”会让 owner metric、supporting metric、debug sample 混在一起。

修复建议：

每个 PR 的指标表增加四列：

| 字段 | 建议值 |
| --- | --- |
| `metricKind` | `blockingOwner` / `reportOnlyOwner` / `supporting` / `debugSample` |
| `producer` | `longRunLab` / `bossHarness` / `whiteBoxContentPack` 等 |
| `ownerBaseline` | baseline path 或 `N/A` |
| `failSemantics` | `fail owner gate` / `warn only` / `display only` |

### P3-3：PR-07 client overlay 属于玩家承诺，应给出最小 UI 完成态

证据：

- PR-07 要求 validation overlay / main menu 展示 active pack id、namespace、op summary、touched content ids、pack-local key resolution status，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:135-145`。
- 当前完成定义只说“能展示 active sample pack 和 touched content ids”，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:155-163`。

影响：

active pack 可见性是玩家和 pack 作者的第一反馈面。如果完成定义不要求 unresolved key、disabled pack、multiple pack order、touched content ids empty state，UI 很容易只做 happy path。

修复建议：

PR-07 completion 增加：

1. 无 active pack 时显示 `N/A` 或明确 empty state。
2. active sample pack 时显示 pack id、namespace、op summary、touched content ids。
3. visual/audio/i18n key resolution failure 时显示非阻塞 warning，不改变 runtime 规则。
4. golden/smoke 覆盖 active pack 与 no pack 两种状态。

## 3. PR 级一致性矩阵

| PR | 玩法目标 | 当前文档成熟度 | 必须先修 |
| --- | --- | --- | --- |
| PR-01 | 升级成为职业树选择 | 方向正确，但 API、Tier gate、base/advanced 范围需修正 | P2-1、P2-2、P2-3、验证入口 |
| PR-02 | 铭文从开局满配变成 run 内购买/替换 | 体验闭环清晰，但 command API 草案有无谓 churn | P2-4、验证入口 |
| PR-03 | reward 真正兑现 build identity | 目标重要，但 owner contract cutover 不完整 | P1-2、验证入口 |
| PR-04 | hidden content 形成主动 Search 学习链 | 玩法目标强，但 snapshot/client 可见性链路缺口大 | P1-3、验证入口 |
| PR-05 | boss variant 有阶段记忆点 | 设计边界正确，但 schema owner 漏列 | P2-5、验证入口 |
| PR-06 | long-run 覆盖多路线，避免调参过拟合 | 相对成熟，但 routing 反向测试要补 | P2-6、验证入口 |
| PR-07 | sample pack 展示 ADD-first 且玩家可见 | 方向正确，但 reachability authority 未定义 | P1-4、P3-3、验证入口 |

## 4. 玩法与系统策划审查

### 4.1 职业树与铭文

PR-01 和 PR-02 是整轮 v4 后续优化的体验地基。它们共同把“开局满配”改成“run 内选择”。这个方向符合类 ToME 构筑体验，但要避免两个设计退化：

1. 职业树不能只变成 UI 展示所有节点。核心是 rank 0 -> 1 学习、新技能 reserve/active slot、前置 rank 和树投入共同制造承诺。
2. 铭文替换不能只变成“满槽也能买”。核心是满槽时牺牲哪个 escape / heal / shield / cleanse answer。

因此 PR-01 和 PR-02 的 report 字段必须证明玩家真的做了选择，而不是系统自动补齐。`learnedTalentChoiceEventRate`、`breakpointChoiceEventRate`、`inscriptionInstallOrReplaceRate`、`inscriptionReplaceReasonDistribution` 这些字段应进入 long-run evidence，而不是只出现在文档。

### 4.2 Reward adoption

PR-03 是 Phase 4 体验闭环的高风险点。职业树和铭文让玩家做选择，但如果 reward 不兑现选择，玩家仍会回到泛化最优装备。PR-03 把 capstone/non-weapon payoff 从 report-only 债务升级为 blocking 是正确的，但必须完整切换 owner contract。

建议 PR-03 不只调数值，还要强制输出解释样本：

1. selected candidate 的 profession / slot / source / score breakdown。
2. top rejected capstone candidates 和 rejected reason。
3. wrong-profession capstone penalty 生效样本。
4. non-weapon payoff 压过普通 base item 的样本。

否则后续 tuning 会变成盲调权重。

### 4.3 Hidden Search 与 zone hook

PR-04 的体验方向是对的：hidden content 必须从“系统赠品”转成“玩家观察异常 -> 主动 Search/Interact -> 得到反馈”。但该 PR 必须非常克制，不要把每个 zone 词汇都做成 runtime subsystem。

建议保留当前“每个 mandatory zone 只落一个 hook”的约束，同时把所有未落地名词进入 `flavorOnlyMechanics`。这能防止文档名词变成第二 authority。

关键验收不应只看 `searchUseRate > 0`，还应看：

1. cue 是否在玩家可行动前出现。
2. cue 是否可被 Search/Interact 消费。
3. cue 是否不挤掉 critical/high priority 战斗提示。
4. Search 失败时是否有可解释反馈，避免玩家把系统当成噪声。

### 4.4 Boss variant phase language

PR-05 的边界合理：Phase 4 只做 data-level phase language，不引入 tactical AI、普通敌人 intent 或完整 graph mutation。这里要防止实现时过度设计。

最小正确实现应满足：

1. variant schema 能声明 override。
2. override 的 trigger、telegraph、action emphasis 可校验。
3. boss harness 能证明 telegraph 触发和 action emphasis 变化。
4. `phaseGraphUnchanged` 保留为说明，而不是 failure。

不要把它升级成 Phase 5 tactical AI。

### 4.5 Long-run route diversity

PR-06 是验证代表性修复，不是内容扩张。增加 seed 是必要但不充分的，真正合同是 route hash 和 branch-inclusive 覆盖进入 owner evidence，并且 `verifyChanged` 不被宽样本拖死。

建议 PR-06 的成功定义增加“冷热路径成本”说明：PR preflight 路由最小 subset，nightly/owner gate 承担宽样本，防止后续因为 CI 慢而回退 gate。

### 4.6 Sample pack ADD-first

PR-07 的设计目标正确。官方 sample pack 不应教 pack 作者用 REPLACE 覆盖官方内容。但是 ADD-first 的玩家可见性不是 registry 层事实，而是 runtime path 事实。

如果不补 reachability authority，sample pack 会变成“看起来可扩展，但装上后玩家看不到”。建议把 PR-07 的核心验收从“manifest 主路径只有 ADD”升级为：

1. manifest 主路径只有 ADD。
2. ADD secret / event / loot / item 都有 namespaced key。
3. fixed-seed sample run 至少触达一个 sample content id。
4. validation overlay 展示 active pack 和 touched content ids。
5. fixture pack 保留 REPLACE/precedence/conflict 证明。

## 5. Removal / Iteration Plan

### 必须移除或改写

1. PR-01 Tier 3 `rank >= 2` 的前置描述，改为 `rank >= 3`，除非同步修改上游补充设计。
2. PR-01 `ProfessionDef` 版本的 API 草案，改为当前 `SchemaCatalog + ProfessionSchemaV2` 口径。
3. PR-02 `BuyShopOffer.offerIndex` 字段重命名，默认保留当前 `index`。
4. PR-03 只在 PR 文档里声明 blocking 的做法，必须补完整 metric contract cutover。
5. PR-04 只跑 game hidden harness 的验证命令，必须补 snapshot/client/golden。
6. PR-07 只声明 ADD manifest 的完成定义，必须补 runtime reachability。

### 可延期但应记录

1. PR-07 multiple pack order UI 的复杂状态可以作为后续 polish，但 active sample / no pack 两种状态必须本 PR 完成。
2. PR-05 action emphasis 的具体调参可以先 report-only，但 coverage 和 telegraph 触发必须 blocking。
3. PR-06 wide corpus 的样本规模可以后续加大，但 route diversity metric 和 routing tests 必须本 PR 完成。

## 6. Suggested Verification

文档修正后，建议把每份 PR 文档的命令改成显式 owner suite。下面是建议模板，不要求一次性在本报告中执行。

PR-01 / PR-02：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab reportPhase4 maintainabilityLint verifyChanged
```

PR-03：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test :tools:whiteBoxLoot :tools:lootBalanceLab longRunLab verifyOwner reportPhase4Only reportPhase4 audioLint contractLint maintainabilityLint verifyChanged
```

PR-04：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint audioLint :core:test :game:test hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent goldenScreenshot clientSmoke reportPhase4 maintainabilityLint verifyChanged
```

PR-05：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test bossHarness reportPhase4 maintainabilityLint verifyChanged
```

PR-06：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test longRunLab reportPhase4 reportPhase4Only scopeCoverageLint maintainabilityLint verifyChanged
```

PR-07：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew manifestLint contractLint localeLint assetLint audioLint contentPackHarness whiteBoxContentPack goldenScreenshot clientSmoke reportPhase4 maintainabilityLint verifyChanged
```

## 7. 修订优先级

1. 先修 README 的统一验证矩阵和资源 lint 规则，因为它会影响 7 个 PR 的执行纪律。
2. 再修 PR-03、PR-04、PR-07 的 P1 缺口，因为这些会直接导致 owner evidence 或玩家可见性失真。
3. 再修 PR-01、PR-02、PR-05、PR-06 的 P2 implementation-readiness 问题，降低实现返工。
4. 最后统一补 metric 表的 `metricKind / producer / ownerBaseline / failSemantics`，防止后续 report 字段漂移。

## 8. 总结

这组 PR 文档已经把 Phase 4 后续优化拆成了合理的玩家体验纵切，但还需要一次 contract-level 收紧。最关键的修订原则是：所有“玩家能感知”的目标都必须有 runtime path 和 client path；所有“report 变绿”的目标都必须进入 canonical owner evidence；所有“复用资源”的目标都必须通过 manifest/lint 证明引用可解析；所有“PR 最小验证”都必须覆盖自身声明的 owner surface。

修完本文 P1/P2 后，这组文档才适合直接作为 Phase 4 completion hardening 的开发输入。
