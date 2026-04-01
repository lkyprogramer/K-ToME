# Phase 4 Verification Checklist

## 1. Automated Verification

```bash
./gradlew test
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew lootBalanceLab
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew hiddenContentHarness
./gradlew contentPackHarness
./gradlew phase4Report
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

说明：

1. `phase4Report` 是 Phase 4 的聚合入口，负责顺序执行全部正式 harness 并产出 `tools/build/reports/phase4/phase4-summary.json`。
2. 单个 harness 仍保留独立命令，便于局部回归和 PR 级收口。
3. `terrainInteractionBatch + bossHarness` 是 `PR-06` 的主验证入口；`hiddenContentHarness` 只消费 terrain/mutation/boss 的结果，不承担其主验证职责。

### 必须检查的结果

1. `mapgenSmoke`
   - 至少覆盖 `500` 个 seed
   - `0` 崩溃
   - `0` 空图
   - `0` 主线不可达
   - 单层生成 `P95 < 2s`
2. `solvabilityHarness`
   - 至少覆盖 `1000` 个 seed
   - `CRITICAL_PATH` 可达率 `100%`
   - `OPTIONAL / SECRET` 失败不计入主线失败，但必须保留 proof
3. `lootBalanceLab`
   - 每组上下文至少 `10000` 次 roll
   - `MAGIC / RARE` 分布偏离公式预期不超过 `±5%`
   - `UNIQUE / ARTIFACT` 分布偏离不超过 `±25%` 相对误差
   - `affixBudget` 平均偏离不超过 `±5%`，`P95` 不超过 `±12%`
4. `hiddenContentHarness`
   - 至少覆盖 `500` 个 seed
   - 至少 `30%` 的 run 触发 `1` 个 hidden event
   - 至少 `10%` 的 run 发现 `1` 个 secret zone
   - 不允许存在某个已升级 zone 的 hidden event 触发率长期为 `0`
   - 设计理据：
     - `30%` 保证平均每 `3~4` 局至少出现一次显式隐藏发现
     - `10%` 保持 secret zone 的稀缺感，但不会在长期游玩中完全不可见
5. `terrainInteractionBatch`
   - 五种地形交互都能在 isolated batch 中稳定复现
   - `0` unresolved interaction rule
   - trace 必须进入 `CombatPipeline step 9`
6. `bossHarness`
   - 至少覆盖 base boss + variant boss 对照样本
   - `actionWeightProfileId` 不得改变 phase graph 结构
   - `threatCost` 汇总必须可追溯
7. `contentPackHarness`
   - 示例 pack `0` schema error
   - `0` unresolved i18n key
   - `0` unresolved visual/audio key
   - 固定 seed headless run 全通过

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
   - 第二 pack fixture 或模拟双 pack 场景下，loader precedence 与 conflict 处理符合文档约定
   - `harnessSeeds` 只能来自 sidecar harness spec，不得出现在 runtime manifest
   - 缺失依赖、依赖环、`versionRange` 冲突和 namespace 冲突都有结构化失败诊断

## 3. Manual White-Box Verification

1. 连续开 `3` 个不同 seed 的 run，人工确认地图差异明显。
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
