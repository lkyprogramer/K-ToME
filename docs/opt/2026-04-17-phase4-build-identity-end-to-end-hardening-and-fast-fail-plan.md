> 执行前必须先完整阅读并接受：
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-02-build-identity-dynamic-loot-and-profession-capstones.md`
> 最新 `tools/build/reports/verification/phase4/report-phase4-summary.{md,json}`
> 最新 `build/reports/harness/long-run-full.json`

# Phase 4 Build Identity End-to-End Hardening 与 Fast-Fail 方案

**阶段**: `Phase 4 / Post-PR-02 Hardening / P4-B follow-up`  
**优先级**: `P0`  
**工作量评估**: `L`（`6~10` 人日）  
**前置条件**: `PR-02` 第一轮 closure 已完成，最新 canonical `phase4Report` 为 `PASS`  
**对应问题**:

1. 这轮问题不是单点 bug，而是 `owner gate -> reward source wiring -> milestone candidate legality / weighting -> harness adoption -> data passive uniqueness` 的整链漂移。
2. 当前 canonical 已通过，但问题依然只能靠长链 artifact 反推，缺少更早的 fast-fail 节点。
3. profession capstone、non-weapon payoff、special-tier identity 这三件事的 authority 仍然分散在：
   - `game` runtime wiring
   - `game` reward selector
   - `game` harness adoption heuristic
   - `tools` owner metric / baseline / report
4. 只要这几层继续各自维护一份“差不多一致”的语义，下一次同类问题仍会花数小时定位。

---

## 0. 当前状态快照

截至 `2026-04-17` 的最新 canonical artifact：

1. `tools/build/reports/verification/phase4/report-phase4-summary.json`
   - `phaseVerdict = PASS`
   - `unexpectedRegressionCount = 0`
   - `specialTierPassiveFamilyDuplicateCount = 0`
   - `professionCapstoneSeenRate = 83.3%`
   - `professionCapstoneAdoptionRate = 50.0%`
   - `nonWeaponBuildPayoffRate = 50.0%`
2. `build/reports/harness/long-run-full.json`
   - per-profession seen floor 已经成立
   - 但 breakdown 仍呈现明显不均衡：
     - `arcanist`: `seen=3`, `adopted=0`, `nonWeapon=0`
     - `rogue`: `seen=1`, `adopted=0`, `nonWeapon=0`
     - `templar`: `seen=3`, `adopted=3`, `nonWeapon=3`
     - `vanguard`: `seen=3`, `adopted=3`, `nonWeapon=3`

这说明：

1. 当前 PR-02 文档要求已经达成。
2. 但“为什么会花几个小时修完”这个问题并没有被 architecture 层真正解决。
3. 现在最缺的不是再调一轮数字，而是把这条链的 authority 和 fast-fail 点补齐。

---

## 1. 阶段目标

本方案不是再做一轮“调权重 + 调数据”的 patch，而是把 Phase 4 build identity 的整条链收成可维护、可 fast-fail、可解释的正式 contract。

完成标准：

1. support / cache / finale 的 reward source routing 不再藏在 `FoundationGameSession` 的硬编码 `when` 分支中。
2. profession build identity 有单一 authority，runtime、harness、tools 使用同一份定义。
3. milestone reward 的 standard / special-linked candidate 走同一套 legality、weighting 和 replacement 规则。
4. special-tier duplicate、reward source drift、capstone path drift 能在 `whiteBox / preflight / unit` 层提前失败，而不是等 `longRunLab + phase4Report` 才看见。
5. owner gate 的“当前 blocking 语义”和“下一阶段计划升级的更强语义”分层明确，不再混在 note 和 baseline 文本里。

---

## 2. 为什么必须作为一组 PR 处理

这次修复慢，不是因为实现复杂，而是因为问题分布在不同层，且每层都能把上一层的问题掩盖掉。

### 2.1 第一层：gate 失真会制造假绿

如果 `tools` 只看 aggregate seen rate，或者 baseline 接受 `nonWeaponBuildPayoffRate >= 0.0`，那么：

1. 某个 profession 完全看不到 capstone 也能通过。
2. 非武器 capstone 完全没有 build payoff 也能通过。
3. `canonical report` 失去 owner 语义。

### 2.2 第二层：reward routing 漂移会把正确数据接回错误池

即使 `loot/index.yaml` 已经给 `underground_river.reward / abyssal_temple.reward / abyssal_heart.reward` 配好了 profession capstone，  
只要 `support/cache/finale` 还在 runtime 里指向 `loot.foundation.*` 或错误 zone reward，玩家看到的就仍然是旧池。

### 2.3 第三层：candidate legality / weighting 分叉会让 capstone“候选可见但排序无效”

这次已经证明确实存在两类问题：

1. explicit special-linked item 进入 candidate list，但没有参与同一套权重累计。
2. special-linked milestone base 仍被普通 `dropFloors` legality 误删。

只修其中一半，会得到“候选里有，但永远选不到”的假象。

### 2.4 第四层：adoption heuristic 如果不是 shared contract，就会让 owner metric 与 runtime 语义脱钩

当前 `SmokeBot` 的 adopt 行为决定了 `longRunLab` 的 final build evidence。  
如果 harness adoption、profession capstone ID 集、tools owner metric 不是同一 authority，就会出现：

1. runtime 看起来给到了职业锚点；
2. harness 不会换装；
3. report 里仍然表现为 adoption = 0；
4. 最后只能人工 spelunk artifact 才知道到底是 reward 没给到，还是 bot 没穿上。

### 2.5 第五层：special-tier data uniqueness 不能只靠最终 report

同区 passive family duplicate 如果只有 `whiteBoxLoot -> phase4Report` 才失败，就意味着每次数据变更都要付出一次重 harness 成本。  
这类问题本应在 data preflight / lint 层就被截住。

---

## 3. 必须冻结的合同

1. 不新增新的 `core` 枚举、战斗语义、资源轴或 content-pack 权限。
2. 不引入第二份 profession build identity authority。
3. 不允许 `tools` 为 capstone / non-weapon payoff 再维护一套独立 item ID 列表。
4. 不允许 `FoundationGameSession` 继续扩张新的 reward-routing `when` 分支。
5. 不允许 `phase4Report` 之外再出现新的 canonical owner aggregate。
6. 不在同一 PR 里同时修改：
   - authority schema
   - baseline gate
   - canonical baseline snapshot
   除非该 PR 明确以“gate cutover”作为主题。
7. 所有 Gradle 命令必须串行执行。

---

## 4. 范围与非目标

### 4.1 范围

1. reward source routing contract
2. profession build identity shared catalog
3. milestone reward selector extraction
4. harness adoption contract hardening
5. special-tier passive-family fast-fail preflight
6. owner metric 升级路径与 report diagnostics

### 4.2 非目标

1. 不重写整套 loot system。
2. 不引入 crafting、forging、equipment evolution 新系统。
3. 不重开 `Phase 4` 的 content 范围。
4. 不在这组 PR 里把所有 profession adoption floor 直接提升成 blocking gate。
5. 不把 `client` 做成新的 authority；capstone cue 仍然只消费 runtime semantic event。

---

## 5. 设计总览

### 5.1 单一 authority 拆分

彻底方案固定拆成四个 authority：

1. `reward routing authority`
   - 决定某个 reward source 应该接到哪些 loot profile
2. `build identity authority`
   - 决定每个 profession 的 capstone / non-weapon anchor / 期望路径
3. `milestone selector authority`
   - 决定 candidate legality、weighting、replacement、score breakdown
4. `owner metric authority`
   - 决定哪些指标是 report-only，哪些是 blocking，哪些按 per-profession floor 执行

### 5.2 目标架构

```text
data/reward-routing
  -> FoundationGameSession reward source resolution
     -> MilestoneRewardSelector
        -> runtime reward grant / direct-drop cue

