> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md`
> `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
> `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - PR-07 Hidden Event、Secret Zone 与 Client Readability

**阶段**: `Phase 4 / P4-B / P4-W4b`  
**优先级**: `P0`  
**前置条件**: `PR-03`, `PR-05`, `PR-06` 完成  
**对应问题**: `PR-03` 只冻结了 hidden entrance 的 proof 和 discovery rule，还没有正式的 `HiddenEventDef / SecretZoneDef / reward bridge`。如果直接堆 secret reward 而不补 harness、client 可读性和返回主线桥接，`Phase 4` 的“隐藏内容成立”仍然无法自证。

---

## 1. 阶段目标

把 hidden content 从“可发现入口”推进到“可验证、可读、可奖励”的正式系统。

完成标准：

1. `HiddenEventDef / SecretZoneDef` 进入正式 registry。
2. hidden reward 和 secret zone 不绕过 `LootBudget` 与 `SolvabilityGraph`。
3. `hiddenContentHarness` root alias 建立，并覆盖 checklist 中的触发率和 secret zone 出现率。
4. client 侧可读性冻结：hidden entrance / secret zone / reveal / mutation 来源都能看懂。

## 2. 当前问题

1. 目前只有 hidden entrance proof，没有正式 event/reward schema。
2. secret zone 的奖励、桥接和返回主线路径还没有结构化 contract。
3. 若不补 client 可读性，hidden content 即使存在也会退化成“只有作者知道怎么触发”。

### 2.1 本 PR 必须冻结的口径

1. hidden event 默认只允许出现在 `OPTIONAL / SECRET` 路径。
2. `SecretZoneDef` 必须显式声明 `entryRule / rewardProfileId / guaranteedContent / entranceBindingId / returnBridgePolicy`。
3. `REVEAL_SECRET_ZONE` 只揭示既有入口，不创建新的物理入口。
4. root alias `./gradlew hiddenContentHarness` 必须建立，并固定报告路径：
   - `tools/build/reports/phase4/hidden/hidden-content-summary.json`
   - `tools/build/reports/phase4/hidden/hidden-content-events.jsonl`
5. 至少一条 hidden content 路径必须依赖显式 `SearchAction`，不允许全部退化为纯被动感知。

## 3. 范围与非目标

### 3.1 范围

1. `HiddenEventDef / SecretZoneDef` schema。
2. reward bridge 与 loot profile 接线。
3. `hiddenContentHarness`、触发率统计和 reproducibility 报告。
4. hidden entrance / secret zone 客户端可读性。
5. 对应资源计划。

### 3.2 非目标

1. 不在本 PR 新增更复杂的 chain event 或 multi-stage puzzle。
2. 不在本 PR 修改 `SolvabilityGraph` 的基础词汇；只消费 `PR-03` 已冻结的 proof contract。

## 4. 技术方案

### 4.1 Hidden event 与 secret zone contract

建议文件：

```text
game/src/main/resources/data/events/*.yaml
game/src/main/resources/data/secret-zones/*.yaml
game/src/main/kotlin/com/ktome/game/hidden/*
```

核心结构：

```kotlin
data class HiddenEventDef(
    val id: String,
    val triggerType: HiddenTriggerType,
    val conditions: List<HiddenEventCondition>,
    val rewards: List<HiddenEventReward>,
    val optionalOnly: Boolean = true,
)

data class SecretZoneDef(
    val id: ContentRef,
    val entryRule: DiscoveryRule,
    val pathClass: PathClass,
    val rewardProfileId: ContentRef,
    val guaranteedContent: List<ContentRef>,
    val entranceBindingId: NodeAnchorId,
    val returnBridgePolicy: ReturnBridgePolicy,
    val returnBridgeAnchorTag: String? = null,
)
```

```kotlin
enum class ReturnBridgePolicy {
    NEAREST_OPTIONAL_ANCHOR,
    LAST_MAINLINE_BRANCH,
    EXPLICIT_ANCHOR,
}
```

`HiddenTriggerType` 在本 PR 继续复用主文档的事件型 taxonomy，负责描述 hidden event 的正式触发源，例如：

1. `ENTER_ROOM`
2. `OPEN_CHEST`
3. `KILL_ELITE`
4. `INTERACT_TILE`
5. `QUEST_STEP`
6. `PERCEPTION_REVEAL`

补充约束：

1. 每个 target zone 至少允许一种“非纯事件链”的 hidden content 路径。
2. 整个 Phase 4 主线至少存在一条必须先执行 `SearchAction` 才能 reveal 的 hidden content 路径，用于证明玩家主动探索是正式 contract，而不是附带 UI 按钮。
3. 主动搜索一律复用 `PR-03` 已冻结的 `SearchAction + DiscoveryRule(predicates + combinator)` 合同，不得再造第二套搜索 trigger 枚举。
4. 脚本揭示若存在，只能作为 `HiddenEventReward(REVEAL_SECRET_ZONE)` 的 payload 解释层语义；它不是独立触发类型，也不进入 `HiddenTriggerType` 的第二套 taxonomy。

### 4.2 奖励桥接

正式规则：

1. hidden reward 只允许走：
   - `LootProfile`
   - `temporary buff`
   - `special encounter`
   - `secret entrance reveal`
2. `rewardProfileId` 必须引用正式 loot registry。
3. `guaranteedContent` 使用 `ContentRef` typed 引用，不允许自由字符串；runtime 支持的 registry 固定为 `hidden_event`、`monster`、`special_item_template`。
4. `HiddenEventReward` 的 payload 必须复用主文档已冻结的 typed payload 结构，不允许把 `REVEAL_SECRET_ZONE / LOOT_PROFILE / TRIGGER_ENCOUNTER / GRANT_BUFF` 再降回自由字符串。
5. 静态内容层不直接写 `returnBridgeNodeId`；只声明 `entranceBindingId + returnBridgePolicy (+ returnBridgeAnchorTag)`。
6. mapgen / solvability 实例化阶段必须把这些静态绑定解析成运行时 `resolvedReturnBridgeNodeId`，并满足：
   - 在 `SolvabilityGraph` 中存在
   - 不位于 `SECRET` 私有死端
   - 从 secret zone 返回后仍能抵达当前 floor 的主线路径出口
7. `resolvedReturnBridgeNodeId` 的校验必须在加载期或 floor materialization 期 fail-fast；禁止运行到事件触发时才发现无法回主线。

### 4.3 `hiddenContentHarness`

建议任务落位：

```text
tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt
build.gradle.kts
```

固定检查：

1. `500` seed
2. `>=30%` run 触发 `1` 个 hidden event
3. `>=10%` run 发现 `1` 个 secret zone
4. hidden event / secret zone 不承载主线必需钥匙
5. 每个 target zone 都必须有非零触发记录；若某个 zone 的 hidden event 或 secret zone 命中率为 `0`，直接判失败。
6. 报告中必须记录 `secretRuleVersion`、`triggerType`、`searchBindingId`、`resolvedReturnBridgeNodeId`、`zoneId`、`searchActionResult`，方便同时和 `SearchAction` / `SolvabilityProof` 对账；如需额外记录入口锚点，统一按 `NodeAnchorId` 口径命名。

### 4.4 client 可读性

最低要求：

1. hidden entrance 有明确可见的 reveal 状态变化。
2. secret zone 在 route / inspect / log 中能区分于普通 optional room。
3. hidden reward 来源可追踪，不显示裸 id。
4. `SearchAction` 必须有明确的玩家反馈：
   - 可见的输入入口或操作提示
   - 成功/失败反馈
   - 不暴露内部判定细节，但要让玩家理解“刚才执行了一次搜索”

## 5. 推荐改动面

### 5.1 `game`

1. 新建 hidden event / secret zone 数据目录和 runtime。
2. zone runtime 中接入奖励桥和返回主线桥接。
3. 加载时先把 `entranceBindingId + returnBridgePolicy (+ returnBridgeAnchorTag)` 解析为实例级 `resolvedReturnBridgeNodeId`，并与 `SolvabilityGraph` 校验一致，不允许 deferred validation。

### 5.2 `tools`

1. 新建 `hiddenContentHarness`。
2. root `build.gradle.kts` 暴露 alias。
3. 记录 discoveryRule、rewardProfile、returnBridge 等关键信息。
4. 输出 zone 级分布和 `SearchAction` 命中情况，避免只看全局平均值。

### 5.3 `client`

1. 新增 secret zone / hidden event 的 log text、inspect、route 可见性处理。
2. 凡新增表现必须复用正式 `nameKey / descKey / visualKey / audioProfile`。
3. 为 `SearchAction` 提供最小反馈链路：输入提示、尝试结果、reveal 后状态切换。

### 5.4 `tools / white-box` 补充改造

1. `hiddenContentHarness` 继续作为业务主 alias，同时新增或内联输出统一 white-box 报告；正式 domain 名称固定为 `whiteBoxHiddenContent`。
2. `whiteBoxHiddenContent` 必须加入：
   - `whiteBoxVerify`
   - `phase4Report`
3. 该 domain 的 case artifact 至少包括：
   - trigger timeline
   - `SearchAction` 结果表
   - secret zone entry / return bridge proof
   - reward bridge 摘要
4. aggregate rule 至少覆盖：
   - `>=30%` run 触发 hidden event
   - `>=10%` run 发现 secret zone
   - 每个 target zone 非零触发
   - hidden content 不承载主线硬门槛
5. `hiddenContentHarness` 与 `whiteBoxHiddenContent` 可以复用同一 runner，但不得形成“统计报告一套、AI artifact 一套”的双重真源。

## 6. 测试与自证

### 6.1 必测行为

1. hidden event 默认只出现在 `OPTIONAL / SECRET`。
2. secret zone 至少包含一个正式奖励节点。
3. reward bridge 不绕过 `LootBudget`。
4. 发现失败不阻断主线。
5. `entranceBindingId + returnBridgePolicy (+ returnBridgeAnchorTag)` 无法解析出有效 `resolvedReturnBridgeNodeId` 时，会在加载/实例化阶段 fail-fast，而不是在 run 中 late crash。
6. 至少一条 hidden content 只能通过显式 `SearchAction` 命中。

### 6.2 自动化命令

```bash
./gradlew :core:test
./gradlew hiddenContentHarness
./gradlew whiteBoxHiddenContent
./gradlew whiteBoxVerify
./gradlew phase4Report
./gradlew clientSmoke
./gradlew goldenScreenshot
```

### 6.3 白盒验证

1. 至少触发一次 `REVEAL_SECRET_ZONE`。
2. 至少进入一次 secret zone 并确认能回主线。
3. 检查 log / inspect / route 文本可读，不暴露内部 id。
4. 至少手动执行一次 `SearchAction`，确认失败反馈、成功反馈和 reveal 后状态变化都清晰可见。

### 6.4 统一白盒框架验证

1. `whiteBoxHiddenContent` 必须自动断言：
   - hidden event 默认只出现在 `OPTIONAL / SECRET`
   - secret zone 至少包含正式奖励节点
   - reward bridge 不绕过 `LootBudget`
   - `SearchAction` 失败不阻断主线
   - `resolvedReturnBridgeNodeId` 与 `SolvabilityProof` 一致
2. AI triage 入口固定为：
   - `whitebox-hidden-content-summary.json`
   - `whitebox-hidden-content-cases.jsonl`
   - per-case artifacts
3. 失败 case 必须能反查：
   - `zoneId`
   - seed
   - `searchBindingId`
   - `entranceBindingId`
   - `resolvedReturnBridgeNodeId`
   - `searchActionResult`

## 7. 资源生成计划

### 7.1 图片

1. 计划文件：`assets-src/image/specs/phase4-pr07-gemini-plan.yaml`
2. 覆盖对象：
   - `prop.hidden_entrance.*`
   - `zone.secret.*.icon`
   - `zone.secret.*.visual`
3. 报告文件：
   - `assets-src/image/manifests/phase4-pr07-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-pr07-processing-report.jsonl`
4. `*-gemini-plan.yaml` 是沿用现有图片资产生成计划文件命名，不代表新的运行时资源格式。

### 7.2 音频

1. 计划文件：`assets-src/audio/specs/phase4-pr07-audio-plan.yaml`
2. 覆盖对象：
   - `audio.hidden.reveal.*`
   - `audio.secret_zone.*`
   - `audio.interactable.hidden_*`
3. 报告文件：
   - `assets-src/audio/manifests/phase4-pr07-processing-report.jsonl`

### 7.3 约束

1. 资源 key 必须 namespaced，不能污染 base key 空间。
2. 所有新增 key 均需通过现有 `assetLint / manifestLint / audioLint`，必要时补 `goldenScreenshot`。
3. `gemini` 仅表示图片 plan 文件命名约定，不能被实现方误解成 pack/runtime 需要感知的新 schema。

## 8. 出口门禁

1. `HiddenEventDef / SecretZoneDef` 与 reward bridge 口径冻结。
2. `hiddenContentHarness` 正式可用，并输出结构化报告。
3. client 可读性满足白盒验证，不再依赖作者记忆触发路径。
4. hidden content 不再只是“被动偶遇”，至少有一条正式的主动搜索路径。
5. 静态 schema 不再直接持有 runtime node id；return bridge 通过 anchor/policy 绑定再实例化。
6. `whiteBoxHiddenContent` 已接入统一白盒框架，可作为 `PR-07` 的 AI 主验证入口。
