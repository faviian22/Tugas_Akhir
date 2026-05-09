package com.example.tugasakhir

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter untuk menampilkan list history di RecyclerView
class HistoryAdapter(private val list: List<HistoryModel>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    // ViewHolder: menyimpan komponen item
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLokasi: TextView = view.findViewById(R.id.tvLokasi)
        val tvWaktu: TextView = view.findViewById(R.id.tvWaktu)
    }

    // membuat tampilan item dari layout XML
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    // jumlah data
    override fun getItemCount() = list.size

    // isi data ke setiap item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]

        holder.tvLokasi.text = data.lokasi
        holder.tvWaktu.text = data.waktu

        // klik item → buka detail lokasi (map)
        holder.itemView.setOnClickListener {
            val intent = Intent(it.context, DetailLocationActivity::class.java).apply {
                putExtra("lat", data.lat)
                putExtra("lng", data.lng)
                putExtra("waktu", data.waktu)
            }
            it.context.startActivity(intent)
        }
    }
}