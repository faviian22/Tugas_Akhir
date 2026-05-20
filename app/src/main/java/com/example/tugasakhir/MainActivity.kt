package com.example.tugasakhir

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference

    private var marker: Marker? = null

    private lateinit var btnLokasi: ImageView
    private lateinit var btnHistori: ImageView
    private lateinit var btnProfil: ImageView

    private lateinit var tvStatusKendaraan: TextView
    private lateinit var tvBadgeStatus: TextView

    // waktu terakhir data diterima
    private var lastUpdateTime: Long = 0

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100

        // jika lebih dari 15 detik tidak ada data = offline
        private const val TIMEOUT_OFFLINE = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLokasi = findViewById(R.id.btnLokasi)
        btnHistori = findViewById(R.id.btnHistori)
        btnProfil = findViewById(R.id.btnProfil)

        tvStatusKendaraan = findViewById(R.id.tvStatusKendaraan)
        tvBadgeStatus = findViewById(R.id.tvBadgeStatus)

        // navigasi
        btnLokasi.setOnClickListener {
            recreate()
        }

        btnHistori.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // firebase gps
        database = FirebaseDatabase.getInstance().getReference("gps")

        // google maps
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // service
        val intent = Intent(this, CrashService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // cek status realtime
        startCheckingStatus()
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        if (
            ActivityCompat.checkSelfPermission(
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

        listenGPS()
    }

    // ambil gps realtime
    private fun listenGPS() {

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val lat =
                    snapshot.child("latitude").value?.toString()?.toDoubleOrNull()

                val lng =
                    snapshot.child("longitude").value?.toString()?.toDoubleOrNull()

                if (lat == null || lng == null) {

                    setOfflineStatus()

                    return
                }

                // update waktu terakhir data masuk
                lastUpdateTime = System.currentTimeMillis()

                // status live
                tvBadgeStatus.text = "Live"
                tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_green)

                tvStatusKendaraan.text =
                    "Lat: %.5f, Lng: %.5f".format(lat, lng)

                val posisi = LatLng(lat, lng)

                if (marker == null) {

                    marker = mMap.addMarker(
                        MarkerOptions()
                            .position(posisi)
                            .title("Lokasi Kendaraan")
                    )

                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(posisi, 16f)
                    )

                } else {

                    marker?.position = posisi

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLng(posisi)
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {

                setOfflineStatus()
            }
        })
    }

    // cek apakah alat mati
    private fun startCheckingStatus() {

        val handler = Handler(Looper.getMainLooper())

        handler.post(object : Runnable {

            override fun run() {

                val now = System.currentTimeMillis()

                // jika tidak ada update > 15 detik
                if (now - lastUpdateTime > TIMEOUT_OFFLINE) {

                    setOfflineStatus()
                }

                handler.postDelayed(this, 3000)
            }
        })
    }

    // status offline
    private fun setOfflineStatus() {

        tvBadgeStatus.text = "Offline"
        tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_red)

        tvStatusKendaraan.text = "GPS tidak aktif"
    }

    // permission lokasi
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {

            recreate()
        }
    }
}