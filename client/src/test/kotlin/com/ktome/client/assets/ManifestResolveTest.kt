package com.ktome.client.assets

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import java.lang.reflect.Proxy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManifestResolveTest {
    @Test
    fun `runtime manifests load from bundled resources`() {
        val assets = ClientAssetBundleLoader.load()

        assertEquals("ktome-middle-fantasy-painterly-tile-v1", assets.visualManifest.styleTag)
        assertEquals("audio.fallback.silence", assets.audioManifest.fallbackKey)
    }

    @Test
    fun `exact visual key resolves without fallback`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        val resolved = resolver.resolve("actor.vanguard")

        assertTrue(resolver.canResolve("actor.vanguard"))
        assertEquals("actor.vanguard", resolved.resolvedKey)
        assertFalse(resolved.fallbackUsed)
        assertFalse(resolved.matchedByPrefix)
    }

    @Test
    fun `damage type icons resolve as exact visual family entries`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "icon.damage_type.physical",
            "icon.damage_type.fire",
            "icon.damage_type.cold",
            "icon.damage_type.lightning",
            "icon.damage_type.holy",
            "icon.damage_type.shadow",
        ).forEach { key ->
            val resolved = resolver.resolve(key)
            assertEquals("icon_damage_type", resolved.entry.category)
            assertFalse(resolved.fallbackUsed)
            assertFalse(resolved.matchedByPrefix)
        }
    }

    @Test
    fun `current monster actor keys resolve to visible runtime sprites instead of debug placeholders`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "actor.beast.rat" to "phase2/p2-b/actor_beast_rat.png",
            "actor.bandit.sentry" to "phase2/p2-b/actor_bandit_raider.png",
            "actor.undead.bone_archer" to "phase2/p2-b/actor_restless_skeleton.png",
            "actor.orc.raider" to "phase2/p2-c/actor_orc_miner.png",
        ).forEach { (key, expectedPath) ->
            val resolved = resolver.resolve(key)
            assertEquals(expectedPath, resolved.entry.rawOutputPath)
            assertFalse(resolved.fallbackUsed)
        }
    }

    @Test
    fun `route zone keys resolve to formal runtime visuals instead of debug placeholders`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "zone.shattered_outpost.visual" to "phase2/p2-c/zone_shattered_outpost_visual.png",
            "zone.shattered_outpost.icon" to "phase2/p2-c/zone_shattered_outpost_icon.png",
            "zone.greenwood_fringe.visual" to "phase2/p2-c/zone_greenwood_fringe_visual.png",
            "zone.greenwood_fringe.icon" to "phase2/p2-c/zone_greenwood_fringe_icon.png",
            "zone.deep_iron_pit.visual" to "phase2/p2-c/zone_deep_iron_pit_visual.png",
            "zone.deep_iron_pit.icon" to "phase2/p2-c/zone_deep_iron_pit_icon.png",
            "zone.grey_gate_depths.visual" to "phase2/p2-c/zone_grey_gate_depths_visual.png",
            "zone.grey_gate_depths.icon" to "phase2/p2-c/zone_grey_gate_depths_icon.png",
        ).forEach { (key, expectedPath) ->
            val resolved = resolver.resolve(key)
            assertEquals(expectedPath, resolved.entry.rawOutputPath)
            assertFalse(resolved.fallbackUsed)
        }
    }

    @Test
    fun `canResolve only accepts exact manifest keys`() {
        val assets = ClientAssetBundleLoader.load()

        assertFalse(assets.visualResolver.canResolve("icon.missing.example"))
        assertFalse(assets.audioResolver.canResolve("audio.missing.example"))
    }

    @Test
    fun `unknown visual icon resolves through prefix fallback`() {
        val messages = mutableListOf<String>()
        val resolver =
            VisualManifestResolver(
                manifest = VisualManifestResourceLoader.load(),
                logSink = ManifestLogSink { message -> messages += message },
            )

        val resolved = resolver.resolve("icon.missing.example")

        assertEquals("missing_visual", resolved.resolvedKey)
        assertTrue(resolved.fallbackUsed)
        assertTrue(resolved.matchedByPrefix)
        assertTrue(messages.any { message -> message.contains("icon.missing.example") })
    }

    @Test
    fun `unknown audio key resolves through fallback family`() {
        val messages = mutableListOf<String>()
        val resolver =
            AudioManifestResolver(
                manifest = AudioManifestResourceLoader.load(),
                logSink = ManifestLogSink { message -> messages += message },
            )

        val resolved = resolver.resolve("audio.missing.example")

        assertEquals("audio.fallback.silence", resolved.resolvedKey)
        assertTrue(resolved.fallbackUsed)
        assertTrue(messages.any { message -> message.contains("audio.missing.example") })
    }

    @Test
    fun `session and warm cache preload resident textures when gdx is available`() {
        withHeadlessGdx {
            val assets = ClientAssetBundleLoader.load()
            val strategy = ClientAssetLoadStrategy(assets)
            val snapshot = sampleRenderSnapshot()

            try {
                strategy.sessionLoad(snapshot)
                strategy.warmCache(snapshot)

                val loadedPaths = assets.textureRepository.loadedPaths()
                assertTrue(loadedPaths.contains(assets.visualResolver.resolve(snapshot.metadata.zoneVisualKey).entry.rawOutputPath))
                assertTrue(loadedPaths.contains(assets.visualResolver.resolve(snapshot.mapCells.first().terrainVisualKey).entry.rawOutputPath))
                assertTrue(loadedPaths.contains(assets.visualResolver.resolve(snapshot.actors.first().visualKey).entry.rawOutputPath))
                assertTrue(loadedPaths.contains(assets.visualResolver.resolve(snapshot.overlays.first().visualKey).entry.rawOutputPath))
            } finally {
                assets.dispose()
            }
        }
    }

    private fun sampleRenderSnapshot(): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = 1,
                    zoneId = "shattered_outpost",
                    zoneNameKey = "zone.shattered_outpost.name",
                    currentFloor = 1,
                    maxFloor = 2,
                    width = 2,
                    height = 2,
                    playerX = 0,
                    playerY = 0,
                    zoneVisualKey = "zone.shattered_outpost.visual",
                    zoneAudioProfile = "audio.zone.shattered_outpost",
                    tilesetKey = "tileset.ruins",
                    ambientProfile = "ambient.shattered_outpost",
                ),
            mapCells =
                listOf(
                    MapCellSnapshot(
                        x = 0,
                        y = 0,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.ruins.ground_01",
                    ),
                ),
            props =
                listOf(
                    PropRenderSnapshot(
                        id = "stairs:down:9",
                        x = 1,
                        y = 0,
                        propTypeId = "stairs",
                        stairDirectionId = "DOWN",
                        visualKey = "prop.stairs.down",
                        audioProfile = "audio.interactable.stairs",
                    ),
                ),
            actors =
                listOf(
                    ActorRenderSnapshot(
                        entityId = 1,
                        x = 0,
                        y = 0,
                        visualKey = "actor.vanguard",
                        audioProfile = "audio.profession.vanguard",
                        nameKey = "actor.player.name",
                        isPlayer = true,
                        roleKind = ActorRoleKindSnapshot.PLAYER,
                    ),
                ),
            overlays =
                listOf(
                    OverlayRenderSnapshot(
                        id = "boss-warning:7",
                        visualKey = "vfx.boss.warning.sigil_01",
                        audioProfile = "audio.boss.warning",
                        previewTurns = 1,
                        dangerLevel = 3,
                        shape = OverlayShapeSnapshot.SINGLE_TILE,
                        sourceAbilityId = "dungeon_lord_encounter",
                        cells = emptyList(),
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
                    talents =
                        listOf(
                            TalentSlotSnapshot(
                                slot = 1,
                                talentId = "power_strike",
                                nameKey = "talent.vanguard.power_strike.name",
                                iconKey = "talent.vanguard.power_strike.icon",
                                damageTypeIconKey = "icon.damage_type.physical",
                                level = 1,
                                maxLevel = 5,
                                resourceCost = 8,
                                resourceLabelKey = "ui.hud.stamina.short",
                                resourceTypeId = "STAMINA",
                                range = 1,
                                minRange = 0,
                                currentCooldown = 0,
                                maxCooldown = 0,
                                requiresTarget = false,
                            ),
                        ),
                    inventory = emptyList(),
                    targetablePositions = emptyList(),
                ),
        )

    private fun <T> withHeadlessGdx(block: () -> T): T {
        val backend = HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        val noOpGl =
            Proxy.newProxyInstance(
                GL20::class.java.classLoader,
                arrayOf(GL20::class.java),
            ) { _, method, _ ->
                when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Float.TYPE -> 0f
                    java.lang.Long.TYPE -> 0L
                    java.lang.Double.TYPE -> 0.0
                    java.lang.Short.TYPE -> 0.toShort()
                    java.lang.Byte.TYPE -> 0.toByte()
                    java.lang.Character.TYPE -> 0.toChar()
                    else -> null
                }
            } as GL20
        Gdx.gl = noOpGl
        Gdx.gl20 = noOpGl
        return try {
            block()
        } finally {
            backend.exit()
        }
    }
}
