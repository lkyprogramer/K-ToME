package com.ktome.game.validation

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.map.Point
import com.ktome.core.mapgen.TerrainOverride
import com.ktome.core.world.solvability.SearchBindingId

sealed interface ValidationAction {
    val family: ValidationActionFamily

    data object RestartSamePreset : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RESTART
    }

    data object RestartNextSeed : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RESTART
    }

    data class TravelToStair(
        val direction: StairDirection,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data object TravelToBoss : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data object TravelToPendingObjective : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data class TravelToPoint(
        val point: Point,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data class TravelToInteractable(
        val interactableId: String,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data class TravelToSearchBinding(
        val bindingId: SearchBindingId,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data class TravelToHiddenEntrance(
        val bindingId: SearchBindingId,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data class TravelToSecretReward(
        val bindingId: SearchBindingId,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data class TravelToSecretReturn(
        val bindingId: SearchBindingId,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TRAVEL
    }

    data object FullHeal : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RECOVERY
    }

    data object RestoreResources : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RECOVERY
    }

    data object ResetCooldowns : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RECOVERY
    }

    data class GrantShards(
        val amount: Int,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RECOVERY
    }

    data class GrantStatPoints(
        val amount: Int,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RECOVERY
    }

    data class GrantTalentPoints(
        val amount: Int,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.RECOVERY
    }

    data object SpawnEliteNearPlayer : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.ENCOUNTER
    }

    data object TriggerBossTelegraph : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.ENCOUNTER
    }

    data object KillNearestHostile : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.ENCOUNTER
    }

    data object KillNearestElite : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.ENCOUNTER
    }

    data object KillActiveBoss : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.ENCOUNTER
    }

    data object ForcePlayerDefeat : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.ENCOUNTER
    }

    data class SetTerrainOverride(
        val point: Point,
        val terrainOverride: TerrainOverride,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TERRAIN
    }

    data class ClearTerrainOverride(
        val point: Point,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.TERRAIN
    }

    data class PresentReward(
        val profileIds: List<String>,
        val fallbackBaseId: String,
        val sourceId: String = "validation.reward",
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.REWARD_AND_ITEM
    }

    data class SpawnItem(
        val baseItemId: String,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.REWARD_AND_ITEM
    }

    data object SpawnPr03ItemShowcase : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.REWARD_AND_ITEM
    }

    data object ExecuteSearch : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.DISCOVERY
    }

    data class RevealBinding(
        val bindingId: SearchBindingId,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.DISCOVERY
    }

    data class Phase4V4ScenarioAction(
        val scenarioId: ValidationScenarioId,
        val actionId: ValidationScenarioActionId,
    ) : ValidationAction {
        override val family: ValidationActionFamily = ValidationActionFamily.PHASE4_V4_FAST
    }
}
