package com.ktome.core.profile

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

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
            encodeDefaults = true
            explicitNulls = false
        },
) {
    fun encode(profile: ProfileData): String = json.encodeToString(ProfileData.serializer(), profile)

    fun decode(payload: String): ProfileData {
        val rootElement =
            try {
                json.parseToJsonElement(payload)
            } catch (exception: SerializationException) {
                throw IllegalArgumentException("Profile file is not valid JSON.", exception)
            }
        val root = rootElement as? JsonObject ?: throw IllegalArgumentException("Profile file root must be a JSON object.")
        requireFields(root, REQUIRED_PROFILE_FIELDS, context = "Profile file")
        val profileVersion = root.getValue("profileVersion").jsonPrimitive.intOrNull
            ?: throw IllegalArgumentException("Profile file field 'profileVersion' must be an integer.")
        require(profileVersion == ProfileData.CURRENT_PROFILE_VERSION) {
            "Unsupported profile version $profileVersion. Expected ${ProfileData.CURRENT_PROFILE_VERSION}."
        }
        val runHistory =
            root["runHistory"] as? JsonArray
                ?: throw IllegalArgumentException("Profile file field 'runHistory' must be a JSON array.")
        runHistory.forEachIndexed { index, element ->
            val summary =
                element as? JsonObject
                    ?: throw IllegalArgumentException("Profile file runHistory[$index] must be a JSON object.")
            requireFields(summary, REQUIRED_RUN_SUMMARY_FIELDS, context = "Profile file runHistory[$index]")
        }
        return try {
            json.decodeFromJsonElement(ProfileData.serializer(), root)
        } catch (exception: SerializationException) {
            throw IllegalArgumentException("Profile file does not match the current profile schema.", exception)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Profile file failed validation: ${exception.message}", exception)
        }
    }

    private fun requireFields(
        root: JsonObject,
        requiredFields: Set<String>,
        context: String,
    ) {
        val missing = requiredFields.filterNot(root::containsKey)
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("$context is missing required fields: ${missing.joinToString()}")
        }
    }

    private companion object {
        private val REQUIRED_PROFILE_FIELDS =
            setOf(
                "profileVersion",
                "releaseUnlockedClasses",
                "runHistory",
            )
        private val REQUIRED_RUN_SUMMARY_FIELDS =
            setOf(
                "seed",
                "finishedAtEpochMillis",
                "classId",
                "raceId",
                "finalZoneId",
                "turnCount",
                "headlessTurnEquivalent",
                "zoneRouteHash",
                "zonePath",
                "defeatedBossIds",
                "claimedRouteRewardIds",
                "shardBalance",
                "buildHash",
                "milestoneRewards",
                "rulesetVersion",
                "victory",
            )
    }
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
