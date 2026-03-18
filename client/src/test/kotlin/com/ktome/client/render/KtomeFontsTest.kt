package com.ktome.client.render

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class KtomeFontsTest {
    @Test
    fun `ui font bundle exists and includes required zh cn glyphs`() {
        withHeadlessGdx {
            assertTrue(KtomeFonts.bundledUiFontExists())

            val font = KtomeFonts.createUiFont(size = 18)
            try {
                listOf('H', '层', '你', '装', '语', '简').forEach { glyph ->
                    assertTrue(font.data.hasGlyph(glyph), "Missing glyph '$glyph' in bundled UI font.")
                }
            } finally {
                font.dispose()
            }
        }
    }

    private fun <T> withHeadlessGdx(block: () -> T): T {
        val backend = HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        val noOpGl =
            Proxy.newProxyInstance(
                GL20::class.java.classLoader,
                arrayOf(GL20::class.java),
            ) { _, method, _ ->
                when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Float.TYPE -> 0f
                    java.lang.Long.TYPE -> 0L
                    java.lang.Double.TYPE -> 0.0
                    java.lang.Short.TYPE -> 0.toShort()
                    java.lang.Byte.TYPE -> 0.toByte()
                    java.lang.Character.TYPE -> 0.toChar()
                    else -> null
                }
            } as GL20
        Gdx.gl = noOpGl
        Gdx.gl20 = noOpGl
        return try {
            block()
        } finally {
            backend.exit()
        }
    }
}
