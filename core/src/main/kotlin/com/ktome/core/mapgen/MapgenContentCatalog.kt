package com.ktome.core.mapgen

data class PatternTemplateDef(
    val id: String,
    val rows: List<String>,
) {
    init {
        require(id.isNotBlank()) { "PatternTemplateDef.id must not be blank." }
        requireRectangularTemplateRows(typeName = "PatternTemplateDef", id = id, rows = rows)
    }
}

data class VaultTemplateDef(
    val id: String,
    val rows: List<String>,
) {
    init {
        require(id.isNotBlank()) { "VaultTemplateDef.id must not be blank." }
        requireRectangularTemplateRows(typeName = "VaultTemplateDef", id = id, rows = rows)
    }
}

data class MapgenContentCatalog(
    val roomDefs: List<RoomDef>,
    val patternRooms: List<PatternRoomDef>,
    val patternTemplates: Map<String, PatternTemplateDef>,
    val vaultDefs: List<VaultDef>,
    val vaultTemplates: Map<String, VaultTemplateDef>,
    val biomeFamilies: List<BiomeFamilyDef>,
)

interface ZoneRewardProfileResolver {
    fun resolve(zoneId: String): ZoneRewardProfile
}

private fun requireRectangularTemplateRows(
    typeName: String,
    id: String,
    rows: List<String>,
) {
    require(rows.isNotEmpty()) { "$typeName.rows must not be empty for '$id'." }
    require(rows.all(String::isNotEmpty)) { "$typeName.rows must not contain empty rows for '$id'." }
    require(rows.map(String::length).distinct().size == 1) { "$typeName.rows must be rectangular for '$id'." }
}
