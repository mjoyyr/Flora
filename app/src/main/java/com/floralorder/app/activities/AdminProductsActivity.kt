package com.floralorder.app.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.adapters.AdminProduct
import com.floralorder.app.adapters.AdminProductAdapter
import com.floralorder.app.models.Category
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.VolleySingleton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class AdminProductsActivity : AppCompatActivity() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var tvEmpty: TextView
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_products)

        rvProducts = findViewById(R.id.rvProducts)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvProducts.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            if (categories.isEmpty()) {
                Toast.makeText(this, "Still loading categories, try again in a moment", Toast.LENGTH_SHORT).show()
            } else {
                showProductDialog(null)
            }
        }

        loadCategories()
        loadProducts()
    }

    private fun loadCategories() {
        val request = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_CATEGORIES, null,
            { response ->
                val arr = response.optJSONArray("categories")
                val list = mutableListOf<Category>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(Category(obj.getInt("id"), obj.getString("name")))
                    }
                }
                categories = list
            },
            { }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun loadProducts() {
        val request = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_PRODUCTS, null,
            { response ->
                val arr = response.optJSONArray("products")
                val products = mutableListOf<AdminProduct>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        products.add(
                            AdminProduct(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                description = obj.optString("description"),
                                imageUrl = obj.optString("image_url"),
                                price = obj.getDouble("price"),
                                stock = obj.optInt("stock"),
                                categoryId = obj.optInt("category_id"),
                                categoryName = obj.optString("category_name"),
                                isFeatured = obj.optInt("is_featured") == 1
                            )
                        )
                    }
                }
                tvEmpty.visibility = if (products.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                rvProducts.adapter = AdminProductAdapter(
                    products,
                    onEdit = { product -> showProductDialog(product) },
                    onDelete = { product -> confirmDelete(product) }
                )
            },
            { error -> Toast.makeText(this, "Failed to load products: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    // ---------- CREATE / UPDATE (shared dialog) ----------

    private fun showProductDialog(existing: AdminProduct?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_product_form, null)
        val etName = view.findViewById<TextInputEditText>(R.id.etDialogName)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDialogDescription)
        val etImageUrl = view.findViewById<TextInputEditText>(R.id.etDialogImageUrl)
        val acCategory = view.findViewById<AutoCompleteTextView>(R.id.acDialogCategory)
        val etPrice = view.findViewById<TextInputEditText>(R.id.etDialogPrice)
        val etStock = view.findViewById<TextInputEditText>(R.id.etDialogStock)
        val switchFeatured = view.findViewById<SwitchMaterial>(R.id.switchDialogFeatured)

        val categoryNames = categories.map { it.name }
        acCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames))

        if (existing != null) {
            etName.setText(existing.name)
            etDescription.setText(existing.description)
            etImageUrl.setText(existing.imageUrl)
            etPrice.setText(existing.price.toString())
            etStock.setText(existing.stock.toString())
            switchFeatured.isChecked = existing.isFeatured
            val currentCategory = categories.firstOrNull { it.id == existing.categoryId }
            acCategory.setText(currentCategory?.name ?: "", false)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add New Product" else "Edit Product")
            .setView(view)
            .setPositiveButton(if (existing == null) "Add" else "Save", null)
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = etName.text.toString().trim()
                        val description = etDescription.text.toString().trim()
                        val imageUrl = etImageUrl.text.toString().trim()
                        val categoryName = acCategory.text.toString().trim()
                        val priceText = etPrice.text.toString().trim()
                        val stockText = etStock.text.toString().trim()
                        val featured = switchFeatured.isChecked

                        if (name.isEmpty() || priceText.isEmpty()) {
                            Toast.makeText(this@AdminProductsActivity, "Name and price are required", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val price = priceText.toDoubleOrNull()
                        if (price == null) {
                            Toast.makeText(this@AdminProductsActivity, "Enter a valid price", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val stock = stockText.toIntOrNull() ?: 0
                        val categoryId = categories.firstOrNull { it.name == categoryName }?.id

                        if (existing == null) {
                            addProduct(name, description, imageUrl, categoryId, price, stock, featured)
                        } else {
                            updateProduct(existing.id, name, description, imageUrl, categoryId, price, stock, featured)
                        }
                        dismiss()
                    }
                }
            }
            .show()
    }

    private fun addProduct(
        name: String, description: String, imageUrl: String, categoryId: Int?,
        price: Double, stock: Int, featured: Boolean
    ) {
        val body = JSONObject()
        body.put("name", name)
        body.put("description", description)
        body.put("image_url", imageUrl)
        if (categoryId != null) body.put("category_id", categoryId)
        body.put("price", price)
        body.put("stock", stock)
        body.put("is_featured", featured)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.ADD_PRODUCT, body,
            { response ->
                Toast.makeText(this, response.optString("message", "Product added"), Toast.LENGTH_SHORT).show()
                if (response.optBoolean("success", false)) loadProducts()
            },
            { error -> Toast.makeText(this, "Failed to add product: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun updateProduct(
        productId: Int, name: String, description: String, imageUrl: String, categoryId: Int?,
        price: Double, stock: Int, featured: Boolean
    ) {
        val body = JSONObject()
        body.put("product_id", productId)
        body.put("name", name)
        body.put("description", description)
        body.put("image_url", imageUrl)
        if (categoryId != null) body.put("category_id", categoryId)
        body.put("price", price)
        body.put("stock", stock)
        body.put("is_featured", featured)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.UPDATE_PRODUCT, body,
            { response ->
                Toast.makeText(this, response.optString("message", "Product updated"), Toast.LENGTH_SHORT).show()
                if (response.optBoolean("success", false)) loadProducts()
            },
            { error -> Toast.makeText(this, "Failed to update product: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    // ---------- DELETE ----------

    private fun confirmDelete(product: AdminProduct) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"${product.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteProduct(product.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteProduct(productId: Int) {
        val body = JSONObject()
        body.put("product_id", productId)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.DELETE_PRODUCT, body,
            { response ->
                Toast.makeText(this, response.optString("message", "Product deleted"), Toast.LENGTH_SHORT).show()
                if (response.optBoolean("success", false)) loadProducts()
            },
            { error -> Toast.makeText(this, "Failed to delete product: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}
