package com.example.tugasakhir

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageView
    private lateinit var btnSimpan: Button
    private lateinit var btnBatal: Button
    private lateinit var btnBack: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var userId: String? = null
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Binding
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnSimpan = findViewById(R.id.btnSimpan)
        btnBatal = findViewById(R.id.btnBatal)
        btnBack = findViewById(R.id.btnBack)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val user = auth.currentUser
        userId = user?.uid

        // Ambil data user dari Realtime Database
        if (userId != null) {
            database.child(userId!!).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val email = snapshot.child("email").getValue(String::class.java)
                    val password = snapshot.child("password").getValue(String::class.java)

                    etEmail.setText(email ?: "")
                    etPassword.setText(password ?: "")
                    etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                } else {
                    Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal ambil data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Toggle password
        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.transformationMethod = null
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Tombol SIMPAN
        btnSimpan.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateProfile(email, password)
        }

        // Tombol BATAL
        btnBatal.setOnClickListener { showCancelDialog() }
        btnBack.setOnClickListener { showCancelDialog() }
    }

    private fun updateProfile(email: String, password: String) {
        val user = auth.currentUser ?: return

        user.updateEmail(email).addOnSuccessListener {
            user.updatePassword(password).addOnSuccessListener {
                val data = mapOf("email" to email, "password" to password)
                database.child(userId!!).updateChildren(data)
                Toast.makeText(this, "Profile berhasil diupdate", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal update password: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal update email: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Batal?")
            .setMessage("Perubahan tidak akan disimpan")
            .setPositiveButton("Ya") { _, _ -> finish() }
            .setNegativeButton("Tidak", null)
            .show()
    }
}