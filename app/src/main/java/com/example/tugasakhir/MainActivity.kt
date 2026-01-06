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

        // Firebase
        database = FirebaseDatabase.getInstance().reference

        // GPS Device
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Load Google Maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Cek permission GPS
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

        // Ambil lokasi GPS HP sebagai posisi awal
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val posisiAwal = LatLng(location.latitude, location.longitude)

                marker = mMap.addMarker(
                    MarkerOptions()
                        .position(posisiAwal)
                        .title("Lokasi GPS")
                )!!

                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(posisiAwal, 16f)
                )
            }
        }

        // Pantau GPS realtime dari Firebase
        listenGPSRealtime()
    }

    private fun listenGPSRealtime() {
        database.child("monitoring").child("gps")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val lat = snapshot.child("lat").getValue(Double::class.java)
                    val lng = snapshot.child("lng").getValue(Double::class.java)

                    if (lat != null && lng != null && ::marker.isInitialized) {
                        val posisi = LatLng(lat, lng)
                        marker.position = posisi
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLng(posisi)
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
