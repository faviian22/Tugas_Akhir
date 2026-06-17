package com.example.tugasakhir

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AlertMonitorService : Service() {

    private lateinit var database: DatabaseReference
    private var sensorListener: ValueEventListener? = null

    /*
     * Snapshot pertama setelah login hanya dijadikan data awal.
     * Notifikasi tidak ditampilkan dari data lama Firebase.
     */
    private var initialSnapshotLoaded = false

    private var lastLidarDistance: Double? = null
    private var lastSpeedKmh: Double? = null
    private var lastMpuAz: Double? = null
    private var lastVibration: String? = null

    companion object {
        private const val MONITOR_CHANNEL_ID = "vehicle_monitor_channel_realtime_v1"
        private const val WARNING_CHANNEL_ID = "vehicle_warning_channel_realtime_v1"

        private const val MONITOR_NOTIFICATION_ID = 100
        private const val SAFE_DISTANCE_NOTIFICATION_ID = 101
        private const val MPU_AZ_NOTIFICATION_ID = 102
        private const val VIBRATION_NOTIFICATION_ID = 103

        /*
         * LiDAR:
         * 100 = 1 meter
         * 800 = 8 meter
         *
         * Notifikasi muncul jika:
         * jarak <= 800 dan kecepatan >= 30 km/jam.
         */
        private const val LIDAR_LIMIT_RAW = 800.0
        private const val SPEED_LIMIT_KMH = 30.0

        /*
         * MPU:
         * Ambil AZ saja.
         * Notifikasi muncul jika AZ 10.0 sampai 20.0.
         */
        private const val MPU_AZ_MIN_LIMIT = 10.0
        private const val MPU_AZ_MAX_LIMIT = 20.0
    }

    override fun onCreate() {
        super.onCreate()

        if (!SessionManager.isLoggedIn(this)) {
            stopSelf()
            return
        }

        createNotificationChannels()

        try {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                buildMonitorNotification()
            )
        } catch (_: Exception) {
            stopSelf()
            return
        }

        database = FirebaseDatabase.getInstance().reference
        startSensorMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SessionManager.isLoggedIn(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!::database.isInitialized) {
            database = FirebaseDatabase.getInstance().reference
        }

        if (sensorListener == null) {
            startSensorMonitoring()
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        if (SessionManager.isLoggedIn(this)) {
            AlertServiceStarter.start(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::database.isInitialized) {
            sensorListener?.let { listener ->
                database.removeEventListener(listener)
            }
        }

        sensorListener = null
        super.onDestroy()
    }

    private fun startSensorMonitoring() {
        if (!SessionManager.isLoggedIn(this)) {
            stopSelf()
            return
        }

        if (sensorListener != null) return

        sensorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!SessionManager.isLoggedIn(this@AlertMonitorService)) {
                    stopSelf()
                    return
                }

                handleRealtimeSnapshot(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                // Tidak dibuat crash jika Firebase gagal membaca data.
            }
        }

        database.addValueEventListener(sensorListener!!)
    }

    private fun handleRealtimeSnapshot(snapshot: DataSnapshot) {
        val currentLidar = readDouble(
            snapshot,
            listOf(
                "lidar/distance",
                "lidar",
                "sensor/lidar",
                "sensor/lidar/distance",
                "gps/lidar",
                "gps/lidar/distance",
                "gps/sensor/lidar",
                "gps/sensor/lidar/distance",
                "distance",
                "jarak",
                "Jarak",
                "gps/jarak",
                "gps/Jarak",
                "sensor/jarak"
            )
        )

        val currentSpeed = readSpeedKmh(snapshot)

        val currentAz = readDouble(
            snapshot,
            listOf(
                "sensor/mpu/az",
                "mpu/az",
                "gps/mpu/az",
                "gps/sensor/mpu/az",
                "az",
                "AZ"
            )
        )

        val currentVibration = readString(
            snapshot,
            listOf(
                "sensor/vibration",
                "vibration",
                "gps/vibration",
                "gps/sensor/vibration",
                "sensor/getar",
                "getar",
                "gps/getar",
                "Getar"
            )
        )

        /*
         * Snapshot pertama setelah login jangan menampilkan notifikasi.
         * Ini mencegah notifikasi dari data Firebase lama.
         */
        if (!initialSnapshotLoaded) {
            lastLidarDistance = currentLidar
            lastSpeedKmh = currentSpeed
            lastMpuAz = currentAz
            lastVibration = currentVibration
            initialSnapshotLoaded = true
            return
        }

        checkSafeDistanceRealtime(
            currentDistance = currentLidar,
            currentSpeed = currentSpeed
        )

        checkMpuAzRealtime(
            currentAz = currentAz
        )

        checkVibrationRealtime(
            currentVibration = currentVibration
        )

        lastLidarDistance = currentLidar
        lastSpeedKmh = currentSpeed
        lastMpuAz = currentAz
        lastVibration = currentVibration
    }

    private fun checkSafeDistanceRealtime(
        currentDistance: Double?,
        currentSpeed: Double?
    ) {
        if (currentDistance == null || currentSpeed == null) return

        val lidarChanged = lastLidarDistance == null || currentDistance != lastLidarDistance
        val speedChanged = lastSpeedKmh == null || currentSpeed != lastSpeedKmh

        if (!lidarChanged && !speedChanged) return

        val distanceMeter = currentDistance / 100.0

        val isDanger =
            currentDistance > 0.0 &&
                    currentDistance <= LIDAR_LIMIT_RAW &&
                    currentSpeed >= SPEED_LIMIT_KMH

        if (isDanger) {
            showWarningNotification(
                notificationId = SAFE_DISTANCE_NOTIFICATION_ID,
                title = "⚠️ Peringatan Jarak Aman",
                message = "Jarak objek %.1f meter dan kecepatan %.1f km/jam. Jaga jarak aman."
                    .format(distanceMeter, currentSpeed)
            )
        }
    }

    private fun checkMpuAzRealtime(currentAz: Double?) {
        if (currentAz == null) return

        val azChanged = lastMpuAz == null || currentAz != lastMpuAz
        if (!azChanged) return

        val isDanger =
            currentAz >= MPU_AZ_MIN_LIMIT &&
                    currentAz <= MPU_AZ_MAX_LIMIT

        if (isDanger) {
            showWarningNotification(
                notificationId = MPU_AZ_NOTIFICATION_ID,
                title = "⚠️ Peringatan Kemiringan Motor",
                message = "Nilai MPU AZ = %.2f. Motor terindikasi miring berlebih atau berpotensi roboh."
                    .format(currentAz)
            )
        }
    }

    private fun checkVibrationRealtime(currentVibration: String?) {
        if (currentVibration == null) return

        val vibrationChanged = lastVibration == null || currentVibration != lastVibration
        if (!vibrationChanged) return

        if (isVibrationDetected(currentVibration)) {
            showWarningNotification(
                notificationId = VIBRATION_NOTIFICATION_ID,
                title = "⚠️ Peringatan Getaran",
                message = "Getaran terdeteksi pada kendaraan."
            )
        }
    }

    private fun readSpeedKmh(snapshot: DataSnapshot): Double? {
        val speedMps = readDouble(
            snapshot,
            listOf(
                "speed_mps",
                "speedMps",
                "gps/speed_mps",
                "gps/speedMps"
            )
        )

        if (speedMps != null) {
            return speedMps * 3.6
        }

        return readDouble(
            snapshot,
            listOf(
                "speedKmh",
                "kecepatanKmh",
                "kecepatan",
                "kmh",
                "speed",
                "Speed",
                "gps/speed",
                "gps/Speed",
                "gps/speedKmh",
                "gps/kecepatan",
                "gps/kecepatanKmh"
            )
        )
    }

    private fun isVibrationDetected(value: String): Boolean {
        val normalized = value.trim().lowercase()

        if (normalized.isEmpty()) return false

        return normalized !in listOf(
            "stable",
            "stabil",
            "normal",
            "aman",
            "safe",
            "false",
            "0",
            "off",
            "tidak",
            "tidak mendeteksi",
            "tidak terdeteksi",
            "none",
            "null",
            "-"
        )
    }

    private fun showWarningNotification(
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (!SessionManager.isLoggedIn(this)) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val uniqueNotificationId =
            notificationId + (System.currentTimeMillis() % 100000).toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            uniqueNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_directions_bike_24)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 450, 180, 450, 180, 650))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setTicker(title)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(uniqueNotificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+ membutuhkan izin notifikasi.
        }
    }

    private fun buildMonitorNotification(): Notification {
        return NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_directions_bike_24)
            .setContentTitle("Pemantauan kendaraan aktif")
            .setContentText("Sensor LiDAR, MPU, dan getaran sedang dipantau.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val monitorChannel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            "Pemantauan Kendaraan",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifikasi status pemantauan sensor kendaraan"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val defaultSoundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val warningChannel = NotificationChannel(
            WARNING_CHANNEL_ID,
            "Peringatan Kendaraan Prioritas Tinggi",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan jarak aman, kemiringan, dan getaran kendaraan"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 450, 180, 450, 180, 650)
            enableLights(true)
            setSound(defaultSoundUri, audioAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(monitorChannel)
        notificationManager.createNotificationChannel(warningChannel)
    }

    private fun readDouble(snapshot: DataSnapshot, paths: List<String>): Double? {
        for (path in paths) {
            val value = getChildByPath(snapshot, path).value

            val result = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }

            if (result != null) return result
        }

        return null
    }

    private fun readString(snapshot: DataSnapshot, paths: List<String>): String? {
        for (path in paths) {
            val value = getChildByPath(snapshot, path).value

            if (value != null) return value.toString()
        }

        return null
    }

    private fun getChildByPath(snapshot: DataSnapshot, path: String): DataSnapshot {
        var current = snapshot

        for (part in path.split("/")) {
            current = current.child(part)
        }

        return current
    }
}