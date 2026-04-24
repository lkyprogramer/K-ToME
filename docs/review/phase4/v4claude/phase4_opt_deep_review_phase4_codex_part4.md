# Phase 4 深度玩法体验审阅 — Part 4：优化建议 + 可延后项 + 最终结论

本 Part 为 Part 3 的 11 条 ISSUE 给出可执行修订方案。建议仍分 P0 / P1 / P2：

- **P0 必须修**（2 条，建议合并为 **V4 PR-01 Build Identity Hardening**）。
- **P1 必须修**（5 条，建议拆为 **V4 PR-02 Boss Variant Phase Overrides + V4 PR-03 Loot Slot & Hidden Distribution + V4 PR-04 Long Run Corpus Diversity**）。
- **P2 建议在收尾 PR 顺手处理**（4 条，可合并为 **V4 PR-05 Pack Sample Refactor + UI Cleanup**）。

每条修订采用固定结构：**目标 / 具体改动 / 改动文件 / 新 owner metric / 验收命令 / 工作量评估**。

---

## § 1 · 修订 PR 总览

| PR | 合并的 ISSUE | 主要改动类别 | 优先级 | 预计工作量 |
| --- | --- | --- | --- | --- |
| V4 PR-01 Build Identity Hardening | ISSUE-001 / 002 / 007 | 源数据（capstone pool / adopt 评估） + owner metric 收紧 | P0 | L（5–8 人日） |
| V4 PR-02 Boss Variant Phase Overrides | ISSUE-004 | 源数据（boss-variants 增 `phaseOverrides`） + bossHarness 新 metric | P1 | M（3–5 人日） |
| V4 PR-03 Loot Slot & Hidden Distribution | ISSUE-003 / 005 | loot/index.yaml slotBias 调整 + hidden per-zone floor | P1 | M（3–5 人日） |
| V4 PR-04 Long Run Corpus Diversity | ISSUE-006 | smoke corpus 扩容 branch 样本 + 新增 route-hash 多样性 metric | P1 | S–M（2–3 人日） |
| V4 PR-05 Pack Sample Refactor + UI Cleanup | ISSUE-008 / 009 / 010 / 011 | sample pack REPLACE→ADD + golden label 清理 + affix 曝光微调 | P2 | S（1–2 人日） |

**串行顺序建议**：`V4 PR-01 → V4 PR-02 → V4 PR-03 → V4 PR-04 → V4 PR-05`。其中 PR-01 最紧要（解决 2 个 P0），PR-02/03/04 相互独立可任意顺序。PR-05 必须等前四 PR 完成后再跑，避免 golden 重录。

---

## § 2 · P0 修订方案

### V4 PR-01：Build Identity Hardening（ISSUE-001 + 002 + 007）

#### 2.1 目标

- 把 `crossProfessionTopWeaponDominance` 从压线 50% 压到 **≤ 40%**；
- 把 `terminalWeaponBaseDiversity` 从 3 提到 **≥ 4**；
- 把 `professionCapstoneAdoptionFloor.reportOnly` 从 2/4 提到 **4/4**，每职业至少 1/3 的采样局能 adopt 自己的 capstone；
- 让 arcanist 在 ARMOR slot 上有 profession-aligned 候选（不再被 `unique_furnace_plate` 吞掉）。

#### 2.2 具体改动

**Step 1 · 为 templar 引入 profession-favored terminal weapon**（对抗 ISSUE-001）

- 在 `game/src/main/resources/data/items/unique.yaml` 或 weapons 源数据中新增（或提升权重）templar 专属 terminal weapon 候选。有两个路径选其一：
  - 路径 A（较轻）：把 `unique_greenwood_watcher_blade`（已存在于 `greenwood_ambush_hideout.secret` fixedItemIds）的 `specialTemplateTagPreference` 抬到 templar-aligned，把该 weapon 声明为 templar capstone；
  - 路径 B（较重）：新增一把 templar 专属 `unique_vesperlight_maul`（与 `unique_vesper_chainmail` 形成成套语言），加入 templar capstone 列表。
- 同步更新 `game/src/main/resources/data/build-identity/` 的 templar capstone 列表与 `preferredSources`。

**Step 2 · 为 arcanist 引入 ARMOR capstone**（对抗 ISSUE-007）

