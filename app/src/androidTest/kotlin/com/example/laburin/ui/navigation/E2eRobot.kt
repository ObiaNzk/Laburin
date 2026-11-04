package com.example.laburin.ui.navigation

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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

/** WorkManager waits for a connection; in tests the constraint is satisfied explicitly. */
fun connectionIsBack(context: android.content.Context) {
    val workManager = androidx.work.WorkManager.getInstance(context)
    val driver = androidx.work.testing.WorkManagerTestInitHelper.getTestDriver(context)!!
    workManager.getWorkInfosForUniqueWork(com.example.laburin.data.sync.WorkManagerSyncScheduler.UNIQUE_NAME).get()
        .filter { !it.state.isFinished }
        .forEach { driver.setAllConstraintsMet(it.id) }
}

/** Replaces the whole text of the n-th editable field on screen (0-based). */
fun AndroidComposeTestRule<*, *>.replaceInField(index: Int, text: String) {
    onAllNodes(hasSetTextAction())[index].performTextReplacement(text)
}

/**
 * Permissions the app asks for along the end-to-end flows: location on the map and, from Android 13, notifications
 * after login. Granted up front so a system dialog never covers the app, whatever test ran before.
 */
fun e2ePermissions(): androidx.test.rule.GrantPermissionRule = androidx.test.rule.GrantPermissionRule.grant(
    *buildList {
        add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray(),
)
