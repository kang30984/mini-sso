package io.github.kang30984.minisso.application.fake

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(private var instant: Instant) : Clock() {
    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }

    override fun instant(): Instant = instant

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this
}
