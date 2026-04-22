package com.ktome.client.ui.item

import com.ktome.core.snapshot.ItemRenderSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QualityPresentationTest {
    @Test
    fun `rarity drives main color and corner glyph`() {
        val normal = QualityPresentation.from(item(qualityTierId = "NORMAL"))
        val magic = QualityPresentation.from(item(qualityTierId = "MAGIC"))
        val rare = QualityPresentation.from(item(qualityTierId = "RARE"))

        assertEquals(QualityColorTokenId.NORMAL, normal.colorTokenId)
        assertNull(normal.cornerGlyph)
        assertEquals(QualityColorTokenId.MAGIC, magic.colorTokenId)
        assertEquals("\u25C6", magic.cornerGlyph)
        assertEquals(QualityColorTokenId.RARE, rare.colorTokenId)
        assertEquals("\u25C6\u25C6", rare.cornerGlyph)
    }

    @Test
    fun `special tier adds accent without replacing rarity main color`() {
        val unique =
            QualityPresentation.from(
                item(
                    qualityTierId = "RARE",
                    specialTemplateId = "unique.thornpath_crook",
                    specialTierId = "UNIQUE",
                ),
            )
        val artifact =
            QualityPresentation.from(
                item(
                    qualityTierId = "MAGIC",
                    specialTemplateId = "artifact.vesper_prism",
                    specialTierId = "ARTIFACT",
                ),
            )

        assertEquals(QualityColorTokenId.RARE, unique.colorTokenId)
        assertEquals(SpecialAccentTokenId.UNIQUE, unique.specialAccentTokenId)
        assertEquals(QualityColorTokenId.MAGIC, artifact.colorTokenId)
        assertEquals(SpecialAccentTokenId.ARTIFACT, artifact.specialAccentTokenId)
    }

    @Test
    fun `unknown quality fails fast`() {
        assertThrows(IllegalStateException::class.java) {
            QualityPresentation.from(item(qualityTierId = "LEGENDARY"))
        }
    }

    private fun item(
        qualityTierId: String,
        specialTemplateId: String? = null,
        specialTierId: String? = null,
    ): ItemRenderSnapshot =
        ItemRenderSnapshot(
            baseItemId = "short_sword",
            specialTemplateId = specialTemplateId,
            specialTierId = specialTierId,
            nameKey = "item.short_sword.name",
            typeId = "WEAPON",
            qualityTierId = qualityTierId,
        )
}
