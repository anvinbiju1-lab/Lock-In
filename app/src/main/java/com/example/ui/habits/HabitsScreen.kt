package com.example.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.HabitEntity
import com.example.data.entity.HabitLogEntity
import com.example.data.repository.UserProfileRepository
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkHeroSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurplePrimaryHero
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DateUtils
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileRepo = remember { UserProfileRepository(context) }
    val userProfile by profileRepo.profileState.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = DateUtils.getTodayDateString()
    val completedTodayCount = uiState.todayLogs.filter { it.status }.size
    val totalHabitsCount = uiState.habits.size
    val targetProgress = if (totalHabitsCount > 0) completedTodayCount.toFloat() / totalHabitsCount else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 260),
        label = "hero_progress"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setAddHabitDialogOpen(true) },
                containerColor = PurplePrimary,
                contentColor = PurplePrimaryDark,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("add_habit_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card in "Professional Polish" Deep Purple (#4F378B) Style
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_hero_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkHeroSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Background decorative watermark
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .size(140.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                        )

                        Column(modifier = Modifier.padding(22.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PurplePrimary)
                                )
                                Text(
                                    text = "FOCUS ENGINE • ${userProfile.name.uppercase(Locale.getDefault())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary,
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Daily Momentum:\n$completedTodayCount of $totalHabitsCount Completed",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Smooth Animated Progress Bar
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = PurplePrimary,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Streak Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(PurplePrimaryDark)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = PurplePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${uiState.currentStreak}d Streak",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PurplePrimary
                                        )
                                    }
                                }

                                Text(
                                    text = "Auto-Synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Calendar: "Habit Pulse"
            item {
                MonthlyCalendarCard(
                    calendar = uiState.currentCalendarMonth,
                    monthlyLogs = uiState.monthlyLogsMap,
                    selectedDate = uiState.selectedDate,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onDayClick = { dateString ->
                        viewModel.selectDate(dateString)
                    }
                )
            }

            // Section: Active Habits
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE HABITS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "+ Add New",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        modifier = Modifier.clickable { viewModel.setAddHabitDialogOpen(true) }
                    )
                }
            }

            if (uiState.habits.isEmpty()) {
                item {
                    EmptyHabitsCard(onAddClick = { viewModel.setAddHabitDialogOpen(true) })
                }
            } else {
                items(uiState.habits, key = { it.id }) { habit ->
                    val isCompleted = uiState.todayLogs.any { it.habitId == habit.id && it.status }
                    val log = uiState.todayLogs.find { it.habitId == habit.id && it.status }
                    HabitCheckItemCard(
                        habit = habit,
                        isCompleted = isCompleted,
                        completedTimestamp = log?.completedTimestamp,
                        onToggle = { viewModel.toggleHabitToday(habit.id) },
                        onDelete = { viewModel.deleteHabit(habit.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add Habit Dialog
    if (uiState.isAddHabitDialogOpen) {
        AddHabitDialog(
            onDismiss = { viewModel.setAddHabitDialogOpen(false) },
            onConfirm = { title, desc, time, color ->
                viewModel.addHabit(title, desc, time, color)
            }
        )
    }

    // Day Details Bottom Sheet
    if (uiState.isDayDetailsSheetOpen) {
        DayDetailsBottomSheet(
            selectedDate = uiState.selectedDate,
            habits = uiState.habits,
            logs = uiState.selectedDateLogs,
            onDismiss = { viewModel.closeDayDetailsSheet() },
            onToggleHabit = { habitId ->
                viewModel.toggleHabitForDate(habitId, uiState.selectedDate)
            }
        )
    }
}

@Composable
fun HabitCheckItemCard(
    habit: HabitEntity,
    isCompleted: Boolean,
    completedTimestamp: Long?,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val checkButtonScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "habit_toggle_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("habit_item_${habit.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isCompleted) listOf(PurplePrimary, DarkBorder)
                else listOf(DarkBorder, DarkBorder)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCompleted) PurplePrimary.copy(alpha = 0.5f) else PurplePrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isCompleted) PurplePrimaryDark else PurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) TextSecondary else TextPrimary
                )
                if (habit.description.isNotEmpty()) {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Due at ${habit.targetTime} • Daily",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    if (isCompleted && completedTimestamp != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Logged at ${DateUtils.formatTime(completedTimestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Habit",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Quick Animated Toggle Check Button
                Box(
                    modifier = Modifier
                        .scale(checkButtonScale)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) PurplePrimary else PurplePrimaryDark)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = if (isCompleted) PurplePrimaryDark else PurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyCalendarCard(
    calendar: Calendar,
    monthlyLogs: Map<String, List<HabitLogEntity>>,
    selectedDate: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (String) -> Unit
) {
    val monthTitle = DateUtils.formatMonthYear(calendar)
    val today = DateUtils.getTodayDateString()

    val displayCal = (calendar.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = displayCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
    val maxDaysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = displayCal.get(Calendar.YEAR)
    val month = displayCal.get(Calendar.MONTH) + 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_calendar_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder))
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Month Header Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Habit Pulse",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(32.dp).testTag("cal_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = PurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(32.dp).testTag("cal_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = PurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day of Week Header
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (dayName in daysOfWeek) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val blankPrefixCount = firstDayOfWeek - 1

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until 6) {
                    val startIndex = row * 7
                    if (startIndex - blankPrefixCount >= maxDaysInMonth) break

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - blankPrefixCount + 1

                            if (dayNumber in 1..maxDaysInMonth) {
                                val dateString = String.format(
                                    Locale.getDefault(),
                                    "%04d-%02d-%02d",
                                    year,
                                    month,
                                    dayNumber
                                )
                                val isToday = (dateString == today)
                                val isSelected = (dateString == selectedDate)
                                val dayLogs = monthlyLogs[dateString] ?: emptyList()
                                val hasCompleted = dayLogs.any { it.status }

                                val animatedCellScale by animateFloatAsState(
                                    targetValue = if (isToday || isSelected) 1.05f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "cell_scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .scale(animatedCellScale)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isToday -> PurplePrimary
                                                hasCompleted -> PurplePrimaryDark
                                                isSelected -> PurplePrimaryDark.copy(alpha = 0.5f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                isToday -> PurplePrimary
                                                hasCompleted -> PurplePrimary.copy(alpha = 0.4f)
                                                isSelected -> PurplePrimary
                                                else -> DarkBorder
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onDayClick(dateString) }
                                        .testTag("cal_day_$dateString"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isToday -> PurplePrimaryDark
                                            hasCompleted -> PurplePrimary
                                            isSelected -> PurplePrimary
                                            else -> TextMuted
                                        }
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).height(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsBottomSheet(
    selectedDate: String,
    habits: List<HabitEntity>,
    logs: List<HabitLogEntity>,
    onDismiss: () -> Unit,
    onToggleHabit: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val completedCount = logs.filter { it.status }.size
    val totalCount = habits.size
    val ratePercentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HABIT PULSE LOG",
                        style = MaterialTheme.typography.labelSmall,
                        color = PurplePrimary,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkHeroSurface)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Adherence Rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = PurplePrimary
                        )
                        Text(
                            text = "$completedCount of $totalCount Completed",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "$ratePercentage%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurplePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Habit Timestamp Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (habits.isEmpty()) {
                Text(
                    text = "No active habits configured for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (habit in habits) {
                        val log = logs.find { it.habitId == habit.id && it.status }
                        val isCompleted = log != null

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleHabit(habit.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) DarkSurfaceCard else DarkBackground
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(
                                    if (isCompleted) listOf(PurplePrimary, DarkBorder)
                                    else listOf(DarkBorder, DarkBorder)
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted) PurplePrimary else PurplePrimaryDark
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = PurplePrimaryDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = habit.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isCompleted && log != null) {
                                                "✓ Completed at ${DateUtils.formatTime(log.completedTimestamp)}"
                                            } else {
                                                "Due: ${habit.targetTime} • Not Logged"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCompleted) PurplePrimary else TextMuted
                                        )
                                    }
                                }

                                Text(
                                    text = if (isCompleted) "Toggle" else "Mark Done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, targetTime: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetHour by remember { mutableStateOf("09") }
    var targetMinute by remember { mutableStateOf("00") }
    var selectedColor by remember { mutableStateOf("#D0BCFF") }

    val colors = listOf("#D0BCFF", "#7DDC97", "#FFD8A8", "#F2B8B5", "#38BDF8")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "New Daily Habit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Title") },
                    placeholder = { Text("e.g. Deep Work Session") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Goal or context") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Text(
                    text = "Daily Target Check-in Time (24h format)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = targetHour,
                        onValueChange = { if (it.length <= 2) targetHour = it },
                        label = { Text("Hour (00-23)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Text(":", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

                    OutlinedTextField(
                        value = targetMinute,
                        onValueChange = { if (it.length <= 2) targetMinute = it },
                        label = { Text("Min (00-59)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val h = targetHour.padStart(2, '0')
                        val m = targetMinute.padStart(2, '0')
                        onConfirm(title, description, "$h:$m", selectedColor)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                modifier = Modifier.testTag("save_habit_button")
            ) {
                Text("Create Habit", color = PurplePrimaryDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun EmptyHabitsCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = PurplePrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Habits Tracked Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add daily routines to start tracking your habit pulse and streaks.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text("Create First Habit", color = PurplePrimaryDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}
