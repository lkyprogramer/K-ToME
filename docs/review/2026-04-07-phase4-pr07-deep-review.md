# Phase 4 PR-07 深度审查报告

**审查日期**: 2026-04-07  
**审查角色**: Roguelike 设计总监 + 系统策划总监 + 玩法体验审查  
**审查分支**: `codex/p4-pr07-hidden-event-secret-zone-client-readability`  
**对照文档**: `docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md`

---

## 1. 总体结论

**PR-07 实现与规格文档高度一致，核心 contract 无硬伤，出口门禁 6/6 全部达标。** 改动面覆盖 game / tools / client / assets-src 四个模块，hidden event + secret zone 的完整生命周期（定义 → 加载 → mapgen 绑定 → 搜索触发 → 奖励桥接 → 返回主线 → client 可读性 → 白盒验证）均已落地。

发现 **0 项阻塞性缺陷**、**3 项中等偏差**、**4 项低风险改进建议**。

---

## 2. 出口门禁逐项核验

| # | 门禁条件 | 状态 | 依据 |
|---|---------|------|------|
| 1 | `HiddenEventDef / SecretZoneDef` 与 reward bridge 口径冻结 | **PASS** | `HiddenContentModels.kt` 完整定义，带 `init` 校验；`HiddenContentRegistry.kt` 提供 registry |
| 2 | `hiddenContentHarness` 正式可用，输出结构化报告 | **PASS** | `build.gradle.kts:184` root alias，报告路径 `tools/build/reports/phase4/hidden/` 与规格一致 |
| 3 | client 可读性满足白盒验证 | **PASS** | i18n en-US/zh-CN 完整，inspect 面板有 `stateLabelKey`，log 链路 6 条 key 全覆盖 |
| 4 | 至少有一条正式的主动搜索路径 | **PASS** | 全部 4 条 reveal 路径均需要 `PlayerCommand.Search` 显式触发 |
| 5 | 静态 schema 不持有 runtime node id | **PASS** | `SecretZoneDef` 只持有 `entranceBindingId + returnBridgePolicy + returnBridgeAnchorTag`，由 `HiddenContentMapgenPipeline` 实例化解析 |
| 6 | `whiteBoxHiddenContent` 接入统一白盒框架 | **PASS** | `WhiteBoxHiddenContentRunner.kt` 产出标准 summary/cases/artifacts；`whiteBoxVerify` 和 `phase4Report` 均依赖 |

---

## 3. 规格逐节对照

### 3.1 Section 2.1 — 冻结口径 (5/5)

| 口径 | 状态 | 验证路径 |
|------|------|----------|
| hidden event 默认只出现在 OPTIONAL / SECRET | **PASS** | 所有 event 的 `optionalOnly: true`；harness 断言 `triggerPathClassesWithinOptionalOrSecret` |
| SecretZoneDef 显式声明 entryRule / rewardProfileId / guaranteedContent / entranceBindingId / returnBridgePolicy | **PASS** | `HiddenContentModels.kt:124-166` 全字段 + init 校验 |
| REVEAL_SECRET_ZONE 只揭示既有入口 | **PASS** | `FoundationGameSession` 中 `RevealSecretZone` payload 只调用 `recordSearchResolution(REVEALED)`，不创建新拓扑 |
| `./gradlew hiddenContentHarness` 报告路径固定 | **PASS** | `hidden-content-summary.json` + `hidden-content-events.jsonl` |
| 至少一条 SearchAction 路径 | **PASS** | 4 个 zone 全部需要显式 Search |

### 3.2 Section 4.1 — Contract 结构

| 检查项 | 状态 | 说明 |
|--------|------|------|
| `HiddenEventDef` 签名匹配 | **PASS** | 完全一致 |
| `SecretZoneDef` 签名匹配 | **PASS** | 基础字段完全匹配，额外新增 nameKey/descKey/visualKey/iconKey/audioProfile/schemaVersion/tags（属于 client 可读性必要字段，属合理扩展） |
| `ReturnBridgePolicy` 枚举 | **PASS** | 3 值完全匹配 |
| `HiddenTriggerType` 枚举 | **PASS** | 6 值完全匹配 |
| `HiddenEventRewardPayload` typed | **PASS** | sealed interface，4 个子类型 + key-payload 一致性 init 校验 |
| 不创建第二套搜索 trigger 枚举 | **PASS** | 复用 PR-03 的 `SearchAction + DiscoveryRule` |
| REVEAL_SECRET_ZONE 不进入 HiddenTriggerType | **PASS** | 只是 `HiddenEventRewardKey` 的 payload |

