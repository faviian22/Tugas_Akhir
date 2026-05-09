package com.example.tugasakhir

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private var marker: Marker? = null // marker lokasi kendaraan

    private lateinit var btnLokasi: ImageView
    private lateinit var btnHistori: ImageView
    private lateinit var btnProfil: ImageView
    private lateinit var tvStatusKendaraan: TextView
    private lateinit var tvBadgeStatus: TextView

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // inisialisasi UI
        btnLokasi = findViewById(R.id.btnLokasi)
        btnHistori = findViewById(R.id.btnHistori)
        btnProfil = findViewById(R.id.btnProfil)
        tvStatusKendaraan = findViewById(R.id.tvStatusKendaraan)
        tvBadgeStatus = findViewById(R.id.tvBadgeStatus)

        // navigasi
        btnLokasi.setOnClickListener { recreate() } // refresh halaman
        btnHistori.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        btnProfil.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        // ambil data GPS dari Firebase
        database = FirebaseDatabase.getInstance().getReference("gps")

        // setup Google Maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // jalankan service monitoring
        val intent = Intent(this, CrashService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // map siap digunakan
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // cek permission lokasi
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
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

        listenGPS() // mulai ambil data GPS realtime
    }

    // ambil data GPS dari Firebase
    private fun listenGPS() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val lat = snapshot.child("latitude").value?.toString()?.toDoubleOrNull()
                val lng = snapshot.child("longitude").value?.toString()?.toDoubleOrNull()

                if (lat == null || lng == null) {
                    // data belum ada
                    tvStatusKendaraan.text = "Sinyal tidak ditemukan"
                    tvBadgeStatus.text = "Offline"
                    tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_red)
                    return
                }

                // tampilkan status
                tvStatusKendaraan.text = "Lat: %.5f, Lng: %.5f".format(lat, lng)
                tvBadgeStatus.text = "Live"
                tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_green)

                val posisi = LatLng(lat, lng)

                // update marker di map
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

            override fun onCancelled(error: DatabaseError) {
                tvStatusKendaraan.text = "Gagal memuat data"
                tvBadgeStatus.text = "Error"
                tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_red)
            }
        })
    }

    // hasil permission lokasi
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
            recreate() // reload map kalau izin diberikan
        }
    }
}