# Phase4 v4 后续优化 PR 拆分索引

本目录把 `docs/review/phase4/v4` 中已经判定必须进入开发的优化项，整理为按执行顺序推进的 PR 级开发文档。拆分标准是玩家体验优先级、系统依赖关系、工作量边界和验证闭环。

## 1. 执行顺序

| 顺序 | PR 文档 | 优先级 | 工作量 | 玩家体验目标 | 资源生成结论 |
| --- | --- | --- | --- | --- | --- |
| 0 | `2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md` | P0 | M | 建立快速白盒验证模式，让后续 PR 能稳定复现目标场景并快速留证 | 不生成图片；不生成音频 |
| 1 | `2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md` | P0 | L | 升级从数值 rank 变成职业树选择 | 不生成图片；不生成音频 |
| 2 | `2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md` | P0 | M | 铭文从开局满配变成 run 内购买与替换选择 | 不生成图片；不生成音频 |
| 3 | `2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md` | P0 | L | capstone、非武器 payoff 和奖励采用率真实改变 build | 不生成图片；不生成音频 |
| 4 | `2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md` | P1 | L | 隐藏探索形成主动 Search 学习链，zone 名词兑现最小 runtime 玩法 | 不生成图片；不生成音频 |
| 5 | `2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md` | P1 | M | Boss variant 从权重差分变成有阶段记忆点的轻量变体 | 不生成图片；不生成音频 |
| 6 | `2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md` | P2 | S | 官方 sample pack 展示 ADD-first 玩家扩展路径，并能被玩家看见 | 不生成图片；不生成音频 |
| 7 | `2026-04-24-phase4-v4-pr06-long-run-route-diversity.md` | P1 | M | 长局验证覆盖多路线，防止奖励和构筑调参过拟合单主线 | 不生成图片；不生成音频 |

执行顺序按依赖链固定，PR-06 虽然是 P1，但它读取 PR-03、PR-04、PR-07 的最终 owner 字段和 route/content-pack summary，因此排在最后合入。

## 2. 统一执行规则

1. 每个 PR 必须先读本文、对应 PR 文档、`docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md` 和对应证据章节。
2. 每个 PR 执行前必须确认 Phase4 UI/UX PR 已作为完成前提处理；本目录只保留玩法、验证与白盒证据收口。
3. 每个 PR 不得把本目录中的确定目标改写成非阻塞描述。
4. 每个 PR 的实现必须同步代码、数据、i18n、report、harness 和 verification checklist。
5. 每个 PR 的新报告字段必须进入 canonical owner evidence；不得只写到 Markdown。
6. 每个 PR 的验证失败必须修实现或修文档口径；不得降低核心体验阈值。
7. 本目录所有文档中的路径保持 repo-relative。
8. 每个 PR 必须包含 `## 6. 测试与自证`、`### 6.1 必测行为`、`### 6.2 自动化命令`、`### 6.3 人工白盒验证流程`、`### 6.4 统一验证框架关系`。
9. 每个 PR 的人工白盒验证必须参考 `docs/computer-use-whitebox-flow.md`，并使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 代替。
10. PR-01 到 PR-07 执行前必须先合入 PR-00 的快速白盒验证模式。PR-00 的主体是现有游戏内 Validation Mode 的 scenario 化、快速 action 与目标状态预置，不是单纯外部脚本。
11. 后续 PR 的人工白盒必须通过 `preparePhase4V4Whitebox` 生成 launch script、CUA runbook、manual record 模板和证据清单，再按当前 PR 的 scenario id 执行。
12. 快速白盒验证模式只负责缩短场景抵达、证据建档和启动路径，不得替代 owner gate、reportPhase4、goldenScreenshot、clientSmoke 或当前 PR 声明的 core/game/tools 测试。
13. 每个 PR 的 metric table 就是该 PR 的 Metric Glossary；开工前必须包含 metric id、阈值、事件来源、聚合公式、分母口径、producer、baseline、failSemantics。
14. 本轮 PR 不承担旧 run save、旧 replay、旧 report id、旧 schema、旧 command payload 的兼容；长期稳定结构优先于旧数据继续运行。
15. PR-03 到 PR-07 必须同时遵守 [development-governance.md](./development-governance.md)；每个文档必须包含 `## 0. 开发治理与验收矩阵`。
16. PR-03 是治理 canary；PR-03 未完成 `acceptanceContractLint`、fast lane、owner producer、report gate、最终 `verifyChanged` 与 doc-vs-implementation self-audit 前，PR-04 到 PR-07 不得声称已经验证新规范有效。
17. 重型 owner gate 不得作为需求探索或调参循环；同一重型 gate 失败超过 2 次或单轮本地验证超过 90 分钟时，先按 `development-governance.md` 写复盘，再继续修。

