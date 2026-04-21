> 执行前必须先完整阅读并接受：
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-03-organic-hidden-loop-secret-reward-identity-and-replay-hook.md`
> 最新 `tools/build/reports/verification/phase4/report-phase4-summary.{md,json}`
> 最新 `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.{md,json}`

# Phase 4 Organic Hidden、Solvability Lane 与 Secret Reward Authority 长期收口方案

**阶段**: `Phase 4 / Post-PR-03 Long-term Hardening`  
**优先级**: `P0`  
**工作量评估**: `L+`（`10~13` 人日）  
**前置条件**: 外部 `v3 PR-03`（organic hidden loop / secret reward identity / replay hook）第一轮 closure 已完成，且最新 canonical `phase4Report` 至少能稳定 materialize  
**对应问题**:

1. `organicHiddenProbe` 现在仍是“大 runner + 大 summary + 大 materialization”一体化结构，bot 语义、owner 聚合语义、artifact 输出语义纠缠在一起。结果是只要有一处语义漂移，通常要等整跑 `organicHiddenProbe + phase4Report` 才会暴露。
2. `whiteBoxSolvability` 目前把 reveal-success 与 reveal-fail 两种相反语义塞在同一 corpus 里，aggregate contract 天然拉扯，最后只能靠删除旧 rule 或留着“没人消费的统计项”来维持表面绿色。
3. secret reward authority 虽然第一轮已经在 runtime / validator / whiteBoxLoot 上收过一次，但 `contentPackHarness`、`whiteBoxLoot`、validator、未来 report/input 层仍有继续长出第二口径的风险。
4. 这三件事现在都已经不是“单点 bug”，而是 authority / lane / test layering 还没彻底冻结的问题。继续 patch 只会让下次定位成本更高。

---

## 0. 当前状态快照

基于当前 review finding 与现有 Phase 4 验证面，当前问题不是“数值阈值还没调够”，而是三条长期 contract 还没有正式拆开：

1. `organicHiddenProbe`
   - headline owner metric 已经切成 `leadDiscoveryRate + secretConversionRate`
   - 但 bot 策略、summary 聚合、payload materialization 仍在同一 runner 内联维护
   - 支撑证据如 `firstHiddenDiscoveryTurn` 的语义一旦改动，容易与 headline metric 继续漂移
2. `whiteBoxSolvability`
   - 现在的 pilot corpus 同时承担：
     - critical path `100%` reachability
     - hidden anchor family coverage
     - reveal-success coverage
     - reveal-fail coverage
   - 其中 reveal-success 与 reveal-fail 是不同 lane 的目标，继续共用一个 corpus 会让 aggregate rule 变成“不是过宽就是互相打架”
3. `contentPackHarness / lootBalanceLab / whiteBoxLoot / validator`
   - 第一轮已经把 secret-zone reward authority 收到 `SecretZoneDef.rewardProfileId`
   - 但 tools 侧仍然容易再手写一套“如果 event 带 `LOOT_PROFILE` 也算数”的平行逻辑
   - 只要 authority 解析和 authority 断言不是共享 helper，下一轮 review 还会再次出现第二真源

这说明长期方案的目标不该再是“把当前报告刷绿一次”，而应该是：

1. 把 owner metric 语义层拆开，保证 drift 能在更早层失败。
2. 把 white-box lane 语义拆开，保证 reveal-success 与 reveal-fail 不再共享一份互相矛盾的 aggregate contract。
3. 把 secret reward authority 抽成 shared helper / shared assertion builder，保证 tools 侧不能再发明第二口径。

---

## 1. 阶段目标

本方案不是再做一轮 `PR-03` patch，而是把 hidden / solvability / reward authority 这三条链的长期维护成本降下来。

完成标准：

1. `organicHiddenProbe` 拆成三层正式结构：
   - bot policy
   - summary aggregator
   - artifact writer
2. `leadDiscoveryRate`、`firstHiddenDiscoveryTurn`、`failingSecretEntryZoneIds` 都能在不跑整套 owner artifact 的前提下单独单测。
3. `whiteBoxSolvability` 拆成 reveal-success 与 reveal-fail 两条正式 lane，各自有独立 corpus 和 aggregate rule。
4. secret reward authority 的解析与断言只有一个共享实现，runtime、validator、`contentPackHarness`、`whiteBoxLoot`、phase report/input 都从同一入口消费。
5. canonical `phase4Report` 只消费新的 lane / authority / owner artifact，不再带 legacy 兼容分叉。

---

## 2. 为什么必须拆成一组 PR

### 2.1 这不是单点 bug，而是三条 contract 还没冻结

如果继续在同一个 PR 里同时改 bot、改 summary、改 lane、改 authority helper，最后只会得到：

1. diff 很大，但谁是 owner 不清楚；
2. baseline / report schema 漂移和 runtime 行为漂移混在一起；
3. 一旦出红，很难判断是 bot 行为变了、聚合逻辑变了，还是 writer 断言变了。

### 2.2 `organicHiddenProbe` 与 `whiteBoxSolvability` 是不同维度的问题

1. `organicHiddenProbe` 的问题是 owner metric layering 不清楚。
2. `whiteBoxSolvability` 的问题是 lane 语义没有拆开。
3. 两者都和 hidden 相关，但一个属于 `organic owner observation`，一个属于 `deterministic white-box proof`。

如果把它们混成一个“hidden 大重构 PR”，后续 review 会非常难做。

### 2.3 secret reward authority 必须早于 organic layering / lane split 的最终 cutover

原因是：

1. `SecretZoneDef.rewardProfileId`、`HiddenEventRewardPayload.SecretZoneReward` 与 validator fail-fast 已经是当前稳定事实，不需要再等 owner/report 结构重排后才能抽 helper。
2. 如果不先把 shared helper 做好，后续 organic layering、lane split、canonical report cutover 都还会继续各自维护一份 secret reward 解释。
3. shared helper 放在 `game/.../hidden/`，不会反向依赖 `tools`，blast radius 最小。

因此最合理的顺序是：

1. 先抽 authority helper
2. 再拆 owner layering
3. 再拆 white-box lane
4. 最后做 canonical cutover 和文档/兼容路径清理

---

## 3. 必须冻结的合同

1. `organicHiddenProbe` 的 owner headline 继续固定为：
   - `leadDiscoveryRate`
   - `secretConversionRate`
   - `searchActionUseRate`
   - `secretZoneEntryRate`
2. `firstHiddenDiscoveryTurn` 的正式语义固定为：
   - `min(firstRevealTurn, firstSecretZoneEntryTurn)`
   - 明确不等于 primer-only / hidden-event-only signal
3. 如果后续仍需要“更早但更弱”的时序证据，`firstSignalTurn` 必须单独命名、只进入 `organic-hidden-probe-summary.json` 的 `supportingEvidence.*`，禁止混入 `firstHiddenDiscoveryTurn`、headline 字段与 phase aggregate。
4. `whiteBoxSolvability` 的 reveal-success lane 与 reveal-fail lane 必须拥有：
   - 不同 corpus
   - 不同 aggregate assertion 集
   - 不同 artifact 输出路径
5. `SecretZoneDef.rewardProfileId` 是 secret-zone reward 的唯一 authority。
6. 通用 `HiddenEventRewardPayload.LootProfile` 继续允许用于非 secret-zone hidden event；本方案只禁止它在 secret-zone context 中重新成为 reward fallback 第二真源。
7. `OrganicHiddenProbeRunner` 继续是 cache owner；`BotPolicy / SummaryAggregator / ArtifactWriter` 不得读写 `kernelCache` 或 `VerificationCacheSupport.cacheDirs(...)` 返回的路径。
8. `whiteBoxSolvability` fail lane 的 seed / fixture 来源必须明确冻结时机；本方案完成前必须完成冻结，但允许拆入子 PR 分步推进，不能靠“主 corpus 恰好出现 fail case”隐式成立。
9. `phase4Report` 仍然是 canonical aggregate owner，不新增第二套 phase summary。
10. canonical summary schema 是否从 `report-phase4-v2` bump 到新版本，必须在最终 cutover PR 中显式决策，不能隐式漂移。
11. `Phase 4` producer inventory 继续只以 `tools/src/main/resources/phase4/aggregation-manifest.yaml` 为 authoritative source。
12. 任何 `tools` 任务不得自行解释：
   - `SECRET_ZONE_REWARD`
   - secret-zone context 中的 `LOOT_PROFILE` fallback
   - secret reward profile fallback
   这些必须全部通过 shared helper / shared assertion builder 获取。

---

## 4. 范围与非目标

### 4.1 范围

1. `organicHiddenProbe` 结构分层
2. `whiteBoxSolvability` lane 正式拆分
3. secret reward authority 共享 helper / 共享 assertion builder
4. phase aggregate / docs / compatibility cleanup

### 4.2 非目标

1. 不重新做一轮 hidden content 数值/内容大扩容。
2. 不在这组 PR 里继续新增新的 hidden trigger type、discovery rule type 或 secret zone schema family。
3. 不重做 `SearchAction` 正式 contract。
4. 不在这组 PR 里重新平衡 `underground_river` / `abyssal_temple` 的内容，只处理能让未来定位和 gate 更稳定的结构问题。
5. 不把 `whiteBoxSolvability` lane split 扩成统一 white-box lane framework 的 repo-wide 重构。
6. 不修改 `hiddenContentHarness` 的 scripted correctness owner 职责。
7. 不调整 `solvabilityHarness` 的 `1000` seed 主 gate 覆盖与 owner 语义。
8. 默认不改变 `verifyOwner` 的 routed owner task 集；若 `PR-03` 最终采用双 task 方案，相关 registry 更新被纳入该 PR 本身范围，不视为 scope 溢出。
9. 不借这组 PR 修改 `.reportOnly` 后缀的命名/阻塞语义；这类 staged gate cutover 仍需独立 PR。

---

## 5. 设计总览

### 5.1 Organic Hidden Probe：三层正式结构

目标不是“把一个大文件拆小”，而是把三种不同 owner 语义分开。

建议结构：

```text
tools/src/main/kotlin/com/ktome/tools/hidden/
  OrganicHiddenProbeRunner.kt
  OrganicHiddenProbeBotPolicy.kt
  OrganicHiddenProbeSummaryAggregator.kt
  OrganicHiddenProbeArtifactWriter.kt
  OrganicHiddenProbeModels.kt
