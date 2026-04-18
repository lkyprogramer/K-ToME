# Phase 4 Verification Checklist

## 1. Automated Verification

```bash
./gradlew test
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew whiteBoxMapgen
./gradlew whiteBoxSolvability
./gradlew whiteBoxVerify
./gradlew lootBalanceLab
./gradlew whiteBoxLoot
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew hiddenContentHarness
./gradlew organicHiddenProbe
./gradlew longRunLab
./gradlew whiteBoxHiddenContent
./gradlew contentPackHarness
./gradlew whiteBoxContentPack
./gradlew verifyOwner
./gradlew phase4Report
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

说明：

1. 当前主干已经落地 `whiteBoxMapgen`、`whiteBoxSolvability`、`whiteBoxVerify` 与 `phase4Report`。
2. 当前 `phase4Report` 聚合 `14` 个任务：`mapgenSmoke`、`solvabilityHarness`、`hiddenContentHarness`、`organicHiddenProbe`、`contentPackHarness`、`bossHarness`、`longRunLab`、`terrainInteractionBatch`、`whiteBoxMapgen`、`whiteBoxSolvability`、`lootBalanceLab`、`whiteBoxLoot`、`whiteBoxHiddenContent`、`whiteBoxContentPack`。
3. `phase4Report` 现在同时是 `scripted vs organic hidden`、`same-zone local reward identity`、`terminal build identity`、`critical-path pacing`、`terrain combat sample contract` 的唯一 owner metric 聚合入口；canonical evidence 至少必须显式保留 `dynamicPoolCoverage`、`dynamicPoolTargetProfiles`、`specialTierPassiveFamilyDuplicateCount`、`specialTierPassiveFamilyDuplicateSummary`、`rewardRoutingCoverageSummary`、`professionCapstoneSourceCoverage.reportOnly`、`professionCapstoneSeenRate`、`professionCapstoneAdoptionRate`、`professionCapstoneAdoptionFloor.reportOnly`、`nonWeaponBuildPayoffRate`、`nonWeaponBuildPayoffFloor.reportOnly`、`professionCapstoneBreakdown`。
4. `verifyOwner` 是 `Phase 4` 后续 owner 级 PR 的默认联合验收入口；它运行 routed owner task 集，不承担 phase aggregate 职责。
5. `phase4Report` 与 `phase4ReportOnly` 都按 artifact-only 语义运行，默认产物落到 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`；前面的 producer 命令必须先产出报告，再由这两个聚合入口消费。
6. `phase4Report / reportPhase4` 必须直接基于当前仓库里最新的 producer artifact materialize canonical summary，不允许只在临时 fixture 目录里生成通过后让工作区 canonical 文件继续停留在旧版本；同目录下还必须增量产出 `build-identity-debug.json`，用于保留 reward source -> selected base id、top rejected capstone candidates、rejection reasons、per-profession source coverage。
7. 旧 `tools/build/reports/phase4/phase4-summary.{json,md}` 只保留为 `phase4LegacyReport` / `phase4LegacyReportOnly` 手工 fallback 产物，不再作为默认 gate 或默认验收证据。
8. `hiddenContentHarness` 继续承担 scripted correctness owner；`organicHiddenProbe` 只负责无 primer 的 organic experience 观测，不得借助内部 reveal 路径。
9. 单个 harness 仍保留独立命令，便于局部回归、PR 级收口与 artifact 定向排查。
10. `terrainInteractionBatch + bossHarness` 是 `PR-06` 的主验证入口；`hiddenContentHarness` 只消费 terrain/mutation/boss 的结果，不承担其主验证职责。
11. Phase 4 producer inventory 的唯一 authoritative source 固定为 `tools/src/main/resources/phase4/aggregation-manifest.yaml`；`build-logic` wiring、`Phase4DomainArtifactRegistry`、`VerificationTaskRegistry` 与 baseline inputs 的一致性必须同时通过 `Phase4AggregationManifest* / Phase4RegistryConsistencyTest / ReportPhase4BuildContractTest`。
12. canonical `phase4Report` summary schema 当前固定为 `report-phase4-v2`；critical-path pacing 共享证据只保留一份在 `sections.criticalPathPacing`，四个 pacing owner metric 必须通过 `details.sectionRef = criticalPathPacing` 指向同一份 evidence，`designAudit` 也必须通过 shared pacing projection 的 additive details 透传，render 侧不得重读 raw `longRunLab.metrics`。
13. phase aggregate 的定向回归命令固定为 `reportPhase4Only`、`phase4LegacyReportOnly`、`reportPhase4`；其中 `reportPhase4` 只做 canonical vs legacy parity，对已删除的 manifest fallback 模式不再提供兼容分支。
14. 带 `.reportOnly` 后缀的 owner metric 只能表示“当前不参与 blocking gate”；命名、baseline、`failSemantics` 与 render status 必须一致。如果某项实际会阻塞 owner gate，就必须移除 `.reportOnly` 后缀，禁止继续用 report-only 名称承载 blocking 语义。
15. producer freshness 有配对约束的任务必须按同一批次重刷；`contentPackHarness + whiteBoxContentPack + phase4Report` 这类成对 producer 若 freshness 失败，标准修复是一起重跑，必要时使用 `--rerun-tasks`，禁止依赖 `UP-TO-DATE` 混用新旧 artifact。
16. 任何 authority 数据改动如果会改变 schema-visible 集合、reward candidate 集合或 loader 可见字段，必须在同一提交同步更新 schema / loader / contract test；不允许接受“本地 harness 通过但 clean-checkout 的 schema expectation 仍锁旧集合”的状态。

