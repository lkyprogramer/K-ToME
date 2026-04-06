package com.ktome.core.phase

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PackId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "PackId must not be blank." }
    }

    override fun toString(): String = value
}

object Phase4ContractVersions {
    const val PHASE_ID: String = "P4"
    const val CONTENT_SCHEMA_VERSION: Int = 2
    const val TOPOLOGY_FINGERPRINT_VERSION: Int = 2
    const val REWARD_LEDGER_VERSION: Int = 1
    const val LOOT_FORMULA_VERSION: Int = 2
    const val SPECIAL_TIER_ELIGIBILITY_VERSION: Int = 2
    const val SEARCH_RULE_VERSION: Int = 1
    const val SECRET_RULE_VERSION: Int = 1
    const val OVERLAY_CONTRACT_VERSION: Int = 1
}
