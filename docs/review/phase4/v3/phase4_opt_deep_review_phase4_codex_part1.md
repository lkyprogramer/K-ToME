# phase4 深度审查报告（Part 1）

## 1. 执行摘要

1. 当前 Phase4 的总体状态不是“功能缺失”，而是“合同与工具链完成度高，但玩家侧体验闭环仍然偏空”。更准确的判断是：**功能完成、基本可玩并具备耐玩雏形，但还没有达到稳定好玩的完成态**。
2. Phase4 最强的部分不是即时玩法，而是 contract、harness、whitebox report、content-pack runtime、seed 可复现性与 phase gate 结构。这套地基对后续维护非常有价值。
3. 当前最危险的问题不是代码质量，而是**主线节奏塌陷**。若中后段地图主要变成“快进到目标点/楼梯/交互点”，探索->战斗->奖励 的主循环会被掏空，Phase5 的 AI/稳定性优化会建立在偏空的玩法地基上。
4. 当前奖励生态“形式上已完成”，但**构筑分化仍然偏弱**。职业 aware loot 已经存在，affix/unique/artifact 也已落地，但终局武器与奖励吸附仍明显收敛，导致“这局想试另一套 build”的驱动力不足。
5. hidden content 系统从合同角度是成立的，但**organic hidden loop 的 owner metric 目前偏假绿**。它能证明“玩家看到了 hidden 相关信号”，但不能证明“玩家真的完成了线索->搜索->入口->秘密区->独特回报”的闭环。
6. boss variant 目前更像“contract-safe 的内容分支”，还不是“会被玩家清晰感知到的差异战斗体验”。至少一类 terrain preference 在现状下几乎不生效，变体存在感不足。
7. 文档与实现之间最大的错位，不是大量未实现，而是**若干系统达到了“能跑、能测、能报表”的工程完成度，但没有同步达到“玩家能感知到价值”的体验完成度**。
8. `docs/review/phase4/v2opt` 的方向基本正确，尤其是 experience gate、build identity、organic hidden loop、frontstage readability 这几条。但当前工作区状态显示，这些优化里仍有部分只完成了一半，或者 owner 指标未能真实约束体验目标。
9. 当前版本已经具备“继续玩下去的基础理由”，但还不足以支撑“重复刷多局仍然稳定有趣”。更准确地说，它是**能成立的系统骨架 + 初步可玩内容**，不是稳态耐玩的 Phase4 完成态。
10. 结论上，**不建议直接把主开发重心完全切到 Phase5**。进入 Phase5 前，至少应先修复关键路径节奏、build/reward 收敛、hidden loop 假绿、boss variant 体感不足、version/pack gating 漂移这几类 Phase4 内生问题。

补充判断：从用户体验提升的性价比看，当前阶段最值得补的一条，不是再加一批“纯低概率爆出的职业终极武器”，而是把现有 unique / artifact 升级为**每个基础职业 1 到 2 个可追逐的终局锚点装备**。主路径应通过事件、秘密区、条件 boss/finale reward 提供可追求获取，低概率直掉只作为惊喜捷径。

## 2. 审阅范围与依据

### 2.1 参考文档

本次审阅重点参考以下文档：

