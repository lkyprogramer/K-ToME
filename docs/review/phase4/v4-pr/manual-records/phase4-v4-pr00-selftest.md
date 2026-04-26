# Phase4 v4 Fast Whitebox Manual Record - phase4-v4-pr00-selftest

- Time: `2026-04-25`
- Packaged app path: `client/build/release/K-ToME.app/Contents/MacOS/K-ToME`
- App executable SHA-256: `dfcf11c13fe88911c851e39eecfb20c4248ca0aa7e1db49fbabb4fef0e80d931`
- Runtime home: `build/whitebox/phase4-v4-pr00-selftest/runtime-home`
- Scenario id: `phase4-v4-pr00-selftest`
- Seed: `2026042430`
- Preset: `MAPGEN_DIFF`
- CUA steps: `build/whitebox/phase4-v4-pr00-selftest/cua-runbook.md`
- Computer Use target: `com.ktome.client`
- Packaged app pid: `50219`
- Manual record path: `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr00-selftest.md`
- Screenshot paths: `build/whitebox/phase4-v4-pr00-selftest/evidence`
- Log path: `build/whitebox/phase4-v4-pr00-selftest/evidence/app.log`
- Conclusion: `PASS`

## Evidence

- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-scenario-bootstrap.png`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-scenario-bootstrap.png.metadata.txt`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-scenario-bootstrap.png.sha256`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-primary-scene.png`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-primary-scene.png.metadata.txt`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-primary-scene.png.sha256`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-secondary-scene.png`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-secondary-scene.png.metadata.txt`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-secondary-scene.png.sha256`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-evidence-summary.png`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-evidence-summary.png.metadata.txt`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-evidence-summary.png.sha256`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/app.log`
- `build/whitebox/phase4-v4-pr00-selftest/evidence/app.pid`

## CUA Steps

| Step | Input | Expected visible result | Result |
| --- | --- | --- | --- |
| 1 | `build/whitebox/phase4-v4-pr00-selftest/launch-packaged-app.sh` | Packaged app opens validation session | `PASS` |
| 2 | `F9` | `PHASE4_V4_FAST` section is selected | `PASS` |
| 3 | `Enter` | Primary scenario action returns ok | `PASS` |
| 4 | `Right, Enter` | Secondary scenario action returns ok | `PASS` |
| 5 | `Right, Enter` | Evidence summary shows expected paths, freshness, and app hash | `PASS` |

## Metadata Check

| Evidence | capture_mode | window_owner | window_pid | window_bounds |
| --- | --- | --- | ---: | --- |
| `phase4-v4-pr00-scenario-bootstrap.png` | `macos-window-id` | `K-ToME` | `50219` | `137,82,1280,828` |
| `phase4-v4-pr00-primary-scene.png` | `macos-window-id` | `K-ToME` | `50219` | `137,82,1280,828` |
| `phase4-v4-pr00-secondary-scene.png` | `macos-window-id` | `K-ToME` | `50219` | `137,82,1280,828` |
| `phase4-v4-pr00-evidence-summary.png` | `macos-window-id` | `K-ToME` | `50219` | `137,82,1280,828` |

## Notes

- This record uses the generated `preparePhase4V4Whitebox` runbook and generated launch script.
- The four required screenshot files were captured with `scripts/capture-macos-app-window.sh`; Computer Use live screenshots were used only for observation and are not referenced as evidence.
- Fast whitebox evidence does not replace owner gates.
