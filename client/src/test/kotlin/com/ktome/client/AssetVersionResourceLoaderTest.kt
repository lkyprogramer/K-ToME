package com.ktome.client

import com.ktome.core.save.AssetVersionContract
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AssetVersionResourceLoaderTest {
    @Test
    fun `default asset version contract is loaded from runtime resource`() {
        assertEquals(AssetVersionContract.CURRENT, AssetVersionResourceLoader.load())
    }

    @Test
    fun `missing manifest fails explicitly`() {
        assertThrows(AssetVersionLoadException::class.java) {
            AssetVersionResourceLoader.load(resourceLoader = { null })
        }
    }

    @Test
    fun `invalid manifest fails explicitly`() {
        assertThrows(AssetVersionLoadException::class.java) {
            AssetVersionResourceLoader.load(
                resourceLoader = {
                    ByteArrayInputStream("""{"styleVersion":""}""".toByteArray())
                },
            )
        }
    }
}
