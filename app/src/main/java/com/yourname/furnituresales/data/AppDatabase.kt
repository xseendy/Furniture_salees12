package com.yourname.furnituresales.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CustomerEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}

