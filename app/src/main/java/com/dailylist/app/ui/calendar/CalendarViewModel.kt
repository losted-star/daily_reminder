package com.dailylist.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailylist.app.data.DayStatus
import com.dailylist.app.data.TaskItem
import com.dailylist.app.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(repository: TaskRepository) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    val selectedDate = MutableStateFlow(LocalDate.now())

    val summary: StateFlow<Map<LocalDate, DayStatus>> = _month
        .flatMapLatest { ym -> flow { emit(repository.monthSummary(ym.year, ym.monthValue)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val tasksForSelectedDate: StateFlow<List<TaskItem>> = selectedDate
        .flatMapLatest { d -> repository.observeTasksForDate(d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun prevMonth() {
        _month.value = _month.value.minusMonths(1)
    }

    fun nextMonth() {
        _month.value = _month.value.plusMonths(1)
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        if (YearMonth.from(date) != _month.value) {
            _month.value = YearMonth.from(date)
        }
    }
}
