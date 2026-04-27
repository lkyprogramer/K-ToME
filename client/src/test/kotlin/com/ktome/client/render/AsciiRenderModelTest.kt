package com.ktome.client.render

import com.ktome.client.assets.ManifestLogSink
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.BossVariantRenderSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.CombatFeedbackSnapshot
import com.ktome.core.snapshot.CombatFeedbackTypeSnapshot
import com.ktome.core.snapshot.FrontstageActionCategorySnapshot
import com.ktome.core.snapshot.FrontstageActionCueSnapshot
import com.ktome.core.snapshot.FrontstageActionPrioritySnapshot
import com.ktome.core.snapshot.FrontstageReadabilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.RewardPresentationEntrySnapshot
import com.ktome.core.snapshot.RewardPresentationSourceSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.snapshot.TalentTreeSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AsciiRenderModelTest {
    @Test
    fun `ascii render model highlights high signal log families with distinct tones`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            AsciiRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        logEvents =
                            listOf(
                                RenderLogEventSnapshot(
                                    RenderTextTokenSnapshot(
                                        key = "log.zone.enter",
                                        arguments =
                                            listOf(
                                                RenderTextArgumentSnapshot(name = "zone", valueKey = "zone.shattered_outpost.name"),
                                                RenderTextArgumentSnapshot(name = "desc", valueKey = "zone.shattered_outpost.desc"),
                                            ),
                                    ),
                                ),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.passive.damage_bonus_vs_tag")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.talent.damage_resisted")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.talent.damage_vulnerable")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.level_up")),
                                RenderLogEventSnapshot(RenderTextTokenSnapshot("log.boss.enrage")),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertEquals(
            listOf(
                AsciiTextTone.CYAN,
                AsciiTextTone.GREEN,
                AsciiTextTone.CYAN,
                AsciiTextTone.RED,
                AsciiTextTone.GOLD,
                AsciiTextTone.RED,
            ),
            model.messageLines.map { line -> line.tone },
        )
    }

    @Test
    fun `ascii talent sidebar uses shared talent presentation lines`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            AsciiRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        talentTrees =
                            listOf(
                                TalentTreeSnapshot(
                                    treeId = "vanguard_arms",
                                    treeOwnerId = "vanguard",
                                    nameKey = "talent_tree.vanguard_arms.name",
                                    descKey = "talent_tree.vanguard_arms.desc",
                                    nodes =
                                        listOf(
                                            TalentTreeNodeSnapshot(
                                                talentId = "charge",
                                                treeId = "vanguard_arms",
                                                treeOwnerId = "vanguard",
                                                nameKey = "talent.vanguard.charge.name",
                                                descKey = "talent.vanguard.charge.desc",
                                                state = TalentNodeStateSnapshot.LEARNABLE,
                                                rank = 0,
                                                maxRank = 5,
                                                unlockLevel = 1,
                                                resourceCost = 8,
                                                resourceLabelKey = "ui.hud.stamina.short",
                                                range = 3,
                                                minRange = 1,
                                                currentCooldown = 0,
                                                maxCooldown = 4,
                                                requiresTarget = true,
                                            ),
                                            TalentTreeNodeSnapshot(
                                                talentId = "war_cry",
                                                treeId = "vanguard_arms",
                                                treeOwnerId = "vanguard",
                                                nameKey = "talent.vanguard.war_cry.name",
                                                descKey = "talent.vanguard.war_cry.desc",
                                                state = TalentNodeStateSnapshot.LEARNED_ACTIVE,
                                                rank = 1,
                                                maxRank = 5,
                                                unlockLevel = 1,
                                                resourceCost = 12,
                                                resourceLabelKey = "ui.hud.stamina.short",
                                                range = 0,
                                                minRange = 0,
                                                currentCooldown = 0,
                                                maxCooldown = 6,
                                                requiresTarget = false,
                                            ),
                                        ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelection = 0, talentTreePreviewExpanded = false),
            )

        assertTrue(model.sidebarLines.any { line -> line.text == "[+] Charge 0/5" && line.tone == AsciiTextTone.CYAN })
        assertTrue(model.sidebarLines.any { line -> line.text == "[*] War Cry 1/5" && line.tone == AsciiTextTone.GOLD })
        assertTrue(model.sidebarLines.any { line -> line.text == "Preview collapsed. Press P to expand." && line.tone == AsciiTextTone.LIGHT_GRAY })
    }

    @Test
    fun `ascii render model includes compact combat feedback sidebar lines`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val model =
            AsciiRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    sampleSnapshot(
                        logEvents = emptyList(),
                        combatFeedbackEvents =
                            listOf(
                                CombatFeedbackSnapshot(
                                    targetEntityId = 1,
                                    x = 0,
                                    y = 0,
                                    type = CombatFeedbackTypeSnapshot.HEAL,
                                    amount = 12,
                                ),
                                CombatFeedbackSnapshot(
                                    targetEntityId = 1,
                                    x = 0,
                                    y = 0,
                                    type = CombatFeedbackTypeSnapshot.MISS,
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        val sidebarTexts = model.sidebarLines.map { line -> line.text }
        assertTrue(sidebarTexts.contains("Combat Feedback"))
        assertTrue(sidebarTexts.contains("+12"))
        assertTrue(sidebarTexts.contains("MISS"))
    }

    @Test
    fun `ascii render model derives boss variant tint from visual manifest metadata`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val baseSnapshot = sampleSnapshot(logEvents = emptyList())
        val model =
            AsciiRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    baseSnapshot.copy(
                        actors =
                            baseSnapshot.actors +
                                ActorRenderSnapshot(
                                    entityId = 2,
                                    x = 1,
                                    y = 0,
                                    visualKey = "actor.vanguard",
                                    nameKey = "boss.test.name",
                                    isPlayer = false,
                                    roleKind = ActorRoleKindSnapshot.BOSS,
                                    bossVariant =
                                        BossVariantRenderSnapshot(
                                            variantId = "boss.variant.molten_glass",
                                            nameKey = "boss.variant.molten_glass.name",
                                            visualTintKey = "vfx.boss.variant.molten_glass",
                                        ),
                                ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        assertEquals("#FF7A3C", model.actorGlyphs.single { glyph -> glyph.x == 1 && glyph.y == 0 }.colorHex)
    }

    @Test
    fun `ascii render model shows frontstage section and recent reward detail line`() {
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val baseSnapshot = sampleSnapshot(logEvents = emptyList())
        val model =
            AsciiRenderer.buildRenderModel(
                localizer = localizer,
                visualResolver = sampleResolver(),
                snapshot =
                    baseSnapshot.copy(
                        uiState =
                            baseSnapshot.uiState.copy(
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
                                        ),
                                    ),
                                frontstageReadability =
                                    FrontstageReadabilitySnapshot(
                                        terrainHighlights = listOf(RenderTextTokenSnapshot("ui.hud.frontstage.terrain.water")),
                                        recentActionCues =
                                            listOf(
                                                FrontstageActionCueSnapshot(
                                                    category = FrontstageActionCategorySnapshot.SEARCH,
                                                    priority = FrontstageActionPrioritySnapshot.MEDIUM,
                                                    stableKey = "search:no_target",
                                                    message = RenderTextTokenSnapshot("log.search.no_target"),
                                                ),
                                                FrontstageActionCueSnapshot(
                                                    category = FrontstageActionCategorySnapshot.SECRET,
                                                    priority = FrontstageActionPrioritySnapshot.CRITICAL,
                                                    stableKey = "secret:enter:test",
                                                    message = RenderTextTokenSnapshot("ui.hud.frontstage.terrain.oil"),
                                                ),
                                                FrontstageActionCueSnapshot(
                                                    category = FrontstageActionCategorySnapshot.PASSIVE,
                                                    priority = FrontstageActionPrioritySnapshot.LOW,
                                                    stableKey = "passive:test",
                                                    message = RenderTextTokenSnapshot("ui.hud.frontstage.terrain.ice"),
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.MAP),
            )

        val sidebarTexts = model.sidebarLines.map { line -> line.text }
        assertTrue(sidebarTexts.contains(localizer.text("ui.sidebar.frontstage")))
        assertTrue(sidebarTexts.any { text -> text.contains(localizer.text("ui.hud.frontstage.terrain.water")) })
        assertTrue(sidebarTexts.any { text -> text.contains(localizer.text("ui.inspect.passive.hp_regen_turn", "amount" to 2)) })
        assertEquals(
            AsciiTextTone.GREEN,
            model.sidebarLines.first { line -> line.text.contains(localizer.text("log.search.no_target")) }.tone,
        )
        assertEquals(
            AsciiTextTone.GOLD,
            model.sidebarLines.first { line -> line.text.contains(localizer.text("ui.hud.frontstage.terrain.oil")) }.tone,
        )
        assertEquals(
            AsciiTextTone.LIGHT_GRAY,
            model.sidebarLines.first { line -> line.text.contains(localizer.text("ui.hud.frontstage.terrain.ice")) }.tone,
        )
    }

    private fun sampleResolver(): VisualManifestResolver =
        VisualManifestResolver(
            manifest =
                VisualManifest(
                    manifestVersion = 1,
                    styleTag = "test-style",
                    fallbackKey = "missing_visual",
                    entries =
                        listOf(
                            VisualManifestEntry(
                                key = "missing_visual",
                                category = "debug",
                                rawOutputPath = "debug/missing_visual.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "tileset.test.ground_01",
                                category = "tile_ground",
                                rawOutputPath = "phase2/p2-b/tileset_ruins_ground_01.png",
                                footprint = "1x1",
                            ),
                            VisualManifestEntry(
                                key = "actor.vanguard",
                                category = "actor_sprite",
                                rawOutputPath = "phase2/p2-b/actor_vanguard.png",
                                footprint = "2x1",
                            ),
                            VisualManifestEntry(
                                key = "vfx.boss.variant.molten_glass",
                                category = "vfx_overlay",
                                rawOutputPath = "phase4/pr06/boss_variant_molten_glass.png",
                                footprint = "1x1",
                                asciiColorHex = "#FF7A3C",
                            ),
                        ),
                    prefixRules = listOf(ManifestPrefixRule(prefix = "icon.", targetKey = "missing_visual")),
                ),
            logSink = ManifestLogSink { error("Unexpected manifest fallback: $it") },
        )

    private fun sampleSnapshot(
        logEvents: List<RenderLogEventSnapshot> = emptyList(),
        combatFeedbackEvents: List<CombatFeedbackSnapshot> = emptyList(),
        talentTrees: List<TalentTreeSnapshot> = emptyList(),
    ): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = 1,
                    zoneId = "shattered_outpost",
                    zoneNameKey = "zone.shattered_outpost.name",
                    zoneDescKey = "zone.shattered_outpost.desc",
                    currentFloor = 1,
                    maxFloor = 2,
                    width = 1,
                    height = 1,
                    playerX = 0,
                    playerY = 0,
                    zoneVisualKey = "zone.shattered_outpost.visual",
                    zoneAudioProfile = "audio.zone.shattered_outpost",
                    tilesetKey = "tileset.test",
                    ambientProfile = "ambient.shattered_outpost",
                ),
            mapCells =
                listOf(
                    MapCellSnapshot(
                        x = 0,
                        y = 0,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    ),
                ),
            actors =
                listOf(
                    ActorRenderSnapshot(
                        entityId = 1,
                        x = 0,
                        y = 0,
                        visualKey = "actor.vanguard",
                        nameKey = "actor.player.name",
                        isPlayer = true,
                        roleKind = ActorRoleKindSnapshot.PLAYER,
                    ),
                ),
            uiState =
                RenderUiStateSnapshot(
                    playerStatus =
                        PlayerStatusSnapshot(
                            currentHp = 24,
                            maxHp = 24,
                            currentResource = 12,
                            maxResource = 12,
                            resourceLabelKey = "ui.hud.stamina.short",
                            resourceTypeId = "STAMINA",
                            level = 1,
                            currentExperience = 0,
                            nextLevelRequirement = 12,
                            statPoints = 0,
                            talentPoints = 0,
                            attack = 7,
                            defense = 5,
                            accuracy = 6,
                            evasion = 4,
                            speed = 100,
                        ),
                    equipment = emptyList(),
                    talents = emptyList(),
                    reserveTalents = emptyList(),
                    talentTrees = talentTrees,
                    inventory = emptyList(),
                    targetablePositions = listOf(GridPointSnapshot(0, 0)),
                ),
            logEvents = logEvents,
            combatFeedbackEvents = combatFeedbackEvents,
        )
}
