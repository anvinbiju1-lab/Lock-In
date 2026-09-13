package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val TIME_FORMAT = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val MONTH_YEAR_FORMAT = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val DAY_OF_WEEK_FORMAT = SimpleDateFormat("EEE", Locale.getDefault())

    fun getTodayDateString(): String {
        return DATE_FORMAT.format(Date())
    }

    fun formatDate(timestamp: Long): String {
        return DATE_FORMAT.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return TIME_FORMAT.format(Date(timestamp))
    }

    fun formatMonthYear(calendar: Calendar): String {
        return MONTH_YEAR_FORMAT.format(calendar.time)
    }

    fun parseDate(dateStr: String): Date? {
        return try {
            DATE_FORMAT.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun isWithinTimeWindow(currentTimeHHmm: String, startHHmm: String, endHHmm: String): Boolean {
        try {
            val (curH, curM) = currentTimeHHmm.split(":").map { it.toInt() }
            val (startH, startM) = startHHmm.split(":").map { it.toInt() }
            val (endH, endM) = endHHmm.split(":").map { it.toInt() }

            val curTotal = curH * 60 + curM
            val startTotal = startH * 60 + startM
            val endTotal = endH * 60 + endM

            return if (startTotal <= endTotal) {
                curTotal in startTotal..endTotal
            } else {
                // Window crosses midnight (e.g. 22:00 - 06:00)
                curTotal >= startTotal || curTotal <= endTotal
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun getCurrentTimeHHmm(): String {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }

    fun getLogicalSleepDateString(cal: Calendar = Calendar.getInstance()): String {
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val targetCal = cal.clone() as Calendar
        // If it's before 3:00 PM (15:00), we consider it part of the previous night's sleep cycle
        if (hour < 15) {
            targetCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return DATE_FORMAT.format(targetCal.time)
    }
}
