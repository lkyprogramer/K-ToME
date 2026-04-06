# Phase 4 PR-05 深度审查报告

## 审查结论

当前改造已经覆盖了 `PR-05` 的大部分交付面：`AffixCost`、`UNIQUE / ARTIFACT` 模板池、`lootBalanceLab` root alias、`whiteBoxLoot`、`phase4Report` 聚合、`castSpeed` DR 接线、`PityTracker` 存档与报告字段、资源计划与 lint 管线都已经落地。

但按 `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md` 的正式口径审查，当前实现还不能算“完全一致”。阻塞点有两个：

1. `affixBudgetDeviation` 指标被实现成恒为 `0`，导致 `lootBalanceLab / whiteBoxLoot / phase4Report` 在预算偏差维度上失去门禁意义。
2. `ARTIFACT` 的非 Boss 来源合同没有被建模清楚，也没有被实验室正确覆盖：运行时把 `SUPPORT` 也放进了 artifact 路径，但 batch / white-box 同时又把 `CHEST` artifact 视为非法，导致“special reward chest”这条正式口径既过宽又漏验。

结论上，这一批改造适合判定为：

- 合同主体已落地
- 门禁与来源语义尚未收口
- 需要一次小范围修正才能达到 PR-05 文档要求

## 审查范围与证据

审查对象：

