package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.R
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.network.Request
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class ContactUsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_us)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        session = SessionManager(this)

        val tilName = findViewById<TextInputLayout>(R.id.tilName)
        val tilPhone = findViewById<TextInputLayout>(R.id.tilPhone)
        val tilMessage = findViewById<TextInputLayout>(R.id.tilMessage)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etMessage = findViewById<TextInputEditText>(R.id.etMessage)
        val btnSend = findViewById<MaterialButton>(R.id.btnSend)

        // Pre-fill with the logged-in user's details, if available
        if (session.isLoggedIn()) {
            etName.setText(session.getUserName())
            val phone = session.getUserPhone()
            val email = session.getUserEmail()
            etPhone.setText(if (phone.isNotEmpty()) phone else email)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_contact
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
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }

        btnSend.setOnClickListener {
            tilName.error = null
            tilPhone.error = null
            tilMessage.error = null

            val name = etName.text.toString().trim()
            val phoneOrEmail = etPhone.text.toString().trim()
            val message = etMessage.text.toString().trim()

            var hasError = false
            if (name.isEmpty()) {
                tilName.error = "Please enter your name"
                hasError = true
            }
            if (phoneOrEmail.isEmpty()) {
                tilPhone.error = "Please enter your phone number or email"
                hasError = true
            }
            if (message.isEmpty()) {
                tilMessage.error = "Please enter your message"
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            btnSend.text = "Sending..."

            val body = JSONObject()
            body.put("name", name)
            body.put("phone", phoneOrEmail)
            body.put("email", phoneOrEmail)
            body.put("message", message)

            val request = JsonObjectRequest(
                Request.Method.POST, ApiConfig.CONTACT, body,
                { response ->
                    btnSend.isEnabled = true
                    btnSend.text = "Send Message"
                    val success = response.optBoolean("success", false)
                    val msg = response.optString("message", if (success) "Message sent successfully!" else "Failed to send message")
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    if (success) {
                        etMessage.text?.clear()
                    }
                },
                { error ->
                    btnSend.isEnabled = true
                    btnSend.text = "Send Message"
                    Toast.makeText(this, "Failed to send: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
            VolleySingleton.getInstance(this).requestQueue.add(request)
        }
    }
}