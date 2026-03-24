package com.ktome.game

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.talent.EffectTracker
import com.ktome.core.effect.WorldEffect
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class StatusRenderSnapshotSyncTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `render snapshot drives steady state hud and excludes world effects from actor matrix`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260323L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("status-render-snapshot-sync")),
            )

        val playerId = session.playerId
        val runtimeWorld = runtimeWorld(session)
        val tracker = runtimeWorld.get<EffectTracker>(playerId) ?: EffectTracker(ownerId = playerId).also { runtimeWorld.add(playerId, it) }
        repeat(2) { index ->
            StatusLifecycle.applyEffect(
                tracker,
                StatusLifecycle.createInstance(
                    type = StatusEffectType.ARMOR_BREAK,
                    effectId = "armor_break_$index",
                    duration = 3,
                    sourceEntityId = EntityId(index + 1),
                ),
            )
        }
        val worldEffectEntity = runtimeWorld.createEntity()
        runtimeWorld.add(
            worldEffectEntity,
            WorldEffect(
                effectId = "arena_aura",
                affectedActorIds = setOf(playerId),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "burn_world",
                            duration = 3,
                        ),
                    ),
            ),
        )
        invalidateRenderSnapshot(session)

        val playerActor = session.renderSnapshot().actors.single { actor -> actor.entityId == playerId.value }
        val armorBreak = playerActor.statusEffects.single()

        assertEquals(StatusEffectType.ARMOR_BREAK.schemaId, armorBreak.typeId)
        assertEquals(2, armorBreak.stackCount)
        assertEquals(3, armorBreak.stackCap)
        assertEquals(StatusEffectCategorySnapshot.DEBUFF, armorBreak.category)
        assertFalse(playerActor.statusEffects.any { effect -> effect.typeId == "burn_world" || effect.typeId == StatusEffectType.BURN.schemaId })
    }

    private fun runtimeWorld(session: FoundationGameSession): World {
        val field = FoundationGameSession::class.java.getDeclaredField("world")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(session) as World
    }

    private fun invalidateRenderSnapshot(session: FoundationGameSession) {
        val method = FoundationGameSession::class.java.getDeclaredMethod("invalidateRenderSnapshot")
        method.isAccessible = true
        method.invoke(session)
    }
}
