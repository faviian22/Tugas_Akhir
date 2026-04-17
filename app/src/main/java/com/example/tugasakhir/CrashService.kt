package com.example.tugasakhir

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*

class CrashService : Service() {

    private lateinit var database: DatabaseReference
    private var lastStatus: String? = null

    override fun onCreate() {
        super.onCreate()

        database = FirebaseDatabase.getInstance().getReference("sensor")

        try {
            startMyForegroundService()
            listenCrash()
        } catch (e: Exception) {
            Log.e("CrashService", "Error: ${e.message}")
        }
    }

    // 🔥 WAJIB untuk stabilitas service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startMyForegroundService() {

        val channelId = "SERVICE_CHANNEL"

        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoring Kendaraan")
            .setContentText("Service aktif berjalan...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    // =========================
    // LISTEN FIREBASE
    // =========================
    private fun listenCrash() {

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                try {
                    val status = snapshot.child("status")
                        .getValue(String::class.java)

                    val lat = snapshot.child("latitude")
                        .getValue(Double::class.java)

                    val lng = snapshot.child("longitude")
                        .getValue(Double::class.java)

                    if (status == null || lat == null || lng == null) return

                    // 🔥 Anti spam notifikasi
                    if ((status == "jatuh" || status == "tabrakan") && status != lastStatus) {
                        showCrashNotification(lat, lng)
                    }

                    lastStatus = status

                } catch (e: Exception) {
                    Log.e("CrashService", "Firebase Error: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CrashService", "DB Error: ${error.message}")
            }
        })
    }

    // =========================
    // NOTIFIKASI
    // =========================
    private fun showCrashNotification(lat: Double, lng: Double) {

        val channelId = "CRASH_CHANNEL"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Crash Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("lat", lat)
            putExtra("lng", lng)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 TERJADI BENTURAN!")
            .setContentText("Klik untuk melihat lokasi kejadian")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(999, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}