```

职责固定如下：

1. `OrganicHiddenProbeBotPolicy`
   - 只负责从 `RunObservation` 决定 `Search / Wait / delegate`
   - 只维护 rejection / cooldown / searched-position budget 等 bot 层状态
   - 不关心 summary 字段、markdown、json 输出
2. `OrganicHiddenProbeSummaryAggregator`
   - 输入：`List<OrganicHiddenProbeCaseResult>`
   - 输出：`OrganicHiddenProbeSummary`
   - 纯函数，不读取文件，不启动 session，不拼 markdown
   - 负责：
     - `leadDiscoveryRate`
     - `secretConversionRate`
     - `failingSecretEntryZoneIds`
     - `firstHiddenDiscoveryTurn*`
     - `firstSecretZoneEntryTurn*`
3. `OrganicHiddenProbeArtifactWriter`
   - 只负责把 `summary + zoneBreakdown + combinations + notes` 写成：
     - `summary.json`
     - `events.jsonl`
     - `report.md`
   - 不重新推导 owner metric
4. `OrganicHiddenProbeRunner`
   - 只负责 case execution、复用 cache、串起 policy + aggregator + writer

补充设计：

1. 如需保留更早的“signal 层时间”，新增 `firstSignalTurn`，但它只能是 supporting evidence，不能再共用 `firstHiddenDiscoveryTurn`。
2. `OrganicHiddenProbeSummaryAggregator` 必须暴露纯函数入口，允许构造 fixture case list 做 focused test。
3. `OrganicHiddenProbeArtifactWriter` 必须能够被独立测试，而不是只能靠 full probe run 间接覆盖。
4. `VerificationCacheSupport`、`kernelCache.cacheStatus` 与 `MISS -> HIT` integration 语义继续由 `OrganicHiddenProbeRunner` 独占；policy / aggregator / writer 不得接管 cache key 或 cache 目录。

### 5.2 WhiteBox Solvability：正式双 lane

建议把现有 `whiteBoxSolvability` 从“一个 corpus + 一堆 aggregate rule”改成“一个 domain，下挂两个 lane”。

建议结构：

```text
tools/src/main/kotlin/com/ktome/tools/mapgen/
  WhiteBoxSolvabilityRunner.kt
  WhiteBoxSolvabilityLaneSpec.kt
  WhiteBoxSolvabilitySuccessLane.kt
  WhiteBoxSolvabilityFailLane.kt
