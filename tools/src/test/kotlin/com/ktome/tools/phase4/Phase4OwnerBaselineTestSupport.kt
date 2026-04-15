package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object Phase4OwnerBaselineTestSupport {
    val prettyJson: Json = Json { prettyPrint = true; explicitNulls = false }

    fun stampBaselineMetadata(
        path: Path,
        marker: String,
    ) {
        val payload = Json.parseToJsonElement(Files.readString(path)).jsonObject
        val updated =
            buildJsonObject {
                payload.forEach { (key, value) ->
                    if (key != "metadata") {
                        put(key, value)
                    }
                }
                put(
                    "metadata",
                    buildJsonObject {
                        payload["metadata"]?.jsonObject?.forEach { (key, value) -> put(key, value) }
                        put("testMarker", JsonPrimitive(marker))
                    },
                )
            }
        Files.writeString(path, prettyJson.encodeToString(JsonElement.serializer(), updated))
    }

    fun overrideStrictPairCeiling(
        path: Path,
        secretProfileId: String,
        maxValue: Double,
    ) {
        val payload = Json.parseToJsonElement(Files.readString(path)).jsonObject
        val strictPairCeilings = payload.getValue("metadata").jsonObject.getValue("strictPairCeilings").jsonArray
        val updated =
            buildJsonObject {
                payload.forEach { (key, value) ->
                    if (key != "metadata") {
                        put(key, value)
                    }
                }
                put(
                    "metadata",
                    buildJsonObject {
                        payload.getValue("metadata").jsonObject.forEach { (key, value) ->
                            if (key != "strictPairCeilings") {
                                put(key, value)
                            }
                        }
                        put(
                            "strictPairCeilings",
                            buildJsonArray {
                                strictPairCeilings.forEach { entry ->
                                    val entryObject = entry.jsonObject
                                    add(
                                        buildJsonObject {
                                            entryObject.forEach { (key, value) ->
                                                put(
                                                    key,
                                                    if (key == "maxValue" && entryObject.getValue("secretProfileId").jsonPrimitive.content == secretProfileId) {
                                                        JsonPrimitive(maxValue)
                                                    } else {
                                                        value
                                                    },
                                                )
                                            }
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            }
        Files.writeString(path, prettyJson.encodeToString(JsonElement.serializer(), updated))
    }
}
