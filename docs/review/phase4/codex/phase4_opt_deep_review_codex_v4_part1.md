# phase4 深度审查报告（Part 1/4）

## 1. 执行摘要

- 当前 `Phase4` 不是“功能看起来很多、实际玩不起来”的伪完成版本；它已经具备了长局、隐藏内容、地形战斗、精英变异、掉落池分化、内容包覆盖等阶段性骨架，且自动化 gate 基本齐全。
- 但它也还不是“已经足够耐玩，可以放心把体验债全部留给 Phase5”的状态。最核心的问题不是系统缺失，而是**少数关键系统虽然完成了合同，却还没有完全完成玩家动力闭环**。
- 最重的问题是**职业幻想被装备底盘收敛压扁**。`build/reports/harness/long-run-full.md` 显示多个职业在长局终盘高度收敛到 `battle_axe`，说明当前奖励与装备体系仍在把不同职业推向同一伤害底盘。
- 第二个关键问题是**隐藏内容的“自然发现闭环”没有被真正证明**。现有 `hiddenContentHarness` 与白盒报告证明了搜索入口、触发器、秘密区合同闭环，但并没有证明玩家在自然跑图中会稳定地产生“发现 -> 冒险 -> 奖励 -> 再探索”的驱动。
- 第三个关键问题是**奖励池平均分化合格，但局部高价值 reward/secret pool 仍高度重叠**。这会让秘密奖励和主线路奖励在体验上变成“同一类东西的更大包”，而不是记忆点明确的探索回报。
- 战斗层已经比 Phase3 更成立：地形标签暴露率、交互遭遇率、精英变异与 Boss 变体都进入正式路径，说明战术层不是纸面系统；但这些决策优势还没有完全转化成更强的构筑差异与职业身份。
- 当前版本的综合判断不是“功能完成但体验未闭环”，而是：**基本可玩，长局成立，耐玩雏形成立，但关键驱动点还不够锋利，尤其是成长分化、奖励识别度、自然探索驱动仍需要在 Phase4 内继续打磨。**
- 因此，本报告的结论是：**Phase4 基本达到文档承诺的功能与合同目标，但还不适合在不补体验债的前提下直接把重心切到 Phase5。** 进入下一阶段前，至少应先修正职业装备收敛、隐藏内容体验指标盲区、以及局部奖励池重叠过高这三类问题。

## 2. 审阅范围与依据

### 2.1 参考文档

- `docs/phase4/roadmap.md`
- `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
- `docs/phase4/2026-03-13-phase4-pr-01-mapgen-and-solvability.md`
- `docs/phase4/2026-03-13-phase4-pr-02-elite-mutation-and-boss-variants.md`
- `docs/phase4/2026-03-13-phase4-pr-03-terrain-and-hazard-semantic-layer.md`
- `docs/phase4/2026-03-13-phase4-pr-04-hidden-content-events-and-secret-zones.md`
- `docs/phase4/2026-03-13-phase4-pr-05-loot-v2-weights-and-budget.md`
- `docs/phase4/2026-03-13-phase4-pr-06-content-pack-manifest-and-overlay.md`
- `docs/phase4/2026-03-13-phase4-pr-07-phase4-integration-vertical-slices.md`
- `docs/phase4/2026-03-13-phase4-pr-08-tools-labs-and-phase4-report.md`
- `docs/phase4/2026-03-13-phase4-pr-09-content-expansion-balance-and-release-hardening.md`
- `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
- `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-01-experience-metrics-baseline-and-phase4-report-expansion.md`
- `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md`
- `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-03-equipment-passive-vocabulary-and-item-content-density.md`
- `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-04-loot-profile-v3-and-reward-pool-differentiation.md`
- `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-05-hidden-content-diversity-and-entrance-entropy.md`
- `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-06-terrain-readability-and-tactical-uptake-tuning.md`
- `docs/review/phase4/opt/2026-04-11-phase4-opt-pr-06-post-dev-review.md`

### 2.2 代码 / 数据 / 报告锚点

