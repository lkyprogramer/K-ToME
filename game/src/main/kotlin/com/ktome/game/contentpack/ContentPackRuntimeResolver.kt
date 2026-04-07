package com.ktome.game.contentpack

import com.ktome.core.phase.PackId
import com.ktome.game.i18n.GameLocale
import com.ktome.game.data.DataLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import org.yaml.snakeyaml.Yaml

enum class ContentPackPrecedenceBucket {
    BASE_GAME,
    DEPENDENCY_PACK,
    ACTIVE_PACK,
}

data class ResolvedContentPack(
    val manifest: ContentPackManifest,
    val packRoot: Path,
    val precedenceBucket: ContentPackPrecedenceBucket,
) {
    val id: PackId
        get() = manifest.id

    val namespace: String
        get() = manifest.namespace

    val manifestPath: Path
        get() = packRoot.resolve(MANIFEST_FILE_NAME)

    fun resolvePath(relativePath: String): Path = packRoot.resolve(relativePath).normalize()

    fun localeBundlePaths(): Map<GameLocale, List<Path>> =
        manifest.localeBundles.groupBy(
            keySelector = { relativePath ->
                GameLocale.fromId(requireNotNull(resolvePath(relativePath).fileName?.nameWithoutExtension) {
                    "Locale bundle '$relativePath' must point to a file."
                })
            },
            valueTransform = { relativePath -> resolvePath(relativePath) },
        )

    companion object {
        const val MANIFEST_FILE_NAME: String = "manifest.yaml"
    }
}

data class ResolvedContentPackSelection(
    val orderedPacks: List<ResolvedContentPack>,
    val activePackIds: List<PackId>,
    val activePackManifestVersions: Map<PackId, String>,
) {
    fun isEmpty(): Boolean = orderedPacks.isEmpty()

    companion object {
        val EMPTY: ResolvedContentPackSelection =
            ResolvedContentPackSelection(
                orderedPacks = emptyList(),
                activePackIds = emptyList(),
                activePackManifestVersions = emptyMap(),
            )
    }
}

object ContentPackRuntimeResolver {
    private val yaml = Yaml()

    fun resolve(
        selection: ContentPackSelection,
        gameVersion: String = GameBuildVersion.current(),
    ): ResolvedContentPackSelection {
        if (selection.isEmpty) {
            return ResolvedContentPackSelection.EMPTY
        }

        val diagnostics = mutableListOf<ContentPackDiagnostic>()
        val activeRootSet = selection.activePackRoots.map(Path::normalize).toSet()
        val availableRoots = selection.availablePackRoots.map(Path::normalize)
        val availableById = linkedMapOf<PackId, ContentPackManifestFile>()
        availableRoots.forEach { packRoot ->
            val manifestFile = parseManifestFile(packRoot = packRoot, diagnostics = diagnostics)
            val previous = availableById.putIfAbsent(manifestFile.manifest.id, manifestFile)
            if (previous != null) {
                diagnostics.addDiagnostic(
                    code = "content-pack.available.duplicate-id",
                    message = "Multiple available pack roots declare id '${manifestFile.manifest.id.value}'.",
                    packId = manifestFile.manifest.id,
                    sourcePath = packRoot,
                    details =
                        mapOf(
                            "firstRoot" to previous.packRoot.toString(),
                            "secondRoot" to packRoot.toString(),
                        ),
                )
            }
        }
        throwIfErrors(diagnostics)

        val directPacks =
            selection.activePackRoots.map { activeRoot ->
                val normalizedRoot = activeRoot.normalize()
                availableById.values.firstOrNull { file -> file.packRoot == normalizedRoot }
                    ?: error("Active pack root '$normalizedRoot' is missing from the available pack set.")
            }
        val orderedIds = resolvePackOrder(directPacks = directPacks, availableById = availableById, diagnostics = diagnostics)
        val currentGameVersion = parseGameVersion(gameVersion = gameVersion, diagnostics = diagnostics)
        val orderedPacks =
            orderedIds.map { packId ->
                val manifestFile = requireNotNull(availableById[packId]) { "Missing manifest file for pack '${packId.value}'." }
                val isActiveRoot = manifestFile.packRoot in activeRootSet
                validateManifestCompatibility(
                    manifestFile = manifestFile,
                    currentGameVersion = currentGameVersion,
                    diagnostics = diagnostics,
                )
                ResolvedContentPack(
                    manifest = manifestFile.manifest,
                    packRoot = manifestFile.packRoot,
                    precedenceBucket =
                        if (isActiveRoot) {
                            ContentPackPrecedenceBucket.ACTIVE_PACK
                        } else {
                            ContentPackPrecedenceBucket.DEPENDENCY_PACK
                        },
                )
            }
        validateDependencyVersionRanges(orderedPacks = orderedPacks, availableById = availableById, diagnostics = diagnostics)
        validateOverlayLintTargets(orderedPacks = orderedPacks, diagnostics = diagnostics)
        validateNamespaceUniqueness(orderedPacks, diagnostics)
        validateSamePriorityDuplicateTargets(orderedPacks, diagnostics)
        throwIfErrors(diagnostics)
        return ResolvedContentPackSelection(
            orderedPacks = orderedPacks,
            activePackIds = orderedPacks.map(ResolvedContentPack::id),
            activePackManifestVersions =
                orderedPacks.associateTo(linkedMapOf()) { pack ->
                    pack.id to pack.manifest.version
                },
        )
    }

