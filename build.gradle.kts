import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.Exec
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import com.ktome.build.testperf.TestPerfPlainTestBand
import com.ktome.build.testperf.TestPerfPlainTestOptIn
import java.nio.file.Files
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    id("com.ktome.build.testperf")
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    jacoco
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
val bootstrapRepo = rootProject.layout.projectDirectory.dir(".bootstrap/m2").asFile
val verificationOnlyTestTags =
    listOf(
        "headlessSmoke",
        "clientSmoke",
        "longRunLab",
        "soloClearLab",
        "goldenScreenshot",
        "bossHarness",
        "terrainInteractionBatch",
        "combatTraceGolden",
        "localeLint",
        "contractLint",
        "keywordRegistryLint",
        "maintainabilityLint",
        "mapgenSmoke",
        "solvabilityHarness",
        "hiddenContentHarness",
        "organicHiddenProbe",
        "lootBalanceLab",
        "contentPackHarness",
        "whiteBoxMapgen",
        "whiteBoxLoot",
        "whiteBoxSolvability",
        "whiteBoxHiddenContent",
        "whiteBoxContentPack",
        "phase4LegacyReport",
        "reportPhase4",
        "reportPhase4Fixture",
        "verifyLootPreflight",
        "verifyHiddenPreflight",
        "verifyContentPackPreflight",
        "scopeCoverageLint",
    )
val verifyOwnerTaskPaths =
    listOf(
        ":tools:contractLint",
        ":tools:keywordRegistryLint",
        ":tools:lootBalanceLab",
        ":tools:whiteBoxLoot",
        ":tools:hiddenContentHarness",
        ":tools:organicHiddenProbe",
        ":tools:contentPackHarness",
        ":tools:whiteBoxContentPack",
        ":tools:whiteBoxMapgen",
        ":tools:whiteBoxSolvability",
        ":tools:terrainInteractionBatch",
        ":tools:bossHarness",
        ":game:longRunLab",
    )
val verifyChangedTaskPaths =
    listOf(
        ":tools:prepareVerifyChangedPlan",
        ":tools:scopeCoverageLint",
        ":tools:maintainabilityLint",
        ":tools:verifyContractLintPreflight",
        ":tools:contractLint",
        ":tools:keywordRegistryLint",
        ":tools:verifyLootPreflight",
        ":tools:verifyHiddenPreflight",
        ":tools:verifyContentPackPreflight",
        ":tools:lootBalanceLab",
        ":tools:whiteBoxLoot",
        ":tools:hiddenContentHarness",
        ":tools:organicHiddenProbe",
        ":tools:contentPackHarness",
        ":tools:whiteBoxContentPack",
        ":tools:reportPhase4Only",
        ":tools:whiteBoxMapgen",
        ":tools:whiteBoxSolvability",
        ":tools:terrainInteractionBatch",
        ":tools:bossHarness",
        ":game:longRunLab",
    )
val verifyChangedPreflightTaskPaths =
    listOf(
        ":tools:prepareVerifyChangedPlan",
        ":tools:scopeCoverageLint",
        ":tools:maintainabilityLint",
        ":tools:verifyContractLintPreflight",
        ":tools:verifyLootPreflight",
        ":tools:verifyHiddenPreflight",
        ":tools:verifyContentPackPreflight",
    )
