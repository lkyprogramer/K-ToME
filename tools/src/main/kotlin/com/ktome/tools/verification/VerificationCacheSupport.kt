package com.ktome.tools.verification

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Comparator
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal data class VerificationDomainCacheDirs(
    val root: Path,
    val kernelDir: Path,
    val evaluationDir: Path,
)

internal object VerificationCacheSupport {
    val json: Json =
        Json {
            prettyPrint = true
            explicitNulls = false
        }

    fun repoRoot(): Path = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()

    fun cacheDirs(
        domainId: String,
        repoRoot: Path = repoRoot(),
    ): VerificationDomainCacheDirs {
        val root = repoRoot.resolve("tools/build/verification-cache")
        val kernelDir = root.resolve("kernels").resolve(domainId)
        val evaluationDir = root.resolve("evaluations").resolve(domainId)
        Files.createDirectories(kernelDir)
        Files.createDirectories(evaluationDir)
        return VerificationDomainCacheDirs(
            root = root,
            kernelDir = kernelDir,
            evaluationDir = evaluationDir,
        )
    }

    fun sha256(vararg values: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        values.forEach { value -> digest.update(value.toByteArray()) }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun sha256Files(files: Iterable<Path>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .sortedBy(Path::toString)
            .forEach { file -> updateDigestForPath(digest = digest, path = file) }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun sha256Json(payload: JsonElement): String = sha256(json.encodeToString(JsonElement.serializer(), payload))

    fun ensureDirectory(path: Path): Path {
        Files.createDirectories(path)
        return path
    }

    fun relativeToRepo(
        path: Path,
        repoRoot: Path = repoRoot(),
    ): String = repoRoot.relativize(path.toAbsolutePath().normalize()).invariantSeparatorsPathString

    fun copyDirectoryContents(
        sourceDir: Path,
        targetDir: Path,
    ) {
        require(sourceDir.exists() && sourceDir.isDirectory()) { "Missing source directory for cache copy: $sourceDir" }
        Files.createDirectories(targetDir)
        Files.walk(sourceDir).use { paths ->
            paths.forEach { sourcePath ->
                val relativePath = sourceDir.relativize(sourcePath)
                val targetPath = targetDir.resolve(relativePath.toString())
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.createDirectories(targetPath.parent)
                    Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    fun clearDirectory(dir: Path) {
        if (!dir.exists()) {
            return
        }
        Files.walk(dir).use { paths ->
            paths
                .sorted(Comparator.reverseOrder())
                .forEach { path ->
                    if (path != dir) {
                        Files.deleteIfExists(path)
                    }
                }
        }
    }

    fun writeJson(
        path: Path,
        payload: JsonElement,
    ) {
        Files.createDirectories(path.parent)
        Files.writeString(path, json.encodeToString(JsonElement.serializer(), payload))
    }

    fun readJson(path: Path): JsonElement = json.parseToJsonElement(Files.readString(path))

    fun mergeJsonlFiles(
        targetPath: Path,
        sourcePaths: Iterable<Path>,
    ) {
        Files.deleteIfExists(targetPath)
        Files.createDirectories(targetPath.parent)
        Files.newBufferedWriter(targetPath).use { writer ->
            sourcePaths.forEach { sourcePath ->
                require(Files.isRegularFile(sourcePath)) { "Missing JSONL shard file: $sourcePath" }
                Files.newBufferedReader(sourcePath).useLines { lines ->
                    lines.filter(String::isNotBlank).forEach { line -> writer.appendLine(line) }
                }
            }
        }
    }

    fun directoryFingerprint(dir: Path): String {
        if (!dir.exists()) {
            return sha256("missing:${dir.toAbsolutePath().normalize()}")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        updateDigestForPath(digest = digest, path = dir.toAbsolutePath().normalize())
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun absolutePath(path: Path): String = path.toAbsolutePath().normalize().absolutePathString()

    private fun updateDigestForPath(
        digest: MessageDigest,
        path: Path,
    ) {
        when {
            path.isRegularFile() -> {
                digest.update("file:${path.invariantSeparatorsPathString}".toByteArray())
                digest.update(Files.readAllBytes(path))
            }

            path.isDirectory() -> {
                Files.walk(path).use { paths ->
                    paths
                        .map(Path::toAbsolutePath)
                        .map(Path::normalize)
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach { currentPath ->
                            digest.update(currentPath.invariantSeparatorsPathString.toByteArray())
                            when {
                                currentPath.isRegularFile() -> digest.update(Files.readAllBytes(currentPath))
                                currentPath.isDirectory() -> digest.update("dir:${currentPath.name}".toByteArray())
                            }
                        }
                }
            }

            else -> digest.update("missing:${path.invariantSeparatorsPathString}".toByteArray())
        }
    }
}
