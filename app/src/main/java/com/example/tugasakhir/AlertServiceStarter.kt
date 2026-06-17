package com.example.tugasakhir

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object AlertServiceStarter {

    fun start(context: Context) {
        val appContext = context.applicationContext

        if (!SessionManager.isLoggedIn(appContext)) {
            stop(appContext)
            return
        }

        try {
            val serviceIntent = Intent(appContext, AlertMonitorService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
        } catch (_: Exception) {
        }
    }

    fun stop(context: Context) {
        try {
            val appContext = context.applicationContext
            appContext.stopService(Intent(appContext, AlertMonitorService::class.java))
        } catch (_: Exception) {
        }
    }
}