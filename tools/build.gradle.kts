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
    providers.systemProperty("ktome.updateMapgenGolden").orNull?.let { value ->
        systemProperty("ktome.updateMapgenGolden", value)
    }
    providers.systemProperty("ktome.updateSolvabilityGolden").orNull?.let { value ->
        systemProperty("ktome.updateSolvabilityGolden", value)
    }
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

tasks.register<Test>("hiddenContentHarness") {
    group = "verification"
    description = "Runs the Phase 4 hidden-content harness and writes structured reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("hiddenContentHarness")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/hidden")
    systemProperty("ktome.phase4.hidden.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}

tasks.register<Test>("whiteBoxMapgen") {
    group = "verification"
    description = "Runs the Phase 4 unified white-box mapgen pilot and writes standard reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("whiteBoxMapgen")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/whitebox/mapgen")
    systemProperty("ktome.phase4.whitebox.mapgen.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}

tasks.register<Test>("whiteBoxSolvability") {
    group = "verification"
    description = "Runs the Phase 4 unified white-box solvability pilot and writes standard reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("whiteBoxSolvability")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/whitebox/solvability")
    systemProperty("ktome.phase4.whitebox.solvability.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
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
}

tasks.register<Test>("whiteBoxLoot") {
    group = "verification"
    description = "Runs the Phase 4 unified white-box loot pilot and writes standard reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("whiteBoxLoot")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/whitebox/loot")
    systemProperty("ktome.phase4.whitebox.loot.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}

tasks.register<Test>("whiteBoxHiddenContent") {
    group = "verification"
    description = "Runs the Phase 4 unified white-box hidden-content domain and writes standard reports."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("whiteBoxHiddenContent")
    }
    val reportDir = layout.buildDirectory.dir("reports/phase4/whitebox/hidden")
    systemProperty("ktome.phase4.hidden.reportDir", layout.buildDirectory.dir("reports/phase4/hidden").get().asFile.absolutePath)
    outputs.dir(reportDir)
}

tasks.register<Test>("phase4Report") {
    group = "verification"
    description = "Aggregates the currently landed Phase 4 verification reports into a single phase summary."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("phase4Report")
    }
    dependsOn(
        ":tools:mapgenSmoke",
        ":tools:solvabilityHarness",
        ":tools:hiddenContentHarness",
        ":tools:lootBalanceLab",
        ":tools:whiteBoxMapgen",
        ":tools:whiteBoxSolvability",
        ":tools:whiteBoxLoot",
        ":tools:whiteBoxHiddenContent",
        ":game:terrainInteractionBatch",
        ":game:bossHarness",
    )
    val reportDir = layout.buildDirectory.dir("reports/phase4")
    systemProperty("ktome.phase4.reportDir", reportDir.get().asFile.absolutePath)
    outputs.dir(reportDir)
}
