package com.example.tugasakhir

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class DetailLocationActivity :
    AppCompatActivity(),
    OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference

    private lateinit var tvJarak: TextView
    private lateinit var tvSpeed: TextView

    private val pathList = ArrayList<LatLng>()

    private var totalSpeed = 0.0
    private var totalData = 0

    private var tanggal = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_location)

        tvJarak = findViewById(R.id.tvJarak)
        tvSpeed = findViewById(R.id.tvSpeed)

        tanggal = intent.getStringExtra("tanggal") ?: ""

        database = FirebaseDatabase.getInstance()
            .getReference("gps")
            .child("path")

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map)
                    as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        loadData()
    }

    private fun loadData() {

        database.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                pathList.clear()

                totalSpeed = 0.0
                totalData = 0

                for (data in snapshot.children) {

                    val lat = data.child("lat")
                        .getValue(Double::class.java)

                    val lng = data.child("lng")
                        .getValue(Double::class.java)

                    val speed = data.child("speed")
                        .getValue(Double::class.java) ?: 0.0

                    val timestamp = data.child("timestamp")
                        .getValue(Long::class.java) ?: 0L

                    val tgl =
                        SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(Date(timestamp))

                    if (tgl != tanggal)
                        continue

                    if (lat != null && lng != null) {

                        val posisi = LatLng(lat, lng)

                        pathList.add(posisi)

                        totalSpeed += speed
                        totalData++
                    }
                }

                if (pathList.isNotEmpty()) {

                    val polyline = PolylineOptions()
                        .addAll(pathList)
                        .width(12f)
                        .color(Color.BLUE)

                    mMap.addPolyline(polyline)

                    // start
                    mMap.addMarker(
                        MarkerOptions()
                            .position(pathList.first())
                            .title("Start")
                    )

                    // motor
                    mMap.addMarker(
                        MarkerOptions()
                            .position(pathList.last())
                            .title("Motor")
                    )

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            pathList.last(),
                            15f
                        )
                    )

                    val jarak = hitungJarak()

                    tvJarak.text =
                        "Jarak : %.2f KM".format(jarak)

                    val rata =
                        if (totalData > 0)
                            totalSpeed / totalData
                        else
                            0.0

                    tvSpeed.text =
                        "Rata-rata : %.1f KM/H".format(rata)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun hitungJarak(): Double {

        var total = 0.0

        for (i in 0 until pathList.size - 1) {

            total += distance(
                pathList[i],
                pathList[i + 1]
            )
        }

        return total
    }

    private fun distance(
        start: LatLng,
        end: LatLng
    ): Double {

        val radius = 6371.0

        val dLat =
            Math.toRadians(end.latitude - start.latitude)

        val dLng =
            Math.toRadians(end.longitude - start.longitude)

        val a =
            sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(start.latitude)) *
                    cos(Math.toRadians(end.latitude)) *
                    sin(dLng / 2).pow(2)

        val c =
            2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c
    }
}