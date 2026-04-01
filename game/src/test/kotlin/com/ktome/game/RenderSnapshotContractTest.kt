package com.ktome.game

import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.item.AffixDef
import com.ktome.core.item.AffixType
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemQuality
import com.ktome.core.item.ItemType
import com.ktome.core.item.StatModifier
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.RenderSnapshotHasher
import com.ktome.core.stats.StatsCalculator
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RenderSnapshotContractTest {
    @TempDir
    lateinit var tempDir: Path

    private val schemaCatalog = DataLoader().loadSchemaCatalog()

    @Test
    fun `same seed and locale produce stable initial render snapshot hash`() {
        val left =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("left")),
            )
        val right =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("right")),
            )

        assertEquals(
            RenderSnapshotHasher.sha256(left.renderSnapshot()),
            RenderSnapshotHasher.sha256(right.renderSnapshot()),
        )
    }

    @Test
    fun `initial snapshot exposes phase2 render contract fields`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager = SaveManager(tempDir.resolve("single")),
            )

        val snapshot = session.renderSnapshot()

        assertFalse(snapshot.mapCells.isEmpty())
        assertFalse(snapshot.actors.isEmpty())
        assertTrue(snapshot.uiState.equipment.size >= 2)
        assertTrue(snapshot.logEvents.isNotEmpty())
        assertEquals("shattered_outpost", snapshot.metadata.zoneId)
        assertEquals("zone.shattered_outpost.name", snapshot.metadata.zoneNameKey)
        assertEquals("zone.shattered_outpost.desc", snapshot.metadata.zoneDescKey)
        assertEquals("audio.zone.shattered_outpost", snapshot.metadata.zoneAudioProfile)
        assertEquals("ambient.shattered_outpost", snapshot.metadata.ambientProfile)
        assertEquals("tileset.ruins", snapshot.metadata.tilesetKey)
        assertEquals("ui.hud.mana.short", snapshot.uiState.playerStatus.resourceLabelKey)
        assertEquals("MANA", snapshot.uiState.playerStatus.resourceTypeId)
        assertEquals(session.playerResourceView().current, snapshot.uiState.playerStatus.currentResource)
        assertEquals(session.playerResourceView().max, snapshot.uiState.playerStatus.maxResource)
        assertTrue(snapshot.props.any { prop -> prop.propTypeId == "supply_crate" })
        assertTrue(snapshot.props.any { prop -> prop.propTypeId == "alarm_bonfire" })
        assertTrue(snapshot.combatFeedbackEvents.isEmpty())
        val zoneEnter = requireNotNull(snapshot.logEvents.firstOrNull { event -> event.message.key == "log.zone.enter" })
        assertEquals("zone.shattered_outpost.name", zoneEnter.message.arguments.first { argument -> argument.name == "zone" }.valueKey)
        assertEquals("zone.shattered_outpost.desc", zoneEnter.message.arguments.first { argument -> argument.name == "desc" }.valueKey)
        assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.objective.activate" })
        assertTrue(snapshot.actors.all { actor -> actor.nameKey.isNotBlank() })
        assertTrue(snapshot.logEvents.all { event -> event.message.key.isNotBlank() })
    }

    @Test
    fun `render snapshot separates active and reserve talents after loadout remap`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("loadout-snapshot")),
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "snapshot_dummy", expReward = 1500)
        val dummyPoint = requireNotNull(session.automationWorld().get<Position>(dummyId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(dummyPoint - session.playerPosition())))

        val leveledSnapshot = session.renderSnapshot()

        assertEquals(listOf(1, 2, 3, 4), leveledSnapshot.uiState.talents.map { talent -> talent.slot })
        assertTrue(leveledSnapshot.uiState.talents.none { talent -> talent.talentId == "charge" })
        assertTrue(leveledSnapshot.uiState.reserveTalents.any { talent -> talent.talentId == "charge" && talent.descKey != null })
        assertTrue(
            leveledSnapshot.uiState.reserveTalents.none { reserve ->
                leveledSnapshot.uiState.talents.any { active -> active.talentId == reserve.talentId }
            },
        )

        assertTrue(session.perform(PlayerCommand.EquipTalentToSlot(slot = 4, talentId = "charge")))
        val remappedSnapshot = session.renderSnapshot()

        assertEquals("charge", remappedSnapshot.uiState.talents.first { talent -> talent.slot == 4 }.talentId)
        assertFalse(remappedSnapshot.uiState.reserveTalents.any { talent -> talent.talentId == "charge" })
        assertTrue(remappedSnapshot.uiState.reserveTalents.any { talent -> talent.talentId == "war_cry" })
    }

    @Test
    fun `item render snapshots expose quality affix material and passive semantics for detail panes`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "greenwood_fringe", playerProfessionId = "rogue"),
                saveManager = SaveManager(tempDir.resolve("item-render-contract")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(playerId))
        val itemFactory = ItemFactory()

        inventory.itemIds +=
            itemFactory.createCarriedItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "battle_axe",
                        name = "Mithril Battle Axe of Speed",
                        type = ItemType.WEAPON,
                        slot = com.ktome.core.item.EquipSlot.WEAPON,
                        glyph = ')',
                        colorHex = "#C0C0C0",
                        quality = ItemQuality.RARE,
                        materialId = "MITHRIL",
                        materialName = "Mithril",
                        affixes = listOf(AffixDef(id = "of_speed", name = "of Speed", type = AffixType.SUFFIX, statModifiers = StatModifier(speed = 15))),
                        stats = StatModifier(attack = 9, speed = 15),
                    ),
            )
        inventory.itemIds +=
            itemFactory.createCarriedItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "bandit_trophy",
                        name = "Bandit Trophy",
                        type = ItemType.ARMOR,
                        slot = com.ktome.core.item.EquipSlot.OFF_HAND,
                        glyph = ']',
                        colorHex = "#8A7148",
                        quality = ItemQuality.COMMON,
                        stats = StatModifier(dex = 1),
                        passive = EquipmentPassive.DamageVsTag("bandit", 0.15),
                    ),
            )
        inventory.itemIds +=
            itemFactory.createCarriedItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "long_sword",
                        name = "Long Sword",
                        type = ItemType.WEAPON,
                        slot = com.ktome.core.item.EquipSlot.WEAPON,
                        glyph = ')',
                        colorHex = "#C0C0C0",
                        quality = ItemQuality.COMMON,
                        stats = StatModifier(attack = 8),
                        passive = EquipmentPassive.DamageVsTag("undead", 0.10),
                    ),
            )

        val snapshot = session.renderSnapshot()
        val rareWeapon = snapshot.uiState.inventory.first { entry -> entry.item.baseItemId == "battle_axe" }.item
        val passiveReward = snapshot.uiState.inventory.first { entry -> entry.item.baseItemId == "bandit_trophy" }.item
        val undeadSlayer = snapshot.uiState.inventory.first { entry -> entry.item.baseItemId == "long_sword" }.item
        val rareWeaponDisplayName = requireNotNull(rareWeapon.displayName)

        assertEquals("item.quality.rare", rareWeapon.qualityNameKey)
        assertEquals("material.mithril.name", rareWeapon.materialNameKey)
        assertEquals(listOf("affix.of_speed.name"), rareWeapon.affixNameKeys)
        assertEquals("item.display.composed", rareWeaponDisplayName.key)
        assertEquals(
            "item.battle_axe.name",
            rareWeaponDisplayName.arguments.first { argument -> argument.name == "base" }.valueKey,
        )
        assertEquals(
            "item.display.part.material",
            requireNotNull(rareWeaponDisplayName.arguments.first { argument -> argument.name == "material" }.valueToken).key,
        )
        assertEquals("ui.inspect.passive.damage_vs_tag", passiveReward.passiveDescriptions.single().key)
        assertEquals(
            "monster.tag.bandit",
            passiveReward.passiveDescriptions.single().arguments.first { argument -> argument.name == "tag" }.valueKey,
        )
        assertEquals("ui.inspect.passive.damage_vs_tag", undeadSlayer.passiveDescriptions.single().key)
        assertEquals(
            "monster.tag.undead",
            undeadSlayer.passiveDescriptions.single().arguments.first { argument -> argument.name == "tag" }.valueKey,
        )
    }

    @Test
    fun `render snapshot exposes visible terrain tags without reconstructing map semantics in client`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 2026040103L, zoneId = "underground_river", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("terrain-tag-render")),
            )
        val taggedPoint = requireNotNull(session.automationTerrainTags().keys.sortedWith(compareBy<Point>(Point::y).thenBy(Point::x)).firstOrNull())

        session.automationMovePlayerTo(taggedPoint)
        val snapshot = session.renderSnapshot()
        val cell = snapshot.mapCells.first { mapCell -> mapCell.x == taggedPoint.x && mapCell.y == taggedPoint.y }

        assertEquals(
            session.automationTerrainTags().getValue(taggedPoint).map { tag -> tag.name }.sorted(),
            cell.terrainTags,
        )
        assertTrue(cell.visibility != CellVisibilitySnapshot.HIDDEN)
    }

    @Test
    fun `arcanist mana cap follows runtime wil after stat recalculation`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager = SaveManager(tempDir.resolve("arcanist-runtime-resource")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val profession = profession("arcanist")
        val stats = requireNotNull(world.get<Stats>(playerId))
        val baseline = session.playerResourceView()

        stats.wil += 2
        StatsCalculator.recalculateAndStore(world, playerId)

        val pools = PlayerResourceService.sync(world, playerId, profession)
        val mana = requireNotNull(pools.pool(ResourceType.MANA))

        assertEquals(baseline.max + 12, mana.max)
        assertEquals(baseline.current + 12, mana.current)
        assertEquals(mana.max, session.renderSnapshot().uiState.playerStatus.maxResource)
    }

    @Test
    fun `rogue energy resource restores on hit`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "rogue"),
                saveManager = SaveManager(tempDir.resolve("rogue-energy")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val profession = profession("rogue")
        val pools = PlayerResourceService.sync(world, playerId, profession)
        val energy = requireNotNull(pools.pool(ResourceType.ENERGY))

        energy.spend(20)
        PlayerResourceService.onSuccessfulHit(world, playerId, profession)

        assertEquals(88, energy.current)
        assertEquals(100, session.renderSnapshot().uiState.playerStatus.maxResource)
        assertEquals(88, session.renderSnapshot().uiState.playerStatus.currentResource)
    }

    @Test
    fun `templar positive energy reacts to combat gain and out of combat decay`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("templar-resource")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val profession = profession("templar")
        val pools = PlayerResourceService.sync(world, playerId, profession)
        val positive = requireNotNull(pools.pool(ResourceType.POSITIVE_ENERGY))

        val status = session.renderSnapshot().uiState.playerStatus

        assertEquals("POSITIVE_ENERGY", status.resourceTypeId)
        assertEquals("ui.hud.positive_energy.short", status.resourceLabelKey)
        assertEquals(32, status.currentResource)
        assertEquals(100, status.maxResource)

        PlayerResourceService.onDamageTaken(world, playerId, profession, damage = 20)
        PlayerResourceService.onSuccessfulHit(world, playerId, profession)
        PlayerResourceService.onTurnStart(world, playerId, profession, inCombat = false)
        session.automationMovePlayerTo(session.playerPosition())

        assertEquals(33, positive.current)
        assertEquals(33, session.renderSnapshot().uiState.playerStatus.currentResource)
    }

    @Test
    fun `vanguard talent spend keeps stamina pool component and snapshot aligned`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("vanguard-stamina-resource")),
            )
        val world = session.automationWorld()
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
        val dummyId =
            EntityFactory().createMonster(
                world = world,
                template = dummyTemplate(),
                position = com.ktome.core.map.Point(session.playerPosition().x + 1, session.playerPosition().y),
            )
        val dummyPoint = requireNotNull(world.get<Position>(dummyId)).toPoint()
        val powerStrikeSlot = session.talentSlots().first { slot -> slot.talentId == "power_strike" }.slot

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = powerStrikeSlot, target = dummyPoint)))

        val staminaPool = requireNotNull(requireNotNull(world.get<com.ktome.core.resource.ResourcePools>(session.playerId)).pool(ResourceType.STAMINA))
        val resource = session.playerResourceView()
        val snapshotStatus = session.renderSnapshot().uiState.playerStatus

        assertEquals(staminaPool.current, resource.current)
        assertEquals(staminaPool.max, resource.max)
        assertEquals(staminaPool.current, snapshotStatus.currentResource)
        assertEquals(staminaPool.max, snapshotStatus.maxResource)
    }

    @Test
    fun `explored cells do not expose hidden actor ids`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("fog-contract")),
            )
        val world = session.automationWorld()
        val origin = session.playerPosition()
        val hiddenMonsterId =
            world.entitiesWith(Position::class, MonsterTemplateId::class)
                .first { entityId ->
                    requireNotNull(world.get<Position>(entityId)).toPoint() !in session.visibleTiles()
                }
        val monsterPoint = requireNotNull(world.get<Position>(hiddenMonsterId)).toPoint()

        session.automationMovePlayerTo(monsterPoint)
        assertTrue(monsterPoint in session.visibleTiles())

        session.automationMovePlayerTo(origin)
        val snapshot = session.renderSnapshot()
        val cell = snapshot.mapCells.single { mapCell -> mapCell.x == monsterPoint.x && mapCell.y == monsterPoint.y }

        assertEquals(CellVisibilitySnapshot.EXPLORED, cell.visibility)
        assertNull(cell.actorEntityId)
        assertTrue(snapshot.actors.none { actor -> actor.entityId == hiddenMonsterId.value })
    }

    @Test
    fun `boss floor telegraph follows the next usable boss talent intent`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "grey_gate_depths", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("boss-warning")),
            )
        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))

        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossId = requireNotNull(session.automationEntityByTemplateId(FOUNDATION_BOSS_TEMPLATE_ID))
        val bossPoint = requireNotNull(session.automationWorld().get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))
        requireNotNull(session.automationWorld().get<com.ktome.core.talent.EffectTracker>(bossId)).effects.removeIf { effect ->
            effect.schemaId == "war_cry_empower"
        }
        session.automationWorld().remove<com.ktome.core.ai.PendingTelegraphState>(bossId)
        requireNotNull(session.automationWorld().get<com.ktome.core.talent.CooldownState>(bossId)).remainingByTalentId.apply {
            this["battlefield_command"] = 0
            this["shadow_bind"] = 99
            this["ritual_break"] = 99
        }

        val initialSnapshot = session.renderSnapshot()
        val overlay = requireNotNull(initialSnapshot.overlays.singleOrNull { candidate -> candidate.id == "boss-warning:${bossId.value}" })
        assertEquals("vfx.boss.warning.sigil_01", overlay.visualKey)
        assertEquals("log.warning.boss_presence", overlay.warningMessage?.key)
        assertEquals(1, overlay.previewTurns)
        assertEquals(3, overlay.dangerLevel)
        assertEquals(OverlayShapeSnapshot.SINGLE_TILE, overlay.shape)
        assertTrue(overlay.cells.any { cell -> cell.x == bossPoint.x && cell.y == bossPoint.y })
        assertNull(initialSnapshot.overlays.firstOrNull { candidate -> candidate.id.startsWith("telegraph:${bossId.value}:") })

        var telegraphSnapshot = session.renderSnapshot()
        var initialTelegraph =
            telegraphSnapshot.overlays.singleOrNull { candidate ->
                candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "battlefield_command"
            }
        for (attempt in 0 until 5) {
            if (initialTelegraph != null) {
                break
            }
            assertTrue(session.perform(PlayerCommand.Wait))
            telegraphSnapshot = session.renderSnapshot()
            initialTelegraph =
                telegraphSnapshot.overlays.singleOrNull { candidate ->
                    candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "battlefield_command"
                }
        }
        initialTelegraph = requireNotNull(initialTelegraph)
        assertEquals("battlefield_command", session.recentAIDecisionTraces().last { trace -> trace.actorId == bossId.value }.selectedActionId)
        assertEquals("log.warning.telegraph", initialTelegraph.warningMessage?.key)
        assertEquals(OverlayShapeSnapshot.RING, initialTelegraph.shape)
        assertEquals(1, initialTelegraph.previewTurns)
        assertTrue(initialTelegraph.cells.any { cell -> cell.x == bossPoint.x && cell.y == bossPoint.y })

        assertTrue(session.perform(PlayerCommand.Wait))
        assertNull(session.renderSnapshot().overlays.firstOrNull { candidate -> candidate.id.startsWith("telegraph:${bossId.value}:") })

        requireNotNull(session.automationWorld().get<com.ktome.core.talent.CooldownState>(bossId)).remainingByTalentId.apply {
            this["battlefield_command"] = 99
            this["ritual_break"] = 0
        }

        var followUpSnapshot = session.renderSnapshot()
        var followUpTelegraph =
            followUpSnapshot.overlays.singleOrNull { candidate ->
                candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "ritual_break"
            }
        for (attempt in 0 until 6) {
            if (followUpTelegraph != null) {
                break
            }
            assertTrue(session.perform(PlayerCommand.Wait))
            followUpSnapshot = session.renderSnapshot()
            followUpTelegraph =
                followUpSnapshot.overlays.singleOrNull { candidate ->
                    candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "ritual_break"
                }
        }
        followUpTelegraph = requireNotNull(followUpTelegraph)
        assertEquals("ritual_break", session.recentAIDecisionTraces().last { trace -> trace.actorId == bossId.value }.selectedActionId)
        assertEquals("ritual_break", followUpTelegraph.sourceAbilityId)
        assertEquals(OverlayShapeSnapshot.RING, followUpTelegraph.shape)
        assertTrue(followUpTelegraph.previewTurns >= 1)
        assertTrue(followUpTelegraph.cells.isNotEmpty())
    }

    @Test
    fun `phase enter telegraph keeps line geometry instead of collapsing to the boss tile`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "grey_gate_depths", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("boss-phase-warning-geometry")),
            )
        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))
        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossId = requireNotNull(session.automationEntityByTemplateId(FOUNDATION_BOSS_TEMPLATE_ID))
        val world = session.automationWorld()
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(findOpenAdjacentPoint(session, bossPoint))
        val holyStrikeSlot = session.talentSlots().first { slot -> slot.talentId == "holy_strike" }.slot
        val bossHealth = requireNotNull(world.get<com.ktome.core.ecs.Health>(bossId))
        bossHealth.current = bossHealth.max / 2
        assertTrue(session.perform(PlayerCommand.UseTalent(slot = holyStrikeSlot, target = bossPoint)))

        val overlay =
            generateSequence(0) { turn -> turn + 1 }
                .map {
                assertTrue(session.perform(PlayerCommand.Wait))
                session.renderSnapshot().overlays.singleOrNull { candidate ->
                    candidate.id.startsWith("telegraph:${bossId.value}:dungeon_lord_phase_warning")
                }
                }.take(4)
                .firstOrNull()

        requireNotNull(overlay)
        assertEquals(OverlayShapeSnapshot.LINE, overlay.shape)
        assertTrue(overlay.cells.size > 1)
        assertTrue(overlay.cells.any { cell -> cell.x != bossPoint.x || cell.y != bossPoint.y })
    }

    @Test
    fun `descending into shattered outpost final floor exposes breach props and objective advance`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("objective-props")),
            )
        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))

        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        val snapshot = session.renderSnapshot()

        assertTrue(snapshot.props.any { prop -> prop.propTypeId == "armory_gate" })
        assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.objective.advance" })
    }

    @Test
    fun `player action advances render snapshot revision and hash`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("action-revision")),
            )

        val initial = session.renderSnapshot()
        assertTrue(session.perform(PlayerCommand.Wait))
        val updated = session.renderSnapshot()

        assertTrue(updated.metadata.revision > initial.metadata.revision)
        assertNotEquals(RenderSnapshotHasher.sha256(initial), RenderSnapshotHasher.sha256(updated))
    }

    @Test
    fun `loaded session save load round trip preserves render snapshot hash`() {
        val saveManager = SaveManager(tempDir.resolve("snapshot-roundtrip"))
        val original =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = saveManager,
            )
        repeat(2) {
            assertTrue(original.perform(PlayerCommand.Wait))
        }
        assertTrue(original.saveOnExit())

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val loadedSnapshot = loaded.renderSnapshot()
        assertTrue(loaded.saveOnExit())

        val reloaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val reloadedSnapshot = reloaded.renderSnapshot()

        assertEquals(RenderSnapshotHasher.sha256(loadedSnapshot), RenderSnapshotHasher.sha256(reloadedSnapshot))
    }

    @Test
    fun `same floor transition produces stable render snapshot hash`() {
        val left =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("transition-left")),
            )
        val right =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("transition-right")),
            )

        left.automationMovePlayerTo(requireNotNull(left.automationStairPoint(StairDirection.DOWN)))
        right.automationMovePlayerTo(requireNotNull(right.automationStairPoint(StairDirection.DOWN)))
        assertTrue(left.perform(PlayerCommand.Descend))
        assertTrue(right.perform(PlayerCommand.Descend))

        val leftSnapshot = left.renderSnapshot()
        val rightSnapshot = right.renderSnapshot()

        assertEquals(2, leftSnapshot.metadata.currentFloor)
        assertEquals(leftSnapshot.metadata.currentFloor, rightSnapshot.metadata.currentFloor)
        assertEquals(RenderSnapshotHasher.sha256(leftSnapshot), RenderSnapshotHasher.sha256(rightSnapshot))
    }

    private fun profession(id: String) =
        requireNotNull(schemaCatalog.professions.firstOrNull { profession -> profession.id == id }) {
            "Unknown profession '$id'."
        }

    private fun dummyTemplate(): com.ktome.game.model.MonsterTemplate =
        com.ktome.game.model.MonsterTemplate(
            id = "dummy",
            name = "Dummy",
            glyph = 'd',
            colorHex = "#AAAAAA",
            stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
            baseHp = 50,
            baseAttack = 1,
            baseDefense = 0,
            speed = 100,
            ai = com.ktome.core.ecs.AIType.CHASE,
            expReward = 1,
            spawnFloors = listOf(1),
            spawnWeight = 1,
        )

    private fun clearMonsters(session: FoundationGameSession) {
        val world = session.automationWorld()
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
    }

    private fun installExperienceDummy(
        session: FoundationGameSession,
        id: String,
        expReward: Int,
    ): com.ktome.core.ecs.EntityId =
        EntityFactory().createMonster(
            world = session.automationWorld(),
            template =
                com.ktome.game.model.MonsterTemplate(
                    id = id,
                    name = "Training Dummy",
                    glyph = 'd',
                    colorHex = "#AAAAAA",
                    stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
                    baseHp = 1,
                    baseAttack = 1,
                    baseDefense = 0,
                    speed = 90,
                    ai = com.ktome.core.ecs.AIType.CHASE,
                    expReward = expReward,
                    spawnFloors = listOf(session.currentFloor()),
                    spawnWeight = 1,
                ),
            position = findOpenAdjacentPoint(session),
        )

    private fun findOpenAdjacentPoint(session: FoundationGameSession): com.ktome.core.map.Point {
        val origin = session.playerPosition()
        val world = session.automationWorld()
        val occupied = world.entitiesWith(Position::class).mapTo(linkedSetOf()) { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return session.map.floorPoints()
            .filter { point -> point != origin && point.chebyshevDistanceTo(origin) == 1 && point !in occupied }
            .sortedWith(compareBy<com.ktome.core.map.Point>(com.ktome.core.map.Point::y).thenBy(com.ktome.core.map.Point::x))
            .first()
    }

    private fun findOpenAdjacentPoint(
        session: FoundationGameSession,
        center: com.ktome.core.map.Point,
    ): com.ktome.core.map.Point {
        val world = session.automationWorld()
        val occupied = world.entitiesWith(Position::class).mapTo(linkedSetOf()) { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return com.ktome.core.map.Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .first { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }
    }
}
