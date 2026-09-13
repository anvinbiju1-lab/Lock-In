package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {

    @Query("SELECT * FROM sleep_logs ORDER BY dateString DESC")
    fun getAllSleepLogs(): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs ORDER BY dateString DESC LIMIT :limit")
    fun getRecentSleepLogs(limit: Int = 14): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs WHERE dateString = :dateString LIMIT 1")
    suspend fun getSleepLogForDate(dateString: String): SleepLogEntity?

    @Query("SELECT * FROM sleep_logs WHERE dateString = :dateString LIMIT 1")
    fun getSleepLogForDateFlow(dateString: String): Flow<SleepLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SleepLogEntity>)

    @Update
    suspend fun updateSleepLog(log: SleepLogEntity)

    @Delete
    suspend fun deleteSleepLog(log: SleepLogEntity)

    @Query("SELECT COUNT(*) FROM sleep_logs")
    suspend fun getCount(): Int
}
