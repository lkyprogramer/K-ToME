package com.ktome.game.validation

import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.SaveManager
import com.ktome.core.save.SaveSnapshot
import com.ktome.game.FoundationGameConfig
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.elites.BossVariantSelectionMode
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.writeText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ValidationSessionMetadataStore(
    private val saveDir: Path,
    private val json: Json =
        Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = false
        },
) {
    private val metadataFile: Path = saveDir.resolve(DEFAULT_FILE_NAME)
    private val codec = ValidationSessionMetadataCodec(json)

    fun save(options: ValidationSessionOptions): Boolean =
        runCatching {
            Files.createDirectories(saveDir)
            val temporaryFile = Files.createTempFile(saveDir, "${DEFAULT_FILE_NAME}.", ".tmp")
            try {
                temporaryFile.writeText(codec.encode(options.toMetadataSnapshot()))
                try {
                    Files.move(temporaryFile, metadataFile, ATOMIC_MOVE, REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporaryFile, metadataFile, REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporaryFile)
            }
        }.isSuccess

    fun load(): ValidationSessionMetadataSnapshot? {
        if (!Files.isRegularFile(metadataFile)) {
            return null
        }
        val payload = Files.readString(metadataFile)
        return try {
            codec.decode(payload)
        } catch (exception: SerializationException) {
            throw InvalidSaveException("Validation metadata file is not valid JSON.", exception)
        } catch (exception: IllegalArgumentException) {
            throw InvalidSaveException("Validation metadata file failed validation: ${exception.message}", exception)
        }
    }

    fun metadataPath(): Path = metadataFile

    companion object {
        const val DEFAULT_FILE_NAME: String = "validation-session.json"
    }
}

fun persistValidationSessionMetadata(
    saveManager: SaveManager,
    options: ValidationSessionOptions,
): Boolean =
    ValidationSessionMetadataStore(saveManager.savePath().parent).save(options)

fun loadPersistedValidationSessionOptions(saveManager: SaveManager): ValidationSessionOptions? {
    val snapshot = saveManager.load() ?: return null
    val store = ValidationSessionMetadataStore(saveManager.savePath().parent)
    val metadata =
        store.load()
            ?: throw InvalidSaveException(
                "Validation metadata file is missing: ${store.metadataPath().fileName}.",
            )
    return try {
        metadata.toValidationSessionOptions(snapshot)
    } catch (exception: IllegalArgumentException) {
        throw InvalidSaveException("Validation metadata file failed validation: ${exception.message}", exception)
    }
}

data class ValidationSessionMetadataSnapshot(
    val metadataVersion: Int = CURRENT_METADATA_VERSION,
    val presetId: String,
    val seedCorpus: List<Long>,
    val contentPackRoots: List<String> = emptyList(),
    val capabilities: ValidationCapabilitySnapshot = ValidationCapabilitySnapshot(),
    val profileRunPersistenceModeId: String,
    val bossVariantSelectionModeId: String,
    val preferredBossVariantId: String? = null,
) {
    init {
        require(metadataVersion == CURRENT_METADATA_VERSION) {
            "Unsupported validation metadata version $metadataVersion."
        }
        require(presetId.isNotBlank()) { "Validation metadata presetId must not be blank." }
        require(seedCorpus.isNotEmpty()) { "Validation metadata seedCorpus must not be empty." }
        require(contentPackRoots.distinct().size == contentPackRoots.size) {
            "Validation metadata contentPackRoots must not contain duplicates."
        }
        require(profileRunPersistenceModeId.isNotBlank()) {
            "Validation metadata profileRunPersistenceModeId must not be blank."
        }
        require(bossVariantSelectionModeId.isNotBlank()) {
            "Validation metadata bossVariantSelectionModeId must not be blank."
        }
    }

    fun toValidationSessionOptions(snapshot: SaveSnapshot): ValidationSessionOptions {
        val bossVariantSelectionMode = enumValueOf<BossVariantSelectionMode>(bossVariantSelectionModeId)
        val foundationConfig =
            FoundationGameConfig(
                width = snapshot.mapWidth,
                height = snapshot.mapHeight,
                seed = snapshot.worldSeed,
                fovRadius = snapshot.fovRadius,
                floor = snapshot.floorIndex,
                maxFloor = snapshot.maxFloor,
                messageLogSize = snapshot.messageLogSize,
                zoneId = snapshot.currentZoneId,
                playerProfessionId = snapshot.playerProfessionId,
                playerRaceId = snapshot.playerRaceId,
                zoneRoute = snapshot.zoneRoute,
                routeIndex = snapshot.routeIndex,
                bossVariantSelectionMode = bossVariantSelectionMode,
                preferredBossVariantId = preferredBossVariantId,
            )
        return ValidationSessionOptions(
            preset = enumValueOf<ValidationPreset>(presetId),
            foundationConfig = foundationConfig,
            seedCorpus = seedCorpus,
            contentPackSelection =
                if (contentPackRoots.isEmpty()) {
                    ContentPackSelection.EMPTY
                } else {
                    ContentPackSelection.of(*contentPackRoots.map(Path::of).toTypedArray())
                },
            capabilities = capabilities.toValidationCapabilitySet(),
            profileRunPersistenceMode = enumValueOf<ProfileRunPersistenceMode>(profileRunPersistenceModeId),
        )
    }

    companion object {
        private const val CURRENT_METADATA_VERSION: Int = 1
    }
}

data class ValidationCapabilitySnapshot(
    val restart: Boolean = true,
    val travel: Boolean = true,
    val recovery: Boolean = true,
    val encounter: Boolean = true,
    val terrain: Boolean = true,
    val rewardAndItem: Boolean = true,
    val discovery: Boolean = true,
    val phase4V4Fast: Boolean = true,
) {
    fun toValidationCapabilitySet(): ValidationCapabilitySet =
        ValidationCapabilitySet(
            restart = restart,
            travel = travel,
            recovery = recovery,
            encounter = encounter,
            terrain = terrain,
            rewardAndItem = rewardAndItem,
            discovery = discovery,
            phase4V4Fast = phase4V4Fast,
        )
}

private class ValidationSessionMetadataCodec(
    private val json: Json,
) {
    fun encode(snapshot: ValidationSessionMetadataSnapshot): String = snapshot.toJson().toString()

    fun decode(payload: String): ValidationSessionMetadataSnapshot {
        val rootElement =
            try {
                json.parseToJsonElement(payload)
            } catch (exception: SerializationException) {
                throw SerializationException("Validation metadata file is not valid JSON.", exception)
            }
        val root = rootElement as? JsonObject
            ?: throw IllegalArgumentException("Validation metadata file root must be a JSON object.")
        return ValidationSessionMetadataSnapshot(
            metadataVersion = root.requireInt("metadataVersion"),
            presetId = root.requireString("presetId"),
            seedCorpus = root.requireLongArray("seedCorpus"),
            contentPackRoots = root.requireStringArray("contentPackRoots"),
            capabilities =
                root.requireObject("capabilities").let { capabilities ->
                    ValidationCapabilitySnapshot(
                        restart = capabilities.requireBoolean("restart"),
                        travel = capabilities.requireBoolean("travel"),
                        recovery = capabilities.requireBoolean("recovery"),
                        encounter = capabilities.requireBoolean("encounter"),
                        terrain = capabilities.requireBoolean("terrain"),
                        rewardAndItem = capabilities.requireBoolean("rewardAndItem"),
                        discovery = capabilities.requireBoolean("discovery"),
                        phase4V4Fast = capabilities.optionalBoolean("phase4V4Fast", defaultValue = true),
                    )
                },
            profileRunPersistenceModeId = root.requireString("profileRunPersistenceModeId"),
            bossVariantSelectionModeId = root.requireString("bossVariantSelectionModeId"),
            preferredBossVariantId = root["preferredBossVariantId"]?.jsonPrimitive?.contentOrNull,
        )
    }
}

private fun ValidationSessionOptions.toMetadataSnapshot(): ValidationSessionMetadataSnapshot =
    ValidationSessionMetadataSnapshot(
        presetId = preset.name,
        seedCorpus = seedCorpus,
        contentPackRoots = contentPackSelection.activePackRoots.map(Path::toString),
        capabilities =
            ValidationCapabilitySnapshot(
                restart = capabilities.restart,
                travel = capabilities.travel,
                recovery = capabilities.recovery,
                encounter = capabilities.encounter,
                terrain = capabilities.terrain,
                rewardAndItem = capabilities.rewardAndItem,
                discovery = capabilities.discovery,
                phase4V4Fast = capabilities.phase4V4Fast,
            ),
        profileRunPersistenceModeId = profileRunPersistenceMode.name,
        bossVariantSelectionModeId = foundationConfig.bossVariantSelectionMode.name,
        preferredBossVariantId = foundationConfig.preferredBossVariantId,
    )

private fun ValidationSessionMetadataSnapshot.toJson(): JsonObject =
    JsonObject(
        mapOf(
            "metadataVersion" to JsonPrimitive(metadataVersion),
            "presetId" to JsonPrimitive(presetId),
            "seedCorpus" to JsonArray(seedCorpus.map(::JsonPrimitive)),
            "contentPackRoots" to JsonArray(contentPackRoots.map(::JsonPrimitive)),
            "capabilities" to capabilities.toJson(),
            "profileRunPersistenceModeId" to JsonPrimitive(profileRunPersistenceModeId),
            "bossVariantSelectionModeId" to JsonPrimitive(bossVariantSelectionModeId),
            "preferredBossVariantId" to JsonPrimitive(preferredBossVariantId),
        ),
    )

private fun ValidationCapabilitySnapshot.toJson(): JsonObject =
    JsonObject(
        mapOf(
            "restart" to JsonPrimitive(restart),
            "travel" to JsonPrimitive(travel),
            "recovery" to JsonPrimitive(recovery),
            "encounter" to JsonPrimitive(encounter),
            "terrain" to JsonPrimitive(terrain),
            "rewardAndItem" to JsonPrimitive(rewardAndItem),
            "discovery" to JsonPrimitive(discovery),
            "phase4V4Fast" to JsonPrimitive(phase4V4Fast),
        ),
    )

private fun JsonObject.requireObject(fieldName: String): JsonObject =
    this[fieldName] as? JsonObject
        ?: throw IllegalArgumentException("Validation metadata field '$fieldName' must be a JSON object.")

private fun JsonObject.requireString(fieldName: String): String =
    this[fieldName]?.jsonPrimitive?.contentOrNull
        ?: throw IllegalArgumentException("Validation metadata field '$fieldName' must be a string.")

private fun JsonObject.requireInt(fieldName: String): Int =
    this[fieldName]?.jsonPrimitive?.intOrNull
        ?: throw IllegalArgumentException("Validation metadata field '$fieldName' must be an integer.")

private fun JsonObject.requireBoolean(fieldName: String): Boolean =
    this[fieldName]?.jsonPrimitive?.booleanOrNull
        ?: throw IllegalArgumentException("Validation metadata field '$fieldName' must be a boolean.")

private fun JsonObject.optionalBoolean(
    fieldName: String,
    defaultValue: Boolean,
): Boolean {
    val element = this[fieldName] ?: return defaultValue
    return (element as? JsonPrimitive)?.booleanOrNull
        ?: throw IllegalArgumentException("Validation metadata field '$fieldName' must be a boolean.")
}

private fun JsonObject.requireLongArray(fieldName: String): List<Long> =
    (this[fieldName] as? JsonArray)?.mapIndexed { index, element ->
        element.jsonPrimitive.longOrNull
            ?: throw IllegalArgumentException(
                "Validation metadata field '$fieldName' must contain only integer values; index $index was invalid.",
            )
    } ?: throw IllegalArgumentException("Validation metadata field '$fieldName' must be a JSON array.")

private fun JsonObject.requireStringArray(fieldName: String): List<String> =
    (this[fieldName] as? JsonArray)?.mapIndexed { index, element ->
        element.jsonPrimitive.contentOrNull
            ?: throw IllegalArgumentException(
                "Validation metadata field '$fieldName' must contain only string values; index $index was invalid.",
            )
    } ?: throw IllegalArgumentException("Validation metadata field '$fieldName' must be a JSON array.")