- 新增 arcanist 专属 ARMOR capstone（例如 `unique_tideveil_robe`，与现有 arcanist OFF_HAND capstone `unique_deepcurrent_lens` 形成统一 water/current 语言）；
- 加入 `underground_river_crystal_rift.secret` 或 arcanist-preferred BOSS drop 的 fixedItemIds；
- 降低 `unique_furnace_plate` 的 cross-profession 触发权重（或在 adopt 评估函数中对 profession mismatch 的 capstone 评分打折）。

**Step 3 · 修复 adopt 评估函数的 artifact 优先级**（对抗 ISSUE-002）

- 在 loot adopt 评估逻辑（大概率在 `core/src/main/kotlin/com/ktome/core/loot/` 或 `game/src/main/kotlin/com/ktome/game/item/` 下）引入：
  - 当候选 item 的 `specialTemplateTagPreference` 包含当前 profession id 且 item 类别为 `capstone` 时，adopt 评分乘以 profession-match 加成系数（例如 1.25x）；
  - 当 OFF_HAND 候选为 artifact + profession-match 时，额外允许替换掉当前已持有的 shield/charm。
- 避免：arcanist 拿到 `artifact_river_echo`（OFF_HAND, arcanist capstone）时被已持有的 `emerald_charm` 压制。

**Step 4 · 收紧 owner metric**

- phase4Report 现有 `crossProfessionTopWeaponDominance` 阈值 `<= 50%`，新建议改为 **`<= 40%`**（收紧 10pp）；
- `terminalWeaponBaseDiversity` 从 `>= 3` 改为 **`>= 4`**；
- `professionCapstoneAdoptionFloor` 从 `reportOnly + APPROVED_DEBT` 升级为 **PASS/FAIL hard gate**，要求 4/4 职业 `adoption>=1, samples>=3`；
- `nonWeaponBuildPayoffFloor` 同步升级。

#### 2.3 改动文件

- `game/src/main/resources/data/boss-variants/index.yaml`（variant-aware capstone 优先）
- `game/src/main/resources/data/items/unique.yaml` 或 `artifact.yaml`（新 capstone 候选）
- `game/src/main/resources/data/loot/index.yaml`（secret zone fixedItemIds + specialTemplateTagPreference）
- `game/src/main/resources/data/build-identity/*.yaml`（capstone 列表 + preferredSources）
- `core/src/main/kotlin/com/ktome/core/loot/ItemAdoptEvaluator.kt`（或等价位置，具体路径以实际代码库为准）
- `tools/src/main/kotlin/com/ktome/tools/phase4/`（owner metric threshold 更新）

#### 2.4 新 owner metric / 约束变更

| Metric | 旧 | 新（建议） |
| --- | --- | --- |
| `crossProfessionTopWeaponDominance` | `<= 50%` PASS | **`<= 40%` PASS** |
| `terminalWeaponBaseDiversity` | `>= 3` PASS | **`>= 4` PASS** |
| `professionCapstoneAdoptionFloor` | `reportOnly`（2/4 APPROVED_DEBT） | **PASS/FAIL hard gate**（4/4 必需，每职业 `adoption>=1, samples>=3`） |
| `nonWeaponBuildPayoffFloor` | `reportOnly`（2/4 APPROVED_DEBT） | **PASS/FAIL hard gate**（4/4 必需） |
| 新增 `professionArmorCapstoneAdoption.reportOnly` | — | 每职业 ARMOR slot 至少 seen 一次 profession-aligned capstone |

#### 2.5 验收命令

```bash
./gradlew longRunLab
./gradlew whiteBoxLoot
./gradlew phase4Report
./gradlew verifyChanged
```

核心断言：

- phase4Report 的 `crossProfessionTopWeaponDominance` ≤ 40%；
- `terminalWeaponBaseDiversity` ≥ 4；
- `professionCapstoneAdoptionFloor` 的 note 显示 `adoption>=1` 对 4/4 职业；
- long-run summary 中 arcanist / rogue 的 `buildHash` 至少在 ARMOR 或 OFF_HAND 一个 slot 出现 profession-aligned artifact。

#### 2.6 工作量评估

- 源数据调整：1–2 人日；
- adopt 评估函数修复 + 单测：1–2 人日；
- owner metric 升级 + phase4Report 配置：1 人日；
- long-run harness 重跑 + 结果回归：1–2 人日；
- 合计：**5–8 人日（L）**。

---

## § 3 · P1 修订方案

### V4 PR-02：Boss Variant Phase Overrides（ISSUE-004）

#### 3.1 目标

- 让每个 boss variant **在源数据中**贡献至少 1 个 `phaseOverrides`，真正引入 variant-level 阶段语言；
- 让 bossHarness 新增 `bossVariantPhaseOverrideCoverage` owner metric，要求 3/3 variants 都有 phase-level 重写。

