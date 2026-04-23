package com.ktome.tools.lint

import com.ktome.client.ui.combat.CombatAffordanceResourceKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

data class CombatAffordanceResourceAuditFinding(
    val key: String,
    val surface: String,
)

data class CombatAffordanceResourceAuditRequest(
    val visualManifestPath: Path,
    val audioManifestPath: Path,
    val runtimeSourceRoots: List<Path>,
    val verificationSourceRoots: List<Path>,
)

object CombatAffordanceResourceAuditRule {
    private data class RequiredKey(
        val key: String,
        val symbol: String,
    )

    private val requiredVisualKeySpecs: List<RequiredKey> =
        listOf(
            RequiredKey(CombatAffordanceResourceKeys.ACTION_ICON, "ACTION_ICON"),
            RequiredKey(CombatAffordanceResourceKeys.METHOD_ICON, "METHOD_ICON"),
            RequiredKey(CombatAffordanceResourceKeys.TARGET_ICON, "TARGET_ICON"),
            RequiredKey(CombatAffordanceResourceKeys.LOCK_ICON, "LOCK_ICON"),
            RequiredKey(CombatAffordanceResourceKeys.INVALID_ICON, "INVALID_ICON"),
        )

    private val requiredAudioKeySpecs: List<RequiredKey> =
        listOf(
            RequiredKey(CombatAffordanceResourceKeys.ACTION_CONFIRM_AUDIO, "ACTION_CONFIRM_AUDIO"),
            RequiredKey(CombatAffordanceResourceKeys.METHOD_CONFIRM_AUDIO, "METHOD_CONFIRM_AUDIO"),
            RequiredKey(CombatAffordanceResourceKeys.TARGET_CONFIRM_AUDIO, "TARGET_CONFIRM_AUDIO"),
            RequiredKey(CombatAffordanceResourceKeys.TARGET_LOCK_AUDIO, "TARGET_LOCK_AUDIO"),
            RequiredKey(CombatAffordanceResourceKeys.INVALID_SUBMIT_AUDIO, "INVALID_SUBMIT_AUDIO"),
        )

    val requiredVisualKeys: Set<String> = requiredVisualKeySpecs.mapTo(linkedSetOf(), RequiredKey::key)
    val requiredAudioKeys: Set<String> = requiredAudioKeySpecs.mapTo(linkedSetOf(), RequiredKey::key)

    fun validate(request: CombatAffordanceResourceAuditRequest): List<CombatAffordanceResourceAuditFinding> {
        val visualKeys = manifestKeys(request.visualManifestPath)
        val audioKeys = manifestKeys(request.audioManifestPath)
        val runtimeText = sourceText(request.runtimeSourceRoots, includeKeyDefinitions = false)
        val verificationText = sourceText(request.verificationSourceRoots, includeKeyDefinitions = true)
        return buildList {
            requiredVisualKeySpecs.filterNot { spec -> spec.key in visualKeys }.forEach { spec ->
                add(CombatAffordanceResourceAuditFinding(spec.key, "visual-manifest"))
            }
            requiredAudioKeySpecs.filterNot { spec -> spec.key in audioKeys }.forEach { spec ->
                add(CombatAffordanceResourceAuditFinding(spec.key, "audio-manifest"))
            }
            (requiredVisualKeySpecs + requiredAudioKeySpecs).filterNot { spec -> spec.isConsumedBy(runtimeText) }.forEach { spec ->
                add(CombatAffordanceResourceAuditFinding(spec.key, "runtime-consumer"))
            }
            (requiredVisualKeySpecs + requiredAudioKeySpecs).filterNot { spec -> spec.isConsumedBy(verificationText) }.forEach { spec ->
                add(CombatAffordanceResourceAuditFinding(spec.key, "verification-consumer"))
            }
        }
    }

    private fun RequiredKey.isConsumedBy(sourceText: String): Boolean = sourceText.contains(key) || sourceText.contains(symbol)

    private fun sourceText(
        roots: List<Path>,
        includeKeyDefinitions: Boolean,
    ): String =
        roots
            .flatMap { root ->
                Files.walk(root).use { paths ->
                    paths
                        .filter { path -> path.toString().endsWith(".kt") }
                        .filter { path -> includeKeyDefinitions || path.fileName.toString() != "CombatAffordanceResourceKeys.kt" }
                        .toList()
                }
            }.joinToString(separator = "\n") { path -> path.readText() }

    private fun manifestKeys(path: Path): Set<String> {
        val root = Json.parseToJsonElement(path.readText()).jsonObject
        return root.getValue("entries").jsonArray.mapTo(linkedSetOf()) { entry ->
            entry.jsonObject.getValue("key").jsonPrimitive.content
        }
    }
}
