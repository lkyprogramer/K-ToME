> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md`
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - PR-05 Affix Cost、Unique/Artifact 与 LootBalanceLab

**阶段**: `Phase 4 / P4-B / P4-W3b`  
**优先级**: `P0`  
**前置条件**: `PR-04` 完成  
**对应问题**: `PR-04` 只冻结了预算公式和掉落入口，当前仓库仍缺少 `affix cost`、`UNIQUE / ARTIFACT` 模板边界和实验室门禁。若直接往旧 affix 池里堆内容，不仅预算不可解释，`castSpeed` 等高风险词条也会继续绕过正式 DR 路径。

---

## 1. 阶段目标

把 `Phase 4` 的掉落生态从“预算模型存在”推进到“可扩展且可实验室量化”。

完成标准：

1. `AffixCost` 体系冻结，并建立 `TRIVIAL / MINOR / MEDIUM / MAJOR / SIGNATURE` 成本带。
2. `UNIQUE / ARTIFACT` 进入正式模板池，不再退化为随机 affix 伪稀有。
3. `castSpeed` affix 被强制接入既有 DR 逻辑。
4. `lootBalanceLab` root alias 建立，并输出结构化 batch 报告。
5. 本 PR 需要的 item icon / inspect portrait / item cue 有完整资源生成计划。

## 2. 当前问题

1. 现有 affix 只有 `tier/minFloor/statModifiers`，没有正式预算货币。
2. `ItemGenerator` 还没有 `UNIQUE / ARTIFACT` 模板路径。
3. `lootBalanceLab` 尚不存在，无法对 `MAGIC / RARE / UNIQUE / ARTIFACT` 分布和 affix budget 偏差做量化。

### 2.1 本 PR 必须冻结的口径

1. `MAGIC` 允许 `1~2` affix，`RARE` 允许 `2~4` affix，总成本不得超过 `affixBudget`。
2. `UNIQUE / ARTIFACT` 只能来自 `SpecialTierEligibility + upgrade roll + 模板池`，不做二次随机 affix 拼装，也不再走“命中后降级”的主路径。
3. `ARTIFACT` 只允许来自 `BOSS / SECRET_ZONE / special reward chest`。
4. `castSpeed` 词条只允许通过正式 `DR_CAST_SPEED_C` 生效。
5. root alias `./gradlew lootBalanceLab` 必须建立，并固定报告路径：
   - `tools/build/reports/phase4/loot/loot-balance-summary.json`
   - `tools/build/reports/phase4/loot/loot-balance-rolls.jsonl`
6. 本 PR 必须把 `PR-04` 已冻结的 `PityTracker` 真正接进生成链和实验室报告，不允许只停留在 schema。

## 3. 范围与非目标

### 3.1 范围

1. 新建 `AffixCost` 与成本带。
2. unique/artifact 模板池和来源过滤。
3. `lootBalanceLab` runner、固定矩阵和偏差报告。
4. item 相关 visual/audio 资源计划。
5. `PityTracker` 运行时接线与统计报告。

### 3.2 非目标

1. 不在本 PR 引入 elite mutation 或 hidden reward。
2. 不在本 PR 重写整个 item schema；只在现有 item/affix schema 上补正式预算字段和模板路径。

## 4. 技术方案

### 4.1 `AffixCost` 体系

建议文件：

```text
core/src/main/kotlin/com/ktome/core/loot/AffixCostModels.kt
game/src/main/resources/data/items/affixes/*.yaml
core/src/test/kotlin/com/ktome/core/loot/AffixCostTest.kt
```

核心结构：

```kotlin
data class AffixCost(
    val affixId: String,
    val cost: Int,
    val affixFamily: String,
    val exclusiveGroup: String? = null,
    val slotTags: Set<String>,
    val phase: String,
)
```

固定成本带：

1. `TRIVIAL = 1`
2. `MINOR = 3`
3. `MEDIUM = 6`
4. `MAJOR = 10`
5. `SIGNATURE = 14`

补充规则：

1. `TRIVIAL` 只用于补齐预算尾差，不允许作为单个高价值 affix 的主要成本带。
2. affix 选择顺序固定为“先大后小”：
   - 先从 `SIGNATURE / MAJOR / MEDIUM` 中选主 affix
   - 剩余预算若落在 `1 ~ 2`，允许用 `TRIVIAL` 填充
3. 同一件物品最多允许 `1` 个 `TRIVIAL` affix，避免通过大量碎片 affix 伪造高复杂度词条组合。
4. `TRIVIAL` 不得承载 `castSpeed`、稀有 on-hit、额外 resource axis 这类高解释成本词条。
5. `affixFamily` 用于控制语义重叠；同一件物品默认不应出现两个高度重复的 family。
6. `exclusiveGroup` 用于硬互斥；同组 affix 不允许同时出现在同一件物品上。

### 4.2 Unique / Artifact 模板池

模板要求：

1. `UNIQUE`：按 zone 和 source tier 过滤模板。
2. `ARTIFACT`：只允许 `BOSS / SECRET_ZONE / special reward chest`。
3. 模板池只在 `PR-04` 已冻结的 `upgrade roll` 命中后参与，不允许绕过 eligibility 直接私有发奖。
4. 模板池最低规模：
   - `UNIQUE >= 12`
   - `ARTIFACT >= 4`
5. 最低规模是 Phase 4 主线门槛；若不满足，则对应 special tier 不进入 eligibility，且不能重置 pity 状态。
6. 覆盖门槛与数量门槛同时成立：
   - 四个 target zone 至少都各有 `1` 个可见差异模板入口
   - 至少覆盖 `2` 类不同 build archetype
   - `BOSS / SECRET_ZONE / special reward chest` 三类来源不能完全共用同一批模板语义
7. 模板必须显式声明：
   - `id`
   - `nameKey`
   - `descKey`
   - `visualKey`
   - `iconKey`
   - `audioProfile`
   - `schemaVersion`
   - `tags`
   - `allowedSourceTiers`
   - `allowedZones`

### 4.3 `lootBalanceLab`

建议任务落位：

```text
tools/src/main/kotlin/com/ktome/tools/balance/LootBalanceLabRunner.kt
build.gradle.kts
```

固定 batch 矩阵至少覆盖：

1. `NORMAL + magicFind=0.00`
2. `ELITE + magicFind=0.15`
3. `BOSS + magicFind=0.25`
4. `CHEST + magicFind=0.10`
5. `BOSS + magicFind=1.00`
6. `BOSS + magicFind=1.50`

报告最小字段：

1. `sourceLevel`
2. `sourceTier`
3. `zoneId`
4. `playerLevel`
5. `magicFind`
6. `rarityTierDistribution`
7. `affixBudgetDeviation`
8. `uniqueRate`
9. `artifactRate`
10. `specialTierEligibilityRate`
11. `rarePityActivations`
12. `uniquePityActivations`
13. `pitySuppressedByMissingTemplates`
14. `castSpeedPostDrP50`
15. `castSpeedPostDrP95`

### 4.4 `castSpeed` 接线

本 PR 明确要求：

1. 所有 affix / unique / artifact / reward buff 对 `castSpeed` 的修改，都必须走正式 DR resolver。
2. `LootBalanceLab` 必须包含 `castSpeed` 相关极端上下文，验证不会线性越界。

## 5. 推荐改动面

### 5.1 `core`

1. 新建 affix cost / unique template / artifact template runtime。
2. 把 item 生成拆成 `budget -> rarity -> template or affix roll` 的明确阶段。
3. 对 `castSpeed` 路径增加 regression test。
4. 把 `PityTracker` 接进正式 roll context，而不是只在 tools 层模拟。
5. 在 affix roll 阶段消费 `affixFamily / exclusiveGroup`，避免高成本词条退化成纯数值堆叠。

### 5.2 `game`

1. 扩展 item/affix YAML。
2. 引入 `unique` 和 `artifact` 注册表或目录。
3. zone/boss/reward chest 接到允许的 source tier。
4. 补齐最低模板量，并为模板缺失场景声明显式降级策略。
5. 模板池必须满足“数量 + 覆盖”双门槛，不允许只用大量同质化 stat stick 充数。

### 5.3 `tools`

1. 新建 `lootBalanceLab` runner。
2. root `build.gradle.kts` 暴露 alias。
3. 保存 summary JSON 和 per-roll JSONL。
4. 在 summary 中显式输出 pity 触发、模板缺失降级与 `magicFind` clamp 边界统计。

### 5.4 `tools / white-box` 补充改造

1. `lootBalanceLab` 除现有 batch summary 之外，还必须通过统一 white-box 框架输出标准四件套：
   - `tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json`
   - `tools/build/reports/phase4/whitebox/loot/whitebox-loot-cases.jsonl`
   - `tools/build/reports/phase4/whitebox/loot/whitebox-loot-report.md`
   - `tools/build/reports/phase4/whitebox/loot/artifacts/`
2. 正式 domain 名称固定为 `whiteBoxLoot`，并加入：
   - `whiteBoxVerify`
   - `phase4Report`
3. `whiteBoxLoot` 的 case artifact 至少包括：
   - rarity table
   - affix cost breakdown
   - special-tier eligibility / upgrade trace
   - pity timeline
   - `castSpeed` DR 结果表
4. AI 读取顺序固定为：
   - 先读 `whitebox-loot-summary.json`
   - 再按 join key 读 `whitebox-loot-cases.jsonl`
   - 最后跳转到对应 artifact
5. `lootBalanceLab` 与 `whiteBoxLoot` 可以复用同一 runner 内核，但不得形成“实验室一套口径、white-box 一套口径”的双报告模型。

## 6. 测试与自证

### 6.1 必测行为

1. `MAGIC / RARE` 分布偏离不超过 checklist 阈值。
2. `UNIQUE / ARTIFACT` 只在允许来源出现。
3. `magicFind > 1.0` 被 clamp，且 `1.50` 与 `1.00` 的分布差异落在 checklist 容忍区间内。
4. `castSpeed` affix 经过 DR 后仍在合理区间。
5. pity 只在达到阈值且最终成功产出目标稀有度时重置；模板缺失降级不重置。
6. `TRIVIAL` 只承担预算尾差，不出现双 `TRIVIAL` 叠加逃逸。
7. `exclusiveGroup` 冲突不会在最终结果阶段被动裁剪，而是在候选集阶段被排除。

### 6.2 自动化命令

```bash
./gradlew :core:test
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew whiteBoxVerify
./gradlew phase4Report
```

### 6.3 白盒验证

1. 至少观察一次 `UNIQUE` 掉落和一次 `ARTIFACT` 奖励。
2. 人工核对 inspect 面板的模板描述、icon 和音频 cue 是否匹配模板来源。
3. 人工抽查一次 pity 生效后的掉落日志，确认报告字段、实际结果和 pity 重置时机一致。

### 6.4 统一白盒框架验证

1. `whiteBoxLoot` 必须自动断言：
   - `MAGIC / RARE / UNIQUE / ARTIFACT` 分布满足 checklist 容差
   - `UNIQUE / ARTIFACT` 只出现在允许来源
   - `magicFind=1.50` 与 `magicFind=1.00` 在 clamp 后分布一致到统计容差内
   - pity 激活与重置逻辑可追溯
   - `castSpeed` affix 经过 DR 后不线性越界
2. aggregate report 至少输出：
   - `rarityTierDistribution`
   - `affixBudgetDeviation`
   - `specialTierEligibilityRate`
   - `rarePityActivations / uniquePityActivations`
   - `pitySuppressedByMissingTemplates`
3. `whiteBoxLoot` 的失败 case 必须能反查到：
   - `sourceLevel`
   - `sourceTier`
   - `zoneId`
   - `playerLevel`
   - `magicFind`
   - seed 或 corpus entry id

## 7. 资源生成计划

### 7.1 图片

1. 计划文件：`assets-src/image/specs/phase4-pr05-gemini-plan.yaml`
2. 覆盖对象：
   - `item.unique.*.icon`
   - `item.artifact.*.icon`
   - `item.unique.*.visual`
   - `item.artifact.*.visual`
3. 报告文件：
   - `assets-src/image/manifests/phase4-pr05-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-pr05-processing-report.jsonl`
4. 管线固定为：
   - `scripts/generate_assets.sh assets-src/image/specs/phase4-pr05-gemini-plan.yaml ...`
   - `scripts/process_assets.py`
   - `./gradlew assetLint styleLint manifestLint`
5. `*-gemini-plan.yaml` 是沿用现有图片生成管线的历史命名，不代表引入新的 manifest 或新的模型绑定 contract；本 PR 只复用既有 spec 文件命名约定。

### 7.2 音频

1. 计划文件：`assets-src/audio/specs/phase4-pr05-audio-plan.yaml`
2. 覆盖对象：
   - `audio.item.unique.*`
   - `audio.item.artifact.*`
3. 报告文件：
   - `assets-src/audio/manifests/phase4-pr05-processing-report.jsonl`
4. 管线固定为：
   - raw 放在 `assets-src/audio/raw`
   - `scripts/process_audio.py --filter-plan assets-src/audio/specs/phase4-pr05-audio-plan.yaml`
   - `./gradlew audioLint`

### 7.3 约束

1. 本 PR 新资源只追加到现有 canonical JSON manifest 体系，不新增平行 manifest 版本。
2. `build.gradle.kts` 中 `assetLint / styleLint / manifestLint / audioLint` 必须追加本 PR 对应 `--extra-plan`。
3. deterministic placeholder 仅允许用于临时音频占位；任何玩家可稳定获取的 `ARTIFACT` 正式发版前必须替换成真实素材。
4. 资源计划文件名中的 `gemini` 仅是资产生成脚本的 plan 入口命名，不得被实现方误读为运行时资源格式或客户端依赖。

## 8. 出口门禁

1. affix cost、template pool 和 `lootBalanceLab` contract 冻结。
2. `UNIQUE / ARTIFACT` 可生成、可验证、可追溯。
3. 资源计划与现有 manifest/lint/process 管线对齐。
4. pity、模板缺失降级和 `magicFind` clamp 都能在实验室报告里直接解释。
5. `whiteBoxLoot` 已接入统一 white-box 框架，并能被 AI 与人类共同消费。
