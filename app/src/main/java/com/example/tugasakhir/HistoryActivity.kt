package com.example.tugasakhir

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val btnLokasi = findViewById<ImageView>(R.id.btnLokasi)
        val btnProfil = findViewById<ImageView>(R.id.btnProfil)

        btnLokasi.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}