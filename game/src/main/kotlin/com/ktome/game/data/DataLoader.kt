package com.ktome.game.data

import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.Stats
import com.ktome.core.item.AffixDef
import com.ktome.core.item.AffixType
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemType
import com.ktome.core.item.MaterialDef
import com.ktome.core.item.StatModifier
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentLevelEffect
import com.ktome.game.model.MonsterCatalog
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import java.io.InputStream
import org.yaml.snakeyaml.Yaml

class DataLoader {
    fun loadMonsterCatalog(resourcePath: String = "/data/monsters.yaml"): MonsterCatalog {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: error("Monster catalog resource not found: $resourcePath")
        return stream.use(::loadMonsterCatalog)
    }

    fun loadItemBundle(resourcePath: String = "/data/items.yaml"): ItemDataBundle {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: error("Item bundle resource not found: $resourcePath")
        return stream.use(::loadItemBundle)
    }

    fun loadTalentDefinitions(resourcePath: String = "/data/talents.yaml"): List<TalentDef> {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: error("Talent definition resource not found: $resourcePath")
        return stream.use(::loadTalentDefinitions)
    }

    fun loadBossDefinition(resourcePath: String = "/data/boss.yaml"): BossDefinition {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: error("Boss definition resource not found: $resourcePath")
        return stream.use(::loadBossDefinition)
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

    fun loadItemBundle(input: InputStream): ItemDataBundle {
        val yaml = Yaml()
        val root = yaml.load<Map<String, Any?>>(input)

        val materials =
            (root["materials"] as? List<*>).orEmpty().map { entry ->
                parseMaterial(entry as? Map<*, *> ?: error("Material entries must be maps"))
            }
        val affixes =
            (root["affixes"] as? List<*>).orEmpty().map { entry ->
                parseAffix(entry as? Map<*, *> ?: error("Affix entries must be maps"))
            }
        val baseItems =
            listOf(
                parseItemSection(root, "weapons", ItemType.WEAPON),
                parseItemSection(root, "armors", ItemType.ARMOR),
                parseItemSection(root, "consumables", ItemType.CONSUMABLE),
            ).flatten()

        return ItemDataBundle(baseItems = baseItems, materials = materials, affixes = affixes)
    }

    fun loadTalentDefinitions(input: InputStream): List<TalentDef> {
        val yaml = Yaml()
        val root = yaml.load<Map<String, Any?>>(input)
        val entries = root["talents"] as? List<*> ?: error("talents.yaml must contain a top-level 'talents' list")
        return entries.map { entry ->
            parseTalent(entry as? Map<*, *> ?: error("Talent entries must be maps"))
        }
    }

    fun loadBossDefinition(input: InputStream): BossDefinition {
        val yaml = Yaml()
        val root = yaml.load<Map<String, Any?>>(input)
        val entry = root["boss"] as? Map<*, *> ?: error("boss.yaml must contain a top-level 'boss' map")
        val template = parseBossTemplate(entry)
        val talentLevels =
            entry.requiredMap("talents").entries.associate { (key, value) ->
                key.toString() to value.toString().toInt()
            }
        return BossDefinition(template = template, talentLevels = talentLevels)
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

    private fun parseMaterial(entry: Map<*, *>): MaterialDef =
        MaterialDef(
            id = entry.requiredString("id"),
            name = entry.requiredString("name"),
            minFloor = entry.requiredInt("minFloor"),
            statModifiers = parseStatModifier(entry.optionalMap("stats")),
        )

    private fun parseAffix(entry: Map<*, *>): AffixDef =
        AffixDef(
            id = entry.requiredString("id"),
            name = entry.requiredString("name"),
            type = AffixType.valueOf(entry.requiredString("type")),
            statModifiers = parseStatModifier(entry.requiredMap("stats")),
            minFloor = entry.requiredInt("minFloor"),
        )

    private fun parseItemSection(
        root: Map<String, Any?>,
        key: String,
        type: ItemType,
    ): List<ItemBaseDef> =
        (root[key] as? List<*>).orEmpty().map { rawEntry ->
            val entry = rawEntry as? Map<*, *> ?: error("$key entries must be maps")
            val baseStats =
                when (type) {
                    ItemType.WEAPON -> StatModifier(attack = entry.optionalInt("baseAttack"))
                    ItemType.ARMOR -> StatModifier(defense = entry.optionalInt("baseDefense"))
                    ItemType.CONSUMABLE -> StatModifier()
                }

            ItemBaseDef(
                id = entry.requiredString("id"),
                name = entry.requiredString("name"),
                type = type,
                slot =
                    entry["slot"]?.toString()?.let { slotValue ->
                        EquipSlot.valueOf(slotValue)
                    },
                glyph = entry.requiredString("glyph").single(),
                colorHex = entry.requiredString("color"),
                baseStats = baseStats + parseStatModifier(entry.optionalMap("stats")),
                allowedMaterials = entry.optionalStringList("materials"),
                dropFloors = entry.requiredIntList("dropFloors"),
                dropWeight = entry.requiredInt("dropWeight"),
                effect = entry["effect"]?.toString()?.let { ConsumableEffect.valueOf(it) },
                magnitude = entry.optionalInt("magnitude"),
            )
        }

    private fun parseTalent(entry: Map<*, *>): TalentDef {
        val levelsMap = entry.requiredMap("levels")
        val levelEffects =
            levelsMap.entries.associate { (rawKey, rawValue) ->
                rawKey.toString().toInt() to parseTalentLevelEffect(rawValue as? Map<*, *> ?: error("Talent levels must be maps"))
            }

        return TalentDef(
            id = entry.requiredString("id"),
            name = entry.requiredString("name"),
            description = entry.requiredString("description"),
            maxLevel = entry.requiredInt("maxLevel"),
            staminaCost = entry.requiredInt("staminaCost"),
            cooldown = entry.requiredInt("cooldown"),
            range = entry.requiredInt("range"),
            minRange = entry.optionalInt("minRange"),
            areaRadius = entry.optionalInt("areaRadius"),
            levelEffects = levelEffects,
        )
    }

    private fun parseTalentLevelEffect(entry: Map<*, *>): TalentLevelEffect =
        TalentLevelEffect(
            damageMultiplier = entry.optionalDouble("damageMultiplier", 1.0),
            knockback = entry.optionalInt("knockback"),
            stunDuration = entry.optionalInt("stunDuration"),
            armorBreakDuration = entry.optionalInt("armorBreakDuration"),
            buffDuration = entry.optionalInt("buffDuration"),
            buffMagnitude = entry.optionalDouble("buffMagnitude", 0.0),
            debuffMagnitude = entry.optionalDouble("debuffMagnitude", 0.0),
            debuffDuration = entry.optionalInt("debuffDuration"),
        )

    private fun parseBossTemplate(entry: Map<*, *>): MonsterTemplate {
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
            spawnFloors = listOf(entry.optionalInt("spawnFloor").takeIf { it > 0 } ?: 5),
            spawnWeight = 1,
        )
    }

    private fun parseStatModifier(entry: Map<*, *>?): StatModifier {
        if (entry == null) {
            return StatModifier()
        }

        return StatModifier(
            str = entry.optionalInt("str"),
            dex = entry.optionalInt("dex"),
            con = entry.optionalInt("con"),
            wil = entry.optionalInt("wil"),
            attack = entry.optionalInt("attack"),
            defense = entry.optionalInt("defense"),
            accuracy = entry.optionalInt("accuracy"),
            evasion = entry.optionalInt("evasion"),
            speed = entry.optionalInt("speed"),
            maxHp = entry.optionalInt("maxHp"),
            maxStamina = entry.optionalInt("maxStamina"),
            hpRegen = entry.optionalDouble("hpRegen", 0.0),
            staminaRegen = entry.optionalDouble("staminaRegen", 0.0),
            critChance = entry.optionalDouble("critChance", 0.0),
            talentPower = entry.optionalDouble("talentPower", 0.0),
            attackMultiplierBonus = entry.optionalDouble("attackMultiplierBonus", 0.0),
            defenseMultiplierBonus = entry.optionalDouble("defenseMultiplierBonus", 0.0),
        )
    }

    private fun Map<*, *>.requiredMap(key: String): Map<*, *> =
        this[key] as? Map<*, *> ?: error("Missing map entry '$key'")

    private fun Map<*, *>.optionalMap(key: String): Map<*, *>? = this[key] as? Map<*, *>

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

    private fun Map<*, *>.optionalStringList(key: String): List<String> =
        (this[key] as? List<*>)?.map { value -> value?.toString() ?: error("List '$key' cannot contain nulls") } ?: emptyList()

    private fun Map<*, *>.optionalInt(key: String): Int =
        when (val value = this[key]) {
            null -> 0
            is Int -> value
            is Long -> value.toInt()
            is Number -> value.toInt()
            is String -> value.toInt()
            else -> error("Entry '$key' must be numeric")
        }

    private fun Map<*, *>.optionalDouble(
        key: String,
        default: Double,
    ): Double =
        when (val value = this[key]) {
            null -> default
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Number -> value.toDouble()
            is String -> value.toDouble()
            else -> error("Entry '$key' must be numeric")
        }
}
