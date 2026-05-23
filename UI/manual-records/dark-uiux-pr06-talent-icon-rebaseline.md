# Dark UI/UX PR06 Talent Icon Rebaseline Manual Record

## Summary

| Field | Value |
| --- | --- |
| result | `PASS_LIMITED_EXISTING_PR04_CAPTURE` |
| date | 2026-05-23 |
| runner | Codex Computer Use packaged-app whitebox |
| source scenario | `dark-uiux-pr04-profession-tree-ui` |
| limitation | Existing capture proves talent panel readability and color-blind state distinction, but it is not a fresh PR06 packaged scenario rerun. |

## Evidence

| Evidence | Path | SHA-256 | Result |
| --- | --- | --- | --- |
| live talent panel | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-talent-icon-rebaseline-live.png` | `849db179f043db35887c0c37ef3c8219b1d4fb783426d075581e191074ac67e5` | `PASS_LIMITED` |
| protanopia simulation | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-talent-icon-rebaseline-protanopia.png` | `5fae7af268d0b3671ad24732b0f2d8ad62e25b165c10f98e4b0f813555d11c3d` | `PASS_LIMITED` |
| deuteranopia simulation | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-talent-icon-rebaseline-deuteranopia.png` | `77a7094a62781fcb84eb45167001f950daabaaca5e7a6717cec2db8d7b437713` | `PASS_LIMITED` |
| tritanopia simulation | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-talent-icon-rebaseline-tritanopia.png` | `aff3e6ec0e97767fc98d201967244d643639b8d45fde9640cfa6c0d7dbd8fd6b` | `PASS_LIMITED` |

## Findings

1. Learned, learnable, locked, and reserve states remain distinguishable by shape/text in the captured talent panel.
2. Color-blind simulations preserve state separation on this panel.
3. This record does not replace the PR06 dedicated packaged run for status fold, quest marker, damage float, and long-list validation overlay.
