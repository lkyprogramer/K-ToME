package com.ktome.client.render

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

internal object KtomeFonts {
    private const val uiFontResourcePath = "/fonts/ktome-ui-subset.otf"
    private const val uiGlyphCatalogResourcePath = "/fonts/ktome-ui-glyphs.txt"
    private const val extractedFontFileName = "ktome-ui-subset.otf"
    private val extractedUiFontPath by lazy(::extractBundledUiFont)
    private val uiGlyphCatalog by lazy(::loadUiGlyphCatalog)

    fun createUiFont(size: Int): BitmapFont {
        val generator = FreeTypeFontGenerator(FileHandle(extractedUiFontPath.toFile()))
        return try {
            generator.generateFont(
                FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                    this.size = size
                    kerning = true
                    incremental = false
                    characters = uiGlyphCatalog
                },
            ).apply {
                setUseIntegerPositions(true)
                color = Color.WHITE
            }
        } finally {
            generator.dispose()
        }
    }

    internal fun bundledUiFontExists(): Boolean = uiFontStreamOrNull()?.use { true } ?: false

    private fun extractBundledUiFont() =
        Files.createTempDirectory("ktome-ui-font").resolve(extractedFontFileName).also { fontPath ->
            if (!fontPath.exists()) {
                checkNotNull(uiFontStreamOrNull()) {
                    "Bundled UI font is missing at $uiFontResourcePath."
                }.use { stream ->
                    Files.copy(stream, fontPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
            fontPath.toFile().deleteOnExit()
            fontPath.parent.toFile().deleteOnExit()
        }

    private fun uiFontStreamOrNull() = KtomeFonts::class.java.getResourceAsStream(uiFontResourcePath)

    private fun loadUiGlyphCatalog(): String =
        checkNotNull(KtomeFonts::class.java.getResourceAsStream(uiGlyphCatalogResourcePath)) {
            "Bundled UI glyph catalog is missing at $uiGlyphCatalogResourcePath."
        }.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
}
