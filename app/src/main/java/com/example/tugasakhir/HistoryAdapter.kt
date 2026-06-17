package com.example.tugasakhir

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private val list: List<HistoryModel>,
    private val onItemClick: (HistoryModel, String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLokasi: TextView = itemView.findViewById(R.id.tvLokasi)
        val tvWaktu: TextView = itemView.findViewById(R.id.tvWaktu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]

        holder.tvLokasi.text = "${data.lokasiAwal} → ${data.lokasiAkhir}"

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateOnlySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val date = data.timestamp?.toDate()
        val formattedDate = date?.let { sdf.format(it) } ?: "-"
        val dateOnly = date?.let { dateOnlySdf.format(it) } ?: ""

        holder.tvWaktu.text = formattedDate

        holder.itemView.setOnClickListener {
            onItemClick(data, dateOnly)
        }
    }
}