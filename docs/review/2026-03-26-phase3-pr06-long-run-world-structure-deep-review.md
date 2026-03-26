# Phase 3 PR-06 Long Run World Structure 深度审查报告

## 1. 直接结论

当前实现对 `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md` 的完成度可以概括为：

- 世界结构数据骨架已落地，`11 zone / 2 shops / 40 affix / 3 boss / WorldProgress / RouteReward / SaveSnapshot` 基本都有正式实现。
- 但运行时语义、验收门禁和最终玩法体验仍存在几处关键偏差，不能算“完全按 PR-06 收口”。
- 我给出的总体判断是：`骨架基本对齐，合同与门禁部分失配，终局体验未完全达标`。

按模块面粗估对齐度：

| 维度 | 结论 | 粗估对齐度 |
| --- | --- | --- |
| 世界拓扑、zone roster、等级带 | 基本对齐 | `90%` |
| `WorldProgress / Save / Shop / RouteReward` 合同落地 | 大体对齐 | `80%` |
| `longRunLab / bossHarness / checkpoint` 门禁收口 | 明显不足 | `55%` |
| `affix v1` 的 build 相关性与语义边界 | 部分对齐 | `60%` |
| `abyssal_guardian` 作为最终 Boss 的独立性 | 明显不足 | `40%` |

结论不是“返工 PR-06”，而是“需要补一轮收口 PR”，重点放在：

1. 修正世界图运行时语义。
2. 把 `longRunLab` 从“主线最短烟雾”提升为真正的 PR-06 验收门禁。
3. 收敛 `RunSummary`、存档 fail-fast、affix build relevance 这三类合同漂移。
4. 给 `abyssal_guardian` 做出真正的最终 Boss 身份，而不是 `dungeon_lord` 的换皮复用。

## 2. 已对齐项

以下内容已经和 PR-06 主体要求基本一致：

- `11-zone` 世界结构已经进入正式数据：`game/src/main/resources/data/world/world_graph.yaml`、`game/src/main/resources/data/zones/index.yaml`。
- `7 mandatory + 4 optional`、各 zone 等级带、`2` 个固定商店节点、`3` 个 Boss roster 已进入 schema 和运行时。
- `WorldProgressDef / GateCondition / RouteReward / RewardClaimPolicy` 已进入 `core` 合同：`core/src/main/kotlin/com/ktome/core/world/WorldProgress.kt`。
- 存档已经纳入 `zoneRoute / routeIndex / worldProgress / shardBalance / shopStates / headlessTurnEquivalent`：`core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt`。
- `RouteReward` 的一次性领取语义已经有实际逻辑和集成测试：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、`game/src/test/kotlin/com/ktome/game/LongRunWorldStructureSessionTest.kt`。
- `shard + 2 shops + rescue affordability` 已有数据、运行时和测试：`game/src/main/resources/data/shops/index.yaml`、`game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt`。
- `40` 条 affix 的预算和拆分已满足文档口径：`12` 武器前缀、`10` 武器后缀、`10` 防具前缀、`8` 防具后缀。
- `bossHarness` 已覆盖 `molten_giant / dungeon_lord / abyssal_guardian` 三个 encounter。

## 3. 主要偏差

### P0. 运行时没有真正遵守“world graph 默认双向可通行”

PR-06 在 4.1 明确要求 world graph 默认是双向图，单向边必须显式声明。

当前数据层确实把主连接都声明成了 `isBidirectional: true`，见：

- `game/src/main/resources/data/world/world_graph.yaml:4-47`

但运行时选路逻辑会把“已经访问过的 zone”直接过滤掉：

- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1626-1648`

这带来的实际结果是：

- mandatory 主线几乎只能单向前进。
- 只有“当前 zone 是 optional 且目标是上一个 zone”时，才允许特例回退。
- 也就是说，数据是双向图，玩法却接近“前向树 + optional 特例回退”。

偏差幅度：

- 文档要求的双向可通行能力是 `100%`。
- 当前 runtime 对 mandatory path 的反向通行支持基本是 `0%`。

影响：

- 文档合同和真实玩法语义不一致。
- `RouteReward`、`Gate`、route selection 的设计语义被 runtime 私自改窄。
- 后续如果要做更严格的长局路线博弈，会发现 graph 和 session 语义不是一回事。

修复建议：

1. `availableRouteAdvanceOptions()` 改为严格尊重 `ZoneConnection.isBidirectional`。
2. 如果设计上就是“不允许回头”，那应该把对应 edge 改成显式 one-way，而不是靠 visited filter 偷偷实现。
3. `claimedRouteRewards` 已经能防止奖励反复薅，不需要再用 visited zone 逻辑替代图语义。

### P0. `longRunLab` 还不是 PR-06 需要的“长局结构门禁”

当前 `longRunLab` 的最大问题不是“没有测试”，而是“测的不是 PR-06 真正要卡的东西”。

证据：

- smoke 场景里，`arcanist / berserker / spellblade` 直接从 `abyssal_temple -> abyssal_heart` 开始，而不是从完整长局入口起跑：
  - `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt:24-66`
- route probe 只要求 `ReachFloor(2)`，不是完整终局路径：
  - `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt:67-107`
- full matrix 默认 `ScenarioSpec.zoneRoute = listOf(zoneId)`，由 bot 自动走主线：
  - `game/src/main/kotlin/com/ktome/game/harness/ScenarioModels.kt:16-28`
  - `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt:25-37`
- bot 对 route selection 永远优先主线，不会主动覆盖 optional zone：
  - `game/src/main/kotlin/com/ktome/game/harness/ScenarioUtil.kt:256-269`

偏差幅度：

- optional branch 真实覆盖：当前约等于 `0`。
- 商店/经济循环在 `longRunLab` 中的显式门禁覆盖：当前约等于 `0`。
- smoke 对完整长局路径的覆盖：只覆盖最终切片，不覆盖完整世界推进。

影响：

- `longRunLab` 可以在“不进 optional zone、不进 shop、不验证 affordability、不验证主支线回切”的情况下通过。
- 这和 PR-06 文档希望它承担的 acceptance gate 不一致。

修复建议：

1. smoke 模式至少补三类固定场景：
   - 从 `shattered_outpost` 起跑的完整主线路径 smoke。
   - 强制访问 `greenwood_supply_post` 的经济 smoke。
   - 强制访问 `deep_iron_pit_waystation` 且经过一条 optional zone 的 branch smoke。
2. full 模式增加 route-plan matrix，而不是把选路完全交给主线优先 helper。
3. 在 `longRunLab` 报告中显式断言 `zoneRouteHash` 不能只收敛到主线单一路径。

### P1. checkpoint round-trip 没有校验 PR-06 最关键的世界推进状态

PR-06 明确要求 save/load round-trip 后，至少要保留：

- `worldFlags`
- `unlockedRoutes`
- `defeatedBossIds`
- `claimedRouteRewards`
- checkpoint 级商店状态

当前 harness 的 round-trip 检查只比较：

- floor
- zone
- profession
- routeIndex
- zoneRoute
- inventory name

代码位置：

- `game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt:221-260`

而且 `full` 模式完全不做 checkpoint round-trip：

- `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt:25-37`

偏差幅度：

- 文档要求的 world-progress 关键态检查覆盖应为 `100%`。
- 当前 harness 内建 round-trip 检查对这些字段的覆盖是 `0%`。

影响：

- `CheckpointRoundTrip` 这个断言名字看起来很强，实际强度不够。
- 即便 `worldProgress`、`shopStates`、`shardBalance` 被破坏，当前 long-run harness 也可能给绿。

修复建议：

1. `roundTripCheckpoint()` 增加以下比较：
   - `loaded.worldProgress() == session.worldProgress()`
   - `loaded.shopStates() == session.shopStates()`
   - `loaded.currentShardBalance() == session.currentShardBalance()`
   - `loaded.currentHeadlessTurnEquivalent() == session.currentHeadlessTurnEquivalent()`
2. `full` 模式至少加入一组固定 checkpoint 场景。

### P1. `RunSummary` 在 `game` 层形成了第二套真源

PR-06 真正的 run history 合同已经冻结在：

- `core/src/main/kotlin/com/ktome/core/profile/ProfileData.kt:24-57`

但 `game` 里又维护了一份 UI 版 `RunSummary`：

- `game/src/main/kotlin/com/ktome/game/GameView.kt:175-196`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:443-466`

这套 `game.RunSummary` 不包含：

- `seed`
- `classId`
- `raceId`
- `zoneRouteHash`
- `buildHash`
- `rulesetVersion`
- `claimedRouteRewardIds`
- `victory`

偏差幅度：

- 合同真源应该只有 `1` 份。
- 当前实际存在 `2` 份，而且 UI 那份字段明显更瘦。

影响：

- 后续很容易继续在 `game.RunSummary` 上加字段，造成 run history 合同继续漂移。
- 审查视角下，这是典型的“平行 DTO 漂移”。

修复建议：

1. `FoundationGameSession` 直接产出 `core.profile.RunSummary`。
2. 结算页只保留 `OutcomeSummaryViewModel` 之类纯 UI DTO。

### P1. `abyssal_guardian` 作为最终 Boss 的独立性明显不足

PR-06 文档把 `abyssal_guardian` 定义为 `Phase 3` 新增的最终 Boss。

但当前实现里，`abyssal_guardian_encounter` 复用了大量 `dungeon_lord` 资产和行为：

- 视觉、icon、audio 复用：
  - `game/src/main/resources/data/bosses/index.yaml:76-78`
- arena 复用：
  - `game/src/main/resources/data/bosses/index.yaml:82`
- AI profile 复用：
  - `game/src/main/resources/data/bosses/index.yaml:87-91`
- telegraph 复用：
  - `game/src/main/resources/data/bosses/index.yaml:97-98`
- reward 复用：
  - `game/src/main/resources/data/bosses/index.yaml:99`

偏差幅度：

- 作为“新最终 Boss”的独立体验面，当前只有名字、templateId、phase 名称是新的。
- 关键体验面里，至少 `6` 项在复用旧 Boss。

影响：

