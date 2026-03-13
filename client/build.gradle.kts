import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named

plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":game"))
    implementation("com.badlogicgames.gdx:gdx:${rootProject.providers.gradleProperty("libgdxVersion").get()}")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${rootProject.providers.gradleProperty("libgdxVersion").get()}")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:${rootProject.providers.gradleProperty("libgdxVersion").get()}:natives-desktop")
}

application {
    mainClass.set("com.ktome.client.DesktopLauncherKt")
}

tasks.named<JavaExec>("run") {
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.named<Test>("test") {
    enabled = true
}
