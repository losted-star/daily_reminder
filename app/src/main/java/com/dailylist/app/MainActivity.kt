package com.dailylist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailylist.app.data.ThemeMode
import com.dailylist.app.ui.AppViewModelFactory
import com.dailylist.app.ui.DailyListRoot
import com.dailylist.app.ui.MainViewModel
import com.dailylist.app.ui.theme.DailyListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = AppViewModelFactory(application)
        val initialTaskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = factory)
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DailyListTheme(darkTheme = darkTheme) {
                DailyListRoot(mainViewModel, factory = factory, initialTaskId = initialTaskId)
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
