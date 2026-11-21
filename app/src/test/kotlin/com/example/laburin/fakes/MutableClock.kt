package com.example.laburin.fakes

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock tests can move forward, e.g. to simulate coming back to the app after a while. */
class MutableClock(var now: Instant = Instant.parse("2026-11-21T12:00:00Z")) : Clock() {
    fun advance(by: Duration) { now += by }
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
}