### 必须检查的结果

1. `mapgenSmoke`
   - 至少覆盖 `500` 个 seed
   - `0` 崩溃
   - `0` 空图
   - `0` 主线不可达
   - 单层生成 `P95 < 2s`
2. `solvabilityHarness`
   - 至少覆盖 `1000` 个 seed
   - `summary.header.seedList` 必须与 `summary.totalCases` 一一对应，不允许 seed collision
   - `CRITICAL_PATH` 可达率 `100%`
   - `OPTIONAL / SECRET` 失败不计入主线失败，但必须保留 proof
3. `whiteBoxMapgen`
   - 当前 pilot corpus 固定为 `4 zone × 2 floor × 5 seed = 40` case
   - `0` failed assertion
   - 每个 upgraded zone 的差异类别数至少为 `3`
   - 必须产出 `summary.json / cases.jsonl / report.md / artifacts/`
4. `whiteBoxSolvability`
   - 当前 pilot corpus 固定为 `4 zone × 2 floor × 5 seed = 40` case
   - `0` failed assertion
   - corpus 必须同时包含 reveal-success 与 reveal-fail case
   - corpus 至少保留 `1` 个 backtrack proof case；不要求每个 sampled seed 都发生回溯
   - 每个 `zone/floor` 都必须保留可解释 proof，且 `criticalPathFailureCount = 0`
   - `PR-03` 的 white-box 自动签收以该任务为主入口
5. `lootBalanceLab`
   - 每组上下文至少 `10000` 次 roll
   - `MAGIC / RARE` 分布偏离公式预期不超过 `±5%`
   - `UNIQUE / ARTIFACT` 分布偏离不超过 `±25%` 相对误差
   - `affixBudget` 平均偏离不超过 `±5%`，`P95` 不超过 `±12%`
   - `whiteBoxLoot` / canonical report 证据必须能从同一批 producer artifact 衍生 `dynamicPoolCoverage / dynamicPoolTargetProfiles / specialTierPassiveFamilyDuplicateCount / specialTierPassiveFamilyDuplicateSummary / rewardRoutingCoverageSummary / professionCapstoneSourceCoverage.reportOnly`
6. `hiddenContentHarness`
   - 至少覆盖 `500` 个 seed
   - 至少 `30%` 的 run 触发 `1` 个 hidden event
   - 至少 `10%` 的 run 发现 `1` 个 secret zone
   - summary 必须显式标记 `scriptedVerification = true`
   - summary 必须保留 `primerActionUsedCount / primerFreeCaseCount`
   - 不允许存在某个已升级 zone 的 hidden event 触发率长期为 `0`
   - 设计理据：
     - `30%` 保证平均每 `3~4` 局至少出现一次显式隐藏发现
     - `10%` 保持 secret zone 的稀缺感，但不会在长期游玩中完全不可见
