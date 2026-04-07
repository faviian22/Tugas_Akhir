package com.example.tugasakhir

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var etEmail: EditText
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        etNama = findViewById(R.id.etNama)
        etEmail = findViewById(R.id.etEmail)

        val btnLokasi = findViewById<ImageView>(R.id.btnLokasi)
        val btnHistori = findViewById<ImageView>(R.id.btnHistori)

        auth = FirebaseAuth.getInstance()

        val userId = auth.currentUser?.uid
        database = FirebaseDatabase.getInstance().getReference("users")

        if (userId != null) {
            ambilDataUser(userId)
        }

        btnLokasi.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnHistori.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun ambilDataUser(uid: String) {

        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val nama = snapshot.child("nama").value.toString()
                val email = snapshot.child("email").value.toString()

                etNama.setText(nama)
                etEmail.setText(email)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}