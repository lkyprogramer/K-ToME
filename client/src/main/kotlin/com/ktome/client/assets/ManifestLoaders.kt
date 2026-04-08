package com.ktome.client.assets

import com.ktome.game.contentpack.ContentPackRuntimeResolver
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.contentpack.ResolvedContentPack
import com.ktome.game.contentpack.ResolvedContentPackSelection
import java.nio.file.Files
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

    fun loadMerged(
        contentPackSelection: ContentPackSelection,
        path: String = resourcePath,
        resourceLoader: (String) -> InputStream? = { candidate -> VisualManifestResourceLoader::class.java.getResourceAsStream(candidate) },
    ): VisualManifest {
        if (contentPackSelection.isEmpty) {
            return load(path = path, resourceLoader = resourceLoader)
        }
        return loadMerged(
            resolvedContentPackSelection = ContentPackRuntimeResolver.resolve(contentPackSelection),
            path = path,
            resourceLoader = resourceLoader,
        )
    }

    fun loadMerged(
        resolvedContentPackSelection: ResolvedContentPackSelection,
        path: String = resourcePath,
        resourceLoader: (String) -> InputStream? = { candidate -> VisualManifestResourceLoader::class.java.getResourceAsStream(candidate) },
    ): VisualManifest {
        val baseManifest = load(path = path, resourceLoader = resourceLoader)
        if (resolvedContentPackSelection.isEmpty()) {
            return baseManifest
        }
        val additionalManifests =
            resolvedContentPackSelection.orderedPacks.mapNotNull { pack ->
                pack.manifest.visualManifest?.let { relativePath ->
                    decodePackVisualManifest(pack = pack, relativePath = relativePath)
                }
            }
        return additionalManifests.fold(baseManifest, ::mergeVisualManifest)
    }

    internal fun decode(
        content: String,
        path: String,
    ): VisualManifest =
        try {
            json.decodeFromString<VisualManifest>(content)
        } catch (exception: SerializationException) {
            throw ManifestLoadException("Visual manifest is invalid: $path", exception)
        } catch (exception: IllegalArgumentException) {
            throw ManifestLoadException("Visual manifest is invalid: $path", exception)
        }

    private fun InputStream.decode(path: String): VisualManifest =
        use { input -> decode(input.readBytes().decodeToString(), path) }
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

    fun loadMerged(
        contentPackSelection: ContentPackSelection,
        path: String = resourcePath,
        resourceLoader: (String) -> InputStream? = { candidate -> AudioManifestResourceLoader::class.java.getResourceAsStream(candidate) },
    ): AudioManifest {
        if (contentPackSelection.isEmpty) {
            return load(path = path, resourceLoader = resourceLoader)
        }
        return loadMerged(
            resolvedContentPackSelection = ContentPackRuntimeResolver.resolve(contentPackSelection),
            path = path,
            resourceLoader = resourceLoader,
        )
    }

    fun loadMerged(
        resolvedContentPackSelection: ResolvedContentPackSelection,
        path: String = resourcePath,
        resourceLoader: (String) -> InputStream? = { candidate -> AudioManifestResourceLoader::class.java.getResourceAsStream(candidate) },
    ): AudioManifest {
        val baseManifest = load(path = path, resourceLoader = resourceLoader)
        if (resolvedContentPackSelection.isEmpty()) {
            return baseManifest
        }
        val additionalManifests =
            resolvedContentPackSelection.orderedPacks.mapNotNull { pack ->
                pack.manifest.audioManifest?.let { relativePath ->
                    decodePackAudioManifest(pack = pack, relativePath = relativePath)
                }
            }
        return additionalManifests.fold(baseManifest, ::mergeAudioManifest)
    }

    internal fun decode(
        content: String,
        path: String,
    ): AudioManifest =
        try {
            json.decodeFromString<AudioManifest>(content)
        } catch (exception: SerializationException) {
            throw ManifestLoadException("Audio manifest is invalid: $path", exception)
        } catch (exception: IllegalArgumentException) {
            throw ManifestLoadException("Audio manifest is invalid: $path", exception)
        }

    private fun InputStream.decode(path: String): AudioManifest =
        use { input -> decode(input.readBytes().decodeToString(), path) }
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

    fun load(
        contentPackSelection: ContentPackSelection,
        logSink: ManifestLogSink = ManifestLogSink { message -> System.err.println(message) },
    ): ClientAssetBundle {
        if (contentPackSelection.isEmpty) {
            return load(logSink = logSink)
        }
        return load(
            resolvedContentPackSelection = ContentPackRuntimeResolver.resolve(contentPackSelection),
            logSink = logSink,
        )
    }

    fun load(
        resolvedContentPackSelection: ResolvedContentPackSelection,
        logSink: ManifestLogSink = ManifestLogSink { message -> System.err.println(message) },
    ): ClientAssetBundle =
        load(
            visualManifestProvider = { VisualManifestResourceLoader.loadMerged(resolvedContentPackSelection) },
            audioManifestProvider = { AudioManifestResourceLoader.loadMerged(resolvedContentPackSelection) },
            logSink = logSink,
        )
}

private fun mergeVisualManifest(
    base: VisualManifest,
    overlay: VisualManifest,
): VisualManifest =
    VisualManifest(
        manifestVersion = base.manifestVersion,
        styleTag = base.styleTag,
        fallbackKey = base.fallbackKey,
        entries = base.entries + overlay.entries,
        prefixRules = base.prefixRules + overlay.prefixRules,
    )

private fun mergeAudioManifest(
    base: AudioManifest,
    overlay: AudioManifest,
): AudioManifest =
    AudioManifest(
        manifestVersion = base.manifestVersion,
        fallbackKey = base.fallbackKey,
        entries = base.entries + overlay.entries,
        prefixRules = base.prefixRules + overlay.prefixRules,
    )

private fun decodePackVisualManifest(
    pack: ResolvedContentPack,
    relativePath: String,
): VisualManifest {
    val manifestPath = pack.resolvePath(relativePath)
    val decoded = VisualManifestResourceLoader.decode(Files.readString(manifestPath), manifestPath.toString())
    return decoded.copy(
        entries =
            decoded.entries.map { entry ->
                entry.copy(rawOutputPath = pack.resolvePath(entry.rawOutputPath).toString())
            },
    )
}

private fun decodePackAudioManifest(
    pack: ResolvedContentPack,
    relativePath: String,
): AudioManifest {
    val manifestPath = pack.resolvePath(relativePath)
    val decoded = AudioManifestResourceLoader.decode(Files.readString(manifestPath), manifestPath.toString())
    return decoded.copy(
        entries =
            decoded.entries.map { entry ->
                entry.copy(sourcePath = pack.resolvePath(entry.sourcePath).toString())
            },
    )
}
