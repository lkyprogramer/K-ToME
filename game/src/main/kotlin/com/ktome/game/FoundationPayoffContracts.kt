package com.ktome.game

data class FoundationBreakpointPayoffContract(
    val professionId: String,
    val talentId: String,
    val breakpointRank: Int,
    val descriptionAddendumKey: String,
    val requiredKeywords: Set<String>,
    val summaryEffectKind: String,
    val payoffStatusId: String? = null,
    val payoffResourceTypeId: String? = null,
    val primaryForProfession: Boolean = false,
) {
    val previewRank: Int
        get() = breakpointRank - 1
}

val FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS: List<FoundationBreakpointPayoffContract> =
    listOf(
        FoundationBreakpointPayoffContract(
            professionId = "vanguard",
            talentId = "guard_stance",
            breakpointRank = 4,
            descriptionAddendumKey = "talent.breakpoint.vanguard.guard_stance.hold_line",
            requiredKeywords = setOf("guard", "hold_line"),
            summaryEffectKind = "apply_status:guard",
            payoffStatusId = "GUARD",
            primaryForProfession = true,
        ),
        FoundationBreakpointPayoffContract(
            professionId = "vanguard",
            talentId = "taunt",
            breakpointRank = 4,
            descriptionAddendumKey = "talent.breakpoint.vanguard.taunt.guard",
            requiredKeywords = setOf("taunt", "guard"),
            summaryEffectKind = "apply_status:guard",
            payoffStatusId = "GUARD",
        ),
        FoundationBreakpointPayoffContract(
            professionId = "arcanist",
            talentId = "blink",
            breakpointRank = 4,
            descriptionAddendumKey = "talent.breakpoint.arcanist.blink.mana_tempo",
            requiredKeywords = setOf("teleport", "mana_tempo"),
            summaryEffectKind = "resource_restore:mana",
            payoffResourceTypeId = "MANA",
            primaryForProfession = true,
        ),
        FoundationBreakpointPayoffContract(
            professionId = "rogue",
            talentId = "shadowstep",
            breakpointRank = 2,
            descriptionAddendumKey = "talent.breakpoint.rogue.shadowstep.marked",
            requiredKeywords = setOf("movement", "marked"),
            summaryEffectKind = "apply_status:marked",
            payoffStatusId = "MARKED",
            primaryForProfession = true,
        ),
        FoundationBreakpointPayoffContract(
            professionId = "rogue",
            talentId = "shadow_bind",
            breakpointRank = 4,
            descriptionAddendumKey = "talent.breakpoint.rogue.shadow_bind.marked",
            requiredKeywords = setOf("control", "marked"),
            summaryEffectKind = "apply_status:marked",
            payoffStatusId = "MARKED",
        ),
        FoundationBreakpointPayoffContract(
            professionId = "templar",
            talentId = "holy_mark",
            breakpointRank = 3,
            descriptionAddendumKey = "talent.breakpoint.templar.holy_mark.bane",
            requiredKeywords = setOf("mark", "bane"),
            summaryEffectKind = "apply_status:bane",
            payoffStatusId = "BANE",
            primaryForProfession = true,
        ),
        FoundationBreakpointPayoffContract(
            professionId = "templar",
            talentId = "purify",
            breakpointRank = 4,
            descriptionAddendumKey = "talent.breakpoint.templar.purify.holy_shield",
            requiredKeywords = setOf("cleanse", "shield"),
            summaryEffectKind = "apply_status:holy_shield_buff",
            payoffStatusId = "HOLY_SHIELD_BUFF",
        ),
    )

val FOUNDATION_PRIMARY_BREAKPOINT_PAYOFF_TALENTS: Map<String, String> =
    FOUNDATION_BREAKPOINT_PAYOFF_CONTRACTS
        .filter(FoundationBreakpointPayoffContract::primaryForProfession)
        .associate { contract -> contract.professionId to contract.talentId }

val FOUNDATION_SYNERGY_AFFIX_IDS: Map<String, Set<String>> =
    mapOf(
        "vanguard" to setOf("of_piercing"),
        "arcanist" to setOf("of_flames", "of_frost"),
        "rogue" to setOf("of_precision", "of_shadow"),
        "templar" to setOf("of_smite"),
    )
