package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SaveSnapshotRoundTripTest {
    private val codec = SaveCodec()

    @Test
    fun `round trip preserves phase2 save fixtures`() {
        listOf(
            SaveFixtures.emptyScene(),
            SaveFixtures.activeCombatScene(),
            SaveFixtures.resourceHeavyScene(),
        ).forEach { snapshot ->
            val restored = codec.decode(codec.encode(snapshot))
            assertEquals(snapshot, restored)
        }
    }
}
