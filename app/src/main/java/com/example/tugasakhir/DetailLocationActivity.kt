package com.example.tugasakhir

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class DetailLocationActivity : AppCompatActivity() {

    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private lateinit var mapContainer: FrameLayout
    private lateinit var cardDetail: CardView
    private var mapFragment: SupportMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_location)

        val tvLat    = findViewById<TextView>(R.id.tvLat)
        val tvLng    = findViewById<TextView>(R.id.tvLng)
        val tvWaktu  = findViewById<TextView>(R.id.tvWaktu)
        val btnMap   = findViewById<Button>(R.id.btnMap)
        val btnBack  = findViewById<ImageView>(R.id.btnBack)
        cardDetail   = findViewById(R.id.cardDetail)
        mapContainer = findViewById(R.id.mapContainer)

        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)
        val waktu = intent.getStringExtra("waktu") ?: "-"

        tvLat.text   = "%.6f".format(lat)
        tvLng.text   = "%.6f".format(lng)
        tvWaktu.text = waktu

        btnMap.setOnClickListener {
            cardDetail.visibility   = View.GONE
            mapContainer.visibility = View.VISIBLE

            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mapContainer, mapFragment!!)
                    .commit()

                mapFragment!!.getMapAsync { googleMap ->
                    val lokasi = LatLng(lat, lng)
                    googleMap.addMarker(MarkerOptions().position(lokasi).title("Lokasi"))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lokasi, 17f))
                }
            }
        }

        btnBack.setOnClickListener {
            if (mapContainer.visibility == View.VISIBLE) {
                mapContainer.visibility = View.GONE
                cardDetail.visibility   = View.VISIBLE
            } else {
                finish()
            }
        }
    }
}