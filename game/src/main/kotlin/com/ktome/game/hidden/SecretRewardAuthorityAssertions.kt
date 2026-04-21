package com.ktome.game.hidden

import com.ktome.game.data.schema.SchemaCatalog

data class SecretRewardAuthorityViolation(
    val secretZoneId: String,
    val hiddenEventId: String,
    val rewardKeys: List<String>,
    val reason: String,
) {
    val culpritId: String
        get() = "$secretZoneId:$hiddenEventId"

    val violationId: String
        get() = "$culpritId:$reason"

    fun validationMessage(): String =
        when (reason) {
            "missing_hidden_event" ->
                "Secret zone '$secretZoneId' guaranteed content hidden event '$hiddenEventId' is missing."
            "loot_profile_present" ->
                "Secret zone '$secretZoneId' hidden event '$hiddenEventId' must not declare LOOT_PROFILE; reward authority belongs to SecretZoneDef.rewardProfileId."
            else ->
                "Secret zone '$secretZoneId' hidden event '$hiddenEventId' must declare exactly one SECRET_ZONE_REWARD reward."
        }
}

object SecretRewardAuthorityAssertions {
    fun rewardStructureKeysByProfileId(schemaCatalog: SchemaCatalog): Map<String, List<String>> =
        schemaCatalog.secretZones.associate { secretZone ->
            val rewardKeys =
                secretZone.guaranteedContent
                    .filter { contentRef -> contentRef.registry.value == HIDDEN_EVENT_REGISTRY_ID }
                    .flatMap { contentRef ->
                        schemaCatalog.hiddenEvents
                            .firstOrNull { hiddenEvent -> hiddenEvent.id == contentRef.id }
                            ?.rewards
                            ?.map { reward -> reward.key.name }
                            .orEmpty()
                    }.distinct()
                    .sorted()
            secretZone.rewardProfileId.id to rewardKeys
        }

    fun scanCatalog(schemaCatalog: SchemaCatalog): List<SecretRewardAuthorityViolation> =
        schemaCatalog.secretZones
            .flatMap { secretZone ->
                secretZone.guaranteedContent
                    .filter { contentRef -> contentRef.registry.value == HIDDEN_EVENT_REGISTRY_ID }
                    .mapNotNull { contentRef ->
                        val hiddenEvent =
                            schemaCatalog.hiddenEvents.firstOrNull { event -> event.id == contentRef.id }
                                ?: return@mapNotNull SecretRewardAuthorityViolation(
                                    secretZoneId = secretZone.id.id,
                                    hiddenEventId = contentRef.id,
                                    rewardKeys = emptyList(),
                                    reason = "missing_hidden_event",
                                )
                        val resolvedReward = SecretRewardAuthority.resolve(secretZone = secretZone, hiddenEvent = hiddenEvent)
                        if (resolvedReward.mismatchReason == null) {
                            null
                        } else {
                            SecretRewardAuthorityViolation(
                                secretZoneId = secretZone.id.id,
                                hiddenEventId = hiddenEvent.id,
                                rewardKeys = hiddenEvent.rewards.map { reward -> reward.key.name }.sorted(),
                                reason = resolvedReward.mismatchReason,
                            )
                        }
                    }
            }.sortedBy(SecretRewardAuthorityViolation::violationId)
}