### 2.1 长期稳定优先与破坏性改造纪律

Phase4 v4 的后续开发固定为破坏性收口，不做旧版本兼容层。

执行规则：

1. 旧 run save / replay / validation fixture 命中已废弃 schema 时必须 fail fast，并提示重新开局或刷新 fixture。
2. 不新增 deprecated wrapper、compat alias、legacy fallback、schema dual-read、旧字段回填、旧 report id 映射。
3. 现有 deprecated wrapper 和 legacy alias 在对应 PR 内删除；下游 PR 只能消费新 schema、新事件、新 metric id。
4. report / owner evidence 只保留最终 canonical metric id；旧 `.reportOnly` id 升级为 blocking 时必须删除旧 id。当前文档声明为 `.reportOnly` 的新增指标仍是 canonical 新 id，不属于旧 id。
5. command payload 变更以清晰、稳定、可验证为先；不为了旧日志或旧 fixture 保留冗余字段。
6. fixture、golden、manual record、white-box baseline 在 PR 内按新 schema 全量刷新。
7. 唯一允许保留的“旧”语义是玩家体验中的被替换对象，例如铭文替换时旧铭文销毁；它不是兼容层。

### 2.2 职业 release classification 与阻塞门槛口径

Phase4 v4 的职业统计固定按以下 release classification 执行：

| classification | 职业 | PR-01 / PR-02 规则执行 | release-facing blocking metric |
| --- | --- | --- | --- |
| `BASE` | `vanguard`, `arcanist`, `rogue`, `templar` | 执行 3 starter、learnable 第 4 技能、铭文 2 starter、Tier prerequisite、断点事件 | 纳入分母 |
| `ADVANCED` | `berserker`, `spellblade` | 执行同一 starter、learnable、铭文、Tier prerequisite、断点事件 | 进入 report-only coverage，不纳入 release-facing blocking 分母 |
| `FROZEN` | `shadowblade`, `warden` | 不改技能数量，不改 starter，不参与 run 内构筑门槛 | report 中列为 excluded frozen，不静默丢弃 |

执行规则：

1. PR-01 / PR-02 的数据改造必须同步覆盖 6 个可用职业。
2. PR-03 的 capstone / non-weapon payoff blocking 分母只统计 4 个 `BASE` 职业。
3. 任何 report 字段涉及职业分母时，必须同时输出 `includedProfessions`、`advancedReportOnlyProfessions`、`excludedFrozenProfessions`。
4. `FROZEN` 由 `tags contains frozen` 派生，只用于 report / eligibility classification，不属于职业发布层级枚举。
5. 本轮 PR 不在 `ProfessionTier` 增加 frozen 成员，不把 `shadowblade / warden` 的 YAML `tier` 改成 `FROZEN`。

### 2.3 PR-01 后续继承的 Talent UI / snapshot 合同

PR-01 合入后，后续 PR 必须继承以下实现边界：

1. `TalentTreeNodeSnapshot.category` 是 `TalentCategory` typed enum；JSON wire 仍是 `"ACTIVE" / "PASSIVE" / "SUSTAINED"` 字符串枚举名。
2. client、game harness、tools 不得再对 `snapshot.category` 做 `TalentCategory.valueOf(...)` 或字符串分支；snapshot consumer 必须直接消费 typed enum。
3. Talent tree sidebar 的 presentation authority 固定为 `client.ui.talent.TalentSidebarPresenter`。
4. `AsciiRenderModel` 和 `TileRenderModel` 只能把 `TalentSidebarLine.role` 映射为各自 tone；Tile 可以额外 resolve `iconKey`。不得在 renderer 内重新拼装 talent tree 文案、glyph、rank label、preview 展开/折叠、active-slot-choice 文案或 footer。
5. `DescriptionPresenter.presentTalentTreeNodeLines` 继续负责 talent node 描述文本；renderer 不得绕过 presenter 直接为 talent tree sidebar 组装描述行。
6. 后续 PR 若触碰 talent sidebar、active-slot-choice modal、`TalentTreeNodeSnapshot` 或相关 RenderSnapshot serialization，必须同步更新 `TalentSidebarPresenterTest`、`InputHandlerTest`、ASCII/Tile render model 测试，以及 snapshot serialization 测试。

