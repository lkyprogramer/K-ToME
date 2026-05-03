# Phase4 v4 PR-04 Hidden Search Zone Hooks 人工白盒记录

- 时间：2026-05-03 10:04:42 CST
- App：`com.ktome.client`
- packaged app 路径：`client/build/release/K-ToME.app/Contents/MacOS/K-ToME`
- app 可执行文件 SHA-256：`419654a12851fa620cab471d29e8d5a68cac78c8246d2bcbf9857a47ebe40cc1`
- app pid：`95463`
- 隔离运行目录：`build/whitebox/phase4-v4-pr04/runtime-home`
- scenario id：`phase4-v4-pr04`
- seed：`2026042434`
- preset：`HIDDEN_CONTENT`
- CUA runbook：`build/whitebox/phase4-v4-pr04/cua-runbook.md`
- 证据目录：`build/whitebox/phase4-v4-pr04/evidence`
- 日志路径：`build/whitebox/phase4-v4-pr04/evidence/app.log`
- 结论：`PASS_WITH_NOTE`

## 前置条件

- 已执行 `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr04`。
- 已通过 `build/whitebox/phase4-v4-pr04/launch-packaged-app.sh` 启动 packaged app。
- Computer Use 目标 app 绑定到 `com.ktome.client`，pid 为 `95463`。
- 截图均通过 `scripts/capture-macos-app-window.sh` 捕获目标 app 窗口，不使用全桌面截图。

## CUA 步骤与观察结果

| 步骤 | 输入 | 实际观察 | 证据 | 结果 |
| --- | --- | --- | --- | --- |
| 1 | 启动脚本 | packaged app 进入 `phase4-v4-pr04` validation session，preset 为 `HIDDEN_CONTENT`，seed 为 `2026042434`，locale 为 `zh-CN`。 | `evidence/app.log` | 通过 |
| 2 | `F9`、`Enter` | `prepare-primary-scene` 将玩家放到 `deep_iron_pit`；Search 前右侧面板可见 Search cue 上下文。 | `evidence/phase4-v4-pr04-deep-iron-search-cue.png` | 通过 |
| 3 | `F9` 关闭 overlay 后按 `S` | 正式 Search action 产生可见反馈；右侧面板显示 Search 状态，底部日志保留 PR04 scenario action 上下文。 | `evidence/phase4-v4-pr04-search-result-feedback.png` | 通过 |
| 4 | `F9`、`Right`、`Enter` | `prepare-secondary-scene` 切到 `abyssal_temple`，输出 `abyssal_void_pressure_hook_ready`，并显示 void pressure / warning 上下文。 | `evidence/phase4-v4-pr04-abyssal-void-pressure.png` | 通过 |
| 5 | `F9` 关闭 overlay | 纯地图/HUD 视图保留 `abyssal_temple` zone hook 上下文，没有依赖 overlay-only 标签作为证据。 | `evidence/phase4-v4-pr04-zone-hook-triggered.png` | 通过 |
| 6 | `F9` 后调整 cursor 到 `show-evidence-summary` | evidence summary 展示 PR04 whitebox root、app SHA-256、scenario id、CUA runbook、manual record 路径和截图证据清单。 | `evidence/phase4-v4-pr04-priority-no-overlap.png` | 通过 |

## 证据文件

- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-deep-iron-search-cue.png`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-deep-iron-search-cue.png.metadata.txt`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-deep-iron-search-cue.png.sha256`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-search-result-feedback.png`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-search-result-feedback.png.metadata.txt`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-search-result-feedback.png.sha256`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-abyssal-void-pressure.png`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-abyssal-void-pressure.png.metadata.txt`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-abyssal-void-pressure.png.sha256`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-zone-hook-triggered.png`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-zone-hook-triggered.png.metadata.txt`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-zone-hook-triggered.png.sha256`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-priority-no-overlap.png`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-priority-no-overlap.png.metadata.txt`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-priority-no-overlap.png.sha256`
- `build/whitebox/phase4-v4-pr04/evidence/app.log`
- `build/whitebox/phase4-v4-pr04/evidence/phase4-v4-pr04-app.log`
- `build/whitebox/phase4-v4-pr04/evidence/app.pid`

## Metadata 核验

- 所有截图 metadata 均包含 `capture_mode=macos-window-id`。
- 所有截图 metadata 均包含 `target_pid=95463` 与 `window_pid=95463`。
- 所有截图 metadata 均包含 `window_owner=K-ToME`。
- 所有截图 metadata 均包含 `window_bounds=137,82,1280,828`。

## 备注

- 第一次执行时，`prepare-secondary-scene` 暴露了 validation scenario 跨 zone 切换后的 client loading-state 问题：client asset session descriptor 仍停留在旧 zone，导致窗口停在 `正在加载...`。
- 已通过“snapshot zone 改变时刷新 session assets”的窄修复解除阻塞；随后 `:client:test --tests com.ktome.client.GameAppLifecycleTest` 通过。
- 修复后重新打包、重新生成白盒材料，并针对新 app hash 重新捕获全部截图。
- 生成的 launch script 只把启动元数据和 pid 写入 `app.log`；runtime action 日志通过 app 窗口日志区截图留证，没有追加到 stdout 日志。
