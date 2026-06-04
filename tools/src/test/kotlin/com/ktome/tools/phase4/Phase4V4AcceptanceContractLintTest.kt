package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class Phase4V4AcceptanceContractLintTest {
    @Test
    @Tag("acceptanceContractLint")
    fun `phase4 v4 pr docs publish executable acceptance contracts`() {
        val root = repoRoot()
        val governance = read(root, "docs/review/phase4/v4-pr/development-governance.md")
        val readme = read(root, "docs/review/phase4/v4-pr/README.md")
        val uiGovernance = read(root, "UI/pr/development-governance.md")
        val uiReadme = read(root, "UI/pr/README.md")
        val pr08IterationLog = read(root, "UI/goal/dark-uiux-pr08-director-grade-iteration-log.md")
        val verificationReadme = read(root, "docs/verification/README.md")
        val aiGovernance = read(root, "docs/rule/ai-change-governance.md")

        assertContains(governance, "Acceptance Matrix", "development-governance.md")
        assertContains(governance, "Gate Budget", "development-governance.md")
        assertContains(governance, "Canonical Artifact", "development-governance.md")
        assertContains(governance, "PR-03 Canary", "development-governance.md")
        assertContains(readme, "development-governance.md", "v4-pr README")
        assertContains(readme, "acceptanceContractLint", "v4-pr README")
        assertContains(readme, "PR-03 是治理 canary", "v4-pr README")
        assertFalse(
            readme.contains(":tools:test --tests com.ktome.tools.loot.WhiteBoxLootRunnerTest"),
            "v4-pr README fast lane must not use :tools:test for tagged whiteBoxLoot tests.",
        )
        assertFalse(
            readme.contains("WhiteBoxLootRunnerTest"),
            "v4-pr README fast lane must not include the owner-level WhiteBoxLootRunnerTest.",
        )
        assertContains(verificationReadme, "Phase4 v4 开发验证阶梯", "verification README")
        assertContains(verificationReadme, "不得为 v4 PR 再造第二套 impact routing", "verification README")
        assertContains(aiGovernance, "Phase4 v4 PR 开发治理", "ai-change-governance")
        assertContains(aiGovernance, "不得引入第二套 verification authority", "ai-change-governance")
        assertContains(uiGovernance, "Dark UI/UX PR Development Governance", "UI/pr development-governance.md")
        assertContains(uiGovernance, "Acceptance Matrix", "UI/pr development-governance.md")
        assertContains(uiGovernance, "Gate Ladder", "UI/pr development-governance.md")
        assertUiGovernanceConvergenceGate(uiGovernance)
        assertPr08ConvergenceState(pr08IterationLog)
        assertContains(uiReadme, "development-governance.md", "UI/pr README")
        assertContains(uiReadme, "acceptanceContractLint", "UI/pr README")
        assertContains(uiReadme, "Gate ladder 固定", "UI/pr README")

        prDocs.forEach { prDoc ->
            assertPrDocContract(root, prDoc)
        }
        uiPrDocs.forEach { prDoc ->
            assertPrDocContract(root, prDoc)
        }
        assertUiD10RetainedUiContract(root)
        assertUiPr05InventoryReferenceArtifacts(root)

        val pr03 = read(root, prDocs.first { doc -> doc.requirementPrefix == "PR03" }.path)
        assertContains(pr03, "canary", "PR03 doc")
        assertContains(pr03, "whitebox=skipped", "PR03 doc")
        assertContains(pr03, "用户要求不进行人工白盒测试", "PR03 doc")

        val rootBuild = read(root, "build.gradle.kts")
        val toolsBuild = read(root, "tools/build.gradle.kts")
        assertContains(rootBuild, "tasks.register(\"acceptanceContractLint\")", "root build.gradle.kts")
        assertContains(toolsBuild, "tasks.register<Test>(\"acceptanceContractLint\")", "tools build.gradle.kts")
        assertContains(rootBuild, "\"acceptanceContractLint\"", "verificationOnlyTestTags")
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()

    private fun read(root: Path, relativePath: String): String {
        val path = root.resolve(relativePath)
        assertTrue(Files.exists(path), "$relativePath must exist.")
        return Files.readString(path)
    }

    private fun acceptanceRows(markdown: String, requirementPrefix: String): List<String> =
        markdown
            .lineSequence()
            .map(String::trim)
            .filter { line -> line.startsWith("| `$requirementPrefix-") }
            .toList()

    private fun assertContains(markdown: String, token: String, owner: String) {
        assertTrue(markdown.contains(token), "$owner must contain '$token'.")
    }

    private fun assertUiGovernanceConvergenceGate(markdown: String) {
        val owner = "UI/pr development-governance.md Visual Convergence Gate"
        listOf(
            "Visual Convergence Gate",
            "`blocker-id`",
            "`accepted-forward`",
            "technique family",
            "`technique-family`",
            "`source-art`",
            "`alpha-tuning`",
            "`compositor-rect`",
            "`wall-family`",
            "`presentation-structure`",
            "`layout-ab`",
            "failure-counter impact",
            "`failure-counter-impact`",
            "`counts-as-progress`",
            "`counts-as-failure`",
            "`freeze-no-more-local-polish`",
            "`not-visual-progress`",
            "`ab-decision-required`",
            "governance-only",
            "routine human wait",
        ).forEach { token ->
            assertContains(markdown, token, owner)
        }
    }

    private fun assertPr08ConvergenceState(markdown: String) {
        val owner = "PR-08 iteration log Current Convergence State"
        val section = sectionBetween(
            markdown = markdown,
            startHeading = "## Current Convergence State",
            endHeading = "## Entry Template",
            owner = owner,
        )
        listOf(
            "blocker-id",
            "technique-family",
            "occurrence-count",
            "failure-counter-impact",
            "required-action",
            "status",
        ).forEach { token ->
            assertContains(section, token, owner)
        }

        val rows = section
            .lineSequence()
            .map(String::trim)
            .filter { line -> line.startsWith("| `") }
            .toList()
        assertTrue(rows.isNotEmpty(), "$owner must declare at least one active blocker row.")

        val rowsByBlocker = mutableMapOf<String, String>()
        rows.forEach { row ->
            val cells = tableCells(row)
            assertTrue(
                cells.size >= PR08_CONVERGENCE_COLUMN_COUNT,
                "$owner row must include blocker, technique, count, impact, action and status: $row",
            )
            val blockerId = unquotedTableCell(cells[0])
            val techniqueFamily = unquotedTableCell(cells[1])
            val occurrenceCount = unquotedTableCell(cells[2])
            val impact = unquotedTableCell(cells[3])
            val requiredAction = unquotedTableCell(cells[4])
            val status = unquotedTableCell(cells[5])

            assertTrue(
                pr08TechniqueFamilies.contains(techniqueFamily),
                "$owner row uses unknown technique-family '$techniqueFamily': $row",
            )
            assertTrue(
                pr08FailureCounterImpacts.contains(impact),
                "$owner row uses unknown failure-counter-impact '$impact': $row",
            )
            if (requiresDirectionAction(occurrenceCount)) {
                assertTrue(
                    pr08RepeatBlockerActions.contains(requiredAction),
                    "$owner repeated blocker must require direction-change/freeze/deferral/A-B action: $row",
                )
                assertFalse(
                    status == "progress",
                    "$owner repeated blocker must not remain in progress status: $row",
                )
            }
            rowsByBlocker[blockerId] = row
        }

        val topologyRiskRow = rowsByBlocker["topology-risk-non-ruins-first-read"]
        assertTrue(
            topologyRiskRow != null,
            "$owner must keep topology-risk non-ruins first-read blocker visible.",
        )
        if (topologyRiskRow != null) {
            assertContains(topologyRiskRow, "`wall-family`", owner)
            assertContains(topologyRiskRow, "`>=3`", owner)
            assertContains(topologyRiskRow, "`counts-as-failure`", owner)
            assertContains(topologyRiskRow, "`ab-decision-required`", owner)
            assertContains(topologyRiskRow, "`direction-change-triggered`", owner)
        }
    }

    private fun sectionBetween(
        markdown: String,
        startHeading: String,
        endHeading: String,
        owner: String,
    ): String {
        val startIndex = markdown.indexOf(startHeading)
        assertTrue(startIndex >= 0, "$owner must contain '$startHeading'.")
        val endIndex = markdown.indexOf(endHeading, startIndex + startHeading.length)
        assertTrue(endIndex > startIndex, "$owner must contain '$endHeading' after '$startHeading'.")
        return markdown.substring(startIndex, endIndex)
    }

    private fun tableCells(row: String): List<String> =
        row
            .trim()
            .trim('|')
            .split("|")
            .map(String::trim)

    private fun unquotedTableCell(cell: String): String =
        cell
            .removePrefix("`")
            .removeSuffix("`")

    private fun requiresDirectionAction(occurrenceCount: String): Boolean =
        occurrenceCount == ">=3" || (occurrenceCount.toIntOrNull()?.let { count -> count >= 3 } ?: false)

    private fun assertPrDocContract(root: Path, prDoc: PrDoc) {
        val markdown = read(root, prDoc.path)
        assertContains(markdown, "## 0. 开发治理与验收矩阵", prDoc.path)
        assertContains(markdown, "development-governance.md", prDoc.path)
        assertContains(markdown, "docs/verification/README.md", prDoc.path)
        assertContains(markdown, "docs/rule/ai-change-governance.md", prDoc.path)
        assertContains(markdown, "### Acceptance Matrix", prDoc.path)
        assertContains(markdown, "### Gate Budget", prDoc.path)
        assertContains(markdown, "### Canonical Artifact", prDoc.path)
        assertContains(markdown, "### Failure Rule", prDoc.path)
        assertContains(markdown, "build/verification/verify-changed/full-task-duration-summary.{json,md}", prDoc.path)

        val rows = acceptanceRows(markdown, prDoc.requirementPrefix)
        assertTrue(
            rows.size >= prDoc.minimumRows,
            "${prDoc.path} must declare at least ${prDoc.minimumRows} acceptance rows for ${prDoc.requirementPrefix}.",
        )
        rows.forEach { row ->
            assertFalse(row.contains("TBD"), "${prDoc.path} acceptance row must not contain TBD: $row")
            assertNoMachinePath(row, prDoc.path)
        }
    }

    private fun assertUiD10RetainedUiContract(root: Path) {
        val docPath = "UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md"
        val manualPath = "UI/manual-records/dark-uiux-pr08-d10-retained-ui.md"
        val openDesignHelperPath = "UI/review/open-design/dark-uiux-pr08-d10-retained-ui-skin-design.md"
        val markdown = read(root, docPath)
        val manualRecord = read(root, manualPath)
        val openDesignHelper = read(root, openDesignHelperPath)
        val owner = "UI08-D10 retained UI contract"

        (0..8).forEach { phase ->
            assertContains(markdown, "D10-P$phase", owner)
        }
        listOf(
            "docs-only authority freeze",
            "Runtime renderer / resource / manifest / golden changes are not valid P0 closure evidence",
            "Phase Transition Checklist",
            "old_route_removed",
            "temporary_adapter",
            "focused_tests",
            "golden_or_actor_tree",
            "packaged_whitebox",
            "next_phase_allowed",
            "Full golden passing alone is not D10-P2 closure evidence",
            "d10StandaloneScreens",
            "d10FocusModalTooltip",
            "d10RetainedShell",
            "d10InventoryEquipment",
            "d10TalentTree",
            "d10FrontstageOverlay",
            "Each D10-P2 through D10-P7 focused evidence tag must have a matching client `Test` task and a root wrapper",
            "Root task to add/run",
            ":client:d10RetainedShell",
            "./gradlew d10RetainedShell --no-configuration-cache",
            "client/build.gradle.kts",
            "tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt",
            "D10-KTX-COMPAT",
            "ktx-scene2d 1.13.1-rc1 declares runtime gdx 1.13.1 and kotlin-stdlib 2.1.10",
            "resolved `com.badlogicgames.gdx:gdx == 1.14.0`",
            "KtomeUiStage does not dispose shared Skin",
            "D10-P4-T0",
            "MapStageRenderFrame",
            "MapStageRenderFrame.fromActor(this)",
            "MapStageActorTest.convertsNestedTableActorBoundsToStageSpace",
            "SpriteBatch-preserving",
            "FoundationViewportSupport.worldWidth/worldHeight/syncViewport",
            "Current executable packaged scenarios after runtime migration",
            "Future dedicated retained-ui scenarios, only after registration",
            "runtimeRoute: \"retained-ui-stage\"",
            "expected-evidence.json",
            "route=retained-ui-stage",
            ".metadata.txt",
            "Phase4V4WhiteboxScenarioCliTest",
            "../review/open-design/dark-uiux-pr08-d10-retained-ui-skin-design.md",
            "../review/open-design/ktome-dark-ui-design.md",
            "MapStageActor.draw() must not call batch.begin",
            "Touchable.disabled",
            "ValidationScenarioRegistry",
            "Phase4V4WhiteboxScenarioMaterializationCatalog",
            "rollback-only quarantined route",
        ).forEach { token ->
            assertContains(markdown, token, owner)
        }

        assertFalse(
            Regex("\\bD10-S\\d+\\b").containsMatchIn(markdown),
            "$docPath must not use numeric D10-S execution labels; use D10-P* phase ids.",
        )
        assertBashBlocksHaveNoAnglePlaceholders(markdown, docPath)
        assertOpenDesignHelperContract(openDesignHelper, openDesignHelperPath)

        assertContains(manualRecord, "runtime migration has not started", manualPath)
        assertContains(manualRecord, "Validation Results", manualPath)
    }

    private fun assertOpenDesignHelperContract(markdown: String, owner: String) {
        listOf(
            "auxiliary Open Design helper",
            "not an implementation contract and not a resource contract",
            "UI/review/open-design/ktome-dark-ui-design.md",
            "test-only in-memory/generated fixture drawables",
            "no committed/generated repo assets in P1",
        ).forEach { token ->
            assertContains(markdown, token, owner)
        }
        markdown.lineSequence().forEach { line ->
            assertNoMachinePath(line, owner)
            assertFalse(line.contains("TODO"), "$owner must not contain TODO markers: $line")
            assertFalse(line.contains("TBD"), "$owner must not contain TBD markers: $line")
            assertFalse(line.contains("FIXME"), "$owner must not contain FIXME markers: $line")
        }
    }

    private fun assertBashBlocksHaveNoAnglePlaceholders(markdown: String, owner: String) {
        var inBashBlock = false
        markdown.lineSequence().forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed == "```bash") {
                inBashBlock = true
            } else if (trimmed.startsWith("```")) {
                inBashBlock = false
            } else if (inBashBlock) {
                val placeholder = BASH_PLACEHOLDER_PATTERN.find(line)?.value
                assertTrue(
                    placeholder == null,
                    "$owner bash command block must not contain placeholder $placeholder at line ${index + 1}: $line",
                )
            }
        }
    }

    private fun assertNoMachinePath(value: String, owner: String) {
        assertFalse(value.contains("/Users/"), "$owner must not contain macOS absolute paths: $value")
        assertFalse(value.contains("/tmp/"), "$owner must not contain tmp absolute paths: $value")
        assertFalse(Regex("[A-Za-z]:\\\\").containsMatchIn(value), "$owner must not contain Windows absolute paths: $value")
    }

    private fun assertUiPr05InventoryReferenceArtifacts(root: Path) {
        assertPngArtifact(
            root,
            PngArtifact(
                relativePath = "UI/dark-uiux-pr03-inventory-page-reference.png",
                expectedWidth = 1672,
                expectedHeight = 941,
                expectedSha256 = "af7ce7992be1838ee75116d89d2284f8c81f48345a46cbe53bb7f50f64e71837",
            ),
        )
        assertTextArtifactHasNoMachinePath(root, "UI/dark-uiux-pr03-inventory-page-reference.prompt.txt")
        assertTextArtifactHasNoMachinePath(root, "UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md")
    }

    private fun assertPngArtifact(
        root: Path,
        artifact: PngArtifact,
    ) {
        val path = root.resolve(artifact.relativePath)
        assertTrue(Files.exists(path), "${artifact.relativePath} must exist.")

        val image = ImageIO.read(path.toFile())
        assertTrue(image != null, "${artifact.relativePath} must be a readable PNG.")
        if (image != null) {
            assertEquals(artifact.expectedWidth, image.width, "${artifact.relativePath} width must stay stable.")
            assertEquals(artifact.expectedHeight, image.height, "${artifact.relativePath} height must stay stable.")
        }
        assertEquals(artifact.expectedSha256, sha256(path), "${artifact.relativePath} sha256 must stay stable.")
    }

    private fun assertTextArtifactHasNoMachinePath(
        root: Path,
        relativePath: String,
    ) {
        val markdown = read(root, relativePath)
        markdown.lineSequence().forEach { line -> assertNoMachinePath(line, relativePath) }
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private data class PrDoc(
        val requirementPrefix: String,
        val path: String,
        val minimumRows: Int,
    )

    private data class PngArtifact(
        val relativePath: String,
        val expectedWidth: Int,
        val expectedHeight: Int,
        val expectedSha256: String,
    )

    private companion object {
        private const val PR08_CONVERGENCE_COLUMN_COUNT = 6

        private val BASH_PLACEHOLDER_PATTERN = Regex("<[^>]+>")

        private val pr08TechniqueFamilies: Set<String> =
            setOf(
                "source-art",
                "alpha-tuning",
                "compositor-rect",
                "wall-family",
                "presentation-structure",
                "layout-ab",
                "interaction-grammar",
                "resource-manifest-wiring",
                "packaged-parity",
                "governance-docs",
            )

        private val pr08FailureCounterImpacts: Set<String> =
            setOf(
                "counts-as-progress",
                "counts-as-failure",
                "freeze-no-more-local-polish",
                "not-visual-progress",
            )

        private val pr08RepeatBlockerActions: Set<String> =
            setOf(
                "direction-change",
                "freeze",
                "explicit-deferral",
                "ab-decision-required",
            )

        private val prDocs: List<PrDoc> =
            listOf(
                PrDoc(
                    requirementPrefix = "PR03",
                    path = "docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md",
                    minimumRows = 7,
                ),
                PrDoc(
                    requirementPrefix = "PR04",
                    path = "docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md",
                    minimumRows = 6,
                ),
                PrDoc(
                    requirementPrefix = "PR05",
                    path = "docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md",
                    minimumRows = 6,
                ),
                PrDoc(
                    requirementPrefix = "PR06",
                    path = "docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md",
                    minimumRows = 5,
                ),
                PrDoc(
                    requirementPrefix = "PR07",
                    path = "docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md",
                    minimumRows = 6,
                ),
            )
        private val uiPrDocs: List<PrDoc> =
            listOf(
                PrDoc(
                    requirementPrefix = "UI00",
                    path = "UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md",
                    minimumRows = 5,
                ),
                PrDoc(
                    requirementPrefix = "UI01",
                    path = "UI/pr/dark-uiux-pr01-client-shell-layout.md",
                    minimumRows = 4,
                ),
                PrDoc(
                    requirementPrefix = "UI01-1",
                    path = "UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md",
                    minimumRows = 7,
                ),
                PrDoc(
                    requirementPrefix = "UI02",
                    path = "UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md",
                    minimumRows = 5,
                ),
                PrDoc(
                    requirementPrefix = "UI02-1",
                    path = "UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md",
                    minimumRows = 8,
                ),
                PrDoc(
                    requirementPrefix = "UI03",
                    path = "UI/pr/dark-uiux-pr03-equipment-inventory-items.md",
                    minimumRows = 5,
                ),
                PrDoc(
                    requirementPrefix = "UI04",
                    path = "UI/pr/dark-uiux-pr04-profession-tree-ui.md",
                    minimumRows = 4,
                ),
                PrDoc(
                    requirementPrefix = "UI05",
                    path = "UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md",
                    minimumRows = 5,
                ),
                PrDoc(
                    requirementPrefix = "UI05-1",
                    path = "UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md",
                    minimumRows = 8,
                ),
                PrDoc(
                    requirementPrefix = "UI06",
                    path = "UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md",
                    minimumRows = 6,
                ),
                PrDoc(
                    requirementPrefix = "UI07",
                    path = "UI/pr/dark-uiux-pr07-golden-whitebox-polish.md",
                    minimumRows = 6,
                ),
                PrDoc(
                    requirementPrefix = "UI08-D10",
                    path = "UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md",
                    minimumRows = 14,
                ),
            )
    }
}
