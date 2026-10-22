package com.dailylist.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailylist.app.data.Category
import com.dailylist.app.data.DayStatus
import com.dailylist.app.data.Task
import com.dailylist.app.ui.components.EmptyState
import com.dailylist.app.ui.components.TaskCard
import com.dailylist.app.ui.components.formatFullDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAY_HEADERS = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun CalendarScreen(
    factory: ViewModelProvider.Factory,
    categories: List<Category>,
    onAddForDate: (LocalDate) -> Unit,
    onEdit: (Task) -> Unit,
    onToggleComplete: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onRestore: (Task) -> Unit,
) {
    val viewModel: CalendarViewModel = viewModel(factory = factory)
    val month by viewModel.month.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedTasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val today = LocalDate.now()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Task?>(null) }

    fun handleDelete(task: Task) {
        onDelete(task)
        pendingDelete = task
        scope.launch {
            val result = snackbarHostState.showSnackbar("已删除「${task.title}」", actionLabel = "撤销")
            if (result == SnackbarResult.ActionPerformed) pendingDelete?.let { onRestore(it) }
            pendingDelete = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // 月份切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.prevMonth() }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上个月")
                }
                Text(
                    text = "${month.year}年${month.monthValue}月",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下个月")
                }
            }

            // 星期表头
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_HEADERS.forEach { w ->
                    Text(
                        text = w,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 月网格
            CalendarGrid(
                month = month,
                summary = summary,
                today = today,
                selectedDate = selectedDate,
                onSelect = { viewModel.selectDate(it) },
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // 选中日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatFullDate(selectedDate),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onAddForDate(selectedDate) }) {
                    Icon(Icons.Filled.Add, contentDescription = "该日添加任务", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (selectedTasks.isEmpty()) {
                EmptyState(title = "这一天没有任务", subtitle = "点击右侧 + 为这一天创建任务")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(selectedTasks, key = { it.task.id }) { item ->
                        TaskCard(
                            task = item.task,
                            completed = item.completed,
                            category = item.task.categoryId?.let { categoryMap[it] },
                            onToggle = { onToggleComplete(item.task, it) },
                            onClick = { onEdit(item.task) },
                            onEdit = { onEdit(item.task) },
                            onDelete = { handleDelete(item.task) },
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    summary: Map<LocalDate, DayStatus>,
    today: LocalDate,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    val offset = firstDay.dayOfWeek.value - 1 // 周一为一周第一天
    val start = firstDay.minusDays(offset.toLong())

    Column {
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (day in 0 until 7) {
                    val date = start.plusDays((week * 7 + day).toLong())
                    DayCell(
                        date = date,
                        inMonth = date.monthValue == month.monthValue,
                        status = summary[date],
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    status: DayStatus?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        inMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val dotColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        status?.hasUncompleted == true -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (status != null && status.hasTask) {
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            } else {
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}
