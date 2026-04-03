package com.ktome.tools.mapgen

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.phase.Phase4ContractVersions
import com.ktome.core.save.SaveSnapshot
import com.ktome.game.i18n.GameLocale
import java.time.Instant

internal fun phase4HarnessHeader(
    harnessId: String,
    seedList: List<Long>,
    timestamp: String = Instant.now().toString(),
    locale: String = GameLocale.DEFAULT.id,
): HarnessReportHeader =
    HarnessReportHeader(
        harnessId = harnessId,
        phaseId = Phase4ContractVersions.PHASE_ID,
        buildId = SaveSnapshot.DEFAULT_BUILD_METADATA,
        locale = locale,
        contentSchemaVersion = Phase4ContractVersions.CONTENT_SCHEMA_VERSION,
        topologyFingerprintVersion = Phase4ContractVersions.TOPOLOGY_FINGERPRINT_VERSION,
        rewardLedgerVersion = Phase4ContractVersions.REWARD_LEDGER_VERSION,
        lootFormulaVersion = Phase4ContractVersions.LOOT_FORMULA_VERSION,
        specialTierEligibilityVersion = Phase4ContractVersions.SPECIAL_TIER_ELIGIBILITY_VERSION,
        searchRuleVersion = Phase4ContractVersions.SEARCH_RULE_VERSION,
        secretRuleVersion = Phase4ContractVersions.SECRET_RULE_VERSION,
        overlayContractVersion = Phase4ContractVersions.OVERLAY_CONTRACT_VERSION,
        activePackIds = emptyList(),
        activePackManifestVersions = emptyMap(),
        timestamp = timestamp,
        seedList = seedList,
    )
