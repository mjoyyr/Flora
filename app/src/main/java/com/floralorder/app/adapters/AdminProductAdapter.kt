package com.floralorder.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.floralorder.app.R

data class AdminProduct(
    val id: Int,
    val name: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val stock: Int,
    val categoryId: Int,
    val categoryName: String,
    val isFeatured: Boolean
)

class AdminProductAdapter(
    private val products: List<AdminProduct>,
    private val onEdit: (AdminProduct) -> Unit,
    private val onDelete: (AdminProduct) -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivProduct)
        val name: TextView = view.findViewById(R.id.tvName)
        val category: TextView = view.findViewById(R.id.tvCategory)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val stock: TextView = view.findViewById(R.id.tvStock)
        val ivEdit: ImageView = view.findViewById(R.id.ivEdit)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.name.text = product.name
        holder.category.text = product.categoryName.ifEmpty { "Uncategorized" }
        holder.price.text = "₱%.2f".format(product.price)

        when {
            product.stock <= 0 -> {
                holder.stock.text = "Out of stock"
                holder.stock.setTextColor(0xFFC0392B.toInt())
            }
            product.stock <= 5 -> {
                holder.stock.text = "${product.stock} left (low stock)"
                holder.stock.setTextColor(0xFFE8A33D.toInt())
            }
            else -> {
                holder.stock.text = "${product.stock} in stock"
                holder.stock.setTextColor(0xFF8BAA8B.toInt())
            }
        }

        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.placeholder_flower)
            .into(holder.image)

        holder.ivEdit.setOnClickListener { onEdit(product) }
        holder.ivDelete.setOnClickListener { onDelete(product) }
    }

    override fun getItemCount(): Int = products.size
}
