package com.dailylist.app.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailylist.app.data.Task
import com.dailylist.app.data.TaskItem
import com.dailylist.app.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class TodayViewModel(repository: TaskRepository) : ViewModel() {

    private val today = LocalDate.now()

    val tasks: StateFlow<List<TaskItem>> = repository.observeTasksForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val backlog: StateFlow<List<Task>> = repository.observeBacklog(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var sortByPriority by mutableStateOf(false)
        private set

    fun toggleSort() {
        sortByPriority = !sortByPriority
    }
}