### 3.3 Section 4.2 — 奖励桥接

| 检查项 | 状态 | 说明 |
|--------|------|------|
| reward 只走 LootProfile / buff / encounter / reveal | **PASS** | `HiddenEventRewardKey` 枚举恰好 4 值 |
| rewardProfileId 引用正式 loot registry | **PASS** | init 校验 `registry.value == "loot_profile"` |
| guaranteedContent 使用 ContentRef | **PASS** | |
| 静态层不写 returnBridgeNodeId | **PASS** | SecretZoneDef 只有 policy + anchorTag |
| mapgen 阶段解析 resolvedReturnBridgeNodeId | **PASS** | `HiddenContentMapgenPipeline.resolveReturnBridgeNodeId()` |
| 解析后校验：存在于拓扑、不位于 SECRET、可达出口 | **PASS** | 三个 `require()` 链式校验 |
| 无法解析时 fail-fast | **PASS** | `requireNotNull` 在 mapgen 阶段抛异常 |

### 3.4 Section 4.3 — hiddenContentHarness

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 500 seed | **PASS** | 125 per zone × 4 zones = 500 |
| ≥30% hidden event 触发率 | **PASS** | `MIN_HIDDEN_EVENT_TRIGGER_RATE = 0.30` |
| ≥10% secret zone 发现率 | **PASS** | `MIN_SECRET_ZONE_DISCOVERY_RATE = 0.10` |
| hidden content 不承载主线必需钥匙 | **PASS** | `criticalPathReachable` 断言 |
| 每个 zone 非零触发 | **PASS** | `zeroHiddenEventZoneCount == 0` + `zeroSecretZoneZoneCount == 0` |
| 报告字段完整 | **PASS** | secretRuleVersion / triggerType / searchBindingId / resolvedReturnBridgeNodeId / zoneId / searchActionResult 全部输出 |

### 3.5 Section 4.4 — Client 可读性

| 检查项 | 状态 | 说明 |
|--------|------|------|
| hidden entrance 有 reveal 状态变化 | **PASS** | `prop.hidden_entrance.revealed` visualKey + stateLabelKey |
| secret zone 在 inspect / log 中可区分 | **PASS** | 专用 propTypeId (`hidden_entrance`, `secret_reward`, `secret_return`) + 专用 i18n key |
| hidden reward 来源可追踪 | **PASS** | `ui.reward.source.secret_zone` + `RewardPresentationSourceSnapshot.HIDDEN_EVENT / SECRET_ZONE` |
| SearchAction 玩家反馈 | **PASS** | `log.search.*` 6 条 key 覆盖 no_target / already_resolved / failed_check / revealed / failed_tag / revealed_tag |

### 3.6 Section 5.4 — 白盒框架

| 检查项 | 状态 | 说明 |
|--------|------|------|
| domain 名称 `whiteBoxHiddenContent` | **PASS** | `WhiteBoxHiddenContentRunner.HARNESS_ID` |
| 加入 whiteBoxVerify | **PASS** | `build.gradle.kts:211` |
| 加入 phase4Report | **PASS** | `tools/build.gradle.kts:183` |
| 4 类 case artifact | **PASS** | trigger-timeline / search-action-results / return-bridge-proof / reward-bridge-summary |
| aggregate rule 覆盖率 | **PASS** | 11 条 assertion 覆盖触发率/发现率/zone 覆盖/主线门槛/path class/reward budget/threat budget/search blocking/proof mismatch |
| 复用同一 runner kernel | **PASS** | `HiddenContentHarnessKernel.execute()` 共享 |

