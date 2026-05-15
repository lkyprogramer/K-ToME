# UI-demo-new Visual Parity Manual Record

scenarioId: dark-uiux-pr02-1-demo-shell-foundation
ownerPr: PR-02-2
reviewedAt: 2026-05-15T00:04:43+0800
manualReviewers:
  - role: UI Director
    verdict: pass
    basis: upright 1672x941 and 1280x800 golden evidence keep left rail, map stage, right panel, and bottom hero/action/log hierarchy with no duplicate bottom command hints.
  - role: Art Director
    verdict: pass
    basis: dark-v1 ruins terrain, vanguard actor, stairs prop, and subdued map-stage backdrop now read as a warm dark dungeon first screen without the previous over-bright brick backdrop.
  - role: Game Design Director
    verdict: pass
    basis: right panel keeps real equipment, inscriptions 5-8 with content, inscriptions 9-12 as empty framed rows, a single-page 4x2 launch backpack, separate pagination evidence, and compact operation hints in one consistent command surface.
demoParityVerdict: pass
blockingFindings: []
evidenceMode: upright-full-frame-plus-dedicated-crops
screenshotLabelCoverage:
  - ui-demo-new-parity-1672x941.png
  - ui-demo-new-parity-1280x800.png
  - ui-demo-new-right-panel-grid.png
  - ui-demo-new-bottom-deck-no-command-hints.png
  - ui-demo-new-inventory-page-1.png
  - ui-demo-new-inventory-page-2.png
  - ui-demo-new-nav-rail-crop.png
  - ui-demo-new-map-stage-crop.png

## Acceptance Notes

- Director-level review passed against `UI/UI-demo-new.png`; no remaining blocker is carried as PR-05 deferred for the first screen.
- `:client:goldenScreenshot --rerun-tasks` passed and writes the full PR02-1 evidence label set under `client/build/reports/golden/dark-uiux-pr02-1`.
- Current visual signoff is based on deterministic golden evidence and the side-by-side artifact `build/reports/verification/dark-uiux/ui-demo-new-side-by-side-current.png`.
- Packaged app smoke capture was not refreshed in this pass; no packaged visual pass is claimed here.
- Right panel, bottom deck, nav rail, map stage, and inventory page evidence are dedicated upright crops, not same-frame duplicate hashes.