data/build-identity
  -> LootBaseSelectionContext / selector weighting
  -> SmokeBot adoption policy
  -> LongRunLab professionCapstoneSummary
  -> Phase4AggregationInputRunner / report rendering

whiteBox/preflight
  -> reward routing coverage
  -> passive-family duplicate coverage
  -> selector score breakdown

phase4Report
  -> canonical owner gate only
```

### 5.3 建议新增的 authority 文件

```text
game/src/main/resources/data/reward-routing/index.yaml
game/src/main/resources/data/build-identity/index.yaml
game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardSelector.kt
game/src/main/kotlin/com/ktome/game/harness/BuildIdentityAdoptionPolicy.kt
tools/src/main/kotlin/com/ktome/tools/loot/LootIdentityDerivations.kt
```

补充说明：

1. `route` 与 `boss` 奖励 authority 继续保留在 `world/world_graph.yaml` 与 `bosses/index.yaml`。
2. 新 `reward-routing/index.yaml` 只承载 `support/cache/finale-interactable` 这类当前仍在 runtime 里硬编码的接线。
3. `build-identity/index.yaml` 不承载数值平衡，只承载 authority 级的 identity facts。

---

## 6. PR 拆分总览

| PR | 主题 | 主要模块 | 主要产出 | 进入下一 PR 的门槛 |
| --- | --- | --- | --- | --- |
| `PR-01` | reward routing contract freeze | `game:data`, `game:runtime`, `game:test` | `support/cache/finale` 奖励接线数据化，移除 runtime 硬编码 routing | 关键 interactable reward contract test 全绿，runtime 不再依赖旧 `when` |
| `PR-02` | shared build identity catalog | `game:data`, `game:harness`, `tools`, `test` | per-profession capstone / non-weapon / anchor source 单一 authority | 不再存在散落的 capstone ID hardcode，schema/fixture/test 全绿 |
| `PR-03` | milestone selector extraction and score breakdown | `game:loot`, `game:runtime`, `test` | standard/special unified selector、rejection reason、score breakdown | selector 级 unit test 能直接复现并解释 river / temple / heart cases |
| `PR-04` | fast-fail diagnostics and staged gate hardening | `tools`, `whiteBox`, `game:harness`, docs | duplicate preflight、routing coverage、per-profession staged metrics、diagnostic artifact | 错 routing / duplicate / identity drift 能在 long-run 之前失败，canonical report 语义清晰 |

固定规则：

1. 当前 PR 未完成前，不并行推进下一个 PR。
2. 当前 PR 的文档、测试、验证、artifact 证据不完整，不允许进入下一 PR。
3. 这组 PR 的目标是“减少下次定位成本”，不是“再做一轮一把梭 balance”。

---

## 7. 全局开发与验证纪律

### 7.1 环境前置

每次验证前都先执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

### 7.2 验证阶梯

这组 PR 必须固定遵守以下红绿顺序：

1. schema / unit / contract
2. reward selector component test
3. `whiteBoxLoot` / preflight
4. `longRunLab`
5. `verifyOwner`
6. `phase4Report`

禁止事项：

1. 不允许一开始就只跑 `longRunLab + phase4Report`。
2. 不允许把 `phase4Report PASS` 当作足够证据，必须同时看对应 producer artifact。

### 7.3 统一通过标准

任一 PR 结束时必须同时满足：

1. 新增 authority 只有一份真源。
2. 有对应自动化测试锁定 authority 与 runtime 一致。
3. 对应 drift 能在比 `longRunLab` 更早的层失败。
4. 文档中声明的必跑命令全部通过。

---

## 8. PR-01：Reward Routing Contract Freeze

### 8.1 目标

把 `support/cache/finale-interactable` 的 reward source routing 从 `FoundationGameSession` 的硬编码 `when` 分支中抽离出来，冻结成数据 authority。

### 8.2 为什么这是第一优先

如果 routing 还是 runtime 硬编码，那么：

1. 数据层已经把 `loot.underground_river.reward` 配好也没用；
2. 文档、数据、owner gate 三者可以看起来都正确，但运行时仍接回旧池；
3. 所有后续 selector / adoption / gate 优化都建立在错误输入上。

### 8.3 非目标

1. 不在本 PR 调整 selector 权重。
2. 不在本 PR 改 baseline。
3. 不在本 PR 调整 harness adoption。

### 8.4 技术方案

#### 8.4.1 新增 reward routing data authority

建议新增：

```text
game/src/main/resources/data/reward-routing/index.yaml
```

每条 routing entry 固定至少包含：

1. `zoneId`
2. `interactableId`
3. `grantMode`
   - `GROUND_CACHE`
   - `SUPPORT_GRANT`
4. `profileIds`
5. `fallbackBaseId`

补充规则：

1. `route` 奖励 authority 仍来自 `world_graph.yaml`。
2. `boss` 奖励 authority 仍来自 `bosses/index.yaml`。
3. `reward-routing/index.yaml` 只负责当前 runtime 中剩余的 interactable routing。

#### 8.4.2 runtime 只做 lookup，不再做业务决策

`FoundationGameSession` 里的：

1. `groundRewardSpecFor(...)`
2. `supportRewardSpecFor(...)`

改成：

1. 先读 routing registry
2. 找不到时 fail fast
3. 不允许继续在 runtime 里追加新的 zone-specific `when`

#### 8.4.3 capstone cue parity 在 routing contract 层一起锁住

direct drop / route / cache / support / boss / hidden reward 的 capstone cue，都必须共用同一套：

1. `isCapstone`
2. `isNonWeaponCapstone`
3. `profession anchor message key`

不允许不同 reward source 各自决定是否出 cue。

### 8.5 代码范围

- `game/src/main/resources/data/reward-routing/index.yaml`
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- 新增 contract test：
  - `game/src/test/kotlin/com/ktome/game/data/RewardRoutingContractTest.kt`

### 8.6 必须实现的行为

1. `river_ferry_anchor / crystal_cache_chest / temple_ward_reliquary / heart_ward_focus` 都从 data routing 读取 profile。
2. routing entry 缺失时，loader 或 contract test 必须直接失败。
3. runtime 不再保留与这些 interactable 绑定的 reward profile 常量分支。

### 8.7 必须新增的测试

1. routing schema loader test
2. 每个关键 interactable 的 runtime integration test
3. capstone cue parity test
4. reward source id -> routing entry 的 contract test

### 8.8 必跑命令

```bash
./gradlew :game:test --tests 'com.ktome.game.data.RewardRoutingContractTest' --tests 'com.ktome.game.FoundationGameSessionTest'
./gradlew verifyChanged
```

### 8.9 退出标准

1. `FoundationGameSession` 中不再存在 zone-specific reward routing `when`。
2. 所有关键 interactable reward contract 都由数据 authority 驱动。
3. direct-drop capstone cue parity regression 有自动化锁定。

---

## 9. PR-02：Shared Build Identity Catalog

### 9.1 目标

把 profession capstone / non-weapon anchor / preferred source 这类 identity facts 从散落 hardcode 收成一份 shared catalog。

### 9.2 为什么这是第二优先

当前同一组事实至少散落在：

1. `SmokeBot`
2. `LongRunLabFullTest`
3. `tools` owner metric / report
4. 各类 fixture / baseline / doc test

这会导致：

1. runtime 修好了，harness 还不知道；
2. harness 会穿上，tools 还不会统计；
3. 文档和实现都更新了，测试还锁着旧 item ID。

### 9.3 非目标

1. 不在本 PR 提升 blocking gate。
2. 不在本 PR 调整 selector 算法。
3. 不在本 PR 改 special-tier data。

### 9.4 技术方案

建议新增：

```text
game/src/main/resources/data/build-identity/index.yaml
```

每个 profession 至少包含：

1. `professionId`
2. `capstoneBaseIds`
3. `nonWeaponCapstoneBaseIds`
4. `preferredRewardSources`
5. `preferredReplacementSlots`
6. `terminalIdentityTags`
7. `reportOnlyFloors`
   - `seenMinCount`
   - `adoptionMinCount`
   - `nonWeaponMinCount`

补充规则：

1. 当前 blocking gate 只消费已经正式冻结的 floor。
2. 更强的 `adoption / nonWeapon` per-profession floor 先放 `reportOnlyFloors`，不在本 PR 直接切 gate。

### 9.5 共享消费点

必须统一切到 catalog 的位置：

1. `LootBaseSelectionContext`
2. `SmokeBot`
3. `LongRunLabFullTest.professionCapstoneSummary(...)`
4. `Phase4AggregationInputRunner`
5. `Phase4ReportRunner`
6. doc consistency / authority tests

### 9.6 代码范围

- `game/src/main/resources/data/build-identity/index.yaml`
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt`
- `game/src/main/kotlin/com/ktome/game/harness/SmokeBot.kt`
- `game/src/test/kotlin/com/ktome/game/data/SchemaV2LoaderTest.kt`
- `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt`
- `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4AuthorityDocConsistencyTest.kt`

