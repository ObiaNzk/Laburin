package com.example.laburin.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LaburinThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Screens were designed for light colors: a dark or wallpaper-based scheme mixes white panels with dark ones. */
    @Test
    fun usesTheLightBrandColorsEvenWhenThePhoneIsInDarkMode() {
        var scheme: ColorScheme? = null
        composeRule.setContent {
            val night = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            CompositionLocalProvider(LocalConfiguration provides night) {
                LaburinTheme { scheme = MaterialTheme.colorScheme }
            }
        }

        composeRule.runOnIdle {
            assertEquals(Purple40, scheme!!.primary)
            assertEquals(LightBackground, scheme!!.background)
        }
    }
}
