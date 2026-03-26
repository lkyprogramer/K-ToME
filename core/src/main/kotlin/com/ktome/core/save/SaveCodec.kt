package com.ktome.core.save

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

sealed class SaveLoadException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

class LegacySaveFormatException :
    SaveLoadException("Legacy saves from before the Phase 2 save contract are not supported. Start a new run.")

class UnsupportedSaveContractVersionException(
    found: SaveContractVersion,
    expected: SaveContractVersion = SaveContractVersion.CURRENT,
) : SaveLoadException("Unsupported save contract version $found. Current supported version is $expected.")

class MalformedSaveException(
    message: String,
    cause: Throwable? = null,
) : SaveLoadException(message, cause)

class InvalidSaveException(
    message: String,
    cause: Throwable? = null,
) : SaveLoadException(message, cause)

class SaveRestoreException(
    message: String,
    cause: Throwable? = null,
) : SaveLoadException(message, cause)

@OptIn(ExperimentalSerializationApi::class)
class SaveCodec(
    private val json: Json =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = false
        },
) {
    fun encode(snapshot: SaveSnapshot): String {
        snapshot.validateOrThrow()
        return json.encodeToString(snapshot)
    }

    fun decode(raw: String): SaveSnapshot {
        val rootElement =
            try {
                json.parseToJsonElement(raw)
            } catch (exception: SerializationException) {
                throw MalformedSaveException("Save file is not valid JSON.", exception)
            }

        val root = rootElement as? JsonObject ?: throw MalformedSaveException("Save file root must be a JSON object.")
        val contractElement = root["saveContractVersion"] ?: throw LegacySaveFormatException()
        val contract =
            try {
                json.decodeFromJsonElement<SaveContractVersion>(contractElement)
            } catch (exception: SerializationException) {
                throw InvalidSaveException("Save file is missing a valid saveContractVersion.", exception)
            } catch (exception: IllegalArgumentException) {
                throw InvalidSaveException("Save file is missing a valid saveContractVersion.", exception)
            }
        if (!contract.isSupported()) {
            throw UnsupportedSaveContractVersionException(found = contract)
        }
        requireFields(root, REQUIRED_TOP_LEVEL_FIELDS, context = "Save file")
        val worldProgressElement = root.getValue("worldProgress")
        val worldProgress =
            worldProgressElement as? JsonObject
                ?: throw InvalidSaveException("Save file field 'worldProgress' must be a JSON object.")
        requireFields(worldProgress, REQUIRED_WORLD_PROGRESS_FIELDS, context = "Save file worldProgress")
        requireArray(root["shopStates"], fieldName = "shopStates")
        requireArray(root["zoneRoute"], fieldName = "zoneRoute")
        requireArray(root["floors"], fieldName = "floors")

        val snapshot =
            try {
                json.decodeFromJsonElement<SaveSnapshot>(root)
            } catch (exception: SerializationException) {
                throw InvalidSaveException("Save file does not match the current save schema.", exception)
            } catch (exception: IllegalArgumentException) {
                throw InvalidSaveException("Save file failed validation: ${exception.message}", exception)
            }

        try {
            snapshot.validateOrThrow()
        } catch (exception: IllegalArgumentException) {
            throw InvalidSaveException("Save file failed validation: ${exception.message}", exception)
        }
        return snapshot
    }

    private fun requireFields(
        root: JsonObject,
        requiredFields: Set<String>,
        context: String,
    ) {
        val missing = requiredFields.filterNot(root::containsKey)
        if (missing.isNotEmpty()) {
            throw InvalidSaveException("$context is missing required fields: ${missing.joinToString()}")
        }
    }

    private fun requireArray(
        element: JsonElement?,
        fieldName: String,
    ) {
        if (element !is JsonArray) {
            throw InvalidSaveException("Save file field '$fieldName' must be a JSON array.")
        }
    }

    private companion object {
        private val REQUIRED_TOP_LEVEL_FIELDS =
            setOf(
                "schemaVersion",
                "saveContractVersion",
                "buildMetadata",
                "timestampEpochMillis",
                "worldSeed",
                "currentZoneId",
                "zoneRoute",
                "routeIndex",
                "worldProgress",
                "shardBalance",
                "shopStates",
                "floorIndex",
                "mapWidth",
                "mapHeight",
                "fovRadius",
                "messageLogSize",
                "playerProfessionId",
                "playerRaceId",
                "maxFloor",
                "turnCount",
                "headlessTurnEquivalent",
                "player",
                "floors",
            )
        private val REQUIRED_WORLD_PROGRESS_FIELDS =
            setOf(
                "questStates",
                "worldFlags",
                "unlockedRoutes",
                "defeatedBossIds",
                "claimedRouteRewards",
            )
    }
}
