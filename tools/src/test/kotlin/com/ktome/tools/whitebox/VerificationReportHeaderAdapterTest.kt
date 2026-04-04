package com.ktome.tools.whitebox

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.phase.PackId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerificationReportHeaderAdapterTest {
    @Test
    fun `adapter preserves packs manifest versions and contract versions`() {
        val header =
            HarnessReportHeader(
                harnessId = "solvabilityHarness",
                phaseId = "P4",
                buildId = "build-42",
                locale = "zh-CN",
                contentSchemaVersion = 2,
                topologyFingerprintVersion = 3,
                rewardLedgerVersion = 4,
                lootFormulaVersion = 5,
                specialTierEligibilityVersion = 6,
                searchRuleVersion = 7,
                secretRuleVersion = 8,
                overlayContractVersion = 9,
                activePackIds = listOf(PackId("ktome.base"), PackId("ktome.sample")),
                activePackManifestVersions =
                    linkedMapOf(
                        PackId("ktome.base") to "1.0.0",
                        PackId("ktome.sample") to "1.2.0",
                    ),
                timestamp = "2026-04-04T00:00:00Z",
                seedList = listOf(101L, 202L),
            )

        val adapted = header.toVerificationReportHeader(corpusId = "P4_PR03_SOLVABILITY_WHITEBOX")

        assertEquals(listOf("ktome.base", "ktome.sample"), adapted.activePackIds)
        assertEquals("1.2.0", adapted.activePackManifestVersions.getValue("ktome.sample"))
        assertEquals("P4_PR03_SOLVABILITY_WHITEBOX", adapted.corpusId)
        assertEquals(listOf(101L, 202L), adapted.seedList)
        assertEquals(
            mapOf(
                "contentSchema" to "2",
                "topologyFingerprint" to "3",
                "rewardLedger" to "4",
                "lootFormula" to "5",
                "specialTierEligibility" to "6",
                "searchRule" to "7",
                "secretRule" to "8",
                "overlayContract" to "9",
            ),
            adapted.contractVersions.associate { stamp -> stamp.contractId to stamp.version },
        )
    }
}
