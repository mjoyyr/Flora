package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.adapters.CartAdapter
import com.floralorder.app.models.CartItem
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import org.json.JSONObject

class CartActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var rvCart: RecyclerView
    private lateinit var tvTotal: TextView
    private val cartItems = mutableListOf<CartItem>()
    private var currentTotal = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        session = SessionManager(this)
        rvCart = findViewById(R.id.rvCart)
        tvTotal = findViewById(R.id.tvTotal)
        rvCart.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnCheckout).setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, CheckoutActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCart()
    }

    private fun loadCart() {
        val request = JsonObjectRequest(
            Request.Method.GET, "${ApiConfig.GET_CART}?user_id=${session.getUserId()}", null,
            { response ->
                cartItems.clear()
                val arr = response.optJSONArray("items")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        cartItems.add(
                            CartItem(
                                cartId = obj.getInt("cart_id"),
                                productId = obj.getInt("product_id"),
                                name = obj.getString("name"),
                                price = obj.getDouble("price"),
                                imageUrl = obj.optString("image_url"),
                                quantity = obj.getInt("quantity")
                            )
                        )
                    }
                }
                currentTotal = response.optDouble("total", 0.0)
                tvTotal.text = "Total: ₱%.2f".format(currentTotal)

                rvCart.adapter = CartAdapter(
                    cartItems,
                    onQuantityChange = { item, newQty -> updateQuantity(item, newQty) },
                    onRemove = { item -> removeItem(item) }
                )
            },
            { error -> Toast.makeText(this, "Failed to load cart: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun updateQuantity(item: CartItem, newQty: Int) {
        val body = JSONObject()
        body.put("cart_id", item.cartId)
        body.put("quantity", newQty)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.UPDATE_CART, body,
            { loadCart() },
            { error -> Toast.makeText(this, "Update failed: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun removeItem(item: CartItem) {
        val body = JSONObject()
        body.put("cart_id", item.cartId)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.REMOVE_FROM_CART, body,
            { loadCart() },
            { error -> Toast.makeText(this, "Remove failed: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}
