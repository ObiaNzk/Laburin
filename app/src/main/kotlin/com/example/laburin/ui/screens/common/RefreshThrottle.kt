package com.example.laburin.ui.screens.common

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Screens ask the server again when the user comes back to them (onStart), but only if what they show is older than
 * [staleAfter]: switching apps for a few seconds must not cost data on a prepaid plan.
 */
class RefreshThrottle(private val clock: Clock, private val staleAfter: Duration = STALE_AFTER) {

    private var lastRefresh: Instant? = null

    /** Runs [refresh] and remembers when, if nothing was refreshed yet or the last refresh is stale. */
    fun ifStale(refresh: () -> Unit) {
        val now = clock.instant()
        val last = lastRefresh
        if (last != null && Duration.between(last, now) < staleAfter) return
        lastRefresh = now
        refresh()
    }

    companion object {
        val STALE_AFTER: Duration = Duration.ofMinutes(5)
    }
}
