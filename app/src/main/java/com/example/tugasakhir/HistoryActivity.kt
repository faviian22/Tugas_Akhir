package com.example.tugasakhir

import android.app.DatePickerDialog
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class HistoryActivity : AppCompatActivity() {

    private var mulai: Long? = null
    private var selesai: Long? = null

    private lateinit var db: FirebaseFirestore

    private lateinit var viewMulai: View
    private lateinit var viewSelesai: View
    private var textMulai: TextView? = null
    private var textSelesai: TextView? = null

    private lateinit var tvInfo: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: HistoryAdapter

    private val list = mutableListOf<HistoryModel>()
    private val routeStatsMap = mutableMapOf<Long, RouteStats>()

    private val formatDisplay = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val formatDateOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val formatTrackDoc = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Dibuat lebih longgar agar rute tidak mudah terpecah saat GPS terlambat mengirim data.
    private val routeIdleTimeoutMs = 10 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.redirectToLoginIfNeeded(this)) return

        setContentView(R.layout.activity_history)

        db = FirebaseFirestore.getInstance()

        viewMulai = findViewById(R.id.tvMulai)
        viewSelesai = findViewById(R.id.tvSelesai)

        textMulai = findTextViewInside(viewMulai)
        textSelesai = findTextViewInside(viewSelesai)

        tvInfo = findViewById(R.id.tvInfo)
        rvHistory = findViewById(R.id.rvHistory)

        rvHistory.layoutManager = LinearLayoutManager(this)

        adapter = HistoryAdapter(list) { model, dateString ->
            val routeKey = model.timestamp?.toDate()?.time ?: 0L
            val stats = routeStatsMap[routeKey]

            val intent = Intent(this, DetailLocationActivity::class.java)
            intent.putExtra("startLat", model.startLat)
            intent.putExtra("startLng", model.startLng)
            intent.putExtra("endLat", model.endLat)
            intent.putExtra("endLng", model.endLng)
            intent.putExtra("waktu", dateString)
            intent.putExtra("jarakKm", stats?.totalDistanceKm ?: 0.0)
            intent.putExtra("kecepatanKmh", stats?.avgSpeedKmh ?: 0.0)
            intent.putExtra("durasiText", stats?.durationText ?: "0 menit")
            intent.putExtra("routeStartTime", stats?.startTime ?: 0L)
            intent.putExtra("routeEndTime", stats?.endTime ?: 0L)

            startActivity(intent)
        }

        rvHistory.adapter = adapter

        viewMulai.setOnClickListener {
            pickDate { ts, label ->
                mulai = ts
                setDateText(viewMulai, textMulai, label)
            }
        }

        viewSelesai.setOnClickListener {
            pickDate { ts, label ->
                selesai = ts
                setDateText(viewSelesai, textSelesai, label)
            }
        }

        findViewById<Button>(R.id.btnCari).setOnClickListener {
            if (mulai == null || selesai == null) {
                Toast.makeText(
                    this,
                    "Pilih tanggal mulai dan selesai terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            loadByRange(mulai!!, selesai!!)
        }

        findViewById<Button>(R.id.btnBatal).setOnClickListener {
            mulai = null
            selesai = null

            setDateText(viewMulai, textMulai, "Mulai")
            setDateText(viewSelesai, textSelesai, "Selesai")

            loadAll()
        }

        findViewById<View>(R.id.btnLokasi).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.btnProfil).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        loadAll()
    }

    private fun loadAll() {
        db.collectionGroup("points")
            .get()
            .addOnSuccessListener { pointDocs ->
                val allPoints = mutableListOf<GpsPoint>()

                for (pointDoc in pointDocs) {
                    val trackDate = pointDoc.reference.parent.parent?.id ?: ""
                    val point = getGpsPointFromTrackPoint(pointDoc, trackDate)

                    if (point != null) {
                        allPoints.add(point)
                    }
                }

                tampilkanRiwayat(allPoints)
            }
            .addOnFailureListener { error ->
                list.clear()
                routeStatsMap.clear()
                adapter.notifyDataSetChanged()
                tvInfo.text = "Menampilkan 0 riwayat"

                Toast.makeText(
                    this,
                    "Gagal mengambil data points: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadByRange(st: Long, en: Long) {
        val enCal = Calendar.getInstance().apply {
            timeInMillis = en
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startTime = st
        val endTime = enCal.timeInMillis

        db.collectionGroup("points")
            .get()
            .addOnSuccessListener { pointDocs ->
                val allPoints = mutableListOf<GpsPoint>()

                for (pointDoc in pointDocs) {
                    val trackDate = pointDoc.reference.parent.parent?.id ?: ""
                    val point = getGpsPointFromTrackPoint(pointDoc, trackDate)

                    if (point != null && point.timestamp in startTime..endTime) {
                        allPoints.add(point)
                    }
                }

                tampilkanRiwayat(allPoints)
            }
            .addOnFailureListener { error ->
                list.clear()
                routeStatsMap.clear()
                adapter.notifyDataSetChanged()
                tvInfo.text = "Menampilkan 0 riwayat"

                Toast.makeText(
                    this,
                    "Gagal mengambil data points: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun tampilkanRiwayat(points: List<GpsPoint>) {
        list.clear()
        routeStatsMap.clear()

        val groupedData = mutableMapOf<String, MutableList<GpsPoint>>()

        for (point in points) {
            val tanggal = formatDateOnly.format(Date(point.timestamp))

            if (!groupedData.containsKey(tanggal)) {
                groupedData[tanggal] = mutableListOf()
            }

            groupedData[tanggal]?.add(point)
        }

        for ((_, groupPoints) in groupedData) {
            val sortedPoints = groupPoints.sortedBy { it.timestamp }
            val routeGroups = kelompokkanRutePerJeda(sortedPoints)

            for (routePoints in routeGroups) {
                if (routePoints.isEmpty()) continue

                val sortedRoutePoints = routePoints.sortedBy { it.timestamp }
                val start = sortedRoutePoints.first()
                val end = sortedRoutePoints.last()

                val namaAwal = getNamaTempat(start.lat, start.lng)
                val namaAkhir = getNamaTempat(end.lat, end.lng)

                val routeKey = end.timestamp
                val stats = hitungStatistikRute(sortedRoutePoints)

                routeStatsMap[routeKey] = stats

                list.add(
                    HistoryModel(
                        lokasiAwal = "Lokasi Awal: $namaAwal",
                        lokasiAkhir = "Lokasi Akhir: $namaAkhir",
                        startLat = start.lat,
                        startLng = start.lng,
                        endLat = end.lat,
                        endLng = end.lng,
                        timestamp = Timestamp(Date(end.timestamp))
                    )
                )
            }
        }

        list.sortByDescending { it.timestamp?.toDate()?.time ?: 0L }

        tvInfo.text = "Menampilkan ${list.size} riwayat"
        adapter.notifyDataSetChanged()
    }

    private fun kelompokkanRutePerJeda(points: List<GpsPoint>): List<List<GpsPoint>> {
        if (points.isEmpty()) return emptyList()

        val result = mutableListOf<List<GpsPoint>>()
        val currentRoute = mutableListOf<GpsPoint>()

        for (point in points) {
            val lastPoint = currentRoute.lastOrNull()

            if (lastPoint != null) {
                val selisihWaktu = point.timestamp - lastPoint.timestamp

                if (selisihWaktu >= routeIdleTimeoutMs) {
                    result.add(currentRoute.toList())
                    currentRoute.clear()
                }
            }

            currentRoute.add(point)
        }

        if (currentRoute.isNotEmpty()) {
            result.add(currentRoute.toList())
        }

        return result
    }

    private fun hitungStatistikRute(points: List<GpsPoint>): RouteStats {
        if (points.isEmpty()) {
            return RouteStats(
                totalDistanceKm = 0.0,
                avgSpeedKmh = 0.0,
                durationText = "0 menit",
                startTime = 0L,
                endTime = 0L
            )
        }

        val sortedPoints = points.sortedBy { it.timestamp }
        var totalDistanceKm = 0.0

        for (i in 0 until sortedPoints.size - 1) {
            val start = LatLng(sortedPoints[i].lat, sortedPoints[i].lng)
            val end = LatLng(sortedPoints[i + 1].lat, sortedPoints[i + 1].lng)

            totalDistanceKm += distance(start, end)
        }

        val startTime = sortedPoints.first().timestamp
        val endTime = sortedPoints.last().timestamp
        val durationMs = if (endTime > startTime) endTime - startTime else 0L
        val durationHours = durationMs.toDouble() / (1000.0 * 60.0 * 60.0)
        val avgSpeedKmh = if (durationHours > 0.0) totalDistanceKm / durationHours else 0.0

        return RouteStats(
            totalDistanceKm = totalDistanceKm,
            avgSpeedKmh = avgSpeedKmh,
            durationText = formatDuration(durationMs),
            startTime = startTime,
            endTime = endTime
        )
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "0 menit"

        val totalMinutes = durationMs / (1000 * 60)
        val jam = totalMinutes / 60
        val menit = totalMinutes % 60

        return if (jam > 0) {
            "%d jam %d menit".format(jam, menit)
        } else {
            "%d menit".format(menit)
        }
    }

    private fun getGpsPointFromTrackPoint(doc: DocumentSnapshot, trackDate: String): GpsPoint? {
        val lat = getDoubleField(doc, "lat", "latitude")
        val lng = getDoubleField(doc, "lng", "longitude")
        val speed = getDoubleField(doc, "speed") ?: 0.0

        var timestamp = getTimestampField(doc, "timestamp")

        if (timestamp == 0L) {
            timestamp = getTimestampField(doc, "time")
        }

        if (timestamp == 0L) {
            timestamp = getTimestampField(doc, "createdAt")
        }

        if (timestamp == 0L) {
            timestamp = parseTrackDateToMillis(trackDate)
        }

        if (timestamp > 0 && timestamp < 10000000000L) {
            timestamp *= 1000
        }

        if (lat == null || lng == null || timestamp == 0L) {
            return null
        }

        return GpsPoint(
            lat = lat,
            lng = lng,
            timestamp = timestamp,
            speed = speed
        )
    }

    private fun getDoubleField(doc: DocumentSnapshot, vararg fieldNames: String): Double? {
        for (fieldName in fieldNames) {
            val value = doc.get(fieldName)
            val result = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }

            if (result != null) return result
        }

        return null
    }

    private fun getTimestampField(doc: DocumentSnapshot, fieldName: String): Long {
        val value = doc.get(fieldName)

        return when (value) {
            is Timestamp -> value.toDate().time
            is Date -> value.time
            is Number -> value.toLong()
            is String -> parseTimestampString(value)
            else -> 0L
        }
    }

    private fun parseTimestampString(value: String): Long {
        val cleanValue = value.trim()

        cleanValue.toLongOrNull()?.let { return it }

        val patterns = listOf(
            "dd/MM/yyyy HH:mm",
            "dd-MM-yyyy HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                val date = sdf.parse(cleanValue)

                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }

        return 0L
    }

    private fun parseTrackDateToMillis(trackDate: String): Long {
        return try {
            formatTrackDoc.parse(trackDate)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun getNamaTempat(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale("id", "ID"))

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val desa = address.subLocality
                val kecamatan = address.locality
                val kabupaten = address.subAdminArea
                val provinsi = address.adminArea
                val alamatLengkap = address.getAddressLine(0)

                when {
                    !desa.isNullOrEmpty() && !kecamatan.isNullOrEmpty() && !kabupaten.isNullOrEmpty() -> "$desa, $kecamatan, $kabupaten"
                    !kecamatan.isNullOrEmpty() && !kabupaten.isNullOrEmpty() -> "$kecamatan, $kabupaten"
                    !kabupaten.isNullOrEmpty() && !provinsi.isNullOrEmpty() -> "$kabupaten, $provinsi"
                    !alamatLengkap.isNullOrEmpty() -> alamatLengkap
                    else -> "Nama lokasi tidak tersedia"
                }
            } else {
                "Nama lokasi tidak tersedia"
            }
        } catch (e: Exception) {
            "Nama lokasi tidak tersedia"
        }
    }

    private fun distance(start: LatLng, end: LatLng): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(start.latitude)) *
                cos(Math.toRadians(end.latitude)) *
                sin(dLng / 2).pow(2)

        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun pickDate(onPicked: (Long, String) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                onPicked(
                    calendar.timeInMillis,
                    formatDisplay.format(calendar.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun findTextViewInside(view: View): TextView? {
        if (view is TextView) return view

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findTextViewInside(view.getChildAt(i))

                if (result != null) return result
            }
        }

        return null
    }

    private fun setDateText(view: View, textView: TextView?, value: String) {
        if (view is TextView) {
            view.text = value
        } else {
            textView?.text = value
        }
    }

    data class GpsPoint(
        val lat: Double,
        val lng: Double,
        val timestamp: Long,
        val speed: Double = 0.0
    )

    data class RouteStats(
        val totalDistanceKm: Double,
        val avgSpeedKmh: Double,
        val durationText: String,
        val startTime: Long,
        val endTime: Long
    )
}
