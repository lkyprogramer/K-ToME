package com.ktome.client.screen

internal sealed interface ContinueAvailability {
    data object Available : ContinueAvailability

    data object Absent : ContinueAvailability

    data class Unavailable(
        val reasonCode: ContinueUnavailableReasonCode,
        val savePath: String,
        val throwableClass: String? = null,
        val throwableMessage: String? = null,
    ) : ContinueAvailability {
        val reasonKey: String = reasonCode.reasonKey
    }
}

internal val ContinueAvailability.isAvailable: Boolean
    get() = this is ContinueAvailability.Available

internal enum class ContinueUnavailableReasonCode(
    val reasonKey: String,
) {
    CORRUPTED("ui.menu.continue.unavailable.corrupted"),
    VERSION_MISMATCH("ui.menu.continue.unavailable.version-mismatch"),
    IO_ERROR("ui.menu.continue.unavailable.io-error"),
    SCHEMA_MISMATCH("ui.menu.continue.unavailable.schema-mismatch"),
    UNKNOWN("ui.menu.continue.unavailable.unknown"),
}