7. `organicHiddenProbe`
   - 至少覆盖 `500` 个 seed
   - 不允许使用 `HiddenPrimerAction` 或直接 reveal API
   - `runtimeFailureCount = 0`
   - summary 必须显式标记 `scriptedVerification = false`
   - summary 必须保留 `searchActionUseRate / organicHiddenDiscoveryRate / secretZoneEntryRate`
8. `longRunLab`
   - `long-run-full.json` 必须保留 `terminalWeaponBaseDiversity`
   - `long-run-full.json` 必须保留 `crossProfessionTopWeaponDominance`
   - `long-run-full.json` 必须保留 `professionAlignedWeaponAdoptionRate`
   - `long-run-full.json` 必须保留 `professionCapstoneSeenRate`
   - `long-run-full.json` 必须保留 `professionCapstoneAdoptionRate`
   - `long-run-full.json` 必须保留 `nonWeaponBuildPayoffRate`
   - `long-run-full.json` 必须保留 `professionCapstoneBreakdown`
   - `long-run-full.json` 必须保留 `professionTerminalWeaponDistribution`
   - `long-run-full.json` 必须保留 `fullRouteZoneTraversalDiagnostics`
   - `long-run-full.json` 必须保留 `criticalPathZoneIds`
   - `long-run-full.json` 必须保留 `criticalPathCombatFloorSatisfied` 对应的 per-zone 计算输入
   - critical-path zone 的 `avgObjectiveAcquireTurn` 最小值必须 `>= 4.0`
   - critical-path zone 的 `avgVisibleHostileTurnCount` 最小值必须 `>= 1.0`
   - critical-path zone 的 `avgEnemyTurns` 最小值必须 `>= 1.0`
   - `professionCapstoneSeenRate` 的 baseline metadata 必须保留 `perProfessionSeenMinCount`，并且 owner gate 需要按 `professionCapstoneBreakdown` 对每个基础职业执行 seen floor 判定
9. `terrainInteractionBatch`
   - 五种地形交互都能在 isolated batch 中稳定复现
   - `0` unresolved interaction rule
   - trace 必须进入 `CombatPipeline step 9`
   - summary 必须保留 `combatSampledZoneIds / combatSampledZoneExclusionNotes`
   - summary 必须保留 `perZoneEncounterLowerBoundTarget / perZoneEncounterFailures`
10. `bossHarness`
   - 至少覆盖 base boss + variant boss 对照样本
   - `actionWeightProfileId` 不得改变 phase graph 结构
   - `threatCost` 汇总必须可追溯
11. `contentPackHarness`
   - 示例 pack `0` schema error
   - `0` unresolved i18n key
   - `0` unresolved visual/audio key
   - 固定 seed headless run 全通过
   - official sample pack 只要求证明 `ADD` 主路径
   - 第二夹具或模拟双包场景必须覆盖 `REPLACE / APPEND / DENY / precedence / conflict`

## 2. Fixed-Seed / Batch Verification

### 2.1 MapGen Batch

1. 固定一组 seed 批量生成地图。
2. 记录：
   - 拓扑摘要
   - 可达性结果
   - 关键房间/秘密入口分布
   - 环路数量
   - key-gate DAG 证明项
   - biome family 组合
   - `TerrainTag` 分布
3. 检查：
   - 每层至少存在 `1` 条主路径
   - 环路数量在 `0 ~ 2`
   - 若 `optionalLoopCount > 0`，则环路边 / 总连通边比例在 `0.15 ~ 0.35`
   - `vault` 只出现在 `OPTIONAL / SECRET` 路径

### 2.2 Solvability Batch

1. 固定 `1000` 个 seed 跑 `SolvabilityGraph` 验证。
2. 记录：
   - `CRITICAL_PATH` 访问顺序
   - 获取到的 key / switch / quest flag
   - 未满足依赖
   - `OPTIONAL / SECRET` 的返回主线桥接
   - `optionalPathCount / secretPathCount / totalReachableNodes / reachabilityRatio`
3. 检查：
   - `summary.header.seedList` 中的 seed 不允许重复
   - Boss 门后不存在主线必需钥匙
   - `PERCEPTION_REVEAL` 失败不会阻断主线
   - secret zone 从不承载主线硬门槛
   - 至少存在 `1` 个「先走 OPTIONAL 拿 key -> 回主路开门」的回溯用例

### 2.3 Loot Balance Batch

