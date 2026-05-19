# Dark UIUX PR04-01 Playable Profession Passive Talents Manual Record

## Summary

- date: `2026-05-19`
- app: `client/build/release/K-ToME.app`
- runtime mode: packaged app + Computer Use target window + repo window-id screenshot capture
- result: `LIMITED_PASS`
- limitation: required screenshot evidence and sidecars are complete, but required runtime log keys were not captured in `*-app.log`.

## Blocking Fixes Applied Before Execution

- `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt`
  - Added presentation entries for the four PR04-01 scenario ids.
- `game/src/main/resources/i18n/en-US.json`
  - Added missing PR04-01 validation scenario title tokens.
- `game/src/main/resources/i18n/zh-CN.json`
  - Added missing PR04-01 validation scenario title tokens.
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
  - Added whitebox-only app-log append support for validation scenario log events.

Initial packaged launch failed with `MISSING_PHASE4_V4_SCENARIO_PRESENTATION`; the failure screenshot was retained at `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/launch-error-missing-presentation.png`.

## Materialization And Build

- command: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-effective-hp-regen-detail`
- result: passed
- artifact roots:
  - `build/whitebox/dark-uiux-pr04-01-static-passive-detail`
  - `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail`
  - `build/whitebox/dark-uiux-pr04-01-passive-action-suppression`
  - `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail`

## Scenario Evidence

### `dark-uiux-pr04-01-static-passive-detail`

- preset: `MAPGEN_DIFF`
- seed: `202605170401`
- locale: `ZH_CN`
- profession: `vanguard`
- targetTalentId: `unyielding`
- CUA input sequence: `F9, Enter, Esc, T`, `P`, `P`, `Enter`
- result: screenshot evidence captured
- evidence:
  - `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-detail-static.png`
  - `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-next-preview.png`
  - `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-preview-collapsed-after-toggle.png`
  - `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-after-learn-no-slot-modal.png`
  - `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-app.log`

### `dark-uiux-pr04-01-trigger-passive-detail`

- preset: `MAPGEN_DIFF`
- seed: `202605170402`
- locale: `ZH_CN`
- profession: `arcanist`
- targetTalentId: `mana_surge`
- CUA input sequence: `F9, Enter, Esc, T`, `P`, `P`, `Down`, `Up, Enter`
- result: screenshot evidence captured
- evidence:
  - `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-detail-trigger.png`
  - `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-next-preview.png`
  - `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-panel-entry.png`
  - `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-cast-speed-effective-detail.png`
  - `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-after-learn-no-slot-modal.png`
  - `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-app.log`

### `dark-uiux-pr04-01-passive-action-suppression`

- preset: `MAPGEN_DIFF`
- seed: `202605170403`
- locale: `ZH_CN`
- profession: `vanguard`
- targetTalentId: `bulwark_march`
- CUA input sequence: `F9, Enter, Esc, T`, `P`, `R`, `Esc`
- result: screenshot evidence captured
- evidence:
  - `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-conditional-detail-before-r.png`
  - `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-preview-expanded.png`
  - `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-no-active-slot-modal.png`
  - `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-log-no-reserve.png`
  - `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-suppression-app.log`

### `dark-uiux-pr04-01-effective-hp-regen-detail`

- preset: `MAPGEN_DIFF`
- seed: `202605170404`
- locale: `ZH_CN`
- profession: `berserker`
- targetTalentId: `pain_fuel`
- CUA input sequence: `F9, Enter, Esc, T`, `P`, `P`, `Enter`
- result: screenshot evidence captured
- evidence:
  - `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-effective-detail.png`
  - `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-effective-preview.png`
  - `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-preview-collapsed.png`
  - `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-after-learn-no-slot-modal.png`
  - `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-app.log`

## Evidence Integrity Check

- command: `python3 - <<'PY' ... required evidence / sidecar / log-fragment check ... PY`
- screenshot files: all required files exist
- screenshot sidecars: all required `.metadata.txt` and `.sha256` files exist
- screenshot metadata: generated by `scripts/capture-macos-app-window.sh` with `capture_mode=macos-window-id` and `window_owner=K-ToME`
- forbidden log fragments: absent from `*-app.log`
- missing required log keys:
  - `dark-uiux-pr04-01-static-passive-detail`: `log.validation.phase4_v4.action`, `log.talent.learned`
  - `dark-uiux-pr04-01-trigger-passive-detail`: `log.validation.phase4_v4.action`, `log.talent.learned`
  - `dark-uiux-pr04-01-passive-action-suppression`: `log.validation.phase4_v4.action`
  - `dark-uiux-pr04-01-effective-hp-regen-detail`: `log.validation.phase4_v4.action`, `log.talent.learned`

## Automated Evidence

- command: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.input.ValidationCommandSourceTest :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-effective-hp-regen-detail`
- result: passed
- command: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-effective-hp-regen-detail`
- result: passed

## Residual Risk

The visual whitebox surface was exercised through Computer Use and target-window screenshots. The PR04-01 log evidence contract is not fully closed because `*-app.log` does not contain the required runtime token keys. Until the packaged launch path reliably routes scenario runtime events into the app log, this manual record must remain `LIMITED_PASS`, not full pass.
