package com.ktome.game.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object UiGlyphCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    fun requiredGlyphs(
        resourceLoader: (String) -> String = { path ->
            checkNotNull(UiGlyphCatalog::class.java.getResource(path)) {
                "UI glyph resource is missing at $path."
            }.readText()
        },
    ): LinkedHashSet<Char> =
        linkedSetOf<Char>().apply {
            // Keep runtime formatting symbols and ASCII UI tokens stable even when locale templates change.
            addAll((32..126).map(Int::toChar))
            GameLocale.entries.forEach { locale ->
                val bundle = json.parseToJsonElement(resourceLoader("/i18n/${locale.id}.json")).jsonObject
                bundle.values.forEach { value ->
                    value.jsonPrimitive.content.forEach { glyph ->
                        if (!Character.isISOControl(glyph)) {
                            add(glyph)
                        }
                    }
                }
            }
        }

    fun requiredGlyphString(
        resourceLoader: (String) -> String = { path ->
            checkNotNull(UiGlyphCatalog::class.java.getResource(path)) {
                "UI glyph resource is missing at $path."
            }.readText()
        },
    ): String = requiredGlyphs(resourceLoader).joinToString(separator = "")
}
