package com.ktome.client.assets

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.ktome.game.contentpack.ContentPackSelection
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
import java.awt.image.BufferedImage
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `visual manifest v1 rejects removed ASCII entry fields`() {
        assertThrows(ManifestLoadException::class.java) {
            VisualManifestResourceLoader.decode(
                visualManifestWithEntryFields(
                    manifestVersion = 1,
                    extraEntryFields =
                        """,
                        "asciiGlyph": "?",
                        "asciiColorHex": "#FFFFFF"
                        """,
                ),
                path = "legacy-v1-visual-manifest.json",
            )
        }
    }

    @Test
    fun `visual manifest v1 still rejects non legacy unknown entry fields`() {
        assertThrows(ManifestLoadException::class.java) {
            VisualManifestResourceLoader.decode(
                visualManifestWithEntryFields(
                    manifestVersion = 1,
                    extraEntryFields =
                        """,
                        "unexpectedField": true
                        """,
                ),
                path = "unknown-field-visual-manifest.json",
            )
        }
    }

    @Test
    fun `visual manifest v2 rejects removed ASCII entry fields`() {
        assertThrows(ManifestLoadException::class.java) {
            VisualManifestResourceLoader.decode(
                visualManifestWithEntryFields(
                    manifestVersion = 2,
                    extraEntryFields =
                        """,
                        "asciiGlyph": "?",
                        "asciiColorHex": "#FFFFFF"
                        """,
                ),
                path = "v2-visual-manifest.json",
            )
        }
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
    fun `dark ui dry-run frame keys resolve through exact manifest entries`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf("ui.frame.panel.body", "ui.frame.panel.focus").forEach { key ->
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals("ui_frame", resolved.entry.category)
            assertFalse(resolved.fallbackUsed)
            assertFalse(resolved.matchedByPrefix)
        }
    }

    @Test
    fun `dark uiux pr02 2 actor and prop owner keys resolve through exact entries`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        mapOf(
            "actor.vanguard" to "dark-v1/actors/actor_vanguard.png",
            "prop.stairs.down" to "dark-v1/props/prop_stairs_down.png",
        ).forEach { (key, expectedPath) ->
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals(key, resolved.requestedKey)
            assertEquals(expectedPath, resolved.entry.rawOutputPath)
            assertTrue(resolved.entry.tags.contains("dark-v1"), key)
            assertTrue(resolved.entry.tags.contains("ui-demo-new"), key)
            assertFalse(resolved.fallbackUsed, key)
            assertFalse(resolved.matchedByPrefix, key)
        }
    }

    @Test
    fun darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        mapOf(
            "tileset.ruins.ground_01" to "dark-v1/tiles/tileset_ruins_ground_01.png",
            "tileset.ruins.ground_01.variant_1" to "dark-v1/tiles/tileset_ruins_ground_01_variant_1.png",
            "tileset.ruins.ground_01.variant_2" to "dark-v1/tiles/tileset_ruins_ground_01_variant_2.png",
            "tileset.ruins.ground_01.variant_3" to "dark-v1/tiles/tileset_ruins_ground_01_variant_3.png",
            "tileset.ruins.wall_01" to "dark-v1/tiles/tileset_ruins_wall_01.png",
            "tileset.ruins.wall_01.crown" to "dark-v1/tiles/tileset_ruins_wall_01_crown.png",
            "tileset.ruins.wall_01.side" to "dark-v1/tiles/tileset_ruins_wall_01_side.png",
            "tileset.ruins.wall_01.corner" to "dark-v1/tiles/tileset_ruins_wall_01_corner.png",
            "tileset.ruins.wall_01.door_contact" to "dark-v1/tiles/tileset_ruins_wall_01_door_contact.png",
            DarkUiMapVisualKeys.RUINS_ROOM_MATERIAL_BREAKUP to "dark-v1/tiles/tileset_ruins_room_breakup_01.png",
            DarkUiMapVisualKeys.RUINS_ROOM_ART_PLATE_PROTOTYPE to "dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png",
            DarkUiMapVisualKeys.RUINS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE to "dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png",
            DarkUiMapVisualKeys.FOREST_EDGE_ROOM_ART_PLATE_PROTOTYPE to "dark-v1/ui/ui_map_stage_forest_edge_room_plate_pr08_demo.png",
            DarkUiMapVisualKeys.FOREST_EDGE_ROOM_TOPOLOGY_SOURCE_PROTOTYPE to "dark-v1/ui/ui_map_stage_forest_edge_room_topology_source_pr08_demo.png",
            DarkUiMapVisualKeys.MINE_ROOM_ART_PLATE_PROTOTYPE to "dark-v1/ui/ui_map_stage_mine_room_plate_pr08_demo.png",
            DarkUiMapVisualKeys.MINE_ROOM_TOPOLOGY_SOURCE_PROTOTYPE to "dark-v1/ui/ui_map_stage_mine_room_topology_source_pr08_demo.png",
            DarkUiMapVisualKeys.SHADOW_DEPTHS_ROOM_ART_PLATE_PROTOTYPE to "dark-v1/ui/ui_map_stage_shadow_depths_room_plate_pr08_demo.png",
            DarkUiMapVisualKeys.SHADOW_DEPTHS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE to "dark-v1/ui/ui_map_stage_shadow_depths_room_topology_source_pr08_demo.png",
        ).forEach { (key, expectedPath) ->
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals(key, resolved.requestedKey)
            assertEquals(expectedPath, resolved.entry.rawOutputPath)
            assertTrue(resolved.entry.tags.contains("dark-v1"), key)
            assertTrue(resolved.entry.tags.contains("ui-demo-new"), key)
            if (key == DarkUiMapVisualKeys.RUINS_ROOM_MATERIAL_BREAKUP) {
                assertEquals("tile_decal", resolved.entry.category)
                assertTrue(resolved.entry.tags.contains("room_material_breakup"), key)
            }
            if (key.endsWith(".room_plate.pr08_demo")) {
                assertEquals("ui_frame", resolved.entry.category)
                assertTrue(resolved.entry.tags.contains("room_art_plate"), key)
            }
            if (key.endsWith(".room_topology_source.pr08_demo")) {
                assertEquals("ui_frame", resolved.entry.category)
                assertTrue(resolved.entry.tags.contains("room_topology_source"), key)
                assertTrue(resolved.entry.tags.contains("topology_fragment_source"), key)
            }
            assertFalse(resolved.fallbackUsed, key)
            assertFalse(resolved.matchedByPrefix, key)
        }
    }

    @Test
    fun darkUiuxPr08DirectorFloorVariantFamilyResolvesFromManifestTags() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        val familyKeys = resolver.terrainVariantKeys("tileset.ruins.ground_01")

        assertEquals(
            listOf(
                "tileset.ruins.ground_01",
                "tileset.ruins.ground_01.variant_1",
                "tileset.ruins.ground_01.variant_2",
                "tileset.ruins.ground_01.variant_3",
            ),
            familyKeys,
        )
        assertEquals("tileset.ruins.ground_01", resolver.resolveTerrainVariant("tileset.ruins.ground_01", 0).resolvedKey)
        assertEquals("tileset.ruins.ground_01.variant_1", resolver.resolveTerrainVariant("tileset.ruins.ground_01", 1).resolvedKey)
        assertEquals("tileset.ruins.ground_01.variant_1", resolver.resolveTerrainVariant("tileset.ruins.ground_01", 5).resolvedKey)
        assertEquals("tileset.ruins.wall_01", resolver.resolveTerrainVariant("tileset.ruins.wall_01", 3).resolvedKey)
    }

    @Test
    fun darkUiuxPr08DirectorWallFamilyResolvesFromManifestTags() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        assertEquals(
            listOf(
                "tileset.ruins.wall_01",
                "tileset.ruins.wall_01.crown",
                "tileset.ruins.wall_01.side",
                "tileset.ruins.wall_01.corner",
                "tileset.ruins.wall_01.door_contact",
            ),
            resolver.terrainWallFamilyKeys("tileset.ruins.wall_01"),
        )
        assertEquals("tileset.ruins.wall_01", resolver.resolveTerrainWallPiece("tileset.ruins.wall_01", TerrainWallPieceRole.BASE).resolvedKey)
        assertEquals("tileset.ruins.wall_01.crown", resolver.resolveTerrainWallPiece("tileset.ruins.wall_01", TerrainWallPieceRole.CROWN).resolvedKey)
        assertEquals("tileset.ruins.wall_01.side", resolver.resolveTerrainWallPiece("tileset.ruins.wall_01", TerrainWallPieceRole.SIDE).resolvedKey)
        assertEquals("tileset.ruins.wall_01.corner", resolver.resolveTerrainWallPiece("tileset.ruins.wall_01", TerrainWallPieceRole.CORNER).resolvedKey)
        assertEquals(
            "tileset.ruins.wall_01.door_contact",
            resolver.resolveTerrainWallPiece("tileset.ruins.wall_01", TerrainWallPieceRole.DOOR_CONTACT).resolvedKey,
        )
        assertEquals(listOf("tileset.ruins.ground_01"), resolver.terrainWallFamilyKeys("tileset.ruins.ground_01"))
    }

    @Test
    fun darkUiuxPr08NonRuinsWallFamiliesResolveDistinctTopologyRiskPieces() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            DarkUiMapVisualKeys.FOREST_EDGE_WALL to "dark-v1/tiles/tileset_forest_edge_wall_01",
            DarkUiMapVisualKeys.MINE_WALL to "dark-v1/tiles/tileset_mine_wall_01",
            DarkUiMapVisualKeys.SHADOW_DEPTHS_WALL to "dark-v1/tiles/tileset_shadow_depths_wall_01",
        ).forEach { (baseKey, pathPrefix) ->
            val expectedKeys =
                listOf(
                    baseKey,
                    "$baseKey.crown",
                    "$baseKey.side",
                    "$baseKey.corner",
                    "$baseKey.door_contact",
                )

            assertEquals(expectedKeys, resolver.terrainWallFamilyKeys(baseKey), baseKey)
            assertEquals(baseKey, resolver.resolveTerrainWallPiece(baseKey, TerrainWallPieceRole.BASE).resolvedKey)
            assertEquals("$baseKey.crown", resolver.resolveTerrainWallPiece(baseKey, TerrainWallPieceRole.CROWN).resolvedKey)
            assertEquals("$baseKey.side", resolver.resolveTerrainWallPiece(baseKey, TerrainWallPieceRole.SIDE).resolvedKey)
            assertEquals("$baseKey.corner", resolver.resolveTerrainWallPiece(baseKey, TerrainWallPieceRole.CORNER).resolvedKey)
            assertEquals(
                "$baseKey.door_contact",
                resolver.resolveTerrainWallPiece(baseKey, TerrainWallPieceRole.DOOR_CONTACT).resolvedKey,
            )
            TerrainWallPieceRole.entries.forEach { role ->
                val resolved = resolver.resolveTerrainWallPiece(baseKey, role)
                assertEquals("tile_wall", resolved.entry.category, resolved.resolvedKey)
                assertTrue(resolved.entry.rawOutputPath.startsWith(pathPrefix), resolved.resolvedKey)
                assertTrue(resolved.entry.tags.contains("terrain_wall_family:$baseKey"), resolved.resolvedKey)
                assertTrue(resolved.entry.tags.contains("terrain_wall_piece:${role.tagValue}"), resolved.resolvedKey)
            }
        }
    }

    @Test
    fun darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "tileset.forest_edge.ground_01" to "dark-v1/tiles/tileset_forest_edge_ground_01.png",
            "tileset.forest_edge.wall_01" to "dark-v1/tiles/tileset_forest_edge_wall_01.png",
            "tileset.mine.ground_01" to "dark-v1/tiles/tileset_mine_ground_01.png",
            "tileset.mine.wall_01" to "dark-v1/tiles/tileset_mine_wall_01.png",
            "tileset.shadow_depths.ground_01" to "dark-v1/tiles/tileset_shadow_depths_ground_01.png",
            "tileset.shadow_depths.wall_01" to "dark-v1/tiles/tileset_shadow_depths_wall_01.png",
        ).forEach { (key, expectedPath) ->
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals(expectedPath, resolved.entry.rawOutputPath)
            assertTrue(resolved.entry.tags.contains("pr05"), key)
            assertFalse(resolved.fallbackUsed, key)
            assertFalse(resolved.matchedByPrefix, key)
        }
    }

    @Test
    fun darkUiuxPr05BestiaryIconsResolveThroughExactEntries() {
        val assets = ClientAssetBundleLoader.load()
        val resolver = assets.visualResolver
        val keys =
            assets.visualManifest.entries
                .filter { entry -> entry.tags.contains("pr05") && entry.rawOutputPath.startsWith("dark-v1/icons/") }
                .map(VisualManifestEntry::key)

        assertTrue(keys.size >= 40, keys.toString())
        listOf("icon.monster.bandit.captain", "icon.monster.abyssal.guardian", "boss.orc.molten_giant.icon").forEach { key ->
            assertTrue(key in keys, keys.toString())
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals("icon", resolved.entry.category)
            assertTrue(resolved.entry.rawOutputPath.startsWith("dark-v1/icons/"))
            assertFalse(resolved.fallbackUsed)
            assertFalse(resolved.matchedByPrefix)
        }
    }

    @Test
    fun darkUiuxPr05PortraitKeysResolveThroughExactEntries() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf("portrait.arcanist", "portrait.rogue", "tree.vanguard_arms", "tree.arcanist_flame").forEach { key ->
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals("portrait", resolved.entry.category)
            assertTrue(resolved.entry.rawOutputPath.startsWith("dark-v1/portraits/"))
            assertTrue(resolved.entry.tags.contains("pr05"), key)
            assertFalse(resolved.fallbackUsed)
            assertFalse(resolved.matchedByPrefix)
        }
    }

    @Test
    fun darkUiuxPr05ZoneVisualKeysResolveThroughExactEntries() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "zone.greenwood_fringe.visual" to "prop_environment",
            "zone.deep_iron_pit.visual" to "prop_environment",
            "zone.grey_gate_depths.visual" to "prop_environment",
            "zone.secret.abyssal_temple_warded_archive.visual" to "portrait",
        ).forEach { (key, expectedCategory) ->
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals(expectedCategory, resolved.entry.category)
            assertTrue(resolved.entry.rawOutputPath.startsWith("dark-v1/portraits/"))
            assertTrue(resolved.entry.tags.contains("r06-portraits-zones"), key)
            assertFalse(resolved.fallbackUsed)
            assertFalse(resolved.matchedByPrefix)
        }
    }

    @Test
    fun darkUiuxPr05OwnerInventoryClosesAgainstExactManifestEntries() {
        val assets = ClientAssetBundleLoader.load()
        val resolver = assets.visualResolver
        val inventory = pr05OwnerInventory()
        val entries = inventory["entries"]!!.jsonArray.map { item -> item.jsonObject }
        val expectedKeys = stringArray(inventory, "ownerExpectedKeys")
        val coveredKeys = stringArray(inventory, "ownerCoveredKeys")

        assertEquals(161, entries.size)
        assertEquals(expectedKeys, coveredKeys)
        assertEquals(expectedKeys, entries.map { entry -> entry.string("targetKey") }.sorted())
        assertEquals(emptyList<String>(), stringArray(inventory, "allowedOwnerFallbackKeys"))
        assertEquals(emptyList<String>(), stringArray(inventory, "oldStyleOwnerKeys"))
        assertEquals(emptyList<String>(), stringArray(inventory, "pendingOwnerKeys"))

        entries.forEach { entry ->
            val key = entry.string("targetKey")
            val resolved = resolver.resolve(key)

            assertEquals("PR-05", entry.string("ownerPr"), key)
            assertEquals("missing_visual", entry.string("fallbackKey"), key)
            assertEquals(key, resolved.resolvedKey)
            assertTrue(resolved.entry.rawOutputPath.startsWith("dark-v1/"), key)
            assertTrue(resolved.entry.tags.contains("pr05"), key)
            assertFalse(resolved.fallbackUsed, key)
            assertFalse(resolved.matchedByPrefix, key)
        }
    }

    @Test
    fun `vanguard starter hotbar icons keep transparent socket background`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "talent.vanguard.power_strike.icon",
            "talent.vanguard.shield_bash.icon",
            "talent.vanguard.guard_stance.icon",
            "talent.vanguard.charge.icon",
            "talent.vanguard.war_cry.icon",
        ).forEach { key ->
            val resolved = resolver.resolve(key)
            val resourceUrl = requireNotNull(javaClass.classLoader.getResource(resolved.entry.rawOutputPath)) { resolved.entry.rawOutputPath }
            val image = ImageIO.read(resourceUrl)
            val cornerAlpha =
                listOf(
                    image.getRGB(0, 0),
                    image.getRGB(image.width - 1, 0),
                    image.getRGB(0, image.height - 1),
                    image.getRGB(image.width - 1, image.height - 1),
                ).map { argb -> argb ushr 24 }

            assertTrue(cornerAlpha.all { alpha -> alpha <= 8 }, "$key corners must stay transparent: $cornerAlpha")
        }
    }

    @Test
    fun `vanguard starter hotbar icons are subject first without circular medallion fill`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "talent.vanguard.power_strike.icon",
            "talent.vanguard.shield_bash.icon",
            "talent.vanguard.guard_stance.icon",
            "talent.vanguard.charge.icon",
        ).forEach { key ->
            val resolved = resolver.resolve(key)
            val resourceUrl = requireNotNull(javaClass.classLoader.getResource(resolved.entry.rawOutputPath)) { resolved.entry.rawOutputPath }
            val image = ImageIO.read(resourceUrl)
            val alphaCoverage = alphaCoverage(image)

            assertTrue(
                alphaCoverage in 0.18..0.40,
                "$key should be a transparent, subject-first action icon rather than a baked circular medallion fill; alphaCoverage=$alphaCoverage",
            )
        }
    }

    @Test
    fun `dark uiux hero crest keeps authored red enamel and gold heraldry`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver
        val resolved = resolver.resolve(DarkUiChromeVisualKeys.SHELL_HERO_CREST_PLACEHOLDER)
        val resourceUrl = requireNotNull(javaClass.classLoader.getResource(resolved.entry.rawOutputPath)) { resolved.entry.rawOutputPath }
        val image = ImageIO.read(resourceUrl)

        assertTrue(
            alphaCoverage(image) in 0.35..0.58,
            "Hero crest should stay as a transparent subject-first sprite, not a baked opaque card.",
        )
        assertTrue(
            pixelShare(image) { red, green, blue ->
                red > 95 && red > green * 1.35 && red > blue * 1.65
            } >= 0.08,
            "Hero crest should carry enough red enamel to read as authored heraldry rather than a grey placeholder shield.",
        )
        assertTrue(
            pixelShare(image) { red, green, blue ->
                red > 130 && green > 92 && blue < 90 && red >= green * 1.05
            } >= 0.04,
            "Hero crest should carry enough gold heraldic metal to read at bottom-HUD scale.",
        )
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
    fun darkUiuxPr06FinalFullKeyFamiliesResolveThroughDarkEntries() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        mapOf(
            "icon.skill.templar.smite" to "icon_skill",
            "talent.vanguard.charge.icon" to "icon",
            "talent.vanguard.charge.visual" to "icon_skill",
            "icon.status.burn" to "icon_status",
            "icon.mutation.ironhide" to "icon_status",
            "icon.damage_type.fire" to "icon_damage_type",
            "icon.quest.objective_marker" to "icon_quest",
            "zone.greenwood_fringe.icon" to "icon_quest",
            "icon.profession.berserker" to "icon",
            "icon.profession.spellblade" to "icon",
            "icon.profession.vanguard" to "icon",
            "icon.tree.vanguard_arms" to "icon",
            "difficulty.normal.icon" to "icon",
            "missing_visual" to "debug",
            "tile.hidden" to "debug",
        ).forEach { (key, expectedCategory) ->
            val resolved = resolver.resolve(key)

            assertEquals(key, resolved.resolvedKey)
            assertEquals(expectedCategory, resolved.entry.category, key)
            assertTrue(resolved.entry.rawOutputPath.startsWith("dark-v1/"), key)
            assertTrue(resolved.entry.tags.contains("pr06"), key)
            assertFalse(resolved.fallbackUsed, key)
            assertFalse(resolved.matchedByPrefix, key)
            assertNotNull(javaClass.classLoader.getResource(resolved.entry.rawOutputPath), key)
        }
    }

    @Test
    fun `current monster actor keys resolve to visible runtime sprites instead of debug placeholders`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        listOf(
            "actor.beast.rat" to "dark-v1/actors/actor_beast_rat.png",
            "actor.bandit.sentry" to "dark-v1/actors/actor_bandit_sentry.png",
            "actor.undead.bone_archer" to "dark-v1/actors/actor_undead_bone_archer.png",
            "actor.orc.raider" to "dark-v1/actors/actor_orc_raider.png",
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
            "zone.shattered_outpost.visual" to "dark-v1/portraits/zone_shattered_outpost_visual.png",
            "zone.shattered_outpost.icon" to "dark-v1/icons/zone_shattered_outpost_icon.png",
            "zone.greenwood_fringe.visual" to "dark-v1/portraits/zone_greenwood_fringe_visual.png",
            "zone.greenwood_fringe.icon" to "dark-v1/icons/zone_greenwood_fringe_icon.png",
            "zone.deep_iron_pit.visual" to "dark-v1/portraits/zone_deep_iron_pit_visual.png",
            "zone.deep_iron_pit.icon" to "dark-v1/icons/zone_deep_iron_pit_icon.png",
            "zone.grey_gate_depths.visual" to "dark-v1/portraits/zone_grey_gate_depths_visual.png",
            "zone.grey_gate_depths.icon" to "dark-v1/icons/zone_grey_gate_depths_icon.png",
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
                assets.visualResolver
                    .terrainVariantKeys(snapshot.mapCells.first().terrainVisualKey)
                    .forEach { key ->
                        assertTrue(loadedPaths.contains(assets.visualResolver.resolve(key).entry.rawOutputPath), key)
                    }
                assertTrue(
                    loadedPaths.contains(assets.visualResolver.resolve(DarkUiMapVisualKeys.RUINS_ROOM_MATERIAL_BREAKUP).entry.rawOutputPath),
                    DarkUiMapVisualKeys.RUINS_ROOM_MATERIAL_BREAKUP,
                )
                assertTrue(
                    loadedPaths.contains(assets.visualResolver.resolve(DarkUiMapVisualKeys.RUINS_ROOM_ART_PLATE_PROTOTYPE).entry.rawOutputPath),
                    DarkUiMapVisualKeys.RUINS_ROOM_ART_PLATE_PROTOTYPE,
                )
                assertTrue(
                    loadedPaths.contains(assets.visualResolver.resolve(DarkUiMapVisualKeys.RUINS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE).entry.rawOutputPath),
                    DarkUiMapVisualKeys.RUINS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE,
                )
                assertTrue(loadedPaths.contains(assets.visualResolver.resolve(snapshot.actors.first().visualKey).entry.rawOutputPath))
                assertTrue(loadedPaths.contains(assets.visualResolver.resolve(snapshot.overlays.first().visualKey).entry.rawOutputPath))
            } finally {
                assets.dispose()
            }
        }
    }

    @Test
    fun `non ruins accepted room art families preload their own room plate and topology source textures`() {
        withHeadlessGdx {
            val base = sampleRenderSnapshot()
            val families =
                listOf(
                    RoomArtPlateFamilyVisualKeys(
                        tilesetKey = DarkUiMapVisualKeys.FOREST_EDGE_TILESET,
                        groundKey = DarkUiMapVisualKeys.FOREST_EDGE_GROUND,
                        wallKey = DarkUiMapVisualKeys.FOREST_EDGE_WALL,
                        roomArtPlateKey = DarkUiMapVisualKeys.FOREST_EDGE_ROOM_ART_PLATE_PROTOTYPE,
                    ),
                    RoomArtPlateFamilyVisualKeys(
                        tilesetKey = DarkUiMapVisualKeys.MINE_TILESET,
                        groundKey = DarkUiMapVisualKeys.MINE_GROUND,
                        wallKey = DarkUiMapVisualKeys.MINE_WALL,
                        roomArtPlateKey = DarkUiMapVisualKeys.MINE_ROOM_ART_PLATE_PROTOTYPE,
                    ),
                    RoomArtPlateFamilyVisualKeys(
                        tilesetKey = DarkUiMapVisualKeys.SHADOW_DEPTHS_TILESET,
                        groundKey = DarkUiMapVisualKeys.SHADOW_DEPTHS_GROUND,
                        wallKey = DarkUiMapVisualKeys.SHADOW_DEPTHS_WALL,
                        roomArtPlateKey = DarkUiMapVisualKeys.SHADOW_DEPTHS_ROOM_ART_PLATE_PROTOTYPE,
                    ),
                )

            families.forEach { family ->
                val assets = ClientAssetBundleLoader.load()
                val strategy = ClientAssetLoadStrategy(assets)
                val snapshot = base.withRoomArtPlateFamily(family)

                try {
                    strategy.sessionLoad(snapshot)
                    strategy.warmCache(snapshot)

                    val loadedPaths = assets.textureRepository.loadedPaths()
                    assertTrue(
                        loadedPaths.contains(assets.visualResolver.resolve(family.roomArtPlateKey).entry.rawOutputPath),
                        family.roomArtPlateKey,
                    )
                    val topologySourceKey = requireNotNull(DarkUiMapVisualKeys.roomTopologySourceKeyFor(family))
                    assertTrue(
                        loadedPaths.contains(assets.visualResolver.resolve(topologySourceKey).entry.rawOutputPath),
                        topologySourceKey,
                    )
                    assertFalse(
                        loadedPaths.contains(assets.visualResolver.resolve(DarkUiMapVisualKeys.RUINS_ROOM_ART_PLATE_PROTOTYPE).entry.rawOutputPath),
                        family.tilesetKey,
                    )
                    assertFalse(
                        loadedPaths.contains(assets.visualResolver.resolve(DarkUiMapVisualKeys.RUINS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE).entry.rawOutputPath),
                        family.tilesetKey,
                    )
                    assertFalse(
                        loadedPaths.contains(assets.visualResolver.resolve(DarkUiMapVisualKeys.RUINS_ROOM_MATERIAL_BREAKUP).entry.rawOutputPath),
                        family.tilesetKey,
                    )
                } finally {
                    assets.dispose()
                }
            }
        }
    }

    @Test
    fun `merged sample pack manifests resolve exact keys to absolute filesystem paths`() {
        val assets = ClientAssetBundleLoader.load(contentPackSelection = samplePackSelection())

        try {
            val zoneVisual = assets.visualResolver.resolve("sample_flooded_relics.zone.flooded_reliquary.visual")
            val zoneAudio = assets.audioResolver.resolve("sample_flooded_relics.audio.zone.flooded_reliquary")

            assertEquals("sample_flooded_relics.zone.flooded_reliquary.visual", zoneVisual.resolvedKey)
            assertFalse(zoneVisual.fallbackUsed)
            assertFalse(zoneVisual.matchedByPrefix)
            assertTrue(Path.of(zoneVisual.entry.rawOutputPath).isAbsolute)
            assertEquals("sample_flooded_relics.audio.zone.flooded_reliquary", zoneAudio.resolvedKey)
            assertFalse(zoneAudio.fallbackUsed)
            assertFalse(zoneAudio.matchedByPrefix)
            assertTrue(Path.of(zoneAudio.entry.sourcePath).isAbsolute)
        } finally {
            assets.dispose()
        }
    }

    @Test
    fun `filesystem backed sample pack textures load when gdx is available`() {
        withHeadlessGdx {
            val assets = ClientAssetBundleLoader.load(contentPackSelection = samplePackSelection())

            try {
                listOf(
                    "sample_flooded_relics.zone.flooded_reliquary.visual",
                    "sample_flooded_relics.prop.reliquary_node.visual",
                    "sample_flooded_relics.item.tideglass_echo.visual",
                ).forEach { key ->
                    assets.textureRepository.preload(assets.visualResolver.resolve(key))
                }

                val loadedPaths = assets.textureRepository.loadedPaths()
                assertTrue(loadedPaths.all { path -> Path.of(path).isAbsolute })
                assertTrue(loadedPaths.any { path -> path.endsWith("phase4/pr09/zone_flooded_reliquary_visual.png") })
                assertTrue(loadedPaths.any { path -> path.endsWith("phase4/pr09/prop_reliquary_node_visual.png") })
                assertTrue(loadedPaths.any { path -> path.endsWith("phase4/pr09/item_tideglass_echo_visual.png") })
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

    private fun RenderSnapshot.withRoomArtPlateFamily(family: RoomArtPlateFamilyVisualKeys): RenderSnapshot =
        copy(
            metadata = metadata.copy(tilesetKey = family.tilesetKey),
            mapCells =
                mapCells.map { cell ->
                    cell.copy(
                        terrainVisualKey =
                            if (cell.terrainTypeId == "wall") {
                                family.wallKey
                            } else {
                                family.groundKey
                            },
                    )
                },
        )

    private fun visualManifestWithEntryFields(
        manifestVersion: Int,
        extraEntryFields: String,
    ): String =
        """
        {
          "manifestVersion": $manifestVersion,
          "styleTag": "test-style",
          "fallbackKey": "missing_visual",
          "entries": [
            {
              "key": "missing_visual",
              "category": "debug",
              "rawOutputPath": "debug/missing_visual.png",
              "footprint": "1x1",
              "pivotX": 0.5,
              "pivotY": 0.5,
              "tags": []$extraEntryFields
            }
          ]
        }
        """.trimIndent()

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

    private fun samplePackSelection(): ContentPackSelection {
        val repoRoot = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()
        val packRoot = repoRoot.resolve("examples/content-packs/sample.flooded_relics")
        return ContentPackSelection.of(packRoot)
    }

    private fun pr05OwnerInventory(): JsonObject {
        val repoRoot = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()
        return Json.parseToJsonElement(
            Files.readString(repoRoot.resolve("UI/sprite-sheets/pr05-owner-key-inventory.json")),
        ).jsonObject
    }

    private fun stringArray(
        payload: JsonObject,
        key: String,
    ): List<String> =
        (payload[key] as JsonArray).map { item -> item.jsonPrimitive.content }

    private fun alphaCoverage(image: BufferedImage): Double {
        var covered = 0
        val total = image.width * image.height
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val alpha = image.getRGB(x, y).ushr(24) and 0xFF
                if (alpha > 24) {
                    covered += 1
                }
            }
        }
        return covered.toDouble() / total.toDouble()
    }

    private fun pixelShare(
        image: BufferedImage,
        matches: (red: Int, green: Int, blue: Int) -> Boolean,
    ): Double {
        var matched = 0
        var covered = 0

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val argb = image.getRGB(x, y)
                val alpha = argb.ushr(24) and 0xFF
                if (alpha <= 24) {
                    continue
                }
                covered += 1
                val red = argb.ushr(16) and 0xFF
                val green = argb.ushr(8) and 0xFF
                val blue = argb and 0xFF
                if (matches(red, green, blue)) {
                    matched += 1
                }
            }
        }

        return matched.toDouble() / covered.toDouble()
    }

    private fun JsonObject.string(key: String): String =
        requireNotNull(this[key]) { "Missing '$key' in PR-05 owner inventory entry." }.jsonPrimitive.content
}
