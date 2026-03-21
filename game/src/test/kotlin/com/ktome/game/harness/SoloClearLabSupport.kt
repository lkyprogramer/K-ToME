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
import com.ktome.core.item.ItemQuality
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
import com.ktome.core.talent.TalentRegistry
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameContent
import com.ktome.game.PlayerCommand
import com.ktome.game.PlayerResourcePools
import com.ktome.game.SessionSnapshotMapper
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

internal const val SOLO_CLEAR_SCRIPT_VERSION: String = "solo-clear-lab-v2"

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
        summary = "20x20 boss room, kill bandit captain, observe warning/telegraph",
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
    val sawBossWarning: Boolean,
    val sawTalentTelegraph: Boolean,
    val commandTrace: List<String>,
    val resourceTimeline: List<Int>,
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
            put("sawBossWarning", sawBossWarning)
            put("sawTalentTelegraph", sawTalentTelegraph)
            putJsonArray("commandTrace") { commandTrace.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("resourceTimeline") { resourceTimeline.forEach { add(JsonPrimitive(it)) } }
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
        val bot = SoloClearScriptBot()
        val commandTrace = mutableListOf<String>()
        val resourceTimeline = mutableListOf<Int>()
        var sawBossWarning = false
        var sawTalentTelegraph = false
        var sawResourceRestoreLog = false
        var failureReason: String? = null
        var turnCount = 0

        while (turnCount < scenario.maxTurns && !session.runOutcome().isTerminal && !goalReached(session, runtime)) {
            val snapshot = session.renderSnapshot()
            sawBossWarning = sawBossWarning || snapshot.overlays.any { it.sourceAbilityId == "bandit_captain_encounter" }
            sawTalentTelegraph = sawTalentTelegraph || snapshot.overlays.any { it.id.startsWith("telegraph:") }
            sawResourceRestoreLog =
                sawResourceRestoreLog || snapshot.logEvents.any { event -> event.message.key == "log.talent.resource_restore" }

            val observation = RunObservationCapture.capture(session, turnCount)
            resourceTimeline += observation.playerResource.current
            val command =
                bot.decide(observation) ?: run {
                    failureReason = "Script bot returned no command."
                    break
                }
            commandTrace += renderCommand(command)
            if (!session.perform(command)) {
                failureReason = "Command rejected: ${renderCommand(command)}"
                break
            }
            if (command.consumesTurn()) {
                turnCount += 1
            }
        }

        if (failureReason == null && goalReached(session, runtime) && professionId == "templar" && scenario == SoloClearScenario.MOB_PACK) {
            repeat(3) {
                val observation = RunObservationCapture.capture(session, turnCount)
                resourceTimeline += observation.playerResource.current
                commandTrace += renderCommand(PlayerCommand.Wait)
                if (session.perform(PlayerCommand.Wait)) {
                    turnCount += 1
                }
            }
        }

        val finalObservation = RunObservationCapture.capture(session, turnCount)
        resourceTimeline += finalObservation.playerResource.current
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
            sawBossWarning = sawBossWarning,
            sawTalentTelegraph = sawTalentTelegraph,
            commandTrace = commandTrace,
            resourceTimeline = resourceTimeline,
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
        installBlueGear(world, playerId, professionId)
        PlayerResourcePools.ensureInitialized(world, playerId, profession)
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
                if (!sawBossWarning || !sawTalentTelegraph) {
                    return "Boss scenario did not expose both warning and telegraph overlays."
                }
            }
        }

        if (runtime.scenario == SoloClearScenario.MOB_PACK && commandTrace.none { command -> command.startsWith("UseTalent(") }) {
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
            if (peak <= runtime.initialResource) {
                return "Templar POSITIVE_ENERGY never increased during combat."
            }
            val peakIndex = resourceTimeline.indexOfFirst { value -> value == peak }
            val decayedAfterPeak = resourceTimeline.drop(peakIndex + 1).any { value -> value < peak }
            if (!decayedAfterPeak) {
                return "Templar POSITIVE_ENERGY did not decay after combat ended."
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
            listOf(
                blueWeapon(professionId),
                blueArmor(professionId),
                healingPotion(),
                healingPotion(),
            )
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
            "arcanist" ->
                ItemInstance(
                    baseId = "arcane_staff",
                    name = "Solo Lab Blue Focus",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#4B6DFF",
                    quality = ItemQuality.RARE,
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
                    quality = ItemQuality.RARE,
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
                    quality = ItemQuality.RARE,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    stats = StatModifier(str = 3, wil = 1, attack = 11, accuracy = 4),
                )
        }

    private fun blueArmor(professionId: String): ItemInstance =
        ItemInstance(
            baseId = "chain_mail",
            name = "Solo Lab Blue Armor",
            type = ItemType.ARMOR,
            slot = EquipSlot.ARMOR,
            glyph = '[',
            colorHex = "#4B6DFF",
            quality = ItemQuality.RARE,
            stats =
                when (professionId) {
                    "rogue" -> StatModifier(defense = 9, maxHp = 26, dex = 2, speed = 5)
                    "arcanist" -> StatModifier(defense = 10, maxHp = 42, wil = 3)
                    else -> StatModifier(defense = 11, maxHp = 30, con = 2)
                },
        )

    private fun healingPotion(): ItemInstance =
        ItemInstance(
            baseId = "healing_potion",
            name = "Greater Healing Potion",
            type = ItemType.CONSUMABLE,
            glyph = '!',
            colorHex = "#FF0000",
            quality = ItemQuality.MAGIC,
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
            "rogue" -> pools.pool(ResourceType.ENERGY)?.syncTo(nextCurrent = 60, nextMax = 100)
            "templar" -> pools.pool(ResourceType.POSITIVE_ENERGY)?.syncTo(nextCurrent = 0, nextMax = 100)
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
            SoloClearScenario.BOSS -> Point(4, 10)
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
            talentRegistry = TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinitions = loader.loadBossDefinitions(),
            schemaCatalog = schemaCatalog,
            localizer = loader.localizer,
        )
    }

    private fun renderCommand(command: PlayerCommand): String =
        when (command) {
            is PlayerCommand.Move -> "Move(${command.delta.x},${command.delta.y})"
            PlayerCommand.Wait -> "Wait"
            PlayerCommand.PickUp -> "PickUp"
            PlayerCommand.Interact -> "Interact"
            PlayerCommand.Ascend -> "Ascend"
            PlayerCommand.Descend -> "Descend"
            PlayerCommand.SaveGame -> "SaveGame"
            is PlayerCommand.ActivateInventoryItem -> "ActivateInventoryItem(${command.index})"
            is PlayerCommand.UseTalent ->
                command.target?.let { target -> "UseTalent(${command.slot},${target.x},${target.y})" } ?: "UseTalent(${command.slot})"
            is PlayerCommand.AssignStat -> "AssignStat(${command.stat})"
            is PlayerCommand.AssignTalent -> "AssignTalent(${command.slot})"
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
                appendLine(
                    "- profession=${report.professionId}, scenario=${report.scenarioId}, success=${report.success}, turns=${report.turns}, hp=${report.currentHp}/${report.maxHp}, resource=${report.resourceTypeId}:${report.finalResource}, outcome=${report.outcome}",
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

private class SoloClearScriptBot : RunBot {
    override fun decide(observation: RunObservation): PlayerCommand? {
        lootFollowUp(observation)?.let { return it }
        useEmergencyConsumable(observation)?.let { return it }
        useEmergencyTalent(observation)?.let { return it }
        useOffensiveTalent(observation)?.let { return it }
        pursueVisibleHostile(observation)?.let { return it }
        return PlayerCommand.Wait
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
        return null
    }

    private fun useOffensiveTalent(observation: RunObservation): PlayerCommand? {
        val nearest = nearestHostile(observation) ?: return null
        val adjacentHostiles = hostilesWithin(observation, 1)
        val nearbyHostiles = hostilesWithin(observation, 2)
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 70

        if (adjacentHostiles >= 2) {
            availableTalent(observation, "blade_flurry")?.let { return PlayerCommand.UseTalent(it.slot, nearest) }
            availableTalent(observation, "holy_aura")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "sweeping_strike")?.let { return PlayerCommand.UseTalent(it.slot, nearest) }
            availableTalent(observation, "frost_nova")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (nearbyHostiles >= 2) {
            availableTalent(observation, "smoke_bomb")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "war_cry")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "intimidation")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (observation.playerResource.typeId == "MANA" && observation.playerResource.current * 100 <= observation.playerResource.max * 40) {
            availableTalent(observation, "mana_surge")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (lowHealth) {
            availableTalent(observation, "holy_light")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        if (nearbyHostiles >= 1) {
            availableTalent(observation, "devotion")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "holy_shield")?.let { return PlayerCommand.UseTalent(it.slot) }
            availableTalent(observation, "stealth")?.let { return PlayerCommand.UseTalent(it.slot) }
        }
        availableTargetedTalent(observation, nearest, "shadowstep")?.let { return it }
        availableTargetedTalent(observation, nearest, "judgment_hammer")?.let { return it }
        availableTargetedTalent(observation, nearest, "deathblow")?.let { return it }
        availableTargetedTalent(observation, nearest, "poison_blade")?.let { return it }
        availableTargetedTalent(observation, nearest, "backstab")?.let { return it }
        availableTargetedTalent(observation, nearest, "holy_strike")?.let { return it }
        availableTargetedTalent(observation, nearest, "ice_prison")?.let { return it }
        availableTargetedTalent(observation, nearest, "shield_bash")?.let { return it }
        availableTargetedTalent(observation, nearest, "sunder_armor")?.let { return it }
        availableTargetedTalent(observation, nearest, "power_strike")?.let { return it }
        if (nearbyHostiles >= 2) {
            clusterTarget(observation)?.let { cluster ->
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
        observation.visibleHostilePositions.minByOrNull { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) }

    private fun clusterTarget(observation: RunObservation): Point? =
        observation.visibleHostilePositions.maxWithOrNull(
            compareBy<Point> { center ->
                observation.visibleHostilePositions.count { hostile -> hostile.chebyshevDistanceTo(center) <= 1 }
            }.thenByDescending { point -> point.chebyshevDistanceTo(observation.playerPosition) },
        )

    private fun hostilesWithin(
        observation: RunObservation,
        radius: Int,
    ): Int = observation.visibleHostilePositions.count { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) <= radius }

    private fun retreatPoint(
        observation: RunObservation,
        slot: com.ktome.game.TalentSlotView,
    ): Point? =
        observation.visibleTiles
            .asSequence()
            .filter { point ->
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
