package com.example.tugasakhir

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Komponen UI
        val etNama = findViewById<EditText>(R.id.etNama)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnDaftar = findViewById<Button>(R.id.btnDaftar)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        // ==================== TOMBOL DAFTAR ====================
        btnDaftar.setOnClickListener {

            val nama = etNama.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validasi input
            if (nama.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua data harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password tidak sama", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ==================== REGISTER KE FIREBASE AUTH ====================
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->

                    // Ambil UID user dari Authentication
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    // Simpan data tambahan (nama) ke Realtime Database
                    val database = FirebaseDatabase.getInstance().reference
                    val userData = HashMap<String, String>()
                    userData["nama"] = nama
                    userData["email"] = email

                    database.child("users").child(userId)
                        .setValue(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal menyimpan data tambahan", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        // ==================== PINDAH KE LOGIN ====================
        tvLogin.setOnClickListener {
            finish()
        }
    }
}