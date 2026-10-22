package com.dailylist.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dailylist.app.data.ServiceLocator
import com.dailylist.app.data.SettingsRepository
import com.dailylist.app.data.TaskRepository
import com.dailylist.app.ui.calendar.CalendarViewModel
import com.dailylist.app.ui.mine.MineViewModel
import com.dailylist.app.ui.today.TodayViewModel

class AppViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    private val repository: TaskRepository by lazy { ServiceLocator.repository(app) }
    private val settings: SettingsRepository by lazy { SettingsRepository(app) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(repository, settings) as T
        modelClass.isAssignableFrom(TodayViewModel::class.java) -> TodayViewModel(repository) as T
        modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(repository) as T
        modelClass.isAssignableFrom(MineViewModel::class.java) -> MineViewModel(repository, settings) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
