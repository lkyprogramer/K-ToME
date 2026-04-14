package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

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
}
