package com.dailylist.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(taskId: Long, fireEpochMillis: Long) {
        val pi = reminderPendingIntent(taskId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            // 未授予精确闹钟权限时降级为不精确闹钟（可能延迟，属合理降级）
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireEpochMillis, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireEpochMillis, pi)
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(reminderPendingIntent(taskId))
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun reminderPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMINDER = "com.dailylist.app.action.REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