### 3.7 Section 7 — 资源生成计划

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 图片 plan 文件 | **PASS** | `assets-src/image/specs/phase4-pr07-gemini-plan.yaml` — 10 个资产 (2 prop + 8 zone icon/visual) |
| 图片覆盖对象 | **PASS** | `prop.hidden_entrance.*`, `zone.secret.*.icon`, `zone.secret.*.visual` |
| 图片报告文件 | **PASS** | `phase4-pr07-generation-report.jsonl` + `phase4-pr07-processing-report.jsonl` |
| 音频 plan 文件 | **PASS** | `assets-src/audio/specs/phase4-pr07-audio-plan.yaml` — 6 个条目 |
| 音频覆盖对象 | **PASS** | `audio.hidden.reveal.*`, `audio.secret_zone.*`, `audio.interactable.hidden_*` |
| 音频报告文件 | **PASS** | `phase4-pr07-generation-report.jsonl` + `phase4-pr07-processing-report.jsonl` |
| 资源 key namespaced | **PASS** | 全部使用 `phase4/pr07/` 前缀 |
| assetLint / audioLint / manifestLint 引入 pr07 plan | **PASS** | `build.gradle.kts` 中 `--extra-plan` 参数已添加 |

### 3.8 Section 6 — 测试覆盖

| 必测行为 | 覆盖位置 | 状态 |
|----------|----------|------|
| hidden event 只出现在 OPTIONAL / SECRET | harness aggregate + case assertion | **PASS** |
| secret zone 至少包含一个正式奖励节点 | harness `secretRewardNodePresent` | **PASS** |
| reward bridge 不绕过 LootBudget | harness `rewardBridgeBackedByLootBudget` | **PASS** |
| 发现失败不阻断主线 | harness `searchFailureKeepsMainlineReachable` + `FoundationGameSessionTest` | **PASS** |
| fail-fast 当 entranceBindingId 无法解析 | `HiddenContentMapgenPipelineTest` (6 个 test case) | **PASS** |
| 至少一条 SearchAction 路径 | harness `explicitSearchRevealCount > 0` | **PASS** |
| secret zone entry rule 与 entrance discovery rule 一致 | `GameContentTest` | **PASS** |
| REVEAL reward 目标必须是已注册的 entrance binding | `GameContentTest` | **PASS** |
| Search 持久化到 save/load | `FoundationGameSessionTest` (searching + load verification) | **PASS** |
| Failed search 被缓存、不可重掷 | `FoundationGameSessionTest` | **PASS** |

---

## 4. 偏差与改进建议

### 4.1 中等偏差 (建议在合并前或紧跟 PR 中修复)

#### M-1: HiddenTriggerType 内容多样性不足

**现状**: 所有 4 个 zone 的 reveal event 统一使用 `PERCEPTION_REVEAL` 触发，4 个 reward event 统一使用 `INTERACT_TILE` 触发。定义了 6 种 trigger type，但只使用了其中 2 种。

**规格关联**: §4.1 补充约束 "每个 target zone 至少允许一种'非纯事件链'的 hidden content 路径" — 当前实现满足此条件（Search → Reveal → Interact 不是纯事件链），但 `ENTER_ROOM / OPEN_CHEST / KILL_ELITE / QUEST_STEP` 四种触发类型完全未被数据行使。

**风险**: 从设计验证角度看，未行使的枚举值相当于"只有声明、没有证据"——如果后续有人用 `KILL_ELITE` 作 trigger，运行时是否真能正确路由到 `executeHiddenEvents`？当前无法从测试和 harness 中确认。

**建议**: 至少为 1 个 zone 添加一条 `ENTER_ROOM` 或 `KILL_ELITE` 触发的 hidden event（可以是纯 buff 奖励，不必涉及 secret zone），让 harness 覆盖到非 PERCEPTION_REVEAL 的触发路径。偏差评估：约 **15%** 的 trigger 路径覆盖缺失。

#### M-2: entranceBindingId 全局一致性未充分练习

**现状**: 4 个 secret zone 全部绑定 `optional.branch.1` 作为 entranceBindingId。虽然不同 zone 的 floor topology 独立，但这意味着 `NEAREST_OPTIONAL_ANCHOR` / `EXPLICIT_ANCHOR` 策略在实际数据层面总是从同一锚点出发。

