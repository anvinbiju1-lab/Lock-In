package com.example.ui.sleep

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SleepLogEntity
import com.example.data.repository.SleepRepository
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkHeroSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurplePrimaryHero
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SleepGraphView(
    logs: List<SleepLogEntity>,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGraphMode by remember { mutableIntStateOf(0) } // 0: Bedtime Timeline & Midnight line, 1: Duration
    var selectedDayIndex by remember { mutableIntStateOf(logs.size - 1) }

    val sortedLogs = remember(logs) {
        logs.sortedBy { it.dateString }
    }

    if (sortedLogs.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, PurplePrimary.copy(alpha = 0.3f))))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sleep logs yet. Set bedtime above to start tracking!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val safeSelectedIndex = selectedDayIndex.coerceIn(0, sortedLogs.size - 1)
    val selectedLog = sortedLogs.getOrNull(safeSelectedIndex) ?: sortedLogs.last()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_analytics_graph_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(DarkBorder, PurplePrimary.copy(alpha = 0.35f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header & Graph Mode Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SLEEP PATTERNS & TRENDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = PurplePrimary,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedGraphMode == 0) "Bedtime & Midnight Line" else "Daily Sleep Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Mode switch pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedGraphMode == 0) PurplePrimary else Color.Transparent)
                            .clickable { selectedGraphMode = 0 }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("tab_bedtime_graph")
                    ) {
                        Text(
                            text = "Bedtime",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedGraphMode == 0) PurplePrimaryDark else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedGraphMode == 1) PurplePrimary else Color.Transparent)
                            .clickable { selectedGraphMode = 1 }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("tab_duration_graph")
                    ) {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedGraphMode == 1) PurplePrimaryDark else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The Canvas Graph
            if (selectedGraphMode == 0) {
                BedtimeCanvasGraph(
                    logs = sortedLogs,
                    selectedIndex = safeSelectedIndex,
                    onSelectIndex = {
                        selectedDayIndex = it
                        onSelectDate(sortedLogs[it].dateString)
                    }
                )
            } else {
                DurationCanvasGraph(
                    logs = sortedLogs,
                    selectedIndex = safeSelectedIndex,
                    onSelectIndex = {
                        selectedDayIndex = it
                        onSelectDate(sortedLogs[it].dateString)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Day Inspection Chip Banner
            SelectedDayDetailBanner(log = selectedLog)
        }
    }
}

