package com.yourname.furnituresales.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomerEntity::class,
        CartItemEntity::class,
        FavoriteEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ProductEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao

    abstract fun cartDao(): CartDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun orderDao(): OrderDao

    abstract fun productDao(): ProductDao
}

