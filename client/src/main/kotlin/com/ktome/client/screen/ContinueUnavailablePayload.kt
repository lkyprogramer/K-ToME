package com.ktome.client.screen

import com.ktome.client.build.BuildInfo
import com.ktome.client.ui.state.UiErrorPayload
import com.ktome.game.contentpack.GameBuildVersion
import com.ktome.game.i18n.Localizer

internal object ContinueUnavailablePayloadFormatter {
    private const val throwableMessageMaxChars: Int = 200

    fun format(
        localizer: Localizer,
        unavailable: ContinueAvailability.Unavailable,
        buildHash: String = BuildInfo.shortHash,
        gameVersion: String = GameBuildVersion.current(),
    ): UiErrorPayload {
        val context =
            mutableListOf(
                "savePath" to unavailable.savePath,
                "reasonCode" to unavailable.reasonCode.name,
                "gameVersion" to gameVersion,
            )
        if (unavailable.reasonCode == ContinueUnavailableReasonCode.UNKNOWN) {
            unavailable.throwableClass?.let { throwableClass -> context += "throwableClass" to throwableClass }
            unavailable.throwableMessage?.let { throwableMessage -> context += "throwableMessage" to truncateThrowableMessage(throwableMessage) }
        }
        return UiErrorPayload(
            heading = localizer.text("ui.menu.continue.error.heading"),
            detail = localizer.text(unavailable.reasonKey),
            contextKeyValuePairs = context,
            buildHash = buildHash,
        )
    }

    private fun truncateThrowableMessage(message: String): String =
        if (message.length <= throwableMessageMaxChars) {
            message
        } else {
            message.take(throwableMessageMaxChars - 3) + "..."
        }
}
