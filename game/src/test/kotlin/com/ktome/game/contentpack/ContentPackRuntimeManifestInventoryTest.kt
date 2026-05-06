package com.ktome.game.contentpack

import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

class ContentPackRuntimeManifestInventoryTest {
    private val yaml = Yaml()

    @Test
    fun `repository runtime manifests use current content pack schema version`() {
        val repoRoot = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()
        val manifestPaths =
            listOf(
                repoRoot.resolve("examples/content-packs"),
                repoRoot.resolve("tools/src/main/resources/fixtures/content-packs/packs"),
            )
                .flatMap { root ->
                    if (!Files.isDirectory(root)) {
                        emptyList()
                    } else {
                        Files.walk(root).use { stream ->
                            stream
                                .asSequence()
                                .filter { path -> Files.isRegularFile(path) }
                                .filter { path -> path.fileName.toString() == "manifest.yaml" }
                                .toList()
                        }
                    }
                }
                .sorted()

        val staleManifests =
            manifestPaths.filter { manifestPath ->
                val root = yaml.load<Map<String, Any?>>(Files.readString(manifestPath))
                    ?: error("Runtime manifest must not be empty: $manifestPath")
                root["schemaVersion"] != ContentPackManifest.SCHEMA_VERSION
            }

        assertTrue(
            manifestPaths.isNotEmpty(),
            "Runtime manifest inventory must discover repository manifests under examples/content-packs or fixture packs.",
        )
        assertTrue(
            staleManifests.isEmpty(),
            "Runtime manifests must use schemaVersion=${ContentPackManifest.SCHEMA_VERSION}: " +
                staleManifests.joinToString { path -> repoRoot.relativize(path).toString() },
        )
    }
}
