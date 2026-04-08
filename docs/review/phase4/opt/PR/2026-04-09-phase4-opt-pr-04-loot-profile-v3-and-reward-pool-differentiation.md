> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md`
> `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - OPT PR-04 LootProfile V3 与奖励池差异化

**阶段**: `Phase 4 / Post-Review Follow-up / OPT-W4`  
**优先级**: `P0`  
**前置条件**: `OPT PR-01`、`OPT PR-03` 完成  
**对应问题**: 当前 `LootProfileSchemaV2` 只有 `itemIds + rewardBudget`，导致不同 zone/reward path 的终端候选池重叠过高，奖励差异感不够。

---

## 1. 阶段目标

以一次性破坏式迁移的方式，把 `LootProfile` 从当前固定列表模型重构为“可按 tag / type / slot 偏置构建候选池”的单一 V3 合同，不保留双 schema 长期共存。

完成标准：

1. `lootProfileBaseItemOverlapMatrix` 的平均重叠率降到 `<30%`
2. `lootProfileDistinctBaseItemCount >= 35`
3. 全部正式 loot profile 统一迁移到 `schemaVersion = 3`
4. runtime / content-pack overlay 只接受 V3，旧 V2 数据 fail-fast

## 2. 当前问题

1. `LootProfileSchemaV2` 无法表达 tag-weighted 候选池
2. 当前 runtime 先从 `profile.itemIds` 选 base item，再套预算，导致 zone identity 容易趋同
3. 原 review 方案中的 `zoneExclusiveIds / rewardBudgetToDropCount / ACCESSORY` 与现状模型冲突

### 2.1 本 PR 必须冻结的口径

1. `rewardBudget` 继续是“质量/预算”概念，不转义成多掉落数量。
2. 仍然走单 item reward 的正式路径。
3. special item 仍走现有 `SpecialTierEligibility + template pool` 真源。
4. V3 允许增加“偏好”，不允许建立第二套 zone-exclusive special template 真源。
5. 本 PR 不做 `V2 + V3` 双路径共存；所有正式 profile 与 loot profile overlay 一次性迁到 V3。

## 3. 范围与非目标

### 3.1 范围

1. 用 `LootProfileSchemaV3` 替换当前正式 schema
2. 一次性迁移全部 `21` 个正式 profile 到 V3
3. 扩 DataLoader、runtime、content-pack overlay 白名单
4. 更新 `whiteBoxLoot / lootBalanceLab / contentPackHarness`

### 3.2 非目标

1. 不重写 `ItemGenerator` 主合同
2. 不引入掉落脚本
3. 不在本 PR 新增玩家可见资源 key
4. 不为了兼容旧 schema 保留第二套 loader / overlay / runtime 分支

## 4. 技术方案

### 4.1 `LootProfileSchemaV3`

保留的业务语义字段：

1. `itemIds`
2. `rewardBudget`

新增字段：

1. `poolStrategy`
2. `itemTagFilter`
3. `excludeIds`
4. `typeWeights`
5. `slotBias`
6. `specialTemplateTagPreference`
7. `affixTagPreference`

明确不纳入：

1. `categoryWeights`
2. `zoneExclusiveIds`
3. `rewardBudgetToDropCount`

### 4.1.1 破坏式迁移规则

本 PR 明确采用单 schema 收口，不保留任何长期双轨：

1. `SchemaModels.kt` 中正式类型直接升级为 `LootProfileSchemaV3`
2. `GameContent`、runtime、tools、content-pack overlay 全部只读 V3
3. `schemaVersion = 2` 的 loot profile 在 DataLoader 层直接报错，禁止进入运行时
4. 不保留 `LegacyLootProfileAdapter`、双 parser、双 white-box summary、双 overlay merge 分支
5. 所有官方 data、sample pack、fixture pack 必须在同一 PR 内完成迁移

### 4.2 候选池算法

目标流程：

1. 先按 `itemTagFilter` 构建 base pool
2. 排除 `excludeIds`
3. 按 `typeWeights` 做 `ItemType` 分桶偏置
4. 如有需要，再按 `slotBias` 做 `EquipSlot` 二次偏置
5. 仍由现有 rarity / pity / special template pipeline 决定稀有度和 special roll

### 4.3 迁移策略