- 当前 `git diff`
- `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
- `docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md`
- `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`

已读取的关键实现面：

- `core/src/main/kotlin/com/ktome/core/item/*`
- `core/src/main/kotlin/com/ktome/core/loot/*`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/data/*`
- `game/src/main/resources/data/items/index.yaml`
- `tools/src/main/kotlin/com/ktome/tools/loot/*`
- `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`
- `build.gradle.kts`
- `tools/build.gradle.kts`
- `tools/build/reports/phase4/loot/loot-balance-summary.json`
- `tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json`
- `tools/build/reports/phase4/phase4-summary.json`

本次实际执行命令：

- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew lootBalanceLab`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew whiteBoxLoot`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew phase4Report`

结果说明：

- 三个命令均返回 `BUILD SUCCESSFUL`
- 本次输出为 `UP-TO-DATE`
- 我进一步直接读取了现有 report artifact 内容做核查

## Findings

### P1

#### 1. `affixBudgetDeviation` 被写空，导致预算偏差门禁实际失效

证据：

- `core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt:157-162`
  - `budgetTarget = budgetConsumed`
- `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt:566-578`
  - `affixBudgetDeviationRatio = affixBudgetDeviation / affixBudgetTarget`
- `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt:500-521`
  - 预算偏差阈值判断直接依赖上述 ratio
- `tools/build/reports/phase4/loot/loot-balance-summary.json:96-99`
  - `greenwood_normal_mf000` 的 `average=0.0, p95=0.0`
- `tools/build/reports/phase4/loot/loot-balance-summary.json:287-290`
  - `abyssal_chest_mf010` 的 `average=0.0, p95=0.0`

问题本质：

- `budgetTarget` 应该表达“预算目标”，即 `lootRoll.budget.affixBudget`
- 现在却被赋值成了 `budgetConsumed`
- 因此 `affixBudgetDeviation = budgetTarget - budgetConsumed = 0`
- 结果是 6 个矩阵的预算偏差统计全部变成 `0.0`

这和文档要求直接冲突：

- PR-05 要求 `affixBudget` 平均偏离不超过 `±5%`，`P95` 不超过 `±12%`
- 当前实现不是“偏离很小”，而是“指标永远为 0，等价于没有测到”

偏差程度：

- 预算偏差维度的有效检测覆盖率当前是 `0 / 6` 矩阵
- `phase4Report` 与 `whiteBoxLoot` 也因此继承了同一个假阳性 PASS

修复建议：

1. 把 `AffixSelectionResult.budgetTarget` 改为真实预算目标 `budget`
2. 保留 `budgetConsumed`
3. 把 `affixBudgetDeviation` 定义为 `max(budgetTarget - budgetConsumed, 0)`
4. 在 `ItemGenerator.traceFor()` 中继续透传 `rawAffixBudgetShortfall`
5. 为 `AffixGeneratorTest / ItemGeneratorTest / LootBalanceLabRunnerTest / WhiteBoxLootRunnerTest` 增加断言：
   - `budgetTarget == lootRoll.budget.affixBudget`
   - 至少一部分样本的 `affixBudgetDeviation > 0`
   - 修复后 summary 不再出现 6 个矩阵全是 `0.0`

#### 2. `ARTIFACT` 的非 Boss 来源合同没有收口，且自动化没有覆盖“special reward chest”正式路径

证据：

- 文档要求：
  - `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
  - `ARTIFACT` 只允许来自 `BOSS / SECRET_ZONE / special reward chest`
- 默认规则：
  - `core/src/main/kotlin/com/ktome/core/loot/LootBudgetResolver.kt:221-226`
  - `SourceTier.CHEST` 默认只允许 `UNIQUE`
- 运行时 override：
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:3099-3112`
  - `CACHE` 和 `SUPPORT` 都被放进 `UNIQUE + ARTIFACT`
- 实际交互来源：
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:4067-4080`
    - `CACHE` 奖励走 `MilestoneRewardSource.CACHE`
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:4109-4123`
    - `SUPPORT` 奖励走 `MilestoneRewardSource.SUPPORT`
- 实验室判定：
  - `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt:523-527`
    - `SourceTier.CHEST` 一旦出现 artifact 就判失败
  - `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt:149-156`
    - white-box 也把 `CHEST -> artifactRate == 0` 当成硬断言
- 现有 summary：
  - `tools/build/reports/phase4/loot/loot-balance-summary.json:279-292`
  - `abyssal_chest_mf010` 的期望和观测都强制为 `artifactRate = 0.0`
- 但模板池门槛同时要求 chest-only artifact：
  - `tools/build/reports/phase4/loot/loot-balance-summary.json:39-46`
  - `chestOnlyArtifactTemplateCount = 2`

问题本质：

- 当前实现把“special reward chest”这个正式来源概念拆散了：
  - 默认 `CHEST` 不允许 artifact
  - `CACHE` 和 `SUPPORT` 再通过 override 临时打开 artifact
- 这带来两个问题：
  1. `SUPPORT` 是否等于文档里的 `special reward chest` 并不清楚，语义过宽
  2. `lootBalanceLab / whiteBoxLoot` 根本没有覆盖非 Boss artifact 的正式发放路径

偏差程度：

- PR-05 范围内已实现的 artifact 来源应至少有两条可验证路径：
  - `BOSS`
  - `special reward chest`
- 当前自动化只真实验证了 `BOSS`
- 非 Boss artifact 自动化覆盖率是 `0 / 1`
- 如果 `SUPPORT` 不被视为 special reward chest，则当前运行时来源集合比文档多出 `1` 条非法来源

修复建议：

1. 不要继续用 `SourceTier.CHEST + override` 隐式表达 special reward chest
2. 二选一收口：
   - 方案 A：新增显式来源语义，例如 `RewardSourceKind.SPECIAL_REWARD_CHEST`
   - 方案 B：保留 `CHEST`，但在 `LootRollContext` / `SpecialTierEligibility` 里增加明确布尔或枚举字段区分“普通 chest”与“special reward chest”
3. 如果 `SUPPORT` 不是 special reward chest，立即从 artifact eligibility 里移除
4. `lootBalanceLab` 至少新增一个 special reward chest 矩阵
5. `whiteBoxLoot` 增加对应 case assertion，验证：
   - special reward chest 可以出 artifact
   - 普通 `CHEST` 不能出 artifact

### P2

#### 3. loot report 的 `locale` 元数据与实际加载 locale 不一致，破坏可复现性头信息

证据：

- `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt:347-350`
  - runner 用 `DataLoader(GameLocale.EN_US)`
- `tools/src/main/kotlin/com/ktome/tools/mapgen/Phase4HarnessHeaderFactory.kt:9-19`
  - header 默认写 `GameLocale.DEFAULT.id`
- `tools/build/reports/phase4/loot/loot-balance-summary.json:1-18`
  - header 中 `locale = "zh-CN"`

问题本质：

- 复现合同要求 header 记录真实 locale
- 当前 loot kernel 实际以 `en-US` 载入数据，但报告头写成 `zh-CN`
- 即使这批统计主要依赖 ID / 权重，不直接依赖本地化文本，这个 header 仍然不可信

偏差程度：

- 当前 loot 实验室与 white-box 的 locale 元数据错误率是 `100%`

修复建议：

1. 在 `LootBalanceLabRunner` 和 `WhiteBoxLootRunner` 中显式把 runner 使用的 locale 传给 `phase4HarnessHeader`
2. 或者直接统一按 header locale 初始化 `DataLoader`
3. 给 `LootBalanceLabRunnerTest` 增加断言：summary header locale 必须等于 kernel 实际使用 locale

#### 4. `AffixGenerator` 已不再消费随机源，affix roll 从“加权抽样”退化为“确定性最优解”

证据：

- `core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt:95-99`
  - 构造器仍注入 `RandomSource`
- 同文件全文没有任何 `random.` 调用
- `core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt:133-163`
  - 现在通过 `selectBestAffixes + isBetterSelection` 做穷举最优选择

问题本质：

- 从玩法体验看，这会把 affix roll 的随机自由度降到 `0`
- 同一 budget / slot / tag context 下，词缀组合会稳定坍缩到同一组“最优答案”
- 这和 `Phase 4` 在 roadmap 中强调的“掉落与遭遇差异可感知且可量化”方向不一致
- `AffixTagWeighting` 名义上是 weighting，当前却只参与 deterministic score，不再参与 roll

偏差程度：

- affix 选择层的随机性当前是 `0`
- 实验室里的 affix histogram 更像“优化器输出分布”，不是“掉落 roll 分布”

修复建议：

1. 保留当前 budget feasibility / blacklist / family / exclusiveGroup 过滤
2. 但在“可行候选集合”内恢复加权随机抽样
3. 如果担心解释性下降，可以：
   - 先做候选裁剪
   - 再按 `AffixTagWeighting` 做 weighted roll
   - 把权重、seed、候选集写入 trace
4. 至少补一个 regression test，证明不同 seed 能在同一 context 下得到不同但合法的 affix 组合

## Requirement Alignment

### 1. `AffixCost` 体系与固定成本带

- 要求：
  - 冻结 `TRIVIAL / MINOR / MEDIUM / MAJOR / SIGNATURE = 1 / 3 / 6 / 10 / 14`
  - affix 需要显式成本、family、exclusiveGroup
- 证据：
  - `core/src/main/kotlin/com/ktome/core/loot/AffixCostModels.kt`
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt`
  - `game/src/main/resources/data/items/index.yaml`
  - `game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt`
- 结论：**一致**

### 2. `MAGIC / RARE` affix 数量与 budget 约束

- 要求：
  - `MAGIC` 为 `1~2`
  - `RARE` 为 `2~4`
  - 总成本不超过 `affixBudget`
- 证据：
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt`
  - `core/src/main/kotlin/com/ktome/core/item/AffixGenerator.kt`
- 结论：**部分一致**

说明：

- 数量与 budget cap 本身已实现
- 但 budget 偏差统计因为 Finding 1 被写空，导致“是否贴近预算目标”无法被实验室真正验证

### 3. `UNIQUE / ARTIFACT` 模板池进入正式路径，不允许模板缺失后运行时降级

- 要求：
  - special tier 只能走 `eligibility + upgrade roll + template pool`
  - 模板缺失必须 fail-fast，不允许命中后静默降级
  - 最低模板量 `UNIQUE >= 12`、`ARTIFACT >= 4`
- 证据：
  - `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt`
  - `core/src/main/kotlin/com/ktome/core/item/ItemGenerator.kt`
  - `game/src/main/resources/data/items/index.yaml`
  - `game/src/test/kotlin/com/ktome/game/data/ZoneContentCoverageTest.kt`
  - `core/src/test/kotlin/com/ktome/core/item/ItemGeneratorTest.kt`
- 结论：**部分一致**

说明：

- 模板池、fail-fast、数量门槛都在
- 但 artifact 来源建模与自动化覆盖没有收口，见 Finding 2

### 4. `castSpeed` 词条必须走正式 DR 路径

- 要求：
  - 所有 affix / unique / artifact / reward buff 对 `castSpeed` 的修改都必须走 `DR_CAST_SPEED_C`
- 证据：
  - `core/src/main/kotlin/com/ktome/core/combat/DiminishingReturns.kt`
  - `core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt`
  - `core/src/main/kotlin/com/ktome/core/item/ItemGenerator.kt`
  - `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt`
  - `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt`
- 结论：**一致**

### 5. `PityTracker` 必须接进正式生成链和实验室报告

- 要求：
  - 不能只停留在 schema
  - 必须进入 runtime / save / report
- 证据：
  - `core/src/main/kotlin/com/ktome/core/loot/LootBudgetResolver.kt`
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
  - `core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt`
  - `tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt`
- 结论：**一致**

### 6. `lootBalanceLab`、`whiteBoxLoot`、`phase4Report` root alias 与标准报告产物

- 要求：
  - `./gradlew lootBalanceLab`
  - `./gradlew whiteBoxLoot`
  - `./gradlew whiteBoxVerify`
  - `./gradlew phase4Report`
  - `whiteBoxLoot` 进入统一 white-box 框架
- 证据：
  - `build.gradle.kts:188-209`
  - `tools/build.gradle.kts:115-160`
  - `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt:28-46`
  - `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt:80-110`
- 结论：**一致**

说明：

- 接线已经到位
- 但报告里的预算偏差和 artifact 来源覆盖仍有缺陷，因此“入口一致”不等于“验证充分”

### 7. 资源计划与现有 manifest/lint/process 管线对齐

- 要求：
  - 图像计划、音频计划、manifest report、extra-plan 接线要落到既有管线
- 证据：
  - `build.gradle.kts` 已追加 `phase4-pr05-gemini-plan.yaml` / `phase4-pr05-audio-plan.yaml`
  - `assets-src/image/specs/phase4-pr05-gemini-plan.yaml`
  - `assets-src/audio/specs/phase4-pr05-audio-plan.yaml`
  - 对应 `generation-report / processing-report` 文件已存在于工作区
- 结论：**一致**

### 8. inspect 面板模板描述 / icon / 音频 cue 是否与模板来源一致

- 要求：
  - 至少人工核对一次 `UNIQUE` 和一次 `ARTIFACT`
- 证据：
  - 代码侧已接入 item visual/icon/audio schema
  - 但本次没有实际跑 `:client:run` 做人工白盒
- 结论：**无法判断**

## Removal/Iteration Plan

- 没有发现可以“立刻安全删除”的实现。
- 当前问题属于合同修正，不属于单纯删遗留代码。

建议的 staged iteration 只有一项：

### Item: 收口 non-boss artifact 来源建模

| Field | Details |
| --- | --- |
| Location | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:3099-3112`、`core/src/main/kotlin/com/ktome/core/loot/LootBudgetResolver.kt:221-226`、`tools/src/main/kotlin/com/ktome/tools/loot/*` |
| Phase/Work Package | `Phase 4 / P4-W3b` |
| Touched contract | `LootBudget`、`SpecialTierEligibility`、`lootBalanceLab`、`whiteBoxLoot` |
| Evidence | 默认 `CHEST` 禁 artifact，但 runtime override 给了 `CACHE + SUPPORT` artifact；lab 又把 `CHEST` artifact 判失败 |
| Preconditions | 先明确“special reward chest”在 runtime 的正式表达方式 |
| Deletion or iteration steps | 1. 明确来源模型 2. 删除临时 override 歧义 3. 增加 special reward chest matrix 4. 更新 white-box 断言 5. 回写 PR-05 文档与 checklist 术语 |
| Affected harness/gates | `lootBalanceLab`、`whiteBoxLoot`、`phase4Report`、相关 `FoundationGameSessionTest` |
| White-box check | 手动验证一次 cache/support 奖励与 boss 奖励，确认 artifact 只出现在文档允许来源 |
| Rollback or fallback | 若短期不拆来源模型，至少先把 `SUPPORT` 排除出 artifact，并在 lab 中单独建 special reward chest scenario |

## Additional Suggestions

1. 给 `AffixSelectionResult` 增加不变量断言：`budgetTarget >= budgetConsumed`。
2. 在 `LootBalanceLabRunnerTest` 中增加一条“修复后预算偏差不能 6 个矩阵全为 0”的测试，避免门禁再次空转。
3. 在 `FoundationGameSessionTest` 中补一条负向用例：
   - 普通 chest 不得出 artifact
   - special reward chest 才允许 artifact
4. 如果保留 special template 与 base item 的双份展示字段，建议在 runtime loader 阶段直接做一致性校验，而不是只依赖 contract lint。

## Suggested Verification

### 已执行

- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew lootBalanceLab`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew whiteBoxLoot`
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env >/dev/null && ./gradlew phase4Report`

说明：

- 三个任务本次都命中了 `UP-TO-DATE`
- 我已直接检查对应产物：
  - `tools/build/reports/phase4/loot/loot-balance-summary.json`
  - `tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json`
  - `tools/build/reports/phase4/phase4-summary.json`

### 修复 Finding 1 / 2 后必须重跑

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :core:test --tests "com.ktome.core.item.AffixGeneratorTest" --tests "com.ktome.core.item.ItemGeneratorTest"
./gradlew :game:test --tests "com.ktome.game.data.ZoneContentCoverageTest" --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew phase4Report
```

### 资源与管线复核

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
```

### 建议补做的人工白盒

1. `./gradlew :client:run`
2. 手动触发一次 `CACHE` 奖励、一处 `SUPPORT` 奖励和一次 Boss 奖励。
3. 确认：
   - 普通 chest / support 是否会错误地产生 artifact
   - `UNIQUE / ARTIFACT` inspect 面板的描述、icon、音频 cue 是否与模板来源一致
   - 物品品质标签是否会误导玩家理解特殊 tier

## Summary

这批 PR-05 改造已经把主框架搭起来了，离“验收完成”不远，但还差最后一层合同收口。

目前最关键的不是继续补内容，而是先修门禁可信度：

1. 修正 `affixBudgetDeviation` 指标，让实验室重新具备真实判定力。
2. 明确 non-boss artifact 的正式来源语义，并把 special reward chest 路径纳入实验室与 white-box。

在这两个问题修完之前，当前 `PASS` 只能说明“流程通了”，不能说明“PR-05 文档要求已被严格满足”。
