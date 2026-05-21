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

        holder.tvLokasi.text = data.lokasi.ifEmpty { "-" }

        holder.tvWaktu.text = data.timestamp
            ?.toDate()
            ?.toString()
            ?: "-"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context

            val intent = Intent(context, DetailLocationActivity::class.java).apply {
                putExtra("lat", data.lat)
                putExtra("lng", data.lng)
                putExtra("waktu", holder.tvWaktu.text.toString())
            }

            context.startActivity(intent)
        }
    }
}