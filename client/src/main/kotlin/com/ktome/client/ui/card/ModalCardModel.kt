package com.ktome.client.ui.card

import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RewardPresentationEntrySnapshot
import com.ktome.core.snapshot.ShopOfferSnapshot

internal enum class ModalCardAction(
    val labelKey: String,
) {
    CONFIRM("ui.modal.action.confirm"),
    CANCEL("ui.modal.action.cancel"),
    BUY("ui.modal.action.buy"),
    SELL("ui.modal.action.sell"),
    ENTER_ROUTE("ui.modal.action.enter-route"),
    READ_MORE("ui.modal.action.read-more"),
    CLOSE("ui.modal.action.close"),
    RETRY("ui.error.action.retry"),
    BACK_TO_MENU("ui.error.action.back-to-menu"),
    COPY_ERROR_DETAIL("ui.error.action.copy-detail"),
}

internal enum class ModalCardInput {
    ENTER,
    SPACE,
    QUESTION,
    ESCAPE,
    BACKSPACE,
}

internal enum class ModalCardReturnPolicy {
    REQUIRES_PROGRESS_ACTION,
    READ_ONLY_POPPABLE,
}

internal data class ModalCardModel(
    val stableKey: String,
    val title: RenderTextTokenSnapshot,
    val iconKey: String?,
    val summary: RenderTextTokenSnapshot?,
    val detailLines: List<RenderTextTokenSnapshot>,
    val costLines: List<RenderTextTokenSnapshot> = emptyList(),
    val rewardLines: List<RenderTextTokenSnapshot> = emptyList(),
    val disabledReason: RenderTextTokenSnapshot? = null,
    val primaryAction: ModalCardAction,
    val secondaryAction: ModalCardAction,
    val returnPolicy: ModalCardReturnPolicy = ModalCardReturnPolicy.REQUIRES_PROGRESS_ACTION,
) {
    init {
        require(stableKey.isNotBlank()) { "ModalCardModel.stableKey must not be blank." }
        require(title.key.isNotBlank()) { "ModalCardModel.title must not be blank." }
        require(!(primaryAction == ModalCardAction.CANCEL && secondaryAction == ModalCardAction.CLOSE)) {
            "ModalCardModel cannot combine Cancel and Close."
        }
        require(!(primaryAction == ModalCardAction.CLOSE && secondaryAction == ModalCardAction.CANCEL)) {
            "ModalCardModel cannot combine Close and Cancel."
        }
        val closeOnly = primaryAction.isTerminalOnly() && secondaryAction.isTerminalOnly()
        require(!closeOnly || returnPolicy == ModalCardReturnPolicy.READ_ONLY_POPPABLE) {
            "ModalCardModel needs a progress action unless it belongs to a read-only poppable owner."
        }
    }

    fun actionFor(input: ModalCardInput): ModalCardAction? =
        when (input) {
            ModalCardInput.ENTER,
            ModalCardInput.SPACE,
            -> if (primaryAction == ModalCardAction.READ_MORE) ModalCardAction.READ_MORE else primaryAction

            ModalCardInput.QUESTION -> ModalCardAction.READ_MORE.takeIf { primaryAction == ModalCardAction.READ_MORE || secondaryAction == ModalCardAction.READ_MORE }
            ModalCardInput.ESCAPE,
            ModalCardInput.BACKSPACE,
            -> secondaryAction.takeIf { action -> action == ModalCardAction.CANCEL || action == ModalCardAction.CLOSE }
        }

    companion object {
        fun routePreview(
            stableKey: String,
            title: RenderTextTokenSnapshot,
            iconKey: String?,
            summary: RenderTextTokenSnapshot?,
            detailLines: List<RenderTextTokenSnapshot>,
            rewardLines: List<RenderTextTokenSnapshot>,
        ): ModalCardModel =
            ModalCardModel(
                stableKey = stableKey,
                title = title,
                iconKey = iconKey,
                summary = summary,
                detailLines = detailLines,
                rewardLines = rewardLines,
                primaryAction = ModalCardAction.ENTER_ROUTE,
                secondaryAction = ModalCardAction.CANCEL,
            )

        fun shopOffer(
            shopId: String,
            offer: ShopOfferSnapshot,
        ): ModalCardModel =
            ModalCardModel(
                stableKey = "shop:$shopId:offer:${offer.index}",
                title = RenderTextTokenSnapshot(offer.labelKey),
                iconKey = null,
                summary = null,
                detailLines = offer.tagLabelKeys.map(::RenderTextTokenSnapshot),
                costLines = listOf(shardCostToken(offer.price)),
                primaryAction = ModalCardAction.BUY,
                secondaryAction = ModalCardAction.CANCEL,
            )

        fun shopSellEntry(
            shopId: String,
            displayIndex: Int,
            item: ItemRenderSnapshot?,
            price: Int,
        ): ModalCardModel =
            ModalCardModel(
                stableKey = "shop:$shopId:sell:$displayIndex",
                title = item?.displayName ?: RenderTextTokenSnapshot(item?.nameKey ?: "ui.empty.inventory.title"),
                iconKey = item?.iconKey,
                summary = null,
                detailLines = listOfNotNull(item?.descKey?.let(::RenderTextTokenSnapshot)),
                rewardLines = listOf(shardCostToken(price)),
                disabledReason = if (item == null) RenderTextTokenSnapshot("ui.empty.inventory.detail") else null,
                primaryAction = ModalCardAction.SELL,
                secondaryAction = ModalCardAction.CANCEL,
            )

        fun rewardPresentation(
            index: Int,
            entry: RewardPresentationEntrySnapshot,
        ): ModalCardModel =
            ModalCardModel(
                stableKey = "reward:${entry.source.name.lowercase()}:$index:${entry.itemDisplayName.key}",
                title = entry.itemDisplayName,
                iconKey = null,
                summary = RenderTextTokenSnapshot(entry.sourceLabelKey),
                detailLines = listOfNotNull(entry.detailText),
                primaryAction = ModalCardAction.CLOSE,
                secondaryAction = ModalCardAction.CLOSE,
                returnPolicy = ModalCardReturnPolicy.READ_ONLY_POPPABLE,
            )

        private fun shardCostToken(amount: Int): RenderTextTokenSnapshot =
            RenderTextTokenSnapshot(
                key = "ui.modal.card.cost.shards",
                arguments = listOf(RenderTextArgumentSnapshot(name = "amount", value = amount.toString())),
            )
    }
}

private fun ModalCardAction.isTerminalOnly(): Boolean =
    this == ModalCardAction.CLOSE || this == ModalCardAction.CANCEL