```

固定 lane：

1. `reveal-success`
   - 目标：证明 upgraded zones 的主路径、hidden anchor family、reveal-success / backtrack proof 继续成立
   - 典型 aggregate：
     - `criticalPathFailureCount == 0`
     - `casesWithReveal > 0`
     - `casesWithBacktrackProof > 0`
     - `hiddenAnchorFamilyFailureCount == 0`
2. `reveal-fail`
   - 目标：证明指定 fail fixture / fail seed 仍能稳定产出 reveal-fail，并保留解释性 proof
   - 典型 aggregate：
     - `casesWithFail > 0`
     - `failReason taxonomy` 完整
     - 不要求 `casesWithReveal > 0`
     - 不参与“主 corpus 必须全绿成功率”的 gate

fail lane 的 seed 来源必须单独冻结，允许两种正式路径二选一：

1. 在 `solvability harness` 层新增显式 fail-fixture 注入点；
2. 先扫描主 deterministic corpus，提取稳定 `casesWithFail > 0` 的 seed snapshot，再固化为 fail lane 专用 corpus。

若 seed 来源在一开始无法稳定冻结，则 `PR-03` 必须拆成：

1. lane 结构与 artifact 路径搬迁
2. fail fixture / fail seed taxonomy 固化

artifact 约束：

```text
tools/build/reports/phase4/whitebox/solvability/
  reveal-success/
  reveal-fail/
```

report / aggregate 约束：

1. `phase4Report` 只显式消费 lane-aware evidence。
2. reveal-fail lane 是辅助 deterministic diagnostic，不得再通过“统计项留着但 assertion 删掉”的方式半残存在 success corpus 里。

### 5.3 Secret Reward Authority：shared helper + shared assertion builder

建议把 authority 拆成 runtime resolver 与 catalog assertion scanner 两个共享入口，都放在 `game`，因为 authority 本身属于 runtime/content contract，而不是 `tools` 私有推断。

建议结构：

```text
game/src/main/kotlin/com/ktome/game/hidden/
  SecretRewardAuthority.kt
  SecretRewardAuthorityAssertions.kt
