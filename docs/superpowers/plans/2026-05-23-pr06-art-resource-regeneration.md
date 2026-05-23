# PR06 Art Resource Regeneration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Regenerate, QA, accept, slice, manifest-wire, and runtime-replace every PR06 Round 8/9 player-visible dark UI art resource before resuming white-box interaction validation.

**Architecture:** `UI/sprite-sheets/sheet-plan.yaml` remains the single mapping authority. New art enters runtime only through `prompt -> raw sheet -> contact sheet -> candidate slice -> manual/random QA -> accepted sprite report -> final runtime slice -> canonical/runtime manifest -> coverage/golden/whitebox`; prompt files, manual notes, or stopgap PNGs are not completion evidence.

**Tech Stack:** Kotlin/libGDX runtime assets, Python sprite-sheet scripts, Gradle verification tasks, Codex image generation wrapper, Pillow-based contact/multi-size QA.

---

## Current Truth

Current PR06 art state is not accepted:

- `UI/manual-records/dark-uiux-pr06-art-random-qa.json` has 53 `MANUAL_REVIEW_REQUIRED` samples.
- `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md` marks current R08/R09 raw/contact sheets as rejected or pending regeneration.
- Runtime screenshot assets are loaded from `client/src/main/resources/dark-v1/icons/*.png`; prompt rewrites alone do not affect the running client.
- Scope from current `sheet-plan.yaml`:

| Sheet | Player-visible cells | Role |
| --- | ---: | --- |
| `r08-skills-vanguard-berserker` | 52 | Vanguard/Berserker skill and talent icons |
| `r08-skills-templar-rogue` | 51 | Templar/Rogue skill and talent icons |
| `r08-skills-arcanist-spellblade` | 51 | Arcanist/Spellblade skill and talent icons |
| `r09-status-damage` | 29 | status, mutation, damage type |
| `r09-quest-zone-profession` | 36 | quest, zone, profession, tree, difficulty |
| `r09-fallback-debug` | 2 | current fallback/debug player-visible keys |
| `r09-rejected-polish` | 0 | reserved polish/rejected source; must still get reserved QA evidence |

## File Structure

Files to modify or generate:

- Modify: `UI/sprite-sheets/sheet-plan.yaml` only when QA identifies a bad subject or cell contract.
- Regenerate: `UI/sprite-sheets/prompts/dark-v1/*.prompt.txt` and `UI/sprite-sheets/prompts/dark-v1/prompt-index.json`.
- Replace after accepted generation: `assets-src/image/raw/sheets/dark-v1/{sheetId}.png`.
- Replace after accepted generation: `assets-src/image/contact-sheets/dark-v1/{sheetId}-contact.png`.
- Replace after final slice only: `client/src/main/resources/dark-v1/**`.
- Update canonical manifest if any target key/path is missing or stale: `assets-src/image/manifests/phase2-visual-manifest.json`.
- Sync runtime manifest from canonical only: `client/src/main/resources/manifests/visual-manifest.json`.
- Produce accepted map report: `assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl`.
- Update final inventory: `UI/sprite-sheets/dark-v1-final-full-inventory.json`.
- Update manual records:
  - `UI/manual-records/dark-uiux-pr06-art-random-qa.json`
  - `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md`
  - `UI/manual-records/dark-uiux-pr06-status-quest-skill-overview.md`
  - `UI/manual-records/dark-uiux-pr06-overview-screenshot.md`

Create if missing:

- `UI/manual-records/dark-uiux-pr06-sheet-art-acceptance.json`
- `build/tmp/dark-uiux-pr06-runtime-candidates/`
- `build/reports/verification/dark-uiux/random-qa/`
- `build/reports/verification/dark-uiux/pr06-art-candidate-map.jsonl`

## Hard Rules

- Do not slice rejected art into `client/src/main/resources`.
- Do not mark `MANUAL_REVIEW_REQUIRED` as pass.
- Do not use stopgap runtime PNGs to close R08/R09.
- Do not edit `row/col` to hide a bad generation.
- Do not write machine absolute paths into committed artifacts.
- If one player-visible random sample fails, the whole sheet is rejected and regenerated.
- `spriteSheetMapLint --require-reviewed-qa` and `darkManifestCoverageLint final-full` must pass before white-box validation resumes.

## Task 1: Freeze Current State And Regenerate Prompts

**Files:**
- Modify: `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md`
- Modify: `UI/sprite-sheets/prompts/dark-v1/*.prompt.txt`
- Modify: `UI/sprite-sheets/prompts/dark-v1/prompt-index.json`

- [ ] **Step 1: Record current rejected hashes**