val verifyChangedTaskPathsFile = rootProject.layout.buildDirectory.file("verification/verify-changed/task-paths.txt")
val verifyChangedPreflightTaskPathsFile = rootProject.layout.buildDirectory.file("verification/verify-changed/preflight-task-paths.txt")
val verifyChangedFullSummaryJsonFile = rootProject.layout.buildDirectory.file("verification/verify-changed/full-task-duration-summary.json")
val verifyChangedFullSummaryMarkdownFile = rootProject.layout.buildDirectory.file("verification/verify-changed/full-task-duration-summary.md")
val verifyChangedPreflightSummaryJsonFile = rootProject.layout.buildDirectory.file("verification/verify-changed/preflight-task-duration-summary.json")
val verifyChangedPreflightSummaryMarkdownFile = rootProject.layout.buildDirectory.file("verification/verify-changed/preflight-task-duration-summary.md")
val verifyChangedLegacyPreflightSummaryJsonFile = rootProject.layout.buildDirectory.file("verification/verify-changed/task-duration-summary.json")
val verifyChangedLegacyPreflightSummaryMarkdownFile = rootProject.layout.buildDirectory.file("verification/verify-changed/task-duration-summary.md")
val verifyChangedTrackedTaskPaths = (verifyChangedTaskPaths + verifyChangedPreflightTaskPaths).toSet()

data class VerifyChangedTaskDurationRecord(
    val taskPath: String,
    val durationMillis: Long,
    val outcome: String,
)

val verifyChangedTaskDurationStartedAtNanos = linkedMapOf<String, Long>()
val verifyChangedTaskDurationRecords = linkedMapOf<String, VerifyChangedTaskDurationRecord>()

fun readVerifyChangedSelectedTaskPaths(taskPathsFile: Provider<RegularFile>): List<String> =
    taskPathsFile
        .get()
        .asFile
        .takeIf { file -> file.exists() }
        ?.readLines()
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        .orEmpty()

