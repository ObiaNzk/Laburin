package com.example.laburin.domain.repository

import com.example.laburin.domain.model.NotificationEvent

fun interface Notifier {
    fun notify(event: NotificationEvent)
}
