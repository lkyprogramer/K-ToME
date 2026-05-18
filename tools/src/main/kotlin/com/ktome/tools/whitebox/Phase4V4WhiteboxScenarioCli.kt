package com.ktome.tools.whitebox

import com.ktome.game.validation.ValidationScenarioDef
import com.ktome.game.validation.ValidationScenarioRegistry
import com.ktome.game.validation.Phase4V4Pr06WhiteboxProperties
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Phase4V4WhiteboxScenarioCli {
    private val json = Json { prettyPrint = true }
    private const val HASH_BUFFER_SIZE: Int = 8192

    @JvmStatic
    fun main(args: Array<String>) {
        run(parseArgs(args.toList()))
    }

    fun run(config: Phase4V4WhiteboxScenarioCliConfig): Phase4V4WhiteboxScenarioCliResult {
        val scenarioId = config.scenarioId?.takeIf(String::isNotBlank)
            ?: fail("Missing -Pktome.whitebox.scenario. Known scenario ids: ${ValidationScenarioRegistry.knownIds().joinToString(", ")}")
        val scenario =
            ValidationScenarioRegistry.find(com.ktome.game.validation.ValidationScenarioId(scenarioId))
                ?: fail("Unknown scenario '$scenarioId'. Known scenario ids: ${ValidationScenarioRegistry.knownIds().joinToString(", ")}")
        val parity = ValidationScenarioRegistry.validateYamlParity(config.scenarioYaml)
        check(parity.isValid) {
            "Scenario yaml parity failed. missingFromYaml=${parity.missingFromYaml}; missingFromKotlin=${parity.missingFromKotlin}"
        }
        val materializationParity = Phase4V4WhiteboxScenarioMaterializationCatalog.validateRegistryParity()
        check(materializationParity.isValid) {
            "Scenario materialization parity failed. missingFromMaterialization=${materializationParity.missingFromMaterialization}; missingFromRegistry=${materializationParity.missingFromRegistry}"
        }
        val materialization = Phase4V4WhiteboxScenarioMaterializationCatalog.require(scenario.id)
        check(config.appExecutable.exists()) {
            "Packaged app executable is missing: ${repoRelative(config.repoRoot, config.appExecutable)}"
        }

        val scenarioRoot = config.outputRoot.resolve(scenario.id.value)
        val runtimeHome = scenarioRoot.resolve("runtime-home")
        val evidenceDir = scenarioRoot.resolve("evidence")
        runtimeHome.createDirectories()
        evidenceDir.createDirectories()

        val appHash = sha256(config.appExecutable)
        val paths =
            Phase4V4WhiteboxScenarioPaths(
                scenarioRoot = scenarioRoot,
                launchScript = scenarioRoot.resolve("launch-packaged-app.sh"),
                runbook = scenarioRoot.resolve("cua-runbook.md"),
                manualRecordTemplate = scenarioRoot.resolve("manual-record-template.md"),
                expectedEvidence = scenarioRoot.resolve("expected-evidence.json"),
                appExecutableSha256 = scenarioRoot.resolve("app-executable.sha256"),
                runtimeHome = runtimeHome,
                evidenceDir = evidenceDir,
                appExecutable = config.appExecutable,
            )

        paths.appExecutableSha256.writeText("$appHash  ${repoRelative(config.repoRoot, config.appExecutable)}\n")
        paths.launchScript.writeText(renderLaunchScript(config.repoRoot, scenario, paths, appHash))
        paths.launchScript.toFile().setExecutable(true)
        paths.runbook.writeText(renderRunbook(config.repoRoot, scenario, materialization, paths))
        paths.manualRecordTemplate.writeText(renderManualRecordTemplate(config.repoRoot, scenario, paths, appHash))
        paths.expectedEvidence.writeText(renderExpectedEvidence(config.repoRoot, scenario, paths, appHash))

        return Phase4V4WhiteboxScenarioCliResult(paths = paths, appHash = appHash)
    }

    private fun parseArgs(args: List<String>): Phase4V4WhiteboxScenarioCliConfig {
        fun value(name: String): String? =
            args.windowed(size = 2, step = 1)
                .firstOrNull { pair -> pair[0] == name }
                ?.get(1)

        return Phase4V4WhiteboxScenarioCliConfig(
            repoRoot = Path.of(requireNotNull(value("--repo-root")) { "--repo-root is required." }).normalize(),
            scenarioId = value("--scenario"),
            appExecutable = Path.of(requireNotNull(value("--app-executable")) { "--app-executable is required." }).normalize(),
            outputRoot = Path.of(requireNotNull(value("--output-root")) { "--output-root is required." }).normalize(),
            scenarioYaml = Path.of(requireNotNull(value("--scenario-yaml")) { "--scenario-yaml is required." }).normalize(),
        )
    }

    private fun renderLaunchScript(
        repoRoot: Path,
        scenario: ValidationScenarioDef,
        paths: Phase4V4WhiteboxScenarioPaths,
        appHash: String,
    ): String {
        val appExecutable = repoRelative(repoRoot, paths.appExecutable)
        val appBundle = repoRelative(repoRoot, paths.appExecutable.parent.parent.parent)
        val runtimeHome = repoRelative(repoRoot, paths.runtimeHome)
        val whiteboxRoot = repoRelative(repoRoot, paths.scenarioRoot)
        val evidenceDir = repoRelative(repoRoot, paths.evidenceDir)
        val runtime = scenario.runtime
        val evidence = scenario.evidence
        val manualRecord = evidence.manualRecordPath
        val appExecutableSha256 = repoRelative(repoRoot, paths.appExecutableSha256)
        val extraLaunchProperties = renderExtraLaunchProperties(repoRoot, scenario)
        val scenarioAppLogName = scenarioAppLogName(scenario)
        return """
            |#!/usr/bin/env bash
            |set -euo pipefail
            |
            |REPO_ROOT="$(cd "$(dirname "${'$'}0")/../../.." && pwd)"
            |cd "${'$'}REPO_ROOT"
            |
            |APP_EXECUTABLE="${'$'}REPO_ROOT/$appExecutable"
            |APP_BUNDLE="${'$'}REPO_ROOT/$appBundle"
            |APP_EXECUTABLE_SHA256="${'$'}REPO_ROOT/$appExecutableSha256"
            |EXPECTED_HASH="$(awk '{print ${'$'}1}' "${'$'}APP_EXECUTABLE_SHA256")"
            |ACTUAL_HASH="$(shasum -a 256 "${'$'}APP_EXECUTABLE" | awk '{print ${'$'}1}')"
            |if [ "${'$'}ACTUAL_HASH" != "${'$'}EXPECTED_HASH" ]; then
            |  echo "APP_HASH_MISMATCH expected=${'$'}EXPECTED_HASH actual=${'$'}ACTUAL_HASH app=$appExecutable" >&2
            |  exit 20
            |fi
            |
            |mkdir -p "$runtimeHome" "$evidenceDir"
            |APP_LOG="$evidenceDir/app.log"
            |SCENARIO_APP_LOG="$evidenceDir/$scenarioAppLogName"
            |{
            |  echo "scenarioId=${scenario.id.value}"
            |  echo "preset=${runtime.preset}"
            |  echo "seed=${runtime.seed}"
            |  echo "profession=${runtime.professionId}"
            |  echo "race=${runtime.raceId}"
            |  echo "zone=${runtime.zoneId}"
            |  echo "floor=${runtime.floor}"
            |} > "${'$'}APP_LOG"
            |cp "${'$'}APP_LOG" "${'$'}SCENARIO_APP_LOG"
            |BEFORE_PIDS="$(pgrep -f "${'$'}APP_EXECUTABLE" || true)"
            |$extraLaunchProperties
            |JAVA_TOOL_OPTIONS="-Duser.home=$runtimeHome -Dktome.validation.scenario=${scenario.id.value} -Dktome.repo.root=${'$'}REPO_ROOT -Dktome.whitebox.root=$whiteboxRoot -Dktome.whitebox.evidenceDir=$evidenceDir -Dktome.whitebox.manualRecord=$manualRecord -Dktome.whitebox.appHash=${'$'}EXPECTED_HASH${'$'}EXTRA_JAVA_TOOL_OPTIONS"
            |env JAVA_TOOL_OPTIONS="${'$'}JAVA_TOOL_OPTIONS" open -n "${'$'}APP_BUNDLE"
            |APP_PID=""
            |for _ in {1..20}; do
            |  CANDIDATE_PIDS="$(pgrep -f "${'$'}APP_EXECUTABLE" || true)"
            |  for PID in ${'$'}CANDIDATE_PIDS; do
            |    if ! printf '%s\n' "${'$'}BEFORE_PIDS" | grep -qx "${'$'}PID"; then
            |      APP_PID="${'$'}PID"
            |      break 2
            |    fi
            |  done
            |  sleep 0.5
            |done
            |if [ -z "${'$'}APP_PID" ]; then
            |  echo "APP_LAUNCH_FAILED app=$appExecutable" >&2
            |  exit 21
            |fi
            |printf '%s\n' "${'$'}APP_PID" > "$evidenceDir/app.pid"
            |echo "pid=${'$'}APP_PID" >> "${'$'}APP_LOG"
            |cp "${'$'}APP_LOG" "${'$'}SCENARIO_APP_LOG"
            |echo "Started K-ToME scenario ${scenario.id.value} pid=${'$'}APP_PID"
            |
        """.trimMargin()
    }

    private fun scenarioAppLogName(scenario: ValidationScenarioDef): String =
        scenario.evidence.requiredEvidenceFiles
            .singleOrNull { file -> file.startsWith("evidence/") && file.endsWith(".log") }
            ?.removePrefix("evidence/")
            ?: "${scenario.id.value}-app.log"

    private fun renderExtraLaunchProperties(
        repoRoot: Path,
        scenario: ValidationScenarioDef,
    ): String =
        if (scenario.id.value == "phase4-v4-pr06") {
            val summary = Phase4V4Pr06LaunchSummary.fromArtifacts(repoRoot)
            val primary = shellSingleQuote(summary.primaryResult)
            val evidence = shellSingleQuote(summary.evidenceResult)
            """
                |PR06_PRIMARY_RESULT=$primary
                |PR06_EVIDENCE_RESULT=$evidence
                |EXTRA_JAVA_TOOL_OPTIONS=" -D${Phase4V4Pr06WhiteboxProperties.PRIMARY_RESULT}=${'$'}PR06_PRIMARY_RESULT -D${Phase4V4Pr06WhiteboxProperties.EVIDENCE_RESULT}=${'$'}PR06_EVIDENCE_RESULT"
            """.trimMargin()
        } else {
            "EXTRA_JAVA_TOOL_OPTIONS=\"\""
        }

    private fun renderRunbook(
        repoRoot: Path,
        scenario: ValidationScenarioDef,
        materialization: Phase4V4WhiteboxScenarioMaterializationSpec,
        paths: Phase4V4WhiteboxScenarioPaths,
    ): String {
        val launchScript = repoRelative(repoRoot, paths.launchScript)
        val evidenceDir = repoRelative(repoRoot, paths.evidenceDir)
        val appExecutable = repoRelative(repoRoot, paths.appExecutable)
        val runtime = scenario.runtime
        val evidence = scenario.evidence
        val steps =
            listOf(
                Phase4V4RunbookStep(
                    mode = "Keyboard",
                    input = "Run launch script",
                    expectedVisibleResult = "Packaged app opens validation session",
                    evidenceFile = "evidence/app.log",
                ),
            ) +
                evidence.cuaSteps.map { step ->
                    Phase4V4RunbookStep(
                        mode = step.mode,
                        input = step.input,
                        expectedVisibleResult = step.expectedVisibleResult,
                        evidenceFile = step.evidenceFile,
                    )
                }
        return buildString {
            appendLine("# Phase4 v4 Fast Whitebox CUA Runbook")
            appendLine()
            appendLine("## 1. Scenario summary")
            appendLine()
            appendLine("- scenario id: `${scenario.id.value}`")
            appendLine("- preset: `${runtime.preset}`")
            appendLine("- seed: `${runtime.seed}`")
            appendLine("- locale: `${runtime.locale.id}`")
            appendLine("- window: `${materialization.windowWidth}x${materialization.windowHeight}`")
            appendLine("- app executable: `$appExecutable`")
            appendLine("- manual record: `${evidence.manualRecordPath}`")
            if (evidence.requiredLogEventKeys.isNotEmpty()) {
                appendLine("- required log events: `${evidence.requiredLogEventKeys.joinToString("`, `")}`")
            }
            appendLine()
            appendLine("## 2. Launch command")
            appendLine()
            appendLine("```bash")
            appendLine("$launchScript")
            appendLine("```")
            appendLine()
            appendLine("## 3. Computer Use target")
            appendLine()
            appendLine("App=`com.ktome.client`; verify the visible window belongs to the pid in `$evidenceDir/app.pid`.")
            appendLine()
            appendLine("## 4. Starting state assertions")
            appendLine()
            appendLine("- The session is validation-only and does not expose the standard player entry.")
            appendLine("- F9 opens `PHASE4_V4_FAST` with `${scenario.id.value}`, `${runtime.preset}`, and `${runtime.seed}` visible.")
            appendLine()
            appendLine("## 5. Input sequence")
            appendLine()
            appendLine("| Step | Mode | Input | Expected visible result | Evidence file |")
            appendLine("| --- | --- | --- | --- | --- |")
            steps.forEachIndexed { index, step ->
                appendLine("| ${index + 1} | ${step.mode} | ${step.input} | ${step.expectedVisibleResult} | `${step.evidenceFile}` |")
            }
            appendLine()
            appendLine("## 6. Screenshot capture commands")
            appendLine()
            evidence.requiredEvidenceFiles
                .filter { file -> file.endsWith(".png") }
                .forEach { file ->
                    appendLine("```bash")
                    appendLine("scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --out ${repoRelative(repoRoot, paths.scenarioRoot.resolve(file))}")
                    appendLine("```")
                }
            appendLine()
            appendLine("## 7. Metadata and SHA-256 checks")
            appendLine()
            appendLine("Each screenshot must have `.metadata.txt` and `.sha256` sidecars. Metadata must contain `capture_mode=macos-window-id`, matching `window_owner`, `window_pid`, and `window_bounds` for the packaged app pid.")
            appendLine()
            appendLine("## 8. Manual record write-back")
            appendLine()
            appendLine("Use `${repoRelative(repoRoot, paths.manualRecordTemplate)}` as the template source and write the completed record to `${evidence.manualRecordPath}`. Do not overwrite committed manual records from this task.")
            appendLine()
            appendLine("## 9. Failure retention")
            appendLine()
            appendLine("On failure, keep `$evidenceDir/app.log`, `app.pid`, screenshot sidecars, and the copied error payload.")
        }
    }

    private fun renderManualRecordTemplate(
        repoRoot: Path,
        scenario: ValidationScenarioDef,
        paths: Phase4V4WhiteboxScenarioPaths,
        appHash: String,
    ): String =
        """
        |# Phase4 v4 Fast Whitebox Manual Record - ${scenario.id.value}
        |
        |- Packaged app path: `${repoRelative(repoRoot, paths.appExecutable)}`
        |- App executable SHA-256: `$appHash`
        |- Runtime home: `${repoRelative(repoRoot, paths.runtimeHome)}`
        |- Scenario id: `${scenario.id.value}`
        |- Seed: `${scenario.runtime.seed}`
        |- Preset: `${scenario.runtime.preset}`
        |- CUA steps: `${repoRelative(repoRoot, paths.runbook)}`
        |- Manual record path: `${scenario.evidence.manualRecordPath}`
        |- Screenshot paths: `${repoRelative(repoRoot, paths.evidenceDir)}`
        |- Log path: `${repoRelative(repoRoot, paths.evidenceDir.resolve("app.log"))}`
        |- Conclusion: `PENDING`
        |
        |## Evidence
        |
        |${scenario.evidence.allRequiredEvidenceFiles.joinToString("\n") { file -> "- `$file`" }}
        |
        |## Notes
        |
        |- This record must be filled after packaged app + Computer Use execution.
        |- Fast whitebox evidence does not replace owner gates.
        |
        """.trimMargin()

    private fun renderExpectedEvidence(
        repoRoot: Path,
        scenario: ValidationScenarioDef,
        paths: Phase4V4WhiteboxScenarioPaths,
        appHash: String,
    ): String {
        val evidenceItems =
            scenario.evidence.requiredEvidenceFiles.map { file ->
                val sidecars =
                    if (file.endsWith(".png")) {
                        JsonArray(
                            listOf(
                                JsonPrimitive("$file.metadata.txt"),
                                JsonPrimitive("$file.sha256"),
                            ),
                        )
                    } else {
                        JsonArray(emptyList())
                    }
                JsonObject(
                    mapOf(
                        "path" to JsonPrimitive(file),
                        "requiresMetadata" to JsonPrimitive(file.endsWith(".png")),
                        "requiresSha256" to JsonPrimitive(file.endsWith(".png")),
                        "sidecars" to sidecars,
                    ),
                )
            }
        val payload =
            JsonObject(
                mapOf(
                    "scenarioId" to JsonPrimitive(scenario.id.value),
                    "appExecutable" to JsonPrimitive(repoRelative(repoRoot, paths.appExecutable)),
                    "appExecutableSha256" to JsonPrimitive(appHash),
                    "manualRecordTemplate" to JsonPrimitive(repoRelative(repoRoot, paths.manualRecordTemplate)),
                    "manualRecordPath" to JsonPrimitive(scenario.evidence.manualRecordPath),
                    "runtimeHome" to JsonPrimitive(repoRelative(repoRoot, paths.runtimeHome)),
                    "evidenceDir" to JsonPrimitive(repoRelative(repoRoot, paths.evidenceDir)),
                    "requiredLogEventKeys" to JsonArray(scenario.evidence.requiredLogEventKeys.map(::JsonPrimitive)),
                    "externalEvidence" to JsonArray(scenario.evidence.requiredExternalEvidenceFiles.map(::JsonPrimitive)),
                    "evidence" to JsonArray(evidenceItems),
                ),
            )
        return json.encodeToString(JsonObject.serializer(), payload) + "\n"
    }

    private fun repoRelative(
        repoRoot: Path,
        path: Path,
    ): String =
        repoRoot.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString()

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(HASH_BUFFER_SIZE)
        path.inputStream().use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) {
                    break
                }
                if (count > 0) {
                    digest.update(buffer, 0, count)
                }
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)

    private fun shellSingleQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"
}

