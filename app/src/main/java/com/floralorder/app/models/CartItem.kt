package com.floralorder.app.models

data class CartItem(
    val cartId: Int,
    val productId: Int,
    val name: String,
    val price: Double,
    val imageUrl: String,
    var quantity: Int
)
