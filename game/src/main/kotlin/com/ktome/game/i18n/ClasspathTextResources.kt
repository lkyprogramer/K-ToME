package com.ktome.game.i18n

internal object ClasspathTextResources {
    fun read(
        anchor: Class<*>,
        path: String,
    ): String {
        val normalizedPath = path.takeIf { it.startsWith("/") } ?: "/$path"
        val stream = anchor.getResourceAsStream(normalizedPath)
            ?: error("Classpath text resource not found: $normalizedPath")
        return stream.use { input -> input.readBytes().decodeToString() }
    }
}