private data class Phase4V4Pr06LaunchSummary(
    val primaryResult: String,
    val evidenceResult: String,
) {
    companion object {
        private val canonicalReportPath = Path.of("tools/build/reports/verification/phase4/report-phase4-summary.json")
        private val longRunProducerPath = Path.of("build/reports/harness/long-run-full.json")
        private val verifyChangedPlanPath = Path.of("build/verification/verify-changed/verify-changed-plan.json")
        private val summaryJson = Json { ignoreUnknownKeys = true }

        fun fromArtifacts(repoRoot: Path): Phase4V4Pr06LaunchSummary {
            val canonicalReport = repoRoot.resolve(canonicalReportPath)
            val longRunProducer = repoRoot.resolve(longRunProducerPath)
            val verifyChangedPlan = repoRoot.resolve(verifyChangedPlanPath)
            val canonicalPayload = readJsonObject(canonicalReport)
            val producerPayload = readJsonObject(longRunProducer)
            val routeDiversity =
                canonicalPayload?.objectOrNull("sections")?.objectOrNull("routeDiversity")
                    ?: canonicalPayload?.objectOrNull("routeDiversity")
                    ?: producerPayload
            val artifactStatus =
                artifactStatus(
                    canonicalReport = canonicalReport,
                    longRunProducer = longRunProducer,
                    routeDiversity = routeDiversity,
                )
            val producerArtifactStatus = if (longRunProducer.exists()) "loaded" else "missing"
            val verifyChangedArtifactStatus = if (verifyChangedPlan.exists()) "loaded" else "missing"
            val sourcePath = if (canonicalPayload != null) canonicalReportPath else longRunProducerPath
            val verifyChangedTasks =
                readJsonObject(verifyChangedPlan)
                    ?.stringList("requestedTaskPaths")
                    .orEmpty()

            return Phase4V4Pr06LaunchSummary(
                primaryResult =
                    primaryResultText(
                        routeDiversity = routeDiversity,
                        artifactStatus = artifactStatus,
                        sourcePath = sourcePath,
                        producerArtifactStatus = producerArtifactStatus,
                    ),
                evidenceResult =
                    evidenceResultText(
                        routeDiversity = routeDiversity,
                        artifactStatus = artifactStatus,
                        sourcePath = sourcePath,
                        producerArtifactStatus = producerArtifactStatus,
                        verifyChangedArtifactStatus = verifyChangedArtifactStatus,
                        verifyChangedTasks = verifyChangedTasks,
                    ),
            )
        }

        private fun artifactStatus(
            canonicalReport: Path,
            longRunProducer: Path,
            routeDiversity: JsonObject?,
        ): String =
            when {
                routeDiversity == null -> "unavailable"
                !canonicalReport.exists() -> "producerOnly"
                longRunProducer.exists() &&
                    java.nio.file.Files.getLastModifiedTime(longRunProducer).toMillis() >
                    java.nio.file.Files.getLastModifiedTime(canonicalReport).toMillis() -> "stale"
                else -> "loaded"
            }

        private fun primaryResultText(
            routeDiversity: JsonObject?,
            artifactStatus: String,
            sourcePath: Path,
            producerArtifactStatus: String,
        ): String {
            val payload =
                routeDiversity
                    ?: return "artifactStatus=unavailable;artifact=$sourcePath;producerArtifactStatus=$producerArtifactStatus"
            val scenarioDistribution = payload.intMap("scenarioTypeDistribution")
            val routeHashDistribution = payload.intMap("zoneRouteHashDistribution")
            val diversity = payload.objectOrNull("zoneRouteHashDiversity")
            val routeTokenSample = payload.stringList("routeTokenSample")
            val secretSamples = routeTokenSample.secretSamples()
            return listOf(
                "artifactStatus=$artifactStatus",
                "producerArtifactStatus=$producerArtifactStatus",
                "scenarioTypeDistribution=${scenarioDistribution.toCompactText()}",
                "zoneRouteHashDistribution=${routeHashDistribution.summaryText()}",
                "topHashShare=${diversity?.doubleValue("topHashShare") ?: 0.0}<=0.40",
                "branchInclusiveRoutes=${secretSamples.size}(secret:${secretSamples.joinToString("|")})",
            ).joinToString(";")
        }

        private fun evidenceResultText(
            routeDiversity: JsonObject?,
            artifactStatus: String,
            sourcePath: Path,
            producerArtifactStatus: String,
            verifyChangedArtifactStatus: String,
            verifyChangedTasks: List<String>,
        ): String {
            val payload =
                routeDiversity
                    ?: return "artifactStatus=unavailable;artifact=$sourcePath;producerArtifactStatus=$producerArtifactStatus;verifyChangedArtifactStatus=$verifyChangedArtifactStatus"
            val scenarioDistribution = payload.intMap("scenarioTypeDistribution")
            val diversity = payload.objectOrNull("zoneRouteHashDiversity")
            val topHashShare = diversity?.doubleValue("topHashShare") ?: 1.0
            return listOf(
                "artifactStatus=$artifactStatus",
                "producerArtifactStatus=$producerArtifactStatus",
                "full_route=${scenarioDistribution.getOrDefault("full_route", 0)}",
                "branch_inclusive=${scenarioDistribution.getOrDefault("branch_inclusive", 0)}",
                "route_probe=${scenarioDistribution.getOrDefault("route_probe", 0)}",
                "late_route_probe=${scenarioDistribution.getOrDefault("late_route_probe", 0)}",
                "topHashShare<=0.40:${topHashShare <= 0.4}",
                "verifyChangedArtifactStatus=$verifyChangedArtifactStatus",
                "verifyChangedTasks=${verifyChangedTasks.summaryText()}",
            ).joinToString(";")
        }

        private fun readJsonObject(path: Path): JsonObject? =
            if (!path.exists()) {
                null
            } else {
                runCatching { summaryJson.parseToJsonElement(java.nio.file.Files.readString(path)).jsonObject }.getOrNull()
            }
    }
}

