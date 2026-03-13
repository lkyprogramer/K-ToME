package com.ktome.core.save

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            }
        if (!contract.isSupported()) {
            throw UnsupportedSaveContractVersionException(found = contract)
        }

        val snapshot =
            try {
                json.decodeFromJsonElement<SaveSnapshot>(root)
            } catch (exception: SerializationException) {
                throw InvalidSaveException("Save file does not match the current Phase 2 save schema.", exception)
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
}
