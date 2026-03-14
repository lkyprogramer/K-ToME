package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AssetVersionBoundaryTest {
    @Test
    fun `asset version gate rejects mismatched manifests outside the save contract`() {
        val gate = AssetVersionGate()

        assertThrows(AssetVersionMismatchException::class.java) {
            gate.requireCompatible(
                AssetVersionContract.CURRENT.copy(visualManifestVersion = AssetVersionContract.CURRENT.visualManifestVersion + 1),
            )
        }
    }

    @Test
    fun `save snapshot json does not embed asset version fields`() {
        val encoded = SaveCodec().encode(SaveFixtures.emptyScene())

        assertFalse(encoded.contains("visualManifestVersion"))
        assertFalse(encoded.contains("audioManifestVersion"))
        assertFalse(encoded.contains("styleVersion"))
        assertFalse(encoded.contains("assetPipelineVersion"))
    }
}