private fun JsonObject.objectOrNull(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.stringList(key: String): List<String> =
    (this[key] as? JsonArray)
        ?.mapNotNull { element -> element.jsonPrimitive.contentOrNull }
        .orEmpty()

private fun JsonObject.intMap(key: String): Map<String, Int> =
    objectOrNull(key)
        ?.entries
        ?.associate { (name, count) -> name to (count.jsonPrimitive.intOrNull ?: 0) }
        .orEmpty()

private fun JsonObject.intValue(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0

private fun JsonObject.doubleValue(key: String): Double = this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0

private fun Map<String, Int>.toCompactText(): String =
    entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (key, value) -> "$key=$value" }

private fun Map<String, Int>.summaryText(): String {
    val total = values.sum()
    val maxCount = values.maxOrNull() ?: 0
    return "${size}_hashes,max=$maxCount/$total"
}

private fun List<String>.secretSamples(): List<String> =
    asSequence()
        .filter { token -> "secret:" in token }
        .mapNotNull { token ->
            token
                .split(">")
                .firstOrNull { segment -> segment.startsWith("secret:") }
                ?.removePrefix("secret:")
        }
        .distinct()
        .take(4)
        .toList()

private fun List<String>.summaryText(): String {
    val representativeTasks =
        listOf(
            ":game:longRunLab",
            ":tools:scopeCoverageLint",
            ":tools:reportPhase4Only",
            ":tools:maintainabilityLint",
        ).filter { task -> task in this }
    return "${size}_tasks(${representativeTasks.joinToString("|")})"
}

private data class Phase4V4RunbookStep(
    val mode: String,
    val input: String,
    val expectedVisibleResult: String,
    val evidenceFile: String,
)

data class Phase4V4WhiteboxScenarioCliConfig(
    val repoRoot: Path,
    val scenarioId: String?,
    val appExecutable: Path,
    val outputRoot: Path,
    val scenarioYaml: Path,
)

data class Phase4V4WhiteboxScenarioPaths(
    val scenarioRoot: Path,
    val launchScript: Path,
    val runbook: Path,
    val manualRecordTemplate: Path,
    val expectedEvidence: Path,
    val appExecutableSha256: Path,
    val runtimeHome: Path,
    val evidenceDir: Path,
    val appExecutable: Path,
)

data class Phase4V4WhiteboxScenarioCliResult(
    val paths: Phase4V4WhiteboxScenarioPaths,
    val appHash: String,
)
