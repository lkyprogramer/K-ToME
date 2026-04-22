package com.ktome.client.screen

internal data class DesktopLauncherTitleContext(
    val localeId: String? = null,
    val seed: Long? = null,
    val saveSlot: String? = null,
)

internal object DesktopLauncherTitleFormatter {
    private const val appTitle: String = "K-ToME"

    fun format(context: DesktopLauncherTitleContext = DesktopLauncherTitleContext()): String {
        val segments =
            listOfNotNull(
                appTitle,
                context.localeId?.takeIf(String::isNotBlank),
                context.seed?.toString(),
                context.saveSlot?.takeIf(String::isNotBlank),
            )
        return segments.joinToString(" · ").ifBlank { appTitle }
    }
}
