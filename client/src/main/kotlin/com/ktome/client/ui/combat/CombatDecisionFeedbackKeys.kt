package com.ktome.client.ui.combat

internal object CombatDecisionFeedbackKeys {
    const val DISABLED_NO_METHOD: String = "ui.combat.disabled.no-method"
    const val DISABLED_COOLDOWN: String = "ui.combat.disabled.cooldown"
    const val DISABLED_RESOURCE: String = "ui.combat.disabled.resource"
    const val NO_LEGAL_TARGET: String = "ui.message.combat.no-legal-target"
    const val ILLEGAL_TARGET: String = "ui.message.combat.illegal-target"
    const val NO_AVAILABLE_ACTION: String = "ui.message.combat.no-available-action"
    const val UNKNOWN_ACTION: String = "ui.combat.disabled.unknown-action"

    val invalidSubmitMessageKeys: Set<String> =
        linkedSetOf(
            NO_LEGAL_TARGET,
            ILLEGAL_TARGET,
            NO_AVAILABLE_ACTION,
            DISABLED_NO_METHOD,
            DISABLED_COOLDOWN,
            DISABLED_RESOURCE,
        )
}