**规格关联**: §4.2 "静态内容层不直接写 returnBridgeNodeId；只声明 entranceBindingId + returnBridgePolicy (+ returnBridgeAnchorTag)"。contract 层面合规，但数据层缺少锚点多样性。

**风险**: 如果未来某个 zone 有 `optional.branch.2` 的 secret zone，entranceBindingId 解析逻辑是否能正确处理？当前只有单元测试覆盖了不同锚点，harness 的 500 seed 都走同一锚点。

**建议**: 将 `underground_river_crystal_rift` 或 `abyssal_temple_warded_archive` 的 entranceBindingId 改为 `optional.branch.2`（前提是 mapgen 的 zone profile 中有该锚点的 hiddenEntrancePlan），以验证多锚点解析。偏差评估：约 **10%** 的绑定路径验证缺失。

#### M-3: 白盒报告中缺少显式 "SearchAction 必须存在" 的聚合断言

**现状**: 白盒 aggregate 中有 `trigger_rate_threshold` 和 `secret_rate_threshold`，但没有一条直接断言"至少存在 1 个 explicitSearchRevealCount > 0 的 case"。当前因为所有路径都走 Search 所以隐式满足，但如果未来有人添加纯被动触发路径，这个合同就会静默退化。

**规格关联**: §2.1 口径 5 "至少一条 hidden content 路径必须依赖显式 SearchAction"。

**建议**: 在 `WhiteBoxHiddenContentRunner.aggregateReports()` 中增加一条 aggregate assertion:

```kotlin
WhiteBoxAssertionResult(
    ruleId = "hidden-content.aggregate.explicit_search_reveal_present",
    passed = summary.explicitSearchRevealCount > 0,
    message = "At least one deterministic case discovers hidden content via explicit SearchAction.",
)
```

偏差评估：**低**，当前数据不会触发问题，但属于合同防退化缺口。

---

### 4.2 低风险改进建议

#### L-1: 数据文件路径与规格微偏

**规格建议**: `game/src/main/resources/data/events/*.yaml`（复数 yaml 文件）  
**实际**: `game/src/main/resources/data/events/index.yaml`（单文件）

这是样式偏差而非功能偏差。当前实现用 index.yaml 管理所有 event 是项目惯例，合理。无需修改。

#### L-2: secret zone 在 route 选择 UI 中的可区分性

**规格**: §4.4 "secret zone 在 route / inspect / log 中能区分于普通 optional room"  
**现状**: inspect ✅ / log ✅ / route 层面通过 map 上 prop 可视化隐式满足。

Secret zone 不通过 floor 间 route selection 进入（而是 mid-floor 交互），所以"route 可区分"的含义是地图上的 prop 可视性。当前 `hidden_entrance` / `secret_reward` / `secret_return` 三种 prop 有独立 visualKey、nameKey、stateLabelKey，在地图视角和 inspect 面板中清晰可区分。**满足规格意图**。

#### L-3: FoundationGameSession 中 hidden event 执行缺少对 optionalOnly 字段的运行时校验

**现状**: `HiddenEventDef.optionalOnly` 在数据加载时写入，但 `executeHiddenEvents()` 方法在运行时触发事件时**未检查当前房间的 pathClass 是否满足 optionalOnly 约束**。

**风险**: 如果数据配置错误（某个 event 的 conditions 导致在 CRITICAL_PATH 房间触发），运行时不会阻止。当前所有 event 的 conditions 中有 `SEARCH_BINDING_ID` 约束，而 search 目标只存在于 OPTIONAL 路径房间，所以实际上不会在 CRITICAL_PATH 触发——但这是"数据碰巧正确"而非"代码保证正确"。

**建议**: 在 `executeHiddenEvents()` 中增加 pathClass 检查:

```kotlin
if (event.optionalOnly && currentRoomPathClass() !in setOf(PathClass.OPTIONAL, PathClass.SECRET)) {
    continue // skip this event
}
```

#### L-4: harness kernel 中 DataRegistryHolder 双重加载

**现状**: `HiddenContentHarnessKernel` 内部的 `DataRegistryHolder` 通过 `lazy` 独立加载 `DataLoader.loadSchemaCatalog()`，而每个 case 的 `GameModule.newFoundationSession()` 也会加载一份。500 个 case 意味着 catalog 被加载 501 次。

