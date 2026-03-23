package com.ktome.core.combat

import com.ktome.core.random.RandomSource

enum class ApplicationPolicy {
    SELF_AUTO,
    HOSTILE_HIT_THEN_SAVE,
    HOSTILE_SAVE_ONLY,
    TAG_AUTO,
    INSTANT_ACTION,
    ;

    fun requiresHit(): Boolean = this == HOSTILE_HIT_THEN_SAVE

    fun requiresSave(): Boolean = this == HOSTILE_HIT_THEN_SAVE || this == HOSTILE_SAVE_ONLY

    fun autoApplies(): Boolean = this == SELF_AUTO || this == TAG_AUTO || this == INSTANT_ACTION
}

data class StatusApplicationRequest(
    val statusId: String,
    val duration: Int,
    val applicationPolicy: ApplicationPolicy,
    val saveDimension: SaveDimension? = null,
    val power: Int = 0,
    val save: Int = 0,
    val tagMatched: Boolean = false,
)

data class StatusApplicationResolution(
    val attempted: Boolean,
    val applied: Boolean,
    val reasonTag: String,
    val applyChance: Double? = null,
    val roll: Double? = null,
)

object ApplicationPolicyResolver {
    fun resolve(
        request: StatusApplicationRequest,
        hitSucceeded: Boolean,
        random: RandomSource,
    ): StatusApplicationResolution {
        return when (request.applicationPolicy) {
            ApplicationPolicy.SELF_AUTO ->
                StatusApplicationResolution(
                    attempted = true,
                    applied = true,
                    reasonTag = "SELF_AUTO",
                )

            ApplicationPolicy.INSTANT_ACTION ->
                StatusApplicationResolution(
                    attempted = true,
                    applied = true,
                    reasonTag = "INSTANT_ACTION",
                )

            ApplicationPolicy.TAG_AUTO ->
                if (request.tagMatched) {
                    StatusApplicationResolution(
                        attempted = true,
                        applied = true,
                        reasonTag = "TAG_AUTO",
                    )
                } else {
                    StatusApplicationResolution(
                        attempted = false,
                        applied = false,
                        reasonTag = "TAG_MISMATCH",
                    )
                }

            ApplicationPolicy.HOSTILE_HIT_THEN_SAVE -> {
                if (!hitSucceeded) {
                    return StatusApplicationResolution(
                        attempted = false,
                        applied = false,
                        reasonTag = "MISS_GATE",
                    )
                }
                resolveWithPowerSave(request, random)
            }

            ApplicationPolicy.HOSTILE_SAVE_ONLY -> resolveWithPowerSave(request, random)
        }
    }

    private fun resolveWithPowerSave(
        request: StatusApplicationRequest,
        random: RandomSource,
    ): StatusApplicationResolution {
        requireNotNull(request.saveDimension) {
            "ApplicationPolicy ${request.applicationPolicy} requires an explicit saveDimension."
        }
        val resolution = PowerSaveFormula.resolve(request.power, request.save, random)
        return StatusApplicationResolution(
            attempted = true,
            applied = resolution.applied,
            reasonTag = if (resolution.applied) "POWER_SAVE_SUCCESS" else "POWER_SAVE_RESIST",
            applyChance = resolution.applyChance,
            roll = resolution.roll,
        )
    }
}