#### 3.2 具体改动

**Step 1 · 扩展 boss-variants 源数据**（`game/src/main/resources/data/boss-variants/index.yaml`）

为每个 variant 补 `phaseOverrides`：

- `molten_glass`（对应 molten_giant 的 phase_enraged）：在 `phase_enraged` 引入 "glass shard" 地形互动，动作池新增 `glass_shatter_wave`（AoE 射线）；
- `grey_crown`（对应 dungeon_lord 的 phase_desperate）：在 `phase_desperate` 引入 "crown summon" 召唤小怪，动作池新增 `summon_crownguard`（召唤 2–3 个 mini minion）；
- `abyssal_eclipse`（对应 abyssal_heart 的 phase_enraged，且已 terrain 切到 `void_mirror + phase_runner`）：在 `phase_enraged` 引入 "eclipse field"，动作池新增 `cast_eclipse_field`（地面 AoE telegraph + 持续 debuff）。

每个 `phaseOverrides` 至少需要：

- `phaseId`：目标阶段；
- `actionWeights`：新动作或权重覆写；
- `telegraphTemplateId`（可选）：对接 frontstage cue 合同；
- `terrainPreference`（可选）：variant 专属地形互动。

**Step 2 · bossHarness 新 metric**

- 新增 `bossVariantPhaseOverrideCoverage`：要求 `count(variants with phaseOverrides) / count(variants) >= 100%`；
- 新增 `bossVariantPhaseOverrideActionDistinctCount.reportOnly`：统计每个 variant 在 overridden phase 中新动作的数量。

**Step 3 · cue 合同对接**（可选但推荐）

- 若 variant 新动作需要 telegraph，接入 `FrontstageActionCueSnapshot` 的 `CRITICAL / HIGH` priority；
- 验证 `frontstageHighPriorityCueRetainedRate` 仍保持 100%。

#### 3.3 改动文件

- `game/src/main/resources/data/boss-variants/index.yaml`
- `game/src/main/kotlin/com/ktome/game/harness/BossHarnessTest.kt` + owner metric
- `tools/src/main/kotlin/com/ktome/tools/phase4/`（metric 定义）

#### 3.4 新 owner metric

| Metric | 目标 | 阈值 |
| --- | --- | --- |
| `bossVariantPhaseOverrideCoverage` | PASS/FAIL | `== 100% (3/3)` |
| `bossVariantPhaseOverrideActionDistinctCount.reportOnly` | 分布透明度 | 每 variant >= 1 个新动作 |

#### 3.5 验收命令

```bash
./gradlew bossHarness
./gradlew clientSmoke goldenScreenshot
./gradlew phase4Report
```

#### 3.6 工作量

- 源数据扩展 + telegraph 合同：2 人日；
- 新 metric + harness 断言：1 人日；
- golden 回归 + 人工白盒：1 人日；
- 合计：**3–5 人日（M）**。

---

### V4 PR-03：Loot Slot & Hidden Distribution（ISSUE-003 + 005）

#### 3.1 目标

- 把 `milestoneRewardSlotDistribution` 的 ARMOR 比例从 7.8% 提到 **≥ 20%**；
- 把 `zoneDiscoveryDistribution` 的最大值从 52% 压到 **≤ 40%**；
- 新增 `perZoneSecretConversionFloor.reportOnly`，要求每 zone 的 `secretEntry/lead >= 25%`。

#### 3.2 具体改动

**Step 1 · 调整 loot slot bias**（`game/src/main/resources/data/loot/index.yaml`）

对所有 cadence 与 main reward profile 的 `slotBias`：

- 抬高 ARMOR 权重：建议每 profile ARMOR 权重从当前 1–3 提到 3–5；
- 对 BOSS milestone 引入 "slot rotation" 约束：连续两个 BOSS milestone 不能同 slot（或引入 slot-coverage check）。

**Step 2 · 调整 hidden lead / secret 分布**

- 降低 `abyssal_temple` 的 lead 曝光密度：在 objective runtime 中减少 `SEARCH` cue 的触发频率，或在 objective 途中保留至少 1 个 secret entry detour node；
- 抬高 `underground_river` 的 lead 密度：该 zone 目前 lead share 只占 9.3%，可通过增加 lead template 或扩 room 覆盖范围补齐；
- 目标均衡：每 zone lead share 在 15–40% 之间。

**Step 3 · 新 owner metric**

