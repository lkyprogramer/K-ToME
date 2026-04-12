> 执行前必须先完整阅读并接受：
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-01-experience-metrics-baseline-and-phase4-report-expansion.md`

# Phase 4 - V2OPT PR-01 Experience Gate 校正与 Owner Metric 硬化

**阶段**: `Phase 4 / Post-Review Follow-up / V2OPT-W1`  
**优先级**: `P0`  
**工作量评估**: `L`（`3~4` 人日）  
**前置条件**: 当前 `phase4Report / whiteBoxLoot / whiteBoxHiddenContent / terrainInteractionBatch / longRunLab` 全部能稳定产出 artifact  
**对应问题**:

1. scripted hidden correctness 被误读为 organic experience
2. reward 差异只看 corpus average，局部 secret/reward 高重叠被掩盖
3. long-run 报表没有把“职业身份被单一底盘吞并”显式暴露为 owner metric
4. terrain 只有 aggregate gate，没有 combat-sampled zone 下限和口径说明

---

## 1. 阶段目标

把 `Phase 4` 当前仍然会误导决策的体验 gate 收口成单一正式口径，让后续 `V2OPT-PR-02 ~ 05` 都建立在可信指标上，而不是继续依赖审查文字人工解释。

完成标准：

1. `phase4Report` 正式区分 scripted verification 与 organic experience。
2. loot 报表新增 same-zone local overlap guardrail，不再只保留 corpus average。
3. long-run 报表新增职业身份 owner metric，能直接暴露 terminal weapon convergence。
4. terrain 报表新增 per-zone lower bound，并显式写明 combat-sampled zone 范围与排除理由。
5. 所有新增指标都有 owner、公式、阈值、输出位置和 FAIL 语义；后续 PR 禁止私自重定义。

---

## 2. 工作量评估与整合结论

### 2.1 为什么这是一个独立 PR

这个 PR 必须在任何内容调优前完成，因为它修的是“观察系统”，不是“被观察对象”。

若把它拆散到后续 PR：

1. `PR-02` 会继续在错误 long-run 指标上调分发。
2. `PR-03` 会继续拿 scripted hidden rate 充当体验证据。
3. `PR-04` 会继续让 terrain aggregate pass 掩盖单区离群。

### 2.2 为什么这里不直接做内容调优

本 PR **不**处理：

1. `items/index.yaml`、`loot/index.yaml` 的实质性平衡修改
2. `events/index.yaml`、`secret-zones/index.yaml` 的 organic tuning
3. `elites/index.yaml`、`boss-variants/index.yaml` 的语义调整

原因：

1. 一旦本 PR 混入内容调优，指标合同会和具体 tuning 相互污染。
2. 这一轮的目标是建立“后续每个 PR 必须对谁负责”的 owner gate。

---

## 3. 当前问题拆解

### 3.1 hidden 指标的问题

当前 `HiddenContentHarnessRunner.kt` 明确通过 `primerAction` 驱动场景：

1. `FORCE_ELITE_KILL`
2. `ENTER_HIDDEN_BRANCH_ROOM`
3. `OPEN_CRYSTAL_CACHE_CHEST`
4. `CLAIM_WARD_RELIQUARY`

这证明的是：

1. `SearchAction` typed contract 正确
2. proof 与 runtime 一致
3. reveal / reward / return bridge 正式路径成立

它**不**证明：

1. 玩家在自然跑图时会主动搜
2. 玩家会自然触发 primer
3. 玩家会愿意进 secret zone

### 3.2 loot 指标的问题

当前 `WhiteBoxLootRunner` 的主判断集中在：

1. `lootProfileAverageBaseItemOverlap`
2. `lootProfileMaxBaseItemOverlap`
3. `lootProfileDistinctBaseItemCount`

但真正伤体验的是 same-zone 高价值通道，例如：

1. `abyssal_temple.cadence ↔ abyssal_temple_warded_archive.secret`
2. `deep_iron_pit.reward ↔ deep_iron_slag_cache.secret`
3. `deep_iron_pit.reward ↔ deep_iron_smuggler_stash.secret`

这些 pair 需要单独被 owner 指标捕捉。

### 3.3 long-run 指标的问题

当前 `build/reports/harness/long-run-full.md/json` 已经暴露了 terminal convergence，但没有正式 gate：

1. full-route 共 `12` 个样本
2. `arcanist / rogue / templar / vanguard` 终盘主武器全部是 `battle_axe`

如果不把这件事变成机器可判定指标，后续任何 PR 都可能“看起来没问题，实际上继续加深收敛”。

### 3.4 terrain 指标的问题

当前 `terrainInteractionBatch` 已明确自己只对 combat-sampled zone 负责：

1. `greenwood_fringe`
2. `deep_iron_pit`
3. `underground_river`
4. `crystal_cavern`

其中 `abyssal_temple` 被排除的原因是“objective/pressure-driven，常规战斗分母会塌缩”。  
这个约束本身没有问题，问题在于：

1. 它没有被写成正式报告口径
2. 只有 aggregate，没有 per-zone lower bound
3. 容易被后续审查误读成“升级 zone 漏采样”

---

## 4. 本 PR 必须冻结的合同

1. `phase4Report` 成为 Phase 4 体验指标的唯一 owner。
2. hidden 必须拆成：
   - `scriptedHiddenVerification*`
   - `organicHiddenExperience*`
3. loot 必须同时保留：
   - corpus average
   - pairwise local guardrail
4. long-run 必须正式输出 terminal identity 指标。
5. terrain 必须显式声明 combat-sampled zone contract，后续不再靠 review 文本解释。
6. 本 PR 不允许通过“调阈值让现状变绿”来掩盖问题；新增指标首次 FAIL 是允许的。

---

## 5. 范围与非目标

### 5.1 范围

1. `phase4Report` 指标目录、markdown、JSON 输出
2. `WhiteBoxLootRunner` local overlap 指标
3. `WhiteBoxHiddenContentRunner` / `HiddenContentHarnessRunner` 的 scripted vs organic 切分
4. `LongRunLabFullTest` / `long-run-full.json` 的职业身份指标
5. `TerrainInteractionBatchTest` 的 per-zone lower bound 与 sample contract 输出

### 5.2 非目标

1. 不调 loot 平衡
2. 不调 hidden 内容数据
3. 不调 elite / terrain 权重
4. 不做 client 可视化抛光

---

## 6. 技术方案

### 6.1 建立统一 Metric Catalog

建议新增：

```text
tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt
```

定义一个正式目录，而不是继续把阈值散落在 `Phase4ReportRunner.kt`。

建议结构：

```kotlin
data class Phase4MetricSpec(
    val id: String,
    val ownerTaskId: String,
    val sourcePath: String,
    val formula: String,
    val targetText: String,
    val decisionNotes: String,
)
```

必须收口的 metric：

1. `scriptedHiddenVerificationRate`
2. `organicHiddenDiscoveryRate`
3. `sameZoneSecretVsCadenceMaxOverlap`
4. `sameZoneSecretVsRewardMaxOverlap`
5. `terminalWeaponBaseDiversity`
6. `crossProfessionTopWeaponDominance`
7. `professionAlignedWeaponAdoptionRate`
8. `terrainInteractionEncounterRate.aggregate`
9. `terrainInteractionEncounterRate.per_zone_lower_bound`

### 6.2 Loot local overlap owner metric

建议落点：

```text
tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt
tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt
```

新增输出：

1. `sameZoneSecretVsCadencePairs`
2. `sameZoneSecretVsRewardPairs`
3. `sameZoneSecretVsCadenceMaxOverlap`
4. `sameZoneSecretVsRewardMaxOverlap`
5. `localIdentityFailurePairs`

配对规则：

1. 仅比较同 zone
2. `.secret` 仅与同 zone 的 `.cadence`、`.reward` 比较
3. 不再把跨 zone 平均值拿来代替 local identity

新增断言：

1. `loot.aggregate.same_zone_secret_cadence_guardrail`
2. `loot.aggregate.same_zone_secret_reward_guardrail`

### 6.3 Long-run identity owner metric

建议落点：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt
```

