package com.example.tugasakhir

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

    private lateinit var tvLokasi: TextView
    private lateinit var tvWaktu: TextView

    private lateinit var btnLokasi: ImageView
    private lateinit var btnHistori: ImageView
    private lateinit var btnProfil: ImageView

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TextView
        tvLokasi = findViewById(R.id.tvLokasi)
        tvWaktu = findViewById(R.id.tvWaktu)

        // Button Menu
        btnLokasi = findViewById(R.id.btnLokasi)
        btnHistori = findViewById(R.id.btnHistori)
        btnProfil = findViewById(R.id.btnProfil)

        // Firebase
        database = FirebaseDatabase.getInstance().getReference("gps")

        // Map Fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment

        mapFragment?.getMapAsync(this)

        // =========================
        // Navigasi Menu Bawah
        // =========================

        btnLokasi.setOnClickListener {
            recreate()
        }

        btnHistori.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
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

        val posisiAwal = LatLng(-7.0, 110.0)

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(posisiAwal, 6f)
        )

        listenGPSRealtime()
    }

    // ===============================
    // Membaca GPS dari Firebase
    // ===============================

    private fun listenGPSRealtime() {

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)

                val lokasi = snapshot.child("namaTempat").value?.toString() ?: "-"
                val waktu = snapshot.child("waktu").value?.toString() ?: "-"

                if (lat != null && lng != null) {

                    val posisi = LatLng(lat, lng)

                    if (marker == null) {

                        marker = mMap.addMarker(
                            MarkerOptions()
                                .position(posisi)
                                .title("Lokasi Kendaraan")
                        )

                    } else {

                        marker?.position = posisi
                    }

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(posisi, 17f)
                    )

                    tvLokasi.text = lokasi
                    tvWaktu.text = "Sejak $waktu"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase Error: ${error.message}")
            }
        })
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
            recreate()
        }
    }
}