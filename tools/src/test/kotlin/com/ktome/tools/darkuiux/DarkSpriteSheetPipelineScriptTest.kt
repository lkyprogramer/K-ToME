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
    fun `key registry accepts sheet aliases and registry-only aliases but rejects cycles`() {
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
              - { targetKey: ui.test.d, category: icon, ownerPr: PR-00, sheetId: r98-base, fallbackKey: missing_visual, consumer: test, consumerTest: test, aliasOf: ui.test.a }
            """.trimIndent(),
        )
        writeText(manifest, manifest("ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))

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
    fun `sheet plan lint allows formal owner sheets without alias cells`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(
            plan,
            """
            schemaVersion: dark-sprite-sheet-plan-v1
            styleTag: ktome-dark-fantasy-sprite-ui-v1
            sheets:
              - sheetId: r97-no-alias-owner
                round: 1
                type: large-sheet
                styleTag: ktome-dark-fantasy-sprite-ui-v1
                rawSheetPath: assets-src/image/raw/sheets/dark-v1/r97-no-alias-owner.png
                outputRoot: client/src/main/resources
                promptBase: no alias formal owner sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.no_alias.a, category: icon, outputName: dark-v1/test/a.png, subject: direct icon a }
                  - { row: 0, col: 1, targetKey: ui.no_alias.b, category: icon, outputName: dark-v1/test/b.png, subject: direct icon b }
                  - { row: 0, col: 2, targetKey: ui.no_alias.c, category: icon, outputName: dark-v1/test/c.png, subject: direct icon c }
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
        val plan = tempDir.resolve("sheet-plan.yaml")
        val runtimeRoot = tempDir.resolve("runtime")
        val fallback = runtimeRoot.resolve("debug/missing_visual.png")
        Files.createDirectories(fallback.parent)
        val original = byteArrayOf(1, 2, 3, 4)
        Files.write(fallback, original)
        writeText(plan, largeSheetPlan("r96-pending-fallback", defaultCells()))

        val result =
            runScript(
                "scripts/slice_spritesheet.py",
                "--plan",
                plan.toString(),
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
                runScriptWithFakePillow(
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
                runScriptWithFakePillow(
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

            Files.writeString(
                report,
                reportText
                    .replace("\"qaStatus\": \"DRY_RUN\"", "\"qaStatus\": \"CODEX_VISUAL_CHECKED\"")
                    .replace("\"reviewer\": null", "\"reviewer\": \"Codex\"")
                    .replace("\"reviewedAt\": null", "\"reviewedAt\": \"2026-05-10T12:00:00+08:00\""),
            )
            val preserved =
                runScriptWithFakePillow(
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
            val preservedText = Files.readString(report)
            assertEquals(0, preserved.exitCode, preserved.output)
            assertTrue(preservedText.contains("\"qaStatus\": \"CODEX_VISUAL_CHECKED\""), preservedText)
            assertTrue(preservedText.contains("\"reviewer\": \"Codex\""), preservedText)
        } finally {
            Files.deleteIfExists(rawSheet)
            Files.deleteIfExists(contactSheet)
        }
    }

    @Test
    fun `sprite map lint rejects duplicate output hash for non alias outputs`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val rawRoot = tempDir.resolve("raw")
        val contactRoot = tempDir.resolve("contact")
        val runtimeRoot = tempDir.resolve("runtime")
        val sheetId = "tmp-duplicate-hash-${System.nanoTime()}"
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
                promptBase: duplicate hash sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.hash.a, category: icon, outputName: dark-v1/test/hash_a.png, subject: hash icon a }
                  - { row: 0, col: 1, targetKey: ui.hash.b, category: icon, outputName: dark-v1/test/hash_b.png, subject: hash icon b }
            """.trimIndent(),
        )
        writeText(
            manifest,
            manifestWithRawPaths(
                "ui.hash.a" to "dark-v1/test/hash_a.png",
                "ui.hash.b" to "dark-v1/test/hash_b.png",
            ),
        )
        writeImage(rawRoot.resolve("$sheetId.png"))
        writeImage(contactRoot.resolve("$sheetId-contact.png"))
        writeImage(runtimeRoot.resolve("dark-v1/test/hash_a.png"))
        writeImage(runtimeRoot.resolve("dark-v1/test/hash_b.png"))

        val result =
            runScriptWithFakePillow(
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
                tempDir.resolve("duplicate-hash.jsonl").toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("duplicate outputHash"), result.output)
        assertTrue(result.output.contains("ui.hash.a"), result.output)
        assertTrue(result.output.contains("ui.hash.b"), result.output)
    }

    @Test
    fun `sprite map lint close mode rejects dry run qa status`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val rawRoot = tempDir.resolve("raw")
        val contactRoot = tempDir.resolve("contact")
        val runtimeRoot = tempDir.resolve("runtime")
        val sheetId = "tmp-dry-run-qa-${System.nanoTime()}"
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
                promptBase: dry run close sheet
                grid: { columns: 4, rows: 4, cellWidth: 256, cellHeight: 256 }
                cells:
                  - { row: 0, col: 0, targetKey: ui.close.dry_run, category: icon, outputName: dark-v1/test/close_dry_run.png, subject: close dry run icon }
            """.trimIndent(),
        )
        writeText(manifest, manifestWithRawPaths("ui.close.dry_run" to "dark-v1/test/close_dry_run.png"))
        writeImage(rawRoot.resolve("$sheetId.png"))
        writeImage(contactRoot.resolve("$sheetId-contact.png"))
        writeImage(runtimeRoot.resolve("dark-v1/test/close_dry_run.png"))

        val result =
            runScriptWithFakePillow(
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
                tempDir.resolve("dry-run.jsonl").toString(),
                "--require-reviewed-qa",
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("qaStatus is DRY_RUN"), result.output)
    }

    @Test
    fun `sprite map lint can close a filtered owner report without historical dry run rows`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val rawRoot = tempDir.resolve("raw")
        val contactRoot = tempDir.resolve("contact")
        val runtimeRoot = tempDir.resolve("runtime")
        val report = tempDir.resolve("filtered-owner.jsonl")
        val selectedSheetId = "tmp-owner-selected-${System.nanoTime()}"
        val historicalSheetId = "tmp-owner-historical-${System.nanoTime()}"
        writeText(
            plan,
            largeSheetPlan(
                listOf(
                    selectedSheetId to
                        "- { row: 0, col: 0, targetKey: ui.report.selected, category: icon, outputName: dark-v1/test/report_selected.png, subject: selected owner icon }",
                    historicalSheetId to
                        "- { row: 0, col: 0, targetKey: ui.report.historical, category: icon, outputName: debug/missing_visual.png, subject: historical pending icon }",
                ),
            ),
        )
        writeText(
            manifest,
            manifestWithRawPaths(
                "ui.report.selected" to "dark-v1/test/report_selected.png",
                "ui.report.historical" to "debug/missing_visual.png",
            ),
        )
        writeImage(rawRoot.resolve("$selectedSheetId.png"))
        writeImage(rawRoot.resolve("$historicalSheetId.png"))
        writeImage(contactRoot.resolve("$selectedSheetId-contact.png"))
        writeImage(contactRoot.resolve("$historicalSheetId-contact.png"))
        writeImage(runtimeRoot.resolve("dark-v1/test/report_selected.png"))

        val initial =
            runScriptWithFakePillow(
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
        assertEquals(0, initial.exitCode, initial.output)

        val reviewedLines =
            Files.readAllLines(report).map { line ->
                if (line.contains("\"targetKey\": \"ui.report.selected\"")) {
                    line
                        .replace("\"qaStatus\": \"DRY_RUN\"", "\"qaStatus\": \"CODEX_VISUAL_CHECKED\"")
                        .replace("\"reviewer\": null", "\"reviewer\": \"Codex\"")
                        .replace("\"reviewedAt\": null", "\"reviewedAt\": \"2026-05-15T12:00:00+08:00\"")
                } else {
                    line
                }
            }
        Files.writeString(report, reviewedLines.joinToString("\n", postfix = "\n"))
        Files.delete(rawRoot.resolve("$historicalSheetId.png"))
        Files.delete(contactRoot.resolve("$historicalSheetId-contact.png"))

        val filtered =
            runScriptWithFakePillow(
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
                "--report-sheet-ids",
                selectedSheetId,
                "--require-reviewed-qa",
            )
        val filteredText = Files.readString(report)

        assertEquals(0, filtered.exitCode, filtered.output)
        assertTrue(filteredText.contains("\"targetKey\": \"ui.report.selected\""), filteredText)
        assertFalse(filteredText.contains("\"targetKey\": \"ui.report.historical\""), filteredText)
        assertEquals(1, filteredText.lineSequence().filter(String::isNotBlank).count())
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
    fun `coverage lint fails owner scope when owner expected keys are empty`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("empty-owner.json")
        writeText(plan, largeSheetPlan("r95-empty-owner", defaultCells()))
        writeText(registry, registry("r95-empty-owner", "ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))
        writeText(manifest, manifest("ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))
        writeText(runtimeManifest, manifest("ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--owner-pr",
                "PR-02",
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
        assertTrue(result.output.contains("owner-scope coverage found no expected keys for PR-02"), result.output)
        assertTrue(reportText.contains("\"ownerExpectedKeys\": []"), reportText)
        assertTrue(reportText.contains("\"status\": \"FAIL\""), reportText)
    }

    @Test
    fun `coverage lint fails owner scope when required owner sheet ids are missing`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("missing-required-sheets.json")
        writeText(plan, largeSheetPlan("r01-ui-chrome", defaultCells()))
        writeText(
            registry,
            registry("r01-ui-chrome", "ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d")
                .replace("ownerPr: PR-00", "ownerPr: PR-02"),
        )
        writeText(
            manifest,
            manifestWithRawPaths(
                "ui.test.a" to "dark-v1/test/a.png",
                "ui.test.b" to "dark-v1/test/b.png",
                "ui.test.c" to "dark-v1/test/c.png",
                "ui.test.d" to "dark-v1/test/d.png",
            ),
        )
        writeText(runtimeManifest, Files.readString(manifest))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--owner-pr",
                "PR-02",
                "--required-owner-sheet-ids",
                "r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons",
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
        assertTrue(result.output.contains("owner-scope missing required sheet ids for PR-02"), result.output)
        assertTrue(result.output.contains("missing=r01-ui-controls, r01-ui-hud-icons"), result.output)
        assertTrue(reportText.contains("\"requiredOwnerSheetIds\": ["), reportText)
        assertTrue(reportText.contains("\"r01-ui-controls\""), reportText)
        assertTrue(reportText.contains("\"r01-ui-hud-icons\""), reportText)
        assertTrue(reportText.contains("\"ownerSheetIds\": ["), reportText)
        assertTrue(reportText.contains("\"r01-ui-chrome\""), reportText)
        assertTrue(reportText.contains("\"status\": \"FAIL\""), reportText)
    }

    @Test
    fun `coverage lint fails owner scope when required owner key is missing`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val ownerContract = tempDir.resolve("owner-contract.yaml")
        val report = tempDir.resolve("missing-required-key.json")
        val keys = listOf("ui.contract.a", "ui.contract.b", "ui.contract.c", "ui.contract.d")
        writeText(plan, largeSheetPlan("r95-contract-owner", cellsForKeys(keys)))
        writeText(
            registry,
            registry("r95-contract-owner", *keys.toTypedArray())
                .replace("ownerPr: PR-00", "ownerPr: PR-02"),
        )
        writeText(
            ownerContract,
            ownerContract(
                ownerPr = "PR-02",
                sheetId = "r95-contract-owner",
                requiredKeys = keys + "ui.contract.missing",
                direct = 5,
                alias = 0,
                reserved = 11,
                total = 16,
            ),
        )
        writeText(
            manifest,
            manifestWithRawPaths(*keys.map { key -> key to "dark-v1/ui/${key.replace('.', '_')}.png" }.toTypedArray()),
        )
        writeText(runtimeManifest, Files.readString(manifest))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--owner-pr",
                "PR-02",
                "--owner-contract",
                ownerContract.toString(),
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
        assertTrue(result.output.contains("owner-scope missing required owner keys for PR-02"), result.output)
        assertTrue(result.output.contains("ui.contract.missing"), result.output)
        assertTrue(reportText.contains("\"ownerMissingRequiredKeys\": ["), reportText)
        assertTrue(reportText.contains("\"requiredOwnerKeys\": ["), reportText)
    }

    @Test
    fun `resource pipeline lint rejects typed inferred and delegated production inventory mirrors`() {
        val productionRoot = tempDir.resolve("production-kotlin")
        val sourceFile = productionRoot.resolve("com/ktome/client/assets/MirroredKeys.kt")
        writeText(
            sourceFile,
            """
            package com.ktome.client.assets

            internal object MirroredKeys {
                val typedOwnerKeys: List<String> = listOf("ui.test.a")
                val inferredOwnerKeys = listOf("ui.test.b")
                val itemIconKeys = setOf("item.test.icon")
                val requiredInventoryKeys by lazy { listOf("ui.test.c") }
            }
            """,
        )

        val result =
            runScript(
                "scripts/resource_pipeline_authority_lint.py",
                "--production-kotlin-root",
                productionRoot.toString(),
                "--report",
                tempDir.resolve("resource-pipeline-authority.json").toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("typedOwnerKeys"), result.output)
        assertTrue(result.output.contains("inferredOwnerKeys"), result.output)
        assertTrue(result.output.contains("itemIconKeys"), result.output)
        assertTrue(result.output.contains("requiredInventoryKeys"), result.output)
    }

    @Test
    fun `coverage lint fails owner scope when owner keys are pending`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("pending-owner.json")
        writeText(plan, largeSheetPlan("r95-pending-owner", defaultCells()))
        writeText(
            registry,
            registry("r95-pending-owner", "ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d")
                .replace("ownerPr: PR-00", "ownerPr: PR-02"),
        )
        writeText(manifest, manifest("ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))
        writeText(runtimeManifest, Files.readString(manifest))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--owner-pr",
                "PR-02",
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
        assertTrue(result.output.contains("owner-scope pending keys for PR-02"), result.output)
        assertTrue(reportText.contains("\"ownerPendingKeys\": ["), reportText)
        assertTrue(reportText.contains("\"ui.test.a\""), reportText)
        assertTrue(reportText.contains("\"ownerCoveredKeys\": []"), reportText)
    }

    @Test
    fun `coverage lint fails owner scope when owner keys use old style output`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("old-style-owner.json")
        writeText(plan, largeSheetPlan("r95-old-style-owner", defaultCells()))
        writeText(
            registry,
            registry("r95-old-style-owner", "ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d")
                .replace("ownerPr: PR-00", "ownerPr: PR-02"),
        )
        writeText(
            manifest,
            manifestWithRawPaths(
                "ui.test.a" to "phase4/legacy_a.png",
                "ui.test.b" to "dark-v1/ui/test_b.png",
                "ui.test.c" to "dark-v1/ui/test_c.png",
                "ui.test.d" to "dark-v1/ui/test_d.png",
            ),
        )
        writeText(runtimeManifest, Files.readString(manifest))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--owner-pr",
                "PR-02",
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
        assertTrue(result.output.contains("owner-scope old-style keys for PR-02"), result.output)
        assertTrue(reportText.contains("\"ownerOldStyleKeys\": ["), reportText)
        assertTrue(reportText.contains("\"ui.test.a\""), reportText)
    }

    @Test
    fun `coverage lint passes owner scope when required sheets have only dark outputs`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("covered-owner.json")
        val sheetKeys =
            listOf(
                "r01-ui-chrome" to keysFor("chrome"),
                "r01-ui-controls" to keysFor("controls"),
                "r01-ui-hud-icons" to keysFor("hud"),
            )
        writeText(plan, largeSheetPlan(sheetKeys.map { (sheetId, keys) -> sheetId to cellsForKeys(keys) }))
        writeText(registry, registryForSheets(ownerPr = "PR-02", sheetKeys))
        writeText(
            tempDir.resolve("owner-contract.yaml"),
            ownerContractForSheets(ownerPr = "PR-02", sheetKeys = sheetKeys, direct = 4, alias = 0, reserved = 12, total = 16),
        )
        val manifestEntries =
            sheetKeys
                .flatMap { (_, keys) -> keys }
                .map { key -> key to "dark-v1/ui/${key.replace('.', '_')}.png" }
                .toTypedArray()
        writeText(manifest, manifestWithRawPaths(*manifestEntries))
        writeText(runtimeManifest, Files.readString(manifest))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--owner-pr",
                "PR-02",
                "--required-owner-sheet-ids",
                "r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons",
                "--owner-contract",
                tempDir.resolve("owner-contract.yaml").toString(),
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
        assertTrue(reportText.contains("\"status\": \"PASS\""), reportText)
        assertTrue(reportText.contains("\"ownerPendingKeys\": []"), reportText)
        assertTrue(reportText.contains("\"ownerOldStyleKeys\": []"), reportText)
        assertTrue(reportText.contains("\"requiredOwnerSheetIds\": ["), reportText)
        assertTrue(reportText.contains("\"requiredOwnerKeyCountBySheet\": {"), reportText)
        assertTrue(reportText.contains("\"ownerExpectedKeyCountBySheet\": {"), reportText)
    }

    @Test
    fun `coverage lint reports registry only aliases without treating them as unexpected owner keys`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val ownerContract = tempDir.resolve("owner-contract.yaml")
        val report = tempDir.resolve("registry-only-alias-owner.json")
        val sheetId = "r01-ui-chrome"
        val keys = keysFor("chrome")
        val aliasOnlyKey = "ui.chrome.registry_only_alias"
        writeText(plan, largeSheetPlan(sheetId, cellsForKeys(keys)))
        writeText(
            registry,
            buildString {
                append(registryForSheets(ownerPr = "PR-02", listOf(sheetId to keys)))
                appendLine("  - targetKey: $aliasOnlyKey")
                appendLine("    category: icon")
                appendLine("    ownerPr: PR-02")
                appendLine("    sheetId: $sheetId")
                appendLine("    fallbackKey: missing_visual")
                appendLine("    consumer: test")
                appendLine("    consumerTest: test")
                appendLine("    aliasOf: ${keys[0]}")
            },
        )
        writeText(
            ownerContract,
            ownerContract(
                ownerPr = "PR-02",
                sheetId = sheetId,
                requiredKeys = keys,
                direct = 4,
                alias = 0,
                reserved = 12,
                total = 16,
            ),
        )
        val manifestEntries =
            (keys + aliasOnlyKey)
                .map { key -> key to "dark-v1/ui/${key.replace('.', '_')}.png" }
                .toTypedArray()
        writeText(manifest, manifestWithRawPaths(*manifestEntries))
        writeText(runtimeManifest, Files.readString(manifest))

        val result =
            runScript(
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                "owner-scope",
                "--owner-pr",
                "PR-02",
                "--owner-contract",
                ownerContract.toString(),
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
        assertTrue(reportText.contains("\"ownerAliasOnlyKeys\": ["), reportText)
        assertTrue(reportText.contains("\"$aliasOnlyKey\""), reportText)
        assertTrue(reportText.contains("\"ownerUnexpectedKeys\": []"), reportText)
        assertTrue(reportText.contains("\"ownerSheetIds\": ["), reportText)
        assertTrue(reportText.contains("\"$sheetId\""), reportText)
    }

    @Test
    fun `sheet plan lint can require every grid slot to be listed`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        writeText(plan, largeSheetPlan("r95-full-grid", defaultCells()))

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
                "--require-full-grid",
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("r95-full-grid must list every grid slot"), result.output)
        assertTrue(result.output.contains("missing slots="), result.output)
    }

    @Test
    fun `sheet plan lint can enforce owner contract cell counts`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val ownerContract = tempDir.resolve("owner-contract.yaml")
        val keys = listOf("ui.grid.a", "ui.grid.b", "ui.grid.c", "ui.grid.d")
        writeText(plan, largeSheetPlan("r95-count-contract", fullGridCellsForKeys(keys)))
        writeText(
            ownerContract,
            ownerContract(
                ownerPr = "PR-02",
                sheetId = "r95-count-contract",
                requiredKeys = keys,
                direct = 4,
                alias = 1,
                reserved = 11,
                total = 16,
            ),
        )

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
                "--require-full-grid",
                "--owner-contract",
                ownerContract.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(result.output.contains("r95-count-contract cell counts mismatch for PR-02"), result.output)
        assertTrue(result.output.contains("'alias': 1"), result.output)
    }

    @Test
    fun `owner contract requires cell counts for every required sheet`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val ownerContract = tempDir.resolve("owner-contract.yaml")
        val sheetKeys =
            listOf(
                "r95-count-a" to keysFor("count-a"),
                "r95-count-b" to keysFor("count-b"),
            )
        writeText(plan, largeSheetPlan(sheetKeys.map { (sheetId, keys) -> sheetId to fullGridCellsForKeys(keys) }))
        val contractText =
            ownerContractForSheets(
                ownerPr = "PR-02",
                sheetKeys = sheetKeys,
                direct = 4,
                alias = 0,
                reserved = 12,
                total = 16,
            )
                .lineSequence()
                .filterNot { line -> line.trim().startsWith("r95-count-b:") }
                .joinToString("\n")
        writeText(ownerContract, "$contractText\n")

        val result =
            runScript(
                "scripts/verify_sprite_sheet_map.py",
                "--check",
                "sheet-plan",
                "--plan",
                plan.toString(),
                "--require-full-grid",
                "--owner-contract",
                ownerContract.toString(),
            )

        assertEquals(1, result.exitCode, result.output)
        assertTrue(
            result.output.contains("owner contract requiredCellCountsBySheet must include every requiredSheetIds entry"),
            result.output,
        )
        assertTrue(result.output.contains("missing=r95-count-b"), result.output)
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
    fun `coverage lint rejects stale runtime fallback when canonical manifest is dark`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        val runtimeManifest = tempDir.resolve("runtime-manifest.json")
        val report = tempDir.resolve("runtime-coverage.json")
        writeText(plan, largeSheetPlan("r95-runtime-coverage", defaultCells()))
        writeText(registry, registry("r95-runtime-coverage", "ui.test.a", "ui.test.b", "ui.test.c", "ui.test.d"))
        writeText(
            manifest,
            manifestWithRawPaths(
                "ui.test.a" to "dark-v1/test/a.png",
                "ui.test.b" to "dark-v1/test/b.png",
                "ui.test.c" to "dark-v1/test/c.png",
                "ui.test.d" to "dark-v1/test/d.png",
            ),
        )
        writeText(
            runtimeManifest,
            manifestWithRawPaths(
                "ui.test.a" to "debug/missing_visual.png",
                "ui.test.b" to "dark-v1/test/b.png",
                "ui.test.c" to "dark-v1/test/c.png",
                "ui.test.d" to "dark-v1/test/d.png",
            ),
        )

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
        assertTrue(reportText.contains("\"pendingOrRejectedPlayerVisibleCells\": ["), reportText)
        assertTrue(reportText.contains("\"ui.test.a\""), reportText)
        assertFalse(reportText.contains("\"coveredKeySet\": [\"ui.test.a\""), reportText)
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
        assertTrue(result.output.contains("^PR-\\d{2}(?:-\\d+)?$"), result.output)
    }

    @Test
    fun `key registry lint accepts pr sub owners and rejects extra or lowercase owner segments`() {
        val plan = tempDir.resolve("sheet-plan.yaml")
        val registry = tempDir.resolve("key-registry.yaml")
        val manifest = tempDir.resolve("manifest.json")
        writeText(plan, largeSheetPlan("r92-owner-sub", cellsFor("owner-sub")))
        writeText(manifest, manifest("ui.owner-sub.a", "ui.owner-sub.b", "ui.owner-sub.c", "ui.owner-sub.d"))

        fun runFor(ownerPr: String): ScriptResult {
            writeText(
                registry,
                registry("r92-owner-sub", "ui.owner-sub.a", "ui.owner-sub.b", "ui.owner-sub.c", "ui.owner-sub.d")
                    .replace("ownerPr: PR-00", "ownerPr: $ownerPr"),
            )
            return runScript(
                "scripts/verify_dark_key_registry.py",
                "--plan",
                plan.toString(),
                "--registry",
                registry.toString(),
                "--manifest",
                manifest.toString(),
            )
        }

        assertEquals(0, runFor("PR-02-1").exitCode)

        val extraSegment = runFor("PR-02-1-1")
        assertEquals(1, extraSegment.exitCode, extraSegment.output)
        assertTrue(extraSegment.output.contains("^PR-\\d{2}(?:-\\d+)?$"), extraSegment.output)

        val lowerCase = runFor("pr-02-1")
        assertEquals(1, lowerCase.exitCode, lowerCase.output)
        assertTrue(lowerCase.output.contains("^PR-\\d{2}(?:-\\d+)?$"), lowerCase.output)
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
    fun `manifest lint lets dark owner sheet plan override legacy generated plan path`() {
        val legacyPlan = tempDir.resolve("legacy-generated-plan.yaml")
        writeText(
            legacyPlan,
            """
            styleTag: ktome-middle-fantasy-painterly-tile-v1
            phase2AssetGates:
              LEGACY-UI:
                description: stale legacy path for a key now owned by dark sheet-plan.
                assets:
                  - id: legacy_ui_combat_action_icon
                    category: icon
                    visualKey: ui.combat.action.icon
                    outputName: phase4/legacy/ui_combat_action_icon.png
            """.trimIndent(),
        )

        val result =
            runScript(
                "scripts/manifest-lint.py",
                "--extra-plan",
                legacyPlan.toString(),
                "--dark-key-registry",
                "UI/sprite-sheets/key-registry.yaml",
                "--dark-sheet-plan",
                "UI/sprite-sheets/sheet-plan.yaml",
            )

        assertFalse(
            result.output.contains("rawOutputPath mismatch for 'ui.combat.action.icon'"),
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
            runScriptWithFakePillowAndEnv(
                mapOf(
                    "PATH" to "${fakeBin}${File.pathSeparator}${System.getenv("PATH")}",
                    "FAKE_CODEX_GENERATED_DIR" to generatedRoot.toString(),
                    "FAKE_CODEX_EXPECT_TTY" to "1",
                    "FAKE_CODEX_EXPECT_IMAGE_ONLY_PROMPT" to "1",
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

    @Test
    fun `codex image script normalizes declared canvas size`() {
        val fakeBin = tempDir.resolve("fake-bin")
        val fakeCodex = fakeBin.resolve("codex")
        val generatedRoot = tempDir.resolve("generated-images")
        val output = tempDir.resolve("out/normalized.png")
        writeFakeCodex(fakeCodex)

        val result =
            runScriptWithFakePillowAndEnv(
                mapOf(
                    "PATH" to "${fakeBin}${File.pathSeparator}${System.getenv("PATH")}",
                    "FAKE_CODEX_GENERATED_DIR" to generatedRoot.toString(),
                ),
                "scripts/codex-generate-image.py",
                "Canvas: 2x2\nfake dark icon prompt",
                "--out",
                output.toString(),
                "--generated-dir",
                generatedRoot.toString(),
                "--timeout-seconds",
                "5",
                "--overwrite",
            )

        assertEquals(0, result.exitCode, result.output)
        val image = ImageIO.read(output.toFile())
        assertEquals(2, image.width, result.output)
        assertEquals(2, image.height, result.output)
    }

    @Test
    fun `codex image script preserves aspect ratio when normalizing wide generated image`() {
        val fakeBin = tempDir.resolve("fake-bin")
        val fakeCodex = fakeBin.resolve("codex")
        val generatedRoot = tempDir.resolve("generated-images")
        val fakeSource = tempDir.resolve("fake-wide.png")
        val output = tempDir.resolve("out/normalized-wide.png")
        writeFakeCodex(fakeCodex)
        writeWideImage(fakeSource)

        val result =
            runScriptWithFakePillowAndEnv(
                mapOf(
                    "PATH" to "${fakeBin}${File.pathSeparator}${System.getenv("PATH")}",
                    "FAKE_CODEX_GENERATED_DIR" to generatedRoot.toString(),
                    "FAKE_CODEX_IMAGE_PATH" to fakeSource.toString(),
                ),
                "scripts/codex-generate-image.py",
                "Canvas: 4x4\nfake wide dark icon prompt",
                "--out",
                output.toString(),
                "--generated-dir",
                generatedRoot.toString(),
                "--timeout-seconds",
                "5",
                "--overwrite",
            )

        assertEquals(0, result.exitCode, result.output)
        val image = ImageIO.read(output.toFile())
        assertEquals(4, image.width, result.output)
        assertEquals(4, image.height, result.output)
        assertEquals(0, image.getRGB(0, 0).ushr(24), result.output)
        assertEquals(255, image.getRGB(0, 1).ushr(24), result.output)
        assertEquals(255, image.getRGB(3, 2).ushr(24), result.output)
        assertEquals(0, image.getRGB(0, 3).ushr(24), result.output)
    }

    private fun runScript(vararg args: String): ScriptResult =
        runScriptWithEnv(emptyMap(), *args)

    private fun runScriptWithFakePillow(vararg args: String): ScriptResult =
        runScriptWithEnv(mapOf("PYTHONPATH" to fakePillowPythonPath().toString()), *args)

    private fun runScriptWithFakePillowAndEnv(extraEnv: Map<String, String>, vararg args: String): ScriptResult =
        runScriptWithEnv(mapOf("PYTHONPATH" to fakePillowPythonPath().toString()) + extraEnv, *args)

    private fun runScriptWithEnv(extraEnv: Map<String, String>, vararg args: String): ScriptResult {
        val processBuilder =
            ProcessBuilder(listOf("python3") + args.toList())
                .directory(repoRoot().toFile())
                .redirectErrorStream(true)
        val environment = processBuilder.environment()
        val requestedPythonPath = extraEnv["PYTHONPATH"]
        if (!requestedPythonPath.isNullOrBlank()) {
            environment["PYTHONPATH"] =
                listOfNotNull(requestedPythonPath, environment["PYTHONPATH"]?.takeIf(String::isNotBlank))
                    .joinToString(File.pathSeparator)
        }
        extraEnv
            .filterKeys { key -> key != "PYTHONPATH" }
            .forEach { (key, value) -> environment[key] = value }
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

    private fun fakePillowPythonPath(): Path {
        val moduleRoot = tempDir.resolve("python-modules")
        val pilDir = moduleRoot.resolve("PIL")
        val initFile = pilDir.resolve("__init__.py")
        val imageFile = pilDir.resolve("Image.py")
        if (!Files.exists(imageFile)) {
            writeText(initFile, "from . import Image")
            writeText(
                imageFile,
                """
                import binascii
                import pathlib
                import struct
                import zlib

                PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"

                class Resampling:
                    LANCZOS = 1

                class PixelAccess:
                    def __init__(self, image):
                        self.image = image

                    def __getitem__(self, key):
                        x, y = key
                        return self.image.pixels[y][x]

                    def __setitem__(self, key, value):
                        x, y = key
                        self.image.pixels[y][x] = tuple(value)

                class FakeImage:
                    def __init__(self, path=None, size=None, rect=None, color=(255, 51, 102, 255), pixels=None):
                        self.path = str(path or "")
                        source_size = size or read_png_size(path) or (1024, 1024)
                        self.rect = tuple(rect or (0, 0, source_size[0], source_size[1]))
                        self.width = max(1, self.rect[2] - self.rect[0])
                        self.height = max(1, self.rect[3] - self.rect[1])
                        self.size = (self.width, self.height)
                        if pixels is None:
                            self.pixels = [
                                [tuple(color) for _ in range(self.width)]
                                for _ in range(self.height)
                            ]
                        else:
                            self.pixels = pixels

                    def __enter__(self):
                        return self

                    def __exit__(self, exc_type, exc_value, traceback):
                        return False

                    def convert(self, mode):
                        return self

                    def crop(self, rect):
                        left, top, right, bottom = rect
                        width = max(1, right - left)
                        height = max(1, bottom - top)
                        return FakeImage(self.path, size=(width, height), rect=(0, 0, width, height))

                    def getbbox(self):
                        return (0, 0, self.size[0], self.size[1])

                    def tobytes(self):
                        return b"".join(
                            bytes(channel for pixel in row for channel in pixel)
                            for row in self.pixels
                        )

                    def load(self):
                        return PixelAccess(self)

                    def resize(self, size, resample=None):
                        return FakeImage(self.path, size=size)

                    def alpha_composite(self, image, dest):
                        dest_x, dest_y = dest
                        for y in range(image.height):
                            target_y = dest_y + y
                            if target_y < 0 or target_y >= self.height:
                                continue
                            for x in range(image.width):
                                target_x = dest_x + x
                                if target_x < 0 or target_x >= self.width:
                                    continue
                                if image.pixels[y][x][3] > 0:
                                    self.pixels[target_y][target_x] = image.pixels[y][x]

                    def save(self, path):
                        write_png(pathlib.Path(path), self.width, self.height, self.pixels)

                def open(path):
                    return FakeImage(path)

                def new(mode, size, color):
                    return FakeImage(size=size, color=color)

                def read_png_size(path):
                    if path is None:
                        return None
                    data = pathlib.Path(path).read_bytes()
                    if data[:8] != PNG_SIGNATURE or data[12:16] != b"IHDR":
                        return None
                    width, height = struct.unpack(">II", data[16:24])
                    return width, height

                def png_chunk(kind, payload):
                    return (
                        struct.pack(">I", len(payload))
                        + kind
                        + payload
                        + struct.pack(">I", binascii.crc32(kind + payload) & 0xFFFFFFFF)
                    )

                def write_png(path, width, height, pixels):
                    path.parent.mkdir(parents=True, exist_ok=True)
                    raw_rows = []
                    for row in pixels:
                        raw_rows.append(b"\x00" + b"".join(bytes(pixel) for pixel in row))
                    payload = zlib.compress(b"".join(raw_rows))
                    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
                    path.write_bytes(
                        PNG_SIGNATURE
                        + png_chunk(b"IHDR", header)
                        + png_chunk(b"IDAT", payload)
                        + png_chunk(b"IEND", b"")
                    )
                """.trimIndent(),
            )
        }
        return moduleRoot
    }

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

    private fun writeWideImage(path: Path) {
        Files.createDirectories(path.parent)
        val image = BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                image.setRGB(x, y, 0xFFFF3366.toInt())
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
            import sys
            import time

            if os.environ.get("FAKE_CODEX_EXPECT_TTY") == "1":
                if not sys.stdin.isatty() or not sys.stdout.isatty():
                    raise SystemExit("fake codex expected tty-backed stdio")

            if os.environ.get("FAKE_CODEX_EXPECT_IMAGE_ONLY_PROMPT") == "1":
                args = sys.argv[1:]
                if "--cd" not in args or "--sandbox" not in args or "read-only" not in args:
                    raise SystemExit(f"fake codex expected isolated read-only exec args: {args}")
                prompt = args[-1]
                if "Generate exactly one image from the prompt below using image generation." not in prompt:
                    raise SystemExit("fake codex expected image-only wrapper")
                if "<image_prompt>\nfake dark icon prompt\n</image_prompt>" not in prompt:
                    raise SystemExit("fake codex expected original prompt in wrapper")
                if "--skip-git-repo-check" not in args:
                    raise SystemExit("fake codex expected --skip-git-repo-check")

            root = pathlib.Path(os.environ["FAKE_CODEX_GENERATED_DIR"])
            folder = root / f"session-{time.time_ns()}"
            folder.mkdir(parents=True, exist_ok=True)
            source_image = os.environ.get("FAKE_CODEX_IMAGE_PATH")
            if source_image:
                png = pathlib.Path(source_image).read_bytes()
            else:
                png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=")
            (folder / "image.png").write_bytes(png)
            """.trimIndent() + "\n",
        )
        assertTrue(path.toFile().setExecutable(true))
    }

    private fun defaultCells(): String =
        cellsFor("test")

    private fun keysFor(prefix: String): List<String> =
        listOf("ui.$prefix.a", "ui.$prefix.b", "ui.$prefix.c", "ui.$prefix.d")

    private fun cellsFor(prefix: String): String =
        cellsForKeys(keysFor(prefix))

    private fun cellsForKeys(keys: List<String>): String =
        """
        - { row: 0, col: 0, targetKey: ${keys[0]}, category: icon, outputName: debug/missing_visual.png, subject: action icon }
        - { row: 0, col: 1, targetKey: ${keys[1]}, category: icon, outputName: debug/missing_visual.png, subject: method icon, aliasOf: ${keys[0]} }
        - { row: 0, col: 2, targetKey: ${keys[2]}, category: icon, outputName: debug/missing_visual.png, subject: target icon }
        - { row: 0, col: 3, targetKey: ${keys[3]}, category: icon, outputName: debug/missing_visual.png, subject: lock icon }
        - { row: 3, col: 3, reserved: true, note: reserved }
        """.trimIndent()

    private fun fullGridCellsForKeys(keys: List<String>): String =
        buildString {
            keys.forEachIndexed { index, key ->
                appendLine(
                    "- { row: ${index / 4}, col: ${index % 4}, targetKey: $key, category: icon, outputName: debug/missing_visual.png, subject: icon $index }",
                )
            }
            for (index in keys.size until 16) {
                appendLine("- { row: ${index / 4}, col: ${index % 4}, reserved: true, note: reserved }")
            }
        }.trim()

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
        registryForSheets(ownerPr = "PR-00", listOf(sheetId to keys.toList()))

    private fun registryForSheets(
        ownerPr: String,
        sheetKeys: List<Pair<String, List<String>>>,
    ): String =
        buildString {
            appendLine("schemaVersion: dark-key-registry-v1")
            appendLine("styleTag: ktome-dark-fantasy-sprite-ui-v1")
            appendLine("entries:")
            sheetKeys.forEach { (sheetId, keys) ->
                keys.forEachIndexed { index, key ->
                    appendLine("  - targetKey: $key")
                    appendLine("    category: icon")
                    appendLine("    ownerPr: $ownerPr")
                    appendLine("    sheetId: $sheetId")
                    appendLine("    fallbackKey: missing_visual")
                    appendLine("    consumer: test")
                    appendLine("    consumerTest: test")
                    if (index == 1) {
                        appendLine("    aliasOf: ${keys[0]}")
                    }
                }
            }
        }

    private fun ownerContract(
        ownerPr: String,
        sheetId: String,
        requiredKeys: List<String>,
        direct: Int,
        alias: Int,
        reserved: Int,
        total: Int,
    ): String =
        ownerContractForSheets(
            ownerPr = ownerPr,
            sheetKeys = listOf(sheetId to requiredKeys),
            direct = direct,
            alias = alias,
            reserved = reserved,
            total = total,
        )

    private fun ownerContractForSheets(
        ownerPr: String,
        sheetKeys: List<Pair<String, List<String>>>,
        direct: Int,
        alias: Int,
        reserved: Int,
        total: Int,
    ): String =
        buildString {
            appendLine("schemaVersion: dark-owner-contract-v1")
            appendLine("ownerPr: $ownerPr")
            appendLine("requiredSheetIds:")
            sheetKeys.forEach { (sheetId, _) -> appendLine("  - $sheetId") }
            appendLine("requiredCellCountsBySheet:")
            sheetKeys.forEach { (sheetId, _) ->
                appendLine("  $sheetId: { direct: $direct, alias: $alias, reserved: $reserved, total: $total }")
            }
            appendLine("requiredCells:")
            sheetKeys.forEach { (sheetId, keys) ->
                keys.forEach { key ->
                    appendLine("  - { targetKey: $key, sheetId: $sheetId, category: icon }")
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

    private fun manifestWithRawPaths(vararg entries: Pair<String, String>): String =
        buildString {
            appendLine("{")
            appendLine("""  "manifestVersion": 1,""")
            appendLine("""  "styleTag": "ktome-middle-fantasy-painterly-tile-v1",""")
            appendLine("""  "fallbackKey": "missing_visual",""")
            appendLine("""  "entries": [""")
            val allEntries = listOf("missing_visual" to "debug/missing_visual.png") + entries.toList()
            allEntries.forEachIndexed { index, (key, rawPath) ->
                val comma = if (index == allEntries.lastIndex) "" else ","
                appendLine(
                    """    {"key":"$key","category":"${if (key == "missing_visual") "debug" else "icon"}","rawOutputPath":"$rawPath","footprint":"ui","pivotX":0.5,"pivotY":0.5,"tags":["test"]}$comma""",
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
