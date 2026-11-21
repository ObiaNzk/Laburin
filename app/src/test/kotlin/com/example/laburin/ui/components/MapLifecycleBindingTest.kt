package com.example.laburin.ui.components

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLifecycleBindingTest {

    private class RecordingMap : PausableMap {
        val calls = mutableListOf<String>()
        override fun onResume() { calls += "resume" }
        override fun onPause() { calls += "pause" }
        override fun onDetach() { calls += "detach" }
    }

    private val map = RecordingMap()
    private val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }
    private val binding = MapLifecycleBinding(map)

    @Test
    fun `the map runs only while the screen is in the foreground`() {
        owner.lifecycle.addObserver(binding)

        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        owner.lifecycle.currentState = Lifecycle.State.CREATED // Home: onPause, onStop
        owner.lifecycle.currentState = Lifecycle.State.RESUMED // back: onStart, onResume

        assertEquals(listOf("resume", "pause", "resume"), map.calls)
    }

    @Test
    fun `leaving the screen while it is visible pauses the map and releases it`() {
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        owner.lifecycle.addObserver(binding)

        binding.release()

        assertEquals(listOf("resume", "pause", "detach"), map.calls)
    }

    @Test
    fun `leaving the screen from the background releases the map without pausing it twice`() {
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        owner.lifecycle.addObserver(binding)
        owner.lifecycle.currentState = Lifecycle.State.CREATED

        binding.release()

        assertEquals(listOf("resume", "pause", "detach"), map.calls)
    }
}
