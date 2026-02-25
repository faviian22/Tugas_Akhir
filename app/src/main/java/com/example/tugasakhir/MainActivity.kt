package com.example.tugasakhir

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private lateinit var marker: Marker
    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Koneksi ke Firebase Realtime Database
        database = FirebaseDatabase.getInstance().reference

        // Inisialisasi GPS perangkat
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Load Google Maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Cek izin lokasi
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

        // Posisi awal default (agar marker selalu ada)
        val posisiAwal = LatLng(-7.4, 112.7)
        marker = mMap.addMarker(
            MarkerOptions().position(posisiAwal).title("Lokasi Kendaraan")
        )!!

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posisiAwal, 15f))

        // Mulai baca lokasi realtime dari Firebase
        listenGPSRealtime()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
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

    // Membaca lokasi realtime dari Firebase
    private fun listenGPSRealtime() {

        database.child("gps")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val lat = snapshot.child("latitude").getValue(Double::class.java)
                    val lng = snapshot.child("longitude").getValue(Double::class.java)

                    if (lat != null && lng != null && ::marker.isInitialized) {

                        val posisi = LatLng(lat, lng)

                        // Update marker
                        marker.position = posisi

                        // Kamera mengikuti kendaraan
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(posisi, 16f)
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}