package com.example.laburin.ui.components

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/** The calls a map view needs from the screen's lifecycle. */
internal interface PausableMap {
    fun onResume()
    fun onPause()
    fun onDetach()
}

/**
 * Lifecycle symmetry for the map: it loads tiles only between onResume and onPause, so it costs no battery or data
 * while the app is in the background, and [release] frees it when the screen leaves the composition.
 */
internal class MapLifecycleBinding(private val map: PausableMap) : LifecycleEventObserver {

    private var resumed = false

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> if (!resumed) { resumed = true; map.onResume() }
            Lifecycle.Event.ON_PAUSE -> pause()
            else -> Unit
        }
    }

    /** The screen left the composition: no more lifecycle events will come. */
    fun release() {
        pause()
        map.onDetach()
    }

    private fun pause() {
        if (resumed) { resumed = false; map.onPause() }
    }
}
