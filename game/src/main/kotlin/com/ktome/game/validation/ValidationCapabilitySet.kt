package com.ktome.game.validation

enum class ValidationActionFamily {
    RESTART,
    TRAVEL,
    RECOVERY,
    ENCOUNTER,
    TERRAIN,
    REWARD_AND_ITEM,
    DISCOVERY,
}

data class ValidationCapabilitySet(
    val restart: Boolean = true,
    val travel: Boolean = true,
    val recovery: Boolean = true,
    val encounter: Boolean = true,
    val terrain: Boolean = true,
    val rewardAndItem: Boolean = true,
    val discovery: Boolean = true,
) {
    fun allows(family: ValidationActionFamily): Boolean =
        when (family) {
            ValidationActionFamily.RESTART -> restart
            ValidationActionFamily.TRAVEL -> travel
            ValidationActionFamily.RECOVERY -> recovery
            ValidationActionFamily.ENCOUNTER -> encounter
            ValidationActionFamily.TERRAIN -> terrain
            ValidationActionFamily.REWARD_AND_ITEM -> rewardAndItem
            ValidationActionFamily.DISCOVERY -> discovery
        }

    companion object {
        val ALL_ENABLED: ValidationCapabilitySet = ValidationCapabilitySet()
    }
}
