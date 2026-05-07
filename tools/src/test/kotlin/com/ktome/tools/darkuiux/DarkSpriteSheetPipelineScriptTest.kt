package com.ktome.tools.darkuiux

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DarkSpriteSheetPipelineScriptTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `verify sprite sheet map reports missing raw sheet as explicit failure`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val manifest = tempDir.resolve("manifest.json")
        writeText(plan, largeSheetPlan("r99-missing-raw", defaultCells()))
        writeText(manifest, manifest("ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "map",
                "--plan",
                plan.toString(),
                "--manifest",
                manifest.toString(),
                "--report",
                tempDir.resolve("map.jsonl").toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("missingRawSheet"), result.output)
        assertTrue(result.output.contains("assets-src/image/raw/sheets/dark-v1/r99-missing-raw.png"), result.output)
    }

    @Test
    fun `key registry accepts same sheet and cross sheet aliases but rejects cycles`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: r98-base
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r98-base.png
                outputRoot: client/src/main/resources
                promptBase: base sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.test.a, category: icon, outputName: debug/missing_visual.png, subject: base icon }
                  - { row: 0, col: 1, targetKey: ui.test.b, category: icon, outputName: debug/missing_visual.png, subject: same sheet alias, aliasOf: ui.test.a }
              - sheetId: r98-cross
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r98-cross.png
                outputRoot: client/src/main/resources
                promptBase: cross sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.test.c, category: icon, outputName: debug/missing_visual.png, subject: cross sheet alias, aliasOf: ui.test.a }
            """.trimIndent(),
        )
        writeText(
            registry,
            """
            schemaVersion: dark-key-registry-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            entries:
              - { targetKey: ui.test.a, category: icon, ownerPr: PR-00, sheetId: r98-base, fallbackKey: missing_visual, consumer: test, consumerTest: test }
              - { targetKey: ui.test.b, category: icon, ownerPr: PR-00, sheetId: r98-base, fallbackKey: missing_visual, consumer: test, consumerTest: test, aliasOf: ui.test.a }
              - { targetKey: ui.test.c, category: icon, ownerPr: PR-00, sheetId: r98-cross, fallbackKey: missing_visual, consumer: test, consumerTest: test, aliasOf: ui.test.a }
            """.trimIndent(),
        )
        writeText(manifest, manifest("ui.test.a", "ui.test.b", "ui.test.c"))

        val pass =
            runScript(
                "scripts/verify_dark_key_registry.py",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
            )

        assertEquals(0, pass.exitCode, pass.output)

        writeText(
            registry,
            Files.readString(registry)
                .replace("aliasOf: ui.test.a }", "aliasOf: ui.test.c }", ignoreCase = false),
        )
        val fail =
            runScript(
                "scripts/verify_dark_key_registry.py",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
            )

        assertEquals(1, fail.exitCode, fail.output)
        assertTrue(fail.output.contains("alias cycle"), fail.output)
    }

    @Test
    fun `sheet plan lint rejects capacity overflow`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val cells =
            (0..64).joinToString("\n") { index ->
                val row = index / 8
                val col = index % 8
                "- { row: $row, col: $col, targetKey: ui.test.$index, category: icon, outputName: debug/missing_visual.png, subject: icon $index }"
            }
        val indentedCells =
            cells
                .lineSequence()
                .joinToString("\n") { line -> "      $line" }
        writeText(
            plan,
            buildString {
                appendLine("schemaVersion: dark-sprite-sheet-plan-v1")
                appendLine("styleTag: ktome-dark-fantasy-sprite-ui-v1")
                appendLine("sheets:")
                appendLine("  - sheetId: r97-overflow")
                appendLine("    round: 1")
                appendLine("    type: icon-sheet")
                appendLine("    styleTag: ktome-dark-fantasy-sprite-ui-v1")
                appendLine("    rawSheetPath: assets-src/image/raw/sheets/dark-v1/r97-overflow.png")
                appendLine("    outputRoot: client/src/main/resources")
                appendLine("    promptBase: overflow sheet")
                appendLine("    grid: { columns: 8, rows: 8, cellWidth: 128, cellHeight: 128 }")
                appendLine("    cells:")
                appendLine(indentedCells)
            },
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("capacity is 64"), result.output)
    }

    @Test
    fun `sheet plan lint rejects wrong schema`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(
            plan,
            largeSheetPlan("r97-schema", defaultCells())
                .replace("dark-sprite-sheet-plan-v1", "dark-sprite-sheet-plan-v0"),
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("sheet-plan schemaVersion"), result.output)
    }

    @Test
    fun `sheet plan lint reports invalid numeric fields without crashing`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: r97-invalid-number
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r97-invalid-number.png
                outputRoot: client/src/main/resources
                promptBase: invalid numeric sheet
                grid: { columns: four, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: first, col: 0, targetKey: ui.invalid.number, category: icon, outputName: debug/missing_visual.png, subject: invalid number icon }
            """.trimIndent(),
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("grid must match large-sheet policy"), result.output)
        assertTrue(result.output.contains("row/col must be inside grid"), result.output)
    }

    @Test
    fun `sheet plan lint rejects duplicate sliced dark outputs`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: r97-duplicate-output
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r97-duplicate-output.png
                outputRoot: client/src/main/resources
                promptBase: duplicate output sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.duplicate.a, category: icon, outputName: dark-v1/test/shared.png, subject: duplicate icon a }
                  - { row: 0, col: 1, targetKey: ui.duplicate.b, category: icon, outputName: dark-v1/test/shared.png, subject: duplicate icon b }
            """.trimIndent(),
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("duplicates sliced dark-v1 output"), result.output)
    }

    @Test
    fun `sheet plan lint allows alias cells to share sliced output`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: r97-shared-alias-output
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r97-shared-alias-output.png
                outputRoot: client/src/main/resources
                promptBase: shared alias output sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.alias.base, category: icon, outputName: dark-v1/test/shared_alias.png, subject: base icon }
                  - { row: 0, col: 1, targetKey: ui.alias.same, category: icon, outputName: dark-v1/test/shared_alias.png, subject: alias icon, aliasOf: ui.alias.base }
                  - { row: 0, col: 2, targetKey: ui.alias.pending, category: icon, outputName: debug/missing_visual.png, subject: pending icon }
            """.trimIndent(),
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
            )

        assertEquals(0, result.exitCode, result.output)
    }

    @Test
    fun `sheet plan lint requires alias cells to share target output`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: r97-alias-output-mismatch
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r97-alias-output-mismatch.png
                outputRoot: client/src/main/resources
                promptBase: alias output mismatch sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.alias.base, category: icon, outputName: dark-v1/test/base.png, subject: base icon }
                  - { row: 0, col: 1, targetKey: ui.alias.mismatch, category: icon, outputName: dark-v1/test/mismatch.png, subject: alias icon, aliasOf: ui.alias.base }
                  - { row: 0, col: 2, targetKey: ui.alias.pending, category: icon, outputName: debug/missing_visual.png, subject: pending icon }
            """.trimIndent(),
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("alias outputName must match"), result.output)
    }

    @Test
    fun `sheet plan lint rejects unsupported cell fields`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(
            plan,
            largeSheetPlan("r97-unsupported-cell-field", cellsFor("unsupported"))
                .replace("subject: action icon }", "subject: action icon, anchor: center, safeMarginPx: 8 }"),
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("unsupported fields: anchor, safeMarginPx"), result.output)
    }

    @Test
    fun `prompt generation keeps stable numbering and prompt hash for unchanged sheets`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val outputDir = tempDir.resolve("prompts")
        writeText(plan, largeSheetPlan("r96-prompt-stable", cellsFor("stable")))

        val first =
            runScript(
                "scripts/generate_sheet_prompt.py",
                "--plan",
                plan.toString(),
                "--output-dir",
                outputDir.toString(),
            )
        val firstIndex = Files.readString(outputDir.resolve("prompt-index.json"))
        val second =
            runScript(
                "scripts/generate_sheet_prompt.py",
                "--plan",
                plan.toString(),
                "--output-dir",
                outputDir.toString(),
            )
        val secondIndex = Files.readString(outputDir.resolve("prompt-index.json"))

        assertEquals(0, first.exitCode, first.output)
        assertEquals(0, second.exitCode, second.output)
        assertTrue(firstIndex.contains("001-r96-prompt-stable"), firstIndex)
        assertEquals(firstIndex, secondIndex)
    }

    @Test
    fun `prompt generation appends new sheet ids without renumbering existing prompts`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val outputDir = tempDir.resolve("prompts")
        writeText(plan, largeSheetPlan("r96-prompt-stable", cellsFor("stable")))

        val first =
            runScript(
                "scripts/generate_sheet_prompt.py",
                "--plan",
                plan.toString(),
                "--output-dir",
                outputDir.toString(),
            )
        assertEquals(0, first.exitCode, first.output)

        writeText(
            plan,
            largeSheetPlan(
                listOf(
                    "r00-lexically-earlier" to cellsFor("earlier"),
                    "r96-prompt-stable" to cellsFor("stable"),
                ),
            ),
        )
        val second =
            runScript(
                "scripts/generate_sheet_prompt.py",
                "--plan",
                plan.toString(),
                "--output-dir",
                outputDir.toString(),
            )
        val secondIndex = Files.readString(outputDir.resolve("prompt-index.json"))

        assertEquals(0, second.exitCode, second.output)
        assertTrue(secondIndex.contains("001-r96-prompt-stable"), secondIndex)
        assertTrue(secondIndex.contains("002-r00-lexically-earlier"), secondIndex)
    }

    @Test
    fun `prompt generation fails when existing prompt index references a removed sheet`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val outputDir = tempDir.resolve("prompts")
        writeText(plan, largeSheetPlan("r96-prompt-stable", cellsFor("stable")))
        val first =
            runScript(
                "scripts/generate_sheet_prompt.py",
                "--plan",
                plan.toString(),
                "--output-dir",
                outputDir.toString(),
            )
        assertEquals(0, first.exitCode, first.output)

        writeText(plan, largeSheetPlan("r97-replacement", cellsFor("replacement")))
        val second =
            runScript(
                "scripts/generate_sheet_prompt.py",
                "--plan",
                plan.toString(),
                "--output-dir",
                outputDir.toString(),
            )

        assertEquals(1, second.exitCode, second.output)
        assertTrue(second.output.contains("absent from sheet-plan.yaml"), second.output)
        assertTrue(second.output.contains("r96-prompt-stable"), second.output)
    }

    @Test
    fun `slice script skips pending non dark outputs without overwriting fallback resources`() {
        val runtimeRoot = tempDir.resolve("runtime")
        val fallback = runtimeRoot.resolve("debug/missing_visual.png")
        Files.createDirectories(fallback.parent)
        val original = byteArrayOf(1, 2, 3, 4)
        Files.write(fallback, original)

        val result =
            runScript(
                "scripts/slice_spritesheet.py",
                "--runtime-root",
                runtimeRoot.toString(),
                "--overwrite",
            )

        assertEquals(0, result.exitCode, result.output)
        assertTrue(result.output.contains("written=0"), result.output)
        assertTrue(result.output.contains("skippedPending=4"), result.output)
        assertArrayEquals(original, Files.readAllBytes(fallback))
    }

    @Test
    fun `slice script writes dark output and map lint records output hash`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val rawRoot = tempDir.resolve("raw")
        val contactRoot = tempDir.resolve("contact")
        val runtimeRoot = tempDir.resolve("runtime")
        val report = tempDir.resolve("map.jsonl")
        val sheetId = "tmp-slice-output-${System.nanoTime()}"
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: $sheetId
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/$sheetId.png
                outputRoot: client/src/main/resources
                promptBase: slice output sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.slice.output, category: icon, outputName: dark-v1/test/ui_slice_output.png, subject: slice output icon }
            """.trimIndent(),
        )
        writeText(
            manifest,
            """
            {
              "manifestVersion": 1,
              "styleTag": "ktome-middle-fantasy-painterly-tile-v1",
              "fallbackKey": "missing_visual",
              "entries": [
                {"key":"missing_visual","category":"debug","rawOutputPath":"debug/missing_visual.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]},
                {"key":"ui.slice.output","category":"icon","rawOutputPath":"dark-v1/test/ui_slice_output.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]}
              ],
              "prefixRules": []
            }
            """.trimIndent(),
        )
        val rawSheet = rawRoot.resolve("$sheetId.png")
        val contactSheet = contactRoot.resolve("$sheetId-contact.png")
        try {
            writeImage(rawSheet)
            writeImage(contactSheet)

            val slice =
                runScript(
                    "scripts/slice_spritesheet.py",
                    "--plan",
                    plan.toString(),
                    "--raw-root",
                    rawRoot.toString(),
                    "--runtime-root",
                    runtimeRoot.toString(),
                    "--overwrite",
                )
            val map =
                runScript(
                    "scripts/verify_sprite_sheet_map.py",
                    "--check",
                    "map",
                    "--plan",
                    plan.toString(),
                    "--manifest",
                    manifest.toString(),
                    "--raw-root",
                    rawRoot.toString(),
                    "--contact-root",
                    contactRoot.toString(),
                    "--runtime-root",
                    runtimeRoot.toString(),
                    "--report",
                    report.toString(),
                )

            assertEquals(0, slice.exitCode, slice.output)
            assertTrue(slice.output.contains("written=1"), slice.output)
            assertEquals(0, map.exitCode, map.output)
            val reportText = Files.readString(report)
            assertTrue(reportText.contains("\"targetKey\": \"ui.slice.output\""), reportText)
            assertTrue(reportText.contains("\"outputHash\": \""), reportText)
        } finally {
            Files.deleteIfExists(rawSheet)
            Files.deleteIfExists(contactSheet)
        }
    }

    @Test
    fun `coverage lint requires owner for owner scope and rejects pending fallback in final full`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        writeText(plan, largeSheetPlan("r95-coverage", defaultCells()))
        writeText(registry, registry("r95-coverage", "ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))
        writeText(manifest, manifest("ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))
        writeText(runtimeManifest, manifest("ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))

        val ownerMissing =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
                "--runtime-manifest",
                runtimeManifest.toString(),
                "--report",
                tempDir.resolve("owner.json").toString(),
            )
        val finalFull =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "final-full",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
                "--runtime-manifest",
                runtimeManifest.toString(),
                "--report",
                tempDir.resolve("final.json").toString(),
            )

        assertEquals(1, ownerMissing.exitCode, ownerMissing.output)
        assertTrue(ownerMissing.output.contains("owner-scope coverage requires --owner-pr"), ownerMissing.output)
        assertEquals(1, finalFull.exitCode, finalFull.output)
        assertTrue(finalFull.output.contains("pendingOrRejectedPlayerVisibleCells"), finalFull.output)
    }

    @Test
    fun `coverage lint separates pending fallback from old style residue`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("coverage-split.json")
        writeText(plan, largeSheetPlan("r95-coverage-split", defaultCells()))
        writeText(registry, registry("r95-coverage-split", "ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))
        val manifestText =
            """
            {
              "manifestVersion": 1,
              "styleTag": "ktome-middle-fantasy-painterly-tile-v1",
              "fallbackKey": "missing_visual",
              "entries": [
                {"key":"missing_visual","category":"debug","rawOutputPath":"debug/missing_visual.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]},
                {"key":"ui.test.a","category":"icon","rawOutputPath":"debug/missing_visual.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]},
                {"key":"ui.test.b","category":"icon","rawOutputPath":"phase4/legacy_icon.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]},
                {"key":"ui.test.c","category":"icon","rawOutputPath":"dark-v1/test/c.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]},
                {"key":"ui.test.d","category":"icon","rawOutputPath":"dark-v1/test/d.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]}
              ],
              "prefixRules": []
            }
            """.trimIndent()
        writeText(manifest, manifestText)
        writeText(runtimeManifest, manifestText)

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "final-full",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
                "--runtime-manifest",
                runtimeManifest.toString(),
                "--report",
                report.toString(),
            )
        val reportText = Files.readString(report)

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("pendingOrRejectedPlayerVisibleCells"), result.output)
        assertTrue(result.output.contains("oldStylePlayerVisibleKeys"), result.output)
        assertTrue(reportText.contains("\"oldStylePlayerVisibleKeys\": ["), reportText)
        assertTrue(reportText.contains("\"ui.test.b\""), reportText)
        assertTrue(reportText.contains("\"pendingOrRejectedPlayerVisibleCells\": ["), reportText)
        assertTrue(reportText.contains("\"ui.test.a\""), reportText)
    }

    @Test
    fun `coverage lint writes pr00 pending artifact schema`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("coverage.json")
        writeText(plan, largeSheetPlan("r94-coverage-snapshot", cellsFor("coverage")))
        writeText(registry, registry("r94-coverage-snapshot", "ui.coverage.a", "ui.coverage.b", "ui.coverage.c", "ui.coverage.d"))
        writeText(manifest, manifest("ui.coverage.a", "ui.coverage.b", "ui.coverage.c", "ui.coverage.d"))
        writeText(runtimeManifest, manifest("ui.coverage.a", "ui.coverage.b", "ui.coverage.c", "ui.coverage.d"))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "pr00-dry-run",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
                "--runtime-manifest",
                runtimeManifest.toString(),
                "--report",
                report.toString(),
            )
        val reportText = Files.readString(report)

        assertEquals(0, result.exitCode, result.output)
        assertTrue(reportText.contains("\"schemaVersion\": \"dark-v1-manifest-coverage-v1\""), reportText)
        assertTrue(reportText.contains("\"scopeMode\": \"pr00-dry-run\""), reportText)
        assertTrue(reportText.contains("\"strictOldStyleResidue\": false"), reportText)
        assertTrue(reportText.contains("\"pendingOrRejectedPlayerVisibleCells\""), reportText)
        assertTrue(reportText.contains("\"fallbackKeyUsage\""), reportText)
        assertTrue(reportText.contains("\"allowedFallbackKeys\""), reportText)
    }

    @Test
    fun `key registry lint rejects wrong schema and malformed owner pr`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        writeText(plan, largeSheetPlan("r92-owner-schema", cellsFor("owner")))
        writeText(
            registry,
            registry("r92-owner-schema", "ui.owner.a", "ui.owner.b", "ui.owner.c", "ui.owner.d")
                .replace("dark-key-registry-v1", "dark-key-registry-v0")
                .replace("ownerPr: PR-00", "ownerPr: PR00"),
        )
        writeText(manifest, manifest("ui.owner.a", "ui.owner.b", "ui.owner.c", "ui.owner.d"))

        val result =
            runScript(
                "scripts/verify_dark_key_registry.py",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("schemaVersion"), result.output)
        assertTrue(result.output.contains("PR-00 format"), result.output)
    }

    @Test
    fun `manifest lint skips dark bridge comparisons when dark inputs are invalid`() {
        val plan = tempDir.resolve("invalid-dark-plan.yaml")
        val registry = tempDir.resolve("dark-registry.yaml")
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v0
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: r93-invalid-bridge
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r93-invalid-bridge.png
                outputRoot: client/src/main/resources
                promptBase: invalid bridge sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.invalid.bridge, category: icon, outputName: debug/missing_visual.png, subject: invalid bridge icon }
                  - { row: 0, col: 1, targetKey: ui.invalid.bridge.b, category: icon, outputName: debug/missing_visual.png, subject: invalid bridge icon b }
                  - { row: 0, col: 2, targetKey: ui.invalid.bridge.c, category: icon, outputName: debug/missing_visual.png, subject: invalid bridge icon c }
            """.trimIndent(),
        )
        writeText(registry, registry("r93-invalid-bridge", "ui.invalid.bridge", "ui.invalid.bridge.b", "ui.invalid.bridge.c"))

        val result =
            runScript(
                "scripts/manifest-lint.py",
                "--dark-key-registry",
                registry.toString(),
                "--dark-sheet-plan",
                plan.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("dark manifest bridge skipped"), result.output)
        assertFalse(
            result.output.contains("canonical visual manifest is missing dark sheet-plan targetKey 'ui.invalid.bridge'"),
            result.output,
        )
    }

    @Test
    fun `codex image script writes smoke report from generated image`() {
        val fakeBin = tempDir.resolve("fake-bin")
        val fakeCodex = fakeBin.resolve("codex")
        val generatedRoot = tempDir.resolve("generated-images")
        val output = tempDir.resolve("out/generated.png")
        val smokeReport = tempDir.resolve("reports/codex-image-smoke.json")
        writeFakeCodex(fakeCodex)

        val result =
            runScriptWithEnv(
                mapOf(
                    "PATH" to "${fakeBin}${File.pathSeparator}${System.getenv("PATH")}",
                    "FAKE_CODEX_GENERATED_DIR" to generatedRoot.toString(),
                ),
                "scripts/codex-generate-image.py",
                "fake dark icon prompt",
                "--out",
                output.toString(),
                "--generated-dir",
                generatedRoot.toString(),
                "--smoke-report",
                smokeReport.toString(),
                "--timeout-seconds",
                "5",
                "--overwrite",
            )
        val smokeText = Files.readString(smokeReport)

        assertEquals(0, result.exitCode, result.output)
        assertTrue(Files.exists(output), result.output)
        assertTrue(smokeText.contains("\"schemaVersion\": \"codex-image-smoke-v1\""), smokeText)
        assertTrue(smokeText.contains("\"sourceFolder\""), smokeText)
        assertTrue(smokeText.contains("\"sourceImage\""), smokeText)
        assertTrue(smokeText.contains("\"output\""), smokeText)
        assertTrue(smokeText.contains("\"sha256\""), smokeText)
    }

    private fun runScript(vararg args: String): ScriptResult =
        runScriptWithEnv(emptyMap(), *args)

    private fun runScriptWithEnv(extraEnv: Map<String, String>, vararg args: String): ScriptResult {
        val processBuilder =
            ProcessBuilder(listOf("python3") + args.toList())
                .directory(repoRoot().toFile())
                .redirectErrorStream(true)
        processBuilder.environment().putAll(extraEnv)
        val process =
            processBuilder.start()
        val completed = process.waitFor(30, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
            val output = process.inputStream.bufferedReader().readText()
            return ScriptResult(-1, "Timed out after 30s: python3 ${args.joinToString(" ")}\n$output")
        }
        val output = process.inputStream.bufferedReader().readText()
        return ScriptResult(process.exitValue(), output)
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()

    private fun writeText(path: Path, content: String) {
        Files.createDirectories(path.parent)
        Files.writeString(path, content.trimIndent() + "\n")
    }

    private fun writeImage(path: Path) {
        Files.createDirectories(path.parent)
        val image = BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB)
        for (x in 32 until 224) {
            for (y in 32 until 224) {
                image.setRGB(x, y, 0xFF1CB7C8.toInt())
            }
        }
        ImageIO.write(image, "png", path.toFile())
    }

    private fun writeFakeCodex(path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            """
            #!/usr/bin/env python3
            import base64
            import os
            import pathlib
            import time

            root = pathlib.Path(os.environ["FAKE_CODEX_GENERATED_DIR"])
            folder = root / f"session-{time.time_ns()}"
            folder.mkdir(parents=True, exist_ok=True)
            png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=")
            (folder / "image.png").write_bytes(png)
            """.trimIndent() + "\n",
        )
        assertTrue(path.toFile().setExecutable(true))
    }

    private fun defaultCells(): String =
        cellsFor("test")

    private fun cellsFor(prefix: String): String =
        """
        - { row: 0, col: 0, targetKey: ui.$prefix.a, category: icon, outputName: debug/missing_visual.png, subject: action icon }
        - { row: 0, col: 1, targetKey: ui.$prefix.b, category: icon, outputName: debug/missing_visual.png, subject: method icon, aliasOf: ui.$prefix.a }
        - { row: 0, col: 2, targetKey: ui.$prefix.c, category: icon, outputName: debug/missing_visual.png, subject: target icon }
        - { row: 0, col: 3, targetKey: ui.$prefix.d, category: icon, outputName: debug/missing_visual.png, subject: lock icon }
        - { row: 3, col: 3, reserved: true, note: reserved }
        """.trimIndent()

    private fun largeSheetPlan(
        sheetId: String,
        cells: String,
    ): String =
        largeSheetPlan(listOf(sheetId to cells))

    private fun largeSheetPlan(sheets: List<Pair<String, String>>): String {
        return buildString {
            appendLine("schemaVersion: dark-sprite-sheet-plan-v1")
            appendLine("styleTag: ktome-dark-fantasy-sprite-ui-v1")
            appendLine("sheets:")
            sheets.forEach { (sheetId, cells) ->
                val indentedCells =
                    cells
                        .lineSequence()
                        .filter(String::isNotBlank)
                        .joinToString("\n") { line -> "      $line" }
                appendLine("  - sheetId: $sheetId")
                appendLine("    round: 1")
                appendLine("    type: large-sheet")
                appendLine("    styleTag: ktome-dark-fantasy-sprite-ui-v1")
                appendLine("    rawSheetPath: assets-src/image/raw/sheets/dark-v1/$sheetId.png")
                appendLine("    outputRoot: client/src/main/resources")
                appendLine("    promptBase: Dark fantasy dry-run sheet")
                appendLine("    grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }")
                appendLine("    cells:")
                appendLine(indentedCells)
            }
        }
    }

    private fun registry(
        sheetId: String,
        vararg keys: String,
    ): String =
        buildString {
            appendLine("schemaVersion: dark-key-registry-v1")
            appendLine("styleTag: ktome-dark-fantasy-sprite-ui-v1")
            appendLine("entries:")
            keys.forEachIndexed { index, key ->
                appendLine("  - targetKey: $key")
                appendLine("    category: icon")
                appendLine("    ownerPr: PR-00")
                appendLine("    sheetId: $sheetId")
                appendLine("    fallbackKey: missing_visual")
                appendLine("    consumer: test")
                appendLine("    consumerTest: test")
                if (index == 1) {
                    appendLine("    aliasOf: ${keys[0]}")
                }
            }
        }

    private fun manifest(vararg keys: String): String =
        buildString {
            appendLine("{")
            appendLine("""  "manifestVersion": 1,""")
            appendLine("""  "styleTag": "ktome-middle-fantasy-painterly-tile-v1",""")
            appendLine("""  "fallbackKey": "missing_visual",""")
            appendLine("""  "entries": [""")
            val entries = listOf("missing_visual") + keys.toList()
            entries.forEachIndexed { index, key ->
                val comma = if (index == entries.lastIndex) "" else ","
                appendLine(
                    """    {"key":"$key","category":"${if (key == "missing_visual") "debug" else "icon"}","rawOutputPath":"debug/missing_visual.png","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]}$comma""",
                )
            }
            appendLine("  ],")
            appendLine("""  "prefixRules": []""")
            appendLine("}")
        }

    private data class ScriptResult(
        val exitCode: Int,
        val output: String,
    )
}
