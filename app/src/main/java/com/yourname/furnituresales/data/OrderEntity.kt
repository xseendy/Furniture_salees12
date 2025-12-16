package com.yourname.furnituresales.data

import androidx.room.Entity

@Entity(
    tableName = "orders",
    primaryKeys = ["uid", "orderId"]
)
data class OrderEntity(
    val uid: String,
    val orderId: String,
    val createdAt: Long,
    val total: Double,
    val status: String,
    val addressLine: String,
    val addressPhone: String,
    val paymentMethod: String
)