- `milestoneRewardSlotBalance`：
  - ARMOR >= 20%
  - WEAPON >= 25%
  - OFF_HAND >= 25%
  - 三 slot 均 <= 50%
- `perZoneSecretConversionFloor.reportOnly`：每 zone `secretEntry / lead >= 25%`；
- `zoneLeadDiscoveryMaxShare`：`<= 40%`。

#### 3.3 改动文件

- `game/src/main/resources/data/loot/index.yaml`（cadence + main profile slotBias）
- `game/src/main/resources/data/hidden/*.yaml`（lead template 分布）
- `game/src/main/resources/data/zones/abyssal_temple.yaml`（如存在，调整 objective 节奏）
- `tools/src/main/kotlin/com/ktome/tools/phase4/` owner metric 定义

#### 3.4 验收命令

```bash
./gradlew longRunLab
./gradlew organicHiddenProbe
./gradlew phase4Report
./gradlew verifyChanged
```

#### 3.5 工作量

- slot bias 调整 + 回归：1 人日；
- hidden 分布调整 + organicHiddenProbe 回归：1–2 人日；
- 新 owner metric：1 人日；
- 合计：**3–5 人日（M）**。

---

### V4 PR-04：Long Run Corpus Diversity（ISSUE-006）

#### 3.1 目标

- 把 `LONG_RUN_SMOKE` 的 `full_route` 样本扩容，使得 `zoneRouteHashDistribution` 的最大值占比 **≤ 40%**；
- 新增 `zoneRouteHashDiversity` owner metric；
- 保证 `branch_inclusive` 样本 >= 3 条，覆盖 3 条不同分支区域。

#### 3.2 具体改动

**Step 1 · 扩容 smoke corpus**

- 把 `LONG_RUN_SMOKE` 的 full_route 采样从 6 提到 12+；
- 增加 3 条 branch_inclusive 样本，分别走 `greenwood → underground_river`、`deep_iron → grey_gate → crystal_cavern`、`abyssal_temple` 分支路径；
- seed 家族扩容，避免都走同 route hash。

**Step 2 · 新 owner metric**

- `zoneRouteHashDiversity`：`topHashShare <= 40%`；
- `branchInclusiveCount`：`>= 3`；
- `topologyCategoryDiversityPerSmokeRun.reportOnly`：每 smoke run 的拓扑类型多样性分布。

#### 3.3 改动文件

- `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt`（corpus 扩容）
- `tools/src/main/kotlin/com/ktome/tools/phase4/` owner metric 定义

#### 3.4 验收命令

```bash
./gradlew longRunLab
./gradlew phase4Report
```

#### 3.5 工作量

- corpus 扩容：1 人日；
- metric + 回归：1–2 人日；
- 合计：**2–3 人日（S–M）**。

---

## § 4 · P2 修订方案

### V4 PR-05：Pack Sample Refactor + UI Cleanup（ISSUE-008 + 009 + 010 + 011）

#### 4.1 目标

- `sample.flooded_relics` 由 REPLACE-heavy 重构为 ADD-first 示范；
- 清理 `combat-decision-stub` golden label 遗留；
- 调整头部 affix 曝光权重，尾部 affix 抬曝光；
- （可选）加入 `explainPaneOpenRate.reportOnly`。

#### 4.2 具体改动

**Step 1 · sample pack refactor**

- 审查 `examples/content-packs/sample.flooded_relics/` 下所有 pack ops；
- 将 REPLACE 改为 ADD（扩展）或 MODIFY（修改字段），除非确有必要 REPLACE；
- 对不可避免的 REPLACE 加 yaml 注释说明 "This REPLACE is intentional because ..."。

**Step 2 · golden label 清理**

- `grep -r "combat-decision-stub" client/ core/ game/ docs/` 验证无遗留；
- 若有 golden 文件，删除或迁移到 `phase4-uiux-pr05-*` 前缀；
- 在 manual-records 留凭证。

**Step 3 · affix 曝光微调**

- 对 `sentinel / of_strength / vampiric / of_life` 的 base weight 降 10–20%；
- 对 `of_piercing / of_shadow / of_smite` 与稀有尾部 affix 抬曝光；
- 目标：头部 5 个 affix 占比从当前 ~50% 降到 ≤ 40%。

**Step 4 · ExplainPane owner metric（可选）**

- 如果决定留在 Phase 4：新增 `explainPaneOpenRate.reportOnly`，只报告不做阈值；
- 如果决定延到 Phase 5：记录在延后列表（见 § 5）。

