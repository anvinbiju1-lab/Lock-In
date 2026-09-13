package com.example.ui.blocker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApp
import com.example.data.entity.AppRestrictionEntity
import com.example.data.entity.EmergencyUnlockEntity
import com.example.data.repository.AppBlockerRepository
import com.example.service.AppMonitorService
import com.example.util.InstalledAppInfo
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

data class PermissionCheckState(
    val hasUsageStats: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasNotifications: Boolean = false,
    val isBatteryOptimizedIgnored: Boolean = false
) {
    val allGranted: Boolean
        get() = hasUsageStats && hasOverlay && hasNotifications
}

data class AppBlockerUiState(
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val filteredApps: List<InstalledAppInfo> = emptyList(),
    val searchQuery: String = "",
    val restrictions: List<AppRestrictionEntity> = emptyList(),
    val emergencyUnlocks: List<EmergencyUnlockEntity> = emptyList(),
    val permissions: PermissionCheckState = PermissionCheckState(),
    val isServiceRunning: Boolean = true,
    val isLoadingApps: Boolean = false,
    val selectedAppForConfig: InstalledAppInfo? = null,
    val selectedRestriction: AppRestrictionEntity? = null
)

class AppBlockerViewModel(
    private val repository: AppBlockerRepository = FocusLockApp.instance.appBlockerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(false)
    private val _permissions = MutableStateFlow(PermissionCheckState())
    private val _isServiceRunning = MutableStateFlow(true)
    private val _selectedAppForConfig = MutableStateFlow<InstalledAppInfo?>(null)

    private val baseDataFlow = combine(
        _installedApps,
        _searchQuery,
        repository.allRestrictions,
        repository.allEmergencyUnlocks
    ) { apps, query, restrictions, emergencyUnlocks ->
        val restrictionMap = restrictions.associateBy { it.packageName }
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        Triple(filtered, restrictions, emergencyUnlocks) to restrictionMap
    }

    val uiState: StateFlow<AppBlockerUiState> = combine(
        baseDataFlow,
        _permissions,
        _isServiceRunning,
        _isLoadingApps,
        _selectedAppForConfig
    ) { (data, restrictionMap), perms, isRunning, isLoading, selectedApp ->
        val (filtered, restrictions, emergencyUnlocks) = data
        val selectedRest = selectedApp?.let { restrictionMap[it.packageName] }

        AppBlockerUiState(
            installedApps = _installedApps.value,
            filteredApps = filtered,
            searchQuery = _searchQuery.value,
            restrictions = restrictions,
            emergencyUnlocks = emergencyUnlocks,
            permissions = perms,
            isServiceRunning = isRunning,
            isLoadingApps = isLoading,
            selectedAppForConfig = selectedApp,
            selectedRestriction = selectedRest
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppBlockerUiState()
    )

    fun checkPermissions(context: Context) {
        val hasUsage = UsageStatsHelper.hasUsageStatsPermission(context)
        val hasOverlay = UsageStatsHelper.hasOverlayPermission(context)
        val hasNotif = UsageStatsHelper.hasNotificationPermission(context)
        val hasBattery = UsageStatsHelper.isIgnoringBatteryOptimizations(context)

        _permissions.value = PermissionCheckState(
            hasUsageStats = hasUsage,
            hasOverlay = hasOverlay,
            hasNotifications = hasNotif,
            isBatteryOptimizedIgnored = hasBattery
        )
    }

    fun loadInstalledApps(context: Context) {
        if (_isLoadingApps.value) return
        _isLoadingApps.value = true
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val list = UsageStatsHelper.getInstalledApps(context)
                val usageMap = UsageStatsHelper.getDailyScreenTimeMap(context)
                list.map { app ->
                    val usageMinutes = usageMap[app.packageName] ?: 0L
                    app.copy(usageTimeTodayMillis = usageMinutes * 60 * 1000L)
                }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppRestriction(app: InstalledAppInfo) {
        viewModelScope.launch {
            val current = repository.getRestriction(app.packageName)
            val newRestricted = !(current?.isRestricted ?: false)
            repository.toggleRestriction(
                packageName = app.packageName,
                appName = app.appName,
                isRestricted = newRestricted
            )
        }
    }

    fun openRuleConfig(app: InstalledAppInfo) {
        _selectedAppForConfig.value = app
    }

    fun closeRuleConfig() {
        _selectedAppForConfig.value = null
    }

    fun saveRuleConfig(
        packageName: String,
        appName: String,
        isRestricted: Boolean,
        dailyLimitMinutes: Int,
        scheduleEnabled: Boolean,
        scheduleStart: String,
        scheduleEnd: String
    ) {
        viewModelScope.launch {
            repository.saveRestriction(
                AppRestrictionEntity(
                    packageName = packageName,
                    appName = appName,
                    isRestricted = isRestricted,
                    dailyLimitMinutes = dailyLimitMinutes,
                    scheduleEnabled = scheduleEnabled,
                    scheduleStart = scheduleStart,
                    scheduleEnd = scheduleEnd
                )
            )
            _selectedAppForConfig.value = null
        }
    }

    fun toggleService(context: Context) {
        val newRunning = !_isServiceRunning.value
        _isServiceRunning.value = newRunning
        if (newRunning) {
            AppMonitorService.start(context)
        } else {
            AppMonitorService.stop(context)
        }
    }
}
