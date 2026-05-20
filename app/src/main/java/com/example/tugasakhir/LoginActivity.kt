package com.example.tugasakhir

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.*
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

        // tombol mata password
        etPassword.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = 2
                val drawable = etPassword.compoundDrawables[drawableEnd]

                if (drawable != null &&
                    event.rawX >= (etPassword.right - drawable.bounds.width())
                ) {

                    if (isPasswordVisible) {

                        etPassword.inputType =
                            InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD

                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye_off, 0
                        )

                    } else {

                        etPassword.inputType =
                            InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye, 0
                        )
                    }

                    etPassword.setSelection(etPassword.text.length)

                    isPasswordVisible = !isPasswordVisible

                    return@setOnTouchListener true
                }
            }

            false
        }

        // service
        val serviceIntent = Intent(this, CrashService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // login
        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    this,
                    "Isi semua field",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)

                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Login berhasil",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(this, MainActivity::class.java)
                    )

                    finish()
                }

                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Login gagal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // register
        tvRegister.setOnClickListener {

            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }
}