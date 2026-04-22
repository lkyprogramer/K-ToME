package com.ktome.client.build

import java.util.Properties

internal data class BuildInfoSnapshot(
    val shortHash: String,
    val hashResolved: Boolean,
    val fallbackReason: String? = null,
)

internal object BuildInfo {
    private const val RESOURCE_PATH: String = "/build-info.properties"
    private const val FALLBACK_SHORT_HASH: String = "unknown"
    private val shortHashPattern = Regex("[0-9a-f]{7,40}")

    @Volatile
    private var cachedSnapshot: BuildInfoSnapshot? = null

    val shortHash: String
        get() = initialize().shortHash

    fun initialize(): BuildInfoSnapshot =
        cachedSnapshot ?: synchronized(this) {
            cachedSnapshot ?: resolve(::loadProperties).also { snapshot ->
                cachedSnapshot = snapshot
                warnIfFallback(snapshot, ::defaultWarningSink)
            }
        }

    internal fun initializeForTest(
        properties: Properties?,
        warningSink: (String) -> Unit,
    ): BuildInfoSnapshot =
        synchronized(this) {
            cachedSnapshot = null
            resolve { properties }.also { snapshot ->
                cachedSnapshot = snapshot
                warnIfFallback(snapshot, warningSink)
            }
        }

    internal fun resetForTest() {
        synchronized(this) {
            cachedSnapshot = null
        }
    }

    private fun resolve(propertiesLoader: () -> Properties?): BuildInfoSnapshot {
        val properties =
            try {
                propertiesLoader()
            } catch (exception: RuntimeException) {
                return fallback("failed to read $RESOURCE_PATH: ${exception.message ?: exception::class.java.name}")
            }
        if (properties == null) {
            return fallback("missing resource $RESOURCE_PATH")
        }
        val rawShortHash = properties.getProperty("shortHash")?.trim().orEmpty()
        val rawResolved = properties.getProperty("shortHashResolved")?.trim().orEmpty()
        if (!rawResolved.equals("true", ignoreCase = true)) {
            return fallback("build hash was not resolved when resources were processed")
        }
        if (!shortHashPattern.matches(rawShortHash)) {
            return fallback("invalid build hash '$rawShortHash'")
        }
        return BuildInfoSnapshot(
            shortHash = rawShortHash,
            hashResolved = true,
        )
    }

    private fun fallback(reason: String): BuildInfoSnapshot =
        BuildInfoSnapshot(
            shortHash = FALLBACK_SHORT_HASH,
            hashResolved = false,
            fallbackReason = reason,
        )

    private fun loadProperties(): Properties? {
        val stream = BuildInfo::class.java.getResourceAsStream(RESOURCE_PATH) ?: return null
        return stream.use { input ->
            Properties().also { properties -> properties.load(input) }
        }
    }

    private fun warnIfFallback(
        snapshot: BuildInfoSnapshot,
        warningSink: (String) -> Unit,
    ) {
        val reason = snapshot.fallbackReason ?: return
        warningSink("[ktome/build-info] WARN BuildInfo.shortHash resolution failed, fell back to '${snapshot.shortHash}': $reason")
    }

    private fun defaultWarningSink(message: String) {
        System.err.println(message)
    }
}
