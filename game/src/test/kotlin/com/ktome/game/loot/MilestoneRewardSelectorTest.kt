package com.ktome.game.loot

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemType
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.SpecialItemTemplate
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
                        rewardPreferenceOrder = listOf("artifact_river_focus", "generic_spell_blade"),
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
        assertTrue(candidate(riverResult, "unique_deepcurrent_lens").score > candidate(riverResult, "artifact_river_echo").score)

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
            )
        assertEquals("artifact_briar_heart", heartResult.selectedBaseId)
        assertTrue(candidate(heartResult, "artifact_briar_heart").legal)
        assertTrue(candidate(heartResult, "artifact_briar_heart").nonWeaponCapstone)
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
    fun `preferred reward sources can tip exact profession capstones over generic aligned weapons`() {
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

        val preferredResult =
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
                        poolWeightByBaseId = mapOf("rogue_generic_blade" to 18, "rogue_capstone_blade" to 8),
                        preferredRewardSources = setOf(MilestoneRewardSource.CACHE),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )
        val nonPreferredResult =
            MilestoneRewardSelector(bundle).select(
                request =
                    MilestoneRewardSelectionRequest(
                        candidateBaseIds = listOf("rogue_generic_blade", "rogue_capstone_blade"),
                        selectorContext =
                            MilestoneRewardSelectorContext(
                                rewardSource = MilestoneRewardSource.SUPPORT,
                                zoneId = "test_zone",
                                sourceTier = SourceTier.CHEST,
                                effectiveFloorBand = 5,
                            ),
                        selectionContext = LootBaseSelectionContext(buildTags = setOf("rogue", "precision"), preferredProfessionTag = "rogue"),
                        poolWeightByBaseId = mapOf("rogue_generic_blade" to 18, "rogue_capstone_blade" to 8),
                        preferredRewardSources = setOf(MilestoneRewardSource.CACHE),
                    ),
                professionSuitability = { true },
                canSatisfyAffixes = { true },
            )

        assertEquals("rogue_capstone_blade", preferredResult.selectedBaseId)
        assertEquals("rogue_generic_blade", nonPreferredResult.selectedBaseId)
        assertEquals(50, candidate(preferredResult, "rogue_capstone_blade").scoreBreakdown.preferredRewardSourceScore)
        assertEquals(0, candidate(nonPreferredResult, "rogue_capstone_blade").scoreBreakdown.preferredRewardSourceScore)
    }

    private fun selectProfile(
        profileId: String,
        professionId: String,
        zoneId: String,
        rewardSource: MilestoneRewardSource,
        occupiedSlots: Set<EquipSlot> = emptySet(),
        currentOwnedBaseIds: Set<String> = emptySet(),
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
                            effectiveFloorBand = 5,
                            occupiedSlots = occupiedSlots,
                        ),
                    poolWeightByBaseId = weightByBaseId(pools, selectionContext),
                    selectionContext = selectionContext,
                    currentOwnedBaseIds = currentOwnedBaseIds,
                    preferredRewardSources = preferredRewardSources(professionId),
                    rewardPreferenceOrder = rewardPreferenceOrder(professionId),
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

    private fun rewardPreferenceOrder(professionId: String): List<String> =
        when (professionId) {
            "vanguard" -> listOf("abyssal_heartstone", "forgebreaker_pick", "long_sword", "basic_shield", "chain_mail", "war_maul", "healing_potion", "scroll_teleport")
            "arcanist" -> listOf("abyssal_heartstone", "arcane_staff", "emerald_charm", "seal_reliquary", "mana_potion", "apprentice_robe", "scroll_teleport", "healing_potion")
            "rogue" -> listOf("abyssal_heartstone", "short_sword", "hunter_bow", "bandit_trophy", "leather_armor", "energy_tonic", "scroll_teleport", "healing_potion")
            "templar" -> listOf("abyssal_heartstone", "long_sword", "basic_shield", "sanctified_seal", "chain_mail", "consecrated_oil", "healing_potion")
            else -> listOf("healing_potion", "scroll_teleport")
        }

    private fun replacementSlotPriority(professionId: String): List<EquipSlot> =
        foundationBuildIdentityByProfessionId[professionId]
            ?.preferredReplacementSlots
            ?.toList()
            ?.let { preferredSlots ->
                preferredSlots + EquipSlot.entries.filterNot(preferredSlots::contains)
            }
            ?: listOf(EquipSlot.OFF_HAND, EquipSlot.ARMOR, EquipSlot.WEAPON)

    private fun preferredRewardSources(professionId: String): Set<MilestoneRewardSource> =
        foundationBuildIdentityByProfessionId[professionId]?.preferredRewardSources.orEmpty()

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
