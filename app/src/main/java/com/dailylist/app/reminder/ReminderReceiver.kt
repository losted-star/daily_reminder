package com.dailylist.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailylist.app.data.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_REMINDER) return
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = ServiceLocator.repository(context)
                val task = repo.getTask(taskId)
                if (task != null) {
                    val today = LocalDate.now()
                    if (!repo.isCompletedOn(task, today)) {
                        NotificationHelper.showTaskReminder(context, task)
                    }
                    repo.resyncReminder(taskId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
