package com.ktome.core.profile

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ProfileManager(
    private val profileDir: Path,
    private val codec: ProfileCodec = ProfileCodec(),
) {
    private val profileFile: Path = profileDir.resolve(DEFAULT_FILE_NAME)

    fun load(): ProfileData =
        if (Files.isRegularFile(profileFile)) {
            codec.decode(Files.readString(profileFile))
        } else {
            ProfileData()
        }

    fun save(profile: ProfileData): Boolean =
        runCatching {
            Files.createDirectories(profileDir)
            val temporaryFile = Files.createTempFile(profileDir, "${DEFAULT_FILE_NAME}.", ".tmp")
            try {
                temporaryFile.writeText(codec.encode(profile))
                try {
                    Files.move(temporaryFile, profileFile, ATOMIC_MOVE, REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporaryFile, profileFile, REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporaryFile)
            }
        }.isSuccess

    fun savePath(): Path = profileFile

    companion object {
        const val DEFAULT_FILE_NAME: String = "profile.json"
    }
}

class ProfileCodec(
    private val json: Json =
        Json {
            prettyPrint = true
            explicitNulls = false
        },
) {
    fun encode(profile: ProfileData): String = json.encodeToString(ProfileData.serializer(), profile)

    fun decode(payload: String): ProfileData = json.decodeFromString(ProfileData.serializer(), payload)
}

@Serializable
data class AdvancedClassUnlockRule(
    val classId: String,
    val requiredProfessionId: String,
) {
    init {
        require(classId.isNotBlank()) { "AdvancedClassUnlockRule.classId must not be blank." }
        require(requiredProfessionId.isNotBlank()) { "AdvancedClassUnlockRule.requiredProfessionId must not be blank." }
    }
}

object ProfileProgression {
    fun appendRun(
        profile: ProfileData,
        summary: RunSummary,
        unlockRules: Iterable<AdvancedClassUnlockRule>,
    ): ProfileData {
        val history = profile.runHistory + summary
        val clearedBaseClasses =
            history
                .asSequence()
                .filter(RunSummary::victory)
                .map(RunSummary::classId)
                .toSet()
        val unlockedClasses =
            profile.releaseUnlockedClasses +
                unlockRules
                    .filter { rule -> rule.requiredProfessionId in clearedBaseClasses }
                    .map(AdvancedClassUnlockRule::classId)

        return profile.copy(
            releaseUnlockedClasses = unlockedClasses,
            runHistory = history,
        )
    }
}
