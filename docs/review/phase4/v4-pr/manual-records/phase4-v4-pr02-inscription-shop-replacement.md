# Phase4 v4 Fast Whitebox Manual Record - phase4-v4-pr02

- Packaged app path: `client/build/release/K-ToME.app/Contents/MacOS/K-ToME`
- App executable SHA-256: `a5681291353cf6aa44a87a82b7a41e82e55d4ffcc04a1e45c6c216616997bc87`
- Runtime home: `build/whitebox/phase4-v4-pr02/runtime-home`
- Scenario id: `phase4-v4-pr02`
- Seed: `2026042432`
- Preset: `LOOT_LAB`
- CUA steps: `build/whitebox/phase4-v4-pr02/cua-runbook.md`
- Computer Use target: `com.ktome.client`
- App pids: `37235` for primary/install/replacement success evidence; `39273` for reject/no-shard-loss evidence
- Manual record path: `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr02-inscription-shop-replacement.md`
- Screenshot paths: `build/whitebox/phase4-v4-pr02/evidence`
- Log path: `build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-app.log`
- Conclusion: `PASS`

## Evidence

- `build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-start-inscriptions.png`
  - sha256: `3717ea9cb81b95bd8e556636a0dc824e86d56dc28d28d4c70dfc672eaaf3adcd`
  - metadata: `target_pid=37235`, `window_pid=37235`, `window_owner=K-ToME`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-install-third-slot.png`
  - sha256: `8a21b74ab3ce0280675c6699a486d807a513433cc061fd3c4e94ac65f2460be0`
  - metadata: `target_pid=37235`, `window_pid=37235`, `window_owner=K-ToME`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-replacement-modal.png`
  - sha256: `f1e947e9afc70dde9cfb1653ab8e1b8f5d817596707bb422c3511e251681702e`
  - metadata: `target_pid=37235`, `window_pid=37235`, `window_owner=K-ToME`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-replace-keep-hotkey.png`
  - sha256: `ec7efd11db2f85df65c2d43b2564d1981bc866ea53d64b242c727ca5110feb3c`
  - metadata: `target_pid=37235`, `window_pid=37235`, `window_owner=K-ToME`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-reject-no-gold-loss.png`
  - sha256: `e2cab85123ca33acdf14f3a01eb190e9d640d9c90c742c075b0d0f3eb12c2411`
  - metadata: `target_pid=39273`, `window_pid=39273`, `window_owner=K-ToME`, `capture_mode=macos-window-id`
- `build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-app.log`

## Notes

- `:client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr02` completed successfully before CUA execution.
- `F9, Enter` executed `prepare-primary-scene`; the packaged app showed rogue starter inscriptions as `healing_light / phase_door` on hotkeys `5 / 6`, with shards at `200`.
- In the primary shop, `Down, Down, Enter` bought an inscription into the third slot; the screenshot shows shards decreased from `200` to `158` and the UI log recorded the inscription purchase.
- Buying a fourth inscription in the same primary scene filled the fourth slot before the replacement verification path.
- `F9, Right, Enter, Down, Down, Enter` executed `prepare-secondary-scene` and opened the replacement modal for a full-slot inscription purchase. The modal shows the candidate, current `5-8` slots, category delta, price, and replacement controls.
- Selecting hotkey `5` and confirming replaced the existing slot while keeping hotkey `5`; shards decreased once and the UI log recorded the replacement.
- The reject path was captured from a fresh packaged app launch of the same scenario. Selecting an inscription already present in the target slot displayed the rejection reason in the replacement modal, with shards still at `200`.
- The generated runbook names the final screenshot `phase4-v4-pr02-reject-no-gold-loss.png`; this is the same no-shard-loss check required by the PR document.
- Fast whitebox evidence does not replace owner gates.
