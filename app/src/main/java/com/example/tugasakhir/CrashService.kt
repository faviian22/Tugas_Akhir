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
    private var lastStatus = false // untuk mencegah notif berulang

    override fun onCreate() {
        super.onCreate()
        // ambil reference ke Firebase "sensor"
        database = FirebaseDatabase.getInstance().getReference("sensor")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMyForeground() // wajib biar service tidak mati
        Log.d("SERVICE", "SERVICE JALAN")
        listenCrash() // mulai listen data Firebase
        return START_STICKY
    }

    // Foreground service (biar tetap aktif di background)
    @Suppress("ForegroundServiceType")
    private fun startMyForeground() {
        val channelId = "SERVICE_CHANNEL"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // buat channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        // notifikasi kecil (biar service jalan terus)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoring aktif")
            .setContentText("Service berjalan...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    // listen perubahan data dari Firebase
    private fun listenCrash() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // ambil data vibration dari Firebase
                val vibration = snapshot.child("vibration").value?.toString() ?: ""
                val crash = vibration.contains("VIBRATION", true) // cek apakah ada getaran

                Log.d("SERVICE", "DATA: $vibration")

                // kirim notif hanya saat perubahan (biar tidak spam)
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

    // fungsi kirim notifikasi
    private fun sendNotification() {
        val channelId = "CRASH_CHANNEL"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // channel notif (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Crash Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // cek user sudah login atau belum
        val prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        val isLogin = prefs.getBoolean("isLogin", false)

        // arahkan ke activity sesuai kondisi login
        val intent = if (isLogin) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // biar tidak bentrok notif lama
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // isi notifikasi
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 GETARAN TERDETEKSI!")
            .setContentText("Segera cek kendaraan!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(999, notification) // tampilkan notif
    }

    override fun onBind(intent: Intent?): IBinder? = null
}