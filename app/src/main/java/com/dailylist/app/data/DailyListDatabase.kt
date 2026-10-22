package com.dailylist.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Task::class, Category::class, TaskOccurrence::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DailyListDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskOccurrenceDao(): TaskOccurrenceDao

    companion object {
        @Volatile
        private var instance: DailyListDatabase? = null

        fun get(context: Context): DailyListDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DailyListDatabase::class.java,
                    "dailylist.db",
                ).build().also { instance = it }
            }
    }
}
