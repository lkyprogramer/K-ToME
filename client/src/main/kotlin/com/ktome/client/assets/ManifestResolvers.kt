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

    private val terrainVariantFamilies = buildTerrainVariantFamilies()
    private val terrainWallFamilies = buildTerrainWallFamilies()

    fun canResolve(key: String): Boolean = key in entriesByKey

    fun terrainVariantKeys(baseKey: String): List<String> =
        terrainVariantFamilies[baseKey]?.map(VisualManifestEntry::key) ?: listOf(baseKey)

    fun terrainWallFamilyKeys(baseKey: String): List<String> =
        terrainWallFamilies[baseKey]?.let { family ->
            TerrainWallPieceRole.entries.map { role -> requireNotNull(family[role]).key }
        } ?: listOf(baseKey)

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

    fun resolveTerrainVariant(
        baseKey: String,
        variant: Int,
    ): ResolvedVisualAsset {
        val family = terrainVariantFamilies[baseKey] ?: return resolve(baseKey)
        val entry = family[Math.floorMod(variant, family.size)]
        return ResolvedVisualAsset(
            requestedKey = baseKey,
            resolvedKey = entry.key,
            matchedByPrefix = false,
            fallbackUsed = false,
            entry = entry,
        )
    }

    fun resolveTerrainWallPiece(
        baseKey: String,
        role: TerrainWallPieceRole,
    ): ResolvedVisualAsset {
        val family = terrainWallFamilies[baseKey] ?: return resolve(baseKey)
        val entry = requireNotNull(family[role]) {
            "Terrain wall family '$baseKey' is missing role '${role.tagValue}'."
        }
        return ResolvedVisualAsset(
            requestedKey = baseKey,
            resolvedKey = entry.key,
            matchedByPrefix = false,
            fallbackUsed = false,
            entry = entry,
        )
    }

    private fun buildTerrainVariantFamilies(): Map<String, List<VisualManifestEntry>> {
        val byFamily = linkedMapOf<String, MutableMap<Int, VisualManifestEntry>>()
        manifest.entries.forEach { entry ->
            val familyTags = entry.tags.mapNotNull { tag -> tag.removePrefixOrNull(TERRAIN_VARIANT_FAMILY_TAG) }
            val indexTags = entry.tags.mapNotNull { tag -> tag.removePrefixOrNull(TERRAIN_VARIANT_INDEX_TAG) }
            if (familyTags.isEmpty() && indexTags.isEmpty()) {
                return@forEach
            }
            require(familyTags.size == 1 && indexTags.size == 1) {
                "Terrain variant entry '${entry.key}' must declare exactly one family tag and one index tag."
            }
            val baseKey = familyTags.single()
            val index =
                indexTags
                    .single()
                    .toIntOrNull()
                    ?.also { value -> require(value >= 0) { "Terrain variant index for '${entry.key}' must be non-negative." } }
                    ?: error("Terrain variant index for '${entry.key}' must be an integer.")
            val variants = byFamily.getOrPut(baseKey) { linkedMapOf() }
            require(index !in variants) {
                "Duplicate terrain variant index $index for family '$baseKey'."
            }
            variants[index] = entry
        }
        return byFamily.mapValues { (baseKey, variantsByIndex) ->
            val base = requireNotNull(entriesByKey[baseKey]) {
                "Terrain variant family '$baseKey' references a missing base visual key."
            }
            val maxIndex = variantsByIndex.keys.maxOrNull() ?: 0
            val entries =
                (0..maxIndex).map { index ->
                    requireNotNull(variantsByIndex[index]) {
                        "Terrain variant family '$baseKey' is missing contiguous index $index."
                    }
                }
            require(entries.first().key == baseKey) {
                "Terrain variant family '$baseKey' must declare the base key at index 0."
            }
            require(entries.all { entry -> entry.category == base.category }) {
                "Terrain variant family '$baseKey' must keep category '${base.category}' for every entry."
            }
            entries
        }
    }

    private fun buildTerrainWallFamilies(): Map<String, Map<TerrainWallPieceRole, VisualManifestEntry>> {
        val byFamily = linkedMapOf<String, MutableMap<TerrainWallPieceRole, VisualManifestEntry>>()
        manifest.entries.forEach { entry ->
            val familyTags = entry.tags.mapNotNull { tag -> tag.removePrefixOrNull(TERRAIN_WALL_FAMILY_TAG) }
            val pieceTags = entry.tags.mapNotNull { tag -> tag.removePrefixOrNull(TERRAIN_WALL_PIECE_TAG) }
            if (familyTags.isEmpty() && pieceTags.isEmpty()) {
                return@forEach
            }
            require(familyTags.size == 1 && pieceTags.size == 1) {
                "Terrain wall entry '${entry.key}' must declare exactly one wall family tag and one wall piece tag."
            }
            val baseKey = familyTags.single()
            val role =
                TerrainWallPieceRole.entries.singleOrNull { candidate -> candidate.tagValue == pieceTags.single() }
                    ?: error("Terrain wall entry '${entry.key}' has unknown wall piece tag '${pieceTags.single()}'.")
            val pieces = byFamily.getOrPut(baseKey) { linkedMapOf() }
            require(role !in pieces) {
                "Duplicate terrain wall piece '${role.tagValue}' for family '$baseKey'."
            }
            pieces[role] = entry
        }
        return byFamily.mapValues { (baseKey, piecesByRole) ->
            val base = requireNotNull(entriesByKey[baseKey]) {
                "Terrain wall family '$baseKey' references a missing base visual key."
            }
            TerrainWallPieceRole.entries.forEach { role ->
                require(role in piecesByRole) {
                    "Terrain wall family '$baseKey' is missing role '${role.tagValue}'."
                }
            }
            require(piecesByRole[TerrainWallPieceRole.BASE]?.key == baseKey) {
                "Terrain wall family '$baseKey' must declare the base key as the base role."
            }
            require(piecesByRole.values.all { entry -> entry.category == base.category }) {
                "Terrain wall family '$baseKey' must keep category '${base.category}' for every entry."
            }
            piecesByRole
        }
    }

    private fun String.removePrefixOrNull(prefix: String): String? =
        if (startsWith(prefix)) {
            removePrefix(prefix).takeIf(String::isNotBlank)
        } else {
            null
        }

    private companion object {
        const val TERRAIN_VARIANT_FAMILY_TAG = "terrain_variant_family:"
        const val TERRAIN_VARIANT_INDEX_TAG = "terrain_variant_index:"
        const val TERRAIN_WALL_FAMILY_TAG = "terrain_wall_family:"
        const val TERRAIN_WALL_PIECE_TAG = "terrain_wall_piece:"
    }
}

enum class TerrainWallPieceRole(val tagValue: String) {
    BASE("base"),
    CROWN("crown"),
    SIDE("side"),
    CORNER("corner"),
    DOOR_CONTACT("door_contact"),
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
