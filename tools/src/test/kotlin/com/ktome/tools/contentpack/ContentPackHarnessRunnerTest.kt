package com.ktome.tools.contentpack

import com.ktome.core.phase.PackId
import com.ktome.game.contentpack.ContentPackFixtureCatalog
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ContentPackHarnessRunnerTest {
    @Test
    @Tag("contentPackHarness")
    fun `content-pack harness sidecar lookup fails fast with pack id`() {
        val exception =
            assertThrows<IllegalStateException> {
                ContentPackFixtureCatalog.harnessSpec(PackId("fixture.missing_sidecar"))
            }

        assertTrue(exception.message.orEmpty().contains("packId=fixture.missing_sidecar"), exception.message)
        assertTrue(exception.message.orEmpty().contains("tools/src/main/resources/fixtures/content-packs/fixture.missing_sidecar.yaml"), exception.message)
    }

    @Test
    @Tag("contentPackHarness")
    fun `content-pack harness writes fixed reports and validates runtime plus failure fixtures`() {
        val run = ContentPackHarnessRunner.run()

        assertEquals(13, run.totalCases)
        assertEquals(0, run.failureCount, "contentPackHarness recorded failures; inspect ${run.summaryPath}")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.runsPath), "Expected runs report at ${run.runsPath}")

        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val cases = payload.getValue("cases").jsonArray
        val harnessSpec = ContentPackFixtureCatalog.harnessSpec(ContentPackFixtureCatalog.samplePackId)
        val precedenceCase =
            cases.first { element -> element.jsonObject.getValue("fixtureId").jsonPrimitive.content == "precedence_fixture" }.jsonObject
        val officialCase =
            cases.first { element -> element.jsonObject.getValue("fixtureId").jsonPrimitive.content == "official_sample_pack" }.jsonObject
        val splitBiasCase =
            cases.first { element -> element.jsonObject.getValue("fixtureId").jsonPrimitive.content == "split_bias_fixture" }.jsonObject
        val legacyRejectCase =
            cases.first { element ->
                element.jsonObject.getValue("fixtureId").jsonPrimitive.content == "legacy_v2_loot_profile_rejected"
            }.jsonObject

        assertEquals("PASS", summary.getValue("verdict").jsonPrimitive.content)
        assertEquals("13", summary.getValue("totalCases").jsonPrimitive.content)
        assertEquals("0", summary.getValue("failureCount").jsonPrimitive.content)
        assertEquals("4", summary.getValue("successfulRuntimeCaseCount").jsonPrimitive.content)
        assertEquals("9", summary.getValue("expectedFailureCaseCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("diagnosticMismatchCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("localeResolutionFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("visualResolutionFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("audioResolutionFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("precedenceFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("resourceContractFailureCount").jsonPrimitive.content)
        assertEquals("0", summary.getValue("generatedTemplateFailureCount").jsonPrimitive.content)
        assertEquals("1.0", summary.getValue("samplePackContentPlayerVisibilityRate.reportOnly").jsonPrimitive.content)
        assertEquals("true", summary.getValue("samplePackAddOnlyMainPath").jsonPrimitive.content)
        assertEquals("true", summary.getValue("samplePackSecondarySecretSlotUsed").jsonPrimitive.content)
        assertEquals("true", summary.getValue("samplePackFixedSeedVisibilityCase").jsonPrimitive.content)
        assertEquals("1", summary.getValue("activeSampleFixedSeedRunCount").jsonPrimitive.content)
        assertEquals("1", summary.getValue("touchedSampleRunCount").jsonPrimitive.content)
        assertEquals("1", summary.getValue("legacyLootProfileSchemaRejectCount").jsonPrimitive.content)
        assertEquals(
            listOf("official_sample_pack", "disabled_pack_fallback", "precedence_fixture", "split_bias_fixture"),
            cases.take(4).map { element -> element.jsonObject.getValue("fixtureId").jsonPrimitive.content },
        )
        assertEquals(
            listOf("sample.flooded_relics", "fixture_sample_flooded_relics_override"),
            precedenceCase.getValue("resolvedOrder").jsonArray.map { value -> value.jsonPrimitive.content },
        )
        assertEquals(
            listOf("REPLACE"),
            precedenceCase.getValue("overlayOps").jsonArray.map { value -> value.jsonPrimitive.content }.filter { op -> op != "ADD" },
        )
        assertEquals("true", officialCase.getValue("resourceContractVerified").jsonPrimitive.content)
        assertEquals(
            "sample.flooded_relics.loot.flooded_reliquary.secret",
            officialCase.getValue("secretRewardProfileId").jsonPrimitive.content,
        )
        assertEquals(
            harnessSpec.generatedTemplateSeeds.map(Long::toString),
            officialCase.getValue("generatedTemplateSeedList").jsonArray.map { value -> value.jsonPrimitive.content },
        )
        assertEquals(
            (harnessSpec.harnessSeeds + harnessSpec.generatedTemplateSeeds).distinct().map(Long::toString),
            header.getValue("seedList").jsonArray.map { value -> value.jsonPrimitive.content },
        )
        assertTrue(
            officialCase.getValue("generatedSpecialTemplateIds").jsonArray.any { value ->
                value.jsonPrimitive.content.startsWith("sample.flooded_relics.")
            },
        )
        assertTrue(
            officialCase.getValue("samplePackTouchedContentIds").jsonArray.map { value -> value.jsonPrimitive.content }
                .containsAll(
                    listOf(
                        "sample.flooded_relics.secret_zone.flooded_reliquary",
                        "sample.flooded_relics.hidden_event.flooded_reliquary.reward",
                        "sample.flooded_relics.loot.flooded_reliquary.secret",
                    ),
                ),
        )
        assertEquals("1", officialCase.getValue("samplePackFixedSeedRunCount").jsonPrimitive.content)
        assertEquals("1", officialCase.getValue("samplePackFixedSeedTouchedRunCount").jsonPrimitive.content)
        assertTrue(
            officialCase.getValue("samplePackFixedSeedTouchedContentIds").jsonArray.map { value -> value.jsonPrimitive.content }
                .containsAll(
                    listOf(
                        "sample.flooded_relics.secret_zone.flooded_reliquary",
                        "sample.flooded_relics.hidden_event.flooded_reliquary.reward",
                        "sample.flooded_relics.loot.flooded_reliquary.secret",
                    ),
                ),
        )
        assertEquals(listOf("underground_river"), splitBiasCase.getValue("lootProfileSpecialTemplateTagPreference").jsonArray.map { value -> value.jsonPrimitive.content })
        assertEquals(listOf("water"), splitBiasCase.getValue("lootProfileAffixTagPreference").jsonArray.map { value -> value.jsonPrimitive.content })
        assertEquals("content-pack.loot-profile.schema-version-mismatch", legacyRejectCase.getValue("diagnosticCodes").jsonArray.single().jsonPrimitive.content)
        assertEquals("loot.foundation.common", legacyRejectCase.getValue("diagnosticDetails").jsonArray.single().jsonObject.getValue("targetProfileId").jsonPrimitive.content)
        assertEquals(13, Files.readAllLines(run.runsPath).count { line -> line.isNotBlank() })
    }
}
