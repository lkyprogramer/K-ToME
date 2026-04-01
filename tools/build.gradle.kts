import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
}

val harnessReportDir = rootProject.layout.buildDirectory.dir("reports/harness")

dependencies {
    implementation(project(":game"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
    testImplementation(project(":client"))
    testImplementation(project(":game"))
    testImplementation("org.yaml:snakeyaml:${rootProject.providers.gradleProperty("snakeyamlVersion").get()}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
}

tasks.withType<Test>().configureEach {
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
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
    description = "Runs the Phase 4 solvability harness skeleton until PR-03 lands."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("solvabilityHarness")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/solvability")
    systemProperty("ktome.phase4.solvability.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}
