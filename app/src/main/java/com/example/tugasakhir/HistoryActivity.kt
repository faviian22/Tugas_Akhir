package com.example.tugasakhir

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        db = FirebaseFirestore.getInstance()

        tvMulai = findViewById(R.id.tvMulai)
        tvSelesai = findViewById(R.id.tvSelesai)
        tvInfo = findViewById(R.id.tvInfo)
        rvHistory = findViewById(R.id.rvHistory)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(list)
        rvHistory.adapter = adapter

        // pilih tanggal
        tvMulai.setOnClickListener { pickDate { ts, label ->
            mulai = ts
            tvMulai.text = label
        }}

        tvSelesai.setOnClickListener { pickDate { ts, label ->
            selesai = ts
            tvSelesai.text = label
        }}

        // cari
        findViewById<Button>(R.id.btnCari).setOnClickListener {
            if (mulai == null || selesai == null) {
                Toast.makeText(this, "Pilih tanggal dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadByRange(mulai!!, selesai!!)
        }

        // reset
        findViewById<Button>(R.id.btnBatal).setOnClickListener {
            loadAll()
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

    private fun loadAll() {
        db.collection("history")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener {
                list.clear()

                for (doc in it) {
                    list.add(
                        HistoryModel(
                            lokasi = doc.getString("lokasi") ?: "",
                            lat = doc.getDouble("latitude") ?: 0.0,
                            lng = doc.getDouble("longitude") ?: 0.0,
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    )
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun loadByRange(start: Long, end: Long) {

        val endCal = Calendar.getInstance().apply {
            timeInMillis = end
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        db.collection("history")
            .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(Date(start)))
            .whereLessThanOrEqualTo("timestamp", com.google.firebase.Timestamp(Date(endCal.timeInMillis)))
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener {
                list.clear()

                for (doc in it) {
                    list.add(
                        HistoryModel(
                            lokasi = doc.getString("lokasi") ?: "",
                            lat = doc.getDouble("latitude") ?: 0.0,
                            lng = doc.getDouble("longitude") ?: 0.0,
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    )
                }

                adapter.notifyDataSetChanged()
            }
    }
}