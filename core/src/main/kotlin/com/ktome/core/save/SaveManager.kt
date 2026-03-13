package com.ktome.core.save

import com.google.gson.GsonBuilder
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SaveManager(private val saveDir: Path) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val saveFile = saveDir.resolve(DEFAULT_FILE_NAME)

    fun save(snapshot: SaveSnapshot): Boolean =
        runCatching {
            snapshot.validateOrThrow()
            Files.createDirectories(saveDir)
            val temporaryFile = Files.createTempFile(saveDir, "${DEFAULT_FILE_NAME}.", ".tmp")
            try {
                temporaryFile.writeText(gson.toJson(snapshot))
                try {
                    Files.move(temporaryFile, saveFile, ATOMIC_MOVE, REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporaryFile, saveFile, REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporaryFile)
            }
        }.isSuccess

    fun load(): SaveSnapshot? {
        if (!hasSaveFile()) {
            return null
        }

        return runCatching {
            gson.fromJson(saveFile.readText(), SaveSnapshot::class.java)
        }.getOrNull()
            ?.takeIf { snapshot -> SaveSnapshot.isSupportedVersion(snapshot.version) }
            ?.takeIf { snapshot -> runCatching { snapshot.validateOrThrow() }.isSuccess }
    }

    fun hasSave(): Boolean = load() != null

    fun hasSaveFile(): Boolean = Files.isRegularFile(saveFile)

    fun deleteSave() {
        Files.deleteIfExists(saveFile)
    }

    fun savePath(): Path = saveFile

    companion object {
        const val DEFAULT_FILE_NAME: String = "run-save.json"
    }
}
