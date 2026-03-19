package com.ktome.client.assets

import java.io.InputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ManifestLoadException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

object VisualManifestResourceLoader {
    private const val resourcePath: String = "/manifests/visual-manifest.json"
    private val json = Json { ignoreUnknownKeys = false }

    fun load(
        path: String = resourcePath,
        resourceLoader: (String) -> InputStream? = { candidate -> VisualManifestResourceLoader::class.java.getResourceAsStream(candidate) },
    ): VisualManifest {
        val stream = resourceLoader(path) ?: throw ManifestLoadException("Visual manifest resource not found: $path")
        return stream.decode(path)
    }

    private fun InputStream.decode(path: String): VisualManifest =
        use { input ->
            try {
                json.decodeFromString<VisualManifest>(input.readBytes().decodeToString())
            } catch (exception: SerializationException) {
                throw ManifestLoadException("Visual manifest is invalid: $path", exception)
            } catch (exception: IllegalArgumentException) {
                throw ManifestLoadException("Visual manifest is invalid: $path", exception)
            }
        }
}

object AudioManifestResourceLoader {
    private const val resourcePath: String = "/manifests/audio-manifest.json"
    private val json = Json { ignoreUnknownKeys = false }

    fun load(
        path: String = resourcePath,
        resourceLoader: (String) -> InputStream? = { candidate -> AudioManifestResourceLoader::class.java.getResourceAsStream(candidate) },
    ): AudioManifest {
        val stream = resourceLoader(path) ?: throw ManifestLoadException("Audio manifest resource not found: $path")
        return stream.decode(path)
    }

    private fun InputStream.decode(path: String): AudioManifest =
        use { input ->
            try {
                json.decodeFromString<AudioManifest>(input.readBytes().decodeToString())
            } catch (exception: SerializationException) {
                throw ManifestLoadException("Audio manifest is invalid: $path", exception)
            } catch (exception: IllegalArgumentException) {
                throw ManifestLoadException("Audio manifest is invalid: $path", exception)
            }
        }
}

object ClientAssetBundleLoader {
    fun load(
        visualManifestProvider: () -> VisualManifest = VisualManifestResourceLoader::load,
        audioManifestProvider: () -> AudioManifest = AudioManifestResourceLoader::load,
        logSink: ManifestLogSink = ManifestLogSink { message -> System.err.println(message) },
    ): ClientAssetBundle {
        try {
            val visualManifest = visualManifestProvider()
            val audioManifest = audioManifestProvider()
            return ClientAssetBundle(
                visualManifest = visualManifest,
                audioManifest = audioManifest,
                visualResolver = VisualManifestResolver(visualManifest, logSink),
                audioResolver = AudioManifestResolver(audioManifest, logSink),
                textureRepository = ClientTextureRepository(),
            )
        } catch (exception: IllegalArgumentException) {
            throw ManifestLoadException("Client asset bundle is invalid.", exception)
        }
    }
}
