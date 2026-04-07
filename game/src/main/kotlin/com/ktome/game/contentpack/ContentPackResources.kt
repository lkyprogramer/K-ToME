package com.ktome.game.contentpack

import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object ContentPackResources {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadMergedLocalizationBundle(
        selection: ResolvedContentPackSelection,
        resourceLoader: (String) -> String,
    ): LocalizationBundle {
        val bundles =
            GameLocale.entries.associateWith { locale ->
                val merged = linkedMapOf<String, String>()
                merged += parseLocaleBundle(resourceLoader("/i18n/${locale.id}.json"))
                selection.orderedPacks.forEach { pack ->
                    pack.localeBundlePaths()[locale].orEmpty().forEach { bundlePath ->
                        merged += parseLocaleBundle(Files.readString(bundlePath))
                    }
                }
                merged.toMap(linkedMapOf())
            }
        return LocalizationBundle.fromMaps(bundles)
    }

    fun collectVisualKeys(selection: ResolvedContentPackSelection): Set<String> =
        selection.orderedPacks.flatMapTo(linkedSetOf()) { pack ->
            pack.manifest.visualManifest?.let(pack::resolvePath)?.let(::parseVisualKeys).orEmpty()
        }

    fun collectAudioKeys(selection: ResolvedContentPackSelection): Set<String> =
        selection.orderedPacks.flatMapTo(linkedSetOf()) { pack ->
            pack.manifest.audioManifest?.let(pack::resolvePath)?.let(::parseAudioKeys).orEmpty()
        }

    fun parseLocaleBundle(content: String): Map<String, String> =
        json.parseToJsonElement(content).jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }

    fun parseVisualKeys(manifestPath: java.nio.file.Path): Set<String> =
        json.parseToJsonElement(Files.readString(manifestPath))
            .jsonObject
            .getValue("entries")
            .jsonArray
            .mapTo(linkedSetOf()) { entry ->
                entry.jsonObject.getValue("key").jsonPrimitive.content
            }

    fun parseAudioKeys(manifestPath: java.nio.file.Path): Set<String> =
        json.parseToJsonElement(Files.readString(manifestPath))
            .jsonObject
            .getValue("entries")
            .jsonArray
            .mapTo(linkedSetOf()) { entry ->
                entry.jsonObject.getValue("key").jsonPrimitive.content
            }
}
