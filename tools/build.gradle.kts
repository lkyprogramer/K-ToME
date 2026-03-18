import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
}

dependencies {
    implementation(project(":game"))
    testImplementation(project(":game"))
    testImplementation("org.yaml:snakeyaml:${rootProject.providers.gradleProperty("snakeyamlVersion").get()}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()}")
}

tasks.withType<Test>().configureEach {
    systemProperty("ktome.repo.root", rootProject.projectDir.absolutePath)
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
