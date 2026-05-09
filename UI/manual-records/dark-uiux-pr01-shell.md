# Dark UI/UX PR-01 Shell Manual Record

## Scope

- PR: `dark-uiux-pr01-client-shell-layout`
- App: `client/build/release/K-ToME.app`
- Bundle id: `com.ktome.client`
- Runtime homes:
  - Clean run: `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/runtime-home`
  - Continue-unavailable run: `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/runtime-home-unavailable`
  - Fix rerun: `build/whitebox/dark-uiux-pr01-client-shell-layout-fix-20260507/runtime-home`
- Evidence dir: `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence`
- Fix evidence dir: `build/whitebox/dark-uiux-pr01-client-shell-layout-fix-20260507/evidence`
- Whitebox status: `completed-passed-after-fix`

## Verification Source

- `UI/pr/dark-uiux-pr01-client-shell-layout.md`
- `docs/computer-use-whitebox-flow.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`

## Commands Run

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --stacktrace
./gradlew :client:packageMacApp --rerun-tasks
./gradlew :client:test --tests 'com.ktome.client.screen.StandaloneScreenLayoutTest.validation setup layout keeps list and active pack summary above footer' --tests com.ktome.client.screen.ValidationSetupControllerTest
./gradlew :client:test --tests com.ktome.client.screen.StandaloneScreenLayoutTest --tests com.ktome.client.screen.ValidationSetupControllerTest
./gradlew :client:packageMacApp
./gradlew maintainabilityLint
./gradlew :client:test --tests com.ktome.client.screen.ContinueUnavailablePayloadFormatterTest --tests com.ktome.client.GameAppLifecycleTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest
./gradlew :client:goldenScreenshot
./gradlew verifyChanged
./gradlew :client:clientSmoke
```

Result:

- `:client:packageMacApp`: passed.
- `:client:test --tests com.ktome.client.assets.ManifestResolveTest --stacktrace`: passed.
- `:client:packageMacApp --rerun-tasks`: passed.
- `:client:test --tests 'com.ktome.client.screen.StandaloneScreenLayoutTest.validation setup layout keeps list and active pack summary above footer' --tests com.ktome.client.screen.ValidationSetupControllerTest`: passed after the fix.
- `:client:test --tests com.ktome.client.screen.StandaloneScreenLayoutTest --tests com.ktome.client.screen.ValidationSetupControllerTest`: passed after aligning talent tones to `UI/ART_STYLE_BIBLE.md`.
- `:client:packageMacApp`: passed after the fix.
- `maintainabilityLint`: passed after the fix.
- `:client:test --tests com.ktome.client.screen.ContinueUnavailablePayloadFormatterTest --tests com.ktome.client.GameAppLifecycleTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest`: passed after redacting local save paths and capping outcome body rows inside the primary action stack.
- `:client:goldenScreenshot`: passed after updating the outcome recap hashes for the intentional standalone chrome/layout change.
- `verifyChanged`: passed; impact routing covered contract/keyword/maintainability and the Phase 4 owner harnesses selected by the changed files.
- `:client:clientSmoke`: passed.

Note:

- The first Computer Use attach hit an older `com.ktome.client` process from a historical broken-manifest app under `build/whitebox/phase4-uiux-pr03-item-content-states-cua/`.
- That process was killed, and the current packaged app was relaunched with `open -n client/build/release/K-ToME.app`.
- All accepted screenshots below use `capture_mode=macos-window-id`; metadata `window_pid` matches the current packaged app PID for each run.

## Evidence

| Label | Evidence | Result | Observation |
| --- | --- | --- | --- |
| `dark-uiux-pr01-home-main-menu` | `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence/dark-uiux-pr01-home-main-menu.png` | PASS | Home screen opens directly to the actionable main menu. Dark header, action stack, language/footer, and player creation summary are visible. |
| `dark-uiux-pr01-home-new-run` | `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence/dark-uiux-pr01-home-new-run.png` | PASS | Profession selection changed by keyboard; player creation panel keeps fixed dark regions and does not push the footer. |
| `dark-uiux-pr01-continue-unavailable` | `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence/dark-uiux-pr01-continue-unavailable.png` | PASS_AFTER_FOLLOWUP_FIX | Corrupted save makes Continue disabled and visible; copy detail works. Follow-up formatter coverage now verifies the copied payload redacts the local save directory as `<local-save-dir>/run-save.json`. |
| `dark-uiux-pr01-validation-entry` | `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence/dark-uiux-pr01-validation-entry.png` | PASS | Validation Mode is a first-class focusable main-menu action, not a debug-only link. |
| `dark-uiux-pr01-validation-setup` | `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence/dark-uiux-pr01-validation-setup.png` | FAIL_BEFORE_FIX | Setup page was reachable, but option rows overlapped in Chinese at 1280x800; `Scenario`, profession, race, seed, zone, and floor lines were not fully readable. |
| `dark-uiux-pr01-validation-setup-after-fix` | `build/whitebox/dark-uiux-pr01-client-shell-layout-fix-20260507/evidence/dark-uiux-pr01-validation-setup-after-fix.png` | PASS | Setup page now renders the option list in two columns; zh-CN rows and footer controls are readable at 1280x800 with no observed row overlap. |
| `dark-uiux-pr01-shell-1280x800` | `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence/dark-uiux-pr01-shell-1280x800.png` | PASS | In-run shell shows left rail, central map, right panel, bottom HUD, grouped hotkeys, and log area. Old duplicated lower-right status bar was not observed. |
| `dark-uiux-pr01-shell-min-window` | `build/whitebox/dark-uiux-pr01-client-shell-layout-20260507/evidence/dark-uiux-pr01-shell-min-window.png` | PASS | At captured bounds `1024x768`, shell regions remain visible and HUD/log text does not overlap the side panels. |

## Metadata Check

- `dark-uiux-pr01-home-main-menu.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=39484`, `window_bounds=137,82,1280,828`
- `dark-uiux-pr01-home-new-run.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=39484`, `window_bounds=137,82,1280,828`
- `dark-uiux-pr01-validation-entry.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=39484`, `window_bounds=137,82,1280,828`
- `dark-uiux-pr01-validation-setup.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=39484`, `window_bounds=137,82,1280,828`
- `dark-uiux-pr01-shell-1280x800.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=39484`, `window_bounds=137,82,1280,828`
- `dark-uiux-pr01-shell-min-window.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=39484`, `window_bounds=137,82,1024,768`
- `dark-uiux-pr01-continue-unavailable.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=41173`, `window_bounds=137,82,1280,828`
- `dark-uiux-pr01-validation-setup-after-fix.png.metadata.txt`: `capture_mode=macos-window-id`, `window_pid=51516`, `window_bounds=137,82,1280,828`, sha256 `38340c2d38b017151181acee96faebc252af8235687144d71824a16f2a9503e1`

## Findings

1. `FIXED`: `ValidationSetupScreen` was not readable enough in zh-CN at 1280x800. The option body stacked multiple rows with insufficient vertical spacing, causing visible text overlap. The follow-up implementation now renders the option list in two columns and keeps footer controls inside the footer panel.
2. `FIXED`: Continue unavailable copy-detail payload previously included a machine-specific save path. The formatter now emits `<local-save-dir>/<save-file>`, and lifecycle coverage verifies the original absolute path is absent from the copied text.

## Residual Risk

- The packaged Computer Use whitebox screenshots were not rerun after the review follow-up fixes; the follow-up scope is covered by focused tests, `:client:goldenScreenshot`, `:client:clientSmoke`, and `verifyChanged`.
- Broader camera recentering for oversized maps remains deferred because it changes shell viewport semantics beyond this review follow-up.
