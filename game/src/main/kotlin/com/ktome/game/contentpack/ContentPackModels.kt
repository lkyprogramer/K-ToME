package com.ktome.game.contentpack

import com.ktome.core.phase.PackId
import com.ktome.core.world.solvability.ContentRef
import java.nio.file.Path

data class PackDependency(
    val id: PackId,
    val versionRange: String,
) {
    init {
        require(versionRange.isNotBlank()) { "PackDependency.versionRange must not be blank." }
    }
}

enum class OverlayOp {
    ADD,
    REPLACE,
    APPEND,
    DENY,
}

data class OverlayEntry(
    val targetRef: ContentRef,
    val op: OverlayOp,
    val sourceFile: String,
    val fieldPath: String? = null,
    val mergePolicy: String? = null,
    val dedupeKey: String? = null,
) {
    init {
        require(sourceFile.isNotBlank()) { "OverlayEntry.sourceFile must not be blank." }
        require(fieldPath == null || fieldPath.isNotBlank()) { "OverlayEntry.fieldPath must not be blank when present." }
        require(mergePolicy == null || mergePolicy.isNotBlank()) { "OverlayEntry.mergePolicy must not be blank when present." }
        require(dedupeKey == null || dedupeKey.isNotBlank()) { "OverlayEntry.dedupeKey must not be blank when present." }
    }
}

data class ContentPackManifest(
    val id: PackId,
    val version: String,
    val schemaVersion: Int,
    val gameVersionRange: String,
    val namespace: String,
    val dependencies: List<PackDependency>,
    val overlays: List<OverlayEntry>,
    val localeBundles: List<String>,
    val visualManifest: String?,
    val audioManifest: String?,
) {
    init {
        require(version.isNotBlank()) { "ContentPackManifest.version must not be blank." }
        require(schemaVersion > 0) { "ContentPackManifest.schemaVersion must be positive." }
        require(gameVersionRange.isNotBlank()) { "ContentPackManifest.gameVersionRange must not be blank." }
        require(namespace.isNotBlank()) { "ContentPackManifest.namespace must not be blank." }
        require(localeBundles.all(String::isNotBlank)) { "ContentPackManifest.localeBundles must not contain blank paths." }
        require(visualManifest == null || visualManifest.isNotBlank()) {
            "ContentPackManifest.visualManifest must not be blank when present."
        }
        require(audioManifest == null || audioManifest.isNotBlank()) {
            "ContentPackManifest.audioManifest must not be blank when present."
        }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

data class DualPackScenario(
    val fixturePackId: PackId,
    val expectedOrder: List<PackId>,
    val expectedOps: List<OverlayOp> = emptyList(),
) {
    init {
        require(expectedOrder.isNotEmpty()) { "DualPackScenario.expectedOrder must not be empty." }
    }
}

data class ContentPackHarnessSpec(
    val packId: PackId,
    val harnessSeeds: List<Long>,
    val dualPackScenarios: List<DualPackScenario> = emptyList(),
    val fixtureOrder: List<String> = emptyList(),
    val overlayContractVersion: Int = 1,
) {
    init {
        require(harnessSeeds.isNotEmpty()) { "ContentPackHarnessSpec.harnessSeeds must not be empty." }
        require(fixtureOrder.distinct().size == fixtureOrder.size) {
            "ContentPackHarnessSpec.fixtureOrder must not contain duplicates."
        }
        require(fixtureOrder.none(String::isBlank)) {
            "ContentPackHarnessSpec.fixtureOrder must not contain blank values."
        }
        require(overlayContractVersion > 0) { "ContentPackHarnessSpec.overlayContractVersion must be positive." }
    }
}

data class ContentPackSelection(
    val activePackRoots: List<Path> = emptyList(),
    val availablePackRoots: List<Path> = activePackRoots,
) {
    init {
        require(activePackRoots.distinct().size == activePackRoots.size) {
            "ContentPackSelection.activePackRoots must not contain duplicates."
        }
        require(availablePackRoots.distinct().size == availablePackRoots.size) {
            "ContentPackSelection.availablePackRoots must not contain duplicates."
        }
        require(activePackRoots.all { packRoot -> packRoot in availablePackRoots }) {
            "ContentPackSelection.activePackRoots must be a subset of availablePackRoots."
        }
    }

    val isEmpty: Boolean
        get() = activePackRoots.isEmpty()

    companion object {
        val EMPTY: ContentPackSelection = ContentPackSelection()

        fun of(vararg packRoots: Path): ContentPackSelection =
            ContentPackSelection(
                activePackRoots = packRoots.toList(),
                availablePackRoots = packRoots.toList(),
            )
    }
}
