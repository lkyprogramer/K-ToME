package com.ktome.core.talent

enum class KeywordSemanticType {
    OFFENSE,
    DEFENSE,
    CONTROL,
    MOBILITY,
    RESOURCE,
    TARGETING,
    UTILITY,
}

data class KeywordSemantic(
    val id: String,
    val type: KeywordSemanticType,
    val nameKey: String,
    val tooltipKey: String,
    val relatedKeywords: List<String> = emptyList(),
)

class KeywordRegistry(
    private val definitions: Map<String, KeywordSemantic>,
) {
    fun resolve(id: String): KeywordSemantic? = definitions[id]

    fun all(): List<KeywordSemantic> = definitions.values.sortedBy(KeywordSemantic::id)

    fun require(id: String): KeywordSemantic =
        requireNotNull(resolve(id)) { "Missing keyword semantic for '$id'." }

    fun resolveAll(ids: Iterable<String>): List<KeywordSemantic> =
        ids.map(::require)

    companion object {
        val CORE: KeywordRegistry =
            KeywordRegistry(
                definitions =
                    listOf(
                        KeywordSemantic("armor_break", KeywordSemanticType.OFFENSE, "keyword.armor_break.name", "keyword.armor_break.tooltip"),
                        KeywordSemantic("bleed", KeywordSemanticType.OFFENSE, "keyword.bleed.name", "keyword.bleed.tooltip"),
                        KeywordSemantic("burst", KeywordSemanticType.OFFENSE, "keyword.burst.name", "keyword.burst.tooltip"),
                        KeywordSemantic("burn", KeywordSemanticType.OFFENSE, "keyword.burn.name", "keyword.burn.tooltip"),
                        KeywordSemantic("bane", KeywordSemanticType.CONTROL, "keyword.bane.name", "keyword.bane.tooltip"),
                        KeywordSemantic("freeze", KeywordSemanticType.CONTROL, "keyword.freeze.name", "keyword.freeze.tooltip"),
                        KeywordSemantic("guard", KeywordSemanticType.DEFENSE, "keyword.guard.name", "keyword.guard.tooltip"),
                        KeywordSemantic("stun", KeywordSemanticType.CONTROL, "keyword.stun.name", "keyword.stun.tooltip"),
                        KeywordSemantic("shield", KeywordSemanticType.DEFENSE, "keyword.shield.name", "keyword.shield.tooltip"),
                        KeywordSemantic("hold_line", KeywordSemanticType.DEFENSE, "keyword.hold_line.name", "keyword.hold_line.tooltip"),
                        KeywordSemantic("penetration", KeywordSemanticType.OFFENSE, "keyword.penetration.name", "keyword.penetration.tooltip"),
                        KeywordSemantic(
                            "diminishing_returns",
                            KeywordSemanticType.UTILITY,
                            "keyword.diminishing_returns.name",
                            "keyword.diminishing_returns.tooltip",
                        ),
                        KeywordSemantic("power_save", KeywordSemanticType.UTILITY, "keyword.power_save.name", "keyword.power_save.tooltip"),
                        KeywordSemantic("telegraph", KeywordSemanticType.UTILITY, "keyword.telegraph.name", "keyword.telegraph.tooltip"),
                        KeywordSemantic("crit", KeywordSemanticType.OFFENSE, "keyword.crit.name", "keyword.crit.tooltip"),
                        KeywordSemantic("dot", KeywordSemanticType.OFFENSE, "keyword.dot.name", "keyword.dot.tooltip"),
                        KeywordSemantic("sustain", KeywordSemanticType.UTILITY, "keyword.sustain.name", "keyword.sustain.tooltip"),
                        KeywordSemantic("cleanse", KeywordSemanticType.UTILITY, "keyword.cleanse.name", "keyword.cleanse.tooltip"),
                        KeywordSemantic("dispel", KeywordSemanticType.UTILITY, "keyword.dispel.name", "keyword.dispel.tooltip"),
                        KeywordSemantic("damage", KeywordSemanticType.OFFENSE, "keyword.damage.name", "keyword.damage.tooltip"),
                        KeywordSemantic("movement", KeywordSemanticType.MOBILITY, "keyword.movement.name", "keyword.movement.tooltip"),
                        KeywordSemantic("single_target", KeywordSemanticType.TARGETING, "keyword.single_target.name", "keyword.single_target.tooltip"),
                        KeywordSemantic("area", KeywordSemanticType.TARGETING, "keyword.area.name", "keyword.area.tooltip"),
                        KeywordSemantic("boss", KeywordSemanticType.OFFENSE, "keyword.boss.name", "keyword.boss.tooltip"),
                        KeywordSemantic("buff", KeywordSemanticType.DEFENSE, "keyword.buff.name", "keyword.buff.tooltip"),
                        KeywordSemantic("cleave", KeywordSemanticType.OFFENSE, "keyword.cleave.name", "keyword.cleave.tooltip"),
                        KeywordSemantic("control", KeywordSemanticType.CONTROL, "keyword.control.name", "keyword.control.tooltip"),
                        KeywordSemantic("counter", KeywordSemanticType.DEFENSE, "keyword.counter.name", "keyword.counter.tooltip"),
                        KeywordSemantic("curse", KeywordSemanticType.CONTROL, "keyword.curse.name", "keyword.curse.tooltip"),
                        KeywordSemantic("debuff", KeywordSemanticType.CONTROL, "keyword.debuff.name", "keyword.debuff.tooltip"),
                        KeywordSemantic("defense", KeywordSemanticType.DEFENSE, "keyword.defense.name", "keyword.defense.tooltip"),
                        KeywordSemantic("finisher", KeywordSemanticType.OFFENSE, "keyword.finisher.name", "keyword.finisher.tooltip"),
                        KeywordSemantic("heal", KeywordSemanticType.DEFENSE, "keyword.heal.name", "keyword.heal.tooltip"),
                        KeywordSemantic("holy", KeywordSemanticType.UTILITY, "keyword.holy.name", "keyword.holy.tooltip"),
                        KeywordSemantic("hybrid", KeywordSemanticType.UTILITY, "keyword.hybrid.name", "keyword.hybrid.tooltip"),
                        KeywordSemantic("knockback", KeywordSemanticType.CONTROL, "keyword.knockback.name", "keyword.knockback.tooltip"),
                        KeywordSemantic("mark", KeywordSemanticType.CONTROL, "keyword.mark.name", "keyword.mark.tooltip"),
                        KeywordSemantic("marked", KeywordSemanticType.CONTROL, "keyword.marked.name", "keyword.marked.tooltip"),
                        KeywordSemantic("mana_tempo", KeywordSemanticType.RESOURCE, "keyword.mana_tempo.name", "keyword.mana_tempo.tooltip"),
                        KeywordSemantic("mobility", KeywordSemanticType.MOBILITY, "keyword.mobility.name", "keyword.mobility.tooltip"),
                        KeywordSemantic("panic", KeywordSemanticType.DEFENSE, "keyword.panic.name", "keyword.panic.tooltip"),
                        KeywordSemantic("ranged", KeywordSemanticType.TARGETING, "keyword.ranged.name", "keyword.ranged.tooltip"),
                        KeywordSemantic("reposition", KeywordSemanticType.MOBILITY, "keyword.reposition.name", "keyword.reposition.tooltip"),
                        KeywordSemantic("resource", KeywordSemanticType.RESOURCE, "keyword.resource.name", "keyword.resource.tooltip"),
                        KeywordSemantic("slow", KeywordSemanticType.CONTROL, "keyword.slow.name", "keyword.slow.tooltip"),
                        KeywordSemantic("stealth", KeywordSemanticType.MOBILITY, "keyword.stealth.name", "keyword.stealth.tooltip"),
                        KeywordSemantic("taunt", KeywordSemanticType.CONTROL, "keyword.taunt.name", "keyword.taunt.tooltip"),
                        KeywordSemantic("teleport", KeywordSemanticType.MOBILITY, "keyword.teleport.name", "keyword.teleport.tooltip"),
                    ).associateBy(KeywordSemantic::id),
            )
    }
}
