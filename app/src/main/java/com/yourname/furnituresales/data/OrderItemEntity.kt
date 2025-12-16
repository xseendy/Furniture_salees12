package com.yourname.furnituresales.data

import androidx.room.Entity

@Entity(
    tableName = "order_items",
    primaryKeys = ["uid", "orderId", "productId"]
)
data class OrderItemEntity(
    val uid: String,
    val orderId: String,
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)