1. 固定 `zone / sourceLevel / sourceTier / playerLevel / magicFind` 组合。
2. 建议至少覆盖：
   - `NORMAL + magicFind=0.00`
   - `ELITE + magicFind=0.15`
   - `BOSS + magicFind=0.25`
   - `CHEST + magicFind=0.10`
   - `BOSS + magicFind=1.00`
   - `BOSS + magicFind=1.50`（验证 clamp 到 `1.0`）
3. 记录：
   - `iLvl / qLvl / rarityScore`
   - affix 分布
   - unique/artifact 出现率
   - `sameZoneSecretVsCadencePairs / sameZoneSecretVsRewardPairs`
   - `dynamicPoolCoverage / dynamicPoolTargetProfiles`
   - `sameZoneSecretVsCadenceMaxOverlap / sameZoneSecretVsRewardMaxOverlap`
   - `localIdentityFailurePairs`
   - `specialTierPassiveFamilyDuplicateCount`
   - `specialTierPassiveFamilyDuplicateSummary`
   - `rewardRoutingCoverageSummary`
   - `professionCapstoneSourceCoverage.reportOnly`
   - pity 激活次数（`rarePityActivations / uniquePityActivations`）
   - 预算偏离
   - `sourceLevel / sourceTier / zone / playerLevel / magicFind` 分层统计
   - `lootFormulaVersion / specialTierEligibilityVersion`
4. 检查：
   - `magicFind` 提高时，高 rarity 权重单调不减
   - `magicFind > 1.0` 时被 clamp 到 `1.0`
   - `BOSS + magicFind=1.50` 与 `BOSS + magicFind=1.00` 的分布结果应一致到统计容差内
   - `UNIQUE / ARTIFACT` 只出现在允许来源
   - `castSpeed` affix 经过收益递减，不出现原始线性叠加越界
   - same-zone `.secret ↔ .cadence / .reward` 冲突必须能从 summary 直接定位到具体 pair
   - reward routing drift / missing capstone source 必须能从 summary 或 `build-identity-debug.json` 直接定位到 culprit source id 与 rejection reason

### 2.4 Terrain Interaction Isolated Batch

1. 使用固定地图、固定 `TerrainTag` 和固定战斗 seed。
2. 记录：
   - `terrain_lightning_water_chain`
   - `terrain_fire_oil_ignite`
   - `terrain_cold_water_freeze`
   - `terrain_fire_ice_melt`
   - `terrain_physical_ice_slip`
   - 元素交互进入 `CombatPipeline step 9` 的 trace
3. 检查：
   - 不依赖 mapgen 也能稳定复现五种交互
   - `LIGHTNING + WATER` 会产生传导目标列表
   - `FIRE + OIL` 会创建持续燃烧地形
   - `COLD + WATER` 会生成 `ICE`，并带持续时间
   - `FIRE + ICE` 会把 `ICE` 融化回 `WATER`
   - 站在 `ICE` 上承受物理冲击时会触发滑倒或失衡检定

### 2.5 Terrain Interaction In-MapGen Batch

1. 固定一组带 `WATER / OIL / ICE` 的 zone seed。
2. 记录：
   - 交互发生时的 `zoneId`
   - 对应 `TerrainTag`
   - 触发的 `ElementInteractionRule` ID
3. 检查：
   - mapgen 生成的地形标签能被战斗回调正确消费
   - `elite mutation` 或 `artifact proc` 引用交互规则时，仍走正式 registry

### 2.6 Hidden Content Batch

1. 固定 `500` 个 seed 跑 `hiddenContentHarness`。
2. 记录：
   - 触发的 hidden event
   - 发现的 secret zone
   - discovery rule
   - `searchBindingId / searchActionResult`
   - `resolvedReturnBridgeNodeId`
   - 奖励 profile
   - `secretRuleVersion`
   - `zoneId` 维度的触发分布
3. 检查：
   - hidden event 默认只出现在 `OPTIONAL / SECRET`
   - secret zone 至少包含 `1` 个正式奖励节点
   - hidden event / secret zone 不承担主线必需钥匙
   - 不允许存在某个已升级 zone 长期无法触发 hidden event 或 secret zone

### 2.6.1 Organic Hidden Probe

