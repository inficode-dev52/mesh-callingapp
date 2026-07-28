package com.example

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CallHistory::class], version = 1, exportSchema = false)
abstract class MeshDatabase : RoomDatabase() {
    abstract fun callHistoryDao(): CallHistoryDao
}
