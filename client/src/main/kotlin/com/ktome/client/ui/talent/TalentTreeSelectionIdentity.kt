package com.ktome.client.ui.talent

import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.talent.TalentTreeOwnerType

data class TalentTreeSelectionIdentity(
    val talentId: String,
    val treeId: String,
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
)

internal fun TalentTreeNodeSnapshot.toTalentTreeSelectionIdentity(): TalentTreeSelectionIdentity =
    TalentTreeSelectionIdentity(
        talentId = talentId,
        treeId = treeId,
        ownerType = enumValueOf(ownerType),
        treeOwnerId = treeOwnerId,
    )

internal fun TalentAssignTreeRowModel.toTalentTreeSelectionIdentity(): TalentTreeSelectionIdentity =
    TalentTreeSelectionIdentity(
        talentId = talentId,
        treeId = treeId,
        ownerType = ownerType,
        treeOwnerId = treeOwnerId,
    )
