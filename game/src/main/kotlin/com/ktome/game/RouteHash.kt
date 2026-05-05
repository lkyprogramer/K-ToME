package com.ktome.game

import java.security.MessageDigest

private const val ROUTE_HASH_LENGTH = 16
private const val ROUTE_TOKEN_SEPARATOR = ">"
private const val SECRET_ROUTE_MARKER_PREFIX = "secret:"

fun routeToken(routeParts: List<String>): String {
    require(routeParts.isNotEmpty()) { "routeParts must not be empty." }
    routeParts.forEach(::validateRouteTokenPart)
    return routeParts.joinToString(separator = ROUTE_TOKEN_SEPARATOR)
}

fun zoneRouteHash(routeParts: List<String>): String = sha256(routeToken(routeParts)).take(ROUTE_HASH_LENGTH)

fun secretRouteMarker(secretZoneId: String): String {
    validateSecretRouteMarkerId(secretZoneId)
    return "$SECRET_ROUTE_MARKER_PREFIX$secretZoneId"
}

fun isSecretRouteMarker(routePart: String): Boolean = routePart.startsWith(SECRET_ROUTE_MARKER_PREFIX)

fun validateRouteTokenPart(routePart: String) {
    require(routePart.isNotBlank()) { "route token part must not be blank." }
    if (isSecretRouteMarker(routePart)) {
        validateSecretRouteMarkerId(routePart.removePrefix(SECRET_ROUTE_MARKER_PREFIX))
    } else {
        require(routePart.none { char -> char == ':' || char == '|' || char == '>' }) {
            "route token part '$routePart' must not contain ':', '|', or '>'."
        }
    }
}

fun validateSecretRouteMarkerId(secretZoneId: String) {
    require(secretZoneId.isNotBlank()) { "secretZoneId must not be blank." }
    require(secretZoneId.none { char -> char == ':' || char == '|' || char == '>' }) {
        "secretZoneId '$secretZoneId' must not contain ':', '|', or '>'."
    }
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
