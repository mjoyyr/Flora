package com.floralorder.app.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.floralorder.app.R

data class InventoryItem(
    val id: Int,
    val name: String,
    val categoryName: String,
    val imageUrl: String,
    var stock: Int
) {
    companion object {
        const val LOW_STOCK_LIMIT = 5
    }

    val isOut: Boolean get() = stock <= 0
    val isLow: Boolean get() = stock in 1..LOW_STOCK_LIMIT
}

class AdminInventoryAdapter(
    private val items: List<InventoryItem>,
    private val onStockChange: (InventoryItem, Int) -> Unit,
    private val onStockTap: (InventoryItem) -> Unit
) : RecyclerView.Adapter<AdminInventoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivProduct)
        val name: TextView = view.findViewById(R.id.tvName)
        val category: TextView = view.findViewById(R.id.tvCategory)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val stock: TextView = view.findViewById(R.id.tvStock)
        val btnMinus: TextView = view.findViewById(R.id.btnMinus)
        val btnPlus: TextView = view.findViewById(R.id.btnPlus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.category.text = item.categoryName.ifEmpty { "Uncategorized" }
        holder.stock.text = item.stock.toString()

        val (label, textColor, bgColor) = when {
            item.isOut -> Triple("Out of stock", 0xFFC0392B.toInt(), 0x1FC0392B)
            item.isLow -> Triple("Low stock", 0xFFB9770E.toInt(), 0x2EE8A33D)
            else -> Triple("In stock", 0xFF5E8A5E.toInt(), 0x338BAA8B)
        }
        holder.status.text = label
        holder.status.setTextColor(textColor)
        holder.status.background = GradientDrawable().apply {
            cornerRadius = 40f
            setColor(bgColor)
        }

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder_flower)
            .into(holder.image)

        holder.btnPlus.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos >= 0) onStockChange(items[pos], items[pos].stock + 1)
        }
        holder.btnMinus.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos >= 0 && items[pos].stock > 0) onStockChange(items[pos], items[pos].stock - 1)
        }
        holder.stock.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos >= 0) onStockTap(items[pos])
        }
    }

    override fun getItemCount(): Int = items.size
}
