package com.ktome.game.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZoneSchemaTest {
    @Test
    fun `zone skeletons reference valid pools and encounter contracts`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val monsterIds = catalog.monsters.map { it.id }.toSet()
        val bossIds = catalog.bossEncounters.map { it.id }.toSet()
        val objectiveIds = catalog.objectiveSets.map { it.id }.toSet()
        val tilesetIds = catalog.tilesets.map { it.id }.toSet()
        val ambientIds = catalog.ambientProfiles.map { it.id }.toSet()

        assertEquals(setOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths"), catalog.zones.map { it.id }.toSet())
        catalog.zones.forEach { zone ->
            assertEquals("zone.${zone.id}.name", zone.nameKey)
            assertEquals("zone.${zone.id}.desc", zone.descKey)
            assertTrue(tilesetIds.contains(zone.tilesetKey), "Unknown tileset ${zone.tilesetKey}")
            assertTrue(ambientIds.contains(zone.ambientProfile), "Unknown ambient profile ${zone.ambientProfile}")
            zone.monsterPools.forEach { monsterId -> assertTrue(monsterIds.contains(monsterId), "Unknown monster pool member $monsterId") }
            zone.elitePools.forEach { monsterId -> assertTrue(monsterIds.contains(monsterId), "Unknown elite pool member $monsterId") }
            zone.bossEncounterId?.let { bossId -> assertTrue(bossIds.contains(bossId), "Unknown boss encounter $bossId") }
            zone.objectiveSetId?.let { objectiveId -> assertTrue(objectiveIds.contains(objectiveId), "Unknown objective set $objectiveId") }
        }
    }
}