### 9.7 必须实现的行为

1. 不再存在第二份 `PROFESSION_CAPSTONE_ITEM_IDS` 或等价 hardcode。
2. SmokeBot 的 capstone equip preference 从 catalog 推导，不再手写 item ID 表。
3. tools report 与 harness summary 对 profession capstone 的定义一致。

### 9.8 必须新增的测试

1. 每个 profession 至少 2 个 capstone、至少 1 个 non-weapon 的 schema test
2. catalog item ID 必须存在且 tags 匹配 profession 的 test
3. `SmokeBot` / `LongRunLab` / `tools` 的 authority consistency test

### 9.9 必跑命令

```bash
./gradlew :game:test --tests 'com.ktome.game.data.SchemaV2LoaderTest' --tests 'com.ktome.game.harness.LongRunLabFullTest'
./gradlew :tools:test --tests 'com.ktome.tools.phase4.Phase4AuthorityDocConsistencyTest'
./gradlew verifyChanged
```

### 9.10 退出标准

1. profession build identity facts 只有一份 authority。
2. harness / tools / tests 不再各自复制 capstone ID 集。
3. report-only stronger floors 已可从 catalog 渲染出来，但尚未 blocking cutover。

---

## 10. PR-03：Milestone Selector Extraction and Score Breakdown

