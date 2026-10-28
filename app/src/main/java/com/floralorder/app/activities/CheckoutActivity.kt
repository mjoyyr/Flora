package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class CheckoutActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var btnPlaceOrder: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        session = SessionManager(this)

        val tilCity = findViewById<TextInputLayout>(R.id.tilCity)
        val acCity = findViewById<AutoCompleteTextView>(R.id.spAddress)
        val tilBarangay = findViewById<TextInputLayout>(R.id.tilBarangay)
        val etBarangay = findViewById<TextInputEditText>(R.id.etBarangay)
        val etStreet = findViewById<TextInputEditText>(R.id.etStreet)
        val etNote = findViewById<TextInputEditText>(R.id.etNote)
        val rgPayment = findViewById<RadioGroup>(R.id.rgPayment)
        val tilGcash = findViewById<TextInputLayout>(R.id.tilGcash)
        val etGcash = findViewById<TextInputEditText>(R.id.etGcash)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // City dropdown (Pangasinan only)
        val areas = resources.getStringArray(R.array.pangasinan_areas_dropdown)
        acCity.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, areas))
        acCity.setOnItemClickListener { _, _, _, _ -> tilCity.error = null }
        etBarangay.doOnTextChanged { _, _, _, _ -> tilBarangay.error = null }
        etGcash.doOnTextChanged { _, _, _, _ -> tilGcash.error = null }

        // Show the GCash number field only when GCash is selected
        rgPayment.setOnCheckedChangeListener { _, checkedId ->
            tilGcash.visibility = if (checkedId == R.id.rbGcash) View.VISIBLE else View.GONE
        }

        loadCartTotal(tvTotal)

        btnPlaceOrder.setOnClickListener {
            var valid = true

            val city = acCity.text.toString().trim()
            if (city.isEmpty()) {
                tilCity.error = "Please select your city / municipality"
                valid = false
            }

            val barangay = etBarangay.text.toString().trim()
            if (barangay.length < 2) {
                tilBarangay.error = "Please enter your barangay"
                valid = false
            }

            var paymentMethod = "Cash on Delivery"
            when (rgPayment.checkedRadioButtonId) {
                R.id.rbGcash -> {
                    val gcash = etGcash.text.toString().trim()
                    if (!Regex("^09\\d{9}$").matches(gcash)) {
                        tilGcash.error = "Enter a valid GCash number (09XXXXXXXXX)"
                        valid = false
                    } else {
                        paymentMethod = "GCash - $gcash"
                    }
                }
            }

            if (!valid) return@setOnClickListener

            val address = buildAddress(
                street = etStreet.text.toString().trim(),
                barangay = barangay,
                city = city,
                note = etNote.text.toString().trim()
            )
            placeOrder(address, paymentMethod)
        }
    }

    private fun buildAddress(street: String, barangay: String, city: String, note: String): String {
        val brgy = if (barangay.startsWith("brgy", true) || barangay.startsWith("barangay", true)) {
            barangay
        } else {
            "Brgy. $barangay"
        }

        val parts = mutableListOf<String>()
        if (street.isNotEmpty()) parts.add(street)
        parts.add(brgy)
        parts.add(city)
        parts.add("Pangasinan")

        var address = parts.joinToString(", ")
        if (note.isNotEmpty()) address += " (Note: $note)"
        return address
    }

    private fun loadCartTotal(tvTotal: TextView) {
        val request = JsonObjectRequest(
            Request.Method.GET, "${ApiConfig.GET_CART}?user_id=${session.getUserId()}", null,
            { response ->
                val total = response.optDouble("total", 0.0)
                tvTotal.text = "₱%.2f".format(total)
            },
            { }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun placeOrder(address: String, paymentMethod: String) {
        btnPlaceOrder.isEnabled = false
        btnPlaceOrder.text = "Placing order..."

        val body = JSONObject()
        body.put("user_id", session.getUserId())
        body.put("delivery_address", address)
        body.put("payment_method", paymentMethod)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.CHECKOUT, body,
            { response ->
                val success = response.optBoolean("success", false)
                if (success) {
                    val intent = Intent(this, OrderConfirmationActivity::class.java)
                    intent.putExtra("order_id", response.optInt("order_id"))
                    intent.putExtra("total", response.optDouble("total"))
                    startActivity(intent)
                    finish()
                } else {
                    resetPlaceOrderButton()
                    Toast.makeText(this, response.optString("message", "Checkout failed"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                resetPlaceOrderButton()
                Toast.makeText(this, "Checkout failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun resetPlaceOrderButton() {
        btnPlaceOrder.isEnabled = true
        btnPlaceOrder.text = "Place Order"
    }
}