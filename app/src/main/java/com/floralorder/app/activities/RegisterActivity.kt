package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.R
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.network.Request
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val tilName = findViewById<TextInputLayout>(R.id.tilName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPhone = findViewById<TextInputLayout>(R.id.tilPhone)
        val tilAddress = findViewById<TextInputLayout>(R.id.tilAddress)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val spAddress = findViewById<AutoCompleteTextView>(R.id.spAddress)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)

        val areas = resources.getStringArray(R.array.pangasinan_areas_dropdown)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, areas)
        spAddress.setAdapter(adapter)

        btnRegister.setOnClickListener {
            tilName.error = null
            tilEmail.error = null
            tilPhone.error = null
            tilAddress.error = null
            tilPassword.error = null

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = spAddress.text.toString().trim()
            val password = etPassword.text.toString().trim()

            var hasError = false

            if (name.isEmpty()) {
                tilName.error = "Please enter your full name"
                hasError = true
            }

            if (email.isEmpty()) {
                tilEmail.error = "Please enter your email address"
                hasError = true
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                hasError = true
            }

            if (phone.isEmpty()) {
                tilPhone.error = "Please enter your phone number"
                hasError = true
            }

            if (address.isEmpty()) {
                tilAddress.error = "Please select your delivery area"
                hasError = true
            }

            if (password.isEmpty()) {
                tilPassword.error = "Please enter a password"
                hasError = true
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            registerUser(name, email, phone, address, password)
        }

        tvGoLogin.setOnClickListener { finish() }
    }

    private fun registerUser(name: String, email: String, phone: String, address: String, password: String) {
        val body = JSONObject()
        body.put("name", name)
        body.put("email", email)
        body.put("phone", phone)
        body.put("address", address)
        body.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.REGISTER, body,
            { response ->
                val success = response.optBoolean("success", false)
                Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                if (success) {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}