Run:

```bash
git hash-object assets-src/image/raw/sheets/dark-v1/r08-skills-vanguard-berserker.png
git hash-object assets-src/image/raw/sheets/dark-v1/r08-skills-templar-rogue.png
git hash-object assets-src/image/raw/sheets/dark-v1/r08-skills-arcanist-spellblade.png
git hash-object assets-src/image/raw/sheets/dark-v1/r09-status-damage.png
git hash-object assets-src/image/raw/sheets/dark-v1/r09-quest-zone-profession.png
git hash-object assets-src/image/raw/sheets/dark-v1/r09-fallback-debug.png
git hash-object assets-src/image/raw/sheets/dark-v1/r09-rejected-polish.png
```

Expected: 7 hashes are available and can be copied into `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md` as rejected historical evidence.

- [ ] **Step 2: Run prompt/source lint**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkKeyRegistryLint darkSpriteSheetLint
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Regenerate prompt files from sheet plan**

Run:

```bash
python3 scripts/generate_sheet_prompt.py \
  --plan UI/sprite-sheets/sheet-plan.yaml \
  --output-dir UI/sprite-sheets/prompts/dark-v1
```

Expected: prompt files and `prompt-index.json` are rewritten from `sheet-plan.yaml`; no hand-edited prompt becomes a second truth.

## Task 2: Generate New Raw Sheets

**Files:**
- Replace: `assets-src/image/raw/sheets/dark-v1/r08-skills-vanguard-berserker.png`
- Replace: `assets-src/image/raw/sheets/dark-v1/r08-skills-templar-rogue.png`
- Replace: `assets-src/image/raw/sheets/dark-v1/r08-skills-arcanist-spellblade.png`
- Replace: `assets-src/image/raw/sheets/dark-v1/r09-status-damage.png`
- Replace: `assets-src/image/raw/sheets/dark-v1/r09-quest-zone-profession.png`
- Replace: `assets-src/image/raw/sheets/dark-v1/r09-fallback-debug.png`
- Replace: `assets-src/image/raw/sheets/dark-v1/r09-rejected-polish.png`

- [ ] **Step 1: Generate each sheet through the repository wrapper**

Run these commands one at a time:

```bash
scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/028-r08-skills-vanguard-berserker.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r08-skills-vanguard-berserker.png \
  --overwrite \
  --smoke-report build/reports/verification/dark-uiux/r08-skills-vanguard-berserker-generation-smoke.json

scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/027-r08-skills-templar-rogue.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r08-skills-templar-rogue.png \
  --overwrite \
  --smoke-report build/reports/verification/dark-uiux/r08-skills-templar-rogue-generation-smoke.json

scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/026-r08-skills-arcanist-spellblade.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r08-skills-arcanist-spellblade.png \
  --overwrite \
  --smoke-report build/reports/verification/dark-uiux/r08-skills-arcanist-spellblade-generation-smoke.json

scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/032-r09-status-damage.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r09-status-damage.png \
  --overwrite \
  --smoke-report build/reports/verification/dark-uiux/r09-status-damage-generation-smoke.json

scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/030-r09-quest-zone-profession.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r09-quest-zone-profession.png \
  --overwrite \
  --smoke-report build/reports/verification/dark-uiux/r09-quest-zone-profession-generation-smoke.json

scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/029-r09-fallback-debug.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r09-fallback-debug.png \
  --overwrite \
  --smoke-report build/reports/verification/dark-uiux/r09-fallback-debug-generation-smoke.json

scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/031-r09-rejected-polish.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r09-rejected-polish.png \
  --overwrite \
  --smoke-report build/reports/verification/dark-uiux/r09-rejected-polish-generation-smoke.json
```

Expected: every raw sheet is `1024x1024`, repo-relative path matches `sheet-plan.yaml.rawSheetPath`, smoke reports contain only repo-owned output paths plus selected-source hash metadata.

- [ ] **Step 2: Repack generated sheets into exact grid slots**

Run:

```bash
for sheet in \
  r08-skills-vanguard-berserker \
  r08-skills-templar-rogue \
  r08-skills-arcanist-spellblade \
  r09-status-damage \
  r09-quest-zone-profession \
  r09-fallback-debug \
  r09-rejected-polish
do
  python3 scripts/repack_generated_sheet.py \
    --plan UI/sprite-sheets/sheet-plan.yaml \
    --sheet-id "$sheet" \
    --overwrite
done
```

Expected: generated sources are normalized to exact `sheet-plan.yaml` grid geometry before contact/render/slice inspection.

