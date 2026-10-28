package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.button.MaterialButton

class TrackOrderActivity : AppCompatActivity() {

    // The order status progresses through these stages, in order
    private val stages = listOf("Pending", "Processing", "Out for Delivery", "Delivered")
    private var orderId = -1
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_order)

        orderId = intent.getIntExtra("order_id", -1)
        findViewById<TextView>(R.id.tvOrderId).text = "Order #$orderId"

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnContactUs).setOnClickListener {
            startActivity(Intent(this, ContactUsActivity::class.java))
        }

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.pink_dark)
        swipeRefresh.setOnRefreshListener { loadOrder(orderId) }

        loadOrder(orderId)
    }

    private fun loadOrder(orderId: Int) {
        val request = JsonObjectRequest(
            Request.Method.GET, "${ApiConfig.GET_ORDER}?id=$orderId", null,
            { response ->
                swipeRefresh.isRefreshing = false
                val success = response.optBoolean("success", false)
                if (!success) {
                    Toast.makeText(this, response.optString("message", "Order not found"), Toast.LENGTH_SHORT).show()
                    return@JsonObjectRequest
                }

                val order = response.getJSONObject("order")
                val status = order.optString("status", "Pending")

                findViewById<TextView>(R.id.tvAddress).text = "🏠  ${order.optString("delivery_address")}"
                findViewById<TextView>(R.id.tvPaymentMethod).text = "💳  ${order.optString("payment_method")}"
                findViewById<TextView>(R.id.tvTotal).text = "₱%.2f".format(order.optDouble("total_amount"))

                val itemsArr = order.optJSONArray("items")
                val itemLines = StringBuilder()
                if (itemsArr != null) {
                    for (i in 0 until itemsArr.length()) {
                        val item = itemsArr.getJSONObject(i)
                        itemLines.append("${item.getInt("quantity")}x ${item.getString("name")}\n")
                    }
                }
                findViewById<TextView>(R.id.tvItems).text = itemLines.toString().trim()

                updateStatusBadge(status)
                updateStepper(status)
            },
            { error ->
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Failed to load order: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun updateStatusBadge(status: String) {
        val badge = findViewById<TextView>(R.id.tvStatusBadge)
        val message = findViewById<TextView>(R.id.tvStatusMessage)
        badge.text = status

        if (status == "Cancelled") {
            badge.background = getDrawable(R.drawable.status_badge_cancelled_bg)
            badge.setTextColor(getColor(android.R.color.holo_red_dark))
            message.text = "This order will not be delivered."
            return
        }

        badge.background = getDrawable(R.drawable.status_badge_bg)
        badge.setTextColor(getColor(R.color.green_accent))

        message.text = when (status) {
            "Pending" -> "We've received your order and will start preparing it soon."
            "Processing" -> "Your flowers are being prepared with care. 🌷"
            "Out for Delivery" -> "Your order is on its way to you! 🚚"
            "Delivered" -> "Your order has arrived. Enjoy your flowers! 💐"
            else -> ""
        }
    }

    private fun updateStepper(status: String) {
        val dots = listOf(
            findViewById<TextView>(R.id.dotPending),
            findViewById<TextView>(R.id.dotProcessing),
            findViewById<TextView>(R.id.dotOutForDelivery),
            findViewById<TextView>(R.id.dotDelivered)
        )
        val labels = listOf(
            findViewById<TextView>(R.id.labelPending),
            findViewById<TextView>(R.id.labelProcessing),
            findViewById<TextView>(R.id.labelOutForDelivery),
            findViewById<TextView>(R.id.labelDelivered)
        )
        val lines = listOf(
            findViewById<android.view.View>(R.id.lineOne),
            findViewById<android.view.View>(R.id.lineTwo),
            findViewById<android.view.View>(R.id.lineThree)
        )

        findViewById<TextView>(R.id.tvCancelledNotice).visibility =
            if (status == "Cancelled") android.view.View.VISIBLE else android.view.View.GONE

        val currentIndex = if (status == "Cancelled") 0 else stages.indexOf(status).let { if (it == -1) 0 else it }

        for (i in dots.indices) {
            val reached = i <= currentIndex
            dots[i].background = getDrawable(if (reached) R.drawable.circle_dot_filled else R.drawable.circle_dot_empty)
            dots[i].text = if (reached) "✓" else ""
            labels[i].alpha = if (reached) 1.0f else 0.5f
        }

        for (i in lines.indices) {
            val reached = i < currentIndex
            lines[i].setBackgroundColor(if (reached) getColor(R.color.pink_dark) else 0xFFE0E0E0.toInt())
        }
    }
}