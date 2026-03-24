package com.ktome.core.talent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RollbackTest {
    @Test
    fun `rollback reverts only the most recent draft step`() {
        val firstDraft =
            TalentAllocationPlanner.applyRankIncrease(
                draft = null,
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "arcanist",
                talentId = "blink",
                nextRank = 2,
            )
        val secondDraft =
            TalentAllocationPlanner.applyRankIncrease(
                draft = firstDraft,
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "arcanist",
                talentId = "mana_surge",
                nextRank = 2,
            )

        val rolledBack = requireNotNull(RollbackManager.rollback(secondDraft))

        assertEquals(mapOf("blink" to 2), rolledBack.pendingRanks)
        assertNull(rolledBack.previousPendingRanks)
    }

    @Test
    fun `rollback clears first draft step back to no preview`() {
        val firstDraft =
            TalentAllocationPlanner.applyRankIncrease(
                draft = null,
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "arcanist",
                talentId = "blink",
                nextRank = 2,
            )

        assertNull(RollbackManager.rollback(firstDraft))
    }
}
