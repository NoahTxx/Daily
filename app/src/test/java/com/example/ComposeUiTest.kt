package com.example

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingCheckbox
import com.example.ui.components.NothingPill
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.LocalAccentColor
import com.example.util.LocalAppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNothingStyleComponentsRenderingAndClick() {
        var checked = false

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                NothingCard {
                    NothingCheckbox(
                        checked = checked,
                        onCheckedChange = { checked = !checked },
                        testTag = "test_checkbox"
                    )
                    NothingPill(
                        text = "Gesundheit",
                        isAccent = true
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("test_checkbox").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gesundheit").assertIsDisplayed()

        composeTestRule.onNodeWithTag("test_checkbox").performClick()
        assertTrue(checked)
    }

    @Test
    fun testLocalizationStringsInGermanAndEnglish() {
        val germanHeader = AppStrings.get("today_header", AppLanguage.DE)
        assertEquals("DAILY", germanHeader)

        val englishHeader = AppStrings.get("today_header", AppLanguage.EN)
        assertEquals("DAILY", englishHeader)

        val deToday = AppStrings.get("nav_today", AppLanguage.DE)
        assertEquals("HEUTE", deToday)

        val enToday = AppStrings.get("nav_today", AppLanguage.EN)
        assertEquals("TODAY", enToday)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.DE,
                LocalAccentColor provides Color.Red
            ) {
                NothingCard {
                    Text(text = AppStrings.get("nav_today", AppLanguage.DE))
                }
            }
        }

        composeTestRule.onNodeWithText("HEUTE").assertIsDisplayed()
    }
}
