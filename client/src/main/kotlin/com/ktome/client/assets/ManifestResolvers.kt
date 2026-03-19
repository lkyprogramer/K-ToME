package com.ktome.client.assets

class VisualManifestResolver(
    private val manifest: VisualManifest,
    private val logSink: ManifestLogSink = ManifestLogSink { message -> System.err.println(message) },
) {
    private val entriesByKey = manifest.entries.associateBy(VisualManifestEntry::key)
    private val prefixRules = manifest.prefixRules.sortedByDescending { it.prefix.length }

    init {
        require(entriesByKey.containsKey(manifest.fallbackKey)) {
            "Visual manifest fallback key '${manifest.fallbackKey}' is missing."
        }
        val duplicateKeys = manifest.entries.groupingBy(VisualManifestEntry::key).eachCount().filterValues { it > 1 }.keys
        require(duplicateKeys.isEmpty()) { "Duplicate visual manifest keys: ${duplicateKeys.joinToString()}" }
    }

    fun canResolve(key: String): Boolean = key in entriesByKey

    fun resolve(key: String): ResolvedVisualAsset {
        entriesByKey[key]?.let { entry ->
            return ResolvedVisualAsset(
                requestedKey = key,
                resolvedKey = key,
                matchedByPrefix = false,
                fallbackUsed = false,
                entry = entry,
            )
        }

        val prefixed = prefixRules.firstOrNull { key.startsWith(it.prefix) }
        if (prefixed != null) {
            val target = requireNotNull(entriesByKey[prefixed.targetKey]) {
                "Visual manifest prefix '${prefixed.prefix}' points to unknown key '${prefixed.targetKey}'."
            }
            logSink.error("Visual manifest fallback for '$key' via prefix '${prefixed.prefix}' -> '${prefixed.targetKey}'.")
            return ResolvedVisualAsset(
                requestedKey = key,
                resolvedKey = prefixed.targetKey,
                matchedByPrefix = true,
                fallbackUsed = prefixed.targetKey == manifest.fallbackKey,
                entry = target,
            )
        }

        val fallback = requireNotNull(entriesByKey[manifest.fallbackKey]) {
            "Visual manifest fallback key '${manifest.fallbackKey}' is missing."
        }
        logSink.error("Visual manifest missing '$key'; using fallback '${manifest.fallbackKey}'.")
        return ResolvedVisualAsset(
            requestedKey = key,
            resolvedKey = manifest.fallbackKey,
            matchedByPrefix = false,
            fallbackUsed = true,
            entry = fallback,
        )
    }
}

class AudioManifestResolver(
    private val manifest: AudioManifest,
    private val logSink: ManifestLogSink = ManifestLogSink { message -> System.err.println(message) },
) {
    private val entriesByKey = manifest.entries.associateBy(AudioManifestEntry::key)
    private val prefixRules = manifest.prefixRules.sortedByDescending { it.prefix.length }

    init {
        require(entriesByKey.containsKey(manifest.fallbackKey)) {
            "Audio manifest fallback key '${manifest.fallbackKey}' is missing."
        }
        val duplicateKeys = manifest.entries.groupingBy(AudioManifestEntry::key).eachCount().filterValues { it > 1 }.keys
        require(duplicateKeys.isEmpty()) { "Duplicate audio manifest keys: ${duplicateKeys.joinToString()}" }
    }

    fun canResolve(key: String): Boolean = key in entriesByKey

    fun resolve(key: String): ResolvedAudioCue {
        entriesByKey[key]?.let { entry ->
            return ResolvedAudioCue(
                requestedKey = key,
                resolvedKey = key,
                matchedByPrefix = false,
                fallbackUsed = false,
                entry = entry,
            )
        }

        val prefixed = prefixRules.firstOrNull { key.startsWith(it.prefix) }
        if (prefixed != null) {
            val target = requireNotNull(entriesByKey[prefixed.targetKey]) {
                "Audio manifest prefix '${prefixed.prefix}' points to unknown key '${prefixed.targetKey}'."
            }
            logSink.error("Audio manifest fallback for '$key' via prefix '${prefixed.prefix}' -> '${prefixed.targetKey}'.")
            return ResolvedAudioCue(
                requestedKey = key,
                resolvedKey = prefixed.targetKey,
                matchedByPrefix = true,
                fallbackUsed = prefixed.targetKey == manifest.fallbackKey,
                entry = target,
            )
        }

        val fallback = requireNotNull(entriesByKey[manifest.fallbackKey]) {
            "Audio manifest fallback key '${manifest.fallbackKey}' is missing."
        }
        logSink.error("Audio manifest missing '$key'; using fallback '${manifest.fallbackKey}'.")
        return ResolvedAudioCue(
            requestedKey = key,
            resolvedKey = manifest.fallbackKey,
            matchedByPrefix = false,
            fallbackUsed = true,
            entry = fallback,
        )
    }
}
