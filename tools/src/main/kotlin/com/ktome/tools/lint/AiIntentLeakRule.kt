package com.ktome.tools.lint

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

data class AiIntentLeakFinding(
    val path: Path,
    val reason: String,
)

object AiIntentLeakRule {
    private val forbiddenIntentTerms =
        listOf(
            "下一步:",
            "预测:",
            "next action:",
            "predicted",
        )

    fun validate(sourceRoots: List<Path>): List<AiIntentLeakFinding> =
        sourceRoots
            .flatMap { root ->
                Files.walk(root).use { paths ->
                    paths
                        .filter { path -> path.toString().endsWith(".kt") || path.toString().endsWith(".json") }
                        .toList()
                }
            }.flatMap { path ->
                val text = path.readText()
                forbiddenIntentTerms
                    .filter { term -> text.contains(term, ignoreCase = true) }
                    .map { term -> AiIntentLeakFinding(path = path, reason = "forbidden ordinary intent term '$term'") }
            }
}
