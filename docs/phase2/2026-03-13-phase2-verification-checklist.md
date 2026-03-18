# Phase 2 Verification Checklist

## 1. Automated Verification

### 1.1 Core & Game

```bash
./gradlew test
./gradlew :core:test
./gradlew :game:test
./gradlew headlessSmoke
./gradlew clientSmoke
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
./gradlew preReleaseAcceptance
```

### 1.2 Contract & Content

```bash
./gradlew localeLint
./gradlew contractLint
./gradlew goldenScreenshot
./gradlew soloClearLab
./gradlew longRunLab
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
```

### 1.3 必须检查的结果

1. save/load 往返稳定。
2. locale key 无缺漏、无占位符不一致。
3. manifest 可完整解析。
4. screenshot golden 无非预期漂移。
5. 四职业短局实验室全部通过。
6. 正式资源的 spec / manifest / style / audio 追溯链完整。
7. `build/reports/harness/headless-smoke.json`、`client-smoke.json`、`long-run-summary.json` 都可追溯。
8. `preReleaseAcceptance` 仍然通过，且 Phase 2 新增内容没有绕过既有发布前验收链。

## 2. Manual White-Box Verification

### 2.1 首页与语言

1. 启动 client。
2. 在首页切换中文/英文。
3. 预期：
   - 菜单即时切换语言。
   - 新开局和读当前阶段存档都使用所选语言。
   - 与 `clientSmoke` 的 locale/continue 场景一致。

### 2.2 Tile 正式路径

1. 新开局进入默认 zone。
2. 预期：
   - 地图、角色、交互物全部按 Tile 渲染。
   - HUD、背包、检视和日志与当前 locale 一致。
   - 缺图/缺音时出现 fallback 和明确错误。
   - 不破坏 `clientSmoke` 默认主路径。

### 2.3 短局闭环

1. 用四个基础职业各开一局默认短局。
2. 预期：
   - 都能完成从开局到 Boss 或失败结算的闭环。
   - 不出现裸字符串、ASCII 正式路径回退、存档损坏。
   - 与 `headlessSmoke` / `SoloClearLab` 的默认路径结论一致。

### 2.4 SoloClearLab v1 硬门禁

1. 固定黄金 seed：`20260313`、`20260314`、`20260315`。
2. 固定三个标准化场景：
   - 杂兵包：`10 x 10` 封闭房间、`6` 只普通怪、等级 `5`
   - 精英战：`15 x 15` 房间、`1` 精英 + `2` 普通怪、等级 `7`
   - Boss 战：`20 x 20` Boss 房、等级 `10`
3. 固定装备预算：阶段蓝装全套。
4. 通过标准：
   - 杂兵包：清光所有怪物且 `HP > 30%`
   - 精英战：击杀精英且角色存活
   - Boss 战：击杀 Boss
5. spot-check：
   - `Rogue` 的 `ENERGY` 命中回复正确触发
   - `Templar` 的 `POSITIVE_ENERGY` 在命中/受击时积攒，并在脱战后衰减
   - elite/Boss 至少一场使用 simple scripted AI，并出现最小 warning/overlay 提示

## 3. Reproducibility Contract

1. 同一 seed + 同一输入序列 = 同一输出。
2. 所有白盒验证都必须记录 seed、职业、locale、zone 链和输入脚本版本。
3. `golden screenshot` 必须固定分辨率、UI scale、字体包和 locale。
4. `SoloClearLab` 必须固定 scenario、输入脚本和胜负口径。
5. 验证失败时必须保留：
   - failing seed
   - input script
   - snapshot/hash
   - 日志 token 输出
   - screenshot 差异
   - 对应的 `headlessSmoke` / `clientSmoke` / `longRunLab` 报告片段
