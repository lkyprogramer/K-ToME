package com.ktome.game

import com.ktome.core.ecs.AIType
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.model.MonsterTemplate

internal data class RouteVisibleEncounterFloor(
    val zoneId: String,
    val floor: Int,
    val isBossFloor: Boolean,
    val packEnabled: Boolean,
    val monsterIds: List<String>,
)

internal fun routeVisibleEncounterFloor(
    zone: ZoneSchemaV2,
    floor: Int,
    runtimeCatalog: Map<String, MonsterTemplate>,
    bossTemplateIdsByEncounterId: Map<String, String>,
): RouteVisibleEncounterFloor {
    val isBossFloor = isBossEncounterFloor(zone, floor)
    val monsterIds =
        when {
            isBossFloor ->
                routeVisibleBossTemplates(zone, runtimeCatalog, bossTemplateIdsByEncounterId)
                    .map(MonsterTemplate::id)
            else ->
                regularEncounterTemplates(
                    zone = zone,
                    floor = floor,
                    allowedIds =
                        buildList {
                            addAll(zone.monsterPools)
                            if (floor > 1) {
                                addAll(zone.elitePools)
                            }
                        },
                    runtimeCatalog = runtimeCatalog,
                ).map(MonsterTemplate::id)
        }
    return RouteVisibleEncounterFloor(
        zoneId = zone.id,
        floor = floor,
        isBossFloor = isBossFloor,
        packEnabled = !isBossFloor && allowsRoomPackForTest(zone, floor),
        monsterIds = monsterIds.distinct(),
    )
}

internal fun routeVisibleMonsters(
    zone: ZoneSchemaV2,
    runtimeCatalog: Map<String, MonsterTemplate>,
    bossTemplateIdsByEncounterId: Map<String, String>,
): List<MonsterTemplate> =
    (1..zone.floorCount)
        .flatMap { floor ->
            when {
                isBossEncounterFloor(zone, floor) ->
                    routeVisibleBossTemplates(zone, runtimeCatalog, bossTemplateIdsByEncounterId)
                else ->
                    regularEncounterTemplates(
                        zone = zone,
                        floor = floor,
                        allowedIds =
                            buildList {
                                addAll(zone.monsterPools)
                                if (floor > 1) {
                                    addAll(zone.elitePools)
                                }
                            },
                        runtimeCatalog = runtimeCatalog,
                    )
            }
        }.distinctBy(MonsterTemplate::id)

internal fun routeVisibleCommonMonsters(
    zone: ZoneSchemaV2,
    runtimeCatalog: Map<String, MonsterTemplate>,
): List<MonsterTemplate> =
    (1..zone.floorCount)
        .filterNot { floor -> isBossEncounterFloor(zone, floor) }
        .flatMap { floor ->
            regularEncounterTemplates(
                zone = zone,
                floor = floor,
                allowedIds = zone.monsterPools,
                runtimeCatalog = runtimeCatalog,
            )
        }.distinctBy(MonsterTemplate::id)

internal fun routeVisibleEliteOrBossMonsters(
    zone: ZoneSchemaV2,
    runtimeCatalog: Map<String, MonsterTemplate>,
    bossTemplateIdsByEncounterId: Map<String, String>,
): List<MonsterTemplate> =
    (1..zone.floorCount)
        .flatMap { floor ->
            when {
                isBossEncounterFloor(zone, floor) ->
                    routeVisibleBossTemplates(zone, runtimeCatalog, bossTemplateIdsByEncounterId)
                floor > 1 ->
                    regularEncounterTemplates(
                        zone = zone,
                        floor = floor,
                        allowedIds = zone.elitePools,
                        runtimeCatalog = runtimeCatalog,
                    )
                else -> emptyList()
            }
        }.distinctBy(MonsterTemplate::id)

internal fun isRangedPressure(monster: MonsterTemplate): Boolean =
    monster.ai == AIType.KITE || monster.archetype in setOf("artillery", "controller")

private fun routeVisibleBossTemplates(
    zone: ZoneSchemaV2,
    runtimeCatalog: Map<String, MonsterTemplate>,
    bossTemplateIdsByEncounterId: Map<String, String>,
): List<MonsterTemplate> {
    val bossTemplateId = zone.bossEncounterId?.let(bossTemplateIdsByEncounterId::get) ?: return emptyList()
    return listOf(requireNotNull(runtimeCatalog[bossTemplateId]) { "Missing runtime boss monster $bossTemplateId." })
}

private fun regularEncounterTemplates(
    zone: ZoneSchemaV2,
    floor: Int,
    allowedIds: List<String>,
    runtimeCatalog: Map<String, MonsterTemplate>,
): List<MonsterTemplate> {
    if (isBossEncounterFloor(zone, floor)) {
        return emptyList()
    }
    val scopedCatalog =
        if (allowedIds.isEmpty()) {
            runtimeCatalog.values.toList()
        } else {
            allowedIds.map { monsterId ->
                requireNotNull(runtimeCatalog[monsterId]) { "Missing runtime monster $monsterId." }
            }
        }
    val floorCatalog = scopedCatalog.filter { monster -> floor in monster.spawnFloors }.ifEmpty { scopedCatalog }
    return floorCatalog.filter { monster -> isRegularEncounterSpawnable(zone, floor, monster) }.distinctBy(MonsterTemplate::id)
}

private fun isBossEncounterFloor(
    zone: ZoneSchemaV2,
    floor: Int,
): Boolean = zone.bossEncounterId != null && floor == zone.floorCount

private fun allowsRoomPackForTest(
    zone: ZoneSchemaV2,
    floor: Int,
): Boolean {
    val method = GameModule::class.java.getDeclaredMethod("allowsRoomPack", ZoneSchemaV2::class.java, Int::class.javaPrimitiveType)
    method.isAccessible = true
    return method.invoke(GameModule, zone, floor) as Boolean
}

private fun isRegularEncounterSpawnable(
    zone: ZoneSchemaV2,
    floor: Int,
    template: MonsterTemplate,
): Boolean {
    val method =
        GameModule::class.java.getDeclaredMethod(
            "canSelectEncounterTemplate",
            ZoneSchemaV2::class.java,
            Int::class.javaPrimitiveType,
            List::class.java,
            MonsterTemplate::class.java,
        )
    method.isAccessible = true
    return method.invoke(GameModule, zone, floor, emptyList<MonsterTemplate>(), template) as Boolean
}
