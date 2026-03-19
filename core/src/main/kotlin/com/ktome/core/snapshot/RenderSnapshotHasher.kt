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

    fun canonicalJson(snapshot: RenderSnapshot): String = json.encodeToString(snapshot)

    fun sha256(snapshot: RenderSnapshot): String =
        MessageDigest.getInstance("SHA-256")
            .digest(canonicalJson(snapshot).toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
