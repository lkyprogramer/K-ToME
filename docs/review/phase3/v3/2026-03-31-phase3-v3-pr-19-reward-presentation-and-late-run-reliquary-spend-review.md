# Phase 3 V3 PR-19 深度审查报告

- 审查目标：核查当前实现是否符合 `docs/review/phase3/v3/2026-03-30-phase3-v3-pr-19-reward-presentation-and-late-run-reliquary-spend.md`
- 审查范围：当前工作树中与 PR-19 直接相关的 `core/game/client` 改动，以及对应测试、harness、golden 输出
- 审查基线：
  - `docs/review/phase3/phase3_opt_deep_review_final.md`
  - `docs/review/phase3/v2/2026-03-29-phase3-v2-pr-14-floor-reward-cadence-and-shard-economy.md`
  - `docs/review/phase3/2026-03-26-phase3-pr-08-reward-milestone-affixization.md`
  - `docs/review/phase3/2026-03-26-phase3-pr-10-player-facing-information-cleanup.md`
  - `docs/phase3/roadmap.md`
  - `docs/phase3/2026-03-13-phase3-verification-checklist.md`

## 直接结论

当前实现已经满足 PR-19 的主体目标，可以判定为“主线完成、体验有感、门禁已接上”：

1. 奖励来源 presentation 已进入正式 snapshot/log/client 主链，`cadence / route / boss / cache / support` 在玩家侧基本可区分。
2. `temple_ward_reliquary -> abyssal_reliquary_post` 的晚局 shard spend 节点已经成立，且复用了既有 `shop/service/interactable` 合同，没有引入第二货币或 Phase 4 经济系统。
3. `FoundationGameSessionTest`、`LongRunWorldStructureSessionTest`、`ClientSmokeHarnessTest`、`RoutePreviewTextTest`、`longRunLab`、`goldenScreenshot` 本轮均已通过，说明这批改动不是停留在静态代码层。

但这次实现还留有 2 个 `P2` 级偏差和 1 个 `P3` 级一致性问题。它们不会否定 PR-19 主体完成，但会影响后续调优的可观测性，以及“奖励来源表达是否真正统一”的收口质量。

## Findings

### P2

#### 1. `support reward` 在 QA / summary 口径里仍被归并为 `CACHE`

- 文档要求：
  - PR-19 的完成标准要求玩家能够区分 `cadence / route / boss / cache / support reward`。
  - W19c 又要求 reward visibility regression 不只停留在 UI，而要进入测试与观测。
- 当前证据：
  - `FoundationGameSession.kt:3871-3887` 在 support interactable 发奖时，`RewardGenerationContext.rewardSource` 和 `recordMilestoneReward()` 仍然写死为 `MilestoneRewardSource.CACHE`。
  - 玩家侧 recent reward panel 确实使用了 `RewardPresentationSourceSnapshot.SUPPORT`，但 long-run / milestone summary 仍只会看到 `CACHE`。
  - 这意味着 UI 已分流，QA 报表没有分流。
- 影响：
  - 玩家看到的是“支援奖励”，实验室报告看到的仍是“CACHE”。
  - 后续如果要单独调 support 节点的收益感、频率或 adoption，当前 harness 无法回答“support reward 到底有没有成立”。
  - 这不是 runtime correctness bug，但它削弱了 PR-19 想建立的“奖励来源正式可见度”。
- 修复方向：
  1. 让 summary / harness 口径也拥有 `SUPPORT` 源。
  2. 若不想改变 PR-08 的 loot bias，可保留生成侧使用 `CACHE` bias，但在 `MilestoneRewardSummary` 或平行的 `RewardPresentationSummary` 中显式区分 `CACHE` 与 `SUPPORT`。
  3. 同步更新 `ScenarioReport`、`HeadlessSmokeSuiteTest`、`LongRunLabTest`、`LongRunLabFullTest` 的聚合与断言。

#### 2. `lateRunReliquaryPurchaseCount` 观测字段过粗，证明了“买过”，但还不能证明“后段 shard sink 用得对”

- 文档要求：
  - PR-19 要求补一个 late-run spend 观测字段，用来证明“后段 shard 仍有明确花法”。
- 当前证据：
  - `FoundationGameSession.kt:478-479` 的 `currentLateRunReliquaryPurchaseCount()` 直接返回 `shopStates[abyssal_reliquary_post].purchasedOfferIds.size`。
  - `build/reports/harness/long-run-summary.md:22-24` 与 `build/reports/harness/long-run-full.md:22-24` 只聚合了单个整数。
  - 报表里大量 full-route run 都出现 `reliquary=3`，它只能说明“买了 3 个唯一 offer”，无法区分是：
    - 买了 rescue 件
    - 买了 offense 件
    - 买了 `REFRESH_STOCK`
    - 还是只是顺手清空了店
