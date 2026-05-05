# Phase4 v4 PR-06 Long-run Route Diversity 人工白盒记录

- 时间：`2026-05-05 10:49 CST`
- 执行者：`Codex + Computer Use`
- scenario id：`phase4-v4-pr06`
- seed：`2026042436`
- preset：`MAPGEN_DIFF`
- locale：`zh-CN`
- 窗口尺寸：`1280x800`
- profession：`rogue`
- routeIndex：`0`
- packaged app 路径：`client/build/release/K-ToME.app/Contents/MacOS/K-ToME`
- packaged app SHA-256：`716e500fd1beeeedff4fa30ce6559e8b488103449355f11a28bdb37d7a21178a`
- launch script SHA-256：`185b04a6a833cd444e31357ae453eb931a96be79f53aa6eeef55984879e37862`
- runtime home：`build/whitebox/phase4-v4-pr06/runtime-home`
- evidence 目录：`build/whitebox/phase4-v4-pr06/evidence`
- CUA runbook：`build/whitebox/phase4-v4-pr06/cua-runbook.md`
- Computer Use target：`com.ktome.client`
- runtime PID：`6764`
- 结论：`PASS`

## 执行命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr06
build/whitebox/phase4-v4-pr06/launch-packaged-app.sh
```

定向测试、打包与白盒材料生成结果均为 `BUILD SUCCESSFUL`。

## 本轮修复

- 根因：packaged app 内直接读取 PR06 repo artifact 时，UI summary 可 stat 到 producer artifact，但 artifact-backed summary 解析结果落成 `artifactStatus=unavailable`，导致人工白盒无法从验证面确认 route diversity。
- 修复：`preparePhase4V4Whitebox` 从 canonical report、long-run producer、verifyChanged plan 生成 artifact-derived 短摘要，并通过 JVM system properties 注入 packaged app；运行时仍保留 repo artifact 读取 fallback。
- 展示优化：primary summary 缩短为人工验收字段，直接显示 `scenarioTypeDistribution`、`zoneRouteHashDistribution` 概要、`topHashShare` 阈值和 branch secret 样本；secondary summary 显示 `verifyChangedArtifactStatus`、任务数量、关键 owner tasks 与 UI-only surface。

## 磁盘产物状态

- `build/reports/harness/long-run-full.json` 存在，mtime `2026-05-05 10:06:14 +0800`，JSON 语法校验通过。
- `tools/build/reports/verification/phase4/report-phase4-summary.json` 存在，mtime `2026-05-05 10:18:56 +0800`，JSON 语法校验通过。
- `tools/build/reports/verification/phase4/report-phase4-summary.json` 显示 `scenarioTypeDistribution={full_route=12, branch_inclusive=4, route_probe=2, late_route_probe=2}`。
- `zoneRouteHashDistribution` 共 `7` 个 hash，最大 bucket 为 `4/16`，`zoneRouteHashDiversity.topHashShare=0.25`，满足 `<= 0.40`。
- `build/verification/verify-changed/verify-changed-plan.json` 存在，mtime `2026-05-05 10:18:54 +0800`，`requestedTaskPaths` 包含 `:game:longRunLab`、`:tools:scopeCoverageLint`、`:tools:reportPhase4Only`、`:tools:maintainabilityLint`。

## CUA 步骤结果

| Step | 操作 | 预期 | 实际 | 结果 | 证据 |
| --- | --- | --- | --- | --- | --- |
| 1 | 启动 `launch-packaged-app.sh` 并连接 `com.ktome.client` | packaged app 直接进入 `phase4-v4-pr06` validation session | Computer Use 绑定 `pid=6764`；启动日志记录 `scenarioId=phase4-v4-pr06`、`preset=MAPGEN_DIFF`、`seed=2026042436` | `PASS` | `build/whitebox/phase4-v4-pr06/evidence/app.log` |
| 2 | `F9`, `Enter` 执行 `prepare-primary-scene` | 可见 `scenarioTypeDistribution`，包含 `full_route=12`、`branch_inclusive=4`、`route_probe=2`、`late_route_probe=2` | UI last result 显示 `artifactStatus=loaded`、`producerArtifactStatus=loaded` 与四类 scenario 分布 | `PASS` | `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-scenario-distribution.png` |
| 3 | 记录 route hash diversity | 可见 `zoneRouteHashDistribution` 与 `topHashShare <= 40%` | UI last result 显示 `zoneRouteHashDistribution=7_hashes,max=4/16` 与 `topHashShare=0.25<=0.40` | `PASS` | `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-route-hash-diversity.png` |
| 4 | 记录 branch-inclusive routes | 至少 3 条 branch-inclusive route intent，mandatory/secret 组合不同 | UI last result 显示 `branchInclusiveRoutes=4`，包含 `deep_iron_smuggler_stash`、`underground_river_crystal_rift`、`greenwood_hidden_cache`、`abyssal_temple_warded_archive` | `PASS` | `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-branch-inclusive-routes.png` |
| 5 | `F9`, `Right`, `Enter` 执行 `prepare-secondary-scene` | verifyChanged routing summary 展示 owner surface 包含 `:game:longRunLab`，presentation-only client surface 不包含 `:game:longRunLab` | UI last result 显示 `verifyChangedArtifactStatus=loaded`、`verifyChangedTasks=9_tasks(:game:longRunLab|:tools:scopeCoverageLint|:tools:reportPhase4Only|:tools:maintainabilityLint)` 与 `uiSurface=>clientSmoke+goldenScreenshot` | `PASS` | `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-verifychanged-routing.png` |
| 6 | `F9`, `Right`, `Right`, `Enter` 执行 `show-evidence-summary` | 证据清单与 runbook/manual record 路径一致 | UI 打开 `Scenario 证据摘要`，显示 whitebox root、evidence dir、manual record、CUA runbook、app hash 与 expected evidence 路径 | `PASS` | Computer Use 观察 |

## 截图与 Sidecar

| 文件 | SHA-256 | metadata 结论 |
| --- | --- | --- |
| `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-scenario-distribution.png` | `52e94a3560630ce56b804672c172d7d795ea6a702cced4160b614caa1ef62137` | `capture_mode=macos-window-id`, `window_pid=6764`, `window_owner=K-ToME` |
| `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-route-hash-diversity.png` | `52e94a3560630ce56b804672c172d7d795ea6a702cced4160b614caa1ef62137` | `capture_mode=macos-window-id`, `window_pid=6764`, `window_owner=K-ToME` |
| `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-branch-inclusive-routes.png` | `52e94a3560630ce56b804672c172d7d795ea6a702cced4160b614caa1ef62137` | `capture_mode=macos-window-id`, `window_pid=6764`, `window_owner=K-ToME` |
| `build/whitebox/phase4-v4-pr06/evidence/phase4-v4-pr06-verifychanged-routing.png` | `2d0cd66dd6f2d8b2dca98ea3ce78e8b16824c929f0beab8f3b75cb4d469a2e74` | `capture_mode=macos-window-id`, `window_pid=6764`, `window_owner=K-ToME` |

## 结论

本轮 packaged app + Computer Use 流程已按 `build/whitebox/phase4-v4-pr06/cua-runbook.md` 完成，截图窗口与 `app.pid=6764` 匹配。PR06 artifact-backed validation summary 已能在 packaged app UI 内展示 route diversity、hash diversity、branch-inclusive samples 与 verifyChanged routing，结果与 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md` 要求一致。

## 剩余风险

- 本次白盒使用 fast whitebox packaged app 路径，不替代 nightly/owner wide corpus。
- `phase4-v4-pr06-app.log` 仍只记录 launch metadata；action 结果通过 packaged app UI last-result 与 Computer Use 截图留证。