不要从 markdown 反解析，直接消费：

```text
build/reports/harness/long-run-full.json
```

新增指标：

1. `terminalWeaponBaseDiversity`
2. `crossProfessionTopWeaponDominance`
3. `professionAlignedWeaponAdoptionRate`
4. `professionTerminalWeaponDistribution`

定义建议：

1. `terminalWeaponBaseDiversity`
   - full-route 样本终盘主武器 base id 的 distinct count
2. `crossProfessionTopWeaponDominance`
   - 全部 full-route 样本中最常见主武器占比
3. `professionAlignedWeaponAdoptionRate`
   - 每个 profession 的终盘主武器是否落在其允许 archetype 集中

profession-aligned archetype 第一版固定：

1. `vanguard` -> `battle_axe / long_sword / shield-forward kit`
2. `templar` -> `long_sword / battle_axe / holy-frontline kit`
3. `rogue` -> `short_sword / hunter_bow / dagger-like fast weapon kit`
4. `arcanist` -> `arcane_staff / caster-tagged weapon kit`

注意：

1. 这里先冻结指标和规则，不在本 PR 调具体分发。
2. `battle_axe` 当前会大面积 FAIL，这正是预期。

### 6.4 hidden scripted / organic 拆分

建议落点：

```text
tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt
tools/src/main/kotlin/com/ktome/tools/hidden/WhiteBoxHiddenContentRunner.kt
tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt
```

