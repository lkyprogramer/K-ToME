> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`

# Phase4 v4 PR-04 Hidden Search And Zone Hooks

**阶段**: `Phase 4 completion hardening / phase4-v4-pr04`
**优先级**: `P1`
**工作量**: `L`
**合并来源**: v4 P1-2、P1-3
**前置条件**: PR-00 已完成；Phase4 frontstage cue、organic hidden probe、zone mechanic audit 已存在
**资源生成结论**: 不生成图片资源；不生成音频资源

## 0. 开发治理与验收矩阵

本 PR 继承 PR-03 canary 验证制度。执行规则见 [development-governance.md](./development-governance.md)，通用验证阶梯见 [docs/verification/README.md](../../../verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../../rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `PR04-M01` | §5.1 per-zone Search 修正 | `game` | `com.ktome.game.hidden.*` tests | `hiddenContentHarness`, `organicHiddenProbe` | `tools/build/reports/phase4/hidden/` | `required` |
| `PR04-M02` | §5.1 `secretZoneSelector.primarySlot / secondarySlot` | `game` | mapgen / hidden selector tests | `whiteBoxHiddenContent` | `tools/build/reports/phase4/whitebox/hidden-content/` | `required` |
| `PR04-M03` | §5.2 runtime hook 固定清单 | `game` | runtime hook materialization tests | `hiddenContentHarness`, `reportPhase4Only` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `required` |
| `PR04-M04` | §5.3 frontstage cue priority / TTL | `client` / `game` | snapshot / cue presenter tests | `goldenScreenshot`, `clientSmoke` | `client/build/reports/golden/` | `required` |
| `PR04-M05` | §5.4 organic hidden probe | `tools` | `com.ktome.tools.hidden.*` tests | `organicHiddenProbe`, `reportPhase4Only` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `N/A` |
| `PR04-M06` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |

### Gate Budget

预计重型任务：`hiddenContentHarness`、`organicHiddenProbe`、`whiteBoxHiddenContent`、`goldenScreenshot`、`clientSmoke`、`reportPhase4Only`、`reportPhase4`、`verifyChanged`。触发原因是 PR-04 同时影响 hidden owner evidence、mapgen selector、RenderSnapshot cue 和 client presentation。

### Canonical Artifact

hidden / cue / zone hook 的 canonical 证据必须进入 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` 或对应 white-box summary。Search cue snapshot 必须保持稳定排序；raw screenshot metadata、runtime-home、本机路径不得成为 owner evidence。

### Failure Rule

PR-04 的重型 gate 失败必须先回到 per-zone Search、selector 或 cue fast check；不得通过放宽 `topZoneLeadShare`、cue priority 或 report-only floor 让 report 变绿。

## 1. 玩家体验目标

本 PR 让 hidden content 从“系统偶尔把入口推到玩家面前”升级为“玩家看到异常、主动 Search、得到反馈”。同时把 mandatory zone 的机制名词绑定到最小 runtime hook，防止文档词汇变成第二真源。

完成标准：

1. 每个 secret-bearing zone 都有可见异常 cue。
2. 每个 secret-bearing zone 都有 Search 或 Interact 触发路径。
3. `abyssal_temple` 的 `searchUseRate` 从 `0.0%` 提升到 report warning 线以上。
4. `deep_iron_pit` 的 Search 行为不再接近空白。
5. `topZoneLeadShare <= 40%`。
6. 每个 zone 的 `secretEntry / lead >= 25%` 进入 report-only floor。
7. 每个 mandatory zone 只有 1 个 runtime hook 进入 Phase4 完成态。
8. 未进入 runtime 的机制名词标记为 `flavorOnly`。

## 2. 当前问题

1. `abyssal_temple leadDiscoveryRate=97.0%`，但 `searchUseRate=0.0%`。
2. `deep_iron_pit searchUseRate=1.5%`。
3. `zoneDiscoveryDistribution` 与 `secretZoneDiscoveryDistribution` 错位。
4. `mechanicsWithoutDedicatedRuntimeHook` 暴露多个机制名词没有 runtime hook。
5. 玩家会把 hidden content 理解为路径赠品，而不是主动探索奖励。

