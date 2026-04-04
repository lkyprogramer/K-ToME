package com.ktome.game.mapgen

import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.mapgen.ZoneRewardProfileResolver
import com.ktome.game.data.schema.ZoneSchemaV2

class SchemaZoneRewardProfileResolver(
    zones: Collection<ZoneSchemaV2>,
    profiles: Collection<ZoneRewardProfile>,
) : ZoneRewardProfileResolver {
    private val zoneById: Map<String, ZoneSchemaV2> = zones.associateBy(ZoneSchemaV2::id)
    private val profileById: Map<String, ZoneRewardProfile> = profiles.associateBy(ZoneRewardProfile::id)

    override fun resolve(zoneId: String): ZoneRewardProfile {
        val zone = requireNotNull(zoneById[zoneId]) { "Unknown zone '$zoneId' for ZoneRewardProfileResolver." }
        val explicitProfileId = zone.rewardProfileId
        if (explicitProfileId != null) {
            val profile = requireNotNull(profileById[explicitProfileId]) {
                "Zone '${zone.id}' references unknown reward profile '$explicitProfileId'."
            }
            require(profile.zoneId == zone.id) {
                "Reward profile '${profile.id}' belongs to '${profile.zoneId}', but zone '${zone.id}' referenced it."
            }
            return profile
        }
        return ZoneRewardProfile(
            id = "${zone.id}.fallback",
            zoneId = zone.id,
            rarityBonus = 0.0f,
            qualityBonus = 0,
            baseRewardBudget = 0,
        )
    }
}
