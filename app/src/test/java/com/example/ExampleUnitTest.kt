package com.example

import com.example.util.AppLanguage
import com.example.util.AppStrings
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testStatsStrings_German() {
    val de = AppLanguage.DE
    assertEquals("Tag in Folge", AppStrings.get("habit_day_single", de))
    assertEquals("Tage in Folge", AppStrings.get("habit_days_plural", de))
    assertEquals("Längste Streak", AppStrings.get("longest_streak_label", de))
    assertEquals("ERFOLGSQUOTE", AppStrings.get("rate_label", de))
    assertEquals("AKTIVE TAGE", AppStrings.get("total_days", de))
    assertEquals("Erster Schritt", AppStrings.get("ms_3_desc", de))
    assertEquals("Noch 1 Tag", AppStrings.get("milestone_left_single", de))
    assertEquals("Noch %d Tage", AppStrings.get("milestone_left", de))
    assertEquals("%d %s gesamt", AppStrings.get("habit_total_count_format", de))
    assertEquals("Rekord: %dd", AppStrings.get("habit_record_format", de))
  }

  @Test
  fun testStatsStrings_English() {
    val en = AppLanguage.EN
    assertEquals("Day in a row", AppStrings.get("habit_day_single", en))
    assertEquals("Days in a row", AppStrings.get("habit_days_plural", en))
    assertEquals("Longest streak", AppStrings.get("longest_streak_label", en))
    assertEquals("COMPLETION RATE", AppStrings.get("rate_label", en))
    assertEquals("TOTAL DAYS", AppStrings.get("total_days", en))
    assertEquals("1 day left", AppStrings.get("milestone_left_single", en))
    assertEquals("%d days left", AppStrings.get("milestone_left", en))
    assertEquals("%d %s total", AppStrings.get("habit_total_count_format", en))
    assertEquals("Record: %dd", AppStrings.get("habit_record_format", en))
  }
}
