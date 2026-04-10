package com.ktome.game.contentpack

import com.ktome.core.phase.PackId
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ContentPackRuntimeResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `resolver orders dependency pack before replace pack and preserves manifest versions`() {
        val selection = ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.replacePackId))

        val resolved = ContentPackRuntimeResolver.resolve(selection)

        assertEquals(
            listOf("fixture.add_monster", "fixture.replace_monster"),
            resolved.activePackIds.map(PackId::value),
        )
        assertEquals("1.0.0", resolved.activePackManifestVersions.getValue(PackId("fixture.add_monster")))
        assertEquals("1.0.0", resolved.activePackManifestVersions.getValue(PackId("fixture.replace_monster")))
    }

    @Test
    fun `resolver rejects namespace collisions and keeps structured diagnostic codes`() {
        val selection =
            ContentPackFixtureCatalog.availableSelection(
                listOf(
                    ContentPackFixtureCatalog.namespaceCollisionLeftId,
                    ContentPackFixtureCatalog.namespaceCollisionRightId,
                ),
            )

        val exception =
            org.junit.jupiter.api.assertThrows<ContentPackLoadException> {
                ContentPackRuntimeResolver.resolve(selection)
            }

        assertEquals(setOf("content-pack.namespace.collision"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }

    @Test
    fun `resolver rejects dependency version range conflicts before runtime apply`() {
        val selection = ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.versionConflictPackId))

        val exception =
            assertThrows<ContentPackLoadException> {
                ContentPackRuntimeResolver.resolve(selection)
            }

        assertEquals(setOf("content-pack.version-range.conflict"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }

    @Test
    fun `resolver fails fast on schema version mismatch`() {
        val packRoot = tempDir.resolve("fixture.schema_mismatch")
        packRoot.toFile().mkdirs()
        packRoot.resolve("manifest.yaml").writeText(
            """
            id: fixture.schema_mismatch
            version: 1.0.0
            schemaVersion: 2
            gameVersionRange: ">=0.1.0 <0.2.0"
            namespace: fixture_schema_mismatch
            dependencies: []
            overlays: []
            """.trimIndent(),
        )
        val selection = ContentPackSelection.of(packRoot)

        val exception =
            assertThrows<ContentPackLoadException> {
                ContentPackRuntimeResolver.resolve(selection)
            }

        assertEquals(setOf("content-pack.schema-version.mismatch"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }

    @Test
    fun `resolver rejects append targets outside the phase4 whitelist`() {
        val packRoot = tempDir.resolve("fixture.append_invalid")
        packRoot.resolve("data/monsters").toFile().mkdirs()
        packRoot.resolve("manifest.yaml").writeText(
            """
            id: fixture.append_invalid
            version: 1.0.0
            schemaVersion: 1
            gameVersionRange: ">=0.1.0 <0.2.0"
            namespace: fixture_append_invalid
            dependencies: []
            overlays:
              - targetRef:
                  registry: monster
                  id: beast.rat
                op: APPEND
                sourceFile: data/monsters/beast.rat.yaml
                fieldPath: tags
                mergePolicy: APPEND_LIST
            """.trimIndent(),
        )
        packRoot.resolve("data/monsters/beast.rat.yaml").writeText(
            """
            monsters:
              - id: beast.rat
                nameKey: beast.rat.name
                descKey: beast.rat.desc
                visualKey: actor.beast.rat
                iconKey: actor.beast.rat
                audioProfile: audio.beast.rat
                schemaVersion: 2
                tags: [monster]
                archetype: beast
                glyph: r
                colorHex: "#ffffff"
                stats: { strength: 1, dexterity: 1, constitution: 1, willpower: 1 }
                baseHp: 1
                baseAttack: 1
                baseDefense: 1
                baseAccuracy: 1
                baseEvasion: 1
                speed: 1000
                ai: melee
                aiProfileId: ai.basic.melee
                lootProfileId: loot.foundation.common
                resistances: {}
                talents: {}
                expReward: 1
                spawnFloors: [1]
                spawnWeight: 1
            """.trimIndent(),
        )

        val exception =
            assertThrows<ContentPackLoadException> {
                ContentPackRuntimeResolver.resolve(ContentPackSelection.of(packRoot))
            }

        assertEquals(setOf("content-pack.overlay.append-target-forbidden"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }

    @Test
    fun `resolver allows loot profile append against v3 whitelist fields`() {
        val packRoot = tempDir.resolve("fixture.append_loot_profile_bias")
        packRoot.resolve("data/loot").toFile().mkdirs()
        packRoot.resolve("manifest.yaml").writeText(
            """
            id: fixture.append_loot_profile_bias
            version: 1.0.0
            schemaVersion: 1
            gameVersionRange: ">=0.1.0 <0.2.0"
            namespace: fixture_append_loot_profile_bias
            dependencies: []
            overlays:
              - targetRef:
                  registry: loot_profile
                  id: loot.foundation.common
                op: APPEND
                sourceFile: data/loot/loot.foundation.common.append.yaml
                fieldPath: itemTagFilter
                mergePolicy: APPEND_LIST
                dedupeKey: tag
            """.trimIndent(),
        )
        packRoot.resolve("data/loot/loot.foundation.common.append.yaml").writeText(
            """
            lootProfiles:
              - id: loot.foundation.common
                schemaVersion: 3
                tags: [loot, common, optional]
                poolStrategy: FIXED_LIST
                rewardBudget: 6
                itemIds: [healing_potion]
                itemTagFilter: [greenwood_fringe]
            """.trimIndent(),
        )

        val resolved = ContentPackRuntimeResolver.resolve(ContentPackSelection.of(packRoot))

        assertEquals(listOf("fixture.append_loot_profile_bias"), resolved.activePackIds.map(PackId::value))
    }

    @Test
    fun `resolver rejects deny overlays against non optional targets`() {
        val packRoot = tempDir.resolve("fixture.deny_non_optional")
        packRoot.resolve("data/monsters").toFile().mkdirs()
        packRoot.resolve("manifest.yaml").writeText(
            """
            id: fixture.deny_non_optional
            version: 1.0.0
            schemaVersion: 1
            gameVersionRange: ">=0.1.0 <0.2.0"
            namespace: fixture_deny_non_optional
            dependencies: []
            overlays:
              - targetRef:
                  registry: monster
                  id: beast.rat
                op: DENY
                sourceFile: data/monsters/beast.rat.yaml
            """.trimIndent(),
        )
        packRoot.resolve("data/monsters/beast.rat.yaml").writeText("monsters: []")

        val exception =
            assertThrows<ContentPackLoadException> {
                ContentPackRuntimeResolver.resolve(ContentPackSelection.of(packRoot))
            }

        assertEquals(setOf("content-pack.overlay.deny-non-optional-target"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }

    @Test
    fun `resolver rejects replace overlays that target a missing entry`() {
        val packRoot = tempDir.resolve("fixture.replace_missing_target")
        packRoot.resolve("data/monsters").toFile().mkdirs()
        packRoot.resolve("manifest.yaml").writeText(
            """
            id: fixture.replace_missing_target
            version: 1.0.0
            schemaVersion: 1
            gameVersionRange: ">=0.1.0 <0.2.0"
            namespace: fixture_replace_missing_target
            dependencies: []
            overlays:
              - targetRef:
                  registry: monster
                  id: fixture.replace_missing_target.ghost_rat
                op: REPLACE
                sourceFile: data/monsters/ghost_rat.yaml
            """.trimIndent(),
        )
        packRoot.resolve("data/monsters/ghost_rat.yaml").writeText(
            """
            monsters:
              - id: fixture.replace_missing_target.ghost_rat
                nameKey: fixture.replace_missing_target.monster.ghost_rat.name
                descKey: fixture.replace_missing_target.monster.ghost_rat.desc
                visualKey: fixture.replace_missing_target.actor.ghost_rat
                iconKey: fixture.replace_missing_target.actor.ghost_rat
                audioProfile: fixture.replace_missing_target.audio.ghost_rat
                schemaVersion: 2
                tags: [monster]
                archetype: beast
                glyph: r
                colorHex: "#ffffff"
                stats: { strength: 1, dexterity: 1, constitution: 1, willpower: 1 }
                baseHp: 1
                baseAttack: 1
                baseDefense: 1
                baseAccuracy: 1
                baseEvasion: 1
                speed: 1000
                ai: melee
                aiProfileId: ai.basic.melee
                lootProfileId: loot.foundation.common
                resistances: {}
                talents: {}
                expReward: 1
                spawnFloors: [1]
                spawnWeight: 1
            """.trimIndent(),
        )

        val exception =
            assertThrows<ContentPackLoadException> {
                ContentPackRuntimeResolver.resolve(ContentPackSelection.of(packRoot))
            }

        assertEquals(setOf("content-pack.overlay.replace-missing-target"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }

    @Test
    fun `resolver rejects manifest resource paths that escape the pack root`() {
        val packRoot = tempDir.resolve("fixture.path_escape")
        packRoot.toFile().mkdirs()
        tempDir.resolve("en-US.json").writeText("""{"fixture.path.escape":"bad"}""")
        packRoot.resolve("manifest.yaml").writeText(
            """
            id: fixture.path_escape
            version: 1.0.0
            schemaVersion: 1
            gameVersionRange: ">=0.1.0 <0.2.0"
            namespace: fixture_path_escape
            dependencies: []
            overlays: []
            localeBundles:
              - ../en-US.json
            """.trimIndent(),
        )

        val exception =
            assertThrows<ContentPackLoadException> {
                ContentPackRuntimeResolver.resolve(ContentPackSelection.of(packRoot))
            }

        assertEquals(setOf("content-pack.resource.path-outside-pack"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }

    @Test
    fun `version range parser supports bounded ranges and exact matches`() {
        val bounded = VersionRangeParser.parse(">=0.1.0 <0.2.0")
        val exact = VersionRangeParser.parse("=1.0.0")

        assertTrue(bounded.matches(VersionRangeParser.parseVersion("0.1.0")))
        assertTrue(bounded.matches(VersionRangeParser.parseVersion("0.1.9")))
        assertTrue(!bounded.matches(VersionRangeParser.parseVersion("0.2.0")))
        assertTrue(exact.matches(VersionRangeParser.parseVersion("1.0.0")))
        assertTrue(!exact.matches(VersionRangeParser.parseVersion("1.0.1")))
        assertThrows<IllegalArgumentException> { VersionRangeParser.parseVersion(">=1.0.0") }
    }
}