### 10.1 目标

把 milestone reward 的 legality、weighting、replacement、rejection reason 从 `FoundationGameSession` 大函数里抽出来，做成可单测、可解释的 selector component。

### 10.2 为什么这是第三优先

这次最贵的 runtime bug 都在这里：

1. `allCandidateBaseIds` 和 `standardCandidateBaseIds` 分叉
2. special-linked item 进候选但不进权重
3. special-linked item 被普通 `dropFloors` 误删
4. replacement slot 只按旧 priority，不看 exact-profession capstone

这些逻辑继续留在 session 里，下次仍然只能靠 long-run artifact 反推。

### 10.3 非目标

1. 不在本 PR 做新的 balance 目标。
2. 不在本 PR 改 owner baseline。

### 10.4 技术方案

建议新增：

```text
game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardSelector.kt
game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardCandidate.kt
game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardScoreBreakdown.kt
game/src/main/kotlin/com/ktome/game/loot/MilestoneRewardRejectionReason.kt
```

selector 输出必须同时提供：

1. `selectedBaseId`
2. `replacementSlot`
3. `rankedCandidates`
4. 每个 candidate 的：
   - legality
   - rejection reason
   - score breakdown

### 10.5 legality 规则必须统一

standard 与 special-linked candidate 必须走同一条 selector，但 legality 规则明确区分：

