package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "streak_freezes")
data class StreakFreeze(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: String, // ISO date e.g. "2026-10-30"
    val endDate: String,   // ISO date e.g. "2026-11-05"
    val type: String = "SICKNESS", // "SICKNESS" (Krankheit/Regeneration), "VACATION" (Urlaub/Reise)
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
