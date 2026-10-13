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
    private val weekCache = ConcurrentHashMap<String, List<String>>(128)
    private val parsedDateCache = ConcurrentHashMap<String, java.util.Date>(128)
    private val displayDateCache = ConcurrentHashMap<String, String>(64)
    private val shortDisplayCache = ConcurrentHashMap<String, String>(64)
    private val daysOfWeekShortCache = ConcurrentHashMap<String, String>(64)
    private val dayNumberCache = ConcurrentHashMap<String, String>(64)
    private val daysEn = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private val daysDe = arrayOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")

    private fun parseDateKey(dateKey: String): java.util.Date? {
        if (dateKey.isBlank()) return null
        val cached = parsedDateCache[dateKey]
        if (cached != null) return cached
        return try {
            val parsed = keyFormat.parse(dateKey) ?: return null
            parsedDateCache[dateKey] = parsed
            parsed
        } catch (_: Exception) {
            null
        }
    }

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

    fun getIsoDayOfWeek(dateKey: String): Int {
        isoDayOfWeekCache[dateKey]?.let { return it }
        val date = parseDateKey(dateKey) ?: return Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val cal = Calendar.getInstance().apply { time = date }
        // ISO day: Monday=1 .. Sunday=7
        val iso = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
        isoDayOfWeekCache[dateKey] = iso
        return iso
    }

    fun getDaysInSameWeek(dateKey: String): List<String> {
        weekCache[dateKey]?.let { return it }
        val date = parseDateKey(dateKey) ?: return emptyList()
        val cal = Calendar.getInstance().apply { time = date }
        // Move to Monday
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) cal.add(Calendar.DAY_OF_YEAR, -1)
        val res = (0..6).map {
            val d = Calendar.getInstance().apply { time = cal.time; add(Calendar.DAY_OF_YEAR, it) }
            keyFormat.format(d.time)
        }
        weekCache[dateKey] = res
        return res
    }

    fun daysBetween(fromKey: String, toKey: String): Int {
        val from = parseDateKey(fromKey) ?: return Int.MAX_VALUE
        val to = parseDateKey(toKey) ?: return Int.MAX_VALUE
        val diff = (to.time - from.time) / (1000 * 60 * 60 * 24)
        return diff.toInt()
    }

    fun wasTaskCreatedOnOrBefore(createdAtMillis: Long, dateKey: String): Boolean {
        val createdDate = java.util.Date(createdAtMillis)
        val createdKey = keyFormat.format(createdDate)
        return createdKey <= dateKey
    }

    fun getEffectiveWeeklyTarget(targetDaysPerWeek: Int, createdAtMillis: Long, dateKey: String): Int {
        // If task created mid-week, reduce target proportionally
        val weekDays = getDaysInSameWeek(dateKey)
        val createdKey = keyFormat.format(java.util.Date(createdAtMillis))
        val createdIndex = weekDays.indexOf(createdKey).let { if (it < 0) 0 else it }
        val availableDays = weekDays.size - createdIndex
        val cappedTarget = targetDaysPerWeek.coerceIn(1, 7)
        return ((cappedTarget.toDouble() / 7.0) * availableDays).coerceAtLeast(1.0).toInt()
    }

    fun getPastDays(count: Int): List<String> {
        val now = Calendar.getInstance()
        return (0 until count).map {
            val c = Calendar.getInstance().apply { time = now.time; add(Calendar.DAY_OF_YEAR, -((count - 1) - it)) }
            keyFormat.format(c.time)
        }
    }

    fun isTaskDueOnDate(
        frequencyType: String,
        daysOfWeek: String,
        targetDaysPerWeek: Int,
        dateKey: String,
        weeklyCompletedCount: Int,
        isCompletedOnDate: Boolean,
        intervalDays: Int,
        intervalAnchorDate: String,
        lastCompletionDateKey: String?,
        createdAtMillis: Long
    ): Boolean {
        return when (frequencyType) {
            "WEEKDAYS" -> {
                val allowed = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                allowed.contains(getIsoDayOfWeek(dateKey))
            }
            "WEEKLY" -> {
                val effective = getEffectiveWeeklyTarget(targetDaysPerWeek, createdAtMillis, dateKey)
                weeklyCompletedCount < effective || !isCompletedOnDate
            }
            "INTERVAL" -> {
                isIntervalTaskDue(intervalDays, intervalAnchorDate, lastCompletionDateKey, dateKey)
            }
            else -> true
        }
    }

    fun isIntervalTaskDue(
        intervalDays: Int,
        anchorDateKey: String,
        lastCompletionDateKey: String?,
        queryDateKey: String
    ): Boolean {
        val safeInterval = intervalDays.coerceAtLeast(1)
        val lastKey = lastCompletionDateKey ?: anchorDateKey
        if (lastKey.isBlank()) return true
        val days = daysBetween(lastKey, queryDateKey)
        return days >= safeInterval
    }

    fun getDaysUntilIntervalDue(
        intervalDays: Int,
        anchorDateKey: String,
        lastCompletionDateKey: String?,
        queryDateKey: String
    ): Int {
        val safeInterval = intervalDays.coerceAtLeast(1)
        val lastKey = lastCompletionDateKey ?: anchorDateKey
        if (lastKey.isBlank()) return 0
        val days = daysBetween(queryDateKey, lastKey)
        return (safeInterval - days).coerceAtLeast(0)
    }

    fun getRemainingDaysInWeek(dateKey: String): Int {
        val week = getDaysInSameWeek(dateKey)
        val index = week.indexOf(dateKey).let { if (it < 0) 0 else it }
        return (week.size - index - 1).coerceAtLeast(0)
    }

    fun getDayOfWeek(dateKey: String, language: AppLanguage = AppLanguage.DE): String {
        val cacheKey = "$dateKey-$language"
        displayDateCache[cacheKey]?.let { return it }
        val iso = getIsoDayOfWeek(dateKey)
        val label = if (language == AppLanguage.EN) daysEn[iso - 1] else daysDe[iso - 1]
        displayDateCache[cacheKey] = label
        return label
    }

    fun getDayNumber(dateKey: String): String {
        dayNumberCache[dateKey]?.let { return it }
        val date = parseDateKey(dateKey) ?: return ""
        val cal = Calendar.getInstance().apply { time = date }
        val num = cal.get(Calendar.DAY_OF_MONTH).toString()
        dayNumberCache[dateKey] = num
        return num
    }
}
