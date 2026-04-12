import java.io.File

pluginManagement {
    val bootstrapRepo = File(settingsDir, "../.bootstrap/m2").canonicalFile

    repositories {
        if (bootstrapRepo.isDirectory) {
            maven(url = bootstrapRepo.toURI())
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ktome-build-logic"