1. standard base
   - obey `dropFloors`
   - obey profession suitability
   - obey material / affix feasibility
2. special-linked base
   - 不再被普通 `dropFloors` 误删
   - 仍必须 obey source tier / zone / material / affix feasibility

### 10.6 replacement 规则必须统一

replacement 规则固定为：

1. 若存在 exact-profession capstone candidate，则在这一组里按：
   - `nonWeaponCapstone`
   - `score`
   - `configuredReplacementPriority`
   排序
2. 若不存在 exact-profession capstone，则回退到配置好的 slot priority

### 10.7 diagnostics

selector 必须提供轻量 diagnostic 输出，供：

1. unit test
2. white-box reward coverage
3. phase4 report debug note

使用，但不作为新的 canonical summary。

### 10.8 代码范围

- `game/src/main/kotlin/com/ktome/game/loot/*`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/test/kotlin/com/ktome/game/loot/MilestoneRewardSelectorTest.kt`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`

### 10.9 必须新增的测试

1. special-linked base legality test
2. standard/special unified weighting test
3. non-weapon capstone replacement preference test
4. river / temple / heart 关键 case selector fixture test
5. generic aligned weapon 不应压过 exact-profession capstone 的 test

### 10.10 必跑命令

```bash
./gradlew :game:test --tests 'com.ktome.game.loot.MilestoneRewardSelectorTest' --tests 'com.ktome.game.loot.LootProfileCandidatePoolResolverTest' --tests 'com.ktome.game.FoundationGameSessionTest'
./gradlew verifyChanged
```

