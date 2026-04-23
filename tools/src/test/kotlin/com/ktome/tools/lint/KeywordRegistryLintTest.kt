package com.ktome.tools.lint

import com.ktome.core.talent.KeywordRegistry
import com.ktome.core.talent.DynamicDescriptionResolver
import com.ktome.core.talent.KeywordSemantic
import com.ktome.core.talent.KeywordSemanticType
import com.ktome.core.status.StatusEffectType
import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("keywordRegistryLint")
class KeywordRegistryLintTest {
    @Test
    fun `repository description keyword consumers resolve through core registry`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val talentKeywordIds = catalog.talents.flatMap { talent -> talent.keywords }
        val formalDescriptionLocaleKeys =
            LintFixtures.schemaFieldValues("templateKey") +
                DynamicDescriptionResolver.BREAKPOINT_TEMPLATE_KEYS
        val formalDescriptionMarkupIds =
            GameLocale.entries.flatMap { locale ->
                LintFixtures.localeKeywordMarkupIds(locale, formalDescriptionLocaleKeys)
            }
        val formalDescriptionKeywordIds = talentKeywordIds + formalDescriptionMarkupIds
        val statusPresentationKeywordIds =
            StatusEffectType
                .values()
                .map { type -> type.schemaId.lowercase() }
                .filter { keywordId -> KeywordRegistry.CORE.resolve(keywordId) != null } +
                "telegraph"
        val findings =
            KeywordRegistryLintRule.validate(
                KeywordRegistryLintRequest(
                    consumedKeywordIds = formalDescriptionKeywordIds + statusPresentationKeywordIds,
                    formalSurfaceKeywordIds = formalDescriptionKeywordIds + statusPresentationKeywordIds,
                    coverageMode = KeywordRegistryCoverageMode.FULL_REGISTRY,
                    missingFormalCoverageSeverity = KeywordRegistryLintSeverity.WARN,
                ),
            )

        assertEquals(
            listOf("diminishing_returns", "dispel", "dot", "penetration", "power_save", "single_target", "sustain"),
            findings
                .filter { finding -> finding.severity == KeywordRegistryLintSeverity.WARN }
                .map(KeywordRegistryLintFinding::keywordId),
        )
        assertEquals(emptyList<KeywordRegistryLintFinding>(), findings.filter { finding -> finding.severity != KeywordRegistryLintSeverity.WARN })
    }

    @Test
    fun `unknown consumed keyword ids fail fast`() {
        val findings =
            KeywordRegistryLintRule.validate(
                KeywordRegistryLintRequest(
                    consumedKeywordIds = listOf("damage", "missing_keyword"),
                    formalSurfaceKeywordIds = listOf("damage"),
                    coverageMode = KeywordRegistryCoverageMode.CONSUMED_ONLY,
                ),
            )

        assertTrue(findings.any { finding -> finding.severity == KeywordRegistryLintSeverity.ERROR && finding.keywordId == "missing_keyword" })
    }

    @Test
    fun `unknown registry related keyword ids fail fast`() {
        val registry =
            KeywordRegistry(
                mapOf(
                    "known" to
                        KeywordSemantic(
                            id = "known",
                            type = KeywordSemanticType.UTILITY,
                            nameKey = "keyword.known.name",
                            tooltipKey = "keyword.known.tooltip",
                            relatedKeywords = listOf("missing_related"),
                        ),
                ),
            )

        val findings =
            KeywordRegistryLintRule.validate(
                KeywordRegistryLintRequest(
                    consumedKeywordIds = listOf("known"),
                    formalSurfaceKeywordIds = listOf("known"),
                    coverageMode = KeywordRegistryCoverageMode.CONSUMED_ONLY,
                    registry = registry,
                ),
            )

        assertTrue(findings.any { finding -> finding.severity == KeywordRegistryLintSeverity.ERROR && finding.keywordId == "missing_related" })
    }

    @Test
    fun `insufficient formal coverage is blocked when full registry coverage is required`() {
        val registry =
            KeywordRegistry(
                mapOf(
                    "known" to KeywordSemantic("known", KeywordSemanticType.UTILITY, "keyword.known.name", "keyword.known.tooltip"),
                    "uncovered" to KeywordSemantic("uncovered", KeywordSemanticType.UTILITY, "keyword.uncovered.name", "keyword.uncovered.tooltip"),
                ),
            )

        val findings =
            KeywordRegistryLintRule.validate(
                KeywordRegistryLintRequest(
                    consumedKeywordIds = listOf("known"),
                    formalSurfaceKeywordIds = listOf("known"),
                    coverageMode = KeywordRegistryCoverageMode.FULL_REGISTRY,
                    registry = registry,
                ),
            )

        assertTrue(findings.any { finding -> finding.severity == KeywordRegistryLintSeverity.BLOCKED && finding.keywordId == "uncovered" })
    }
}
