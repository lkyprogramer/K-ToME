package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
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
        assertContains(uiReadme, "development-governance.md", "UI/pr README")
        assertContains(uiReadme, "acceptanceContractLint", "UI/pr README")
        assertContains(uiReadme, "Gate ladder 固定", "UI/pr README")

        prDocs.forEach { prDoc ->
            assertPrDocContract(root, prDoc)
        }
        uiPrDocs.forEach { prDoc ->
            assertPrDocContract(root, prDoc)
        }

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

    private fun assertNoMachinePath(value: String, owner: String) {
        assertFalse(value.contains("/Users/"), "$owner must not contain macOS absolute paths: $value")
        assertFalse(value.contains("/tmp/"), "$owner must not contain tmp absolute paths: $value")
        assertFalse(Regex("[A-Za-z]:\\\\").containsMatchIn(value), "$owner must not contain Windows absolute paths: $value")
    }

    private data class PrDoc(
        val requirementPrefix: String,
        val path: String,
        val minimumRows: Int,
    )

    private companion object {
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
                    requirementPrefix = "UI06",
                    path = "UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md",
                    minimumRows = 6,
                ),
                PrDoc(
                    requirementPrefix = "UI07",
                    path = "UI/pr/dark-uiux-pr07-golden-whitebox-polish.md",
                    minimumRows = 6,
                ),
            )
    }
}
