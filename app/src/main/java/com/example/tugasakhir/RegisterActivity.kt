package com.example.tugasakhir

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnTogglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
        val btnToggleConfirm = findViewById<ImageView>(R.id.btnToggleConfirm)
        val btnDaftar = findViewById<Button>(R.id.btnDaftar)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val btnBack = findViewById<android.view.View>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
        btnToggleConfirm.setImageResource(R.drawable.ic_eye_off)

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            setPasswordVisibility(etPassword, btnTogglePassword, isPasswordVisible)
        }

        btnToggleConfirm.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            setPasswordVisibility(etConfirmPassword, btnToggleConfirm, isConfirmPasswordVisible)
        }

        // Warna biru hanya pada kata "Masuk"
        val spannable = SpannableString("Masuk")
        val start = spannable.indexOf("Masuk")
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#2563EB")),
            start,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvLogin.text = spannable
        tvLogin.movementMethod = LinkMovementMethod.getInstance()

        // Daftar akun
        btnDaftar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua data harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Kata sandi tidak sama", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val userMap = hashMapOf<String, Any>("email" to email, "password" to password)

                        database.child("users").child(userId).setValue(userMap)
                            .addOnSuccessListener {
                                SessionManager.markLoggedOut(this)
                                Toast.makeText(this, "Akun berhasil dibuat, silakan masuk", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal menyimpan data pengguna", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Kembali ke halaman masuk
        tvLogin.setOnClickListener { finish() }
    }

    private fun setPasswordVisibility(editText: EditText, toggleButton: ImageView, visible: Boolean) {
        if (visible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.ic_eye)
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.ic_eye_off)
        }

        editText.setSelection(editText.text.length)
    }
}
