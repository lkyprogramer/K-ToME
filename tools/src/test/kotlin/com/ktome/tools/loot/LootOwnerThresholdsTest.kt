package com.ktome.tools.loot

import com.ktome.tools.verification.BaselineMode
import com.ktome.tools.verification.VerificationBaseline
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LootOwnerThresholdsTest {
    @Test
    fun `strict pair ceilings load from loot baseline metadata`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = 1,
                baselineId = "test-loot-baseline",
                domainId = "loot",
                mode = BaselineMode.BUDGET_THRESHOLD,
                metricDefinitionVersion = "phase4-owner-metrics-v1",
                metadata =
                    buildJsonObject {
                        put(
                            "strictPairCeilings",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("secretProfileId", "loot.greenwood_hidden_cache.secret")
                                        put("maxValue", 0.30)
                                    },
                                )
                            },
                        )
                    },
            )

        assertEquals(
            mapOf("loot.greenwood_hidden_cache.secret" to 0.30),
            strictSecretProfileMaxOverlapTargets(baseline),
        )
    }

    @Test
    fun `strict pair ceilings fall back to defaults when metadata is absent`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = 1,
                baselineId = "test-loot-baseline-defaults",
                domainId = "loot",
                mode = BaselineMode.BUDGET_THRESHOLD,
                metricDefinitionVersion = "phase4-owner-metrics-v1",
                metadata = JsonObject(emptyMap()),
            )

        assertEquals(
            0.40,
            strictSecretProfileMaxOverlapTargets(baseline).getValue("loot.deep_iron_smuggler_stash.secret"),
        )
    }
}
