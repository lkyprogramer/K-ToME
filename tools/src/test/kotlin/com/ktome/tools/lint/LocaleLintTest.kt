package com.ktome.tools.lint

import com.ktome.game.i18n.GameLocale
import java.awt.Font
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("localeLint")
class LocaleLintTest {
    private val en = LintFixtures.loadLocale(GameLocale.EN_US)
    private val zh = LintFixtures.loadLocale(GameLocale.ZH_CN)
    private val referencedKeys = LintFixtures.schemaReferencedKeys() + LintFixtures.codeReferencedLocaleKeys()

    @Test
    fun `locale bundles stay in lockstep and cover all schema referenced keys`() {
        assertEquals(en.keys, zh.keys, "Locale bundles must expose the same key set.")
        val missingKeys = referencedKeys - en.keys
        val extraKeys = en.keys - referencedKeys
        assertTrue(
            missingKeys.isEmpty() && extraKeys.isEmpty(),
            "Locale bundles must match the union of schema and code referenced keys. " +
                "missing=${missingKeys.take(20)} extra=${extraKeys.take(20)}",
        )

        val allowedPrefixes =
            listOf(
                "ui.",
                "log.",
                "tile.",
                "terrain.",
                "damage_type.",
                "actor.",
                "stairs.",
                "status.",
                "ai.",
                "profession.",
                "race.",
                "inscription.",
                "talent_tree.",
                "talent.",
                "monster.",
                "boss.",
                "zone.",
                "shop.",
                "difficulty.",
                "material.",
                "affix.",
                "item.",
                "interactable.",
                "objective.",
                "mutation.",
                "keyword.",
            )
        en.keys.forEach { key ->
            assertTrue(allowedPrefixes.any(key::startsWith), "Unexpected locale key namespace: $key")
        }
    }

    @Test
    fun `placeholder contracts and formal text entries are valid`() {
        en.keys.forEach { key ->
            assertEquals(placeholders(en.getValue(key)), placeholders(zh.getValue(key)), "Placeholder mismatch for key $key")
            if (key.endsWith(".name") || key.endsWith(".desc")) {
                assertTrue(en.getValue(key).isNotBlank(), "English value is blank for $key")
                assertTrue(zh.getValue(key).isNotBlank(), "Chinese value is blank for $key")
            }
        }
    }

    @Test
    fun `formal schema does not regress to bare name or desc fields`() {
        LintFixtures.formalObjectMaps().forEach { entry ->
            assertFalse(entry.containsKey("name"), "Formal schema cannot contain bare 'name': $entry")
            assertFalse(entry.containsKey("desc"), "Formal schema cannot contain bare 'desc': $entry")
            assertFalse(entry.containsKey("description"), "Formal schema cannot contain bare 'description': $entry")
        }
    }

    @Test
    fun `locale lint owns ui font glyph coverage and forbids manual glyph catalogs`() {
        val legacyGlyphCatalog = LintFixtures.legacyUiGlyphCatalogPath()
        assertFalse(legacyGlyphCatalog.exists(), "Manual UI glyph catalog must not be checked in: $legacyGlyphCatalog")

        val fontPath = LintFixtures.bundledUiFontPath()
        assertTrue(fontPath.exists(), "Bundled UI font is missing: $fontPath")

        val noticeText = LintFixtures.bundledUiFontNoticePath().readText()
        assertTrue(noticeText.contains("full Simplified Chinese coverage"), "Bundled font notice must describe the full-coverage contract.")
        assertTrue(noticeText.contains("Original font file"), "Bundled font notice must preserve upstream source provenance.")

        val requiredGlyphs = LintFixtures.requiredUiGlyphs()
        fontPath.toFile().inputStream().use { input ->
            val font = Font.createFont(Font.TRUETYPE_FONT, input)
            val missingGlyphs = requiredGlyphs.filterNot(font::canDisplay)
            assertTrue(
                missingGlyphs.isEmpty(),
                "Bundled UI font is missing ${missingGlyphs.size} locale glyph(s): ${missingGlyphs.take(20).joinToString(separator = "")}",
            )
        }
    }

    private fun placeholders(template: String): Set<String> =
        "\\{([a-zA-Z0-9_]+)}".toRegex().findAll(template).map { match -> match.groupValues[1] }.toSet()
}
