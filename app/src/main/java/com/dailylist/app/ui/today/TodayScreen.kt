package com.dailylist.app.ui.today

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailylist.app.data.Category
import com.dailylist.app.data.Task
import com.dailylist.app.ui.components.EmptyState
import com.dailylist.app.ui.components.TaskCard
import com.dailylist.app.ui.components.formatFullDate
import com.dailylist.app.ui.components.greeting
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TodayScreen(
    factory: ViewModelProvider.Factory,
    categories: List<Category>,
    onAdd: () -> Unit,
    onEdit: (Task) -> Unit,
    onToggleComplete: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onRestore: (Task) -> Unit,
) {
    val viewModel: TodayViewModel = viewModel(factory = factory)
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val backlog by viewModel.backlog.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Task?>(null) }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    fun handleDelete(task: Task) {
        onDelete(task)
        pendingDelete = task
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已删除「${task.title}」",
                actionLabel = "撤销",
            )
            if (result == SnackbarResult.ActionPerformed) {
                pendingDelete?.let { onRestore(it) }
            }
            pendingDelete = null
        }
    }

    val sortedTasks = remember(tasks, viewModel.sortByPriority) {
        if (viewModel.sortByPriority) {
            tasks.sortedWith(compareBy({ it.completed }, { -it.task.priority.sortOrder }, { it.task.dueDate }))
        } else {
            tasks.sortedWith(compareBy({ it.completed }, { it.task.dueDate }))
        }
    }

    val completedCount = tasks.count { it.completed }
    val totalCount = tasks.size

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Header() }
            item { ProgressCard(completed = completedCount, total = totalCount) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "今日待办",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.toggleSort() }) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = "切换排序",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (sortedTasks.isEmpty()) {
                item {
                    EmptyState(
                        title = "今天还没有任务",
                        subtitle = "点击右下角 + 添加一条待办",
                    )
                }
            } else {
                items(sortedTasks, key = { it.task.id }) { item ->
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

            if (backlog.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "待处理 (${backlog.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(backlog, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        completed = false,
                        category = task.categoryId?.let { categoryMap[it] },
                        onToggle = { onToggleComplete(task, it) },
                        onClick = { onEdit(task) },
                        onEdit = { onEdit(task) },
                        onDelete = { handleDelete(task) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加任务")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            text = greeting(),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatFullDate(LocalDate.now()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressCard(completed: Int, total: Int) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "今日进度",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "已完成 $completed / $total 项",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
