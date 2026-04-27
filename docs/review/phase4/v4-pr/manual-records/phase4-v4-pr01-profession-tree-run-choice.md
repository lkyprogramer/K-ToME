# Phase4 v4 Fast Whitebox Manual Record - phase4-v4-pr01

- Packaged app path: `client/build/release/K-ToME.app/Contents/MacOS/K-ToME`
- App executable SHA-256: `a385e4c2112b38dc3969706ba74add979333ff285386f8be1f3acc969a18b855`
- Runtime home: `build/whitebox/phase4-v4-pr01/runtime-home`
- Scenario id: `phase4-v4-pr01`
- Seed: `2026042431`
- Preset: `MAPGEN_DIFF`
- CUA steps: `build/whitebox/phase4-v4-pr01/cua-runbook.md`
- Computer Use target: `com.ktome.client`
- App pid: `53038`
- Manual record path: `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md`
- Screenshot paths: `build/whitebox/phase4-v4-pr01/evidence`
- Log path: `build/whitebox/phase4-v4-pr01/evidence/phase4-v4-pr01-app.log`
- Conclusion: `PASS`

## Evidence

- `build/whitebox/phase4-v4-pr01/evidence/phase4-v4-pr01-talent-tree-start.png`
  - sha256: `aa26995919c4640bd05d53176fa1b4d983148dc848ad2f77fe8ee197d11343c8`
  - metadata: `target_pid=53038`, `window_pid=53038`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr01/evidence/phase4-v4-pr01-learnable-confirm.png`
  - sha256: `d81df20f8bd73e2a3fa4e79dbaee6858af47a140d2482bb613b37f9991885e96`
  - metadata: `target_pid=53038`, `window_pid=53038`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr01/evidence/phase4-v4-pr01-tier3-locked-reason.png`
  - sha256: `9dd758065f996be864517012428fdcc945cc6c5892c670d09b10ab428d5ab9f6`
  - metadata: `target_pid=53038`, `window_pid=53038`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr01/evidence/phase4-v4-pr01-reserve-active-slot.png`
  - sha256: `5137c88a168451979a0fc16c473d33d8e3defbca9246dd5e898ad9241f70d2e1`
  - metadata: `target_pid=53038`, `window_pid=53038`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr01/evidence/phase4-v4-pr01-app.log`

## Notes

- `prepare-primary-scene` produced the vanguard starter-only target: three starter talents, empty fourth active slot, one talent point, learnable non-starter nodes, and locked higher-tier nodes.
- `Down, Enter, Enter` from the primary target learned `charge`, consumed the point after confirmation, and bound `charge` to the fourth active slot.
- The locked tier screenshot shows `linebreaker` selected with concrete lock requirements.
- `prepare-secondary-scene` materialized the reserve-choice target directly: `charge` occupied the fourth active slot and `sunder_armor` was pending as a new active talent.
- The reserve screenshot was captured after `F9, Right, Enter, Esc, T, Enter`; it shows `ACTIVE_TALENT_SLOT_CHOICE` with replacement slots, reserve action, and cancel action visible.
- All four required PNG files were captured from the same packaged app pid `53038`.
- Fast whitebox evidence does not replace owner gates.
