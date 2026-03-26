package com.ktome.game.harness

import com.ktome.core.combat.CombatRuleset
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.save.SaveSnapshot

object HarnessMetadata {
    val BUILD_ID: String = SaveSnapshot.DEFAULT_BUILD_METADATA
    val PHASE_ID: String = CombatRuleset.PHASE_ID
    val RULESET_VERSION: String = CombatRuleset.RULESET_VERSION
    val TRACE_SCHEMA_VERSION: String = CombatRuleset.TRACE_SCHEMA_VERSION
    val PROFILE_ID: String = AvailabilityContext.DEV_LAB.name
    const val DEFAULT_CORPUS_ID: String = "HEADLESS"
    const val LONG_RUN_SMOKE_CORPUS_ID: String = "LONG_RUN_SMOKE"
    const val LONG_RUN_FULL_CORPUS_ID: String = "LONG_RUN_FULL"
}
