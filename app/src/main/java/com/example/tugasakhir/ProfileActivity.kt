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
    private lateinit var btnEdit: Button
    private lateinit var btnLogout: Button

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnEdit = findViewById(R.id.btnEdit)
        btnLogout = findViewById(R.id.btnLogout)

        val btnLokasi = findViewById<ImageView>(R.id.btnLokasi)
        val btnHistori = findViewById<ImageView>(R.id.btnHistori)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val user = auth.currentUser

        if (user != null) {
            val userId = user.uid
            ambilDataUser(userId)
            etEmail.setText(user.email)
        }

        // 🔒 hanya view (tidak bisa diedit)
        etEmail.isEnabled = false
        etPassword.isEnabled = false

        // 🔥 PINDAH KE EDIT PROFILE (INI YANG PENTING)
        btnEdit.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // 🔥 logout
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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
                if (snapshot.exists()) {
                    val password = snapshot.child("password").value?.toString()
                    etPassword.setText(password ?: "")
                } else {
                    Toast.makeText(this@ProfileActivity, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}