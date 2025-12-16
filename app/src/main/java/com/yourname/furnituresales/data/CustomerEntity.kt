package com.yourname.furnituresales.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val uid: String,
    val email: String?,
    val displayName: String?,
    val role: String,
    val address: String?,
    val phone: String?,
    val password: String?
)

