package com.ktome.game.contentpack

import com.ktome.core.phase.PackId
import java.nio.file.Files
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml

object ContentPackFixtureCatalog {
    val samplePackId: PackId = PackId("sample.flooded_relics")
    val samplePrecedenceFixturePackId: PackId = PackId("fixture_sample_flooded_relics_override")
    val sampleBiasSplitFixturePackId: PackId = PackId("fixture.sample_flooded_relics_bias_split")
    val addPackId: PackId = PackId("fixture.add_monster")
    val replacePackId: PackId = PackId("fixture.replace_monster")
    val appendPackId: PackId = PackId("fixture.append_loot_pool")
    val legacyV2LootProfilePackId: PackId = PackId("fixture.legacy_v2_loot_profile")
    val denyPackId: PackId = PackId("fixture.deny_hidden_event")
    val missingDependencyPackId: PackId = PackId("fixture.missing_dependency")
    val cyclePackAId: PackId = PackId("fixture.cycle_a")
    val cyclePackBId: PackId = PackId("fixture.cycle_b")
    val versionConflictPackId: PackId = PackId("fixture.version_conflict")
    val namespaceCollisionLeftId: PackId = PackId("fixture.namespace-collision")
    val namespaceCollisionRightId: PackId = PackId("fixture.namespace_collision")
    val samePriorityLeftId: PackId = PackId("fixture.same_priority_left")
    val samePriorityRightId: PackId = PackId("fixture.same_priority_right")
    val duplicateWithoutReplacePackId: PackId = PackId("fixture.duplicate_base_monster")

    val allPackIds: List<PackId> =
        listOf(
            addPackId,
            samplePrecedenceFixturePackId,
            sampleBiasSplitFixturePackId,
            replacePackId,
            appendPackId,
            legacyV2LootProfilePackId,
            denyPackId,
            missingDependencyPackId,
            cyclePackAId,
            cyclePackBId,
            versionConflictPackId,
            namespaceCollisionLeftId,
            namespaceCollisionRightId,
            samePriorityLeftId,
            samePriorityRightId,
            duplicateWithoutReplacePackId,
        )

    fun samplePackRoot(): Path =
        repoRoot()
            .resolve("examples/content-packs")
            .resolve(samplePackId.value)

    fun fixturePackRoot(packId: PackId): Path =
        repoRoot()
            .resolve("tools/src/main/resources/fixtures/content-packs/packs")
            .resolve(packId.value)

    fun harnessSpec(packId: PackId): ContentPackHarnessSpec {
        val path = sidecarPath(packId)
        if (!Files.exists(path)) {
            val repoRoot = repoRoot()
            error("Missing content-pack harness sidecar for packId=${packId.value}: ${repoRoot.relativize(path)}")
        }
        val root = Files.newBufferedReader(path).use { reader -> Yaml().load<Map<String, Any?>>(reader) }
            ?: error("Harness spec root must not be null: $path")
        return ContentPackHarnessSpec(
            packId = PackId(root.requiredString("packId")),
            harnessSeeds = root.requiredLongList("harnessSeeds"),
            generatedTemplateSeeds = root.optionalLongList("generatedTemplateSeeds").ifEmpty { root.requiredLongList("harnessSeeds") },
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

    fun selection(
        activePackRoots: List<Path>,
        availablePackRoots: List<Path> = activePackRoots,
    ): ContentPackSelection =
        ContentPackSelection(
            activePackRoots = activePackRoots,
            availablePackRoots = availablePackRoots,
        )

    fun availableSelection(activePackIds: Iterable<PackId>): ContentPackSelection =
        selection(
            activePackRoots = activePackIds.map(::fixturePackRoot),
            availablePackRoots = allPackIds.map(::fixturePackRoot),
        )

    private fun sidecarPath(packId: PackId): Path =
        repoRoot()
            .resolve("tools/src/main/resources/fixtures/content-packs")
            .resolve("${packId.value}.yaml")
}

fun repoRoot(): Path =
    Path.of(
        requireNotNull(System.getProperty("ktome.repo.root")) {
            "ktome.repo.root system property is required for content-pack fixtures."
        },
    )

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

private fun Map<*, *>.optionalLongList(key: String): List<Long> =
    (this[key] as? List<*>)?.map { raw ->
        when (raw) {
            is Long -> raw
            is Int -> raw.toLong()
            is Number -> raw.toLong()
            is String -> raw.toLong()
            else -> error("Entry '$key' must contain integer-like values.")
        }
    } ?: emptyList()

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