```

职责固定如下：

1. `SecretRewardAuthority`
   - 输入：`SecretZoneDef?`、`HiddenEventDef?`
   - 输出：
     - `rewardProfileId: String`
     - `source: ResolutionSource`（至少包含 `SECRET_ZONE_DEF` / `MISMATCH`）
     - `mismatchReason: String?`
   - 只认：
     - `SECRET_ZONE_REWARD -> SecretZoneDef.rewardProfileId`
   - 只服务 `SECRET_ZONE_REWARD` payload 的 runtime / content-pack 解析面
   - `HiddenEventRewardPayload.LootProfile` 的通用路径不经过 resolver，继续直接读 `payload.lootProfileRef.id`
2. `SecretRewardAuthorityAssertions`
   - 为 validator / `LootBalanceLabRunner` / `whiteBoxLoot` / `contentPackHarness` 提供共享断言构建
   - 产出统一的 violation payload，而不是三边各写一套 message / culprit format
   - 负责 catalog-scan 级的“列出所有 authority violation”
   - `scanCatalog(...)` 至少必须覆盖以下 reason：
     - `missing_hidden_event`
     - `loot_profile_present`
     - `secret_zone_reward_count_<n>`
   - `SecretRewardAuthorityViolation` 至少必须包含：
     - `culpritId`
     - `reason`
     - `secretZoneId`
     - `hiddenEventId`

必须消费这套 shared helper 的位置：

1. `FoundationGameSession`
2. `Phase4StaticContentValidator`
3. `LootBalanceLabRunner.buildSecretZoneRewardAuthorityViolations`
4. `WhiteBoxLootRunner`
5. `ContentPackHarnessRunner`
6. 若 future `Phase4AggregationInputRunner` / phase4Report debug evidence 引入 secret-zone reward 解析，必须通过这套 shared helper；禁止绕过

长期目标不是“减少几行代码”，而是让 future review 不可能再问“为什么 tools 这边还接受 fallback LOOT_PROFILE”。

### 5.4 Canonical Cutover：最后再清 compat path

前面三条结构稳定后，最后一条 PR 再统一处理：

1. alias / compat metric 清理
2. lane-aware report rendering
3. docs / checklist / review note 同步
4. baseline / canonical artifact schema 升级

这样可以避免在结构未稳时就提前 cutover canonical gate。

---

## 6. PR 拆分总览

| PR | 主题 | 主要模块 | 主要产出 | 进入下一 PR 的门槛 |
| --- | --- | --- | --- | --- |
| `PR-01` | secret reward authority shared helper cutover | `game`, `tools` | `SecretRewardAuthority.resolve(...)` + `SecretRewardAuthorityAssertions.scanCatalog(...)`，runtime/validator/tools 全部改走共享 authority | 不再存在任何 secret reward fallback 第二口径 |
| `PR-02` | `organicHiddenProbe` 分层与可测化 | `tools` | bot policy、summary aggregator、artifact writer 三层拆分；focused unit + smoke tests | 不跑整套 probe 也能验证 bot / summary / writer 的关键语义 |
| `PR-03` | `whiteBoxSolvability` success/fail 双 lane | `tools` | `reveal-success` / `reveal-fail` 两条正式 lane、独立 aggregate contract、独立 artifact 路径 | 不再在 success corpus 中维持 reveal-fail 残留统计或伪 gate |
| `PR-04` | canonical report / docs / compat cleanup | `tools`, docs | lane-aware `phase4Report`、compat alias 清理、checklist/docs 同步、最终 gate 收口 | canonical artifact、docs、tests、review note 全部指向新结构 |

固定规则：

1. 当前 PR 未完成前，不并行推进下一 PR。
2. 当前 PR 的 tests / report / docs / compat path 未收口，不允许进入下一 PR。
3. `PR-04` 才允许删 compat alias 和 legacy wording；前面 PR 以“先建立新结构，再切 canonical” 为原则。
4. 不并行推进的原因不是编码冲突，而是 `PR-01` 的 shared authority 签名一旦漂移，会同时影响 `PR-02 / PR-03` 的 aggregator 与 lane assertion；并行推进只会把最终 `PR-04` 再次变成三条结构同时合流的 cutover。

建议估时：

1. `PR-01`：`2` 人日
2. `PR-02`：`2` 人日
3. `PR-03`：`3~4` 人日
4. `PR-04`：`3` 人日

---

## 7. PR-01：Secret Reward Authority Shared Helper Cutover

### 7.1 目标

把 secret reward authority 的解析与断言收成一份 shared helper，解决：

1. runtime / validator / `LootBalanceLabRunner` / `whiteBoxLoot` / `contentPackHarness` 再次各写一套 authority 逻辑；
2. tools 侧继续默许 `LOOT_PROFILE` fallback；
3. violation 格式、culprit id、message taxonomy 不统一；
4. future report/debug evidence 再次围绕 secret reward authority 派生第二真源。

### 7.2 范围

涉及文件建议：

1. 新增：
   - `game/src/main/kotlin/com/ktome/game/hidden/SecretRewardAuthority.kt`
   - `game/src/main/kotlin/com/ktome/game/hidden/SecretRewardAuthorityAssertions.kt`
2. 修改：
   - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
   - `game/src/main/kotlin/com/ktome/game/Phase4StaticContentValidator.kt`
   - `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt`
   - `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt`
   - `tools/src/main/kotlin/com/ktome/tools/contentpack/ContentPackHarnessRunner.kt`
   - 如有需要，`tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt`
3. 对应 test：
   - `:game:test` 全量回归
   - 其中关键 contract tests 必须被单独列出并在 PR 描述中点名，例如 `ContentPackRewardPresentationTest`、`DataLoaderContentPackTest`、`SchemaV2LoaderTest`
   - `:tools:test` 中 loot/content-pack/phase4 相关 contract tests

### 7.3 任务拆解

#### Task 1：runtime resolver

增加：

```kotlin
SecretRewardAuthority.resolve(secretZone, hiddenEvent): ResolvedSecretReward
```

要求：

1. 只处理单对 `(secretZone, hiddenEvent)` 的当前 authority 解析。
2. 返回的 `ResolvedSecretReward` 至少必须包含：
   - `rewardProfileId: String`
   - `source: ResolutionSource`
   - `mismatchReason: String?`
3. 不负责 catalog-scan。
4. 明确保留“非 secret-zone hidden event 仍可合法使用 `LOOT_PROFILE`”。

#### Task 2：catalog assertion scanner

增加：

```kotlin
SecretRewardAuthorityAssertions.scanCatalog(schemaCatalog): List<SecretRewardAuthorityViolation>
```

要求：

1. validator 直接复用这条扫描结果。
2. `LootBalanceLabRunner.buildSecretZoneRewardAuthorityViolations(...)` 不再自行推导。
3. `whiteBoxLoot` 不再自行拼 violation payload。
4. `contentPackHarness` 不再维护第二解释路径。
5. culprit id 与 violation reason 集必须对齐当前 catalog-scan 事实，不允许 runtime/validator/tools 各自增删 reason。

#### Task 3：second-authority cleanup

1. 删除 tools 侧 secret reward fallback path。
2. 删除“只在本工具里接受 legacy secret reward 解释”的 compat 分支。
3. 统一 culprit id、violation reason、reward key summary 的输出格式。
4. 同批次核对 `examples/content-packs/` 与 `tools/src/main/resources/fixtures/` 下受 secret reward authority 影响的 sample/fixture 数据，确保 shared authority 解析结果与示例数据一致。
5. 至少提供一条 architectural / contract test，阻止 `tools` 模块再次在 secret-zone 上下文里新增 `HiddenEventRewardPayload.LootProfile` fallback 处理分支。

### 7.4 退出条件

以下全部满足才能进入 `PR-02`：

1. `FoundationGameSession`、validator、`LootBalanceLabRunner`、`whiteBoxLoot`、`contentPackHarness` 都从 shared authority 入口消费。
2. 不再存在 secret reward fallback 的第二解释路径。
3. 通用 `LOOT_PROFILE` 在非 secret-zone hidden event 上继续合法，不发生 over-remove。
4. secret reward authority violation 的 culprit 格式在 validator / whiteBox / harness 中完全一致。

### 7.5 推荐验证

```bash
./gradlew :game:test --tests 'com.ktome.game.ContentPackRewardPresentationTest'
./gradlew :game:test --tests 'com.ktome.game.data.DataLoaderContentPackTest'
./gradlew :tools:test --tests 'com.ktome.tools.loot.WhiteBoxLootRunnerTest'
./gradlew :tools:test --tests 'com.ktome.tools.contentpack.ContentPackHarnessRunnerTest'
./gradlew whiteBoxLoot
./gradlew contentPackHarness
./gradlew phase4Report
```

---

## 8. PR-02：OrganicHiddenProbe 分层与可测化

### 8.1 目标

把 `organicHiddenProbe` 从“大 runner”改成“三层正式结构”，解决：

1. bot 策略测试只能靠整跑；
2. summary 语义测试只能靠整跑；
3. artifact writer 缺字段只能靠整跑；
4. `firstHiddenDiscoveryTurn` 和 headline metric 容易再次漂移；
5. cache / total case integration 契约在重构时容易被无意打破。

### 8.2 范围

涉及文件建议：

1. `tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt`
2. 新增：
   - `OrganicHiddenProbeBotPolicy.kt`
   - `OrganicHiddenProbeSummaryAggregator.kt`
   - `OrganicHiddenProbeArtifactWriter.kt`
   - `OrganicHiddenProbeModels.kt`
3. `tools/src/test/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunnerTest.kt`
4. 新增：
   - `OrganicHiddenProbeBotPolicyTest.kt`
   - `OrganicHiddenProbeSummaryAggregatorTest.kt`
   - `OrganicHiddenProbeArtifactWriterTest.kt`

### 8.3 任务拆解

#### Task 1：模型与职责切分

1. 把 case result / summary / notes / payload models 抽到独立 model 文件。
2. 把 bot state 和 `shouldSearch()` 从 runner 中拆出；现有 `OrganicHiddenProbeBot` 可以改名为 `OrganicHiddenProbeBotPolicy`，或作为委托层保留，但 `PR-02` 必须显式选定其一。
3. 把 summary 聚合逻辑收成纯函数。
4. 把 markdown / json / events materialization 收成单独 writer。
5. 明确 `VerificationCacheSupport`、cache key、`MISS -> HIT` 语义继续由 runner 独占。

#### Task 2：严格 timing 语义冻结

1. `firstHiddenDiscoveryTurn` 固定为：
   - `revealedBindingIds.isNotEmpty()`
   - 或 `secretZoneIds.isNotEmpty()`
2. 若仍需更早的 primer/signal 证据，新增 `firstSignalTurn`，但它只允许进入 `supportingEvidence.*`，默认不进 owner headline 与 phase aggregate。

#### Task 3：focused tests

必须新增三类测试：

1. bot policy tests
   - prompt 可见时优先 `Search`
   - rejection/cooldown 仍生效
   - `canDescend / visibleHostiles / visibleBoss` 不再错误压制 search
2. summary aggregator tests
   - primer-only 不算 `leadDiscovery`
   - `failingSecretEntryZoneIds` 计算正确
   - `firstHiddenDiscoveryTurn` 不被 primer-only 提前污染
3. artifact writer smoke
   - 缺字段 fail fast
   - report/json 中关键 field name 稳定

### 8.4 退出条件

以下全部满足才能进入 `PR-03`：

1. `OrganicHiddenProbeSummaryAggregatorTest` 能在不跑 full probe 的情况下直接验证 `leadDiscoveryRate`、`secretConversionRate`、`failingSecretEntryZoneIds`、`firstHiddenDiscoveryTurn`。
2. `OrganicHiddenProbeArtifactWriterTest` 能在 fixture summary 上直接生成 owner artifact。
3. `organicHiddenProbe` 真实整跑仍与 current canonical contract 一致。
4. `OrganicHiddenProbeRunnerTest` 的全部既有 integration assertions 继续为绿；不接受为让 focused unit test 通过而选择性弱化 integration golden。

### 8.5 推荐验证

```bash
./gradlew :tools:test --tests 'com.ktome.tools.hidden.OrganicHiddenProbeBotPolicyTest'
./gradlew :tools:test --tests 'com.ktome.tools.hidden.OrganicHiddenProbeSummaryAggregatorTest'
./gradlew :tools:test --tests 'com.ktome.tools.hidden.OrganicHiddenProbeArtifactWriterTest'
./gradlew :tools:test --tests 'com.ktome.tools.hidden.OrganicHiddenProbeRunnerTest'
./gradlew organicHiddenProbe
./gradlew phase4Report
```

---

## 9. PR-03：WhiteBoxSolvability Success/Fail 双 Lane

### 9.1 目标

把 reveal-success 与 reveal-fail 从同一 corpus 中拆开，解决：

1. success corpus 既要求 `criticalPathFailureCount == 0`，又想要求“至少有 fail case”，语义自相矛盾；
2. 删除 old aggregate 之后留下 `casesWithFail` 这种“有人统计、没人签收”的残留指标；
3. fail diagnostics 无法稳定保留专用 artifact 与 aggregate contract；
4. fail seed / fixture 来源没有正式冻结，容易把 lane 设计写成不可落地的硬要求。

### 9.2 范围

涉及文件建议：

1. `tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxSolvabilityRunner.kt`
2. 新增：
   - `WhiteBoxSolvabilityLaneSpec.kt`
   - `WhiteBoxSolvabilitySuccessLane.kt`
   - `WhiteBoxSolvabilityFailLane.kt`
3. `tools/src/test/kotlin/com/ktome/tools/mapgen/*`
4. `tools/src/main/kotlin/com/ktome/tools/phase4/*` 内与 solvability artifact inventory / report projection 相关的消费端

### 9.3 任务拆解

#### Task 1：lane spec 抽象

把当前 runner 拆成：

1. case builder
2. lane spec
3. lane-specific aggregate rules
4. lane-aware writer path

默认实现策略：

1. 优先保持单个 `whiteBoxSolvability` task，不先把 lane split 扩成两个 Gradle producer。
2. lane 先体现在 task 内部 artifact 目录、summary 结构与 aggregate assertion。
3. 只有当 fail lane 的运行成本或 freshness 需求与 success lane 明显不同，才再评估是否拆成独立 task，并在同一 PR 同步 `aggregation-manifest` / registry / build-logic。

#### Task 2：reveal-success lane

固定 contract：

1. `criticalPathFailureCount == 0`
2. `casesWithReveal > 0`
3. `casesWithBacktrackProof > 0`
4. `hiddenAnchorFamilyFailureCount == 0`

#### Task 3：reveal-fail lane seed 来源冻结

固定 contract：

1. 由显式 fail fixtures / fail seeds 组成
2. lane 内全部 fixture 都必须保持 `casesWithFail == sampleCount`
3. fail taxonomy / proof trace 可解释
4. 不再要求 reveal-success coverage
5. `greenwood_fringe` 若继续在无 primer 的正式 search 语义下稳定 reveal，则留在 success lane，不强行塞进 fail fixture corpus

正式选型必须二选一并写入实现文档：

1. 在 `solvability harness` 层新增 fail-fixture 注入点；
2. 先扫描主 deterministic corpus，提取稳定 `casesWithFail > 0` 的 seed snapshot，再固化为 fail lane corpus。

选型判据：

1. 若主 deterministic corpus 在本方案期间仍可能调整，优先选 fail-fixture 注入点，避免 seed snapshot 随主 corpus 漂移。
2. 只有当主 corpus 已冻结，且 `PR-03` 不计划再改 seed generator / case builder 时，才允许采用 seed snapshot 路径。

如果选型在当前 PR 内无法一次性冻结，则本 PR 必须拆成：

1. lane 结构搬迁
2. fail seed / fixture 固化

#### Task 4：artifact / report 切换

1. success / fail lane 分目录落盘
2. `phase4Report` 消费 lane-aware summary
3. `PR-03` 内只新增 lane-aware 说明段，不删除旧 single-corpus wording；所有 legacy wording 删除统一留到 `PR-04`

### 9.4 退出条件

以下全部满足才能进入 `PR-04`：

1. success lane 与 fail lane 各有独立 aggregate assertion。
2. success lane 不再保留 reveal-fail 的伪 requirement。
3. fail lane 有稳定的 deterministic corpus，不依赖“主 corpus 自然碰巧出现 fail case”。
4. fail lane seed / fixture 来源已冻结，并写进实现说明或对应 test fixture 说明；当前冻结为 `abyssal_temple / deep_iron_pit / underground_river` 三个 fail-capable zone 的 `2 floor × 1 seed = 6` fixtures。

### 9.5 推荐验证

```bash
./gradlew :tools:test --tests 'com.ktome.tools.mapgen.WhiteBoxSolvability*'
./gradlew whiteBoxSolvability
./gradlew phase4Report
```

---

## 10. PR-04：Canonical Report / Docs / Compat Cleanup

### 10.1 目标

把前三个 PR 建立的新结构真正切到 canonical gate，解决：

1. report 仍消费旧 artifact path / 旧 wording；
2. checklist / docs / review 说明仍保留旧单 corpus / 旧 authority 描述；
3. compat alias 长期不删，未来继续制造双语义。

### 10.2 范围

涉及文件建议：

1. `tools/src/main/kotlin/com/ktome/tools/phase4/*`
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
3. 必要时：
   - `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-03-organic-hidden-loop-secret-reward-identity-and-replay-hook.md`
   - 本文档与对应 review note

### 10.3 任务拆解

#### Task 1：report cutover

1. 明确 lane split 之后 `whiteBoxSolvability` 仍保持单 task 还是拆成双 producer task。
2. 若保持单 task，则同步更新：
   - `Phase4DomainArtifactRegistry`
   - `Phase4ReportRunner / ReportPhase4Runner`
   - `Phase4MetricCatalog` 的 formula / failSemantics wording
   - 对应 report consumer tests
3. 若拆成双 task，则同一 PR 内同步更新：
   - `tools/src/main/resources/phase4/aggregation-manifest.yaml`
   - `build-logic` wiring
   - `VerificationTaskRegistry`
   - `Phase4DomainArtifactRegistry`
   - `Phase4MetricCatalog`
4. `phase4Report` 只消费新的 lane-aware solvability artifact。
5. `phase4Report` 只消费新的 organic hidden summary / payload contract。
6. secret reward authority debug evidence 只消费 shared authority helper 的产物。
7. 明确决策 canonical summary schema 是继续兼容 `report-phase4-v2` 还是 bump 到新 schema；不得隐式漂移。
9. 当前落地决策：
   - `whiteBoxSolvability` 保持单个 producer task，root summary 与 report 改为消费 lane-aware artifact
   - canonical summary schema 继续保持 `report-phase4-v2`；本 PR 只做 consumer wording / markdown / checklist cutover，不做 schema bump
10. 若 schema / field set / baseline input 发生变化，同批次同步更新：
   - `Phase4AggregationManifest*`
   - `Phase4RegistryConsistencyTest`
   - `ReportPhase4BuildContractTest`
   - `Phase4MetricCatalog`
   - 必要的 baseline / materialization contract tests
11. 若 baseline 需要重刷，必须同批次提交到 `docs/review/phase4/opt/baselines/` 对应文件，不允许跨 PR 残留“代码新结构、baseline 旧结构”的过渡态。

#### Task 2：compat cleanup

1. 删除旧 metric alias / 旧 wording
2. 删除旧 single-corpus solvability wording
3. 删除旧 secret reward fallback 文案
4. 若 schema bump，显式更新 canonical report schema 文档与测试，不接受 silent additive drift。

#### Task 3：文档同步

1. checklist
2. `docs/opt` 方案
3. 必要的 review PR note / implementation note
4. 若 lane split 仍保持单 `whiteBoxSolvability` task，必须在文档里写清楚“lane-aware artifact，不等于双 producer task”
5. checklist 至少要明确改到：
   - `whiteBoxSolvability` 条目中 reveal-success / reveal-fail 共用 corpus 的 wording
   - `organicHiddenProbe` 条目中 headline owner metric 与 supporting evidence 的分层说明

### 10.4 退出条件

整组方案完成定义：

1. `organicHiddenProbe`、`whiteBoxSolvability`、secret reward authority 都有单独可测层。
2. canonical `phase4Report` 只消费新结构，不再带旧兼容解释。
3. docs / checklist / report wording 全部与新结构一致。
4. build-logic / manifest / registry / metricCatalog / contract tests 与最终 cutover 结构一致，不存在“代码能跑，但 authoritative inventory 仍锁旧结构”的状态。

### 10.5 推荐验证

```bash
./gradlew maintainabilityLint
./gradlew verifyOwner
./gradlew phase4Report
./gradlew organicHiddenProbe whiteBoxSolvability whiteBoxLoot contentPackHarness phase4Report
```

---

## 11. 全局验证纪律

### 11.1 环境前置

每次执行 Gradle 验证前先运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

### 11.2 固定验证阶梯

按以下顺序执行，禁止倒序：

1. focused unit / contract test
2. lane / helper / writer 级组件测试
3. domain owner task
4. `verifyOwner`
5. `phase4Report`

### 11.3 这组 PR 的默认联合验收

```bash
./gradlew maintainabilityLint organicHiddenProbe whiteBoxSolvability whiteBoxLoot contentPackHarness verifyOwner phase4Report
```

补充规则：

1. producer artifact 有 freshness 绑定的任务必须同批次重刷。
2. `phase4Report` 的成功不能代替 focused unit / lane / helper test。
3. 不允许在 `PR-01 ~ PR-03` 阶段提前删 compat path；compat 删除统一放到 `PR-04`。
4. 若 `PR-03` 最终采用双 task 方案，本节联合验收命令必须同批次更新。

---

## 12. 风险与对策

### 12.1 `organicHiddenProbe` 拆层后字段漂移

风险：

1. writer 与 summary model 字段名不一致
2. `phase4Report` 仍读旧 key

对策：

1. `ArtifactWriterTest` 直接锁字段集
2. `phase4Report` 增 focused consumer test
3. `OrganicHiddenProbeRunnerTest` 的全部既有 integration assertions 继续为绿，不接受只剩 focused test 的“半收口”

### 12.2 `organicHiddenProbe` 纯函数化后 cache owner 漂移

风险：

1. cache key / cacheStatus 被拆到 policy 或 writer
2. `VerificationCacheSupport` 语义与实际 runner 生命周期脱钩

对策：

1. 明确 `Runner` 独占 cache 目录与 kernel cache owner 身份
2. policy / aggregator / writer 全部维持无 cache 绑定
3. `OrganicHiddenProbeRunnerTest` 继续锁 `MISS -> HIT`
4. 若实现层允许，增加简单 import-check 或 maintainability 规则，阻止 `Policy / Aggregator / Writer` 直接依赖 `VerificationCacheSupport`

### 12.3 success/fail lane 切开后 report source 不清晰

风险：

1. `phase4Report` 不知道该读哪条 lane
2. fail lane 变成新的无人消费 artifact

对策：

1. 在 `PR-03` 就同步加入 lane-aware consumer routing
2. `PR-04` 才删旧 single-corpus wording

### 12.4 fail lane seed / fixture 来源不稳定

风险：

1. 主 deterministic corpus 今天有 fail case，明天没有
2. fail lane 无法稳定复现，最终只能退回“统计有人写、没人签收”

对策：

1. `PR-03` 内必须先冻结 seed/fixture 来源，再写 aggregate hard assertion
2. 若无法一次冻结，则先完成 lane 结构搬迁，再单独补 fail fixture 固化
3. 文档与 test fixture 都必须写明 fail lane seed 所有权；如需跨 task 复用，必须显式记录而不是顺手引用

### 12.5 shared authority helper 反向变成 `tools` owner

风险：

1. helper 如果放在 `tools`，runtime/validator 会倒依赖错误 owner

对策：

1. authority helper 必须放在 `game`
2. `tools` 只能消费，不得重新实现

### 12.6 authority helper 误删合法的非 secret-zone `LOOT_PROFILE`

风险：

1. helper 把“secret-zone reward 单一真源”错误扩大成“所有 hidden event 都不许有 `LOOT_PROFILE`”
2. 非 secret-zone hidden reward 合法路径被误杀

对策：

1. runtime resolver 明确只治理 secret-zone reward authority
2. contract test 覆盖“非 secret-zone hidden event 上 `LOOT_PROFILE` 仍合法”
3. validator / scanner 的 violation reason 必须显式带 secret-zone 上下文

### 12.7 canonical cutover 时 schema / baseline / registry 不一致

风险：

1. report schema 悄悄漂移但 tests 还锁旧版本
2. `aggregation-manifest`、registry、build-logic、baseline inputs 没同批次更新
3. canonical report 能生成，但 authoritative inventory 仍停在旧结构

对策：

1. `PR-04` 必须显式决定 schema 是否 bump
2. 若 field set / schema / producer inventory 有变化，同批次刷新：
   - §10.3 Task 1 列出的 contract tests、registry、metric catalog 与 baseline 文件
3. 不接受“代码能跑但 contract tests 还锁旧结构”的合并状态

---

## 13. 完成后才能进入下一组工作

以下全部满足，才算这组长期治理方案完成：

1. `organicHiddenProbe` 的 bot policy、summary aggregator、artifact writer 都有独立测试面。
2. `whiteBoxSolvability` 的 reveal-success / reveal-fail 正式分 lane，且各自 aggregate contract 不再互相冲突。
3. secret reward authority 只有一个 shared helper / shared assertion builder。
4. canonical `phase4Report`、checklist、相关 docs 不再残留旧 wording / 旧 compat 语义。

如果只完成 runtime patch 或只完成 canonical 报告收口，而没完成上述 layering / lane / authority 结构冻结，视为“症状被修过，但长期问题未解决”。

本方案完成后，`Phase 4` 在 hidden/solvability/authority 这三条长期治理线上的 layering/lane/authority 收口视为完成；后续工作重新回到 [docs/phase4/roadmap.md](/Users/luo/Documents/github/K-ToME/docs/phase4/roadmap.md) 指向的主线推进与下一阶段衔接。