采用一次性正式迁移，不保留 V2：

1. `21` 个正式 profile 全部升到 `schemaVersion = 3`
2. foundation fallback 仍可使用 `poolStrategy = FIXED_LIST`
3. 早期 zone 若不需要差异化，仍使用 V3，但走 `FIXED_LIST`
4. 中后期 zone、reward、secret profile 走 `TAG_WEIGHTED`
5. DataLoader 在运行时拒绝 `schemaVersion = 2` 的正式 profile

### 4.3.1 执行顺序

为避免中间态长期存在，迁移顺序固定为：

1. 先改 `SchemaModels.kt / GameContent.kt / DataLoader.kt`
2. 再一次性迁移 `data/loot/index.yaml`
3. 同轮修改 `ContentPackRuntimeResolver.kt` 与 sample/fixture pack
4. 最后跑 `whiteBoxLoot / lootBalanceLab / contentPackHarness`

不接受的做法：

1. 先让 runtime 同时接受 V2/V3，等以后再清理
2. 先只迁移一半 profile，另一半靠 V2 挂着
3. 先给 official data 升级，把 sample pack/fixture pack 留到后续 PR

### 4.4 content-pack 与破坏式迁移

本 PR 不为旧 schema 保留 pack 兼容层。策略如下：

1. runtime 只接受 V3 loot profile overlay
2. 旧 V2 pack 若仍试图覆盖 loot profile，直接 fail-fast 并给出结构化错误
3. sample pack 与所有 fixture pack 在本 PR 同步迁到 V3
4. `ContentPackHarness` 只验证 V3 profile 的 `REPLACE / ADD / precedence`

### 4.4.1 Fail-Fast 诊断要求

对旧 V2 overlay 的拒绝必须结构化，不接受“读不懂就当成无效字段跳过”：

1. 错误信息至少包括：
   - pack id
   - overlay 目标 profile id
   - 实际 schemaVersion
   - 期望 schemaVersion
2. `contentPackHarness` 要保留失败 artifact
3. `phase4Report` 要能汇总“旧 pack 被拒绝”的原因摘要

特别约束：

`ContentPackRuntimeResolver` 对 loot profile 的 overlay 白名单必须扩到：

1. `itemTagFilter`
2. `excludeIds`
3. `typeWeights`
4. `slotBias`
5. `specialTemplateTagPreference`
6. `affixTagPreference`

## 5. 推荐改动面

### 5.1 `game`

1. `SchemaModels.kt`
2. `DataLoader.kt`
3. `GameContent.kt`
4. `FoundationGameSession.kt` 或实际候选池构建入口
5. `game/src/main/resources/data/loot/index.yaml`

### 5.2 `content pack`

1. `ContentPackRuntimeResolver.kt`
2. sample pack 若覆盖相关 profile，需同步迁移

### 5.3 `tools`

1. `WhiteBoxLootRunner`
2. `LootBalanceLabRunner`
3. `ContentPackHarnessRunner`
4. `Phase4ReportRunner`

## 6. 测试与自证

### 6.1 自动化命令

```bash
./gradlew :game:test
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew contentPackHarness
./gradlew whiteBoxVerify
./gradlew phase4Report
```

### 6.2 必测行为

1. `schemaVersion = 2` 的 loot profile 会被明确拒绝
2. tag-weighted 候选池不为空
3. `lootProfileBaseItemOverlapMatrix` 明显下降
4. rarity / pity 分布不出现不可解释漂移
5. pack overlay 不裁剪 V3 字段

## 7. 资源生成计划

### 7.1 图片

本 PR 不新增图片资源。

### 7.2 音频

本 PR 不新增音频资源。

### 7.3 约束

1. 不允许为 schema 迁移生造空 plan
2. 若后续发现迁移引入了净新增 item/zone key，应另开 companion asset PR，而不是在本 PR 临时补图补音

## 8. 出口门禁

1. `lootProfileDistinctBaseItemCount >= 35`
2. `lootProfileBaseItemOverlapMatrix` 平均重叠 `<30%`
3. V3 迁移 profile 全部通过 DataLoader 校验
4. `contentPackHarness` 覆盖 V3 overlay 场景，并能对 V2 overlay fail-fast
5. `lootBalanceLab` 的整体分布仍在 checklist 容差内