- 影响：
  - 作为“节点确实可消费 shard”的硬证明，它够用。
  - 作为“这个晚局 sink 是否真的承担了 shard 出口”的平衡指标，它不够用。
  - 后续调价时，团队很容易把“商店被访问过”误判成“reliquary sink 已经健康工作”。
- 修复方向：
  1. 至少拆成：
     - `lateRunReliquaryVisitCount`
     - `lateRunReliquaryRefreshCount`
     - `lateRunReliquaryItemPurchaseCount`
     - `lateRunReliquaryShardSpent`
  2. 如果想更贴近设计目标，再进一步按 `CLEANSING / PROTECTION / OFFENSE / INSCRIPTION / SERVICE` 做分桶。
  3. `LongRunLabFullTest` 的 gate 建议从“至少有一次 purchase”升级为“至少有一次非 mandatory rescue spend 或 refresh spend”。

### P3

#### 3. `supply_crate` 仍保留旧的 generic log wording，导致 cache reward 的 log 层表达没有完全统一

- 文档要求：
  - W19a 明确要求 `route / boss / cache` 奖励来源至少在 log 或 summary 层可区分。
- 当前证据：
  - `FoundationGameSession.kt:3399-3410` 中，`logCacheRewardGrant()` 对 `supply_crate` 仍走 `log.interactable.supply_crate`。
  - `game/src/main/resources/i18n/en-US.json:650-651` 中，`log.interactable.supply_crate` 仍是旧的“撬开箱子拿到物品”表述，而不是 `Cache reward: ...`。
  - 当前 recent reward sidebar 会把它标成 `CACHE`，所以不是完全不可见，但 log 层口径仍不统一。
- 影响：
  - 这会让“cache reward 已统一进入正式来源表达”这件事只成立了大半。
  - 玩家如果主要通过 log 感知奖励来源，会发现不同 cache 节点的表达风格不一致。
- 修复方向：
  1. 最直接：把 `supply_crate` 也切到 `log.reward.cache.claimed`。
  2. 若想保留 flavour copy，也应至少把 `log.interactable.supply_crate` 改成带 source 前缀的 wording，例如 `Cache reward: ...`。

## Requirement Alignment

### 1. 奖励 presentation 只消费正式 snapshot/log/mainfest 主链，不允许 client 自行推断第二套来源体系

- 证据：
  - `RenderSnapshot` 新增 `recentRewards`，类型为 `RewardPresentationEntrySnapshot`。
  - `FoundationGameSession` 负责装配 source 与 label key。
  - `TileRenderModel` / `AsciiRenderModel` 只消费 snapshot 并做本地化渲染。
- 结论：`一致`

### 2. `cadence reward` 必须与普通掉落有明确不同的玩家可见表达

- 证据：
  - `log.reward.cadence.claimed` / `log.reward.cadence.dropped` 已显式前缀化。
  - recent reward sidebar 会显示 `Cadence reward / 节拍奖励`。
  - `FoundationGameSessionTest` 已断言 `recentRewards.last().source == CADENCE`。
- 结论：`一致`

### 3. `route / boss / cache / support` 奖励来源需要在玩家侧可区分

- 证据：
  - route / boss / cadence 在 log 与 sidebar 上都已清晰分流。
  - cache / support 在 recent reward sidebar 中已分成 `CACHE` 与 `SUPPORT`。
  - 但 `supply_crate` 仍保留 generic log wording；support 在 milestone/harness summary 中仍归并为 `CACHE`。
- 结论：`部分一致`

### 4. late-run shard spend 第一版优先复用既有 `shop/service/interactable` 合同，并绑定 `temple_ward_reliquary`

- 证据：
  - `interactables/index.yaml` 为 `temple_ward_reliquary` 加了 `shopNodeId: abyssal_reliquary_post`。
  - `shops/index.yaml` 新增 `abyssal_reliquary_post`，继续复用既有 `ShopOffer.serviceType`。
  - 没有引入新菜单模式或第二货币。
- 结论：`一致`

### 5. 新 spend 节点必须有明确价格、明确次数限制，且不破坏 objective/runtime

- 证据：
  - `abyssal_reliquary_post` 所有 offer 都有固定价格。
  - 购买次数沿用既有 `purchasedOfferIds` 语义；`REFRESH_STOCK` 仍是一局一次。
  - `FoundationGameSessionTest` 与 `LongRunWorldStructureSessionTest` 已覆盖“开 shop、买 shard spend、仍可通往 abyssal_heart”。
- 结论：`一致`

### 6. 本 PR 不引入 crafting / reforge / artifact economy，也不新增第二套 reliquary 资源

