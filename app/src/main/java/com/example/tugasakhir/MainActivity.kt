package com.example.tugasakhir

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference

    private var marker: Marker? = null

    private lateinit var btnLokasi: View
    private lateinit var btnHistori: View
    private lateinit var btnProfil: View
    private lateinit var btnCenterMap: View

    private lateinit var tvStatusKendaraan: TextView
    private lateinit var tvSpeed: TextView

    private lateinit var badgeStatusView: View
    private var badgeStatusText: TextView? = null

    private var lastUpdateTime: Long = 0L

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
        private const val NOTIFICATION_PERMISSION_REQUEST = 200
        private const val TIMEOUT_OFFLINE = 3000L

        // FIX: Filter GPS noise — kecepatan di bawah threshold ditampilkan sebagai 0
        private const val SPEED_NOISE_THRESHOLD_KMH = 2.5
    }

    private val statusChecker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (lastUpdateTime == 0L || now - lastUpdateTime > TIMEOUT_OFFLINE) {
                setOfflineStatus()
            }
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.redirectToLoginIfNeeded(this)) return

        setContentView(R.layout.activity_main)

        AlertServiceStarter.start(this)

        btnLokasi = findViewById(R.id.btnLokasi)
        btnHistori = findViewById(R.id.btnHistori)
        btnProfil = findViewById(R.id.btnProfil)
        btnCenterMap = findViewById(R.id.btnCenterMap)

        tvStatusKendaraan = findViewById(R.id.tvStatusKendaraan)
        tvSpeed = findViewById(R.id.tvSpeed)

        badgeStatusView = findViewById(R.id.tvBadgeStatus)
        badgeStatusText = findTextViewInside(badgeStatusView)

        database = FirebaseDatabase.getInstance().getReference("gps")

        btnLokasi.setOnClickListener {
            recreate()
        }

        btnHistori.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnCenterMap.setOnClickListener {
            val currentPosition = marker?.position

            if (currentPosition != null && ::mMap.isInitialized) {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentPosition, 16f)
                )
            } else {
                Toast.makeText(
                    this,
                    "Lokasi kendaraan belum tersedia",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

        if (mapFragment == null) {
            Toast.makeText(
                this,
                "Peta tidak ditemukan pada layout activity_main",
                Toast.LENGTH_SHORT
            ).show()

            setOfflineStatus()
        } else {
            mapFragment.getMapAsync(this)
        }

        /*
         * Permission dan optimasi baterai tetap diminta dari Activity,
         * karena foreground service membutuhkan izin notifikasi agar peringatan tampil.
         */
        requestNotificationPermissionIfNeeded()
        requestIgnoreBatteryOptimizationIfNeeded()

        /*
         * Mulai pengecekan status kendaraan pada halaman utama.
         */
        startCheckingStatus()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }

        listenGPS()
    }

    private fun listenGPS() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").value?.toString()?.toDoubleOrNull()
                    ?: snapshot.child("lat").value?.toString()?.toDoubleOrNull()

                val lng = snapshot.child("longitude").value?.toString()?.toDoubleOrNull()
                    ?: snapshot.child("lng").value?.toString()?.toDoubleOrNull()

                if (lat == null || lng == null) {
                    setOfflineStatus()
                    return
                }

                lastUpdateTime = System.currentTimeMillis()
                setOnlineStatus()

                // FIX: Terapkan threshold agar GPS noise tidak tampil sebagai kecepatan
                val rawSpeedKmh = readSpeedKmh(snapshot) ?: 0.0
                val displaySpeed = if (rawSpeedKmh < SPEED_NOISE_THRESHOLD_KMH) 0.0 else rawSpeedKmh
                tvSpeed.text = "%.0f".format(displaySpeed)

                tvStatusKendaraan.text = "Lokasi kendaraan diperbarui"

                val posisi = LatLng(lat, lng)

                if (::mMap.isInitialized) {
                    if (marker == null) {
                        marker = mMap.addMarker(
                            MarkerOptions().position(posisi).title("Lokasi Kendaraan")
                        )
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posisi, 16f))
                    } else {
                        marker?.position = posisi
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(posisi))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                setOfflineStatus()
            }
        })
    }

    private fun startCheckingStatus() {
        handler.removeCallbacks(statusChecker)
        handler.post(statusChecker)
    }

    private fun setOnlineStatus() {
        // FIX: Teks badge sesuai XML yaitu "LIVE" bukan "Live"
        badgeStatusText?.text = "LIVE"
        try {
            badgeStatusView.setBackgroundResource(R.drawable.bg_badge_live)
        } catch (_: Exception) {}
    }

    private fun setOfflineStatus() {
        badgeStatusText?.text = "Offline"
        try {
            badgeStatusView.setBackgroundResource(R.drawable.bg_badge_red)
        } catch (_: Exception) {}
        tvSpeed.text = "0"
        tvStatusKendaraan.text = "GPS tidak aktif"
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun startAlertMonitorService() {
        AlertServiceStarter.start(this)
    }

    private fun requestIgnoreBatteryOptimizationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    private fun readSpeedKmh(snapshot: DataSnapshot): Double? {
        val speedMps = readDouble(snapshot, listOf("speed_mps", "speedMps", "gps/speed_mps"))
        if (speedMps != null) return speedMps * 3.6

        return readDouble(
            snapshot,
            listOf("speedKmh", "kecepatanKmh", "kecepatan", "kmh", "speed", "gps/speed")
        )
    }

    private fun readDouble(snapshot: DataSnapshot, paths: List<String>): Double? {
        for (path in paths) {
            var current = snapshot
            for (part in path.split("/")) {
                current = current.child(part)
            }
            val result = when (val value = current.value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
            if (result != null) return result
        }
        return null
    }

    private fun findTextViewInside(view: View): TextView? {
        if (view is TextView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findTextViewInside(view.getChildAt(i))
                if (result != null) return result
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(statusChecker)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (::mMap.isInitialized &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
            }
        }
    }
}
