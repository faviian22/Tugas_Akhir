package com.example.tugasakhir

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // inisialisasi
        etEmail   = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        auth      = FirebaseAuth.getInstance()
        database  = FirebaseDatabase.getInstance().getReference("users")

        // field tidak bisa diedit langsung
        etEmail.isEnabled   = false
        etPassword.isEnabled = false

        // ambil data user login
        auth.currentUser?.let { user ->
            etEmail.setText(user.email)
            ambilDataUser(user.uid) // ambil data tambahan dari database
        }

        // tombol edit profile
        findViewById<Button>(R.id.btnEdit).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // logout
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // navigasi bawah
        findViewById<ImageView>(R.id.btnLokasi).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnHistori).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // refresh email saat balik dari edit
    override fun onResume() {
        super.onResume()
        auth.currentUser?.let { etEmail.setText(it.email) }
    }

    // ambil data dari Realtime Database
    private fun ambilDataUser(uid: String) {
        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // ambil password dari database
                val password = snapshot.child("password").value?.toString()
                etPassword.setText(password ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}