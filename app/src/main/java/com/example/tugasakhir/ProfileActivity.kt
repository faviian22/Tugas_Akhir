package com.example.tugasakhir

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvEmailDisplay: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.redirectToLoginIfNeeded(this)) return

        setContentView(R.layout.activity_profile)

        tvEmailDisplay = findViewById(R.id.tvEmailDisplay)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        etEmail.isEnabled = false
        etPassword.isEnabled = false

        loadUserData()

        findViewById<Button>(R.id.btnEdit).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {

            stopService(Intent(this, AlertMonitorService::class.java))

            auth.signOut()
            SessionManager.markLoggedOut(this)
            AlertServiceStarter.stop(this)

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<android.view.View>(R.id.btnLokasi).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<android.view.View>(R.id.btnHistori).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

        findViewById<android.view.View>(R.id.btnProfil).setOnClickListener {
            // Sudah di halaman profil
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        currentUser.reload()

        val email = currentUser.email ?: ""

        tvEmailDisplay.text = email
        etEmail.setText(email)

        database.child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        val password =
                            snapshot.child("password")
                                .getValue(String::class.java)

                        etPassword.setText(password ?: "******")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ProfileActivity,
                        error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}