package com.floralorder.app.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.bumptech.glide.Glide
import com.floralorder.app.R
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import org.json.JSONObject

class ProductDetailActivity : AppCompatActivity() {

    private var quantity = 1
    private var productId = -1
    private var availableStock = 0
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        session = SessionManager(this)
        productId = intent.getIntExtra("product_id", -1)

        val ivProduct = findViewById<ImageView>(R.id.ivProduct)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvPrice = findViewById<TextView>(R.id.tvPrice)
        val tvStock = findViewById<TextView>(R.id.tvStock)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val tvQuantity = findViewById<TextView>(R.id.tvQuantity)
        val btnMinus = findViewById<Button>(R.id.btnMinus)
        val btnPlus = findViewById<Button>(R.id.btnPlus)
        val btnAddToCart = findViewById<Button>(R.id.btnAddToCart)

        val request = JsonObjectRequest(
            Request.Method.GET, "${ApiConfig.GET_PRODUCTS}?id=$productId", null,
            { response ->
                val arr = response.optJSONArray("products")
                if (arr != null && arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    tvName.text = obj.getString("name")
                    val price = obj.getDouble("price")
                    tvPrice.text = "₱%.2f".format(price)
                    tvDescription.text = obj.optString("description")
                    availableStock = obj.optInt("stock")

                    updateStockUi(tvStock, btnMinus, btnPlus, tvQuantity, btnAddToCart)

                    Glide.with(this)
                        .load(obj.optString("image_url"))
                        .placeholder(R.drawable.placeholder_flower)
                        .into(ivProduct)
                }
            },
            { error -> Toast.makeText(this, "Failed to load product: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)

        btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
            }
        }
        btnPlus.setOnClickListener {
            if (quantity < availableStock) {
                quantity++
                tvQuantity.text = quantity.toString()
            } else {
                Toast.makeText(this, "Only $availableStock in stock", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddToCart.setOnClickListener {
            if (!session.isLoggedIn()) {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (availableStock <= 0) {
                Toast.makeText(this, "This product is out of stock", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addToCart()
        }
    }

    private fun updateStockUi(
        tvStock: TextView,
        btnMinus: Button,
        btnPlus: Button,
        tvQuantity: TextView,
        btnAddToCart: Button
    ) {
        if (availableStock <= 0) {
            tvStock.text = "Out of Stock"
            tvStock.setTextColor(getColor(android.R.color.holo_red_dark))
            btnAddToCart.isEnabled = false
            btnAddToCart.alpha = 0.5f
            btnAddToCart.text = "Out of Stock"
            btnMinus.isEnabled = false
            btnPlus.isEnabled = false
        } else if (availableStock <= 5) {
            tvStock.text = "Only $availableStock left in stock!"
            tvStock.setTextColor(getColor(R.color.pink_dark))
        } else {
            tvStock.text = "In Stock"
            tvStock.setTextColor(getColor(R.color.green_accent))
        }

        quantity = 1
        tvQuantity.text = quantity.toString()
    }

    private fun addToCart() {
        val body = JSONObject()
        body.put("user_id", session.getUserId())
        body.put("product_id", productId)
        body.put("quantity", quantity)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.ADD_TO_CART, body,
            { response ->
                Toast.makeText(this, response.optString("message", "Added to cart"), Toast.LENGTH_SHORT).show()
            },
            { error -> Toast.makeText(this, "Failed to add to cart: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}
