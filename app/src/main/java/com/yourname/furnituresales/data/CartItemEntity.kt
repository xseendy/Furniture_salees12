package com.yourname.furnituresales.data

import androidx.room.Entity

@Entity(
    tableName = "cart_items",
    primaryKeys = ["uid", "productId"]
)
data class CartItemEntity(
    val uid: String,
    val productId: String,
    val quantity: Int
)
