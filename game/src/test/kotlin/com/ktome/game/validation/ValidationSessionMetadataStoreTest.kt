package com.ktome.game.validation

import com.ktome.core.save.SaveManager
import com.ktome.core.save.InvalidSaveException
import com.ktome.game.GameModule
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.elites.BossVariantSelectionMode
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ValidationSessionMetadataStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `persisted validation metadata restores preset capabilities and content packs`() {
        val saveManager = SaveManager(tempDir.resolve("validation/save"))
        val samplePackRoot =
            Path.of(System.getProperty("ktome.repo.root", "."))
                .toAbsolutePath()
                .normalize()
                .resolve("examples/content-packs/sample.flooded_relics")
        val baseOptions =
            validationSessionOptionsForPreset(
                preset = ValidationPreset.CONTENT_PACK,
                contentPackSelection = ContentPackSelection.of(samplePackRoot),
            )
        val options =
            baseOptions.copy(
                foundationConfig =
                    baseOptions.foundationConfig.copy(
                        seed = 20269999L,
                        bossVariantSelectionMode = BossVariantSelectionMode.FORCE_AVAILABLE,
                        preferredBossVariantId = "boss.variant.grey_crown",
                    ),
                capabilities =
                    ValidationCapabilitySet(
                        restart = true,
                        travel = false,
                        recovery = true,
                        encounter = false,
                        terrain = true,
                        rewardAndItem = false,
                        discovery = true,
                    ),
            )
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = saveManager,
                    options = options,
                ),
            )

        assertTrue(session.saveOnExit())

        val restored = requireNotNull(loadPersistedValidationSessionOptions(saveManager))
        assertEquals(ValidationPreset.CONTENT_PACK, restored.preset)
        assertEquals(options.foundationConfig.seed, restored.foundationConfig.seed)
        assertEquals(options.seedCorpus, restored.seedCorpus)
        assertEquals(options.foundationConfig.bossVariantSelectionMode, restored.foundationConfig.bossVariantSelectionMode)
        assertEquals(options.foundationConfig.preferredBossVariantId, restored.foundationConfig.preferredBossVariantId)
        assertEquals(options.capabilities, restored.capabilities)
        assertEquals(options.contentPackSelection, restored.contentPackSelection)
    }

    @Test
    fun `legacy metadata without phase4 v4 capability restores default enabled`() {
        val saveManager = SaveManager(tempDir.resolve("validation/legacy-save"))
        val options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF)
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = saveManager,
                    options = options,
                ),
            )

        assertTrue(session.saveOnExit())
        val metadataFile = saveManager.savePath().parent.resolve(ValidationSessionMetadataStore.DEFAULT_FILE_NAME)
        Files.writeString(
            metadataFile,
            Files.readString(metadataFile).replace(",\"phase4V4Fast\":true", ""),
        )

        val restored = requireNotNull(loadPersistedValidationSessionOptions(saveManager))
        assertTrue(restored.capabilities.phase4V4Fast)
    }

    @Test
    fun `existing validation save without metadata fails fast`() {
        val saveManager = SaveManager(tempDir.resolve("validation/save"))
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = saveManager,
                    options = validationSessionOptionsForPreset(ValidationPreset.HIDDEN_CONTENT),
                ),
            )

        assertTrue(session.saveOnExit())
        Files.delete(saveManager.savePath().parent.resolve(ValidationSessionMetadataStore.DEFAULT_FILE_NAME))

        assertThrows(InvalidSaveException::class.java) {
            loadPersistedValidationSessionOptions(saveManager)
        }
    }
}
