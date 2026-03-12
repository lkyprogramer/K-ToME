import java.math.BigDecimal
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    `java-library`
}

tasks.register("verifyNoGdxDependencies") {
    group = "verification"
    description = "Fails if the core module accidentally pulls any libGDX dependency."

    doLast {
        val forbiddenArtifacts = listOf("compileClasspath", "runtimeClasspath")
            .flatMap { configurationName ->
                configurations.getByName(configurationName)
                    .resolvedConfiguration
                    .resolvedArtifacts
                    .filter { it.moduleVersion.id.group == "com.badlogicgames.gdx" }
                    .map { artifact ->
                        "$configurationName -> ${artifact.moduleVersion.id.group}:${artifact.name}:${artifact.moduleVersion.id.version}"
                    }
            }

        check(forbiddenArtifacts.isEmpty()) {
            "core must remain engine-free. Forbidden libGDX dependencies found:\n${forbiddenArtifacts.joinToString("\n")}"
        }
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named<Test>("test"))

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.70")
            }
        }

        listOf(
            "com.ktome.core.ecs.*",
            "com.ktome.core.map.*",
            "com.ktome.core.fov.*",
            "com.ktome.core.turn.*",
            "com.ktome.core.combat.*",
            "com.ktome.core.pathfinding.*",
            "com.ktome.core.ai.*",
        ).forEach { packagePattern ->
            rule {
                element = "PACKAGE"
                includes = listOf(packagePattern)

                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = BigDecimal("0.85")
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn("verifyNoGdxDependencies")
    dependsOn("jacocoTestCoverageVerification")
}
