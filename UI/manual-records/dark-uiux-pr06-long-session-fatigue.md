# Dark UI/UX PR06 Long-Session Fatigue Manual Record

## Summary

| Field | Value |
| --- | --- |
| result | `NOT_COVERED_60MIN_RERUN_REQUIRED` |
| date | 2026-05-23 |
| runner | Codex Computer Use packaged-app whitebox |
| app | `build/whitebox/dark-uiux-pr04-profession-tree-ui/runtime-app/K-ToME.app` |
| app pid | `95462` |
| scenario | `dark-uiux-pr04-profession-tree-ui` |
| runtime home | `build/whitebox/dark-uiux-pr04-profession-tree-ui/runtime-home` |

## Timestamped Log

| Time | Action | Evidence | Result |
| --- | --- | --- | --- |
| 2026-05-23 10:44 CST | Launched packaged PR04 profession-tree scenario and captured shell overview. | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-status-quest-skill-overview-live.png` | `LIMITED_PASS` |
| 2026-05-23 10:44 CST | Opened talent allocation panel with `T`. | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-talent-icon-rebaseline-live.png` | `LIMITED_PASS` |
| 2026-05-23 10:46 CST | Captured minute 0 long-session image. | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-long-session-minute-00.png` | `CAPTURED` |
| 2026-05-23 10:46 CST | Opened inventory workbench with `I`. | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-inventory-equipment-workbench-live.png` | `LIMITED_PASS` |

## Required Screenshots

| Minute | Path | SHA-256 | Status |
| --- | --- | --- | --- |
| 0 | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-long-session-minute-00.png` | `26bb590a5abefb6f6d693b067e293e42672fb11608125fa00dae8f9cd4242199` | `CAPTURED` |
| 30 | `N/A` | `N/A` | `NOT_CAPTURED_YET` |
| 60 | `N/A` | `N/A` | `NOT_CAPTURED_YET` |

## Fatigue / Readability Findings

1. The PR04 scenario does not cover two profession switches, three combat encounters, quest completion, zone transition, or long-list validation overlay, so it cannot satisfy PR06 §6.7 by itself.
2. The bottom right quest/log panel is narrow; long validation/scenario text wraps aggressively, which remains a PR06 overlay readability risk until the dedicated PR06 scenario is rerun with long-list evidence.
3. The same-screen shell uses multiple cyan/ember focus cues, but current captured surfaces do not exceed the visible strength budget by inspection; this remains `LIMITED` because telegraph and damage-float surfaces were not exercised.

## Reviewer Acknowledgement

`NOT_COVERED`: this record should not be acknowledged as a PR06 long-session pass. The dedicated `dark-uiux-pr06-status-quest-skill-overview` scenario now exists, but §6.7 still needs a fresh packaged long-session rerun before reviewer sign-off.