    private fun parseManifestFile(
        packRoot: Path,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ): ContentPackManifestFile {
        val manifestPath = packRoot.resolve(ResolvedContentPack.MANIFEST_FILE_NAME)
        if (!Files.exists(manifestPath)) {
            diagnostics.addDiagnostic(
                code = "content-pack.manifest.missing",
                message = "Pack root '$packRoot' does not contain ${ResolvedContentPack.MANIFEST_FILE_NAME}.",
                sourcePath = manifestPath,
            )
            throwIfErrors(diagnostics)
        }
        val rawRoot = Files.newBufferedReader(manifestPath).use { reader -> yaml.load<Map<String, Any?>>(reader) }
        val root = rawRoot ?: emptyMap()
        val manifest =
            try {
                root.toContentPackManifest()
            } catch (exception: IllegalArgumentException) {
                diagnostics.addDiagnostic(
                    code = "content-pack.manifest.invalid",
                    message = exception.message ?: "Invalid content-pack manifest.",
                    sourcePath = manifestPath,
                )
                throwIfErrors(diagnostics)
                error("Unreachable")
            }
        validateSchemaVersion(packRoot = packRoot, manifest = manifest, diagnostics = diagnostics)
        validateManifestFilesExist(packRoot = packRoot, manifest = manifest, diagnostics = diagnostics)
        validateNamespace(packRoot = packRoot, manifest = manifest, diagnostics = diagnostics)
        validateOverlayShape(packRoot = packRoot, manifest = manifest, diagnostics = diagnostics)
        return ContentPackManifestFile(packRoot = packRoot, manifest = manifest)
    }

    private fun validateSchemaVersion(
        packRoot: Path,
        manifest: ContentPackManifest,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        if (manifest.schemaVersion != ContentPackManifest.SCHEMA_VERSION) {
            diagnostics.addDiagnostic(
                code = "content-pack.schema-version.mismatch",
                message =
                    "Pack '${manifest.id.value}' must use schemaVersion ${ContentPackManifest.SCHEMA_VERSION}, " +
                        "got ${manifest.schemaVersion}.",
                packId = manifest.id,
                sourcePath = packRoot.resolve(ResolvedContentPack.MANIFEST_FILE_NAME),
                details = mapOf("expectedSchemaVersion" to ContentPackManifest.SCHEMA_VERSION.toString()),
            )
        }
    }

