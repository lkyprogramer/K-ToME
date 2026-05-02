package com.ktome.game.loot

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemType
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.SpecialItemTemplate
import com.ktome.core.item.StatModifier
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.resource.ResourceType
import com.ktome.game.data.DataLoader
import com.ktome.game.loot.foundationBuildIdentityByProfessionId
import com.ktome.game.professionAffixBuildContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MilestoneRewardSelectorTest {
    private val dataLoader = DataLoader()
    private val schemaCatalog = dataLoader.loadSchemaCatalog()
    private val itemBundle = dataLoader.loadItemBundle()
    private val lootProfilesById = schemaCatalog.lootProfiles.associateBy { profile -> profile.id }
    private val professionsById = schemaCatalog.professions.associateBy { profession -> profession.id }
    private val itemSchemasById = schemaCatalog.itemBundle.items.associateBy { item -> item.id }
    private val resolver = LootProfileCandidatePoolResolver(itemBundle)

    @Test
    fun `special linked bases ignore standard drop floors when template zone and source match`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "artifact_test_lens",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "arcanist", "capstone", "non_weapon_capstone", "water"),
                            dropFloors = emptyList(),
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
                specialTemplates =
                    listOf(
                        specialTemplate(
                            id = "artifact.test_lens",
                            itemId = "artifact_test_lens",
                            allowedSourceTiers = setOf(SourceTier.CHEST),
                            allowedZones = setOf("test_zone"),
                            tags = setOf("arcanist", "capstone"),
                        ),
                    ),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("artifact_test_lens"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.CACHE,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 1,
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("water"), preferredProfessionTag = "arcanist"),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals("artifact_test_lens", result.selectedBaseId)
        val candidate = requireNotNull(result.rankedCandidates.singleOrNull())
        assertTrue(candidate.legal)
        assertNull(candidate.rejectionReason)
        assertTrue(candidate.specialLinkedBase)
    }

    @Test
    fun `standard and special linked candidates share one weighted ranking`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "generic_spell_blade",
                            tags = setOf("item", "weapon", "spell", "mana", "offense"),
                            dropWeight = 9,
                        ),
                        baseItem(
                            id = "artifact_river_focus",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "arcanist", "mana", "capstone", "non_weapon_capstone"),
                            dropWeight = 0,
                            dropFloors = emptyList(),
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
                specialTemplates =
                    listOf(
                        specialTemplate(
                            id = "artifact.river_focus",
                            itemId = "artifact_river_focus",
                            allowedSourceTiers = setOf(SourceTier.CHEST),
                            allowedZones = setOf("test_zone"),
                            tags = setOf("arcanist", "capstone"),
                        ),
                    ),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("generic_spell_blade", "artifact_river_focus"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.SUPPORT,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 4,
                        ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("mana", "spell"), preferredProfessionTag = "arcanist"),
                        poolWeightByBaseId = mapOf("generic_spell_blade" to 18, "artifact_river_focus" to 10),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals("artifact_river_focus", result.selectedBaseId)
        val rankedById = result.rankedCandidates.associateBy(MilestoneRewardCandidate::baseItemId)
        assertTrue(requireNotNull(rankedById["artifact_river_focus"]).legal)
        assertTrue(requireNotNull(rankedById["generic_spell_blade"]).legal)
        assertTrue(
            requireNotNull(rankedById["artifact_river_focus"]).score >
                requireNotNull(rankedById["generic_spell_blade"]).score,
        )
    }

    @Test
    fun `non weapon capstones can override configured replacement priority`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "weapon_capstone",
                            tags = setOf("item", "weapon", "templar", "holy", "capstone"),
                            dropWeight = 6,
                        ),
                        baseItem(
                            id = "offhand_capstone",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "templar", "holy", "capstone", "non_weapon_capstone"),
                            dropWeight = 4,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("weapon_capstone", "offhand_capstone"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.CACHE,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                                occupiedSlots = setOf(EquipSlot.WEAPON, EquipSlot.OFF_HAND),
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("holy", "guard"), preferredProfessionTag = "templar"),
                        replacementSlotPriority = listOf(EquipSlot.WEAPON, EquipSlot.OFF_HAND),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals(EquipSlot.OFF_HAND, result.replacementSlot)
        assertEquals("offhand_capstone", result.selectedBaseId)
    }

    @Test
    fun `selector reuses ranked candidates for the chosen replacement slot`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "generic_guard_blade",
                            tags = setOf("item", "weapon", "templar", "holy"),
                            dropWeight = 10,
                        ),
                        baseItem(
                            id = "offhand_capstone",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "templar", "holy", "capstone", "non_weapon_capstone"),
                            dropWeight = 4,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )
        var affixChecks = 0

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("generic_guard_blade", "offhand_capstone"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.CACHE,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                                occupiedSlots = setOf(EquipSlot.OFF_HAND),
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("holy", "guard"), preferredProfessionTag = "templar"),
                        replacementSlotPriority = listOf(EquipSlot.OFF_HAND),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = {
                    affixChecks += 1
                    true
                },
            )

        assertEquals("offhand_capstone", result.selectedBaseId)
        assertEquals(4, affixChecks)
    }

    @Test
    fun `river temple and heart fixture cases stay explainable through selector diagnostics`() {
        val riverResult =
            selectProfile(
                profileId = "loot.underground_river.reward",
                professionId = "arcanist",
                zoneId = "underground_river",
                rewardSource = MilestoneRewardSource.SUPPORT,
                occupiedSlots = setOf(EquipSlot.OFF_HAND),
                currentOwnedBaseIds = setOf("emerald_charm"),
        )
        assertEquals(EquipSlot.OFF_HAND, riverResult.replacementSlot)
        assertEquals("unique_deepcurrent_lens", riverResult.selectedBaseId)
        assertEquals(
            MilestoneRewardRejectionReason.REPLACEMENT_SLOT_MISMATCH,
            candidate(riverResult, "artifact_river_echo").rejectionReason,
        )

        val templeResult =
            selectProfile(
                profileId = "loot.abyssal_temple.reward",
                professionId = "templar",
                zoneId = "abyssal_temple",
                rewardSource = MilestoneRewardSource.CACHE,
                occupiedSlots = setOf(EquipSlot.OFF_HAND, EquipSlot.ARMOR),
            )
        assertNotNull(templeResult.replacementSlot)
        assertTrue(
            requireNotNull(templeResult.selectedBaseId) in
                setOf("artifact_eclipsed_relic", "unique_vesper_chainmail", "unique_voidlit_seal"),
        )
        assertTrue(candidate(templeResult, requireNotNull(templeResult.selectedBaseId)).exactProfessionCapstone)
        assertTrue(candidate(templeResult, requireNotNull(templeResult.selectedBaseId)).score > candidate(templeResult, "sanctified_seal").score)

        val heartResult =
            selectProfile(
                profileId = "loot.abyssal_heart.reward",
                professionId = "rogue",
                zoneId = "abyssal_heart",
                rewardSource = MilestoneRewardSource.BOSS,
                playerLevel = 5,
            )
        assertEquals("artifact_briar_heart", heartResult.selectedBaseId)
        assertTrue(candidate(heartResult, "artifact_briar_heart").legal)
        assertTrue(candidate(heartResult, "artifact_briar_heart").nonWeaponCapstone)
        assertTrue(!candidate(heartResult, "basic_shield").legal)
        assertTrue(candidate(heartResult, "basic_shield").scoreBreakdown.lateCommonPenalty > 0)
        assertTrue(
            heartResult.rankedCandidates
                .filter(MilestoneRewardCandidate::legal)
                .any { candidate -> candidate.baseItemId in setOf("artifact_briar_heart", "unique_thornpath_crook") },
        )
    }

    @Test
    fun `generic aligned weapon does not suppress exact profession capstone`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "generic_guard_blade",
                            tags = setOf("item", "weapon", "frontline", "guard", "holy", "dominant_risk"),
                            dropWeight = 10,
                        ),
                        baseItem(
                            id = "templar_relic",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "templar", "holy", "capstone", "non_weapon_capstone"),
                            dropWeight = 2,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("generic_guard_blade", "templar_relic"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.CACHE,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                            ),
                        selectionContext =
                            LootBaseSelectionContext(
                                buildTags = setOf("templar", "holy", "guard"),
                                preferredProfessionTag = "templar",
                            ),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals("templar_relic", result.selectedBaseId)
        assertTrue(candidate(result, "templar_relic").score > candidate(result, "generic_guard_blade").score)
    }

    @Test
    fun `cinderveil is arcanist non weapon payoff but not exact profession capstone`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "unique_cinderveil_plate",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.ARMOR,
                            tags = setOf("item", "armor", "unique", "arcanist", "survival", "capstone", "non_weapon_capstone"),
                            dropWeight = 4,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("unique_cinderveil_plate"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.SUPPORT,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                                professionId = "arcanist",
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("arcanist", "survival"), preferredProfessionTag = "arcanist"),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        val cinderveil = candidate(result, "unique_cinderveil_plate")
        assertEquals("unique_cinderveil_plate", result.selectedBaseId)
        assertTrue(cinderveil.nonWeaponCapstone)
        assertTrue(!cinderveil.exactProfessionCapstone)
        assertEquals(0, cinderveil.scoreBreakdown.professionCapstoneBonus)
        assertTrue(cinderveil.scoreBreakdown.nonWeaponPayoffBonus > 0)
        assertTrue(cinderveil.scoreBreakdown.terminalIdentityBonus > 0)
    }

    @Test
    fun `off hand non weapon capstones keep off hand slot family despite accessory tags`() {
        val base =
            baseItem(
                id = "offhand_identity_relic",
                type = ItemType.ARMOR,
                slot = EquipSlot.OFF_HAND,
                tags = setOf("item", "armor", "accessory", "rogue", "capstone", "non_weapon_capstone"),
            )

        assertEquals(MilestoneRewardSlotFamily.OFF_HAND, milestoneRewardSlotFamily(base))
    }

    @Test
    fun `ordinary off hand seals stay utility while talismans stay accessory`() {
        val seal =
            baseItem(
                id = "ordinary_seal",
                type = ItemType.ARMOR,
                slot = EquipSlot.OFF_HAND,
                tags = setOf("item", "armor", "accessory", "seal", "spell", "protection"),
            )
        val talisman =
            baseItem(
                id = "ordinary_talisman",
                type = ItemType.ARMOR,
                slot = EquipSlot.OFF_HAND,
                tags = setOf("item", "armor", "accessory", "fire", "protection"),
            )

        assertEquals(MilestoneRewardSlotFamily.CONSUMABLE_OR_UTILITY, milestoneRewardSlotFamily(seal))
        assertEquals(MilestoneRewardSlotFamily.ACCESSORY, milestoneRewardSlotFamily(talisman))
    }

    @Test
    fun `slot rotation bonus only applies inside non weapon payoff cap`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "generic_guard_blade",
                            tags = setOf("item", "weapon", "templar", "holy", "guard"),
                            dropWeight = 4,
                        ),
                        baseItem(
                            id = "templar_relic",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "templar", "holy", "capstone", "non_weapon_capstone"),
                            dropWeight = 4,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("generic_guard_blade", "templar_relic"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.CACHE,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                                professionId = "templar",
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("templar", "holy", "guard"), preferredProfessionTag = "templar"),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals(0, candidate(result, "generic_guard_blade").scoreBreakdown.slotRotationBonus)
        val relicBreakdown = candidate(result, "templar_relic").scoreBreakdown
        assertTrue(relicBreakdown.slotRotationBonus > 0)
        assertTrue(relicBreakdown.slotRotationBonus <= relicBreakdown.nonWeaponPayoffBonus / 2)
    }

    @Test
    fun `equal score tie break uses item id after frozen capstone rarity and slot order`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "zeta_blade",
                            tags = setOf("item", "weapon", "generic"),
                            dropWeight = 4,
                        ),
                        baseItem(
                            id = "alpha_blade",
                            tags = setOf("item", "weapon", "generic"),
                            dropWeight = 4,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("zeta_blade", "alpha_blade"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.CACHE,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("generic")),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals("alpha_blade", result.selectedBaseId)
    }

    @Test
    fun `owned support identity candidates still rank by score before item id`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "owned_high_score_capstone",
                            tags = setOf("item", "weapon", "templar", "capstone"),
                            baseStats = StatModifier(attack = 6),
                            dropWeight = 10,
                        ),
                        baseItem(
                            id = "unowned_low_score_capstone",
                            tags = setOf("item", "weapon", "templar", "capstone"),
                            dropWeight = 1,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("unowned_low_score_capstone", "owned_high_score_capstone"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.SUPPORT,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                                professionId = "templar",
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("templar"), preferredProfessionTag = "templar"),
                        currentOwnedBaseIds = setOf("owned_high_score_capstone"),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals("owned_high_score_capstone", result.selectedBaseId)
        assertTrue(candidate(result, "owned_high_score_capstone").score > candidate(result, "unowned_low_score_capstone").score)
    }

    @Test
    fun `support rewards reject starter shield duplicates while allowing non starter and identity fallback`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "basic_shield",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "shield", "guard"),
                            dropWeight = 4,
                        ),
                        baseItem(
                            id = "sanctified_seal",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "templar", "holy", "protection"),
                            dropWeight = 4,
                        ),
                        baseItem(
                            id = "unique_voidlit_seal",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            tags = setOf("item", "armor", "accessory", "templar", "holy", "protection", "capstone", "non_weapon_capstone"),
                            dropWeight = 4,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val starterSupportResult =
            duplicateOwnedBaseSelection(
                bundle = bundle,
                baseItemId = "basic_shield",
                rewardSource = MilestoneRewardSource.SUPPORT,
            )
        val nonStarterSupportResult =
            duplicateOwnedBaseSelection(
                bundle = bundle,
                baseItemId = "sanctified_seal",
                rewardSource = MilestoneRewardSource.SUPPORT,
            )
        val capstoneSupportResult =
            duplicateOwnedBaseSelection(
                bundle = bundle,
                baseItemId = "unique_voidlit_seal",
                rewardSource = MilestoneRewardSource.SUPPORT,
            )
        val capstoneRouteResult =
            duplicateOwnedBaseSelection(
                bundle = bundle,
                baseItemId = "unique_voidlit_seal",
                rewardSource = MilestoneRewardSource.ROUTE,
            )

        assertNull(starterSupportResult.selectedBaseId)
        assertEquals(MilestoneRewardRejectionReason.OWNED_BASE_DUPLICATE, candidate(starterSupportResult, "basic_shield").rejectionReason)
        assertEquals("sanctified_seal", nonStarterSupportResult.selectedBaseId)
        assertTrue(candidate(nonStarterSupportResult, "sanctified_seal").legal)
        assertEquals("unique_voidlit_seal", capstoneSupportResult.selectedBaseId)
        assertTrue(candidate(capstoneSupportResult, "unique_voidlit_seal").legal)
        assertNull(capstoneRouteResult.selectedBaseId)
        assertEquals(MilestoneRewardRejectionReason.OWNED_BASE_DUPLICATE, candidate(capstoneRouteResult, "unique_voidlit_seal").rejectionReason)
    }

    @Test
    fun `profession capstone bonus can tip exact profession capstones over generic aligned weapons`() {
        val bundle =
            ItemDataBundle(
                baseItems =
                    listOf(
                        baseItem(
                            id = "rogue_generic_blade",
                            tags = setOf("item", "weapon", "rogue", "precision", "mobility"),
                            dropWeight = 10,
                        ),
                        baseItem(
                            id = "rogue_capstone_blade",
                            tags = setOf("item", "weapon", "rogue", "precision", "capstone"),
                            dropWeight = 1,
                        ),
                    ),
                materials = emptyList(),
                affixes = emptyList(),
            )

        val result =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("rogue_generic_blade", "rogue_capstone_blade"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.CACHE,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("rogue", "precision"), preferredProfessionTag = "rogue"),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals("rogue_capstone_blade", result.selectedBaseId)
        assertEquals(
            candidate(result, "rogue_generic_blade").scoreBreakdown.baseScore,
            candidate(result, "rogue_capstone_blade").scoreBreakdown.baseScore,
        )
        assertTrue(candidate(result, "rogue_capstone_blade").scoreBreakdown.professionCapstoneBonus > 0)
        assertEquals(
            0,
            candidate(result, "rogue_generic_blade").scoreBreakdown.professionCapstoneBonus,
        )
    }

    private fun selectProfile(
        profileId: String,
        professionId: String,
        zoneId: String,
        rewardSource: MilestoneRewardSource,
        occupiedSlots: Set<EquipSlot> = emptySet(),
        currentOwnedBaseIds: Set<String> = emptySet(),
        effectiveFloorBand: Int = 5,
        playerLevel: Int = 1,
    ): MilestoneRewardSelectionResult {
        val profile = requireNotNull(lootProfilesById[profileId]) { "Missing loot profile '$profileId'." }
        val pools = listOf(resolver.resolve(profile))
        val selectionContext = selectionContextForProfession(professionId)
        return MilestoneRewardSelector(itemBundle).select(
            request =
                MilestoneRewardSelectionRequest(
                    candidateBaseIds = pools.flatMapTo(linkedSetOf()) { pool -> pool.allCandidateBaseIds }.toList(),
                    selectorContext =
                        MilestoneRewardSelectorContext(
                            rewardSource = rewardSource,
                            zoneId = zoneId,
                            sourceTier = rewardSourceTier(rewardSource),
                            effectiveFloorBand = effectiveFloorBand,
                            professionId = professionId,
                            playerLevel = playerLevel,
                            occupiedSlots = occupiedSlots,
                        ),
                    poolWeightByBaseId = weightByBaseId(pools, selectionContext),
                    selectionContext = selectionContext,
                    currentOwnedBaseIds = currentOwnedBaseIds,
                    replacementSlotPriority = replacementSlotPriority(professionId),
                    forbiddenBaseIds = setOf("healing_potion", "scroll_teleport", "mana_potion", "stamina_draught", "energy_tonic", "consecrated_oil"),
                ),
            professionSuitability = professionSuitability(professionId),
            canSatisfyAffixes = { true },
        )
    }

    private fun selectionContextForProfession(professionId: String): LootBaseSelectionContext {
        val profession = requireNotNull(professionsById[professionId]) { "Missing profession '$professionId'." }
        return LootBaseSelectionContext(
            buildTags = professionAffixBuildContext(schemaCatalog, profession).buildTags,
            preferredProfessionTag = professionId,
        )
    }

    private fun professionSuitability(professionId: String): (ItemBaseDef) -> Boolean {
        val profession = requireNotNull(professionsById[professionId]) { "Missing profession '$professionId'." }
        return { base ->
            if (base.resourceTypeId != null && base.resourceTypeId != profession.resourceType) {
                false
            } else {
                val itemSchema = itemSchemasById[base.id]
                !(profession.resourceType != ResourceType.MANA.name && itemSchema?.tags?.contains("arcane") == true)
            }
        }
    }

    private fun weightByBaseId(
        pools: List<LootProfileCandidatePool>,
        selectionContext: LootBaseSelectionContext,
    ): Map<String, Int> {
        val baseItemsById = itemBundle.baseItems.associateBy(ItemBaseDef::id)
        val weightsByBaseId = linkedMapOf<String, Int>()
        pools.forEach { pool ->
            pool.allCandidateBaseIds.forEach { baseId ->
                val base = requireNotNull(baseItemsById[baseId]) { "Missing base '$baseId'." }
                val weightedDrop = pool.weightFor(base, selectionContext)
                weightsByBaseId[baseId] = weightsByBaseId.getOrDefault(baseId, 0) + weightedDrop
            }
        }
        return weightsByBaseId
    }

    private fun replacementSlotPriority(professionId: String): List<EquipSlot> =
        foundationBuildIdentityByProfessionId[professionId]
            ?.preferredReplacementSlots
            ?.toList()
            ?.let { preferredSlots ->
                preferredSlots + EquipSlot.entries.filterNot(preferredSlots::contains)
            }
            ?: listOf(EquipSlot.OFF_HAND, EquipSlot.ARMOR, EquipSlot.WEAPON)

    private fun duplicateOwnedBaseSelection(
        bundle: ItemDataBundle,
        baseItemId: String,
        rewardSource: MilestoneRewardSource,
    ): MilestoneRewardSelectionResult =
        MilestoneRewardSelector(bundle).select(
            request =
                MilestoneRewardSelectionRequest(
                    candidateBaseIds = listOf(baseItemId),
                    selectorContext =
                        MilestoneRewardSelectorContext(
                            rewardSource = rewardSource,
                            zoneId = "test_zone",
                            sourceTier = SourceTier.CHEST,
                            effectiveFloorBand = 5,
                            professionId = "templar",
                        ),
                    selectionContext = LootBaseSelectionContext(buildTags = setOf("templar", "holy"), preferredProfessionTag = "templar"),
                    currentOwnedBaseIds = setOf(baseItemId),
                ),
            professionSuitability = { true },
            canSatisfyAffixes = { true },
        )

    private fun rewardSourceTier(source: MilestoneRewardSource): SourceTier =
        when (source) {
            MilestoneRewardSource.ROUTE,
            MilestoneRewardSource.CACHE,
            MilestoneRewardSource.SUPPORT,
            -> SourceTier.CHEST
            MilestoneRewardSource.BOSS -> SourceTier.BOSS
        }

    private fun candidate(
        result: MilestoneRewardSelectionResult,
        baseItemId: String,
    ): MilestoneRewardCandidate =
        requireNotNull(result.rankedCandidates.firstOrNull { candidate -> candidate.baseItemId == baseItemId }) {
            "Missing candidate '$baseItemId'."
        }

    private fun baseItem(
        id: String,
        type: ItemType = ItemType.WEAPON,
        slot: EquipSlot? = if (type == ItemType.WEAPON) EquipSlot.WEAPON else null,
        tags: Set<String>,
        baseStats: StatModifier = StatModifier(),
        dropWeight: Int = 1,
        dropFloors: List<Int> = listOf(1, 2, 3, 4, 5),
    ): ItemBaseDef =
        ItemBaseDef(
            id = id,
            name = id,
            type = type,
            slot = slot,
            tags = tags,
            glyph = if (type == ItemType.CONSUMABLE) '!' else ')',
            colorHex = "#FFFFFF",
            baseStats = baseStats,
            dropWeight = dropWeight,
            dropFloors = dropFloors,
            effect = if (type == ItemType.CONSUMABLE) ConsumableEffect.HEAL else null,
            magnitude = if (type == ItemType.CONSUMABLE) 20 else 0,
        )

    private fun specialTemplate(
        id: String,
        itemId: String,
        allowedSourceTiers: Set<SourceTier>,
        allowedZones: Set<String>,
        tags: Set<String>,
    ): SpecialItemTemplate =
        SpecialItemTemplate(
            id = id,
            itemId = itemId,
            specialTier = SpecialTier.ARTIFACT,
            nameKey = "$id.name",
            descKey = "$id.desc",
            visualKey = "$id.visual",
            iconKey = "$id.icon",
            audioProfile = "$id.audio",
            schemaVersion = 2,
            tags = tags,
            allowedSourceTiers = allowedSourceTiers,
            allowedZones = allowedZones,
        )
}
