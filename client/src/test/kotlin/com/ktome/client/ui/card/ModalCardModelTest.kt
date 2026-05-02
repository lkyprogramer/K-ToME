package com.ktome.client.ui.card

import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RewardPresentationBuildIdentitySnapshot
import com.ktome.core.snapshot.RewardPresentationEntrySnapshot
import com.ktome.core.snapshot.RewardPresentationSourceSnapshot
import com.ktome.core.snapshot.ShopOfferSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ModalCardModelTest {
    @Test
    fun `enter and space trigger primary action while read more stays on question when secondary`() {
        val card =
            ModalCardModel(
                stableKey = "shop:offer:1",
                title = RenderTextTokenSnapshot("shop.test.name"),
                iconKey = "item.short_sword.icon",
                summary = null,
                detailLines = emptyList(),
                primaryAction = ModalCardAction.BUY,
                secondaryAction = ModalCardAction.READ_MORE,
            )

        assertEquals(ModalCardAction.BUY, card.actionFor(ModalCardInput.ENTER))
        assertEquals(ModalCardAction.BUY, card.actionFor(ModalCardInput.SPACE))
        assertEquals(ModalCardAction.READ_MORE, card.actionFor(ModalCardInput.QUESTION))
    }

    @Test
    fun `cancel and close cannot coexist on one card`() {
        assertThrows(IllegalArgumentException::class.java) {
            ModalCardModel(
                stableKey = "bad",
                title = RenderTextTokenSnapshot("ui.modal.action.close"),
                iconKey = null,
                summary = null,
                detailLines = emptyList(),
                primaryAction = ModalCardAction.CANCEL,
                secondaryAction = ModalCardAction.CLOSE,
            )
        }
    }

    @Test
    fun `read only close only card must declare poppable owner`() {
        assertThrows(IllegalArgumentException::class.java) {
            ModalCardModel(
                stableKey = "readonly",
                title = RenderTextTokenSnapshot("ui.modal.action.close"),
                iconKey = null,
                summary = null,
                detailLines = emptyList(),
                primaryAction = ModalCardAction.CLOSE,
                secondaryAction = ModalCardAction.CLOSE,
            )
        }

        val card =
            ModalCardModel(
                stableKey = "readonly",
                title = RenderTextTokenSnapshot("ui.modal.action.close"),
                iconKey = null,
                summary = null,
                detailLines = emptyList(),
                primaryAction = ModalCardAction.CLOSE,
                secondaryAction = ModalCardAction.CLOSE,
                returnPolicy = ModalCardReturnPolicy.READ_ONLY_POPPABLE,
            )

        assertEquals(ModalCardAction.CLOSE, card.actionFor(ModalCardInput.ESCAPE))
    }

    @Test
    fun `shop and reward cards use shared modal card contract`() {
        val offer =
            ModalCardModel.shopOffer(
                shopId = "reliquary",
                offer =
                    ShopOfferSnapshot(
                        index = 1,
                        labelKey = "item.healing_potion.name",
                        price = 12,
                        offerFingerprint = "offer-1",
                        tagLabelKeys = listOf("ui.shop.tag.once"),
                    ),
            )
        val sell =
            ModalCardModel.shopSellEntry(
                shopId = "reliquary",
                displayIndex = 0,
                item =
                    ItemRenderSnapshot(
                        baseItemId = "short_sword",
                        nameKey = "item.short_sword.name",
                        typeId = "WEAPON",
                        iconKey = "item.short_sword.icon",
                    ),
                price = 4,
            )
        val reward =
            ModalCardModel.rewardPresentation(
                index = 0,
                entry =
                    RewardPresentationEntrySnapshot(
                        source = RewardPresentationSourceSnapshot.HIDDEN_EVENT,
                        sourceLabelKey = "ui.reward.source.hidden_event",
                        itemDisplayName = RenderTextTokenSnapshot("item.healing_potion.name"),
                    ),
            )

        assertEquals("shop:reliquary:offer:1", offer.stableKey)
        assertEquals(ModalCardAction.BUY, offer.primaryAction)
        assertEquals("ui.card.shop.header.icon", offer.iconKey)
        assertEquals("ui.modal.card.cost.shards", offer.costLines.single().key)
        assertEquals(ModalCardAction.SELL, sell.primaryAction)
        assertEquals("item.short_sword.icon", sell.iconKey)
        assertEquals("ui.card.reward.header.icon", reward.iconKey)
        assertEquals(ModalCardReturnPolicy.READ_ONLY_POPPABLE, reward.returnPolicy)
        assertEquals(ModalCardAction.CLOSE, reward.actionFor(ModalCardInput.ESCAPE))
    }

    @Test
    fun `reward cards expose build identity slot profession and score reason`() {
        val reward =
            ModalCardModel.rewardPresentation(
                index = 0,
                entry =
                    RewardPresentationEntrySnapshot(
                        source = RewardPresentationSourceSnapshot.CACHE,
                        sourceLabelKey = "ui.reward.source.cache",
                        itemDisplayName = RenderTextTokenSnapshot("item.unique.deepcurrent_lens.name"),
                        buildIdentity =
                            RewardPresentationBuildIdentitySnapshot(
                                slotId = "OFF_HAND",
                                slotLabelKey = "ui.reward.slot.off_hand",
                                professionId = "arcanist",
                                professionLabelKey = "profession.arcanist.name",
                                scoreReason =
                                    RenderTextTokenSnapshot(
                                        "ui.reward.identity.reason.non_weapon_capstone",
                                        listOf(
                                            RenderTextArgumentSnapshot(name = "profession", valueKey = "profession.arcanist.name"),
                                            RenderTextArgumentSnapshot(name = "slot", valueKey = "ui.reward.slot.off_hand"),
                                        ),
                                    ),
                            ),
                    ),
            )

        assertEquals(
            listOf(
                "ui.reward.identity.slot",
                "ui.reward.identity.profession",
                "ui.reward.identity.reason.non_weapon_capstone",
            ),
            reward.detailLines.map(RenderTextTokenSnapshot::key),
        )
    }
}
