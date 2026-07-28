package com.example

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "call_history")
data class CallHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isIncoming: Boolean
)

@Dao
interface CallHistoryDao {
    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CallHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(callHistory: CallHistory)
}
