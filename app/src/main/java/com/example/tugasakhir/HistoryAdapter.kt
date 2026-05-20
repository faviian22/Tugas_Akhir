package com.example.tugasakhir

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val list: List<HistoryModel>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLokasi: TextView = view.findViewById(R.id.tvLokasi)
        val tvWaktu: TextView = view.findViewById(R.id.tvWaktu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val data = list[position]

        holder.tvLokasi.text = data.lokasi
        holder.tvWaktu.text = data.waktu

        holder.itemView.setOnClickListener {

            val intent = Intent(it.context, DetailLocationActivity::class.java)

            intent.putExtra("lat", data.lat)
            intent.putExtra("lng", data.lng)
            intent.putExtra("waktu", data.waktu)
            intent.putExtra("lokasi", data.lokasi)

            it.context.startActivity(intent)
        }
    }
}