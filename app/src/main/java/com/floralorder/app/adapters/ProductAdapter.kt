package com.floralorder.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.floralorder.app.R
import com.floralorder.app.models.Product

class ProductAdapter(
    private val products: List<Product>,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivProduct)
        val name: TextView = view.findViewById(R.id.tvName)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val overlayOutOfStock: LinearLayout = view.findViewById(R.id.overlayOutOfStock)
        val lowStockBadge: TextView = view.findViewById(R.id.tvLowStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.name.text = product.name
        holder.price.text = "₱%.2f".format(product.price)
        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.placeholder_flower)
            .into(holder.image)

        holder.overlayOutOfStock.visibility = if (product.stock <= 0) View.VISIBLE else View.GONE
        holder.lowStockBadge.visibility = if (product.stock in 1..5) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(product) }
    }

    override fun getItemCount(): Int = products.size
}
