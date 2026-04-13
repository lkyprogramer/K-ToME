package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private val phase4ArtifactJson: Json = Json { prettyPrint = true }

internal fun readPhase4Json(path: Path): JsonObject {
    check(Files.exists(path)) { "Missing phase4 report source: $path" }
    return phase4ArtifactJson.parseToJsonElement(Files.readString(path)).jsonObject
}