### 10.11 退出标准

1. milestone selector 的核心逻辑不再散落在 `FoundationGameSession` 内部多个 helper。
2. river / temple / heart 关键 reward case 可用 selector unit test 直接解释。
3. selector diagnostics 能指出是 legality 问题还是 weighting 问题。

---

## 11. PR-04：Fast-Fail Diagnostics and Staged Gate Hardening

### 11.1 目标

把仍然依赖 `longRunLab + phase4Report` 才能发现的问题前移到 `whiteBox / preflight / report-only metrics`，并把更强 gate 的升级路径写清楚。

### 11.2 为什么这是最后一优先

前面三个 PR 不先做，这个 PR 只会把现状的偶然行为门禁化。  
只有 authority 收干净后，fast-fail 和 stronger gate 才有意义。

### 11.3 非目标

1. 不在本 PR 一次性提高所有 blocking floor。
2. 不重写 `verifyOwner` 路由。

### 11.4 技术方案

#### 11.4.1 special-tier duplicate 前移

当前 `specialTierPassiveFamilyDuplicateCount` 已经是 owner metric，  
下一步要把同区 duplicate 从“最终 report 才红”前移到：

1. `verifyLootPreflight`
2. `whiteBoxLoot`

至少有一层直接指出：

1. `zoneId`
2. `passiveFamily`
3. `templateIds`
4. `itemIds`

#### 11.4.2 reward routing coverage 前移

新增 white-box / contract evidence：

1. 每个关键 interactable 是否接到了期望的 profile
2. 每个 profession 的主路径上是否存在至少一个 expected capstone source
3. 缺失时直接在 report note 中列出 culprit source id

#### 11.4.3 staged gate hardening

gate 分两步：

1. **本 PR 落地 report-only stronger metrics**
   - per-profession adoption floor
   - per-profession non-weapon floor
   - capstone source coverage floor
2. **后续单独 gate-cutover PR 再提升为 blocking**
   - baseline refresh
   - canonical artifact 更新
   - docs/checklist 同步

#### 11.4.4 diagnostic artifact

