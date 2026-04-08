package com.example.tugasakhir

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private var mulai: Long? = null
    private var selesai: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val btnLokasi = findViewById<ImageView>(R.id.btnLokasi)
        val btnProfil = findViewById<ImageView>(R.id.btnProfil)

        val tvMulai = findViewById<TextView>(R.id.tvMulai)
        val tvSelesai = findViewById<TextView>(R.id.tvSelesai)
        val btnCari = findViewById<Button>(R.id.btnCari)
        val btnBatal = findViewById<Button>(R.id.btnBatal)
        val tvData = findViewById<TextView>(R.id.tvData)

        val format = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        //  TANGGAL MULAI
        tvMulai.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                mulai = cal.timeInMillis
                tvMulai.text = format.format(cal.time)
            },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // TANGGAL SELESAI
        tvSelesai.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selesai = cal.timeInMillis
                tvSelesai.text = format.format(cal.time)
            },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // BUTTON CARI
        btnCari.setOnClickListener {

            if (mulai == null || selesai == null) {
                tvData.text = "Menampilkan semua histori"
                loadAllHistory()
            } else {
                tvData.text = "Filter dari ${tvMulai.text} sampai ${tvSelesai.text}"
                loadHistoryByRange(mulai!!, selesai!!)
            }
        }

        // BUTTON BATAL
        btnBatal.setOnClickListener {

            mulai = null
            selesai = null

            tvMulai.text = "Mulai"
            tvSelesai.text = "Selesai"

            tvData.text = "Menampilkan semua histori"
            loadAllHistory()
        }

        // DEFAULT LOAD
        loadAllHistory()

        // NAVIGASI
        btnLokasi.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    // LOAD SEMUA DATA
    private fun loadAllHistory() {
        // TODO: ambil semua histori dari database / alat GPS
    }

    // LOAD DATA BERDASARKAN RANGE
    private fun loadHistoryByRange(start: Long, end: Long) {
        // TODO: filter histori berdasarkan tanggal mulai - selesai
    }
}