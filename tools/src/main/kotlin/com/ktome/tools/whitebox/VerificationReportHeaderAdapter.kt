package com.ktome.tools.whitebox

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.whitebox.ContractVersionStamp
import com.ktome.core.harness.whitebox.VerificationReportHeader

internal fun HarnessReportHeader.toVerificationReportHeader(corpusId: String): VerificationReportHeader =
    VerificationReportHeader(
        harnessId = harnessId,
        phaseId = phaseId,
        buildId = buildId,
        locale = locale,
        corpusId = corpusId,
        timestamp = timestamp,
        activePackIds = activePackIds.map { packId -> packId.value },
        activePackManifestVersions = activePackManifestVersions.mapKeys { (packId, _) -> packId.value },
        contractVersions =
            listOf(
                ContractVersionStamp(contractId = "contentSchema", version = contentSchemaVersion.toString()),
                ContractVersionStamp(contractId = "topologyFingerprint", version = topologyFingerprintVersion.toString()),
                ContractVersionStamp(contractId = "rewardLedger", version = rewardLedgerVersion.toString()),
                ContractVersionStamp(contractId = "lootFormula", version = lootFormulaVersion.toString()),
                ContractVersionStamp(contractId = "specialTierEligibility", version = specialTierEligibilityVersion.toString()),
                ContractVersionStamp(contractId = "searchRule", version = searchRuleVersion.toString()),
                ContractVersionStamp(contractId = "secretRule", version = secretRuleVersion.toString()),
                ContractVersionStamp(contractId = "overlayContract", version = overlayContractVersion.toString()),
            ),
        seedList = seedList,
    )
