import java.io.File

pluginManagement {
    val bootstrapRepo = File(settingsDir, ".bootstrap/m2")

    repositories {
        if (bootstrapRepo.isDirectory) {
            maven(url = bootstrapRepo.toURI())
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "K-ToME"

include("core", "game", "client")
