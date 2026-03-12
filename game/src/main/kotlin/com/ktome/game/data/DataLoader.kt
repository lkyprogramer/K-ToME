package com.ktome.game.data

import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.Stats
import com.ktome.game.model.MonsterCatalog
import com.ktome.game.model.MonsterTemplate
import java.io.InputStream
import org.yaml.snakeyaml.Yaml

class DataLoader {
    fun loadMonsterCatalog(resourcePath: String = "/data/monsters.yaml"): MonsterCatalog {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: error("Monster catalog resource not found: $resourcePath")
        return stream.use(::loadMonsterCatalog)
    }

    fun loadMonsterCatalog(input: InputStream): MonsterCatalog {
        val yaml = Yaml()
        val root = yaml.load<Map<String, Any?>>(input)
        val monsterEntries = root["monsters"] as? List<*> ?: error("monsters.yaml must contain a top-level 'monsters' list")

        return MonsterCatalog(
            monsters = monsterEntries.map { entry ->
                parseMonster(entry as? Map<*, *> ?: error("Monster entries must be maps"))
            },
        )
    }

    private fun parseMonster(entry: Map<*, *>): MonsterTemplate {
        val statsMap = entry.requiredMap("stats")
        return MonsterTemplate(
            id = entry.requiredString("id"),
            name = entry.requiredString("name"),
            glyph = entry.requiredString("glyph").single(),
            colorHex = entry.requiredString("color"),
            stats = Stats(
                str = statsMap.requiredInt("str"),
                dex = statsMap.requiredInt("dex"),
                con = statsMap.requiredInt("con"),
                wil = statsMap.requiredInt("wil"),
            ),
            baseHp = entry.requiredInt("baseHp"),
            baseAttack = entry.requiredInt("baseAttack"),
            baseDefense = entry.requiredInt("baseDefense"),
            speed = entry.requiredInt("speed"),
            ai = AIType.valueOf(entry.requiredString("ai")),
            expReward = entry.requiredInt("expReward"),
            spawnFloors = entry.requiredIntList("spawnFloors"),
            spawnWeight = entry.requiredInt("spawnWeight"),
        )
    }

    private fun Map<*, *>.requiredMap(key: String): Map<*, *> =
        this[key] as? Map<*, *> ?: error("Missing map entry '$key'")

    private fun Map<*, *>.requiredString(key: String): String =
        this[key]?.toString() ?: error("Missing string entry '$key'")

    private fun Map<*, *>.requiredInt(key: String): Int =
        when (val value = this[key]) {
            is Int -> value
            is Long -> value.toInt()
            is Number -> value.toInt()
            is String -> value.toInt()
            else -> error("Missing integer entry '$key'")
        }

    private fun Map<*, *>.requiredIntList(key: String): List<Int> =
        (this[key] as? List<*>)?.map { value ->
            when (value) {
                is Int -> value
                is Long -> value.toInt()
                is Number -> value.toInt()
                is String -> value.toInt()
                else -> error("List '$key' must contain only integers")
            }
        } ?: error("Missing integer list '$key'")
}
