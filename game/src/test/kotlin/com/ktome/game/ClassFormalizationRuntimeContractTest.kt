package com.ktome.game

import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourceType
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ClassFormalizationRuntimeContractTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `blood rush fails cleanly when no charge landing tile exists`() {
        val session = ClassFormalizationTestSupport.newSession(tempDir, professionId = "berserker")
        val playerPoint = session.playerPosition()
        val targetPoint = chargeTargetPoint(session)
        ClassFormalizationTestSupport.installCombatDummy(session, position = targetPoint)
        surroundTargetWithBlockingDummies(session, targetPoint)

        val bloodRushSlot = ClassFormalizationTestSupport.talentSlot(session, "blood_rush")

        assertFalse(session.perform(PlayerCommand.UseTalent(bloodRushSlot, targetPoint)))
        assertEquals(playerPoint, session.playerPosition())
    }

    @Test
    fun `mana lunge fails cleanly when no charge landing tile exists`() {
        val session = ClassFormalizationTestSupport.newSession(tempDir, professionId = "spellblade")
        val playerPoint = session.playerPosition()
        val targetPoint = chargeTargetPoint(session)
        ClassFormalizationTestSupport.installCombatDummy(session, position = targetPoint)
        surroundTargetWithBlockingDummies(session, targetPoint)

        val manaLungeSlot = ClassFormalizationTestSupport.talentSlot(session, "mana_lunge")

        assertFalse(session.perform(PlayerCommand.UseTalent(manaLungeSlot, targetPoint)))
        assertEquals(playerPoint, session.playerPosition())
    }

    @Test
    fun `spellblade miss does not shift equilibrium`() {
        for (seed in 1L..32L) {
            val session =
                ClassFormalizationTestSupport.newSession(
                    tempDir = tempDir.resolve("spellblade-miss-$seed"),
                    professionId = "spellblade",
                    seed = seed,
                )
            ClassFormalizationTestSupport.clearMonsters(session)
            val dummyId = ClassFormalizationTestSupport.installCombatDummy(session)
            val world = ClassFormalizationTestSupport.runtimeWorld(session)
            val dummyStats = requireNotNull(world.get<DerivedStats>(dummyId))
            world.add(dummyId, dummyStats.copy(evasion = 500))

            val manaPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.MANA)
            val equilibriumPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.EQUILIBRIUM)
            manaPool.current = manaPool.max
            equilibriumPool.current = 50

            val dummyHpBefore = ClassFormalizationTestSupport.monsterHp(session, dummyId)
            val talentSlot = ClassFormalizationTestSupport.talentSlot(session, "arcane_edge")
            val dummyPoint = ClassFormalizationTestSupport.entityPoint(session, dummyId)

            assertTrue(session.perform(PlayerCommand.UseTalent(talentSlot, dummyPoint)))
            val dummyHpAfter = ClassFormalizationTestSupport.monsterHp(session, dummyId)
            if (dummyHpAfter == dummyHpBefore) {
                assertEquals(50, requireNotNull(session.playerResourceView().secondary).current)
                return
            }
        }
        error("Expected at least one deterministic miss seed for spellblade equilibrium verification.")
    }

    @Test
    fun `spellblade equilibrium stable zone keeps arcane damage between low and high extremes`() {
        for (seed in 1L..32L) {
            val lowDamage = arcaneEdgeDamage(seed = seed, equilibrium = 10)
            val stableDamage = arcaneEdgeDamage(seed = seed, equilibrium = 50)
            val highDamage = arcaneEdgeDamage(seed = seed, equilibrium = 90)
            if (lowDamage > 0 && stableDamage > 0 && highDamage > 0) {
                assertTrue(lowDamage < stableDamage, "Low equilibrium should penalize arcane damage.")
                assertTrue(highDamage > stableDamage, "High equilibrium should amplify arcane damage.")
                return
            }
        }
        error("Expected at least one deterministic hit seed for equilibrium stable-zone verification.")
    }

    @Test
    fun `human race active talent is usable at runtime`() {
        val session =
            ClassFormalizationTestSupport.newSession(
                tempDir = tempDir,
                professionId = "vanguard",
                playerRaceId = "human",
            )
        val world = ClassFormalizationTestSupport.runtimeWorld(session)
        val health = requireNotNull(world.get<com.ktome.core.ecs.Health>(session.playerId))
        health.current = (health.max / 2).coerceAtLeast(1)

        val resolveSlot = ensureTalentEquipped(session, "human_resolve")

        assertTrue(session.perform(PlayerCommand.UseTalent(resolveSlot)))
        assertTrue(health.current > health.max / 2)
    }

    @Test
    fun `elf race active talent can execute mobility branch`() {
        val session =
            ClassFormalizationTestSupport.newSession(
                tempDir = tempDir,
                professionId = "rogue",
                playerRaceId = "elf",
            )
        ClassFormalizationTestSupport.clearMonsters(session)
        val before = session.playerPosition()
        val target = Point(before.x + 3, before.y)
        val scoutingSlot = ensureTalentEquipped(session, "elf_scouting")

        assertTrue(session.perform(PlayerCommand.UseTalent(scoutingSlot, target)))
        assertTrue(session.playerPosition() != before)
    }

    @Test
    fun `dwarf race active talent can execute self buff branch`() {
        val session =
            ClassFormalizationTestSupport.newSession(
                tempDir = tempDir,
                professionId = "templar",
                playerRaceId = "dwarf",
            )
        val world = ClassFormalizationTestSupport.runtimeWorld(session)
        val defenseBefore = requireNotNull(world.get<DerivedStats>(session.playerId)).defense
        val gritSlot = ensureTalentEquipped(session, "dwarf_grit")

        assertTrue(session.perform(PlayerCommand.UseTalent(gritSlot)))
        val defenseAfter = requireNotNull(world.get<DerivedStats>(session.playerId)).defense
        assertTrue(defenseAfter > defenseBefore)
    }

    private fun surroundTargetWithBlockingDummies(
        session: FoundationGameSession,
        targetPoint: Point,
    ) {
        Point.ALL_DIRECTIONS
            .map { delta -> targetPoint + delta }
            .filter { point ->
                point != session.playerPosition() &&
                    session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement
            }.forEachIndexed { index, point ->
                ClassFormalizationTestSupport.installCombatDummy(
                    session = session,
                    id = "blocking_dummy_$index",
                    position = point,
                    clearExistingMonsters = false,
                )
            }
    }

    private fun chargeTargetPoint(session: FoundationGameSession): Point {
        val origin = session.playerPosition()
        return listOf(
            Point(origin.x + 2, origin.y),
            Point(origin.x - 2, origin.y),
            Point(origin.x, origin.y + 2),
            Point(origin.x, origin.y - 2),
        ).first { point ->
            session.map.isInBounds(point.x, point.y) &&
                !session.map[point].blocksMovement
        }
    }

    private fun ensureTalentEquipped(
        session: FoundationGameSession,
        talentId: String,
        slot: Int = 4,
    ): Int {
        session.talentSlots().firstOrNull { talent -> talent.talentId == talentId }?.let { talent ->
            return talent.slot
        }
        check(session.perform(PlayerCommand.EquipTalentToSlot(slot = slot, talentId = talentId))) {
            "Failed to equip talent '$talentId' into slot $slot."
        }
        return slot
    }

    private fun arcaneEdgeDamage(
        seed: Long,
        equilibrium: Int,
    ): Int {
        val session =
            ClassFormalizationTestSupport.newSession(
                tempDir = tempDir.resolve("equilibrium-$seed-$equilibrium"),
                professionId = "spellblade",
                seed = seed,
            )
        ClassFormalizationTestSupport.clearMonsters(session)
        val dummyId = ClassFormalizationTestSupport.installCombatDummy(session)
        val world = ClassFormalizationTestSupport.runtimeWorld(session)
        val dummyStats = requireNotNull(world.get<DerivedStats>(dummyId))
        world.add(dummyId, dummyStats.copy(evasion = -100))

        val manaPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.MANA)
        val equilibriumPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.EQUILIBRIUM)
        manaPool.current = manaPool.max
        equilibriumPool.current = equilibrium

        val dummyPoint = ClassFormalizationTestSupport.entityPoint(session, dummyId)
        val hpBefore = ClassFormalizationTestSupport.monsterHp(session, dummyId)
        val talentSlot = ClassFormalizationTestSupport.talentSlot(session, "arcane_edge")
        check(session.perform(PlayerCommand.UseTalent(talentSlot, dummyPoint))) {
            "Arcane Edge should remain usable for equilibrium verification."
        }
        return hpBefore - ClassFormalizationTestSupport.monsterHp(session, dummyId)
    }
}
