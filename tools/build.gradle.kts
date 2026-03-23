import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
}

val harnessReportDir = rootProject.layout.buildDirectory.dir("reports/harness")

dependencies {
    implementation(project(":game"))
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
