# Phase 4 PR-06 深度审查报告

## 审查结论

当前改造已经把 `PR-06` 的主体骨架落进主路径：

1. 五条正式地形交互规则已经冻结到 `ElementInteractionRegistry`，并且只在 `CombatPipeline step 9` 挂接。
2. `EliteMutationDef / BossVariantDef` 已进入正式 schema、registry、runtime、snapshot、inspect、log、visual/audio 资源与 white-box 报告链路。
3. `terrainInteractionBatch + bossHarness + phase4Report` 已经形成可读取的 artifact 收口，现有报告也显示为 `PASS`。

但按 `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md` 的正式口径审查，当前实现仍然不能判定为“完全一致”。

阻塞性偏差主要有一个，重要偏差有四个：

1. `bossVariant.lootProfileOverride` 只被“挂到账本”，但账本金额语义不是正式 reward budget，而且缺少 fail-fast 校验；一旦 profile 写错，运行时可能不掉落，账本却仍然记账。
2. `visualTintKey` 又被翻译成硬编码颜色，形成第二套 Boss variant 表现权威。
3. `actionWeightProfileId` 只校验 profile 是否存在，不校验 action 是否属于 base encounter 已暴露动作；未知 action 会被静默忽略。
4. elite mutation 的 tier 权重没有按 floor / zone 递进，当前只是“固定权重 + 数据层 minFloor”。
5. `terrainInteractionBatch` 的 “registry consumption” 断言只检查字符串前缀，不证明 mutation / boss metadata 真被地形 registry 消费。

结论上，这一批改造适合判定为：

- 主合同已接通
- 白盒与资源链路已成型
- 仍需一次“合同收口 + 证据收口”的小修，才能算和 PR-06 文档完全一致

## 审查范围与证据

审查对象：

- 当前 `git diff`
- `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`
- `docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md`
- `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
- `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
- `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`

已读取的关键实现面：

