package com.ktome.game

import java.security.MessageDigest

fun zoneRouteHash(zoneRoute: List<String>): String = sha256(zoneRoute.joinToString(separator = ">"))

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