拆分为两条任务：

1. `hiddenContentHarness`
   - 继续是 scripted correctness owner
   - 明确输出 `scriptedVerification = true`
2. `organicHiddenProbe`
   - 禁用 primerAction
   - 基于真实 bot/pathing 采集：
     - first hidden discovery turn
     - search action use count
     - secret zone entry count
     - discovery without primer count

约束：

1. `organicHiddenProbe` 第一版不调内容，只读现状。
2. 允许初始 FAIL。
3. 不允许借助 `HiddenPrimerAction` 或内部直接 reveal API。
4. 导航与搜索决策只允许消费 `RunObservation` / `RenderSnapshot` 已暴露的可见线索，不允许读取 hidden entrance / search point ground truth。

### 6.5 terrain per-zone lower bound

建议落点：

```text
game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt
tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt
```

新增输出：

1. `combatSampledZoneIds`
2. `combatSampledZoneExclusionNotes`
3. `perZoneEncounterLowerBoundTarget`
4. `perZoneEncounterFailures`

第一版 lower bound：

1. 对 combat-sampled zone 增加统一 lower bound
2. 不把 `abyssal_temple` 强塞进同一口径
3. 必须把 `crystal_cavern` 为何存在写清楚

### 6.6 phase4Report 输出收口

必须新增：

1. “指标 owner 表”
2. “local reward identity” 段
3. “terminal build identity” 段
4. “scripted vs organic hidden” 段
5. “terrain combat sample contract” 段

并且：

1. 每个段都要有 source task
2. 每个 FAIL 都要给出解释 note

---

## 7. 推荐改动面

### 7.1 `tools`

1. [Phase4ReportRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt)
2. [WhiteBoxLootRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt)
3. [LootBalanceLabRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/loot/LootBalanceLabRunner.kt)
4. [HiddenContentHarnessRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt)
5. `OrganicHiddenProbeRunner.kt`（新增）
6. [Phase4ReportRunnerTest.kt](/Users/luo/Documents/github/K-ToME/tools/src/test/kotlin/com/ktome/tools/phase4/Phase4ReportRunnerTest.kt)

### 7.2 `game` harness / tests

1. [LongRunLabFullTest.kt](/Users/luo/Documents/github/K-ToME/game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt)
2. [TerrainInteractionBatchTest.kt](/Users/luo/Documents/github/K-ToME/game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt)

---

## 8. 实施顺序

### Task 1：Metric Catalog 与 `phase4Report` 骨架收口

- **位置**：`tools/src/main/kotlin/com/ktome/tools/phase4/*`
- **目标**：把 metric owner 和阈值从 `Phase4ReportRunner` 内联逻辑里抽出来
- **验收**：
  - `phase4-summary.json` 每个新增指标都有 owner/taskId
  - `Phase4ReportRunnerTest` 更新

### Task 2：loot local overlap guardrail

- **位置**：`WhiteBoxLootRunner.kt`, `LootBalanceLabRunner.kt`
- **目标**：输出 same-zone secret/cadence/reward 局部冲突
- **验收**：
  - local pair 列表进入 summary
  - FAIL 能指出具体 profile pair

