package com.example

import com.example.data.model.SocialPlatform
import com.example.data.repository.RazakRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM test suite aligned for offline CI execution without external SDK artifact downloads.
 */
class ExampleRobolectricTest {

    @Test
    fun `promote platforms include all required organic social networks`() {
        assertEquals(
            listOf(
                SocialPlatform.TIKTOK,
                SocialPlatform.YOUTUBE,
                SocialPlatform.INSTAGRAM,
                SocialPlatform.FACEBOOK,
                SocialPlatform.X_TWITTER
            ),
            SocialPlatform.promotePlatforms
        )
        assertTrue(RazakRepository.BUSINESS_ACCOUNT_ID == "razak_main")
    }
}
