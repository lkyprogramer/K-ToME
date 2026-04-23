package com.ktome.client.ui.combat

object CombatAffordanceResourceKeys {
    const val ACTION_ICON: String = "ui.combat.action.icon"
    const val METHOD_ICON: String = "ui.combat.method.icon"
    const val TARGET_ICON: String = "ui.combat.target.icon"
    const val LOCK_ICON: String = "ui.combat.lock.icon"
    const val INVALID_ICON: String = "ui.combat.invalid.icon"

    const val ACTION_CONFIRM_AUDIO: String = "audio.combat.action.confirm"
    const val METHOD_CONFIRM_AUDIO: String = "audio.combat.method.confirm"
    const val TARGET_CONFIRM_AUDIO: String = "audio.combat.target.confirm"
    const val TARGET_LOCK_AUDIO: String = "audio.combat.target.lock"
    const val INVALID_SUBMIT_AUDIO: String = "audio.combat.invalid.submit"

    val visualKeys: Set<String> =
        linkedSetOf(
            ACTION_ICON,
            METHOD_ICON,
            TARGET_ICON,
            LOCK_ICON,
            INVALID_ICON,
        )

    val audioCueKeys: Set<String> =
        linkedSetOf(
            ACTION_CONFIRM_AUDIO,
            METHOD_CONFIRM_AUDIO,
            TARGET_CONFIRM_AUDIO,
            TARGET_LOCK_AUDIO,
            INVALID_SUBMIT_AUDIO,
        )
}
