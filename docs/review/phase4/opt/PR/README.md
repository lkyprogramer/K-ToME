# Phase 4 Review Follow-up PR 文档索引

本目录把 [2026-04-08-phase4-verified-optimization-pr-plan.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md) 中的优化项，整理成可直接实施的标准单 PR 设计文档。

## 拆分结果

| 标准 PR | 来源 | 说明 |
| --- | --- | --- |
| `OPT PR-01` | 原 `OPT-PR-01` | 体验度量基线与 `phase4Report` 扩域 |
| `OPT PR-02` | 原 `OPT-PR-02` | 精英突变补齐与 Boss 变体差异拉开；长期保留的 elite 行为改为 dedicated talent id |
| `OPT PR-03` | 原 `OPT-PR-03 + OPT-PR-04` | 合并为一个 PR：先扩 `EquipmentPassive` 词汇，再落 affix / unique / artifact 内容密度，减少中间态返工 |
| `OPT PR-04` | 原 `OPT-PR-05` | `LootProfile V3` 与奖励池差异化；采用单 schema 破坏式迁移，不保留 V2/V3 双轨 |
| `OPT PR-05` | 原 `OPT-PR-06` | Hidden content 多样化与入口熵提升；若现有 anchor 不足，允许同步升级 topology/anchor 合同 |
| `OPT PR-06` | 原 `OPT-PR-07` | 地形可感知性与战术 uptake 调优；若 terrain affinity 成为正式语义，提升为显式数据字段 |

## 文档列表

1. [2026-04-09-phase4-opt-pr-01-experience-metrics-baseline-and-phase4-report-expansion.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-01-experience-metrics-baseline-and-phase4-report-expansion.md)
2. [2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md)
3. [2026-04-09-phase4-opt-pr-03-equipment-passive-vocabulary-and-item-content-density.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-03-equipment-passive-vocabulary-and-item-content-density.md)
4. [2026-04-09-phase4-opt-pr-04-loot-profile-v3-and-reward-pool-differentiation.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-04-loot-profile-v3-and-reward-pool-differentiation.md)
5. [2026-04-09-phase4-opt-pr-05-hidden-content-diversity-and-entrance-entropy.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-05-hidden-content-diversity-and-entrance-entropy.md)
6. [2026-04-09-phase4-opt-pr-06-terrain-readability-and-tactical-uptake-tuning.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-06-terrain-readability-and-tactical-uptake-tuning.md)
