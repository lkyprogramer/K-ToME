package com.ktome.tools.lint

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AiIntentLeakRuleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `client and locale do not introduce ordinary enemy intent prediction language`() {
        val root = repoRoot()
        val findings =
            AiIntentLeakRule.validate(
                listOf(
                    root.resolve("client/src/main/kotlin"),
                    root.resolve("game/src/main/resources/i18n"),
                ),
            )

        assertEquals(emptyList<AiIntentLeakFinding>(), findings)
    }

    @Test
    fun `synthetic ordinary enemy intent language is reported`() {
        val file = tempDir.resolve("ordinary-intent.json")
        java.nio.file.Files.writeString(file, """{"hint":"next action: strike","zh":"预测: 斩击"}""")

        val findings = AiIntentLeakRule.validate(listOf(tempDir))

        assertEquals(
            listOf(
                AiIntentLeakFinding(file, "forbidden ordinary intent term '预测:'"),
                AiIntentLeakFinding(file, "forbidden ordinary intent term 'next action:'"),
            ),
            findings,
        )
    }

    private fun repoRoot(): Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.repo.root")) {
                "ktome.repo.root system property is required."
            },
        )
}
