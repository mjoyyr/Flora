package com.floralorder.app.activities

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
import com.floralorder.app.adapters.AdminOrderAdapter
import com.floralorder.app.adapters.AdminOrderItem
import com.floralorder.app.adapters.AdminOrderSummary
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.VolleySingleton
import org.json.JSONObject

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private val statusOptions = listOf("Pending", "Processing", "Out for Delivery", "Delivered", "Cancelled")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders)

        rvOrders = findViewById(R.id.rvOrders)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvOrders.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        val request = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_ALL_ORDERS, null,
            { response ->
                val arr = response.optJSONArray("orders")
                val orders = mutableListOf<AdminOrderSummary>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val itemsArr = obj.optJSONArray("items")
                        val orderItems = mutableListOf<AdminOrderItem>()
                        if (itemsArr != null) {
                            for (j in 0 until itemsArr.length()) {
                                val itemObj = itemsArr.getJSONObject(j)
                                orderItems.add(
                                    AdminOrderItem(
                                        name = itemObj.getString("name"),
                                        quantity = itemObj.getInt("quantity"),
                                        price = itemObj.optDouble("price", 0.0),
                                        imageUrl = itemObj.optString("image_url")
                                    )
                                )
                            }
                        }
                        orders.add(
                            AdminOrderSummary(
                                id = obj.getInt("id"),
                                customerName = obj.optString("customer_name"),
                                customerEmail = obj.optString("customer_email"),
                                address = obj.optString("delivery_address"),
                                total = obj.getDouble("total_amount"),
                                status = obj.getString("status"),
                                date = obj.optString("created_at"),
                                paymentMethod = obj.optString("payment_method"),
                                items = orderItems
                            )
                        )
                    }
                }

                tvEmpty.visibility = if (orders.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                rvOrders.adapter = AdminOrderAdapter(orders, statusOptions) { order, newStatus ->
                    updateStatus(order.id, newStatus)
                }
            },
            { error -> Toast.makeText(this, "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun updateStatus(orderId: Int, newStatus: String) {
        val body = JSONObject()
        body.put("order_id", orderId)
        body.put("status", newStatus)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.UPDATE_ORDER_STATUS, body,
            { response ->
                Toast.makeText(this, response.optString("message", "Status updated"), Toast.LENGTH_SHORT).show()
                loadOrders()
            },
            { error -> Toast.makeText(this, "Failed to update status: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}