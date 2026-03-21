# Phase 2 Verification Checklist

> `PR-07 / P2-C` 的审查后执行顺序、formal path 资源门禁与 sprint/PR 拆分，见：
> [2026-03-20-phase2-pr-07-post-review-execution-plan.md](./2026-03-20-phase2-pr-07-post-review-execution-plan.md)

## 0. Current Stage Truth

1. 当前主线状态固定为 `P2-B 已完成 / P2-C 未完成`。
2. `OfficialSliceStability`（位于 `:game:test`）、`longRunLab`、`clientSmoke`、`goldenScreenshot` 对 `PR-06` 官方切片的通过，只能作为 `P2-B` 稳定性证据，不能视为 `P2-C` 已完成。
3. `PR-06` 稳定性问题不再是当前 blocker；后续只允许对 `P2-B` 正式切片做 bugfix，不允许为了 `P2-C` 目标重写其既有真相。
4. 当前 `P2-C` 的正式阻塞项是：`Rogue / Templar` 正式化、`4 zone` 真实 route、`24 怪 + 24 物品` 下限、gate 重对齐，以及 Visual/Audio formal path 收口。
5. 命中 `phase2` formal-path required key 的 `debug/missing_visual.png` 或 `audio/fallback/silence.ogg`，必须视为 `P2-C` blocker，而不是“最后再补”的资源尾债。

## 1. Automated Verification

### 1.0 Gate Interpretation

1. `:game:test` 中的 `OfficialSliceStability` 继续承担 `P2-B` 官方切片回归；它的意义是守住最小正式切片，不是替代 `P2-C` 的 route / content / formal path 验收。
2. `headlessSmoke`、`clientSmoke`、`goldenScreenshot`、`longRunLab`、`preReleaseAcceptance` 必须逐步覆盖当前短局正式主路径，而不是只证明 `shattered_outpost` 仍可跑通。
3. `soloClearLab`、`assetLint`、`styleLint`、`audioLint`、`manifestLint` 是 `P2-C` 完成态门禁的一部分，用来证明四职业短局、资源追溯链和 formal path required key 已经收口。
4. 只要 `P2-C` required route、content floor 或 formal path 仍缺任一项，即使基础 smoke 全绿，也不能宣称 Phase 2 完成。
5. `manifestLint` 与 `audioLint` 的输出必须显式区分：
   - required formal-path key 当前是否仍命中 fallback
   - 剩余 `phase2` debug budget 还保留多少 `missing_visual` / `silence.ogg`

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
2. `OfficialSliceStability` 继续证明 `P2-B` 官方切片稳定，没有因 `P2-C` 扩张产生回归。
3. `4 zone` route 可以启动、推进、结算，并在 save/load 后恢复正确 route 位置。
4. locale key 无缺漏、无占位符不一致。
5. manifest 可完整解析。
6. screenshot golden 无非预期漂移。
7. 四职业短局实验室全部通过。
8. `P2-C` formal-path required visual key 不再命中 `missing_visual`，required audio key 不再命中 `silence.ogg`。
9. 正式资源的 spec / manifest / style / audio 追溯链完整。
10. `build/reports/harness/headless-smoke.json`、`client-smoke.json`、`long-run-summary.json` 都可追溯。
11. `preReleaseAcceptance` 仍然通过，且 Phase 2 新增内容没有绕过既有发布前验收链。
12. `assetLint`、`manifestLint`、`audioLint` 的成功输出里，必须能读到 required key 总数、required fallback 数，以及剩余 `phase2` placeholder/silence budget 数。

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
