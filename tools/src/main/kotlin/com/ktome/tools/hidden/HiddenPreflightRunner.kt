package com.ktome.tools.hidden

import com.ktome.game.Phase4StaticContentValidator
import com.ktome.game.data.DataLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HiddenPreflightSummary(
    val hiddenEventCount: Int,
    val secretZoneCount: Int,
    val searchBindingIds: List<String>,
    val secretZoneIds: List<String>,
)

data class HiddenPreflightRun(
    val hiddenEventCount: Int,
    val secretZoneCount: Int,
    val summaryPath: Path,
)

object HiddenPreflightRunner {
    private const val SUMMARY_FILE_NAME: String = "hidden-preflight-summary.json"
    private val json: Json = Json { prettyPrint = true }

    fun run(): HiddenPreflightRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val bossDefinitions = loader.loadBossDefinitions()
        val monsterTemplatesById =
            (loader.loadMonsterCatalog().monsters + bossDefinitions.values.map { definition -> definition.template })
                .associateBy { template -> template.id }
        val snapshot =
            Phase4StaticContentValidator.validateHiddenContentContracts(
                schemaCatalog = schemaCatalog,
                lootProfilesById = schemaCatalog.lootProfiles.associateBy { profile -> profile.id },
                monsterTemplatesById = monsterTemplatesById,
                statuses = schemaCatalog.statuses,
            )
        val summary =
            HiddenPreflightSummary(
                hiddenEventCount = snapshot.hiddenEventCount,
                secretZoneCount = snapshot.secretZoneCount,
                searchBindingIds = snapshot.searchBindingIds,
                secretZoneIds = snapshot.secretZoneIds,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE_NAME)
        Files.writeString(summaryPath, json.encodeToString(summary))
        return HiddenPreflightRun(
            hiddenEventCount = summary.hiddenEventCount,
            secretZoneCount = summary.secretZoneCount,
            summaryPath = summaryPath,
        )
    }

    private fun reportDir(): Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.phase4.hidden.preflight.reportDir")) {
                "ktome.phase4.hidden.preflight.reportDir system property is required for hidden preflight output."
            },
        )
}
