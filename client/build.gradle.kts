import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.named
import org.gradle.jvm.application.tasks.CreateStartScripts

plugins {
    application
    id("org.jetbrains.kotlin.plugin.serialization")
}

val harnessReportDir = rootProject.layout.buildDirectory.dir("reports/harness")

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
    mainClass.set("com.ktome.client.DesktopLauncherKt")
}

distributions {
    main {
        contents {
            from(rootProject.file("README.md")) {
                into("docs")
            }
            from(rootProject.file("docs/phase1/2026-03-12-phase1-5.0-regression-checklist.md")) {
                into("docs")
            }
            from(rootProject.file("docs/releases/v0.1.0-pre-release-acceptance.md")) {
                into("docs")
            }
            from(rootProject.file("docs/releases/v0.1.0-known-limitations.md")) {
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

tasks.named<Test>("test") {
    enabled = true
}

tasks.named("processResources") {
    dependsOn(rootProject.tasks.named("syncPhase2Manifests"))
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
