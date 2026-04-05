package com.ktome.core.loot

enum class AffixCostBand(
    val cost: Int,
) {
    TRIVIAL(1),
    MINOR(3),
    MEDIUM(6),
    MAJOR(10),
    SIGNATURE(14),
}

data class AffixCost(
    val affixId: String,
    val cost: Int,
    val affixFamily: String,
    val exclusiveGroup: String? = null,
    val slotTags: Set<String>,
    val phase: String,
) {
    init {
        require(affixId.isNotBlank()) { "AffixCost.affixId must not be blank." }
        require(cost > 0) { "AffixCost.cost must be positive." }
        require(affixFamily.isNotBlank()) { "AffixCost.affixFamily must not be blank." }
        require(exclusiveGroup?.isNotBlank() != false) {
            "AffixCost.exclusiveGroup must not be blank when present."
        }
        require(slotTags.isNotEmpty()) { "AffixCost.slotTags must not be empty." }
        require(slotTags.all(String::isNotBlank)) { "AffixCost.slotTags must not contain blank tags." }
        require(phase.isNotBlank()) { "AffixCost.phase must not be blank." }
    }
}

fun AffixCost.band(): AffixCostBand =
    AffixCostBand.entries.firstOrNull { band -> band.cost == cost }
        ?: error("Unsupported affix cost value $cost for '$affixId'.")

fun Int.toAffixCostBand(): AffixCostBand =
    AffixCostBand.entries.firstOrNull { band -> band.cost == this }
        ?: error("Unsupported affix cost value $this.")
