package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_completions",
    indices = [
        Index(value = ["taskId", "dateKey"], unique = true, name = "index_task_completions_taskId_dateKey"),
        Index(value = ["dateKey"], name = "index_task_completions_dateKey")
    ]
)
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val dateKey: String, // "YYYY-MM-DD"
    val count: Int = 1,
    val completedAt: Long = System.currentTimeMillis()
)
