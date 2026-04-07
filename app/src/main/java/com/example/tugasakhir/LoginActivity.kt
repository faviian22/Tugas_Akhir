package com.example.tugasakhir

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // ==================== TOGGLE PASSWORD ====================
        etPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = etPassword.compoundDrawables[2]

                if (drawableEnd != null) {

                    val drawableWidth = drawableEnd.bounds.width()

                    // FIX: pakai event.x biar lebih akurat
                    if (event.x >= (etPassword.width - drawableWidth - etPassword.paddingEnd)) {

                        isPasswordVisible = !isPasswordVisible

                        if (isPasswordVisible) {
                            etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye, 0
                            )
                        } else {
                            etPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_eye_off, 0
                            )
                        }

                        // Biar cursor tetap di belakang
                        etPassword.setSelection(etPassword.text.length)

                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        // ==================== LOGIN ====================
        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Email atau password salah",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // ==================== REGISTER ====================
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}