    private fun validateManifestFilesExist(
        packRoot: Path,
        manifest: ContentPackManifest,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        val manifestPath = packRoot.resolve(ResolvedContentPack.MANIFEST_FILE_NAME)
        manifest.localeBundles.forEach { relativePath ->
            val resolved = packRoot.resolve(relativePath).normalize()
            if (!Files.exists(resolved)) {
                diagnostics.addDiagnostic(
                    code = "content-pack.resource.locale-missing",
                    message = "Pack '${manifest.id.value}' references a missing locale bundle '$relativePath'.",
                    packId = manifest.id,
                    sourcePath = manifestPath,
                )
            }
        }
        listOfNotNull(manifest.visualManifest, manifest.audioManifest).forEach { relativePath ->
            val resolved = packRoot.resolve(relativePath).normalize()
            if (!Files.exists(resolved)) {
                diagnostics.addDiagnostic(
                    code = "content-pack.resource.manifest-missing",
                    message = "Pack '${manifest.id.value}' references a missing resource manifest '$relativePath'.",
                    packId = manifest.id,
                    sourcePath = manifestPath,
                )
            }
        }
        manifest.overlays.forEach { overlay ->
            val resolved = packRoot.resolve(overlay.sourceFile).normalize()
            if (!Files.exists(resolved)) {
                diagnostics.addDiagnostic(
                    code = "content-pack.overlay.source-missing",
                    message = "Overlay '${overlay.op.name}' for '${overlay.targetRef.registry.value}:${overlay.targetRef.id}' points to missing source file '${overlay.sourceFile}'.",
                    packId = manifest.id,
                    targetRef = overlay.targetRef,
                    sourcePath = manifestPath,
                )
            }
        }
    }

    private fun validateNamespace(
        packRoot: Path,
        manifest: ContentPackManifest,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        val expectedNamespace = normalizeNamespace(manifest.id)
        if (manifest.namespace != expectedNamespace) {
            diagnostics.addDiagnostic(
                code = "content-pack.namespace.mismatch",
                message = "Pack '${manifest.id.value}' must use namespace '$expectedNamespace', got '${manifest.namespace}'.",
                packId = manifest.id,
                sourcePath = packRoot.resolve(ResolvedContentPack.MANIFEST_FILE_NAME),
            )
        }
    }

    private fun validateOverlayShape(
        packRoot: Path,
        manifest: ContentPackManifest,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        val manifestPath = packRoot.resolve(ResolvedContentPack.MANIFEST_FILE_NAME)
        manifest.overlays.forEach { overlay ->
            when (overlay.op) {
                OverlayOp.ADD,
                OverlayOp.REPLACE,
                -> {
                    if (overlay.fieldPath != null || overlay.mergePolicy != null || overlay.dedupeKey != null) {
                        diagnostics.addDiagnostic(
                            code = "content-pack.overlay.runtime-shape",
                            message = "Runtime overlay '${overlay.op.name}' must stay whole-entry and must not declare fieldPath/mergePolicy/dedupeKey.",
                            packId = manifest.id,
                            targetRef = overlay.targetRef,
                            sourcePath = manifestPath,
                        )
                    }
                }

                OverlayOp.APPEND -> {
                    if (overlay.fieldPath == null || overlay.mergePolicy == null) {
                        diagnostics.addDiagnostic(
                            code = "content-pack.overlay.append-shape",
                            message = "APPEND overlays must declare fieldPath and mergePolicy for lint/harness validation.",
                            packId = manifest.id,
                            targetRef = overlay.targetRef,
                            sourcePath = manifestPath,
                        )
                    }
                }

                OverlayOp.DENY -> Unit
            }
        }
    }

    private fun resolvePackOrder(
        directPacks: List<ContentPackManifestFile>,
        availableById: Map<PackId, ContentPackManifestFile>,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ): List<PackId> {
        val orderedIds = mutableListOf<PackId>()
        val stateById = mutableMapOf<PackId, VisitState>()
        val activeRootIds = directPacks.map { manifestFile -> manifestFile.manifest.id }.toSet()

        fun visit(packId: PackId, stack: MutableList<PackId>) {
            when (stateById[packId]) {
                VisitState.COMPLETE -> return
                VisitState.ACTIVE -> {
                    val cycle = (stack + packId).joinToString(separator = " -> ") { id -> id.value }
                    diagnostics.addDiagnostic(
                        code = "content-pack.dependency.cycle",
                        message = "Dependency cycle detected: $cycle.",
                        packId = packId,
                    )
                    return
                }

                null -> Unit
            }

            val manifestFile = availableById[packId]
            if (manifestFile == null) {
                val requester = stack.lastOrNull()
                diagnostics.addDiagnostic(
                    code = "content-pack.dependency.missing",
                    message = "Missing dependency '${packId.value}'.",
                    packId = requester,
                    details = requester?.let { id -> mapOf("missingDependency" to packId.value, "requester" to id.value) }.orEmpty(),
                )
                return
            }

            stateById[packId] = VisitState.ACTIVE
            stack.add(packId)
            manifestFile.manifest.dependencies.forEach { dependency ->
                visit(dependency.id, stack)
            }
            stack.removeLast()
            stateById[packId] = VisitState.COMPLETE
            if (packId !in orderedIds) {
                orderedIds += packId
            }
        }

        directPacks.forEach { directPack -> visit(directPack.manifest.id, mutableListOf()) }
        val activeOrderedIds = orderedIds.filter { packId -> packId in activeRootIds }
        if (activeOrderedIds.size != activeRootIds.size) {
            val missing = activeRootIds - activeOrderedIds.toSet()
            missing.forEach { packId ->
                diagnostics.addDiagnostic(
                    code = "content-pack.selection.unresolved-root",
                    message = "Active root pack '${packId.value}' could not be resolved.",
                    packId = packId,
                )
            }
        }
        return orderedIds
    }

