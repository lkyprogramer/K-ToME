package com.ktome.core.profession

import com.ktome.core.profile.ClassUnlockState
import com.ktome.core.resource.ResourceAxis
import com.ktome.core.resource.ResourceProfileRef

data class ProfessionDef(
    val id: String,
    val tier: ProfessionTier,
    val resourceProfiles: List<ResourceProfileRef>,
    val primarySpendAxis: ResourceAxis?,
    val stateAxis: ResourceAxis?,
    val initialUnlockState: ClassUnlockState = ClassUnlockState.RELEASE_UNLOCKED,
    val releaseUnlockCondition: ReleaseUnlockCondition? = null,
    val soloContract: SoloContractDef,
) {
    init {
        require(id.isNotBlank()) { "Profession id must not be blank." }
        require(resourceProfiles.isNotEmpty()) { "Profession '$id' must declare at least one resource profile." }
        require(resourceProfiles.distinctBy(ResourceProfileRef::axis).size == resourceProfiles.size) {
            "Profession '$id' must not declare duplicate resource axes."
        }
        require(resourceProfiles.size <= 2) { "Profession '$id' must not declare more than two resource axes." }
        primarySpendAxis?.let { axis ->
            require(resourceProfiles.any { profile -> profile.axis == axis }) {
                "Profession '$id' primary axis '$axis' must be present in resourceProfiles."
            }
        }
        stateAxis?.let { axis ->
            require(resourceProfiles.any { profile -> profile.axis == axis }) {
                "Profession '$id' state axis '$axis' must be present in resourceProfiles."
            }
        }
    }
}

enum class ProfessionTier {
    BASE,
    ADVANCED,
}

sealed interface ReleaseUnlockCondition {
    data class RequireProfessionCleared(val professionId: String) : ReleaseUnlockCondition {
        init {
            require(professionId.isNotBlank()) { "Required profession id must not be blank." }
        }
    }
}

data class SoloContractDef(
    val offenseTags: List<String>,
    val defenseTags: List<String>,
    val mobilityTags: List<String>,
    val aoeAnswerTags: List<String>,
    val bossAnswerTags: List<String>,
    val panicAnswerTags: List<String>,
) {
    init {
        require(offenseTags.isNotEmpty()) { "Solo contract offenseTags must not be empty." }
        require(defenseTags.isNotEmpty()) { "Solo contract defenseTags must not be empty." }
        require(mobilityTags.isNotEmpty()) { "Solo contract mobilityTags must not be empty." }
        require(aoeAnswerTags.isNotEmpty()) { "Solo contract aoeAnswerTags must not be empty." }
        require(bossAnswerTags.isNotEmpty()) { "Solo contract bossAnswerTags must not be empty." }
        require(panicAnswerTags.isNotEmpty()) { "Solo contract panicAnswerTags must not be empty." }
    }
}
