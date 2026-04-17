package com.example.tugasakhir

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private var marker: Marker? = null
    private var lastLatLng: LatLng? = null

    private lateinit var tvLokasi: TextView
    private lateinit var tvWaktu: TextView

    private lateinit var btnLokasi: ImageView
    private lateinit var btnHistori: ImageView
    private lateinit var btnProfil: ImageView

    private var lastCrashStatus = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
        private const val NOTIF_PERMISSION_REQUEST = 101
        private const val CHANNEL_ID = "crash_channel"
        private const val TAG = "GPS_TRACK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLokasi = findViewById(R.id.tvLokasi)
        tvWaktu = findViewById(R.id.tvWaktu)

        btnLokasi = findViewById(R.id.btnLokasi)
        btnHistori = findViewById(R.id.btnHistori)
        btnProfil = findViewById(R.id.btnProfil)

        // 🔥 Firebase
        database = FirebaseDatabase.getInstance().getReference("gps")

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // 🔥 PERMISSION NOTIF
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERMISSION_REQUEST
            )
        }

        // 🔥 MENU (TETAP ADA)
        btnLokasi.setOnClickListener { recreate() }

        btnHistori.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        createNotificationChannel()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true

        listenGPSRealtime()
    }

    private fun listenGPSRealtime() {

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val lat = snapshot.child("latitude").value
                    ?.toString()?.replace(",", ".")?.toDoubleOrNull()

                val lng = snapshot.child("longitude").value
                    ?.toString()?.replace(",", ".")?.toDoubleOrNull()

                val lokasi = snapshot.child("namaTempat").value?.toString() ?: "-"
                val waktu = snapshot.child("waktu").value?.toString() ?: "-"
                val crash = snapshot.child("crash").getValue(Boolean::class.java) == true

                if (lat == null || lng == null) return
                if (lat == 0.0 && lng == 0.0) return

                val newPos = LatLng(lat, lng)

                // 🔥 FILTER ANTI LONCAT
                if (lastLatLng != null) {
                    val distance = FloatArray(1)
                    android.location.Location.distanceBetween(
                        lastLatLng!!.latitude, lastLatLng!!.longitude,
                        newPos.latitude, newPos.longitude,
                        distance
                    )

                    if (distance[0] > 50) {
                        Log.e(TAG, "Loncat terlalu jauh")
                        return
                    }
                }

                lastLatLng = newPos

                if (marker == null) {
                    marker = mMap.addMarker(
                        MarkerOptions().position(newPos).title("Lokasi Kendaraan")
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 16f))
                } else {
                    marker?.let { animateMarker(it, newPos) }
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(newPos))
                }

                tvLokasi.text = lokasi
                tvWaktu.text = "Sejak $waktu"

                if (crash && !lastCrashStatus) {
                    showNotification(lat, lng)
                }
                lastCrashStatus = crash
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
            }
        })
    }

    private fun animateMarker(marker: Marker, toPosition: LatLng) {
        val start = marker.position
        val handler = Handler()
        val startTime = System.currentTimeMillis()
        val duration = 1000L
        val interpolator = LinearInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val t = interpolator.getInterpolation(
                    (System.currentTimeMillis() - startTime).toFloat() / duration
                )

                val lat = (toPosition.latitude - start.latitude) * t + start.latitude
                val lng = (toPosition.longitude - start.longitude) * t + start.longitude

                marker.position = LatLng(lat, lng)

                if (t < 1.0) handler.postDelayed(this, 16)
            }
        })
    }

    private fun showNotification(lat: Double, lng: Double) {
        val intent = Intent(this@MainActivity, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
            .setContentTitle("⚠️ Terjadi Benturan!")
            .setContentText("Klik untuk melihat lokasi kejadian")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Crash Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}