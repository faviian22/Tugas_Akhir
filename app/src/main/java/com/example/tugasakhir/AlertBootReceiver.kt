package com.example.tugasakhir

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlertBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        val shouldStart = action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == "android.intent.action.QUICKBOOT_POWERON"

        if (shouldStart && SessionManager.isLoggedIn(context)) {
            AlertServiceStarter.start(context)
        }
    }
}