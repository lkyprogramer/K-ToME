# Phase 4 PR 级开发文档复审报告 R6

**日期**: `2026-04-01`  
**审阅视角**: 资深游戏设计 / 开发总监  
**审阅轮次**: 第六轮残余一致性复审  
**审阅范围**:
1. `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
2. `docs/phase4/roadmap.md`
3. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
4. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
5. `docs/phase4/2026-03-13-phase4-pr-01` ~ `pr-09`

## Findings

### P2

- **[cross-cutting-contracts.md:30] `RunSaveData` 的 prose 最小字段列表在清掉 floor-scoped pity 后，漏掉了 run-scoped `PityTracker`**
  - 横切文档已经把 `pityTracker` 从 floor 级字段移除，这是对的；`Phase4RunState` 也已经把它固定在 run 级，[2026-03-13-phase4-cross-cutting-contracts.md:96](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:96)。
  - 但 `RunSaveData 至少保存` 的 prose 列表没有把它补回 run 级最小字段，[2026-03-13-phase4-cross-cutting-contracts.md:30](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:30)。
  - `PR-04` 仍明确要求 `PityTracker` 必须进入 save/replay/session，[2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:203](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:203)。
  - 当前风险不是“合同冲突”，而是“最小保存字段 prose 漏了一项关键 run state”。实现者如果按 prose 清单先落 DTO，仍可能把 pity persistence 漏掉。
  - 建议在 `RunSaveData 至少保存` 下显式补一条 `phase4RunState.pityTracker` 或等价 wording，避免代码块和 prose 各说一半。

- **[cross-cutting-contracts.md:166] `SearchAction / PerceptionScore` 的结构示例还没有完全对齐主文档与 PR-03 语义**
  - 横切文档的 `PerceptionScore` 带默认值和 `total` 属性，[2026-03-13-phase4-cross-cutting-contracts.md:166](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:166)；主文档 code block 则只保留四个字段，没有 `total`，也没有默认值，[2026-03-13-phase4-procgen-loot-and-content-pack.md:470](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:470)。
  - 横切文档的 `SearchAction` 只有 `bindingId + actorId`，[2026-03-13-phase4-cross-cutting-contracts.md:178](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:178)；主文档又额外带了 `energyCost: Int = 1000`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:477](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:477)。
  - `PR-03` prose 已经把正式语义写死为 `PerceptionScore.total` 和“真实判定时消耗一次标准 `1000` 能量行动”，[2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:176](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:176) [2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:198](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:198)。
  - 这意味着 prose 已统一，但 code block 还在漂。实现者如果照结构示例抄类型，仍可能在 `total` 和 `energyCost` 上各自脑补。
  - 建议选一套结构示例回写全部文档，优先以 `PR-03` 的正式语义为锚。

- **[pr-07-hidden-event-secret-zone-and-client-readability.md:153] `hiddenContentHarness` 现在改用了 `entranceBindingId`，但丢掉了真正与 `SearchAction` 对账的 `searchBindingId`**
  - verification checklist 记录项仍是 `searchBindingId / searchActionResult`，[2026-03-13-phase4-verification-checklist.md:160](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-verification-checklist.md:160)。
  - `SearchStateEntry` 的正式 join key 也是 `bindingId: SearchBindingId`，[2026-03-13-phase4-cross-cutting-contracts.md:79](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:79)；`SearchAction` 也是围绕 `bindingId` 定义，[2026-03-13-phase4-procgen-loot-and-content-pack.md:477](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:477)。
  - 但 `PR-07` 的 harness 字段改成了 `entranceBindingId`，[2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md:153](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md:153)，而 `HiddenEntranceDef` 里 `id` 才是 `SearchBindingId`，`entranceBindingId` 是 `NodeAnchorId`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:584](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:584)。
  - 这会让 hidden harness 与 search/replay/report 的正式 join key 错位。`entranceBindingId` 可以补充记录，但不能替代 `searchBindingId`。
  - 建议把 `searchBindingId` 加回必记录字段，`entranceBindingId` 若有价值可作为附加字段保留。

- **[pr-08-content-pack-overlay-loader-and-pack-lint.md:183] `contentPackHarness` 的输出定义仍然没有显式接到统一 `HarnessReportHeader`**
  - `PR-08` 目前只写到 `overlayContractVersion`，没有把 `activePackIds / activePackManifestVersions / seedList` 等全局 reproducibility 字段显式纳入固定输出，[2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:183](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:183)。
  - 但 verification checklist 已经把这些字段收进所有 batch/harness 的统一 contract，[2026-03-13-phase4-verification-checklist.md:203](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-verification-checklist.md:203)，主文档也已经冻结 `HarnessReportHeader`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:1274](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1274)。
  - `PR-09` 已经开始复用这套全局词汇，[2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md:123](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md:123)，反而让引入 `contentPackHarness` 的 `PR-08` 看起来像更旧的局部报告格式。
  - 建议在 `PR-08` 直接写明：`contentPackHarness` 的 summary/report 统一带 `HarnessReportHeader`，而当前条目列表只是业务载荷，不是完整头字段。

### P3

- **[cross-cutting-contracts.md:119] reward / threat DTO 的默认值还有最后一层 shape drift**
  - 横切文档里 `ZoneRewardProfile.baseRewardBudget` 仍是 `= 0`，`FloorRewardBudget.rewardDeltas` / `EncounterThreatBudget.threatDeltas` 没有默认值，[2026-03-13-phase4-cross-cutting-contracts.md:119](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:119)。
  - `PR-04` 的 `ZoneRewardProfile` 也沿用 `baseRewardBudget: Int = 0`，[2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:231](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:231)。
  - 但主文档把 `ZoneRewardProfile.baseRewardBudget` 改成了必填，同时给 `rewardDeltas / threatDeltas` 加了 `emptyList()` 默认值，[2026-03-13-phase4-procgen-loot-and-content-pack.md:684](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:684)。
  - 这已经不是概念冲突，而是最后一层“同名 DTO 的默认值口径”没有完全抹平。实现时通常不会出错，但仍会逼实现者决定“到底按哪个文档抄签名”。
  - 建议选一套默认值策略全量回写。若目标是表达“总账对象构建时允许空 delta 列表”，那就把 cross-cutting 也同步成带默认值；若目标是强迫调用方显式提供空列表，那就把主文档改回去。

## Open Questions

- 无。当前残差都不是信息缺失，而是文档内已有内容还未完全对齐。

## Suggested Verification

- 再做一次纯文档一致性检查：
  - 搜索 `PityTracker`，确认它只作为 run-scoped 持久化状态出现，不再在最小 floor state 或 prose 最小字段里漂移。
  - 对照 `PerceptionScore / SearchAction` 的 code block 与 `PR-03` prose，确认 `total` 和 `energyCost` 不再双轨。
  - 搜索 `searchBindingId`、`entranceBindingId`，确认 `hiddenContentHarness` 报告字段既能对账 `SearchAction`，又不丢实例化桥接信息。
  - 搜索 `HarnessReportHeader`、`contentPackHarness`，确认 `PR-08` 直接接到统一报告头，而不是只列业务字段。

## Summary

这轮复审没有再发现新的大方向问题。`Phase 4` 的核心横切合同已经基本闭合，现在剩下的都是“最后一层结构示例、报告字段和 prose 最小要求”没有完全抹平。

最值得优先清的是两类问题：
1. save/replay 最小字段 prose 还没把 run-scoped `PityTracker` 说完整。
2. `SearchAction` 与 `contentPackHarness` 这两条横切路径，prose 已统一，但结构示例和 PR 级输出定义还没完全跟上。

其余残差已经降到默认值和字段清单层面的轻微漂移。如果把这几处一并收掉，`Phase 4` 文档基本就可以作为真正的最终收尾稿使用，后续实现方不需要再自行补语义。
