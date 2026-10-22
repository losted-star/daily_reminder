package com.dailylist.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailylist.app.data.Task
import com.dailylist.app.ui.calendar.CalendarScreen
import com.dailylist.app.ui.edit.TaskEditorSheet
import com.dailylist.app.ui.mine.MineScreen
import com.dailylist.app.ui.today.TodayScreen
import java.time.LocalDate

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun DailyListRoot(
    mainViewModel: MainViewModel,
    factory: ViewModelProvider.Factory,
    initialTaskId: Long = -1L,
) {
    val navController = rememberNavController()
    val editorState = mainViewModel.editorState
    val categories by mainViewModel.categories.collectAsStateWithLifecycle()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(initialTaskId) {
        if (initialTaskId >= 0) mainViewModel.openTaskById(initialTaskId)
    }

    val items = listOf(
        BottomItem("today", "今日", Icons.Outlined.Today),
        BottomItem("calendar", "日历", Icons.Outlined.DateRange),
        BottomItem("mine", "我的", Icons.Outlined.Person),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("today") {
                TodayScreen(
                    factory = factory,
                    categories = categories,
                    onAdd = { mainViewModel.openNewTask() },
                    onEdit = { mainViewModel.openEditTask(it) },
                    onToggleComplete = { task, completed -> mainViewModel.setCompleted(task, LocalDate.now(), completed) },
                    onDelete = { mainViewModel.deleteTask(it.id) },
                    onRestore = { mainViewModel.saveTask(it.copy(id = 0L)) },
                )
            }
            composable("calendar") {
                CalendarScreen(
                    factory = factory,
                    categories = categories,
                    onAddForDate = { mainViewModel.openNewTask(it) },
                    onEdit = { mainViewModel.openEditTask(it) },
                    onToggleComplete = { task, completed -> mainViewModel.setCompleted(task, LocalDate.now(), completed) },
                    onDelete = { mainViewModel.deleteTask(it.id) },
                    onRestore = { mainViewModel.saveTask(it.copy(id = 0L)) },
                )
            }
            composable("mine") {
                MineScreen(
                    factory = factory,
                    themeMode = settings.themeMode,
                    onThemeModeChange = { mainViewModel.setThemeMode(it) },
                )
            }
        }
    }

    editorState?.let { state ->
        TaskEditorSheet(
            editingTask = state.editingTask,
            defaultDate = state.defaultDate,
            categories = categories,
            onDismiss = { mainViewModel.closeEditor() },
            onSave = { task ->
                mainViewModel.saveTask(task)
                mainViewModel.closeEditor()
            },
            defaultReminderMinutes = settings.defaultReminderMinutes,
            defaultReminderAdvanceMinutes = settings.defaultReminderAdvanceMinutes,
        )
    }
}