    private fun parseGameVersion(
        gameVersion: String,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ): SemanticVersion =
        try {
            VersionRangeParser.parseVersion(gameVersion)
        } catch (exception: IllegalArgumentException) {
            diagnostics.addDiagnostic(
                code = "content-pack.game-version.invalid",
                message = exception.message ?: "Invalid build version '$gameVersion'.",
                details = mapOf("gameVersion" to gameVersion),
            )
            throwIfErrors(diagnostics)
            error("Unreachable")
        }

    private fun validateManifestCompatibility(
        manifestFile: ContentPackManifestFile,
        currentGameVersion: SemanticVersion,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        val manifest = manifestFile.manifest
        val versionRange =
            try {
                VersionRangeParser.parse(manifest.gameVersionRange)
            } catch (exception: IllegalArgumentException) {
                diagnostics.addDiagnostic(
                    code = "content-pack.version-range.invalid",
                    message = exception.message ?: "Invalid version range '${manifest.gameVersionRange}'.",
                    packId = manifest.id,
                    sourcePath = manifestFile.packRoot.resolve(ResolvedContentPack.MANIFEST_FILE_NAME),
                )
                return
            }
        if (!versionRange.matches(currentGameVersion)) {
            diagnostics.addDiagnostic(
                code = "content-pack.version-range.conflict",
                message =
                    "Pack '${manifest.id.value}' requires gameVersionRange '${manifest.gameVersionRange}', " +
                        "but current build version is '$currentGameVersion'.",
                packId = manifest.id,
                sourcePath = manifestFile.packRoot.resolve(ResolvedContentPack.MANIFEST_FILE_NAME),
                details = mapOf("currentGameVersion" to currentGameVersion.toString()),
            )
        }
    }

    private fun validateNamespaceUniqueness(
        orderedPacks: List<ResolvedContentPack>,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        orderedPacks
            .groupBy(ResolvedContentPack::namespace)
            .filterValues { packs -> packs.size > 1 }
            .forEach { (namespace, packs) ->
                packs.forEach { pack ->
                    diagnostics.addDiagnostic(
                        code = "content-pack.namespace.collision",
                        message = "Namespace '$namespace' is claimed by multiple active packs.",
                        packId = pack.id,
                        sourcePath = pack.manifestPath,
                    )
                }
            }
    }

    private fun validateDependencyVersionRanges(
        orderedPacks: List<ResolvedContentPack>,
        availableById: Map<PackId, ContentPackManifestFile>,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        orderedPacks.forEach { pack ->
            pack.manifest.dependencies.forEach { dependency ->
                val dependencyManifest = availableById[dependency.id]?.manifest ?: return@forEach
                val requiredRange =
                    try {
                        VersionRangeParser.parse(dependency.versionRange)
                    } catch (exception: IllegalArgumentException) {
                        diagnostics.addDiagnostic(
                            code = "content-pack.version-range.invalid",
                            message = exception.message ?: "Invalid dependency versionRange '${dependency.versionRange}'.",
                            packId = pack.id,
                            sourcePath = pack.manifestPath,
                            details = mapOf("dependencyId" to dependency.id.value),
                        )
                        return@forEach
                    }
                val dependencyVersion =
                    try {
                        VersionRangeParser.parseVersion(dependencyManifest.version)
                    } catch (exception: IllegalArgumentException) {
                        diagnostics.addDiagnostic(
                            code = "content-pack.version.invalid",
                            message = exception.message ?: "Invalid dependency version '${dependencyManifest.version}'.",
                            packId = dependency.id,
                            sourcePath = pack.manifestPath,
                            details = mapOf("requesterPackId" to pack.id.value),
                        )
                        return@forEach
                    }
                if (!requiredRange.matches(dependencyVersion)) {
                    diagnostics.addDiagnostic(
                        code = "content-pack.version-range.conflict",
                        message =
                            "Pack '${pack.id.value}' requires dependency '${dependency.id.value}' to match '${dependency.versionRange}', " +
                                "but found version '${dependencyManifest.version}'.",
                        packId = pack.id,
                        sourcePath = pack.manifestPath,
                        details =
                            mapOf(
                                "dependencyId" to dependency.id.value,
                                "requiredVersionRange" to dependency.versionRange,
                                "actualDependencyVersion" to dependencyManifest.version,
                            ),
                    )
                }
            }
        }
    }

