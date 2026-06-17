package com.example.tugasakhir

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            /*
             * Setelah user menjawab izin notifikasi,
             * service tetap dijalankan karena login sudah berhasil.
             */
            startServiceAndGoToMain()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        /*
         * Saat halaman login tampil, user dianggap belum masuk.
         * Service notifikasi dimatikan supaya notifikasi sensor tidak muncul
         * sebelum user berhasil login.
         */
        SessionManager.markLoggedOut(this)
        AlertServiceStarter.stop(this)

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnTogglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        btnTogglePassword.setImageResource(R.drawable.ic_eye_off)

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
                etPassword.contentDescription = "Kata sandi ditampilkan"
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
                etPassword.contentDescription = "Kata sandi disembunyikan"
            }

            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Email dan kata sandi wajib diisi",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Memproses..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    SessionManager.markLoggedIn(this)

                    Toast.makeText(
                        this,
                        "Berhasil masuk",
                        Toast.LENGTH_SHORT
                    ).show()

                    requestNotificationPermissionIfNeeded()
                }
                .addOnFailureListener { e ->
                    SessionManager.markLoggedOut(this)
                    AlertServiceStarter.stop(this)

                    btnLogin.isEnabled = true
                    btnLogin.text = "Masuk"

                    Toast.makeText(
                        this,
                        "Login gagal: Email atau Password salah",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        startServiceAndGoToMain()
    }

    private fun startServiceAndGoToMain() {
        if (!SessionManager.isLoggedIn(this)) {
            Toast.makeText(
                this,
                "Sesi login tidak valid. Silakan masuk ulang.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertServiceStarter.start(this)
        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}