fun verifyChangedJsonEscape(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

fun writeVerifyChangedTaskDurationSummary(
    title: String,
    summaryKind: String,
    selectedTaskPaths: List<String>,
    summaryJsonFile: Provider<RegularFile>,
    summaryMarkdownFile: Provider<RegularFile>,
    mirrorJsonFile: Provider<RegularFile>? = null,
    mirrorMarkdownFile: Provider<RegularFile>? = null,
) {
    val selectedRecords =
        selectedTaskPaths.map { taskPath ->
            verifyChangedTaskDurationRecords[taskPath]
                ?: VerifyChangedTaskDurationRecord(
                    taskPath = taskPath,
                    durationMillis = 0L,
                    outcome = "NOT_RECORDED",
                )
        }
    val totalDurationMillis = selectedRecords.sumOf(VerifyChangedTaskDurationRecord::durationMillis)
    val slowestRecords =
        selectedRecords
            .sortedByDescending(VerifyChangedTaskDurationRecord::durationMillis)
            .take(10)
    val json =
        buildString {
            appendLine("{")
            appendLine("""  "summaryKind": "${verifyChangedJsonEscape(summaryKind)}",""")
            appendLine("""  "selectedTaskCount": ${selectedRecords.size},""")
            appendLine("""  "totalDurationMillis": $totalDurationMillis,""")
            appendLine("""  "tasks": [""")
            selectedRecords.forEachIndexed { index, record ->
                append(
                    """    {"taskPath":"${verifyChangedJsonEscape(record.taskPath)}","durationMillis":${record.durationMillis},"outcome":"${verifyChangedJsonEscape(record.outcome)}"}""",
                )
                appendLine(if (index == selectedRecords.lastIndex) "" else ",")
            }
            appendLine("  ],")
            appendLine("""  "slowestTasks": [""")
            slowestRecords.forEachIndexed { index, record ->
                append(
                    """    {"taskPath":"${verifyChangedJsonEscape(record.taskPath)}","durationMillis":${record.durationMillis},"outcome":"${verifyChangedJsonEscape(record.outcome)}"}""",
                )
                appendLine(if (index == slowestRecords.lastIndex) "" else ",")
            }
            appendLine("  ]")
            appendLine("}")
        }
    val markdown =
        buildString {
            appendLine("# $title")
            appendLine()
            appendLine("- summaryKind: `$summaryKind`")
            appendLine("- selectedTaskCount: `${selectedRecords.size}`")
            appendLine("- totalDurationMillis: `$totalDurationMillis`")
            appendLine()
            appendLine("## Slowest Tasks")
            appendLine()
            appendLine("| Task | Duration (ms) | Outcome |")
            appendLine("| --- | ---: | --- |")
            slowestRecords.forEach { record ->
                appendLine("| `${record.taskPath}` | `${record.durationMillis}` | `${record.outcome}` |")
            }
            appendLine()
            appendLine("## Selected Tasks")
            appendLine()
            appendLine("| Task | Duration (ms) | Outcome |")
            appendLine("| --- | ---: | --- |")
            selectedRecords.forEach { record ->
                appendLine("| `${record.taskPath}` | `${record.durationMillis}` | `${record.outcome}` |")
            }
        }
    val summaryJson = summaryJsonFile.get().asFile.toPath()
    Files.createDirectories(summaryJson.parent)
    Files.writeString(summaryJson, json)
    Files.writeString(summaryMarkdownFile.get().asFile.toPath(), markdown)
    mirrorJsonFile?.let { file -> Files.writeString(file.get().asFile.toPath(), json) }
    mirrorMarkdownFile?.let { file -> Files.writeString(file.get().asFile.toPath(), markdown) }
    println("$summaryKind task-duration summary")
    selectedRecords.forEach { record ->
        println("- ${record.taskPath} durationMs=${record.durationMillis} outcome=${record.outcome}")
    }
}

fun recordVerifyChangedTaskStarted(taskPath: String) {
    verifyChangedTaskDurationStartedAtNanos[taskPath] = System.nanoTime()
}

fun recordVerifyChangedTaskFinished(taskPath: String) {
    val startedAt = verifyChangedTaskDurationStartedAtNanos.remove(taskPath)
    verifyChangedTaskDurationRecords[taskPath] =
        VerifyChangedTaskDurationRecord(
            taskPath = taskPath,
            durationMillis =
                if (startedAt == null) {
                    0L
                } else {
                    (System.nanoTime() - startedAt) / 1_000_000
                },
            outcome = "EXECUTED",
        )
}

extensions.configure<JacocoPluginExtension> {
    toolVersion = providers.gradleProperty("jacocoVersion").get()
}

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        if (bootstrapRepo.isDirectory) {
            maven(url = bootstrapRepo.toURI())
        }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(
                JavaLanguageVersion.of(
                    JavaVersion.toVersion(providers.gradleProperty("javaVersion").get()).majorVersion.toInt(),
                ),
            )
        }
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = providers.gradleProperty("jacocoVersion").get()
    }

    dependencies {
        add(
            "testImplementation",
            "org.junit.jupiter:junit-jupiter:${providers.gradleProperty("junitVersion").get()}",
        )
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        if (name == "test") {
            useJUnitPlatform {
                excludeTags(*verificationOnlyTestTags.toTypedArray())
            }
            TestPerfPlainTestOptIn.monitor(this, TestPerfPlainTestBand.SMALL_TEST)
        }
        testLogging {
            events("failed", "skipped")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(providers.gradleProperty("javaVersion").get()))
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}

val test = tasks.register("test") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests for all subprojects."
    dependsOn(subprojects.map { it.tasks.named("test") })
}

val unitAndToolsGate = tasks.register("unitAndToolsGate") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the unit, lint, trace-golden, and core coverage verification gates."
    dependsOn(":core:test")
    dependsOn(":tools:test")
    dependsOn("localeLint")
    dependsOn("contractLint")
    dependsOn("combatTraceGolden")
    dependsOn(":core:jacocoTestCoverageVerification")
}

val gameHarnessGate = tasks.register("gameHarnessGate") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the game harness verification gates."
    dependsOn(":game:test")
    dependsOn("headlessSmoke")
    dependsOn("soloClearLab")
    dependsOn("longRunLab")
    dependsOn("bossHarness")
}

val clientAndAssetsGate = tasks.register("clientAndAssetsGate") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the client, golden screenshot, and asset verification gates."
    dependsOn(":client:test")
    dependsOn("clientSmoke")
    dependsOn("goldenScreenshot")
    dependsOn("assetLint")
    dependsOn("styleLint")
    dependsOn("audioLint")
    dependsOn("manifestLint")
}

