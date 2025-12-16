package com.yourname.furnituresales.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers")
    fun getCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity)
}

