package com.ktome.game.hidden

import com.ktome.core.mapgen.PathClass
import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.core.world.solvability.SearchBindingId

const val HIDDEN_EVENT_REGISTRY_ID: String = "hidden_event"
const val SECRET_ZONE_REGISTRY_ID: String = "secret_zone"
const val LOOT_PROFILE_REGISTRY_ID: String = "loot_profile"
const val STATUS_REGISTRY_ID: String = "status"
const val MONSTER_REGISTRY_ID: String = "monster"

enum class HiddenTriggerType {
    ENTER_ROOM,
    OPEN_CHEST,
    KILL_ELITE,
    INTERACT_TILE,
    QUEST_STEP,
    PERCEPTION_REVEAL,
}

enum class HiddenConditionKey {
    ZONE_ID,
    FLOOR_INDEX,
    SEARCH_BINDING_ID,
    SECRET_ZONE_ID,
    INTERACTABLE_ID,
    ROOM_TAG,
    OBJECTIVE_STEP_KEY,
    ENTRANCE_REVEALED,
    SECRET_ZONE_UNVISITED,
}

data class HiddenEventCondition(
    val key: HiddenConditionKey,
    val expectedValue: String,
) {
    init {
        require(expectedValue.isNotBlank()) { "HiddenEventCondition.expectedValue must not be blank." }
    }
}

enum class HiddenEventRewardKey {
    REVEAL_SECRET_ZONE,
    GRANT_BUFF,
    SECRET_ZONE_REWARD,
    LOOT_PROFILE,
    TRIGGER_ENCOUNTER,
}

sealed interface HiddenEventRewardPayload {
    data class RevealSecretZone(
        val bindingId: SearchBindingId,
    ) : HiddenEventRewardPayload

    data class GrantBuff(
        val statusRef: ContentRef,
        val durationTurns: Int,
        val magnitude: Double = 0.0,
    ) : HiddenEventRewardPayload {
        init {
            require(durationTurns > 0) { "HiddenEventRewardPayload.GrantBuff.durationTurns must be positive." }
        }
    }

    data class LootProfile(
        val lootProfileRef: ContentRef,
    ) : HiddenEventRewardPayload

    data object SecretZoneReward : HiddenEventRewardPayload

    data class TriggerEncounter(
        val encounterRef: ContentRef,
        val threatCost: Int = 0,
    ) : HiddenEventRewardPayload {
        init {
            require(threatCost >= 0) { "HiddenEventRewardPayload.TriggerEncounter.threatCost must not be negative." }
        }
    }
}

data class HiddenEventReward(
    val key: HiddenEventRewardKey,
    val payload: HiddenEventRewardPayload,
) {
    init {
        when (key) {
            HiddenEventRewardKey.REVEAL_SECRET_ZONE -> require(payload is HiddenEventRewardPayload.RevealSecretZone) {
                "REVEAL_SECRET_ZONE rewards must use RevealSecretZone payload."
            }

            HiddenEventRewardKey.GRANT_BUFF -> require(payload is HiddenEventRewardPayload.GrantBuff) {
                "GRANT_BUFF rewards must use GrantBuff payload."
            }

            HiddenEventRewardKey.SECRET_ZONE_REWARD -> require(payload is HiddenEventRewardPayload.SecretZoneReward) {
                "SECRET_ZONE_REWARD rewards must use SecretZoneReward payload."
            }

            HiddenEventRewardKey.LOOT_PROFILE -> require(payload is HiddenEventRewardPayload.LootProfile) {
                "LOOT_PROFILE rewards must use LootProfile payload."
            }

            HiddenEventRewardKey.TRIGGER_ENCOUNTER -> require(payload is HiddenEventRewardPayload.TriggerEncounter) {
                "TRIGGER_ENCOUNTER rewards must use TriggerEncounter payload."
            }
        }
    }
}

data class HiddenEventDef(
    val id: String,
    val triggerType: HiddenTriggerType,
    val conditions: List<HiddenEventCondition>,
    val rewards: List<HiddenEventReward>,
    val grantedDiscoveryTags: Set<String> = emptySet(),
    val optionalOnly: Boolean = true,
) {
    init {
        require(id.isNotBlank()) { "HiddenEventDef.id must not be blank." }
        require(conditions.isNotEmpty()) { "HiddenEventDef.conditions must not be empty." }
        require(rewards.isNotEmpty() || grantedDiscoveryTags.isNotEmpty()) {
            "HiddenEventDef must grant at least one reward or discovery tag."
        }
        require(grantedDiscoveryTags.all(String::isNotBlank)) {
            "HiddenEventDef.grantedDiscoveryTags must not contain blank tags."
        }
    }
}

enum class ReturnBridgePolicy {
    NEAREST_OPTIONAL_ANCHOR,
    LAST_MAINLINE_BRANCH,
    EXPLICIT_ANCHOR,
}

data class SecretZoneDef(
    val id: ContentRef,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val entryRule: DiscoveryRule,
    val pathClass: PathClass,
    val rewardProfileId: ContentRef,
    val guaranteedContent: List<ContentRef>,
    val entranceBindingId: NodeAnchorId,
    val returnBridgePolicy: ReturnBridgePolicy,
    val returnBridgeAnchorTag: String? = null,
) {
    init {
        require(id.registry.value == SECRET_ZONE_REGISTRY_ID) {
            "SecretZoneDef.id must use registry '$SECRET_ZONE_REGISTRY_ID'."
        }
        require(nameKey.isNotBlank()) { "SecretZoneDef.nameKey must not be blank." }
        require(descKey.isNotBlank()) { "SecretZoneDef.descKey must not be blank." }
        require(visualKey.isNotBlank()) { "SecretZoneDef.visualKey must not be blank." }
        require(iconKey.isNotBlank()) { "SecretZoneDef.iconKey must not be blank." }
        require(audioProfile.isNotBlank()) { "SecretZoneDef.audioProfile must not be blank." }
        require(schemaVersion > 0) { "SecretZoneDef.schemaVersion must be positive." }
        require(tags.all(String::isNotBlank)) { "SecretZoneDef.tags must not contain blank entries." }
        require(pathClass == PathClass.SECRET) { "SecretZoneDef.pathClass must remain SECRET." }
        require(rewardProfileId.registry.value == LOOT_PROFILE_REGISTRY_ID) {
            "SecretZoneDef.rewardProfileId must use registry '$LOOT_PROFILE_REGISTRY_ID'."
        }
        require(guaranteedContent.all { content -> content.id.isNotBlank() }) {
            "SecretZoneDef.guaranteedContent must not contain blank content refs."
        }
        require(returnBridgePolicy != ReturnBridgePolicy.EXPLICIT_ANCHOR || !returnBridgeAnchorTag.isNullOrBlank()) {
            "SecretZoneDef.returnBridgeAnchorTag is required for EXPLICIT_ANCHOR."
        }
        require(returnBridgePolicy == ReturnBridgePolicy.EXPLICIT_ANCHOR || returnBridgeAnchorTag == null) {
            "SecretZoneDef.returnBridgeAnchorTag is only supported for EXPLICIT_ANCHOR."
        }
    }
}
