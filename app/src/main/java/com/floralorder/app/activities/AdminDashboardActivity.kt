package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.card.MaterialCardView

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        session = SessionManager(this)

        findViewById<MaterialCardView>(R.id.cardOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardInventory).setOnClickListener {
            startActivity(Intent(this, AdminInventoryActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardProducts).setOnClickListener {
            startActivity(Intent(this, AdminProductsActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardMessages).setOnClickListener {
            startActivity(Intent(this, AdminMessagesActivity::class.java))
        }
        findViewById<ImageView>(R.id.ivLogout).setOnClickListener {
            confirmLogout()
        }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
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

    private fun loadStats() {
        // Orders + pending count
        val ordersRequest = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_ALL_ORDERS, null,
            { response ->
                val arr = response.optJSONArray("orders")
                val total = arr?.length() ?: 0
                var pending = 0
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        if (arr.getJSONObject(i).optString("status") == "Pending") pending++
                    }
                }
                findViewById<TextView>(R.id.tvStatOrders).text = total.toString()
                findViewById<TextView>(R.id.tvStatPending).text = pending.toString()
            },
            { }
        )
        VolleySingleton.getInstance(this).requestQueue.add(ordersRequest)

        // Low-stock + out-of-stock products (stock of 5 or less)
        val productsRequest = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_PRODUCTS, null,
            { response ->
                val arr = response.optJSONArray("products")
                var needsRestock = 0
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        if (arr.getJSONObject(i).optInt("stock") <= 5) needsRestock++
                    }
                }
                findViewById<TextView>(R.id.tvStatLowStock).text = needsRestock.toString()
            },
            { }
        )
        VolleySingleton.getInstance(this).requestQueue.add(productsRequest)
    }
}