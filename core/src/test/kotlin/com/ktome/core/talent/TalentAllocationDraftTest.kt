package com.ktome.core.talent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TalentAllocationDraftTest {
    @Test
    fun `preview computes pending deltas without mutating live ranks`() {
        val liveRanks = linkedMapOf("power_strike" to 1, "charge" to 2)
        val minimumRanks = linkedMapOf("power_strike" to 1, "charge" to 1)
        val draft =
            TalentAllocationPlanner.applyRankIncrease(
                draft = null,
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "vanguard",
                talentId = "charge",
                nextRank = 3,
            )

        val preview = TalentAllocationPlanner.preview(liveRanks, minimumRanks, availablePoints = 4, draft = draft)

        assertEquals(mapOf("power_strike" to 1, "charge" to 3), preview.effectiveRanks)
        assertEquals(1, preview.spentPointsDelta)
        assertEquals(3, preview.remainingPoints)
        assertEquals(mapOf("power_strike" to 1, "charge" to 2), liveRanks)
    }

    @Test
    fun `respec manager generates minimum rank draft and remembers live ranks for one step rollback`() {
        val respecDraft =
            RespecManager().createDraft(
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "vanguard",
                liveRanks = mapOf("power_strike" to 4, "charge" to 2),
                minimumRanks = mapOf("power_strike" to 1, "charge" to 1),
            )

        assertEquals(TalentTreeOwnerType.PROFESSION, respecDraft.ownerType)
        assertEquals(mapOf("power_strike" to 1, "charge" to 1), respecDraft.pendingRanks)
        assertEquals(mapOf("power_strike" to 4, "charge" to 2), respecDraft.previousPendingRanks)
    }

    @Test
    fun `phase three talent budget can branch across multiple advanced class nodes`() {
        var draft: TalentAllocationDraft? = null
        draft =
            TalentAllocationPlanner.applyRankIncrease(
                draft = draft,
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "berserker",
                talentId = "pursuit_drive",
                nextRank = 2,
            )
        draft =
            TalentAllocationPlanner.applyRankIncrease(
                draft = draft,
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "berserker",
                talentId = "savage_hew",
                nextRank = 3,
            )
        draft =
            TalentAllocationPlanner.applyRankIncrease(
                draft = draft,
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "berserker",
                talentId = "aftershock",
                nextRank = 2,
            )
        val preview =
            TalentAllocationPlanner.preview(
                liveRanks =
                    linkedMapOf(
                        "blood_rush" to 1,
                        "pursuit_drive" to 1,
                        "savage_hew" to 1,
                        "reckless_slam" to 1,
                        "aftershock" to 1,
                    ),
                minimumRanks =
                    linkedMapOf(
                        "blood_rush" to 1,
                        "pursuit_drive" to 1,
                        "savage_hew" to 1,
                        "reckless_slam" to 1,
                        "aftershock" to 1,
                    ),
                availablePoints = 19,
                draft = draft,
            )

        assertEquals(4, preview.spentPointsDelta)
        assertEquals(15, preview.remainingPoints)
        assertEquals(2, preview.effectiveRanks["pursuit_drive"])
        assertEquals(3, preview.effectiveRanks["savage_hew"])
        assertEquals(2, preview.effectiveRanks["aftershock"])
    }
}
