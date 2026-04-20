package com.example.tugasakhir

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private var mulai: Long? = null
    private var selesai: Long? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var tvMulai: TextView
    private lateinit var tvSelesai: TextView
    private lateinit var tvInfo: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val list = mutableListOf<HistoryModel>()
    private val format = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        db = FirebaseFirestore.getInstance()

        tvMulai   = findViewById(R.id.tvMulai)
        tvSelesai = findViewById(R.id.tvSelesai)
        tvInfo    = findViewById(R.id.tvInfo)
        rvHistory = findViewById(R.id.rvHistory)

        // Fix scroll
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.setHasFixedSize(false)
        rvHistory.isNestedScrollingEnabled = true
        adapter = HistoryAdapter(list)
        rvHistory.adapter = adapter

        tvMulai.setOnClickListener {
            pickDate { ts, label ->
                mulai = ts
                tvMulai.text = label
                tvMulai.setTextColor(resources.getColor(android.R.color.black, null))
            }
        }

        tvSelesai.setOnClickListener {
            pickDate { ts, label ->
                selesai = ts
                tvSelesai.text = label
                tvSelesai.setTextColor(resources.getColor(android.R.color.black, null))
            }
        }

        findViewById<Button>(R.id.btnCari).setOnClickListener {
            if (mulai == null || selesai == null) {
                Toast.makeText(this, "Pilih tanggal mulai dan selesai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tvInfo.text = "Filter: ${tvMulai.text} - ${tvSelesai.text}"
            loadByRange(mulai!!, selesai!!)
        }

        // Fix batal — reset semua state
        findViewById<Button>(R.id.btnBatal).setOnClickListener {
            mulai   = null
            selesai = null
            tvMulai.text  = "Mulai"
            tvSelesai.text = "Selesai"
            tvMulai.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            tvSelesai.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            tvInfo.text = "Menampilkan semua histori"
            loadAll()
        }

        findViewById<ImageView>(R.id.btnLokasi).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnProfil).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        loadAll()
    }

    private fun pickDate(onPicked: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            onPicked(cal.timeInMillis, format.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun parseSnapshot(result: com.google.firebase.firestore.QuerySnapshot) {
        list.clear()
        for (doc in result) {
            list.add(HistoryModel(
                lokasi = doc.getString("lokasi") ?: "-",
                lat    = doc.getDouble("latitude") ?: 0.0,
                lng    = doc.getDouble("longitude") ?: 0.0,
                waktu  = doc.getTimestamp("timestamp")?.toDate()?.let { sdf.format(it) } ?: "-"
            ))
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadAll() {
        db.collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { parseSnapshot(it) }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadByRange(start: Long, end: Long) {
        // Set end ke akhir hari (23:59:59)
        val endCal = Calendar.getInstance().apply {
            timeInMillis = end
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        db.collection("history")
            .whereGreaterThanOrEqualTo("timestamp", Date(start))
            .whereLessThanOrEqualTo("timestamp", Date(endCal.timeInMillis))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { parseSnapshot(it) }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }
}