val verificationGate = tasks.register("verificationGate") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all CI verification gates without generating the aggregate coverage report."
    dependsOn(unitAndToolsGate)
    dependsOn(gameHarnessGate)
    dependsOn(clientAndAssetsGate)
}

val bootstrapOfflineSmoke = tasks.register("bootstrapOfflineSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the PR bootstrap smoke gate with isolated offline core/tools verification."
    dependsOn(":core:test")
    dependsOn(":tools:test")
    dependsOn(":core:jacocoTestCoverageVerification")
}

val bootstrapOfflineVerify = tasks.register("bootstrapOfflineVerify") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the full offline bootstrap verification gate with the aggregate root test suite."
    dependsOn(test)
    dependsOn(":core:jacocoTestCoverageVerification")
}

tasks.register("saveSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs save/load smoke coverage across the core, game, and client modules."
    dependsOn(":core:test", ":game:test", ":client:test")
}

tasks.register("headlessSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the game-layer AI smoke harness."
    dependsOn(":game:headlessSmoke")
}

tasks.register("soloClearLab") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the fixed-seed solo clear acceptance lab."
    dependsOn(":game:soloClearLab")
}

tasks.register("clientSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs headless client lifecycle smoke coverage."
    dependsOn(":client:clientSmoke")
}

tasks.register("longRunLab") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the nightly long-run AI matrix."
    dependsOn(":game:longRunLab")
}

tasks.register("localeLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates i18n resource completeness, placeholders, and schema text discipline."
    dependsOn(":tools:localeLint")
}

tasks.register("contractLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates schema V2 structure, cross-references, and key namespaces."
    dependsOn(":tools:contractLint")
}

tasks.register("keywordRegistryLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates keyword registry consumers and formal description coverage probes."
    dependsOn(":tools:keywordRegistryLint")
}

tasks.register("maintainabilityLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the anti-bloat maintainability lint and baseline debt gate."
    dependsOn(":tools:maintainabilityLint")
}

tasks.register("verifyContractLintPreflight") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the unified verification STATIC_GRAPH demo for contractLint."
    dependsOn(":tools:verifyContractLintPreflight")
}

tasks.register("verifyContractLintPreflightReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Rebuilds the unified verification demo report for contractLint from existing artifacts."
    dependsOn(":tools:verifyContractLintPreflightReport")
}

tasks.register("verifyLootPreflight") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 loot static preflight."
    dependsOn(":tools:verifyLootPreflight")
}

tasks.register("verifyHiddenPreflight") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 hidden-content static preflight."
    dependsOn(":tools:verifyHiddenPreflight")
}

tasks.register("verifyContentPackPreflight") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 content-pack static preflight."
    dependsOn(":tools:verifyContentPackPreflight")
}

tasks.register("scopeCoverageLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks that Phase 4 impact scopes and false-negative fallbacks still cover critical shared entry points."
    dependsOn(":tools:scopeCoverageLint")
}

tasks.register("combatTraceGolden") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 3 FORMULA corpus combat trace golden harness."
    dependsOn(":tools:combatTraceGolden")
}

tasks.register("bossHarness") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 boss owner domain through the unified verification task path."
    dependsOn(":tools:bossHarness")
}

tasks.register("terrainInteractionBatch") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 terrain owner domain through the unified verification task path."
    dependsOn(":tools:terrainInteractionBatch")
}

tasks.register("mapgenSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 map generation smoke baseline and writes fixed reports."
    dependsOn(":tools:mapgenSmoke")
}

tasks.register("solvabilityHarness") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 solvability proof harness and writes structured reports."
    dependsOn(":tools:solvabilityHarness")
}

tasks.register("hiddenContentHarness") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 hidden-content harness and writes structured reports."
    dependsOn(":tools:hiddenContentHarness")
}

