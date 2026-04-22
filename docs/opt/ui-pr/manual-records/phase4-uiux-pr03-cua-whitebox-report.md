# Phase 4 UI/UX PR03 Computer Use White-Box Report

## Scope

- PR doc: `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md`
- Flow doc: `docs/computer-use-whitebox-flow.md`
- Target app: `client/build/release/K-ToME.app`
- CUA target: `com.ktome.client`
- Locale/window: `zh-CN`, `1280x800` logical app window
- Validation preset/seed: `LOOT_LAB`, `20260413`
- Git HEAD: `9ebb0946`

## Important Evidence Boundary

An earlier CUA attempt auto-launched `/Applications/K-ToME.app`; those screenshots are excluded from signoff. The accepted evidence is from the repo-packaged app only. Valid screenshots have `capture_mode=macos-window-id` and metadata `window_owner=K-ToME` with target PIDs listed in the launch evidence.

## Launch Evidence

- Packaged app launch: `pgrep-packaged-open-after-launch.txt` shows PID `29432` at `client/build/release/K-ToME.app/Contents/MacOS/K-ToME`.
- Broken-manifest app launch: `pgrep-broken-manifest-after-launch.txt` shows PID `32145` at the isolated `broken-manifest-app-20260422T144934Z/K-ToME-broken.app/Contents/MacOS/K-ToME`.
- Cleanup: `pgrep-final-app-cleanup.txt` is empty for this white-box app path after teardown.

## Input Sequence

1. Main menu: select `验证模式`.
2. Validation setup: cycle preset to `掉落实验室`, confirm `Seed: 20260413`, start validation session.
3. Game screen: open validation overlay with `F9`.
4. Overlay: navigate to reward/item section; execute `Validation reward`, then switch action and execute `Validation item`.
5. Game screen: open inventory with `I`; capture item quality/card presentation.
6. Inventory: drop two items with `D`, close inventory with `I`; capture stacked ground-loot marker on player cell.
7. Ground loot: pick up one item with `G`; capture single ground-loot marker on player cell.
8. Broken manifest app: launch modified packaged app with invalid `manifests/visual-manifest.json`; capture recoverable manifest error actions.
9. Error screen: press `C`; capture clipboard payload via `pbpaste`.

## Evidence Matrix

| Requirement | Result | Evidence |
| --- | --- | --- |
| Packaged app and main menu target | PASS | `phase4-uiux-pr03-packaged-main-menu.png` plus metadata PID `29432` |
| `LOOT_LAB / 20260413` setup | PASS | `phase4-uiux-pr03-packaged-validation-loot-lab-setup.png` |
| Loading completes into playable screen and empty ground panel is readable | PASS | `phase4-uiux-pr03-packaged-loot-lab-loaded-empty-ground.png` |
| Validation overlay shows PR03 route/seed context | PASS | `phase4-uiux-pr03-packaged-validation-overlay-loot-lab.png` |
| Shared card/reward presentation path | PASS | `phase4-uiux-pr03-packaged-card-shared-reward-validation.png` |
| Item quality/inventory card presentation | PASS | `phase4-uiux-pr03-packaged-inventory-quality-card.png` |
| Ground-loot stack and player-standing marker | PASS | `phase4-uiux-pr03-packaged-ground-loot-stack-player-standing.png` |
| Ground-loot single marker and player-standing marker | PASS | `phase4-uiux-pr03-packaged-ground-loot-single-player-standing.png` |
| Manifest error exposes Retry / Back To Menu / Copy Error Detail | PASS | `phase4-uiux-pr03-packaged-error-manifest-actions.png` |
| Copy Error Detail writes diagnostic payload | PASS | `phase4-uiux-pr03-error-copy-detail-payload.txt` |

## Notes

- Live CUA directly covered single and 2-item stack ground loot. The `9+` badge remains covered by the existing golden/canvas owner evidence because the live validation overlay does not expose a direct bulk-spawn command for ten items.
- Live CUA directly covered reward and inventory item cards. Shop card sharing remains covered by source/test owner evidence; this run did not route a live packaged session into a shop room.
- The broken-manifest app is an isolated copy under `build/whitebox/...`; the repo packaged app was not modified.
