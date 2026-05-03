# Phase4 v4 PR-04 Hidden Search & Zone Hooks 实施审查

- **审查日期**：2026-05-02
- **审查对象**：当前分支 `codex/phase4-v4-pr04-hidden-search-zone-hooks` 工作树相对 `main` 的全部改动
- **审查依据**：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`
- **审查身份**：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- **报告语言**：简体中文（代码标识符保留英文）

---

## 0. 总评

PR-04 已经把"主真源"层的骨架基本搭起来：四类前台 cue 类型枚举、五个 mandatory zone 的 runtime hook 注册、`flavorOnlyMechanics` 划分、`secretZoneSelector.primarySlot` 数据模型与解析、`topZoneLeadShare` 排除 `greenwood_fringe`、scripted 与 organic 两份 owner baseline 的指标分流，这些都已落地。但是从"开发设计总监 + 玩法体验"角度看，**仍有 4 类显著偏差让玩家可见的体验循环、owner 证据稳定性、以及 PR-07 接力点未达到 PR-04 的完成定义**：

| 等级 | 类别 | 摘要 |
| --- | --- | --- |
| **Blocker** | 玩法循环 | `grey_gate_depths` 没有 ritual cue hidden event，规范要求的"ritual cue 触发一次 Search/Interact 并输出可见 warning"不成立 |
| **Blocker** | 玩法循环 | `slag_alert` runtime effect 在 `applyZoneRuntimeHookEffect` 落空（`-> Unit`），slag/ore Search prompt visibility 实际依赖另一条预存通路；FIRE/OIL warning 未真实驱动 visual/audio overlay |
| **High** | 数据层落点 | `abyssal_temple` 缺"objective 完成前保留 1 个 secret entry detour node"，hiddenEntrancePlans 无 objective-gated 字段 |
| **High** | Snapshot 合同 | `RenderSnapshotHasher.canonicalJson` 仅做 `Json.encodeToString`，`recentActionCues` 列表未按 `stableKey` 排序，违反"对新 cue 字段保持稳定排序"硬约束，存在 owner evidence 哈希漂移风险 |
| **High** | Owner 证据 | `greenwoodFringeSearchDrivenPathPresent=true` 没有进入 `hiddenContentHarness` 的 supporting 字段（HiddenContentSummaryMetrics 不含此字段），违反 §5.1 floor rule 6 |
| **Medium** | Dedup 合同 | `search_available` 的 dedup key 多包含 `playerPoint.x,playerPoint.y`，与规范 `zoneId + tileId + searchTargetId` 偏差，刷新统计/去重语义偏移 |
| **Medium** | Hook 数据真实性 | `ferry_crossing` 的 `quick_crossing.fatigueDelta=1` 与 `inspect_ferry.turnDelta=-2` 中前者只写入 `evidenceTags`，玩家 fatigue 资源不会真实变化 |
| **Medium** | 数据层落点 | `greenwood_fringe` 缺规范 §5.1 第 4 项要求的"自动入口权重限制"字段，仅靠 `hiddenEntrancePlans` 数量隐式控制 |
| **Medium** | 范围归属 | `flavorOnlyMechanics` / `mechanicsWithoutDedicatedRuntimeHook` / `zoneHookTriggered` 三字段被放到 `ZoneMechanicRuntime.kt` 与 `FoundationGameSession.kt`，规范 §3.1 要求的 owner 文件 `HiddenContentModels.kt` 没有承接 |
| **Low** | 合同冗余 | `FrontstageActionPrioritySnapshot` 多出 `CRITICAL`、`HIGH` 两个枚举值，规范五级排序 (`CRITICAL_COMBAT > HIGH_COMBAT > HIGH_HIDDEN > MEDIUM > LOW`) 没有这两项 |
| **Low** | 接力遗漏 | `secret-zones/index.yaml` 中无任何 `secondarySlot: true` 的 sample 占位，PR-07 接力点没有显式预留行 |

**完成定义对照（§8）**：除 1、3 项已客观满足以外，其它 8 条都还需要回到 fast check 复跑（详见后文 M01–M06 拆解）。

---

## 1. Acceptance Matrix 实施判定

| reqId | 范围 | 实施判定 | 评分 | 主要偏差 |
| --- | --- | --- | --- | --- |
| `PR04-M01` | per-zone Search 修正 | 部分实施 | 60/100 | abyssal detour、grey_gate ritual event、greenwood 自动权重限制、harness `greenwoodFringeSearchDrivenPathPresent` 缺失 |
| `PR04-M02` | secretZoneSelector 槽位 | 部分实施 | 70/100 | 模型与 DataLoader 已支持 primary/secondary，但 yaml 中 sample zone 没有任何 `secondarySlot: true` 落点 |
| `PR04-M03` | runtime hook 固定清单 | 部分实施 | 75/100 | 5 hook 注册 + flavorOnly 划分 OK；slag_alert effect 落空、ferry fatigue 无实效、字段归属偏差 |
| `PR04-M04` | frontstage cue 优先级 / TTL | 部分实施 | 70/100 | TTL 全部正确；canonicalJson 排序缺失（**关键**），search_available dedup key 偏差，priority 枚举多出冗余值 |
| `PR04-M05` | organic hidden probe | 基本实施 | 85/100 | 8 指标全部存在；scripted 与 organic baseline 切割正确（V4 note 已确认）；slag cue density 由真实 `boundedRatio` 计算 |
| `PR04-M06` | governance 继承 | 通过 | 90/100 | acceptance matrix、artifact、failure rule 段落齐备；PR 描述需按 §5.4 三组分组撰写 |

> 评分仅作为"完成度"内部参考，不替换 fastCheck / ownerGate 的硬性判定。

---

## 2. 具体偏差清单与修复建议

### 2.1 §5.1 Per-zone Search 修正 — `PR04-M01`

#### 2.1.1 `abyssal_temple`：缺 detour node 与 objective-gated 条件

- **现状**：
  - `game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt:346-358` 已注册 `void_pressure` hook，绑定 `temple_ward_reliquary`，并自动 grant `hidden.primer.abyssal_temple.warded_archive`。
  - `game/src/main/resources/data/events/index.yaml:119-154` 提供 `INTERACT_TILE → PERCEPTION_REVEAL → INTERACT_TILE` 三段 hidden event。
  - `game/src/main/resources/data/mapgen/zones/index.yaml:151-164` 的 `hiddenEntrancePlans` 含 `REQUIRED_TAG` + `PERCEPTION_CHECK`。
- **偏差**：
  - 整个 codebase 未出现 `detour`、`detourNode`、`objectiveComplete`、`objectiveStage` 等 gate 字段，无法保证"在 objective 完成前 detour node 仍存活"。当前路径在 objective 标记完成后会随 hidden 入口销毁逻辑共同失效。
- **修复建议**：
  1. 在 `HiddenContentModels.kt` 新增 `EntrancePersistence`（枚举 `BEFORE_OBJECTIVE_COMPLETION` / `ALWAYS` / `AFTER_REVEAL`）字段挂在 `HiddenEntrancePlan`。
  2. `mapgen/zones/index.yaml` `abyssal_temple` 至少 1 条 hiddenEntrancePlan 设置 `persistence: BEFORE_OBJECTIVE_COMPLETION`。
  3. `HiddenContentMapgenPipeline` 在生成时按字段过滤；`whiteBoxHiddenContent` 增加一条 case 验证"objective 完成前 detour 仍活"。

#### 2.1.2 `deep_iron_pit`：searchPromptAvailable 由 hook 间接驱动，slag/ore 数据字段缺失

- **现状**：
  - `ZoneMechanicRuntime.kt:312-322` `slag_alert` 注入 `SlagAlertRuntimeEffect(searchPromptVisible=true, warningEventId="zone.trigger.oil_or_fire_seen")`。
  - `secret-zones/index.yaml:52-156` 提供 `deep_iron_slag_cache` / `deep_iron_smuggler_stash` 两条线索，`REQUIRED_TAG` / `PERCEPTION_CHECK` 入口规则到位。
- **偏差**：
  - 规范 §5.1 表格写明"slag/ore cue 设置 `searchPromptAvailable=true`"，意指**数据层** (interactables/index.yaml) 的 cue 自身具备字段，但实现是通过运行时 hook 让所有 slag/ore interactable 共享一个布尔标志。等效但语义错位：脚本测试无法只针对单个 cue 验证 prompt 可见性。
  - `applyZoneRuntimeHookEffect` 中 `is SlagAlertRuntimeEffect -> Unit`（空操作），FIRE/OIL warning 没有真实生成 visual/audio overlay；`buildRenderUiState` 仍使用 `unresolvedSearchableEntranceAtPlayerPosition()` 作为 prompt 判定，没有读取 hook effect 字段。
- **修复建议**：
  1. 在 `interactables/index.yaml` 的 slag/ore cue 上新增 `searchPromptAvailable: true` 数据字段，并在 `DataLoader` 解析为 `InteractableDescriptor.searchPromptAvailable`。`buildRenderUiState` 优先读取数据字段，hook 仅在 cue 缺字段时回退。
  2. `applyZoneRuntimeHookEffect` 的 `SlagAlertRuntimeEffect` 分支接入 `audioProfile.queueWarning(audio.zone.warning.slag)` + frontstage `WARN` overlay，确保规范"FIRE/OIL warning 只做 visual/audio，不改伤害"成立。
  3. `whiteBoxHiddenContent` 新增 case：单个 slag cue 在 hook 未触发前 `searchPromptAvailable=true` 即可被脚本读取。

#### 2.1.3 `underground_river`：`secretZoneSelector` 模型已就位，但缺 secondarySlot 占位

- **现状**：
  - `HiddenContentModels.kt:139-151` 已实现 `SecretZoneSelector(primarySlot, secondarySlot)` 互斥校验。
  - `DataLoader.kt:1715-1719` 正确解析 yaml 字段。
  - `secret-zones/index.yaml:103-104` `underground_river_crystal_rift` 已声明 `secretZoneSelector.primarySlot: true`。
- **偏差**：
  - PR-07 sample zone 的接力点没有显式 `secondarySlot: true` 占位，PR-04 完成时无证据证明"两槽位互斥"在 yaml 层活着。
- **修复建议**：
  1. 在 `secret-zones/index.yaml` 末尾追加一条 `pack_local_underground_river_secondary_sample` 的占位条目（标记 `enabled: false` + `availability: PR07_PENDING`），写明 `secretZoneSelector.secondarySlot: true`，让 schema 与 owner 证据现在就能验证互斥。
  2. `whiteBoxHiddenContent` 新增 negative case：同一 zone 同时声明 primarySlot + secondarySlot 应失败；同一 slot 重复也应失败。

#### 2.1.4 `greenwood_fringe`：自动入口权重限制缺失

- **现状**：`mapgen/zones/index.yaml:18-59` 两条 floor 各有 `hiddenEntrancePlans`，分别绑定 `search.greenwood.hidden_cache` 与 `search.greenwood.ambush_hideout`。`OrganicHiddenProbeSummaryAggregator.kt:6,46` 已在 `topZoneLeadShare` 分母中排除 `greenwood_fringe`。
- **偏差**：
  - 规范 §5.1 行 4 要求"限制自动入口权重，确保至少一条 Search-driven path"。当前仅靠条目数量隐式实现，没有 `autoEntranceWeight` / `autoEntranceCap` 字段。
  - 规范 §5.1 行 6 要求 `greenwoodFringeSearchDrivenPathPresent=true` 写入 `hiddenContentHarness` supporting 字段。`HiddenContentHarnessRunner.kt` 中 0 处匹配该字段，`HiddenContentSummaryMetrics` 不含此字段。
- **修复建议**：
  1. 在 `HiddenEntrancePlan` 上新增 `autoEntranceWeight: Double = 1.0` 字段，`mapgen/zones/index.yaml` `greenwood_fringe` 两条 plan 中至少一条设置 `autoEntranceWeight: 0.0`（强制 Search-driven），另一条 ≤ 0.5。
  2. `HiddenContentHarnessRunner.kt` 的 `HiddenContentSummaryMetrics` 增加 `greenwoodFringeSearchDrivenPathPresent: Boolean`，case 输入对 `greenwood_fringe` 走一次 scripted route trace，trace 命中 Search 即写入 true；其余 zone 不进入分子分母。

#### 2.1.5 `grey_gate_depths`：ritual cue hidden event 完全缺失

- **现状**：`ZoneMechanicRuntime.kt:324-329` 注册 `ritual_pressure` hook，`EncounterPressureRuntimeEffect(nextEncounterPressureBonus=1)`，触发 interactable `seal_cache` / `ritual_altar` / `shadow_brazier`。`interactables/index.yaml:90-105` ritual_altar / shadow_brazier 已带 `interactionTags`。
- **偏差**：
  - `events/index.yaml` 全文 0 处 grey_gate；没有 ritual cue 对应的 PERCEPTION_REVEAL / INTERACT_TILE hidden event。
  - `EncounterPressureRuntimeEffect` 没有 `warningEventId`（对比 `SlagAlertRuntimeEffect`），玩家不会看到 ritual 触发的 warning。
- **修复建议**：
  1. 在 `events/index.yaml` 新增 `hidden.event.grey_gate_depths.ritual.primer` (`PERCEPTION_REVEAL`) 与 `hidden.event.grey_gate_depths.ritual.interact` (`INTERACT_TILE`)，分别 grant `hidden.primer.grey_gate_depths.ritual` 和 `hidden.entry.grey_gate_depths.ritual_*`。
  2. `EncounterPressureRuntimeEffect` 增加 `warningEventId: String? = "zone.trigger.ritual_pressure_active"`，`applyZoneRuntimeHookEffect` 走 `addMessage(triggerFactId)` + audio profile `audio.zone.warning.ritual`。
  3. `hiddenContentHarness` 增加 case 覆盖 grey_gate ritual cue 的 Search/Interact 路径。

---

### 2.2 §5.2 Runtime hook 固定清单 — `PR04-M03`

#### 2.2.1 `slag_alert` 与 `ferry_crossing` 落点偏差

| Hook | 规范 payload | 实现 | 偏差 |
| --- | --- | --- | --- |
| `slag_alert` | `searchPromptVisible=true`，FIRE/OIL warning 只做 visual/audio，不改伤害；emit `zone.trigger.oil_or_fire_seen` | `SlagAlertRuntimeEffect.searchPromptVisible=true` 字段存在，但 `applyZoneRuntimeHookEffect` 分支为 `-> Unit`；warning event 仅 `addMessage`，无 audio profile / overlay 接入 | **High**：FIRE/OIL warning 不可玩家可见 |
| `ferry_crossing` | `quick_crossing:+1 fatigue, inspect_ferry:+1 hidden clue and -2 turns` | `quick_crossing.fatigueDelta=1` 仅 evidenceTags 写入；`inspect_ferry.turnDelta=-2` 实际通过 `applyFerryCrossingChoiceEffect` 调整剩余回合；`+1 hidden clue` 通过 `grantedDiscoveryTags` 落地 | **Medium**：`+1 fatigue` 没有真实扣 fatigue 资源 |
| `void_pressure` | `resourcePressureWarning=true`，penalty multiplier `1.2`，emit `zone.trigger.void_pressure_active` | `applyPendingObjectivePressurePenalty` 把 multiplier 转成 turns penalty | OK |
| `ritual_pressure` | `nextEncounterPressure += 1`，仅一次 | `recordZoneHookTriggered` 用 `discoveryTags` 幂等保护 | OK，但缺 warning（见 2.1.5）|
| `trail_pressure` | `hiddenLeadWeightMultiplier=1.25`，持续 `3 floors` | 字段全部对齐 | OK |

- **修复建议**（slag_alert）：
  1. `FoundationGameSession.applyZoneRuntimeHookEffect` 的 `SlagAlertRuntimeEffect` 分支真实接 audio + overlay：
     ```kotlin
     is SlagAlertRuntimeEffect -> {
         visualEvents.queueOverlay(VisualOverlay.WARN_FIRE_OIL)
         audioProfile.queueOneShot(AudioKey("audio.zone.warning.slag"))
         addMessage(effect.warningEventId)
     }
     ```
  2. 在 `clientSmoke` golden 中增加 `slag_alert.warn` 帧。
- **修复建议**（ferry_crossing）：
  - `applyFerryCrossingChoiceEffect` 在 `quick_crossing` 分支上调用现有 `playerStats.adjustFatigue(+1)`（与 PR-03 reward adoption 共用），并在 evidenceTags 中保留 `ferry.fatigueApplied=true` 以便 owner 证据可观测。

#### 2.2.2 字段归属：`HiddenContentModels.kt` 没有承接

- **规范 §3.1**：`HiddenContentModels.kt` 是 owner 文件，应包含 `zoneHookTriggered`、`mechanicsWithoutDedicatedRuntimeHook`、`flavorOnlyMechanics` 字段。
- **现状**：
  - 三字段实际位于 `ZoneMechanicRuntime.kt:254-265`（`ZoneMechanicClassification`）与 `FoundationGameSession.kt:357`（`ZONE_HOOK_TRIGGERED_TAG_PREFIX`）。
  - `HiddenContentModels.kt`（1–196 行）完全没有 zone hook / flavor only 字段。
- **影响**：scripted-baseline 的 `zoneHookCoverage` evidence 链上"hook 数据模型 vs 报告字段"分散在两个 owner 模块；后续 PR 改 hook 列表需要同步两份代码。
- **修复建议**：
  1. 把 `ZoneMechanicClassification` 搬到 `HiddenContentModels.kt`，`ZoneMechanicRuntime` 保留行为，仅 import data class。
  2. `flavorOnlyMechanics` 校验改为 `HiddenContentModels` 暴露的纯函数，`hiddenContentHarness` 直接调用（规范 §5.2 校验规则 3：`runtimeHookIds ∩ flavorOnlyMechanics = empty`）。

---

### 2.3 §5.3 Frontstage cue — `PR04-M04`

#### 2.3.1 `RenderSnapshotHasher.canonicalJson` 缺稳定排序（**Blocker 候选**）

- **现状**：`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshotHasher.kt:8-14` 直接 `json.encodeToString(snapshot)`，对 `recentActionCues: List<FrontstageActionCueSnapshot>` 没有手动按 `stableKey` 排序。
- **影响**：规范 §5.3 第 2 项是硬约束（owner evidence 哈希稳定）；只要 `buildRecentActionCues()` 内部触发顺序受到 RNG / set 迭代影响，golden / clientSmoke 的 hash 就会漂移。
- **修复建议**：
  ```kotlin
  fun canonicalJson(snapshot: RenderSnapshot): String {
      val canonical = snapshot.copy(
          frontstage = snapshot.frontstage.copy(
              recentActionCues = snapshot.frontstage.recentActionCues
                  .sortedWith(compareBy({ -it.priority.ordinal }, { it.stableKey }))
          )
      )
      return json.encodeToString(canonical)
  }
  ```
  并在 `core` 测试中加入"shuffled list -> 同一 hash"用例。

#### 2.3.2 `search_available` dedup key 多包含 player 坐标

- **现状**：`FoundationGameSession.kt:12470` `"search_available:${zone.id}:${playerPoint.x},${playerPoint.y}:${searchTarget.entrance.bindingId.value}"`。
- **规范**：`zoneId + tileId + searchTargetId`。
- **影响**：玩家在同一 search target 周围 1 格徘徊时 dedup key 会变化，TTL 不再覆盖原 cue，触发重复推送（规范 §5.3 cue TTL/dedup 表）。
- **修复建议**：把 `${playerPoint.x},${playerPoint.y}` 替换为目标 cue 自身的 `tileId`（即 `searchTarget.entrance.tileId`）。tileId 已在 hidden entrance plan 定义；如尚未 owner 字段化，需在 `HiddenContentModels.HiddenEntrancePlan` 暴露。

#### 2.3.3 `lead_discovered` dedup key 用 `discoveryTag` 替代 `leadId`

- **现状**：`FoundationGameSession.kt:11720` `"lead_discovered:${zoneId}:${discoveryTag}"`。
- **规范**：`zoneId + leadId`。
- **判断**：`discoveryTag` 与 `leadId` 概念域接近，但当前模型未做强制等价。后续 PR-07 sample lead 增加时，二者可能分裂。
- **修复建议**：在 `HiddenLeadDescriptor` 上明确 `leadId: HiddenLeadId`，`discoveryTag` 派生自 `leadId`；统一使用 `leadId` 进入 dedup key。

#### 2.3.4 `FrontstageActionPrioritySnapshot` 多出冗余值

- **现状**：`RenderSnapshot.kt:484-492` 含 `CRITICAL` / `HIGH` 两个值，规范五级排序仅有 `CRITICAL_COMBAT > HIGH_COMBAT > HIGH_HIDDEN > MEDIUM > LOW`。
- **影响**：枚举膨胀让 client / harness 容易把"非 hidden 的 high"和"hidden 的 high_hidden"混淆，违反规范 §5.3 优先级冲突规则的字面定义。
- **修复建议**：
  - 若 PR-01 typed cue contract 仍需要 `CRITICAL` / `HIGH`（旧 cue），改为命名 `CRITICAL_COMBAT` / `HIGH_COMBAT` 与现有 typed cue 统一；非战斗的 medium/low cue 不再走 `CRITICAL`/`HIGH`。
  - `clientSmoke` 增加一条 fixture：snapshot 里出现非五级值时加载失败。

#### 2.3.5 `SessionSnapshotMapper` 职责偏差

- **现状**：四类 cue 的 TTL 与 dedup 全部写在 `FoundationGameSession`，`SessionSnapshotMapper` 不参与 cue 映射。
- **规范 §5.3 第 3 项**：`SessionSnapshotMapper` 只映射 core semantic cue，不生成 client-only 第二状态。
- **判断**：当前实现"未引入 client-only 第二状态"这一硬约束 OK，但 cue 生命周期完全在 `FoundationGameSession`，违反"映射"的职责分离。
- **修复建议**：把 `recordFrontstagePassiveCue` 抽到 `HiddenCueRegistry`（new owner，置于 `core/snapshot` 或 `game/hidden`），由 `SessionSnapshotMapper.toFrontstageSnapshot()` 统一拉取并执行 priority/TTL filter。`FoundationGameSession` 只负责"在何时投递事件"。

#### 2.3.6 `clientSmoke` / `goldenScreenshot` 战斗降级证据

- **现状**：`FoundationGameSession.kt:13080-13085` 在 `targetableHostilePositions().isNotEmpty()` 时把 `HIGH_HIDDEN` 降为 `MEDIUM`，逻辑正确。
- **缺失**：规范 §5.3 第 4 项 `frontstageSearchCueVisibilityRate.scripted` 必须同时覆盖"非战斗可见 + 战斗降级"两类。当前 scripted baseline `frontstageSearchCueVisibilityRate >= 0.9`（行 42-44），但 case 是否真的覆盖战斗降级路径需要确认。
- **修复建议**：在 `HiddenContentHarnessRunnerTest` 增加一条 `combatThreatPresent=true` 的 case；预期 cue 仍存在但 `priority=MEDIUM`。

---

### 2.4 §5.4 Organic hidden probe — `PR04-M05`

#### 2.4.1 baseline 切割（已正确）

| 指标 | 期望位置 | 实际位置 | 阈值 | 判定 |
| --- | --- | --- | ---: | --- |
| `topZoneLeadShare` | organic baseline | `organic-baseline.json:22-24` | maxValue 0.4 | ✓ |
| `zoneSearchPromptVisibility` | scripted baseline | `scripted-baseline.json:17-20` (V4 note) | minValue 1.0 | ✓ |
| `zoneHookCoverage` | scripted baseline | `scripted-baseline.json:47-49` | minValue 1.0 | ✓ |
| `frontstageSearchCueVisibilityRate` | scripted baseline | `scripted-baseline.json:42-44` | minValue 0.9 | ✓ |
| `perZoneSecretConversionFloor.reportOnly` | 不进入 baseline | 未注册 | — | ✓ |
| `secretZoneSearchConversionFloor.reportOnly` | 不进入 baseline | 未注册 | — | ✓ |
| `perZoneSearchUseFloor.reportOnly` | 不进入 baseline | 未注册 | — | ✓ |
| `slagCueDensityPerEligibleRoom.reportOnly` | 不进入 baseline | 未注册 | — | ✓ |

> 早期 V3 把 `zoneSearchPromptVisibility` 留在 organic baseline，本次 V4 commit 已迁移到 scripted baseline，notes 行 56 明确写明"V4 moves zoneSearchPromptVisibility to hiddenContentHarness"。**baseline 切割本身合规**，无需修改。

#### 2.4.2 greenwood_fringe 排除分母（已正确）

`OrganicHiddenProbeSummaryAggregator.kt:6` 与 `:42-50` 用 `TOP_LEAD_SHARE_EXCLUDED_ZONE_ID` 排除 greenwood_fringe，符合 §5.1 floor rule 3。

#### 2.4.3 PR 描述分组（待确认）

规范 §5.4 metric 落地顺序固定为三组（runtime hook / lead distribution / report-only observation），并要求 PR 描述按这三组列 baseline delta 与 producer freshness。审查无法直接看到 PR 描述文本——**修复建议**：发起 PR 时严格按这三组写"Baseline delta"小节，避免 8 个指标混成一段。

---

### 2.5 §6 测试与自证 — 覆盖度审查

| 必测行为（§6.1） | 覆盖判定 | 备注 |
| --- | --- | --- |
| 1. `abyssal_temple` 不再 `searchUseRate=0.0%` | 部分 | void_pressure 接通，但 detour persistence 缺失，路径仍可能失活 |
| 2. searchUseRate 双 5% floor | 部分 | metric 已实现；但 deep_iron_pit cue 字段未数据化，证据链脆弱 |
| 3. slag/ore cue searchPromptAvailable=true 后进 cache/stash | 部分 | hook 路径可达，单 cue 数据字段缺 |
| 4. 五个 mandatory zone 各 1 个 hook | ✓ | `MANDATORY_ZONE_RUNTIME_HOOKS_BY_ZONE.associateBy(zoneId)` 保证唯一 |
| 5. 未实现机制名词进 `flavorOnlyMechanics` | ✓ | `ZoneMechanicClassification.termsPartitioned` 已校验 |
| 6. RenderSnapshot 输出四类 cue | ✓ | 枚举 + cue list OK |
| 7. canonicalJson 稳定排序 | ✗ | 未做手动排序（见 2.3.1） |
| 8. 客户端 cue 不遮挡 CRITICAL/HIGH 战斗 | 部分 | 服务端降级 OK，client 侧无主动排序，依赖服务端排序 |
| 9. harness / probe / whitebox / report 同批 producer | 待 fastCheck | 需要 `./gradlew ... reportPhase4Only reportPhase4` 实跑回归 |

---

## 3. 推荐修复优先级（fast check 复跑前必做）

> 排序按"修复后能让 owner gate 与玩家循环都通过"。

### P0（阻塞 ownerGate / 玩家循环）

1. `RenderSnapshotHasher.canonicalJson` 增加 `recentActionCues` 稳定排序（含相同 priority 下按 `stableKey`）。
2. `applyZoneRuntimeHookEffect` 中 `SlagAlertRuntimeEffect` 真实接 visual overlay + audio key + addMessage；clientSmoke / goldenScreenshot 同步增加帧。
3. `events/index.yaml` 新增 `grey_gate_depths.ritual.{primer,interact}` 两个 hidden event；`EncounterPressureRuntimeEffect.warningEventId` 落地。
4. `HiddenContentSummaryMetrics` 增加 `greenwoodFringeSearchDrivenPathPresent: Boolean`，scripted route trace 写入。

### P1（合同与归属）

5. `FoundationGameSession.kt:12470` 把 `search_available` dedup key 改成 `zoneId + tileId + searchTargetId`，移除 player 坐标。
6. `HiddenContentModels.kt` 承接 `ZoneMechanicClassification` 与 `flavorOnlyMechanics` 数据模型；`ZoneMechanicRuntime` 仅持有行为。
7. `HiddenEntrancePlan` 增加 `persistence` 与 `autoEntranceWeight` 两个字段；`abyssal_temple` 与 `greenwood_fringe` 数据填充。
8. `applyFerryCrossingChoiceEffect` 接通 `playerStats.adjustFatigue(+1)`，让 `quick_crossing` 真实扣 fatigue。

### P2（接力点 / 冗余清理）

9. `secret-zones/index.yaml` 增加 `secondarySlot: true` 占位 sample（`enabled:false` + `availability:PR07_PENDING`），固化两槽位互斥的 owner 证据。
10. `FrontstageActionPrioritySnapshot` 移除 `CRITICAL` / `HIGH` 冗余值，统一规范五级；处理 PR-01 typed cue 兼容层。
11. `lead_discovered` dedup key 切到 `leadId`，并在模型上明示 `discoveryTag` 派生关系。
12. `clientSmoke` / scripted harness case 增加"战斗中 cue 降级"专用 fixture。

---

## 4. 验证命令（修复后必跑）

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint \
    :core:test :game:test \
    hiddenContentHarness organicHiddenProbe whiteBoxHiddenContent \
    goldenScreenshot clientSmoke \
    reportPhase4Only reportPhase4 \
    maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox \
    -Pktome.whitebox.scenario=phase4-v4-pr04
```

