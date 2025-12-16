package com.yourname.furnituresales.data

import androidx.room.Entity

@Entity(
    tableName = "products",
    primaryKeys = ["productId"]
)
data class ProductEntity(
    val productId: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String?,
    val imageResName: String?,
    val dimensions: String,
    val material: String,
    val color: String
)
