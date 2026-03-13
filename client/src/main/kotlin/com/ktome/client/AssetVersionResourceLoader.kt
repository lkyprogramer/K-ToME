package com.ktome.client

import com.ktome.core.save.AssetVersionContract
import java.io.InputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class AssetVersionLoadException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal object AssetVersionResourceLoader {
    private const val resourcePath: String = "/bootstrap/asset-version-contract.json"

    private val json =
        Json {
            ignoreUnknownKeys = false
        }

    fun load(
        path: String = resourcePath,
        resourceLoader: (String) -> InputStream? = { candidate -> AssetVersionResourceLoader::class.java.getResourceAsStream(candidate) },
    ): AssetVersionContract {
        val stream = resourceLoader(path) ?: throw AssetVersionLoadException("Asset version contract resource not found: $path")

        return stream.use { input ->
            try {
                json.decodeFromString<AssetVersionContract>(input.readBytes().decodeToString())
            } catch (exception: SerializationException) {
                throw AssetVersionLoadException("Asset version contract is invalid: $path", exception)
            } catch (exception: IllegalArgumentException) {
                throw AssetVersionLoadException("Asset version contract is invalid: $path", exception)
            }
        }
    }
}