- 运行时与系统装配
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
  - `game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt`
  - `game/src/main/kotlin/com/ktome/game/hidden/HiddenContentMapgenPipeline.kt`
  - `game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt`
  - `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt`
  - `game/src/main/kotlin/com/ktome/game/harness/SmokeBot.kt`
- 客户端表现与解释性
  - `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
- 内容数据
  - `game/src/main/resources/data/elites/index.yaml`
  - `game/src/main/resources/data/boss-variants/index.yaml`
  - `game/src/main/resources/data/events/index.yaml`
  - `game/src/main/resources/data/secret-zones/index.yaml`
  - `game/src/main/resources/data/loot/index.yaml`
  - `game/src/main/resources/data/items/index.yaml`
  - `game/src/main/resources/data/professions/index.yaml`
  - `game/src/main/resources/i18n/zh-CN.json`
- 现有自动化与生成报告
  - `tools/build/reports/phase4/phase4-summary.md`
  - `tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-summary.json`
  - `tools/build/reports/phase4/whitebox/solvability/whitebox-solvability-summary.json`
  - `tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json`
  - `tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json`
  - `tools/build/reports/phase4/hidden/hidden-content-summary.json`
  - `tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md`
  - `build/reports/harness/long-run-full.md`
  - `build/reports/harness/solo-clear-lab.md`
  - `build/reports/harness/headless-smoke.md`

### 2.3 审阅方法

1. 先按 `phase4` 主文档与 `opt PR` 文档归纳每个系统的设计目标、验收口径与体验意图。
2. 再把这些设计项映射到当前运行时代码、数据资源与自动化报告，判断是“合同成立”还是“体验成立”。
3. 最后从玩家角度重构实际体验链：探索 -> 战斗 -> 奖励 -> 成长 -> 再探索，重点检查是否存在伪完成、负资产设计、或者会把后续内容扩张建立在错误地基上的问题。

说明：本次结论基于仓库当前代码、数据和已生成报告进行审阅，没有把“理论上应当如此”当成证据。

## 3. Phase4 设计实现一致性矩阵

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| ProcGen / `MapgenPipeline` / `SolvabilityGraph` | Phase4 需要把地图生成从“能出图”推进到“可达、可验证、有差异、可挂接隐藏内容”的正式管线。 | 已实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-01-mapgen-and-solvability.md`；实现：`game/src/main/kotlin/com/ktome/game/hidden/HiddenContentMapgenPipeline.kt`；报告：`tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-summary.json`、`tools/build/reports/phase4/whitebox/solvability/whitebox-solvability-summary.json` | 合同层基本成立，白盒与 solvability 证据完整。问题不在“没做”，而在个别前期 zone 的差异度只刚刚过线。 | Low |
| 精英变异 / Boss 变体 | 需要形成可组合、可排斥、可地形联动、可驱动 Boss 区分的正式 encounter 装饰层。 | 已实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-02-elite-mutation-and-boss-variants.md`、`docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md`；实现：`game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt`、`game/src/main/resources/data/elites/index.yaml`、`game/src/main/resources/data/boss-variants/index.yaml`；报告：`tools/build/reports/phase4/phase4-summary.md` | 运行时已把 `preferredTerrainTags`、双变异、Boss 变体、排斥关系打通，且体验指标达到目标。当前没有明显偏离。 | Low |
| 地形语义层 / 战术地形 | 需要让地形从背景装饰变成战斗语义的一部分，并能被玩家识别、利用、验证。 | 已实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-03-terrain-and-hazard-semantic-layer.md`、`docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-06-terrain-readability-and-tactical-uptake-tuning.md`；实现：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、`client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`、`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`；报告：`tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json`、`tools/build/reports/phase4/phase4-summary.md` | 合同与指标都已成立，`terrainTaggedCombatExposureRate=40.0%`、`terrainInteractionEncounterRate=16.6%`。偏差只在表现层：核心说明较多沉在 inspect/log，不是完全“零理解成本”。 | Medium |
| 隐藏内容 / 搜索 / Secret Zone | 需要形成“触发条件 -> 搜索揭示 -> 隐藏事件 -> Secret Zone/奖励”的正式闭环，并具备多入口、多触发类型与可验证性。 | 部分实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-04-hidden-content-events-and-secret-zones.md`、`docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-05-hidden-content-diversity-and-entrance-entropy.md`；实现：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、`game/src/main/resources/data/events/index.yaml`、`game/src/main/resources/data/secret-zones/index.yaml`；报告：`tools/build/reports/phase4/hidden/hidden-content-summary.json`、`tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md`；Harness：`tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt` | 合同层闭环成立，但体验验收存在盲区。当前 100% 发现率主要来自脚本化 primer + 定点搜索，不足以证明自然游玩中的发现驱动成立。 | High |
| Loot Profile V3 / 奖励池分化 | 需要把掉落从“随机发装备”推进到按 zone / 路线 / 奖励来源分化的正式奖励系统。 | 部分实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-05-loot-v2-weights-and-budget.md`、`docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-04-loot-profile-v3-and-reward-pool-differentiation.md`；实现：`game/src/main/resources/data/loot/index.yaml`、`game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt`；报告：`tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json`、`tools/build/reports/phase4/phase4-summary.md` | 平均分化达标，但同一 zone 内若干 reward/secret pool 仍接近子集关系，局部重叠过高，秘密奖励的身份感不够强。 | High |
| 装备词汇 / 被动效果 / 构筑驱动 | 需要通过 affix、unique、被动词汇把“拿到装备”转化为“改变打法”的真实驱动。 | 偏离实现 | 文档：`docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-03-equipment-passive-vocabulary-and-item-content-density.md`；实现：`game/src/main/resources/data/items/index.yaml`、`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、`client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`；报告：`build/reports/harness/long-run-full.md`、`tools/build/reports/phase4/phase4-summary.md` | 词汇量与被动展示本身已经存在，但真实长局中多职业终盘装备底盘高度收敛到 `battle_axe`，说明装备系统还没有把职业差异稳稳保住。系统“做出来了”，但玩家身份表达还没有跟上。 | Critical |
| 内容包 / Overlay / Manifest 边界 | 需要允许 content pack 对内容注册表做 overlay，而不越权篡改 `core` 规则语义。 | 已实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-06-content-pack-manifest-and-overlay.md`；实现：`game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt`；测试：`game/src/test/kotlin/com/ktome/game/contentpack/DataLoaderContentPackTest.kt` | 从当前代码与测试看，边界控制是成立的。Phase4 该项更偏基础设施，不是主要体验短板。 | Low |
| 纵切集成 / 长局闭环 | 需要把 ProcGen、奖励、Boss、隐藏内容、成长串成一个能打通的长局。 | 部分实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-07-phase4-integration-vertical-slices.md`、`docs/phase4/roadmap.md`；报告：`build/reports/harness/solo-clear-lab.md`、`build/reports/harness/long-run-full.md`、`build/reports/harness/headless-smoke.md` | 长局“能打穿”已经成立，但“为什么想再打一局”仍不够锐利。主要短板不是流程断裂，而是成长分化和探索回报识别度不足。 | High |
| 工具 / 报告 / 体验指标 | Phase4 需要把验证从纯 correctness 扩展到 experience-facing 指标。 | 偏离实现 | 文档：`docs/phase4/2026-03-13-phase4-pr-08-tools-labs-and-phase4-report.md`、`docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-01-experience-metrics-baseline-and-phase4-report-expansion.md`；实现/报告：`tools/build/reports/phase4/phase4-summary.md`、`tools/build/reports/phase4/whitebox/*` | 当前指标体系很强，但仍有两个盲区：一是用“平均 overlap”掩盖局部奖励池扁平化；二是用脚本化 hidden harness 代替自然发现率。文档验收口径本身需要修正。 | High |
| UI / 日志 / 可解释性 | 需要保证玩家能理解地形、变异、奖励、搜索结果与被动效果。 | 已实现 | 实现：`client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`、`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`、`game/src/main/resources/i18n/zh-CN.json`、`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`；测试：`game/src/test/kotlin/com/ktome/game/RenderSnapshotContractTest.kt`、`game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt` | 解释性路径存在，且 key/日志/inspect 已打通。问题是它偏“可查阅”，而不是始终“足够显眼”。这会增加新手和中强度玩家的理解负担。 | Medium |
