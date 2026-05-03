package com.ktome.core.snapshot

import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RenderSnapshotHasher {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }

    fun canonicalJson(snapshot: RenderSnapshot): String = json.encodeToString(canonicalSnapshot(snapshot))

    private fun canonicalSnapshot(snapshot: RenderSnapshot): RenderSnapshot =
        snapshot.copy(
            uiState =
                snapshot.uiState.copy(
                    frontstageReadability =
                        snapshot.uiState.frontstageReadability.copy(
                            recentActionCues =
                                snapshot.uiState.frontstageReadability.recentActionCues.sortedWith(
                                    compareBy<FrontstageActionCueSnapshot> { cue -> cue.stableKey }
                                        .thenBy { cue -> cue.category.name }
                                        .thenBy { cue -> cue.cueType.name }
                                        .thenBy { cue -> cue.priority.name }
                                        .thenBy { cue -> cue.message.key }
                                        .thenBy { cue -> cue.message.arguments.joinToString(separator = "|", transform = ::argumentSortKey) },
                                ),
                        ),
                ),
        )

    private fun argumentSortKey(argument: RenderTextArgumentSnapshot): String =
        listOf(
            argument.name,
            argument.value.orEmpty(),
            argument.valueKey.orEmpty(),
            argument.valueToken?.let(::tokenSortKey).orEmpty(),
        ).joinToString(separator = "=")

    private fun tokenSortKey(token: RenderTextTokenSnapshot): String =
        token.key + ":" + token.arguments.joinToString(separator = "|", transform = ::argumentSortKey)

    fun sha256(snapshot: RenderSnapshot): String =
        MessageDigest.getInstance("SHA-256")
            .digest(canonicalJson(snapshot).toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
