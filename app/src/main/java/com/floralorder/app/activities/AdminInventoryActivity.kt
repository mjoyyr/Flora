package com.floralorder.app.activities

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.R
import com.floralorder.app.adapters.AdminInventoryAdapter
import com.floralorder.app.adapters.InventoryItem
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.network.Request
import com.floralorder.app.network.VolleySingleton
import org.json.JSONObject

/**
 * Admin inventory: shows the stock of every product (lowest first) and lets
 * the admin restock / adjust quantities right from the list.
 */
class AdminInventoryActivity : AppCompatActivity() {

    private lateinit var rvInventory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvSumTotal: TextView
    private lateinit var tvSumLow: TextView
    private lateinit var tvSumOut: TextView

    private val items = mutableListOf<InventoryItem>()
    private lateinit var adapter: AdminInventoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_inventory)

        rvInventory = findViewById(R.id.rvInventory)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvSumTotal = findViewById(R.id.tvSumTotal)
        tvSumLow = findViewById(R.id.tvSumLow)
        tvSumOut = findViewById(R.id.tvSumOut)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        adapter = AdminInventoryAdapter(
            items,
            onStockChange = { item, newStock -> changeStock(item, newStock) },
            onStockTap = { item -> showSetStockDialog(item) }
        )
        rvInventory.layoutManager = LinearLayoutManager(this)
        rvInventory.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadInventory()
    }

    private fun loadInventory() {
        val request = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_PRODUCTS, null,
            { response ->
                val arr = response.optJSONArray("products")
                val loaded = mutableListOf<InventoryItem>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        loaded.add(
                            InventoryItem(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                categoryName = obj.optString("category_name"),
                                imageUrl = obj.optString("image_url"),
                                stock = obj.optInt("stock")
                            )
                        )
                    }
                }
                // Items that need attention (lowest stock) go to the top.
                loaded.sortWith(compareBy<InventoryItem> { it.stock }.thenBy { it.name.lowercase() })

                items.clear()
                items.addAll(loaded)
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                updateSummary()
            },
            { error -> Toast.makeText(this, "Failed to load inventory: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun updateSummary() {
        tvSumTotal.text = items.size.toString()
        tvSumLow.text = items.count { it.isLow }.toString()
        tvSumOut.text = items.count { it.isOut }.toString()
    }

    /** Updates the row right away, then saves it; reloads from the database if saving fails. */
    private fun changeStock(item: InventoryItem, newStock: Int) {
        if (newStock < 0) return
        item.stock = newStock
        val index = items.indexOf(item)
        if (index >= 0) adapter.notifyItemChanged(index)
        updateSummary()

        val body = JSONObject()
        body.put("product_id", item.id)
        body.put("stock", newStock)

        val request = JsonObjectRequest(
            Request.Method.POST, ApiConfig.UPDATE_STOCK, body,
            { response ->
                if (!response.optBoolean("success", false)) {
                    Toast.makeText(this, response.optString("message", "Could not update stock"), Toast.LENGTH_SHORT).show()
                    loadInventory()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to update stock: ${error.message}", Toast.LENGTH_SHORT).show()
                loadInventory()
            }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }

    private fun showSetStockDialog(item: InventoryItem) {
        val dp = resources.displayMetrics.density
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(item.stock.toString())
            setSelectAllOnFocus(true)
            hint = "Stock quantity"
        }
        val container = FrameLayout(this).apply {
            val pad = (24 * dp).toInt()
            setPadding(pad, (8 * dp).toInt(), pad, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setMessage("Set the exact stock quantity")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim().toIntOrNull()
                if (value == null || value < 0) {
                    Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
                } else {
                    changeStock(item, value)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