tasks.register("contentPackHarness") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 content-pack harness plus its paired white-box content-pack summary."
    dependsOn(":tools:contentPackHarness", ":tools:whiteBoxContentPack")
}

tasks.register("organicHiddenProbe") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 organic hidden-content probe and writes structured reports."
    dependsOn(":tools:organicHiddenProbe")
}

tasks.register("whiteBoxMapgen") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 unified white-box mapgen pilot."
    dependsOn(":tools:whiteBoxMapgen")
}

tasks.register("whiteBoxSolvability") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 unified white-box solvability pilot."
    dependsOn(":tools:whiteBoxSolvability")
}

tasks.register("whiteBoxHiddenContent") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 unified white-box hidden-content domain."
    dependsOn(":tools:whiteBoxHiddenContent")
}

tasks.register("whiteBoxContentPack") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 unified white-box content-pack domain."
    dependsOn(":tools:whiteBoxContentPack")
}

tasks.register("whiteBoxVerify") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all currently registered unified white-box verification pilots."
    dependsOn("whiteBoxMapgen", "whiteBoxSolvability", "whiteBoxLoot", "whiteBoxHiddenContent", "whiteBoxContentPack")
}

tasks.register<JavaExec>("preparePhase4V4Whitebox") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates Phase4 v4 fast whitebox packaged-app launch material for one validation scenario."
    dependsOn(":tools:classes", ":client:packageMacApp")
    val toolsSourceSets = project(":tools").extensions.getByType(SourceSetContainer::class.java)
    classpath = toolsSourceSets.named("main").get().runtimeClasspath
    mainClass.set("com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCli")
    val scenarioId = providers.gradleProperty("ktome.whitebox.scenario").orNull
    args(
        "--repo-root",
        layout.projectDirectory.asFile.path,
        "--scenario",
        scenarioId ?: "",
        "--app-executable",
        layout.projectDirectory.file("client/build/release/K-ToME.app/Contents/MacOS/K-ToME").asFile.path,
        "--output-root",
        layout.buildDirectory.dir("whitebox").get().asFile.path,
        "--scenario-yaml",
        layout.projectDirectory.file("tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml").asFile.path,
    )
}

tasks.register("lootBalanceLab") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 loot balance lab and writes structured reports."
    dependsOn(":tools:lootBalanceLab")
}

tasks.register("whiteBoxLoot") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 4 unified white-box loot domain."
    dependsOn(":tools:whiteBoxLoot")
}

tasks.register("phase4LegacyReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Manually rebuilds the legacy Phase 4 aggregate report from existing artifacts."
    dependsOn(":tools:phase4LegacyReport")
}

tasks.register("phase4LegacyReportOnly") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Explicit artifact-only alias for manually rebuilding the legacy Phase 4 aggregate report."
    dependsOn(":tools:phase4LegacyReportOnly")
}

tasks.register("phase4Report") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Rebuilds the canonical unified Phase 4 aggregate report from existing owner artifacts."
    dependsOn(":tools:phase4Report")
}

tasks.register("phase4ReportOnly") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Explicit artifact-only alias for rebuilding the canonical unified Phase 4 aggregate report."
    dependsOn(":tools:phase4ReportOnly")
}

tasks.register("reportPhase4") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the explicit parity gate that compares the canonical unified Phase 4 aggregate against the legacy fallback report."
    dependsOn(":tools:reportPhase4")
}

tasks.register("reportPhase4Only") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Rebuilds the canonical unified Phase 4 aggregate report from existing domain summaries without legacy comparison."
    dependsOn(":tools:reportPhase4Only")
}

tasks.register("reportPhase4Fixture") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the fixture-backed reportPhase4 contract tests without materializing the repo-scoped canonical report."
    dependsOn(":tools:reportPhase4Fixture")
}

tasks.register("verifyOwner") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the routed Phase 4 owner verification set on top of the unified verification contract."
    dependsOn(verifyOwnerTaskPaths)
}

