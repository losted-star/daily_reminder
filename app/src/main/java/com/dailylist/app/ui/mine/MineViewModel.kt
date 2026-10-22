package com.dailylist.app.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailylist.app.data.AppSettings
import com.dailylist.app.data.Category
import com.dailylist.app.data.ImportResult
import com.dailylist.app.data.SettingsRepository
import com.dailylist.app.data.TaskRepository
import com.dailylist.app.data.TodayStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MineViewModel(
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 最近 7 天（含今天）每天的完成数量，下标 0 为最早一天 */
    val weekCompletions: StateFlow<List<Pair<LocalDate, Int>>> = refreshTrigger
        .flatMapLatest { flow { emit(repository.completedPerDay(LocalDate.now().minusDays(6), 7)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayStats: StateFlow<TodayStats> = refreshTrigger
        .flatMapLatest { flow { emit(repository.todayStats(LocalDate.now())) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayStats(0, 0))

    fun refresh() {
        refreshTrigger.value++
    }

    fun addCategory(name: String, colorArgb: Int) {
        viewModelScope.launch { repository.addCategory(Category(name = name, colorArgb = colorArgb)) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    fun setDefaultReminder(minutes: Int?, advanceMinutes: Int) {
        viewModelScope.launch { settingsRepository.setDefaultReminder(minutes, advanceMinutes) }
    }

    suspend fun exportJson(): String = repository.exportData()

    suspend fun importJson(json: String): ImportResult = repository.importData(json)
}
