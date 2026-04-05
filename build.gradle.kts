import org.gradle.api.JavaVersion
import org.gradle.api.tasks.Exec
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
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
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    jacoco
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
val bootstrapRepo = rootProject.layout.projectDirectory.dir(".bootstrap/m2").asFile

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
                excludeTags(
                    "headlessSmoke",
                    "clientSmoke",
                    "longRunLab",
                    "soloClearLab",
                    "mapgenSmoke",
                    "solvabilityHarness",
                    "whiteBoxMapgen",
                    "whiteBoxSolvability",
                    "phase4Report",
                )
            }
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

tasks.register("combatTraceGolden") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 3 FORMULA corpus combat trace golden harness."
    dependsOn(":tools:combatTraceGolden")
}

tasks.register("bossHarness") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Phase 3 boss encounter stability harness."
    dependsOn(":game:bossHarness")
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

tasks.register("whiteBoxVerify") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all currently registered unified white-box verification pilots."
    dependsOn("whiteBoxMapgen", "whiteBoxSolvability")
}

tasks.register("phase4Report") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Aggregates currently landed Phase 4 verification tasks into tools/build/reports/phase4/phase4-summary.json."
    dependsOn(":tools:phase4Report")
}

tasks.register<Exec>("assetLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates the primary image asset plan plus PR-09 Gemini additions."
    commandLine(
        "python3",
        "scripts/asset-lint.py",
        "--plan",
        "assets-src/image/specs/phase2-asset-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-pr09-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml",
        "--report-dir",
        "assets-src/image/manifests",
    )
}

tasks.register<Exec>("styleLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates the primary art style contract bindings plus PR-09 Gemini additions."
    commandLine(
        "python3",
        "scripts/style-lint.py",
        "--plan",
        "assets-src/image/specs/phase2-asset-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-pr09-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml",
        "--extra-plan",
        "assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml",
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
}