并按 §6.3 执行人工白盒，记录到 `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr04-hidden-search-zone-hooks.md`（当前目录 `manual-records/` 缺该文件，必须补齐）。

---

## 5. 与 §8 完成定义对照

| # | 完成定义 | 当前状态 |
| --- | --- | --- |
| 1 | `abyssal_temple` 不再 `searchUseRate=0.0%` | 路径已建立但 detour persistence 缺，存在回退风险 |
| 2 | `deep_iron_pit` Search 行为进入 report | metric 已实现；cue 数据字段化欠缺 |
| 3 | `topZoneLeadShare <= 40%`，greenwood_fringe 不进分母 | ✓ |
| 4 | 五个 mandatory zone 各 1 个 hook | ✓ |
| 5 | 未实现机制名词标 `flavorOnlyMechanics` | ✓ |
| 6 | 前台 cue 区分四类 | ✓ |
| 7 | RenderSnapshot/hash/client/golden 同批覆盖四类 cue | hash 稳定排序未做，**未达标** |
| 8 | frontstage cue 不遮挡 CRITICAL/HIGH | 服务端 OK，client 侧测试覆盖不足 |
| 9 | localeLint contractLint assetLint audioLint goldenScreenshot clientSmoke verifyChanged 同批通过 | 待 fastCheck 实跑回归 |
| 10 | 没有新增图片计划 | ✓ |
| 11 | 没有新增音频计划 | ✓ |

**结论**：当前 PR 不满足完成定义第 1、2、7、8、9 项。建议把上文 P0/P1 修复项一次性合入后再启动 ownerGate 串行跑。

---

## 6. 参考与产出

- 审查依据文档：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`
- 主要审计代码点：
  - `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:477-516`
  - `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshotHasher.kt:8-14`
  - `game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt:254-360`
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:11720, 12470-12584, 13051-13085`
  - `game/src/main/kotlin/com/ktome/game/hidden/HiddenContentModels.kt:139-151`
  - `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt:1715-1719`
  - `game/src/main/resources/data/secret-zones/index.yaml:52-156`
  - `game/src/main/resources/data/events/index.yaml:119-154`
  - `game/src/main/resources/data/mapgen/zones/index.yaml:18-164`
  - `tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt:379-1144`
  - `tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeSummaryAggregator.kt:6,42-127`
  - `docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json`
  - `docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json`

> 报告作者：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
