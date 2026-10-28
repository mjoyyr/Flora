package com.floralorder.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.R

data class AdminMessage(
    val name: String,
    val email: String,
    val message: String,
    val date: String
)

class AdminMessageAdapter(private val messages: List<AdminMessage>) :
    RecyclerView.Adapter<AdminMessageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val email: TextView = view.findViewById(R.id.tvEmail)
        val message: TextView = view.findViewById(R.id.tvMessage)
        val date: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.name.text = msg.name
        holder.email.text = msg.email
        holder.message.text = msg.message
        holder.date.text = msg.date
    }

    override fun getItemCount(): Int = messages.size
}