    private fun validateSamePriorityDuplicateTargets(
        orderedPacks: List<ResolvedContentPack>,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        val overlaysByBucket =
            orderedPacks
                .groupBy(ResolvedContentPack::precedenceBucket)
                .mapValues { (_, packs) ->
                    packs.flatMap { pack -> pack.manifest.overlays.map { overlay -> pack to overlay } }
                }
        overlaysByBucket.forEach { (bucket, overlays) ->
            overlays
                .groupBy { (_, overlay) -> overlay.targetRef }
                .filterValues { entries -> entries.size > 1 }
                .forEach { (targetRef, entries) ->
                    if (bucket == ContentPackPrecedenceBucket.BASE_GAME) {
                        return@forEach
                    }
                    entries.forEach { (pack, _) ->
                        diagnostics.addDiagnostic(
                            code = "content-pack.overlay.same-priority-duplicate-target",
                            message =
                                "Packs in precedence bucket '${bucket.name}' target '${targetRef.registry.value}:${targetRef.id}' at the same priority.",
                            packId = pack.id,
                            targetRef = targetRef,
                            sourcePath = pack.manifestPath,
                        )
                    }
                }
        }
    }

    private fun validateOverlayLintTargets(
        orderedPacks: List<ResolvedContentPack>,
        diagnostics: MutableList<ContentPackDiagnostic>,
    ) {
        val knownTargets = OverlayLintCatalog.snapshot()
        orderedPacks.forEach { pack ->
            pack.manifest.overlays.forEach { overlay ->
                val targetKey = OverlayTargetKey.from(overlay.targetRef)
                when (overlay.op) {
                    OverlayOp.APPEND -> {
                        val metadata = knownTargets[targetKey]
                        if (metadata == null) {
                            diagnostics.addDiagnostic(
                                code = "content-pack.overlay.append-missing-target",
                                message =
                                    "APPEND overlay for '${overlay.targetRef.registry.value}:${overlay.targetRef.id}' requires an existing target.",
                                packId = pack.id,
                                targetRef = overlay.targetRef,
                                sourcePath = pack.manifestPath,
                            )
                        } else if (overlay.fieldPath !in metadata.appendAllowedFieldPaths) {
                            diagnostics.addDiagnostic(
                                code = "content-pack.overlay.append-target-forbidden",
                                message =
                                    "APPEND overlay for '${overlay.targetRef.registry.value}:${overlay.targetRef.id}' " +
                                        "must target one of ${metadata.appendAllowedFieldPaths.sorted()} but got '${overlay.fieldPath}'.",
                                packId = pack.id,
                                targetRef = overlay.targetRef,
                                sourcePath = pack.manifestPath,
                            )
                        }
                    }

                    OverlayOp.DENY -> {
                        val metadata = knownTargets[targetKey]
                        if (metadata == null) {
                            diagnostics.addDiagnostic(
                                code = "content-pack.overlay.deny-missing-target",
                                message =
                                    "DENY overlay for '${overlay.targetRef.registry.value}:${overlay.targetRef.id}' requires an existing target.",
                                packId = pack.id,
                                targetRef = overlay.targetRef,
                                sourcePath = pack.manifestPath,
                            )
                        } else if (!metadata.optional) {
                            diagnostics.addDiagnostic(
                                code = "content-pack.overlay.deny-non-optional-target",
                                message =
                                    "DENY overlay for '${overlay.targetRef.registry.value}:${overlay.targetRef.id}' is only allowed for optional targets.",
                                packId = pack.id,
                                targetRef = overlay.targetRef,
                                sourcePath = pack.manifestPath,
                            )
                        }
                    }

                    OverlayOp.ADD,
                    -> inferOverlayLintMetadata(pack, overlay)?.let { metadata ->
                        knownTargets[targetKey] = metadata
                    }

                    OverlayOp.REPLACE -> {
                        if (!knownTargets.containsKey(targetKey)) {
                            diagnostics.addDiagnostic(
                                code = "content-pack.overlay.replace-missing-target",
                                message =
                                    "REPLACE overlay for '${overlay.targetRef.registry.value}:${overlay.targetRef.id}' requires an existing target.",
                                packId = pack.id,
                                targetRef = overlay.targetRef,
                                sourcePath = pack.manifestPath,
                            )
                        } else {
                            inferOverlayLintMetadata(pack, overlay)?.let { metadata ->
                                knownTargets[targetKey] = metadata
                            }
                        }
                    }
                }
            }
        }
    }

