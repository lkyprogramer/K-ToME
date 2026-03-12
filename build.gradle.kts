import org.gradle.api.JavaVersion
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

tasks.register<JacocoReport>("jacocoTestReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates an aggregate JaCoCo coverage report for all subprojects."
    dependsOn(test)
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
    dependsOn("jacocoTestReport")
    dependsOn(":core:jacocoTestCoverageVerification")
}
