package com.ktome.game

import com.ktome.core.combat.DiminishingReturns
import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.get
import com.ktome.core.item.PassiveEffect
import com.ktome.core.item.PassiveSource
import com.ktome.core.item.PassiveSourceKind
import com.ktome.core.item.StatModifier
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.PassiveDetailDeltaLineSnapshot
import com.ktome.core.snapshot.PassiveDetailLineKindSnapshot
import com.ktome.core.snapshot.PassiveDetailLineSnapshot
import com.ktome.core.talent.ActionCost
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.talent.TalentCategory
import com.ktome.core.talent.TalentTargetingType
import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PlayableProfessionPassiveTalentTest {
    @TempDir
    lateinit var tempDir: Path

    private val passiveTalentIds =
        listOf(
            "unyielding",
            "arcane_overload",
            "killer_instinct",
            "devotion",
            "pain_fuel",
            "balance_point",
            "bulwark_march",
            "mana_surge",
            "deathblow",
            "beacon_of_zeal",
            "last_stand",
            "flux_anchor",
        )

    @Test
    fun `PR04-01 playable profession passive talents use passive-only runtime contract`() {
        val talents = DataLoader(GameLocale.EN_US).loadTalentDefinitions().associateBy { talent -> talent.id }

        passiveTalentIds.forEach { talentId ->
            val talent = requireNotNull(talents[talentId]) { "Missing talent $talentId" }
            assertEquals(TalentCategory.PASSIVE, talent.category, talentId)
            assertEquals(0, talent.cooldown, talentId)
            assertEquals(ActionCost.INSTANT, talent.actionCost, talentId)
            assertEquals(TalentTargetingType.SELF, talent.targetingDef.type, talentId)
            assertTrue(talent.resourceCosts.isEmpty(), talentId)
            assertTrue(talent.callbacks.isEmpty(), talentId)
            assertEquals(null, talent.telegraphRef, talentId)
            assertTrue(talent.levelEffects.values.all { effect -> effect.effectOps.isEmpty() }, talentId)
            assertTrue(talent.levelEffects.values.all { effect -> effect.passiveEffects.isNotEmpty() }, talentId)
        }
    }

    @Test
    fun `PR04-01 passive talent rank tables match the design document`() {
        val talents = DataLoader(GameLocale.EN_US).loadTalentDefinitions().associateBy { talent -> talent.id }

        val unyieldingRank5 = requireNotNull(talents["unyielding"]).levelEffects.getValue(5).passiveEffects
        val unyieldingStats = unyieldingRank5.filterIsInstance<PassiveEffect.StatModifierEffect>().single().statModifier
        assertEquals(40, unyieldingStats.maxHp)
        assertEquals(5, unyieldingStats.defense)
        assertEquals(
            6,
            unyieldingRank5.filterIsInstance<PassiveEffect.ResistanceBonus>().single { effect -> effect.damageType == DamageType.PHYSICAL }.amount,
        )

        val manaSurgeRank1 = requireNotNull(talents["mana_surge"]).levelEffects.getValue(1).passiveEffects
        assertEquals(3, manaSurgeRank1.filterIsInstance<PassiveEffect.OnKillResourceRestore>().single().amount)
        assertEquals(ResourceType.MANA, manaSurgeRank1.filterIsInstance<PassiveEffect.OnKillResourceRestore>().single().resourceType)
        assertEquals(
            setOf(DamageType.FIRE, DamageType.COLD, DamageType.LIGHTNING),
            manaSurgeRank1.filterIsInstance<PassiveEffect.DamageTypeBonus>().mapTo(linkedSetOf()) { effect -> effect.type },
        )
        assertTrue(manaSurgeRank1.filterIsInstance<PassiveEffect.DamageTypeBonus>().all { effect -> effect.bonusPercent == 0.03 })

        val deathblowRank5 = requireNotNull(talents["deathblow"]).levelEffects.getValue(5).passiveEffects
        assertEquals(0.25, deathblowRank5.filterIsInstance<PassiveEffect.DamageVsStatus>().single().bonusPercent, 0.0001)
        assertEquals(12, deathblowRank5.filterIsInstance<PassiveEffect.OnKillResourceRestore>().single().amount)

        val painFuelRank1 = requireNotNull(talents["pain_fuel"]).levelEffects.getValue(1).passiveEffects
        assertFalse(painFuelRank1.any { effect -> effect is PassiveEffect.HpRegenPerTurn })
        val painFuelStats = painFuelRank1.filterIsInstance<PassiveEffect.StatModifierEffect>().single().statModifier
        assertEquals(0.05, painFuelStats.attackMultiplierBonus, 0.0001)
        assertEquals(0.4, painFuelStats.hpRegen, 0.0001)
    }

    @Test
    fun `PR04-01 static passive detail snapshot exposes typed current and next lines`() {
        val session = newProfessionSession("vanguard", "static-passive-detail")
        val node = passiveNode(session, "unyielding")
        val passiveDetail = requireNotNull(node.passiveDetail)

        assertEquals(
            listOf(
                PassiveDetailLineKindSnapshot.STAT_MODIFIER,
                PassiveDetailLineKindSnapshot.STAT_MODIFIER,
                PassiveDetailLineKindSnapshot.RESISTANCE_BONUS,
            ),
            passiveDetail.currentLines.map(PassiveDetailLineSnapshot::lineKind),
        )
        assertEquals("maxHp", passiveDetail.currentLines[0].textArg("statId"))
        assertEquals("+10", passiveDetail.currentLines[0].textArg("value"))
        assertEquals("ui.talent.passive.detail.kind.stat_modifier", passiveDetail.currentLines[0].labelKey)
        assertEquals("ui.talent.passive.detail.stat_modifier", passiveDetail.currentLines[0].valueToken.key)
        assertEquals(listOf("statId", "value"), passiveDetail.currentLines[0].tokenArgNames())
        assertEquals("ui.hud.hp.short", passiveDetail.currentLines[0].tokenValueKey("statId"))
        assertEquals("+10", passiveDetail.currentLines[0].tokenValue("value"))
        assertEquals("defense", passiveDetail.currentLines[1].textArg("statId"))
        assertEquals("+1", passiveDetail.currentLines[1].textArg("value"))
        assertEquals("PHYSICAL", passiveDetail.currentLines[2].textArg("damageType"))
        assertEquals("damage_type.physical.name", passiveDetail.currentLines[2].tokenValueKey("damageType"))
        assertEquals("+1", passiveDetail.currentLines[2].textArg("value"))

        val nextMaxHp = passiveDetail.nextLines.first { line -> line.textArg("statId") == "maxHp" }
        assertEquals(PassiveDetailLineKindSnapshot.STAT_MODIFIER, nextMaxHp.lineKind)
        assertEquals("ui.talent.passive.detail.stat_modifier.delta", nextMaxHp.valueToken.key)
        assertEquals(listOf("statId", "value", "before", "after"), nextMaxHp.tokenArgNames())
        assertEquals("+10", nextMaxHp.textArg("before"))
        assertEquals("+16", nextMaxHp.textArg("after"))
        assertEquals("+10", nextMaxHp.tokenValue("before"))
        assertEquals("+16", nextMaxHp.tokenValue("after"))
    }

    @Test
    fun `PR04-01 trigger passive detail snapshot exposes typed resource and damage args`() {
        val session = newProfessionSession("arcanist", "trigger-passive-detail")
        val passiveDetail = requireNotNull(passiveNode(session, "mana_surge").passiveDetail)

        assertEquals(
            listOf(
                PassiveDetailLineKindSnapshot.ON_KILL_RESOURCE_RESTORE,
                PassiveDetailLineKindSnapshot.DAMAGE_TYPE_BONUS,
                PassiveDetailLineKindSnapshot.DAMAGE_TYPE_BONUS,
                PassiveDetailLineKindSnapshot.DAMAGE_TYPE_BONUS,
            ),
            passiveDetail.currentLines.map(PassiveDetailLineSnapshot::lineKind),
        )
        val restore = passiveDetail.currentLines.first()
        assertEquals("MANA", restore.textArg("resourceType"))
        assertEquals("ui.hud.mana.short", restore.tokenValueKey("resourceType"))
        assertEquals(3, restore.intArg("amount"))
        assertEquals("3", restore.tokenValue("amount"))
        assertEquals("+3", restore.textArg("value"))
        assertEquals(listOf("amount", "resourceType"), restore.tokenArgNames())

        assertEquals(
            listOf("FIRE", "COLD", "LIGHTNING"),
            passiveDetail.currentLines.drop(1).map { line -> line.textArg("damageType") },
        )
        assertEquals(
            listOf("damage_type.fire.name", "damage_type.cold.name", "damage_type.lightning.name"),
            passiveDetail.currentLines.drop(1).map { line -> line.tokenValueKey("damageType") },
        )
        assertEquals(listOf("+3%", "+3%", "+3%"), passiveDetail.currentLines.drop(1).map { line -> line.textArg("percent") })
    }

    @Test
    fun `PR04-01 conditional passive detail snapshot renders one row per documented stat`() {
        val session = newProfessionSession("vanguard", "conditional-passive-detail")
        val passiveDetail = requireNotNull(passiveNode(session, "bulwark_march").passiveDetail)

        assertEquals(
            listOf(
                PassiveDetailLineKindSnapshot.CONDITIONAL_STAT_BONUS,
                PassiveDetailLineKindSnapshot.CONDITIONAL_STAT_BONUS,
                PassiveDetailLineKindSnapshot.DAMAGE_VS_STATUS,
            ),
            passiveDetail.currentLines.map(PassiveDetailLineSnapshot::lineKind),
        )
        assertEquals(listOf("condition", "statusId", "statId", "value"), passiveDetail.currentLines[0].tokenArgNames())
        assertEquals("SELF_HAS_STATUS", passiveDetail.currentLines[0].textArg("condition"))
        assertEquals("ui.inspect.passive.condition.self_has_status", passiveDetail.currentLines[0].tokenValueKey("condition"))
        assertEquals("GUARD_STANCE_BUFF", passiveDetail.currentLines[0].statusArg("statusId"))
        assertEquals("status.guard_stance_buff", passiveDetail.currentLines[0].tokenValueKey("statusId"))
        assertEquals("defense", passiveDetail.currentLines[0].textArg("statId"))
        assertEquals("+2", passiveDetail.currentLines[0].textArg("value"))
        assertEquals("speed", passiveDetail.currentLines[1].textArg("statId"))
        assertEquals("+1", passiveDetail.currentLines[1].textArg("value"))
        assertEquals("TAUNT", passiveDetail.currentLines[2].statusArg("statusId"))
        assertEquals("+4%", passiveDetail.currentLines[2].textArg("percent"))
    }

    @Test
    fun `PR04-01 official effective stat passives render effective display values`() {
        val arcanist = newProfessionSession("arcanist", "official-cast-speed-effective-detail")
        setTalentRank(arcanist, talentId = "arcane_overload", rank = 5)
        assertEquals(
            signedDecimalForTest(DiminishingReturns.effectiveCastSpeed(5)),
            passiveNode(arcanist, "arcane_overload").passiveDetailLine("castSpeedRating").textArg("value"),
        )

        val spellblade = newProfessionSession("spellblade", "official-balance-effective-detail")
        setTalentRank(spellblade, talentId = "balance_point", rank = 5)
        assertEquals(
            signedDecimalForTest(DiminishingReturns.effectiveCastSpeed(8)),
            passiveNode(spellblade, "balance_point").passiveDetailLine("castSpeedRating").textArg("value"),
        )

        val berserker = newProfessionSession("berserker", "official-hp-regen-effective-detail")
        setTalentRank(berserker, talentId = "pain_fuel", rank = 5)
        assertEquals(
            signedDecimalForTest(DiminishingReturns.effectiveHpRegen(1.2)),
            passiveNode(berserker, "pain_fuel").passiveDetailLine("hpRegen").textArg("value"),
        )
    }

    @Test
    fun `PR04-01 passive detail projector applies diminishing returns before formatting`() {
        val session = newProfessionSession("arcanist", "synthetic-effective-detail-route")
        val projectedValues =
            passiveStatDetailValues(
                session = session,
                modifier = StatModifier(castSpeedRating = 8, hpRegen = 7.0),
            )

        assertEquals(signedDecimalForTest(DiminishingReturns.effectiveCastSpeed(8)), projectedValues.getValue("castSpeedRating"))
        assertEquals(signedDecimalForTest(DiminishingReturns.effectiveHpRegen(7.0)), projectedValues.getValue("hpRegen"))
        assertFalse(projectedValues.containsValue("+8"))
        assertFalse(projectedValues.containsValue("+7"))
    }

    @Test
    fun `PR04-01 talent passive stable key fails fast when rank is missing`() {
        val session = newProfessionSession("arcanist", "talent-passive-rank-required")
        val method =
            FoundationGameSession::class.java
                .getDeclaredMethod("passiveStableKey", PassiveSource::class.java, String::class.java)
                .apply { isAccessible = true }
        val source =
            PassiveSource(
                kind = PassiveSourceKind.TALENT,
                sourceId = "arcane_overload",
                sourceTemplateId = "arcane_overload",
                passive = PassiveEffect.StatModifierEffect(StatModifier(castSpeedRating = 1)),
            )

        val error =
            assertThrows(InvocationTargetException::class.java) {
                method.invoke(session, source, "stat_modifier")
            }
        assertTrue(error.cause is IllegalArgumentException)
        assertTrue(requireNotNull(error.cause?.message).contains("must carry talentRank"))
    }

    @Test
    fun `PR04-01 passive talents cannot be equipped into active talent slots`() {
        val session = newProfessionSession("templar", "passive-active-slot-rejection")
        val world = ClassFormalizationTestSupport.runtimeWorld(session)
        requireNotNull(world.get<Experience>(session.playerId)).unspentTalentPoints = 1

        assertTrue(session.perform(PlayerCommand.AssignTalent("devotion")))
        assertTrue(session.perform(PlayerCommand.ConfirmTalentDraft))
        assertEquals(1, requireNotNull(world.get<TalentLoadout>(session.playerId)).levelOf("devotion"))
        assertFalse(session.perform(PlayerCommand.EquipTalentToSlot(slot = 1, talentId = "devotion")))
        assertTrue(session.talentSlots().none { slot -> slot.talentId == "devotion" })
    }

    @Test
    fun `PR04-01 learned passive talents do not enter reserve loadout`() {
        val session = newProfessionSession("arcanist", "passive-reserve-suppression")

        setTalentRank(session, talentId = "mana_surge", rank = 1)

        assertTrue(session.reserveTalentSlots().none { talent -> talent.talentId == "mana_surge" })
        assertTrue(session.renderSnapshot().uiState.reserveTalents.none { talent -> talent.talentId == "mana_surge" })
        assertEquals(1, passiveNode(session, "mana_surge").rank)
    }

    private fun newProfessionSession(
        professionId: String,
        saveName: String,
    ): FoundationGameSession =
        GameModule.newFoundationSession(
            config = FoundationGameConfig(seed = 20260518L, zoneId = "shattered_outpost", playerProfessionId = professionId),
            saveManager = SaveManager(tempDir.resolve(saveName)),
            availabilityContext = AvailabilityContext.WHITE_BOX,
        )

    private fun passiveNode(
        session: FoundationGameSession,
        talentId: String,
    ) = session.renderSnapshot().uiState.talentTrees.flatMap { tree -> tree.nodes }.single { node -> node.talentId == talentId }

    private fun setTalentRank(
        session: FoundationGameSession,
        talentId: String,
        rank: Int,
    ) {
        val world = ClassFormalizationTestSupport.runtimeWorld(session)
        requireNotNull(world.get<TalentLoadout>(session.playerId)).talentLevels[talentId] = rank
    }

    private fun passiveStatDetailValues(
        session: FoundationGameSession,
        modifier: StatModifier,
    ): Map<String, String> {
        val method =
            FoundationGameSession::class.java
                .getDeclaredMethod("passiveStatDetailValues", StatModifier::class.java)
                .apply { isAccessible = true }
        val values = method.invoke(session, modifier) as List<*>
        return values.associate { value ->
            requireNotNull(value)
            val statId =
                value.javaClass
                    .getDeclaredField("statId")
                    .apply { isAccessible = true }
                    .get(value) as String
            val displayValue =
                value.javaClass
                    .getDeclaredField("value")
                    .apply { isAccessible = true }
                    .get(value) as String
            statId to displayValue
        }
    }

    private fun com.ktome.core.snapshot.TalentTreeNodeSnapshot.passiveDetailLine(statId: String): PassiveDetailLineSnapshot =
        requireNotNull(passiveDetail)
            .currentLines
            .single { line -> line.diagnosticArgs["statId"] == statId }

    private fun PassiveDetailLineSnapshot.textArg(name: String): String = diagnosticArgs.getValue(name)

    private fun PassiveDetailDeltaLineSnapshot.textArg(name: String): String = diagnosticArgs.getValue(name)

    private fun PassiveDetailLineSnapshot.intArg(name: String): Int = diagnosticArgs.getValue(name).toInt()

    private fun PassiveDetailLineSnapshot.statusArg(name: String): String = diagnosticArgs.getValue(name)

    private fun PassiveDetailLineSnapshot.tokenArgNames(): List<String> = valueToken.arguments.map { argument -> argument.name }

    private fun PassiveDetailDeltaLineSnapshot.tokenArgNames(): List<String> = valueToken.arguments.map { argument -> argument.name }

    private fun PassiveDetailLineSnapshot.tokenValue(name: String): String? =
        valueToken.arguments.firstOrNull { argument -> argument.name == name }?.value

    private fun PassiveDetailDeltaLineSnapshot.tokenValue(name: String): String? =
        valueToken.arguments.firstOrNull { argument -> argument.name == name }?.value

    private fun PassiveDetailLineSnapshot.tokenValueKey(name: String): String? =
        valueToken.arguments.firstOrNull { argument -> argument.name == name }?.valueKey

    private fun signedDecimalForTest(value: Double): String {
        val normalized =
            if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                "%.1f".format(value)
            }
        return if (value > 0) "+$normalized" else normalized
    }
}
