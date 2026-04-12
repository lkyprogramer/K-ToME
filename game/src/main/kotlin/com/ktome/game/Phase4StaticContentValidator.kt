package com.ktome.game

import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.StatusSchemaV2
import com.ktome.game.hidden.HiddenConditionKey
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.hidden.LOOT_PROFILE_REGISTRY_ID
import com.ktome.game.hidden.MONSTER_REGISTRY_ID
import com.ktome.game.hidden.STATUS_REGISTRY_ID
import com.ktome.game.model.MonsterTemplate

data class HiddenContentStaticSnapshot(
    val hiddenEventCount: Int,
    val secretZoneCount: Int,
    val searchBindingIds: List<String>,
    val secretZoneIds: List<String>,
)

object Phase4StaticContentValidator {
    fun validateHiddenContentContracts(
        schemaCatalog: SchemaCatalog,
        lootProfilesById: Map<String, LootProfileSchemaV3>,
        monsterTemplatesById: Map<String, MonsterTemplate>,
        statuses: List<StatusSchemaV2>,
    ): HiddenContentStaticSnapshot {
        val hiddenEventIds = schemaCatalog.hiddenEvents.mapTo(linkedSetOf()) { hiddenEvent -> hiddenEvent.id }
        val secretZoneIds = schemaCatalog.secretZones.mapTo(linkedSetOf()) { secretZone -> secretZone.id.id }
        val statusIds =
            statuses.flatMapTo(linkedSetOf()) { status ->
                listOf(status.id, status.effectType)
            }
        val hiddenEntrancePlans = schemaCatalog.zoneMapgenProfiles.flatMap { profile -> profile.hiddenEntrancePlans }
        val hiddenEntrancePlansByBindingId = hiddenEntrancePlans.associateBy { plan -> plan.bindingId }
        val hiddenEntrancePlansBySecretZoneId = hiddenEntrancePlans.associateBy { plan -> plan.targetSecretZoneId.id }
        val hiddenEntranceBindingIds = hiddenEntrancePlansByBindingId.keys.mapTo(linkedSetOf()) { bindingId -> bindingId.value }
        require(hiddenEntrancePlansBySecretZoneId.size == hiddenEntrancePlans.size) {
            "Each hidden entrance plan must target a unique secret zone id."
        }
        val validatedSecretZoneIds = linkedSetOf<String>()
        schemaCatalog.secretZones.forEach { secretZone ->
            validatedSecretZoneIds += secretZone.id.id
            val hiddenEntrancePlan = requireNotNull(hiddenEntrancePlansBySecretZoneId[secretZone.id.id]) {
                "Secret zone '${secretZone.id.id}' is not targeted by any hidden entrance plan."
            }
            require(hiddenEntrancePlan.targetSecretZoneId == secretZone.id) {
                "Secret zone '${secretZone.id.id}' must be targeted by hidden entrance '${hiddenEntrancePlan.bindingId.value}'."
            }
            require(secretZone.entranceBindingId == hiddenEntrancePlan.entranceAnchorId) {
                "Secret zone '${secretZone.id.id}' must bind to hidden entrance anchor '${hiddenEntrancePlan.entranceAnchorId.value}'."
            }
            require(secretZone.entryRule == hiddenEntrancePlan.discoveryRule) {
                "Secret zone '${secretZone.id.id}' entryRule must match hidden entrance discoveryRule '${hiddenEntrancePlan.bindingId.value}'."
            }
            require(lootProfilesById.containsKey(secretZone.rewardProfileId.id)) {
                "Secret zone '${secretZone.id.id}' references unknown reward profile '${secretZone.rewardProfileId.id}'."
            }
            secretZone.guaranteedContent.forEach { contentRef ->
                when (contentRef.registry.value) {
                    "hidden_event" ->
                        require(contentRef.id in hiddenEventIds) {
                            "Secret zone '${secretZone.id.id}' guaranteed content references unknown hidden event '${contentRef.id}'."
                        }

                    "monster" ->
                        require(monsterTemplatesById.containsKey(contentRef.id)) {
                            "Secret zone '${secretZone.id.id}' guaranteed content references unknown monster '${contentRef.id}'."
                        }

                    else -> error("Secret zone '${secretZone.id.id}' guaranteed content registry '${contentRef.registry.value}' is unsupported.")
                }
            }
        }
        schemaCatalog.hiddenEvents.forEach { hiddenEvent ->
            hiddenEvent.conditions.forEach { condition ->
                when (condition.key) {
                    HiddenConditionKey.SEARCH_BINDING_ID ->
                        require(condition.expectedValue in hiddenEntranceBindingIds) {
                            "Hidden event '${hiddenEvent.id}' references unknown search binding '${condition.expectedValue}'."
                        }

                    HiddenConditionKey.SECRET_ZONE_ID ->
                        require(condition.expectedValue in secretZoneIds) {
                            "Hidden event '${hiddenEvent.id}' references unknown secret zone '${condition.expectedValue}'."
                        }

                    else -> Unit
                }
            }
            hiddenEvent.rewards.forEach { reward ->
                when (val payload = reward.payload) {
                    is HiddenEventRewardPayload.RevealSecretZone ->
                        require(hiddenEntrancePlansByBindingId.containsKey(payload.bindingId)) {
                            "Hidden event '${hiddenEvent.id}' reveal payload references unknown search binding '${payload.bindingId.value}'."
                        }

                    is HiddenEventRewardPayload.GrantBuff -> {
                        require(payload.statusRef.registry.value == STATUS_REGISTRY_ID) {
                            "Hidden event '${hiddenEvent.id}' buff payload must use registry '$STATUS_REGISTRY_ID'."
                        }
                        require(payload.statusRef.id in statusIds) {
                            "Hidden event '${hiddenEvent.id}' references unknown status '${payload.statusRef.id}'."
                        }
                    }

                    is HiddenEventRewardPayload.LootProfile -> {
                        require(payload.lootProfileRef.registry.value == LOOT_PROFILE_REGISTRY_ID) {
                            "Hidden event '${hiddenEvent.id}' loot payload must use registry '$LOOT_PROFILE_REGISTRY_ID'."
                        }
                        require(lootProfilesById.containsKey(payload.lootProfileRef.id)) {
                            "Hidden event '${hiddenEvent.id}' references unknown loot profile '${payload.lootProfileRef.id}'."
                        }
                    }

                    is HiddenEventRewardPayload.TriggerEncounter -> {
                        require(payload.encounterRef.registry.value == MONSTER_REGISTRY_ID) {
                            "Hidden event '${hiddenEvent.id}' encounter payload must use registry '$MONSTER_REGISTRY_ID'."
                        }
                        require(monsterTemplatesById.containsKey(payload.encounterRef.id)) {
                            "Hidden event '${hiddenEvent.id}' references unknown monster '${payload.encounterRef.id}'."
                        }
                    }
                }
            }
        }
        return HiddenContentStaticSnapshot(
            hiddenEventCount = schemaCatalog.hiddenEvents.size,
            secretZoneCount = schemaCatalog.secretZones.size,
            searchBindingIds = hiddenEntrancePlansByBindingId.keys.map { bindingId -> bindingId.value }.sorted(),
            secretZoneIds = validatedSecretZoneIds.toList().sorted(),
        )
    }
}
