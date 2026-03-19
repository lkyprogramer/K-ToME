package com.ktome.client.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.utils.Disposable

class ClientTextureRepository : Disposable {
    private val texturesByPath = linkedMapOf<String, Texture>()

    fun preload(asset: ResolvedVisualAsset) {
        if (!runtimeReady()) {
            return
        }
        textureFor(asset)
    }

    fun textureFor(asset: ResolvedVisualAsset): Texture {
        require(runtimeReady()) {
            "Texture '${asset.entry.rawOutputPath}' requested before libGDX graphics initialization."
        }
        return texturesByPath.getOrPut(asset.entry.rawOutputPath) {
            loadTexture(asset.entry.rawOutputPath)
        }
    }

    internal fun loadedPaths(): Set<String> = texturesByPath.keys.toSet()

    override fun dispose() {
        texturesByPath.values.forEach(Texture::dispose)
        texturesByPath.clear()
    }

    private fun loadTexture(resourcePath: String): Texture {
        val bytes =
            requireNotNull(ClientTextureRepository::class.java.getResourceAsStream("/$resourcePath")) {
                "Texture resource '/$resourcePath' is missing."
            }.use { stream -> stream.readBytes() }
        val pixmap = Pixmap(bytes, 0, bytes.size)
        return Texture(pixmap).also { texture ->
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
            pixmap.dispose()
        }
    }

    private fun runtimeReady(): Boolean =
        Gdx.app != null && Gdx.gl != null
}
