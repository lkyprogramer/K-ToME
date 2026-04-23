package com.ktome.tools.lint

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CombatAffordanceResourceAuditRuleTest {
    @Test
    fun `combat affordance formal keys are in manifests and consumed by client code`() {
        val root = repoRoot()
        val findings =
            CombatAffordanceResourceAuditRule.validate(
                CombatAffordanceResourceAuditRequest(
                    visualManifestPath = root.resolve("client/src/main/resources/manifests/visual-manifest.json"),
                    audioManifestPath = root.resolve("client/src/main/resources/manifests/audio-manifest.json"),
                    runtimeSourceRoots = listOf(root.resolve("client/src/main/kotlin")),
                    verificationSourceRoots =
                        listOf(
                            root.resolve("client/src/test/kotlin"),
                            root.resolve("tools/src/test/kotlin"),
                        ),
                ),
            )

        assertEquals(emptyList<CombatAffordanceResourceAuditFinding>(), findings)
    }

    private fun repoRoot(): Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.repo.root")) {
                "ktome.repo.root system property is required."
            },
        )
}
