package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DateUtils {
    private val keyFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    private val keyFormat: SimpleDateFormat get() = keyFormatThreadLocal.get()!!

    @Volatile
    private var cachedTodayDayMillis: Long = 0L
    @Volatile
    private var cachedTodayKey: String = ""

    private val isoDayOfWeekCache = ConcurrentHashMap<String, Int>(64)
    private val daysEn = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private val daysDe = arrayOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")

    fun getTodayKey(): String {
        val now = System.currentTimeMillis()
        if (now - cachedTodayDayMillis in 0..5000L && cachedTodayKey.isNotEmpty()) {
            return cachedTodayKey
        }
        val key = keyFormat.format(java.util.Date(now))
        cachedTodayKey = key
        cachedTodayDayMillis = now
        return key
    }

    fun isToday(dateKey: String): Boolean {
        return dateKey == getTodayKey()
    }

    fun formatDisplayDate(dateKey: String, language: AppLanguage = AppLanguage.DE): String {
        return try {
            val date = keyFormat.parse(dateKey) ?: return dateKey
            val isEn = language == AppLanguage.EN
            if (isToday(dateKey)) {
                if (isEn) {
                    "TODAY, " + SimpleDateFormat("MMMM d", Locale.US).format(date).uppercase()
                } else {
                    "HEUTE, " + SimpleDateFormat("d. MMMM", Locale.GERMAN).format(date).uppercase()
                }
            } else {
                if (isEn) {
                    SimpleDateFormat("EEEE, MMMM d", Locale.US).format(date).uppercase()
                } else {
                    SimpleDateFormat("EEEE, d. MMMM", Locale.GERMAN).format(date).uppercase()
                }
            }
        } catch (e: Exception) {
            dateKey
        }
    }

    fun formatShortDisplay(dateKey: String, language: AppLanguage = AppLanguage.DE): String {
        return try {
            val date = keyFormat.parse(dateKey) ?: return dateKey
            val locale = if (language == AppLanguage.EN) Locale.US else Locale.GERMAN
            val pattern = if (language == AppLanguage.EN) "EEE, MMM d" else "EE, d. MMM"
            SimpleDateFormat(pattern, locale).format(date).uppercase()
        } catch (e: Exception) {
            dateKey
        }
    }

    fun getDayOfWeek(dateKey: String, language: AppLanguage = AppLanguage.DE): String {
        val iso = getIsoDayOfWeek(dateKey)
        if (iso in 1..7) {
            return if (language == AppLanguage.EN) daysEn[iso - 1] else daysDe[iso - 1]
        }
        return ""
    }

    fun getDayNumber(dateKey: String): String {
        return if (dateKey.length >= 10) {
            val dayStr = dateKey.substring(8, 10)
            val d = dayStr.toIntOrNull()
            d?.toString() ?: dayStr
        } else {
            dateKey
        }
    }

    fun getPastDays(daysCount: Int): List<String> {
        val list = ArrayList<String>(daysCount)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(daysCount - 1))
        for (i in 0 until daysCount) {
            list.add(keyFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun getIsoDayOfWeek(dateKey: String): Int {
        val cached = isoDayOfWeekCache[dateKey]
        if (cached != null) return cached

        return try {
            val date = keyFormat.parse(dateKey) ?: 1
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                time = date as java.util.Date
            }
            val day = cal.get(Calendar.DAY_OF_WEEK)
            val res = if (day == Calendar.SUNDAY) 7 else day - 1
            isoDayOfWeekCache[dateKey] = res
            res
        } catch (e: Exception) {
            1
        }
    }

    fun getDaysInSameWeek(dateKey: String): List<String> {
        return try {
            val date = keyFormat.parse(dateKey) ?: return listOf(dateKey)
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                time = date
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val list = ArrayList<String>(7)
            for (i in 0 until 7) {
                list.add(keyFormat.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            list
        } catch (e: Exception) {
            listOf(dateKey)
        }
    }

    fun formatDaysOfWeekShort(daysOfWeekCsv: String, language: AppLanguage = AppLanguage.DE): String {
        val days = daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        if (days.size == 7) {
            return if (language == AppLanguage.EN) "Daily" else "Täglich"
        }
        if (days == listOf(1, 2, 3, 4, 5)) {
            return if (language == AppLanguage.EN) "Weekdays" else "Werktage"
        }
        if (days == listOf(6, 7)) {
            return if (language == AppLanguage.EN) "Weekend" else "Wochenende"
        }
        val namesEn = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
        val namesDe = mapOf(1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do", 5 to "Fr", 6 to "Sa", 7 to "So")
        val nameMap = if (language == AppLanguage.EN) namesEn else namesDe
        return days.mapNotNull { nameMap[it] }.joinToString(", ")
    }

    fun isTaskDueOnDate(
        frequencyType: String,
        daysOfWeek: String,
        targetDaysPerWeek: Int,
        dateKey: String,
        weeklyCompletedCount: Int = 0,
        isCompletedOnDate: Boolean = false
    ): Boolean {
        return when (frequencyType) {
            "WEEKDAYS" -> {
                val isoDay = getIsoDayOfWeek(dateKey)
                daysOfWeek.contains(('0' + isoDay).toString())
            }
            "WEEKLY" -> {
                weeklyCompletedCount < targetDaysPerWeek || isCompletedOnDate
            }
            else -> true
        }
    }

    fun getDateKeyFromMillis(millis: Long): String {
        return try {
            keyFormat.format(java.util.Date(millis))
        } catch (e: Exception) {
            getTodayKey()
        }
    }

    fun wasTaskCreatedOnOrBefore(createdAtMillis: Long, dateKey: String): Boolean {
        if (createdAtMillis <= 0L) return true
        val createdKey = getDateKeyFromMillis(createdAtMillis)
        if (createdKey.isBlank()) return true
        return createdKey <= dateKey
    }
}
