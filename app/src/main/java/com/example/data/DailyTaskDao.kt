package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {

    @Query("SELECT * FROM daily_tasks WHERE isArchived = 0 ORDER BY orderIndex ASC, id ASC")
    fun getAllActiveTasksFlow(): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks WHERE isArchived = 0 ORDER BY orderIndex ASC, id ASC")
    fun getAllActiveTasksSync(): List<DailyTask>

    @Query("SELECT * FROM daily_tasks ORDER BY isArchived ASC, orderIndex ASC, id ASC")
    fun getAllTasksFlow(): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks ORDER BY isArchived ASC, orderIndex ASC, id ASC")
    fun getAllTasksSync(): List<DailyTask>

    @Query("SELECT * FROM daily_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): DailyTask?

    @Query("SELECT * FROM daily_tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdSync(id: Long): DailyTask?

    @Query("SELECT * FROM daily_tasks WHERE isArchived = 0 AND reminderEnabled = 1")
    suspend fun getTasksWithReminders(): List<DailyTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTaskSync(task: DailyTask): Long

    @Update
    suspend fun updateTask(task: DailyTask)

    @Delete
    suspend fun deleteTask(task: DailyTask)

    @Query("DELETE FROM daily_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("SELECT COUNT(*) FROM daily_tasks")
    suspend fun getTaskCount(): Int

    // Completions
    @Query("SELECT * FROM task_completions WHERE dateKey = :dateKey")
    fun getCompletionsForDateFlow(dateKey: String): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM task_completions WHERE dateKey = :dateKey")
    fun getCompletionsForDateSync(dateKey: String): List<TaskCompletion>

    @Query("SELECT * FROM task_completions WHERE dateKey IN (:dateKeys)")
    fun getCompletionsForDateKeysFlow(dateKeys: List<String>): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM task_completions WHERE dateKey IN (:dateKeys)")
    fun getCompletionsForDateKeysSync(dateKeys: List<String>): List<TaskCompletion>

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND dateKey = :dateKey LIMIT 1")
    suspend fun getCompletion(taskId: Long, dateKey: String): TaskCompletion?

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND dateKey = :dateKey LIMIT 1")
    fun getCompletionSync(taskId: Long, dateKey: String): TaskCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: TaskCompletion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCompletionSync(completion: TaskCompletion): Long

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND dateKey = :dateKey")
    suspend fun deleteCompletion(taskId: Long, dateKey: String)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND dateKey = :dateKey")
    fun deleteCompletionSync(taskId: Long, dateKey: String)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun deleteAllCompletionsForTask(taskId: Long)

    @Query("SELECT * FROM task_completions WHERE dateKey >= :startDateKey ORDER BY dateKey ASC")
    fun getRecentCompletionsFlow(startDateKey: String): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM task_completions ORDER BY dateKey DESC")
    fun getAllCompletionsFlow(): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM task_completions ORDER BY dateKey DESC")
    fun getAllCompletionsSync(): List<TaskCompletion>

    @Query("SELECT DISTINCT dateKey FROM task_completions ORDER BY dateKey DESC")
    fun getAllCompletionDatesFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT dateKey FROM task_completions ORDER BY dateKey DESC")
    fun getAllCompletionDatesSync(): List<String>

    @Query("""
        SELECT tc.dateKey 
        FROM task_completions tc
        INNER JOIN daily_tasks dt ON tc.taskId = dt.id
        WHERE dt.isArchived = 0
        GROUP BY tc.dateKey
        HAVING COUNT(DISTINCT tc.taskId) >= (SELECT COUNT(*) FROM daily_tasks WHERE isArchived = 0) 
           AND (SELECT COUNT(*) FROM daily_tasks WHERE isArchived = 0) > 0
        ORDER BY tc.dateKey DESC
    """)
    fun getFullyCompletedDatesFlow(): Flow<List<String>>

    @Query("""
        SELECT tc.dateKey 
        FROM task_completions tc
        INNER JOIN daily_tasks dt ON tc.taskId = dt.id
        WHERE dt.isArchived = 0
        GROUP BY tc.dateKey
        HAVING COUNT(DISTINCT tc.taskId) >= (SELECT COUNT(*) FROM daily_tasks WHERE isArchived = 0) 
           AND (SELECT COUNT(*) FROM daily_tasks WHERE isArchived = 0) > 0
        ORDER BY tc.dateKey DESC
    """)
    fun getFullyCompletedDatesSync(): List<String>
}
