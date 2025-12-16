package com.yourname.furnituresales.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE uid = :uid")
    suspend fun getCartItems(uid: String): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CartItemEntity>)

    @Query("DELETE FROM cart_items WHERE uid = :uid")
    suspend fun clear(uid: String)

    @Query("DELETE FROM cart_items WHERE uid = :uid AND productId = :productId")
    suspend fun remove(uid: String, productId: String)
}
