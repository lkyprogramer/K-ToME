package com.ktome.client

import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.talent.TalentLoadout
import com.ktome.game.FoundationGameSession

internal fun automationWorld(session: FoundationGameSession): World {
    val field = session.javaClass.getDeclaredField("world")
    field.isAccessible = true
    return field.get(session) as World
}

internal fun installReserveTalent(
    session: FoundationGameSession,
    talentId: String,
    fixtureLabel: String,
) {
    val loadout = requireNotNull(automationWorld(session).get<TalentLoadout>(session.playerId)) {
        "Expected talent loadout for $fixtureLabel."
    }
    loadout.talentLevels.putIfAbsent(talentId, 1)
    invalidateSessionRenderSnapshot(session)
    requireNotNull(session.renderSnapshot().uiState.reserveTalents.firstOrNull { talent -> talent.talentId == talentId }) {
        "Expected reserve talent $talentId to be visible for $fixtureLabel."
    }
}

internal fun installProfessionReserveTalent(session: FoundationGameSession) {
    val reserveTalentId =
        when (session.config.playerProfessionId) {
            "vanguard" -> "charge"
            "arcanist" -> "mana_surge"
            "rogue" -> "shadowstep"
            "templar" -> "judgment_hammer"
            else -> error("Unsupported profession ${session.config.playerProfessionId} for smoke loadout fixture.")
        }
    installReserveTalent(session, reserveTalentId, fixtureLabel = "smoke reserve fixture")
}

internal fun invalidateSessionRenderSnapshot(session: FoundationGameSession) {
    val method =
        session.javaClass.declaredMethods.firstOrNull { declared ->
            declared.name == "invalidateRenderSnapshot" ||
                declared.name.startsWith("invalidateRenderSnapshot-") ||
                declared.name.startsWith("invalidateRenderSnapshot$")
        }
            ?: error("No declared invalidateRenderSnapshot helper found on ${session.javaClass.name}.")
    method.isAccessible = true
    method.invoke(session)
}
