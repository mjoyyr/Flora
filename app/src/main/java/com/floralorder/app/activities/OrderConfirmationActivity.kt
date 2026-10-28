package com.floralorder.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.floralorder.app.R

class OrderConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        val orderId = intent.getIntExtra("order_id", 0)
        val total = intent.getDoubleExtra("total", 0.0)

        findViewById<TextView>(R.id.tvOrderId).text = "Order #$orderId"
        findViewById<TextView>(R.id.tvOrderTotal).text = "Total: ₱%.2f".format(total)

        findViewById<Button>(R.id.btnBackHome).setOnClickListener { goHome() }

        // The phone's back button also returns to Home (the order is already placed).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
    }

    private fun goHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}