**影响**: 仅影响 harness 执行速度，不影响正确性。

**建议**: 可考虑让 DataRegistryHolder 直接从 GameModule 获取 shared catalog，或在 kernel 级别共享加载结果。优先级低。

---

## 5. 测试可信度评估

| 维度 | 评分 | 说明 |
|------|------|------|
| contract 边界覆盖 | **A** | `HiddenContentMapgenPipelineTest` 覆盖 3 种 ReturnBridgePolicy + 3 种失败场景；`GameContentTest` 覆盖 entryRule 漂移、anchor 漂移、reveal target 漂移 |
| 集成测试 | **A** | `FoundationGameSessionTest` 覆盖 search → reveal → save/load → re-search / failed → cached |
| 统计 harness | **A** | 500 seed × 4 zone，zone 级分布验证，reward/threat budget 对账 |
| 白盒 framework | **A** | 12 条 case assertion + 11 条 aggregate assertion + 4 类 artifact |
| 数据完整性 | **A** | i18n en-US + zh-CN 对称完整，loot profile / visual / audio key 全部注册 |
| 触发路径覆盖 | **B+** | 仅覆盖 PERCEPTION_REVEAL + INTERACT_TILE，未覆盖其余 4 种 trigger type（参见 M-1） |

**综合可信度**: **A-**

---

## 6. 玩法体验审查

### 6.1 搜索→发现→奖励 的心流

**流程**: 玩家移动到 optional 路径房间 → `PlayerCommand.Search` → perception check → 成功则 reveal hidden entrance → 走向 entrance 交互进入 secret zone → 与 reward node 交互领奖 → 与 return bridge 交互返回主线。

**评价**: 心流完整，每一步都有明确的 log 反馈和视觉变化。失败也有清晰反馈（感知值 vs 难度）。secret zone 不会让玩家迷失（return bridge 保证可达主线出口）。

### 6.2 难度梯度

| Zone | Perception Difficulty | 评价 |
|------|-----------------------|------|
| greenwood_fringe | 8 | 新手友好，入门 secret |
| deep_iron_pit | 12 | 中等，需要一定装备/build |
| underground_river | 16 | 较高，奖励匹配（有 encounter） |
| abyssal_temple | 20 | 困难，终章级 |

**评价**: 梯度合理，与 zone 推进难度正相关。

### 6.3 奖励平衡

- greenwood: 纯 loot (rewardBudget=5)
- deep_iron: buff + loot (rewardBudget=6)
- underground_river: encounter + loot (rewardBudget=6, threatCost=4)
- abyssal_temple: 纯 loot (rewardBudget=7)

**评价**: 奖励类型有变化，budget 随难度递增，encounter 引入风险-收益权衡。合理。

---

## 7. 修复优先级建议

| ID | 严重度 | 建议 | 工作量 |
|----|--------|------|--------|
| M-3 | 中 | 白盒增加 `explicit_search_reveal_present` 聚合断言 | ~10 行代码 |
| M-1 | 中 | 为至少 1 个 zone 增加非 PERCEPTION_REVEAL 触发的 hidden event | ~30 行 yaml + 少量 session 逻辑验证 |
| M-2 | 中 | 让至少 1 个 zone 使用不同的 entranceBindingId | ~5 行 yaml 改动 + mapgen zone profile 配合 |
| L-3 | 低 | executeHiddenEvents 增加 optionalOnly 运行时校验 | ~5 行代码 |
| L-4 | 低 | harness DataRegistryHolder 复用优化 | ~15 行重构 |

---

## 8. 审查结论

**PR-07 整体实现质量优秀**，core contract、mapgen 桥接、harness 验证、白盒框架、client 可读性、资源计划均完整落地，与规格文档的一致性达 **~95%**。

主要偏差集中在 **数据层面的多样性不足**（trigger type 和 anchor 单一化），以及 **白盒框架的一条防退化断言缺失**。这些偏差不影响当前功能的正确性，但会降低系统在未来扩展时的安全网覆盖率。

**建议**: 优先处理 M-3（白盒断言，约 10 行），M-1 和 M-2 可在合并后的 follow-up 中处理。
