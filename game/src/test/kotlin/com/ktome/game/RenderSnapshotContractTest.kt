package com.ktome.game

import com.ktome.core.combat.DiminishingReturns
import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.item.AffixDef
import com.ktome.core.item.AffixType
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.ItemInstance
import com.ktome.core.loot.RarityTier
import com.ktome.core.item.ItemType
import com.ktome.core.item.StatModifier
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.FrontstageActionCategorySnapshot
import com.ktome.core.snapshot.FrontstageActionCueSnapshot
import com.ktome.core.snapshot.FrontstageActionPrioritySnapshot
import com.ktome.core.snapshot.FrontstageReadabilitySnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.RewardPresentationBuildIdentitySnapshot
import com.ktome.core.snapshot.RewardPresentationEntrySnapshot
import com.ktome.core.snapshot.RewardPresentationSourceSnapshot
import com.ktome.core.snapshot.RenderSnapshotHasher
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.stats.StatsCalculator
import com.ktome.game.data.DataLoader
import com.ktome.game.elites.BossVariantSelectionMode
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import java.nio.file.Path
import kotlinx.serialization.json.Json
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

    private val dataLoader = DataLoader()
    private val schemaCatalog = dataLoader.loadSchemaCatalog()
    private val itemBundle = dataLoader.loadItemBundle()

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
    fun `render snapshot ui state keeps frontstage readability and reward detail contract serializable`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("frontstage-serialization")),
            )

        val snapshot =
            session.renderSnapshot().copy(
                uiState =
                    session.renderSnapshot().uiState.copy(
                        recentRewards =
                            listOf(
                                RewardPresentationEntrySnapshot(
                                    source = RewardPresentationSourceSnapshot.SECRET_ZONE,
                                    sourceLabelKey = "ui.reward.source.secret_zone",
                                    itemDisplayName = RenderTextTokenSnapshot("tile.floor.name"),
                                    detailText =
                                        RenderTextTokenSnapshot(
                                            "ui.inspect.passive.hp_regen_turn",
                                            listOf(RenderTextArgumentSnapshot(name = "amount", value = "2")),
                                        ),
                                    buildIdentity =
                                        RewardPresentationBuildIdentitySnapshot(
                                            slotId = "OFF_HAND",
                                            slotLabelKey = "ui.reward.slot.off_hand",
                                            professionId = "arcanist",
                                            professionLabelKey = "profession.arcanist.name",
                                            scoreReason =
                                                RenderTextTokenSnapshot(
                                                    "ui.reward.identity.reason.non_weapon_capstone",
                                                    listOf(
                                                        RenderTextArgumentSnapshot(
                                                            name = "profession",
                                                            valueKey = "profession.arcanist.name",
                                                        ),
                                                        RenderTextArgumentSnapshot(name = "slot", valueKey = "ui.reward.slot.off_hand"),
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                        frontstageReadability =
                            FrontstageReadabilitySnapshot(
                                mutationHighlights =
                                    listOf(
                                        RenderTextTokenSnapshot(
                                            "ui.hud.frontstage.mutation_line",
                                            listOf(
                                                RenderTextArgumentSnapshot(name = "actor", valueKey = "actor.player.name"),
                                                RenderTextArgumentSnapshot(name = "mutation", valueKey = "status.stealth.name"),
                                                RenderTextArgumentSnapshot(
                                                    name = "summary",
                                                    valueToken = RenderTextTokenSnapshot("ui.inspect.mutation.summary.phase_runner"),
                                                ),
                                            ),
                                        ),
                                    ),
                                terrainHighlights = listOf(RenderTextTokenSnapshot("ui.hud.frontstage.terrain.water")),
                                recentActionCues =
                                    listOf(
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.SEARCH,
                                            priority = FrontstageActionPrioritySnapshot.MEDIUM,
                                            stableKey = "search:no_target",
                                            message = RenderTextTokenSnapshot("log.search.no_target"),
                                        ),
                                    ),
                            ),
                    ),
            )

        val encoded = Json.encodeToString(snapshot)
        val decoded = Json.decodeFromString<com.ktome.core.snapshot.RenderSnapshot>(encoded)

        assertEquals("ui.hud.frontstage.mutation_line", decoded.uiState.frontstageReadability.mutationHighlights.single().key)
        assertEquals("ui.inspect.passive.hp_regen_turn", decoded.uiState.recentRewards.single().detailText?.key)
        assertEquals("OFF_HAND", decoded.uiState.recentRewards.single().buildIdentity?.slotId)
        assertEquals("profession.arcanist.name", decoded.uiState.recentRewards.single().buildIdentity?.professionLabelKey)
        assertEquals("ui.reward.identity.reason.non_weapon_capstone", decoded.uiState.recentRewards.single().buildIdentity?.scoreReason?.key)
        val actionCue = decoded.uiState.frontstageReadability.recentActionCues.single()
        assertEquals(FrontstageActionCategorySnapshot.SEARCH, actionCue.category)
        assertEquals(FrontstageActionPrioritySnapshot.MEDIUM, actionCue.priority)
        assertEquals("search:no_target", actionCue.stableKey)
        assertEquals("log.search.no_target", actionCue.message.key)
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
    fun `render snapshot exposes formal cast speed status fields`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager = SaveManager(tempDir.resolve("cast-speed-status")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val equipment = requireNotNull(world.get<com.ktome.core.item.Equipment>(playerId))
        val itemId =
            ItemFactory().createCarriedItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "bandit_trophy",
                        name = "Bandit Trophy of Focus",
                        type = ItemType.ARMOR,
                        slot = com.ktome.core.item.EquipSlot.OFF_HAND,
                        glyph = ']',
                        colorHex = "#8A7148",
                        quality = RarityTier.MAGIC,
                        affixes =
                            listOf(
                                AffixDef(
                                    id = "of_focus",
                                    name = "of Focus",
                                    type = AffixType.SUFFIX,
                                    cost = 10,
                                    affixFamily = "suffix_cast_focus",
                                    statModifiers = StatModifier(castSpeedRating = 18),
                                ),
                            ),
                        stats = StatModifier(castSpeedRating = 18),
                    ),
            )
        equipment.slots[com.ktome.core.item.EquipSlot.OFF_HAND] = itemId
        StatsCalculator.recalculateAndStore(world, playerId)

        val snapshot = session.renderSnapshot()

        assertEquals(18, session.playerStatus().castSpeedRating)
        assertEquals(DiminishingReturns.effectiveCastSpeed(18), session.playerStatus().effectiveCastSpeed, 1e-6)
        assertEquals(18, snapshot.uiState.playerStatus.castSpeedRating)
        assertEquals(DiminishingReturns.effectiveCastSpeed(18), snapshot.uiState.playerStatus.effectiveCastSpeed, 1e-6)
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

        assertEquals(listOf(1, 2, 3), leveledSnapshot.uiState.talents.map { talent -> talent.slot })
        assertTrue(leveledSnapshot.uiState.talents.none { talent -> talent.talentId == "war_cry" })
        assertTrue(leveledSnapshot.uiState.reserveTalents.none { talent -> talent.talentId == "war_cry" })
        assertTrue(
            leveledSnapshot.uiState.talentTrees
                .flatMap { tree -> tree.nodes }
                .any { node -> node.talentId == "war_cry" && node.state == com.ktome.core.snapshot.TalentNodeStateSnapshot.LEARNABLE },
        )

        assertTrue(session.perform(PlayerCommand.AssignTalent("war_cry")))
        assertTrue(session.perform(PlayerCommand.ConfirmTalentDraft))
        val remappedSnapshot = session.renderSnapshot()

        assertEquals(listOf(1, 2, 3, 4), remappedSnapshot.uiState.talents.map { talent -> talent.slot })
        assertEquals("war_cry", remappedSnapshot.uiState.talents.first { talent -> talent.slot == 4 }.talentId)
        assertFalse(remappedSnapshot.uiState.reserveTalents.any { talent -> talent.talentId == "war_cry" })
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
                        quality = RarityTier.RARE,
                        materialId = "MITHRIL",
                        materialName = "Mithril",
                        affixes = listOf(AffixDef(id = "of_speed", name = "of Speed", type = AffixType.SUFFIX, cost = 6, affixFamily = "suffix_speed", statModifiers = StatModifier(speed = 15))),
                        stats = StatModifier(attack = 9, speed = 15),
                    ),
            )
        val damageVsTagCases =
            listOf(
                Triple("hunter_bow", "huntsbane", "bandit"),
                Triple("long_sword", "gravehunter", "undead"),
                Triple("battle_axe", "orcslayer", "orc"),
                Triple("short_sword", "of_iconoclasm", "cultist"),
                Triple("forgebreaker_pick", "forgehunter", "forge"),
                Triple("arcane_staff", "of_tidehunt", "river"),
                Triple("war_maul", "crystalrend", "crystal"),
                Triple("battle_axe", "of_abyssbane", "abyssal"),
            )
        damageVsTagCases.forEach { (baseId, affixId, _) ->
            inventory.itemIds +=
                itemFactory.createCarriedItem(
                    world = world,
                    item = affixItem(baseId = baseId, affixId = affixId),
                )
        }

        val snapshot = session.renderSnapshot()
        val rareWeapon =
            snapshot.uiState.inventory.first { entry ->
                entry.item.baseItemId == "battle_axe" && "affix.of_speed.name" in entry.item.affixNameKeys
            }.item
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
        damageVsTagCases.forEach { (baseId, affixId, tag) ->
            val renderedItem =
                snapshot.uiState.inventory.first { entry ->
                    entry.item.baseItemId == baseId && "affix.$affixId.name" in entry.item.affixNameKeys
                }.item
            val damageVsTagPassive =
                renderedItem.passiveDescriptions.firstOrNull { passive ->
                    passive.key == "ui.inspect.passive.damage_vs_tag" &&
                        passive.arguments.firstOrNull { argument -> argument.name == "tag" }?.valueKey == "monster.tag.$tag"
                }
            assertTrue(damageVsTagPassive != null, "Expected $baseId/$affixId to expose a localized DamageVsTag passive for $tag.")
        }
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
    fun `render snapshot exposes opt pr03 passive inspect tokens and special template presentation`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 2026040912L, zoneId = "underground_river", playerProfessionId = "rogue"),
                saveManager = SaveManager(tempDir.resolve("opt-pr03-inspect-render")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(playerId))
        val itemFactory = ItemFactory()

        inventory.itemIds += itemFactory.createCarriedItem(world = world, item = affixItem(baseId = "long_sword", affixId = "briarhook"))
        inventory.itemIds += itemFactory.createCarriedItem(world = world, item = affixItem(baseId = "bandit_trophy", affixId = "floodtouched"))
        inventory.itemIds += itemFactory.createCarriedItem(world = world, item = specialItem("unique.thornpath_crook"))
        inventory.itemIds += itemFactory.createCarriedItem(world = world, item = specialItem("unique.deepcurrent_lens"))
        inventory.itemIds += itemFactory.createCarriedItem(world = world, item = specialItem("unique.cinderveil_plate"))
        inventory.itemIds += itemFactory.createCarriedItem(world = world, item = specialItem("artifact.heartroot_gambit"))

        val snapshot = session.renderSnapshot()
        val onHitItem =
            snapshot.uiState.inventory.first { entry ->
                entry.item.baseItemId == "long_sword" && "affix.briarhook.name" in entry.item.affixNameKeys
            }.item
        val terrainItem =
            snapshot.uiState.inventory.first { entry ->
                entry.item.baseItemId == "bandit_trophy" && "affix.floodtouched.name" in entry.item.affixNameKeys
            }.item
        val onKillItem =
            snapshot.uiState.inventory.first { entry ->
                entry.item.baseItemId == "unique_deepcurrent_lens"
            }.item
        val conditionalItem =
            snapshot.uiState.inventory.first { entry ->
                entry.item.baseItemId == "unique_cinderveil_plate"
            }.item
        val uniqueItem =
            snapshot.uiState.inventory.first { entry ->
                entry.item.baseItemId == "unique_thornpath_crook"
            }.item
        val artifactItem =
            snapshot.uiState.inventory.first { entry ->
                entry.item.baseItemId == "artifact_heartroot_gambit"
            }.item

        assertTrue(onHitItem.passiveDescriptions.any { passive -> passive.key == "ui.inspect.passive.on_hit_status_proc" })
        assertTrue(terrainItem.passiveDescriptions.any { passive -> passive.key == "ui.inspect.passive.terrain_affinity_bonus" })
        assertTrue(onKillItem.passiveDescriptions.any { passive -> passive.key == "ui.inspect.passive.on_kill_resource_restore" })
        assertTrue(conditionalItem.passiveDescriptions.any { passive -> passive.key == "ui.inspect.passive.conditional_stat_bonus" })
        assertTrue(
            conditionalItem.passiveDescriptions
                .flatMap { passive -> passive.arguments }
                .any { argument -> argument.name == "condition" && argument.valueKey == "ui.inspect.passive.condition.hp_below_50" },
        )
        assertEquals("item.unique.thornpath_crook.name", uniqueItem.nameKey)
        assertEquals("item.unique.thornpath_crook.desc", uniqueItem.descKey)
        assertEquals("item.unique.thornpath_crook.visual", uniqueItem.visualKey)
        assertEquals("item.unique.thornpath_crook.icon", uniqueItem.iconKey)
        assertEquals("audio.item.unique.thornpath_crook", uniqueItem.audioProfile)
        assertEquals("UNIQUE", uniqueItem.specialTierId)
        assertEquals("item.artifact.heartroot_gambit.name", artifactItem.nameKey)
        assertEquals("item.artifact.heartroot_gambit.desc", artifactItem.descKey)
        assertEquals("item.artifact.heartroot_gambit.visual", artifactItem.visualKey)
        assertEquals("item.artifact.heartroot_gambit.icon", artifactItem.iconKey)
        assertEquals("audio.item.artifact.heartroot_gambit", artifactItem.audioProfile)
        assertEquals("ARTIFACT", artifactItem.specialTierId)
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

        session.automationWorld().add(
            bossId,
            com.ktome.core.ai.PendingTelegraphState(
                telegraphSpecId = "self_buff_aura",
                sourceAbilityId = "battlefield_command",
                remainingTurns = 2,
                targetPoint = bossPoint,
                queuedAbilityId = "battlefield_command",
                resolvedDangerLevel = com.ktome.core.ai.DangerLevel.HIGH,
            ),
        )
        var telegraphSnapshot = session.renderSnapshot()
        var initialTelegraph =
            telegraphSnapshot.overlays.singleOrNull { candidate ->
                candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "battlefield_command"
            }
        repeat(3) {
            if (initialTelegraph != null) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
            telegraphSnapshot = session.renderSnapshot()
            initialTelegraph =
                telegraphSnapshot.overlays.singleOrNull { candidate ->
                    candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "battlefield_command"
                }
        }
        initialTelegraph = requireNotNull(initialTelegraph)
        assertEquals("log.warning.telegraph", initialTelegraph.warningMessage?.key)
        assertEquals(OverlayShapeSnapshot.RING, initialTelegraph.shape)
        assertTrue(initialTelegraph.previewTurns >= 1)
        assertTrue(initialTelegraph.cells.any { cell -> cell.x == bossPoint.x && cell.y == bossPoint.y })
        var telegraphCleared = false
        repeat(3) {
            telegraphCleared =
                session.renderSnapshot().overlays.none { candidate ->
                    candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "battlefield_command"
                }
            if (!telegraphCleared) {
                assertTrue(session.perform(PlayerCommand.Wait))
            }
        }
        assertNull(
            session.renderSnapshot().overlays.firstOrNull { candidate ->
                candidate.id.startsWith("telegraph:${bossId.value}:") && candidate.sourceAbilityId == "battlefield_command"
            },
        )

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
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "grey_gate_depths",
                        playerProfessionId = "templar",
                        bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
                    ),
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

    private fun affixItem(
        baseId: String,
        affixId: String,
    ): ItemInstance {
        val base = requireNotNull(itemBundle.baseItems.firstOrNull { item -> item.id == baseId }) { "Unknown base item '$baseId'." }
        val affix = requireNotNull(itemBundle.affixes.firstOrNull { candidate -> candidate.id == affixId }) { "Unknown affix '$affixId'." }
        return ItemInstance(
            baseId = base.id,
            name = base.name,
            type = base.type,
            slot = base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = RarityTier.MAGIC,
            affixes = listOf(affix),
            stats = base.baseStats + affix.statModifiers,
            effect = base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = base.magnitude,
            passive = affix.passive ?: base.passive,
        )
    }

    private fun specialItem(
        templateId: String,
    ): ItemInstance {
        val template = requireNotNull(itemBundle.specialTemplate(templateId)) { "Unknown special template '$templateId'." }
        val base = requireNotNull(itemBundle.baseItems.firstOrNull { item -> item.id == template.itemId }) {
            "Unknown special item base '${template.itemId}'."
        }
        val material = template.fixedMaterialId?.let { materialId ->
            requireNotNull(itemBundle.materials.firstOrNull { candidate -> candidate.id == materialId }) {
                "Unknown material '$materialId' for '$templateId'."
            }
        }
        val affixes =
            template.fixedAffixIds.map { affixId ->
                requireNotNull(itemBundle.affixes.firstOrNull { candidate -> candidate.id == affixId }) {
                    "Unknown affix '$affixId' for '$templateId'."
                }
            }
        val stats =
            listOf(base.baseStats, material?.statModifiers ?: StatModifier.ZERO)
                .plus(affixes.map(AffixDef::statModifiers))
                .fold(StatModifier.ZERO) { acc, modifier -> acc + modifier }
        return ItemInstance(
            baseId = base.id,
            name = base.name,
            type = base.type,
            slot = base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = RarityTier.RARE,
            materialId = material?.id,
            materialName = material?.name,
            affixes = affixes,
            stats = stats,
            effect = base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = base.magnitude,
            passive = base.passive,
            specialTemplateId = template.id,
        )
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
