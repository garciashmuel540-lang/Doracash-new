package com.example

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.DoraemonAssistant
import com.example.ui.components.DoraemonGesture
import com.example.ui.theme.DoracashTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DoraemonAssistantRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `DoraemonAssistant toggles visibility and switches salute think celebrate gestures`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            DoracashTheme {
                var visible by remember { mutableStateOf(false) }
                var gesture by remember { mutableStateOf(DoraemonGesture.SALUTE) }

                Button(
                    onClick = { visible = !visible },
                    modifier = Modifier.testTag("trigger_overlay_button")
                ) {
                    Text("Toggle")
                }

                DoraemonAssistant(
                    visible = visible,
                    gesture = gesture,
                    thoughtText = "Hola desde el Bolsillo 4D",
                    onGestureSelect = { gesture = it },
                    onDismiss = { visible = false }
                )
            }
        }

        // Initially hidden
        composeTestRule.onNodeWithTag("doraemon_mascot_button").assertDoesNotExist()

        // Trigger visibility ON -> defaults to SALUTE AVD
        composeTestRule.onNodeWithTag("trigger_overlay_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(450)
        composeTestRule.onNodeWithTag("doraemon_mascot_button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Doraemon saludando").assertIsDisplayed()

        // Switch to THINK AVD
        composeTestRule.onNodeWithTag("gesture_think_chip").performClick()
        composeTestRule.mainClock.advanceTimeBy(450)
        composeTestRule.onNodeWithContentDescription("Doraemon pensando").assertIsDisplayed()

        // Switch to CELEBRATE AVD
        composeTestRule.onNodeWithTag("gesture_celebrate_chip").performClick()
        composeTestRule.mainClock.advanceTimeBy(450)
        composeTestRule.onNodeWithContentDescription("Doraemon celebrando").assertIsDisplayed()

        // Dismiss via close button -> hidden again
        composeTestRule.onNodeWithTag("doraemon_close_bubble_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(450)
        composeTestRule.onNodeWithTag("doraemon_mascot_button").assertDoesNotExist()
    }
}
