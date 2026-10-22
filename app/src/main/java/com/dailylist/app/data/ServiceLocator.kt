package com.dailylist.app.data

import android.content.Context
import com.dailylist.app.reminder.AlarmScheduler

object ServiceLocator {
    @Volatile
    private var repo: TaskRepository? = null

    fun repository(context: Context): TaskRepository =
        repo ?: synchronized(this) {
            repo ?: run {
                val appContext = context.applicationContext
                val db = DailyListDatabase.get(appContext)
                val scheduler = AlarmScheduler(appContext)
                TaskRepository(
                    db.taskDao(),
                    db.categoryDao(),
                    db.taskOccurrenceDao(),
                    scheduler,
                ).also { repo = it }
            }
        }
}
