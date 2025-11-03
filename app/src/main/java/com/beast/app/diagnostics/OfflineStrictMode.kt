package com.beast.app.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.StrictMode

object OfflineStrictMode {
    fun enforce(context: Context) {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy())
                .detectNetwork()
                .penaltyDeath()
                .build()
        )
    }
}