tasks.register("verifyChanged") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Collects changed files, analyzes impacted Phase 4 domains, and runs the selected preflight/owner verification tasks."
    dependsOn(verifyChangedTaskPaths)
    doLast {
        writeVerifyChangedTaskDurationSummary(
            title = "verifyChanged Full Task Duration Summary",
            summaryKind = "verifyChangedFull",
            selectedTaskPaths = readVerifyChangedSelectedTaskPaths(verifyChangedTaskPathsFile),
            summaryJsonFile = verifyChangedFullSummaryJsonFile,
            summaryMarkdownFile = verifyChangedFullSummaryMarkdownFile,
        )
    }
}

tasks.register("verifyChangedPreflight") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Collects changed files, analyzes impacted Phase 4 domains, and runs only the routed lightweight preflight checks."
    dependsOn(verifyChangedPreflightTaskPaths)
    doLast {
        writeVerifyChangedTaskDurationSummary(
            title = "verifyChangedPreflight Task Duration Summary",
            summaryKind = "verifyChangedPreflight",
            selectedTaskPaths = readVerifyChangedSelectedTaskPaths(verifyChangedPreflightTaskPathsFile),
            summaryJsonFile = verifyChangedPreflightSummaryJsonFile,
            summaryMarkdownFile = verifyChangedPreflightSummaryMarkdownFile,
            mirrorJsonFile = verifyChangedLegacyPreflightSummaryJsonFile,
            mirrorMarkdownFile = verifyChangedLegacyPreflightSummaryMarkdownFile,
        )
    }
}

gradle.projectsEvaluated {
    verifyChangedTrackedTaskPaths.forEach { taskPath ->
        val taskProjectPath = taskPath.substringBeforeLast(':').ifBlank { ":" }
        val taskName = taskPath.substringAfterLast(':')
        project(taskProjectPath).tasks.named(taskName).configure {
            doFirst {
                recordVerifyChangedTaskStarted(path)
            }
            doLast {
                recordVerifyChangedTaskFinished(path)
            }
        }
    }
}

tasks.register("nightlyGovernanceGate") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description =
        "Runs the shared governance preflight plus Phase 4 aggregate smoke on top of the canonical nightly producer set."
    dependsOn("scopeCoverageLint")
    dependsOn("maintainabilityLint")
    dependsOn("verifyOwner")
    dependsOn("mapgenSmoke")
    dependsOn("solvabilityHarness")
    dependsOn("reportPhase4")
}

tasks.register<Exec>("assetLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates the primary image asset plan, packaging icon plan, plus Gemini additions."
    commandLine(
        "python3",
        "scripts/asset-lint.py",
        "--plan",
        "assets-src/image/specs/phase2-asset-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/macos-app-icon-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-pr09-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr06-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr02-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-v3-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-uiux-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-uiux-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr07-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr09-gemini-plan.yaml",
        "--report-dir",
        "assets-src/image/manifests",
    )
}

tasks.register<Exec>("styleLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates the primary art style contract bindings, packaging icon plan, plus Gemini additions."
    commandLine(
        "python3",
        "scripts/style-lint.py",
        "--plan",
        "assets-src/image/specs/phase2-asset-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/macos-app-icon-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-pr09-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr06-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr02-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-uiux-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-uiux-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr07-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr09-gemini-plan.yaml",
    )
}

tasks.register<Exec>("audioLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates the Phase 2 audio cue plan and manifest."
    dependsOn("syncPhase2Manifests")
    commandLine(
        "python3",
        "scripts/audio-lint.py",
        "--plan",
        "assets-src/audio/specs/phase2-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase3-pr09-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase3-v2-pr11-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase3-v3-pr16-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase3-v3-pr17-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-pr05-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-pr06-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-opt-pr02-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-opt-pr03-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-opt-pr05-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-v3-pr03-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-uiux-pr03-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-uiux-pr05-audio-plan.yaml",
        "--extra-plan",
        "assets-src/audio/specs/phase4-pr07-audio-plan.yaml",
        "--manifest",
        "assets-src/audio/manifests/phase2-audio-manifest.json",
        "--runtime-manifest",
        "client/src/main/resources/manifests/audio-manifest.json",
        "--runtime-root",
        "client/src/main/resources",
    )
}