#### 4.3 改动文件

- `examples/content-packs/sample.flooded_relics/**/*.yaml`
- `client/` golden asset 目录（具体路径以实际 golden 库为准）
- `game/src/main/resources/data/affixes/**`
- `docs/opt/ui-pr/manual-records/`（留 manual record 凭证）

#### 4.4 验收命令

```bash
./gradlew contentPackHarness
./gradlew assetLint styleLint manifestLint
./gradlew clientSmoke goldenScreenshot verifyChanged
./gradlew phase4Report
```

#### 4.5 工作量

- sample pack 重构：0.5 人日；
- label 清理 + 回归：0.5 人日；
- affix 微调：0.5 人日；
- ExplainPane metric（可选）：0.5 人日；
- 合计：**1–2 人日（S）**。

---

## § 5 · 可延后到 Phase 5 的条目

以下条目**不会阻塞 Phase 4 收口**，且拖到 Phase 5 不会引发结构性回归。可在 Phase 5 起步时作为 "Phase 4 长尾清扫" 任务消化。

| 条目 | 来源 ISSUE | 延后理由 |
| --- | --- | --- |
| `explainPaneOpenRate` owner metric | ISSUE-011 | UX 反馈信号；不影响玩法本身；若 V4 PR-05 已顺手加则不必延后 |
| `samplePackContentPlayerVisibilityRate` owner metric | § 7.4 of Part 2 | pack 生态验证；pack 作者真正开始产 pack 时才有验证价值 |
| 头部 affix 集中度微调若已做则不再迭代 | ISSUE-010 | 奖励多样性；属于调优而非结构性缺陷 |
| Berserker / Spellblade 等非 canonical 职业的 build identity 收口 | — | 当前 phase4Report `professionCapstoneSeenRate` 只覆盖 4 个 canonical profession；berserker / spellblade 在 long-run 采样中出现但未纳入 floor |
| 前中期阵亡率 owner metric | § 1.4 of Part 2 | 当前 long_run smoke 是 "全胜" corpus；hard_route corpus 可在 Phase 5 作为补充 |

---

## § 6 · Phase 4 收口验证清单（完整版）

按 V4 PR-01..05 全部完成后，跑以下收口 gate：

```bash
# 1. 核心 harness
./gradlew longRunLab
./gradlew bossHarness
./gradlew whiteBoxLoot
./gradlew organicHiddenProbe
./gradlew contentPackHarness
./gradlew terrainInteractionBatch

# 2. Phase 4 汇总报告
./gradlew phase4Report

# 3. UI 合规
./gradlew clientSmoke goldenScreenshot verifyChanged

# 4. Lint / 资源
./gradlew assetLint styleLint manifestLint audioLint
./scripts/verify-bootstrap.sh

# 5. Review gate
# ktome-diff-doc-review
# ktome-code-review
# simplify-code-review-cleanup （全部 PR 完成后）
```

**核心 owner metric 断言**（收口必要条件）：

| Metric | 当前 | 收口目标 |
| --- | --- | --- |
| `crossProfessionTopWeaponDominance` | 50% | **≤ 40%** |
| `terminalWeaponBaseDiversity` | 3 | **≥ 4** |
| `professionCapstoneAdoptionFloor` | reportOnly 2/4 (APPROVED_DEBT) | **hard gate 4/4 PASS** |
| `nonWeaponBuildPayoffFloor` | reportOnly 2/4 (APPROVED_DEBT) | **hard gate 4/4 PASS** |
| `bossVariantPhaseOverrideCoverage` | — | **100% (3/3) PASS** |
| `milestoneRewardSlotBalance` | — | **ARMOR >= 20%, 三 slot 均 <= 50% PASS** |
| `zoneLeadDiscoveryMaxShare` | 52% | **≤ 40% PASS** |
| `perZoneSecretConversionFloor.reportOnly` | — | **每 zone secretEntry/lead >= 25%** |
| `zoneRouteHashDiversity` | topHash=6/6 | **topHashShare ≤ 40%** |
| `phaseVerdict` | PASS_WITH_DEBT (2 APPROVED_DEBT) | **PASS (0 APPROVED_DEBT)** |

---

## § 7 · 最终结论

### 7.1 当前 Phase 4 状态（一句话）

> **"工程合同落地 9 成 / 玩家体验深度 7 成"** — Phase 4 架构、cross-cutting contracts、V3 PR-01..05 修订承诺都已落地，`phase4Report` PASS_WITH_DEBT；但 build identity、boss variant 语言、loot slot 分布、organic hidden 分布这 4 个核心体验维度有结构性薄弱，不适合在此状态下直接进入 Phase 5。

