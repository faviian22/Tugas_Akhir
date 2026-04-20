package com.example.tugasakhir

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*

class CrashService : Service() {

    private lateinit var database: DatabaseReference
    private var lastStatus = false

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance().getReference("sensor")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMyForeground()
        Log.d("SERVICE", "SERVICE JALAN")
        listenCrash()
        return START_STICKY
    }

    @Suppress("ForegroundServiceType")
    private fun startMyForeground() {
        val channelId = "SERVICE_CHANNEL"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoring aktif")
            .setContentText("Service berjalan...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun listenCrash() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val vibration = snapshot.child("vibration").value?.toString() ?: ""
                val crash = vibration.contains("VIBRATION", true)

                Log.d("SERVICE", "DATA: $vibration")

                if (crash && !lastStatus) {
                    Log.d("SERVICE", "KIRIM NOTIF")
                    sendNotification()
                }

                lastStatus = crash
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SERVICE", error.message)
            }
        })
    }

    private fun sendNotification() {
        val channelId = "CRASH_CHANNEL"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Crash Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        val isLogin = prefs.getBoolean("isLogin", false)

        val intent = if (isLogin) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // penting biar tidak reuse
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 GETARAN TERDETEKSI!")
            .setContentText("Segera cek kendaraan!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(999, notification)
    }
    override fun onBind(intent: Intent?): IBinder? = null
}