## 3. 范围与非目标

### 3.1 范围

生产代码：

- `game/src/main/kotlin/com/ktome/game/hidden/HiddenContentMapgenPipeline.kt`
- `game/src/main/kotlin/com/ktome/game/hidden/HiddenContentModels.kt`
- `game/src/main/kotlin/com/ktome/game/hidden/HiddenContentRegistry.kt`
- `game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt`
- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshotHasher.kt`
- `client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt`
- `client/src/main/kotlin/com/ktome/client/ui/panel/LogPresentationModel.kt`
- `tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeSummaryAggregator.kt`
- `tools/src/main/kotlin/com/ktome/tools/hidden/WhiteBoxHiddenContentRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/**`

数据：

- `game/src/main/resources/data/hidden-events/index.yaml`
- `game/src/main/resources/data/secret-zones/index.yaml`
- `game/src/main/resources/data/objectives/index.yaml`
- `game/src/main/resources/data/zones/index.yaml`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`

测试：

- `game/src/test/kotlin/com/ktome/game/hidden/**`
- `core/src/test/kotlin/com/ktome/core/snapshot/**`
- `client/src/test/kotlin/com/ktome/client/**`
- `tools/src/test/kotlin/com/ktome/tools/hidden/**`
- `tools/src/test/kotlin/com/ktome/tools/phase4/**`

### 3.2 非目标

1. 不新增 secret zone。
2. 不新增 hidden event。
3. 不新增地图生成子系统。
4. 不引入通用脚本宿主。
5. 不把所有机制名词都做成 runtime 机制。
6. 不降低 secret 发现难度来刷指标。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

执行要求：

1. Search cue 使用现有 hidden/frontstage/zone visual key。
2. zone hook 使用现有 zone icon、interactable visual、telegraph visual。
3. 本 PR 不新增 image plan、generation report、processing report。
4. `assetLint` 不需要新增资源，但 client golden 中引用的 hidden/frontstage visual key 必须保持可解析。

### 4.2 音频资源

不生成新音频资源。

执行要求：

1. hidden reveal 继续使用 `audio.hidden.reveal.secret_entrance`。
2. interactable cue 继续使用 `audio.interactable.*`。
3. zone warning 继续使用现有 zone/hidden/boss warning 音频。
4. 本 PR 不新增 audio plan、generation report、processing report。
5. `audioLint` 必须证明新增 hidden cue 事件只引用已有 audio key。

## 5. 技术方案

### 5.1 Per-zone Search 修正

固定改造：

| zone | 改造 |
| --- | --- |
| `abyssal_temple` | objective 完成前保留 1 个 secret entry detour node；detour 必须由可见异常 cue 触发 Search/Interact |
| `deep_iron_pit` | slag/ore cue 设置 `searchPromptAvailable=true`；Search 成功后进入 slag cache 或 smuggler stash 线索 |
| `underground_river` | 提升 lead density，使 lead share 落在 15%~40%；Search 与 ferry cue 共同导向 river secret |
| `greenwood_fringe` | 保持高 entry 转化，但限制自动入口权重，确保至少一条 Search-driven path |
| `grey_gate_depths` | ritual cue 触发一次 Search/Interact，并输出可见 warning |

Search floor 口径：

1. `abyssal_temple.searchUseRate` 的 report warning floor 固定为 `>= 5%`。
2. `deep_iron_pit.searchUseRate` 的 report warning floor 固定为 `>= 5%`。
3. `greenwood_fringe` 是 beginner onramp zone，不参与 `topZoneLeadShare` top-share 分母，但必须保留 Search-driven path 证据。
4. `deep_iron_pit` 每个 generated room 至少放置 `1` 个 slag/ore cue candidate；blocked room 不计入分母，report 输出 `slagCueDensityPerEligibleRoom >= 1.0`。
5. `underground_river` 的 `secretZoneSelector` 必须支持 `primarySlot` 与 `secondarySlot`；base `underground_river_crystal_rift` 固定占用 `primarySlot`，pack-local sample secret 由 PR-07 消费 `secondarySlot`。
6. `greenwoodFringeSearchDrivenPathPresent=true` 必须进入 `hiddenContentHarness` supporting 字段；证据来源为 scripted route trace，不用人工截图替代。

### 5.2 Runtime hook 固定清单

每个 mandatory zone 只落一个 Phase4 runtime hook：

| zone | runtime hook | 玩家可感知效果 | report 字段 |
| --- | --- | --- | --- |
| `greenwood_fringe` | `trail_pressure` | 偏离主路后前台提示巡逻压力，隐藏线索刷新权重变化 | `zoneHookTriggered.trail_pressure` |
| `deep_iron_pit` | `slag_alert` | slag/ore cue 使 Search prompt 可见，并提示 FIRE/OIL 风险 | `zoneHookTriggered.slag_alert` |
| `grey_gate_depths` | `ritual_pressure` | ritual cue 进入高优先级 warning，并影响下一次 encounter pressure | `zoneHookTriggered.ritual_pressure` |
| `underground_river` | `ferry_crossing` | ferry anchor 提供一次 crossing choice，并能连接 secret clue | `zoneHookTriggered.ferry_crossing` |
| `abyssal_temple` | `void_pressure` | objective 前出现 void pressure warning，并提高一次资源压力 | `zoneHookTriggered.void_pressure` |

未进入 runtime 的名词必须在 report 中标为 `flavorOnlyMechanics`。

runtime hook 最小接线：

| hook | runtime 输入 | runtime 效果 | 量化 payload | snapshot 输出 | 不触碰范围 |
| --- | --- | --- | --- | --- | --- |
| `trail_pressure` | route deviation count | 调整隐藏线索刷新权重 | `hiddenLeadWeightMultiplier=1.25`，持续 `3 floors` | `zone_hook_triggered` | 不改敌人强度 |
| `slag_alert` | slag/ore cue Search | 生成 Search prompt 与 FIRE/OIL warning | `searchPromptVisible=true`，FIRE/OIL warning 只做 visual/audio，不改伤害；同时发出 `zone.trigger.oil_or_fire_seen` | `search_available`, `zone_hook_triggered` | 不改伤害公式 |
| `ritual_pressure` | ritual cue Interact | 下一次 encounter pressure +1 | `nextEncounterPressure += 1`，只计一次 | `zone_hook_triggered` | 不改 AI 行为树 |
| `ferry_crossing` | ferry anchor Interact | 生成 crossing choice 与 secret clue | `choice={quick_crossing:+1 fatigue, inspect_ferry:+1 hidden clue and -2 turns}` | `secret_entry_nearby` | 不新增地图子系统 |
| `void_pressure` | abyssal objective 前状态 | 触发一次资源压力 warning | `resourcePressureWarning=true`，下一个 objective 的 light/time penalty multiplier `1.2`，同时发出 `zone.trigger.void_pressure_active` | `zone_hook_triggered` | 不改 Boss phase |

`flavorOnlyMechanics` 校验规则：

1. `ZoneMechanicRuntime` 未注册的机制名词必须出现在 `flavorOnlyMechanics`。
2. `flavorOnlyMechanics` 不得被任何 runtime selector、encounter modifier、reward modifier 消费。
3. `hiddenContentHarness` 必须校验 `runtimeHookIds ∩ flavorOnlyMechanics = empty`。
4. `allMechanicTerms == runtimeHookIds ∪ flavorOnlyMechanics`。
5. `runtimeHookIds ∩ flavorOnlyMechanics == empty`。
6. `mechanicsWithoutDedicatedRuntimeHook == flavorOnlyMechanics`。
7. 出现未分类机制名词时 validator fail。

### 5.3 Frontstage cue

前台 cue 必须区分：

1. `lead_discovered`: 玩家发现异常。
2. `search_available`: 玩家当前格或邻近格能主动 Search。
3. `secret_entry_nearby`: 入口已经可进入。
4. `zone_hook_triggered`: zone runtime hook 已触发。

这些 cue 必须有 stable key、TTL、dedup，并进入 `RenderSnapshot`。

snapshot 合同：

1. `RenderSnapshot` 新增的 cue 字段必须位于 core snapshot owner 文件。
2. `RenderSnapshotHasher.canonicalJson` 对新增 cue 字段保持稳定排序。
3. `SessionSnapshotMapper` 只映射 core semantic cue，不生成 client-only 第二状态。
4. client 只消费 snapshot cue，不从日志文本反推 hidden 状态。
5. fixed-seed golden 必须证明四类 cue 在玩家行动前可见，并且不遮挡 CRITICAL / HIGH priority frontstage cue。
6. 若同批触碰 `RenderSnapshot.kt`，必须保留 PR-01 的 `TalentTreeNodeSnapshot.category: TalentCategory` typed enum 合同；JSON wire 仍是枚举名字符串，client / harness 不得重新引入 `valueOf(snapshot.category)` 字符串解析。

cue TTL / dedup 固定为：

| cue type | TTL | dedup key | 优先级 |
| --- | ---: | --- | --- |
| `lead_discovered` | `6 turns` | `zoneId + leadId` | `MEDIUM` |
| `search_available` | `8 turns` | `zoneId + tileId + searchTargetId` | `HIGH_HIDDEN` |
| `secret_entry_nearby` | `8 turns` | `zoneId + secretZoneId` | `HIGH_HIDDEN` |
| `zone_hook_triggered` | `6 turns` | `zoneId + hookId` | `MEDIUM` |

同一 dedup key 在 TTL 内只刷新剩余回合，不重复推送日志。

优先级冲突规则：

1. frontstage priority 固定排序为 `CRITICAL_COMBAT > HIGH_COMBAT > HIGH_HIDDEN > MEDIUM > LOW`。
2. `search_available` 与 `secret_entry_nearby` 在非战斗状态使用 `HIGH_HIDDEN`。
3. 玩家处于战斗威胁范围时，hidden cue 保持存在但显示层级降到 `MEDIUM`，不得遮挡 `CRITICAL_COMBAT / HIGH_COMBAT`。
4. `frontstageSearchCueVisibilityRate` 的 scripted case 必须同时覆盖非战斗可见与战斗降级两类。

### 5.4 Organic hidden probe

新增统计：

| 指标 | 阈值 | metricKind | producer | ownerBaseline | failSemantics |
| --- | ---: | --- | --- | --- | --- |
| `topZoneLeadShare` | `<= 40%` | `blockingOwner` | `organicHiddenProbe` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json` | `fail owner gate` |
| `perZoneSecretConversionFloor.reportOnly` | `secretEntry / lead >= 25%` | `reportOnlyOwner` | `organicHiddenProbe` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json` | `warn only` |
| `secretZoneSearchConversionFloor.reportOnly` | `successful Search -> secretEntry >= 5% per secret-bearing zone` | `reportOnlyOwner` | `organicHiddenProbe` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json` | `warn only` |
| `zoneSearchPromptVisibility` | 每个 secret-bearing zone 至少 1 个非 flavor Search prompt case | `blockingOwner` | `hiddenContentHarness` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json` | `fail owner gate` |
| `perZoneSearchUseFloor.reportOnly` | `abyssal_temple >= 5%`, `deep_iron_pit >= 5%`, 其他 secret-bearing zone `> 0%` | `reportOnlyOwner` | `organicHiddenProbe` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json` | `warn only` |
| `slagCueDensityPerEligibleRoom.reportOnly` | `>= 1.0` | `reportOnlyOwner` | `organicHiddenProbe` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json` | `warn only` |
| `zoneHookCoverage` | `5/5 mandatory zones` | `blockingOwner` | `hiddenContentHarness` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json` | `fail owner gate` |
| `frontstageSearchCueVisibilityRate` | `>= 90% scripted cue cases` | `blockingOwner` | `hiddenContentHarness` | `docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json` | `fail owner gate` |

metric 落地顺序固定为：

1. runtime hook group：`zoneHookCoverage`、`frontstageSearchCueVisibilityRate`。
2. lead distribution group：`topZoneLeadShare`、`zoneSearchPromptVisibility`。
3. report-only observation group：`perZoneSecretConversionFloor.reportOnly`、`secretZoneSearchConversionFloor.reportOnly`、`perZoneSearchUseFloor.reportOnly`、`slagCueDensityPerEligibleRoom.reportOnly`。
4. PR description 必须按上述三组列出 baseline delta 与 producer freshness，不得把 8 个指标混成一段总说明。

## 6. 测试与自证

### 6.1 必测行为

1. `abyssal_temple` 不再出现 `searchUseRate=0.0%`。
2. `abyssal_temple.searchUseRate >= 5%`，`deep_iron_pit.searchUseRate >= 5%`。
3. `deep_iron_pit` 的 slag/ore cue 设置 `searchPromptAvailable=true`，Search 成功后进入线索或 cache。
4. 五个 mandatory zone 均有且仅有一个 Phase4 runtime hook。
5. 未实现机制名词进入 `flavorOnlyMechanics`，不得成为 runtime 第二真源。
6. `RenderSnapshot` 输出 `lead_discovered / search_available / secret_entry_nearby / zone_hook_triggered` 四类 cue。
7. `RenderSnapshotHasher.canonicalJson` 对新增 cue 字段保持稳定排序。
8. client 前台 cue 在玩家行动前可见，且不遮挡 CRITICAL / HIGH priority 战斗提示。
9. `hiddenContentHarness`、`organicHiddenProbe`、`whiteBoxHiddenContent`、`reportPhase4Only` 和 `reportPhase4` 使用同一批 producer evidence。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr04
```

必须保留以下自证产物：

1. `build/reports/tests/` 中 core snapshot、hidden content、client smoke/golden 相关测试结果。
2. `tools/build/reports/` 中 `hiddenContentHarness`、`organicHiddenProbe`、`whiteBoxHiddenContent` producer 产物。
3. `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` canonical report 产物，且 `reportPhase4Only` 与 `reportPhase4` 对 hidden search / zone hook metrics 读取同一 producer artifact。
4. `build/reports/verification/` 中 `verifyChanged` 和 `maintainabilityLint` 产物。
5. `build/whitebox/phase4-v4-pr04/evidence/` 中人工白盒截图、日志、manual record。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

已有游戏 Validation Mode 改造要求：

1. 本 PR 必须接入 PR-00 的 `PHASE4_V4_FAST` section，scenario id 固定为 `phase4-v4-pr04`。
2. `prepare-primary-scene` 必须在现有游戏内 validation session 中把玩家放到 `deep_iron_pit` 搜索 cue 可见位置，并保证正式 Search action 能触发 clue/cache/stash 反馈。
3. `prepare-secondary-scene` 必须切换到 `abyssal_temple` objective 前区域，展示 `void_pressure` warning 与 zone hook 触发面。
4. `show-evidence-summary` 必须展示 hidden search cue、Search result、zone hook、priority no-overlap 四组证据清单。
5. 搜索绑定、前台 cue、zone hook、优先级排序必须来自 core/game snapshot 与正式 validation action，不得由 client overlay 伪造。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr04`
4. preset：`HIDDEN_CONTENT`
5. seed：`2026042434`
6. runtime home：`build/whitebox/phase4-v4-pr04/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr04/evidence`
8. manual record：`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr04-hidden-search-zone-hooks.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr04
```

2. 执行 `build/whitebox/phase4-v4-pr04/launch-packaged-app.sh` 启动 packaged app，Computer Use 目标 app 固定为 `com.ktome.client`。
3. 按 `build/whitebox/phase4-v4-pr04/cua-runbook.md` 打开 validation overlay，执行 `PHASE4_V4_FAST / prepare-primary-scene`。
4. 截图记录 `deep_iron_pit` slag/ore cue 附近的 `search_available` 前台 cue。
5. 执行 Search，确认线索、cache 或 stash 反馈出现，并记录日志。
6. 执行 `PHASE4_V4_FAST / prepare-secondary-scene`，截图记录 `abyssal_temple` objective 前区域的 `void_pressure` warning，确认 Search/Interact 在玩家行动前可见。
7. 触发 `greenwood_fringe` 或 `underground_river` 的 zone hook，截图记录 `zone_hook_triggered` cue。
8. 在战斗附近重复 Search cue 检查，确认 cue 不遮挡 CRITICAL / HIGH priority 战斗提示。
9. 执行 `PHASE4_V4_FAST / show-evidence-summary`，确认证据清单与本节文件名一致。
10. 保存证据：
    - `phase4-v4-pr04-deep-iron-search-cue.png`
    - `phase4-v4-pr04-search-result-feedback.png`
    - `phase4-v4-pr04-abyssal-void-pressure.png`
    - `phase4-v4-pr04-zone-hook-triggered.png`
    - `phase4-v4-pr04-priority-no-overlap.png`
    - `phase4-v4-pr04-app.log`

通过标准：

1. 玩家能在 Search 前看到异常 cue。
2. Search/Interact 后反馈能解释线索、入口或奖励状态。
3. zone hook 的前台提示在 packaged app 中可见。
4. manual record 写明 packaged app 路径、runtime home、seed、输入序列、截图路径和结论。

### 6.4 统一验证框架关系

本 PR 同时触碰 hidden owner evidence、core snapshot 与 client presentation。`hiddenContentHarness / organicHiddenProbe / whiteBoxHiddenContent / reportPhase4Only / reportPhase4` 是 owner 证据；`goldenScreenshot / clientSmoke` 是玩家可见性证据；人工白盒验证 Search 学习链是否在真实 packaged app 中成立。

### 6.5 玩家体验 Golden Path

1. 玩家进入 `deep_iron_pit` 房间时，必须在行动前看到 slag/ore 异常 cue 和 Search prompt。
2. 玩家执行 Search 后，必须得到线索、cache 或 stash 反馈，不得只输出空日志。
3. 玩家进入 `abyssal_temple` objective 前区域时，必须看到 `void_pressure` warning，并能从 Search/Interact 理解隐藏入口接近。
4. 玩家触发任一 zone hook 后，前台提示必须说明 hook 结果，不遮挡 CRITICAL / HIGH 战斗提示。
5. 玩家反复移动时，同一 cue 不得刷屏；TTL 内只刷新剩余回合。

## 7. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

## 8. 完成定义

1. `abyssal_temple` 不再出现 `searchUseRate=0.0%`。
2. `deep_iron_pit` 的 Search 行为进入 report。
3. `topZoneLeadShare <= 40%`，且 `greenwood_fringe` 不进入 top-share 分母。
4. 五个 mandatory zone 均有且仅有一个 Phase4 runtime hook。
5. 未实现机制名词被 report 标记为 `flavorOnlyMechanics`。
6. 前台 cue 能区分线索、可 Search、入口接近和 hook 触发。
7. `RenderSnapshot`、snapshot hash、client presentation 和 golden screenshot 同批覆盖四类 cue。
8. frontstage cue 不遮挡 CRITICAL / HIGH priority 战斗提示。
9. `localeLint contractLint assetLint audioLint goldenScreenshot clientSmoke verifyChanged` 同批通过。
10. 没有新增图片计划文件。
11. 没有新增音频计划文件。