@Composable
private fun BedtimeCanvasGraph(
    logs: List<SleepLogEntity>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    // Y-Axis Range: 20:00 (8:00 PM) to 03:00 (3:00 AM)
    // 20:00 = 1200m, 23:00 = 1380m (Target), 24:00 = 1440m (Midnight), 03:00 = 1620m (Post-Midnight)
    val minYMinutes = 1200f // 8:00 PM
    val maxYMinutes = 1620f // 3:00 AM (next day)
    val targetMinutes = 1380f // 11:00 PM
    val midnightMinutes = 1440f // 12:00 AM Midnight

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "bedtime_graph_anim"
    )

    Column {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldSuccess))
                Text("Early", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PurplePrimary))
                Text("On Time", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AmberWarning))
                Text("Late", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFBA86FC)))
                Text("Post-12AM 🌙", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBA86FC), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(DarkBackground.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { /* handled by tap regions */ }
            ) {
                val width = size.width
                val height = size.height
                val count = logs.size
                if (count == 0) return@Canvas

                val stepX = width / (count.coerceAtLeast(1))

                // Convert minute to Y coordinate
                fun getY(minutes: Float): Float {
                    val norm = (minutes - minYMinutes) / (maxYMinutes - minYMinutes)
                    return (norm * height).coerceIn(10f, height - 10f)
                }

                val targetY = getY(targetMinutes)
                val midnightY = getY(midnightMinutes)

                // 1. Draw Target Line (11:00 PM) - Dotted Purple
                drawLine(
                    color = PurplePrimary.copy(alpha = 0.4f),
                    start = Offset(0f, targetY),
                    end = Offset(width, targetY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // 2. Draw 12:00 AM Midnight Reference Line - Bold Neon
                drawLine(
                    color = Color(0xFFBA86FC).copy(alpha = 0.8f),
                    start = Offset(0f, midnightY),
                    end = Offset(width, midnightY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                )

                // Draw Connecting Line between points
                val path = Path()
                logs.forEachIndexed { index, log ->
                    val normBed = if (log.actualBedtimeHour in 0..6) {
                        (log.actualBedtimeHour + 24) * 60f + log.actualBedtimeMinute
                    } else {
                        log.actualBedtimeHour * 60f + log.actualBedtimeMinute
                    }
                    val x = index * stepX + stepX / 2
                    val rawY = getY(normBed)
                    val y = targetY + (rawY - targetY) * animProgress

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = PurplePrimary.copy(alpha = 0.6f),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw Nodes for each day
                logs.forEachIndexed { index, log ->
                    val normBed = if (log.actualBedtimeHour in 0..6) {
                        (log.actualBedtimeHour + 24) * 60f + log.actualBedtimeMinute
                    } else {
                        log.actualBedtimeHour * 60f + log.actualBedtimeMinute
                    }
                    val x = index * stepX + stepX / 2
                    val rawY = getY(normBed)
                    val y = targetY + (rawY - targetY) * animProgress

                    val isSelected = index == selectedIndex
                    val isPostMidnight = log.actualBedtimeHour in 0..6
                    val delta = log.deltaBedtimeMinutes

                    val nodeColor = when {
                        isPostMidnight -> Color(0xFFBA86FC)
                        delta < -15 -> EmeraldSuccess
                        delta in -15..15 -> PurplePrimary
                        else -> AmberWarning
                    }

                    // Selection Glow
                    if (isSelected) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.35f),
                            radius = 18.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.7f),
                            radius = 12.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    // Center Solid Node
                    drawCircle(
                        color = nodeColor,
                        radius = if (isSelected) 8.dp.toPx() else 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = DarkBackground,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // Midnight Line Label
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "12:00 AM (Midnight)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBA86FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days bottom labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            logs.forEachIndexed { index, log ->
                val isSelected = index == selectedIndex
                val isPostMid = log.actualBedtimeHour in 0..6
                val dayLabel = formatDayLabel(log.dateString)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PurplePrimaryDark else Color.Transparent)
                        .clickable { onSelectIndex(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PurplePrimary else TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = SleepRepository.formatTime12H(log.actualBedtimeHour, log.actualBedtimeMinute),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPostMid) Color(0xFFBA86FC) else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (isPostMid) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationCanvasGraph(
    logs: List<SleepLogEntity>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "duration_graph_anim"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldSuccess))
                Text("7-9h Optimal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AmberWarning))
                Text("<6h Short", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text("Goal: 8.0 hrs", style = MaterialTheme.typography.labelSmall, color = PurplePrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(DarkBackground.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val count = logs.size
                if (count == 0) return@Canvas

                val stepX = width / count
                val barWidth = (stepX * 0.48f).coerceAtMost(36.dp.toPx())
                val maxHours = 12f

                // Draw 8-hour reference goal line
                val goalY = height * (1f - (8f / maxHours))
                drawLine(
                    color = PurplePrimary.copy(alpha = 0.5f),
                    start = Offset(0f, goalY),
                    end = Offset(width, goalY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                logs.forEachIndexed { index, log ->
                    val hours = log.durationMinutes / 60f
                    val barHeight = (height * (hours / maxHours) * animProgress).coerceIn(4f, height)
                    val x = index * stepX + (stepX - barWidth) / 2
                    val y = height - barHeight
                    val isSelected = index == selectedIndex

                    val barColor = when {
                        log.durationMinutes in 420..540 -> EmeraldSuccess
                        log.durationMinutes < 360 -> AmberWarning
                        else -> PurplePrimary
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isSelected) barColor else barColor.copy(alpha = 0.85f),
                                if (isSelected) barColor.copy(alpha = 0.7f) else barColor.copy(alpha = 0.4f)
                            )
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    if (isSelected) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days bottom labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            logs.forEachIndexed { index, log ->
                val isSelected = index == selectedIndex
                val dayLabel = formatDayLabel(log.dateString)
                val durationStr = String.format(Locale.getDefault(), "%.1fh", log.durationMinutes / 60f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PurplePrimaryDark else Color.Transparent)
                        .clickable { onSelectIndex(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PurplePrimary else TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = durationStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDayDetailBanner(log: SleepLogEntity) {
    val isPostMid = log.actualBedtimeHour in 0..6
    val delta = log.deltaBedtimeMinutes

    val statusBadgeColor = when {
        isPostMid -> Color(0xFFBA86FC)
        delta < -15 -> EmeraldSuccess
        delta in -15..15 -> PurplePrimary
        else -> AmberWarning
    }

    val statusText = when {
        isPostMid -> "🌙 Post-Midnight Sleep (${Math.abs(delta / 60)}h ${Math.abs(delta % 60)}m after 11 PM)"
        delta < -15 -> "🌟 Slept ${Math.abs(delta)}m Early (Target: 11:00 PM)"
        delta in -15..15 -> "🎯 On Target Bedtime"
        else -> "⚠️ Slept ${delta}m Late (Target: 11:00 PM)"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, statusBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.dateString,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusBadgeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = statusBadgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bed: ${SleepRepository.formatTime12H(log.actualBedtimeHour, log.actualBedtimeMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wake: ${SleepRepository.formatTime12H(log.actualWakeHour, log.actualWakeMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Score: ${log.sleepScore}/100",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning
                    )
                }
            }
        }
    }
}

private fun formatDayLabel(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("EEE", Locale.getDefault())
        val date = parser.parse(dateStr) ?: return dateStr
        formatter.format(date)
    } catch (e: Exception) {
        dateStr
    }
}