    private fun inferOverlayLintMetadata(
        pack: ResolvedContentPack,
        overlay: OverlayEntry,
    ): OverlayLintMetadata? {
        val sourcePath = pack.resolvePath(overlay.sourceFile)
        if (!Files.exists(sourcePath)) {
            return null
        }
        val root = Files.newBufferedReader(sourcePath).use { reader -> yaml.load<Map<String, Any?>>(reader) } ?: emptyMap()
        return when (overlay.targetRef.registry.value) {
            "hidden_event" -> {
                val event = root.requiredList("events").singleOrNull()?.requiredMap() ?: return null
                OverlayLintMetadata(
                    optional = event.optionalBoolean("optionalOnly", default = true),
                    appendAllowedFieldPaths = setOf("rewards"),
                )
            }

            "secret_zone" -> {
                val secretZone = root.requiredList("secretZones").singleOrNull()?.requiredMap() ?: return null
                OverlayLintMetadata(
                    optional = "optional" in secretZone.optionalStringList("tags"),
                    appendAllowedFieldPaths = setOf("guaranteedContent"),
                )
            }

            "loot_profile" -> {
                val lootProfile = root.requiredList("lootProfiles").singleOrNull()?.requiredMap() ?: return null
                OverlayLintMetadata(
                    optional = "optional" in lootProfile.optionalStringList("tags"),
                    appendAllowedFieldPaths = setOf("itemIds"),
                )
            }

            "monster" -> {
                val monster = root.requiredList("monsters").singleOrNull()?.requiredMap() ?: return null
                OverlayLintMetadata(optional = "optional" in monster.optionalStringList("tags"))
            }

            else -> OverlayLintMetadata(optional = false)
        }
    }

    fun normalizeNamespace(packId: PackId): String =
        packId.value.replace('.', '_').replace('-', '_')

    private fun throwIfErrors(diagnostics: List<ContentPackDiagnostic>) {
        val errors = diagnostics.filter { diagnostic -> diagnostic.severity == ContentPackDiagnosticSeverity.ERROR }
        if (errors.isNotEmpty()) {
            throw ContentPackLoadException(errors)
        }
    }

    private data class ContentPackManifestFile(
        val packRoot: Path,
        val manifest: ContentPackManifest,
    )

    private enum class VisitState {
        ACTIVE,
        COMPLETE,
    }

    private object OverlayLintCatalog {
        private val baseTargets: Map<OverlayTargetKey, OverlayLintMetadata> by lazy(LazyThreadSafetyMode.NONE) {
            val catalog = DataLoader.loadBaseSchemaCatalogForContentPackLint(locale = GameLocale.EN_US)
            buildMap {
                catalog.hiddenEvents.forEach { event ->
                    put(
                        OverlayTargetKey(registry = "hidden_event", id = event.id),
                        OverlayLintMetadata(
                            optional = event.optionalOnly,
                            appendAllowedFieldPaths = setOf("rewards"),
                        ),
                    )
                }
                catalog.secretZones.forEach { secretZone ->
                    put(
                        OverlayTargetKey(registry = "secret_zone", id = secretZone.id.id),
                        OverlayLintMetadata(
                            optional = "optional" in secretZone.tags,
                            appendAllowedFieldPaths = setOf("guaranteedContent"),
                        ),
                    )
                }
                catalog.lootProfiles.forEach { lootProfile ->
                    put(
                        OverlayTargetKey(registry = "loot_profile", id = lootProfile.id),
                        OverlayLintMetadata(
                            optional = "optional" in lootProfile.tags,
                            appendAllowedFieldPaths = setOf("itemIds"),
                        ),
                    )
                }
                catalog.monsters.forEach { monster ->
                    put(
                        OverlayTargetKey(registry = "monster", id = monster.id),
                        OverlayLintMetadata(optional = "optional" in monster.tags),
                    )
                }
            }
        }