建议新增 report-only additive artifact：

```text
tools/build/reports/verification/phase4/build-identity-debug.json
```

至少包含：

1. reward source -> selected base id
2. top rejected capstone candidates
3. rejection reasons
4. per-profession source coverage

它不是新的 canonical summary，只是为了避免下次继续人工 grep 大 JSON。

### 11.5 代码范围

- `tools/src/main/kotlin/com/ktome/tools/loot/LootIdentityDerivations.kt`
- `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`
- `tools/src/test/kotlin/com/ktome/tools/loot/WhiteBoxLootRunnerTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunnerTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4ReportRunnerTest.kt`
- `docs/review/phase4/opt/baselines/*`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`

### 11.6 必须实现的行为

1. wrong routing / duplicate family / missing capstone source 至少有一层在 `longRunLab` 前失败。
2. stronger metrics 先 report-only，再单独 PR 提 blocking。
3. `phase4Report` note 不再只说 aggregate 数值，必须能指向 culprit zone/source/item。

### 11.7 必须新增的测试

1. duplicate passive family preflight regression test
2. reward source coverage regression test
3. staged stronger metrics render test
4. debug artifact schema test

### 11.8 必跑命令

```bash
./gradlew maintainabilityLint whiteBoxLoot longRunLab verifyOwner phase4Report
```

### 11.9 退出标准

1. 同区 passive-family duplicate 能在 `whiteBoxLoot` 或更早失败。
2. reward source drift 能在 `whiteBox / preflight` 层直接报 culprit。
3. stronger per-profession adoption / non-weapon floor 已经 report-only 可见。
4. `phase4Report` 不再需要人工全文 grep 才能判断根因。

---

## 12. 全局风险与防呆

### 12.1 最大风险

1. 把 authority 做成第二真源
2. 在同一 PR 里同时修改 authority + gate + baseline，导致无法判断回归来自哪里
3. selector extraction 改语义过大，顺手重开 balance

### 12.2 防呆策略

1. PR-01 只动 routing，不动 selector。
2. PR-02 只做 shared catalog，不切 stronger blocking gate。
3. PR-03 只收 selector，不改 baseline。
4. PR-04 只做 fast-fail 和 staged metrics；真正 gate cutover 如有必要，单独再开 PR。

### 12.3 不允许的 shortcut

1. 不允许继续在 `SmokeBot` 里追加新的 profession item hardcode。
2. 不允许在 `tools` 里单独维护一套 capstone item list。
3. 不允许为 debug 再造一个平行 summary schema。
4. 不允许通过放松 baseline 来掩盖 routing / selector / adoption 问题。

---

## 13. Rollback 策略

如果这组 hardening 中任一 PR 导致主干 owner gate 失稳，回滚顺序固定为：

1. 先回滚新的 blocking gate 或 baseline cutover
2. 保留 additive diagnostic artifact
3. 保留数据 authority 与 contract test
4. 最后才考虑回滚 selector implementation

原因：

1. authority 和 diagnostics 是长期收益；
2. 真正高风险的是过早提升 blocking 语义；
3. 不应该因为一个 gate cutover 失败就把 fast-fail 基础设施一起撤掉。

---

## 14. 完成定义

这套方案真正完成时，应该满足以下结果：

1. 一个 interactable 奖励接错 profile，会在 data/contract 层直接失败。
2. 一个 capstone 候选被普通 floor 规则误删，会在 selector unit test 直接失败。
3. 一个 profession capstone 被看到但 bot 不穿，会在 shared build identity / harness adoption 层直接解释。
4. 一个 special-tier passive family 冲突，会在 preflight/white-box 层直接列出 culprit zone 和 item。
5. 只有真正的 run-level 组合问题，才需要进入 `longRunLab + phase4Report`。

达到这五条，才能说这次“修了几个小时”的教训被真正工程化，而不是只把当前这一次 artifact 调绿。

