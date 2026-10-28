package com.floralorder.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.R

data class OrderSummary(
    val id: Int,
    val total: Double,
    val status: String,
    val date: String,
    val itemsSummary: String
)

class OrderAdapter(
    private val orders: List<OrderSummary>,
    private val onTrackClick: (OrderSummary) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderId: TextView = view.findViewById(R.id.tvOrderId)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val date: TextView = view.findViewById(R.id.tvDate)
        val items: TextView = view.findViewById(R.id.tvItems)
        val total: TextView = view.findViewById(R.id.tvTotal)
        val btnTrack: Button = view.findViewById(R.id.btnTrack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.orderId.text = "Order #${order.id}"
        holder.status.text = order.status
        holder.date.text = order.date
        holder.items.text = order.itemsSummary
        holder.total.text = "₱%.2f".format(order.total)
        holder.btnTrack.setOnClickListener { onTrackClick(order) }
    }

    override fun getItemCount(): Int = orders.size
}
