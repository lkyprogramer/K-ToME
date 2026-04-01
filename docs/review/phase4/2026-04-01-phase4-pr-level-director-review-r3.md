# Phase 4 PR 级开发文档复审报告 R3

**日期**: `2026-04-01`  
**审阅视角**: 资深游戏设计 / 开发总监  
**审阅轮次**: 第三轮残余问题复审  
**审阅范围**:
1. `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
2. `docs/phase4/roadmap.md`
3. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
4. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
5. `docs/phase4/2026-03-13-phase4-pr-01` ~ `pr-09`

## Findings

### P1

- **[docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:175] SearchAction 仍未真正从 discovery contract 中剥离**
  - 横切文档已经明确 `DiscoveryRule` 不再承载动作，只描述“搜索动作之后要过什么判定”；但主文档仍把 `REQUIRES_SEARCH_ACTION` 放在 `DiscoveryPredicateType` 中，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:479](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)；`PR-03` 又额外引入了 `DiscoveryTriggerType.SEARCH_ACTION`，[docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:104](../phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md)；`PR-07` 继续用 `ACTIVE_SEARCH` 作为 `HiddenTriggerType` 的一部分，[docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md:105](../phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md)。
  - 这说明“搜索是动作”这个收口只做了一半。现在仍然存在至少三条语义轴：`SearchAction`、`DiscoveryTriggerType.SEARCH_ACTION`、`HiddenTriggerType.ACTIVE_SEARCH`。实现时极易再次长出第二套事件分类。
  - 建议只保留一条正式路径：`SearchAction` 负责动作，`DiscoveryRule` 只保留判定，`HiddenEvent` 只保留事件触发。把 `REQUIRES_SEARCH_ACTION`、`DiscoveryTriggerType.SEARCH_ACTION`、`HiddenTriggerType.ACTIVE_SEARCH` 统一收回。

- **[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:573] Hidden entrance 与 secret zone 的绑定合同仍未完全统一**
  - 主文档的 `HiddenEntranceDef` 已经改成 `targetSecretZoneId: ContentRef + entranceBindingId: NodeAnchorId`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:573](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)；但 `PR-03` 里仍然是 `targetId: NodeId`，[docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:221](../phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md)。与此同时，主文档的 `SecretZoneDef` 仍要求 `entranceBindingId + returnBridgePolicy`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:588](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)，而 `PR-07` 的 `SecretZoneDef` 已经不包含 `entranceBindingId`，[docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md:82](../phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md)。
  - 这不是命名小差异，而是“入口到底绑定到内容 ID、运行时 node 还是 anchor”还没彻底冻结。`returnBridge` 的实例化规则也因此没有稳定锚点。
  - 建议选一套并全量回写：`HiddenEntranceDef` 指向 `ContentRef secretZoneId`，`SecretZoneDef` 保留 `entranceBindingId`，return bridge 再通过 anchor/policy 解析运行时节点。`PR-03` 和 `PR-07` 不应再各自维护不同版本。

- **[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:938] Hidden trigger taxonomy 仍然存在双份权威**
  - 主文档的 `HiddenTriggerType` 还是事件型枚举：`ENTER_ROOM / OPEN_CHEST / KILL_ELITE / INTERACT_TILE / QUEST_STEP / PERCEPTION_REVEAL`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:938](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)；但 `PR-07` 又重新定义成发现机制型枚举：`PASSIVE_DISCOVERY / ACTIVE_SEARCH / SCRIPTED_REVEAL`，[docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md:105](../phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md)。
  - 这会直接导致实现期在 hidden event loader、日志和 harness 里不知道该按“事件触发源”还是“发现方式”分桶。
  - 建议保留事件型 `HiddenTriggerType` 作为正式真源，把被动发现、主动搜索、脚本揭示降为 `DiscoveryRule` 或 reward 解释层的子语义，不要在 `PR-07` 继续单独维护第二套枚举。

### P2

- **[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:411] `ZoneRewardProfile` 的 sibling 拆分还没完全落干净**
  - 主文档已经明确 `ZoneRewardProfile` 是 `ZoneMapgenProfile` 的 sibling，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:189](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)，但同一份主文档的 `zone mapgen profile` YAML 示例仍然包含 `rewardProfileId`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:411](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)。
  - 这个例子会直接误导实现者把奖励配置继续塞回 mapgen profile。
  - 建议把示例拆成两份：`zone mapgen profile` 只保留地图字段；`zone reward profile` 独立给出绑定方式。否则 sibling 拆分只是 prose 正确、示例错误。

- **[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:121] 顶层 Tools/QA 视图仍然比 roadmap/checklist 落后一版**
  - `roadmap` 和 `verification-checklist` 已经把 `terrainInteractionBatch + bossHarness` 作为 `PR-06` 主验证线，[docs/phase4/roadmap.md:32](../phase4/roadmap.md) [docs/phase4/2026-03-13-phase4-verification-checklist.md:24](../phase4/2026-03-13-phase4-verification-checklist.md)；但主文档 `Tools/QA Lane` 仍只列了 `mapgenSmoke / solvabilityHarness / lootBalanceLab / hiddenContentHarness / contentPackHarness`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:121](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)。
  - 这类顶层入口图如果不同步，团队在快速浏览时仍会得到过期执行图。
  - 建议把主文档 `Tools/QA Lane` 也补齐到和 `roadmap/checklist` 完全一致，避免“正文修过了，入口摘要没修”的残余漂移。

- **[docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:205] `ContentPackHarnessSpec` 类型签名仍然跨文档不一致**
  - 横切文档把 `expectedOps` 写成 `List<String>`，[docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:205](../phase4/2026-03-13-phase4-cross-cutting-contracts.md)；`PR-08` 则写成 `List<OverlayOp>`，[docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md:117](../phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md)。
  - 既然这已经是 typed harness sidecar，就不应该在横切真源和 PR 文档里分别使用 stringly typed 和 enum typed 两套口径。
  - 建议统一成 `List<OverlayOp>`，或者如果考虑 sidecar 文本兼容，就显式声明“文件里是字符串，loader 解析后映射为 `OverlayOp`”。现在这点仍然不够清楚。

- **[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1215] `HarnessReportHeader` 里的版本字段仍是 nullable，但 checklist 已把它们升级成必需元数据**
  - 主文档 `HarnessReportHeader` 里 `topologyFingerprintVersion / rewardLedgerVersion / lootFormulaVersion / specialTierEligibilityVersion / secretRuleVersion / overlayContractVersion / searchActionVersion` 仍全部是可空字段，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1215](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)；但 checklist 已经把这些列为 reproducibility contract 的固定字段，[docs/phase4/2026-03-13-phase4-verification-checklist.md:203](../phase4/2026-03-13-phase4-verification-checklist.md)。
  - 这会让实现方自然把它们当成“能填就填”，而不是“缺失即不合规”。
  - 建议把这些字段至少在 `Phase 4` 相关 harness 上改成非空，或者明确只有不适用的旧 harness 才允许缺失。当前写成 nullable，会削弱你刚刚建立起来的版本纪律。

- **[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:461] `PerceptionScore` / `SearchBinding` 的 typed contract 仍有命名漂移**
  - 横切文档使用 `equipmentBonus / passiveBonus` 和 `SearchBindingId`，[docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:142](../phase4/2026-03-13-phase4-cross-cutting-contracts.md)；主文档改成了 `gearBonus / classBonus` 和 `SearchAnchorId`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:461](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)；`PR-03` 又回到 `SearchBindingId`，[docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md:183](../phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md)。
  - 这类 typed ref 一旦开始漂，后面就会把“是同一概念还是邻近概念”重新变成靠人脑记忆。
  - 建议选一套命名并全量回写。尤其 `SearchBindingId` 和 `SearchAnchorId` 不能同时存在于同一阶段合同里。

- **[docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md:155] pack id、namespace 和资源 key 前缀之间还缺一个明确的规范化规则**
  - 当前示例 pack id 是 `sample.flooded_relics`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1146](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)；namespace 是 `sample_flooded_relics`，[docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1150](../phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)；资源计划里的 key 前缀也是 `sample_flooded_relics.*`，[docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md:155](../phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md)。
  - 这看起来合理，但文档没有明确“点号 pack id 如何规范化成下划线 namespace / key prefix”。实现方如果自行发明规则，最终容易在 lint、manifest、资源生成脚本里出现不同口径。
  - 建议明确写一条规则：`packId`、`namespace`、资源 key prefix 是否允许不同，若不同，映射规则是什么，谁是运行时真源。

### P3

- **[docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:78] 横切文档里还残留几个未定义的占位类型**
  - `ResolvedEntranceBinding`、`SearchStateEntry`、`RewardDelta`、`ThreatDelta` 在横切文档中被直接引用，[docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:78](../phase4/2026-03-13-phase4-cross-cutting-contracts.md) [docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md:97](../phase4/2026-03-13-phase4-cross-cutting-contracts.md)，但没有最小骨架或“这里只是占位”的说明。
  - 这不会立刻破坏设计，但会降低横切真源文档的可执行性。
  - 建议要么补最小结构，要么明确标注为 placeholder，避免后续每个实现者各自脑补。

- **[docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:276] `PR-01` 的测试文字还残留旧调用边界**
  - 文档正文已经把入口收回到 `MapgenPipeline.run(request)`，[docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:39](../phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md)；但测试行为仍写成“同一 `MapgenRequest + seed + profile` 生成的 `TopologyGraph` 稳定一致”，[docs/phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md:276](../phase4/2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md)。
  - 这是小问题，但会让执行者怀疑 profile 是否仍然是调用参数。
  - 建议把这一句改成和正式入口一致的表述，顺手清掉这类残留旧词。

## Open Questions

- `HiddenEventReward` 当前在主文档里仍是 `key + value:String` 形式，而 `rewardProfileId / guaranteedContent / targetSecretZoneId` 已经全面 typed 化。如果团队准备继续加强 fail-fast，这里是否也应该跟进到 typed reward payload？

## Suggested Verification

- 逐文档做一次术语 diff，重点对齐：
  - `SearchBindingId / SearchAnchorId`
  - `PerceptionScore` 字段名
  - `HiddenTriggerType / DiscoveryRule / SearchAction`
  - `ReturnBridgePolicy`
  - `ContentPackHarnessSpec`
- 补一轮文档 lint：
  - 搜索 `rewardProfileId:` 是否仍出现在 `ZoneMapgenProfile` 示例中
  - 搜索 `ACTIVE_SEARCH` / `SEARCH_ACTION` / `REQUIRES_SEARCH_ACTION` 是否仍在多套 contract 同时出现
  - 搜索 `returnBridgeNodeId` 是否已经彻底从静态 schema 消失
  - 搜索 `HarnessReportHeader` 的版本字段是否还有不必要的可空

## Summary

第二轮收口已经把最危险的结构性歧义大部分压下去了，这一点是成立的。现在剩下的问题不再是“设计没想清楚”，而是“同一个概念还在几份文档里保留了不同版本的残影”。这类残影如果不清掉，真正进入实现时仍然会长出第二套语义，只是规模比之前更小、更隐蔽。

我建议先做一轮纯文档对齐，不必继续发散新设计。把本报告列出的 residual drift 清干净之后，这套 `Phase 4` 文档基本就可以进入稳定实现阶段了。
