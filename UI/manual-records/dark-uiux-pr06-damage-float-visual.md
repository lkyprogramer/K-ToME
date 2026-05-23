# Dark UI/UX PR06 Damage Float Visual Manual Record

| Field | Value |
| --- | --- |
| result | `NOT_COVERED` |
| date | 2026-05-23 |
| runner | Codex Computer Use packaged-app whitebox |
| reason | The available packaged PR04 scenario did not provide a stable boss/combat path with repeated damage floats. |

## Required PR06 Check

PR06 requires same-screen comparison of damage type icon, damage float icon, status icon, and skill icon at runtime sizes. This run did not reach a state that proves that contract.

## Evidence

| Evidence | Path | Result |
| --- | --- | --- |
| shell skill / quest overview | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-status-quest-skill-overview-live.png` | `INSUFFICIENT_FOR_DAMAGE_FLOAT` |
| inventory equipment workbench | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-inventory-equipment-workbench-live.png` | `INSUFFICIENT_FOR_DAMAGE_FLOAT` |

## Follow-Up

Add or reuse a PR06/PR07 packaged scenario that starts in a combat state with repeated damage float emission and status application, then capture the target window at 16-24px runtime float scale.
