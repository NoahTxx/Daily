package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DailyTask::class, TaskCompletion::class],
    version = 3,
    exportSchema = false
)
abstract class DailyDatabase : RoomDatabase() {

    abstract fun dailyTaskDao(): DailyTaskDao

    companion object {
        @Volatile
        private var INSTANCE: DailyDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN frequencyType TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN targetDaysPerWeek INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
            }
        }

        fun getInstance(context: Context): DailyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DailyDatabase::class.java,
                    "daily_habits.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
