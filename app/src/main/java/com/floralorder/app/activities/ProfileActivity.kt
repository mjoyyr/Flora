package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.R
import com.floralorder.app.network.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        session = SessionManager(this)

        val name = session.getUserName()
        val email = session.getUserEmail()
        val phone = session.getUserPhone()
        val address = session.getUserAddress()

        findViewById<TextView>(R.id.tvName).text = name.ifEmpty { "Your Name" }
        findViewById<TextView>(R.id.tvEmail).text = email.ifEmpty { "you@email.com" }
        findViewById<TextView>(R.id.tvPhone).text = phone.ifEmpty { "Not provided" }
        findViewById<TextView>(R.id.tvAddress).text = address.ifEmpty { "Not provided" }
        findViewById<TextView>(R.id.tvAvatarInitial).text =
            name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        findViewById<TextView>(R.id.rowMyOrders).setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }
        findViewById<TextView>(R.id.rowContactUs).setOnClickListener {
            startActivity(Intent(this, ContactUsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            confirmLogout()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, OrderHistoryActivity::class.java))
                    false
                }
                R.id.nav_contact -> {
                    startActivity(Intent(this, ContactUsActivity::class.java))
                    false
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ ->
                session.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}