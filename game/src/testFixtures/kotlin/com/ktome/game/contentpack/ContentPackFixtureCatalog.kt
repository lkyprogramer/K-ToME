package com.ktome.game.contentpack

import com.ktome.core.phase.PackId
import java.nio.file.Path

object ContentPackFixtureCatalog {
    val samplePackId: PackId = PackId("sample.flooded_relics")
    val samplePrecedenceFixturePackId: PackId = PackId("fixture.sample_flooded_relics_override")
    val addPackId: PackId = PackId("fixture.add_monster")
    val replacePackId: PackId = PackId("fixture.replace_monster")
    val appendPackId: PackId = PackId("fixture.append_loot_pool")
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
            replacePackId,
            appendPackId,
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
}

fun repoRoot(): Path =
    Path.of(
        requireNotNull(System.getProperty("ktome.repo.root")) {
            "ktome.repo.root system property is required for content-pack fixtures."
        },
    )
