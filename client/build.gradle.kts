import com.ktome.build.verification.VerifyChangedPlanGate
import org.gradle.api.GradleException
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.named
import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

plugins {
    application
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.ktome.build.verification")
}

val harnessReportDir = rootProject.layout.buildDirectory.dir("reports/harness")
val verifyChangedTaskPathsFile = rootProject.layout.buildDirectory.file("verification/verify-changed/task-paths.txt")
val verifyChangedPreflightTaskPathsFile = rootProject.layout.buildDirectory.file("verification/verify-changed/preflight-task-paths.txt")
val desktopMainClass = "com.ktome.client.DesktopLauncherKt"
val desktopAppVersion = project.version.toString()
val macAppName = "K-ToME"
val macAppBundleIdentifier = "com.ktome.client"
val validationSamplePackDir = rootProject.file("examples/content-packs/sample.flooded_relics")
val macPackagingWorkspaceDir = layout.buildDirectory.dir("jpackage")
val macPackagingInputDir = macPackagingWorkspaceDir.map { it.dir("input") }
val macPackagingIconDir = macPackagingWorkspaceDir.map { it.dir("icon") }
val macPackagingIconSource = layout.projectDirectory.file("src/packaging/macos/K-ToME-app-icon.png")
val macPackagingIcnsFile = macPackagingIconDir.map { it.file("$macAppName.icns") }
val macAppOutputDir = layout.buildDirectory.dir("release")
val macAppImageDir = layout.buildDirectory.dir("release/$macAppName.app")
val isMacOs = System.getProperty("os.name").contains("Mac", ignoreCase = true)

data class GitShortHash(
    val value: String,
    val resolved: Boolean,
)

val gitShortHashPattern = Regex("[0-9a-f]{7,40}")

fun resolveGitShortHash(): GitShortHash =
    try {
        val output =
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim()
        if (output.matches(gitShortHashPattern)) {
            GitShortHash(value = output, resolved = true)
        } else {
            GitShortHash(value = "unknown", resolved = false)
        }
    } catch (_: Exception) {
        GitShortHash(value = "unknown", resolved = false)
    }

val gitShortHash = providers.provider { resolveGitShortHash() }
val buildInfoShortHash = gitShortHash.map { hash -> hash.value }
val buildInfoShortHashResolved = gitShortHash.map { hash -> hash.resolved.toString() }

fun requireMacPackagingEnvironment(taskName: String, requireIconTools: Boolean = false): File {
    if (!isMacOs) {
        throw GradleException("$taskName is macOS-only.")
    }

    val jpackageExecutable = File(System.getProperty("java.home"), "bin/jpackage")
    if (!jpackageExecutable.isFile) {
        throw GradleException("$taskName requires jpackage from the SDKMAN-managed JDK 21 environment.")
    }

    if (requireIconTools) {
        listOf("/usr/bin/sips", "/usr/bin/iconutil").forEach { toolPath ->
            if (!File(toolPath).isFile) {
                throw GradleException("$taskName requires $toolPath to be available on macOS.")
            }
        }
    }

    return jpackageExecutable
}

fun toJpackageAppVersion(version: String): String {
    val components =
        version.split('.').map { fragment ->
            fragment.toIntOrNull()
                ?: throw GradleException("packageMacApp requires a numeric project.version, but found '$version'.")
        }.toMutableList()

    while (components.size < 3) {
        components += 0
    }

    if (components.first() <= 0) {
        components[0] = 1
    }

    return components.take(3).joinToString(".")
}

val macAppPackageVersion = toJpackageAppVersion(desktopAppVersion)
val desktopRuntimeClasspath = configurations.runtimeClasspath

