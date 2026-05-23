import com.ktome.build.verification.Phase4AggregationManifestValueSource
import com.ktome.build.verification.Phase4TaskPathResolver
import com.ktome.build.verification.VerificationReportTask
import com.ktome.build.verification.VerificationTask
import com.ktome.build.testperf.TestPerfPlainTestBand
import com.ktome.build.testperf.TestPerfPlainTestOptIn
import com.ktome.build.verification.VerifyChangedPlanGate
import java.security.MessageDigest
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
    kotlin("plugin.serialization")
    id("com.ktome.build.verification")
}

val harnessReportDir = rootProject.layout.buildDirectory.dir("reports/harness")
val verifyChangedPlanOutputDir = rootProject.layout.buildDirectory.dir("verification/verify-changed")
val verifyChangedTaskPathsFile = rootProject.layout.buildDirectory.file("verification/verify-changed/task-paths.txt")
val verifyChangedPreflightTaskPathsFile = rootProject.layout.buildDirectory.file("verification/verify-changed/preflight-task-paths.txt")
val verifyChangedBaseRef = rootProject.findProperty("verifyChangedBaseRef")?.toString() ?: "origin/main"
val maintainabilityLintOutputDir = layout.buildDirectory.dir("reports/verification/maintainability")
val darkUiuxReportDir = rootProject.layout.buildDirectory.dir("reports/verification/dark-uiux")

