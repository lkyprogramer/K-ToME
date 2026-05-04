# Phase4 v4 PR-05 Boss Variant Phase Language 人工白盒记录

- 时间：2026-05-04 11:14:52 CST
- App：`com.ktome.client`
- packaged app 路径：`client/build/release/K-ToME.app/Contents/MacOS/K-ToME`
- app 可执行文件 SHA-256：`8d0a8680c162a997744ecda9f1b2ed6801255802ce96c57aeca7f16631d5e550`
- app pid：`85691`
- 隔离运行目录：`build/whitebox/phase4-v4-pr05/runtime-home`
- scenario id：`phase4-v4-pr05`
- seed：`2026042435`
- preset：`BOSS_VARIANT`
- CUA runbook：`build/whitebox/phase4-v4-pr05/cua-runbook.md`
- 证据目录：`build/whitebox/phase4-v4-pr05/evidence`
- 日志路径：`build/whitebox/phase4-v4-pr05/evidence/app.log`
- 结论：`PASS_WITH_AUDIO_CAPTURE_LIMITATION`

## 前置条件

- 已使用 `build/whitebox/phase4-v4-pr05/launch-packaged-app.sh` 启动 packaged app。
- launch script 启动前校验 app executable hash，实际 hash 与 `build/whitebox/phase4-v4-pr05/app-executable.sha256` 一致。
- Computer Use 目标 app 绑定到 `com.ktome.client`，pid 为 `85691`。
- 截图均通过 `scripts/capture-macos-app-window.sh` 捕获目标 app 窗口，不使用全桌面截图。
- `tools/build/reports/verification/phase4/report-phase4-summary.md` 生成时间为 `2026-05-04 10:53:36 CST`，`phaseVerdict=PASS`。

## CUA 步骤与观察结果

| 步骤 | 输入 | 实际观察 | 证据 | 结果 |
| --- | --- | --- | --- | --- |
| 1 | 启动脚本 | packaged app 进入 `phase4-v4-pr05` validation session，preset 为 `BOSS_VARIANT`，seed 为 `2026042435`，locale 为 `zh-CN`。 | `evidence/app.log` | 通过 |
| 2 | `F9`、`Enter` | `prepare-primary-scene` 生成 `boss.variant.molten_glass` 场景；窗口可见 molten 专属 phase override warning、阶段进入反馈和 boss variant 可见状态。 | `evidence/phase4-v4-pr05-molten-glass-warning.png` | 通过 |
| 3 | `Right`、`Enter` | `prepare-secondary-scene` 首次轮转到 `boss.variant.grey_crown`；窗口可见 grey crown phase override warning，日志区显示 `战场指挥` 等阶段行动反馈。 | `evidence/phase4-v4-pr05-grey-crown-warning.png` | 通过 |
| 4 | `Enter` | `prepare-secondary-scene` 再次轮转到 `boss.variant.abyssal_eclipse`；窗口可见 abyssal eclipse phase override warning，日志区显示 `虚空裂隙` 等阶段行动反馈。 | `evidence/phase4-v4-pr05-abyssal-eclipse-warning.png` | 通过 |
| 5 | `Right`、`Enter` | `show-evidence-summary` 展示 PR05 whitebox root、app SHA-256、scenario id、CUA runbook、manual record 路径和截图证据清单。 | `evidence/phase4-v4-pr05-report-coverage.png` | 通过 |

## Owner Evidence 交叉核验

- `tools/build/reports/verification/phase4/report-phase4-summary.md`：`phaseVerdict=PASS`。
- `bossVariantPhaseOverrideSchemaCoverage`：`100.0% (3/3)` / `PASS`。
- `bossVariantPhaseOverrideRuntimeTriggerCoverage`：`100.0% (3/3)` / `PASS`。
- `bossVariantPhaseOverrideTelegraphCoverage`：`100.0% (3/3)` / `PASS`。
- `bossVariantPhaseOverrideActionDistinctCount.reportOnly`：`min 2; boss.variant.abyssal_eclipse=2, boss.variant.grey_crown=2, boss.variant.molten_glass=2` / `PASS`。
- `tools/build/reports/verification/phase4/inputs/bossHarness.json`：`phaseGraphUnchangedReason=data_level_override_only`。
- `game/src/main/resources/data/telegraph/index.yaml`：三个 override telegraph 分别引用 `audio.boss.variant.molten_glass`、`audio.boss.variant.grey_crown`、`audio.boss.variant.abyssal_eclipse`。

## 证据文件

- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-molten-glass-warning.png`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-molten-glass-warning.png.metadata.txt`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-molten-glass-warning.png.sha256`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-grey-crown-warning.png`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-grey-crown-warning.png.metadata.txt`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-grey-crown-warning.png.sha256`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-abyssal-eclipse-warning.png`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-abyssal-eclipse-warning.png.metadata.txt`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-abyssal-eclipse-warning.png.sha256`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-report-coverage.png`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-report-coverage.png.metadata.txt`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-report-coverage.png.sha256`
- `build/whitebox/phase4-v4-pr05/evidence/app.log`
- `build/whitebox/phase4-v4-pr05/evidence/phase4-v4-pr05-app.log`
- `build/whitebox/phase4-v4-pr05/evidence/app.pid`

## SHA-256

- `phase4-v4-pr05-molten-glass-warning.png`: `5bfec56d3549d77422d68ea2cf903b209b2d958f4997fbde1cd0813f4603d1af`
- `phase4-v4-pr05-grey-crown-warning.png`: `2e9a206b65860def80dd1ad3449940b2a1b1dfaf835f0ae1a10e5cbd25fbbb97`
- `phase4-v4-pr05-abyssal-eclipse-warning.png`: `a521a4af9b2bfb16109e5c0201e28cc2f1d6cdd8be67a64102597114b34fdf17`
- `phase4-v4-pr05-report-coverage.png`: `732682d16044867b9cfb070d05739a88f0486a8230de9c494ec5f83009c1b34f`
- `phase4-v4-pr05-app.log`: `0d37936622b88c46a438f97c49f2478f95820a5abd82755e58479dec64cf4c60`

## Metadata 核验

- 所有截图 metadata 均包含 `capture_mode=macos-window-id`。
- 所有截图 metadata 均包含 `target_pid=85691` 与 `window_pid=85691`。
- 所有截图 metadata 均包含 `window_owner=K-ToME`。
- 所有截图 metadata 均包含 `window_bounds=137,82,1280,828`。

## 备注

- 本轮完成 packaged app + Computer Use 白盒验证，覆盖三个 boss variant 的 warning、phase entered、行动反馈和 evidence summary。
- Computer Use 当前没有音频采集能力；本记录不伪造主观听感或录音证据。音频 cue 只按 telegraph `audioProfile`、audio manifest key 和 `audioLint`/owner gate 链路做结构性核验。
- owner gates 仍以 `bossHarness`、`reportPhase4Only`、`reportPhase4`、`goldenScreenshot` 和 `clientSmoke` 为准，人工白盒不替代自动化 gate。
