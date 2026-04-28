package com.ktome.game.harness

import com.ktome.core.dungeon.DungeonManager
import com.ktome.core.dungeon.FloorState
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.Equipment
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.Inventory
import com.ktome.core.item.InventoryManager
import com.ktome.core.item.ItemInstance
import com.ktome.core.loot.RarityTier
import com.ktome.core.item.ItemType
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.talent.TalentRegistry
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameContent
import com.ktome.game.PlayerCommand
import com.ktome.game.PlayerResourceService
import com.ktome.game.SessionSnapshotMapper
import com.ktome.game.TalentProgression
import com.ktome.game.TalentProgressionRequest
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.factory.BossFactory
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal const val SOLO_CLEAR_SCRIPT_VERSION: String = "solo-clear-lab-v6"
internal const val SOLO_CLEAR_BOSS_TELEGRAPH_WAIT_TURNS: Int = 3
internal val SOLO_CLEAR_PROFESSIONS: List<String> =
    listOf(
        "vanguard",
        "arcanist",
        "rogue",
        "templar",
        "berserker",
        "spellblade",
    )

internal enum class SoloClearScenario(
    val seed: Long,
    val level: Int,
    val width: Int,
    val height: Int,
    val zoneId: String,
    val floor: Int,
    val maxTurns: Int,
    val summary: String,
) {
    MOB_PACK(
        seed = 20260313L,
        level = 5,
        width = 10,
        height = 10,
        zoneId = "greenwood_fringe",
        floor = 1,
        maxTurns = 80,
        summary = "10x10 closed room, 6 normal monsters, HP > 30%",
    ),
    ELITE(
        seed = 20260314L,
        level = 7,
        width = 15,
        height = 15,
        zoneId = "deep_iron_pit",
        floor = 1,
        maxTurns = 100,
        summary = "15x15 room, 1 elite + 2 normal monsters, survive",
    ),
    BOSS(
        seed = 20260315L,
        level = 10,
        width = 20,
        height = 20,
        zoneId = "shattered_outpost",
        floor = 2,
        maxTurns = 140,
        summary = "20x20 boss room, kill bandit captain, observe boss warning",
    ),
}

