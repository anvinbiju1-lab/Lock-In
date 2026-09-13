package com.example.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApp
import com.example.data.entity.HabitEntity
import com.example.data.entity.HabitLogEntity
import com.example.data.repository.HabitRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayDetailInfo(
    val habit: HabitEntity,
    val isCompleted: Boolean,
    val completedTimestamp: Long? = null
)

data class HabitsUiState(
    val habits: List<HabitEntity> = emptyList(),
    val todayLogs: List<HabitLogEntity> = emptyList(),
    val currentStreak: Int = 0,
    val selectedDate: String = DateUtils.getTodayDateString(),
    val selectedDateLogs: List<HabitLogEntity> = emptyList(),
    val monthlyLogsMap: Map<String, List<HabitLogEntity>> = emptyMap(),
    val currentCalendarMonth: Calendar = Calendar.getInstance(),
    val isAddHabitDialogOpen: Boolean = false,
    val isDayDetailsSheetOpen: Boolean = false
)

class HabitViewModel(
    private val repository: HabitRepository = FocusLockApp.instance.habitRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateUtils.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _calendarMonth = MutableStateFlow(Calendar.getInstance())
    val calendarMonth: StateFlow<Calendar> = _calendarMonth.asStateFlow()

    private val _isAddHabitDialogOpen = MutableStateFlow(false)
    val isAddHabitDialogOpen: StateFlow<Boolean> = _isAddHabitDialogOpen.asStateFlow()

    private val _isDayDetailsSheetOpen = MutableStateFlow(false)
    val isDayDetailsSheetOpen: StateFlow<Boolean> = _isDayDetailsSheetOpen.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val baseDataFlow = combine(
        repository.allHabits,
        repository.getAllLogs(),
        _selectedDate,
        _calendarMonth,
        _currentStreak
    ) { habits, allLogs, selDate, calMonth, streak ->
        val today = DateUtils.getTodayDateString()
        val todayLogs = allLogs.filter { it.completedDate == today }
        val selectedLogs = allLogs.filter { it.completedDate == selDate }
        val monthlyLogs = allLogs.groupBy { it.completedDate }

        HabitsUiState(
            habits = habits,
            todayLogs = todayLogs,
            currentStreak = streak,
            selectedDate = selDate,
            selectedDateLogs = selectedLogs,
            monthlyLogsMap = monthlyLogs,
            currentCalendarMonth = calMonth
        )
    }

    val uiState: StateFlow<HabitsUiState> = combine(
        baseDataFlow,
        _isAddHabitDialogOpen,
        _isDayDetailsSheetOpen
    ) { baseState, isAddOpen, isSheetOpen ->
        baseState.copy(
            isAddHabitDialogOpen = isAddOpen,
            isDayDetailsSheetOpen = isSheetOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    init {
        refreshStreak()
    }

    fun refreshStreak() {
        viewModelScope.launch {
            _currentStreak.value = repository.calculateCurrentStreak()
        }
    }

    fun toggleHabitToday(habitId: Long) {
        toggleHabitForDate(habitId, DateUtils.getTodayDateString())
    }

    fun toggleHabitForDate(habitId: Long, date: String) {
        viewModelScope.launch {
            repository.toggleHabitStatus(habitId, date)
            refreshStreak()
        }
    }

    fun addHabit(title: String, description: String, targetTime: String, colorHex: String = "#38BDF8") {
        viewModelScope.launch {
            repository.insertHabit(
                HabitEntity(
                    title = title.trim(),
                    description = description.trim(),
                    targetTime = targetTime,
                    colorHex = colorHex
                )
            )
            _isAddHabitDialogOpen.value = false
            refreshStreak()
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            refreshStreak()
        }
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
        _isDayDetailsSheetOpen.value = true
    }

    fun closeDayDetailsSheet() {
        _isDayDetailsSheetOpen.value = false
    }

    fun setAddHabitDialogOpen(open: Boolean) {
        _isAddHabitDialogOpen.value = open
    }

    fun previousMonth() {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = _calendarMonth.value.timeInMillis
            add(Calendar.MONTH, -1)
        }
        _calendarMonth.value = newCal
    }

    fun nextMonth() {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = _calendarMonth.value.timeInMillis
            add(Calendar.MONTH, 1)
        }
        _calendarMonth.value = newCal
    }
}
