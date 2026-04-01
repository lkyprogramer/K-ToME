# Phase 4 PR 级开发文档复审报告 R4

**日期**: `2026-04-01`  
**审阅视角**: 资深游戏设计 / 开发总监  
**审阅轮次**: 第四轮残余一致性复审  
**审阅范围**:
1. `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
2. `docs/phase4/roadmap.md`
3. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
4. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
5. `docs/phase4/2026-03-13-phase4-pr-01` ~ `pr-09`

## Findings

### P1

- **`ContentRef` 仍然存在两套互不兼容的真源定义**
  - 横切文档把 `ContentRef` 定义为单字符串 typed ref，[2026-03-13-phase4-cross-cutting-contracts.md:273](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:273)。
  - 主文档和 `PR-03` 已经把它定义成 `registry + id` 结构，[2026-03-13-phase4-procgen-loot-and-content-pack.md:451](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:451) [2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:99](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:99)。
  - 这不是命名差异，而是引用模型差异。继续并存会让 loader、save/replay、harness、content registry 再次各自选择“内容引用到底是 namespaced string 还是结构化 pair”。
  - 建议直接以主文档 / `PR-03` 这套 `registry + id` 为正式真源，并回写横切文档的 typed-ref 章节，避免横切文档继续成为过时抽象。

- **reward / threat ledger 仍未真正收成单一合同**
  - 横切文档里 `FloorRewardBudget` 是 `floorId + rewardDeltas`，`ZoneRewardProfile` 不含 `id`，[2026-03-13-phase4-cross-cutting-contracts.md:118](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:118) [2026-03-13-phase4-cross-cutting-contracts.md:130](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:130)。
  - 主文档里 `ZoneRewardProfile` 带 `id`，`FloorRewardBudget` 变成 `zoneId + floorIndex + baseBudget`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:680](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:680) [2026-03-13-phase4-procgen-loot-and-content-pack.md:688](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:688)。
  - `PR-04` 的 sibling `ZoneRewardProfile` 又回到无 `id` 版本，[2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:226](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:226)。
  - 同时，verification checklist 和主文档的 `HarnessReportHeader` 已经使用 `rewardLedgerVersion`，[2026-03-13-phase4-verification-checklist.md:210](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-verification-checklist.md:210) [2026-03-13-phase4-procgen-loot-and-content-pack.md:1236](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1236)，但横切文档的版本字段清单并未纳入它，[2026-03-13-phase4-cross-cutting-contracts.md:279](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:279)。
  - 这说明“reward/threat 是横切总账”这件事在叙述上成立了，在 DTO 和 version contract 上还没有真正合龙。建议先冻结一套正式 DTO，再把 `rewardLedgerVersion` 补进横切版本纪律。

- **content pack sidecar schema 仍然不是单一真源**
  - 横切文档定义的 `ContentPackHarnessSpec` 只有 `fixturePacks + expectedOps`，[2026-03-13-phase4-cross-cutting-contracts.md:233](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:233)。
  - `PR-08` 又在此基础上加入 `overlayContractVersion`，[2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:117](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:117)。
  - 主文档 sidecar 示例则写成 `dualPackScenarios`，已经不是同一个 schema，[2026-03-13-phase4-procgen-loot-and-content-pack.md:1184](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1184)。
  - 这会让 `contentPackHarness` 的 fixture 作者无法判断“到底以结构定义、PR 扩展版，还是示例 YAML”为准。
  - 建议立即选定最终 sidecar schema，然后统一改横切文档、主文档示例和 `PR-08`。这已经不是说明文案问题，而是 QA 真源本身还在漂。

### P2

- **`SolvabilityProof` 在主文档与 `PR-03` 之间仍有字段集漂移**
  - 主文档只有 `criticalPathReachable / acquiredKeys / unresolvedRequirements / visitedNodes`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:525](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:525)。
  - `PR-03` 已经扩成 `optionalPathCount / secretPathCount / totalReachableNodes / reachabilityRatio`，[2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:153](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:153)。
  - 既然 checklist 和 harness 要消费扩展版统计，主文档就不该继续保留缩减版结构；否则实现时仍要先做一轮“主文档是不是只给最小 proof”的解释。

- **`PityTracker` 规则内部仍有一处关键语义冲突**
  - `PR-04` 第 2 条规则写的是“连续 20 次未出 `RARE+`，下次 `RARE` 权重乘以 `2.0`”，[2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:170](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:170)。
  - 但第 6 条又写“pity 只围绕 special tier upgrade 结果统计”，[2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:174](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md:174)。
  - `RARE` 明显属于基础 rarity roll，不属于 special tier upgrade。当前写法会让实现者不知道 rare pity 是保留，还是已经被统一并入 special tier pity。
  - 建议拆成 `rarePity` 与 `specialTierPity` 两条明确规则，或者删掉其中一条，避免同一 PR 内部自相矛盾。

- **typed-ref 纪律没有真正落实到 overlay contract**
  - 横切文档已经明确“跨 loader、runtime、harness、report 传递的 ID 不再继续裸用 `String`”，[2026-03-13-phase4-cross-cutting-contracts.md:262](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:262)。
  - 但主文档 `OverlayEntry` 仍然是 `registry: String`、`targetId: String`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:1034](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1034)；`PR-08` 也仍然是 `targetId: String`，[2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:92](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:92)。
  - 这会让 pack overlay 这条最依赖 deterministic addressing 的路径，继续停留在 stringly-typed contract。
  - 建议至少先统一成 `registry: RegistryId`，再明确 `targetId` 究竟采用 `ContentRef` 还是 `namespaced id` 的专用 typed ref，避免 overlay 成为 typed-ref 纪律的例外。

- **`HiddenEventReward` 的 payload 仍然是字符串，和正文口径不一致**
  - 主文档代码里 `HiddenEventReward` 仍是 `key + value: String`，[2026-03-13-phase4-procgen-loot-and-content-pack.md:969](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:969)。
  - 但紧接着正文又写“`rewards` 统一使用 typed 结构，不接受未声明前缀的自由字符串”，[2026-03-13-phase4-procgen-loot-and-content-pack.md:982](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:982)。
  - 这说明 reward key 已经 typed 了，但 reward payload 还没 typed；当前文档在“算不算 typed reward”上自我矛盾。
  - 建议为 `REVEAL_SECRET_ZONE / LOOT_PROFILE / TRIGGER_ENCOUNTER / GRANT_BUFF` 至少补出最小结构化 payload，不要把字符串解析责任再次下放给 loader、client 和 harness。

### P3

- **`PR-01` 的测试文案仍残留旧调用边界**
  - `PR-01` 正文已经明确正式入口是 `MapgenPipeline.run(request)`，[2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:152](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:152)。
  - 但测试条目仍写成“同一 `MapgenRequest` 在相同 resolver 输入与 seed 下生成的 `TopologyGraph` 稳定一致”，[2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:276](/Users/luo/Documents/github/K-ToME/docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:276)。
  - 这是小问题，但它会继续暗示“测试对象是旧级别的中间产物，而不是正式 pipeline 边界”。

## 结论

这轮文档已经明显进入“可实施前的最后清边”阶段。此前最危险的几类歧义基本都收住了，包括 `SearchAction` 与 discovery 的剥离、hidden entrance / secret zone 绑定统一、`ZoneRewardProfile` sibling 拆分、Tools/QA 骨架对齐，以及命名规范的横切收口。

现在剩下的问题不再是方向错，而是“少数横切 DTO 和示例仍未完全只剩一份真源”。这些点看起来都不大，但它们刚好都落在最容易让实现者自行补语义的地方：content refs、reward/threat ledger、pack sidecar、overlay target、hidden reward payload。若不先清零，后续实现阶段仍然会在局部重新长出第二套解释。

## 建议的最终收口顺序

1. 先把 `ContentRef`、`OverlayEntry`、reward/threat DTO 与对应 version 字段收成真正单一真源。
2. 再统一 `ContentPackHarnessSpec` 和主文档 sidecar YAML 示例，确保 fixture 作者不会按示例写出另一套 schema。
3. 最后同步主文档的 `SolvabilityProof`、`HiddenEventReward` 以及 `PR-01` 测试文案这类“不会改方向、但会制造实现噪音”的残差。

## 建议复查项

1. 搜索 `ContentRef(`，确认只剩一种定义。
2. 搜索 `FloorRewardBudget(`、`ZoneRewardProfile(`、`rewardLedgerVersion`，确认 DTO 与 version 纪律一致。
3. 搜索 `ContentPackHarnessSpec`、`dualPackScenarios`、`overlayContractVersion`，确认 sidecar schema 只剩一种。
4. 搜索 `OverlayEntry(`、`targetId: String`、`registry: String`，确认 overlay contract 不再绕过 typed-ref 规则。
5. 搜索 `HiddenEventReward(` 与 `value: String`，确认 reward payload 结构化完成。
