package com.ktome.game.hidden

data class SecretEncounterRuntime(
    val encounterId: String,
    val secretZoneId: String,
    val threatCost: Int = 0,
) {
    init {
        require(encounterId.isNotBlank()) { "SecretEncounterRuntime.encounterId must not be blank." }
        require(secretZoneId.isNotBlank()) { "SecretEncounterRuntime.secretZoneId must not be blank." }
        require(threatCost >= 0) { "SecretEncounterRuntime.threatCost must not be negative." }
    }
}
