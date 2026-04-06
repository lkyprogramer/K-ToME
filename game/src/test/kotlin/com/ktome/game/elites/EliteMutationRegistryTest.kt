package com.ktome.game.elites

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EliteMutationRegistryTest {
    @Test
    fun `signature tier stays unavailable before late floors even when content exists`() {
        val registry = testRegistry()
        val counts =
            sampleTierCounts(
                registry = registry,
                context =
                    MutationSelectionContext(
                        zoneId = "abyssal_temple",
                        floorIndex = 3,
                        applyToTags = setOf("elite"),
                    ),
            )

        assertEquals(0, counts.getValue(MutationTier.SIGNATURE))
    }

    @Test
    fun `high pressure late zones increase major and signature selection share`() {
        val registry = testRegistry()
        val baseline =
            sampleTierCounts(
                registry = registry,
                context =
                    MutationSelectionContext(
                        zoneId = "deep_iron_pit",
                        floorIndex = 4,
                        applyToTags = setOf("elite"),
                    ),
            )
        val highPressure =
            sampleTierCounts(
                registry = registry,
                context =
                    MutationSelectionContext(
                        zoneId = "abyssal_temple",
                        floorIndex = 4,
                        applyToTags = setOf("elite"),
                    ),
            )

        assertTrue(highPressure.getValue(MutationTier.SIGNATURE) > baseline.getValue(MutationTier.SIGNATURE))
        assertTrue(highPressure.getValue(MutationTier.MAJOR) > baseline.getValue(MutationTier.MAJOR))
        assertTrue(highPressure.getValue(MutationTier.MINOR) < baseline.getValue(MutationTier.MINOR))
    }

    private fun sampleTierCounts(
        registry: EliteMutationRegistry,
        context: MutationSelectionContext,
        iterations: Int = 240,
    ): Map<MutationTier, Int> {
        var cursor = 0
        val counts = mutableMapOf<MutationTier, Int>().withDefault { 0 }
        repeat(iterations) {
            val selected =
                registry.select(context) { bound ->
                    val roll = cursor % bound
                    cursor += 1
                    roll
                }
            val tier = selected.single().tier
            counts[tier] = counts.getValue(tier) + 1
        }
        return counts
    }

    private fun testRegistry(): EliteMutationRegistry =
        EliteMutationRegistry(
            config = EliteMutationConfig(),
            statModifiersById = emptyMap(),
            definitionsById =
                listOf(
                    EliteMutationDef(
                        id = "elite.minor.test",
                        kind = MutationKind.STAT_PACKAGE,
                        tier = MutationTier.MINOR,
                        threatCost = 1,
                        nameKey = "mutation.test.minor",
                        iconKey = "icon.mutation.test.minor",
                        applyToTags = setOf("elite"),
                        minFloor = 1,
                        maxFloor = null,
                        allowedZones = emptySet(),
                        statModifiers = emptyList(),
                        grantedTalents = emptyList(),
                        aiProfileOverlay = null,
                        incompatibleWith = emptySet(),
                    ),
                    EliteMutationDef(
                        id = "elite.major.test",
                        kind = MutationKind.ABILITY_GRANT,
                        tier = MutationTier.MAJOR,
                        threatCost = 2,
                        nameKey = "mutation.test.major",
                        iconKey = "icon.mutation.test.major",
                        applyToTags = setOf("elite"),
                        minFloor = 1,
                        maxFloor = null,
                        allowedZones = emptySet(),
                        statModifiers = emptyList(),
                        grantedTalents = emptyList(),
                        aiProfileOverlay = null,
                        incompatibleWith = emptySet(),
                    ),
                    EliteMutationDef(
                        id = "elite.signature.test",
                        kind = MutationKind.ELEMENT_PACKAGE,
                        tier = MutationTier.SIGNATURE,
                        threatCost = 3,
                        nameKey = "mutation.test.signature",
                        iconKey = "icon.mutation.test.signature",
                        applyToTags = setOf("elite"),
                        minFloor = 1,
                        maxFloor = null,
                        allowedZones = emptySet(),
                        statModifiers = emptyList(),
                        grantedTalents = emptyList(),
                        aiProfileOverlay = null,
                        incompatibleWith = emptySet(),
                    ),
                ).associateBy(EliteMutationDef::id),
        )
}
