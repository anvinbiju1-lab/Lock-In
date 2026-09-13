package com.example.ui.sleep

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import java.util.Locale

@Composable
fun SleepScreen(
    viewModel: SleepViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentLogs by viewModel.recentSleepLogs.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    val actualBedHour by viewModel.actualBedHour.collectAsStateWithLifecycle()
    val actualBedMin by viewModel.actualBedMin.collectAsStateWithLifecycle()
    val actualWakeHour by viewModel.actualWakeHour.collectAsStateWithLifecycle()
    val actualWakeMin by viewModel.actualWakeMin.collectAsStateWithLifecycle()
    val qualityRating by viewModel.qualityRating.collectAsStateWithLifecycle()
    val bedtimeFocusEnabled by viewModel.bedtimeFocusEnabled.collectAsStateWithLifecycle()
    val isSavedSuccess by viewModel.isSavedSuccess.collectAsStateWithLifecycle()

    val dayItems = remember(recentLogs, selectedDate) {
        viewModel.getUpcomingAndRecentDays(recentLogs)
    }

    val isPostMidnight = actualBedHour in 0..5
    val deltaMinutes = remember(actualBedHour, actualBedMin) {
        SleepRepository.calculateDeltaMinutes(actualBedHour, actualBedMin, 23, 0)
    }
    val calculatedDuration = remember(actualBedHour, actualBedMin, actualWakeHour, actualWakeMin) {
        SleepRepository.calculateDuration(actualBedHour, actualBedMin, actualWakeHour, actualWakeMin)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Sleep Summary Header
        item {
            SleepHeroBanner(logs = recentLogs)
        }

        // 2. Live Time & One-Tap Logging
        item {
            val liveTime by viewModel.liveTimeString.collectAsStateWithLifecycle()
            val liveAmPm by viewModel.liveAmPm.collectAsStateWithLifecycle()
            val logicalDate by viewModel.logicalDateString.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_live_clock_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(DarkBorder, PurplePrimary.copy(alpha = 0.4f)))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CURRENT TIME",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = liveTime.ifEmpty { "--:--" },
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurplePrimary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = liveAmPm,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Text(
                        text = "Logging for night of: $logicalDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBA86FC),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logSleepingNow() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("sleep_now_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                        ) {
                            Icon(
                                imageVector = if (isSavedSuccess) Icons.Default.Check else Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = PurplePrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSavedSuccess) "Saved!" else "Sleep",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimaryDark
                            )
                        }

                        Button(
                            onClick = { viewModel.logWakingNow() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("wake_now_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                        ) {
                            Icon(
                                imageVector = if (isSavedSuccess) Icons.Default.Check else Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = DarkSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSavedSuccess) "Saved!" else "Wake",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Manual Edit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Bedtime edit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m -> viewModel.setBedtime(h, m) },
                                        actualBedHour,
                                        actualBedMin,
                                        false
                                    ).show()
                                }
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Bedtime (${SleepRepository.formatTime12H(actualBedHour, actualBedMin)})", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }

                        // Wake time edit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m -> viewModel.setWakeTime(h, m) },
                                        actualWakeHour,
                                        actualWakeMin,
                                        false
                                    ).show()
                                }
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Wake (${SleepRepository.formatTime12H(actualWakeHour, actualWakeMin)})", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Optional: Small shield toggle under the main action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = bedtimeFocusEnabled,
                            onCheckedChange = { viewModel.setBedtimeFocusEnabled(it) },
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PurplePrimaryDark,
                                checkedTrackColor = PurplePrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-Enable Focus Shield when asleep",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 3. Interactive Sleep Analytics & Graphs (Bedtime Trends with Midnight Line & Duration)
        item {
            SleepGraphView(
                logs = recentLogs,
                onSelectDate = { dateStr -> viewModel.selectDate(dateStr) }
            )
        }

        // 4. Quick Actions & Chronotype Sleep Tips Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, PurplePrimary.copy(alpha = 0.3f))))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "💡 SLEEP HYGIENE & INSIGHTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = PurplePrimary,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Consistent Bedtimes: Going to sleep within 30 minutes of your target enhances deep REM recovery.\n• Post-Midnight Impact: Sleeping after 12:00 AM delays circadian temperature drops, causing morning grogginess.\n• Wind-Down Shield: Lock In keeps app distractions silenced during your designated sleep window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SleepHeroBanner(logs: List<SleepLogEntity>) {
    val count = logs.size
    val avgDurationMinutes = if (count > 0) logs.map { it.durationMinutes }.average().toInt() else 480
    val avgScore = if (count > 0) logs.map { it.sleepScore }.average().toInt() else 85

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sleep_hero_banner"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkHeroSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(DarkBorder, PurplePrimary.copy(alpha = 0.5f))
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(PurplePrimaryHero, DarkHeroSurface)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SLEEP TRACKER & RHYTHM",
                            style = MaterialTheme.typography.labelSmall,
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Circadian Harmony",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PurplePrimaryDark)
                            .border(1.5.dp, PurplePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Avg Duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${avgDurationMinutes / 60}h ${avgDurationMinutes % 60}m",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurplePrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(DarkBorder)
                    )

                    Column {
                        Text(
                            text = "Sleep Quality",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "$avgScore%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldSuccess
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(DarkBorder)
                    )

                    Column {
                        Text(
                            text = "Midnight Line",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Tracked 🌙",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFBA86FC)
                        )
                    }
                }
            }
        }
    }
}
