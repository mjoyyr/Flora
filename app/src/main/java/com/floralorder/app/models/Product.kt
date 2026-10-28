package com.floralorder.app.models

data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val categoryId: Int,
    val isFeatured: Boolean,
    val stock: Int
)
