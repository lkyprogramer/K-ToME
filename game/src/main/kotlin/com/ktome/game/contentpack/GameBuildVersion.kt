package com.ktome.game.contentpack

import java.util.Properties

object GameBuildVersion {
    private const val RESOURCE_PATH: String = "/ktome-build.properties"

    fun current(): String {
        val properties = Properties()
        val stream =
            requireNotNull(GameBuildVersion::class.java.getResourceAsStream(RESOURCE_PATH)) {
                "Missing build-version resource: $RESOURCE_PATH"
            }
        stream.use(properties::load)
        return requireNotNull(properties.getProperty("version")) {
            "Missing 'version' in build-version resource."
        }.trim().also { version ->
            require(version.isNotBlank()) { "Build version must not be blank." }
        }
    }
}
