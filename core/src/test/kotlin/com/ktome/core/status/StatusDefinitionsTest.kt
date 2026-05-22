package com.ktome.core.status

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusDefinitionsTest {
    @Test
    fun `runtime status definitions keep status icon family separate from skills`() {
        val offFamilyIcons =
            StatusEffectType.values()
                .filterNot { type -> type == StatusEffectType.CUSTOM }
                .mapNotNull { type ->
                    StatusDefinitions.definitionFor(type).iconKey?.let { iconKey -> type to iconKey }
                }
                .filterNot { (_, iconKey) -> iconKey.startsWith("icon.status.") }

        assertTrue(
            offFamilyIcons.isEmpty(),
            "Runtime status definitions must use icon.status.* keys: $offFamilyIcons",
        )
    }
}
