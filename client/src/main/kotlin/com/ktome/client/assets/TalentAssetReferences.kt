package com.ktome.client.assets

import com.ktome.core.snapshot.RenderSnapshot

internal fun RenderSnapshot.forEachTalentAssetReference(
    visual: (String?) -> Unit,
    iconVisual: (String?) -> Unit,
    audio: (String?) -> Unit,
) {
    uiState.talents.forEach { talent ->
        visual(talent.visualKey)
        iconVisual(talent.iconKey)
        iconVisual(talent.damageTypeIconKey)
        audio(talent.audioProfile)
    }
    uiState.reserveTalents.forEach { talent ->
        visual(talent.visualKey)
        iconVisual(talent.iconKey)
        iconVisual(talent.damageTypeIconKey)
        audio(talent.audioProfile)
    }
    uiState.talentTrees.forEach { tree ->
        visual(tree.visualKey)
        iconVisual(tree.iconKey)
        audio(tree.audioProfile)
        tree.nodes.forEach { node ->
            visual(node.visualKey)
            iconVisual(node.iconKey)
            iconVisual(node.damageTypeIconKey)
            audio(node.audioProfile)
        }
    }
}