1. 固定 `500` 个 seed 跑 `organicHiddenProbe`。
2. 记录：
   - `searchAttemptCount / runsWithSearchActionCount / searchActionUseCount`
   - `discoveryWithoutPrimerCount / organicHiddenDiscoveryRate`
   - `secretZoneEntryCount / secretZoneEntryRate`
   - `averageFirstHiddenDiscoveryTurn / averageFirstSecretZoneEntryTurn`
   - zone breakdown
3. 检查：
   - 不允许使用 primer action
   - 不允许借助内部直接 reveal API
   - 导航与搜索决策只允许消费 `RunObservation` / `RenderSnapshot` 暴露的可见 prompt、可见交互物与探索状态，不得读取 hidden entrance ground truth
   - `runtimeFailureCount = 0`
   - 即使首次 owner metric 为 `FAIL`，也必须输出真实观测值而不是假数据

### 2.7 Content Pack Batch

1. 装载 base game + 示例 pack。
2. 记录：
   - manifest 解析
   - harness sidecar 解析
   - schema/lint
   - overlay 冲突
   - i18n / visual / audio key 解析
   - headless run 结果
   - 双 pack fixture 的 precedence / conflict 结果
   - dependency / `versionRange` / namespace / pack order 失败诊断
3. 检查：
   - pack namespace 唯一
   - 未声明 `REPLACE` 的重复 ID 会被 lint 拒绝
   - 禁用示例 pack 后能回落到 base manifest
   - runtime 主路径固定为 `ADD + whole-entry REPLACE`
   - official sample pack 只承担 `ADD` 的玩家可见验证；非 `ADD` 语义由第二夹具或模拟双包场景覆盖
   - 第二 pack fixture 或模拟双 pack 场景下，loader precedence 与 conflict 处理符合文档约定
   - `harnessSeeds` 只能来自 sidecar harness spec，不得出现在 runtime manifest
   - 缺失依赖、依赖环、`versionRange` 冲突和 namespace 冲突都有结构化失败诊断

## 3. Manual White-Box Verification

统一白盒验证框架架构、artifact/report 合同、AI 消费协议与人工一致性策略，以 [../2026-04-04-unified-white-box-verification-framework.md](../2026-04-04-unified-white-box-verification-framework.md) 为权威。

本节只保留 `Phase 4` 当前仍需人工抽样确认的体验项。对差异性域，抽样口径统一为固定 `5` seed；对固定场景域，不强行套用 seed 差异门槛。`PR-03` 的自动白盒签收以 `whiteBoxSolvability` 为主，`whiteBoxMapgen` 提供其上游拓扑与可感知差异证据。

1. 连续开 `5` 个不同 seed 的 run，人工确认至少存在 `3` 类可感知差异：
   - 主路径 / 环路形态差异
   - vault / pattern room / hidden entrance 出现差异
   - biome family 混合、terrain tag 痕迹或奖励/遭遇差异
2. 至少触发一次隐藏入口或 secret event，并确认发现逻辑清楚。
3. 在至少两个不同 zone 中观察 `WATER / OIL / ICE` 的表现与规则一致。
4. 用装有示例 content pack 的客户端进入一局，确认新增内容真实可见。
5. 至少观察一次 elite mutation 的命名、图标、日志和 inspect 信息，确认来源可读。
6. 至少击败一次带 Boss 变体的 encounter，确认 phase 结构未被破坏，仅 mutation / loot / 表现发生变化。
7. 至少手动执行一次对有效目标的 `SearchAction`，确认会消耗标准行动、产生日志，并在失败/成功时都有明确反馈。

## 4. Reproducibility Contract

1. 所有 batch/harness 都必须固定：
   - `harnessId`
   - seed 列表
   - build id
   - `phase: P4`
   - `contentSchemaVersion`
   - `topologyFingerprintVersion`
   - `rewardLedgerVersion`
   - `lootFormulaVersion`
   - `specialTierEligibilityVersion`
   - `searchRuleVersion`
   - `secretRuleVersion`
   - `overlayContractVersion`
   - `activePackIds`（按 `PackId` 集合解释）
   - `activePackManifestVersions`（按 `Map<PackId, String>` 解释）
   - timestamp
   - 可 join 键：`seed + zoneId + floorIndex`
2. 失败时必须保留：
   - 失败 seed
   - map topology 摘要
   - key-gate DAG 证明项
   - loot rollout 摘要
   - terrain interaction trace
   - hidden content 触发日志
   - content pack 加载日志
