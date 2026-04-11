import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `java-test-fixtures`
}

val harnessReportDir = rootProject.layout.buildDirectory.dir("reports/harness")

dependencies {
    api(project(":core"))
    implementation("org.yaml:snakeyaml:${rootProject.providers.gradleProperty("snakeyamlVersion").get()}")
    testFixturesImplementation("org.yaml:snakeyaml:${rootProject.providers.gradleProperty("snakeyamlVersion").get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
}

tasks.withType<Test>().configureEach {
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("ktome-build.properties") {
        expand("version" to project.version.toString())
    }
}

tasks.register<Test>("headlessSmoke") {
    group = "verification"
    description = "Runs headless AI smoke scenarios."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("headlessSmoke")
    }
    systemProperty("ktome.harness.reportDir", harnessReportDir.get().asFile.absolutePath)
    outputs.dir(harnessReportDir)
}

tasks.register<Test>("soloClearLab") {
    group = "verification"
    description = "Runs the fixed-seed solo clear acceptance lab."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("soloClearLab")
    }
    systemProperty("ktome.harness.reportDir", harnessReportDir.get().asFile.absolutePath)
    outputs.dir(harnessReportDir)
}

tasks.register<Test>("longRunLab") {
    group = "verification"
    description = "Runs nightly long-run AI matrix."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("longRunLab")
    }
    systemProperty("ktome.harness.reportDir", harnessReportDir.get().asFile.absolutePath)
    outputs.dir(harnessReportDir)
}

tasks.register<Test>("bossHarness") {
    group = "verification"
    description = "Runs boss encounter stability and warning harness coverage."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("bossHarness")
    }
    systemProperty("ktome.harness.reportDir", harnessReportDir.get().asFile.absolutePath)
    systemProperty(
        "ktome.phase4.whitebox.boss.reportDir",
        rootProject.layout.projectDirectory.dir("tools/build/reports/phase4/whitebox/boss").asFile.absolutePath,
    )
    outputs.dir(harnessReportDir)
    outputs.dir(rootProject.layout.projectDirectory.dir("tools/build/reports/phase4/whitebox/boss"))
}

tasks.register<Test>("terrainInteractionBatch") {
    group = "verification"
    description = "Runs the PR-06 terrain interaction batch and writes unified white-box reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("terrainInteractionBatch")
    }
    systemProperty(
        "ktome.phase4.whitebox.terrain.reportDir",
        rootProject.layout.projectDirectory.dir("tools/build/reports/phase4/whitebox/terrain").asFile.absolutePath,
    )
    outputs.dir(rootProject.layout.projectDirectory.dir("tools/build/reports/phase4/whitebox/terrain"))
}