tasks.register<Exec>("syncPhase2Manifests") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Syncs canonical Phase 2 manifests from assets-src into runtime resources."
    commandLine("python3", "scripts/sync_phase2_manifests.py")
}

tasks.register<Exec>("audioProcess") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Builds the Phase 2 runtime audio placeholders from raw sources."
    dependsOn("syncPhase2Manifests")
    commandLine(
        "python3",
        "scripts/process_audio.py",
        "--runtime-manifest",
        "client/src/main/resources/manifests/audio-manifest.json",
        "--raw-dir",
        "assets-src/audio/raw",
        "--cleaned-dir",
        "assets-src/audio/cleaned",
        "--runtime-root",
        "client/src/main/resources",
        "--bootstrap-missing",
    )
}

tasks.register<Exec>("manifestLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates the Phase 2 image manifest against the asset plan."
    dependsOn("syncPhase2Manifests")
    commandLine(
        "python3",
        "scripts/manifest-lint.py",
        "--plan",
        "assets-src/image/specs/phase2-asset-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-pr09-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-pr09-visual-alias-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v2-pr11-visual-alias-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr06-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr02-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-opt-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-v3-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-uiux-pr03-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-uiux-pr05-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase4-pr07-gemini-plan.yaml",
        "--manifest",
        "assets-src/image/manifests/phase2-visual-manifest.json",
        "--runtime-manifest",
        "client/src/main/resources/manifests/visual-manifest.json",
        "--runtime-root",
        "client/src/main/resources",
    )
}

tasks.register("goldenScreenshot") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs deterministic screenshot golden coverage."
    dependsOn(":client:goldenScreenshot")
}

tasks.register("preReleaseAcceptance") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the full pre-release acceptance gate, including smoke coverage, coverage reports, and desktop packaging."
    dependsOn("clean")
    dependsOn(test)
    dependsOn("localeLint")
    dependsOn("contractLint")
    dependsOn("assetLint")
    dependsOn("styleLint")
    dependsOn("audioLint")
    dependsOn("manifestLint")
    dependsOn("goldenScreenshot")
    dependsOn("headlessSmoke")
    dependsOn("soloClearLab")
    dependsOn("longRunLab")
    dependsOn("clientSmoke")
    dependsOn("jacocoTestReport")
    dependsOn(":core:jacocoTestCoverageVerification")
    dependsOn(":client:releaseDesktopDist")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates an aggregate JaCoCo coverage report for all subprojects."
    dependsOn(test)
    dependsOn("combatTraceGolden")
    dependsOn("bossHarness")
    dependsOn("localeLint")
    dependsOn("contractLint")
    dependsOn("assetLint")
    dependsOn("styleLint")
    dependsOn("audioLint")
    dependsOn("manifestLint")
    dependsOn("goldenScreenshot")
    dependsOn("headlessSmoke")
    dependsOn("soloClearLab")
    dependsOn("longRunLab")
    dependsOn("clientSmoke")
    dependsOn(subprojects.map { it.tasks.named("jacocoTestReport") })

    val mainSourceSets = subprojects.mapNotNull { project ->
        project.extensions.findByType<SourceSetContainer>()?.findByName("main")
    }

    additionalSourceDirs.from(mainSourceSets.map { it.allSource.srcDirs })
    sourceDirectories.from(mainSourceSets.map { it.allSource.srcDirs })
    classDirectories.from(mainSourceSets.map { it.output })
    executionData.from(
        subprojects.map { project ->
            project.fileTree(project.layout.buildDirectory) {
                include("jacoco/*.exec")
            }
        },
    )

    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.named("check") {
    dependsOn(verificationGate)
}
