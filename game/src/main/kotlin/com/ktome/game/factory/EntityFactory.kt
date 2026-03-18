package com.ktome.game.factory

import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Energy
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.ExperienceReward
import com.ktome.core.ecs.Faction
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.PatrolRoute
import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.item.Equipment
import com.ktome.core.item.Inventory
import com.ktome.core.map.Point
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentLoadout
import com.ktome.game.model.MonsterTemplate

class EntityFactory {
    fun createPlayer(
        world: World,
        position: Point,
        talents: List<TalentDef>,
        playerName: String = "Hero",
    ): com.ktome.core.ecs.EntityId {
        val playerId = world.createEntity()
        val profile = CombatProfile(
            baseAttack = 5,
            baseDefense = 2,
            baseAccuracy = 10,
            baseEvasion = 5,
            baseSpeed = 100,
            baseHp = 50,
        )
        val stats = Stats(str = 10, dex = 10, con = 10, wil = 10)
        val derivedStats = StatsCalculator.calculate(stats, profile)

        world.add(playerId, Position(position.x, position.y))
        world.add(playerId, Glyph('@'))
        world.add(playerId, DisplayColor("#FFD700"))
        world.add(playerId, Name(playerName))
        world.add(playerId, PlayerControlled)
        world.add(playerId, FactionTag(Faction.PLAYER))
        world.add(playerId, BlocksMovement())
        world.add(playerId, stats)
        world.add(playerId, profile)
        world.add(playerId, derivedStats)
        world.add(playerId, Health(current = derivedStats.maxHp, max = derivedStats.maxHp))
        world.add(playerId, Stamina(current = derivedStats.maxStamina, max = derivedStats.maxStamina))
        world.add(playerId, Energy())
        world.add(playerId, Experience())
        world.add(playerId, Inventory())
        world.add(playerId, Equipment())
        world.add(playerId, CooldownState())
        world.add(playerId, EffectTracker())
        world.add(
            playerId,
            TalentLoadout(
                slotToTalentId = talents.take(4).mapIndexed { index, talent -> (index + 1) to talent.id }.toMap(linkedMapOf()).toMutableMap(),
                talentLevels = talents.associate { talent -> talent.id to 1 }.toMutableMap(),
            ),
        )

        return playerId
    }

    fun createMonster(
        world: World,
        template: MonsterTemplate,
        position: Point,
        patrolRoute: PatrolRoute? = null,
    ): com.ktome.core.ecs.EntityId {
        val monsterId = world.createEntity()
        val profile = CombatProfile(
            baseAttack = template.baseAttack,
            baseDefense = template.baseDefense,
            baseAccuracy = 10,
            baseEvasion = 5,
            baseSpeed = template.speed,
            baseHp = template.baseHp,
        )
        val derivedStats = StatsCalculator.calculate(template.stats, profile)

        world.add(monsterId, Position(position.x, position.y))
        world.add(monsterId, Glyph(template.glyph))
        world.add(monsterId, DisplayColor(template.colorHex))
        world.add(monsterId, Name(template.name))
        world.add(monsterId, MonsterTemplateId(template.id))
        world.add(monsterId, FactionTag(Faction.MONSTER))
        world.add(monsterId, BlocksMovement())
        world.add(monsterId, template.stats.copy())
        world.add(monsterId, profile)
        world.add(monsterId, derivedStats)
        world.add(monsterId, Health(current = derivedStats.maxHp, max = derivedStats.maxHp))
        world.add(monsterId, Energy())
        world.add(monsterId, ExperienceReward(template.expReward))
        world.add(monsterId, EffectTracker())
        world.add(
            monsterId,
            when (resolveAiType(template)) {
                AIType.KITE -> AIBehavior(AIType.KITE, sightRadius = 8, preferredRangeStart = 2, preferredRangeEnd = 3)
                AIType.CHASE -> AIBehavior(AIType.CHASE, sightRadius = 8)
                AIType.PATROL -> AIBehavior(AIType.PATROL, sightRadius = 8)
            },
        )
        patrolRoute?.let { world.add(monsterId, it) }

        return monsterId
    }

    private fun resolveAiType(template: MonsterTemplate): AIType =
        when (template.aiProfileId) {
            "ai.kite.basic" -> AIType.KITE
            "ai.patrol.basic" -> AIType.PATROL
            "ai.chase.basic", "ai.boss.dungeon_lord" -> AIType.CHASE
            else -> template.ai
        }
}
