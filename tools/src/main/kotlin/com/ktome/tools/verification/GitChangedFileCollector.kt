package com.ktome.tools.verification

import java.nio.file.Path

data class GitChangedFileCollection(
    val changedFiles: List<String>,
    val notes: List<String>,
)

object GitChangedFileCollector {
    fun collect(
        repoRoot: Path,
        preferredBaseRef: String?,
    ): GitChangedFileCollection {
        val notes = mutableListOf<String>()
        val committedFiles =
            when (val diffBase = resolveDiffBase(repoRoot = repoRoot, preferredBaseRef = preferredBaseRef, notes = notes)) {
                null -> trackedFilesSnapshot(repoRoot, notes)
                else -> diffFilesSinceBase(repoRoot = repoRoot, diffBase = diffBase, notes = notes)
            }
        val stagedFiles =
            parseNameStatusPaths(
                runGitOrEmpty(
                    repoRoot,
                    "diff",
                    "--cached",
                    "--name-status",
                    "--find-renames",
                    "--diff-filter=ACMRD",
                ),
            )
        val unstagedFiles =
            parseNameStatusPaths(
                runGitOrEmpty(
                    repoRoot,
                    "diff",
                    "--name-status",
                    "--find-renames",
                    "--diff-filter=ACMRD",
                ),
            )
        val untrackedFiles =
            runGitOrEmpty(repoRoot, "ls-files", "--others", "--exclude-standard")
                .lineSequence()
                .map(InputScope.Companion::normalizePath)
                .map(String::trim)
                .filter(String::isNotBlank)
                .toList()

        return GitChangedFileCollection(
            changedFiles = (committedFiles + stagedFiles + unstagedFiles + untrackedFiles).distinct().sorted(),
            notes = notes.toList(),
        )
    }

    internal fun parseNameStatusPaths(output: String): List<String> =
        output
            .lineSequence()
            .flatMap(::parseNameStatusLine)
            .map(InputScope.Companion::normalizePath)
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()

    private fun parseNameStatusLine(line: String): Sequence<String> {
        if (line.isBlank()) {
            return emptySequence()
        }
        val parts = line.split('\t')
        if (parts.size < 2) {
            return emptySequence()
        }
        val status = parts.first().trim()
        return when {
            status.startsWith("R") || status.startsWith("C") -> parts.asSequence().drop(1).take(2)
            else -> sequenceOf(parts.last())
        }
    }

    private fun resolveDiffBase(
        repoRoot: Path,
        preferredBaseRef: String?,
        notes: MutableList<String>,
    ): String? {
        val candidates =
            buildList {
                preferredBaseRef?.takeIf(String::isNotBlank)?.let(::add)
                add("origin/main")
                add("main")
                add("origin/master")
                add("master")
            }.distinct()
        val resolvedRef =
            candidates.firstOrNull { candidate ->
                runGit(repoRoot, "rev-parse", "--verify", candidate) != null
            }
        if (resolvedRef == null) {
            notes += "Base ref unavailable; verifyChanged falls back to a tracked-file snapshot to stay conservative."
            return null
        }
        if (preferredBaseRef != null && resolvedRef != preferredBaseRef) {
            notes += "Base ref '$preferredBaseRef' was unavailable; using '$resolvedRef' instead."
        }
        return runGit(repoRoot, "merge-base", resolvedRef, "HEAD")
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: run {
                notes += "Unable to resolve merge-base for '$resolvedRef'; verifyChanged falls back to a tracked-file snapshot."
                null
            }
    }

    private fun diffFilesSinceBase(
        repoRoot: Path,
        diffBase: String,
        notes: MutableList<String>,
    ): List<String> {
        val output =
            runGit(
                repoRoot,
                "diff",
                "--name-status",
                "--find-renames",
                "--diff-filter=ACMRD",
                "$diffBase...HEAD",
            )
        if (output == null) {
            notes += "Unable to diff from merge-base '$diffBase'; verifyChanged falls back to a tracked-file snapshot."
            return trackedFilesSnapshot(repoRoot, notes)
        }
        return parseNameStatusPaths(output)
    }

    private fun trackedFilesSnapshot(
        repoRoot: Path,
        notes: MutableList<String>,
    ): List<String> {
        val trackedFiles =
            runGitOrEmpty(repoRoot, "ls-files")
                .lineSequence()
                .map(InputScope.Companion::normalizePath)
                .map(String::trim)
                .filter(String::isNotBlank)
                .toList()
        if (trackedFiles.isEmpty()) {
            notes += "Tracked-file snapshot fallback found no files."
        }
        return trackedFiles
    }

    private fun runGitOrEmpty(
        repoRoot: Path,
        vararg args: String,
    ): String = runGit(repoRoot, *args).orEmpty()

    private fun runGit(
        repoRoot: Path,
        vararg args: String,
    ): String? {
        val process =
            ProcessBuilder(listOf("git", *args))
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().use { reader -> reader.readText() }
        return if (process.waitFor() == 0) {
            output.trimEnd('\n', '\r')
        } else {
            null
        }
    }
}
