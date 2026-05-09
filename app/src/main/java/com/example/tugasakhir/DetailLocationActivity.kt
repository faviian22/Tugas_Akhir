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

        // ambil komponen UI
        val tvLat    = findViewById<TextView>(R.id.tvLat)
        val tvLng    = findViewById<TextView>(R.id.tvLng)
        val tvWaktu  = findViewById<TextView>(R.id.tvWaktu)
        val btnMap   = findViewById<Button>(R.id.btnMap)
        val btnBack  = findViewById<ImageView>(R.id.btnBack)
        cardDetail   = findViewById(R.id.cardDetail)
        mapContainer = findViewById(R.id.mapContainer)

        // ambil data dari intent
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)
        val waktu = intent.getStringExtra("waktu") ?: "-"

        // tampilkan data ke UI
        tvLat.text   = "%.6f".format(lat)
        tvLng.text   = "%.6f".format(lng)
        tvWaktu.text = waktu

        // tombol untuk buka peta
        btnMap.setOnClickListener {
            cardDetail.visibility   = View.GONE   // sembunyikan detail
            mapContainer.visibility = View.VISIBLE // tampilkan map

            // buat map hanya sekali
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mapContainer, mapFragment!!)
                    .commit()

                // tampilkan marker di lokasi
                mapFragment!!.getMapAsync { googleMap ->
                    val lokasi = LatLng(lat, lng)
                    googleMap.addMarker(MarkerOptions().position(lokasi).title("Lokasi"))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lokasi, 17f))
                }
            }
        }

        // tombol back
        btnBack.setOnClickListener {
            if (mapContainer.visibility == View.VISIBLE) {
                // kalau lagi di map → balik ke detail
                mapContainer.visibility = View.GONE
                cardDetail.visibility   = View.VISIBLE
            } else {
                // kalau di detail → keluar activity
                finish()
            }
        }
    }
}