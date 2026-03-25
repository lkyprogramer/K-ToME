package com.ktome.tools.lint

import com.ktome.core.talent.DynamicDescriptionResolver
import com.ktome.core.talent.KeywordRegistry
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.UiGlyphCatalog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

internal object LintFixtures {
    private val json = Json { ignoreUnknownKeys = true }
    private val yaml = Yaml()
    private val repoRoot: Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.repo.root")) {
                "ktome.repo.root system property is required for lint tests."
            },
        )
    private val localeKeyCallPattern =
        """(?:\btr|(?:\b[A-Za-z_][A-Za-z0-9_.]*\.)?text|MenuEntry|RenderTextTokenSnapshot)\s*\(\s*(?:[A-Za-z_][A-Za-z0-9_]*\s*=\s*)?"((?:ui|log|tile|actor|stairs|status|ai|profession|race|inscription|talent_tree|talent|monster|boss|zone|difficulty|material|affix|item|interactable|objective)\.[A-Za-z0-9_.-]+)""""
            .toRegex()
    private val directLocaleLiteralPattern =
        """"((?:ui|log|stairs|status|ai|damage_type)\.[A-Za-z0-9_.-]+|(?:actor|profession|race|inscription|talent_tree|talent|monster|boss|zone|difficulty|material|affix|interactable)\.[A-Za-z0-9_.-]+\.(?:name|desc|role|resource_hint)|objective\.[A-Za-z0-9_.-]+\.(?:name|desc|role)|objective\.[A-Za-z0-9_.-]+\.step\.[A-Za-z0-9_.-]+|item\.[A-Za-z0-9_.-]+\.(?:name|desc|role)|item\.(?:quality|display)\.[A-Za-z0-9_.-]+|monster\.tag\.[A-Za-z0-9_.-]+)""""
            .toRegex()
    private val keywordMarkupPattern = Regex("\\[\\[([a-z0-9_]+)]]")

    val schemaResources: List<String> =
        listOf(
            "/data/professions/index.yaml",
            "/data/races/index.yaml",
            "/data/inscriptions/index.yaml",
            "/data/statuses/index.yaml",
            "/data/talents/index.yaml",
            "/data/monsters/index.yaml",
            "/data/bosses/index.yaml",
            "/data/telegraph/index.yaml",
            "/data/telegraph/threat_profiles/index.yaml",
            "/data/zones/index.yaml",
            "/data/interactables/index.yaml",
            "/data/objectives/index.yaml",
            "/data/difficulties/index.yaml",
            "/data/items/index.yaml",
            "/data/loot/index.yaml",
            "/data/tilesets/index.yaml",
            "/data/ai/index.yaml",
            "/data/arenas/index.yaml",
            "/data/ambient/index.yaml",
            "/data/visuals/index.yaml",
            "/data/audio/index.yaml",
        )

    fun loadLocale(locale: GameLocale): Map<String, String> =
        json.parseToJsonElement(loadText("/i18n/${locale.id}.json")).jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }

    fun loadYaml(resource: String): Map<String, Any?> =
        yaml.load(loadText(resource)) ?: error("YAML root must not be null for $resource")

    fun schemaReferencedKeys(): Set<String> =
        schemaResources.flatMapTo(linkedSetOf()) { resource ->
            extractFieldValues(loadYaml(resource), "nameKey") +
                extractFieldValues(loadYaml(resource), "descKey") +
                extractFieldValues(loadYaml(resource), "postMessageKey") +
                extractFieldValues(loadYaml(resource), "messageKey")
        } +
            extractFieldValues(loadYaml("/data/professions/index.yaml"), "id")
                .mapTo(linkedSetOf()) { professionId -> "profession.$professionId.resource_hint" }

    fun codeReferencedLocaleKeys(): Set<String> {
        val files =
            listOf(
                repoRoot.resolve("core/src/main/kotlin"),
                repoRoot.resolve("client/src/main/kotlin"),
                repoRoot.resolve("game/src/main/kotlin"),
            ).flatMap { root ->
                if (Files.exists(root)) {
                    Files.walk(root).use { paths ->
                        paths.filter { path -> path.isRegularFile() && path.extension == "kt" }.toList()
                    }
                } else {
                    emptyList()
                }
            }

        return buildSet {
            files.forEach { path ->
                val content = path.readText()
                localeKeyCallPattern.findAll(content).forEach { match ->
                    val key = match.groupValues[1]
                    if ((!key.startsWith("ai.") || key.count { char -> char == '.' } == 1) && !key.endsWith('.')) {
                        add(key)
                    }
                }
                directLocaleLiteralPattern.findAll(content).forEach { match ->
                    val key = match.groupValues[1]
                    if ((!key.startsWith("ai.") || key.count { char -> char == '.' } == 1) && !key.endsWith('.')) {
                        add(key)
                    }
                }
            }
            GameLocale.entries.forEach { locale -> add("ui.locale.${locale.id}") }
            KeywordRegistry.CORE.all().forEach { keyword ->
                add(keyword.nameKey)
                add(keyword.tooltipKey)
            }
            addAll(DynamicDescriptionResolver.BREAKPOINT_TEMPLATE_KEYS)
        }
    }

    fun schemaFieldValues(field: String): Set<String> =
        schemaResources.flatMapTo(linkedSetOf()) { resource -> extractFieldValues(loadYaml(resource), field) }

    fun localeKeywordMarkupIds(locale: GameLocale): Set<String> =
        loadLocale(locale)
            .values
            .flatMapTo(linkedSetOf()) { value ->
                keywordMarkupPattern.findAll(value).map { match -> match.groupValues[1] }.toList()
            }

    fun formalObjectMaps(): List<Map<*, *>> =
        schemaResources.flatMap { resource -> extractFormalObjects(loadYaml(resource)) }

    fun requiredUiGlyphs(): Set<Char> = UiGlyphCatalog.requiredGlyphs(::loadText)

    fun bundledUiFontPath(): Path = repoRoot.resolve("client/src/main/resources/fonts/source-han-sans-sc-regular.otf")

    fun bundledUiFontNoticePath(): Path = repoRoot.resolve("client/src/main/resources/fonts/OFL.txt")

    fun legacyUiGlyphCatalogPath(): Path = repoRoot.resolve("client/src/main/resources/fonts/ktome-ui-glyphs.txt")

    private fun loadText(resource: String): String =
        requireNotNull(LintFixtures::class.java.getResource(resource)) { "Missing resource $resource" }.readText()

    private fun extractFieldValues(
        value: Any?,
        field: String,
    ): Set<String> =
        when (value) {
            is Map<*, *> -> {
                buildSet {
                    value[field]?.toString()?.takeIf(String::isNotBlank)?.let(::add)
                    value.values.forEach { nested -> addAll(extractFieldValues(nested, field)) }
                }
            }

            is List<*> -> value.flatMapTo(linkedSetOf()) { element -> extractFieldValues(element, field) }
            else -> emptySet()
        }

    private fun extractFormalObjects(value: Any?): List<Map<*, *>> =
        when (value) {
            is Map<*, *> -> {
                val nested = value.values.flatMap(::extractFormalObjects)
                if (value.containsKey("schemaVersion")) {
                    listOf(value) + nested
                } else {
                    nested
                }
            }

            is List<*> -> value.flatMap(::extractFormalObjects)
            else -> emptyList()
        }
}
