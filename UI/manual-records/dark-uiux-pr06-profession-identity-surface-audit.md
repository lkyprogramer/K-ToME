# Dark UI/UX PR06 Profession Identity Surface Audit

## Summary

| Field | Value |
| --- | --- |
| result | `PASS_RESOURCE_GATE_LIMITED_RUNTIME_QA_PENDING` |
| date | 2026-05-23 |
| source data | `game/src/main/resources/data/professions/index.yaml` |
| final-full inventory | `UI/sprite-sheets/dark-v1-final-full-inventory.json` |

## Profession Icon Disposition

| Profession | Unlock state | Schema iconKey | PR06 disposition |
| --- | --- | --- | --- |
| `vanguard` | `RELEASE_UNLOCKED` | `icon.profession.vanguard` | Required dark-v1 key covered by PR06 final-full inventory. |
| `arcanist` | `RELEASE_UNLOCKED` | `icon.profession.arcanist` | Required dark-v1 key covered by PR06 final-full inventory. |
| `rogue` | `RELEASE_UNLOCKED` | `icon.profession.rogue` | Required dark-v1 key covered by PR06 final-full inventory. |
| `templar` | `RELEASE_UNLOCKED` | `icon.profession.templar` | Required dark-v1 key covered by PR06 final-full inventory. |
| `berserker` | `DEV_UNLOCKED` | `icon.profession.berserker` | Dev playable profession now has a dedicated PR06 dark-v1 key. |
| `spellblade` | `DEV_UNLOCKED` | `icon.profession.spellblade` | Dev playable profession now has a dedicated PR06 dark-v1 key. |
| `shadowblade` | `LOCKED` | `icon.profession.rogue` | Frozen profession; not treated as a separate player-visible profession icon key in PR06. |
| `warden` | `LOCKED` | `icon.profession.templar` | Frozen profession; not treated as a separate player-visible profession icon key in PR06. |

## Findings

1. Release and dev playable profession identities are now covered by six dedicated `icon.profession.*` keys in `r09-quest-zone-profession`.
2. Frozen professions remain locked and reuse visible release-family fallback identities; they do not add synthetic PR06 profession icon keys until a playable-profession PR gives them a player-visible surface.
3. The required 128 / 48 / 24 profession icon runtime-size QA remains manual evidence, not a schema/coverage result.

## Follow-Up

Before PR-06 close, rerun packaged whitebox and replace this limited record with screenshots or hashes proving profession icon readability at `128 / 48 / 24` runtime sizes.
