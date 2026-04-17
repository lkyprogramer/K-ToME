import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `java-gradle-plugin`
}

val bootstrapRepo = rootDir.resolve("../.bootstrap/m2").canonicalFile

repositories {
    if (bootstrapRepo.isDirectory) {
        maven(url = bootstrapRepo.toURI())
    }
    gradlePluginPortal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of((providers.gradleProperty("javaVersion").orNull ?: "21").toInt()))
    }
}

dependencies {
    implementation("org.yaml:snakeyaml:${providers.gradleProperty("snakeyamlVersion").orNull ?: "2.6"}")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:${providers.gradleProperty("junitVersion").orNull ?: "5.12.2"}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${providers.gradleProperty("junitPlatformVersion").orNull ?: "1.12.2"}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("ktomeVerification") {
            id = "com.ktome.build.verification"
            implementationClass = "com.ktome.build.verification.VerificationTaskPlugin"
        }
    }
}