@Serializable
internal data class SoloClearLabReport(
    val professionId: String,
    val scenarioId: String,
    val seed: Long,
    val level: Int,
    val scriptVersion: String,
    val success: Boolean,
    val outcome: String,
    val turns: Int,
    val failureReason: String? = null,
    val currentHp: Int,
    val maxHp: Int,
    val resourceTypeId: String,
    val initialResource: Int,
    val peakResource: Int,
    val finalResource: Int,
    val secondaryResourceTypeId: String? = null,
    val initialSecondaryResource: Int? = null,
    val peakSecondaryResource: Int? = null,
    val finalSecondaryResource: Int? = null,
    val sawBossWarning: Boolean,
    val sawTalentTelegraph: Boolean,
    val commandTrace: List<String>,
    val executedTalentIds: List<String>,
    val resourceTimeline: List<Int>,
    val secondaryResourceTimeline: List<Int>,
    val lastMessages: List<String>,
    val eventTail: List<String>,
) {
    fun toJson() =
        buildJsonObject {
            put("professionId", professionId)
            put("scenarioId", scenarioId)
            put("seed", seed)
            put("level", level)
            put("scriptVersion", scriptVersion)
            put("success", success)
            put("outcome", outcome)
            put("turns", turns)
            failureReason?.let { put("failureReason", it) }
            put("currentHp", currentHp)
            put("maxHp", maxHp)
            put("resourceTypeId", resourceTypeId)
            put("initialResource", initialResource)
            put("peakResource", peakResource)
            put("finalResource", finalResource)
            secondaryResourceTypeId?.let { put("secondaryResourceTypeId", it) }
            initialSecondaryResource?.let { put("initialSecondaryResource", it) }
            peakSecondaryResource?.let { put("peakSecondaryResource", it) }
            finalSecondaryResource?.let { put("finalSecondaryResource", it) }
            put("sawBossWarning", sawBossWarning)
            put("sawTalentTelegraph", sawTalentTelegraph)
            putJsonArray("commandTrace") { commandTrace.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("executedTalentIds") { executedTalentIds.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("resourceTimeline") { resourceTimeline.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("secondaryResourceTimeline") { secondaryResourceTimeline.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("lastMessages") { lastMessages.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("eventTail") { eventTail.forEach { add(JsonPrimitive(it)) } }
        }
}

internal class SoloClearLabHarness(
    private val rootDir: Path = Files.createTempDirectory("ktome-solo-clear-lab"),
) {
    private val dataLoader = DataLoader()
    private val content = loadContent(dataLoader)
    private val professionsById = content.schemaCatalog.professions.associateBy(ProfessionSchemaV2::id)
    private val talentsById = content.talents.associateBy(TalentDef::id)
    private val monstersById = content.monsterCatalog.associateBy(MonsterTemplate::id)

    fun run(
        professionId: String,
        scenario: SoloClearScenario,
    ): SoloClearLabReport {
        val runtime = buildScenarioRuntime(professionId, scenario)
        val session = runtime.session
        val bot: RunBot = SoloClearScriptBot()
        val commandTrace = mutableListOf<String>()
        val executedTalentIds = mutableListOf<String>()
        val resourceTimeline = mutableListOf<Int>()
        val secondaryResourceTimeline = mutableListOf<Int>()
        var sawBossWarning = false
        var sawTalentTelegraph = false
        var sawResourceRestoreLog = false
        var failureReason: String? = null
        var turnCount = 0
        var bossTelegraphWaitTurns = 0

        while (turnCount < scenario.maxTurns && !session.runOutcome().isTerminal && !goalReached(session, runtime)) {
            val snapshot = session.renderSnapshot()
            sawBossWarning = sawBossWarning || snapshot.overlays.any { it.sourceAbilityId == "bandit_captain_encounter" }
            sawTalentTelegraph = sawTalentTelegraph || snapshot.overlays.any { it.id.startsWith("telegraph:") }
            sawResourceRestoreLog =
                sawResourceRestoreLog || snapshot.logEvents.any { event -> event.message.key == "log.talent.resource_restore" }

            val observation = RunObservationCapture.capture(session, turnCount)
            resourceTimeline += observation.playerResource.current
            observation.playerResource.secondary?.current?.let(secondaryResourceTimeline::add)
            val command =
                pendingBossTelegraphObservationCommand(
                    scenario = runtime.scenario,
                    observation = observation,
                    sawBossWarning = sawBossWarning,
                    sawTalentTelegraph = sawTalentTelegraph,
                    waitedTurns = bossTelegraphWaitTurns,
                )?.also {
                    bossTelegraphWaitTurns += 1
                } ?: run {
                    bossTelegraphWaitTurns = 0
                    bot.decide(observation)
                } ?: run {
                    failureReason = "Script bot returned no command."
                    break
                }
            commandTrace += renderCommand(command, observation)
            if (!session.perform(command)) {
                failureReason = "Command rejected: ${renderCommand(command)}"
                break
            }
            if (command is PlayerCommand.UseTalent) {
                observation.talentSlots.firstOrNull { slot -> slot.slot == command.slot }?.talentId?.let(executedTalentIds::add)
            }
            if (command.consumesTurn()) {
                turnCount += 1
            }
        }

        if (failureReason == null && goalReached(session, runtime) && professionId == "templar" && scenario == SoloClearScenario.MOB_PACK) {
            repeat(3) {
                val observation = RunObservationCapture.capture(session, turnCount)
                resourceTimeline += observation.playerResource.current
                observation.playerResource.secondary?.current?.let(secondaryResourceTimeline::add)
                commandTrace += renderCommand(PlayerCommand.Wait)
                if (session.perform(PlayerCommand.Wait)) {
                    turnCount += 1
                }
            }
        }

        val finalObservation = RunObservationCapture.capture(session, turnCount)
        resourceTimeline += finalObservation.playerResource.current
        finalObservation.playerResource.secondary?.current?.let(secondaryResourceTimeline::add)
        val finalSnapshot = session.renderSnapshot()
        sawBossWarning = sawBossWarning || finalSnapshot.overlays.any { it.sourceAbilityId == "bandit_captain_encounter" }
        sawTalentTelegraph = sawTalentTelegraph || finalSnapshot.overlays.any { it.id.startsWith("telegraph:") }
        sawResourceRestoreLog =
            sawResourceRestoreLog || finalSnapshot.logEvents.any { event -> event.message.key == "log.talent.resource_restore" }

        if (failureReason == null) {
            failureReason =
                validateRuntime(
                    runtime = runtime,
                    observation = finalObservation,
                    commandTrace = commandTrace,
                    resourceTimeline = resourceTimeline,
                    secondaryResourceTimeline = secondaryResourceTimeline,
                    executedTalentIds = executedTalentIds,
                    sawBossWarning = sawBossWarning,
                    sawTalentTelegraph = sawTalentTelegraph,
                    sawResourceRestoreLog = sawResourceRestoreLog,
                )
        }

        val success = failureReason == null
        return SoloClearLabReport(
            professionId = professionId,
            scenarioId = scenario.name.lowercase(),
            seed = scenario.seed,
            level = scenario.level,
            scriptVersion = SOLO_CLEAR_SCRIPT_VERSION,
            success = success,
            outcome = session.runOutcome().toString(),
            turns = turnCount,
            failureReason = failureReason,
            currentHp = finalObservation.playerStatus.currentHp,
            maxHp = finalObservation.playerStatus.maxHp,
            resourceTypeId = finalObservation.playerResource.typeId,
            initialResource = runtime.initialResource,
            peakResource = resourceTimeline.maxOrNull() ?: runtime.initialResource,
            finalResource = resourceTimeline.lastOrNull() ?: runtime.initialResource,
            secondaryResourceTypeId = finalObservation.playerResource.secondary?.typeId,
            initialSecondaryResource = secondaryResourceTimeline.firstOrNull(),
            peakSecondaryResource = secondaryResourceTimeline.maxOrNull(),
            finalSecondaryResource = secondaryResourceTimeline.lastOrNull(),
            sawBossWarning = sawBossWarning,
            sawTalentTelegraph = sawTalentTelegraph,
            commandTrace = commandTrace,
            executedTalentIds = executedTalentIds,
            resourceTimeline = resourceTimeline,
            secondaryResourceTimeline = secondaryResourceTimeline,
            lastMessages = finalObservation.messageLogTail,
            eventTail = finalObservation.eventTail,
        )
    }

    private fun buildScenarioRuntime(
        professionId: String,
        scenario: SoloClearScenario,
    ): SoloClearRuntime {
        val profession = requireNotNull(professionsById[professionId]) { "Unknown profession '$professionId'." }
        val playerStart = defaultPlayerStart(scenario)
        val map = createRoomMap(width = scenario.width, height = scenario.height, playerStart = playerStart)
        val world = World()
        val factory = EntityFactory()
        val playerId =
            factory.createPlayer(
                world = world,
                position = playerStart,
                talents = profession.startingTalents.mapNotNull(talentsById::get),
                playerName = content.localizer.text("actor.player.name"),
                stats = scaledStats(profession, scenario.level),
            )

        installScenarioLevel(world, playerId, scenario.level)
        seedScenarioTalents(world, playerId, professionId, scenario)
        installBlueGear(world, playerId, professionId)
        PlayerResourceService.ensureInitialized(world, playerId, profession)
        setInitialResource(world, playerId, professionId)

        val taggedEntities =
            when (scenario) {
                SoloClearScenario.MOB_PACK -> spawnMobPack(world)
                SoloClearScenario.ELITE -> spawnElitePack(world)
                SoloClearScenario.BOSS -> spawnBossEncounter(world)
            }

        val playerSnapshot = SessionSnapshotMapper.capturePlayer(world, playerId)
        val excludedEntities =
            linkedSetOf<EntityId>().apply {
                add(playerId)
                playerSnapshot.carriedEntities.mapTo(this) { snapshot -> EntityId(snapshot.id) }
            }
        val floorRuntime =
            SessionSnapshotMapper.captureFloor(
                map = map,
                stairsUp = null,
                stairsDown = null,
                exploredTiles = emptySet(),
                world = world,
                excludedEntities = excludedEntities,
            )
        val floorState = FloorState(floor = scenario.floor, stairsUp = null, stairsDown = null, payload = floorRuntime)
        val dungeonManager =
            DungeonManager(
                maxFloor = scenario.floor,
                startFloor = scenario.floor,
                floorLoader = { requestedFloor ->
                    require(requestedFloor == scenario.floor) { "Solo clear lab only defines floor ${scenario.floor}, requested $requestedFloor." }
                    floorState
                },
            )

        val saveManager = SaveManager(rootDir.resolve("${professionId.lowercase()}-${scenario.name.lowercase()}").resolve("save"))
        saveManager.deleteSave()
        val session =
            FoundationGameSession(
                config =
                    FoundationGameConfig(
                        seed = scenario.seed,
                        width = scenario.width,
                        height = scenario.height,
                        fovRadius = maxOf(scenario.width, scenario.height),
                        floor = scenario.floor,
                        maxFloor = scenario.floor,
                        zoneId = scenario.zoneId,
                        playerProfessionId = professionId,
                    ),
                content = content,
                saveManager = saveManager,
                dungeonManager = dungeonManager,
                playerSnapshot = playerSnapshot,
                initialMessageLog = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.session.loaded"))),
                isolatedZoneSlice = true,
            )

        return SoloClearRuntime(
            session = session,
            scenario = scenario,
            professionId = professionId,
            taggedEntities = taggedEntities,
            initialResource = session.playerResourceView().current,
        )
    }

    private fun validateRuntime(
        runtime: SoloClearRuntime,
        observation: RunObservation,
        commandTrace: List<String>,
        resourceTimeline: List<Int>,
        secondaryResourceTimeline: List<Int>,
        executedTalentIds: List<String>,
        sawBossWarning: Boolean,
        sawTalentTelegraph: Boolean,
        sawResourceRestoreLog: Boolean,
    ): String? {
        val session = runtime.session
        val world = session.automationWorld()
        when (runtime.scenario) {
            SoloClearScenario.MOB_PACK -> {
                val hasLivingMonsters = world.entitiesWith(MonsterTemplateId::class).any(world::isAlive)
                if (hasLivingMonsters) {
                    return "Mob pack scenario did not clear all monsters."
                }
                if (observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 30) {
                    return "Mob pack scenario ended below the 30% HP threshold."
                }
            }

            SoloClearScenario.ELITE -> {
                val eliteId = runtime.taggedEntities["elite"] ?: return "Elite scenario lost its tagged elite entity."
                if (world.isAlive(eliteId)) {
                    return "Elite scenario did not kill the elite target."
                }
                if (session.isGameOver()) {
                    return "Elite scenario ended in defeat."
                }
            }

            SoloClearScenario.BOSS -> {
                if (!session.isVictory()) {
                    return "Boss scenario did not produce a victory outcome."
                }
                if (!sawBossWarning) {
                    return "Boss scenario did not expose the boss warning overlay."
                }
            }
        }

        if (
            runtime.scenario == SoloClearScenario.MOB_PACK &&
            commandTrace.none { command -> command.startsWith("UseTalent(") }
        ) {
            return "Solo clear script did not execute any talent command."
        }

        if (runtime.professionId == "rogue" && runtime.scenario == SoloClearScenario.MOB_PACK) {
            val spentResource = resourceTimeline.zipWithNext().any { (before, after) -> after < before }
            val restoredResource = resourceTimeline.zipWithNext().any { (before, after) -> after > before }
            if (!spentResource) {
                return "Rogue ENERGY never dropped, so the spend loop was not exercised."
            }
            if (!restoredResource) {
                return "Rogue ENERGY never recovered after talent usage or successful hits."
            }
            if (!sawResourceRestoreLog) {
                return "Rogue ENERGY recovered only through passive turn regen; hit-driven restore was never observed."
            }
        }

        if (runtime.professionId == "templar" && runtime.scenario == SoloClearScenario.MOB_PACK) {
            val peak = resourceTimeline.maxOrNull() ?: runtime.initialResource
            val spentResource = resourceTimeline.zipWithNext().any { (before, after) -> after < before }
            val restoredResource = resourceTimeline.zipWithNext().any { (before, after) -> after > before }
            if (!spentResource) {
                return "Templar POSITIVE_ENERGY was never spent during combat."
            }
            if (!restoredResource) {
                return "Templar POSITIVE_ENERGY never restored during combat."
            }
            val peakIndex = resourceTimeline.indexOfFirst { value -> value == peak }
            val decayedAfterPeak = resourceTimeline.drop(peakIndex + 1).any { value -> value < peak }
            if (!decayedAfterPeak) {
                return "Templar POSITIVE_ENERGY did not decay after combat ended."
            }
        }

        if (runtime.professionId == "berserker" && runtime.scenario == SoloClearScenario.MOB_PACK) {
            val restoredResource = resourceTimeline.zipWithNext().any { (before, after) -> after > before }
            val spentResource = resourceTimeline.zipWithNext().any { (before, after) -> after < before }
            if (!restoredResource) {
                return "Berserker HATE never increased during combat."
            }
            if (!spentResource) {
                return "Berserker HATE was never spent by an active talent."
            }
            if (executedTalentIds.none { talentId -> talentId in BERSERKER_TALENT_IDS }) {
                return "Berserker smoke never executed a berserker talent."
            }
        }

        if (runtime.professionId == "spellblade" && runtime.scenario == SoloClearScenario.MOB_PACK) {
            if (secondaryResourceTimeline.distinct().size <= 1) {
                return "Spellblade EQUILIBRIUM never changed during combat."
            }
            if (executedTalentIds.none { talentId -> talentId in SPELLBLADE_TALENT_IDS }) {
                return "Spellblade smoke never executed a spellblade talent."
            }
        }

        return null
    }

    private fun goalReached(
        session: FoundationGameSession,
        runtime: SoloClearRuntime,
    ): Boolean {
        val world = session.automationWorld()
        return when (runtime.scenario) {
            SoloClearScenario.MOB_PACK ->
                !session.isGameOver() &&
                    world.entitiesWith(MonsterTemplateId::class).none(world::isAlive) &&
                    session.playerStatus().currentHp * 100 > session.playerStatus().maxHp * 30

            SoloClearScenario.ELITE -> {
                val eliteId = runtime.taggedEntities["elite"] ?: return false
                !session.isGameOver() && !world.isAlive(eliteId)
            }

            SoloClearScenario.BOSS -> session.isVictory()
        }
    }

    private fun installScenarioLevel(
        world: World,
        playerId: EntityId,
        level: Int,
    ) {
        val experience = requireNotNull(world.get<Experience>(playerId)) { "Missing Experience for $playerId." }
        experience.level = level
        experience.current = 0
        experience.unspentStatPoints = 0
        experience.unspentTalentPoints = 0
    }

    private fun seedScenarioTalents(
        world: World,
        playerId: EntityId,
        professionId: String,
        scenario: SoloClearScenario,
    ) {
        val profession = requireNotNull(professionsById[professionId]) { "Unknown profession '$professionId'." }
        val loadout = requireNotNull(world.get<TalentLoadout>(playerId)) { "Missing TalentLoadout for $playerId." }
        var materializedChoice = true
        while (materializedChoice) {
            val learnableTalentIds =
                TalentProgression.learnableTalentIds(
                    TalentProgressionRequest(
                        schemaCatalog = content.schemaCatalog,
                        profession = profession,
                        level = scenario.level,
                        learnedRanks = loadout.talentLevels,
                    ),
                )
            materializedChoice = false
            learnableTalentIds.forEach { talentId ->
                if (talentId !in loadout.talentLevels) {
                    loadout.talentLevels[talentId] = 1
                    materializedChoice = true
                }
            }
        }

        val desiredActiveTalents =
            preferredScenarioTalentOrder(professionId = professionId, scenario = scenario)
                .filter(talentsById::containsKey)
        desiredActiveTalents.forEach { talentId ->
            loadout.talentLevels.putIfAbsent(talentId, 1)
        }

        val activeTalentIds =
            linkedSetOf<String>().apply {
                desiredActiveTalents.forEach(::add)
                profession.startingTalents.filter(loadout.talentLevels::containsKey).forEach(::add)
                loadout.talentLevels.keys.forEach(::add)
            }.take(4)

        loadout.slotToTalentId.clear()
        activeTalentIds.forEachIndexed { index, talentId ->
            loadout.slotToTalentId[index + 1] = talentId
        }
    }

    private fun preferredScenarioTalentOrder(
        professionId: String,
        scenario: SoloClearScenario,
    ): List<String> =
        when (professionId) {
            "vanguard" ->
                when (scenario) {
                    SoloClearScenario.BOSS -> listOf("power_strike", "linebreaker", "earthshaker", "battlefield_command")
                    SoloClearScenario.MOB_PACK -> listOf("sweeping_strike", "earthshaker", "battlefield_command", "linebreaker")
                    SoloClearScenario.ELITE -> listOf("power_strike", "linebreaker", "battlefield_command", "earthshaker")
                }

            "arcanist" ->
                when (scenario) {
                    SoloClearScenario.BOSS -> listOf("fireball", "void_breach", "inferno_orb", "glacial_seal")
                    SoloClearScenario.MOB_PACK -> listOf("fireball", "blink", "arcane_shield", "frost_nova")
                    SoloClearScenario.ELITE -> listOf("fireball", "blink", "arcane_shield", "void_breach")
                }

            "rogue" ->
                when (scenario) {
                    SoloClearScenario.BOSS -> listOf("backstab", "shadow_bind", "eviscerate", "deathblow")
                    SoloClearScenario.MOB_PACK -> listOf("ricochet_knives", "smoke_bomb", "shadow_bind", "blade_flurry")
                    SoloClearScenario.ELITE -> listOf("poison_blade", "shadow_bind", "backstab", "eviscerate")
                }

            "templar" ->
                when (scenario) {
                    SoloClearScenario.BOSS -> listOf("holy_strike", "consecration", "ritual_break", "sanctuary")
                    SoloClearScenario.MOB_PACK -> listOf("judgment_hammer", "consecration", "holy_aura", "sanctuary")
                    SoloClearScenario.ELITE -> listOf("holy_strike", "judgment_hammer", "consecration", "sanctuary")
                }

            else -> emptyList()
        }

    private fun installBlueGear(
        world: World,
        playerId: EntityId,
        professionId: String,
    ) {
        val inventory = requireNotNull(world.get<Inventory>(playerId)) { "Missing Inventory for $playerId." }
        val equipment = requireNotNull(world.get<Equipment>(playerId)) { "Missing Equipment for $playerId." }
        val itemFactory = ItemFactory()
        val inventoryManager = InventoryManager()
        val items =
            buildList {
                add(blueWeapon(professionId))
                add(blueArmor(professionId))
                blueAccessory(professionId)?.let(::add)
                add(healingPotion())
                add(healingPotion())
                if (professionId == "arcanist" || professionId == "templar" || professionId == "berserker" || professionId == "spellblade") {
                    add(healingPotion())
                }
            }
        items.forEach { item ->
            inventory.itemIds += itemFactory.createCarriedItem(world, item)
        }
        inventory.itemIds.forEachIndexed { index, itemId ->
            val slot = world.get<ItemInstance>(itemId)?.slot ?: return@forEachIndexed
            if (slot !in equipment.slots && inventoryManager.equip(world, playerId, index).success) {
                return@forEachIndexed
            }
        }
    }

    private fun blueWeapon(professionId: String): ItemInstance =
        when (professionId) {
            "berserker" ->
                ItemInstance(
                    baseId = "battle_axe",
                    name = "Solo Lab Blue Axe",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    stats = StatModifier(str = 6, con = 2, attack = 13, accuracy = 4, critChance = 0.06),
                )

            "spellblade" ->
                ItemInstance(
                    baseId = "long_sword",
                    name = "Solo Lab Blue Spellblade",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    stats = StatModifier(wil = 4, attack = 9, accuracy = 6, talentPower = 0.18),
                )

            "arcanist" ->
                ItemInstance(
                    baseId = "arcane_staff",
                    name = "Solo Lab Blue Focus",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    stats = StatModifier(wil = 5, attack = 10, accuracy = 5, talentPower = 0.20),
                )

            "rogue" ->
                ItemInstance(
                    baseId = "short_sword",
                    name = "Solo Lab Blue Blade",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    stats = StatModifier(str = 2, dex = 3, attack = 10, accuracy = 5, speed = 8, critChance = 0.08),
                )

            else ->
                ItemInstance(
                    baseId = "short_sword",
                    name = "Solo Lab Blue Weapon",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    stats = StatModifier(str = 3, wil = 1, attack = 11, accuracy = 4),
                )
        }

    private fun blueArmor(professionId: String): ItemInstance =
        ItemInstance(
            baseId =
                when (professionId) {
                    "spellblade" -> "apprentice_robe"
                    else -> "chain_mail"
                },
            name = "Solo Lab Blue Armor",
            type = ItemType.ARMOR,
            slot = EquipSlot.ARMOR,
            glyph = '[',
            colorHex = "#4B6DFF",
            quality = RarityTier.RARE,
            stats =
                when (professionId) {
                    "berserker" -> StatModifier(defense = 16, maxHp = 88, con = 4, str = 2)
                    "spellblade" -> StatModifier(defense = 11, maxHp = 62, wil = 4, evasion = 2, talentPower = 0.12)
                    "rogue" -> StatModifier(defense = 9, maxHp = 26, dex = 2, speed = 5)
                    "arcanist" -> StatModifier(defense = 14, maxHp = 78, wil = 3)
                    "templar" -> StatModifier(defense = 15, maxHp = 72, con = 2)
                    else -> StatModifier(defense = 11, maxHp = 30, con = 2)
                },
        )

    private fun blueAccessory(professionId: String): ItemInstance? =
        when (professionId) {
            "arcanist" ->
                ItemInstance(
                    baseId = "emerald_charm",
                    name = "Solo Lab Blue Charm",
                    type = ItemType.ARMOR,
                    slot = EquipSlot.OFF_HAND,
                    glyph = ']',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    stats = StatModifier(defense = 2, evasion = 2, maxHp = 18, wil = 1),
                )

            "spellblade" ->
                ItemInstance(
                    baseId = "emerald_charm",
                    name = "Solo Lab Blue Spell Charm",
                    type = ItemType.ARMOR,
                    slot = EquipSlot.OFF_HAND,
                    glyph = ']',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    stats = StatModifier(defense = 2, evasion = 2, maxHp = 18, wil = 1),
                )

            "templar" ->
                ItemInstance(
                    baseId = "sanctified_seal",
                    name = "Solo Lab Blue Seal",
                    type = ItemType.ARMOR,
                    slot = EquipSlot.OFF_HAND,
                    glyph = ']',
                    colorHex = "#4B6DFF",
                    quality = RarityTier.RARE,
                    stats = StatModifier(defense = 3, maxHp = 22, wil = 1),
                )

            else -> null
        }

    private fun healingPotion(): ItemInstance =
        ItemInstance(
            baseId = "healing_potion",
            name = "Greater Healing Potion",
            type = ItemType.CONSUMABLE,
            glyph = '!',
            colorHex = "#FF0000",
            quality = RarityTier.MAGIC,
            effect = ConsumableEffect.HEAL,
            magnitude = 45,
        )

    private fun setInitialResource(
        world: World,
        playerId: EntityId,
        professionId: String,
    ) {
        val pools = world.get<ResourcePools>(playerId) ?: return
        when (professionId) {
            "berserker" -> pools.pool(ResourceType.HATE)?.syncTo(nextCurrent = 40, nextMax = 100)
            "spellblade" -> pools.pool(ResourceType.MANA)?.syncTo(nextCurrent = 160, nextMax = 160)
            "arcanist" -> pools.pool(ResourceType.MANA)?.syncTo(nextCurrent = 160, nextMax = 160)
            "rogue" -> pools.pool(ResourceType.ENERGY)?.syncTo(nextCurrent = 60, nextMax = 100)
            "templar" -> pools.pool(ResourceType.POSITIVE_ENERGY)?.syncTo(nextCurrent = 24, nextMax = 100)
            else -> Unit
        }
    }

    private fun spawnMobPack(world: World): Map<String, EntityId> =
        linkedMapOf<String, EntityId>().apply {
            createMonster(world, "beast.rat_scavenger", Point(3, 2))
            createMonster(world, "goblin.scout", Point(5, 2))
            createMonster(world, "bandit.raider", Point(6, 3))
            createMonster(world, "bandit.archer", Point(6, 5))
            createMonster(world, "undead.restless_skeleton", Point(3, 6))
            createMonster(world, "beast.rat_scavenger", Point(5, 6))
        }

    private fun spawnElitePack(world: World): Map<String, EntityId> =
        linkedMapOf<String, EntityId>().apply {
            put("elite", createMonster(world, "undead.bone_guard", Point(8, 7)))
            createMonster(world, "bandit.archer", Point(10, 5))
            createMonster(world, "bandit.raider", Point(10, 9))
        }

    private fun spawnBossEncounter(world: World): Map<String, EntityId> =
        linkedMapOf(
            "boss" to createBoss(world, "bandit_captain_encounter", Point(11, 10)),
        )

    private fun createMonster(
        world: World,
        monsterId: String,
        position: Point,
    ): EntityId =
        EntityFactory().createMonster(
            world = world,
            template = requireNotNull(monstersById[monsterId]) { "Unknown monster '$monsterId'." },
            position = position,
        )

    private fun createBoss(
        world: World,
        encounterId: String,
        position: Point,
    ): EntityId =
        BossFactory().createBoss(
            world = world,
            definition = requireNotNull(content.bossDefinitions[encounterId]) { "Unknown boss encounter '$encounterId'." },
            position = position,
        )

    private fun scaledStats(
        profession: ProfessionSchemaV2,
        level: Int,
    ): Stats {
        val bonusLevels = (level - 1).coerceAtLeast(0)
        return Stats(
            str = profession.baseStats.str + profession.statGrowth.str * bonusLevels,
            dex = profession.baseStats.dex + profession.statGrowth.dex * bonusLevels,
            con = profession.baseStats.con + profession.statGrowth.con * bonusLevels,
            wil = profession.baseStats.wil + profession.statGrowth.wil * bonusLevels,
        )
    }

    private fun defaultPlayerStart(scenario: SoloClearScenario): Point =
        when (scenario) {
            SoloClearScenario.MOB_PACK -> Point(2, 2)
            SoloClearScenario.ELITE -> Point(3, 7)
            SoloClearScenario.BOSS -> Point(10, 10)
        }

    private fun createRoomMap(
        width: Int,
        height: Int,
        playerStart: Point,
    ): GameMap {
        val rows =
            buildList {
                repeat(height) { y ->
                    add(
                        buildString {
                            repeat(width) { x ->
                                append(
                                    if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                                        '#'
                                    } else {
                                        '.'
                                    },
                                )
                            }
                        },
                    )
                }
            }
        return GameMap.fromAscii(rows = rows, playerStart = playerStart)
    }

    private fun loadContent(loader: DataLoader): GameContent {
        val schemaCatalog = loader.loadSchemaCatalog()
        val talents = loader.loadTalentDefinitions()
        return GameContent(
            talents = talents,
            statuses = schemaCatalog.statuses,
            statusCatalog = loader.loadStatusCatalog(),
            talentRegistry = TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinitions = loader.loadBossDefinitions(),
            schemaCatalog = schemaCatalog,
            localizer = loader.localizer,
        )
    }

    private fun renderCommand(
        command: PlayerCommand,
        observation: RunObservation? = null,
    ): String =
        when (command) {
            is PlayerCommand.Move -> "Move(${command.delta.x},${command.delta.y})"
            PlayerCommand.Wait -> "Wait"
            PlayerCommand.PickUp -> "PickUp"
            PlayerCommand.Interact -> "Interact"
            PlayerCommand.Search -> "Search"
            PlayerCommand.Ascend -> "Ascend"
            PlayerCommand.Descend -> "Descend"
            PlayerCommand.SaveGame -> "SaveGame"
            PlayerCommand.CloseShop -> "CloseShop"
            PlayerCommand.CancelInscriptionReplacementPurchase -> "CancelInscriptionReplacementPurchase"
            is PlayerCommand.BuyShopOffer -> "BuyShopOffer(${command.index})"
            is PlayerCommand.SellInventoryItem -> "SellInventoryItem(${command.index})"
            is PlayerCommand.DropInventoryItem -> "DropInventoryItem(${command.index})"
            is PlayerCommand.SelectRoute -> "SelectRoute(${command.index})"
            is PlayerCommand.ActivateInventoryItem -> "ActivateInventoryItem(${command.index})"
            is PlayerCommand.UseInscription -> "UseInscription(${command.hotkey})"
            is PlayerCommand.UseTalent ->
                observation
                    ?.talentSlots
                    ?.firstOrNull { slot -> slot.slot == command.slot }
                    ?.talentId
                    ?.let { talentId ->
                        command.target?.let { target -> "UseTalent(${command.slot}:$talentId,${target.x},${target.y})" } ?: "UseTalent(${command.slot}:$talentId)"
                    } ?: command.target?.let { target -> "UseTalent(${command.slot},${target.x},${target.y})" } ?: "UseTalent(${command.slot})"
            is PlayerCommand.EquipTalentToSlot -> "EquipTalentToSlot(${command.slot},${command.talentId})"
            is PlayerCommand.AssignStat -> "AssignStat(${command.stat})"
            is PlayerCommand.AssignTalent -> "AssignTalent(${command.talentId})"
            PlayerCommand.ConfirmTalentDraft -> "ConfirmTalentDraft"
            PlayerCommand.ConfirmTalentDraftToReserve -> "ConfirmTalentDraftToReserve"
            is PlayerCommand.ConfirmTalentDraftReplacingSlot -> "ConfirmTalentDraftReplacingSlot(${command.slot})"
            PlayerCommand.RollbackTalentDraft -> "RollbackTalentDraft"
            is PlayerCommand.RespecTalentTree -> "RespecTalentTree(${command.ownerType},${command.treeOwnerId})"
            is PlayerCommand.Validation -> com.ktome.game.harness.renderCommand(command)
        }

    internal fun reportsToJson(reports: List<SoloClearLabReport>) =
        buildJsonObject {
            put("scriptVersion", SOLO_CLEAR_SCRIPT_VERSION)
            putJsonObject("scenarios") {
                SoloClearScenario.entries.forEach { scenario ->
                    put(scenario.name.lowercase(), scenario.summary)
                }
            }
            putJsonArray("reports") {
                reports.forEach { report -> add(report.toJson()) }
            }
        }

    internal fun reportsToMarkdown(reports: List<SoloClearLabReport>): String =
        buildString {
            appendLine("# Solo Clear Lab")
            appendLine("- scriptVersion: $SOLO_CLEAR_SCRIPT_VERSION")
            SoloClearScenario.entries.forEach { scenario ->
                appendLine("- ${scenario.name.lowercase()}: seed=${scenario.seed}, ${scenario.summary}")
            }
            reports.forEach { report ->
                val secondarySummary =
                    report.secondaryResourceTypeId?.let { type ->
                        ", secondary=$type:${report.finalSecondaryResource}"
                    }.orEmpty()
                appendLine(
                    "- profession=${report.professionId}, scenario=${report.scenarioId}, success=${report.success}, turns=${report.turns}, hp=${report.currentHp}/${report.maxHp}, resource=${report.resourceTypeId}:${report.finalResource}$secondarySummary, outcome=${report.outcome}",
                )
            }
        }
}

private data class SoloClearRuntime(
    val session: FoundationGameSession,
    val scenario: SoloClearScenario,
    val professionId: String,
    val taggedEntities: Map<String, EntityId>,
    val initialResource: Int,
)

internal fun pendingBossTelegraphObservationCommand(
    scenario: SoloClearScenario,
    observation: RunObservation,
    sawBossWarning: Boolean,
    sawTalentTelegraph: Boolean,
    waitedTurns: Int,
): PlayerCommand? {
    if (scenario != SoloClearScenario.BOSS) {
        return null
    }
    if (!sawBossWarning || sawTalentTelegraph || waitedTurns >= SOLO_CLEAR_BOSS_TELEGRAPH_WAIT_TURNS) {
        return null
    }
    val nearestBossDistance =
        observation.visibleBossPositions
            .minOfOrNull { bossPosition -> bossPosition.chebyshevDistanceTo(observation.playerPosition) }
            ?: return null
    if (nearestBossDistance > 1) {
        return null
    }
    val safeEnoughToWait = observation.playerStatus.currentHp * 100 > observation.playerStatus.maxHp * 55
    if (!safeEnoughToWait) {
        return null
    }
    return PlayerCommand.Wait
}

private class SoloClearScriptBot : RunBot {
    override fun decide(observation: RunObservation): PlayerCommand? {
        val shouldReconfigureLoadout =
            isDensePackScenario(observation) || observation.visibleBossPositions.isNotEmpty()
        val usesSpecializedPackLoadout = usesSpecializedPackLoadout(observation)
        if (shouldReconfigureLoadout && usesSpecializedPackLoadout) {
            preferredCombatLoadoutCommand(observation)?.let { return it }
        }
        if (shouldReconfigureLoadout && !usesSpecializedPackLoadout) {
            LoadoutPlanner.preferredLoadoutCommand(observation)?.let { return it }
        }
        lootFollowUp(observation)?.let { return it }
        useEmergencyConsumable(observation)?.let { return it }
        useEmergencyTalent(observation)?.let { return it }
        useOffensiveTalent(observation)?.let { return it }
        pursueVisibleHostile(observation)?.let { return it }
        return PlayerCommand.Wait
    }

    private fun isDensePackScenario(observation: RunObservation): Boolean =
        observation.visibleBossPositions.isEmpty() && observation.visibleHostilePositions.size >= 4

    private fun usesSpecializedPackLoadout(observation: RunObservation): Boolean =
        observation.playerResource.typeId in setOf("STAMINA", "MANA", "ENERGY", "POSITIVE_ENERGY", "HATE")

    private fun preferredCombatLoadoutCommand(observation: RunObservation): PlayerCommand? {
        if (!usesSpecializedPackLoadout(observation)) {
            return null
        }
        val learnedTalentIds =
            linkedSetOf<String>().apply {
                observation.talentSlots.mapTo(this) { slot -> slot.talentId }
                observation.reserveTalents.mapTo(this) { talent -> talent.talentId }
            }
        val desiredOrder =
            when (observation.playerResource.typeId) {
                "STAMINA" ->
                    if (observation.visibleBossPositions.isNotEmpty()) {
                        listOf("power_strike", "linebreaker", "earthshaker", "guard_stance", "battlefield_command", "shield_bash")
                    } else if (isDensePackScenario(observation)) {
                        listOf("sweeping_strike", "earthshaker", "battlefield_command", "shield_bash", "linebreaker", "war_cry")
                    } else {
                        listOf("power_strike", "shield_bash", "linebreaker", "earthshaker", "battlefield_command", "charge")
                    }
                "MANA" ->
                    if (listOf("arcane_edge", "mana_lunge", "spell_parry", "spell_rend", "flux_burst", "flux_anchor").any(learnedTalentIds::contains)) {
                        if (observation.visibleBossPositions.isNotEmpty()) {
                            listOf("sunder_sigil", "arcane_edge", "blink_strike", "spell_parry", "balance_point", "spell_rend")
                        } else if (isDensePackScenario(observation)) {
                            listOf("runic_edge", "flux_burst", "blink_strike", "flux_reversal", "balance_point", "mana_lunge")
                        } else {
                            listOf("arcane_edge", "runic_edge", "spell_parry", "balance_point", "blink_strike", "flux_reversal")
                        }
                    } else {
                        if (observation.visibleBossPositions.isNotEmpty()) {
                            listOf("fireball", "void_breach", "blink", "arcane_shield", "inferno_orb", "glacial_seal")
                        } else if (isDensePackScenario(observation)) {
                            listOf("fireball", "inferno_orb", "frost_nova", "glacial_seal", "blink", "arcane_shield")
                        } else {
                            listOf("fireball", "blink", "arcane_shield", "void_breach", "glacial_seal", "mana_surge")
                        }
                    }
                "ENERGY" ->
                    if (observation.visibleBossPositions.isNotEmpty()) {
                        listOf("backstab", "shadow_bind", "eviscerate", "deathblow", "dusk_shroud", "shadowstep")
                    } else if (isDensePackScenario(observation)) {
                        listOf("ricochet_knives", "smoke_bomb", "shadow_bind", "blade_flurry", "eviscerate", "stealth")
                    } else {
                        listOf("poison_blade", "shadow_bind", "backstab", "eviscerate", "roll", "stealth")
                    }
                "POSITIVE_ENERGY" ->
                    if (observation.visibleBossPositions.isNotEmpty()) {
                        listOf("holy_strike", "consecration", "ritual_break", "sanctuary", "judgment_hammer", "holy_shield")
                    } else if (isDensePackScenario(observation)) {
                        listOf("judgment_hammer", "consecration", "holy_aura", "holy_strike", "sanctuary", "holy_light")
                    } else {
                        listOf("holy_strike", "judgment_hammer", "consecration", "holy_shield", "holy_light", "sanctuary")
                    }
                "HATE" ->
                    if (observation.visibleBossPositions.isNotEmpty()) {
                        listOf("riven_edge", "savage_hew", "fault_line", "slaughter_drive", "last_stand", "aftershock")
                    } else if (isDensePackScenario(observation)) {
                        listOf("aftershock", "reckless_slam", "slaughter_drive", "pain_fuel", "fault_line", "kill_frenzy")
                    } else {
                        listOf("savage_hew", "pursuit_drive", "pain_fuel", "slaughter_drive", "fault_line", "kill_frenzy")
                    }
                else -> emptyList()
            }
        if (desiredOrder.isEmpty()) {
            return null
        }
        desiredOrder
            .filter(learnedTalentIds::contains)
            .take(4)
            .forEachIndexed { index, talentId ->
                val targetSlot = index + 1
                val currentTalentId = observation.talentSlots.firstOrNull { slot -> slot.slot == targetSlot }?.talentId
                if (currentTalentId != talentId) {
                    return PlayerCommand.EquipTalentToSlot(slot = targetSlot, talentId = talentId)
                }
            }
        return null
    }

    private fun lootFollowUp(observation: RunObservation): PlayerCommand? {
        if (observation.visibleHostilePositions.isNotEmpty()) {
            return null
        }
        if (observation.visibleGroundItemPositions.any { point -> point == observation.playerPosition }) {
            return PlayerCommand.PickUp
        }
        val target =
            observation.visibleGroundItemPositions
                .minByOrNull { point -> point.chebyshevDistanceTo(observation.playerPosition) }
                ?: return null
        val nextStep = stepToward(observation, target) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun useEmergencyConsumable(observation: RunObservation): PlayerCommand? {
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 45
        if (!lowHealth) {
            return null
        }
        val potionIndex = observation.inventoryItems.indexOfFirst { item -> item.effect == ConsumableEffect.HEAL }
        return potionIndex.takeIf { it >= 0 }?.let(PlayerCommand::ActivateInventoryItem)
    }

    private fun useEmergencyTalent(observation: RunObservation): PlayerCommand? {
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 50
        if (!lowHealth) {
            return null
        }
        availableTalent(observation, "last_stand")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "slaughter_drive")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "spell_parry")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "flux_reversal")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "kill_frenzy")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "flux_anchor")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "balance_point")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "holy_light")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "divine_intervention")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "holy_shield")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "stealth")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "unyielding")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "arcane_shield")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "guard_stance")?.let { return PlayerCommand.UseTalent(it.slot) }
        availableTalent(observation, "blink")?.let { slot ->
            retreatPoint(observation, slot)?.let { target ->
                return PlayerCommand.UseTalent(slot.slot, target)
            }
        }
        availableTalent(observation, "roll")?.let { slot ->
            retreatPoint(observation, slot)?.let { target ->
                return PlayerCommand.UseTalent(slot.slot, target)
            }
        }
        nearestHostile(observation)?.let { target ->
            availableTargetedTalent(observation, target, "charge")?.let { return it }
        }
        return null
    }

    private fun useOffensiveTalent(observation: RunObservation): PlayerCommand? {
        val nearest = preferredOffensiveTarget(observation) ?: return null
        val adjacentHostiles = hostilesWithin(observation, 1)
        val nearbyHostiles = hostilesWithin(observation, 2)
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 70

        if (adjacentHostiles >= 2) {
            availableTalent(observation, "earthshaker")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "consecration")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "blade_flurry")?.let { return PlayerCommand.UseTalent(it.slot, nearest) }
            availableTalent(observation, "reckless_slam")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "flux_burst")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "holy_aura")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "sweeping_strike")?.let { return PlayerCommand.UseTalent(it.slot, nearest) }
            availableTalent(observation, "frost_nova")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (nearbyHostiles >= 2) {
            availableTalent(observation, "battlefield_command")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "beacon_of_zeal")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "smoke_bomb")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "war_cry")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "intimidation")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "kill_frenzy")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "balance_point")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "slaughter_drive")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (observation.playerResource.typeId == "MANA" && observation.playerResource.current * 100 <= observation.playerResource.max * 40) {
            availableTalent(observation, "mana_surge")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (observation.playerResource.typeId == "MANA" && observation.visibleHostilePositions.size >= 4) {
            availableTalent(observation, "arcane_shield")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (nearbyHostiles >= 1) {
            availableTalent(observation, "bulwark_march")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "sanctuary")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "dusk_shroud")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "devotion")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "holy_shield")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "stealth")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        availableTargetedTalent(observation, nearest, "shadow_bind")?.let { return it }
        availableTargetedTalent(observation, nearest, "ritual_break")?.let { return it }
        availableTargetedTalent(observation, nearest, "radiant_lance")?.let { return it }
        availableTargetedTalent(observation, nearest, "glacial_seal")?.let { return it }
        availableTargetedTalent(observation, nearest, "void_breach")?.let { return it }
        availableTargetedTalent(observation, nearest, "linebreaker")?.let { return it }
        availableTargetedTalent(observation, nearest, "riven_edge")?.let { return it }
        availableTargetedTalent(observation, nearest, "runic_edge")?.let { return it }
        availableTargetedTalent(observation, nearest, "sunder_sigil")?.let { return it }
        availableTargetedTalent(observation, nearest, "crippling_strike")?.let { return it }
        availableTargetedTalent(observation, nearest, "eviscerate")?.let { return it }
        availableTargetedTalent(observation, nearest, "cinder_burst")?.let { return it }
        availableTargetedTalent(observation, nearest, "arcane_edge")?.let { return it }
        availableTargetedTalent(observation, nearest, "savage_hew")?.let { return it }
        availableTargetedTalent(observation, nearest, "spell_rend")?.let { return it }
        availableTargetedTalent(observation, nearest, "blink_strike")?.let { return it }
        availableTargetedTalent(observation, nearest, "mana_lunge")?.let { return it }
        availableTargetedTalent(observation, nearest, "shadowstep")?.let { return it }
        availableTargetedTalent(observation, nearest, "judgment_hammer")?.let { return it }
        availableTargetedTalent(observation, nearest, "deathblow")?.let { return it }
        availableTargetedTalent(observation, nearest, "poison_blade")?.let { return it }
        availableTargetedTalent(observation, nearest, "backstab")?.let { return it }
        availableTargetedTalent(observation, nearest, "holy_strike")?.let { return it }
        availableTargetedTalent(observation, nearest, "ice_prison")?.let { return it }
        availableTargetedTalent(observation, nearest, "shield_bash")?.let { return it }
        availableTargetedTalent(observation, nearest, "sunder_armor")?.let { return it }
        availableTargetedTalent(observation, nearest, "charge")?.let { return it }
        availableTargetedTalent(observation, nearest, "power_strike")?.let { return it }
        availableTargetedTalent(observation, nearest, "rupture_wave")?.let { return it }
        if (nearbyHostiles >= 2) {
            clusterTarget(observation)?.let { cluster ->
                availableTalent(observation, "inferno_orb")
                    ?.takeIf { slot -> cluster.isWithin(slot, observation.playerPosition) }
                    ?.let { slot -> return PlayerCommand.UseTalent(slot.slot, cluster) }
                availableTalent(observation, "shard_storm")
                    ?.takeIf { slot -> cluster.isWithin(slot, observation.playerPosition) }
                    ?.let { slot -> return PlayerCommand.UseTalent(slot.slot, cluster) }
                availableTalent(observation, "ricochet_knives")
                    ?.takeIf { slot -> cluster.isWithin(slot, observation.playerPosition) }
                    ?.let { slot -> return PlayerCommand.UseTalent(slot.slot, cluster) }
                availableTalent(observation, "flame_wall")
                    ?.takeIf { slot -> cluster.isWithin(slot, observation.playerPosition) }
                    ?.let { slot -> return PlayerCommand.UseTalent(slot.slot, cluster) }
            }
        }
        availableTargetedTalent(observation, nearest, "fireball")?.let { return it }
        availableTargetedTalent(observation, nearest, "ice_bolt")?.let { return it }
        return null
    }

    private fun availableTargetedTalent(
        observation: RunObservation,
        target: Point,
        talentId: String,
    ): PlayerCommand? {
        val slot = availableTalent(observation, talentId) ?: return null
        if (target == observation.playerPosition) {
            return null
        }
        if (!target.isWithin(slot, observation.playerPosition)) {
            return null
        }
        return PlayerCommand.UseTalent(slot.slot, target)
    }

    private fun pursueVisibleHostile(observation: RunObservation): PlayerCommand? {
        val playerPosition = observation.playerPosition
        val target =
            observation.visibleHostilePositions
                .minByOrNull { hostile -> hostile.chebyshevDistanceTo(playerPosition) }
                ?: return null
        if (target.chebyshevDistanceTo(playerPosition) <= 1) {
            return PlayerCommand.Move(target.deltaFrom(playerPosition))
        }
        observation.visibleHostilePositions
            .sortedWith(compareBy<Point> { hostile -> hostile.chebyshevDistanceTo(playerPosition) }.thenBy(Point::y).thenBy(Point::x))
            .forEach { hostile ->
                val nextStep = stepToward(observation, hostile) ?: return@forEach
                return PlayerCommand.Move(nextStep.deltaFrom(playerPosition))
            }
        val directStep = target.deltaFrom(playerPosition)
        val directPoint = playerPosition + directStep
        if (observation.map.isInBounds(directPoint.x, directPoint.y) && !observation.map.blocksMovement(directPoint.x, directPoint.y)) {
            return PlayerCommand.Move(directStep)
        }
        return PlayerCommand.Wait
    }

    private fun availableTalent(
        observation: RunObservation,
        talentId: String,
    ) =
        observation.talentSlots.firstOrNull { slot ->
            slot.talentId == talentId &&
                slot.currentCooldown <= 0 &&
                slot.resourceTypeId == observation.playerResource.typeId &&
                slot.resourceCost <= observation.playerResource.current
        }

    private fun nearestHostile(observation: RunObservation): Point? =
        observation.visibleHostilePositions
            .filterNot { hostile -> hostile == observation.playerPosition }
            .minByOrNull { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) }

    private fun preferredOffensiveTarget(observation: RunObservation): Point? {
        val visibleHostiles = observation.visibleHostilePositions.filterNot { hostile -> hostile == observation.playerPosition }
        if (visibleHostiles.isEmpty()) {
            return null
        }
        if (isDensePackScenario(observation) && observation.playerResource.typeId in setOf("MANA", "POSITIVE_ENERGY", "HATE")) {
            return visibleHostiles.maxWithOrNull(
                compareBy<Point> { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) }
                    .thenByDescending { hostile -> hostile.y }
                    .thenByDescending { hostile -> hostile.x },
            )
        }
        return visibleHostiles.minByOrNull { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) }
    }

    private fun clusterTarget(observation: RunObservation): Point? =
        observation.visibleHostilePositions
            .filterNot { hostile -> hostile == observation.playerPosition }
            .maxWithOrNull(
            compareBy<Point> { center ->
                observation.visibleHostilePositions.count { hostile -> hostile != observation.playerPosition && hostile.chebyshevDistanceTo(center) <= 1 }
            }.thenByDescending { point -> point.chebyshevDistanceTo(observation.playerPosition) },
        )

    private fun hostilesWithin(
        observation: RunObservation,
        radius: Int,
    ): Int =
        observation.visibleHostilePositions.count { hostile ->
            hostile != observation.playerPosition && hostile.chebyshevDistanceTo(observation.playerPosition) <= radius
        }

    private fun retreatPoint(
        observation: RunObservation,
        slot: com.ktome.game.TalentSlotView,
    ): Point? =
        observation.visibleTiles
            .asSequence()
            .filter { point ->
                point != observation.playerPosition &&
                !observation.map.blocksMovement(point.x, point.y) &&
                    point !in observation.visibleBlockingPositions &&
                    point.isWithin(slot, observation.playerPosition)
            }.maxWithOrNull(
                compareBy<Point> { candidate ->
                    observation.visibleHostilePositions.minOfOrNull { hostile -> hostile.chebyshevDistanceTo(candidate) } ?: 0
                }.thenBy { candidate -> candidate.chebyshevDistanceTo(observation.playerPosition) },
            )

    private fun Point.isWithin(
        slot: com.ktome.game.TalentSlotView,
        origin: Point,
    ): Boolean {
        val distance = chebyshevDistanceTo(origin)
        return distance in slot.minRange..slot.range
    }
}

private val BERSERKER_TALENT_IDS: Set<String> =
    setOf(
        "blood_rush",
        "pursuit_drive",
        "savage_hew",
        "riven_edge",
        "reckless_slam",
        "rupture_wave",
        "fault_line",
        "aftershock",
        "kill_frenzy",
        "pain_fuel",
        "slaughter_drive",
        "last_stand",
    )

private val SPELLBLADE_TALENT_IDS: Set<String> =
    setOf(
        "arcane_edge",
        "runic_edge",
        "sunder_sigil",
        "spell_rend",
        "flux_anchor",
        "balance_point",
        "flux_reversal",
        "flux_burst",
        "mana_lunge",
        "blink_strike",
        "spell_parry",
        "counter_seal",
    )
