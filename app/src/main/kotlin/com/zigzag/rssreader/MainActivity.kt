package com.zigzag.rssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zigzag.rssreader.core.domain.model.ThemeMode
import com.zigzag.rssreader.core.domain.model.UserSettings
import com.zigzag.rssreader.core.domain.repository.FeedRepository
import com.zigzag.rssreader.core.domain.repository.NetworkMonitor
import com.zigzag.rssreader.core.domain.repository.SettingsRepository
import com.zigzag.rssreader.core.ui.theme.RssReaderTheme
import com.zigzag.rssreader.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(useDarkTheme) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
                onDispose { }
            }

            LaunchedEffect(Unit) { appViewModel.refreshOnStartup() }

            RssReaderTheme(themeMode = settings.themeMode) {
                AppNavigation()
            }
        }
    }
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val feedRepository: FeedRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        settingsRepository.observeSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UserSettings(),
            )

    private var refreshStarted = false

    fun refreshOnStartup() {
        if (refreshStarted) return
        refreshStarted = true
        viewModelScope.launch {
            if (!settingsRepository.observeSettings().first().refreshOnStartup) return@launch
            if (networkMonitor.isOnline.firstOrNull() == false) return@launch
            runCatching { feedRepository.refreshAll() }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
