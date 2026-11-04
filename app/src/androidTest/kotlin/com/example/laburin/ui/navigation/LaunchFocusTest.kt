package com.example.laburin.ui.navigation

import android.Manifest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.laburin.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Up to Android 8.1 the system gives initial focus to the first focusable view, even in touch mode.
 * The map must open with the list visible, not with the search field focused and the keyboard covering it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LaunchFocusTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION))
        .around(composeRule)

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun theMapOpensWithoutFocusingTheSearchField() {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Buscar puesto").fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasSetTextAction() and isFocused()).assertCountEquals(0)
    }
}
