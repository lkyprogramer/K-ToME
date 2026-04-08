package com.ktome.tools.contentpack

import com.ktome.core.phase.PackId
import com.ktome.game.contentpack.ContentPackHarnessSpec
import com.ktome.game.contentpack.DualPackScenario
import com.ktome.game.contentpack.OverlayOp
import com.ktome.game.contentpack.repoRoot
import java.nio.file.Files
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml

internal object ContentPackSidecarCatalog {
    fun sidecarPath(packId: PackId): Path =
        repoRoot()
            .resolve("tools/src/main/resources/fixtures/content-packs")
            .resolve("${packId.value}.yaml")

    fun loadHarnessSpec(packId: PackId): ContentPackHarnessSpec {
        val path = sidecarPath(packId)
        val root = Files.newBufferedReader(path).use { reader -> Yaml().load<Map<String, Any?>>(reader) }
            ?: error("Harness spec root must not be null: $path")
        return ContentPackHarnessSpec(
            packId = PackId(root.requiredString("packId")),
            harnessSeeds = root.requiredLongList("harnessSeeds"),
            dualPackScenarios =
                root.optionalList("dualPackScenarios").map { raw ->
                    val scenario = raw.requiredMap()
                    DualPackScenario(
                        fixturePackId = PackId(scenario.requiredString("fixturePackId")),
                        expectedOrder = scenario.requiredStringList("expectedOrder").map(::PackId),
                        expectedOps = scenario.optionalStringList("expectedOps").map { op -> OverlayOp.valueOf(op.uppercase()) },
                    )
                },
            fixtureOrder = root.optionalStringList("fixtureOrder"),
            overlayContractVersion = root.requiredInt("overlayContractVersion"),
        )
    }
}

private fun Map<*, *>.requiredString(key: String): String =
    this[key]?.toString()?.trim()?.takeIf(String::isNotBlank) ?: error("Missing string entry '$key'.")

private fun Map<*, *>.requiredInt(key: String): Int =
    when (val value = this[key]) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toInt()
        else -> error("Missing int entry '$key'.")
    }

private fun Map<*, *>.requiredLongList(key: String): List<Long> =
    (this[key] as? List<*>)?.map { raw ->
        when (raw) {
            is Long -> raw
            is Int -> raw.toLong()
            is Number -> raw.toLong()
            is String -> raw.toLong()
            else -> error("Entry '$key' must contain integer-like values.")
        }
    } ?: error("Missing list entry '$key'.")

private fun Map<*, *>.requiredStringList(key: String): List<String> =
    (this[key] as? List<*>)?.map { raw ->
        raw?.toString()?.trim()?.takeIf(String::isNotBlank) ?: error("Entry '$key' must not contain blank strings.")
    } ?: error("Missing list entry '$key'.")

private fun Map<*, *>.optionalStringList(key: String): List<String> =
    (this[key] as? List<*>)?.map { raw ->
        raw?.toString()?.trim()?.takeIf(String::isNotBlank) ?: error("Entry '$key' must not contain blank strings.")
    } ?: emptyList()

private fun Map<*, *>.optionalList(key: String): List<Any?> =
    (this[key] as? List<Any?>).orEmpty()

private fun Any?.requiredMap(): Map<*, *> =
    this as? Map<*, *> ?: error("Entry must be a map.")
