package com.ktome.core.loot

import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.random.SplitMix64RandomSource
import com.ktome.core.support.TestRandomSource
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LootBudgetResolverTest {
    private val zoneRewardProfile =
        ZoneRewardProfile(
            id = "zone_reward.test",
            zoneId = "greenwood_fringe",
            rarityBonus = 0.05f,
            qualityBonus = 1,
            baseRewardBudget = 8,
        )

    @Test
    fun `loot roll is deterministic for the same random sequence`() {
        val context = context(sourceTier = SourceTier.NORMAL, magicFindBonus = 0.25f)

        val first =
            LootBudgetResolver(TestRandomSource(ints = listOf(1, 700)))
                .roll(context = context, zoneRewardProfile = zoneRewardProfile)
        val second =
            LootBudgetResolver(TestRandomSource(ints = listOf(1, 700)))
                .roll(context = context, zoneRewardProfile = zoneRewardProfile)

        assertEquals(first, second)
    }

    @Test
    fun `magic find above one is clamped to one`() {
        val fullMagicFind =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 0)))
                .roll(context = context(magicFindBonus = 1.0f), zoneRewardProfile = zoneRewardProfile)
        val overflowMagicFind =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 0)))
                .roll(context = context(magicFindBonus = 1.5f), zoneRewardProfile = zoneRewardProfile)

        assertEquals(fullMagicFind.budget.rarityScore, overflowMagicFind.budget.rarityScore)
        assertEquals(fullMagicFind.budget.rarityTier, overflowMagicFind.budget.rarityTier)
    }

    @Test
    fun `higher magic find increases high rarity outcomes monotonically`() {
        val lowMagicFindDistribution = rarityDistribution(magicFindBonus = 0.0f)
        val highMagicFindDistribution = rarityDistribution(magicFindBonus = 0.75f)

        val lowHighRarity = lowMagicFindDistribution.getValue(RarityTier.MAGIC) + lowMagicFindDistribution.getValue(RarityTier.RARE)
        val highHighRarity = highMagicFindDistribution.getValue(RarityTier.MAGIC) + highMagicFindDistribution.getValue(RarityTier.RARE)

        assertTrue(highHighRarity >= lowHighRarity)
        assertTrue(highMagicFindDistribution.getValue(RarityTier.RARE) >= lowMagicFindDistribution.getValue(RarityTier.RARE))
    }

    @Test
    fun `source tier affects item level rarity score and affix budget`() {
        val normal =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 0)))
                .roll(context = context(sourceTier = SourceTier.NORMAL), zoneRewardProfile = zoneRewardProfile)
        val boss =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 0)))
                .roll(context = context(sourceTier = SourceTier.BOSS), zoneRewardProfile = zoneRewardProfile)

        assertTrue(boss.budget.iLvl > normal.budget.iLvl)
        assertTrue(boss.budget.rarityScore > normal.budget.rarityScore)
        assertTrue(boss.budget.affixBudget > normal.budget.affixBudget)
    }

    @Test
    fun `special tiers only enter trace when no template pool is available`() {
        val result =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 0)))
                .roll(
                    context = context(sourceTier = SourceTier.BOSS),
                    zoneRewardProfile = zoneRewardProfile,
                    specialTierEligibility = SpecialTierEligibility(availableSpecialTiers = setOf(SpecialTier.UNIQUE, SpecialTier.ARTIFACT)),
                )

        assertTrue(result.specialUpgradeAttempted)
        assertNull(result.upgradedSpecialTier)
        assertEquals(1, result.resultingPityTracker.eligibleSpecialRollsSinceLastUnique)
    }

    @Test
    fun `rare pity doubles rare weight and resets on rare outcome`() {
        val result =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 1000)))
                .roll(
                    context = context(),
                    zoneRewardProfile = zoneRewardProfile,
                    pityTracker = PityTracker(rollsSinceLastRare = 20),
                )

        assertTrue(result.rarePityApplied)
        assertEquals(RarityTier.RARE, result.rolledRarityTier)
        assertEquals(0, result.resultingPityTracker.rollsSinceLastRare)
    }

    @Test
    fun `special pity resets only when a unique plus tier is actually issued`() {
        val result =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 0, 0)))
                .roll(
                    context = context(sourceTier = SourceTier.ELITE),
                    zoneRewardProfile = zoneRewardProfile,
                    specialTierEligibility =
                        SpecialTierEligibility(
                            availableSpecialTiers = setOf(SpecialTier.UNIQUE),
                            availableTemplateIds = setOf("unique.test_blade"),
                        ),
                    pityTracker = PityTracker(eligibleSpecialRollsSinceLastUnique = 50),
                )

        assertTrue(result.specialPityApplied)
        assertEquals(SpecialTier.UNIQUE, result.upgradedSpecialTier)
        assertEquals(0, result.resultingPityTracker.eligibleSpecialRollsSinceLastUnique)
    }

    @Test
    fun `special tier eligibility serializes to stable json object`() {
        val json =
            SpecialTierEligibility(
                availableSpecialTiers = setOf(SpecialTier.ARTIFACT, SpecialTier.UNIQUE),
                availableTemplateIds = setOf("artifact.void_crown", "unique.sun_blade"),
            ).toJson()

        assertEquals(
            listOf("ARTIFACT", "UNIQUE"),
            json.getValue("availableSpecialTiers").jsonArray.map { element -> element.jsonPrimitive.content },
        )
        assertEquals(
            listOf("artifact.void_crown", "unique.sun_blade"),
            json.getValue("availableTemplateIds").jsonArray.map { element -> element.jsonPrimitive.content },
        )
    }

    @Test
    fun `floor reward budget serializes to stable json object`() {
        val json =
            FloorRewardBudget(
                zoneId = "greenwood_fringe",
                floorIndex = 2,
                baseBudget = 8,
                rewardDeltas = listOf(RewardDelta(source = "route", amount = 3), RewardDelta(source = "boss", amount = 5)),
            ).toJson()

        assertEquals("greenwood_fringe", json.getValue("zoneId").jsonPrimitive.content)
        assertEquals("2", json.getValue("floorIndex").jsonPrimitive.content)
        assertEquals("8", json.getValue("baseBudget").jsonPrimitive.content)
        assertEquals("16", json.getValue("totalBudget").jsonPrimitive.content)
        assertEquals(
            listOf("boss", "route"),
            json.getValue("rewardDeltas").jsonArray.map { element ->
                element.jsonObject.getValue("source").jsonPrimitive.content
            },
        )
    }

    @Test
    fun `loot contracts serialize to stable json objects`() {
        val result =
            LootBudgetResolver(TestRandomSource(ints = listOf(0, 0)))
                .roll(context = context(sourceTier = SourceTier.CHEST), zoneRewardProfile = zoneRewardProfile)

        val contextJson = result.context.toJson()
        val budgetJson = result.budget.toJson()
        val pityJson = result.resultingPityTracker.toJson()
        val resultJson = result.toJson()

        assertEquals("CHEST", contextJson.getValue("sourceTier").jsonPrimitive.content)
        assertEquals(result.budget.rarityTier.name, budgetJson.getValue("rarityTier").jsonPrimitive.content)
        assertEquals(
            result.budget.specialTierEligibility.availableTemplateIds.size.toString(),
            budgetJson.getValue("specialTierEligibility").jsonObject.getValue("availableTemplateIds").jsonArray.size.toString(),
        )
        assertEquals(
            result.resultingPityTracker.rollsSinceLastRare.toString(),
            pityJson.getValue("rollsSinceLastRare").jsonPrimitive.content,
        )
        assertEquals(result.rolledRarityTier.name, resultJson.getValue("rolledRarityTier").jsonPrimitive.content)
    }

    private fun rarityDistribution(magicFindBonus: Float): Map<RarityTier, Int> {
        val random = SplitMix64RandomSource.fromSeed(20260405L)
        val resolver = LootBudgetResolver(random)
        return buildMap {
            repeat(4_000) {
                val result =
                    resolver.roll(
                        context = context(magicFindBonus = magicFindBonus),
                        zoneRewardProfile = zoneRewardProfile,
                    )
                put(result.rolledRarityTier, (get(result.rolledRarityTier) ?: 0) + 1)
            }
            RarityTier.entries.forEach { tier -> putIfAbsent(tier, 0) }
        }
    }

    private fun context(
        sourceTier: SourceTier = SourceTier.NORMAL,
        magicFindBonus: Float = 0.0f,
    ): LootRollContext =
        LootRollContext(
            sourceLevel = 8,
            sourceTier = sourceTier,
            zoneId = "greenwood_fringe",
            playerLevel = 7,
            magicFindBonus = magicFindBonus,
            seed = 42L,
        )
}