- `core/src/main/kotlin/com/ktome/core/combat/*`
- `core/src/main/kotlin/com/ktome/core/mapgen/TerrainOverride.kt`
- `core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt`
- `game/src/main/kotlin/com/ktome/game/elites/*`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/GameContent.kt`
- `game/src/main/kotlin/com/ktome/game/GameModule.kt`
- `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt`
- `game/src/main/resources/data/elites/index.yaml`
- `game/src/main/resources/data/boss-variants/index.yaml`
- `client/src/main/kotlin/com/ktome/client/render/*`
- `client/src/main/kotlin/com/ktome/client/audio/AudioRouter.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`
- `game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt`
- `game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt`
- `tools/build/reports/phase4/phase4-summary.json`
- `tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json`
- `tools/build/reports/phase4/whitebox/boss/whitebox-boss-summary.json`

本次实际执行命令：

- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew terrainInteractionBatch`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew bossHarness`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew phase4Report`

结果说明：

- 三个命令均返回 `BUILD SUCCESSFUL`
- `terrainInteractionBatch` 与 `bossHarness` 本轮均为 `UP-TO-DATE`
- `phase4Report` 本轮重新聚合了 summary；当前 `tools/build/reports/phase4/phase4-summary.json` 显示 `bossHarness`、`terrainInteractionBatch`、`lootBalanceLab`、mapgen/solvability white-box 均为 `PASS`
- 由于主验证任务本轮未重算 case，因此下面关于实现偏差的判断主要来自代码与现有 artifact 的交叉核查，而不是新鲜跑出的失败样本

## Findings

### P1

#### 1. `bossVariant.lootProfileOverride` 已接入账本，但账本金额不是正式 reward budget，且缺少 fail-fast 校验

证据：

- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:3297-3311`
  - `FloorRewardBudget.rewardDeltas` 的 `amount` 直接取 `lootProfile(lootProfileOverride)?.itemIds?.size?.coerceAtLeast(1) ?: 1`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:9165-9179`
  - 实际掉落路径里，`lootProfile(profileId)` 解析失败会直接 `return null`
- `game/src/main/kotlin/com/ktome/game/GameContent.kt:118-131`
  - `BossVariantDef` 只校验了 `baseEncounterId`、`grantedMutations`、`actionWeightProfileId`
  - 没有对 `lootProfileOverride` 是否存在做 fail-fast 校验

问题本质：

- 文档要求 `bossVariant.lootProfileOverride` 必须进入统一 `FloorRewardBudget` 记账，不能绕开横切总账。
- 当前实现虽然“记账了”，但 `amount` 不是正式 reward budget 货币，而是 `itemIds.size` 的近似值。
- 更严重的是，如果 `lootProfileOverride` 拼错：
  - 运行时掉落路径会因为 `lootProfile(profileId) ?: return null` 直接不掉东西
  - 账本仍然会用 `?: 1` 记一条正向 `RewardDelta`

偏差程度：

- `FloorRewardBudget.totalBudget` 与实际 variant 奖励强度之间目前不是同一种量纲。
- `bossHarness` / `phase4Report` 现在只能证明“source 出现在账本里”，不能证明“账本金额是对的”。
- 一旦数据写错，会出现“报告看起来通过，但玩家实际少拿奖励”的假阳性。

修复建议：

1. 对 `BossVariantDef.lootProfileOverride` 增加正式存在性校验，至少在 `GameContent` 初始化时 fail-fast。
2. 不要再用 `itemIds.size` 充当 reward budget。
3. 二选一收口：
   - 方案 A：给 `BossVariantDef` 增加显式 `rewardBudgetDelta`
   - 方案 B：给 `LootProfileSchemaV2` 增加正式预算字段，再由 `FloorRewardBudget` 消费
4. 把 `bossHarness` 的 `rewardLedgerMatched` 从“source 存在”升级为“delta 数值与正式预算字段一致”。

### P2

#### 2. `visualTintKey` 又被翻译成硬编码颜色，形成第二套 Boss variant 表现权威

证据：

- `game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt:103-119`
  - `BossVariantRuntime` 已保存 `visualTintKey`
  - 同时又把 `DisplayColor.hex` 改写成 `tintToColorHex(...)`
- `game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt:257-266`
  - `visualTintKey -> colorHex` 是硬编码表
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1376-1388`
  - render snapshot 同时输出 `bossVariant.visualTintKey` 与 `displayTintColorHex`

问题本质：

- PR-06 文档要求 Boss variant 只 overlay `mutation / lootProfile / 表现 key / 有限权重`。
- 当前实现把 `visualTintKey` 之外，又额外派生出一套 `displayTintColorHex`。
- 这会导致：
  - 资源 key 是一套权威
  - `tintToColorHex` 又是另一套权威
  - 任一侧修改都可能让 variant 表现与资源计划漂移

偏差程度：

- 当前不是“完全错误”，因为 client 最终能显示 tint。
- 但它破坏了 PR-06 想冻结的“表现 key 单一权威”。

修复建议：

1. 去掉 `EncounterDecorationService.tintToColorHex()` 这条派生链。
2. client 只消费 `visualTintKey`，不要再从 `game` 层拿硬编码颜色。
3. 若必须保留 tint 颜色，应把颜色映射下沉到 manifest / asset metadata，而不是写在 `game` 代码里。

#### 3. `actionWeightProfileId` 没有校验 action 是否属于 base encounter 已暴露动作，未知 action 会被静默忽略

证据：

- `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt:128-137`
  - `ActionWeightProfileDef` 只要求 key 非空、value 非负
- `game/src/main/kotlin/com/ktome/game/GameContent.kt:118-131`
  - `BossVariantDef` 只校验 profile id 存在
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:2313-2327`
  - `applyActionWeightProfile()` 只会覆写当前 profile 里命中的 action
  - 不存在的 action id 会被静默忽略

问题本质：

- 文档要求 `actionWeightProfileId` 只能覆写“既有 encounter 已暴露给 variant 的少量候选动作权重”。
- 现在 profile 里即使写了不存在的 action：
  - 初始化不会失败
  - 运行时也不会报警
  - 变体效果会被悄悄吞掉

偏差程度：

- 当前样例数据刚好命中了已有动作，所以 `bossHarness` 全绿。
- 但 contract 仍然没有被代码层真正冻结。

修复建议：

1. 在 `GameContent` 初始化时，把 `actionWeightProfileId` 对应的 action id 与 `baseEncounterId` 的 phase AI profile 动作集合做交集校验。
2. 出现未知 action id 时直接 fail-fast，而不是在 runtime 静默忽略。
3. 在 `BossHarnessTest` 中增加一个负样本 fixture，专门验证非法 action id 会被拒绝。

#### 4. elite mutation 的 tier 权重没有按 floor / zone 递进，当前只是固定权重

证据：

- `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt:180-225`
  - 候选集只按 `floor/zone/tag` 过滤
  - 真正抽样权重固定为 `MINOR=6 / MAJOR=3 / SIGNATURE=1`

问题本质：

- 文档明确要求：
  - `MutationTier` 固定为 `MINOR / MAJOR / SIGNATURE`
  - 默认权重要按 `floor / zone` 递进
  - 不能只靠低层数据碰巧不写 `SIGNATURE`
- 当前实现的低层保护主要依赖 `minFloor`
- 这能防止“完全过早刷出 signature”，但没有实现“默认权重递进”

偏差程度：

- 合同实现属于“能用但不完整”。
- 一旦后续内容量扩大，不同 zone / floor 的 mutation 体感差异会比文档预期更平。

修复建议：

1. 把 `tierWeight()` 改为 `tierWeight(tier, context)`。
2. 至少引入：
   - floor 段位修正
   - zone allow-list / zone danger band 修正
3. 给 `EliteMutationRegistry` 增加固定 seed 测试，覆盖：
   - 低层 signature 权重受限
   - 高层 / 高压 zone 的 major / signature 权重上升

#### 5. `terrainInteractionBatch` 对“mutation / boss registry consumption”的证明过弱，当前只是检查 tag 前缀

证据：

- `core/src/main/kotlin/com/ktome/core/combat/ElementInteractionRegistry.kt:37-45`
  - `TerrainInteractionContext` 虽然携带了 `sourceTags`
- 但 registry 内没有任何逻辑消费 `sourceTags`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:6621-6625`
  - session 只是在构造 context 时把 `sourceTags` 塞进去
- `game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt:447-455`
  - `terrain.case.registry_consumption` 断言只检查 tag 是否以 `mutation:elite.` 或 `bossVariant:boss.variant.` 开头
- `game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt:468-480`
  - aggregate 里的 `registryConsumptionCount` 也是按同样的 tag 前缀统计

问题本质：

- white-box summary 当前声称：
  - “At least one terrain interaction provenance case consumes mutation/boss registry metadata.”
- 但代码实际上并没有证明 registry 真的消费了 mutation / boss metadata
- 现在证明的只是“case 人工塞了一个符合命名规范的 tag”

偏差程度：

- 这不会直接破坏运行时功能
- 但会让 `terrainInteractionBatch` 对 PR-06 的 provenance 证据强度不足，属于“报告比实现更强”

修复建议：

1. 如果 mutation / boss metadata 本轮确实不参与 rule 决策，就不要在白盒 aggregate 里宣称“consumes registry metadata”。
2. 如果后续要让 mutation / boss metadata 参与 rule 决策，就必须在 `ElementInteractionRegistry.resolve()` 里显式消费这些 typed source 条件，而不是只带字符串 tag。
3. 把当前 aggregate rule 改成更保守的表述，例如：
   - “provenance case retains formal mutation source tags”

## Requirement Alignment

### 1. 五个正式地形交互 rule id 已冻结，且 `CombatPipeline step 9` 是唯一正式挂点

- 证据：
  - `core/src/main/kotlin/com/ktome/core/combat/ElementInteractionRegistry.kt`
  - `core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt`
- 结论：**一致**

### 2. `EliteMutationDef` 与 `BossVariantDef` 已进入正式 schema / registry / runtime / snapshot

- 证据：
  - `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt`
  - `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
  - `game/src/main/kotlin/com/ktome/game/GameContent.kt`
  - `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt`
  - `core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt`
- 结论：**一致**

### 3. mutation 可读性（inspect / log / icon / 资源计划）已接通

- 证据：
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/main/kotlin/com/ktome/client/audio/AudioRouter.kt`
  - `assets-src/image/specs/phase4-pr06-gemini-plan.yaml`
  - `assets-src/audio/specs/phase4-pr06-audio-plan.yaml`
- 结论：**一致**

### 4. Boss variant 没有重写 phase graph，只做 mutation / loot / 表现 / action weight overlay

- 证据：
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:2282-2327`
  - `game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt`
  - `tools/build/reports/phase4/whitebox/boss/whitebox-boss-summary.json`
- 结论：**一致**

### 5. mutation 组合与 tier 递进口径

- 证据：
  - `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt:180-225`
- 结论：**部分一致**

说明：

- `SIGNATURE + SIGNATURE` 已被禁止
- `maxMutationsPerElite = 2` 也已冻结
- 但默认权重没有按 `floor / zone` 递进，仍与文档有差距

### 6. `bossVariant.lootProfileOverride / threatCost` 进入统一 ledger

- 证据：
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:3297-3339`
  - `tools/build/reports/phase4/whitebox/boss/whitebox-boss-summary.json`
- 结论：**部分一致**

说明：

- `threatCost` 接线是干净的
- `lootProfileOverride` 虽然已经记账，但金额语义不是正式 budget，且缺少 fail-fast 校验

### 7. `terrainInteractionBatch + bossHarness + phase4Report` 的统一证据链

- 证据：
  - `build.gradle.kts`
  - `game/build.gradle.kts`
  - `tools/build.gradle.kts`
  - `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`
  - `tools/build/reports/phase4/phase4-summary.json`
- 结论：**一致**

### 8. PR-06 白盒对 provenance / registry 消费的证明强度

- 证据：
  - `game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt:447-480`
  - `tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json`
- 结论：**部分一致**

说明：

- 报告结构已经齐
- 但关于 mutation / boss registry consumption 的证明仍偏弱

## Removal/Iteration Plan

### Safe to Remove Now

#### Item: `vfx.boss.variant.ashen_tyrant` 的遗留 tint 分支

| Field | Details |
| --- | --- |
| **Location** | `game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt:263` |
| **Phase/Work Package** | `Phase 4 / P4-W4a` |
| **Touched contract** | `BossVariantDef` 表现 overlay |
| **Evidence** | 全仓库 `rg "ashen_tyrant"` 只剩这一处命中，没有数据、资源、测试或 manifest 引用 |
| **Deletion or iteration steps** | 1. 删除 `tintToColorHex()` 中该分支 2. 跑 `bossHarness` 与 `phase4Report` 确认无影响 |
| **Affected harness/gates** | `./gradlew bossHarness`, `./gradlew phase4Report` |
| **White-box check** | 不需要额外白盒；Boss variant corpus 已覆盖现有三条正式 variant |
| **Rollback or fallback** | 若后续文档重新引入该 variant，再连同数据与资源一起恢复 |

### Defer Removal

- 当前未识别出其他可以“立即删除”的安全冗余路径。
- `displayTintColorHex` 相关路径虽然存在第二套权威问题，但它已经被 client 渲染链消费，属于“先收口 contract，再 staged cleanup”的范畴，不建议在未调整 render contract 前直接删。

## Additional Suggestions

1. `visibleEncounterReadabilityEvents()` 与 `logDecorationMessages()` 现在同时承担 variant / mutation 可读性输出，建议后续收口成“事件日志一条主路径 + render 只消费结果”，避免长期存在双日志来源。
2. `BossVariantSelectionMode.AUTO` 目前是简单的 `50%` 开关；如果后续要体现“有限权重”，建议把“是否出 variant”与“出哪个 variant”统一到一个显式 weighted policy，而不是一半随机跳过。

## Summary

这批 PR-06 改造已经把正式地形交互、elite mutation、Boss variant、资源计划、white-box 报告与 phase4 汇总都搭起来了，工程完成度明显高于“只把功能拼出来”的状态。

但当前还差最后一层合同收口：

1. reward ledger 还不是正式货币；
2. variant 表现仍有双权威；
3. action-weight overlay 与 provenance 证据还不够硬；
4. mutation tier 递进规则仍然是简化版。

建议优先修复 Finding 1，再处理 Finding 2 和 3。这样可以先把“账本正确性”和“overlay 边界”收实，再去补强体验分布与白盒证据强度。
