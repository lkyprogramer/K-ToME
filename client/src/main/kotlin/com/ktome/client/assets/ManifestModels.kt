package com.ktome.client.assets

import kotlinx.serialization.Serializable

@Serializable
data class VisualManifest(
    val manifestVersion: Int,
    val styleTag: String,
    val fallbackKey: String,
    val entries: List<VisualManifestEntry>,
    val prefixRules: List<ManifestPrefixRule> = emptyList(),
) {
    init {
        require(manifestVersion > 0) { "manifestVersion must be positive." }
        require(styleTag.isNotBlank()) { "styleTag must not be blank." }
        require(fallbackKey.isNotBlank()) { "fallbackKey must not be blank." }
        require(entries.isNotEmpty()) { "entries must not be empty." }
    }
}

@Serializable
data class VisualManifestEntry(
    val key: String,
    val category: String,
    val rawOutputPath: String,
    val footprint: String,
    val pivotX: Double = 0.5,
    val pivotY: Double = 0.5,
    val tags: List<String> = emptyList(),
    val tintColorHex: String? = null,
) {
    init {
        require(key.isNotBlank()) { "Visual manifest key must not be blank." }
        require(category.isNotBlank()) { "Visual manifest category must not be blank." }
        require(rawOutputPath.isNotBlank()) { "Visual manifest rawOutputPath must not be blank." }
        require(footprint.isNotBlank()) { "Visual manifest footprint must not be blank." }
        require(tintColorHex == null || tintColorHex.matches(HEX_COLOR_PATTERN)) {
            "Visual manifest tintColorHex must match #RRGGBB when present."
        }
    }
}

private val HEX_COLOR_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")

@Serializable
data class AudioManifest(
    val manifestVersion: Int,
    val fallbackKey: String,
    val entries: List<AudioManifestEntry>,
    val prefixRules: List<ManifestPrefixRule> = emptyList(),
) {
    init {
        require(manifestVersion > 0) { "manifestVersion must be positive." }
        require(fallbackKey.isNotBlank()) { "fallbackKey must not be blank." }
        require(entries.isNotEmpty()) { "entries must not be empty." }
    }
}

@Serializable
data class AudioManifestEntry(
    val key: String,
    val cueFamily: String,
    val eventId: String,
    val sourcePath: String,
    val tags: List<String> = emptyList(),
) {
    init {
        require(key.isNotBlank()) { "Audio manifest key must not be blank." }
        require(cueFamily.isNotBlank()) { "Audio manifest cueFamily must not be blank." }
        require(eventId.isNotBlank()) { "Audio manifest eventId must not be blank." }
        require(sourcePath.isNotBlank()) { "Audio manifest sourcePath must not be blank." }
    }
}

@Serializable
data class ManifestPrefixRule(
    val prefix: String,
    val targetKey: String,
) {
    init {
        require(prefix.isNotBlank()) { "Manifest prefix must not be blank." }
        require(targetKey.isNotBlank()) { "Manifest targetKey must not be blank." }
    }
}

data class ResolvedVisualAsset(
    val requestedKey: String,
    val resolvedKey: String,
    val matchedByPrefix: Boolean,
    val fallbackUsed: Boolean,
    val entry: VisualManifestEntry,
)

data class ResolvedAudioCue(
    val requestedKey: String,
    val resolvedKey: String,
    val matchedByPrefix: Boolean,
    val fallbackUsed: Boolean,
    val entry: AudioManifestEntry,
)

fun interface ManifestLogSink {
    fun error(message: String)
}

data class ClientAssetBundle(
    val visualManifest: VisualManifest,
    val audioManifest: AudioManifest,
    val visualResolver: VisualManifestResolver,
    val audioResolver: AudioManifestResolver,
    val textureRepository: ClientTextureRepository,
) {
    fun dispose() {
        textureRepository.dispose()
    }
}
