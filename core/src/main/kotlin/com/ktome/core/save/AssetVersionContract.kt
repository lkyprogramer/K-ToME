package com.ktome.core.save

import kotlinx.serialization.Serializable

@Serializable
data class AssetVersionContract(
    val styleVersion: String,
    val visualManifestVersion: Int,
    val audioManifestVersion: Int,
    val assetPipelineVersion: Int,
) {
    init {
        require(styleVersion.isNotBlank()) { "styleVersion must not be blank." }
        require(visualManifestVersion > 0) { "visualManifestVersion must be positive." }
        require(audioManifestVersion > 0) { "audioManifestVersion must be positive." }
        require(assetPipelineVersion > 0) { "assetPipelineVersion must be positive." }
    }

    companion object {
        val CURRENT: AssetVersionContract =
            AssetVersionContract(
                styleVersion = "phase1-ascii",
                visualManifestVersion = 1,
                audioManifestVersion = 1,
                assetPipelineVersion = 1,
            )
    }
}

class AssetVersionMismatchException(
    expected: AssetVersionContract,
    actual: AssetVersionContract,
) : IllegalStateException(
        "Asset contract mismatch. Expected style=${expected.styleVersion}, visual=${expected.visualManifestVersion}, audio=${expected.audioManifestVersion}, pipeline=${expected.assetPipelineVersion}, " +
            "but found style=${actual.styleVersion}, visual=${actual.visualManifestVersion}, audio=${actual.audioManifestVersion}, pipeline=${actual.assetPipelineVersion}.",
    )

class AssetVersionGate(
    private val expected: AssetVersionContract = AssetVersionContract.CURRENT,
) {
    fun requireCompatible(actual: AssetVersionContract) {
        if (actual != expected) {
            throw AssetVersionMismatchException(expected = expected, actual = actual)
        }
    }
}