### Task 3：long-run terminal identity metric

- **位置**：`LongRunLabFullTest.kt`, `Phase4ReportRunner.kt`
- **目标**：让职业身份问题成为正式机器可读输出
- **验收**：
  - `long-run-full.json` 或其 phase4Report 聚合段包含 4 个 identity 指标

### Task 4：hidden scripted / organic 拆分

- **位置**：`HiddenContentHarnessRunner.kt`, `OrganicHiddenProbeRunner.kt`
- **目标**：把 correctness 与 experience 分成两条路径
- **验收**：
  - `organicHiddenProbe` 有独立 artifact
  - report 能并排展示 scripted vs organic

### Task 5：terrain per-zone lower bound 与 sample contract

- **位置**：`TerrainInteractionBatchTest.kt`, `Phase4ReportRunner.kt`
- **目标**：把 combat-sampled zone contract 正式写进报告
- **验收**：
  - sample zone 列表和 exclusion note 可读
  - per-zone lower bound 明确

---

## 9. 资源生成计划

### 9.1 图片

本 PR 不新增图片资源。

### 9.2 音频

本 PR 不新增音频资源。

### 9.3 约束

1. `phase4Report / whiteBox* / terrainInteractionBatch / longRunLab` 的改动只允许触碰 markdown、JSON、metric catalog 和 gate 文本，不得引入新的 `visualKey / audioProfile`。
2. 若实现方认为 report 需要 icon、badge 或 cue，默认打回 `V2OPT-PR-05`；本 PR 不拥有任何前台资源 owner。
3. 本 PR 不新增 `assets-src/image/specs/*`、`assets-src/audio/specs/*`、`generation-report.jsonl` 或 `processing-report.jsonl`。

---

## 10. 测试策略

### 9.1 自动化命令

```bash
./gradlew whiteBoxLoot
./gradlew whiteBoxHiddenContent
./gradlew terrainInteractionBatch
./gradlew longRunLab
./gradlew phase4Report
```

### 9.2 必测行为

1. `phase4Report` 能聚合新 task / 新 metric，不破坏已有 12 个 task。
2. `sameZoneSecretVsCadenceMaxOverlap` 与 `sameZoneSecretVsRewardMaxOverlap` 能稳定输出。
3. `long-run-full.json` 的 terminal identity 指标可读且 deterministic。
4. `organicHiddenProbe` 在禁用 primer 的条件下仍能运行结束，不允许崩溃或偷用 scripted path。
5. `terrainInteractionBatch` 对 sample contract 的说明进入 summary。

### 9.3 白盒检查

1. 打开 `phase4-summary.md`，应能直接看出：
   - 哪些 hidden 指标是 scripted
   - 哪些 reward pair 在破坏 local identity
   - terminal weapon dominance 是否存在
   - terrain sample scope 是什么

---

## 11. 出口门禁

1. `phase4-summary.md/json` 新增 owner metric 目录。
2. hidden 分成 scripted 与 organic 两条正式指标。
3. loot local overlap guardrail 正式进入 report。
4. long-run terminal identity 正式进入 report。
5. terrain combat-sampled zone contract 正式进入 report。

---

## 12. 风险与 Gotchas

1. **不要从 markdown 反解析 long-run 结果**  
   必须直接消费 `long-run-full.json`。
2. **不要把 `organicHiddenProbe` 写成换皮 primer harness**  
   任何直接调用 primer/reveal 的路径都算失败。
3. **不要为了让 report 更绿而放宽 local overlap 阈值**  
   这个 PR 的职责是暴露问题，不是掩盖问题。
4. **不要把 `terrainInteractionBatch` 的 sample scope 改成“所有升级 zone”**  
   这会重新引入错误口径。

---

## 13. 回滚策略

1. 若 `organicHiddenProbe` 在本 PR 周期内无法稳定完成，可先保留 task shell 和 JSON schema，但必须输出 `UNIMPLEMENTED` 而不是假数据。
2. 若 `long-run-full.json` 聚合过重，可先只在 `phase4Report` 读取必要字段，不重写 long-run harness 主体。
