package com.ktome.game.hidden

enum class SecretRewardAuthorityResolutionSource {
    SECRET_ZONE_DEF,
    MISMATCH,
}

data class ResolvedSecretReward(
    val rewardProfileId: String,
    val source: SecretRewardAuthorityResolutionSource,
    val mismatchReason: String? = null,
)

object SecretRewardAuthority {
    fun resolve(
        secretZone: SecretZoneDef?,
        hiddenEvent: HiddenEventDef?,
    ): ResolvedSecretReward {
        val resolvedSecretZone = requireNotNull(secretZone) { "Secret reward authority requires a secret zone." }
        val mismatchReason = hiddenEvent?.let(::mismatchReason)
        return ResolvedSecretReward(
            rewardProfileId = resolvedSecretZone.rewardProfileId.id,
            source =
                if (mismatchReason == null) {
                    SecretRewardAuthorityResolutionSource.SECRET_ZONE_DEF
                } else {
                    SecretRewardAuthorityResolutionSource.MISMATCH
                },
            mismatchReason = mismatchReason,
        )
    }

    private fun mismatchReason(hiddenEvent: HiddenEventDef): String? {
        if (hiddenEvent.rewards.any { reward -> reward.payload is HiddenEventRewardPayload.LootProfile }) {
            return "loot_profile_present"
        }
        val secretZoneRewardCount =
            hiddenEvent.rewards.count { reward -> reward.payload is HiddenEventRewardPayload.SecretZoneReward }
        return if (secretZoneRewardCount == 1) {
            null
        } else {
            "secret_zone_reward_count_$secretZoneRewardCount"
        }
    }
}
