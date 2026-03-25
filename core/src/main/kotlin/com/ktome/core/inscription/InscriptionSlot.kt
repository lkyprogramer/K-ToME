package com.ktome.core.inscription

const val MAX_INSCRIPTION_SLOTS: Int = 4
const val MAX_INSCRIPTION_PER_CATEGORY: Int = 2
const val INSCRIPTION_HOTKEY_START: Int = 5

data class InscriptionSlot(
    val hotkey: Int,
    val inscriptionId: String,
) {
    init {
        require(hotkey in INSCRIPTION_HOTKEY_START until (INSCRIPTION_HOTKEY_START + MAX_INSCRIPTION_SLOTS)) {
            "Inscription hotkey $hotkey must be within supported range."
        }
        require(inscriptionId.isNotBlank()) { "Inscription slot id must not be blank." }
    }
}

data class InscriptionLoadout(
    val slots: MutableList<InscriptionSlot> = mutableListOf(),
)

data class InscriptionCooldownState(
    val remainingByInscriptionId: MutableMap<String, Int> = linkedMapOf(),
)
