package com.jarvis.aegis.session

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.time.Instant

class DeviceTimeSource(private val context: Context) {
    fun snapshot(): ClockSnapshot = ClockSnapshot(
        wallTime = Instant.now(),
        elapsedRealtimeMs = SystemClock.elapsedRealtime(),
        bootCount = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(-1),
    )

    fun anchor(): TimeAnchor = snapshot().let { TimeAnchor(it.wallTime, it.elapsedRealtimeMs, it.bootCount) }
}
