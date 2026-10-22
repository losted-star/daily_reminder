package com.dailylist.app

import android.app.Application
import com.dailylist.app.data.ServiceLocator
import com.dailylist.app.reminder.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyListApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        appScope.launch {
            val repo = ServiceLocator.repository(this@DailyListApp)
            repo.seedDefaults()
            // 应用启动时对账，恢复可能因进程被杀而丢失的闹钟
            repo.resyncAllReminders()
        }
    }
}
