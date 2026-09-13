package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AppRestrictionEntity
import com.example.data.entity.EmergencyUnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppBlockerDao {
    @Query("SELECT * FROM app_restrictions ORDER BY appName ASC")
    fun getAllRestrictions(): Flow<List<AppRestrictionEntity>>

    @Query("SELECT * FROM app_restrictions WHERE isRestricted = 1")
    fun getActiveRestrictions(): Flow<List<AppRestrictionEntity>>

    @Query("SELECT * FROM app_restrictions WHERE isRestricted = 1")
    suspend fun getActiveRestrictionsDirect(): List<AppRestrictionEntity>

    @Query("SELECT * FROM app_restrictions WHERE packageName = :packageName LIMIT 1")
    suspend fun getRestrictionForPackage(packageName: String): AppRestrictionEntity?

    @Query("SELECT * FROM app_restrictions WHERE packageName = :packageName LIMIT 1")
    fun getRestrictionFlow(packageName: String): Flow<AppRestrictionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRestriction(restriction: AppRestrictionEntity)

    @Update
    suspend fun updateRestriction(restriction: AppRestrictionEntity)

    @Delete
    suspend fun deleteRestriction(restriction: AppRestrictionEntity)

    @Query("DELETE FROM app_restrictions WHERE packageName = :packageName")
    suspend fun deleteRestrictionByPackage(packageName: String)

    // Emergency Unlocks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyUnlock(unlock: EmergencyUnlockEntity): Long

    @Query("SELECT * FROM emergency_unlocks WHERE packageName = :packageName AND expiresAtTimestamp > :currentTimestamp ORDER BY expiresAtTimestamp DESC LIMIT 1")
    suspend fun getActiveEmergencyUnlock(packageName: String, currentTimestamp: Long): EmergencyUnlockEntity?

    @Query("SELECT * FROM emergency_unlocks WHERE expiresAtTimestamp > :currentTimestamp")
    fun getAllActiveEmergencyUnlocks(currentTimestamp: Long): Flow<List<EmergencyUnlockEntity>>

    @Query("SELECT * FROM emergency_unlocks ORDER BY unlockedAtTimestamp DESC")
    fun getAllEmergencyUnlocks(): Flow<List<EmergencyUnlockEntity>>

    @Query("DELETE FROM emergency_unlocks WHERE expiresAtTimestamp < :cutoffTimestamp")
    suspend fun cleanOldEmergencyUnlocks(cutoffTimestamp: Long)
}
