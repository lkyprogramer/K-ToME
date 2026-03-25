package com.ktome.game

import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.talent.CooldownState
import com.ktome.game.factory.EntityFactory
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Path

internal object ClassFormalizationTestSupport {
    fun newSession(
        tempDir: Path,
        professionId: String,
        zoneId: String = "shattered_outpost",
        playerRaceId: String = FOUNDATION_RACE_ID,
        seed: Long = 20260325L,
    ): FoundationGameSession =
        GameModule.newFoundationSession(
            config =
                FoundationGameConfig(
                    seed = seed,
                    zoneId = zoneId,
                    playerProfessionId = professionId,
                    playerRaceId = playerRaceId,
                ),
            saveManager = SaveManager(tempDir.resolve("$professionId-formalization-save")),
            availabilityContext = AvailabilityContext.DEV_LAB,
        )

    fun clearMonsters(session: FoundationGameSession) {
        val world = runtimeWorld(session)
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
    }

    fun installCombatDummy(
        session: FoundationGameSession,
        id: String = "training_dummy",
        position: Point? = null,
        clearExistingMonsters: Boolean = true,
    ): EntityId {
        val world = runtimeWorld(session)
        if (clearExistingMonsters) {
            world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
        }
        val dummyPosition = position ?: findOpenAdjacentPoint(session, session.playerPosition())
        val dummyId =
            EntityFactory().createMonster(
                world = world,
                template =
                    MonsterTemplate(
                        id = id,
                        name = "Training Dummy",
                        glyph = 'd',
                        colorHex = "#AAAAAA",
                        stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
                        baseHp = 200,
                        baseAttack = 1,
                        baseDefense = 0,
                        speed = 90,
                        ai = com.ktome.core.ecs.AIType.CHASE,
                        expReward = 0,
                        spawnFloors = listOf(session.currentFloor()),
                        spawnWeight = 1,
                    ),
                position = dummyPosition,
            )
        world.remove<AIBehavior>(dummyId)
        return dummyId
    }

    fun talentSlot(
        session: FoundationGameSession,
        talentId: String,
    ): Int = session.talentSlots().first { slot -> slot.talentId == talentId }.slot

    fun resourcePool(
        session: FoundationGameSession,
        type: ResourceType,
    ): ResourcePool = requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(type))

    fun entityPoint(
        session: FoundationGameSession,
        entityId: EntityId,
    ): Point = requireNotNull(runtimeWorld(session).get<Position>(entityId)).toPoint()

    fun monsterHp(
        session: FoundationGameSession,
        entityId: EntityId,
    ): Int = requireNotNull(runtimeWorld(session).get<Health>(entityId)).current

    fun playerCooldown(
        session: FoundationGameSession,
        talentId: String,
    ): Int = requireNotNull(runtimeWorld(session).get<CooldownState>(session.playerId)).remainingByTalentId[talentId] ?: 0

    fun runtimeWorld(session: FoundationGameSession): World = session.automationWorld()

    private fun findOpenAdjacentPoint(
        session: FoundationGameSession,
        center: Point,
    ): Point {
        val occupied =
            runtimeWorld(session)
                .entitiesWith(Position::class)
                .map { entityId -> requireNotNull(runtimeWorld(session).get<Position>(entityId)).toPoint() }
                .toSet()

        return Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .first { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }
    }
}
