package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class UserProfile(
    val name: String = "Anvin Biju",
    val initials: String = "AB",
    val focusMotto: String = "High performance & zero distractions",
    val avatarColorHex: String = "#D0BCFF"
)

class UserProfileRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    private val _profileState = MutableStateFlow(loadProfile())
    val profileState: StateFlow<UserProfile> = _profileState.asStateFlow()

    fun hasCompletedPermissionOnboarding(): Boolean {
        return prefs.getBoolean("permission_onboarding_completed", false)
    }

    fun setPermissionOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("permission_onboarding_completed", completed).apply()
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean("is_first_launch_v1", true)
    }

    fun markFirstLaunchComplete() {
        prefs.edit().putBoolean("is_first_launch_v1", false).apply()
    }

    private fun loadProfile(): UserProfile {
        val name = prefs.getString("user_name", "Anvin Biju") ?: "Anvin Biju"
        val motto = prefs.getString("user_motto", "High performance & zero distractions")
            ?: "High performance & zero distractions"
        val color = prefs.getString("avatar_color", "#D0BCFF") ?: "#D0BCFF"
        val initials = deriveInitials(name)
        return UserProfile(
            name = name,
            initials = initials,
            focusMotto = motto,
            avatarColorHex = color
        )
    }

    fun updateProfile(name: String, motto: String, colorHex: String) {
        val cleanName = name.trim().ifEmpty { "Anvin Biju" }
        val cleanMotto = motto.trim().ifEmpty { "High performance & zero distractions" }
        val initials = deriveInitials(cleanName)

        prefs.edit()
            .putString("user_name", cleanName)
            .putString("user_motto", cleanMotto)
            .putString("avatar_color", colorHex)
            .apply()

        _profileState.value = UserProfile(
            name = cleanName,
            initials = initials,
            focusMotto = cleanMotto,
            avatarColorHex = colorHex
        )
    }

    companion object {
        fun deriveInitials(name: String): String {
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "AB"
                parts.size == 1 -> {
                    val word = parts[0].uppercase(Locale.getDefault())
                    if (word.length >= 2) word.substring(0, 2) else word.padEnd(2, ' ')
                }
                else -> {
                    val first = parts[0].firstOrNull()?.uppercaseChar() ?: 'A'
                    val second = parts[1].firstOrNull()?.uppercaseChar() ?: 'B'
                    "$first$second"
                }
            }
        }
    }
}
