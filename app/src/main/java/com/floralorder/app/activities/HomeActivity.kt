package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.adapters.ProductAdapter
import com.floralorder.app.models.Category
import com.floralorder.app.models.Product
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.SessionManager
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var rvProducts: RecyclerView
    private lateinit var categoryContainer: LinearLayout
    private var selectedCategoryId: Int? = null
    private val chips = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        session = SessionManager(this)
        rvProducts = findViewById(R.id.rvProducts)
        categoryContainer = findViewById(R.id.categoryContainer)
        rvProducts.layoutManager = GridLayoutManager(this, 2)

        val firstName = session.getUserName().trim().split(" ").firstOrNull().orEmpty()
        findViewById<TextView>(R.id.tvGreeting).text =
            if (firstName.isEmpty()) "Hello 👋" else "Hello, $firstName 👋"

        findViewById<ImageView>(R.id.ivCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_orders -> {
                    startActivity(Intent(this, OrderHistoryActivity::class.java))
                    false
                }
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
        bottomNav.setOnItemReselectedListener {
            rvProducts.smoothScrollToPosition(0)
        }

        loadCategories()
        loadProducts(null)
    }

    override fun onResume() {
        super.onResume()
        loadProducts(selectedCategoryId)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadCategories() {
        val request = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_CATEGORIES, null,
            { response ->
                val arr = response.optJSONArray("categories") ?: return@JsonObjectRequest
                val categories = mutableListOf<Category>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    categories.add(Category(obj.getInt("id"), obj.getString("name")))
                }
                buildCategoryChips(categories)
            },
            { /* categories are optional; ignore failure */ }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun buildCategoryChips(categories: List<Category>) {
        categoryContainer.removeAllViews()
        chips.clear()

        addChip("All", null)
        for (category in categories) {
            addChip(category.name, category.id)
        }
        updateChipStyles()
    }

    private fun addChip(label: String, categoryId: Int?) {
        val chip = TextView(this)
        chip.text = label
        chip.textSize = 14f
        chip.setPadding(dp(18), dp(8), dp(18), dp(8))
        chip.tag = categoryId

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = dp(8)
        chip.layoutParams = params

        chip.setOnClickListener {
            selectedCategoryId = categoryId
            updateChipStyles()
            loadProducts(categoryId)
        }

        chips.add(chip)
        categoryContainer.addView(chip)
    }

    private fun updateChipStyles() {
        for (chip in chips) {
            val selected = chip.tag == selectedCategoryId
            chip.background = getDrawable(if (selected) R.drawable.chip_selected_bg else R.drawable.chip_bg)
            chip.setTextColor(getColor(if (selected) R.color.white else R.color.text_dark))
        }
    }

    private fun loadProducts(categoryId: Int?) {
        var url = ApiConfig.GET_PRODUCTS
        if (categoryId != null) {
            url += "?category_id=$categoryId"
        }

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val arr = response.optJSONArray("products") ?: return@JsonObjectRequest
                val products = mutableListOf<Product>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    products.add(
                        Product(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description"),
                            price = obj.getDouble("price"),
                            imageUrl = obj.optString("image_url"),
                            categoryId = obj.optInt("category_id"),
                            isFeatured = obj.optInt("is_featured") == 1,
                            stock = obj.optInt("stock")
                        )
                    )
                }
                rvProducts.adapter = ProductAdapter(products) { product ->
                    val intent = Intent(this, ProductDetailActivity::class.java)
                    intent.putExtra("product_id", product.id)
                    startActivity(intent)
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load products: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}