- 从系统策划和玩法体验角度，这更像“最终关换皮”，不是“Phase 3 新最终 Boss”。
- `bossHarness` 绿了，但不代表终局体验真的完成。

修复建议：

1. 至少给 `abyssal_guardian` 独立的：
   - telegraph spec
   - AI profile
   - reward profile
   - arena id
2. 如果资源预算有限，先做“独立行为 + 独立 telegraph + 独立 reward”，视觉音频可以后补。

### P1. `affix v1` 的 build 相关性没有真正贯通到掉落生成

`core` 已经预留了 build 相关性入口：

- `core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt:5-17`

但实际掉落生成默认只带 `itemTags`：

- `core/src/main/kotlin/com/ktome/core/item/ItemGenerator.kt:16-19`

`game` 侧地图掉落和战利品生成也没有补入 build context：

- `game/src/main/kotlin/com/ktome/game/GameModule.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`

偏差幅度：

- 文档要求 affix 不只是“40 条入池”，还要有 build relevance。
- 当前 buildTags 通道在运行时使用率是 `0%`。

影响：

- affix 更接近“物品类别相关”，而不是“职业/资源轴/构筑相关”。
- `berserker`、`spellblade` 的 affix 分布不会自然向其 build 偏移。

修复建议：

1. 从 profession、primary resource axis、已解锁 talent tree 推导 `buildTags`。
2. 掉落入口统一传入 `AffixSelectionContext(buildTags = ...)`。
3. 补固定 seed 分布测试，验证不同 build 的 affix 倾向确实发生变化。

## 4. 次级风险

### P2. optional zone 的“独有内容”仍偏 metadata 化

当前 optional zone 基本只冻结了 `uniqueContentTag`：

- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt:341-365`
- `game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt:15-23`

但没有强约束“独有 elite / 独有 reward profile / 独有 interactable-event”这样的运行时内容锚点。

建议：

- 每个 optional zone 至少绑定一项真正可生成、可验证、可掉落的独有内容。

### P2. save/profile 的 fail-fast 版本纪律不够硬

`SaveSnapshot`、`ProfileData` 的 PR-06 扩展字段仍然大量使用默认值：

- `core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt:11-32`
- `core/src/main/kotlin/com/ktome/core/profile/ProfileData.kt:6-40`

这会让缺字段坏档在反序列化时被静默补齐，而不是立刻 fail-fast。

建议：

- 对 PR-06 新增关键字段改成严格持久化字段。
- 补 malformed-save / malformed-profile 测试。

### P2. 复现元数据还不够完整

`longRunLab` 报告的 envelope 基本齐了，但 `bossHarness` / `official-slice` 仍然偏瘦，`buildId` 也是静态值。

建议：

- 抽统一 `HarnessEnvelope`。
- 由 Gradle 或 CI 注入真实 `buildId`。

## 5. 建议修复顺序

### 第一批：必须先补

1. 修正 `availableRouteAdvanceOptions()`，让 runtime 真正尊重 world graph 双向语义。
2. 强化 `HeadlessRunHarness.roundTripCheckpoint()`，把 `worldProgress/shopStates/shardBalance` 纳入断言。
3. 重写 `longRunLab` smoke/full 场景，使其覆盖 optional branch 和 shop path。

### 第二批：阶段收口

1. 收敛 `RunSummary` 为单一 core 合同。
2. 把 `affix buildTags` 真正接入掉落生成。
3. 为 `abyssal_guardian` 补独立 telegraph/AI/reward。

### 第三批：长期质量

1. 强化 save/profile fail-fast。
2. 给 optional zone 增加真实独有内容锚点。
3. 统一 harness 元数据 envelope 和 buildId 注入。

## 6. 本次验证

已实际运行并通过的命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test \
  --tests "com.ktome.game.LongRunWorldStructureSessionTest" \
  --tests "com.ktome.game.SaveLoadWorldProgressTest" \
  --tests "com.ktome.game.data.ZoneReachabilityTest" \
  --tests "com.ktome.game.data.ZoneContentCoverageTest" \
  --tests "com.ktome.game.harness.BossHarnessTest" \
  --tests "com.ktome.game.harness.LongRunLabTest" \
  --tests "com.ktome.game.harness.LongRunLabFullTest"
```

结果：

- 以上目标测试集通过。
- 我没有额外跑 root alias 级别的 `./gradlew bossHarness`、`./gradlew longRunLab`、`./gradlew test` 全量门禁。
- 审查结论同时基于文档对照、代码静态阅读和上述针对性测试。

## 7. 最终判断

如果只问“PR-06 有没有做出来”，答案是：

- `有，而且主骨架已经完成。`

如果问“是否已经和 PR-06 文档要求严格一致并达到总监级验收口径”，答案是：

- `还没有。`

当前最核心的偏差不在“少了几个类”，而在：

- 世界图语义没有完全兑现。
- `longRunLab` 还没成为真正的 PR-06 gate。
- 最终 Boss、affix build relevance、RunSummary 单一合同这三条主线还没有完全收口。

我建议把这份报告对应的修复看作一个 `Phase 3 / PR-06 post-review hardening` 收口包，而不是继续往 Phase 4 推进前顺手修零碎问题。