dependencies {
    implementation(project(":client"))
    implementation(project(":game"))
    implementation(testFixtures(project(":game")))
    implementation("org.yaml:snakeyaml:${rootProject.providers.gradleProperty("snakeyamlVersion").get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
    implementation("org.junit.platform:junit-platform-engine:${rootProject.providers.gradleProperty("junitPlatformVersion").get()}")
    implementation("org.junit.platform:junit-platform-launcher:${rootProject.providers.gradleProperty("junitPlatformVersion").get()}")
    testImplementation(project(":game"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
}

val gameTestRuntimeClasspath =
    providers.provider {
        project(":game")
            .extensions
            .getByType(SourceSetContainer::class.java)
            .named("test")
            .get()
            .runtimeClasspath
    }

fun verificationSnapshotHash(vararg values: Any): Provider<String> =
    providers.provider {
        val digest = MessageDigest.getInstance("SHA-256")
        values.forEach { value ->
            when (value) {
                is String -> digest.update(value.toByteArray())
                is FileCollection ->
                    value.files
                        .sortedBy { file -> file.invariantSeparatorsPath }
                        .forEach { file ->
                            digest.update(file.relativeTo(rootProject.projectDir).invariantSeparatorsPath.toByteArray())
                            if (file.isFile) {
                                digest.update(file.readBytes())
                            }
                        }
                else -> error("Unsupported snapshot hash input: ${value::class.qualifiedName}")
            }
        }
        digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

val verificationFoundationInputs =
    files(
        rootProject.files(
            "settings.gradle.kts",
            "build.gradle.kts",
            "tools/build.gradle.kts",
            "build-logic/build.gradle.kts",
            "build-logic/settings.gradle.kts",
        ),
        rootProject.fileTree("build-logic/src/main") {
            include("**/*.kt", "**/*.java")
        },
        fileTree("src/main/kotlin/com/ktome/tools/verification") {
            include("**/*.kt")
        },
    )

val contractLintVerificationInputs =
    files(
        verificationFoundationInputs,
        fileTree("src/test/kotlin/com/ktome/tools/lint") {
            include("**/*.kt")
        },
        rootProject.fileTree("game/src/main/resources") {
            include("**/*.yaml", "**/*.yml", "**/*.json")
        },
        rootProject.fileTree("client/src/main/resources/manifests") {
            include("**/*.json")
        },
    )
val contractLintVerificationSnapshotHash =
    verificationSnapshotHash(
        "contractLint",
        "PREFLIGHT",
        "contractLint.staticGraph",
        contractLintVerificationInputs,
    )
val contractLintVerificationOutputDir = layout.buildDirectory.dir("reports/verification/contract-lint/preflight")
val contractLintVerificationReportDir = layout.buildDirectory.dir("reports/verification/contract-lint/preflight-report")
val lootPreflightInputs =
    files(
        verificationFoundationInputs,
        fileTree("src/main/kotlin/com/ktome/tools/loot") {
            include("**/*.kt")
        },
        fileTree("src/test/kotlin/com/ktome/tools/loot") {
            include("LootPreflightRunnerTest.kt")
        },
        rootProject.fileTree("game/src/main/kotlin/com/ktome/game/loot") {
            include("**/*.kt")
        },
        rootProject.fileTree("game/src/main/resources/data/items") {
            include("**/*.yaml", "**/*.yml")
        },
        rootProject.fileTree("game/src/main/resources/data/loot") {
            include("**/*.yaml", "**/*.yml")
        },
        rootProject.fileTree("game/src/main/resources/data/world") {
            include("**/*.yaml", "**/*.yml")
        },
    )
val lootPreflightOutputDir = layout.buildDirectory.dir("reports/verification/loot/preflight")
val lootPreflightSnapshotHash =
    verificationSnapshotHash(
        "loot",
        "PREFLIGHT",
        "loot.preflight",
        lootPreflightInputs,
    )
val hiddenPreflightInputs =
    files(
        verificationFoundationInputs,
        fileTree("src/main/kotlin/com/ktome/tools/hidden") {
            include("**/*.kt")
        },
        fileTree("src/test/kotlin/com/ktome/tools/hidden") {
            include("HiddenPreflightRunnerTest.kt")
        },
        rootProject.files("game/src/main/kotlin/com/ktome/game/Phase4StaticContentValidator.kt"),
        rootProject.fileTree("game/src/main/kotlin/com/ktome/game/hidden") {
            include("**/*.kt")
        },
        rootProject.fileTree("game/src/main/resources/data/events") {
            include("**/*.yaml", "**/*.yml")
        },
        rootProject.fileTree("game/src/main/resources/data/secret-zones") {
            include("**/*.yaml", "**/*.yml")
        },
        rootProject.fileTree("game/src/main/resources/data/mapgen") {
            include("**/*.yaml", "**/*.yml")
        },
    )
val hiddenPreflightOutputDir = layout.buildDirectory.dir("reports/verification/hidden/preflight")
val hiddenPreflightSnapshotHash =
    verificationSnapshotHash(
        "hidden",
        "PREFLIGHT",
        "hidden.preflight",
        hiddenPreflightInputs,
    )
val contentPackPreflightInputs =
    files(
        verificationFoundationInputs,
        fileTree("src/main/kotlin/com/ktome/tools/contentpack") {
            include("**/*.kt")
        },
        fileTree("src/test/kotlin/com/ktome/tools/contentpack") {
            include("ContentPackPreflightRunnerTest.kt")
        },
        rootProject.fileTree("game/src/main/kotlin/com/ktome/game/contentpack") {
            include("**/*.kt")
        },
        rootProject.fileTree("examples/content-packs") {
            include("**/*")
        },
        fileTree("src/main/resources/fixtures/content-packs") {
            include("**/*")
        },
    )
val contentPackPreflightOutputDir = layout.buildDirectory.dir("reports/verification/content-pack/preflight")
val contentPackPreflightSnapshotHash =
    verificationSnapshotHash(
        "content-pack",
        "PREFLIGHT",
        "content-pack.preflight",
        contentPackPreflightInputs,
    )
val mapgenOwnerInputs =
    files(
        verificationFoundationInputs,
        fileTree("src/main/kotlin/com/ktome/tools/mapgen") {
            include("**/*.kt")
        },
        fileTree("src/test/kotlin/com/ktome/tools/mapgen") {
            include("WhiteBoxMapgenHarnessTest.kt")
        },
        rootProject.fileTree("game/src/main/resources/data") {
            include("mapgen/**/*.yaml", "mapgen/**/*.yml", "world/**/*.yaml", "world/**/*.yml")
        },
    )
val mapgenOwnerOutputDir = layout.buildDirectory.dir("reports/phase4/whitebox/mapgen")
val mapgenOwnerSnapshotHash =
    verificationSnapshotHash(
        "mapgen",
        "OWNER",
        "mapgen.owner",
        mapgenOwnerInputs,
    )
val solvabilityOwnerInputs =
    files(
        verificationFoundationInputs,
        fileTree("src/main/kotlin/com/ktome/tools/mapgen") {
            include("**/*.kt")
        },
        fileTree("src/test/kotlin/com/ktome/tools/mapgen") {
            include("WhiteBoxSolvabilityHarnessTest.kt")
        },
        rootProject.fileTree("game/src/main/resources/data") {
            include("mapgen/**/*.yaml", "mapgen/**/*.yml", "world/**/*.yaml", "world/**/*.yml")
        },
    )
val solvabilityOwnerOutputDir = layout.buildDirectory.dir("reports/phase4/whitebox/solvability")
val solvabilityOwnerSnapshotHash =
    verificationSnapshotHash(
        "solvability",
        "OWNER",
        "solvability.owner",
        solvabilityOwnerInputs,
    )
val hiddenOwnerInputs =
    files(
        hiddenPreflightInputs,
        fileTree("src/test/kotlin/com/ktome/tools/hidden") {
            include("HiddenContentHarnessRunnerTest.kt")
        },
    )
val hiddenOwnerOutputDir = layout.buildDirectory.dir("reports/phase4/hidden")
val hiddenOwnerSnapshotHash =
    verificationSnapshotHash(
        "hidden",
        "OWNER",
        "hidden.owner",
        hiddenOwnerInputs,
    )
val contentPackOwnerInputs =
    files(
        contentPackPreflightInputs,
        fileTree("src/test/kotlin/com/ktome/tools/contentpack") {
            include("ContentPackHarnessRunnerTest.kt")
        },
    )
val contentPackOwnerOutputDir = layout.buildDirectory.dir("reports/phase4/content-pack")
val contentPackOwnerSnapshotHash =
    verificationSnapshotHash(
        "content-pack",
        "OWNER",
        "content-pack.owner",
        contentPackOwnerInputs,
    )
val gameHarnessSharedInputs =
    files(
        verificationFoundationInputs,
        rootProject.fileTree("game/src/main") {
            include("**/*.kt")
        },
        rootProject.fileTree("game/src/main/resources") {
            include("**/*.yaml", "**/*.yml", "**/*.json")
        },
        rootProject.fileTree("game/src/testFixtures") {
            include("**/*.kt")
        },
        rootProject.fileTree("core/src/main") {
            include("**/*.kt")
        },
        rootProject.files("game/src/test/kotlin/com/ktome/game/harness/WhiteBoxHarnessWriter.kt"),
        fileTree("src/main/kotlin/com/ktome/tools/phase4") {
            include("**/*.kt")
        },
    )
val terrainOwnerInputs =
    files(
        gameHarnessSharedInputs,
        rootProject.files("game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt"),
    )
val terrainOwnerOutputDir = layout.buildDirectory.dir("reports/phase4/whitebox/terrain")
val terrainOwnerSnapshotHash =
    verificationSnapshotHash(
        "terrain",
        "OWNER",
        "terrain.owner",
        terrainOwnerInputs,
    )
val bossOwnerInputs =
    files(
        gameHarnessSharedInputs,
        rootProject.files(
            "game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt",
            "game/src/test/kotlin/com/ktome/game/harness/OfficialSliceStabilityTest.kt",
        ),
    )
val bossOwnerOutputDir = layout.buildDirectory.dir("reports/phase4/whitebox/boss")
val bossOwnerSnapshotHash =
    verificationSnapshotHash(
        "boss",
        "OWNER",
        "boss.owner",
        bossOwnerInputs,
    )
val scopeCoverageLintInputs =
    files(
        verificationFoundationInputs,
        fileTree("src/test/kotlin/com/ktome/tools/verification") {
            include("ScopeCoverageLintTest.kt", "VerificationImpactAnalyzerTest.kt")
        },
        rootProject.files(
            "game/src/main/kotlin/com/ktome/game/data/DataLoader.kt",
            "game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt",
            "game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt",
        ),
    )
val scopeCoverageLintOutputDir = layout.buildDirectory.dir("reports/verification/scope-coverage/lint")
val scopeCoverageLintSnapshotHash =
    verificationSnapshotHash(
        "scopeCoverage",
        "PREFLIGHT",
        "scopeCoverage.lint",
        scopeCoverageLintInputs,
    )

tasks.withType<Test>().configureEach {
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
    providers.systemProperty("ktome.updateMapgenGolden").orNull?.let { value ->
        systemProperty("ktome.updateMapgenGolden", value)
    }
    providers.systemProperty("ktome.updateSolvabilityGolden").orNull?.let { value ->
        systemProperty("ktome.updateSolvabilityGolden", value)
    }
    if (name == "test") {
        useJUnitPlatform {
            excludeTags("verificationFixtureFailure")
        }
    }
}

tasks.register<JavaExec>("prepareVerifyChangedPlan") {
    group = "verification"
    description = "Builds the verifyChanged impact plan from current repo changes."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.ktome.tools.verification.VerificationCli")
    workingDir = rootProject.projectDir
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
    args(
        "plan-changed",
        "--output-dir",
        verifyChangedPlanOutputDir.get().asFile.absolutePath,
        "--cache-status",
        "LOCAL_EXECUTION",
        "--base-ref",
        verifyChangedBaseRef,
    )
    inputs.property("baseRef", verifyChangedBaseRef)
    outputs.dir(verifyChangedPlanOutputDir)
    outputs.upToDateWhen { false }
}

tasks.register<Test>("combatTraceGolden") {
    group = "verification"
    description = "Runs the Phase 3 FORMULA corpus golden trace harness."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("combatTraceGolden")
    }
    systemProperty("ktome.harness.reportDir", harnessReportDir.get().asFile.absolutePath)
    providers.systemProperty("ktome.updateCombatGolden").orNull?.let { value ->
        systemProperty("ktome.updateCombatGolden", value)
    }
    outputs.dir(harnessReportDir)
}

tasks.register<Test>("localeLint") {
    group = "verification"
    description = "Runs locale bundle lint and placeholder validation."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("localeLint")
    }
}

tasks.register<Test>("contractLint") {
    group = "verification"
    description = "Runs schema V2 structure and cross-reference validation."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("contractLint")
    }
}

tasks.register<Test>("keywordRegistryLint") {
    group = "verification"
    description = "Validates keyword registry consumers and formal description coverage probes."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("keywordRegistryLint")
    }
}

