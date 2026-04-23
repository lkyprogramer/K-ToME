package com.ktome.tools.lint

import com.ktome.core.talent.KeywordRegistry

enum class KeywordRegistryLintSeverity {
    WARN,
    ERROR,
    BLOCKED,
}

data class KeywordRegistryLintFinding(
    val severity: KeywordRegistryLintSeverity,
    val keywordId: String,
    val message: String,
)

enum class KeywordRegistryCoverageMode {
    CONSUMED_ONLY,
    FULL_REGISTRY,
}

data class KeywordRegistryLintRequest(
    val consumedKeywordIds: Iterable<String>,
    val formalSurfaceKeywordIds: Iterable<String>,
    val coverageMode: KeywordRegistryCoverageMode,
    val registry: KeywordRegistry = KeywordRegistry.CORE,
    val missingFormalCoverageSeverity: KeywordRegistryLintSeverity = KeywordRegistryLintSeverity.BLOCKED,
)

object KeywordRegistryLintRule {
    fun validate(request: KeywordRegistryLintRequest): List<KeywordRegistryLintFinding> {
        val consumed = request.consumedKeywordIds.filter(String::isNotBlank).toSet()
        val formal = request.formalSurfaceKeywordIds.filter(String::isNotBlank).toSet()
        val registryEntries = request.registry.all()
        val known = registryEntries.map { semantic -> semantic.id }.toSet()
        val registryReferences = registryEntries.flatMap { semantic -> semantic.relatedKeywords }.filter(String::isNotBlank).toSet()
        val findings = mutableListOf<KeywordRegistryLintFinding>()

        (consumed + formal + registryReferences)
            .filterNot(known::contains)
            .sorted()
            .forEach { keywordId ->
                findings +=
                    KeywordRegistryLintFinding(
                        severity = KeywordRegistryLintSeverity.ERROR,
                        keywordId = keywordId,
                        message = "Unknown keyword id '$keywordId'.",
                    )
            }

        if (request.coverageMode == KeywordRegistryCoverageMode.FULL_REGISTRY) {
            known
                .filterNot(formal::contains)
                .sorted()
                .forEach { keywordId ->
                    findings +=
                        KeywordRegistryLintFinding(
                            severity = request.missingFormalCoverageSeverity,
                            keywordId = keywordId,
                            message = "Keyword '$keywordId' is not covered by a formal description surface.",
                        )
                }
        }

        return findings
    }
}
