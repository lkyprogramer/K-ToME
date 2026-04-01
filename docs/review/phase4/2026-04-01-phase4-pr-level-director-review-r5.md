# Phase 4 PR 级开发文档复审报告 R5

**日期**: `2026-04-01`  
**审阅视角**: 资深游戏设计 / 开发总监  
**审阅轮次**: 第五轮残余一致性复审  
**审阅范围**:
1. `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
2. `docs/phase4/roadmap.md`
3. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
4. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
5. `docs/phase4/2026-03-13-phase4-pr-01` ~ `pr-09`

## Findings

### P1

- **`pityTracker` 仍被同时写成 run-scoped 和 floor-scoped，两套持久化布局还没完全收干净**
  - 横切文档在 per-floor 持久化字段里仍把 `pityTracker` 列进去，[2026-03-13-phase4-cross-cutting-contracts.md:44](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:44)。
  - 但同一文档的 `Phase4RunState` 又把 `pityTracker` 放在 run 级，[2026-03-13-phase4-cross-cutting-contracts.md:98](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:98)。
  - `PR-04` 已经明确它是 run-scoped 状态，[2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:213](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:213)，但 `PR-01` 的预留说明仍写成要挂在 floor state 上，[2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:241](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:241)。
  - 这不是细枝末节，而是会直接决定 save/replay DTO 的形状。若现在不收干净，后面实现时极容易出现“一份 pity 挂 run，一份 pity 挂 floor”的重复状态。
  - 建议以 `PR-04` 的 run-scoped 口径为唯一真源，把 cross-cutting 和 `PR-01` 的 floor-state 表述一并回写。

### P2

- **typed-ref 纪律在 persistence / report 断面仍有漏口**
  - 横切文档已经明确“跨 loader、runtime、harness、report 传递的 ID，不再继续裸用 `String`”，[2026-03-13-phase4-cross-cutting-contracts.md:280](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:280)。
  - 但 `FloorGenerationState` 里 `revealedEntranceIds`、`visitedSecretZoneIds` 仍是 `Set<String>`，[2026-03-13-phase4-cross-cutting-contracts.md:92](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:92)。
  - 主文档的 `HarnessReportHeader` 里 `activePackIds` 还是 `List<String>`，`activePackManifestVersions` 还是 `Map<String, String>`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:1283](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1283)。
  - 这些字段都已经落在 save/replay/harness/report 正式边界上，不再是内部临时值。继续保留裸字符串，会让 typed-ref 纪律在最关键的追溯面上失效。
  - 建议至少收成：
    - `revealedEntranceIds: Set<SearchBindingId>`
    - `visitedSecretZoneIds: Set<ContentRef>` 或等价正式 ref
    - `activePackIds: List<PackId>`
    - `activePackManifestVersions: Map<PackId, String>`

- **主文档 `OverlayEntry` 结构仍落后于 `PR-08` 与其自身 overlay prose**
  - 主文档 `OverlayEntry` 目前只有 `targetRef / op / sourceFile`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:1069](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1069)。
  - `PR-08` 的正式结构已经包含 `fieldPath / mergePolicy / dedupeKey`，[2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:100](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:100)。
  - 主文档自己的 overlay 表格也明确写了，如果未来 `APPEND` 进入 runtime，必须补这三项字段，[2026-03-13-phase4-procgen-loot-and-content-pack.md:1095](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1095)。
  - 现在的问题是：主文档 code block 看起来像正式 schema，但它比 `PR-08` 少字段，和正文解释不一致。实现方很容易把主文档误读成“正式模型里根本没有这些字段”。
  - 建议把主文档 code block 升级到与 `PR-08` 同一结构，即便三项字段在 `Phase 4` 主路径默认不启用，也应保留为显式可选字段。

- **`hiddenContentHarness` 报告字段里仍有两个“文档内无定义”的幽灵字段**
  - `PR-07` 要求报告记录 `entryRuleId` 和 `revealCause`，[2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md:153](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md:153)。
  - 但当前 `Phase 4` 文档里，`SecretZoneDef` 暴露的是 `entryRule: DiscoveryRule`，不是 `entryRuleId`；`revealCause` 也没有任何统一枚举或字段定义。我全局检索 `docs/phase4`，这两个名字只出现在这一条报告要求里。
  - 这会迫使实现者临时决定：
    - `entryRuleId` 是不是 `DiscoveryRule` 的人工命名、hash，还是 pack-local key
    - `revealCause` 到底来自 `HiddenTriggerType`、`SearchActionResult`，还是另一套新枚举
  - 建议把这两个字段改成文档内已有真源，或者先在横切/主文档补出正式定义，再要求 harness 输出。

### P3

- **`PR-09` 的 pack 报告词汇开始偏离全局 reproducibility contract**
  - `PR-09` 里写的是 “pack run 报告增加 `packManifestVersion / packId / harnessSeeds / overlayContractVersion`”，[2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md:123](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md:123)。
  - 但全局 reproducibility contract 和 `HarnessReportHeader` 已经统一使用 `activePackIds / activePackManifestVersions`，[2026-03-13-phase4-verification-checklist.md:216](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-verification-checklist.md:216) [2026-03-13-phase4-procgen-loot-and-content-pack.md:1283](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1283)。
  - 对单 pack sample 来说，`packManifestVersion` 看似无害，但它会重新长出一套“单包报告词汇”，而不是继续沿用全局 active-pack 元数据模型。
  - 建议 `PR-09` 直接复用全局口径，哪怕 sample 只有一个 pack，也写成 `activePackIds / activePackManifestVersions` 的单元素形式。

## 结论

这轮之后，`Phase 4` 文档的主干合同已经基本成立，上一轮那种会直接导致 schema 分叉的大漂移基本没有了。现在剩下的都是最后一层“落实现时会不会逼人自己补定义”的问题。

其中最该优先处理的是 `pityTracker` 的层级归属，因为它会直接影响 save/replay 结构。其次是 typed-ref 在 persistence/report 断面的漏口，以及 `hiddenContentHarness` 的幽灵字段。这几处如果不清，会在实现时重新把“文档已经收好的东西”变回局部脑补。

## 建议的收口顺序

1. 先修 `pityTracker` 的 run-scoped / floor-scoped 冲突。
2. 再把 persistence/report 里的 pack / entrance / secret-zone 标识统一切回 typed ref。
3. 然后同步主文档 `OverlayEntry` 结构，避免主文档 code block 落后于 `PR-08`。
4. 最后把 `PR-07` 和 `PR-09` 的报告字段收回全局既有词汇，不再新造局部命名。
