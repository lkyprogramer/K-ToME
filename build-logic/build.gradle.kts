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

gradlePlugin {
    plugins {
        register("ktomeVerification") {
            id = "com.ktome.build.verification"
            implementationClass = "com.ktome.build.verification.VerificationTaskPlugin"
        }
    }
}