## Task 3: Build Candidate Contact Sheets And Candidate Runtime Slices

**Files:**
- Replace: `assets-src/image/contact-sheets/dark-v1/*-contact.png`
- Create/replace: `build/tmp/dark-uiux-pr06-runtime-candidates/dark-v1/**`
- Create/replace: `build/reports/verification/dark-uiux/pr06-art-candidate-map.jsonl`

- [ ] **Step 1: Render labeled contact sheets**

Run:

```bash
python3 scripts/render_contact_sheet.py \
  --plan UI/sprite-sheets/sheet-plan.yaml \
  --output-root assets-src/image/contact-sheets/dark-v1 \
  --overwrite
```

Expected: 7 PR06 contact sheets are visible under `assets-src/image/contact-sheets/dark-v1/` with row/col/key labels.

- [ ] **Step 2: Slice into a temporary runtime candidate root**

Run:

```bash
python3 scripts/slice_spritesheet.py \
  --plan UI/sprite-sheets/sheet-plan.yaml \
  --runtime-root build/tmp/dark-uiux-pr06-runtime-candidates \
  --overwrite
```

Expected: candidate PNGs are under `build/tmp/dark-uiux-pr06-runtime-candidates/dark-v1/**`; real `client/src/main/resources` is not touched yet.

- [ ] **Step 3: Validate candidate geometry and manifest mapping**

Run:

```bash
python3 scripts/verify_sprite_sheet_map.py \
  --check map \
  --plan UI/sprite-sheets/sheet-plan.yaml \
  --runtime-root build/tmp/dark-uiux-pr06-runtime-candidates \
  --report build/reports/verification/dark-uiux/pr06-art-candidate-map.jsonl \
  --report-sheet-ids r08-skills-vanguard-berserker,r08-skills-templar-rogue,r08-skills-arcanist-spellblade,r09-status-damage,r09-quest-zone-profession,r09-fallback-debug,r09-rejected-polish
```

Expected: command passes without missing raw/contact/candidate slice, empty alpha bbox, manifest path mismatch, or duplicate output hash.

## Task 4: Deterministic Random QA And Manual Sheet Acceptance

**Files:**
- Replace: `UI/manual-records/dark-uiux-pr06-art-random-qa.json`
- Create/replace: `UI/manual-records/dark-uiux-pr06-sheet-art-acceptance.json`
- Replace: `build/reports/verification/dark-uiux/random-qa/*-random-qa.png`
- Modify: `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md`

- [ ] **Step 1: Generate deterministic random QA samples**

Run:

```bash
python3 scripts/generate_dark_art_random_qa.py \
  --sheet-ids r08-skills-vanguard-berserker,r08-skills-templar-rogue,r08-skills-arcanist-spellblade,r09-status-damage,r09-quest-zone-profession,r09-fallback-debug,r09-rejected-polish \
  --out UI/manual-records/dark-uiux-pr06-art-random-qa.json \
  --sample-root build/reports/verification/dark-uiux/random-qa \
  --raw-root assets-src/image/raw/sheets/dark-v1 \
  --contact-root assets-src/image/contact-sheets/dark-v1 \
  --overwrite
```

Expected: random QA record uses seed `dark-uiux-pr06-art-random-qa-v1`; multi-size sample images are regenerated.

- [ ] **Step 2: Manual inspect full contact sheets**

Open these files in the image viewer and inspect every player-visible cell:

```text
assets-src/image/contact-sheets/dark-v1/r08-skills-vanguard-berserker-contact.png
assets-src/image/contact-sheets/dark-v1/r08-skills-templar-rogue-contact.png
assets-src/image/contact-sheets/dark-v1/r08-skills-arcanist-spellblade-contact.png
assets-src/image/contact-sheets/dark-v1/r09-status-damage-contact.png
assets-src/image/contact-sheets/dark-v1/r09-quest-zone-profession-contact.png
assets-src/image/contact-sheets/dark-v1/r09-fallback-debug-contact.png
assets-src/image/contact-sheets/dark-v1/r09-rejected-polish-contact.png
```

Pass criteria for every visible cell:

- concrete subject matches `targetKey`
- no text, number, logo, watermark, or people
- no cross-cell bleed
- one subject per cell
- dark fantasy UI material, no clean vector sticker, no neon/glass
- skill icons read as active command silhouettes
- talent icons read as progression emblems
- status/mutation/damage use distinct visual grammar
- quest/zone/profession/tree/fallback/debug do not collapse into the same rune token language

- [ ] **Step 3: Manual inspect random multi-size samples**

Open every `build/reports/verification/dark-uiux/random-qa/*-random-qa.png`.