### 2.4 Phase4 v4 开发治理入口

长期治理入口固定为 [development-governance.md](./development-governance.md)。

本 README 只维护 PR 索引、phase 继承规则和验证入口摘要；Acceptance Matrix、Gate Budget、Canonical Artifact、Failure Rule、doc-vs-implementation self-audit 与 whitebox skip 记录规则只在 [development-governance.md](./development-governance.md) 维护。

## 3. 资源管线结论

本轮 v4 后续优化优先修玩法结构、构筑选择、奖励采用、探索学习链、验证效率和验证代表性。当前仓库已经存在足够的职业、天赋、铭文、Boss variant、special item、sample pack visual/audio 资源。因此 8 个 PR 均不生成新图片、不生成新音频。

实现过程中不得新增 `assets-src/image/specs/phase4-v4-pr*.yaml` 或 `assets-src/audio/specs/phase4-v4-pr*.yaml`。确实发现资源 key 缺失时，先修正式 manifest / data 引用，复用已有 visual/audio key；不得用新资源生成绕过本轮玩法结构问题。

本轮不新增 image/audio generation plan。凡 PR 新增或改动 `visualKey`、`iconKey`、`audioProfile`、locale key、content pack visual/audio manifest 引用，必须在同批验证中执行对应的 `assetLint`、`audioLint`、`manifestLint`，证明复用资源可解析。

资源与 manifest 口径固定为：

1. 禁止新增图片生成规格、音频生成规格和生成报告。
2. 允许新增 runtime manifest / data entry，使新 semantic key 指向现有资源文件。
3. PR-04 的 frontstage cue key、PR-05 的 `data/telegraph/index.yaml` spec、PR-07 的 content pack visual/audio manifest entry 均属于 manifest/data ADD，不属于新资源生成。
4. 新增 manifest/data key 必须同步 i18n、asset/audio/manifest lint、golden 或白盒证据。

## 4. 统一验证入口

所有 PR 的 Gradle 执行前必须使用仓库 SDKMAN 环境：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

每个 PR 文档列出的验证命令是该 PR 的最小验收入口。涉及 `reportPhase4` 的 PR 必须保证 producer freshness，特别是 content pack 相关任务必须同批刷新 `contentPackHarness` 与 `whiteBoxContentPack`。
每个 PR 完成人工白盒前必须同批执行 `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=<scenario-id>`，并把 packaged app 路径、隔离 `runtime-home`、evidence 目录和 manual record 写入对应记录。

producer freshness 纪律：

1. 改动数据、schema、harness 或 report 字段的 PR，必须在同一 build 内刷新对应 producer、`reportPhase4Only` 与 `reportPhase4`。
2. PR-03 的 `longRunLab / whiteBoxLoot / lootBalanceLab` 与 reward/build/affix 数据同批刷新。
3. PR-04 的 `hiddenContentHarness / organicHiddenProbe / whiteBoxHiddenContent` 与 zone hook/search 数据同批刷新。
4. PR-05 的 `bossHarness` 与 boss variant YAML、telegraph data、trigger registry 同批刷新。
5. PR-07 的 `contentPackHarness / whiteBoxContentPack` 与 sample pack manifest、hidden binding、visual/audio/i18n manifest 同批刷新。

| PR | 最小 owner suite |
| --- | --- |
| PR-00 | `./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest :client:test --tests com.ktome.client.screen.ValidationScenarioBootstrapTest :client:test --tests com.ktome.client.input.ValidationCommandSourceTest :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:packageMacApp preparePhase4V4Whitebox maintainabilityLint verifyChanged -Pktome.whitebox.scenario=phase4-v4-pr00-selftest` |
| PR-01 | `./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab soloClearLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged` |
| PR-02 | `./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged` |
| PR-03 | `./gradlew localeLint :game:test :tools:whiteBoxLoot :tools:lootBalanceLab longRunLab verifyOwner reportPhase4Only reportPhase4 audioLint contractLint maintainabilityLint verifyChanged` |
| PR-04 | `./gradlew localeLint contractLint assetLint audioLint :core:test :game:test hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged` |
| PR-05 | `./gradlew localeLint contractLint assetLint audioLint :core:test :game:test bossHarness goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged` |
| PR-07 | `./gradlew manifestLint contractLint localeLint assetLint audioLint :game:test contentPackHarness whiteBoxContentPack goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged` |
| PR-06 | `./gradlew :game:test longRunLab reportPhase4Only reportPhase4 scopeCoverageLint maintainabilityLint verifyChanged` |

