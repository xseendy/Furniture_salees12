package com.yourname.furnituresales.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE uid = :uid ORDER BY createdAt DESC")
    suspend fun getOrders(uid: String): List<OrderEntity>

    @Query("SELECT * FROM order_items WHERE uid = :uid AND orderId = :orderId")
    suspend fun getOrderItems(uid: String, orderId: String): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun upsertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        upsertOrder(order)
        if (items.isNotEmpty()) {
            upsertItems(items)
        }
    }
}