Required sizes:

- skills/talents: `16/24/32/48px`
- status/damage/mutation: `16/24/32px`
- quest/zone/tree/difficulty: `12/16/24/32px`
- profession: `128/48/24px`
- fallback/debug: `16/24/32/48px`
- rejected polish reserved sample: `32/48/128px`

If any sampled player-visible icon is unreadable at its required size, reject the whole sheet.

- [ ] **Step 4: Write sheet-level acceptance record**

Create `UI/manual-records/dark-uiux-pr06-sheet-art-acceptance.json` with this schema:

```json
{
  "schemaVersion": "dark-uiux-pr06-sheet-art-acceptance-v1",
  "reviewedAt": "2026-05-23T00:00:00+08:00",
  "reviewer": "Codex visual QA",
  "seed": "dark-uiux-pr06-art-random-qa-v1",
  "sheets": [
    {
      "sheetId": "r08-skills-vanguard-berserker",
      "promptPath": "UI/sprite-sheets/prompts/dark-v1/028-r08-skills-vanguard-berserker.prompt.txt",
      "rawSheetPath": "assets-src/image/raw/sheets/dark-v1/r08-skills-vanguard-berserker.png",
      "contactSheetPath": "assets-src/image/contact-sheets/dark-v1/r08-skills-vanguard-berserker-contact.png",
      "decision": "PASS",
      "rawSheetGitHash": "<fill-with-git-hash-object-output>",
      "sampleKeys": ["icon.skill.vanguard.power_strike"],
      "rejectReasons": []
    }
  ]
}
```

Replace the placeholder hash with the actual output from:

```bash
git hash-object assets-src/image/raw/sheets/dark-v1/r08-skills-vanguard-berserker.png
```

Expected: every sheet has `decision: PASS` before the final runtime slice. Any `REJECT` means return to Task 1 Step 3 for subject rewrite and Task 2 for regeneration.

## Task 5: Promote Accepted QA Into Sprite Map Report

**Files:**
- Create/modify: `scripts/apply_dark_sheet_art_acceptance.py`
- Test: `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`
- Replace: `assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl`

- [ ] **Step 1: Add a helper that promotes only hash-matched accepted sheets**

The helper must:

- read `build/reports/verification/dark-uiux/pr06-art-candidate-map.jsonl`
- read `UI/manual-records/dark-uiux-pr06-sheet-art-acceptance.json`
- fail if any sheet is not `PASS`
- fail if a raw sheet git hash differs from the acceptance record
- write `assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl`
- set `qaStatus: "ACCEPTED"` only for records whose sheet is accepted and hash-matched
- preserve `reviewer`, `reviewedAt`, and empty `rejectionReason`

- [ ] **Step 2: Add focused tests**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
```

Expected: tests prove accepted sheets promote to `ACCEPTED`, rejected sheets fail, and stale raw hashes fail.

- [ ] **Step 3: Apply acceptance to the candidate map**

Run:

```bash
python3 scripts/apply_dark_sheet_art_acceptance.py \
  --input-report build/reports/verification/dark-uiux/pr06-art-candidate-map.jsonl \
  --acceptance UI/manual-records/dark-uiux-pr06-sheet-art-acceptance.json \
  --out assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl
```

Expected: accepted JSONL report contains no `DRY_RUN` records for sliced player-visible outputs.

## Task 6: Final Runtime Replacement And Manifest Sync

**Files:**
- Replace: `client/src/main/resources/dark-v1/**`
- Modify if needed: `assets-src/image/manifests/phase2-visual-manifest.json`
- Sync: `client/src/main/resources/manifests/visual-manifest.json`
- Replace: `UI/sprite-sheets/dark-v1-final-full-inventory.json`

- [ ] **Step 1: Slice accepted sheets into the real runtime resource root**

Run:

```bash
python3 scripts/slice_spritesheet.py \
  --plan UI/sprite-sheets/sheet-plan.yaml \
  --runtime-root client/src/main/resources \
  --overwrite
```

Expected: runtime PNGs under `client/src/main/resources/dark-v1/**` match accepted raw sheet cells.

- [ ] **Step 2: Validate accepted runtime slices with reviewed QA requirement**

Run:

```bash
python3 scripts/verify_sprite_sheet_map.py \
  --check map \
  --plan UI/sprite-sheets/sheet-plan.yaml \
  --runtime-root client/src/main/resources \
  --report assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl \
  --require-reviewed-qa \
  --report-sheet-ids r08-skills-vanguard-berserker,r08-skills-templar-rogue,r08-skills-arcanist-spellblade,r09-status-damage,r09-quest-zone-profession,r09-fallback-debug,r09-rejected-polish
```

Expected: command passes; no `DRY_RUN`, no duplicate runtime hash, no missing contact sheet, no manifest rawOutputPath mismatch.

- [ ] **Step 3: Sync runtime manifest from canonical**

Run:

```bash
python3 scripts/sync_phase2_manifests.py
```

Expected: `client/src/main/resources/manifests/visual-manifest.json` equals canonical visual manifest for visual resources.

- [ ] **Step 4: Regenerate final-full inventory**

Run:

```bash
python3 scripts/generate_dark_final_full_inventory.py \
  --out UI/sprite-sheets/dark-v1-final-full-inventory.json
```

Expected: final-full inventory references current sheet ids, consumers, tests, and accepted source paths.

## Task 7: Automated Gate Closure

**Files:**
- Build reports only, except any generated final inventory/report already listed above.

- [ ] **Step 1: Run resource static gates**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkArtRandomQa resourcePipelineLint
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run final-full manifest coverage**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=final-full \
  -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json \
  -Pktome.darkUiux.artRandomQaRecord=UI/manual-records/dark-uiux-pr06-art-random-qa.json \
  -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md
```

Expected: `BUILD SUCCESSFUL`; report has zero player-visible pending/rejected art and zero `MANUAL_REVIEW_REQUIRED` blocking samples.

- [ ] **Step 3: Run focused client/resource tests**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew \
  :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest \
  :client:test --tests com.ktome.client.assets.ManifestResolveTest \
  :client:test --tests com.ktome.client.ui.status.StatusIconResolverTest \
  :client:test --tests com.ktome.client.render.TileRenderModelTest \
  :client:test --tests com.ktome.client.render.TileRendererCanvasTest
```

Expected: focused tests pass.

- [ ] **Step 4: Run PR06 owner-level visual regressions**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:clientSmoke :client:goldenScreenshot
```

Expected: golden artifacts show accepted PR06 skill/status/quest/profession icons, not old stopgap assets.

## Task 8: In-App White-Box Visual Verification

**Files:**
- Modify: `UI/manual-records/dark-uiux-pr06-status-quest-skill-overview.md`
- Modify: `UI/manual-records/dark-uiux-pr06-overview-screenshot.md`
- Modify: `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md`

- [ ] **Step 1: Restart the client**

Close any already-running client window before launching the new build. The running libGDX process may keep old textures in memory.

- [ ] **Step 2: Follow `docs/computer-use-whitebox-flow.md`**

Inspect:

- hotbar skills at 32/48px
- inscription slots
- status HUD row
- quest summary row
- profession/tree overview
- fallback/missing/debug surfaces that are player-visible

Expected: no visible skill/inscription/status/quest/profession icon still uses rejected rune-token or stopgap art.

- [ ] **Step 3: Capture evidence**

Manual records must contain:

- app build/run command
- screenshot path
- exact surfaces inspected
- pass/fail per family
- any remaining visual risk
- statement that the client was restarted after runtime PNG replacement

## Task 9: Final Verification And Handoff

**Files:**
- Modify: `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md`
- Modify: PR/manual records listed in Task 8

- [ ] **Step 1: Run final changed verification**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew verifyChanged
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run whitespace check**

Run:

```bash
git diff --check
```

Expected: no whitespace errors.

- [ ] **Step 3: Write final handoff summary**

The handoff must include:

- prompt paths
- raw sheet paths and `git hash-object` hashes
- contact sheet paths
- QA record path and result counts
- accepted sprite map report path
- runtime replacement summary
- manifest sync result
- final-full coverage result
- white-box screenshot/manual record paths
- commands run and failures, if any

## Completion Definition

The PR06 art work is complete only when all are true:

1. Every R08/R09 player-visible sheet has `decision: PASS`.
2. `UI/manual-records/dark-uiux-pr06-art-random-qa.json` has no blocking `MANUAL_REVIEW_REQUIRED` or reject decision.
3. `assets-src/image/manifests/dark-v1-pr06-sprite-map-report.jsonl` has reviewed/accepted QA for all sliced PR06 outputs.
4. `client/src/main/resources/dark-v1/**` is generated from accepted sheets, not stopgap hand edits.
5. Canonical and runtime visual manifests are synced.
6. `spriteSheetMapLint` passes with reviewed QA.
7. `darkManifestCoverageLint final-full` passes.
8. Same-screen golden or white-box evidence shows skill, inscription, status, quest, and profession/tree icons using accepted resources.
9. Manual records do not claim any command or visual QA that was not actually executed.
