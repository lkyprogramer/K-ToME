plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    implementation("org.yaml:snakeyaml:${rootProject.providers.gradleProperty("snakeyamlVersion").get()}")
}
