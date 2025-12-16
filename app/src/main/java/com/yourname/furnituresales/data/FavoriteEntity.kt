package com.yourname.furnituresales.data

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["uid", "productId"]
)
data class FavoriteEntity(
    val uid: String,
    val productId: String
)
