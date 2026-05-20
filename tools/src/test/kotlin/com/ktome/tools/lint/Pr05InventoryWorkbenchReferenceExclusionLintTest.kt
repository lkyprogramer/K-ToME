package com.ktome.tools.lint

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("contractLint")
class Pr05InventoryWorkbenchReferenceExclusionLintTest {
    @Test
    fun `pr05 inventory workbench reference-only systems stay out of production surfaces`() {
        val findings = scanProductionSurfaces(repoRoot())

        assertTrue(
            findings.isEmpty(),
            "PR05-1 reference-only inventory terms must stay out of production surfaces: $findings",
        )
    }

    @Test
    fun `reference exclusion denylist reports every forbidden token`() {
        val findings =
            findForbiddenTokens(
                relativePath = "client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt",
                text = FORBIDDEN_TOKENS.joinToString(separator = "\n"),
            )

        assertEquals(
            FORBIDDEN_TOKENS.map { token ->
                ReferenceExclusionFinding(
                    "client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt",
                    token,
                )
            },
            findings,
        )
        assertEquals(FORBIDDEN_TOKENS.size, findings.size)
    }

    @Test
    fun `production surface fixture scan covers kotlin and locale roots`() {
        val findings =
            listOf(
                scanProductionSurfaceText(
                    relativePath = "client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt",
                    text = "ui.inventory.filter",
                ),
                scanProductionSurfaceText(
                    relativePath = "game/src/main/resources/i18n/en-US.json",
                    text = "carryWeight",
                ),
            ).flatten()

        assertEquals(
            listOf(
                ReferenceExclusionFinding(
                    "client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt",
                    "ui.inventory.filter",
                ),
                ReferenceExclusionFinding("game/src/main/resources/i18n/en-US.json", "carryWeight"),
            ),
            findings,
        )
    }

    @Test
    fun `production surface matching normalizes platform path separators`() {
        val findings =
            scanProductionSurfaceText(
                relativePath = """client\src\main\kotlin\com\ktome\client\render\InventoryWorkbenchPresentation.kt""",
                text = "ui.inventory.category_tab",
            )

        assertEquals(
            listOf(
                ReferenceExclusionFinding(
                    "client/src/main/kotlin/com/ktome/client/render/InventoryWorkbenchPresentation.kt",
                    "ui.inventory.category_tab",
                ),
            ),
            findings,
        )
    }

    @Test
    fun `docs reviews and prompts are not scanned production surfaces`() {
        val forbiddenText = FORBIDDEN_TOKENS.joinToString(separator = "\n")
        val findings =
            listOf(
                scanProductionSurfaceText(
                    relativePath = "UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md",
                    text = forbiddenText,
                ),
                scanProductionSurfaceText(
                    relativePath = "UI/review/2026-05-20-dark-uiux-pr05-1-inventory-page-workbench-rereview-round4.md",
                    text = forbiddenText,
                ),
                scanProductionSurfaceText(
                    relativePath = "UI/dark-uiux-pr03-inventory-page-reference.prompt.txt",
                    text = forbiddenText,
                ),
            ).flatten()

        assertTrue(findings.isEmpty(), "Docs, review reports and prompts must not be scanned: $findings")
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()

    private fun scanProductionSurfaces(root: Path): List<ReferenceExclusionFinding> =
        PRODUCTION_SURFACE_ROOTS.flatMap { relativeRoot ->
            val base = root.resolve(relativeRoot)
            if (!Files.exists(base)) {
                emptyList()
            } else {
                Files.walk(base).use { paths ->
                    paths
                        .filter(Files::isRegularFile)
                        .filter(::isScannedTextFile)
                        .flatMap { path ->
                            scanProductionSurfaceText(
                                relativePath = root.relativize(path).toString(),
                                text = Files.readString(path),
                            ).stream()
                        }
                        .toList()
                }
            }
        }

    private fun scanProductionSurfaceText(
        relativePath: String,
        text: String,
    ): List<ReferenceExclusionFinding> {
        val normalizedPath = normalizePath(relativePath)
        if (!isProductionSurfacePath(normalizedPath) || !isScannedTextFile(normalizedPath)) {
            return emptyList()
        }

        return findForbiddenTokens(relativePath = normalizedPath, text = text)
    }

    private fun normalizePath(path: String): String =
        path.replace('\\', '/')

    private fun isProductionSurfacePath(relativePath: String): Boolean =
        PRODUCTION_SURFACE_ROOTS.any { root ->
            relativePath == root || relativePath.startsWith("$root/")
        }

    private fun isScannedTextFile(path: Path): Boolean =
        isScannedTextFile(path.fileName.toString())

    private fun isScannedTextFile(path: String): Boolean =
        path.substringAfterLast('/').let { name ->
            name.endsWith(".kt") || name.endsWith(".json") || name.endsWith(".jsonl")
        }

    private fun findForbiddenTokens(
        relativePath: String,
        text: String,
    ): List<ReferenceExclusionFinding> =
        FORBIDDEN_TOKENS
            .filter(text::contains)
            .map { token -> ReferenceExclusionFinding(relativePath, token) }

    private data class ReferenceExclusionFinding(
        val relativePath: String,
        val token: String,
    )

    private companion object {
        private val PRODUCTION_SURFACE_ROOTS =
            listOf(
                "client/src/main/kotlin",
                "client/src/main/resources/manifests",
                "assets-src/image/manifests",
                "game/src/main/resources/i18n",
            )

        private val FORBIDDEN_TOKENS =
            listOf(
                "ui.inventory.filter",
                "ui.inventory.category_tab",
                "ui.inventory.weight",
                "burden",
                "encumbrance",
                "carryWeight",
                "weightCapacity",
                "负重",
                "承重",
            )
    }
}
