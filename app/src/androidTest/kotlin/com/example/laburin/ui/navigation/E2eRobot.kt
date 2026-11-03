package com.example.laburin.ui.navigation

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performScrollTo

/** Small helpers shared by end-to-end tests. */
/** On timeout the error includes what was on screen, so failures explain themselves. */
fun AndroidComposeTestRule<*, *>.waitFor(text: String, timeoutMillis: Long = 15_000) {
    try {
        waitUntil(timeoutMillis) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
        throw AssertionError("'$text' did not appear. On screen: ${dumpTexts()}", e)
    }
}

private fun androidx.compose.ui.semantics.SemanticsConfiguration.flattenTexts(): List<String> =
    getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.map { it.text }.orEmpty()

private fun AndroidComposeTestRule<*, *>.dumpTexts(): List<String> =
    onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
        .flatMap { it.config.flattenTexts() }.distinct()

fun AndroidComposeTestRule<*, *>.tap(text: String) {
    val node = onNodeWithText(text)
    // Scroll only when the node lives inside a scrollable container.
    runCatching { node.performScrollTo() }
    node.performClick()
}

/** Types into the n-th editable field on screen (0-based). */
fun AndroidComposeTestRule<*, *>.typeInField(index: Int, text: String) {
    onAllNodes(hasSetTextAction())[index].performTextInput(text)
}
