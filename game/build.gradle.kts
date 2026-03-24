import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
}

val harnessReportDir = rootProject.layout.buildDirectory.dir("reports/harness")

dependencies {
    api(project(":core"))
    implementation("org.yaml:snakeyaml:${rootProject.providers.gradleProperty("snakeyamlVersion").get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
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
    outputs.dir(harnessReportDir)
}
