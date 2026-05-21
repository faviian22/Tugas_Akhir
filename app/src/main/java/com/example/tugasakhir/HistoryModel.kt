package com.example.tugasakhir

import com.google.firebase.Timestamp

data class HistoryModel(
    val lokasi: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Timestamp? = null
)