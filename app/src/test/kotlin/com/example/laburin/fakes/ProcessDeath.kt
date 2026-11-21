package com.example.laburin.fakes

import androidx.lifecycle.SavedStateHandle

/**
 * What a new ViewModel receives after Android killed the process: only what was written to the handle survives,
 * and it must be plain values a Bundle can hold.
 */
fun SavedStateHandle.afterProcessDeath(): SavedStateHandle {
    val saved = keys().associateWith { key -> get<Any?>(key) }
    saved.forEach { (key, value) ->
        require(value == null || value is String) { "$key holds ${value!!::class.simpleName}; save plain strings" }
    }
    return SavedStateHandle(saved)
}
