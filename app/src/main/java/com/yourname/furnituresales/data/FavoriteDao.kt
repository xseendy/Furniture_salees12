package com.yourname.furnituresales.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteDao {
    @Query("SELECT productId FROM favorites WHERE uid = :uid")
    suspend fun getFavoriteIds(uid: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uid = :uid AND productId = :productId")
    suspend fun remove(uid: String, productId: String)

    @Query("DELETE FROM favorites WHERE uid = :uid")
    suspend fun clear(uid: String)
}
