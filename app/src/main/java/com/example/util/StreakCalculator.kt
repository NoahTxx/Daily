package com.example.util

import com.example.data.StreakFreeze
import com.example.ui.StreakStats

object StreakCalculator {

    fun isDateFrozen(dateKey: String, freezes: List<StreakFreeze>): Boolean {
        return freezes.any { (_, startDate, endDate) -> (dateKey >= startDate) && (dateKey <= endDate) }
    }

    fun calculateStreak(
        completionDates: Collection<String>,
        freezes: List<StreakFreeze> = emptyList(),
    ): Int {
        return calculateFullStreakStats(completionDates, freezes).currentStreak
    }

    fun calculateFullStreakStats(
        completionDates: Collection<String>,
        freezes: List<StreakFreeze> = emptyList(),
    ): StreakStats {
        val dateSet = completionDates.toSet()
        val todayKey = DateUtils.getTodayKey()
        val isTodayFrozen = isDateFrozen(todayKey, freezes)
        val isTodayDone = dateSet.contains(todayKey)
        val isTodaySecured = isTodayDone || isTodayFrozen
        val activeFreeze = freezes.firstOrNull { (_, startDate, endDate) -> (todayKey >= startDate) && (todayKey <= endDate) }

        if (completionDates.isEmpty() && freezes.isEmpty()) {
            return StreakStats(
                isTodaySecured = isTodaySecured,
                isTodayFrozen = isTodayFrozen,
                activeFreeze = activeFreeze,
            )
        }

        // 1. Current streak calculation
        var checkKey = todayKey
        var currentStreak = 0

        if (!isTodayDone && !isTodayFrozen) {
            checkKey = DateUtils.addDays(todayKey, -1)
        }

        while (true) {
            checkKey = if (dateSet.contains(checkKey)) {
                currentStreak++
                DateUtils.addDays(checkKey, -1)
            } else if (isDateFrozen(checkKey, freezes)) {
                // A frozen day preserves the streak without breaking it
                DateUtils.addDays(checkKey, -1)
            } else {
                break
            }
        }

        // 2. Longest streak calculation across all historic completion dates
        val sortedDates = completionDates.asSequence().distinct().sorted().toList()
        var longestStreak = 0
        var tempStreak = 0
        var prevDateStr: String? = null

        for (dStr in sortedDates) {
            if (prevDateStr == null) {
                tempStreak = 1
            } else {
                val diffDays = DateUtils.daysBetween(prevDateStr, dStr)
                if (diffDays == 1) {
                    tempStreak++
                } else if (diffDays > 1) {
                    // Check if all days in between were frozen
                    var allMidFrozen = true
                    var mid = DateUtils.addDays(prevDateStr, 1)
                    while (mid < dStr) {
                        if (!isDateFrozen(mid, freezes)) {
                            allMidFrozen = false
                            break
                        }
                        mid = DateUtils.addDays(mid, 1)
                    }
                    if (allMidFrozen) {
                        tempStreak++
                    } else {
                        tempStreak = 1
                    }
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDateStr = dStr
        }

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak
        }

        return StreakStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalDays = dateSet.size,
            isTodaySecured = isTodaySecured,
            isTodayFrozen = isTodayFrozen,
            activeFreeze = activeFreeze,
        )
    }
}
