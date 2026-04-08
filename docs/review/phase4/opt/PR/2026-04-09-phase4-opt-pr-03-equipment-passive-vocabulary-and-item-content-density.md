> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
> `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - OPT PR-03 装备机制词汇扩容与装备内容密度补齐

**阶段**: `Phase 4 / Post-Review Follow-up / OPT-W3`  
**优先级**: `P0`  
**前置条件**: `OPT PR-01` 完成，现有 `lootBalanceLab / whiteBoxLoot` 可稳定回归  
**对应问题**: 当前 `EquipmentPassive` 词汇过窄，导致新增 affix / unique / artifact 也很难形成 build-driving 差异。原 `OPT-PR-03` 与 `OPT-PR-04` 依赖过强，拆开实施会造成两轮内容返工，因此合并为一个标准 PR。

---

## 1. 阶段目标

在不引入第二套装备系统的前提下，同时完成两件事：

1. 扩 `EquipmentPassive` 的最小正式词汇
2. 在同一 PR 内把 affix / unique / artifact 内容密度补到可发布水位

完成标准：

1. `EquipmentPassive` 从 `5` 类扩到 `9` 类
2. affix 从 `40` 扩到 `>=75`
3. unique 从 `12` 扩到 `>=20`
4. artifact 从 `4` 扩到 `>=8`
5. `affixPassiveCoverage >= 80%`
6. `uniqueArtifactMeaningfulSwapRate >= 50%`

## 2. 当前问题

1. affix 侧实际只使用 `DamageVsStatus` 与 `HpRegenPerTurn`
2. unique / artifact 虽有部分被动，但 build identity 仍弱
3. 继续只加数值 YAML，会让内容总量上去、体验差异不成立

### 2.1 本 PR 必须冻结的口径

1. 不新增脚本 runtime。
2. 不新增 `DamageType / StatusEffectType / ResourceType`。
3. 不引入 `ACCESSORY` 或 `ANY` 这类当前模型不存在的 slot / equip type。
4. final affix id / affixFamily / exclusiveGroup 必须先做 registry audit，不能直接锁死到文档示例名。
5. terrain affinity 只消费 `game` 层归一化 terrain context，不把地图状态重新拉进 `core`。

## 3. 范围与非目标

### 3.1 范围

1. 新增 `4` 个 `EquipmentPassive` 家族
2. 扩充 affix / unique / artifact 数据
3. 扩 inspect / log / trace 的被动可追溯性
4. 补 item 相关 visual/audio 资源主批次

### 3.2 非目标

1. 不重写 `LootBudget` 或 rarity pipeline
2. 不在本 PR 做 `LootProfile V3`
3. 不引入新的装备栏位

## 4. 技术方案

### 4.1 `EquipmentPassive` 最小扩展

新增 `4` 类：

1. `OnHitStatusProc`
2. `OnKillResourceRestore`
3. `ConditionalStatBonus`
4. `TerrainAffinityBonus`

`PassiveCondition` 只保留最小集：

1. `HP_BELOW_50`
2. `HP_BELOW_30`
3. `HP_ABOVE_80`
4. `SELF_HAS_STATUS`

不纳入本 PR：

1. `TARGET_HAS_STATUS`
2. `CONSECUTIVE_HIT`

原因：

1. 它们需要新的 combat memory / target-state tracking
2. 会把“最小 contract 扩展”推成“半套新战斗记忆系统”

### 4.2 运行时集成

建议落点：

```text
core/src/main/kotlin/com/ktome/core/item/ItemModels.kt
core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt
core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt
game/src/main/kotlin/com/ktome/game/data/DataLoader.kt
```

关键规则：

1. `OnHitStatusProc`
   - 走正式 status application
   - RNG 只允许使用正式 combat RNG
2. `OnKillResourceRestore`
   - 走现有 resource restore 路径
3. `ConditionalStatBonus`
   - 只在 stat aggregation 阶段生效
4. `TerrainAffinityBonus`
   - 由 `game` 层把当前格 terrain context 显式传入
   - `core` 不读地图

### 4.3 内容密度目标

| 类别 | 当前 | 目标 |
| --- | --- | --- |
| affix | 40 | `>=75` |
| unique | 12 | `>=20` |
| artifact | 4 | `>=8` |
| 总量 | 56 | `>=103` |

内容策略：

1. affix 新增分为：
   - 机制型前缀
   - 机制型后缀
   - 通用数值型补齐
2. unique / artifact 的新增模板必须优先使用新 passive 家族
3. 现有 artifact 允许做 “rework” 而不是只再堆更多模板

### 4.4 数据设计约束

1. affix 和 item template 的最终正式 ID 以 registry audit 后结果为准
2. 文档里的语义方向可以先占位，但不允许直接与现有 key 冲突
3. 任何 terrain 相关 item 被动都必须与 `OPT PR-06` 的 terrain 可感知性策略兼容

### 4.5 Trace / Log / Inspect

本 PR 必须同步补：

1. `CombatResolutionTrace` 的 passive trigger 记录
2. inventory / reward / inspect 的 passive 可读描述
3. 日志中的 item display token 不得退回 base item 的弱语义

## 5. 推荐改动面

### 5.1 `core`

1. 新增 passive 家族与 resolver
2. 扩 `CombatPipeline` 的 passive 触发挂点
3. 补 unit / integration test

### 5.2 `game`

1. 扩 schema / loader
2. 扩 `items/index.yaml`
3. 扩 i18n
4. 把 terrain context 归一化后喂给战斗/统计路径

### 5.3 `tools`

1. `whiteBoxLoot` 指标扩展
2. `lootBalanceLab` 观测新 passive 覆盖
3. `phase4Report` 透传

### 5.4 `client`

1. inspect 面板显示新 passive 描述
2. 如果 item cue / visual 更新，需要补 `goldenScreenshot`

## 6. 测试与自证

### 6.1 自动化命令

```bash
./gradlew :core:test
./gradlew :game:test
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew whiteBoxVerify
./gradlew phase4Report
./gradlew clientSmoke
./gradlew goldenScreenshot
```

### 6.2 必测行为

1. 每个新 passive 至少 `2` 个单测 + `1` 个 combat 集成测试
2. `PassiveEffectResolver` 的新增分支都有覆盖
3. 新 affix / unique / artifact 能被正式生成并正确展示
4. deterministic replay 不被 on-hit RNG 打坏

## 7. 资源生成计划

### 7.1 图片

1. 计划文件：`assets-src/image/specs/phase4-opt-pr03-gemini-plan.yaml`
2. 报告文件：
   - `assets-src/image/manifests/phase4-opt-pr03-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-opt-pr03-processing-report.jsonl`
3. 覆盖对象：
   - `item.unique.*.icon`
   - `item.unique.*.visual`
   - `item.artifact.*.icon`
   - `item.artifact.*.visual`
   - affix 净新增 icon
4. 推荐拆分：
   - Batch A: new unique
   - Batch B: new / reworked artifact
   - Batch C: affix icon

### 7.2 音频

1. 计划文件：`assets-src/audio/specs/phase4-opt-pr03-audio-plan.yaml`
2. 报告文件：
   - `assets-src/audio/manifests/phase4-opt-pr03-generation-report.jsonl`
   - `assets-src/audio/manifests/phase4-opt-pr03-processing-report.jsonl`
3. 覆盖对象：
   - `audio.item.unique.*`
   - `audio.item.artifact.*`

### 7.3 管线

图片：

```bash
GEMINI_API_KEY=your_key \
GEMINI_CONCURRENCY=4 \
./scripts/generate_assets.sh \
  assets-src/image/specs/phase4-opt-pr03-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase4-opt-pr03-generation-report.jsonl
```

音频：

```bash
python3 scripts/generate_opt_pr03_audio.py \
  --plan assets-src/audio/specs/phase4-opt-pr03-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr03-generation-report.jsonl

python3 scripts/process_audio.py \
  --filter-plan assets-src/audio/specs/phase4-opt-pr03-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr03-processing-report.jsonl
```

### 7.4 约束

1. 最终资源 key 集必须与 `items/index.yaml` 净新增 key 一一对应
2. 不允许靠 fallback manifest 吞掉漏图/漏音
3. 纯复用现有 key 的模板不重复生成资源

## 8. 出口门禁

1. `EquipmentPassive` 子类达到 `9`
2. affix / unique / artifact 总量达到 `>=103`
3. `affixPassiveCoverage >= 80%`
4. `uniqueArtifactMeaningfulSwapRate >= 50%`
5. 新 item 资源全部进入正式 manifest / lint / golden 路径
