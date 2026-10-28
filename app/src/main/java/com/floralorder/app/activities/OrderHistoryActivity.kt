package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.adapters.OrderAdapter
import com.floralorder.app.adapters.OrderSummary
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.bottomnavigation.BottomNavigationView

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        session = SessionManager(this)
        rvOrders = findViewById(R.id.rvOrders)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvOrders.layoutManager = LinearLayoutManager(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_orders
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> true
                R.id.nav_contact -> {
                    startActivity(Intent(this, ContactUsActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        val request = JsonObjectRequest(
            Request.Method.GET, "${ApiConfig.GET_ORDERS}?user_id=${session.getUserId()}", null,
            { response ->
                val arr = response.optJSONArray("orders")
                val orders = mutableListOf<OrderSummary>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val itemsArr = obj.optJSONArray("items")
                        val itemNames = mutableListOf<String>()
                        if (itemsArr != null) {
                            for (j in 0 until itemsArr.length()) {
                                val itemObj = itemsArr.getJSONObject(j)
                                itemNames.add("${itemObj.getInt("quantity")}x ${itemObj.getString("name")}")
                            }
                        }
                        orders.add(
                            OrderSummary(
                                id = obj.getInt("id"),
                                total = obj.getDouble("total_amount"),
                                status = obj.getString("status"),
                                date = obj.optString("created_at"),
                                itemsSummary = itemNames.joinToString(", ")
                            )
                        )
                    }
                }

                tvEmpty.visibility = if (orders.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                rvOrders.adapter = OrderAdapter(orders) { order ->
                    val intent = Intent(this, TrackOrderActivity::class.java)
                    intent.putExtra("order_id", order.id)
                    startActivity(intent)
                }
            },
            { error -> Toast.makeText(this, "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}