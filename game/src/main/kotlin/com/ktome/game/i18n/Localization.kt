package com.ktome.game.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class GameLocale(val id: String) {
    EN_US("en-US"),
    ZH_CN("zh-CN"),
    ;

    fun cycle(): GameLocale =
        when (this) {
            EN_US -> ZH_CN
            ZH_CN -> EN_US
        }

    companion object {
        val DEFAULT: GameLocale = ZH_CN

        val FALLBACK: GameLocale = EN_US

        fun fromId(id: String): GameLocale =
            entries.firstOrNull { locale -> locale.id == id }
                ?: error("Unsupported locale id: $id")
    }
}

class LocalizationBundle private constructor(
    private val bundles: Map<GameLocale, Map<String, String>>,
) {
    fun translator(locale: GameLocale): Localizer {
        val current = requireNotNull(bundles[locale]) { "Missing bundle for locale ${locale.id}" }
        val fallback = requireNotNull(bundles[GameLocale.FALLBACK]) { "Missing fallback bundle for ${GameLocale.FALLBACK.id}" }
        return Localizer(locale = locale, current = current, fallback = fallback)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(
            resourceLoader: (String) -> String = { path ->
                LocalizationBundle::class.java.getResource(path)?.readText()
                    ?: error("Localization resource not found: $path")
            },
        ): LocalizationBundle =
            LocalizationBundle(
                bundles =
                    GameLocale.entries.associateWith { locale ->
                        parseBundle(resourceLoader("/i18n/${locale.id}.json"))
                    },
            )

        private fun parseBundle(content: String): Map<String, String> =
            json.parseToJsonElement(content).jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
    }
}

class Localizer internal constructor(
    val locale: GameLocale,
    private val current: Map<String, String>,
    private val fallback: Map<String, String>,
) {
    fun text(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String {
        val template = current[key] ?: fallback[key] ?: "!!$key!!"
        return args.fold(template) { acc, (name, value) ->
            acc.replace("{$name}", value?.toString() ?: "")
        }
    }

    fun localeLabel(): String = text("ui.locale.${locale.id}")
}
