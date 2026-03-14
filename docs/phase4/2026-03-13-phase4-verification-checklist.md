# Phase 4 Verification Checklist

## 1. Automated Verification

```bash
./gradlew test
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew lootBalanceLab
./gradlew hiddenContentHarness
./gradlew contentPackHarness
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 必须检查的结果

1. mapgen smoke 无崩溃、无空图、无主线死局。
2. solvability harness 批量 seed 通过。
3. loot budget 无明显越界。
4. hidden content 可被触发并可被验证。
5. 示例 content pack 通过全部 lint/harness。

## 2. Fixed-Seed / Batch Verification

### 2.1 MapGen Batch

1. 固定一组 seed 批量生成地图。
2. 记录：
   - 拓扑摘要
   - 可达性结果
   - 关键房间/秘密入口分布
   - 环路数量
   - key-gate DAG 证明项

### 2.2 Loot Balance Batch

1. 固定 zone/level/rarity 组合。
2. 记录：
   - affix 分布
   - unique/artifact 出现率
   - 预算偏离
   - `sourceLevel/sourceTier/zone/playerLevel/magicFind` 分层统计

### 2.3 Terrain Interaction Batch

1. 固定一组带地形标签的战斗 seed。
2. 记录：
   - `LIGHTNING + WATER`
   - `FIRE + OIL`
   - `COLD + WATER / ICE`
   - 元素交互是否正确进入战斗回调

### 2.4 Content Pack Batch

1. 装载 base game + 示例 pack。
2. 记录：
   - schema/lint
   - key 解析
   - headless run 结果

## 3. Manual White-Box Verification

1. 连续开 3 个不同 seed 的 run，人工确认地图差异明显。
2. 至少触发一次隐藏入口或 secret event，并确认发现逻辑清楚。
3. 用装有示例 content pack 的客户端进入一局，确认新增内容真实可见。

## 4. Reproducibility Contract

1. 所有 batch/harness 都必须固定 seed 列表和版本号。
2. 失败时必须保留：
   - 失败 seed
   - map topology 摘要
   - key-gate DAG 证明项
   - loot rollout 摘要
   - content pack 加载日志
