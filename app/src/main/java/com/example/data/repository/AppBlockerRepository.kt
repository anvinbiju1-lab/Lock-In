package com.example.data.repository

import com.example.data.dao.AppBlockerDao
import com.example.data.entity.AppRestrictionEntity
import com.example.data.entity.EmergencyUnlockEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow

data class BlockCheckResult(
    val shouldBlock: Boolean,
    val reason: String = "",
    val timeSpentMinutes: Long = 0L,
    val limitMinutes: Int = 0,
    val isEmergencyActive: Boolean = false,
    val emergencyRemainingSeconds: Long = 0L
)

class AppBlockerRepository(private val appBlockerDao: AppBlockerDao) {

    val allRestrictions: Flow<List<AppRestrictionEntity>> = appBlockerDao.getAllRestrictions()
    val activeRestrictions: Flow<List<AppRestrictionEntity>> = appBlockerDao.getActiveRestrictions()
    val allEmergencyUnlocks: Flow<List<EmergencyUnlockEntity>> = appBlockerDao.getAllEmergencyUnlocks()

    suspend fun getRestriction(packageName: String): AppRestrictionEntity? {
        return appBlockerDao.getRestrictionForPackage(packageName)
    }

    suspend fun saveRestriction(restriction: AppRestrictionEntity) {
        appBlockerDao.insertOrUpdateRestriction(restriction)
    }

    suspend fun toggleRestriction(packageName: String, appName: String, isRestricted: Boolean) {
        val existing = appBlockerDao.getRestrictionForPackage(packageName)
        if (existing != null) {
            appBlockerDao.insertOrUpdateRestriction(existing.copy(isRestricted = isRestricted, updatedAt = System.currentTimeMillis()))
        } else {
            appBlockerDao.insertOrUpdateRestriction(
                AppRestrictionEntity(
                    packageName = packageName,
                    appName = appName,
                    isRestricted = isRestricted,
                    dailyLimitMinutes = 20,
                    scheduleEnabled = false
                )
            )
        }
    }

    suspend fun deleteRestriction(packageName: String) {
        appBlockerDao.deleteRestrictionByPackage(packageName)
    }

    suspend fun recordEmergencyUnlock(packageName: String, appName: String): Long {
        val now = System.currentTimeMillis()
        val expires = now + (5 * 60 * 1000L) // 5 minutes
        val unlock = EmergencyUnlockEntity(
            packageName = packageName,
            appName = appName,
            unlockedAtTimestamp = now,
            expiresAtTimestamp = expires,
            reason = "Emergency 5-min bypass"
        )
        return appBlockerDao.insertEmergencyUnlock(unlock)
    }

    suspend fun getActiveEmergencyUnlock(packageName: String): EmergencyUnlockEntity? {
        val now = System.currentTimeMillis()
        return appBlockerDao.getActiveEmergencyUnlock(packageName, now)
    }

    suspend fun evaluateAppBlock(packageName: String, currentUsageMinutes: Long): BlockCheckResult {
        val restriction = appBlockerDao.getRestrictionForPackage(packageName)
            ?: return BlockCheckResult(shouldBlock = false)

        if (!restriction.isRestricted) {
            return BlockCheckResult(shouldBlock = false)
        }

        // Check if there is an active emergency unlock
        val now = System.currentTimeMillis()
        val emergencyUnlock = appBlockerDao.getActiveEmergencyUnlock(packageName, now)
        if (emergencyUnlock != null && emergencyUnlock.expiresAtTimestamp > now) {
            val remainingSec = (emergencyUnlock.expiresAtTimestamp - now) / 1000
            return BlockCheckResult(
                shouldBlock = false,
                isEmergencyActive = true,
                emergencyRemainingSeconds = remainingSec,
                timeSpentMinutes = currentUsageMinutes,
                limitMinutes = restriction.dailyLimitMinutes
            )
        }

        // Check scheduled lockout window
        if (restriction.scheduleEnabled) {
            val currentHHmm = DateUtils.getCurrentTimeHHmm()
            val inWindow = DateUtils.isWithinTimeWindow(
                currentTimeHHmm = currentHHmm,
                startHHmm = restriction.scheduleStart,
                endHHmm = restriction.scheduleEnd
            )
            if (inWindow) {
                return BlockCheckResult(
                    shouldBlock = true,
                    reason = "Scheduled Lockout Window (${restriction.scheduleStart} - ${restriction.scheduleEnd})",
                    timeSpentMinutes = currentUsageMinutes,
                    limitMinutes = restriction.dailyLimitMinutes
                )
            }
        }

        // Check daily usage limit
        if (restriction.dailyLimitMinutes > 0 && currentUsageMinutes >= restriction.dailyLimitMinutes) {
            return BlockCheckResult(
                shouldBlock = true,
                reason = "Daily Limit Reached (${restriction.dailyLimitMinutes} min limit exceeded: $currentUsageMinutes min spent)",
                timeSpentMinutes = currentUsageMinutes,
                limitMinutes = restriction.dailyLimitMinutes
            )
        }

        return BlockCheckResult(
            shouldBlock = false,
            timeSpentMinutes = currentUsageMinutes,
            limitMinutes = restriction.dailyLimitMinutes
        )
    }
}
