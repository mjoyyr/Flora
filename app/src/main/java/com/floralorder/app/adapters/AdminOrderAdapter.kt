package com.floralorder.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.floralorder.app.R

data class AdminOrderItem(
    val name: String,
    val quantity: Int,
    val price: Double,
    val imageUrl: String
)

data class AdminOrderSummary(
    val id: Int,
    val customerName: String,
    val customerEmail: String,
    val address: String,
    val total: Double,
    val status: String,
    val date: String,
    val paymentMethod: String,
    val items: List<AdminOrderItem>
)

class AdminOrderAdapter(
    private val orders: List<AdminOrderSummary>,
    private val statusOptions: List<String>,
    private val onStatusChange: (AdminOrderSummary, String) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderId: TextView = view.findViewById(R.id.tvOrderId)
        val customer: TextView = view.findViewById(R.id.tvCustomer)
        val address: TextView = view.findViewById(R.id.tvAddress)
        val date: TextView = view.findViewById(R.id.tvDate)
        val payment: TextView = view.findViewById(R.id.tvPayment)
        val itemsContainer: LinearLayout = view.findViewById(R.id.llItems)
        val total: TextView = view.findViewById(R.id.tvTotal)
        val spStatus: Spinner = view.findViewById(R.id.spStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        holder.orderId.text = "Order #${order.id}  •  ${order.status}"
        holder.customer.text = "${order.customerName} (${order.customerEmail})"
        holder.address.text = "Deliver to: ${order.address}"
        holder.date.text = order.date
        holder.payment.text = "Payment: ${order.paymentMethod}"
        bindItems(holder, order)
        holder.total.text = "₱%.2f".format(order.total)

        // Clear any previous listener before changing the adapter/selection,
        // so setting the initial value doesn't fire a spurious update call.
        holder.spStatus.onItemSelectedListener = null

        val adapter = ArrayAdapter(
            holder.itemView.context, android.R.layout.simple_spinner_dropdown_item, statusOptions
        )
        holder.spStatus.adapter = adapter

        val currentIndex = statusOptions.indexOf(order.status).let { if (it == -1) 0 else it }
        holder.spStatus.setSelection(currentIndex, false)

        // Attach the listener only after the view is fully laid out, so the
        // programmatic setSelection above doesn't trigger onStatusChange.
        holder.spStatus.post {
            holder.spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    val newStatus = statusOptions[pos]
                    if (newStatus != order.status) {
                        onStatusChange(order, newStatus)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /** One row per ordered flower: picture, name, quantity and subtotal. */
    private fun bindItems(holder: ViewHolder, order: AdminOrderSummary) {
        val container = holder.itemsContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        for (item in order.items) {
            val row = inflater.inflate(R.layout.item_admin_order_line, container, false)
            row.findViewById<TextView>(R.id.tvItemName).text = item.name
            row.findViewById<TextView>(R.id.tvItemQty).text =
                "Qty: ${item.quantity}  •  ₱%.2f each".format(item.price)
            row.findViewById<TextView>(R.id.tvItemSubtotal).text =
                "₱%.2f".format(item.price * item.quantity)
            Glide.with(container.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.placeholder_flower)
                .into(row.findViewById<ImageView>(R.id.ivItem))
            container.addView(row)
        }
    }

    override fun getItemCount(): Int = orders.size
}
