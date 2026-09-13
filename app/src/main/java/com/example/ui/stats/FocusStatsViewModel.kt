package com.example.ui.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApp
import com.example.data.entity.AppRestrictionEntity
import com.example.data.entity.EmergencyUnlockEntity
import com.example.data.repository.AppBlockerRepository
import com.example.data.repository.HabitRepository
import com.example.util.DateUtils
import com.example.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUsageStatItem(
    val packageName: String,
    val appName: String,
    val usageMinutes: Long,
    val limitMinutes: Int,
    val isRestricted: Boolean
)

data class FocusStatsUiState(
    val totalScreenTimeMinutesToday: Long = 0L,
    val restrictedScreenTimeMinutesToday: Long = 0L,
    val appUsageList: List<AppUsageStatItem> = emptyList(),
    val currentHabitStreak: Int = 0,
    val totalHabitCompletions: Int = 0,
    val emergencyUnlocks: List<EmergencyUnlockEntity> = emptyList()
)

class FocusStatsViewModel(
    private val habitRepository: HabitRepository = FocusLockApp.instance.habitRepository,
    private val blockerRepository: AppBlockerRepository = FocusLockApp.instance.appBlockerRepository
) : ViewModel() {

    private val _totalScreenTime = MutableStateFlow(0L)
    private val _restrictedScreenTime = MutableStateFlow(0L)
    private val _appUsageStats = MutableStateFlow<List<AppUsageStatItem>>(emptyList())

    val uiState: StateFlow<FocusStatsUiState> = combine(
        _totalScreenTime,
        _restrictedScreenTime,
        _appUsageStats,
        habitRepository.totalCompletedCount,
        blockerRepository.allEmergencyUnlocks
    ) { totalTime, restrictedTime, appUsage, completedCount, unlocks ->
        val streak = habitRepository.calculateCurrentStreak()
        FocusStatsUiState(
            totalScreenTimeMinutesToday = totalTime,
            restrictedScreenTimeMinutesToday = restrictedTime,
            appUsageList = appUsage,
            currentHabitStreak = streak,
            totalHabitCompletions = completedCount,
            emergencyUnlocks = unlocks
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusStatsUiState()
    )

    fun refreshStats(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val usageMap = UsageStatsHelper.getDailyScreenTimeMap(context)
                val restrictions = blockerRepository.allRestrictions
                val pm = context.packageManager

                var total = 0L
                var restrictedTotal = 0L
                val items = mutableListOf<AppUsageStatItem>()

                for ((pkg, minutes) in usageMap) {
                    total += minutes
                    val restriction = blockerRepository.getRestriction(pkg)
                    val isRestricted = restriction?.isRestricted ?: false
                    val limit = restriction?.dailyLimitMinutes ?: 0

                    if (isRestricted) {
                        restrictedTotal += minutes
                    }

                    var appName = restriction?.appName
                    if (appName == null) {
                        try {
                            val info = pm.getApplicationInfo(pkg, 0)
                            appName = pm.getApplicationLabel(info).toString()
                        } catch (e: Exception) {
                            appName = pkg
                        }
                    }

                    items.add(
                        AppUsageStatItem(
                            packageName = pkg,
                            appName = appName,
                            usageMinutes = minutes,
                            limitMinutes = limit,
                            isRestricted = isRestricted
                        )
                    )
                }

                _totalScreenTime.value = total
                _restrictedScreenTime.value = restrictedTotal
                _appUsageStats.value = items.sortedByDescending { it.usageMinutes }
            }
        }
    }
}