dependencies {
    implementation(project(":core"))
    implementation(project(":game"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
    implementation("com.badlogicgames.gdx:gdx:${rootProject.providers.gradleProperty("libgdxVersion").get()}")
    implementation("com.badlogicgames.gdx:gdx-freetype:${rootProject.providers.gradleProperty("libgdxVersion").get()}")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${rootProject.providers.gradleProperty("libgdxVersion").get()}")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:${rootProject.providers.gradleProperty("libgdxVersion").get()}:natives-desktop")
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:${rootProject.providers.gradleProperty("libgdxVersion").get()}:natives-desktop")
    testImplementation("com.badlogicgames.gdx:gdx-backend-headless:${rootProject.providers.gradleProperty("libgdxVersion").get()}")
}

application {
    mainClass.set(desktopMainClass)
}

distributions {
    main {
        contents {
            from(validationSamplePackDir) {
                into("content-packs/sample.flooded_relics")
            }
            from(rootProject.file("README.md")) {
                into("docs")
            }
            from(rootProject.file("docs/phase1/2026-03-12-phase1-5.0-regression-checklist.md")) {
                into("docs")
            }
            from(rootProject.file("docs/releases/v0.4.0-pre-release-acceptance.md")) {
                into("docs")
            }
            from(rootProject.file("docs/releases/v0.4.0-known-limitations.md")) {
                into("docs")
            }
        }
    }
}

tasks.named<JavaExec>("run") {
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        val darwinAwareJvmOpts =
            """
            if [ "${'$'}darwin" = true ] ; then
                DEFAULT_JVM_OPTS='"-XstartOnFirstThread"'
            else
                DEFAULT_JVM_OPTS=""
            fi
            """.trimIndent()

        unixScript.writeText(
            unixScript.readText().replace(
                """DEFAULT_JVM_OPTS=""",
                darwinAwareJvmOpts,
            ),
        )
    }
}

tasks.register<Copy>("releaseDesktopDist") {
    group = "distribution"
    description = "Copies the packaged desktop distribution zip into build/release."
    val distZip = tasks.named<Zip>("distZip")
    dependsOn(distZip)
    from(distZip.flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("release"))
    rename { "ktome-v${project.version}-desktop.zip" }
}

val desktopJar = tasks.named<Jar>("jar")

val prepareMacAppInput =
    tasks.register<Sync>("prepareMacAppInput") {
    group = "distribution"
    description = "Stages the desktop runtime jars for jpackage."
    dependsOn(desktopJar)
    dependsOn(desktopRuntimeClasspath)
    into(macPackagingInputDir)
    from(desktopJar.flatMap { it.archiveFile })
    from(desktopRuntimeClasspath) {
        include("*.jar")
    }
    from(validationSamplePackDir) {
        into("content-packs/sample.flooded_relics")
    }
    doFirst {
        requireMacPackagingEnvironment(name)
    }
}

val prepareMacAppIcon =
    tasks.register("prepareMacAppIcon") {
    group = "distribution"
    description = "Builds a macOS .icns icon for the desktop app image."
    inputs.file(macPackagingIconSource)
    outputs.file(macPackagingIcnsFile)

    doLast {
        requireMacPackagingEnvironment(name, requireIconTools = true)

        val sourceIcon = macPackagingIconSource.asFile
        if (!sourceIcon.isFile) {
            throw GradleException("Missing macOS packaging icon source: ${sourceIcon.absolutePath}")
        }

        val iconRoot = macPackagingIconDir.get().asFile
        val iconsetDir = iconRoot.resolve("$macAppName.iconset")
        val icnsFile = macPackagingIcnsFile.get().asFile
        val iconVariants =
            listOf(
                "icon_16x16.png" to 16,
                "icon_16x16@2x.png" to 32,
                "icon_32x32.png" to 32,
                "icon_32x32@2x.png" to 64,
                "icon_128x128.png" to 128,
                "icon_128x128@2x.png" to 256,
                "icon_256x256.png" to 256,
                "icon_256x256@2x.png" to 512,
                "icon_512x512.png" to 512,
                "icon_512x512@2x.png" to 1024,
            )

        delete(iconsetDir)
        iconRoot.mkdirs()
        iconsetDir.mkdirs()

        iconVariants.forEach { (fileName, size) ->
            providers.exec {
                commandLine(
                    "/usr/bin/sips",
                    "-z",
                    size.toString(),
                    size.toString(),
                    sourceIcon.absolutePath,
                    "--out",
                    iconsetDir.resolve(fileName).absolutePath,
                )
            }.result.get()
        }

        delete(icnsFile)
        providers.exec {
            commandLine(
                "/usr/bin/iconutil",
                "-c",
                "icns",
                iconsetDir.absolutePath,
                "-o",
                icnsFile.absolutePath,
            )
        }.result.get()
    }
}

