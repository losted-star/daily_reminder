package com.dailylist.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailylist.app.data.AppSettings
import com.dailylist.app.data.Category
import com.dailylist.app.data.SettingsRepository
import com.dailylist.app.data.Task
import com.dailylist.app.data.TaskRepository
import com.dailylist.app.data.ThemeMode
import com.dailylist.app.domain.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditorState(
    val editingTask: Task? = null,
    val defaultDate: LocalDate = LocalDate.now(),
)

class MainViewModel(
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    var editorState by mutableStateOf<EditorState?>(null)
        private set

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openNewTask(defaultDate: LocalDate = LocalDate.now()) {
        editorState = EditorState(editingTask = null, defaultDate = defaultDate)
    }

    fun openEditTask(task: Task) {
        editorState = EditorState(editingTask = task, defaultDate = DateUtils.parseDate(task.dueDate))
    }

    fun openTaskById(id: Long) {
        viewModelScope.launch {
            repository.getTask(id)?.let {
                editorState = EditorState(editingTask = it, defaultDate = DateUtils.parseDate(it.dueDate))
            }
        }
    }

    fun closeEditor() {
        editorState = null
    }

    fun saveTask(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) repository.addTask(task) else repository.updateTask(task)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.deleteTask(id) }
    }

    fun setCompleted(task: Task, date: LocalDate, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(task, date, completed) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