- [docs/mvp-development-guide.md](/Users/luo/Documents/github/K-ToME/docs/mvp-development-guide.md)
- [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](/Users/luo/Documents/github/K-ToME/docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
- [docs/2026-03-13-core-systems-design-and-phase-supplements.md](/Users/luo/Documents/github/K-ToME/docs/2026-03-13-core-systems-design-and-phase-supplements.md)
- [docs/phase4/roadmap.md](/Users/luo/Documents/github/K-ToME/docs/phase4/roadmap.md)
- [docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)
- [docs/phase4/2026-03-13-phase4-verification-checklist.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-verification-checklist.md)
- [docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md)
- [docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md)
- [docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md)
- [docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md)
- [docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md)
- [docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md)
- [docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md)
- [docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md)
- [docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md)
- [docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md)
- [docs/review/phase4/v2opt/README.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/README.md)
- [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md)
- [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md)
- [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md)
- [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-04-terrain-mutation-semantics-and-theme-hardening.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-04-terrain-mutation-semantics-and-theme-hardening.md)
- [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md)
- [docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md)
- [docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part1.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part1.md)
- [docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part2.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part2.md)
- [docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part3.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part3.md)
- [docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part4.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_claude_part4.md)

### 2.2 核查的实现与工件

本次没有重新跑 Gradle gate，而是基于当前工作区代码、数据资源、现有 harness/report 工件做审阅。核心锚点包括：

- 运行时与数据：
  - [core/src/main/kotlin/com/ktome/core/mapgen/MapgenContracts.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/mapgen/MapgenContracts.kt)
  - [core/src/main/kotlin/com/ktome/core/loot/LootModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/loot/LootModels.kt)
  - [core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt)
  - [game/src/main/kotlin/com/ktome/game/GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt)
  - [game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
  - [game/src/main/kotlin/com/ktome/game/mapgen/SchemaZoneMapgenProfileResolver.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/mapgen/SchemaZoneMapgenProfileResolver.kt)
  - [game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt)
  - [game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt)
  - [game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt)
  - [game/src/main/kotlin/com/ktome/game/contentpack/GameBuildVersion.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/contentpack/GameBuildVersion.kt)
  - [game/src/main/resources/data/mapgen/zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/mapgen/zones/index.yaml)
  - [game/src/main/resources/data/mapgen/patterns/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/mapgen/patterns/index.yaml)
  - [game/src/main/resources/data/objectives/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/objectives/index.yaml)
  - [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
  - [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)
  - [game/src/main/resources/data/events/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/events/index.yaml)
  - [game/src/main/resources/data/secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml)
  - [game/src/main/resources/data/elites/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/elites/index.yaml)
  - [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml)
  - [examples/content-packs/sample.flooded_relics/manifest.yaml](/Users/luo/Documents/github/K-ToME/examples/content-packs/sample.flooded_relics/manifest.yaml)
- client 表现与前台可读性：
  - [client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt)
  - [client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt)
  - [client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt)
  - [client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt)
  - [client/src/main/kotlin/com/ktome/client/render/RewardPresentationText.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/RewardPresentationText.kt)
- 报告与白盒工件：
  - [tools/build/reports/verification/phase4/report-phase4-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/verification/phase4/report-phase4-summary.md)
  - [tools/build/reports/phase4/phase4-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/phase4-summary.md)
  - [tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md)
  - [tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-report.md)
  - [tools/build/reports/phase4/whitebox/loot/whitebox-loot-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/loot/whitebox-loot-report.md)
  - [tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-report.md)
  - [tools/build/reports/phase4/whitebox/boss/boss-harness.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/boss/boss-harness.md)
  - [tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md)
  - [tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-report.md)
  - [build/reports/harness/long-run-summary.md](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-summary.md)
  - [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json)
  - [build/reports/harness/solo-clear-lab.md](/Users/luo/Documents/github/K-ToME/build/reports/harness/solo-clear-lab.md)

### 2.3 审阅方法

本次审阅采用四步法：

1. 先按 `docs/phase4` 与 `docs/review/phase4/v2opt` 拆出设计承诺、owner metric、体验目标与非目标。
2. 再把设计项映射到当前代码、数据、资源与现有白盒/报表工件，区分“已实现”“部分实现”“偏离实现”“伪完成”。
3. 然后不以“能运行”作为完成判据，而是重构玩家在 Phase4 当前状态下的完整体验链路，检查核心循环、奖励驱动、成长分化、探索新鲜感与信息反馈是否形成正循环。
4. 最后只保留对当前 Phase4 有决定意义的问题。凡是当前阶段就应该修、且不修会把后续内容搭在坏地基上的项，都直接判为本阶段必须处理。

### 2.4 审阅边界说明

1. 本报告关注的是 **当前工作区状态**，而不是只看某个历史提交。因此会同时参考当前未提交代码与已有报表。
2. 部分现有 report 工件早于当前工作区里的 PR-05 调整，尤其是 `frontstage readability`、`greenwood` 第二秘密分支等。因此报告会明确区分“代码已存在”与“owner report 尚未覆盖”。
3. 本次没有重新执行 `./gradlew` 系列 gate，因此凡是量化判断，均基于现有工件与源码交叉验证，而不是新跑出来的数字。
4. 本轮已将 Claude 的 4 份 v3 审阅报告作为外部结论源再次核实，但**只吸纳能被当前源码、YAML 和现有工件同时支撑的判断**。
5. 经核实后未采纳的 PR-05 旧问题包括：
   - frontstage 玩家标签仍显示 `Frontstage`
   - `recentFrontstageActionCues` 没有大小上限
   这两条在当前工作区已经被修正为 `Awareness / 态势感知` 与 `FRONTSTAGE_RECENT_ACTION_CUE_CAP`，不再列为现存问题。

## 3. Phase4 设计实现一致性矩阵

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Mapgen contract / biome / topology baseline | 建立 `MapgenPipeline`、`BiomeFamilyDef`、zone profile、pattern/vault 基线，并由 `mapgenSmoke` 与白盒报告兜底 | 已实现 | [docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md), [docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md), [core/src/main/kotlin/com/ktome/core/mapgen/MapgenContracts.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/mapgen/MapgenContracts.kt), [game/src/main/resources/data/mapgen/zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/mapgen/zones/index.yaml), [tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-report.md) | 合同、资源与报告面都成立，属于 Phase4 最稳的部分之一。当前偏差主要不在“有没有系统”，而在关键路径地图实际节奏未被这些合同约束住。 | Low |
| Hybrid topology / replay hook / floor-aware zone profile | 通过多 profile、pattern room、路线扰动提升重复游玩中的路径差异和早期 replay 价值 | 部分实现 | [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md), [game/src/main/kotlin/com/ktome/game/mapgen/SchemaZoneMapgenProfileResolver.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/mapgen/SchemaZoneMapgenProfileResolver.kt), [game/src/main/resources/data/mapgen/zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/mapgen/zones/index.yaml), [game/src/main/resources/data/mapgen/patterns/hideout_switchback.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/mapgen/patterns/hideout_switchback.yaml) | 当前工作区已加入 `greenwood_fringe` floor-aware profile 与新 pattern，但现有 owner report 尚未覆盖，且新秘密分支的奖励/视觉身份仍复用旧 cache，导致“布局变化 > 玩家记忆点变化”。 | Medium |
| Solvability graph / SearchAction / hidden entrance | 建立 secret entrance、SearchAction、solvability/可达性图与对应 harness，使隐藏入口既可验证又不脱离主路径 | 已实现 | [docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md), [tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md), [tools/build/reports/phase4/phase4-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/phase4-summary.md) | 从合同和白盒验证看已闭合，问题不在“入口不存在”，而在 organic hidden owner metric 没能真正度量“闭环成功”。 | Low |
| LootBudget / rarity / pity / reward profile | 建立分层奖励来源、budget、rarity、pity、防止奖励失控，并让奖励可被 lab 度量 | 已实现 | [docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md), [core/src/main/kotlin/com/ktome/core/loot/LootModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/loot/LootModels.kt), [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml), [tools/build/reports/phase4/whitebox/loot/whitebox-loot-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/loot/whitebox-loot-report.md) | 工程与合同层面完成度高，问题在于 live 体验中 reward slot 过度偏向 weapon，成长驱动被拉向单一武器主导。 | Low |
| Dynamic loot pool coverage / zone reward identity | profession-aware loot 不应只在少数热点区成立，而要在主路径中后段持续提供区域化奖励身份 | 部分实现 | [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md), [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml) | 当前只有 `greenwood_fringe`、`deep_iron_pit` 的 cadence/reward 以及 `underground_river.cadence`、`abyssal_temple.cadence` 进入 `TAG_WEIGHTED`；`shattered_outpost`、`bandit_camp`、`elven_ruins`、`molten_core`、`grey_gate_depths`、`crystal_cavern` 及多个 reward/finale profile 仍为 `FIXED_LIST`，中后段奖励惊喜直接变平。 | High |
| Affix / unique / artifact / loot balance lab | 让奖励不只是数值抬升，而能形成 build identity、记忆点与 adoption 数据 | 部分实现 | [docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md), [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md), [game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt), [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json), [build/reports/harness/long-run-summary.md](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-summary.md) | 系统存在，但 build-aware 权重仍偏弱，templar 与 vanguard 终局武器明显收敛，reward adoption 和 affix 激活也说明“形式上有丰富度，体感上仍偏平”。 | High |
| Special-tier identity（unique / artifact） | 高稀有度掉落应成为可感知的 build 锚，而不是只在普通 affix 语义上抬高数值和固定组合 | 部分实现 | [core/src/main/kotlin/com/ktome/core/item/ItemModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/item/ItemModels.kt), [game/src/main/kotlin/com/ktome/game/data/DataLoader.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/data/DataLoader.kt), [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml) | 当前 special template 合同只提供 `fixedAffixIds / fixedMaterialId`，而 unique/artifact 自身虽已带 `OnHitStatusProc`、`TerrainAffinityBonus`、`ConditionalStatBonus` 等通用 passive，但 special-tier 身份主要仍靠“预烘焙组合的通用词汇”成立，拉升深度不够。问题不是“完全没有身份”，而是“special-tier 还不够 special”。 | Medium |
| Terrain interaction / elite mutation | 让地形、elite mutation、zone theme 进入战斗语义，而不只是地图背景 | 已实现 | [docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md), [game/src/main/resources/data/elites/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/elites/index.yaml), [tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-report.md) | 结构已成立，但受关键路径战斗密度不足影响，这套语义没有被持续放大到玩家层面，部分 zone 的“主题即玩法”仍不够有感。 | Medium |
| Boss variant differentiation | 让变体 boss 在不破坏 phase/boss contract 的前提下形成可被感知的战斗差异与主题强化 | 偏离实现 | [docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md), [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml), [tools/build/reports/phase4/whitebox/boss/boss-harness.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/boss/boss-harness.md) | harness 表明多个 variant 的 selected actions / trace hash 与 base 基本一致；同时源数据本身也偏薄：当前只有 3 个 boss variant、每个 `actionWeightProfile` 只有 2–3 个 action，且不存在 `phaseOverrides` 一类阶段切换合同。`abyssal_eclipse` 还出现 `preferredTerrain=[OIL]` 但 `preferenceAvailable=false`。 | High |
| Hidden event / secret zone / reward bridge | 让隐藏事件、秘密区、奖励桥接到主玩法，并可由 harness 与白盒验证 | 已实现 | [docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md), [game/src/main/resources/data/events/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/events/index.yaml), [game/src/main/resources/data/secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml), [tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md) | 功能面与 whitebox 面都在，属于“系统存在且能跑”的完成项。主要问题出在 organic loop 的 owner metric 与 secret reward identity 没完全约束住实际体验。 | Low |
| Organic hidden loop / secret reward identity | 文档要求隐藏内容不只是“被发现”，而是形成线索、搜索、入口、秘密区、独特奖励的有机循环 | 偏离实现 | [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md), [tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt), [tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md), [game/src/main/resources/data/secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml) | `discovered` 目前把 hidden event / binding / secret zone 任一命中都算发现，导致 `abyssal_temple` 这类 `discoveryRate=100%` 但 `secretEntryRate=0%` 的假绿结果。再加上新 greenwood 分支仍复用旧 reward/visual，体验闭环仍不扎实。 | High |
| Profession-aware loot / terminal build identity | v2opt 要求 rewards 更懂职业语义，减少不同职业在后期掉落上的收敛 | 部分实现 | [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md), [game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt), [game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt), [game/src/main/resources/data/items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml), [build/reports/harness/long-run-full.json](/Users/luo/Documents/github/K-ToME/build/reports/harness/long-run-full.json) | 职业 aware 逻辑存在，但实际权重不够强，且 item taxonomy 本身让 `long_sword` 成为多职业的共同吸附点。系统不是没做，而是没把结果拉开。 | High |
| Content pack runtime / overlay / pack lint / sample pack | Phase4 末应建立 namespaced、可 lint、可版本校验、可 sample 验证的 content pack path | 已实现 | [docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md), [docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md), [game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt), [tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-report.md](/Users/luo/Documents/github/K-ToME/tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-report.md) | 这是当前 Phase4 最完整、最可持续的产物之一。路径、lint、fixtures、sample pack 都已经形成“以后可扩内容”的可靠边界。 | Low |
| Version discipline / gameVersionRange / build identity | 文档要求 Phase4 有明确版本纪律，content pack 版本约束必须与当前 phase/build 对齐 | 偏离实现 | [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](/Users/luo/Documents/github/K-ToME/docs/2026-03-13-phase2-to-phase5-final-roadmap.md), [docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md), [gradle.properties](/Users/luo/Documents/github/K-ToME/gradle.properties), [client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt), [examples/content-packs/sample.flooded_relics/manifest.yaml](/Users/luo/Documents/github/K-ToME/examples/content-packs/sample.flooded_relics/manifest.yaml) | 文档语义已进入 Phase4，但 runtime/version range 仍停在 `0.1.0` 口径。这不是文案问题，而是 pack gating、示例清晰度和 release discipline 的漂移。 | High |
| Frontstage readability / reward surface readability | 提升前台文本、奖励回显、可理解性，减少玩家在 hidden/reward/路线变化上的理解成本 | 部分实现 | [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md), [docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md), [core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt), [game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt), [client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt), [client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt), [game/src/main/resources/i18n/en-US.json](/Users/luo/Documents/github/K-ToME/game/src/main/resources/i18n/en-US.json), [game/src/main/resources/i18n/zh-CN.json](/Users/luo/Documents/github/K-ToME/game/src/main/resources/i18n/zh-CN.json) | 当前工作区已实现 `frontstageReadability`、`recentRewards.detailText`、`Awareness / 态势感知` 标签与 cue cap，说明 PR-05 的基础修口已经到位；残留问题不再是“有没有通道”，而是 `recentActionHighlights` 仍只按最近两条回放，passive/search/hidden 共用 ACTION 通道，缺少 source priority。 | Medium |

### 3.1 一致性矩阵后的总体判断

1. 从“有没有按文档做完”看，Phase4 不是失败状态，绝大多数承诺功能都已经有实体系统、资源、测试入口和报表锚点。
2. 从“是否按设计意图工作”看，最大的偏差集中在玩家感知层，而不是基础 contract 层。
3. 从“功能完成但体验不成立的伪完成项”看，最典型的是三类：
   - build-aware reward 已存在，但 build identity 仍偏收敛；
   - hidden loop 已存在，但 owner metric 可以在秘密区根本没转化时仍显示绿色；
   - boss variant 已存在，但战斗体感差异不足。
4. Claude 报告里真正被本轮吸纳的补充点，主要有三类：
   - 中后段 reward pool 的动态化覆盖不足；
   - special-tier identity 仍主要依赖通用 passive 组合；
   - PR-05 剩余问题集中在 action cue priority，而不是 label/cap。
5. 从“文档没要求但实现多做了可能破坏体验的内容”看，当前没有明显越界到 Phase5 的大系统；真正的问题更像是 **工具/合同侧比玩家体验侧更先完成**，造成“工程完成度高于玩法完成度”的结构性错位。
