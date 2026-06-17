package com.example.tugasakhir

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageView

    private lateinit var btnBack: View

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var userId: String? = null
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.redirectToLoginIfNeeded(this)) return

        setContentView(R.layout.activity_edit_profile)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnBack = findViewById(R.id.btnBack)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")
        userId = auth.currentUser?.uid

        loadUserData()

        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        findViewById<Button>(R.id.btnSimpan).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(
                    this,
                    "Email tidak boleh kosong",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(
                    this,
                    "Kata sandi minimal 6 karakter",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            updateProfile(email, password)
        }

        findViewById<Button>(R.id.btnBatal).setOnClickListener {
            showBatalDialog()
        }

        btnBack.setOnClickListener {
            showBatalDialog()
        }
    }

    private fun loadUserData() {
        val uid = userId

        if (uid == null) {
            Toast.makeText(
                this,
                "Pengguna belum masuk",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        database.child(uid).get()
            .addOnSuccessListener { snapshot ->
                val email = snapshot.child("email").getValue(String::class.java) ?: auth.currentUser?.email ?: ""
                val password = snapshot.child("password").getValue(String::class.java) ?: ""

                etEmail.setText(email)
                etPassword.setText(password)
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
                etPassword.setSelection(etPassword.text.length)
            }
            .addOnFailureListener { error ->
                etEmail.setText(auth.currentUser?.email ?: "")

                Toast.makeText(
                    this,
                    "Gagal mengambil data profil: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_eye)
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
        }

        etPassword.setSelection(etPassword.text.length)
    }

    private fun updateProfile(email: String, password: String) {
        val user = auth.currentUser
        val uid = userId

        if (user == null || uid == null) {
            Toast.makeText(
                this,
                "Pengguna belum masuk",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        user.updateEmail(email)
            .addOnSuccessListener {
                user.updatePassword(password)
                    .addOnSuccessListener {
                        database.child(uid).updateChildren(
                            mapOf(
                                "email" to email,
                                "password" to password
                            )
                        ).addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Profil berhasil diperbarui",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()
                        }.addOnFailureListener { error ->
                            Toast.makeText(
                                this,
                                "Gagal menyimpan data: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            "Gagal memperbarui kata sandi: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Gagal memperbarui email: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showBatalDialog() {
        AlertDialog.Builder(this)
            .setTitle("Batalkan perubahan?")
            .setMessage("Perubahan tidak akan disimpan")
            .setPositiveButton("Ya") { _, _ ->
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
}