tasks.register("packageMacApp") {
    group = "distribution"
    description = "Packages a macOS .app image under build/release using jpackage."
    dependsOn(prepareMacAppInput, prepareMacAppIcon)
    inputs.files(files(macPackagingInputDir).builtBy(prepareMacAppInput))
    inputs.files(files(macPackagingIcnsFile).builtBy(prepareMacAppIcon))
    outputs.dir(macAppImageDir)

    doLast {
        val jpackageExecutable = requireMacPackagingEnvironment(name)
        val outputDir = macAppOutputDir.get().asFile
        val appImageDir = macAppImageDir.get().asFile

        delete(appImageDir)
        outputDir.mkdirs()

        providers.exec {
            commandLine(
                jpackageExecutable.absolutePath,
                "--type",
                "app-image",
                "--dest",
                outputDir.absolutePath,
                "--input",
                macPackagingInputDir.get().asFile.absolutePath,
                "--name",
                macAppName,
                "--main-jar",
                desktopJar.get().archiveFileName.get(),
                "--main-class",
                desktopMainClass,
                "--app-version",
                macAppPackageVersion,
                "--icon",
                macPackagingIcnsFile.get().asFile.absolutePath,
                "--vendor",
                macAppName,
                "--mac-package-identifier",
                macAppBundleIdentifier,
                "--java-options",
                "-XstartOnFirstThread",
            )
        }.result.get()

        val infoPlist = appImageDir.resolve("Contents/Info.plist")
        if (!infoPlist.isFile) {
            throw GradleException("packageMacApp did not produce ${infoPlist.absolutePath}")
        }

        providers.exec {
            commandLine(
                "/usr/bin/plutil",
                "-replace",
                "CFBundleShortVersionString",
                "-string",
                desktopAppVersion,
                infoPlist.absolutePath,
            )
        }.result.get()
    }
}

tasks.named<Test>("test") {
    enabled = true
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(rootProject.tasks.named("syncPhase2Manifests"))
    inputs.property("buildInfoShortHash", buildInfoShortHash)
    inputs.property("buildInfoShortHashResolved", buildInfoShortHashResolved)
    filesMatching("build-info.properties") {
        expand(
            "shortHash" to buildInfoShortHash.get(),
            "shortHashResolved" to buildInfoShortHashResolved.get(),
        )
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.register<Test>("clientSmoke") {
    group = "verification"
    description = "Runs headless client smoke coverage."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("clientSmoke")
    }
    systemProperty("ktome.harness.reportDir", harnessReportDir.get().asFile.absolutePath)
    outputs.dir(harnessReportDir)
}

tasks.register<Test>("goldenScreenshot") {
    group = "verification"
    description = "Runs deterministic screenshot golden coverage."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("goldenScreenshot")
    }
}

listOf(
    tasks.named("clientSmoke"),
    tasks.named("goldenScreenshot"),
).forEach { taskProvider ->
    taskProvider.configure {
        VerifyChangedPlanGate.applyTo(
            this,
            verifyChangedTaskPathsFile,
            verifyChangedPreflightTaskPathsFile,
            ":tools:prepareVerifyChangedPlan",
        )
    }
}