        fun snapshot(): LinkedHashMap<OverlayTargetKey, OverlayLintMetadata> = LinkedHashMap(baseTargets)
    }

    private data class OverlayTargetKey(
        val registry: String,
        val id: String,
    ) {
        companion object {
            fun from(targetRef: com.ktome.core.world.solvability.ContentRef): OverlayTargetKey =
                OverlayTargetKey(registry = targetRef.registry.value, id = targetRef.id)
        }
    }

    private data class OverlayLintMetadata(
        val optional: Boolean,
        val appendAllowedFieldPaths: Set<String> = emptySet(),
    )
}

private fun Map<String, Any?>.toContentPackManifest(): ContentPackManifest =
    ContentPackManifest(
        id = PackId(requiredString("id")),
        version = requiredString("version"),
        schemaVersion = requiredInt("schemaVersion"),
        gameVersionRange = requiredString("gameVersionRange"),
        namespace = requiredString("namespace"),
        dependencies =
            requiredList("dependencies").map { entry ->
                val dependency = entry.requiredMap()
                PackDependency(
                    id = PackId(dependency.requiredString("id")),
                    versionRange = dependency.requiredString("versionRange"),
                )
            },
        overlays =
            requiredList("overlays").map { entry ->
                val overlay = entry.requiredMap()
                OverlayEntry(
                    targetRef = overlay.requiredMap("targetRef").toContentRef(),
                    op = OverlayOp.valueOf(overlay.requiredString("op").uppercase()),
                    sourceFile = overlay.requiredString("sourceFile"),
                    fieldPath = overlay.optionalString("fieldPath"),
                    mergePolicy = overlay.optionalString("mergePolicy"),
                    dedupeKey = overlay.optionalString("dedupeKey"),
                )
            },
        localeBundles = optionalStringList("localeBundles"),
        visualManifest = optionalString("visualManifest"),
        audioManifest = optionalString("audioManifest"),
    )

private fun Map<*, *>.toContentRef(): com.ktome.core.world.solvability.ContentRef =
    com.ktome.core.world.solvability.ContentRef(
        registry = com.ktome.core.world.solvability.RegistryId(requiredString("registry")),
        id = requiredString("id"),
    )

private fun Map<*, *>.requiredString(key: String): String =
    this[key]?.toString()?.trim()?.takeIf(String::isNotBlank)
        ?: error("Missing required string '$key'.")

private fun Map<*, *>.requiredInt(key: String): Int =
    when (val value = this[key]) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: error("Field '$key' must be an integer.")
        else -> error("Missing required int '$key'.")
    }

private fun Map<*, *>.requiredList(key: String): List<Any?> =
    this[key] as? List<*> ?: error("Missing required list '$key'.")

private fun Any?.requiredMap(): Map<*, *> =
    this as? Map<*, *> ?: error("Entry must be a map.")

private fun Map<*, *>.requiredMap(key: String): Map<*, *> =
    this[key] as? Map<*, *> ?: error("Missing required map '$key'.")

private fun Map<*, *>.optionalString(key: String): String? =
    this[key]?.toString()?.trim()?.takeIf(String::isNotBlank)

private fun Map<*, *>.optionalStringList(key: String): List<String> =
    (this[key] as? List<*>)?.map { value ->
        value?.toString()?.trim()?.takeIf(String::isNotBlank)
            ?: error("Field '$key' must not contain blank strings.")
    } ?: emptyList()

private fun Map<*, *>.optionalBoolean(
    key: String,
    default: Boolean,
): Boolean =
    when (val value = this[key]) {
        null -> default
        is Boolean -> value
        is String -> value.trim().lowercase().let { normalized ->
            when (normalized) {
                "true" -> true
                "false" -> false
                else -> error("Field '$key' must be a boolean.")
            }
        }

        else -> error("Field '$key' must be a boolean.")
    }
