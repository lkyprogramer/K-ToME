package com.ktome.core.save

import kotlinx.serialization.Serializable

@Serializable
data class SaveContractVersion(
    val major: Int,
    val minor: Int = 0,
) {
    init {
        require(major > 0) { "Save contract major version must be positive." }
        require(minor >= 0) { "Save contract minor version must not be negative." }
    }

    override fun toString(): String = "$major.$minor"

    fun isSupported(): Boolean = this == CURRENT

    companion object {
        val CURRENT: SaveContractVersion = SaveContractVersion(4, 0)
    }
}