- 证据：
  - 当前 diff 只扩展了 snapshot、log、shop、interactable 与 harness。
  - 没有新货币、没有新 crafting contract、没有 raw art/raw audio 变更。
  - reliquary 继续挂在现有 `temple_ward_reliquary` 资源与 `REFRESH_STOCK` service 上。
- 结论：`一致`

### 7. QA lane 需要补齐 cadence / route / boss / reliquary 的回归与观测

- 证据：
  - `FoundationGameSessionTest`、`LongRunWorldStructureSessionTest`、`ClientSmokeHarnessTest`、`RoutePreviewTextTest`、`LongRunLabTest`、`LongRunLabFullTest`、`GoldenScreenshotHarnessTest` 均已被更新并通过。
  - 但晚局 spend 的观测目前只有单一整数，无法支撑更细的 balance 判断。
- 结论：`部分一致`

## Removal / Iteration Plan

- 当前没有“可立即安全删除”的实现路径；本次改动总体是增量合同和观测补强，不存在明显可直接回收的桥接层。
- 建议作为下一轮 staged cleanup 处理两件事：
  1. 停止在 summary/harness 层复用 `CACHE` 承载 `SUPPORT`，补独立 reward source 后再迁移现有聚合。
  2. 收敛 `log.interactable.supply_crate` 与 `log.reward.cache.claimed` 的双口径，只保留一套正式 cache reward wording。

## Additional Suggestions

- 如果目标是把“后段 shard 仍有明确花法”从“能买”推进到“值得买”，建议在 reliquary shop panel 上补一条轻量信息：
  - `late-run ward stock`
  - `refresh once`
  - 或至少一个 `RELIQUARY` / `ONCE` badge
- 当前 route preview 已经能显示 deterministic utility reward，但还没有展示 milestone reward 的兴奋点。下一步如果还想抬高奖励记忆点，优先考虑把 route preview 分成：
  - `Guaranteed utility`
  - `Milestone reward`
  这样玩家会更早感知“这条路的奖励身份”，而不是只看到一个保底件。

## Suggested Verification

### 本次已实际运行

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest" --tests "com.ktome.game.LongRunWorldStructureSessionTest" :client:test --tests "com.ktome.client.render.RoutePreviewTextTest" :client:clientSmoke --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew longRunLab :client:goldenScreenshot --tests "*GoldenScreenshotHarnessTest"
```

### 本次关键观测

- `build/reports/harness/long-run-summary.md` 本轮输出：
  - `cadenceRewardCount: 25`
  - `shopRefreshPurchaseCount: 4`
  - `lateRunReliquaryPurchaseCount: 13`
- `build/reports/harness/long-run-full.md` 本轮输出：
  - `cadenceRewardCount: 56`
  - `shopRefreshPurchaseCount: 10`
  - `lateRunReliquaryPurchaseCount: 32`

这些结果足以证明：

1. cadence reward 仍在正式 run 中稳定触发；
2. shop refresh 仍在使用；
3. 晚局 reliquary 节点已经被 full-route run 实际消费。

### 验证口径说明

- 曾尝试用 `./gradlew longRunLab --tests ...` 只跑定向类，但 root alias `longRunLab` 不接受 `--tests` 选项，Gradle 直接返回 `Unknown command-line option '--tests'`。
- 这属于命令形态差异，不是 PR-19 实现回归；后续若要做更细粒度的 long-run 定向执行，建议补明确的 task usage 或单测入口文档。

### 建议补做的白盒

1. 在普通非 Boss 楼层故意打出一次 cadence fallback，确认 log 与 sidebar 同时出现 `节拍奖励 / Cadence reward`。
2. 在 `abyssal_temple` 先带威胁接触 `temple_ward_reliquary`，再清场后复开商店，确认“objective/runtime 先成立，交易后解锁”的节奏符合预期。
3. 在 reliquary 节点分别购买：
   - cleanse
   - protection
   - offense
   - `REFRESH_STOCK`
   观察 UI 是否足够让玩家意识到“这是晚局 shard sink，而不是普通补给站”。

## Summary

这次实现已经把 PR-19 的主线目标真正落进了正式链路：奖励来源表达成立了，`temple_ward_reliquary` 的晚局 spend 成立了，相关 smoke / golden / long-run 也都接上了。从“能否进入主线”的角度看，这个 PR 可以通过。

剩余问题主要集中在“观测口径还不够干净”：

1. `support reward` 在玩家 UI 已分流，但在 QA / summary 仍被并入 `CACHE`。
2. `lateRunReliquaryPurchaseCount` 只够证明“买过”，还不够解释“为什么买、买得值不值”。
3. `supply_crate` 的 cache log wording 还没完全统一到 source-tagged reward 口径。

建议把这三件事作为 PR-19 的 follow-up cleanup，而不是回退当前实现。当前版本已经过线，但还没达到“奖励来源表达和晚局 spend 观测都完全收口”的最好状态。