### 4.1 开发期快路径

开发期快路径只用于提交前快速发现局部错误，不能替代各 PR 的最小 owner suite。

| PR | 快速验证入口 |
| --- | --- |
| PR-00 | `./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest :client:test --tests com.ktome.client.screen.ValidationScenarioBootstrapTest` |
| PR-01 | `./gradlew :core:test --tests com.ktome.core.talent.TalentAllocationPlannerTest :game:test --tests com.ktome.game.TalentProgressionTest :game:test --tests com.ktome.game.FoundationGameSessionTest` |
| PR-02 | `./gradlew :core:test --tests com.ktome.core.inscription.InscriptionSlotTest :game:test --tests com.ktome.game.FoundationGameSessionTest --tests com.ktome.game.harness.SmokeBotTest :client:test --tests com.ktome.client.input.InputHandlerTest` |
| PR-03 | `./gradlew :game:test --tests com.ktome.game.loot.MilestoneRewardSelectorTest` |
| PR-04 | `./gradlew :game:test --tests 'com.ktome.game.hidden.*' :tools:test --tests 'com.ktome.tools.hidden.*'` |
| PR-05 | `./gradlew :core:test --tests com.ktome.core.ai.BossPhaseManagerTest :game:test --tests com.ktome.game.BossVariantDataLoaderTest` |
| PR-06 | `./gradlew :game:test --tests 'com.ktome.game.harness.*' :tools:test --tests 'com.ktome.tools.verification.*' scopeCoverageLint` |
| PR-07 | `./gradlew :game:test --tests com.ktome.game.contentpack.ContentPackRuntimeResolverTest --tests com.ktome.game.contentpack.DataLoaderContentPackTest contentPackHarness whiteBoxContentPack manifestLint localeLint assetLint audioLint` |

## 5. Report 字段合同

每个 PR 新增或替换的 report 字段必须在对应 PR 文档中声明以下四项；实现时不得只把字段写进 Markdown report。

