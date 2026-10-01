package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.GeistFontFamily
import com.example.ui.theme.GeistMonoFontFamily
import com.example.ui.theme.NDotFontFamily
import com.example.widget.DailyMonthMatrixWidgetProvider
import com.example.widget.DailyProgressWidgetProvider
import com.example.widget.DailyStreakWidgetProvider
import com.example.widget.DailyWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AccentColor(val key: String, val displayName: String, val color: Color) {
    RED("RED", "Signal Red", Color(0xFFD71921)),
    WHITE("WHITE", "Pure White", Color(0xFFF5F5F7)),
    ORANGE("ORANGE", "Electric Orange", Color(0xFFFF6B00)),
    GREEN("GREEN", "Matrix Green", Color(0xFFA3E635)),
    CYAN("CYAN", "Neon Cyan", Color(0xFF06B6D4))
}

enum class HeadingFont(val key: String, val displayName: String, val subtitle: String) {
    NDOT("NDOT", "NDot 57", "Dot Matrix"),
    GEIST("GEIST", "Geist Mono", "Monospace"),
    SILKSCREEN("SILKSCREEN", "Silkscreen", "Pixel Matrix"),
    SANS("SANS", "System Sans", "Clean Sans")
}

enum class BodyFont(val key: String, val displayName: String, val subtitle: String) {
    GEIST("GEIST", "Geist Sans", "Modern Clean"),
    MONO("MONO", "Geist Mono", "Monospace"),
    SANS("SANS", "System Sans", "Clean Sans")
}

enum class AppLanguage(val key: String, val displayName: String) {
    DE("DE", "Deutsch"),
    EN("EN", "English")
}

val LocalAccentColor = compositionLocalOf { Color(0xFFD71921) }
val LocalHeadingFontFamily = compositionLocalOf { NDotFontFamily }
val LocalBodyFontFamily = compositionLocalOf { GeistFontFamily }
val LocalAppLanguage = compositionLocalOf { AppLanguage.DE }

class AppSettings private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("daily_settings", Context.MODE_PRIVATE)

    private val _routinesResetInfoDismissed = MutableStateFlow(
        prefs.getBoolean(KEY_ROUTINES_RESET_INFO_DISMISSED, false)
    )
    val routinesResetInfoDismissed: StateFlow<Boolean> = _routinesResetInfoDismissed.asStateFlow()

    private val _generalReminderEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_GENERAL_REMINDER_ENABLED, false)
    )
    val generalReminderEnabled: StateFlow<Boolean> = _generalReminderEnabled.asStateFlow()

    private val _generalReminderHour = MutableStateFlow(
        prefs.getInt(KEY_GENERAL_REMINDER_HOUR, 20)
    )
    val generalReminderHour: StateFlow<Int> = _generalReminderHour.asStateFlow()

    private val _generalReminderMinute = MutableStateFlow(
        prefs.getInt(KEY_GENERAL_REMINDER_MINUTE, 0)
    )
    val generalReminderMinute: StateFlow<Int> = _generalReminderMinute.asStateFlow()

    private val _selectedAccent = MutableStateFlow(
        try {
            AccentColor.valueOf(prefs.getString(KEY_ACCENT, AccentColor.RED.name) ?: AccentColor.RED.name)
        } catch (e: Exception) {
            AccentColor.RED
        }
    )
    val selectedAccent: StateFlow<AccentColor> = _selectedAccent.asStateFlow()

    private val _selectedHeadingFont = MutableStateFlow(
        try {
            HeadingFont.valueOf(prefs.getString(KEY_HEADING_FONT, HeadingFont.NDOT.name) ?: HeadingFont.NDOT.name)
        } catch (e: Exception) {
            HeadingFont.NDOT
        }
    )
    val selectedHeadingFont: StateFlow<HeadingFont> = _selectedHeadingFont.asStateFlow()

    private val _selectedBodyFont = MutableStateFlow(
        try {
            BodyFont.valueOf(prefs.getString(KEY_BODY_FONT, BodyFont.GEIST.name) ?: BodyFont.GEIST.name)
        } catch (e: Exception) {
            BodyFont.GEIST
        }
    )
    val selectedBodyFont: StateFlow<BodyFont> = _selectedBodyFont.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, AppLanguage.DE.name) ?: AppLanguage.DE.name)
        } catch (e: Exception) {
            AppLanguage.DE
        }
    )
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _vibrationFeedbackEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_VIBRATION_FEEDBACK_ENABLED, false)
    )
    val vibrationFeedbackEnabled: StateFlow<Boolean> = _vibrationFeedbackEnabled.asStateFlow()

    private val _completionAnimationEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_COMPLETION_ANIMATION_ENABLED, true)
    )
    val completionAnimationEnabled: StateFlow<Boolean> = _completionAnimationEnabled.asStateFlow()

    fun dismissRoutinesResetInfo() {
        prefs.edit().putBoolean(KEY_ROUTINES_RESET_INFO_DISMISSED, true).apply()
        _routinesResetInfoDismissed.value = true
    }

    fun setGeneralReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GENERAL_REMINDER_ENABLED, enabled).apply()
        _generalReminderEnabled.value = enabled
    }

    fun setGeneralReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_GENERAL_REMINDER_HOUR, hour)
            .putInt(KEY_GENERAL_REMINDER_MINUTE, minute)
            .apply()
        _generalReminderHour.value = hour
        _generalReminderMinute.value = minute
    }

    fun setAccentColor(accent: AccentColor) {
        prefs.edit().putString(KEY_ACCENT, accent.name).apply()
        _selectedAccent.value = accent
        DailyWidgetProvider.notifyDataChanged(appContext)
        DailyStreakWidgetProvider.notifyDataChanged(appContext)
        DailyMonthMatrixWidgetProvider.notifyDataChanged(appContext)
        DailyProgressWidgetProvider.notifyDataChanged(appContext)
    }

    fun setHeadingFont(font: HeadingFont) {
        prefs.edit().putString(KEY_HEADING_FONT, font.name).apply()
        _selectedHeadingFont.value = font
    }

    fun setBodyFont(font: BodyFont) {
        prefs.edit().putString(KEY_BODY_FONT, font.name).apply()
        _selectedBodyFont.value = font
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _selectedLanguage.value = language
        DailyWidgetProvider.notifyDataChanged(appContext)
        DailyStreakWidgetProvider.notifyDataChanged(appContext)
        DailyMonthMatrixWidgetProvider.notifyDataChanged(appContext)
        DailyProgressWidgetProvider.notifyDataChanged(appContext)
    }

    fun setVibrationFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_FEEDBACK_ENABLED, enabled).apply()
        _vibrationFeedbackEnabled.value = enabled
    }

    fun setCompletionAnimationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETION_ANIMATION_ENABLED, enabled).apply()
        _completionAnimationEnabled.value = enabled
    }

    companion object {
        private const val KEY_ROUTINES_RESET_INFO_DISMISSED = "routines_reset_info_dismissed"
        private const val KEY_GENERAL_REMINDER_ENABLED = "general_reminder_enabled"
        private const val KEY_GENERAL_REMINDER_HOUR = "general_reminder_hour"
        private const val KEY_GENERAL_REMINDER_MINUTE = "general_reminder_minute"
        private const val KEY_ACCENT = "app_accent"
        private const val KEY_HEADING_FONT = "heading_font"
        private const val KEY_BODY_FONT = "body_font"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_VIBRATION_FEEDBACK_ENABLED = "vibration_feedback_enabled"
        private const val KEY_COMPLETION_ANIMATION_ENABLED = "completion_animation_enabled"

        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }
}
