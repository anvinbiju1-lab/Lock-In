package com.example.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApp
import com.example.data.entity.SleepLogEntity
import com.example.data.repository.SleepRepository
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SleepDayItem(
    val dateString: String,
    val dayLabel: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isTomorrow: Boolean,
    val log: SleepLogEntity?
)

class SleepViewModel(
    private val sleepRepository: SleepRepository = FocusLockApp.instance.sleepRepository
) : ViewModel() {

    val recentSleepLogs: StateFlow<List<SleepLogEntity>> = sleepRepository.recentSleepLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(DateUtils.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _targetBedHour = MutableStateFlow(23)
    val targetBedHour: StateFlow<Int> = _targetBedHour.asStateFlow()

    private val _targetBedMin = MutableStateFlow(0)
    val targetBedMin: StateFlow<Int> = _targetBedMin.asStateFlow()

    private val _targetWakeHour = MutableStateFlow(7)
    val targetWakeHour: StateFlow<Int> = _targetWakeHour.asStateFlow()

    private val _targetWakeMin = MutableStateFlow(0)
    val targetWakeMin: StateFlow<Int> = _targetWakeMin.asStateFlow()

    private val _actualBedHour = MutableStateFlow(23)
    val actualBedHour: StateFlow<Int> = _actualBedHour.asStateFlow()

    private val _actualBedMin = MutableStateFlow(0)
    val actualBedMin: StateFlow<Int> = _actualBedMin.asStateFlow()

    private val _actualWakeHour = MutableStateFlow(7)
    val actualWakeHour: StateFlow<Int> = _actualWakeHour.asStateFlow()

    private val _actualWakeMin = MutableStateFlow(0)
    val actualWakeMin: StateFlow<Int> = _actualWakeMin.asStateFlow()

    private val _qualityRating = MutableStateFlow(4)
    val qualityRating: StateFlow<Int> = _qualityRating.asStateFlow()

    private val _bedtimeFocusEnabled = MutableStateFlow(true)
    val bedtimeFocusEnabled: StateFlow<Boolean> = _bedtimeFocusEnabled.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _isSavedSuccess = MutableStateFlow(false)
    val isSavedSuccess: StateFlow<Boolean> = _isSavedSuccess.asStateFlow()

    // Live Clock State
    private val _liveTimeString = MutableStateFlow("")
    val liveTimeString: StateFlow<String> = _liveTimeString.asStateFlow()

    private val _liveAmPm = MutableStateFlow("")
    val liveAmPm: StateFlow<String> = _liveAmPm.asStateFlow()
    
    private val _logicalDateString = MutableStateFlow(DateUtils.getLogicalSleepDateString())
    val logicalDateString: StateFlow<String> = _logicalDateString.asStateFlow()

    init {
        loadDataForDate(_selectedDate.value)
        startLiveClock()
    }

    private fun startLiveClock() {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
            val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
            
            while (isActive) {
                val cal = Calendar.getInstance()
                _liveTimeString.value = timeFormat.format(cal.time)
                _liveAmPm.value = amPmFormat.format(cal.time)
                _logicalDateString.value = DateUtils.getLogicalSleepDateString(cal)
                delay(1000L) // Update every second to keep colon blink synced if needed
            }
        }
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
        loadDataForDate(dateString)
    }

    private fun loadDataForDate(dateString: String) {
        viewModelScope.launch {
            val log = sleepRepository.getSleepLogForDate(dateString)
            if (log != null) {
                _targetBedHour.value = log.targetBedtimeHour
                _targetBedMin.value = log.targetBedtimeMinute
                _targetWakeHour.value = log.targetWakeHour
                _targetWakeMin.value = log.targetWakeMinute
                _actualBedHour.value = log.actualBedtimeHour
                _actualBedMin.value = log.actualBedtimeMinute
                _actualWakeHour.value = log.actualWakeHour
                _actualWakeMin.value = log.actualWakeMinute
                _qualityRating.value = log.qualityRating
                _bedtimeFocusEnabled.value = log.bedtimeFocusEnabled
                _notes.value = log.notes
            } else {
                // Defaults for a new day
                _targetBedHour.value = 23
                _targetBedMin.value = 0
                _targetWakeHour.value = 7
                _targetWakeMin.value = 0
                _actualBedHour.value = 23
                _actualBedMin.value = 0
                _actualWakeHour.value = 7
                _actualWakeMin.value = 0
                _qualityRating.value = 4
                _bedtimeFocusEnabled.value = true
                _notes.value = ""
            }
        }
    }

    fun setBedtime(hour: Int, min: Int) {
        _actualBedHour.value = hour
        _actualBedMin.value = min
        saveCurrentSleep()
    }

    fun setWakeTime(hour: Int, min: Int) {
        _actualWakeHour.value = hour
        _actualWakeMin.value = min
        saveCurrentSleep()
    }

    fun setQualityRating(rating: Int) {
        _qualityRating.value = rating
    }

    fun setBedtimeFocusEnabled(enabled: Boolean) {
        _bedtimeFocusEnabled.value = enabled
    }

    fun setNotes(newNotes: String) {
        _notes.value = newNotes
    }

    fun logSleepingNow() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val logicalDate = DateUtils.getLogicalSleepDateString(cal)
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMin = cal.get(Calendar.MINUTE)

            val existingLog = sleepRepository.getSleepLogForDate(logicalDate)
            
            sleepRepository.saveSleepSchedule(
                dateString = logicalDate,
                targetBedHour = existingLog?.targetBedtimeHour ?: 23,
                targetBedMin = existingLog?.targetBedtimeMinute ?: 0,
                targetWakeHour = existingLog?.targetWakeHour ?: 7,
                targetWakeMin = existingLog?.targetWakeMinute ?: 0,
                actualBedHour = currentHour,
                actualBedMin = currentMin,
                actualWakeHour = existingLog?.actualWakeHour ?: 7,
                actualWakeMin = existingLog?.actualWakeMinute ?: 0,
                qualityRating = existingLog?.qualityRating ?: 4,
                bedtimeFocusEnabled = existingLog?.bedtimeFocusEnabled ?: true,
                notes = existingLog?.notes ?: ""
            )
            
            _selectedDate.value = logicalDate
            loadDataForDate(logicalDate)
            
            _isSavedSuccess.value = true
            delay(1800L)
            _isSavedSuccess.value = false
        }
    }

    fun logWakingNow() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val logicalDate = DateUtils.getLogicalSleepDateString(cal)
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMin = cal.get(Calendar.MINUTE)

            val existingLog = sleepRepository.getSleepLogForDate(logicalDate)
            
            sleepRepository.saveSleepSchedule(
                dateString = logicalDate,
                targetBedHour = existingLog?.targetBedtimeHour ?: 23,
                targetBedMin = existingLog?.targetBedtimeMinute ?: 0,
                targetWakeHour = existingLog?.targetWakeHour ?: 7,
                targetWakeMin = existingLog?.targetWakeMinute ?: 0,
                actualBedHour = existingLog?.actualBedtimeHour ?: 23,
                actualBedMin = existingLog?.actualBedtimeMinute ?: 0,
                actualWakeHour = currentHour,
                actualWakeMin = currentMin,
                qualityRating = existingLog?.qualityRating ?: 4,
                bedtimeFocusEnabled = existingLog?.bedtimeFocusEnabled ?: true,
                notes = existingLog?.notes ?: ""
            )
            
            _selectedDate.value = logicalDate
            loadDataForDate(logicalDate)
            
            _isSavedSuccess.value = true
            delay(1800L)
            _isSavedSuccess.value = false
        }
    }

    fun saveCurrentSleep() {
        viewModelScope.launch {
            sleepRepository.saveSleepSchedule(
                dateString = _selectedDate.value,
                targetBedHour = _targetBedHour.value,
                targetBedMin = _targetBedMin.value,
                targetWakeHour = _targetWakeHour.value,
                targetWakeMin = _targetWakeMin.value,
                actualBedHour = _actualBedHour.value,
                actualBedMin = _actualBedMin.value,
                actualWakeHour = _actualWakeHour.value,
                actualWakeMin = _actualWakeMin.value,
                qualityRating = _qualityRating.value,
                bedtimeFocusEnabled = _bedtimeFocusEnabled.value,
                notes = _notes.value
            )
            _isSavedSuccess.value = true
            delay(1800L)
            _isSavedSuccess.value = false
        }
    }

    fun getUpcomingAndRecentDays(allLogs: List<SleepLogEntity>): List<SleepDayItem> {
        val todayStr = DateUtils.getTodayDateString()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1) // Tomorrow
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
        val tomorrowStr = dateFormat.format(cal.time)

        val logMap = allLogs.associateBy { it.dateString }
        val list = mutableListOf<SleepDayItem>()

        // 5 past days + today + tomorrow
        for (offset in -5..1) {
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dStr = dateFormat.format(c.time)
            list.add(
                SleepDayItem(
                    dateString = dStr,
                    dayLabel = when (dStr) {
                        todayStr -> "Today"
                        tomorrowStr -> "Tmrw"
                        else -> dayLabelFormat.format(c.time)
                    },
                    dayNumber = dayNumberFormat.format(c.time),
                    isToday = dStr == todayStr,
                    isTomorrow = dStr == tomorrowStr,
                    log = logMap[dStr]
                )
            )
        }
        return list
    }
}