Phase4 canonical report 产物固定为 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`。`tools/build/reports/phase4/phase4-summary.{json,md}` 只作为 legacy parity evidence；PR 文档的自证产物不得只写泛化 `build/reports/phase4/`。

| 字段 | 固定含义 |
| --- | --- |
| `metricKind` | `blockingOwner`、`reportOnlyOwner`、`supporting`、`debugSample` 四类之一 |
| `producer` | 真实 producer task，例如 `longRunLab`、`bossHarness`、`whiteBoxContentPack` |
| `ownerBaseline` | blocking / report-only owner 指标对应 baseline repo-relative path；非 owner 字段写 `N/A` |
| `failSemantics` | `fail owner gate`、`warn only`、`display only` 三类之一 |

执行规则：

1. blocking owner 指标不得使用 `.reportOnly` 后缀。
2. report-only owner 指标必须使用 `.reportOnly` 后缀，并且在完成定义中不得替代 blocking 指标。
3. 支撑说明字段使用 `supporting` 或 `debugSample`，不得进入 owner gate。
4. 每个新增指标必须在 PR 文档中给出事件来源、聚合公式、分母口径和 fail semantics。

### 5.1 指标目录扩增纪律

1. 所有新增 field 必须登记到 `Phase4MetricCatalog`，并指向唯一 producer。
2. blocking owner 指标必须登记到 `Phase4OwnerMetricTargets`，并在 `Phase4OwnerBaselineRegistry` 中声明 baseline。
3. report-only owner 指标必须登记 warning floor，不得进入 release gate。
4. 同一 metric id 不得同时出现在 blocking、report-only、supporting、debugSample 四类中。
5. 新增 metric id 必须通过 catalog linter 校验：id 唯一、metricKind 唯一、producer 存在、baseline repo-relative、failSemantics 合法。
6. 每个 PR 的 report 字段清单必须在实现 PR description 中逐项列出，便于 review 对照。
7. 单个 PR 新增 blocking owner metric 超过 `5` 个时，必须在 PR 文档中给出 metric group、baseline split、落地顺序和回滚边界；PR-03 固定按 capstone identity、slot balance、affix distribution 三组串行接线。

### 5.2 i18n key convention

新增 i18n key 固定按以下前缀命名：

| 用途 | 前缀 |
| --- | --- |
| UI label / button / modal | `ui.<module>.<screen>.<label>` |
| UI hint / footer / empty state | `ui.<module>.<screen>.<hint>` |
| combat / talent / shop / hidden log | `log.<module>.<event>` |
| metric 描述 | `metric.phase4.<metricId>.desc` |
| validation overlay | `validation.phase4.v4.<scenarioId>.<field>` |
| content pack sample | `sample_flooded_relics.<domain>.<id>` |

执行规则：

1. `en-US.json` 与 `zh-CN.json` 必须同批新增。
2. client 不得硬编码正式展示文案。
3. `localeLint` 必须覆盖新增 key 与未使用 key。

### 5.3 UI snapshot 测试纪律

以下 PR 新增或改动 UI layout 时，必须补对应 snapshot / render model 测试：

| PR | UI surface | 必测测试面 |
| --- | --- | --- |
| PR-01 | Talent tree sidebar、active slot choice modal | `client/src/test/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenterTest.kt`、`AsciiRenderModelTest`、`TileRendererCanvasTest` |
| PR-02 | Inscription replacement modal | `client/src/test/kotlin/com/ktome/client/ui/card/**/*SnapshotTest.kt` |
| PR-04 | Frontstage cue / log priority | `client/src/test/kotlin/com/ktome/client/render/**/*SnapshotTest.kt` |
| PR-05 | Boss variant warning / telegraph presentation | `client/src/test/kotlin/com/ktome/client/render/**/*SnapshotTest.kt` |
| PR-07 | Main menu pack summary / validation overlay pack summary | `client/src/test/kotlin/com/ktome/client/screen/**/*SnapshotTest.kt` |

测试必须覆盖 `1280x800` 和窄屏等价布局，证明文本不溢出、modal 不遮挡关键前台信息、empty state 与 active state 都可读。
Talent tree sidebar 是 presenter-first surface：新增或调整 sidebar line role 时，必须先更新 `TalentSidebarPresenterTest`，再用 ASCII/Tile render model 测试证明 renderer 只做 role 到 tone/icon 的映射。

## 6. 串行开发与破坏性改造守则

执行依赖图固定为：

```text
PR-00 -> PR-01 -> PR-02 -> PR-03 -> PR-04 -> PR-05 -> PR-07 -> PR-06
```

1. PR-01、PR-02、PR-03 是同一条构筑闭环链。PR-02 与 PR-03 分支必须以 PR-01 的 schema / event / report 字段为上游，不得在下游分支复制临时 alias。
2. PR-04 是 PR-05 与 PR-07 的共同上游：PR-05 消费 PR-04 的 `zone.trigger.void_pressure_active` 与 `zone.trigger.oil_or_fire_seen`；PR-07 消费 PR-04 的 `secretZoneSelector.primarySlot / secondarySlot`。
3. PR-05 不得在 PR-04 合入前开发完成验收；PR-07 不得在 PR-04 合入前开发完成验收。
4. PR-06 最后合入；它只能消费 PR-03、PR-04、PR-05、PR-07 已稳定的 report 字段、route 字段、content pack 字段和 owner producer。
5. PR 被否决时，下游分支必须重建到新的上游合同；不得保留旧字段、空字段、兼容 alias 让 report 绿。
6. PR-01 合入后，PR-02 / PR-03 只消费新 talent event；旧 `unlocked` 语义不得出现在生产路径。
7. PR-02 合入后，PR-03 只消费新 inscription install/replace event；旧满槽购买失败字段不得进入 blocking 分母。
8. PR-03 合入后，PR-06 只消费新 blocking reward/build metric；旧 `.reportOnly` metric 不再作为输入。
9. 共享文件写入顺序固定为执行依赖图顺序；`ValidationScenarioRegistryTest.kt`、`Phase4MetricCatalog.kt`、`Phase4OwnerMetricTargets.kt`、`Phase4OwnerBaselineRegistry.kt`、`tools/src/main/resources/phase4/aggregation-manifest.yaml`、`phase4-v4-scenarios.yaml` 只能在当前 PR 基于最新上游后追加或替换自身 domain 条目。
10. 下游 PR 发现共享文件冲突时，必须重放自身 domain 条目到最新上游，不得保留重复 id、临时 id、兼容 id 或并行 catalog。
