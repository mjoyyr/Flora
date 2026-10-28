package com.floralorder.app.activities

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floralorder.app.network.Request
import com.floralorder.app.network.JsonObjectRequest
import com.floralorder.app.R
import com.floralorder.app.adapters.AdminMessage
import com.floralorder.app.adapters.AdminMessageAdapter
import com.floralorder.app.network.ApiConfig
import com.floralorder.app.network.VolleySingleton

class AdminMessagesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_messages)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        rvMessages.layoutManager = LinearLayoutManager(this)

        val request = JsonObjectRequest(
            Request.Method.GET, ApiConfig.GET_ALL_MESSAGES, null,
            { response ->
                val arr = response.optJSONArray("messages")
                val messages = mutableListOf<AdminMessage>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        messages.add(
                            AdminMessage(
                                name = obj.optString("name"),
                                email = obj.optString("email"),
                                message = obj.optString("message"),
                                date = obj.optString("created_at")
                            )
                        )
                    }
                }
                tvEmpty.visibility = if (messages.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                rvMessages.adapter = AdminMessageAdapter(messages)
            },
            { error -> Toast.makeText(this, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).requestQueue.add(request)
    }
}
