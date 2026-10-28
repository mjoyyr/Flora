package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.R
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.network.Request
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = SessionManager(this)

        // If already logged in, skip straight to the right screen
        if (session.isLoggedIn()) {
            goToNextScreen()
            finish()
            return
        }

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        btnLogin.setOnClickListener {
            tilEmail.error = null
            tilPassword.error = null

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var hasError = false

            if (email.isEmpty()) {
                tilEmail.error = "Please enter your email address"
                hasError = true
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                hasError = true
            }

            if (password.isEmpty()) {
                tilPassword.error = "Please enter your password"
                hasError = true
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        val body = JSONObject()
        body.put("email", email)
        body.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.LOGIN, body,
            { response ->
                val success = response.optBoolean("success", false)
                if (success) {
                    val user = response.getJSONObject("user")
                    val role = user.optString("role", "customer")
                    session.saveUser(
                        user.getInt("id"),
                        user.getString("name"),
                        user.getString("email"),
                        role,
                        user.optString("phone", ""),
                        user.optString("address", "")
                    )
                    goToNextScreen()
                    finish()
                } else {
                    Toast.makeText(this, response.optString("message", "Login failed"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun goToNextScreen() {
        val destination = if (session.isAdmin()) AdminDashboardActivity::class.java else HomeActivity::class.java
        startActivity(Intent(this, destination))
    }
}