package com.example.tugasakhir

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var userId: String? = null
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etEmail          = findViewById(R.id.etEmail)
        etPassword       = findViewById(R.id.etPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        auth             = FirebaseAuth.getInstance()
        database         = FirebaseDatabase.getInstance().getReference("users")
        userId           = auth.currentUser?.uid

        userId?.let { uid ->
            database.child(uid).get().addOnSuccessListener { snapshot ->
                etEmail.setText(snapshot.child("email").getValue(String::class.java) ?: "")
                etPassword.setText(snapshot.child("password").getValue(String::class.java) ?: "")
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
        }

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.transformationMethod = if (passwordVisible) null
            else PasswordTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(
                if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
            etPassword.setSelection(etPassword.text.length)
        }

        findViewById<Button>(R.id.btnSimpan).setOnClickListener {
            val email    = etEmail.text.toString().trim()
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

        findViewById<Button>(R.id.btnBatal).setOnClickListener { showCancelDialog() }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { showCancelDialog() }
    }

    private fun updateProfile(email: String, password: String) {
        val user = auth.currentUser ?: return
        user.updateEmail(email).addOnSuccessListener {
            user.updatePassword(password).addOnSuccessListener {
                database.child(userId!!).updateChildren(mapOf("email" to email, "password" to password))
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