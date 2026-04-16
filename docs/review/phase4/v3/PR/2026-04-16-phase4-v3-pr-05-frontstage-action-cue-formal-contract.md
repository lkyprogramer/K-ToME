> 执行前必须先完整阅读并接受：
> `docs/rule/kotlin.md`
> `docs/rule/ai-change-governance.md`
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md`
> `docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part2.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-03-organic-hidden-loop-secret-reward-identity-and-replay-hook.md`
> `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-04-boss-phase-identity-and-version-discipline.md`

# Phase 4 V3 PR-05 Frontstage Action Cue Formal Contract

**阶段**: `Phase 4 / Post-Review Follow-up / V3-PR-05`  
**优先级**: `P2`  
**工作量评估**: `M`（`3~5` 人日）  
**前置条件**: `V3-PR-03`，建议在 `V3-PR-04` 后执行  
**对应问题**:

1. frontstage action cue 仍无正式 typed contract
2. 目前 action lane 的 priority / TTL / 去重 主要依赖调用点隐式约定
3. reward / hidden clue / boss cue 在 whitebox 和日志里更可解释，但在玩家前台仍不够稳定可理解

## 0. 验证约束

1. 默认开发回路先跑 `./gradlew verifyChanged`。
2. 本 PR 默认联合验收固定为：
   - `clientSmoke`
   - `goldenScreenshot`
   - `bossHarness`
   - `verifyOwner`
   - `phase4Report`
3. 如果修改 `RenderSnapshot` 合同或 golden 语料，必须在同提交中完成受影响 snapshot / golden 重录。
4. canonical 证据必须包含：
   - `frontstageHighPriorityCueRetainedRate`
   - `frontstageCueDedupAppliedCount`
   - `frontstageCueExpiryParity`
   - `frontstageSecretCueVisibilityRate`

## 1. 阶段目标

把当前“轻量 frontstage 可读性补强”升级成正式的 action cue contract，让 priority、TTL、去重和 tone 都由 `game -> snapshot` 决定，`client` 不再靠隐式约定补排序。

完成标准：

1. `recentActionHighlights` 被正式 typed contract 替代
2. `search / secret / passive` 三类 cue 的优先级、生命周期和去重规则固定下来
3. 玩家能在前台稳定看到高价值线索，而不是继续被低价值 trigger 抢占展位

## 2. 为什么单独成 PR

1. 这已经不是一个“顺手补排序”的小修，而是一套新的 snapshot/action 抽象族。
2. 它同时跨 `game`、`core snapshot`、`client` 和 golden/contract 测试；继续塞在 `PR-04` 里会把 boss phase 和 frontstage contract 两条线互相污染。
3. `PR-03` 先收 secret reward authority，`PR-04` 再收 boss/version，最后单独收前台合同，依赖关系最干净。

## 3. 当前问题拆解

### 3.1 action cue 仍缺正式 typed contract

当前 `recentActionHighlights` 更像一个轻量展示槽位，而不是稳定 contract。不同来源的 cue 仍需要调用点自己约定排序与保留策略。

### 3.2 priority / TTL / 去重 仍停留在 patch 级约定

search、secret、passive 这几类信息都在竞争有限 action 槽位，但目前没有统一的 typed priority、稳定 `stableKey`、明确 TTL 与替换规则。

### 3.3 工程可解释性强于玩家可理解性

很多高价值信息在 whitebox、harness、日志里已经存在，但在真实游玩前台并不总是稳定可见，导致“debug 能看懂，玩家不一定看懂”。

## 4. 必须冻结的合同

1. 不新增新的 HUD 系统。
2. `mutationHighlights` 与 `terrainHighlights` 继续保持当前 owner，不顺手重开第二套前台层。
3. `client` 只消费 typed snapshot，不再自行定义 action cue 优先级。
4. 不保留旧 `recentActionHighlights` 的长期兼容双路径；按仓库现行规则直接重录受影响 snapshot/golden。

## 5. 范围与非目标

### 5.1 范围

1. `RenderSnapshot` action cue 合同升级
2. `FoundationGameSession` 队列模型与显式 record API
3. `client` 消费、tone 与 top-level 展示规则
4. session / render / golden / contract 测试与文档同步

### 5.2 非目标

1. 不重做 `mutation / terrain` 的显示 owner
2. 不新增大 UI 系统或教程面板
3. 不做 Replay 产品化
4. 不顺手扩 hidden/boss 本体语义，只收它们的前台 action cue contract

## 6. 技术方案

### 6.1 snapshot contract 升级

保留 `FrontstageReadabilitySnapshot` 的 `mutationHighlights` 与 `terrainHighlights`，用新字段替换 `recentActionHighlights`：

1. `recentActionCues: List<FrontstageActionCueSnapshot>`
2. `enum class FrontstageActionCategorySnapshot { SEARCH, SECRET, PASSIVE }`
3. `enum class FrontstageActionPrioritySnapshot { CRITICAL, HIGH, MEDIUM, LOW }`
4. `data class FrontstageActionCueSnapshot(val category, val priority, val stableKey, val message)`

### 6.2 runtime 队列模型

`FoundationGameSession` 内部改成 `QueuedFrontstageActionCue(category, priority, stableKey, message, expiresAfterTurn, sequence)`，并删除通用 `addFrontstageMessage(...)`，改成显式入口：

1. `recordFrontstageSearchCue(...)`
2. `recordFrontstageSecretCue(...)`
3. `recordFrontstagePassiveCue(...)`

统一规则：

1. 队列 cap：`12`
2. 去重：写入前按 `stableKey` 替换旧项
3. TTL：`CRITICAL/HIGH = 3 turns`，`MEDIUM = 2 turns`，`LOW = 1 turn`
4. 出 snapshot 时按 `priority desc, sequence desc` 排序，取前 `2` 条 action cue

### 6.3 精确优先级映射

第一轮按下面规则冻结：

1. `SEARCH_REVEALED / SEARCH_REVEALED_TAG -> SEARCH + CRITICAL`
2. `SEARCH_FAILED_CHECK / SEARCH_FAILED_TAG -> SEARCH + HIGH`
3. `SEARCH_NO_TARGET -> SEARCH + MEDIUM`
4. `SECRET_ZONE_REVEALED / SECRET_ZONE_ENTER / HIDDEN_REWARD_CLAIMED / HIDDEN_REWARD_DROPPED / HIDDEN_REWARD_ENCOUNTER -> SECRET + CRITICAL`
5. `HIDDEN_REWARD_BUFF -> SECRET + HIGH`
6. `PASSIVE_DAMAGE_BONUS_* / PASSIVE_ON_HIT_STATUS / PASSIVE_ON_KILL_RESOURCE_RESTORE -> PASSIVE + MEDIUM`
7. `PASSIVE_HP_REGEN -> PASSIVE + LOW`
8. `search.already_resolved / hidden.reward.already_claimed / hidden.secret_zone.return -> log-only`，永不进入 frontstage

### 6.4 stableKey 与 client 消费规则

`stableKey` 至少覆盖：

1. `search:no_target`
2. `search:<bindingId>:<result>`
3. `secret:reveal:<secretZoneId>`
4. `secret:enter:<secretZoneId>`
5. `secret:reward:<secretZoneId>`
6. `secret:encounter:<secretZoneId>:<monsterTemplateId>`
7. `secret:buff:<secretZoneId>:<statusId>`
8. `passive:hp_regen:<itemBaseId>`
9. `passive:on_hit_status:<sourceItemBaseId>:<statusId>`
10. `passive:on_kill_resource_restore:<sourceItemBaseId>:<resourceType>`
11. `passive:damage_bonus:<sourceItemBaseId>:<kind>`

`client` 消费规则：

1. `client` 只消费 typed snapshot，不再自己推优先级
2. top-level 仍保持 `mutation -> terrain -> action` 顺序
3. action cue tone 固定：
   - `SEARCH = GREEN`
   - `SECRET = GOLD`
   - `PASSIVE = LIGHT_GRAY`

## 7. 推荐改动面

1. `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
2. `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
3. `client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt`
4. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
5. `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
6. `game/src/test/kotlin/com/ktome/game/RenderSnapshotContractTest.kt`
7. `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
8. `client/src/test/kotlin/com/ktome/client/render/*`
9. 相关 golden / v2opt / v3 文档

## 8. 任务拆解

### Task 1：snapshot contract typed 化

- **目标**: 用正式 snapshot contract 替换轻量 action slot
- **验收**:
  - `recentActionCues` 进入正式 schema
  - 旧 `recentActionHighlights` 不再作为长期真源

### Task 2：runtime queue 与 record API 收口

- **目标**: 把 priority、TTL、去重从调用点约定收成统一运行时规则
- **验收**:
  - 同 `stableKey` cue 被替换而不是堆积
  - TTL 到期后 cue 消失
  - 高优先级 cue 不会被低价值 trigger 挤掉

### Task 3：client 消费与 tone 固化

- **目标**: 让前台展示完全服从 typed cue contract
- **验收**:
  - ascii/tile 的排序、文案、tone 都来自 typed cue
  - `SEARCH / SECRET / PASSIVE` 的玩家感知差异稳定存在

### Task 4：golden / contract / docs 同步

- **目标**: 避免 snapshot 合同升级后留下第二真源或旧语料
- **验收**:
  - 受影响 golden 完成重录
  - 文档已从 `recentActionHighlights` 更新到 `recentActionCues`

## 9. 推荐命令

```bash
./gradlew clientSmoke
./gradlew goldenScreenshot
./gradlew bossHarness
./gradlew verifyOwner
./gradlew phase4Report
```

## 10. 完成后才能认为 Phase4 前台可读性真正收口

1. 前台高价值信号不再依赖“谁先写消息谁上屏”的偶然顺序。
2. `search / secret / passive` 的信息优先级、生命周期和去重规则已经进入正式合同。
3. 玩家侧可理解性不再明显落后于 whitebox/report 的工程可解释性。
