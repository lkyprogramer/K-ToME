package com.ktome.core.combat

import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationPolicyTest {
    @Test
    fun `self and instant policies auto apply`() {
        val random = TestRandomSource()

        val selfAuto =
            ApplicationPolicyResolver.resolve(
                request = StatusApplicationRequest("self", 2, ApplicationPolicy.SELF_AUTO),
                hitSucceeded = false,
                random = random,
            )
        val instant =
            ApplicationPolicyResolver.resolve(
                request = StatusApplicationRequest("instant", 1, ApplicationPolicy.INSTANT_ACTION),
                hitSucceeded = false,
                random = random,
            )

        assertTrue(selfAuto.applied)
        assertTrue(instant.applied)
        assertEquals("SELF_AUTO", selfAuto.reasonTag)
        assertEquals("INSTANT_ACTION", instant.reasonTag)
    }

    @Test
    fun `hostile hit then save respects the miss gate while save only does not`() {
        val hitThenSave =
            ApplicationPolicyResolver.resolve(
                request =
                    StatusApplicationRequest(
                        statusId = "stun",
                        duration = 2,
                        applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
                        saveDimension = SaveDimension.PHYSICAL,
                        power = 30,
                        save = 10,
                    ),
                hitSucceeded = false,
                random = TestRandomSource(),
            )
        val saveOnly =
            ApplicationPolicyResolver.resolve(
                request =
                    StatusApplicationRequest(
                        statusId = "mark",
                        duration = 3,
                        applicationPolicy = ApplicationPolicy.HOSTILE_SAVE_ONLY,
                        saveDimension = SaveDimension.SPELL,
                        power = 30,
                        save = 10,
                    ),
                hitSucceeded = false,
                random = TestRandomSource(doubles = listOf(0.0)),
            )

        assertFalse(hitThenSave.attempted)
        assertEquals("MISS_GATE", hitThenSave.reasonTag)
        assertTrue(saveOnly.attempted)
        assertTrue(saveOnly.applied)
    }

    @Test
    fun `tag auto only applies when the declared tag path matched`() {
        val matched =
            ApplicationPolicyResolver.resolve(
                request = StatusApplicationRequest("bane", 4, ApplicationPolicy.TAG_AUTO, tagMatched = true),
                hitSucceeded = true,
                random = TestRandomSource(),
            )
        val skipped =
            ApplicationPolicyResolver.resolve(
                request = StatusApplicationRequest("bane", 4, ApplicationPolicy.TAG_AUTO, tagMatched = false),
                hitSucceeded = true,
                random = TestRandomSource(),
            )

        assertTrue(matched.applied)
        assertFalse(skipped.attempted)
        assertEquals("TAG_MISMATCH", skipped.reasonTag)
    }
}
