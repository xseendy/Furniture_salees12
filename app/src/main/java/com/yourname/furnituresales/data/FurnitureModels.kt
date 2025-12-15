package com.yourname.furnituresales.data

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val imageResName: String? = null,
    val dimensions: String = "",
    val material: String = "",
    val color: String = ""
)

data class CartItem(
    val product: Product,
    val quantity: Int
)

data class Address(
    val id: String,
    val label: String,
    val line: String,
    val phone: String
)

data class OrderItem(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

data class Order(
    val id: String,
    val items: List<OrderItem>,
    val total: Double,
    val address: Address,
    val status: String = "Новый"
)

data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val role: String = "customer" // supports "admin" for elevated actions
)