tasks.register<Test>("maintainabilityLint") {
    group = "verification"
    description = "Runs the anti-bloat maintainability lint against the versioned debt baseline."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("maintainabilityLint")
    }
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemProperty("ktome.maintainability.baselinePath", rootProject.file("maintainability-baseline.json").absolutePath)
    systemProperty("ktome.maintainability.reportDir", maintainabilityLintOutputDir.get().asFile.absolutePath)
    systemProperty(
        "ktome.maintainability.blockingMode",
        providers.systemProperty("ktome.maintainability.blockingMode").orElse("true").get(),
    )
    inputs.files(
        rootProject.fileTree("core/src/main/kotlin") { include("**/*.kt") },
        rootProject.fileTree("game/src/main/kotlin") { include("**/*.kt") },
        rootProject.fileTree("client/src/main/kotlin") { include("**/*.kt") },
        rootProject.fileTree("tools/src/main/kotlin") { include("**/*.kt") },
        rootProject.fileTree("build-logic/src/main/kotlin") { include("**/*.kt") },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(
        rootProject.files(
            "docs/rule/ai-change-governance.md",
            "build.gradle.kts",
            "tools/build.gradle.kts",
        ),
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(rootProject.file("maintainability-baseline.json")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("ktome.maintainability.blockingMode", providers.systemProperty("ktome.maintainability.blockingMode").orElse("true"))
    outputs.dir(maintainabilityLintOutputDir)
}

tasks.register<Test>("acceptanceContractLint") {
    group = "verification"
    description = "Validates Phase4 v4 PR acceptance matrices and governance links."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("acceptanceContractLint")
    }
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
    inputs.files(
        rootProject.files(
            "docs/review/phase4/v4-pr/README.md",
            "docs/review/phase4/v4-pr/development-governance.md",
            "UI/pr/README.md",
            "UI/pr/development-governance.md",
            "docs/verification/README.md",
            "docs/rule/ai-change-governance.md",
            "tools/build.gradle.kts",
            "build.gradle.kts",
        ),
        rootProject.fileTree("docs/review/phase4/v4-pr") {
            include("2026-04-24-phase4-v4-pr0*.md")
        },
        rootProject.fileTree("UI/pr") {
            include("dark-uiux-pr*.md")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.register<Exec>("darkKeyRegistryLint") {
    group = "verification"
    description = "Validates dark-v1 key registry ownership, fallbacks, aliases, and sheet consistency."
    workingDir = rootProject.projectDir
    commandLine(
        "python3",
        "scripts/verify_dark_key_registry.py",
        "--report",
        darkUiuxReportDir.get().file("key-registry.json").asFile.path,
    )
    inputs.files(
        rootProject.files(
            "scripts/verify_dark_key_registry.py",
            "scripts/dark_sprite_sheet_contract.py",
            "scripts/asset_pipeline_common.py",
            "UI/sprite-sheets/key-registry.yaml",
            "UI/sprite-sheets/sheet-plan.yaml",
            "assets-src/image/manifests/phase2-visual-manifest.json",
        ),
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(darkUiuxReportDir.map { dir -> dir.file("key-registry.json") })
}

val darkSpriteSheetRequireFullGrid =
    rootProject.providers.gradleProperty("ktome.darkUiux.requireFullGrid").orElse("false")
val darkUiuxOwnerContract =
    rootProject.providers.gradleProperty("ktome.darkUiux.ownerContract").orElse("")

tasks.register<Exec>("darkSpriteSheetLint") {
    group = "verification"
    description = "Validates dark-v1 sheet-plan schema, style tag, grid policy, cells, aliases, and repo-relative paths."
    workingDir = rootProject.projectDir
    val command =
        mutableListOf(
            "python3",
            "scripts/verify_sprite_sheet_map.py",
            "--check",
            "sheet-plan",
        )
    if (darkSpriteSheetRequireFullGrid.get().toBooleanStrictOrNull() == true) {
        command += "--require-full-grid"
    }
    if (darkUiuxOwnerContract.get().isNotBlank()) {
        command += listOf("--owner-contract", darkUiuxOwnerContract.get())
    }
    commandLine(command)
    inputs.files(
        rootProject.files(
            "scripts/verify_sprite_sheet_map.py",
            "scripts/dark_sprite_sheet_contract.py",
            "scripts/asset_pipeline_common.py",
            "UI/sprite-sheets/sheet-plan.yaml",
        ),
        rootProject.fileTree("UI/sprite-sheets/owner-contracts") {
            include("*.yaml")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("ktome.darkUiux.requireFullGrid", darkSpriteSheetRequireFullGrid)
    inputs.property("ktome.darkUiux.ownerContract", darkUiuxOwnerContract)
}

val darkSpriteMapReportPath =
    rootProject.providers.gradleProperty("ktome.darkUiux.spriteMapReport")
        .orElse("assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl")
val darkSpriteMapRequireReviewedQa =
    rootProject.providers.gradleProperty("ktome.darkUiux.requireReviewedQa")
        .orElse("false")
val darkSpriteMapReportSheetIds =
    rootProject.providers.gradleProperty("ktome.darkUiux.spriteMapReportSheetIds")
        .orElse("")
val darkArtRandomQaSheetIds =
    rootProject.providers.gradleProperty("ktome.darkUiux.artRandomQaSheetIds")
        .orElse(
            "r07-items-base,r07-items-unique-artifact,r07-items-affix-material," +
                "r08-skills-vanguard-berserker,r08-skills-templar-rogue,r08-skills-arcanist-spellblade," +
                "r09-status-damage,r09-quest-zone-profession,r09-fallback-debug,r09-rejected-polish",
        )
val darkArtRandomQaOut =
    rootProject.providers.gradleProperty("ktome.darkUiux.artRandomQaOut")
        .orElse("UI/manual-records/dark-uiux-pr06-art-random-qa.json")
val darkArtRandomQaSampleRoot =
    rootProject.providers.gradleProperty("ktome.darkUiux.artRandomQaSampleRoot")
        .orElse("build/reports/verification/dark-uiux/random-qa")

tasks.register<Exec>("spriteSheetMapLint") {
    group = "verification"
    description = "Validates dark-v1 raw sheet dimensions, contact sheet, hash report, and manifest rawOutputPath mapping."
    workingDir = rootProject.projectDir
    val command =
        mutableListOf(
            "python3",
            "scripts/verify_sprite_sheet_map.py",
            "--check",
            "map",
            "--report",
            darkSpriteMapReportPath.get(),
        )
    if (darkSpriteMapRequireReviewedQa.get().toBooleanStrictOrNull() == true) {
        command += "--require-reviewed-qa"
    }
    if (darkSpriteMapReportSheetIds.get().isNotBlank()) {
        command += listOf("--report-sheet-ids", darkSpriteMapReportSheetIds.get())
    }
    commandLine(command)
    inputs.files(
        rootProject.files(
            "scripts/verify_sprite_sheet_map.py",
            "scripts/dark_sprite_sheet_contract.py",
            "scripts/asset_pipeline_common.py",
            "UI/sprite-sheets/sheet-plan.yaml",
            "assets-src/image/manifests/phase2-visual-manifest.json",
        ),
        rootProject.fileTree("assets-src/image/raw/sheets/dark-v1") {
            include("*.png")
        },
        rootProject.fileTree("assets-src/image/contact-sheets/dark-v1") {
            include("*.png")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("ktome.darkUiux.spriteMapReport", darkSpriteMapReportPath)
    inputs.property("ktome.darkUiux.requireReviewedQa", darkSpriteMapRequireReviewedQa)
    inputs.property("ktome.darkUiux.spriteMapReportSheetIds", darkSpriteMapReportSheetIds)
    outputs.file(rootProject.layout.projectDirectory.file(darkSpriteMapReportPath.get()))
}

tasks.register<Exec>("darkArtRandomQa") {
    group = "verification"
    description = "Writes deterministic random art QA samples for PR06 dark-v1 sprite sheets."
    workingDir = rootProject.projectDir
    commandLine(
        "python3",
        "scripts/generate_dark_art_random_qa.py",
        "--sheet-ids",
        darkArtRandomQaSheetIds.get(),
        "--out",
        darkArtRandomQaOut.get(),
        "--sample-root",
        darkArtRandomQaSampleRoot.get(),
        "--overwrite",
    )
    inputs.files(
        rootProject.files(
            "scripts/generate_dark_art_random_qa.py",
            "scripts/dark_sprite_sheet_contract.py",
            "scripts/asset_pipeline_common.py",
            "UI/sprite-sheets/sheet-plan.yaml",
            "UI/sprite-sheets/prompts/dark-v1/prompt-index.json",
        ),
        rootProject.fileTree("assets-src/image/raw/sheets/dark-v1") {
            include("*.png")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("ktome.darkUiux.artRandomQaSheetIds", darkArtRandomQaSheetIds)
    inputs.property("ktome.darkUiux.artRandomQaOut", darkArtRandomQaOut)
    inputs.property("ktome.darkUiux.artRandomQaSampleRoot", darkArtRandomQaSampleRoot)
    outputs.file(rootProject.layout.projectDirectory.file(darkArtRandomQaOut.get()))
    outputs.dir(rootProject.layout.projectDirectory.dir(darkArtRandomQaSampleRoot.get()))
}

fun registerDarkManifestCoverageTask(
    name: String,
    mode: String,
    ownerPr: String,
    reportFileName: String,
    ownerContract: String = "",
): TaskProvider<Exec> =
    tasks.register<Exec>(name) {
        group = "verification"
        description = "Validates dark-v1 manifest coverage in $mode mode."
        workingDir = rootProject.projectDir
        val command =
            mutableListOf(
                "python3",
                "scripts/verify_dark_manifest_coverage.py",
                "--coverage-mode",
                mode,
                "--report",
                darkUiuxReportDir.get().file(reportFileName).asFile.path,
            )
        if (ownerPr.isNotBlank()) {
            command += listOf("--owner-pr", ownerPr)
        }
        if (ownerContract.isNotBlank()) {
            command += listOf("--owner-contract", ownerContract)
        }
        commandLine(command)
        inputs.files(
            rootProject.files(
                "scripts/verify_dark_manifest_coverage.py",
                "scripts/dark_sprite_sheet_contract.py",
                "scripts/asset_pipeline_common.py",
                "UI/sprite-sheets/key-registry.yaml",
                "UI/sprite-sheets/sheet-plan.yaml",
                "assets-src/image/manifests/phase2-visual-manifest.json",
                "client/src/main/resources/manifests/visual-manifest.json",
            ),
        ).withPathSensitivity(PathSensitivity.RELATIVE)
        if (ownerContract.isNotBlank()) {
            inputs.file(rootProject.file(ownerContract)).withPathSensitivity(PathSensitivity.RELATIVE)
        }
        inputs.property("ktome.darkUiux.fixedOwnerContract", ownerContract)
        outputs.file(darkUiuxReportDir.map { dir -> dir.file(reportFileName) })
    }

val darkCoverageMode = rootProject.providers.gradleProperty("ktome.darkUiux.coverageMode").orElse("final-full")
val darkCoverageOwnerPr = rootProject.providers.gradleProperty("ktome.darkUiux.ownerPr").orElse("")
val darkCoverageRequiredOwnerSheetIds =
    rootProject.providers.gradleProperty("ktome.darkUiux.requiredOwnerSheetIds").orElse("")
val darkCoverageExpectedInventory = rootProject.providers.gradleProperty("ktome.darkUiux.expectedInventory").orElse("")
val darkCoveragePackagedSentinelEvidence =
    rootProject.providers.gradleProperty("ktome.darkUiux.packagedSentinelEvidence").orElse("")
val darkCoverageArtRandomQaRecord =
    rootProject.providers.gradleProperty("ktome.darkUiux.artRandomQaRecord")
        .orElse("UI/manual-records/dark-uiux-pr06-art-random-qa.json")

tasks.register<Exec>("darkManifestCoverageLint") {
    group = "verification"
    description = "Validates dark-v1 manifest coverage using ktome.darkUiux.coverageMode."
    workingDir = rootProject.projectDir
    val command =
        mutableListOf(
            "python3",
            "scripts/verify_dark_manifest_coverage.py",
            "--coverage-mode",
            darkCoverageMode.get(),
            "--report",
            darkUiuxReportDir.get().file("dark-v1-manifest-coverage.json").asFile.path,
        )
    if (darkCoverageOwnerPr.get().isNotBlank()) {
        command += listOf("--owner-pr", darkCoverageOwnerPr.get())
    }
    if (darkCoverageRequiredOwnerSheetIds.get().isNotBlank()) {
        command += listOf("--required-owner-sheet-ids", darkCoverageRequiredOwnerSheetIds.get())
    }
    if (darkUiuxOwnerContract.get().isNotBlank()) {
        command += listOf("--owner-contract", darkUiuxOwnerContract.get())
    }
    if (darkCoverageExpectedInventory.get().isNotBlank()) {
        command += listOf("--expected-inventory", darkCoverageExpectedInventory.get())
    }
    if (darkCoverageArtRandomQaRecord.get().isNotBlank()) {
        command += listOf("--art-random-qa-record", darkCoverageArtRandomQaRecord.get())
    }
    if (darkCoveragePackagedSentinelEvidence.get().isNotBlank()) {
        command += listOf("--packaged-sentinel-evidence", darkCoveragePackagedSentinelEvidence.get())
    }
    commandLine(command)
    inputs.files(
        rootProject.files(
            "scripts/verify_dark_manifest_coverage.py",
            "scripts/generate_dark_final_full_inventory.py",
            "scripts/dark_sprite_sheet_contract.py",
            "scripts/asset_pipeline_common.py",
            "UI/sprite-sheets/key-registry.yaml",
            "UI/sprite-sheets/sheet-plan.yaml",
            "assets-src/image/manifests/phase2-visual-manifest.json",
            "client/src/main/resources/manifests/visual-manifest.json",
            "UI/pr/screen-coverage-matrix.md",
        ),
        rootProject.fileTree("UI/sprite-sheets/owner-contracts") {
            include("*.yaml")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    if (darkCoverageExpectedInventory.get().isNotBlank()) {
        inputs.file(rootProject.file(darkCoverageExpectedInventory.get())).withPathSensitivity(PathSensitivity.RELATIVE)
    }
    if (darkCoverageArtRandomQaRecord.get().isNotBlank()) {
        inputs.file(rootProject.file(darkCoverageArtRandomQaRecord.get())).withPathSensitivity(PathSensitivity.RELATIVE)
    }
    darkCoveragePackagedSentinelEvidence.get()
        .split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)
        .forEach { evidencePath ->
            inputs.file(rootProject.file(evidencePath)).withPathSensitivity(PathSensitivity.RELATIVE)
        }
    inputs.property("ktome.darkUiux.coverageMode", darkCoverageMode)
    inputs.property("ktome.darkUiux.ownerPr", darkCoverageOwnerPr)
    inputs.property("ktome.darkUiux.requiredOwnerSheetIds", darkCoverageRequiredOwnerSheetIds)
    inputs.property("ktome.darkUiux.ownerContract", darkUiuxOwnerContract)
    inputs.property("ktome.darkUiux.expectedInventory", darkCoverageExpectedInventory)
    inputs.property("ktome.darkUiux.artRandomQaRecord", darkCoverageArtRandomQaRecord)
    inputs.property("ktome.darkUiux.packagedSentinelEvidence", darkCoveragePackagedSentinelEvidence)
    outputs.file(darkUiuxReportDir.map { dir -> dir.file("dark-v1-manifest-coverage.json") })
}

tasks.register<Exec>("resourcePipelineLint") {
    group = "verification"
    description = "Validates the project-wide resource generation authority inventory across image, sheet, and audio plans."
    dependsOn(rootProject.tasks.named("syncPhase2Manifests"))
    commandLine(
        "python3",
        "scripts/resource_pipeline_authority_lint.py",
        "--report",
        layout.buildDirectory.file("reports/resource-pipeline/resource-pipeline-authority.json").get().asFile.path,
    )
    workingDir = rootProject.projectDir
    inputs.files(
        rootProject.files(
            "scripts/resource_pipeline_authority_lint.py",
            "scripts/asset_pipeline_common.py",
            "scripts/dark_sprite_sheet_contract.py",
            "UI/sprite-sheets/key-registry.yaml",
            "UI/sprite-sheets/sheet-plan.yaml",
            "assets-src/image/manifests/phase2-visual-manifest.json",
            "assets-src/audio/manifests/phase2-audio-manifest.json",
            "client/src/main/resources/manifests/visual-manifest.json",
            "client/src/main/resources/manifests/audio-manifest.json",
        ),
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(rootProject.fileTree("assets-src/image/specs") { include("*.yaml") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(rootProject.fileTree("assets-src/audio/specs") { include("*.yaml") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(rootProject.fileTree("core/src/main/kotlin") { include("**/*.kt") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(rootProject.fileTree("game/src/main/kotlin") { include("**/*.kt") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(rootProject.fileTree("client/src/main/kotlin") { include("**/*.kt") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(rootProject.fileTree("tools/src/main/kotlin") { include("**/*.kt") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(layout.buildDirectory.file("reports/resource-pipeline/resource-pipeline-authority.json"))
}

registerDarkManifestCoverageTask(
    name = "darkManifestCoveragePr00DryRun",
    mode = "pr00-dry-run",
    ownerPr = "",
    reportFileName = "dark-v1-manifest-coverage-pr00-dry-run.json",
)

registerDarkManifestCoverageTask(
    name = "darkManifestCoveragePr02OwnerScope",
    mode = "owner-scope",
    ownerPr = "PR-02",
    ownerContract = "UI/sprite-sheets/owner-contracts/pr02-owner-keys.yaml",
    reportFileName = "dark-v1-manifest-coverage-pr02-owner-scope.json",
)

registerDarkManifestCoverageTask(
    name = "darkManifestCoveragePr02_1OwnerScope",
    mode = "owner-scope",
    ownerPr = "PR-02-1",
    ownerContract = "UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml",
    reportFileName = "dark-v1-manifest-coverage-pr02-1-owner-scope.json",
)

registerDarkManifestCoverageTask(
    name = "darkManifestCoveragePr02_2OwnerScope",
    mode = "owner-scope",
    ownerPr = "PR-02-2",
    ownerContract = "UI/sprite-sheets/owner-contracts/pr02-2-owner-keys.yaml",
    reportFileName = "dark-v1-manifest-coverage-pr02-2-owner-scope.json",
)

tasks.register<VerificationTask>("verifyContractLintPreflight") {
    description = "Runs the contractLint STATIC_GRAPH demo through the unified verification task foundation."
    domainId.set("contractLint")
    tier.set("PREFLIGHT")
    nodeId.set("contractLint.staticGraph")
    inputSnapshotHash.set(contractLintVerificationSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(contractLintVerificationInputs)
    outputDir.set(contractLintVerificationOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
}

tasks.register<VerificationReportTask>("verifyContractLintPreflightReport") {
    description = "Rebuilds the contractLint STATIC_GRAPH summary from existing artifacts without rerunning producer tests."
    domainId.set("contractLint")
    tier.set("PREFLIGHT")
    nodeId.set("contractLint.staticGraph")
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    artifactInputs.from(contractLintVerificationOutputDir)
    outputDir.set(contractLintVerificationReportDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    mustRunAfter("verifyContractLintPreflight")
}

tasks.register<VerificationTask>("verifyLootPreflight") {
    description = "Runs the Phase 4 loot static preflight through the unified verification task path."
    domainId.set("loot")
    tier.set("PREFLIGHT")
    nodeId.set("loot.preflight")
    inputSnapshotHash.set(lootPreflightSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(lootPreflightInputs)
    outputDir.set(lootPreflightOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.loot.preflight.reportDir", lootPreflightOutputDir.get().asFile.absolutePath)
}

tasks.register<VerificationTask>("verifyHiddenPreflight") {
    description = "Runs the Phase 4 hidden-content static preflight through the unified verification task path."
    domainId.set("hidden")
    tier.set("PREFLIGHT")
    nodeId.set("hidden.preflight")
    inputSnapshotHash.set(hiddenPreflightSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(hiddenPreflightInputs)
    outputDir.set(hiddenPreflightOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.hidden.preflight.reportDir", hiddenPreflightOutputDir.get().asFile.absolutePath)
}

tasks.register<VerificationTask>("verifyContentPackPreflight") {
    description = "Runs the Phase 4 content-pack static preflight through the unified verification task path."
    domainId.set("content-pack")
    tier.set("PREFLIGHT")
    nodeId.set("content-pack.preflight")
    inputSnapshotHash.set(contentPackPreflightSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(contentPackPreflightInputs)
    outputDir.set(contentPackPreflightOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.contentPack.preflight.reportDir", contentPackPreflightOutputDir.get().asFile.absolutePath)
}

tasks.register<VerificationTask>("scopeCoverageLint") {
    description = "Runs the static scope coverage lint for Phase 4 impact analysis."
    domainId.set("scopeCoverage")
    tier.set("PREFLIGHT")
    nodeId.set("scopeCoverage.lint")
    inputSnapshotHash.set(scopeCoverageLintSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(scopeCoverageLintInputs)
    outputDir.set(scopeCoverageLintOutputDir)
    systemPropertiesMap.put("ktome.phase4.scopeCoverage.reportDir", scopeCoverageLintOutputDir.get().asFile.absolutePath)
}

tasks.register<Test>("mapgenSmoke") {
    group = "verification"
    description = "Runs the Phase 4 map generation smoke baseline."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("mapgenSmoke")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/mapgen")
    systemProperty("ktome.phase4.mapgen.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}

tasks.register<Test>("solvabilityHarness") {
    group = "verification"
    description = "Runs the Phase 4 solvability proof harness and writes structured reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("solvabilityHarness")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/solvability")
    systemProperty("ktome.phase4.solvability.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}

tasks.register<VerificationTask>("hiddenContentHarness") {
    description = "Runs the Phase 4 hidden-content owner domain through the unified verification task path."
    domainId.set("hidden")
    tier.set("OWNER")
    nodeId.set("hidden.owner")
    inputSnapshotHash.set(hiddenOwnerSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(hiddenOwnerInputs)
    outputDir.set(hiddenOwnerOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.hidden.reportDir", hiddenOwnerOutputDir.get().asFile.absolutePath)
}

tasks.register<Test>("organicHiddenProbe") {
    group = "verification"
    description = "Runs the Phase 4 organic hidden-content probe and writes structured reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("organicHiddenProbe")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/hidden")
    systemProperty("ktome.phase4.hidden.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}

tasks.register<VerificationTask>("contentPackHarness") {
    description = "Runs the Phase 4 content-pack owner domain through the unified verification task path."
    domainId.set("content-pack")
    tier.set("OWNER")
    nodeId.set("content-pack.owner")
    inputSnapshotHash.set(contentPackOwnerSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(contentPackOwnerInputs)
    outputDir.set(contentPackOwnerOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.contentPack.reportDir", contentPackOwnerOutputDir.get().asFile.absolutePath)
    doNotTrackState("contentPackHarness must stay fresh when paired with whiteBoxContentPack artifact freshness checks.")
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
}

tasks.register<VerificationTask>("whiteBoxMapgen") {
    description = "Runs the Phase 4 mapgen owner domain through the unified verification task path."
    domainId.set("mapgen")
    tier.set("OWNER")
    nodeId.set("mapgen.owner")
    inputSnapshotHash.set(mapgenOwnerSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(mapgenOwnerInputs)
    outputDir.set(mapgenOwnerOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.whitebox.mapgen.reportDir", mapgenOwnerOutputDir.get().asFile.absolutePath)
}

tasks.register<VerificationTask>("whiteBoxSolvability") {
    description = "Runs the Phase 4 solvability owner domain through the unified verification task path."
    domainId.set("solvability")
    tier.set("OWNER")
    nodeId.set("solvability.owner")
    inputSnapshotHash.set(solvabilityOwnerSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath)
    sourceInputs.from(solvabilityOwnerInputs)
    outputDir.set(solvabilityOwnerOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.whitebox.solvability.reportDir", solvabilityOwnerOutputDir.get().asFile.absolutePath)
}

tasks.register<Test>("lootBalanceLab") {
    group = "verification"
    description = "Runs the Phase 4 loot balance lab and writes structured reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("lootBalanceLab")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/loot")
    systemProperty("ktome.phase4.loot.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
    TestPerfPlainTestOptIn.monitor(this, TestPerfPlainTestBand.HEAVY_EVALUATION)
}

tasks.register<Test>("whiteBoxLoot") {
    group = "verification"
    description = "Runs the Phase 4 unified white-box loot pilot and writes standard reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("whiteBoxLoot")
    }
    dependsOn("lootBalanceLab", "verifyLootPreflight")
    systemProperty("ktome.phase4.loot.reportDir", layout.buildDirectory.dir("reports/phase4/loot").get().asFile.absolutePath)
    systemProperty("ktome.phase4.loot.preflight.reportDir", lootPreflightOutputDir.get().asFile.absolutePath)
    systemProperty("ktome.phase4.reuseHarnessOutputs", "true")
    systemProperty("ktome.phase4.whitebox.loot.allowKernelFallback", "false")
    val reportDir = layout.buildDirectory.dir("reports/phase4/whitebox/loot")
    systemProperty("ktome.phase4.whitebox.loot.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
    TestPerfPlainTestOptIn.monitor(this, TestPerfPlainTestBand.HEAVY_EVALUATION)
}

tasks.register<Test>("lootVerificationCacheContract") {
    group = "verification"
    description = "Verifies Phase 4 loot kernel and white-box evaluation cache contracts outside the default owner gate."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("lootVerificationCacheContract")
    }
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemProperty("ktome.phase4.reuseHarnessOutputs", "true")
    systemProperty("ktome.phase4.whitebox.loot.allowKernelFallback", "true")
    TestPerfPlainTestOptIn.monitor(this, TestPerfPlainTestBand.HEAVY_EVALUATION)
}

tasks.register<Test>("whiteBoxHiddenContent") {
    group = "verification"
    description = "Runs the Phase 4 unified white-box hidden-content domain and writes standard reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("whiteBoxHiddenContent")
    }
    dependsOn("hiddenContentHarness")
    systemProperty("ktome.phase4.reuseHarnessOutputs", "true")
    val reportDir = layout.buildDirectory.dir("reports/phase4/whitebox/hidden")
    systemProperty("ktome.phase4.hidden.reportDir", layout.buildDirectory.dir("reports/phase4/hidden").get().asFile.absolutePath)
    outputs.dir(reportDir)
    TestPerfPlainTestOptIn.monitor(this, TestPerfPlainTestBand.HEAVY_EVALUATION)
}

tasks.register<VerificationTask>("terrainInteractionBatch") {
    description = "Runs the Phase 4 terrain owner domain through the unified verification task path."
    domainId.set("terrain")
    tier.set("OWNER")
    nodeId.set("terrain.owner")
    inputSnapshotHash.set(terrainOwnerSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath, gameTestRuntimeClasspath)
    sourceInputs.from(terrainOwnerInputs)
    outputDir.set(terrainOwnerOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.phase4.whitebox.terrain.reportDir", terrainOwnerOutputDir.get().asFile.absolutePath)
}

tasks.register<VerificationTask>("bossHarness") {
    description = "Runs the Phase 4 boss owner domain through the unified verification task path."
    domainId.set("boss")
    tier.set("OWNER")
    nodeId.set("boss.owner")
    inputSnapshotHash.set(bossOwnerSnapshotHash)
    runtimeClasspath.from(sourceSets.test.get().runtimeClasspath, gameTestRuntimeClasspath)
    sourceInputs.from(bossOwnerInputs)
    outputDir.set(bossOwnerOutputDir)
    systemPropertiesMap.put("ktome.repo.root", rootProject.projectDir.absolutePath)
    systemPropertiesMap.put("ktome.harness.reportDir", bossOwnerOutputDir.get().asFile.absolutePath)
    systemPropertiesMap.put("ktome.phase4.whitebox.boss.reportDir", bossOwnerOutputDir.get().asFile.absolutePath)
}

tasks.register<Test>("whiteBoxContentPack") {
    group = "verification"
    description = "Runs the Phase 4 unified white-box content-pack domain and writes standard reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("whiteBoxContentPack")
    }
    dependsOn("contentPackHarness")
    mustRunAfter("contentPackHarness")
    val reportDir = layout.buildDirectory.dir("reports/phase4/whitebox/content-pack")
    val pairedHarnessSummary = layout.buildDirectory.file("reports/phase4/content-pack/content-pack-summary.json")
    systemProperty("ktome.phase4.whitebox.contentPack.reportDir", reportDir.get().asFile.absolutePath)
    systemProperty("ktome.phase4.contentPack.reportDir", layout.buildDirectory.dir("reports/phase4/content-pack").get().asFile.absolutePath)
    inputs.file(pairedHarnessSummary)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(reportDir)
    doNotTrackState("whiteBoxContentPack consumes a freshly produced contentPackHarness summary.")
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    TestPerfPlainTestOptIn.monitor(this, TestPerfPlainTestBand.HEAVY_EVALUATION)
}

listOf(
    "contractLint",
    "hiddenContentHarness",
    "lootBalanceLab",
    "maintainabilityLint",
    "organicHiddenProbe",
    "terrainInteractionBatch",
    "verifyLootPreflight",
    "whiteBoxLoot",
    "whiteBoxMapgen",
    "whiteBoxSolvability",
).forEach { taskName ->
    tasks.named(taskName) {
        mustRunAfter("whiteBoxContentPack")
    }
}

val legacyPhase4ReportDir = layout.buildDirectory.dir("reports/phase4")
val unifiedPhase4ReportDir = layout.buildDirectory.dir("reports/verification/phase4")
val legacyPhase4ReportOutputs =
    files(
        legacyPhase4ReportDir.map { directory -> directory.file("phase4-summary.json").asFile },
        legacyPhase4ReportDir.map { directory -> directory.file("phase4-summary.md").asFile },
    )
val unifiedPhase4ReportOutputs =
    files(
        unifiedPhase4ReportDir.map { directory -> directory.file("report-phase4-summary.json").asFile },
        unifiedPhase4ReportDir.map { directory -> directory.file("report-phase4-summary.md").asFile },
    )
val unifiedPhase4ParityOutputs =
    files(
        unifiedPhase4ReportOutputs,
        unifiedPhase4ReportDir.map { directory -> directory.file("report-phase4-legacy-comparison.json").asFile },
    )
val phase4OwnerBaselineInputs =
    rootProject.files(
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-24-phase4-profession-tree-run-choice-owner-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-24-phase4-inscription-shop-replacement-owner-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-16-phase4-boss-phase-identity-owner-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json",
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-terrain-per-zone-lower-bound-baseline.json",
        "docs/review/phase4/opt/baselines/2026-04-24-phase4-sample-pack-add-first-owner-baseline.json",
    )
val phase4AggregationManifest =
    providers.of(Phase4AggregationManifestValueSource::class) {
        parameters.manifestFile.set(rootProject.layout.projectDirectory.file("tools/src/main/resources/phase4/aggregation-manifest.yaml"))
    }.get()
val phase4AggregateProducerArtifactRelativePaths =
    phase4AggregationManifest.artifactRelativePaths
val phase4AggregateProducerTaskPaths =
    phase4AggregationManifest.taskPaths
val phase4AggregateProducerInputs =
    rootProject.files(
        phase4AggregateProducerArtifactRelativePaths.map { relativePath -> rootProject.file(relativePath) },
        phase4OwnerBaselineInputs,
    )
val phase4AggregateProducerTasks: List<TaskProvider<out Task>> =
    phase4AggregateProducerTaskPaths.map { taskPath -> Phase4TaskPathResolver.resolve(rootProject, taskPath) }
val soloClearLabProducerTask: TaskProvider<out Task> = project(":game").tasks.named("soloClearLab")
val gameBossHarnessProducerTask: TaskProvider<out Task> = project(":game").tasks.named("bossHarness")
val gameTerrainInteractionProducerTask: TaskProvider<out Task> = project(":game").tasks.named("terrainInteractionBatch")
val clientSmokeProducerTask: TaskProvider<out Task> = project(":client").tasks.named("clientSmoke")
val phase4AggregateGameAliasProducers = listOf(gameBossHarnessProducerTask, gameTerrainInteractionProducerTask)
val phase4AggregateClientAliasProducers = listOf(clientSmokeProducerTask)
val legacyPhase4SummaryInput = layout.buildDirectory.file("reports/phase4/phase4-summary.json")
val testSourceSet = sourceSets.test.get()

fun registerPhase4AggregateTask(
    name: String,
    descriptionText: String,
    includeTag: String,
    outputDir: Provider<org.gradle.api.file.Directory>,
    outputArtifacts: FileCollection,
    producerInputs: FileCollection,
    producerTasks: List<TaskProvider<out Task>>,
    additionalDependsOn: List<TaskProvider<out Task>> = emptyList(),
    additionalInputs: FileCollection = files(),
    additionalMustRunAfter: List<TaskProvider<out Task>> = emptyList(),
    aggregateReportDir: Provider<org.gradle.api.file.Directory>? = null,
    legacyReportDir: Provider<org.gradle.api.file.Directory>? = null,
    compareLegacy: Boolean? = null,
) {
    tasks.register<Test>(name) {
        group = "verification"
        description = descriptionText
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnitPlatform {
            includeTags(includeTag)
            if (includeTag == "reportPhase4") {
                excludeTags("phase4AggregationInput")
                excludeTags("reportPhase4Fixture")
            }
        }
        systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
        aggregateReportDir?.let {
            systemProperty("ktome.phase4.aggregate.reportDir", it.get().asFile.absolutePath)
        }
        legacyReportDir?.let {
            systemProperty("ktome.phase4.reportDir", it.get().asFile.absolutePath)
        }
        compareLegacy?.let {
            systemProperty("ktome.phase4.aggregate.compareLegacy", it.toString())
        }
        dependsOn(additionalDependsOn)
        mustRunAfter(producerTasks)
        mustRunAfter(additionalMustRunAfter)
        inputs.files(producerInputs)
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.files(additionalInputs)
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.files(outputArtifacts)
        TestPerfPlainTestOptIn.monitor(this, TestPerfPlainTestBand.LIGHT_AGGREGATE)
    }
}

registerPhase4AggregateTask(
    name = "phase4LegacyReport",
    descriptionText = "Manually rebuilds the legacy Phase 4 aggregate summary from existing artifacts without rerunning producer tasks.",
    includeTag = "phase4LegacyReport",
    outputDir = legacyPhase4ReportDir,
    outputArtifacts = legacyPhase4ReportOutputs,
    producerInputs = phase4AggregateProducerInputs,
    producerTasks = phase4AggregateProducerTasks,
    additionalMustRunAfter = listOf(soloClearLabProducerTask) + phase4AggregateGameAliasProducers + phase4AggregateClientAliasProducers,
    legacyReportDir = legacyPhase4ReportDir,
)

registerPhase4AggregateTask(
    name = "phase4LegacyReportOnly",
    descriptionText = "Explicit artifact-only alias for manually rebuilding the legacy Phase 4 aggregate report.",
    includeTag = "phase4LegacyReport",
    outputDir = legacyPhase4ReportDir,
    outputArtifacts = legacyPhase4ReportOutputs,
    producerInputs = phase4AggregateProducerInputs,
    producerTasks = phase4AggregateProducerTasks,
    additionalMustRunAfter = listOf(soloClearLabProducerTask) + phase4AggregateGameAliasProducers + phase4AggregateClientAliasProducers,
    legacyReportDir = legacyPhase4ReportDir,
)

registerPhase4AggregateTask(
    name = "phase4Report",
    descriptionText = "Rebuilds the canonical unified Phase 4 aggregate report from existing domain summaries and cached evaluation artifacts.",
    includeTag = "reportPhase4",
    outputDir = unifiedPhase4ReportDir,
    outputArtifacts = unifiedPhase4ReportOutputs,
    producerInputs = phase4AggregateProducerInputs,
    producerTasks = phase4AggregateProducerTasks,
    additionalMustRunAfter = listOf(soloClearLabProducerTask) + phase4AggregateGameAliasProducers + phase4AggregateClientAliasProducers,
    aggregateReportDir = unifiedPhase4ReportDir,
    compareLegacy = false,
)

registerPhase4AggregateTask(
    name = "phase4ReportOnly",
    descriptionText = "Explicit artifact-only alias for rebuilding the canonical unified Phase 4 aggregate report.",
    includeTag = "reportPhase4",
    outputDir = unifiedPhase4ReportDir,
    outputArtifacts = unifiedPhase4ReportOutputs,
    producerInputs = phase4AggregateProducerInputs,
    producerTasks = phase4AggregateProducerTasks,
    additionalMustRunAfter = listOf(soloClearLabProducerTask) + phase4AggregateGameAliasProducers + phase4AggregateClientAliasProducers,
    aggregateReportDir = unifiedPhase4ReportDir,
    compareLegacy = false,
)

registerPhase4AggregateTask(
    name = "reportPhase4",
    descriptionText = "Runs the explicit parity gate that compares the canonical unified Phase 4 aggregate against the legacy fallback report.",
    includeTag = "reportPhase4",
    outputDir = unifiedPhase4ReportDir,
    outputArtifacts = unifiedPhase4ParityOutputs,
    producerInputs = phase4AggregateProducerInputs,
    producerTasks = phase4AggregateProducerTasks,
    additionalDependsOn = listOf(tasks.named("phase4LegacyReport")),
    additionalMustRunAfter = listOf(
        tasks.named("phase4LegacyReportOnly"),
        soloClearLabProducerTask,
    ) + phase4AggregateGameAliasProducers + phase4AggregateClientAliasProducers,
    additionalInputs = files(legacyPhase4SummaryInput),
    aggregateReportDir = unifiedPhase4ReportDir,
    legacyReportDir = legacyPhase4ReportDir,
    compareLegacy = true,
)

registerPhase4AggregateTask(
    name = "reportPhase4Only",
    descriptionText = "Rebuilds the canonical unified Phase 4 aggregate report from existing domain summaries without legacy comparison or producer reruns.",
    includeTag = "reportPhase4",
    outputDir = unifiedPhase4ReportDir,
    outputArtifacts = unifiedPhase4ReportOutputs,
    producerInputs = phase4AggregateProducerInputs,
    producerTasks = phase4AggregateProducerTasks,
    additionalMustRunAfter = listOf(soloClearLabProducerTask) + phase4AggregateGameAliasProducers + phase4AggregateClientAliasProducers,
    aggregateReportDir = unifiedPhase4ReportDir,
    compareLegacy = false,
)

tasks.register<Test>("reportPhase4Fixture") {
    group = "verification"
    description = "Runs the fixture-backed reportPhase4 contract tests without materializing the repo-scoped canonical report."
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnitPlatform {
        includeTags("reportPhase4Fixture")
    }
}

listOf(
    tasks.named("contractLint"),
    tasks.named("keywordRegistryLint"),
    tasks.named("darkKeyRegistryLint"),
    tasks.named("darkSpriteSheetLint"),
    tasks.named("spriteSheetMapLint"),
    tasks.named("resourcePipelineLint"),
    tasks.named("darkManifestCoveragePr00DryRun"),
    tasks.named("darkManifestCoveragePr02OwnerScope"),
    tasks.named("darkManifestCoveragePr02_1OwnerScope"),
    tasks.named("darkManifestCoveragePr02_2OwnerScope"),
    tasks.named("verifyContractLintPreflight"),
    tasks.named("verifyLootPreflight"),
    tasks.named("verifyHiddenPreflight"),
    tasks.named("verifyContentPackPreflight"),
    tasks.named("scopeCoverageLint"),
    tasks.named("maintainabilityLint"),
    tasks.named("lootBalanceLab"),
    tasks.named("whiteBoxLoot"),
    tasks.named("hiddenContentHarness"),
    tasks.named("organicHiddenProbe"),
    tasks.named("contentPackHarness"),
    tasks.named("whiteBoxContentPack"),
    tasks.named("reportPhase4Only"),
    tasks.named("terrainInteractionBatch"),
    tasks.named("bossHarness"),
    tasks.named("whiteBoxMapgen"),
    tasks.named("whiteBoxSolvability"),
    tasks.named("mapgenSmoke"),
    tasks.named("solvabilityHarness"),
).forEach { taskProvider ->
    taskProvider.configure {
        VerifyChangedPlanGate.applyTo(this, verifyChangedTaskPathsFile, verifyChangedPreflightTaskPathsFile, "prepareVerifyChangedPlan")
    }
}