### 7.2 是否可以进入 Phase 5？

**不可以。**

理由：

1. **成长维度（profession build identity）有 2 条 P0 伪完成**（ISSUE-001 / 002）——这是 Phase 4 核心卖点"重复游玩差异明显"的最重要承载维度，当前仅 2/4 职业真正兑现；
2. **战斗维度（boss variant）有 1 条 P1 伪完成**（ISSUE-004）——V3 PR-04 的 bossHarness 指标满分来自测量角度偏移，源数据 `phaseOverrides` 空白；
3. **奖励维度（slot 分布）与探索维度（hidden 分布）各有 1 条 P1 分布失衡**（ISSUE-003 / 005）——aggregate 指标过关但 per-slot / per-zone 维度严重偏斜；
4. **2 条 APPROVED_DEBT 不应该成为 Phase 4 的最终状态**——它们是 V3 PR-02 的直接残余，如果 Phase 4 以 APPROVED_DEBT 收尾，下游 Phase 5 承担的将是"Phase 4 没收完"的历史债。

若强行进入 Phase 5，玩家感知层面的 "4 个 profession 重玩差异不够" 与 "终盘 boss 语言偏薄" 会成为 Phase 5 replay / 元进程层设计的噪音：Phase 5 的 replay 只能放大 Phase 4 的重玩内容，而 Phase 4 的重玩内容目前在 2 个职业上是破的。

### 7.3 最小收口路径（理想工作量估算）

| PR | 工作量 | 优先级 | 是否可并行 |
| --- | --- | --- | --- |
| V4 PR-01 Build Identity Hardening | 5–8 人日 | P0 最高 | 必须串行（后置 PR 都依赖它的 capstone 源数据） |
| V4 PR-02 Boss Variant Phase Overrides | 3–5 人日 | P1 | 可与 PR-03/04 并行 |
| V4 PR-03 Loot Slot & Hidden Distribution | 3–5 人日 | P1 | 可与 PR-02/04 并行 |
| V4 PR-04 Long Run Corpus Diversity | 2–3 人日 | P1 | 可与 PR-02/03 并行 |
| V4 PR-05 Pack Sample Refactor + UI Cleanup | 1–2 人日 | P2 | 必须放在最后 |
| **合计** | **14–23 人日** | — | — |

按一个开发 1.0 并发估算，**最小收口路径约 3–5 周**。

### 7.4 审阅总判定

**`PASS_WITH_DEBT` → `Phase 4 不合适作为完成态定稿`**。

- 工程合同：PASS；
- 玩家体验深度：**尚未收口**；
- 推荐动作：执行 V4 PR-01..05，期望收口后 phase4Report 状态为 **`PASS`**（0 APPROVED_DEBT），`crossProfessionTopWeaponDominance ≤ 40%`，`professionCapstoneAdoptionFloor` 与 `nonWeaponBuildPayoffFloor` 都从 reportOnly 提升为 hard gate；
- 收口后 Phase 4 才满足"重复游玩差异明显且稳定好玩的长局"的承诺，进入 Phase 5 才不背历史债。

### 7.5 给主创与 owner 的三句话总结

1. **Phase 4 不是"没做完"，是"做完了但体验层没兑现"**——2 个 P0 都是伪完成，属于"数据流穿透完整、玩家行为未发生"。
2. **最怕的 trap 是"看 phase4Report 全绿就放行"**——V3 PR-04 的 bossHarness 4 项 100% 是测量角度没覆盖承诺真实语义的教科书案例；这次审阅 owner 要拒绝"指标满分就是完成"。
3. **V4 PR-01 是关键路径**——单它一条的通过就能拉动 Phase 4 从"刚够"到"真好玩"。其他 4 条 PR 的收益相对边际，但缺了就会出现"修了 3 个 profession build / 剩 1 个还在撞车"的收尾尴尬。

---

> 审阅结束。本报告 4 个 Part 共覆盖 20 条一致性矩阵条目、7 个体验维度、11 条关键 ISSUE、5 条修订 PR 方案。所有结论均锚定到 `docs/phase4/` 设计文档、`docs/review/phase4/v3/` V3 修订承诺、`docs/opt/ui-pr/` UIUX 修订承诺，以及 `tools/build/reports/verification/phase4/report-phase4-summary.md` 与 `build/reports/harness/long-run-summary.md` 两份最新 harness 报告的实际数据。
